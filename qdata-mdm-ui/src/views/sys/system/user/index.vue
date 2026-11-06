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
  <div class="app-container user-management-page" ref="app-container">
    <el-container class="user-page-layout">
      <qt-tree
        ref="deptTreeRef"
        v-model:dept-name="deptName"
        :data="deptOptions"
        :tree-props="{ label: 'label', children: 'children' }"
        node-key="id"
        :use-builtin-cat-edit-dialog="false"
        :title="td('sys.system.user.dept')"
        :placeholder="td('sys.system.user.deptNamePlaceholder')"
        wrap-height-class=".user-management-page .qt-wrap"
        @node-click="handleNodeClick"
      >
      </qt-tree>
      <!-- right part -->
      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="tableStore.params"
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
              v-hasPermi="['system:user:add']"
            >
              {{ td('common.button.add') }}
            </el-button>
            <el-button
              type="primary"
              plain
              icon="Edit"
              :disabled="single"
              @click="handleUpdate"
              v-hasPermi="['system:user:edit']"
              >{{ td('common.button.update') }}
            </el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple || batchDeleteDisabled"
              @click="handleDelete"
              v-hasPermi="['system:user:remove']"
              >{{ td('common.button.delete') }}
            </el-button>
            <el-button
              type="info"
              plain
              icon="Upload"
              @click="handleImport"
              v-hasPermi="['system:user:import']"
            >
              {{ td('common.button.import') }}
            </el-button>
            <el-button
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
              v-hasPermi="['system:user:export']"
              >{{ td('common.button.export') }}
            </el-button>
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #department="{ row }">
              {{ row.dept?.deptName }}
            </template>
            <template #status="{ row }">
              <el-switch
                v-model="row.status"
                active-value="0"
                inactive-value="1"
                @change="handleStatusChange(row)"
              />
            </template>
            <template #createTime="{ row }">
              <span>{{ parseTime(row.createTime) }}</span>
            </template>
            <template #action="{ row }">
              <el-button
                link
                type="primary"
                icon="View"
                :disabled="!checkPermi(['system:user:query'])"
                @click="$refs.detailDialogRef.openDialog(row)"
                >{{ td('common.button.details') }}</el-button
              >
              <el-button
                link
                type="primary"
                icon="Edit"
                @click="handleUpdate(row)"
                v-hasPermi="['system:user:edit']"
                :disabled="!(row.userId !== 1)"
                >{{ td('common.button.update') }}
              </el-button>
              <el-popover
                placement="bottom"
                :width="110"
                trigger="click"
                popper-class="table-action-popover handle-popover"
                :disabled="
                  (!(row.userId !== 1) || !checkPermi(['system:user:resetPwd'])) &&
                  (!(row.userId !== 1) || !checkPermi(['system:user:edit'])) &&
                  (isDeleteDisabled(row) ||
                    !(row.userId !== 1) ||
                    !checkPermi(['system:user:remove']))
                "
              >
                <template #reference>
                  <el-button
                    link
                    type="primary"
                    icon="ArrowDown"
                    :disabled="
                      (!(row.userId !== 1) || !checkPermi(['system:user:resetPwd'])) &&
                      (!(row.userId !== 1) || !checkPermi(['system:user:edit'])) &&
                      (isDeleteDisabled(row) ||
                        !(row.userId !== 1) ||
                        !checkPermi(['system:user:remove']))
                    "
                    >{{ td('common.button.more') }}</el-button
                  >
                </template>
                <div class="butgdlist task-action-list">
                  <el-button
                    style="padding-left: 14px"
                    link
                    type="primary"
                    icon="Key"
                    @click="handleResetPwd(row)"
                    v-hasPermi="['system:user:resetPwd']"
                    :disabled="!(row.userId !== 1)"
                    >{{ td('sys.system.user.resetPassword') }}
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    icon="CircleCheck"
                    @click="handleAuthRole(row)"
                    v-hasPermi="['system:user:edit']"
                    :disabled="!(row.userId !== 1)"
                  >
                    {{ td('sys.system.user.assignRole') }}
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    icon="Delete"
                    @click="handleDelete(row)"
                    v-hasPermi="['system:user:remove']"
                    :disabled="isDeleteDisabled(row) || !(row.userId !== 1)"
                    >{{ td('common.button.delete') }}
                  </el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
    <user-form-dialog
      ref="userFormDialogRef"
      :dept-options="deptOptions || []"
      @success="getList"
    />
    <user-import-dialog ref="userImportDialogRef" @success="getList" />
    <user-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script setup name="User">
