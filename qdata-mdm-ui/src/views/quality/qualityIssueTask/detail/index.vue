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
  整改任务详情：展示问题项、处理状态和整改记录。
-->
<template>
  <div v-loading="loading" class="app-container">
    <template v-if="task">
      <detail-info
          :data="{ ...task, displayName: task.taskName || '-' }"
          mode="free"
          :items="infoItems"
          :header="{ idKey: 'id', nameKey: 'displayName', statusKey: 'status', statusOptions: TASK_STATUS_OPTIONS }"
      >
        <template #modelInfo>
          <rectification-model-info :model="modelInfo" :fallback="task" />
        </template>
        <template #location>
          <div v-if="locationText" class="request-model-location flex-center">
            <img :src="getProjectIcon(modelInfo.projectIcon)" alt="" class="request-info-icon" />
            <span class="request-model-title text-ellipsis" :title="locationText">{{ locationText }}</span>
          </div>
          <span v-else>-</span>
        </template>
      </detail-info>
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab">
          <qt-tab-pane name="basic" label="基本信息">
            <div class="basic-info pb15">
              <div class="h2-titles mb15">请求说明</div>
              <descriptions-info
                  :data="task"
                  :items="requestItems"
                  :column="2"
                  class="detail-descriptions"
              >
                <template #modelInfo>
                  <rectification-model-info :model="modelInfo" :fallback="task" />
                </template>
              </descriptions-info>

              <div class="mt15">
                <div class="h2-titles mb15">整改要求</div>
                <descriptions-info :data="task" :items="requirementItems" :column="2" class="detail-descriptions"/>
              </div>
            </div>
          </qt-tab-pane>
          <qt-tab-pane name="issue" label="问题数据">
            <div class="h2-titles mb15">问题范围说明</div>
            <descriptions-info
                :data="issueScope"
                :items="issueScopeItems"
                :column="2"
                class="detail-descriptions"
            />

            <qt-wrap
                v-loading="issuesLoading"
                :columns="issueTableStore.columns"
                :table-ref="{ getList: () => loadTask({ refresh: true }) }"
                :config="{ fullContent: false, actions: { table: { search: false } } }"
                class="mt15"
            >
              <template #actions-data>
                <div class="h2-titles">问题数据列表</div>
              </template>
              <qt-table
                  ref="issueTableRef"
                  v-bind="issueTableStore"
              >
                <template #unavailable>-</template>
                <template #ruleType="{ row }">
                  <dict-tag v-if="row.ruleType" :options="rule_rulequality_type" :value="row.ruleType"/>
                  <span v-else>-</span>
                </template>
                <template #status="{ row }">
                  <dict-tag :options="ITEM_STATUS_OPTIONS" :value="row.taskStatus"/>
                </template>
                <template #action="{ row }">
                  <el-button
                      link
                      type="primary"
                      icon="View"
                      @click="viewIssueDetail(row)"
                  >查看
                  </el-button>
                  <el-button
                      v-if="isAssignee && row.taskStatus === '1'"
                      v-hasPermi="['quality:qualityissuetask:edit']"
                      link
                      type="primary"
                      :disabled="submitting"
                      @click="handleStart(row)"
                  >
                    <template #icon>
                      <svg-icon icon-class="start-rectification"/>
                    </template>
                    开始整改
                  </el-button>
                  <el-button
                      v-else-if="isAssignee && row.taskStatus === '2'"
                      v-hasPermi="['quality:qualityissuetask:edit']"
                      link
                      type="primary"
                      :disabled="submitting"
                      @click="handleComplete(row)"
                  >
                    <template #icon>
                      <svg-icon icon-class="continue-rectification"/>
                    </template>
                    继续整改
                  </el-button>
                  <template v-else-if="row.taskStatus === '3'">
                    <el-button
                        v-if="isAssignee"
                        v-hasPermi="['quality:qualityissuetask:edit']"
                        link
                        type="primary"
                        :disabled="submitting"
                        @click="handleModify(row)"
                    >
                      <template #icon>
                        <svg-icon icon-class="edit-result"/>
                      </template>
                      修改结果
                    </el-button>
                  </template>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
          <qt-tab-pane name="history" label="整改记录">
            <div v-loading="logsLoading" class="history pb15">
              <div class="h2-titles mb15">整改说明</div>
              <descriptions-info
                  :data="historySummary"
                  :items="historySummaryItems"
                  :column="2"
                  class="detail-descriptions"
              />

              <div v-if="logsFailed" class="mt15">
                <div class="h2-titles mb15">整改记录列表</div>
                <el-empty description="整改记录加载失败">
                  <el-button @click="loadLogs(task.id)">重试</el-button>
                </el-empty>
              </div>
              <qt-wrap
                  v-show="!logsFailed"
                  :columns="historyTableStore.columns"
                  :table-ref="{ getList: () => loadLogs(task.id) }"
                  :config="{ fullContent: false, actions: { table: { search: false } } }"
                  class="mt15"
              >
                <template #actions-data>
                  <div class="h2-titles">整改记录列表</div>
                </template>
                <qt-table
                    ref="historyTableRef"
                    v-bind="historyTableStore"
                >
                  <template #execResult>
                    <dict-tag type="success">成功</dict-tag>
                  </template>
                  <template #handler="{ row }">
                    <div class="flex-column">
                      <span>{{ row.createBy || '-' }}</span>
                      <span class="desc-text">{{ row.handlerPhone || '-' }}</span>
                    </div>
                  </template>
                  <template #action="{ row }">
                    <el-button
                        link
                        type="primary"
                        icon="View"
                        @click="showLog(row)"
                    >查看
                    </el-button>
                  </template>
                </qt-table>
              </qt-wrap>
            </div>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </template>
    <el-empty v-else-if="!loading" :description="loadFailed ? '任务加载失败，请重试' : '缺少有效任务编号'">
      <el-button v-if="loadFailed" @click="loadTask()">重试</el-button>
      <el-button @click="goBack">返回</el-button>
    </el-empty>
    <el-dialog
        v-model="logDialogVisible"
        title="整改记录详情"
        width="800px"
        destroy-on-close
        @close="handleLogClose"
        class="rectification-detail-dialog"
    >
      <el-form
          :model="selectedLog"
          label-width="110px"
          class="column-form rectification-detail-form"
      >
        <qt-form-item
            v-for="item in logDetailItems"
            :key="item.key"
            :label="item.label"
            :prop="item.key"
            class="row-full"
        >
          <dict-tag v-if="item.key === 'execResult'" type="success">成功</dict-tag>
          <div v-else class="form-readonly">
            <template v-if="item.type === 'time'">
              {{ parseTime(selectedLog[item.key], '{y}-{m}-{d} {h}:{i}') || '-' }}
            </template>
            <template v-else>{{ selectedLog[item.key] || '-' }}</template>
          </div>
        </qt-form-item>
      </el-form>
      <template #footer>
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-dialog
        v-model="issueDialogVisible"
        title="问题数据详情"
        width="800px"
        destroy-on-close
        @close="handleIssueClose"
        class="rectification-detail-dialog"
    >
      <el-form
          :model="selectedIssue"
          label-width="110px"
          class="column-form rectification-detail-form"
      >
        <qt-form-item
            v-for="item in issueDetailItems"
            :key="item.key"
            :label="item.label"
            :prop="item.key"
            class="row-full"
        >
          <template v-if="item.dictOptions">
            <dict-tag
                v-if="selectedIssue[item.key]"
                :options="item.dictOptions"
                :value="selectedIssue[item.key]"
            />
            <span v-else>-</span>
          </template>
          <div v-else class="form-readonly">
            <template v-if="item.type === 'time'">
              {{ parseTime(selectedIssue[item.key], '{y}-{m}-{d} {h}:{i}') || '-' }}
            </template>
            <template v-else>{{ selectedIssue[item.key] || '-' }}</template>
          </div>
        </qt-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="QualityIssueTaskDetail">
