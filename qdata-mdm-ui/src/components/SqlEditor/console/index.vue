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
  <div class="container console" :style="{ height: `${currHeight}px` }">
    <div class="move" @mousedown="resizeCurrDialog"></div>
    <div class="container-header">
      <span class="title">{{ currItem.name }}</span>
      <span class="close" @click="closeCurrDialog">
        <el-icon>
          <Minus />
        </el-icon>
      </span>
    </div>
    <div class="container-content">
      <template v-if="currItem.type == 'console'">
        <div class="console-view" v-loading="consoleLoading">
          <div v-if="hasConsoleLog" class="codeEdit" style="width: 100%">
            <CodeShow
              v-model="currCode.log"
              enableMiniMap
              enableAutoScroll
              language="javalog"
              :config="codeConfig"
            />
          </div>
          <div v-else class="emptyBg console-empty-state">
            <img
              :src="emptyStateUrl"
              alt=""
            />
            <p style="color: #909399; font-size: 14px">暂无日志</p>
          </div>
        </div>
      </template>
      <template v-if="currItem.type == 'result'">
        <div class="result-view">
          <el-row :gutter="10" class="btn-style pb15">
            <el-button
              :disabled="!(currItem.data?.data?.length > 0)"
              type="warning"
              plain
              icon="Download"
              @click="handleExport"
              :loading="downloadLoading"
              >导出</el-button
            >
          </el-row>

          <div
            v-if="localData.data && localData.data.length"
            class="result-table"
          >
            <el-table
              :data="localData.data"
              stripe
              style="width: 100%"
              v-loading="loading"
            >
              <!-- 动态生成列 -->
              <template v-for="column in localData.columnList" :key="column">
                <el-table-column
                  :prop="column"
                  :label="column"
                  align="center"
                  :min-width="180"
                  :show-overflow-tooltip="{ effect: 'light' }"
                />
              </template>
              <template #empty>
                <div class="emptyBg console-empty-state table-empty-state">
                  <img
                    :src="emptyStateUrl"
                    alt=""
                  />
                  <p>暂无记录</p>
                </div>
              </template>
            </el-table>
            <div v-if="localData.total">
              <pagination
                v-show="localData.total > 0"
                :total="localData.total"
                v-model:page="localData.pageNum"
                v-model:limit="localData.pageSize"
                @pagination="handlePagination"
              />
            </div>
          </div>
          <div v-else class="emptyBg console-empty-state result-empty">
            <img
              :src="emptyStateUrl"
              alt=""
            />
            <p style="color: #909399; font-size: 14px">暂无数据</p>
          </div>
        </div>
      </template>
      <template v-if="currItem.type == 'history'">
        <el-row :gutter="10" class="btn-style pb15">
          <el-button type="primary" plain icon="Refresh" @click="fetchHistory"
            >刷新</el-button
          >
        </el-row>
        <div class="history-view">
          <qt-table ref="historyTableRef" v-bind="historyTableStore">
            <template #empty>
              <div class="emptyBg console-empty-state table-empty-state">
                <img
                  :src="emptyStateUrl"
                  alt=""
                />
                <p>暂无记录</p>
              </div>
            </template>
            <template #action="scope">
              <el-button link type="primary" @click="viewLogDetail(scope.row)">
                <el-icon><View /></el-icon> 查看日志
              </el-button>
              <el-button
                link
                type="warning"
                @click="downloadLog(scope.row)"
                @mousedown="(e) => e.preventDefault()"
              >
                <el-icon><Download /></el-icon> 下载日志
              </el-button>
            </template>
          </qt-table>
        </div>
      </template>
    </div>
  </div>

  <!-- Log Detail Dialog -->
  <el-dialog
    v-model="logDialogVisible"
    :title="logTitle"
    width="1200px"
    draggable
    destroy-on-close
  >
    <div class="log-detail-content">
      <div v-if="logLoading" class="log-loading-container">
        <el-skeleton :rows="10" animated class="log-skeleton" />
      </div>
      <div v-else v-html="formattedText"></div>
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeLogDialog">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>
<script setup name="EditorConsole">
import {
  ref,
  computed,
  watch,
  reactive,
  onMounted,
  getCurrentInstance,
} from "vue";
import { ElMessage, ElMessageBox, ElNotification } from "element-plus";
import CodeShow from "@/components/SqlEditor/editorShow/index.vue";
import emptyStateUrl from "@/assets/common/images/empty-state/empty-nodata.png";
import Pagination from "@/components/Pagination/index.vue";
import {
  getRunTaskInstance,
  getLogByTaskInstanceId,
} from "@/api/dpp/task/index.js";
import {
  listDppEtlNodeInstance,
  logDetailCat,
  getLastLog,
} from "@/api/dpp/instance/integratio.js";
import { executeSqlQuery } from "@/api/da/dataSource/dataSource";
import QtTable from "@/components/QtTable/index.vue";
// #region curr弹框拖拽
const currHeight = ref(345); // 初始左侧宽度
const isCurrResizing = ref(false); // 判断是否正在拖拽
let startY = 0; // 鼠标按下时的初始位置
const resizeCurrDialog = (event) => {
  isCurrResizing.value = true;
  startY = event.clientY;
  // 使用 requestAnimationFrame 减少重绘频率
  document.addEventListener("mousemove", updateCurrResize);
  document.addEventListener("mouseup", stopCurrResize);
};
const updateCurrResize = (event) => {
  if (isCurrResizing.value) {
    const delta = startY - event.clientY; // 计算鼠标移动距离
    currHeight.value += delta; // 修改左侧宽度
    startY = event.clientY; // 更新起始位置
    if (currHeight.value > 720) {
      currHeight.value = 720;
      return;
    } else if (currHeight.value < 150) {
      currHeight.value = 150;
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
const props = defineProps({
  currValue: {
    type: Object,
    default: () => {
      return {
        name: "",
        data: {
          id: "",
          sqlText: "",
          pageNum: 1,
          pageSize: 20,
          data: [],
          total: 0,
        },
      };
    },
  },
});
const emits = defineEmits(["close"]);
const closeCurrDialog = () => {
  emits("close");
};

// 本地数据存储
const localData = ref({
  id: props.currValue.data?.id || "",
  sqlText: props.currValue.data?.sqlText || "",
  pageNum: 1,
  pageSize: 20,
  data: [],
  total: 0,
  columnList: [],
});

// 数据加载状态
const loading = ref(false);

// 获取数据的函数
const fetchData = async () => {
  if (!localData.value.id) return;

  loading.value = true;
  try {
    const res = await executeSqlQuery({
      id: localData.value.id,
      sqlText: localData.value.sqlText,
      pageNum: localData.value.pageNum,
      pageSize: localData.value.pageSize,
    });

    if (res.code === 200) {
      const dataList = Array.isArray(res.data.data) ? [...res.data.data] : [];
      const columnList = dataList.length > 0 ? Object.keys(dataList[0]) : [];

      localData.value = {
        ...localData.value,
        ...res.data,
        data: dataList,
        columnList: columnList,
        total: res.data.total || 0,
      };
    } else {
      ElMessage.warning(res.msg || "查询失败");
    }
  } catch (error) {
    console.error("查询失败:", error);
    ElMessage.warning("查询失败，请联系管理员！");
  } finally {
    loading.value = false;
  }
};

// 监听外部数据变化
watch(
  () => props.currValue,
  (newValue, oldValue) => {
    if (newValue && newValue.type === "result" && newValue.data) {
      const dataList = Array.isArray(newValue.data.data)
        ? [...newValue.data.data]
        : [];
      const columnList = dataList.length > 0 ? Object.keys(dataList[0]) : [];

      localData.value = {
        ...localData.value,
        id: newValue.data.id || "",
        sqlText: newValue.data.sqlText || "",
        pageNum: newValue.data.pageNum || 1,
        pageSize: newValue.data.pageSize || 20,
        data: dataList,
        total: newValue.data.total || 0,
        columnList,
      };
    }
  },
  { deep: true, immediate: true }
);

// 监听历史记录面板的 id 变化，确保参数就绪后刷新
watch(
  () => props.currValue,
  (newValue, oldValue) => {
    if (
      newValue?.type === "history" &&
      newValue?.id &&
      newValue?.id !== oldValue?.id
    ) {
      historyTableStore.params = {
        taskType: 3,
        nodeCode: newValue.id,
      };
    }
  },
  { deep: true }
);
// 处理分页变化
const handlePagination = (pageNum, pageSize) => {
  localData.value.pageNum = pageNum;
  localData.value.pageSize = pageSize;
  fetchData();
};

// 保留 currItem 计算属性用于其他用途
const currItem = computed(() => {
  return props.currValue;
});
const currCode = ref({
  log: "",
});
const codeConfig = {
  readOnly: true,
  domReadOnly: true,
  renderLineHighlight: "none",
  occurrencesHighlight: false,
  selectionHighlight: false,
  matchBrackets: "never",
  cursorStyle: "line-thin",
  cursorBlinking: "solid",
  hideCursorInOverviewRuler: true,
  overviewRulerLanes: 0,
};
const hasConsoleLog = computed(() => {
  return !!(currCode.value.log && currCode.value.log.trim());
});
const consoleLoading = ref(false);

// Get proxy for modal and download
const { proxy } = getCurrentInstance();

// Execution history data structures
const historyTableRef = ref(null);

// Get dictionary data for status and command type - matching developTask/index.vue
const dicts = proxy.useDict(
  "dpp_etl_node_instance",
  "dpp_etl_task_instance_command_type"
);

// History table configuration - exact match to developTask/index.vue pattern
const historyTableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "create_time", order: "descending" },
    },
  },
  columns: [
    {
      label: "编号",
      prop: "id",
      sortable: true,
      width: 100,
    },
    {
      label: "节点实例名称",
      prop: "name",
      align: "left",
      width: 300,
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "任务名称",
      prop: "taskInstanceName",
      width: 400,
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "执行类型",
      prop: "commandType",
      width: 120,
      dict: "dpp_etl_task_instance_command_type",
    },
    {
      label: "执行状态",
      prop: "status",
      width: 100,
      dict: "dpp_etl_node_instance",
    },
    {
      label: "开始时间",
      prop: "startTime",
      width: 160,
      date: true,
      sortable: true,
      slot: "start-time",
    },
    {
      label: "结束时间",
      prop: "endTime",
      width: 160,
      date: true,
      sortable: true,
      slot: "end-time",
      sortable: "custom",
    },
    {
      label: "责任人",
      prop: "personChargeName",
      width: 100,
    },
    {
      label: "创建人",
      prop: "createBy",
      width: 100,
      showOverflowTooltip: true,
    },
    {
      label: "创建时间",
      prop: "createTime",
      sortable: true,
      sortableKey: "create_time",
      date: true,
      width: 160,
    },
    {
      label: "操作",
      width: 200,
      align: "center",
      fixed: "right",
      slot: "action",
    },
  ],
  func: listDppEtlNodeInstance,
  events: {
    formatParams(params) {
      if (!params.time || !params.time.length) return params;
      const { time, ...other } = { ...params };
      other.startTime = `${time[0]} 00:00:00`;
      other.endTime = `${time[1]} 23:59:59`;
      return other;
    },
  },
  params: {
    taskType: 3,
    nodeCode: currItem.value.id,
  },
});

