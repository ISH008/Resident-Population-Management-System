<script setup>
import { onMounted, reactive, ref } from "vue";
import {
  downloadJudgeApplicationAttachmentApi,
  judgeApplicationsMineApi,
  listJudgeApplicationAttachmentsApi
} from "../api/judgeApplications";

const loading = ref(false);
const attachmentLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const detailVisible = ref(false);
const attachmentList = ref([]);
const current = ref(null);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: ""
});

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

const statusText = (status) => {
  if (status === "APPROVED") {
    return "已通过";
  }
  if (status === "REJECTED") {
    return "已驳回";
  }
  return "待处理";
};

const statusType = (status) => {
  if (status === "APPROVED") {
    return "success";
  }
  if (status === "REJECTED") {
    return "danger";
  }
  return "warning";
};

const openDetail = async (row) => {
  current.value = row;
  detailVisible.value = true;
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

onMounted(fetchData);
</script>

<template>
  <el-card>
    <template #header>
      <div class="head">判定进度</div>
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
      <el-table-column prop="id" label="申请ID" width="90" />
      <el-table-column prop="residentName" label="居民姓名" width="120" />
      <el-table-column prop="applyReason" label="申请原因" min-width="180" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" min-width="170" />
      <el-table-column prop="reviewedAt" label="处理时间" min-width="170" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看进度</el-button>
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

  <el-dialog v-model="detailVisible" title="申请进度详情" width="760px">
    <el-row :gutter="16">
      <el-col :span="11">
        <el-timeline>
          <el-timeline-item :timestamp="current?.createdAt" type="primary">
            已提交申请
          </el-timeline-item>
          <el-timeline-item
            v-if="current?.status === 'PENDING'"
            timestamp="处理中"
            type="warning"
          >
            管理员审核中
          </el-timeline-item>
          <el-timeline-item
            v-else
            :timestamp="current?.reviewedAt"
            :type="current?.status === 'APPROVED' ? 'success' : 'danger'"
          >
            {{ current?.status === "APPROVED" ? "审核通过" : "已驳回" }}
          </el-timeline-item>
        </el-timeline>
        <el-alert
          v-if="current?.reviewComment"
          :title="`审核意见：${current.reviewComment}`"
          :type="current?.status === 'REJECTED' ? 'error' : 'success'"
          show-icon
          :closable="false"
        />
      </el-col>
      <el-col :span="13">
        <el-table :data="attachmentList" border v-loading="attachmentLoading" max-height="320">
          <el-table-column prop="originalName" label="附件" min-width="180" />
          <el-table-column label="大小" width="100">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="上传时间" min-width="150" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="downloadAttachment(row)">下载</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>
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
</style>
