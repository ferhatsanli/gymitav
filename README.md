# Gym IT AV

Gym IT is a standalone Wear OS rest timer designed for round watch screens, including Samsung Galaxy Watch 7.

The app manifest identifies this as a watch-only Wear OS app and marks it as standalone because the workout timer does not need a companion phone. Google Play will filter this package for Wear OS devices.

## Workout behavior

- The center starts and pauses the rest timer; **SET OK** counts a completed set, resets the timer, and leaves it paused. Completing the final target advances to the next exercise at `0/defaultSets`.
- The annular control ring has tappable sectors: top **+ SET**, right **SETTINGS**, bottom **SET OK**, and left **BACK**. Swiping down, left, up, or right performs those same actions.
- **+ SET** increases only the current exercise target. Settings independently edits exercise number, default sets, rest limit, and overdue reminder interval.
- The timer turns red and emits one haptic at the rest limit. The default overdue reminder repeats every 10 seconds; the user can select OFF or 5–30 seconds in 5-second steps.
- A soft radial glow appears only while running: green before the limit, red at or after it. The timer continues counting after the limit.
- System Back and **BACK** reset the timer on the main screen. Back in Settings returns to the timer without changing workout progress.
- Elapsed time comes from `SystemClock.elapsedRealtime()`. The ViewModel updates the display once per second only while the timer runs, and refreshes from the monotonic clock when the activity resumes. The screen is not held awake.
- Settings uses Wear Compose's scaling lazy list and scroll indicator, which support rotary and touch scrolling.

## MVVM source layout

- `app/src/main/java/com/ferhat/gymitav/model/WorkoutState.kt` and `WorkoutAction.kt` — immutable app state, UI intentions, and notification events.
- `app/src/main/java/com/ferhat/gymitav/model/WorkoutSession.kt` — pure workout transitions, elapsed-time accounting, and reminder scheduling.
- `app/src/main/java/com/ferhat/gymitav/viewmodel/GymViewModel.kt` — state publication, running-only one-second display ticks, and buffered haptic events.
- `app/src/main/java/com/ferhat/gymitav/ui/` — annular timer UI, tap/swipe routing, Wear settings list, Back handling, and vibration side effects.
- `app/src/main/java/com/ferhat/gymitav/MainActivity.kt` — application entry point and theme setup.
- `app/src/test/java/com/ferhat/gymitav/` — deterministic workout-state and gesture-resolution unit tests.

Run the local validation with `./gradlew :app:testDebugUnitTest :app:assembleDebug`.

The namespace, minimum and target SDK, launcher activity, and project structure are retained. Compile SDK is 37 because the starter's existing AndroidX Core 1.19 and Lifecycle 2.11 artifacts require API 37.
