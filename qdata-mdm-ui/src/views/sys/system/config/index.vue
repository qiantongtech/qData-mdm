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
          v-hasPermi="['system:config:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:config:edit']"
        >
          {{ td('common.button.update') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:config:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:config:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Refresh"
          @click="handleRefreshCache"
          v-hasPermi="['system:config:remove']"
        >
          {{ td('sys.system.config.refreshCache') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #configId="{ row }">{{ row.configId }}</template>
          <template #configName="{ row }">{{ row.configName }}</template>
          <template #configKey="{ row }">{{ row.configKey }}</template>
          <template #configValue="scope">
            <span>{{ scope.row.configValue || '-' }}</span>
          </template>
          <template #configType="scope">
            <dict-tag :options="sys_yes_no" :value="scope.row.configType" />
          </template>
          <template #remark="{ row }">{{ row.remark }}</template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['system:config:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:config:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:config:remove']"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <config-form-dialog ref="formDialogRef" :ids="ids" @success="getList" />
    <config-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Config">
import ConfigDetailDialog from './components/configDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import {
  listConfig,
  getConfig,
  delConfig,
  addConfig,
  updateConfig,
  refreshCache
} from '@/api/system/system/config.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import ConfigFormDialog from './components/configFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_yes_no } = proxy.useDict('sys_yes_no');

const configList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const ids = ref([]);

const single = ref(true);

const multiple = ref(true);

const total = ref(0);

const dateRange = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    configName: undefined,
    configKey: undefined,
    configType: undefined
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listConfig(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    configList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  dateRange.value = [];
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.configId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleDelete(row) {
  const configIds = row.configId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.config.confirmDelete', { id: configIds }))
    .then(function () {
      return delConfig(configIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    'system/config/export',
    {
      ...queryParams.value
    },
    `config_${new Date().getTime()}.xlsx`
  );
}

function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess(td('sys.system.config.refreshCacheSuccess'));
  });
}

getList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get configName() {
    return unref(queryParams).configName;
  },
  set configName(value) {
    unref(queryParams).configName = value;
  },
  get configKey() {
    return unref(queryParams).configKey;
  },
  set configKey(value) {
    unref(queryParams).configKey = value;
  },
  get configType() {
    return unref(queryParams).configType;
  },
  set configType(value) {
    unref(queryParams).configType = value;
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
      label: td('sys.system.config.paramName'),
      prop: 'configName',
      component: {
        is: 'input',
        placeholder: td('sys.system.config.paramNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.config.paramKey'),
      prop: 'configKey',
      component: {
        is: 'input',
        placeholder: td('sys.system.config.paramKeyPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.config.systemBuiltIn'),
      prop: 'configType',
      component: {
        is: 'select',
        placeholder: td('sys.system.config.systemBuiltIn'),
        clearable: true,
        options: unref(sys_yes_no).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    },
    {
      label: td('common.texts.createdTime'),
      prop: 'dateRange',
      component: {
        is: 'date-picker',
        valueFormat: 'YYYY-MM-DD',
        type: 'daterange',
        rangeSeparator: '-',
        startPlaceholder: td('common.form.startDatePlaceholder'),
        endPlaceholder: td('common.form.endDatePlaceholder'),
        clearable: false
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '60vh',
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: document.body.clientWidth < 992 ? 5 : 7,
      params: {
        pageNum: unref(queryParams).pageNum ?? 1,
        pageSize: unref(queryParams).pageSize ?? 10
      }
    }
  })),
  columns: [
    { type: 'selection', width: '55', align: 'center' },
    {
      prop: 'configId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.system.config.paramPrimaryKey');
      },
      align: 'center',
      slot: 'configId'
    },
    {
      get label() {
        return td('sys.system.config.paramName');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'configName'
    },
    {
      get label() {
        return td('sys.system.config.paramKey');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'configKey'
    },
    {
      get label() {
        return td('sys.system.config.paramValue');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'configValue'
    },
    {
      get label() {
        return td('sys.system.config.systemBuiltIn');
      },
      align: 'center',
      slot: 'configType'
    },
    {
      get label() {
        return td('common.texts.remark');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'remark'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('common.texts.createdTime');
      },
      align: 'center',
      width: '180',
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
  func: async () => ({ data: { rows: unref(configList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [configList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
