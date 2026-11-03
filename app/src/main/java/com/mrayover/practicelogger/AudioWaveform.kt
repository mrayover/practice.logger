package com.mrayover.practicelogger

import android.media.*
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max

/** Fixed 5ms source-time peak envelope: at most 180,001 floats (~704KiB), never full PCM. */
data class AudioWaveform(val durationMs: Long, val peaks: FloatArray) {
    companion object { const val BIN_MS = 5L; const val MAX_BINS = 180001 }
}

class AudioWaveformCache(private val repository: MediaLibraryRepository, private val directory: File) {
    companion object { private val mutex = Mutex(); private val name = Regex("[0-9a-f-]{36}-[0-9a-f]{64}\\.wave") }
    suspend fun load(mediaId: String): AudioWaveform = withContext(Dispatchers.IO) {
        mutex.withLock {
            val (asset, original) = repository.original(mediaId, "AUDIO")
            val duration = checkNotNull(asset.durationMs)
            val count = (duration / AudioWaveform.BIN_MS + 1).toInt()
            require(count in 1..AudioWaveform.MAX_BINS)
            check(directory.isDirectory || directory.mkdirs())
            directory.listFiles()?.filter { it.isFile && Regex("wave-[0-9]+\\.pending").matches(it.name) }?.forEach { it.delete() }
            val cache = File(directory, "${asset.id}-${asset.sha256}.wave")
            val cached = runCatching {
                DataInputStream(cache.inputStream().buffered()).use { input ->
                    check(input.readInt() == 0x57415631 && input.readLong() == duration && input.readInt() == count)
                    val peaks = FloatArray(count) { input.readFloat().also { check(it.isFinite() && it in 0f..1f) } }
                    check(input.read() == -1); AudioWaveform(duration, peaks)
                }
            }.getOrNull()
            if (cached != null) { cache.setLastModified(System.currentTimeMillis()); return@withLock cached }
            val peaks = FloatArray(count)
            fun peak(timeUs: Long, amplitude: Float) {
                val index = (timeUs / (AudioWaveform.BIN_MS * 1000)).toInt()
                if (index in peaks.indices && amplitude.isFinite()) peaks[index] = max(peaks[index], abs(amplitude).coerceAtMost(1f))
            }
            if (original.extension == "wav") wavePeaks(original, ::peak) else decodedPeaks(original, ::peak)
            currentCoroutineContext().ensureActive()
            // A unique pending file prevents partially written cache reuse after interruption.
            val pending = File.createTempFile("wave-", ".pending", directory)
            try {
                DataOutputStream(pending.outputStream().buffered()).use { output ->
                    output.writeInt(0x57415631); output.writeLong(duration); output.writeInt(count)
                    peaks.forEach(output::writeFloat)
                }
                if (cache.exists()) cache.delete()
                check(pending.renameTo(cache))
            } finally { pending.delete() }
            // Cache is expendable and capped at 12 recordings; never touches canonical/legacy files.
            directory.listFiles()?.filter { it.isFile && name.matches(it.name) && it != cache }
                ?.sortedByDescending { it.lastModified() }?.drop(11)?.forEach { it.delete() }
            AudioWaveform(duration, peaks)
        }
    }
    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock { directory.listFiles()?.filter { name.matches(it.name) && it.name.startsWith("$id-") }?.forEach { it.delete() } }
    }
}

