<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  createJudgeApplicationApi,
  downloadJudgeApplicationAttachmentApi,
  judgeApplicationsMineApi,
  listJudgeApplicationAttachmentsApi,
  uploadJudgeApplicationAttachmentApi
} from "../api/judgeApplications";
import { useAuthStore } from "../stores/auth";

const authStore = useAuthStore();
const loading = ref(false);
const submitLoading = ref(false);
const attachmentLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const formRef = ref(null);
const uploadFiles = ref([]);
const attachmentList = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: ""
});

const form = reactive({
  applyReason: "",
  evidenceText: ""
});

const rules = {
  applyReason: [{ required: true, message: "请输入申请原因", trigger: "blur" }]
};

const statusTagType = (status) => {
  if (status === "APPROVED") {
    return "success";
  }
  if (status === "REJECTED") {
    return "danger";
  }
  return "warning";
};

const statusText = (status) => {
  if (status === "APPROVED") {
    return "已通过";
  }
  if (status === "REJECTED") {
    return "已驳回";
  }
  return "待处理";
};

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await judgeApplicationsMineApi(query);
    tableData.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
};

const openCreate = async () => {
  if (!authStore.userInfo?.residentId) {
    ElMessage.warning("当前账号未绑定居民档案，请联系管理员");
    return;
  }
  dialogVisible.value = true;
};

const resetForm = () => {
  form.applyReason = "";
  form.evidenceText = "";
  uploadFiles.value = [];
};

const formatSize = (size) => {
  if (!size && size !== 0) {
    return "-";
  }
  const kb = 1024;
  const mb = kb * 1024;
  if (size >= mb) {
    return `${(size / mb).toFixed(2)} MB`;
  }
  if (size >= kb) {
    return `${(size / kb).toFixed(2)} KB`;
  }
  return `${size} B`;
};

const handleUploadChange = (_, fileList) => {
  uploadFiles.value = fileList;
};

const handleUploadExceed = () => {
  ElMessage.warning("最多上传5个附件");
};

const beforeUpload = (file) => {
  const max = 20 * 1024 * 1024;
  if (file.size > max) {
    ElMessage.error("单个附件不能超过20MB");
    return false;
  }
  return false;
};

const openAttachments = async (row) => {
  attachmentDialogVisible.value = true;
  attachmentLoading.value = true;
  try {
    const { data } = await listJudgeApplicationAttachmentsApi(row.id);
    attachmentList.value = data || [];
  } finally {
    attachmentLoading.value = false;
  }
};

const downloadAttachment = async (row) => {
  const response = await downloadJudgeApplicationAttachmentApi(row.id);
  const blob = new Blob([response.data]);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = row.originalName || "attachment";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};

const submit = async () => {
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const { data: applicationId } = await createJudgeApplicationApi({
      residentId: authStore.userInfo?.residentId,
      applyReason: form.applyReason,
      evidenceText: form.evidenceText || undefined
    });
    const files = uploadFiles.value
      .map((item) => item.raw)
      .filter((item) => !!item);
    for (const file of files) {
      await uploadJudgeApplicationAttachmentApi(applicationId, file);
    }
    ElMessage.success("申请已提交");
    dialogVisible.value = false;
    resetForm();
    query.pageNum = 1;
    await fetchData();
  } finally {
    submitLoading.value = false;
  }
};

onMounted(async () => {
  if (!authStore.userInfo) {
    await authStore.fetchMe();
  }
  await fetchData();
});
</script>

<template>
  <el-card>
    <template #header>
      <div class="head-wrap">
        <span class="head">我的判定申请</span>
        <div>
          <el-tag v-if="authStore.userInfo?.residentId" type="info" style="margin-right: 10px;">
            绑定居民：{{ authStore.userInfo?.residentName || "-" }}（ID:{{ authStore.userInfo?.residentId }}）
          </el-tag>
          <el-button type="primary" @click="openCreate">提交申请</el-button>
        </div>
      </div>
    </template>

    <el-form :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query.pageNum = 1; fetchData()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="residentName" label="居民姓名" width="120" />
      <el-table-column prop="residentId" label="居民ID" width="90" />
      <el-table-column prop="applyReason" label="申请原因" min-width="180" />
      <el-table-column prop="evidenceText" label="补充说明" min-width="160" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="附件" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAttachments(row)">
            查看({{ row.attachmentCount || 0 }})
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="reviewComment" label="审核意见" min-width="160" />
      <el-table-column prop="createdAt" label="提交时间" min-width="170" />
      <el-table-column prop="reviewedAt" label="处理时间" min-width="170" />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        @current-change="fetchData"
        @size-change="
          () => {
            query.pageNum = 1;
            fetchData();
          }
        "
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" title="提交判定申请" width="620px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="居民档案">
        <el-input :model-value="`${authStore.userInfo?.residentName || '-'}（ID:${authStore.userInfo?.residentId || '-'}）`" disabled />
      </el-form-item>
      <el-form-item label="申请原因" prop="applyReason">
        <el-input v-model="form.applyReason" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
      <el-form-item label="补充说明">
        <el-input v-model="form.evidenceText" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      </el-form-item>
      <el-form-item label="添加附件">
        <el-upload
          :auto-upload="false"
          :file-list="uploadFiles"
          :limit="5"
          :on-change="handleUploadChange"
          :on-exceed="handleUploadExceed"
          :before-upload="beforeUpload"
          multiple
        >
          <el-button type="primary" plain>添加附件</el-button>
          <template #tip>
            <div style="margin-top: 6px; color: #6b7280;">
              支持从电脑选择文件，最多5个，单个不超过20MB
            </div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submit">提交</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="attachmentDialogVisible" title="附件列表" width="680px">
    <el-table :data="attachmentList" border v-loading="attachmentLoading" max-height="420">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="originalName" label="文件名" min-width="220" />
      <el-table-column prop="contentType" label="类型" min-width="120" />
      <el-table-column label="大小" width="120">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="downloadAttachment(row)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>
</template>

<style scoped>
.head-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head {
  font-weight: 600;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
