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
  任务运行实例弹窗：汇总状态、筛选运行记录并查看或下载执行日志。
  props：taskId、taskName 标识当前任务；active 表示所在页签是否激活。
  emits：loaded 返回 total 和 latestRun，供父页更新运行概况。
-->
<template>
  <div class="run-records task-instances" ref="app-container">
    <div class="run-overview">
      <div class="run-overview__main">
        <el-icon class="run-overview__icon"><info-filled /></el-icon>
        <span>
          当前共有 运行中
          <span class="run-overview__count run-overview__running" @click="openLatestRunningRun">{{ statusCounts.running }}</span>
          个， 成功
          <span class="run-overview__count run-overview__success">{{ statusCounts.success }}</span> 个， 失败
          <span class="run-overview__count run-overview__failed">{{ statusCounts.failed }}</span> 个实例
        </span>
      </div>
      <div class="run-overview__meta">
        <span>最后刷新时间：{{ refreshedAt || '-' }}</span>
        <el-tooltip content="刷新任务实例" placement="top">
          <el-button link
                     type="primary"
                     icon="Refresh"
                     :loading="listLoading"
                     @click="handleRefresh"
                     aria-label="刷新任务实例"
          />
        </el-tooltip>
      </div>
    </div>
    <div class="run-toolbar">
      <el-tabs v-model="statusFilter"
               class="run-tabs"
               aria-label="筛选执行状态"
               @tab-change="handleStatusChange"
      >
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane label="运行中" name="RUNNING" />
        <el-tab-pane label="成功" name="SUCCESS" />
        <el-tab-pane label="失败" name="FAILED" />
      </el-tabs>
    </div>
    <el-alert v-if="error"
              title="运行记录加载失败，请点击刷新重试"
              type="error"
              :closable="false"
              show-icon
    />
    <div v-loading="listLoading" class="task-instances__table" :class="{ 'is-empty': filteredRuns.length === 0 }">
      <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
        <template #taskName>{{ props.taskName || '-' }}</template>
        <template #id="{ row }">{{ row.id }}</template>
        <template #triggerType="{ row }">{{ getTriggerLabel(row.triggerType) }}</template>
        <template #status="{ row }">
          <div @click="showDetail(row)">
            <status-tag v-if="row.status != null && row.status !== ''" :status="row.status" />
            <span v-else>-</span>
          </div>
        </template>
        <template #startTime="{ row }">{{ formatTime(row.startTime) }}</template>
        <template #count="{ row, column_data }">{{ row[(column_data.property || column_data.prop)] ?? '-' }}</template>
        <template #durationMillis="{ row }">{{ formatDuration(row.durationMillis) }}</template>
        <template #errorMessage="{ row }">{{ row.errorMessage || '-' }}</template>
        <template #handle="{ row }">
          <div class="run-actions">
            <el-button
              link
              type="primary"
              :icon="IconView"
              @click="showDetail(row)"
            >{{ row.status === '1' ? '实时日志' : '查看日志' }}</el-button>
            <el-button
              link
              type="warning"
              :icon="Download"
              @mousedown="event => event.preventDefault()"
              @click="downloadLog(row)"
            >下载日志</el-button>
          </div>
        </template>
      </qt-table>
      <div v-if="filteredRuns.length === 0" class="task-instances__empty-pagination" aria-hidden="true" />
    </div>
    <el-dialog
      v-if="dialogTarget"
      :key="dialogKey"
      v-model="detailVisible"
      title="查看日志"

      class="medium-dialog ingest-run-log-dialog"
      append-to-body
      :append-to="dialogTarget"
      destroy-on-close
      draggable
      @close="pauseRefresh"
    >
      <div class="log-dialog-container">
        <detail-info :data="logTaskInfo" :items="detailItems" mode="free">
          <template #statusSlot="{ data }">
            <status-tag :status="data.status" />
          </template>
        </detail-info>
        <div class="log-toolbar">
          <el-input v-model="searchKeyword"
                    placeholder="搜索日志内容"
                    class="search-input"
                    clearable
                    @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><search /></el-icon></template>
          </el-input>
          <div class="toolbar-right">
            <span class="auto-refresh-label">自动刷新</span>
            <el-switch v-model="autoRefresh" :disabled="!isRunning" @change="handleAutoRefreshChange" />
            <el-button type="warning" size="small" @click="downloadLog(selectedRun)">
              <el-icon class="mr5"><download /></el-icon>下载日志
            </el-button>
          </div>
        </div>
        <el-alert v-if="logError"
                  title="日志加载失败，请重新打开详情重试"
                  type="error"
                  :closable="false"
        />
        <div ref="logContentRef"
             class="log-content"
             v-loading="logLoading"
             @scroll="handleLogScroll"
        >
          <div v-for="(log, index) in filteredLogs"
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
          <div v-if="!logLoading && filteredLogs.length === 0" class="log-empty">暂无日志记录</div>
        </div>
      </div>
      <template #footer><div class="log-footer"><el-button @click="detailVisible = false">关闭</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, onActivated, onDeactivated, reactive, ref, watch } from 'vue';

