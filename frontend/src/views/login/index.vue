<script setup lang="ts">
// ============================================
// 登录页 - 管理面板登录(无注册)
// ============================================
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store'
import { authApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImg = ref('')

const form = reactive({
  username: '',
  password: '',
  captcha: '',
  captchaKey: '',
  remember: true,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' },
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

// ======== 加载验证码 ========
async function loadCaptcha() {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
    form.captchaKey = 'local-' + Date.now()
    captchaImg.value = generateLocalCaptcha()
    return
  }
  try {
    const res = await authApi.getCaptcha(true)
    captchaImg.value = res.data.img
    form.captchaKey = res.data.key
  } catch {
    form.captchaKey = 'local-' + Date.now()
    captchaImg.value = generateLocalCaptcha()
  }
}

function generateLocalCaptcha(): string {
  const canvas = document.createElement('canvas')
  canvas.width = 120
  canvas.height = 40
  const ctx = canvas.getContext('2d')!
  ctx.fillStyle = '#eef4ff'
  ctx.fillRect(0, 0, 120, 40)
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let code = ''
  for (let i = 0; i < 4; i++) {
    const ch = chars[Math.floor(Math.random() * chars.length)]
    code += ch
    ctx.font = `${20 + Math.random() * 6}px Georgia`
    ctx.fillStyle = `hsl(${Math.floor(Math.random() * 60 + 210)}, 70%, 45%)`
    ctx.save()
    ctx.translate(20 + i * 24, 28)
    ctx.rotate((Math.random() - 0.5) * 0.5)
    ctx.fillText(ch, 0, 0)
    ctx.restore()
  }
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `hsla(${Math.floor(Math.random() * 360)}, 50%, 50%, 0.4)`
    ctx.beginPath()
    ctx.moveTo(Math.random() * 120, Math.random() * 40)
    ctx.lineTo(Math.random() * 120, Math.random() * 40)
    ctx.stroke()
  }
  ;(form as { __localCode?: string }).__localCode = code
  return canvas.toDataURL()
}

// ======== 登录 ========
async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: form.username,
        password: form.password,
        captcha: form.captcha,
        captchaKey: form.captchaKey,
      })
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } catch {
      loadCaptcha()
    } finally {
      loading.value = false
    }
  })
}

// ======== 忘记密码 ========
const forgotVisible = ref(false)
const forgotStep = ref<0 | 1 | 2>(0) // 0:验证账号 1:设新密码 2:完成
const forgotLoading = ref(false)
const forgotForm = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})
const forgotCodeSent = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const forgotRulesStep0: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}
const forgotRulesStep1: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, cb) => {
        if (value !== forgotForm.newPassword) {
          cb(new Error('两次输入的密码不一致'))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openForgot() {
  forgotStep.value = 0
  forgotForm.username = ''
  forgotForm.email = ''
  forgotForm.code = ''
  forgotForm.newPassword = ''
  forgotForm.confirmPassword = ''
  forgotCodeSent.value = false
  forgotVisible.value = true
}

function closeForgot() {
  forgotVisible.value = false
  if (countdownTimer) clearInterval(countdownTimer)
  countdown.value = 0
}

// 发送重置码
async function sendResetCode() {
  if (!forgotForm.username || !forgotForm.email) {
    ElMessage.warning('请先填写账号和邮箱')
    return
  }
  forgotLoading.value = true
  try {
    // 对接后端 POST /auth/forgot-password (按邮箱发送验证码)
    await authApi.sendResetCode({ email: forgotForm.email })
    forgotCodeSent.value = true
    countdown.value = 60
    countdownTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(countdownTimer!)
        countdownTimer = null
      }
    }, 1000)
    ElMessage.success('验证码已发送至您的邮箱, 5 分钟内有效')
  } catch {
    ElMessage.error('发送失败, 请稍后重试')
  } finally {
    forgotLoading.value = false
  }
}

async function forgotNext() {
  // step 0 → step 1
  if (forgotStep.value === 0) {
    const valid = await validateForgotStep(0)
    if (!valid) return
    if (!forgotCodeSent.value) {
      ElMessage.warning('请先获取重置码')
      return
    }
    if (!forgotForm.code || forgotForm.code.length < 4) {
      ElMessage.warning('请输入有效的重置码')
      return
    }
    forgotStep.value = 1
    return
  }
  // step 1 → step 2
  if (forgotStep.value === 1) {
    const valid = await validateForgotStep(1)
    if (!valid) return
    forgotLoading.value = true
    try {
      // 对接后端 POST /auth/reset-password (校验验证码并重置)
      await authApi.resetPassword({
        email: forgotForm.email,
        code: forgotForm.code,
        newPassword: forgotForm.newPassword,
      })
      forgotStep.value = 2
    } catch {
      ElMessage.error('重置失败, 请稍后重试')
    } finally {
      forgotLoading.value = false
    }
  }
}

