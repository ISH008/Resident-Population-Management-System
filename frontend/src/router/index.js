import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../stores/auth";
import LoginPage from "../views/LoginPage.vue";
import MainLayout from "../views/MainLayout.vue";
import ResidentsPage from "../views/ResidentsPage.vue";
import UsersPage from "../views/UsersPage.vue";
import AuditLogsPage from "../views/AuditLogsPage.vue";
import DashboardPage from "../views/DashboardPage.vue";
import JudgeApplicationsPage from "../views/JudgeApplicationsPage.vue";
import JudgeApplicationsReviewPage from "../views/JudgeApplicationsReviewPage.vue";
import JudgeProgressPage from "../views/JudgeProgressPage.vue";
import UserProfilePage from "../views/UserProfilePage.vue";
import AboutPage from "../views/AboutPage.vue";

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
        redirect: "/judge-applications"
      },
      {
        path: "/residents",
        name: "residents",
        component: ResidentsPage,
        meta: { roles: ["ADMIN"] }
      },
      {
        path: "/dashboard",
        name: "dashboard",
        component: DashboardPage,
        meta: { roles: ["ADMIN"] }
      },
      {
        path: "/judge-applications",
        name: "judgeApplications",
        component: JudgeApplicationsPage,
        meta: { roles: ["USER"] }
      },
      {
        path: "/judge-applications-review",
        name: "judgeApplicationsReview",
        component: JudgeApplicationsReviewPage,
        meta: { roles: ["ADMIN"] }
      },
      {
        path: "/judge-progress",
        name: "judgeProgress",
        component: JudgeProgressPage,
        meta: { roles: ["USER"] }
      },
      {
        path: "/profile",
        name: "profile",
        component: UserProfilePage,
        meta: { roles: ["USER"] }
      },
      {
        path: "/about",
        name: "about",
        component: AboutPage,
        meta: { roles: ["USER"] }
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
  const currentRole = authStore.userInfo?.currentRole;
  const hasRole = !!currentRole && requiredRoles.includes(currentRole);
  if (!hasRole) {
    return currentRole === "ADMIN" ? "/dashboard" : "/judge-applications";
  }
  return true;
});

export default router;