import { InfoFilled, View as IconView, Download, Search } from '@element-plus/icons-vue';

import DetailInfo from '@/components/DetailInfo/index.vue';
import StatusTag from '@/components/StatusTag/index.vue';
import { getIngestRunLogs, listIngestTaskRuns } from '@/api/ingest/task/task';

const RESULT_LABELS = { 1: '执行中', 2: '失败', 3: '失败', 4: '成功' };

const normalizeRunStatus = status => String(status) === '3' ? '2' : String(status ?? '');

const props = defineProps({ taskId: { type: [String, Number], default: undefined }, taskName: { type: String, default: '' }, active: { type: Boolean, default: true } });
const emit = defineEmits(['loaded']);
const { proxy } = getCurrentInstance();
const tableRef = ref();
const error = ref(false);
const allRuns = ref([]);
const listLoading = ref(false);
const statusFilter = ref('ALL');
const refreshedAt = ref('');
const filteredRuns = computed(() => allRuns.value.filter(run => statusFilter.value === 'ALL'
  || (statusFilter.value === 'RUNNING' && run.status === '1')
  || (statusFilter.value === 'SUCCESS' && run.status === '4')
  || (statusFilter.value === 'FAILED' && run.status === '2')));
const statusCounts = computed(() => allRuns.value.reduce((counts, run) => {
  if (run.status === '1') counts.running++;
  else if (run.status === '4') counts.success++;
  else if (run.status === '2') counts.failed++;
  return counts;
}, { running: 0, success: 0, failed: 0 }));
const runningRuns = computed(() => allRuns.value.filter(run => run.status === '1')
  .sort((left, right) => new Date(right.startTime).getTime() - new Date(left.startTime).getTime()));
const detailVisible = ref(false);
const selectedRun = ref(null);
const logs = ref([]);
const logError = ref(false);
const dialogKey = ref(0);
// 嵌套弹窗挂到外层页面，避免向自身渲染容器传送节点时破坏 Vue 的插入锚点。
const dialogTarget = ref();
const logLoading = ref(false);
const logContentRef = ref();
const searchKeyword = ref('');
const autoRefresh = ref(false);
const isRunning = computed(() => selectedRun.value?.status === '1');
let shouldAutoScroll = true;
const logTaskInfo = computed(() => ({
  ...selectedRun.value,
  name: props.taskName || '-',
  runDuration: formatDuration(selectedRun.value?.durationMillis),
}));
const detailItems = computed(() => [
  { label: '任务名称', key: 'name' },
  { label: '编号', key: 'id' },
  { label: '执行状态', key: 'status', slot: 'statusSlot' },
  { label: '开始时间', key: 'startTime', type: 'time', className: 'mt2 mb2' },
  { label: '已运行', key: 'runDuration', className: 'mt2 mb2' },
  ...(selectedRun.value?.errorMessage ? [{ label: '错误原因', key: 'errorMessage' }] : []),
]);
const filteredLogs = computed(() => {
  const keyword = searchKeyword.value.toLowerCase();
  return logs.value.filter(log => !keyword || formatLog(log).toLowerCase().includes(keyword));
});

