<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>教学工作台</h1>
        <p class="sub">
          2026 学年第一学期 · 数据实时统计 · 待审核成绩 {{ d.pendingReview || 0 }} 份 · 待处理申诉 {{ d.pendingAppeal || 0 }} 条
        </p>
      </div>
      <div class="acts head-actions">
        <el-button :loading="loading" @click="load">刷新数据</el-button>
        <el-button type="primary" @click="$router.push('/assignment/new')">发布作业</el-button>
      </div>
    </div>

    <!-- 8 个核心指标：与文档第 9 节口径一致，全部取自 /dashboard -->
    <div class="kpis cols8" style="margin-bottom:12px">
      <div class="kpi accent">
        <div class="kpi-l">待审核成绩</div>
        <div class="kpi-v">{{ d.pendingReview ?? 0 }}<small>份</small></div>
        <div class="kpi-f">互评与师评已合成</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">代码提交率</div>
        <div class="kpi-v">{{ n1(d.submitRate) }}<small>%</small></div>
        <div class="kpi-f">实交 / 应交</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">迟交提交</div>
        <div class="kpi-v">{{ d.lateCount ?? 0 }}<small>份</small></div>
        <div class="kpi-f">截止后窗口内提交</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">用例平均通过率</div>
        <div class="kpi-v">{{ n1(d.avgPassRate) }}<small>%</small></div>
        <div class="kpi-f">仅统计编译通过</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">互评完成率</div>
        <div class="kpi-v">{{ n1(d.peerFinishRate) }}<small>%</small></div>
        <div class="kpi-f">已评 / 已分配</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">低质量评语</div>
        <div class="kpi-v">{{ d.lowQuality ?? 0 }}<small>条</small></div>
        <div class="kpi-f">不足 15 字或套话</div>
      </div>
      <div class="kpi warn">
        <div class="kpi-l">相似度告警</div>
        <div class="kpi-v">{{ d.similarityAlert ?? 0 }}<small>条</small></div>
        <div class="kpi-f">相似度 &gt; 70 待复核</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">待处理申诉</div>
        <div class="kpi-v">{{ d.pendingAppeal ?? 0 }}<small>条</small></div>
        <div class="kpi-f">待受理 + 处理中</div>
      </div>
    </div>

    <!-- 7 张图：两行 4 列栅格，图表高 168px，1440×900 一屏不滚动 -->
    <div class="dash">
      <div class="chart-card span2">
        <h4>各作业提交率与迟交率</h4>
        <Chart v-if="optSubmit" :option="optSubmit" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>用例通过数分布</h4>
        <Chart v-if="optPass" :option="optPass" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>成绩分段占比</h4>
        <Chart v-if="optScore" :option="optScore" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>互评总分与教师评分</h4>
        <Chart v-if="optPeer" :option="optPeer" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>相似度区间分布</h4>
        <Chart v-if="optSim" :option="optSim" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>互评可信度等级占比</h4>
        <Chart v-if="optGrade" :option="optGrade" />
        <el-empty v-else description="暂无数据" :image-size="52" />
      </div>

      <div class="chart-card">
        <h4>近 6 个月提交趋势</h4>
        <Chart v-if="optTrend" :option="optTrend" />
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

// 图表统一配色，与 theme.css 设计令牌同源，同含义跨页同色
const C = ['#1E4B3F', '#3D7A66', '#C2703A', '#A9B7B0', '#B02A37']
const AXIS = { axisLine: { lineStyle: { color: '#DEE4E9' } }, axisTick: { show: false }, axisLabel: { fontSize: 10, color: '#5B6770' } }
const SPLIT = { splitLine: { lineStyle: { color: '#EDF0F3' } } }
const GRID = { left: 4, right: 8, top: 26, bottom: 2, containLabel: true }
const LEGEND = { top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10, color: '#5B6770' } }

const loading = ref(false)
const raw = ref({})
const d = computed(() => raw.value || {})

const n1 = (v) => (v === null || v === undefined ? '0.0' : Number(v).toFixed(1))

async function load() {
  loading.value = true
  try {
    raw.value = (await api.dashboard()) || {}
  } catch (e) {
    // 接口异常时保持空看板，不阻断页面渲染
    raw.value = {}
  } finally {
    loading.value = false
  }
}

/** 长标题在窄图上截断，避免坐标轴互相重叠 */
const short = (s, len = 9) => {
  const t = String(s || '')
  return t.length > len ? t.slice(0, len) + '…' : t
}

// 图 1：各作业提交率（分母=课程学生数）与迟交率（分母=实交数）双柱
const optSubmit = computed(() => {
  const rows = d.value.submitByAssignment || []
  if (!rows.length) return null
  return {
    color: [C[1], C[2]],
    grid: { ...GRID, top: 24 },
    legend: LEGEND,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: rows.map((r) => short(r.name, 8)), ...AXIS },
    yAxis: { type: 'value', max: 100, ...AXIS, ...SPLIT },
    series: [
      { name: '提交率', type: 'bar', barWidth: 9, data: rows.map((r) => Number(r.rate ?? 0)) },
      { name: '迟交率', type: 'bar', barWidth: 9, data: rows.map((r) => Number(r.lateRate ?? 0)) }
    ]
  }
})

