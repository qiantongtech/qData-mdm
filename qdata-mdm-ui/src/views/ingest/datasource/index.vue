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
    <GuideTip tip-id="da/daDatasource.list"/>

    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
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
            type="primary"
            plain
            @click="handleAdd"
            v-hasPermi="['ingest:datasource:add']"
            @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
      </template>

      <qt-table v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" ref="tableRef">
        <template #creator="{ row }">
          <div class="flex-column">
            <span>{{ row.createBy || '-' }}</span>
            <span class="desc-text">{{ row.createPhoneNumber || '-' }}</span>
          </div>
        </template>
        <template #datasourceType="{ row }">
          <dict-tag
              v-if="row.datasourceType"
              :type="getDatasourceActive(row.datasourceType)?.type || 'primary'"
              :datasource-type="row.datasourceType"
          >{{ getDatasourceActive(row.datasourceType)?.label || datasourceTypeMap[row.datasourceType] || row.datasourceType }}</dict-tag>
        </template>
        <template #validFlag="{ row }">
          <el-switch
              v-model="row.validFlag"
              @change="handleStatusChange(row)"
              :loading="statusLoading[row.id]"
          />
        </template>
        <template #action="{ row }">
          <el-button
              link
              type="primary"
              icon="Connection"
              :loading="testConnectionLoading[row.id]"
              @click="handleTestConnection(row)"
              v-hasPermi="['ingest:datasource:edit']"
          >测试连接
          </el-button
          >
          <el-button
              link
              type="primary"
              icon="view"
              @click="handleDetail(row)"
              v-hasPermi="['ingest:datasource:edit']"
          >详情
          </el-button
          >
          <el-popover placement="bottom" :width="100" trigger="click">
            <template #reference>
              <el-button
                  link
                  type="primary"
                  icon="ArrowDown"
              >
                更多
              </el-button>
            </template>
            <div class="butgdlist">
              <el-button
                  link
                  type="primary"
                  icon="Edit"
                  @click="handleUpdate(row)"
                  v-hasPermi="['ingest:datasource:edit']"
              >修改连接
              </el-button
              >

              <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(row)"
                  v-hasPermi="['ingest:datasource:remove']"
              >删除连接
              </el-button
              >
            </div>
          </el-popover>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 新增或修改数据连接对话框 -->
    <el-dialog
        :title="title"
        v-model="open"
        width="1200px"
        :append-to="$refs['app-container']"
        draggable
        class="datasource-wizard-dialog"
    >
      <template #header="{ close, titleId, titleClass }">
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>

      <Steps
          v-if="title === '新增数据连接' || title === '修改数据连接'"
          :steps="stepsList"
          :active-step="currentStep"
          :is-dialog="true"
          @change="handleStepChange"
      />

      <!-- 新增模式：第一步：选择数据连接类型 -->
      <div v-show="currentStep === 0 && title === '新增数据连接'">
        <DatasourceTypeSelector v-model="form.datasourceType"/>
      </div>

      <!-- 新增模式：第二步/修改模式：第一步：填写/修改连接信息 -->
      <DatasourceForm
          v-if="
          (currentStep === 1 && title === '新增数据连接') ||
          (currentStep === 0 && title === '修改数据连接')
        "
          ref="datasourceFormRef"
          v-model="form"
          :datasource-type="form.datasourceType"
          :mode="
          title === '数据连接详情'
            ? 'detail'
            : title === '修改数据连接'
            ? 'edit'
            : 'add'
        "
          :show-datasource-type="true"
          :dict-options="{ sys_disable: toValue(dicts.sys_disable) }"
          @field-change="handleFieldChange"
      />

      <!-- 详情模式 -->
      <DatasourceForm
          v-if="title === '数据连接详情' && open"
          ref="datasourceDetailFormRef"
          v-model="form"
          :datasource-type="form.datasourceType"
          mode="detail"
          :show-datasource-type="true"
          :dict-options="{ sys_disable: toValue(dicts.sys_disable) }"
      />

      <!-- 第三步：确认信息（新增模式）/第二步：确认信息（修改模式） -->
      <div
          v-show="
          (currentStep === 2 && title === '新增数据连接') ||
          (currentStep === 1 && title === '修改数据连接')
        "
          class="step-three-content"
      >

        <DatasourceConfirmStep
            ref="confirmStepRef"
            :form="form"
            @loading-change="(val) => (testLoading = val)"
            @diagnosis-change="onDiagnosisChange"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <!-- 新增模式第一步：选择数据连接类型 -->
          <template v-if="title === '新增数据连接' && currentStep === 0">
            <div class="footer-right">
              <el-button @click="cancel">取消</el-button>
              <el-button type="primary" @click="handleNextStep"
              >下一步
              </el-button
              >
            </div>
          </template>

          <!-- 新增模式第二步 / 修改模式第一步：填写/修改连接信息 -->
          <template
              v-if="
              (title === '新增数据连接' && currentStep === 1) ||
              (title === '修改数据连接' && currentStep === 0)
            "
          >
            <div class="footer-right">
              <el-button @click="handlePrevStep">上一步</el-button>
              <el-button @click="cancel">取消</el-button>
              <el-button type="primary" @click="handleNextStep"
              >下一步
              </el-button
              >
            </div>
          </template>

          <!-- 新增模式第三步 / 修改模式第二步：测试连接并保存 -->
          <template
              v-if="
              (title === '新增数据连接' && currentStep === 2) ||
              (title === '修改数据连接' && currentStep === 1)
            "
          >
            <div class="footer-right">
              <el-button @click="handlePrevStep">上一步</el-button>
              <el-button
                  type="primary"
                  :loading="testLoading"
                  @click="submitForm(true)"
              >
                测试连接
              </el-button>
              <el-button
                  type="primary"
                  :loading="btnLoading"
                  @click="submitForm"
              >
                保存
              </el-button>
            </div>
          </template>

          <!-- 详情模式 -->
          <template v-if="title === '数据连接详情'">
            <div class="footer-right">
              <el-button @click="cancel">关闭</el-button>
            </div>
          </template>
        </div>
      </template>
    </el-dialog>

    <!-- Project Dialog -->
    <el-dialog
        title="选择项目"
        v-model="openProject"
        width="900px"
        append-to-body
    >
      <el-form
          :model="queryParamsProject"
          ref="queryRefProject"
          :inline="true"
          v-show="true"
          label-width="68px"
          @submit.prevent
      >
        <el-form-item label="项目名称" prop="name">
          <el-input
              v-model="queryParamsProject.name"
              placeholder="请输入项目名称"
              clearable
              @keyup.enter="handleQueryProject"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQueryProject"
          >搜索
          </el-button
          >
          <el-button icon="Refresh" @click="resetQueryProject">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table
          v-loading="loadingProject"
          :data="projectList"
          @selection-change="handleSelectionChangeProject"
          ref="projectTableRef"
      >
        <el-table-column
            type="selection"
            width="55"
            align="center"
            :selectable="selectable"
        />
        <el-table-column label="项目名称" align="center" prop="name"/>
        <el-table-column label="项目编码" align="center" prop="code"/>
        <el-table-column label="负责人" align="center" prop="managerName"/>
        <template #empty>
          <div class="emptyBg">
            <img :src="emptyDataImage" alt=""/>
            <p>暂无记录</p>
          </div>
        </template>
      </el-table>
      <pagination
          v-show="totalProject > 0"
          :total="totalProject"
          v-model:page="queryParamsProject.pageNum"
          v-model:limit="queryParamsProject.pageSize"
          @pagination="getListProject"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFormProject">确 定</el-button>
          <el-button @click="openProject = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 列表测试连接结果弹窗 -->
    <el-dialog
        title="测试连接结果"
        v-model="openTestResultDialog"
        width="1200px"
        class="datasource-wizard-dialog datasource-test-result-dialog"
        :append-to="$refs['app-container']"

    >
      <DatasourceConfirmStep
          ref="listConfirmStepRef"
          :form="listTestForm"
          @loading-change="(val) => (listTestLoading = val)"
      />
      <template #footer>
        <div class="dialog-footer">
          <div class="footer-right">
            <el-button plain @click="closeTestResultDialog"
            >关闭
            </el-button
            >
            <el-button
                type="primary"
                :loading="listTestLoading"
                @click="retryListTestConnection"
            >
              测试连接
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DaDatasource">
import { tableTitleLink, titleCellClass } from '@/utils/anivia.js'
import emptyDataImage from '@/assets/common/images/empty-state/empty-nodata.png';
import {
  reactive,
  ref,
  getCurrentInstance,
  toValue,
  computed,
  watch,
  nextTick,
} from "vue";
import {ElMessageBox} from "element-plus";
import {
  listDaDatasource,
  getDaDatasource,
  removeDppOrDa,
  addDaDatasource,
  listDaDatasourceByProjectCode,
  updateDaDatasource,
  editDatasourceStatus,
  noDppAdd,
} from "@/api/ingest/datasource/dataSource";
import {encrypt, isDecrypted} from "@/utils/aesEncrypt";
import {deptUserTree} from "@/api/system/system/user.js";
import {getToken} from "@/utils/auth.js";
import useUserStore from "@/store/system/user";
import DatasourceTypes from "@/components/Datasource/Types.vue";
import DatasourceTag from "@/components/Datasource/Tag.vue";
import {getDatasourceActive, getDatasourceTypes} from "@/components/Datasource/utils";
import Steps from "@/components/Steps/index.vue";
import DatasourceConfirmStep from "./datasourceConfirmStep.vue";
import DatasourceTypeSelector from "./datasourceTypeSelector.vue";
import DatasourceForm from "./datasourceForm.vue";

