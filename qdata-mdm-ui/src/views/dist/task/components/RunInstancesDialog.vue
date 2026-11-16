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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <el-dialog v-if="appendTo" v-model="visibleDialog" class="medium-dialog run-instances-dialog" :title="title"
             draggable destroy-on-close append-to-body :append-to="appendTo">
    <RunInstances v-if="visibleDialog && data.id != null" :task-id="data.id" :task-name="data.name || ''" :append-to="appendTo" />
    <template #footer>
      <div class="dialog-footer-right">
        <el-button @click="visibleDialog = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';
import RunInstances from './RunInstances.vue';

// 列表页只负责传入任务和控制弹窗开关。
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '任务实例' },
  data: { type: Object, default: () => ({}) },
  appendTo: { type: [Object, String], default: 'body' }
});
const emit = defineEmits(['update:visible']);
const visibleDialog = computed({
  get: () => props.visible,
  set: value => emit('update:visible', value)
});
</script>

<style scoped>
.run-instances-dialog :deep(.el-dialog__body) {
  padding: 20px 40px;
}

.run-instances-dialog :deep(.el-dialog__footer) {
  padding: 15px 20px;
}

.dialog-footer-right {
  text-align: right;
}
</style>
