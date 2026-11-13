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
      v-model="dialogVisible"
      :title="td('login.forgetPassword')"
      class="fp-form-dialog"
      width="650px"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <el-form
        class="column-form"
        :model="fpForm"
        label-width="120px"
        style="padding: 10px 60px 0"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('login.username')"
          :label-position="labelPosition"
        >
          <el-input v-model="fpForm.name" :placeholder="td('login.usernameInputPlaceholder')" />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('login.code')" :label-position="labelPosition">
          <div class="wrapper">
            <el-input v-model="fpForm.code" :placeholder="td('login.codePlaceholder')" />
            <el-button
              type="primary"
              :disabled="codeFlag"
              style="margin-left: 10px"
              @click="handleFPCodeClick"
            >
              {{ codeFlag ? `${codeTime}s` : td('login.getCode') }}
            </el-button>
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('login.newPassword')"
          :label-position="labelPosition"
        >
          <el-input v-model="fpForm.password" :placeholder="td('login.newPasswordPlaceholder')" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('login.confirmPassword')"
          :label-position="labelPosition"
        >
          <el-input
            v-model="fpForm.password2"
            :placeholder="td('login.confirmPasswordPlaceholder')"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="dialogVisible = false">{{
            td('login.resetPassword')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="LoginHelpDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { dialogVisible, fpForm, codeFlag, handleFPCodeClick, codeTime } = toRefs(props.state);

defineExpose({});
</script>
