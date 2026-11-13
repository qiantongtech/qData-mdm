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
  使用已有检测实例生成执行摘要，提供搜索、自动刷新和下载。
  @prop taskId 检测任务编号。
  @prop taskNo 检测任务编号文本。
  @author chentangwen
-->
<template>
  <el-dialog
    v-if="dialogTarget"
    v-model="visible"
    title="查看日志"
    append-to-body
    :append-to="dialogTarget"
    draggable
    destroy-on-close
    @closed="handleClosed"
    class="medium-dialog ingest-run-log-dialog detect-instance-logs-dialog"
  >
    <div v-if="selectedRun" class="log-dialog-container">
      <DetailInfo :data="runInfo" :items="detailItems" mode="free">
        <template #statusSlot="{ data }">
          <StatusTag :status="data.status" />
        </template>
      </DetailInfo>
      <div class="log-toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索日志内容"
          clearable
          @keyup.enter="handleSearch"
          class="search-input"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="toolbar-right">
          <span class="auto-refresh-label">自动刷新</span>
          <el-switch v-model="autoRefresh" :disabled="!isRunning" @change="handleAutoRefreshChange" />
          <el-button type="warning" size="small" @click="download()"><el-icon class="mr5"><Download /></el-icon>下载日志</el-button>
        </div>
      </div>
      <el-alert
        v-if="loadError"
        title="日志加载失败，请重新打开详情重试"
        type="error"
        :closable="false"
      />
      <div ref="logContentRef" v-loading="loading" class="log-content">
        <div
          v-for="(log, index) in filteredLogs"
          :key="log.key"
          class="log-row"
          :class="getLogLevelClass(log.logLevel)"
        >
          <span class="log-line-number">{{ index + 1 }}</span>
          <span class="log-level-text">{{ log.logLevel }}</span>
          <span class="log-content-text">{{ log.message }}</span>
        </div>
        <div v-if="!loading && filteredLogs.length === 0" class="log-empty">暂无记录</div>
      </div>
    </div>
    <template #footer><div class="log-footer"><el-button @click="visible = false">关闭</el-button></div></template>
  </el-dialog>
</template>

<script setup>
import { useDict } from '@/utils/dict'
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Download, Search } from '@element-plus/icons-vue'
import DetailInfo from '@/components/DetailInfo/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import { getMergeInstance } from '@/api/merge/detect'

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

const { merge_task_status: TASK_STATUS_OPTIONS } = useDict('merge_task_status')

const props = defineProps({
  taskId: { type: [String, Number], default: undefined },
  taskNo: { type: String, default: '' },
})
const { proxy } = getCurrentInstance()
const dialogTarget = ref()
const visible = ref(false)
const selectedRun = ref(null)
const loading = ref(false)
const loadError = ref(false)
const searchKeyword = ref('')
const autoRefresh = ref(false)
const logContentRef = ref()
let requestVersion = 0
let refreshTimer

const isRunning = computed(() => String(selectedRun.value?.status) === '1')
const runInfo = computed(() => ({
  ...selectedRun.value,
  taskNo: props.taskNo || '-',
  runDuration: formatDuration(selectedRun.value),
}))
const detailItems = computed(() => [
  { label: '检测任务编号', key: 'taskNo' },
  { label: '执行编号', key: 'instanceNo' },
  { label: '执行状态', key: 'status', slot: 'statusSlot' },
  { label: '开始时间', key: 'startTime', type: 'time', className: 'mt2 mb2' },
  { label: '已运行', key: 'runDuration', className: 'mt2 mb2' },
  ...(selectedRun.value?.errorMessage ? [{ label: '错误原因', key: 'errorMessage' }] : []),
])
/**
 * 将数据库已有实例字段转为统一的执行摘要，供页面和下载共同使用。
 * @param {*} row 当前业务记录。
 * @returns {Array} 整理后的业务记录集合。
 */
function buildSummaryLines(row) {
  if (!row) return []
  return [
    { key: 'status', logLevel: 'INFO', message: `执行状态：${statusLabel(row.status)}` },
    { key: 'startTime', logLevel: 'INFO', message: `开始时间：${formatTime(row.startTime)}` },
    { key: 'endTime', logLevel: 'INFO', message: `结束时间：${formatTime(row.endTime)}` },
    { key: 'sourceCount', logLevel: 'INFO', message: `实际检测数据：${row.sourceCount ?? '-'}` },
    { key: 'groupCount', logLevel: 'INFO', message: `候选对象组：${row.groupCount ?? '-'}` },
    { key: 'errorMessage', logLevel: row.errorMessage ? 'ERROR' : 'INFO', message: `失败原因：${row.errorMessage || '-'}` },
  ]
}

