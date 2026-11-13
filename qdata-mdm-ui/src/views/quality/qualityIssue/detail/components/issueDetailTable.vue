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
      <GuideTipPanel type="plain" title="查看当前问题数据的检测明细。" />
    </template>

    <qt-table v-bind="tableStore" ref="tableRef">
      <template #status="{ row }">
        <dict-tag :options="statusOptions" :value="row.status" />
      </template>
      <template #rectification="{ row }">
        <RectificationStatusTag :value="row.rectificationDesc || '未下发'" />
      </template>
      <template #action="{ row }">
        <el-button
          link
          type="primary"
          icon="Promotion"
          :disabled="String(issueStatus) === '3' || String(row.status) === '2'"
          @click="emit('assign', row)"
        >
          下发整改
        </el-button>
      </template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="IssueDetailTable">
import {reactive, ref} from 'vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import RectificationStatusTag from '../../components/RectificationStatusTag.vue';

const props = defineProps({
  queryRows: {type: Function, required: true},
  issueStatus: {type: [String, Number], default: ''}
});
const emit = defineEmits(['assign']);
const tableRef = ref();
const statusOptions = [
  {value: '1', label: '未解决', elTagType: 'warning'},
  {value: '2', label: '已解决', elTagType: 'success'}
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
    {label: '问题字段', prop: 'fieldName', minWidth: 180, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '质量规则', prop: 'ruleName', minWidth: 220, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '失败原因', prop: 'failReason', minWidth: 260, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '当前状态', prop: 'status', slot: 'status', minWidth: 110, align: 'left'},
    {label: '整改情况', prop: 'rectificationDesc', slot: 'rectification', minWidth: 180, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '操作', slot: 'action', minWidth: 110, fixed: 'right'}
  ],
  func: params => props.queryRows(params),
  params: {}
});

defineExpose({
  getList: () => tableRef.value?.getList(),
  resetQuery: () => tableRef.value?.resetQuery()
});
</script>