function validateForgotStep(step: 0 | 1): Promise<boolean> {
  return new Promise((resolve) => {
    const el = document.querySelector<HTMLElement>(`[data-forgot-step="${step}"]`)
    if (!el) {
      // fallback: 简单校验
      resolve(true)
      return
    }
    // 用隐藏 el-form 做校验会比较麻烦, 这里直接手动校验
    let ok = true
    if (step === 0) {
      if (!forgotForm.username) ok = false
      if (!forgotForm.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(forgotForm.email)) ok = false
    } else {
      if (!forgotForm.newPassword || forgotForm.newPassword.length < 6) ok = false
      if (forgotForm.confirmPassword !== forgotForm.newPassword) ok = false
    }
    resolve(ok)
  })
}

function forgotDone() {
  closeForgot()
  ElMessage.success('密码已重置, 请使用新密码登录')
}

loadCaptcha()
</script>

<template>
  <div class="login-page">
    <!-- ============ 左侧视觉区 ============ -->
    <section class="visual-side">
      <div class="visual-grid" aria-hidden="true"></div>
      <div class="visual-glow" aria-hidden="true"></div>

      <div class="brand-block">
        <div class="brand-mark">
          <svg viewBox="0 0 48 48" width="44" height="44" aria-hidden="true">
            <defs>
              <linearGradient id="lgBrand" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#5990fc" />
                <stop offset="100%" stop-color="#3b82f6" />
              </linearGradient>
            </defs>
            <rect width="48" height="48" rx="12" fill="url(#lgBrand)" />
            <path d="M12 14h8a3 3 0 0 1 3 3v18a3 3 0 0 0-3-3h-8z" fill="#fff" />
            <path d="M36 14h-8a3 3 0 0 0-3 3v18a3 3 0 0 1 3-3h8z" fill="#fff" opacity="0.85" />
          </svg>
        </div>
        <div class="brand-name">智阅图书馆</div>
        <div class="brand-sub">SMART LIBRARY · 2026</div>
      </div>

      <!-- SVG 图书馆插画 -->
      <div class="illustration" aria-hidden="true">
        <svg viewBox="0 0 480 320" width="100%" height="100%" preserveAspectRatio="xMidYMid meet">
          <defs>
            <linearGradient id="book1" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#f5c542" />
              <stop offset="100%" stop-color="#d4a427" />
            </linearGradient>
            <linearGradient id="book2" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#5990fc" />
              <stop offset="100%" stop-color="#2563eb" />
            </linearGradient>
            <linearGradient id="book3" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#a5b4fc" />
              <stop offset="100%" stop-color="#818cf8" />
            </linearGradient>
            <linearGradient id="book4" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#fda4af" />
              <stop offset="100%" stop-color="#f43f5e" />
            </linearGradient>
            <linearGradient id="book5" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#6ee7b7" />
              <stop offset="100%" stop-color="#10b981" />
            </linearGradient>
            <linearGradient id="shelfGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#475569" />
              <stop offset="100%" stop-color="#1e293b" />
            </linearGradient>
          </defs>

          <rect x="60" y="80" width="34" height="170" rx="3" fill="url(#book2)" opacity="0.85" />
          <rect x="98" y="60" width="34" height="190" rx="3" fill="url(#book3)" />
          <rect x="136" y="90" width="34" height="160" rx="3" fill="url(#book4)" opacity="0.9" />
          <rect x="174" y="70" width="34" height="180" rx="3" fill="url(#book5)" />
          <rect x="212" y="100" width="34" height="150" rx="3" fill="url(#book1)" />
          <rect x="250" y="75" width="34" height="175" rx="3" fill="url(#book2)" opacity="0.9" />
          <rect x="288" y="85" width="34" height="165" rx="3" fill="url(#book3)" opacity="0.85" />
          <rect x="326" y="65" width="34" height="185" rx="3" fill="url(#book4)" />

          <g transform="rotate(-12 384 250)">
            <rect
              x="372"
              y="100"
              width="30"
              height="155"
              rx="3"
              fill="url(#book5)"
              opacity="0.95"
            />
          </g>

          <rect x="40" y="248" width="400" height="8" rx="2" fill="url(#shelfGrad)" />
          <rect x="40" y="256" width="400" height="3" rx="1.5" fill="rgba(0,0,0,0.4)" />

          <rect x="70" y="260" width="22" height="40" rx="2" fill="#fbbf24" />
          <rect x="95" y="260" width="22" height="40" rx="2" fill="#60a5fa" />
          <rect x="120" y="260" width="22" height="40" rx="2" fill="#a78bfa" />
          <rect x="145" y="260" width="22" height="40" rx="2" fill="#fb7185" />

          <circle cx="420" cy="120" r="3" fill="#f5c542" opacity="0.8" />
          <circle cx="440" cy="160" r="2" fill="#5990fc" opacity="0.6" />
          <circle cx="405" cy="180" r="2.5" fill="#fff" opacity="0.5" />
        </svg>
      </div>

      <blockquote class="quote">
        <p>「一座图书馆, 便是文明延续的灯塔。」</p>
        <footer>—— 智阅 · 阅读让生活更有厚度</footer>
      </blockquote>
    </section>

    <!-- ============ 右侧表单区 ============ -->
    <section class="form-side">
      <div class="form-card">
        <header class="form-head">
          <div class="head-badge">
            <el-icon><UserFilled /></el-icon>
            管理面板
          </div>
          <h1>欢迎回来</h1>
          <p>登录以开启今日的工作</p>
        </header>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="请输入验证码"
                maxlength="6"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
              <div class="captcha-img" @click="loadCaptcha" title="点击刷新验证码">
                <img v-if="captchaImg" :src="captchaImg" alt="验证码" />
                <span v-else>加载中</span>
              </div>
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="form.remember">记住我</el-checkbox>
            <button type="button" class="forgot-btn" @click="openForgot">忘记密码?</button>
          </div>

          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>
      </div>

      <footer class="form-footer">© 2026 智阅图书馆 · Library Controller Admin 2026</footer>
    </section>

    <!-- ============ 忘记密码弹窗 ============ -->
    <el-dialog
      v-model="forgotVisible"
      width="440px"
      :show-close="false"
      align-center
      class="forgot-dialog"
      append-to-body
    >
      <div class="forgot-head">
        <h3>重置密码</h3>
        <el-button text circle @click="closeForgot">
          <el-icon><Close /></el-icon>
        </el-button>
      </div>

      <!-- 步骤条 -->
      <el-steps :active="forgotStep" finish-status="success" align-center class="forgot-steps">
        <el-step title="验证身份" />
        <el-step title="设置新密码" />
        <el-step title="完成" />
      </el-steps>

      <!-- Step 0: 验证账号 -->
      <div v-if="forgotStep === 0" data-forgot-step="0">
        <el-form
          :model="forgotForm"
          :rules="forgotRulesStep0"
          ref="forgotStep0Ref"
          label-position="top"
        >
          <el-form-item label="账号" prop="username">
            <el-input v-model="forgotForm.username" placeholder="请输入管理员账号" />
          </el-form-item>
          <el-form-item label="注册邮箱" prop="email">
            <el-input v-model="forgotForm.email" placeholder="用于接收重置码" />
          </el-form-item>
          <el-form-item label="重置码" required>
            <div class="forgot-code-row">
              <el-input v-model="forgotForm.code" placeholder="输入 6 位重置码" maxlength="6" />
              <el-button
                :disabled="countdown > 0"
                :loading="forgotLoading"
                type="primary"
                plain
                @click="sendResetCode"
              >
                {{ countdown > 0 ? `${countdown}s 后重发` : '获取重置码' }}
              </el-button>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 1: 设新密码 -->
      <div v-if="forgotStep === 1" data-forgot-step="1">
        <el-form
          :model="forgotForm"
          :rules="forgotRulesStep1"
          ref="forgotStep1Ref"
          label-position="top"
        >
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="forgotForm.newPassword"
              type="password"
              show-password
              placeholder="至少 6 位"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="forgotForm.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入新密码"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 完成 -->
      <div v-if="forgotStep === 2" class="forgot-done">
        <div class="done-icon">
          <el-icon :size="56" color="#22c55e"><CircleCheckFilled /></el-icon>
        </div>
        <p class="done-text">密码重置成功</p>
        <p class="done-sub">请使用新密码登录管理面板</p>
      </div>

      <template #footer>
        <div class="forgot-footer">
          <template v-if="forgotStep < 2">
            <el-button @click="closeForgot">取消</el-button>
            <el-button type="primary" :loading="forgotLoading" @click="forgotNext">
              {{ forgotStep === 0 ? '下一步' : '确认重置' }}
            </el-button>
          </template>
          <template v-else>
            <el-button type="primary" class="w-full" @click="forgotDone">立即登录</el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  min-height: 100vh;
  width: 100%;
  background: var(--bg-page);
  transition: background-color 0.28s ease;
}

