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
        <qt-form-item class="row-full" :label="td('common.texts.name')" prop="name">
          <el-input v-model="form.name" :placeholder="td('common.form.namePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.cert.issueSubject')"
          prop="issuer"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.subjectId"
            :placeholder="td('sys.cert.selectIssueSubject')"
            @change="subjectChange"
            :style="'width:100%'"
          >
            <el-option
              v-for="item in subjectList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.cert.issuer')" prop="issuer">
          <el-input
            v-model="form.issuer"
            disabled
            :placeholder="td('sys.cert.issuerPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.cert.owner')" prop="possessor">
          <el-input v-model="form.possessor" :placeholder="td('sys.cert.ownerPlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.cert.validPeriod')"
          prop="validTime"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.validTime"
            type="number"
            :max="30"
            :min="1"
            :placeholder="td('sys.cert.validPeriodPlaceholder')"
          >
            <el-button slot="append">{{ td('sys.cert.year') }}</el-button>
          </el-input>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.remark')"
          prop="remark"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="td('sys.cert.inputContent')"
          />
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
<script setup name="CertFormDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { title, open, cancel, submitLoading, submitForm, form, rules, subjectChange, subjectList } =
  toRefs(props.state);
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
