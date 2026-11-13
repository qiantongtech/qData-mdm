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
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['monitor:job:remove']"
        >
          {{ td('common.button.delete') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          @click="handleClean"
          v-hasPermi="['monitor:job:remove']"
        >
          {{ td('sys.monitor.jobLog.clearAll') }}
        </el-button>
        <el-button
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          v-hasPermi="['monitor:job:export']"
        >
          {{ td('common.button.export') }}
        </el-button>
        <el-button type="warning" plain icon="Close" @click="handleClose">
          {{ td('common.button.close') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #jobLogId="{ row }">{{ row.jobLogId }}</template>
          <template #jobGroup="scope">
            <dict-tag :options="sys_job_group" :value="scope.row.jobGroup" />
          </template>
          <template #invokeTarget="{ row }">{{ row.invokeTarget }}</template>
          <template #jobMessage="{ row }">{{ row.jobMessage }}</template>
          <template #status="scope">
            <dict-tag :options="sys_common_status" :value="scope.row.status" />
          </template>
          <template #createTime="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleView(scope.row)"
              v-hasPermi="['monitor:job:query']"
            >
              {{ td('sys.monitor.jobLog.detail') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <job-log-detail-dialog ref="jobLogDetailDialogRef" :state="jobLogDetailDialogState" />
  </div>
</template>

<script setup name="JobLog">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import JobLogDetailDialog from './components/jobLogDetailDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import { getJob } from '@/api/system/monitor/job.js';
import { listJobLog, delJobLog, cleanJobLog } from '@/api/system/monitor/jobLog.js';
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();
const { sys_common_status, sys_job_group } = proxy.useDict('sys_common_status', 'sys_job_group');

const jobLogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);
const route = useRoute();

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    dictName: undefined,
    dictType: undefined,
    status: undefined
  }
});

const { queryParams, form, rules } = toRefs(data);

/** Query scheduling log list */
function getList() {
  loading.value = true;
  listJobLog(proxy.addDateRange(queryParams.value, dateRange.value)).then((response) => {
    jobLogList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// back button
function handleClose() {
  const obj = { path: '/monitor/job' };
  proxy.$tab.closeOpenPage(obj);
}

/** Search button action */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** reset button action */
function resetQuery() {
  dateRange.value = [];
  proxy.$refs.queryRef?.$refs.formRef?.resetFields();
  handleQuery();
}

// Multiple selection box selected data
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.jobLogId);
  multiple.value = !selection.length;
}

/** Detailed button operations */
function handleView(row) {
  open.value = true;
  form.value = row;
}

/** Delete button action */
function handleDelete(row) {
  proxy.$modal
    .confirm(td('sys.monitor.jobLog.confirmDelete', { ids: ids.value }))
    .then(function () {
      return delJobLog(ids.value);
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('common.message.deleteSuccess'));
    })
    .catch(() => {});
}

/** Clear button action */
function handleClean() {
  proxy.$modal
    .confirm(td('sys.monitor.jobLog.confirmClearAll'))
    .then(function () {
      return cleanJobLog();
    })
    .then(() => {
      getList();
      proxy.$modal.msgSuccess(td('sys.monitor.jobLog.clearSuccess'));
    })
    .catch(() => {});
}

/** Export button action */
function handleExport() {
  proxy.download(
    'monitor/jobLog/export',
    {
      ...queryParams.value
    },
    `job_log_${new Date().getTime()}.xlsx`
  );
}

(() => {
  const jobId = route.params && route.params.jobId;
  if (jobId !== undefined && jobId != 0) {
    getJob(jobId).then((response) => {
      queryParams.value.jobName = response.data.jobName;
      queryParams.value.jobGroup = response.data.jobGroup;
      getList();
    });
  } else {
    getList();
  }
})();

const jobLogDetailDialogState = reactive({
  get open() {
    return unref(open);
  },
  set open(value) {
    if (isRef(open)) open.value = value;
    else Object.assign(open, value);
  },
  get form() {
    return unref(form);
  },
  set form(value) {
    if (isRef(form)) form.value = value;
    else Object.assign(form, value);
  }
});

const searchParams = reactive({
  get jobName() {
    return unref(queryParams).jobName;
  },
  set jobName(value) {
    unref(queryParams).jobName = value;
  },
  get jobGroup() {
    return unref(queryParams).jobGroup;
  },
  set jobGroup(value) {
    unref(queryParams).jobGroup = value;
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
      label: td('sys.monitor.jobLog.taskName'),
      prop: 'jobName',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.jobLog.taskNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.jobLog.taskGroup'),
      prop: 'jobGroup',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.jobLog.taskGroupPlaceholder'),
        clearable: true,
        options: (unref(sys_job_group) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('sys.monitor.jobLog.execStatus'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.jobLog.execStatusPlaceholder'),
        clearable: true,
        options: (unref(sys_common_status) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('sys.monitor.jobLog.execTime'),
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
    { type: 'selection', width: '55', align: 'center' },
    {
      prop: 'jobLogId',
      sortable: true,
      get label() {
        return td('sys.monitor.jobLog.logNo');
      },
      width: 60,
      align: 'center',
      slot: 'jobLogId'
    },
    {
      get label() {
        return td('sys.monitor.jobLog.taskName');
      },
      align: 'center',
      showOverflowTooltip: true,
      prop: 'jobName',
      className: 'title-column',
      get link() { return tableTitleLink(handleView, 'monitor:job:query'); }
    },
    {
      get label() {
        return td('sys.monitor.jobLog.taskGroup');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'jobGroup'
    },
    {
      get label() {
        return td('sys.monitor.jobLog.invokeTarget');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'invokeTarget'
    },
    {
      get label() {
        return td('sys.monitor.jobLog.logInfo');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'jobMessage'
    },
    {
      get label() {
        return td('sys.monitor.jobLog.execStatus');
      },
      align: 'center',
      slot: 'status'
    },
    {
      prop: 'createTime',
      sortableKey: 'create_time',
      sortable: true,
      get label() {
        return td('sys.monitor.jobLog.execTime');
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
      slot: 'action'
    }
  ],
  func: async () => ({ data: { rows: unref(jobLogList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(jobLogList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>
<style scoped lang="scss">
.form-value-ifon {
  width: 100%;
  border: 1px solid #c0c4cc;
  padding: 0px 10px;
  min-height: 34px;
}
</style>
