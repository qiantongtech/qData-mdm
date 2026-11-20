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

<template>
  <div class="run-records task-instances run-instances">
    <div class="run-overview">
      <div class="run-overview__main">
        <el-icon class="run-overview__icon"><InfoFilled /></el-icon>
        <span v-if="statusFilter === 'ALL'">当前共有 运行中
          <span class="run-overview__count run-overview__running" @click="openLatestRunningLog">{{ summary.running }}</span>
          个，成功 <span class="run-overview__count run-overview__success">{{ summary.success }}</span>
          个，失败 <span class="run-overview__count run-overview__failed">{{ summary.failed }}</span> 个实例</span>
        <span v-else>当前共有 <span class="run-overview__count" :class="statusFilter.toLowerCase()">{{ filteredTotal }}</span>
          个实例{{ statusLabels[statusFilter] }}</span>
      </div>
      <div class="run-overview__meta">
        <span>最后刷新时间：{{ refreshedAt || '-' }}</span>
        <el-button link type="primary" icon="Refresh" :loading="summaryLoading" @click="refresh" aria-label="刷新任务实例" />
      </div>
    </div>

    <div class="run-toolbar">
      <el-tabs v-model="statusFilter" class="run-tabs" aria-label="筛选执行状态">
        <el-tab-pane label="全部" name="ALL" />
        <el-tab-pane label="运行中" name="RUNNING" />
        <el-tab-pane label="成功" name="SUCCESS" />
        <el-tab-pane label="失败" name="FAILED" />
      </el-tabs>
    </div>

    <el-alert v-if="loadError" title="任务实例加载失败，请点击刷新重试" type="error" :closable="false" show-icon />
    <div class="task-instances__table" :class="{ 'is-empty': tableRows.length === 0 }">
      <qt-table ref="tableRef" v-bind="tableStore">
        <template #taskName>{{ taskName || '-' }}</template>
        <template #triggerType="{ row }">{{ row.triggerType === 'MANUAL' ? '人工执行' : row.triggerType === 'SCHEDULED' ? '定时执行' : '-' }}</template>
        <template #runStatus="{ row }">
          <span class="run-status-action" @click="selectRun(row)">
            <RunStatusTag :status="row.runStatus" :success-count="Number(row.successCount || 0)"
                          :failure-count="Number(row.failureCount || 0)"
/>
          </span>
        </template>
        <template #duration="{ row }">{{ formatDuration(row) }}</template>
        <template #action="{ row }">
          <div class="run-actions">
            <el-button link type="primary" icon="View" @click="selectRun(row)">{{ row.runStatus === 'RUNNING' ? '实时日志' : '查看日志' }}</el-button>
            <el-button link type="warning" icon="Download" :loading="!!downloadLoading[row.id]"
                       @click="downloadLog(row)"
>下载日志</el-button>
          </div>
        </template>
      </qt-table>
      <div v-if="tableRows.length === 0" class="task-instances__empty-pagination" aria-hidden="true" />
    </div>

    <RunLogDialog :visible="logDialogVisible" :run="selectedRun || {}" :task-name="taskName"
                  :append-to="appendTo"
                  @update:visible="logDialogVisible = $event"
/>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { listINSTANCE } from '@/api/dist/task/INSTANCE';
import { InfoFilled } from '@element-plus/icons-vue';
import RunLogDialog from './RunLogDialog.vue';
import RunStatusTag from './RunStatusTag.vue';

