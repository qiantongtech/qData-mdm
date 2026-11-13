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
  候选组匹配审核列表与处理入口，负责确认、暂缓、恢复和删除。
  @author zyy
-->

<template>
  <div ref="appContainer" class="app-container">
    <GuideTip tip-id="merge/review.list"/>
    <el-container>
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        :editable="false"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      />
      <el-main class="main-content merge-list-main">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #actions-data>
            <GuideTipPanel
                type="plain"
                title="请结合候选对象组的来源记录和业务字段，判断是否属于同一业务对象，核对无误后进行确认，存在疑问时可暂缓处理。"
            />
          </template>
          <template #search>
            <qt-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef" @reset="resetTreeSelection"/>
          </template>
          <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
            <template #taskInfoTitle="{ row }">
              <el-link
                v-if="getDisplayTitle(row.taskName) && $auth.hasPermi('merge:review:query')"
                type="primary" :underline="false"
                role="link" tabindex="0"
                class="qt-detail-link text-ellipsis"
                :title="String(row.taskName)"
                @click.stop="openDetails(row)"
                @keydown.enter.stop.prevent="openDetails(row)"
                @keydown.space.stop.prevent="openDetails(row)"
              >{{ row.taskName }}</el-link>
              <span v-else class="qt-detail-link text-ellipsis" :title="getDisplayTitle(row.taskName)">{{ getDisplayTitle(row.taskName) ?? '-' }}</span>
            </template>
            <template #location="{ row }">
              <qt-model-location :model="getModelLocation(row)" />
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
                  >{{ formatModelTitle(row) }}</span>
                  <dict-tag
                    v-if="row.modelType"
                    :options="MODEL_TYPE_OPTIONS"
                    :value="row.modelType"
                    size="small"
                    type="primary"
                  />
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
            <template #sourceSystems="{ row }">
              <source-relation-tags :items="getSourceSystemList(row.sourceSystems)"/>
            </template>
            <template #matchRules="{ row }">
              <source-relation-tags :items="getMatchRuleTags(row.ruleNames)"/>
            </template>
            <template #status="{ row }">
              <el-tag :type="getOptionType(REVIEW_STATUS_OPTIONS, getReviewStatusValue(row.status))" size="default">
                {{ getOptionLabel(REVIEW_STATUS_OPTIONS, getReviewStatusValue(row.status)) }}
              </el-tag>
            </template>
            <template #handle="{ row }">
              <div class="review-row-actions">
                <el-button
                    v-hasPermi="['merge:review:query']"
                    link
                    type="primary"
                    icon="View"
                    @click="openDetails(row)"
                >详情
                </el-button>
                <el-button
                    v-hasPermi="['merge:review:confirm']"
                    link
                    type="primary"
                    :loading="actionLoadingId === row.id"
                    :disabled="!canReviewAction(row, '1') || actionLoadingId !== null"
                    @click="handleConfirm(row)"
                >
                  <template #icon>
                    <img :src="confirmMatchIcon" alt="" class="merge-action-svg-icon"
                         :class="{ 'merge-action-svg-icon-disabled': !canReviewAction(row, '1') || actionLoadingId !== null }"
                    />
                  </template>
                  确认匹配
                </el-button>
                <el-popover
                    placement="bottom"
                    :width="150"
                    trigger="click"
                    popper-class="table-action-popover handle-popover"
                >
                  <template #reference>
                    <el-button link type="primary" icon="ArrowDown">更多</el-button>
                  </template>
                  <div class="butgdlist task-action-list">
                    <el-button
                        v-hasPermi="['merge:review:suspend']"
                        link
                        type="primary"
                        :loading="actionLoadingId === row.id"
                        :disabled="!canReviewAction(row, '1') || actionLoadingId !== null"
                        @click="handleSuspend(row)"
                    >
                      <template #icon>
                        <img :src="suspendProcessingIcon" alt="" class="merge-action-svg-icon"
                             :class="{ 'merge-action-svg-icon-disabled': !canReviewAction(row, '1') || actionLoadingId !== null }"
                        />
                      </template>
                      暂缓处理
                    </el-button>
                    <el-button
                        v-hasPermi="['merge:review:resume']"
                        link
                        type="primary"
                        :loading="actionLoadingId === row.id"
                        :disabled="!canReviewAction(row, '2') || actionLoadingId !== null"
                        @click="handleResume(row)"
                    >
                      <template #icon>
                        <img :src="continueReviewIcon" alt="" class="merge-action-svg-icon"
                             :class="{ 'merge-action-svg-icon-disabled': !canReviewAction(row, '2') || actionLoadingId !== null }"
                        />
                      </template>
                      继续审核
                    </el-button>
                    <el-button
                        v-hasPermi="['merge:manage:list']"
                        link
                        type="primary"
                        :disabled="canReviewAction(row, '1') || canReviewAction(row, '2')"
                        @click="openManage(row)"
                    >
                      <template #icon>
                        <img :src="enterMergeManagementIcon" alt="" class="merge-action-svg-icon"
                             :class="{ 'merge-action-svg-icon-disabled': canReviewAction(row, '1') || canReviewAction(row, '2') }"
                        />
                      </template>
                      进入合并管理
                    </el-button>
                    <el-button
                        v-hasPermi="['merge:review:delete']"
                        link
                        type="danger"
                        icon="Delete"
                        :loading="actionLoadingId === row.id"
                        :disabled="(!canReviewAction(row, '1') && !canReviewAction(row, '2')) || actionLoadingId !== null"
                        @click="handleDelete(row)"
                        class="review-delete-action"
                    >删除任务
                    </el-button>
                  </div>
                </el-popover>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="Review">
