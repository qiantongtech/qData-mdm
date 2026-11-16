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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div ref="targetPage" class="app-container target-detail" v-loading="loading">
    <el-empty v-if="detailError" description="目标系统详情加载失败">
      <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
    </el-empty>
    <template v-else>
      <!-- 详情头部复用项目组件；描述独占第二行，创建人与时间展示在第三行。 -->
      <DetailInfo
          :gutter="2"
          :data="detail"
          mode="free"
          :header="{
          idKey: 'id',
          nameKey: 'name',
          statusKey: 'status',
          statusOptions,
        }"
          :items="detailItems"
      >
      </DetailInfo>

      <!-- 服务授权按原型展示；授权主体关联确定后再接入真实记录。 -->
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab" class="demo-tabs">
          <qt-tab-pane label="服务授权" name="services">
            <qt-wrap class="target-related-wrap" :columns="serviceTable.columns" :tableRef="serviceTableRef"
                     :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <!-- 列表说明沿用项目公共提示组件的默认样式。 -->
                <GuideTipPanel
                    type="plain"
                    title="本页展示当前目标系统关联的服务授权信息，包括授权主体、数据范围、字段权限和有效期，便于了解服务访问权限。"
                />
              </template>
              <qt-table ref="serviceTableRef" v-bind="serviceTable">
                <template #subject="{ row }">
                  <div class="target-authorization-name">{{ row.subjectName }}</div>
                  <dict-tag class="target-related-tag" :options="subjectTypeOptions" :value="row.subjectType"/>
                </template>
                <template #service="{ row }">
                  <div class="target-authorization-name">{{ row.serviceName }}</div>
                  <div class="target-authorization-secondary">{{ row.serviceCode }} · {{ row.version }}</div>
                </template>
                <template #scope="{ row }">
                  <dict-tag class="target-related-tag" :options="scopeTypeOptions" :value="row.scopeType"/>
                  <div v-if="row.scopeSummary" class="target-authorization-secondary">{{ row.scopeSummary }}</div>
                </template>
                <template #fields="{ row }">可见 {{ row.visibleFields }} 个字段</template>
                <template #status="{ row }">
                  <dict-tag class="target-related-tag" :options="authorizationStatusOptions" :value="row.status"/>
                  <div v-if="row.statusRemark" class="target-authorization-secondary">{{ row.statusRemark }}</div>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
          <qt-tab-pane label="关联分发任务" name="tasks">
            <qt-wrap class="target-related-wrap" :columns="taskTable.columns" :tableRef="taskTableRef"
                     :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <!-- 关联任务说明与表格工具按钮共行。 -->
                <GuideTipPanel
                    type="plain"
                    title="本页展示当前目标系统关联的分发任务，包括主数据模型、分发类型、调度周期和执行结果，便于了解任务运行情况。"
                />
              </template>
              <qt-table ref="taskTableRef" v-bind="taskTable">
                <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
                <template #modelName="{ row }">{{ row.modelName || row.modelId || '-' }}</template>
                <template #dataMode="{ row }">
                  <dict-tag v-if="row.dataMode" class="target-related-tag" :options="dataModeOptions"
                            :value="row.dataMode"
                  />
                  <span v-else>-</span>
                </template>
                <template #schedule="{ row }">
                  <div>{{ formatExecutionSchedule(row) }}</div>
                  <div>{{ row.lastRunTime ? proxy.parseTime(row.lastRunTime, '{y}-{m}-{d} {h}:{i}') : '-' }}</div>
                </template>
                <template #status="{ row }">
                  <!-- 只读展示任务上线状态；编辑操作在分发任务页面完成。 -->
                  <dict-tag v-if="row.status" class="target-related-tag" :options="taskStatusOptions"
                            :value="row.status"
                  />
                  <span v-else>-</span>
                </template>
                <template #lastRunStatus="{ row }">{{ runResultText(row) }}</template>
                <template #handle="{ row }">
                  <el-button link type="primary" icon="View" @click="openTaskDetail(row)"
                             v-hasPermi="['dist:task:query']"
                  >查看任务
                  </el-button>
                  <el-button link type="primary" icon="Stopwatch" @click="openTaskInstances(row)"
                             v-hasPermi="['dist:task:query']"
                  >任务实例
                  </el-button>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
          <qt-tab-pane label="执行日志" name="logs">
            <qt-wrap class="target-related-wrap" :columns="logTable.columns" :tableRef="logTableRef"
                     :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <!-- 执行日志说明与表格工具按钮共行。 -->
                <GuideTipPanel
                    type="plain"
                    title="本页展示订阅执行、服务调用和分发任务的日志示例，包括执行内容、执行结果和操作信息，便于了解日志展示方式。"
                />
              </template>
              <qt-table ref="logTableRef" v-bind="logTable">
                <template #executionResult="{ row }">
                  <dict-tag class="target-related-tag" :options="logResultOptions" :value="row.executionResult"/>
                </template>
                <template #handle="{ row }">
                  <el-button link type="primary" icon="View" @click="openExecutionLog(row)">查看日志</el-button>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
        </el-tabs>
      </div>
      <TaskViewDialog v-model:visible="taskDialogVisible" :task-id="selectedTaskId"
                      :target-system-name="detail.name || ''" :append-to="targetPage"
      />
      <RunInstancesDialog v-model:visible="instancesVisible" :data="instancesTask" :append-to="targetPage"/>
      <ExecutionLogViewDialog v-model:visible="logDialogVisible" :log="selectedLog" :append-to="targetPage"
                              @refresh="logTableRef?.resetQuery()"
      />
    </template>
  </div>
