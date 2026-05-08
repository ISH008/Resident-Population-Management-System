<script setup>
const rules = [
  { no: 1, code: "排除规则", status: "NON_RESIDENT", desc: "满足以下任一条件：调查时点后出生、调查前夜临时借住、现役军人、港澳台居民、外籍人员、全户死亡、常住地无法确定、全户外出超半年。" },
  { no: 2, code: "时点后死亡纳入", status: "RESIDENT", desc: "调查时点后死亡，仍计入常住人口。" },
  { no: 3, code: "人在户在/经常居住", status: "RESIDENT", desc: "户口在本街镇，且经常居住在本地，或调查时点在本地。" },
  { no: 4, code: "户口待定但人在本地", status: "RESIDENT", desc: "调查时点人在本地，且户口状态为待定。" },
  { no: 5, code: "离开户籍地超半年", status: "RESIDENT", desc: "调查时点人在本地，且离开户籍地持续超过半年。" },
  { no: 6, code: "户在外出不足半年", status: "RESIDENT", desc: "户口在本街镇，且外出时间不足半年。" },
  { no: 7, code: "户在境外学习工作", status: "RESIDENT", desc: "户口在本街镇，且处于境外学习或工作状态。" },
  { no: 8, code: "住校生登记在家", status: "RESIDENT", desc: "住校生且户口在家。" },
  { no: 9, code: "出租房房东补录", status: "RESIDENT", desc: "出租房场景下，房东户口仍在本址。" },
  { no: 10, code: "时点后迁居原址登记", status: "RESIDENT", desc: "调查时点后迁居，原居住地仍需登记。" },
  { no: 11, code: "返籍常住重算", status: "NON_RESIDENT", desc: "返回户籍地常住超半年，且不属于偶尔返乡。" },
  { no: 12, code: "信息不足待核实", status: "PENDING", desc: "未命中明确规则，进入待判定。" }
];

const statusText = (status) => {
  if (status === "RESIDENT") return "常住人口";
  if (status === "NON_RESIDENT") return "非常住人口";
  return "待判定";
};

const statusTagType = (status) => {
  if (status === "RESIDENT") return "success";
  if (status === "NON_RESIDENT") return "danger";
  return "warning";
};
</script>

<template>
  <el-card class="about-card">
    <template #header>
      <div class="head">关于</div>
    </template>

    <div class="content-wrap">
      <section class="section">
        <h3>常住人口判定规则</h3>
        <el-table :data="rules" border stripe class="rule-table">
          <el-table-column prop="no" label="优先级" width="90" />
          <el-table-column prop="code" label="规则名称" min-width="220" />
          <el-table-column label="判定结果" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="desc" label="规则条件（规则化表述）" min-width="420" />
        </el-table>
      </section>

      <section class="section">
        <h3>用户端说明</h3>
        <p>
          若因各种特殊情况导致常住人口未统计的、统计错误的，均可在“判定申请”界面进行申请（支持上传佐证材料与附件），并在“判定进度”中查看判定的进度，在“我的资料中”进行个人资料的维护。
        </p>
      </section>
    </div>
  </el-card>
</template>

<style scoped>
.about-card {
  height: calc(100vh - 120px);
}

.head {
  font-weight: 600;
}

.content-wrap {
  height: calc(100vh - 220px);
  overflow: auto;
  padding-right: 6px;
}

.section {
  margin-bottom: 20px;
}

h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

p {
  margin: 0;
  line-height: 1.8;
  color: #374151;
}

.rule-table :deep(.el-table__cell) {
  vertical-align: top;
}
</style>
