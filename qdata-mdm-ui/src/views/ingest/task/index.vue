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
  接入任务列表：维护任务配置和调度状态，手工执行任务并查看运行实例。
-->
<template>
  <div class="app-container ingest-task" ref="app-container">
    <guide-tip tip-id="ingest/task.list"/>
    <el-container>
      <qt-tree
          ref="sourceTreeRef"
          directory-icons
          :dept-options="sources"
          :action-loading="sourcesLoading"
          :use-builtin-cat-edit-dialog="false"
          :tree-props="{ label: 'name', children: 'children' }"
          node-key="id"
          title="来源系统"
          placeholder="请输入来源系统名称"
          @node-click="selectSource"
      />
      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #search>
            <qt-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef"/>
          </template>
          <template #actions-data>
            <el-button v-hasPermi="['ingest:task:add']"
                       type="primary"
                       plain
                       @click="openForm()"
            ><i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
            <el-button
                v-hasPermi="['ingest:task:remove']"
                type="danger"
                plain
                icon="Delete"
                :disabled="!store.rows.length"
                @click="removeTasks(store.rows)"
            >批量删除
            </el-button>
          </template>
          <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({icon: row.modelIcon, type: row.modelType})" alt="" class="info-icon"/>
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ formatModelTitle(row) }}</span>
                  <dict-tag
                      v-if="row.modelType"
                      :options="MODEL_TYPE_OPTIONS"
                      :value="row.modelType"
                      size="small"
                      type="primary"
                  />
                  <dict-tag
                      v-if="row.modelVersionNo"
                      class="ml4"
                      :class="{ pointer: row.modelId != null }"
                      :options="getVersionTagOptions(row)"
                      :value="row.modelVersionStatus ?? row.modelVersionNo"
                      size="small"
                      type="info"
                      :role="row.modelId != null ? 'link' : undefined"
                      :tabindex="row.modelId != null ? 0 : undefined"
                      @click="openModelDetail(row, 'versions')"
                      @keydown.enter="openModelDetail(row, 'versions')"
                      @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || '-' }}</div>
              </div>
            </template>
            <template #sourceInfo="{ row }">
              <div class="source-info">
                <div class="flex-center">
                  <img :src="getSourceIcon({ icon: row.sourceIcon })" class="info-icon mr5" alt=""/>
                  <span class="table-primary-text text-ellipsis"
                        :title="formatSourceSystemTitle(row.sourceName, row.sourceCode)"
                  >
                    {{ formatSourceSystemTitle(row.sourceName, row.sourceCode) }}
                  </span>
                </div>
                <el-tag type="info" size="small">可信度：{{ row.sourceTrustScore ?? '-' }}</el-tag>
              </div>
            </template>
            <template #executionInfo="{ row }">
              <StatusTag size="small" :status="row.lastRunStatus || 'idle'"/>
              <div class="last-execute-info">
                <div class="last-execute-info__row">
                  <span></span>
                  <span class="last-execute-info__value">{{ row.lastRunTime ? timeAgo(row.lastRunTime) : '-' }}</span>
                  <span class="ml10"></span>
                  <span class="last-execute-info__value">{{ formatDuration(row.lastRunDurationMillis) }}</span>
                </div>
              </div>
            </template>
            <template #schedule="{ row }">
              <div class="flex-column flex-column-gap">
                <dict-tag
                    :icon="getTaskTypeIcon(row.type)"
                    :type="taskTypeTagType(row.type)"
                    size="small"
                >{{ getOptionLabel(TASK_TYPES, row.type) }}</dict-tag>
                <span class="flex-center"><el-icon class="mr5"><Clock/></el-icon>{{
                    formatExecutionSchedule(row)
                  }}</span>
              </div>
            </template>
            <template #status="{ row }">
              <div class="flex-column flex-column-gap">
                <div class="flex-center">
                  <span class="mr5">任务状态:</span>
                  <el-switch
                      v-model="row.status"
                      active-value="1"
                      inactive-value="0"
                      :loading="busyId === row.id"
                      :disabled="busyId === row.id || !proxy.$auth.hasPermi('ingest:task:edit')"
                      @change="changeStatus(row, $event)"
                  />
                </div>
                <div class="flex-center">
                  <span class="mr5">调度状态:</span>
                  <el-switch
                      v-model="row.schedulerStatus"
                      active-value="1"
                      inactive-value="0"
                      :loading="busyId === row.id"
                      :disabled="busyId === row.id || !proxy.$auth.hasPermi('ingest:task:edit') || row.status !== '1' || row.triggerType !== 'SCHEDULED'"
                      @change="changeScheduleStatus(row, $event)"
                  />
                </div>
              </div>
            </template>
            <template #responsiblePerson="{ row }">
              <div class="flex-column">
                <span>{{ row.responsiblePersonName || '-' }}</span>
                <span class="desc-text">{{ row.responsiblePersonPhone || '-' }}</span>
              </div>
            </template>
            <template #creator="{ row }">
              <div class="flex-column">
                <span>{{ row.createBy || '-' }}</span>
                <span class="desc-text">{{ row.createPhoneNumber || '-' }}</span>
              </div>
            </template>
            <template #createTime="{ row }">
              {{ row.createTime ? proxy.parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
            </template>
            <template #handle="{ row }">
              <el-button v-hasPermi="['ingest:task:query']"
                         link
                         type="primary"
                         icon="View"
                         @click="openForm(row, true)"
              >详情
              </el-button>
              <el-button v-hasPermi="['ingest:task:edit']"
                         link
                         type="primary"
                         icon="Edit"
                         @click="openForm(row)"
              >修改
              </el-button>
              <el-popover v-if="proxy.$auth.hasPermiOr(['ingest:task:edit', 'ingest:task:query', 'ingest:task:remove'])"
                          placement="bottom"
                          :width="150"
                          trigger="click"
                          popper-class="table-action-popover handle-popover"
              >
                <template #reference>
                  <el-button link type="primary" icon="ArrowDown">更多</el-button>
                </template>
                <div class="butgdlist task-action-list">
                  <el-button
                      v-hasPermi="['ingest:task:edit']"
                      link
                      type="primary"
                      icon="VideoPlay"
                      :disabled="row.status !== '1'"
                      :title="row.status !== '1' ? '下线任务不可执行' : ''"
                      @click="runTask(row)"
                  >
                    执行一次
                  </el-button>
                  <el-button
                      v-hasPermi="['ingest:task:query']"
                      link
                      type="primary"
                      icon="Stopwatch"
                      @click="openRuns(row)"
                  >任务实例
                  </el-button>
                  <el-button
                      v-hasPermi="['ingest:task:add']"
                      link
                      type="primary"
                      icon="CopyDocument"
                      @click="copyTask(row)"
                  >复制任务
                  </el-button>
                  <el-button v-hasPermi="['ingest:task:remove']"
                             link
                             type="danger"
                             icon="Delete"
                             @click="removeTasks([row])"
                  >删除任务
                  </el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>
        <el-dialog
            title="任务实例"
            v-model="runsVisible"

            class="medium-dialog ingest-run-dialog"
            destroy-on-close
            :append-to="$refs['app-container']"
            draggable
        >
          <task-instances v-if="runsVisible"
                          :key="runsTaskId"
                          :task-id="runsTaskId"
                          :task-name="runsTaskName"
          />
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="runsVisible = false">关闭</el-button>
            </div>
          </template>
        </el-dialog>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import {ref, reactive, nextTick, onMounted, getCurrentInstance} from 'vue';

import {Clock} from '@element-plus/icons-vue';
import QtTree from '@/components/QtTree/index.vue';

import {getModelIcon} from '@/utils/imageUrl';
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType';
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus';
import {listIngestSource} from '@/api/ingest/source/source';

import ingestTaskIcon from '@/assets/modules/ingest/icons/task/ingest-task.svg';
import {timeAgo} from '@/utils/time';
import StatusTag from '@/components/StatusTag/index.vue';
import {useRouter} from 'vue-router';

import {usePageRefresh} from '@/composables/usePageRefresh';
import {cronToZh} from '@/utils/cronUtils';
import {
  listIngestTask,
  copyIngestTask,
  runIngestTask,
  changeIngestTaskStatus,
  changeIngestTaskScheduleStatus,
  delIngestTask,
} from '@/api/ingest/task/task';

import TaskInstances from '@/views/ingest/task/components/taskInstances.vue';

import { getProjectIcon } from '@/utils/imageUrl';

import databaseIcon from '@/assets/modules/ingest/icons/task/database.svg';

import fileIcon from '@/assets/modules/ingest/icons/task/file.svg';

import apiIcon from '@/assets/modules/ingest/icons/task/api.svg';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

function formatSourceSystemTitle(name, code) {
  if (!name) return '-';
  return code ? `${name} (${code})` : name;
}

function formatDuration(value) {
  if (value == null) return '-';
  if (value < 1000) return `${value} 毫秒`;
  return `${(value / 1000).toFixed(1)} 秒`;
}

const TASK_TYPE_ICONS = { DATABASE: databaseIcon, FILE: fileIcon, API: apiIcon };

function getTaskTypeIcon(type) {
  const normalizedType = String(type || '').toUpperCase();
  return TASK_TYPE_ICONS[normalizedType === 'DB' ? 'DATABASE' : normalizedType];
}

const TASK_TYPES = [
  { value: 'DATABASE', label: '数据库' },
  { value: 'FILE', label: '文件' },
  { value: 'API', label: 'API' },
];

const getOptionLabel = (options, value) =>
  options.find((item) => item.value === value)?.label || value || '-';

const TASK_STATUSES = [
  { value: '1', label: '上线' },
  { value: '0', label: '下线' },
];

defineOptions({name: 'IngestTask'});
const {proxy} = getCurrentInstance();
const router = useRouter();
const tableRef = ref();
const busyId = ref();
const sourceTreeRef = ref();
const sources = ref([]);
const sourcesLoading = ref(false);
const selectedSourceId = ref();
onMounted(() => {
  loadSources().catch(() => {
  });
});

async function loadSources() {
  sourcesLoading.value = true;
  try {
    const response = await listIngestSource({pageNum: 1, pageSize: -1});
    sources.value = response.data?.rows || response.data || [];
    if (!sources.value.some(item => String(item.id) === String(selectedSourceId.value))) {
      selectedSourceId.value = undefined;
    }
    sourcesLoading.value = false;
    await nextTick();
    sourceTreeRef.value?.setCurrentKey(selectedSourceId.value);
    tableRef.value?.resetQuery();
  } finally {
    sourcesLoading.value = false;
  }
}

function selectSource(node) {
  selectedSourceId.value = node.id;
  tableRef.value?.resetQuery();
}

const runsVisible = ref(false);
const runsTaskId = ref();
const runsTaskName = ref('');
const store = reactive({rows: []});
/** 重新查询当前任务列表，保留表格当前查询条件。 */
const refresh = () => tableRef.value?.getList();
usePageRefresh('ingestTask', refresh);
const tableStore = reactive({
  func: listIngestTask,
  params: {},
  events: {
    formatParams: params => ({...params, sourceId: selectedSourceId.value}),
    formatData: rows => rows.map(row => {
      const source = sources.value.find(item => String(item.id) === String(row.sourceId));
      return {...row, sourceCode: row.sourceCode || source?.code, sourceIcon: row.sourceIcon || source?.icon};
    }),
  },
  config: {
    sort: true,
    initResquest: false,
    table: {
      class: 'ingest-task-table',
      rowKey: 'id',
      stripe: true,
      defaultSort: {prop: 'create_time', order: 'descending'},
      onSelectionChange: (rows) => {
        store.rows = rows;
      },
    },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50, 100],
    },
  },
  columns: [
    {type: 'selection', width: 48, minWidth: 48},
    {label: '编号', prop: 'id', sortable: true, width: 60, minWidth: 60, align: 'center'},
    {
      label: '任务信息', prop: 'name', minWidth: 260, align: 'left', info: {
        icon: () => ingestTaskIcon,
        title: row => row.name ? `${row.name}${row.code ? ` (${row.code})` : ''}` : '-',
        desc: 'remark',
        get click() {
          return proxy.$auth.hasPermi('ingest:task:query') ? row => openForm(row, true) : undefined;
        },
      }
    },
    {label: '来源系统信息', prop: 'sourceName', minWidth: 240, slot: 'sourceInfo', align: 'left'},
    {
      label: '主数据模型',
      prop: 'modelName',
      minWidth: 320,
      align: 'left',
      slot: 'modelInfo',
      showOverflowTooltip: {effect: 'light'},
    },
    {
      label: '最近执行信息', prop: 'triggerType', slot: 'executionInfo', minWidth: 255, align: 'left',
      headerTooltip: '点击下方的状态标签，任务运行中可查看实时日志，运行结束可查看最新日志。',
      showHeaderTooltip: true,
    },
    {label: '运行控制', prop: 'status', slot: 'status', minWidth: 165, align: 'left'},
    {label: '接入方式与调度', prop: 'ingestMode', slot: 'schedule', minWidth: 220, align: 'left'},
    {
      label: '责任人',
      prop: 'responsiblePersonName',
      slot: 'responsiblePerson',
      minWidth: 160,
      align: 'left'
    },
    {label: '创建人', prop: 'createBy', slot: 'creator', minWidth: 160, align: 'left'},
    {
      label: '创建时间',
      prop: 'createTime',
      slot: 'createTime',
      sortable: true,
      sortableKey: 'create_time',
      align: 'center',
      minWidth: 180,
    },
    {
      label: '操作',
      slot: 'handle',
      fixed: 'right',
      minWidth: 240,
      align: 'left',
      className: 'is-handle',
      labelClassName: 'is-handle'
    },
  ],
});
const searchStore = reactive({
  items: [
    {
      label: '任务名称',
      prop: 'name',
      component: {is: 'input', placeholder: '请输入任务名称'},
    },
    {
      label: '任务编码',
      prop: 'code',
      component: {is: 'input', placeholder: '请输入任务编码'},
    },
    {
      label: '接入类型',
      prop: 'type',
      component: {is: 'select', options: TASK_TYPES},
    },
    {
      label: '任务状态',
      prop: 'status',
      component: {is: 'select', options: TASK_STATUSES},
    },
  ],
});

