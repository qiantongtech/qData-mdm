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
        <qt-form-item
          class="row-full"
          :label="td('sys.client.clientHomepage')"
          prop="homeUrl"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.homeUrl"
            :placeholder="td('sys.client.clientHomepagePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.syncUrl')"
          prop="syncUrl"
          :label-position="labelPosition"
        >
          <el-input v-model="form.syncUrl" :placeholder="td('sys.client.syncUrlPlaceholder')" />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.client.clientName')" prop="name">
          <el-input v-model="form.name" :placeholder="td('sys.client.clientNamePlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.clientType')"
          prop="type"
          :label-position="labelPosition"
        >
          <el-select v-model="form.type" :placeholder="td('sys.client.selectClientType')">
            <el-option
              v-for="dict in auth_app_type"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.isPublic')"
          prop="publicFlag"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.publicFlag">
            <el-radio v-for="dict in auth_public" :key="dict.value" :label="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.isActive')"
          prop="validFlag"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.validFlag">
            <el-radio v-for="dict in sys_valid" :key="dict.value" :label="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.clientIcon')"
          prop="icon"
          :label-position="labelPosition"
        >
          <div class="xgtpcont">
            <image-upload class="sctplist" v-model="form.icon" />
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.client.authorizedUrl')"
          prop="redirectUrl"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.redirectUrl"
            type="textarea"
            :placeholder="td('sys.client.inputContent')"
          />
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
            :placeholder="td('sys.client.inputContent')"
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
<script setup name="ClientFormDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const {
  title,
  open,
  cancel,
  submitLoading,
  submitForm,
  form,
  rules,
  auth_app_type,
  auth_public,
  sys_valid
} = toRefs(props.state);
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
