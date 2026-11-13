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
  来源数据列表：按主数据模型加载动态业务字段，查询记录并删除待融合数据。
-->
<template>
  <div ref="sourceDataContainerRef" class="app-container source-data-page">
    <guide-tip tip-id="ingest/sourceData.list"/>
    <el-container>
      <qt-tree
          ref="modelTreeRef"
          :data="modelTreeData"
          :action-loading="modelTreeLoading"
          :use-builtin-cat-edit-dialog="false"
          :tree-props="{ label: 'name', children: 'children' }"
          node-key="treeKey"
          title="主数据模型"
          placeholder="搜索名称或编码"
          @node-click="node => handleTreeClick(node)"
      />
      <el-main v-loading="treeLoading" class="main-content">
        <qt-wrap
            :columns="tableStore.columns"
            :table-ref="tableRef"
        >
          <template #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                :table-ref="tableRef"
                @query="handleQuery"
                @reset="handleReset"
            />
          </template>
          <template #actions-data>
            <el-button
                v-hasPermi="['ingest:sourceData:remove']"
                type="danger"
                plain
                icon="Delete"
                :disabled="!selectedRows.length || deleteLoading"
                :loading="deleteLoading"
                @click="handleBatchDelete"
            >批量删除
            </el-button>
          </template>
          <qt-table :key="modelRequestId" ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
            <template #rowIndex="{ $index }">{{ (currentPageNum - 1) * currentPageSize + $index + 1 }}</template>
            <template #businessName="{ row, column_data }">
              <el-link
                v-if="getDisplayTitle(displayBusinessValue(row[column_data.property || column_data.prop]))"
                type="primary" :underline="false"
                role="link" tabindex="0"
                class="qt-detail-link text-ellipsis"
                :title="String(displayBusinessValue(row[column_data.property || column_data.prop]))"
                @click.stop="openDetail(row)"
                @keydown.enter.stop.prevent="openDetail(row)"
                @keydown.space.stop.prevent="openDetail(row)"
              >{{ displayBusinessValue(row[column_data.property || column_data.prop]) }}</el-link>
              <span v-else class="qt-detail-link text-ellipsis">-</span>
            </template>
            <template #businessValue="{ row, column_data }">
              {{ displayBusinessValue(row[(column_data.property || column_data.prop)]) }}
            </template>
            <template #governanceStatus="{ row }">
              <el-tag :type="governanceStatusType(row.governanceStatus)" effect="light">
                {{ governanceStatusLabel(row.governanceStatus) }}
              </el-tag>
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({ icon: row.modelIcon, type: row.modelType })" alt="" class="info-icon"/>
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ row.modelName ? formatEntityTitle(row.modelName, row.modelCode) : row.modelCode || '-' }}</span>
                  <dict-tag v-if="row.modelType" :options="MODEL_TYPE_OPTIONS" :value="row.modelType" size="small" type="primary"/>
                  <dict-tag
                      v-if="row.modelVersionNo"
                      class="ml4"
                      :class="{ pointer: row.modelId != null }"
                      :options="getVersionTagOptions(row)"
                      :value="row.modelVersionStatus ?? row.modelVersionNo"
                      size="small"
                      type="info"
                      :role="row.modelId != null ? 'link' : undefined"
                      :tabindex="row.modelId != null ? 0 : undefined"
                      @click="openModelDetail(row, 'versions')"
                      @keydown.enter="openModelDetail(row, 'versions')"
                      @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || '-' }}</div>
              </div>
            </template>
            <template #sourceSystemInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getSourceIcon({ icon: row.sourceSystemIcon })" alt="" class="info-icon"/>
                  <span class="info-title text-ellipsis" :title="formatEntityTitle(row.sourceSystemName, row.sourceSystemCode)">
                    {{ formatEntityTitle(row.sourceSystemName, row.sourceSystemCode) }}
                  </span>
                </div>
                <div class="info-desc text-ellipsis">{{ row.sourceSystemDescription || '-' }}</div>
              </div>
            </template>
            <template #taskInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="ingestTaskIcon" alt="" class="info-icon"/>
                  <span class="info-title text-ellipsis" :title="formatEntityTitle(row.taskName, row.taskCode)">
                    {{ formatEntityTitle(row.taskName, row.taskCode) }}
                  </span>
                </div>
                <div class="info-desc text-ellipsis">{{ row.taskDescription || '-' }}</div>
              </div>
            </template>
            <template #action="{ row }">
              <el-button link
                         type="primary"
                         icon="view"
                         @click="openDetail(row)"
              >详情
              </el-button>
              <el-button
                  v-if="isPendingGovernanceStatus(row.governanceStatus)"
                  v-hasPermi="['ingest:sourceData:remove']"
                  link
                  type="danger"
                  icon="Delete"
                  :disabled="deleteLoading"
                  @click="handleDelete(row)"
              >删除
              </el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="SourceData">
