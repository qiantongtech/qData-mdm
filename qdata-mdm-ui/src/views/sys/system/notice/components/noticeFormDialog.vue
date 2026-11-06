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
        ref="noticeRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.noticeTitle')"
          prop="noticeTitle"
        >
          <el-input
            v-model="form.noticeTitle"
            :placeholder="td('sys.system.notice.noticeTitlePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.noticeType')"
          prop="noticeType"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.noticeType"
            :placeholder="td('sys.system.notice.selectPlaceholder')"
          >
            <el-option
              v-for="dict in sys_notice_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.effectTime')"
          :label-position="labelPosition"
        >
          <el-date-picker
            class="el-form-input-width"
            v-model="dateRange"
            value-format="YYYY-MM-DD HH:mm:ss"
            type="daterange"
            range-separator="-"
            :start-placeholder="td('common.form.startDatePlaceholder')"
            :end-placeholder="td('common.form.endDatePlaceholder')"
            :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.isTopLabel')"
          prop="topFlag"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.topFlag">
            <el-radio v-for="dict in sys_is_or_not" :key="dict.value" :value="+dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.isPopupLabel')"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.alertFlag">
            <el-radio v-for="dict in sys_is_or_not" :key="dict.value" :value="+dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.status')"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_notice_status" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.notice.content')"
          :label-position="labelPosition"
        >
          <editor v-model="form.noticeContentText" :min-height="192" :max-height="300" />
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

<script setup name="NoticeFormDialog">
import {
  listNotice,
  getNoticeOne,
  delNotice,
  addNotice,
  updateNotice
} from '@/api/system/system/notice.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const props = defineProps({ ids: { type: Array, required: true } });
const ids = toRef(props, 'ids');
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { sys_notice_status, sys_notice_type, sys_is_or_not } = proxy.useDict(
  'sys_notice_status',
  'sys_notice_type',
  'sys_is_or_not'
);

const open = ref(false);

const loading = ref(true);

const title = ref('');

const dateRange = ref([]);

const data = reactive({
  form: {},
  rules: {
    noticeTitle: [
      { required: true, message: td('sys.system.notice.noticeTitleRequired'), trigger: 'blur' }
    ],
    // AlertFlag: [ {required: true, message: "Wasn't the window empty," trigger: "blur"},
    // TopFlag: [ {required: true, message: "Is the top not empty," trigger: "blur"},
    noticeType: [
      { required: true, message: td('sys.system.notice.noticeTypeRequired'), trigger: 'change' }
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
    noticeId: undefined,
    noticeTitle: undefined,
    noticeType: undefined,
    noticeContent: undefined,
    noticeContentText: undefined,
    status: '0',
    topFlag: 0,
    alertFlag: 0,
    alertStartTime: null,
    alertEndTime: null
  };
  dateRange.value = [];
  proxy.resetForm('noticeRef');
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.system.notice.addTitle');
}

function handleUpdate(row) {
  reset();
  const noticeId = row.noticeId || ids.value;
  getNoticeOne(noticeId).then((response) => {
    form.value = response.data;
    dateRange.value = [response.data.alertStartTime, response.data.alertEndTime];
    open.value = true;
    title.value = td('sys.system.notice.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  // Split dateRange into fields
  form.value.alertStartTime = dateRange.value?.[0] || null;
  form.value.alertEndTime = dateRange.value?.[1] || null;

  proxy.$refs['noticeRef'].validate((valid) => {
    if (valid) {
      if (form.value.noticeId != undefined) {
        updateNotice(form.value)
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
        addNotice(form.value)
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
