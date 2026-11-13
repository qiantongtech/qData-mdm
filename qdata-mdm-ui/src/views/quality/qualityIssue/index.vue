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
  <div ref="app-container" class="app-container quality-page">
    <GuideTip tip-id="quality/issue.list"/>
    <el-container class="quality-layout">
      <qt-tree
          ref="modelTreeRef"
          :data="modelTreeData"
          :action-loading="modelTreeLoading"
          :use-builtin-cat-edit-dialog="false"
          :editable="false"
          node-key="treeKey"
          title="主数据模型"
          placeholder="搜索名称或编码"
          @node-click="handleModelTreeClick"
      />
      <el-main class="quality-content">
        <div class="status-tabs">
          <el-tabs v-model="activeTab" class="issue-tabs" @tab-change="handleTabChange">
            <el-tab-pane v-for="item in statusOptions" :key="item.value" :name="item.value" :label="`${item.label} ${statusCounts[item.value] ?? '-'}`" />
            <el-tab-pane name="all" :label="`全部 ${statusCounts.all ?? '-'}`" />
          </el-tabs>
        </div>
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #search><qt-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef" @reset="resetTreeSelection" /></template>
          <template #actions-data>
            <el-button type="primary" plain icon="Promotion" :disabled="!selected.length" @click="batchAssign">批量下发整改</el-button>
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #location="{ row }">
              <div v-if="modelInfo(row).projectName || modelInfo(row).layers?.length"
                   class="qt-table-info flex-column-gap overflow-hidden"
              >
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(modelInfo(row).projectIcon)" alt="" class="info-icon"/>
                  <span class="text-ellipsis" :title="modelInfo(row).projectName">{{
                      modelInfo(row).projectName || '-'
                    }}</span>
                </div>
                <div v-if="modelInfo(row).layers?.length" class="text-ellipsis" :title="modelDirectory(row)">
                  <dict-tag size="small" type="primary" :value="modelDirectory(row)"/>
                </div>
              </div>
              <span v-else class="muted">-</span>
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(modelInfo(row))" alt="" class="info-icon"/>
                  <span
                      class="info-title model-info-title text-ellipsis"
                      :role="canOpenModel(row) ? 'link' : undefined"
                      :tabindex="canOpenModel(row) ? 0 : undefined"
                      @click="openModelDetail(row)"
                      @keydown.enter="openModelDetail(row)"
                      @keydown.space.prevent="openModelDetail(row)"
                  >{{ modelTitle(row) }}</span>
                  <dict-tag v-if="modelInfo(row).type" :options="model_type" :value="modelInfo(row).type" size="small"
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
                  >{{ getModelVersionNo(row) }}
                  </dict-tag>
                </div>
                <div class="info-desc text-ellipsis">{{ modelInfo(row).description || '-' }}</div>
              </div>
            </template>
            <template #problemCount="{row}">
              <span class="problem-count">未解决 {{ unresolvedCount(row) }}项</span>
              <div class="muted">{{ row.problemFields }}</div></template>
            <template #status="{row}"><el-tag :type="statusType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag></template>
            <template #rectification="{ row }">
              <RectificationStatusTag :value="row.rectificationStatus === '未下发整改' ? '未下发' : row.rectificationStatus" />
            </template>
            <template #action="{row}">
              <el-button link type="primary" icon="View"  @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" icon="Promotion" :disabled="String(row.status)==='3'" @click="assign(row)">下发整改</el-button>
              <el-button link type="primary" icon="Close" :loading="closingIssueId === row.id"
                         :disabled="String(row.status)==='3' || closingIssueId !== null" @click="closeIssue(row)"
              >关闭</el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <el-dialog
      v-model="assignDialog.visible"
      :title="assignDialog.parentRows.length > 1 ? '批量下发整改' : '下发整改'"
      width="min(1100px, calc(100% - 24px))"
      top="5vh"
      :append-to="$refs['app-container']"
      class="scrollbar"
      draggable
      destroy-on-close
      :close-on-click-modal="false"
    >
      <GuideTipPanel
        type="hint"
        allow-wrap
        class="mb20"
        title="请指定整改责任人、整改期限及优先级，并确认待下发的问题项。系统将根据配置生成整改任务，由责任人处理相关质量问题。"
      />
      <el-form
        ref="assignFormRef"
        :model="assignForm"
        :rules="assignRules"
        label-position="right"
        label-width="120px"
        class="column-form mb20"
        :disabled="assignDialog.submitting"
        scroll-to-error
        @submit.prevent
      >
        <div class="h2-title row-full">基础信息</div>
        <qt-form-item label="整改责任人" prop="assigneeId" class="row-full">
          <el-select
            v-model="assignForm.assigneeId"
            filterable
            clearable
            placeholder="请选择整改责任人"
            :loading="userLoading"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.userId"
              :label="userLabel(user)"
              :value="user.userId"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item label="联系电话" prop="assigneePhone" class="row-full">
          <el-input :model-value="assignForm.assigneePhone" disabled placeholder="整改责任人联系电话" />
        </qt-form-item>
        <qt-form-item label="整改期限" prop="rectificationTime" class="row-full">
          <el-date-picker v-model="assignForm.rectificationTime" type="date"
                          value-format="YYYY-MM-DD" class="number-input"
                          placeholder="请选择整改完成的截止日期" :disabled-date="isPastDate"
          />
        </qt-form-item>
        <qt-form-item label="优先级" prop="priority" class="row-full">
          <el-select v-model="assignForm.priority" clearable placeholder="请选择整改优先级">
            <template #label="{ value }"><dict-tag :options="PRIORITY_OPTIONS" :value="value" /></template>
            <el-option v-for="item in PRIORITY_OPTIONS" :key="item.value" :label="item.label" :value="item.value">
              <dict-tag :options="PRIORITY_OPTIONS" :value="item.value" />
            </el-option>
          </el-select>
        </qt-form-item>
      </el-form>

      <div class="h2-title mb15">待下发问题项</div>
      <qt-table v-if="assignDialog.visible" ref="assignTableRef" v-bind="assignTableStore" />

      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="assignDialog.submitting" @click="closeAssignDialog">取消</el-button>
          <el-button
            type="primary"
            :loading="assignDialog.submitting"
            :disabled="!assignForm.assigneeId || !assignForm.rectificationTime || !assignForm.priority || !assignDialog.rows.length"
            @click="submitAssign"
          >
            下发
          </el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>
