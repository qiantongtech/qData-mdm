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
        ref="menuRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.menu.parentMenu')"
          :label-position="labelPosition"
        >
          <el-tree-select
            v-model="form.parentId"
            :data="menuOptions"
            :props="{ value: 'menuId', label: 'menuName', children: 'children' }"
            value-key="menuId"
            :placeholder="td('sys.system.menu.selectParentMenu')"
            check-strictly
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.menu.menuType')"
          prop="menuType"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.menuType">
            <el-radio value="M">{{ td('sys.system.menu.directory') }}</el-radio>
            <el-radio value="C">{{ td('sys.system.menu.menu') }}</el-radio>
            <el-radio value="F">{{ td('sys.system.menu.button') }}</el-radio>
          </el-radio-group>
        </qt-form-item>
        <template v-if="form.menuType != 'F'">
          <qt-form-item
            class="row-full"
            :label="td('sys.system.menu.menuIcon')"
            prop="icon"
            :label-position="labelPosition"
          >
            <el-popover placement="bottom-start" :width="540" trigger="click">
              <template #reference>
                <el-input
                  v-model="form.icon"
                  :placeholder="td('sys.system.menu.selectIcon')"
                  @blur="showSelectIcon"
                  readonly
                >
                  <template #prefix>
                    <svg-icon
                      v-if="form.icon"
                      :icon-class="form.icon"
                      class="el-input__icon"
                      style="height: 32px; width: 16px"
                    />
                    <el-icon v-else style="height: 32px; width: 16px">
                      <search />
                    </el-icon>
                  </template>
                </el-input>
              </template>
              <icon-select ref="iconSelectRef" @selected="selected" :active-icon="form.icon" />
            </el-popover>
          </qt-form-item>
        </template>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.menu.showSort')"
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
        <qt-form-item class="row-full" :label="td('sys.system.menu.menuName')" prop="menuName">
          <el-input
            v-model="form.menuName"
            :placeholder="td('sys.system.menu.menuNamePlaceholder')"
          />
        </qt-form-item>
        <template v-if="form.menuType == 'C'">
          <qt-form-item class="row-full" prop="routeName" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.routeNameTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.routeName') }}
              </span>
            </template>
            <el-input
              v-model="form.routeName"
              :placeholder="td('sys.system.menu.routeNamePlaceholder')"
            />
          </qt-form-item>
        </template>
        <template v-if="form.menuType != 'F'">
          <qt-form-item class="row-full" prop="path" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.routePathTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.routePath') }}
              </span>
            </template>
            <el-input
              v-model="form.path"
              :placeholder="td('sys.system.menu.routePathPlaceholder')"
            />
          </qt-form-item>
        </template>
        <template v-if="form.menuType != 'F'">
          <qt-form-item class="row-full" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.isExternalLinkTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.isExternalLink') }}
              </span>
            </template>
            <el-radio-group v-model="form.isFrame">
              <el-radio value="0">{{ td('sys.system.menu.yes') }}</el-radio>
              <el-radio value="1">{{ td('sys.system.menu.no') }}</el-radio>
            </el-radio-group>
          </qt-form-item>
        </template>
        <template v-if="form.menuType == 'C'">
          <qt-form-item class="row-full" prop="component" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.componentPathTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.componentPath') }}
              </span>
            </template>
            <el-input
              v-model="form.component"
              :placeholder="td('sys.system.menu.componentPathPlaceholder')"
            />
          </qt-form-item>
        </template>
        <template v-if="form.menuType != 'M'">
          <qt-form-item class="row-full" :label-position="labelPosition">
            <el-input
              v-model="form.perms"
              :placeholder="td('sys.system.menu.permissionPlaceholder')"
              maxlength="100"
            />
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.permissionTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.permissionChar') }}
              </span>
            </template>
          </qt-form-item>
        </template>
        <template v-if="form.menuType == 'C'">
          <qt-form-item class="row-full" :label-position="labelPosition">
            <el-input
              v-model="form.query"
              :placeholder="td('sys.system.menu.routeParamPlaceholder')"
              maxlength="255"
            />
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.routeParamTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.routeParam') }}
              </span>
            </template>
          </qt-form-item>
        </template>
        <template v-if="form.menuType == 'C'">
          <qt-form-item class="row-full" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.isCacheTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.isCache') }}
              </span>
            </template>
            <el-radio-group v-model="form.isCache">
              <el-radio value="0">{{ td('sys.system.menu.cache') }}</el-radio>
              <el-radio value="1">{{ td('sys.system.menu.noCache') }}</el-radio>
            </el-radio-group>
          </qt-form-item>
        </template>
        <template v-if="form.menuType != 'F'">
          <qt-form-item class="row-full" :label-position="labelPosition">
            <template #label>
              <span>
                <el-tooltip :content="td('sys.system.menu.showStatusTooltip')" placement="top">
                  <el-icon style="color: var(--qt-text-secondary)">
                    <info-filled />
                  </el-icon>
                </el-tooltip>
                {{ td('sys.system.menu.showStatus') }}
              </span>
            </template>
            <el-radio-group v-model="form.visible">
              <el-radio v-for="dict in sys_show_hide" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </qt-form-item>
        </template>
        <qt-form-item class="row-full">
          <template #label>
            <span>
              <el-tooltip :content="td('sys.system.menu.menuStatusTooltip')" placement="top">
                <el-icon style="color: var(--qt-text-secondary)">
                  <info-filled />
                </el-icon>
              </el-tooltip>
              {{ td('sys.system.menu.menuStatus') }}
            </span>
          </template>
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

