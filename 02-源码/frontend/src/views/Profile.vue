<template>
  <AdminLayout>
    <div class="page-head">
      <div>
        <h1>个人中心</h1>
        <p class="sub">资料修改、密码修改与头像上传 · 头像保存后顶栏与门户同步更新</p>
      </div>
    </div>

    <div class="profile-grid">
      <!-- 左：资料卡 -->
      <div class="card" style="margin-bottom:0;text-align:center">
        <el-upload
          :show-file-list="false"
          :http-request="uploadAvatar"
          accept="image/jpeg,image/png,image/webp,image/gif"
          class="avatar-upload"
        >
          <div class="avatar-big">
            <img :src="info.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <span class="avatar-mask">更换头像</span>
          </div>
        </el-upload>
        <div class="profile-name">{{ info.realName || info.username || '未登录' }}</div>
        <div class="profile-role">{{ info.roleName || '—' }}<span v-if="info.title"> · {{ info.title }}</span></div>

        <div class="info-list">
          <div class="info-row"><span>账号</span><b class="mono">{{ info.username || '—' }}</b></div>
          <div class="info-row"><span>学号</span><b class="mono">{{ info.studentNo || '—' }}</b></div>
          <div class="info-row"><span>班级</span><b>{{ info.className || '—' }}</b></div>
          <div class="info-row"><span>角色</span><b>{{ info.roleName || '—' }}</b></div>
          <div class="info-row"><span>学院/部门</span><b>{{ info.orgName || '—' }}</b></div>
          <div class="info-row"><span>互评信用分</span><b>{{ info.creditScore ?? '—' }}</b></div>
        </div>
        <div class="avatar-tip">支持 jpg/png/webp/gif，单文件不超过 20MB</div>
      </div>

      <!-- 右：两个 tab -->
      <div class="card" style="margin-bottom:0;padding-top:6px">
        <el-tabs v-model="tab">
          <el-tab-pane label="修改资料" name="profile">
            <div class="fgrid" style="max-width:760px">
              <div class="it">
                <div class="lb req">姓名</div>
                <el-input v-model="profileForm.realName" placeholder="真实姓名" />
              </div>
              <div class="it">
                <div class="lb">手机号</div>
                <el-input v-model="profileForm.phone" placeholder="11 位手机号" maxlength="20" />
              </div>
              <div class="it">
                <div class="lb">邮箱</div>
                <el-input v-model="profileForm.email" placeholder="name@univ.edu.cn" />
              </div>
              <div class="it">
                <div class="lb">头像路径</div>
                <el-input v-model="profileForm.avatar" readonly placeholder="通过左侧头像上传更换" />
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:10px;margin-top:16px">
              <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
              <span style="font-size:11.5px;color:var(--text-2)">保存后顶栏姓名与头像立即同步，下次登录仍然生效</span>
            </div>
          </el-tab-pane>

          <el-tab-pane label="修改密码" name="password">
            <div class="fgrid" style="max-width:760px">
              <div class="it">
                <div class="lb req">原密码</div>
                <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
              </div>
              <div class="it">
                <div class="lb req">新密码</div>
                <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少 6 位" />
              </div>
              <div class="it">
                <div class="lb req">确认密码</div>
                <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
              </div>
            </div>
            <div style="display:flex;align-items:center;gap:10px;margin-top:16px">
              <el-button type="primary" :loading="savingPwd" @click="savePassword">更新密码</el-button>
              <span style="font-size:11.5px;color:var(--text-2)">新密码至少 6 位；修改成功后请使用新密码重新登录</span>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { api } from '@/api'
import { store } from '@/store/user'

const tab = ref('profile')
const savingProfile = ref(false)
const savingPwd = ref(false)
const info = reactive({ ...store.user })

const profileForm = reactive({
  realName: store.user.realName || '',
  phone: store.user.phone || '',
  email: store.user.email || '',
  avatar: store.user.avatar || ''
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 同步 store 与 localStorage，保证顶栏/门户不用刷新即可看到新资料 */
function persistUser(patch) {
  Object.assign(store.user, patch)
  Object.assign(info, patch)
  try {
    const cur = JSON.parse(localStorage.getItem('user') || '{}') || {}
    localStorage.setItem('user', JSON.stringify({ ...cur, ...patch }))
  } catch (e) {
    /* localStorage 异常不阻断页面 */
  }
}

async function loadMe() {
  try {
    const d = await api.me()
    if (!d) return
    const u = d.user || d
    Object.assign(info, u)
    Object.assign(profileForm, {
      realName: u.realName || '',
      phone: u.phone || '',
      email: u.email || '',
      avatar: u.avatar || ''
    })
    persistUser({
      realName: u.realName,
      avatar: u.avatar,
      studentNo: u.studentNo,
      className: u.className,
      phone: u.phone,
      email: u.email,
      orgName: u.orgName,
      roleName: u.roleName
    })
  } catch (e) {
    /* 使用 store 缓存兜底 */
  }
}

/** 头像上传：先传文件拿相对路径，再回写 profile.avatar */
async function uploadAvatar(options) {
  const file = options?.file
  if (!file) return
  try {
    const res = await api.upload(file)
    const url = res?.url || res?.path
    if (!url) {
      ElMessage.warning('上传返回缺少文件路径')
      return
    }
    profileForm.avatar = url
    await api.updateProfile({ avatar: url })
    persistUser({ avatar: url })
    ElMessage.success('头像已更新')
  } catch (e) {
    /* 接口层已提示 */
  }
}

async function saveProfile() {
  if (!String(profileForm.realName || '').trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  if (profileForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  savingProfile.value = true
  try {
    const payload = {
      realName: profileForm.realName.trim(),
      phone: profileForm.phone.trim(),
      email: profileForm.email.trim(),
      avatar: profileForm.avatar
    }
    await api.updateProfile(payload)
    persistUser(payload)
    ElMessage.success('资料已保存')
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (!pwdForm.oldPassword) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (String(pwdForm.newPassword || '').length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await api.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码已更新')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (e) {
    /* 接口层已提示 */
  } finally {
    savingPwd.value = false
  }
}

onMounted(loadMe)
</script>

<style scoped>
.page-head h1 { margin: 0; font-size: 18px; font-weight: 600; letter-spacing: .2px; }
.page-head .sub { margin: 4px 0 0; }
.profile-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}
.avatar-upload {
  display: inline-block;
}
.avatar-big {
  width: 96px;
  height: 96px;
  margin: 6px auto 0;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  background: #e8eaee;
  cursor: pointer;
}
.avatar-big img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.avatar-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 26px;
  line-height: 26px;
  font-size: 11px;
  color: #fff;
  background: rgba(18, 36, 31, 0.62);
  opacity: 0;
  transition: opacity 0.15s;
}
.avatar-big:hover .avatar-mask {
  opacity: 1;
}
.profile-name {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
}
.profile-role {
  margin-top: 3px;
  font-size: 12px;
  color: var(--text-2);
}
.info-list {
  margin-top: 16px;
  border-top: 1px solid var(--line);
  padding-top: 8px;
  text-align: left;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 0;
  font-size: 12.5px;
}
.info-row span {
  color: var(--text-2);
}
.info-row b {
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}
.avatar-tip {
  margin-top: 10px;
  font-size: 11px;
  color: var(--text-2);
  line-height: 1.7;
}
@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
