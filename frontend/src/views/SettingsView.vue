<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { settingsApi } from '@/api/settings'
import { ElMessage } from 'element-plus'

const aiPolicy = ref<Record<string, unknown> | null>(null)
const quality = ref<Record<string, unknown> | null>(null)
const ai = ref<Record<string, unknown> | null>(null)
const levels = ref<{ levelsJson: string; version: number } | null>(null)
const warning = ref<Record<string, unknown> | null>(null)
const courseCode = ref<Record<string, unknown> | null>(null)

async function refresh() {
  aiPolicy.value = await settingsApi.getAIPolicy()
  quality.value = (await settingsApi.getQuality()) as Record<string, unknown>
  ai.value = (await settingsApi.getAI()) as Record<string, unknown>
  levels.value = (await settingsApi.getAbilityLevels()) as { levelsJson: string; version: number }
  warning.value = (await settingsApi.getWarningRules()) as Record<string, unknown>
  courseCode.value = (await settingsApi.getCourseCode()) as Record<string, unknown>
}

onMounted(refresh)

async function toggleAI() {
  if (!aiPolicy.value) return
  const current = aiPolicy.value.externalEnabled as boolean
  await settingsApi.updateAIPolicy({
    externalEnabled: !current,
    approvalRecordId: !current ? 'OSG-APPROVAL-EXAMPLE-001' : '',
    studentDataOutbound: false
  })
  ElMessage.success('AI 策略已更新。')
  await refresh()
}
</script>

<template>
  <div>
    <h2>学校级配置中心</h2>
    <p class="hint">仅 SCHOOL_ADMIN 可修改；其他角色只读（基线 PERM-SCHOOL-002）。</p>

    <el-card v-if="aiPolicy" style="margin-top: 16px">
      <template #header>
        <div class="header-row">
          <span>学校级 AI 策略</span>
          <el-button size="small" :type="aiPolicy.externalEnabled ? 'danger' : 'primary'" @click="toggleAI">
            {{ aiPolicy.externalEnabled ? '禁用外部 AI' : '启用外部 AI' }}
          </el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="外部 Provider">{{ aiPolicy.externalProviderCode }}</el-descriptions-item>
        <el-descriptions-item label="启用">{{ aiPolicy.externalEnabled ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="学生数据出域">{{ aiPolicy.studentDataOutbound ? '是（违规）' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="审批记录 ID">{{ aiPolicy.approvalRecordId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="Prompt 版本">{{ aiPolicy.promptVersion }}</el-descriptions-item>
        <el-descriptions-item label="Schema 版本">{{ aiPolicy.schemaVersion }}</el-descriptions-item>
        <el-descriptions-item label="每日配额">{{ aiPolicy.quotaPerDay }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ aiPolicy.version }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="quality" style="margin-top: 16px">
      <template #header><span>课程质量阈值</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="最少学分">{{ (quality as any).minCredits }}</el-descriptions-item>
        <el-descriptions-item label="最多学分">{{ (quality as any).maxCredits }}</el-descriptions-item>
        <el-descriptions-item label="实践比例下限">{{ (quality as any).minPracticeRatio }}</el-descriptions-item>
        <el-descriptions-item label="学期最多课程">{{ (quality as any).maxCoursePerTerm }}</el-descriptions-item>
        <el-descriptions-item label="支撑等级下限">{{ (quality as any).minSupportDegree }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ (quality as any).version }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="levels" style="margin-top: 16px">
      <template #header><span>能力等级配置</span></template>
      <pre>{{ levels.levelsJson }}</pre>
    </el-card>

    <el-card v-if="warning" style="margin-top: 16px">
      <template #header><span>成长预警配置</span></template>
      <pre>{{ JSON.stringify(warning, null, 2) }}</pre>
    </el-card>

    <el-card v-if="courseCode" style="margin-top: 16px">
      <template #header><span>临时代码策略</span></template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="允许临时代码">{{ (courseCode as any).allowTempCode ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="前缀">{{ (courseCode as any).tempCodePrefix }}</el-descriptions-item>
        <el-descriptions-item label="有效期（天）">{{ (courseCode as any).tempCodeTtlDays }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
