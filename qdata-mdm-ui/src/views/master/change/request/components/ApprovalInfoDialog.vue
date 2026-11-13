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
  <div ref="dialogContainer" class="app-container">
    <el-dialog v-if="dialogContainer" :model-value="modelValue" title="审批信息" width="800px"
               append-to-body :append-to="dialogContainer" draggable destroy-on-close
               class="dialog" @close="close"
    >
      <DescriptionsInfo
        :data="row || {}"
        :items="infoItems"
        :column="1"
        :border="true"
        placeholder="-"
      >
        <template #approvalStatus>
          <dict-tag v-if="hasValue(row?.MASTER_APPROVAL_STATUS)"
                    :options="approvalStatusOptions" :value="row.MASTER_APPROVAL_STATUS"
          />
          <span v-else>{{ approvalStatus }}</span>
        </template>
      </DescriptionsInfo>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="close">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MasterChangeApprovalInfoDialog">
import { computed, ref } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  row: { type: Object, default: null },
  approvalStatus: { type: String, default: '-' },
  changeType: { type: String, default: '-' },
  approvalStatusOptions: { type: Array, default: () => [] }
})

const infoItems = computed(() => [
  { label: '申请编号', key: 'MASTER_APPROVAL_REQUEST_NO' },
  { label: '审批状态', slot: 'approvalStatus' },
  { label: '变更类型', value: props.changeType },
  { label: '主数据ID', key: 'ID' }
])

const emit = defineEmits(['update:modelValue'])
const dialogContainer = ref(null)

function close() {
  emit('update:modelValue', false)
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}
</script>
