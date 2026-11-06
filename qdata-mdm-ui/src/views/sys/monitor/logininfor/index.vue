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
      :config="{ actions: { table: { columns: false } } }"
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
      <template #actions-data>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:logininfor:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          @click="handleClean"
          v-hasPermi="['monitor:logininfor:remove']"
        >
          {{ td('sys.monitor.logininfor.clearAll') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Unlock"
          :disabled="single"
          @click="handleUnlock"
          v-hasPermi="['monitor:logininfor:unlock']"
        >
          {{ td('sys.monitor.logininfor.unlock') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['monitor:logininfor:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #infoId="{ row }">{{ row.infoId }}</template>
          <template #userName="{ row }">{{ row.userName }}</template>
          <template #ipaddr="{ row }">{{ row.ipaddr }}</template>
          <template #loginLocation="{ row }">{{ row.loginLocation }}</template>
          <template #os="{ row }">{{ row.os }}</template>
          <template #browser="{ row }">{{ row.browser }}</template>
          <template #status="scope">
            <dict-tag :options="enSysCommonStatus" :value="scope.row.status" />
          </template>
          <template #msg="{ row }">{{ row.msg }}</template>
          <template #loginTime="scope">
            <span>{{ parseTime(scope.row.loginTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['monitor:logininfor:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <logininfor-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Logininfor">
import LogininforDetailDialog from './components/logininforDetailDialog.vue';
import { checkPermi } from '@/utils/permission';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import {
  list,
  delLogininfor,
  cleanLogininfor,
  unlockLogininfor
} from '@/api/system/monitor/logininfor.js';
import useDefaultLang from '@/composables/useDefaultLang';
import enDict from '@/locales/en-US/dict/index.js';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const { sys_common_status } = proxy.useDict('sys_common_status');

const enSysCommonStatus = computed(() => {
  return (sys_common_status.value || []).map((item) => ({
    ...item,
    label: enDict.sys_common_status?.[item.value] ?? item.label
  }));
});

const logininforList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const selectName = ref('');
const total = ref(0);
const dateRange = ref([]);
const defaultSort = ref({ prop: 'infoId', order: 'descending' });

// query parameters
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  ipaddr: undefined,
  userName: undefined,
  status: undefined,
  orderByColumn: undefined,
  isAsc: undefined
});

/** Query login log list */
function getList() {
  loading.value = true;
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    logininforList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** Search button action */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** reset button action */
function resetQuery() {
  dateRange.value = [];
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  queryParams.value.pageNum = 1;
  qtTableRef0.value.elTableRef.sort(defaultSort.value.prop, defaultSort.value.order);
}

/** Multiple selection box selected data */
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.infoId);
  multiple.value = !selection.length;
  single.value = selection.length != 1;
  selectName.value = selection.map((item) => item.userName);
}

/** Sorting trigger events */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

/** Delete button action */
function handleDelete(row) {
  const infoIds = row.infoId || ids.value;
  proxy.$modal
    .confirm(td('logininfor.confirmDelete', { ids: infoIds }))
    .then(function () {
      return delLogininfor(infoIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

/** Clear button action */
function handleClean() {
  proxy.$modal
    .confirm(td('logininfor.confirmClearAll'))
    .then(function () {
      return cleanLogininfor();
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('logininfor.clearSuccess'));
    })
    .catch(() => {});
}

/** Unlock button operation */
function handleUnlock() {
  const username = selectName.value;
  proxy.$modal
    .confirm(td('logininfor.confirmUnlock', { name: username }))
    .then(function () {
      return unlockLogininfor(username);
    })
    .then(() => {
      proxy.$modal.msgSuccess(td('logininfor.unlockSuccess', { name: username }));
    })
    .catch(() => {});
}

/** Export button action */
function handleExport() {
  proxy.download(
    'monitor/logininfor/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  );
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
  },
  get status() {
    return unref(queryParams).status;
  },
  set status(value) {
    unref(queryParams).status = value;
  },
  get dateRange() {
    return dateRange.value;
  },
  set dateRange(value) {
    dateRange.value = value;
  }
});

const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.monitor.logininfor.loginAddr'),
      prop: 'ipaddr',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.logininfor.loginAddrPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.logininfor.userName'),
      prop: 'userName',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.logininfor.userNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.logininfor.loginStatus'),
        clearable: true,
        options: (unref(sys_common_status) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('sys.monitor.logininfor.loginTime'),
      prop: 'dateRange',
      component: {
        is: 'date-picker',
        valueFormat: 'YYYY-MM-DD HH:mm:ss',
        type: 'daterange',
        rangeSeparator: '-',
        startPlaceholder: td('common.form.startDatePlaceholder'),
        endPlaceholder: td('common.form.endDatePlaceholder'),
        defaultTime: [new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)],
        clearable: false
      }
    }
  ])
});

function handleTablePagination0(params) {
  unref(queryParams).pageNum = params.pageNum;
  unref(queryParams).pageSize = params.pageSize;
  getList();
}

const qtTableRef0 = ref(null);

const tableStore0 = reactive({
  config: computed(() => ({
    table: {
      stripe: true,
      height: '60vh',
      onSelectionChange: handleSelectionChange,
      defaultSort: defaultSort,
      onSortChange: handleSortChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: 5,
      params: { pageNum: unref(queryParams).pageNum, pageSize: unref(queryParams).pageSize }
    }
  })),
  columns: [
    { type: 'selection', width: '55', align: 'center' },
    {
      sortableKey: 'info_id',
      prop: 'infoId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.monitor.logininfor.accessNo');
      },
      align: 'center',
      slot: 'infoId'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.userName');
      },
      width: '120',
      align: 'center',
      showOverflowTooltip: true,
      sortable: 'custom',
      sortOrders: ['descending', 'ascending'],
      slot: 'userName',
      prop: 'userName',
      sortableKey: 'userName'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.address');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'ipaddr'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.loginLocation');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'loginLocation'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.os');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'os'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.browser');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'browser'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.loginStatus');
      },
      align: 'center',
      slot: 'status'
    },
    {
      get label() {
        return td('common.texts.description');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'msg'
    },
    {
      get label() {
        return td('sys.monitor.logininfor.accessTime');
      },
      align: 'center',
      sortable: 'custom',
      sortOrders: ['descending', 'ascending'],
      width: '180',
      slot: 'loginTime',
      prop: 'loginTime',
      sortableKey: 'loginTime'
    },
    {
      get label() {
        return td('common.texts.operation');
      },
      slot: 'action',
      fixed: 'right',
      width: 120,
      align: 'center'
    }
  ],
  func: async () => ({ data: { rows: unref(logininforList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(logininforList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>
