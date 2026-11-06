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
  <div class="app-container role-management-page" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
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
          v-hasPermi="['system:role:add']"
          >{{ td('common.button.add') }}</el-button
        >
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:role:edit']"
          >{{ td('common.button.update') }}</el-button
        >
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || batchDeleteDisabled"
          @click="handleDelete"
          v-hasPermi="['system:role:remove']"
          >{{ td('common.button.delete') }}</el-button
        >
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:role:export']"
          >{{ td('common.button.export') }}</el-button
        >
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #status="{ row }">
          <el-switch
            v-model="row.status"
            active-value="0"
            inactive-value="1"
            @change="handleStatusChange(row)"
          />
        </template>
        <template #createTime="{ row }">
          <span>{{ parseTime(row.createTime) }}</span>
        </template>
        <template #action="{ row }">
          <el-button
            link
            type="primary"
            icon="View"
            :disabled="!checkPermi(['system:role:query'])"
            @click="$refs.detailDialogRef.openDialog(row)"
            >{{ td('common.button.details') }}</el-button
          >
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(row)"
            v-hasPermi="['system:role:edit']"
            :disabled="row.roleId === 1"
            >{{ td('common.button.update') }}</el-button
          >
          <el-popover
            placement="bottom"
            :width="110"
            trigger="click"
            popper-class="table-action-popover handle-popover"
            :disabled="
              (!checkPermi(['system:role:edit']) ||
                row.roleId === 1 ||
                !checkPermi(['system:role:edit'])) &&
              (!checkPermi(['system:role:edit']) ||
                row.roleId === 1 ||
                !checkPermi(['system:role:edit'])) &&
              (isDeleteDisabled(row) || !checkPermi(['system:role:remove']))
            "
          >
            <template #reference>
              <el-button
                link
                type="primary"
                icon="ArrowDown"
                :disabled="
                  (!checkPermi(['system:role:edit']) ||
                    row.roleId === 1 ||
                    !checkPermi(['system:role:edit'])) &&
                  (!checkPermi(['system:role:edit']) ||
                    row.roleId === 1 ||
                    !checkPermi(['system:role:edit'])) &&
                  (isDeleteDisabled(row) || !checkPermi(['system:role:remove']))
                "
                >{{ td('common.button.more') }}</el-button
              >
            </template>
            <div class="butgdlist task-action-list">
              <el-button
                v-hasPermi="['system:role:edit']"
                link
                type="primary"
                icon="CircleCheck"
                @click="handleDataScope(row)"
                :disabled="!checkPermi(['system:role:edit']) || row.roleId === 1"
                >{{ td('sys.system.role.dataPermission') }}</el-button
              >
              <el-button
                v-hasPermi="['system:role:edit']"
                link
                type="primary"
                icon="User"
                @click="handleAuthUser(row)"
                :disabled="!checkPermi(['system:role:edit']) || row.roleId === 1"
                >{{ td('sys.system.role.assignUser') }}</el-button
              >
              <el-button
                link
                type="danger"
                icon="Delete"
                @click="handleDelete(row)"
                v-hasPermi="['system:role:remove']"
                :disabled="isDeleteDisabled(row)"
                >{{ td('common.button.delete') }}</el-button
              >
            </div>
          </el-popover>
        </template>
      </qt-table>
    </qt-wrap>
    <role-form-dialog ref="roleFormDialogRef" @success="getList" />
    <role-data-scope-dialog ref="roleDataScopeDialogRef" @success="getList" />
    <role-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Role">
import RoleDetailDialog from './components/roleDetailDialog.vue';

