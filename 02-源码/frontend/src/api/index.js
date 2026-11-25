import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 统一请求实例：后端返回 { code, message, data }，code=200 视为成功 */
const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body.data
      if (body.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        localStorage.removeItem('menus')
        if (!location.hash.includes('/login')) location.hash = '#/login'
        return Promise.reject(new Error(body.message || '登录已失效'))
      }
      ElMessage.error(body.message || '请求失败')
      const e = new Error(body.message || '请求失败')
      e.code = body.code
      return Promise.reject(e)
    }
    return body
  },
  (err) => {
    // 页面跳转/刷新时的网络中断不打扰用户
    if (document.visibilityState === 'visible') {
      ElMessage.error(err?.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

/**
 * 接口集中定义：页面只调用这里的方法，不直接写 URL。
 * 列表接口统一返回 { total, page, size, records }，字段为驼峰。
 */
export const api = {
  // ---------------- 认证与个人中心 ----------------
  login: (data) => http.post('/auth/login', data),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me'),
  menus: () => http.get('/auth/menus'),
  updateProfile: (data) => http.put('/auth/profile', data),
  changePassword: (data) => http.put('/auth/password', data),

  // ---------------- 消息 ----------------
  messagePage: (params) => http.get('/system/message/page', { params }),
  unread: () => http.get('/system/message/unread'),
  readMessage: (id) => http.put(`/system/message/${id}/read`),
  readAll: () => http.put('/system/message/read-all'),

  // ---------------- 公共下拉 ----------------
  options: () => http.get('/options'),

  // ---------------- 看板与统计 ----------------
  dashboard: (params) => http.get('/dashboard', { params }),
  statsSummary: () => http.get('/stats/summary'),
  portalHome: () => http.get('/portal/home'),

  // ---------------- 课程 ----------------
  coursePage: (params) => http.get('/course/page', { params }),
  courseSave: (data) => http.post('/course', data),

  // ---------------- 作业 ----------------
  assignmentPage: (params) => http.get('/assignment/page', { params }),
  assignmentDetail: (id) => http.get(`/assignment/${id}`),
  assignmentSave: (data) => http.post('/assignment', data),
  transition: (id, data) => http.post(`/assignment/${id}/transition`, data),

  // ---------------- 提交 ----------------
  submissionPage: (params) => http.get('/submission/page', { params }),
  mySubmissions: (params) => http.get('/submission/mine', { params }),
  submitCode: (data) => http.post('/submission', data),
  submitContext: (assignmentId) => http.get(`/submission/context/${assignmentId}`),
  versionDetail: (id) => http.get(`/submission/version/${id}`),
  resultEntry: (data) => http.post('/submission/result', data),
  resultImport: (data) => http.post('/submission/result/import', data),

  // ---------------- 互评 ----------------
  peerTaskPage: (params) => http.get('/peer/task/page', { params }),
  peerTaskDetail: (id) => http.get(`/peer/task/${id}`),
  peerScore: (data) => http.post('/peer/score', data),
  peerProgress: (params) => http.get('/peer/progress', { params }),
  peerAssign: (data) => http.post('/peer/assign', data),
  peerAssignPreview: (params) => http.get('/peer/assign/preview', { params }),
  peerAppend: (data) => http.post('/peer/append', data),
  peerRemind: (data) => http.post('/peer/remind', data),

  // ---------------- 查重 ----------------
  similarityPage: (params) => http.get('/similarity/page', { params }),
  similarityReview: (id, data) => http.post(`/similarity/${id}/review`, data),
  similarityCheck: (data) => http.post('/similarity/check', data),

  // ---------------- 成绩 ----------------
  scorePage: (params) => http.get('/score/page', { params }),
  scoreCompose: (data) => http.post('/score/compose', data),
  scoreAdjust: (id, data) => http.post(`/score/${id}/adjust`, data),
  scorePublish: (data) => http.post('/score/publish', data),

  // ---------------- 申诉 ----------------
  appealPage: (params) => http.get('/appeal/page', { params }),
  appealCreate: (data) => http.post('/appeal', data),
  appealHandle: (id, data) => http.post(`/appeal/${id}/handle`, data),

  // ---------------- 互评可信度（创新点） ----------------
  credibilityPage: (params) => http.get('/credibility/page', { params }),
  credibilityDetail: (id) => http.get(`/credibility/${id}`),
  credibilityRule: () => http.get('/credibility/rule'),
  ruleSave: (data) => http.post('/credibility/rule', data),
  recalc: (data) => http.post('/credibility/recalc', data),
  manualAdjust: (id, data) => http.post(`/credibility/${id}/manual`, data),

  // ---------------- 系统管理 ----------------
  userPage: (params) => http.get('/system/user/page', { params }),
  userCreate: (data) => http.post('/system/user', data),
  userUpdate: (id, data) => http.put(`/system/user/${id}`, data),
  userReset: (id) => http.post(`/system/user/${id}/reset`),
  rolePage: (params) => http.get('/system/role/page', { params }),
  roleList: () => http.get('/system/role/list'),
  roleMenus: (id) => http.get(`/system/role/${id}/menus`),
  roleSaveMenus: (id, data) => http.post(`/system/role/${id}/menus`, data),
  menuTree: () => http.get('/system/menu/tree'),
  menuList: () => http.get('/system/menu/list'),
  dictPage: (params) => http.get('/system/dict/page', { params }),
  dictAll: () => http.get('/system/dict/all'),
  logPage: (params) => http.get('/system/log/page', { params }),

  // ---------------- 文件上传 ----------------
  upload: (file) => {
    const fd = new FormData()
    fd.append('file', file)
    return http.post('/file/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export default http
