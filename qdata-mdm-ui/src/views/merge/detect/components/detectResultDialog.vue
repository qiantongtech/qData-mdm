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
      width="min(1000px, 94vw)"
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

        <div v-loading="getGroupDetail(groupRow).loading">
          <qt-wrap
            :columns="groupDetailTableStore.columns"
            :tableRef="groupDetailTableRef"
            :config="{ fullContent: false, actions: { table: { show: false, search: false } } }"
            class="group-source-wrap"
          >
            <template #actions-data>
              <div class="h2-title group-source-title">来源数据</div>
            </template>
            <qt-table ref="groupDetailTableRef" v-bind="groupDetailTableStore" :columns="groupDetailTableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #sourceSystem="{ row }">{{ row.systemName || row.systemCode || '-' }}</template>
              <template #sourceRecordId="{ row }">{{ row.stableRecordId || row.sourceRecordId || '-' }}</template>
              <template #businessName="{ row }">{{ getMemberBusinessName(row) }}</template>
              <template #businessKey="{ row }">{{ getMemberBusinessKey(row) }}</template>
            </qt-table>
          </qt-wrap>
        </div>
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
import { getMergeReview } from '@/api/merge/review'
import { listMergeGroup } from '@/api/merge/detect'
import { getSourceDataModelColumns } from '@/api/ingest/LOG'

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

const emit = defineEmits(['close'])
const visible = ref(false)
const instance = ref(null)
const tableRef = ref()
const groupDetailTableRef = ref()
const resultColumns = ref([])
const groupDetails = reactive({})
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

const groupDetailTableStore = reactive({
  func: queryGroupMembers,
  params: { groupId: undefined },
  config: {
    initResquest: false,
    autoPagination: true,
    table: { stripe: true, rowKey: 'memberId' },
    pagination: { pageSizes: [10, 20, 50], notAutoScroll: true },
  },
  columns: [
    { label: '编号', prop: 'sourceIndex', sortable: 'custom', width: 80, minWidth: 80, align: 'center' },
    { label: '来源系统', prop: 'systemName', slot: 'sourceSystem', minWidth: 150, align: 'left', showOverflowTooltip: true },
    { label: '来源记录ID', prop: 'stableRecordId', slot: 'sourceRecordId', minWidth: 180, align: 'left', showOverflowTooltip: true },
    { label: '业务对象', prop: 'businessName', slot: 'businessName', minWidth: 220, align: 'left', showOverflowTooltip: true },
    { label: '来源业务标识', prop: 'businessKey', slot: 'businessKey', minWidth: 200, align: 'left', showOverflowTooltip: true },
  ],
})

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

/**
 * 读取候选组来源成员并适配表格数据。
 * @param {*} params 表格查询、分页及排序参数。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function queryGroupMembers(params = {}) {
  const members = getTargetMembers({ id: groupDetailTableStore.params.groupId })
    .map((member, index) => ({ ...member, sourceIndex: index + 1 }))
  if (params.orderByColumn === 'sourceIndex' && params.isAsc) {
    const direction = params.isAsc === 'descending' ? -1 : 1
    members.sort((left, right) => direction * (left.sourceIndex - right.sourceIndex))
  }
  return Promise.resolve({ data: { rows: members, total: members.length } })
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

/**
 * 读取当前模型字段并构建结果展示列。
 * @param {*} modelId 主数据模型编号。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadResultColumns(modelId) {
  resultColumns.value = []
  if (!modelId) return
  try {
    const res = await getSourceDataModelColumns(modelId)
    resultColumns.value = res.data || []
  } catch {
    resultColumns.value = []
  }
}

/**
 * 根据模型字段编码和路径读取来源值。
 * @param {*} member 候选组来源成员。
 * @param {*} column 模型列定义。
 * @returns {*} 当前业务计算结果。
 */
function getColumnValue(member, column) {
  if (!member?.values || !column) return ''
  const keys = [column.columnCode, column.code, column.id]
  for (const key of keys) {
    if (key === undefined || key === null || key === '') continue
    const value = member.values[key] ?? member.values[String(key)]
    if (value !== undefined && value !== null && value !== '') return value
  }
  return ''
}

/**
 * 读取来源成员的业务名称。
 * @param {*} member 候选组来源成员。
 * @returns {string} 用于页面展示的格式化文本。
 */
function getMemberBusinessName(member) {
  const column = resultColumns.value.find((item) => String(item.nameFlag) === '1')
    || resultColumns.value.find((item) => String(item.columnName || '').includes('名称'))
  return getColumnValue(member, column) || '-'
}

/**
 * 读取来源成员的业务标识。
 * @param {*} member 候选组来源成员。
 * @returns {*} 当前业务计算结果。
 */
function getMemberBusinessKey(member) {
  const column = resultColumns.value.find((item) => String(item.columnName || '').includes('代码'))
    || resultColumns.value.find((item) => String(item.uniqueFlag) === '1' && String(item.nameFlag) !== '1')
  return getColumnValue(member, column) || '-'
}

/**
 * 按候选组编号取得组详情。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getGroupDetail(row) {
  return groupDetails[row.id] || { loading: false, members: [] }
}

/**
 * 筛选候选组中参与处理的来源成员。
 * @param {*} row 当前业务记录。
 * @returns {Array} 整理后的业务记录集合。
 */
function getTargetMembers(row) {
  return (getGroupDetail(row).members || []).filter((item) => String(item.memberRole) !== '2')
}

/**
 * 打开候选组详情并加载成员数据。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openGroup(row) {
  groupRow.value = row
  groupDetailTableStore.params.groupId = row.id
  groupVisible.value = true
  loadGroupDetail(row).finally(() => nextTick(() => groupDetailTableRef.value?.resetQuery()))
}

/**
 * 加载当前候选组的详情和来源成员。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadGroupDetail(row) {
  if (groupDetails[row.id]) return
  groupDetails[row.id] = { loading: true, members: [] }
  try {
    const res = await getMergeReview(row.id)
    groupDetails[row.id] = { loading: false, members: res.data?.members || [] }
  } catch {
    groupDetails[row.id] = { loading: false, members: [] }
  }
}

/**
 * 打开当前弹窗并初始化检测任务数据。
 * @param {*} row 当前业务记录。
 * @param {*} modelId 主数据模型编号。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function open(row, modelId) {
  instance.value = row
  tableStore.params.taskInstanceId = row.id
  loadResultColumns(row.modelId || modelId)
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
  resultColumns.value = []
  Object.keys(groupDetails).forEach((key) => delete groupDetails[key])
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
</style>
