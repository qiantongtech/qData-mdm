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

  Business License: https://docs.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<!--
  展示检测执行产生的候选组及其来源成员详情。
  @emits close 通知父页面详情已关闭。
  @author chentangwen
-->
<template>
  <div v-if="visible" ref="app-container">
    <div>
      <template v-if="instance">
        <el-alert
          v-if="!isSuccessInstance"
          :closable="false"
          type="warning"
          :title="emptyText"
        />

        <qt-wrap
          v-else
          :columns="tableStore.columns"
          :tableRef="tableRef"
          :config="{ fullContent: false, actions: { table: { search: false } } }"
        >
          <template #actions-data>
            <el-button
              type="primary"
              plain
              icon="Back"
              @click="close"
            >返回执行记录</el-button>
          </template>
          <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
            <template #action="{ row }">
              <el-button
                link
                type="primary"
                icon="View"
                @click="openGroup(row)"
              >查看</el-button>
            </template>
            <template #empty>
              <div v-if="groupQueryError" class="emptyBg">
                <img :src="emptyDataImage" alt="" />
                <p>结果加载失败，<el-button link type="primary" @click="retryQueryGroups">点击重试</el-button></p>
              </div>
              <div v-else class="emptyBg">
                <img :src="emptyDataImage" alt="" />
                <p>暂无记录</p>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </template>
    </div>
    <el-dialog
      v-model="groupVisible"
      title="查看"
      width="min(1400px, 94vw)"
      append-to-body
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">查看</span>
      </template>
      <template v-if="groupRow">
        <el-form :model="groupRow" label-width="110px" class="column-form">
          <div class="h2-title row-full">基础信息</div>
          <el-form-item v-for="item in groupBasicItems" :key="item.label" :label="item.label">
            <div class="form-readonly"><span>{{ item.value }}</span></div>
          </el-form-item>
        </el-form>

        <qt-wrap
          :columns="groupSourceColumns"
          :tableRef="groupDetailTableRef"
          :config="{ fullContent: false, actions: { table: { show: false, search: false } } }"
          class="group-source-wrap"
        >
          <template #actions-data>
            <div class="h2-title group-source-title">来源数据</div>
          </template>
          <qt-table
            :key="groupSourceViewId"
            ref="groupDetailTableRef"
            :columns="groupSourceColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
            :func="queryGroupSources"
            :config="groupSourceTableConfig"
          >
            <template #rowIndex="{ row }">{{ row.sourceIndex }}</template>
            <template #businessValue="{ row, column_data }">
              {{ displayBusinessValue(row[column_data.property || column_data.prop]) }}
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({ icon: row.modelIcon, type: row.modelType })" alt="" class="info-icon"/>
                  <span class="info-title model-info-title text-ellipsis">{{ row.modelName ? formatEntityTitle(row.modelName, row.modelCode) : row.modelCode || '-' }}</span>
                  <dict-tag v-if="row.modelType" :options="MODEL_TYPE_OPTIONS" :value="row.modelType" size="small" type="primary"/>
                  <dict-tag
                      v-if="row.modelVersionNo"
                      class="ml4"
                      :options="getVersionTagOptions(row)"
                      :value="row.modelVersionStatus ?? row.modelVersionNo"
                      size="small"
                      type="info"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || '-' }}</div>
              </div>
            </template>
            <template #sourceSystemInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(row.sourceSystemIcon)" alt="" class="info-icon"/>
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
            <template v-if="groupSourceError" #empty>
              <div class="emptyBg">
                <img :src="emptyDataImage" alt="" />
                <p>来源数据加载失败，<el-button link type="primary" @click="groupDetailTableRef?.getList()">点击重试</el-button></p>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </template>
      <template #footer>
        <div class="dialog-footer"><el-button @click="groupVisible = false">关 闭</el-button></div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DetectResultDialog">
import emptyDataImage from '@/assets/common/images/empty-state/empty-nodata.png'
import { computed, nextTick, reactive, ref } from 'vue'
import { listMergeGroup, listMergeGroupSources } from '@/api/merge/detect'
import { getModelIcon, getProjectIcon } from '@/utils/imageUrl'
import { MODEL_TYPE_OPTIONS } from '@/utils/model/modelType'
import { MODEL_STATUS_OPTIONS } from '@/utils/model/modelStatus'
import ingestTaskIcon from '@/assets/modules/ingest/icons/task/ingest-task.svg'

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const emit = defineEmits(['close'])
const visible = ref(false)
const instance = ref(null)
const tableRef = ref()
const groupDetailTableRef = ref()
const groupSourceFields = ref([])
const groupSourceColumns = computed(() => buildGroupSourceColumns(groupSourceFields.value))
const groupSourceError = ref(false)
const groupSourceViewId = ref(0)
let groupSourceRequest = null
const groupVisible = ref(false)
const groupRow = ref(null)
const groupQueryError = ref(false)
const isSuccessInstance = computed(() => String(instance.value?.status) === '4')
const emptyText = computed(() => String(instance.value?.status) === '2' ? '本次执行失败，未形成检测结果。' : '本次执行尚未完成，暂无检测结果。')
let groupQueryVersion = 0
let latestGroupRequest = null

