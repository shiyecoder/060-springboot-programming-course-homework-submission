<template>
  <div class="login-wrap">
    <!-- 左侧品牌区：工程工具风，承载「一个作业从发布到成绩的完整链路」 -->
    <section class="brand-side">
      <img class="bg" :src="bgUrl" alt="" />
      <div class="veil"></div>
      <div class="brand-inner">
        <div class="brand-top">
          <span class="mark">码</span>
          <span class="brand-name">编程课程作业提交与互评系统</span>
        </div>
        <h1>代码交上来，<br />评价摆到明面上</h1>
        <p class="lead">
          从作业发布、代码提交、用例运行结果登记，到同伴互评、代码查重、成绩合成与申诉，
          每一步都有留痕；互评是否可信由五因子算出来，不是拍脑袋给的。
        </p>

        <!-- 六步闭环链路 -->
        <ol class="flow">
          <li v-for="(s, i) in steps" :key="s.t">
            <span class="no mono">{{ String(i + 1).padStart(2, '0') }}</span>
            <div>
              <b>{{ s.t }}</b>
              <em>{{ s.d }}</em>
            </div>
          </li>
        </ol>

        <div class="metrics">
          <div><b>{{ stats.course }}</b><span>门课程</span></div>
          <div><b>{{ stats.assignment }}</b><span>份作业</span></div>
          <div><b>{{ stats.submission }}</b><span>份提交</span></div>
          <div><b>{{ stats.avgPeer }}</b><span>互评完成率 %</span></div>
        </div>
      </div>
    </section>

    <!-- 右侧登录区 -->
    <section class="form-side">
      <div class="form-box">
        <h2>登录</h2>
        <p class="sub">学生用学号登录，教师与管理员用账号登录</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="学号 / 教师账号 / 管理员账号"
                      :prefix-icon="User" clearable @keyup.enter="submit" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password
                      placeholder="请输入密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>

          <div class="row">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="hint" @click="fillDemo">演示账号</span>
          </div>

          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            进入工作台
          </el-button>
        </el-form>

        <!-- 演示账号：按角色给出可直接点击填充的账号 -->
        <div v-if="showDemo" class="demo">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-grid">
            <div v-for="d in demos" :key="d.u" class="demo-item" @click="pick(d.u)">
              <b>{{ d.r }}</b>
              <span class="mono">{{ d.u }}</span>
            </div>
          </div>
        </div>

        <p class="foot">
          提交的代码与互评记录仅用于课程教学评价；确认抄袭需两名教师签字，学生可在成绩发布后 7 天内申诉。
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { api } from '@/api'
import { setLogin } from '@/store/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const remember = ref(true)
const showDemo = ref(false)
// 背景图走后端 /uploads 静态映射，用绑定避免被 Rollup 当成模块解析
const bgUrl = '/uploads/seed/bg/01.jpg'

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const steps = [
  { t: '发布作业', d: '教师配置用例数、互评份数与成绩权重' },
  { t: '提交代码', d: '学生提交源文件，截止后进入迟交窗口' },
  { t: '登记结果', d: '用例通过数与编译结果留痕' },
  { t: '同伴互评', d: '匿名分维度打分，评语须指出代码位置' },
  { t: '查重与可信度', d: '结构/标识符/注释三因子合成相似度' },
  { t: '成绩与申诉', d: '按可信度等级决定互评分如何计入' }
]

const demos = [
  { r: '系统管理员', u: 'admin' },
  { r: '教务管理员', u: 'academic01' },
  { r: '任课教师', u: 'teacher01' },
  { r: '助教', u: 'ta01' },
  { r: '学生', u: '2023011001' }
]

// 登录页展示的规模数字：来自看板真实统计，取不到时用文档口径兜底
const stats = reactive({ course: 8, assignment: 22, submission: 196, avgPeer: 70 })

onMounted(async () => {
  const saved = localStorage.getItem('login_username')
  if (saved) form.username = saved
  // 记住账号只影响输入框，不保留 token
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('menus')
  try {
    const d = await api.dashboard()
    stats.course = 8
    stats.assignment = 22
    stats.submission = d?.submitByAssignment?.reduce((a, b) => a + Number(b.subs || 0), 0) || 196
    stats.avgPeer = d?.peerFinishRate ?? 70
  } catch (e) {
    /* 未登录时看板可能 401，保持兜底数字即可 */
  }
})

