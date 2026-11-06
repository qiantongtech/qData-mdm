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
    <qt-wrap
      :columns="tableStore0.columns"
      :config="{ actions: { show: false } }"
      :table-ref="{ getList }"
    >
      <template #search>
        <qt-search-bar
          ref="queryRef"
          v-bind="searchStore"
          :params="searchParams"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #cell0="scope">
            <span>{{ (pageNum - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
          <template #tokenId="{ row }">{{ row.tokenId }}</template>
          <template #userName="{ row }">{{ row.userName }}</template>
          <template #deptName="{ row }">{{ row.deptName }}</template>
          <template #ipaddr="{ row }">{{ row.ipaddr }}</template>
          <template #loginLocation="{ row }">{{ row.loginLocation }}</template>
          <template #os="{ row }">{{ row.os }}</template>
          <template #browser="{ row }">{{ row.browser }}</template>
          <template #loginTime="scope">
            <span>{{ parseTime(scope.row.loginTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['monitor:online:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleForceLogout(scope.row)"
              v-hasPermi="['monitor:online:forceLogout']"
            >
              {{ td('sys.monitor.online.forceLogout') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <online-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Online">
import OnlineDetailDialog from './components/onlineDetailDialog.vue';
import { checkPermi } from '@/utils/permission';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import useDefaultLang from '@/composables/useDefaultLang';
import { forceLogout, list as initData } from '@/api/system/monitor/online.js';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();

const onlineList = ref([]);
const loading = ref(true);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const queryParams = ref({
  ipaddr: undefined,
  userName: undefined
});

/** Query login log list */
function getList() {
  loading.value = true;
  initData(queryParams.value).then((response) => {
    onlineList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** Search button action */
function handleQuery() {
  pageNum.value = 1;
  getList();
}

/** reset button action */
function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

/** Forced back button operation */
function handleForceLogout(row) {
  proxy.$modal
    .confirm(td('sys.monitor.online.confirmForceLogout', { name: row.userName }))
    .then(function () {
      return forceLogout(row.tokenId);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

getList();

const searchParams = reactive({
  get ipaddr() {
    return unref(queryParams).ipaddr;
  },
  set ipaddr(value) {
    unref(queryParams).ipaddr = value;
  },
  get userName() {
    return unref(queryParams).userName;
  },
  set userName(value) {
    unref(queryParams).userName = value;
  }
});

const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.monitor.online.loginAddr'),
      prop: 'ipaddr',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.online.loginAddrPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.online.userName'),
      prop: 'userName',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.online.userNamePlaceholder'),
        clearable: true
      }
    }
  ])
});

function handleTablePagination0(params) {
  pageNum.value = params.pageNum;
  pageSize.value = params.pageSize;
}

const qtTableRef0 = ref(null);

const tableStore0 = reactive({
  config: computed(() => ({
    autoPagination: true,
    table: {
      defaultSort: { prop: 'tokenId', order: 'descending' },
      height: '60vh',
      stripe: true,
      style: 'width: 100%;'
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: 5,
      params: { pageNum: pageNum.value, pageSize: pageSize.value }
    }
  })),
  columns: [
    {
      sortable: true,
      get label() {
        return td('common.display.index');
      },
      width: 60,
      type: 'index',
      align: 'center',
      slot: 'cell0'
    },
    {
      sortableKey: 'token_id',
      prop: 'tokenId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.monitor.online.sessionId');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'tokenId'
    },
    {
      get label() {
        return td('sys.monitor.online.loginName');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'userName'
    },
    {
      get label() {
        return td('sys.monitor.online.dept');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'deptName'
    },
    {
      get label() {
        return td('sys.monitor.online.host');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'ipaddr'
    },
    {
      get label() {
        return td('sys.monitor.online.loginLocation');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'loginLocation'
    },
    {
      get label() {
        return td('sys.monitor.online.os');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'os'
    },
    {
      get label() {
        return td('sys.monitor.online.browser');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'browser'
    },
    {
      get label() {
        return td('sys.monitor.online.loginTime');
      },
      align: 'center',
      width: '180',
      slot: 'loginTime'
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
  func: async () => ({ data: unref(onlineList) }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch([() => unref(onlineList)], () => qtTableRef0.value?.resetQuery(), { flush: 'post' });
</script>

<style scoped lang="scss"></style>
