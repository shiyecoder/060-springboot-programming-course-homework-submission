<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p class="sub">学生 / 教师 / 助教 / 教务 / 管理员统一账号体系 · 共 {{ total }} 个账号 · 停用后立即无法登录</p>
      </div>
      <div class="acts">
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="账号 / 姓名 / 手机号"
        clearable
        style="width:220px"
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.roleId" placeholder="全部角色" clearable style="width:200px" @change="onSearch">
        <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width:200px" @change="onSearch">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button @click="onSearch">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id" style="width:100%">
        <el-table-column prop="username" label="账号" min-width="130">
          <template #default="{ row }"><span class="mono">{{ row.username }}</span></template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="100">
          <template #default="{ row }">{{ row.realName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" min-width="120">
          <!-- 学生账号以 username 存学号；非学生角色不展示学号 -->
          <template #default="{ row }">
            <span class="mono">{{ row.studentNo || (row.roleCode === 'STUDENT' ? row.username : '—') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="100">
          <template #default="{ row }">{{ row.className || '—' }}</template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色" min-width="110">
          <template #default="{ row }">
            <span class="tag" :class="roleTag(row.roleCode)">{{ row.roleName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orgName" label="所属学院/部门" min-width="130">
          <template #default="{ row }">{{ row.orgName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="125">
          <template #default="{ row }"><span class="mono">{{ row.phone || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="130">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <el-switch
                :model-value="row.status === 1"
                :loading="row.__switching"
                @change="(v) => changeStatus(row, v)"
              />
              <span class="tag" :class="row.status === 1 ? 't-green' : 't-gray'">
                {{ row.status === 1 ? '正常' : '停用' }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" min-width="150">
          <template #default="{ row }">{{ fmtTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link size="small" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的账号" :image-size="72" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSizeChange"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 新增 / 编辑抽屉：password 仅新增时出现 -->
    <el-drawer v-model="drawer" :title="isEdit ? '编辑用户' : '新增用户'" size="440px" destroy-on-close>
      <el-form label-width="88px" label-position="right">
        <el-form-item label="账号" required>
          <el-input v-model="form.username" :disabled="isEdit" placeholder="学生填学号，教师填工号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="name@univ.edu.cn" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select v-model="form.deptId" placeholder="请选择学院/部门" clearable style="width:100%">
            <el-option v-for="o in orgs" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称/年级">
          <el-input v-model="form.title" placeholder="如 副教授 / 2023级" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码">
          <el-input v-model="form.password" placeholder="默认 123456" show-password />
        </el-form-item>
      </el-form>
      <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:18px">
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </div>
    </el-drawer>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const roles = ref([])
const drawer = ref(false)
const isEdit = ref(false)
const editId = ref(null)

// 组织数据无独立下拉接口，使用初始化内置的 5 个学院/部门，避免额外造接口
const orgs = [
  { id: 1, name: '计算机学院' },
  { id: 2, name: '软件学院' },
  { id: 3, name: '教务处' },
  { id: 4, name: '教学运行科' },
  { id: 5, name: '质量监控科' }
]

const query = reactive({
  keyword: '',
  roleId: null,
  status: null,
  page: 1,
  size: 10
})

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  roleId: null,
  deptId: null,
  title: '',
  password: '123456'
})


function roleTag(code) {
  if (code === 'ADMIN') return 't-red'
  if (code === 'ACADEMIC') return 't-amber'
  if (code === 'TEACHER') return 't-blue'
  if (code === 'TA') return 't-green'
  return 't-gray'
}

async function load() {
  loading.value = true
  try {
    const d = await api.userPage({
      page: query.page,
      size: query.size,
      keyword: query.keyword.trim() || undefined,
      roleId: query.roleId || undefined,
      status: query.status === null || query.status === '' ? undefined : query.status
    })
    rows.value = (d?.records || []).map((r) => ({ ...r, __switching: false }))
    total.value = d?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    const d = await api.roleList()
    roles.value = Array.isArray(d) ? d : []
  } catch (e) {
    roles.value = []
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onSizeChange() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.roleId = null
  query.status = null
  query.page = 1
  load()
}

function resetForm() {
  Object.assign(form, {
    username: '',
    realName: '',
    phone: '',
    email: '',
    roleId: null,
    deptId: null,
    title: '',
    password: '123456'
  })
}

function openCreate() {
  isEdit.value = false
  editId.value = null
  resetForm()
  drawer.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    username: row.username || '',
    realName: row.realName || '',
    phone: row.phone || '',
    email: row.email || '',
    roleId: row.roleId ?? null,
    deptId: row.deptId ?? null,
    title: row.title || '',
    password: ''
  })
  drawer.value = true
}

async function submit() {
  if (!form.username.trim()) {
    ElMessage.warning('请填写登录账号')
    return
  }
  if (!form.realName.trim()) {
    ElMessage.warning('请填写真实姓名')
    return
  }
  if (!form.roleId) {
    ElMessage.warning('请选择角色')
    return
  }
  if (!isEdit.value && String(form.password || '').length < 6) {
    ElMessage.warning('初始密码至少 6 位')
    return
  }
  saving.value = true
  try {
    // 后端新增/修改使用 orgId 接收组织，这里同时带 deptId 兼容字段命名
    const payload = {
      realName: form.realName.trim(),
      phone: form.phone.trim(),
      email: form.email.trim(),
      roleId: form.roleId,
      deptId: form.deptId,
      orgId: form.deptId,
      title: form.title.trim()
    }
    if (isEdit.value) {
      await api.userUpdate(editId.value, payload)
      ElMessage.success('账号资料已更新')
    } else {
      await api.userCreate({ ...payload, username: form.username.trim(), password: form.password })
      ElMessage.success('账号已创建，初始密码为填写的密码')
    }
    drawer.value = false
    load()
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    saving.value = false
  }
}

/** 状态切换：停用属于影响登录的操作，先二次确认；取消则不改动 */
async function changeStatus(row, val) {
  if (!val) {
    try {
      await ElMessageBox.confirm(
        `确认停用账号 ${row.username}？停用后该账号将无法登录系统。`,
        '停用账号',
        { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' }
      )
    } catch (e) {
      return
    }
  }
  row.__switching = true
  try {
    await api.userUpdate(row.id, { status: val ? 1 : 0 })
    row.status = val ? 1 : 0
    ElMessage.success(val ? '账号已启用' : '账号已停用')
  } catch (e) {
    /* 失败时开关保持原值 */
  } finally {
    row.__switching = false
  }
}

async function resetPwd(row) {
  try {
    await ElMessageBox.confirm(
      `确认将账号 ${row.username} 的密码重置为初始密码 123456？该操作会解除登录锁定。`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await api.userReset(row.id)
    ElMessage.success('密码已重置为 123456')
  } catch (e) {
    /* 接口层已提示 */
  }
}

onMounted(() => {
  load()
  loadRoles()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
</style>
