# Communa8 Tasks

Communa8 Tasks is a Nextcloud-first task application for the Communa8 platform, based on the open-source [Tasks.org](https://github.com/tasks/tasks) project.

The app is focused on direct synchronization with Communa8's Nextcloud server at `https://app.communa8.org`. Communa8 members do not need DAVx⁵, Google Tasks, Microsoft To Do, or another synchronization service for normal Communa8 task use.

## Current status

**Android foundation is working end-to-end.**

Verified on a physical Android device:

- Communa8 Tasks installs as its own app using application ID `org.communa8.tasks`
- Communa8 sign-in is completed inside the app
- Nextcloud issues a revocable app-specific password; the user's normal account password is not stored by Communa8 Tasks
- existing Nextcloud task lists appear in Communa8 Tasks
- a task created in Communa8 Tasks syncs to Nextcloud
- a task created in Nextcloud syncs back to Communa8 Tasks
- the app uses its own OpenTasks provider authority instead of colliding with an installed Tasks.org app
- Communa8 launcher branding is in place

Shared-list read/write behavior is the next collaboration test.

## Architecture

Communa8 Tasks deliberately reuses the mature Tasks.org CalDAV/VTODO implementation rather than introducing a second synchronization engine.

### Communa8 account flow

The Android app uses Nextcloud's one-time Login Flow against:

`https://app.communa8.org/index.php/login/flow`

The real Communa8/Nextcloud login page is displayed in a temporary in-app WebView. Password and 2FA handling remain on the Communa8 server. The app intercepts the final `nc://login/...` callback, accepts credentials only for `https://app.communa8.org`, destroys the temporary WebView state, stores the returned app password using the existing encrypted credential path, and saves the account as `SERVER_NEXTCLOUD`.

The resulting CalDAV root is:

`https://app.communa8.org/remote.php/dav`

### Task synchronization

The existing Tasks.org CalDAV stack handles:

- VTODO discovery and synchronization
- own task lists
- Nextcloud shared task lists
- server ownership and DAV privileges
- read-only shared resources
- recurring tasks and reminders where supported
- local/offline task storage and background synchronization

The internal Kotlin/Java namespace remains `org.tasks` intentionally. The installed Android application ID is `org.communa8.tasks`.

The embedded OpenTasks authority has been changed from `org.tasks.opentasks` to `org.communa8.tasks.opentasks` so Communa8 Tasks can coexist with Tasks.org on the same Android device.

## Branding

The visible app name is **Communa8 Tasks**.

The launcher icon follows the existing Communa8 green/cream infinity-and-leaf visual family and adds a task-list/checklist symbol.

Legacy Tasks.org launcher colour aliases are mapped to the single Communa8 Tasks launcher identity.

## Build

The currently tested Android build is the Generic debug flavor.

From the repository root on Windows:

```powershell
.\gradlew.bat :app:assembleGenericDebug
```

APK output:

```text
app\build\outputs\apk\generic\debug\app-generic-debug.apk
```

To install on a connected Android device:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r .\app\build\outputs\apk\generic\debug\app-generic-debug.apk
```

The development environment used for the Windows build includes JDK 21 and Android Studio.

## Windows build compatibility work

The upstream multiplatform project contains Android, desktop and iOS-related code. Several changes were required to make Android development reliable from Windows:

- retained upstream Gradle root project name `Tasks` because generated Compose resource packages depend on it
- disabled Google Services processing for the Generic flavor, which uses the no-Firebase implementation
- replaced Git locale symlinks that Windows checked out as plain files
- removed unsupported Compose legacy locale aliases `values-in` and `values-iw` while retaining the modern `values-id` and `values-he` resources
- converted remaining Android locale symlinks to real directories
- made Compose compiler metrics/reports opt-in instead of forcing a Windows-invalid output path

Warnings about disabled iOS native targets on Windows are expected and do not prevent the Android Generic build.

## CI policy

GitHub Actions is intentionally kept lightweight.

The default branch uses manual/call-only workflows for normal Android builds rather than running a large automatic matrix on every push or pull request. Release/tag workflows should only be run deliberately.

Local Android Studio/Gradle builds are the normal development loop.

## Next work

Before treating Android as release-ready:

- verify a Nextcloud task list shared from another Communa8 user appears automatically
- verify write access and read-only DAV privileges on shared lists
- polish remaining Tasks.org-specific text/UI and remove providers/features Communa8 does not expose
- review notification/reminder behavior
- review backup/export and privacy-facing text
- produce signed release builds and release metadata

The codebase already contains a Compose Multiplatform desktop target. A Communa8 Tasks Windows application is a planned follow-on once the Android experience is sufficiently polished.

A future Android enhancement may use Nextcloud's official Android Single Sign-On mechanism when a compatible Communa8/Nextcloud Files account already exists on the device. The standalone in-app login remains important so Communa8 Tasks does not depend on another app being installed.

## Upstream and licence

Communa8 Tasks is a fork of [Tasks.org](https://github.com/tasks/tasks).

Tasks.org itself grew from the open-source Astrid Android application. Copyright in upstream code remains with its respective authors and contributors.

This fork is distributed under the **GNU General Public License v3.0**, consistent with the upstream project. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).

Communa8 Tasks is an independent fork and is not affiliated with or endorsed by the Tasks.org project.

## Commercial use

GPLv3 permits commercial distribution. Communa8 may charge for hosted services, accounts, support, membership, or distribution while preserving the GPLv3 rights that apply to this software and its corresponding source code.
