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
  来源详情字段网格：统一展示只读值、状态标签和空值占位符。
  props：items 为字段列表，每项包含 label、value 及可选的 tagType。
-->
<template>
  <el-form label-width="140px" class="column-form source-detail-form">
    <el-form-item v-for="item in items" :key="item.label" :label="item.label">
      <div class="form-readonly source-detail-form__value" :title="displayValue(item.value)">
        <el-tag v-if="item.tagType" :type="item.tagType" size="small">
          {{ displayValue(item.value) }}
        </el-tag>
        <span v-else>{{ displayValue(item.value) }}</span>
      </div>
    </el-form-item>
  </el-form>
</template>

<script setup>
defineProps({
  items: { type: Array, default: () => [] }
})

/**
 * 将对象序列化为 JSON 文本，空值显示占位符。
 * @param {*} value 字段值。
 * @returns {string} 展示文本。
 */
function displayValue(value) {
  if (value == null || value === '') return '-'
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
</script>

<style scoped lang="scss">
/* 来源系统编辑与来源数据只读表单共用的布局规则。 */
.trust-input {
  width: 100%;
}

/* 只读字段保持两列均分，允许列宽随容器收缩。 */
.source-detail-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

/* 解除表单项及内容区的最小宽度限制，防止长字段撑开整列。 */
.source-detail-form :deep(.el-form-item) {
  min-width: 0;
}

.source-detail-form :deep(.el-form-item__content) {
  min-width: 0;
}

.source-detail-form :deep(.el-form-item__label) {
  white-space: nowrap;
}

/* 长字段值在本项内横向滚动，保持标签与相邻行对齐。 */
.source-detail-form__value.form-readonly {
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  word-break: normal;
}

/* 保留普通文本的完整宽度，使超长内容能触发横向滚动。 */
.source-detail-form__value>span:not(.el-tag) {
  flex: none;
}
</style>
