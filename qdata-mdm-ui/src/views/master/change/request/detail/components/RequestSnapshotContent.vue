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
  <div v-if="kind === 'create' || detail.changeType === 'BATCH_IMPORT'" class="mb15">
    <qt-wrap :config="descriptionConfig">
      <template #actions-data><div class="h2-titles">基础信息</div></template>
      <el-form v-if="businessItems.length" :model="item.businessValues || {}" label-width="120px" class="column-form" @submit.prevent>
        <qt-form-item v-for="field in businessItems" :key="field.fieldCode" :label="field.label"
          :prop="field.fieldCode" :class="{ 'row-full': field.longText }"
        >
          <div class="form-readonly" :class="{ textarea: field.longText }">{{ displaySnapshotValue(field.value) }}</div>
        </qt-form-item>
      </el-form>
      <el-empty v-else description="暂无业务字段" />
    </qt-wrap>
  </div>
  <qt-wrap v-else :columns="changeTableStore.columns" :config="changeTableStore.config"
    :table-ref="changeTableRef" class="mb15"
  >
    <qt-table ref="changeTableRef" v-bind="changeTableStore">
      <template #beforeValue="{ row }">
        <dict-tag v-if="row.status && row.beforeValue != null && row.beforeValue !== ''" :value="row.beforeValue" :options="statusOptions" />
        <span v-else>{{ displayRequestFieldValue(detail, row.fieldCode, row.beforeValue) }}</span>
      </template>
      <template #afterValue="{ row }">
        <dict-tag v-if="row.status && row.afterValue != null && row.afterValue !== ''" :value="row.afterValue" :options="statusOptions" />
        <span v-else>{{ displayRequestFieldValue(detail, row.fieldCode, row.afterValue) }}</span>
      </template>
    </qt-table>
  </qt-wrap>

  <template v-for="group in submodelGroups" :key="group.id">
    <qt-wrap :columns="group.tableStore.columns" :config="group.tableStore.config"
      :table-ref="group.tableRef" class="mb15"
    >
      <template #actions-data><div class="h2-titles">{{ group.name }}</div></template>
      <qt-table :ref="el => group.tableRef = el" v-bind="group.tableStore">
        <template #businessValue="{ row, column_data }">
          {{ displaySnapshotValue(row.businessValues[column_data.fieldCode.toUpperCase()]) }}
        </template>
      </qt-table>
    </qt-wrap>
  </template>
</template>

<script setup>
import { computed, nextTick, reactive, ref, watch } from 'vue'

function getFieldDataType(column) {
  return String(column.dataType || '').trim().toUpperCase().replace(/\(.*\)$/, '')
}

function isAutomaticColumn(column) {
  return String(column.encodingMode || '').toUpperCase() === 'AUTOMATIC'
}

function getFieldControl(column) {
  const category = String(column.columnCategory || column.category || '').toUpperCase()
  if (category === 'SYSTEM' || column.businessField === false) return 'readonly'
  if (category === 'CLASS') return 'tree'
  if (category === 'RELATION') return 'relation'
  if (isAutomaticColumn(column)) return 'automatic'
  if (column.dictTypeId != null && column.dictTypeId !== '') return 'dictionary'
  if (isBooleanColumn(column)) return 'boolean'
  if (isDateColumn(column)) return 'date'
  if (isDateTimeColumn(column)) return 'datetime'
  if (/TEXT|CLOB/.test(getFieldDataType(column))) return 'textarea'
  return 'text'
}

function isLongText(column) { return getFieldControl(column) === 'textarea' }

function isBooleanColumn(column) { return /^(BOOL|BOOLEAN)$/.test(getFieldDataType(column)) }

function isDateColumn(column) { return getFieldDataType(column) === 'DATE' }

function isDateTimeColumn(column) { return /^(DATETIME|TIMESTAMP)$/.test(getFieldDataType(column)) }

function normalizeFieldFormValue(column, value) {
  const text = String(value ?? '')
  if (['dictionary', 'relation', 'tree'].includes(getFieldControl(column))) return text
  if (isBooleanColumn(column)) {
    if (value === true || text === '1' || text.toLowerCase() === 'true') return 'true'
    if (value === false || text === '0' || text.toLowerCase() === 'false') return 'false'
  }
  if (isDateColumn(column) && /^\d{4}-\d{2}-\d{2}(?:$|[T ])/.test(text)) return text.slice(0, 10)
  if (isDateTimeColumn(column) && /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(text)) {
    const dateTime = text.replace('T', ' ').slice(0, 19)
    return dateTime.length === 16 ? `${dateTime}:00` : dateTime
  }
  return text
}

function getSnapshotValue(values, code) {
  const key = Object.keys(values).find(key => key.toUpperCase() === String(code).toUpperCase())
  return key === undefined ? undefined : values[key]
}

function findSnapshotField(fields, code) {
  return fields.find(field => field.code?.toUpperCase() === code.toUpperCase())
}

