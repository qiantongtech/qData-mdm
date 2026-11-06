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
  <el-form class="column-form" ref="basicInfoForm" :model="info" :rules="rules" label-width="150px">
    <qt-form-item class="row-full" :label="td('sys.tool.genBasic.tableName')" prop="tableName">
      <el-input
        :placeholder="td('sys.tool.genBasic.tableNamePlaceholder')"
        v-model="info.tableName"
      />
    </qt-form-item>
    <qt-form-item class="row-full" :label="td('sys.tool.genBasic.tableDesc')" prop="tableComment">
      <el-input
        :placeholder="td('sys.tool.genBasic.tableDescRequired')"
        v-model="info.tableComment"
      />
    </qt-form-item>
    <qt-form-item class="row-full" :label="td('sys.tool.genBasic.entityName')" prop="className">
      <el-input
        :placeholder="td('sys.tool.genBasic.entityNameRequired')"
        v-model="info.className"
      />
    </qt-form-item>
    <qt-form-item class="row-full" :label="td('sys.tool.genBasic.author')" prop="functionAuthor">
      <el-input
        :placeholder="td('sys.tool.genBasic.authorRequired')"
        v-model="info.functionAuthor"
      />
    </qt-form-item>
    <qt-form-item class="row-full" :label="td('common.texts.remark')" prop="remark">
      <el-input type="textarea" :rows="3" v-model="info.remark" />
    </qt-form-item>
  </el-form>
</template>

<script setup>
import QtFormItem from '@/components/QtFormItem/index.vue';

import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
defineProps({
  info: {
    type: Object,
    default: null
  }
});

// form validation
const rules = ref({
  tableName: [
    { required: true, message: td('sys.tool.genBasic.tableNameRequired'), trigger: 'blur' }
  ],
  tableComment: [
    { required: true, message: td('sys.tool.genBasic.tableDescRequired'), trigger: 'blur' }
  ],
  className: [
    { required: true, message: td('sys.tool.genBasic.entityNameRequired'), trigger: 'blur' }
  ],
  functionAuthor: [
    { required: true, message: td('sys.tool.genBasic.authorRequired'), trigger: 'blur' }
  ]
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
