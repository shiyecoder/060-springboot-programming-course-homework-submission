<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>我的提交</h1>
        <p class="sub">{{ headSub }}</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </div>

    <!-- 顶部 3 个统计：提交总数 / 平均通过率 / 迟交数 -->
    <div class="kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:14px">
      <div class="kpi">
        <div class="kpi-l">提交总数</div>
        <div class="kpi-v">{{ statTotal }}<small>次</small></div>
        <div class="kpi-f">同一作业多次提交只保留最新有效版本</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">平均用例通过率</div>
        <div class="kpi-v">{{ avgPass }}<small>%</small></div>
        <div class="kpi-f">按「通过用例数 / 用例总数」逐条平均</div>
      </div>
      <div class="kpi" :class="{ warn: lateCount > 0 }">
        <div class="kpi-l">迟交次数</div>
        <div class="kpi-v">{{ lateCount }}<small>次</small></div>
        <div class="kpi-f">迟交仍可提交，但会计入时效标记</div>
      </div>
    </div>

    <!-- 筛选区：课程下拉 200px，输入框 220px -->
    <div class="filter-bar">
      <el-select v-model="query.courseId" placeholder="全部课程" clearable style="width:200px" @change="search">
        <el-option v-for="c in courses" :key="c.id" :label="c.courseName" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="spacer"></span>
      <span style="font-size:12px;color:var(--text-2)">提交编号前缀 TD，按提交时间倒序</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" size="default" empty-text="暂无提交记录">
        <el-table-column label="提交编号">
          <template #default="{ row }">
            <span class="mono">{{ row.submitNo || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="作业">
          <template #default="{ row }">
            <div>{{ row.assignmentTitle || '—' }}</div>
            <div style="font-size:11px;color:var(--text-2)">{{ row.courseName || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="codeLanguage" label="语言" />
        <el-table-column label="提交时间">
          <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="代码行数">
          <template #default="{ row }">
            <span class="mono">{{ row.codeLines ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="编译结果">
          <template #default="{ row }">
            <span class="tag" :class="compileTag(row.compileResult)">{{ row.compileResult || '未运行' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用例通过率">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px;min-width:120px">
              <el-progress
                :percentage="passRate(row)"
                :stroke-width="6"
                :show-text="false"
                :color="rateColor(passRate(row))"
                style="flex:1"
              />
              <span class="mono" style="font-size:12px">{{ row.passCount ?? 0 }}/{{ row.totalCount ?? 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时效">
          <template #default="{ row }">
            <span class="tag" :class="row.isLate === 1 ? 't-amber' : 't-green'">
              {{ row.isLate === 1 ? '迟交' : '按时' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="互评">
          <template #default="{ row }">
            <span v-if="!row.peerCount" class="tag t-gray">待互评</span>
            <span v-else style="font-size:12px">
              {{ row.peerCount }} 份 · 均分 <b class="mono">{{ row.peerAvg ?? '—' }}</b>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="可信度">
          <template #default="{ row }">
            <template v-if="row.credibility !== null && row.credibility !== undefined">
              <b class="mono">{{ row.credibility }}</b>
              <div style="font-size:11px;color:var(--text-2)">{{ gradeOf(row.credibility) }}</div>
            </template>
            <span v-else style="color:var(--text-2)">—</span>
          </template>
        </el-table-column>
        <el-table-column label="提交状态">
          <template #default="{ row }">
            <span class="tag" :class="statusTag(row.status)">{{ row.status || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <div style="white-space:nowrap">
              <el-button link type="primary" @click="toDetail(row)">查看代码</el-button>
              <el-button link type="primary" @click="toDetail(row)">重新提交</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const query = reactive({ courseId: null })
const courses = ref([])

// 统计用全量样本（该接口最多返回 100 条，学生本人数据量足够覆盖）
const statList = ref([])
const statTotal = ref(0)

const headSub = computed(() => {
  const who = store.user.realName || store.user.username || '本人'
  return `${who} · 共 ${statTotal.value} 次提交记录 · 通过率与互评数据实时同步`
})

const avgPass = computed(() => {
  if (!statList.value.length) return '0.0'
  const sum = statList.value.reduce((acc, r) => acc + passRate(r), 0)
  return (sum / statList.value.length).toFixed(1)
})

const lateCount = computed(() => statList.value.filter((r) => r.isLate === 1).length)

/** 用例通过率：passCount / totalCount，防御 totalCount 为 0 */
function passRate(row) {
  const t = Number(row?.totalCount || 0)
  if (!t) return 0
  const p = Number(row?.passCount || 0)
  return Math.min(100, Math.max(0, Math.round((p * 100) / t)))
}

/** 通过率色阶：<60 红，60-84 橙，≥85 绿（与系统同一含义跨页同色） */
function rateColor(v) {
  if (v >= 85) return '#2B7955'
  if (v >= 60) return '#C2703A'
  return '#B02A37'
}

function compileTag(v) {
  if (v === '通过') return 't-green'
  if (v === '编译失败') return 't-red'
  return 't-gray'
}

function statusTag(v) {
  const map = { 待登记: 't-gray', 已登记: 't-blue', 已进入互评: 't-amber', 已评分: 't-green' }
  return map[v] || 't-gray'
}

/** 可信度等级：高≥85 / 中 65-84 / 较低 45-64 / 低 <45 */
function gradeOf(v) {
  const n = Number(v)
  if (isNaN(n)) return '—'
  if (n >= 85) return '高可信'
  if (n >= 65) return '中可信'
  if (n >= 45) return '较低可信'
  return '低可信'
}

async function load() {
  loading.value = true
  try {
    const d = await api.mySubmissions({
      page: page.value,
      size: size.value,
      courseId: query.courseId || undefined
    })
    rows.value = d?.records || []
    total.value = d?.total || 0
  } catch (e) {
    // 接口异常时保持空表，不抛出到控制台
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const d = await api.mySubmissions({ page: 1, size: 100, courseId: query.courseId || undefined })
    statList.value = d?.records || []
    statTotal.value = d?.total ?? statList.value.length
  } catch (e) {
    statList.value = []
    statTotal.value = 0
  }
}

async function loadCourses() {
  try {
    const d = await api.options()
    courses.value = d?.courses || []
  } catch (e) {
    courses.value = []
  }
}

function search() {
  page.value = 1
  load()
  loadStats()
}

function reset() {
  query.courseId = null
  page.value = 1
  load()
  loadStats()
}

function onSizeChange() {
  page.value = 1
  load()
}

function refresh() {
  load()
  loadStats()
}

/** 查看详情 / 重新提交统一跳作业详情（详情页内含代码与重交通道） */
function toDetail(row) {
  if (!row?.assignmentId) return
  router.push(`/my/submit/${row.assignmentId}`)
}

onMounted(() => {
  loadCourses()
  load()
  loadStats()
})
</script>

<style scoped>
.page-head h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.page-head .sub {
  color: var(--text-2);
  font-size: 12px;
  margin: 4px 0 0;
}
.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
