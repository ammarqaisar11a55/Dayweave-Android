# Architecture

```
app (Kotlin, Jetpack Compose)
  │  JSON over JNI: NativeCore.open / rpc / tick
native (Rust, libdayweave.so)
  │  Core::rpc(method, params)
core/crates/dayweave-core   ← the desktop repository, as a Git submodule
  │
SQLite database in the app's private storage
```

## The shared core

`core/` is a submodule of [Dayweave](https://github.com/ammarqaisar11a55/Dayweave). Its
`dayweave-core` crate owns the database, the schema and its migrations, deadlines, recurrence, streaks,
reminders, analytics, backups and validation. The Android app adds no rules of its own, so both apps
always agree.

Everything crosses into the core through one dispatcher, `Core::rpc(method, params)`, as JSON. The
desktop interface uses the same methods, so a method tested there behaves the same here.

`native/` is a small `cdylib` that wraps the core for JNI. Each call returns `{"ok": …}` or
`{"error": {"code", "message"}}`, and a panic becomes an error rather than a crash. Gradle's
`buildRust` task cross-compiles it with the NDK for `arm64-v8a` and `x86_64`.

## The app

- `core/Dayweave.kt` is the only way into the data. Every write bumps a version number that every
  screen observes, so each screen reloads after any change and nothing shows stale data.
- `ui/` holds the Compose screens. `ui/theme` mirrors the desktop design tokens (docs/DESIGN.md in the
  desktop repository) and bundles Inter.
- `notify/` delivers reminders. Android can't keep a background loop running, so the core reports when
  it next has work (`scheduler.nextWake`) and an exact alarm wakes the app then. A WorkManager job every
  15 minutes is the safety net, and boot, time, time zone and permission changes set the alarm again.
  Each wake runs the core's scheduler, which decides what is due and never repeats a reminder.
- `update/` checks `releases/latest/download/latest.json` in this repository, downloads the APK it
  names, checks its SHA-256 and signing certificate, and hands it to Android's `PackageInstaller`.

## Releases

Pushing a tag such as `v0.2.0` runs `.github/workflows/release.yml`. It checks that the tag matches
`dayweave.version` in `gradle.properties`, builds a signed release, and publishes the APK, its checksum
and `latest.json` with the notes from `CHANGELOG.md`.

The workflow needs these repository secrets:

| Secret | Contents |
| --- | --- |
| `DAYWEAVE_KEYSTORE_BASE64` | The release keystore, base64 encoded |
| `DAYWEAVE_KEYSTORE_PASSWORD` | Its password |
| `DAYWEAVE_KEY_ALIAS` | The key's alias |
| `DAYWEAVE_KEY_PASSWORD` | The key's password |

Updates install only over a build signed with the same key, so the keystore must never change or be
lost. Keep a copy somewhere safe outside GitHub.

For a local release build, put the same four values in an untracked `keystore.properties`
(`storeFile`, `storePassword`, `keyAlias`, `keyPassword`).

## Core patches

`core-patches/` holds core changes the app needs before they're published in the Dayweave repository.
`scripts/apply-core-patches.sh` applies them to the submodule, skipping any already applied, and every
workflow runs it after checkout. Once a patch's change is in a published Dayweave commit, move the
submodule to that commit and delete the patch.

## Updating the core

```bash
git -C core fetch origin && git -C core checkout <commit>
```

```bash
git add core
```

Then rebuild. A core with a newer schema migrates the database on first launch, like the desktop app.
