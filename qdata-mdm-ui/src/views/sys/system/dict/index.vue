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
          v-hasPermi="['system:dict:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:dict:edit']"
        >
          {{ td('common.button.update') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || batchDeleteDisabled"
          @click="handleDelete"
          v-hasPermi="['system:dict:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:dict:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
        <el-button type="primary" plain icon="Download" :disabled="multiple" @click="handleEnum">
          {{ td('common.button.download') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Refresh"
          @click="handleRefreshCache"
          v-hasPermi="['system:dict:remove']"
        >
          {{ td('sys.system.dictType.refreshCache') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #dictId="{ row }">{{ row.dictId }}</template>
          <template #cell3="{ row }">
            <router-link
              v-if="getDisplayTitle(row.dictType)"
              class="el-link el-link--primary qt-detail-link text-ellipsis"
              :title="String(row.dictType)"
              :to="'/system/dict-data/index/' + row.dictId"
              @click.stop
            >{{ row.dictType }}</router-link>
            <span v-else class="qt-detail-link text-ellipsis">-</span>
          </template>
          <template #status="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
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
              :disabled="!checkPermi(['system:dict:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:dict:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-popover
              placement="bottom"
              :width="110"
              trigger="click"
              popper-class="table-action-popover handle-popover"
              :disabled="
                false && (isDeleteDisabled(scope.row) || !checkPermi(['system:dict:remove']))
              "
            >
              <template #reference>
                <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
                  :disabled="
                    false && (isDeleteDisabled(scope.row) || !checkPermi(['system:dict:remove']))
                  "
                  >{{ td('common.button.more') }}</el-button
                >
              </template>
              <div class="butgdlist task-action-list">
                <el-button link type="primary" icon="Download" @click="handleEnum(scope.row)">
                  {{ td('common.button.download') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['system:dict:remove']"
                  :disabled="isDeleteDisabled(scope.row)"
                >
                  {{ td('common.button.delete') }}
                </el-button>
              </div>
            </el-popover>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <dict-type-form-dialog ref="formDialogRef" :ids="ids" @success="getList" />
    <dict-type-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Dict">
import { getDisplayTitle, tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import DictTypeDetailDialog from './components/dictTypeDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import useDictStore from '@/store/system/dict.js';
import {
  listType,
  getType,
  delType,
  addType,
  updateType,
  refreshCache
} from '@/api/system/system/dict/type.js';
import { genCode } from '@/api/system/tool/gen.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import DictTypeFormDialog from './components/dictTypeFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const typeList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const ids = ref([]);

const single = ref(true);

const multiple = ref(true);

const total = ref(0);

const dateRange = ref([]);

const dictTypes = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listType(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    typeList.value = response.rows;
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
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.dictId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
  dictTypes.value = selection.map((item) => item.dictType);
}

const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row?.status) === '0';
}

function handleDelete(row) {
  if (row?.dictId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const dictIds = row.dictId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.dictType.confirmDelete', { id: dictIds }))
    .then(function () {
      return delType(dictIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    'system/dict/type/export',
    {
      ...queryParams.value
    },
    `dict_${new Date().getTime()}.xlsx`
  );
}

function handleRefreshCache() {
  refreshCache().then(() => {
    proxy.$modal.msgSuccess(td('sys.system.dictType.refreshSuccess'));
    useDictStore().cleanDict();
  });
}

function handleEnum(row) {
  const dtNames = row.dictType || dictTypes.value;
  if (dtNames == '') {
    proxy.$modal.msgError(td('sys.system.dictType.selectDictToGenerate'));
    return;
  }

  proxy.$download.zip('/system/dict/type/batchDictData?dictTypes=' + dtNames, 'qdata-mdm.zip');
}

getList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get dictName() {
    return unref(queryParams).dictName;
  },
  set dictName(value) {
    unref(queryParams).dictName = value;
  },
  get dictType() {
    return unref(queryParams).dictType;
  },
  set dictType(value) {
    unref(queryParams).dictType = value;
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
      label: td('sys.system.dictType.dictName'),
      prop: 'dictName',
      component: {
        is: 'input',
        placeholder: td('sys.system.dictType.dictNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.dictType.dictType'),
      prop: 'dictType',
      component: {
        is: 'input',
        placeholder: td('sys.system.dictType.dictTypePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.dictType.dictStatus'),
        clearable: true,
        options: unref(sys_normal_disable).map((dict) => ({ label: dict.label, value: dict.value }))
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
      cellClassName: titleCellClass,
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
      prop: 'dictId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.system.dictType.dictNo');
      },
      align: 'center',
      slot: 'dictId'
    },
    {
      get label() {
        return td('sys.system.dictType.dictName');
      },
      align: 'left',
      showOverflowTooltip: true,
      prop: 'dictName',
      className: 'title-column',
      get link() { return tableTitleLink(row => proxy.$refs.detailDialogRef.openDialog(row), 'system:dict:query'); }
    },
    {
      get label() {
        return td('sys.system.dictType.dictType');
      },
      align: 'left',
      showOverflowTooltip: true,
      slot: 'cell3'
    },
    {
      get label() {
        return td('common.texts.status');
      },
      align: 'center',
      slot: 'status'
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
  func: async () => ({ data: { rows: unref(typeList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [typeList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
