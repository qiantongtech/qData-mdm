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
  <div ref="app-container">
    <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
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
          v-hasPermi="['model:dict:data:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          @mousedown="(e) => e.preventDefault()"
        >
          新增
        </el-button>
        <el-button
          v-hasPermi="['model:dict:data:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleBatchDelete"
        >批量删除</el-button>
        <el-button
          v-hasPermi="['model:dict:data:import']"
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          @mousedown="(e) => e.preventDefault()"
        >
          导入
        </el-button>
        <el-button
          v-hasPermi="['model:dict:data:export']"
          type="warning"
          plain
          icon="Download"
          @click="handleExport"
          @mousedown="(e) => e.preventDefault()"
        >
          导出
        </el-button>
      </template>

      <qt-table v-bind="tableStore" ref="tableRef">
        <template #id="{ row }">
          {{ row.id || '-' }}
        </template>
        <template #code="{ row }">
          {{ row.code || '-' }}
        </template>
        <template #name="{ row }">
          {{ row.name || '-' }}
        </template>
        <template #description="{ row }">
          {{ row.description || '-' }}
        </template>
        <template #sortOrder="{ row }">
          {{ row.sortOrder || '-' }}
        </template>
        <template #action="{ row }">
          <el-button
            v-hasPermi="['model:dict:data:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(row)"
          >修改
          </el-button>
          <el-button
            v-hasPermi="['model:dict:data:remove']"
            link
            type="danger"
            icon="Delete"
            :disabled="row.validFlag === true"
            @click="handleDelete(row)"
          >删除
          </el-button>
        </template>
        <template #validFlag="{ row }">
          <el-switch
            v-model="row.validFlag"
            :disabled="!checkPermi(['model:dict:data:edit'])"
            :active-value="true"
            :inactive-value="false"
            :loading="statusChanging.has(row.id)"
            @change="handleStatusChange(row)"
          >
          </el-switch>
        </template>
      </qt-table>
    </qt-wrap>

    <data-form-dialog
      v-model:open="open"
      :title="title"
      :form="form"
      :rules="rules"
      :type-id="currentTypeId"
      :append-to="$refs['app-container']"
      @cancel="cancel"
      @submit="submitForm"
    />

    <!-- 用户导入对话框 -->
    <el-dialog
      v-model="upload.open"
      :title="upload.title"
      width="800px"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <upload-filled />
        </el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />
              是否更新已经存在的参考数据项数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link
              type="primary"
              :underline="false"
              class="template-link"
              @click="importTemplate"
            >下载模板
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="upload.open = false">取 消</el-button>
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="DictData">
/**
 * @description 参考数据项列表，负责查询、维护与导入导出。
 * @prop {[String, Number]} typeId 参考数据类型或主数据域 ID
 */
import { useRoute } from "vue-router";

import { checkPermi } from "@/utils/permission";
import { listData, getData, delData, addData, updateData, updateDataStatus } from "@/api/model/dict/data/data";
import { getToken } from "@/utils/auth.js";
import DataFormDialog from "./components/dataFormDialog.vue";
import { trimText, validatePattern, validateTrimmedRequired } from "@/utils/validate.js";

const { proxy } = getCurrentInstance();
const statusChanging = reactive(new Set());
const props = defineProps({
  typeId: {
    type: [String, Number],
    default: undefined,
  },
});
const emit = defineEmits(["itemsChange"]);

const open = ref(false);
const tableRef = ref(null);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const title = ref("");

/** 当前业务数据的导入配置 */
const upload = reactive({
  // 是否显示弹出层（数据导入）
  open: false,
  // 弹出层标题（数据导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的业务数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/model/dict/data/importData"
});
const route = useRoute();
const currentTypeId = computed(() => props.typeId ?? route.query.id);

