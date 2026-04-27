<script setup>
import { computed, onMounted, ref } from "vue";
import { residentsPageApi } from "../api/residents";
import { auditLogsPageApi } from "../api/audit";

const loading = ref(false);
const residents = ref([]);
const auditLogs = ref([]);

const parseDate = (value) => {
  if (!value) {
    return null;
  }
  const normalized = String(value).includes("T") ? String(value) : String(value).replace(" ", "T");
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
};

const formatDay = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const daysBetween = (start, end) => {
  const msPerDay = 24 * 60 * 60 * 1000;
  return Math.max(0, Math.floor((end.getTime() - start.getTime()) / msPerDay));
};

const calcAge = (birthday) => {
  const birth = parseDate(birthday);
  if (!birth) {
    return null;
  }
  const now = new Date();
  let age = now.getFullYear() - birth.getFullYear();
  const monthDiff = now.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age < 0 ? null : age;
};

const percentText = (value, total) => {
  if (!total) {
    return "0%";
  }
  return `${((value / total) * 100).toFixed(1)}%`;
};

const RESIDENCE_STATUS_LABEL_MAP = {
  RESIDENT: "常住人口",
  PENDING: "待判定",
  NON_RESIDENT: "非常住人口"
};

const formatResidenceStatus = (status) => RESIDENCE_STATUS_LABEL_MAP[status] || status || "-";

const fetchAll = async (apiCall, baseParams = {}) => {
  const pageSize = 100;
  let pageNum = 1;
  let total = 0;
  const merged = [];
  do {
    const { data } = await apiCall({ ...baseParams, pageNum, pageSize });
    const records = data?.records || [];
    total = data?.total || 0;
    merged.push(...records);
    pageNum += 1;
  } while (merged.length < total);
  return merged;
};

const refresh = async () => {
  loading.value = true;
  try {
    const [residentRecords, logRecords] = await Promise.all([
      fetchAll(residentsPageApi),
      fetchAll(auditLogsPageApi)
    ]);
    residents.value = residentRecords;
    auditLogs.value = logRecords;
  } finally {
    loading.value = false;
  }
};

const todayStr = computed(() => formatDay(new Date()));

const isJudgeActionLog = (item) => {
  if (item.module === "RESIDENT_JUDGE") {
    return true;
  }
  return item.module === "RESIDENT_JUDGE_APPLY" && item.action === "审批通过判定申请";
};

const kpi = computed(() => {
  const totalResidents = residents.value.length;
  const residentCount = residents.value.filter((item) => item.residenceStatus === "RESIDENT").length;
  const pendingCount = residents.value.filter((item) => item.residenceStatus === "PENDING").length;
  const nonResidentCount = residents.value.filter((item) => item.residenceStatus === "NON_RESIDENT").length;
  const todayJudgeCount = auditLogs.value.filter((item) => {
    const day = parseDate(item.createdAt);
    return day && formatDay(day) === todayStr.value && item.result === "SUCCESS" && isJudgeActionLog(item);
  }).length;
  const todayFailCount = auditLogs.value.filter((item) => {
    const day = parseDate(item.createdAt);
    return day && formatDay(day) === todayStr.value && item.result === "FAIL";
  }).length;
  return {
    totalResidents,
    residentCount,
    pendingCount,
    nonResidentCount,
    todayJudgeCount,
    todayFailCount
  };
});

const statusRows = computed(() => {
  const rows = [
    { label: "常住人口", count: kpi.value.residentCount, color: "#16a34a" },
    { label: "待判定", count: kpi.value.pendingCount, color: "#f59e0b" },
    { label: "非常住人口", count: kpi.value.nonResidentCount, color: "#ef4444" }
  ];
  const max = Math.max(1, ...rows.map((item) => item.count));
  return rows.map((item) => ({
    ...item,
    percent: Math.round((item.count / max) * 100)
  }));
});

