<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>站内消息</h1>
        <p class="sub">未读 {{ unreadCount }} 条 · 共 {{ allTotal }} 条 · 点击消息即标记已读，关联业务可直接跳转</p>
      </div>
      <div class="acts">
        <el-button type="primary" :loading="readingAll" :disabled="!unreadCount" @click="readAll">全部已读</el-button>
      </div>
    </div>

    <div class="msg-grid">
      <!-- 左：按 msgType 分组筛选 -->
      <div class="card" style="margin-bottom:0">
        <h3 class="card-title">消息类型</h3>
        <div class="type-item" :class="{ on: !query.type }" @click="pickType('')">
          <span>全部消息</span>
          <b>{{ allTotal }}</b>
        </div>
        <div
          v-for="t in types"
          :key="t.name"
          class="type-item"
          :class="{ on: query.type === t.name }"
          @click="pickType(t.name)"
        >
          <span>{{ t.name }}</span>
          <b>
            {{ t.count }}
            <em v-if="t.unread">{{ t.unread }} 未读</em>
          </b>
        </div>
        <el-empty v-if="!types.length" description="暂无消息类型" :image-size="56" />
        <div class="type-tip">
          互评截止前 2 小时未完成的评阅人会自动收到提醒；查重相似度超过告警线时向教师推送复核消息。
        </div>
      </div>

      <!-- 右：消息列表 -->
      <div class="card" style="margin-bottom:0;padding:0">
        <div v-loading="loading" class="msg-list">
          <div
            v-for="m in rows"
            :key="m.id"
            class="msg-item"
            :class="{ unread: m.isRead === 0 }"
            @click="openMessage(m)"
          >
            <div class="msg-ico" :class="iconClass(m.msgType)">
              <Icon :name="iconName(m.msgType)" :size="16" />
            </div>
            <div class="msg-main">
              <div class="msg-title">
                <i v-if="m.isRead === 0" class="dot"></i>
                <span>{{ m.title || '（无标题）' }}</span>
              </div>
              <div class="msg-content">{{ m.content || '' }}</div>
            </div>
            <div class="msg-side">
              <span class="tag" :class="typeTag(m.msgType)">{{ m.msgType || '系统' }}</span>
              <span class="msg-time">{{ fmtTime(m.createTime) }}</span>
            </div>
          </div>
          <el-empty v-if="!loading && !rows.length" description="当前类型下暂无消息" :image-size="72" />
        </div>

        <div class="pager" style="padding:12px 16px 14px">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[8, 16, 30]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @size-change="onSizeChange"
            @current-change="load"
          />
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Icon from '@/components/Icon.vue'
import { api } from '@/api'
import { fmtTime } from '@/utils/format'
import { store } from '@/store/user'

const router = useRouter()

const loading = ref(false)
const readingAll = ref(false)
const rows = ref([])
const total = ref(0)
const allRecords = ref([])
const allTotal = ref(0)

const query = reactive({ type: '', page: 1, size: 8 })

const unreadCount = computed(() => allRecords.value.filter((m) => m.isRead === 0).length)

// 消息类型分组：从当前账号的全部消息中聚合，避免硬编码枚举
const types = computed(() => {
  const map = new Map()
  allRecords.value.forEach((m) => {
    const k = m.msgType || '系统'
    const cur = map.get(k) || { name: k, count: 0, unread: 0 }
    cur.count += 1
    if (m.isRead === 0) cur.unread += 1
    map.set(k, cur)
  })
  return Array.from(map.values()).sort((a, b) => b.count - a.count)
})

function typeTag(t) {
  const s = String(t || '')
  if (s.includes('查重')) return 't-red'
  if (s.includes('成绩')) return 't-green'
  if (s.includes('待办') || s.includes('申诉')) return 't-amber'
  if (s.includes('作业') || s.includes('互评')) return 't-blue'
  return 't-gray'
}

function iconName(t) {
  const s = String(t || '')
  if (s.includes('查重')) return 'shield'
  if (s.includes('成绩')) return 'chart'
  if (s.includes('申诉')) return 'flag'
  if (s.includes('待办')) return 'clock'
  if (s.includes('互评') || s.includes('作业')) return 'task'
  return 'bell'
}

