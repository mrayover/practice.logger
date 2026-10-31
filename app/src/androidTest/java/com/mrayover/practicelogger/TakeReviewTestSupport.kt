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
    compose.onNodeWithTag("review-save").scrollIfNeeded().assertIsDisplayed().performClick()
    compose.waitUntil(15000) { !vm.busy && vm.reviewTake == null && vm.takes.value.any { it.id == id } }
    if (playAfterSave) compose.runOnIdle { vm.play(vm.takes.value.single { it.id == id }) }
}
