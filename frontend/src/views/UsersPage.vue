<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { createUserApi, deleteUserApi, rolesApi, updateUserApi, usersPageApi } from "../api/users";
import { residentsPageApi } from "../api/residents";

const loading = ref(false);
const submitLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const roles = ref([]);
const residentOptions = ref([]);
const residentLoading = ref(false);
const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: ""
});

const form = reactive({
  id: null,
  username: "",
  password: "",
  realName: "",
  phone: "",
  residentId: null,
  status: 1,
  roleIds: []
});

const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 32, message: "长度4-32个字符", trigger: "blur" },
    { pattern: /^[\u4e00-\u9fa5a-zA-Z0-9_]+$/, message: "仅支持中文/字母/数字/下划线", trigger: "blur" }
  ],
  password: [
    {
      validator: (_, value, callback) => {
        if (!isEdit.value && !value) {
          callback(new Error("请输入密码"));
          return;
        }
        if (value && value.length < 6) {
          callback(new Error("密码至少6位"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ],
  phone: [{ pattern: /^1\d{10}$/, message: "手机号格式错误", trigger: "blur" }],
  roleIds: [{ type: "array", required: true, message: "请选择至少一个角色", trigger: "change" }]
};

const nowLabel = () => new Date().toLocaleString("zh-CN", { hour12: false });

const fetchRoles = async () => {
  const { data } = await rolesApi();
  roles.value = data;
};

const fetchResidents = async (name = "") => {
  residentLoading.value = true;
  try {
    const { data } = await residentsPageApi({
      pageNum: 1,
      pageSize: 50,
      name
    });
    residentOptions.value = data.records || [];
  } finally {
    residentLoading.value = false;
  }
};

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await usersPageApi(query);
    tableData.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.id = null;
  form.username = "";
  form.password = "";
  form.realName = "";
  form.phone = "";
  form.residentId = null;
  form.status = 1;
  form.roleIds = [];
};

const openCreate = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

const openEdit = (row) => {
  isEdit.value = true;
  form.id = row.id;
  form.username = row.username;
  form.password = "";
  form.realName = row.realName || "";
  form.phone = row.phone || "";
  form.residentId = row.residentId || null;
  form.status = row.status ?? 1;
  form.roleIds = roles.value.filter((r) => (row.roles || []).includes(r.roleCode)).map((r) => r.id);
  dialogVisible.value = true;
};

const submit = async () => {
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (isEdit.value) {
      const payload = {
        password: form.password || undefined,
        realName: form.realName,
        phone: form.phone,
        residentId: form.residentId,
        status: form.status,
        roleIds: form.roleIds
      };
      await updateUserApi(form.id, payload);
      ElMessage.success(`修改成功，已记录审计（${nowLabel()}）`);
    } else {
      await createUserApi({
        username: form.username,
        password: form.password,
        realName: form.realName,
        phone: form.phone,
        residentId: form.residentId,
        status: form.status,
        roleIds: form.roleIds
      });
      ElMessage.success(`新增成功，已记录审计（${nowLabel()}）`);
    }
    dialogVisible.value = false;
    await fetchData();
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户 ${row.username} 吗？`, "提示", { type: "warning" });
  await deleteUserApi(row.id);
  ElMessage.success(`删除成功，已记录审计（${nowLabel()}）`);
  await fetchData();
};

onMounted(async () => {
  await Promise.all([fetchRoles(), fetchResidents()]);
  await fetchData();
});
</script>

<template>
  <el-card>
    <template #header>
      <div class="head-wrap">
        <span class="head">用户管理</span>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </template>
    <el-form :inline="true" :model="query">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="用户名" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query.pageNum = 1; fetchData()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="绑定居民" min-width="160">
        <template #default="{ row }">
          <span v-if="row.residentId">{{ row.residentName || "-" }}（ID:{{ row.residentId }}）</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? "启用" : "禁用" }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role" style="margin-right: 6px;">
            {{ role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="isEdit" />
      </el-form-item>
      <el-form-item :label="isEdit ? '新密码' : '密码'" prop="password">
        <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空表示不修改' : ''" />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.realName" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" />
      </el-form-item>
      <el-form-item label="绑定居民">
        <el-select
          v-model="form.residentId"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="fetchResidents"
          :loading="residentLoading"
          placeholder="输入姓名搜索"
          style="width: 100%"
        >
          <el-option
            v-for="item in residentOptions"
            :key="item.id"
            :label="`${item.name}（ID:${item.id}）`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="角色" prop="roleIds">
        <el-select v-model="form.roleIds" multiple style="width: 100%">
          <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submit">保存</el-button>
    </template>
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
