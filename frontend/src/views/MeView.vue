<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchMe, logout, type AuthUser } from '@/api/auth'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const me = ref<AuthUser | null>(null)

onMounted(async () => {
  me.value = await fetchMe()
})

async function onLogout() {
  await logout()
  ElMessage.success('已退出登录。')
  router.push('/login')
}
</script>

<template>
  <el-page-header :icon="null" title="返回">
    <template #content>
      <span class="title">当前用户</span>
    </template>
  </el-page-header>
  <el-card v-if="me" style="margin-top: 16px">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="账号">{{ me.username }}</el-descriptions-item>
      <el-descriptions-item label="显示名">{{ me.displayName }}</el-descriptions-item>
      <el-descriptions-item label="邮箱">{{ me.email || '—' }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ me.status }}</el-descriptions-item>
      <el-descriptions-item label="学校 ID">{{ me.schoolId }}</el-descriptions-item>
      <el-descriptions-item label="租户 ID">{{ me.tenantId }}</el-descriptions-item>
      <el-descriptions-item label="角色">
        <el-tag v-for="r in me.roles" :key="r" type="info" style="margin-right: 4px">{{ r }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="用户类型">{{ me.userType }}</el-descriptions-item>
    </el-descriptions>
    <el-button type="danger" style="margin-top: 16px" @click="onLogout">退出登录</el-button>
  </el-card>
</template>
