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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<!--
  按游标读取重复检测过程日志，提供搜索、自动刷新和下载。
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
      <detail-info :data="runInfo" :items="detailItems" mode="free">
        <template #statusSlot="{ data }">
          <status-tag :status="data.status" />
        </template>
      </detail-info>
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
      <div ref="logContentRef" v-loading="loading && logs.length === 0" class="log-content" @scroll="handleLogScroll">
        <div
          v-for="(log, index) in filteredLogs"
          :key="log.eventSeq"
          class="log-row"
          :class="getLogLevelClass(log.logLevel)"
        >
          <span class="log-line-number">{{ index + 1 }}</span>
          <span class="log-time-text">【{{ formatTime(log.logTime) }}】</span>
          <span v-if="log.stage" class="log-stage-text">{{ log.stage }}</span>
          <span class="log-level-text">{{ log.logLevel }}</span>
          <span class="log-content-text"><span v-for="(part, partIndex) in highlightParts(log.message)" :key="partIndex" :class="{ highlight: part.match }">{{ part.text }}</span></span>
        </div>
        <div v-if="!loading && filteredLogs.length === 0" class="log-empty">暂无过程日志记录</div>
      </div>
    </div>
    <template #footer><div class="log-footer"><el-button @click="visible = false">关闭</el-button></div></template>
  </el-dialog>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onActivated, onBeforeUnmount, onDeactivated, onMounted, ref } from 'vue'

import { Download, Search } from '@element-plus/icons-vue'

import { useDict } from '@/utils/dict'
import DetailInfo from '@/components/DetailInfo/index.vue'
import StatusTag from '@/components/StatusTag/index.vue'
import { getMergeDetectRunLogs } from '@/api/merge/detect'

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
const logs = ref([])
let requestVersion = 0
let refreshTimer
let afterSeq = 0
let shouldAutoScroll = true
let active = true

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
  { label: isRunning.value ? '待检测数据' : '实际检测数据', key: 'sourceCount' },
  { label: '历史比对数据', key: 'historySourceCount' },
  { label: isRunning.value ? '已保存候选组' : '候选对象组', key: 'groupCount' },
  { label: isRunning.value ? '已保存单条组' : '单条对象组', key: 'singleCount' },
  ...(selectedRun.value?.errorMessage ? [{ label: '错误原因', key: 'errorMessage' }] : []),
])
const filteredLogs = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  return logs.value.filter(log => !keyword || formatLog(log).toLowerCase().includes(keyword))
})

/** 打开指定实例并从头增量读取，切换实例时丢弃旧响应。 */
function open(row) {
  requestVersion++
  clearTimeout(refreshTimer)
  afterSeq = 0
  shouldAutoScroll = true
  loading.value = false
  selectedRun.value = row
  logs.value = []
  visible.value = true
  loadError.value = false
  searchKeyword.value = ''
  autoRefresh.value = String(row.status) === '1'
  nextTick(() => loadLogs(requestVersion))
}

/** 使用字典显示实例状态，兼容现有状态编码。 */
function statusLabel(value) {
  return { '1': '运行中', '4': '成功', '2': '失败' }[String(value)]
    || TASK_STATUS_OPTIONS.value?.find(item => String(item.value) === String(value))?.label || '-'
}

/** 按平台时间格式化日志时间。 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

/** 计算运行时长，运行中使用当前时间。 */
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

/** 搜索、下载使用相同的时间、阶段、级别和正文。 */
function formatLog(row) {
  return `${formatTime(row.logTime)} [${row.stage || '-'}] [${row.logLevel}] ${row.message || ''}`
}

/** 旧实例没有过程日志时仅展示明确标记的最终摘要，不伪造历史过程。 */
function legacySummary(row) {
  return [
    '该实例未记录过程日志，以下为最终结果摘要',
    `执行状态：${statusLabel(row.status)}；检测数据：${row.sourceCount ?? '-'} 条；历史比对数据：${row.historySourceCount ?? '-'} 条`,
    `候选对象组：${row.groupCount ?? '-'} 组；单条对象组：${row.singleCount ?? '-'} 组；失败原因：${row.errorMessage || '-'}`,
  ].map((message, index) => ({ eventSeq: -(index + 1), logTime: row.endTime || row.startTime, stage: 'SUMMARY', logLevel: 'INFO', message }))
}

