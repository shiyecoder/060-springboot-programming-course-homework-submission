<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>{{ a.title || '作业详情' }}</h1>
        <p class="sub">
          <span class="mono">{{ a.assignmentNo || '—' }}</span>
          · {{ a.courseName || '—' }}（{{ a.className || '—' }}）
          · {{ a.languageType || '—' }} · 用例 {{ a.testCaseCount ?? 0 }} 条 · 互评 {{ a.peerReviewCount ?? 0 }} 份
        </p>
      </div>
      <div class="acts head-actions">
        <el-button @click="$router.push('/assignment')">返回台账</el-button>
        <!-- 流转按钮完全按接口 nextActions 渲染，前端不自行推断目标状态 -->
        <el-button
          v-for="act in nextActions"
          :key="act"
          plain
          :type="act === '已撤回' ? 'danger' : 'primary'"
          :loading="acting === act"
          @click="doTransition(act)"
        >
          {{ actionLabel(act) }}
        </el-button>
      </div>
    </div>

    <!-- 作业信息卡 -->
    <div class="card info-card">
      <div class="info-top">
        <div class="meta">
          <div class="m"><span>作业编号</span><b class="mono">{{ a.assignmentNo || '—' }}</b></div>
          <div class="m"><span>所属课程</span><b>{{ a.courseName || '—' }}</b></div>
          <div class="m"><span>教学班</span><b>{{ a.className || '—' }}</b></div>
          <div class="m"><span>编程语言</span><b>{{ a.languageType || '—' }}</b></div>
          <div class="m"><span>截止时间</span><b>{{ fmtTime(a.deadline) }}</b></div>
          <div class="m"><span>迟交截止</span><b>{{ fmtTime(a.lateDeadline) }}</b></div>
          <div class="m"><span>互评截止</span><b>{{ fmtTime(a.reviewDeadline) }}</b></div>
          <div class="m"><span>用例数量</span><b>{{ a.testCaseCount ?? '—' }} 条</b></div>
          <div class="m"><span>互评份数</span><b>{{ a.peerReviewCount ?? '—' }} 份</b></div>
          <div class="m"><span>成绩权重</span><b>{{ Number(a.weight ?? 0).toFixed(2) }} / 100</b></div>
          <div class="m"><span>任课教师</span><b>{{ a.teacherName || '—' }}</b></div>
          <div class="m"><span>状态</span><b><span class="tag" :class="statusCls(a.status)">{{ a.status || '—' }}</span></b></div>
        </div>
      </div>

      <!-- 状态机进度条：已过的节点置灰绿，当前节点描边 -->
      <div class="flow">
        <template v-for="(s, i) in FLOW" :key="s">
          <span class="st" :class="{ done: flowIndex > i, on: flowIndex === i }">{{ s }}</span>
          <span v-if="i < FLOW.length - 1" class="arw">›</span>
        </template>
        <span v-if="a.status === '已撤回'" class="tag t-red" style="margin-left:10px">已撤回（可重新发布）</span>
      </div>

      <div class="desc">
        <div class="desc-t">题目描述</div>
        <div class="doc-block">{{ a.description || '暂无题目描述' }}</div>
      </div>
    </div>

    <div class="split right">
      <!-- 提交列表 -->
      <div class="table-card">
        <div class="card-title" style="padding:6px 2px 0">
          <span>提交列表</span>
          <span class="muted">共 {{ subTotal }} 份提交</span>
        </div>
        <el-table v-loading="subLoading" :data="subs" row-key="id" empty-text="暂无提交记录">
          <el-table-column label="提交编号">
            <template #default="{ row }"><span class="mono">{{ row.submitNo }}</span></template>
          </el-table-column>
          <el-table-column label="学生">
            <template #default="{ row }">{{ row.studentName || '—' }}</template>
          </el-table-column>
          <el-table-column label="学号">
            <template #default="{ row }"><span class="mono">{{ row.studentNo || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="编译结果">
            <template #default="{ row }">
              <span class="tag" :class="compileCls(row.compileResult)">{{ row.compileResult || '未运行' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用例通过">
            <template #default="{ row }">
              <span class="mono">{{ row.passCount ?? 0 }}/{{ row.totalCount ?? row.testCaseCount ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="迟交">
            <template #default="{ row }">
              <span v-if="row.isLate === 1" class="tag t-red">迟交</span>
              <span v-else class="tag t-gray">按时</span>
            </template>
          </el-table-column>
          <el-table-column label="代码行数" align="right">
            <template #default="{ row }">{{ row.codeLines ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="提交时间">
            <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="状态">
            <template #default="{ row }">
              <span class="tag" :class="subStatusCls(row.status)">{{ row.status || '待登记' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="subQuery.page"
            v-model:page-size="subQuery.size"
            :total="subTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="onSubSize"
            @current-change="loadSubs"
          />
        </div>
      </div>

      <!-- 已提交统计 -->
      <div class="side">
        <div class="card">
          <div class="card-title">已提交统计</div>
          <div class="stat4">
            <div><div class="k">已提交</div><div class="v">{{ a.submitCount ?? 0 }} / {{ a.studentCount ?? 0 }}</div></div>
            <div><div class="k">未提交</div><div class="v">{{ Math.max(0, (a.studentCount ?? 0) - (a.submitCount ?? 0)) }}</div></div>
            <div><div class="k">迟交</div><div class="v red">{{ a.lateCount ?? 0 }} 份</div></div>
            <div><div class="k">用例平均通过</div><div class="v">{{ n1(a.avgPass) }}%</div></div>
          </div>
          <div class="rate-line">
            <span>提交率</span>
            <el-progress :percentage="submitRate" :stroke-width="6" :show-text="false" style="flex:1" />
            <b>{{ submitRate }}%</b>
          </div>
        </div>

        <div class="card">
          <div class="card-title">用例与互评配置</div>
          <div class="kv"><span>用例数量</span><b>{{ a.testCaseCount ?? '—' }} 条（登记上限）</b></div>
          <div class="kv"><span>互评份数</span><b>{{ a.peerReviewCount ?? '—' }} 份 / 人</b></div>
          <div class="kv"><span>互评截止</span><b>{{ fmtTime(a.reviewDeadline) }}</b></div>
          <div class="kv"><span>成绩权重</span><b>{{ Number(a.weight ?? 0).toFixed(2) }} / 100</b></div>
          <div class="kv"><span>创建时间</span><b>{{ fmtTime(a.createTime) }}</b></div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !a.id" description="作业不存在或已删除" />
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const FLOW = ['草稿', '已发布', '提交中', '互评中', '评分中', '已归档']

const loading = ref(false)
const acting = ref('')
const a = ref({})

const subs = ref([])
const subTotal = ref(0)
const subLoading = ref(false)
const subQuery = reactive({ page: 1, size: 10, assignmentId: null })

const nextActions = computed(() => a.value.nextActions || [])
const flowIndex = computed(() => Math.max(0, FLOW.indexOf(a.value.status)))
const n1 = (v) => (v === null || v === undefined ? '0.0' : Number(v).toFixed(1))
const submitRate = computed(() => {
  const all = Number(a.value.studentCount || 0)
  if (!all) return 0
  return Math.min(100, Math.round((Number(a.value.submitCount || 0) * 100) / all))
})

function statusCls(s) {
  const map = { 草稿: 't-gray', 已发布: 't-blue', 提交中: 't-blue', 互评中: 't-amber', 评分中: 't-amber', 已归档: 't-green', 已撤回: 't-red' }
  return map[s] || 't-gray'
}
function compileCls(s) {
  return { 通过: 't-green', 编译失败: 't-red' }[s] || 't-gray'
}
function subStatusCls(s) {
  const map = { 待登记: 't-gray', 已登记: 't-blue', 已进入互评: 't-blue', 已评分: 't-green' }
  return map[s] || 't-gray'
}
/** 按钮文案本地化，避免直接把状态名当按钮 */
function actionLabel(target) {
  const map = { 已发布: '发布作业', 提交中: '进入提交中', 互评中: '进入互评', 评分中: '进入评分', 已归档: '归档作业', 已撤回: '撤回作业' }
  return map[target] || target
}

async function loadDetail() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    a.value = (await api.assignmentDetail(id)) || {}
    subQuery.assignmentId = Number(id)
    subQuery.page = 1
    await loadSubs()
  } catch (e) {
    a.value = {}
  } finally {
    loading.value = false
  }
}

async function loadSubs() {
  if (!subQuery.assignmentId) return
  subLoading.value = true
  try {
    const d = await api.submissionPage({ ...subQuery })
    subs.value = d?.records || []
    subTotal.value = d?.total || 0
  } catch (e) {
    subs.value = []
    subTotal.value = 0
  } finally {
    subLoading.value = false
  }
}
function onSubSize() {
  subQuery.page = 1
  loadSubs()
}

/**
 * 状态流转：
 * - 撤回必须填写原因（后端强校验），因此用 prompt 收集并做长度校验；
 * - 其余流转用 confirm，确认文案复述作业编号与起止状态。
 */
async function doTransition(target) {
  const cur = a.value.status
  const no = a.value.assignmentNo || ''
  try {
    if (target === '已撤回') {
      const { value } = await ElMessageBox.prompt(
        `撤回作业 ${no} 后学生将无法提交，请填写撤回原因（不少于 4 字）`,
        '撤回作业',
        {
          type: 'warning',
          confirmButtonText: '确认撤回',
          cancelButtonText: '取消',
          inputType: 'textarea',
          inputPlaceholder: '如：题目描述有误，需重新发布',
          inputValidator: (v) => (v && v.trim().length >= 4 ? true : '撤回原因不少于 4 字')
        }
      )
      acting.value = target
      await api.transition(a.value.id, { target, reason: value.trim() })
    } else {
      await ElMessageBox.confirm(
        `确认将作业 ${no} 从「${cur}」流转到「${target}」？流转后不可回退到上一状态。`,
        '作业状态流转',
        { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
      )
      acting.value = target
      await api.transition(a.value.id, { target })
    }
    ElMessage.success(`已流转到「${target}」`)
    await loadDetail()
  } catch (e) {
    // 取消弹窗或后端拒绝：保持当前页面，不提示错误
  } finally {
    acting.value = ''
  }
}

watch(() => route.params.id, () => loadDetail())
onMounted(loadDetail)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.muted { color: #5B6770; font-size: 12px; }
.info-card { padding: 14px 18px; }
.meta { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px 18px; }
.meta .m { display: flex; gap: 8px; font-size: 12.5px; line-height: 22px; min-width: 0; }
.meta .m span { color: #5B6770; flex: none; }
.meta .m b { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.flow { display: flex; align-items: center; gap: 8px; margin: 12px 0; padding: 8px 12px; background: #FAFBFC; border: 1px solid #DEE4E9; border-radius: 6px; flex-wrap: wrap; }
.flow .st { font-size: 12px; color: #5B6770; padding: 1px 9px; border-radius: 10px; border: 1px solid transparent; }
.flow .st.done { color: #2B7955; background: #E3F0E9; }
.flow .st.on { color: #1E4B3F; background: #E7F0EC; border-color: #A9C0B6; font-weight: 600; }
.flow .arw { color: #B7C2BC; }
.desc-t { font-size: 13px; font-weight: 600; margin-bottom: 6px; }
.side { display: flex; flex-direction: column; gap: 12px; }
.stat4 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.stat4 .k { font-size: 11.5px; color: #5B6770; }
.stat4 .v { font-size: 18px; font-weight: 600; color: #1E4B3F; font-variant-numeric: tabular-nums; }
.stat4 .v.red { color: #B02A37; }
.rate-line { display: flex; align-items: center; gap: 8px; margin-top: 12px; font-size: 12.5px; color: #5B6770; }
.rate-line b { color: #1F2933; font-variant-numeric: tabular-nums; }
.kv { display: flex; justify-content: space-between; font-size: 12.5px; padding: 5px 0; border-bottom: 1px dashed #E8EDEA; }
.kv:last-child { border-bottom: 0; }
.kv span { color: #5B6770; }
.kv b { font-weight: 500; }
</style>
