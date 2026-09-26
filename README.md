# Gym IT AV

Gym IT is a standalone Wear OS rest timer designed for round watch screens, including Samsung Galaxy Watch 7.

The app manifest identifies this as a watch-only Wear OS app and marks it as standalone because the workout timer does not need a companion phone. Google Play will filter this package for Wear OS devices.

## Workout behavior

- The center starts and pauses the rest timer; **SET OK** advances to the next current set, resets the timer, and leaves it paused. The final target remains visible until **SET OK** is pressed again, then the next exercise starts at `1/defaultSets`.
- The annular dark-glass control ring has tappable sectors: top **+ SET**, right **SETTINGS**, bottom **SET OK**, and left **RESET**. Swiping down, left, up, or right performs those same actions. **+ SET**, **SET OK**, and **RESET** briefly pulse a green LED arc at the corresponding screen edge. Opening Settings by tap or swipe has no green edge pulse.
- **+ SET** increases only the current exercise target. Settings independently edits exercise number, default sets, rest limit, and overdue reminder interval.
- The timer turns red and emits one haptic at the rest limit. The default overdue reminder repeats every 10 seconds; the user can select OFF or 5–30 seconds in 5-second steps. A foreground service owns these haptics independently of Compose; while the timer runs it waits for the next reminder boundary without polling. It uses a partial CPU wake lock (never a display wake lock) so reminders remain timely while the display sleeps.
- While running, four steady green inner-edge arcs follow the control sectors, with the black gaps between sectors left open. They turn red at the rest limit and disappear when paused. There is no center radial glow or continuous light animation. The timer continues counting after the limit.
- System Back and **RESET** first reset a non-zero/running rest timer. If it is already paused at `00:00`, Reset ends the workout session: exercise and current set become 1, current target returns to `defaultSets`, and timer/reminder state resets. User preferences are preserved. System Back in Settings remains navigation-only.
- The workout session becomes active on the first timer start, **+ SET**, or **SET OK**. One foreground service owns both the rest haptics and a Wear OS `OngoingActivity`, so the same low-priority notification provides the system return affordance during running, pause, Settings, Set OK, and timer-only Back. Its status changes on workout actions, not every timer tick. Tapping it targets the existing single-top `MainActivity` task.
- At the first session action, Gym IT requests notification permission in context because Wear OS needs it to publish the ongoing activity. If notification access is denied, the workout and haptics still proceed; the app tells the user that watch-face return needs notifications enabled. Full-session Back stops the foreground service and removes the Ongoing Activity.
- Elapsed time comes from `SystemClock.elapsedRealtime()`. The ViewModel updates the display once per second only while the timer runs, and refreshes from the monotonic clock when the activity resumes. The Activity does not finish itself or hold the screen awake; wake-and-return behavior still needs verification on a Wear OS device because this workspace has no Wear OS emulator/device.
- Settings uses Wear Compose's scaling lazy list and scroll indicator, which support rotary and touch scrolling.

## MVVM source layout

- `app/src/main/java/com/ferhat/gymitav/model/WorkoutState.kt` and `WorkoutAction.kt` — immutable app state and UI intentions.
- `app/src/main/java/com/ferhat/gymitav/model/WorkoutSession.kt` — pure workout transitions, elapsed-time accounting, and notification progress.
- `app/src/main/java/com/ferhat/gymitav/model/RestReminderPlan.kt` — pure, unit-testable calculation of the next reminder boundary.
- `app/src/main/java/com/ferhat/gymitav/viewmodel/GymViewModel.kt` — state publication, display ticks, focused Settings state, workout business actions, and synchronization with the background runner.
- `app/src/main/java/com/ferhat/gymitav/background/RestReminderService.kt` — shared foreground workout notification/Ongoing Activity and event-driven haptic execution while the UI is inactive.
- `app/src/main/java/com/ferhat/gymitav/ui/` — annular timer UI, tap/swipe routing, transient edge-pulse animation, Wear settings list, and Back handling.
- `app/src/main/java/com/ferhat/gymitav/MainActivity.kt` — application entry point and theme setup.
- `app/src/test/java/com/ferhat/gymitav/` — deterministic workout-state and gesture-resolution unit tests.

Run the local validation with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

The rest timer service is declared as Android's `specialUse` foreground-service type because it runs only for the explicitly started workout session and no standard media/location/sensor type describes the ongoing workout and timed rest haptics. It keeps a foreground notification while a session is active, including while paused, but holds a partial CPU wake lock only while a rest reminder is pending. Pausing cancels the reminder wait and releases the wake lock without ending the Ongoing Activity. Ending the session removes the notification/service. The display is never kept awake.

The namespace, minimum and target SDK, launcher activity, and project structure are retained. Compile SDK is 37 because the starter's existing AndroidX Core 1.19 and Lifecycle 2.11 artifacts require API 37.
