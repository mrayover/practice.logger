package com.mrayover.practicelogger

import android.media.*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Real platform feasibility probe, deliberately independent of the surrounding workspace UI. */
class LibraryPlatformGateTest {
    @Test fun platformSpeedSeekAndShortRepeatedRanges() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        for (ext in listOf("mp3", "m4a", "wav")) {
            val file = File(context.cacheDir, "gate.$ext")
            inst.context.assets.open("audio/backing.$ext").use { input -> file.outputStream().use(input::copyTo) }
            lateinit var player: MediaPlayer
            val prepared = CountDownLatch(1)
            var failure: String? = null
            inst.runOnMainSync {
                player = MediaPlayer()
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                player.setDataSource(file.path)
                player.setOnPreparedListener { prepared.countDown() }
                player.setOnErrorListener { _, what, extra -> failure = "$what/$extra"; prepared.countDown(); true }
                player.prepareAsync()
            }
            try {
                assertTrue(prepared.await(15, TimeUnit.SECONDS)); assertNull(failure)
                for (speed in listOf(1f, .75f, .5f, .95f)) {
                    var start = 0
                    inst.runOnMainSync {
                        player.playbackParams = PlaybackParams().setSpeed(speed).setPitch(1f)
                        player.start(); start = player.currentPosition
                        assertEquals(1f, player.playbackParams.pitch, .001f)
                        assertEquals(speed, player.playbackParams.speed, .001f)
                    }
                    Thread.sleep(400)
                    inst.runOnMainSync {
                        val delta = player.currentPosition - start
                        assertTrue("$ext speed=$speed delta=$delta", delta in 60..650)
                    }
                }
                repeat(4) {
                    val seek = CountDownLatch(1)
                    inst.runOnMainSync { player.setOnSeekCompleteListener { seek.countDown() }; player.seekTo(500, MediaPlayer.SEEK_CLOSEST) }
                    assertTrue("$ext seek stalled", seek.await(5, TimeUnit.SECONDS))
                    var position = 0
                    inst.runOnMainSync { position = player.currentPosition }
                    assertTrue("$ext boundary=$position", position in 300..750)
                    Thread.sleep(300)
                }
                inst.runOnMainSync { player.pause(); assertFalse(player.isPlaying) }
                assertNull(failure)
            } finally { inst.runOnMainSync { player.release() }; file.delete() }
        }
    }
}