import {useRoute, useRouter} from "vue-router";

const DatasourceFlag = "datasource";

const datasourceTypeMap = {
  MYSQL: "MySQL",
  ORACLE: "Oracle",
  POSTGRESQL: "PostgreSQL",
  SQLSERVER: "SQL Server",
  GBase8c: "GBase 8c",
  HighGo: "瀚高数据库",
  UNKNOWN: "未知类型",
};

const stepsListAdd = [
  {title: "选择数据连接类型"},
  {title: "填写连接信息"},
  {title: "测试连接并保存"},
];

const stepsListEdit = [{title: "修改连接信息"}, {title: "测试连接并保存"}];

// 计算属性：根据当前模式返回对应的步骤列表
const stepsList = computed(() => {
  if (title.value === "新增数据连接") {
    return stepsListAdd;
  } else if (title.value === "修改数据连接") {
    return stepsListEdit;
  }
  return [];
});
const currentStep = ref(0);

const {proxy} = getCurrentInstance();

const router = useRouter();

const dicts = proxy.useDict("sys_disable");
const userStore = useUserStore();

const open = ref(false);
const openProject = ref(false);
const openDetail = ref(false);
const openTestResultDialog = ref(false);
const ids = ref([]);
const title = ref("");
const projectOptions = ref([]);
const projectList = ref([]);
const totalProject = ref(0);
const projectTableRef = ref(null);
const loadingProject = ref(false);
const projectIdAndCodeList = ref([]);
const route = useRoute();
let type = route.query.type || null;
const btnLoading = ref(false);
const testLoading = ref(false);
const listTestLoading = ref(false);
const testConnectionLoading = ref({});
const editLoading = reactive({});
const statusLoading = reactive({});
// 保存 DatasourceConfirmStep 组件传出的诊断结果
const currentDiagnosis = ref(null);
const listTestDiagnosis = ref(null);
const listTestForm = reactive({
  id: null,
  datasourceName: null,
  datasourceType: null,
  ip: null,
  port: null,
  username: null,
});
const datasourceFormRef = ref(null);
const datasourceDetailFormRef = ref(null);
// DatasourceConfirmStep 组件引用
const confirmStepRef = ref(null);
const listConfirmStepRef = ref(null);

