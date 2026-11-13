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
  <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
    <template #actions-data>
      <guide-tip-panel type="plain" title="查看当前编码方案引用情况。" />
    </template>
    <qt-table v-bind="tableStore" ref="tableRef">
      <template #modelInfo="{ row }">
        <div v-if="row.model" class="qt-table-info model-info">
          <div class="qt-table-info__main">
            <img :src="getModelIcon(row.model)" alt="" class="info-icon" />
            <span
              class="info-title model-info-title text-ellipsis"
              :role="canOpenModel(row.model) ? 'link' : undefined"
              :tabindex="canOpenModel(row.model) ? 0 : undefined"
              @click="handleDetail(row.model)"
              @keydown.enter="handleDetail(row.model)"
              @keydown.space.prevent="handleDetail(row.model)"
            >{{ formatModelTitle(row.model) }}</span>
            <dict-tag v-if="row.model.type" :options="model_type" :value="row.model.type" size="small" type="primary" />
            <dict-tag
              v-if="getModelVersionNo(row.model)"
              class="ml4"
              :class="{ pointer: canOpenModel(row.model) }"
              :type="getVersionTagType(row.model)"
              size="small"
              :role="canOpenModel(row.model) ? 'link' : undefined"
              :tabindex="canOpenModel(row.model) ? 0 : undefined"
              @click="handleDetail(row.model, 'versions')"
              @keydown.enter="handleDetail(row.model, 'versions')"
              @keydown.space.prevent="handleDetail(row.model, 'versions')"
            >{{ getModelVersionNo(row.model) }}</dict-tag>
          </div>
          <div class="info-desc text-ellipsis">{{ row.model.description || '-' }}</div>
        </div>
        <span v-else>-</span>
      </template>
      <template #field="{ row }">{{ row.name || '-' }}（{{ row.code || '-' }}）</template>
      <template #dataType="{ row }">{{ row.dataType?.toLowerCase() || '-' }}</template>
      <template #dataLength="{ row }">{{ ['STRING', 'DECIMAL'].includes(row.dataType) ? (row.dataLength ?? '-') : '-' }}</template>
      <template #dataScale="{ row }">{{ row.dataType === 'DECIMAL' ? (row.dataScale ?? '-') : '-' }}</template>
      <template #defaultValue="{ row }">{{ formatDefaultValue(row) }}</template>
      <template #requiredFlag="{ row }">
        <dict-tag :options="FLAG_TAG_OPTIONS" :value="requiredFlagLabel(row.requiredFlag)" />
      </template>
      <template #uniqueFlag="{ row }">
        <dict-tag :options="FLAG_TAG_OPTIONS" :value="requiredFlagLabel(row.uniqueFlag)" />
      </template>
      <template #nameFlag="{ row }">
        <dict-tag :options="FLAG_TAG_OPTIONS" :value="requiredFlagLabel(row.nameFlag)" />
      </template>
      <template #modelVersionNo="{ row }">{{ getModelVersionNo(row.model) || '-' }}</template>
      <template #version="{ row }">
        <dict-tag :options="model_status" :value="row.model?.version?.status" />
      </template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="UsageSituation">

import {useDict} from "@/utils/dict";

