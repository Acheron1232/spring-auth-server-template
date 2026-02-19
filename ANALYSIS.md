# Spring Auth Server — Full Project Analysis

> Generated after reading every source file, entity, config, migration, gateway config, and docker-compose.
> Stack: **Spring Boot 4.0.2 · Java 25 · Spring Authorization Server (latest) · PostgreSQL · Redis · Spring Cloud Gateway**

---

## 1. Critical Bugs 🔴

### 1.1 `UserApi.getQrCode()` — Hardcoded credentials in production code
```java
// UserApi.java:88
return qrCodeService.generateQrCode("Talent", "aryemfedorov@gmail.com", "K4RJK7LR3FFUSTCG");
```
**Problem:** Hardcoded issuer name, personal email, and TOTP secret. This endpoint is completely broken for any real user — it always returns the same QR code.  
**Fix:** Inject `@AuthenticationPrincipal User user`, read `user.getMfaSecret()` and `user.getEmail()`. The issuer should come from `@Value("${spring.application.name}")`.

---

### 1.2 `UserService.confirmEmail()` — Wrong token type generated
```java
// UserService.java:175
Token token = tokenService.generateToken(user, Token.TokenType.RESET);  // ← RESET, not CONFIRM
```
**Problem:** `confirmEmail()` generates a `RESET` token, but `confirm()` checks for `Token.TokenType.CONFIRM`. Email confirmation will **always fail** with `"Token is not confirm"`.  
**Fix:** Change to `Token.TokenType.CONFIRM`.

---

### 1.3 `UserService.confirm()` — Expiry check is inverted
```java
// UserService.java:190
if (confirmationToken.getExpiredAt().isAfter(Instant.now())) { // valid
```
This is actually correct here, but in `reset()` the same pattern is used — both are fine. ✅ (False alarm — logic is correct.)

---

### 1.4 `UserService.reset()` — Returns plaintext password in HTTP response
```java
// UserService.java:246
return newPassword.get(); // returned as plain text via ResponseEntity.ok(userService.reset(token))
```
**Problem:** The new random password is returned as a plain HTTP response body. This is a security anti-pattern — it should be sent via email only, never returned in the HTTP response.  
**Fix:** Send the new password via email (like `confirmEmail` does), return only `200 OK` with a generic message.

---

### 1.5 `AuthApi.registrationApi()` — No input validation, MFA secret ignored
```java
// AuthApi.java:39-56
String mfa_secret = null;
if (params.getOrDefault("mfa_enabled", "false").equals("true")) {
    mfa_secret = Base32.random(); // generated but never used!
}
User user = User.builder()
    ...
    .mfaEnabled(false)   // always false regardless of param
    .mfaSecret(null)     // always null regardless of generated secret
```
**Problems:**
1. `mfa_secret` is generated but never set on the user.
2. `mfaEnabled` is always `false` regardless of the form param.
3. No `@Valid` validation — raw `Map<String, String>` with no sanitization.
4. No duplicate email/username check before saving.
5. No CSRF protection (the form endpoint is inside the `defaultSecurityFilterChain` which has CSRF enabled, but the registration form must include the CSRF token).

---

### 1.6 `AuthApi.logout()` — Incomplete logout
```java
// AuthApi.java:59-67
@GetMapping("/spa/logout")
public void logout(HttpServletRequest request) {
    HttpSession session = request.getSession(true); // creates session if not exists!
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
}
```
**Problems:**
1. `getSession(true)` **creates** a new session if none exists — should be `getSession(false)`.
2. Does not revoke OAuth2 tokens/authorizations.
3. Returns `void` with no status — should return `200 OK` or redirect.
4. GET for logout violates HTTP semantics (CSRF risk) — should be POST.

---

### 1.7 `OAuth2PublicClientRefreshTokenGenerator` — Fragile iterator usage
```java
// OAuth2PublicClientRefreshTokenGenerator.java:26
context.getRegisteredClient().getClientAuthenticationMethods().iterator().next()
```
**Problem:** Assumes the first authentication method is `NONE`. If a client has multiple methods (e.g., `NONE` + `CLIENT_SECRET_BASIC`), this check will fail silently or incorrectly. The set is not ordered.  
**Fix:** Use `.contains(ClientAuthenticationMethod.NONE)` instead.

---

