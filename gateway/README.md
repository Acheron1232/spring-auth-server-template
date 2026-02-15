# 🌐 Spring Cloud Gateway (BFF)

A **stateful Backend-for-Frontend (BFF) gateway** built on **Spring Cloud Gateway** and **Spring Boot 4**. Designed to work with the [Spring Auth Server Template](../README.md) to provide a secure token relay pattern for SPA clients.

---

## Purpose

Modern SPA applications should **never store access or refresh tokens in the browser** (localStorage, sessionStorage, or cookies accessible to JavaScript). This gateway solves that problem by:

1. **Handling the OAuth2 login flow** on behalf of the SPA
2. **Storing tokens server-side** in Redis sessions
3. **Relaying tokens** to backend resource servers transparently via the `TokenRelay` filter
4. **Exposing only an HTTP-only session cookie** to the browser

This is the **BFF (Backend-for-Frontend) pattern** recommended by Spring Security and OAuth2 security best practices.

---

## Architecture

```
┌──────────┐   session cookie   ┌──────────────┐   Bearer token   ┌─────────────────┐
│   SPA    │ ─────────────────> │   Gateway    │ ───────────────> │ Resource Server │
│ (React)  │ <───────────────── │  (This app)  │ <─────────────── │   (Your API)    │
└──────────┘                    └──────┬───────┘                  └─────────────────┘
                                       │
                                       │ OAuth2 flow
                                       ▼
                                ┌──────────────┐
                                │  Auth Server  │
                                └──────┬───────┘
                                       │
                                ┌──────▼───────┐
                                │    Redis     │
                                │  (Sessions)  │
                                └──────────────┘
```

---

## Features

| Feature | Description |
|---|---|
| **TokenRelay** | Automatically attaches access tokens to proxied requests |
| **Redis Sessions** | Server-side session storage with custom Jackson serialization |
| **CSRF Protection** | Cookie-based CSRF with SameSite configuration |
| **CORS** | Configurable cross-origin support for SPA development |
| **Profile-aware** | Dev mode uses `SameSite=None` for cross-origin cookies |
| **Docker Ready** | Multi-stage Dockerfile included |
| **Tested** | Integration tests with TestContainers (Redis) |

---

## Quick Start

### With Docker Compose (full stack)

From the root project directory:

```bash
docker compose up -d
```

The gateway will be available at `http://localhost:8080`.

### Local Development

```bash
# Ensure Redis is running on port 6380
# Ensure Auth Server is running on port 9000

cd gateway
./gradlew bootRun
```

---

## Configuration

### Key Properties (`application.yaml`)

```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          auth-server:
            issuer-uri: http://localhost:9000    # Auth server URL
        registration:
          messaging-client-oidc:
            client-id: gateway-client
            client-secret: zxczxczxc            # Must match auth server
            scope: openid, profile, message.read

  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: resource-service
              uri: http://localhost:8081         # Your API
              predicates:
                - Path=/api/**
              filters:
                - TokenRelay=

  data:
    redis:
      host: localhost
      port: 6380                                # Separate Redis for sessions
```

### Route Configuration

| Route | Target | Purpose |
|---|---|---|
| `/login/**`, `/oauth2/**`, `/.well-known/**` | Auth Server | OAuth2 flow passthrough |
| `/api/**` | Resource Server | API with TokenRelay |
| `/**` | SPA | Catch-all redirect to frontend |

### Docker Environment Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host for sessions |
| `SPRING_DATA_REDIS_PORT` | `6380` | Redis port |
| `SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_AUTH_SERVER_ISSUER_URI` | `http://localhost:9000` | Auth server issuer URI |

---

## How Token Relay Works

1. User visits `http://localhost:8080/api/data`
2. Gateway checks for valid session → no session → redirects to auth server login
3. User authenticates at auth server → authorization code returned to gateway
4. Gateway exchanges code for tokens → stores tokens in Redis session
5. Gateway proxies request to resource server with `Authorization: Bearer <access_token>`
6. Resource server validates token and returns data
7. Browser only ever sees the session cookie — **never the tokens**

---

## Session Serialization

The gateway uses a custom Redis serialization configuration (`RedisConfig`) to properly handle:

- Spring Security OAuth2 types (`OAuth2AuthorizedClient`, `OAuth2AuthorizationRequest`)
- Jackson polymorphic type handling for session data
- Custom mixins for proper JSON serialization

---

## Testing

```bash
cd gateway
./gradlew test
```

Tests use **TestContainers** with a real Redis instance.

---

## Project Structure

```
gateway/
├── src/main/java/com/acheron/gateway/
│   ├── GatewayApplication.java
│   └── config/
│       ├── SecurityConfig.java    # WebFlux security + CSRF
│       └── RedisConfig.java       # Session serialization
├── src/main/resources/
│   └── application.yaml           # Routes, OAuth2, Redis config
├── src/test/                      # Integration tests
├── Dockerfile                     # Multi-stage build
└── build.gradle
```
