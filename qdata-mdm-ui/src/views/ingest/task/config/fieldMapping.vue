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
  接入字段映射：匹配来源字段与模型字段，配置转换参数并展示平台自动生成字段。
  props：modelValue 为映射行；sourceFields、modelFields 为来源字段和目标模型字段；uniqueFields 为唯一标识。
  props：sourceName、sourceTrust、taskName 用于平台字段预览；readonly 控制是否可编辑。
  emits：update:modelValue 在来源字段同步导致映射集合变化时通知父页。
-->
<template>
  <div class="column-form field-mapping">
    <!-- QtTable 不透传 class，整行布局由外层容器承接。 -->
    <div class="row-full">
      <qt-table ref="mappingTableRef" v-bind="mappingTable" :columns="mappingTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
        <template #sourceFieldName="{ row }">
          <span class="mr10">{{ row.sourceFieldName }}</span>
          <el-tag
            v-if="uniqueFields.includes(row.sourceFieldName)"
            type="success"
            size="small"
          >
            唯一标识
          </el-tag>
        </template>
        <template #transformType="{ row }">
          <el-select
            v-model="row.transformType"
            :disabled="readonly"
            @change="handleTransformChange(row)"
          >
            <el-option
              v-for="item in TRANSFORM_OPTIONS"
              :key="item.value"
              v-bind="item"
            />
          </el-select>
        </template>
        <template #transformConfig="{ row }">
          <el-input
            v-if="[TRANSFORM_NONE, TRANSFORM_TRIM].includes(row.transformType)"
            disabled
            placeholder="无需填写"
          />
          <el-select
            v-else-if="row.transformType === TRANSFORM_TYPE"
            v-model="row.transformConfig.targetType"
            :disabled="readonly"
            placeholder="请选择转换类型"
          >
            <el-option
              v-for="item in model_column_data_type"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
          <el-input
            v-else-if="row.transformType === TRANSFORM_NULL"
            v-model="row.transformConfig.defaultValue"
            :disabled="readonly"
            placeholder="来源值为空时使用"
          />
          <el-input
            v-else-if="row.transformType === TRANSFORM_FIXED"
            v-model="row.transformConfig.value"
            :disabled="readonly"
            placeholder="请输入固定值"
          />
          <el-input
            v-else-if="row.transformType === TRANSFORM_VALUE_MAP"
            :model-value="formatValueMapText(row)"
            :disabled="readonly"
            placeholder="例如 A=生产商;B=服务商"
            @update:model-value="setValueMap(row, $event)"
          />
          <div v-else-if="row.transformType === TRANSFORM_DATE" class="flex-column">
            <el-input
              v-model="row.transformConfig.inputFormat"
              class="mb8"
              :disabled="readonly"
              placeholder="来源格式，例如 yyyy-MM-dd HH:mm:ss"
            />
            <el-input
              v-model="row.transformConfig.outputFormat"
              :disabled="readonly"
              placeholder="目标格式，例如 yyyy-MM-dd"
            />
          </div>
        </template>
        <template #targetFieldId="{ row }">
          <el-select
            v-model="row.targetFieldId"
            :value-on-clear="null"
            filterable
            clearable
            :disabled="readonly"
            placeholder="请选择"
          >
            <el-option
              v-for="field in modelFields"
              :key="field.id"
              :value="field.id"
              :label="`${field.name} (${field.code})`"
              :disabled="
                modelValue.some(
                  (mapping) =>
                    mapping !== row && mapping.targetFieldId === field.id,
                )
              "
            />
          </el-select>
        </template>
      </qt-table>
    </div>
    <div class="h2-titles row-full">接入后平台字段</div>

    <div class="row-full">
      <qt-table ref="platformTableRef" v-bind="platformTable" :columns="platformTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
        <template #text="{ row, column_data }">{{ row[(column_data.property || column_data.prop)] }}</template>
        <template #value="{ row }">
          <el-tag v-if="row.tag"
                  type="warning"
                  size="small"
                  effect="light"
          >{{ row.value }}</el-tag>
          <span v-else>{{ row.value }}</span>
        </template>
      </qt-table>
    </div>
  </div>
