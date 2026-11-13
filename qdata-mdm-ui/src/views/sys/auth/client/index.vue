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
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['auth:client:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #id="{ row }">{{ row.id }}</template>
          <template #secretKey="{ row }">{{ row.secretKey }}</template>
          <template #type="scope">
            <dict-tag :options="auth_app_type" :value="scope.row.type" />
          </template>
          <template #icon="scope">
            <image-preview :src="scope.row.icon" :width="50" :height="50" />
          </template>
          <template #homeUrl="scope">
            <span>{{ scope.row.homeUrl || '-' }}</span>
          </template>
          <template #redirectUrl="{ row }">{{ row.redirectUrl }}</template>
          <template #publicFlag="scope">
            <dict-tag :options="auth_public" :value="scope.row.publicFlag" />
          </template>
          <template #validFlag="scope">
            <dict-tag :options="sys_valid" :value="scope.row.validFlag" />
          </template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
          </template>
          <template #remark="scope">
            <span>{{ scope.row.remark || '-' }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['auth:client:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['auth:client:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-button
              link
              type="danger"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['auth:client:remove']"
              :disabled="isDeleteDisabled(scope.row)"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <client-form-dialog ref="clientFormDialogRef" :state="clientFormDialogState" />
    <client-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="Client">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import ClientDetailDialog from './components/clientDetailDialog.vue';
import { checkPermi } from '@/utils/permission';
import ClientFormDialog from './components/clientFormDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import useDefaultLang from '@/composables/useDefaultLang';
import {
  listClient,
  getClient,
  delClient,
  addClient,
  updateClient
} from '@/api/system/auth/client.js';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const submitLoading = ref(false);
const { auth_app_type } = proxy.useDict('auth_app_type');
const { sys_valid } = proxy.useDict('sys_valid');
const { auth_public } = proxy.useDict('auth_public');

const clientList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    id: null,
    secretKey: null,
    name: null,
    type: null,
    icon: null,
    homeUrl: null,
    syncUrl: null,
    redirectUrl: null,
    publicFlag: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  },
  rules: {
    secretKey: [
      { required: true, message: td('sys.client.clientSecretRequired'), trigger: 'blur' }
    ],
    name: [{ required: true, message: td('sys.client.clientNameRequired'), trigger: 'blur' }],
    type: [{ required: true, message: td('sys.client.clientTypeRequired'), trigger: 'change' }],
    redirectUrl: [
      { required: true, message: td('sys.client.authorizedUrlRequired'), trigger: 'blur' }
    ],
    publicFlag: [{ required: true, message: td('sys.client.isPublicRequired'), trigger: 'blur' }],
    validFlag: [{ required: true, message: td('sys.client.isActiveRequired'), trigger: 'blur' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** Query application management list */
function getList() {
  loading.value = true;
  listClient(queryParams.value).then((response) => {
    clientList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// Cancel button
function cancel() {
  open.value = false;
  reset();
}

// form reset
function reset() {
  form.value = {
    id: null,
    secretKey: null,
    name: null,
    type: null,
    icon: null,
    homeUrl: null,
    syncUrl: null,
    redirectUrl: null,
    publicFlag: '1',
    validFlag: '1',
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.$refs.clientFormDialogRef.dialogElement0?.resetFields();
}

/** Search button action */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** reset button action */
function resetQuery() {
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

// Multiple selection box selected data
function handleSelectionChange(selection) {
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** Add button operation */
function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.client.addTitle');
}

/** Modify button actions */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getClient(_id).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.client.editTitle');
  });
}

/** submit button */
function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs.clientFormDialogRef.dialogElement0.validate((valid) => {
    if (valid) {
      if (form.value.id != null) {
        updateClient(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess(td('common.message.editSuccess'));
            open.value = false;
            getList();
            submitLoading.value = false;
          })
          .catch(() => {
            submitLoading.value = false;
          });
      } else {
        addClient(form.value)
          .then((response) => {
            proxy.$modal.msgSuccess(td('common.message.addSuccess'));
            open.value = false;
            getList();
            submitLoading.value = false;
          })
          .catch(() => {
            submitLoading.value = false;
          });
      }
    } else {
      submitLoading.value = false;
    }
  });
}

/** Delete button action */
const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return ['1', 'true'].includes(String(row?.validFlag));
}

function handleDelete(row) {
  if (row?.id != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const _ids = row.id || ids.value;
  proxy.$modal
    .confirm(td('sys.client.confirmDelete', { id: _ids }))
    .then(function () {
      return delClient(_ids);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

/** Export button action */
function handleExport() {
  proxy.download(
    'auth/client/export',
    {
      ...queryParams.value
    },
    `client_${new Date().getTime()}.xlsx`
  );
}

getList();

const clientFormDialogState = reactive({
  get title() {
    return unref(title);
  },
  set title(value) {
    if (isRef(title)) title.value = value;
    else Object.assign(title, value);
  },
  get open() {
    return unref(open);
  },
  set open(value) {
    if (isRef(open)) open.value = value;
    else Object.assign(open, value);
  },
  get cancel() {
    return unref(cancel);
  },
  set cancel(value) {
    if (isRef(cancel)) cancel.value = value;
    else Object.assign(cancel, value);
  },
  get submitLoading() {
    return unref(submitLoading);
  },
  set submitLoading(value) {
    if (isRef(submitLoading)) submitLoading.value = value;
    else Object.assign(submitLoading, value);
  },
  get submitForm() {
    return unref(submitForm);
  },
  set submitForm(value) {
    if (isRef(submitForm)) submitForm.value = value;
    else Object.assign(submitForm, value);
  },
  get form() {
    return unref(form);
  },
  set form(value) {
    if (isRef(form)) form.value = value;
    else Object.assign(form, value);
  },
  get rules() {
    return unref(rules);
  },
  set rules(value) {
    if (isRef(rules)) rules.value = value;
    else Object.assign(rules, value);
  },
  get auth_app_type() {
    return unref(auth_app_type);
  },
  set auth_app_type(value) {
    if (isRef(auth_app_type)) auth_app_type.value = value;
    else Object.assign(auth_app_type, value);
  },
  get auth_public() {
    return unref(auth_public);
  },
  set auth_public(value) {
    if (isRef(auth_public)) auth_public.value = value;
    else Object.assign(auth_public, value);
  },
  get sys_valid() {
    return unref(sys_valid);
  },
  set sys_valid(value) {
    if (isRef(sys_valid)) sys_valid.value = value;
    else Object.assign(sys_valid, value);
  }
});

const searchParams = reactive({
  get id() {
    return unref(queryParams).id;
  },
  set id(value) {
    unref(queryParams).id = value;
  },
  get name() {
    return unref(queryParams).name;
  },
  set name(value) {
    unref(queryParams).name = value;
  }
});

const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.client.clientId'),
      prop: 'id',
      component: { is: 'input', placeholder: td('sys.client.clientIdPlaceholder'), clearable: true }
    },
    {
      label: td('sys.client.clientName'),
      prop: 'name',
      component: {
        is: 'input',
        placeholder: td('sys.client.clientNamePlaceholder'),
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
      cellClassName: titleCellClass,
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
    {
      prop: 'id',
      sortable: true,
      width: 60,
      get label() {
        return td('common.texts.number');
      },
      align: 'center',
      slot: 'id'
    },
    {
      get label() {
        return td('sys.client.clientSecret');
      },
      align: 'center',
      width: '300',
      slot: 'secretKey'
    },
    {
      get label() {
        return td('sys.client.clientName');
      },
      align: 'center',
      width: '120',
      prop: 'name',
      className: 'title-column',
      get link() { return tableTitleLink(row => proxy.$refs.detailDialogRef.openDialog(row), 'auth:client:query'); }
    },
    {
      get label() {
        return td('sys.client.clientType');
      },
      align: 'center',
      slot: 'type'
    },
    {
      get label() {
        return td('sys.client.clientIcon');
      },
      align: 'center',
      width: '100',
      slot: 'icon'
    },
    {
      get label() {
        return td('sys.client.clientHomepage');
      },
      align: 'center',
      slot: 'homeUrl'
    },
    {
      get label() {
        return td('sys.client.authorizedUrl');
      },
      align: 'center',
      width: '150',
      slot: 'redirectUrl'
    },
    {
      get label() {
        return td('sys.client.isPublic');
      },
      align: 'center',
      slot: 'publicFlag'
    },
    {
      get label() {
        return td('sys.client.isActive');
      },
      align: 'center',
      slot: 'validFlag'
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
  func: async () => ({ data: { rows: unref(clientList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(clientList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>

<style scoped lang="scss"></style>
