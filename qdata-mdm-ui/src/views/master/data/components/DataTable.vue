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
  <qt-table :key="contextKey" ref="tableRef" v-bind="tableStore">
    <template #authorityValue="{ row, column_data: column }">
      <span
        v-if="column.nameField && !isApprovalMode && getNameTitle(row, column) && $auth.hasPermi('master:instance:query')"
        role="link" tabindex="0" class="pointer"
        :title="getNameTitle(row, column)"
        @click.stop="$emit('view', row)"
        @keydown.enter.stop.prevent="$emit('view', row)"
        @keydown.space.stop.prevent="$emit('view', row)"
      >{{ displayCellValue(row[column.prop], column) }}</span>
      <span v-else-if="column.nameField && !isApprovalMode" :title="getNameTitle(row, column)">{{ getNameTitle(row, column) ?? '-' }}</span>
      <span v-else>{{ displayCellValue(row[column.prop], column) }}</span>
    </template>

    <template #approvalStatus="{ row }">
      <slot v-if="isApprovalMode" name="approvalStatus" :row="row">
        <dict-tag v-if="hasValue(row.approvalStatus)"
                  :options="approvalStatusOptions" :value="row.approvalStatus"
        />
        <span v-else>-</span>
      </slot>
    </template>

    <template #requestName="{ row }">
      <span
        v-if="getDisplayTitle(row.requestName)"
        role="link" tabindex="0" class="pointer"
        :title="String(row.requestName)"
        @click.stop="$emit('view', row)"
        @keydown.enter.stop.prevent="$emit('view', row)"
        @keydown.space.stop.prevent="$emit('view', row)"
      >{{ row.requestName }}</span>
      <span v-else>-</span>
    </template>

    <template #requestNo="{ row }">
      <slot v-if="isApprovalMode" name="requestNo" :row="row">
        <span>{{ row.requestNo || '-' }}</span>
      </slot>
    </template>

    <template #changeType="{ row }">
      <slot v-if="isApprovalMode" name="changeType" :row="row">
        <span>{{ row.changeType || '-' }}</span>
      </slot>
    </template>

    <template #submitTime="{ row }">
      <slot v-if="isApprovalMode" name="submitTime" :row="row">
        <span>{{ row.submitTime || '-' }}</span>
      </slot>
    </template>

    <template #action="{ row }">
      <slot v-if="isApprovalMode" name="action" :row="row">
        <el-button link type="primary" icon="View" @click="$emit('view', row)">
          详情
        </el-button>
      </slot>
      <template v-else>
        <el-space :size="12" alignment="center">
          <el-button v-if="checkPermi(['master:instance:query'])" link type="primary" icon="view" @click="$emit('view', row)">
            详情
          </el-button>
          <el-button
            v-if="checkPermi(['master:instance:edit'])"
            link
            type="primary"
            icon="Edit"
            :disabled="!canModify(row)"
            @click="$emit('modify', row)"
          >修改</el-button>
          <el-popover v-if="hasMoreActions" placement="bottom" :width="120" trigger="click"
                      popper-class="table-action-popover handle-popover"
          >
            <template #reference>
              <el-button link type="primary" icon="ArrowDown">更多</el-button>
            </template>
            <div class="butgdlist task-action-list">
              <el-button
                v-if="isTreeMode && checkPermi(['master:instance:add'])"
                link
                type="primary"
                icon="Plus"
                :disabled="!canModify(row)"
                @click="$emit('addChild', row)"
              >新增子级</el-button>
              <el-button
                v-if="checkPermi(['master:instance:add'])"
                link
                type="primary"
                icon="DocumentCopy"
                :disabled="!canCopy(row)"
                @click="$emit('copy', row)"
              >复制</el-button>
              <el-button
                v-for="action in allStatusActions()"
                :key="action.code"
                link
                type="primary"
                :icon="action.icon"
                :disabled="!canStatusAction(row, action.code)"
                @click="$emit('status', row, action.code)"
              >
                {{ action.label }}
              </el-button>
            </div>
          </el-popover>
        </el-space>
      </template>
    </template>
  </qt-table>
</template>

<script setup>
import { computed, ref } from 'vue'
import { checkPermi } from '@/utils/permission'
import { useDict } from '@/utils/dict'
import { getDisplayTitle, handleTree, selectDictLabel } from '@/utils/anivia.js'
import { CircleClose, FolderChecked, Lock, RefreshLeft, Unlock } from '@element-plus/icons-vue'

function getReferenceDisplayValue(value, column) {
  if (value === null || value === undefined || value === '') return value
  const label = column.valueLabels?.[String(value)]
    ?? column.referenceOptions?.find(option => String(option.value) === String(value))?.label
  return label === null || label === undefined || label === '' ? value : label
}

