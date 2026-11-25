<template>
  <PortalLayout>
    <div class="portal">
      <!-- Hero：问候 + 学生信息 + 3 个统计，保证 1440x900 首屏可见 -->
      <div class="portal-hero">
        <div class="hero-inner">
          <div>
            <h1 class="hero-title">{{ greeting }}，{{ student.realName || student.username || '同学' }}</h1>
            <p class="hero-sub">
              {{ student.className || '—' }} · {{ student.studentNo || '—' }} · 互评信用分
              <b>{{ creditText }}</b>
            </p>
          </div>
          <el-button class="hero-btn" @click="goTodo">去提交作业</el-button>
        </div>

        <div class="hero-kpis">
          <div class="hero-kpi">
            <div class="hk-l">待提交</div>
            <div class="hk-v">{{ stats.pendingSubmit }}<small>项</small></div>
          </div>
          <div class="hero-kpi">
            <div class="hk-l">待互评</div>
            <div class="hk-v">{{ stats.pendingPeer }}<small>项</small></div>
          </div>
          <div class="hero-kpi">
            <div class="hk-l">已发布成绩</div>
            <div class="hk-v">{{ stats.published }}<small>条</small></div>
          </div>
        </div>
      </div>

      <!-- 我的课程：首屏第二块 -->
      <div class="sec-head">
        <h3>我的课程</h3>
        <span>共 {{ courses.length }} 门 · 按班级课表自动关联</span>
      </div>
      <div v-if="courses.length" class="course-grid">
        <div v-for="c in courses" :key="c.id" class="course-card">
          <img :src="c.coverImage || '/uploads/seed/course/01.jpg'" :alt="c.courseName" />
          <div class="course-body">
            <div class="course-name">{{ c.courseName }}</div>
            <div class="course-meta">
              <span>{{ c.teacherName || '—' }}</span>
              <span class="tag t-blue">{{ c.languageType || '—' }}</span>
            </div>
            <div class="course-sem">{{ c.semester || '本学期' }}</div>
          </div>
        </div>
      </div>
      <div v-else class="card" style="padding:8px">
        <el-empty description="本学期暂无关联课程" :image-size="56" />
      </div>

      <div class="portal-grid">
        <div>
          <div class="card">
            <h3 class="card-title">
              <span>待完成作业</span>
              <span class="more" @click="router.push('/my/submissions')">查看全部 →</span>
            </h3>
            <div v-if="todos.length" class="todo-list">
              <div v-for="a in todos" :key="a.id" class="todo-item">
                <div class="todo-main">
                  <div class="todo-title">{{ a.title }}</div>
                  <div class="todo-meta">
                    {{ a.courseName || '—' }} · 截止 {{ fmtTime(a.deadline) }}
                  </div>
                </div>
                <span class="tag" :class="a.submitted ? 't-green' : 't-amber'">
                  {{ a.submitted ? '已提交' : '未提交' }}
                </span>
                <el-button size="small" @click="router.push(`/my/submit/${a.id}`)">
                  {{ a.submitted ? '查看/重交' : '去提交' }}
                </el-button>
              </div>
            </div>
            <el-empty v-else description="暂无待完成作业" :image-size="56" />
          </div>

          <div class="card">
            <h3 class="card-title">
              <span>已发布成绩</span>
              <span class="more" @click="router.push('/my/submissions')">成绩明细 →</span>
            </h3>
            <div v-if="scores.length" class="score-list">
              <div v-for="s in scores" :key="s.id" class="score-item">
                <div class="score-main">
                  <div class="score-title">{{ s.assignmentTitle || '—' }}</div>
                  <div class="score-meta">{{ s.courseName || '—' }} · 班级排名 {{ s.classRank ?? '—' }}</div>
                </div>
                <span class="tag" :class="gradeTag(s.credibilityGrade)">{{ s.credibilityGrade || '未评级' }}</span>
                <b class="score-num">{{ scoreText(s.finalScore) }}</b>
              </div>
            </div>
            <el-empty v-else description="成绩尚未发布" :image-size="56" />
          </div>
        </div>

        <div>
          <div class="card">
            <h3 class="card-title">
              <span>待互评任务</span>
              <span class="tag t-amber">{{ peers.length }} 份待评</span>
            </h3>
            <div v-if="peers.length" class="peer-list">
              <div v-for="p in peers" :key="p.id" class="peer-item">
                <div>
                  <div class="peer-title">{{ p.assignmentTitle || '—' }}</div>
                  <div class="peer-meta">截止 {{ fmtTime(p.deadline) }}</div>
                </div>
                <el-button size="small" @click="router.push('/peer/desk')">去互评</el-button>
              </div>
            </div>
            <el-empty v-else description="暂无待互评任务" :image-size="56" />
          </div>

          <div class="card" style="margin-bottom:0">
            <h3 class="card-title">通知公告</h3>
            <div v-if="notices.length" class="notice-list">
              <div v-for="n in notices" :key="n.id" class="notice-item">
                <span class="tag" :class="noticeTag(n.noticeType)">{{ n.noticeType || '通知' }}</span>
                <span class="notice-title">{{ n.title }}</span>
                <span class="notice-time">{{ fmtDate(n.publishTime) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无通知" :image-size="56" />
          </div>
        </div>
      </div>
    </div>
  </PortalLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import PortalLayout from '@/layouts/PortalLayout.vue'
import { api } from '@/api'
import { fmtDate, fmtTime } from '@/utils/format'
import { store } from '@/store/user'

const router = useRouter()

const student = reactive({ ...store.user })
const todos = ref([])
const peers = ref([])
const scores = ref([])
const notices = ref([])
const courses = ref([])

const stats = reactive({ pendingSubmit: 0, pendingPeer: 0, published: 0 })

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '早上好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const creditText = computed(() => {
  const n = Number(student.creditScore)
  return Number.isFinite(n) ? n.toFixed(1) : '—'
})

