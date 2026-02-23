# 🔐 Spring Auth Server Template

[![CI](https://github.com/Acheron1232/spring-auth-server-template/actions/workflows/ci.yml/badge.svg)](https://github.com/Acheron1232/spring-auth-server-template/actions/workflows/ci.yml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE.md)

A **production-ready OAuth 2.1 / OpenID Connect Authorization Server** built on **Spring Boot 4** and **Spring Authorization Server**. Designed as a drop-in template for business projects that need a secure, modern, and extensible authentication backend.

---

## Why This Project?

Setting up a proper OAuth2 Authorization Server is complex. This template gives you:

- **Zero-to-production** auth server with one `docker compose up`
- **Best-practice security** out of the box (PKCE, token rotation, MFA, encrypted keys)
- **Stateful BFF pattern** via the included Spring Cloud Gateway with TokenRelay
- **Federated identity** (Google, GitHub) with JIT user provisioning
- **Fully tested** with TestContainers against real PostgreSQL and Redis

Perfect for teams who need to **add authentication to a new project quickly** or want to **learn how to properly configure Spring Authorization Server** in the latest Spring Boot 4.

---

## Features

| Feature | Description |
|---|---|
| **OAuth 2.1 Compliant** | Authorization Code + PKCE, Refresh Token, Client Credentials |
| **OpenID Connect** | ID Tokens, UserInfo endpoint, OIDC Discovery |
| **MFA / TOTP** | Time-based One-Time Password via Google Authenticator |
| **Federated Login** | Google & GitHub OAuth2 with automatic account linking |
| **Stateful Gateway (BFF)** | Spring Cloud Gateway with TokenRelay — tokens never reach the browser |
| **Public Client Refresh Tokens** | Selective refresh token support for mobile/test clients (`_mobile`, `_test`, `_with_refresh` suffix) |
| **Dynamic CORS** | Origins auto-derived from registered client redirect URIs |
| **JDBC-backed** | All OAuth2 data (clients, authorizations, consents) stored in PostgreSQL |
| **Redis Sessions** | Server-side session management for the gateway |
| **JWT with KMS** | AWS KMS integration for key management |
| **Liquibase Migrations** | Versioned database schema management |
| **Swagger / OpenAPI** | Built-in API documentation |
| **Docker Ready** | Multi-stage Dockerfiles, full docker-compose for the entire stack |
| **CI/CD** | GitHub Actions with tests, build, and deploy pipeline |
| **Virtual Threads** | Java 25 virtual threads enabled for maximum throughput |

---

## Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   SPA / App │────>│  Spring Gateway  │────>│Resource Servers │
│  (Browser)  │     │  (BFF + Session) │     │  (Your APIs)    │
└─────────────┘     └────────┬─────────┘     └─────────────────┘
                             │
                    ┌────────▼─────────┐
                    │   Auth Server    │
                    │  (This project)  │
                    │                  │
                    │  OAuth2 + OIDC   │
                    │  MFA / TOTP      │
                    │  Google/GitHub   │
                    └────────┬─────────┘
                             │
                   ┌─────────┴──────────┐
                   │                    │
              ┌────▼─────┐       ┌──────▼──┐
              │PostgreSQL│       │  Redis  │
              └──────────┘       └─────────┘
```

**For SPA clients:** The gateway acts as a Backend-for-Frontend (BFF). Tokens are stored server-side in Redis sessions. The browser never sees access/refresh tokens — only an HTTP-only session cookie.

**For mobile/native clients:** Public clients with IDs ending in `_mobile`, `_test`, or `_with_refresh` can use refresh tokens directly via PKCE flow.

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 25+ (for local development)

### Run with Docker (recommended)

```bash
# Clone the repository
git clone https://github.com/Acheron1232/spring-auth-server-template.git
cd spring-auth-server-template

# Copy and configure environment variables
cp .env.example .env
# Edit .env with your values

# Start everything
docker compose up -d
```

This starts: **PostgreSQL** + **Redis** (x2) + **Auth Server** (port 9000) + **Gateway** (port 8080)

### Run Locally

```bash
# Start infrastructure
docker compose -f local-infra/docker-compose.yaml up -d

# Run auth server
./gradlew bootRun

# Run gateway (separate terminal)
cd gateway
./gradlew bootRun
```

### Endpoints

| Endpoint | URL |
|---|---|
| Auth Server | `http://localhost:9000` |
| Gateway | `http://localhost:8080` |
| Login Page | `http://localhost:9000/login` |
| OIDC Discovery | `http://localhost:9000/.well-known/openid-configuration` |
| JWKS | `http://localhost:9000/.well-known/jwks.json` |
| Swagger UI | `http://localhost:9000/swagger-ui.html` |

---

## Configuration

### Environment Variables

| Variable | Description | Required |
|---|---|---|
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | For Google login |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | For Google login |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 client ID | For GitHub login |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 client secret | For GitHub login |
| `AUTH_SERVER_CLIENT_ID` | Service-to-service client ID | Yes |
| `AUTH_SERVER_CLIENT_SECRET` | Service-to-service client secret | Yes |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP for email verification & password reset | For email features |
| `AWS_PUBLIC_KEY` / `AWS_PRIVATE_KEY` / `AWS_ARN` | AWS KMS for key encryption | For KMS features |

### Registering New Clients

Clients are stored in PostgreSQL via `JdbcRegisteredClientRepository`. Use the `ClientService` to register new clients programmatically:

```java
@Autowired
private ClientService clientService;

RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
    .clientId("my-spa_mobile")  // _mobile suffix enables refresh tokens
    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
    .redirectUri("myapp://callback")
    .scope(OidcScopes.OPENID)
    .clientSettings(ClientSettings.builder().requireProofKey(true).build())
    .build();

clientService.registerNewClient(client);
```

### Public Client Refresh Token Policy

By default, Spring Authorization Server does **not** issue refresh tokens to public clients (no client secret). This project selectively allows it:

- Clients with ID ending in `_mobile` — for native mobile apps
- Clients with ID ending in `_test` — for integration testing
- Clients with ID ending in `_with_refresh` — explicit opt-in

All other public clients should use the **gateway BFF pattern** where tokens are managed server-side.

---

## MFA (Multi-Factor Authentication)

The auth server supports TOTP-based MFA via Google Authenticator:

1. User enables MFA in their profile
2. Server generates a TOTP secret and QR code
3. User scans QR code with Google Authenticator
4. On subsequent logins, user must provide the 6-digit TOTP code

MFA is enforced at the `DaoAuthenticationProvider` level via `MFADaoAuthProvider`.

---

## Testing

```bash
# Auth server tests (uses TestContainers — Docker must be running)
./gradlew test

# Gateway tests
cd gateway
./gradlew test
```

Tests use **TestContainers** with real PostgreSQL and Redis instances. No mocks for infrastructure.

---

## Project Structure

```
auth-server/
├── src/main/java/com/acheron/authserver/
│   ├── api/              # REST controllers (Auth, User)
│   ├── config/           # Security, CORS, MFA, OAuth2 configuration
│   ├── dto/              # Request/Response DTOs
│   ├── entity/           # JPA entities (User, Token, FederatedIdentity)
│   ├── mapper/           # Entity mappers
│   ├── repository/       # Spring Data JPA repositories
│   └── service/          # Business logic
├── src/main/resources/
│   ├── db/changelog/     # Liquibase migrations
│   ├── templates/        # Thymeleaf login/registration pages
│   └── application.yaml  # Configuration
├── gateway/              # Spring Cloud Gateway (BFF)
├── local-infra/          # Docker compose for local dev (Postgres + Redis)
├── Dockerfile            # Multi-stage production build
├── docker-compose.yaml   # Full stack compose
└── .github/workflows/    # CI/CD pipeline
```

---

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE.md) file for details.

---

## Acknowledgments

- [Spring Authorization Server](https://docs.spring.io/spring-authorization-server/reference/)
- [Spring Boot 4](https://docs.spring.io/spring-boot/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/reference/)