/**
 * 按字面关键词拆分日志文本供高亮，保留纯文本渲染以避免执行日志中的 HTML。
 * @param {string} message 日志文本。
 * @returns {Array<Object>} 包含 text 与 match 的文本片段。
 */
function highlightParts(message) {
  const text = String(message || '');
  const keyword = searchKeyword.value.toLowerCase();
  if (!keyword) return [{ text, match: false }];
  const parts = [];
  const lowerText = text.toLowerCase();
  let start = 0;
  let index = lowerText.indexOf(keyword);
  while (index >= 0) {
    parts.push({ text: text.slice(start, index), match: false });
    parts.push({ text: text.slice(index, index + keyword.length), match: true });
    start = index + keyword.length;
    index = lowerText.indexOf(keyword, start);
  }
  parts.push({ text: text.slice(start), match: false });
  return parts;
}
/** 将日志级别映射为样式类，未知级别按普通信息展示。 */
function getLogLevelClass(level) {
  return { ERROR: 'error', WARN: 'warn', INFO: 'info', DEBUG: 'debug' }[level] || 'info';
}
/** 搜索关键词变化后将日志滚动到顶部，便于查看匹配结果。 */
function handleSearch() {
  if (logContentRef.value) logContentRef.value.scrollTop = 0;
}
/** 根据当前位置是否接近底部，决定追加日志后是否自动跟随。 */
function handleLogScroll() {
  const el = logContentRef.value;
  shouldAutoScroll = !el || el.scrollHeight - el.scrollTop - el.clientHeight <= 20;
}
/** 切换自动刷新时取消上一轮轮询，开启后立即读取日志。 */
function handleAutoRefreshChange() {
  pauseRefresh();
  if (autoRefresh.value) loadLogs();
}
let logTimer;
let logVersion = 0;
let afterSeq = 0;
let mounted = false;
let deactivated = false;

/**
 * 检查组件、页签、浏览器页面及任务状态，确定是否允许日志轮询。
 * @returns {boolean} 当前是否具备刷新条件。
 */
function canRefresh() {
  return mounted && !deactivated && props.active && !document.hidden && props.taskId != null;
}
/** 拼接时间、级别、阶段及正文，统一搜索与下载的日志文本。 */
function formatLog(record) {
  return `${formatTime(record.logTime)} [${record.logLevel}] [${record.stage || '-'}] ${record.message || ''}`;
}
/**
 * 每轮固定日志读取上界，增量追加日志并按运行状态安排下一次刷新。
 * @param {number} [version] 当前日志请求版本，变化后丢弃旧响应。
 */
