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

<!--
  按分页和排序展示合并结果的审批流转记录。
  @prop detail 合并管理详情数据。
  @prop active 当前页签是否激活。
  @author chentangwen
-->
<template>
  <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableWrapConfig">
    <template #actions-data>
      <GuideTipPanel
        type="plain"
        title="查看合并结果各次处理的操作人员、处理动作、意见、附件及时间，按记录了解审批过程，追溯结果的处理情况和流转经过。"
      />
    </template>
    <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" class="approval-record-table">
      <template #rowIndex="{ $index }">
        {{ (pageNum - 1) * pageSize + $index + 1 }}
      </template>
      <template #operation="{ row }">
        <el-tag size="small" :type="getOptionType(PENDING_LOG_ACTION_OPTIONS, row.actionType)">
          {{ getOperationLabel(row.actionType) }}
        </el-tag>
      </template>
      <template #reason="{ row }">{{ displayText(row.reason) }}</template>
      <template #attachments="{ row }">{{ displayText(row.attachments) }}</template>
      <template #operateTime="{ row }">{{ formatTime(row.operateTime) }}</template>
      <template #empty>
        <div class="emptyBg">
          <img :src="emptyDataImage" alt="" />
          <p>{{ loadError ? '审批记录加载失败，请刷新重试' : '暂无审批记录' }}</p>
        </div>
      </template>
    </qt-table>
  </qt-wrap>

</template>

<script setup name="MergeApprovalRecordTab">
import { useDict } from '@/utils/dict'
import emptyDataImage from '@/assets/common/images/empty-state/empty-nodata.png'
import { getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getMergePendingApprovalRecords } from '@/api/merge/manage'

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

function getOptionType(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.elTagType || 'info'
}

const { merge_pending_log_action: PENDING_LOG_ACTION_OPTIONS } = useDict('merge_pending_log_action')

/** 合并结果详情的审批记录页签，展示持久化的操作人与状态流转日志。 */
const props = defineProps({
  detail: { type: Object, default: null },
  active: { type: Boolean, default: false },
})

const { proxy } = getCurrentInstance()
const tableRef = ref()
const pageNum = ref(1)
const pageSize = ref(10)
const loadError = ref(false)
let requestVersion = 0

const tableWrapConfig = {
  fullContent: false,
  actions: { table: { search: false, refresh: true, columns: true } },
}

const tableStore = reactive({
  func: loadApprovalRecords,
  params: {},
  events: {
    onPageCurrentChange(params) { pageNum.value = params.pageNum },
    onPageSizeChange(params) {
      pageNum.value = params.pageNum
      pageSize.value = params.pageSize
    },
  },
  config: {
    initResquest: false,
    table: { rowKey: 'id', stripe: true },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50, 100],
      notAutoScroll: true,
    },
  },
  columns: [
    { label: '编号', prop: 'rowIndex', slot: 'rowIndex', sortable: 'custom', sortableKey: 'id', width: 70, minWidth: 70, align: 'center' },
    { label: '处理人', prop: 'operatorName', minWidth: 140, align: 'left', showOverflowTooltip: true },
    { label: '处理动作', prop: 'actionType', slot: 'operation', minWidth: 130, align: 'center' },
    { label: '处理意见', prop: 'reason', slot: 'reason', minWidth: 220, align: 'left', showOverflowTooltip: true },
    { label: '附件', prop: 'attachments', slot: 'attachments', minWidth: 160, showOverflowTooltip: true },
    { label: '处理时间', prop: 'operateTime', slot: 'operateTime', sortable: 'custom', sortableKey: 'operate_time', minWidth: 180, align: 'left' },
  ],
})

/** 仅在打开审批记录页签时请求当前待合并结果的操作日志。 */
watch(() => [props.detail?.id, props.active], async ([id, active]) => {
  const version = ++requestVersion
  loadError.value = false
  pageNum.value = 1
  if (!active || id == null) return
  await nextTick()
  if (version !== requestVersion) return
  tableRef.value?.resetQuery()
}, { immediate: true })

/**
 * 通过已存在的合并操作日志填充审批记录表，并保留后端分页总数。
 * @param {*} params 业务筛选、分页和排序参数。
 * @returns {Promise<Object>} 整理后的业务参数或展示数据。
 */
async function loadApprovalRecords(params) {
  const id = props.detail?.id
  if (!props.active || id == null) return { data: { rows: [], total: 0 } }
  const version = ++requestVersion
  loadError.value = false
  try {
    const response = await getMergePendingApprovalRecords(id, params)
    if (version !== requestVersion || String(id) !== String(props.detail?.id) || !props.active) {
      return { data: { rows: [], total: 0 } }
    }
    return { data: { rows: response?.data?.rows || [], total: response?.data?.total || 0 } }
  } catch (error) {
    if (version !== requestVersion || String(id) !== String(props.detail?.id) || !props.active) {
      return { data: { rows: [], total: 0 } }
    }
    loadError.value = true
    throw error
  }
}

/**
 * 将操作日志类型转换为简洁的表格动作文案。
 * @param {*} actionType 调用方传入的业务配置。
 * @returns {string} 用于页面展示的格式化文本。
 */
function getOperationLabel(actionType) {
  const operationMap = {
    '1': '调整',
    '2': '确认',
    '3': '提交',
    '4': '撤回',
    '5': '取消',
  }
  return operationMap[String(actionType)] || getOptionLabel(PENDING_LOG_ACTION_OPTIONS.value, actionType) || '-'
}

/**
 * 统一格式化操作时间。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}') || '-' : '-'
}

</script>

<style scoped>
.approval-record-table {
  margin-top: var(--qt-space-2);
}
</style>
