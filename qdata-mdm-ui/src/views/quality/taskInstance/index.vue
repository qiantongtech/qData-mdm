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
          v-hasPermi="['quality:taskInstance:add']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
        <el-button
          type="primary"
          plain
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['quality:taskInstance:edit']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xiugai--copy mr5"></i>修改
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['quality:taskInstance:remove']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
        </el-button>
        <el-button
          type="info"
          plain
          @click="handleImport"
          v-hasPermi="['quality:taskInstance:export']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-upload-cloud-line mr5"></i>导入
        </el-button>
        <el-button
          type="warning"
          plain
          @click="handleExport"
          v-hasPermi="['quality:taskInstance:export']"
          @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-download-line mr5"></i>导出
        </el-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #id="scope">{{ scope.row.id }}</template>
        <template #qualityTaskId="scope">{{ scope.row.qualityTaskId || '-' }}</template>
        <template #taskCode="scope">{{ scope.row.taskCode || '-' }}</template>
        <template #modelId="scope">{{ scope.row.modelId || '-' }}</template>
        <template #modelVersion="scope">{{ scope.row.modelVersion || '-' }}</template>
        <template #executionNo="scope">{{ scope.row.executionNo || '-' }}</template>
        <template #personCharge="scope">{{ scope.row.personCharge || '-' }}</template>
        <template #contactNumber="scope">{{ scope.row.contactNumber || '-' }}</template>
        <template #triggerType="scope">{{ scope.row.triggerType || '-' }}</template>
        <template #status="scope">{{ scope.row.status || '-' }}</template>
        <template #startTime="scope"><span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span></template>
        <template #endTime="scope"><span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d}') }}</span></template>
        <template #totalCount="scope">{{ scope.row.totalCount || '-' }}</template>
        <template #passCount="scope">{{ scope.row.passCount || '-' }}</template>
        <template #failCount="scope">{{ scope.row.failCount || '-' }}</template>
        <template #errorMessage="scope">{{ scope.row.errorMessage || '-' }}</template>
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
            v-hasPermi="['quality:taskInstance:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['quality:taskInstance:remove']"
          >删除</el-button>
          <el-button
            link
            type="primary"
            icon="view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['quality:taskInstance:edit']"
          >详情</el-button>
          <el-button
            link
            type="primary"
            icon="view"
            @click="routeTo('/tech/qiantong/qdata/mdm/quality/qualityTaskInstance/qualitytaskinstanceDetail',scope.row)"
            v-hasPermi="['quality:taskInstance:edit']"
          >复杂详情</el-button>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 添加或修改主数据质量任务执行实例对话框 -->
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
      <el-form ref="qualitytaskinstanceRef"
               :model="form"
               :rules="rules"
               label-width="80px"
               @submit.prevent
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="质量任务ID" prop="qualityTaskId">
              <el-input v-model="form.qualityTaskId" placeholder="请输入质量任务ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质量任务编码" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="请输入质量任务编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主数据模型ID" prop="modelId">
              <el-input v-model="form.modelId" placeholder="请输入主数据模型ID" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主数据模型版本" prop="modelVersion">
              <el-input v-model="form.modelVersion" placeholder="请输入主数据模型版本" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实例编号" prop="executionNo">
              <el-input v-model="form.executionNo" placeholder="请输入实例编号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人ID" prop="personCharge">
              <el-input v-model="form.personCharge" placeholder="请输入负责人ID" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人电话" prop="contactNumber">
              <el-input v-model="form.contactNumber" placeholder="请输入负责人电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="本次执行开始时间" prop="startTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.startTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择本次执行开始时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="本次执行结束时间" prop="endTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.endTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择本次执行结束时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查数据记录数" prop="totalCount">
              <el-input v-model="form.totalCount" placeholder="请输入检查数据记录数" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="通过数据记录数" prop="passCount">
              <el-input v-model="form.passCount" placeholder="请输入通过数据记录数" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题数据记录数" prop="failCount">
              <el-input v-model="form.failCount" placeholder="请输入问题数据记录数" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="执行失败说明" prop="errorMessage">
              <el-input v-model="form.errorMessage" placeholder="请输入执行失败说明" />
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

    <!-- 主数据质量任务执行实例详情对话框 -->
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
      <el-form ref="qualitytaskinstanceRef" :model="form"  label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="质量任务ID" prop="qualityTaskId">
              <div>
                {{ form.qualityTaskId }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质量任务编码" prop="taskCode">
              <div>
                {{ form.taskCode }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主数据模型ID" prop="modelId">
              <div>
                {{ form.modelId }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主数据模型版本" prop="modelVersion">
              <div>
                {{ form.modelVersion }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实例编号" prop="executionNo">
              <div>
                {{ form.executionNo }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人ID" prop="personCharge">
              <div>
                {{ form.personCharge }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人电话" prop="contactNumber">
              <div>
                {{ form.contactNumber }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="触发方式：1人工执行、2定时执行" prop="triggerType">
              <div>
                {{ form.triggerType }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="执行结果：1执行中、2失败、3部分成功、4成功" prop="status">
              <div>
                {{ form.status }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="本次执行开始时间" prop="startTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.startTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择本次执行开始时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="本次执行结束时间" prop="endTime">
              <el-date-picker clearable
                              class="full-width"
                              v-model="form.endTime"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="请选择本次执行结束时间"
              >
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查数据记录数" prop="totalCount">
              <div>
                {{ form.totalCount }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="通过数据记录数" prop="passCount">
              <div>
                {{ form.passCount }}
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题数据记录数" prop="failCount">
              <div>
                {{ form.failCount }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="执行失败说明" prop="errorMessage">
              <div>
                {{ form.errorMessage }}
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
              <el-checkbox v-model="upload.updateSupport" />是否更新已经存在的主数据质量任务执行实例数据
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

<script setup name="Qualitytaskinstance">
import { listQualitytaskinstance, getQualitytaskinstance, delQualitytaskinstance, addQualitytaskinstance, updateQualitytaskinstance } from "@/api/quality/taskInstance/taskInstance";
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
  url: import.meta.env.VITE_APP_BASE_API + "/tech/qiantong/qdata/mdm/quality/qualitytaskinstance/importData"
});

const data = reactive({
  form: {},
  queryParams: {
    qualityTaskId: null,
    taskCode: null,
    modelId: null,
    modelVersion: null,
    executionNo: null,
    personCharge: null,
    contactNumber: null,
    triggerType: null,
    status: null,
    startTime: null,
    endTime: null,
    totalCount: null,
    passCount: null,
    failCount: null,
    errorMessage: null,
    validFlag: null,
    createTime: null,
    updateTime: null,
  },
  rules: {
  }
});

const { queryParams, form, rules } = toRefs(data);

const tableRef = ref(null);
const listParams = ref({});
const searchItems = [
  {label: "质量任务ID", prop: "qualityTaskId", component: {is: "input"}},
  {label: "质量任务编码", prop: "taskCode", component: {is: "input"}},
  {label: "主数据模型ID", prop: "modelId", component: {is: "input"}},
  {label: "主数据模型版本", prop: "modelVersion", component: {is: "input"}},
  {label: "实例编号", prop: "executionNo", component: {is: "input"}},
  {label: "负责人ID", prop: "personCharge", component: {is: "input"}},
  {label: "负责人电话", prop: "contactNumber", component: {is: "input"}},
  {label: "本次执行开始时间", prop: "startTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}},
  {label: "本次执行结束时间", prop: "endTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}},
  {label: "检查数据记录数", prop: "totalCount", component: {is: "input"}},
  {label: "通过数据记录数", prop: "passCount", component: {is: "input"}},
  {label: "问题数据记录数", prop: "failCount", component: {is: "input"}},
  {label: "执行失败说明", prop: "errorMessage", component: {is: "input"}},
  {label: "是否有效；0：无效，1：有效", prop: "validFlag", component: {is: "input"}},
  {label: "创建时间", prop: "createTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}},
  {label: "更新时间", prop: "updateTime", component: {is: "date-picker", type: "date", valueFormat: "YYYY-MM-DD"}}
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
    {label: "质量任务ID", align: "center", prop: "qualityTaskId", slot: "qualityTaskId"},
    {label: "质量任务编码", align: "center", prop: "taskCode", slot: "taskCode"},
    {label: "主数据模型ID", align: "center", prop: "modelId", slot: "modelId"},
    {label: "主数据模型版本", align: "center", prop: "modelVersion", slot: "modelVersion"},
    {label: "实例编号", align: "center", prop: "executionNo", slot: "executionNo"},
    {label: "负责人ID", align: "center", prop: "personCharge", slot: "personCharge"},
    {label: "负责人电话", align: "center", prop: "contactNumber", slot: "contactNumber"},
    {label: "触发方式：1人工执行、2定时执行", align: "center", prop: "triggerType", slot: "triggerType"},
    {label: "执行结果：1执行中、2失败、3部分成功、4成功", align: "center", prop: "status", slot: "status"},
    {label: "本次执行开始时间", align: "center", prop: "startTime", width: 180, slot: "startTime"},
    {label: "本次执行结束时间", align: "center", prop: "endTime", width: 180, slot: "endTime"},
    {label: "检查数据记录数", align: "center", prop: "totalCount", slot: "totalCount"},
    {label: "通过数据记录数", align: "center", prop: "passCount", slot: "passCount"},
    {label: "问题数据记录数", align: "center", prop: "failCount", slot: "failCount"},
    {label: "执行失败说明", align: "center", prop: "errorMessage", slot: "errorMessage"},
    {label: "是否有效；0：无效，1：有效", align: "center", prop: "validFlag", slot: "validFlag"},
    {label: "创建人", align: "center", prop: "createBy", slot: "createBy"},
    {label: "创建时间", align: "center", prop: "createTime", width: 180, slot: "createTime"},
    {label: "更新人", align: "center", prop: "updateBy", slot: "updateBy"},
    {label: "更新时间", align: "center", prop: "updateTime", width: 180, slot: "updateTime"},
    {label: "备注", align: "center", prop: "remark", slot: "remark"},
    {label: "操作", align: "center", className: "small-padding fixed-width", fixed: "right", width: 240, slot: "action", prop: "action"}
  ],
  func: listQualitytaskinstance,
  params: queryParams.value,
  events: {
    formatParams(params) {
      listParams.value = { ...params };
      return params;
    },
  },
});

/** 查询主数据质量任务执行实例列表 */
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
    qualityTaskId: null,
    taskCode: null,
    modelId: null,
    modelVersion: null,
    executionNo: null,
    personCharge: null,
    contactNumber: null,
    triggerType: null,
    status: null,
    startTime: null,
    endTime: null,
    totalCount: null,
    passCount: null,
    failCount: null,
    errorMessage: null,
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
  proxy.resetForm("qualitytaskinstanceRef");
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
  title.value = "添加主数据质量任务执行实例";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const _id = row.id || ids.value
  getQualitytaskinstance(_id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改主数据质量任务执行实例";
  });
}

/** 详情按钮操作 */
function handleDetail(row) {
  reset();
  const _id = row.id || ids.value
  getQualitytaskinstance(_id).then(response => {
    form.value = response.data;
    openDetail.value = true;
    title.value = "主数据质量任务执行实例详情";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["qualitytaskinstanceRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateQualitytaskinstance(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        }).catch(error => {
        });
      } else {
        addQualitytaskinstance(form.value).then(response => {
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
  proxy.$modal.confirm('是否确认删除主数据质量任务执行实例编号为"' + _ids + '"的数据项？').then(function() {
    return delQualitytaskinstance(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

/** 导出按钮操作 */
function handleExport() {
  proxy.download('tech/qiantong/qdata/mdm/quality/qualitytaskinstance/export', {
    ...listParams.value,
    ...queryParams.value
  }, `qualitytaskinstance_${new Date().getTime()}.xlsx`)
}

/** ---------------- 导入相关操作 -----------------**/
/** 导入按钮操作 */
function handleImport() {
  upload.title = "主数据质量任务执行实例导入";
  upload.open = true;
}

/** 下载模板操作 */
function importTemplate() {
  proxy.download("system/user/importTemplate", {
  }, `qualitytaskinstance_template_${new Date().getTime()}.xlsx`)
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
  proxy.$alert("<div class='quality-task-instance-import-result'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
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

:global(.quality-task-instance-import-result) {
  @include patterns.import-result-scroll;
}
</style>
