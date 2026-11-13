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
  <div
    class="container configView"
    :class="{ 'is-embedded': !props.showHeader }"
    :style="{ width: props.fill ? '100%' : `${currWidth}px` }"
  >
    <div v-if="props.showResize" class="move" @mousedown="resizeCurrDialog"></div>
    <div v-if="props.showHeader" class="container-header">
      <span class="title">{{ props.currValue.name }}</span>
      <!-- <div class="icon">
        <el-icon><Operation /></el-icon>
        <span>{{ props.currValue.name }}</span>
      </div> -->
      <span class="close" @click="closeCurrDialog">
        <el-icon>
          <Minus />
        </el-icon>
      </span>
    </div>
    <div class="container-content">
      <template v-if="props.currValue.type == 'attrConfig'">
        <el-form
          ref="configRef"
          :model="form"
          :rules="rules"
          label-width="118px"
          @submit.prevent
          :disabled="readOnly"
        >
          <div class="h2">
            <img
              class="icon"
              src="@/assets/modules/da/icons/asset/h2 (1).svg"
              alt=""
            />基础配置
          </div>
          <qt-form-item label="任务优先级:" prop="taskPriority" :tip="{content:`设置该任务在系统中的执行优先级，高优先级任务会优先调度<br/>HIGHEST：最高优先级，立即执行<br/>HIGH：高优先级<br/>MEDIUM：中等优先级<br/>LOW：低优先级<br/>LOWEST：最低优先级`,custom:true}">
            <el-select
              v-model="form.taskPriority"
              placeholder="请选择任务优先级"
            >
              <el-option
                v-for="(item, index) in dpp_etl_task_priority"
                :key="index"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </qt-form-item>
          <qt-form-item label="Worker分组:" prop="workerGroup" :tip="{content:`指定该任务运行的 Worker 组，用于资源隔离和负载均衡。<br/>不同分组可分配不同资源池；<br/>支持多租户场景下的任务隔离。<br/>示例：default（默认分组）、data_quality_high（高性能分组）`,custom:true}">
            <el-input
              v-model="form.workerGroup"
              placeholder="请输入Worker分组"
              disabled
            />
          </qt-form-item>
          <qt-form-item label="失败重试次数:" prop="failRetryTimes" :tip="{content:'当任务执行失败时，系统将自动重试的次数。设置合理可避免临时故障导致任务中断。'}">
            <el-input-number
              style="width: 85%; margin-right: 5px"
              controls-position="right"
              :min="0"
              v-model="form.failRetryTimes"
              placeholder="请输入失败重试次数"
            >
            </el-input-number>
            <span>次</span>
          </qt-form-item>
          <qt-form-item label="失败重试间隔:" prop="failRetryInterval" :tip="{content:'两次重试之间的等待时间（单位：分钟）'}">
            <el-input-number
              style="width: 85%; margin-right: 5px"
              controls-position="right"
              :min="0"
              v-model="form.failRetryInterval"
              placeholder="请输入失败重试间隔"
            >
            </el-input-number>
            <span>分</span>
          </qt-form-item>
          <qt-form-item label="延迟执行时间:" prop="delayTime" :tip="{content:'若任务因资源不足等原因无法立即执行，系统将延迟多久后再次尝试'}">
            <el-input-number
              style="width: 85%; margin-right: 5px"
              controls-position="right"
              :min="isShowWithTypeName('DM,Oracle,MYSQL,Kingbase') ? 1 : 0"
              v-model="form.delayTime"
              placeholder="请输入延迟执行时间"
            >
            </el-input-number>
            <span>分</span>
          </qt-form-item>
          <template v-if="isShowWithTypeName('Flink批,Flink流')">
            <qt-form-item label="JobManager内存数:" prop="jobManagerMemory" :tip="{content:`JobManager 是 Flink 集群的主控节点，负责任务调度、容错管理等。`}">
              <el-input
                v-model="form.jobManagerMemory"
                placeholder="请输入JobManager内存数"
              >
              </el-input>
            </qt-form-item>
            <qt-form-item label="TaskManager内存数:" prop="taskManagerMemory" :tip="{content:`实际执行计算任务的工作节点`}">
              <el-input
                v-model="form.taskManagerMemory"
                placeholder="请输入TaskManager内存数"
              >
              </el-input>
            </qt-form-item>
            <qt-form-item label="Slot数量:" prop="slot" :tip="{content:`TaskManager 可以同时运行的任务线程数`}">
              <el-input-number
                placeholder="请输入Slot数量"
                v-model="form.slot"
                controls-position="right"
                :min="0"
              />
            </qt-form-item>
            <qt-form-item label="TaskManager数量:" prop="taskManager" :tip="{content:`决定任务的最大并行能力`}">
              <el-input
                v-model="form.taskManager"
                placeholder="请输入TaskManager数量"
              >
              </el-input>
            </qt-form-item>
            <qt-form-item label="并行度:" prop="parallelism" :tip="{content:`任务的并行执行程度，即有多少个子任务可以同时运行。`}">
              <el-input-number
                placeholder="请输入并行度"
                v-model="form.parallelism"
                controls-position="right"
                :min="0"
              />
            </qt-form-item>
            <qt-form-item label="Yarn队列:" prop="yarnQueue" :tip="{content:`指定该任务提交到 YARN 的哪个队列，用于资源隔离和权限控制。`}">
              <el-input
                v-model="form.yarnQueue"
                placeholder="请输入Yarn队列(选填)"
              >
              </el-input>
            </qt-form-item>
          </template>
          <template v-if="isShowWithTypeName('SparkSql')">
            <el-form-item label="Driver核心数:" prop="driverCores">
              <el-input-number
                placeholder="请输入Driver核心数"
                v-model="form.driverCores"
                controls-position="right"
                :min="0"
              />
            </el-form-item>
            <el-form-item label="Driver内存数:" prop="driverMemory">
              <el-input
                v-model="form.driverMemory"
                placeholder="请输入Driver内存数"
              >
              </el-input>
            </el-form-item>
            <el-form-item label="Executor数量:" prop="numExecutors">
              <el-input-number
                placeholder="请输入Executor数量"
                v-model="form.numExecutors"
                controls-position="right"
                :min="0"
              />
            </el-form-item>
            <el-form-item label="Executor内存数:" prop="executorMemory">
              <el-input
                v-model="form.executorMemory"
                placeholder="请输入Executor内存数"
              >
              </el-input>
            </el-form-item>
            <el-form-item label="Executor核心数:" prop="executorCores">
              <el-input-number
                placeholder="请输入Executor核心数"
                v-model="form.executorCores"
                controls-position="right"
                :min="0"
              />
            </el-form-item>
            <el-form-item label="Yarn队列:" prop="yarnQueue">
              <el-input
                v-model="form.yarnQueue"
                placeholder="请输入Yarn队列(选填)"
              >
              </el-input>
            </el-form-item>
          </template>
          <div class="h2">
            <img
              class="icon"
              src="@/assets/modules/da/icons/asset/h2 (1).svg"
              alt=""
            />其他配置
          </div>
          <el-form-item label="数据连接类型:" prop="typaCode">
            {{ typaName }}
          </el-form-item>
          <el-form-item
            label="数据源连接:"
            prop="datasourceId"
            v-if="isShowWithTypeName('SparkSql,Flink批,Flink流', false)"
          >
            <el-select
              v-model="form.datasourceId"
              placeholder="请选择数据源连接"
              @change="handleDatasourceChange"
              filterable
            >
              <el-option
                v-for="dict in createTypeList"
                :key="dict.id"
                :label="dict.datasourceName"
                :value="dict.id"
              ></el-option>
            </el-select>
          </el-form-item>
          <qt-form-item v-if="!isOdpsType" label="分段执行符号:" prop="segm" :tip="{content:'若脚本包含多个独立SQL语句，可用此符号分隔。<br/>示例：;（分号）表示每条语句以分号结尾',custom:true}">
            <el-input
              v-model="form.segm"
              placeholder="请输入分段执行符号"
            ></el-input>
          </qt-form-item>
          <div class="h2">
            <img
              class="icon"
              src="@/assets/modules/da/icons/asset/h2 (1).svg"
              alt=""
            />参数配置
          </div>
          <qt-form-item label="自定义参数:" prop="localParams" :tip="{content:`可添加额外的参数，用于动态传参。<br/>示例：@p_username = 'admin'，可在脚本中引用`,custom:true}">
            <button class="add-btn" type="button" @click="handleAddDiy">
              <el-icon class="icon">
                <Plus />
              </el-icon>
              添加配置项
            </button>
          </qt-form-item>
          <div
            class="wrap"
            v-for="(item, index) in form.localParams"
            :key="index"
          >
            <el-input
              style="width: 30%"
              v-model="item.prop"
              placeholder="参数名称"
            ></el-input>
            <el-select
              style="width: 40%; margin: 0 4px"
              v-model="item.type"
              placeholder="请选择参数类型"
            >
              <el-option
                v-for="dict in columnType"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              ></el-option>
            </el-select>
            <el-input
              style="width: 30%; margin-right: 4px"
              v-model="item.value"
              placeholder="值"
            ></el-input>
            <div class="del-btn" @click="handleDelDiy(index)">
              <el-icon class="icon">
                <Delete />
              </el-icon>
            </div>
          </div>
        </el-form>
      </template>
    </div>
  </div>
