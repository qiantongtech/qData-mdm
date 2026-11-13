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
  <div class="app-container master-authority-data-page">
    <ModelSummary :detail="detail" model-type="NORMAL" @back="goBack" />
    <el-container>
      <ClassificationDataTree v-if="activeView === 'formal' && (classificationFields.length || classificationError)"
                              :groups="classificationGroups" :selected-id="classificationSelectedKey" :loading="classificationLoading"
                              @select="selectClassification"
      />
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
          :toolbar-ref="{ getList: refreshData }"
        >
          <template #actions>
            <el-button type="primary" plain v-hasPermi="['master:instance:add']" @click="openCreate" @mousedown="event => event.preventDefault()">
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
            <el-button type="primary" plain icon="Edit" :disabled="!selectedRows.length"
                       v-hasPermi="['master:instance:edit']" @click="openBatch('BATCH_UPDATE')" @mousedown="event => event.preventDefault()"
            >批量修改</el-button>
            <el-button type="info" plain icon="Upload" v-hasPermi="['master:instance:import']" @click="openBatch('BATCH_IMPORT')"
                       @mousedown="event => event.preventDefault()"
            >批量导入</el-button>
            <el-button type="warning" plain icon="Download" :loading="exportLoading" :disabled="!total"
                       v-hasPermi="['master:instance:export']" @click="exportData" @mousedown="event => event.preventDefault()"
            >批量导出</el-button>
            <el-button type="primary" plain icon="Refresh" :disabled="!selectedRows.length"
                       v-hasPermi="['master:instance:status']" @click="openBatch('BATCH_STATUS')" @mousedown="event => event.preventDefault()"
            >批量状态</el-button>
          </template>
          <el-skeleton v-if="metadataLoading && !metadata" :rows="5" animated />
          <data-table
            v-show="!!metadata"
            ref="dataTableRef"
            :context-key="classificationTableKey"
            :columns="columns"
            :func="getList"
            :params="queryParams"
            :status-options="master_data_status"
            :approval-status-options="master_change_request_status"
            @selection-change="handleSelectionChange"
            @view="openDetail"
            @modify="openModify"
            @copy="openCopy"
            @status="openStatus"
          />
        </ModelDataList>
      </el-main>
    </el-container>
    <MaintenanceFormDialog v-if="metadata && checkPermi([maintenanceMode === 'update' ? 'master:instance:edit' : 'master:instance:add'])" v-model="maintenanceVisible" :instance-id="instanceId" :mode="maintenanceMode"
                           :record-id="maintenanceRecordId" :model-id="metadata?.modelId"
                           :model-version-id="metadata?.modelVersionId"
                           :model-type="detail.modelType" :initial-values="classificationPrefill" @saved="handleMaintenanceSaved"
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

