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
          type="primary"
          plain
          icon="Download"
          :disabled="multiple"
          @click="handleGenTable"
          v-hasPermi="['tool:gen:code']"
        >
          {{ td('sys.tool.genIndex.generate') }}
        </el-button>
        <el-button type="primary" plain icon="Plus" @click="openCreateTable" v-hasRole="['admin']">
          {{ td('sys.tool.genIndex.create') }}
        </el-button>
        <el-button
          type="info"
          plain
          icon="Upload"
          @click="openImportTable"
          v-hasPermi="['tool:gen:import']"
        >
          {{ td('common.button.import') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleEditTable"
          v-hasPermi="['tool:gen:edit']"
        >
          {{ td('common.button.update') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['tool:gen:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #cell1="scope">
            <span>{{ (queryParams.pageNum - 1) * queryParams.pageSize + scope.$index + 1 }}</span>
          </template>
          <template #tableName="{ row }">{{ row.tableName }}</template>
          <template #tableComment="{ row }">{{ row.tableComment }}</template>
          <template #className="{ row }">{{ row.className }}</template>
          <template #createTime="{ row }">{{ row.createTime }}</template>
          <template #updateTime="{ row }">{{ row.updateTime }}</template>
          <template #action="scope">
            <el-tooltip :content="td('common.button.details')" placement="top">
              <el-button
                link
                type="primary"
                icon="View"
                @click="handlePreview(scope.row)"
                v-hasPermi="['tool:gen:preview']"
                >{{ td('common.button.details') }}</el-button
              >
            </el-tooltip>
            <el-tooltip :content="td('common.button.update')" placement="top">
              <el-button
                link
                type="primary"
                icon="Edit"
                @click="handleEditTable(scope.row)"
                v-hasPermi="['tool:gen:edit']"
                >{{ td('common.button.update') }}</el-button
              >
            </el-tooltip>
            <el-popover
              placement="bottom"
              :width="110"
              trigger="click"
              popper-class="table-action-popover handle-popover"
              :disabled="!checkPermi(['tool:gen:code']) && !checkPermi(['tool:gen:remove'])"
            >
              <template #reference>
                <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
                  :disabled="!checkPermi(['tool:gen:code']) && !checkPermi(['tool:gen:remove'])"
                  >{{ td('common.button.more') }}
                </el-button>
              </template>
              <div class="butgdlist task-action-list">
                <el-button
                  link
                  type="primary"
                  icon="Download"
                  @click="handleGenTable(scope.row)"
                  v-hasPermi="['tool:gen:code']"
                  >{{ td('sys.tool.genIndex.genCode') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['tool:gen:remove']"
                  >{{ td('common.button.delete') }}
                </el-button>
              </div>
            </el-popover>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <gen-preview-dialog ref="genPreviewDialogRef" :state="genPreviewDialogState" />
    <import-table ref="importRef" @ok="handleQuery" />
    <create-table ref="createRef" @ok="handleQuery" />
  </div>
</template>

<script setup name="Gen">
import { checkPermi } from '@/utils/permission';
import GenPreviewDialog from './components/genPreviewDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import { listTable, previewTable, delTable, genCode, synchDb } from '@/api/system/tool/gen.js';
import router from '@/router/index.js';
import importTable from './components/importTableDialog.vue';
import createTable from './components/createTableDialog.vue';
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const route = useRoute();
const { proxy } = getCurrentInstance();

const tableList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const tableNames = ref([]);
const dateRange = ref([]);
const uniqueId = ref('');

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    tableName: undefined,
    tableComment: undefined
  },
  preview: {
    open: false,
    title: td('sys.tool.genIndex.codePreview'),
    data: {},
    activeName: 'do.java'
  }
});

const { queryParams, preview } = toRefs(data);

onActivated(() => {
  const time = route.query.t;
  if (time != null && time != uniqueId.value) {
    uniqueId.value = time;
    queryParams.value.pageNum = Number(route.query.pageNum);
    dateRange.value = [];
    proxy.resetForm('queryForm');
    getList();
  }
});

/** Query table collection */
function getList() {
  loading.value = true;
  listTable(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    tableList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** Search button action */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** Generate code operations */
function handleGenTable(row) {
  const tbNames = row.tableName || tableNames.value;
  if (tbNames == '') {
    proxy.$modal.msgError(td('sys.tool.genIndex.selectDataToGenerate'));
    return;
  }
  if (row.genType === '1') {
    genCode(row.tableName).then((response) => {
      proxy.$modal.msgSuccess(
        td('sys.tool.genIndex.genSuccessToCustomPath', { path: row.genPath })
      );
    });
  } else {
    proxy.$download.zip('/tool/gen/batchGenCode?tables=' + tbNames, 'qdata-mdm.zip');
  }
}

/** Synchronous database operations */
function handleSynchDb(row) {
  const tableName = row.tableName;
  proxy.$modal
    .confirm(td('sys.tool.genIndex.confirmSyncTable', { name: tableName }))
    .then(function () {
      return synchDb(tableName);
    })
    .then(() => {
      proxy.$modal.msgSuccess(td('sys.tool.genIndex.syncSuccess'));
    })
    .catch(() => {});
}

/** Open the import table pop-up window */
function openImportTable() {
  proxy.$refs['importRef'].show();
}

/** Open the create table pop-up window */
function openCreateTable() {
  proxy.$refs['createRef'].show();
}

/** reset button action */
function resetQuery() {
  dateRange.value = [];
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

/** preview button */
function handlePreview(row) {
  previewTable(row.tableId).then((response) => {
    preview.value.data = response.data;
    preview.value.open = true;
    preview.value.activeName = 'do.java';
  });
}

/** Copied code successfully */
function copyTextSuccess() {
  proxy.$modal.msgSuccess(td('common.message.copySuccess'));
}

// Multiple selection box selected data
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.tableId);
  tableNames.value = selection.map((item) => item.tableName);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** Modify button actions */
function handleEditTable(row) {
  const tableId = row.tableId || ids.value[0];
  router.push({
    path: '/tool/gen-edit/index/' + tableId,
    query: { pageNum: queryParams.value.pageNum }
  });
}

/** Delete button action */
function handleDelete(row) {
  const tableIds = row.tableId || ids.value;
  proxy.$modal
    .confirm(td('sys.tool.genIndex.confirmDelete', { ids: tableIds }))
    .then(function () {
      return delTable(tableIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

getList();

const genPreviewDialogState = reactive({
  get preview() {
    return unref(preview);
  },
  set preview(value) {
    if (isRef(preview)) preview.value = value;
    else Object.assign(preview, value);
  },
  get copyTextSuccess() {
    return unref(copyTextSuccess);
  },
  set copyTextSuccess(value) {
    if (isRef(copyTextSuccess)) copyTextSuccess.value = value;
    else Object.assign(copyTextSuccess, value);
  }
});

const searchParams = reactive({
  get tableName() {
    return unref(queryParams).tableName;
  },
  set tableName(value) {
    unref(queryParams).tableName = value;
  },
  get tableComment() {
    return unref(queryParams).tableComment;
  },
  set tableComment(value) {
    unref(queryParams).tableComment = value;
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
      label: td('sys.tool.genIndex.tableName'),
      prop: 'tableName',
      component: {
        is: 'input',
        placeholder: td('sys.tool.genIndex.tableNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.tool.genIndex.tableDesc'),
      prop: 'tableComment',
      component: {
        is: 'input',
        placeholder: td('sys.tool.genIndex.tableDescPlaceholder'),
        clearable: true
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

function handleTablePagination0(params) {
  unref(queryParams).pageNum = params.pageNum;
  unref(queryParams).pageSize = params.pageSize;
  getList();
}

const qtTableRef0 = ref(null);

const tableStore0 = reactive({
  config: computed(() => ({
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '60vh',
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: 5,
      params: { pageNum: unref(queryParams).pageNum, pageSize: unref(queryParams).pageSize }
    }
  })),
  columns: [
    { type: 'selection', align: 'center', width: '55' },
    {
      sortable: true,
      get label() {
        return td('sys.tool.genIndex.index');
      },
      type: 'index',
      width: 60,
      align: 'center',
      slot: 'cell1'
    },
    {
      get label() {
        return td('sys.tool.genIndex.tableName');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'tableName'
    },
    {
      get label() {
        return td('sys.tool.genIndex.tableDesc');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'tableComment'
    },
    {
      get label() {
        return td('sys.tool.genIndex.entity');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'className'
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
        return td('common.texts.updatedTime');
      },
      align: 'center',
      width: '160',
      slot: 'updateTime'
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
  func: async () => ({ data: { rows: unref(tableList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(tableList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>
<style scoped>
.precont {
  padding: 0px 15px;
  height: 444px;
  overflow-y: auto;
}
</style>
