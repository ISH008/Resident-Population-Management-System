import ResidentsPage from "../../views/ResidentsPage.vue";
import UsersPage from "../../views/UsersPage.vue";
import AuditLogsPage from "../../views/AuditLogsPage.vue";
import DashboardPage from "../../views/DashboardPage.vue";
import JudgeApplicationsReviewPage from "../../views/JudgeApplicationsReviewPage.vue";

export const adminChildRoutes = [
  {
    path: "/dashboard",
    name: "dashboard",
    component: DashboardPage,
    meta: { roles: ["ADMIN"] }
  },
  {
    path: "/residents",
    name: "residents",
    component: ResidentsPage,
    meta: { roles: ["ADMIN"] }
  },
  {
    path: "/judge-applications-review",
    name: "judgeApplicationsReview",
    component: JudgeApplicationsReviewPage,
    meta: { roles: ["ADMIN"] }
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
];