// Handle search button click (refresh data)
const resultTableRef = ref(null);

const handleSearch = () => {
  // Refresh data by resetting to first page and calling pagination handler
  localData.value.pageNum = 1;
  fetchData();
};

// Loading state for export
const downloadLoading = ref(false);

// Calculate total files for export (5000 records per file)
const totalFiles = computed(() => {
  const totalRecords =
    localData.value.total || localData.value.data?.length || 0;
  return Math.ceil(totalRecords / 5000);
});

// Export query results using server-side API
const downloadMethod = () => {
  const pageSize = 5000;
  const total = localData.value.total || localData.value.data?.length || 0;
  let pageNum = 1;

  downloadLoading.value = true;

  if (total === 0) {
    ElNotification({
      title: "提示",
      message: "该表没有数据",
      type: "info",
      duration: 2000,
    });
    downloadLoading.value = false;
    return;
  }

  // Prepare export parameters
  const exportParams = {
    id: localData.value.id || "",
    sqlText: localData.value.sqlText || "",
    pageSize,
    exportType: 0,
  };

  // Export data in chunks of 5000 records per file
  while ((pageNum - 1) * pageSize < total) {
    exportParams.pageNum = pageNum;
    proxy.download2(
      "/da/dataSource/exportSqlQueryResult/export",
      exportParams,
      `${new Date().getTime()}_${pageNum}.xlsx`
    );
    pageNum++;
  }

  downloadLoading.value = false;
};

