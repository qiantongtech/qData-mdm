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
  <div ref="app-container" class="app-container select-user-dialog-host">
    <el-dialog
      v-model="visible"
      :title="td('sys.system.roleAuth.selectUser')"
      width="800px"
      append-to-body
      draggable
      destroy-on-close
      class="dialog"
      :append-to="$refs['app-container']"
      @opened="getList"
      @closed="clearSelection"
    >
      <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
        <template #search>
          <qt-search-bar
            v-bind="searchStore"
            :params="tableStore.params"
            @query="handleQuery"
            @reset="resetQuery"
          />
        </template>
        <qt-table v-bind="tableStore" ref="tableRef">
          <template #status="{ row }">
            <dict-tag :options="sys_normal_disable" :value="row.status" />
          </template>
          <template #createTime="{ row }">
            <span>{{ parseTime(row.createTime) }}</span>
          </template>
        </qt-table>
      </qt-wrap>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSelectUser">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SelectUser">
import { computed, getCurrentInstance, reactive, ref } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { authUserSelectAll, unallocatedUserList } from '@/api/system/system/role.js';

const props = defineProps({ roleId: { type: [Number, String] } });
const emit = defineEmits(['ok']);
const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const visible = ref(false);
const submitting = ref(false);
const tableRef = ref(null);
const userIds = ref([]);
const pageParams = reactive({ pageNum: 1, pageSize: 10 });
const queryParams = reactive({ roleId: undefined, userName: undefined, phonenumber: undefined });

const tableStore = reactive({
  config: {
    // 弹窗打开、表格挂载后再查询，关闭期间不请求用户列表。
    initResquest: false,
    fullContent: false,
    actions: { show: false },
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      height: '260px',
      rowKey: 'userId',
      onRowClick: clickRow,
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: 5,
      notAutoScroll: true,
      params: computed(() => ({ ...pageParams }))
    }
  },
  columns: [
    { type: 'selection', width: 55 },
    {
      prop: 'userName',
      get label() {
        return td('sys.system.roleAuth.userName');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'nickName',
      get label() {
        return td('sys.system.roleAuth.userNickName');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'email',
      get label() {
        return td('sys.system.roleAuth.email');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      prop: 'phonenumber',
      get label() {
        return td('sys.system.roleAuth.mobile');
      },
      align: 'left',
      showOverflowTooltip: true
    },
    {
      slot: 'status',
      get label() {
        return td('common.texts.status');
      }
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      slot: 'createTime',
      get label() {
        return td('common.texts.createdTime');
      },
      width: 180
    }
  ],
  func: async (params) => {
    const response = await unallocatedUserList(params);
    return { data: { rows: response.rows, total: response.total } };
  },
  params: queryParams,
  events: {
    formatParams(params) {
      pageParams.pageNum = params.pageNum;
      pageParams.pageSize = params.pageSize;
      return params;
    }
  }
});

const searchStore = reactive({
  visibleCount: 2,
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.roleAuth.userName'),
      prop: 'userName',
      component: { is: 'input', placeholder: td('sys.system.roleAuth.userNamePlaceholder') }
    },
    {
      label: td('sys.system.roleAuth.phone'),
      prop: 'phonenumber',
      component: { is: 'input', placeholder: td('sys.system.roleAuth.phonePlaceholder') }
    }
  ])
});

/** 保留父页面通过 show 打开弹窗的调用方式。 */
function show() {
  queryParams.roleId = props.roleId;
  clearSelection();
  visible.value = true;
}

function clearSelection() {
  userIds.value = [];
  tableRef.value?.elTableRef?.clearSelection();
}

function clickRow(row) {
  tableRef.value?.elTableRef?.toggleRowSelection(row);
}

function handleSelectionChange(selection) {
  userIds.value = selection.map((item) => item.userId);
}

function getList() {
  tableRef.value?.getList();
}

function handleQuery() {
  pageParams.pageNum = 1;
  tableRef.value?.resetQuery();
}

function resetQuery() {
  queryParams.userName = undefined;
  queryParams.phonenumber = undefined;
  handleQuery();
}

async function handleSelectUser() {
  if (submitting.value) return;
  const uIds = userIds.value.join(',');
  if (!uIds) {
    proxy.$modal.msgError(td('sys.system.roleAuth.selectAssignUser'));
    return;
  }
  submitting.value = true;
  try {
    const response = await authUserSelectAll({ roleId: queryParams.roleId, userIds: uIds });
    proxy.$modal.msgSuccess(response.msg);
    visible.value = false;
    emit('ok');
  } finally {
    submitting.value = false;
  }
}

defineExpose({ show });
</script>

<style scoped lang="scss">
.select-user-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}

:deep(.qt-wrap--search) {
  margin-bottom: 15px;
  padding: 0;
  box-shadow: none;
}
</style>
