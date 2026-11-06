<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This document is part of the qData Master Data Platform (open source version).

  qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
  You may use qData for commercial purposes, but you may not remove, hide,
  modify, or replace the qData master data platform logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData master data platform as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div ref="app-container" class="app-container sys-dialog-host">
    <el-dialog
      append-to-body
      :title="preview.title"
      v-model="preview.open"
      width="80%"
      top="5vh"
      :append-to="$refs['app-container']"
      class="scrollbar"
      draggable
      destroy-on-close
    >
      <el-tabs v-model="preview.activeName">
        <el-tab-pane
          v-for="(value, key) in preview.data"
          :label="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.vm'))"
          :name="key.substring(key.lastIndexOf('/') + 1, key.indexOf('.vm'))"
          :key="value"
        >
          <div class="precont">
            <el-link
              :underline="false"
              icon="DocumentCopy"
              v-copyText="value"
              v-copyText:callback="copyTextSuccess"
              style="float: right"
            >
              {{ td('common.button.copy') }}
            </el-link>
            <pre>{{ value }}</pre>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>
<script setup name="GenPreviewDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { preview, copyTextSuccess } = toRefs(props.state);

defineExpose({});
</script>
<style scoped lang="scss">
.sys-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}

.column-form {
  display: flex;
  flex-direction: column;
}

.row-full {
  width: 100%;
  margin-right: 0;
}

.column-form :deep(.el-select),
.column-form :deep(.el-tree-select) {
  width: 100%;
}
</style>
<style scoped>
.precont {
  padding: 0px 15px;
  height: 444px;
  overflow-y: auto;
}
</style>
