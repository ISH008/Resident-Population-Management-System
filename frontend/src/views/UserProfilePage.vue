<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { updateProfileApi } from "../api/auth";
import { useAuthStore } from "../stores/auth";
import { useRouter } from "vue-router";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const dialogVisible = ref(false);
const view = reactive({
  username: "",
  realName: "",
  phone: "",
  residentName: "",
  residentId: null,
  currentRole: ""
});
const editForm = reactive({
  username: "",
  realName: "",
  phone: ""
});

const syncView = () => {
  const user = authStore.userInfo || {};
  view.username = user.username || "";
  view.realName = user.realName || "";
  view.phone = user.phone || "";
  view.residentName = user.residentName || "";
  view.residentId = user.residentId || null;
  view.currentRole = user.currentRole || "";
};

const openEdit = () => {
  editForm.username = view.username;
  editForm.realName = view.realName;
  editForm.phone = view.phone;
  dialogVisible.value = true;
};

const saveProfile = async () => {
  if (!editForm.username) {
    ElMessage.error("用户名不能为空");
    return;
  }
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9_]+$/.test(editForm.username)) {
    ElMessage.error("用户名仅支持中文/字母/数字/下划线");
    return;
  }
  const usernameChanged = editForm.username !== view.username;
  loading.value = true;
  try {
    await updateProfileApi({
      username: editForm.username,
      realName: editForm.realName || "",
      phone: editForm.phone || ""
    });
    dialogVisible.value = false;
    if (usernameChanged) {
      ElMessage.success("资料已更新，请重新登录以生效");
      authStore.logout();
      router.replace("/login");
      return;
    }
    await authStore.fetchMe();
    syncView();
    ElMessage.success("资料已更新");
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  if (!authStore.userInfo) {
    await authStore.fetchMe();
  }
  syncView();
});
</script>

<template>
  <el-card>
    <template #header>
      <div class="head">我的资料</div>
    </template>
    <el-form label-width="110px" style="max-width: 560px;">
      <el-form-item label="用户名">
        <el-input v-model="view.username" disabled />
      </el-form-item>
      <el-form-item label="当前角色">
        <el-tag type="success">{{ view.currentRole || "-" }}</el-tag>
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="view.realName" disabled />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="view.phone" disabled />
      </el-form-item>
      <el-form-item label="绑定居民">
        <el-input :model-value="view.residentId ? `${view.residentName || '-'}（ID:${view.residentId}）` : '未绑定'" disabled />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="openEdit">修改资料</el-button>
      </el-form-item>
    </el-form>
  </el-card>

  <el-dialog v-model="dialogVisible" title="修改资料" width="460px">
    <el-form label-width="90px">
      <el-form-item label="用户名">
        <el-input v-model="editForm.username" maxlength="32" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="editForm.realName" maxlength="64" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="editForm.phone" maxlength="11" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消修改</el-button>
      <el-button type="primary" :loading="loading" @click="saveProfile">保存资料</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.head {
  font-weight: 600;
}
</style>
