<script setup>
import { computed } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const menus = computed(() => {
  const base = [{ path: "/residents", title: "常住人口" }];
  if (authStore.roles.includes("ADMIN")) {
    base.push({ path: "/users", title: "用户管理" });
    base.push({ path: "/audit-logs", title: "操作审计" });
  }
  return base;
});

const activeMenu = computed(() => route.path);

const handleLogout = () => {
  authStore.logout();
  router.replace("/login");
};
</script>

<template>
  <el-container class="layout-wrap">
    <el-aside width="220px" class="sidebar">
      <h3>人口管理后台</h3>
      <el-menu :default-active="activeMenu" router>
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          {{ item.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>当前用户：{{ authStore.userInfo?.username }}</div>
        <div>
          <el-tag type="success">{{ authStore.userInfo?.currentRole }}</el-tag>
          <el-button link type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-wrap {
  min-height: 100vh;
}

.sidebar {
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px 10px;
}

h3 {
  margin: 4px 12px 16px;
  color: #f8fafc;
}

.header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
