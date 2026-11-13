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
            <GuideTipPanel type="plain" title="本页展示当前主数据关联的维护申请记录，包括操作类型、提交时间、完成时间及审批状态，支持查看申请详情。" />
          </template>
          <el-alert v-if="recordApprovalError" :title="recordApprovalError" type="error" :closable="false" show-icon class="mb15" />
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #changeType="{ row }">
              <dict-tag :value="row.changeType" :options="master_change_type" />
            </template>
            <template #approvalStatus="{ row }">
              <dict-tag v-if="hasValue(row.approvalStatus)"
                        :options="approvalStatusOptions"
                        :value="row.approvalStatus"
              />
              <span v-else>-</span>
            </template>
            <template #action="{ row }">
              <el-button link type="primary" icon="View" @click="loadRecordApprovalDetail(row)">查看</el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </div>

      <div ref="dialogContainer">
        <el-dialog
          v-if="dialogContainer"
          v-model="dialogVisible"
          title="审批记录详情"
          width="1100px"
          append-to-body
          :append-to="dialogContainer"
          destroy-on-close
          draggable
          class="dialog"
        >
          <template #header>
            <span role="heading" aria-level="2" class="el-dialog__title">审批记录详情</span>
          </template>
          <el-empty v-if="recordApprovalDetailError" :description="recordApprovalDetailError" />
          <div v-else v-loading="recordApprovalDetailLoading">
            <el-form :model="recordApprovalDetail || {}" label-width="150px" class="column-form approval-record-form" @submit.prevent>
              <div class="h2-title row-full">基础信息</div>
              <qt-form-item v-for="item in detailInfoItems" :key="item.key || item.label"
                            :label="item.label" :prop="item.key" :class="{ 'row-full': item.span === 2 }"
              >
                <div class="form-readonly" :class="{ textarea: item.slot === 'textarea' }">{{ detailInfoValue(item) }}</div>
              </qt-form-item>

              <div class="h2-title row-full">申请明细</div>
              <div class="row-full">
                <qt-wrap v-if="recordApprovalDetail" :columns="approvalItemsTableStore.columns"
                         :config="approvalItemsTableStore.config" :table-ref="approvalItemsTableRef" class="mt10"
                >
                  <qt-table ref="approvalItemsTableRef" v-bind="approvalItemsTableStore">
                    <template #changeType="{ row }">
                      <dict-tag :value="row.changeType" :options="master_change_type" />
                    </template>
                    <template #lifecycle="{ row }">{{ statusChangeLabel(row) }}</template>
                    <template #version="{ row }">{{ versionChangeLabel(row) }}</template>
                    <template #execute="{ row }">
                      {{ itemExecuteLabel(row.executeStatus) }}{{ row.executeMessage ? `：${row.executeMessage}` : '' }}
                    </template>
                  </qt-table>
                </qt-wrap>
              </div>
            </el-form>
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="dialogVisible = false">关 闭</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, onUnmounted, nextTick } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getMasterChangeRecordApprovals, getMasterChangeRecordApprovalDetail } from '@/api/master/change/request'
import { useDict } from '@/utils/dict'
import { parseTime, selectDictLabel } from '@/utils/anivia'
// 主数据详情按记录归属查询关联的变更申请和审批明细。

const props = defineProps({
  modelValue: Boolean,
  activeTab: { type: String, default: '' },
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null },
  statusOptions: { type: Array, default: () => [] },
  approvalStatusOptions: { type: Array, default: () => [] }
})

const { master_change_type, master_change_request_execute_status, master_change_item_execute_status } = useDict(
  'master_change_type', 'master_change_request_execute_status', 'master_change_item_execute_status'
)

const recordApprovalError = ref('')
const selectedRecordApproval = ref(null)
const dialogVisible = ref(false)
const dialogContainer = ref(null)
const recordApprovalDetailLoading = ref(false)
const recordApprovalDetailError = ref('')
const recordApprovalDetail = ref(null)
const detailInfoItems = computed(() => {
  const detail = recordApprovalDetail.value || {}
  const items = [
    { label: '申请编号', value: detail.requestNo || selectedRecordApproval.value?.requestNo },
    { label: '申请名称', key: 'requestName' },
    { label: '申请类型', key: 'changeType', dictOptions: master_change_type.value },
    { label: '审批状态', key: 'approvalStatus', dictOptions: props.approvalStatusOptions },
    { label: '执行状态', key: 'executeStatus', formatter: requestExecuteLabel },
    { label: '申请人', key: 'applicantName' },
    { label: '明细数', value: detail.itemCount ?? 0 },
    { label: '提交时间', value: detail.submitTime, formatter: formatApprovalTime },
    { label: '完成时间', value: detail.finishTime, formatter: formatApprovalTime },
    { label: '申请原因', key: 'applyReason', slot: 'textarea', span: 2 }
  ]
  if (detail.terminateReason) {
    items.push({ label: '终止原因', key: 'terminateReason', slot: 'textarea', span: 2 })
  }
  if (detail.executeMessage) {
    items.push({ label: '执行结果', key: 'executeMessage', slot: 'textarea', span: 2 })
  }
  return items
})
let recordApprovalDetailRequest = 0