import { getDisplayTitle } from '@/utils/anivia.js'
import {computed, nextTick, watch} from 'vue'
import {useRouter} from 'vue-router'

import QtTree from '@/components/QtTree/index.vue'
import {usePublishedModelTree, withModelTreeKeys} from '@/composables/usePublishedModelTree'
import {deleteSourceData, getSourceDataModelColumns, listSourceData} from '@/api/ingest/LOG'
import {listIngestSource} from '@/api/ingest/source/source'

import {getModelIcon} from '@/utils/imageUrl'
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType'
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus'
import ingestTaskIcon from '@/assets/modules/ingest/icons/task/ingest-task.svg'

import { getProjectIcon } from '@/utils/imageUrl';

const GOVERNANCE_STATUS = Object.freeze({
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  MERGED: 'MERGED'
})

const statusDisplay = {
  [GOVERNANCE_STATUS.PENDING]: { label: '待融合', type: 'warning' },
  [GOVERNANCE_STATUS.PROCESSING]: { label: '融合中', type: 'primary' },
  [GOVERNANCE_STATUS.MERGED]: { label: '已融合', type: 'success' }
}

function governanceStatusLabel(status) {
  return statusDisplay[String(status)]?.label || '-'
}

const governanceStatusOptions = [
  { label: '全部', value: null },
  { label: '待融合', value: GOVERNANCE_STATUS.PENDING },
  { label: '融合中', value: GOVERNANCE_STATUS.PROCESSING },
  { label: '已融合', value: GOVERNANCE_STATUS.MERGED }
]

function governanceStatusType(status) {
  return statusDisplay[String(status)]?.type || 'info'
}

function isPendingGovernanceStatus(status) {
  return String(status) === GOVERNANCE_STATUS.PENDING
}

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const props = defineProps({sourceSystemId: {type: [String, Number], default: undefined}})
const {proxy} = getCurrentInstance()
const router = useRouter()
const sourceDataContainerRef = ref(null)
const modelTreeRef = ref()
// 页面自己取已发布模型树再交给 QtTree，树组件不再决定这里显示哪些业务数据。
const {data: modelTreeData, loading: modelTreeLoading} = usePublishedModelTree(withModelTreeKeys)
const modelTreeProps = {label: 'name', children: 'children'}
const modelTreeExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey))
const tableRef = ref()
const modelRequestId = ref(0)
const treeLoading = ref(false)
const deleteLoading = ref(false)
const selectedRows = ref([])
const currentPageNum = ref(1)
const currentPageSize = ref(10)
const selectedModel = ref(null)
const sourceOptions = ref([{label: '全部', value: null}])
const currentModelColumns = ref([])
const tableStore = reactive({
  func: loadPage,
  params: {sourceRecordKey: '', sourceSystemId: null, businessFieldValue: '', governanceStatus: null, modelId: null},
  events: {formatParams, formatData},
  config: {
    sort: true,
    /*
     * QtTable 现有参数名是 initResquest；关闭首次自动请求，等默认模型选中后再查。
     */
    initResquest: false,
    table: {
      rowKey: 'id',
      stripe: true,
      height: 'calc(100vh - 394px)',
      scrollbarAlwaysOn: true,
      onSelectionChange: rows => {
        selectedRows.value = rows
      },
      defaultSort: {prop: 'lastReceiveTime', order: 'descending'}
    }
  },
  columns: buildTableColumns()
})

