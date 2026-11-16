package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ComposerC4DTest {
    private fun project(): CompositionProject {
        var p=CompositionProject.empty("Durable Mixer")
        repeat(8) { p=ComposerManagement.addTrack(p) }
        p=ComposerManagement.addPattern(p,p.tracks[2].id)
        return p.copy(formatVersion=3,masterVolume=.23f,tracks=p.tracks.mapIndexed { i,t ->
            t.copy(mixer=TrackMixer(.1f*i,if(i%2==0) -1f else 1f,i==0,i==2,t.patterns.last().id))
        })
    }
    @Test fun versionsAndStrictValidation() {
        val p=project(); assertEquals(p,CompositionCodec.decode(CompositionCodec.encode(p)))
        for(v in 1..2) {
            val old=p.copy(formatVersion=v,masterVolume=1f,tracks=p.tracks.map { it.copy(mixer=TrackMixer()) })
            val reopened=CompositionCodec.decode(CompositionCodec.encode(old))
            assertEquals(old,reopened); assertTrue(reopened.tracks.all { it.mixer==TrackMixer() })
        }
        fun invalid(value: CompositionProject) { assertTrue(runCatching { CompositionCodec.validate(value) }.isFailure) }
        invalid(p.copy(masterVolume=Float.NaN)); invalid(p.copy(masterVolume=1.01f))
        for(m in listOf(TrackMixer(pan=Float.POSITIVE_INFINITY),TrackMixer(pan=-1.01f),TrackMixer(volume=-.1f),TrackMixer(selectedPatternId=p.tracks[1].patterns[0].id))) {
            invalid(p.copy(tracks=p.tracks.mapIndexed { i,t -> if(i==0) t.copy(mixer=m) else t }))
        }
        val json=JSONObject(CompositionCodec.encode(p)); json.getJSONArray("tracks").getJSONObject(0).getJSONObject("mixer").put("mute","true")
        assertTrue(runCatching { CompositionCodec.decode(json.toString()) }.isFailure)
    }
    @Test fun copyDeletionAndAtomicFailurePreserveOwnership() {
        val p=project(); val copy=p.independentCopy("Independent")
        assertEquals(p.masterVolume,copy.masterVolume)
        p.tracks.zip(copy.tracks).forEach { (a,b) ->
            assertNotEquals(a.id,b.id); assertEquals(a.mixer.copy(selectedPatternId=b.patterns.last().id),b.mixer)
        }
        val removed=ComposerManagement.deleteTrack(p,p.tracks[1].id)
        assertEquals(p.tracks.filterIndexed { i,_ -> i!=1 },removed.tracks)
        val t=p.tracks[2]; val deleted=ComposerManagement.deletePattern(p,t.id,t.patterns.last().id)
        assertNull(deleted.tracks[2].mixer.selectedPatternId)
        assertEquals(t.mixer.copy(selectedPatternId=null),deleted.tracks[2].mixer)
        val duplicated=ComposerManagement.addPattern(p,t.id,t.patterns.last().id)
        assertEquals(t.mixer,duplicated.tracks[2].mixer)
        val dir=kotlin.io.path.createTempDirectory().toFile()
        try {
            val store=CompositionStore(dir); store.create(p)
            val saved=store.save(p.copy(masterVolume=.42f)); assertEquals(saved,CompositionStore(dir).read(p.id))
            val failing=CompositionStore(dir) { error("Injected before atomic commit") }
            assertTrue(runCatching { failing.save(saved.copy(masterVolume=.7f)) }.isFailure)
            assertEquals(saved,store.read(p.id)); store.create(copy); assertEquals(saved,store.read(p.id)); assertEquals(copy,store.read(copy.id))
        } finally { dir.deleteRecursively() }
    }
    @Test fun customSnapshotsAndTimingSurviveMixerEdits() {
        val p=project()
        val t=p.tracks[0]
        val sound=ComposerSound(pan=.63f)
        val ref=t.instrument.copy(presetId=newCompositionId(),presetTitle="Custom",sound=sound)
        val notes=listOf(CompositionNote(newCompositionId(),60,137,319,47),CompositionNote(newCompositionId(),60,200,700,113))
        val custom=p.copy(tracks=p.tracks.map { if(it.id==t.id) it.copy(instrument=ref,patterns=it.patterns.map { pat -> pat.copy(notes=notes) }) else it })
        val changed=custom.copy(tracks=custom.tracks.map { if(it.id==t.id) it.copy(mixer=it.mixer.copy(pan=-.8f,volume=.42f)) else it })
        assertEquals(custom.tracks[0].instrument,changed.tracks[0].instrument)
        val roundTrip=CompositionCodec.decode(CompositionCodec.encode(changed))
        assertEquals(changed,roundTrip);assertEquals(sound,roundTrip.independentCopy("Copy").tracks[0].instrument.sound)
        val before=LoopPlan.snapshot(custom,custom.tracks.map { it.patterns.first().id })
        val after=LoopPlan.snapshot(roundTrip,roundTrip.tracks.map { it.patterns.first().id })
        assertEquals(before,after)
    }
}
