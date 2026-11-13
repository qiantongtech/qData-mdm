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
  <div v-loading="loading" class="app-container">
    <detail-info :data="detail" :header="detailHeader" :items="detailItems" :gutter="2"
      class="rule-detail-info" mode="free" @back="router.push('/rule/quality')"
    >
      <template #model>
        <RuleModelDisplay :model="modelInfo" :fallback="modelFallback" />
      </template>
      <template #location>
        <RuleModelDisplay :model="modelInfo" :fallback="modelFallback" location />
      </template>
      <template #responsible>{{ getResponsibleName(detail.responsibleUserId) || '-' }} / {{ getResponsiblePhone(detail.responsibleUserId) || '-' }}</template>
      <template #ruleType><dict-tag :options="rule_rulequality_type" :value="detail.ruleType" /></template>
    </detail-info>

    <div v-if="detail.modelId" class="pagecont-bottom">
      <el-tabs v-model="activeTab" class="demo-tabs">
        <el-tab-pane label="模型信息" name="model">
          <qt-wrap :columns="fieldColumns" :table-ref="fieldTableRef" :config="wrapConfig">
            <template #actions-data>
              <guide-tip-panel type="plain" title="本页展示当前质量规则所选字段及其所属模型信息，包括字段名称、数据类型及属性配置，便于了解规则的校验范围。" />
            </template>
            <qt-table ref="fieldTableRef" :key="detail.id" :columns="fieldColumns" :config="fieldConfig" :func="loadFields">
            <template #dataType="{ row }"><dict-tag :options="model_field_data_type" :value="row.dataType" /></template>
            <template #modelType="{ row }"><dict-tag :options="model_type" :value="row.modelType" /></template>
            <template #requiredFlag="{ row }"><dict-tag :options="sys_yes_no" :value="yesNoValue(row.requiredFlag)" /></template>
            <template #uniqueFlag="{ row }"><dict-tag :options="sys_yes_no" :value="yesNoValue(row.uniqueFlag)" /></template>
            <template #nameFlag="{ row }"><dict-tag :options="sys_yes_no" :value="yesNoValue(row.nameFlag)" /></template>
              <template #reference="{ row }">{{ referenceLabel(row.dictTypeId) }}</template>
            </qt-table>
          </qt-wrap>
        </el-tab-pane>
        <el-tab-pane label="质量任务数" name="qualityTasks" lazy>
          <qt-wrap :columns="taskColumns" :table-ref="taskTableRef" :config="wrapConfig">
            <template #actions-data>
              <guide-tip-panel type="plain" title="本页展示当前模型下的质量任务，包括任务名称、任务编码和创建时间，便于了解该模型的质量检测任务配置。" />
            </template>
            <qt-table ref="taskTableRef" :key="detail.id" :columns="taskColumns" :config="taskConfig" :func="loadTasks" />
          </qt-wrap>
        </el-tab-pane>
        <el-tab-pane label="合并管理记录" name="mergeRecords" lazy>
          <qt-wrap :columns="mergeColumns" :table-ref="mergeTableRef" :config="mergeWrapConfig">
            <template #actions-data>
              <guide-tip-panel type="plain" title="本页展示质量检测规则快照中包含当前规则的合并管理记录，包括所属模型、处理状态和最近更新时间，便于追踪相关记录。" />
            </template>
            <qt-table ref="mergeTableRef" :key="detail.id" :columns="mergeColumns" :config="mergeConfig" :func="loadMergeRecords">
              <template #modelName>{{ model.name || model.code || detail.modelId || '-' }}</template>
              <template #status="{ row }">
                <dict-tag :options="merge_pending_status" :value="row.status" />
              </template>
            </qt-table>
          </qt-wrap>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="ruleQualityDetail">
import { useUserDirectory } from '@/store/system/userDirectory';
import { useDict } from "@/utils/dict";
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getRulequality, listQualityMergeRecords } from '@/api/rule/quality/quality';
import { getPublishedModelStructure } from '@/api/model/base/base';
import { listQualitytask } from '@/api/quality/qualityTask/qualitytask';
import DetailInfo from '@/components/DetailInfo/index.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import RuleModelDisplay from '@/views/rule/components/RuleModelDisplay.vue';
import { checkPermi } from '@/utils/permission';

