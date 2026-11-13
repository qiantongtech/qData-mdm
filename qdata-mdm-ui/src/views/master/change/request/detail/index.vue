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
  <div ref="dialogContainer" class="app-container">
    <template v-if="canViewDetail && (detail || loading)">
      <DetailInfo
        :data="headerData"
        :header="headerConfig"
        :items="headerItems"
        :default-span="8"
        :gutter="2"
        mode="free"
        placeholder="-"
        time-format="{y}-{m}-{d} {h}:{i}:{s}"
        class="request-header"
      >
        <template #modelInfo>
          <div class="request-model-info">
            <div class="request-model-main">
              <img :src="getModelIcon(modelInfo)" alt="" class="request-info-icon" />
              <span class="request-model-title" :title="modelTitle">{{ modelTitle }}</span>
              <dict-tag v-if="modelInfo.type" :options="model_type" :value="modelInfo.type" size="small" type="primary" />
              <dict-tag v-if="modelInfo.releaseVersion?.versionNo" :options="model_status"
                :value="modelInfo.version?.status" :label="modelInfo.releaseVersion.versionNo" size="small"
              />
            </div>
          </div>
        </template>
        <template #location>
          <div v-if="modelLocation" class="request-model-location">
            <img :src="getProjectIcon(modelInfo.projectIcon)" alt="" class="request-info-icon" />
            <span class="request-model-title" :title="modelLocation">{{ modelLocation }}</span>
          </div>
          <span v-else>-</span>
        </template>
      </DetailInfo>
      <div class="pagecont-bottom" v-loading="loading" element-loading-text="正在加载申请详情">
        <el-tabs v-model="activeTab">
          <qt-tab-pane label="数据明细" name="content" />
          <qt-tab-pane label="审批记录" name="approvals" />
        </el-tabs>

        <template v-if="detail && activeTab === 'content'">
          <template v-if="showSupplementarySections">
            <div v-if="importItems.length" class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data><div class="h2-titles">导入文件信息</div></template>
                <DescriptionsInfo :items="importItems" :column="2" :border="true" placeholder="-" />
              </qt-wrap>
            </div>

            <qt-wrap v-if="detail.changeType === 'BATCH_UPDATE'" :columns="ruleTableStore.columns"
              :config="ruleTableStore.config" :table-ref="ruleTableRef" class="mb15"
            >
              <template #actions-data><div class="h2-titles">批量修改规则</div></template>
              <qt-table ref="ruleTableRef" v-bind="ruleTableStore" />
            </qt-wrap>

            <div v-if="processingItems.length" class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data><div class="h2-titles">处理说明</div></template>
                <DescriptionsInfo :items="processingItems" :column="2" :border="true" placeholder="-" />
              </qt-wrap>
            </div>
          </template>

          <qt-wrap :columns="itemTableStore.columns" :config="itemTableStore.config"
            :table-ref="itemTableRef" class="mb15"
          >
            <template #actions-data>
              <GuideTipPanel type="plain" title="本区域展示本次申请提交的主数据明细，包括业务字段、操作类型及当前处理状态，支持查看单条数据详情。" />
            </template>
            <qt-table ref="itemTableRef" v-bind="itemTableStore">
              <template #changeType="{ row }">
                <dict-tag :value="row.changeType" :options="changeTypeOptions" />
              </template>
              <template #approvalStatus="{ row }">
                <dict-tag :value="row.approvalStatus" :options="approvalStatusOptions" />
              </template>
              <template #businessValue="{ row, column_data }">
                {{ displayRequestFieldValue(detail, column_data.fieldCode, row.businessValues[column_data.fieldCode]) }}
              </template>
              <template #action="{ row }">
                <el-button link type="primary" icon="View" @click.stop="selectItem(row)">查看</el-button>
              </template>
            </qt-table>
          </qt-wrap>

          <RequestSnapshotContent v-if="selectedItem && !usesSnapshotDialog" :detail="detail" :item="selectedItem"
            :status-options="master_data_status || []"
          />
        </template>
        <qt-wrap v-else-if="detail" :columns="approvalTableStore.columns" :config="approvalTableStore.config"
          :table-ref="approvalTableRef"
        >
          <template #actions-data>
            <GuideTipPanel type="plain" title="以下展示申请的提交及处理记录。" />
          </template>
          <qt-table ref="approvalTableRef" v-bind="approvalTableStore">
            <template #approvalAction="{ row }">
              <el-tag :type="row.actionType">{{ row.actionName }}</el-tag>
            </template>
          </qt-table>
        </qt-wrap>
      </div>
      <el-dialog v-if="dialogContainer && usesSnapshotDialog" v-model="snapshotDialogVisible"
        :title="snapshotDialogTitle"
        width="800px" append-to-body :append-to="dialogContainer"
        draggable destroy-on-close class="dialog"
      >
        <RequestSnapshotContent v-if="selectedItem" :key="snapshotDialogKey"
          :detail="detail" :item="selectedItem" :status-options="master_data_status || []"
        />
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="snapshotDialogVisible = false">关 闭</el-button>
          </div>
        </template>
      </el-dialog>
    </template>
    <div v-else-if="error" class="pagecont-bottom">
      <el-empty :description="error">
        <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
        <el-button plain @click="returnToList">返回列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup name="MasterMyRequestDetail">
