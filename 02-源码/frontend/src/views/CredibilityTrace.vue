<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>可信度计算留痕</h1>
        <p class="sub">
          每次计算都冻结输入快照与规则版本，可逐项复算；后续数据变化不会影响本条结果
        </p>
      </div>
      <div class="acts">
        <el-button @click="back">返回明细</el-button>
        <el-button type="primary" :disabled="!detail" :loading="recalcLoading" @click="recalc">重算</el-button>
      </div>
    </div>

    <!-- 等级如何影响成绩：四档口径固定在页首，避免答辩时口头解释 -->
    <div class="card level-guide">
      <div class="card-title" style="margin-bottom:8px">等级如何影响成绩</div>
      <div class="level-grid">
        <div class="level-item">
          <span class="tag t-green">高可信 ≥85</span>
          <p>互评分直接计入最终成绩。</p>
        </div>
        <div class="level-item">
          <span class="tag t-blue">中可信 65-84</span>
          <p>互评分按 0.8 折算后计入。</p>
        </div>
        <div class="level-item">
          <span class="tag t-amber">较低可信 45-64</span>
          <p>互评分仅作参考，教师评分权重上调至 60%。</p>
        </div>
        <div class="level-item">
          <span class="tag t-red">低可信 &lt;45</span>
          <p>互评分不计入成绩，需追加互评或教师复核。</p>
        </div>
      </div>
    </div>

    <div v-if="!detail && !loading" class="card">
      <el-empty description="暂无可计算的可信度快照，请先完成互评打分" :image-size="80" />
    </div>

    <div v-else v-loading="loading" class="trace-grid">
      <!-- 左栏：结论 + 五因子 -->
      <div>
        <div class="card">
          <div class="score-head">
            <div>
              <div class="score-num" :style="{ color: gradeColor }">{{ scoreText }}</div>
              <div style="display:flex;align-items:center;gap:8px;margin-top:4px">
                <span class="tag" :class="gradeTag">{{ detail?.grade || '未评级' }}</span>
                <span style="font-size:12px;color:var(--text-2)">可信度总分</span>
              </div>
            </div>
            <div style="text-align:right">
              <div class="mono" style="font-size:13px;font-weight:600">{{ detail?.snapshotNo || '—' }}</div>
              <div style="font-size:11.5px;color:var(--text-2);margin-top:3px">快照号</div>
            </div>
          </div>

          <div class="conclusion">{{ detail?.conclusion || '暂无结论说明' }}</div>

          <div class="meta-grid">
            <div><span>学生</span><b>{{ detail?.studentName || '—' }}</b></div>
            <div><span>学号</span><b class="mono">{{ detail?.studentNo || '—' }}</b></div>
            <div><span>作业</span><b>{{ detail?.assignmentTitle || '—' }}</b></div>
            <div><span>互评任务</span><b class="mono">{{ detail?.taskNo || '—' }}</b></div>
            <div><span>规则版本</span><b class="mono">{{ detail?.ruleVersion || '—' }}</b></div>
            <div><span>计算时间</span><b>{{ fmtTime(detail?.calcTime) }}</b></div>
          </div>
        </div>

        <div class="card">
          <h3 class="card-title">
            <span>五因子贡献</span>
            <span style="font-size:12px;color:var(--text-2);font-weight:400">
              贡献分 = 得分 × 权重 / 100
            </span>
          </h3>
          <div v-for="f in factors" :key="f.name" class="factor-row trace-row">
            <div class="fname">
              {{ f.name }}
              <small>{{ fmtScore(f.weight) }}%</small>
              <span v-if="f.name === detail?.mainFactor" class="main-flag">主要影响因素</span>
            </div>
            <div class="ftrack">
              <div class="fbar" :class="barClass(f.score)" :style="{ width: clamp(f.score) + '%' }"></div>
            </div>
            <div class="fval">
              {{ fmtScore(f.score) }}
              <small>贡献 {{ fmtScore(f.contribution) }}</small>
            </div>
          </div>

          <div class="sum-line">
            <span>合计贡献</span>
            <b>{{ fmtScore(contributionSum) }} / 100</b>
          </div>
          <div v-if="detail?.mainFactor" class="main-note">
            主要影响因素：<b>{{ detail.mainFactor }}</b>
            （贡献 {{ fmtScore(mainContribution) }} 分），说明该维度对本次可信度结果影响最大。
          </div>
        </div>
      </div>

      <!-- 右栏：输入快照 / 可改进项 / 人工修正 -->
      <div>
        <div class="card">
          <h3 class="card-title">
            <span>输入快照</span>
            <span class="tag t-gray">计算时冻结</span>
          </h3>
          <pre class="mono snapshot">{{ prettyInput }}</pre>
        </div>

        <div class="card">
          <h3 class="card-title">未满足条件 / 可改进项</h3>
          <ul class="improve-list">
            <li v-for="(t, i) in improvements" :key="i">{{ t }}</li>
          </ul>
        </div>

        <div class="card">
          <h3 class="card-title">
            <span>人工修正</span>
            <span v-if="detail?.manualFlag" class="tag t-amber">已修正</span>
          </h3>
          <div v-if="detail?.manualFlag" class="manual-old">
            原修正原因：{{ detail?.manualReason || '—' }}
          </div>
          <el-input
            v-model="manual.manualReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写不少于 5 字的修正原因，原始计算结果会保留"
          />
          <div style="display:flex;gap:8px;margin-top:10px;align-items:center">
            <el-select v-model="manual.grade" placeholder="修正等级" style="width:200px">
              <el-option v-for="g in GRADES" :key="g" :label="g" :value="g" />
            </el-select>
            <el-button :loading="manualLoading" @click="submitManual">提交修正</el-button>
          </div>
          <div class="manual-tip">
            人工修正只改结论等级，不改五因子原始得分；修正后 manualFlag=1，便于统计人工干预率。
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const GRADES = ['高可信', '中可信', '较低可信', '低可信']
const GRADE_STYLE = {
  高可信: { tag: 't-green', color: '#2B7955' },
  中可信: { tag: 't-blue', color: '#3D7A66' },
  较低可信: { tag: 't-amber', color: '#C2703A' },
  低可信: { tag: 't-red', color: '#B02A37' }
}

