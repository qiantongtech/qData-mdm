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

<template>
  <el-dialog
    v-model="visible"
    title="质量任务运行记录"
    width="1080px"
    top="5vh"
    draggable
    destroy-on-close
    class="run-history-dialog"
    @closed="handleClosed"
  >

<!--    <el-descriptions :column="2" border class="task-summary">
      <el-descriptions-item label="任务名称">{{ task.taskName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="主数据模型">{{ modelName || task.modelId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="当前检测范围">{{ String(task.dataScopeType) === '2' ? '条件筛选' : '全部数据' }}</el-descriptions-item>
      <el-descriptions-item label="当前状态">{{ String(task.status) === '1' ? '启用' : '停用' }}</el-descriptions-item>
    </el-descriptions>-->
    <div class="history-hint">
      <div class="history-stat">
        <el-icon color="#2A7BFD" size="16px"><InfoFilled /></el-icon>
        <span v-if="activeTab === 'all'">
          当前共有运行中
          <span class="count-number running">{{ displayCount(stats.running) }}</span>
          个，成功
          <span class="count-number success">{{ displayCount(stats.success) }}</span>
          个，失败
          <span class="count-number failed">{{ displayCount(stats.failed) }}</span>
          个实例
        </span>
        <span v-else-if="activeTab === 'running'">
          当前共有 <span class="count-number running">{{ displayCount(stats.running) }}</span> 个实例运行中
        </span>
        <span v-else-if="activeTab === 'success'">
          当前共有 <span class="count-number success">{{ displayCount(stats.success) }}</span> 个实例成功
        </span>
        <span v-else>
          当前共有 <span class="count-number failed">{{ displayCount(stats.failed) }}</span> 个实例失败
        </span>
      </div>
      <div class="refresh-info">
        <span>最后刷新时间：{{ lastRefreshTime }}</span>
        <el-button
          :icon="Refresh"
          link
          type="primary"
          :loading="statsLoading"
          aria-label="刷新运行记录"
          @click="handleRefresh"
        />
      </div>
    </div>
    <el-tabs v-model="activeTab" class="history-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="运行中" name="running" />
      <el-tab-pane label="成功" name="success" />
      <el-tab-pane label="失败" name="failed" />
    </el-tabs>
    <el-alert v-if="error" title="运行记录加载失败" type="error" :closable="false" class="history-error">
      <el-button link type="primary" @click="handleRefresh">重试</el-button>
    </el-alert>
    <qt-table v-bind="tableStore" ref="tableRef" class="history-table">
      <template #triggerType="{ row }">{{ triggerLabel(row.triggerType) }}</template>
      <template #status="{ row }">
        <el-tag
          :type="statusType(row.status)"
          :class="['status-tag', `status-tag--${String(row.status)}`]"
        >
          <span class="status-content">
            <el-icon
              v-if="statusIconComponent(row.status)"
              class="status-icon"
              :class="{ 'is-loading': String(row.status) === '1' }"
            >
              <component :is="statusIconComponent(row.status)" />
            </el-icon>
            <img
              v-else-if="statusIconUrl(row.status)"
              :src="statusIconUrl(row.status)"
              class="status-icon"
              alt=""
            />
            <span>{{ statusLabel(row.status) }}</span>
          </span>
        </el-tag>
      </template>
      <template #action="{ row }">
        <el-button link type="primary" icon="View"   @click="showResult(row)">查看结果</el-button>
      </template>
      <template #empty><span class="muted">暂无运行记录</span></template>
    </qt-table>
    <template #footer><el-button @click="visible = false">关闭</el-button></template>
  </el-dialog>
  <el-dialog
    v-model="resultVisible"
    title="运行结果"
    width="800px"
    class="result-dialog"
    draggable
    destroy-on-close
  >
    <GuideTipPanel v-if="resultPassed || !resultIssues.length" type="hint" allow-wrap class="mb15">
      <template #title-append>
        <span>{{ resultPassed ? (Number(result.totalCount) === 0 ? '本次检测范围内没有数据。' : '本次检测范围内全部数据通过质量规则。') : result.errorMessage || `本次检测发现 ${result.failCount ?? 0} 条问题数据。` }}</span>
      </template>
    </GuideTipPanel>
    <el-form :model="result" label-width="110px" class="column-form result-summary">
      <qt-form-item label="执行时间" class="row-full">
        <div class="form-readonly">{{ resultTime }}</div>
      </qt-form-item>
      <qt-form-item label="执行方式" class="row-full">
        <el-tag type="primary">{{ triggerLabel(result.triggerType) }}</el-tag>
      </qt-form-item>
      <qt-form-item label="检测数据量" class="row-full">
        <div class="form-readonly">{{ result.totalCount ?? '-' }}</div>
      </qt-form-item>
      <qt-form-item label="问题数量" class="row-full">
        <div class="form-readonly">{{ result.failCount ?? '-' }}</div>
      </qt-form-item>
    </el-form>

    <div v-if="!resultPassed && resultIssues.length" class="result-issue-list">
      <div v-for="issue in resultIssues" :key="issue.key" class="result-issue-card">
        <div class="result-issue-heading">
          <el-tag class="result-issue-status" type="warning" effect="plain">未通过</el-tag>
          <strong>{{ issue.enterpriseName }}</strong>
          <span class="result-issue-code">{{ issue.enterpriseCode }}</span>
        </div>
        <div class="result-issue-meta">
          <span class="result-issue-rule">{{ issue.ruleName }}</span>
          <span><em>字段：</em>{{ issue.field }}</span>
          <span><em>原因：</em>{{ issue.reason }}</span>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="dialog-footer"><el-button @click="resultVisible = false">关 闭</el-button></div>
    </template>
  </el-dialog>
</template>
<script setup>
import { computed, nextTick, reactive, ref } from 'vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import {
  InfoFilled,
  Loading,
  Refresh,
  WarningFilled
} from '@element-plus/icons-vue';
import statusIconSuccess from '@/assets/common/icons/status-tag/success.svg';
import statusIconFailed from '@/assets/common/icons/status-tag/failed.svg';
import { parseTime } from '@/utils/anivia';
import { listQualitytaskinstance } from '@/api/quality/qualityTaskInstance/qualitytaskinstance';
const visible = ref(false);
const resultVisible = ref(false);
const task = ref({});
const modelName = ref('');
const error = ref(false);
const result = ref({});
const tableRef = ref(null);
const activeTab = ref('all');
const statsLoading = ref(false);
const stats = reactive({ running: 0, success: 0, failed: 0 });
const lastRefreshTime = ref('-');
let requestId = 0;
let statsRequestId = 0;
const formatTime = value => value ? parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-';
const resultTime = computed(() => {
  const value = result.value.startTime || result.value.endTime;
  return value ? parseTime(value, '{y}-{m}-{d} {h}:{i}') : '-';
});
const resultIssues = computed(() => normalizeIssues(result.value));
const resultPassed = computed(() => {
  const status = String(result.value.status ?? '');
  const failCount = result.value.failCount;
  const hasFailCount = failCount !== undefined && failCount !== null && failCount !== '';
  if (status === '1' || status === '2' || status === '3') return false;
  return status === '4' ? (!hasFailCount || Number(failCount) === 0) : hasFailCount && Number(failCount) === 0;
});
const statusLabel = value => ({ 1: '运行中', 2: '失败', 3: '部分成功', 4: '成功' })[String(value)] || '未知状态';
const statusType = value => ({ 1: 'primary', 2: 'danger', 3: 'warning', 4: 'success' })[String(value)] || 'info';
const statusIconComponent = value => ({
  1: Loading,
  3: WarningFilled
})[String(value)];
const statusIconUrl = value => ({
  2: statusIconFailed,
  4: statusIconSuccess
})[String(value)];
const triggerLabel = value => ({ 1: '手动', 2: '定时', manual: '手动', scheduled: '定时' })[value] || '-';
const displayCount = value => statsLoading.value ? '-' : value;

const tableStore = reactive({
  config: {
    initResquest: false,
    table: {
      stripe: true,
      rowKey: 'id',
      height: '440px'
    },
    pagination: {
      pageSizes: [10, 20, 50]
    }
  },
  columns: [
    { label: '执行时间', prop: 'startTime', date: '{y}-{m}-{d} {h}:{i}', minWidth: 165 },
    { label: '执行方式', prop: 'triggerType', slot: 'triggerType', width: 100,type:'number' },
    { label: '检测数量', prop: 'totalCount', minWidth: 105 },
    { label: '通过数量', prop: 'passCount', minWidth: 105 },
    { label: '问题数量', prop: 'failCount', minWidth: 105 },
    { label: '执行结果', prop: 'status', slot: 'status', minWidth: 125 },
    { label: '操作', slot: 'action', width: 110, fixed: 'right' }
  ],
  func: fetchRecords,
  params: {
    qualityTaskId: undefined,
    status: undefined
  }
});

function firstValue(source, keys) {
  if (!source || typeof source !== 'object') return '';
  for (const key of keys) {
    const value = source[key];
    if (value !== undefined && value !== null && value !== '') return value;
  }
  return '';
}

function displayValue(value) {
  if (Array.isArray(value)) return value.map(displayValue).filter(Boolean).join('、') || '-';
  if (value && typeof value === 'object') {
    return String(value.name ?? value.label ?? value.value ?? value.code ?? '-');
  }
  return value === undefined || value === null || value === '' ? '-' : String(value);
}

function toIssueArray(value) {
  if (Array.isArray(value)) return value;
  if (typeof value !== 'string') {
    const list = value?.rows || value?.list || value?.items || value?.data?.rows || value?.data?.list
      || (Array.isArray(value?.data) ? value.data : []);
    return Array.isArray(list) ? list : [];
  }
  try {
    const parsed = JSON.parse(value);
    const list = Array.isArray(parsed)
      ? parsed
      : parsed?.rows || parsed?.list || parsed?.items || parsed?.data?.rows || parsed?.data?.list
        || (Array.isArray(parsed?.data) ? parsed.data : []);
    return Array.isArray(list) ? list : [];
  } catch {
    return [];
  }
}

function normalizeIssues(instance) {
  const raw = instance?.issues
    ?? instance?.issueDetails
    ?? instance?.problemDetails
    ?? instance?.issueList
    ?? instance?.qualityIssues
    ?? instance?.qualityIssueList
    ?? instance?.qualityIssueInfoList
    ?? instance?.problemList
    ?? instance?.problems
    ?? instance?.details
    ?? instance?.result?.issues
    ?? instance?.result?.details;
  return toIssueArray(raw).map((item, index) => {
    const record = item?.record || item?.data || item?.entity || item?.masterRecord || item?.masterData || {};
    const source = {...record, ...item};
    const enterprise = source.enterprise || source.company || source.organization || {};
    const rule = source.rule || source.qualityRule || {};
    const field = source.fieldInfo || source.modelField || {};
    return {
      key: source.id ?? source.issueId ?? index,
      enterpriseName: displayValue(firstValue(source, ['enterpriseName', 'companyName', 'corpName', 'orgName', 'masterName', 'masterDataName', 'entityName', 'dataName', 'businessName']) || firstValue(enterprise, ['name', 'enterpriseName', 'companyName'])),
      enterpriseCode: displayValue(firstValue(source, ['enterpriseCode', 'companyCode', 'corpCode', 'orgCode', 'masterCode', 'masterRecordId', 'recordCode', 'dataCode', 'businessCode']) || firstValue(enterprise, ['code', 'enterpriseCode', 'companyCode'])),
      ruleName: displayValue(firstValue(source, ['ruleName', 'qualityRuleName', 'ruleTitle']) || firstValue(rule, ['name', 'ruleName', 'title'])),
      field: displayValue(firstValue(source, ['field', 'fieldName', 'modelFieldName', 'modelColumnName', 'columnName', 'fieldCode']) || firstValue(field, ['name', 'fieldName', 'code'])),
      reason: displayValue(firstValue(source, ['reason', 'failReason', 'message', 'errorMessage', 'description', 'remark']))
    };
  });
}

async function fetchRecords(params) {
  const current = requestId;
  if (!task.value.id) return {data: {rows: [], total: 0}};
  error.value = false;
  try {
    const response = await listQualitytaskinstance({
      ...params,
      qualityTaskId: task.value.id,
      orderByColumn: 'id',
      isAsc: 'descending'
    });
    if (current !== requestId) return {data: {rows: [], total: 0}};
    if (Array.isArray(response?.data)) {
      return {...response, data: {rows: response.data, total: response.data.length}};
    }
    return response;
  } catch {
    if (current === requestId) error.value = true;
    return {data: {rows: [], total: 0}};
  }
}

function resetStats() {
  stats.running = 0;
  stats.success = 0;
  stats.failed = 0;
}

async function fetchStatistics() {
  const taskId = task.value.id;
  const current = ++statsRequestId;
  if (!taskId) {
    resetStats();
    statsLoading.value = false;
    return;
  }
  statsLoading.value = true;
  try {
    const statuses = [
      ['running', '1'],
      ['success', '4'],
      ['failed', '2']
    ];
    const responses = await Promise.all(statuses.map(([, status]) => listQualitytaskinstance({
      qualityTaskId: taskId,
      status,
      pageNum: 1,
      pageSize: 1
    })));
    if (current !== statsRequestId) return;
    statuses.forEach(([key], index) => {
      const data = responses[index]?.data;
      stats[key] = Number(data?.total ?? (Array.isArray(data) ? data.length : 0));
    });
    lastRefreshTime.value = formatTime(new Date());
  } catch {
    if (current === statsRequestId) lastRefreshTime.value = formatTime(new Date());
  } finally {
    if (current === statsRequestId) statsLoading.value = false;
  }
}

function handleRefresh() {
  if (!task.value.id) return;
  error.value = false;
  fetchStatistics();
  tableRef.value?.getList();
}

function handleTabChange(tabName) {
  tableStore.params.status = ({running: '1', success: '4', failed: '2'})[tabName];
  tableRef.value?.resetQuery();
}

function open(row, name) {
  requestId++;
  statsRequestId++;
  task.value = { ...row };
  modelName.value = name;
  activeTab.value = 'all';
  tableStore.params.qualityTaskId = row?.id;
  tableStore.params.status = undefined;
  resetStats();
  error.value = false;
  lastRefreshTime.value = '-';
  visible.value = true;
  nextTick(() => {
    tableRef.value?.resetQuery();
    fetchStatistics();
  });
}

function handleClosed() {
  requestId++;
  statsRequestId++;
  tableStore.params.qualityTaskId = undefined;
  tableStore.params.status = undefined;
  activeTab.value = 'all';
  resetStats();
  statsLoading.value = false;
  error.value = false;
}
function showResult(row) {
  result.value = { ...row };
  resultVisible.value = true;
}
defineExpose({ open, showResult });
</script>

<style scoped>

.run-history-dialog {
  max-width: calc(100vw - 40px);
}

.run-history-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px 15px;
}

.task-summary { margin-bottom: 18px; }

.history-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 28px;
  color: var(--el-text-color-regular);
  border-radius: 2px;
  background-color: #ecf5ff;
  padding: 8px 19px;
  box-shadow: -1px 1px 2px #e5f1f8;
  font-size: 14px;
  width: 100%;
  max-width: 100%;
}

.history-stat,
.refresh-info,
.status-content {
  display: inline-flex;
  align-items: center;
}

.history-stat {
  gap: 6px;
  min-width: 0;
  line-height: 20px;
}

.count-number {
  margin: 0 2px;
  font-weight: 600;
}

.count-number.running { color: var(--el-color-primary); }
.count-number.success { color: var(--el-color-success); }
.count-number.failed { color: var(--el-color-danger); }

.refresh-info {
  flex: 0 0 auto;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.history-tabs {
  margin-top: 8px;
}

.history-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.history-error {
  margin-bottom: 10px;
}

.history-table :deep(.el-table__body-wrapper) {
  min-height: 160px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  min-width: 68px;
  height: 24px;
  padding: 3px 8px;
  justify-content: center;
  vertical-align: middle;
}

.status-tag--4 {
  background: rgba(0, 158, 33, 0.1) !important;
  border: 0 !important;
  color: #009e21 !important;
}

.status-tag--2 {
  background: rgba(236, 84, 77, 0.1) !important;
  border: 0 !important;
  color: #ec544d !important;
}

.status-content {
  gap: 4px;
}

.status-icon {
  width: 14px;
  height: 14px;
}

.status-icon.is-loading {
  animation: status-rotating 2s linear infinite;
}

.muted {
  color: var(--el-text-color-secondary);
}

@keyframes status-rotating {
  0% { transform: rotateZ(0deg); }
  100% { transform: rotateZ(360deg); }
}

.result-dialog {
  max-width: calc(100vw - 40px);
}

.result-dialog :deep(.el-dialog__body) {
  max-height: 65vh;
  overflow-y: auto;
}

.result-issue-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.result-issue-card {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 1px;
  background: var(--el-bg-color);
}

.result-issue-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  line-height: 22px;
}

.result-issue-status {
  flex: 0 0 auto;
  height: 24px;
  padding: 2px 8px;
  border-color: #f5dab0 !important;
  background: #fdf6ec !important;
  color: #b88230 !important;
  line-height: 18px;
}

.result-issue-heading strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-issue-code {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-issue-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
}

.result-issue-meta em {
  font-style: normal;
  color: var(--el-text-color-secondary);
}

@media (max-width: 760px) {
  .run-history-dialog :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
  }

  .history-hint {
    align-items: flex-start;
    flex-direction: column;
  }

  .result-issue-meta {
    display: grid;
    gap: 2px;
  }
}
</style>
