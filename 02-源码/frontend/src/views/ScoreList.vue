<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>成绩汇总</h1>
        <p class="sub">{{ scopeText }} · 统计口径与明细一致 · 导出会记录导出人与时间</p>
      </div>
      <div class="head-actions">
        <el-button type="primary" @click="exportCsv">导出成绩单</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.assignmentId" placeholder="全部作业" clearable style="width:200px" @change="search">
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-select v-model="query.publishStatus" placeholder="全部发布状态" clearable style="width:200px" @change="search">
        <el-option label="已发布" value="已发布" />
        <el-option label="未发布" value="未发布" />
      </el-select>
      <el-select v-model="query.grade" placeholder="全部可信度等级" clearable style="width:200px" @change="search">
        <el-option v-for="g in grades" :key="g.value" :label="g.label" :value="g.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="姓名 / 学号"
        clearable
        style="width:220px"
        @keyup.enter="search"
      />
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" size="default" empty-text="暂无成绩数据">
        <el-table-column label="学生">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <img
                v-if="row.studentAvatar"
                :src="row.studentAvatar"
                alt="头像"
                style="width:24px;height:24px;border-radius:50%;object-fit:cover"
              />
              <span>{{ row.studentName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学号">
          <template #default="{ row }"><span class="mono">{{ row.studentNo }}</span></template>
        </el-table-column>
        <el-table-column prop="className" label="班级" />
        <el-table-column label="作业">
          <template #default="{ row }">
            <div>{{ row.assignmentTitle }}</div>
            <div style="font-size:11px;color:var(--text-2)">{{ row.courseName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="权重">
          <template #default="{ row }">
            <span class="mono">{{ row.weight ?? '—' }}</span><span style="font-size:11px;color:var(--text-2)">%</span>
          </template>
        </el-table-column>
        <el-table-column label="运行结果">
          <template #default="{ row }"><span class="mono">{{ num(row.autoScore) }}</span></template>
        </el-table-column>
        <el-table-column label="互评">
          <template #default="{ row }">
            <span class="mono">{{ num(row.peerScore) }}</span>
            <div v-if="folded(row)" style="font-size:11px;color:var(--warn)">
              原始 {{ num(row.peerRawScore) }} 已折算
            </div>
          </template>
        </el-table-column>
        <el-table-column label="教师">
          <template #default="{ row }"><span class="mono">{{ num(row.teacherScore) }}</span></template>
        </el-table-column>
        <el-table-column label="最终成绩">
          <template #default="{ row }">
            <b class="mono" :style="{ color: scoreColor(row.finalScore), fontSize: '14px' }">
              {{ num(row.finalScore) }}
            </b>
            <div style="font-size:11px;color:var(--text-2)">班级第 {{ row.classRank ?? '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="可信度">
          <template #default="{ row }">
            <span class="tag" :class="gradeTag(row.credibilityGrade)">{{ row.credibilityGrade || '—' }}</span>
            <div class="mono" style="font-size:11px;color:var(--text-2)">{{ num(row.credibilityScore) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="调整依据">
          <template #default="{ row }">
            <el-tooltip v-if="row.adjustReason" :content="row.adjustReason" placement="top">
              <span style="font-size:12px;color:var(--warn);cursor:help">已人工调整</span>
            </el-tooltip>
            <span v-else style="color:var(--text-2)">—</span>
          </template>
        </el-table-column>
        <el-table-column label="发布状态">
          <template #default="{ row }">
            <span class="tag" :class="row.publishStatus === '已发布' ? 't-green' : 't-gray'">
              {{ row.publishStatus || '未发布' }}
            </span>
            <div v-if="row.publishTime" style="font-size:11px;color:var(--text-2)">
              {{ fmtTime(row.publishTime) }}
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

    <div class="card" style="margin-top:14px;padding:11px 16px">
      <div style="font-size:12px;color:var(--text-2);line-height:1.85">
        <b style="color:var(--text)">导出口径</b>：导出当前页 {{ rows.length }} 条，字段含学号、姓名、班级、作业、权重、
        运行结果分、互评分（折算后）、教师评分、最终成绩、可信度等级与班级排名；
        导出为 UTF-8 CSV（带 BOM），可直接用 Excel 打开。发布后改分必须走申诉流程，导出内容与明细实时一致。
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const assignments = ref([])
const grades = ref([])

const query = reactive({
  assignmentId: null,
  publishStatus: null,
  grade: null,
  keyword: ''
})

/** 数据范围说明：教师仅本人课程，学生仅本人成绩，教务/管理员为全量 */
const scopeText = computed(() => {
  const role = store.user.roleCode
  if (role === 'TEACHER') return '教师视角：仅本人所授课程的成绩'
  if (role === 'STUDENT') return '学生视角：仅本人已发布成绩'
  if (role === 'TA') return '助教视角：仅可查看被授权的课程成绩'
  return '教务 / 管理员视角：全量成绩'
})

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return isNaN(n) ? '—' : n.toFixed(1)
}

/** 最终成绩色阶：≥90 深绿、80-89 绿、70-79 橙、60-69 浅橙、<60 红 */
function scoreColor(v) {
  const n = Number(v)
  if (isNaN(n)) return 'inherit'
  if (n >= 90) return '#1E4B3F'
  if (n >= 80) return '#2B7955'
  if (n >= 70) return '#3D7A66'
  if (n >= 60) return '#9C5C12'
  return '#B02A37'
}

function gradeTag(g) {
  const map = { 高可信: 't-green', 中可信: 't-blue', 较低可信: 't-amber', 低可信: 't-red' }
  return map[g] || 't-gray'
}

/** 互评是否被按可信度等级折算（有原始分且与折算分不同） */
function folded(row) {
  if (row.peerRawScore == null || row.peerScore == null) return false
  return Number(row.peerRawScore) !== Number(row.peerScore)
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
  } catch (e) {
    assignments.value = []
  }
}

async function loadGrades() {
  try {
    const d = await api.options()
    grades.value = d?.grades || []
  } catch (e) {
    grades.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const d = await api.scorePage({
      page: page.value,
      size: size.value,
      assignmentId: query.assignmentId || undefined,
      publishStatus: query.publishStatus || undefined,
      grade: query.grade || undefined,
      keyword: query.keyword ? query.keyword.trim() : undefined
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

function search() {
  page.value = 1
  load()
}

function reset() {
  query.assignmentId = null
  query.publishStatus = null
  query.grade = null
  query.keyword = ''
  page.value = 1
  load()
}

function onSizeChange() {
  page.value = 1
  load()
}

function cell(v) {
  if (v === null || v === undefined) return ''
  return `"${String(v).replace(/"/g, '""')}"`
}

/** 前端导出当前页为 CSV（Blob），不依赖外部依赖 */
function exportCsv() {
  if (!rows.value.length) {
    ElMessage.warning('当前页没有可导出的成绩')
    return
  }
  const header = [
    '学号', '姓名', '班级', '作业', '课程', '权重(%)', '运行结果分',
    '互评原始分', '互评分(折算后)', '教师评分', '最终成绩',
    '可信度分', '可信度等级', '班级排名', '发布状态', '调整依据'
  ]
  const lines = [header.map(cell).join(',')]
  for (const r of rows.value) {
    lines.push([
      r.studentNo, r.studentName, r.className, r.assignmentTitle, r.courseName, r.weight,
      num(r.autoScore), num(r.peerRawScore), num(r.peerScore), num(r.teacherScore), num(r.finalScore),
      num(r.credibilityScore), r.credibilityGrade, r.classRank, r.publishStatus, r.adjustReason
    ].map(cell).join(','))
  }
  const blob = new Blob(['\ufeff' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `成绩单_${new Date().toISOString().slice(0, 10)}_第${page.value}页.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出当前页 ${rows.value.length} 条成绩`)
}

onMounted(async () => {
  await loadAssignments()
  await loadGrades()
  await load()
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
