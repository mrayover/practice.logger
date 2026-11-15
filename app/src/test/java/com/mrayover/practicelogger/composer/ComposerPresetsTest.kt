package com.mrayover.practicelogger.composer

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerPresetsTest {
    @get:Rule val folder=TemporaryFolder()
    private val directory get()=File(folder.root,"presets")
    private fun rejects(block: ()->Unit) { assertTrue(runCatching(block).isFailure) }
    @Test fun immutableIdentityReopenDuplicateNamesAndAtomicFailure() {
        val sound=ComposerSound(345f,1234f,765f,2.1f,.43f,-.6f)
        val store=ComposerPresetStore(directory)
        val first=store.create("  Warm lead  ","synth-lead",sound)
        val bytes=File(directory,"${first.id}.json").readBytes()
        val second=store.create("Warm lead","synth-lead",sound.copy(cutoffHz=4000f))
        assertNotEquals(first.id,second.id); assertEquals(first,ComposerPresetStore(directory).read(first.id))
        assertEquals(sound,first.reference.sound); assertEquals("Warm lead",first.title)
        assertArrayEquals(bytes,File(directory,"${first.id}.json").readBytes())
        rejects { ComposerPresetStore(directory) { error("Write interruption") }.create("Failure","organ",sound) }
        assertEquals(2,store.list().first.size); assertEquals(0,store.list().second)
        assertFalse(directory.listFiles()!!.any { it.extension=="new" })
        rejects { store.read("../bad") }; rejects { store.create("\n","organ",sound) }
        rejects { store.create("Name","unknown",sound) }; rejects { store.create("Name","organ",sound.copy(pan=Float.NaN)) }
        File(directory,"${newCompositionId()}.json").writeText("{broken")
        assertEquals(1,store.list().second); assertEquals(first,store.read(first.id))
    }
    @Test fun v1UnchangedV2SelfContainedIndependentTracksAndStrictFields() {
        val note=CompositionNote(newCompositionId(),64,320,640,73)
        val track=CompositionTrack(newCompositionId(),"Keys",InstrumentReference(1,"reference-piano","default"),listOf(CompositionPattern(newCompositionId(),"B",4,listOf(note))))
        val p=CompositionProject.empty("Original").copy(tracks=listOf(track,track.copy(id=newCompositionId(),patterns=emptyList())))
        val text=CompositionCodec.encode(p); assertFalse(text.contains("cutoffHz")); assertEquals(p,CompositionCodec.decode(text))
        val preset=ComposerPresetStore(directory).create("Soft","acoustic-piano",ComposerSound(cutoffHz=900f))
        val edited=p.copy(formatVersion=2,tracks=p.tracks.mapIndexed { i,t -> if(i==0) t.copy(instrument=preset.reference) else t })
        val reopened=CompositionCodec.decode(CompositionCodec.encode(edited)); assertEquals(edited,reopened)
        val projects=File(folder.root,"projects"); val projectStore=CompositionStore(projects); projectStore.create(edited)
        rejects { CompositionStore(projects) { error("Atomic project failure") }.save(edited.copy(title="Failed change")) }
        assertEquals(edited,projectStore.read(edited.id))
        assertEquals(p.tracks[0].patterns,reopened.tracks[0].patterns); assertEquals(p.tracks[1],reopened.tracks[1])
        File(directory,"${preset.id}.json").delete() // Project playback never depends on the library file.
        val plan=LoopPlan.snapshot(reopened,listOf(track.patterns[0].id,""))
        assertEquals(preset.sound,plan.tracks[0].sound); assertNull(plan.tracks[1].sound); assertEquals(960,COMPOSER_PPQ)
        rejects { CompositionCodec.encode(edited.copy(formatVersion=1)) }
        val bad=JSONObject(CompositionCodec.encode(edited)); bad.getJSONArray("tracks").getJSONObject(0).getJSONObject("instrument").getJSONObject("sound").put("future",1)
        rejects { CompositionCodec.decode(bad.toString()) }
        rejects { ComposerSound.decode(preset.sound.json().put("cutoffHz","900")) }
        rejects { ComposerSound(releaseMs=0f).validate() }; rejects { ComposerSound(cutoffHz=18001f).validate() }
        rejects { CompositionCodec.decode(JSONObject(text).put("formatVersion",3).toString()) }
    }
}
