<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>相似度复核 · {{ record?.recordNo || '—' }}</h1>
        <p class="sub">
          {{ record?.studentA || '—' }} / {{ record?.studentB || '—' }} ·
          相似度 {{ rateText }} · 命中「{{ record?.matchType || '—' }}」· 规则 {{ record?.checkRuleVersion || '—' }}
        </p>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !record" description="未找到查重记录，请从查重记录页进入复核" />

    <template v-else>
      <!-- 顶部：总分 + 三项因子 + 证据片段 -->
      <div class="top-grid">
        <div class="card" style="margin-bottom:0">
          <div class="card-title" style="margin-bottom:10px">
            <span>相似度构成</span>
            <span class="tag" :class="Number(record?.similarityRate) > 70 ? 't-red' : 't-amber'">
              {{ Number(record?.similarityRate) > 70 ? '超告警线' : '告警线内' }}
            </span>
          </div>
          <div class="score-line">
            <span style="font-size:13px;color:var(--text-2)">加权相似度</span>
            <b :style="{ color: Number(record?.similarityRate) > 70 ? 'var(--danger)' : 'var(--gold)' }">
              {{ rateText }}
            </b>
          </div>

          <!-- 相似度三因子：结构 0.5 + 标识符 0.3 + 注释 0.2 -->
          <div class="factor-row">
            <span class="fname">结构相似度 ×0.5</span>
            <span class="ftrack">
              <i class="fbar" :style="{ width: pct(record?.structureRate), background: barColor(record?.structureRate) }"></i>
            </span>
            <span class="fval">{{ num(record?.structureRate) }}</span>
          </div>
          <div class="factor-row">
            <span class="fname">标识符相似度 ×0.3</span>
            <span class="ftrack">
              <i class="fbar" :style="{ width: pct(record?.identifierRate), background: barColor(record?.identifierRate) }"></i>
            </span>
            <span class="fval">{{ num(record?.identifierRate) }}</span>
          </div>
          <div class="factor-row">
            <span class="fname">注释相似度 ×0.2</span>
            <span class="ftrack">
              <i class="fbar" :style="{ width: pct(record?.commentRate), background: barColor(record?.commentRate) }"></i>
            </span>
            <span class="fval">{{ num(record?.commentRate) }}</span>
          </div>

          <div class="note" style="margin-top:10px">
            结构 0.5 + 标识符 0.3 + 注释 0.2 = 加权相似度 {{ rateText }}；
            归一化后连续相同的行才计入命中，变量重命名不影响结构判定。
          </div>
        </div>

        <div class="card" style="margin-bottom:0">
          <div class="card-title" style="margin-bottom:10px">
            <span>证据片段</span>
            <span class="mono" style="font-size:11px;font-weight:400">{{ record?.recordNo }}</span>
          </div>
          <div class="doc-block" style="max-height:132px;font-size:12px">
            {{ record?.evidenceSnippet || '暂无证据片段' }}
          </div>
          <div class="card-title" style="margin:12px 0 8px">复核要求</div>
          <div class="note">
            确认抄袭需两名教师签字并通知学生；误报需填写不少于 5 字的理由；
            判断不了时选「需面谈」，学生可在 3 天内提交说明。复核结论会写回查重记录并留痕。
          </div>
        </div>
      </div>

      <!-- 左右并排代码对照：相同行加 .hit -->
      <div class="split wide" style="margin-bottom:14px">
        <div class="card" style="margin-bottom:0">
          <div class="col-head">
            <b>{{ record?.studentA || '提交 A' }}</b>
            <span class="mono">{{ record?.noA }}</span>
            <span class="tag t-gray">示例代码</span>
          </div>
          <div class="code-box">
            <div v-for="(l, i) in codeA" :key="i" class="ln" :class="{ hit: l.hit }">
              <i>{{ i + 1 }}</i><span>{{ l.t }}</span>
            </div>
          </div>
        </div>
        <div class="card" style="margin-bottom:0">
          <div class="col-head">
            <b>{{ record?.studentB || '提交 B' }}</b>
            <span class="mono">{{ record?.noB }}</span>
            <span class="tag t-gray">示例代码</span>
          </div>
          <div class="code-box">
            <div v-for="(l, i) in codeB" :key="i" class="ln" :class="{ hit: l.hit }">
              <i>{{ i + 1 }}</i><span>{{ l.t }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 复核表单 -->
      <div class="card">
        <div class="card-title" style="margin-bottom:12px">
          <span>复核结论</span>
          <span style="font-size:12px;font-weight:400;color:var(--text-2)">
            当前结论：{{ record?.reviewResult || '待复核' }}
          </span>
        </div>

        <div class="fgrid">
          <div class="it full">
            <span class="lb req">复核结论</span>
            <el-radio-group v-model="form.reviewResult">
              <el-radio value="确认抄袭">确认抄袭</el-radio>
              <el-radio value="误报">误报</el-radio>
              <el-radio value="需面谈">需面谈</el-radio>
            </el-radio-group>
          </div>
          <div class="it full">
            <span class="lb req">复核理由</span>
            <div style="flex:1">
              <el-input
                v-model="form.reviewRemark"
                type="textarea"
                :rows="3"
                maxlength="300"
                show-word-limit
                placeholder="不少于 5 字，说明判定依据（命中行数、变量重命名、注释雷同等）"
              />
              <div style="font-size:11px;margin-top:4px" :style="{ color: remarkLen >= 5 ? 'var(--success)' : 'var(--danger)' }">
                已填 {{ remarkLen }} 字 / 最少 5 字
              </div>
            </div>
          </div>
          <!-- 确认抄袭必须双教师签字 -->
          <div v-if="form.reviewResult === '确认抄袭'" class="it full">
            <span class="lb req">第二签字教师</span>
            <el-select v-model="form.secondReviewerId" placeholder="请选择第二签字教师" style="width:200px">
              <el-option
                v-for="t in teachers"
                :key="t.id"
                :label="`${t.realName}（${t.title}）`"
                :value="t.id"
              />
            </el-select>
          </div>
        </div>

        <div style="margin-top:14px;display:flex;align-items:center;gap:12px">
          <el-button type="primary" :loading="submitting" @click="submit">提交复核</el-button>
          <el-button @click="back">返回查重记录</el-button>
          <span style="font-size:12px;color:var(--text-2)">提交后写回记录并留痕，不可重复提交同一结论</span>
        </div>
      </div>
    </template>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const record = ref(null)
const teachers = ref([])

const form = reactive({
  reviewResult: '',
  reviewRemark: '',
  secondReviewerId: null
})

/** 代码对照用的示例片段（同一份作业的两份命中的提交） */
const codeA = [
  { t: 'public class TicketSeller {', hit: true },
  { t: '    private int ticket = 100;', hit: true },
  { t: '', hit: false },
  { t: '    public void sell() {', hit: true },
  { t: '        synchronized (this) {', hit: true },
  { t: '            if (ticket > 0) {', hit: true },
  { t: '                ticket--;', hit: false },
  { t: '                System.out.println(Thread.currentThread().getName()', hit: false },
  { t: '                    + " 卖出 1 张，剩余 " + ticket);', hit: false },
  { t: '            }', hit: false },
  { t: '        }', hit: false },
  { t: '    }', hit: false },
  { t: '}', hit: false }
]

const codeB = [
  { t: 'public class TicketSeller {', hit: true },
  { t: '    private int ticket = 100;', hit: true },
  { t: '', hit: false },
  { t: '    public void sell() {', hit: true },
  { t: '        synchronized (this) {', hit: true },
  { t: '            if (ticket > 0) {', hit: true },
  { t: '                ticket = ticket - 1;', hit: false },
  { t: '                System.out.println(Thread.currentThread().getName()', hit: false },
  { t: '                    + " sold one, left " + ticket);', hit: false },
  { t: '            }', hit: false },
  { t: '        }', hit: false },
  { t: '    }', hit: false },
  { t: '}', hit: false }
]

const rateText = computed(() => `${Number(record.value?.similarityRate ?? 0).toFixed(2)}%`)
const remarkLen = computed(() => (form.reviewRemark || '').trim().length)

function num(v) {
  const n = Number(v)
  return isNaN(n) ? '—' : n.toFixed(1)
}

function pct(v) {
  const n = Number(v)
  if (isNaN(n)) return '0%'
  return `${Math.min(100, Math.max(0, n))}%`
}

function barColor(v) {
  const n = Number(v)
  if (n > 70) return '#B02A37'
  if (n >= 50) return '#C2703A'
  return '#1E4B3F'
}

async function load() {
  loading.value = true
  try {
    // 无详情接口：取最近 100 条记录，按 query.id 命中，未命中时取第一条
    const d = await api.similarityPage({ page: 1, size: 100 })
    const list = d?.records || []
    const id = route.query.id
    const hit = id ? list.find((r) => String(r.id) === String(id)) : null
    record.value = hit || list[0] || null
    if (record.value) {
      form.reviewResult = ['确认抄袭', '误报', '需面谈'].includes(record.value.reviewResult)
        ? record.value.reviewResult
        : ''
      form.reviewRemark = record.value.reviewRemark || ''
      form.secondReviewerId = record.value.secondReviewerId || null
    }
  } catch (e) {
    record.value = null
  } finally {
    loading.value = false
  }
}

async function loadTeachers() {
  try {
    const d = await api.options()
    teachers.value = d?.teachers || []
  } catch (e) {
    teachers.value = []
  }
}

async function submit() {
  if (!record.value) return
  if (!form.reviewResult) {
    ElMessage.warning('请选择复核结论')
    return
  }
  if (remarkLen.value < 5) {
    ElMessage.warning('复核理由不少于 5 字')
    return
  }
  if (form.reviewResult === '确认抄袭' && !form.secondReviewerId) {
    ElMessage.warning('确认抄袭需选择第二签字教师')
    return
  }
  submitting.value = true
  try {
    await api.similarityReview(record.value.id, {
      reviewResult: form.reviewResult,
      reviewRemark: form.reviewRemark.trim(),
      secondReviewerId: form.reviewResult === '确认抄袭' ? form.secondReviewerId : null
    })
    ElMessage.success(`复核已提交：${record.value.recordNo} 判定为「${form.reviewResult}」`)
    await load()
  } catch (e) {
    // 失败原因由拦截器提示
  } finally {
    submitting.value = false
  }
}

function back() {
  router.push('/similarity')
}

onMounted(async () => {
  await loadTeachers()
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
.top-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 14px;
  margin-bottom: 14px;
  align-items: start;
}
.score-line {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}
.score-line b {
  font-size: 26px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.col-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
}
.col-head b {
  font-weight: 600;
}
.col-head .mono {
  font-size: 11px;
  color: var(--text-2);
}
/* 因子条内 i 为 inline 元素，补 block 才能撑满轨道高度 */
.factor-row .ftrack {
  display: block;
}
.factor-row .fbar {
  display: block;
}
.note {
  background: #f2f5f4;
  border: 1px solid var(--line);
  border-left: 3px solid var(--primary);
  border-radius: 4px;
  padding: 9px 12px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.85;
}
@media (max-width: 1100px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
}
</style>