const data = reactive({
  form: {},
  queryParams: {
    typeId: currentTypeId.value,
    code: null,
    name: null,
    sortOrder: null,
    createTime: null,
    description: null,
  },
  rules: {
    code: [
      { required: true, validator: validateTrimmedRequired("编码值不能为空", 64), trigger: "blur" },
      {
        validator: validatePattern(
            /^[A-Za-z0-9_]+$/,
          "编码值支持英文字母、数字和下划线"
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    name: [
      { required: true, validator: validateTrimmedRequired("名称不能为空", 64), trigger: "blur" },
      {
        validator: validatePattern(
            /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "名称支持中文、英文字符、数字、下划线和短横线格"
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    sortOrder: [{ required: true, message: "显示排序不能为空", trigger: "blur" }],
    validFlag: [{ required: true, message: "状态不能为空", trigger: "blur" }],
  }
});

const { queryParams, form, rules } = toRefs(data);

const tableStore = reactive({
  config: {
    stripe: true,
    fullContent: false,
    notPagination: false,
    notPaginationParams: true,
    sort: true,
    search: true,
    table: {
      rowKey: "id",
      stripe: true,
      defaultSort: { prop: "sortOrder", order: "ascending" },
      onSelectionChange: handleSelectionChange,
    }
  },
  columns: [
    { type: "selection", width: 55, align: "center" },
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    { label: "编码值", prop: "code", slot: "code", align: "left",  minWidth: 200 },
    { label: "名称", prop: "name", slot: "name", align: "left", minWidth: 200 },
    {
      label: "描述",
      prop: "description",
      slot: "description",
      align: "left",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "排序",
      prop: "sortOrder",
      sortable: true,
      sortableKey: "sortOrder",
      minWidth: 80
    },
    {
      label: "状态", prop: "validFlag", slot: "validFlag",
      minWidth: 100
    },
    {
      label: "创建时间",
      prop: "createTime",
      minWidth: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    { label: "操作", minWidth: 240, slot: "action", fixed: "right", align: "center" },
  ],
  func: listData,
  params: queryParams
});

const searchStore = reactive({
  items: [
    {
      label: "编码值",
      prop: "code",
      component: {
        is: "input",
      },
    },
    {
      label: "名称",
      prop: "name",
      component: {
        is: "input",
      },
    },
    {
      label: "状态",
      prop: "validFlag",
      component: {
        is: "select",
        placeholder: "请选择状态",
        options: [
          { value: true, label: "启用" },
          { value: false, label: "禁用" },
        ],
      },
    },
  ],
});

/** 数据项写入后刷新当前列表，并通知详情页更新总数。 */
function refreshList() {
  tableRef.value?.getList();
  emit("itemsChange");
}

watch(
  currentTypeId,
  value => {
    queryParams.value.typeId = value;
    if (form.value) {
      form.value.typeId = value;
    }
    nextTick(() => tableRef.value?.resetQuery());
  }
);

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    typeId: currentTypeId.value,
    code: null,
    name: null,
    sortOrder: 0,
    description: null,
    validFlag: true,
  };
}

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  tableRef.value?.resetQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增参考数据项";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getData(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改参考数据项";
  });
}

/** 提交按钮 */
function submitForm(submitData) {
  form.value = submitData;
  if (submitData.id != null) {
    updateData(submitData).then(() => {
      proxy.$modal.msgSuccess("修改成功");
      open.value = false;
      refreshList();
    }).catch(() => {
    });
  } else {
    addData(submitData).then(() => {
      proxy.$modal.msgSuccess("新增成功");
      open.value = false;
      refreshList();
    }).catch(() => {
    });
  }
}

/** 确认后更新启停状态，取消或失败时还原开关。 */
async function handleStatusChange(row) {
  if (statusChanging.has(row.id)) return;
  const enabled = row.validFlag === true;
  const previousStatus = !enabled;
  const text = enabled ? "启用" : "禁用";
  statusChanging.add(row.id);
  try {
    await proxy.$modal.confirm(`确认要${text}“${row.name}”参考数据项吗？`);
    await updateDataStatus(row.id, enabled);
  } catch {
    row.validFlag = previousStatus;
    return;
  } finally {
    statusChanging.delete(row.id);
  }
  proxy.$modal.msgSuccess(text + "成功");
  refreshList();
}

/** 批量删除参考数据项。 */
function handleBatchDelete() {
  proxy.$modal.confirm("是否确认删除选中的参考数据项？").then(() => {
    return delData(ids.value.join(","));
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    refreshList();
  }).catch(() => {
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除参考数据项编号为"' + _ids + '"的数据项？').then(function () {
    return delData(_ids);
  }).then(() => {
    refreshList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('model/dict/data/export', {
    ...queryParams.value
  }, `data_${new Date().getTime()}.xlsx`);
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = "参考数据项导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {}, `data_template_${new Date().getTime()}.xlsx`);
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
};

/** 文件上传中处理 */
const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert("<div class='dict-data-import-result'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  refreshList();
};

</script>

<style scoped lang="scss">
.template-link {
  font-size: var(--qt-font-size-small);
  vertical-align: baseline;
}

:global(.dict-data-import-result) {
  max-height: var(--qt-import-result-max-height);
  padding: var(--qt-import-result-padding);
  overflow: auto;
  overflow-x: hidden;
}
</style>
