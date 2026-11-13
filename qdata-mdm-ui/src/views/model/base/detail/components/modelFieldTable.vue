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
  <!-- 普通、关联、分类和系统字段共用表格；关联类字段额外展示引用信息。 -->
  <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableStore.config">
    <template #actions-data>
      <guide-tip-panel
          type="plain"
          title="查看当前模型定义相关的字段信息。"
      />
    </template>

    <qt-table v-bind="tableStore" ref="tableRef">
      <template #dataType="{ row }">{{ formatDataType(row) }}</template>
      <template #relatedModel="{ row }">{{ row.relatedModelName || row.relatedModelId || "-" }}</template>
      <template #relatedCode="{ row }">{{ row.relatedCodeName || row.relatedCode || "-" }}</template>
      <template #displayCode="{ row }">{{ row.displayCodeName || row.displayCode || "-" }}</template>
      <template #requiredFlag="{ row }">
        <dict-tag :options="sys_yes_no" :value="yesNoValue(row.requiredFlag)"/>
      </template>
      <template #uniqueFlag="{ row }">
        <dict-tag :options="sys_yes_no" :value="yesNoValue(row.uniqueFlag)"/>
      </template>
      <template #nameFlag="{ row }">
        <dict-tag :options="sys_yes_no" :value="yesNoValue(row.nameFlag)"/>
      </template>
      <template #dataLength="{ row }">{{
          ["STRING", "DECIMAL"].includes(row.dataType) ? (row.dataLength ?? "-") : "-"
        }}
      </template>
      <template #dataScale="{ row }">{{ row.dataType === "DECIMAL" ? (row.dataScale ?? "-") : "-" }}</template>
      <template #defaultValue="{ row }">{{ formatDefaultValue(row) }}</template>
      <template #reference="{ row }">{{ formatReference(row) }}</template>
      <template #storageType="{ row }">{{ formatStorageType(row) }}</template>
      <template #nodeRule="{ row }">{{ getNodeRuleLabel(row.nodeRule) }}</template>
      <template #displayMode="{ row }">{{ getDisplayModeLabel(row.displayMode) }}</template>
      <template #systemLock>
        <dict-tag type="warning">
          <span class="justify-center system-lock-tag">
            <el-icon><lock/></el-icon>
            <span>锁定</span>
          </span>
        </dict-tag>
      </template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="ModelFieldTable">

import {useDict} from "@/utils/dict";

/**
 * @description 模型字段只读列表。
 * @prop {Array} fields 模型字段列表
 * @prop {String} category 字段分类，与新增页共用分类标识
 * @prop {Function} queryRows 重新查询当前分类字段的方法
 */
import {computed, onMounted, reactive, ref} from "vue";

import {Lock} from "@element-plus/icons-vue";

import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";
import {listType} from "@/api/model/dict/type/type";

const {
  model_field_node_rule,
  model_field_display_mode,
  model_field_data_type,
  sys_yes_no
} = useDict('model_field_node_rule', 'model_field_display_mode', 'model_field_data_type', 'sys_yes_no');
const nodeRuleLabel = value => model_field_node_rule.value.find(item => item.value === value)?.label || '';
const displayModeLabel = value => model_field_display_mode.value.find(item => item.value === value)?.label || '';

const props = defineProps({
  fields: {type: Array, default: () => []},
  category: {type: String, default: 'BUSINESS'},
  queryRows: {type: Function, default: null},
});
const tableRef = ref();
const referenceOptions = ref([]);

const showRelation = computed(() => ['RELATION', 'CLASS'].includes(props.category));
const showBusiness = computed(() => props.category === 'BUSINESS');
const showSystem = computed(() => props.category === 'SYSTEM');

const tableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    actions: {table: {search: false, refresh: true, columns: true}},
    table: {
      stripe: true, rowKey: "id",
    },
  },
  columns: computed(() => [
    {label: "编号", prop: "id", width: 60, align: "center", sortable: true},
    {label: "字段英文名称", prop: "code", minWidth: 140, align: "left", showOverflowTooltip: {effect: "light"}},
    {label: "字段中文名称", prop: "name", minWidth: 120, align: "left", showOverflowTooltip: {effect: "light"}},
    ...(!showRelation.value ? [
      {label: "数据类型", prop: "dataType", slot: "dataType", minWidth: 120, align: "left"},
    ] : []),
    ...(showBusiness.value ? [
      {label: "属性长度", prop: "dataLength", slot: "dataLength", minWidth: 100, align: "left"},
      {label: "小数", prop: "dataScale", slot: "dataScale", minWidth: 65, align: "left"},
      {
        label: "默认值",
        prop: "defaultValue",
        slot: "defaultValue",
        minWidth: 132,
        align: "left",
        showOverflowTooltip: {effect: "light"}
      },
    ] : []),
    ...(showRelation.value ? [
      {
        label: props.category === 'CLASS' ? "分类模型" : "关联模型",
        prop: "relatedModelId",
        slot: "relatedModel",
        minWidth: 120,
        align: "left",
        showOverflowTooltip: {effect: "light"}
      },
      {
        label: "关联键字段",
        prop: "relatedCode",
        slot: "relatedCode",
        minWidth: 120,
        align: "left",
        showOverflowTooltip: {effect: "light"}
      },
      {
        label: "展示字段",
        prop: "displayCode",
        slot: "displayCode",
        minWidth: 120,
        align: "left",
        showOverflowTooltip: {effect: "light"}
      },
      {label: "存储类型", prop: "storageType", slot: "storageType", minWidth: 120},
    ] : []),
    ...(!showSystem.value ? [
      {
        label: showBusiness.value ? "必填" : "是否必填",
        prop: "requiredFlag",
        slot: "requiredFlag",
        minWidth: 100,
        align: "center"
      },
    ] : []),
    ...(showBusiness.value ? [
      {label: "唯一", prop: "uniqueFlag", slot: "uniqueFlag", minWidth: 60},
      {label: "名称字段", prop: "nameFlag", slot: "nameFlag", minWidth: 80},
      {
        label: "参考数据",
        prop: "id",
        slot: "reference",
        minWidth: 190,
        align: "left",
        showOverflowTooltip: {effect: "light"}
      },
    ] : []),
    ...(props.category === 'CLASS' ? [
      {label: "节点选择规则", prop: "nodeRule", slot: "nodeRule", minWidth: 120},
      {label: "展示方式", prop: "displayMode", slot: "displayMode", minWidth: 110},
    ] : []),
    ...(showSystem.value ? [
      {label: "系统维护", slot: "systemLock", minWidth: 130},
      {label: "描述", prop: "description", minWidth: 240, align: "left", showOverflowTooltip: {effect: "light"}},
    ] : []),
  ]),
  // 刷新时优先重新查询当前分类字段；未提供查询函数时兼容直接使用传入数据。
  func: async params => {
    if (props.queryRows) return props.queryRows(props.category, params);
    return {data: {rows: props.fields, total: props.fields.length}};
  },
  params: {},
});

/**
 * 普通字段类型沿用新增页的小写名称，系统字段保留原类型编码。
 * @param {Object} row 字段配置。
 * @returns {string} 类型名称或占位。
 */
function formatDataType(row) {
  if (!row.dataType) return "-";
  return props.category === 'BUSINESS' ? (model_field_data_type.value.find(item => item.value === row.dataType)?.label || row.dataType) : row.dataType;
}

/**
 * 按编辑弹窗的展示口径拼接字段类型及其长度或精度。
 * @param {Object} row 字段配置。
 * @returns {string} 存储类型文本或占位。
 */
function formatStorageType(row) {
  const dataType = model_field_data_type.value.find(item => item.value === row.dataType)?.label || row.dataType;
  if (row.dataType === 'DECIMAL' && row.dataLength > 0) {
    return `${dataType}(${row.dataLength},${row.dataScale ?? 0})`;
  }
  if (row.dataType === 'STRING' && row.dataLength > 0) {
    return `${dataType}(${row.dataLength})`;
  }
  return dataType || "-";
}

/**
 * 将节点选择规则编码转换为新增页使用的中文名称。
 * @param {string} value 规则编码。
 * @returns {string} 名称或占位。
 */
function getNodeRuleLabel(value) {
  return nodeRuleLabel(value) || "-";
}

/**
 * 将展示方式编码转换为新增页使用的中文名称。
 * @param {string} value 展示方式编码。
 * @returns {string} 名称或占位。
 */
function getDisplayModeLabel(value) {
  return displayModeLabel(value) || "-";
}

/**
 * 将布尔属性及历史数据转换为 sys_yes_no 字典编码。
 * @param {boolean|number|string} value 属性值。
 * @returns {string} Y 或 N。
 */
function yesNoValue(value) {
  return [true, 1, "1", "true", "Y"].includes(value) ? "Y" : "N";
}

function requiredFlagLabel(value) {
  return sys_yes_no.value.find(item => item.value === yesNoValue(value))?.label || "-";
}

/**
 * 格式化默认值，保留零值，布尔类型沿用编辑页的是/否展示。
 * @param {Object} row 字段配置。
 * @returns {string|number|boolean} 默认值或空值占位。
 */
function formatDefaultValue(row) {
  if (row.defaultValue == null || row.defaultValue === "") return "-";
  return row.dataType === "BOOLEAN" ? requiredFlagLabel(row.defaultValue) : row.defaultValue;
}

/**
 * 参考数据展示名称，未关联时与编辑页保持一致。
 * @param {Object} row 字段配置。
 * @returns {string} 参考数据名称或占位文本。
 */
function formatReference(row) {
  if (row.dictTypeId == null) return "不关联";
  return referenceOptions.value.find(item => String(item.id) === String(row.dictTypeId))?.name || "-";
}

onMounted(async () => {
  if (props.category !== 'BUSINESS') return;
  try {
    const response = await listType({pageNum: 1, pageSize: -1});
    referenceOptions.value = response.data?.rows || response.data || [];
  } catch {
    // 请求层已提示错误，保留字段列表，未取得的名称显示占位。
    referenceOptions.value = [];
  }
});
</script>

<style scoped>
.system-lock-tag {
  gap: var(--qt-space-1);
}
</style>
