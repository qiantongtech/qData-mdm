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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <!-- 分发请求体只展示根据字段映射生成的示例 JSON，不允许在预览区修改协议。 -->
  <pre class="json-preview"><code class="language-json" v-html="highlighted" /></pre>
</template>

<script setup>
import { computed } from 'vue';
import hljs from 'highlight.js/lib/core';
import json from 'highlight.js/lib/languages/json';

hljs.registerLanguage('json', json);

const props = defineProps({
  value: { type: String, default: '{}' }
});

/** 复用项目已有的 highlight.js 依赖，为只读示例请求体提供 JSON 语法着色。 */
const highlighted = computed(() => hljs.highlight(props.value, { language: 'json', ignoreIllegals: true }).value);
</script>

<style scoped>
/* 参照 Pro2 的深色代码区域，保持固定高度内滚动和可读的等宽字体。 */
.json-preview {
  margin: 0;
  min-height: 110px;
  max-height: 230px;
  overflow: auto;
  padding: var(--qt-space-4);
  background: #182537;
  border-radius: 3px;
  color: #dceafd;
  font: var(--qt-font-size-secondary)/1.7 Consolas, Monaco, monospace;
  white-space: pre;
}

.json-preview code {
  font: inherit;
}

.json-preview :deep(.hljs-attr) {
  color: #85c1ff;
}

.json-preview :deep(.hljs-string) {
  color: #b6e3a0;
}

.json-preview :deep(.hljs-number),
.json-preview :deep(.hljs-literal) {
  color: #f5bb82;
}
</style>