const data = reactive({
  form: {
    projectNameListStr: "-",
    projectNameList: [],
    projectIdList: [],
    projectList: [],
  },
  queryParamsProject: {
    pageNum: 1,
    pageSize: 10,
    name: null,
    managerId: null,
    datasourceId: null,
  },
  rules: {
    datasourceName: [
      {required: true, message: "数据连接名称不能为空", trigger: "blur"},
    ],
    datasourceConfig: [
      {
        required: true,
        message: "数据连接配置(json字符串)不能为空",
        trigger: "blur",
      },
    ],
  },
});

const {form, rules, queryParamsProject} = toRefs(data);
const selectable = (row) => !row.dppAssigned;

// QtTable logic
const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    table: {
      cellClassName: titleCellClass,
      stripe: true,
      rowKey: "id",
      defaultSort: {prop: "create_time", order: "descending"},
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    {label: "编号", prop: "id", minWidth: 60, sortable: true},
    {
      label: "数据连接名称",
      prop: "datasourceName",
      className: 'title-column',
      get link() { return tableTitleLink(handleDetail, 'ingest:datasource:edit'); },
      align: "left",
      minWidth: 240,
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      minWidth: 240,
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "数据连接类型",
      prop: "datasourceType",
      slot: "datasourceType",
      minWidth: 180,
    },
    {
      label: "状态",
      prop: "validFlag",
      slot: "validFlag",
    },
    {
      label: "创建人",
      prop: "createBy",
      slot: "creator",
      align: "left",
      width: 160,
      minWidth: 160,
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
      minWidth: 160,
    },

    {label: "操作", minWidth: 280, fixed: "right", slot: "action"},
  ],
  func: getListWrapper,
  params: {
    datasourceName: null,
    connectionType: null,
    ip: null,
  },
});

