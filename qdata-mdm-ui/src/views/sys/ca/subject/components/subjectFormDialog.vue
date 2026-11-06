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
      :title="title"
      v-model="open"
      width="800px"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <el-form
        class="column-form"
        ref="dialogElement0"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item class="row-full" :label="td('sys.subject.subjectName')" prop="name">
          <el-input v-model="form.name" :placeholder="td('sys.subject.subjectNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.commonName')"
          prop="cn"
          :label-position="labelPosition"
        >
          <el-input v-model="form.cn" :placeholder="td('sys.subject.commonNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.deptName')"
          prop="ou"
          :label-position="labelPosition"
        >
          <el-input v-model="form.ou" :placeholder="td('sys.subject.deptNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.orgName')"
          prop="o"
          :label-position="labelPosition"
        >
          <el-input v-model="form.o" :placeholder="td('sys.subject.orgNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.cityName')"
          prop="l"
          :label-position="labelPosition"
        >
          <el-input v-model="form.l" :placeholder="td('sys.subject.cityNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.province')"
          prop="st"
          :label-position="labelPosition"
        >
          <el-input v-model="form.st" :placeholder="td('sys.subject.provincePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.subject.countryName')"
          prop="c"
          :label-position="labelPosition"
        >
          <el-input v-model="form.c" :placeholder="td('sys.subject.countryPlaceholder')" />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">{{
            td('common.button.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="SubjectFormDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { title, open, cancel, submitLoading, submitForm, form, rules } = toRefs(props.state);
const dialogElement0 = ref(null);
defineExpose({ dialogElement0 });
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
