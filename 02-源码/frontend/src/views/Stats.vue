<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>成绩统计</h1>
        <p class="sub">按课程、教学班、可信度等级三个维度统计 · 口径与成绩明细一致 · 数据实时汇总</p>
      </div>
      <div class="acts head-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button @click="$router.push('/score')">成绩汇总</el-button>
      </div>
    </div>

    <!-- 4 个汇总指标：均由 statsSummary 的数据派生，不写死 -->
    <div class="kpis" style="margin-bottom:12px">
      <div class="kpi">
        <div class="kpi-l">开课课程</div>
        <div class="kpi-v">{{ s.byCourse?.length || 0 }}<small>门</small></div>
        <div class="kpi-f">按课程名称去重</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">教学班</div>
        <div class="kpi-v">{{ s.byClass?.length || 0 }}<small>个</small></div>
        <div class="kpi-f">按班级汇总</div>
      </div>
      <div class="kpi accent">
        <div class="kpi-l">总评平均分</div>
        <div class="kpi-v">{{ avgAll }}<small>分</small></div>
        <div class="kpi-f">按成绩条数加权</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">互评平均可信度</div>
        <div class="kpi-v">{{ avgCred }}<small>分</small></div>
        <div class="kpi-f">高可信 ≥ 85 · 低可信 &lt; 45</div>
      </div>
    </div>

    <div class="dash">
      <div class="chart-card">
        <h4>各课程平均分</h4>
        <Chart v-if="optCourse" :option="optCourse" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>各教学班平均分</h4>
        <Chart v-if="optClass" :option="optClass" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card span2">
        <h4>各课程互评可信度等级（堆叠）</h4>
        <Chart v-if="optGrade" :option="optGrade" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>按月提交量与迟交量</h4>
        <Chart v-if="optLate" :option="optLate" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>各作业用例通过率</h4>
        <Chart v-if="optPass" :option="optPass" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>互评可信度月度趋势</h4>
        <Chart v-if="optCred" :option="optCred" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>申诉类型分布</h4>
        <Chart v-if="optAppeal" :option="optAppeal" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Chart from '@/components/Chart.vue'
import { api } from '@/api'

const C = ['#1E4B3F', '#3D7A66', '#C2703A', '#A9B7B0', '#B02A37']
const AXIS = { axisLine: { lineStyle: { color: '#DEE4E9' } }, axisTick: { show: false }, axisLabel: { fontSize: 10, color: '#5B6770' } }
const SPLIT = { splitLine: { lineStyle: { color: '#EDF0F3' } } }
const GRID = { left: 4, right: 12, top: 26, bottom: 2, containLabel: true }
const LEGEND = { top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: '#5B6770' } }

const loading = ref(false)
const s = ref({})

const short = (v, n = 7) => {
  const t = String(v || '')
  return t.length > n ? t.slice(0, n) + '…' : t
}

/** 总评平均分：以各课程/班级的人次数为权重，避免小样本班级拉偏 */
const avgAll = computed(() => {
  const rows = s.value.byCourse || []
  const sum = rows.reduce((a, r) => a + Number(r.avgScore || 0) * Number(r.value || 0), 0)
  const cnt = rows.reduce((a, r) => a + Number(r.value || 0), 0)
  return cnt ? (sum / cnt).toFixed(1) : '0.0'
})
const avgCred = computed(() => {
  const rows = s.value.credibilityTrend || []
  if (!rows.length) return '0.0'
  return (rows.reduce((a, r) => a + Number(r.value || 0), 0) / rows.length).toFixed(1)
})

// 图 1：各课程平均分（1 位小数，柱顶标值）
const optCourse = computed(() => {
  const rows = s.value.byCourse || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, bottom: 6 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: rows.map((r) => short(r.name, 5)),
      ...AXIS,
      axisLabel: { fontSize: 9, color: '#5B6770', rotate: 30, interval: 0 }
    },
    yAxis: { type: 'value', max: 100, ...AXIS, ...SPLIT },
    series: [
      {
        name: '平均分',
        type: 'bar',
        barWidth: '52%',
        itemStyle: { color: C[1], borderRadius: [3, 3, 0, 0] },
        label: { show: true, position: 'top', fontSize: 9, color: '#5B6770' },
        data: rows.map((r) => Number(r.avgScore ?? 0))
      }
    ]
  }
})

// 图 2：各教学班平均分
const optClass = computed(() => {
  const rows = s.value.byClass || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, bottom: 6 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: rows.map((r) => r.name), ...AXIS },
    yAxis: { type: 'value', max: 100, ...AXIS, ...SPLIT },
    series: [
      {
        name: '平均分',
        type: 'bar',
        barWidth: '46%',
        itemStyle: { color: C[0], borderRadius: [3, 3, 0, 0] },
        label: { show: true, position: 'top', fontSize: 9, color: '#5B6770' },
        data: rows.map((r) => Number(r.avgScore ?? 0))
      }
    ]
  }
})