<script setup name="QualityIssue">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import {ref, reactive, computed, getCurrentInstance, nextTick, watch} from 'vue';
import QtTree from '@/components/QtTree/index.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import RectificationStatusTag from './components/RectificationStatusTag.vue';
import {useRouter} from 'vue-router';
import {getQualityIssueStatusCount, listQualityIssue, updateQualityIssue} from '@/api/quality/qualityIssue/qualityIssue';
import {addQualityIssueTask} from '@/api/quality/qualityIssueTask/qualityIssueTask';
import {getBase, getPublishedModelTree} from '@/api/model/base/base';
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import {useDict} from '@/utils/dict';

const {model_type, model_status} = useDict('model_type', 'model_status');
const userDirectoryStore = useUserDirectoryStore();

const {proxy} = getCurrentInstance();
const router = useRouter();
const canViewModels = computed(() => proxy.$auth.hasPermi('model:base:query'));
const PRIORITY_OPTIONS = [
  {value: 1, label: '高', elTagType: 'danger'},
  {value: 2, label: '中', elTagType: 'warning'},
  {value: 3, label: '低', elTagType: 'primary'}
];
const modelOptions = reactive([]);
const modelTreeRef = ref();
const modelTreeData = ref([]);
const modelTreeLoading = ref(false);
const selectedTreeNode = ref(null);
const modelMetadata = reactive({});
const modelMetadataRequests = new Map();

function normalizeModelTree(nodes = [], context = {}) {
  return nodes.map(node => {
    const projectName = node.nodeType === 'project' ? node.name : context.projectName || '';
    const layerPath = node.nodeType === 'layer'
        ? [...(context.layerPath || []), node.name]
        : (context.layerPath || []);
    return {
      ...node,
      projectName,
      layerPath,
      treeKey: `${node.nodeType}_${node.id}`,
      children: normalizeModelTree(node.children || [], {projectName, layerPath})
    };
  });
}

