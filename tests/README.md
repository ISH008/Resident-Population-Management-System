# Test Suite Guide

This repository now includes three categories of tests:

## 1) Unit Tests (Backend)

- Location: `backend/src/test/java/**/service`, `backend/src/test/java/**/security`
- Run:

```bash
cd backend
mvn test
```

## 2) Functional Tests (Backend API Layer)

- Location: `backend/src/test/java/**/controller`
- Style: controller-level functional tests with `MockMvc` + global exception handling.
- Included focus:
  - Auth endpoints (login/register/change-password/profile)
  - Judge application endpoints (mine/admin/reject auth flow)

Run with the same command:

```bash
cd backend
mvn test
```

## 3) Non-Functional Tests

### Load/Performance (k6)

- Location: `tests/non-functional/k6`
- Scripts:
  - `judge-applications-load.js`
  - `auth-profile-soak.js`
  - `residents-admin-load.js`

Examples:

```bash
k6 run tests/non-functional/k6/judge-applications-load.js
k6 run tests/non-functional/k6/auth-profile-soak.js
k6 run tests/non-functional/k6/residents-admin-load.js
```

Optional env vars:
- `BASE_URL` (default `http://localhost:8080`)
- `USERNAME` (default `user01`)
- `PASSWORD` (default `123456`)

### Security/Authz Smoke

- Location: `tests/non-functional/security/authz-smoke.ps1`
- Additional token smoke: `tests/non-functional/security/auth-token-smoke.ps1`
- Run in PowerShell:

```powershell
pwsh ./tests/non-functional/security/authz-smoke.ps1
pwsh ./tests/non-functional/security/auth-token-smoke.ps1
```

This script validates baseline authorization expectations (e.g., USER cannot call admin residents APIs).
