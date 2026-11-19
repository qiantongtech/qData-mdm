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

  Business License: https://docs.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div class="app-container" ref="app-container">
    <qt-wrap :columns="tableStore.columns" :table-ref="tableRef">
      <template #search>
        <qt-search-bar
          :items="searchItems"
          :params="queryParams"
          :config="{ defaultExpand: true, form: { labelWidth: '75px' } }"
          @query="handleQuery"
          @reset="handleQuery"
        />
      </template>
      <template #actions-data>
        <el-button
          type="primary"
          plain
          @click="handleAdd"
          v-hasPermi="['quality:issueTaskLog:add']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
        <el-button
          type="primary"
          plain
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['quality:issueTaskLog:edit']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xiugai--copy mr5"></i>修改
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['quality:issueTaskLog:remove']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
        </el-button>
        <el-button
          type="info"
          plain
          @click="handleImport"
          v-hasPermi="['quality:issueTaskLog:export']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-upload-cloud-line mr5"></i>导入
        </el-button>
        <el-button
          type="warning"
          plain
          @click="handleExport"
          v-hasPermi="['quality:issueTaskLog:export']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-download-line mr5"></i>导出
        </el-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #id="scope">{{ scope.row.id }}</template>
        <template #qualityIssueTaskId="scope">{{ scope.row.qualityIssueTaskId || '-' }}</template>
        <template #issueId="scope">{{ scope.row.issueId || '-' }}</template>
        <template #taskSendId="scope">{{ scope.row.taskSendId || '-' }}</template>
        <template #taskHandlerId="scope">{{ scope.row.taskHandlerId || '-' }}</template>
        <template #sendTime="scope"><span>{{ parseTime(scope.row.sendTime, '{y}-{m}-{d}') }}</span></template>
        <template #handlerTime="scope"><span>{{ parseTime(scope.row.handlerTime, '{y}-{m}-{d}') }}</span></template>
        <template #description="scope">{{ scope.row.description || '-' }}</template>
        <template #validFlag="scope">{{ scope.row.validFlag || '-' }}</template>
        <template #createBy="scope">{{ scope.row.createBy || '-' }}</template>
        <template #createTime="scope"><span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span></template>
        <template #updateBy="scope">{{ scope.row.updateBy || '-' }}</template>
        <template #updateTime="scope"><span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span></template>
        <template #remark="scope">{{ scope.row.remark || '-' }}</template>
        <template #action="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['quality:issueTaskLog:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['quality:issueTaskLog:remove']"
          >删除</el-button>
          <el-button
            link
            type="primary"
            icon="view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['quality:issueTaskLog:edit']"
          >详情</el-button>
          <el-button
            link
            type="primary"
            icon="view"
            @click="routeTo('/tech/qiantong/qdata/mdm/quality/qualityIssueTaskLog/qualityIssueTaskLogDetail',scope.row)"
            v-hasPermi="['quality:issueTaskLog:edit']"
          >复杂详情</el-button>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 添加或修改问题整改记录日志对话框 -->
    <el-dialog :title="title"
               v-model="open"
               width="800px"
               :append-to="$refs['app-container']"
               draggable
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="qualityIssueTaskLogRef"
               :model="form"
               :rules="rules"
               label-width="80px"
               @submit.prevent
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="整改任务ID" prop="qualityIssueTaskId">
              <el-input v-model="form.qualityIssueTaskId" placeholder="请输入整改任务ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题数据ID" prop="issueId">
              <el-input v-model="form.issueId" placeholder="请输入问题数据ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务下发人ID" prop="taskSendId">
              <el-input v-model="form.taskSendId" placeholder="请输入任务下发人ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务处理人ID" prop="taskHandlerId">
              <el-input v-model="form.taskHandlerId" placeholder="请输入任务处理人ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="下发时间" prop="sendTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.sendTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择下发时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理时间" prop="handlerTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.handlerTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择处理时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="整改说明" prop="description">
              <el-input v-model="form.description" placeholder="请输入整改说明" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否有效；0：无效，1：有效" prop="validFlag">
              <el-input v-model="form.validFlag" placeholder="请输入是否有效；0：无效，1：有效" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 问题整改记录日志详情对话框 -->
    <el-dialog :title="title"
               v-model="openDetail"
               width="800px"
               :append-to="$refs['app-container']"
               draggable
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="qualityIssueTaskLogRef" :model="form"  label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="整改任务ID" prop="qualityIssueTaskId">
              <div>
                {{ form.qualityIssueTaskId }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题数据ID" prop="issueId">
              <div>
                {{ form.issueId }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务下发人ID" prop="taskSendId">
              <div>
                {{ form.taskSendId }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="任务处理人ID" prop="taskHandlerId">
              <div>
                {{ form.taskHandlerId }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="下发时间" prop="sendTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.sendTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择下发时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处理时间" prop="handlerTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.handlerTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择处理时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="整改说明" prop="description">
              <div>
                {{ form.description }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否有效；0：无效，1：有效" prop="validFlag">
              <div>
                {{ form.validFlag }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <div>
                {{ form.remark }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 用户导入对话框 -->
    <el-dialog :title="upload.title"
               v-model="upload.open"
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
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的问题整改记录日志数据
            </div>
            <span>仅允许导入xls、xlsx格式文件。</span>
            <el-link type="primary"
                     :underline="false"
                     class="template-link"
                     @click="importTemplate"
            >下载模板</el-link>
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

<script setup name="QualityIssueTaskLog">
import { listQualityIssueTaskLog, getQualityIssueTaskLog, delQualityIssueTaskLog, addQualityIssueTaskLog, updateQualityIssueTaskLog } from "@/api/quality/issueTaskLog/taskLog";
import {getToken} from "@/utils/auth.js";

const { proxy } = getCurrentInstance();

const open = ref(false);
const openDetail = ref(false);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const title = ref("");
const defaultSort = ref({ prop: "createTime", order: "desc" });
const router = useRouter();

/*** 用户导入参数 */
const upload = reactive({
  // 是否显示弹出层（用户导入）
  open: false,
  // 弹出层标题（用户导入）
  title: "",
  // 是否禁用上传
  isUploading: false,
  // 是否更新已经存在的用户数据
  updateSupport: 0,
  // 设置上传的请求头部
  headers: { Authorization: "Bearer " + getToken() },
  // 上传的地址
  url: import.meta.env.VITE_APP_BASE_API + "/tech/qiantong/qdata/mdm/quality/qualityIssueTaskLog/importData"
});

const data = reactive({
  form: {},
  queryParams: {
    qualityIssueTaskId: null,
    issueId: null,
    taskSendId: null,
    taskHandlerId: null,
    sendTime: null,
    handlerTime: null,
    description: null,
    validFlag: null,
    createTime: null,
  },
  rules: {
  }
});

const { queryParams, form, rules } = toRefs(data);

const tableRef = ref(null);
const listParams = ref({});
const searchItems = [
  {label: "整改任务ID", prop: "qualityIssueTaskId", component: {is: "input"}},
  {label: "问题数据ID", prop: "issueId", component: {is: "input"}},
  {label: "任务下发人ID", prop: "taskSendId", component: {is: "input"}},
  {label: "任务处理人ID", prop: "taskHandlerId", component: {is: "input"}},
  {label: "下发时间", prop: "sendTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}},
  {label: "处理时间", prop: "handlerTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}},
  {label: "整改说明", prop: "description", component: {is: "input"}},
  {label: "是否有效；0：无效，1：有效", prop: "validFlag", component: {is: "input"}},
  {label: "创建时间", prop: "createTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}}
];
const tableStore = reactive({
  config: {
    table: {
      stripe: true,
      height: '60vh',
      defaultSort: defaultSort.value,
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    {type: "selection", width: 55, align: "center"},
    {label: "主键ID", align: "center", prop: "id", slot: "id"},
    {label: "整改任务ID", align: "center", prop: "qualityIssueTaskId", slot: "qualityIssueTaskId"},
    {label: "问题数据ID", align: "center", prop: "issueId", slot: "issueId"},
    {label: "任务下发人ID", align: "center", prop: "taskSendId", slot: "taskSendId"},
    {label: "任务处理人ID", align: "center", prop: "taskHandlerId", slot: "taskHandlerId"},
    {label: "下发时间", align: "center", prop: "sendTime", width: 180, slot: "sendTime"},
    {label: "处理时间", align: "center", prop: "handlerTime", width: 180, slot: "handlerTime"},
    {label: "整改说明", align: "center", prop: "description", slot: "description"},
    {label: "是否有效；0：无效，1：有效", align: "center", prop: "validFlag", slot: "validFlag"},
    {label: "创建人", align: "center", prop: "createBy", slot: "createBy"},
    {label: "创建时间", align: "center", prop: "createTime", width: 180, slot: "createTime"},
    {label: "更新人", align: "center", prop: "updateBy", slot: "updateBy"},
    {label: "更新时间", align: "center", prop: "updateTime", width: 180, slot: "updateTime"},
    {label: "备注", align: "center", prop: "remark", slot: "remark"},
    {label: "操作", align: "center", className: "small-padding fixed-width", fixed: "right", width: 240, slot: "action", prop: "action"}
  ],
  func: listQualityIssueTaskLog,
  params: queryParams.value,
  events: {
    formatParams(params) {
      listParams.value = { ...params };
      return params;
    },
  },
});

/** 查询问题整改记录日志列表 */
function getList() {
  tableRef.value?.getList();
}

// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    qualityIssueTaskId: null,
    issueId: null,
    taskSendId: null,
    taskHandlerId: null,
    sendTime: null,
    handlerTime: null,
    description: null,
    validFlag: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null
  };
  proxy.resetForm("qualityIssueTaskLogRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加问题整改记录日志";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getQualityIssueTaskLog(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改问题整改记录日志";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value
  getQualityIssueTaskLog(_id).then(response => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "问题整改记录日志详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["qualityIssueTaskLogRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateQualityIssueTaskLog(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        }).catch(error => {
        });
      } else {
        addQualityIssueTaskLog(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        }).catch(error => {
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除问题整改记录日志编号为"' + _ids + '"的数据项？').then(function() {
    return delQualityIssueTaskLog(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('tech/qiantong/qdata/mdm/quality/qualityIssueTaskLog/export', {
    ...listParams.value,
    ...queryParams.value
  }, `qualityIssueTaskLog_${new Date().getTime()}.xlsx`)
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "问题整改记录日志导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {
  }, `qualityIssueTaskLog_template_${new Date().getTime()}.xlsx`)
}

/** 提交上传文件 */
function submitFileForm() {
  proxy.$refs["uploadRef"].submit();
};

/**文件上传中处理 */
const handleFileUploadProgress = (event, file, fileList) => {
  upload.isUploading = true;
};

/** 文件上传成功处理 */
const handleFileSuccess = (response, file, fileList) => {
  upload.open = false;
  upload.isUploading = false;
  proxy.$refs["uploadRef"].handleRemove(file);
  proxy.$alert("<div class='quality-issue-task-log-import-result'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
  getList();
};
/** ---------------------------------**/

function routeTo(link, row) {
  if (link !== "" && link.indexOf("http") !== -1) {
    window.location.href = link;
    return
  }
  if (link !== "") {
    if(link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      router.push({
        path: link,
        query: {
          id:row.id
        }
      });
    }
  }
}

</script>

<style scoped lang="scss">
@use '@/styles/common/component-patterns.scss' as patterns;

.full-width {
  @include patterns.full-width;
}

.template-link {
  @include patterns.template-link;
}

:global(.quality-issue-task-log-import-result) {
  @include patterns.import-result-scroll;
}
</style>
