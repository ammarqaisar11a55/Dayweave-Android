# Verification status

This records what has actually been run. Last updated 2026-11-21 for version 0.1.0, on an Android 15
(API 35) x86_64 emulator.

## Results

| Suite | Result |
| --- | --- |
| Unit tests (deadline labels, version comparison, weave layout) | 18 passed |
| JNI bridge test on the host | 1 passed |
| Android lint | No issues |
| Clippy with warnings as errors, and rustfmt | Clean |
| Shared core tests, in the desktop repository | 193 passed |

## Verified in the emulator

| Area | How |
| --- | --- |
| The Rust core on Android | Opened the database, then every screen loaded and changed real data |
| First run | Today's empty state and the notification prompt |
| Restore | A desktop-format backup of 219 tasks, 7 habits and 161 focus sessions was chosen in Android's file picker, previewed with its counts, and restored |
| Export | A full backup was saved to Downloads through the file picker and read back |
| Every screen | Today, Tasks, task details, Habits, habit details, Calendar, Focus, Projects, Analytics and Settings, in light and dark themes |
| Habit controls | A quantity habit went from 12 to 13 pages from Today |
| Notification permission | Asked from Today and granted |
| Exact alarms | Granting the permission moved the pending alarm from a window of about 18 minutes to the exact time |
| Focus | A session started, its countdown notification appeared, and with the clock moved forward its end notification arrived on time |
| Task reminder | With the clock moved to the reminder time, the notification arrived with Complete and Snooze |
| Complete on the notification | Pressed in the shade. The task finished without opening the app, and Analytics counted it |
| Updates | A local copy of the release layout offered 0.1.1. The dialog showed, the APK downloaded, and its checksum and signature were accepted |

## Not verified

| Area | Why |
| --- | --- |
| Installing an update | Installing needs the "install unknown apps" permission, a security setting that was left for the owner to grant. The download and its checks were verified up to that step |
| A real GitHub release | No release exists yet, so the check has only run against the local copy |
| The release workflow and signing | Not run yet. It needs the signing secrets |
| A physical phone | Only the emulator was used |
| Restart and time zone changes | The receiver is in place and the alarm is set again on the permission change, but no reboot was tried |
| Snooze | Only Complete was pressed. Snooze runs the core's `tasks.snooze`, which the core tests cover |
| Screen readers | Labels and roles are set, but TalkBack wasn't used |

## Running the suites

```bash
./gradlew testDebugUnitTest lintDebug
```

```bash
cd native && cargo test && cargo clippy --all-targets -- -D warnings
```
