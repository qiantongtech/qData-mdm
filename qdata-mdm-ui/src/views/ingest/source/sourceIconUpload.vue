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

<!-- 来源系统图标上传沿用主数据域交互，空路径展示相同的默认图标。 -->
<template>
  <image-upload
      v-model="value"
      class="source-icon-upload"
      :class="{ 'source-icon-upload--default': !modelValue }"
      :title="!modelValue ? '点击上传图标' : undefined"
      :limit="1"
      :file-size="5"
      :file-type="['png', 'jpg', 'jpeg']"
      :is-show-tip="false"
  />
</template>

<script setup name="SourceIconUpload">
import {computed} from 'vue';
import ImageUpload from '@/components/ImageUpload/index.vue';

const props = defineProps({modelValue: {type: String, default: ''}});
const emit = defineEmits(['update:modelValue']);
const value = computed({get: () => props.modelValue, set: value => emit('update:modelValue', value)});
</script>

<style scoped lang="scss">
.source-icon-upload {
  line-height: 1;

  :deep(.el-upload--picture-card),
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 48px !important;
    height: 48px !important;
    line-height: 48px !important;
  }

  :deep(.avatar-uploader-icon) {
    font-size: 18px;
  }

  &--default {
    :deep(.el-upload--picture-card) {
      position: relative;
      border: 0;
      background: transparent url('@/assets/common/icons/icon-project-default.svg') center / 32px 32px no-repeat !important;

      &::after {
        content: '+';
        position: absolute;
        inset: 8px;
        display: grid;
        place-items: center;
        border-radius: var(--el-border-radius-base);
        color: var(--el-color-white);
        background: var(--el-overlay-color-light);
        font-size: 22px;
        line-height: 1;
        opacity: 0;
        pointer-events: none;
      }

      &:hover::after,
      &:focus-within::after {
        opacity: 1;
      }
    }

    :deep(.avatar-uploader-icon) {
      visibility: hidden;
    }
  }
}
</style>