function buildSnapshotBusinessItems(fields, values, fieldValueLabels = {}) {
  const items = fields.map(field => ({
    fieldCode: field.code,
    label: field.name || field.code,
    longText: isLongText(field),
    // 使用 value 而非 key，兼容业务字段编码中含有点号的历史快照。
    value: displayRequestFieldValue({ fieldValueLabels }, field.code, getSnapshotValue(values, field.code)),
    formatter: displaySnapshotValue
  }))
  // 弹窗内分类信息置顶，多行文本放到末尾，同组字段保持模型定义顺序。
  const priority = item => item.longText ? 2 : item.label === '所属产品分类' ? 0 : 1
  return items.sort((left, right) => priority(left) - priority(right))
}

function buildRequestChangeRows(detail, item) {
  if (!item || requestContentKind(detail) === 'create') return []
  if (requestContentKind(detail) === 'status') {
    if (item.beforeStatus == null && item.targetStatus == null) return []
    return [{ number: 1, fieldCode: 'STATUS', fieldName: '数据状态', status: true,
      beforeValue: item.beforeStatus, afterValue: item.targetStatus }]
  }
  const fields = detail.fields || []
  return buildFieldChangeRows(item, fields)
    .filter(row => !['ID', 'CATEGORY_ID', 'VALID_FLAG', 'DEL_FLAG'].includes(row.fieldCode.toUpperCase()) &&
      findSnapshotField(fields, row.fieldCode)?.category?.toUpperCase() !== 'SYSTEM')
    .map((row, index) => ({ ...row, number: index + 1 }))
}

function buildFieldChangeRows(item, fields = []) {
  if (!item) return []
  const before = snapshotValues(item.beforeData)
  const proposed = snapshotValues(item.proposedData)
  // 提交快照可能只包含所选字段，未提交的字段保持原值，不能视为清空。
  const rows = Object.entries(proposed).flatMap(([code, afterValue]) => {
    if (['submodelChanges', 'targetRecordId'].includes(code)) return []
    const field = findSnapshotField(fields, code)
    const beforeValue = getSnapshotValue(before, code)
    if (normalizeFieldFormValue(field || {}, beforeValue) === normalizeFieldFormValue(field || {}, afterValue)) return []
    return [{ fieldCode: code, fieldName: field?.name || code, beforeValue, afterValue }]
  })
  if (item.beforeStatus !== item.targetStatus && item.targetStatus != null) {
    rows.push({ fieldCode: 'STATUS', fieldName: '数据状态', beforeValue: item.beforeStatus,
      afterValue: item.targetStatus, status: true })
  }
  if (item.changeType === 'VERSION_RESTORE' && item.currentVersionNo !== item.targetVersionNo) {
    rows.push({ fieldCode: '_version', fieldName: '数据版本', beforeValue: item.currentVersionNo,
      afterValue: item.targetVersionNo })
  }
  return rows.map((row, index) => ({ ...row, number: index + 1 }))
}

const requestNumberCollator = new Intl.Collator('zh-CN', { numeric: true })

function sortRequestRows(rows, params = {}) {
  if (!params.orderByColumn || !['ascending', 'descending'].includes(params.isAsc)) return rows
  const direction = params.isAsc === 'descending' ? -1 : 1
  return [...rows].sort((left, right) => direction * requestNumberCollator.compare(
    String(left[params.orderByColumn] ?? ''), String(right[params.orderByColumn] ?? '')
  ))
}

function parseSnapshot(value) {
  if (!value) return {}
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch { return {} }
}

function snapshotValues(value) {
  const snapshot = parseSnapshot(value)
  return snapshot.values && typeof snapshot.values === 'object' && !Array.isArray(snapshot.values)
    ? snapshot.values : snapshot
}

function displaySnapshotValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function displayRequestFieldValue(detail, code, value) {
  const labels = getSnapshotValue(detail.fieldValueLabels || {}, code)
  return displaySnapshotValue(labels?.[String(value)] ?? value)
}

function requestBusinessFields(detail) {
  const fields = (detail.fields || []).filter(field => field.code && field.category?.toUpperCase() !== 'SYSTEM')
  if (detail.fields?.length) {
    return fields.sort((a, b) => (a.sortOrder ?? Number.MAX_SAFE_INTEGER) - (b.sortOrder ?? Number.MAX_SAFE_INTEGER))
  }
  // 历史模型配置不可用时仍展示申请保存的业务字段，以快照编码作为列名。
  const excluded = new Set(['ID', 'STATUS', 'CATEGORY_ID', 'VALID_FLAG', 'DEL_FLAG', 'TARGETRECORDID', 'SUBMODELCHANGES'])
  const codes = (detail.items || []).flatMap(item => [
    ...Object.keys(snapshotValues(item.beforeData)), ...Object.keys(snapshotValues(item.proposedData))
  ]).map(code => code.toUpperCase()).filter(code => !excluded.has(code))
  return [...new Set(codes)].map(code => ({ code, name: code }))
}

function requestContentKind(detail) {
  return { CREATE: 'create', UPDATE: 'update', BATCH_UPDATE: 'update', STATUS_CHANGE: 'status' }[detail.changeType] || 'changes'
}

