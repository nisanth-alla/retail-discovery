# Production Readiness

## Implemented

- Google OAuth2 sign-in through Spring Security.
- HTTP-only, secure session cookie in the production profile.
- Server-side `/api/auth/me`, `/api/auth/csrf`, and `/api/auth/logout` endpoints.
- Frontend session hydration from the backend rather than localStorage flags.
- CSRF protection for browser-originated state-changing backend requests.
- Server-side vendor authorization using the `AUTH_VENDOR_EMAILS` allowlist.
- Google users receive the normal user role by default.

## Google Cloud Setup

Create an OAuth 2.0 Web application client in Google Cloud Console.

Authorized redirect URIs:

```text
http://localhost:8080/login/oauth2/code/google
https://retail-discovery.onrender.com/login/oauth2/code/google
```

Configure these deployment variables in Render. Store secrets only in the Render environment dashboard:

| Variable | Required | Purpose |
| --- | --- | --- |
| `GOOGLE_CLIENT_ID` | Yes | Google OAuth client ID |
| `GOOGLE_CLIENT_SECRET` | Yes | Google OAuth client secret |
| `FRONTEND_URL` | Yes | URL used after OAuth success |
| `AUTH_VENDOR_EMAILS` | Recommended | Comma-separated Google emails allowed to upload products |
| `GROQ_API_KEY` | For chat | Groq API credential |
| `REPLICATE_API_TOKEN` | For try-on | Replicate credential |

For the current unified deployment, set `FRONTEND_URL` to:

```text
https://retail-discovery.onrender.com
```

If the frontend and backend are split later, set `FRONTEND_URL` and `CORS_ORIGIN` to the Vercel frontend URL, and set `VITE_API_BASE_URL` in Vercel to the backend URL.

## Not Yet Production-Grade

- Email/password registration is intentionally not implemented. Adding it requires a database, password hashing with Argon2 or BCrypt, email verification, password reset, lockout controls, and abuse protection.
- User profiles and role assignments are not persisted. Vendor authorization currently depends on an environment allowlist.
- Session storage is in-process. A multi-instance deployment requires Spring Session backed by Redis or a database.
- `/api/image/search`, chat, style, and try-on endpoints need authentication policy and rate limiting before public launch.
- The frontend currently sends the Replicate token from the browser. Move virtual try-on calls behind the backend before using a real token in production.
- Uploaded files and generated crops use local ephemeral storage. Use object storage with lifecycle cleanup for persistent uploads.
- Add structured error responses, request IDs, metrics, and alerting for upstream Groq, Replicate, and model failures.
- Disable Swagger and OpenAPI endpoints in the production profile unless they are intentionally public.
- Add a content security policy, security headers, dependency scanning, and secret scanning to CI.
- Add integration tests for OAuth callback handling, unauthenticated API access, vendor authorization, CSRF, and logout.
- Render Free is not sufficient for reliable visual inference. Use a memory tier sized for the native ML models and keep concurrency at one unless load testing proves otherwise.
