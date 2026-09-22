# Tasks — Communa8 App TODO

## Authentication and onboarding — mandatory baseline

- [ ] Hard-wire the production server to `https://app.communa8.org`; normal customers must never see or enter a server address.
- [ ] Start Nextcloud Login Flow v2 with `POST /index.php/login/v2` against the fixed Communa8 server.
- [ ] Keep the complete password / 2FA / authorization experience inside the app.
- [ ] Intercept `communa8://login-complete` internally and poll Login Flow v2 for the app-specific credential.
- [ ] Accept returned credentials only when the server is `app.communa8.org`.
- [ ] Store only this app's server-issued app password in platform-secure storage.
- [ ] Never use an external browser, Custom Tab, or TWA for normal Communa8 onboarding.
- [ ] Never fall back to stock Nextcloud server-selection UI; errors retry the fixed Communa8 server.
- [ ] Brand login/loading/error surfaces with the Communa8 dark-green treatment and infinity/leaf identity.
- [ ] Verify password login and 2FA.
- [ ] Verify logout/re-login and expired/revoked app-password recovery.
- [ ] Verify a clean install with no other Communa8 app installed.
- [ ] Preserve a path for future same-signed cross-app SSO without changing the embedded-login baseline or sharing app passwords.

### Android implementation checklist

- [ ] Use an app-owned WebView for the returned Login Flow v2 login URL.
- [ ] Enable JavaScript + DOM storage only as needed for Nextcloud login.
- [ ] Disable file access, content access, mixed-content downgrade, and third-party cookies.
- [ ] Use the `Communa8 Android` login user agent.
- [ ] Stop/destroy the temporary WebView and clear transient login state when authentication finishes.