</template>

<script setup>
import { watch, computed, getCurrentInstance, ref } from 'vue';

const TRANSFORM_DATE = '6';

const TRANSFORM_FIXED = '4';

const TRANSFORM_NONE = '0';

const TRANSFORM_NULL = '3';

const TRANSFORM_TRIM = '2';

const TRANSFORM_TYPE = '1';

const TRANSFORM_VALUE_MAP = '5';

const TRANSFORM_OPTIONS = [
  { value: TRANSFORM_NONE, label: '不处理' },
  { value: TRANSFORM_TYPE, label: '类型转换' },
  { value: TRANSFORM_TRIM, label: '去除首尾空格' },
  { value: TRANSFORM_NULL, label: '空值处理' },
  { value: TRANSFORM_FIXED, label: '固定值' },
  { value: TRANSFORM_VALUE_MAP, label: '值映射' },
  { value: TRANSFORM_DATE, label: '日期格式转换' },
];

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  sourceFields: { type: Array, default: () => [] },
  modelFields: { type: Array, default: () => [] },
  uniqueFields: { type: Array, default: () => [] },
  sourceName: { type: String, default: '-' },
  sourceTrust: { type: [Number, String], default: 50 },
  taskName: { type: String, default: '' },
  readonly: Boolean,
});
const emit = defineEmits(['update:modelValue']);
const { proxy } = getCurrentInstance();
const { model_column_data_type } = proxy.useDict('model_column_data_type');
const platformFields = computed(() => [
  {
    name: '来源记录 ID',
    method: '根据唯一标识自动生成',
    value: '接入成功后生成',
  },
  {
    name: '来源系统',
    method: '由当前任务自动带入',
    value: props.sourceName || '-',
  },
  {
    name: '来源可信度',
    method: '从来源系统读取，供后续融合使用',
    value: `${props.sourceTrust ?? 50} / 100`,
  },
  {
    name: '接入任务',
    method: '由当前任务自动带入',
    value: props.taskName || '保存后生成任务',
  },
  {
    name: '融合状态',
    method: '成功接入后自动初始化',
    value: '待融合',
    tag: true,
  },
  {
    name: '平台接收时间',
    method: '数据成功接入时自动记录',
    value: '接入成功后记录',
  },
]);
// 本地表格沿用 QtTable 的数据接口，保留原行引用以同步表单编辑。
const mappingTableRef = ref();
const platformTableRef = ref();
const mappingTable = {
  func: async () => ({ data: props.modelValue }),
  config: { initResquest: false, notPagination: true, table: { stripe: true } },
  columns: [
    { label: '来源字段', prop: 'sourceFieldName', slot: 'sourceFieldName', minWidth: 190, align: 'left' },
    { label: '处理方式', prop: 'transformType', slot: 'transformType', minWidth: 170, align: 'left' },
    { label: '处理参数', prop: 'transformConfig', slot: 'transformConfig', minWidth: 220, align: 'left' },
    { label: '目标模型字段', prop: 'targetFieldId', slot: 'targetFieldId', minWidth: 220, align: 'left' },
  ],
};
const platformTable = {
  func: async () => ({ data: platformFields.value }),
  config: { initResquest: false, notPagination: true, table: { stripe: true } },
  columns: [
    { label: '字段', prop: 'name', slot: 'text', minWidth: 160, align: 'left' },
    { label: '生成方式', prop: 'method', slot: 'text', minWidth: 240, align: 'left' },
    { label: '示例 / 初始值', prop: 'value', slot: 'value', minWidth: 220, align: 'left' },
  ],
};
watch([() => props.modelValue, mappingTableRef], () => mappingTableRef.value?.getList(), { flush: 'post' });
watch([platformFields, platformTableRef], () => platformTableRef.value?.getList(), { flush: 'post' });
watch(
  () => [props.sourceFields, props.modelFields],
  () => syncMappings(),
  { immediate: true, deep: true },
);
/**
 * 为新增来源字段创建默认映射，未匹配目标时保持为空。
 * @param {string} name 来源字段名。
 * @param {string|number} [targetFieldId] 自动匹配的目标字段 ID。
 * @returns {Object} 默认映射行。
 */
