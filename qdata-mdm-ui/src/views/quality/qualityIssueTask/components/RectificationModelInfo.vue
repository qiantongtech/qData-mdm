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
  <div class="rectification-model-info flex-column">
    <div class="rectification-model-main flex-center">
      <img :src="getModelIcon(modelDisplayInfo)" alt="" class="rectification-model-icon" />
      <span class="rectification-model-title text-ellipsis" :title="modelTitle">{{ modelTitle }}</span>
      <dict-tag v-if="modelType" :options="model_type" :value="modelType" size="small" type="primary" />
      <dict-tag v-if="modelVersion && model.version?.status != null"
                :options="versionTagOptions" :value="model.version.status" size="small"
      />
      <dict-tag v-else-if="modelVersion" :value="modelVersion" size="small" type="info" />
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue';
import {useDict} from '@/utils/dict';
import {getModelIcon} from '@/utils/imageUrl';

const props = defineProps({
  model: {type: Object, default: () => ({})},
  fallback: {type: Object, default: () => ({})},
});
const {model_type, model_status} = useDict('model_type', 'model_status');
const modelType = computed(() => props.model.type || props.fallback.modelType);
const modelDisplayInfo = computed(() => ({
  type: modelType.value,
  icon: props.model.icon || props.fallback.modelIcon,
  parentId: props.model.parentId || props.fallback.parentId,
}));
const modelTitle = computed(() => {
  const name = props.model.name || props.fallback.modelName;
  const code = props.model.code || props.fallback.modelCode;
  return name ? (code ? `${name} (${code})` : name) : code || '-';
});
const modelVersion = computed(() => props.model.releaseVersion?.versionNo
    || props.model.version?.versionNo || props.fallback.modelVersion);
/** 保留状态字典的样式，标签文字使用模型版本号。 */
const versionTagOptions = computed(() => model_status.value.map(item => ({
  ...item,
  label: modelVersion.value || '尚未发布',
})));
</script>

<style scoped>
.rectification-model-info {
  width: 100%;
  min-width: 0;
}

.rectification-model-main {
  gap: 6px;
  min-width: 0;
  flex-wrap: wrap;
}

.rectification-model-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  object-fit: contain;
}

.rectification-model-title {
  min-width: 0;
  max-width: 100%;
}
</style>
