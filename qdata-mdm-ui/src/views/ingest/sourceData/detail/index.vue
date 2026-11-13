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
  来源数据详情：展示当前记录的来源与业务字段，并查看同一来源记录的历史版本。
-->
<template>
  <div v-loading="loading" class="app-container source-data-detail">
    <detail-info
      class="source-data-detail-info"
      :data="headerData"
      :header="{ idKey: 'id', nameKey: 'title', backText: '返回' }"
      :items="detailItems"
      mode="free"
      :gutter="2"
    >
      <template #sourceSystem="{ item }">
        <div class="qt-table-info">
          <div class="qt-table-info__main">
            <img :src="getSourceIcon(sourceSystemMeta)" alt="" class="info-icon" />
            <span class="info-title text-ellipsis">{{ item.value }}</span>
          </div>
        </div>
      </template>
      <template #model="{ item }">
        <div class="qt-table-info">
          <div class="qt-table-info__main">
            <img :src="getModelIcon(modelMeta)" alt="" class="info-icon" />
            <span class="info-title text-ellipsis">{{ item.value }}</span>
            <dict-tag
              :options="MODEL_TYPE_OPTIONS"
              :value="detail.modelType || modelMeta.type"
              size="small"
              type="primary"
            />
            <dict-tag
              v-if="detail.modelVersionNo"
              class="ml4"
              :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: detail.modelVersionNo }))"
              :value="detail.modelVersionStatus"
              size="small"
            />
          </div>
        </div>
      </template>
      <template #ingestType>
        <dict-tag
          :icon="TASK_TYPE_ICONS[ingestTaskType]"
          :type="taskTypeTagType"
        >{{ getOptionLabel(TASK_TYPES, ingestTaskType) }}</dict-tag>
      </template>
      <template #ingestTask="{ item }">
        <div class="qt-table-info">
          <div class="qt-table-info__main">
            <img :src="ingestTaskIcon" alt="" class="info-icon" />
            <span class="info-title text-ellipsis">{{ item.value }}</span>
          </div>
        </div>
      </template>
    </detail-info>
    <div class="pagecont-bottom">
      <el-tabs v-model="activeTab" class="demo-tabs">
        <el-tab-pane label="业务数据" name="business">
          <qt-wrap
            :columns="businessTable.columns"
            :table-ref="businessTableRef"
            :config="{ fullContent: false, actions: { table: { search: false } } }"
          >
            <template #actions-data>
              <guide-tip-panel
                type="plain"
                title="按主数据模型定义的字段展示当前来源记录业务数据，可对照字段名称和值核验采集结果，了解数据在模型中的对应内容"
              />
            </template>
            <qt-table ref="businessTableRef" v-bind="businessTable" :columns="businessTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" />
          </qt-wrap>
        </el-tab-pane>
        <el-tab-pane label="历史记录" name="history">
          <qt-wrap
            :columns="historyTable.columns"
            :table-ref="historyTableRef"
            :config="{ fullContent: false, actions: { table: { search: false } } }"
          >
            <template #actions-data>
              <guide-tip-panel
                type="plain"
                title="查看当前来源记录历次接入批次及采集时间，并按批次打开历史快照、对比记录变化，追溯来源数据的更新过程"
              />
            </template>
            <qt-table ref="historyTableRef" v-bind="historyTable" :columns="historyTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #rowIndex="{ $index }">
                {{ (historyTable.params.pageNum - 1) * historyTable.params.pageSize + $index + 1 }}
              </template>
              <template #lastReceiveTime="{ row }">{{ formatTime(row.lastReceiveTime) }}</template>
              <template #handle="{ row }">
                <el-button link type="primary" @click="viewHistoryRecord(row)">
                  <el-icon><icon-view /></el-icon>查看
                </el-button>
              </template>
            </qt-table>
          </qt-wrap>
        </el-tab-pane>
      </el-tabs>
    </div>
    <el-dialog v-model="historyDetailVisible"
               title="来源数据详情"

               class="medium-dialog"
               destroy-on-close
    >
      <div v-loading="historyDetailLoading">
        <source-data-field-grid :items="historyFields" />
      </div>
      <template #footer>
        <el-button @click="historyDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SourceDataDetail">
import { computed, reactive, ref, watch } from 'vue'

import { useRoute } from 'vue-router'
import { View as IconView } from '@element-plus/icons-vue'

