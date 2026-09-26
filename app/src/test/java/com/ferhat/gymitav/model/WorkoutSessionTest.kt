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
    fun resetCurrentTargetToDefaultChangesOnlyTheCurrentTarget() {
        val session = WorkoutSession(WorkoutState(exercise = 7, completedSets = 1, targetSets = 8, defaultSets = 3))
        session.toggleTimer(1_000L)
        session.refresh(12_500L)

        val update = session.resetCurrentTargetToDefault()

        assertEquals(3, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(7, update.state.exercise)
        assertEquals(1, update.state.completedSets)
        assertEquals(11L, update.state.elapsedSeconds)
        assertTrue(update.state.isRunning)
    }

    @Test
    fun restLimitMinimumIsFifteenAndDecrementCannotGoLower() {
        val session = WorkoutSession(WorkoutState(restLimitSeconds = 15))

        assertEquals(15, session.adjustRestLimit(-15, 0L).state.restLimitSeconds)
        assertEquals(30, session.adjustRestLimit(15, 0L).state.restLimitSeconds)
        assertEquals(15, session.adjustRestLimit(-15, 0L).state.restLimitSeconds)
    }

    @Test
    fun restLimitUsesFifteenSecondGridAroundTwoMinutes() {
        val session = WorkoutSession(WorkoutState(restLimitSeconds = 105))

        assertEquals(120, session.adjustRestLimit(15, 0L).state.restLimitSeconds)
        assertEquals(105, session.adjustRestLimit(-15, 0L).state.restLimitSeconds)
        var value = 105
        repeat(32) {
            value = WorkoutSession(WorkoutState(restLimitSeconds = value)).adjustRestLimit(15, 0L).state.restLimitSeconds
            assertEquals(0, value % 15)
        }
        value = 120
        repeat(32) {
            value = WorkoutSession(WorkoutState(restLimitSeconds = value)).adjustRestLimit(-15, 0L).state.restLimitSeconds
            assertEquals(0, value % 15)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun tenSecondRestLimitCannotExistInWorkoutState() {
        WorkoutState(restLimitSeconds = 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun offGridRestLimitCannotExistInWorkoutState() {
        WorkoutState(restLimitSeconds = 115)
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
    fun mainBackWithElapsedTimerResetsOnlyTimerAndPreservesProgress() {
        val session = WorkoutSession(WorkoutState(exercise = 3, completedSets = 2, targetSets = 5, defaultSets = 4))
        session.toggleTimer(0L)
        session.refresh(77_000L)
        session.toggleTimer(77_000L)

        val update = session.handleMainScreenBack(77_000L)

        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertEquals(3, update.state.exercise)
        assertEquals(2, update.state.completedSets)
        assertEquals(5, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
    }

    @Test
    fun mainBackWhileRunningResetsAndPausesTimer() {
        val session = WorkoutSession(WorkoutState(completedSets = 1))
        session.toggleTimer(1_000L)

        val update = session.handleMainScreenBack(43_000L)

        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertEquals(1, update.state.completedSets)
    }

    @Test
    fun mainBackAtPausedZeroClearsCompletedSetsOnly() {
        val session = WorkoutSession(
            WorkoutState(exercise = 3, completedSets = 2, targetSets = 5, defaultSets = 4, overdueReminderSeconds = 15)
        )

        val update = session.handleMainScreenBack(0L)

        assertEquals(0, update.state.completedSets)
        assertEquals(3, update.state.exercise)
        assertEquals(5, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
        assertEquals(120, update.state.restLimitSeconds)
        assertEquals(15, update.state.overdueReminderSeconds)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
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
        val session = WorkoutSession(WorkoutState(restLimitSeconds = 15, overdueReminderSeconds = 0))
        session.toggleTimer(0L)
        session.refresh(15_000L)
        session.resetTimer()
        session.toggleTimer(20_000L)

        assertEquals(listOf(RestNotification.REST_LIMIT_REACHED), session.refresh(35_000L).notifications)
    }

    @Test
    fun backgroundReminderPlanSchedulesThresholdAndConfiguredIntervals() {
        assertEquals(120L, RestReminderPlan.nextReminderAtSeconds(true, 30, 120, 10, false, null))
        assertEquals(120L, RestReminderPlan.nextReminderAtSeconds(true, 120, 120, 0, false, null))
        assertEquals(null, RestReminderPlan.nextReminderAtSeconds(true, 120, 120, 0, true, null))
        assertEquals(125L, RestReminderPlan.nextReminderAtSeconds(true, 120, 120, 5, true, 125L))
        assertEquals(130L, RestReminderPlan.nextReminderAtSeconds(true, 120, 120, 10, true, 130L))
        assertEquals(140L, RestReminderPlan.nextReminderAtSeconds(true, 133, 120, 10, true, 130L))
    }

    @Test
    fun backgroundReminderPlanSuspendsWhilePausedAndResetStartsFresh() {
        val paused = WorkoutSession(WorkoutState(isRunning = false, elapsedSeconds = 121, overdueReminderSeconds = 5))
        assertFalse(paused.state.isRunning)
        assertEquals(null, RestReminderPlan.nextReminderAtSeconds(false, 121, 120, 5, false, null))

        val reset = WorkoutSession(WorkoutState(restLimitSeconds = 15, overdueReminderSeconds = 0))
        reset.toggleTimer(0)
        reset.refresh(15_000)
        reset.resetTimer()
        assertEquals(15L, RestReminderPlan.nextReminderAtSeconds(true, reset.state.elapsedSeconds, 15, 0, reset.hasNotifiedRestLimit, reset.nextOverdueReminderAtSeconds))
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