// ====== 左侧视觉区 ======
.visual-side {
  position: relative;
  background: var(--login-visual-bg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 48px 56px;
  color: var(--login-visual-text);
}

.visual-grid {
  position: absolute;
  inset: 0;
  background-image: var(--login-visual-grid);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
  -webkit-mask-image: radial-gradient(ellipse at center, #000 30%, transparent 80%);
}

.visual-glow {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: var(--login-visual-glow);
  top: -200px;
  right: -150px;
  filter: blur(60px);
  animation: drift 12s ease-in-out infinite alternate;
}

@keyframes drift {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(-30px, 40px) scale(1.1);
  }
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 2;

  .brand-mark {
    filter: drop-shadow(0 6px 16px rgba(59, 130, 246, 0.5));
    animation: rise 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
  }
}

.brand-name {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-sub {
  font-size: 11px;
  color: var(--login-visual-text-sub);
  letter-spacing: 3px;
  margin-top: 4px;
}

.illustration {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  position: relative;
  z-index: 2;
  animation: rise 0.9s 0.1s cubic-bezier(0.22, 1, 0.36, 1) both;

  :deep(svg) {
    max-width: 480px;
    width: 100%;
    filter: drop-shadow(0 20px 50px var(--login-visual-shadow));
  }
}

.quote {
  position: relative;
  z-index: 2;
  border-top: 1px solid var(--login-visual-line);
  padding-top: 24px;
  margin-top: 16px;
  animation: rise 1s 0.2s cubic-bezier(0.22, 1, 0.36, 1) both;

  p {
    font-family: 'Source Han Serif SC', 'Noto Serif SC', Georgia, serif;
    font-size: 18px;
    font-style: italic;
    color: var(--login-visual-text);
    margin: 0 0 10px;
    line-height: 1.6;
  }
  footer {
    font-size: 12px;
    color: var(--login-visual-text-sub);
    letter-spacing: 1px;
  }
}

// ====== 右侧表单区 ======
.form-side {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background: var(--bg-card);
  position: relative;
  transition: background-color 0.28s ease;
}

.form-card {
  width: 100%;
  max-width: 400px;
  animation: rise 0.8s 0.15s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.form-head {
  margin-bottom: 32px;

  .head-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 10px;
    font-size: 11px;
    font-weight: 600;
    color: $brand-primary;
    background: rgba(59, 130, 246, 0.08);
    border-radius: 100px;
    border: 1px solid rgba(59, 130, 246, 0.2);
    margin-bottom: 14px;
    letter-spacing: 1px;
  }

  h1 {
    font-size: 30px;
    font-weight: 700;
    color: var(--text-primary);
    margin: 0 0 8px;
    letter-spacing: 0.5px;
  }
  p {
    color: var(--text-secondary);
    font-size: 14px;
    margin: 0;
  }
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 20px;
  }
  :deep(.el-input__wrapper) {
    border-radius: $radius-md;
    padding: 4px 14px;
    box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.1) inset;
    transition: all 0.25s ease;

    &:hover {
      box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.4) inset;
    }
    &.is-focus {
      box-shadow:
        0 0 0 1.5px $brand-primary inset,
        0 4px 16px rgba(59, 130, 246, 0.15);
    }
  }
  :deep(.el-input__inner) {
    height: 44px;
    font-size: 14px;
    color: var(--text-primary);
  }
  :deep(.el-input__inner::placeholder) {
    color: var(--text-placeholder);
  }
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  overflow: visible;

  .el-input {
    flex: 1;
    min-width: 0;
  }
}

