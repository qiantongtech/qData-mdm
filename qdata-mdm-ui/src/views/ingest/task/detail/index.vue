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
  接入任务详情：展示来源、字段映射及平台字段，联动运行记录更新执行概况。
-->
<template>
  <div v-loading="loading" class="app-container task-detail">
    <el-empty v-if="detailError" description="任务详情加载失败">
      <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
    </el-empty>
    <template v-else>
      <detail-info
          class="task-detail-info"
          :gutter="2"
          :data="task"
          mode="free"
          :header="{ nameKey: 'name', statusKey: 'status', statusOptions: TASK_DETAIL_STATUS_OPTIONS }"
          :items="detailItems"
      >
        <template #source>
          <span class="task-detail-entity">
            <img :src="getSourceIcon({ icon: task.sourceIcon })" alt=""/>
            <span>{{ task.sourceName || '-' }}</span>
          </span>
        </template>
        <template #model>
          <div class="qt-table-info model-info task-detail-model">
            <div class="qt-table-info__main">
              <img :src="getModelIcon({ icon: task.modelIcon, type: task.modelType })" alt="" class="info-icon"/>
              <span class="info-title text-ellipsis" :title="formatTaskModelTitle(task)">{{ formatTaskModelTitle(task) }}</span>
              <dict-tag :options="MODEL_TYPE_OPTIONS" :value="task.modelType" size="small" type="primary"/>
              <dict-tag
                  v-if="task.modelVersionNo"
                  :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: task.modelVersionNo }))"
                  :value="task.modelVersionStatus"
                  size="small"
              />
            </div>
          </div>
        </template>
        <template #type>
          <dict-tag :icon="TASK_TYPE_ICONS[task.type]" :type="taskTypeTagType">{{
              getOptionLabel(TASK_TYPES, task.type)
            }}
          </dict-tag>
        </template>
        <template #schedule>
          <template v-if="task.triggerType === 'SCHEDULED'">
            <el-icon>
              <Clock/>
            </el-icon>
            {{ cronToZh(task.cronExpression) }}
          </template>
          <el-tag v-else type="info">手动执行</el-tag>
        </template>
      </detail-info>
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab" class="demo-tabs">
          <el-tab-pane label="字段映射与数据处理" name="mapping">
            <qt-wrap v-loading="fieldsLoading"
                     :columns="mappingTable.columns"
                     :config="mappingTable.config"
                     :table-ref="{ getList: refreshTaskFields }"
            >
              <template #actions-data>
                <guide-tip-panel
                    type="plain"
                    title="查看当前接入任务配置的来源字段与目标字段对应关系，并了解字段类型、唯一标识及转换规则等数据处理信息"
                />
              </template>
              <qt-table ref="mappingTableRef" v-bind="mappingTable" :columns="mappingTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
                <template #text="{ row, column_data }">{{ row[(column_data.property || column_data.prop)] }}</template>
                <template #empty>暂无字段映射</template>
                <template #sourceFieldName="{ row }">
                  <span class="mr10">{{ row.sourceFieldName }}</span>
                  <span v-if="row.sourceFieldType" class="field-type">{{ row.sourceFieldType }}</span>
                  <el-tag v-if="task.uniqueFields?.includes(row.sourceFieldName)" type="success" size="small">唯一标识
                  </el-tag>
                </template>
                <template #example>-</template>
                <template #transformType="{ row }">
                  {{ getOptionLabel(TRANSFORM_OPTIONS, row.transformType || TRANSFORM_NONE) }}
                </template>
                <template #transformConfig="{ row }">{{ formatTransformConfig(row.transformConfig) }}</template>
                <template #targetFieldId="{ row }">{{ getTargetFieldName(row.targetFieldId) }}</template>
              </qt-table>
            </qt-wrap>
          </el-tab-pane>
          <el-tab-pane label="接入后平台字段" name="platform">
            <qt-wrap v-loading="fieldsLoading"
                     :columns="platformTable.columns"
                     :config="platformTable.config"
                     :table-ref="{ getList: refreshTaskFields }"
            >
              <template #actions-data>
                <guide-tip-panel
                    type="plain"
                    title="查看数据接入后平台字段的生成情况，包括字段名称、类型、初始值及相关属性，便于核对模型结构与实际落库结果"
                />
              </template>
              <qt-table ref="platformTableRef" v-bind="platformTable" :columns="platformTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
                <template #text="{ row, column_data }">{{ row[(column_data.property || column_data.prop)] }}</template>
                <template #value="{ row }">
                  <el-tag v-if="row.tag" type="warning" size="small">{{ row.value }}</el-tag>
                  <span v-else>{{ row.value }}</span>
                </template>
              </qt-table>
            </qt-wrap>
          </el-tab-pane>
          <el-tab-pane label="运行记录" name="runs">
            <run-records :task-id="task.id"
                         :task-name="task.name"
                         :active="activeTab === 'runs'"
                         @loaded="updateRunSummary"
            />
          </el-tab-pane>
          <el-tab-pane label="基础信息" name="basic">
            <descriptions-info class="basic-info"
                               :data="task"
                               :items="basicInfoItems"
                               :column="2"
                               :border="true"
            >
              <template #model>
                <div class="qt-table-info model-info task-detail-model">
                  <div class="qt-table-info__main">
                    <img :src="getModelIcon({ icon: task.modelIcon, type: task.modelType })" alt="" class="info-icon"/>
                    <span class="info-title text-ellipsis" :title="formatTaskModelTitle(task)">{{ formatTaskModelTitle(task) }}</span>
                    <dict-tag :options="MODEL_TYPE_OPTIONS" :value="task.modelType" size="small" type="primary"/>
                    <dict-tag
                        v-if="task.modelVersionNo"
                        :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: task.modelVersionNo }))"
                        :value="task.modelVersionStatus"
                        size="small"
                    />
                  </div>
                </div>
              </template>
              <template #source>
                <span class="task-detail-entity">
                  <img :src="getSourceIcon({ icon: task.sourceIcon })" alt=""/>
                  <span>{{ task.sourceName || '-' }}</span>
                </span>
              </template>
              <template #unique-fields>
                <div class="unique-fields">
                  <el-tag v-for="field in task.uniqueFields || []"
                          :key="field"
                          type="success"
                  >{{ field }}
                  </el-tag>
                  <span v-if="!task.uniqueFields?.length">-</span>
                </div>
              </template>
              <template #latest-result>
                <el-tag v-if="latestRun?.status" :type="RESULT_TYPES[latestRun.status] || 'info'">
                  {{ RESULT_LABELS[latestRun.status] || latestRun.status }}
                </el-tag>
                <span v-else>-</span>
              </template>
              <template #task-type>
                <dict-tag :icon="TASK_TYPE_ICONS[task.type]" :type="taskTypeTagType">
                  {{ getOptionLabel(TASK_TYPES, task.type) }}
                </dict-tag>
              </template>
              <template #remark>
                <span class="task-remark">{{ task.remark || '-' }}</span>
              </template>
            </descriptions-info>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<script setup>
