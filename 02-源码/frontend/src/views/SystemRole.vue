<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>角色与菜单权限</h1>
        <p class="sub">
          菜单权限 + 按钮权限 + 数据范围三层控制 · 「菜单管理」节点不参与分配，避免越权授予
        </p>
      </div>
    </div>

    <div class="role-grid">
      <!-- 左：角色列表 -->
      <div class="card" style="margin-bottom:0">
        <h3 class="card-title">角色列表</h3>
        <div
          v-for="r in roles"
          :key="r.id"
          class="role-item"
          :class="{ on: current && current.id === r.id }"
          @click="pick(r)"
        >
          <div class="role-name">
            <span>{{ r.roleName }}</span>
            <span class="tag t-gray">{{ r.userCount || 0 }} 人</span>
          </div>
          <div class="role-sub">
            <span class="mono">{{ r.roleCode }}</span>
            <span>· {{ scopeText(r.dataScope) }}</span>
          </div>
        </div>
        <el-empty v-if="!roles.length" description="暂无角色" :image-size="60" />
      </div>

      <!-- 右：菜单树 -->
      <div class="card" style="margin-bottom:0">
        <div class="card-title">
          <span>
            {{ current ? current.roleName : '请选择角色' }} · 菜单权限
            <span style="font-size:12px;font-weight:400;color:var(--text-2);margin-left:6px">
              已选 {{ checkedCount }} / {{ visibleCount }} 项
            </span>
          </span>
          <span style="font-size:12px;font-weight:400;color:var(--text-2)">
            {{ current ? scopeText(current.dataScope) : '—' }}
          </span>
        </div>

        <div v-if="current?.remark" class="role-remark">{{ current.remark }}</div>

        <div v-loading="treeLoading" class="tree-wrap">
          <el-tree
            ref="treeRef"
            :data="treeData"
            node-key="id"
            show-checkbox
            default-expand-all
            :props="treeProps"
            empty-text=""
            @check="syncChecked"
          />
          <el-empty v-if="!treeLoading && !treeData.length" description="暂无菜单数据" :image-size="60" />
        </div>

        <div class="tree-tip">
          硬性规则：树中已隐藏「菜单管理 / 菜单与字典」节点，任何角色都无法通过本页获得菜单管理权限，该入口仅系统管理员的基线配置可达。
        </div>

        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:14px">
          <el-button :disabled="!current" @click="reloadChecked">还原</el-button>
          <el-button type="primary" :disabled="!current" :loading="saving" @click="save">保存权限</el-button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'

const roles = ref([])
const current = ref(null)
const rawTree = ref([])
const checkedKeys = ref([])
const treeLoading = ref(false)
const saving = ref(false)
const treeRef = ref(null)

// 菜单接口字段为驼峰 menuName；兼容 menu_name 命名，避免后端联调期字段差异
function menuName(n) {
  return n?.menuName || n?.menu_name || n?.name || '未命名菜单'
}

const treeProps = {
  children: 'children',
  label: (data) => menuName(data)
}

// 递归剔除「菜单管理」节点及其子树（硬性要求）
// 实际菜单名为「菜单与字典」（path=/system/menu），同时兼容「菜单管理」命名
function hideMenuManage(nodes) {
  const out = []
  for (const n of nodes || []) {
    const nm = String(menuName(n))
    const isMenuAdmin =
      nm.includes('菜单管理') ||
      nm.includes('菜单与字典') ||
      n.path === '/system/menu' ||
      n.perms === 'menu:list' ||
      n.perm === 'menu:list'
    if (isMenuAdmin) continue
    out.push({ ...n, children: hideMenuManage(n.children) })
  }
  return out
}

const treeData = computed(() => hideMenuManage(rawTree.value))

// 当前树中可见节点总数（用于「已选 x / y」）
const visibleCount = computed(() => {
  let n = 0
  const walk = (list) => {
    for (const it of list || []) {
      n += 1
      walk(it.children)
    }
  }
  walk(treeData.value)
  return n
})

const checkedCount = computed(() => checkedKeys.value.length)

