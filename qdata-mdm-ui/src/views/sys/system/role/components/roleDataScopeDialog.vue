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
  <div ref="app-container" class="app-container role-dialog-host">
    <el-dialog
      v-model="visible"
      :title="title"
      width="800px"
      append-to-body
      draggable
      destroy-on-close
      class="dialog"
      :append-to="$refs['app-container']"
      @closed="resetForm"
    >
      <el-form
        ref="roleDataScopeFormRef"
        :model="form"
        label-width="120px"
        :label-position="labelPosition"
        class="column-form"
        @submit.prevent
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.roleNameDataScope')"
          :label-position="labelPosition"
        >
          <el-input v-model="form.roleName" :disabled="true" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.permissionCharDataScope')"
          :label-position="labelPosition"
        >
          <el-input v-model="form.roleKey" :disabled="true" />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.permissionRange')"
          :label-position="labelPosition"
        >
          <el-select v-model="form.dataScope" @change="dataScopeSelectChange">
            <el-option
              v-for="item in dataScopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.dataPermission')"
          v-show="form.dataScope == 2"
          :label-position="labelPosition"
        >
          <el-checkbox v-model="deptExpand" @change="handleCheckedTreeExpand($event, 'dept')">{{
            td('common.button.un_fold')
          }}</el-checkbox>
          <el-checkbox v-model="deptNodeAll" @change="handleCheckedTreeNodeAll($event, 'dept')">{{
            td('sys.system.role.selectAll')
          }}</el-checkbox>
          <el-checkbox
            v-model="form.deptCheckStrictly"
            @change="handleCheckedTreeConnect($event, 'dept')"
            >{{ td('sys.system.role.parentChildLink') }}</el-checkbox
          >
          <el-tree
            class="tree-border"
            :data="deptOptions"
            show-checkbox
            default-expand-all
            ref="deptRef"
            node-key="id"
            :check-strictly="!form.deptCheckStrictly"
            :empty-text="td('sys.system.role.loadingTree')"
            :props="{ label: 'label', children: 'children' }"
          ></el-tree>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="submitDataScope">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RoleDataScopeDialog">
import { computed, getCurrentInstance, nextTick, ref } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { dataScope, deptTreeSelect, getRole } from '@/api/system/system/role.js';

const emit = defineEmits(['success']);
const { proxy } = getCurrentInstance();
const { td } = useDefaultLang();
const visible = ref(false);
const title = ref('');
const submitting = ref(false);
const createEmptyForm = () => ({
  roleId: undefined,
  projectId: 0,
  roleName: undefined,
  roleKey: undefined,
  roleSort: 0,
  status: '0',
  menuIds: [],
  deptIds: [],
  menuCheckStrictly: true,
  deptCheckStrictly: true,
  remark: undefined
});
const form = ref(createEmptyForm());

const roleDataScopeFormRef = ref();
const deptRef = ref(null);
const deptOptions = ref([]);
const deptExpand = ref(true);
const deptNodeAll = ref(false);
const dataScopeOptions = computed(() => [
  { value: '1', label: td('sys.system.role.allDataPermission') },
  { value: '2', label: td('sys.system.role.customDataPermission') },
  { value: '3', label: td('sys.system.role.deptDataPermission') },
  { value: '4', label: td('sys.system.role.deptAndBelowDataPermission') },
  { value: '5', label: td('sys.system.role.selfDataPermission') }
]);

function resetForm() {
  deptRef.value?.setCheckedKeys([]);
  deptOptions.value = [];
  deptExpand.value = true;
  deptNodeAll.value = false;
  form.value = createEmptyForm();
}

async function openDialog(id) {
  resetForm();
  const [response, tree] = await Promise.all([getRole(id), deptTreeSelect(id)]);
  form.value = response.data;
  deptOptions.value = tree.depts;
  title.value = td('sys.system.role.assignDataPermissionTitle');
  visible.value = true;
  await nextTick();
  deptRef.value?.setCheckedKeys(tree.checkedKeys);
}

function handleCheckedTreeExpand(value) {
  for (const item of deptOptions.value) {
    const node = deptRef.value?.getNode(item.id);
    if (node) node.expanded = value;
  }
}
function handleCheckedTreeNodeAll(value) {
  deptRef.value?.setCheckedNodes(value ? deptOptions.value : []);
}
function handleCheckedTreeConnect(value) {
  form.value.deptCheckStrictly = !!value;
}

function getDeptAllCheckedKeys() {
  // The currently selected department node
  let checkedKeys = deptRef.value.getCheckedKeys();
  // Half-selected department node
  let halfCheckedKeys = deptRef.value.getHalfCheckedKeys();
  checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
  return checkedKeys;
}

function dataScopeSelectChange(value) {
  if (value !== '2') {
    deptRef.value.setCheckedKeys([]);
  }
}

async function submitDataScope() {
  if (submitting.value || form.value.roleId == null) return;
  submitting.value = true;
  try {
    form.value.deptIds = getDeptAllCheckedKeys();
    await dataScope(form.value);
    proxy.$modal.msgSuccess(td('common.message.editSuccess'));
    visible.value = false;
    emit('success');
  } finally {
    submitting.value = false;
  }
}

defineExpose({ openDialog });
</script>

<style scoped lang="scss">
.role-dialog-host {
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