import {Clock} from '@element-plus/icons-vue';
import {cronToZh} from '@/utils/cronUtils';

import {getModelIcon} from '@/utils/imageUrl';
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType';
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus';

import {computed, getCurrentInstance, reactive, ref, watch} from 'vue';

import {useRoute} from 'vue-router';

import DetailInfo from '@/components/DetailInfo/index.vue';
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import {getIngestTask} from '@/api/ingest/task/task';
import {getPublishedModelStructure} from '@/api/model/base/base';
import {getIngestSource} from '@/api/ingest/source/source';

import RunRecords from '@/views/ingest/task/detail/runRecords.vue';

import { getProjectIcon } from '@/utils/imageUrl';

import databaseIcon from '@/assets/modules/ingest/icons/task/database.svg';

import fileIcon from '@/assets/modules/ingest/icons/task/file.svg';

import apiIcon from '@/assets/modules/ingest/icons/task/api.svg';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const TASK_TYPE_ICONS = { DATABASE: databaseIcon, FILE: fileIcon, API: apiIcon };

const getOptionLabel = (options, value) =>
  options.find((item) => item.value === value)?.label || value || '-';

const TASK_TYPES = [
  { value: 'DATABASE', label: '数据库' },
  { value: 'FILE', label: '文件' },
  { value: 'API', label: 'API' },
];

const TASK_DETAIL_STATUS_OPTIONS = [
  { value: '1', label: '上线', elTagType: 'success' },
  { value: '0', label: '下线', elTagType: 'danger' },
];

const TRANSFORM_NONE = '0';

const TRANSFORM_TYPE = '1';

const TRANSFORM_TRIM = '2';

const TRANSFORM_NULL = '3';

const TRANSFORM_FIXED = '4';

const TRANSFORM_VALUE_MAP = '5';

const TRANSFORM_DATE = '6';

