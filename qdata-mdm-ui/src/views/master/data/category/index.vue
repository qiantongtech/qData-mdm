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
  <div class="app-container authority-category-page">
    <ModelSummary :detail="detail" model-type="CATEGORY" @back="goBack" />
    <el-container>
      <el-main class="main-content">
        <ModelDataList
          v-model:view="activeView"
          :instance-id="instanceId"
          :params="queryParams"
          :columns="columns"
          :table-ref="dataTableRef"
          :source-system-select="sourceSystemSelect"
          :status-options="master_data_status"
          :approval-status-options="master_change_request_status"
          :search-disabled="!metadata || metadataLoading || !!metadataError"
        >
          <template #actions>
            <el-button type="primary" plain :disabled="!metadata || metadataLoading" v-hasPermi="['master:instance:add']" @click="openCreate()" @mousedown="event => event.preventDefault()">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
            <el-button type="primary" plain icon="Edit" :disabled="!selectedRows.length"
                       v-hasPermi="['master:instance:edit']" @click="openBatch('BATCH_UPDATE')" @mousedown="event => event.preventDefault()"
            >批量修改</el-button>
            <el-button type="info" plain icon="Upload" :disabled="!metadata || metadataLoading" v-hasPermi="['master:instance:import']" @click="openBatch('BATCH_IMPORT')"
                       @mousedown="event => event.preventDefault()"
            >批量导入</el-button>
            <el-button type="warning" plain icon="Download" :loading="exportLoading" :disabled="!total"
                       v-hasPermi="['master:instance:export']" @click="exportData" @mousedown="event => event.preventDefault()"
            >批量导出</el-button>
            <el-button type="primary" plain icon="Refresh" :disabled="!selectedRows.length"
                       v-hasPermi="['master:instance:status']" @click="openBatch('BATCH_STATUS')" @mousedown="event => event.preventDefault()"
            >批量状态</el-button>
            <el-button class="toggle-expand-all" type="primary" plain @click="toggleExpandAll">
              <svg-icon v-if="defaultExpandAll" icon-class="toggle" />
              <svg-icon v-else icon-class="expand" />
              <span>{{ defaultExpandAll ? '折叠' : '展开' }}</span>
            </el-button>
          </template>
          <el-skeleton v-if="metadataLoading && !metadata" :rows="5" animated />
          <DataTable
            v-show="!!metadata"
            ref="dataTableRef"
            :context-key="categoryTableKey"
            :columns="columns"
            :func="getList"
            :params="queryParams"
            tree
            :default-expand-all="defaultExpandAll"
            :status-options="master_data_status"
            :approval-status-options="master_change_request_status"
            @selection-change="handleSelectionChange"
            @view="openDetail"
            @modify="openModify"
            @copy="openCopy"
            @status="openStatus"
            @add-child="openCreate"
          />
        </ModelDataList>
      </el-main>
    </el-container>

    <MaintenanceFormDialog v-if="metadata && checkPermi([maintenanceMode === 'update' ? 'master:instance:edit' : 'master:instance:add'])" v-model="maintenanceVisible" :instance-id="instanceId" :mode="maintenanceMode"
                           :record-id="maintenanceRecordId" :model-id="metadata?.modelId"
                           :model-version-id="metadata?.modelVersionId"
                           :model-type="detail.modelType" :parent-id="maintenanceParentId"
                           :category-tree="treeData"
                           @saved="handleMaintenanceSaved"
/>
    <LifecyclePreparationDialog v-if="metadata && checkPermi(['master:instance:status'])" v-model="lifeVisible" :instance-id="instanceId" :record-id="lifeRecordId" :model-id="metadata.modelId"
                                :action="lifeAction" @saved="handleMaintenanceSaved"