const searchStore = reactive({
  items: buildSearchItems()
})

/**
 * 生成当前列表的来源、业务字段和融合状态查询项。
 * @returns {Array<Object>} 查询栏配置。
 */
function buildSearchItems() {
  return [
    {label: '来源记录ID', prop: 'sourceRecordKey', component: {is: 'input', placeholder: '请输入来源记录ID'}},
    ...(!props.sourceSystemId ? [{
      label: '来源系统',
      prop: 'sourceSystemId',
      component: {is: 'select', options: sourceOptions}
    }] : []),
    {label: '业务字段', prop: 'businessFieldValue', component: {is: 'input', placeholder: '请输入业务字段内容'}},
    {label: '融合状态', prop: 'governanceStatus', component: {is: 'select', options: governanceStatusOptions}}
  ]
}

// 首次进入加载来源系统候选项；模型树由上面的公共取数方法自行加载。
onMounted(() => {
  loadSourceOptions()
})

/**
 * 按模型查询来源记录；未选模型时返回空分页数据。
 * @param {Object} params 表格分页及筛选参数。
 * @returns {Promise<Object>} 来源数据分页响应。
 */
function loadPage(params) {
  clearSelection()
  if (!params.modelId) return Promise.resolve({data: {rows: [], total: 0}})
  return listSourceData({...params, sourceSystemId: props.sourceSystemId || params.sourceSystemId})
}

/** 加载有效来源系统，供查询栏筛选来源记录。 */
async function loadSourceOptions() {
  try {
    const response = await listIngestSource({pageNum: 1, pageSize: 1000})
    const rows = response.data?.rows || []
    sourceOptions.value = [
      {label: '全部', value: null},
      ...rows
          .filter(item => String(item.validFlag ?? '1') === '1')
          .map(item => ({label: item.name, value: item.id}))
    ]
  } catch {
    proxy.$modal.msgError('加载来源系统失败')
  }
}

// 只监听接口返回的模型树，避免树组件重建配置时重复触发字段查询。
watch(modelTreeData, handleModelTreeChange)

/**
 * 模型树刷新后优先恢复原选中模型，无模型时清空表格配置。
 * @param {Array<Object>} nodes 模型树节点。
 */
async function handleModelTreeChange(nodes) {
  treeLoading.value = true
  try {
    const previousModelId = selectedModel.value?.id
    const defaultModel = findModelNode(nodes, previousModelId) || findModelNode(nodes)
    if (defaultModel) {
      await nextTick()
      await handleTreeClick(defaultModel, false)
    } else {
      clearSelection()
      modelRequestId.value++
      selectedModel.value = null
      currentModelColumns.value = []
      tableStore.params.modelId = null
      tableStore.columns = buildTableColumns()
      searchStore.items = buildSearchItems()
    }
  } finally {
    treeLoading.value = false
  }
}

/**
 * 深度优先查找模型节点，跳过项目与目录节点。
 * @param {Array<Object>} nodes 待遍历的树节点。
 * @param {string|number|null} [modelId] 指定模型 ID，省略时返回首个模型。
 * @returns {Object|null} 匹配的模型节点。
 */
function findModelNode(nodes, modelId = null) {
  for (const node of nodes) {
    if (node.modelId && (modelId == null || String(node.modelId) === String(modelId))) {
      return node
    }
    const childModel = findModelNode(node.children || [], modelId)
    if (childModel) return childModel
  }
  return null
}

/**
 * 点击目录时选中其下首个模型，重建动态列且仅应用当前模型请求的返回结果。
 * @param {Object} node 选中的树节点。
 * @param {boolean} [shouldAutoExpandParent=true] 是否展开父目录。
 */