import DetailInfo from '@/components/DetailInfo/index.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import SourceDataFieldGrid from '@/views/ingest/sourceData/detail/sourceDataFieldGrid.vue'
import { getSourceDataDetail, getSourceDataModelColumns, listSourceData } from '@/api/ingest/LOG'
import { getIngestSource } from '@/api/ingest/source/source.js'
import { getIngestTask } from '@/api/ingest/task/task.js'

import { getModelIcon } from '@/utils/imageUrl'
import { MODEL_TYPE_OPTIONS } from '@/utils/model/modelType'
import { MODEL_STATUS_OPTIONS } from '@/utils/model/modelStatus'

import ingestTaskIcon from '@/assets/modules/ingest/icons/task/ingest-task.svg'

import { getProjectIcon } from '@/utils/imageUrl';

import databaseIcon from '@/assets/modules/ingest/icons/task/database.svg';

import fileIcon from '@/assets/modules/ingest/icons/task/file.svg';

import apiIcon from '@/assets/modules/ingest/icons/task/api.svg';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const getOptionLabel = (options, value) =>
  options.find((item) => item.value === value)?.label || value || '-';

const TASK_TYPES = [
  { value: 'DATABASE', label: '数据库' },
  { value: 'FILE', label: '文件' },
  { value: 'API', label: 'API' },
];

const TASK_TYPE_ICONS = { DATABASE: databaseIcon, FILE: fileIcon, API: apiIcon };

const route = useRoute()
const detail = ref(window.history.state?.sourceData || {})
const modelColumns = ref(window.history.state?.modelColumns || [])
const historyTableRef = ref()
const businessTableRef = ref()
const activeTab = ref('business')
const historyDetailVisible = ref(false)
const historyDetail = ref({})
const loading = ref(false)
const historyDetailLoading = ref(false)
const historyLoaded = ref(false)
const requestId = ref(0)
const sourceTrustRequestId = ref(0)
const sourceSystemMeta = ref({})
const ingestTaskType = ref('')
const modelMeta = ref({})
const taskTypeRequestId = ref(0)
const historyDetailRequestId = ref(0)
const recordId = computed(() => route.query.id ?? null)
const modelId = computed(() => route.query.modelId ?? null)
const sourceSystemId = computed(() => route.query.sourceSystemId ?? null)
const sourceRecordKey = computed(() => route.query.sourceRecordKey ?? '')
const businessData = computed(() => parseModelData(detail.value?.modelData))
const historyBusinessData = computed(() => parseModelData(historyDetail.value?.modelData))
const headerData = computed(() => ({ id: recordId.value, ...detail.value, title: '来源数据详情' }))
const businessRow = computed(() => Object.fromEntries(
  modelColumns.value.map(column => [column.columnCode, displayBusinessValue(businessData.value[column.columnCode])])
))
const businessColumns = computed(() => modelColumns.value.map(column => ({
  label: column.columnName,
  prop: column.columnCode,
  minWidth: 140,
  align: 'center',
  showOverflowTooltip: { effect: 'light' }
})))
const detailItems = computed(() => [
  { label: '来源系统编码', value: sourceSystemMeta.value.code || '-', className: 'mt2 mb2' },
  { label: '主数据模型', value: formatModelName(detail.value), slot: 'model', className: 'mt2 mb2' },
  { label: '来源系统', value: detail.value?.sourceSystemName || detail.value?.sourceSystemId || '-', slot: 'sourceSystem', className: 'mt2 mb2' },
  { label: '接入类型', slot: 'ingestType', className: 'mt2 mb2' },
  { label: '来源可信度', value: formatTrustScore(sourceTrustScore.value), className: 'mt2 mb2' },
  { label: '接入任务', value: detail.value?.taskName || detail.value?.taskId || '-', slot: 'ingestTask', className: 'mt2 mb2' },
  { label: '接入批次', value: detail.value?.batchNo || '-', className: 'mt2 mb2' },
  { label: '来源记录', value: detail.value?.sourceRecordKey || sourceRecordKey.value || '-', className: 'mt2 mb2' },
  { label: '采集时间', value: formatTime(detail.value?.receiveTime || detail.value?.lastReceiveTime), className: 'mt2 mb2' }
])
const sourceTrustScore = ref(null)
const historySourceFields = computed(() => [
  { label: '来源系统', value: historyDetail.value?.sourceSystemName || historyDetail.value?.sourceSystemId },
  { label: '接入任务', value: historyDetail.value?.taskName || historyDetail.value?.taskId },
  { label: '接入批次', value: historyDetail.value?.batchNo },
  { label: '来源记录', value: historyDetail.value?.sourceRecordKey },
  { label: '采集时间', value: formatTime(historyDetail.value?.receiveTime || historyDetail.value?.lastReceiveTime) }
])
const historyFields = computed(() => [
  ...historySourceFields.value,
  ...modelColumns.value.map(column => ({
    label: column.columnName,
    value: historyBusinessData.value[column.columnCode]
  }))
])
const historyColumns = computed(() => [
  { label: '编号', prop: 'id', slot: 'rowIndex', width: 90, minWidth: 90, sortable: 'custom', align: 'left', headerAlign: 'left' },
  { label: '接入批次', prop: 'batchNo', minWidth: 180, showOverflowTooltip: true, align: 'left', headerAlign: 'left' },
  { label: '采集时间', prop: 'lastReceiveTime', slot: 'lastReceiveTime', minWidth: 170, align: 'left', headerAlign: 'left' },
  ...modelColumns.value.map(column => ({
    label: column.columnName,
    prop: column.columnCode,
    minWidth: 140,
    align: 'center',
    showOverflowTooltip: { effect: 'light' }
  })),
  { label: '操作', slot: 'handle', width: 100, minWidth: 100, fixed: 'right', align: 'center', headerAlign: 'center' }
])
const businessTable = reactive({
  func: () => Promise.resolve({ data: [{ ...businessRow.value, rowKey: 'current-source-record' }] }),
  config: {
    notPagination: true,
    table: { rowKey: 'rowKey', stripe: true }
  },
  columns: businessColumns.value
})
const historyTable = reactive({
  func: params => listSourceData(params),
  events: { formatData: formatHistoryRows },
  params: {
    pageNum: 1,
    pageSize: 5,
    modelId: modelId.value,
    sourceRecordKey: sourceRecordKey.value,
    sourceSystemId: sourceSystemId.value,
    includeHistory: true
  },
  config: {
    initResquest: false,
    sort: true,
    table: { rowKey: 'id', stripe: true },
    pagination: { pageSizes: [5, 10, 20], notAutoScroll: true }
  },
  columns: historyColumns.value
})