/>
    <BatchMaintenanceDialog v-if="checkPermi([batchMode === 'BATCH_IMPORT' ? 'master:instance:import' : batchMode === 'BATCH_UPDATE' ? 'master:instance:edit' : 'master:instance:status'])" v-model="batchVisible" :instance-id="instanceId" :mode="batchMode"
                            :model-id="metadata?.modelId" :model-version-id="metadata?.modelVersionId"
                            :selected-rows="selectedRows" :record-ids="batchRecordIds"
                            :columns="businessColumns" :model-name="detail.modelName"
                            :model-type="detail.modelType"
                            @saved="handleMaintenanceSaved" @submitted="handleMaintenanceSaved"
/>
  </div>
</template>

<script setup name="MasterDataCategory">
import { computed, getCurrentInstance, nextTick, onUnmounted, reactive, ref, watch } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import { usePageRefresh } from '@/composables/usePageRefresh'
import { checkPermi } from '@/utils/permission'
import { useDict } from '@/utils/dict'
import { getMasterCategoryData, getMasterDataMetadata, getMasterTree, getMasterSourceSystems } from '@/api/master/data/data'
import DataTable from '../components/DataTable.vue'
import ModelDataList from '../components/ModelDataList.vue'
import MaintenanceFormDialog from '../../change/request/components/MaintenanceFormDialog.vue'
import LifecyclePreparationDialog from '../../change/request/components/LifecyclePreparationDialog.vue'
import BatchMaintenanceDialog from '../../change/request/components/BatchMaintenanceDialog.vue'
import ModelSummary from '../../instance/components/ModelSummary.vue'

const METADATA_HEADER_KEYS = ['modelName', 'modelCode', 'modelType', 'domainName', 'versionNo']
const METADATA_COUNT_KEYS = ['dataCount', 'activeCount', 'frozenCount', 'invalidCount', 'archivedCount', 'pendingMaintenanceApprovalCount']
const OMITTED_LIST_FIELDS = new Set(['UPDATED_BY', 'UPDATE_BY', 'UPDATED_TIME', 'UPDATE_TIME', 'REMARK'])
const TRAILING_LIST_FIELDS = ['STATUS', 'CREATED_BY', 'CREATE_BY', 'CREATED_TIME', 'CREATE_TIME']

const route = useRoute()
const router = useRouter()
const { setRefreshNeeded } = usePageRefresh('masterInstance')
const routeBase = computed(() => '/master/instance')
const { proxy } = getCurrentInstance()
const { master_data_status, master_change_request_status } = useDict(
  'master_data_status', 'master_change_request_status'
)
const instanceId = computed(() => route.query.instanceId)
const modelId = computed(() => route.query.modelId)
const modelVersionId = computed(() => route.query.modelVersionId)
const sourceLoading = ref(false)
let latestSourceRequest = 0
const queryParams = reactive({ name: undefined, status: undefined,
  sourceSystem: undefined, approvalStatus: undefined })
const sourceSystemSelect = reactive({
  is: 'select', options: [], disabled: true,
  loading: sourceLoading, placeholder: '正在加载来源系统'
})
const columns = ref([])
const businessColumns = computed(() => columns.value.filter(column => column.businessField === true))
const metadata = ref(null)
const metadataLoading = ref(false)
const metadataError = ref('')
let pendingMetadataRequest = null
let metadataRequestId = 0
let loadedMetadataKey = ''
const treeData = ref([])
const defaultExpandAll = ref(false)
const total = ref(0)
const exportLoading = ref(false)
let latestRequest = 0
let loadedTreeKey = ''
const dataTableRef = ref(null)
const selectedRows = ref([])
const batchRecordIds = ref([])
const activeView = ref('formal')
const maintenanceVisible = ref(false)
const maintenanceMode = ref('create')
const maintenanceRecordId = ref('')
const maintenanceParentId = ref('')
const lifeVisible = ref(false)
const lifeRecordId = ref('')
const lifeAction = ref('')
const batchVisible = ref(false)
const batchMode = ref('BATCH_UPDATE')
const detail = reactive({ modelName: '', modelCode: '', modelType: '', domainName: '', versionNo: '',
  dataCount: null, activeCount: null, frozenCount: null, invalidCount: null, archivedCount: null,
  latestDataUpdateTime: '', pendingMaintenanceApprovalCount: null })
