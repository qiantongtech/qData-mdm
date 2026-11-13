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
  <div ref="app-container" class="app-container">
    <guide-tip tip-id="model/encoding.list" />
    <qt-wrap :columns="tableStore.columns" :table-ref="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>

      <template #actions-data>
        <el-button
          v-hasPermi="['model:encoding:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          @mousedown="(e) => e.preventDefault()"
        >
          新增
        </el-button>
        <el-button
          v-hasPermi="['model:encoding:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="!ids.length"
          @click="handleBatchDelete"
        >批量删除
        </el-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #versionNo="{ row }">
          <span>{{ row.version?.versionNo || "" }}</span>
        </template>
        <template #status="{ row }">
          <dict-tag v-if="row.version?.status"
            :options="model_encoding_status"
            :value="row.version?.status"
          />
          <span v-else></span>
        </template>
        <template #action="{ row }">
          <el-button
            v-hasPermi="['model:encoding:query']"
            link
            type="primary"
            icon="view"
            @click="handleDetail(row)"
          >详情
          </el-button>
          <el-button
            v-hasPermi="['model:encoding:edit']"
            link
            type="primary"
            icon="Edit"
            :disabled="Number(row.version?.status) === 3"
            @click="handleUpdate(row)"
          >修改
          </el-button>
          <el-button
            v-hasPermi="['model:encoding:remove']"
            link
            type="danger"
            icon="Delete"
            :disabled="Number(row.version?.status) === 3"
            @click="handleDelete(row)"
          >删除
          </el-button>
        </template>
      </qt-table>
    </qt-wrap>
    <encoding-form-dialog ref="encodingDialogRef" @success="refreshList" />
  </div>
</template>

<script setup name="ModelEncoding">
/** @description 编码方案列表，负责方案查询与维护。 */
import { listEncoding, delEncoding } from "@/api/model/encoding/encoding.js";
import EncodingFormDialog from "./components/encodingFormDialog.vue";

const { proxy } = getCurrentInstance();
const { model_encoding_status } = proxy.useDict(
  "model_encoding_status",
);

const ids = ref([]);
const encodingDialogRef = ref();

const data = reactive({
  queryParams: {
    code: null,
    name: null,
    currentVersionId: null,
    description: null,
    createTime: null,
  }
});

const { queryParams } = toRefs(data);

const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    stripe: true,
    notPagination: false,
    notPaginationParams: true,
    table: {
      onSelectionChange: handleSelectionChange,
      rowKey: "id",
      defaultExpandAll: false,
      lazy: false,
      load: null,
      treeProps: { children: "children", hasChildren: "hasChildren" },
      defaultSort: { prop: "createTime", order: "descending" },
    },
  },
  columns: [
    { type: "selection", width: 55, align: "center" },
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    {
      label: "方案名称",
      prop: "name",
      align: "left",
      minWidth: 200,
      info: {
        title: "name",
        get click() {
          return proxy.$auth.hasPermi("model:encoding:query")
                  ? handleDetail
                  : undefined;
        },
      },
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "方案编码",
      prop: "code",
      align: "left",
      minWidth: 240,
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      minWidth: 240,
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "当前版本",
      prop: "version",
      slot: "versionNo",
      align: "center",
      minWidth: 100,
    },
    {
      label: "使用模型",
      prop: "modelCount",
      minWidth: 100,
    },
    {
      label: "版本状态",
      prop: "version",
      slot: "status",
      align: "left",
      minWidth: 100,
    },
    {
      label: "创建时间",
      prop: "createTime",
      minWidth: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    { label: "操作", minWidth: 240, fixed: "right", slot: "action" },
  ],
  func: listEncoding,
  params: queryParams
});

const searchStore = reactive({
  items: [
    {
      label: "方案编码",
      prop: "code",
      component: {
        is: "input",
      },
    },
    {
      label: "方案名称",
      prop: "name",
      component: {
        is: "input",
      },
    },
  ],
});

/** 刷新编码方案列表 */
function refreshList() {
  tableRef.value?.getList();
}

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  tableRef.value?.resetQuery();
}

/** 新增按钮操作 */
function handleAdd() {
  encodingDialogRef.value.openDialog();
}

/** 修改按钮操作 */
function handleUpdate(row) {
  encodingDialogRef.value.openDialog(row.id || ids.value);
}

/** 详情按钮操作 */
function handleDetail(row) {
  proxy.$router.push({
    path: "/model/encoding/detail",
    query: { id: row.id },
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  if (row && Number(row.modelCount || 0) > 1) {
    proxy.$modal.msgWarning("删除失败,编码被主数据模型引用!");
    return;
  }
  proxy.$modal.confirm('是否确认删除编码方案编号为"' + _ids + '"的数据项？').then(function () {
    return delEncoding(_ids);
  }).then(() => {
    refreshList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 记录列表选中项 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
}

/** 批量删除编码方案 */
function handleBatchDelete() {
  proxy.$modal.confirm('是否确认删除选中的编码方案？').then(() => delEncoding(ids.value.join(','))).then(() => {
    refreshList();
    ids.value = [];
    proxy.$modal.msgSuccess("删除成功");
  });
}
</script>
