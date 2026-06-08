<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { userApi, roleApi, type Role, type UserMgmt } from '@/api/user'
import { ElMessage } from 'element-plus'

const users = ref<UserMgmt[]>([])
const roles = ref<Role[]>([])
const filter = ref({ keyword: '', status: '' })
const total = ref(0)

async function refresh() {
  const resp = await userApi.list({
    keyword: filter.value.keyword || undefined,
    status: filter.value.status || undefined,
    page: 1,
    pageSize: 20
  })
  users.value = resp.items
  total.value = resp.total
}

onMounted(async () => {
  await refresh()
  roles.value = await roleApi.list()
})

const newUser = ref({ username: '', displayName: '', email: '', phone: '', initialPassword: '' })
async function createUser() {
  if (!newUser.value.username || !newUser.value.displayName) {
    ElMessage.warning('用户名和显示名不能为空。')
    return
  }
  await userApi.create(newUser.value)
  ElMessage.success('用户创建成功。')
  newUser.value = { username: '', displayName: '', email: '', phone: '', initialPassword: '' }
  await refresh()
}

async function disableUser(u: UserMgmt) {
  await userApi.disable(u.id)
  ElMessage.success('已停用。')
  await refresh()
}

const bindOpen = ref(false)
const bindTarget = ref<UserMgmt | null>(null)
const bindRoles = ref<string[]>([])

async function openBind(u: UserMgmt) {
  bindTarget.value = u
  bindRoles.value = []
  bindOpen.value = true
}

async function submitBind() {
  if (!bindTarget.value) return
  await userApi.bindRoles(bindTarget.value.id,
    bindRoles.value.map((code) => ({ roleCode: code, isPrimary: code === bindRoles.value[0] })))
  ElMessage.success('角色绑定已更新。')
  bindOpen.value = false
}
</script>

<template>
  <div>
    <h2>用户与角色</h2>

    <el-card>
      <el-form :model="filter" inline>
        <el-form-item label="关键字">
          <el-input v-model="filter.keyword" placeholder="用户名 / 姓名 / 邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" clearable>
            <el-option label="ACTIVE" value="ACTIVE" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="refresh">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>用户列表（{{ total }}）</span>
      </template>
      <el-table :data="users" border>
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="displayName" label="显示名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="userType" label="类型" width="100" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="openBind(row)">绑定角色</el-button>
            <el-button size="small" type="danger" @click="disableUser(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header><span>新建用户</span></template>
      <el-form :model="newUser" label-width="100px" inline>
        <el-form-item label="用户名"><el-input v-model="newUser.username" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="newUser.displayName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="newUser.email" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="newUser.phone" /></el-form-item>
        <el-form-item label="初始密码">
          <el-input v-model="newUser.initialPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="createUser">创建</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="bindOpen" title="绑定角色" width="480px">
      <el-form v-if="bindTarget" :model="{ username: bindTarget.username }" label-width="100px">
        <el-form-item label="用户"><el-input :model-value="bindTarget.username" disabled /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="bindRoles" multiple placeholder="选择角色">
            <el-option v-for="r in roles" :key="r.code" :label="`${r.name} (${r.code})`" :value="r.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBind">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