import {computed, getCurrentInstance, onBeforeUnmount, reactive, ref, watch} from 'vue';

import {useRoute, useRouter} from 'vue-router';

import useUserStore from '@/store/system/user';
import {usePageRefresh} from '@/composables/usePageRefresh';
import {getQualityIssueTask, updateQualityIssueTask} from '@/api/quality/qualityIssueTask/qualityIssueTask';
import {listQualityIssueTaskLog} from '@/api/quality/qualityIssueTaskLog/qualityIssueTaskLog';
import {getBase} from '@/api/model/base/base';
import {getProjectIcon} from '@/utils/imageUrl';
import RectificationModelInfo from '../components/RectificationModelInfo.vue';

const TASK_STATUS_OPTIONS = [
  {value: '1', label: '待审批', elTagType: 'warning'},
  {value: '2', label: '审批中', elTagType: 'primary'},
  {value: '3', label: '已完成', elTagType: 'success'},
  {value: '4', label: '已取消', elTagType: 'info'},
  {value: '5', label: '处理中', elTagType: 'primary'}
];
const ITEM_STATUS_OPTIONS = [
  {value: '1', label: '待整改', elTagType: 'warning'},
  {value: '2', label: '整改中', elTagType: 'primary'},
  {value: '3', label: '已整改', elTagType: 'success'}
];

