package com.mrayover.practicelogger

import org.junit.Assert.*
import org.junit.Test

class LibraryTimelineTest {
    @Test fun zoomedSourceCoordinatesRoundTripAcrossFifteenMinutes() {
        var timeline = AudioTimeline(900000)
        repeat(6) { timeline = timeline.transform(2f, 0f, .7f) }
        assertTrue(timeline.spanMs < 15000)
        for (fraction in listOf(0f, .1f, .5f, 1f)) {
            assertEquals(fraction, timeline.fraction(timeline.timeAt(fraction)), .001f)
        }
        timeline = timeline.transform(1f, -1000f)
        assertEquals(900000L, timeline.timeAt(1f))
        timeline = timeline.transform(1f, 1000f)
        assertEquals(0L, timeline.timeAt(0f))
        repeat(10) { timeline = timeline.transform(2f, 0f) }
        assertEquals(200.0, timeline.spanMs, .001)
    }
    @Test fun invalidRangesAndMarkerTypesAreRejected() {
        val point = MasterAudioMarker("id", "audio", "POINT", 100, null, "Solo", "")
        validateMasterMarker(point, 1000)
        validateMasterMarker(point.copy(type = "SECTION", endMs = 101), 1000)
        for (invalid in listOf(point.copy(startMs = -1), point.copy(startMs = 1001), point.copy(label = " "),
            point.copy(type = "SECTION", endMs = 100), point.copy(type = "SECTION", endMs = 99),
            point.copy(type = "SECTION", endMs = 1001), point.copy(type = "POINT", endMs = 200), point.copy(type = "OTHER"))) {
            assertThrows(IllegalArgumentException::class.java) { validateMasterMarker(invalid, 1000) }
        }
    }
}