// Show export confirmation dialog
const handleExport = () => {
  const totalRecords =
    localData.value.total || localData.value.data?.length || 0;
  const totalFilesCount = totalFiles.value;

  // Confirm export with user
  ElMessageBox.confirm(
    `是否导出总数为：<span style="color: rgb(0, 160, 233);">${totalRecords}</span>，以每5000数据一份文件进行导出，总共导出 ${totalFilesCount} 份？`,
    "提示",
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning",
    }
  )
    .then(() => {
      downloadMethod();
    })
    .catch(() => {
      // User cancelled export
    });
};

// Log detail dialog
const logDialogVisible = ref(false);
const logTitle = ref("");
const logLoading = ref(false);
let msg = ref();

// Format log text - matching developTask/index.vue pattern
const formattedText = computed(() => {
  return typeof msg.value === "string" ? msg.value.replace(/\n/g, "<br>") : "";
});
const fetchConsoleLog = async () => {
  if (!currItem.value || currItem.value.type !== "console") {
    return;
  }
  const nodeCode = currItem.value?.data?.code || currItem.value?.id;
  if (!nodeCode) {
    currCode.value.log = "";
    return;
  }
  consoleLoading.value = true;
  try {
    const response = await getLastLog(nodeCode);
    if (response && typeof response.msg === "string" && response.msg.trim()) {
      currCode.value.log = response.msg;
    } else {
      currCode.value.log = "";
    }
  } catch (error) {
    console.error("获取日志失败", error);
    currCode.value.log = "获取日志失败";
  } finally {
    consoleLoading.value = false;
  }
};