watch(businessColumns, columns => {
  businessTable.columns = columns
}, { deep: true })

watch(businessRow, () => businessTableRef.value?.getList(), { deep: true })

watch(historyColumns, columns => {
  historyTable.columns = columns
}, { deep: true })

watch(
  () => [route.path, recordId.value, modelId.value, sourceSystemId.value, sourceRecordKey.value],
  () => {
    activeTab.value = 'business'
    const routeState = window.history.state || {}
    detail.value = routeState.sourceData || { id: recordId.value, sourceRecordKey: sourceRecordKey.value }
    modelMeta.value = {
      icon: detail.value?.modelIcon,
      type: detail.value?.modelType
    }
    if (routeState.modelColumns) modelColumns.value = routeState.modelColumns
    historyLoaded.value = false
    historyTable.params.modelId = modelId.value
    historyTable.params.sourceSystemId = sourceSystemId.value
    historyTable.params.sourceRecordKey = sourceRecordKey.value
    loadSourceTrustScore(sourceSystemId.value)
    loadTaskType(detail.value?.taskId)
    if (route.path !== '/ingest/sourceData/detail' || recordId.value == null || modelId.value == null) return
    if (!modelColumns.value.length) loadModelColumns()
    loadDetail(recordId.value)
  },
  { immediate: true }
)

watch(activeTab, tab => {
  if (tab === 'history' && !historyLoaded.value && sourceRecordKey.value && sourceSystemId.value != null) {
    historyTableRef.value?.resetQuery()
    historyLoaded.value = true
  }
})

/**
 * 补充加载当前来源记录详情，忽略旧请求并在失败时保留已有内容。
 * @param {string|number} id 来源记录 ID。
 */
async function loadDetail(id) {
  if (id == null) return
  const currentRequest = ++requestId.value
  loading.value = !detail.value?.modelData
  try {
    const response = await getSourceDataDetail(id, modelId.value)
    if (currentRequest === requestId.value && response.data) {
      detail.value = response.data
      modelMeta.value = {
        icon: response.data.modelIcon,
        type: response.data.modelType
      }
      loadTaskType(response.data.taskId)
    }
  } catch {
    /*
     * 列表行已提供可展示内容，详情补充请求失败时保留当前内容。
     */
  } finally {
    if (currentRequest === requestId.value) loading.value = false
  }
}