/**
 * 按操作模式跳转新增、修改或详情路由。
 * @param {Object} [row] 任务记录，省略时新增。
 * @param {boolean} [readonly=false] 是否打开只读详情。
 */
function openForm(row, readonly = false) {
  router.push({
    path: readonly ? '/ingest/task/detail' : row ? '/ingest/task/edit' : '/ingest/task/add',
    query: row ? {id: row.id} : {},
  });
}

/** 将定时任务的 Cron 转为中文说明，人工任务显示执行方式。 */
function formatExecutionSchedule(row) {
  if (row.triggerType !== 'SCHEDULED') return '仅人工执行';
  return cronToZh(row.cronExpression) || '定时执行';
}

/** 统一显示主数据模型名称及可用编码。 */
function formatModelTitle(row) {
  if (!row.modelName) return row.modelCode || '-';
  return row.modelCode ? `${row.modelName} (${row.modelCode})` : row.modelName;
}

function getVersionTagOptions(row) {
  const status = MODEL_STATUS_OPTIONS.find(item => String(item.value) === String(row.modelVersionStatus));
  return [{
    value: row.modelVersionStatus ?? row.modelVersionNo,
    label: row.modelVersionNo,
    elTagType: status?.elTagType || 'info',
  }];
}

function openModelDetail(row, tab) {
  if (row.modelId == null) return;
  router.push({
    path: '/model/base/detail',
    query: {id: row.modelId, ...(tab ? {tab} : {})},
  });
}

