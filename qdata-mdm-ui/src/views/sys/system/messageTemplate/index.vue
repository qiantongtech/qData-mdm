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
          @click="handleAdd"
          v-hasPermi="['system:messageTemplate:add']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5" />
          {{ td('common.button.add') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #id="{ row }">{{ row.id }}</template>
          <template #title="scope">
            {{ scope.row.title || '-' }}
          </template>
          <template #content="scope">
            {{ scope.row.content || '-' }}
          </template>
          <template #category="scope">
            <dict-tag :options="message_category" :value="scope.row.category" />
          </template>
          <template #msgLevel="scope">
            <dict-tag :options="message_level" :value="scope.row.msgLevel" />
          </template>
          <template #createBy="scope">
            {{ scope.row.createBy || '-' }}
          </template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
          </template>
          <template #remark="scope">
            {{ scope.row.remark || '-' }}
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['system:message:messageTemplate:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['system:messageTemplate:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['system:messageTemplate:remove']"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <message-template-form-dialog ref="formDialogRef" :ids="ids" @success="getList" />
    <message-template-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="MessageTemplate">
import MessageTemplateDetailDialog from './components/messageTemplateDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import useDefaultLang from '@/composables/useDefaultLang';
import {
  listMessageTemplate,
  getMessageTemplate,
  delMessageTemplate,
  addMessageTemplate,
  updateMessageTemplate
} from '@/api/system/system/message/messageTemplate';
import { ref, reactive, computed, watch, unref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import MessageTemplateFormDialog from './components/messageTemplateFormDialog.vue';
const tableRef = ref(null);
// 工具栏刷新仍走原有接口；QtTable 使用原列表数据展示和分页。
const tableActions = { getList: () => getList() };
const formDialogRef = ref(null);
const { td } = useDefaultLang();

const { proxy } = getCurrentInstance();

const { message_category, message_level } = proxy.useDict('message_category', 'message_level');

const messageTemplateList = ref([]);

const loading = ref(true);

const showSearch = ref(true);

const ids = ref([]);

const total = ref(0);

const title = ref('');

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: null,
    category: null,
    msgLevel: null
  }
});
const { queryParams } = toRefs(data);

function getList() {
  loading.value = true;
  listMessageTemplate(queryParams.value).then((response) => {
    messageTemplateList.value = response.data.rows;
    total.value = response.data.total;
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

function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(td('sys.system.messageTemplate.confirmDelete', { id: _ids }))
    .then(function () {
      return delMessageTemplate(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function handleExport() {
  proxy.download(
    'system/messageTemplate/export',
    {
      ...queryParams.value
    },
    `messageTemplate_${new Date().getTime()}.xlsx`
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
  get title() {
    return unref(queryParams).title;
  },
  set title(value) {
    unref(queryParams).title = value;
  },
  get category() {
    return unref(queryParams).category;
  },
  set category(value) {
    unref(queryParams).category = value;
  },
  get msgLevel() {
    return unref(queryParams).msgLevel;
  },
  set msgLevel(value) {
    unref(queryParams).msgLevel = value;
  }
});
const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.messageTemplate.msgTitle'),
      prop: 'title',
      component: {
        is: 'input',
        placeholder: td('sys.system.messageTemplate.msgTitlePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.system.messageTemplate.msgCategory'),
      prop: 'category',
      component: {
        is: 'select',
        placeholder: td('sys.system.messageTemplate.selectPlaceholder'),
        clearable: false,
        options: unref(message_category).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    },
    {
      label: td('sys.system.messageTemplate.msgLevel'),
      prop: 'msgLevel',
      component: {
        is: 'select',
        placeholder: td('sys.system.messageTemplate.selectPlaceholder'),
        clearable: false,
        options: unref(message_level).map((dict) => ({ label: dict.label, value: dict.value }))
      }
    }
  ])
});

const tableStore = reactive({
  config: computed(() => ({
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '60vh'
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
      prop: 'id',
      sortable: true,
      width: 60,
      get label() {
        return td('sys.system.messageTemplate.templateId');
      },
      align: 'center',
      slot: 'id'
    },
    {
      get label() {
        return td('sys.system.messageTemplate.msgTitle');
      },
      align: 'center',
      slot: 'title'
    },
    {
      get label() {
        return td('sys.system.messageTemplate.msgTemplateContent');
      },
      align: 'center',
      width: '180',
      showOverflowTooltip: true,
      slot: 'content'
    },
    {
      get label() {
        return td('sys.system.messageTemplate.msgCategory');
      },
      align: 'center',
      slot: 'category'
    },
    {
      get label() {
        return td('sys.system.messageTemplate.msgLevel');
      },
      align: 'center',
      slot: 'msgLevel'
    },
    {
      get label() {
        return td('common.texts.createdBy');
      },
      align: 'center',
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
      width: '180',
      slot: 'createTime'
    },
    {
      get label() {
        return td('common.texts.remark');
      },
      align: 'center',
      slot: 'remark'
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
  func: async () => ({ data: { rows: unref(messageTemplateList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination, onPageSizeChange: handleTablePagination }
});
function handleTablePagination(params) {
  Object.assign(unref(queryParams), params);
  getList();
}

watch(
  [messageTemplateList, total, () => unref(queryParams).pageNum, () => unref(queryParams).pageSize],
  () => tableRef.value?.resetQuery(),
  { flush: 'post' }
);
</script>
