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
          v-hasPermi="['monitor:operlog:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          @click="handleClean"
          v-hasPermi="['monitor:operlog:remove']"
        >
          {{ td('sys.monitor.operlog.clearAll') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['monitor:operlog:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #operId="{ row }">{{ row.operId }}</template>
          <template #title="{ row }">{{ row.title }}</template>
          <template #businessType="scope">
            <dict-tag :options="enSysOperType" :value="scope.row.businessType" />
          </template>
          <template #operName="{ row }">{{ row.operName }}</template>
          <template #operIp="{ row }">{{ row.operIp }}</template>
          <template #status="scope">
            <dict-tag :options="enSysCommonStatus" :value="scope.row.status" />
          </template>
          <template #operTime="scope">
            <span>{{ parseTime(scope.row.operTime) }}</span>
          </template>
          <template #costTime="scope">
            <span>{{ scope.row.costTime }}{{ td('sys.monitor.operlog.millisecond') }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleView(scope.row, scope.index)"
              v-hasPermi="['monitor:operlog:query']"
            >
              {{ td('sys.monitor.operlog.detail') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <operlog-detail-dialog ref="operlogDetailDialogRef" :state="operlogDetailDialogState" />
  </div>
</template>

<script setup name="Operlog">
import OperlogDetailDialog from './components/operlogDetailDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import { list, delOperlog, cleanOperlog } from '@/api/system/monitor/operlog.js';
import useDefaultLang from '@/composables/useDefaultLang';
import enDict from '@/locales/en-US/dict/index.js';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const { sys_oper_type, sys_common_status } = proxy.useDict('sys_oper_type', 'sys_common_status');

/** Always use the English dict tag option */
const enSysOperType = computed(() => {
  return (sys_oper_type.value || []).map((item) => ({
    ...item,
    label: enDict.sys_oper_type?.[item.value] ?? item.label
  }));
});

const enSysCommonStatus = computed(() => {
  return (sys_common_status.value || []).map((item) => ({
    ...item,
    label: enDict.sys_common_status?.[item.value] ?? item.label
  }));
});

const operlogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const dateRange = ref([]);
const defaultSort = ref({ prop: 'operId', order: 'descending' });

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operIp: undefined,
    title: undefined,
    operName: undefined,
    businessType: undefined,
    status: undefined
  }
});

const { queryParams, form } = toRefs(data);

/** Query login log */
function getList() {
  loading.value = true;
  list(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    operlogList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** Operation log type dictionary translation */
function typeFormat(row, column) {
  return proxy.selectDictLabel(sys_oper_type.value, row.businessType);
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
  ids.value = selection.map((item) => item.operId);
  multiple.value = !selection.length;
}

/** Sorting trigger events */
function handleSortChange(column, prop, order) {
  queryParams.value.orderByColumn = column.prop;
  queryParams.value.isAsc = column.order;
  getList();
}

/** Detailed button operations */
function handleView(row) {
  open.value = true;
  form.value = row;
}

/** Delete button action */
function handleDelete(row) {
  const operIds = row.operId || ids.value;
  proxy.$modal
    .confirm(td('sys.monitor.operlog.confirmDelete', { ids: operIds }))
    .then(function () {
      return delOperlog(operIds);
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
    .confirm(td('sys.monitor.operlog.confirmClearAll'))
    .then(function () {
      return cleanOperlog();
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('sys.monitor.operlog.clearSuccess'));
    })
    .catch(() => {});
}

/** Export button action */
function handleExport() {
  proxy.download(
    'monitor/operlog/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  );
}

getList();

const operlogDetailDialogState = reactive({
  get open() {
    return unref(open);
  },
  set open(value) {
    if (isRef(open)) open.value = value;
    else Object.assign(open, value);
  },
  get form() {
    return unref(form);
  },
  set form(value) {
    if (isRef(form)) form.value = value;
    else Object.assign(form, value);
  },
  get typeFormat() {
    return unref(typeFormat);
  },
  set typeFormat(value) {
    if (isRef(typeFormat)) typeFormat.value = value;
    else Object.assign(typeFormat, value);
  }
});

const searchParams = reactive({
  get operIp() {
    return unref(queryParams).operIp;
  },
  set operIp(value) {
    unref(queryParams).operIp = value;
  },
  get title() {
    return unref(queryParams).title;
  },
  set title(value) {
    unref(queryParams).title = value;
  },
  get operName() {
    return unref(queryParams).operName;
  },
  set operName(value) {
    unref(queryParams).operName = value;
  },
  get businessType() {
    return unref(queryParams).businessType;
  },
  set businessType(value) {
    unref(queryParams).businessType = value;
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
      label: td('sys.monitor.operlog.operAddr'),
      prop: 'operIp',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.operlog.operAddrPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.operlog.systemModule'),
      prop: 'title',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.operlog.systemModulePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.operlog.operPerson'),
      prop: 'operName',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.operlog.operPersonPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.operlog.type'),
      prop: 'businessType',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.operlog.operType'),
        clearable: true,
        options: (unref(sys_oper_type) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.operlog.operStatus'),
        clearable: true,
        options: (unref(sys_common_status) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('sys.monitor.operlog.operTime'),
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
    { type: 'selection', width: '50', align: 'center' },
    {
      sortableKey: 'oper_id',
      prop: 'operId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.monitor.operlog.logNo');
      },
      align: 'center',
      slot: 'operId'
    },
    {
      get label() {
        return td('sys.monitor.operlog.systemModule');
      },
      align: 'center',
      showOverflowTooltip: { effect: 'light' },
      slot: 'title'
    },
    {
      get label() {
        return td('sys.monitor.operlog.operType');
      },
      align: 'center',
      slot: 'businessType'
    },
    {
      get label() {
        return td('sys.monitor.operlog.operPerson');
      },
      align: 'center',
      width: '110',
      showOverflowTooltip: { effect: 'light' },
      sortable: 'custom',
      sortOrders: ['descending', 'ascending'],
      slot: 'operName',
      prop: 'operName',
      sortableKey: 'operName'
    },
    {
      get label() {
        return td('sys.monitor.operlog.operAddr');
      },
      align: 'center',
      width: '130',
      showOverflowTooltip: { effect: 'light' },
      slot: 'operIp'
    },
    {
      get label() {
        return td('sys.monitor.operlog.operStatus');
      },
      align: 'center',
      slot: 'status'
    },
    {
      get label() {
        return td('sys.monitor.operlog.operDate');
      },
      align: 'center',
      width: '180',
      sortable: 'custom',
      sortOrders: ['descending', 'ascending'],
      slot: 'operTime',
      prop: 'operTime',
      sortableKey: 'operTime'
    },
    {
      get label() {
        return td('sys.monitor.operlog.costTime');
      },
      align: 'center',
      width: '110',
      showOverflowTooltip: { effect: 'light' },
      sortable: 'custom',
      sortOrders: ['descending', 'ascending'],
      slot: 'costTime',
      prop: 'costTime',
      sortableKey: 'costTime'
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
  func: async () => ({ data: { rows: unref(operlogList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(operlogList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>
<style scoped lang="scss">
.form-readonly {
  width: 100%;
  border: 1px solid #c0c4cc;
  padding: 0px 10px;
  min-height: 34px;
}
</style>
