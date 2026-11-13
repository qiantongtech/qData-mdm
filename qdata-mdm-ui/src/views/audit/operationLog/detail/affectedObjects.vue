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

<!-- 关联对象：该次操作影响的业务对象清单 -->
<!--
  @file operationLog/detail/affectedObjects.vue
  @description 操作审计影响对象列表
  @author zyy
  @prop logList 包含影响对象的审计记录
-->
<template>
  <div ref="app-container">
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef" :config="{ actions: { table: { columns: false } } }">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          :tableRef="tableRef"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>

      <template #actions-data>
        <span class="audit-tip">该次操作影响的业务对象</span>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #objectName="{ row }">
          <div class="cell-main">{{ row.objectName }}</div>
          <div class="cell-sub">{{ row.objectCode }}</div>
        </template>
        <template #lifecycle="{ row }">
          <dict-tag :type="row.lifecycle === '生效' ? 'success' : 'info'">{{ row.lifecycle }}</dict-tag>
        </template>
        <template #result="{ row }">
          <dict-tag :type="row.result === '成功' ? 'success' : row.result === '失败' ? 'danger' : 'info'">
            {{ row.result }}
          </dict-tag>
        </template>
      </qt-table>
    </qt-wrap>
  </div>
</template>

<script setup name="AuditAffectedObjects">

const props = defineProps({
  logList: {
    type: Array,
    default: () => [],
  },
});

const tableRef = ref(null);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    objectCode: null,
    modelName: "",
    result: "",
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：必须是 reactive 原始对象（不能传 ref） */
const queryParams = data.queryParams;

/** 只展示调用方传入的影响对象。 */
function getList(params) {
  const source = props.logList.flatMap(item => Array.isArray(item.affectedRows) ? item.affectedRows : []);
  const rows = source.filter(item =>
      (!params.objectCode || (item.objectCode || '').includes(params.objectCode)) &&
      (!params.modelName || item.modelName === params.modelName) &&
      (!params.result || item.result === params.result),
  );
  return Promise.resolve({ code: 200, data: { rows, total: rows.length } });
}

const tableStore = reactive({
  config: {
    stripe: true,
    notPagination: true,
    notPaginationParams: true,
    table: { rowKey: "id", stripe: true, maxHeight: 420 },
  },
  columns: [
    { type: "selection", width: 55, minWidth: 55, align: "center" },
    { label: "序号", type: "index", width: 60, minWidth: 60, align: "center" },
    {
      label: "业务对象",
      prop: "objectName",
      slot: "objectName",
      align: "left",
      minWidth: 200,
      showOverflowTooltip: { effect: "light" },
    },
    { label: "主数据模型", prop: "modelName", align: "left", width: 140, minWidth: 140 },
    { label: "当前生命周期", prop: "lifecycle", slot: "lifecycle", align: "center", width: 120, minWidth: 120 },
    { label: "本次处理结果", prop: "result", slot: "result", width: 120, minWidth: 120 },
  ],
  func: getList,
  params: queryParams,
});

const searchStore = reactive({
  items: [
    { label: "对象编码", prop: "objectCode", component: { is: "input" } },
    {
      label: "主数据模型",
      prop: "modelName",
      component: {
        is: "select",
        placeholder: "全部模型",
        options: [
          { value: "", label: "全部模型" },
          { value: "供应商主数据", label: "供应商主数据" },
          { value: "供应商主数据模型", label: "供应商主数据模型" },
          { value: "客户主数据", label: "客户主数据" },
          { value: "客户主数据模型", label: "客户主数据模型" },
          { value: "物料主数据", label: "物料主数据" },
          { value: "物料主数据模型", label: "物料主数据模型" },
        ],
      },
    },
    {
      label: "处理结果",
      prop: "result",
      component: {
        is: "select",
        placeholder: "全部结果",
        options: [
          { value: "", label: "全部结果" },
          { value: "成功", label: "成功" },
          { value: "失败", label: "失败" },
          { value: "回滚", label: "回滚" },
        ],
      },
    },
  ],
});

</script>

<style lang="scss" scoped>
.audit-tip {
  color: var(--qt-text-secondary);
  font-size: 13px;
}

.cell-main {
  line-height: 20px;
}

.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
}
</style>
