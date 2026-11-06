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
          @click="openSelectUser"
          v-hasPermi="['system:role:add']"
          >{{ td('sys.system.roleAuth.addUser') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="CircleClose"
          :disabled="multiple"
          @click="cancelAuthUserAll"
          v-hasPermi="['system:role:remove']"
          >{{ td('sys.system.roleAuth.batchCancelAuth') }}
        </el-button>
        <el-button type="warning" plain icon="Close" @click="handleClose">{{
          td('common.button.close')
        }}</el-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #status="{ row }">
          <dict-tag :options="sys_normal_disable" :value="row.status" />
        </template>
        <template #createTime="{ row }">
          <span>{{ parseTime(row.createTime) }}</span>
        </template>
        <template #action="{ row }">
          <el-button
            link
            type="primary"
            icon="CircleClose"
            @click="cancelAuthUser(row)"
            v-hasPermi="['system:role:remove']"
            >{{ td('sys.system.roleAuth.cancelAuth') }}
          </el-button>
        </template>
      </qt-table>
    </qt-wrap>
    <select-user ref="selectRef" :roleId="queryParams.roleId" @ok="handleQuery" />
  </div>
</template>

<script setup name="AuthUser">
import { computed, getCurrentInstance, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import selectUser from './components/selectUserDialog.vue';
import { allocatedUserList, authUserCancel, authUserCancelAll } from '@/api/system/system/role.js';

const { td } = useDefaultLang();
const route = useRoute();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const multiple = ref(true);
const userIds = ref([]);

const queryParams = reactive({
  roleId: route.params.roleId,
  userName: undefined,
  phonenumber: undefined
});

const tableRef = ref(null);
const pageSize = ref(10);

const tableStore = reactive({
  config: {
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '60vh',
      rowKey: 'userId',
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: document.body.clientWidth < 992 ? 5 : 7,
      params: computed(() => ({ pageNum: 1, pageSize: pageSize.value }))
    }
  },
  columns: [
    { type: 'selection', width: 55 },
    {
      prop: 'userName',
      get label() {
        return td('sys.system.roleAuth.userName');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'nickName',
      get label() {
        return td('sys.system.roleAuth.userNickName');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'email',
      get label() {
        return td('sys.system.roleAuth.email');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'phonenumber',
      get label() {
        return td('sys.system.roleAuth.mobile');
      },
      align: 'left',
      showOverflowTooltip: true
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
      width: 180
    },
    {
      slot: 'action',
      get label() {
        return td('common.texts.operation');
      },
      fixed: 'right',
      width: 160,
      className: 'small-padding fixed-width'
    }
  ],
  func: async (params) => {
    const response = await allocatedUserList(params);
    return { data: { rows: response.rows, total: response.total } };
  },
  params: queryParams,
  events: {
    onPageSizeChange(params) {
      pageSize.value = params.pageSize;
    }
  }
});

const searchStore = reactive({
  visibleCount: 2,
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.roleAuth.userName'),
      prop: 'userName',
      component: { is: 'input', placeholder: td('sys.system.roleAuth.userNamePlaceholder') }
    },
    {
      label: td('sys.system.roleAuth.phone'),
      prop: 'phonenumber',
      component: { is: 'input', placeholder: td('sys.system.roleAuth.phonePlaceholder') }
    }
  ])
});

/** 刷新当前页，授权成功后由 handleQuery 回到第一页。 */
function getList() {
  tableRef.value?.getList();
}

/** back button */
function handleClose() {
  const obj = { path: '/system/role' };
  proxy.$tab.closeOpenPage(obj);
}

/** Search button action */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** reset button action */
function resetQuery() {
  queryParams.userName = undefined;
  queryParams.phonenumber = undefined;
  handleQuery();
}

/** Multiple selection box selected data */
function handleSelectionChange(selection) {
  userIds.value = selection.map((item) => item.userId);
  multiple.value = !selection.length;
}

/** Open the authorized user table pop-up window */
function openSelectUser() {
  proxy.$refs['selectRef'].show();
}

/** Cancel authorization button operation */
function cancelAuthUser(row) {
  proxy.$modal
    .confirm(td('sys.system.roleAuth.confirmCancelAuth', { name: row.userName }))
    .then(function () {
      return authUserCancel({ userId: row.userId, roleId: queryParams.roleId });
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('sys.system.roleAuth.cancelAuthSuccess'));
    })
    .catch(() => {});
}

/** Batch deauthorization button operation */
function cancelAuthUserAll(row) {
  const roleId = queryParams.roleId;
  const uIds = userIds.value.join(',');
  proxy.$modal
    .confirm(td('sys.system.roleAuth.confirmBatchCancel'))
    .then(function () {
      return authUserCancelAll({ roleId: roleId, userIds: uIds });
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('sys.system.roleAuth.cancelAuthSuccess'));
    })
    .catch(() => {});
}
</script>
