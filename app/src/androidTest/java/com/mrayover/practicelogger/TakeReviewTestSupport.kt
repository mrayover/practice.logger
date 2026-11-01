package com.mrayover.practicelogger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

/** Existing workflows now make the explicit P1 acceptance decision before asserting saved takes. */
fun acceptTemporaryTake(compose: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    vm: RecordingViewModel, playAfterSave: Boolean = false) {
    compose.waitUntil(20000) { !vm.busy && !vm.recording && vm.reviewTake != null }
    val id = vm.reviewTake!!.id
    org.junit.Assert.assertTrue(vm.takes.value.none { it.id == id })
    // Completed-loop audition asynchronously prepares output before enabling Save.
    // Observe the real composed control after that transition; never tap a disabled stale node.
    compose.waitForIdle()
    compose.waitUntil(10000) { !vm.busy && compose.onAllNodes(hasTestTag("review-save") and isEnabled()).fetchSemanticsNodes().size == 1 }
    compose.onNodeWithTag("review-save").scrollIfNeeded().assertIsDisplayed().assertIsEnabled().performClick()
    compose.waitUntil(15000) { !vm.busy && vm.reviewTake == null && vm.takes.value.any { it.id == id } }
    if (playAfterSave) compose.runOnIdle { vm.play(vm.takes.value.single { it.id == id }) }
}
