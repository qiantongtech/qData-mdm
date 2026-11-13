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
  <!-- 带！号的  -->
  <el-form-item v-bind="$attrs" :class="{ 'has-tip': props.tip !== undefined }">
    <template #default="scope">
      <div class="default-wrap">
        <slot name="default" v-bind="scope || {}" />
        <div class="tip-content" v-if="isString && props.tip !== undefined">
          <!-- <el-icon> <InfoFilled /> </el-icon> -->
          <span :title="props.tip" v-html="props.tip || '&nbsp;'"></span>
        </div>
      </div>
    </template>

    <template #label="scope">
      <div class="label-wrap">
        <slot name="label" v-bind="scope">
          {{ scope.label }}
        </slot>
        <el-tooltip
            v-if="!isString && props.tip"
            v-bind="props.tip"
            :effect="props.tip.effect || 'light'"
            :placement="props.tip.placement || 'top'"
        >
          <!-- <el-icon class="tip-icon"> <InfoFilled /> </el-icon> -->
          <template #content v-if="props.tip.custom">
            <div class="tip-content" v-html="props.tip.content"></div>
          </template>
        </el-tooltip>
      </div>
    </template>
  </el-form-item>
</template>

<script setup name="QtFromItem">
import { computed } from 'vue';

const props = defineProps({
  tip: {
    type: [String, Object],
    default: undefined
  }
});

const isString = computed(() => {
  return typeof props.tip === 'string' || props.tip === null;
});
</script>

<style lang="scss" scoped>
:global(.column-form .el-form-item.has-tip) {
  margin-bottom: 10px !important;
}

.default-wrap {
  width: 100%;
  position: relative;

  .tip-content {
    position: absolute;
    top: 100%;
    left: 0;
    width: 100%;
    padding-top: 2px;
    display: flex;
    align-items: flex-start;
    gap: 4px;
    color: #888;
    font-size: 12px;
    font-weight: 400 !important;
    line-height: 1;
    letter-spacing: 0.2px;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;

    span {
      flex: 1;
      font-weight: 400 !important;
      color: inherit;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.label-wrap{
  display: flex;
  align-items: center;
  gap: 2px;
  .el-icon{
    color: #888;
  }
}

::v-deep(.el-form-item__error) {
  padding-top: 2px;
  top: 100%;
}

.el-form-item.has-tip {
  ::v-deep(.el-form-item__error) {
    position: absolute;
    top: 100%;
    bottom: auto;
    padding-top: 2px;
    line-height: 1;
    margin: 0;
    display: flex;
    align-items: flex-start;
    gap: 4px;
  }
}

.el-form-item.has-tip.is-error {
  .tip-content {
    display: none;
  }
}
</style>