/** 按接入类型匹配详情页使用的标签颜色。 */
function taskTypeTagType(type) {
  return ({DATABASE: 'primary', FILE: 'warning', API: 'success'})[type] || 'info';
}

/**
 * 仅允许上线任务手工执行，确认提交后刷新列表。
 * @param {Object} row 接入任务。
 */
function runTask(row) {
  if (row.status !== '1') return;
  proxy.$modal.confirm(`确认执行接入任务“${row.name}”一次？执行将按当前配置读取数据，并写入来源数据层。`)
      .then(() => runIngestTask(row.id))
      .then(() => {
        proxy.$modal.msgSuccess('任务已提交');
        refresh();
      })
      .catch(() => {
      });
}

/** 设置任务标识并打开运行实例弹窗。 */
function openRuns(row) {
  runsTaskId.value = row.id;
  runsTaskName.value = row.name || '';
  runsVisible.value = true;
}

/**
 * 确认并保存任务上下线状态，取消或失败时回退开关后刷新列表。
 * @param {Object} row 接入任务。
 * @param {string} status 目标状态。
 */
async function changeStatus(row, status) {
  const previousStatus = status === '1' ? '0' : '1';
  try {
    await proxy.$modal.confirm(
        `确认${status === '1' ? '上线' : '下线'}该接入任务？`,
    );
    busyId.value = row.id;
    await changeIngestTaskStatus({id: row.id, status});
    if (status === '0') row.schedulerStatus = '0';
    proxy.$modal.msgSuccess('操作成功');
  } catch {
    row.status = previousStatus;
  } finally {
    busyId.value = undefined;
    refresh();
  }
}

