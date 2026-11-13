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
  来源系统详情：展示基本信息、关联数据连接及引用该来源的接入任务。
-->
<template>
  <div class="app-container" v-loading="loading">
    <detail-info
        class="source-detail-info"
        :gutter="2"
        :show="true"
        :data="detail"
        mode="free"
        time-format="{y}-{m}-{d} {h}:{i}"
        :header="{
        className: 'clearfixs',
        idKey: 'id',
        nameKey: 'name',
        statusKey: 'validFlag',
        statusOptions: SOURCE_STATUS_OPTIONS,
      }"
        :items="detailItems"
    />
    <div class="pagecont-bottom">
      <el-tabs v-model="activeName" class="demo-tabs">
        <qt-tab-pane label="数据连接" name="datasource">
          <qt-wrap :columns="datasourceTable.columns" :tableRef="datasourceTableRef"
                   :config="{ fullContent: false, actions: { table: { search: false } } }">
            <template #actions-data>
              <guide-tip-panel
                  type="plain"
                  title="查看当前来源系统已关联的数据连接，核对数据库类型、连接地址及端口等配置信息，确认任务读取数据所使用的连接"
              />
            </template>
            <qt-table ref="datasourceTableRef" v-bind="datasourceTable" :columns="datasourceTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #dbName="{ row }">{{ formatDbName(row) }}</template>
              <template #ip="{ row }">{{ row.ip || "-" }}</template>
              <template #port="{ row }">{{ row.port || "-" }}</template>
              <template #connectionId="{ row }">{{ row.id }}</template>
            </qt-table>
          </qt-wrap>
        </qt-tab-pane>
        <qt-tab-pane label="主数据模型" name="model">
          <qt-wrap :columns="modelTable.columns" :table-ref="modelTableRef" :config="relatedWrapConfig">
            <template #actions-data>
              <guide-tip-panel
                  type="plain"
                  title="查看当前来源系统关联的主数据模型及其版本、状态和所属目录，了解该来源数据接入后对应的目标模型范围"
              />
            </template>
            <qt-table ref="modelTableRef" v-bind="modelTable" :columns="modelTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #modelInfo="{ row }">
                <!-- 标题自己处理省略号，悬浮全文交给本列的 showOverflowTooltip，避免出现两层提示。 -->
                <div class="qt-table-info model-info">
                  <div class="qt-table-info__main">
                    <img :src="getModelIcon(row)" alt="" class="info-icon"/>
                    <span class="info-title text-ellipsis">{{ row.name ? `${row.name} (${row.code})` : '-' }}</span>
                    <dict-tag :options="MODEL_TYPE_OPTIONS" :value="row.type" size="small" type="primary"/>
                    <dict-tag
                        v-if="row.releaseVersion?.versionNo || row.version?.versionNo"
                        class="ml4"
                        :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: row.releaseVersion?.versionNo || row.version?.versionNo }))"
                        :value="row.version?.status"
                        size="small"
                    />
                  </div>
                  <div class="info-desc text-ellipsis">{{ row.description || '-' }}</div>
                </div>
              </template>
              <template #modelResponsible="{ row }">
                <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
                <div class="flex-column overflow-hidden">
                  <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)">{{
                      getResponsibleName(row.responsibleUserId) || '-'
                    }}</span>
                  <span v-if="getResponsiblePhone(row.responsibleUserId)"
                        class="desc-text">{{ getResponsiblePhone(row.responsibleUserId) }}</span>
                </div>
              </template>
              <template #modelCreator="{ row }">
                <div class="flex-column overflow-hidden">
                  <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)">{{
                      row.createBy || getResponsibleName(row.creatorId) || '-'
                    }}</span>
                  <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text">{{
                      getResponsiblePhone(row.creatorId)
                    }}</span>
                </div>
              </template>
              <template #layers="{ row }">
                <!-- 主数据域单独展示；layers 全部是目录，不能把第一项目录当成域或从路径里删掉。 -->
                <div v-if="row.projectName || row.layers?.length" class="flex-column flex-column-gap">
                  <div class="qt-table-info__main">
                    <img :src="getProjectIcon(row.projectIcon)" alt=""/>
                    <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
                  </div>
                  <div v-if="row.layers?.length" class="text-ellipsis flex-center">
                    <dict-tag size="small"
                              type="primary"
                              :value="row.layers[row.layers.length - 1].name"
                    />
                  </div>
                </div>
                <span v-else class="text-muted">-</span>
              </template>
              <template #modelStatus="{ row }">
                <dict-tag :options="MODEL_STATUS_OPTIONS" :value="row.version?.status"/>
              </template>
            </qt-table>
          </qt-wrap>
        </qt-tab-pane>
        <qt-tab-pane label="接入任务" name="task">
          <qt-wrap :columns="taskTable.columns" :tableRef="taskTableRef"
                   :config="{ fullContent: false, actions: { table: { search: false } } }">
            <template #actions-data>
              <guide-tip-panel
                  type="plain"
                  title="查看引用当前来源系统的接入任务，了解各任务关联的主数据模型、任务类型与运行状态，便于追踪来源数据的接入安排"
              />
            </template>
            <qt-table ref="taskTableRef" v-bind="taskTable" :columns="taskTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #taskType="{ row }">
                <dict-tag
                    :icon="getTaskTypeIcon(row.type)"
                    :type="getTaskTypeTagType(row.type)"
                >
                  {{ formatTaskType(row.type) }}
                </dict-tag>
              </template>
              <template #modelName="{ row }">
                <div class="qt-table-info model-info">
                  <div class="qt-table-info__main">
                    <img :src="getModelIcon(getTaskModel(row))" alt="" class="info-icon"/>
                    <span class="info-title text-ellipsis" :title="formatTaskModelTitle(row)">
                      {{ formatTaskModelTitle(row) }}
                    </span>
                    <dict-tag :options="MODEL_TYPE_OPTIONS" :value="getTaskModel(row).type" size="small" type="primary"/>
                    <dict-tag
                        v-if="getTaskModelVersionNo(row)"
                        class="ml4"
                        :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: getTaskModelVersionNo(row) }))"
                        :value="getTaskModel(row).versionStatus ?? getTaskModel(row).version?.status"
                        size="small"
                    />
                  </div>
                </div>
              </template>
              <template #taskStatus="{ row }">
                <el-tag :type="getTaskStatusTagType(row.status)">
                  {{ formatTaskStatus(row.status) }}
                </el-tag>
              </template>
              <template #taskCode="{ row }">{{ row.code || "-" }}</template>
            </qt-table>
          </qt-wrap>
        </qt-tab-pane>
        <qt-tab-pane label="重复检测" name="detect" lazy>
          <qt-wrap :columns="detectTable.columns" :table-ref="detectTableRef" :config="relatedWrapConfig">
            <template #actions-data>
              <guide-tip-panel
                  type="plain"
                  title="查看当前来源系统关联的重复检测任务及其配置状态，进入任务详情可核对检测规则和执行情况，掌握重复数据处理进度"
              />
            </template>
            <qt-table v-if="detail.id" :key="detail.id" ref="detectTableRef" v-bind="detectTable" :columns="detectTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
              <template #handle="{ row }">
                <el-button v-hasPermi="['merge:detect:query']" link type="primary" icon="View"
                           @click="router.push({ path: '/merge/detect/detail', query: { id: row.id, taskNo: row.taskNo } })">
                  详情
                </el-button>
              </template>
            </qt-table>
          </qt-wrap>
        </qt-tab-pane>
        <qt-tab-pane label="基础信息" name="basic">
          <descriptions-info
              class="basicInfo source-basic-info"
              :data="detail"
              :items="basicInfoItems"
              :column="2"
              time-format="{y}-{m}-{d} {h}:{i}:{s}"
          >
            <template #sourceName="{ data }">
              <div class="source-basic-source-info flex-center">
                <img :src="getSourceIcon({ icon: data.icon })" class="info-icon mr5" alt=""/>
                <span class="table-primary-text text-ellipsis" :title="data.name || '-'">
                  {{ data.name || '-' }}
                </span>
              </div>
            </template>
            <template #models="{ data }">
              <div v-if="getReferencedModels(data).length" class="source-basic-models">
                <div v-for="task in getReferencedModels(data)" :key="task.modelId || task.modelName"
                     class="qt-table-info model-info source-basic-model-info">
                  <div class="qt-table-info__main">
                    <img :src="getModelIcon(getTaskModel(task))" alt="" class="info-icon"/>
                    <span class="info-title text-ellipsis" :title="formatTaskModelTitle(task)">
                      {{ formatTaskModelTitle(task) }}
                    </span>
                    <dict-tag :options="MODEL_TYPE_OPTIONS" :value="getTaskModel(task).type" size="small" type="primary"/>
                    <dict-tag
                        v-if="getTaskModelVersionNo(task)"
                        :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: getTaskModelVersionNo(task) }))"
                        :value="getTaskModel(task).versionStatus ?? getTaskModel(task).version?.status"
                        size="small"
                    />
                  </div>
                </div>
              </div>
              <span v-else>-</span>
            </template>
          </descriptions-info>
        </qt-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="IngestSourceDetail">
