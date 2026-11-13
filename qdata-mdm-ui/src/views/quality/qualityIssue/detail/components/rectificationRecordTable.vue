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
  <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableStore.config">
    <template #actions-data>
      <GuideTipPanel type="plain" title="查看当前问题数据关联的整改任务。" />
    </template>

    <qt-table v-bind="tableStore" ref="tableRef">
      <template #status="{ row }">
        <dict-tag :options="taskStatusOptions" :value="row.status" />
      </template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="RectificationRecordTable">
import {reactive, ref} from 'vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';

const props = defineProps({
  queryRows: {type: Function, required: true}
});
const tableRef = ref();
const taskStatusOptions = [
  {value: '1', label: '待审批', elTagType: 'warning'},
  {value: '2', label: '审批中', elTagType: 'primary'},
  {value: '3', label: '已完成', elTagType: 'success'},
  {value: '4', label: '已取消', elTagType: 'info'},
  {value: '5', label: '处理中', elTagType: 'primary'}
];

const tableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    actions: {table: {search: false, refresh: true, columns: true}},
    table: {stripe: true, rowKey: 'id'}
  },
  columns: [
    {label: '编号', prop: 'id', width: 60, align: 'center', sortable: true},
    {label: '整改任务', prop: 'taskNo', minWidth: 190, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '问题字段', prop: 'fieldName', minWidth: 180, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '质量规则', prop: 'ruleName', minWidth: 220, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '负责人', prop: 'assigneeName', minWidth: 140, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '任务状态', prop: 'status', slot: 'status', minWidth: 140, align: 'left'}
  ],
  func: params => props.queryRows(params),
  params: {}
});

defineExpose({
  getList: () => tableRef.value?.getList(),
  resetQuery: () => tableRef.value?.resetQuery()
});
</script>