function scoreText(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(1) : '—'
}

function gradeTag(g) {
  if (g === '高可信') return 't-green'
  if (g === '中可信') return 't-blue'
  if (g === '较低可信') return 't-amber'
  if (g === '低可信') return 't-red'
  return 't-gray'
}

function noticeTag(t) {
  if (t === '规则变更') return 't-amber'
  if (t === '成绩管理') return 't-green'
  if (t === '学术规范') return 't-red'
  return 't-blue'
}

function goTodo() {
  const first = todos.value.find((a) => !a.submitted) || todos.value[0]
  if (first) router.push(`/my/submit/${first.id}`)
  else router.push('/my/submissions')
}

async function load() {
  try {
    const d = await api.portalHome()
    if (d?.student) Object.assign(student, d.student)
    todos.value = d?.todoAssignments || []
    peers.value = d?.myPeerTasks || []
    scores.value = d?.myScores || []
    notices.value = d?.notices || []
    courses.value = d?.myCourses || []
    stats.pendingSubmit = todos.value.filter((a) => !a.submitted).length
    stats.pendingPeer = peers.value.length
    stats.published = scores.value.length
  } catch (e) {
    /* 接口层已提示，页面保留空态 */
  }
}

onMounted(load)
</script>

<style scoped>
.portal {
  min-height: 0;
}
.hero-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  position: relative;
  z-index: 1;
}
.hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.hero-sub {
  margin: 7px 0 0;
  font-size: 12.5px;
  color: #cfdcd6;
}
.hero-sub b {
  color: #fff;
  font-variant-numeric: tabular-nums;
}
.hero-btn {
  background: #fff;
  border-color: #fff;
  color: var(--primary);
  font-weight: 500;
}
.hero-kpis {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 16px;
  position: relative;
  z-index: 1;
}
.hero-kpi {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 6px;
  padding: 8px 12px;
}
.hk-l {
  font-size: 11.5px;
  color: #cfdcd6;
}
.hk-v {
  font-size: 22px;
  font-weight: 600;
  line-height: 30px;
  font-variant-numeric: tabular-nums;
}
.hk-v small {
  font-size: 11px;
  font-weight: 400;
  margin-left: 3px;
  color: #cfdcd6;
}
.sec-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 16px 0 10px;
}
.sec-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.sec-head span {
  font-size: 11.5px;
  color: var(--text-2);
}
.course-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.course-card {
  transition: box-shadow 0.15s;
}
.course-card:hover {
  box-shadow: var(--shadow);
}
.course-body {
  padding: 9px 12px 11px;
}
.course-name {
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.course-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
  font-size: 11.5px;
  color: var(--text-2);
}
.course-sem {
  margin-top: 3px;
  font-size: 11px;
  color: #8b969f;
}
.portal-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr);
  gap: 14px;
  margin-top: 14px;
}
.more {
  font-size: 11.5px;
  color: var(--secondary);
  font-weight: 400;
  cursor: pointer;
}
.todo-list,
.score-list,
.peer-list,
.notice-list {
  display: flex;
  flex-direction: column;
}
.todo-item,
.score-item,
.peer-item,
.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
}
.todo-item:last-child,
.score-item:last-child,
.peer-item:last-child,
.notice-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.todo-main,
.score-main {
  flex: 1;
  min-width: 0;
}
.todo-title,
.score-title,
.peer-title {
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.todo-meta,
.score-meta,
.peer-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-2);
}
.score-num {
  width: 52px;
  text-align: right;
  font-size: 16px;
  color: var(--primary);
  font-variant-numeric: tabular-nums;
}
.peer-item > div {
  flex: 1;
  min-width: 0;
}
.notice-title {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.notice-time {
  font-size: 11px;
  color: var(--text-2);
  white-space: nowrap;
}
@media (max-width: 1000px) {
  .course-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .portal-grid {
    grid-template-columns: 1fr;
  }
}
</style>
