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
        <el-button @click="readAllMsg" plain>
          <i class="iconfont-mini icon-a-zu22378 mr5" />
          {{ td('sys.system.message.setAllRead') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #category="scope">
            <dict-tag :options="message_category" :value="scope.row.category" />
          </template>
          <template #hasRead="scope">
            <el-tag :type="scope.row.hasRead ? 'success' : 'danger'">
              {{
                scope.row.hasRead ? td('sys.system.message.read') : td('sys.system.message.unread')
              }}
            </el-tag>
          </template>
          <template #content="{ row }">{{ row.content }}</template>
          <template #createTime="{ row }">{{ row.createTime }}</template>
          <template #action="scope">
            <el-button link type="primary" icon="View" @click="handleView(scope.row)">
              {{ td('common.button.details') }}
            </el-button>
            <el-button link type="danger" icon="Delete" @click="deleteMsg(scope.row.id)">
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <message-detail-dialog
      v-model:open="openView"
      :view-data="viewData"
      :message-category="message_category"
    />
  </div>
</template>

<script setup name="Message">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import { getCurrentInstance, ref } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import useUserStore from '@/store/system/user';
import {
  listMessage,
  delMessage,
  read,
  readAll,
  updateMessage
} from '@/api/system/system/message/message';
import { reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import MessageDetailDialog from './components/messageDetailDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const openView = ref(false);

const viewData = ref({});

const userStore = useUserStore();

const queryParams = ref({
  dateRange: [],
  pageNum: 1,
  pageSize: 10,
  receiverId: userStore.userId
});

const total = ref(0);

const loading = ref(false);

const showSearch = ref(true);

const msgList = ref([]);

const { proxy } = getCurrentInstance();

const { message_category } = proxy.useDict('message_category');

const handleQuery = () => {
  console.log(queryParams.value);
  getList();
};

const resetQuery = () => {
  queryParams.value = {};
};

const startTime = ref(null);

const endTime = ref(null);

const getList = () => {
  const reqData = {
    category: queryParams.value.category,
    receiverId: userStore.userId,
    pageNum: queryParams.value.pageNum,
    pageSize: queryParams.value.pageSize
  };
  if (queryParams.value.dateRange && queryParams.value.dateRange.length > 0) {
    ((reqData.startTime = queryParams.value.dateRange[0]),
      (reqData.endTime = queryParams.value.dateRange[1]));
  }
  listMessage(reqData).then((response) => {
    msgList.value = response.data.rows;
    total.value = response.data.total;
  });
};

getList();

function readAllMsg() {
  ElMessageBox.confirm(td('sys.system.message.confirmSetAllRead'))
    .then(() => {
      return readAll();
    })
    .then((res) => {
      console.log('------Mark as read----', res);
      getList();
      ElMessage.success(td('common.message.msgOpSuccess'));
    })
    .catch(() => {});
}

function deleteMsg(id) {
  ElMessageBox.confirm(td('sys.system.message.confirmDelete'))
    .then(() => {
      return delMessage(id);
    })
    .then(() => {
      getList();
      ElMessage.success(td('common.message.msgOpSuccess'));
    })
    .catch(() => {});
}
const searchParams = reactive({
  get category() {
    return unref(queryParams).category;
  },
  set category(value) {
    unref(queryParams).category = value;
  },
  get dateRange() {
    return unref(queryParams).dateRange;
  },
  set dateRange(value) {
    unref(queryParams).dateRange = value;
  }
});
const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.message.msgType'),
      prop: 'category',
      component: {
        is: 'select',
        placeholder: td('sys.system.message.msgTypePlaceholder'),
        clearable: true,
        options: unref(message_category).map((dict) => ({ label: dict.label, value: dict.value }))
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
      defaultSort: { prop: 'createTime', order: 'descending' }, stripe: true
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
    {
      get label() {
        return td('sys.system.message.msgTitle');
      },
      align: 'center',
      prop: 'title',
      className: 'title-column',
      link: tableTitleLink(row => handleView(row))
    },
    {
      get label() {
        return td('sys.system.message.msgType');
      },
      align: 'center',
      slot: 'category'
    },
    {
      get label() {
        return td('sys.system.message.isRead');
      },
      align: 'center',
      slot: 'hasRead'
    },
    {
      get label() {
        return td('sys.system.message.msgContent');
      },
      align: 'center',
      slot: 'content'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('common.texts.createdTime');
      },
      align: 'center',
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
  func: async () => ({ data: { rows: unref(msgList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [msgList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);

const handleView = (e) => {
  e.hasRead = '1';
  e.delFlag = 1;
  updateMessage(e);
  msgList.value = msgList.value.map((item) => {
    if (e.id == item.id) {
      return { ...item, hasRead: '1' }; // Create a new object and modify hasRead
    }
    return item; // Keep everything else unchanged
  });
  openView.value = true;
  viewData.value = e;
};
</script>