.captcha-img {
  width: 120px;
  height: 44px;
  border-radius: $radius-md;
  cursor: pointer;
  background: #eef4ff;
  border: 1px solid rgba(15, 23, 42, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  transition: all 0.2s ease;

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }
  &:hover {
    border-color: $brand-primary;
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.2);
  }
  span {
    font-size: 12px;
    color: $text-secondary;
  }
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 24px;
  font-size: 13px;
}

.forgot-btn {
  border: none;
  background: transparent;
  color: $brand-primary;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.75;
    text-decoration: underline;
  }
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $brand-primary 0%, $brand-primary-light 100%);
  border: none;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
  transition: all 0.25s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 30px rgba(59, 130, 246, 0.45);
  }
  &:active {
    transform: translateY(0);
  }
}

.form-footer {
  position: absolute;
  bottom: 24px;
  font-size: 12px;
  color: var(--text-placeholder);
  letter-spacing: 0.5px;
}

// ====== 忘记密码弹窗 - 白色主题 + 品牌蓝点缀 ======
.forgot-dialog {
  :deep(.el-dialog) {
    border-radius: 14px;
    background: var(--bg-card);
    box-shadow: 0 24px 64px rgba(15, 23, 42, 0.18);
    border-top: 3px solid $brand-primary;
    transition: background-color 0.28s ease;
  }
  :deep(.el-dialog__header) {
    display: none;
  }
  :deep(.el-dialog__body) {
    padding: 0;
  }
  :deep(.el-dialog__footer) {
    padding: 0 28px 28px;
    border-top: 1px solid var(--border-soft);
  }
}