### 1.8 `TokenVersionCheckingOAuth2AuthorizationService.stampTokenVersion()` — DB query on every token save
```java
// TokenVersionCheckingOAuth2AuthorizationService.java:52-63
private OAuth2Authorization stampTokenVersion(OAuth2Authorization authorization) {
    Object stored = authorization.getAttributes().get(ATTR_TOKEN_VERSION);
    if (stored != null) return authorization; // ← only skips if already stamped
    // else: DB query on every save
    userRepository.findUserByUsername(principalName);
```
**Problem:** Every `save()` call (which happens multiple times per auth flow) triggers a DB lookup if `token_version` is not yet stamped. For `client_credentials` grants (no user), `principalName` is the client ID — `userRepository.findUserByUsername(clientId)` will always return empty, causing a no-op but still hitting the DB every time.  
**Fix:** Short-circuit early for non-user grants (check `authorization.getAuthorizationGrantType()`).

---

### 1.9 `RefreshTokenReuseDetectionErrorHandler` — Double `findByToken` call on reuse
When a stolen/reused refresh token is presented:
1. `TokenVersionCheckingOAuth2AuthorizationService.findByToken()` is called → throws `INVALID_GRANT`.
2. `RefreshTokenReuseDetectionErrorHandler.onAuthenticationFailure()` calls `authorizationService.findByToken()` **again** on the same token.

Since the first call already threw an exception (not found after version mismatch), the second call in the error handler will return `null` and **the authorization will NOT be revoked**. The reuse detection handler only works for the case where the token is genuinely expired/invalid, not for the version-mismatch case.

**Fix:** The `verifyTokenVersion()` should not throw — instead it should return a marker, OR the error handler should use a different revocation strategy (e.g., revoke by `principal_name` from the token payload).

---

### 1.10 `DynamicCorsConfigurationSource` — SQL query returns CSV, not individual URIs
```java
// DynamicCorsConfigurationSource.java:39-48
List<String> redirectUris = jdbcTemplate.queryForList(
    "SELECT redirect_uris FROM oauth2_registered_client", String.class);
for (String csv : redirectUris) {
    for (String uri : csv.split(",")) { // ← assumes comma-separated
```
**Problem:** The `redirect_uris` column in `oauth2_registered_client` stores URIs as a **space-separated** string (Spring Authorization Server default), not comma-separated. Origins will not be parsed correctly.  
**Fix:** Use `csv.split("\\s+")` or query with `string_to_array`.

---

## 2. Security Weaknesses 🟠

### 2.1 Actuator fully exposed in production
```yaml
# application.yaml:91-98
management:
  endpoints:
    web:
      exposure:
        include: "*"   # ALL endpoints exposed
  endpoint:
    health:
      show-details: always
```
This exposes `/actuator/env`, `/actuator/beans`, `/actuator/heapdump`, `/actuator/threaddump`, `/actuator/loggers` etc. in production. The `application-docker.yaml` does not restrict this.  
**Fix:** In `application-docker.yaml`, restrict to `health,info,prometheus` only.

### 2.2 AWS credentials stored as static keys
```java
// AwsConfig.java
StaticCredentialsProvider.create(AwsBasicCredentials.create(publicKey, privateKey))
```
Using long-lived static IAM access keys is a security anti-pattern. Should use IAM roles (EC2/ECS instance profiles) or at minimum AWS Secrets Manager rotation.

### 2.3 `X-Forwarded-For` trusted blindly
```java
// AuthHistoryService.java:34-37
String forwarded = request.getHeader("X-Forwarded-For");
if (forwarded != null && !forwarded.isBlank()) {
    return forwarded.split(",")[0].trim(); // first IP — can be spoofed
```
Without configuring a trusted proxy list, any client can spoof their IP by setting `X-Forwarded-For: 1.2.3.4`. Should configure `server.forward-headers-strategy=FRAMEWORK` in Spring Boot and use `ForwardedHeaderFilter`.

### 2.4 `open-in-view: true` (OSIV anti-pattern)
```yaml
# application.yaml:62
jpa:
  open-in-view: true
```
Keeps DB connections open for the entire HTTP request lifecycle. With virtual threads this is especially wasteful. Set to `false`.

### 2.5 Gateway client secret hardcoded in gateway `application.yaml`
```yaml
# gateway/src/main/resources/application.yaml:18
client-secret: zxczxczxc
```
This is a real secret in a config file committed to git. Should be `${GATEWAY_CLIENT_SECRET}`.

