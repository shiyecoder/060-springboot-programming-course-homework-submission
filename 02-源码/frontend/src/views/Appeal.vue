<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>成绩申诉</h1>
        <p class="sub">{{ headSub }}</p>
      </div>
      <div class="head-actions">
        <el-button v-if="isStu" type="primary" @click="openCreate">发起申诉</el-button>
        <el-button v-else :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="split">
      <!-- 左侧申诉列表 -->
      <div class="card list-card">
        <div class="list-head">
          <el-select v-model="status" placeholder="全部状态" clearable size="small" style="width:150px" @change="search">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
          <span style="font-size:12px;color:var(--text-2)">{{ total }} 条</span>
        </div>

        <div v-loading="loading" class="alist">
          <div
            v-for="a in rows"
            :key="a.id"
            class="a-item"
            :class="{ on: current && current.id === a.id }"
            @click="select(a)"
          >
            <div class="a-top">
              <span class="mono" style="font-size:12px">{{ a.appealNo }}</span>
              <span class="tag" :class="statusTag(a.status)">{{ a.status }}</span>
            </div>
            <div class="a-name">{{ a.studentName }} <span class="mono" style="font-size:11px;color:var(--text-2)">{{ a.studentNo }}</span></div>
            <div class="a-meta">{{ a.assignmentTitle || '—' }}</div>
            <div class="a-meta">
              <span>{{ a.appealType }}</span>
              <span>·</span>
              <span>{{ fmtTime(a.createTime) }}</span>
            </div>
          </div>

          <el-empty v-if="!loading && !rows.length" description="暂无申诉记录" :image-size="70" />
        </div>

        <div class="pager" style="padding-top:8px">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            :pager-count="5"
            small
            layout="prev, pager, next"
            @current-change="load"
          />
        </div>
      </div>

      <!-- 右侧处理面板 -->
      <div>
        <template v-if="current">
          <div class="card">
            <div class="card-title" style="margin-bottom:12px">
              <span>
                {{ current.appealNo }}
                <span style="font-size:12px;font-weight:400;color:var(--text-2)"> · {{ current.appealType }}</span>
              </span>
              <span class="tag" :class="statusTag(current.status)">{{ current.status }}</span>
            </div>

            <div class="meta">
              <div><span>学生</span><b>{{ current.studentName }} · <span class="mono">{{ current.studentNo }}</span></b></div>
              <div><span>班级</span><b>{{ current.className || '—' }}</b></div>
              <div><span>作业</span><b>{{ current.assignmentTitle || '—' }}</b></div>
              <div><span>课程</span><b>{{ current.courseName || '—' }}</b></div>
              <div><span>期望分数</span><b class="mono">{{ current.expectScore ?? '未填写' }}</b></div>
              <div><span>提交时间</span><b>{{ fmtTime(current.createTime) }}</b></div>
            </div>

            <div class="sub-title">申诉理由（{{ (current.reason || '').length }} 字）</div>
            <div class="doc-block">{{ current.reason || '—' }}</div>

            <!-- 当前成绩构成：autoScore 来自成绩明细（申诉列表接口不返回） -->
            <div class="sub-title">当前成绩构成</div>
            <div class="score-grid">
              <div class="sbox">
                <div class="sl">运行结果分</div>
                <div class="sv mono">{{ num(scoreDetail?.autoScore) }}</div>
              </div>
              <div class="sbox">
                <div class="sl">互评原始分</div>
                <div class="sv mono">{{ num(current.peerRawScore) }}</div>
              </div>
              <div class="sbox">
                <div class="sl">教师评分</div>
                <div class="sv mono">{{ num(current.teacherScore) }}</div>
              </div>
              <div class="sbox">
                <div class="sl">最终成绩</div>
                <div class="sv mono" style="color:var(--gold)">{{ num(current.finalScore) }}</div>
              </div>
              <div class="sbox">
                <div class="sl">互评可信度</div>
                <div class="sv" style="font-size:13px;line-height:26px">
                  <span class="tag" :class="gradeTag(current.credibilityGrade)">{{ current.credibilityGrade || '—' }}</span>
                </div>
              </div>
            </div>

            <!-- 证据图片：后端返回逗号分隔的 /uploads/... 相对路径 -->
            <div class="sub-title">证据附件（{{ evidenceList.length }}）</div>
            <div v-if="evidenceList.length" class="imgs">
              <el-image
                v-for="(u, i) in evidenceList"
                :key="i"
                :src="u"
                :preview-src-list="evidenceList"
                :initial-index="i"
                fit="cover"
                preview-teleported
                style="width:104px;height:78px;border-radius:6px;border:1px solid var(--line)"
              />
            </div>
            <div v-else style="font-size:12px;color:var(--text-2)">学生未上传证据图片</div>

            <!-- 处理轨迹 -->
            <div class="sub-title">处理轨迹</div>
            <div class="timeline">
              <div class="tl-item">
                <div class="tl-time">{{ fmtTime(current.createTime) }}</div>
                <div class="tl-text">学生提交申诉（{{ current.appealType }}）</div>
              </div>
              <div v-if="current.handleTime" class="tl-item">
                <div class="tl-time">{{ fmtTime(current.handleTime) }}</div>
                <div class="tl-text">
                  处理人 {{ current.handlerName || '—' }} 处理为「{{ current.status }}」：{{ current.handleResult }}
                </div>
              </div>
              <div v-else class="tl-item pending">
                <div class="tl-time">待处理</div>
                <div class="tl-text">由非原评分教师受理，处理后学生可查看结果</div>
              </div>
            </div>
          </div>

          <!-- 处理表单：学生视角隐藏 -->
          <div v-if="!isStu" class="card">
            <div class="card-title" style="margin-bottom:12px">处理申诉</div>
            <div class="fgrid">
              <div class="it">
                <span class="lb req">处理状态</span>
                <el-select v-model="form.status" placeholder="请选择处理结果" style="width:200px">
                  <el-option v-for="s in handleStatuses" :key="s" :label="s" :value="s" />
                </el-select>
              </div>
              <div v-if="form.status === '已改分'" class="it">
                <span class="lb req">新分数</span>
                <el-input-number v-model="form.newScore" :min="0" :max="100" :precision="1" :step="0.5" />
              </div>
              <div class="it full">
                <span class="lb req">处理结果</span>
                <div style="flex:1">
                  <el-input
                    v-model="form.handleResult"
                    type="textarea"
                    :rows="3"
                    maxlength="300"
                    show-word-limit
                    placeholder="不少于 5 字；改分需写明新分数与依据"
                  />
                  <div style="font-size:11px;margin-top:4px" :style="{ color: resultLen >= 5 ? 'var(--success)' : 'var(--danger)' }">
                    已填 {{ resultLen }} 字 / 最少 5 字
                  </div>
                </div>
              </div>
            </div>
            <div style="margin-top:14px;display:flex;align-items:center;gap:12px">
              <el-button type="primary" :loading="handling" @click="submitHandle">提交处理</el-button>
              <span style="font-size:12px;color:var(--text-2)">原评分教师不能受理本人课程的申诉，改分会写入调整依据</span>
            </div>
          </div>

          <div v-else class="card">
            <div class="card-title" style="margin-bottom:8px">处理结果</div>
            <div v-if="current.handleResult" class="doc-block" style="max-height:120px">
              {{ current.handleResult }}
              <div style="margin-top:6px;font-size:11px;color:var(--text-2)">
                {{ current.handlerName || '—' }} · {{ fmtTime(current.handleTime) }}
              </div>
            </div>
            <el-empty v-else description="申诉已提交，等待教师处理" :image-size="70" />
          </div>
        </template>

        <div v-else class="card">
          <el-empty description="左侧暂无申诉记录" />
        </div>
      </div>
    </div>

    <!-- 学生发起申诉 -->
    <el-dialog v-model="createVisible" title="发起成绩申诉" width="520px">
      <el-form label-width="96px">
        <el-form-item label="成绩记录" required>
          <el-select v-model="createForm.scoreId" placeholder="请选择已发布的成绩" style="width:100%">
            <el-option
              v-for="s in scoreOptions"
              :key="s.id"
              :label="`${s.assignmentTitle} · 最终 ${s.finalScore ?? '—'} 分`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉类型" required>
          <el-select v-model="createForm.appealType" placeholder="请选择类型" style="width:200px">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望分数">
          <el-input-number v-model="createForm.expectScore" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="申诉理由" required>
          <el-input
            v-model="createForm.reason"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="不少于 20 字，说明具体问题（如第几行代码、哪项互评分数不合理）"
          />
        </el-form-item>
      </el-form>
      <div style="font-size:12px;margin-left:96px" :style="{ color: reasonLen >= 20 ? 'var(--success)' : 'var(--danger)' }">
        已填 {{ reasonLen }} 字 / 最少 20 字
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">提交申诉</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { store } from '@/store/user'
import { fmtTime } from '@/utils/format'