import UserDetailDialog from './components/userDetailDialog.vue';

import { checkPermi } from '@/utils/permission';

import {
  changeUserStatus,
  listUser,
  resetUserPwd,
  delUser,
  deptTreeSelect
} from '@/api/system/system/user.js';
import { computed } from 'vue';
import QtTree from '@/components/QtTree/index.vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import UserFormDialog from './components/userFormDialog.vue';
import UserImportDialog from './components/userImportDialog.vue';
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const router = useRouter();
const { proxy } = getCurrentInstance();
const userFormDialogRef = ref();
const userImportDialogRef = ref();
const { sys_normal_disable } = proxy.useDict('sys_normal_disable');
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const deptName = ref('');
const deptOptions = ref(undefined);

const tableRef = ref(null);
// 分页交给 QtTable 管理；保存当前分页供导出和查询时保留每页条数。
const pageParams = reactive({ pageNum: 1, pageSize: 10 });

const data = reactive({
  queryParams: {
    userName: undefined,
    phonenumber: undefined,
    status: undefined,
    deptId: undefined,
    dateRange: []
  }
});

const { queryParams } = toRefs(data);

const tableStore = reactive({
  config: {
    table: {
      defaultSort: { prop: 'createTime', order: 'descending' },
      stripe: true,
      height: '58vh',
      rowKey: 'userId',
      onSelectionChange: handleSelectionChange
    },
    pagination: {
      pageSizes: [10, 20, 30, 50],
      pagerCount: document.body.clientWidth < 992 ? 5 : 7,
      params: computed(() => ({ pageNum: 1, pageSize: pageParams.pageSize }))
    }
  },
  columns: [
    { type: 'selection', width: 50 },
    {
      prop: 'userId',
      width: 60,
      sortable: true,
      get label() {
        return td('sys.system.user.userNo');
      }
    },
    {
      prop: 'userName',
      get label() {
        return td('sys.system.user.userName');
      },
      showOverflowTooltip: true
    },
    {
      prop: 'nickName',
      get label() {
        return td('sys.system.user.userNickName');
      },
      showOverflowTooltip: true
    },
    {
      slot: 'department',
      get label() {
        return td('sys.system.user.dept');
      },
      showOverflowTooltip: true
    },
    {
      prop: 'phonenumber',
      get label() {
        return td('sys.system.user.phone');
      },
      width: 120
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
      width: 160
    },
    {
      slot: 'action',
      get label() {
        return td('common.texts.operation');
      },
      fixed: 'right',
      width: 240,
      className: 'small-padding fixed-width'
    }
  ],
  func: async (params) => {
    const response = await listUser(params);
    return { data: { rows: response.rows, total: response.total } };
  },
  params: queryParams,
  events: {
    formatParams(params) {
      pageParams.pageNum = params.pageNum;
      pageParams.pageSize = params.pageSize;
      return buildQueryParams(params);
    }
  }
});

const searchStore = reactive({
  visibleCount: 4,
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.system.user.userName'),
      prop: 'userName',
      component: { is: 'input', placeholder: td('sys.system.user.userNamePlaceholder') }
    },
    {
      label: td('sys.system.user.phone'),
      prop: 'phonenumber',
      component: { is: 'input', placeholder: td('sys.system.user.phonePlaceholder') }
    },
    {
      label: td('common.texts.status'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.system.user.userStatus'),
        options: sys_normal_disable.value
      }
    },
    {
      label: td('common.texts.createdTime'),
      prop: 'dateRange',
      component: {
        is: 'date-picker',
        type: 'daterange',
        valueFormat: 'YYYY-MM-DD',
        rangeSeparator: '-',
        startPlaceholder: td('common.form.startDatePlaceholder'),
        endPlaceholder: td('common.form.endDatePlaceholder')
      }
    }
  ])
});

/** 保持接口的原有日期参数结构，不发送搜索组件内部的 dateRange 字段。 */
function buildQueryParams(values = { ...queryParams.value, ...pageParams }) {
  const { dateRange, ...params } = values;
  return proxy.addDateRange(params, dateRange);
}

/** Query department drop-down tree structure */
function getDeptTree() {
  deptTreeSelect().then((response) => {
    deptOptions.value = response.data;
  });
}

/** Query user list */
function getList() {
  tableRef.value?.getList();
}