// 与后端 MasterLifecycleRules 保持一致，归档为终态。
const LIFECYCLE_TRANSITIONS = {
  '1': { FREEZE: '2', INVALIDATE: '3' },
  '2': { UNFREEZE: '1', INVALIDATE: '3' },
  '3': { RECOVER: '1', ARCHIVE: '4' }
}

function getLifecycleTarget(status, action) {
  return Object.hasOwn(LIFECYCLE_TRANSITIONS, status) && Object.hasOwn(LIFECYCLE_TRANSITIONS[status], action)
    ? LIFECYCLE_TRANSITIONS[status][action] : ''
}

function hasPendingMaintenance(row) {
  if (!row.MASTER_APPROVAL_REQUEST_ID) return false
  const approval = String(row.MASTER_APPROVAL_STATUS)
  return approval === '0' || (approval === '1' && ['0', '1'].includes(String(row.MASTER_EXECUTE_STATUS)))
}

const tableRef = ref(null)
const { master_change_type } = useDict('master_change_type')
const props = defineProps({
  contextKey: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  func: { type: Function, required: true },
  params: { type: Object, default: () => ({}) },
  mode: { type: String, default: 'data' },
  tree: { type: Boolean, default: false },
  defaultExpandAll: Boolean,
  statusOptions: { type: Array, default: () => [] },
  approvalStatusOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['selectionChange', 'view', 'modify', 'copy', 'status', 'addChild'])
const isApprovalMode = computed(() => props.mode === 'approval')
const hasMoreActions = computed(() => checkPermi(['master:instance:add', 'master:instance:status']))
const hasBatchActions = computed(() => checkPermi(['master:instance:edit', 'master:instance:status']))
const hasRowActions = computed(() => isApprovalMode.value || checkPermi([
  'master:instance:query', 'master:instance:edit', 'master:instance:add', 'master:instance:status'
]))
const isTreeMode = computed(() => props.tree && !isApprovalMode.value)
const displayColumns = computed(() => {
  const columns = props.columns.filter(column => column.visible !== false && (!isApprovalMode.value || column.slot !== 'action'))
  if (!isTreeMode.value) return columns
  // Element Plus 在第一个普通列绘制树节点的缩进与展开箭头。
  return [...columns.filter(column => column.nameField), ...columns.filter(column => !column.nameField)]
})

const tableColumns = computed(() => [
  ...(isApprovalMode.value || !hasBatchActions.value ? [] : [
    { type: 'selection', width: 55, align: 'center', selectable }
  ]),
  ...displayColumns.value
    .map(column => {
      const prop = column.prop || column.field
      const date = dateConfig(column)
      const isIdColumn = String(prop).toUpperCase() === 'ID'
      const width = isIdColumn ? 60
        : (column.width || defaultColumnWidth(column))
      const creatorColumn = isCreatorColumn(column)
      const list = column.list || (creatorColumn ? [
        { prop: 'CREATED_BY_NAME' },
        { prop: 'CREATED_BY_PHONE' }
      ] : undefined)
      return {
        ...column,
        // 创建人完全由公共 list 能力渲染，避免原字段为空时再多显示一个占位符。
        prop: creatorColumn ? undefined : prop,
        label: ['STATUS', 'MASTER_APPROVAL_STATUS', 'APPROVALSTATUS'].includes(String(prop).toUpperCase())
          ? '状态' : (column.label || column.name),
        list,
        width,
        minWidth: isIdColumn ? 60
          : (width ? column.minWidth : (column.minWidth || 140)),
        align: isIdColumn ? 'center' : (isTreeMode.value && column.nameField)
          ? 'left' : (column.align || (isCenteredColumn(column) ? 'center' : 'left')),
        date: isCreationTimeColumn(column) ? date : (column.date || date),
        sortable: sortableColumn(column) ? 'custom' : false,
        sortableKey: sortableColumn(column) ? (column.sortableKey || prop) : undefined,
        showOverflowTooltip: column.showOverflowTooltip ?? { effect: 'light' },
        slot: column.slot || (list || column.dict || date ? undefined : 'authorityValue')
      }
    }),
    ...(hasRowActions.value ? [{
      label: "操作",
      width: isApprovalMode.value ? 100 : 240,
      slot: "action",
      fixed: "right"
    }] : [])
])

const tableStore = computed(() => ({
  config: {
    notPagination: isTreeMode.value,
    notPaginationParams: isTreeMode.value,
    table: {
      rowKey: isApprovalMode.value ? 'requestId' : 'ID',
      stripe: true,
      ...(isApprovalMode.value ? {
        tableLayout: 'fixed'
      } : {}),
      ...(isTreeMode.value ? {
        defaultExpandAll: props.defaultExpandAll,
        treeProps: { children: 'children', hasChildren: 'hasChildren' }
      } : {}),
      onSelectionChange: selection => emit('selectionChange', selection)
    },
    pagination: {
      pageSizes: [10, 20, 50]
    }
  },
  columns: tableColumns.value,
  func: props.func,
  params: props.params,
  events: {
    formatData: data => isTreeMode.value ? handleTree(data || [], 'ID', 'CATEGORY_ID') : data,
    onPageSizeChange: params => Object.assign(props.params, params),
    onPageCurrentChange: params => Object.assign(props.params, params)
  }
}))

function clearSelection() {
  tableRef.value?.elTableRef?.clearSelection()
}

function getList() {
  if (Number(props.params.pageNum || 1) === 1) {
    tableRef.value?.resetQuery()
    return
  }
  tableRef.value?.getList()
}

function resetQuery() {
  tableRef.value?.resetQuery()
}

function reload() {
  tableRef.value?.reload()
}

defineExpose({ clearSelection, getList, resetQuery, reload })

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function getNameTitle(row, column) {
  return getDisplayTitle(displayCellValue(row[column.prop], column))
}

function displayCellValue(value, column) {
  if (!hasValue(value)) return ''
  const displayValue = getReferenceDisplayValue(value, column)
  if (String(column.prop || column.field || '').toUpperCase() === 'DATA_SOURCE') {
    switch (displayValue) {
      case 'MANUAL': return '平台录入'
      case 'IMPORT': return '批量导入'
      case 'MERGE': return '多源融合'
    }
  }
  return displayValue
}

function isCreatorColumn(column) {
  const prop = String(column.prop || column.field || '').toUpperCase()
  const label = String(column.label || column.name || '').trim()
  return label === '创建人' || prop === 'CREATED_BY' || prop === 'CREATE_BY'
}

function sortableColumn(column) {
  if (column.businessField === true) return false
  const prop = String(column.prop || column.field || '').toUpperCase()
  if (isCreatorColumn(column) || ['DATA_SOURCE', 'STATUS'].includes(prop)) return false
  return column.sortable === true || ['ID', 'CREATED_TIME', 'CREATE_TIME'].includes(prop)
    || (isTreeMode.value && prop === 'SORT_ORDER')
}

function isCenteredColumn(column) {
  const prop = String(column.prop || column.field || '').toUpperCase()
  const label = String(column.label || column.name || '').trim()
  return label === '编号' ||
    prop === 'ID' ||
    label === '创建时间' ||
    prop === 'CREATED_TIME' ||
    prop === 'CREATE_TIME' ||
    label === '审核时间' ||
    label === '审批时间' ||
    /(AUDIT|APPROVAL).*(TIME|DATE)/.test(prop)
}

function dateConfig(column) {
  if (isCreationTimeColumn(column)) return '{y}-{m}-{d} {h}:{i}'
  const getFieldDataType = String(column.dataType || '').trim().toUpperCase()
  if (getFieldDataType === 'DATE') return '{y}-{m}-{d}'
  return getFieldDataType.includes('TIME') ? true : undefined
}

function isCreationTimeColumn(column) {
  return ['CREATED_TIME', 'CREATE_TIME'].includes(String(column.prop || column.field || '').toUpperCase())
}

function defaultColumnWidth(column) {
  const prop = String(column.prop || column.field || '').toUpperCase()
  const label = String(column.label || column.name || '').trim()
  if (prop === 'ID') return 60
  if (label === '编号') return 80
  if (dateConfig(column)) return 150
  if (column.dict || prop === 'MASTER_VERSION_NO') return 110
  return undefined
}

function canModify(row) {
  return String(row.STATUS) === '1' && !hasPendingMaintenance(row)
}

function canCopy(row) {
  return !hasPendingMaintenance(row)
}

function selectable(row) {
  return !hasPendingMaintenance(row)
}

function allStatusActions() {
  if (!checkPermi(['master:instance:status'])) return []
  const actions = ['FREEZE', 'INVALIDATE', 'UNFREEZE', 'RECOVER', 'ARCHIVE']
  const icons = {
    FREEZE: Lock,
    INVALIDATE: CircleClose,
    UNFREEZE: Unlock,
    RECOVER: RefreshLeft,
    ARCHIVE: FolderChecked
  }
  return actions.map(code => ({
    code,
    label: selectDictLabel(master_change_type.value || [], code) || code,
    icon: icons[code]
  }))
}

function canStatusAction(row, code) {
  return !hasPendingMaintenance(row) && Boolean(getLifecycleTarget(row.STATUS, code))
}
</script>
