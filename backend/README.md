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
- `APP_STORAGE_BASE_DIR` (default: `${java.io.tmpdir}/resident-mgmt`, used for judge-application attachments)

## API Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Default Account (dev)
- username: `admin`
- password: `123456`

## Notes
- Current auth is JWT + interceptor.
- `admin` seed password uses BCrypt hash in DB initialization.
- 申请附件默认保存到 `APP_STORAGE_BASE_DIR/judge-applications/yyyyMMdd`。

## Implemented APIs (v1)
- Auth
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register` (only user register)
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/switch-role`
- `POST /api/v1/auth/change-password`
- `POST /api/v1/auth/profile`
- User & Role
- `GET /api/v1/users`
- `POST /api/v1/users`
- `PUT /api/v1/users/{id}`
- `DELETE /api/v1/users/{id}`
- `GET /api/v1/roles`
- Resident
- `GET /api/v1/residents`
- `GET /api/v1/residents/{id}`
- `POST /api/v1/residents`
- `PUT /api/v1/residents/{id}`
- `DELETE /api/v1/residents/{id}`
- `POST /api/v1/residents/{id}/judge`
- `GET /api/v1/residents/{id}/judge-log`
- `PUT /api/v1/residents/{id}/judge/manual`
- Judge Application
- `POST /api/v1/judge-applications`
- `GET /api/v1/judge-applications/mine`
- `GET /api/v1/judge-applications` (admin)
- `PUT /api/v1/judge-applications/{id}/approve` (admin)
- `PUT /api/v1/judge-applications/{id}/reject` (admin)
- `POST /api/v1/judge-applications/{id}/attachments`
- `GET /api/v1/judge-applications/{id}/attachments`
- `GET /api/v1/judge-applications/attachments/{attachmentId}/download`
- Audit
- `GET /api/v1/audit-logs`
