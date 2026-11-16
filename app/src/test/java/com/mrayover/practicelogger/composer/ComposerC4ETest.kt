package com.mrayover.practicelogger.composer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerC4ETest {
    private fun project(): CompositionProject {
        var p=CompositionProject.empty("Processing")
        repeat(2) { p=ComposerManagement.addTrack(p) }
        return p.copy(formatVersion=4,tracks=p.tracks.mapIndexed { i,t -> t.copy(mixer=t.mixer.copy(processing=
            if(i==0) ChannelProcessing(-12f,3.1f,-2.2f,12f,false,1.87f) else ChannelProcessing())) })
    }
    @Test fun oldVersionsStayNeutralAndUnchangedOnRead() {
        for(v in 1..3) {
            val p=project().copy(formatVersion=v,tracks=project().tracks.map { it.copy(mixer=TrackMixer()) })
            val dir=kotlin.io.path.createTempDirectory().toFile()
            try { val store=CompositionStore(dir);store.create(p);val before=java.io.File(dir,"${p.id}.json").readBytes()
                assertEquals(p,store.read(p.id));assertArrayEquals(before,java.io.File(dir,"${p.id}.json").readBytes())
                assertTrue(store.read(p.id).tracks.all { it.mixer.processing==ChannelProcessing() })
            } finally { dir.deleteRecursively() }
        }
    }
    @Test fun strictV4AndIndependentCopyDeletionDuplication() {
        val p=project();assertEquals(p,CompositionCodec.decode(CompositionCodec.encode(p)))
        val copy=p.independentCopy("Copy")
        p.tracks.zip(copy.tracks).forEach { (a,b) -> assertNotEquals(a.id,b.id);assertEquals(a.mixer.processing,b.mixer.processing) }
        assertEquals(p.tracks.drop(1),ComposerManagement.deleteTrack(p,p.tracks[0].id).tracks)
        assertEquals(p.tracks[0].mixer,ComposerManagement.addPattern(p,p.tracks[0].id,p.tracks[0].patterns[0].id).tracks[0].mixer)
        for(bad in listOf(ChannelProcessing(lowDb=Float.NaN),ChannelProcessing(highDb=12.01f),ChannelProcessing(width=-.01f),ChannelProcessing(width=2.01f))) {
            assertTrue(runCatching { CompositionCodec.validate(p.copy(tracks=p.tracks.map { it.copy(mixer=it.mixer.copy(processing=bad)) })) }.isFailure)
        }
        fun reject(mutate:(JSONObject)->Unit) { val j=JSONObject(CompositionCodec.encode(p));mutate(j);assertTrue(runCatching { CompositionCodec.decode(j.toString()) }.isFailure) }
        reject { it.put("formatVersion",5) }
        reject { it.getJSONArray("tracks").getJSONObject(0).getJSONObject("mixer").getJSONObject("processing").remove("width") }
        reject { it.getJSONArray("tracks").getJSONObject(0).getJSONObject("mixer").getJSONObject("processing").put("eqEnabled","false") }
        reject { it.getJSONArray("tracks").getJSONObject(0).getJSONObject("mixer").getJSONObject("processing").put("lowDb",-12.0000001) }
        reject { it.getJSONArray("tracks").getJSONObject(0).getJSONObject("mixer").getJSONObject("processing").put("extra",1) }
        reject { it.put("formatVersion",3) }
    }
    @Test fun exactSaveAsAndMusicalTiming() {
        val p=project();val dir=kotlin.io.path.createTempDirectory().toFile()
        try {
            val store=CompositionStore(dir);store.create(p);val changed=p.copy(masterVolume=.37f)
            val saved=store.save(changed);assertEquals(saved,store.read(p.id));val copy=saved.independentCopy("Independent");store.create(copy)
            assertEquals(saved,store.read(p.id));assertEquals(copy,store.read(copy.id));assertEquals(saved.tracks.map { it.mixer.processing },copy.tracks.map { it.mixer.processing })
            val plain=p.copy(tracks=p.tracks.map { it.copy(mixer=TrackMixer()) })
            assertEquals(LoopPlan.snapshot(p,p.tracks.map { it.patterns.first().id }),LoopPlan.snapshot(plain,plain.tracks.map { it.patterns.first().id }))
        } finally { dir.deleteRecursively() }
    }
}
