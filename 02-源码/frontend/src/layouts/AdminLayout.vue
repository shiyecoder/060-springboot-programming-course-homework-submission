<template>
  <div class="shell">
    <!-- 侧栏：底色按角色区分，菜单由后端按角色动态下发 -->
    <aside class="sidebar" :style="{ background: sideColor }">
      <div class="brand">
        <span class="logo">码</span>
        <span>作业提交与互评</span>
      </div>
      <nav class="menu-wrap">
        <template v-for="m in menuTree" :key="m.id">
          <!-- 一级无子项：直接是可点击菜单 -->
          <div v-if="!m.children || !m.children.length" class="m-item" :class="{ on: isOn(m.path) }"
               @click="go(m.path)">
            <Icon class="ico" :name="m.icon" :size="15" />
            <span>{{ m.name }}</span>
          </div>
          <!-- 一级有子项：分组标题 + 子菜单 -->
          <div v-else class="m-group">
            <div class="m-title" @click="toggle(m.id)">
              <span>{{ m.name }}</span>
              <span style="font-size:10px;opacity:.6">{{ closed[m.id] ? '▸' : '▾' }}</span>
            </div>
            <div v-show="!closed[m.id]">
              <div v-for="c in m.children" :key="c.id" class="m-item sub" :class="{ on: isOn(c.path) }"
                   @click="go(c.path)">
                <Icon class="ico" :name="c.icon" :size="15" />
                <span>{{ c.name }}</span>
              </div>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="crumb">
          <span v-if="parentName">{{ parentName }} / </span><b>{{ currentTitle }}</b>
        </div>
        <div class="top-right">
          <button class="icon-btn" title="站内消息" @click="go('/message')" style="position:relative">
            <Icon name="bell" :size="16" />
            <i v-if="store.unread > 0" class="dot">{{ store.unread > 99 ? '99+' : store.unread }}</i>
          </button>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="avatar-box">
              <img :src="avatarUrl" alt="头像" />
              <div>
                <div class="nm">{{ store.user.realName || store.user.username }}</div>
                <div class="rl">{{ roleShort }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="message">站内消息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '../components/Icon.vue'
import { store, clearLogin, sideColor, roleShort } from '../store/user'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const closed = reactive({})

const menuTree = computed(() => store.menus || [])
const currentTitle = computed(() => route.meta.title || '')
const parentName = computed(() => {
  for (const m of menuTree.value) {
    const kids = m.children || []
    if (kids.some((c) => c.path === route.path)) return m.name
  }
  return ''
})
const avatarUrl = computed(() => store.user.avatar || '/uploads/seed/avatar/01.jpg')

function isOn(path) {
  if (!path) return false
  if (route.path === path) return true
  // 详情页高亮归属列表菜单
  if (path === '/assignment' && route.path.startsWith('/assignment/detail')) return true
  return false
}
function go(path) {
  if (path) router.push(path)
}
function toggle(id) {
  closed[id] = !closed[id]
}
function onCommand(cmd) {
  if (cmd === 'logout') {
    api.logout ? api.logout() : null
    clearLogin()
    router.push('/login')
    return
  }
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'message') router.push('/message')
}

onMounted(async () => {
  try {
    const d = await api.unread()
    store.unread = d?.unread ?? d?.count ?? 0
  } catch (e) {
    store.unread = 0
  }
})
</script>

<style scoped>
.dot {
  position: absolute; top: -4px; right: -5px; min-width: 15px; height: 15px; line-height: 15px;
  padding: 0 3px; border-radius: 8px; background: #B02A37; color: #fff; font-size: 10px;
  font-style: normal; text-align: center;
}
</style>
