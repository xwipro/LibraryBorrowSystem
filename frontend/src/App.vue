<script setup lang="ts">
// 根组件 - 仅承载路由出口
</script>

<template>
  <RouterView v-slot="{ Component }">
    <transition name="fade-transform" mode="out-in">
      <component :is="Component" />
    </transition>
  </RouterView>
</template>