const isStu = computed(() => store.user.roleCode === 'STUDENT')

const loading = ref(false)
const handling = ref(false)
const creating = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref(null)
const current = ref(null)
const scoreDetail = ref(null)

const types = ref([])
const statuses = ref([])
const handleStatuses = ['处理中', '已驳回', '已改分', '需重新互评']

const form = reactive({ status: '', handleResult: '', newScore: 0 })
const resultLen = computed(() => (form.handleResult || '').trim().length)

const createVisible = ref(false)
const scoreOptions = ref([])
const createForm = reactive({ scoreId: null, appealType: '', reason: '', expectScore: null })
const reasonLen = computed(() => (createForm.reason || '').trim().length)

const headSub = computed(() => {
  if (isStu.value) return '学生视角：仅可查看本人申诉，成绩发布后 7 天内可提交，理由不少于 20 字'
  return '由非原评分教师受理 · 确认改分需写明新分数与依据 · 处理结果实时通知学生'
})

/** 证据图片：evidenceUrls 为逗号分隔的相对路径 */
const evidenceList = computed(() => {
  const raw = current.value?.evidenceUrls || ''
  return String(raw)
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
})

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return isNaN(n) ? '—' : n.toFixed(1)
}

function statusTag(v) {
  const map = { 待受理: 't-amber', 处理中: 't-blue', 已驳回: 't-red', 已改分: 't-green', 需重新互评: 't-amber' }
  return map[v] || 't-gray'
}

