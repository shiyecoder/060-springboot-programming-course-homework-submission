<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>互评分配</h1>
        <p class="sub">按作业智能分配互评任务 · 禁止自评 · 匿名评阅 · 截止前 2 小时自动催评</p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="loadProgress">刷新进度</el-button>
      </div>
    </div>

    <!-- 分配表单：作业下拉 200px + 互评份数 1-5 -->
    <div class="filter-bar">
      <el-select v-model="form.assignmentId" placeholder="请选择作业" style="width:200px" @change="loadProgress">
        <el-option
          v-for="a in assignments"
          :key="a.id"
          :label="`${a.title}（${a.status}）`"
          :value="a.id"
        />
      </el-select>
      <span style="font-size:13px;color:var(--text-2)">每人评</span>
      <el-input-number v-model="form.reviewCount" :min="1" :max="5" :step="1" style="width:120px" />
      <span style="font-size:13px;color:var(--text-2)">份</span>
      <el-button type="primary" :loading="assigning" :disabled="!form.assignmentId" @click="doAssign">
        智能分配
      </el-button>
      <span class="spacer"></span>
      <span style="font-size:12px;color:var(--text-2)">已分配过的提交会跳过，不会重复生成任务</span>
    </div>

    <div class="split right">
      <div class="table-card">
        <div class="card-title" style="padding:8px 2px 10px">
          <span>各作业互评任务分布</span>
          <span style="font-size:12px;color:var(--text-2);font-weight:400">
            共 {{ progressRows.length }} 个作业
          </span>
        </div>
        <el-table v-loading="loading" :data="pagedProgress" size="default" empty-text="暂无互评任务，请先分配">
          <el-table-column label="作业">
            <template #default="{ row }">
              <div>{{ row.title }}</div>
              <div style="font-size:11px;color:var(--text-2)">{{ row.courseName }} · {{ row.status }}</div>
            </template>
          </el-table-column>
          <el-table-column label="互评份数">
            <template #default="{ row }">
              <span class="mono">{{ row.peerReviewCount }}</span> 份/人
            </template>
          </el-table-column>
          <el-table-column label="任务总数">
            <template #default="{ row }"><span class="mono">{{ row.total ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column label="已评">
            <template #default="{ row }"><span class="mono">{{ row.done ?? 0 }}</span></template>
          </el-table-column>
          <el-table-column label="待评">
            <template #default="{ row }">
              <span class="mono">{{ (row.total ?? 0) - (row.done ?? 0) - (row.timeout ?? 0) - (row.exempt ?? 0) }}</span>
            </template>
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
              <div style="display:flex;align-items:center;gap:8px;min-width:110px">
                <el-progress
                  :percentage="Number(row.rate ?? 0)"
                  :stroke-width="6"
                  :show-text="false"
                  :color="rateColor(Number(row.rate ?? 0))"
                  style="flex:1"
                />
                <span class="mono" style="font-size:12px">{{ Number(row.rate ?? 0).toFixed(1) }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="平均分">
            <template #default="{ row }"><span class="mono">{{ row.avgScore ?? '—' }}</span></template>
          </el-table-column>
        </el-table>

        <div class="pager">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="progressRows.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
          />
        </div>
      </div>

      <div>
        <!-- 选中作业的分配概览 -->
        <div class="card">
          <div class="card-title" style="margin-bottom:10px">分配概览</div>
          <template v-if="current">
            <div class="ov-row"><span>作业</span><b>{{ current.title }}</b></div>
            <div class="ov-row"><span>课程</span><b>{{ current.courseName }}</b></div>
            <div class="ov-row"><span>任务总数</span><b class="mono">{{ current.total ?? 0 }}</b></div>
            <div class="ov-row"><span>已评 / 待评</span><b class="mono">{{ current.done ?? 0 }} / {{ pendingOf(current) }}</b></div>
            <div class="ov-row">
              <span>完成率</span>
              <b class="mono" :style="{ color: rateColor(Number(current.rate ?? 0)) }">
                {{ Number(current.rate ?? 0).toFixed(1) }}%
              </b>
            </div>
          </template>
          <el-empty v-else description="请选择作业查看分配情况" :image-size="70" />
        </div>

        <!-- 分配规则说明 -->
        <div class="card">
          <div class="card-title" style="margin-bottom:10px">分配规则</div>
          <div class="note">
            <b>1. 不能评自己</b>：评阅人 ≠ 被评提交作者，自评任务不会被生成。<br />
            <b>2. 全程匿名</b>：评阅人只看到「匿名同学」，作者信息在互评阶段隐藏。<br />
            <b>3. 截止前 2 小时催评</b>：未完成任务在互评截止前 2 小时自动推送提醒，超时记「超时未评」。<br />
            <b>4. 均衡分配</b>：同一份提交被评次数偏差不超过 1 次，已分配任务不重复生成。
          </div>
        </div>

        <div class="card">
          <div class="card-title" style="margin-bottom:10px">可信度影响</div>
          <div class="note">
            互评完成率占互评可信度 <b>20%</b>；超时未评会拉低该评阅人历史任务完成率，进而影响其评分的可信度等级与成绩折算。
          </div>
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

const loading = ref(false)
const assigning = ref(false)
const assignments = ref([])
const progressRows = ref([])
const page = ref(1)
const size = ref(10)

const form = reactive({
  assignmentId: null,
  reviewCount: 2
})

/** 选中的作业进度（未选择时取第一条，保证右侧有内容） */
const current = computed(() => {
  const id = form.assignmentId
  if (!id) return progressRows.value[0] || null
  return progressRows.value.find((r) => Number(r.id) === Number(id)) || null
})

/** 接口返回全量数组，此处做前端分页仅用于展示，不影响后端统计口径 */
const pagedProgress = computed(() => {
  const start = (page.value - 1) * size.value
  return progressRows.value.slice(start, start + size.value)
})

function pendingOf(row) {
  return Math.max(0, (row.total ?? 0) - (row.done ?? 0) - (row.timeout ?? 0) - (row.exempt ?? 0))
}

function rateColor(v) {
  if (v >= 85) return '#2B7955'
  if (v >= 60) return '#C2703A'
  return '#B02A37'
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
    if (!form.assignmentId && assignments.value.length) {
      form.assignmentId = assignments.value[0].id
    }
  } catch (e) {
    assignments.value = []
  }
}

