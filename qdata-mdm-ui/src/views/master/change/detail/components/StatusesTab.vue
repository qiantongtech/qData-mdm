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
  <div>
      <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableStore.config">
        <template #actions-data>
          <GuideTipPanel type="plain" title="本页用于追踪当前主数据的状态流转记录，展示状态变化、审批结果、变更原因及执行信息，便于了解主数据状态变更历史。" />
        </template>
        <el-alert v-if="statusError" :title="statusError" type="error" :closable="false" show-icon class="mb15" />
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #statusChange="{ row }">
            {{ statusLabel(row.beforeStatus) }} → {{ statusLabel(row.targetStatus) }}
          </template>
          <template #changeType="{ row }">
            <dict-tag :value="row.changeType" :options="master_change_type" />
          </template>
        </qt-table>
      </qt-wrap>
  </div>
</template>

<script setup>
import { reactive, ref, watch, nextTick } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getMasterChangeStatusRecords } from '@/api/master/change/request'
import { useDict } from '@/utils/dict'
// 状态记录由变更申请明细提供，沿用已验收的详情展示。

const props = defineProps({
  modelValue: Boolean,
  activeTab: { type: String, default: '' },
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null },
  statusOptions: { type: Array, default: () => [] }
})

const { master_change_type } = useDict('master_change_type')

const statusError = ref('')
const tableRef = ref(null)
const tableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    actions: { table: { search: false } }
  },
  columns: [
    { label: '编号', prop: 'itemId', width: 120, align: 'center', sortable: true, sortableKey: 'id' },
    { label: '审批单号', prop: 'requestNo', minWidth: 220, align: 'left' },
    { label: '状态变化', prop: 'beforeStatus', width: 190, align: 'left', slot: 'statusChange' },
    { label: '审批结果', prop: 'approvalStatus', width: 120, align: 'left', dict: 'master_change_request_status' },
    { label: '变更原因', prop: 'applyReason', minWidth: 180, align: 'left' },
    { label: '操作类型', prop: 'changeType', width: 150, align: 'left', slot: 'changeType' },
    { label: '申请人', prop: 'applicantName', width: 130, align: 'left' },
    { label: '执行时间', prop: 'executeTime', width: 190, align: 'left', date: true, sortable: 'custom' }
  ],
  params: {},
  func: async (params) => {
    const recordId = props.detail?.row?.ID
    if (!props.modelValue || props.activeTab !== 'statuses' || !props.instanceId || !recordId) {
      return { data: { rows: [], total: 0 } }
    }
    try {
      statusError.value = ''
      const response = await getMasterChangeStatusRecords(props.instanceId, recordId, params)
      return { data: { rows: response?.data?.rows || [], total: response?.data?.total || 0 } }
    } catch (error) {
      statusError.value = '状态记录加载失败，请稍后重试。'
      throw error
    }
  }
})

watch([
  () => props.modelValue,
  () => props.activeTab,
  () => props.instanceId,
  () => props.detail?.row?.ID
], async () => {
  statusError.value = ''
  await nextTick()
  tableRef.value?.resetQuery()
}, { immediate: true })

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function optionLabel(options, value) {
  if (!hasValue(value)) return '-'
  return options.find(option => String(option.value) === String(value))?.label || String(value)
}

function statusLabel(value) {
  return optionLabel(props.statusOptions, value)
}

</script>