const searchStore = reactive({
  items: [
    {
      label: "数据连接名称",
      prop: "datasourceName",
      align: "left",
      component: {
        is: "input",
      },
    },
    {
      label: "数据连接类型",
      prop: "connectionType",
      type: "select",
      component: {is: "select", options: getDatasourceTypes(DatasourceFlag)},
    },
    {
      label: "IP",
      prop: "ip",
      component: {
        is: "input",
      },
    },
  ],
});

function getListWrapper(params) {
  const {connectionType, ...queryParams} = params;
  queryParams.datasourceType = connectionType;
  if (type == 1) {
    queryParams.projectId = userStore.projectId;
    queryParams.projectCode = userStore.projectCode;
    return listDaDatasourceByProjectCode(queryParams);
  } else {
    return listDaDatasource(queryParams);
  }
}

function handleQuery() {
  tableRef.value && tableRef.value.getList();
}

// 监听 id 变化
watch(
    () => userStore.projectCode,
    (newCode) => {
      handleQuery();
    },
    {immediate: true}
);

function getProjectOptions() {
  deptUserTree().then((response) => {
    projectOptions.value = response.data;
  });
}

// 处理字段变化
function handleFieldChange({field}) {
  // 处理 SSL 开关变化
  if (field.prop === "useSSL") {
    handleSSLChange();
  }
}

//数据连接类型change事件
function handleDatasourceChange(type) {
  form.value.useSSL = 0;
  form.value.sslConfig = {};
  if (isOceanBaseDatasource(type)) {
    form.value.port = form.value.port || "2881";
  }
}

function handleSSLChange() {
  form.value.sslConfig = {};
}

function getListProject() {
  openProject.value = true;
  loadingProject.value = true;
  noDppAdd(queryParamsProject.value).then((response) => {
    projectList.value = response.data.rows;
    totalProject.value = response.data.total;
    loadingProject.value = false;

    // 在表格加载完成后，设置之前选中的项目
    nextTick(() => {
      projectList.value.forEach((project) => {
        form.value.projectList.forEach((item) => {
          if (item.projectId === project.id) {
            proxy.$refs.projectTableRef.toggleRowSelection(project, true);
          }
        });
      });
    });
  });
}

function handleSelectionChangeProject(selection) {
  projectIdAndCodeList.value = [];
  for (let i = 0; i < selection.length; i++) {
    const element = selection[i];
    let project = {
      projectId: element.id,
      projectCode: element.code,
    };
    projectIdAndCodeList.value.push(project);
  }

  form.value.projectNameList = selection.map((item) => item.name);
}

function submitFormProject() {
  openProject.value = false;
  form.value.projectList = projectIdAndCodeList.value;
}

function handleQueryProject() {
  queryParamsProject.value.pageNum = 1;
  getListProject();
}

function resetQuery() {
  handleQuery();
}

function resetQueryProject() {
  queryParamsProject.value.name = null;
  handleQueryProject();
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
    projectNameList: [],
    projectIdList: [],
    projectList: [],
    datasourceName: null,
    datasourceType: null,
    datasourceConfig: null,
    ip: null,
    port: null,
    listCount: null,
    syncCount: null,
    dataSize: null,
    description: null,
    validFlag: false,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
    allowOperate: 0,
    useSSL: 0,
    sslConfig: {},

  };
  currentDiagnosis.value = null;
  proxy.resetForm("daDatasourceRef");
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  currentStep.value = 0;
  if (type == 1) {
    form.value.isDaOrDpp = true;
    form.value.projectList = [
      {
        projectId: userStore.projectId,
        projectCode: userStore.projectCode,
        dppAssigned: true,
      },
    ];
  } else {
    form.value.isDaOrDpp = false;
    form.value.projectList = [];
  }
  open.value = true;
  title.value = "新增数据连接";
}

