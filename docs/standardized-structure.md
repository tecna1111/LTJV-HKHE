# COSRE standardized structure

## Backend

Backend follows a module-first layered structure:

```text
common/                 shared response and exception types
config/                 security, JWT, CORS and bootstrap configuration
modules/<module>/
  controller/           HTTP boundary
  dto/                  validated requests and safe responses
  entity/               JPA persistence model
  repository/           database access
  service/              business rules
```

The default `dev` profile uses H2 and creates `admin / Admin@123`. Use this account only locally.
For MySQL, set `SPRING_PROFILES_ACTIVE=mysql`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and allowed frontend origins through `CORS_ALLOWED_ORIGINS`.

## Frontend

Frontend follows the same module-first principle:

```text
config/                 Axios and environment configuration
routes/                 application routes and access guards
store/                  global client state
modules/<module>/
  pages/                route-level components
  *Service.js           module API calls
```

Copy `.env.example` to `.env` when the API URL differs from the local default.

## Verification

```text
Backend:  mvn test
Frontend: npm run lint && npm run build
```