function createEmptyRow(name, targetFieldId) {
  return {
    sourceFieldName: name,
    targetFieldId: targetFieldId ?? null,
    transformType: TRANSFORM_NONE,
    transformConfig: {},
  };
}
/**
 * 忽略大小写匹配模型字段名称或编码，并跳过已被占用的目标。
 * @param {string} sourceName 来源字段名。
 * @param {Set} usedIds 已占用的目标字段 ID。
 * @returns {string|number|undefined} 匹配的目标 ID。
 */
function matchTarget(sourceName, usedIds) {
  const key = String(sourceName || '').toLowerCase();
  return props.modelFields.find(
    (field) =>
      !usedIds.has(field.id) &&
      (String(field.code || '').toLowerCase() === key ||
        String(field.name || '').toLowerCase() === key),
  )?.id;
}
/** 按来源字段同步映射集合，保留已有配置及用户明确取消的目标映射。 */
function syncMappings() {
  if (!props.sourceFields.length) return;
  const current = props.modelValue || [];
  const bySource = new Map(
    current
      .filter((row) => row?.sourceFieldName)
      .map((row) => [row.sourceFieldName, row]),
  );
  const used = new Set(
    current.map((row) => row.targetFieldId).filter(Boolean),
  );
  let changed = current.length !== props.sourceFields.length;
  const rows = props.sourceFields.map((name, index) => {
    const existing = bySource.get(name);
    if (existing) {
      if (!existing.transformType) existing.transformType = TRANSFORM_NONE;
      if (!existing.transformConfig) existing.transformConfig = {};
      if (existing.transformType === TRANSFORM_TYPE && existing.transformConfig.targetType) {
        // 兼容此前手工输入的小写类型，回显为字典中的类型选项。
        existing.transformConfig.targetType = existing.transformConfig.targetType.trim().toUpperCase();
      }
      if (existing.sourceFieldName !== current[index]?.sourceFieldName)
        changed = true;
      return existing;
    }
    changed = true;
    const targetFieldId = matchTarget(name, used);
    if (targetFieldId) used.add(targetFieldId);
    return createEmptyRow(name, targetFieldId);
  });
  // 已存在的空目标表示明确不映射，回显和重新读取时不能再次自动匹配。
  if (changed) emit('update:modelValue', rows);
}
/** 切换转换方式时清空旧参数，避免不同转换配置相互残留。 */
function handleTransformChange(row) {
  row.transformConfig = {};
}
/**
 * 将值映射对象转为分号分隔的键值对，供输入框回显。
 * @param {Object} row 映射行。
 * @returns {string} 值映射文本。
 */
function formatValueMapText(row) {
  const values = row.transformConfig?.values;
  if (!values || Array.isArray(values) || typeof values !== 'object') return '';
  return Object.entries(values)
    .map(([key, value]) => `${key}=${value}`)
    .join(';');
}
/**
 * 以分号分隔映射项、首个等号分隔键值，忽略缺少键的输入片段。
 * @param {Object} row 映射行。
 * @param {string} text 用户输入的值映射文本。
 */
function setValueMap(row, text) {
  const values = {};
  String(text || '')
    .split(';')
    .forEach((part) => {
      const index = part.indexOf('=');
      if (index <= 0) return;
      const key = part.slice(0, index).trim();
      if (key) values[key] = part.slice(index + 1).trim();
    });
  row.transformConfig = { ...row.transformConfig, values };
}
</script>

