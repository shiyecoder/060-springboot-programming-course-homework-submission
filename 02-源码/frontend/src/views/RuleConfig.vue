<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>互评可信度规则配置</h1>
        <p class="sub">
          五因子权重合计必须为 100% · 保存后生成新版本并立即生效 · 历史快照仍按计算时的规则版本留痕
        </p>
      </div>
      <div class="acts">
        <el-button @click="load">重新读取</el-button>
        <el-button type="primary" :disabled="!canSave" :loading="saving" @click="save">保存并生效</el-button>
      </div>
    </div>

    <div class="rule-grid">
      <div>
        <!-- 权重配置：合计不等于 100 时标红并禁止保存 -->
        <div class="card">
          <h3 class="card-title">
            <span>五因子权重</span>
            <span :style="{ color: weightValid ? 'var(--success)' : 'var(--danger)', fontSize: '13px' }">
              合计 {{ weightSum }}%{{ weightValid ? '' : '（必须等于 100%）' }}
            </span>
          </h3>

          <div v-for="f in factorFields" :key="f.key" class="factor-row rule-row">
            <div class="fname">
              {{ f.label }}
              <small style="display:block;font-size:11px;color:var(--text-2)">{{ f.hint }}</small>
            </div>
            <el-input-number
              v-model="form[f.key]"
              :min="0"
              :max="100"
              :step="1"
              :precision="0"
              controls-position="right"
              style="width:100%"
            />
            <div class="fval">{{ form[f.key] }}%</div>
          </div>

          <div class="weight-note" :class="{ bad: !weightValid }">
            权重口径：可信度分 = Σ（单项归一化得分 × 权重 / 100）。任一项权重调整后，其余项需同步增减，保证合计恒为 100%。
          </div>
        </div>

        <!-- 等级阈值与相似度告警线 -->
        <div class="card">
          <h3 class="card-title">等级阈值与告警线</h3>
          <div class="fgrid">
            <div class="it">
              <div class="lb req">高可信线</div>
              <el-input-number v-model="form.gradeHigh" :min="0" :max="100" :step="1" controls-position="right" style="width:180px" />
            </div>
            <div class="it">
              <div class="lb req">中可信线</div>
              <el-input-number v-model="form.gradeMid" :min="0" :max="100" :step="1" controls-position="right" style="width:180px" />
            </div>
            <div class="it">
              <div class="lb req">较低可信线</div>
              <el-input-number v-model="form.gradeLow" :min="0" :max="100" :step="1" controls-position="right" style="width:180px" />
            </div>
            <div class="it">
              <div class="lb req">相似度告警</div>
              <el-input-number v-model="form.similarityAlert" :min="0" :max="100" :step="1" controls-position="right" style="width:180px" />
            </div>
            <div class="it full">
              <div class="lb">阈值预览</div>
              <div class="threshold-preview">
                <span class="tag t-green">高可信 ≥ {{ form.gradeHigh }}</span>
                <span class="tag t-blue">中可信 {{ form.gradeMid }}-{{ Math.max(form.gradeHigh - 1, 0) }}</span>
                <span class="tag t-amber">较低可信 {{ form.gradeLow }}-{{ Math.max(form.gradeMid - 1, 0) }}</span>
                <span class="tag t-red">低可信 &lt; {{ form.gradeLow }}</span>
                <span class="tag t-gray">相似度 &gt; {{ form.similarityAlert }} 告警</span>
              </div>
            </div>
          </div>
          <div style="font-size:11.5px;color:var(--text-2);margin-top:10px;line-height:1.8">
            等级决定互评分如何计入成绩：高可信直接计入；中可信按 0.8 折算；较低可信仅作参考且教师权重上调至 60%；低可信不计入并需追加互评或教师复核。
          </div>
        </div>

        <!-- 版本与变更说明 -->
        <div class="card">
          <h3 class="card-title">版本与变更说明</h3>
          <div class="fgrid">
            <div class="it">
              <div class="lb req">版本号</div>
              <el-input v-model="form.ruleVersion" placeholder="如 v2.2" style="width:180px" />
            </div>
            <div class="it">
              <div class="lb">规则名称</div>
              <el-input v-model="form.ruleName" placeholder="如 现行规则（含评语引用校验）" style="width:100%" />
            </div>
            <div class="it full">
              <div class="lb req">变更说明</div>
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="写明本次调整的原因与影响范围，便于答辩时说明规则演进"
              />
            </div>
          </div>
          <div style="font-size:11.5px;color:var(--text-2);margin-top:8px">
            当前生效版本：<b class="mono">{{ current.ruleVersion || '—' }}</b>
            <span v-if="current.effectTime"> · 生效于 {{ fmtTime(current.effectTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧历史版本 -->
      <div class="card" style="margin-bottom:0">
        <h3 class="card-title">
          <span>历史版本</span>
          <span style="font-size:12px;color:var(--text-2);font-weight:400">共 {{ versions.length }} 条</span>
        </h3>
        <el-table :data="versions" size="small" style="width:100%">
          <el-table-column prop="ruleVersion" label="版本" min-width="72">
            <template #default="{ row }"><span class="mono">{{ row.ruleVersion }}</span></template>
          </el-table-column>
          <el-table-column prop="effectTime" label="生效时间" min-width="120">
            <template #default="{ row }">{{ fmtTime(row.effectTime) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="82">
            <template #default="{ row }">
              <span class="tag" :class="row.status === '生效中' ? 't-green' : 't-gray'">{{ row.status || '已失效' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" label="修改人" min-width="76">
            <template #default="{ row }">{{ row.operatorName || '—' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || row.ruleName || '—' }}</template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无历史版本" :image-size="60" />
          </template>
        </el-table>
        <div class="version-tip">
          历史版本只读：修改规则不会回溯已生成的快照，<b>计算留痕页</b>仍按快照的 ruleVersion 展示当次口径。
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

const saving = ref(false)
const versions = ref([])
const current = reactive({ ruleVersion: '', effectTime: '', ruleName: '' })

// 表单字段与接口字段一一对应（credibility/rule 的 current）
const form = reactive({
  weightDeviation: 25,
  weightComment: 20,
  weightFinishRate: 20,
  weightSimilarity: 20,
  weightConsistency: 15,
  gradeHigh: 85,
  gradeMid: 65,
  gradeLow: 45,
  similarityAlert: 70,
  ruleVersion: '',
  ruleName: '',
  remark: ''
})

const factorFields = [
  { key: 'weightDeviation', label: '评分偏离度', hint: '与教师评分均值的偏差' },
  { key: 'weightComment', label: '评语有效长度', hint: '去套话后的有效字数' },
  { key: 'weightFinishRate', label: '互评完成率', hint: '近 3 次任务按期完成比' },
  { key: 'weightSimilarity', label: '提交相似度', hint: '同作业最高代码相似度' },
  { key: 'weightConsistency', label: '评分一致性', hint: '同提交多份互评标准差' }
]

// 权重校验：合计必须为 100，否则禁用保存（后端也会二次校验）
const weightSum = computed(() =>
  Number(
    (
      Number(form.weightDeviation || 0) +
      Number(form.weightComment || 0) +
      Number(form.weightFinishRate || 0) +
      Number(form.weightSimilarity || 0) +
      Number(form.weightConsistency || 0)
    ).toFixed(1)
  )
)
const weightValid = computed(() => Math.abs(weightSum.value - 100) < 0.01)

// 阈值必须严格递减：高 > 中 > 较低
const thresholdValid = computed(
  () =>
    Number(form.gradeHigh) > Number(form.gradeMid) &&
    Number(form.gradeMid) > Number(form.gradeLow) &&
    Number(form.gradeLow) > 0
)

const canSave = computed(
  () => weightValid.value && thresholdValid.value && String(form.ruleVersion || '').trim().length > 0
)

function nextVersion(v) {
  const m = /^v(\d+)\.(\d+)$/i.exec(String(v || ''))
  if (!m) return 'v1.0'
  return `v${m[1]}.${Number(m[2]) + 1}`
}

async function load() {
  try {
    const d = await api.credibilityRule()
    const c = d?.current || {}
    Object.assign(current, {
      ruleVersion: c.ruleVersion || '',
      effectTime: c.effectTime || '',
      ruleName: c.ruleName || ''
    })
    Object.assign(form, {
      weightDeviation: Number(c.weightDeviation ?? 25),
      weightComment: Number(c.weightComment ?? 20),
      weightFinishRate: Number(c.weightFinishRate ?? 20),
      weightSimilarity: Number(c.weightSimilarity ?? 20),
      weightConsistency: Number(c.weightConsistency ?? 15),
      gradeHigh: Number(c.gradeHigh ?? 85),
      gradeMid: Number(c.gradeMid ?? 65),
      gradeLow: Number(c.gradeLow ?? 45),
      similarityAlert: Number(c.similarityAlert ?? 70),
      // 默认给出下一个版本号，避免与现行版本重名导致保存失败
      ruleVersion: nextVersion(c.ruleVersion),
      ruleName: c.ruleName || '',
      remark: ''
    })
    versions.value = d?.versions || []
  } catch (e) {
    versions.value = []
  }
}

async function save() {
  if (!weightValid.value) {
    ElMessage.warning(`权重合计必须等于 100%，当前为 ${weightSum.value}%`)
    return
  }
  if (!thresholdValid.value) {
    ElMessage.warning('等级阈值必须满足 高可信 > 中可信 > 较低可信 > 0')
    return
  }
  if (!String(form.ruleVersion || '').trim()) {
    ElMessage.warning('请填写规则版本号')
    return
  }
  if (!String(form.remark || '').trim()) {
    ElMessage.warning('请填写变更说明')
    return
  }
  saving.value = true
  try {
    await api.ruleSave({
      ruleVersion: form.ruleVersion.trim(),
      ruleName: form.ruleName.trim() || `规则 ${form.ruleVersion.trim()}`,
      weightDeviation: form.weightDeviation,
      weightComment: form.weightComment,
      weightFinishRate: form.weightFinishRate,
      weightSimilarity: form.weightSimilarity,
      weightConsistency: form.weightConsistency,
      gradeHigh: form.gradeHigh,
      gradeMid: form.gradeMid,
      gradeLow: form.gradeLow,
      similarityAlert: form.similarityAlert,
      remark: form.remark.trim()
    })
    ElMessage.success('规则已保存并生效，历史快照仍按旧版本留痕')
    await load()
  } catch (e) {
    /* 接口层已提示失败原因 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.rule-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 460px;
  gap: 14px;
  align-items: start;
}
/* 在全局 .factor-row 基础上加宽名称列，容纳说明文字 */
.rule-row {
  grid-template-columns: 168px 1fr 62px;
  margin-bottom: 14px;
}
.rule-row .fname {
  line-height: 1.4;
}
.weight-note {
  margin-top: 4px;
  padding: 9px 12px;
  border-left: 3px solid var(--success);
  background: #f4f8f6;
  border-radius: 4px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.8;
}
.weight-note.bad {
  border-left-color: var(--danger);
  background: #fdf2f3;
  color: var(--danger);
}
.threshold-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 34px;
}
.version-tip {
  margin-top: 10px;
  font-size: 11.5px;
  color: var(--text-2);
  line-height: 1.8;
  padding: 8px 10px;
  background: var(--bg);
  border-radius: 4px;
}
@media (max-width: 1100px) {
  .rule-grid {
    grid-template-columns: 1fr;
  }
}
</style>
