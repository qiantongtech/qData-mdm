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

<template>
  <div v-if="model.projectName || directory" class="qt-table-info flex-column-gap overflow-hidden">
    <div class="qt-table-info__main">
      <img :src="getProjectIcon(model.projectIcon)" alt="" class="info-icon" />
      <span class="text-ellipsis" :title="model.projectName">{{ model.projectName || '-' }}</span>
    </div>
    <div v-if="directory" class="text-ellipsis" :title="directory">
      <dict-tag size="small" type="primary" :value="directory" />
    </div>
  </div>
  <span v-else class="model-location-empty">-</span>
</template>

<script setup>
import { computed } from 'vue'
import { getProjectIcon } from '@/utils/imageUrl'

const props = defineProps({ model: { type: Object, default: () => ({}) } })
const directory = computed(() => props.model.layers?.at(-1)?.name || '')
</script>

<style scoped lang="scss">
.model-location-empty {
  color: var(--el-text-color-secondary);
}
</style>
