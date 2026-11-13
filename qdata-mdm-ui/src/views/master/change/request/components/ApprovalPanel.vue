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
  <section ref="dialogContainer">
    <qt-wrap
      :columns="columns"
      :table-ref="tableActions"
      :config="{ fullContent: props.fullContent, actions: { table: { search: true, refresh: true, columns: true } } }"
    >
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="queryParams"
          :table-ref="tableActions"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>
      <template #actions-data>
        <slot name="tabs" />
        <GuideTipPanel v-if="!$slots.tabs" type="default" no-border allow-wrap
                       title="以下展示当前模型已提交的维护申请及审批状态。"
        />
      </template>
      <GuideTipPanel v-if="$slots.tabs" type="default" no-border allow-wrap
                     title="以下展示当前模型已提交的维护申请及审批状态。" />
      <DataTable
        ref="tableRef"
        mode="approval"
        :columns="columns"
        :func="getList"
        :params="queryParams"
        :approval-status-options="approvalOptions"
        @view="openDetail"
      >
        <template #requestNo="{ row }">
          <el-button link type="primary" @click="openDetail(row)">{{ row.requestNo || '-' }}</el-button>
        </template>
        <template #changeType="{ row }">{{ changeTypeLabel(row.changeType) }}</template>
        <template #submitTime="{ row }">{{ formatApplicationTime(row.submitTime) }}</template>
        <template #approvalStatus="{ row }">
          <dict-tag v-if="hasValue(row.approvalStatus)"
                    :options="approvalOptions"
                    :value="row.approvalStatus"
          />
          <span v-else>-</span>
        </template>
        <template #action="{ row }">
          <el-button link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
        </template>
      </DataTable>
    </qt-wrap>

    <el-dialog
      v-if="dialogContainer"
      v-model="detailVisible"
      title="申请详情"
      width="1100px"
      append-to-body
      :append-to="dialogContainer"
      destroy-on-close
      draggable
      class="dialog"
    >
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">申请详情</span>
      </template>
      <div v-loading="detailLoading">
        <el-form :model="detail" label-width="130px" class="column-form approval-detail-form" @submit.prevent>
          <div class="h2-title row-full">基础信息</div>
          <qt-form-item
            v-for="item in detailInfoItems"
            :key="item.key || item.label"
            :label="item.label"
            :prop="item.key"
            :class="{ 'row-full': item.span === 2 }"
          >
            <div class="form-readonly" :class="{ textarea: item.slot === 'textarea' }">
              {{ detailInfoValue(item) }}
            </div>
          </qt-form-item>
          <RequestItemDetails :detail="detail" :change-type-options="changeTypeOptions" id-placeholder="-" />
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup name="MasterChangeApprovalPanel">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getMasterChangeApprovalDetail, getMasterChangeApprovals } from '@/api/master/change/request'
import { parseTime, selectDictLabel } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import RequestItemDetails from './RequestItemDetails.vue'
import DataTable from '../../../data/components/DataTable.vue'

const props = defineProps({
  instanceId: { type: [String, Number], required: true },
  treeId: { type: [String, Number], default: undefined },
  fullContent: { type: Boolean, default: false },
  approvalStatusOptions: { type: Array, default: null }
})
const { master_change_request_status, master_change_type } = useDict(
  'master_change_request_status', 'master_change_type'
)

