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
  <div class="app-container">
    <qt-wrap :columns="columns" :config="config" :table-ref="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="searchParams"
          @query="tableRef.resetQuery()"
          @reset="tableRef.resetQuery()"
        />
      </template>
      <template #actions-data>
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="formDialogRef.openDialog()"
          v-hasPermi="['system:notice:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="batchDeleteDisabled"
          @click="handleDelete()"
          v-hasPermi="['system:notice:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
      </template>
      <qt-table ref="tableRef" :columns="columns" :config="config" :params="searchParams" :func="fetchNotices">
        <template #noticeId="{ row }">{{ row.noticeId }}</template>
        <template #noticeTitle="{ row }">
          <div class="notice-title-cell">
            <router-link
              v-if="getDisplayTitle(row.noticeTitle) && $auth.hasPermi('system:notice:query')"
              class="el-link el-link--primary qt-detail-link text-ellipsis"
              :title="String(row.noticeTitle)"
              :to="{ name: 'noticeDetail', query: { id: row.noticeId } }"
              @click.stop
            >{{ row.noticeTitle }}</router-link>
            <span v-else class="qt-detail-link text-ellipsis" :title="getDisplayTitle(row.noticeTitle)">{{ getDisplayTitle(row.noticeTitle) ?? '-' }}</span>
            <el-tag v-if="Number(row.topFlag) === 1" size="small" effect="plain">
              {{ td('sys.system.notice.isTop') }}
            </el-tag>
            <el-tag v-if="Number(row.alertFlag) === 1" size="small" type="info" effect="plain">
              {{ td('sys.system.notice.popup') }}
            </el-tag>
          </div>
        </template>
        <template #effectivePeriod="{ row }">
          <span :class="{ 'empty-period': !row.alertStartTime && !row.alertEndTime }">
            {{ formatPeriod(row) }}
          </span>
        </template>
        <template #createBy="{ row }">{{ row.createBy || '—' }}</template>
        <template #action="{ row }">
          <el-button
            v-if="canEdit"
            link
            type="primary"
            icon="Edit"
            @click="formDialogRef.openDialog(row, true)"
          >{{ td('common.button.update') }}</el-button>
          <el-button
            v-if="canRemove"
            link
            type="danger"
            icon="Delete"
            :disabled="isDeleteDisabled(row)"
            @click="handleDelete(row)"
          >{{ td('common.button.delete') }}</el-button>
        </template>
      </qt-table>
    </qt-wrap>
    <notice-form-dialog ref="formDialogRef" :ids="ids" @success="tableRef.getList()" />
  </div>
</template>

<script setup name="Notice">
import { computed, reactive, ref, unref } from 'vue';
import { delNotice, listNotice } from '@/api/system/system/notice.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { getDisplayTitle, parseTime } from '@/utils/anivia';
import { checkPermi } from '@/utils/permission';
import QtSearchBar from '@/components/QtSearchBar/index.vue';
import NoticeFormDialog from './components/noticeFormDialog.vue';

const { td, t } = useDefaultLang();
const { proxy } = getCurrentInstance();
const { sys_notice_type } = proxy.useDict('sys_notice_type');
const canEdit = computed(() => checkPermi(['system:notice:edit']));
const canRemove = computed(() => checkPermi(['system:notice:remove']));
const tableRef = ref(null);
const formDialogRef = ref(null);
const selection = ref([]);
const ids = computed(() => selection.value.map((row) => row.noticeId));
const batchDeleteDisabled = computed(
  () => !selection.value.length || selection.value.some(isDeleteDisabled)
);

function isDeleteDisabled(row) {
  return String(row.status) === '0';
}

function handleDelete(row) {
  if (row ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const noticeIds = row?.noticeId ?? ids.value;
  proxy.$modal
    .confirm(t('sys.system.notice.confirmDelete', { id: String(noticeIds) }))
    .then(() => delNotice(noticeIds))
    .then(() => {
      tableRef.value.getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

function formatPeriod(row) {
  const start = parseTime(row.alertStartTime, '{y}-{m}-{d}');
  const end = parseTime(row.alertEndTime, '{y}-{m}-{d}');
  if (start && end) return start === end ? start : `${start} – ${end}`;
  if (start) return `${td('common.date.startDate')} ${start}`;
  if (end) return `${td('common.date.endDate')} ${end}`;
  return '—';
}

async function fetchNotices(params) {
  defaultPage.pageSize = params.pageSize;
  const { rows, total } = await listNotice(params);
  return { data: { rows: rows || [], total } };
}

const searchParams = reactive({ noticeTitle: undefined, createBy: undefined, noticeType: undefined });
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
      label: td('sys.system.notice.creator'),
      prop: 'createBy',
      component: {
        is: 'input',
        placeholder: td('sys.system.notice.creatorPlaceholder'),
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

const defaultPage = reactive({ pageNum: 1, pageSize: 10 });
const config = {
  sort: true,
  table: {
    defaultSort: { prop: 'createTime', order: 'descending' },
    stripe: true,
    onSelectionChange: (rows) => { selection.value = rows; }
  },
  pagination: {
    pageSizes: [10, 20, 30, 50],
    pagerCount: document.body.clientWidth < 992 ? 5 : 7,
    params: defaultPage
  }
};

const tableColumns = reactive([
  { type: 'selection', width: 44 },
  {
    prop: 'noticeId',
    get label() { return td('common.display.index'); },
    sortable: 'custom',
    width: 80,
    slot: 'noticeId'
  },
  {
    prop: 'noticeTitle',
    get label() { return td('sys.system.notice.noticeTitle'); },
    align: 'left',
    headerAlign: 'left',
    className: 'notice-title-column',
    minWidth: 280,
    slot: 'noticeTitle'
  },
  {
    prop: 'noticeType',
    get label() { return td('sys.system.notice.noticeType'); },
    width: 110,
    dict: 'sys_notice_type'
  },
  {
    prop: 'status',
    get label() { return td('common.texts.status'); },
    width: 90,
    dict: 'sys_notice_status'
  },
  {
    prop: 'alertStartTime',
    get label() { return td('sys.system.notice.effectTime'); },
    width: 208,
    showOverflowTooltip: true,
    slot: 'effectivePeriod'
  },
  {
    prop: 'createBy',
    get label() { return td('sys.system.notice.creator'); },
    width: 138,
    showOverflowTooltip: true,
    slot: 'createBy'
  },
  {
    prop: 'createTime',
    get label() { return td('common.texts.createdTime'); },
    sortable: 'custom',
    width: 136,
    date: '{y}-{m}-{d}'
  },
  {
    prop: 'action',
    get label() { return td('common.texts.operation'); },
    fixed: 'right',
    get width() { return canEdit.value && canRemove.value ? 168 : 94; },
    slot: 'action'
  }
]);

const columns = computed(() => tableColumns.filter((column) => {
  if (column.type === 'selection') return canRemove.value;
  if (column.prop === 'action') return canEdit.value || canRemove.value;
  return true;
}));
</script>

<style scoped>
:deep(td.notice-title-column > .cell) {
  padding-left: 10px;
}

.notice-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice-title-cell .el-tag {
  flex: none;
}

.empty-period {
  color: var(--el-text-color-placeholder);
}
</style>
