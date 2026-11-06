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
        <el-button type="warning" plain icon="Close" @click="handleClose">
          {{ td('common.button.close') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #dictCode="{ row }">{{ row.dictCode }}</template>
          <template #dictLabel="scope">
            <span
              v-if="
                (scope.row.listClass == '' || scope.row.listClass == 'default') &&
                (scope.row.cssClass == '' || scope.row.cssClass == null)
              "
            >
              {{ scope.row.dictLabel }}
            </span>
            <el-tag
              v-else
              :type="scope.row.listClass == 'primary' ? '' : scope.row.listClass"
              :class="scope.row.cssClass"
            >
              {{ scope.row.dictLabel }}
            </el-tag>
          </template>
          <template #dictValue="{ row }">{{ row.dictValue }}</template>
          <template #dictSort="{ row }">{{ row.dictSort }}</template>
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
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:dict:edit']"
            >
              {{ td('common.button.update') }}
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
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <dict-data-form-dialog
      ref="formDialogRef"
      :query-params="queryParams"
      :ids="ids"
      @success="getList"
    />
  </div>
</template>

<script setup name="Data">
import useDefaultLang from '@/composables/useDefaultLang';
import useDictStore from '@/store/system/dict.js';
import { optionselect as getDictOptionselect, getType } from '@/api/system/system/dict/type.js';
import { listData, getData, delData, addData, updateData } from '@/api/system/system/dict/data.js';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import DictDataFormDialog from './components/dictDataFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const dataList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const ids = ref([]);

const single = ref(true);

const multiple = ref(true);

const total = ref(0);

const defaultDictType = ref('');

const typeOptions = ref([]);

const route = useRoute();

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictType: undefined,
    dictLabel: undefined,
    status: undefined
  }
});
const { queryParams } = toRefs(data);

function getTypes(dictId) {
  getType(dictId).then((response) => {
    queryParams.value.dictType = response.data.dictType;
    defaultDictType.value = response.data.dictType;
    getList();
  });
}

function getTypeList() {
  getDictOptionselect().then((response) => {
    typeOptions.value = response.data;
  });
}

function getList() {
  loading.value = true;
  listData(queryParams.value).then((response) => {
    dataList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function handleClose() {
  const obj = { path: '/system/dict' };
  proxy.$tab.closeOpenPage(obj);
}

function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  queryParams.value.dictType = defaultDictType.value;
  handleQuery();
}

function handleSelectionChange(selection) {
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.dictCode);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row?.status) === '0';
}

function handleDelete(row) {
  if (row?.dictCode != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const dictCodes = row.dictCode || ids.value;
  proxy.$modal
    .confirm(td('sys.system.dictData.confirmDelete', { code: dictCodes }))
    .then(function () {
      return delData(dictCodes);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
      useDictStore().removeDict(queryParams.value.dictType);
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    'system/dict/data/export',
    {
      ...queryParams.value
    },
    `dict_data_${new Date().getTime()}.xlsx`
  );
}

getTypes(route.params && route.params.dictId);

getTypeList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get dictType() {
    return unref(queryParams).dictType;
  },
  set dictType(value) {
    unref(queryParams).dictType = value;
  },
  get dictLabel() {
    return unref(queryParams).dictLabel;
  },
  set dictLabel(value) {
    unref(queryParams).dictLabel = value;
  },
  get status() {
    return unref(queryParams).status;
  },
  set status(value) {
    unref(queryParams).status = value;
  }
});
const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.dictData.dictName'),
      prop: 'dictType',
      component: {
        is: 'select',
        clearable: false,
        options: unref(typeOptions).map((item) => ({ label: item.dictName, value: item.dictType }))
      }
    },
    {
      label: td('sys.system.dictData.dictLabel'),
      prop: 'dictLabel',
      component: {
        is: 'input',
        placeholder: td('sys.system.dictData.dictLabelPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.dictData.dataStatus'),
        clearable: true,
        options: unref(sys_normal_disable).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    table: {
      defaultSort: { prop: 'dictSort', order: 'ascending' },
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
      get label() {
        return td('sys.system.dictData.dictCode');
      },
      align: 'center',
      slot: 'dictCode'
    },
    {
      get label() {
        return td('sys.system.dictData.dictLabel');
      },
      align: 'center',
      slot: 'dictLabel'
    },
    {
      get label() {
        return td('sys.dictData.dictValue');
      },
      align: 'center',
      slot: 'dictValue'
    },
    {
      sortableKey: 'dict_sort',
      sortable: true,
      prop: 'dictSort',
      get label() {
        return td('sys.dictData.dictSort');
      },
      align: 'center',
      slot: 'dictSort'
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
  func: async () => ({ data: { rows: unref(dataList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [dataList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
