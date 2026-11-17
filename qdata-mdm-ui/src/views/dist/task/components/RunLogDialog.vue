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
  <el-dialog v-if="appendTo" v-model="visibleDialog" :title="title" class="medium-dialog ingest-run-log-dialog"
             append-to-body :append-to="appendTo" draggable destroy-on-close>
    <div class="log-dialog-container">
      <DetailInfo :data="summary" :items="detailItems" mode="free">
        <template #run-status>
          <RunStatusTag :status="currentRun.runStatus" :success-count="Number(currentRun.successCount || 0)"
                        :failure-count="Number(currentRun.failureCount || 0)" />
        </template>
      </DetailInfo>

      <div class="log-toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索日志内容" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="toolbar-right">
          <span class="auto-refresh-label">自动刷新</span>
          <el-switch v-model="autoRefresh" />
          <el-button type="warning" size="small" :loading="downloading" @click="downloadLog">
            <el-icon class="mr5"><Download /></el-icon>下载日志
          </el-button>
        </div>
      </div>

      <el-alert v-if="loadError" title="日志加载失败，请关闭后重试" type="error" :closable="false" show-icon />
      <div class="log-content" v-loading="loading">
        <div v-for="(line, index) in filteredLines" :key="line.key" class="log-row" :class="line.level.toLowerCase()">
          <span class="log-line-number">{{ index + 1 }}</span>
          <span class="log-time-text">【{{ line.time }}】</span>
          <span class="log-level-text">{{ line.level }}</span>
          <span class="log-content-text">{{ line.text }}</span>
        </div>
        <div v-if="!loading && !filteredLines.length" class="log-empty">
          {{ keyword ? '没有匹配的日志' : '暂无日志记录' }}
        </div>
        <div v-if="!loading && hasMore" class="run-log-more">
          <el-button link type="primary" @click="loadMore">加载更多日志（已加载 {{ records.length }} / {{ total }} 条）</el-button>
        </div>
      </div>
    </div>
    <template #footer><div class="log-footer"><el-button @click="visibleDialog = false">关闭</el-button></div></template>
  </el-dialog>
</template>

<script setup>
import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from 'vue';
import { Download, Search } from '@element-plus/icons-vue';
import DetailInfo from '@/components/DetailInfo/index.vue';
import { getINSTANCE } from '@/api/dist/task/INSTANCE';
import { listLOG } from '@/api/dist/task/LOG';
import RunStatusTag from './RunStatusTag.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  run: { type: Object, default: () => ({}) },
  taskName: { type: String, default: '' },
  title: { type: String, default: '查看日志' },
  appendTo: { type: [Object, String], default: 'body' }
});
const emit = defineEmits(['update:visible']);
const { proxy } = getCurrentInstance();
const visibleDialog = computed({ get: () => props.visible, set: value => emit('update:visible', value) });
const currentRun = ref({});
const records = ref([]);
const total = ref(0);
const page = ref(1);
const loading = ref(false);
const loadError = ref(false);
const downloading = ref(false);
const keyword = ref('');
const autoRefresh = ref(false);
let refreshTimer;
let requestVersion = 0;
const PAGE_SIZE = 200;

const summary = computed(() => ({
  name: props.taskName || '-',
  id: currentRun.value.id,
  runStatus: currentRun.value.runStatus,
  startTime: currentRun.value.startTime,
  duration: duration(currentRun.value)
}));
const detailItems = [
  { label: '任务名称', key: 'name' },
  { label: '编号', key: 'id' },
  { label: '执行状态', slot: 'run-status' },
  { label: '开始时间', key: 'startTime', type: 'time', className: 'mt2 mb2' },
  { label: '已运行', key: 'duration', className: 'mt2 mb2' }
];
const hasMore = computed(() => records.value.length < total.value);