async function handleTreeClick(node, shouldAutoExpandParent = true) {
  const modelNode = node?.modelId != null ? node : findModelNode(node?.children || [])
  if (!modelNode) return
  clearSelection()
  const requestId = ++modelRequestId.value
  selectedModel.value = {id: modelNode.modelId, label: modelNode.name, code: modelNode.code}
  currentModelColumns.value = []
  tableStore.params.modelId = modelNode.modelId
  tableStore.columns = buildTableColumns()
  searchStore.items = buildSearchItems()
  modelTreeRef.value?.setCurrentKey(modelNode.treeKey, shouldAutoExpandParent)
  try {
    const response = await getSourceDataModelColumns(modelNode.modelId)
    if (requestId !== modelRequestId.value) return
    currentModelColumns.value = response.data || []
    tableStore.columns = buildTableColumns(currentModelColumns.value)
    await nextTick()
    if (requestId === modelRequestId.value) tableRef.value?.resetQuery()
  } catch {
    /*
     * 新模型的表格已重建为空；错误提示复用请求拦截器。
     */
  }
}

/** 查询时补回当前模型 ID，限定来源数据所属模型。 */
function handleQuery() {
  tableStore.params.modelId = selectedModel.value?.id || null
}

/** 重置搜索条件时恢复默认模型，并同步树选择、动态列和列表范围。 */
function handleReset() {
  const defaultModel = findModelNode(modelTreeData.value)
  if (defaultModel) {
    handleTreeClick(defaultModel)
    return
  }

  modelRequestId.value++
  clearSelection()
  selectedModel.value = null
  currentModelColumns.value = []
  tableStore.params.modelId = null
  tableStore.columns = buildTableColumns()
  searchStore.items = buildSearchItems()
  modelTreeRef.value?.resetTree()
}

/**
 * 清理查询文本并转换空条件，组装来源数据接口参数。
 * @param {Object} params 表格查询参数。
 * @returns {Object} 接口请求参数。
 */
function formatParams(params) {
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    sourceRecordKey: params.sourceRecordKey?.trim() || undefined,
    sourceSystemId: params.sourceSystemId || undefined,
    businessFieldValue: params.businessFieldValue?.trim() || undefined,
    governanceStatus: params.governanceStatus || undefined,
    modelId: params.modelId || undefined,
    orderByColumn: params.orderByColumn || undefined,
    isAsc: params.isAsc || undefined
  }
}

/**
 * 展开业务 JSON 并保留平台字段，更新当前模型的分页序号信息。
 * @param {Array<Object>} data 来源记录。
 * @param {Object} params 本次查询参数。
 * @returns {Array<Object>} 表格展示数据。
 */
function formatData(data, params) {
  if ((selectedModel.value?.id || undefined) === params.modelId) {
    currentPageNum.value = params.pageNum || 1
    currentPageSize.value = params.pageSize || 10
  }
  return data.map(row => ({...parseModelData(row.modelData), ...row}))
}

/**
 * 按模型字段生成业务列，并拼接来源信息、融合状态及操作列。
 * @param {Array<Object>} [modelColumns] 模型业务字段定义。
 * @returns {Array<Object>} 表格列配置。
 */
function buildTableColumns(modelColumns = []) {
  const businessColumns = modelColumns.map(buildBusinessColumn)
  const overviewColumns = modelColumns.length
      ? []
      : [{label: '主数据模型', prop: 'modelName', minWidth: 160, align: 'center'}]
  return [
    {
      type: 'selection',
      width: 48,
      minWidth: 48,
      fixed: 'left',
      selectable: row => isPendingGovernanceStatus(row.governanceStatus)
    },
    rowIndexColumn(),
    {label: '主数据模型', prop: 'modelName', slot: 'modelInfo', minWidth: 320, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '接入任务信息', prop: 'taskName', slot: 'taskInfo', minWidth: 280, align: 'left'},
    {label: '来源系统信息', prop: 'sourceSystemName', slot: 'sourceSystemInfo', minWidth: 280, align: 'left'},
    ...overviewColumns,
    ...businessColumns,
    {label: '融合状态', prop: 'governanceStatus', slot: 'governanceStatus', width: 115, minWidth: 115, align: 'center'},
    {
      label: '形成时间',
      prop: 'lastReceiveTime',
      width: 180,
      minWidth: 180,
      date: true,
      sortable: 'custom',
      align: 'center'
    },
    {label: '操作', slot: 'action', width: 160, minWidth: 160, fixed: 'right', align: 'center'}
  ]
}

/** 统一显示实体名称及可用编码。 */
function formatEntityTitle(name, code) {
  if (!name) return '-'
  return code ? `${name} (${code})` : name
}

