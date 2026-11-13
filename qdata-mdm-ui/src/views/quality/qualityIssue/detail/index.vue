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
  <div ref="app-container" v-loading="loading" class="app-container">
    <detail-info
      v-if="issue.id"
      :data="issue"
      :header="{
        className: 'clearfixs',
        idKey: 'id',
        nameKey: 'qualityTaskName',
        statusKey: 'status',
        statusOptions: issueStatusOptions,
      }"
      :items="detailItems"
      mode="free"
    />

    <el-empty v-if="!loading && !issue.id" description="问题数据不存在或已删除" />

    <div v-if="issue.id" class="pagecont-bottom">
      <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="handleTabChange">
        <qt-tab-pane label="问题明细" name="details">
          <IssueDetailTable
            ref="issueTableRef"
            :query-rows="fetchIssueRows"
            :issue-status="issue.status"
            @assign="assignIssue"
          />
        </qt-tab-pane>

        <qt-tab-pane label="整改记录" name="rectification">
          <RectificationRecordTable ref="taskTableRef" :query-rows="fetchTaskRows" />
        </qt-tab-pane>
      </el-tabs>
    </div>

    <el-dialog
      v-model="assignDialog.visible"
      title="下发整改"
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
          <el-select v-model="assignForm.priority" clearable placeholder="请选择整改优先级" popper-class="quality-issue-priority-popper">
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
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="QualityIssueDetail">
import {computed, getCurrentInstance, nextTick, reactive, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import DetailInfo from '@/components/DetailInfo/index.vue';
import IssueDetailTable from './components/issueDetailTable.vue';
import RectificationRecordTable from './components/rectificationRecordTable.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import {getQualityIssue, updateQualityIssue} from '@/api/quality/qualityIssue/qualityIssue';
import {getBase} from '@/api/model/base/base';
import {listQualityIssueInfo} from '@/api/quality/qualityIssueInfo/qualityIssueInfo';
import {addQualityIssueTask, listQualityIssueTask} from '@/api/quality/qualityIssueTask/qualityIssueTask';
import { useUserDirectoryStore } from '@/store/system/userDirectory';

const userDirectoryStore = useUserDirectoryStore();

const {proxy} = getCurrentInstance();
const route = useRoute();
const detailRoutePath = route.path;
const PRIORITY_OPTIONS = [
  {value: 1, label: '高', elTagType: 'danger'},
  {value: 2, label: '中', elTagType: 'warning'},
  {value: 3, label: '低', elTagType: 'primary'}
];

const loading = ref(false);
const activeTab = ref('details');
const issue = ref({});
const model = ref({});
const issueRows = ref([]);
const issueTableRef = ref();
const taskTableRef = ref();
const assignFormRef = ref();
const assignTableRef = ref();
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

watch(() => assignForm.assigneeId, id => {
  assignForm.assigneePhone = userOptions.value.find(user => String(user.userId) === String(id))?.phonenumber || '';
});
const assignTableStore = {
  config: {
    autoPagination: true,
    sort: true,
    pagination: {pageSizes: [10, 20, 50, 100], size: 'small', pagerCount: 5,
      layout: 'total, sizes, prev, pager, next', notAutoScroll: true},
    table: {stripe: true, maxHeight: 340, rowKey: 'key'}
  },
  columns: [
    {label: '编号', prop: 'rowNo', width: 70, align: 'left', sortable: 'custom'},
    {label: '主数据信息', prop: 'masterInfo', minWidth: 280, align: 'left', showOverflowTooltip: true},
    {label: '问题字段', prop: 'fieldName', minWidth: 160, align: 'left', showOverflowTooltip: true},
    {label: '质量规则', prop: 'ruleName', minWidth: 180, align: 'left', showOverflowTooltip: true},
    {label: '失败原因', prop: 'failReason', minWidth: 200, align: 'left', showOverflowTooltip: true}
  ],
  func: async (params = {}) => {
    const data = assignDialog.rows.map((item, index) => ({
      key: item.key,
      rowNo: index + 1,
      masterInfo: `${model.value.name || '主数据'} (${item.issueRow.masterRecordId || '-'})`,
      fieldName: item.issueInfo?.modelColumnName || item.issueInfo?.fieldName || item.issueRow.problemFields || item.issueRow.fieldName || '-',
      ruleName: item.issueInfo?.qualityRuleName || item.issueInfo?.ruleName || item.issueRow.qualityRuleName || '-',
      failReason: item.issueInfo?.failReason || item.issueInfo?.reason || item.issueInfo?.errorMessage
        || item.issueRow.reason || item.issueRow.failReason || item.issueRow.errorMessage || '-'
    }));
    if (params.orderByColumn === 'rowNo' && ['ascending', 'descending'].includes(params.isAsc)) {
      const direction = params.isAsc === 'ascending' ? 1 : -1;
      data.sort((a, b) => (a.rowNo - b.rowNo) * direction);
    }
    return {data};
  }
};

function isPastDate(date) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return date.getTime() < today.getTime();
}
let detailRequest = 0;