<script setup name="MasterData">
import { computed, getCurrentInstance, nextTick, onActivated, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

import { useRoute, useRouter } from 'vue-router'

import { usePageRefresh } from '@/composables/usePageRefresh'
import { checkPermi } from '@/utils/permission'
import { useDict } from '@/utils/dict'
import { getMasterClassifications, getMasterData, getMasterDataMetadata, getMasterSourceSystems } from '@/api/master/data/data'
import DataTable from './components/DataTable.vue'
import ModelDataList from './components/ModelDataList.vue'
import MaintenanceFormDialog from '../change/request/components/MaintenanceFormDialog.vue'
import LifecyclePreparationDialog from '../change/request/components/LifecyclePreparationDialog.vue'
import BatchMaintenanceDialog from '../change/request/components/BatchMaintenanceDialog.vue'
import ModelSummary from '../instance/components/ModelSummary.vue'
import ClassificationDataTree from './components/ClassificationDataTree.vue'

const METADATA_HEADER_KEYS = ['modelName', 'modelCode', 'modelType', 'domainName', 'versionNo']
const METADATA_COUNT_KEYS = ['dataCount', 'activeCount', 'frozenCount', 'invalidCount', 'archivedCount', 'pendingMaintenanceApprovalCount']
const OMITTED_LIST_FIELDS = new Set(['UPDATED_BY', 'UPDATE_BY', 'UPDATED_TIME', 'UPDATE_TIME', 'REMARK'])
const TRAILING_LIST_FIELDS = ['STATUS', 'CREATED_BY', 'CREATE_BY', 'CREATED_TIME', 'CREATE_TIME']

const route = useRoute()
const router = useRouter()
const { setRefreshNeeded } = usePageRefresh('masterInstance')
const routeBase = computed(() => '/master/instance')
const { proxy } = getCurrentInstance()
const instanceId = computed(() => route.query.instanceId)
const modelId = computed(() => route.query.modelId)
const modelVersionId = computed(() => route.query.modelVersionId)
const { master_data_status, master_change_request_status } = useDict(
  'master_data_status', 'master_change_request_status'
)

const queryParams = reactive({ pageNum: 1, pageSize: 10, name: undefined, status: undefined, treeId: undefined,
  sourceSystem: undefined, approvalStatus: undefined })
const sourceSystemSelect = reactive({
  is: 'select', options: [], disabled: true, placeholder: '暂无来源系统'
})
const columns = ref([])
const businessColumns = computed(() => columns.value.filter(column => column.businessField === true))
const metadata = ref(null)
const metadataLoading = ref(false)
let pendingMetadataRequest = null
let metadataRequestId = 0
let loadedMetadataKey = ''

const classificationFields = ref([])
const classificationFieldId = ref('')
const classificationTrees = ref({})
const classificationSelectedKey = ref('__all__')
const classificationNode = ref(null)
const classificationLoading = ref(false)
const classificationError = ref('')
const activeClassificationField = computed(() => classificationFields.value
  .find(field => String(field.id) === classificationFieldId.value))
const activeClassificationTree = computed(() => classificationTrees.value[classificationFieldId.value])
const classificationGroups = computed(() => {
  const references = new Map()
  for (const field of classificationFields.value) {
    if (!field.targetModelId) continue
    const id = String(field.targetModelId)
    references.set(id, (references.get(id) || 0) + 1)
  }
  return classificationFields.value.map(field => {
    const fieldId = String(field.id)
    const name = field.targetModelName
    const repeated = name && references.get(String(field.targetModelId)) > 1
    return {
      id: `field:${fieldId}`,
      kind: 'field',
      fieldId,
      label: name ? `${name}${repeated ? `（${field.label}）` : ''}` : field.label,
      children: buildClassificationNodes(classificationTrees.value[fieldId]?.nodes, fieldId)
    }
  })
})
const classificationPrefill = computed(() => !classificationLoading.value && activeClassificationTree.value
  && !classificationNode.value?.unavailable && classificationNode.value?.assignable && activeClassificationField.value
  ? { [activeClassificationField.value.code]: classificationNode.value.value } : {})
let classificationContextKey = ''
let loadedClassificationKey = ''
let classificationRequestId = 0
let pendingClassificationRequest = null
const selectedRows = ref([])
const batchRecordIds = ref([])
const dataTableRef = ref(null)
const total = ref(0)
const exportLoading = ref(false)
let latestRequest = 0
let latestSourceRequest = 0
const activeView = ref('formal')
const maintenanceVisible = ref(false)
const maintenanceMode = ref('create')
const maintenanceRecordId = ref('')
const lifeVisible = ref(false)
const lifeRecordId = ref('')
const lifeAction = ref('')
const batchVisible = ref(false)
const batchMode = ref('BATCH_UPDATE')
const detail = reactive({ modelName: '', modelCode: '', modelType: '', domainName: '', versionNo: '',
  dataCount: null, activeCount: null, frozenCount: null, invalidCount: null, archivedCount: null,
  latestDataUpdateTime: '', pendingMaintenanceApprovalCount: null })
const classificationTableKey = computed(() => [instanceId.value, metadata.value?.modelVersionId,
  classificationFieldId.value, classificationNode.value?.id || '__all__'].join(':'))

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

function buildClassificationNodes(nodes, fieldId) {
  return (nodes || []).map(node => ({
    ...node,
    id: `field:${fieldId}:node:${node.id}`,
    nodeId: String(node.id),
    fieldId,
    kind: 'classification',
    children: buildClassificationNodes(node.children, fieldId)
  }))
}

function findClassificationNode(nodes, id) {
  for (const node of nodes || []) {
    if (String(node.id) === String(id)) return node
    const child = findClassificationNode(node.children, id)
    if (child) return child
  }
  return null
}

function resetClassificationNavigation() {
  classificationRequestId++
  classificationContextKey = ''
  loadedClassificationKey = ''
  pendingClassificationRequest = null
  classificationFields.value = []
  classificationFieldId.value = ''
  classificationTrees.value = {}
  classificationSelectedKey.value = '__all__'
  classificationNode.value = null
  classificationLoading.value = false
  classificationError.value = ''
}

function setClassificationSelection(node) {
  if (node.id === '__all__') {
    classificationFieldId.value = ''
    classificationSelectedKey.value = '__all__'
    classificationNode.value = null
    return
  }
  const field = classificationFields.value.find(item => String(item.id) === String(node.fieldId))
  if (!field) throw new Error('分类字段不属于当前模型')
  classificationFieldId.value = String(field.id)
  classificationSelectedKey.value = node.id
  classificationNode.value = node.kind === 'field' ? null : { ...node, id: String(node.nodeId) }
}

// 刷新分类树后恢复原选择；节点失效时保留阻断态，避免扩大查询范围。
function restoreClassificationSelection(previous) {
  if (!previous || previous.key === '__all__') {
    setClassificationSelection({ id: '__all__', kind: 'all' })
    return
  }
  if (!previous.node) {
    const group = classificationGroups.value.find(item => item.fieldId === previous.fieldId)
    setClassificationSelection(group || { id: '__all__', kind: 'all' })
    return
  }
  classificationFieldId.value = previous.fieldId
  const node = findClassificationNode(classificationTrees.value[previous.fieldId]?.nodes, previous.node.id)
  classificationSelectedKey.value = previous.key
  classificationNode.value = node || { ...previous.node, unavailable: true }
}

// 按发布定义加载分类导航，仅缓存成功响应，失败后允许再次加载。
function loadClassificationNavigation(definition, force = false) {
  const nextKey = [definition.instanceId, definition.modelId, definition.modelVersionId].map(String).join(':')
  if (!force && pendingClassificationRequest?.key === nextKey) return pendingClassificationRequest.promise
  if (!force && loadedClassificationKey === nextKey) return Promise.resolve()
  const previous = nextKey === classificationContextKey
    ? { fieldId: classificationFieldId.value, node: classificationNode.value, key: classificationSelectedKey.value }
    : null
  if (nextKey !== classificationContextKey) resetClassificationNavigation()
  classificationContextKey = nextKey
  const requestId = ++classificationRequestId
  classificationLoading.value = true
  classificationError.value = ''
  const promise = (async () => {
    try {
      const response = await getMasterClassifications(definition.instanceId, {
        modelId: definition.modelId,
        modelVersionId: definition.modelVersionId
      })
      if (requestId !== classificationRequestId) return
      const navigation = response?.data || []
      classificationFields.value = navigation
      classificationTrees.value = Object.fromEntries(navigation.filter(field => field.available)
        .map(field => [String(field.id), field]))
      classificationError.value = navigation.filter(field => !field.available)
        .map(field => field.message || '当前分类字段不可用').join('；')
      restoreClassificationSelection(previous)
      loadedClassificationKey = nextKey
    } catch (error) {
      if (requestId === classificationRequestId) {
        loadedClassificationKey = ''
        classificationTrees.value = {}
        classificationError.value = error?.msg || error?.message || '分类导航加载失败'
        restoreClassificationSelection(previous)
      }
    } finally {
      if (requestId === classificationRequestId) {
        classificationLoading.value = false
        pendingClassificationRequest = null
      }
    }
  })()
  pendingClassificationRequest = { key: nextKey, promise }
  return promise
}

// 组装当前分类的列表及导出筛选参数。
function getClassificationFilterParams() {
  if (!classificationNode.value) return {}
  if (classificationNode.value.unavailable || !activeClassificationTree.value || !activeClassificationField.value) {
    throw new Error('当前分类不可用')
  }
  return {
    classificationFieldId: activeClassificationField.value.id,
    classificationNodeId: classificationNode.value.id,
    classificationTargetVersionId: activeClassificationTree.value.targetVersionId,
    classificationRevision: activeClassificationTree.value.revision
  }
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
    if (definition.modelType === 'CATEGORY') {
      router.replace({ path: `${routeBase.value}/category`, query: { ...route.query, instanceId: requestedId } })
      return empty
    }
    await loadClassificationNavigation(definition)
    if (String(instanceId.value) !== String(requestedId) || requestId !== latestRequest) return empty
    const response = await getMasterData(requestedId, {
      ...params, ...getClassificationFilterParams(), modelId: definition.modelId, modelVersionId: definition.modelVersionId
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

async function loadSourceSystems() {
  const requestedId = instanceId.value
  const requestId = ++latestSourceRequest
  const sourceSelect = sourceSystemSelect
  sourceSelect.options = []
  sourceSelect.disabled = true
  sourceSelect.placeholder = '暂无来源系统'
  if (!requestedId) return
  try {
    const response = await getMasterSourceSystems(requestedId, { modelId: modelId.value || undefined })
    if (requestId !== latestSourceRequest || String(instanceId.value) !== String(requestedId)) return
    sourceSelect.options = (response?.data || []).map(item => ({ label: item.name, value: String(item.id) }))
    sourceSelect.disabled = sourceSelect.options.length === 0
    sourceSelect.placeholder = sourceSelect.disabled ? '暂无来源系统' : '请选择来源系统'
  } catch {
    if (requestId === latestSourceRequest) sourceSelect.placeholder = '来源系统加载失败'
  }
}

function handleMaintenanceSaved() {
  setRefreshNeeded()
  return refreshData()
}

async function refreshData() {
  const definition = await loadMetadata(true)
  if (definition) await loadClassificationNavigation(definition, true)
  dataTableRef.value?.getList()
  loadSourceSystems()
}

async function exportData() {
  if (!checkPermi(['master:instance:export'])) return
  if (!instanceId.value || !total.value) return
  exportLoading.value = true
  try {
    await proxy.download(`master/data/export/${instanceId.value}`,
      { ...queryParams, ...getClassificationFilterParams(), modelId: metadata.value?.modelId, modelVersionId: metadata.value?.modelVersionId }, `authority_${instanceId.value}_${Date.now()}.xlsx`)
  } finally {
    exportLoading.value = false
  }
}

function resetClassificationSelection() {
  latestRequest++
  queryParams.pageNum = 1
  selectedRows.value = []
  total.value = 0
  dataTableRef.value?.clearSelection()
}

function selectClassification(node) {
  resetClassificationSelection()
  setClassificationSelection(node)
  dataTableRef.value?.getList()
}

function handleSelectionChange(selection) {
  selectedRows.value = selection
}

function openCreate() {
  if (!checkPermi(['master:instance:add'])) return
  maintenanceMode.value = 'create'
  maintenanceRecordId.value = ''
  maintenanceVisible.value = true
}

function openModify(row) {
  if (!checkPermi(['master:instance:edit'])) return
  maintenanceMode.value = 'update'
  maintenanceRecordId.value = String(row.ID)
  maintenanceVisible.value = true
}

function openCopy(row) {
  if (!checkPermi(['master:instance:add'])) return
  maintenanceMode.value = 'copy'
  maintenanceRecordId.value = String(row.ID)
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
    query: { instanceId: instanceId.value, modelId: metadata.value?.modelId,
      modelVersionId: metadata.value?.modelVersionId, from: 'data' }
  })
}

function goBack() {
  router.push(routeBase.value)
}

watch([instanceId, modelId, modelVersionId], () => {
  latestRequest++
  maintenanceVisible.value = false
  lifeVisible.value = false
  batchVisible.value = false
  activeView.value = 'formal'
  latestSourceRequest++
  resetClassificationNavigation()
  queryParams.pageNum = 1
  queryParams.name = undefined
  queryParams.status = undefined
  queryParams.sourceSystem = undefined
  queryParams.approvalStatus = undefined
  total.value = 0
  selectedRows.value = []
  resetMetadata()
  nextTick(() => dataTableRef.value?.getList())
  loadSourceSystems()
})

onMounted(loadSourceSystems)

onUnmounted(() => {
  latestRequest++
  latestSourceRequest++
  resetClassificationNavigation()
  resetMetadata()
})

let hasActivated = false
onActivated(() => {
  // 首次进入由表格初始化加载；缓存页面再次激活时补加载。
  if (!hasActivated) {
    hasActivated = true
    return
  }
  if (route.path === `${routeBase.value}/data` && instanceId.value) {
    refreshData()
  }
})
</script>
