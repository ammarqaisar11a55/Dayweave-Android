# Privacy

Dayweave keeps everything on your phone unless you turn on sync.

- **No account needed.** Dayweave works fully without one.
- **No cloud unless you choose it.** Without sync nothing is uploaded, and Android's cloud backup is
  turned off for the app.
- **No telemetry, advertising or third-party services.** The Inter typeface ships inside the app.

## Sync

Sync is off until you sign in under Settings → Sync. Dayweave's own server is
`dayweave-server.vercel.app`, run by Dayweave's developer on Vercel, with data in a Supabase Postgres
database in Tokyo. You can enter the address of a server you run instead
([Dayweave-Server](https://github.com/ammarqaisar11a55/Dayweave-Server)). Then:

- Dayweave talks only to that server, over HTTPS, and only to sign in, sync and sign out.
- Your tasks, habits, check-ins, projects, finished focus sessions and preferences are sent to the
  server and stored there, so your other devices can fetch them.
- The server stores your email and a hash of your password. The phone keeps a token for itself
  instead of your password. Backups never contain it.
- Signing out stops syncing and revokes this phone's token. **Delete account** removes everything
  stored on the server. Data on the phone stays either way.

Whoever runs the server can read what it stores. Use a server you trust, or run your own.

## Update checks

Once a day, and when you press **Check now** in Settings, Dayweave downloads a small file called
`latest.json` from this repository's latest GitHub release. If it names a newer version, Dayweave asks
before downloading that version's APK from the same release.

The request carries nothing about you or your data. GitHub sees your IP address and an app identifier
(`Dayweave-Android/<version>`), as it would for any download. Turn off **Check for updates
automatically** in Settings to stop the daily check.

Before installing, Dayweave checks the download's SHA-256 checksum against the one in `latest.json`, and
checks that it's signed with the same key as the installed app. Android shows its own confirmation, and
refuses any update signed with a different key.

## Your data

Your data is in one SQLite database in the app's private storage, which other apps can't read. It isn't
encrypted beyond the protection Android gives every app's storage.

A safety copy of the database is kept before each restore, up to the last five.

Backups and exports are ordinary files saved where you choose. What happens to them after that is up
to you.

## Permissions

| Permission | Why |
| --- | --- |
| Notifications | To show reminders and the focus timer |
| Alarms and reminders | To deliver reminders on the minute instead of a few minutes late |
| Run at startup | To set reminders again after the phone restarts |
| Internet | For the update check above, and for sync once you sign in |
| Install apps | Only to install an update you've agreed to |

## Notifications

Reminders show the task or habit's name, and can be seen on the lock screen unless you've hidden
sensitive notification content in Android's settings.