const issueStatusOptions = [
  {label: '待处理', value: '1', elTagType: 'warning'},
  {label: '整改中', value: '2', elTagType: 'primary'},
  {label: '已关闭', value: '3', elTagType: 'success'},
  {label: '已完成', value: '4', elTagType: 'success'},
];
const detailItems = computed(() => [
  {label: '主数据模型', key: 'modelId', formatter: value => model.value.name || displayId(value)},
  {label: '模型版本', key: 'modelVersion'},
  {label: '主数据记录ID', key: 'masterRecordId'},
  {
    label: '质量任务名称',
    key: 'qualityTaskName',
    span: 24,
    ellipsisClass: 'ellipsis-2',
    className: 'mt2 mb2'
  },
  {label: '问题编号', key: 'issueCode'},
  {label: '首次发现时间', key: 'firstFoundTime', type: 'time'},
  {label: '最近检测时间', key: 'lastFoundTime', type: 'time'},
]);

watch(() => [route.path, route.query.id], ([, id]) => {
  loadDetail(id);
}, {immediate: true});

async function loadDetail(id) {
  const request = ++detailRequest;
  if (route.path !== detailRoutePath) return;
  const currentId = normaliseIssueId(id);
  activeTab.value = 'details';
  issueRows.value = [];
  issue.value = {};
  model.value = {};
  loading.value = false;
  if (!currentId) return;

  loading.value = true;
  try {
    const response = await getQualityIssue(currentId);
    if (request !== detailRequest) return;
    issue.value = response?.data || {};
    if (issue.value.modelId && proxy.$auth.hasPermi('model:base:query')) {
      const metadata = await getBase(issue.value.modelId).catch(() => null);
      if (request === detailRequest) model.value = metadata?.data || {};
    }
  } catch {
    if (request === detailRequest) {
      issue.value = {};
      proxy.$modal.msgError('问题数据加载失败，请稍后重试');
    }
  } finally {
    if (request === detailRequest) {
      loading.value = false;
      await nextTick();
      issueTableRef.value?.resetQuery();
      taskTableRef.value?.resetQuery();
    }
  }
}

async function fetchIssueRows(params) {
  const issueId = issue.value.id;
  if (!issueId) return {data: {rows: [], total: 0}};

  const response = await getQualityIssue(issueId);
  if (issueId !== issue.value.id) return {data: {rows: [], total: 0}};
  const latestIssue = response?.data;
  if (latestIssue) issue.value = {...issue.value, ...latestIssue};
  const sourceRows = Array.isArray(latestIssue?.qualityIssueInfoList)
    ? latestIssue.qualityIssueInfoList
    : await fetchAllDetailRows(listQualityIssueInfo, params, issueId);
  if (issueId !== issue.value.id) return {data: {rows: [], total: 0}};
  const rows = sourceRows.map(normaliseIssueRow);
  if (params?.orderByColumn === 'id' && ['ascending', 'descending'].includes(params.isAsc)) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    rows.sort((a, b) => direction * String(a.id).localeCompare(String(b.id), undefined, {numeric: true}));
  }
  issueRows.value = rows;
  return {data: {rows, total: rows.length}};
}

async function fetchTaskRows(params) {
  const issueId = issue.value.id;
  if (!issueId) return {data: {rows: [], total: 0}};

  const sourceRows = await fetchAllDetailRows(listQualityIssueTask, params, issueId);
  if (issueId !== issue.value.id) return {data: {rows: [], total: 0}};
  const embedded = issue.value.qualityIssueInfoList;
  const relatedRows = Array.isArray(embedded) && embedded.length
    ? embedded.map(normaliseIssueRow)
    : issueRows.value;
  const rows = sourceRows.map(row => normaliseTaskRow(row, relatedRows));
  if (params?.orderByColumn === 'id' && ['ascending', 'descending'].includes(params.isAsc)) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    rows.sort((a, b) => direction * String(a.id).localeCompare(String(b.id), undefined, {numeric: true}));
  }
  return {data: {rows, total: rows.length}};
}

async function fetchAllDetailRows(query, params, issueId) {
  const rows = [];
  let pageNum = 1;
  let total;
  do {
    // 接口每页最多 100 条，按 ID 稳定排序取齐后交给表格做前端分页。
    const response = await query({...params, issueId, pageNum, pageSize: 100, orderByColumn: 'id', isAsc: 'asc'});
    if (issueId !== issue.value.id) return [];
    const pageRows = extractRows(response);
    total = Number(response?.data?.total ?? response?.total ?? 0);
    rows.push(...pageRows);
    if (!pageRows.length) break;
    pageNum++;
  } while (rows.length < total);
  return rows;
}

