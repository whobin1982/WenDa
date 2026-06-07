<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { schoolApi, collegeApi, type School, type College } from '@/api/school'
import { ElMessage } from 'element-plus'

const school = ref<School | null>(null)
const colleges = ref<College[]>([])

async function refresh() {
  school.value = await schoolApi.current()
  const resp = await collegeApi.list(1, 20)
  colleges.value = resp.items
}

onMounted(refresh)

const newCollege = ref({ collegeCode: '', name: '', shortName: '', description: '' })
async function createCollege() {
  if (!newCollege.value.collegeCode || !newCollege.value.name) {
    ElMessage.warning('学院代码与名称不能为空。')
    return
  }
  await collegeApi.create(newCollege.value)
  ElMessage.success('学院创建成功。')
  newCollege.value = { collegeCode: '', name: '', shortName: '', description: '' }
  await refresh()
}
</script>

<template>
  <div>
    <h2>学校空间与学院</h2>
    <el-card v-if="school" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="学校代码">{{ school.schoolCode }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ school.name }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ school.shortName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ school.status }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ school.contactEmail || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系手机">{{ school.contactPhone || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>学院列表</span>
      </template>
      <el-table :data="colleges" border>
        <el-table-column prop="collegeCode" label="学院代码" />
        <el-table-column prop="name" label="学院名称" />
        <el-table-column prop="shortName" label="简称" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="version" label="版本" width="80" />
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>新建学院</span></template>
      <el-form :model="newCollege" label-width="100px" inline>
        <el-form-item label="学院代码">
          <el-input v-model="newCollege.collegeCode" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="newCollege.name" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="newCollege.shortName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newCollege.description" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createCollege">创建</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>
