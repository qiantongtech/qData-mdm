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

<!-- 操作明细：审计记录只读，不提供新增、修改、删除 -->
<!--
  @file operationLog/detail/operationDetails.vue
  @description 操作审计操作明细列表
  @author zyy
  @prop logList 待展示的审计记录
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
        <span class="audit-tip">记录该业务操作的执行明细</span>
        <el-button
          type="warning"
          plain
          @click="handleExport"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-download-line mr5"></i>导出
        </el-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #type="{ row }">
          <dict-tag :type="operationTagType(row.type)">{{ row.type }}</dict-tag>
        </template>
        <template #objectName="{ row }">
          <div class="cell-main">{{ row.objectName || '-' }}</div>
          <div class="cell-sub">{{ row.modelCode || row.objectId || '-' }}</div>
        </template>
        <template #modelVersion="{ row }">
          <dict-tag v-if="row.modelVersion" type="primary">{{ row.modelVersion }}</dict-tag>
          <span v-else>-</span>
        </template>
        <template #action="{ row }">
          <el-button link
                     :underline="false"
                     type="primary"
                     icon="View"
                     @click="handleDetail(row)"
          >详情</el-button>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 业务操作审计详情对话框 -->
    <el-dialog
      v-model="detailOpen"
      title="业务操作审计详情"
      width="800px"
      :append-to="$refs['app-container']"
      draggable
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item v-for="item in detailFields" :key="item.label" :label="item.label">
          {{ item.value }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="AuditOperationDetails">
const operationTypeTagMap = {
  批量修改: 'primary', 修改: 'primary', 冻结: 'info', 失效: 'danger',
  历史版本恢复: 'warning', 批量导入: 'success', 修改模型: 'primary',
  发布模型: 'success', 发起修订: 'warning', 新增模型: 'success', 停用封存: 'danger',
};

const { proxy } = getCurrentInstance();

const props = defineProps({
  logList: {
    type: Array,
    default: () => [],
  },
});

const tableRef = ref(null);
const detailOpen = ref(false);
const detailRow = ref({});

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    operatorName: null,
    operateId: null,
    type: "",
    operateResult: "",
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：必须是 reactive 原始对象（不能传 ref） */
const queryParams = data.queryParams;

/** 只展示调用方传入的操作记录。 */
function getList(params) {
  const source = props.logList;
  const rows = source.filter(item =>
      (!params.operatorName || (item.operatorName || '').includes(params.operatorName)) &&
      (!params.operateId || (item.operateId || '').includes(params.operateId)) &&
      (!params.type || item.type === params.type) &&
      (!params.operateResult || item.operateResult === params.operateResult),
  );
  return Promise.resolve({ code: 200, data: { rows, total: rows.length } });
}

const tableStore = reactive({
  config: {
    stripe: true,
    notPagination: true,
    notPaginationParams: true,
    table: {
      rowKey: "id",
      stripe: true,
      maxHeight: 420,
      defaultSort: { prop: "operateTime", order: "descending" },
    },
  },
  columns: [
    { type: "selection", width: 55, minWidth: 55, align: "center" },
    { label: "序号", type: "index", width: 60, minWidth: 60, align: "center" },
    {
      label: "操作时间",
      prop: "operateTime",
      date: "{y}-{m}-{d} {h}:{i}:{s}",
      align: "center",
      width: 170, minWidth: 170,
    },
    { label: "操作人", prop: "operatorName", align: "center", width: 100, minWidth: 100 },
    { label: "操作类型", prop: "type", slot: "type", align: "center", width: 130, minWidth: 130 },
    { label: "主数据模型", prop: "objectType", align: "left", width: 160, minWidth: 160 },
    {
      label: "业务对象",
      prop: "objectName",
      slot: "objectName",
      align: "left",
      minWidth: 180,
      showOverflowTooltip: { effect: "light" },
    },
    { label: "模型版本", prop: "modelVersion", slot: "modelVersion", align: "center", width: 110, minWidth: 110 },
    { label: "操作编号", prop: "operateId", align: "left", width: 170, minWidth: 170 },
    { label: "请求IP", prop: "requestIp", align: "center", width: 130, minWidth: 130 },
    { label: "操作", prop: "action", slot: "action", width: 90, minWidth: 90, fixed: "right", align: "center" },
  ],
  func: getList,
  params: queryParams,
});

const searchStore = reactive({
  items: [
    { label: "操作编号", prop: "operateId", component: { is: "input" } },
    { label: "操作人", prop: "operatorName", component: { is: "input" } },
    {
      label: "操作类型",
      prop: "type",
      component: {
        is: "select",
        placeholder: "请选择操作类型",
        options: [
          { value: "", label: "全部类型" },
          { value: "批量修改", label: "批量修改" },
          { value: "修改", label: "修改" },
          { value: "冻结", label: "冻结" },
          { value: "失效", label: "失效" },
          { value: "历史版本恢复", label: "历史版本恢复" },
          { value: "批量导入", label: "批量导入" },
          { value: "修改模型", label: "修改模型" },
          { value: "发布模型", label: "发布模型" },
          { value: "发起修订", label: "发起修订" },
          { value: "新增模型", label: "新增模型" },
          { value: "停用封存", label: "停用封存" },
        ],
      },
    },
    {
      label: "操作结果",
      prop: "operateResult",
      component: {
        is: "select",
        placeholder: "全部结果",
        options: [
          { value: "", label: "全部结果" },
          { value: "成功", label: "成功" },
          { value: "失败", label: "失败" },
        ],
      },
    },
  ],
});

/** 操作类型标签配色 */
function operationTagType(type) {
  return operationTypeTagMap[type] || 'primary';
}

/** 详情字段 */
const detailFields = computed(() => {
  const info = detailRow.value || {};
  return [
    { label: "主键ID", value: info.id ?? '-' },
    { label: "操作编号", value: info.operateId || '-' },
    { label: "业务范围", value: info.businessScope || '-' },
    { label: "所属功能", value: info.functionName || '-' },
    { label: "业务模块编码", value: info.moduleCode || '-' },
    { label: "功能编码", value: info.functionCode || '-' },
    { label: "对象类型", value: info.objectType || '-' },
    { label: "业务对象ID", value: info.objectId || '-' },
    { label: "业务对象名称", value: info.objectName || '-' },
    { label: "模型编码", value: info.modelCode || '-' },
    { label: "模型版本", value: info.modelVersion || '-' },
    { label: "操作类型", value: info.type || '-' },
    { label: "操作人名称", value: info.operatorName || '-' },
    { label: "操作结果", value: info.operateResult || '-' },
    {
      label: "操作时间",
      value: info.operateTime ? proxy.parseTime(info.operateTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-',
    },
    { label: "操作说明", value: info.operateDesc || '-' },
    { label: "请求IP", value: info.requestIp || '-' },
    { label: "请求ID", value: info.requestId || '-' },
    { label: "链路追踪ID", value: info.traceId || '-' },
    { label: "错误信息", value: info.errorMessage || '-' },
    { label: "备注", value: info.remark || '-' },
  ];
});

</script>

<style lang="scss" scoped>
.audit-tip {
  color: var(--qt-text-secondary);
  font-size: 13px;
  margin-right: 12px;
}

.cell-main {
  line-height: 20px;
}

.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

/* 操作列：去掉 link 按钮及内部文字的下划线 */
:deep(.el-button.is-link),
:deep(.el-button.is-link > span) {
  text-decoration: none !important;
}
</style>
