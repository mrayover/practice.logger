package com.mrayover.practicelogger.composer

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class) @Config(sdk=[35])
class ArrangementPatternTest {
    private fun base(): CompositionProject {
        val a=CompositionPattern(newCompositionId(),"A",1,listOf(
            CompositionNote(newCompositionId(),60,120,480,83),CompositionNote(newCompositionId(),67,960,240,114)))
        val t=CompositionTrack(newCompositionId(),"Piano",ComposerInstruments.all[0].reference,listOf(a))
        var p=CompositionProject.empty("C5C").copy(tracks=listOf(t))
        for(bar in listOf(0L,4L,8L)) p=ArrangementEdits.add(p,t.id,a.id,bar*p.barTicks())
        return p
    }
    @Test fun uniqueRebindsOnlyOneClipAndPreservesMusicalAndMixerData() {
        val initial=base()
        val t=initial.tracks.single()
        val p=initial.copy(formatVersion=4,masterVolume=.43f,tracks=listOf(t.copy(mixer=TrackMixer(.62f,-.3f,true,true,t.patterns[0].id,ChannelProcessing(2f,3f,-4f,5f,false,1.4f)))))
        val result=ArrangementEdits.makeUnique(p,p.clips[1].id)
        val source=t.patterns[0]; val variation=result.tracks[0].patterns.last()
        assertEquals(p.copy(tracks=result.tracks,clips=result.clips),result)
        assertEquals(t.patterns,result.tracks[0].patterns.dropLast(1))
        assertEquals(p.tracks[0].copy(patterns=result.tracks[0].patterns),result.tracks[0])
        assertEquals("A Variation",variation.title); assertNotEquals(source.id,variation.id)
        assertEquals(source.lengthBars,variation.lengthBars)
        assertEquals(source.notes,variation.notes.mapIndexed { i,n -> assertNotEquals(source.notes[i].id,n.id); n.copy(id=source.notes[i].id) })
        assertEquals(p.clips.mapIndexed { i,c -> if(i==1) c.copy(patternId=variation.id) else c },result.clips)
        assertEquals(ArrangementEdits.end(p,p.clips[1]),ArrangementEdits.end(result,result.clips[1]))
        val edited=result.withNotes(variation,variation.notes.map { it.copy(pitch=72) })
        assertEquals(source,ArrangementEdits.pattern(edited,edited.clips[0]))
        assertEquals(source,ArrangementEdits.pattern(edited,edited.clips[2]))
        val sourceEdited=edited.withNotes(source,source.notes.map { it.copy(velocity=12) })
        assertEquals(edited.tracks[0].patterns.last(),sourceEdited.tracks[0].patterns.last())
        val song=LoopPlan.songSnapshot(sourceEdited).tracks.single().notes
        assertEquals(listOf(60,67,72,72,60,67),song.map { it.pitch })
        assertEquals(listOf(12,12,83,114,12,12),song.map { it.velocity })
        assertTrue(runCatching { result.replacePattern(variation.copy(lengthBars=8)) }.isFailure)
    }
    @Test fun singletonNamingAndLimitsAreAtomic() {
        val p=base(); val t=p.tracks[0]
        val singleton=p.copy(clips=p.clips.take(1))
        assertEquals(2,ArrangementEdits.makeUnique(singleton,singleton.clips[0].id).tracks[0].patterns.size)
        val named=p.copy(tracks=listOf(t.copy(patterns=t.patterns.map { it.copy(title="x".repeat(120)) })))
        val once=ArrangementEdits.makeUnique(named,named.clips[1].id)
        val twice=ArrangementEdits.makeUnique(once,once.clips[0].id)
        assertTrue(twice.tracks[0].patterns.all { it.title.length<=120 })
        assertEquals(3,twice.tracks[0].patterns.map { it.title }.distinct().size)
        val full=p.copy(tracks=listOf(t.copy(patterns=t.patterns+(1 until 256).map { CompositionPattern(newCompositionId(),"P$it",1) })))
        var current=full
        assertTrue(runCatching { current=ArrangementEdits.makeUnique(current,current.clips[1].id) }.exceptionOrNull()!!.message!!.contains("256"))
        assertSame(full,current)
        assertTrue(runCatching { ArrangementEdits.makeUnique(p,newCompositionId()) }.isFailure)
        // Source is valid at the global note budget; duplication must reject the complete snapshot.
        val many=t.patterns[0].copy(notes=(1..100000).map { CompositionNote(newCompositionId(),60,0,1,100) })
        val large=p.copy(tracks=listOf(t.copy(patterns=listOf(many))))
        CompositionCodec.validate(large)
        assertTrue(runCatching { ArrangementEdits.makeUnique(large,large.clips[1].id) }.isFailure)
        assertEquals(1,large.tracks[0].patterns.size)
    }
    @Test fun exactSaveReopenAndIndependentSaveAsForAllSupportedVersions() {
        val store=CompositionStore(Files.createTempDirectory("c5c").toFile())
        for(v in 1..4) {
            val p=store.create(base().copy(formatVersion=v))
            val unique=ArrangementEdits.makeUnique(p,p.clips[1].id)
            val saved=store.save(unique)
            assertEquals(saved,store.read(saved.id))
            val copied=store.create(saved.independentCopy("Independent"))
            assertEquals(copied,store.read(copied.id)); assertEquals(saved,store.read(saved.id))
            copied.clips.forEachIndexed { i,c ->
                assertNotEquals(saved.clips[i].id,c.id); assertNotEquals(saved.clips[i].trackId,c.trackId)
                assertNotEquals(saved.clips[i].patternId,c.patternId)
                assertEquals(saved.clips[i].startTick,c.startTick)
                assertEquals(ArrangementEdits.pattern(saved,saved.clips[i]).title,ArrangementEdits.pattern(copied,c).title)
            }
            assertEquals(copied.clips[0].patternId,copied.clips[2].patternId)
            assertNotEquals(copied.clips[0].patternId,copied.clips[1].patternId)
        }
    }
}