const {proxy} = getCurrentInstance();
const {rule_rulequality_type} = proxy.useDict('rule_rulequality_type');
const route = useRoute();
// 保存本页路径，避免切换路由时误读其他详情页的 ID。
const detailRoutePath = route.path;
const router = useRouter();
const user = useUserStore();
const task = ref();
const modelInfo = ref({});
const locationText = computed(() => [
  modelInfo.value.projectName,
  modelInfo.value.layers?.at(-1)?.name,
].filter(Boolean).join(' / '));

const logs = ref([]);
const activeTab = ref('basic');
const loading = ref(false);
const issuesLoading = ref(false);
const loadFailed = ref(false);
const logsLoading = ref(false);
const logsFailed = ref(false);
const issueTableRef = ref();
const historyTableRef = ref();
const submitting = ref(false);
const logDialogVisible = ref(false);
const selectedLog = ref({});
const issueDialogVisible = ref(false);
const selectedIssue = ref({});
const {setRefreshNeeded} = usePageRefresh('quality-rectification');
// 操作权限不依赖列表来源视图，负责人从合并列表进入详情后仍可处理本人任务。
const isAssignee = computed(() => task.value
    && String(task.value.assigneeId) === String(user.id));
const canHandle = computed(() => isAssignee.value
    && proxy.$auth.hasPermi('quality:qualityissuetask:edit'));