const residentPortrait = computed(() => {
  const residentOnly = residents.value.filter((item) => item.residenceStatus === "RESIDENT");
  const total = residentOnly.length;
  const maleCount = residentOnly.filter((item) => item.gender === "M").length;
  const femaleCount = residentOnly.filter((item) => item.gender === "F").length;

  const ageBuckets = { under18: 0, between18And60: 0, above60: 0 };
  residentOnly.forEach((item) => {
    const age = calcAge(item.birthday);
    if (age === null) {
      return;
    }
    if (age < 18) {
      ageBuckets.under18 += 1;
      return;
    }
    if (age <= 60) {
      ageBuckets.between18And60 += 1;
      return;
    }
    ageBuckets.above60 += 1;
  });

  return {
    total,
    genderRows: [
      { label: "男性", count: maleCount, percent: percentText(maleCount, total), color: "#3b82f6" },
      { label: "女性", count: femaleCount, percent: percentText(femaleCount, total), color: "#ec4899" }
    ],
    ageRows: [
      { label: "18岁以下", count: ageBuckets.under18, percent: percentText(ageBuckets.under18, total), color: "#22c55e" },
      { label: "18-60岁", count: ageBuckets.between18And60, percent: percentText(ageBuckets.between18And60, total), color: "#f59e0b" },
      { label: "60岁以上", count: ageBuckets.above60, percent: percentText(ageBuckets.above60, total), color: "#8b5cf6" }
    ]
  };
});

const trendRows = computed(() => {
  const rows = [];
  const today = new Date();
  for (let i = 6; i >= 0; i -= 1) {
    const current = new Date(today);
    current.setDate(today.getDate() - i);
    const dayStr = formatDay(current);
    rows.push({
      day: dayStr.slice(5),
      fullDay: dayStr,
      newResidents: 0,
      judgeActions: 0
    });
  }
  const indexByDay = new Map(rows.map((item) => [item.fullDay, item]));
  residents.value.forEach((item) => {
    const created = parseDate(item.createdAt);
    if (!created) {
      return;
    }
    const key = formatDay(created);
    if (indexByDay.has(key)) {
      indexByDay.get(key).newResidents += 1;
    }
  });
  auditLogs.value.forEach((item) => {
    const created = parseDate(item.createdAt);
    if (!created) {
      return;
    }
    const key = formatDay(created);
    if (indexByDay.has(key) && isJudgeActionLog(item)) {
      indexByDay.get(key).judgeActions += 1;
    }
  });
  const maxNew = Math.max(1, ...rows.map((item) => item.newResidents));
  const maxJudge = Math.max(1, ...rows.map((item) => item.judgeActions));
  return rows.map((item) => ({
    ...item,
    newPercent: Math.round((item.newResidents / maxNew) * 100),
    judgePercent: Math.round((item.judgeActions / maxJudge) * 100)
  }));
});

const riskList = computed(() => {
  const now = new Date();
  return residents.value
    .filter((item) => item.residenceStatus === "PENDING" || Number(item.residenceScore || 0) < 40)
    .map((item) => {
      const created = parseDate(item.createdAt);
      return {
        id: item.id,
        name: item.name,
        residenceStatus: item.residenceStatus,
        residenceScore: Number(item.residenceScore || 0),
        pendingDays: created ? daysBetween(created, now) : 0
      };
    })
    .sort((a, b) => {
      if (a.residenceStatus === "PENDING" && b.residenceStatus !== "PENDING") {
        return -1;
      }
      if (a.residenceStatus !== "PENDING" && b.residenceStatus === "PENDING") {
        return 1;
      }
      if (b.pendingDays !== a.pendingDays) {
        return b.pendingDays - a.pendingDays;
      }
      return a.residenceScore - b.residenceScore;
    })
    .slice(0, 10);
});

onMounted(refresh);
</script>