/** Node click event */
function handleNodeClick(data) {
  queryParams.value.deptId = data.id;
  handleQuery();
}

/** Search button action */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** reset button action */
function resetQuery() {
  queryParams.value.userName = undefined;
  queryParams.value.phonenumber = undefined;
  queryParams.value.status = undefined;
  queryParams.value.dateRange = [];
  queryParams.value.deptId = undefined;
  proxy.$refs.deptTreeRef.setCurrentKey(null);
  handleQuery();
}

/** Delete button action */
const deleteSelection = ref([]);
const batchDeleteDisabled = computed(
  () => !deleteSelection.value.length || deleteSelection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row?.status) === '0' || String(row?.userId) === '1';
}

function handleDelete(row) {
  if (row?.userId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const userIds = row.userId || ids.value;
  proxy.$modal
    .confirm(td('sys.system.user.confirmDelete', { id: userIds }))
    .then(function () {
      return delUser(userIds);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

/** Export button action */
function handleExport() {
  proxy.download('system/user/export', buildQueryParams(), `user_${new Date().getTime()}.xlsx`);
}

/** User status modification  */
function handleStatusChange(row) {
  let text = row.status === '0' ? td('sys.system.user.enable') : td('sys.system.user.disable');
  proxy.$modal
    .confirm(td('sys.system.user.confirmStatusChange', { text, name: row.userName }))
    .then(function () {
      return changeUserStatus(row.userId, row.status);
    })
    .then(() => {
      proxy.$modal.msgSuccess(td('common.message.updateSuccess'));
    })
    .catch(function () {
      row.status = row.status === '0' ? '1' : '0';
    });
}

/** More actions */
function handleCommand(command, row) {
  switch (command) {
    case 'handleResetPwd':
      handleResetPwd(row);
      break;
    case 'handleAuthRole':
      handleAuthRole(row);
      break;
    default:
      break;
  }
}

/** Jump to role assignment */
function handleAuthRole(row) {
  const userId = row.userId;
  router.push('/system/user-auth/role/' + userId);
}

/** Reset password button action */
function handleResetPwd(row) {
  proxy
    .$prompt(
      td('sys.system.user.resetPasswordPrompt', { name: row.userName }),
      td('common.message.prompt'),
      {
        confirmButtonText: td('common.button.confirm'),
        cancelButtonText: td('common.button.cancel'),
        closeOnClickModal: false,
        inputType: 'password',
        inputPattern: /^.{8,20}$/, // Password length requirement is between 8 and 20
        inputErrorMessage: td('sys.system.user.resetPasswordInputError'),
        inputValidator: (value) => {
          // Verify that the password contains illegal characters
          if (/<|>|"|'|\||\\/.test(value)) {
            return td('sys.system.user.invalidChars');
          }
          // Check password strength
          const strengthRegex = {
            upperCase: /[A-Z]/, // at least one capital letter
            lowerCase: /[a-z]/, // at least one lowercase letter
            number: /\d/, // at least one number
            specialChar: /[!@#$%^&*(),.?":{}|<>]/ // at least one special character
          };

          if (!strengthRegex.upperCase.test(value)) {
            return td('sys.system.user.passwordUppercase');
          }
          if (!strengthRegex.lowerCase.test(value)) {
            return td('sys.system.user.passwordLowercase');
          }
          if (!strengthRegex.number.test(value)) {
            return td('sys.system.user.passwordDigit');
          }
          if (!strengthRegex.specialChar.test(value)) {
            return td('sys.system.user.passwordSpecial');
          }
        }
      }
    )
    .then(({ value }) => {
      resetUserPwd(row.userId, value).then((response) => {
        proxy.$modal.msgSuccess(td('sys.system.user.resetPasswordSuccess', { password: value }));
      });
    })
    .catch(() => {
      // Handling cancellation operations
    });
}

/** Select the number of items  */
function handleSelectionChange(selection) {
  deleteSelection.value = selection;
  ids.value = selection.map((item) => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 打开独立的用户导入弹窗。 */
function handleImport() {
  userImportDialogRef.value.openDialog();
}

/** 打开新增用户弹窗。 */
function handleAdd() {
  userFormDialogRef.value.openDialog();
}

/** 按当前行或选中用户打开编辑弹窗。 */
function handleUpdate(row) {
  userFormDialogRef.value.openDialog(row.userId || ids.value[0]);
}

getDeptTree();
</script>
