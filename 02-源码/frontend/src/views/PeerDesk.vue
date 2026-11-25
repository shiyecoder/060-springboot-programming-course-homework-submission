<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>互评工作台</h1>
        <p class="sub">匿名评阅 · 三个维度合计 100 分 · 评语不少于 20 字并指出具体代码位置</p>
      </div>
      <div class="head-actions">
        <span class="tag t-blue">匿名展示 · 不可查看作者</span>
      </div>
    </div>

    <div class="split">
      <!-- 左侧任务队列 -->
      <div class="card queue-card">
        <div class="card-title" style="margin-bottom:10px">
          <span>待评任务</span>
          <span style="font-size:12px;font-weight:400;color:var(--text-2)">{{ tasks.length }} 份</span>
        </div>

        <div v-loading="loading" class="queue">
          <div
            v-for="t in tasks"
            :key="t.id"
            class="q-item"
            :class="{ on: current && current.id === t.id }"
            @click="pick(t)"
          >
            <div class="q-top">
              <span class="q-title">{{ t.assignmentTitle }}</span>
              <span class="tag" :class="t.status === '待评' ? 't-amber' : 't-gray'">{{ t.status }}</span>
            </div>
            <div class="q-meta">
              <span>{{ t.languageType || '—' }}</span>
              <span>·</span>
              <span>{{ t.codeLines ?? 0 }} 行</span>
              <span>·</span>
              <span class="mono">{{ t.taskNo }}</span>
            </div>
            <div class="q-meta">
              <span>截止 {{ fmtTime(t.deadline) }}</span>
            </div>
          </div>

          <el-empty v-if="!loading && !tasks.length" description="暂无待评任务，全部已完成" :image-size="80" />
        </div>
      </div>

      <!-- 右侧评分区 -->
      <div>
        <template v-if="current">
          <div class="card">
            <div class="card-title" style="margin-bottom:10px">
              <span>被评提交 · {{ current.taskNo }}</span>
              <span class="tag t-gray">匿名 {{ current.authorName || '匿名同学' }}</span>
            </div>
            <div class="meta-grid">
              <div><span>作业</span><b>{{ current.assignmentTitle }}</b></div>
              <div><span>语言</span><b>{{ current.languageType || '—' }}</b></div>
              <div><span>提交时间</span><b>{{ fmtTime(current.submittedAt) }}</b></div>
              <div>
                <span>编译结果</span>
                <b>
                  <span class="tag" :class="compileTag(current.compileResult)">{{ current.compileResult || '未运行' }}</span>
                </b>
              </div>
              <div><span>用例通过</span><b class="mono">{{ current.passCount ?? 0 }}/{{ current.totalCount ?? 0 }}</b></div>
              <div><span>截止时间</span><b>{{ fmtTime(current.deadline) }}</b></div>
            </div>

            <div class="code-head">
              <span class="tag t-gray">示例代码</span>
              <span style="font-size:12px;color:var(--text-2)">
                演示用片段（{{ current.languageType || '通用' }}）· 实际评阅请以提交文件为准
              </span>
            </div>
            <!-- 带行号代码块：.code-box 由全局样式提供 -->
            <div class="code-box">
              <div v-for="(line, i) in codeLines" :key="i" class="ln">
                <i>{{ i + 1 }}</i>
                <span>{{ line }}</span>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card-title" style="margin-bottom:14px">三维度打分</div>

            <!-- 正确性 0-60 / 可读性 0-25 / 规范 0-15，三项之和即互评总分 -->
            <div class="slider-row">
              <span class="sname">正确性<small>功能是否与题目要求一致</small></span>
              <el-slider v-model="score.correctnessScore" :min="0" :max="60" :step="1" />
              <span class="sval">{{ score.correctnessScore }}<em>/60</em></span>
            </div>
            <div class="slider-row">
              <span class="sname">可读性<small>命名、层次、注释是否清晰</small></span>
              <el-slider v-model="score.readabilityScore" :min="0" :max="25" :step="1" />
              <span class="sval">{{ score.readabilityScore }}<em>/25</em></span>
            </div>
            <div class="slider-row">
              <span class="sname">代码规范<small>缩进、空格、异常处理</small></span>
              <el-slider v-model="score.styleScore" :min="0" :max="15" :step="1" />
              <span class="sval">{{ score.styleScore }}<em>/15</em></span>
            </div>

            <div class="total-row">
              <span>互评总分（三项之和）</span>
              <b>{{ totalScore.toFixed(1) }}</b>
            </div>

            <div class="card-title" style="margin:16px 0 8px">
              <span>评语</span>
              <span style="font-size:12px;font-weight:400" :style="{ color: commentLen >= 20 ? 'var(--success)' : 'var(--danger)' }">
                {{ commentLen }} / 最少 20 字
              </span>
            </div>
            <el-input
              v-model="score.comment"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="请指出具体代码位置与改进方式，例如「第 7 行的自减未加锁，可能出现负数」"
            />
            <div class="note" style="margin-top:10px">
              评语少于 20 字或只有「写得不错」这类套话会被标记为低质量，评语有效长度因子计 0 分并拉低可信度。
            </div>

            <div class="submit-row">
              <el-button type="primary" :loading="submitting" @click="submit">提交互评</el-button>
              <span style="font-size:12px;color:var(--text-2)">提交后不可修改，如需重评只能由教师追加补评任务</span>
            </div>
          </div>

          <!-- 提交成功后的可信度结果（创新点：五因子加权） -->
          <div v-if="result" class="card">
            <div class="card-title" style="margin-bottom:10px">
              <span>本次互评可信度</span>
              <span class="tag" :class="gradeTag(result.grade)">{{ result.grade }}</span>
            </div>
            <div class="cr-head">
              <b class="mono">{{ result.totalScore }}</b>
              <span style="font-size:12px;color:var(--text-2)">/ 100 · {{ result.ruleVersion || 'v2.1' }} 规则</span>
            </div>

            <div class="factor-row">
              <span class="fname">评分偏离度 ×25%</span>
              <span class="ftrack"><i class="fbar" :class="barClass(result.sDeviation)" :style="{ width: pct(result.sDeviation) }"></i></span>
              <span class="fval">{{ show(result.sDeviation) }}</span>
            </div>
            <div class="factor-row">
              <span class="fname">评语有效长度 ×20%</span>
              <span class="ftrack"><i class="fbar" :class="barClass(result.sComment)" :style="{ width: pct(result.sComment) }"></i></span>
              <span class="fval">{{ show(result.sComment) }}</span>
            </div>
            <div class="factor-row">
              <span class="fname">互评完成率 ×20%</span>
              <span class="ftrack"><i class="fbar" :class="barClass(result.sFinishRate)" :style="{ width: pct(result.sFinishRate) }"></i></span>
              <span class="fval">{{ show(result.sFinishRate) }}</span>
            </div>
            <div class="factor-row">
              <span class="fname">提交相似度 ×20%</span>
              <span class="ftrack"><i class="fbar" :class="barClass(result.sSimilarity)" :style="{ width: pct(result.sSimilarity) }"></i></span>
              <span class="fval">{{ show(result.sSimilarity) }}</span>
            </div>
            <div class="factor-row">
              <span class="fname">评分一致性 ×15%</span>
              <span class="ftrack"><i class="fbar" :class="barClass(result.sConsistency)" :style="{ width: pct(result.sConsistency) }"></i></span>
              <span class="fval">{{ result.sConsistency === null || result.sConsistency === undefined ? '仅 1 份' : show(result.sConsistency) }}</span>
            </div>

            <div class="note" style="margin-top:8px">
              <b>{{ result.grade }}</b>：{{ result.conclusion }}<br />
              {{ result.weightNote }}
            </div>

            <ul v-if="result.unmet && result.unmet.length" class="unmet">
              <li v-for="(u, i) in result.unmet" :key="i">{{ u }}</li>
            </ul>
          </div>
        </template>

        <div v-else class="card">
          <el-empty description="左侧暂无可评任务，完成全部互评后请查看互评进度" />
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const tasks = ref([])
const current = ref(null)
const result = ref(null)

