<script setup lang="ts">
// 404 Not Found 页面
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="code">404</div>
    <p class="title">页面走丢了 · 让书签指引你回去</p>
    <p class="sub">您访问的页面可能已被移动或暂时下架</p>
    <el-button type="primary" round @click="router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px;
  background: var(--bg-gradient-page);
}

.code {
  font-size: 120px;
  font-weight: 800;
  background: var(--brand-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  line-height: 1;
  letter-spacing: -4px;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 24px 0 8px;
}

.sub {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 28px;
}
</style>