const TRANSFORM_OPTIONS = [
  { value: TRANSFORM_NONE, label: '不处理' },
  { value: TRANSFORM_TYPE, label: '类型转换' },
  { value: TRANSFORM_TRIM, label: '去除首尾空格' },
  { value: TRANSFORM_NULL, label: '空值处理' },
  { value: TRANSFORM_FIXED, label: '固定值' },
  { value: TRANSFORM_VALUE_MAP, label: '值映射' },
  { value: TRANSFORM_DATE, label: '日期格式转换' },
];

const RESULT_LABELS = { 1: '执行中', 2: '失败', 3: '失败', 4: '成功' };

const RESULT_TYPES = { 1: 'primary', 2: 'danger', 3: 'danger', 4: 'success' };

defineOptions({name: 'IngestTaskDetail'});
const {proxy} = getCurrentInstance();
const route = useRoute();
const loading = ref(false);
const fieldsLoading = ref(false);
const detailError = ref(false);
const task = ref({});
const mappingRows = computed(() => (task.value.mappings || []).map((row, index) => ({...row, rowNumber: index + 1})));
const source = ref({});
const modelFields = ref([]);
const activeTab = ref('mapping');
const latestRun = ref(null);
const runCount = ref(null);
let detailVersion = 0;
const trust = computed(() => source.value.trustScore == null ? '-' : `${source.value.trustScore}`);
const detailItems = computed(() => [
  {label: '来源系统', slot: 'source'},
  {label: '主数据模型', slot: 'model'},
  {label: '接入类型', slot: 'type'},
  {label: '描述', key: 'remark', span: 24, className: 'mt2 mb2'},
  {label: '调度周期', slot: 'schedule'},
  {
    label: '责任人',
    value: [task.value.responsiblePersonName, task.value.responsiblePersonPhone].filter(Boolean).join(' / ') || '-',
  },
  {label: '创建时间', value: task.value.createTime, formatter: formatTaskTime},
]);
const fetchSummary = computed(() => {
  if (task.value.dataFetch) return task.value.dataFetch;
  if (task.value.type === 'FILE') return task.value.fileConfig?.fileName || '-';
  if (task.value.type === 'API') return [task.value.apiConfig?.httpMethod, task.value.apiConfig?.url].filter(Boolean).join(' ') || '-';
  const config = task.value.dbConfig || {};
  const connection = source.value.datasourceList?.find(item => String(item.id) === String(config.datasourceId));
  return [connection?.datasourceName || '-', config.sourceObject || '-', task.value.ingestMode === 'INCREMENTAL' ? '增量更新' : '追加'].join(' · ');
});
const basicInfoItems = computed(() => [
  {label: '任务编号', key: 'code'},
  {label: '主数据模型', slot: 'model'},
  {label: '来源系统', slot: 'source'},
  {label: '接入类型', slot: 'task-type'},
  {label: '数据获取', value: fetchSummary.value},
  {label: '唯一标识', slot: 'unique-fields'},
  {
    label: '执行方式',
    value: task.value.triggerType === 'SCHEDULED' ? `定时执行 · ${task.value.cronExpression || '-'}` : '仅人工执行'
  },
  {label: '累计执行', value: runCount.value == null ? '-' : `${runCount.value} 次`},
  {label: '最近执行', value: formatTaskTime(latestRun.value?.startTime)},
  {label: '最近结果', slot: 'latest-result'},
  {label: '来源可信度', value: trust.value},
  {
    label: '负责人',
    value: [task.value.responsiblePersonName, task.value.responsiblePersonPhone]
        .filter(Boolean)
        .join(' / ') || '-',
  },
  {
    label: '创建人',
    value: [task.value.createBy, task.value.createPhoneNumber]
        .filter(Boolean)
        .join(' / ') || '-',
  },
  {label: '创建时间', value: formatTaskTime(task.value.createTime)},
  {label: '描述', slot: 'remark', span: 2},
]);
const taskTypeTagType = computed(() => ({
  DATABASE: 'primary',
  FILE: 'warning',
  API: 'success'
}[task.value.type] || 'info'));

function formatTaskModelTitle(model) {
  if (!model?.modelName) return '-';
  return model.modelCode ? `${model.modelName} (${model.modelCode})` : model.modelName;
}

