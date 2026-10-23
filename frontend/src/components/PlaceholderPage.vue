<script setup lang="ts">
// ============================================
// 占位页面 - 后续逐步实现具体业务
// ============================================
defineProps<{ title?: string; hint?: string }>()
</script>

<template>
  <div class="placeholder">
    <el-empty :description="hint ?? `${title ?? '页面'} · 待实现`">
      <template #image>
        <div class="ph-icon">
          <svg viewBox="0 0 64 64" width="80" height="80" aria-hidden="true">
            <defs>
              <linearGradient id="phg" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#60a5fa" />
                <stop offset="100%" stop-color="#3b82f6" />
              </linearGradient>
            </defs>
            <rect
              x="6"
              y="10"
              width="52"
              height="44"
              rx="8"
              fill="none"
              stroke="url(#phg)"
              stroke-width="2.5"
              stroke-dasharray="4 4"
            />
            <path
              d="M20 26h24M20 34h24M20 42h14"
              stroke="url(#phg)"
              stroke-width="2.5"
              stroke-linecap="round"
            />
          </svg>
        </div>
      </template>
    </el-empty>
    <p class="hint">该功能将在后续迭代中逐步完善</p>
  </div>
</template>

<style scoped lang="scss">
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px;
}

.ph-icon {
  margin-bottom: 8px;
  animation: float 4s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

.hint {
  color: var(--text-placeholder);
  font-size: 13px;
  margin: 12px 0 0;
}
</style>
