# Gym IT AV

First Wear OS timer prototype for a round watch display.

The app manifest identifies this as a watch-only Wear OS app and marks it as standalone because the workout timer does not need a companion phone. Google Play will filter this package for Wear OS devices.

## Workout behavior

- The center starts and pauses the rest timer.
- The timer is reset and paused by **SET OK**; completing the last target set advances to the next exercise.
- **+ SET** increases the current exercise's target. Settings controls the default target for later exercises and a rest limit.
- A dim radial glow is visible only while the timer is running. It is green below the rest limit and red at or beyond the limit.
- System Back and **BACK** reset the timer on the main screen. System Back returns from Settings to the timer.
- Elapsed time is calculated from Android's monotonic elapsed-realtime clock, so display sleep does not pause or skew the logical timer.

## MVVM source layout

- `app/src/main/java/com/ferhat/gymitav/model/WorkoutState.kt` — immutable workout state and derived overtime status.
- `app/src/main/java/com/ferhat/gymitav/viewmodel/GymViewModel.kt` — timer clock and workout actions.
- `app/src/main/java/com/ferhat/gymitav/ui/` — timer and settings composables, including Back routing.
- `app/src/main/java/com/ferhat/gymitav/MainActivity.kt` — application entry point and theme setup.

The namespace, minimum and target SDK, launcher activity, and project structure are retained. Compile SDK is 37 because the starter's existing AndroidX Core 1.19 and Lifecycle 2.11 artifacts require API 37.