// Fetch execution history
const fetchHistory = () => {
  if (!currItem.value || !currItem.value.id) {
    proxy.$modal.msgWarning("无效的任务ID");
    return;
  }

  if (historyTableRef.value) {
    historyTableRef.value.getList();
  }
};

onMounted(() => {
  if (currItem.value.type == "console") {
    fetchConsoleLog();
  }
});
// View log detail - matching developTask/index.vue pattern
const viewLogDetail = (row) => {
  logTitle.value = `${row.name} - 日志详情`;
  msg.value = {};
  logDialogVisible.value = true;
  logLoading.value = true;

  logDetailCat(row.id)
    .then((response) => {
      if (response && response) {
        msg.value = response.msg;
      } else {
        msg.value = "暂无日志内容";
      }
    })
    .catch((error) => {
      console.error("获取日志详情失败", error);
      msg.value = "获取日志详情失败";
    })
    .finally(() => {
      logLoading.value = false;
    });
};

// Download log
const downloadLog = (row) => {
  proxy.download(
    "/dpp/etlNodeInstance/downloadLog",
    {
      nodeInstanceId: row.id,
    },
    `${row.name}.log`
  );
};

// Close log dialog
const closeLogDialog = () => {
  logDialogVisible.value = false;
  msg.value = {};
  logTitle.value = "";
};