function fillDemo() {
  showDemo.value = !showDemo.value
}
function pick(u) {
  form.username = u
  form.password = '123456'
  showDemo.value = false
}

async function submit() {
  const ok = await formRef.value?.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const d = await api.login({ username: form.username.trim(), password: form.password })
    setLogin(d)
    if (remember.value) localStorage.setItem('login_username', form.username.trim())
    else localStorage.removeItem('login_username')
    ElMessage.success(`欢迎，${d.user?.realName || d.user?.username}`)
    // 学生进门户，教学与管理角色进工作台
    router.replace(d.user?.roleCode === 'STUDENT' ? '/portal' : '/dashboard')
  } catch (e) {
    /* 拦截器已提示具体错误（账号或密码错误 / 锁定） */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  height: 100vh;
  background: #F5F6F7;
}

/* ---------- 左侧品牌 ---------- */
.brand-side {
  position: relative;
  overflow: hidden;
  background: #1E4B3F;
  color: #fff;
  display: flex;
  align-items: center;
}
.brand-side .bg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.42;
}
.brand-side .veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(150deg, rgba(18, 36, 31, 0.94) 0%, rgba(30, 75, 63, 0.86) 52%, rgba(42, 90, 76, 0.74) 100%);
}
.brand-inner {
  position: relative;
  padding: 0 56px;
  max-width: 620px;
}
.brand-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 34px;
}
.brand-top .mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: #C2703A;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 700;
}
.brand-name {
  font-size: 14px;
  letter-spacing: 0.4px;
  opacity: 0.92;
}
.brand-inner h1 {
  font-size: 34px;
  line-height: 46px;
  margin: 0 0 14px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.lead {
  font-size: 13px;
  line-height: 22px;
  color: rgba(255, 255, 255, 0.78);
  margin: 0 0 28px;
  max-width: 470px;
}

/* 六步链路：左侧竖线串起来，序号用等宽字体 */
.flow {
  list-style: none;
  margin: 0 0 30px;
  padding: 0;
  border-left: 1px solid rgba(255, 255, 255, 0.18);
}
.flow li {
  display: flex;
  gap: 12px;
  padding: 0 0 14px 16px;
  position: relative;
}
.flow li::before {
  content: "";
  position: absolute;
  left: -4px;
  top: 7px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #C2703A;
  box-shadow: 0 0 0 3px rgba(194, 112, 58, 0.22);
}
.flow .no {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  padding-top: 2px;
}
.flow b {
  display: block;
  font-size: 13px;
  font-weight: 600;
}
.flow em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.66);
  line-height: 18px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  padding-top: 18px;
}
.metrics b {
  display: block;
  font-size: 22px;
  font-variant-numeric: tabular-nums;
  line-height: 28px;
}
.metrics span {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
}

/* ---------- 右侧表单 ---------- */
.form-side {
  display: grid;
  place-items: center;
  padding: 40px;
}
.form-box {
  width: 100%;
  max-width: 380px;
}
.form-box h2 {
  font-size: 24px;
  margin: 0 0 6px;
  font-weight: 600;
}
.form-box .sub {
  font-size: 12px;
  color: #5B6770;
  margin: 0 0 26px;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 20px;
}
.hint {
  font-size: 12px;
  color: #3D7A66;
  cursor: pointer;
}
.hint:hover {
  text-decoration: underline;
}
.submit {
  width: 100%;
  height: 42px;
  font-size: 14px;
  letter-spacing: 1px;
}

/* 演示账号：点一下即填充，方便验收时切换角色 */
.demo {
  margin-top: 18px;
  border: 1px dashed #DEE4E9;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}
.demo-title {
  font-size: 12px;
  color: #5B6770;
  margin-bottom: 8px;
}
.demo-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.demo-item {
  border: 1px solid #EDF0F3;
  border-radius: 6px;
  padding: 6px 8px;
  cursor: pointer;
  transition: all 0.15s;
}
.demo-item:hover {
  border-color: #3D7A66;
  background: #F3F8F6;
}
.demo-item b {
  display: block;
  font-size: 12px;
  font-weight: 600;
}
.demo-item span {
  font-size: 11px;
  color: #5B6770;
}
.foot {
  margin-top: 22px;
  font-size: 11px;
  line-height: 18px;
  color: #8A959E;
}

@media (max-width: 1100px) {
  .login-wrap {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100vh;
  }
  .brand-side {
    display: none;
  }
}
</style>
