<script setup lang="ts">
// MVP-1 第一批 P0 阶段仅暴露最简路由：登录、当前用户、看板骨架。
// 业务模块（M-05 / M-07 等）随后续 PR 接入。
import { RouterView } from 'vue-router'
</script>

<template>
  <el-config-provider>
    <RouterView />
  </el-config-provider>
</template>
