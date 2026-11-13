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
          v-hasPermi="['system:dept:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button type="info" plain icon="Sort" @click="toggleExpandAll">{{
          td('common.button.un_fold')
        }}</el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef" v-if="refreshTable">
          <template #orderNum="{ row }">{{ row.orderNum }}</template>
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
              :disabled="!checkPermi(['system:dept:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:dept:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-popover
              placement="bottom"
              :width="110"
              trigger="click"
              popper-class="table-action-popover handle-popover"
              :disabled="
                !checkPermi(['system:dept:add']) &&
                (isDeleteDisabled(scope.row) ||
                  !(scope.row.parentId != 0) ||
                  !checkPermi(['system:dept:remove']))
              "
            >
              <template #reference>
                <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
                  :disabled="
                    !checkPermi(['system:dept:add']) &&
                    (isDeleteDisabled(scope.row) ||
                      !(scope.row.parentId != 0) ||
                      !checkPermi(['system:dept:remove']))
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
                  v-hasPermi="['system:dept:add']"
                >
                  {{ td('common.button.add') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['system:dept:remove']"
                  :disabled="isDeleteDisabled(scope.row) || !(scope.row.parentId != 0)"
                >
                  {{ td('common.button.delete') }}
                </el-button>
              </div>
            </el-popover>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <dept-form-dialog ref="formDialogRef" @success="getList" />
    <dept-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Dept">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import DeptDetailDialog from './components/deptDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import useDefaultLang from '@/composables/useDefaultLang';
import {
  listDept,
  getDept,
  delDept,
  addDept,
  updateDept,
  listDeptExcludeChild
} from '@/api/system/system/dept.js';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import DeptFormDialog from './components/deptFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const deptList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const isExpandAll = ref(true);

const refreshTable = ref(true);

const data = reactive({
  queryParams: {
    deptName: undefined,
    status: undefined
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listDept(queryParams.value).then((response) => {
    deptList.value = proxy.handleTree(response.data, 'deptId');
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
  return String(row?.status) === '0' || String(row?.parentId) === '0';
}

function handleDelete(row) {
  if (isDeleteDisabled(row)) return;
  proxy.$modal
    .confirm(td('sys.system.dept.confirmDelete', { name: row.deptName }))
    .then(function () {
      return delDept(row.deptId);
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
  get deptName() {
    return unref(queryParams).deptName;
  },
  set deptName(value) {
    unref(queryParams).deptName = value;
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
      label: td('sys.system.dept.deptName'),
      prop: 'deptName',
      component: {
        is: 'input',
        placeholder: td('sys.system.dept.deptNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.dept.deptStatus'),
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
      cellClassName: titleCellClass,
      defaultSort: { prop: 'orderNum', order: 'ascending' },
      height: '60vh',
      rowKey: 'deptId',
      defaultExpandAll: unref(isExpandAll),
      treeProps: { children: 'children', hasChildren: 'hasChildren' }
    }
  })),
  columns: [
    {
      get label() {
        return td('sys.system.dept.deptName');
      },
      showOverflowTooltip: true,
      prop: 'deptName',
      className: 'title-column',
      get link() { return tableTitleLink(row => proxy.$refs.detailDialogRef.openDialog(row), 'system:dept:query'); },
      align: 'left'
    },
    {
      sortableKey: 'order_num',
      sortable: true,
      prop: 'orderNum',
      get label() {
        return td('sys.system.dept.sort');
      },
      slot: 'orderNum',
      align: 'left'
    },
    {
      get label() {
        return td('common.texts.status');
      },
      slot: 'status',
      align: 'left'
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
  func: async () => ({ data: unref(deptList) }),
  params: queryParams,
  events: {}
});

watch([deptList], () => tableRef.value?.resetQuery(), { flush: 'post' });
</script>
