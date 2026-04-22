<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { registerApi } from "../api/auth";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const registerLoading = ref(false);
const formRef = ref(null);
const registerRef = ref(null);
const registerVisible = ref(false);
const form = reactive({
  username: "admin",
  password: "123456"
});
const registerForm = reactive({
  username: "",
  password: "",
  realName: "",
  phone: ""
});

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }]
};
const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 32, message: "长度4-32个字符", trigger: "blur" },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/, message: "仅支持中文/字母/数字/下划线", trigger: "blur" }
  ],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "至少6位", trigger: "blur" }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: "手机号格式错误", trigger: "blur" }]
};

const handleLogin = async () => {
  await formRef.value.validate();
  loading.value = true;
  try {
    await authStore.login(form);
    const role = authStore.userInfo?.currentRole;
    if (role === "USER") {
      router.replace("/judge-applications");
    } else {
      router.replace("/dashboard");
    }
  } finally {
    loading.value = false;
  }
};

const openRegister = () => {
  registerForm.username = "";
  registerForm.password = "";
  registerForm.realName = "";
  registerForm.phone = "";
  registerVisible.value = true;
};

const handleRegister = async () => {
  await registerRef.value.validate();
  registerLoading.value = true;
  try {
    await registerApi(registerForm);
    ElMessage.success("注册成功，请登录");
    registerVisible.value = false;
    form.username = registerForm.username;
    form.password = "";
  } finally {
    registerLoading.value = false;
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
        <el-button link class="register-btn" @click="openRegister">新用户注册</el-button>
      </el-form>
    </el-card>
  </div>

  <el-dialog v-model="registerVisible" title="用户注册" width="460px">
    <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="registerForm.username" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="registerForm.password" type="password" show-password />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="registerForm.realName" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="registerForm.phone" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="registerVisible = false">取消</el-button>
      <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
    </template>
  </el-dialog>
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

.register-btn {
  margin-top: 8px;
}
</style>