import { getDisplayTitle } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import QtTree from '@/components/QtTree/index.vue'
import QtModelLocation from '@/views/merge/components/QtModelLocation/index.vue'
import {computed, getCurrentInstance, nextTick, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  confirmMergeReview,
  deleteMergeReview,
  listMergeReview,
  resumeMergeReview,
  suspendMergeReview
} from '@/api/merge/review'

import {getModelIcon} from '@/utils/imageUrl'
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType'
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus'
import enterMergeManagementIcon from '@/assets/modules/merge/icons/review/enter-merge-management.svg'
import continueReviewIcon from '@/assets/modules/merge/icons/review/continue-review.svg'
import confirmMatchIcon from '@/assets/modules/merge/icons/review/confirm-match.svg'
import suspendProcessingIcon from '@/assets/modules/merge/icons/review/suspend-processing.svg'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import SourceRelationTags from '@/views/ingest/source/sourceRelationTags.vue'
import {listIngestSource} from '@/api/ingest/source/source'

import { getPublishedModelTree, listPublishedModels } from '@/api/model/base/base';

import { getProjectIcon } from '@/utils/imageUrl';

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

function getOptionType(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.elTagType || 'info'
}

function parseQueryNumber(value) {
  if (value === undefined || value === null || value === '') return undefined
  const number = Number(value)
  return Number.isNaN(number) ? undefined : number
}

const { merge_review_status: REVIEW_STATUS_OPTIONS } = useDict('merge_review_status')

function getReviewStatusValue(value) {
  const status = String(value ?? '')
  return ['3', '4'].includes(status) ? 'completed' : status
}

let publishedModelsLoading

function loadPublishedModels() {
  if (!publishedModelsLoading) {
    // 与接入任务保持一致，只从成功发布模型接口构建可选项。
    publishedModelsLoading = listPublishedModels({ pageNum: 1, pageSize: -1 })
      .then((res) => (res.data?.rows || res.rows || [])
        .map((item) => ({ ...item, label: item.name, value: item.id })))
      .catch((error) => {
        publishedModelsLoading = undefined
        throw error
      })
  }
  return publishedModelsLoading
}

function getModelName(models, modelId) {
  const found = (models || []).find((item) => String(item.value) === String(modelId))
  return found?.label || (modelId ? `模型${modelId}` : '—')
}

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

function getSourceSystemTags(value, systems = []) {
  return [...new Set(String(value || '').split(/[,，、/|]+/).map(item => item.trim()).filter(Boolean))]
    .map(item => {
      const source = systems.find(system => system.code === item) || systems.find(system => system.name === item)
      return {label: source?.name || item, value: item, elTagType: 'primary', icon: getSourceIcon(source)}
    })
}

const {proxy} = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const appContainer = ref(null)
const tableRef = ref()
const modelOptions = ref([])
const sourceSystems = ref(null)
const actionLoadingId = ref(null)
const currentReviewPage = ref(1)