// 三维度评分：正确性 0-60、可读性 0-25、规范 0-15
const score = reactive({
  correctnessScore: 0,
  readabilityScore: 0,
  styleScore: 0,
  comment: ''
})

/** 内置示例代码片段：用于演示带行号的评阅界面 */
const codeLines = [
  '// 统计单词出现次数，输出前 3 名',
  'function topWords(text) {',
  '  const map = new Map()',
  "  const words = text.toLowerCase().match(/[a-z']+/g) || []",
  '  for (const w of words) {',
  '    map.set(w, (map.get(w) || 0) + 1)',
  '  }',
  '  const list = [...map.entries()]',
  '  list.sort((a, b) => b[1] - a[1])',
  '  return list.slice(0, 3)',
  '}',
  '',
  "const text = 'the quick brown fox jumps over the lazy dog the fox'",
  'console.log(topWords(text))'
]

const totalScore = computed(
  () => Number(score.correctnessScore) + Number(score.readabilityScore) + Number(score.styleScore)
)
const commentLen = computed(() => (score.comment || '').trim().length)

function compileTag(v) {
  if (v === '通过') return 't-green'
  if (v === '编译失败') return 't-red'
  return 't-gray'
}

function gradeTag(g) {
  const map = { 高可信: 't-green', 中可信: 't-blue', 较低可信: 't-amber', 低可信: 't-red' }
  return map[g] || 't-gray'
}