defineExpose({ currHeight });
</script>
<style lang="scss" scoped>
.console {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  user-select: auto;
  z-index: 999;
  height: 345px;
  display: block;
  //   border-radius: 5px;
  width: 100%;
  max-width: 100%;
  max-height: 720px;
  min-height: 40px;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-left: none;
  border-bottom: none;
  background-color: #fff;

  .move {
    position: absolute;
    user-select: none;
    width: 100%;
    height: 10px;
    top: -5px;
    left: 0px;
    cursor: row-resize;
  }

  .container-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 40px;
    padding: 0 20px;
    border-bottom: 1px solid rgb(147 147 147 / 6%);
    background-color: #f9f9f9;

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
  }

  .container-content {
    height: calc(100% - 40px);
    overflow: hidden;
    background-color: #FFFFFF;
    padding: 15px 20px;
    box-sizing: border-box;

    .console-empty-state {
      width: 100%;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      img {
        width: min(214px, 35%);
        max-height: calc(100% - 28px);
        object-fit: contain;
      }

      p {
        color: #909399;
        margin: 0;
        font-size: 14px;
      }
    }

    .table-empty-state {
      min-height: 210px;
    }

    .console-view {
      width: 100%;
      height: 100%;
      display: flex;
      background-color: #FFFFFF;
      .emptyBg {
        width: 100%;
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        p {
          color: #909399;
          margin-top: 0px;
        }
      }

      :deep(.leftTree) {
        overflow-x: auto;

        .console-tree {
          background-color: #FFFFFF;
        }

        //组织树 背景颜色 及右边线颜色
        .console-tree.el-tree--highlight-current
          .el-tree-node.is-current
          > .el-tree-node__content {
                        background: var(--qt-primary-light) !important;
          border: none;

          .custom-tree-node {
            .label,
            .value {
              color: var(--el-color-primary);
            }
          }
        }

        .el-tree-node__content {
          position: relative;

          .el-tree-node__expand-icon {
            position: absolute;
            right: 10px;
            color: transparent;
            font-size: 12px;

            & > svg {
              background: url("@/assets/modules/da/images/asset/arrow.png") no-repeat;
              background-size: 100% 100%;
            }
          }
        }

        .el-tree-node__content {
          height: 30px !important;
        }

        .custom-tree-node {
          display: flex;
          align-items: center;
          padding: 0 20px;

          .icon {
            color: #63e25c;
          }

          .label {
            margin: 10px;
          }
        }
      }

      .codeEdit {
        height: 100%;
        position: relative;

        .codeEdit-move {
          position: absolute;
          user-select: none;
          width: 10px;
          height: 100%;
          top: 0px;
          left: -5px;
          cursor: col-resize;
          transition: all 0.3s ease;
          border-right: 5px solid rgba(255, 255, 255, 0);
          border-left: 5px solid rgba(255, 255, 255, 0);
          z-index: 1;

          &:hover {
            border-right: 5px solid rgba(0, 0, 0, 0.1);
            border-left: 5px solid rgba(0, 0, 0, 0.1);
          }
        }

        .read-json-editor {
          border-left: 1px solid rgba(0, 0, 0, 0.06);
        }
      }
    }

    .result-view {
      width: 100%;
      height: 100%;
      padding: 0;
      display: flex;
      flex-direction: column;
      box-sizing: border-box;

      .result-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin: 0;
        padding: 0 0 10px 0;
      }

      .result-table {
        flex: 1;
        overflow: auto;
        padding-right: 10px;
      }

      .el-table {
        margin: 0;
      }

      .result-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 0;
        margin-top: 10px;
        border-top: 1px solid #ebeef5;
      }

      .result-empty {
        margin-top: -110px;
      }
    }

    /* 统一按钮样式 - 遵循系统规范 */
    .result-header .el-button,
    .history-header .el-button {
      margin-right: 8px;
    }

    .history-view {
      width: 100%;
      height: 100%;
      position: relative;
      display: flex;
      flex-direction: column;
      padding: 0;

      :deep(.el-table__empty-block) {
        min-height: 230px;
      }

      .history-header {
        padding-bottom: 10px;
        border-bottom: 1px solid #ebeef5;
        background-color: transparent;
      }

      .el-table {
        flex: 1;
        overflow: auto;
        padding-right: 10px;
      }

      .history-pagination {
        padding: 10px 15px;
        border-top: 1px solid #ebeef5;
        background-color: #fafafa;
        display: flex;
        justify-content: flex-end;
        align-items: center;
      }
    }

    .log-detail-content {
      max-height: 500px;
      overflow: auto;
      background-color: #f5f5f5;
      padding: 15px;
      border-radius: 4px;

      pre {
        margin: 0;
        font-family: "Consolas", "Monaco", "Courier New", monospace;
        font-size: 14px;
      }
    }

    .log-loading-container {
      position: relative;
    }

    .log-skeleton {
      font-family: "Consolas", "Monaco", "Courier New", monospace;
      font-size: 14px;
    }

    :deep(.el-skeleton__row .el-skeleton__column) {
      height: 18px;
      margin-bottom: 4px;
    }

    :deep(.el-skeleton__row:last-child .el-skeleton__column) {
      margin-bottom: 0;
    }
  }

  .overflow-guard {
    background-color: red;
  }
}
</style>
