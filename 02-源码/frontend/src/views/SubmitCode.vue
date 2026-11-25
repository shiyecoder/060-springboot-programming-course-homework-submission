<template>
  <PortalLayout>
    <div class="submit-view">
      <div class="page-head">
        <div class="head-main">
          <h1>{{ title }}</h1>
          <div class="sub">{{ headSub }}</div>
        </div>
        <span class="tag" :class="submitTag.cls">{{ submitTag.text }}</span>
      </div>

      <el-alert
        v-if="lateExpired"
        class="late-alert"
        type="error"
        :closable="false"
        show-icon
        title="已超过迟交截止时间，无法提交"
        :description="lateDesc"
      />

      <div v-loading="loading" class="two-col">
        <!-- 左栏：题目描述 -->
        <div class="col-left">
          <div class="card desc-card">
            <h3 class="card-title">题目描述</h3>
            <div class="desc">{{ ctx?.description || '暂无题目描述' }}</div>

            <div class="meta-grid">
              <div class="meta">
                <div class="k">用例数量</div>
                <div class="v mono">{{ ctx?.testCaseCount ?? 0 }} <small>条</small></div>
              </div>
              <div class="meta">
                <div class="k">互评份数</div>
                <div class="v mono">{{ ctx?.peerReviewCount ?? 0 }} <small>份</small></div>
              </div>
              <div class="meta">
                <div class="k">成绩权重（运行 : 互评 : 教师）</div>
                <div class="v mono">
                  {{ ctx?.runWeight ?? 0 }} : {{ ctx?.peerWeight ?? 0 }} : {{ ctx?.teacherWeight ?? 0 }}
                </div>
                <div class="k2">合计 100%</div>
              </div>
              <div class="meta">
                <div class="k">编程语言</div>
                <div class="v">{{ ctx?.languageType || '—' }}</div>
              </div>
            </div>

            <div class="hint">
              提交前本地校验：扩展名 {{ extNow }} 与所选语言一致 · 单文件 ≤ {{ maxKB }}KB · 代码 ≤ {{ maxLines }} 行
            </div>
          </div>
        </div>

        <!-- 右栏：编辑器 + 提交记录 -->
        <div class="col-right">
          <div class="card editor-card">
            <div class="code-head">
              <svg class="ic" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
                <path d="M8.5 8.5 4 12l4.5 3.5M15.5 8.5 20 12l-4.5 3.5" />
                <path d="M13.5 5.5l-3 13" />
              </svg>
              <input v-model="fileName" class="fn-input mono" spellcheck="false" aria-label="源文件名" />
              <el-select v-model="codeLanguage" size="small" class="lang-select" @change="onLangChange">
                <el-option v-for="l in LANG_OPTIONS" :key="l" :label="l" :value="l" />
              </el-select>
              <span class="tag code-tag" :class="overLines ? 't-red' : 't-green'">行数 {{ lineCount }} / {{ maxLines }}</span>
              <span class="tag code-tag" :class="overBytes ? 't-red' : 't-green'">{{ sizeKB }} KB / {{ maxSizeText }} KB</span>
              <span class="spacer"></span>
              <el-button size="small" @click="fileRef.click()">上传文件</el-button>
              <input
                ref="fileRef"
                type="file"
                accept=".java,.py,.c,.cpp,.cc,.h,.js,.go"
                hidden
                @change="onFile"
              />
            </div>

            <div class="code-editor">
              <div ref="gutterRef" class="gutter">
                <div v-for="n in lineNos" :key="n" class="ln-no">{{ n }}</div>
              </div>
              <textarea
                ref="taRef"
                v-model="codeText"
                class="code-area"
                spellcheck="false"
                wrap="off"
                placeholder="在此粘贴代码，或点击右上角「上传文件」选择单个源文件"
                @scroll="onScroll"
              ></textarea>
            </div>

            <div class="editor-foot">
              <span class="foot-hint">{{ footHint }}</span>
              <span class="spacer"></span>
              <el-button size="small" :disabled="submitting" @click="onReset">重置</el-button>
              <el-button size="small" type="primary" :loading="submitting" :disabled="lateExpired" @click="onSubmit">
                提交代码
              </el-button>
            </div>
          </div>

          <div class="card records-card">
            <h3 class="card-title">
              <span>提交记录</span>
              <span class="rec-count">{{ versionsList.length }} 个版本 · 以最后一次为准</span>
            </h3>

            <el-table v-if="versionsList.length" :data="versionsList" size="small" max-height="240" style="width:100%">
              <el-table-column label="版本" width="70">
                <template #default="{ row }">
                  <span class="mono">v{{ row.seqNo }}</span>
                </template>
              </el-table-column>
              <el-table-column label="提交时间" width="150">
                <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
              </el-table-column>
              <el-table-column label="行数" width="70" align="right">
                <template #default="{ row }">
                  <span class="mono">{{ row.codeLines ?? '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="大小（KB）" width="90" align="right">
                <template #default="{ row }">
                  <span class="mono">{{ kbText(row.fileSize) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="迟交" width="80">
                <template #default="{ row }">
                  <span class="tag" :class="isLateOf(row.isLate) ? 't-amber' : 't-green'">
                    {{ isLateOf(row.isLate) ? '迟交' : '按时' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="120">
                <template #default="{ row }">
                  <div class="row-acts">
                    <el-button link type="primary" @click="openView(row)">查看</el-button>
                    <el-button link type="primary" :disabled="isEarliest(row)" @click="openCompare(row)">对比</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="还没有提交过，粘贴代码后点提交" :image-size="60" />
          </div>
        </div>
      </div>

      <!-- 查看某个版本 -->
      <el-dialog v-model="viewVisible" title="查看提交代码" width="900px" :close-on-click-modal="false">
        <div v-loading="viewLoading" class="dlg-body">
          <template v-if="viewData">
            <div class="dlg-meta">
              <span class="tag t-blue">v{{ viewData.seqNo }}</span>
              <span>{{ viewData.codeLanguage || '—' }}</span>
              <span class="mono">{{ viewData.codeLines ?? 0 }} 行</span>
              <span class="mono">{{ kbText(viewData.fileSize) }} KB</span>
              <span>{{ fmtTime(viewData.submitTime) }}</span>
              <span class="tag" :class="isLateOf(viewData.isLate) ? 't-amber' : 't-green'">
                {{ isLateOf(viewData.isLate) ? '迟交' : '按时' }}
              </span>
            </div>
            <div class="diff-wrap">
              <table class="diff-table one">
                <tbody>
                  <tr v-for="(l, i) in viewLines" :key="i">
                    <td class="ln">{{ i + 1 }}</td>
                    <td class="cl">{{ l || ' ' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>
        <template #footer>
          <el-button @click="viewVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 版本对比 -->
      <el-dialog v-model="compareVisible" title="版本对比" width="1080px" :close-on-click-modal="false">
        <div v-loading="compareLoading" class="dlg-body">
          <template v-if="compareData">
            <div class="cmp-summary">
              <span class="cmp-lines">{{ cmpSummary }}</span>
              <span>差异 {{ diffCount }} 行</span>
              <span class="tag t-amber">左侧标黄为差异行</span>
            </div>
            <div class="cmp-heads">
              <div>
                <b>上一版 v{{ compareData.prev?.seqNo }}</b>
                <span>{{ fmtTime(compareData.prev?.submitTime) }}</span>
                <span class="mono">{{ compareData.prev?.codeLines ?? 0 }} 行</span>
              </div>
              <div>
                <b>当前版 v{{ compareData.curr?.seqNo }}</b>
                <span>{{ fmtTime(compareData.curr?.submitTime) }}</span>
                <span class="mono">{{ compareData.curr?.codeLines ?? 0 }} 行</span>
              </div>
            </div>
            <div class="diff-wrap">
              <table class="diff-table two">
                <colgroup>
                  <col style="width: 38px" />
                  <col />
                  <col style="width: 38px" />
                  <col />
                </colgroup>
                <tbody>
                  <tr v-for="r in compareRows" :key="r.no">
                    <td class="ln">{{ r.leftBlank ? '' : r.no }}</td>
                    <td class="cl left" :class="{ hit: r.leftDiff && !r.leftBlank }">{{ r.leftBlank ? '' : (r.left || ' ') }}</td>
                    <td class="ln">{{ r.rightBlank ? '' : r.no }}</td>
                    <td class="cl">{{ r.rightBlank ? '' : (r.right || ' ') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </div>
        <template #footer>
          <el-button @click="compareVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </PortalLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalLayout from '@/layouts/PortalLayout.vue'
import { api } from '@/api'

const LANG_OPTIONS = ['Java', 'Python', 'C', 'C++', 'JavaScript', 'Go']

/** 各语言允许的源文件扩展名（与后端校验一致） */
const ALLOWED_EXT = {
  Java: ['.java'],
  Python: ['.py'],
  C: ['.c', '.h'],
  'C++': ['.cpp', '.cc', '.hpp'],
  JavaScript: ['.js'],
  Go: ['.go']
}

/** 各语言默认文件扩展名 */
const DEFAULT_EXT = {
  Java: '.java',
  Python: '.py',
  C: '.c',
  'C++': '.cpp',
  JavaScript: '.js',
  Go: '.go'
}

/** 扩展名反查语言（上传文件时猜测） */
const EXT_LANG = {
  '.java': 'Java',
  '.py': 'Python',
  '.c': 'C',
  '.h': 'C',
  '.cpp': 'C++',
  '.cc': 'C++',
  '.hpp': 'C++',
  '.js': 'JavaScript',
  '.go': 'Go'
}

const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const ctx = ref(null)

const codeText = ref('')
const fileName = ref('Solution.java')
const codeLanguage = ref('Java')

const now = ref(Date.now())
const taRef = ref(null)
const gutterRef = ref(null)
const fileRef = ref(null)

let clock = null

/* ---------------- 时间处理 ---------------- */

function parseTime(str) {
  if (!str) return null
  const d = new Date(String(str).replace(/-/g, '/'))
  return isNaN(d.getTime()) ? null : d
}

function fmtTime(str) {
  const d = parseTime(str)
  if (!d) return '—'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function remainText(deadline) {
  const t = parseTime(deadline)
  if (!t) return '截止时间待定'
  const diff = t.getTime() - now.value
  if (diff <= 0) return '已截止'
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  if (days > 0) return `剩余 ${days} 天 ${hours} 小时`
  const mins = Math.floor((diff % 3600000) / 60000)
  if (hours > 0) return `剩余 ${hours} 小时 ${mins} 分`
  return `剩余 ${mins} 分`
}

/* ---------------- 计算属性 ---------------- */

const title = computed(() => ctx.value?.title || '提交代码')
const maxBytes = computed(() => Number(ctx.value?.rules?.maxBytes) || 204800)
const maxLines = computed(() => Number(ctx.value?.rules?.maxLines) || 2000)
const maxKB = computed(() => Math.round(maxBytes.value / 1024))
const maxSizeText = computed(() => (maxBytes.value / 1024).toFixed(1))

const byteSize = computed(() => new TextEncoder().encode(codeText.value).length)
const sizeKB = computed(() => (byteSize.value / 1024).toFixed(1))
const lineCount = computed(() => (codeText.value ? codeText.value.split('\n').length : 0))
const lineNos = computed(() => {
  const n = Math.max(lineCount.value, 1)
  return Array.from({ length: n }, (_, i) => i + 1)
})

const overBytes = computed(() => byteSize.value > maxBytes.value)
const overLines = computed(() => lineCount.value > maxLines.value)

const lateExpired = computed(() => {
  const t = parseTime(ctx.value?.lateDeadline)
  return !!t && now.value > t.getTime()
})

const lateDesc = computed(() =>
  ctx.value?.lateDeadline
    ? `迟交截止时间 ${fmtTime(ctx.value.lateDeadline)} 已过，本次作业不再接收提交，请联系任课教师。`
    : '本次作业已关闭提交通道，请联系任课教师。'
)

const extNow = computed(() => DEFAULT_EXT[codeLanguage.value] || '.java')
const extListText = computed(() => (ALLOWED_EXT[codeLanguage.value] || []).join(' / '))

const headSub = computed(() => {
  const d = ctx.value
  if (!d) return '正在加载作业信息…'
  const parts = []
  if (d.courseName) parts.push(d.courseName)
  if (d.className) parts.push(d.className)
  parts.push(remainText(d.deadline))
  if (d.lateDeadline) parts.push(`迟交窗口至 ${fmtTime(d.lateDeadline)}`)
  return parts.join(' · ')
})

const versionsList = computed(() => {
  const list = ctx.value?.versions || []
  return [...list].sort((a, b) => Number(b.seqNo || 0) - Number(a.seqNo || 0))
})

const submitTag = computed(() => {
  const s = ctx.value?.mySubmission
  if (!s) return { text: '尚未提交 · 提交后以最后一次为准', cls: 't-gray' }
  const times = Number(s.submitCount || versionsList.value.length || 1)
  return { text: `已提交 ${times} 次 · 以最后一次为准`, cls: 't-amber' }
})

const footHint = computed(() => {
  const s = ctx.value?.mySubmission
  if (!s) return `首次提交 · 提交后自动运行 ${ctx.value?.testCaseCount ?? 0} 条用例`
  const times = Number(s.submitCount || 1) + 1
  const pass = Number(s.passCount || 0)
  const total = Number(s.totalCount || 0)
  return `第 ${times} 次提交 · 上次 ${fmtTime(s.submitTime)} · 用例 ${pass}/${total}`
})

/* ---------------- 交互 ---------------- */

function extOf(name) {
  const n = String(name || '')
  const i = n.lastIndexOf('.')
  return i < 0 ? '' : n.slice(i).toLowerCase()
}

function extByLang(lang) {
  return DEFAULT_EXT[lang] || '.java'
}

function kbText(bytes) {
  const n = Number(bytes)
  if (!Number.isFinite(n) || n <= 0) return '0.0'
  return (n / 1024).toFixed(1)
}

function isLateOf(v) {
  return v === 1 || v === true || v === '1'
}

function onScroll(e) {
  if (gutterRef.value) gutterRef.value.scrollTop = e.target.scrollTop
}

function onLangChange(val) {
  const allowed = ALLOWED_EXT[val] || []
  if (!allowed.includes(extOf(fileName.value))) {
    fileName.value = `Solution${extByLang(val)}`
  }
}

function onFile(e) {
  const file = e.target.files && e.target.files[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    codeText.value = String(reader.result || '')
    fileName.value = file.name
    const guess = EXT_LANG[extOf(file.name)]
    if (guess) codeLanguage.value = guess
    nextTick(() => {
      if (taRef.value) taRef.value.scrollTop = 0
      if (gutterRef.value) gutterRef.value.scrollTop = 0
    })
  }
  reader.readAsText(file, 'utf-8')
  e.target.value = ''
}

function onReset() {
  const s = ctx.value?.mySubmission
  codeLanguage.value = s?.codeLanguage || ctx.value?.languageType || 'Java'
  fileName.value = `Solution${extByLang(codeLanguage.value)}`
  codeText.value = s?.codeText || ''
  nextTick(() => {
    if (taRef.value) taRef.value.scrollTop = 0
    if (gutterRef.value) gutterRef.value.scrollTop = 0
  })
}

/** 提交前本地校验：与后端同规则，先拦一次减少无效请求 */
function validate() {
  const code = codeText.value
  if (!code.trim()) {
    ElMessage.warning('请粘贴代码或上传单个源文件后再提交')
    return false
  }

  const ext = extOf(fileName.value)
  const allowed = ALLOWED_EXT[codeLanguage.value] || []
  if (!ext || !allowed.includes(ext)) {
    ElMessage.warning(`扩展名与所选语言不一致：${codeLanguage.value} 仅支持 ${extListText.value} 源文件`)
    return false
  }

  if (byteSize.value > maxBytes.value) {
    ElMessage.warning(`单个源文件不得超过 ${maxKB.value}KB（当前 ${sizeKB.value}KB），请拆分文件或联系教师`)
    return false
  }

  if (lineCount.value > maxLines.value) {
    ElMessage.warning(`代码行数不得超过 ${maxLines.value} 行（当前 ${lineCount.value} 行），请拆分文件或联系教师`)
    return false
  }

  if (lateExpired.value) {
    ElMessage.warning('已超过迟交截止时间，无法提交')
    return false
  }

  return true
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const res = await api.submitCode({
      assignmentId: route.params.id,
      codeLanguage: codeLanguage.value,
      fileName: (fileName.value || `Solution${extByLang(codeLanguage.value)}`).trim(),
      codeText: codeText.value
    })
    ElMessage.success(res?.message || '提交成功')
    await load()
  } catch (e) {
    // 校验类错误由请求拦截器统一提示，这里只恢复按钮状态
  } finally {
    submitting.value = false
  }
}

/* ---------------- 版本查看与对比 ---------------- */

const viewVisible = ref(false)
const viewLoading = ref(false)
const viewData = ref(null)

const viewLines = computed(() => String(viewData.value?.codeText ?? '').split('\n'))

const compareVisible = ref(false)
const compareLoading = ref(false)
const compareData = ref(null)

const compareRows = computed(() => {
  if (!compareData.value) return []
  const a = String(compareData.value.prev?.codeText ?? '').split('\n')
  const b = String(compareData.value.curr?.codeText ?? '').split('\n')
  const len = Math.max(a.length, b.length)
  const rows = []
  for (let i = 0; i < len; i += 1) {
    const left = a[i]
    const right = b[i]
    const leftBlank = left === undefined
    const rightBlank = right === undefined
    rows.push({
      no: i + 1,
      left,
      right,
      leftBlank,
      rightBlank,
      leftDiff: left !== right
    })
  }
  return rows
})

const diffCount = computed(() => compareRows.value.filter((r) => r.leftDiff).length)

const cmpSummary = computed(() => {
  const p = compareData.value?.prev
  const c = compareData.value?.curr
  if (!p || !c) return ''
  const a = Number(p.codeLines ?? 0)
  const b = Number(c.codeLines ?? 0)
  const d = b - a
  return `行数 ${a} → ${b}（${d > 0 ? '+' : ''}${d}）`
})

function isEarliest(row) {
  const list = versionsList.value
  return list.length > 0 && list[list.length - 1].id === row.id
}

async function openView(row) {
  viewVisible.value = true
  viewLoading.value = true
  viewData.value = null
  try {
    viewData.value = await api.versionDetail(row.id)
  } catch (e) {
    viewData.value = null
  } finally {
    viewLoading.value = false
  }
}

async function openCompare(row) {
  const list = versionsList.value
  const idx = list.findIndex((v) => v.id === row.id)
  const prev = idx >= 0 ? list[idx + 1] : null
  if (!prev) {
    ElMessage.info('这是最早的一个版本，暂无上一版可对比')
    return
  }
  compareVisible.value = true
  compareLoading.value = true
  compareData.value = null
  try {
    const [a, b] = await Promise.all([api.versionDetail(prev.id), api.versionDetail(row.id)])
    compareData.value = { prev: a, curr: b }
  } catch (e) {
    compareData.value = null
  } finally {
    compareLoading.value = false
  }
}

/* ---------------- 数据加载 ---------------- */

async function load() {
  loading.value = true
  try {
    const d = await api.submitContext(route.params.id)
    ctx.value = d
    const s = d?.mySubmission
    const lang = s?.codeLanguage || d?.languageType || 'Java'
    codeLanguage.value = LANG_OPTIONS.includes(lang) ? lang : 'Java'
    fileName.value = `Solution${extByLang(codeLanguage.value)}`
    codeText.value = s?.codeText || ''
    await nextTick()
    if (taRef.value) taRef.value.scrollTop = 0
    if (gutterRef.value) gutterRef.value.scrollTop = 0
  } catch (e) {
    ctx.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => route.params.id,
  (id) => {
    if (id) load()
  }
)

onMounted(() => {
  load()
  clock = setInterval(() => {
    now.value = Date.now()
  }, 60000)
})

onUnmounted(() => {
  if (clock) clearInterval(clock)
})
</script>

<style scoped>
.page-head h1 {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.head-main {
  min-width: 0;
}
.page-head .sub {
  margin-top: 5px;
}
.page-head > .tag {
  align-self: center;
  flex: none;
}
.late-alert {
  margin-bottom: 12px;
}

.two-col {
  display: flex;
  align-items: stretch;
  gap: 12px;
  min-width: 0;
}
.col-left {
  flex: 0 0 44%;
  width: 44%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.col-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.desc-card {
  flex: 1;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
}
.desc {
  font-size: 13px;
  line-height: 1.9;
  color: #3a4750;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}
.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 14px;
  margin-top: 14px;
  margin-bottom: auto;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}
.meta .k {
  font-size: 11px;
  color: var(--text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta .v {
  margin-top: 3px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}
.meta .v small {
  font-size: 11px;
  font-weight: 400;
  color: var(--text-2);
}
.meta .k2 {
  margin-top: 2px;
  font-size: 11px;
  color: #8b969f;
}
.hint {
  margin-top: 16px;
  background: #f2f5f4;
  border: 1px solid var(--line);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  padding: 9px 12px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--text-2);
}
.editor-card {
  margin-bottom: 0;
  padding: 12px 14px;
}
.code-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 10px;
}
.code-head .ic {
  color: var(--secondary);
  flex: none;
}
.fn-input {
  width: 150px;
  height: 26px;
  padding: 0 7px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: #f2f5f4;
  color: var(--text);
  font-size: 12px;
  outline: none;
}
.fn-input:focus {
  background: #fff;
  border-color: var(--secondary);
}
.lang-select {
  width: 112px;
}
.code-tag {
  font-size: 11.5px;
  padding: 1px 6px;
  font-variant-numeric: tabular-nums;
}
.spacer {
  flex: 1;
}

.code-editor {
  display: flex;
  height: 340px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fbfcfc;
  overflow: hidden;
}
.gutter {
  flex: 0 0 46px;
  padding: 10px 0;
  background: #f2f5f4;
  border-right: 1px solid var(--line);
  overflow: hidden;
  font-family: "SF Mono", Menlo, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 20px;
  color: #a8b4ae;
  text-align: right;
}
.gutter .ln-no {
  height: 20px;
  padding-right: 10px;
  user-select: none;
}
.code-area {
  flex: 1;
  min-width: 0;
  padding: 10px 12px;
  border: 0;
  outline: none;
  resize: none;
  background: transparent;
  color: #1f2933;
  font-family: "SF Mono", Menlo, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 20px;
  white-space: pre;
  overflow: auto;
  tab-size: 4;
}
.code-area::placeholder {
  color: #a8b4ae;
}
.editor-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.foot-hint {
  font-size: 12px;
  color: var(--text-2);
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.records-card {
  margin-bottom: 0;
  padding: 12px 14px 10px;
}
.records-card .card-title {
  margin-bottom: 6px;
}
.rec-count {
  font-size: 11.5px;
  font-weight: 400;
  color: var(--text-2);
}
.records-card :deep(.el-table td.el-table__cell),
.records-card :deep(.el-table th.el-table__cell) {
  padding: 6px 0;
}
.row-acts {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}

.dlg-body {
  min-height: 120px;
}
.dlg-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--text-2);
  flex-wrap: wrap;
}
.diff-wrap {
  max-height: 58vh;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fbfcfc;
}
.diff-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-family: "SF Mono", Menlo, Consolas, "Courier New", monospace;
  font-size: 12px;
  line-height: 20px;
}
.diff-table td {
  padding: 0;
  vertical-align: top;
}
.diff-table .ln {
  width: 38px;
  padding-right: 8px;
  text-align: right;
  color: #a8b4ae;
  background: #f2f5f4;
  user-select: none;
}
.diff-table .cl {
  padding: 0 10px;
  white-space: pre;
  overflow: hidden;
  color: #1f2933;
}
.diff-table .cl.left.hit {
  background: #fbeddf;
}
.cmp-summary {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  font-size: 12px;
  color: var(--text-2);
}
.cmp-lines {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}
.cmp-heads {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-2);
}
.cmp-heads > div {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 46px;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
}
.cmp-heads b {
  color: var(--text);
  font-weight: 600;
}

@media (max-width: 1100px) {
  .two-col {
    flex-direction: column;
  }
  .col-left {
    flex: none;
    width: 100%;
  }
}
</style>