.forgot-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-soft);
  transition: background-color 0.28s ease;

  h3 {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: var(--text-primary);
    letter-spacing: 0.5px;
  }

  .el-button {
    color: var(--text-secondary) !important;
    &:hover {
      color: var(--text-primary) !important;
      background: var(--bg-hover) !important;
    }
  }
}

.forgot-steps {
  margin: 20px 28px 24px;

  :deep(.el-step__title) {
    font-size: 13px;
  }
  :deep(.el-step.is-finish .el-step__icon),
  :deep(.el-step.is-finish + .el-step__line) {
    background-color: $brand-primary !important;
    border-color: $brand-primary !important;
  }
  :deep(.el-step.is-finish .el-step__title) {
    color: $brand-primary !important;
  }
  :deep(.el-step.is-process .el-step__icon) {
    background-color: $brand-primary !important;
    border-color: $brand-primary !important;
    color: #fff !important;
  }
  :deep(.el-step.is-process .el-step__title) {
    color: $brand-primary !important;
  }
}

.forgot-code-row {
  display: flex;
  gap: 10px;
  width: 100%;

  .el-input {
    flex: 1;
    min-width: 0;
  }

  :deep(.el-button--primary.is-plain) {
    background: linear-gradient(135deg, $brand-primary 0%, $brand-primary-light 100%);
    border: none;
    color: #fff !important;
    font-weight: 500;
    flex-shrink: 0;
    box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
    }
    &:disabled {
      background: linear-gradient(135deg, #94a3b8 0%, #94a3b8 100%);
      color: #fff !important;
      box-shadow: none;
    }
  }
}

.forgot-done {
  text-align: center;
  padding: 32px 0;

  .done-icon {
    margin-bottom: 12px;
  }
  .done-text {
    font-size: 18px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 6px;
  }
  .done-sub {
    font-size: 13px;
    color: var(--text-secondary);
    margin: 0;
  }
}

.forgot-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;

  :deep(.el-button--primary) {
    background: linear-gradient(135deg, $brand-primary 0%, $brand-primary-light 100%);
    border: none;
    box-shadow: 0 6px 18px rgba(59, 130, 246, 0.35);
    font-weight: 600;
  }
  :deep(.el-button--primary:hover) {
    transform: translateY(-1px);
    box-shadow: 0 10px 24px rgba(59, 130, 246, 0.45);
  }
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .visual-side {
    display: none;
  }
}
</style>
.

<style lang="scss">
// ============================================
// 登录页视觉区主题变量
// 亮色: 浅蓝亮色调  暗色: 深蓝品牌渐变
// ============================================
:root {
  --login-visual-bg: linear-gradient(135deg, #eef4ff 0%, #dbeafe 55%, #bfdbfe 100%);
  --login-visual-text: #1e3a8a;
  --login-visual-text-sub: rgba(30, 58, 138, 0.62);
  --login-visual-line: rgba(30, 58, 138, 0.14);
  --login-visual-grid:
    linear-gradient(rgba(30, 58, 138, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 58, 138, 0.06) 1px, transparent 1px);
  --login-visual-glow: radial-gradient(circle, rgba(59, 130, 246, 0.22) 0%, transparent 60%);
  --login-visual-shadow: rgba(30, 58, 138, 0.2);
}

html.dark,
html[data-theme='dark'] {
  --login-visual-bg:
    radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.35) 0%, transparent 60%),
    radial-gradient(circle at 80% 70%, rgba(212, 164, 39, 0.18) 0%, transparent 60%),
    linear-gradient(135deg, #0f172a 0%, #1e3a8a 60%, #1d4ed8 100%);
  --login-visual-text: #ffffff;
  --login-visual-text-sub: rgba(255, 255, 255, 0.55);
  --login-visual-line: rgba(255, 255, 255, 0.1);
  --login-visual-grid:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  --login-visual-glow: radial-gradient(circle, rgba(89, 144, 252, 0.5) 0%, transparent 60%);
  --login-visual-shadow: rgba(0, 0, 0, 0.4);
}
</style>
