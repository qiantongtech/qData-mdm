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
          <GuideTipPanel type="plain" title="本页展示当前主数据的版本及变更记录，支持查看各版本对应的申请单、变更类型、操作人和操作时间，便于追溯历史变更。" />
        </template>
        <el-alert v-if="versionError" :title="versionError" type="error" :closable="false" show-icon class="mb15" />
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #text="{ row, column_data }">{{ row[column_data.prop] || '-' }}</template>
          <template #generateTime="{ row }">{{ parseTime(row.generateTime) || '-' }}</template>
          <template #versionNo="{ row }">
            {{ formatVersionNo(row.versionNo) }}
            <el-tag v-if="row.currentFlag === '1'" size="small" type="success">当前</el-tag>
          </template>
          <template #changeType="{ row }">
            <dict-tag v-if="row.changeType" :value="row.changeType" :options="master_change_type" />
            <el-tag v-else-if="row.generateType" type="info">{{ generationLabel(row.generateType) }}</el-tag>
            <span v-else>-</span>
          </template>
          <template #action="{ row }">
            <el-button link type="primary" icon="View" @click="loadSnapshot(row)">查看</el-button>
          </template>
        </qt-table>
      </qt-wrap>
    </div>
    <div ref="dialogContainer">
      <el-dialog
        v-if="dialogContainer"
        v-model="dialogVisible"
        title="版本记录详情"
        width="800px"
        append-to-body
        :append-to="dialogContainer"
        destroy-on-close
        draggable
        class="dialog"
      >
        <template #header>
          <span role="heading" aria-level="2" class="el-dialog__title">版本记录详情</span>
        </template>
        <el-empty v-if="snapshotError" :description="snapshotError" />
        <div v-else v-loading="snapshotLoading" class="version-snapshot-dialog">
          <template v-if="selectedVersion">
            <el-text v-if="snapshotData?.truncated" type="info">快照内容过长，当前仅显示前 262144 个字符。</el-text>
            <el-form :model="selectedVersion" label-width="130px" class="column-form" @submit.prevent>
              <qt-form-item v-for="item in versionInfoItems" :key="item.key"
                            :label="item.label" :prop="item.key"
              >
                <div class="form-readonly">{{ versionInfoValue(item) }}</div>
              </qt-form-item>
              <qt-form-item label="字段快照" class="row-full">
                <pre class="form-readonly textarea snapshot-data-pre">{{ snapshotData ? prettySnapshot(snapshotData.snapshotData) : '-' }}</pre>
              </qt-form-item>
            </el-form>
          </template>
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
import { getMasterDataVersions, getMasterDataVersionSnapshot } from '@/api/master/data/version'
import { parseTime } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'

const props = defineProps({
  modelValue: Boolean,
  activeTab: { type: String, default: '' },
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null }
})

const { master_change_type } = useDict('master_change_type')

const versionError = ref('')
const selectedVersion = ref(null)
const dialogVisible = ref(false)
const dialogContainer = ref(null)
const snapshotLoading = ref(false)
const snapshotError = ref('')
const snapshotData = ref(null)
const versionInfoItems = computed(() => [
  { label: '版本', key: 'versionNo', formatter: formatVersionNo },
  { label: '操作时间', key: 'generateTime', formatter: value => parseTime(value) || '-' },
  { label: '变更类型', key: 'changeType', formatter: changeTypeLabel },
  { label: '操作人', key: 'operatorName' },
  { label: '关联申请编号', key: 'requestNo' }
])
// 父级详情关闭、切换版本或组件卸载后，旧快照响应不得覆盖当前版本。
let snapshotRequest = 0
let versionRequest = 0
let latestVersionRequest = Promise.resolve({ data: { rows: [], total: 0 } })

const tableRef = ref(null)
const tableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    actions: { table: { search: false } }
  },
  columns: [
    { label: '编号', prop: 'id', width: 120, align: 'center', sortable: 'custom', sortableKey: 'id', slot: 'text' },
    { label: '版本', prop: 'versionNo', width: 180, align: 'left', slot: 'versionNo' },
    { label: '变更类型', prop: 'changeType', minWidth: 160, align: 'left', slot: 'changeType' },
    { label: '申请编号', prop: 'requestNo', minWidth: 220, align: 'left', slot: 'text' },
    { label: '操作人', prop: 'operatorName', minWidth: 140, align: 'left', slot: 'text' },
    { label: '操作时间', prop: 'generateTime', width: 190, align: 'left', sortable: 'custom', sortableKey: 'generate_time', slot: 'generateTime' },
    { label: '操作', prop: 'action', width: 90, align: 'center', slot: 'action', fixed: 'right' }
  ],
  // 列表仅取版本展示字段；大字段快照在查看时按版本 ID 单独加载。
  func: loadVersionPage
})

