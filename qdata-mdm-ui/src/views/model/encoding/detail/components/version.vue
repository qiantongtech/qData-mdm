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
      <el-button
        v-hasPermi="['model:encoding:version:add']"
        type="primary"
        plain
        icon="Plus"
        :loading="addingVersion"
        @click="handleAdd"
        @mousedown="(e) => e.preventDefault()"
      >
        新增
      </el-button>
    </template>

    <qt-table v-bind="tableStore" ref="tableRef">
      <template #action="{ row }">
        <el-button
          v-hasPermi="['model:encoding:version:query']"
          link
          type="primary"
          icon="view"
          @click="handleDetail(row)"
        >详情
        </el-button>
        <el-button
          :disabled="!(row.status === 1)"
          v-hasPermi="['model:encoding:version:edit']"
          link
          type="primary"
          icon="Position"
          @click="handleUpdate(row)"
        >
          发布
        </el-button>
        <el-button
          v-if="Number(row.status) === 1"
          v-hasPermi="['model:encoding:version:remove']"
          link
          type="danger"
          icon="Delete"
          @click="handleDelete(row)"
        >删除</el-button>
        <el-button
          v-else
          v-hasPermi="['model:encoding:version:edit']"
          :disabled="Number(row.status) !== 3"
          link
          type="danger"
          icon="CloseBold"
          @click="handleUpdate(row)"
        >停用</el-button>
      </template>
    </qt-table>
  </qt-wrap>

  <version-detail-dialog ref="versionDetailDialogRef" />

</template>

<script setup name="EncodingVersion">
/**
 * @description 编码方案版本列表，负责新增、发布、停用与草稿删除。
 * @prop {[String, Number]} encodingId 编码方案 ID
 * @emits change 版本新增、删除或状态变更成功，携带所属编码方案 ID
 */
import { useRoute } from "vue-router";

import {
  listVersion,
  addVersion,
  delVersion,
  updateStatus
} from "@/api/model/encoding/version";
import VersionDetailDialog from "./versionDetailDialog.vue";

const route = useRoute();
const { proxy } = getCurrentInstance();
const props = defineProps({
  encodingId: {
    type: [String, Number],
    default: undefined,
  },
});
const emit = defineEmits(["change"]);
const currentTypeId = computed(() => props.encodingId ?? route.query.id);

const tableRef = ref(null);
const addingVersion = ref(false);
const versionDetailDialogRef = ref();
const ids = ref([]);

/** 当前业务数据的导入配置 */
const data = reactive({
  form: {},
  queryParams: {
    encodingId: currentTypeId,
    versionNo: null,
    status: null,
    createTime: null,
  },
});

const { queryParams, form } = toRefs(data);

const tableStore = reactive({
  config: {
    sort: true,
    fullContent: false,
    table: {
      defaultSort: { prop: "createTime", order: "descending" },
    },
    actions: { table: { search: false, refresh: true, columns: true } },
  },
  columns: [
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    { label: "版本号", prop: "versionNo", align: "center", minWidth: 120 },
    {
      label: "备注",
      prop: "remark",
      align: "left",
      minWidth: 220,
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "状态", prop: "status", align: "center",
      dict: "model_encoding_status", minWidth: 100
    },
    { label: "创建人", prop: "createBy", minWidth: 180 },
    { label: "创建时间", prop: "createTime", sortable: true, date: true, minWidth: 180 },
    { label: "更新人", prop: "updateBy", minWidth: 180 },
    { label: "更新时间", prop: "updateTime", sortable: true, date: true, minWidth: 180 },
    { label: "操作", minWidth: 240, slot: "action", fixed: "right", align: "center" },
  ],
  func: listVersion,
  params: queryParams
});

/** 刷新编码方案版本列表 */
function refreshList() {
  tableRef.value?.getList();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    encodingId: currentTypeId,
    versionNo: null,
    status: null,
    remark: null,
    createTime: null,
  };
}

/** 新增按钮操作 */
async function handleAdd() {
  if (addingVersion.value) return;
  reset();
  const encodingId = currentTypeId.value;
  addingVersion.value = true;
  try {
    await addVersion(form.value);
    proxy.$modal.msgSuccess("新增成功");
    refreshList();
    emit("change", encodingId);
  } catch {
    // 接口失败由统一请求层提示。
  } finally {
    addingVersion.value = false;
  }
}

/** 详情按钮操作 */
function handleDetail(row) {
  versionDetailDialogRef.value?.openDialog(row.id || ids.value);
}

/** 仅草稿版本可删除；成功后同步刷新版本列表及详情中的当前版本。 */
async function handleDelete(row) {
  if (Number(row.status) !== 1) return;
  const encodingId = currentTypeId.value;
  try {
    await proxy.$modal.confirm(`确认删除版本“${row.versionNo}”吗？`);
    await delVersion(row.id);
    proxy.$modal.msgSuccess("删除成功");
    refreshList();
    emit("change", encodingId);
  } catch {
    // 取消操作无需处理，接口失败由统一请求层提示。
  }
}

/** 修改按钮操作 */
function handleUpdate(row) {
  const encodingId = currentTypeId.value;
  const text = row.status === 1 ? "发布" : "停用";
  const status = row.status === 1 ? 3 : 2;

  proxy.$modal
    .confirm('确认要"' + text + '","' + row.versionNo + `"吗？`)
    .then(function () {
      updateStatus(row.id, status)
        .then(() => {
          proxy.$modal.msgSuccess(text + "成功");
          refreshList();
          emit("change", encodingId);
        })
        .catch(() => {
          row.status = status;
        });
    })
    .catch(function () {
      row.status = status;
    });
}
</script>