import {computed, nextTick, onActivated, reactive, ref, toRefs, watch} from "vue";

import {useRoute, useRouter} from "vue-router";

import {getIngestSource} from "@/api/ingest/source/source.js";
import ingestTaskIcon from '@/assets/modules/ingest/icons/task/ingest-task.svg';
import DetailInfo from "@/components/DetailInfo/index.vue";
import DescriptionsInfo from "@/components/DescriptionsInfo/index.vue";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType';
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus';
import {getModelIcon} from '@/utils/imageUrl';
import {getProjectIcon} from '@/utils/imageUrl';
import {listRelatedModels} from '@/api/model/base/base';
import { useUserDirectory } from '@/store/system/userDirectory';
import {listMergeTask} from '@/api/merge/detect';

import databaseIcon from '@/assets/modules/ingest/icons/task/database.svg';

import fileIcon from '@/assets/modules/ingest/icons/task/file.svg';

import apiIcon from '@/assets/modules/ingest/icons/task/api.svg';

const TASK_TYPE_ICONS = { DATABASE: databaseIcon, FILE: fileIcon, API: apiIcon };

const TASK_STATUS_MAP = {
  1: "上线",
  0: "下线",
};

const TASK_TYPE_MAP = {
  DB: "数据库",
  DATABASE: "数据库",
  API: "API",
  FILE: "文件",
};

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const SOURCE_STATUS_OPTIONS = [
  { label: '启用', value: '1', elTagType: 'success' },
  { label: '停用', value: '0', elTagType: 'info' },
];

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const activeName = ref("datasource");
const datasourceTableRef = ref(null);
const taskTableRef = ref(null);