/** 因子为 null 时（仅 1 份互评，一致性退出）按 0 展示，不画满条 */
function pct(v) {
  const n = Number(v)
  if (isNaN(n)) return '0%'
  return `${Math.min(100, Math.max(0, n))}%`
}

function show(v) {
  const n = Number(v)
  return isNaN(n) ? '—' : n.toFixed(1)
}

function barClass(v) {
  const n = Number(v)
  if (isNaN(n)) return 'low'
  if (n >= 85) return ''
  if (n >= 65) return 'mid'
  return 'low'
}

async function loadTasks() {
  loading.value = true
  try {
    const d = await api.peerTaskPage({ page: 1, size: 100, status: '待评' })
    tasks.value = d?.records || []
    if (current.value) {
      const still = tasks.value.find((t) => t.id === current.value.id)
      current.value = still || tasks.value[0] || null
    } else {
      current.value = tasks.value[0] || null
    }
  } catch (e) {
    tasks.value = []
    current.value = null
  } finally {
    loading.value = false
  }
}

/** 清空三维度与评语（切换任务、提交成功后退回初始态） */
function resetForm() {
  score.correctnessScore = 0
  score.readabilityScore = 0
  score.styleScore = 0
  score.comment = ''
}

/** 切换任务时清空评分，避免把上一份的分数带到下一份 */
function pick(t) {
  current.value = t
  resetForm()
  result.value = null
}

async function submit() {
  if (!current.value) {
    ElMessage.warning('请先在左侧选择待评任务')
    return
  }
  if (commentLen.value < 20) {
    ElMessage.warning('评语不少于 20 字，并指出具体代码位置')
    return
  }
  if (totalScore.value <= 0) {
    ElMessage.warning('三个维度至少需要给出 1 分')
    return
  }
  submitting.value = true
  try {
    const d = await api.peerScore({
      taskId: current.value.id,
      correctnessScore: Number(score.correctnessScore),
      readabilityScore: Number(score.readabilityScore),
      styleScore: Number(score.styleScore),
      comment: score.comment.trim()
    })
    result.value = d?.credibility || null
    ElMessage.success(`互评已提交，总分 ${d?.totalScore ?? totalScore.value}，可信度 ${d?.credibility?.grade || '—'}`)
    // 队列刷新：该任务转为已评并从待评列表移除；表单复位但保留可信度结果
    await loadTasks()
    resetForm()
  } catch (e) {
    // 具体校验信息由拦截器提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadTasks)
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
.queue-card {
  padding-bottom: 8px;
}
.queue {
  max-height: 620px;
  overflow-y: auto;
}
.q-item {
  border: 1px solid var(--line);
  border-radius: var(--radius-s);
  padding: 10px 12px;
  margin-bottom: 8px;
  cursor: pointer;
  background: #fff;
  transition: border-color 0.15s, background 0.15s;
}
.q-item:hover {
  border-color: var(--secondary);
}
.q-item.on {
  border-color: var(--primary);
  background: #f3f7f5;
  box-shadow: inset 3px 0 0 var(--primary);
}
.q-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}
.q-title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}
.q-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-2);
  margin-top: 5px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px 18px;
  margin-bottom: 12px;
}
.meta-grid > div {
  display: flex;
  gap: 8px;
  font-size: 12.5px;
}
.meta-grid span {
  color: var(--text-2);
  flex: none;
}
.meta-grid b {
  font-weight: 500;
}
.code-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 4px 0 8px;
}
.total-row {
  border-top: 1px solid var(--line);
  margin-top: 4px;
  padding-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 13px;
  color: var(--text-2);
}
.total-row b {
  font-size: 26px;
  font-weight: 600;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}
.submit-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}
.note {
  background: #f2f5f4;
  border: 1px solid var(--line);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  padding: 9px 12px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.8;
}
.note b {
  color: var(--text);
}
.cr-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
}
/* 因子条内 i 为 inline 元素，这里补 block 才能撑满轨道高度（不改全局样式） */
.factor-row .ftrack {
  display: block;
}
.factor-row .fbar {
  display: block;
}
.cr-head b {
  font-size: 28px;
  font-weight: 600;
}
.unmet {
  margin: 10px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--warn);
  line-height: 1.8;
}
</style>
