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
        ref="messageTemplateRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.messageTemplate.msgTitle')"
          prop="title"
        >
          <el-input
            v-model="form.title"
            :placeholder="td('sys.system.messageTemplate.msgTitlePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.messageTemplate.msgCategory')"
          prop="category"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.category"
            :placeholder="td('sys.system.messageTemplate.selectPlaceholder')"
          >
            <el-option
              v-for="dict in message_category"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.messageTemplate.msgLevel')"
          prop="level"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.msgLevel"
            :placeholder="td('sys.system.messageTemplate.selectPlaceholder')"
          >
            <el-option
              v-for="dict in message_level"
              :key="dict.value"
              :label="dict.label"
              :value="parseInt(dict.value)"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.messageTemplate.msgTemplate')"
          prop="content"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.content"
            type="textarea"
            :placeholder="td('sys.system.messageTemplate.inputContent')"
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
            :placeholder="td('sys.system.messageTemplate.inputContent')"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button size="mini" @click="cancel">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" size="mini" :loading="submitLoading" @click="submitForm">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MessageTemplateFormDialog">
import useDefaultLang from '@/composables/useDefaultLang';
import {
  listMessageTemplate,
  getMessageTemplate,
  delMessageTemplate,
  addMessageTemplate,
  updateMessageTemplate
} from '@/api/system/system/message/messageTemplate';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const props = defineProps({ ids: { type: Array, required: true } });
const ids = toRef(props, 'ids');
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { message_category, message_level } = proxy.useDict('message_category', 'message_level');

const open = ref(false);

const loading = ref(true);

const title = ref('');

const data = reactive({
  form: {},
  rules: {
    title: [
      {
        required: true,
        message: td('sys.system.messageTemplate.msgTitleRequired'),
        trigger: 'blur'
      }
    ],
    content: [
      {
        required: true,
        message: td('sys.system.messageTemplate.msgTemplateRequired'),
        trigger: 'blur'
      }
    ],
    category: [
      {
        required: true,
        message: td('sys.system.messageTemplate.msgCategoryRequired'),
        trigger: 'blur'
      }
    ],
    msgLevel: [
      {
        required: true,
        message: td('sys.system.messageTemplate.msgLevelRequired'),
        trigger: 'blur'
      }
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
    id: null,
    title: null,
    content: null,
    category: null,
    msgLevel: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm('messageTemplateRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.messageTemplate.addTitle');
}

function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getMessageTemplate(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.messageTemplate.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['messageTemplateRef'].validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateMessageTemplate(form.value)
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
        addMessageTemplate(form.value)
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
