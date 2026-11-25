<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>互评进度</h1>
        <p class="sub">按作业统计互评完成情况 · 超时未评计入完成率，完成率因子按 0 计</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 4 个汇总 KPI -->
    <div class="kpis" style="margin-bottom:14px">
      <div class="kpi">
        <div class="kpi-l">总任务数</div>
        <div class="kpi-v">{{ sum.total }}<small>条</small></div>
        <div class="kpi-f">覆盖 {{ rows.length }} 个互评作业</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">整体完成率</div>
        <div class="kpi-v">{{ overallRate }}<small>%</small></div>
        <div class="kpi-f">已评 {{ sum.done }} 条</div>
      </div>
      <div class="kpi" :class="{ warn: sum.timeout > 0 }">
        <div class="kpi-l">超时未评</div>
        <div class="kpi-v">{{ sum.timeout }}<small>条</small></div>
        <div class="kpi-f">拉低评阅人完成率因子</div>
      </div>
      <div class="kpi" :class="{ accent: sum.lowQuality > 0 }">
        <div class="kpi-l">低质量评语</div>
        <div class="kpi-v">{{ sum.lowQuality }}<small>条</small></div>
        <div class="kpi-f">少于 20 字或疑似套话</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="assignmentId" placeholder="全部作业" clearable style="width:200px" @change="page = 1">
        <el-option v-for="r in rows" :key="r.id" :label="r.title" :value="r.id" />
      </el-select>
      <el-button type="primary" @click="page = 1">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="spacer"></span>
      <span style="font-size:12px;color:var(--text-2)">统计口径：已评 / 任务总数，超时未评不入已评</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="pagedRows" size="default" empty-text="暂无互评任务">
        <el-table-column label="作业">
          <template #default="{ row }">
            <div>{{ row.title }}</div>
            <div style="font-size:11px;color:var(--text-2)">互评 {{ row.peerReviewCount }} 份/人</div>
          </template>
        </el-table-column>
        <el-table-column prop="courseName" label="课程" />
        <el-table-column label="任务总数">
          <template #default="{ row }"><span class="mono">{{ row.total ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="已评">
          <template #default="{ row }"><span class="mono">{{ row.done ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="超时未评">
          <template #default="{ row }">
            <span class="tag" :class="(row.timeout ?? 0) > 0 ? 't-red' : 't-gray'">{{ row.timeout ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已豁免">
          <template #default="{ row }"><span class="mono">{{ row.exempt ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="完成率">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px;min-width:130px">
              <el-progress
                :percentage="Number(Number(row.rate ?? 0).toFixed(1))"
                :stroke-width="6"
                :show-text="false"
                :color="rateColor(Number(row.rate ?? 0))"
                style="flex:1"
              />
              <span class="mono" style="font-size:12px">{{ Number(row.rate ?? 0).toFixed(1) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="低质量评语">
          <template #default="{ row }">
            <b class="mono" :style="{ color: (row.lowQuality ?? 0) > 0 ? 'var(--warn)' : 'inherit' }">
              {{ row.lowQuality ?? 0 }}
            </b>
          </template>
        </el-table-column>
        <el-table-column label="平均分">
          <template #default="{ row }"><span class="mono">{{ row.avgScore ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="作业状态">
          <template #default="{ row }">
            <span class="tag" :class="statusTag(row.status)">{{ row.status }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="filteredRows.length"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'

const loading = ref(false)
const rows = ref([])
const assignmentId = ref(null)
const page = ref(1)
const size = ref(10)

/** peerProgress 返回全量数组（无分页参数），此处按作业筛选 + 前端分页展示 */
const filteredRows = computed(() => {
  if (!assignmentId.value) return rows.value
  return rows.value.filter((r) => Number(r.id) === Number(assignmentId.value))
})

const pagedRows = computed(() => {
  const start = (page.value - 1) * size.value
  return filteredRows.value.slice(start, start + size.value)
})

const sum = computed(() => {
  const acc = { total: 0, done: 0, timeout: 0, lowQuality: 0 }
  for (const r of filteredRows.value) {
    acc.total += Number(r.total ?? 0)
    acc.done += Number(r.done ?? 0)
    acc.timeout += Number(r.timeout ?? 0)
    acc.lowQuality += Number(r.lowQuality ?? 0)
  }
  return acc
})

const overallRate = computed(() => {
  if (!sum.value.total) return '0.0'
  return ((sum.value.done * 100) / sum.value.total).toFixed(1)
})

function rateColor(v) {
  if (v >= 85) return '#2B7955'
  if (v >= 60) return '#C2703A'
  return '#B02A37'
}

function statusTag(v) {
  const map = { 互评中: 't-amber', 评分中: 't-blue', 已归档: 't-green', 已撤回: 't-gray' }
  return map[v] || 't-gray'
}

async function load() {
  loading.value = true
  try {
    const d = await api.peerProgress()
    rows.value = Array.isArray(d) ? d : d?.records || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function reset() {
  assignmentId.value = null
  page.value = 1
  load()
}

onMounted(load)
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
