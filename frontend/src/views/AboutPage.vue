<script setup>
const rules = [
  {
    code: "DOC_EXCLUDE",
    name: "文档排除类规则（v2）",
    weight: "-",
    desc: "临时借住、调查时点后出生、现役军人、港澳台/外籍、全户外出超半年、全户死亡或常住地无法确定，判定为非常住。"
  },
  {
    code: "DOC_RESIDENT_CORE",
    name: "文档核心常住规则（v2）",
    weight: "-",
    desc: "人在户在、户口待定但人在本地、人在且离开户籍地超半年、户在且外出不满半年、户在境外学习工作等判定为常住。"
  },
  {
    code: "DOC_SPECIAL_INCLUDE",
    name: "文档特殊纳入规则（v2）",
    weight: "-",
    desc: "调查时点后死亡、住校生且户口在家、调查时点后迁居但原居住地登记等，按常住纳入。"
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
          <el-table-column prop="weight" label="类型" width="80" />
          <el-table-column prop="desc" label="说明" min-width="260" />
        </el-table>
      </section>

      <section class="section">
        <h3>状态判定标准</h3>
        <el-alert title="仅保留 v2（文档规则）：按条件直接判定 RESIDENT / NON_RESIDENT，信息不足或冲突时为 PENDING。" type="success" :closable="false" />
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
