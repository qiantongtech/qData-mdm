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
  <el-tabs class="model-data-tabs" :model-value="activeTab" @tab-change="$emit('change', $event)">
    <qt-tab-pane label="全部数据" name="all" />
    <qt-tab-pane v-for="status in ['1', '2', '3', '4']" :key="status" :name="status"
                 :label="selectDictLabel(statusOptions, status)" />
    <qt-tab-pane v-if="canViewApprovals" label="维护审批" name="approvals" />
  </el-tabs>
</template>

<script setup>
import { selectDictLabel } from '@/utils/anivia.js'

defineProps({
  activeTab: { type: String, required: true },
  statusOptions: { type: Array, required: true },
  canViewApprovals: Boolean
})
defineEmits(['change'])
</script>

<style scoped>
.model-data-tabs {
  width: 100%;
}
.model-data-tabs :deep(.el-tabs__header),
.model-data-tabs :deep(.el-tabs__nav-wrap) {
  margin-bottom: 0;
}
.model-data-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
</style>