<script setup name="MenuFormDialog">
import useDefaultLang from '@/composables/useDefaultLang';
import { addMenu, delMenu, getMenu, listMenu, updateMenu } from '@/api/system/system/menu.js';
import SvgIcon from '@/components/SvgIcon/index.vue';
import IconSelect from '@/components/IconSelect/index.vue';
import { ref, reactive, toRefs, toRef, getCurrentInstance } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
const emit = defineEmits(['success']);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const submitLoading = ref(false);

const { sys_show_hide, sys_normal_disable } = proxy.useDict('sys_show_hide', 'sys_normal_disable');

const open = ref(false);

const loading = ref(true);

const title = ref('');

const menuOptions = ref([]);

const iconSelectRef = ref(null);

const data = reactive({
  form: {},
  rules: {
    menuName: [
      { required: true, message: td('sys.system.menu.menuNameRequired'), trigger: 'blur' }
    ],
    orderNum: [
      { required: true, message: td('sys.system.menu.menuSortRequired'), trigger: 'blur' }
    ],
    path: [{ required: true, message: td('sys.system.menu.routePathRequired'), trigger: 'blur' }]
  }
});
const { form, rules } = toRefs(data);

function getTreeselect() {
  menuOptions.value = [];
  listMenu().then((response) => {
    const menu = { menuId: 0, menuName: td('sys.system.menu.rootCategory'), children: [] };
    menu.children = proxy.handleTree(response.data, 'menuId');
    menuOptions.value.push(menu);
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    menuId: undefined,
    parentId: 0,
    menuName: undefined,
    icon: undefined,
    menuType: 'M',
    orderNum: 0,
    isFrame: '1',
    isCache: '0',
    visible: '0',
    status: '0'
  };
  proxy.resetForm('menuRef');
}

function showSelectIcon() {
  iconSelectRef.value.reset();
}

function selected(name) {
  form.value.icon = name;
}

function handleAdd(row) {
  reset();
  getTreeselect();
  if (row != null && row.menuId) {
    form.value.parentId = row.menuId;
  } else {
    form.value.parentId = 0;
  }
  open.value = true;
  title.value = td('sys.system.menu.addTitle');
}

async function handleUpdate(row) {
  reset();
  await getTreeselect();
  getMenu(row.menuId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.system.menu.editTitle');
  });
}

function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs['menuRef'].validate((valid) => {
    if (valid) {
      if (form.value.menuId != undefined) {
        updateMenu(form.value)
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
        addMenu(form.value)
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