const approvalItemsTableRef = ref(null)
const approvalItemsTableStore = {
  config: {
    initResquest: true,
    fullContent: false,
    notPagination: true,
    actions: { show: false },
    table: {
      class: 'field-design-table', stripe: true,
      rowStyle: { height: '48px' }, scrollbarAlwaysOn: false, border: false
    }
  },
  columns: [
    { label: '编号', prop: 'itemNo', width: 70, align: 'left' },
    { label: '变更类型', prop: 'changeType', minWidth: 110, align: 'left', slot: 'changeType' },
    { label: '生命周期', prop: 'lifecycle', minWidth: 160, align: 'left', slot: 'lifecycle' },
    { label: '数据版本', prop: 'version', minWidth: 135, align: 'left', slot: 'version' },
    { label: '执行结果', prop: 'execute', minWidth: 180, align: 'left', slot: 'execute', showOverflowTooltip: true }
  ],
  func: async () => ({ data: recordApprovalDetail.value?.items || [] })
}

const tableRef = ref(null)
const tableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    actions: { table: { search: false } }
  },
  columns: [
    { label: '编号', prop: 'requestId', width: 120, align: 'center', sortable: true, sortableKey: 'id' },
    { label: '审批单号', prop: 'requestNo', minWidth: 220, align: 'left' },
    { label: '操作类型', prop: 'changeType', minWidth: 150, align: 'left', slot: 'changeType' },
    { label: '提交人', prop: 'applicantName', minWidth: 130, align: 'left' },
    { label: '提交时间', prop: 'submitTime', width: 190, align: 'left', date: true },
    { label: '完成时间', prop: 'finishTime', width: 190, align: 'left', date: true },
    { label: '审批状态', prop: 'approvalStatus', width: 120, align: 'left', slot: 'approvalStatus' },
    { label: '操作', prop: 'action', width: 80, align: 'center', slot: 'action', fixed: 'right' }
  ],
  params: {},
  func: async (params) => {
    const recordId = props.detail?.row?.ID
    if (!props.modelValue || props.activeTab !== 'approvals' || !props.instanceId || !recordId) {
      return { data: { rows: [], total: 0 } }
    }
    recordApprovalError.value = ''
    try {
      const response = await getMasterChangeRecordApprovals(props.instanceId, recordId, params)
      const rows = response?.data?.rows || []
      return { data: { rows, total: response?.data?.total || 0 } }
    } catch (error) {
      recordApprovalError.value = '审批记录加载失败，请稍后重试。'
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
  recordApprovalDetailRequest++
  selectedRecordApproval.value = null
  dialogVisible.value = false
  recordApprovalDetail.value = null
  recordApprovalError.value = ''
  recordApprovalDetailError.value = ''
  recordApprovalDetailLoading.value = false
  await nextTick()
  tableRef.value?.resetQuery()
}, { immediate: true })

watch(dialogVisible, visible => {
  if (!visible) {
    recordApprovalDetailRequest++
    recordApprovalDetailLoading.value = false
  }
})

onUnmounted(() => {
  recordApprovalDetailRequest++
})

async function loadRecordApprovalDetail(approval) {
  const recordId = props.detail?.row?.ID
  if (!props.modelValue || !props.instanceId || !recordId) return
  const request = ++recordApprovalDetailRequest
  const instanceId = props.instanceId
  selectedRecordApproval.value = approval
  dialogVisible.value = true
  recordApprovalDetail.value = null
  recordApprovalDetailError.value = ''
  recordApprovalDetailLoading.value = true
  try {
    const response = await getMasterChangeRecordApprovalDetail(instanceId, recordId, approval.requestId)
    if (request !== recordApprovalDetailRequest || !props.modelValue || !dialogVisible.value
        || String(instanceId) !== String(props.instanceId)
        || String(recordId) !== String(props.detail?.row?.ID)) return
    recordApprovalDetail.value = response?.data || null
  } catch {
    if (request === recordApprovalDetailRequest) recordApprovalDetailError.value = '审批详情加载失败，请稍后重试。'
  } finally {
    if (request === recordApprovalDetailRequest) recordApprovalDetailLoading.value = false
  }
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function optionLabel(options, value) {
  if (!hasValue(value)) return '-'
  return options.find(option => String(option.value) === String(value))?.label || String(value)
}

function detailInfoValue(item) {
  const value = Object.hasOwn(item, 'value') ? item.value : recordApprovalDetail.value?.[item.key]
  if (item.dictOptions) return optionLabel(item.dictOptions, value)
  const formatted = item.formatter ? item.formatter(value) : value
  return hasValue(formatted) ? String(formatted) : '-'
}

function requestExecuteLabel(value) {
  return selectDictLabel(master_change_request_execute_status.value || [], value) || value || '-'
}

function itemExecuteLabel(value) {
  return selectDictLabel(master_change_item_execute_status.value || [], value) || value || '-'
}

function formatApprovalTime(value) {
  return value ? parseTime(value) || value : '-'
}

function statusLabel(value) {
  return optionLabel(props.statusOptions, value)
}

function statusChangeLabel(item) {
  if (!item.beforeStatus && !item.targetStatus) return '-'
  return `${item.beforeStatus ? statusLabel(item.beforeStatus) : '-'} → ${item.targetStatus ? statusLabel(item.targetStatus) : '-'}`
}

function versionChangeLabel(item) {
  if (!hasValue(item.currentVersionNo) && !hasValue(item.targetVersionNo)) return '-'
  return `${formatVersionNo(item.currentVersionNo)} → ${formatVersionNo(item.targetVersionNo)}`
}

function formatVersionNo(value) {
  if (!hasValue(value)) return '-'
  return `V${String(value).replace(/^v/i, '')}`
}
</script>

<style scoped lang="scss">
.approval-record-form :deep(.qt-wrap.mt10) {
  margin-top: 0 !important;
}
</style>