</template>

<script setup name="DistTargetSystemDetail">
import {getTargetSystem} from "@/api/dist/target/targetSystem";
import {listTASK} from "@/api/dist/task/TASK";
import DetailInfo from "@/components/DetailInfo/index.vue";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";
import TaskViewDialog from "./TaskViewDialog.vue";
import RunInstancesDialog from "@/views/dist/task/components/RunInstancesDialog.vue";
import ExecutionLogViewDialog from "./ExecutionLogViewDialog.vue";
import {cronToZh} from "@/utils/cronUtils";
import {useRoute} from "vue-router";

const route = useRoute();
const {proxy} = getCurrentInstance();
const targetPage = ref(null);
const loading = ref(false);
const detailError = ref(false);
const detail = ref({});
const activeTab = ref("services");
const serviceTableRef = ref(null);
const taskTableRef = ref(null);
const logTableRef = ref(null);
const taskDialogVisible = ref(false);
const selectedTaskId = ref(null);
const instancesVisible = ref(false);
const instancesTask = ref({});
const logDialogVisible = ref(false);
const selectedLog = ref({});

const statusOptions = [
  {label: "启用", value: "1", elTagType: "success"},
  {label: "禁用", value: "2", elTagType: "danger"},
];

/** 复用分发任务列表的 DictTag 展示规则：全量黄色，增量绿色。 */
const dataModeOptions = [
  {value: "FULL", label: "全量", elTagType: "warning"},
  {value: "INCREMENTAL", label: "增量", elTagType: "success"},
];

/** 任务状态标签：已上线绿色，未上线蓝色，与分发任务详情一致。 */
const taskStatusOptions = [
  {value: "ENABLED", label: "已上线", elTagType: "success"},
  {value: "DISABLED", label: "未上线", elTagType: "primary"},
];

/** 执行结果沿用项目 DictTag 的成功、失败配色。 */
const logResultOptions = [
  {value: "成功", label: "成功", elTagType: "success"},
  {value: "失败", label: "失败", elTagType: "danger"},
];

