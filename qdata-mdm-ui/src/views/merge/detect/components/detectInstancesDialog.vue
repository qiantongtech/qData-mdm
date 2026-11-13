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
  展示检测任务的执行实例、状态统计以及日志查看和下载入口。
  @prop modelValue 弹窗显示状态（v-model）。
  @prop task 当前检测任务。
  @emits update:modelValue 同步弹窗显示状态。
  @author chentangwen
-->
<template>
  <el-dialog
    v-if="dialogTarget"
    v-model="dialogVisible"
    title="任务实例"
    append-to-body
    :append-to="dialogTarget"
    draggable
    destroy-on-close
    class="medium-dialog detect-instances-dialog"
  >
    <div class="run-records task-instances detect-task-instances">
      <div class="run-overview">
        <div class="run-overview__main">
          <el-icon class="run-overview__icon"><InfoFilled /></el-icon>
          <span>
            当前共有 运行中
            <span class="run-overview__count run-overview__running" @click="openLatestRunningRun">{{ counts.running }}</span>
            个， 成功
            <span class="run-overview__count run-overview__success">{{ counts.success }}</span> 个， 失败
            <span class="run-overview__count run-overview__failed">{{ counts.failure }}</span> 个实例
          </span>
        </div>
        <div class="run-overview__meta">
          <span>最后刷新时间：{{ refreshedAt }}</span>
          <el-tooltip content="刷新任务实例" placement="top">
            <el-button
              :icon="Refresh"
              link
              type="primary"
              :loading="statsLoading"
              aria-label="刷新任务实例"
              @click="refresh"
            />
          </el-tooltip>
        </div>
      </div>
      <div class="run-toolbar">
        <el-tabs v-model="activeStatus" class="run-tabs" aria-label="筛选执行状态" @tab-change="changeStatus">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="运行中" name="1" />
          <el-tab-pane label="成功" name="4" />
          <el-tab-pane label="失败" name="2" />
        </el-tabs>
      </div>
      <el-alert
        v-if="error"
        title="运行记录加载失败，请点击刷新重试"
        type="error"
        :closable="false"
        show-icon
      >
        <el-button link type="primary" @click="refresh">重试</el-button>
      </el-alert>
      <div v-loading="tableLoading" class="task-instances__table" :class="{ 'is-empty': !tableRows.length }">
        <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
          <template #sourceCount="{ row }">{{ formatCountText(row.sourceCount, '条') }}</template>
          <template #groupCount="{ row }">{{ String(row.status) === '4' ? formatCountText(row.groupCount, '组') : '-' }}</template>
          <template #status="{ row }">
            <div @click="openLogs(row)">
              <StatusTag v-if="row.status != null && row.status !== ''" :status="row.status" />
              <span v-else>-</span>
            </div>
          </template>
          <template #startTime="{ row }">{{ formatTime(row.startTime) }}</template>
          <template #duration="{ row }">{{ formatDuration(row) }}</template>
          <template #action="{ row }">
            <div class="run-actions">
              <el-button
                link
                type="primary"
                :icon="View"
                @click="openLogs(row)"
              >{{ String(row.status) === '1' ? '实时日志' : '查看日志' }}</el-button>
              <el-button
                link
                type="warning"
                :icon="Download"
                @mousedown="event => event.preventDefault()"
                @click="downloadLogs(row)"
              >下载日志</el-button>
            </div>
          </template>
        </qt-table>
        <div v-if="!tableRows.length" class="task-instances__empty-pagination" aria-hidden="true" />
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer"><el-button @click="dialogVisible = false">关闭</el-button></div>
    </template>
  </el-dialog>
  <DetectInstanceLogsDialog ref="logsDialogRef" :task-id="task?.id" :task-no="task?.taskNo" />
</template>

<script setup name="DetectInstancesDialog">
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Download, InfoFilled, Refresh, View } from '@element-plus/icons-vue'
import { listMergeInstance } from '@/api/merge/detect'
import StatusTag from '@/components/StatusTag/index.vue'

import DetectInstanceLogsDialog from './detectInstanceLogsDialog.vue'