const data = reactive({
  detail: {},
});
const {detail} = toRefs(data);

const datasourceTable = reactive({
  func: () => Promise.resolve({data: detail.value.datasourceList || []}),
  config: {
    initResquest: false,
    autoPagination: true,
    table: {rowKey: "id", stripe: true, emptyText: "暂无关联数据连接"}
  },
  columns: [
    {label: "编号", prop: "id", width: 60, minWidth: 60, align: "center", sortable: true},
    {label: "连接名称", prop: "datasourceName", align: "left", minWidth: 200, showOverflowTooltip: true},
    {label: "数据库类型", prop: "datasourceType", minWidth: 140, align: "left"},
    {label: "数据库", prop: "dbName", slot: "dbName", minWidth: 160, align: "left", showOverflowTooltip: true},
    {label: "IP地址", prop: "ip", slot: "ip", minWidth: 180, align: "left", showOverflowTooltip: true},
    {label: "端口号", prop: "port", slot: "port", minWidth: 100, align: "left"},
    //{ label: "连接标识", prop: "connectionId", slot: "connectionId", minWidth: 140 },
  ],
});

const taskTable = reactive({
  func: () => Promise.resolve({data: detail.value.taskList || []}),
  config: {
    initResquest: false,
    autoPagination: true,
    table: {rowKey: "id", stripe: true, emptyText: "暂无接入任务引用"}
  },
  columns: [
    {label: "编号", prop: "id", width: 60, minWidth: 60, align: "center", sortable: true},
    {
      label: "任务名称",
      prop: "name",
      minWidth: 220,
      align: "left",
      info: {
        icon: () => ingestTaskIcon,
        title: row => row.name ? `${row.name}${row.code ? ` (${row.code})` : ''}` : '-',
        desc: 'remark',
      }
    },
    {label: "接入类型", prop: "type", slot: "taskType", minWidth: 120},
    {
      label: "主数据模型",
      prop: "modelName",
      slot: "modelName",
      minWidth: 260,
      align: "left",
      showOverflowTooltip: true
    },
    {label: "状态", prop: "status", slot: "taskStatus", minWidth: 120},
    {label: "最近执行时间", prop: "lastRunTime", date: "{y}-{m}-{d} {h}:{i}", minWidth: 180},
    {label: "任务编码", prop: "code", slot: "taskCode", minWidth: 160, showOverflowTooltip: true},
  ],
});

