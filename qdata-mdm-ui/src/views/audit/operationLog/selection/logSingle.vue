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

<!-- 业务操作审计-单选弹窗：对外保持 open / confirm / cancel 契约 -->
<!--
  @file operationLog/selection/logSingle.vue
  @description 操作审计记录单选
  @author zyy
  @emits open 打开选择器
  @emits confirm 确认选中记录
  @emits cancel 取消选择
-->
<template>
  <el-dialog
    title="业务操作审计-单选"
    v-model="visible"
    width="1200px"
    :append-to="$refs['app-container']"
    draggable
    destroy-on-close
    @close="cancel"
  >
    <div ref="app-container">
      <qt-wrap :columns="tableStore.columns" :tableRef="tableRef" :config="{ fullContent: false }">
        <template #search>
          <qt-search-bar
            v-bind="searchStore"
            :params="tableStore.params"
            :tableRef="tableRef"
            @query="handleQuery"
            @reset="resetQuery"
          />
        </template>

        <qt-table v-bind="tableStore" ref="tableRef">
          <template #objectName="{ row }">
            <div class="cell-main">{{ row.objectName || '-' }}</div>
            <div class="cell-sub">{{ row.objectId || '-' }}</div>
          </template>
          <template #operateResult="{ row }">
            <dict-tag :type="row.operateResult === '成功' ? 'success' : 'danger'">
              {{ row.operateResult || '-' }}
            </dict-tag>
          </template>
        </qt-table>
      </qt-wrap>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="confirm">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="LOGSingle">
import { listLOG } from '@/api/audit/operationLog';

const { proxy } = getCurrentInstance();

const visible = ref(false);
// 定义单选数据
const single = ref();
// 当前界面table
const tableRef = ref();
// 当前页数据（由 qt-table 的 formatData 事件回传，用于翻页后回显选中行）
const pageRows = ref([]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    moduleCode: "",
    functionCode: null,
    objectName: null,
    type: null,
    operatorName: null,
    operateResult: "",
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：必须是 reactive 原始对象（不能传 ref） */
const queryParams = data.queryParams;

const emit = defineEmits(["open", "confirm", "cancel"]);

/** 查询真实操作日志。 */
function getList(params) {
  return listLOG(params);
}

const tableStore = reactive({
  config: {
    stripe: true,
    notPaginationParams: true,
    table: {
      rowKey: "id",
      stripe: true,
      highlightCurrentRow: true,
      maxHeight: 380,
      onCurrentChange: handleCurrentChange,
    },
  },
  columns: [
    { label: "主键ID", prop: "id", align: "center", width: 90, minWidth: 90 },
    { label: "业务模块编码", prop: "moduleCode", align: "center", width: 120, minWidth: 120 },
    { label: "功能编码", prop: "functionCode", align: "center", width: 160, minWidth: 160 },
    { label: "业务对象类型", prop: "objectType", align: "center", width: 120, minWidth: 120 },
    {
      label: "业务对象",
      prop: "objectName",
      slot: "objectName",
      align: "left",
      minWidth: 180,
      showOverflowTooltip: { effect: "light" },
    },
    { label: "操作类型", prop: "type", align: "center", width: 110, minWidth: 110 },
    { label: "操作人名称", prop: "operatorName", align: "center", width: 110, minWidth: 110 },
    { label: "操作结果", prop: "operateResult", slot: "operateResult", width: 100, minWidth: 100 },
    {
      label: "操作时间",
      prop: "operateTime",
      date: "{y}-{m}-{d} {h}:{i}:{s}",
      align: "center",
      width: 170, minWidth: 170,
    },
    { label: "请求IP", prop: "requestIp", align: "center", width: 130, minWidth: 130 },
  ],
  func: getList,
  params: queryParams,
  events: {
    formatData: (data) => {
      // 记录当前页数据，并在分页切换后同步当前行选中状态
      pageRows.value = data;
      nextTick(() => setCurrentRow(single.value));
      return data;
    },
  },
});

const searchStore = reactive({
  items: [
    {
      label: "业务模块",
      prop: "moduleCode",
      component: {
        is: "select",
        placeholder: "全部模块",
        options: [
          { value: "", label: "全部模块" },
          { value: "GOVERN", label: "数据治理" },
          { value: "MODEL", label: "模型管理" },
          { value: "ACCESS", label: "数据接入" },
        ],
      },
    },
    { label: "功能编码", prop: "functionCode", component: { is: "input" } },
    { label: "操作对象", prop: "objectName", component: { is: "input" } },
    { label: "操作人", prop: "operatorName", component: { is: "input" } },
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

/** 单选选中事件 */
function handleCurrentChange(selection) {
  if (selection) {
    single.value = selection;
  }
}

/**
 * 设置当前行
 * @param {Object} row 行对象
 * @returns 更改选中对象
 */
function setCurrentRow(row) {
  if (!row) return;
  const target = pageRows.value.find(item => item.id == row.id);
  if (target) {
    tableRef.value?.elTableRef?.setCurrentRow(target);
  }
}

/**
 * 打开选择框
 * @param {Object} val 选中的对象
 */
function open(val) {
  visible.value = true;
  single.value = val;
  nextTick(() => {
    tableRef.value?.resetQuery();
  });
}

/**
 * 取消按钮
 * @description 取消按钮时，重置所有状态
 */
function cancel() {
  queryParams.pageNum = 1;
  single.value = undefined;
  visible.value = false;
  emit("cancel");
}

/**
 * 确定按钮
 * @description 确定按钮时，emit confirm 事件，以便父组件接收到选中的数据
 */
function confirm() {
  if (!single.value) {
    proxy.$modal.msgWarning("请选择数据！");
    return;
  }
  emit("confirm", single.value);
  visible.value = false;
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1;
}

/** 重置按钮操作 */
function resetQuery() {
}

defineExpose({ open });
</script>

<style lang="scss" scoped>
.cell-main {
  line-height: 20px;
}

.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
}
</style>
