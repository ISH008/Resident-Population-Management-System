<script setup>
const rules = [
  {
    code: "STAY_180_DAYS",
    name: "连续居住 >= 180 天",
    weight: 50,
    desc: "按居住开始日期与结束日期（或当前日期）计算连续居住天数，达到阈值记 50 分。"
  },
  {
    code: "VALID_PROOF",
    name: "有有效居住证明",
    weight: 20,
    desc: "存在有效居住证明（如租赁合同、房产证明、居住证等）记 20 分。"
  },
  {
    code: "LOCAL_EMPLOY_SOCIAL",
    name: "本地就业/社保/学籍",
    weight: 20,
    desc: "申请或审核时确认有本地就业、社保或学籍证明，记 20 分。"
  },
  {
    code: "LOCAL_ACTIVITY_90D",
    name: "近 90 天本地活动记录",
    weight: 10,
    desc: "申请或审核时确认近 90 天有本地活动证据，记 10 分。"
  }
];
</script>

<template>
  <el-card class="about-card">
    <template #header>
      <div class="head">关于系统</div>
    </template>

    <div class="content-wrap">
      <section class="section">
        <h3>系统简介</h3>
        <p>
          常住人口信息管理系统用于支撑常住人口档案管理、判定申请受理、审核处理与审计追踪。
          用户端可提交本人判定申请并查看处理进度，管理员端可统一审核并执行判定。
        </p>
        <p>
          系统设计目标是实现“流程可追溯、规则可解释、操作可审计”，减少线下反复沟通成本，
          提升判定效率与一致性。
        </p>
      </section>

      <section class="section">
        <h3>判定规则（当前版本）</h3>
        <el-table :data="rules" border>
          <el-table-column prop="code" label="规则编码" width="190" />
          <el-table-column prop="name" label="规则名称" min-width="180" />
          <el-table-column prop="weight" label="分值" width="80" />
          <el-table-column prop="desc" label="说明" min-width="260" />
        </el-table>
      </section>

      <section class="section">
        <h3>状态判定标准</h3>
        <el-alert title="RESIDENT（常住人口）：总分 >= 70" type="success" :closable="false" />
        <el-alert title="PENDING（待判定）：40 <= 总分 < 70" type="warning" :closable="false" class="mt10" />
        <el-alert title="NON_RESIDENT（非常住人口）：总分 < 40" type="error" :closable="false" class="mt10" />
      </section>

      <section class="section">
        <h3>说明</h3>
        <p>
          具体判定结果以管理员审核与系统最新规则版本为准。若申请被驳回，可根据审核意见补充材料后再次提交。
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
  margin: 0 0 10px;
  font-size: 16px;
}

p {
  margin: 0 0 10px;
  line-height: 1.7;
  color: #374151;
}

.mt10 {
  margin-top: 10px;
}
</style>