import { computed, getCurrentInstance, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { changeRoleStatus, delRole, listRole } from '@/api/system/system/role.js';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import RoleFormDialog from './components/roleFormDialog.vue';
import RoleDataScopeDialog from './components/roleDataScopeDialog.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { checkPermi } from '@/utils/permission';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const router = useRouter();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const tableRef = ref(null);
const roleFormDialogRef = ref();
const roleDataScopeDialogRef = ref();
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const pageParams = reactive({ pageNum: 1, pageSize: 10 });
const queryParams = reactive({
  projectId: 0,
  roleName: undefined,
  roleKey: undefined,
  status: undefined,
  dateRange: []
});

const tableStore = reactive({
  config: {
    table: {
      defaultSort: { prop: 'roleSort', order: 'ascending' },
      stripe: true,
      height: '60vh',
      rowKey: 'roleId',
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: document.body.clientWidth < 992 ? 5 : 7,
      params: computed(() => ({ pageNum: 1, pageSize: pageParams.pageSize }))
    }
  },
  columns: [
    { type: 'selection', width: 55 },
    {
      sortable: true,
      width: 60,
      prop: 'roleId',
      get label() {
        return td('sys.system.role.roleNo');
      }
    },
    {
      prop: 'roleName',
      get label() {
        return td('sys.system.role.roleName');
      },
      showOverflowTooltip: true
    },
    {
      prop: 'roleKey',
      get label() {
        return td('sys.system.role.permissionChar');
      },
      showOverflowTooltip: true
    },
    {
      sortableKey: 'role_sort',
      sortable: true,
      prop: 'roleSort',
      get label() {
        return td('sys.system.role.showOrder');
      }
    },
    {
      slot: 'status',
      get label() {
        return td('common.texts.status');
      }
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      slot: 'createTime',
      get label() {
        return td('common.texts.createdTime');
      },
      width: 160
    },
    {
      slot: 'action',
      get label() {
        return td('common.texts.operation');
      },
      fixed: 'right',
      width: 240,
      className: 'small-padding fixed-width'
    }
  ],
  func: async (params) => {
    const response = await listRole(params);
    return { data: { rows: response.rows, total: response.total } };
  },
  params: queryParams,
  events: {
    formatParams(params) {
      pageParams.pageNum = params.pageNum;
      pageParams.pageSize = params.pageSize;
      return buildQueryParams(params);
    }
  }
});

const searchStore = reactive({
  visibleCount: 4,
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.role.roleName'),
      prop: 'roleName',
      component: { is: 'input', placeholder: td('sys.system.role.roleNamePlaceholder') }
    },
    {
      label: td('sys.system.role.permissionChar'),
      prop: 'roleKey',
      component: { is: 'input', placeholder: td('sys.system.role.permissionCharPlaceholder') }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.role.roleStatus'),
        options: sys_normal_disable.value
      }
    },
    {
      label: td('common.texts.createdTime'),
      prop: 'dateRange',
      component: {
        is: 'date-picker',
        type: 'daterange',
        valueFormat: 'YYYY-MM-DD',
        rangeSeparator: '-',
        startPlaceholder: td('common.form.startDatePlaceholder'),
        endPlaceholder: td('common.form.endDatePlaceholder')
      }
    }
  ])
});

function buildQueryParams(values = { ...queryParams, ...pageParams }) {
  const { dateRange, ...params } = values;
  return proxy.addDateRange(params, dateRange);
}

function getList() {
  tableRef.value?.getList();
}

function handleQuery() {
  tableRef.value?.resetQuery();
}

function resetQuery() {
  queryParams.roleName = undefined;
  queryParams.roleKey = undefined;
  queryParams.status = undefined;
  queryParams.dateRange = [];
  handleQuery();
}

const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row?.status) === '0' || ['1', '3'].includes(String(row?.roleId));
}

function handleDelete(row) {
  if (row?.roleId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const roleIds = row.roleId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.role.confirmDelete', { id: roleIds }))
    .then(function () {
      return delRole(roleIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function handleSelectionChange(selection) {
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.roleId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleStatusChange(row) {
  let text = row.status === '0' ? td('sys.system.role.enable') : td('sys.system.role.disable');
  proxy.$modal
    .confirm(td('sys.system.role.confirmStatusChange', { text: text, name: row.roleName }))
    .then(function () {
      return changeRoleStatus(row.roleId, row.status);
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + td('common.message.success'));
    })
    .catch(function () {
      row.status = row.status === '0' ? '1' : '0';
    });
}

function handleAuthUser(row) {
  router.push('/system/role-auth/user/' + row.roleId);
}

function handleExport() {
  proxy.download('system/role/export', buildQueryParams(), `role_${new Date().getTime()}.xlsx`);
}

function handleAdd() {
  roleFormDialogRef.value.openDialog();
}

function handleUpdate(row) {
  roleFormDialogRef.value.openDialog(row.roleId || ids.value[0]);
}

function handleDataScope(row) {
  roleDataScopeDialogRef.value.openDialog(row.roleId);
}
</script>

<style lang="scss">
.table-action-popover.handle-popover.el-popover.el-popper {
  padding: 12px 0;
  min-width: 120px !important;

  .task-action-list {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    box-sizing: border-box;

    .el-button {
      width: 100%;
      box-sizing: border-box;
      justify-content: center;
      margin-left: 0;
      padding: 8px 12px;
      text-align: center;
      border: none;
      border-radius: 0;

      &:hover {
        background-color: var(--el-fill-color-light);
      }
    }
  }
}
</style>
