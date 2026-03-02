import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import LoginPage from "../views/LoginPage.vue";
import MainLayout from "../views/MainLayout.vue";
import ResidentsPage from "../views/ResidentsPage.vue";
import UsersPage from "../views/UsersPage.vue";
import AuditLogsPage from "../views/AuditLogsPage.vue";

const routes = [
  {
    path: "/login",
    name: "login",
    component: LoginPage,
    meta: { public: true }
  },
  {
    path: "/",
    component: MainLayout,
    children: [
      {
        path: "",
        redirect: "/residents"
      },
      {
        path: "/residents",
        name: "residents",
        component: ResidentsPage,
        meta: { roles: ["ADMIN", "USER"] }
      },
      {
        path: "/users",
        name: "users",
        component: UsersPage,
        meta: { roles: ["ADMIN"] }
      },
      {
        path: "/audit-logs",
        name: "auditLogs",
        component: AuditLogsPage,
        meta: { roles: ["ADMIN"] }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    return true;
  }
  if (!authStore.token) {
    return "/login";
  }
  if (!authStore.userInfo) {
    try {
      await authStore.fetchMe();
    } catch (error) {
      authStore.logout();
      return "/login";
    }
  }
  const requiredRoles = to.meta.roles || [];
  if (!requiredRoles.length) {
    return true;
  }
  const hasRole = requiredRoles.some((role) => authStore.roles.includes(role));
  if (!hasRole) {
    return "/residents";
  }
  return true;
});

export default router;