/** 候选组查看弹窗的基础信息按目标系统查看任务的双列只读表单组织。 */
const groupBasicItems = computed(() => [
  { label: '候选对象组', value: displayText(groupRow.value?.groupNo || groupRow.value?.id) },
  { label: '主要业务对象', value: displayText(groupRow.value?.primaryObject) },
  { label: '来源数据', value: formatCountCompact(groupRow.value?.memberCount, '条') },
  { label: '最高匹配分', value: formatScoreText(groupRow.value || {}) },
  { label: '命中规则', value: getRuleText(groupRow.value || {}) },
  { label: '主要命中字段', value: displayText(groupRow.value?.hitFields) },
])

const tableStore = reactive({
  func: queryGroups,
  params: { taskInstanceId: undefined },
  events: {
    formatParams(params) {
      return {
        pageNum: params.pageNum,
        pageSize: params.pageSize,
        taskInstanceId: params.taskInstanceId,
        orderByColumn: params.orderByColumn,
        isAsc: params.isAsc,
      }
    },
    formatData(rows) {
      return (rows || []).map((row) => ({
        ...row,
        memberCountText: row.memberCount === null || row.memberCount === undefined || row.memberCount === ''
          ? displayText(row.memberCount)
          : `${row.memberCount}条`,
        primaryObjectText: displayText(row.primaryObject),
        scoreText: formatScoreText(row),
        ruleText: getRuleText(row),
        hitFieldsText: displayText(row.hitFields),
      }))
    },
  },
  config: {
    initResquest: false,
    table: {
      rowKey: 'id',
      stripe: true,
    },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50],
      notAutoScroll: true,
    },
  },
  columns: [
    { label: '候选对象组', prop: 'groupNo', minWidth: 180, align: 'left', showOverflowTooltip: true },
    { label: '主要业务对象', prop: 'primaryObjectText', minWidth: 220, align: 'left', showOverflowTooltip: true },
    { label: '来源数据', prop: 'memberCountText', width: 110, minWidth: 110 },
    { label: '最高匹配分', prop: 'scoreText', width: 110, minWidth: 110 },
    { label: '命中规则', prop: 'ruleText', width: 110, minWidth: 110 },
    { label: '主要命中字段', prop: 'hitFieldsText', minWidth: 180, align: 'left', showOverflowTooltip: true },
    { label: '操作', slot: 'action', fixed: 'right', width: 100, minWidth: 100 },
  ],
})

const groupSourceTableConfig = {
  autoPagination: true,
  sort: true,
  table: {
    stripe: true,
    rowKey: 'id',
    maxHeight: '45vh',
    scrollbarAlwaysOn: true,
    defaultSort: { prop: 'lastReceiveTime', order: 'descending' },
  },
  pagination: { notAutoScroll: true },
}

/**
 * 查询所选执行实例生成的候选组。
 * @param {*} params 表格查询、分页及排序参数。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function queryGroups(params) {
  const version = ++groupQueryVersion
  groupQueryError.value = false
  if (!params?.taskInstanceId) return Promise.resolve({ data: { rows: [], total: 0 } })

  latestGroupRequest = listMergeGroup(params)
  return latestGroupRequest.then((response) => {
    // 结果页可能快速切换实例，旧请求完成时返回最新请求，避免覆盖当前实例数据。
    return version === groupQueryVersion ? response : latestGroupRequest
  }).catch((error) => {
    if (version !== groupQueryVersion) return latestGroupRequest
    groupQueryError.value = true
    throw error
  })
}

/**
 * 重试候选组查询并清理查询失败状态。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function retryQueryGroups() {
  groupQueryError.value = false
  tableRef.value?.getList()
}

/** 候选组按来源数据列表的列顺序展示，不提供状态或操作列。 */
function buildGroupSourceColumns(modelColumns) {
  const businessColumns = modelColumns.map(column => ({
    label: column.columnName,
    prop: column.columnCode,
    slot: 'businessValue',
    minWidth: 140,
    align: 'left',
    showOverflowTooltip: {effect: 'light'}
  }))
  const overviewColumns = modelColumns.length
      ? []
      : [{label: '主数据模型', prop: 'modelName', minWidth: 160, align: 'center'}]
  return [
    {label: '编号', prop: 'id', slot: 'rowIndex', width: 40, minWidth: 40, sortable: 'custom', align: 'center'},
    {label: '主数据模型', prop: 'modelName', slot: 'modelInfo', minWidth: 320, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '接入任务信息', prop: 'taskName', slot: 'taskInfo', minWidth: 280, align: 'left'},
    {label: '来源系统信息', prop: 'sourceSystemName', slot: 'sourceSystemInfo', minWidth: 280, align: 'left'},
    ...overviewColumns,
    ...businessColumns,
    {label: '形成时间', prop: 'lastReceiveTime', width: 180, minWidth: 180, date: true, sortable: 'custom', align: 'center'}
  ]
}

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