watch([
  () => props.modelValue,
  () => props.instanceId,
  () => props.detail?.row?.ID,
  () => props.activeTab
], async () => {
  versionRequest++
  snapshotRequest++
  selectedVersion.value = null
  dialogVisible.value = false
  snapshotData.value = null
  versionError.value = ''
  snapshotError.value = ''
  snapshotLoading.value = false

  await nextTick()
  tableRef.value?.resetQuery()
}, { immediate: true })

watch(dialogVisible, (visible) => {
  if (!visible) {
    snapshotRequest++
    snapshotLoading.value = false
  }
})

onUnmounted(() => {
  versionRequest++
  snapshotRequest++
})

async function loadVersionPage(params) {
  const request = ++versionRequest
  const instanceId = props.instanceId
  const recordId = props.detail?.row?.ID
  versionError.value = ''
  latestVersionRequest = (async () => {
    const empty = { data: { rows: [], total: 0 } }
    if (!props.modelValue || props.activeTab !== 'versions' || !instanceId || !recordId) return empty
    try {
      const response = await getMasterDataVersions(instanceId, recordId, params)
      if (request !== versionRequest || !props.modelValue || props.activeTab !== 'versions'
          || String(instanceId) !== String(props.instanceId)
          || String(recordId) !== String(props.detail?.row?.ID)) return empty
      return { data: { rows: response?.data?.rows || [], total: response?.data?.total || 0 } }
    } catch {
      if (request === versionRequest) versionError.value = '版本记录加载失败，请稍后重试。'
      return empty
    }
  })()

  // QtTable 会应用每次返回值；旧调用也等待最新请求，避免旧页覆盖新页。
  let pendingRequest
  let result
  do {
    pendingRequest = latestVersionRequest
    result = await pendingRequest
  } while (pendingRequest !== latestVersionRequest)
  return result
}

async function loadSnapshot(version) {
  const recordId = props.detail?.row?.ID
  if (!props.modelValue || !props.instanceId || !recordId) return
  const request = ++snapshotRequest
  const instanceId = props.instanceId
  selectedVersion.value = version
  dialogVisible.value = true
  snapshotData.value = null
  snapshotError.value = ''
  snapshotLoading.value = true
  try {
    const response = await getMasterDataVersionSnapshot(instanceId, recordId, version.id)
    if (request !== snapshotRequest || !props.modelValue || !dialogVisible.value
        || String(instanceId) !== String(props.instanceId)
        || String(recordId) !== String(props.detail?.row?.ID)) return
    snapshotData.value = response?.data || null
  } catch {
    if (request === snapshotRequest) snapshotError.value = '版本快照加载失败，请稍后重试。'
  } finally {
    if (request === snapshotRequest) snapshotLoading.value = false
  }
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function formatVersionNo(value) {
  if (!hasValue(value)) return '-'
  return `V${String(value).replace(/^v/i, '')}`
}

function versionInfoValue(item) {
  const value = selectedVersion.value?.[item.key]
  const formatted = item.formatter ? item.formatter(value) : value
  return hasValue(formatted) ? String(formatted) : '-'
}

function changeTypeLabel(value) {
  if (!hasValue(value)) return generationLabel(selectedVersion.value?.generateType)
  return (master_change_type.value || []).find(option => String(option.value) === String(value))?.label || String(value)
}

function generationLabel(value) {
  if (!hasValue(value)) return '-'
  return {
    CREATE: '新增', UPDATE: '修改', MERGE: '融合', VERSION_RESTORE: '历史版本恢复',
    FREEZE: '冻结', UNFREEZE: '解冻', INVALIDATE: '失效', RECOVER: '恢复', ARCHIVE: '归档'
  }[value] || value
}

// 仅对可解析的历史快照做展示格式化，解析失败时保留数据库原文。
function prettySnapshot(content) {
  if (!content) return '该版本没有保存字段快照。'
  try { return JSON.stringify(formatSnapshotValue(JSON.parse(content)), null, 2) } catch { return content }
}

function formatSnapshotValue(value) {
  if (Array.isArray(value)) return value.map(formatSnapshotValue)
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, formatSnapshotValue(item)]))
  }
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(value.trim())) return value
  return parseTime(value) || value
}
</script>

<style scoped lang="scss">
.version-snapshot-dialog { min-height: 120px; }
.snapshot-data-pre { max-height: 40vh; margin: 0; overflow-y: auto; white-space: pre-wrap; overflow-wrap: anywhere; font-family: inherit; }
</style>