const tableStore = reactive({
  func: queryReview,
  params: {
    modelId: parseQueryNumber(route.query.modelId),
    status: undefined,
    taskName: undefined,
    transferTimeRange: [],
    groupNo: route.query.groupNo || undefined,
    orderByColumn: 'create_time',
    isAsc: 'descending',
  },
  events: {
    formatParams,
    formatData,
    onPageCurrentChange(params) {
      currentReviewPage.value = Number(params.pageNum) || 1
    },
    onPageSizeChange(params) {
      currentReviewPage.value = Number(params.pageNum) || 1
    },
  },
  config: {
    sort: true,
    table: {rowKey: 'id', stripe: true, defaultSort: {prop: 'create_time', order: 'descending'}},
    pagination: {layout: 'total, sizes, prev, pager, next, jumper', pageSizes: [10, 20, 50, 100]},
  },
  columns: [
    {label: '编号', prop: 'id', sortable: true, width: 60, minWidth: 60, align: 'center'},
    {
      label: '任务信息',
      property: 'taskName',
      info: { custom: 'taskInfoTitle', desc: 'remark' },
      minWidth: 240,
      align: 'left',
    },
    {label: '主数据模型', prop: 'modelName', slot: 'modelInfo', minWidth: 260, align: 'left', showOverflowTooltip: {effect: 'light'}},
    { label: '归属位置', prop: 'location', slot: 'location', minWidth: 190, align: 'left' },
    {label: '来源系统', prop: 'sourceSystems', slot: 'sourceSystems', minWidth: 280, align: 'left'},
    {
      label: '匹配概况',
      prop: 'ruleCount',
      slot: 'matchRules',
      minWidth: 300,
      align: 'left',
    },
    {label: '当前状态', prop: 'status', slot: 'status', width: 120, minWidth: 120},
    {
      label: '转入时间',
      prop: 'transferTime',
      sortable: true,
      sortableKey: 'transfer_time',
      date: true,
      width: 160,
      minWidth: 160
    },
    {label: '操作', slot: 'handle', fixed: 'right', width: 270, minWidth: 270, align: 'center'},
  ],
})

const modelTreeRef = ref()
const modelTreeData = ref([])
const modelTreeLoading = ref(false)

function normalizeModelTree(nodes = [], context = {}) {
  return nodes.map(node => {
    const projectName = node.nodeType === 'project' ? node.name : context.projectName
    const projectIcon = node.nodeType === 'project' ? node.icon : context.projectIcon
    const layers = node.nodeType === 'layer' ? [...(context.layers || []), { name: node.name }] : (context.layers || [])
    return {
      ...node, projectName, projectIcon, layers,
      treeKey: `${node.nodeType}_${node.id}`,
      children: normalizeModelTree(node.children || [], { projectName, projectIcon, layers }),
    }
  })
}

function collectTreeModels(nodes = []) {
  const models = new Map()
  function visit(items) {
    for (const node of items) {
      if (node.nodeType === 'model' && node.modelId != null) models.set(String(node.modelId), node)
      visit(node.children || [])
    }
  }
  visit(nodes)
  return [...models.values()]
}

function handleModelTreeClick(node) {
  tableStore.params.modelId = node.nodeType === 'model' ? node.modelId : undefined
  const ids = collectTreeModels([node]).map(model => model.modelId)
  tableStore.params.modelIds = node.nodeType === 'model' ? undefined : (ids.length ? ids : [-1])
  tableRef.value?.resetQuery()
}

function handleModelSelect(id) {
  tableStore.params.modelIds = undefined
  const node = collectTreeModels(modelTreeData.value).find(model => String(model.modelId) === String(id))
  modelTreeRef.value?.setCurrentKey(node?.treeKey ?? null, false)
}

function resetTreeSelection() {
  tableStore.params.modelIds = undefined
  modelTreeRef.value?.resetTree()
}

const modelsById = computed(() => new Map(collectTreeModels(modelTreeData.value).map(node => [String(node.modelId), node])))
function getModelLocation(row) {
  return modelsById.value.get(String(row.modelId)) || {}
}

