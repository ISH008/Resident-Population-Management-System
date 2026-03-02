<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const formRef = ref(null);
const form = reactive({
  username: "admin",
  password: "123456"
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};

const handleLogin = async () => {
  await formRef.value.validate();
  loading.value = true;
  try {
    await authStore.login(form);
    router.replace("/residents");
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-wrap">
    <div class="bg-shape shape-a"></div>
    <div class="bg-shape shape-b"></div>
    <el-card class="login-card">
      <h2>常住人口管理系统</h2>
      <p>登录后进入管理后台</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrap {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.bg-shape {
  position: absolute;
  border-radius: 9999px;
  filter: blur(8px);
}

.shape-a {
  width: 420px;
  height: 420px;
  background: rgba(59, 130, 246, 0.24);
  top: -110px;
  right: -100px;
}

.shape-b {
  width: 380px;
  height: 380px;
  background: rgba(16, 185, 129, 0.22);
  bottom: -130px;
  left: -90px;
}

.login-card {
  width: 420px;
  z-index: 1;
}

h2 {
  margin: 0 0 6px;
}

p {
  margin: 0 0 18px;
  color: #6b7280;
}

.submit-btn {
  width: 100%;
}
</style>