const { merge_pending_status } = useDict('merge_pending_status');

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const { sys_yes_no, model_field_data_type } = useDict("sys_yes_no", "model_field_data_type");
const { model_type } = useDict('model_type');

const route = useRoute();
const router = useRouter();
const { rule_rulequality_type } = useDict('rule_rulequality_type');
const detail = ref({});
const model = ref({});
const modelInfo = ref({});
const modelFallback = computed(() => ({
  ...detail.value,
  modelName: model.value.name || detail.value.modelName,
  modelCode: model.value.code || detail.value.modelCode,
  modelType: model.value.type || detail.value.modelType,
  modelIcon: model.value.icon || detail.value.modelIcon,
  modelDescription: model.value.description || detail.value.modelDescription,
}));
const structure = ref({});
const loading = ref(false);
const activeTab = ref('model');
const fieldTableRef = ref();
const taskTableRef = ref();
const mergeTableRef = ref();
const detailHeader = {
  className: 'clearfixs', idKey: 'id', nameKey: 'ruleName', routerBack: false, statusKey: 'validFlag',
  statusOptions: [{ label: '启用', value: 'true', elTagType: 'success' }, { label: '禁用', value: 'false', elTagType: 'danger' }],
};
const detailItems = [
  { label: '规则编码', key: 'ruleCode' },
  { label: '主数据模型', key: 'modelId', slot: 'model' },
  { label: '归属位置', key: 'projectName', slot: 'location' },
  { label: '描述', key: 'description', span: 24 },
  { label: '规则类型', key: 'ruleType', slot: 'ruleType' },
  { label: '负责人', key: 'responsibleUserId', slot: 'responsible' },
  { label: '创建时间', key: 'createTime', type: 'time' },
];
const wrapConfig = { fullContent: false, actions: { table: { search: false, refresh: true, columns: true } } };
const fieldConfig = { autoPagination: true, sort: true, table: { stripe: true } };
const taskConfig = { sort: true, table: { stripe: true, rowKey: 'id' } };
const mergeWrapConfig = { fullContent: false, actions: { table: { search: false, refresh: true, columns: true } } };
const mergeConfig = { sort: true, table: { stripe: true, rowKey: 'id', defaultSort: { prop: 'updateTime', order: 'descending' } } };
const mergeColumns = [
  { label: '编号', prop: 'id', width: 100, sortable: 'custom', sortableKey: 'id' },
  { label: '主数据模型', prop: 'modelName', slot: 'modelName', minWidth: 200, align: 'left' },
  { label: '处理状态', prop: 'status', slot: 'status', minWidth: 140, align: 'left' },
  { label: '最近更新时间', prop: 'updateTime', date: true, sortable: 'custom', sortableKey: 'update_time', minWidth: 180 },
];
const taskColumns = [
  { label: '编号', prop: 'id', width: 90, sortable: 'custom', sortableKey: 'id' },
  { label: '任务名称', prop: 'taskName', minWidth: 220, align: 'left' },
  { label: '任务编码', prop: 'taskCode', minWidth: 180, align: 'left' },
  { label: '创建时间', prop: 'createTime', date: true, minWidth: 180 },
];
const fieldColumns = [
  { label: '编号', prop: 'id', width: 70, sortable: 'custom', sortableKey: 'id' },
  { label: '模型名称', prop: 'modelName', minWidth: 190, align: 'left' },
  { label: '模型编码', prop: 'modelCode', minWidth: 150, align: 'left' },
  { label: '模型类型', prop: 'modelType', slot: 'modelType', minWidth: 110 },
  { label: '字段英文名称', prop: 'code', minWidth: 150, align: 'left' },
  { label: '字段中文名称', prop: 'name', minWidth: 150, align: 'left' },
  { label: '数据类型', prop: 'dataType', slot: 'dataType', minWidth: 110 },
  { label: '属性长度', prop: 'dataLength', minWidth: 90 },
  { label: '小数', prop: 'dataScale', minWidth: 70 },
  { label: '默认值', prop: 'defaultValue', minWidth: 110 },
  { label: '必填', prop: 'requiredFlag', slot: 'requiredFlag', minWidth: 70 },
  { label: '唯一', prop: 'uniqueFlag', slot: 'uniqueFlag', minWidth: 70 },
  { label: '名称字段', prop: 'nameFlag', slot: 'nameFlag', minWidth: 90 },
  { label: '参考数据', prop: 'dictTypeId', slot: 'reference', minWidth: 150 },
];
const config = computed(() => {
  if (typeof detail.value.ruleConfig !== 'string') return detail.value.ruleConfig || {};
  try { return JSON.parse(detail.value.ruleConfig) || {}; } catch { return {}; }
});
function parseIds(value) {
  if (Array.isArray(value)) return value.flatMap(parseIds);
  if (value && typeof value === 'object') return parseIds(value.modelFieldId ?? value.fieldId ?? value.id);
  if (value == null || value === '') return [];
  if (typeof value === 'string' && value.trim().startsWith('[')) {
    try { return parseIds(JSON.parse(value)); } catch { /* 兼容逗号分隔的历史数据。 */ }
  }
  return String(value).split(',').map(item => item.trim()).filter(Boolean);
}
const selectedFieldIds = computed(() => {
  const value = config.value;
  const ids = [value.fieldIdList, value.fieldList, value.fieldId, value.modelFieldId, value.fieldId1,
    value.fieldId2, value.modelFieldId1, value.modelFieldId2, value.left, value.right, detail.value.modelFieldId]
    .flatMap(parseIds);
  return new Set(ids.map(String));
});
function loadTasks(params) {
  return listQualitytask({ ...params, modelId: detail.value.modelId });
}
function loadMergeRecords(params) {
  return listQualityMergeRecords(detail.value.id, params);
}
function yesNoValue(value) {
  return [true, 1, '1', 'true', 'Y'].includes(value) ? 'Y' : 'N';
}
function referenceLabel(id) {
  if (id == null) return '不关联';
  return structure.value.referenceTypes?.find(item => String(item.id) === String(id))?.name || String(id);
}
async function loadFields(params = {}) {
  if (detail.value.modelId == null) return { data: [] };
  const response = await getPublishedModelStructure(detail.value.modelId);
  structure.value = response.data || {};
  model.value = structure.value.model || {};
  const modelType = model.value.type;
  const fields = (structure.value.fields || []).filter(field => selectedFieldIds.value.has(String(field.id)) || selectedFieldIds.value.has(String(field.code)));
  if (params.orderByColumn === 'id' && ['ascending', 'descending'].includes(params.isAsc)) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    fields.sort((left, right) => direction * String(left.id).localeCompare(String(right.id), undefined, { numeric: true }));
  }
  return { data: fields.map(field => ({ ...field, modelName: model.value.name || '-', modelCode: model.value.code || '-', modelType })) };
}
watch(() => [route.path, route.query.id], async ([path, id], _, onCleanup) => {
  let active = true;
  onCleanup(() => { active = false; });
  if (path !== '/rule/quality/detail') return;
  detail.value = {}; model.value = {}; modelInfo.value = {}; structure.value = {}; activeTab.value = 'model';
  if (!id) return;
  loading.value = true;
  try {
    const response = await getRulequality(id);
    if (!active) return;
    detail.value = response.data || {};
    if (detail.value.modelId && checkPermi(['model:base:query'])) {
      try {
        const modelResponse = await getPublishedModelStructure(detail.value.modelId);
        if (active) modelInfo.value = modelResponse.data?.model || {};
      } catch {
        // 模型查询失败时保留规则接口中已有的信息。
      }
    }
  } finally {
    if (active) loading.value = false;
  }
}, { immediate: true });
</script>

<style scoped lang="scss">
.rule-detail-info :deep(.el-row) { row-gap: var(--qt-space-1); }
</style>
