import { defineStore } from "pinia";
import { loginApi, meApi } from "../api/auth";

const TOKEN_KEY = "resident_mgmt_token";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || "",
    userInfo: null
  }),
  getters: {
    roles(state) {
      return state.userInfo?.roles || [];
    }
  },
  actions: {
    async login(payload) {
      const { data } = await loginApi(payload);
      this.token = data.token;
      this.userInfo = data.userInfo;
      localStorage.setItem(TOKEN_KEY, data.token);
    },
    async fetchMe() {
      const { data } = await meApi();
      this.userInfo = data;
    },
    logout() {
      this.token = "";
      this.userInfo = null;
      localStorage.removeItem(TOKEN_KEY);
    }
  }
});