import { computed, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import DetailInfo from '@/components/DetailInfo/index.vue'
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue'
import { getMasterChangeRequestDetail } from '@/api/master/change/request'
import { getBase } from '@/api/model/base/base'
import { checkPermi } from '@/utils/permission'
import { getModelIcon, getProjectIcon } from '@/utils/imageUrl'
import { useDict } from '@/utils/dict'
import RequestSnapshotContent from './components/RequestSnapshotContent.vue'

const requestNumberCollator = new Intl.Collator('zh-CN', { numeric: true })

function sortRequestRows(rows, params = {}) {
  if (!params.orderByColumn || !['ascending', 'descending'].includes(params.isAsc)) return rows
  const direction = params.isAsc === 'descending' ? -1 : 1
  return [...rows].sort((left, right) => direction * requestNumberCollator.compare(
    String(left[params.orderByColumn] ?? ''), String(right[params.orderByColumn] ?? '')
  ))
}

function parseSnapshot(value) {
  if (!value) return {}
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch { return {} }
}

function snapshotValues(value) {
  const snapshot = parseSnapshot(value)
  return snapshot.values && typeof snapshot.values === 'object' && !Array.isArray(snapshot.values)
    ? snapshot.values : snapshot
}

function lookup(values, code) {
  const key = Object.keys(values).find(key => key.toUpperCase() === String(code).toUpperCase())
  return key === undefined ? undefined : values[key]
}

function displaySnapshotValue(value) {
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function displayRequestFieldValue(detail, code, value) {
  const labels = lookup(detail.fieldValueLabels || {}, code)
  return displaySnapshotValue(labels?.[String(value)] ?? value)
}

function requestBusinessFields(detail) {
  const fields = (detail.fields || []).filter(field => field.code && field.category?.toUpperCase() !== 'SYSTEM')
  if (detail.fields?.length) {
    return fields.sort((a, b) => (a.sortOrder ?? Number.MAX_SAFE_INTEGER) - (b.sortOrder ?? Number.MAX_SAFE_INTEGER))
  }
  // 历史模型配置不可用时仍展示申请保存的业务字段，以快照编码作为列名。
  const excluded = new Set(['ID', 'STATUS', 'CATEGORY_ID', 'VALID_FLAG', 'DEL_FLAG', 'TARGETRECORDID', 'SUBMODELCHANGES'])
  const codes = (detail.items || []).flatMap(item => [
    ...Object.keys(snapshotValues(item.beforeData)), ...Object.keys(snapshotValues(item.proposedData))
  ]).map(code => code.toUpperCase()).filter(code => !excluded.has(code))
  return [...new Set(codes)].map(code => ({ code, name: code }))
}

function fieldFor(fields, code) {
  return fields.find(field => field.code?.toUpperCase() === code.toUpperCase())
}

function requestItemRows(detail) {
  const fields = detail.fields || []
  const businessFields = requestBusinessFields(detail)
  const nameField = fields.find(field => field.nameField)
  const codeField = fields.find(field => field.unique && !field.nameField && field.category !== 'SYSTEM')
  return (detail.items || []).map(item => {
    const before = snapshotValues(item.beforeData)
    const proposed = snapshotValues(item.proposedData)
    const value = code => {
      const after = lookup(proposed, code)
      return after === undefined ? lookup(before, code) : after
    }
    return {
      ...item,
      requestNo: detail.requestNo,
      recordCode: codeField ? value(codeField.code) : undefined,
      recordName: nameField ? value(nameField.code) : undefined,
      recordId: item.masterRecordId || item.resultMasterRecordId,
      businessValues: Object.fromEntries(businessFields.map(field => [field.code, value(field.code)])),
      // 当前 QtTable 按 prop 直接取值；同步展示列，保留嵌套快照和含点号的历史字段编码。
      ...Object.fromEntries(businessFields.map(field => [`businessValues.${field.code}`, value(field.code) ?? ''])),
      approvalStatus: detail.approvalStatus
    }
  })
}

const lifecycleActions = {
  FREEZE: '冻结', UNFREEZE: '解冻', INVALIDATE: '失效', RECOVER: '恢复', ARCHIVE: '归档'
}

function statusProcessingItems(detail) {
  if (detail.changeType !== 'STATUS_CHANGE') return []
  const actions = [...new Set((detail.items || []).map(item => lifecycleActions[item.changeType]).filter(Boolean))]
  return [
    { label: '生效方式', value: '审核通过后由执行器异步执行' },
    { label: '操作类型', value: actions.join('、') || '-' },
    { label: '变更范围', value: '仅变更生命周期状态，不涉及其他业务字段', span: 2 }
  ]
}

function requestImportItems(detail) {
  if (detail.changeType !== 'BATCH_IMPORT') return []
  const info = detail.importInfo || {}
  return [
    { label: '文件名称', value: info.fileName },
    { label: '导入模式', value: info.importMode, formatter: value => ({
      CREATE_ONLY: '仅新增', UPSERT: '新增＋更新', UPDATE_ONLY: '仅更新'
    }[value] || value || '-') },
    { label: '模板版本', value: info.templateVersionNo },
    { label: '导入总数', value: info.totalCount },
    { label: '校验通过', value: info.passedCount, span: 2 }
  ]
}

function batchRuleRows(detail) {
  if (detail.changeType !== 'BATCH_UPDATE' || !detail.items?.length) return []
  const snapshots = detail.items.map(item => snapshotValues(item.proposedData))
  // 批量修改提交时只保存统一选中的字段；只展示所有明细均包含且赋值一致的字段。
  return Object.entries(snapshots[0]).filter(([code, value]) =>
    snapshots.every(snapshot => Object.keys(snapshot).some(key => key.toUpperCase() === code.toUpperCase()) &&
      JSON.stringify(lookup(snapshot, code)) === JSON.stringify(value))
  ).map(([code, value], index) => ({
    number: index + 1,
    fieldName: fieldFor(detail.fields || [], code)?.name || code,
    method: '统一赋值',
    value: displaySnapshotValue(value)
  }))
}

function requestApprovalRows(detail) {
  if (!detail.requestId) return []
  const rows = [{
    number: 1,
    nodeName: '提交申请',
    handlerName: detail.applicantName,
    actionName: '提交',
    actionType: 'primary',
    opinion: detail.applyReason,
    handledTime: detail.submitTime
  }]
  const status = String(detail.approvalStatus ?? '')
  const results = {
    1: { nodeName: '审批结果', actionName: '通过', actionType: 'success' },
    2: { nodeName: '审批结果', actionName: '驳回', actionType: 'danger' },
    3: { nodeName: '申请撤回', actionName: '撤回', actionType: 'info' },
    4: { nodeName: '申请终止', actionName: '终止', actionType: 'warning' }
  }
  if (!results[status]) return rows
  rows.push({
    number: 2,
    ...results[status],
    handlerName: status === '3' ? detail.applicantName
      : status === '1' && !detail.flowInstanceId ? '系统' : undefined,
    opinion: status === '1' ? undefined : detail.terminateReason ||
      (status === '4' ? detail.executeMessage : undefined),
    // 已通过申请的 finishTime 是执行完成时间，不能当作尚未记录的审批时间。
    handledTime: status === '1' ? undefined : detail.finishTime
  })
  return rows
}

const route = useRoute()
const router = useRouter()
const { master_change_type, master_change_request_status, master_data_status, model_type, model_status } = useDict(
  'master_change_type', 'master_change_request_status', 'master_data_status', 'model_type', 'model_status'
)
const detail = ref(null)
const modelInfo = ref({})
const loading = ref(false)
const error = ref('')
const activeTab = ref('content')
const selectedItemId = ref()
const dialogContainer = ref(null)
const snapshotDialogVisible = ref(false)
const snapshotDialogKey = ref(0)
// 暂时隐藏数据明细上方的导入、批量修改和状态处理补充信息。
const showSupplementarySections = false
const snapshotDialogTitle = computed(() => ({
  CREATE: '新增数据详情',
  UPDATE: '修改数据详情',
  BATCH_UPDATE: '批量修改明细',
  BATCH_IMPORT: '导入数据详情',
  STATUS_CHANGE: '状态变更详情'
}[detail.value?.changeType] || ''))
const usesSnapshotDialog = computed(() => Boolean(snapshotDialogTitle.value))
let latestRequest = 0

const changeTypeOptions = computed(() => master_change_type.value || [])
const approvalStatusOptions = computed(() => (master_change_request_status.value || []).map(item => ({
  ...item, elTagType: { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info' }[item.value] || 'info'
})))
const itemRows = computed(() => requestItemRows(detail.value || {}))
const headerData = computed(() => ({
  ...detail.value,
  title: detail.value?.requestName || '我的申请详情',
  // 从申请明细汇总稳定主数据 ID，保留数字字符串以免 BIGINT 精度丢失。
  masterDataIds: [...new Set(itemRows.value.map(item => item.recordId).filter(Boolean))].join('、')
}))
const modelTitle = computed(() => {
  const name = modelInfo.value.name || detail.value?.modelName
  const code = modelInfo.value.code
  return name ? (code ? `${name} (${code})` : name) : code || '-'
})
const modelLocation = computed(() => [
  modelInfo.value.projectName,
  modelInfo.value.layers?.at(-1)?.name
].filter(Boolean).join(' / '))
const headerConfig = computed(() => ({
  className: 'clearfixs', idKey: 'requestId', nameKey: 'title', statusKey: 'approvalStatus',
  statusOptions: approvalStatusOptions.value
}))
// 顶部信息分三行展示，申请原因独占中间一行。
const headerItems = computed(() => [
  { label: '主数据ID', key: 'masterDataIds' },
  { label: '模型信息', slot: 'modelInfo' },
  { label: '归属位置', slot: 'location' },
  { label: '申请原因', key: 'applyReason', span: 24 },
  { label: '申请类型', key: 'changeType', dictOptions: changeTypeOptions.value },
  { label: '申请人', formatter: (_, data) => `${data.applicantName || '-'} / ${data.applicantPhone || '-'}` },
  { label: '申请时间', key: 'submitTime', type: 'time' }
])
const businessFields = computed(() => requestBusinessFields(detail.value || {}))
const selectedItem = computed(() => itemRows.value.find(item => item.itemId === selectedItemId.value))
const ruleRows = computed(() => batchRuleRows(detail.value || {}))
const approvalRows = computed(() => requestApprovalRows(detail.value || {}))
const processingItems = computed(() => statusProcessingItems(detail.value || {}))
const importItems = computed(() => requestImportItems(detail.value || {}))

const ruleColumns = reactive([
  { prop: 'number', label: '编号', width: 80, sortable: 'custom' },
  { prop: 'fieldName', label: '字段名称', minWidth: 180, align: 'left' },
  { prop: 'method', label: '修改方式', minWidth: 150, align: 'left' },
  { prop: 'value', label: '修改值', minWidth: 240, align: 'left', showOverflowTooltip: { effect: 'light' } }
])
const itemColumns = ref([])
watch(businessFields, fields => {
  const orderedFields = [
    ...fields.filter(field => field.name === '所属产品分类'),
    ...fields.filter(field => field.name !== '所属产品分类')
  ]
  itemColumns.value = [
    { prop: 'recordId', label: '编号', width: 200, sortable: 'custom' },
    ...orderedFields.map(field => ({
      prop: `businessValues.${field.code}`, fieldCode: field.code, label: field.name || field.code,
      minWidth: 150, align: 'left', slot: 'businessValue', showOverflowTooltip: { effect: 'light' }
    })),
    { prop: 'changeType', label: '操作类型', width: 120, slot: 'changeType' },
    { prop: 'approvalStatus', label: '当前状态', width: 110, slot: 'approvalStatus' },
    { label: '操作', width: 90, fixed: 'right', slot: 'action' }
  ]
}, { immediate: true })
const approvalColumns = reactive([
  { prop: 'number', label: '编号', width: 100, sortable: 'custom' },
  { prop: 'nodeName', label: '审批节点', width: 220, align: 'left' },
  { prop: 'handlerName', label: '处理人', width: 200, align: 'left' },
  { prop: 'actionName', label: '处理动作', width: 160, slot: 'approvalAction' },
  { prop: 'opinion', label: '处理意见', minWidth: 260, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { prop: 'attachments', label: '附件', width: 160, align: 'left' },
  { prop: 'handledTime', label: '处理时间', width: 180, align: 'left', date: true, sortable: 'custom' }
])

const ruleTableRef = ref(null)
const itemTableRef = ref(null)
const approvalTableRef = ref(null)
const ruleTableStore = {
  config: {
    initResquest: false,
    fullContent: false,
    autoPagination: true,
    pagination: { notAutoScroll: true },
    actions: { table: { search: false } },
    table: { rowKey: 'number', stripe: true, border: false }
  },
  columns: ruleColumns,
  func: async params => ({ data: sortRequestRows(ruleRows.value, params) })
}
const itemTableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    autoPagination: true,
    pagination: { notAutoScroll: true },
    actions: { table: { search: false } },
    table: { rowKey: 'itemId', stripe: true, border: false }
  },
  columns: computed(() => itemColumns.value),
  func: async params => ({ data: sortRequestRows(itemRows.value, params) })
})
const approvalTableStore = {
  config: {
    initResquest: false,
    fullContent: false,
    autoPagination: true,
    pagination: { notAutoScroll: true },
    actions: { table: { search: false } },
    table: { rowKey: 'number', stripe: true, border: false }
  },
  columns: approvalColumns,
  func: async params => ({ data: sortRequestRows(approvalRows.value, params) })
}

// 申请内容在详情响应中一次返回，分页和排序由公共表格处理。
watch([ruleRows, itemRows, approvalRows, activeTab], async () => {
  await nextTick()
  ruleTableRef.value?.resetQuery()
  itemTableRef.value?.resetQuery()
  approvalTableRef.value?.resetQuery()
}, { immediate: true })

function selectItem(item) {
  selectedItemId.value = item.itemId
  if (usesSnapshotDialog.value) {
    // 每次查看重置弹窗内的子记录选择；关闭时由公共弹窗在动画结束后销毁内容。
    snapshotDialogKey.value++
    snapshotDialogVisible.value = true
  }
}

const canViewDetail = computed(() => checkPermi(['master:change:query']))

async function loadDetail() {
  const request = ++latestRequest
  detail.value = null
  modelInfo.value = {}
  snapshotDialogVisible.value = false
  selectedItemId.value = undefined
  activeTab.value = 'content'
  error.value = ''
  loading.value = false
  if (!canViewDetail.value) {
    error.value = '当前账号无申请详情查看权限'
    return
  }
  const requestId = String(route.params.requestId || '')
  if (!/^[1-9]\d*$/.test(requestId)) {
    error.value = '申请ID无效，无法加载详情'
    return
  }
  loading.value = true
  try {
    const response = await getMasterChangeRequestDetail(requestId)
    if (request !== latestRequest) return
    if (!response?.data?.requestId) {
      error.value = '申请不存在或无权查看'
      return
    }
    detail.value = response.data
    if (response.data.modelId && checkPermi(['model:base:query'])) {
      try {
        const modelResponse = await getBase(response.data.modelId)
        if (request !== latestRequest) return
        modelInfo.value = modelResponse.data || {}
      } catch {
        // 模型查询失败时保留申请内容，错误提示由公共请求工具处理。
      }
    }
  } catch (err) {
    if (request === latestRequest) error.value = err?.msg || err?.message || '申请详情加载失败，请稍后重试'
  } finally {
    if (request === latestRequest) loading.value = false
  }
}

function returnToList() {
  router.push('/master/change/request')
}

watch([() => route.params.requestId, canViewDetail], loadDetail, { immediate: true })
onUnmounted(() => { latestRequest++ })
</script>

<style scoped lang="scss">
.request-header {
  :deep(.el-row) {
    row-gap: 2px;
  }

  :deep(.infotop-row) {
    height: 100%;
    align-items: stretch;
  }

  :deep(.infotop-row-lable),
  :deep(.infotop-row-value) {
    height: auto;
    min-height: 40px;
  }

  :deep(.infotop-row-value) {
    min-width: 0;
  }
}

.request-model-info {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.request-model-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.request-info-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.request-model-title,
.request-model-description {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-model-title {
  max-width: 100%;
}

.request-model-description {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.request-model-location {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
}

@media (max-width: 767px) {
  .request-header {
    :deep(.el-col) {
      max-width: 100%;
      flex-basis: 100%;
    }

    :deep(.infotop-row-lable) {
      width: 90px;
    }
  }
}
</style>