/** 根据来源系统 ID 加载来源可信度，忽略已过期的请求结果。 */
async function loadSourceTrustScore(id) {
  const currentRequest = ++sourceTrustRequestId.value
  sourceTrustScore.value = null
  sourceSystemMeta.value = {}
  if (id == null) return
  try {
    const response = await getIngestSource(id)
    if (currentRequest === sourceTrustRequestId.value) {
      sourceSystemMeta.value = response.data || {}
      sourceTrustScore.value = response.data?.trustScore ?? null
    }
  } catch {
    if (currentRequest === sourceTrustRequestId.value) {
      sourceSystemMeta.value = {}
      sourceTrustScore.value = null
    }
  }
}

/** 根据接入任务补充接入类型，用于详情字段和对应图标。 */
async function loadTaskType(id) {
  const currentRequest = ++taskTypeRequestId.value
  ingestTaskType.value = ''
  if (id == null) return
  try {
    const response = await getIngestTask(id)
    if (currentRequest === taskTypeRequestId.value) {
      ingestTaskType.value = response.data?.type || ''
      modelMeta.value = {
        icon: response.data?.modelIcon || modelMeta.value.icon,
        type: response.data?.modelType || modelMeta.value.type
      }
    }
  } catch {
    if (currentRequest === taskTypeRequestId.value) ingestTaskType.value = ''
  }
}

/** 加载当前模型业务字段定义，失败时清空字段展示配置。 */
async function loadModelColumns() {
  if (modelId.value == null) return
  try {
    const response = await getSourceDataModelColumns(modelId.value)
    modelColumns.value = response.data || []
  } catch {
    modelColumns.value = []
  }
}

/**
 * 先展示历史列表行，再补充详情；仅接受最近一次打开记录的响应。
 * @param {Object} row 历史来源记录。
 */
async function viewHistoryRecord(row) {
  historyDetail.value = row
  historyDetailVisible.value = true
  const currentRequest = ++historyDetailRequestId.value
  historyDetailLoading.value = true
  try {
    const response = await getSourceDataDetail(row.id, modelId.value)
    if (currentRequest === historyDetailRequestId.value && response.data) historyDetail.value = response.data
  } catch {
    /*
     * 请求失败时继续显示历史列表中的记录数据。
     */
  } finally {
    if (currentRequest === historyDetailRequestId.value) historyDetailLoading.value = false
  }
}

/** 将日期时间字符串中的 T 分隔符转为空格，无法匹配时保留原文。 */
function formatTime(value) {
  if (!value) return '-'
  const result = String(value).match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/)
  return result ? `${result[1]} ${result[2]}` : String(value)
}

/** 将来源可信度格式化为百分制文本。 */
function formatTrustScore(value) {
  return value == null ? '-' : String(value)
}

/** 格式化主数据模型名称与编码。 */
function formatModelName(data) {
  const name = data?.modelName || data?.modelId || '-'
  return data?.modelCode ? `${name}（${data.modelCode}）` : name
}

const taskTypeTagType = computed(() => ({
  DATABASE: 'primary',
  FILE: 'warning',
  API: 'success'
}[ingestTaskType.value] || 'info'))

/**
 * 兼容业务数据对象与 JSON 字符串，空值或解析失败时返回空对象。
 * @param {Object|string} modelData 来源业务数据。
 * @returns {Object} 业务数据。
 */
function parseModelData(modelData) {
  if (modelData && typeof modelData === 'object') return modelData
  try {
    return modelData ? JSON.parse(modelData) : {}
  } catch {
    return {}
  }
}

/** 展开历史来源记录的业务 JSON，并格式化模型字段值。 */
function formatHistoryRows(rows) {
  return rows.map(row => {
    const data = { ...parseModelData(row.modelData), ...row }
    return {
      ...data,
      ...Object.fromEntries(modelColumns.value.map(column => [
        column.columnCode,
        displayBusinessValue(data[column.columnCode])
      ]))
    }
  })
}

/** 统一格式化业务字段空值和对象值。 */
function displayBusinessValue(value) {
  if (value == null || value === '') return '-'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
</script>