function iconClass(t) {
  const s = String(t || '')
  if (s.includes('查重')) return 'red'
  if (s.includes('成绩')) return 'green'
  if (s.includes('待办') || s.includes('申诉')) return 'amber'
  return 'blue'
}

async function load() {
  loading.value = true
  try {
    const d = await api.messagePage({
      type: query.type || undefined,
      page: query.page,
      size: query.size
    })
    rows.value = d?.records || []
    total.value = d?.total || 0
    if (typeof d?.unread === 'number') store.unread = d.unread
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 一次性取 100 条做类型数量统计（消息量小，避免额外统计接口） */
async function loadAll() {
  try {
    const d = await api.messagePage({ page: 1, size: 100 })
    allRecords.value = d?.records || []
    allTotal.value = d?.total || allRecords.value.length
    if (typeof d?.unread === 'number') store.unread = d.unread
  } catch (e) {
    allRecords.value = []
    allTotal.value = 0
  }
}

function pickType(t) {
  query.type = t
  query.page = 1
  load()
}

function onSizeChange() {
  query.page = 1
  load()
}

/** 点击消息：先置已读，再按 bizId 跳作业详情；无 bizId 时用后端 linkUrl */
async function openMessage(m) {
  if (m.isRead === 0) {
    try {
      await api.readMessage(m.id)
      m.isRead = 1
      const hit = allRecords.value.find((x) => x.id === m.id)
      if (hit) hit.isRead = 1
      store.unread = Math.max(0, (store.unread || 0) - 1)
    } catch (e) {
      /* 已读失败不阻断跳转 */
    }
  }
  if (m.bizId) {
    router.push(`/assignment/detail/${m.bizId}`)
  } else if (m.linkUrl) {
    router.push(m.linkUrl)
  }
}

async function readAll() {
  readingAll.value = true
  try {
    await api.readAll()
    allRecords.value.forEach((m) => (m.isRead = 1))
    rows.value.forEach((m) => (m.isRead = 1))
    store.unread = 0
    ElMessage.success('已将全部消息标记为已读')
    load()
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    readingAll.value = false
  }
}

onMounted(() => {
  loadAll()
  load()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.msg-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.type-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: var(--text);
}
.type-item:hover {
  background: var(--bg);
}
.type-item.on {
  background: #e7efeb;
  color: var(--primary);
  font-weight: 600;
}
.type-item b {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-2);
  white-space: nowrap;
}
.type-item em {
  font-style: normal;
  color: var(--danger);
  margin-left: 3px;
  font-size: 11px;
}
.type-tip {
  margin-top: 10px;
  padding: 8px 10px;
  font-size: 11.5px;
  color: var(--text-2);
  line-height: 1.8;
  background: var(--bg);
  border-radius: 4px;
}
.msg-list {
  min-height: 280px;
}
.msg-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.15s;
}
.msg-item:last-child {
  border-bottom: none;
}
.msg-item:hover {
  background: #fafbfb;
}
.msg-item.unread {
  background: #f7faf8;
}
.msg-ico {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  color: var(--secondary);
  background: #e7efeb;
}
.msg-ico.red {
  color: var(--danger);
  background: #fbecee;
}
.msg-ico.green {
  color: var(--success);
  background: #eaf4ef;
}
.msg-ico.amber {
  color: #9c5c12;
  background: #fbedE1;
}
.msg-ico.blue {
  color: var(--secondary);
  background: #eef2f9;
}
.msg-main {
  flex: 1;
  min-width: 0;
}
.msg-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  line-height: 1.5;
}
.msg-item.unread .msg-title span {
  font-weight: 600;
}
.dot {
  width: 6px;
  height: 6px;
  flex: 0 0 6px;
  border-radius: 50%;
  background: #2f6fed;
  display: inline-block;
}
.msg-content {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.msg-side {
  flex: 0 0 auto;
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.msg-time {
  font-size: 11px;
  color: var(--text-2);
  white-space: nowrap;
}
@media (max-width: 900px) {
  .msg-grid {
    grid-template-columns: 1fr;
  }
}
</style>