function collectModelNodes(nodes = []) {
  const models = new Map();

  function collect(items) {
    for (const node of items) {
      if (node.nodeType === 'model' && node.modelId != null) {
        models.set(String(node.modelId), node);
      }
      collect(node.children || []);
    }
  }

  collect(nodes);
  return [...models.values()];
}

async function loadModelTree() {
  modelTreeLoading.value = true;
  try {
    const {data} = await getPublishedModelTree();
    modelTreeData.value = normalizeModelTree(data || []);
    modelOptions.splice(0, modelOptions.length, ...collectModelNodes(modelTreeData.value).map(node => ({
      label: node.versionNo ? `${node.name}（${node.versionNo}）` : node.name,
      treeLabel: node.name,
      code: node.code,
      value: node.modelId,
      projectId: node.projectId,
      projectName: node.projectName,
      layerId: node.layerId,
      layerPath: node.layerPath || [],
      version: node.versionNo,
      treeKey: node.treeKey,
      fields: []
    })));
  } catch {
    modelTreeData.value = [];
  } finally {
    modelTreeLoading.value = false;
  }
}
const statusOptions = [{label: '待处理', value: '1'}, {label: '整改中', value: '2'}, {label: '已关闭', value: '3'}];
const statusCounts = reactive({all: undefined, '1': undefined, '2': undefined, '3': undefined});
const selected = ref([]);
const assignFormRef = ref(null);
const assignTableRef = ref(null);
const userOptions = ref([]);
const userLoading = ref(false);
const assignForm = reactive({assigneeId: null, assigneePhone: '', rectificationTime: null, priority: null});
const assignRules = {
  assigneeId: [{required: true, message: '请选择整改责任人', trigger: 'change'}],
  assigneePhone: [{required: true, message: '该责任人未维护联系电话，请选择其他人员或补全用户信息', trigger: 'change'}],
  rectificationTime: [{required: true, message: '请选择整改期限', trigger: 'change'}],
  priority: [{required: true, message: '请选择优先级', trigger: 'change'}]
};
const assignDialog = reactive({
  visible: false,
  rows: [],
  parentRows: [],
  submitting: false
});
const closingIssueId = ref(null);
watch(() => assignForm.assigneeId, id => {
  assignForm.assigneePhone = userOptions.value.find(user => String(user.userId) === String(id))?.phonenumber || '';
});
const assignTableStore = {
  config: {
    autoPagination: true,
    pagination: {pageSizes: [10, 20, 50, 100], size: 'small', pagerCount: 5,
      layout: 'total, sizes, prev, pager, next', notAutoScroll: true},
    table: {stripe: true, maxHeight: 340, rowKey: 'key'}
  },
  columns: [
    {label: '编号', prop: 'rowNo', width: 70, align: 'left'},
    {label: '主数据信息', prop: 'masterInfo', minWidth: 280, align: 'left', showOverflowTooltip: true},
    {label: '问题字段', prop: 'fieldName', minWidth: 160, align: 'left', showOverflowTooltip: true},
    {label: '质量规则', prop: 'ruleName', minWidth: 180, align: 'left', showOverflowTooltip: true},
    {label: '失败原因', prop: 'failReason', minWidth: 200, align: 'left', showOverflowTooltip: true}
  ],
  func: async () => ({data: assignDialog.rows.map((item, index) => ({
    key: item.key,
    rowNo: index + 1,
    masterInfo: `${item.issueRow.masterName || '未命名主数据'} (${item.issueRow.masterRecordId || '-'})`,
    fieldName: item.issueInfo?.modelColumnName || item.issueInfo?.fieldName || item.issueRow.problemFields || item.issueRow.fieldName || '-',
    ruleName: item.issueInfo?.qualityRuleName || item.issueInfo?.ruleName || item.issueRow.qualityRuleName || '-',
    failReason: item.issueInfo?.failReason || item.issueInfo?.reason || item.issueInfo?.errorMessage
      || item.issueRow.reason || item.issueRow.failReason || item.issueRow.errorMessage || '-'
  }))})
};

