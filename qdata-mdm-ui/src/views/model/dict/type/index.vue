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
    <guide-tip tip-id="model/dict.list" />
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
          v-hasPermi="['model:dict:type:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          @mousedown="(e) => e.preventDefault()"
        >
          新增
        </el-button>
        <el-button
          v-hasPermi="['model:dict:type:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleBatchDelete"
        >批量删除
        </el-button>
        <el-button
          v-hasPermi="['model:dict:type:import']"
          type="info"
          plain
          icon="Upload"
          @click="handleImport"
          @mousedown="(e) => e.preventDefault()"
        >
          导入
        </el-button>
        <el-button
          v-hasPermi="['model:dict:type:export']"
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
        <template #action="{ row }">
          <el-button
            v-hasPermi="['model:dict:type:query']"
            link
            type="primary"
            icon="view"
            @click="handleDetail(row)"
          >详情
          </el-button>
          <el-button
            v-hasPermi="['model:dict:type:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(row)"
          >修改
          </el-button>
          <el-button
            v-hasPermi="['model:dict:type:remove']"
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
            :disabled="!checkPermi(['model:dict:type:edit'])"
            :active-value="true"
            :inactive-value="false"
            :loading="statusChanging.has(row.id)"
            @change="handleStatusChange(row)"
          >
          </el-switch>
        </template>
      </qt-table>
    </qt-wrap>

    <type-form-dialog
      v-model:open="open"
      :title="title"
      :form="form"
      :rules="rules"
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
              是否更新已经存在的参考数据数据
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

<script setup name="DictType">
/** @description 参考数据类型列表，负责查询、维护与导入导出。 */
import { checkPermi } from "@/utils/permission";
import {
  listType,
  getType,
  delType,
  addType,
  updateType,
  updateTypeStatus
} from "@/api/model/dict/type/type";
import { getToken } from "@/utils/auth.js";
import TypeFormDialog from "./components/typeFormDialog.vue";
import { trimText, validatePattern, validateTrimmedRequired } from "@/utils/validate.js";

const { proxy } = getCurrentInstance();
const statusChanging = reactive(new Set());

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
  url: import.meta.env.VITE_APP_BASE_API + "/model/dict/type/importData"
});

const data = reactive({
  form: {},
  queryParams: {
    code: null,
    name: null,
    description: null,
    createTime: null,
  },
  rules: {
    code: [
      { required: true, validator: validateTrimmedRequired("参考数据编码不能为空", 64), trigger: "blur" },
      {
        validator: validatePattern(
            /^[A-Za-z0-9_]+$/,
          "参考数据编码支持英文字母、数字和下划线"
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    name: [
      { required: true, validator: validateTrimmedRequired("参考数据名称不能为空", 64), trigger: "blur" },
      {
        validator: validatePattern(
            /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "参考数据名称支持中文、英文字符、数字、下划线和短横线格"
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
  }
});

const { queryParams, form, rules } = toRefs(data);

const tableStore = reactive({
  config: {
    stripe: true,
    notPagination: false,
    notPaginationParams: true,
    sort: true,
    table: {
      rowKey: "id",
      stripe: true,
      defaultSort: { prop: "createTime", order: "descending" },
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    { type: "selection", width: 55, align: "center" },
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    {
      label: "参考数据名称",
      prop: "name",
      align: "left",
      minWidth: 200,
      info: {
        title: "name",
        click: proxy.$auth.hasPermi("model:dict:type:query")
                ? handleDetail
                : null,
      },
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "参考数据编码",
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
      label: "数据项数",
      prop: "dataCount",
      minWidth: 100,
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
  func: listType,
  params: queryParams
});

const searchStore = reactive({
  items: [
    {
      label: "参考数据编码",
      prop: "code",
      component: {
        is: "input",
      },
    },
    {
      label: "参考数据名称",
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

/** 刷新参考数据列表 */
function refreshList() {
  tableRef.value?.getList();
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    code: null,
    name: null,
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
  title.value = "新增参考数据";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value;
  getType(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改参考数据";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  proxy.$router.push({
    path: "/model/type/detail",
    query: { id: row.id },
  });
}

/** 提交按钮 */
function submitForm(submitData) {
  form.value = submitData;
  if (submitData.id != null) {
    updateType(submitData).then(() => {
      proxy.$modal.msgSuccess("修改成功");
      open.value = false;
      refreshList();
    }).catch(() => {
    });
  } else {
    addType(submitData).then(() => {
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
    await proxy.$modal.confirm(`确认要${text}“${row.name}”参考数据吗？`);
    await updateTypeStatus(row.id, enabled);
  } catch {
    row.validFlag = previousStatus;
    return;
  } finally {
    statusChanging.delete(row.id);
  }
  proxy.$modal.msgSuccess(text + "成功");
  refreshList();
}

/** 批量删除参考数据 */
function handleBatchDelete() {
  proxy.$modal.confirm('是否确认删除选中的参考数据？').then(() => delType(ids.value.join(','))).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    refreshList();
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除参考数据编号为"' + _ids + '"的数据项？').then(function () {
    return delType(_ids);
  }).then(() => {
    refreshList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('model/dict/type/export', {
    ...queryParams.value
  }, `type_${new Date().getTime()}.xlsx`);
}

/** 导入按钮操作 */
function handleImport() {
  upload.title = "参考数据导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {}, `type_template_${new Date().getTime()}.xlsx`);
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
  proxy.$alert("<div class='dict-type-import-result'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  refreshList();
};

</script>

<style scoped lang="scss">
.template-link {
  font-size: var(--qt-font-size-small);
  vertical-align: baseline;
}

:global(.dict-type-import-result) {
  max-height: var(--qt-import-result-max-height);
  padding: var(--qt-import-result-padding);
  overflow: auto;
  overflow-x: hidden;
}
</style>
