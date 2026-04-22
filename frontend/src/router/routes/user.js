import JudgeApplicationsPage from "../../views/JudgeApplicationsPage.vue";
import JudgeProgressPage from "../../views/JudgeProgressPage.vue";
import UserProfilePage from "../../views/UserProfilePage.vue";
import AboutPage from "../../views/AboutPage.vue";

export const userChildRoutes = [
  {
    path: "/judge-applications",
    name: "judgeApplications",
    component: JudgeApplicationsPage,
    meta: { roles: ["USER"] }
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
  }
];