function handleNextStep() {
  return new Promise((resolve) => {
    console.log(
        "handleNextStep called, currentStep:",
        currentStep.value,
        "title:",
        title.value
    );

    // 第一步（新增模式）：选择数据连接类型
    if (title.value === "新增数据连接" && currentStep.value === 0) {
      if (!form.value.datasourceType) {
        proxy.$modal.msgWarning("请选择数据连接类型");
        resolve(false);
        return;
      }
      handleDatasourceChange(form.value.datasourceType);
      currentStep.value++;
      // 进入第二步前，让表单组件同步数据
      nextTick(() => {
        if (datasourceFormRef.value) {
          datasourceFormRef.value.syncFromProps();
        }
        resolve(true);
      });
      return;
    }

    // 第二步（新增模式）或第一步（修改模式）：需要验证表单
    if (
        (title.value === "新增数据连接" && currentStep.value === 1) ||
        (title.value === "修改数据连接" && currentStep.value === 0)
    ) {
      console.log("Trying to validate form...");
      if (datasourceFormRef.value) {
        datasourceFormRef.value
            .validate()
            .then((valid) => {
              console.log("Form validation result:", valid);
              if (valid) {
                // 验证通过后，同步表单数据到父组件
                datasourceFormRef.value.syncToParent();
                currentStep.value++;
                // 进入第三步后调用 DatasourceConfirmStep 的 diagnose 方法
                nextTick(() => {
                  const testForm = buildTestData();
                  confirmStepRef.value?.diagnose({connection: testForm});
                  resolve(true);
                });
              } else {
                proxy.$modal.msgWarning("请填写完整的表单信息");
                resolve(false);
              }
            })
            .catch(() => {
              proxy.$modal.msgWarning("请填写完整的表单信息");
              resolve(false);
            });
      } else {
        currentStep.value++;
        // 进入第三步后调用 DatasourceConfirmStep 的 diagnose 方法
        nextTick(() => {
          const testForm = buildTestData();
          confirmStepRef.value?.diagnose({connection: testForm});
          resolve(true);
        });
      }
      return;
    }

    resolve(false);
  });
}

function handlePrevStep() {
  return new Promise((resolve) => {
    if (currentStep.value > 0) {
      // 如果是新增模式且从第二步回到第一步
      if (title.value === "新增数据连接" && currentStep.value === 1) {
        // 先同步表单数据，确保 datasourceName 等字段保存到父组件
        if (datasourceFormRef.value) {
          datasourceFormRef.value.syncToParent();
        }
        // 保留固定字段：datasourceName 和 datasourceType
        const keepDatasourceName = form.value.datasourceName;
        const keepDatasourceType = form.value.datasourceType;
        // 保留项目相关字段
        const keepProjectList = form.value.projectList;
        const keepProjectNameList = form.value.projectNameList;
        const keepProjectIdList = form.value.projectIdList;
        const keepIsDaOrDpp = form.value.isDaOrDpp;

        // 重置为初始状态
        reset();

        // 恢复固定字段
        form.value.datasourceName = keepDatasourceName;
        form.value.datasourceType = keepDatasourceType;
        form.value.projectList = keepProjectList;
        form.value.projectNameList = keepProjectNameList;
        form.value.projectIdList = keepProjectIdList;
        form.value.isDaOrDpp = keepIsDaOrDpp;
      }
      currentStep.value--;
      resolve(true);
    } else {
      resolve(false);
    }
  });
}

async function handleStepChange(targetIndex) {
  if (targetIndex === currentStep.value) return;

  if (targetIndex > currentStep.value) {
    // If user clicks a step ahead, we need to go next step by step
    while (currentStep.value < targetIndex) {
      const success = await handleNextStep();
      if (!success) break; // Stop if validation fails
    }
  } else {
    // If user clicks a step behind, we go prev step by step
    while (currentStep.value > targetIndex) {
      const success = await handlePrevStep();
      if (!success) break;
    }
  }
}

// 接收 DatasourceConfirmStep 传出的诊断结果
function onDiagnosisChange(result) {
  currentDiagnosis.value = result;
}

function isOceanBaseDatasource(type) {
  const datasourceType = String(type || "").toUpperCase();
  return datasourceType === "OCEANBASE" || datasourceType === "OCEANBASE_ORACLE";
}

// 构建测试连接数据
function buildTestData() {
  const testForm = {...form.value};

  // 处理密码加密逻辑
  if (testForm.id != null) {
    // 修改时检查密码是否需要重新加密
    if (old_password !== testForm.password || !isDecrypted(testForm.password)) {
      testForm.password = encrypt(testForm.password);
    }
  } else {
    // 新增时直接加密密码
    testForm.password = encrypt(testForm.password);
  }

  // 构造datasourceConfig
  const saveConfigTest = (() => {
    let obj = {};
    const c = testForm.config;
    if (typeof c === "string" && c.trim() !== "") {
      try {
        obj = JSON.parse(c) || {};
      } catch (e) {
        obj = {};
      }
    } else if (typeof c === "object" && c) {
      obj = c;
    }

    return JSON.stringify(obj);
  })();

  testForm.datasourceConfig = JSON.stringify({
    username: testForm.username,
    password: testForm.password,
    dbname: testForm.dbname,
    sid: testForm.sid,

    config: saveConfigTest,
    allowOperate: testForm.allowOperate,
    useSSL: testForm.useSSL,
    sslConfig: testForm.useSSL === 1 ? testForm.sslConfig : {},

  });

  delete testForm.sslConfig;
  delete testForm.useSSL;

  return testForm;
}

