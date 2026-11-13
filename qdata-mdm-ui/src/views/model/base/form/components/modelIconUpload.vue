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
  <ImageUpload
      :model-value="modelValue"
      class="model-icon-upload"
      :class="{ 'model-icon-upload--default': !modelValue }"
      :style="{ '--model-default-icon': defaultIconBackground }"
      :title="!modelValue ? '点击上传图标' : undefined"
      :limit="1"
      :file-size="5"
      :file-type="['png', 'jpg', 'jpeg']"
      :is-show-tip="false"
      @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup name="ModelIconUpload">
import {computed} from 'vue';
import ImageUpload from '@/components/ImageUpload/index.vue';
import {getDefaultModelIcon} from '@/utils/imageUrl';

const props = defineProps({
  modelValue: {type: String, default: ''},
  type: {type: String, default: ''},
  isSubModel: {type: Boolean, default: false},
});
defineEmits(['update:modelValue']);
// 没上传图片时跟随类型切换默认图标；默认图标不写入 modelValue，避免当成自定义图片保存。
const defaultIconBackground = computed(() => `url("${getDefaultModelIcon(props.type, props.isSubModel)}")`);
</script>

<style scoped lang="scss">
.model-icon-upload {
  line-height: 1;

  :deep(.el-upload--picture-card),
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: var(--model-icon-card-size, 48px) !important;
    height: var(--model-icon-card-size, 48px) !important;
    line-height: var(--model-icon-card-size, 48px) !important;
  }

  :deep(.avatar-uploader-icon) {
    font-size: 18px;
  }

  &.model-icon-upload--default {
    // 空上传框直接展示默认图标，鼠标移上去才出现加号，仍可点击上传。
    :deep(.el-upload--picture-card) {
      position: relative;
      border: 0;
      background: transparent var(--model-default-icon) center / 32px 32px no-repeat !important;

      &::after {
        content: '+';
        position: absolute;
        inset: var(--model-icon-overlay-inset, 8px);
        display: grid;
        place-items: center;
        border-radius: 4px;
        color: #fff;
        background: rgba(0, 0, 0, 0.5);
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
