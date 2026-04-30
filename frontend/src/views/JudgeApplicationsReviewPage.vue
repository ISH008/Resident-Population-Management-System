<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  approveJudgeApplicationApi,
  downloadJudgeApplicationAttachmentApi,
  judgeApplicationsPageApi,
  listJudgeApplicationAttachmentsApi,
  rejectJudgeApplicationApi
} from "../api/judgeApplications";

const loading = ref(false);
const approveLoading = ref(false);
const rejectLoading = ref(false);
const attachmentLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const approveDialogVisible = ref(false);
const rejectDialogVisible = ref(false);
const attachmentDialogVisible = ref(false);
const currentRow = ref(null);
const attachmentList = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: "PENDING",
  residentName: "",
  applicantUsername: ""
});

const approveForm = reactive({
  approveMode: "AUTO",
  manualStatus: "PENDING",
  manualReason: "",
  reviewComment: ""
});

const rejectForm = reactive({
  reviewComment: ""
});

const parseDate = (value) => {
  if (!value) {
    return null;
  }
  const normalized = String(value).includes("T") ? String(value) : String(value).replace(" ", "T");
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
};

const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate());

const calcPendingDays = (createdAt) => {
  const created = parseDate(createdAt);
  if (!created) {
    return 0;
  }
  const today = startOfDay(new Date());
  const createdDay = startOfDay(created);
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.max(0, Math.floor((today.getTime() - createdDay.getTime()) / msPerDay));
};

const enrichRow = (row) => {
  const pendingDays = row.status === "PENDING" ? calcPendingDays(row.createdAt) : 0;
  return {
    ...row,
    pendingDays,
    isOverdue: row.status === "PENDING" && pendingDays > 7
  };
};

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await judgeApplicationsPageApi(query);
    tableData.value = (data.records || []).map(enrichRow);
    total.value = data.total;
  } finally {
    loading.value = false;
  }
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

const tableRowClassName = ({ row }) => {
  if (row.isOverdue) {
    return "overdue-row";
  }
  return "";
};

const openApprove = (row) => {
  currentRow.value = row;
  approveForm.approveMode = "AUTO";
  approveForm.manualStatus = "PENDING";
  approveForm.manualReason = "";
  approveForm.reviewComment = "";
  approveDialogVisible.value = true;
};

const submitApprove = async () => {
  if (!currentRow.value) {
    return;
  }
  if (approveForm.approveMode === "MANUAL" && !approveForm.manualStatus) {
    ElMessage.error("请选择人工判定状态");
    return;
  }
  approveLoading.value = true;
  try {
    await approveJudgeApplicationApi(currentRow.value.id, {
      approveMode: approveForm.approveMode,
      manualStatus: approveForm.approveMode === "MANUAL" ? approveForm.manualStatus : undefined,
      manualReason: approveForm.approveMode === "MANUAL" ? approveForm.manualReason || undefined : undefined,
      reviewComment: approveForm.reviewComment || undefined
    });
    ElMessage.success("审批通过并已执行判定");
    approveDialogVisible.value = false;
    await fetchData();
  } finally {
    approveLoading.value = false;
  }
};

const openReject = (row) => {
  currentRow.value = row;
  rejectForm.reviewComment = "";
  rejectDialogVisible.value = true;
};

const submitReject = async () => {
  if (!currentRow.value) {
    return;
  }
  if (!rejectForm.reviewComment) {
    ElMessage.error("请输入驳回原因");
    return;
  }
  rejectLoading.value = true;
  try {
    await rejectJudgeApplicationApi(currentRow.value.id, {
      reviewComment: rejectForm.reviewComment
    });
    ElMessage.success("已驳回申请");
    rejectDialogVisible.value = false;
    await fetchData();
  } finally {
    rejectLoading.value = false;
  }
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

onMounted(fetchData);
</script>

<template>
  <el-card>
    <template #header>
      <div class="head">判定申请处理</div>
    </template>

    <el-form :inline="true" :model="query">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="居民姓名">
        <el-input v-model="query.residentName" clearable />
      </el-form-item>
      <el-form-item label="申请人">
        <el-input v-model="query.applicantUsername" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query.pageNum = 1; fetchData()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border :row-class-name="tableRowClassName">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="residentName" label="居民姓名" width="120" />
      <el-table-column prop="residentId" label="居民ID" width="90" />
      <el-table-column prop="applicantUsername" label="申请人" width="120" />
      <el-table-column prop="applyReason" label="申请原因" min-width="160" />
      <el-table-column prop="evidenceText" label="补充说明" min-width="150" />
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
      <el-table-column prop="reviewComment" label="审核意见" min-width="150" />
      <el-table-column prop="createdAt" label="提交时间" min-width="170" />
      <el-table-column label="待处理天数" width="130">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <span :class="{ 'overdue-text': row.isOverdue }">
              {{ row.pendingDays }}天
            </span>
            <el-tag v-if="row.isOverdue" type="danger" size="small" effect="plain" class="timeout-tag">已超时</el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="reviewedAt" label="处理时间" min-width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="primary" @click="openApprove(row)">通过并判定</el-button>
            <el-button link type="danger" @click="openReject(row)">驳回</el-button>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
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

  <el-dialog v-model="approveDialogVisible" title="通过并执行判定" width="520px">
    <el-form label-width="130px">
      <el-form-item label="审批方式">
        <el-radio-group v-model="approveForm.approveMode">
          <el-radio value="AUTO">自动判定</el-radio>
          <el-radio value="MANUAL">人工覆核</el-radio>
        </el-radio-group>
      </el-form-item>

      <template v-if="approveForm.approveMode === 'MANUAL'">
        <el-form-item label="人工判定状态">
          <el-select v-model="approveForm.manualStatus">
            <el-option label="常住人口" value="RESIDENT" />
            <el-option label="待判定" value="PENDING" />
            <el-option label="非常住人口" value="NON_RESIDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="人工判定理由">
          <el-input v-model="approveForm.manualReason" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </template>

      <el-form-item label="审核备注">
        <el-input v-model="approveForm.reviewComment" type="textarea" :rows="2" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="approveDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="approveLoading" @click="submitApprove">确认通过</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="rejectDialogVisible" title="驳回申请" width="460px">
    <el-form label-width="90px">
      <el-form-item label="驳回原因">
        <el-input v-model="rejectForm.reviewComment" type="textarea" :rows="3" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rejectDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="rejectLoading" @click="submitReject">确认驳回</el-button>
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
.head {
  font-weight: 600;
}

.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table .overdue-row) {
  --el-table-tr-bg-color: #fff1f2;
}

.overdue-text {
  color: #dc2626;
  font-weight: 600;
}

.timeout-tag {
  margin-left: 6px;
}
</style>
