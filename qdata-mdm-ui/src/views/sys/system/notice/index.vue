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
          v-hasPermi="['system:notice:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || batchDeleteDisabled"
          @click="handleDelete"
          v-hasPermi="['system:notice:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #noticeId="{ row }">{{ row.noticeId }}</template>
          <template #noticeTitle="{ row }">{{ row.noticeTitle }}</template>
          <template #noticeType="scope">
            <dict-tag :options="sys_notice_type" :value="scope.row.noticeType" />
          </template>
          <template #topFlag="scope">
            <dict-tag :options="sys_is_or_not" :value="scope.row.topFlag" />
          </template>
          <template #alertFlag="scope">
            <dict-tag :options="sys_is_or_not" :value="scope.row.alertFlag" />
          </template>
          <template #cell6="scope">
            <span>
              {{ parseTime(scope.row.alertStartTime, '{y}-{m}-{d}') }}
              ~
              {{ parseTime(scope.row.alertEndTime, '{y}-{m}-{d}') }}
            </span>
          </template>
          <template #status="scope">
            <dict-tag :options="sys_notice_status" :value="scope.row.status" />
          </template>
          <template #createBy="{ row }">{{ row.createBy }}</template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleView(scope.row)"
              v-hasPermi="['system:notice:detail']"
            >
              {{ td('common.button.details') }}
            </el-button>
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:notice:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:notice:remove']"
              :disabled="isDeleteDisabled(scope.row)"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <notice-form-dialog ref="formDialogRef" :ids="ids" @success="getList" />
  </div>
</template>

<script setup name="Notice">
import {
  listNotice,
  getNoticeOne,
  delNotice,
  addNotice,
  updateNotice
} from '@/api/system/system/notice.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import NoticeFormDialog from './components/noticeFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { sys_notice_status, sys_notice_type, sys_is_or_not } = proxy.useDict(
  'sys_notice_status',
  'sys_notice_type',
  'sys_is_or_not'
);

const router = useRouter();

const noticeList = ref([]);

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
    noticeTitle: undefined,
    createBy: undefined,
    status: undefined,
    orderByColumn: 'createTime',
    isAsc: 'descending'
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listNotice(queryParams.value).then((response) => {
    noticeList.value = [...(response.rows || [])].sort(
      (a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
    );
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
  ids.value = selection.map((item) => item.noticeId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleView(row) {
  router.push({ path: '/sys/system/notice/detail', query: { id: row.noticeId } });
}

const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row?.status) === '0';
}

function handleDelete(row) {
  if (row?.noticeId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const noticeIds = row.noticeId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.notice.confirmDelete', { id: noticeIds }))
    .then(function () {
      return delNotice(noticeIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

getList();
function handleAdd(row) {
  return formDialogRef.value.openDialog(row);
}
function handleUpdate(row) {
  return formDialogRef.value.openDialog(row, true);
}

const searchParams = reactive({
  get noticeTitle() {
    return unref(queryParams).noticeTitle;
  },
  set noticeTitle(value) {
    unref(queryParams).noticeTitle = value;
  },
  get createBy() {
    return unref(queryParams).createBy;
  },
  set createBy(value) {
    unref(queryParams).createBy = value;
  },
  get noticeType() {
    return unref(queryParams).noticeType;
  },
  set noticeType(value) {
    unref(queryParams).noticeType = value;
  }
});
const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.notice.noticeTitle'),
      prop: 'noticeTitle',
      component: {
        is: 'input',
        placeholder: td('sys.system.notice.noticeTitlePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.notice.operPerson'),
      prop: 'createBy',
      component: {
        is: 'input',
        placeholder: td('sys.system.notice.operPersonPlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.notice.type'),
      prop: 'noticeType',
      component: {
        is: 'select',
        placeholder: td('sys.system.notice.noticeType'),
        clearable: true,
        options: unref(sys_notice_type).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
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
      prop: 'noticeId',
      sortable: true,
      get label() {
        return td('common.display.index');
      },
      align: 'center',
      width: 60,
      slot: 'noticeId'
    },
    {
      get label() {
        return td('sys.system.notice.noticeTitle');
      },
      align: 'center',
      showOverflowTooltip: { effect: 'light' },
      slot: 'noticeTitle'
    },
    {
      get label() {
        return td('sys.system.notice.noticeType');
      },
      align: 'center',
      width: '120',
      slot: 'noticeType'
    },
    {
      get label() {
        return td('sys.system.notice.isTop');
      },
      align: 'center',
      width: '100',
      slot: 'topFlag'
    },
    {
      get label() {
        return td('sys.system.notice.popup');
      },
      align: 'center',
      width: '100',
      slot: 'alertFlag'
    },
    {
      get label() {
        return td('sys.system.notice.effectTime');
      },
      align: 'center',
      width: '220',
      slot: 'cell6'
    },
    {
      get label() {
        return td('common.texts.status');
      },
      align: 'center',
      width: '100',
      slot: 'status'
    },
    {
      get label() {
        return td('sys.system.notice.creator');
      },
      align: 'center',
      width: '100',
      slot: 'createBy'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('common.texts.createdTime');
      },
      align: 'center',
      width: '100',
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
  func: async () => ({ data: { rows: unref(noticeList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [noticeList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