// 整改期限使用任务的 rectificationTime 字段。
const infoItems = [
  {label: '任务编号', key: 'taskNo'},
  {label: '模型信息', slot: 'modelInfo'},
  {label: '归属位置', slot: 'location'},
  {label: '问题数据', key: 'issueDataCount', className: 'mt2'},
  {label: '涉及主数据数量', key: 'masterDataCount', className: 'mt2'},
  {label: '整改负责人', key: 'assigneeName', className: 'mt2', formatter: formatAssignee},
  {label: '下发时间', key: 'createTime', type: 'time', className: 'mt2'},
  {label: '整改期限', key: 'rectificationTime', type: 'time', className: 'mt2'},
  {label: '任务来源', value: '质量检测任务', className: 'mt2'}
];
const requestItems = [
  {label: '任务名称', key: 'taskName'},
  {label: '涉及主数据数', key: 'masterDataCount'},
  {label: '任务说明', key: 'requirement'},
  {label: '整改负责人', key: 'assigneeName', formatter: formatAssignee},
  {label: '主数据模型', key: 'modelName', slot: 'modelInfo'},
  {label: '问题数据数', key: 'issueDataCount'},
  {label: '当前状态', key: 'status', dictOptions: TASK_STATUS_OPTIONS},
  {label: '整改进度', key: 'completedIssueCount', formatter: (_value, row) => getRectificationProgress(row).label},
  {label: '验收通过数', key: 'completedIssueCount'}
];
const requirementItems = [
  {label: '整改方式', value: '人工整改'},
  {label: '验收方式', value: '任务统一提交验收'},
  {label: '整改范围', value: '任务内全部问题数据'},
  {label: '超期处理', value: '超期标记并提醒'},
  {label: '处理流程', value: '待处理→处理中→待验收→已完成(驳回返回处理中)'},
  {label: '提交规则', value: '所有未通过问题整改完成后方可提交验收'},
  {label: '备注', value: '已验收通过问题不再重复整改', span: 2}
];
const issueScopeItems = [
  {label: '任务问题数', key: 'issueDataCount'},
  {label: '涉及主数据数', key: 'masterDataCount'},
  {label: '待处理问题', key: 'pendingCount'},
  {label: '整改中问题', key: 'processingCount'},
  {label: '待验收问题', key: 'pendingAcceptanceCount'},
  {label: '已完成问题', key: 'completedCount'},
  {label: '任务处理规则', value: '一条整改任务包含多条问题数据'},
  {label: '提交规则', value: '所有未通过问题整改完成后方可提交验收'}
];
const issueScope = computed(() => {
  const items = Array.isArray(task.value?.issueItems) ? task.value.issueItems : null;
  const pendingCount = items?.filter(item => item.taskStatus === '1').length ?? 0;
  const processingCount = items?.filter(item => item.taskStatus === '2').length ?? 0;
  return {
    issueDataCount: task.value?.issueDataCount,
    masterDataCount: task.value?.masterDataCount,
    pendingCount,
    processingCount,
    completedCount: items?.filter(item => item.taskStatus === '3').length ?? 0,
    // 待验收数包含待整改和整改中的问题项。
    pendingAcceptanceCount: pendingCount + processingCount
  };
});
const issueColumns = reactive([
  {label: '编号', prop: 'id', minWidth: 100, sortable: true},
  {label: '问题编码', prop: 'issueCode', minWidth: 220, align: 'left'},
  {label: '主数据', prop: 'masterName', minWidth: 220, align: 'left'},
  {label: '主数据编码', prop: 'masterCode', minWidth: 130, align: 'left'},
  {label: '问题字段', prop: 'modelColumnName', minWidth: 140, align: 'left'},
  {label: '问题类型', prop: 'ruleType', slot: 'ruleType', minWidth: 150, align: 'left'},
  {label: '问题描述', prop: 'failReason', minWidth: 260, showOverflowTooltip: true, align: 'left'},
  {label: '整改状态', slot: 'status', width: 110, minWidth: 110, align: 'left'},
  {label: '最新整改结果', slot: 'unavailable', minWidth: 170, align: 'left'},
  {label: '操作', slot: 'action', width: 180, minWidth: 180, fixed: 'right'}
]);
const historySummaryItems = [
  {label: '整改方式', value: '逐条整改与批量整改结合'},
  {label: '整改提交人', key: 'updateBy'},
  {label: '最近整改时间', key: 'lastHandledTime', type: 'time'},
  {label: '整改进度', key: 'progressLabel'},
  {label: '是否允许多次整改', value: '是'},
  {label: '当前提交状态', key: 'status', dictOptions: TASK_STATUS_OPTIONS},
  {label: '提交规则', value: '问题完成整改后记录整改结果'},
  {label: '描述', value: '同一问题多次整改保留多条整改记录'}
];
const historyColumns = reactive([
  {label: '编号', prop: 'id', minWidth: 100, sortable: true, align: 'left'},
  {label: '整改编号', prop: 'taskInfoId', minWidth: 130, sortable: true, align: 'left'},
  {label: '问题编码', prop: 'issueCode', minWidth: 220, align: 'left'},
  {label: '主数据', prop: 'masterDataObject', minWidth: 220, align: 'left'},
  {label: '整改字段', prop: 'rectificationField', minWidth: 140, align: 'left'},
  {label: '整改前值', prop: 'beforeValue', minWidth: 180, align: 'left'},
  {label: '整改后值', prop: 'afterValue', minWidth: 180, align: 'left'},
  {label: '整改人', prop: 'createBy', slot: 'handler', minWidth: 200, align: 'left'},
  {
    label: '整改时间',
    prop: 'handlerTime',
    date: '{y}-{m}-{d} {h}:{i}:{s}',
    minWidth: 170,
    sortable: true,
    align: 'left'
  },
  {label: '执行结果', slot: 'execResult', width: 110, minWidth: 110, align: 'left'},
  {label: '操作', slot: 'action', width: 120, minWidth: 120, fixed: 'right'}
]);
const logDetailItems = [
  {label: '日志 ID', key: 'id'},
  {label: '整改编号', key: 'taskInfoId'},
  {label: '问题编码', key: 'issueCode'},
  {label: '主数据', key: 'masterDataObject'},
  {label: '整改字段', key: 'rectificationField'},
  {label: '执行结果', key: 'execResult'},
  {label: '整改时间', key: 'handlerTime', type: 'time'},
  {label: '整改人', key: 'createBy'},
  {label: '联系电话', key: 'handlerPhone'}
];
const issueDetailItems = computed(() => [
  {label: '问题编码', key: 'issueCode'},
  {label: '主数据', key: 'masterName'},
  {label: '主数据编码', key: 'masterCode'},
  {label: '问题字段', key: 'modelColumnName'},
  {label: '问题类型', key: 'ruleType', dictOptions: rule_rulequality_type.value},
  {label: '问题描述', key: 'failReason'},
  {label: '整改状态', key: 'taskStatus', dictOptions: ITEM_STATUS_OPTIONS},
  {label: '整改时间', key: 'rectificationTime', type: 'time'},
  {label: '更新人', key: 'updateBy'},
  {label: '联系电话', key: 'updaterPhone'}
]);
// 过滤下发日志，并关联问题编码。
const historyRows = computed(() => {
  const issueCodes = new Map((task.value?.issueItems || []).map(item => [String(item.issueId), item.issueCode]));
  return logs.value
      .filter(entry => entry.remark !== 'rectification:v1:ASSIGN')
      .map(entry => ({
        ...entry,
        issueCode: issueCodes.get(String(entry.issueId))
      }));
});
const historySummary = computed(() => ({
  lastHandledTime: historyRows.value.reduce((latest, entry) => {
    const handledTime = entry.handlerTime || entry.createTime;
    return handledTime && (!latest || handledTime > latest) ? handledTime : latest;
  }, null),
  progressLabel: getRectificationProgress(task.value).label,
  updateBy: task.value.updateBy,
  status: task.value.status
}));
let taskSequence = 0;
let logSequence = 0;
let modelSequence = 0;
const issueTableStore = reactive({
  config: {
    autoPagination: true,
    sort: true,
    table: {maxHeight: 480},
    pagination: {notAutoScroll: true}
  },
  columns: issueColumns,
  func: getIssueRows,
  params: {},
  events: {}
});
const historyTableStore = reactive({
  config: {
    autoPagination: true,
    sort: true,
    table: {maxHeight: 480, defaultSort: {prop: 'handlerTime', order: 'descending'}},
    pagination: {notAutoScroll: true}
  },
  columns: historyColumns,
  func: getHistoryRows,
  params: {},
  events: {}
});

