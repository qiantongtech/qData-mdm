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
        ref="dictRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item class="row-full" :label="td('sys.system.dictType.dictName')" prop="dictName">
          <el-input
            v-model="form.dictName"
            :placeholder="td('sys.system.dictType.dictNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.dictType.dictType')" prop="dictType">
          <el-input
            v-model="form.dictType"
            :placeholder="td('sys.system.dictType.dictTypePlaceholder')"
          />
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
            :placeholder="td('sys.dictType.inputContent')"
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

<script setup name="DictTypeFormDialog">
import useDictStore from '@/store/system/dict.js';
import {
  listType,
  getType,
  delType,
  addType,
  updateType,
  refreshCache
} from '@/api/system/system/dict/type.js';
import { genCode } from '@/api/system/tool/gen.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const props = defineProps({ ids: { type: Array, required: true } });
const ids = toRef(props, 'ids');
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const open = ref(false);

const loading = ref(true);

const title = ref('');

const data = reactive({
  form: {},
  rules: {
    dictName: [
      { required: true, message: td('sys.system.dictType.dictNameRequired'), trigger: 'blur' }
    ],
    dictType: [
      { required: true, message: td('sys.system.dictType.dictTypeRequired'), trigger: 'blur' }
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
    dictId: undefined,
    dictName: undefined,
    dictType: undefined,
    status: '0',
    remark: undefined
  };
  proxy.resetForm('dictRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.dictType.addTitle');
}

function handleUpdate(row) {
  reset();
  const dictId = row.dictId || ids.value;
  getType(dictId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.dictType.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['dictRef'].validate((valid) => {
    if (valid) {
      if (form.value.dictId != undefined) {
        updateType(form.value)
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
        addType(form.value)
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
