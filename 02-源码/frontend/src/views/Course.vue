<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>课程管理</h1>
        <p class="sub">同学期同课程可开多个教学班，学生人数由选课名单汇总，页面只读</p>
      </div>
      <div class="acts head-actions">
        <el-button type="primary" @click="openCreate">新增课程</el-button>
      </div>
    </div>

    <!-- 筛选：输入 220px、下拉 200px，全部走后端分页查询 -->
    <div class="filter-bar">
      <el-input
        v-model="query.keyword"
        placeholder="课程编号 / 名称 / 教学班"
        clearable
        style="width:220px"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width:200px" @change="search">
        <el-option v-for="s in courseStatuses" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="query.languageType" placeholder="全部语言" clearable style="width:200px" @change="search">
        <el-option v-for="l in langs" :key="l.value" :label="l.label" :value="l.value" />
      </el-select>
      <el-button @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id" size="default" empty-text="暂无课程">
        <el-table-column label="课程编号">
          <template #default="{ row }"><span class="mono">{{ row.courseNo }}</span></template>
        </el-table-column>
        <el-table-column label="课程名称" min-width="180">
          <template #default="{ row }">
            <div class="course-cell">
              <!-- 封面走后端 /uploads 相对路径，禁止外链 -->
              <el-image :src="row.coverImage" fit="cover" class="cover">
                <template #error><div class="cover-fb">{{ (row.courseName || '课')[0] }}</div></template>
              </el-image>
              <div class="course-txt">
                <div>{{ row.courseName }}</div>
                <div class="muted">{{ row.teacherName || '未指派教师' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期" />
        <el-table-column prop="collegeName" label="开课学院" />
        <el-table-column prop="className" label="教学班" />
        <el-table-column prop="languageType" label="语言" />
        <el-table-column prop="studentCount" label="学生数" align="right" />
        <el-table-column prop="assignmentCount" label="作业数" align="right" />
        <el-table-column label="状态">
          <template #default="{ row }">
            <span class="tag" :class="statusCls(row.status)">{{ row.status || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间">
          <template #default="{ row }">{{ fmtDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="$router.push({ path: '/assignment', query: { courseId: row.id } })">
              作业
            </el-button>
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

    <!-- 新增 / 编辑抽屉：共用一套表单，id 为空即新增 -->
    <el-drawer v-model="drawer" :title="form.id ? '编辑课程' : '新增课程'" size="480px" destroy-on-close>
      <el-form :model="form" label-width="88px" label-position="right">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.courseName" maxlength="50" placeholder="如：Java程序设计" />
        </el-form-item>
        <el-form-item label="课程编号">
          <el-input v-model="form.courseNo" placeholder="留空由系统生成" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="学期">
          <el-select v-model="form.semester" placeholder="请选择" style="width:100%">
            <el-option label="2026学年第一学期" value="2026学年第一学期" />
            <el-option label="2025学年第二学期" value="2025学年第二学期" />
          </el-select>
        </el-form-item>
        <el-form-item label="开课学院">
          <el-input v-model="form.collegeName" placeholder="如：计算机学院" />
        </el-form-item>
        <el-form-item label="教学班">
          <el-select v-model="form.className" placeholder="请选择" filterable allow-create style="width:100%">
            <el-option v-for="c in classes" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="编程语言">
          <el-select v-model="form.languageType" placeholder="请选择" style="width:100%">
            <el-option v-for="l in langs" :key="l.value" :label="l.label" :value="l.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="任课教师">
          <el-select v-model="form.teacherId" placeholder="默认本人" clearable style="width:100%">
            <el-option v-for="t in teachers" :key="t.id" :label="`${t.realName}（${t.title || '教师'}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学生人数">
          <el-input-number v-model="form.studentCount" :min="0" :max="300" controls-position="right" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-edit">
            <el-image v-if="form.coverImage" :src="form.coverImage" fit="cover" class="cover-lg" />
            <el-upload :show-file-list="false" :http-request="doUpload" accept="image/*">
              <el-button size="small">{{ form.coverImage ? '更换封面' : '上传封面' }}</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item v-if="form.id" label="课程状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option v-for="s in courseStatuses" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </AdminLayout>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { fmtDate } from '@/utils/format'

const courseStatuses = ['进行中', '已结课', '已归档']
const langs = ref([])
const teachers = ref([])
const classes = ref([])

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '', status: '', languageType: '' })

const drawer = ref(false)
const saving = ref(false)
const form = reactive(emptyForm())

function emptyForm() {
  return {
    id: null,
    courseNo: '',
    courseName: '',
    semester: '2026学年第一学期',
    collegeName: '计算机学院',
    className: '',
    languageType: 'Java',
    teacherId: null,
    studentCount: 0,
    assignmentCount: 0,
    coverImage: '',
    status: '进行中'
  }
}

/** 状态色：进行中蓝、已结课/归档灰绿，均带中文，不单靠颜色 */
function statusCls(s) {
  if (s === '进行中') return 't-blue'
  if (s === '已结课') return 't-green'
  return 't-gray'
}

async function load() {
  loading.value = true
  try {
    const d = await api.coursePage({ ...query })
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
  query.page = 1
  load()
}
function reset() {
  query.keyword = ''
  query.status = ''
  query.languageType = ''
  search()
}
function onSizeChange() {
  query.page = 1
  load()
}

function openCreate() {
  Object.assign(form, emptyForm())
  drawer.value = true
}
function openEdit(row) {
  Object.assign(form, emptyForm(), {
    id: row.id,
    courseNo: row.courseNo || '',
    courseName: row.courseName || '',
    semester: row.semester || '2026学年第一学期',
    collegeName: row.collegeName || '',
    className: row.className || '',
    languageType: row.languageType || '',
    teacherId: row.teacherId ?? null,
    studentCount: row.studentCount ?? 0,
    assignmentCount: row.assignmentCount ?? 0,
    coverImage: row.coverImage || '',
    status: row.status || '进行中'
  })
  drawer.value = true
}

/** 封面先上传拿到 /uploads 相对路径，再随课程表单一起提交 */
async function doUpload(opt) {
  try {
    const d = await api.upload(opt.file)
    form.coverImage = d?.url || d?.path || ''
    ElMessage.success('封面上传成功')
  } catch (e) {
    // 拦截器已提示，这里只保证不抛未捕获异常
  }
}

async function save() {
  if (!form.courseName) return ElMessage.warning('请填写课程名称')
  saving.value = true
  try {
    await api.courseSave({ ...form })
    ElMessage.success(form.id ? '课程已更新' : '课程已创建')
    drawer.value = false
    load()
  } catch (e) {
    // 后端校验信息由拦截器提示
  } finally {
    saving.value = false
  }
}

async function loadOptions() {
  try {
    const o = await api.options()
    langs.value = o?.languages || []
    teachers.value = o?.teachers || []
    classes.value = (o?.classes || []).map((c) => c.class_name).filter(Boolean)
  } catch (e) {
    langs.value = []
    teachers.value = []
    classes.value = []
  }
}

onMounted(() => {
  loadOptions()
  load()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.course-cell { display: flex; align-items: center; gap: 10px; }
.cover { width: 46px; height: 34px; border-radius: 4px; flex: none; background: #eef1f0; }
.cover-fb { width: 46px; height: 34px; display: grid; place-items: center; background: #e7efeb; color: #1E4B3F; font-weight: 600; }
.course-txt { min-width: 0; }
.muted { color: #5B6770; font-size: 12px; margin-top: 2px; }
.cover-edit { display: flex; align-items: center; gap: 10px; }
.cover-lg { width: 100px; height: 64px; border-radius: 6px; background: #eef1f0; }
</style>
