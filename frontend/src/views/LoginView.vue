<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const form = ref({ schoolCode: '', username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (!form.value.schoolCode || !form.value.username || !form.value.password) {
    ElMessage.warning('请填写学校代码、用户名和密码。')
    return
  }
  loading.value = true
  try {
    await login(form.value)
    ElMessage.success('登录成功。')
    router.push((router.currentRoute.value.query.redirect as string) || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div class="login-header">登录 Wenda 平台</div>
      </template>
      <el-form :model="form" label-width="100px" @submit.prevent="onSubmit">
        <el-form-item label="学校代码">
          <el-input v-model="form.schoolCode" placeholder="例如：NUAA" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" native-type="submit">登录</el-button>
        </el-form-item>
      </el-form>
      <p class="hint">
        MVP-1 阶段仅内置本地账号；SSO / OAuth 接入由后续 PR 完成。
      </p>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
.login-card {
  width: 420px;
}
.login-header {
  font-size: 18px;
  font-weight: 600;
}
.hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
