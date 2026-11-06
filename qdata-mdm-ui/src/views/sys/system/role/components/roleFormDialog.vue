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
        ref="roleFormRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
        class="column-form"
        @submit.prevent
      >
        <qt-form-item class="row-full" :label="td('sys.system.role.roleName')" prop="roleName">
          <el-input
            v-model="form.roleName"
            :placeholder="td('sys.system.role.roleNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item class="row-full" prop="roleKey" :label-position="labelPosition">
          <template #label>
            <span>
              <el-tooltip :content="td('sys.system.role.roleTooltip')" placement="top">
                <el-icon style="color: var(--qt-text-secondary)"><info-filled /></el-icon>
              </el-tooltip>
              {{ td('sys.system.role.permissionChar') }}
            </span>
          </template>
          <el-input
            v-model="form.roleKey"
            :placeholder="td('sys.system.role.permissionCharPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.roleOrder')"
          prop="roleSort"
          :label-position="labelPosition"
        >
          <el-input-number
            style="width: 100%"
            v-model="form.roleSort"
            controls-position="right"
            :min="0"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.status')"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{
              dict.label
            }}</el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.role.menuPermission')"
          :label-position="labelPosition"
        >
          <el-checkbox v-model="menuExpand" @change="handleCheckedTreeExpand($event, 'menu')">{{
            td('common.button.un_fold')
          }}</el-checkbox>
          <el-checkbox v-model="menuNodeAll" @change="handleCheckedTreeNodeAll($event, 'menu')">{{
            td('sys.system.role.selectAll')
          }}</el-checkbox>
          <el-checkbox
            v-model="form.menuCheckStrictly"
            @change="handleCheckedTreeConnect($event, 'menu')"
            >{{ td('sys.system.role.parentChildLink') }}</el-checkbox
          >
          <el-tree
            class="tree-border"
            :data="menuOptions"
            show-checkbox
            ref="menuRef"
            node-key="id"
            :check-strictly="!form.menuCheckStrictly"
            :empty-text="td('sys.system.role.loadingTree')"
            :props="{ label: 'label', children: 'children' }"
          ></el-tree>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.remark')"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="td('sys.system.role.inputContent')"
          ></el-input>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RoleFormDialog">
import { computed, getCurrentInstance, nextTick, ref } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { addRole, getRole, updateRole } from '@/api/system/system/role.js';
import { treeselect as menuTreeselect, roleMenuTreeselect } from '@/api/system/system/menu.js';

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

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const roleFormRef = ref();
const menuRef = ref(null);
const menuOptions = ref([]);
const menuExpand = ref(false);
const menuNodeAll = ref(false);
const rules = computed(() => ({
  roleName: [{ required: true, message: td('sys.system.role.roleNameRequired'), trigger: 'blur' }],
  roleKey: [
    { required: true, message: td('sys.system.role.permissionCharRequired'), trigger: 'blur' }
  ],
  roleSort: [{ required: true, message: td('sys.system.role.roleOrderRequired'), trigger: 'blur' }]
}));

function resetForm() {
  menuRef.value?.setCheckedKeys([]);
  menuExpand.value = false;
  menuNodeAll.value = false;
  menuOptions.value = [];
  form.value = createEmptyForm();
  roleFormRef.value?.clearValidate();
}

async function openDialog(id) {
  resetForm();
  const editing = id != null;
  let checkedKeys = [];
  if (editing) {
    const [response, tree] = await Promise.all([getRole(id), roleMenuTreeselect(id)]);
    form.value = { ...response.data, roleSort: Number(response.data.roleSort) };
    menuOptions.value = tree.menus;
    checkedKeys = tree.checkedKeys;
  } else {
    const response = await menuTreeselect();
    menuOptions.value = response.data;
  }
  title.value = td(editing ? 'sys.system.role.editTitle' : 'sys.system.role.addTitle');
  visible.value = true;
  await nextTick();
  checkedKeys.forEach((key) => menuRef.value?.setChecked(key, true, false));
}

function handleCheckedTreeExpand(value) {
  for (const item of menuOptions.value) {
    const node = menuRef.value?.getNode(item.id);
    if (node) node.expanded = value;
  }
}
function handleCheckedTreeNodeAll(value) {
  menuRef.value?.setCheckedNodes(value ? menuOptions.value : []);
}
function handleCheckedTreeConnect(value) {
  form.value.menuCheckStrictly = !!value;
}

function getMenuAllCheckedKeys() {
  // The currently selected menu node
  let checkedKeys = menuRef.value.getCheckedKeys();
  // Half-selected menu node
  let halfCheckedKeys = menuRef.value.getHalfCheckedKeys();
  checkedKeys.unshift.apply(checkedKeys, halfCheckedKeys);
  return checkedKeys;
}

async function submitForm() {
  if (submitting.value || !roleFormRef.value) return;
  submitting.value = true;
  try {
    const valid = await roleFormRef.value.validate().catch(() => false);
    if (!valid) return;
    const editing = form.value.roleId != null;
    form.value.menuIds = getMenuAllCheckedKeys();
    await (editing ? updateRole(form.value) : addRole(form.value));
    proxy.$modal.msgSuccess(
      td(editing ? 'common.message.editSuccess' : 'common.message.addSuccess')
    );
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