// 图 3：可信度等级按课程堆叠，同含义跨页同色（高可信恒为深墨绿）
const GRADES = [
  { key: 'high', name: '高可信', color: C[0] },
  { key: 'mid', name: '中可信', color: C[1] },
  { key: 'low', name: '较低可信', color: C[2] },
  { key: 'veryLow', name: '低可信', color: C[4] }
]
const optGrade = computed(() => {
  const rows = s.value.gradeByCourse || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, bottom: 4 },
    legend: LEGEND,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: rows.map((r) => short(r.name, 6)),
      ...AXIS,
      axisLabel: { fontSize: 9, color: '#5B6770', rotate: 22, interval: 0 }
    },
    yAxis: { type: 'value', ...AXIS, ...SPLIT },
    series: GRADES.map((g, i) => ({
      name: g.name,
      type: 'bar',
      stack: 'grade',
      barWidth: '48%',
      itemStyle: { color: g.color, borderRadius: i === GRADES.length - 1 ? [3, 3, 0, 0] : 0 },
      data: rows.map((r) => Number(r[g.key] ?? 0))
    }))
  }
})

// 图 4：按月提交量与迟交量折线（迟交率为派生指标，不额外画柱）
const optLate = computed(() => {
  const rows = s.value.lateByMonth || []
  if (!rows.length) return null
  return {
    color: [C[0], C[2]],
    grid: GRID,
    legend: LEGEND,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((r) => short(r.name, 7)), ...AXIS },
    yAxis: { type: 'value', ...AXIS, ...SPLIT },
    series: [
      {
        name: '提交量',
        type: 'line',
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(30,75,63,.10)' },
        data: rows.map((r) => Number(r.value ?? 0))
      },
      {
        name: '迟交量',
        type: 'line',
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.6, type: 'dashed' },
        data: rows.map((r) => Number(r.late ?? 0))
      }
    ]
  }
})

// 图 5：各作业用例通过率（横向条形，便于放下长标题）
const optPass = computed(() => {
  const rows = s.value.passByAssignment || []
  if (!rows.length) return null
  return {
    grid: { left: 4, right: 34, top: 6, bottom: 2, containLabel: true },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b}<br/>通过率 {c}%' },
    xAxis: { type: 'value', max: 100, ...AXIS, ...SPLIT },
    yAxis: { type: 'category', data: rows.map((r) => short(r.name, 10)), ...AXIS },
    series: [
      {
        name: '通过率',
        type: 'bar',
        barWidth: 9,
        itemStyle: { color: C[1], borderRadius: [0, 3, 3, 0] },
        label: { show: true, position: 'right', fontSize: 9, color: '#5B6770', formatter: '{c}%' },
        data: rows.map((r) => Number(r.value ?? 0))
      }
    ]
  }
})

// 图 6：互评可信度月均趋势（scale 打开，波动更直观）
const optCred = computed(() => {
  const rows = s.value.credibilityTrend || []
  if (!rows.length) return null
  return {
    grid: GRID,
    tooltip: { trigger: 'axis', formatter: '{b}<br/>平均可信度 {c}' },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((r) => r.name), ...AXIS },
    yAxis: { type: 'value', scale: true, min: 40, max: 100, ...AXIS, ...SPLIT },
    series: [
      {
        name: '平均可信度',
        type: 'line',
        smooth: true,
        symbolSize: 5,
        itemStyle: { color: C[0] },
        lineStyle: { width: 2 },
        areaStyle: { color: 'rgba(61,122,102,.12)' },
        label: { show: true, fontSize: 9, color: '#5B6770' },
        data: rows.map((r) => Number(r.value ?? 0))
      }
    ]
  }
})

// 图 7：申诉类型环形图
const optAppeal = computed(() => {
  const rows = s.value.appealByType || []
  if (!rows.length) return null
  return {
    color: [C[2], C[1], C[0], C[3], C[4]],
    tooltip: { trigger: 'item', formatter: '{b}：{c} 条（{d}%）' },
    legend: { bottom: 0, itemWidth: 7, itemHeight: 7, textStyle: { fontSize: 9, color: '#5B6770' } },
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '42%'],
        label: { show: false },
        data: rows.map((r) => ({ name: r.name, value: Number(r.value ?? 0) }))
      }
    ]
  }
})

async function load() {
  loading.value = true
  try {
    s.value = (await api.statsSummary()) || {}
  } catch (e) {
    s.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
</style>
