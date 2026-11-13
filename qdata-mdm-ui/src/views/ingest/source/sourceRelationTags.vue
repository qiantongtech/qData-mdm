<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This file is part of qData-mdm Master Data Platform (Open Source Edition).

  qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
  You may use qData-mdm for commercial purposes, but you may not remove, hide,
  modify, or replace the qData-mdm logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData-mdm as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<!-- 来源系统关联标签：按实际宽度显示两行，其余折叠。 -->
<template>
  <div ref="rootRef" class="source-relation-tags">
    <qt-tag-group
      class="relation-group"
      :class="{ 'has-overflow': visibleCount < items.length }"
      :items="items"
      :is-datasource="isDatasource"
      :max-count="visibleCount"
      type="none"
    />
    <div v-if="items.length" ref="measureRef" class="relation-measure" aria-hidden="true" inert>
      <qt-tag-group
        class="relation-group"
        :items="items"
        :is-datasource="isDatasource"
        :max-count="items.length"
        type="none"
      />
      <el-tag class="measure-more" disable-transitions>+{{ "8".repeat(String(Math.max(1, items.length - 1)).length) }}</el-tag>
    </div>
  </div>
</template>

<script setup name="SourceRelationTags">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import QtTagGroup from "@/components/QtTagGroup/index.vue";

const props = defineProps({
  items: { type: Array, default: () => [] },
  isDatasource: { type: Boolean, default: false },
});

const rootRef = ref(null);
const measureRef = ref(null);
const visibleCount = ref(props.items.length);
const moreWidth = ref(0);
const moreTagWidth = computed(() => `${moreWidth.value}px`);
const lastTagMaxWidth = computed(() => `calc(100% - ${moreWidth.value}px - var(--qt-space-1))`);
let resizeObserver;
let animationFrame;
let previousWidth = 0;

/** 按实际标签及折叠提示宽度装入两行，避免固定少显示一个标签。 */
function calculateVisibleCount() {
  const container = measureRef.value?.querySelector(".tag-container");
  if (!container) return;
  const width = container.getBoundingClientRect().width;
  if (!width) return;
  const gap = parseFloat(getComputedStyle(container).columnGap) || 0;
  const widths = Array.from(container.children, item => item.getBoundingClientRect().width);
  moreWidth.value = Math.ceil(measureRef.value.querySelector(".measure-more").getBoundingClientRect().width);

  const fitsTwoRows = (tagWidths) => {
    let rows = 1;
    let usedWidth = 0;
    for (const tagWidth of tagWidths) {
      const nextWidth = usedWidth ? usedWidth + gap + tagWidth : tagWidth;
      if (nextWidth > width) {
        rows++;
        usedWidth = tagWidth;
      } else {
        usedWidth = nextWidth;
      }
      if (rows > 2) return false;
    }
    return true;
  };

  if (fitsTwoRows(widths)) {
    visibleCount.value = widths.length;
    return;
  }
  let countToShow = 1;
  for (let count = 1; count < widths.length; count++) {
    const candidate = widths.slice(0, count);
    candidate[count - 1] = Math.min(candidate[count - 1], Math.max(1, width - moreWidth.value - gap));
    if (fitsTwoRows([...candidate, moreWidth.value])) {
      countToShow = count;
    } else {
      break;
    }
  }
  visibleCount.value = countToShow;
}

function scheduleCalculate() {
  nextTick(() => {
    const container = measureRef.value?.querySelector(".tag-container");
    const more = measureRef.value?.querySelector(".measure-more");
    if (container) resizeObserver?.observe(container);
    if (more) resizeObserver?.observe(more);
    cancelAnimationFrame(animationFrame);
    animationFrame = requestAnimationFrame(calculateVisibleCount);
  });
}

onMounted(() => {
  resizeObserver = new ResizeObserver((entries) => {
    if (entries.some(entry => entry.target !== rootRef.value || entry.contentRect.width !== previousWidth)) {
      previousWidth = rootRef.value.getBoundingClientRect().width;
      scheduleCalculate();
    }
  });
  resizeObserver.observe(rootRef.value);
  scheduleCalculate();
});

watch(() => props.items, scheduleCalculate, { deep: true, flush: "post" });

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  cancelAnimationFrame(animationFrame);
});
</script>

<style scoped lang="scss">
.source-relation-tags {
  position: relative;
  width: 100%;
  min-width: 0;
}

.relation-group {
  display: block;
  width: 100%;

  :deep(.tag-container) { gap: var(--qt-space-1); }
  :deep(.qt-tag-item) { min-width: 0; max-width: 100%; }
  :deep(.dict-tag-el) { max-width: 100%; }
  :deep(.el-tag__content) { display: inline-flex; min-width: 0; max-width: 100%; }
  :deep(.dict-tag-icon) { flex-shrink: 0; }
  :deep(.el-tag__content > span) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  :deep(.more) { flex-shrink: 0; width: v-bind(moreTagWidth); margin-left: 0; }

  &.has-overflow :deep(.qt-tag-item:nth-last-child(2)) {
    max-width: v-bind(lastTagMaxWidth);
  }
}

.relation-measure {
  position: absolute;
  top: 0;
  width: 100%;
  height: 0;
  overflow: hidden;
  visibility: hidden;
  pointer-events: none;
}
</style>