function isPastDate(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
}
const query = reactive({issueCode: undefined, modelId: undefined, modelIds: undefined, status: undefined});
const activeTab = computed({
  get: () => query.status || 'all',
  set: value => { query.status = value === 'all' ? undefined : value; },
});

function handleTabChange() {
  selected.value = [];
  tableRef.value?.resetQuery();
}

const tableRef = ref(null);
const tableStore = reactive({
  config: { table: { cellClassName: titleCellClass, stripe: true, rowKey: 'id', onSelectionChange: selection => { selected.value = selection; } } },
  columns: [
    { type: 'selection', width: 50 },
    {label: '编号', prop: 'id', width: 60, align: 'center', sortable: true, fixed: 'left'},
    { label: '质量任务名称', prop: 'qualityTaskName', className: 'title-column', link: tableTitleLink(openDetail), minWidth: 190, align: 'left', showOverflowTooltip: { effect: 'light' } },
    {
      label: '主数据模型',
      prop: 'modelId',
      slot: 'modelInfo',
      align: 'left',
      minWidth: 380,
      showOverflowTooltip: {effect: 'light'}
    },
    {label: '归属位置', slot: 'location', align: 'left', minWidth: 190},
    { label: '问题情况', prop: 'problemCount', slot: 'problemCount', minWidth: 180,align: "left" },
    { label: '状态', prop: 'status',  width: 100,dict: "quality_qualityissue_status"},
    { label: '整改情况', prop: 'rectificationStatus', slot: 'rectification', width: 130, showOverflowTooltip: true },
    { label: '最近检测时间', prop: 'lastFoundTime', width: 180, sortable: 'custom', sortableKey: 'last_found_time' },
    { label: '操作', slot: 'action', fixed: 'right', width: 280 },
  ],
  func: loadIssues,
  params: query,
  events: {
    formatData: rows => {
      rows.forEach(row => loadModelMetadata(row.modelId));
      return rows.map(row => {
        const infos = issueInfoList(row);
        return {
          ...row,
          status: String(row.status),
          problemCount: infos.length,
          problemFields: [...new Set(infos.map(item => item.modelColumnName).filter(Boolean))].join('、') || '-'
        };
      });
    }
  },
});
const searchStore = reactive({ items: [
  { label: '问题编号', prop: 'issueCode', component: { is: 'input', placeholder: '请输入问题编号' } },
  { label: '状态', prop: 'status', component: { is: 'select', placeholder: '全部', options: statusOptions } },
] });
function handleModelTreeClick(node) {
  selectedTreeNode.value = node;
  tableStore.params.modelId = node.nodeType === 'model' ? node.modelId : undefined;
  tableStore.params.modelIds = node.nodeType === 'model' ? undefined
      : collectModelNodes([node]).map(model => model.modelId);
  if (tableStore.params.modelIds?.length === 0) tableStore.params.modelIds = [-1];
  tableRef.value?.resetQuery();
}

function resetTreeSelection() {
  selectedTreeNode.value = null;
  tableStore.params.modelIds = undefined;
  modelTreeRef.value?.resetTree();
}

const statusLabel = v => ({1: '待处理', 2: '整改中', 3: '已关闭',4: '已完成'})[v] || '未知';
const statusType = v => ({1: 'danger', 2: 'warning', 3: 'info',4: 'success'})[v] || 'info';
const issueInfoList = row => Array.isArray(row?.qualityIssueInfoList) ? row.qualityIssueInfoList : [];
function modelLabel(id) {
  return modelMetadata[id]?.name || modelOptions.find(item => String(item.value) === String(id))?.treeLabel || id || '-';
}

function modelInfo(row) {
  const option = modelOptions.find(item => String(item.value) === String(row.modelId)) || {};
  return {
    ...option, ...(modelMetadata[row.modelId] || {}),
    name: modelMetadata[row.modelId]?.name || option.treeLabel || modelLabel(row.modelId)
  };
}

