package com.ferhat.gymitav.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTest {
    @Test
    fun initialStateIsExerciseOneZeroOfThreeAndPaused() {
        val session = WorkoutSession()

        assertEquals(1, session.state.exercise)
        assertEquals(0, session.state.completedSets)
        assertEquals(3, session.state.targetSets)
        assertEquals(0L, session.state.elapsedSeconds)
        assertFalse(session.state.isRunning)
    }

    @Test
    fun setOkIncrementsCompletedSetsAndResetsPausedTimer() {
        val session = WorkoutSession()
        session.toggleTimer(0L)
        session.refresh(42_500L)

        val update = session.completeSet(42_500L)

        assertEquals(1, update.state.completedSets)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
    }

    @Test
    fun setOkNeverStartsTimerAutomatically() {
        val session = WorkoutSession()

        val update = session.completeSet(0L)

        assertFalse(update.state.isRunning)
        assertEquals(0L, update.state.elapsedSeconds)
    }

    @Test
    fun finalSetAdvancesExerciseAndStartsAtZeroOfDefaultTarget() {
        val session = WorkoutSession(WorkoutState(targetSets = 2, defaultSets = 4, completedSets = 1))

        val update = session.completeSet(0L)

        assertEquals(2, update.state.exercise)
        assertEquals(0, update.state.completedSets)
        assertEquals(4, update.state.targetSets)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
    }

    @Test
    fun increaseTargetSetsChangesCurrentTargetOnly() {
        val session = WorkoutSession(WorkoutState(completedSets = 1, targetSets = 3, defaultSets = 3))

        val update = session.increaseTargetSets()

        assertEquals(4, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(1, update.state.completedSets)
    }

    @Test
    fun defaultSetSettingIsUsedByTheFollowingExercise() {
        val session = WorkoutSession(WorkoutState(targetSets = 4, completedSets = 3))
        session.adjustDefaultSets(1)

        val update = session.completeSet(0L)

        assertEquals(4, update.state.defaultSets)
        assertEquals(4, update.state.targetSets)
    }

    @Test
    fun exerciseSettingChangesCurrentExerciseAndClampsAtOne() {
        val session = WorkoutSession()

        assertEquals(2, session.adjustExercise(1).state.exercise)
        assertEquals(1, session.adjustExercise(-1).state.exercise)
        assertEquals(1, session.adjustExercise(-1).state.exercise)
    }

    @Test
    fun resetPreservesWorkoutProgressAndSettings() {
        val session = WorkoutSession(WorkoutState(exercise = 3, completedSets = 2, targetSets = 5, defaultSets = 4))
        session.toggleTimer(1_000L)

        val update = session.resetTimer()

        assertEquals(3, update.state.exercise)
        assertEquals(2, update.state.completedSets)
        assertEquals(5, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
        assertFalse(update.state.isRunning)
        assertEquals(0L, update.state.elapsedSeconds)
    }

    @Test
    fun restLimitNotificationOccursOnceWhenThresholdIsCrossed() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 0))
        session.toggleTimer(0L)

        assertTrue(session.refresh(119_000L).notifications.isEmpty())
        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(120_000L).notifications)
        assertTrue(session.refresh(121_000L).notifications.isEmpty())
    }

    @Test
    fun pausingAndResumingAfterTheLimitDoesNotRepeatInitialNotification() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 0))
        session.toggleTimer(0L)
        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(120_000L).notifications)

        session.toggleTimer(121_000L)
        session.toggleTimer(130_000L)

        assertTrue(session.refresh(131_000L).notifications.isEmpty())
    }

    @Test
    fun delayedTimerRefreshEmitsThresholdOnlyThenResumesReminderCadence() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 10))
        session.toggleTimer(0L)

        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(133_000L).notifications)
        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(140_000L).notifications)
    }

    @Test
    fun overdueOffKeepsInitialVibrationAndDisablesRepeatedReminders() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 0))
        session.toggleTimer(0L)

        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(120_000L).notifications)
        assertTrue(session.refresh(300_000L).notifications.isEmpty())
    }

    @Test
    fun fiveSecondReminderRepeatsOnFiveSecondBoundaries() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 5))
        session.toggleTimer(0L)

        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(120_000L).notifications)
        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(125_000L).notifications)
        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(130_000L).notifications)
    }

    @Test
    fun tenSecondReminderRepeatsOnTenSecondBoundaries() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 10))
        session.toggleTimer(0L)

        session.refresh(120_000L)
        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(130_000L).notifications)
        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(140_000L).notifications)
    }

    @Test
    fun reminderChangesUseOnlyOffAndFiveSecondSteps() {
        val session = WorkoutSession()
        val seen = mutableListOf<Int>()
        repeat(6) { seen += session.adjustOverdueReminder(5, 0L).state.overdueReminderSeconds }

        assertEquals(listOf(15, 20, 25, 30, 30, 30), seen)
        assertEquals(25, session.adjustOverdueReminder(-5, 0L).state.overdueReminderSeconds)
        repeat(5) { session.adjustOverdueReminder(-5, 0L) }
        assertEquals(listOf(0, 5, 10, 15, 20, 25, 30), WorkoutSession.REMINDER_INTERVALS)
        assertEquals(0, session.state.overdueReminderSeconds)
    }

    @Test
    fun duplicateRefreshAtOneReminderBoundaryDoesNotDuplicateEvent() {
        val session = WorkoutSession(WorkoutState(overdueReminderSeconds = 10))
        session.toggleTimer(0L)
        session.refresh(120_000L)

        assertEquals(listOf(RestNotification.OVERDUE_REMINDER), session.refresh(130_000L).notifications)
        assertTrue(session.refresh(130_000L).notifications.isEmpty())
    }

    @Test
    fun resetBeginsANewThresholdNotificationSession() {
        val session = WorkoutSession(WorkoutState(restLimitSeconds = 10, overdueReminderSeconds = 0))
        session.toggleTimer(0L)
        session.refresh(10_000L)
        session.resetTimer()
        session.toggleTimer(20_000L)

        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(30_000L).notifications)
    }

    @Test
    fun pauseAndResumeAccumulateMonotonicElapsedTime() {
        val session = WorkoutSession()
        session.toggleTimer(10_000L)
        session.refresh(12_500L)
        session.toggleTimer(12_500L)

        assertEquals(2L, session.state.elapsedSeconds)
        assertFalse(session.state.isRunning)

        session.toggleTimer(20_000L)
        val resumed = session.refresh(22_000L)

        assertEquals(4L, resumed.state.elapsedSeconds)
        assertTrue(resumed.state.isRunning)
    }
}
