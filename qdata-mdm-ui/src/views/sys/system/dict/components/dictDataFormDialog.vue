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
        ref="dataRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.dictType')"
          :label-position="labelPosition"
        >
          <el-input v-model="form.dictType" :disabled="true" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.dataLabel')"
          prop="dictLabel"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.dictLabel"
            :placeholder="td('sys.system.dictData.dataLabelPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.dataValue')"
          prop="dictValue"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.dictValue"
            :placeholder="td('sys.system.dictData.dataValuePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.styleAttr')"
          prop="cssClass"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.cssClass"
            :placeholder="td('sys.system.dictData.styleAttrPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.showSort')"
          prop="dictSort"
          :label-position="labelPosition"
        >
          <el-input-number
            style="width: 100%"
            v-model="form.dictSort"
            controls-position="right"
            :min="0"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dictData.echoStyle')"
          prop="listClass"
          :label-position="labelPosition"
        >
          <el-select v-model="form.listClass">
            <el-option
              v-for="item in listClassOptions"
              :key="item.value"
              :label="item.label + '(' + item.value + ')'"
              :value="item.value"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('common.texts.status')" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
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
            :placeholder="td('sys.dictData.inputContent')"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" @click="submitForm">{{
            td('common.button.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DictDataFormDialog">
import useDefaultLang from '@/composables/useDefaultLang';
import useDictStore from '@/store/system/dict.js';
import { optionselect as getDictOptionselect, getType } from '@/api/system/system/dict/type.js';
import { listData, getData, delData, addData, updateData } from '@/api/system/system/dict/data.js';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const props = defineProps({
  queryParams: { type: Object, required: true },
  ids: { type: Array, required: true }
});
const queryParams = toRef(props, 'queryParams');
const ids = toRef(props, 'ids');
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const open = ref(false);

const title = ref('');

const listClassOptions = ref([
  { value: 'default', label: td('sys.system.dictData.default') },
  { value: 'primary', label: td('sys.system.dictData.primary') },
  { value: 'success', label: td('sys.system.dictData.success') },
  { value: 'info', label: td('sys.system.dictData.info') },
  { value: 'warning', label: td('sys.system.dictData.warning') },
  { value: 'danger', label: td('sys.system.dictData.danger') }
]);

const data = reactive({
  form: {},
  rules: {
    dictLabel: [
      { required: true, message: td('sys.system.dictData.dataLabelRequired'), trigger: 'blur' }
    ],
    dictValue: [
      { required: true, message: td('sys.system.dictData.dataValueRequired'), trigger: 'blur' }
    ],
    dictSort: [
      { required: true, message: td('sys.system.dictData.dataSortRequired'), trigger: 'blur' }
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
    dictCode: undefined,
    dictLabel: undefined,
    dictValue: undefined,
    cssClass: undefined,
    listClass: 'default',
    dictSort: 0,
    status: '0',
    remark: undefined
  };
  proxy.resetForm('dataRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.dictData.addTitle');
  form.value.dictType = queryParams.value.dictType;
}

function handleUpdate(row) {
  reset();
  const dictCode = row.dictCode || ids.value;
  getData(dictCode).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.dictData.editTitle');
  });
}

function submitForm() {
  proxy.$refs['dataRef'].validate((valid) => {
    if (valid) {
      if (form.value.dictCode != undefined) {
        updateData(form.value).then((response) => {
          useDictStore().removeDict(queryParams.value.dictType);
          proxy.$modal.msgSuccess(td('common.message.editSuccess'));
          open.value = false;
          emit('success');
        });
      } else {
        addData(form.value).then((response) => {
          useDictStore().removeDict(queryParams.value.dictType);
          proxy.$modal.msgSuccess(td('common.message.addSuccess'));
          open.value = false;
          emit('success');
        });
      }
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