/** 服务授权表格沿用授权管理中的主体、范围和状态标签文案。 */
const subjectTypeOptions = [
  {value: "client", label: "服务客户端", elTagType: "info"},
  {value: "user", label: "平台用户", elTagType: "primary"},
];
const scopeTypeOptions = [
  {value: "all", label: "全部数据", elTagType: "success"},
  {value: "condition", label: "条件范围", elTagType: "warning"},
];
const authorizationStatusOptions = [
  {value: "active", label: "有效", elTagType: "success"},
  {value: "pending", label: "未生效", elTagType: "warning"},
];

/** 三个关联表格使用相同的分页规格和滚动行为。 */
const relatedPagination = {pageSizes: [10, 20, 50, 100], notAutoScroll: true};

/** 按最新原型排布三行：基础字段、描述、创建人与时间。 */
const detailItems = [
  {label: "目标系统编码", key: "code"},
  {label: "分发任务", key: "taskCount"},
  {
    label: "负责人",
    key: "contactName",
    formatter: (name, data) => [name, data.contactInfo].filter(Boolean).join(" / ") || "-"
  },
  {label: "描述", key: "description", span: 24, ellipsisClass: "ellipsis-2", className: "mt2 mb2"},
  {label: "创建人", key: "createBy", formatter: (name, data) => [name, data.creatorPhone].filter(Boolean).join(" / ")},
  {label: "创建时间", key: "createTime", type: "time"},
  {label: "最近更新时间", key: "updateTime", type: "time"},
];

/** 最近执行结果读取最新执行实例；未产生实例时不展示虚构的成功数。 */
const runResultText = row => {
  if (!row.lastRunStatus) return "暂无执行记录";
  if (row.lastRunStatus === "RUNNING") return "执行中";
  if (row.lastSuccessCount != null || row.lastFailureCount != null) {
    return `成功 ${row.lastSuccessCount ?? 0} / 失败 ${row.lastFailureCount ?? 0}`;
  }
  return ({
    SUCCESS: "成功",
    FAILED: "失败",
    FAILURE: "失败",
    RUNNING: "执行中"
  })[row.lastRunStatus] || row.lastRunStatus;
};

/** 调度周期与分发任务列表使用相同的 Cron 文案，下一行展示最近执行时间。 */
const formatExecutionSchedule = row => {
  if (row.triggerType === "MANUAL") return "仅人工执行";
  if (row.triggerType === "SCHEDULED") return cronToZh(row.cronExpression) || "-";
  return row.triggerType || "-";
};

/** 在目标系统详情中打开只读任务弹窗，使用现有任务详情接口加载字段。 */
function openTaskDetail(row) {
  selectedTaskId.value = row.id;
  taskDialogVisible.value = true;
}

/** 从目标系统的关联分发任务直接查看该任务的真实执行实例。 */
function openTaskInstances(row) {
  instancesTask.value = {id: row.id, name: row.name};
  instancesVisible.value = true;
}

/** 点击执行日志行时展示该行已有字段。 */
function openExecutionLog(row) {
  selectedLog.value = {...row};
  logDialogVisible.value = true;
}

/** 授权主体关联暂未接入，页面记录用于展示服务授权列表布局。 */
const serviceAuthorizationRows = [
  {
    subjectName: "SRM-PROD",
    subjectType: "client",
    serviceName: "供应商变更订阅服务",
    serviceCode: "API_SUPPLIER_CHANGE",
    version: "v1.3",
    scopeType: "all",
    visibleFields: 8,
    validPeriod: "长期有效",
    status: "active"
  },
  {
    subjectName: "SRM-PROD",
    subjectType: "client",
    serviceName: "供应商主数据查询服务",
    serviceCode: "API_SUPPLIER_QUERY",
    version: "v2.1",
    scopeType: "all",
    visibleFields: 8,
    validPeriod: "长期有效",
    status: "active"
  },
].map((row, index) => ({...row, id: `local-${index + 1}`, rowNumber: index + 1}));