const platformFields = computed(() => [
  {name: '来源记录 ID', method: '根据来源唯一标识或记录内容自动生成', value: '接入成功后生成'},
  {name: '来源系统', method: '由当前任务自动带入', value: task.value.sourceName || '-'},
  {name: '来源可信度', method: '从来源系统读取，供后续融合使用', value: trust.value},
  {name: '接入任务', method: '由当前任务自动带入', value: task.value.name || '-'},
  {name: '融合状态', method: '成功接入后自动初始化', value: '待融合', tag: true},
  {name: '平台接收时间', method: '数据成功接入时自动记录', value: '接入成功后记录'},
].map((row, index) => ({...row, rowNumber: index + 1})));
const mappingTableRef = ref();
const platformTableRef = ref();
const mappingTable = reactive({
  func: async (params) => ({data: sortLocalRows(mappingRows.value, params)}),
  config: {
    initResquest: false,
    fullContent: false,
    autoPagination: true,
    actions: {table: {search: false, refresh: true, columns: true}},
    table: {stripe: true},
    pagination: {pageSizes: [10, 20, 50, 100], pagerCount: 5, notAutoScroll: true},
  },
  columns: [
    {label: '编号', prop: 'rowNumber', slot: 'text', width: 80, minWidth: 80, align: 'center', sortable: 'custom'},
    {label: '来源字段', prop: 'sourceFieldName', slot: 'sourceFieldName', minWidth: 190, align: 'left'},
    {label: '示例值', prop: 'sourceExample', slot: 'example', minWidth: 150, align: 'left'},
    {label: '处理方式', prop: 'transformType', slot: 'transformType', minWidth: 180, align: 'left'},
    {
      label: '处理参数',
      prop: 'transformConfig',
      slot: 'transformConfig',
      minWidth: 180,
      align: 'left',
      showOverflowTooltip: true
    },
    {
      label: '模型字段',
      prop: 'targetFieldId',
      slot: 'targetFieldId',
      minWidth: 200,
      align: 'left',
      showOverflowTooltip: true
    },
    {label: '接入后示例', prop: 'targetExample', slot: 'example', minWidth: 170, align: 'left'},
  ],
});
const platformTable = reactive({
  func: async (params) => ({data: sortLocalRows(platformFields.value, params)}),
  config: {
    initResquest: false,
    fullContent: false,
    notPagination: true,
    actions: {table: {search: false, refresh: true, columns: true}},
    table: {stripe: true},
  },
  columns: [
    {label: '编号', prop: 'rowNumber', slot: 'text', width: 80, minWidth: 80, align: 'center', sortable: 'custom'},
    {label: '字段', prop: 'name', slot: 'text', minWidth: 180, align: 'left'},
    {label: '生成方式', prop: 'method', slot: 'text', minWidth: 280, align: 'left'},
    {label: '示例 / 初始值', prop: 'value', slot: 'value', minWidth: 220, align: 'left'},
  ],
});
watch([mappingRows, mappingTableRef], () => mappingTableRef.value?.resetQuery(), {flush: 'post'});
watch([platformFields, platformTableRef], () => platformTableRef.value?.getList(), {flush: 'post'});

/**
 * 按表格编号排序本地副本，保持任务原始映射顺序。
 * @param {Array<Object>} rows 本地记录。
 * @param {Object} params 表格排序参数。
 * @returns {Array<Object>} 排序后的展示记录。
 */
function sortLocalRows(rows, params) {
  if (params.orderByColumn !== 'rowNumber' || !params.isAsc) return rows;
  const direction = params.isAsc === 'descending' ? -1 : 1;
  return [...rows].sort((left, right) => (left.rowNumber - right.rowNumber) * direction);
}

/** 按字段 ID 回显目标模型字段名称与编码。 */
function getTargetFieldName(id) {
  const field = modelFields.value.find(item => String(item.id) === String(id));
  return field ? `${field.name || field.code} (${field.code})` : '-';
}

/** 将转换参数对象或 JSON 字符串整理为可读文本，解析失败时保留原文。 */
function formatTransformConfig(config) {
  if (!config || (typeof config === 'object' && Object.keys(config).length === 0)) return '-';
  let normalizedConfig = config;
  if (typeof config === 'string') {
    try {
      normalizedConfig = JSON.parse(config);
    } catch {
      return config;
    }
  }
  if (typeof normalizedConfig !== 'object') return String(normalizedConfig);
  return Object.entries(normalizedConfig)
      .map(([key, value]) => `${key}: ${typeof value === 'object' ? JSON.stringify(value) : value}`)
      .join('；') || '-';
}