function getVersionTagOptions(row) {
  const status = MODEL_STATUS_OPTIONS.find(item => String(item.value) === String(row.modelVersionStatus))
  return [{
    value: row.modelVersionStatus ?? row.modelVersionNo,
    label: row.modelVersionNo,
    elTagType: status?.elTagType || 'info',
  }]
}

function openModelDetail(row, tab) {
  if (row.modelId == null) return
  router.push({
    path: '/model/base/detail',
    query: {id: row.modelId, ...(tab ? {tab} : {})},
  })
}

/**
 * 生成跨页连续编号列，序号由分页信息计算。
 * @returns {Object} 编号列配置。
 */
function rowIndexColumn() {
  return {label: '编号', prop: 'id', slot: 'rowIndex', width: 40, minWidth: 40, sortable: 'custom', align: 'center'}
}

/**
 * 按模型字段名称及编码生成业务列，复用统一值展示插槽。
 * @param {Object} column 模型字段定义。
 * @returns {Object} 业务列配置。
 */
function buildBusinessColumn(column) {
  return {
    label: column.columnName,
    prop: column.columnCode,
    slot: column.nameFlag ? 'businessName' : 'businessValue',
    minWidth: 140,
    align: 'left',
    showOverflowTooltip: {effect: 'light'}
  }
}

/**
 * 空业务值显示占位符，对象序列化为 JSON 文本。
 * @param {*} value 业务字段值。
 * @returns {string} 展示文本。
 */
function displayBusinessValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

/** 解析业务 JSON 字符串，空值或解析失败时返回空对象。 */
function parseModelData(modelData) {
  try {
    return modelData ? JSON.parse(modelData) : {}
  } catch {
    return {}
  }
}

/**
 * 携带记录、模型及来源标识跳转详情，供详情与历史版本查询使用。
 * @param {Object} row 来源记录。
 */
function openDetail(row) {
  router.push({
    path: '/ingest/sourceData/detail',
    query: {
      id: row.id,
      modelId: selectedModel.value?.id,
      sourceSystemId: row.sourceSystemId,
      sourceRecordKey: row.sourceRecordKey,
    }
  })
}

/** 清空当前页勾选，避免刷新或切换模型后保留旧记录。 */
function clearSelection() {
  selectedRows.value = []
  tableRef.value?.elTableRef?.clearSelection()
}

/** 删除单条待融合来源数据。 */
function handleDelete(row) {
  return removeSourceRecords([row], false)
}

/** 批量删除当前页勾选的待融合来源数据。 */
function handleBatchDelete() {
  return removeSourceRecords([...selectedRows.value], true)
}

/** 复用单条删除接口，汇总执行结果并刷新对应模型的列表。 */
async function removeSourceRecords(rows, batch) {
  if (deleteLoading.value || !rows.length) return
  if (rows.some(row => !isPendingGovernanceStatus(row.governanceStatus))) return
  const requestId = modelRequestId.value
  deleteLoading.value = true
  try {
    try {
      await proxy.$modal.confirm(batch
          ? `确认删除选中的 ${rows.length} 条待融合来源数据？`
          : '确认删除该待融合来源数据？')
    } catch {
      return
    }
    let successCount = 0
    for (const row of rows) {
      try {
        await deleteSourceData(row.id, row.modelId)
        successCount++
      } catch {
        // 请求拦截器展示失败原因，其余选中记录继续处理。
      }
    }
    if (successCount === rows.length) {
      proxy.$modal.msgSuccess(batch ? `成功删除 ${successCount} 条来源数据` : '删除成功')
    } else if (batch) {
      proxy.$modal.msgWarning(`成功删除 ${successCount} 条，失败 ${rows.length - successCount} 条，请刷新后重试`)
    }
    if (requestId === modelRequestId.value) {
      clearSelection()
      if (batch) tableRef.value?.resetQuery()
      else tableRef.value?.getList()
    }
  } finally {
    deleteLoading.value = false
  }
}

</script>

<style scoped>
.model-info {
  min-width: 0;
}

.model-info-title {
  display: block;
}

.model-info-title[role='link'] {
  cursor: pointer;
  color: var(--el-color-primary);
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}
</style>

