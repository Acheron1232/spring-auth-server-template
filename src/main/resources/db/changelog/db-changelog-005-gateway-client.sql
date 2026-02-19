--liquibase formatted sql

--changeset acheron:1
--comment Seed gateway-client OAuth2 registered client (password is bcrypt of ${GATEWAY_CLIENT_SECRET})

INSERT INTO oauth2_registered_client (
    id,
    client_id,
    client_id_issued_at,
    client_secret,
    client_name,
    client_authentication_methods,
    authorization_grant_types,
    redirect_uris,
    post_logout_redirect_uris,
    scopes,
    client_settings,
    token_settings
)
SELECT
    gen_random_uuid()::varchar,
    'gateway-client',
    NOW(),
    '{bcrypt}REPLACE_WITH_ENCODED_SECRET',
    'Gateway BFF Client',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'http://localhost:8080/login/oauth2/code/messaging-client-oidc',
    'http://localhost:8080/logout',
    'openid,profile,email,message.read',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"@class":"java.util.Collections$UnmodifiableMap","settings.token.reuse-refresh-tokens":false,"settings.token.id-token-signature-algorithm":["org.springframework.security.oauth2.jose.jws.SignatureAlgorithm","RS256"],"settings.token.access-token-time-to-live":["java.time.Duration",300.000000000],"settings.token.access-token-format":{"@class":"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat","value":"self-contained"},"settings.token.refresh-token-time-to-live":["java.time.Duration",1728000.000000000],"settings.token.authorization-code-time-to-live":["java.time.Duration",300.000000000],"settings.token.device-code-time-to-live":["java.time.Duration",300.000000000]}'
WHERE NOT EXISTS (
    SELECT 1 FROM oauth2_registered_client WHERE client_id = 'gateway-client'
);
