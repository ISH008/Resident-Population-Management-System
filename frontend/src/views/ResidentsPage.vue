<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  createResidentApi,
  deleteResidentApi,
  judgeResidentApi,
  residentDetailApi,
  residentJudgeLogsApi,
  residentsPageApi,
  updateResidentApi
} from "../api/residents";
import {
  createResidentMobilityLogApi,
  deleteResidentMobilityLogApi,
  listResidentMobilityLogsApi
} from "../api/mobility";
import { useAuthStore } from "../stores/auth";
import { REGION_OPTIONS } from "../constants/regionOptions";

const authStore = useAuthStore();
const isAdmin = computed(() => authStore.roles.includes("ADMIN"));

const loading = ref(false);
const submitLoading = ref(false);
const judgeLoading = ref(false);
const tableData = ref([]);
const total = ref(0);
const dialogVisible = ref(false);
const judgeDialogVisible = ref(false);
const logsDialogVisible = ref(false);
const mobilityDialogVisible = ref(false);
const mobilityLoading = ref(false);
const mobilitySubmitLoading = ref(false);
const isEdit = ref(false);
const formRef = ref(null);
const logs = ref([]);
const mobilityLogs = ref([]);
const mobilityResident = reactive({
  id: null,
  name: ""
});
const mobilityForm = reactive({
  changeType: "INFLOW",
  changeDate: "",
  fromRegion: "",
  toRegion: "",
  reason: "",
  remark: ""
});

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  name: "",
  idCard: "",
  residenceStatus: ""
});

const form = reactive({
  id: null,
  name: "",
  idCard: "",
  gender: "M",
  birthday: "",
  phone: "",
  addressProvince: "",
  addressCity: "",
  addressDistrict: "",
  addressDetail: "",
  residenceType: "PERMANENT",
  status: "NORMAL",
  stayStartDate: "",
  stayEndDate: "",
  isLocalHukou: 0,
  proofType: "RENT_CONTRACT"
});

const judgeForm = reactive({
  id: null,
  localEmploySocial: true,
  localActivity90d: true,
  version: "v1"
});