function extractRows(response) {
  if (Array.isArray(response?.data)) return response.data;
  return response?.data?.rows || response?.rows || [];
}

function normaliseIssueRow(row) {
  return {
    ...row,
    fieldName: row.modelColumnName || row.fieldName || displayId(row.modelColumnId),
    ruleName: row.qualityRuleName || row.ruleName || displayId(row.qualityRuleId),
    failReason: row.failReason || row.reason || row.errorMessage || '-',
    status: String(row.status ?? '1'),
    rectificationDesc: row.rectificationDesc || row.rectificationStatus || '未下发'
  };
}

function normaliseTaskRow(row, relatedRows = issueRows.value) {
  const related = relatedRows.find(item => String(item.qualityRuleId) === String(row.qualityRuleId));
  return {
    ...row,
    fieldName: row.modelColumnName || row.fieldName || related?.fieldName || displayId(row.modelColumnId || related?.modelColumnId),
    ruleName: row.qualityRuleName || row.ruleName || related?.ruleName || displayId(row.qualityRuleId),
    assigneeName: row.assigneeName || row.nickName || row.assigneeNickName || displayId(row.assigneeId),
    status: String(row.status ?? '1')
  };
}

function normaliseIssueId(value) {
  const id = Array.isArray(value) ? value[0] : value;
  return /^\d+$/.test(String(id ?? '')) ? String(id) : '';
}

function displayId(value) {
  return value === null || value === undefined || value === '' ? '-' : String(value);
}

function handleTabChange(name) {
  if (name === 'details') issueTableRef.value?.getList();
  if (name === 'rectification') taskTableRef.value?.getList();
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

function assignIssue(row) {
  if (!row || String(issue.value.status) === '3' || String(row.status) === '2') return;
  const key = `${issue.value.id}-${row.id || row.qualityRuleId || 'detail'}`;
  assignDialog.parentRows = [issue.value];
  assignDialog.rows = [{key, issueRow: issue.value, issueInfo: row}];
  assignDialog.submitting = false;
  Object.assign(assignForm, {assigneeId: null, assigneePhone: '', rectificationTime: null, priority: null});
  assignDialog.visible = true;
  loadAssignUsers();
  nextTick(() => {
    assignFormRef.value?.clearValidate();
    assignTableRef.value?.resetQuery();
  });
}

function closeAssignDialog() {
  assignDialog.visible = false;
  assignDialog.rows = [];
  assignDialog.parentRows = [];
  assignDialog.submitting = false;
  Object.assign(assignForm, {assigneeId: null, assigneePhone: '', rectificationTime: null, priority: null});
}

async function submitAssign() {
  if (assignDialog.submitting || !await assignFormRef.value?.validate().catch(() => false)) return;
  const selectedItems = assignDialog.rows;
  if (!selectedItems.length) {
    proxy.$modal.msgWarning('暂无待下发问题');
    return;
  }
  assignDialog.submitting = true;
  const item = selectedItems[0];
  const row = item.issueInfo;
  const taskNo = `RT${Date.now()}${issue.value.id}${item.key}`;
  try {
    await addQualityIssueTask({
      taskNo,
      taskName: `${issue.value.masterName || model.value.name || '主数据'}问题整改`.slice(0, 128),
      ...(row.id ? {issueInfoIds: [row.id]} : {issueId: issue.value.id, qualityRuleId: row.qualityRuleId || issue.value.qualityRuleId}),
      modelId: issue.value.modelId,
      modelVersion: issue.value.modelVersion,
      masterRecordId: issue.value.masterRecordId,
      assigneeId: assignForm.assigneeId,
      priority: assignForm.priority,
      rectificationTime: `${assignForm.rectificationTime} 23:59:59`,
      status: '1',
      validFlag: true,
      taskDescription: '请完成质量问题整改'
    });
    await updateQualityIssue({
      id: issue.value.id,
      status: '2',
      rectificationStatus: '待处理'
    });
    issue.value.status = '2';
    issue.value.rectificationStatus = '待处理';
    row.rectificationDesc = '待处理';
    proxy.$modal.msgSuccess('下发成功');
    closeAssignDialog();
    await nextTick();
    issueTableRef.value?.getList();
    taskTableRef.value?.getList();
  } catch {
    proxy.$modal.msgError('下发整改失败，请稍后重试');
  } finally {
    assignDialog.submitting = false;
  }
}
</script>

<style scoped lang="scss">
.pagecont-bottom {
  min-height: calc(100vh - 343px) !important;
}

</style>
