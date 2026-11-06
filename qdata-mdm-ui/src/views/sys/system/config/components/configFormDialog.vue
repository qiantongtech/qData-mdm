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
  <div ref="app-container" class="app-container system-dialog-host">
    <el-dialog
      class="dialog"
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
        ref="configRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item class="row-full" :label="td('sys.system.config.paramName')" prop="configName">
          <el-input
            v-model="form.configName"
            :placeholder="td('sys.system.config.paramNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.config.paramKey')" prop="configKey">
          <el-input
            v-model="form.configKey"
            :placeholder="td('sys.system.config.paramKeyPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.config.paramValue')"
          prop="configValue"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.configValue"
            :placeholder="td('sys.system.config.paramValuePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.config.systemBuiltIn')"
          prop="configType"
        >
          <el-radio-group v-model="form.configType">
            <el-radio v-for="dict in sys_yes_no" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
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
            :placeholder="td('sys.config.inputContent')"
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

<script setup name="ConfigFormDialog">
import {
  listConfig,
  getConfig,
  delConfig,
  addConfig,
  updateConfig,
  refreshCache
} from '@/api/system/system/config.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const props = defineProps({ ids: { type: Array, required: true } });
const ids = toRef(props, 'ids');
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { sys_yes_no } = proxy.useDict('sys_yes_no');

const open = ref(false);

const loading = ref(true);

const title = ref('');

const data = reactive({
  form: {},
  rules: {
    configName: [
      { required: true, message: td('sys.system.config.paramNameRequired'), trigger: 'blur' }
    ],
    configKey: [
      { required: true, message: td('sys.system.config.paramKeyRequired'), trigger: 'blur' }
    ],
    configValue: [
      { required: true, message: td('sys.system.config.paramValueRequired'), trigger: 'blur' }
    ]
  }
});
const { form, rules } = toRefs(data);

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    configId: undefined,
    configName: undefined,
    configKey: undefined,
    configValue: undefined,
    configType: 'Y',
    remark: undefined
  };
  proxy.resetForm('configRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.config.addTitle');
}

function handleUpdate(row) {
  reset();
  const configId = row.configId || ids.value;
  getConfig(configId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.config.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['configRef'].validate((valid) => {
    if (valid) {
      if (form.value.configId != undefined) {
        updateConfig(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess(td('common.message.editSuccess'));
            open.value = false;
            emit('success');
            submitLoading.value = false;
          })
          .catch(() => {
            submitLoading.value = false;
          });
      } else {
        addConfig(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess(td('common.message.addSuccess'));
            open.value = false;
            emit('success');
            submitLoading.value = false;
          })
          .catch(() => {
            submitLoading.value = false;
          });
      }
    } else {
      submitLoading.value = false;
    }
  });
}
function openDialog(row, editing = false) {
  if (editing) return handleUpdate(row);
  return handleAdd(row);
}
defineExpose({ openDialog });
</script>
<style scoped lang="scss">
.system-dialog-host {
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
