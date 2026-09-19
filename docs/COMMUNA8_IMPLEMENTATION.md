# Communa8 Tasks implementation record

This document records the Communa8-specific decisions made while establishing the Android foundation.

## Identity

- Repository: `rikibuckingham/communa8-tasks`
- Upstream: `tasks/tasks`
- Android application ID: `org.communa8.tasks`
- Android test ID: `org.communa8.tasks.test`
- AppAuth redirect scheme: `org.communa8.tasks`
- visible app name: `Communa8 Tasks`
- desktop package name: `communa8-tasks`
- internal source namespace retained as `org.tasks`

Retaining the internal namespace avoids a high-risk mass package rename while still giving Communa8 Tasks an independent Android identity.

## Nextcloud integration

Communa8 server:

`https://app.communa8.org`

CalDAV root:

`https://app.communa8.org/remote.php/dav`

Accounts are marked as `CaldavAccount.SERVER_NEXTCLOUD`, preserving Nextcloud-specific DAV/share behavior in the upstream synchronization code.

### Login design

The first prototype used Nextcloud Login Flow v2 and the external system browser. This was replaced after device testing because Communa8 apps should provide a seamless in-app experience.

Current Android flow:

1. Communa8 Tasks opens the real Nextcloud Login Flow page inside a temporary WebView.
2. The user completes normal Communa8 authentication and any 2FA on the server page.
3. Nextcloud redirects to its `nc://login/...` callback.
4. Communa8 Tasks intercepts the callback.
5. The callback is rejected unless the returned server resolves to HTTPS `app.communa8.org`.
6. The temporary WebView, history, cache, cookies and form state are destroyed.
7. The returned app password is saved through the existing encrypted CalDAV account mechanism.
8. The account is saved as a Nextcloud server account.
9. Existing account-insertion sync behavior performs CalDAV discovery/synchronization.

Normal Communa8 account passwords are not stored by the app.

### Future SSO

Nextcloud provides an Android Single Sign-On library that can reuse an account made available by the Nextcloud Files app. This is a possible fast path for the future Communa8 app family.

It is not currently required. Standalone in-app login is retained so Tasks can operate independently.

## Provider isolation

After changing the Android application ID, an initial runtime crash occurred because the embedded OpenTasks DAO still referenced:

`org.tasks.opentasks`

On a device with Tasks.org installed, this caused Communa8 Tasks to open the upstream provider and fail its permission check.

The authority is now:

`org.communa8.tasks.opentasks`

This allows both applications to coexist.

## Verified synchronization

Physical-device testing confirmed:

- existing Nextcloud lists are discovered
- Communa8 Tasks -> Nextcloud task creation works
- Nextcloud -> Communa8 Tasks task creation works

Still to verify:

- shared-list discovery from another Communa8 account
- writable shared lists
- read-only shared-list enforcement
- shared-list ownership/permission presentation

## Windows development fixes

The Android app is being developed from Windows against a Kotlin Multiplatform upstream codebase.

Fixes applied:

- keep `rootProject.name = "Tasks"` because Compose-generated resource namespaces depend on it
- disable Generic Google Services processing so Firebase configuration is not required for `org.communa8.tasks`
- make Compose compiler metrics optional
- replace/resolve resource-directory symlinks that do not survive a normal Windows Git checkout
- remove unsupported Compose legacy locale aliases while preserving modern locale directories

These changes are build-host compatibility fixes; they do not imply that the Windows desktop product is complete.

## Launcher branding

The default launcher references now point to a single Communa8 Tasks icon rather than Tasks.org's selectable launcher colour family.

The chosen artwork uses the established Communa8 dark-green background, cream infinity mark and green leaf, with a checklist/task symbol in the right loop.

For now the finished icon bitmap is used directly rather than putting the already-composed rounded-square artwork through a second adaptive-icon mask.

## CI

Normal development should be local.

Automatic heavy GitHub Actions matrices are disabled on the default branch. The Android build workflow is manual/call-only. This keeps CI usage predictable and leaves capacity for other Communa8 applications.

## Android release-readiness checklist

- [x] independent application ID
- [x] Communa8 visible name
- [x] direct Communa8/Nextcloud account connection
- [x] in-app login and automatic continuation
- [x] encrypted app-password storage through existing account path
- [x] Nextcloud CalDAV discovery
- [x] two-way personal-task synchronization
- [x] independent OpenTasks provider authority
- [x] Communa8 launcher branding
- [ ] shared-list collaboration test
- [ ] read-only shared-list test
- [ ] remove/hide remaining irrelevant upstream providers and commercial UI
- [ ] reminder/notification regression test
- [ ] backup/export/privacy review
- [ ] signed release configuration
- [ ] release/versioning documentation

## iOS and desktop

Communa8 Tasks must ultimately ship on **Android, iPhone/iPad, and Windows**.

The upstream project already contains multiplatform/iOS and desktop foundations. The Communa8 work should preserve as much shared task, sync, model and UI code as practical.

Platform-specific responsibilities should remain isolated:

- Android: Communa8 login WebView/SSO option, Android notifications, encrypted credential integration and packaging
- iOS: Nextcloud Login Flow using an iOS one-time web authentication surface, Keychain-backed credential storage, Apple notification/background APIs and Xcode packaging/signing
- Windows: desktop credential storage, notification integration and native packaging

All platforms should use the same Communa8/Nextcloud data model and direct DAV synchronization rather than Google or third-party synchronization services.

A macOS host with Xcode is required to produce and test the iOS application.
