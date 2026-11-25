<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>作业台账</h1>
        <p class="sub">共 {{ total }} 个作业 · 状态机：草稿 → 已发布 → 提交中 → 互评中 → 评分中 → 已归档</p>
      </div>
      <div class="acts head-actions">
        <el-button type="primary" @click="$router.push('/assignment/new')">发布作业</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.courseId" placeholder="全部课程" clearable filterable style="width:200px" @change="search">
        <el-option v-for="c in courses" :key="c.id" :label="`${c.courseName} · ${c.className}`" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width:200px" @change="search">
        <el-option v-for="s in statuses" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.languageType" placeholder="全部语言" clearable style="width:200px" @change="search">
        <el-option v-for="l in langs" :key="l.value" :label="l.label" :value="l.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="作业编号 / 标题"
        clearable
        style="width:220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id" empty-text="暂无作业">
        <el-table-column label="作业编号">
          <template #default="{ row }"><span class="mono">{{ row.assignmentNo }}</span></template>
        </el-table-column>
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="课程 / 教学班">
          <template #default="{ row }">
            <div>{{ row.courseName }}</div>
            <div class="muted">{{ row.className }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="languageType" label="语言" />
        <el-table-column label="截止时间">
          <template #default="{ row }">{{ fmtTime(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="用例 / 互评">
          <template #default="{ row }">{{ row.testCaseCount }} 条 / {{ row.peerReviewCount }} 份</template>
        </el-table-column>
        <el-table-column label="提交进度" min-width="150">
          <template #default="{ row }">
            <div class="prog">
              <span class="mono">{{ row.submitCount ?? 0 }}/{{ row.studentCount ?? 0 }}</span>
              <el-progress :percentage="rate(row)" :stroke-width="5" :show-text="false" style="flex:1" />
              <span class="pct">{{ rate(row) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="迟交" align="right">
          <template #default="{ row }">
            <span :class="row.lateCount > 0 ? 'late' : 'muted'">{{ row.lateCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <span class="tag" :class="statusCls(row.status)">{{ row.status || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <div class="ops">
              <el-button link type="primary" @click="goDetail(row)">详情</el-button>
              <el-button v-if="nextOf(row.status)" link type="primary" @click="doTransition(row)">流转</el-button>
              <el-button link type="primary" @click="$router.push({ path: '/result/entry', query: { assignmentId: row.id } })">
                登记
              </el-button>
            </div>
          </template>
        </el-table-column>
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const statuses = ['草稿', '已发布', '提交中', '互评中', '评分中', '已归档', '已撤回']
const courses = ref([])
const langs = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, courseId: null, status: '', languageType: '', keyword: '' })

/** 状态色与中文双通道：草稿灰、进行中蓝、评分中橙、归档绿、撤回红 */
function statusCls(s) {
  const map = { 草稿: 't-gray', 已发布: 't-blue', 提交中: 't-blue', 互评中: 't-amber', 评分中: 't-amber', 已归档: 't-green', 已撤回: 't-red' }
  return map[s] || 't-gray'
}

/** 提交进度：分母为课程学生数，0 人时按 0% 处理避免除零 */
function rate(row) {
  const all = Number(row.studentCount || 0)
  if (!all) return 0
  return Math.min(100, Math.round((Number(row.submitCount || 0) * 100) / all))
}

/** 与后端 FLOW 一致的下一个自然流转目标（撤回需填原因，放在详情页做） */
function nextOf(s) {
  return { 草稿: '已发布', 已发布: '提交中', 提交中: '互评中', 互评中: '评分中', 评分中: '已归档', 已撤回: '已发布' }[s] || ''
}

async function load() {
  loading.value = true
  try {
    const d = await api.assignmentPage({ ...query })
    rows.value = d?.records || []
    total.value = d?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}
function reset() {
  query.courseId = null
  query.status = ''
  query.languageType = ''
  query.keyword = ''
  search()
}
function onSizeChange() {
  query.page = 1
  load()
}
function goDetail(row) {
  router.push(`/assignment/detail/${row.id}`)
}

/** 行内流转：二次确认文案复述作业编号，避免误操作 */
async function doTransition(row) {
  const target = nextOf(row.status)
  if (!target) return
  try {
    await ElMessageBox.confirm(
      `确认将作业 ${row.assignmentNo} 从「${row.status}」流转到「${target}」？`,
      '作业状态流转',
      { type: 'warning', confirmButtonText: '确认流转', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await api.transition(row.id, { target })
    ElMessage.success(`已流转到「${target}」`)
    load()
  } catch (e) {
    // 业务提示已由拦截器弹出
  }
}

async function loadOptions() {
  try {
    const o = await api.options()
    courses.value = o?.courses || []
    langs.value = o?.languages || []
  } catch (e) {
    courses.value = []
    langs.value = []
  }
}

onMounted(() => {
  // 从课程页带过来的 courseId 直接作为筛选条件
  const cid = Number(route.query.courseId)
  if (cid) query.courseId = cid
  loadOptions()
  load()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.muted { color: #5B6770; font-size: 12px; }
.late { color: #B02A37; font-weight: 600; font-variant-numeric: tabular-nums; }
.prog { display: flex; align-items: center; gap: 8px; }
.prog .mono { font-size: 12px; color: #1F2933; }
.pct { font-size: 11px; color: #5B6770; width: 32px; text-align: right; }
.ops { display: flex; gap: 2px; white-space: nowrap; }
</style>
