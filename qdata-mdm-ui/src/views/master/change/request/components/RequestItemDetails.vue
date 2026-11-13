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
  <div class="h2-title row-full">申请明细</div>
  <div class="row-full">
    <qt-wrap :columns="itemsTableStore.columns" :config="itemsTableStore.config" :table-ref="itemsTableRef" class="mt10">
      <qt-table ref="itemsTableRef" v-bind="itemsTableStore">
        <template #masterRecordId="{ row }">{{ row.masterRecordId || idPlaceholder }}</template>
        <template #resultMasterRecordId="{ row }">{{ row.resultMasterRecordId || idPlaceholder }}</template>
        <template #changeType="{ row }">
          <dict-tag :value="row.changeType" :options="changeTypeOptions" />
        </template>
        <template #executeMessage="{ row }">
          {{ detail.approvalStatus === '0' ? '-' : row.executeMessage || '-' }}
        </template>
      </qt-table>
    </qt-wrap>
  </div>
  <template v-if="submodelChangeRows.length">
    <div class="h2-title row-full">子模型申请明细</div>
    <div class="row-full">
      <qt-wrap :columns="submodelTableStore.columns" :config="submodelTableStore.config" :table-ref="submodelTableRef" class="mt10">
        <qt-table ref="submodelTableRef" v-bind="submodelTableStore">
          <template #operation="{ row }">{{ submodelOperationLabel(row.operation) }}</template>
          <template #recordId="{ row }">{{ row.recordId || '保存后生成' }}</template>
          <template #values="{ row }">{{ formatSubmodelValues(row.values) }}</template>
        </qt-table>
      </qt-wrap>
    </div>
  </template>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps({
  detail: { type: Object, required: true },
  changeTypeOptions: { type: Array, default: () => [] },
  idPlaceholder: { type: String, default: '' }
})

const itemsTableRef = ref(null)
const submodelTableRef = ref(null)
const submodelChangeRows = computed(() => (props.detail.items || []).flatMap((item, index) =>
  parseSubmodelChanges(item.proposedData).map(change => ({
    ...change,
    masterItemNo: item.itemNo || index + 1
  }))
))

const itemsTableStore = {
  config: {
    initResquest: false,
    fullContent: false,
    notPagination: true,
    actions: { show: false },
    table: {
      class: 'field-design-table', stripe: true,
      rowStyle: { height: '48px' }, scrollbarAlwaysOn: false, border: false
    }
  },
  columns: [
    { prop: 'itemNo', label: '编号', width: 80, align: 'center', sortable: 'custom' },
    { prop: 'masterRecordId', label: '来源主数据 ID', minWidth: 150, align: 'left', slot: 'masterRecordId' },
    { prop: 'resultMasterRecordId', label: '结果主数据 ID', minWidth: 150, align: 'left', slot: 'resultMasterRecordId' },
    { prop: 'changeType', label: '变更类型', minWidth: 110, align: 'left', slot: 'changeType' },
    { prop: 'executeMessage', label: '执行结果', minWidth: 210, align: 'left', slot: 'executeMessage', showOverflowTooltip: { effect: 'light' } }
  ],
  func: async (params = {}) => {
    const rows = [...(props.detail.items || [])]
    if (params.orderByColumn === 'itemNo' && params.isAsc) {
      const direction = params.isAsc === 'descending' ? -1 : 1
      rows.sort((left, right) => direction * (left.itemNo - right.itemNo))
    }
    return { data: rows }
  }
}

const submodelTableStore = {
  config: {
    initResquest: false,
    fullContent: false,
    notPagination: true,
    actions: { show: false },
    table: {
      class: 'field-design-table', stripe: true,
      rowStyle: { height: '48px' }, scrollbarAlwaysOn: false, border: false
    }
  },
  columns: [
    { prop: 'masterItemNo', label: '主数据序号', width: 100, align: 'center' },
    { prop: 'submodelName', label: '子模型', minWidth: 150, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { prop: 'operation', label: '操作', width: 90, align: 'center', slot: 'operation' },
    { prop: 'recordId', label: '子数据 ID', minWidth: 140, align: 'left', slot: 'recordId' },
    { prop: 'values', label: '字段值', minWidth: 260, align: 'left', slot: 'values', showOverflowTooltip: { effect: 'light' } }
  ],
  func: async () => ({ data: submodelChangeRows.value })
}

watch(() => props.detail, async () => {
  await nextTick()
  itemsTableRef.value?.getList()
  submodelTableRef.value?.getList()
}, { immediate: true })

function parseSubmodelChanges(value) {
  if (!value) return []
  let payload = value
  if (typeof value === 'string') {
    try { payload = JSON.parse(value) } catch { return [] }
  }
  return Array.isArray(payload?.submodelChanges) ? payload.submodelChanges : []
}

function submodelOperationLabel(value) {
  return { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }[value] || value || '-'
}

function formatSubmodelValues(values) {
  if (!values || typeof values !== 'object' || !Object.keys(values).length) return '-'
  return Object.entries(values).map(([key, value]) => {
    const text = value != null && typeof value === 'object' ? JSON.stringify(value) : value
    return `${key}: ${text ?? ''}`
  }).join('；')
}
</script>
