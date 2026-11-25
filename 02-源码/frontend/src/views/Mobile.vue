<template>
  <PortalLayout>
    <div class="mobile-head">
      <h2>移动端 · 学生门户</h2>
      <p>390 × 780 手机屏 · 小于 768px 时侧栏改抽屉、表格转卡片、主按钮固定安全区</p>
    </div>

    <div class="phone-stage">
      <!-- 手机一：作业列表 -->
      <div class="phone">
        <div class="p-status">
          <span>9:41</span>
          <span>5G ▮▮▮ 86%</span>
        </div>
        <div class="p-title">我的作业</div>
        <div class="p-body">
          <div v-if="todos.length" class="p-list">
            <div
              v-for="(a, i) in todos"
              :key="a.id"
              class="p-card"
              :class="{ current: i === 0 }"
              @click="router.push(`/assignment/detail/${a.id}`)"
            >
              <div class="p-card-top">
                <span class="p-course">{{ a.courseName || '—' }}</span>
                <span class="tag" :class="a.submitted ? 't-green' : 't-amber'">
                  {{ a.submitted ? '已提交' : '待提交' }}
                </span>
              </div>
              <div class="p-card-title">{{ a.title }}</div>
              <div class="p-card-meta">
                截止 {{ fmtTime(a.deadline) }} · {{ countdown(a.deadline) }}
              </div>
              <div class="p-card-foot">
                <span class="tag" :class="i === 0 ? 't-gray' : 't-blue'">{{ a.languageType || '—' }}</span>
                <el-button size="small" :class="{ ghost: i === 0 }">查看提交</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无待完成作业" :image-size="60" />
        </div>
        <div class="p-tabs">
          <div v-for="t in tabs" :key="t.key" class="p-tab" :class="{ on: t.key === 'todo' }" @click="goTab(t)">
            <Icon :name="t.icon" :size="17" />
            <span>{{ t.name }}</span>
          </div>
        </div>
      </div>

      <!-- 手机二：我的提交 -->
      <div class="phone">
        <div class="p-status">
          <span>9:41</span>
          <span>5G ▮▮▮ 86%</span>
        </div>
        <div class="p-title">我的提交</div>
        <div class="p-body">
          <div v-if="subs.length" class="p-list">
            <div v-for="s in subs" :key="s.id" class="p-card">
              <div class="p-card-top">
                <span class="p-course">{{ s.courseName || '—' }}</span>
                <span class="tag" :class="compileTag(s.compileResult)">{{ s.compileResult || '未运行' }}</span>
              </div>
              <div class="p-card-title">{{ s.assignmentTitle || '—' }}</div>
              <div class="p-card-meta">提交于 {{ fmtTime(s.submitTime) }}</div>
              <div class="pass-row">
                <div class="pass-track">
                  <div class="pass-bar" :style="{ width: passRate(s) + '%' }"></div>
                </div>
                <span class="pass-text">
                  用例 {{ s.passCount ?? 0 }}/{{ s.totalCount ?? 0 }} · {{ passRate(s) }}%
                </span>
              </div>
              <div class="p-card-foot">
                <span class="tag" :class="s.isLate ? 't-red' : 't-gray'">{{ s.isLate ? '迟交' : '按时' }}</span>
                <span class="p-status-text">{{ s.status || '—' }}</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无提交记录" :image-size="60" />
        </div>
        <div class="p-tabs">
          <div v-for="t in tabs" :key="t.key" class="p-tab" :class="{ on: t.key === 'score' }" @click="goTab(t)">
            <Icon :name="t.icon" :size="17" />
            <span>{{ t.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="mobile-note">
      说明：移动端复用学生门户接口（portal/home + submission/mine），仅做布局收敛；底部 tab 对应作业、互评、成绩、我的四个入口。
    </div>
  </PortalLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PortalLayout from '@/layouts/PortalLayout.vue'
import Icon from '@/components/Icon.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'

const router = useRouter()

const todos = ref([])
const subs = ref([])

const tabs = [
  { key: 'todo', name: '作业', icon: 'book', path: '/portal' },
  { key: 'peer', name: '互评', icon: 'task', path: '/peer/desk' },
  { key: 'score', name: '成绩', icon: 'chart', path: '/my/submissions' },
  { key: 'me', name: '我的', icon: 'user', path: '/profile' }
]

// 截止倒计时：小时粒度，已过截止时间直接标「已截止」
function countdown(deadline) {
  if (!deadline) return '—'
  const t = new Date(String(deadline).replace(/-/g, '/')).getTime()
  if (Number.isNaN(t)) return '—'
  const diff = t - Date.now()
  if (diff <= 0) return '已截止'
  const hours = Math.floor(diff / 3600000)
  if (hours < 1) return '剩不到 1 小时'
  if (hours < 24) return `剩 ${hours} 小时`
  return `剩 ${Math.floor(hours / 24)} 天`
}

function passRate(row) {
  const total = Number(row.totalCount)
  const pass = Number(row.passCount)
  if (!Number.isFinite(total) || total <= 0) return 0
  return Math.min(100, Math.round((pass * 100) / total))
}

function compileTag(v) {
  if (v === '通过') return 't-green'
  if (v === '编译失败') return 't-red'
  return 't-gray'
}

function goTab(t) {
  router.push(t.path)
}

async function load() {
  try {
    const d = await api.portalHome()
    todos.value = d?.todoAssignments || []
  } catch (e) {
    todos.value = []
  }
  try {
    const d = await api.mySubmissions({ page: 1, size: 5 })
    subs.value = d?.records || []
  } catch (e) {
    subs.value = []
  }
}

onMounted(load)
</script>

<style scoped>
.mobile-head {
  text-align: center;
  margin-bottom: 14px;
}
.mobile-head h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.mobile-head p {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--text-2);
}
.phone-stage {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 28px;
}
.phone {
  width: 390px;
  height: 700px;
  flex: 0 0 390px;
  border-radius: 26px;
  background: #fff;
  border: 8px solid #1f2933;
  box-shadow: 0 10px 30px rgba(31, 41, 51, 0.14);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.p-status {
  height: 26px;
  flex: 0 0 26px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.p-title {
  height: 44px;
  flex: 0 0 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid var(--line);
}
.p-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
  background: var(--bg);
}
.p-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.p-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 11px 12px;
  cursor: pointer;
}
.p-card.current {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}
.p-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.p-course {
  font-size: 11.5px;
  color: var(--text-2);
}
.p-card.current .p-course,
.p-card.current .p-card-meta {
  color: #cfdcd6;
}
.p-card-title {
  margin-top: 5px;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.5;
}
.p-card-meta {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-2);
}
.p-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 9px;
}
.p-card.current :deep(.el-button) {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.6);
  color: #fff;
}
.pass-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.pass-track {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #edf1ef;
  overflow: hidden;
}
.pass-bar {
  height: 100%;
  background: var(--secondary);
  border-radius: 3px;
}
.pass-text {
  font-size: 10.5px;
  color: var(--text-2);
  white-space: nowrap;
}
.p-status-text {
  font-size: 11px;
  color: var(--text-2);
}
.p-tabs {
  height: 54px;
  flex: 0 0 54px;
  border-top: 1px solid var(--line);
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: #fff;
}
.p-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  font-size: 10.5px;
  color: var(--text-2);
  cursor: pointer;
}
.p-tab.on {
  color: var(--primary);
  font-weight: 600;
}
.mobile-note {
  max-width: 808px;
  margin: 14px auto 0;
  text-align: center;
  font-size: 11.5px;
  color: var(--text-2);
  line-height: 1.7;
}
@media (max-width: 900px) {
  .phone-stage {
    flex-direction: column;
    align-items: center;
  }
}
</style>