const modelTableRef = ref(null);
const detectTableRef = ref(null);
const relatedWrapConfig = {fullContent: false, actions: {table: {search: false}}};
const modelTable = reactive({
  func: loadSourceModels,
  config: {
    initResquest: false,
    autoPagination: true,
    sort: true,
    table: {rowKey: 'id', stripe: true, defaultSort: {prop: 'createTime', order: 'descending'}},
  },
  columns: [
    {label: '编号', prop: 'id', width: 60, minWidth: 60, align: 'center', sortable: true},
    {label: '模型信息', minWidth: 380, align: 'left', slot: 'modelInfo', showOverflowTooltip: {effect: 'light'}},
    {
      label: '归属位置',
      prop: 'layers',
      align: 'left',
      minWidth: 200,
      slot: 'layers',
      showOverflowTooltip: {effect: 'light'}
    },
    {label: '状态', prop: 'version', slot: 'modelStatus', minWidth: 140},
    {label: '负责人', prop: 'responsibleUserId', slot: 'modelResponsible', align: 'left', minWidth: 120},
    {label: '创建人', prop: 'creatorId', slot: 'modelCreator', align: 'left', minWidth: 120},
    {label: '创建时间', prop: 'createTime', minWidth: 150, sortable: true, sortableKey: 'create_time', date: true},
  ],
});

/** 复用模型定义查询，范围限定为当前来源系统关联的模型。 */
function loadSourceModels(params) {
  const modelIds = [...new Set((detail.value.modelList || []).map(model => model.modelId).filter(id => id != null))];
  if (!modelIds.length) return Promise.resolve({data: []});
  return listRelatedModels({
    ...params,
    ids: modelIds.join(','),
    id: modelIds.length === 1 ? modelIds[0] : undefined
  }).then(response => {
    const allowedIds = new Set(modelIds.map(String));
    return {...response, data: (response.data || []).filter(model => allowedIds.has(String(model.id)))};
  });
}

const detectTable = reactive({
  func: params => listMergeTask({...params, sourceSystemId: detail.value.id}),
  config: {
    sort: true,
    table: {defaultSort: {prop: 'create_time', order: 'descending'}},
  },
  columns: [
    {label: '编号', prop: 'id', width: 60, minWidth: 60, sortable: true, align: 'center'},
    {label: '任务编号', prop: 'taskNo', minWidth: 180, align: 'left'},
    {
      label: '主数据模型',
      prop: 'modelId',
      minWidth: 180,
      align: 'left',
      formatter: row => detail.value.modelList?.find(model => String(model.modelId) === String(row.modelId))?.modelName || '-'
    },
    {label: '创建人', prop: 'createBy', minWidth: 130, align: 'left'},
    {label: '创建时间', prop: 'createTime', minWidth: 170, sortable: true, sortableKey: 'create_time', date: true, align: 'center'},
    {label: '操作', slot: 'handle', width: 100, minWidth: 100, align: 'left'},
  ],
});

const detailItems = computed(() => [
  {label: "来源系统编码", key: "code"},
  {label: "来源可信度", formatter: formatTrustScore},
  {
    label: "负责人",
    formatter: (_, row) => [row.responsiblePersonName, row.responsiblePersonPhone].filter(Boolean).join(" / ")
  },
  {label: "描述", key: "description", span: 24, ellipsisClass: "ellipsis", className: "mt2 mb2"},
  {label: "创建人", formatter: (_, row) => [row.createBy, row.creatorPhone].filter(Boolean).join(" / ")},
  {label: "创建时间", key: "createTime", type: "time"},
  {label: "修改时间", key: "updateTime", type: "time"},
]);

const basicInfoItems = computed(() => [
  {label: "状态", key: "validFlag", dictOptions: SOURCE_STATUS_OPTIONS},
  {label: "来源系统名称", key: "name", slot: "sourceName"},
  {label: "来源系统编码", key: "code"},
  {label: "来源可信度", formatter: formatTrustScore},
  {label: "接入任务引用", formatter: formatTaskCount},
  {
    label: "主数据模型",
    slot: "models",
  },
  {
    label: "关联数据库连接",
    formatter: (_, row) =>
        row?.datasourceList && row.datasourceList.length
            ? `${row.datasourceList.length} 个`
            : "未关联",
  },
  {
    label: "负责人",
    formatter: (_, row) =>
        [row.responsiblePersonName, row.responsiblePersonPhone]
            .filter(Boolean)
            .join(" / "),
  },
  {
    label: "创建人",
    formatter: (_, row) =>
        [row.createBy, row.creatorPhone]
            .filter(Boolean)
            .join(" / "),
  },
  {label: "创建时间", key: "createTime", type: "time"},
  {label: "修改时间", key: "updateTime", type: "time"},
  {label: "描述", key: "description", span: 2},
]);

