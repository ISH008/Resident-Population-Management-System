# Resident Management Frontend

## Run
1. Install dependencies:
   - `npm install`
2. Start dev server:
   - `npm run dev`
3. Open:
   - `http://localhost:5173`

## Notes
- Dev proxy forwards `/api` to `http://localhost:8080`.
- Ensure backend is running before frontend login.
- Route guards use `currentRole` from `/api/v1/auth/me`.

## Pages
- Public
- `/login` 登录页
- User
- `/judge-applications` 判定申请（含提交与附件上传）
- `/judge-progress` 判定进度
- `/profile` 我的资料（弹窗修改）
- `/about` 关于系统与判定规则
- Admin
- `/dashboard` 数据看板
- `/residents` 常住人口管理
- `/judge-applications-review` 申请处理
- `/users` 用户管理
- `/audit-logs` 操作审计

## Router Structure
- `src/router/index.js`: router + global guard
- `src/router/routes/public.js`: 公共路由
- `src/router/routes/user.js`: 用户侧路由
- `src/router/routes/admin.js`: 管理侧路由