/**
 * 格式化整改负责人的姓名和联系电话。
 * @param {*} _value 未使用的字段值
 * @param {Object} row 任务
 * @returns {string} 姓名和联系电话
 */
function formatAssignee(_value, row) {
  return `${row.assigneeName || '-'} / ${row.assigneePhone || '-'}`;
}


/**
 * 返回任务的全部问题项，供表格本地分页。
 * @param {Object} params 排序参数
 * @returns {Promise<{data: {rows: Object[], total: number}}>} 问题项及总数
 */
function getIssueRows(params) {
  const rows = Array.isArray(task.value?.issueItems)
      ? task.value.issueItems.map(item => ({
        ...item,
        modelName: task.value.modelName || modelInfo.value.name || '-',
        modelCode: task.value.modelCode || modelInfo.value.code || '-'
      }))
      : [];
  if (params?.orderByColumn === 'id' && params.isAsc) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    rows.sort((a, b) => (Number(a.id ?? 0) - Number(b.id ?? 0)) * direction);
  }
  return Promise.resolve({data: {rows, total: rows.length}});
}

/**
 * 返回排序后的整改记录，供表格本地分页。
 * @param {Object} params 排序参数
 * @returns {Promise<{data: {rows: Object[], total: number}}>} 整改记录及总数
 */
