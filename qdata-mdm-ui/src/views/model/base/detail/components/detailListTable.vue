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
  <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
    <template #actions-data>
      <guide-tip-panel
        type="plain"
        :title="(props.mode === 'history' ? '查看当前模型定义的发布历史':'查看当前模型定义的版本').replace(/([^。.])$/, '$1。')"
      />
    </template>
    <qt-table v-bind="tableStore" ref="tableRef">
      <template #versionNo="{ row }">{{ row.versionNo || "-" }}</template>
      <template #versionStatus="{ row }">
        <dict-tag :options="model_status"
          :value="row.status"
        />
      </template>
      <template #releaseTime="{ row }">{{ formatReleaseTime(row) }}</template>
      <template #releaseBy="{ row }">{{ row.updateBy || row.createBy || "-" }}</template>
      <template #versionAction="{ row }">
        <el-button
          link
          :disabled="rows.length < 2"
          type="primary"
          icon="view"
          @click="openCompare(row)"
        >
          版本对比
        </el-button>
      </template>
      <template #releaseVersion="{ row }">{{ row.versionNo || "-" }}</template>
      <template #releaseStatus="{ row }">
        <dict-tag :type="publishStatusOptions[row.releaseResult]?.type || 'info'">
          {{ publishStatusOptions[row.releaseResult]?.label || row.releaseResult || "-" }}
        </dict-tag>
      </template>
    </qt-table>
    <version-compare-dialog
        ref="versionCompareDialogRef"
        :rows="rows"
        :current-version-id="currentVersionId"
    />
  </qt-wrap>
</template>

<script setup name="DetailListTable">

import {useDict} from "@/utils/dict";

/**
 * @description 模型版本与发布历史列表。
 * @prop {Array} rows 列表数据
 * @prop {String} mode 列表展示模式
 * @prop {[Number, String]} currentVersionId 当前版本 ID
 * @prop {Function} queryRows queryRows
 */
import VersionCompareDialog from "./versionCompareDialog.vue";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const {model_status} = useDict('model_status');

const props = defineProps({
  rows: { type: Array, default: () => [] },
  mode: { type: String, default: "version" },
  currentVersionId: { type: [Number, String], default: null },
  queryRows: { type: Function, default: null },
});
const tableRef = ref();
const versionCompareDialogRef = ref();

const versionColumns = [
  { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
  { label: "版本", prop: "versionNo", slot: "versionNo", align: "left", minWidth: 140 },
  { label: "版本状态", prop: "status", slot: "versionStatus", minWidth: 150 },
  { label: "发布时间", prop: "updateTime", slot: "releaseTime", minWidth: 180, align: "left", sortable: true },
  { label: "发布人", prop: "updateBy", slot: "releaseBy", minWidth: 140, align: "left" },
  { label: "操作", prop: "id", slot: "versionAction", minWidth: 120, fixed: "right" },
];
// 这里是发布实例状态，与版本管理中的草稿、已发布、封存状态不同。
const publishStatusOptions = {
  PREPARING: { label: "准备中", type: "info" },
  EXECUTING: { label: "执行中", type: "primary" },
  COMPENSATING: { label: "回撤中", type: "warning" },
  SUCCEEDED: { label: "成功", type: "success" },
  FAILED: { label: "失败", type: "danger" },
  PARTIAL_FAILED: { label: "部分失败", type: "danger" },
  UNKNOWN: { label: "待核验", type: "warning" },
  COMMIT_PENDING: { label: "待保存发布结果", type: "warning" },
};
const historyColumns = [
  { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
  { label: "发布版本", prop: "versionNo", slot: "releaseVersion", align: "left", minWidth: 150 },
  { label: "发布时间", prop: "releaseTime", date: true, minWidth: 180, sortable: true },
  { label: "发布人", prop: "releaseBy", align: "left", minWidth: 140 },
  { label: "发布结果", prop: "releaseResult", slot: "releaseStatus", minWidth: 140 },
  { label: "描述", prop: "releaseDescription", align: "left", minWidth: 240, showOverflowTooltip: true },
];

// 刷新时优先执行父组件提供的查询函数；未提供时兼容直接使用传入的静态数据。
const tableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    actions: { table: { search: false, refresh: true, columns: true } },
    table: {
      stripe: true, rowKey: "id",
    },
  },
  columns: props.mode === "history" ? historyColumns : versionColumns,
  func: async params => {
    if (props.queryRows) return props.queryRows(props.mode, params);
    return { data: { rows: props.rows, total: props.rows.length } };
  },
  params: {},
});

/** 格式化发布记录的时间，空值展示为占位符。 */
function formatReleaseTime(row) {
  return row.updateTime || row.createTime || "-";
}

/** 打开所选模型版本的比较弹窗。 */
function openCompare(row) {
  versionCompareDialogRef.value?.openDialog(row);
}
</script>
