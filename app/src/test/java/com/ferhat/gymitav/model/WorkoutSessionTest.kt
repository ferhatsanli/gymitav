package com.ferhat.gymitav.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTest {
    @Test
    fun initialStateIsExerciseOneSetOneOfThreeAndPaused() {
        val session = WorkoutSession()

        assertEquals(1, session.state.exercise)
        assertEquals(1, session.state.currentSet)
        assertEquals(3, session.state.targetSets)
        assertEquals(0L, session.state.elapsedSeconds)
        assertFalse(session.state.isRunning)
        assertFalse(session.state.isWorkoutSessionActive)
    }

    @Test
    fun setOkAdvancesToNextCurrentSetAndResetsPausedTimer() {
        val session = WorkoutSession()
        session.toggleTimer(0L)
        session.refresh(42_500L)

        val update = session.completeSet(42_500L)

        assertEquals(2, update.state.currentSet)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun setOkNeverStartsTimerAutomatically() {
        val session = WorkoutSession()

        val update = session.completeSet(0L)

        assertFalse(update.state.isRunning)
        assertEquals(0L, update.state.elapsedSeconds)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun setOkFromSetTwoShowsFinalSetWithoutAdvancingExercise() {
        val session = WorkoutSession(WorkoutState(targetSets = 3, defaultSets = 4, currentSet = 2))

        val update = session.completeSet(0L)

        assertEquals(1, update.state.exercise)
        assertEquals(3, update.state.currentSet)
        assertEquals(3, update.state.targetSets)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun setOkFromFinalSetAdvancesExerciseAndStartsAtSetOneOfDefaultTarget() {
        val session = WorkoutSession(WorkoutState(targetSets = 3, defaultSets = 4, currentSet = 3))

        val update = session.completeSet(0L)

        assertEquals(2, update.state.exercise)
        assertEquals(1, update.state.currentSet)
        assertEquals(4, update.state.targetSets)
        assertFalse(update.state.isRunning)
        assertEquals(0L, update.state.elapsedSeconds)
    }

    @Test
    fun oneTargetSetAdvancesOnlyWhenSetOkIsPressed() {
        val session = WorkoutSession(WorkoutState(targetSets = 1, defaultSets = 3, currentSet = 1))

        val update = session.completeSet(0L)

        assertEquals(2, update.state.exercise)
        assertEquals(1, update.state.currentSet)
        assertEquals(3, update.state.targetSets)
    }

    @Test
    fun increaseTargetSetsChangesCurrentTargetOnly() {
        val session = WorkoutSession(WorkoutState(currentSet = 1, targetSets = 3, defaultSets = 3))

        val update = session.increaseTargetSets()

        assertEquals(4, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(1, update.state.currentSet)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun resetCurrentTargetToDefaultClampsCurrentSetToKeepStateValid() {
        val session = WorkoutSession(WorkoutState(exercise = 7, currentSet = 7, targetSets = 8, defaultSets = 3))
        session.toggleTimer(1_000L)
        session.refresh(12_500L)

        val update = session.resetCurrentTargetToDefault()

        assertEquals(3, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(7, update.state.exercise)
        assertEquals(3, update.state.currentSet)
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

    @Test(expected = IllegalArgumentException::class)
    fun currentSetCannotExceedTargetSets() {
        WorkoutState(currentSet = 4, targetSets = 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun currentSetCannotStartAtZero() {
        WorkoutState(currentSet = 0)
    }

    @Test
    fun updatedDefaultSetSettingIsUsedByTheFollowingExercise() {
        val session = WorkoutSession(WorkoutState(targetSets = 4, currentSet = 4))
        session.adjustDefaultSets(1)

        val update = session.completeSet(0L)

        assertEquals(4, update.state.defaultSets)
        assertEquals(4, update.state.targetSets)
        assertEquals(2, update.state.exercise)
        assertEquals(1, update.state.currentSet)
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
        val session = WorkoutSession(WorkoutState(exercise = 3, currentSet = 2, targetSets = 5, defaultSets = 4))
        session.toggleTimer(1_000L)

        val update = session.resetTimer()

        assertEquals(3, update.state.exercise)
        assertEquals(2, update.state.currentSet)
        assertEquals(5, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
        assertTrue(update.state.isWorkoutSessionActive)
        assertFalse(update.state.isRunning)
        assertEquals(0L, update.state.elapsedSeconds)
    }

    @Test
    fun mainBackWithElapsedTimerResetsOnlyTimerAndPreservesProgress() {
        val session = WorkoutSession(WorkoutState(
            exercise = 3,
            currentSet = 2,
            targetSets = 5,
            defaultSets = 4,
            restLimitSeconds = 180,
            overdueReminderSeconds = 15
        ))
        session.toggleTimer(0L)
        session.refresh(77_000L)
        session.toggleTimer(77_000L)

        val update = session.handleMainScreenBack(77_000L)

        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertEquals(3, update.state.exercise)
        assertEquals(2, update.state.currentSet)
        assertEquals(5, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
        assertEquals(180, update.state.restLimitSeconds)
        assertEquals(15, update.state.overdueReminderSeconds)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun mainBackWhileRunningResetsAndPausesTimer() {
        val session = WorkoutSession(WorkoutState(
            exercise = 4,
            currentSet = 1,
            targetSets = 5,
            defaultSets = 3,
            restLimitSeconds = 180,
            overdueReminderSeconds = 20
        ))
        session.toggleTimer(1_000L)

        val update = session.handleMainScreenBack(43_000L)

        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertEquals(4, update.state.exercise)
        assertEquals(1, update.state.currentSet)
        assertEquals(5, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(180, update.state.restLimitSeconds)
        assertEquals(20, update.state.overdueReminderSeconds)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun pausingTheTimerKeepsTheWorkoutSessionActive() {
        val session = WorkoutSession()
        session.toggleTimer(0L)

        val update = session.toggleTimer(20_000L)

        assertFalse(update.state.isRunning)
        assertTrue(update.state.isWorkoutSessionActive)
    }

    @Test
    fun mainBackAtPausedZeroEndsSessionAndPreservesSettings() {
        val session = WorkoutSession(
            WorkoutState(exercise = 3, currentSet = 2, targetSets = 5, defaultSets = 4,
                restLimitSeconds = 180, overdueReminderSeconds = 15, isWorkoutSessionActive = true)
        )

        val update = session.handleMainScreenBack(0L)

        assertEquals(1, update.state.currentSet)
        assertEquals(1, update.state.exercise)
        assertEquals(4, update.state.targetSets)
        assertEquals(4, update.state.defaultSets)
        assertEquals(180, update.state.restLimitSeconds)
        assertEquals(15, update.state.overdueReminderSeconds)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertFalse(update.state.isWorkoutSessionActive)
    }

    @Test
    fun fullSessionResetClearsReminderSchedulingButRetainsUserPreferences() {
        val session = WorkoutSession(
            WorkoutState(exercise = 5, currentSet = 2, targetSets = 7, defaultSets = 3,
                restLimitSeconds = 15, overdueReminderSeconds = 5, isWorkoutSessionActive = true)
        )
        session.toggleTimer(0L)
        session.refresh(15_000L)
        session.toggleTimer(15_000L)

        session.handleMainScreenBack(15_000L) // Stage 1: clear the timer.
        val update = session.handleMainScreenBack(15_000L) // Stage 2: end the session.

        assertEquals(1, update.state.exercise)
        assertEquals(1, update.state.currentSet)
        assertEquals(3, update.state.targetSets)
        assertEquals(3, update.state.defaultSets)
        assertEquals(15, update.state.restLimitSeconds)
        assertEquals(5, update.state.overdueReminderSeconds)
        assertEquals(0L, update.state.elapsedSeconds)
        assertFalse(update.state.isRunning)
        assertFalse(update.state.isWorkoutSessionActive)
        assertFalse(session.hasNotifiedRestLimit)
        org.junit.Assert.assertNull(session.nextOverdueReminderAtSeconds)
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