/** 按编号对全部页面记录排序后分页，取消排序时恢复原始顺序。 */
function loadServiceAuthorizations(params = {}) {
  const pageNum = Number(params.pageNum) || 1;
  const pageSize = Number(params.pageSize) || 10;
  const start = (pageNum - 1) * pageSize;
  let rows = serviceAuthorizationRows;
  if (params.isAsc && params.orderByColumn === "rowNumber") {
    const direction = params.isAsc === "ascending" ? 1 : -1;
    rows = [...serviceAuthorizationRows].sort((a, b) => direction * (a.rowNumber - b.rowNumber));
  }
  return Promise.resolve({data: {rows: rows.slice(start, start + pageSize), total: rows.length}});
}

/** 与授权管理列表使用相同的授权主体、服务和权限字段。 */
const serviceTable = reactive({
  func: loadServiceAuthorizations,
  config: {
    table: {rowKey: "id", class: "target-related-table", stripe: true},
    pagination: relatedPagination,
  },
  columns: [
    {label: "编号", prop: "rowNumber", minWidth: 70, align: "center", sortable: "custom"},
    {label: "授权主体", prop: "subjectName", slot: "subject", minWidth: 180, align: "left", headerAlign: "left"},
    {label: "数据服务 / API", prop: "serviceName", slot: "service", minWidth: 260, align: "left", headerAlign: "left"},
    {label: "数据范围", prop: "scopeType", slot: "scope", minWidth: 170, align: "left", headerAlign: "left"},
    {label: "字段权限", prop: "visibleFields", slot: "fields", minWidth: 150, align: "left", headerAlign: "left"},
    {label: "有效期", prop: "validPeriod", minWidth: 200, align: "left", headerAlign: "left"},
    {label: "授权状态", prop: "status", slot: "status", minWidth: 120, align: "left", headerAlign: "left"},
  ],
});

/** 执行日志原型示例；订阅、调用和分发记录均为前端静态数据。 */
const executionLogSamples = [
  {
    businessType: "订阅执行",
    objectName: "供应商主数据订阅",
    executionContent: "执行增量拉取",
    executionResult: "成功",
    executionTime: "2026-09-29 15:20:01",
    operatorName: "吴同"
  },
  {
    businessType: "服务调用",
    objectName: "supplier_master_api",
    executionContent: "查询供应商主数据",
    executionResult: "失败",
    executionTime: "2026-09-29 14:56:13",
    operatorName: "张华"
  },
  {
    businessType: "分发任务",
    objectName: "供应商主数据分发",
    executionContent: "执行全量分发",
    executionResult: "失败",
    executionTime: "2026-09-29 14:21:38",
    operatorName: "李四"
  },
  {
    businessType: "服务调用",
    objectName: "customer_master_api",
    executionContent: "条件查询客户主数据",
    executionResult: "成功",
    executionTime: "2026-09-29 13:48:02",
    operatorName: "系统"
  },
  {
    businessType: "订阅执行",
    objectName: "material_subscribe",
    executionContent: "执行增量拉取",
    executionResult: "成功",
    executionTime: "2026-09-29 13:15:44",
    operatorName: "吴同"
  },
  {
    businessType: "服务调用",
    objectName: "emp_master_api",
    executionContent: "条件查询员工主数据",
    executionResult: "成功",
    executionTime: "2026-09-29 12:40:22",
    operatorName: "张华"
  },
  {
    businessType: "分发任务",
    objectName: "warehouse_master_api",
    executionContent: "执行仓库主数据分发",
    executionResult: "成功",
    executionTime: "2026-09-29 10:26:53",
    operatorName: "李四"
  },
  {
    businessType: "服务调用",
    objectName: "region_master_api",
    executionContent: "查询区域主数据",
    executionResult: "成功",
    executionTime: "2026-09-29 09:17:31",
    operatorName: "系统"
  },
  {
    businessType: "订阅执行",
    objectName: "finance_org_subscribe",
    executionContent: "执行增量拉取",
    executionResult: "失败",
    executionTime: "2026-09-29 09:17:31",
    operatorName: "吴同"
  },
  {
    businessType: "服务调用",
    objectName: "dept_master_api",
    executionContent: "查询部门主数据",
    executionResult: "成功",
    executionTime: "2026-09-29 09:17:31",
    operatorName: "张华"
  },
].map((row, index) => ({id: index + 1, rowNumber: index + 1, ...row}));