async function loadLogs(version = logVersion) {
  if (!canRefresh() || !detailVisible.value || !selectedRun.value) return;
  let upperSeq;
  try {
    let hasMore = true;
    while (hasMore) {
      const { data } = await getIngestRunLogs(props.taskId, selectedRun.value.id, afterSeq);
      if (version !== logVersion) return;
      upperSeq ??= data.lastSeq;
      selectedRun.value = { ...data.run, status: normalizeRunStatus(data.run.status) };
      const page = data.logs.filter(record => record.eventSeq > afterSeq && record.eventSeq <= upperSeq);
      logs.value.push(...page);
      if (logs.value.length > 2000) logs.value.splice(0, logs.value.length - 2000);
      const previousSeq = afterSeq;
      // 下一页可能包含上界之后的新日志，留给下一轮读取，不能提前推进游标。
      afterSeq = Math.min(data.nextSeq, upperSeq);
      hasMore = data.hasMore && afterSeq < upperSeq && afterSeq > previousSeq;
      logError.value = false;
    }
    await nextTick();
    if (version !== logVersion) return;
    if (shouldAutoScroll && !searchKeyword.value && logContentRef.value) {
      logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
    }
    if (!isRunning.value) autoRefresh.value = false;
  } catch {
    if (version === logVersion) logError.value = true;
  } finally {
    if (version === logVersion) {
      logLoading.value = false;
      if (autoRefresh.value && isRunning.value && canRefresh() && detailVisible.value) {
        logTimer = setTimeout(() => loadLogs(version), logError.value ? 10000 : 3000);
      }
    }
  }
}
const tableStore = reactive({
  func: queryRuns,
  params: {},
  events: {},
  config: {
    initResquest: false,
    autoPagination: true,
    table: {
      stripe: true,
      rowKey: 'id',
      height: '525px',
    },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50, 100],
      pagerCount: 5,
      notAutoScroll: true,
    },
  },
  columns: [
    { label: '编号', prop: 'id', slot: 'id', width: 150, minWidth: 150, sortable: true },
    { label: '任务名称', prop: 'taskName', slot: 'taskName', minWidth: 200, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '执行类型', prop: 'triggerType', slot: 'triggerType', width: 100, minWidth: 100 },
    { label: '执行状态', prop: 'status', slot: 'status', width: 100, minWidth: 100 },
    { label: '开始时间', prop: 'startTime', slot: 'startTime', width: 160, minWidth: 160, sortable: true, showOverflowTooltip: { effect: 'light' } },
    { label: '运行时长', prop: 'durationMillis', slot: 'durationMillis', width: 100, minWidth: 100 },
    { label: '操作', prop: 'actions', slot: 'handle', width: 200, minWidth: 200, fixed: 'right' },
  ],
});
let requestVersion = 0;
/** 将人工或定时触发类型转换为展示名称，未知值保留原文。 */
function getTriggerLabel(value) {
  return { MANUAL: '人工执行', SCHEDULED: '定时执行' }[value] || value || '-';
}
/** 使用系统时间格式化方法，空值显示占位符。 */
function formatTime(value) {
  return value == null || value === '' ? '-' : proxy.parseTime(value) || '-';
}
/** 将毫秒时长转换为保留一位小数的秒数。 */
function formatDuration(value) {
  return value == null ? '-' : `${(value / 1000).toFixed(1)} 秒`;
}
/**
 * 切换运行实例时重置日志游标与搜索状态，再打开日志弹窗。
 * @param {Object} row 运行实例记录。
 */
function showDetail(row) {
  clearTimeout(logTimer);
  logVersion++;
  afterSeq = 0;
  dialogKey.value++;
  autoRefresh.value = false;
  searchKeyword.value = '';
  shouldAutoScroll = true;
  logLoading.value = true;
  logs.value = [];
  logError.value = false;
  selectedRun.value = row;
  autoRefresh.value = isRunning.value;
  detailVisible.value = true;
  loadLogs();
}

/** 打开最近的运行中实例日志。 */
function openLatestRunningRun() {
  const run = runningRuns.value[0];
  if (run) showDetail(run);
  else proxy.$modal.msgWarning('没有找到运行中的任务实例');
}
/**
 * 以下载开始时的日志上界读取快照，合并运行概况后导出文本。
 * @param {Object} row 运行实例记录。
 */