// 当前接口保存的是逐条分发记录；转换为 Pro2 日志窗口使用的时间、级别、内容行。
const lines = computed(() => {
  const output = [];
  if (currentRun.value.errorMessage) output.push({
    key: 'summary-error', time: formatTime(currentRun.value.endTime || currentRun.value.startTime),
    level: 'ERROR', text: currentRun.value.errorMessage
  });
  for (const record of records.value) {
    const time = formatTime(record.distributeTime);
    const target = [record.masterCode || record.masterRecordId, record.masterName].filter(Boolean).join(' / ') || '-';
    const success = record.distributeStatus === 'SUCCESS';
    output.push({
      key: `${record.id}-result`, time, level: success ? 'INFO' : 'ERROR',
      text: `主数据 ${target} 分发${success ? '成功' : '失败'}${record.responseCode ? `，响应码 ${record.responseCode}` : ''}${record.failReason || record.responseMessage ? `，${record.failReason || record.responseMessage}` : ''}`
    });
    if (record.requestData) output.push({
      key: `${record.id}-request`, time, level: 'DEBUG', text: `请求体：${record.requestData}`
    });
  }
  return output;
});
const filteredLines = computed(() => {
  const search = keyword.value.trim().toLowerCase();
  return search ? lines.value.filter(line => `${line.time} ${line.level} ${line.text}`.toLowerCase().includes(search)) : lines.value;
});

/** 格式化日志时间，空值或无效值显示占位符。 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-' : '-';
}
/**
 * 根据运行实例的开始和结束时间计算已运行时长。
 * @param {Object} run 当前运行实例。
 * @returns {string} 格式化后的时长；开始时间缺失或无效时返回占位符。
 */
function duration(run) {
  if (!run?.startTime) return '-';
  const start = new Date(String(run.startTime).replace(' ', 'T')).getTime();
  const end = run.endTime ? new Date(String(run.endTime).replace(' ', 'T')).getTime() : Date.now();
  return Number.isFinite(start) && Number.isFinite(end) && end >= start ? `${Math.floor((end - start) / 1000)}s` : '-';
}

/**
 * 分页读取当前运行实例的日志，并按需重置已加载内容。
 * @param {boolean} [reset=false] 是否从第一页重新读取。
 * @returns {Promise<void>} 日志读取完成后结束。
 */
async function loadLogs(reset = false) {
  if (!currentRun.value.id) return;
  const version = ++requestVersion;
  if (reset) { page.value = 1; records.value = []; total.value = 0; }
  loading.value = true;
  loadError.value = false;
  try {
    const response = await listLOG({ instanceId: currentRun.value.id, pageNum: page.value,
      pageSize: PAGE_SIZE, orderByColumn: 'id', isAsc: 'asc' });
    if (version !== requestVersion) return;
    records.value = reset ? (response.data?.rows || []) : [...records.value, ...(response.data?.rows || [])];
    total.value = Number(response.data?.total || 0);
  } catch {
    if (version === requestVersion) {
      loadError.value = true;
      if (!reset) page.value = Math.max(1, page.value - 1);
    }
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}

/** 有更多日志且当前没有请求时，加载下一页内容。 */
function loadMore() {
  if (loading.value || !hasMore.value) return;
  page.value += 1;
  loadLogs();
}

/**
 * 刷新运行实例状态和日志内容；刷新失败时关闭自动刷新。
 * @returns {Promise<void>} 刷新请求完成后结束。
 */
async function refresh() {
  if (!currentRun.value.id || loading.value) return;
  try {
    const response = await getINSTANCE(currentRun.value.id);
    if (response.data && visibleDialog.value) currentRun.value = response.data;
    await loadLogs(true);
  } catch {
    autoRefresh.value = false;
  }
}

/**
 * 导出当前运行实例的执行日志。
 * @returns {Promise<void>} 日志导出请求完成后结束。
 */
async function downloadLog() {
  if (!currentRun.value.id || downloading.value) return;
  downloading.value = true;
  try {
    await proxy.download('dist/task/log/export', { instanceId: currentRun.value.id }, `分发日志_${currentRun.value.id}.xlsx`);
  } finally {
    downloading.value = false;
  }
}

watch(() => props.visible, value => {
  requestVersion++;
  if (value) {
    currentRun.value = { ...props.run };
    keyword.value = '';
    loadError.value = false;
    autoRefresh.value = currentRun.value.runStatus === 'RUNNING';
    loadLogs(true);
  } else {
    autoRefresh.value = false;
    records.value = [];
  }
});
watch(autoRefresh, value => {
  clearInterval(refreshTimer);
  if (value && visibleDialog.value) refreshTimer = setInterval(refresh, 3000);
});
onBeforeUnmount(() => { requestVersion++; clearInterval(refreshTimer); });
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
.run-log-more {
  padding: var(--qt-space-3);
  text-align: center;
}
</style>