function formatCountText(value, unit) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} ${unit}`
}

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  task: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue'])
const { proxy } = getCurrentInstance()
const dialogTarget = ref()
const dialogVisible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const tableRef = ref()
const tableRows = ref([])
const tableLoading = ref(false)
const activeStatus = ref('all')
const error = ref(false)
const statsLoading = ref(false)
const refreshedAt = ref('-')
const logsDialogRef = ref()
const counts = reactive({ running: 0, success: 0, failure: 0 })
const clockNow = ref(Date.now())
let requestVersion = 0
let summaryVersion = 0
let clockTimer

const tableStore = reactive({
  func: queryInstances,
  params: { taskId: undefined, status: undefined },
  config: {
    initResquest: false,
    sort: true,
    table: { stripe: true, rowKey: 'id', height: '525px', defaultSort: { prop: 'startTime', order: 'descending' } },
    pagination: { pageSizes: [10, 20, 50, 100], pagerCount: 5, notAutoScroll: true },
  },
  columns: [
    { label: '编号', prop: 'id', sortable: true, width: 65, minWidth: 65, align: 'center' },
    { label: '执行编号', prop: 'instanceNo', minWidth: 190, showOverflowTooltip: true },
    { label: '实际检测数据', prop: 'sourceCount', slot: 'sourceCount', minWidth: 125 },
    { label: '候选对象组', prop: 'groupCount', slot: 'groupCount', minWidth: 120 },
    { label: '执行状态', prop: 'status', slot: 'status', width: 105, minWidth: 105 },
    { label: '开始时间', prop: 'startTime', slot: 'startTime', sortable: true, sortableKey: 'start_time', minWidth: 170 },
    { label: '运行时长', prop: 'duration', slot: 'duration', width: 105, minWidth: 105 },
    { label: '操作', slot: 'action', width: 190, minWidth: 190, fixed: 'right', align: 'center' },
  ],
})

/**
 * 将接口时间格式化为页面展示文本。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

/**
 * 根据实例起止时间计算运行时长。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatDuration(row) {
  if (!row.startTime || (!row.endTime && String(row.status) !== '1')) return '-'
  const start = new Date(String(row.startTime).replace(' ', 'T')).getTime()
  const end = row.endTime ? new Date(String(row.endTime).replace(' ', 'T')).getTime() : clockNow.value
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return '-'
  const seconds = Math.floor((end - start) / 1000)
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  return `${Math.floor(seconds / 3600)}时${Math.floor(seconds % 3600 / 60)}分`
}

/**
 * 按当前任务、状态和表头排序获取实例，由项目通用表格负责分页。
 * @param {*} params 业务筛选、分页和排序参数。
 * @returns {Promise<*>} 当前业务计算结果。
 */
async function queryInstances(params) {
  const version = ++requestVersion
  error.value = false
  tableLoading.value = true
  try {
    const orderByColumn = params.isAsc ? (params.orderByColumn === 'id' ? 'id' : 'start_time') : 'start_time'
    const response = await listMergeInstance({
      ...params,
      taskId: props.task?.id,
      status: tableStore.params.status,
      orderByColumn,
      isAsc: params.isAsc === 'ascending' ? 'asc' : 'descending',
    })
    if (version !== requestVersion) return { data: { rows: [], total: 0 } }
    tableRows.value = response.data?.rows || []
    return response
  } catch {
    if (version === requestVersion) {
      tableRows.value = []
      error.value = true
    }
    return { data: { rows: [], total: 0 } }
  } finally {
    if (version === requestVersion) tableLoading.value = false
  }
}

/**
 * 查询当前任务各执行状态的实例数量。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadCounts() {
  if (!props.task?.id) return
  const version = ++summaryVersion
  const taskId = props.task.id
  statsLoading.value = true
  const statuses = [['running', '1'], ['success', '4'], ['failure', '2']]
  const results = await Promise.allSettled(statuses.map(([, status]) =>
    listMergeInstance({ taskId, status, pageNum: 1, pageSize: 1 })))
  if (version !== summaryVersion || !props.modelValue || props.task?.id !== taskId) return
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') counts[statuses[index][0]] = Number(result.value.data?.total || 0)
    else error.value = true
  })
  refreshedAt.value = formatTime(new Date())
  statsLoading.value = false
}

/**
 * 刷新当前实例列表及统计信息。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function refresh() {
  error.value = false
  loadCounts()
  tableRef.value?.getList()
}

/**
 * 切换执行状态筛选并重置列表页码。
 * @param {*} status 业务处理状态。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function changeStatus(status) {
  tableStore.params.status = status === 'all' ? undefined : status
  tableRef.value?.resetQuery()
}

/**
 * 打开所选执行实例的日志摘要。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openLogs(row) {
  logsDialogRef.value?.open(row)
}

/** 查询并打开最近的运行中实例日志。 */
async function openLatestRunningRun() {
  try {
    const { data } = await listMergeInstance({
      taskId: props.task?.id,
      status: '1',
      pageNum: 1,
      pageSize: 1,
      orderByColumn: 'start_time',
      isAsc: 'descending',
    })
    const latestRunning = data?.rows?.[0]
    if (latestRunning) openLogs(latestRunning)
    else proxy.$modal.msgWarning('没有找到运行中的任务实例')
  } catch {
    proxy.$modal.msgError('运行实例加载失败，请稍后重试')
  }
}

/**
 * 下载所选执行实例的日志摘要。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function downloadLogs(row) {
  logsDialogRef.value?.download(row)
}

watch(() => [props.modelValue, props.task?.id], ([visible]) => {
  requestVersion++
  summaryVersion++
  clearInterval(clockTimer)
  if (!visible || !props.task?.id) {
    tableRows.value = []
    tableLoading.value = false
    return
  }
  activeStatus.value = 'all'
  tableRows.value = []
  tableStore.params.taskId = props.task.id
  tableStore.params.status = undefined
  counts.running = 0
  counts.success = 0
  counts.failure = 0
  refreshedAt.value = '-'
  clockTimer = setInterval(() => { clockNow.value = Date.now() }, 1000)
  nextTick(() => {
    tableRef.value?.resetQuery()
    loadCounts()
  })
}, { immediate: true })

onBeforeUnmount(() => {
  requestVersion++
  summaryVersion++
  clearInterval(clockTimer)
})

// 运行实例弹窗与接入任务一样挂在页面容器内，复用项目的表格和状态标签样式。
onMounted(() => { dialogTarget.value = document.querySelector('.app-container') })
</script>

<style scoped lang="scss">
/* 任务实例弹窗与详情页共用的运行概况、记录操作及日志展示样式。 */
.run-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--qt-space-4);
  padding: var(--qt-space-3) var(--qt-space-5);
  background: var(--qt-primary-light);
  margin-bottom: var(--qt-space-2);
  font-size: var(--qt-font-size-base);
}

.run-overview__main {
  display: flex;
  align-items: center;
  gap: var(--qt-space-2);
}

.run-overview__running {
  color: var(--qt-primary);
}

.run-overview__success {
  color: var(--qt-status-success);
}

.run-overview__failed {
  color: var(--qt-status-danger);
}

/* 刷新时间等辅助信息保持单行，与运行数量分开排列。 */
.run-overview__meta {
  display: flex;
  align-items: center;
  gap: var(--qt-space-2);
  color: var(--qt-text-regular);
  font-size: var(--el-font-size-small);
  white-space: nowrap;
}

.run-toolbar {
  margin-bottom: var(--qt-space-3);
}

.run-tabs {
  width: 100%;
}

.run-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.run-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

/* 日志弹窗使用固定总高度，工具栏之外的空间留给可滚动日志正文。 */
.log-dialog-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  min-height: 0;
}

.log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--qt-space-3);
}

.search-input {
  width: 300px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--qt-space-4);
}

.auto-refresh-label {
  font-size: var(--qt-font-size-secondary);
  color: var(--qt-text-regular);
}

/* 日志区独立纵向滚动，等宽字体便于核对时间、阶段和正文。 */
.log-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  border: var(--el-border-width) solid var(--el-border-color-light);
  border-radius: var(--qt-radius-medium);
  padding: var(--qt-space-1) 0;
  font-family: Consolas, Monaco, Courier New, monospace;
  font-size: var(--qt-font-size-secondary);
  background-color: var(--el-fill-color-lighter);
}

.log-row {
  display: flex;
  align-items: flex-start;
  padding: calc(var(--qt-space-1) / 2) var(--qt-space-3);
  line-height: 1.6;
  margin-top: calc(var(--qt-space-1) / 2);
}

/* 行号、时间与阶段保持宽度，正文占据剩余空间。 */
.log-line-number {
  width: 30px;
  text-align: right;
  color: var(--qt-text-secondary);
  margin-right: var(--qt-space-2);
  flex-shrink: 0;
}

.log-time-text {
  color: var(--qt-text-primary);
  margin-right: var(--qt-space-2);
  flex-shrink: 0;
}

.log-level-text {
  text-align: left;
  color: var(--qt-text-regular);
  margin-right: var(--qt-space-2);
  flex-shrink: 0;
}

.log-stage-text {
  color: var(--qt-text-regular);
  margin-right: var(--qt-space-2);
  flex-shrink: 0;
}

/* 保留日志换行并允许长文本折行，避免正文撑开弹窗。 */
.log-content-text {
  color: var(--qt-text-primary);
  flex: 1;
  min-width: 0;
  word-break: break-all;
  white-space: pre-wrap;
}

.log-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--el-text-color-placeholder);
}

/* 错误和警告同时强调级别及正文，普通日志仅强调级别。 */
.log-row.error .log-level-text,
.log-row.error .log-content-text {
  color: var(--qt-status-danger);
}

.log-row.warn .log-level-text,
.log-row.warn .log-content-text {
  color: var(--qt-status-warning);
}

.log-row.info .log-level-text,
.log-row.debug .log-level-text {
  color: var(--qt-primary);
}

.log-row.info .log-content-text,
.log-row.debug .log-content-text {
  color: var(--qt-text-primary);
}

/* 关键词使用强调色和字重，配合模板中的纯文本片段高亮。 */
.highlight {
  color: var(--el-color-danger);
  font-weight: var(--qt-font-weight-bold);
}

.log-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

:deep(.ingest-run-log-dialog .el-dialog__body) {
  padding: 20px 40px !important;
}


.run-overview__icon {
  color: var(--qt-primary);
}

/* 任务实例弹窗。 */
.task-instances .run-overview {
  margin-bottom: 0;
  padding: 8px 19px;
  border-radius: var(--qt-radius-small);
  box-shadow: -1px 1px 2px rgba(var(--qt-primary-rgb), 0.08);
  color: var(--qt-text-primary);
  line-height: 1.5;
}

.task-instances .run-overview__icon {
  font-size: var(--qt-font-size-title);
}

.task-instances .run-overview__main {
  gap: 0;
}

.task-instances .run-overview__main > span {
  margin-left: 5px;
  line-height: 1.5;
}

.task-instances .run-overview__count {
  margin-left: 5px;
  line-height: 1.5;
}

.task-instances .run-overview__count {
  font-weight: bold;
}

.task-instances .run-overview__meta {
  gap: 10px;
  font-size: var(--qt-font-size-base);
}

.task-instances .run-toolbar {
  margin-bottom: 15px;
}

/* 整库同步的 QtTable 在空数据时保留分页容器及其内边距。 */
.task-instances .task-instances__empty-pagination {
  padding: 20px 10px;
}

.task-instances .task-instances__table.is-empty {
  :deep(.el-table__inner-wrapper::before) {
    background-color: transparent !important;
  }
}
</style>
<style scoped>
.detect-instances-dialog {
  max-width: calc(100vw - 40px);
}

.detect-instances-dialog :deep(.el-dialog__body) {
  padding: 20px 40px !important;
}

.detect-instances-dialog :deep(.el-dialog__footer) {
  padding: 15px 20px;
}
</style>