async function loadModelTree() {
  modelTreeLoading.value = true
  try {
    const { data } = await getPublishedModelTree()
    modelTreeData.value = normalizeModelTree(data || [])
    await nextTick()
    handleModelSelect(tableStore.params.modelId)
  } catch {
    // 接口统一提示错误，保留列表和下拉筛选可用。
    modelTreeData.value = []
  } finally {
    modelTreeLoading.value = false
  }
}

loadModelTree()

const searchStore = reactive({
  items: [
    {label: '任务名称', prop: 'taskName', component: {is: 'input', placeholder: '请输入任务名称'}},
    {
      label: '转入时间',
      prop: 'transferTimeRange',
      class: 'review-transfer-time',
      component: {
        is: 'date-picker',
        type: 'daterange',
        startPlaceholder: '开始日期',
        endPlaceholder: '结束日期',
      },
    },
    {label: '审核状态', prop: 'status', component: {is: 'select', options: REVIEW_STATUS_OPTIONS}},
  ],
})

/**
 * 加载模型名称并分页查询候选组审核列表。
 * @param {*} params 业务查询、分页和排序参数。
 * @returns {Promise<*>} 当前业务计算结果。
 */
async function queryReview(params) {
  if (!sourceSystems.value) {
    try {
      const response = await listIngestSource({pageNum: 1, pageSize: -1})
      sourceSystems.value = response.data?.rows || response.data || []
    } catch {
      // 来源信息加载失败时保留原始文本并使用默认图标，下次查询重试。
    }
  }
  try {
    if (!modelOptions.value.length) modelOptions.value = await loadPublishedModels()
  } catch {
    // 模型名称降级为编号。
  }
  return listMergeReview(params)
}

/**
 * 整理业务筛选条件及分页排序参数。
 * @param {*} params 业务查询、分页和排序参数。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatParams(params) {
  currentReviewPage.value = Number(params.pageNum) || 1
  const groupNo = String(params.groupNo || '').trim()
  const taskName = String(params.taskName || '').trim()
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    modelId: params.modelId || undefined,
    modelIds: params.modelIds,
    status: params.status || undefined,
    groupNo: groupNo || undefined,
    taskName: taskName || undefined,
    transferStartTime: params.transferTimeRange?.[0] ? `${params.transferTimeRange[0]} 00:00:00` : undefined,
    transferEndTime: params.transferTimeRange?.[1] ? `${params.transferTimeRange[1]} 23:59:59` : undefined,
    orderByColumn: params.orderByColumn,
    isAsc: params.isAsc,
  }
}

/**
 * 将接口记录转换为页面展示数据。
 * @param {*} rows 待处理记录列表。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatData(rows) {
  return (rows || []).map((row) => ({
    ...row,
    modelName: getModelName(modelOptions.value, row.modelId),
    ...getModelInfo(row.modelId),
  }))
}

/** 将接口中的来源系统文本转换为标签项。 */
function getSourceSystemList(value) {
  return getSourceSystemTags(value, sourceSystems.value || [])
}

/** 将具体匹配规则转换为蓝色标签。 */
function getMatchRuleTags(names) {
  return (names || []).map((name) => ({label: name, value: name, elTagType: 'primary'}))
}

/** 使用已发布模型信息回显模型类型、版本及描述。 */
function getModelInfo(modelId) {
  const model = modelOptions.value.find((item) => String(item.value) === String(modelId))
  return {
    modelType: model?.type,
    modelIcon: model?.icon,
    modelCode: model?.code,
    modelDescription: model?.description,
  }
}