async function loadProgress() {
  loading.value = true
  try {
    const d = await api.peerProgress()
    progressRows.value = Array.isArray(d) ? d : d?.records || []
    if (!form.assignmentId && progressRows.value.length) {
      form.assignmentId = progressRows.value[0].id
    }
  } catch (e) {
    progressRows.value = []
  } finally {
    loading.value = false
  }
}

/** 智能分配：返回 created/skipped，提示里同时给出份数与结果 */
async function doAssign() {
  if (!form.assignmentId) {
    ElMessage.warning('请先选择作业')
    return
  }
  assigning.value = true
  try {
    const d = await api.peerAssign({
      assignmentId: form.assignmentId,
      reviewCount: form.reviewCount
    })
    const created = d?.created ?? 0
    const skipped = d?.skipped ?? 0
    ElMessage.success(`分配完成：新增互评任务 ${created} 条，跳过已有任务 ${skipped} 份提交`)
    await loadProgress()
  } catch (e) {
    // 失败原因由请求拦截器统一提示，这里不重复弹窗
  } finally {
    assigning.value = false
  }
}

onMounted(async () => {
  await loadAssignments()
  await loadProgress()
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
.ov-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12.5px;
  padding: 6px 0;
  border-bottom: 1px dashed var(--line);
}
.ov-row:last-child {
  border-bottom: none;
}
.ov-row span {
  color: var(--text-2);
  flex: none;
}
.ov-row b {
  font-weight: 600;
  text-align: right;
  word-break: break-all;
}
.note {
  background: #f2f5f4;
  border: 1px solid var(--line);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  padding: 10px 12px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.85;
}
.note b {
  color: var(--text);
}
</style>
