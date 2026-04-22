import LoginPage from "../../views/LoginPage.vue";

export const publicRoutes = [
  {
    path: "/login",
    name: "login",
    component: LoginPage,
    meta: { public: true }
  }
];
