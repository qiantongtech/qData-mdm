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
  整改任务列表：按模型筛选、查看和导出任务。
-->
<template>
  <div class="app-container rectification-page">
    <guide-tip tip-id="quality/rectification.list" />
    <el-container>
      <qt-tree
        ref="treeRef"
        :key="treeVersion"
        node-key="treeKey"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        :editable="false"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelClick"
      />
      <el-main class="main-content task-main">
        <div v-if="SHOW_TASK_FILTERS" class="task-filters mb15">
          <el-tabs v-model="status" @tab-change="queryTable">
            <qt-tab-pane
              v-for="item in STATUS_TABS"
              :key="item.value"
              :name="item.value"
              :label="`${item.label} ${counts[item.value] ?? '-'}`"
            />
          </el-tabs>
        </div>
        <qt-wrap :columns="tableStore.columns" :table-ref="tableRef">
          <template #search>
            <qt-search-bar
              :items="searchItems"
              :params="tableStore.params"
              :table-ref="null"
              @query="queryTable"
              @reset="resetSearch"
            />
          </template>
          <template #actions-data>
            <el-button
              v-hasPermi="['quality:qualityissuetask:add']"
              type="primary"
              plain
              icon="Plus"
              @click="handleCreate"
            >新增</el-button>
            <el-button
              v-hasPermi="['quality:qualityissuetask:remove']"
              type="danger"
              plain
              icon="Delete"
              :disabled="!selectedTaskIds.length"
              :loading="deleting"
              @click="handleBatchDelete()"
            >批量删除</el-button>
            <el-button
              v-hasPermi="['quality:qualityissuetask:export']"
              type="warning"
              plain
              icon="Download"
              :loading="exporting"
              @click="handleExport"
            >导出</el-button>
          </template>
          <qt-table
            ref="tableRef"
            v-bind="tableStore"
          >
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(modelMetadata[row.modelId])" alt="" class="info-icon" />
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="canOpenModel(row) ? 'link' : undefined"
                    :tabindex="canOpenModel(row) ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ modelTitle(row) }}</span>
                  <dict-tag
                    v-if="modelMetadata[row.modelId]?.type"
                    :options="model_type"
                    :value="modelMetadata[row.modelId].type"
                    size="small"
                    type="primary"
                  />
                  <dict-tag
                    v-if="getModelVersionNo(row)"
                    class="ml4"
                    :class="{ pointer: canOpenModel(row) }"
                    :type="getVersionTagType(row)"
                    size="small"
                    :role="canOpenModel(row) ? 'link' : undefined"
                    :tabindex="canOpenModel(row) ? 0 : undefined"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  >{{ getModelVersionNo(row) }}</dict-tag>
                </div>
                <div class="info-desc text-ellipsis">{{ modelMetadata[row.modelId]?.description || '-' }}</div>
              </div>
            </template>
            <template #modelLocation="{ row }">
              <div v-if="modelLocation(row.modelId).projectName || modelLocation(row.modelId).layers?.length" class="flex-column flex-column-gap">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(modelLocation(row.modelId).projectIcon)" alt="" />
                  <span class="text-ellipsis" :title="modelLocation(row.modelId).projectName || ''">{{ modelLocation(row.modelId).projectName || '-' }}</span>
                </div>
                <div v-if="modelLocation(row.modelId).layers?.length" class="text-ellipsis flex-center">
                  <dict-tag size="small" type="primary" :value="modelLocation(row.modelId).layers[modelLocation(row.modelId).layers.length - 1].name" />
                </div>
              </div>
              <span v-else class="text-muted">-</span>
            </template>
            <template #assignee="{ row }">
              <div class="flex-column">
                <span>{{ row.assigneeName || '-' }}</span>
                <span class="desc-text">{{ row.assigneePhone || '-' }}</span>
              </div>
            </template>
            <template #progress="{ row }">
              <div v-if="getRectificationProgress(row).label !== '-'" class="rectification-progress flex-center">
                <div class="rectification-progress__bar" :class="{ 'rectification-progress__bar--zero': getRectificationProgress(row).percentage === 0 }">
                  <el-progress
                    :percentage="getRectificationProgress(row).percentage"
                    :stroke-width="14"
                    text-inside
                    :color="getRectificationProgress(row).percentage === 100 ? 'var(--qt-status-success)' : 'var(--qt-element-fixed-primary-light-3)'"
                  />
                </div>
                <span>{{ getRectificationProgress(row).label }}</span>
              </div>
              <span v-else>-</span>
            </template>
            <template #status="{ row }">
              <dict-tag :options="TASK_STATUS_OPTIONS" :value="row.status" />
            </template>
            <template #priority="{ row }">
              <dict-tag :options="PRIORITY_OPTIONS" :value="row.priority" />
            </template>
            <template #rectificationTime="{ row }">
              <span :class="{ 'overdue-text': isOverdue(row) }">
                {{ row.rectificationTime || '-' }}
                <template v-if="isOverdue(row)"><br />(已逾期)</template>
              </span>
            </template>
            <template #action="{ row }">
              <el-button
                v-hasPermi="['quality:qualityissuetask:query']"
                link
                type="primary"
                icon="View"
                @click="openDetail(row)"
              >详情</el-button>
              <el-button
                v-hasPermi="['quality:qualityissuetask:remove']"
                link
                type="danger"
                icon="Delete"
                :disabled="deleting"
                @click="handleBatchDelete(row)"
              >删除</el-button>
              <el-button
                v-if="['1', '5'].includes(String(row.status))"
                v-hasPermi="['quality:qualityissuetask:edit']"
                link
                type="primary"
                :disabled="String(row.status) === '5' || pendingActionId === row.id"
                :loading="pendingActionId === row.id"
                @click="handleApprovalChange(row, true)"
              >
                <template #icon>
                  <svg-icon icon-class="submit-approval" />
                </template>
                提交审批
              </el-button>
              <el-button
                v-if="['2', '3'].includes(String(row.status))"
                v-hasPermi="['quality:qualityissuetask:edit']"
                link
                type="primary"
                :disabled="String(row.status) === '3' || pendingActionId === row.id"
                :loading="pendingActionId === row.id"
                @click="handleApprovalChange(row, false)"
              >
                <template #icon>
                  <svg-icon icon-class="withdraw-approval" />
                </template>
                撤回审批
              </el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="QualityIssueTask">
