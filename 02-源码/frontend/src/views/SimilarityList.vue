<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>查重记录</h1>
        <p class="sub">
          代码相似度 = 结构 0.5 + 标识符 0.3 + 注释 0.2 · 超过 70% 触发告警并生成待复核记录
        </p>
      </div>
      <div class="head-actions">
        <el-button type="primary" :loading="checking" @click="doCheck">发起检测</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.assignmentId" placeholder="全部作业" clearable style="width:200px" @change="search">
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-select v-model="query.reviewResult" placeholder="全部复核结论" clearable style="width:200px" @change="search">
        <el-option v-for="s in reviewOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.minRate" placeholder="最低相似度" clearable style="width:200px" @change="search">
        <el-option v-for="r in rateOptions" :key="r" :label="`相似度 ≥ ${r}%`" :value="r" />
      </el-select>
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="spacer"></span>
      <span style="font-size:12px;color:var(--text-2)">按相似度倒序 · 确认抄袭需第二教师签字</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" size="default" empty-text="暂无查重记录，请先发起检测">
        <el-table-column label="记录编号">
          <template #default="{ row }">
            <span class="mono">{{ row.recordNo }}</span>
            <div style="font-size:11px;color:var(--text-2)">{{ row.batchNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="作业">
          <template #default="{ row }">
            <div>{{ row.assignmentTitle }}</div>
            <div style="font-size:11px;color:var(--text-2)">{{ row.languageType }} · {{ row.checkRuleVersion }}</div>
          </template>
        </el-table-column>
        <el-table-column label="提交对">
          <template #default="{ row }">
            <div style="font-size:12.5px">{{ row.studentA }} <span style="color:var(--text-2)">/</span> {{ row.studentB }}</div>
            <div class="mono" style="font-size:11px;color:var(--text-2)">{{ row.noA }} / {{ row.noB }}</div>
          </template>
        </el-table-column>
        <el-table-column label="相似度">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px;min-width:130px">
              <el-progress
                :percentage="Number(Number(row.similarityRate ?? 0).toFixed(2))"
                :stroke-width="6"
                :show-text="false"
                :color="simColor(row.similarityRate)"
                style="flex:1"
              />
              <b class="mono" :style="{ color: Number(row.similarityRate) > 70 ? 'var(--danger)' : 'inherit' }">
                {{ Number(row.similarityRate ?? 0).toFixed(2) }}%
              </b>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="结构">
          <template #default="{ row }"><span class="mono" style="font-size:12px">{{ Number(row.structureRate ?? 0).toFixed(1) }}</span></template>
        </el-table-column>
        <el-table-column label="标识符">
          <template #default="{ row }"><span class="mono" style="font-size:12px">{{ Number(row.identifierRate ?? 0).toFixed(1) }}</span></template>
        </el-table-column>
        <el-table-column label="注释">
          <template #default="{ row }"><span class="mono" style="font-size:12px">{{ Number(row.commentRate ?? 0).toFixed(1) }}</span></template>
        </el-table-column>
        <el-table-column label="命中类型">
          <template #default="{ row }">
            <span class="tag" :class="matchTag(row.matchType)">{{ row.matchType || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="复核结论">
          <template #default="{ row }">
            <span class="tag" :class="reviewTag(row.reviewResult)">{{ row.reviewResult || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测时间">
          <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <div style="white-space:nowrap">
              <el-button link type="primary" @click="toReview(row)">复核</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const checking = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const assignments = ref([])

const reviewOptions = ['待复核', '确认抄袭', '误报', '需面谈']
const rateOptions = [30, 50, 70, 85]

const query = reactive({
  assignmentId: null,
  reviewResult: null,
  minRate: null
})

/** 相似度色阶：>70 告警红，50-70 橙，<50 绿 */
function simColor(v) {
  const n = Number(v)
  if (n > 70) return '#B02A37'
  if (n >= 50) return '#C2703A'
  return '#2B7955'
}

function matchTag(v) {
  const map = { 完全一致: 't-red', 结构一致: 't-amber', 变量重命名: 't-amber', 注释雷同: 't-blue' }
  return map[v] || 't-gray'
}

function reviewTag(v) {
  const map = { 待复核: 't-amber', 确认抄袭: 't-red', 误报: 't-gray', 需面谈: 't-blue' }
  return map[v] || 't-gray'
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
  } catch (e) {
    assignments.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const d = await api.similarityPage({
      page: page.value,
      size: size.value,
      assignmentId: query.assignmentId || undefined,
      reviewResult: query.reviewResult || undefined,
      minRate: query.minRate || undefined
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
  query.reviewResult = null
  query.minRate = null
  page.value = 1
  load()
}

function onSizeChange() {
  page.value = 1
  load()
}

function toReview(row) {
  router.push({ path: '/similarity/review', query: { id: row.id } })
}

/** 发起检测：必须指定作业，两两比对新记录，确认框里复述作业名 */
async function doCheck() {
  if (!query.assignmentId) {
    ElMessage.warning('请先在筛选中选择要检测的作业')
    return
  }
  const a = assignments.value.find((x) => Number(x.id) === Number(query.assignmentId))
  try {
    await ElMessageBox.confirm(
      `将对【${a?.title || query.assignmentId}】的全部提交两两比对，已存在的记录不会重复生成，确认发起检测？`,
      '发起查重检测',
      { type: 'warning', confirmButtonText: '发起检测', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  checking.value = true
  try {
    const d = await api.similarityCheck({ assignmentId: query.assignmentId })
    ElMessage.success(`检测完成：批次 ${d?.batchNo || '—'}，新增 ${d?.count ?? 0} 条记录，告警线 ${d?.alertLine ?? 70}%`)
    page.value = 1
    await load()
  } catch (e) {
    // 失败提示由拦截器统一处理
  } finally {
    checking.value = false
  }
}

onMounted(async () => {
  await loadAssignments()
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