function gradeTag(g) {
  const map = { 高可信: 't-green', 中可信: 't-blue', 较低可信: 't-amber', 低可信: 't-red' }
  return map[g] || 't-gray'
}

async function load() {
  loading.value = true
  try {
    const d = await api.appealPage({ page: page.value, size: size.value, status: status.value || undefined })
    rows.value = d?.records || []
    total.value = d?.total || 0
    if (current.value) {
      const still = rows.value.find((r) => r.id === current.value.id)
      current.value = still || rows.value[0] || null
    } else {
      current.value = rows.value[0] || null
    }
    if (current.value) await loadScoreDetail(current.value)
  } catch (e) {
    rows.value = []
    total.value = 0
    current.value = null
  } finally {
    loading.value = false
  }
}

/** 申诉列表不含运行结果分，按学号回查成绩明细补齐成绩构成 */
async function loadScoreDetail(a) {
  scoreDetail.value = null
  if (!a?.studentNo) return
  try {
    const d = await api.scorePage({ page: 1, size: 50, keyword: a.studentNo })
    const list = d?.records || []
    scoreDetail.value = list.find((s) => Number(s.id) === Number(a.scoreId)) || list[0] || null
  } catch (e) {
    scoreDetail.value = null
  }
}

function select(a) {
  current.value = a
  form.status = ''
  form.handleResult = ''
  form.newScore = Number(a.finalScore ?? 0)
  loadScoreDetail(a)
}

function search() {
  page.value = 1
  load()
}

