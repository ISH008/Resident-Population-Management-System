# Resident Management Backend

## Run
1. Ensure MySQL has executed `../db/init_v1.sql`.
2. Set profile and env vars.
3. Start:
   - Dev: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
   - Prod: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`

## Environment Profiles
- Base config: `src/main/resources/application.yml`
- Dev config: `src/main/resources/application-dev.yml`
- Prod config: `src/main/resources/application-prod.yml`

## Required Environment Variables (prod)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

## Optional Environment Variables
- `SPRING_PROFILES_ACTIVE` (default: `dev`)
- `SERVER_PORT` (default: `8080`)
- `JWT_EXPIRE_SECONDS` (default: `7200`)
- `JWT_ISSUER`

## API Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Default Account (dev)
- username: `admin`
- password: `123456`

## Notes
- Current auth is JWT + interceptor.
- `admin` password is plain text for development bootstrap only.

## Implemented APIs (v1)
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/switch-role`
- `GET /api/v1/users`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/roles`
- `GET /api/v1/audit-logs`
- `GET /api/v1/residents`
- `GET /api/v1/residents/{id}`
- `POST /api/v1/residents`
- `PUT /api/v1/residents/{id}`
- `DELETE /api/v1/residents/{id}`
- `POST /api/v1/residents/{id}/judge`
- `GET /api/v1/residents/{id}/judge-log`
- `PUT /api/v1/residents/{id}/judge/manual`