import { computed, getCurrentInstance, nextTick, onActivated, onBeforeUnmount, reactive, ref } from 'vue';

import { useRouter } from 'vue-router';

import QtTree from '@/components/QtTree/index.vue';
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import {useDict} from '@/utils/dict';
const {model_type, model_status} = useDict('model_type', 'model_status');
import { usePageRefresh } from '@/composables/usePageRefresh';
import { getBase } from '@/api/model/base/base';
import { usePublishedModelTree, withModelTreeKeys } from '@/composables/usePublishedModelTree';
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import { delQualityIssueTask, listQualityIssueTask, getQualityIssueTaskStatusCount, submitQualityIssueTaskApproval, withdrawQualityIssueTaskApproval } from '@/api/quality/qualityIssueTask/qualityIssueTask';

const userDirectoryStore = useUserDirectoryStore();
const TASK_STATUS_OPTIONS = [
  { value: '1', label: '待审批', elTagType: 'warning' },
  { value: '2', label: '审批中', elTagType: 'primary' },
  { value: '3', label: '已完成', elTagType: 'success' },
  { value: '4', label: '已取消', elTagType: 'info' },
  { value: '5', label: '处理中', elTagType: 'primary' }
];
const PRIORITY_OPTIONS = [
  { value: 1, label: '高', elTagType: 'danger' },
  { value: 2, label: '中', elTagType: 'warning' },
  { value: 3, label: '低', elTagType: 'primary' }
];
const OVERDUE_OPTIONS = [
  { value: 1, label: '逾期', elTagType: 'danger' },
  { value: 0, label: '未逾期', elTagType: 'success' }
];