const loading = ref(false)
const recalcLoading = ref(false)
const manualLoading = ref(false)
const detail = ref(null)
const currentId = ref(null)
const manual = reactive({ manualReason: '', grade: '' })

const gradeTag = computed(() => GRADE_STYLE[detail.value?.grade]?.tag || 't-gray')
const gradeColor = computed(() => GRADE_STYLE[detail.value?.grade]?.color || 'var(--primary)')
const scoreText = computed(() => fmtScore(detail.value?.totalScore))

// 五因子：优先用后端 factors（已按贡献降序），缺失时用 s* 字段兜底
const factors = computed(() => {
  const d = detail.value
  if (!d) return []
  if (Array.isArray(d.factors) && d.factors.length) return d.factors
  const w = d.weights || {}
  const rows = [
    { name: '评分偏离度', score: d.sDeviation, weight: w.deviation },
    { name: '评语有效长度', score: d.sComment, weight: w.comment },
    { name: '互评完成率', score: d.sFinishRate, weight: w.finishRate },
    { name: '提交相似度', score: d.sSimilarity, weight: w.similarity },
    { name: '评分一致性', score: d.sConsistency, weight: w.consistency }
  ]
  return rows.map((r) => ({
    ...r,
    contribution: (Number(r.score || 0) * Number(r.weight || 0)) / 100
  }))
})

const contributionSum = computed(() =>
  factors.value.reduce((s, f) => s + Number(f.contribution || 0), 0)
)
const mainContribution = computed(() => {
  const m = factors.value.find((f) => f.name === detail.value?.mainFactor)
  return m ? m.contribution : 0
})

// 输入快照格式化：解析失败时原样展示，保证不抛错
const inputObj = computed(() => {
  const raw = detail.value?.inputJson
  if (!raw) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    return null
  }
})
const prettyInput = computed(() => {
  const raw = detail.value?.inputJson
  if (!raw) return '（本条记录未保存输入快照）'
  if (inputObj.value) return JSON.stringify(inputObj.value, null, 2)
  return String(raw)
})

// 未满足条件：结合快照原始输入与因子得分推导，口径与规则说明一致
const improvements = computed(() => {
  const d = detail.value
  if (!d) return []
  const out = []
  const raw = inputObj.value || {}
  const alertLine = Number(d.similarityAlert ?? 70)

  if (raw.comment_len != null && Number(raw.comment_len) < 20) {
    out.push(`评语有效字数仅 ${raw.comment_len} 字，低于 20 字低质量线，评语有效长度项被压低。`)
  }
  if (raw.finish_rate != null && Number(raw.finish_rate) < 0.8) {
    out.push(`近 3 次互评完成率 ${(Number(raw.finish_rate) * 100).toFixed(0)}%，低于 80% 基准。`)
  }
  if (raw.max_similarity != null && Number(raw.max_similarity) > alertLine) {
    out.push(`提交最高代码相似度 ${Number(raw.max_similarity).toFixed(2)}%，超过告警线 ${alertLine}%。`)
  }
  if (raw.peer_count != null && Number(raw.peer_count) < 2) {
    out.push(`同一提交仅 ${raw.peer_count} 份互评，评分一致性参考性不足。`)
  }
  factors.value.forEach((f) => {
    if (Number(f.score) < 50) {
      out.push(`${f.name}得分 ${fmtScore(f.score)}，低于 50 分基准，是本次主要失分项。`)
    }
  })
  const high = Number(d.thresholds?.high ?? 85)
  if (Number(d.totalScore) < high) {
    out.push(`可信度总分 ${fmtScore(d.totalScore)} 未达到高可信线 ${high} 分。`)
  }
  if (d.lowQuality === 1) {
    out.push('该条互评被标记为低质量评语（评语过短或判定为套话）。')
  }
  if (d.manualFlag) {
    out.push('该记录已被人工修正，原始计算结果保留，仅结论等级发生变化。')
  }
  return out.length ? out.slice(0, 6) : ['五项因子均达到基准，本次未发现明显短板。']
})