function requestSubmodelGroups(item) {
  const changes = parseSnapshot(item?.proposedData).submodelChanges
  if (!Array.isArray(changes)) return []
  const groups = new Map()
  changes.forEach((change, index) => {
    if (!change || typeof change !== 'object' || !change.submodelId) return
    const id = String(change.submodelId)
    if (!groups.has(id)) groups.set(id, { id, name: change.submodelName || `子模型 ${id}`, fields: [], rows: [] })
    const group = groups.get(id)
    const configured = Array.isArray(change.fields) ? change.fields.filter(field => field?.code &&
      field.category?.toUpperCase() !== 'SYSTEM') : []
    const before = snapshotValues(change.beforeData)
    const values = snapshotValues(change.values)
    const fields = configured.length ? configured : requestBusinessFields({ items: [{ beforeData: before, proposedData: values }] })
    fields.forEach(field => {
      const existing = group.fields.find(saved => saved.code.toUpperCase() === field.code.toUpperCase())
      if (!existing) group.fields.push({ ...field })
      else if (configured.length && field.name) Object.assign(existing, field)
    })
    const merged = { ...Object.fromEntries(Object.entries(before).map(([code, value]) => [code.toUpperCase(), value])),
      ...Object.fromEntries(Object.entries(values).map(([code, value]) => [code.toUpperCase(), value])) }
    group.rows.push({
      snapshotKey: String(index), number: group.rows.length + 1, operation: change.operation,
      operationName: { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }[change.operation] || change.operation || '-',
      businessValues: merged, beforeData: before, proposedData: values
    })
  })
  return [...groups.values()].map(group => ({
    ...group,
    rows: group.rows.map(row => ({
      ...row,
      ...Object.fromEntries(group.fields.map(field => [
        `businessValues.${field.code.toUpperCase()}`, row.businessValues[field.code.toUpperCase()] ?? ''
      ]))
    }))
  }))
}

const props = defineProps({
  detail: { type: Object, required: true },
  item: { type: Object, required: true },
  statusOptions: { type: Array, default: () => [] }
})
const descriptionConfig = { fullContent: false, actions: { table: { show: false } } }
const kind = computed(() => requestContentKind(props.detail))
const businessItems = computed(() => buildSnapshotBusinessItems(requestBusinessFields(props.detail),
  props.item.businessValues || {}, props.detail.fieldValueLabels))
const changeRows = computed(() => buildRequestChangeRows(props.detail, props.item))
const fieldColumns = reactive([
  { prop: 'number', label: '编号', width: 80, sortable: 'custom' },
  { prop: 'fieldName', label: '字段名称', width: 180, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { prop: 'beforeValue', label: '修改前值', minWidth: 240, align: 'left', slot: 'beforeValue', showOverflowTooltip: { effect: 'light' } },
  { prop: 'afterValue', label: '修改后值', minWidth: 240, align: 'left', slot: 'afterValue', showOverflowTooltip: { effect: 'light' } }
])
const statusColumns = reactive(fieldColumns.map(column => ({
  ...column, label: { beforeValue: '变更前状态', afterValue: '变更后状态' }[column.prop] || column.label
})))
const changeColumns = computed(() => kind.value === 'status' ? statusColumns : fieldColumns)
const changeTableRef = ref(null)
const changeTableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    autoPagination: true,
    pagination: { notAutoScroll: true },
    actions: { show: false },
    table: { rowKey: 'number', stripe: true, border: false }
  },
  columns: changeColumns,
  func: async params => ({ data: sortRequestRows(changeRows.value.map(row => ({
    ...row, beforeValue: row.beforeValue ?? '', afterValue: row.afterValue ?? ''
  })), params) })
})
const submodelGroups = computed(() => {
  if (kind.value === 'status') return []
  return requestSubmodelGroups(props.item).map(group => {
    return reactive({
      ...group,
      tableRef: null,
      tableStore: {
        config: {
          initResquest: false,
          fullContent: false,
          notPagination: true,
          actions: { table: { search: false } },
          table: { rowKey: 'snapshotKey', stripe: true, border: false }
        },
        columns: [
          { prop: 'number', label: '编号', width: 80, sortable: 'custom' },
          ...group.fields.map(field => ({
            prop: `businessValues.${field.code.toUpperCase()}`, fieldCode: field.code,
            label: field.name || field.code, minWidth: 150, align: 'left', slot: 'businessValue',
            showOverflowTooltip: { effect: 'light' }
          })),
          ...(kind.value === 'create' || props.detail.changeType === 'BATCH_IMPORT'
            ? [] : [{ prop: 'operationName', label: '操作类型', width: 110 }])
        ],
        func: async params => ({ data: sortRequestRows(group.rows, params) })
      }
    })
  })
})

watch([changeRows, submodelGroups], async () => {
  await nextTick()
  changeTableRef.value?.resetQuery()
  submodelGroups.value.forEach(group => group.tableRef?.resetQuery())
}, { immediate: true })
</script>