const props = defineProps({
  taskId: { type: [String, Number], default: null },
  taskName: { type: String, default: '' },
  appendTo: { type: [Object, String], default: 'body' }
});
const { proxy } = getCurrentInstance();
const tableRef = ref();
const tableRows = ref([]);
const statusFilter = ref('ALL');
const statusLabels = { RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败' };
const summary = reactive({ running: 0, success: 0, failed: 0 });
const filteredTotal = computed(() => statusFilter.value === 'ALL' ? 0 : summary[statusFilter.value.toLowerCase()]);
const summaryLoading = ref(false);
const refreshedAt = ref('');
const selectedRun = ref(null);
const logDialogVisible = ref(false);
const loadError = ref(false);
const downloadLoading = reactive({});
let requestVersion = 0;
let summaryVersion = 0;

const tableStore = reactive({
  func: queryRuns,
  params: {},
  events: {},
  config: {
    initResquest: false,
    table: { stripe: true, rowKey: 'id', height: '525px' },
    pagination: { layout: 'total, sizes, prev, pager, next, jumper', pageSizes: [10, 20, 50, 100], notAutoScroll: true }
  },
  columns: [
    { label: '编号', prop: 'id', sortable: 'custom', width: 150 },
    { label: '任务名称', prop: 'taskName', slot: 'taskName', minWidth: 200, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '执行类型', prop: 'triggerType', slot: 'triggerType', width: 100 },
    { label: '执行状态', prop: 'runStatus', slot: 'runStatus', width: 100 },
    { label: '开始时间', prop: 'startTime', width: 160, sortable: 'custom', showOverflowTooltip: { effect: 'light' } },
    { label: '运行时长', prop: 'endTime', slot: 'duration', width: 100 },
    { label: '操作', prop: 'action', slot: 'action', width: 200, fixed: 'right' }
  ]
});

/** 格式化运行实例时间，空值或无效值显示占位符。 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-' : '-';
}

/**
 * 根据运行实例的开始和结束时间计算运行时长。
 * @param {Object} run 运行实例记录。
 * @returns {string} 格式化后的时长；时间缺失或无效时返回占位符。
 */
function formatDuration(run) {
  if (!run?.startTime || !run?.endTime) return '-';
  const start = new Date(String(run.startTime).replace(' ', 'T')).getTime();
  const end = new Date(String(run.endTime).replace(' ', 'T')).getTime();
  return Number.isFinite(start) && Number.isFinite(end) && end >= start ? `${((end - start) / 1000).toFixed(1)} 秒` : '-';
}

/**
 * 按当前任务和状态筛选条件查询运行实例列表。
 * @param {Object} params 表格分页及排序参数。
 * @returns {Promise<Object>} 通用表格数据对象。
 * @throws {Error} 查询失败时抛出异常，由表格组件处理请求状态。
 */
async function queryRuns(params) {
  const version = ++requestVersion;
  loadError.value = false;
  try {
    const queryParams = {
      ...params, taskId: props.taskId,
      runStatus: statusFilter.value === 'ALL' ? undefined : statusFilter.value
    };
    if (['id', 'startTime'].includes(params.orderByColumn)
      && ['ascending', 'descending'].includes(params.isAsc)) {
      queryParams.orderByColumn = params.orderByColumn;
      queryParams.isAsc = params.isAsc === 'ascending' ? 'asc' : 'desc';
    } else {
      delete queryParams.orderByColumn;
      delete queryParams.isAsc;
    }
    const response = await listINSTANCE(queryParams);
    if (version !== requestVersion) return { data: { rows: [], total: 0 } };
    tableRows.value = response.data?.rows || [];
    return response;
  } catch (error) {
    if (version === requestVersion) loadError.value = true;
    throw error;
  }
}

/**
 * 并行统计当前任务各运行状态的实例数，并更新刷新时间。
 * @returns {Promise<void>} 统计请求完成后结束。
 */
async function loadSummary() {
  if (props.taskId == null || props.taskId === '') return;
  const version = ++summaryVersion;
  summaryLoading.value = true;
  try {
    const counts = await Promise.all(['RUNNING', 'SUCCESS', 'FAILED'].map(runStatus =>
      listINSTANCE({ taskId: props.taskId, runStatus, pageNum: 1, pageSize: 1 })
    ));
    if (version !== summaryVersion) return;
    [summary.running, summary.success, summary.failed] = counts.map(response => Number(response.data?.total || 0));
    refreshedAt.value = formatTime(new Date());
  } catch {
    if (version === summaryVersion) loadError.value = true;
  } finally {
    if (version === summaryVersion) summaryLoading.value = false;
  }
}

/** 重新加载实例列表和运行状态统计。 */
function refresh() {
  tableRef.value?.getList();
  loadSummary();
}

/**
 * 选择运行实例并打开对应的日志窗口。
 * @param {Object} run 当前运行实例记录。
 */
function selectRun(run) {
  selectedRun.value = run;
  logDialogVisible.value = true;
}

/**
 * 查询并打开最新运行中实例的日志。
 * @returns {Promise<void>} 查询和打开日志后结束。
 */
async function openLatestRunningLog() {
  if (!summary.running || !props.taskId) return;
  try {
    const response = await listINSTANCE({ taskId: props.taskId, runStatus: 'RUNNING',
      pageNum: 1, pageSize: 1, orderByColumn: 'id', isAsc: 'desc' });
    const run = response.data?.rows?.[0];
    if (run) selectRun(run);
    else proxy.$modal.msgWarning('没有找到运行中的任务实例');
  } catch {
    proxy.$modal.msgError('获取运行中的任务实例失败');
  }
}

/**
 * 导出指定运行实例的执行日志。
 * @param {Object} run 当前运行实例记录。
 * @returns {Promise<void>} 日志导出请求完成后结束。
 */
async function downloadLog(run) {
  if (downloadLoading[run.id]) return;
  downloadLoading[run.id] = true;
  try {
    await proxy.download('dist/task/log/export', { instanceId: run.id }, `分发日志_${run.id}.xlsx`);
  } finally {
    downloadLoading[run.id] = false;
  }
}

watch(() => props.taskId, () => {
  requestVersion++;
  summaryVersion++;
  selectedRun.value = null;
  logDialogVisible.value = false;
  summary.running = 0;
  summary.success = 0;
  summary.failed = 0;
  refreshedAt.value = '';
  statusFilter.value = 'ALL';
  if (props.taskId != null && props.taskId !== '') nextTick(() => { tableRef.value?.resetQuery(); loadSummary(); });
}, { immediate: true });
watch(statusFilter, () => { nextTick(() => tableRef.value?.resetQuery()); });
onBeforeUnmount(() => { requestVersion++; summaryVersion++; });
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
.run-status-action {
  display: inline-flex;
  cursor: pointer;
}

.run-overview__running {
  cursor: pointer;
}
</style>
