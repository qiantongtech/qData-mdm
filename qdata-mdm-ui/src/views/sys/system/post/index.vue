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
          v-hasPermi="['system:post:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['system:post:edit']"
        >
          {{ td('common.button.update') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || batchDeleteDisabled"
          @click="handleDelete"
          v-hasPermi="['system:post:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['system:post:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #postId="{ row }">{{ row.postId }}</template>
          <template #postCode="{ row }">{{ row.postCode }}</template>
          <template #postSort="{ row }">{{ row.postSort }}</template>
          <template #status="scope">
            <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
          </template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['system:post:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:post:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:post:remove']"
              :disabled="isDeleteDisabled(scope.row)"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <post-form-dialog ref="formDialogRef" :ids="ids" @success="getList" />
    <post-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Post">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import PostDetailDialog from './components/postDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import useDefaultLang from '@/composables/useDefaultLang';
import { listPost, addPost, delPost, getPost, updatePost } from '@/api/system/system/post.js';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import PostFormDialog from './components/postFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_normal_disable } = proxy.useDict('sys_normal_disable');

const postList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const ids = ref([]);

const single = ref(true);

const multiple = ref(true);

const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    postCode: undefined,
    postName: undefined,
    status: undefined
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listPost(queryParams.value).then((response) => {
    postList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

function handleSelectionChange(selection) {
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.postId);
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
  if (row?.postId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const postIds = row.postId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.post.confirmDelete', { id: postIds }))
    .then(function () {
      return delPost(postIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    'system/post/export',
    {
      ...queryParams.value
    },
    `post_${new Date().getTime()}.xlsx`
  );
}

getList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get postCode() {
    return unref(queryParams).postCode;
  },
  set postCode(value) {
    unref(queryParams).postCode = value;
  },
  get postName() {
    return unref(queryParams).postName;
  },
  set postName(value) {
    unref(queryParams).postName = value;
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
      label: td('sys.system.post.postCode'),
      prop: 'postCode',
      component: {
        is: 'input',
        placeholder: td('sys.system.post.postCodePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.post.postName'),
      prop: 'postName',
      component: {
        is: 'input',
        placeholder: td('sys.system.post.postNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.post.postStatus'),
        clearable: true,
        options: unref(sys_normal_disable).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    table: {
      cellClassName: titleCellClass,
      defaultSort: { prop: 'postSort', order: 'ascending' },
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
      prop: 'postId',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.system.post.postNo');
      },
      align: 'center',
      slot: 'postId'
    },
    {
      get label() {
        return td('sys.system.post.postCode');
      },
      align: 'left',
      slot: 'postCode'
    },
    {
      get label() {
        return td('sys.system.post.postName');
      },
      align: 'left',
      prop: 'postName',
      className: 'title-column',
      get link() { return tableTitleLink(row => proxy.$refs.detailDialogRef.openDialog(row), 'system:post:query'); }
    },
    {
      sortableKey: 'post_sort',
      sortable: true,
      prop: 'postSort',
      get label() {
        return td('sys.system.post.postSort');
      },
      align: 'center',
      slot: 'postSort'
    },
    {
      get label() {
        return td('common.texts.status');
      },
      align: 'center',
      slot: 'status'
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
  func: async () => ({ data: { rows: unref(postList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [postList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