/** 修改按钮操作 */
let old_password;

function handleUpdate(row, type) {
  editLoading[row.id] = true;
  reset();
  const _id = row.id || ids.value;
  getDaDatasource(_id)
      .then((response) => {
        form.value = response.data;
        form.value.projectList = form.value.projectList || [];
        form.value.projectIdList = form.value.projectList.map(
            (item) => item.projectId
        );
        form.value.projectNameList = form.value.projectList.map(
            (item) => item.projectName
        );

        // 拆解 datasourceConfig
        if (form.value.datasourceConfig) {
          const config = JSON.parse(form.value.datasourceConfig);
          console.log("🚀 ~ handleUpdate ~ config:", config);

          form.value.username = config.username;
          form.value.password = config.password;
          form.value.dbname = config.dbname;
          form.value.sid = config.sid;
          if (config.config) {
            form.value.config = config.config;
            if (typeof config.config === "string") {
              try {
                const inner = JSON.parse(config.config);
                if (inner && inner.protocol) {
                  form.value.protocol = inner.protocol;
                }
              } catch (e) {
              }
            } else if (typeof config.config === "object" && config.config) {
              if (config.config.protocol) {
                form.value.protocol = config.config.protocol;
              }
            }
          }
          form.value.allowOperate =
              config.allowOperate !== undefined ? config.allowOperate : 0;

          // 从 datasourceConfig 中读取 useSSL 和 sslConfig
          form.value.useSSL = config.useSSL !== undefined ? config.useSSL : 0;
          if (config.sslConfig) {
            if (typeof config.sslConfig === "string") {
              try {
                form.value.sslConfig = JSON.parse(config.sslConfig);
              } catch (e) {
                form.value.sslConfig = {};
              }
            } else {
              form.value.sslConfig = config.sslConfig;
            }
          } else {
            form.value.sslConfig = {};
          }
        }

        form.value.projectListOld = form.value.projectIdList;
        queryParamsProject.value.datasourceId = form.value.id;
        open.value = true;
        if (type == 3) {
          title.value = "数据连接详情";
          // 详情模式下，详情表单组件同步数据
          nextTick(() => {
            if (datasourceDetailFormRef.value) {
              datasourceDetailFormRef.value.syncFromProps();
            }
          });
        } else {
          old_password = form.value.password;
          title.value = "修改数据连接";
          currentStep.value = 0; // 初始化修改模式的当前步骤为第一步
          // 编辑模式下，编辑表单组件同步数据
          nextTick(() => {
            if (datasourceFormRef.value) {
              datasourceFormRef.value.syncFromProps();
            }
          });
        }
      })
      .finally(() => {
        editLoading[row.id] = false;
      });
}

/** 详情按钮操作 */
function handleDetail(row) {
  handleUpdate(row, 3);
}

/** 文件管理操作 */

/** 详情按钮操作 */
function handleTestConnection(row) {
  if (testConnectionLoading.value[row.id]) {
    return;
  }
  const _id = row.id || ids.value;
  testConnectionLoading.value[row.id] = true;
  // 先获取数据源详情，用于弹窗展示（概览信息）
  getDaDatasource(_id)
      .then((detailResp) => {
        const detailData = detailResp.data;
        // 填充 listTestForm 基础字段，仅用于 DatasourceConfirmStep 展示
        listTestForm.id = detailData.id;
        listTestForm.datasourceName = detailData.datasourceName;
        listTestForm.datasourceType = detailData.datasourceType;
        listTestForm.ip = detailData.ip;
        listTestForm.port = detailData.port;

        // 拆解 datasourceConfig，提取用户名用于展示
        if (detailData.datasourceConfig) {
          try {
            const config = JSON.parse(detailData.datasourceConfig);
            listTestForm.username =
                config.username ||
                config.accessId ||
                config.access_id ||
                config.accessKeyId ||
                detailData.username;
          } catch (e) {
            console.error("解析 datasourceConfig 失败", e);
          }
        }

        // 执行实际测试连接（仅传 ID，让后端使用数据库中已保存的配置进行测试）
        runListTestConnection(_id);
      })
      .catch(() => {
        testConnectionLoading.value[row.id] = false;
      });
}