function displayBusinessValue(value) {
  if (value === null || value === undefined || value === '') return '-'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

function parseModelData(modelData) {
  try {
    return modelData ? JSON.parse(modelData) : {}
  } catch {
    return {}
  }
}

/** 复用当前组已加载的数据，排序后由 QtTable 分页。 */
async function queryGroupSources(params = {}) {
  if (!groupRow.value || !instance.value) return { data: { rows: [] } }
  groupSourceError.value = false
  groupSourceRequest ??= listMergeGroupSources(groupRow.value.id, instance.value.id)
  const request = groupSourceRequest
  try {
    const { data } = await request
    if (groupSourceRequest !== request) return { data: { rows: [] } }
    groupSourceFields.value = data.columns || []
    const direction = params.isAsc === 'ascending' ? 1 : -1
    const rows = (data.rows || []).map(row => ({ ...parseModelData(row.modelData), ...row })).sort((left, right) => {
      const leftId = BigInt(left.id)
      const rightId = BigInt(right.id)
      const ids = leftId === rightId ? 0 : leftId < rightId ? -1 : 1
      if (params.orderByColumn === 'id') return direction * ids
      const times = formationTime(left.lastReceiveTime) - formationTime(right.lastReceiveTime)
      return direction * times || -ids
    }).map((row, index) => ({ ...row, sourceIndex: index + 1 }))
    return { data: { rows } }
  } catch (error) {
    if (groupSourceRequest === request) {
      groupSourceRequest = null
      groupSourceError.value = true
    }
    throw error
  }
}

function formationTime(value) {
  if (typeof value === 'number') return value
  return Date.parse(String(value ?? '').replace(' ', 'T')) || 0
}

/**
 * 将数量格式化为紧凑的条数展示文本。
 * @param {*} value 待处理的字段值或状态编码。
 * @param {*} unit 数量展示单位。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatCountCompact(value, unit) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value}${unit}`
}

/**
 * 将匹配分格式化为页面展示文本。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatScoreText(row) {
  if (String(row?.type) === '2' || row?.maxMatchScore === null || row?.maxMatchScore === undefined || row?.maxMatchScore === '') return '-'
  const number = Number(row.maxMatchScore)
  return Number.isNaN(number) ? displayText(row.maxMatchScore) : `${number}%`
}

/**
 * 整理候选组命中规则的展示内容。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function getRuleText(row) {
  if (String(row?.type) === '2' || !row?.ruleCount) return '-'
  return formatCountCompact(row.ruleCount, '条')
}

/** 打开候选组的只读来源列表。 */
function openGroup(row) {
  groupSourceRequest = null
  groupSourceFields.value = []
  groupSourceError.value = false
  groupSourceViewId.value++
  groupRow.value = row
  groupVisible.value = true
}

/**
 * 打开当前弹窗并初始化检测任务数据。
 * @param {*} row 当前执行记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function open(row) {
  instance.value = row
  tableStore.params.taskInstanceId = row.id
  visible.value = true
  nextTick(() => tableRef.value?.resetQuery())
}

/**
 * 关闭当前检测任务弹窗并清理交互状态。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function close() {
  groupQueryVersion++
  groupVisible.value = false
  groupRow.value = null
  visible.value = false
  instance.value = null
  groupQueryError.value = false
  groupSourceRequest = null
  groupSourceFields.value = []
  groupSourceError.value = false
  emit('close')
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.group-source-title {
  margin: var(--qt-space-2) 0;
}

.group-source-wrap {
  margin-top: 10px;
}

.model-info {
  min-width: 0;
}

.model-info-title {
  display: block;
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}
</style>