const categoryTableKey = computed(() => [instanceId.value, metadata.value?.modelId,
  metadata.value?.modelVersionId].join(':'))

function buildListColumns(definitionColumns, modelType) {
  const visible = definitionColumns.filter(column => !OMITTED_LIST_FIELDS.has(String(column.prop).toUpperCase()))
  const idColumn = visible.find(column => String(column.prop).toUpperCase() === 'ID')
  const business = visible.filter(column => column.businessField === true && String(column.prop).toUpperCase() !== 'ID')
  const system = visible.filter(column => column.businessField !== true)
  return [
    {
      ...idColumn,
      prop: 'ID',
      label: '编号',
      dataType: idColumn?.dataType || 'BIGINT',
      businessField: false,
      nameField: false,
      visible: true,
      hide: modelType === 'CATEGORY'
    },
    ...business,
    ...system.filter(column => String(column.prop).toUpperCase() !== 'ID'
      && !TRAILING_LIST_FIELDS.includes(String(column.prop).toUpperCase())),
    ...TRAILING_LIST_FIELDS.flatMap(prop => system.filter(column => String(column.prop).toUpperCase() === prop))
  ]
}

function getMetadataContextKey() {
  return [instanceId.value, modelId.value, modelVersionId.value].map(value => value ?? '').join(':')
}

function clearMetadataDefinition() {
  metadata.value = null
  loadedMetadataKey = ''
  columns.value = []
  for (const key of METADATA_HEADER_KEYS) detail[key] = ''
  for (const key of METADATA_COUNT_KEYS) detail[key] = null
  detail.latestDataUpdateTime = ''
}

function resetMetadata() {
  metadataRequestId++
  clearMetadataDefinition()
  metadataLoading.value = false
  metadataError.value = ''
  pendingMetadataRequest = null
}

// 加载当前发布定义；同一上下文合并请求，刷新时保留用户设置的列显隐。
function loadMetadata(force = false) {
  const requestedId = String(instanceId.value || '')
  if (!requestedId) return Promise.resolve(null)
  const requestedKey = getMetadataContextKey()
  if (pendingMetadataRequest?.key === requestedKey) return pendingMetadataRequest.promise
  if (!force && metadata.value && loadedMetadataKey === requestedKey) return Promise.resolve(metadata.value)
  const expectedModelId = modelId.value
  const expectedVersionId = modelVersionId.value
  const requestId = ++metadataRequestId
  metadataLoading.value = true
  metadataError.value = ''
  const promise = getMasterDataMetadata(requestedId, { modelId: expectedModelId || undefined }).then(response => {
    if (requestId !== metadataRequestId || getMetadataContextKey() !== requestedKey) return null
    const definition = response?.data
    if (!definition || String(definition.instanceId) !== requestedId || definition.modelVersionId == null
      || (expectedModelId && String(definition.modelId) !== String(expectedModelId))
      || (expectedVersionId && String(definition.modelVersionId) !== String(expectedVersionId))) {
      throw new Error('列表定义与当前发布实例不匹配，请返回卡片列表后重新进入')
    }
    const hiddenColumns = new Map(columns.value.map(column => [column.prop, column.hide]))
    columns.value = buildListColumns(definition.columns || [], definition.modelType).map(column => ({
      ...column,
      hide: hiddenColumns.get(column.prop) ?? column.hide
    }))
    for (const key of METADATA_HEADER_KEYS) detail[key] = definition[key] ?? ''
    metadata.value = definition
    loadedMetadataKey = requestedKey
    return definition
  }).catch(error => {
    if (requestId !== metadataRequestId || getMetadataContextKey() !== requestedKey) return null
    clearMetadataDefinition()
    metadataError.value = error?.msg || error?.message || '模型定义加载失败，请稍后重试'
    throw error
  }).finally(() => {
    if (requestId === metadataRequestId) {
      pendingMetadataRequest = null
      metadataLoading.value = false
    }
  })
  pendingMetadataRequest = { key: requestedKey, promise }
  return promise
}

