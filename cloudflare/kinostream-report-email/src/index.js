const RECIPIENT = "janczesko12@gmail.com";
const SENDER = "KinoStream <powiadomienia@novydev.pl>";
const ALLOWED_BROWSER_ORIGINS = new Set([
  "https://novydev.pl",
  "https://www.novydev.pl",
]);
const MAX_BODY_BYTES = 10_000;

function jsonResponse(data, status = 200, origin = null, extraHeaders = {}) {
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    ...extraHeaders,
  };

  if (origin && ALLOWED_BROWSER_ORIGINS.has(origin)) {
    headers["Access-Control-Allow-Origin"] = origin;
    headers["Vary"] = "Origin";
  }

  return new Response(JSON.stringify(data), { status, headers });
}

function validText(value, maxLength, allowEmpty = false) {
  return typeof value === "string"
    && value.length <= maxLength
    && (allowEmpty || value.trim().length > 0);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const origin = request.headers.get("Origin");

    if (origin && !ALLOWED_BROWSER_ORIGINS.has(origin)) {
      return jsonResponse({ ok: false, error: "Origin not allowed." }, 403);
    }

    if (request.method === "OPTIONS") {
      if (!origin || !ALLOWED_BROWSER_ORIGINS.has(origin)) {
        return new Response(null, { status: 204 });
      }
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": origin,
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
          "Access-Control-Max-Age": "86400",
          "Vary": "Origin",
        },
      });
    }

    if (url.pathname === "/health" && request.method === "GET") {
      return jsonResponse({ ok: true, service: "kinostream-report-email" }, 200, origin);
    }

    if (url.pathname !== "/api/report") {
      return jsonResponse({ ok: false, error: "Not found." }, 404, origin);
    }

    if (request.method !== "POST") {
      return jsonResponse({ ok: false, error: "Method not allowed." }, 405, origin, {
        "Allow": "POST, OPTIONS",
      });
    }

    if (!env.RESEND_API_KEY) {
      return jsonResponse({ ok: false, error: "Email service is not configured." }, 503, origin);
    }

    if (url.protocol !== "https:") {
      return jsonResponse({ ok: false, error: "HTTPS is required." }, 400, origin);
    }

    // Apply rate limits before parsing the body or calling the email provider.
    // Native Android requests don't send an Origin header; CF-Connecting-IP is set at the edge.
    if (!env.REPORT_RATE_LIMIT || typeof env.REPORT_RATE_LIMIT.limit !== "function") {
      return jsonResponse({ ok: false, error: "Rate limiter is not configured." }, 503, origin);
    }

    const clientIp = request.headers.get("CF-Connecting-IP") || "unknown";
    let rateLimitResult;
    try {
      rateLimitResult = await env.REPORT_RATE_LIMIT.limit({ key: clientIp });
    } catch {
      return jsonResponse({ ok: false, error: "Rate limiter is unavailable." }, 503, origin);
    }

    if (!rateLimitResult?.success) {
      return jsonResponse({ ok: false, error: "Too many reports. Try again later." }, 429, origin, {
        "Retry-After": "60",
      });
    }

    const contentType = request.headers.get("Content-Type") || "";
    if (!contentType.toLowerCase().startsWith("application/json")) {
      return jsonResponse({ ok: false, error: "Content-Type must be application/json." }, 415, origin);
    }

    const reportedLength = Number(request.headers.get("Content-Length") || "0");
    if (reportedLength > MAX_BODY_BYTES) {
      return jsonResponse({ ok: false, error: "Request is too large." }, 413, origin);
    }

    let rawBody;
    try {
      rawBody = await request.text();
      if (new TextEncoder().encode(rawBody).byteLength > MAX_BODY_BYTES) {
        return jsonResponse({ ok: false, error: "Request is too large." }, 413, origin);
      }
    } catch {
      return jsonResponse({ ok: false, error: "Could not read request." }, 400, origin);
    }

    let data;
    try {
      data = JSON.parse(rawBody);
    } catch {
      return jsonResponse({ ok: false, error: "Invalid JSON." }, 400, origin);
    }

    if (!data || typeof data !== "object" || Array.isArray(data)) {
      return jsonResponse({ ok: false, error: "Invalid report." }, 400, origin);
    }

    const allowedKeys = new Set([
      "reportId", "description", "context", "appVersion", "device", "androidVersion",
    ]);
    if (Object.keys(data).some((key) => !allowedKeys.has(key))) {
      return jsonResponse({ ok: false, error: "Unexpected report fields." }, 400, origin);
    }

    if (
      !validText(data.reportId, 128)
      || !/^[A-Za-z0-9_-]+$/.test(data.reportId)
      || !validText(data.description, 3000)
      || !validText(data.context, 500, true)
      || !validText(data.appVersion, 80)
      || !validText(data.device, 200)
      || !validText(data.androidVersion, 80)
    ) {
      return jsonResponse({ ok: false, error: "Invalid report fields." }, 400, origin);
    }

    // Fixed recipient and sender are server-side constants. No addresses supplied by clients
    // are ever used for either end of the email.
    const subject = "[KinoStream] Nowe zgłoszenie błędu";
    const text = [
      "Otrzymano nowe zgłoszenie błędu w KinoStream.",
      "",
      "Opis:",
      data.description.trim(),
      "",
      "Kontekst:",
      data.context.trim() || "(brak)",
      "",
      "Wersja aplikacji: " + data.appVersion.trim(),
      "Urządzenie: " + data.device.trim(),
      "Android: " + data.androidVersion.trim(),
      "ID zgłoszenia Firestore: " + data.reportId,
      "",
      "Zgłoszenie zostało zapisane w bazie KinoStream; ta wiadomość potwierdza przyjęcie zgłoszenia.",
    ].join("\n");

    let resendResponse;
    try {
      resendResponse = await fetch("https://api.resend.com/emails", {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + env.RESEND_API_KEY,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          from: SENDER,
          to: [RECIPIENT],
          subject,
          text,
        }),
      });
    } catch {
      return jsonResponse({ ok: false, error: "Email provider is unavailable." }, 502, origin);
    }

    if (!resendResponse.ok) {
      // Avoid logging the report body or provider response, which can include sensitive details.
      console.error("Resend email request failed with HTTP status", resendResponse.status);
      return jsonResponse({ ok: false, error: "Email provider rejected the request." }, 502, origin);
    }

    let providerId = null;
    try {
      const result = await resendResponse.json();
      providerId = typeof result?.id === "string" ? result.id : null;
    } catch {
      // Provider accepted the request; a missing response ID isn't a delivery failure.
    }

    return jsonResponse({ ok: true, accepted: true, id: providerId }, 200, origin);
  },
};
