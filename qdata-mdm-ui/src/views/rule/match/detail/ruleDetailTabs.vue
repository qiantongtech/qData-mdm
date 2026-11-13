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
  <el-tabs v-model="activeTab" class="demo-tabs">
    <el-tab-pane label="模型信息" name="model">
      <div>
        <el-result v-if="loadError" icon="error" title="模型信息加载失败">
          <template #extra>
            <el-button @click="loadModel">重试</el-button>
          </template>
        </el-result>
        <template v-else>
          <qt-wrap :columns="fieldColumns" :table-ref="fieldTableRef" :config="wrapConfig">
            <template #actions-data>
              <guide-tip-panel type="plain" title="查看当前匹配规则所选字段及其所属模型信息。"/>
            </template>
            <qt-table ref="fieldTableRef" :key="props.modelId" :columns="fieldColumns" :config="fieldConfig"
                      :func="loadFields">
              <template #dataType="{ row }">
                <dict-tag :options="model_field_data_type" :value="row.dataType"/>
              </template>
              <template #modelType="{ row }">
                <dict-tag :options="model_type" :value="row.modelType"/>
              </template>
              <template #requiredFlag="{ row }">
                <dict-tag :options="sys_yes_no" :value="yesNoValue(row.requiredFlag)"/>
              </template>
              <template #uniqueFlag="{ row }">
                <dict-tag :options="sys_yes_no" :value="yesNoValue(row.uniqueFlag)"/>
              </template>
              <template #nameFlag="{ row }">
                <dict-tag :options="sys_yes_no" :value="yesNoValue(row.nameFlag)"/>
              </template>
              <template #reference="{ row }">{{ referenceLabel(row.dictTypeId) }}</template>
            </qt-table>
          </qt-wrap>
        </template>
      </div>
    </el-tab-pane>
    <el-tab-pane label="检测任务" name="tasks" lazy>
      <qt-wrap :columns="taskColumns" :table-ref="taskTableRef" :config="wrapConfig">
        <template #actions-data>
          <guide-tip-panel type="plain" title="查看当前模型的多源融合检测任务，历史匹配规则以执行快照为准。"/>
        </template>
        <qt-table ref="taskTableRef" :key="props.modelId" :columns="taskColumns" :config="taskConfig" :func="loadTasks">
          <template #status="{ row }">
            <dict-tag :options="merge_task_status" :value="row.status" />
          </template>
          <template #lastExecuteTime="{ row }">{{ formatTaskTime(row.lastExecuteTime) }}</template>
          <template #createTime="{ row }">{{ formatTaskTime(row.createTime) }}</template>
        </qt-table>
      </qt-wrap>
    </el-tab-pane>
  </el-tabs>
</template>

<script setup name="RuleDetailTabs">
import {useDict} from "@/utils/dict";
import {ref, watch} from 'vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import {parseTime} from '@/utils/anivia';
import {getPublishedModelStructure} from '@/api/model/base/base';
import { listMergeTask } from '@/api/merge/detect';

const { merge_task_status } = useDict('merge_task_status');

const {sys_yes_no, model_field_data_type} = useDict("sys_yes_no", "model_field_data_type");
const {model_type} = useDict('model_type');

/** 匹配规则详情的模型定义与检测任务。@author chentangwen */

const props = defineProps({
  modelId: {type: [String, Number], required: true},
  selectedFieldIds: {type: Array, default: () => []},
});
const emit = defineEmits(["modelLoaded"]);
const fieldTableRef = ref();
const taskTableRef = ref();
const wrapConfig = {fullContent: false, actions: {table: {search: false, refresh: true, columns: true}}};
const activeTab = ref('model');
const loadError = ref(false);
const structure = ref({});
let structureRequest = null;
let structureRequestModelId = null;

