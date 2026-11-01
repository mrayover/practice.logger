package com.mrayover.practicelogger

import android.media.*
import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** File repeat is a distinct persisted identity, never a beat-based phrase. Room11 needs no new columns. */
enum class TakeKind(val stored: String, val loop: Boolean) {
    FREE("FREE", false), RECORDED_LOOP("LOOP", true), IMPORTED_LOOP("IMPORTED_LOOP", true)
}
val BlockTake.kind: TakeKind get() = TakeKind.entries.firstOrNull { it.stored == mode } ?: error("Unknown take type")
val BlockTake.isLoop: Boolean get() = kind.loop
val BlockTake.audioMime: String get() = when (fileName.substringAfterLast('.')) {
    "wav" -> "audio/wav"; "mp3" -> "audio/mpeg"; else -> "audio/mp4"
}
fun takeDescription(take: BlockTake): String = when (take.kind) {
    TakeKind.FREE -> "Free"
    TakeKind.RECORDED_LOOP -> "Loop · ${take.recordedBpm} BPM · ${take.beatCount} beats"
    TakeKind.IMPORTED_LOOP -> "Imported loop"
}
data class ImportedAudioInfo(val name: String, val durationMs: Long)

/** Private bounded staging; original bytes remain compressed/on disk, never a whole decoded PCM array. */
class AudioImport(private val repository: TakeRepository) {
    companion object {
        const val MAX_BYTES = 50L * 1024 * 1024
        const val MAX_DURATION_MS = 15L * 60 * 1000
        fun extension(name: String): String {
            require(name.isNotBlank() && name.length <= 255 && name.none { it.isISOControl() }) { "Choose a filename of 1–255 characters." }
            return name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT).also {
                require(it in setOf("mp3", "m4a", "wav")) { "Choose MP3, M4A/AAC or PCM16 WAV audio." }
            }
        }
        suspend fun copy(input: InputStream, output: java.io.OutputStream) {
            val bytes = ByteArray(32 * 1024); var total = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(bytes); if (count < 0) break
                require(count > 0) { "Audio provider returned no data." }
                total += count; require(total <= MAX_BYTES) { "Audio must be no larger than 50 MiB." }
                output.write(bytes, 0, count)
            }
            require(total > 0) { "The audio file is empty." }
        }
    }
    suspend fun prepare(blockId: String, name: String, open: () -> InputStream): PendingTake {
        val extension = extension(name)
        val pending = repository.prepareImport(blockId, extension)
        var retained = false
        try {
            open().use { input -> pending.file.outputStream().use { copy(input, it) } }
            currentCoroutineContext().ensureActive()
            val duration = if (extension == "wav") waveDuration(pending.file) else decodedDuration(pending.file, extension)
            require(duration in 1..MAX_DURATION_MS) { "Audio must last between 1 ms and 15 minutes." }
            currentCoroutineContext().ensureActive()
            return pending.copy(imported = ImportedAudioInfo(name, duration)).also { retained = true }
        } finally { if (!retained) repository.abandon(pending) }
    }
}