/** 统一显示主数据模型名称及编码。 */
function formatModelTitle(row) {
  if (!row.modelName) return row.modelCode || '-'
  return row.modelCode ? `${row.modelName} (${row.modelCode})` : row.modelName
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
 * 判断候选组是否仍有效且处于指定审核状态。
 * @param {*} row 当前业务记录。
 * @param {*} status 业务处理状态。
 * @returns {boolean} 是否满足当前业务条件。
 */
function canReviewAction(row, status) {
  return String(row?.status) === status && String(row?.validFlag) !== '0'
}

/**
 * 跳转到候选组的只读详情页。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openDetails(row) {
  return router.push({path: '/merge/review/detail', query: {id: row.id}})
}

/**
 * 执行候选组审核动作，仅在服务端确认成功后刷新列表。
 * @param {Object} row 当前候选组。
 * @param {Function} action 审核接口调用。
 * @param {string} successMessage 操作成功后的提示。
 * @returns {Promise<boolean>} 接口调用是否成功。
 */
async function runReviewAction(row, action, successMessage) {
  if (actionLoadingId.value !== null) return false
  const currentPage = currentReviewPage.value
  actionLoadingId.value = row.id
  try {
    await action()
    proxy.$modal.msgSuccess(successMessage)
  } catch {
    // 请求拦截器负责提示接口错误，失败时保留当前列表状态。
    return false
  } finally {
    actionLoadingId.value = null
  }
  // 等待行内 loading 复位，再刷新当前页；状态始终以服务端结果为准。
  await nextTick()
  // resetQuery 会使用 pagination.params 初始化页码；临时指定当前页后立即移除，
  // 这样审核动作保留当前页，而搜索栏的正常重置仍回到第一页。
  tableStore.config.pagination.params = {pageNum: currentPage}
  tableRef.value?.resetQuery()
  delete tableStore.config.pagination.params
  return true
}

/**
 * 确认候选组属于同一业务对象，并转入后续合并处理。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleConfirm(row) {
  if (!canReviewAction(row, '1') || actionLoadingId.value !== null) return
  try {
    await proxy.$modal.confirm('确认这些来源数据属于同一对象吗？确认后将转入合并管理。')
  } catch {
    return
  }
  await runReviewAction(row, () => confirmMergeReview({groupId: row.id}), '已确认匹配')
}

/**
 * 暂缓当前候选组，后续仍可恢复审核。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleSuspend(row) {
  if (!canReviewAction(row, '1') || actionLoadingId.value !== null) return
  try {
    await proxy.$modal.confirm('确认暂缓处理当前候选对象组吗？暂缓后仍可继续审核。')
  } catch {
    return
  }
  await runReviewAction(row, () => suspendMergeReview({groupId: row.id}), '已暂缓处理')
}

/**
 * 恢复暂缓候选组的审核状态。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleResume(row) {
  if (!canReviewAction(row, '2')) return
  await runReviewAction(row, () => resumeMergeReview({groupId: row.id}), '已恢复为待审核')
}

/**
 * 删除待审核或暂缓处理的候选组，并刷新审核列表。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleDelete(row) {
  if ((!canReviewAction(row, '1') && !canReviewAction(row, '2')) || actionLoadingId.value !== null) return
  try {
    await proxy.$modal.confirm('确认删除当前候选对象组吗？删除后将释放来源数据，可重新参与检测。')
  } catch {
    return
  }
  await runReviewAction(row, () => deleteMergeReview(row.id), '删除成功')
}

/**
 * 将已确认或单条转入的候选组带条件打开合并管理列表。
 * @param {*} row 当前业务记录。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function openManage(row) {
  return router.push({path: '/merge/manage', query: {modelId: row.modelId, groupNo: row.groupNo}})
}

watch(() => route.fullPath, async () => {
  if (route.path !== '/merge/review') return
  tableStore.params.modelId = parseQueryNumber(route.query.modelId)
  handleModelSelect(tableStore.params.modelId)
  tableStore.params.status = undefined
  tableStore.params.taskName = undefined
  tableStore.params.transferTimeRange = []
  tableStore.params.groupNo = route.query.groupNo || undefined
  await nextTick()
  tableRef.value?.getList()
})

</script>

<style scoped lang="scss">
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

.merge-list-main {
  min-width: 0;
}

:deep(.review-transfer-time) {
  width: 320px;
}

.review-row-actions {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  align-items: center;
  gap: 0 6px;
  white-space: nowrap;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

.merge-action-svg-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.merge-action-svg-icon-disabled {
  opacity: 0.45;
}

:global(.merge-review-action-popover .task-action-list) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--qt-space-2);
  width: 100%;
}

/* 下拉项与「暂缓处理」保持同一左起点，避免文字长短不同的项居中后错位。 */
:global(.merge-review-action-popover .task-action-list .el-button) {
  width: 100%;
  margin-left: 0;
  padding-left: 18px;
  justify-content: flex-start;
  text-align: left;
}

:global(.merge-review-action-popover .review-delete-action.is-disabled .el-icon) {
  color: var(--qt-status-danger) !important;
  opacity: 1;
}
</style>