async function getList(params = queryParams) {
  const requestedId = instanceId.value
  const empty = { data: { rows: [], total: 0 } }
  if (!requestedId || !checkPermi(['master:instance:list'])) return empty
  const requestId = ++latestRequest
  dataTableRef.value?.clearSelection()
  selectedRows.value = []
  for (let attempt = 0; attempt < 2; attempt++) {
    const definition = await loadMetadata(attempt > 0)
    if (!definition || String(instanceId.value) !== String(requestedId) || requestId !== latestRequest) return empty
    if (definition.modelType === 'NORMAL') {
      router.replace({ path: `${routeBase.value}/data`, query: { ...route.query, instanceId: requestedId } })
      return empty
    }
    if (definition.modelType !== 'CATEGORY') throw new Error('当前模型不是可用的分类模型')
    const response = await getMasterCategoryData(requestedId, {
      ...params, modelId: definition.modelId, modelVersionId: definition.modelVersionId
    })
    if (String(instanceId.value) !== String(requestedId) || requestId !== latestRequest) return empty
    const data = response?.data || {}
    if (String(data.modelId) !== String(definition.modelId)
        || String(data.instanceId) !== String(definition.instanceId)
        || String(data.modelVersionId) !== String(definition.modelVersionId)) continue
    total.value = Number(data.total || 0)
    Object.assign(detail, data)
    return response
  }
  throw new Error('模型版本发生变化，请刷新页面后重试')
}

async function exportData() {
  if (!checkPermi(['master:instance:export'])) return
  if (!instanceId.value || !total.value) return
  exportLoading.value = true
  try {
    await proxy.download(`master/data/export/${instanceId.value}`,
      { ...queryParams, modelId: metadata.value?.modelId, modelVersionId: metadata.value?.modelVersionId }, `authority_${instanceId.value}_${Date.now()}.xlsx`)
  } finally {
    exportLoading.value = false
  }
}

function handleMaintenanceSaved() {
  setRefreshNeeded()
  return refreshData()
}

async function refreshData() {
  latestRequest++
  if (!await loadMetadata(true)) return
  await loadSourceSystems()
  loadedTreeKey = ''
  dataTableRef.value?.getList()
}

// 来源筛选独立请求，不随列表翻页、状态查询重复加载。
async function loadSourceSystems() {
  const requestedId = String(instanceId.value || '')
  const requestedModelId = String(modelId.value || '')
  const requestId = ++latestSourceRequest
  const sourceSelect = sourceSystemSelect
  sourceLoading.value = true
  sourceSelect.disabled = true
  sourceSelect.placeholder = '正在加载来源系统'
  if (!requestedId) {
    sourceLoading.value = false
    return
  }
  try {
    const response = await getMasterSourceSystems(requestedId, { modelId: requestedModelId || undefined })
    if (requestId !== latestSourceRequest || String(instanceId.value) !== requestedId
        || String(modelId.value || '') !== requestedModelId) return
    sourceSelect.options = (response?.data || []).map(item => ({ label: item.name, value: String(item.id) }))
    sourceSelect.disabled = sourceSelect.options.length === 0
    sourceSelect.placeholder = sourceSelect.options.length ? '请选择来源系统' : '暂无关联来源系统'
    if (queryParams.sourceSystem && !sourceSelect.options.some(item => item.value === String(queryParams.sourceSystem))) {
      queryParams.sourceSystem = undefined
    }
  } catch {
    if (requestId === latestSourceRequest) {
      sourceSelect.options = []
      sourceSelect.placeholder = '来源系统加载失败'
    }
  } finally {
    if (requestId === latestSourceRequest) sourceLoading.value = false
  }
}

function toggleExpandAll() {
  defaultExpandAll.value = !defaultExpandAll.value
  dataTableRef.value?.clearSelection()
  selectedRows.value = []
  dataTableRef.value?.reload()
}