/** RIFF PCM16, mono/stereo, 8–96kHz. Unlike PhraseWave this supports external rates and long files. */
internal fun waveDuration(file: File): Long = RandomAccessFile(file, "r").use { input ->
    fun u32(): Long = Integer.reverseBytes(input.readInt()).toLong() and 0xffffffffL
    fun u16(): Int = java.lang.Short.reverseBytes(input.readShort()).toInt() and 65535
    fun tag(): String = ByteArray(4).also { input.readFully(it) }.toString(Charsets.US_ASCII)
    require(file.length() in 44..AudioImport.MAX_BYTES) { "Invalid WAV size." }
    require(tag() == "RIFF" && u32() == file.length()-8 && tag() == "WAVE") { "Choose an ordinary RIFF WAV." }
    var rate = 0L; var align = 0; var data = 0L; var format = false; var chunks = 0
    while (input.filePointer < file.length()) {
        require(++chunks <= 4096) { "Too many WAV chunks." }
        require(file.length()-input.filePointer >= 8) { "Truncated WAV chunk." }
        val kind = tag(); val size = u32(); val start = input.filePointer
        require(size <= file.length()-start) { "Truncated WAV audio." }
        if (kind == "fmt ") {
            require(!format && size in 16..40) { "Invalid WAV format." }; format = true
            require(u16() == 1) { "WAV must contain uncompressed PCM16." }
            val channels = u16(); rate = u32(); val byteRate = u32(); align = u16(); val bits = u16()
            require(channels in 1..2 && rate in 8000..96000 && bits == 16 && align == channels*2 && byteRate == rate*align) { "Use mono/stereo PCM16 WAV at 8–96 kHz." }
        }
        if (kind == "data") { require(data == 0L && size > 0) { "Invalid WAV data." }; data = size }
        val next = start+size+(size and 1L); require(next <= file.length()) { "Truncated WAV padding." }; input.seek(next)
    }
    require(format && data > 0 && data % align == 0L) { "Incomplete WAV audio." }
    (data/align)*1000/rate
}

/** Drain the decoder to EOS with bounded buffers, frame/duration caps and a 90-second validation budget. */
private suspend fun decodedDuration(file: File, extension: String): Long {
    val extractor = MediaExtractor(); var codec: MediaCodec? = null
    val deadline = SystemClock.elapsedRealtime()+90_000
    try {
        extractor.setDataSource(file.absolutePath)
        require(extractor.trackCount == 1) { "Choose an audio-only file with one track." }
        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME)
        require(mime == if (extension == "mp3") "audio/mpeg" else "audio/mp4a-latm") { "File contents do not match supported MP3/M4A audio." }
        require(!format.containsKey("crypto-key")) { "Protected audio cannot be imported." }
        val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE); val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        require(rate in 8000..96000 && channels in 1..2) { "Use mono/stereo audio at 8–96 kHz." }
        if (format.containsKey(MediaFormat.KEY_DURATION)) require(format.getLong(MediaFormat.KEY_DURATION) <= AudioImport.MAX_DURATION_MS*1000) { "Audio must be no longer than 15 minutes." }
        extractor.selectTrack(0)
        val decoder = MediaCodec.createDecoderByType(checkNotNull(mime)); codec = decoder
        decoder.configure(format, null, null, 0); decoder.start()
        val bufferInfo = MediaCodec.BufferInfo(); var ended = false; var frames = 0L; var bytesPerFrame = channels*2
        while (true) {
            currentCoroutineContext().ensureActive()
            require(SystemClock.elapsedRealtime() < deadline) { "Audio validation took too long. Try a shorter file." }
            if (!ended) {
                val index = decoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val buffer = checkNotNull(decoder.getInputBuffer(index)); buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) { decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM); ended = true }
                    else {
                        require(extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_ENCRYPTED == 0) { "Protected audio cannot be imported." }
                        decoder.queueInputBuffer(index,0,size,extractor.sampleTime,0); extractor.advance()
                    }
                }
            }
            val index = decoder.dequeueOutputBuffer(bufferInfo,10_000)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val out = decoder.outputFormat
                require(out.getInteger(MediaFormat.KEY_SAMPLE_RATE) == rate && out.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == channels) { "Changing audio format is unsupported." }
                val encoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) out.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                bytesPerFrame = channels * when (encoding) { AudioFormat.ENCODING_PCM_16BIT -> 2; AudioFormat.ENCODING_PCM_FLOAT -> 4; else -> error("Unsupported decoded audio") }
            } else if (index >= 0) {
                try { frames += bufferInfo.size/bytesPerFrame } finally { decoder.releaseOutputBuffer(index,false) }
                require(frames <= rate.toLong()*AudioImport.MAX_DURATION_MS/1000) { "Audio must be no longer than 15 minutes." }
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }
        require(frames > 0) { "No decodable audio found." }
        return frames*1000/rate
    } finally { runCatching { codec?.stop() }; runCatching { codec?.release() }; extractor.release() }
}