/**
 * 确认并更新定时调度开关，结束后重新读取服务端状态。
 * @param {Object} row 接入任务。
 * @param {string} status 目标调度状态。
 */
async function changeScheduleStatus(row, status) {
  const previousStatus = status === '1' ? '0' : '1';
  busyId.value = row.id;
  try {
    await proxy.$modal.confirm(`确认${status === '1' ? '启用' : '暂停'}该任务的定时调度？`);
    await changeIngestTaskScheduleStatus({id: row.id, status});
    proxy.$modal.msgSuccess('操作成功');
  } catch {
    row.schedulerStatus = previousStatus;
  } finally {
    busyId.value = undefined;
    // 同步失败时配置可能已保存，重新读取服务端状态，避免仅在页面切换。
    refresh();
  }
}

/**
 * 确认后批量删除所选任务，成功时刷新列表。
 * @param {Array<Object>} rows 所选任务记录。
 */
async function removeTasks(rows) {
  try {
    await proxy.$modal.confirm(
        `确认删除选中的 ${rows.length} 个接入任务？`,
    );
    await delIngestTask(rows.map((row) => row.id).join(','));
    proxy.$modal.msgSuccess('删除成功');
    refresh();
  } catch {
    /* 取消操作或请求失败，不更新列表 */
  }
}