const { proxy } = getCurrentInstance();
const router = useRouter();
const {data: modelTreeData, loading: modelTreeLoading} = usePublishedModelTree(withModelTreeKeys);
const canViewModel = computed(() => proxy.$auth.hasPermi('model:base:query'));
const modelMetadata = reactive({});
const modelMetadataRequests = new Set();
const modelProjects = computed(() => {
  const projects = new Map();
  for (const project of modelTreeData.value) {
    const location = { projectName: project.name, projectIcon: project.icon };
    for (const layer of project.children || []) {
      for (const model of layer.children || []) {
        projects.set(String(model.modelId), location);
      }
    }
  }
  return projects;
});
const TABLE_CONFIG = {
  sort: true,
  table: {
    rowKey: 'id',
    onSelectionChange: handleSelectionChange,
    defaultSort: {prop: 'createTime', order: 'descending'}
  }
};
// 状态筛选关闭时显示全部任务。
const SHOW_TASK_FILTERS = false;
const STATUS_TABS = [{ label: '全部', value: 'all' }, ...TASK_STATUS_OPTIONS];
const status = ref('all');
const tableParams = reactive({
  taskName: '', taskNo: '', modelId: undefined, assigneeId: undefined, status: undefined,
  priority: undefined, overdue: undefined, createTimeRange: []
});
const users = ref([]);
const selectedModel = ref();
const modelId = computed(() => selectedModel.value?.modelId);
const treeRef = ref();
const treeVersion = ref(0);
const tableRef = ref();
const counts = ref({});
const exporting = ref(false);
const selectedTaskIds = ref([]);
const deleting = ref(false);
const pendingActionId = ref();
const searchItems = computed(() => [
  { label: '任务名称', prop: 'taskName', component: { is: 'input', placeholder: '请输入整改任务名称' } },
  { label: '任务编码', prop: 'taskNo', component: { is: 'input', placeholder: '请输入任务编码' } },
  { label: '负责人', prop: 'assigneeId', component: { is: 'select', filterable: true, options: users.value } },
  { label: '任务状态', prop: 'status', component: { is: 'select', placeholder: '请选择任务状态', options: TASK_STATUS_OPTIONS } },
  { label: '优先级', prop: 'priority', component: { is: 'select', placeholder: '请选择优先级', options: PRIORITY_OPTIONS } },
  { label: '是否逾期', prop: 'overdue', component: { is: 'select', clearable: true, placeholder: '请选择是否逾期', options: OVERDUE_OPTIONS } },
  { label: '下发时间', prop: 'createTimeRange', component: {
    is: 'date-picker', type: 'daterange', startPlaceholder: '开始日期', endPlaceholder: '结束日期'
  } }
]);
const columns = reactive([
  { type: 'selection', width: 48, minWidth: 48 },
  { label: '编号', prop: 'id', width: 60, minWidth: 60, sortable: true, align: 'left' },
  {
    label: '整改任务信息',
    info: {
      title: 'taskName',
      desc: 'taskNo',
      click: proxy.$auth.hasPermi('quality:qualityissuetask:query')
              ? openDetail
              : null,
    },
    minWidth: 260,
    align: 'left'
  },
  { label: '模型信息', slot: 'modelInfo', width: 380, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { label: '归属位置', slot: 'modelLocation', width: 200, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { label: '整改进度', slot: 'progress', minWidth: 220, align: 'left' },
  { label: '优先级', slot: 'priority', width: 90, minWidth: 90, align: 'left' },
  { label: '状态', slot: 'status', width: 110, minWidth: 110, align: 'center' },
  { label: '涉及主数据数', prop: 'masterDataCount', width: 120, minWidth: 120, sortable: true },
  { label: '问题数据数量', prop: 'issueDataCount', width: 120, minWidth: 120, sortable: true },
  {label: '负责人', prop: 'assigneeName', slot: 'assignee', width: 160, minWidth: 160, align: 'left'},
  {
    label: '整改期限',
    prop: 'rectificationTime',
    slot: 'rectificationTime',
    width: 180,
    minWidth: 180,
    sortable: true,
    sortableKey: 'rectification_time',
    align: 'left'
  },
  {
    label: '下发时间',
    prop: 'createTime',
    date: true,
    width: 180,
    minWidth: 180,
    sortable: true,
    sortableKey: 'create_time',
    align: 'left'
  },
  { label: '操作', slot: 'action', fixed: 'right', width: 250, minWidth: 250 }
]);
const tableStore = reactive({
  config: TABLE_CONFIG,
  columns,
  func: requestRows,
  params: tableParams,
  events: {
    formatData: rows => {
      rows.forEach(row => loadModelMetadata(row.modelId));
      return rows;
    }
  }
});
let requestSequence = 0;
let latestRequest;

/**
 * 获取模型名称和编码。
 * @param {Object} row 整改任务
 * @returns {string} 模型信息标题
 */
function modelTitle(row) {
  const model = modelMetadata[row.modelId];
  const name = model?.name || row.modelName;
  const code = model?.code || row.modelCode;
  return name ? (code ? `${name} (${code})` : name) : code || '-';
}

function canOpenModel(row) {
  return canViewModel.value && row.modelId != null;
}

function getModelVersionNo(row) {
  const model = modelMetadata[row.modelId];
  return model?.releaseVersion?.versionNo || model?.version?.versionNo || row.modelVersion;
}

function getVersionTagType(row) {
  const status = modelMetadata[row.modelId]?.version?.status;
  return model_status.value.find(item => String(item.value) === String(status))?.elTagType || 'info';
}

/**
 * 打开模型详情。
 * @param {Object} row 整改任务
 * @param {string} [tab] 模型详情页签
 */
function openModelDetail(row, tab) {
  if (!canOpenModel(row)) return;
  router.push({
    path: '/model/base/detail',
    query: { id: row.modelId, ...(tab ? { tab } : {}) }
  });
}

/**
 * 没有模型详情时，从左侧树取主数据域。
 * @param {number|string} modelId 模型 ID
 * @returns {Object} 归属信息，找不到时返回空对象
 */
function modelLocation(modelId) {
  return modelMetadata[modelId] || modelProjects.value.get(String(modelId)) || {};
}

/**
 * 查询模型详情。
 * @param {number|string} modelId 模型 ID
 */
function loadModelMetadata(modelId) {
  if (modelId == null || !canViewModel.value) return;
  const key = String(modelId);
  if (modelMetadata[key] || modelMetadataRequests.has(key)) return;

  modelMetadataRequests.add(key);
  getBase(modelId).then(({ data }) => {
    if (data) modelMetadata[key] = data;
  }).catch(() => {
    // 列表继续显示已有数据。
  }).finally(() => modelMetadataRequests.delete(key));
}

/**
 * 查询整改任务，并处理过期请求。
 * @param {Object} params 查询、分页及排序参数
 * @returns {Promise<Object>} 任务分页数据
 */
function requestRows(params) {
  const sequence = ++requestSequence;
  const query = buildQueryParams(params);
  if (SHOW_TASK_FILTERS) {
    counts.value = {};
    // 统计独立加载，隐藏统计栏时不发请求，也不阻塞列表显示。
    loadStatusCounts(query, sequence);
  }
  const request = listQualityIssueTask(query).then(response => {
    if (sequence !== requestSequence && latestRequest?.sequence > sequence) {
      return latestRequest.promise;
    }
    return response;
  }, error => {
    if (sequence !== requestSequence && latestRequest?.sequence > sequence) {
      return latestRequest.promise;
    }
    throw error;
  });
  // 旧请求复用较新的请求结果，避免连续翻页时覆盖当前页。
  latestRequest = { sequence, promise: request };
  return request;
}

/**
 * 加载状态数量，仅更新当前查询。
 * @param {Object} params 查询条件
 * @param {number} sequence 请求序号
 * @returns {Promise<void>}
 */
async function loadStatusCounts(params, sequence) {
  try {
    const response = await getQualityIssueTaskStatusCount(params);
    if (sequence !== requestSequence) {
      return;
    }
    const data = response.data;
    counts.value = {
      all: data.allCount,
      '1': data.pendingCount,
      '2': data.processingCount,
      '3': data.completedCount,
      '4': data.cancelledCount,
      '5': data.rectifyingCount
    };
  } catch {
    // 统计失败时清空数量，列表继续显示。
    if (sequence === requestSequence) {
      counts.value = {};
    }
  }
}

/**
 * 将查询条件转换为接口参数，不修改表单值。
 * @param {Object} params 查询、分页及排序参数
 * @returns {Object} 文本去空格、日期展开后的参数
 */
function buildQueryParams(params) {
  const { createTimeRange, ...query } = params;
  return {
    ...query,
    status: query.status || undefined,
    taskName: query.taskName?.trim() || '',
    taskNo: query.taskNo?.trim() || '',
    beginTime: createTimeRange?.[0] ? `${createTimeRange[0]} 00:00:00` : undefined,
    endTime: createTimeRange?.[1] ? `${createTimeRange[1]} 23:59:59` : undefined
  };
}

/** 按当前条件查询第一页。 */
function queryTable() {
  requestSequence++;
  return nextTick(() => tableRef.value?.resetQuery());
}

/** 按当前条件刷新当前页。 */
function refreshTable() {
  requestSequence++;
  return nextTick(() => tableRef.value?.getList());
}

/** 清空筛选条件和树选中项，再查询第一页。 */
function resetSearch() {
  tableParams.taskName = '';
  tableParams.taskNo = '';
  tableParams.modelId = undefined;
  tableParams.assigneeId = undefined;
  tableParams.status = undefined;
  tableParams.priority = undefined;
  tableParams.overdue = undefined;
  tableParams.createTimeRange = [];
  status.value = 'all';
  selectedModel.value = undefined;
  treeRef.value?.resetTree();
  queryTable();
}

/**
 * 切换模型筛选，点击非模型节点时保留原筛选。
 * @param {Object} node 点击的树节点
 */
function handleModelClick(node) {
  if (node.nodeType !== 'model' || !node.modelId) {
    // 目录节点仅展开，保持当前模型筛选及选中样式一致。
    treeRef.value?.setCurrentKey(selectedModel.value?.treeKey ?? null, false);
    return;
  }
  if (String(node.modelId) === String(modelId.value)) {
    return;
  }
  selectedModel.value = node;
  tableParams.modelId = node.modelId;
  queryTable();
}

/**
 * 打开整改任务详情。
 * @param {Object} row 整改任务
 */
function openDetail(row) {
  router.push({ path: '/quality/qualityIssueTask/detail', query: { id: row.id } });
}

/**
 * 判断任务是否超过整改期限。
 * @param {Object} row 任务
 * @returns {boolean} 未设置期限时返回 false
 */
function isOverdue(row) {
  const deadline = row?.rectificationTime;
  if (!deadline) return false;
  const time = new Date(String(deadline).replace(/-/g, '/')).getTime();
  return !Number.isNaN(time) && time < Date.now();
}

/**
 * 提交或撤回审批，执行期间阻止重复操作。
 * @param {Object} row 任务
 * @param {boolean} submit true：提交审批；false：撤回审批
 * @returns {Promise<void>}
 */
async function handleApprovalChange(row, submit) {
  if (pendingActionId.value != null || !proxy.$auth.hasPermi('quality:qualityissuetask:edit')) return;
  if (String(row.status) !== (submit ? '1' : '2')) return;
  pendingActionId.value = row.id;
  try {
    if (submit) {
      await submitQualityIssueTaskApproval(row.id);
    } else {
      await withdrawQualityIssueTaskApproval(row.id);
    }
    proxy.$modal.msgSuccess('执行成功');
    await refreshTable();
  } catch {
    // 请求封装已提示失败原因，保留列表当前状态。
  } finally {
    pendingActionId.value = undefined;
  }
}

/**
 * 保存选中任务的 ID。
 * @param {Object[]} rows 选中的整改任务
 */
function handleSelectionChange(rows) {
  selectedTaskIds.value = rows.map(row => row.id);
}

/** 确认后删除当前行或所选任务，并刷新当前页。 */
async function handleBatchDelete(row) {
  const taskIds = row?.id != null ? [row.id] : selectedTaskIds.value;
  if (deleting.value || !taskIds.length) return;
  const ids = taskIds.join(',');
  deleting.value = true;
  try {
    await proxy.$modal.confirm(row?.id != null
      ? `是否删除整改任务“${row.taskName || row.id}”？`
      : `是否删除选中的 ${taskIds.length} 条整改任务？`);
    await delQualityIssueTask(ids);
    tableRef.value?.elTableRef?.clearSelection();
    proxy.$modal.msgSuccess('删除成功');
    await refreshTable();
  } catch {
    // 取消确认或请求失败时保留勾选，请求封装负责提示失败原因。
  } finally {
    deleting.value = false;
  }
}

/** 新建功能尚未开放。 */
function handleCreate() {
  proxy.$modal.msgWarning('功能正在开发中');
}

/**
 * 按勾选任务或当前筛选条件导出。
 * @returns {Promise<void>}
 */
async function handleExport() {
  if (exporting.value) return;
  const selectedRows = tableRef.value?.elTableRef?.getSelectionRows() || [];
  const params = buildQueryParams(tableParams);
  if (selectedRows.length) {
    params.ids = selectedRows.map(row => row.id).join(',');
  }
  exporting.value = true;
  try {
    await proxy.download('quality/qualityIssueTask/export', params, `整改任务_${Date.now()}.xlsx`);
  } finally {
    exporting.value = false;
  }
}

/** 分页加载负责人选项，无用户查询权限时跳过。 */
async function loadUsers() {
  if (!proxy.$auth.hasPermi('system:user:list')) {
    return;
  }
  try {
    const { rows: userRows } = await userDirectoryStore.loadUserList({ status: '0' });
    users.value = userRows.map(item => ({ value: item.userId, label: item.nickName || item.userName }));
  } catch {
    // 加载失败时清空负责人选项。
    users.value = [];
  }
}
// 返回缓存页时，全部任务视图恢复树的默认展开层级。
onActivated(() => {
  if (!selectedModel.value) treeVersion.value++;
});

usePageRefresh('quality-rectification', refreshTable);
// 页面卸载后，使状态统计的旧响应失效。
onBeforeUnmount(() => {
  requestSequence++;
});
loadUsers();

/**
 * 按已整改问题项数和问题项总数计算进度，统计无效时显示短横线。
 * @param {Object} task 包含已整改数和总数的任务
 * @returns {{completed: number, total: number, percentage: number, label: string}} 整改进度
 */
function getRectificationProgress(task) {
  const total = Number(task?.issueDataCount);
  const completed = Number(task?.completedIssueCount);
  if (task?.issueDataCount == null || task?.completedIssueCount == null
    || !Number.isFinite(total) || !Number.isFinite(completed) || total <= 0) {
    return { completed: 0, total: 0, percentage: 0, label: '-' };
  }
  const percentage = Math.round((completed / total) * 100);
  return { completed, total, percentage, label: `${completed}/${total}` };
}
</script>

<style scoped lang="scss">
.model-info {
  min-width: 0;

  .model-info-title {
    display: block;

    &[role='link'] {
      cursor: pointer;
      color: var(--el-color-primary);
    }
  }

  :deep(.qt-table-info__main > .dict-tag-container) {
    flex-shrink: 0;
  }
}

.overdue-text {
  color: var(--el-color-danger);
}
.rectification-progress {
  gap: var(--qt-space-2);
  white-space: nowrap;
}
.rectification-progress__bar {
  --rectification-bar-width: 96px;
  position: relative;
  flex: 0 0 var(--rectification-bar-width);
  width: var(--rectification-bar-width);
}
.rectification-progress__bar .el-progress {
  width: 100%;
}
.rectification-progress :deep(.el-progress-bar__inner) {
  background-image: none;
  text-align: center;
}
.rectification-progress :deep(.el-progress-bar__innerText) {
  position: absolute;
  top: 50%;
  left: calc(var(--rectification-bar-width) / 2);
  margin: 0;
  transform: translate(-50%, -50%);
  font-size: 11px;
}
.rectification-progress__bar--zero :deep(.el-progress-bar__innerText) {
  color: var(--el-text-color-secondary);
}
.task-main {
  min-width: 0;
}
.task-filters {
  display: flex;
  flex-direction: column;
  background: var(--qt-surface-background);
}
.task-filters :deep(.qt-toggle-button-wrapper) {
  margin-bottom: 0;
}
.task-filters :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 var(--qt-space-4);
  border-top: 1px solid var(--el-border-color-light);
}
</style>