// 图 2：用例通过数分布（按通过百分比分桶的直方图）
const optPass = computed(() => {
  const rows = d.value.passDistribution || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, bottom: 8 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: rows.map((r) => String(r.name).replace('分', '')),
      ...AXIS,
      axisLabel: { fontSize: 9, color: '#5B6770', rotate: 40, interval: 0 }
    },
    yAxis: { type: 'value', ...AXIS, ...SPLIT },
    series: [
      {
        name: '提交数',
        type: 'bar',
        barWidth: '62%',
        itemStyle: { color: C[1], borderRadius: [2, 2, 0, 0] },
        data: rows.map((r) => Number(r.value ?? 0))
      }
    ]
  }
})

// 图 3：成绩分段环形图，固定「高分段→不及格」顺序保证颜色语义稳定
const BAND_ORDER = ['90以上', '80-89', '70-79', '60-69', '不及格']
const optScore = computed(() => {
  const rows = d.value.scoreBand || []
  if (!rows.length) return null
  const data = BAND_ORDER.map((name) => {
    const hit = rows.find((r) => r.name === name)
    return { name, value: hit ? Number(hit.value) : 0 }
  })
  return {
    color: C,
    tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
    legend: { bottom: 0, itemWidth: 7, itemHeight: 7, textStyle: { fontSize: 9, color: '#5B6770' }, itemGap: 6 },
    series: [
      {
        type: 'pie',
        radius: ['46%', '68%'],
        center: ['50%', '42%'],
        avoidLabelOverlap: true,
        label: { show: false },
        data
      }
    ]
  }
})

// 图 4：互评总分 vs 教师评分散点，越靠对角线说明两者越一致
const optPeer = computed(() => {
  const rows = d.value.peerVsTeacher || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, top: 12 },
    tooltip: {
      trigger: 'item',
      formatter: (p) => `互评 ${p.value[0]} 分 / 教师 ${p.value[1]} 分`
    },
    xAxis: { type: 'value', min: 0, max: 100, ...AXIS, ...SPLIT, name: '互评', nameTextStyle: { fontSize: 9, color: '#5B6770' } },
    yAxis: { type: 'value', min: 0, max: 100, ...AXIS, ...SPLIT, name: '教师', nameTextStyle: { fontSize: 9, color: '#5B6770' } },
    series: [
      {
        type: 'scatter',
        symbolSize: 6,
        itemStyle: { color: 'rgba(61,122,102,.68)', borderColor: C[0], borderWidth: 1 },
        data: rows.map((r) => [Number(r.peer ?? 0), Number(r.teacher ?? 0)])
      }
    ]
  }
})

// 图 5：相似度区间分布，70 以上（告警区）用橙/红强调
const optSim = computed(() => {
  const rows = d.value.similarityBand || []
  if (!rows.length) return null
  return {
    grid: { ...GRID, top: 8, bottom: 8 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', ...AXIS, ...SPLIT },
    yAxis: { type: 'category', data: rows.map((r) => r.name), ...AXIS },
    series: [
      {
        name: '对数',
        type: 'bar',
        barWidth: 12,
        data: rows.map((r, i) => ({
          value: Number(r.value ?? 0),
          itemStyle: { color: i >= 4 ? C[4] : i >= 3 ? C[2] : C[1], borderRadius: [0, 2, 2, 0] }
        })),
        label: { show: true, position: 'right', fontSize: 9, color: '#5B6770' }
      }
    ]
  }
})

// 图 6：可信度等级堆叠柱（单一分组，直观展示高/中/较低/低的可信度构成）
const GRADE_ORDER = [
  { key: '高可信', color: C[0] },
  { key: '中可信', color: C[1] },
  { key: '较低可信', color: C[2] },
  { key: '低可信', color: C[4] }
]
const optGrade = computed(() => {
  const rows = d.value.gradeDist || []
  if (!rows.length) return null
  const val = (name) => {
    const hit = rows.find((r) => r.name === name)
    return hit ? Number(hit.value) : 0
  }
  return {
    grid: { ...GRID, top: 26, bottom: 4 },
    legend: LEGEND,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'category', data: ['互评样本'], ...AXIS },
    yAxis: { type: 'value', ...AXIS, ...SPLIT },
    series: GRADE_ORDER.map((g, i) => ({
      name: g.key,
      type: 'bar',
      stack: 'grade',
      barWidth: 34,
      itemStyle: { color: g.color, borderRadius: i === GRADE_ORDER.length - 1 ? [3, 3, 0, 0] : 0 },
      label: { show: true, fontSize: 9, color: '#fff' },
      data: [val(g.key)]
    }))
  }
})

// 图 7：近 6 个月提交量与迟交量趋势
const optTrend = computed(() => {
  const rows = d.value.trend || []
  if (!rows.length) return null
  return {
    color: [C[0], C[2]],
    grid: { ...GRID, top: 24 },
    legend: LEGEND,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', boundaryGap: false, data: rows.map((r) => r.name), ...AXIS },
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

onMounted(load)
</script>

<style scoped>
/* theme.css 只给了 .page-head h2 的字号，这里把规范要求的 h1 对齐同一视觉层级 */
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
:deep(.el-empty) { padding: 6px 0; }
:deep(.el-empty__description p) { font-size: 12px; }
</style>