<template>
  <div v-loading="loading">
    <el-card>
      <template #header>
          <div class="head-wrap">
            <div class="head-title">数据看板</div>
            <el-button type="primary" @click="refresh">刷新数据</el-button>
          </div>
        </template>

      <div class="kpi-grid">
        <el-statistic title="人口总量" :value="kpi.totalResidents" />
        <el-statistic title="已判定常住" :value="kpi.residentCount" />
        <el-statistic title="待判定" :value="kpi.pendingCount" />
        <el-statistic title="非常住" :value="kpi.nonResidentCount" />
        <el-statistic title="今日判定(成功)" :value="kpi.todayJudgeCount" />
        <el-statistic title="今日失败操作" :value="kpi.todayFailCount" />
      </div>
    </el-card>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <div class="section-title">常住状态分布</div>
          </template>
          <div v-for="row in statusRows" :key="row.label" class="bar-row">
            <div class="bar-label">{{ row.label }}</div>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: `${row.percent}%`, backgroundColor: row.color }" />
            </div>
            <div class="bar-value">{{ row.count }}</div>
          </div>
        </el-card>

        <el-card class="mt16">
          <template #header>
            <div class="section-title">常住人口画像</div>
          </template>
          <div class="portrait-tip">统计口径：仅包含常住状态为常住人口的居民</div>

          <div class="portrait-block">
            <div class="portrait-subtitle">性别构成</div>
            <div v-for="row in residentPortrait.genderRows" :key="row.label" class="portrait-row">
              <div class="portrait-label">{{ row.label }}</div>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: row.percent, backgroundColor: row.color }"
                />
              </div>
              <div class="portrait-value">{{ row.count }}（{{ row.percent }}）</div>
            </div>
          </div>

          <div class="portrait-block">
            <div class="portrait-subtitle">年龄结构</div>
            <div v-for="row in residentPortrait.ageRows" :key="row.label" class="portrait-row">
              <div class="portrait-label">{{ row.label }}</div>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: row.percent, backgroundColor: row.color }"
                />
              </div>
              <div class="portrait-value">{{ row.count }}（{{ row.percent }}）</div>
            </div>
          </div>

          <div v-if="residentPortrait.total < 10" class="portrait-tip warn">
            当前常住样本量较小（{{ residentPortrait.total }}人），画像结果仅供参考。
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="14">
        <el-card>
          <template #header>
            <div class="section-title">近7天趋势（新增人口 / 判定操作）</div>
          </template>
          <div v-for="row in trendRows" :key="row.fullDay" class="trend-row">
            <div class="trend-day">{{ row.day }}</div>
            <div class="trend-bars">
              <div class="trend-item">
                <span class="trend-name">新增</span>
                <div class="bar-track">
                  <div class="bar-fill trend-new" :style="{ width: `${row.newPercent}%` }" />
                </div>
                <span class="trend-value">{{ row.newResidents }}</span>
              </div>
              <div class="trend-item">
                <span class="trend-name">判定</span>
                <div class="bar-track">
                  <div class="bar-fill trend-judge" :style="{ width: `${row.judgePercent}%` }" />
                </div>
                <span class="trend-value">{{ row.judgeActions }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="mt16">
      <template #header>
        <div class="section-title">风险名单（长期待判定 / 低分）</div>
      </template>
      <el-table :data="riskList" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="residenceStatus" label="状态" width="140">
          <template #default="{ row }">{{ formatResidenceStatus(row.residenceStatus) }}</template>
        </el-table-column>
        <el-table-column prop="residenceScore" label="评分" width="100" />
        <el-table-column prop="pendingDays" label="待判定天数" width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.head-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.head-title {
  font-size: 16px;
  font-weight: 600;
}

.section-title {
  font-weight: 600;
}

.mt16 {
  margin-top: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 16px;
}

.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 52px;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.bar-label {
  font-size: 13px;
  color: #374151;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #e5e7eb;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
}

.bar-value {
  text-align: right;
  font-weight: 600;
  color: #111827;
}

.trend-row {
  display: grid;
  grid-template-columns: 60px 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.trend-day {
  color: #6b7280;
  font-weight: 600;
}

.trend-bars {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.trend-item {
  display: grid;
  grid-template-columns: 40px 1fr 30px;
  align-items: center;
  gap: 8px;
}

.trend-name {
  color: #4b5563;
  font-size: 12px;
}

.trend-new {
  background: #2563eb;
}

.trend-judge {
  background: #059669;
}

.trend-value {
  text-align: right;
  font-weight: 600;
  color: #111827;
}

.portrait-tip {
  margin-bottom: 10px;
  color: #6b7280;
  font-size: 12px;
}

.portrait-tip.warn {
  margin-top: 10px;
}

.portrait-block + .portrait-block {
  margin-top: 12px;
}

.portrait-subtitle {
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
  font-weight: 600;
}

.portrait-row {
  display: grid;
  grid-template-columns: 70px 1fr 110px;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.portrait-label {
  font-size: 12px;
  color: #4b5563;
}

.portrait-value {
  text-align: right;
  font-size: 12px;
  color: #111827;
}
</style>