/** 将来源可信度格式化为百分制文本，缺省分值沿用业务默认值。 */
function formatTrustScore(_, row) {
  return `${row?.trustScore ?? 50}`;
}

/** 展示接入任务引用数量，无引用时显示说明文本。 */
function formatTaskCount(_, row) {
  return row?.taskCount ? `${row.taskCount} 个任务` : "无引用";
}

/**
 * 兼容对象或 JSON 字符串配置，空值与解析失败时返回空对象。
 * @param {Object|string} config 数据连接配置。
 * @returns {Object} 可供展示读取的配置。
 */
function parseDatasourceConfig(config) {
  if (!config) {
    return {};
  }
  if (typeof config === "object") {
    return config;
  }
  try {
    return JSON.parse(config) || {};
  } catch {
    return {};
  }
}

/**
 * 从连接配置中依次读取数据库名、模式或项目名。
 * @param {Object} row 数据连接记录。
 * @returns {string} 展示名称或占位符。
 */
function formatDbName(row) {
  const config = parseDatasourceConfig(row?.datasourceConfig);
  return config.dbname || config.sid || config.projectName || config.project_name || "-";
}

/** 通过接入任务公共映射回显类型，未知类型保留原值。 */
function formatTaskType(type) {
  if (!type) return "-";
  return TASK_TYPE_MAP[type] || type;
}

/** 将历史数据库类型映射到统一的类型图标键。 */
function getTaskTypeIcon(type) {
  return TASK_TYPE_ICONS[type === 'DB' ? 'DATABASE' : type];
}

/** 按接入类型选择标签颜色，未知类型使用默认信息色。 */
function getTaskTypeTagType(type) {
  return ({
    DB: 'primary',
    DATABASE: 'primary',
    FILE: 'warning',
    API: 'success',
  })[type] || 'info';
}

/** 从当前来源详情的模型清单中读取任务对应的模型展示信息。 */
function getTaskModel(task) {
  return detail.value.modelList?.find(model => String(model.modelId) === String(task.modelId)) || {
    modelName: task.modelName,
    name: task.modelName,
  };
}

/** 优先展示成功发布版本，关联数据未带发布版本时回退当前版本号。 */
function getTaskModelVersionNo(task) {
  const model = getTaskModel(task);
  return model.releaseVersion?.versionNo || model.versionNo || model.version?.versionNo;
}

/** 模型名称后附编码，编码缺失时保留可用名称。 */
function formatTaskModelTitle(task) {
  const model = getTaskModel(task);
  const name = model.name || model.modelName || task.modelName;
  if (!name) return '-';
  const code = model.code || model.modelCode;
  return code ? `${name} (${code})` : name;
}

/** 从来源系统引用的接入任务中提取去重后的目标模型。 */
function getReferencedModels(source) {
  const models = new Map();
  (source?.taskList || []).forEach(task => {
    const name = task.modelName || getTaskModel(task).modelName;
    if (!name) return;
    const key = task.modelId != null ? String(task.modelId) : name;
    if (!models.has(key)) models.set(key, task);
  });
  return [...models.values()];
}

/** 保留原状态文案，并为已知启停状态提供标签颜色。 */
function formatTaskStatus(status) {
  if (status == null || status === '') return '-';
  return TASK_STATUS_MAP[status] || status;
}

function getTaskStatusTagType(status) {
  return ({1: 'success', 0: 'info'})[status] || 'info';
}

/** 仅在来源详情路由加载当前记录，并刷新关联连接与任务表格。 */
function loadDetail() {
  if (route.path !== "/ingest/source/detail") {
    return;
  }
  const id = route.query.id;
  if (!id) {
    return;
  }
  loading.value = true;
  getIngestSource(id)
      .then(async (response) => {
        detail.value = {...response.data, validFlag: String(response.data?.validFlag ?? "1")};
        await nextTick();
        datasourceTableRef.value?.getList();
        taskTableRef.value?.getList();
        modelTableRef.value?.getList();
      })
      .finally(() => {
        loading.value = false;
      });
}

watch(
    () => [route.path, route.query.id],
    () => {
      loadDetail();
    },
    {immediate: true}
);

onActivated(() => {
  activeName.value = "datasource";
  loadDetail();
});
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

.source-basic-models {
  display: flex;
  flex-wrap: wrap;
  gap: var(--qt-space-3);
}

.table-primary-text {
  color: var(--el-text-color-primary);
  font-weight: var(--qt-font-weight-medium);
}

.source-basic-source-info .info-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

</style>