function runListTestConnection(id) {
  // 先打开对话框，确保子组件已挂载
  if (!openTestResultDialog.value) {
    openTestResultDialog.value = true;
  }
  // 对话框打开后，调用 DatasourceConfirmStep 的 diagnose 方法
  nextTick(() => {
    listConfirmStepRef.value?.diagnose({datasourceId: id}).finally(() => {
      if (testConnectionLoading.value[id]) {
        testConnectionLoading.value[id] = false;
      }
    });
  });
}

function closeTestResultDialog() {
  openTestResultDialog.value = false;
}

function retryListTestConnection() {
  if (!listTestForm.id) return;
  nextTick(() => {
    listConfirmStepRef.value?.diagnose({datasourceId: listTestForm.id});
  });
}

/** 提交按钮 */
function submitForm(isTestConnection) {
  // 如果是测试连接
  if (isTestConnection === true) {
    // 测试连接前先验证表单
    if (datasourceFormRef.value) {
      datasourceFormRef.value
          .validate()
          .then((valid) => {
            if (valid) {
              // 验证通过后，同步表单数据到父组件
              datasourceFormRef.value.syncToParent();
              // 调用 DatasourceConfirmStep 的 diagnose 方法
              nextTick(() => {
                const testForm = buildTestData();
                confirmStepRef.value?.diagnose({connection: testForm});
              });
            } else {
              proxy.$modal.msgWarning("请填写完整的表单信息");
            }
          })
          .catch(() => {
            proxy.$modal.msgWarning("请填写完整的表单信息");
          });
    } else {
      // 调用 DatasourceConfirmStep 的 diagnose 方法
      nextTick(() => {
        const testForm = buildTestData();
        confirmStepRef.value?.diagnose({connection: testForm});
      });
    }
    return;
  }

  // 保存前先验证表单
  if (datasourceFormRef.value) {
    datasourceFormRef.value
        .validate()
        .then((valid) => {
          if (valid) {
            // 验证通过后，同步表单数据到父组件
            datasourceFormRef.value.syncToParent();
            doSubmit();
          } else {
            proxy.$modal.msgWarning("请填写完整的表单信息");
          }
        })
        .catch(() => {
          proxy.$modal.msgWarning("请填写完整的表单信息");
        });
  } else {
    doSubmit();
  }
}

