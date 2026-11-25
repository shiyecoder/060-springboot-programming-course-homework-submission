<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>发布作业</h1>
        <p class="sub">发布后题目描述不可直接修改，如需更正只能撤回重发；保存草稿不产生提交入口</p>
      </div>
      <div class="acts head-actions">
        <el-button @click="$router.push('/assignment')">返回作业台账</el-button>
      </div>
    </div>

    <div class="split right">
      <!-- 左：作业表单 -->
      <div class="card">
        <div class="card-title">作业基本信息</div>
        <el-form label-width="96px" label-position="right">
          <div class="fgrid">
            <div class="it full">
              <span class="lb req">所属课程</span>
              <el-select v-model="form.courseId" placeholder="请选择开课课程（含教学班）" style="width:100%" @change="onCourseChange">
                <el-option
                  v-for="c in courses"
                  :key="c.id"
                  :label="`${c.courseNo} · ${c.courseName} · ${c.className}`"
                  :value="c.id"
                />
              </el-select>
            </div>
            <div class="it full">
              <span class="lb req">作业标题</span>
              <el-input v-model="form.title" maxlength="60" show-word-limit placeholder="如：第4次作业：文件IO与异常处理" />
            </div>
            <div class="it">
              <span class="lb req">编程语言</span>
              <el-select v-model="form.languageType" placeholder="请选择" style="width:100%">
                <el-option v-for="l in langs" :key="l.value" :label="l.label" :value="l.value" />
              </el-select>
            </div>
            <div class="it">
              <span class="lb req">用例数量</span>
              <el-input-number v-model="form.testCaseCount" :min="1" :max="30" controls-position="right" style="width:100%" />
            </div>
            <div class="it">
              <span class="lb req">截止时间</span>
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="选择截止时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </div>
            <div class="it">
              <span class="lb">迟交截止</span>
              <el-date-picker
                v-model="form.lateDeadline"
                type="datetime"
                placeholder="默认不接收迟交"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </div>
            <div class="it">
              <span class="lb">互评截止</span>
              <el-date-picker
                v-model="form.reviewDeadline"
                type="datetime"
                placeholder="默认截止后 5 天"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width:100%"
              />
            </div>
            <div class="it">
              <span class="lb">互评份数</span>
              <el-select v-model="form.peerReviewCount" style="width:100%">
                <el-option v-for="n in [1, 2, 3, 4, 5]" :key="n" :label="`${n} 份 / 人`" :value="n" />
              </el-select>
            </div>
            <div class="it">
              <span class="lb">成绩权重</span>
              <el-input-number v-model="form.weight" :min="0" :max="100" :precision="2" :step="5" controls-position="right" style="width:100%" />
            </div>
            <div class="it full">
              <span class="lb req">题目描述</span>
              <div style="flex:1;min-width:0">
                <el-input
                  v-model="form.description"
                  type="textarea"
                  :rows="9"
                  maxlength="2000"
                  placeholder="写清输入、输出与评分要求，不少于 30 字"
                />
                <div class="hint" :class="{ bad: form.description.length > 0 && form.description.length < 30 }">
                  已输入 {{ form.description.length }} 字，要求不少于 30 字
                </div>
              </div>
            </div>
          </div>
        </el-form>

        <!-- 底部两键：草稿 / 发布，status 分别传「草稿」「已发布」 -->
        <div class="form-foot">
          <el-button :loading="saving === '草稿'" @click="submit('草稿')">保存草稿</el-button>
          <el-button type="primary" :loading="saving === '已发布'" @click="submit('已发布')">发布并生成提交入口</el-button>
        </div>
      </div>

      <!-- 右：学生端预览，所见即学生门户里看到的卡片 -->
      <div class="preview">
        <div class="card">
          <div class="card-title">学生端预览</div>
          <div class="pv-card">
            <div class="pv-tag">{{ form.languageType || '语言未选' }}</div>
            <div class="pv-title">{{ form.title || '未命名作业' }}</div>
            <div class="pv-course">{{ courseLabel }}</div>
            <div class="pv-desc">{{ form.description || '题目说明将在这里展示给学生，建议写清输入输出格式与评分点。' }}</div>
            <div class="pv-meta">
              <div><span>截止时间</span><b>{{ fmtTime(form.deadline) }}</b></div>
              <div><span>用例数量</span><b>{{ form.testCaseCount }} 条</b></div>
              <div><span>互评份数</span><b>{{ form.peerReviewCount }} 份</b></div>
              <div><span>成绩权重</span><b>{{ Number(form.weight || 0).toFixed(2) }} / 100</b></div>
            </div>
          </div>
        </div>
        <div class="card">
          <div class="card-title">发布前校验</div>
          <div class="checks">
            <div v-for="c in checks" :key="c.text" class="ck" :class="c.ok ? 'ok' : 'no'">
              <span class="dot">{{ c.ok ? '✓' : '!' }}</span>{{ c.text }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const router = useRouter()
const courses = ref([])
const langs = ref([])
const saving = ref('')

const form = reactive({
  courseId: null,
  title: '',
  description: '',
  languageType: 'Java',
  deadline: '',
  lateDeadline: '',
  reviewDeadline: '',
  testCaseCount: 6,
  peerReviewCount: 2,
  weight: 15
})

const courseLabel = computed(() => {
  const c = courses.value.find((x) => x.id === form.courseId)
  return c ? `${c.courseName} · ${c.className}` : '未选择课程'
})

/** 选中课程后自动带出语言，减少手填错误 */
function onCourseChange(id) {
  const c = courses.value.find((x) => x.id === id)
  if (c && c.languageType) form.languageType = c.languageType
}

// 发布前实时校验：与后端 assignmentSave 的校验规则保持一致
const checks = computed(() => [
  { text: form.courseId ? '已选择所属课程' : '尚未选择所属课程', ok: !!form.courseId },
  { text: form.description.length >= 30 ? `题目描述 ${form.description.length} 字（≥30）` : '题目描述不足 30 字', ok: form.description.length >= 30 },
  { text: form.deadline ? '截止时间已设置' : '截止时间未设置', ok: !!form.deadline },
  { text: form.testCaseCount >= 1 && form.testCaseCount <= 30 ? `用例数量 ${form.testCaseCount} 在 1–30 之间` : '用例数量需在 1–30 之间', ok: form.testCaseCount >= 1 && form.testCaseCount <= 30 }
])

function validate() {
  if (!form.courseId) return '请选择所属课程'
  if (!form.title.trim()) return '请填写作业标题'
  if (form.description.trim().length < 30) return '题目描述不少于 30 字'
  if (!form.deadline) return '请设置截止时间'
  if (form.lateDeadline && form.lateDeadline < form.deadline) return '迟交截止不能早于正式截止'
  if (form.reviewDeadline && form.reviewDeadline < form.deadline) return '互评截止不能早于正式截止'
  if (form.testCaseCount < 1 || form.testCaseCount > 30) return '用例数量需在 1–30 之间'
  return ''
}

/** status 传「草稿」时后端不写 publish_time，即为未发布的草稿 */
async function submit(status) {
  const err = validate()
  if (err) return ElMessage.warning(err)
  saving.value = status
  try {
    await api.assignmentSave({ ...form, status })
    ElMessage.success(status === '草稿' ? '已保存为草稿' : '作业已发布，提交入口已生成')
    router.push('/assignment')
  } catch (e) {
    // 后端业务异常由 axios 拦截器统一提示
  } finally {
    saving.value = ''
  }
}

async function loadOptions() {
  try {
    const o = await api.options()
    courses.value = o?.courses || []
    langs.value = o?.languages || []
  } catch (e) {
    courses.value = []
    langs.value = []
  }
}

onMounted(loadOptions)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.fgrid .it { align-items: flex-start; }
/* 表单控件撑满剩余宽度，标签固定 96px，避免下拉/日期选择器溢出栅格 */
:deep(.fgrid .it > .el-select),
:deep(.fgrid .it > .el-date-editor),
:deep(.fgrid .it > .el-input-number) { flex: 1; min-width: 0; }
.hint { font-size: 11.5px; color: #5B6770; margin-top: 4px; }
.hint.bad { color: #B02A37; }
.form-foot {
  display: flex; justify-content: flex-end; gap: 8px;
  margin-top: 16px; padding-top: 14px; border-top: 1px solid #DEE4E9;
}
.preview { display: flex; flex-direction: column; gap: 12px; }
.pv-card { border: 1px solid #DEE4E9; border-radius: 8px; padding: 14px; background: #FCFCFD; }
.pv-tag {
  display: inline-block; font-size: 11px; color: #1E4B3F; background: #E7F0EC;
  border-radius: 3px; padding: 1px 7px; margin-bottom: 8px;
}
.pv-title { font-size: 15px; font-weight: 600; line-height: 1.45; }
.pv-course { font-size: 12px; color: #5B6770; margin-top: 4px; }
.pv-desc {
  margin: 10px 0 12px; font-size: 12.5px; line-height: 1.8; color: #3A4750;
  white-space: pre-wrap; max-height: 168px; overflow: hidden;
}
.pv-meta { border-top: 1px dashed #E8EDEA; padding-top: 8px; }
.pv-meta > div { display: flex; justify-content: space-between; font-size: 12.5px; padding: 3px 0; }
.pv-meta span { color: #5B6770; }
.checks .ck { display: flex; gap: 6px; font-size: 12.5px; line-height: 1.9; color: #5B6770; }
.checks .ck .dot { font-weight: 700; }
.checks .ok .dot { color: #2B7955; }
.checks .no .dot { color: #C2703A; }
</style>
