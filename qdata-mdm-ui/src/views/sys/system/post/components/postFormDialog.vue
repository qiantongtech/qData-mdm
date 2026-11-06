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
        ref="postRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item class="row-full" :label="td('sys.system.post.postName')" prop="postName">
          <el-input
            v-model="form.postName"
            :placeholder="td('sys.system.post.postNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.post.postCode')" prop="postCode">
          <el-input
            v-model="form.postCode"
            :placeholder="td('sys.system.post.postCodeLabelPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.post.postOrder')"
          prop="postSort"
          :label-position="labelPosition"
        >
          <el-input-number
            style="width: 100%"
            v-model="form.postSort"
            controls-position="right"
            :min="0"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.post.postStatus')"
          prop="status"
          :label-position="labelPosition"
        >
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
            :placeholder="td('sys.system.post.inputContent')"
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

<script setup name="PostFormDialog">
import useDefaultLang from '@/composables/useDefaultLang';
import { listPost, addPost, delPost, getPost, updatePost } from '@/api/system/system/post.js';
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
    postName: [
      { required: true, message: td('sys.system.post.postNameRequired'), trigger: 'blur' }
    ],
    postCode: [
      { required: true, message: td('sys.system.post.postCodeRequired'), trigger: 'blur' }
    ],
    postSort: [
      { required: true, message: td('sys.system.post.postOrderRequired'), trigger: 'blur' }
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
    postId: undefined,
    postCode: undefined,
    postName: undefined,
    postSort: 0,
    status: '0',
    remark: undefined
  };
  proxy.resetForm('postRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.post.addTitle');
}

function handleUpdate(row) {
  reset();
  const postId = row.postId || ids.value;
  getPost(postId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.post.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['postRef'].validate((valid) => {
    if (valid) {
      if (form.value.postId != undefined) {
        updatePost(form.value)
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
        addPost(form.value)
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