const queryParams = reactive({ pageNum: 1, pageSize: 10, requestName: undefined, masterDataId: undefined, changeType: undefined, approvalStatus: undefined })
const searchStore = computed(() => ({ items: [
  { label: '申请名称', prop: 'requestName', component: { is: 'input' } },
  { label: '主数据ID', prop: 'masterDataId', component: { is: 'input' } },
  { label: '操作类型', prop: 'changeType', component: { is: 'select', options: master_change_type.value } },
  { label: '状态', prop: 'approvalStatus', component: { is: 'select', options: master_change_request_status.value } }
] }))
const tableRef = ref(null)
const dialogContainer = ref(null)
const columns = reactive([
  { prop: 'requestId', label: '编号', width: 60, align: 'center', headerAlign: 'center', sortable: true, sortableKey: 'id' },
  { prop: 'requestNo', label: '审批单号', minWidth: 170, align: 'left', slot: 'requestNo', showOverflowTooltip: { effect: 'light' } },
  { prop: 'changeType', label: '操作类型', minWidth: 120, align: 'left', slot: 'changeType' },
  { slot: 'requestName', prop: 'requestName', label: '申请名称', minWidth: 210, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { prop: 'targetSummary', label: '变更目标', width: 220, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { prop: 'approvalStatus', label: '状态', width: 120, align: 'center', headerAlign: 'center', slot: 'approvalStatus' },
  { prop: 'applicantName', label: '提交人', width: 120, align: 'center', headerAlign: 'center', showOverflowTooltip: { effect: 'light' } },
  {
    prop: 'submitTime',
    label: '提交时间',
    width: 150,
    align: 'center',
    headerAlign: 'center',
    slot: 'submitTime',
    sortable: true,
    sortableKey: 'submit_time',
    showOverflowTooltip: { effect: 'light' }
  },
  {
    label: '操作',
    width: 100,
    fixed: 'right',
    align: 'center',
    headerAlign: 'center',
    slot: 'action'
  }
])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref({ items: [] })

const changeTypeOptions = computed(() => master_change_type.value || [])
const approvalOptions = computed(() => (
  props.approvalStatusOptions?.length
    ? props.approvalStatusOptions
    : (master_change_request_status.value || [])
))
const detailInfoItems = computed(() => {
  const items = [
    { label: '申请编号', key: 'requestNo' },
    { label: '申请名称', key: 'requestName' },
    { label: '申请类型', key: 'changeType', formatter: changeTypeLabel },
    { label: '处理进度', key: 'approvalStatus', formatter: value => optionLabel(approvalOptions.value, value) },
    { label: '提交人', key: 'applicantName' },
    { label: '明细数', value: detail.value.itemCount ?? 0 },
    { label: '提交时间', value: detail.value.submitTime, formatter: formatApplicationTime },
    { label: '完成时间', value: detail.value.finishTime, formatter: formatApplicationTime },
    { label: '申请原因', key: 'applyReason', slot: 'textarea', span: 2 }
  ]
  if (detail.value.terminateReason) {
    items.push({ label: '终止原因', key: 'terminateReason', slot: 'textarea', span: 2 })
  }
  if (showExecuteMessage(detail.value)) {
    items.push({ label: '执行结果', key: 'executeMessage', slot: 'textarea', span: 2 })
  }
  return items
})

function getList(params) {
  if (!props.instanceId || !checkPermi(['master:instance:approval:list'])) {
    return Promise.resolve({ data: { rows: [], total: 0 } })
  }
  return getMasterChangeApprovals(props.instanceId, { ...params, treeId: props.treeId || undefined })
}

const tableActions = {
  getList() {
    if (Number(queryParams.pageNum || 1) === 1) {
      tableRef.value?.resetQuery()
      return
    }
    tableRef.value?.getList()
  },
  resetQuery() {
    tableRef.value?.resetQuery()
  }
}

function handleQuery() {
  queryParams.pageNum = 1
}

function resetQuery() {
  queryParams.requestName = undefined
  queryParams.masterDataId = undefined
  queryParams.changeType = undefined
  queryParams.approvalStatus = undefined
}

async function openDetail(row) {
  if (!checkPermi(['master:instance:approval:list'])) return
  detailVisible.value = true
  detailLoading.value = true
  detail.value = { requestNo: row.requestNo, items: [] }
  try {
    const response = await getMasterChangeApprovalDetail(props.instanceId, row.requestId)
    detail.value = response?.data || { items: [] }
  } finally {
    detailLoading.value = false
  }
}

function optionLabel(options, value) {
  return selectDictLabel(options || [], value) || value || '-'
}

function detailInfoValue(item) {
  const value = Object.hasOwn(item, 'value') ? item.value : detail.value?.[item.key]
  const formatted = item.formatter ? item.formatter(value) : value
  return hasValue(formatted) ? String(formatted) : '-'
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function changeTypeLabel(value) {
  return optionLabel(master_change_type.value, value)
}

function formatApplicationTime(value) {
  if (!hasValue(value)) return '-'
  const formatted = parseTime(value, '{y}-{m}-{d} {h}:{i}')
  return formatted && !formatted.startsWith('0-0-0') ? formatted : '-'
}

function showExecuteMessage(row) {
  return row?.approvalStatus !== '0' && Boolean(row?.executeMessage)
}

watch(() => props.instanceId, () => {
  queryParams.pageNum = 1
  columns.forEach(column => { column.hide = false })
  nextTick(() => tableRef.value?.getList())
})

watch(() => props.treeId, () => {
  queryParams.pageNum = 1
  nextTick(() => tableRef.value?.resetQuery())
})
</script>

<style scoped lang="scss">
.approval-detail-form :deep(.qt-wrap.mt10) {
  margin-top: 0 !important;
}
</style>