function getHistoryRows(params) {
  const rows = [...historyRows.value];
  const sortKey = params?.orderByColumn;
  if (['id', 'taskInfoId', 'createTime', 'handlerTime'].includes(sortKey) && params.isAsc) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    rows.sort((a, b) => {
      const av = a[sortKey];
      const bv = b[sortKey];
      if (av == null) return bv == null ? 0 : 1;
      if (bv == null) return -1;
      const comparison = ['createTime', 'handlerTime'].includes(sortKey)
          ? String(av).localeCompare(String(bv))
          : Number(av) - Number(bv);
      return comparison * direction;
    });
  }
  return Promise.resolve({data: {rows, total: rows.length}});
}

/**
 * 打开整改记录详情。
 * @param {Object} row 整改记录
 */
function showLog(row) {
  selectedLog.value = row;
  logDialogVisible.value = true;
}

/**
 * 打开问题项详情。
 * @param {Object} row 问题项
 */
function viewIssueDetail(row) {
  selectedIssue.value = row;
  issueDialogVisible.value = true;
}

/** 关闭弹窗时清空问题项。 */
function handleIssueClose() {
  selectedIssue.value = {};
}

/** 关闭弹窗时清空整改记录。 */
function handleLogClose() {
  selectedLog.value = {};
}

/**
 * 加载整改任务详情，可保留表格当前页。
 * @param {Object} [options] 加载选项
 * @param {boolean} [options.refresh=false] 保留当前页
 * @returns {Promise<void>}
 */
async function loadTask({refresh = false} = {}) {
  const sequence = ++taskSequence;
  if (!refresh) {
    logSequence++;
    modelSequence++;
    logsLoading.value = false;
    logsFailed.value = false;
    task.value = undefined;
    modelInfo.value = {};
    logs.value = [];
    logDialogVisible.value = false;
    selectedLog.value = {};
    issueDialogVisible.value = false;
    selectedIssue.value = {};
    loadFailed.value = false;
  }
  const id = route.query.id;
  if (route.path !== detailRoutePath || !id || !/^\d+$/.test(String(id)) || Number(id) <= 0) {
    loading.value = false;
    issuesLoading.value = false;
    return;
  }
  loading.value = !refresh;
  issuesLoading.value = refresh;
  try {
    const response = await getQualityIssueTask(id);
    if (sequence !== taskSequence) {
      return;
    }
    if (!response.data) {
      if (!refresh) loadFailed.value = true;
      return;
    }
    const previousModelId = task.value?.modelId;
    task.value = response.data;
    if (refresh) {
      issueTableRef.value?.getList();
    } else {
      loadLogs(id);
    }
    if (!refresh || String(previousModelId) !== String(task.value.modelId)) {
      loadModelInfo(task.value.modelId);
    }
  } catch {
    if (sequence === taskSequence && !refresh) {
      loadFailed.value = true;
    }
  } finally {
    if (sequence === taskSequence) {
      loading.value = false;
      issuesLoading.value = false;
    }
  }
}

/**
 * 异步补全任务关联的模型信息。
 * @param {number|string} modelId 模型 ID
 * @returns {Promise<void>}
 */
async function loadModelInfo(modelId) {
  const sequence = ++modelSequence;
  modelInfo.value = {};
  if (!modelId || !proxy.$auth.hasPermi('model:base:query')) return;
  try {
    const {data} = await getBase(modelId);
    if (sequence === modelSequence && data) modelInfo.value = data;
  } catch {
    // 查询失败时继续显示任务自带的模型信息。
  }
}

/**
 * 分页读取全部整改记录，仅更新当前任务。
 * @param {number|string} id 整改任务 ID
 * @returns {Promise<void>}
 */
async function loadLogs(id) {
  const sequence = ++logSequence;
  logsLoading.value = true;
  logsFailed.value = false;
  try {
    const logRows = [];
    let pageNum = 1;
    let total;
    do {
      // 同一时刻的记录按 ID 排序，保证连续分页时的顺序稳定。
      const response = await listQualityIssueTaskLog({
        qualityIssueTaskId: id,
        pageNum,
        pageSize: 100,
        orderByColumn: 'create_time,id',
        isAsc: 'asc'
      });
      if (sequence !== logSequence || String(route.query.id) !== String(id)) {
        return;
      }
      const rows = response.data?.rows || [];
      total = Number(response.data?.total || 0);
      logRows.push(...rows);
      if (!rows.length) {
        break;
      }
      pageNum++;
    } while (logRows.length < total);
    logs.value = logRows;
    historyTableRef.value?.getList();
  } catch {
    if (sequence === logSequence) {
      logsFailed.value = true;
    }
  } finally {
    if (sequence === logSequence) {
      logsLoading.value = false;
    }
  }
}

