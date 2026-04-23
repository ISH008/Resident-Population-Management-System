<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { updateProfileApi } from "../api/auth";
import { myMobilityLogsApi } from "../api/mobility";
import { useAuthStore } from "../stores/auth";
import { useRouter } from "vue-router";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const mobilityLoading = ref(false);
const dialogVisible = ref(false);
const mobilityLogs = ref([]);
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

const mobilityTypeText = (type) => (type === "INFLOW" ? "迁入" : "迁出");

const fetchMyMobilityLogs = async () => {
  mobilityLoading.value = true;
  try {
    const { data } = await myMobilityLogsApi();
    mobilityLogs.value = data;
  } finally {
    mobilityLoading.value = false;
  }
};

onMounted(async () => {
  if (!authStore.userInfo) {
    await authStore.fetchMe();
  }
  syncView();
  await fetchMyMobilityLogs();
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

  <el-card class="mt16">
    <template #header>
      <div class="head">我的迁移记录</div>
    </template>
    <div v-if="!view.residentId" class="hint">当前账号未绑定居民档案，暂无迁移记录。</div>
    <el-table v-else :data="mobilityLogs" border v-loading="mobilityLoading">
      <el-table-column prop="changeDate" label="迁移日期" width="120" />
      <el-table-column prop="changeType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.changeType === 'INFLOW' ? 'success' : 'warning'">
            {{ mobilityTypeText(row.changeType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fromRegion" label="迁出地" min-width="140" />
      <el-table-column prop="toRegion" label="迁入地" min-width="140" />
      <el-table-column prop="reason" label="迁移原因" min-width="160" />
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column prop="createdAt" label="登记时间" min-width="170" />
    </el-table>
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

.mt16 {
  margin-top: 16px;
}

.hint {
  color: #6b7280;
  font-size: 13px;
}
</style>