function modelTitle(row) {
  const {name, code} = modelInfo(row);
  return name ? (code ? `${name} (${code})` : name) : code || '-';
}

const unresolvedCount = row => {
  const infos = issueInfoList(row);
  if (infos.length) return infos.filter(item => String(item.status || '1') === '1').length;
  return Number(row?.problemCount || 0);
};
function canOpenModel(row) {
  return canViewModels.value && row.modelId != null;
}

function getModelVersionNo(row) {
  const model = modelInfo(row);
  return model?.releaseVersion?.versionNo || model?.version?.versionNo || row.modelVersion;
}

function getVersionTagType(row) {
  const status = modelInfo(row)?.version?.status;
  return model_status.value.find(item => String(item.value) === String(status))?.elTagType || 'info';
}

function openModelDetail(row, tab) {
  if (!canOpenModel(row)) return;
  router.push({path: '/model/base/detail', query: {id: row.modelId, ...(tab ? {tab} : {})}});
}

function modelDirectory(row) {
  let list = (modelInfo(row).layers || []);
  return list[list.length - 1]?.name;
}

async function loadModelMetadata(id) {
  if (id == null || !proxy.$auth.hasPermi('model:base:query')) return null;
  if (modelMetadata[id]) return modelMetadata[id];
  if (!modelMetadataRequests.has(String(id))) {
    const request = getBase(id).then(({data}) => {
      if (data) modelMetadata[id] = data;
      return data;
    }).catch(() => null).finally(() => modelMetadataRequests.delete(String(id)));
    modelMetadataRequests.set(String(id), request);
  }
  return modelMetadataRequests.get(String(id));
}

async function loadIssues(params) {
  selected.value = [];
  loadStatusCounts(params);
  const queryParams = {...params};
  if (!queryParams.orderByColumn || !queryParams.isAsc) {
    queryParams.orderByColumn = 'id';
    queryParams.isAsc = 'descending';
  }
  return listQualityIssue(queryParams);
}

let countRequestSequence = 0;
async function loadStatusCounts(params) {
  const sequence = ++countRequestSequence;
  const filters = {...params};
  ['status', 'pageNum', 'pageSize', 'orderByColumn', 'isAsc'].forEach(key => delete filters[key]);
  try {
    const response = await getQualityIssueStatusCount(filters);
    if (sequence !== countRequestSequence) return;
    Object.assign(statusCounts, response?.data || {});
  } catch {
    if (sequence !== countRequestSequence) return;
    Object.keys(statusCounts).forEach(key => { statusCounts[key] = undefined; });
  }
}

function getList() {
  tableRef.value?.getList();
}

function openDetail(row) {
  router.push({
    path: '/quality/qualityIssue/qualityIssueDetail',
    query: {id: row.id}
  });
}

const userLabel = user => user.nickName || user.userName || `用户${user.userId}`;

async function loadAssignUsers() {
  userLoading.value = true;
  try {
    const response = await userDirectoryStore.loadUserList({ status: '0' });
    const users = response?.rows || response?.data?.rows || [];
    userOptions.value = users;
  } catch {
    userOptions.value = [];
    proxy.$modal.msgWarning('用户列表加载失败，请稍后重试');
  } finally {
    userLoading.value = false;
  }
}

function openAssignDialog(rows) {
  const pendingRows = Array.from(new Map(
    rows.filter(row => String(row.status) !== '3').map(row => [String(row.id), row])
  ).values());
  if (!pendingRows.length) {
    proxy.$modal.msgWarning('当前没有可下发整改的问题');
    return;
  }
  assignDialog.parentRows = pendingRows;
  assignDialog.rows = pendingRows.flatMap(row => {
    const infos = issueInfoList(row);
    if (!infos.length) {
      return [{key: String(row.id), issueRow: row, issueInfo: null}];
    }
    return infos.map((issueInfo, index) => ({
      key: `${row.id}-${issueInfo.id || index}`,
      issueRow: row,
      issueInfo
    }));
  });
  assignDialog.submitting = false;
  Object.assign(assignForm, {assigneeId: null, assigneePhone: '', rectificationTime: null, priority: null});
  assignDialog.visible = true;
  loadAssignUsers(pendingRows);
  nextTick(() => {
    assignFormRef.value?.clearValidate();
    assignTableRef.value?.resetQuery();
  });
}