const summaryLines = computed(() => buildSummaryLines(selectedRun.value))
const filteredLogs = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return summaryLines.value.filter(log => !keyword || log.message.toLowerCase().includes(keyword))
})

/**
 * 打开指定运行实例并读取最新执行摘要。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function open(row) {
  requestVersion++
  clearTimeout(refreshTimer)
  loading.value = false
  selectedRun.value = row
  visible.value = true
  loadError.value = false
  searchKeyword.value = ''
  autoRefresh.value = String(row.status) === '1'
  nextTick(loadSummary)
}

/**
 * 将实例状态转成页面显示文本。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function statusLabel(value) {
  return { '1': '运行中', '4': '成功', '2': '失败' }[String(value)] || getOptionLabel(TASK_STATUS_OPTIONS.value, value) || '-'
}

/**
 * 按平台时间格式化实例时间。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

/**
 * 计算实例运行时长，运行中的实例按当前时间计算。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatDuration(row) {
  if (!row?.startTime) return '-'
  const start = new Date(String(row.startTime).replace(' ', 'T')).getTime()
  const end = row.endTime ? new Date(String(row.endTime).replace(' ', 'T')).getTime() : Date.now()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return '-'
  const seconds = Math.floor((end - start) / 1000)
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  return `${Math.floor(seconds / 3600)}时${Math.floor(seconds % 3600 / 60)}分`
}

/**
 * 重新获取实例，运行中的摘要随已保存状态更新。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadSummary() {
  if (!visible.value || !selectedRun.value || props.taskId == null || loading.value) return
  const version = ++requestVersion
  loading.value = true
  loadError.value = false
  try {
    const { data } = await getMergeInstance(props.taskId, selectedRun.value.id)
    if (version !== requestVersion) return
    selectedRun.value = data
    if (!isRunning.value) autoRefresh.value = false
  } catch {
    if (version === requestVersion) loadError.value = true
  } finally {
    if (version === requestVersion) {
      loading.value = false
      scheduleRefresh()
    }
  }
}

/**
 * 自动刷新开启时定时读取最新摘要，已完成实例也可刷新。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function scheduleRefresh() {
  clearTimeout(refreshTimer)
  if (autoRefresh.value && visible.value) {
    refreshTimer = setTimeout(loadSummary, loadError.value ? 10000 : (isRunning.value ? 3000 : 10000))
  }
}

/**
 * 搜索后将日志视口移动到顶部。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleSearch() {
  if (logContentRef.value) logContentRef.value.scrollTop = 0
}

/**
 * 切换自动刷新后重置定时器。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleAutoRefreshChange() {
  clearTimeout(refreshTimer)
  if (autoRefresh.value) loadSummary()
}

/**
 * 用实例的最新字段下载执行摘要，历史实例也可以下载。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function download(row = selectedRun.value) {
  if (!row || props.taskId == null) return
  try {
    const { data } = await getMergeInstance(props.taskId, row.id)
    const content = [`执行编号：${data.instanceNo || data.id || '-'}`, ...buildSummaryLines(data).map(line => line.message)].join('\n')
    proxy.downloadContent(content, `${data.instanceNo || data.id || '重复检测'}_执行摘要.log`)
  } catch {
    proxy.$modal?.msgError?.('执行摘要下载失败，请稍后重试')
  }
}

/**
 * 根据日志级别匹配终端日志颜色。
 * @param {*} level 调用方传入的业务配置。
 * @returns {*} 当前业务计算结果。
 */
function getLogLevelClass(level) {
  return { ERROR: 'error', WARN: 'warn', INFO: 'info', DEBUG: 'debug' }[level] || 'info'
}

/**
 * 关闭弹窗时停止定时器并清理当前实例状态。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleClosed() {
  clearTimeout(refreshTimer)
  requestVersion++
  selectedRun.value = null
  loading.value = false
  autoRefresh.value = false
}

watch(autoRefresh, scheduleRefresh)
// 通用详情信息的边框样式
onMounted(() => { dialogTarget.value = document.querySelector('.app-container') })
onBeforeUnmount(() => {
  clearTimeout(refreshTimer)
  requestVersion++
})

defineExpose({ open, download })
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
.detect-instance-logs-dialog {
  max-width: calc(100vw - 40px);
}

.detect-instance-logs-dialog :deep(.el-dialog__body) {
  padding: 20px 40px !important;
}
</style>
