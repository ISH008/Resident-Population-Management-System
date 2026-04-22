<script setup>
import { computed, reactive, ref } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { changePasswordApi } from "../api/auth";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const pwdDialogVisible = ref(false);
const pwdLoading = ref(false);
const pwdForm = reactive({
  oldPassword: "",
  newPassword: ""
});

const menus = computed(() => {
  const base = [];
  const currentRole = authStore.userInfo?.currentRole;

  if (currentRole === "USER") {
    base.push({ path: "/judge-applications", title: "判定申请" });
    base.push({ path: "/judge-progress", title: "判定进度" });
    base.push({ path: "/profile", title: "我的资料" });
    base.push({ path: "/about", title: "关于" });
  }

  if (currentRole === "ADMIN") {
    base.unshift({ path: "/dashboard", title: "数据看板" });
    base.push({ path: "/residents", title: "常住人口" });
    base.push({ path: "/judge-applications-review", title: "申请处理" });
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

const openChangePassword = () => {
  pwdForm.oldPassword = "";
  pwdForm.newPassword = "";
  pwdDialogVisible.value = true;
};

const submitChangePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.error("请输入旧密码和新密码");
    return;
  }
  if (pwdForm.newPassword.length < 6) {
    ElMessage.error("新密码至少6位");
    return;
  }
  pwdLoading.value = true;
  try {
    await changePasswordApi({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    });
    ElMessage.success("密码修改成功，请重新登录");
    pwdDialogVisible.value = false;
    handleLogout();
  } finally {
    pwdLoading.value = false;
  }
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
          <el-button link type="primary" @click="openChangePassword">修改密码</el-button>
          <el-button link type="danger" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px">
    <el-form label-width="80px">
      <el-form-item label="旧密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="pwdLoading" @click="submitChangePassword">保存</el-button>
    </template>
  </el-dialog>
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
