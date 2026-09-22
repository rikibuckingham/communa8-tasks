# Communa8 Agent Instructions

This file contains project-level rules for coding agents and future development work.

---

## Communa8 Authentication — Project-Wide Invariant

This repository is part of the Communa8 application family.

**Do not design or add an independent primary login architecture for this app.**

The Communa8 product architecture is:

- Any Communa8 app may be the first app the user installs.
- There is **no mandatory Communa8 Core companion app** that customers must install first.
- Shared authentication code is built into each Communa8 app via the reusable Communa8 auth library.
- Canonical server: `https://app.communa8.org`.
- The first Communa8 app initiates Nextcloud Login Flow v2 using the user's default browser / Android Custom Tab.
- The user's real Communa8 password and 2FA secrets are never stored by Communa8 client apps.
- Login Flow v2 returns an app-specific app password. Each app receives and securely stores its own credential.
- Never copy or share one app password between Communa8 apps.
- Later Communa8 apps reuse the existing browser login session where it is still valid, so the user should not normally need to re-enter username/password/2FA. A per-app authorization/grant step may still be shown.
- If the browser session has expired or been cleared, reauthentication may be required.
- Do not require the official Nextcloud Files app for authentication.
- Existing upstream or app-specific login code is transitional and must not be treated as the final Communa8 architecture.

### Android

- Shared module: `auth-client`, developed in the Communa8 authentication/core source project and embedded into each Android app.
- Use Nextcloud Login Flow v2: `POST /index.php/login/v2`, open the returned login URL in a Custom Tab/default browser, and poll the returned endpoint until the one-time credential is issued.
- Store each app's returned credential in that app's own protected storage using Android Keystore-backed encryption.
- Do not make `AccountManager`, `org.communa8.account`, a broker APK, or `sharedUserId` a required product dependency.
- The Communa8 Core/auth development project may contain internal demo/test applications, but these are not customer prerequisites.

### iOS and other platforms

Use the platform-specific Communa8 shared-auth implementation while preserving the same product behaviour: any Communa8 app can be installed first; authenticate once in the shared browser session; later apps obtain their own revocable credentials without normally asking for the human credentials again.

### Product invariant

**SIGN INTO COMMUNA8 ONCE; CONNECT THE REST WITHOUT RE-ENTERING CREDENTIALS WHILE THE SHARED LOGIN SESSION REMAINS VALID.**

When authentication work is required, integrate with the shared Communa8 auth architecture rather than inventing a local replacement.

