<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>结果登记</h1>
        <p class="sub">系统只存结果不执行代码 · 通过数不得超过用例总数 · 编译失败必须填写错误摘要</p>
      </div>
      <div class="acts head-actions">
        <el-button @click="$router.push('/result/import')">批量导入</el-button>
        <el-button type="primary" :disabled="!selected.length" @click="openBatch">
          批量登记{{ selected.length ? `（${selected.length}）` : '' }}
        </el-button>
      </div>
    </div>

    <!-- 汇总：按当前作业的全部提交抽样统计，避免只统计当前页造成误读 -->
    <div class="kpis" style="margin-bottom:12px">
      <div class="kpi">
        <div class="kpi-l">已提交份数</div>
        <div class="kpi-v">{{ stat.total }}<small>份</small></div>
        <div class="kpi-f">当前作业的提交记录数</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">已登记</div>
        <div class="kpi-v">{{ stat.done }}<small>份</small></div>
        <div class="kpi-f">统计样本 {{ stat.sample }} 条</div>
      </div>
      <div class="kpi warn">
        <div class="kpi-l">编译失败</div>
        <div class="kpi-v">{{ stat.fail }}<small>份</small></div>
        <div class="kpi-f">需补错误摘要</div>
      </div>
      <div class="kpi">
        <div class="kpi-l">用例平均通过</div>
        <div class="kpi-v">{{ stat.avg }}<small>%</small></div>
        <div class="kpi-f">按通过数 / 用例总数</div>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.assignmentId" placeholder="选择作业" filterable style="width:200px" @change="onAssignmentChange">
        <el-option v-for="item in assignments" :key="item.id" :label="`${item.assignmentNo} · ${item.title}`" :value="item.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width:200px" @change="search">
        <el-option v-for="s in ['待登记', '已登记', '已进入互评', '已评分']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="学号 / 姓名"
        clearable
        style="width:220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div class="spacer"></div>
      <span class="tip">点击任意行展开登记面板</span>
    </div>

    <div class="table-card">
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="rows"
        row-key="id"
        empty-text="暂无提交记录"
        @row-click="onRowClick"
        @selection-change="onSelect"
        @expand-change="onExpand"
      >
        <el-table-column type="selection" width="42" />
        <el-table-column type="expand">
          <template #default="{ row }">
            <!-- 四段式行内登记：编译结果 / 通过数 / 错误摘要 / 保存 -->
            <div class="expand">
              <div class="seg">
                <div class="seg-t">编译结果</div>
                <el-radio-group v-model="drafts[row.id].compileResult" size="small" @change="onCompileChange(row)">
                  <el-radio-button v-for="c in COMPILE" :key="c" :label="c">{{ c }}</el-radio-button>
                </el-radio-group>
              </div>
              <div class="seg">
                <div class="seg-t">用例通过数（上限 {{ totalOf(row) }}）</div>
                <el-input-number
                  v-model="drafts[row.id].passCount"
                  :min="0"
                  :max="totalOf(row)"
                  size="small"
                  controls-position="right"
                  style="width:140px"
                  :disabled="drafts[row.id].compileResult === '编译失败'"
                />
              </div>
              <div class="seg grow">
                <div class="seg-t">错误摘要{{ drafts[row.id].compileResult === '编译失败' ? '（必填）' : '（选填）' }}</div>
                <el-input
                  v-model="drafts[row.id].errorSummary"
                  size="small"
                  maxlength="200"
                  placeholder="如：error: cannot find symbol — 第 22 行变量未声明"
                />
              </div>
              <div class="seg act">
                <el-button size="small" type="primary" :loading="savingId === row.id" @click="saveRow(row)">保存</el-button>
                <el-button size="small" @click="resetDraft(row)">重置</el-button>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学号">
          <template #default="{ row }"><span class="mono">{{ row.studentNo || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="姓名">
          <template #default="{ row }">{{ row.studentName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="className" label="班级" />
        <el-table-column label="提交时间">
          <template #default="{ row }">{{ fmtTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column label="代码行数" align="right">
          <template #default="{ row }">{{ row.codeLines ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="版本" align="right">
          <template #default="{ row }">v{{ row.submitCount ?? 1 }}</template>
        </el-table-column>
        <el-table-column label="编译结果">
          <template #default="{ row }">
            <span class="tag" :class="compileCls(row.compileResult)">{{ row.compileResult || '未运行' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用例通过">
          <template #default="{ row }">
            <span class="mono">{{ row.passCount ?? '—' }}/{{ totalOf(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="迟交">
          <template #default="{ row }">
            <span v-if="row.isLate === 1" class="tag t-red">迟交</span>
            <span v-else class="tag t-gray">按时</span>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <span class="tag" :class="row.status === '待登记' ? 't-gray' : 't-blue'">{{ row.status || '待登记' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <div class="ops">
              <el-button link type="primary" @click.stop="toggle(row)">展开登记</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="onSizeChange"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 批量登记：同一组结果一次提交，后端按 ids 批量更新，最后一次为准 -->
    <el-dialog v-model="batch" title="批量登记运行结果" width="460px" destroy-on-close>
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        :title="`将对已选 ${selected.length} 份提交写入同一组结果，请确认后提交`"
        style="margin-bottom:12px"
      />
      <el-form label-width="92px">
        <el-form-item label="编译结果">
          <el-select v-model="batchForm.compileResult" style="width:100%">
            <el-option v-for="c in COMPILE" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item :label="`通过数`">
          <el-input-number
            v-model="batchForm.passCount"
            :min="0"
            :max="batchMax"
            controls-position="right"
            style="width:100%"
            :disabled="batchForm.compileResult === '编译失败'"
          />
          <div class="tip" style="margin-top:4px">所选记录用例上限最小为 {{ batchMax }}，通过数不能超过该值</div>
        </el-form-item>
        <el-form-item label="错误摘要">
          <el-input v-model="batchForm.errorSummary" type="textarea" :rows="3" maxlength="200" placeholder="编译失败时必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batch = false">取消</el-button>
        <el-button type="primary" :loading="batchSaving" @click="saveBatch">确认批量登记</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const COMPILE = ['通过', '编译失败', '未运行']
const route = useRoute()
const tableRef = ref(null)

const assignments = ref([])
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drafts = reactive({})
const selected = ref([])
const savingId = ref(null)
const stat = reactive({ total: 0, done: 0, fail: 0, avg: '0.0', sample: 0 })

const query = reactive({ page: 1, size: 10, assignmentId: null, status: '', keyword: '' })

const batch = ref(false)
const batchSaving = ref(false)
const batchForm = reactive({ compileResult: '通过', passCount: 0, errorSummary: '' })

/** 用例总数：优先接口 totalCount，退回作业配置的 testCaseCount */
function totalOf(row) {
  return Number(row?.totalCount ?? row?.testCaseCount ?? 0)
}
function compileCls(s) {
  return { 通过: 't-green', 编译失败: 't-red' }[s] || 't-gray'
}

function initDrafts() {
  rows.value.forEach((r) => {
    drafts[r.id] = {
      compileResult: r.compileResult || '通过',
      passCount: Number(r.passCount ?? 0),
      errorSummary: r.errorSummary || ''
    }
  })
}

/** 编译失败时通过数强制归零，与后端落库口径一致 */
function onCompileChange(row) {
  if (drafts[row.id].compileResult === '编译失败') drafts[row.id].passCount = 0
}

async function load() {
  if (!query.assignmentId) return
  loading.value = true
  try {
    const d = await api.submissionPage({ ...query })
    rows.value = d?.records || []
    total.value = d?.total || 0
    initDrafts()
    await loadStat()
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 统计口径：取该作业前 100 条提交（接口单页上限 100），在页面上注明样本量 */
async function loadStat() {
  try {
    const d = await api.submissionPage({ page: 1, size: 100, assignmentId: query.assignmentId })
    const rs = d?.records || []
    stat.total = d?.total || 0
    stat.sample = rs.length
    stat.done = rs.filter((r) => r.status && r.status !== '待登记').length
    stat.fail = rs.filter((r) => r.compileResult === '编译失败').length
    const pcts = rs.filter((r) => totalOf(r) > 0).map((r) => (Number(r.passCount || 0) * 100) / totalOf(r))
    stat.avg = pcts.length ? (pcts.reduce((a, b) => a + b, 0) / pcts.length).toFixed(1) : '0.0'
  } catch (e) {
    stat.total = total.value
    stat.sample = rows.value.length
    stat.done = 0
    stat.fail = 0
    stat.avg = '0.0'
  }
}

function search() {
  query.page = 1
  load()
}
function reset() {
  query.status = ''
  query.keyword = ''
  search()
}
function onSizeChange() {
  query.page = 1
  load()
}
function onAssignmentChange() {
  query.page = 1
  selected.value = []
  load()
}
function onSelect(sel) {
  selected.value = sel
}

/** 行点击展开：复选框与展开列自身不触发，避免误展开 */
function onRowClick(row, column) {
  if (column && (column.type === 'selection' || column.type === 'expand')) return
  toggle(row)
}
function toggle(row) {
  tableRef.value?.toggleRowExpansion(row)
}
/** 展开时补齐草稿；expand-change 第二参在展开列下是行数组，所以只判断草稿是否存在 */
function onExpand(row) {
  if (!drafts[row.id]) {
    drafts[row.id] = {
      compileResult: row.compileResult || '通过',
      passCount: Number(row.passCount ?? 0),
      errorSummary: row.errorSummary || ''
    }
  }
}
function resetDraft(row) {
  drafts[row.id] = {
    compileResult: row.compileResult || '通过',
    passCount: Number(row.passCount ?? 0),
    errorSummary: row.errorSummary || ''
  }
}

/** 单行登记：前端先做与后端一致的校验，减少无效请求 */
async function saveRow(row) {
  const d = drafts[row.id]
  if (!d) return
  if (d.compileResult === '编译失败' && !String(d.errorSummary || '').trim()) {
    return ElMessage.warning('编译失败必须填写错误摘要')
  }
  if (Number(d.passCount) > totalOf(row)) {
    return ElMessage.warning(`通过数不能大于用例总数 ${totalOf(row)}`)
  }
  savingId.value = row.id
  try {
    await api.resultEntry({
      id: row.id,
      compileResult: d.compileResult,
      passCount: d.compileResult === '编译失败' ? 0 : Number(d.passCount),
      errorSummary: d.errorSummary
    })
    ElMessage.success(`已登记 ${row.studentName} 的结果`)
    load()
  } catch (e) {
    // 后端异常由拦截器提示
  } finally {
    savingId.value = null
  }
}

const batchMax = computed(() => {
  const list = selected.value.map(totalOf).filter((n) => n > 0)
  return list.length ? Math.min(...list) : 100
})

function openBatch() {
  if (!selected.value.length) return ElMessage.warning('请先勾选需要登记的提交')
  batchForm.compileResult = '通过'
  batchForm.passCount = 0
  batchForm.errorSummary = ''
  batch.value = true
}

async function saveBatch() {
  if (batchForm.compileResult === '编译失败' && !String(batchForm.errorSummary || '').trim()) {
    return ElMessage.warning('编译失败必须填写错误摘要')
  }
  if (Number(batchForm.passCount) > batchMax.value) {
    return ElMessage.warning(`通过数不能大于所选记录的最小用例数 ${batchMax.value}`)
  }
  batchSaving.value = true
  try {
    const d = await api.resultEntry({
      ids: selected.value.map((r) => r.id),
      compileResult: batchForm.compileResult,
      passCount: batchForm.compileResult === '编译失败' ? 0 : Number(batchForm.passCount),
      errorSummary: batchForm.errorSummary
    })
    ElMessage.success(`已批量登记 ${d?.count ?? selected.value.length} 条`)
    batch.value = false
    load()
  } catch (e) {
    // 后端异常由拦截器提示
  } finally {
    batchSaving.value = false
  }
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
    const qid = Number(route.query.assignmentId)
    const inProgress = assignments.value.find((x) => ['提交中', '互评中', '评分中'].includes(x.status))
    query.assignmentId = qid || inProgress?.id || assignments.value[0]?.id || null
    if (query.assignmentId) load()
  } catch (e) {
    assignments.value = []
  }
}

onMounted(loadAssignments)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.tip { font-size: 11.5px; color: #5B6770; }
.ops { white-space: nowrap; }
.expand {
  display: flex; align-items: flex-end; gap: 16px;
  padding: 12px 14px; background: #FAFBFC; border: 1px solid #DEE4E9; border-radius: 6px;
}
.expand .seg { min-width: 0; }
.expand .seg.grow { flex: 1; }
.expand .seg.act { display: flex; gap: 6px; }
.seg-t { font-size: 12px; color: #5B6770; margin-bottom: 6px; }
</style>
