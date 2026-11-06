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
  <div ref="app-container" class="app-container sys-dialog-host">
    <!-- Import table -->
    <el-dialog
      :title="td('sys.tool.genImport.title')"
      v-model="visible"
      width="800px"
      top="5vh"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
        <template #search>
          <qt-search-bar
            ref="queryRef"
            v-bind="searchStore"
            :params="searchParams"
            @query="handleQuery"
            @reset="resetQuery"
          />
        </template>
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #tableName="{ row }">{{ row.tableName }}</template>
          <template #tableComment="{ row }">{{ row.tableComment }}</template>
          <template #createTime="{ row }">{{ row.createTime }}</template>
          <template #updateTime="{ row }">{{ row.updateTime }}</template>
        </qt-table>
      </qt-wrap>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" @click="handleImportTable">{{
            td('common.button.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import useDefaultLang from '@/composables/useDefaultLang';
import { listDbTable, importTable } from '@/api/system/tool/gen.js';

const { td } = useDefaultLang();
const total = ref(0);
const visible = ref(false);
const tables = ref([]);
const dbTableList = ref([]);
const { proxy } = getCurrentInstance();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  tableComment: undefined
});

const emit = defineEmits(['ok']);

/** Query parameter list */
function show() {
  getList();
  visible.value = true;
}

/** Click to select row */
function clickRow(row) {
  qtTableRef0.value.elTableRef.toggleRowSelection(row);
}

/** Multiple selection box selected data */
function handleSelectionChange(selection) {
  tables.value = selection.map((item) => item.tableName);
}

/** Query table data */
function getList() {
  listDbTable(queryParams).then((res) => {
    dbTableList.value = res.rows;
    total.value = res.total;
  });
}

/** Search button action */
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

/** reset button action */
function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

/** Import button actions */
function handleImportTable() {
  const tableNames = tables.value.join(',');
  if (tableNames == '') {
    proxy.$modal.msgError(td('sys.tool.genImport.selectTableToImport'));
    return;
  }
  importTable({ tables: tableNames }).then((res) => {
    proxy.$modal.msgSuccess(res.msg);
    if (res.code === 200) {
      visible.value = false;
      emit('ok');
    }
  });
}

defineExpose({
  show
});

const searchParams = reactive({
  get tableName() {
    return unref(queryParams).tableName;
  },
  set tableName(value) {
    unref(queryParams).tableName = value;
  }
});

const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.tool.genImport.tableName'),
      prop: 'tableName',
      component: {
        is: 'input',
        placeholder: td('sys.tool.genImport.tableNamePlaceholder'),
        clearable: true
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
      onRowClick: clickRow,
      onSelectionChange: handleSelectionChange,
      height: '380px'
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: 5,
      params: { pageNum: unref(queryParams).pageNum, pageSize: unref(queryParams).pageSize }
    }
  })),
  columns: [
    { type: 'selection', width: '55' },
    {
      get label() {
        return td('sys.tool.genImport.tableName');
      },
      showOverflowTooltip: true,
      slot: 'tableName',
      align: 'left'
    },
    {
      get label() {
        return td('sys.tool.genImport.tableDesc');
      },
      showOverflowTooltip: true,
      slot: 'tableComment',
      align: 'left'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('common.texts.createdTime');
      },
      slot: 'createTime',
      align: 'left'
    },
    {
      get label() {
        return td('common.texts.updatedTime');
      },
      slot: 'updateTime',
      align: 'left'
    }
  ],
  func: async () => ({ data: { rows: unref(dbTableList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(dbTableList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>

<style scoped lang="scss">
.sys-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}

.column-form {
  display: flex;
  flex-direction: column;
}

.row-full {
  width: 100%;
  margin-right: 0;
}

.sys-panel {
  padding: 15px;
  background: var(--qt-surface-background);
  border-radius: var(--qt-radius-small);
}

.sys-panel-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 15px;
}
</style>