/** 执行日志示例支持按编号、执行时间排序；取消排序时恢复原型顺序，再按页截取。 */
function loadExecutionLogs(params = {}) {
  const pageNum = Number(params.pageNum) || 1;
  const pageSize = Number(params.pageSize) || 10;
  const start = (pageNum - 1) * pageSize;
  // 时间字段统一为 YYYY-MM-DD HH:mm:ss，直接比较字符串即等于比较时间。
  const comparators = {
    rowNumber: (a, b) => a.rowNumber - b.rowNumber,
    executionTime: (a, b) => a.executionTime.localeCompare(b.executionTime),
  };
  const compare = comparators[params.orderByColumn];
  let rows = executionLogSamples;
  // el-table 取消排序时 isAsc 为 null，此时回到原型默认顺序。
  if (params.isAsc && compare) {
    const direction = params.isAsc === "ascending" ? 1 : -1;
    rows = [...executionLogSamples].sort((a, b) => direction * compare(a, b));
  }
  return Promise.resolve({data: {rows: rows.slice(start, start + pageSize), total: rows.length}});
}

/** 执行日志按原型展示前端示例，并提供与项目一致的查看日志操作。 */
const logTable = reactive({
  func: loadExecutionLogs,
  config: {
    table: {rowKey: "id", class: "target-related-table", stripe: true, emptyText: "暂无执行日志", defaultSort: {prop: "executionTime", order: "descending"}},
    sort: true,
    pagination: relatedPagination,
  },
  columns: [
    {label: "编号", prop: "rowNumber", minWidth: 80, align: "center", sortable: "custom"},
    {label: "业务类型", prop: "businessType", minWidth: 150, align: "left", headerAlign: "left"},
    {label: "对象名称", prop: "objectName", minWidth: 180, align: "left", headerAlign: "left"},
    {label: "执行内容", prop: "executionContent", minWidth: 180, align: "left", headerAlign: "left"},
    {label: "执行结果", prop: "executionResult", minWidth: 130, align: "center", slot: "executionResult"},
    {label: "操作人", prop: "operatorName", minWidth: 130, align: "left", headerAlign: "left"},
    {label: "执行时间", prop: "executionTime", minWidth: 170, align: "center", sortable: "custom"},
    {
      label: "操作",
      minWidth: 140,
      align: "center",
      headerAlign: "center",
      fixed: "right",
      slot: "handle"
    },
  ],
});

/** 切到关联任务时重新读取当前目标系统的数据，兼容非活动页签延迟渲染。 */
watch(activeTab, (tab) => {
  if (tab === "tasks" && detail.value.id) nextTick(() => taskTableRef.value?.resetQuery());
});

