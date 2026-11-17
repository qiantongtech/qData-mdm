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

  Business License: https://docs.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <el-tag class="run-status-tag" :type="display.type" size="small" effect="light">
    <span class="run-status-tag__content">
      <el-icon :class="{ 'is-loading': status === 'RUNNING' }"><component :is="display.icon" /></el-icon>
      <span>{{ display.label }}</span>
    </span>
  </el-tag>
</template>

<script setup>
import { computed } from 'vue';
import { Clock, Loading, CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue';

// 沿用 Pro2 StatusTag 的图标加状态标签形式，适配分发实例状态。
const props = defineProps({
  status: { type: String, default: '' },
  successCount: { type: Number, default: 0 },
  failureCount: { type: Number, default: 0 }
});

const display = computed(() => {
  if (props.status === 'RUNNING') return { label: '运行中', type: 'primary', icon: Loading };
  if (props.status === 'SUCCESS') return { label: '成功', type: 'success', icon: CircleCheckFilled };
  if (['FAILED', 'FAILURE'].includes(props.status)) {
    return { label: props.successCount > 0 && props.failureCount > 0 ? '部分失败' : '失败', type: 'danger', icon: CircleCloseFilled };
  }
  return { label: '未运行', type: 'info', icon: Clock };
});
</script>

<style scoped>
.run-status-tag.el-tag {
  height: 24px;
  font-size: var(--qt-font-size-base) !important;
  font-weight: var(--qt-font-weight-regular);
}

.run-status-tag__content {
  display: inline-flex;
  align-items: center;
  gap: var(--qt-space-1);
  line-height: 20px;
}

.run-status-tag__content .el-icon {
  font-size: var(--qt-font-size-base);
}
</style>