private suspend fun wavePeaks(file: File, peak: (Long, Float) -> Unit) {
    RandomAccessFile(file, "r").use { input ->
        input.seek(12); var rate = 0; var channels = 0; var dataStart = 0L; var dataLength = 0L; var chunks = 0
        while (input.filePointer + 8 <= input.length()) {
            currentCoroutineContext().ensureActive()
            require(++chunks <= 4096)
            val tag = ByteArray(4).also(input::readFully).toString(Charsets.US_ASCII)
            val length = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
            val start = input.filePointer
            require(length <= input.length() - start)
            if (tag == "fmt ") {
                input.skipBytes(2); channels = java.lang.Short.reverseBytes(input.readShort()).toInt()
                rate = Integer.reverseBytes(input.readInt())
            }
            if (tag == "data") {
                dataStart = start; dataLength = length
            }
            input.seek(start + length + (length and 1))
        }
        require(rate in 8000..96000 && channels in 1..2 && dataLength > 0)
        input.seek(dataStart)
                val bytes = ByteArray(32768); var left = dataLength; var sample = 0L
                while (left > 0) {
                    currentCoroutineContext().ensureActive()
                    val n = minOf(left, bytes.size.toLong()).toInt(); input.readFully(bytes, 0, n)
                    for (i in 0 until n - 1 step 2) {
                        val value = ((bytes[i].toInt() and 255) or (bytes[i+1].toInt() shl 8)).toShort()
                        peak((sample / channels) * 1_000_000 / rate, value / 32768f); sample++
                    }
                    left -= n
                }
    }
}

private suspend fun decodedPeaks(file: File, peak: (Long, Float) -> Unit) {
    val extractor = MediaExtractor(); var codec: MediaCodec? = null
    val deadline = SystemClock.elapsedRealtime() + 120000
    try {
        extractor.setDataSource(file.path); extractor.selectTrack(0)
        val format = extractor.getTrackFormat(0)
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val decoder = MediaCodec.createDecoderByType(checkNotNull(format.getString(MediaFormat.KEY_MIME))); codec = decoder
        decoder.configure(format, null, null, 0); decoder.start()
        val info = MediaCodec.BufferInfo(); var ended = false; var floating = false; var frames = 0L
        while (true) {
            currentCoroutineContext().ensureActive()
            check(SystemClock.elapsedRealtime() < deadline) { "Waveform analysis timed out. Retry with the app open." }
            if (!ended) {
                val index = decoder.dequeueInputBuffer(10000)
                if (index >= 0) {
                    val buffer = checkNotNull(decoder.getInputBuffer(index)); buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) { decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); ended = true }
                    else { decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0); extractor.advance() }
                }
            }
            val index = decoder.dequeueOutputBuffer(info, 10000)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val out = decoder.outputFormat
                rate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE); channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                require(rate in 8000..96000 && channels in 1..2)
                val encoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) out.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                require(encoding in setOf(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT))
                floating = encoding == AudioFormat.ENCODING_PCM_FLOAT
            } else if (index >= 0) {
                try {
                    val buffer = checkNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.position(info.offset); buffer.limit(info.offset + info.size)
                    val bytes = if (floating) 4 else 2
                    val samples = info.size / bytes
                    require(frames + samples/channels <= rate.toLong() * 901)
                    repeat(samples) { sample ->
                        val amplitude = if (floating) buffer.float else buffer.short / 32768f
                        peak(info.presentationTimeUs + (sample / channels) * 1_000_000L / rate, amplitude)
                    }
                    frames += samples/channels
                } finally { decoder.releaseOutputBuffer(index, false) }
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        check(frames > 0)
    } finally { runCatching { codec?.stop() }; runCatching { codec?.release() }; extractor.release() }
}

/** Shared source-time viewport, independent of Compose and player ownership. */
data class AudioTimeline(val durationMs: Long, val startMs: Double = 0.0, val spanMs: Double = durationMs.toDouble()) {
    fun timeAt(fraction: Float): Long = (startMs + fraction.coerceIn(0f, 1f) * spanMs).toLong().coerceIn(0, durationMs)
    fun fraction(time: Long): Float = ((time - startMs) / spanMs).toFloat()
    fun transform(zoom: Float, panFraction: Float, anchor: Float = .5f): AudioTimeline {
        val span = (spanMs / zoom.coerceIn(.1f, 10f)).coerceIn(minOf(200.0, durationMs.toDouble()), durationMs.toDouble())
        val start = (startMs + spanMs * anchor - span * anchor - panFraction * span).coerceIn(0.0, durationMs - span)
        return copy(startMs = start, spanMs = span)
    }
}
