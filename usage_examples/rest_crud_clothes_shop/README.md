# Alt Clothes Shop — Usage Example

A REST CRUD online clothes shop demonstrating integration with the Spring Auth Server template.

## Architecture

```
Browser (Vite/React SPA)
    ↓ OAuth2 Authorization Code + PKCE (public client)
Auth Server (port 9000)
    ↓ JWT access token
Shop Backend (port 8081) — Spring Boot Resource Server
    ↓ JPA
PostgreSQL
```

## Quick Start

### 1. Start the auth server
```bash
# From the root of auth-server
./gradlew bootRun
```

### 2. Register a public client for the shop
```bash
curl -X POST http://localhost:9000/admin/api/clients \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "alt-shop_mobile",
    "redirectUris": ["http://localhost:5173/callback"],
    "scopes": ["openid", "profile", "email", "message.read"],
    "requirePkce": true,
    "requireConsent": false,
    "accessTokenTtlMinutes": 5,
    "refreshTokenTtlDays": 20
  }'
```

### 3. Start the shop backend
```bash
cd app && ./gradlew bootRun
```

### 4. Start the frontend
```bash
cd front && npm install && npm run dev
```

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/products | Public | List products (filterable) |
| GET | /api/products/{id} | Public | Get product |
| POST | /api/products | JWT | Create product |
| PUT | /api/products/{id} | JWT | Update product |
| DELETE | /api/products/{id} | JWT | Delete product |
| GET | /api/categories | Public | List categories |
| POST | /api/categories | JWT | Create category |
| GET | /api/orders | JWT | My orders |
| POST | /api/orders | JWT | Place order |
| PATCH | /api/orders/{id}/cancel | JWT | Cancel order |

## Frontend Features

- Dark alt/non-conformist aesthetic
- Product catalog with category filtering and search
- Shopping cart (localStorage)
- OAuth2 PKCE login flow
- Order history