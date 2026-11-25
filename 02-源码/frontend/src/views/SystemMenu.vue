<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>菜单与字典</h1>
        <p class="sub">三级菜单权限与业务字典统一入口 · 字典改动直接作用于全站下拉与状态标签</p>
      </div>
      <div class="acts">
        <el-button @click="refresh">刷新缓存</el-button>
      </div>
    </div>

    <div class="card" style="padding-top:6px">
      <el-tabs v-model="tab">
        <!-- 菜单 -->
        <el-tab-pane name="menu">
          <template #label>菜单结构 <span class="tab-count">{{ menus.length }}</span></template>
          <el-table v-loading="menuLoading" :data="menus" row-key="id" style="width:100%">
            <el-table-column prop="menuName" label="菜单名称" min-width="160">
              <template #default="{ row }">
                <span :style="{ paddingLeft: (row.parentId ? 12 : 0) + 'px' }">{{ row.menuName || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="menuType" label="类型" min-width="90">
              <template #default="{ row }">
                <span class="tag" :class="typeTag(row.menuType)">{{ typeText(row.menuType) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="路径" min-width="180">
              <template #default="{ row }">
                <span class="mono" style="font-size:12px">{{ row.path || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="perms" label="权限标识" min-width="190">
              <template #default="{ row }">
                <span class="mono" style="font-size:12px">{{ row.perms || row.perm || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="icon" label="图标" min-width="110">
              <template #default="{ row }">
                <span v-if="row.icon" style="display:inline-flex;align-items:center;gap:6px">
                  <Icon :name="row.icon" :size="14" />
                  <span style="font-size:12px;color:var(--text-2)">{{ row.icon }}</span>
                </span>
                <span v-else>—</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" min-width="70" align="right">
              <template #default="{ row }">{{ row.sortNo ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <span class="tag" :class="row.status === 0 ? 't-gray' : 't-green'">
                  {{ row.status === 0 ? '停用' : '正常' }}
                </span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无菜单数据" :image-size="64" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 字典：按 dictType 分组，同组仅首行显示类型与名称 -->
        <el-tab-pane name="dict">
          <template #label>业务字典 <span class="tab-count">{{ dictTotal }}</span></template>

          <div class="dict-filter">
            <el-select
              v-model="dictQuery.dictType"
              placeholder="全部字典类型"
              clearable
              filterable
              style="width:200px"
              @change="onDictSearch"
            >
              <el-option v-for="t in dictTypes" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <span style="font-size:12px;color:var(--text-2)">
              同一 dictType 下的标签/值由后端字典表统一维护，前端只做展示
            </span>
          </div>

          <el-table
            v-loading="dictLoading"
            :data="dictRows"
            row-key="id"
            :span-method="dictSpan"
            style="width:100%"
          >
            <el-table-column prop="dictType" label="字典类型" min-width="150">
              <template #default="{ row }"><span class="mono">{{ row.dictType }}</span></template>
            </el-table-column>
            <el-table-column prop="dictName" label="字典名称" min-width="120">
              <template #default="{ row }">{{ row.dictName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="dictLabel" label="标签" min-width="140">
              <template #default="{ row }">{{ row.dictLabel || '—' }}</template>
            </el-table-column>
            <el-table-column prop="dictValue" label="值" min-width="150">
              <template #default="{ row }"><span class="mono">{{ row.dictValue || '—' }}</span></template>
            </el-table-column>
            <el-table-column prop="sortNo" label="排序" min-width="70" align="right">
              <template #default="{ row }">{{ row.sortNo ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" min-width="90">
              <template #default="{ row }">
                <span class="tag" :class="row.status === 0 ? 't-gray' : 't-green'">
                  {{ row.status === 0 ? '停用' : '正常' }}
                </span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无字典数据" :image-size="64" />
            </template>
          </el-table>

          <div class="pager">
            <el-pagination
              v-model:current-page="dictQuery.page"
              v-model:page-size="dictQuery.size"
              :total="dictTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="onDictSizeChange"
              @current-change="loadDict"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Icon from '@/components/Icon.vue'
import { api } from '@/api'

const tab = ref('menu')

const menuLoading = ref(false)
const menus = ref([])

const dictLoading = ref(false)
const dictTotal = ref(0)
const dictRecords = ref([])
const dictTypes = ref([])

const dictQuery = reactive({ dictType: '', page: 1, size: 10 })

// 分组展示前先按 dictType + sortNo 排序，保证同一类型的行连续
const dictRows = computed(() => {
  const rows = [...dictRecords.value]
  rows.sort((a, b) => {
    const t = String(a.dictType || '').localeCompare(String(b.dictType || ''))
    if (t !== 0) return t
    return Number(a.sortNo || 0) - Number(b.sortNo || 0)
  })
  return rows
})

function typeText(t) {
  return { DIR: '目录', MENU: '菜单', BTN: '按钮' }[t] || t || '—'
}
function typeTag(t) {
  // 后端字典存中文（目录/菜单/按钮），同时兼容英文枚举
  return { DIR: 't-blue', MENU: 't-green', BTN: 't-gray', 目录: 't-blue', 菜单: 't-green', 按钮: 't-gray' }[t] || 't-gray'
}

async function loadMenus() {
  menuLoading.value = true
  try {
    const d = await api.menuList()
    menus.value = Array.isArray(d) ? d : []
  } catch (e) {
    menus.value = []
  } finally {
    menuLoading.value = false
  }
}

async function loadDict() {
  dictLoading.value = true
  try {
    const d = await api.dictPage({
      page: dictQuery.page,
      size: dictQuery.size,
      dictType: dictQuery.dictType || undefined
    })
    dictRecords.value = d?.records || []
    dictTotal.value = d?.total || 0
  } catch (e) {
    dictRecords.value = []
    dictTotal.value = 0
  } finally {
    dictLoading.value = false
  }
}

/** 字典类型下拉：/dict/all 返回 { dictType: [label...] }，只取 key 作为类型选项 */
async function loadDictTypes() {
  try {
    const d = await api.dictAll()
    dictTypes.value = Object.keys(d || {})
      .sort()
      .map((k) => ({ value: k, label: k }))
  } catch (e) {
    dictTypes.value = []
  }
}

function onDictSearch() {
  dictQuery.page = 1
  loadDict()
}

function onDictSizeChange() {
  dictQuery.page = 1
  loadDict()
}

/** 合并单元格：连续相同 dictType 的类型列、名称列只显示一次 */
function dictSpan({ rowIndex, columnIndex }) {
  if (columnIndex > 1) return
  const rows = dictRows.value
  if (rowIndex > 0 && rows[rowIndex]?.dictType === rows[rowIndex - 1]?.dictType) {
    return { rowspan: 0, colspan: 0 }
  }
  let span = 1
  while (rowIndex + span < rows.length && rows[rowIndex + span]?.dictType === rows[rowIndex]?.dictType) {
    span += 1
  }
  return { rowspan: span, colspan: 1 }
}

function refresh() {
  loadMenus()
  loadDict()
  loadDictTypes()
}

onMounted(() => {
  loadMenus()
  loadDict()
  loadDictTypes()
})
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.tab-count {
  display: inline-block;
  margin-left: 4px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 17px;
  color: var(--text-2);
  background: var(--bg);
  border-radius: 9px;
}
.dict-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
</style>