/**
 * 开始整改：待整改 → 整改中。
 * @param {Object} row 待整改的问题项
 * @returns {Promise<boolean|undefined>|undefined} 更新成功返回 true，失败返回 false，跳过时无值
 */
function handleStart(row) {
  if (!canHandle.value || row.taskStatus !== '1') {
    return;
  }
  return updateStatus(row.taskInfoId, '2', '执行成功');
}

/**
 * 完成整改：整改中 → 已整改。
 * @param {Object} row 整改中的问题项
 * @returns {Promise<boolean|undefined>|undefined} 更新成功返回 true，失败返回 false，跳过时无值
 */
function handleComplete(row) {
  if (submitting.value || !canHandle.value || row.taskStatus !== '2') {
    return;
  }
  return updateStatus(row.taskInfoId, '3', '执行成功');
}

/**
 * 重新整改：已整改 → 整改中。
 * @param {Object} row 已整改的问题项
 * @returns {Promise<boolean|undefined>|undefined} 更新成功返回 true，失败返回 false，跳过时无值
 */
function handleModify(row) {
  if (submitting.value || !canHandle.value || row.taskStatus !== '3') {
    return;
  }
  return updateStatus(row.taskInfoId, '2', '执行成功');
}

/**
 * 更新任务明细状态，刷新详情并设置列表刷新标记。
 * @param {number|string} id 任务明细 ID（taskInfoId）
 * @param {'2'|'3'} status 2：开始或重新整改；3：完成整改
 * @param {string} message 成功提示
 * @returns {Promise<boolean|undefined>} 更新成功返回 true，失败返回 false，跳过时无值
 */
async function updateStatus(id, status, message) {
  if (submitting.value || !id || !task.value?.issueItems?.some(item => String(item.taskInfoId) === String(id))) {
    return;
  }
  const detailId = route.query.id;
  submitting.value = true;
  try {
    await updateQualityIssueTask({id, status});
    proxy.$modal.msgSuccess(message);
    setRefreshNeeded();
    if (route.path === detailRoutePath && String(route.query.id) === String(detailId)) {
      await loadTask({refresh: true});
      if (route.path === detailRoutePath && String(route.query.id) === String(detailId)) {
        loadLogs(detailId);
      }
    }
    return true;
  } catch {
    // 全局请求拦截器负责反馈，失败时保留详情。
    return false;
  } finally {
    submitting.value = false;
  }
}

/** 任务未加载成功时，供空态按钮返回上一页或任务列表。 */
function goBack() {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push('/quality/qualityIssueTask');
  }
}

onBeforeUnmount(() => {
  taskSequence++;
  logSequence++;
  modelSequence++;
});
watch(() => [route.path, route.query.id], () => loadTask(), {immediate: true});

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
    return {completed: 0, total: 0, percentage: 0, label: '-'};
  }
  const percentage = Math.round((completed / total) * 100);
  return {completed, total, percentage, label: `${completed}/${total}`};
}
</script>

<style scoped>
.rectification-detail-dialog {
  max-width: calc(100vw - 32px);
}

.rectification-detail-form :deep(.el-form-item),
.rectification-detail-form :deep(.el-form-item__content) {
  min-width: 0;
}

.history {
  min-height: 480px;
}

.detail-descriptions :deep(.el-descriptions__table) {
  width: 100%;
  table-layout: fixed !important;
}

.detail-descriptions :deep(.el-descriptions__cell) {
  white-space: normal;
  word-break: break-word;
  vertical-align: top;
}

.basic-info {
  min-height: 480px;
}

.request-model-location {
  width: 100%;
  min-width: 0;
  gap: 6px;
}

.request-info-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.request-model-title {
  min-width: 0;
  max-width: 100%;
}
</style>