/** 分页查询当前目标系统关联的任务，不拉取其他系统任务后在前端过滤。 */
const taskTable = reactive({
  func: params => listTASK({...params, targetSystemId: detail.value.id}),
  params: {},
  events: {
    // 编号排序交给后端按任务 ID 处理，保证跨页顺序一致。
    formatParams: params => {
      if (!params.isAsc) {
        const requestParams = {...params};
        delete requestParams.orderByColumn;
        delete requestParams.isAsc;
        return requestParams;
      }
      return {
        ...params,
        orderByColumn: params.orderByColumn === "rowNumber" ? "id" : params.orderByColumn,
        isAsc: ["ascending", "asc"].includes(params.isAsc) ? "asc" : "desc",
      };
    },
    // 编号按当前页连续生成，避免将数据库主键误显示为原型中的序号。
    formatData: (rows, params) => rows.map((row, index) => ({
      ...row,
      rowNumber: (params.pageNum - 1) * params.pageSize + index + 1,
    })),
  },
  config: {
    initResquest: false,
    table: {rowKey: "id", class: "target-related-table", stripe: true, emptyText: "暂无关联分发任务"},
    pagination: relatedPagination,
    sort: true,
  },
  columns: [
    // 编号列固定窄宽度，避免序号在宽列中显得偏右；仍支持手动排序。
    {
      label: "编号",
      prop: "rowNumber",
      width: 80,
      align: "center",
      headerAlign: "center",
      slot: "rowNumber",
      sortable: "custom",
      sortableKey: "id"
    },
    {label: "主数据模型", prop: "modelName", minWidth: 140, align: "left", headerAlign: "left", slot: "modelName"},
    {label: "任务名称", prop: "name", minWidth: 140, align: "left", headerAlign: "left"},
    {label: "分发类型", prop: "dataMode", minWidth: 140, align: "center", headerAlign: "center", slot: "dataMode"},
    {
      label: "调度周期/最近执行时间",
      prop: "triggerType",
      minWidth: 140,
      align: "left",
      headerAlign: "left",
      slot: "schedule"
    },
    {label: "任务状态", prop: "status", minWidth: 140, align: "center", headerAlign: "center", slot: "status"},
    {
      label: "最近执行结果",
      prop: "lastRunStatus",
      minWidth: 140,
      align: "left",
      headerAlign: "left",
      slot: "lastRunStatus"
    },
    {
      label: "操作",
      minWidth: 210,
      align: "center",
      headerAlign: "center",
      fixed: "right",
      slot: "handle"
    },
  ],
});

/** 按路由中的目标系统 ID 查询详情；不使用默认 ID，避免显示其他记录。 */
function loadDetail() {
  taskDialogVisible.value = false;
  selectedTaskId.value = null;
  instancesVisible.value = false;
  instancesTask.value = {};
  logDialogVisible.value = false;
  selectedLog.value = {};
  const id = route.query.id;
  if (!id) {
    detail.value = {};
    detailError.value = true;
    return;
  }
  detailError.value = false;
  loading.value = true;
  detail.value = {};
  getTargetSystem(id)
      .then((response) => {
        detail.value = response.data || {};
        // 关联任务页签激活时再查询，避免默认展示服务授权时发起无关请求。
        if (detail.value.id && activeTab.value === "tasks") nextTick(() => taskTableRef.value?.resetQuery());
      })
      .catch(() => {
        detailError.value = true;
      })
      .finally(() => {
        loading.value = false;
      });
}

watch(() => route.query.id, loadDetail, {immediate: true});
</script>

<style scoped>
/* 与分发任务详情一致，底部页签区域填充剩余空间。 */
.target-detail {
  display: flex;
  flex-direction: column;
}

.pagecont-bottom {
  flex: 1;
  min-height: 0 !important;
}


/* 各页签的说明与右侧工具按钮共行，空间不足时说明文字可换行。 */
.target-related-wrap :deep(.qt-wrap--actions) {
  gap: var(--qt-space-4);
}

.target-related-wrap :deep(.data-actions) {
  flex: 1;
  min-width: 0;
}

/* 标签文字与项目表格正文保持相同的字号和字重。 */
.target-related-tag :deep(.el-tag) {
  font-size: var(--qt-font-size-base);
  font-weight: var(--qt-font-weight-regular);
}

/* 授权主体和服务编码沿用授权管理列表的主次信息排布。 */
.target-authorization-name {
  color: var(--qt-text-primary);
  font-size: var(--qt-font-size-base);
}

.target-authorization-secondary {
  margin-top: var(--qt-space-1);
  color: var(--qt-text-secondary);
  font-size: var(--qt-font-size-small);
  line-height: 18px;
}

/* 三个关联表格统一表头换行规则，避免长字段覆盖相邻列。 */
:deep(.target-related-table .el-table th .cell) {
  white-space: normal !important;
  overflow: hidden !important;
}
</style>
