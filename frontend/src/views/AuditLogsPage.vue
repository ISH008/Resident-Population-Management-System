<script setup>
import { onMounted, reactive, ref } from "vue";
import { auditLogsPageApi } from "../api/audit";

const loading = ref(false);
const total = ref(0);
const tableData = ref([]);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  module: "",
  result: ""
});

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await auditLogsPageApi(query);
    tableData.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);
</script>

<template>
  <el-card>
    <template #header>
      <div class="head">操作审计日志</div>
    </template>
    <el-form :inline="true" :model="query">
      <el-form-item label="模块">
        <el-select v-model="query.module" clearable placeholder="全部">
          <el-option label="AUTH" value="AUTH" />
          <el-option label="USER" value="USER" />
          <el-option label="RESIDENT" value="RESIDENT" />
          <el-option label="RESIDENT_JUDGE" value="RESIDENT_JUDGE" />
          <el-option label="RESIDENT_JUDGE_APPLY" value="RESIDENT_JUDGE_APPLY" />
        </el-select>
      </el-form-item>
      <el-form-item label="结果">
        <el-select v-model="query.result" clearable placeholder="全部">
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="FAIL" value="FAIL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query.pageNum = 1; fetchData()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="operatorUsername" label="操作人" width="120" />
      <el-table-column prop="module" label="模块" width="130" />
      <el-table-column prop="action" label="动作" min-width="160" />
      <el-table-column prop="targetId" label="目标ID" width="110" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestPath" label="路径" min-width="180" />
      <el-table-column prop="result" label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.result === 'SUCCESS' ? 'success' : 'danger'">{{ row.result }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="信息" min-width="160" />
      <el-table-column prop="createdAt" label="时间" min-width="170" />
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
