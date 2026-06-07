<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { auditApi } from '@/api/settings'
import type { PageResp } from '@/api/school'

const data = ref<PageResp<Record<string, unknown>> | null>(null)
const filter = ref({ action: '', resourceType: '' })

async function refresh() {
  data.value = await auditApi.search({
    action: filter.value.action || undefined,
    resourceType: filter.value.resourceType || undefined,
    page: 1,
    pageSize: 20
  })
}

onMounted(refresh)
</script>

<template>
  <div>
    <h2>审计日志</h2>
    <el-card>
      <el-form :model="filter" inline>
        <el-form-item label="动作">
          <el-input v-model="filter.action" placeholder="如 CREATE_USER" />
        </el-form-item>
        <el-form-item label="资源类型">
          <el-input v-model="filter.resourceType" placeholder="如 user / school" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card style="margin-top: 16px">
      <el-table v-if="data" :data="data.items" border>
        <el-table-column prop="created_at" label="时间" width="200" />
        <el-table-column prop="action" label="动作" width="180" />
        <el-table-column prop="resource_type" label="资源类型" width="120" />
        <el-table-column prop="resource_id" label="资源 ID" width="240" />
        <el-table-column prop="user_name" label="操作人" width="120" />
        <el-table-column prop="risk_level" label="风险等级" width="100" />
        <el-table-column prop="status_code" label="状态码" width="80" />
        <el-table-column prop="ip" label="IP" width="140" />
      </el-table>
    </el-card>
  </div>
</template>