function fmtScore(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(1) : '—'
}

function clamp(v) {
  const n = Number(v)
  return Math.min(100, Math.max(0, Number.isFinite(n) ? n : 0))
}

function barClass(v) {
  const n = Number(v)
  if (n >= 80) return ''
  if (n >= 60) return 'mid'
  return 'low'
}

/** 无 id 时自动取列表第一条，保证从菜单直接进入也有内容 */
async function load() {
  loading.value = true
  try {
    let id = route.query.id
    if (!id) {
      const page = await api.credibilityPage({ page: 1, size: 1 })
      id = page?.records?.[0]?.id
    }
    if (!id) {
      detail.value = null
      return
    }
    currentId.value = id
    const d = await api.credibilityDetail(id)
    detail.value = d || null
    manual.grade = d?.grade || ''
    manual.manualReason = ''
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

function back() {
  router.push('/credibility')
}

/** 重算：按快照所属作业重算，重新读取当前记录 */
async function recalc() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `将按现行规则重算快照 ${detail.value.snapshotNo} 所在作业的可信度，人工修正结论会保留。是否继续？`,
      '重算确认',
      { type: 'warning', confirmButtonText: '确认重算', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  recalcLoading.value = true
  try {
    await api.recalc({ assignmentId: detail.value.assignmentId })
    ElMessage.success('重算完成，已刷新留痕')
    await load()
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    recalcLoading.value = false
  }
}

/** 人工修正：原因 ≥5 字，等级必须为四档之一 */
async function submitManual() {
  if (!detail.value) return
  const reason = String(manual.manualReason || '').trim()
  if (reason.length < 5) {
    ElMessage.warning('人工修正原因不能少于 5 个字')
    return
  }
  if (!GRADES.includes(manual.grade)) {
    ElMessage.warning('请选择修正后的可信度等级')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将快照 ${detail.value.snapshotNo} 的等级修正为「${manual.grade}」？原始计算得分与修正原因都会保留。`,
      '人工修正',
      { type: 'warning', confirmButtonText: '确认修正', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  manualLoading.value = true
  try {
    await api.manualAdjust(detail.value.id || currentId.value, {
      manualReason: reason,
      grade: manual.grade
    })
    ElMessage.success('已记录人工修正')
    await load()
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    manualLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.level-guide {
  padding: 12px 16px;
  background: #f7faf8;
  border-left: 3px solid var(--primary);
}
.level-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.level-item p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.6;
}
.trace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 14px;
  align-items: start;
}
.score-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.score-num {
  font-size: 38px;
  font-weight: 600;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.conclusion {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f4f8f6;
  border-left: 3px solid var(--success);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.8;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 18px;
  margin-top: 14px;
}
.meta-grid > div {
  display: flex;
  gap: 8px;
  font-size: 12.5px;
  min-width: 0;
}
.meta-grid span {
  width: 68px;
  flex: none;
  color: var(--text-2);
}
.meta-grid b {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* 名称列加宽以容纳「主要影响因素」标记 */
.trace-row {
  grid-template-columns: 150px 1fr 88px;
  margin-bottom: 12px;
}
.trace-row .fname {
  line-height: 1.5;
}
.trace-row .fname small {
  color: var(--text-2);
  font-size: 11px;
  margin-left: 3px;
}
.trace-row .fval small {
  display: block;
  font-size: 10.5px;
  color: var(--text-2);
  font-weight: 400;
}
.main-flag {
  display: inline-block;
  margin-left: 4px;
  padding: 0 5px;
  font-size: 10px;
  line-height: 16px;
  color: #9c5c12;
  background: #fbedE1;
  border-radius: 3px;
  white-space: nowrap;
}
.sum-line {
  display: flex;
  justify-content: space-between;
  padding-top: 10px;
  margin-top: 6px;
  border-top: 1px solid var(--line);
  font-size: 13px;
  color: var(--text-2);
}
.sum-line b {
  color: var(--text);
  font-variant-numeric: tabular-nums;
}
.main-note {
  margin-top: 10px;
  padding: 8px 10px;
  background: var(--bg);
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.8;
}
.snapshot {
  margin: 0;
  padding: 10px 12px;
  background: #f7faf8;
  border: 1px solid var(--line);
  border-radius: 6px;
  font-size: 11.5px;
  line-height: 1.75;
  max-height: 240px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.improve-list {
  margin: 0;
  padding-left: 16px;
  font-size: 12.5px;
  color: var(--text-2);
  line-height: 1.9;
}
.improve-list li + li {
  margin-top: 4px;
}
.manual-old {
  font-size: 12px;
  color: #9c5c12;
  background: #fbedE1;
  border-radius: 4px;
  padding: 7px 10px;
  margin-bottom: 10px;
  line-height: 1.7;
}
.manual-tip {
  margin-top: 10px;
  font-size: 11.5px;
  color: var(--text-2);
  line-height: 1.7;
}
@media (max-width: 1100px) {
  .trace-grid {
    grid-template-columns: 1fr;
  }
  .level-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
