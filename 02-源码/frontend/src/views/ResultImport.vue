<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>批量导入运行结果</h1>
        <p class="sub">按学号批量登记编译结果与用例通过数 · 逐行校验并返回失败原因，不允许整批静默失败</p>
      </div>
      <div class="acts head-actions">
        <el-button @click="downloadTpl">下载导入模板</el-button>
        <el-button @click="$router.push('/result/entry')">返回结果登记</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="assignmentId" placeholder="选择目标作业" filterable style="width:200px" @change="onAssignmentChange">
        <el-option v-for="item in assignments" :key="item.id" :label="`${item.assignmentNo} · ${item.title}`" :value="item.id" />
      </el-select>
      <span class="tip">用例上限 {{ caseLimit }} 条 · 通过数超过上限的行会被拒绝</span>
      <div class="spacer"></div>
      <el-tag type="warning" effect="plain" size="small">演示数据</el-tag>
    </div>

    <div class="split">
      <!-- 左：上传区 -->
      <div class="card">
        <div class="card-title">上传结果文件</div>
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="false"
          accept=".csv,.xlsx,.xls"
          :on-change="onFile"
        >
          <div class="up-box">
            <div class="up-ic">↑</div>
            <div class="up-t">拖拽文件到此处，或<em>点击选择</em></div>
            <div class="up-s">仅支持 csv / xlsx，单文件不超过 20 MB</div>
          </div>
        </el-upload>

        <div v-if="fileName" class="file-info">
          <div class="fi-name">{{ fileName }}</div>
          <div class="fi-meta">大小 {{ fileSize }} · 已校验表头：学号 / 姓名 / 编译结果 / 通过数 / 错误摘要</div>
          <el-button link type="primary" @click="clearFile">重新选择</el-button>
        </div>

        <el-alert
          v-if="!fileName"
          type="info"
          :closable="false"
          show-icon
          title="未选择文件时使用内置示例数据预览"
          description="页面按示例数据演示解析与逐行校验流程，选择真实文件后仅替换文件名，内容仍为演示数据。"
          style="margin-top:12px"
        />

        <div class="foot">
          <div class="tip">校验通过 {{ validCount }} 行 / 共 {{ demoRows.length }} 行，明细见右侧预览表</div>
        </div>
      </div>

      <!-- 右：导入说明 + 预览结果 -->
      <div class="right">
        <div class="card">
          <div class="card-title">导入说明</div>
          <ol class="rules">
            <li>表头必须为「学号 / 姓名 / 编译结果 / 通过数 / 错误摘要」，顺序不限。</li>
            <li>通过数不得超过作业用例总数（当前 {{ caseLimit }} 条），超过整行拒绝。</li>
            <li>编译失败时通过数一律按 0 落库，且错误摘要必填。</li>
            <li>学号必须在本课程选课名单内，否则该行标记失败并返回原因。</li>
            <li>同一学号重复出现时以最后一行为准，导入结果逐行回显。</li>
          </ol>
        </div>

        <div class="card table-card" style="padding:8px 14px 14px">
          <div class="card-title" style="margin:4px 0 10px">
            <span>导入结果预览（前 10 行）</span>
            <span class="cnt">
              共 {{ demoRows.length }} 行 ·
              <span class="tag t-green">可导入 {{ validCount }}</span>
              <span class="tag t-red" style="margin-left:6px">校验失败 {{ demoRows.length - validCount }}</span>
            </span>
          </div>
          <el-table :data="previewRows" row-key="no" empty-text="暂无可预览数据">
            <el-table-column label="学号">
              <template #default="{ row }"><span class="mono">{{ row.no }}</span></template>
            </el-table-column>
            <el-table-column prop="name" label="姓名" />
            <el-table-column label="编译结果">
              <template #default="{ row }">
                <span class="tag" :class="compileCls(row.compileResult)">{{ row.compileResult || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="通过数" align="right">
              <template #default="{ row }">{{ row.passCount ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="错误摘要" min-width="200">
              <template #default="{ row }">
                <span :class="row.errorSummary ? '' : 'muted'">{{ row.errorSummary || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="校验 / 导入结果" min-width="200">
              <template #default="{ row }">
                <span v-if="row.imported" class="tag t-green">已导入</span>
                <span v-else-if="row.reason" class="tag t-red">{{ row.reason }}</span>
                <span v-else class="tag t-blue">待导入</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="tip" style="margin-top:8px">
            预览仅展示前 10 行，实际导入按全部 {{ demoRows.length }} 行执行；数据为页面内置演示样例。
          </div>
        </div>
      </div>
    </div>

    <!-- 页面底部操作条：开始导入按结果分组批量提交（resultEntry 的 ids 批量语义） -->
    <div class="import-foot">
      <div class="tip">
        已解析 {{ demoRows.length }} 行 · 校验通过 {{ validCount }} 行 · 校验失败 {{ demoRows.length - validCount }} 行
      </div>
      <div class="acts">
        <el-button @click="$router.push('/result/entry')">改为逐条登记</el-button>
        <el-button type="primary" :loading="importing" :disabled="!assignmentId || !validCount" @click="startImport">
          开始导入{{ validCount ? `（${validCount} 条）` : '' }}
        </el-button>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'

const COMPILE = ['通过', '编译失败', '未运行']

const assignments = ref([])
const assignmentId = ref(null)
const caseLimit = ref(6)

const fileName = ref('')
const fileSize = ref('')
const importing = ref(false)

/** 内置演示样例：覆盖正常、超上限、编译失败缺摘要、学号不存在、字典非法 5 种情况 */
const demoRows = reactive([
  { no: '2023011001', name: '王思远', compileResult: '通过', passCount: 6, errorSummary: '', imported: false, reason: '' },
  { no: '2023011002', name: '李佳怡', compileResult: '通过', passCount: 5, errorSummary: '', imported: false, reason: '' },
  { no: '2023011003', name: '张浩然', compileResult: '通过', passCount: 4, errorSummary: '', imported: false, reason: '' },
  { no: '2023011004', name: '刘子涵', compileResult: '编译失败', passCount: 0, errorSummary: 'error: cannot find symbol — 变量 errors 未声明（第 22 行）', imported: false, reason: '' },
  { no: '2023011005', name: '陈思远', compileResult: '通过', passCount: 6, errorSummary: '', imported: false, reason: '' },
  { no: '2023011006', name: '赵一鸣', compileResult: '通过', passCount: 6, errorSummary: '', imported: false, reason: '' },
  { no: '2023011007', name: '孙悦', compileResult: '编译失败', passCount: 0, errorSummary: '缺少 import java.util.List', imported: false, reason: '' },
  { no: '2023011008', name: '周子豪', compileResult: '通过', passCount: 3, errorSummary: '', imported: false, reason: '' },
  { no: '2023011009', name: '陆思远', compileResult: '通过', passCount: 2, errorSummary: '', imported: false, reason: '' },
  { no: '2023011010', name: '夏若涵', compileResult: '通过', passCount: 6, errorSummary: '', imported: false, reason: '' },
  { no: '2023011011', name: '郑凯', compileResult: '通过', passCount: 7, errorSummary: '', imported: false, reason: '' },
  { no: '2023099999', name: '不存在', compileResult: '通过', passCount: 0, errorSummary: '', imported: false, reason: '' }
])

const previewRows = computed(() => demoRows.slice(0, 10))

function compileCls(s) {
  return { 通过: 't-green', 编译失败: 't-red' }[s] || 't-gray'
}

/** 逐行校验：编译结果字典、通过数上限、编译失败必填摘要；学号是否在名单需等导入时比对 */
function validate(row) {
  if (!COMPILE.includes(row.compileResult)) return `编译结果「${row.compileResult}」不在字典内`
  if (Number(row.passCount) > caseLimit.value) return `通过数 ${row.passCount} 超过作业上限 ${caseLimit.value}`
  if (row.compileResult === '编译失败' && !String(row.errorSummary || '').trim()) return '编译失败缺少错误摘要'
  return ''
}

const validCount = computed(() => demoRows.filter((r) => !validate(r)).length)

function onFile(file) {
  fileName.value = file?.name || ''
  const kb = (file?.size || 0) / 1024
  fileSize.value = kb > 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb.toFixed(1)} KB`
  ElMessage.info('演示环境未接入解析器，预览使用内置示例数据')
}
function clearFile() {
  fileName.value = ''
  fileSize.value = ''
}

function downloadTpl() {
  // 演示环境不生成真实文件，仅提示模板表头
  ElMessage.success('模板表头：学号 / 姓名 / 编译结果 / 通过数 / 错误摘要')
}

/**
 * 导入流程：
 * 1. 前端先按行校验（字典、上限、摘要）；
 * 2. 拉取该作业全部提交，用学号匹配 submissionId；
 * 3. 因批量接口对同一组 ids 写同一组结果，按「编译结果+通过数+摘要」分组，逐组调用 resultEntry；
 * 4. 未匹配到学号的行直接标记失败，不参与提交。
 */
async function startImport() {
  if (!assignmentId.value) return ElMessage.warning('请先选择目标作业')
  importing.value = true
  demoRows.forEach((r) => { r.imported = false; r.reason = validate(r) })
  try {
    const page = await api.submissionPage({ page: 1, size: 100, assignmentId: assignmentId.value })
    const subs = page?.records || []
    const idMap = new Map()
    subs.forEach((s) => { if (s.studentNo) idMap.set(String(s.studentNo), s.id) })

    const groups = new Map()
    demoRows.forEach((r) => {
      if (r.reason) return
      const sid = idMap.get(String(r.no))
      if (!sid) {
        r.reason = '学号不在本课程选课名单'
        return
      }
      const key = `${r.compileResult}|${r.passCount}|${r.errorSummary}`
      if (!groups.has(key)) groups.set(key, { rows: [], ids: [] })
      const g = groups.get(key)
      g.rows.push(r)
      g.ids.push(sid)
    })

    let ok = 0
    for (const g of groups.values()) {
      try {
        await api.resultEntry({
          ids: g.ids,
          compileResult: g.rows[0].compileResult,
          passCount: g.rows[0].compileResult === '编译失败' ? 0 : Number(g.rows[0].passCount),
          errorSummary: g.rows[0].errorSummary
        })
        g.rows.forEach((r) => { r.imported = true; r.reason = '' })
        ok += g.rows.length
      } catch (e) {
        g.rows.forEach((r) => { r.reason = '写入失败，请重试' })
      }
    }
    const fail = demoRows.length - ok
    ElMessage.success(`导入完成：成功 ${ok} 条，失败 ${fail} 条`)
  } catch (e) {
    ElMessage.error('导入失败，请检查作业与文件后重试')
  } finally {
    importing.value = false
  }
}

function onAssignmentChange(id) {
  const hit = assignments.value.find((x) => x.id === id)
  const n = Number(hit?.testCaseCount || 0)
  caseLimit.value = n > 0 ? n : 6
  demoRows.forEach((r) => { r.reason = ''; r.imported = false })
}

async function loadAssignments() {
  try {
    const d = await api.assignmentPage({ page: 1, size: 100 })
    assignments.value = d?.records || []
    const inProgress = assignments.value.find((x) => ['提交中', '互评中', '评分中'].includes(x.status))
    assignmentId.value = inProgress?.id || assignments.value[0]?.id || null
    if (assignmentId.value) onAssignmentChange(assignmentId.value)
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
.muted { color: #5B6770; }
.right { display: flex; flex-direction: column; gap: 12px; }
.up-box { padding: 18px 10px; text-align: center; }
.up-ic { font-size: 22px; color: #3D7A66; line-height: 1; }
.up-t { font-size: 13px; margin-top: 8px; color: #1F2933; }
.up-t em { font-style: normal; color: #1E4B3F; font-weight: 600; }
.up-s { font-size: 11.5px; color: #5B6770; margin-top: 4px; }
.file-info { margin-top: 10px; padding: 10px 12px; background: #F7FAF8; border: 1px solid #DEE4E9; border-radius: 6px; }
.fi-name { font-size: 13px; font-weight: 600; word-break: break-all; }
.fi-meta { font-size: 11.5px; color: #5B6770; margin: 4px 0 2px; line-height: 1.7; }
.foot { margin-top: 14px; padding-top: 12px; border-top: 1px solid #DEE4E9; }
.import-foot {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  margin-top: 4px; padding: 12px 16px; background: #fff;
  border: 1px solid #DEE4E9; border-radius: 8px;
}
.import-foot .acts { display: flex; gap: 8px; }
.rules { margin: 0; padding-left: 18px; font-size: 12.5px; color: #5B6770; line-height: 2; }
.cnt { font-size: 12px; font-weight: 400; color: #5B6770; display: flex; align-items: center; }
</style>
