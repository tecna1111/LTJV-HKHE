# Secure Jitsi setup for COSRE

COSRE protects meeting rooms with short-lived JWTs. `meet.jit.si` must not be used because it cannot validate COSRE's private application secret.

## 1. Deploy a Jitsi instance

Use the official `docker-jitsi-meet` release on a server with a DNS name and HTTPS certificate, for example `meet.example.edu`. Follow the official Docker deployment guide; do not expose the service directly from a developer laptop to the Internet.

In Jitsi's `.env`, set these values before starting its Docker Compose stack:

```env
PUBLIC_URL=https://meet.example.edu
ENABLE_AUTH=1
ENABLE_GUESTS=0
AUTH_TYPE=jwt
JWT_APP_ID=cosre
JWT_APP_SECRET=<generate-a-unique-32-plus-character-secret>
JWT_ACCEPTED_ISSUERS=cosre
JWT_ACCEPTED_AUDIENCES=cosre
JWT_ALLOW_EMPTY=0
```

Generate independent, strong passwords for every other Jitsi service variable using the upstream `gen-passwords.sh` script. Do not reuse the JWT secret as an account or database password.

## 2. Configure COSRE backend

Set the same values in the backend runtime environment. Never put the secret in `VITE_*`, source code, or a committed `.env` file.

```env
MEETING_BASE_URL=https://meet.example.edu
MEETING_JITSI_APP_ID=cosre
MEETING_JITSI_SECRET=<the-exact-same-jitsi-jwt-secret>
MEETING_TOKEN_TTL_SECONDS=7200
```

COSRE issues a token only after checking team membership. The JWT uses `iss=cosre`, `aud=cosre`, `sub=meet.example.edu`, and an exact room name. With guest and empty-token access disabled, a copied room URL without a valid token cannot join.

## 3. Verify before demo

1. Start a meeting as a team lecturer/leader; confirm the owner receives a JWT URL.
2. Join from a second team member account in a separate browser profile; test camera, microphone, and screen share.
3. Call the join API as a user outside the team; it must return 403 and no token.
4. Paste the room URL into a signed-out browser; Jitsi must reject it.
