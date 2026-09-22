# Communa8 Agent Instructions

This file contains project-level rules for coding agents and future development work.

---

## Communa8 Authentication — Project-Wide Invariant

This repository is part of the Communa8 application family.

**Normal Communa8 onboarding must use the same embedded login pattern proven in Communa8 Chat and Communa8 Files. Do not replace it with an external browser flow.**

### Canonical production login contract

- Canonical server is hard-wired to `https://app.communa8.org`.
- Normal users do **not** type, choose, or edit a server address during Communa8 onboarding.
- Start Nextcloud Login Flow v2 with `POST https://app.communa8.org/index.php/login/v2`.
- Load the returned `login` URL inside an app-owned WebView.
- Do **not** use `Intent.ACTION_VIEW`, Custom Tabs, Trusted Web Activity (TWA), or a visible external browser for normal Communa8 login.
- The real password and any 2FA challenge remain inside the Nextcloud login page and are never stored by Communa8 client code.
- Intercept the internal completion callback `communa8://login-complete` inside the app.
- Poll the Login Flow v2 endpoint returned by the server and obtain this app's own `server`, `loginName`, and `appPassword`.
- Accept credentials only for `app.communa8.org`.
- Store only this app's app-specific app password using platform-secure storage / the app's established secure account store.
- Never copy another Communa8 app's app password into this app.
- After successful login, clear/destroy the temporary login WebView, cookies, cache/history, and form state as appropriate.

### Android embedded-login reference

The working Communa8 Android pattern is:

1. Create the Login Flow v2 request against the fixed Communa8 server.
2. Display the returned login page in the app's own WebView.
3. Enable JavaScript and DOM storage only as required by the real Nextcloud login page.
4. Disable WebView file access and content access.
5. Never allow mixed-content downgrade.
6. Use the Communa8 login user agent (`Communa8 Android`).
7. Accept first-party cookies; do not enable third-party cookies.
8. Intercept `communa8://login-complete` before Android can hand it to another app/browser.
9. Poll Login Flow v2 immediately after the callback.
10. Feed the returned app password into the app's existing authenticated account/session machinery.
11. Remove the temporary WebView login session after completion.

### Communa8 login appearance

- Login/loading/fallback surfaces must use Communa8 branding rather than stock Nextcloud onboarding branding.
- Use the dark Communa8 green visual treatment and the Communa8 infinity/leaf identity.
- Do not expose the stock blue Nextcloud server-address screen during normal Communa8 onboarding.
- If authentication fails, show a Communa8 error/retry state that retries the fixed server; do not fall back to asking the customer for a server URL.

### Cross-app identity

Cross-app silent SSO is a future enhancement layered on top of this proven embedded-login baseline.

- Do not regress the working embedded flow in order to add SSO.
- Any future broker/SSO design must authenticate same-signed Communa8 apps and mint a new app-specific credential for the requesting app.
- Do not use `sharedUserId`.
- Do not require a separate mandatory Communa8 Core app.
- Do not require the official Nextcloud Files app.

### Product invariant

**FIRST APP OR RECOVERY LOGIN: LOGIN ENTIRELY INSIDE THE COMMUNA8 APP, AGAINST THE FIXED COMMUNA8 SERVER.**

**NO SERVER PICKER. NO EXTERNAL BROWSER. NO CUSTOM TAB. NO TWA.**
