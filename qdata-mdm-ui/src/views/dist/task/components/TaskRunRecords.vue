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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div>
    <div class="basicInfo mb15">
      <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
        <template #actions-data>
          <div class="h2-titles">执行概况</div>
        </template>

        <DescriptionsInfo :data="{}" :items="summaryItems" :column="2" :border="true" />
      </qt-wrap>
    </div>

    <!-- 执行记录沿用已有的运行实例接口，按原型展示列字段。 -->
    <div class="basicInfo mb15">
      <qt-wrap :columns="recordTable.columns" :table-ref="tableRef"
               :config="{ fullContent: false, actions: { table: { show: false } } }"
  >
        <template #actions-data>
          <div class="h2-titles">执行记录列表</div>
        </template>
        <qt-table ref="tableRef" v-bind="recordTable">
          <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
          <template #startTime="{ row }">{{ formatTime(row.startTime) }}</template>
          <template #triggerType="{ row }">{{ triggerName(row.triggerType) }}</template>
          <template #runStatus="{ row }">
            <RunStatusTag :status="row.runStatus" :success-count="Number(row.successCount || 0)"
                          :failure-count="Number(row.failureCount || 0)" />
          </template>
          <template #totalCount="{ row }">{{ row.totalCount ?? 0 }}</template>
          <template #successCount="{ row }">{{ row.successCount ?? 0 }}</template>
          <template #failureCount="{ row }">{{ row.failureCount ?? 0 }}</template>
          <template #duration="{ row }">{{ formatDuration(row.startTime, row.endTime) }}</template>
          <template #action="{ row }">
            <el-button link type="primary" icon="View" @click="showResult(row)">查看日志</el-button>
          </template>
        </qt-table>
      </qt-wrap>
    </div>

    <RunLogDialog :visible="resultVisible" :run="selectedRun || {}" :task-name="taskName"
                  :append-to="appendTo"
                  title="查看日志" @update:visible="resultVisible = $event"
/>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref } from 'vue';
import { listINSTANCE } from '@/api/dist/task/INSTANCE';
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue';
import RunLogDialog from './RunLogDialog.vue';
import RunStatusTag from './RunStatusTag.vue';

const props = defineProps({
  taskId: { type: [String, Number], required: true },
  taskName: { type: String, default: '' },
  lastRunTime: { type: [String, Number, Date], default: null },
  lastRunStatus: { type: String, default: '' },
  appendTo: { type: [Object, String], default: 'body' }
});
const { proxy } = getCurrentInstance();
const tableRef = ref(null);
const selectedRun = ref(null);
const resultVisible = ref(false);

/** 汇总接口尚未提供；下面四项仅用于当前页面展示，接入汇总接口时替换。 */
const sampleSummary = { total: 12, failed: 4, success: 8, partial: 0 };
const summaryItems = computed(() => [
  { label: '近7天执行次数', value: sampleSummary.total },
  { label: '失败次数', value: sampleSummary.failed },
  { label: '成功次数', value: sampleSummary.success },
  { label: '最近执行时间', value: formatTime(props.lastRunTime) },
  { label: '部分成功', value: sampleSummary.partial },
  { label: '最近执行结果', value: ({ SUCCESS: '成功', FAILED: '失败', FAILURE: '失败', RUNNING: '执行中' })[props.lastRunStatus] || '-' }
]);

const recordTable = reactive({
  func: params => listINSTANCE({
    ...params, taskId: props.taskId
  }),
  params: {},
  events: {
    // 未点击排序时不传排序字段；编号和执行时间均由服务端排序，保证跨页顺序一致。
    formatParams: params => {
      const { orderByColumn, isAsc, ...query } = params;
      if (['id', 'startTime'].includes(orderByColumn) && isAsc) {
        query.orderByColumn = orderByColumn;
        query.isAsc = isAsc === 'ascending' ? 'asc' : 'desc';
      }
      return query;
    },
    formatData: (rows, params) => rows.map((row, index) => ({
      ...row, rowNumber: (Number(params.pageNum || 1) - 1) * Number(params.pageSize || 10) + index + 1
    }))
  },
  config: {
    table: { rowKey: 'id',
      stripe: true ,
      defaultSort: { prop: 'startTime', order: 'descending' }
    },
    pagination: { pageSizes: [10, 20, 50, 100], notAutoScroll: true }
  },
  columns: [
    { label: '编号', prop: 'id', slot: 'rowNumber', sortable: 'custom', width: 88, align: 'left' },
    { label: '触发方式', prop: 'triggerType', slot: 'triggerType', minWidth: 110, align: 'left' },
    { label: '推送总数', prop: 'totalCount', slot: 'totalCount', minWidth: 110, align: 'left' },
    { label: '成功数', prop: 'successCount', slot: 'successCount', minWidth: 100, align: 'left' },
    { label: '失败数', prop: 'failureCount', slot: 'failureCount', minWidth: 100, align: 'left' },
    { label: '执行耗时', prop: 'duration', slot: 'duration', minWidth: 110, align: 'left' },
    { label: '执行状态', prop: 'runStatus', slot: 'runStatus', minWidth: 110, align: 'left' },
    { label: '执行时间', prop: 'startTime', slot: 'startTime', sortable: 'custom', minWidth: 180, align: 'left' },
    { label: '操作', prop: 'action', slot: 'action', minWidth: 110, align: 'center' }
  ]
});

/** 将执行时间格式化为列表中的日期时间文本。 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-' : '-';
}

/** 将执行触发类型转换为页面展示名称。 */
function triggerName(type) {
  return ({ MANUAL: '手动执行', SCHEDULED: '定时执行' })[type] || type || '-';
}

/** 运行实例已有起止时间，按它们计算列表展示的耗时。 */
function formatDuration(startTime, endTime) {
  if (!startTime || !endTime) return '-';
  const seconds = Math.max(0, Math.floor((new Date(String(endTime).replace(' ', 'T')) - new Date(String(startTime).replace(' ', 'T'))) / 1000));
  return Number.isFinite(seconds) ? `${seconds}秒` : '-';
}

/** 打开当前运行实例的结果窗口。 */
function showResult(run) {
  selectedRun.value = run;
  resultVisible.value = true;
}
</script>