async function loadOptions() {
  try {
    const d = await api.options()
    types.value = d?.appealTypes || []
    statuses.value = d?.appealStatuses || []
  } catch (e) {
    types.value = []
    statuses.value = []
  }
}

/** 处理申诉：已改分必须填新分数，处理结果不少于 5 字 */
async function submitHandle() {
  if (!current.value) return
  if (!form.status) {
    ElMessage.warning('请选择处理状态')
    return
  }
  if (resultLen.value < 5) {
    ElMessage.warning('处理结果不少于 5 字')
    return
  }
  if (form.status === '已改分' && (form.newScore === null || form.newScore === undefined)) {
    ElMessage.warning('改分需填写新分数')
    return
  }
  handling.value = true
  try {
    await api.appealHandle(current.value.id, {
      status: form.status,
      handleResult: form.handleResult.trim(),
      newScore: form.status === '已改分' ? form.newScore : undefined
    })
    ElMessage.success(`申诉 ${current.value.appealNo} 已处理为「${form.status}」`)
    await load()
  } catch (e) {
    // 失败原因由拦截器提示
  } finally {
    handling.value = false
  }
}

/** 学生发起申诉：仅可选择本人已发布成绩 */
async function openCreate() {
  createForm.scoreId = null
  createForm.appealType = ''
  createForm.reason = ''
  createForm.expectScore = null
  createVisible.value = true
  try {
    const d = await api.scorePage({ page: 1, size: 100, publishStatus: '已发布' })
    scoreOptions.value = d?.records || []
  } catch (e) {
    scoreOptions.value = []
  }
}

async function submitCreate() {
  if (!createForm.scoreId) {
    ElMessage.warning('请选择要申诉的成绩记录')
    return
  }
  if (!createForm.appealType) {
    ElMessage.warning('请选择申诉类型')
    return
  }
  if (reasonLen.value < 20) {
    ElMessage.warning('申诉理由不少于 20 字')
    return
  }
  creating.value = true
  try {
    await api.appealCreate({
      scoreId: createForm.scoreId,
      appealType: createForm.appealType,
      reason: createForm.reason.trim(),
      expectScore: createForm.expectScore
    })
    ElMessage.success('申诉已提交，等待教师受理')
    createVisible.value = false
    page.value = 1
    await load()
  } catch (e) {
    // 已有处理中申诉等提示由拦截器给出
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  await loadOptions()
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
.list-card {
  padding: 12px;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.alist {
  max-height: 560px;
  overflow-y: auto;
}
.a-item {
  border: 1px solid var(--line);
  border-radius: var(--radius-s);
  padding: 9px 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.a-item:hover {
  border-color: var(--secondary);
}
.a-item.on {
  border-color: var(--primary);
  background: #f3f7f5;
  box-shadow: inset 3px 0 0 var(--primary);
}
.a-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 6px;
}
.a-name {
  font-size: 13px;
  font-weight: 600;
  margin-top: 6px;
}
.a-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 11.5px;
  color: var(--text-2);
  margin-top: 4px;
  line-height: 1.5;
}
.meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 7px 18px;
  font-size: 12.5px;
}
.meta > div {
  display: flex;
  gap: 8px;
}
.meta span {
  color: var(--text-2);
  flex: none;
  width: 60px;
}
.meta b {
  font-weight: 500;
  word-break: break-all;
}
.sub-title {
  font-size: 12px;
  color: var(--text-2);
  margin: 14px 0 6px;
}
.score-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
.sbox {
  border: 1px solid var(--line);
  border-radius: var(--radius-s);
  padding: 8px 10px;
  background: #fafbfc;
  min-width: 0;
}
.sbox .sl {
  font-size: 11.5px;
  color: var(--text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sbox .sv {
  font-size: 20px;
  font-weight: 600;
  line-height: 26px;
  font-variant-numeric: tabular-nums;
}
.imgs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
@media (max-width: 1100px) {
  .score-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .meta {
    grid-template-columns: 1fr;
  }
}
</style>
