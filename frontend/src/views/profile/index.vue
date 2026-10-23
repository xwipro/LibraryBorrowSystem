<script setup lang="ts">
// ============================================
// 后台 - 个人中心
// 数据源: authApi.userInfo() -> LoginUserInfo{ id,username,realName,avatar,roles,permissions }
// ============================================
defineOptions({ name: 'AdminProfile' })

import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import authApi from '@/api/modules/auth'
import type { LoginUserInfo } from '@/types/api'

// ===== 用户信息 =====
const loading = ref(false)
const loadError = ref('')
const profile = ref<LoginUserInfo | null>(null)

async function loadProfile() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await authApi.userInfo()
    profile.value = res.data
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '用户信息加载失败'
  } finally {
    loading.value = false
  }
}

// 无头像时取 realName(兜底 username)首字作为占位
function avatarText(): string {
  const name = profile.value?.realName || profile.value?.username || ''
  return name ? name.charAt(0).toUpperCase() : '?'
}

// ===== 修改密码(对接后端 POST /auth/change-password) =====
const formRef = ref<FormInstance>()
const submitting = ref(false)
const pwdForm = reactive({
  oldPwd: '',
  newPwd: '',
  confirmPwd: '',
})

function validateConfirm(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (value !== pwdForm.newPwd) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const pwdRules: FormRules = {
  oldPwd: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPwd: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPwd,
      newPassword: pwdForm.newPwd,
    })
    ElMessage.success('密码修改成功')
    pwdForm.oldPwd = ''
    pwdForm.newPwd = ''
    pwdForm.confirmPwd = ''
    formRef.value.clearValidate()
  } catch {
    // 失败提示由 request 拦截器统一弹出(旧密码错误/未登录等)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  void loadProfile()
})
</script>

<template>
  <div v-loading="loading" class="page-container profile-page">
    <h2 class="page-title">个人中心</h2>

    <!-- 加载失败 -->
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      title="用户信息加载失败"
      class="err-alert"
    >
      <template #default>
        <span>{{ loadError }}，请检查后端服务是否已启动。</span>
      </template>
    </el-alert>

    <!-- 账号信息 -->
    <div v-if="profile" class="card profile-card">
      <el-avatar :size="72" :src="profile.avatar" class="profile-avatar theme-avatar">
        {{ avatarText() }}
      </el-avatar>
      <div class="profile-meta">
        <div class="profile-name-row">
          <span class="profile-name">{{ profile.realName || profile.username }}</span>
          <el-tag v-for="role in profile.roles" :key="role" type="primary" effect="plain">
            {{ role }}
          </el-tag>
          <el-tag v-if="!profile.roles.length" type="info" effect="plain">暂无角色</el-tag>
        </div>
        <div class="profile-username">@{{ profile.username }}</div>
      </div>
    </div>

    <!-- 权限列表 -->
    <div v-if="profile" class="card">
      <h3 class="block-title">权限列表</h3>
      <div class="perm-tags">
        <el-tag
          v-if="profile.permissions.length === 1 && profile.permissions[0] === '*'"
          type="success"
        >
          全部权限
        </el-tag>
        <template v-else>
          <el-tag v-for="perm in profile.permissions" :key="perm" effect="plain" class="perm-tag">
            {{ perm }}
          </el-tag>
          <span v-if="!profile.permissions.length" class="empty-text">暂无权限</span>
        </template>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="card pwd-card">
      <h3 class="block-title">修改密码</h3>
      <el-form ref="formRef" :model="pwdForm" :rules="pwdRules" label-width="auto" class="pwd-form">
        <el-form-item label="旧密码" prop="oldPwd">
          <el-input
            v-model="pwdForm.oldPwd"
            type="password"
            show-password
            placeholder="请输入旧密码"
            autocomplete="off"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPwd">
          <el-input
            v-model="pwdForm.newPwd"
            type="password"
            show-password
            placeholder="请输入新密码（至少 6 位）"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPwd">
          <el-input
            v-model="pwdForm.confirmPwd"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.err-alert {
  margin-bottom: 4px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  flex-shrink: 0;
  font-size: 26px;
  font-weight: 600;
  background: var(--avatar-bg);
  color: var(--avatar-color);
}

.profile-meta {
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.profile-username {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.block-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  font-family: monospace;
}

.empty-text {
  color: var(--text-placeholder);
  font-size: 13px;
}

.pwd-form {
  max-width: 480px;
}
</style>