### 2.6 MFA TOTP — No replay protection (time window)
`aerogear-otp-java`'s `Totp.verify()` by default accepts codes within a ±30 second window. There is no tracking of used codes, meaning the same TOTP code can be replayed within the 30-second window for multiple login attempts.  
**Fix:** Store the last used TOTP timestamp/code in Redis and reject reuse within the same window.

### 2.7 Password reset returns new password in HTTP body (see Bug 1.4)
Already covered above — critical security issue.

### 2.8 Registration endpoint has no rate limiting
No brute-force protection on `/registration` or `/login`. Should add rate limiting (e.g., Bucket4j or Spring Cloud Gateway rate limiter).

### 2.9 `springdoc` version `3.0.1` is incompatible with Spring Boot 4.x
Spring Boot 4.x uses Spring Framework 7.x. `springdoc-openapi-starter-webmvc-ui:3.0.1` targets Spring Boot 3.x / Spring Framework 6.x. This will likely cause classpath conflicts or runtime errors.  
**Fix:** Check springdoc releases for Spring Boot 4 compatibility or use the Spring-native OpenAPI support.

### 2.10 `Role` enum — `getAuthority()` returns name without `ROLE_` prefix
```java
// Role.java
public String getAuthority() { return name(); } // returns "USER", "ADMIN"
```
Spring Security's `RoleHierarchyImpl.withDefaultRolePrefix()` expects `ROLE_USER`, `ROLE_ADMIN`. The `RoleConfig` uses `withDefaultRolePrefix()` but the `GrantedAuthority` returns bare names. This means `hasRole("USER")` checks will fail because Spring internally prepends `ROLE_` when using `hasRole()`.  
**Fix:** Either return `"ROLE_" + name()` from `getAuthority()`, or use `hasAuthority("USER")` consistently everywhere, or remove `withDefaultRolePrefix()`.

---

## 3. Design / Architecture Issues 🟡

### 3.1 `SecurityConfig.registeredClientRepository()` — Clients should be in Liquibase migrations
```java
// SecurityConfig.java:199 — TODO comment already there
//TODO move clients to changelogs
```
The `gateway-client` is created in Java code with `if (repository.findByClientId(gatewayClientId) == null)`. This is not idempotent in a multi-instance deployment and mixes infrastructure setup with application config.

### 3.2 `UserService` — Mixed concerns (UserDetailsService + business logic + HTTP responses)
`UserService` implements `UserDetailsService` AND returns `ResponseEntity<>` from business methods. Services should not know about HTTP. Extract HTTP concerns to controllers.

### 3.3 `AuthApi.registrationApi()` — Uses raw `Map<String, String>` instead of a DTO
No validation, no type safety. Should use a `@Valid @RequestBody RegistrationRequest` DTO with proper constraints.

### 3.4 `UserMapper` — Not using MapStruct despite it being a dependency
`UserMapper` is a plain `@Component` with manual mapping. MapStruct is in `build.gradle` but unused here.

### 3.5 `UserMapper.toUserEntity()` — Username set to email
```java
.username(dto.getEmail()) // username = email for OAuth users
```
This is a reasonable default but should be documented and potentially made configurable (some apps want `login` as username for GitHub users).

### 3.6 `ClientService.registerNewClient()` — Publishes two events for same origin
```java
eventPublisher.publishEvent(new ClientRegisteredEvent(url));       // full redirect URI
eventPublisher.publishEvent(new ClientRegisteredEvent(origin));    // extracted origin
```
`DynamicCorsConfigurationSource.onClientRegistered()` calls `addOriginFromUri()` which extracts the origin from the URI anyway. The first event (full URL) will be parsed correctly, the second (already an origin) will also work. Redundant but not broken.

### 3.7 No global exception handler
`AppException` exists but there's no `@ControllerAdvice` / `@RestControllerAdvice` to handle it. Unhandled `AppException` will result in a 500 with a stack trace. `BadCredentialsException` thrown in `UserService.confirm()` will also result in a 500.

### 3.8 `AuthHistory` — No soft delete, no index on `timestamp` alone
`AuthHistory` doesn't extend `AbstractAuditableEntity` (no `created_at`, `updated_at`, `deleted_at`). The `timestamp` field uses `LocalDateTime` (not `Instant`) — inconsistent with the rest of the codebase.

