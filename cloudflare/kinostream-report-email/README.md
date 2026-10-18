# KinoStream report-email Worker

Sends an email to `janczesko12@gmail.com` when an Android client has already saved a report to Firestore. Sender: `powiadomienia@novydev.pl`.

No Firebase Extensions or Firebase Blaze plan are needed for this email path. The Worker uses the Resend HTTP API directly. The Resend API key lives only in Cloudflare Worker secrets and must never be stored in the Android app, GitHub source, or Gradle BuildConfig.

## Deploy using Cloudflare

1. Sign in to the [Cloudflare Dashboard](https://dash.cloudflare.com/) and open **Workers & Pages**.
2. Create a Worker named `kinostream-report-email`. If you connect the GitHub repository through Workers Builds, set the root directory to `cloudflare/kinostream-report-email`, leave the build command empty, and use `npx wrangler deploy` as the deploy command. The folder contains `wrangler.jsonc` and `src/index.js`.
3. In **Resend**, add the domain `novydev.pl`. Copy the exact DNS records shown by Resend into Cloudflare DNS, keeping the record names and values exactly as provided. Wait until Resend reports the domain as verified. Do not guess or replace the generated DNS values.
4. In Resend, create a server-side API key with permission to send emails. Copy it once.
5. In the Cloudflare Worker, open **Settings → Variables and Secrets → Add → Secret**. Name it `RESEND_API_KEY` and paste the Resend API key. Deploy the Worker after adding the secret. Do not place the key in a `vars` setting or commit it.
6. Verify the health route by opening `https://kinostream-report-email.<your-workers-subdomain>.workers.dev/health`. It should return JSON with `"ok": true`.
7. The configuration includes a Cloudflare Rate Limiting binding named `REPORT_RATE_LIMIT`, allowing at most 3 requests per minute per connecting IP. If Cloudflare reports that the namespace ID is already in use by a different rate-limit definition, change `namespace_id` in `wrangler.jsonc` to another unique numeric string and redeploy.

## Connect the Android app

The Worker URL is public, not secret. In the KinoStream GitHub repository open **Settings → Secrets and variables → Actions → Variables → New repository variable** and create:

- Name: `REPORT_EMAIL_WORKER_URL`
- Value: `https://kinostream-report-email.<your-workers-subdomain>.workers.dev/api/report`

Use the exact host shown on the Worker Overview page; replace the example placeholder. Start a new GitHub Actions build afterward. The Android app keeps storing the report in Firestore and asks the Worker to send email only after Firestore confirms that the save succeeded.

For a local build, pass the Gradle property `-PREPORT_EMAIL_WORKER_URL=https://.../api/report`.

## Security behavior and limitations

- The recipient and sender are constants in Worker source. User input cannot redirect email to another address.
- The API key is a Cloudflare secret, never an Android client secret.
- The request is size-limited, field-validated, HTTPS-only and rate-limited to 3 submissions per minute per IP.
- The mobile endpoint is public, so rate limiting is abuse reduction, not strong user authentication. Monitor Worker/Resend usage. For a larger public release, add Firebase App Check token verification or another server-verifiable app/user identity check.
- A successful response from Resend means the provider accepted the message, not that it reached the inbox. Verify `novydev.pl` and review Resend logs/bounces.
- Crashlytics alert emails are configured separately in Firebase Console under **Project settings → Alerts**.