/** 固定本轮上界，追加日志而不覆盖已有内容，避免持续产生日志导致读取不结束。 */
async function loadLogs(version = requestVersion) {
  if (!visible.value || !selectedRun.value || props.taskId == null || loading.value || !active || document.hidden) return
  loading.value = true
  let upperSeq
  try {
    let hasMore = true
    while (hasMore) {
      const { data } = await getMergeDetectRunLogs(props.taskId, selectedRun.value.id, afterSeq)
      if (version !== requestVersion) return
      upperSeq ??= data.lastSeq
      // 分页期间任务结束时，补齐已归档的最后一批日志后再停止刷新。
      if (String(data.run.status) !== '1') upperSeq = data.lastSeq
      selectedRun.value = data.run
      const page = data.logs.filter(row => row.eventSeq > afterSeq && row.eventSeq <= upperSeq)
      logs.value.push(...page)
      if (logs.value.length > 2000) logs.value.splice(0, logs.value.length - 2000)
      const previousSeq = afterSeq
      afterSeq = Math.min(data.nextSeq, upperSeq)
      hasMore = data.hasMore && afterSeq < upperSeq && afterSeq > previousSeq
    }
    loadError.value = false
    if (!isRunning.value && logs.value.length === 0) logs.value = legacySummary(selectedRun.value)
    await nextTick()
    if (version !== requestVersion) return
    if (shouldAutoScroll && !searchKeyword.value && logContentRef.value) {
      logContentRef.value.scrollTop = logContentRef.value.scrollHeight
    }
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

/** 仅对可见且激活的日志弹窗轮询，失败后降低重试频率。 */
function scheduleRefresh() {
  clearTimeout(refreshTimer)
  if (autoRefresh.value && visible.value && active && !document.hidden) {
    refreshTimer = setTimeout(() => loadLogs(requestVersion), loadError.value ? 10000 : 3000)
  }
}

/** 用户向上翻阅时暂停自动滚动。 */
function handleLogScroll() {
  const el = logContentRef.value
  shouldAutoScroll = !el || el.scrollHeight - el.scrollTop - el.clientHeight <= 20
}

/** 搜索后把视口移动到顶部。 */
function handleSearch() {
  if (logContentRef.value) logContentRef.value.scrollTop = 0
}

/** 开启自动刷新后立即读取。 */
function handleAutoRefreshChange() {
  clearTimeout(refreshTimer)
  if (autoRefresh.value) loadLogs(requestVersion)
}

/** 按下载开始时的日志上界导出全部过程，不受页面保留条数限制。 */
async function download(row = selectedRun.value) {
  if (!row || props.taskId == null) return
  const taskId = props.taskId
  const records = []
  let cursor = 0
  let upperSeq
  let run = row
  try {
    let hasMore = true
    while (hasMore) {
      const { data } = await getMergeDetectRunLogs(taskId, row.id, cursor)
      upperSeq ??= data.lastSeq
      run = data.run
      records.push(...data.logs.filter(log => log.eventSeq > cursor && log.eventSeq <= upperSeq).map(formatLog))
      const previousSeq = cursor
      cursor = Math.min(data.nextSeq, upperSeq)
      hasMore = data.hasMore && cursor < upperSeq && cursor > previousSeq
    }
    if (!records.length && String(run.status) !== '1') records.push(...legacySummary(run).map(formatLog))
    const content = [
      `执行编号：${run.instanceNo || run.id || '-'}`,
      `执行状态：${statusLabel(run.status)}`,
      `开始时间：${formatTime(run.startTime)}`,
      `结束时间：${formatTime(run.endTime)}`,
      '',
      ...records,
    ].join('\n')
    proxy.downloadContent(content, `${run.instanceNo || run.id || '重复检测'}_日志.log`)
  } catch {
    proxy.$modal.msgWarning('执行日志下载失败，请稍后重试')
  }
}

/** 按字面关键词拆分正文，保持纯文本渲染并高亮搜索结果。 */
function highlightParts(message) {
  const text = String(message || '')
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return [{ text, match: false }]
  const parts = []
  const lowerText = text.toLowerCase()
  let start = 0
  let index = lowerText.indexOf(keyword)
  while (index >= 0) {
    parts.push({ text: text.slice(start, index), match: false })
    parts.push({ text: text.slice(index, index + keyword.length), match: true })
    start = index + keyword.length
    index = lowerText.indexOf(keyword, start)
  }
  parts.push({ text: text.slice(start), match: false })
  return parts
}

/** 根据级别显示日志颜色。 */
function getLogLevelClass(level) {
  return { ERROR: 'error', WARN: 'warn', INFO: 'info', DEBUG: 'debug' }[level] || 'info'
}

/** 关闭后取消轮询并丢弃尚未返回的请求。 */
function handleClosed() {
  clearTimeout(refreshTimer)
  requestVersion++
  selectedRun.value = null
  logs.value = []
  loading.value = false
  autoRefresh.value = false
}

/** 浏览器重新可见时继续增量读取。 */
function handleVisibilityChange() {
  clearTimeout(refreshTimer)
  if (!document.hidden && autoRefresh.value) loadLogs(requestVersion)
}

onMounted(() => {
  dialogTarget.value = document.querySelector('.app-container')
  document.addEventListener('visibilitychange', handleVisibilityChange)
})
onActivated(() => { active = true; scheduleRefresh() })
onDeactivated(() => { active = false; clearTimeout(refreshTimer) })
onBeforeUnmount(() => {
  handleClosed()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
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
