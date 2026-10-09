# Implementation Plan: Alarm Test & Simulation System

Add a dedicated **Test & Debug Menu / Drawer** enabling users to test the exact alarm wake-up experience with full siren audio, notification, and camera boxing challenge.

## Proposed Changes

### 1. Test & Simulation Engine
- Implement test trigger methods in `AlarmScheduler.kt` and `KnockoutViewModel.kt`:
  - **Instant Trigger**: Immediately starts `AlarmRingingService` with full siren sound and high-priority notification, and transitions to the `FightArenaScreen` to knock out the snooze with boxing punches.
  - **Delayed Trigger (5s / 10s countdown)**: Schedules a real Android `AlarmManager` exact alarm for 5s or 10s in the future. Allows the user to lock the screen, background the app, or put the device on standby to verify that the alarm truly wakes the device and triggers full-screen fight mode.
  - Support setting test punch targets (e.g. quick 5 punches for rapid testing or standard 30 punches).

### 2. Dedicated Test & Debug Drawer / Bottom Sheet
- Add a dedicated **Test & Debug Sheet** accessible via a header button/chip on `HomeScreen`:
  - **Instant Ring & Fight (0s)**: Immediately blasts the alarm siren and opens the fight arena requiring punches.
  - **Delayed Trigger (5s / 10s)**: Shows a countdown banner and schedules exact alarm so the user can lock the device to test the real lock-screen wake-up.
  - **Configurable Test Punch Target**: Quick selector (5, 10, or 30 punches) so testing doesn't always require 30 punches during testing iterations.
  - **Alarm Service Status Badge**: Visual indicator of whether `AlarmRingingService` is currently active.

### 3. Integration with MainActivity & Navigation
- Handle test alarm triggers seamlessly:
  - If triggered within the app: transitions smoothly into `FightArenaScreen` with `AlarmRingingService` running.
  - If triggered via delayed `AlarmReceiver`: wakes screen and launches full-screen intent as designed in production.
  - Ensure test sessions cleanly stop the service upon completion or emergency stop and update the stats accordingly.

### 4. Verification
- Verify build with `compile_applet`.
- Run unit and Robolectric tests (`gradle :app:testDebugUnitTest`).