</template>
<script setup name="EditorConfigView">
//
import { treeData } from "@/views/dpp/task/developTask/data";
import { listDaDatasourceNoKafkaByProjectCode } from "@/api/da/dataSource/dataSource";
const { proxy } = getCurrentInstance();
const { dpp_etl_task_priority } = proxy.useDict("dpp_etl_task_priority");
import useUserStore from "@/store/system/user";
const userStore = useUserStore();
const emits = defineEmits(["close"]);
const props = defineProps({
  currValue: {
    type: Object,
    default: () => {
      return {
        name: "",
        taskDefinitionList: [],
      };
    },
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
  showHeader: {
    type: Boolean,
    default: true,
  },
  showResize: {
    type: Boolean,
    default: true,
  },
  fill: {
    type: Boolean,
    default: false,
  },
});
const isShowWithTypeName = (row, boo = true) => {
  if (boo) {
    return row.includes(typaName.value);
  } else {
    return !row.includes(typaName.value);
  }
};
const columnType = ref([
  {
    value: "VARCHAR",
    label: "VARCHAR",
  },
  {
    value: "INTEGER",
    label: "INTEGER",
  },
  {
    value: "LONG",
    label: "LONG",
  },
  {
    value: "FLOAT",
    label: "FLOAT",
  },
  {
    value: "DOUBLE",
    label: "DOUBLE",
  },
  {
    value: "DATE",
    label: "DATE",
  },
  {
    value: "TIME",
    label: "TIME",
  },
  {
    value: "TIMESTAMP",
    label: "TIMESTAMP",
  },
  {
    value: "BOOLEAN",
    label: "BOOLEAN",
  },
]);

const typaName = computed(() => {
  let typeNmae = treeData.find(
    (item) => item.value == form.value.typaCode
  )?.label;
  return typeNmae;
});
const configRef = ref();
const createTypeList = ref([]);
function getDaDatasource() {
  listDaDatasourceNoKafkaByProjectCode({
    projectCode: userStore.projectCode,
    projectId: userStore.projectId,
    datasourceType: form.value.typaCode,
  }).then((response) => {
    createTypeList.value = response.data;
  });
}
const handleDatasourceChange = async (value) => {
  const selectedDatasource = createTypeList.value.find(
    (item) => item.id == value
  );
  let { datasourceType, datasourceConfig, ip, port, id } = selectedDatasource;
  let code = JSON.parse(datasourceConfig);
  form.value.datasources = {
    datasourceType,
    datasourceConfig,
    ip,
    port,
    dbname: code.dbname,
    datasource_id: id,
    datasourceId: id,
  };
};

const radioOptions = ref([
  { componentType: "51", label: "查询", taskType: "SQL", id: "0", show: true },
  {
    componentType: "51",
    label: "非查询",
    taskType: "SQL",
    id: "1",
    show: true,
  },
  {
    componentType: "52",
    label: "储存过程",
    taskType: "PROCEDURE",
    id: "2",
    show: true,
  },
  {
    componentType: "53",
    label: "SparkSql开发",
    taskType: "SPARK",
    id: "4",
    show: false,
  },
  {
    componentType: "55",
    label: "FlinkSql开发",
    taskType: "FLINK",
    id: "5",
    show: false,
  },
]);

const visibleRadioOptions = computed(() =>
  radioOptions.value.filter((option) => option.show)
);
const closeCurrDialog = () => {
  emits("close");
};

// #region curr弹框拖拽
const currWidth = ref(340); // 初始左侧宽度
const isCurrResizing = ref(false); // 判断是否正在拖拽
let startX = 0; // 鼠标按下时的初始位置
const resizeCurrDialog = (event) => {
  isCurrResizing.value = true;
  startX = event.clientX;
  // 使用 requestAnimationFrame 减少重绘频率
  document.addEventListener("mousemove", updateCurrResize);
  document.addEventListener("mouseup", stopCurrResize);
};
const updateCurrResize = (event) => {
  if (isCurrResizing.value) {
    const delta = startX - event.clientX; // 计算鼠标移动距离
    currWidth.value += delta; // 修改左侧宽度
    startX = event.clientX; // 更新起始位置
    if (currWidth.value > 650) {
      currWidth.value = 650;
      return;
    } else if (currWidth.value < 300) {
      currWidth.value = 300;
      return;
    }
    // 使用 requestAnimationFrame 来减少页面重绘频率
    requestAnimationFrame(() => {});
  }
};
const stopCurrResize = () => {
  isCurrResizing.value = false;
  document.removeEventListener("mousemove", resizeCurrDialog);
  document.removeEventListener("mouseup", stopCurrResize);
};
// #endregion

const data = reactive({
  form: {
    // 基础配置
    taskPriority: "MEDIUM",
    workerGroup: "default",
    failRetryTimes: "0",
    failRetryInterval: "1",
    delayTime: "0",
    // Fink配置
    jobManagerMemory: "1G",
    taskManagerMemory: "2G",
    slot: 1,
    taskManager: 2,
    parallelism: 1,
    yarnQueue: "",
    // Spark配置
    driverCores: 1,
    driverMemory: "512M",
    numExecutors: 1,
    executorMemory: "1G",
    executorCores: 1,
    // 其他配置
    typaCode: "",
    datasourceId: "",
    sqlType: "0",
    taskType: "",
    segm: "",
    componentType: "",
    // 参数配置
    localParams: [],
  },
  rules: {
    taskPriority: [
      { required: true, message: "请选择任务优先级", trigger: "change" },
    ],
    workerGroup: [
      { required: true, message: "请输入Worker分组", trigger: "change" },
      {
        validator: (rule, value, callback) => {
          if (value && value.trim() === "") {
            callback(new Error("请输入Worker分组"));
          } else {
            callback();
          }
        },
        trigger: "blur",
      },
    ],
    datasourceId: [
      { required: true, message: "请选择数据源连接", trigger: "change" },
    ],
  },
});
const { form, rules } = toRefs(data);
const lastConfigTaskKey = ref("");

function getConfigTaskKey(value = {}) {
  const taskData = value.data || {};
  return String(
    taskData.taskConfig?.id ||
      taskData.id ||
      taskData.taskConfig?.code ||
      taskData.code ||
      ""
  );
}

watch(
  () => props.currValue,
  (val) => {
    if (val.type == "attrConfig") {
      const taskKey = getConfigTaskKey(val);
      // Preserve unsaved values when the same task's panel is minimized and reopened.
      if (taskKey && taskKey === lastConfigTaskKey.value) {
        getDaDatasource();
        return;
      }
      let taskParams =
        val.data.taskDefinitionList.length > 0 &&
        val.data.taskDefinitionList[0].taskParams;
      form.value = {
        ...form.value,
        ...taskParams,
        typaCode: val.data.draftJson
          ? JSON.parse(val.data.draftJson).typaCode
          : "",
      };
      // 执行时间默认为1分钟
      if (
        isShowWithTypeName("DM,Oracle,MYSQL,Kingbase") &&
        form.value.delayTime == 0
      ) {
        form.value.delayTime = 1;
      }
      // Flink特殊字段
      form.value.executeMode =
        form.value.typaCode == "FlinkBatch"
          ? "BATCH"
          : form.value.typaCode == "FlinkStream"
          ? "STREAM"
          : "";
      let obj;
      if (form.value.typaCode == "SparkSql") {
        obj = radioOptions.value?.find((option) => option.id == 4);
      } else if (
        form.value.typaCode == "FlinkBatch" ||
        form.value.typaCode == "FlinkStream"
      ) {
        obj = radioOptions.value?.find((option) => option.id == 5);
      } else {
        obj = radioOptions.value?.find(
          (option) => option.id == form.value.sqlType
        );
      }
      form.value.taskType = obj?.taskType;
      form.value.componentType = obj?.componentType;
      lastConfigTaskKey.value = taskKey;
      // 获取： 数据源连接
      getDaDatasource();
      console.log("🚀 ~ form.value:", form.value);
    }
  },
  {
    immediate: true,
  }
);

const handleSqlTypeChange = () => {
  normalizeOdpsSqlType();
  let obj = radioOptions.value?.find(
    (option) => option.id == form.value.sqlType
  );
  form.value.taskType = obj?.taskType;
  form.value.componentType = obj?.componentType;
};
watch(
  () => form.value.typaCode,
  () => {
    normalizeOdpsSqlType();
  }
);
const handleAddDiy = () => {
  form.value.localParams.push({
    prop: "",
    type: "",
    value: "",
    direct: "IN",
  });
};
const handleDelDiy = (index) => {
  form.value.localParams.splice(index, 1);
};
defineExpose({ currWidth, form, configRef });
</script>
<style lang="scss" scoped>
.configView {
  //   position: absolute;
  //   bottom: 15px;
  position: relative;
  user-select: auto;
  height: 100%;
  margin-top: 0px;
  border-radius: 5px;
  background-color: #fbfbfb;
  // max-width: 650px;
  // min-width: 200px;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-left: none;
  border-top: none;

  .move {
    position: absolute;
    user-select: none;
    width: 10px;
    height: 100%;
    top: 0px;
    left: -5px;
    cursor: col-resize;
  }

  .container-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 40px;
    padding: 0 15px;
    border-bottom: 1px solid rgb(147 147 147 / 6%);
    background-color: #f9f9f9;
    box-sizing: border-box;

    .title {
      overflow: visible;
      font-weight: 500;
      font-size: 16px;
      font-family: xPingFang SC;
      white-space: nowrap;
      text-overflow: ellipsis;
      color: #333;
      display: flex;
      align-items: center;

      &::before {
        display: inline-block;
        content: "";
        width: var(--qt-title-marker-width);
        height: var(--qt-title-marker-height);
        border-radius: var(--qt-title-marker-radius);
        background: var(--qt-title-marker-color);
        margin-right: var(--qt-title-marker-gap);
      }
    }

    .close {
      cursor: pointer;
      display: inline-flex;
      justify-content: center;
      align-items: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      font-size: 16px;
      color: var(--el-color-primary);

      &:hover {
        background-color: rgb(0, 0, 0, 0.06);
      }
    }

    .icon {
      width: 110px;
      height: 32px;
      display: flex;
      justify-content: center;
      align-items: center;
      background: #eaf0ff;
      border-radius: 2px;
      border: 1px solid var(--el-color-primary);
      color: var(--el-color-primary);
      font-size: 14px;
    }
  }

  .container-content {
    height: calc(100% - 40px);
    overflow: hidden auto;
    padding: 14px 10px 10px;
    background-color: #fff;

    &::-webkit-scrollbar {
      width: 4px;
    }

    .h2 {
      min-height: 30px;
      padding: 0;
      background: transparent;
      border-radius: 0;
      display: flex;
      align-items: center;
      font-weight: 600;
      font-size: 14px;
      font-family: PingFang SC;
      margin: 0 0 8px;
      color: var(--el-color-primary);
      line-height: 20px;
      white-space: nowrap;

      // span {
      //   display: inline-block;
      //   width: 20px;
      //   height: 20px;
      //   line-height: 18px;
      //   border-radius: 50%;
      //   background-color: var(--el-color-primary);
      //   text-align: center;
      //   font-size: 14px;
      //   margin-right: 5px;
      //   color: #fff;
      // }
      .icon {
        display: none;
        // height: 20px;
        // background: url("@/assets/modules/da/icons/asset/h2 (1).svg") no-repeat;
      }
    }

    :deep(.el-form-item) {
      margin-bottom: 10px;

      .el-form-item__label {
        height: 30px;
        padding-right: 8px;
        color: #606266;
        font-size: 13px;
        line-height: 30px;
        text-align: right;
        white-space: nowrap;
        word-break: keep-all;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .add-btn {
      cursor: pointer;
      width: 100%;
      height: 30px;
      padding: 0 10px;
      border-radius: 2px;
      display: flex;
      justify-content: center;
      align-items: center;
      border: 1px solid var(--el-color-primary);
      background: #fff;
      font-size: 13px;
      line-height: 28px;
      color: var(--el-color-primary);
      box-sizing: border-box;

      .icon {
        margin-right: 5px;
        color: var(--el-color-primary);
      }

      &:hover,
      &:focus,
      &:active {
        color: var(--el-color-primary);
        border-color: var(--el-color-primary);
        background: #fff;
        outline: none;
      }
    }

    .del-btn {
      cursor: pointer;

      .icon {
        color: #f00;
      }
    }

    .wrap {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 10px;
    }
  }

  &.is-embedded {
    border: 0;
    border-radius: 0;

    .container-content {
      height: 100%;
    }
  }
}
</style>
