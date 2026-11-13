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
        ref="deptRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <template v-if="form.parentId !== 0">
          <qt-form-item
            class="row-full"
            :label="td('sys.system.dept.parentDept')"
            prop="parentId"
            :label-position="labelPosition"
          >
            <el-tree-select
              v-model="form.parentId"
              :data="deptOptions"
              :props="{ value: 'deptId', label: 'deptName', children: 'children' }"
              value-key="deptId"
              :placeholder="td('sys.system.dept.selectParentDept')"
              check-strictly
            />
          </qt-form-item>
        </template>
        <qt-form-item class="row-full" :label="td('sys.system.dept.deptName')" prop="deptName">
          <el-input
            v-model="form.deptName"
            :placeholder="td('sys.system.dept.deptNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dept.showSort')"
          prop="orderNum"
          :label-position="labelPosition"
        >
          <el-input-number
            style="width: 100%"
            v-model="form.orderNum"
            controls-position="right"
            :min="0"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dept.leader')"
          prop="leader"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.leader"
            :placeholder="td('sys.system.dept.leaderPlaceholder')"
            maxlength="20"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dept.contactPhone')"
          prop="phone"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.phone"
            :placeholder="td('sys.system.dept.contactPhonePlaceholder')"
            maxlength="11"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dept.email')"
          prop="email"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.email"
            :placeholder="td('sys.system.dept.emailPlaceholder')"
            maxlength="50"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.dept.deptStatus')"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
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

<script setup name="DeptFormDialog">
import useDefaultLang from '@/composables/useDefaultLang';
import {
  listDept,
  getDept,
  delDept,
  addDept,
  updateDept,
  listDeptExcludeChild
} from '@/api/system/system/dept.js';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const open = ref(false);

const loading = ref(true);

const title = ref('');

const deptOptions = ref([]);

const data = reactive({
  form: {},
  rules: {
    parentId: [
      { required: true, message: td('sys.system.dept.parentDeptRequired'), trigger: 'blur' }
    ],
    deptName: [
      { required: true, message: td('sys.system.dept.deptNameRequired'), trigger: 'blur' }
    ],
    orderNum: [
      { required: true, message: td('sys.system.dept.showSortRequired'), trigger: 'blur' }
    ],
    email: [
      { type: 'email', message: td('sys.system.dept.emailRequired'), trigger: ['blur', 'change'] }
    ],
    phone: [
      {
        pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
        message: td('sys.system.dept.phoneRequired'),
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
    deptId: undefined,
    parentId: undefined,
    deptName: undefined,
    orderNum: 0,
    leader: undefined,
    phone: undefined,
    email: undefined,
    status: '0'
  };
  proxy.resetForm('deptRef');
}

function handleAdd(row) {
  reset();
  listDept().then((response) => {
    deptOptions.value = proxy.handleTree(response.data, 'deptId');
  });
  if (row != undefined) {
    form.value.parentId = row.deptId;
  }
  open.value = true;
  title.value = td('sys.system.dept.addTitle');
}

function handleUpdate(row) {
  reset();
  listDeptExcludeChild(row.deptId).then((response) => {
    deptOptions.value = proxy.handleTree(response.data, 'deptId');
  });
  getDept(row.deptId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.dept.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['deptRef'].validate((valid) => {
    if (valid) {
      if (form.value.deptId != undefined) {
        updateDept(form.value)
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
        addDept(form.value)
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