/**
 * 确认复制任务后跳转到新任务的编辑页。
 * @param {Object} row 待复制任务。
 */
async function copyTask(row) {
  try {
    await proxy.$modal.confirm('确认复制该接入任务？');
    const res = await copyIngestTask(row.id);
    refresh();
    openForm({id: res.data});
  } catch {
    /* 取消操作或请求失败，不跳转 */
  }
}
</script>

<style scoped lang="scss">
/* 接入任务列表的信息层级与列对齐规则。 */
.table-primary-text {
  color: var(--el-text-color-primary);
  font-weight: var(--qt-font-weight-medium);
}

.source-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  max-width: 100%;
}

.source-info .info-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.source-info .text-ellipsis {
  min-width: 0;
}

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

.fetch-value {
  color: var(--el-text-color-regular);
  font-family: var(--el-font-family);
}

:deep(.ingest-run-dialog .el-dialog__body) {
  padding: 20px 40px !important;
}

:deep(.ingest-run-dialog .el-dialog__footer) {
  padding: 15px 20px;
}

.last-execute-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: #1e293b;
}

.last-execute-info__row {
  display: flex;
  align-items: center;
  min-width: 0;
}

.last-execute-info :deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
}

.last-execute-info :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.last-execute-info :deep(.status-icon) {
  display: block;
  width: 12px !important;
  height: 12px !important;
}

.last-execute-info :deep(.status-text) {
  position: static;
  top: auto;
}

.last-execute-info :deep(.el-tag--small) {
  height: 20px;
  padding: 3px 6px;
  font-size: 10px !important;
  line-height: 18px;
}

.last-execute-info__value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