const rules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  idCard: [
    { required: true, message: "请输入身份证号", trigger: "blur" },
    {
      validator: (_, value, callback) => {
        if (!value || !/^[0-9Xx]{18}$/.test(value)) {
          callback(new Error("身份证号格式错误"));
          return;
        }
        if (!isValidIdCard(value)) {
          callback(new Error("身份证号校验失败"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ],
  gender: [{ required: true, message: "请选择性别", trigger: "change" }],
  phone: [{ pattern: /^1\d{10}$/, message: "手机号格式错误", trigger: "blur" }],
  stayEndDate: [
    {
      validator: (_, value, callback) => {
        if (value && form.stayStartDate && value < form.stayStartDate) {
          callback(new Error("居住结束日期不能早于开始日期"));
          return;
        }
        callback();
      },
      trigger: "change"
    }
  ]
};

const nowLabel = () => new Date().toLocaleString("zh-CN", { hour12: false });
const provinceOptions = REGION_OPTIONS.map((item) => ({ label: item.label, value: item.value }));

const cityOptions = computed(() => {
  const province = REGION_OPTIONS.find((item) => item.value === form.addressProvince);
  return (province?.cities || []).map((item) => ({ label: item.label, value: item.value }));
});

const districtOptions = computed(() => {
  const province = REGION_OPTIONS.find((item) => item.value === form.addressProvince);
  const city = (province?.cities || []).find((item) => item.value === form.addressCity);
  return (city?.districts || []).map((item) => ({ label: item, value: item }));
});

const handleProvinceChange = () => {
  form.addressCity = "";
  form.addressDistrict = "";
};

const handleCityChange = () => {
  form.addressDistrict = "";
};

const isValidIdCard = (idCard) => {
  const code = idCard.toUpperCase();
  const factors = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const checks = ["1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"];
  const birth = code.substring(6, 14);
  const date = `${birth.substring(0, 4)}-${birth.substring(4, 6)}-${birth.substring(6, 8)}`;
  if (Number.isNaN(Date.parse(date))) {
    return false;
  }
  let sum = 0;
  for (let i = 0; i < 17; i += 1) {
    sum += Number(code[i]) * factors[i];
  }
  return checks[sum % 11] === code[17];
};

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await residentsPageApi(query);
    tableData.value = data.records;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.id = null;
  form.name = "";
  form.idCard = "";
  form.gender = "M";
  form.birthday = "";
  form.phone = "";
  form.addressProvince = "";
  form.addressCity = "";
  form.addressDistrict = "";
  form.addressDetail = "";
  form.residenceType = "PERMANENT";
  form.status = "NORMAL";
  form.stayStartDate = "";
  form.stayEndDate = "";
  form.isLocalHukou = 0;
  form.proofType = "RENT_CONTRACT";
};

const openCreate = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

const openEdit = async (row) => {
  isEdit.value = true;
  const { data } = await residentDetailApi(row.id);
  form.id = data.id;
  form.name = data.name;
  form.idCard = data.idCard;
  form.gender = data.gender;
  form.birthday = data.birthday || "";
  form.phone = data.phone || "";
  form.addressProvince = data.addressProvince || "";
  form.addressCity = data.addressCity || "";
  form.addressDistrict = data.addressDistrict || "";
  form.addressDetail = data.addressDetail || "";
  if (!form.addressProvince && !form.addressCity && !form.addressDistrict && !form.addressDetail && data.actualAddress) {
    form.addressDetail = data.actualAddress;
  }
  form.residenceType = data.residenceType || "PERMANENT";
  form.status = data.status || "NORMAL";
  form.stayStartDate = data.stayStartDate || "";
  form.stayEndDate = data.stayEndDate || "";
  form.isLocalHukou = data.isLocalHukou ?? 0;
  form.proofType = data.proofType || "";
  dialogVisible.value = true;
};

const submit = async () => {
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const payload = {
      name: form.name,
      idCard: form.idCard,
      gender: form.gender,
      birthday: form.birthday || null,
      phone: form.phone,
      addressProvince: form.addressProvince || null,
      addressCity: form.addressCity || null,
      addressDistrict: form.addressDistrict || null,
      addressDetail: form.addressDetail || null,
      residenceType: form.residenceType,
      status: form.status,
      stayStartDate: form.stayStartDate || null,
      stayEndDate: form.stayEndDate || null,
      isLocalHukou: form.isLocalHukou,
      proofType: form.proofType
    };
    if (isEdit.value) {
      await updateResidentApi(form.id, payload);
      ElMessage.success(`修改成功，已记录审计（${nowLabel()}）`);
    } else {
      await createResidentApi(payload);
      ElMessage.success(`新增成功，已记录审计（${nowLabel()}）`);
    }
    dialogVisible.value = false;
    await fetchData();
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除 ${row.name} 吗？`, "提示", {
    type: "warning",
    cancelButtonText: "取消",
    confirmButtonText: "确定"
  });
  await deleteResidentApi(row.id);
  ElMessage.success(`删除成功，已记录审计（${nowLabel()}）`);
  await fetchData();
};

const openJudge = (row) => {
  judgeForm.id = row.id;
  judgeForm.localEmploySocial = true;
  judgeForm.localActivity90d = true;
  judgeForm.version = "v1";
  judgeDialogVisible.value = true;
};

const doJudge = async () => {
  judgeLoading.value = true;
  try {
    const { data } = await judgeResidentApi(judgeForm.id, {
      localEmploySocial: judgeForm.localEmploySocial,
      localActivity90d: judgeForm.localActivity90d,
      version: judgeForm.version
    });
    ElMessage.success(`判定完成: ${data.finalStatus} (${data.finalScore}分)，已记录审计（${nowLabel()}）`);
    judgeDialogVisible.value = false;
    await fetchData();
  } finally {
    judgeLoading.value = false;
  }
};

const openLogs = async (row) => {
  const { data } = await residentJudgeLogsApi(row.id);
  logs.value = data;
  logsDialogVisible.value = true;
};

const mobilityTypeText = (type) => (type === "INFLOW" ? "迁入" : "迁出");

const resetMobilityForm = () => {
  mobilityForm.changeType = "INFLOW";
  mobilityForm.changeDate = new Date().toISOString().slice(0, 10);
  mobilityForm.fromRegion = "";
  mobilityForm.toRegion = "";
  mobilityForm.reason = "";
  mobilityForm.remark = "";
};

const fetchMobilityLogs = async () => {
  mobilityLoading.value = true;
  try {
    const { data } = await listResidentMobilityLogsApi(mobilityResident.id);
    mobilityLogs.value = data;
  } finally {
    mobilityLoading.value = false;
  }
};

const openMobility = async (row) => {
  mobilityResident.id = row.id;
  mobilityResident.name = row.name;
  resetMobilityForm();
  mobilityDialogVisible.value = true;
  await fetchMobilityLogs();
};

const submitMobility = async () => {
  if (!mobilityForm.changeDate) {
    ElMessage.error("请选择迁移日期");
    return;
  }
  if (!mobilityForm.fromRegion.trim() || !mobilityForm.toRegion.trim() || !mobilityForm.reason.trim()) {
    ElMessage.error("请填写完整的迁移信息");
    return;
  }
  mobilitySubmitLoading.value = true;
  try {
    await createResidentMobilityLogApi(mobilityResident.id, {
      changeType: mobilityForm.changeType,
      changeDate: mobilityForm.changeDate,
      fromRegion: mobilityForm.fromRegion.trim(),
      toRegion: mobilityForm.toRegion.trim(),
      reason: mobilityForm.reason.trim(),
      remark: mobilityForm.remark.trim()
    });
    ElMessage.success(`迁移记录新增成功，已记录审计（${nowLabel()}）`);
    resetMobilityForm();
    await fetchMobilityLogs();
  } finally {
    mobilitySubmitLoading.value = false;
  }
};

const deleteMobility = async (row) => {
  await ElMessageBox.confirm("确认删除该迁移记录吗？", "提示", { type: "warning" });
  await deleteResidentMobilityLogApi(mobilityResident.id, row.id);
  ElMessage.success(`迁移记录已删除，已记录审计（${nowLabel()}）`);
  await fetchMobilityLogs();
};

onMounted(fetchData);
</script>

<template>
  <el-card>
    <template #header>
      <div class="head-wrap">
        <span class="head">居民档案列表</span>
        <el-button v-if="isAdmin" type="primary" @click="openCreate">新增人口</el-button>
      </div>
    </template>
    <el-form :inline="true" :model="query">
      <el-form-item label="姓名">
        <el-input v-model="query.name" placeholder="姓名" clearable />
      </el-form-item>
      <el-form-item label="身份证">
        <el-input v-model="query.idCard" placeholder="身份证号" clearable />
      </el-form-item>
      <el-form-item label="常住状态">
        <el-select v-model="query.residenceStatus" placeholder="全部" clearable style="width: 160px">
          <el-option label="常住人口" value="RESIDENT" />
          <el-option label="待判定" value="PENDING" />
          <el-option label="非常住人口" value="NON_RESIDENT" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query.pageNum = 1; fetchData()">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="idCard" label="身份证号" min-width="180" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="residenceStatus" label="常住状态" width="120" />
      <el-table-column prop="residenceScore" label="评分" width="80" />
      <el-table-column label="操作" min-width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openLogs(row)">判定日志</el-button>
          <template v-if="isAdmin">
            <el-button link type="primary" @click="openJudge(row)">判定</el-button>
            <el-button link type="primary" @click="openMobility(row)">迁移记录</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
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

  <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑人口' : '新增人口'" width="720px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="身份证" prop="idCard">
            <el-input v-model="form.idCard" :disabled="isEdit" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender">
              <el-radio value="M">男</el-radio>
              <el-radio value="F">女</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="出生日期">
            <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="户籍类型">
            <el-select v-model="form.residenceType" style="width: 100%">
              <el-option label="常住" value="PERMANENT" />
              <el-option label="临时" value="TEMPORARY" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="省份">
            <el-select
              v-model="form.addressProvince"
              filterable
              allow-create
              clearable
              default-first-option
              style="width: 100%"
              placeholder="选择或输入省份"
              @change="handleProvinceChange"
            >
              <el-option v-for="item in provinceOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="城市">
            <el-select
              v-model="form.addressCity"
              filterable
              allow-create
              clearable
              default-first-option
              style="width: 100%"
              placeholder="选择或输入城市"
              @change="handleCityChange"
            >
              <el-option v-for="item in cityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="区县">
            <el-select
              v-model="form.addressDistrict"
              filterable
              allow-create
              clearable
              default-first-option
              style="width: 100%"
              placeholder="选择或输入区县"
            >
              <el-option v-for="item in districtOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="详细地址">
            <el-input v-model="form.addressDetail" placeholder="请输入门牌号、楼栋等详细地址" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="居住开始">
            <el-date-picker v-model="form.stayStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="居住结束" prop="stayEndDate">
            <el-date-picker v-model="form.stayEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="本地户籍">
            <el-radio-group v-model="form.isLocalHukou">
              <el-radio :value="1">是</el-radio>
              <el-radio :value="0">否</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="居住证明">
            <el-select v-model="form.proofType" style="width: 100%">
              <el-option label="租赁合同" value="RENT_CONTRACT" />
              <el-option label="房产证明" value="HOUSE_CERT" />
              <el-option label="居住证" value="RESIDENCE_PERMIT" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="submit">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="judgeDialogVisible" title="常住判定" width="420px">
    <el-form label-width="140px">
      <el-form-item label="本地就业/社保/学籍">
        <el-switch v-model="judgeForm.localEmploySocial" />
      </el-form-item>
      <el-form-item label="近90天本地活动">
        <el-switch v-model="judgeForm.localActivity90d" />
      </el-form-item>
      <el-form-item label="规则版本">
        <el-input v-model="judgeForm.version" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="judgeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="judgeLoading" @click="doJudge">执行判定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="logsDialogVisible" title="判定日志" width="840px">
    <el-table :data="logs" border max-height="420">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="ruleCode" label="规则" min-width="160" />
      <el-table-column prop="hitFlag" label="命中" width="80">
        <template #default="{ row }">{{ row.hitFlag === 1 ? "是" : "否" }}</template>
      </el-table-column>
      <el-table-column prop="scoreDelta" label="分值变化" width="100" />
      <el-table-column prop="finalScore" label="累计分" width="90" />
      <el-table-column prop="finalStatus" label="状态" width="120" />
      <el-table-column prop="judgeVersion" label="版本" width="90" />
      <el-table-column prop="judgeTime" label="时间" min-width="170" />
    </el-table>
  </el-dialog>

  <el-dialog v-model="mobilityDialogVisible" title="迁移记录管理" width="920px">
    <div class="mobility-head">居民：{{ mobilityResident.name }}（ID: {{ mobilityResident.id }}）</div>
    <el-form label-width="100px" class="mobility-form">
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="迁移类型">
            <el-select v-model="mobilityForm.changeType" style="width: 100%">
              <el-option label="迁入" value="INFLOW" />
              <el-option label="迁出" value="OUTFLOW" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="迁移日期">
            <el-date-picker
              v-model="mobilityForm.changeDate"
              type="date"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="迁移原因">
            <el-input v-model="mobilityForm.reason" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="迁出地">
            <el-input v-model="mobilityForm.fromRegion" maxlength="120" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="迁入地">
            <el-input v-model="mobilityForm.toRegion" maxlength="120" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="mobilityForm.remark" maxlength="500" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div class="mobility-actions">
      <el-button type="primary" :loading="mobilitySubmitLoading" @click="submitMobility">新增迁移记录</el-button>
    </div>

    <el-table :data="mobilityLogs" border max-height="360" v-loading="mobilityLoading">
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
      <el-table-column prop="reason" label="原因" min-width="160" />
      <el-table-column prop="operatorUsername" label="操作人" width="100" />
      <el-table-column prop="createdAt" label="登记时间" min-width="160" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="deleteMobility(row)">删除</el-button>
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

.mobility-head {
  margin-bottom: 10px;
  color: #374151;
  font-size: 13px;
}

.mobility-form {
  padding: 12px 12px 0;
  margin-bottom: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.mobility-actions {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
