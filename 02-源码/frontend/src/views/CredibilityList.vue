<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>互评可信度明细</h1>
        <p class="sub">
          一次互评是否可信由 5 项因素加权计算（评分偏离度 25 / 评语有效长度 20 / 互评完成率 20 / 提交相似度 20 / 评分一致性 15）
        </p>
      </div>
      <div class="acts">
        <el-button :loading="recalcLoading" @click="recalcAll">主动重算</el-button>
      </div>
    </div>

    <!-- 筛选：等级 / 作业 / 关键字，下拉统一 200px -->
    <div class="filter-bar">
      <el-select v-model="query.grade" placeholder="可信度等级" clearable style="width:200px" @change="onSearch">
        <el-option v-for="g in GRADES" :key="g" :label="g" :value="g" />
      </el-select>
      <el-select
        v-model="query.assignmentId"
        placeholder="全部作业"
        clearable
        filterable
        style="width:200px"
        @change="onSearch"
      >
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="学生姓名 / 学号 / 快照号"
        clearable
        style="width:220px"
        @keyup.enter="onSearch"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 4 个等级 KPI：数量取各等级分页接口的 total（按当前作业/关键字口径统计） -->
    <div class="kpis" style="margin-bottom:14px">
      <div v-for="k in kpiCards" :key="k.grade" class="kpi" :class="k.cls">
        <div class="kpi-l">{{ k.grade }}</div>
        <div class="kpi-v" :style="{ color: k.color }">
          {{ kpis[k.grade] }}<small>条</small>
        </div>
        <div class="kpi-f">{{ k.foot }}</div>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id" style="width:100%">
        <el-table-column prop="snapshotNo" label="快照号" min-width="160">
          <template #default="{ row }"><span class="mono">{{ row.snapshotNo }}</span></template>
        </el-table-column>
        <el-table-column prop="studentName" label="学生" min-width="110">
          <template #default="{ row }">
            <div>{{ row.studentName || '—' }}</div>
            <div class="mono" style="font-size:11px;color:var(--text-2)">{{ row.studentNo || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="assignmentTitle" label="作业" min-width="210" show-overflow-tooltip />
        <el-table-column prop="ruleVersion" label="规则版本" min-width="90">
          <template #default="{ row }"><span class="mono">{{ row.ruleVersion || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="totalScore" label="可信度分" min-width="100" align="right">
          <template #default="{ row }">
            <b style="font-variant-numeric:tabular-nums">{{ fmtScore(row.totalScore) }}</b>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="等级" min-width="100">
          <template #default="{ row }">
            <span class="tag" :class="gradeTag(row.grade)">{{ row.grade || '未评级' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="manualFlag" label="来源" min-width="110">
          <template #default="{ row }">
            <span class="tag" :class="row.manualFlag ? 't-amber' : 't-gray'">
              {{ row.manualFlag ? '人工修正' : '系统计算' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="calcTime" label="计算时间" min-width="150">
          <template #default="{ row }">{{ fmtTime(row.calcTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="toTrace(row)">查看明细</el-button>
            <el-button link size="small" @click="recalcRow(row)">主动重算</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无可信度快照，请先完成互评打分" :image-size="72" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const router = useRouter()

// 等级口径固定：高 ≥85 / 中 65-84 / 较低 45-64 / 低 <45
const GRADES = ['高可信', '中可信', '较低可信', '低可信']
const GRADE_STYLE = {
  高可信: { tag: 't-green', color: '#1E4B3F', foot: '≥85 分 · 直接计入' },
  中可信: { tag: 't-blue', color: '#3D7A66', foot: '65-84 分 · 0.8 折算' },
  较低可信: { tag: 't-amber', color: '#C2703A', foot: '45-64 分 · 仅参考' },
  低可信: { tag: 't-red', color: '#B02A37', foot: '<45 分 · 不计入' }
}

const loading = ref(false)
const recalcLoading = ref(false)
const rows = ref([])
const total = ref(0)
const assignments = ref([])
const kpis = reactive({ 高可信: 0, 中可信: 0, 较低可信: 0, 低可信: 0 })

const query = reactive({
  grade: '',
  assignmentId: null,
  keyword: '',
  page: 1,
  size: 10
})

const kpiCards = computed(() =>
  GRADES.map((g) => ({
    grade: g,
    cls: g === '高可信' ? '' : g === '低可信' ? 'warn' : g === '较低可信' ? 'accent' : '',
    color: GRADE_STYLE[g].color,
    foot: GRADE_STYLE[g].foot
  }))
)

function gradeTag(g) {
  return GRADE_STYLE[g]?.tag || 't-gray'
}

function fmtScore(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(1) : '—'
}

function onSearch() {
  query.page = 1
  load()
  loadKpis()
}

function onSizeChange() {
  query.page = 1
  load()
}

function reset() {
  query.grade = ''
  query.assignmentId = null
  query.keyword = ''
  query.page = 1
  load()
  loadKpis()
}

/** 列表：后端分页，参数与 credibilityPage 契约一致 */
async function load() {
  loading.value = true
  try {
    const d = await api.credibilityPage({
      page: query.page,
      size: query.size,
      grade: query.grade || undefined,
      assignmentId: query.assignmentId || undefined,
      keyword: query.keyword.trim() || undefined
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

/** KPI：每个等级请求一次 size=1，只取 total，避免前端自己分页统计 */
async function loadKpis() {
  const base = {
    page: 1,
    size: 1,
    assignmentId: query.assignmentId || undefined,
    keyword: query.keyword.trim() || undefined
  }
  await Promise.all(
    GRADES.map(async (g) => {
      try {
        const d = await api.credibilityPage({ ...base, grade: g })
        kpis[g] = d?.total || 0
      } catch (e) {
        kpis[g] = 0
      }
    })
  )
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
  } catch (e) {
    assignments.value = []
  }
}

function toTrace(row) {
  router.push({ path: '/credibility/trace', query: { id: row.id } })
}

/** 单条重算：后端按作业重算，确认文案复述快照号 */
async function recalcRow(row) {
  try {
    await ElMessageBox.confirm(
      `将按现行规则重算快照 ${row.snapshotNo} 所在作业的全部可信度记录，已人工修正的结论会保留。是否继续？`,
      '主动重算',
      { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    const d = await api.recalc({ assignmentId: row.assignmentId })
    ElMessage.success(`重算完成，更新 ${d?.updated ?? d?.count ?? 0} 条`)
    load()
    loadKpis()
  } catch (e) {
    /* 接口层已提示 */
  }
}

/** 全量重算：不传 assignmentId */
async function recalcAll() {
  try {
    await ElMessageBox.confirm(
      '将按现行规则重算全部可信度快照，历史计算快照与人工修正记录都会保留。是否继续？',
      '主动重算',
      { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  recalcLoading.value = true
  try {
    const d = await api.recalc({})
    ElMessage.success(`重算完成，更新 ${d?.updated ?? d?.count ?? 0} 条`)
    load()
    loadKpis()
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    recalcLoading.value = false
  }
}

onMounted(() => {
  load()
  loadKpis()
  loadAssignments()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
</style>
