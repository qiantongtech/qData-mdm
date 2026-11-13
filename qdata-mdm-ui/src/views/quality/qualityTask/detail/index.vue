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
  <div class="app-container quality-task-detail" ref="app-container">
    <div v-loading="loading" class="detail-summary">
      <DetailInfo
        v-if="task.id"
        :data="task"
        mode="free"
        :header="{
          className: 'clearfixs',
          idKey: 'taskCode',
          nameKey: 'taskName',
          statusKey: 'status',
          statusOptions: qualityTaskStatusOptions,
          routerBack: false,
        }"
        :items="detailItems"
        time-format="{y}-{m}-{d} {h}:{i}:{s}"
        @back="goBack"
      >
        <template #model-info="{ item }">
          <div v-if="modelInfo.name || modelInfo.code || modelVersionNo" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(modelInfo)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="item.value">{{ item.value }}</span>
              <dict-tag
                v-if="modelInfo.type"
                :options="MODEL_TYPE_OPTIONS"
                :value="modelInfo.type"
                size="small"
                type="primary"
              />
              <dict-tag
                v-if="modelVersionNo"
                class="ml4"
                :options="modelVersionTagOptions"
                :value="modelVersionNo"
                size="small"
              />
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template #location="{ item }">
          <div v-if="item.value" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getProjectIcon(modelInfo.projectIcon)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="item.value">{{ item.value }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template #responsiblePerson="{data}">
          <span>{{ [getResponsibleName(data.responsibleUserId) || '-', getResponsiblePhone(data.responsibleUserId)].filter(Boolean).join(' / ') }}</span>
        </template>
        <template #data-scope="{data}">
          <el-tag v-if="['1', '2'].includes(String(data.dataScopeType))" type="primary" effect="light" class="summary-scope-tag">
            {{ String(data.dataScopeType) === '2' ? '条件筛选数据' : '全部数据' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </DetailInfo>
    </div>

    <el-empty v-if="!loading && !task.id" description="质量任务不存在或已删除" />

    <div v-if="task.id" class="pagecont-bottom">
      <el-tabs v-model="activeTab"    class="detail-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="质量规则" name="rules">
          <div class="detail-tab-body">
            <el-alert v-if="rulesError" title="质量规则加载失败，请重试" type="error" :closable="false">
              <el-button link type="primary" @click="loadRules()">重新加载</el-button>
            </el-alert>
            <qt-wrap
              :columns="rulesTableStore.columns"
              :table-ref="rulesTableRef"
              :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <el-button  plain type="primary" icon="Edit"   @click="goQualityRules">前往质量规则维护</el-button>
              </template>

              <qt-table v-bind="rulesTableStore" ref="rulesTableRef">
                <template #ruleType="{ row }">{{ ruleTypeLabel(row.ruleType) }}</template>
                <template #modelFieldId="{ row }">{{ fieldLabel(row) }}</template>
                <template #ruleStatus>
                  <el-tag type="success" effect="plain">启用</el-tag>
                </template>
                <template #empty>
                  <span class="muted">{{ task.modelId ? '该模型暂无可执行质量规则' : '暂无质量规则' }}</span>
                </template>
              </qt-table>
            </qt-wrap>
          </div>
        </el-tab-pane>

        <el-tab-pane label="运行记录" name="history">

          <div >
            <qt-wrap
              :columns="historyTableStore.columns"
              :table-ref="historyTableRef"
              :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <GuideTipPanel type="plain" title="查看当前质量任务的运行记录、检测统计与执行结果" />
              </template>

              <qt-table  v-bind="historyTableStore" ref="historyTableRef">
                <template #triggerType="{ row }">{{ triggerLabel(row.triggerType) }}</template>
                <template #status="{ row }">
                  <el-tag :type="resultType(row.status)" effect="plain">{{ resultStatusLabel(row.status) }}</el-tag>
                </template>
                <template #action="{ row }">
                  <el-button link type="primary" icon="View"  @click="showResult(row)">查看结果</el-button>
                </template>
                <template #empty><span class="muted">暂无运行记录</span></template>
              </qt-table>
            </qt-wrap>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <RunHistoryDialog ref="runHistoryRef" />
  </div>
</template>

<script setup name="QualitytaskDetail">
import {computed, nextTick, onUnmounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import {getQualitytask} from '@/api/quality/qualityTask/qualitytask';
import {getBase} from '@/api/model/base/base';
import {checkPermi} from '@/utils/permission';
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType';
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus';
import { useUserDirectory } from '@/store/system/userDirectory';
import {listQualitytaskinstance} from '@/api/quality/qualityTaskInstance/qualitytaskinstance';
import {listRulequality} from '@/api/rule/quality/quality';
import RunHistoryDialog from '@/views/quality/qualityTaskInstance/RunHistoryDialog.vue';
import {cronToZh} from '@/utils/cronUtils';

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const route = useRoute();
const router = useRouter();
const task = ref({});
const modelInfo = ref({});
const activeTab = ref('rules');
const loading = ref(false);
const rulesError = ref(false);
const recordsError = ref(false);
const rulesTableRef = ref();
const historyTableRef = ref();
const runHistoryRef = ref();
let detailRequest = 0;
let rulesRequest = 0;

const fieldLabels = {
  101: {201: '供应商名称', 202: '统一社会信用代码', 203: '供应商类型', 204: '供应商等级', 205: '注册地址', 206: '联系人', 207: '联系电话', 208: '注册日期', 209: '年度采购额'},
  102: {301: '客户名称', 302: '统一社会信用代码', 303: '客户类型', 304: '客户等级', 305: '注册地址', 306: '联系人', 307: '联系电话', 308: '注册日期', 309: '年度销售额'},
  103: {401: '物料编码', 402: '物料名称', 403: '物料类型', 404: '物料规格', 405: '计量单位', 406: '物料等级', 407: '生产厂家', 408: '上市日期', 409: '标准成本'}
};
const ruleTypeOptions = {
  NOT_EMPTY: '非空', FORMAT: '格式', LENGTH: '长度校验', NUMBER_RANGE: '数值范围',
  REF_DATA_VALID: '参考数据合法性', UNIQUE: '唯一性字段组合', FIELD_LOGIC: '字段间逻辑比较'
};
const qualityTaskStatusOptions = [
  {label: '启用', value: '1', elTagType: 'success'},
  {label: '停用', value: '2', elTagType: 'danger'},
];
const modelTitle = computed(() => {
  const {name, code} = modelInfo.value;
  return name ? (code ? `${name} (${code})` : name) : code || '-';
});
const modelVersionNo = computed(() => {
  const {releaseVersion, version} = modelInfo.value;
  return releaseVersion?.versionNo || version?.versionNo || task.value.modelVersion;
});
const modelVersionTagOptions = computed(() => {
  const status = modelInfo.value.version?.status;
  const option = MODEL_STATUS_OPTIONS.find(item => String(item.value) === String(status));
  return [{value: modelVersionNo.value, label: modelVersionNo.value, elTagType: option?.elTagType || 'info'}];
});
// 与“我的申请详情”保持一致：主数据域 / 最末级目录。
const modelLocation = computed(() => [
  modelInfo.value.projectName,
  modelInfo.value.layers?.at(-1)?.name
].filter(Boolean).join(' / '));
const detailItems = computed(() => [
  {label: '主数据模型', value: modelTitle.value, slot: 'model-info'},
  {label: '归属位置', value: modelLocation.value, slot: 'location'},
  {label: '检测范围', slot: 'data-scope'},
  {
    label: '描述',
    key: 'remark',
    span: 24,
    ellipsisClass: 'ellipsis',
  },
  {label: '调度周期', value: scheduleText()},
  {label: '负责人', key: 'responsibleUserId', slot: 'responsiblePerson'},
  {label: '最近执行时间', key: 'lastRunTime', type: 'time'},
]);

const rulesTableStore = reactive({
  config: {
    table: {stripe: true, rowKey: 'id', maxHeight: 420}
  },
  columns: [
    {label: '编号', prop: 'id', width: 60, sortable: true,align: "left"},
    {label: '规则编码', prop: 'ruleCode', minWidth: 160, showOverflowTooltip: true,align: "left"},
    {label: '规则名称', prop: 'ruleName', minWidth: 220, showOverflowTooltip: true,align: "left"},
    {label: '规则类型', prop: 'ruleType', slot: 'ruleType', minWidth: 150,align: "left"},
    {label: '校验字段', prop: 'modelFieldId', slot: 'modelFieldId', minWidth: 180, showOverflowTooltip: true,align: "left"},
    {label: '状态', prop: 'validFlag', slot: 'ruleStatus', width: 90, align: 'center', headerAlign: 'center'}
  ],
  func: fetchRuleRows,
  params: {}
});

const historyTableStore = reactive({
  config: {
    table: {stripe: true, rowKey: 'id'}
  },
  columns: [
    {label: '编号', prop: 'id', width: 60, sortable: true,align: "left"},
    {label: '执行时间', prop: 'startTime', date: '{y}-{m}-{d} {h}:{i}', minWidth: 170,align: "left"},
    {label: '执行方式', prop: 'triggerType', slot: 'triggerType', width: 100,align: "left"},
    {label: '检测数量', prop: 'totalCount', minWidth: 110},
    {label: '通过数量', prop: 'passCount', minWidth: 110},
    {label: '问题数量', prop: 'failCount', minWidth: 110},
    {label: '执行结果', prop: 'status', slot: 'status', minWidth: 110,align: "left"},
    {label: '操作', slot: 'action', width: 110}
  ],
  func: fetchRecordRows,
  params: {}
});

watch(() => route.query.id, (id) => {
  if (id) loadDetail(id);
}, {immediate: true});
onUnmounted(() => { detailRequest++; rulesRequest++; });

async function loadDetail(id = route.query.id) {
  if (!id) return;
  const request = ++detailRequest;
  loading.value = true;
  modelInfo.value = {};
  try {
    const {data} = await getQualitytask(id);
    if (request !== detailRequest) return;
    task.value = {...(data || {}), taskName: data?.taskName || '质量任务详情'};
    activeTab.value = 'rules';
    await nextTick();
    loadRules(true);
    loadRecords();
    if (data?.modelId && checkPermi(['model:base:query'])) {
      try {
        const modelResponse = await getBase(data.modelId);
        if (request !== detailRequest) return;
        modelInfo.value = modelResponse.data || {};
      } catch {
        // 模型查询失败时保留任务信息；错误提示由公共请求工具处理。
      }
    }
  } finally {
    if (request === detailRequest) loading.value = false;
  }
}

function loadRules(resetPage = false) {
  rulesError.value = false;
  if (resetPage) {
    rulesTableRef.value?.resetQuery();
  } else {
    rulesTableRef.value?.getList();
  }
}

async function fetchRuleRows(params) {
  const request = ++rulesRequest;
  const modelId = task.value.modelId;
  if (!modelId) return {data: {rows: [], total: 0}};
  try {
    const response = await listRulequality({...params, modelId, validFlag: true, abnormalFlag: '0'});
    if (request !== rulesRequest) return {data: {rows: [], total: 0}};
    rulesError.value = false;
    return response;
  } catch {
    if (request === rulesRequest) {
      rulesError.value = true;
    }
    return {data: {rows: [], total: 0}};
  }
}

async function fetchRecordRows(params) {
  const taskId = task.value.id;
  if (!taskId) return {data: {rows: [], total: 0}};
  try {
    return await listQualitytaskinstance({
      ...params,
      qualityTaskId: taskId,
      orderByColumn: 'id',
      isAsc: 'descending'
    });
  } catch {
    recordsError.value = true;
    return {data: {rows: [], total: 0}};
  }
}

function loadRecords() {
  if (!task.value.id) return;
  recordsError.value = false;
  historyTableRef.value?.getList();
}

function goBack() {
  router.back();
}

function handleTabChange(name) {
  if (name === 'history') loadRecords();
}

function showResult(row) {
  runHistoryRef.value?.showResult(row);
}

async function goQualityRules() {
  const target = router.getRoutes().find(route =>
    String(route.name).toLowerCase() === 'qualitys' || /\/qualitys\/?$/i.test(route.path)
  );
  await router.push(target?.path || '/rule/qualitys');
}

function resultStatusLabel(value) {
  return ({1: '执行中', 2: '失败', 3: '部分成功', 4: '成功'})[value] || '未知状态';
}

function resultType(value) {
  return ({1: 'primary', 2: 'danger', 3: 'warning', 4: 'success'})[value] || 'info';
}

function triggerLabel(value) {
  return ({1: '手动', 2: '定时'})[value] || '-';
}

function configText(value, key) {
  if (!value) return '';
  try {
    const parsed = JSON.parse(value);
    return typeof parsed === 'string' ? parsed : parsed?.[key] ?? value;
  } catch {
    return value;
  }
}

function scheduleText() {
  if (String(task.value.scheduleFlag) !== '1') return '手动执行';
  const expression = configText(task.value.scheduleConfig, 'expression');
  if (!expression) return '-';
  const fieldCount = String(expression).trim().split(/\s+/).length;
  return [6, 7].includes(fieldCount) ? cronToZh(expression) : expression;
}

function ruleTypeLabel(value) {
  return ruleTypeOptions[value] || value || '-';
}

function fieldLabel(row) {
  let ids = row.modelFieldId;
  try {
    ids = JSON.parse(ids);
  } catch {
    // 兼容逗号分隔字段。
  }
  ids = Array.isArray(ids) ? ids : String(ids ?? '').split(',').filter(Boolean);
  const labels = fieldLabels[row.modelId] || {};
  return ids.map(id => labels[id] || id).join('、') || '-';
}
</script>

<style scoped lang="scss">
.quality-task-detail {
  .detail-summary {
    :deep(.el-row) {
      row-gap: 2px;
    }

    :deep(.infotop-row-value) {
      min-width: 0;
    }
  }

  .summary-scope-tag {
    background-color: var(--el-color-primary-light-9);
    border: 1px solid var(--el-color-primary-light-7) !important;
    color: var(--el-color-primary);
    padding: 0 9px;
  }
  .detail-tab-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    min-height: 48px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  .muted {
    color: var(--el-text-color-secondary);
  }

  @media (max-width: 700px) {
    .detail-tab-toolbar {
      align-items: flex-start;
      flex-direction: column;
      justify-content: center;
      padding: 8px 0;
    }
  }
}
</style>
