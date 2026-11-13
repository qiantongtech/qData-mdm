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
    <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
      <template #search>
        <h4 class="form-header h4">{{ td('sys.system.user.basicInfo') }}</h4>
        <el-form class="btn-style" :model="form" :inline="true" :label-position="labelPosition">
          <qt-form-item :label="td('sys.system.user.userNameLabel')" prop="nickName">
            <el-input v-model="form.nickName" disabled />
          </qt-form-item>
          <qt-form-item :label="td('sys.system.user.loginAccount')" prop="userName">
            <el-input v-model="form.userName" disabled />
          </qt-form-item>
        </el-form>
      </template>
      <template #actions-data>
        <el-button type="primary" @click="submitForm">{{ td('common.button.confirm') }}</el-button>
        <el-button @click="close">{{ td('common.button.return') }}</el-button>
      </template>
      <h4 class="form-header h4">{{ td('sys.system.user.roleInfo') }}</h4>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #index="{ $index }">
          <span>{{ (pageParams.pageNum - 1) * pageParams.pageSize + $index + 1 }}</span>
        </template>
        <template #createTime="{ row }">
          <span>{{ parseTime(row.createTime) }}</span>
        </template>
      </qt-table>
    </qt-wrap>
  </div>
</template>

<script setup name="AuthRole">
import { getCurrentInstance, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import QtFormItem from '@/components/QtFormItem/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { getAuthRole, updateAuthRole } from '@/api/system/system/user.js';

const { td } = useDefaultLang();
const route = useRoute();
const { proxy } = getCurrentInstance();
const tableRef = ref(null);
const pageParams = reactive({ pageNum: 1, pageSize: 10 });
const roleIds = ref([]);
const roles = ref([]);
const form = ref({ nickName: undefined, userName: undefined, userId: undefined });
const initialSelection = ref(null);
let rolesRequest;

const tableStore = reactive({
  config: {
    autoPagination: true,
    actions: { table: { search: false, refresh: false } },
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '500px',
      rowKey: 'roleId',
      onRowClick: clickRow,
      onSelectionChange: handleSelectionChange
    },
    pagination: { pageSizes: [10, 20, 30, 50], pagerCount: document.body.clientWidth < 992 ? 5 : 7 }
  },
  columns: [
    {
      sortable: true,
      slot: 'index',
      get label() {
        return td('common.display.index');
      },
      width: 60
    },
    { type: 'selection', reserveSelection: true, width: 55 },
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
      }
    },
    {
      prop: 'roleKey',
      get label() {
        return td('sys.system.role.permissionChar');
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
      width: 180
    }
  ],
  func: loadRoles,
  params: {},
  events: {
    formatParams(params) {
      pageParams.pageNum = params.pageNum;
      pageParams.pageSize = params.pageSize;
      return params;
    },
    formatData(rows) {
      // 仅在首次加载后回显授权；翻页时保留用户当前的勾选。
      if (initialSelection.value === null) initialSelection.value = rows;
      return rows;
    }
  }
});

/** 接口一次返回全部角色，分页由 QtTable 在前端处理。 */
function loadRoles() {
  if (!rolesRequest) {
    const userId = route.params && route.params.userId;
    if (!userId) return Promise.resolve({ data: [] });
    rolesRequest = getAuthRole(userId)
      .then((response) => {
        form.value = response.user;
        roles.value = response.roles;
        return { data: roles.value };
      })
      .catch((error) => {
        rolesRequest = undefined;
        throw error;
      });
  }
  return rolesRequest;
}

/** 等表格渲染完成后恢复所有页的已授权角色。 */
watch(
  initialSelection,
  (rows) => {
    rows?.forEach((row) => {
      if (row.flag) tableRef.value?.elTableRef?.toggleRowSelection(row, true);
    });
  },
  { flush: 'post' }
);

function clickRow(row) {
  tableRef.value?.elTableRef?.toggleRowSelection(row);
}

function handleSelectionChange(selection) {
  roleIds.value = selection.map((item) => item.roleId);
}

function close() {
  proxy.$tab.closeOpenPage({ path: '/system/user' });
}

function submitForm() {
  const userId = form.value.userId;
  const rIds = roleIds.value.join(',');
  updateAuthRole({ userId, roleIds: rIds }).then(() => {
    proxy.$modal.msgSuccess(td('sys.system.user.authSuccess'));
    close();
  });
}
</script>