/** @description 按引用字段展示编码方案版本使用情况，保留对应模型信息。 */
import { computed, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { listEncodingUsage } from "@/api/model/encoding/encoding";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";
import { getModelIcon } from "@/utils/imageUrl";
import { checkPermi } from "@/utils/permission";

const {model_type, model_status} = useDict('model_type', 'model_status');

const FLAG_TAG_OPTIONS = [
  { value: "是", label: "是", elTagType: "primary" },
  { value: "否", label: "否", elTagType: "danger" },
];

const props = defineProps({
  encodingVersionId: { type: [String, Number], default: null },
});
const emit = defineEmits(["modelCount"]);
const router = useRouter();
const canViewModel = computed(() => checkPermi(["model:base:query"]));
const tableRef = ref(null);
let latestListRequest = 0;
const tableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    table: { rowKey: "usageKey", stripe: true },
    actions: { table: { search: false, refresh: true, columns: true } },
  },
  columns: [
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    { label: "引用模型", prop: "model", slot: "modelInfo", minWidth: 380, align: "left", showOverflowTooltip: { effect: "light" } },
    { label: "引用字段", prop: "name", slot: "field", minWidth: 220, align: "left", showOverflowTooltip: { effect: "light" } },
    { label: "数据类型", prop: "dataType", slot: "dataType", minWidth: 120, align: "left" },
    { label: "属性长度", prop: "dataLength", slot: "dataLength", minWidth: 100, align: "left" },
    { label: "小数", prop: "dataScale", slot: "dataScale", minWidth: 65, align: "left" },
    { label: "默认值", prop: "defaultValue", slot: "defaultValue", minWidth: 132, align: "left", showOverflowTooltip: { effect: "light" } },
    { label: "必填", prop: "requiredFlag", slot: "requiredFlag", minWidth: 100, align: "center" },
    { label: "唯一", prop: "uniqueFlag", slot: "uniqueFlag", minWidth: 60 },
    { label: "名称字段", prop: "nameFlag", slot: "nameFlag", minWidth: 80 },
    { label: "模型版本", prop: "id", slot: "modelVersionNo", minWidth: 100, align: "center" },
    { label: "引用数量", prop: "usageCount", minWidth: 100, align: "center" },
    { label: "模型状态", prop: "version", slot: "version", minWidth: 140 },
  ],
  func: getList,
});

// 与模型详情普通字段保持一致，兼容历史布尔值并保留默认值 0。
function requiredFlagLabel(value) {
  return [true, 1, "1", "true"].includes(value) ? "是" : "否";
}
function formatDefaultValue(row) {
  if (row.defaultValue == null || row.defaultValue === "") return "-";
  return row.dataType === "BOOLEAN" ? requiredFlagLabel(row.defaultValue) : row.defaultValue;
}
function formatModelTitle({ name, code }) {
  return name ? (code ? `${name} (${code})` : name) : code || "-";
}

function getModelVersionNo(model) {
  return model?.releaseVersion?.versionNo || model?.version?.versionNo;
}

function getVersionTagType(model) {
  const status = model.version?.status ?? model.releaseVersion?.status;
  return model_status.value.find(item => String(item.value) === String(status))?.elTagType || "info";
}

function canOpenModel(model) {
  return canViewModel.value && model?.id != null;
}

function handleDetail(model, tab) {
  if (!canOpenModel(model)) return;
  router.push({ path: "/model/base/detail", query: { id: model.id, ...(tab ? { tab } : {}) } });
}

/**
 * 展开当前编码版本的引用字段，并按模型去重统计关联模型数。
 * @returns {Promise<Object>} 引用表格请求结果
 */
async function getList() {
  const requestId = ++latestListRequest;
  const encodingVersionId = props.encodingVersionId;
  if (encodingVersionId == null || encodingVersionId === "") {
    emit("modelCount", 0);
    return { data: [] };
  }
  const response = await listEncodingUsage(encodingVersionId);
  if (requestId !== latestListRequest || encodingVersionId !== props.encodingVersionId) return { data: [] };
  const models = Array.isArray(response.data) ? response.data : response.data?.rows || [];
  const rows = models.flatMap(model => (model.fields || []).map(field => ({
    ...field,
    id: model.id,
    usageKey: `${model.id}:${field.id}`,
    usageCount: model.fieldUsageCounts?.[field.id],
    model,
  })));
  emit("modelCount", new Set(rows.map(row => row.model.id)).size);
  return { ...response, data: rows };
}

watch(() => props.encodingVersionId, () => tableRef.value?.getList());
</script>

<style scoped lang="scss">
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
</style>