async function downloadLog(row) {
  const taskId = props.taskId;
  const records = [];
  let cursor = 0;
  let lastSeq;
  try {
    // 下载开始时取得日志上界，执行中的任务也能生成有限的下载快照。
    let remaining = true;
    while (remaining) {
      const { data } = await getIngestRunLogs(taskId, row.id, cursor);
      lastSeq ??= data.lastSeq;
      const page = data.logs.filter(record => record.eventSeq <= lastSeq);
      records.push(...page.map(formatLog));
      remaining = data.hasMore && data.nextSeq < lastSeq;
      cursor = data.nextSeq;
    }
  } catch { return; }
  const content = [
    `接入批次：${row.batchNo || '-'}`,
    `执行方式：${getTriggerLabel(row.triggerType)}`,
    `执行结果：${RESULT_LABELS[row.status] || row.status || '-'}`,
    `开始时间：${formatTime(row.startTime)}`,
    `结束时间：${formatTime(row.endTime)}`,
    `总数：${row.totalCount ?? '-'}`,
    `成功数：${row.successCount ?? '-'}`,
    `失败数：${row.failureCount ?? '-'}`,
    `跳过数：${row.skipCount ?? '-'}`,
    `失败原因：${row.errorMessage || '-'}`,
    '',
    ...records,
  ].join('\n');
  proxy.downloadContent(content, `${row.batchNo || row.id || '接入任务'}_日志.log`);
}
/** 按首次响应的总量逐页读取实例并去重，更新列表及父页运行概况。 */
async function handleRefresh() {
  if (props.taskId == null || listLoading.value) return;
  const version = ++requestVersion;
  const taskId = props.taskId;
  listLoading.value = true;
  error.value = false;
  try {
    const records = new Map();
    let total = 0;
    for (let pageNum = 1; ; pageNum++) {
      const { data } = await listIngestTaskRuns(taskId, { pageNum, pageSize: 100 });
      if (version !== requestVersion) return;
      if (pageNum === 1) total = data.total || 0;
      const rows = data.rows || [];
      rows.forEach(row => records.set(String(row.id), { ...row, status: normalizeRunStatus(row.status) }));
      if (!rows.length || pageNum * 100 >= total) break;
    }
    allRuns.value = [...records.values()];
    refreshedAt.value = formatTime(new Date());
    emit('loaded', { total: allRuns.value.length, latestRun: allRuns.value[0] || null });
    tableRef.value?.resetQuery();
  } catch {
    if (version === requestVersion) error.value = true;
  } finally {
    if (version === requestVersion) listLoading.value = false;
  }
}
/**
 * 按状态筛选并排序本地运行记录，由 QtTable 完成本地分页。
 * @param {Object} params 表格排序参数。
 * @returns {Promise<Object>} 通用表格数据对象。
 */
async function queryRuns(params) {
  const rows = [...filteredRuns.value];
  const prop = params.orderByColumn;
  if (['id', 'startTime'].includes(prop)) {
    const direction = ['asc', 'ascending'].includes(params.isAsc) ? 1 : -1;
    rows.sort((a, b) => direction * (prop === 'id' ? Number(a.id) - Number(b.id)
      : new Date(a.startTime).getTime() - new Date(b.startTime).getTime()));
  }
  // 复用 QtTable 的本地分页能力，不在业务组件重复切分数据。
  return { data: rows };
}
/** 运行状态筛选变化后重置分页并刷新本地表格。 */
function handleStatusChange() {
  tableRef.value?.resetQuery();
}
watch(() => props.taskId, () => {
  requestVersion++;
  clearTimeout(logTimer);
  logVersion++;
  error.value = false;
  detailVisible.value = false;
  selectedRun.value = null;
  if (props.taskId == null || props.taskId === '') return;
  allRuns.value = [];
  refreshedAt.value = '';
  statusFilter.value = 'ALL';
  listLoading.value = false;
  nextTick(() => { tableRef.value?.resetQuery(); handleRefresh(); });
}, { immediate: true });
watch(detailVisible, (visible) => {
  if (!visible) {
    autoRefresh.value = false;
    pauseRefresh();
  }
});
/** 取消轮询计时并使在途日志请求失效，避免关闭后继续更新。 */
function pauseRefresh() {
  clearTimeout(logTimer);
  logVersion++;
  logLoading.value = false;
}
/** 重新检查可见性和运行状态，满足条件时恢复自动日志读取。 */
function resumeRefresh() {
  pauseRefresh();
  if (autoRefresh.value && isRunning.value && detailVisible.value && canRefresh()) loadLogs();
}
watch(() => props.active, resumeRefresh);
onMounted(() => {
  mounted = true;
  dialogTarget.value = proxy.$el.closest('.app-container');
  document.addEventListener('visibilitychange', resumeRefresh);
});
onActivated(() => { deactivated = false; resumeRefresh(); });
onDeactivated(() => { deactivated = true; pauseRefresh(); });
onBeforeUnmount(() => {
  mounted = false;
  requestVersion++;
  pauseRefresh();
  document.removeEventListener('visibilitychange', resumeRefresh);
});
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
