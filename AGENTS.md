# Communa8 Agent Instructions

This file contains project-level rules for coding agents and future development work.


---

## Communa8 Identity — Project-Wide Invariant

This repository is part of the Communa8 application family.

**Do not design or add an independent primary login flow for this app.**

The Communa8 product architecture is:

- One user sign-in across the Communa8 app family.
- Canonical server: `https://app.communa8.org`.
- The user's real Communa8 password must not be stored or duplicated by individual client apps.
- Client apps consume the shared Communa8 identity/authentication layer.
- Individual apps may receive app-specific or scoped credentials/tokens underneath, but this must not require the user to perform a separate primary login for each app.
- Do not introduce a dependency on the official Nextcloud Files app for Communa8 authentication.
- Existing app-specific or upstream login/SSO code is transitional and must not be treated as the final Communa8 architecture.

### Android

The canonical Android identity owner is **Communa8 Core / Identity**.

- Provider package: `org.communa8.account`
- Android account type: `org.communa8.account`
- Shared client library: `auth-client` from the Communa8 Core project.
- Cross-app access is signature-protected.
- Participating production Communa8 Android apps must use the same Communa8 signing certificate.
- Do not use deprecated `sharedUserId`.
- Do not create a second AccountManager authenticator or a competing account type inside an individual app.

### iOS and other platforms

Use the platform-specific Communa8 shared identity implementation. Preserve the same product invariant: **sign in once to Communa8; do not create an app-specific primary account system.**

When authentication work is required, first inspect the Communa8 Core/Identity architecture and integrate with it rather than inventing a local replacement.

