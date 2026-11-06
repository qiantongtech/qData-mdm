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
  <div class="app-container" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableActions">
      <template #search>
        <qt-search-bar
          ref="queryRef"
          v-bind="searchStore"
          :params="searchParams"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>
      <template #actions-data>
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:menu:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">
          {{ td('common.button.un_fold') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef" v-if="refreshTable">
          <template #menuName="{ row }">{{ row.menuName }}</template>
          <template #icon="scope">
            <svg-icon :icon-class="scope.row.icon" />
          </template>
          <template #orderNum="{ row }">{{ row.orderNum }}</template>
          <template #perms="scope">
            <span>{{ scope.row.perms || '-' }}</span>
          </template>
          <template #component="scope">
            <span>{{ scope.row.component || '-' }}</span>
          </template>
          <template #status="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['system:menu:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:menu:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-popover
              placement="bottom"
              :width="110"
              trigger="click"
              popper-class="table-action-popover handle-popover"
              :disabled="
                !checkPermi(['system:menu:add']) &&
                (isDeleteDisabled(scope.row) || !checkPermi(['system:menu:remove']))
              "
            >
              <template #reference>
                <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
                  :disabled="
                    !checkPermi(['system:menu:add']) &&
                    (isDeleteDisabled(scope.row) || !checkPermi(['system:menu:remove']))
                  "
                  >{{ td('common.button.more') }}</el-button
                >
              </template>
              <div class="butgdlist task-action-list">
                <el-button
                  link
                  type="primary"
                  icon="Plus"
                  @click="handleAdd(scope.row)"
                  v-hasPermi="['system:menu:add']"
                >
                  {{ td('common.button.add') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['system:menu:remove']"
                  :disabled="isDeleteDisabled(scope.row)"
                >
                  {{ td('common.button.delete') }}
                </el-button>
              </div>
            </el-popover>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <menu-form-dialog ref="formDialogRef" @success="getList" />
    <menu-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Menu">
import MenuDetailDialog from './components/menuDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import useDefaultLang from '@/composables/useDefaultLang';
import { addMenu, delMenu, getMenu, listMenu, updateMenu } from '@/api/system/system/menu.js';
import SvgIcon from '@/components/SvgIcon/index.vue';
import IconSelect from '@/components/IconSelect/index.vue';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import MenuFormDialog from './components/menuFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_show_hide, sys_normal_disable } = proxy.useDict('sys_show_hide', 'sys_normal_disable');

const menuList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const isExpandAll = ref(false);

const refreshTable = ref(true);

const data = reactive({
  queryParams: {
    menuName: undefined,
    visible: undefined
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listMenu(queryParams.value).then((response) => {
    menuList.value = proxy.handleTree(response.data, 'menuId');
    loading.value = false;
  });
}

function handleQuery() {
  getList();
}

function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => {
    refreshTable.value = true;
  });
}

function isDeleteDisabled(row) {
  return String(row?.status) === '0';
}

function handleDelete(row) {
  if (isDeleteDisabled(row)) return;
  proxy.$modal
    .confirm(td('sys.system.menu.confirmDelete', { name: row.menuName }))
    .then(function () {
      return delMenu(row.menuId);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

getList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get menuName() {
    return unref(queryParams).menuName;
  },
  set menuName(value) {
    unref(queryParams).menuName = value;
  },
  get status() {
    return unref(queryParams).status;
  },
  set status(value) {
    unref(queryParams).status = value;
  }
});
const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.menu.menuName'),
      prop: 'menuName',
      component: {
        is: 'input',
        placeholder: td('sys.system.menu.menuNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.menu.menuStatus'),
        clearable: true,
        options: unref(sys_normal_disable).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    notPagination: true,
    table: {
      defaultSort: { prop: 'orderNum', order: 'ascending' },
      height: '60vh',
      rowKey: 'menuId',
      defaultExpandAll: unref(isExpandAll),
      treeProps: { children: 'children', hasChildren: 'hasChildren' }
    }
  })),
  columns: [
    {
      get label() {
        return td('sys.system.menu.menuName');
      },
      showOverflowTooltip: true,
      width: '160',
      slot: 'menuName',
      align: 'left'
    },
    {
      get label() {
        return td('sys.system.menu.icon');
      },
      align: 'center',
      width: '100',
      slot: 'icon'
    },
    {
      sortableKey: 'order_num',
      sortable: true,
      prop: 'orderNum',
      get label() {
        return td('sys.system.menu.sort');
      },
      align: 'center',
      width: '60',
      slot: 'orderNum'
    },
    {
      get label() {
        return td('sys.system.menu.permission');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'perms'
    },
    {
      get label() {
        return td('sys.system.menu.componentPath');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'component'
    },
    {
      get label() {
        return td('common.texts.status');
      },
      width: '80',
      align: 'center',
      slot: 'status'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('common.texts.createdTime');
      },
      align: 'center',
      width: '160',
      slot: 'createTime'
    },
    {
      get label() {
        return td('common.texts.operation');
      },
      align: 'center',
      className: 'small-padding fixed-width',
      fixed: 'right',
      width: '240',
      slot: 'action'
    }
  ],
  func: async () => ({ data: unref(menuList) }),
  params: queryParams,
  events: {}
});

watch([menuList], () => tableRef.value?.resetQuery(), { flush: 'post' });
</script>
