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
  <div ref="dialogContainer">
    <el-dialog
      v-if="dialogContainer"
      v-model="visible"
      title="变更记录详情"
      width="800px"
      append-to-body
      :append-to="dialogContainer"
      draggable
      destroy-on-close
      class="dialog change-record-dialog"
      @close="invalidateRequest"
      @closed="resetDialog"
    >
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">变更记录详情</span>
      </template>
      <el-empty v-if="error" :description="error" />
      <div v-else v-loading="loading" class="column-form change-snapshot-content">
        <el-form v-if="selectedChange" :model="selectedChange" label-width="120px" class="column-form row-full" @submit.prevent>
          <div class="h2-title row-full">基础信息</div>
          <qt-form-item v-for="item in overviewItems" :key="item.key"
                        :label="item.label" :prop="item.key" :class="{ 'row-full': item.span === 2 }"
          >
            <div class="form-readonly" :class="{ textarea: item.slot === 'textarea' }">{{ overviewValue(item) }}</div>
          </qt-form-item>
        </el-form>

        <template v-if="snapshot">
          <div class="h2-title row-full">字段变化</div>
          <qt-wrap
            v-if="snapshot.diffAvailable && snapshot.diffs?.length"
            :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef" class="row-full mt10"
          >
            <qt-table ref="tableRef" v-bind="tableStore">
              <template #changeType="{ row }">
                <dict-tag :type="changeType(row).type">{{ changeType(row).label }}</dict-tag>
              </template>
            </qt-table>
          </qt-wrap>
          <el-empty
            v-else-if="snapshot.diffAvailable"
            description="前后业务快照没有字段值差异" class="row-full"
          />
          <template v-else>
            <el-text type="info" class="row-full">快照缺失。</el-text>
            <el-form :model="snapshot" label-width="150px" class="column-form row-full" @submit.prevent>
              <qt-form-item v-for="item in snapshotItems" :key="item.key"
                            :label="item.label" :prop="item.key" class="row-full"
              >
                <el-input :model-value="displayValue(snapshot[item.key])" readonly type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
              </qt-form-item>
            </el-form>
          </template>
        </template>
        <el-empty v-else-if="!loading" description="暂无变更详情" class="row-full" />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ChangeSnapshotDialog">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { getMasterChangeSnapshot } from '@/api/master/change/change'
import { useDict } from '@/utils/dict'
import { parseTime } from '@/utils/anivia'

const props = defineProps({
  instanceId: { type: [String, Number], default: null },
  masterDataId: { type: [String, Number], default: null }
})

const { master_change_type } = useDict('master_change_type')

const dialogContainer = ref(null)
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const snapshot = ref(null)
const selectedChange = ref(null)
const tableRef = ref(null)
let snapshotRequest = 0

const overviewItems = computed(() => [
  { label: '变更编号', key: 'id' },
  { label: '变更时间', key: 'changeTime', formatter: value => value ? parseTime(value) || value : '-' },
  { label: '操作人', key: 'operatorName' },
  { label: '变更方式', key: 'type', dictOptions: master_change_type.value || [] },
  { label: '版本变化', key: 'versionChange' },
  { label: '关联申请编号', key: 'requestNo' },
  { label: '变更原因', key: 'changeReason', slot: 'textarea', span: 2 }
])

const snapshotItems = [
  { label: '变更前', key: 'beforeData' },
  { label: '变更后', key: 'afterData' }
]

const tableStore = reactive({
  config: {
    initResquest: true,
    fullContent: false,
    notPagination: true,
    notPaginationParams: true,
    actions: { show: false },
    table: {
      class: 'field-design-table', stripe: true, scrollbarAlwaysOn: false,
      border: false, height: null, maxHeight: 240, rowStyle: { height: '40px' }
    }
  },
  columns: [
    { label: '编号', prop: 'rowNo', width: 80, align: 'center', sortable: 'custom' },
    { label: '字段路径', prop: 'fieldPath', minWidth: 220, align: 'left' },
    { label: '变更前', prop: 'beforeValue', minWidth: 280, align: 'left', showOverflowTooltip: { effect: 'light' }, formatter: row => displayValue(row.beforeValue) },
    { label: '变更后', prop: 'afterValue', minWidth: 280, align: 'left', showOverflowTooltip: { effect: 'light' }, formatter: row => displayValue(row.afterValue) },
    { label: '变化', prop: 'changeType', width: 90, align: 'center', slot: 'changeType' }
  ],
  func: async (params = {}) => {
    // 编号对应原始差异顺序，排序后保留编号，取消排序时恢复原始顺序。
    const rows = (snapshot.value?.diffs || []).map((row, index) => ({ ...row, rowNo: index + 1 }))
    if (params.orderByColumn === 'rowNo' && params.isAsc === 'descending') rows.reverse()
    return { data: rows }
  }
})

async function openDialog(change) {
  if (!props.instanceId || !props.masterDataId || !change?.id) return
  const request = ++snapshotRequest
  const instanceId = props.instanceId
  const masterDataId = props.masterDataId
  selectedChange.value = change
  snapshot.value = null
  error.value = ''
  loading.value = true
  visible.value = true
  try {
    const response = await getMasterChangeSnapshot(instanceId, masterDataId, change.id)
    if (request !== snapshotRequest
      || String(instanceId) !== String(props.instanceId)
      || String(masterDataId) !== String(props.masterDataId)) return
    snapshot.value = response?.data || null
  } catch {
    if (request === snapshotRequest) error.value = '字段差异加载失败，请稍后重试。'
  } finally {
    if (request === snapshotRequest) loading.value = false
  }
}

function invalidateRequest() {
  snapshotRequest++
  loading.value = false
}

function resetDialog() {
  selectedChange.value = null
  snapshot.value = null
  error.value = ''
}

function displayValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return value
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function overviewValue(item) {
  const value = selectedChange.value?.[item.key]
  if (!hasValue(value)) return '-'
  if (item.dictOptions) {
    return item.dictOptions.find(option => String(option.value) === String(value))?.label || String(value)
  }
  const formatted = item.formatter ? item.formatter(value) : value
  return displayValue(formatted)
}

function changeType(row) {
  if (!hasValue(row.beforeValue) && hasValue(row.afterValue)) return { label: '新增', type: 'success' }
  if (hasValue(row.beforeValue) && !hasValue(row.afterValue)) return { label: '清空', type: 'danger' }
  return { label: '修改', type: 'warning' }
}

onUnmounted(invalidateRequest)

defineExpose({ openDialog })
</script>

<style scoped>
.change-snapshot-content :deep(.qt-wrap.mt10) {
  margin-top: 0 !important;
}

.app-container :deep(.change-record-dialog .el-dialog__body) {
  max-height: calc(80vh - 110px);
}
</style>
