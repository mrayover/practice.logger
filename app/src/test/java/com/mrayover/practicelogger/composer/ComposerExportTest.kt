package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerExportTest {
    private fun fixture(): CompositionProject {
        val tracks=listOf(4,2,1).mapIndexed { i,bars ->
            CompositionTrack(newCompositionId(),"Voice",ComposerInstruments.all[listOf(0,3,8)[i]].reference,
                listOf(CompositionPattern(newCompositionId(),"A",bars,listOf(CompositionNote(newCompositionId(),if(i==2)36 else 60,0,960,80+i)))))
        }
        return CompositionProject.empty("Backing",120).copy(tracks=tracks)
    }
    @Test fun loopRepeatsEveryTrackWithoutEndpointOnAndPreservesDraft() {
        val p=fixture(); val before=CompositionCodec.encode(p)
        val snapshot=ComposerExportSnapshot.capture(p,emptyList(),false); val c=LoopClock(snapshot.plan)
        val end=snapshot.plan.frame(snapshot.plan.regionTicks); val ons=IntArray(3); val offs=IntArray(3)
        while(true) {
            c.dispatch { i,e -> if(e.on && c.frame<end) ons[i]++ else if(!e.on) offs[i]++ }
            if(c.frame>=end) break
            c.advance(minOf(256L,c.nextFrame()-c.frame,end-c.frame).toInt())
        }
        assertEquals(384000L,end); assertArrayEquals(intArrayOf(1,2,4),ons); assertArrayEquals(ons,offs)
        assertEquals(before,CompositionCodec.encode(p)); assertEquals(0L,p.revision)
    }
    @Test fun snapshotFreezesSelectedUnsavedPatternAndMixer() {
        val p=fixture().copy(formatVersion=4,masterVolume=.4f)
        val variation=p.tracks[0].patterns[0].copy(id=newCompositionId(),title="Unique",notes=listOf(CompositionNote(newCompositionId(),77,80,120,33)))
        val draft=p.copy(tracks=p.tracks.mapIndexed { i,t -> if(i==0) t.copy(patterns=t.patterns+variation,mixer=t.mixer.copy(pan=-.8f,processing=ChannelProcessing(lowDb=6f,width=.3f))) else t })
        val captured=ComposerExportSnapshot.capture(draft,listOf(variation.id),false)
        val edited=draft.replacePattern(variation.copy(notes=emptyList()))
        assertEquals(variation.notes,captured.plan.tracks[0].notes); assertTrue(edited.tracks[0].patterns[1].notes.isEmpty())
        assertEquals(draft,captured.project); assertNotSame(draft.tracks,captured.project.tracks)
    }
    @Test fun arrangementUsesClipPositionsVariationAndFiniteLatestEnd() {
        val p=fixture(); val t=p.tracks[2]; val a=t.patterns[0]
        val v=a.copy(id=newCompositionId(),title="Variation",notes=listOf(a.notes[0].copy(id=newCompositionId(),pitch=38)))
        val draft=p.copy(tracks=p.tracks.map { if(it.id==t.id) it.copy(patterns=it.patterns+v) else it },clips=listOf(
            ArrangementClip(newCompositionId(),t.id,a.id,0),ArrangementClip(newCompositionId(),t.id,v.id,3*p.barTicks())))
        val s=ComposerExportSnapshot.capture(draft,emptyList(),true)
        assertTrue(s.plan.song); assertEquals(4*p.barTicks(),s.plan.regionTicks)
        assertEquals(listOf(0L,3*p.barTicks()),s.plan.tracks[2].notes.map { it.startTick })
        assertEquals(listOf(36,38),s.plan.tracks[2].notes.map { it.pitch })
    }
    @Test fun waveHeaderSignedStereoLittleEndianAndExactLength() {
        val f=File.createTempFile("composer-wave",".wav")
        try {
            ComposerWaveSink(f).use { it.write(shortArrayOf(-32768,32767,-1,0),2); it.finish() }
            val bytes=f.readBytes(); val b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(52,bytes.size); assertEquals("RIFF",String(bytes,0,4)); assertEquals(44,b.getInt(4))
            assertEquals("WAVEfmt ",String(bytes,8,8)); assertEquals(1,b.getShort(20).toInt());assertEquals(2,b.getShort(22).toInt())
            assertEquals(48000,b.getInt(24));assertEquals(192000,b.getInt(28));assertEquals(4,b.getShort(32).toInt());assertEquals(16,b.getShort(34).toInt())
            assertEquals("data",String(bytes,36,4));assertEquals(8,b.getInt(40))
            assertArrayEquals(byteArrayOf(0,0x80.toByte(),0xff.toByte(),0x7f,0xff.toByte(),0xff.toByte(),0,0),bytes.copyOfRange(44,52))
        } finally { f.delete() }
    }
    @Test fun farArrangementRefusedBeforeRenderingAndFilenamesAreSafe() {
        val p=fixture();val t=p.tracks[0]
        val far=p.copy(clips=listOf(ArrangementClip(newCompositionId(),t.id,t.patterns[0].id,960L*4*1000000)))
        assertTrue(runCatching { ComposerExportSnapshot.capture(far,emptyList(),true) }.isFailure)
        assertEquals("a_b_c.m4a",exportFilename("a/b:c",ComposerExportFormat.M4A))
        val s=ComposerExportSnapshot.capture(p,emptyList(),false)
        assertEquals(44+(384000+30L*48000)*4,ComposerOfflineExport.estimatedBytes(s,ComposerExportFormat.WAV))
        assertTrue(ComposerOfflineExport.estimatedBytes(s,ComposerExportFormat.M4A)<ComposerOfflineExport.estimatedBytes(s,ComposerExportFormat.WAV)+16L*1024*1024)
    }
}