function scopeText(s) {
  return { ALL: '全部数据', ORG: '本部门数据', SELF: '仅本人数据' }[s] || '按角色授权'
}

function collectIds(nodes, set) {
  for (const n of nodes || []) {
    set.add(n.id)
    collectIds(n.children, set)
  }
  return set
}

async function loadRoles() {
  try {
    const d = await api.roleList()
    roles.value = Array.isArray(d) ? d : []
    if (roles.value.length) {
      await pick(roles.value[0])
    }
  } catch (e) {
    roles.value = []
  }
}

async function loadTree() {
  treeLoading.value = true
  try {
    const d = await api.menuTree()
    rawTree.value = Array.isArray(d) ? d : []
  } catch (e) {
    // 兜底：菜单树接口异常时用扁平菜单列表组装两级以上的树，保证权限分配仍可使用
    try {
      const list = await api.menuList()
      rawTree.value = buildTree(Array.isArray(list) ? list : [])
    } catch (e2) {
      rawTree.value = []
    }
  } finally {
    treeLoading.value = false
  }
}

/** 扁平菜单 → 树（parentId=0 或找不到父节点视为根） */
function buildTree(list) {
  const map = new Map()
  list.forEach((m) => map.set(Number(m.id), { ...m, children: [] }))
  const roots = []
  list.forEach((m) => {
    const node = map.get(Number(m.id))
    const pid = Number(m.parentId || 0)
    if (pid && map.has(pid)) map.get(pid).children.push(node)
    else roots.push(node)
  })
  return roots
}

async function pick(role) {
  current.value = role
  await reloadChecked()
}

/** 回显：只保留当前树中可见的菜单 id，隐藏节点不会写回 */
async function reloadChecked() {
  if (!current.value) {
    checkedKeys.value = []
    return
  }
  let ids = []
  try {
    const d = await api.roleMenus(current.value.id)
    ids = Array.isArray(d) ? d.map((v) => Number(v)) : []
  } catch (e) {
    ids = []
  }
  await nextTick()
  const visible = collectIds(treeData.value, new Set())
  const filtered = ids.filter((id) => visible.has(id))
  treeRef.value?.setCheckedKeys(filtered)
  checkedKeys.value = treeRef.value?.getCheckedKeys?.() || filtered
}

function syncChecked() {
  checkedKeys.value = treeRef.value?.getCheckedKeys?.() || []
}

/** 保存：含半选父节点，保证目录菜单不会因只选子项而丢失 */
async function save() {
  if (!current.value) return
  const checked = treeRef.value?.getCheckedKeys?.() || []
  const half = treeRef.value?.getHalfCheckedKeys?.() || []
  const menuIds = Array.from(new Set([...checked, ...half]))
  try {
    await ElMessageBox.confirm(
      `确认保存角色「${current.value.roleName}」的 ${menuIds.length} 项菜单权限？保存后该角色成员重新登录即可生效。`,
      '保存权限',
      { type: 'warning', confirmButtonText: '确认保存', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await api.roleSaveMenus(current.value.id, { menuIds })
    ElMessage.success('菜单权限已保存')
    checkedKeys.value = checked
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await loadTree()
  await loadRoles()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.role-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.role-item {
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}
.role-item:hover {
  border-color: var(--secondary);
}
.role-item.on {
  background: #e7efeb;
  border-color: var(--primary);
}
.role-name {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
}
.role-sub {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-2);
}
.role-remark {
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.7;
  padding: 8px 10px;
  background: var(--bg);
  border-radius: 4px;
  margin-bottom: 10px;
}
.tree-wrap {
  min-height: 120px;
  border: 1px solid var(--line);
  border-radius: 6px;
  padding: 10px 12px;
  max-height: 520px;
  overflow: auto;
}
.tree-wrap :deep(.el-tree-node__content) {
  height: 30px;
}
.tree-tip {
  margin-top: 10px;
  padding: 8px 10px;
  font-size: 11.5px;
  color: var(--text-2);
  line-height: 1.8;
  background: #f7faf8;
  border-left: 3px solid var(--gold);
  border-radius: 4px;
}
@media (max-width: 1000px) {
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
