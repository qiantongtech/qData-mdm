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
  <!-- 外层统一使用详情列表结构，展开行保留每个子模型的字段明细。 -->
  <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableStore.config">
    <template #actions-data>
      <guide-tip-panel
        type="plain"
        title="查看当前模型定义相关的子模型信息。"
      />
    </template>
    <qt-table v-bind="tableStore" ref="tableRef">
      <template #expand="{ row }">
        <div class="sub-model-fields">
          <div class="sub-model-fields__title">字段结构（{{ row.fields?.length || 0 }}）</div>
          <qt-table v-bind="getFieldTableStore(row.fields || [])">
            <template #name="{ row: field }">{{ field.name }}</template>
            <template #code="{ row: field }">{{ field.code }}</template>
            <template #dataType="{ row: field }">{{ field.dataType || "-" }}</template>
            <template #category="{ row: field }">{{ categoryLabel(field.columnCategory) }}</template>
            <template #constraint="{ row: field }">{{ constraintLabel(field) }}</template>
            <template #empty>暂无数据</template>
          </qt-table>
        </div>
      </template>
      <template #name="{ row }">{{ row.name || "未命名子模型" }}</template>
      <template #fieldCount="{ row }">{{ row.fields?.length || 0 }}</template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="SubModelList">
/**
 * @description 子模型只读列表。
 * @prop {Array} subModels 子模型列表
 * @prop {Function} queryRows queryRows
 */
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const props = defineProps({
  subModels: { type: Array, default: () => [] },
  queryRows: { type: Function, default: null },
});
const tableRef = ref();

// 本地数据继续直接绑定原数组，不增加请求、分页或数据副本。
const fieldColumns = [
  { label: "字段名称", prop: "name", slot: "name", minWidth: 160, align: "left" },
  { label: "字段编码", prop: "code", slot: "code", minWidth: 180, align: "left" },
  { label: "字段类型", slot: "dataType", minWidth: 150, align: "left" },
  { label: "字段类别", slot: "category", minWidth: 150, align: "left" },
  { label: "约束", slot: "constraint", minWidth: 180, align: "left" },
];
function getFieldTableStore(fields) {
  return {
    columns: fieldColumns,
    config: { initResquest: false, notPagination: true, table: { data: fields, stripe: true } },
    func: async () => ({ data: fields }),
  };
}

const tableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    actions: { table: { search: false, refresh: true, columns: true } },
    table: {
      stripe: true, rowKey: "id",
    },
  },
  columns: [
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    { label: "子模型名称", prop: "name", slot: "name", minWidth: 180, align: "left" },
    { label: "子模型编码", prop: "code", minWidth: 180, align: "left" },
    { label: "描述", prop: "description", minWidth: 240, align: "left", showOverflowTooltip: { effect: "light" } },
    { label: "字段数量", prop: "fields", slot: "fieldCount", minWidth: 120 },
  ],
  // 刷新时优先重新查询子模型；未提供查询函数时兼容直接使用传入数据。
  func: async params => {
    if (props.queryRows) return props.queryRows(params);
    return { data: { rows: props.subModels, total: props.subModels.length } };
  },
  params: {},
});

// 子模型字段分类与主模型字段页签保持同一套展示文案。
const categoryLabel = category => ({
  BUSINESS: "普通字段",
  RELATION: "关联字段",
  CLASS: "分类字段",
  SYSTEM: "系统字段",
}[category] || category || "-");

/** 汇总字段约束；关联字段和分类字段同时展示其引用模型。 */
function constraintLabel(row) {
  const labels = [];
  if (row.requiredFlag) labels.push("必填");
  if (row.uniqueFlag) labels.push("唯一");
  if (row.nameFlag) labels.push("名称字段");
  if (row.columnCategory === "RELATION") labels.push(`引用：${row.relatedModelName || row.relatedModelId || "关联模型"}`);
  if (row.columnCategory === "CLASS") labels.push(`分类：${row.relatedModelName || row.relatedModelId || "分类模型"}`);
  return labels.join("、") || "-";
}
</script>

<style scoped lang="scss">
.sub-model-name {
  color: var(--qt-text-primary);
}

.sub-model-fields {
  padding: var(--qt-space-3) var(--qt-space-6) var(--qt-space-5) 56px;
  background: var(--qt-element-fixed-table-header-background);
}

.sub-model-fields__title {
  margin-bottom: var(--qt-space-3);
  font-weight: var(--qt-font-weight-medium);
  color: var(--qt-text-regular);
}
</style>
