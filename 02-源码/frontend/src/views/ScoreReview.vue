<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>成绩审核</h1>
        <p class="sub">
          待审核（未发布）成绩按作业分组 · 改分需填写不少于 10 字依据 · 发布后只能走申诉
        </p>
      </div>
      <div class="head-actions">
        <el-button :disabled="!assignmentId" :loading="composing" @click="doCompose">合成成绩</el-button>
        <el-button type="primary" :disabled="!assignmentId" :loading="publishing" @click="doPublish">
          发布成绩
        </el-button>
      </div>
    </div>

    <!-- 成绩合成口径说明 -->
    <div class="card" style="padding:11px 16px;display:flex;gap:18px;align-items:center;flex-wrap:wrap">
      <span style="font-size:13px;color:var(--text-2)">合成口径</span>
      <span class="tag t-blue">高可信：互评直接计入</span>
      <span class="tag t-amber">中可信：互评 ×0.8</span>
      <span class="tag t-gray">较低可信：互评仅参考</span>
      <span class="tag t-red">低可信：互评不计入</span>
      <span style="flex:1"></span>
      <span style="font-size:12px;color:var(--text-2)">
        最终成绩 = 运行结果 20% + 互评（折算后）20% + 教师 60%；低可信时按 运行 30% + 教师 70%
      </span>
    </div>

    <div class="filter-bar">
      <el-select v-model="assignmentId" placeholder="全部作业（未发布）" clearable style="width:200px" @change="search">
        <el-option v-for="a in assignments" :key="a.id" :label="a.title" :value="a.id" />
      </el-select>
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="spacer"></span>
      <span style="font-size:12px;color:var(--text-2)">
        共 {{ total }} 条待发布成绩 · 合成/发布需先选择具体作业
      </span>
    </div>

    <div v-loading="loading">
      <!-- 按作业分组展示 -->
      <div v-for="g in groups" :key="g.assignmentId" class="card" style="padding:12px 14px 4px">
        <div class="card-title" style="margin-bottom:10px">
          <span>
            {{ g.assignmentTitle }}
            <span style="font-size:12px;font-weight:400;color:var(--text-2)"> · {{ g.courseName }}</span>
          </span>
          <span style="font-size:12px;font-weight:400;color:var(--text-2)">
            本页 {{ g.rows.length }} 条 · 权重 {{ g.weight ?? '—' }}%
          </span>
        </div>

        <el-table :data="g.rows" size="default" empty-text="暂无数据">
          <el-table-column label="学生">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:8px">
                <img
                  v-if="row.studentAvatar"
                  :src="row.studentAvatar"
                  alt="头像"
                  style="width:24px;height:24px;border-radius:50%;object-fit:cover"
                />
                <span>{{ row.studentName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="学号">
            <template #default="{ row }"><span class="mono">{{ row.studentNo }}</span></template>
          </el-table-column>
          <el-table-column prop="className" label="班级" />
          <el-table-column label="运行结果分">
            <template #default="{ row }"><span class="mono">{{ num(row.autoScore) }}</span></template>
          </el-table-column>
          <el-table-column label="互评原始">
            <template #default="{ row }"><span class="mono">{{ num(row.peerRawScore) }}</span></template>
          </el-table-column>
          <el-table-column label="互评折算">
            <template #default="{ row }">
              <span class="mono" :style="{ color: peerFoldedColor(row) }">{{ num(row.peerScore) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="教师评分">
            <template #default="{ row }"><span class="mono">{{ num(row.teacherScore) }}</span></template>
          </el-table-column>
          <el-table-column label="最终成绩">
            <template #default="{ row }">
              <b class="mono" style="font-size:14px">{{ num(row.finalScore) }}</b>
              <div v-if="row.adjustReason" style="font-size:11px;color:var(--warn)">已人工调整</div>
            </template>
          </el-table-column>
          <el-table-column label="可信度">
            <template #default="{ row }">
              <span class="tag" :class="gradeTag(row.credibilityGrade)">{{ row.credibilityGrade || '—' }}</span>
              <div class="mono" style="font-size:11px;color:var(--text-2)">{{ num(row.credibilityScore) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="班级排名">
            <template #default="{ row }"><span class="mono">{{ row.classRank ?? '—' }}</span></template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <div style="white-space:nowrap">
                <el-button link type="primary" @click="openAdjust(row)">改分</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="!loading && !groups.length" class="card">
        <el-empty description="没有待审核成绩，请先合成成绩或切换作业" />
      </div>
    </div>

    <div class="pager" v-if="total > 0">
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

    <!-- 改分弹窗：新分数 + 依据 ≥10 字 -->
    <el-dialog v-model="adjustVisible" title="人工改分" width="480px">
      <div v-if="adjustRow" class="adjust-head">
        <span>{{ adjustRow.studentName }} · <span class="mono">{{ adjustRow.studentNo }}</span></span>
        <span style="color:var(--text-2)">原最终成绩 <b class="mono">{{ num(adjustRow.finalScore) }}</b></span>
      </div>
      <el-form label-width="88px" style="margin-top:12px">
        <el-form-item label="新分数">
          <el-input-number v-model="adjustForm.finalScore" :min="0" :max="100" :precision="1" :step="0.5" />
        </el-form-item>
        <el-form-item label="调整依据">
          <el-input
            v-model="adjustForm.adjustReason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="不少于 10 字，写明改分原因与依据"
          />
        </el-form-item>
      </el-form>
      <div style="font-size:12px;color:var(--text-2)">已填 {{ adjustReasonLen }} 字 / 最少 10 字</div>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjusting" @click="submitAdjust">保存改分</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'

const loading = ref(false)
const composing = ref(false)
const publishing = ref(false)
const adjusting = ref(false)
const assignments = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const assignmentId = ref(null)

const adjustVisible = ref(false)
const adjustRow = ref(null)
const adjustForm = reactive({ finalScore: 0, adjustReason: '' })
const adjustReasonLen = computed(() => (adjustForm.adjustReason || '').trim().length)

/** 按作业分组（后端分页，本页可能落在多个作业） */
const groups = computed(() => {
  const map = new Map()
  for (const r of rows.value) {
    const key = r.assignmentId
    if (!map.has(key)) {
      map.set(key, {
        assignmentId: key,
        assignmentTitle: r.assignmentTitle,
        courseName: r.courseName,
        weight: r.weight,
        rows: []
      })
    }
    map.get(key).rows.push(r)
  }
  return [...map.values()]
})

function num(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  return isNaN(n) ? '—' : n.toFixed(1)
}

function gradeTag(g) {
  const map = { 高可信: 't-green', 中可信: 't-blue', 较低可信: 't-amber', 低可信: 't-red' }
  return map[g] || 't-gray'
}

/** 互评折算与原始分不同则说明按等级折算过 */
function peerFoldedColor(row) {
  if (row.peerRawScore == null || row.peerScore == null) return 'inherit'
  if (Number(row.peerRawScore) === Number(row.peerScore)) return 'inherit'
  return Number(row.peerScore) < Number(row.peerRawScore) ? 'var(--warn)' : 'inherit'
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
    const d = await api.scorePage({
      page: page.value,
      size: size.value,
      assignmentId: assignmentId.value || undefined,
      publishStatus: '未发布'
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
  assignmentId.value = null
  page.value = 1
  load()
}

function onSizeChange() {
  page.value = 1
  load()
}

async function doCompose() {
  if (!assignmentId.value) return
  composing.value = true
  try {
    const d = await api.scoreCompose({ assignmentId: assignmentId.value })
    ElMessage.success(`成绩合成完成，共更新 ${d?.count ?? 0} 条（互评分已按可信度等级折算）`)
    page.value = 1
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    composing.value = false
  }
}

/** 发布成绩：危险操作，二次确认里复述作业名与影响范围 */
async function doPublish() {
  if (!assignmentId.value) return
  const a = assignments.value.find((x) => Number(x.id) === Number(assignmentId.value))
  try {
    await ElMessageBox.confirm(
      `确认发布【${a?.title || assignmentId.value}】的未发布成绩？发布后学生立即可见，改分只能走申诉流程。`,
      '发布成绩确认',
      { type: 'warning', confirmButtonText: '确认发布', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  publishing.value = true
  try {
    const d = await api.scorePublish({ assignmentId: assignmentId.value })
    ElMessage.success(`已发布 ${d?.count ?? 0} 条成绩，并已通知学生`)
    await load()
  } catch (e) {
    // 未完成互评等错误由拦截器提示
  } finally {
    publishing.value = false
  }
}

function openAdjust(row) {
  adjustRow.value = row
  adjustForm.finalScore = Number(row.finalScore ?? 0)
  adjustForm.adjustReason = row.adjustReason || ''
  adjustVisible.value = true
}

async function submitAdjust() {
  if (!adjustRow.value) return
  if (adjustReasonLen.value < 10) {
    ElMessage.warning('人工调整必须填写不少于 10 字的依据')
    return
  }
  adjusting.value = true
  try {
    await api.scoreAdjust(adjustRow.value.id, {
      finalScore: Number(adjustForm.finalScore),
      adjustReason: adjustForm.adjustReason.trim()
    })
    ElMessage.success(`${adjustRow.value.studentName} 的最终成绩已改为 ${adjustForm.finalScore}`)
    adjustVisible.value = false
    await load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    adjusting.value = false
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
.adjust-head {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--line);
}
</style>
