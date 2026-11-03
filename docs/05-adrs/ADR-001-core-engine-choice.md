# ADR-001: Core Network Engine & Token Interception Strategy

- **Status**: Accepted
- **Deciders**: Lead Software Architect, Engineering Team
- **Date**: 2026-11-03

---

## 1. Context & Problem Statement

**BBSBEC Attend** must exchange academic data, attendance records, exam marks, and notifications with the college ERP platform. The college ERP does not provide an official public mobile SDK or GraphQL endpoint. Instead, it exposes REST endpoints requiring custom headers (`X-College-Id: 1399`) and short-lived JWT Bearer authentication tokens.

The engineering challenge was selecting the core network transport and session management strategy:
1. Should we embed a hidden `WebView` and scrape DOM elements?
2. Should we use raw `HttpURLConnection` to minimize APK size?
3. Should we adopt `Retrofit 2` + `OkHttp 3/4` with an automated `Authenticator` interceptor?

---

## 2. Decision Drivers

- **Zero Session Friction**: Students should not be repeatedly prompted to log in when access tokens expire.
- **Strict Typing & Contract Safety**: Network payloads must map reliably to compile-time verified Java objects.
- **Zero Third-Party Data Leakage**: No intermediary proxy servers or third-party cloud bridges.
- **Battery & Connection Efficiency**: Connection pooling, HTTP/2 multiplexing, and robust timeout handling.
- **Maintainability**: Clear separation between API declarations, deserialization, and business logic.

---

## 3. Considered Options

### Option A: Headless WebView DOM Scraper
- **Approach**: Launch a hidden `WebView` in the background, load `erp.bbsbec.edu.in`, inject JavaScript to simulate form submission, and extract HTML tables.
- **Drawbacks**: Fragile against minor HTML/CSS layout changes; high RAM footprint (30–60MB per WebView process); prone to background execution kills on Android 12+; impossible to run reliably in 30-minute background alarm receivers.

### Option B: Raw `HttpURLConnection`
- **Approach**: Use Android standard library HTTP classes with manual JSON parsing.
- **Drawbacks**: Verbose boilerplate; lack of built-in connection pooling or HTTP/2 multiplexing; complex manual thread management and token retry loops.

### Option C: Retrofit 2 + OkHttp 4 with Custom Authenticator (Chosen)
- **Approach**: Use Square's `Retrofit 2.11.0` declarative interface on top of `OkHttp 4.12.0`, configuring an `authHeaderInterceptor` and a `tokenAuthenticator` that executes synchronous re-authentication (`reloginSync()`) upon receiving HTTP 401.

---

## 4. Decision Outcome

We selected **Option C: Retrofit 2 + OkHttp 4**.

### Architecture Implementation Details
1. **Interface Contract**: All ERP endpoints are statically declared in `BbsbecApiService.java`.
2. **Transparent Token Recovery**: When a JWT token expires, OkHttp's `Authenticator` automatically intercepts the HTTP 401 response, calls `NetworkClient.reloginSync()` on a background thread using saved credentials, updates `TokenManager`, and replays the original request with the new Bearer token.
3. **Resilience**: A maximum retry counter (`responseCount(response) >= 2`) prevents infinite loops if credentials are invalid.

---

## 5. Consequences & Trade-offs

### Positive Consequences
- **Silent Token Renewal**: Students experience uninterrupted usage without unexpected logout prompts.
- **Compile-Time Contract Enforcement**: Endpoints, query parameters, and body payloads are statically checked by javac.
- **Diagnostic Visibility**: `HttpLoggingInterceptor` enables detailed inspection of raw headers and JSON during development.

### Negative / Mitigated Consequences
- **Library Footprint**: Adds ~2.5MB to uncompressed APK size (mitigated through Proguard/R8 shrinking).
- **Credential Storage Responsibility**: Requires storing user credentials securely in private `SharedPreferences` to allow background re-login.