async function loadCategoryOptions() {
  const requestedId = String(instanceId.value || '')
  const versionId = metadata.value?.modelVersionId
  if (!requestedId || versionId == null) return false
  const treeKey = `${requestedId}:${versionId}`
  if (loadedTreeKey === treeKey) return true
  const response = await getMasterTree(requestedId)
  if (`${instanceId.value}:${metadata.value?.modelVersionId}` !== treeKey) return false
  treeData.value = response?.data || []
  loadedTreeKey = treeKey
  return true
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

async function openCreate(parent) {
  if (!checkPermi(['master:instance:add'])) return
  if (!await loadCategoryOptions()) return
  maintenanceMode.value = 'create'
  maintenanceRecordId.value = ''
  maintenanceParentId.value = parent?.ID == null ? '' : String(parent.ID)
  maintenanceVisible.value = true
}

async function openModify(row) {
  if (!checkPermi(['master:instance:edit'])) return
  if (!await loadCategoryOptions()) return
  maintenanceMode.value = 'update'
  maintenanceRecordId.value = String(row.ID)
  maintenanceParentId.value = String(row.CATEGORY_ID ?? '')
  maintenanceVisible.value = true
}

async function openCopy(row) {
  if (!checkPermi(['master:instance:add'])) return
  if (!await loadCategoryOptions()) return
  maintenanceMode.value = 'copy'
  maintenanceRecordId.value = String(row.ID)
  maintenanceParentId.value = String(row.CATEGORY_ID ?? '')
  maintenanceVisible.value = true
}

function openStatus(row, action) {
  if (!checkPermi(['master:instance:status'])) return
  lifeRecordId.value = String(row.ID)
  lifeAction.value = action
  lifeVisible.value = true
}

function openBatch(mode) {
  const permission = {
    BATCH_UPDATE: 'master:instance:edit',
    BATCH_IMPORT: 'master:instance:import',
    BATCH_STATUS: 'master:instance:status'
  }[mode]
  if (typeof permission !== 'string' || !checkPermi([permission])) return
  if (mode !== 'BATCH_IMPORT' && !selectedRows.value.length) return
  batchRecordIds.value = mode === 'BATCH_IMPORT'
    ? []
    : selectedRows.value
      .map(row => row?.ID)
      .filter(id => id !== null && id !== undefined && id !== '')
      .map(String)
  batchMode.value = mode
  batchVisible.value = true
}

function openDetail(row) {
  if (!checkPermi(['master:instance:query'])) return
  router.push({
    path: `${routeBase.value}/detail/${row.ID}`,
    query: { ...route.query, instanceId: instanceId.value, modelId: metadata.value?.modelId,
      modelVersionId: metadata.value?.modelVersionId, from: 'category' }
  })
}

function goBack() {
  router.push(routeBase.value)
}

watch([modelId, instanceId, modelVersionId], (_, previous) => {
  latestRequest++
  latestSourceRequest++
  sourceLoading.value = false
  const sourceSelect = sourceSystemSelect
  sourceSelect.options = []
  sourceSelect.disabled = true
  sourceSelect.placeholder = '正在加载来源系统'
  maintenanceVisible.value = false
  lifeVisible.value = false
  batchVisible.value = false
  activeView.value = 'formal'
  Object.assign(queryParams, { name: undefined, status: undefined, sourceSystem: undefined, approvalStatus: undefined })
  selectedRows.value = []
  total.value = 0
  treeData.value = []
  defaultExpandAll.value = false
  maintenanceParentId.value = ''
  resetMetadata()
  loadedTreeKey = ''
  loadMetadata().catch(() => {})
  loadSourceSystems()
  // 首次由表格自行加载；复用路由组件切换模型时才主动刷新。
  if (previous?.some(Boolean)) nextTick(() => dataTableRef.value?.getList())
}, { immediate: true })

onUnmounted(() => {
  latestRequest++
  latestSourceRequest++
  resetMetadata()
})

</script>
