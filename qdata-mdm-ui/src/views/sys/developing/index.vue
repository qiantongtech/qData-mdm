<template>
  <div :class="['app-container', { 'is-tab': type === 'tab' }]">
    <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
      <div class="developing-wrapper">
        <img
          src="@/assets/common/images/error/illust-building.png"
          :alt="td('sys.developing.underDevelopment')"
          class="developing-img"
        />
      </div>
    </qt-wrap>
  </div>
</template>

<script setup>
import useDefaultLang from '@/composables/useDefaultLang';
const { td } = useDefaultLang();

defineProps({
  type: {
    type: String,
    default: 'page'
  }
});
</script>

<style scoped>
.developing-wrapper {
  width: 100%;
  height: calc(100vh - 125px);
  background-color: var(--qt-surface-background);
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: var(--qt-radius-medium);
  overflow: hidden;
}

.developing-img {
  width: 100%;
  max-width: 500px;
  height: auto;
  object-fit: contain;
  display: block;
  margin-top: -46px;
}

.is-tab.app-container {
  padding: 0;
}

.is-tab .developing-wrapper {
  height: 500px;
}

.is-tab .developing-img {
  max-width: 400px;
  margin-top: 0;
}
</style>
