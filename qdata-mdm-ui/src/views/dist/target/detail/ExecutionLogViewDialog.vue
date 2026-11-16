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

<template>
  <el-dialog v-if="appendTo" v-model="dialogVisible" title="查看日志" class="medium-dialog ingest-run-log-dialog target-execution-log-dialog"
             append-to-body :append-to="appendTo" draggable destroy-on-close>
    <div class="log-dialog-container">
      <DetailInfo :data="summary" :items="detailItems" mode="free">
        <template #statusSlot>
          <el-tag :type="statusTagType" size="small">{{ log.executionResult || '-' }}</el-tag>
        </template>
      </DetailInfo>

      <div class="log-toolbar">
        <el-input v-model="searchKeyword" class="search-input" placeholder="搜索日志内容" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <div class="toolbar-right">
          <span class="auto-refresh-label">自动刷新</span>
          <el-switch v-model="autoRefresh" />
          <el-button type="warning" size="small" @click="handleDownload">
            <el-icon class="mr5"><Download /></el-icon>下载日志
          </el-button>
        </div>
      </div>

      <div class="log-content">
        <div v-for="(line, index) in filteredLines" :key="index" class="log-row" :class="line.level.toLowerCase()">
          <span class="log-line-number">{{ index + 1 }}</span>
          <span class="log-time-text">【{{ line.time }}】</span>
          <span class="log-level-text">{{ line.level }}</span>
          <span class="log-content-text">{{ line.text }}</span>
        </div>
        <div v-if="!filteredLines.length" class="log-empty">{{ searchKeyword ? '没有匹配的日志' : '暂无日志记录' }}</div>
      </div>
    </div>
    <template #footer><div class="log-footer"><el-button @click="dialogVisible = false">关闭</el-button></div></template>
  </el-dialog>
</template>

<script setup name="DistTargetExecutionLogViewDialog">
import { computed, getCurrentInstance, onBeforeUnmount, ref, watch } from 'vue';
import { Download, Search } from '@element-plus/icons-vue';
import DetailInfo from '@/components/DetailInfo/index.vue';

const props = defineProps({
  visible: { type: Boolean, default: false },
  log: { type: Object, default: () => ({}) },
  appendTo: { type: [Object, String], default: 'body' },
});
const emit = defineEmits(['update:visible', 'refresh']);
const { proxy } = getCurrentInstance();
const searchKeyword = ref('');
const autoRefresh = ref(false);
let refreshTimer;

/** 关闭日志窗口时同步父页面状态。 */
const dialogVisible = computed({
  get: () => props.visible,
  set: value => emit('update:visible', value),
});

const statusTagType = computed(() => props.log.executionResult === '成功' ? 'success'
  : props.log.executionResult === '失败' ? 'danger' : 'info');

/** 只展示列表已有的摘要字段；没有运行时长时使用空值占位。 */
const summary = computed(() => ({
  name: props.log.objectName,
  id: props.log.rowNumber,
  startTime: props.log.executionTime,
  duration: '-',
}));
const detailItems = [
  { label: '任务名称', key: 'name' },
  { label: '编号', key: 'id' },
  { label: '执行状态', key: 'executionResult', slot: 'statusSlot' },
  { label: '开始时间', key: 'startTime', type: 'time', className: 'mt2 mb2' },
  { label: '已运行', key: 'duration', className: 'mt2 mb2' },
];

/** 目标系统执行列表目前为示例数据，按选中记录生成相符的示例日志。 */
const logLines = computed(() => {
  const log = props.log;
  if (!log?.objectName) return [];
  const baseTime = new Date(String(log.executionTime || '').replace(' ', 'T')).getTime();
  const time = offset => Number.isFinite(baseTime)
    ? proxy.parseTime(new Date(baseTime + offset * 1000), '{y}-{m}-{d} {h}:{i}:{s}')
    : log.executionTime || '-';
  const businessType = log.businessType || '任务';
  const action = log.executionContent || '执行任务';
  const success = log.executionResult === '成功';
  const failureReason = businessType === '服务调用' ? '目标服务返回异常，请检查接口响应'
    : businessType === '分发任务' ? '目标系统接收失败，请检查连接和请求参数'
      : '增量拉取失败，请检查订阅配置和来源数据';
  return [
    { time: time(-4), level: 'INFO', text: `【示例日志】开始${businessType}：${log.objectName}` },
    { time: time(-3), level: 'INFO', text: `操作人：${log.operatorName || '系统'}；执行内容：${action}` },
    { time: time(-2), level: 'INFO', text: `正在处理 ${log.objectName} 的执行请求` },
    { time: time(-1), level: success ? 'INFO' : 'ERROR', text: success ? `${action}完成，执行结果：成功` : `${action}失败：${failureReason}` },
    { time: time(0), level: success ? 'INFO' : 'ERROR', text: `${businessType}结束，执行结果：${log.executionResult || '未知'}` },
  ];
});
const filteredLines = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return keyword
    ? logLines.value.filter(line => `${line.time} ${line.level} ${line.text}`.toLowerCase().includes(keyword))
    : logLines.value;
});

/** 将当前弹窗中的日志内容导出为文本文件。 */
function handleDownload() {
  if (!logLines.value.length) return;
  const content = logLines.value.map(line => `【${line.time}】 ${line.level} ${line.text}`).join('\n');
  const url = URL.createObjectURL(new Blob([content], { type: 'text/plain;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = `目标系统日志_${props.log.rowNumber || '示例'}.log`;
  link.click();
  URL.revokeObjectURL(url);
}

/** 每次打开日志窗口时恢复搜索和自动刷新开关的初始状态。 */
watch(() => props.visible, value => {
  if (value) {
    searchKeyword.value = '';
    autoRefresh.value = false;
  } else {
    autoRefresh.value = false;
  }
});
watch(autoRefresh, value => {
  clearInterval(refreshTimer);
  if (value && props.visible) refreshTimer = setInterval(() => emit('refresh'), 3000);
});
onBeforeUnmount(() => clearInterval(refreshTimer));
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
.target-execution-log-dialog {
  max-width: calc(100vw - var(--qt-space-5) - var(--qt-space-5));
}
</style>
