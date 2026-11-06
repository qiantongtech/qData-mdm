<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This document is part of the qData Master Data Platform (open source version).

  qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
  You may use qData for commercial purposes, but you may not remove, hide,
  modify, or replace the qData master data platform logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData master data platform as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <el-tooltip
    effect="dark"
    :content="text"
    placement="top-start"
    :disabled="!isOverflow"
  >
    <span ref="textRef" class="overflow-text" :style="{ maxWidth }">{{ text }}</span>
  </el-tooltip>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, onBeforeUnmount } from 'vue';

const props = defineProps({
  text: { type: String, required: true },
  maxWidth: { type: String, default: '200px' } // Must bring unit
});

const textRef = ref(null);
const isOverflow = ref(false);

// Measure whether text overflows
const measure = () => {
  if (textRef.value) {
    isOverflow.value = textRef.value.scrollWidth > textRef.value.offsetWidth;
  }
};

// Initialization and window resize
onMounted(() => {
  nextTick(() => requestAnimationFrame(measure));
  window.addEventListener('resize', measure);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', measure);
});

// Listen for text or maxWidth changes
watch([() => props.text, () => props.maxWidth], () => {
  nextTick(() => requestAnimationFrame(measure));
});
</script>

<style scoped>
.overflow-text {
  display: inline-block;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  vertical-align: middle;
}
</style>
