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
    <!-- Create table -->
    <el-dialog
      :title="td('sys.tool.genCreate.title')"
      v-model="visible"
      width="800px"
      top="5vh"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <span>{{ td('sys.tool.genCreate.createTableStmt') }}</span>
      <el-input
        type="textarea"
        :rows="10"
        :placeholder="td('sys.tool.genCreate.inputTextPlaceholder')"
        v-model="content"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" @click="handleImportTable">{{
            td('common.button.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import useDefaultLang from '@/composables/useDefaultLang';
import { createTable } from '@/api/system/tool/gen.js';

const { td } = useDefaultLang();
const visible = ref(false);
const content = ref('');
const { proxy } = getCurrentInstance();
const emit = defineEmits(['ok']);

/** Show popup */
function show() {
  visible.value = true;
}

/** Import button actions */
function handleImportTable() {
  if (content.value === '') {
    proxy.$modal.msgError(td('sys.tool.genCreate.inputCreateStmt'));
    return;
  }
  createTable({ sql: content.value }).then((res) => {
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit('ok');
    }
  });
}

defineExpose({
  show
});
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

.sys-panel {
  padding: 15px;
  background: var(--qt-surface-background);
  border-radius: var(--qt-radius-small);
}

.sys-panel-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 15px;
}
</style>
