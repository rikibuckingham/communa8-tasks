# Communa8 Agent Instructions

This file contains project-level rules for coding agents and future development work.

---

## Communa8 Authentication — Project-Wide Invariant

This repository is part of the Communa8 application family.

**Do not design or add an independent primary login architecture for this app.**

Communa8 authentication architecture:

- Any Communa8 app may be the first app the user installs.
- There is **no mandatory Communa8 Core companion app**.
- Shared authentication code is embedded into each Communa8 app.
- Canonical server: `https://app.communa8.org`.
- The first Communa8 app authenticates entirely **inside that app** using Nextcloud's in-app Login Flow WebView at `/index.php/login/flow`.
- Do not launch an external browser or Custom Tab for normal Communa8 login.
- The user's real password and 2FA secrets are handled by the Nextcloud login page and are never stored by Communa8 client code.
- The first app receives its own app-specific app password and stores it securely.
- Every authenticated Communa8 Android app exposes the shared Communa8 SSO broker from `auth-client`.
- A newly installed Communa8 app first attempts silent SSO with an already-authenticated Communa8 app.
- Android app-to-app SSO must verify that the caller is signed with the same Communa8 signing certificate before releasing or requesting any credential material.
- The broker requests a **new app-specific credential** for the requesting app; credentials are never copied between apps.
- If no authenticated Communa8 app remains installed, the user signs in again inside whichever Communa8 app is installed next.
- Do not require the official Nextcloud Files app.
- Do not use `sharedUserId`.
- Existing upstream/app-specific login code is transitional only.

### Android

Shared module: `auth-client`.

The production flow is:

1. Look for an authenticated same-signed Communa8 SSO broker.
2. If found, perform silent SSO and obtain this app's own credential.
3. If none is available, present Nextcloud's Login Flow inside an app-owned WebView.
4. Capture the returned `nc://login/...` credential redirect.
5. Store only this app's app password using Android Keystore-backed encryption.
6. Register this authenticated app as an SSO broker for later Communa8 apps.

A Communa8 server-side SSO endpoint may mint per-app credentials for trusted broker requests. Never reuse one app's app password as another app's stored credential.

### Product invariant

**FIRST COMMUNA8 APP: LOGIN INSIDE THE APP.**

**LATER COMMUNA8 APPS: OPEN ALREADY CONNECTED, WITH NO EXTERNAL BROWSER AND NO RE-ENTERED USER CREDENTIALS WHEN AN AUTHENTICATED COMMUNA8 APP IS PRESENT.**

