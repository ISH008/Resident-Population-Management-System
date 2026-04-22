import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import MainLayout from "../views/MainLayout.vue";
import { publicRoutes } from "./routes/public";
import { adminChildRoutes } from "./routes/admin";
import { userChildRoutes } from "./routes/user";

const routes = [
  ...publicRoutes,
  {
    path: "/",
    component: MainLayout,
    children: [
      {
        path: "",
        redirect: "/judge-applications"
      },
      ...adminChildRoutes,
      ...userChildRoutes
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
  const currentRole = authStore.userInfo?.currentRole;
  const hasRole = !!currentRole && requiredRoles.includes(currentRole);
  if (!hasRole) {
    return currentRole === "ADMIN" ? "/dashboard" : "/judge-applications";
  }
  return true;
});

export default router;
