<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>操作日志</h1>
        <p class="sub">关键业务操作留痕 · 按时间倒序 · 日志只读，不允许页面修改或删除</p>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.module" placeholder="全部模块" clearable style="width:200px" @change="onSearch">
        <el-option v-for="m in MODULES" :key="m" :label="m" :value="m" />
      </el-select>
      <el-select v-model="query.operType" placeholder="全部操作类型" clearable style="width:200px" @change="onSearch">
        <el-option v-for="t in OPER_TYPES" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="操作人 / 内容 / IP"
        clearable
        style="width:220px"
        @keyup.enter="onSearch"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id" style="width:100%">
        <el-table-column prop="module" label="模块" min-width="110">
          <template #default="{ row }">
            <span class="tag t-gray">{{ row.module || '系统' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="operType" label="操作类型" min-width="105">
          <template #default="{ row }">{{ row.operType || row.action || '—' }}</template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务编号" min-width="150">
          <template #default="{ row }">
            <span class="mono" style="font-size:12px">{{ row.bizNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容摘要" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || row.params || '—' }}</template>
        </el-table-column>
        <el-table-column prop="operName" label="操作人" min-width="100">
          <template #default="{ row }">{{ row.operName || row.userName || '系统' }}</template>
        </el-table-column>
        <el-table-column prop="operTime" label="操作时间" min-width="150">
          <template #default="{ row }">{{ fmtTime(row.operTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" min-width="125">
          <template #default="{ row }"><span class="mono">{{ row.ip || '—' }}</span></template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的操作日志" :image-size="72" />
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
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const MODULES = [
  '用户管理',
  '角色管理',
  '规则配置',
  '可信度',
  '课程管理',
  '作业管理',
  '代码提交',
  '结果登记',
  '互评',
  '互评打分',
  '查重',
  '成绩',
  '申诉处理',
  '系统管理'
]
const OPER_TYPES = ['新增', '修改', '删除', '发布', '重算', '提交', '处理', '重置密码', '人工修正', '分配菜单', '导出']

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({
  module: '',
  operType: '',
  keyword: '',
  page: 1,
  size: 10
})

async function load() {
  loading.value = true
  try {
    // 操作类型后端无独立参数，用 keyword 命中 action 字段；模块按 module 精确过滤
    const d = await api.logPage({
      page: query.page,
      size: query.size,
      module: query.module || undefined,
      keyword: query.operType || query.keyword.trim() || undefined
    })
    rows.value = d?.records || []
    total.value = d?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
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
  query.module = ''
  query.operType = ''
  query.keyword = ''
  query.page = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
</style>
