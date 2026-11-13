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
  <template v-if="location">
    <div v-if="locationText" class="rule-model-location">
      <img :src="getProjectIcon(model.projectIcon || fallback.projectIcon)" alt="" class="rule-model-icon"/>
      <span class="rule-model-title" :title="locationText">{{ locationText }}</span>
    </div>
    <span v-else>-</span>
  </template>
  <div v-else class="rule-model-info">
    <div class="rule-model-main">
      <img :src="getModelIcon({ type: model.type || fallback.modelType, icon: model.icon || fallback.modelIcon, parentId: model.parentId || fallback.parentId })"
           alt="" class="rule-model-icon"
      />
      <span class="rule-model-title" :title="modelTitle">{{ modelTitle }}</span>
      <dict-tag v-if="modelType" :options="model_type" :value="modelType" size="small" type="primary"/>
      <dict-tag v-if="model.releaseVersion?.versionNo && model.version?.status != null" :options="model_status"
                :value="model.version.status" :label="model.releaseVersion.versionNo" size="small"
      />
      <dict-tag v-else-if="versionNo" :value="versionNo" size="small" type="info"/>
    </div>
  </div>
</template>

<script setup>
import { useDict } from "@/utils/dict";
import {computed} from 'vue';
import {getProjectIcon, getModelIcon} from "@/utils/imageUrl";

const { model_type, model_status } = useDict('model_type', 'model_status');

const props = defineProps({
  model: {type: Object, default: () => ({})},
  fallback: {type: Object, default: () => ({})},
  location: {type: Boolean, default: false},
});
const modelType = computed(() => props.model.type || props.fallback.modelType);
const versionNo = computed(() => props.model.releaseVersion?.versionNo || props.fallback.modelVersion);
const modelTitle = computed(() => {
  const name = props.model.name || props.fallback.modelName;
  const code = props.model.code || props.fallback.modelCode;
  return name ? (code ? `${name} (${code})` : name) : code || '-';
});
const locationText = computed(() => [
  props.model.projectName || props.fallback.projectName,
  props.model.layers?.at(-1)?.name || props.fallback.layerName,
].filter(Boolean).join(' / '));
</script>

<style scoped lang="scss">
.rule-model-info {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rule-model-main {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.rule-model-main :deep(.dict-tag-container) {
  flex-shrink: 0;
}

.rule-model-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.rule-model-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rule-model-title {
  max-width: 100%;
  flex: 0 1 auto;
}

.rule-model-location {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
}
</style>
