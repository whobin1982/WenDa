<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { dashboardApi } from '@/api/settings'

const data = ref<Awaited<ReturnType<typeof dashboardApi.school>> | null>(null)

onMounted(async () => {
  data.value = await dashboardApi.school()
})
</script>

<template>
  <div>
    <h2>学校级看板</h2>
    <p class="hint">MVP-1 看板骨架；专业 / 学院级复杂指标随业务模块就绪接入。</p>
    <el-card v-if="data" style="margin-top: 16px">
      <el-statistic :value="data.metrics.colleges ?? 0" title="学院数" />
      <el-statistic :value="data.metrics.majors ?? 0" title="专业数" style="margin-top: 12px" />
      <el-statistic :value="data.metrics.activeUsers ?? 0" title="活跃用户" style="margin-top: 12px" />
      <el-statistic :value="data.metrics.pendingReviews ?? 0" title="待审核" style="margin-top: 12px" />
    </el-card>
  </div>
</template>
