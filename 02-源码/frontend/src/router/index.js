import { createRouter, createWebHashHistory } from 'vue-router'
import { store } from '../store/user'

/**
 * 路由表与后端 sys_menu.path 一一对应：侧栏菜单由后端按角色下发（/auth/menus），
 * 这里只负责「路径 → 组件」映射与登录/角色守卫。
 * 29 个页面与 design/ 下的 29 张设计稿一一对应。
 */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { title: '登录', public: true } },

  // 教学管理
  { path: '/dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '教学工作台' } },
  { path: '/course', component: () => import('../views/Course.vue'), meta: { title: '课程管理' } },
  { path: '/assignment/new', component: () => import('../views/AssignmentNew.vue'), meta: { title: '发布作业' } },
  { path: '/assignment', component: () => import('../views/AssignmentList.vue'), meta: { title: '作业台账' } },
  { path: '/assignment/detail/:id', component: () => import('../views/AssignmentDetail.vue'), meta: { title: '作业详情' } },
  { path: '/result/entry', component: () => import('../views/ResultEntry.vue'), meta: { title: '结果登记' } },
  { path: '/result/import', component: () => import('../views/ResultImport.vue'), meta: { title: '批量导入' } },

  // 互评
  { path: '/peer/assign', component: () => import('../views/PeerAssign.vue'), meta: { title: '互评分配' } },
  { path: '/peer/desk', component: () => import('../views/PeerDesk.vue'), meta: { title: '互评工作台' } },
  { path: '/peer/progress', component: () => import('../views/PeerProgress.vue'), meta: { title: '互评进度' } },

  // 查重
  { path: '/similarity', component: () => import('../views/SimilarityList.vue'), meta: { title: '查重记录' } },
  { path: '/similarity/review', component: () => import('../views/SimilarityReview.vue'), meta: { title: '相似度复核' } },

  // 成绩
  { path: '/score/review', component: () => import('../views/ScoreReview.vue'), meta: { title: '成绩审核' } },
  { path: '/score', component: () => import('../views/ScoreList.vue'), meta: { title: '成绩汇总' } },
  { path: '/appeal', component: () => import('../views/Appeal.vue'), meta: { title: '申诉处理' } },

  // 互评可信度（创新点）
  { path: '/credibility', component: () => import('../views/CredibilityList.vue'), meta: { title: '可信度明细' } },
  { path: '/credibility/rule', component: () => import('../views/RuleConfig.vue'), meta: { title: '规则配置' } },
  { path: '/credibility/trace', component: () => import('../views/CredibilityTrace.vue'), meta: { title: '计算留痕' } },

  // 统计
  { path: '/stats', component: () => import('../views/Stats.vue'), meta: { title: '成绩统计' } },

  // 系统管理
  { path: '/system/user', component: () => import('../views/SystemUser.vue'), meta: { title: '用户管理' } },
  { path: '/system/role', component: () => import('../views/SystemRole.vue'), meta: { title: '角色与菜单' } },
  // 菜单与字典共用一页：字典维护在本页，菜单分配在角色页完成
  { path: '/system/menu', component: () => import('../views/SystemMenu.vue'), meta: { title: '菜单与字典' } },
  { path: '/system/log', component: () => import('../views/SystemLog.vue'), meta: { title: '操作日志' } },

  // 学生端
  { path: '/my/submissions', component: () => import('../views/MySubmissions.vue'), meta: { title: '我的提交' } },
  { path: '/my/submit/:id', component: () => import('../views/SubmitCode.vue'), meta: { title: '提交代码', portal: true } },
  { path: '/message', component: () => import('../views/Message.vue'), meta: { title: '站内消息' } },
  { path: '/profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } },
  { path: '/portal', component: () => import('../views/Portal.vue'), meta: { title: '学生门户', portal: true } },
  { path: '/mobile', component: () => import('../views/Mobile.vue'), meta: { title: '移动端', portal: true } },

  { path: '/', redirect: () => (store.user.roleCode === 'STUDENT' ? '/portal' : '/dashboard') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

/** 学生可访问的前台页面：我的提交、提交代码、互评工作台、申诉、门户、移动端、消息与个人中心 */
const STU_ALLOW = ['/portal', '/mobile', '/my/submissions', '/my/submit', '/peer/desk', '/appeal',
  '/message', '/profile']

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) return true
  if (!token) return '/login'
  // 学生的后台页一律打回门户，避免点到空页面
  if (store.user.roleCode === 'STUDENT' && !STU_ALLOW.some((p) => to.path === p || to.path.startsWith(p + '/'))) {
    return '/portal'
  }
  return true
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '编程课程作业提交与互评系统'
})

export default router