/** 兼容时间对象及字符串，格式化失败时保留原始展示值。 */
function formatTaskTime(value) {
  if (!value) return '-';
  let normalized = value;
  if (typeof value === 'object') {
    normalized = value.time ?? value.value ?? value.timestamp ?? value.date;
  }
  if (!normalized) return '-';
  const date = normalized instanceof Date ? normalized : new Date(String(normalized).replace(/-/g, '/').replace('T', ' '));
  if (Number.isNaN(date.getTime())) return String(normalized);
  return proxy.parseTime(date, '{y}-{m}-{d} {h}:{i}') || '-';
}

/**
 * 并行加载来源系统和模型业务字段，单项失败不阻断另一项回显。
 * @param {number} version 当前详情请求版本。
 */
async function loadTaskFields(version) {
  const {modelId, sourceId} = task.value;
  const [sourceResult, modelFieldsResult] = await Promise.allSettled([
    sourceId != null && sourceId !== '' ? getIngestSource(sourceId) : Promise.resolve({data: {}}),
    getPublishedModelStructure(modelId),
  ]);
  if (version !== detailVersion) return;
  if (sourceResult.status === 'fulfilled') source.value = sourceResult.value.data || {};
  if (modelFieldsResult.status === 'fulfilled') {
    modelFields.value = (modelFieldsResult.value.data?.fields || []).filter(field => field.columnCategory === 'BUSINESS');
  }
}

/** 重新读取任务及关联字段，仅应用当前请求版本的结果。 */
async function refreshTaskFields() {
  if (fieldsLoading.value || task.value.id == null) return;
  const version = ++detailVersion;
  fieldsLoading.value = true;
  try {
    const response = await getIngestTask(task.value.id);
    if (version !== detailVersion) return;
    task.value = response.data || {};
    await loadTaskFields(version);
  } catch {
    return;
  } finally {
    if (version === detailVersion) fieldsLoading.value = false;
  }
}

/** 加载路由指定任务，主体返回后再补充来源、模型字段及运行概况。 */
async function loadDetail() {
  if (route.path !== '/ingest/task/detail') return;
  const version = ++detailVersion;
  fieldsLoading.value = false;
  task.value = {};
  source.value = {};
  modelFields.value = [];
  latestRun.value = null;
  runCount.value = null;
  activeTab.value = route.query.tab === 'runs' ? 'runs' : 'mapping';
  detailError.value = false;
  loading.value = true;
  try {
    if (!route.query.id) throw new Error('缺少任务 ID');
    const response = await getIngestTask(route.query.id);
    if (version !== detailVersion) return;
    task.value = response.data || {};
    // 任务主体返回后立即解除整页遮罩，来源、模型字段和运行记录属于附属信息，分别加载。
    loading.value = false;
    await loadTaskFields(version);
  } catch {
    if (version === detailVersion) detailError.value = true;
  } finally {
    if (version === detailVersion) loading.value = false;
  }
}

/**
 * 接收运行记录组件的加载结果，更新执行次数与最近一次运行。
 * @param {Object} summary 包含 total 和 latestRun 的运行概况。
 */
function updateRunSummary({total, latestRun: latest}) {
  runCount.value = total;
  if (latest !== undefined) latestRun.value = latest;
}

watch(() => [route.path, route.query.id], loadDetail, {immediate: true});
</script>

<style scoped lang="scss">
/* 来源与任务详情共用的展示规则；固定描述表列宽，避免长内容挤压相邻字段。 */
.basic-info :deep(.el-descriptions__table),
.source-business-fields :deep(.el-descriptions__table) {
  width: 100%;
  table-layout: fixed;
}

/* 保留任务描述的换行，长连续文本可在容器内折行。 */
.task-remark {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* 来源系统和主数据模型以图标配合名称展示，不使用标签底色。 */
.task-detail-entity {
  display: inline-flex;
  align-items: center;
  gap: var(--qt-space-2);

  img {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
  }
}

/* 唯一标识较多时允许标签换行，保持各项间距一致。 */
.unique-fields {
  display: flex;
  flex-wrap: wrap;
  gap: var(--qt-space-2);
  padding: var(--qt-space-2) 0;
}

/* 让详情标题内容可收缩，并将操作按钮排在信息项之后。 */
:deep(.infotop-title.clearfixs) {
  .task-item {
    flex: 1;
    min-width: 0;
  }

  .btn-style {
    order: 3;
  }
}

/* 来源系统描述保留用户输入的空格和换行。 */
.source-basic-info :deep(.base-content) {
  white-space: pre-wrap;
}
</style>