const fieldConfig = {autoPagination: true, sort: true, table: {stripe: true}};
const taskConfig = {sort: true, table: {stripe: true}};
const fieldColumns = [
  {label: '编号', prop: 'id', width: 70, sortable: 'custom'},
  {label: '模型名称', prop: 'modelName', minWidth: 190, align: 'left'},
  {label: '模型编码', prop: 'modelCode', minWidth: 150, align: 'left'},
  {label: '模型类型', prop: 'modelType', slot: 'modelType', minWidth: 110},
  {label: '字段英文名称', prop: 'code', minWidth: 150, align: 'left'},
  {label: '字段中文名称', prop: 'name', minWidth: 150, align: 'left'},
  {label: '数据类型', prop: 'dataType', slot: 'dataType', minWidth: 110},
  {label: '属性长度', prop: 'dataLength', minWidth: 90},
  {label: '小数', prop: 'dataScale', minWidth: 70},
  {label: '默认值', prop: 'defaultValue', minWidth: 110},
  {label: '必填', prop: 'requiredFlag', slot: 'requiredFlag', minWidth: 70},
  {label: '唯一', prop: 'uniqueFlag', slot: 'uniqueFlag', minWidth: 70},
  {label: '名称字段', prop: 'nameFlag', slot: 'nameFlag', minWidth: 90},
  {label: '参考数据', prop: 'dictTypeId', slot: 'reference', minWidth: 150},
];
const taskColumns = [
  {label: '编号', prop: 'id', width: 70, sortable: 'custom'},
  {label: '检测任务编号', prop: 'taskNo', minWidth: 220, align: 'left'},
  {label: '检测范围', prop: 'scopeLabel', minWidth: 180, align: 'left'},
  {label: '来源数据量', prop: 'sourceCount', minWidth: 110},
  {label: '匹配规则数', prop: 'ruleCount', minWidth: 110},
  {label: '最近执行状态', prop: 'status', slot: 'status', minWidth: 130},
  {label: '最近执行时间', prop: 'lastExecuteTime', slot: 'lastExecuteTime', minWidth: 180},
  {label: '执行次数', prop: 'executeCount', minWidth: 90},
  {label: '创建时间', prop: 'createTime', slot: 'createTime', minWidth: 180},
];

function yesNoValue(value) {
  return [true, 1, '1', 'true', 'Y'].includes(value) ? 'Y' : 'N';
}

function referenceLabel(id) {
  if (id == null) return '不关联';
  return structure.value.referenceTypes?.find(item => String(item.id) === String(id))?.name || String(id);
}

async function loadModel() {
  const modelId = props.modelId;
  structure.value = {};
  if (modelId === null || modelId === undefined || modelId === '') {
    return;
  }
  loadError.value = false;
  try {
    const result = await loadStructure(modelId);
    if (String(props.modelId) !== String(modelId)) return;
    structure.value = result;
    emit("modelLoaded", result.model || {});
  } catch {
    if (String(props.modelId) === String(modelId)) loadError.value = true;
  }
}

function loadStructure(modelId) {
  if (structureRequest && String(structureRequestModelId) === String(modelId)) return structureRequest;
  structureRequestModelId = modelId;
  structureRequest = getPublishedModelStructure(modelId)
      .then(response => response.data || {})
      .finally(() => {
        if (String(structureRequestModelId) === String(modelId)) structureRequest = null;
      });
  return structureRequest;
}

async function loadFields(params = {}) {
  const modelId = props.modelId;
  if (modelId === null || modelId === undefined || modelId === '') return {data: []};
  const modelStructure = Object.keys(structure.value).length
      ? structure.value
      : await loadStructure(modelId);
  if (String(props.modelId) !== String(modelId)) return {data: []};
  structure.value = modelStructure;
  const model = modelStructure.model || {};
  const modelType = model.type;
  const selectedIds = new Set(props.selectedFieldIds.map(String));
  const fields = (modelStructure.fields || []).filter(field => selectedIds.has(String(field.id)));
  if (params.orderByColumn === 'id' && ['ascending', 'descending'].includes(params.isAsc)) {
    const direction = params.isAsc === 'ascending' ? 1 : -1;
    fields.sort((left, right) => direction * String(left.id).localeCompare(String(right.id), undefined, {numeric: true}));
  }
  return {
    data: fields.map(field => ({
      ...field,
      modelName: model.name || '-',
      modelCode: model.code || '-',
      modelType,
    }))
  };
}

function loadTasks(params) {
  if (props.modelId === null || props.modelId === undefined || props.modelId === '') return {rows: [], total: 0};
  return listMergeTask({ ...params, modelId: props.modelId });
}

function formatTaskTime(value) {
  if (!value) return '-';
  if (typeof value === 'string' && value.includes('T')) {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '-' : parseTime(date, '{y}-{m}-{d} {h}:{i}:{s}');
  }
  return parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-';
}

watch(() => props.modelId, loadModel, {immediate: true});
watch(() => props.selectedFieldIds, () => fieldTableRef.value?.getList(), {deep: true, flush: 'post'});
</script>