function doSubmit() {
  // 原有的新增/修改逻辑
  btnLoading.value = true;
  const submitData = {...form.value};

  const proceedSubmit = () => {
    const handleSavePromise = (promise, data, isUpdate) => {
      promise
          .then((response) => {
            proxy.$modal.msgSuccess(isUpdate ? "修改成功" : "新增成功");
            open.value = false;
            handleQuery();
            btnLoading.value = false;
          })
          .catch((err) => {
            if (err.code == 10001 && data.validFlag === true) {
              ElMessageBox.confirm(
                  "连接测试未通过，暂无法启用。请检查连接配置后重试；如继续保存，连接状态将自动调整为“禁用”。",
                  "提示",
                  {
                    confirmButtonText: "保存",
                    cancelButtonText: "取消",
                    type: "warning",
                  }
              )
                  .then(() => {
                    data.validFlag = false;
                    const api = isUpdate ? updateDaDatasource : addDaDatasource;
                    handleSavePromise(api(data), data, isUpdate);
                  })
                  .catch(() => {
                    btnLoading.value = false;
                  });
            } else {
              btnLoading.value = false;
            }
          });
    };

    if (submitData.id != null) {

      if (
          old_password !== submitData.password ||
          !isDecrypted(submitData.password)
      ) {
        submitData.password = encrypt(submitData.password);
      }
      const saveConfigUpdate = (() => {
        let obj = {};
        const c = submitData.config;
        if (typeof c === "string" && c.trim() !== "") {
          try {
            obj = JSON.parse(c) || {};
          } catch (e) {
            obj = {};
          }
        } else if (typeof c === "object" && c) {
          obj = c;
        }

        return JSON.stringify(obj);
      })();

      submitData.datasourceConfig = JSON.stringify({
        username: submitData.username,
        password: submitData.password,

        dbname: submitData.dbname,
        sid: submitData.sid,

        config: saveConfigUpdate,
        allowOperate: submitData.allowOperate,
        useSSL: submitData.useSSL,
        sslConfig: submitData.useSSL === 1 ? submitData.sslConfig : {},

      });

      // 删除外层的 sslConfig 和 useSSL，避免重复传递
      delete submitData.sslConfig;
      delete submitData.useSSL;

      let projectListOld = [];
      submitData.projectListOld.forEach((item) => {
        if (!submitData.projectList.includes(item)) {
          projectListOld.push(item);
        }
      });
      submitData.projectListOld = projectListOld;
      handleSavePromise(updateDaDatasource(submitData), submitData, true);
    } else {

      const saveConfigAdd = (() => {
        let obj = {};
        const c = submitData.config;
        if (typeof c === "string" && c.trim() !== "") {
          try {
            obj = JSON.parse(c) || {};
          } catch (e) {
            obj = {};
          }
        } else if (typeof c === "object" && c) {
          obj = c;
        }

        return JSON.stringify(obj);
      })();

      submitData.datasourceConfig = JSON.stringify({
        username: submitData.username,
        password: encrypt(submitData.password),

        dbname: submitData.dbname,
        sid: submitData.sid,

        config: saveConfigAdd,
        allowOperate: submitData.allowOperate,
        useSSL: submitData.useSSL,
        sslConfig: submitData.useSSL === 1 ? submitData.sslConfig : {},

      });

      // 删除外层的 sslConfig 和 useSSL，避免重复传递
      delete submitData.sslConfig;
      delete submitData.useSSL;

      handleSavePromise(addDaDatasource(submitData), submitData, false);
    }
  };

  const hasError = currentDiagnosis.value && (currentDiagnosis.value.overallStatus === "FAIL" || currentDiagnosis.value.errorCount > 0);

  // 预期状态：新建默认为启用(true)，编辑则保持原有状态
  const intendToEnable = submitData.id == null ? true : submitData.validFlag;

  if (hasError && intendToEnable) {
    ElMessageBox.confirm(
        "连接测试未通过，暂无法启用。请检查连接配置后重试；如继续保存，连接状态将自动调整为“禁用”。",
        "提示",
        {
          confirmButtonText: "保存",
          cancelButtonText: "取消",
          type: "warning",
        }
    )
        .then(() => {
          submitData.validFlag = false;
          proceedSubmit();
        })
        .catch(() => {
          btnLoading.value = false;
        });
  } else {
    submitData.validFlag = intendToEnable;
    proceedSubmit();
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  proxy.$modal
      .confirm('是否确认删除数据连接编号为"' + _ids + '"的数据项？')
      .then(function () {
        return removeDppOrDa(_ids, type);
      })
      .then(() => {
        handleQuery();
        proxy.$modal.msgSuccess("删除成功");
      })
      .catch(() => {
      });
}

/** 改变启用状态值 */
function handleStatusChange(row) {
  const text = row.validFlag === true ? "启用" : "禁用";
  const status = row.validFlag === true ? 1 : 0;
  // 先临时反转，弹窗取消时还原
  const tempVal = row.validFlag;
  proxy.$modal
      .confirm("确认要" + text + ' "' + row.datasourceName + '" 数据连接吗？')
      .then(function () {
        statusLoading[row.id] = true;
        editDatasourceStatus(row.id, status)
            .then((response) => {
              proxy.$modal.msgSuccess(text + "成功");
            })
            .catch((error) => {
              // 接口失败/取消弹窗，恢复开关
              row.validFlag = tempVal;
              if (error.code === 10001) {
                ElMessageBox.alert(error.msg, "提示", {
                  cancelButtonText: "取消",
                  type: "warning",
                })
                    .then(() => {
                      // 点击取消后，恢复开关状态并刷新列表
                      row.validFlag = tempVal;
                      statusLoading[row.id] = false;
                      handleQuery();
                    })
                    .catch(() => {
                      row.validFlag = tempVal;
                      statusLoading[row.id] = false;
                      handleQuery();
                    });
              } else {
                ElMessageBox.alert(error.msg, "提示", {
                  confirmButtonText: "确定",
                  type: "warning",
                });
              }
            })
            .finally(() => {
              statusLoading[row.id] = false;
              handleQuery();
            });
      })
      .catch(function () {
        row.validFlag = !row.validFlag;
      });
}

getProjectOptions();
</script>

<style lang="scss" scoped>
.datasource-wizard-dialog {
  :deep(.el-dialog__body) {
    padding-top: 0;
  }
}

.app-container {
  :deep(.datasource-test-result-dialog .el-dialog__body) {
    padding: 20px 40px !important;
  }
}

.butgdlist .el-button {
  margin-left: 0;
}
</style>