### 3.9 `Token` entity — `TokenStatus` field is never used
`TokenStatus.ACTIVE/INACTIVE` is stored but never checked in `TokenService.getToken()`. Tokens are only validated by expiry and type. The status field is dead code.

### 3.10 `Oauth2AccessTokenCustomizer` — DB query on every token issuance
```java
user = (User) userService.loadUserByUsername(userDetails.getUsername());
```
This hits the DB on every access token and ID token generation. Should use the principal already loaded in the security context (it's already a `User` entity for form login).

### 3.11 `springdoc` Swagger UI exposed in docker profile
```yaml
# application-docker.yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```
Swagger UI should be disabled in production/docker profile.

### 3.12 `application.yaml` — `spring.profiles.active: dev` hardcoded
The active profile is hardcoded in `application.yaml`. This should be set via environment variable (`SPRING_PROFILES_ACTIVE`) only, not in the base config file.

---

## 4. Incomplete / Missing Features 🔵

### 4.1 MFA enrollment flow is broken
- `QrCodeService.generateQrCode(issuer, email, secret)` works correctly.
- But `UserApi.getQrCode()` uses hardcoded values (Bug 1.1).
- There is no endpoint to enable MFA (generate secret → show QR → verify code → save).
- `patchUser` can set `mfaEnabled=true` without verifying the user actually scanned the QR code.

### 4.2 Email verification flow is broken
- `confirmEmail()` generates a RESET token instead of CONFIRM (Bug 1.2).
- The confirmation link points to `http://localhost:8080/user/confirm` but the endpoint is `/user-info/confirm`.
- No re-send confirmation email endpoint.

### 4.3 Password reset flow — URL hardcoded
```java
// UserService.java:214
"http://127.0.0.1:" + "9000" + "/reset_password_token?token=" + token.getToken()
```
Port and host are hardcoded. Should come from `@Value("${server.base-url}")` or similar.

### 4.4 No admin API
No endpoints for admin operations: list users, ban/unban, change roles, view auth history for any user, manage registered clients.

### 4.5 No client registration API
`ClientService.registerNewClient()` exists but there's no REST endpoint to call it. Clients can only be registered by modifying code.

### 4.6 No PKCE enforcement for public clients
Public clients (mobile/SPA) should be required to use PKCE. The `PublicClientRefreshTokenAuthenticationProvider` doesn't enforce PKCE on the initial authorization code flow.

### 4.7 Redis is configured but not used for sessions in auth-server
`spring-boot-starter-data-redis` is in `build.gradle` and Redis is in `docker-compose.yaml`, but there's no `spring-session-data-redis` configuration in the auth server. Redis appears to be unused in the auth server itself (only used in the gateway for sessions).

### 4.8 `AuthHandler` is a dead stub
```java
// AuthHandler.java — incomplete, commented-out gRPC reference
public ResponseEntity<String> resetPassword(String email) { ... }
```
This service is never called and appears to be an abandoned feature.

### 4.9 No JWK key rotation
`JWKSource` is configured (presumably via auto-configuration), but there's no key rotation mechanism. For production, RSA keys should be rotatable without downtime.

### 4.10 No `usage_examples` for most flows
Only partial examples exist. Missing: mobile client flow, public client refresh, MFA flow, federated login, client registration.

### 4.11 `application.yaml` — `spring.origins` property defined but never used
```yaml
# application-dev.yaml:6
spring:
  origins: "http://localhost:5173,..."
```
This property is not read anywhere in the code. `DynamicCorsConfigurationSource` loads from DB only.

### 4.12 No `@Transactional` on `UserService.saveOauthUser()` for the federated identity save
```java
@Transactional
public User saveOauthUser(...) {
    ...
    user = userRepository.save(user);       // TX 1
    federatedIdentityRepository.save(identity); // same TX — OK
```
Actually this is fine — `@Transactional` is present. ✅

---

## 5. Refresh Token Reuse Detection — Detailed Analysis

The implementation uses two complementary mechanisms:

**Mechanism 1: Token Version (Security Stamp)**
- `token_version` UUID on `User` entity.
- Stamped into `OAuth2Authorization.attributes` on first save.
- Verified on every `findByToken(..., REFRESH_TOKEN)` call.
- `revokeAllSessions()` rotates the version → all existing refresh tokens become invalid.
- ✅ Correct concept, but see Bug 1.9 — the error handler cannot revoke the authorization when version mismatch occurs because `findByToken` throws before returning the authorization.

**Mechanism 2: Reuse Detection Error Handler**
- Intercepts `INVALID_GRANT` errors on refresh_token grant.
- Tries to find the authorization by the presented refresh token and revoke it.
- ❌ **Broken for version-mismatch case**: `findByToken` throws `OAuth2AuthenticationException` instead of returning `null`, so the error handler's `findByToken` call also throws (or returns null if the exception is caught upstream), and the authorization is never revoked.
- ✅ Works correctly for the standard Spring AS case where `reuseRefreshTokens=false` and an already-used token is presented (the token is deleted from DB, so `findByToken` returns null — but then there's nothing to revoke anyway).

**Recommendation:** The token version approach is the right one. The error handler should be simplified to just log the event. The real revocation happens via `revokeAllSessions()` which rotates the version. Consider adding an alert/notification when reuse is detected.

---

## 6. Killer Feature Ideas 💡

### 6.1 Admin Dashboard (Thymeleaf or React)
- User management: list, ban, role change, force logout.
- Client management: register/edit/delete OAuth2 clients via UI.
- Auth history viewer per user.
- Active sessions viewer.

### 6.2 Passkey / WebAuthn Support
Spring Security 6.4+ has native WebAuthn support. Adding passkey login would make this a truly modern auth server and a massive differentiator for open source.

### 6.3 Webhook / Event System
Publish events (user registered, login failed, MFA enabled, token revoked) to a configurable webhook URL or message queue. Downstream services can react without polling.

### 6.4 Brute Force Protection with Redis
Track failed login attempts per IP/username in Redis with TTL. Lock accounts or add CAPTCHA after N failures. Integrate with `AuthHistoryService`.

### 6.5 PKCE Enforcement + Device Authorization Grant
Enforce PKCE for all public clients. Add Device Authorization Grant (RFC 8628) for TV/CLI flows.

### 6.6 Suspicious Login Detection
Compare new login IP/UA against `auth_history`. If new country/device → send email alert. Use MaxMind GeoIP (free tier) for location resolution (currently hardcoded as `"-"`).

### 6.7 Client Self-Registration Portal
A protected endpoint (requires `ADMIN` scope) to register new OAuth2 clients dynamically with a UI. Generates client ID/secret, sets redirect URIs, scopes, token TTLs.

### 6.8 Token Introspection Endpoint (already in Spring AS)
Expose and document the `/oauth2/introspect` endpoint for resource servers that cannot validate JWTs locally (e.g., legacy services).

### 6.9 Configurable Password Policy
Min length, complexity, history (last N passwords), expiry. Store as application properties or in DB per-tenant.

### 6.10 Multi-tenancy (Tenant-per-schema or Tenant-per-row)
Allow multiple organizations to use the same auth server instance with isolated user bases and client registrations.

---

## 7. Release Checklist 📋

- [ ] Fix all Critical Bugs (Section 1)
- [ ] Restrict Actuator endpoints in docker/production profile
- [ ] Remove hardcoded gateway client secret from gateway `application.yaml`
- [ ] Add `@RestControllerAdvice` global exception handler
- [ ] Fix `Role.getAuthority()` — add `ROLE_` prefix or align usage
- [ ] Move `gateway-client` registration to Liquibase changelog
- [ ] Add rate limiting on `/login` and `/registration`
- [ ] Add PKCE enforcement for public clients
- [ ] Fix email confirmation flow (token type + URL)
- [ ] Fix password reset — send via email only, not HTTP response
- [ ] Fix MFA enrollment flow end-to-end
- [ ] Disable Swagger UI in production profile
- [ ] Remove `spring.profiles.active: dev` from `application.yaml`
- [ ] Set `open-in-view: false`
- [ ] Configure `ForwardedHeaderFilter` for trusted proxy IP resolution
- [ ] Add TOTP replay protection (Redis-based used-code tracking)
- [ ] Add `@RestControllerAdvice` for `AppException`
- [ ] Add `usage_examples` for all major flows
- [ ] Add integration tests (Testcontainers already configured)
- [ ] Add health check to `auth-server` service in `docker-compose.yaml` (currently missing — gateway `depends_on` has no condition)
- [ ] Review and update `springdoc` version for Spring Boot 4.x compatibility
- [ ] Document JWK key rotation procedure
- [ ] Add `CONTRIBUTING.md` and issue templates for open source
