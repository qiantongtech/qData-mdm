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
          v-hasPermi="['monitor:job:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
        <el-button
          type="primary"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['monitor:job:edit']"
        >
          {{ td('common.button.update') }}
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple || batchDeleteDisabled"
          @click="handleDelete"
          v-hasPermi="['monitor:job:remove']"
        >
          {{ td('common.button.delete') }}
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
        <el-button
          type="info"
          plain
          icon="Operation"
          @click="handleJobLog"
          v-hasPermi="['monitor:job:query']"
        >
          {{ td('sys.monitor.job.log') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #jobId="{ row }">{{ row.jobId }}</template>
          <template #jobGroup="scope">
            <dict-tag :options="sys_job_group" :value="scope.row.jobGroup" />
          </template>
          <template #invokeTarget="{ row }">{{ row.invokeTarget }}</template>
          <template #cronExpression="{ row }">{{ row.cronExpression }}</template>
          <template #cell6="scope">
            <el-switch
              v-model="scope.row.status"
              active-value="0"
              inactive-value="1"
              @change="handleStatusChange(scope.row)"
            />
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              @click="handleView(scope.row)"
              v-hasPermi="['monitor:job:query']"
            >
              {{ td('sys.monitor.job.taskDetail') }}
            </el-button>
            <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['monitor:job:edit']"
            >
              {{ td('common.button.update') }}
            </el-button>
            <el-popover
              placement="bottom"
              :width="110"
              trigger="click"
              popper-class="table-action-popover handle-popover"
              :disabled="
                !checkPermi(['monitor:job:changeStatus']) &&
                !checkPermi(['monitor:job:query']) &&
                (isDeleteDisabled(scope.row) || !checkPermi(['monitor:job:remove']))
              "
            >
              <template #reference>
                <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
                  :disabled="
                    !checkPermi(['monitor:job:changeStatus']) &&
                    !checkPermi(['monitor:job:query']) &&
                    (isDeleteDisabled(scope.row) || !checkPermi(['monitor:job:remove']))
                  "
                  >{{ td('common.button.more') }}</el-button
                >
              </template>
              <div class="butgdlist task-action-list">
                <el-button
                  link
                  type="primary"
                  icon="CaretRight"
                  @click="handleRun(scope.row)"
                  v-hasPermi="['monitor:job:changeStatus']"
                >
                  {{ td('sys.monitor.job.executeOnce') }}
                </el-button>
                <el-button
                  link
                  type="primary"
                  icon="Operation"
                  @click="handleJobLog(scope.row)"
                  v-hasPermi="['monitor:job:query']"
                >
                  {{ td('sys.monitor.job.scheduleLog') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['monitor:job:remove']"
                  :disabled="isDeleteDisabled(scope.row)"
                >
                  {{ td('common.button.delete') }}
                </el-button>
              </div>
            </el-popover>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <job-form-dialog ref="jobFormDialogRef" :state="jobFormDialogState" />
    <job-cron-dialog ref="jobCronDialogRef" :state="jobCronDialogState" />
    <job-detail-dialog ref="jobDetailDialogRef" :state="jobDetailDialogState" />
  </div>
</template>

<script setup name="Job">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import { checkPermi } from '@/utils/permission';
import JobFormDialog from './components/jobFormDialog.vue';
import JobCronDialog from './components/jobCronDialog.vue';
import JobDetailDialog from './components/jobDetailDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import useDefaultLang from '@/composables/useDefaultLang';
import {
  listJob,
  getJob,
  delJob,
  addJob,
  updateJob,
  runJob,
  changeJobStatus
} from '@/api/system/monitor/job.js';
import Crontab from '@/components/Crontab/index.vue';

const { td } = useDefaultLang();
const router = useRouter();
const { proxy } = getCurrentInstance();
const submitLoading = ref(false);
const { sys_job_group, sys_job_status } = proxy.useDict('sys_job_group', 'sys_job_status');

const jobList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref('');
const openView = ref(false);
const openCron = ref(false);
const expression = ref('');

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    jobName: undefined,
    jobGroup: undefined,
    status: undefined
  },
  rules: {
    jobName: [{ required: true, message: td('sys.monitor.job.taskNameRequired'), trigger: 'blur' }],
    invokeTarget: [
      { required: true, message: td('sys.monitor.job.invokeTargetRequired'), trigger: 'blur' }
    ],
    cronExpression: [
      { required: true, message: td('sys.monitor.job.cronExprRequired'), trigger: 'change' }
    ]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** Query scheduled task list */
function getList() {
  loading.value = true;
  listJob(queryParams.value).then((response) => {
    jobList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

/** Task team name dictionary translation */
function jobGroupFormat(row, column) {
  return proxy.selectDictLabel(sys_job_group.value, row.jobGroup);
}

/** Cancel button */
function cancel() {
  open.value = false;
  reset();
}

/** form reset */
function reset() {
  form.value = {
    jobId: undefined,
    jobName: undefined,
    jobGroup: undefined,
    invokeTarget: undefined,
    cronExpression: undefined,
    misfirePolicy: 1,
    concurrent: 1,
    status: '0'
  };
  proxy.$refs.jobFormDialogRef.dialogElement0?.resetFields();
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
  ids.value = selection.map((item) => item.jobId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// More action triggers
function handleCommand(command, row) {
  switch (command) {
    case 'handleRun':
      handleRun(row);
      break;
    case 'handleView':
      handleView(row);
      break;
    case 'handleJobLog':
      handleJobLog(row);
      break;
    default:
      break;
  }
}

// Task status modification
function handleStatusChange(row) {
  let text = row.status === '0' ? td('sys.monitor.job.enable') : td('sys.monitor.job.disable');
  proxy.$modal
    .confirm(td('sys.monitor.job.confirmStatusChange', { text: text, name: row.jobName }))
    .then(function () {
      return changeJobStatus(row.jobId, row.status);
    })
    .then(() => {
      proxy.$modal.msgSuccess(text + td('common.message.success'));
    })
    .catch(function () {
      row.status = row.status === '0' ? '1' : '0';
    });
}

/* Execute once immediately */
function handleRun(row) {
  proxy.$modal
    .confirm(td('sys.monitor.job.confirmImmediateExec', { name: row.jobName }))
    .then(function () {
      return runJob(row.jobId, row.jobGroup);
    })
    .then(() => {
      proxy.$modal.msgSuccess(td('sys.monitor.job.execSuccess'));
    })
    .catch(() => {});
}

/** Task details */
function handleView(row) {
  getJob(row.jobId).then((response) => {
    form.value = response.data;
    openView.value = true;
  });
}

/** cron expression button action */
function handleShowCron() {
  expression.value = form.value.cronExpression;
  openCron.value = true;
}

/** Return value after confirmation */
function crontabFill(value) {
  form.value.cronExpression = value;
}

/** Task log list query */
function handleJobLog(row) {
  const jobId = row.jobId || 0;
  router.push('/monitor/job-log/index/' + jobId);
}

/** Add button operation */
function handleAdd() {
  reset();
  open.value = true;
  title.value = td('sys.monitor.job.addTitle');
}

/** Modify button actions */
function handleUpdate(row) {
  reset();
  const jobId = row.jobId || ids.value;
  getJob(jobId).then((response) => {
    form.value = response.data;
    open.value = true;
    title.value = td('sys.monitor.job.editTitle');
  });
}

/** submit button */
function submitForm() {
  if (submitLoading.value) return;
  submitLoading.value = true;
  proxy.$refs.jobFormDialogRef.dialogElement0.validate((valid) => {
    if (valid) {
      if (form.value.jobId != undefined) {
        updateJob(form.value)
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
        addJob(form.value)
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
  return String(row?.status) === '0';
}

function handleDelete(row) {
  if (row?.jobId != null ? isDeleteDisabled(row) : batchDeleteDisabled.value) return;
  const jobIds = row.jobId || ids.value;
  proxy.$modal
    .confirm(td('sys.monitor.job.confirmDelete', { ids: jobIds }))
    .then(function () {
      return delJob(jobIds);
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
    'monitor/job/export',
    {
      ...queryParams.value
    },
    `job_${new Date().getTime()}.xlsx`
  );
}

getList();

const jobFormDialogState = reactive({
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
  get sys_job_group() {
    return unref(sys_job_group);
  },
  set sys_job_group(value) {
    if (isRef(sys_job_group)) sys_job_group.value = value;
    else Object.assign(sys_job_group, value);
  },
  get handleShowCron() {
    return unref(handleShowCron);
  },
  set handleShowCron(value) {
    if (isRef(handleShowCron)) handleShowCron.value = value;
    else Object.assign(handleShowCron, value);
  },
  get sys_job_status() {
    return unref(sys_job_status);
  },
  set sys_job_status(value) {
    if (isRef(sys_job_status)) sys_job_status.value = value;
    else Object.assign(sys_job_status, value);
  }
});

const jobCronDialogState = reactive({
  get openCron() {
    return unref(openCron);
  },
  set openCron(value) {
    if (isRef(openCron)) openCron.value = value;
    else Object.assign(openCron, value);
  },
  get crontabFill() {
    return unref(crontabFill);
  },
  set crontabFill(value) {
    if (isRef(crontabFill)) crontabFill.value = value;
    else Object.assign(crontabFill, value);
  },
  get expression() {
    return unref(expression);
  },
  set expression(value) {
    if (isRef(expression)) expression.value = value;
    else Object.assign(expression, value);
  }
});

const jobDetailDialogState = reactive({
  get openView() {
    return unref(openView);
  },
  set openView(value) {
    if (isRef(openView)) openView.value = value;
    else Object.assign(openView, value);
  },
  get form() {
    return unref(form);
  },
  set form(value) {
    if (isRef(form)) form.value = value;
    else Object.assign(form, value);
  },
  get jobGroupFormat() {
    return unref(jobGroupFormat);
  },
  set jobGroupFormat(value) {
    if (isRef(jobGroupFormat)) jobGroupFormat.value = value;
    else Object.assign(jobGroupFormat, value);
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
  }
});

const searchStore = reactive({
  config: { form: { labelPosition: proxy.labelPosition } },
  items: computed(() => [
    {
      label: td('sys.monitor.job.taskName'),
      prop: 'jobName',
      component: {
        is: 'input',
        placeholder: td('sys.monitor.job.taskNamePlaceholder'),
        clearable: true
      }
    },
    {
      label: td('sys.monitor.job.taskGroup'),
      prop: 'jobGroup',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.job.taskGroupPlaceholder'),
        clearable: true,
        options: (unref(sys_job_group) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
      }
    },
    {
      label: td('sys.monitor.job.taskStatus'),
      prop: 'status',
      component: {
        is: 'select',
        placeholder: td('sys.monitor.job.taskStatusPlaceholder'),
        clearable: true,
        options: (unref(sys_job_status) || []).map((dict) => ({
          label: dict.label,
          value: dict.value
        }))
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
      defaultSort: { prop: 'jobId', order: 'descending' },
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
      sortableKey: 'job_id',
      prop: 'jobId',
      sortable: true,
      get label() {
        return td('sys.monitor.job.taskNo');
      },
      width: 60,
      align: 'center',
      slot: 'jobId'
    },
    {
      get label() {
        return td('sys.monitor.job.taskName');
      },
      align: 'center',
      showOverflowTooltip: true,
      prop: 'jobName',
      className: 'title-column',
      get link() { return tableTitleLink(handleView, 'monitor:job:query'); }
    },
    {
      get label() {
        return td('sys.monitor.job.taskGroup');
      },
      align: 'center',
      slot: 'jobGroup'
    },
    {
      get label() {
        return td('sys.monitor.job.invokeTarget');
      },
      align: 'center',
      showOverflowTooltip: true,
      width: '300px',
      slot: 'invokeTarget'
    },
    {
      get label() {
        return td('sys.monitor.job.cronExpression');
      },
      align: 'center',
      showOverflowTooltip: true,
      width: '300px',
      slot: 'cronExpression'
    },
    {
      get label() {
        return td('common.display.status');
      },
      align: 'center',
      slot: 'cell6'
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
  func: async () => ({ data: { rows: unref(jobList), total: unref(total) } }),
  params: queryParams,
  events: { onPageCurrentChange: handleTablePagination0, onPageSizeChange: handleTablePagination0 }
});

watch(
  [
    () => unref(jobList),
    () => unref(total),
    () => unref(queryParams).pageNum,
    () => unref(queryParams).pageSize
  ],
  () => qtTableRef0.value?.resetQuery(),
  { flush: 'post' }
);
</script>

<style lang="scss">
.table-action-popover.handle-popover.el-popover.el-popper {
  padding: 12px 0;
  min-width: 120px !important;

  .task-action-list {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    box-sizing: border-box;

    .el-button {
      width: 100%;
      box-sizing: border-box;
      justify-content: center;
      margin-left: 0;
      padding: 8px 12px;
      text-align: center;
      border: none;
      border-radius: 0;

      &:hover {
        background-color: var(--el-fill-color-light);
      }
    }
  }
}
</style>
