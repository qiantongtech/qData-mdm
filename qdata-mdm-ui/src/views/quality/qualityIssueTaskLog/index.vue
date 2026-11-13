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
  <div class="app-container" ref="app-container">
    <div class="pagecont-top" v-show="showSearch">
     <el-form class="btn-style" :model="queryParams" ref="queryRef" :inline="true" label-width="75px" v-show="showSearch" @submit.prevent>
      <el-form-item label="整改任务ID" prop="qualityIssueTaskId">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.qualityIssueTaskId"
            placeholder="请输入整改任务ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="问题数据ID" prop="issueId">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.issueId"
            placeholder="请输入问题数据ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务下发人ID" prop="taskSendId">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.taskSendId"
            placeholder="请输入任务下发人ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务处理人ID" prop="taskHandlerId">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.taskHandlerId"
            placeholder="请输入任务处理人ID"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下发时间" prop="sendTime">
        <el-date-picker class="el-form-input-width"
            clearable
            v-model="queryParams.sendTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择下发时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="处理时间" prop="handlerTime">
        <el-date-picker class="el-form-input-width"
            clearable
            v-model="queryParams.handlerTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择处理时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="整改说明" prop="description">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.description"
            placeholder="请输入整改说明"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="是否有效；0：无效，1：有效" prop="validFlag">
        <el-input
            class="el-form-input-width"
            v-model="queryParams.validFlag"
            placeholder="请输入是否有效；0：无效，1：有效"
            clearable
            @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker class="el-form-input-width"
            clearable
            v-model="queryParams.createTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择创建时间">
        </el-date-picker>
      </el-form-item>

      <el-form-item>
        <el-button plain type="primary" @click="handleQuery" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </el-button>
        <el-button @click="resetQuery" @mousedown="(e) => e.preventDefault()">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </el-button>
      </el-form-item>
     </el-form>
    </div>

    <div  class="pagecont-bottom">
     <div class="justify-between mb15">
       <el-row :gutter="15" class="btn-style">
         <el-col :span="1.5">
           <el-button type="primary" plain @click="handleAdd" v-hasPermi="['quality:qualityissuetasklog:add']"
                      @mousedown="(e) => e.preventDefault()">
             <i class="iconfont-mini icon-xinzeng mr5"></i>新增
           </el-button>
         </el-col>
         <el-col :span="1.5">
           <el-button type="primary" plain :disabled="single" @click="handleUpdate" v-hasPermi="['quality:qualityissuetasklog:edit']"
                      @mousedown="(e) => e.preventDefault()">
             <i class="iconfont-mini icon-xiugai--copy mr5"></i>修改
           </el-button>
         </el-col>
         <el-col :span="1.5">
           <el-button type="danger" plain :disabled="multiple" @click="handleDelete" v-hasPermi="['quality:qualityissuetasklog:remove']"
                      @mousedown="(e) => e.preventDefault()">
             <i class="iconfont-mini icon-shanchu-huise mr5"></i>删除
           </el-button>
         </el-col>
         <el-col :span="1.5">
           <el-button type="info" plain  @click="handleImport" v-hasPermi="['quality:qualityissuetasklog:export']"
                      @mousedown="(e) => e.preventDefault()">
             <i class="iconfont-mini icon-upload-cloud-line mr5"></i>导入
           </el-button>
         </el-col>
         <el-col :span="1.5">
           <el-button type="warning" plain @click="handleExport" v-hasPermi="['quality:qualityissuetasklog:export']"
                      @mousedown="(e) => e.preventDefault()">
             <i class="iconfont-mini icon-download-line mr5"></i>导出
           </el-button>
         </el-col>
       </el-row>
       <div class="justify-end top-right-btn">
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
       </div>
     </div>
     <el-table stripe height="60vh" v-loading="loading" :data="qualityIssueTaskLogList" @selection-change="handleSelectionChange" :default-sort="defaultSort" @sort-change="handleSortChange">
       <el-table-column type="selection" width="55" align="center" />
       <el-table-column v-if="getColumnVisibility(0)" label="主键ID" align="center" prop="id" />
       <el-table-column v-if="getColumnVisibility(1)" label="整改任务ID" align="center" prop="qualityIssueTaskId">
         <template #default="scope">
           {{ scope.row.qualityIssueTaskId || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(2)" label="问题数据ID" align="center" prop="issueId">
         <template #default="scope">
           {{ scope.row.issueId || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(3)" label="任务下发人ID" align="center" prop="taskSendId">
         <template #default="scope">
           {{ scope.row.taskSendId || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(4)" label="任务处理人ID" align="center" prop="taskHandlerId">
         <template #default="scope">
           {{ scope.row.taskHandlerId || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(5)" label="下发时间" align="center" prop="sendTime" width="180">
         <template #default="scope">
           <span>{{ parseTime(scope.row.sendTime, '{y}-{m}-{d}') }}</span>
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(6)" label="处理时间" align="center" prop="handlerTime" width="180">
         <template #default="scope">
           <span>{{ parseTime(scope.row.handlerTime, '{y}-{m}-{d}') }}</span>
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(7)" label="整改说明" align="center" prop="description">
         <template #default="scope">
           {{ scope.row.description || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(8)" label="是否有效；0：无效，1：有效" align="center" prop="validFlag">
         <template #default="scope">
           {{ scope.row.validFlag || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(10)" label="创建人" align="center" prop="createBy">
         <template #default="scope">
           {{ scope.row.createBy || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(12)" label="创建时间" align="center" prop="createTime" width="180">
         <template #default="scope">
           <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d}') }}</span>
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(13)" label="更新人" align="center" prop="updateBy">
         <template #default="scope">
           {{ scope.row.updateBy || '-' }}
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(15)" label="更新时间" align="center" prop="updateTime" width="180">
         <template #default="scope">
           <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
         </template>
       </el-table-column>
       <el-table-column v-if="getColumnVisibility(16)" label="备注" align="center" prop="remark">
         <template #default="scope">
           {{ scope.row.remark || '-' }}
         </template>
       </el-table-column>
       <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="240">
         <template #default="scope">
           <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)"
                      v-hasPermi="['quality:qualityissuetasklog:edit']">修改</el-button>
           <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)"
                      v-hasPermi="['quality:qualityissuetasklog:remove']">删除</el-button>
           <el-button link type="primary" icon="view" @click="handleDetail(scope.row)"
                      v-hasPermi="['quality:qualityissuetasklog:edit']">详情</el-button>
           <el-button link type="primary" icon="view" @click="routeTo('/tech/qiantong/qdata/mdm/quality/qualityIssueTaskLog/qualityIssueTaskLogDetail',scope.row)"
                      v-hasPermi="['quality:qualityissuetasklog:edit']">复杂详情</el-button>
         </template>
       </el-table-column>

       <template #empty>
         <div class="emptyBg">
           <img src="@/assets/common/images/empty-state/noData.png" alt="" />
           <p>暂无记录</p>
         </div>
       </template>
     </el-table>

     <pagination
         v-show="total>0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
     />
    </div>

    <!-- 添加或修改问题整改记录日志对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" :append-to="$refs['app-container']" draggable>
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="qualityIssueTaskLogRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
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
                                style="width: 100%"
                                v-model="form.sendTime"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="请选择下发时间">
                </el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="处理时间" prop="handlerTime">
                <el-date-picker clearable
                                style="width: 100%"
                                v-model="form.handlerTime"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="请选择处理时间">
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
    <el-dialog :title="title" v-model="openDetail" width="800px" :append-to="$refs['app-container']" draggable>
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
                                style="width: 100%"
                                v-model="form.sendTime"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="请选择下发时间">
                </el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="处理时间" prop="handlerTime">
                <el-date-picker clearable
                                style="width: 100%"
                                v-model="form.handlerTime"
                                type="date"
                                value-format="YYYY-MM-DD"
                                placeholder="请选择处理时间">
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
    <el-dialog :title="upload.title" v-model="upload.open" width="800px"  :append-to="$refs['app-container']" draggable destroy-on-close>
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
            <el-link type="primary" :underline="false" style="font-size:12px;vertical-align: baseline;" @click="importTemplate">下载模板</el-link>
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
  import { listQualityIssueTaskLog, getQualityIssueTaskLog, delQualityIssueTaskLog, addQualityIssueTaskLog, updateQualityIssueTaskLog } from "@/api/quality/qualityIssueTaskLog/qualityIssueTaskLog";
  import {getToken} from "@/utils/auth.js";

  const { proxy } = getCurrentInstance();

  const qualityIssueTaskLogList = ref([]);

  // 列显隐信息
  const columns = ref([
            { key: 1, label: "整改任务ID", visible: true },
            { key: 2, label: "问题数据ID", visible: true },
            { key: 3, label: "任务下发人ID", visible: true },
            { key: 4, label: "任务处理人ID", visible: true },
            { key: 5, label: "下发时间", visible: true },
            { key: 6, label: "处理时间", visible: true },
            { key: 7, label: "整改说明", visible: true },
            { key: 8, label: "是否有效；0：无效，1：有效", visible: true },
            { key: 10, label: "创建人", visible: true },
            { key: 12, label: "创建时间", visible: true },
            { key: 13, label: "更新人", visible: true },
            { key: 15, label: "更新时间", visible: true },
            { key: 16, label: "备注", visible: true }
  ]);

  const getColumnVisibility = (key) => {
    const column = columns.value.find(col => col.key === key);
    // 如果没有找到对应列配置，默认显示
    if (!column) return true;
    // 如果找到对应列配置，根据visible属性来控制显示
    return column.visible;
  };

  const open = ref(false);
  const openDetail = ref(false);
  const loading = ref(true);
  const showSearch = ref(true);
  const ids = ref([]);
  const single = ref(true);
  const multiple = ref(true);
  const total = ref(0);
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
      pageNum: 1,
      pageSize: 10,
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

  /** 查询问题整改记录日志列表 */
  function getList() {
    loading.value = true;
    listQualityIssueTaskLog(queryParams.value).then(response => {
            qualityIssueTaskLogList.value = response.data.rows;
      total.value = response.data.total;
      loading.value = false;
    });
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
    queryParams.value.pageNum = 1;
    getList();
  }

  /** 重置按钮操作 */
  function resetQuery() {
    proxy.resetForm("queryRef");
    handleQuery();
  }

  // 多选框选中数据
  function handleSelectionChange(selection) {
    ids.value = selection.map(item => item.id);
    single.value = selection.length != 1;
    multiple.value = !selection.length;
  }


  /** 排序触发事件 */
  function handleSortChange(column, prop, order) {
    queryParams.value.orderByColumn = column.prop;
    queryParams.value.isAsc = column.order;
    getList();
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
    proxy.$alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + "</div>", "导入结果", { dangerouslyUseHTMLString: true });
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

  getList();
</script>