function assign(row) {
  openAssignDialog([row]);
}

async function closeIssue(row) {
  if (!row || String(row.status) === '3' || closingIssueId.value !== null) return;
  closingIssueId.value = row.id;
  try {
    await proxy.$modal.confirm('是否确认关闭该条质量问题？');
  } catch {
    closingIssueId.value = null;
    return;
  }
  try {
    await updateQualityIssue({
      id: row.id,
      status: '3',
      closeTime: new Date()
    });
    row.status = '3';
    row.rectificationStatus = '已关闭';
    selected.value = selected.value.filter(item => String(item.id) !== String(row.id));
    proxy.$modal.msgSuccess('问题已关闭');
    getList();
  } catch {
    proxy.$modal.msgError('关闭问题失败，请稍后重试');
  } finally {
    closingIssueId.value = null;
  }
}

function batchAssign() {
  openAssignDialog(selected.value);
}

function closeAssignDialog() {
  assignDialog.visible = false;
  assignDialog.rows = [];
  assignDialog.parentRows = [];
  assignDialog.submitting = false;
  Object.assign(assignForm, {assigneeId: null, assigneePhone: '', rectificationTime: null, priority: null});
}

function markRowAssigned(row) {
  row.rectificationStatus = '待处理';
  row.status = '2';
}

async function submitAssign() {
  if (assignDialog.submitting || !await assignFormRef.value?.validate().catch(() => false)) return;
  const selectedItems = assignDialog.rows;
  if (!selectedItems.length) {
    proxy.$modal.msgWarning('暂无待下发问题');
    return;
  }
  assignDialog.submitting = true;
  try {
    const assignedRows = new Map();
    for (const item of selectedItems) {
      const row = item.issueRow;
      await addQualityIssueTask({
        taskNo: `RT${Date.now()}${row.id}${item.key}`,
        taskName: `${row.masterName || modelInfo(row).name || '-'}问题整改`.slice(0, 128),
        ...(item.issueInfo?.id ? {issueInfoIds: [item.issueInfo.id]} : {issueId: row.id, qualityRuleId: item.issueInfo?.qualityRuleId || row.qualityRuleId}),
        modelId: row.modelId,
        modelVersion: row.modelVersion || getModelVersionNo(row),
        masterRecordId: row.masterRecordId,
        assigneeId: assignForm.assigneeId,
        priority: assignForm.priority,
        rectificationTime: `${assignForm.rectificationTime} 23:59:59`,
        status: '1',
        validFlag: true,
        taskDescription: '请完成质量问题整改'
      });
      assignedRows.set(String(row.id), row);
    }
    for (const row of assignedRows.values()) {
      await updateQualityIssue({
        id: row.id,
        status: '2',
        rectificationStatus: '待处理'
      });
      markRowAssigned(row);
    }
    proxy.$modal.msgSuccess(selectedItems.length > 1 ? '批量下发成功' : '下发成功');
    selected.value = [];
    closeAssignDialog();
    getList();
  } catch {
    proxy.$modal.msgError('下发整改失败，请稍后重试');
  } finally {
    assignDialog.submitting = false;
  }
}

loadModelTree();

</script>
<style scoped>
.quality-layout {
  display: flex;
  flex-direction: row;
  align-items: stretch;
  min-height: calc(100vh - 190px);
}

.quality-content {
  flex: 1;
  min-width: 0;
  padding: 0;
}

.quality-page :deep(.left-pane),
.quality-page :deep(.resize-bar) {
  height: auto !important;
  align-self: stretch;
}

.status-tabs {
  display: none;
  background: #fff;
  padding: 0 16px;
}

.issue-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.problem-count {
  color: rgb(236, 84, 77) !important;
}

.model-info {
  min-width: 0;
}

.model-info-title {
  display: block;
}

.model-info-title[role='link'] {
  cursor: pointer;
  color: var(--el-color-primary);
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}

.muted {
  color: #8b95a7;
  font-size: 12px
}

</style>
