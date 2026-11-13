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
      <div>
        <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableStore.config">
          <template #actions-data>
            <GuideTipPanel type="plain" title="本页记录当前主数据的历史变更情况，展示版本变化、变更方式、关联申请及操作信息，便于追踪和追溯主数据的变更过程。" />
          </template>
          <el-alert v-if="changeError" :title="changeError" type="error" :closable="false" show-icon class="mb15" />
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #changeType="{ row }">
              <dict-tag :value="row.type" :options="master_change_type" />
            </template>
            <template #action="{ row }">
              <el-button link type="primary" icon="View" @click="openChangeSnapshot(row)">查看</el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </div>
    <ChangeSnapshotDialog
      ref="changeSnapshotDialogRef"
      :instance-id="instanceId"
      :master-data-id="detail?.row?.ID"
    />
  </div>
</template>

<script setup>
import { reactive, ref, watch, nextTick } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getMasterChangeList } from '@/api/master/change/change'
import ChangeSnapshotDialog from './ChangeSnapshotDialog.vue'
import { useDict } from '@/utils/dict'

const props = defineProps({
  modelValue: Boolean,
  activeTab: { type: String, default: '' },
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null }
})

const { master_change_type } = useDict('master_change_type')

const changeError = ref('')
const changeSnapshotDialogRef = ref(null)

const tableRef = ref(null)
const tableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    actions: { table: { search: false } }
  },
  columns: [
    { label: '编号', prop: 'id', width: 120, align: 'center', sortable: 'custom' },
    { label: '版本变化', prop: 'versionChange', width: 180, align: 'left' },
    { label: '变更方式', prop: 'type', minWidth: 160, align: 'left', slot: 'changeType' },
    { label: '关联申请编号', prop: 'requestNo', minWidth: 220, align: 'left' },
    { label: '操作人', prop: 'operatorName', minWidth: 140, align: 'left' },
    { label: '变更时间', prop: 'changeTime', width: 190, align: 'left', date: true, sortable: 'custom' },
    { label: '操作', prop: 'action', width: 90, align: 'center', slot: 'action', fixed: 'right' }
  ],
  params: {},
  func: async (params) => {
    const recordId = props.detail?.row?.ID
    if (!props.modelValue || props.activeTab !== 'changes' || !props.instanceId || !recordId) {
      return { data: { rows: [], total: 0 } }
    }
    changeError.value = ''
    try {
      const response = await getMasterChangeList(props.instanceId, recordId, params)
      const rows = (response?.data?.rows || []).map(row => ({
        ...row,
        versionChange: formatVersionChange(row)
      }))
      return { data: { rows, total: response?.data?.total || 0 } }
    } catch (error) {
      changeError.value = '变更记录加载失败，请稍后重试。'
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
  changeError.value = ''
  await nextTick()
  tableRef.value?.resetQuery()
}, { immediate: true })

function openChangeSnapshot(change) {
  changeSnapshotDialogRef.value?.openDialog({
    ...change,
    versionChange: change.versionChange || formatVersionChange(change)
  })
}

function formatVersionChange(change) {
  return `${formatVersionNo(change.beforeVersionNo)} → ${formatVersionNo(change.afterVersionNo)}`
}

function formatVersionNo(value) {
  if (value === undefined || value === null || value === '') return '-'
  return `V${String(value).replace(/^v/i, '')}`
}

</script>
