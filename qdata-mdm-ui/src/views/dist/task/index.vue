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
  <div class="app-container" ref="app-container">
    <GuideTip tip-id="dist/task.list" />
    <el-container>
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :editable="false"
        :default-expand="false"
        :default-expanded-keys="defaultExpandedKeys"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      />
      <el-main class="main-content">
    <!-- 与目标系统列表一致，复用 QtWrap 管理搜索区和列表工具栏。 -->
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar v-bind="searchStore" :params="queryParams" :tableRef="searchTableRef" />
      </template>
      <template #actions-data>
        <!-- 任务列表只提供新增和批量删除；批量删除依赖表格勾选状态。 -->
        <el-button type="primary" plain @click="handleAdd" v-hasPermi="['dist:task:add']"
                   @mousedown="(e) => e.preventDefault()"
>
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
                   v-hasPermi="['dist:task:remove']"
>批量删除</el-button>
      </template>
      <!-- QtTable 按原型展示任务摘要，未提供的数据以空值展示。 -->
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
        <template #taskInfo="{ row }">
          <div class="qt-table-info task-info">
            <div class="qt-table-info__main">
              <span v-if="row.name && proxy.$auth.hasPermi('dist:task:query')"
                    class="info-title task-info-title text-ellipsis" role="link" tabindex="0"
                    @click="handleDetail(row)" @keydown.enter="handleDetail(row)"
                    @keydown.space.prevent="handleDetail(row)">{{ row.name }}</span>
              <span v-else class="info-title text-ellipsis">{{ row.name || '-' }}</span>
            </div>
            <div class="info-desc text-ellipsis">{{ row.remark || '-' }}</div>
          </div>
        </template>
        <template #location="{ row }">
          <div v-if="row.projectName || row.layerName" class="flex-column flex-column-gap">
            <div class="qt-table-info__main">
              <img :src="getProjectIcon(row.projectIcon)" alt="" />
              <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
            </div>
            <div v-if="row.layerName" class="text-ellipsis flex-center">
              <dict-tag size="small" type="primary" :value="row.layerName" />
            </div>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
        <template #modelName="{ row }">
          <div v-if="row.modelName || row.modelId" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(modelDetails.get(String(row.modelId)) || {})" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="modelTitle(row)">{{ modelTitle(row) }}</span>
                <dict-tag v-if="modelDetails.get(String(row.modelId))?.type" :options="MODEL_TYPE_OPTIONS" :value="modelDetails.get(String(row.modelId)).type" size="small" type="primary" /> 
              <dict-tag v-if="modelVersion(row)" :value="modelVersion(row)" size="small" type="info" />
            </div>
            <div class="info-desc text-ellipsis" :title="modelDetails.get(String(row.modelId))?.description || '-'">{{ modelDetails.get(String(row.modelId))?.description || '-' }}</div>
          </div>
          <span v-else>-</span>
        </template>
        <template #targetName="{ row }">
          <div class="task-icon-text">
              <img v-if="row.targetSystemName || targetNames[row.targetSystemId]" :src="getProjectIcon(row.projectIcon)" alt="" class="task-list-icon" />
            <span class="text-ellipsis" :title="targetDisplayName(row)">{{ targetDisplayName(row) }}</span>
          </div>
        </template>
        <template #distribution="{ row }">
          <div class="task-execution-info">
            <div><dict-tag v-if="row.dataMode" class="task-data-mode-tag" :options="dataModeOptions" :value="row.dataMode" /><span v-else>-</span></div>
            <div class="task-secondary text-ellipsis" :title="row.apiUrl || ''">{{ row.apiUrl || '-' }}</div>
          </div>
        </template>
        <template #lastExecution="{ row }">
          <div class="task-execution-info">
            <div>
              <StatusTag v-if="row.lastRunStatus" size="small" :status="executionStatus(row.lastRunStatus)" />
              <span v-else>-</span>
            </div>
            <div class="last-execute-info">
              <span class="mr10" :title="row.lastRunTime ? proxy.parseTime(row.lastRunTime, '{y}-{m}-{d} {h}:{i}:{s}') : ''">{{ row.lastRunTime ? timeAgo(row.lastRunTime) : '-' }}</span>
              <span>{{ formatDuration(row.lastRunDurationMillis) }}</span>
            </div>
          </div>
        </template>
        <template #schedule="{ row }">
          <div class="flex-column flex-column-gap">
            <div>{{ row.triggerType === 'MANUAL' ? '人工执行' : row.triggerType === 'SCHEDULED' ? '定时执行' : '-' }}</div>
            <div v-if="row.triggerType === 'SCHEDULED'" class="flex-center" :title="formatExecutionSchedule(row)">
              <el-icon class="mr5"><Clock /></el-icon><span class="text-ellipsis">{{ formatExecutionSchedule(row) }}</span>
            </div>
          </div>
        </template>
        <template #status="{ row }">
          <div class="task-status-switches">
            <div class="task-status-switches__item">
              <span class="task-status-switches__label">任务状态:</span>
              <el-switch v-if="['ENABLED', 'DISABLED'].includes(row.status)" v-model="row.status"
                         active-value="ENABLED" inactive-value="DISABLED" :loading="statusLoading[row.id]"
                         :disabled="!proxy.$auth.hasPermi('dist:task:edit') || statusLoading[row.id]"
                         @change="handleTaskStatusChange(row, $event)"
/>
              <span v-else>{{ row.status || '-' }}</span>
            </div>
            <div class="task-status-switches__item">
              <span class="task-status-switches__label">调度状态:</span>
              <el-switch v-model="row.schedulerStatus" active-value="ENABLED" inactive-value="DISABLED"
                         :disabled="row.status !== 'ENABLED'" />
            </div>
          </div>
        </template>
        <template #responsible="{ row }">
          <div>{{ row.responsibleUserName || '-' }}</div>
          <div v-if="row.responsiblePhone" class="task-secondary">{{ row.responsiblePhone }}</div>
        </template>
        <template #creator="{ row }">
          <div>{{ row.createBy || '-' }}</div>
          <div v-if="row.creatorPhone" class="task-secondary">{{ row.creatorPhone }}</div>
        </template>
        <template #handle="{ row }">
          <div class="task-action-toolbar">
            <el-button link type="primary" icon="View" @click="handleDetail(row)"
                       v-hasPermi="['dist:task:query']"
>详情</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)"
                       v-hasPermi="['dist:task:edit']"
>修改</el-button>
            <el-popover v-if="proxy.$auth.hasPermiOr(['dist:task:edit', 'dist:task:query', 'dist:task:add', 'dist:task:remove'])"
                        placement="bottom" :width="150" trigger="click" popper-class="dist-task-action-popover"
>
              <template #reference><el-button link type="primary" icon="ArrowDown">更多</el-button></template>
              <div class="butgdlist task-action-list">
                <el-button link type="primary" icon="VideoPlay"
                           :disabled="row.status !== 'ENABLED' || !!executeLoading[row.id]"
                           :title="row.status !== 'ENABLED' ? '未上线任务不可执行' : ''"
                           :loading="!!executeLoading[row.id]" @click="runTask(row)" v-hasPermi="['dist:task:edit']"
>执行一次</el-button>
                <el-button link type="primary" icon="Stopwatch" @click="openRuns(row)"
                           v-hasPermi="['dist:task:query']"
>任务实例</el-button>
                <el-button link type="primary" icon="CopyDocument" :loading="!!cloneLoading[row.id]"
                           :disabled="!!cloneLoading[row.id]"
                           @click="cloneTask(row)" v-hasPermi="['dist:task:add']"
>克隆任务</el-button>
                <el-button link type="danger" icon="Delete" @click="handleDelete(row)"
                           v-hasPermi="['dist:task:remove']"
>删除任务</el-button>
              </div>
            </el-popover>
          </div>
        </template>
      </qt-table>
    </qt-wrap>
      </el-main>
    </el-container>

    <RunInstancesDialog :visible="runsVisible" :data="runsTask" :append-to="$refs['app-container']"
                        @update:visible="runsVisible = $event"
/>

  </div>
</template>

<script setup name="TASK">
  import { listTASK, delTASK, updateTASK, runTASK, cloneTASK } from "@/api/dist/task/TASK";
  import { listBase } from "@/api/model/base/base";
  import { listTargetSystem } from "@/api/dist/target/targetSystem";
  import { cronToZh } from "@/utils/cronUtils";
  import { Clock } from "@element-plus/icons-vue";
  import { getProjectIcon, getModelIcon } from "@/utils/imageUrl";
  import { MODEL_TYPE_OPTIONS } from "@/utils/model/modelType";
  import QtTree from "@/components/QtTree/index.vue";
  import StatusTag from '@/components/StatusTag/index.vue';
  import { timeAgo } from '@/utils/time';


  function formatDuration(value) {
    if (value == null) return '-';
    return `${(value / 1000).toFixed(1)} 秒`;
  }
  import RunInstancesDialog from "./components/RunInstancesDialog.vue";
  import { usePageRefresh } from '@/composables/usePageRefresh';
  import { usePublishedModelTree } from '@/composables/usePublishedModelTree';


  const { proxy } = getCurrentInstance();

  const tableRef = ref(null);
  const modelTreeRef = ref(null);
  const { data: modelTreeData, loading: modelTreeLoading } = usePublishedModelTree(addTreeKeys);
  const defaultExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey));
  const modelOptions = ref([]);
  const models = ref([]);
  const modelDetails = computed(() => new Map(models.value.map(model => [String(model.id), model])));
  const modelTitle = row => {
    const model = modelDetails.value.get(String(row.modelId));
    const name = model?.name || row.modelName || String(row.modelId);
    return model?.code ? `${name} (${model.code})` : name;
  };
  const modelVersion = row => {
    const model = modelDetails.value.get(String(row.modelId));
    return model?.releaseVersion?.versionNo || model?.version?.versionNo || model?.versionNo;
  };
  const targetNames = ref({});
  const statusLoading = reactive({});
  const executeLoading = reactive({});
  const cloneLoading = reactive({});
  // 搜索栏查询或重置时回到第一页；QtWrap 刷新则保留当前页。
  const searchTableRef = {
    getList: () => tableRef.value?.resetQuery(),
    resetQuery: () => tableRef.value?.resetQuery()
  };
  const runsVisible = ref(false);
  const runsTask = ref({});
  const ids = ref([]);
  const multiple = ref(true);
  const dataModeOptions = [
    { value: 'FULL', label: '全量', elTagType: 'warning' },
    { value: 'INCREMENTAL', label: '增量', elTagType: 'success' }
  ];
  const data = reactive({
    queryParams: {
      name: null,
      code: null,
      targetSystemName: null,
      modelId: null,
      dataMode: null,
      triggerType: null,
      status: null,
      lastRunStatus: null,
    }
  });

  const { queryParams } = toRefs(data);

  // 域、目录和模型的原始 ID 可能相同，组合节点类型以避免树节点冲突。
  function addTreeKeys(nodes = []) {
    return nodes.map(node => ({
      ...node,
      treeKey: `${node.nodeType}-${node.id}`,
      children: addTreeKeys(node.children || [])
    }));
  }

  function findModelNode(nodes, modelId) {
    for (const node of nodes || []) {
      if (node.nodeType === 'model' && String(node.modelId) === String(modelId)) return node;
      const found = findModelNode(node.children, modelId);
      if (found) return found;
    }
    return null;
  }

  function syncTreeSelection(modelId, expandParent = true) {
    const node = modelId == null || modelId === '' ? null : findModelNode(modelTreeData.value, modelId);
    if (node && !modelOptions.value.some(option => String(option.value) === String(node.modelId))) {
      modelOptions.value.push({ label: node.name, value: node.modelId });
    }
    modelTreeRef.value?.setCurrentKey(node?.treeKey ?? null, expandParent);
  }

  function handleModelTreeClick(node) {
    if (node.nodeType !== 'model' || node.modelId == null) {
      syncTreeSelection(queryParams.value.modelId, false);
      return;
    }
    if (!modelOptions.value.some(option => String(option.value) === String(node.modelId))) {
      modelOptions.value.push({ label: node.name, value: node.modelId });
    }
    queryParams.value.modelId = node.modelId;
    tableRef.value?.resetQuery();
  }

  watch([() => queryParams.value.modelId, modelTreeData], async () => {
    await nextTick();
    syncTreeSelection(queryParams.value.modelId);
  }, { flush: 'post' });

  // 列顺序与分发任务原型一致，QtTable 负责分页、勾选和列显隐。
  const tableStore = reactive({
    config: {
      table: {
        stripe: true,
        rowKey: "id",
        onSelectionChange: handleSelectionChange,
        defaultSort: { prop: "createTime", order: "descending" }
      },
      sort: true
    },
    columns: [
      { type: "selection", width: 55 },
      { label: "编号", prop: "rowNumber", width: 65, align: "center", slot: "rowNumber", sortable: "custom", sortableKey: "id" },
        {
            label: "分发任务信息",
            prop: "name",
            minWidth: 220,
            align: "left",
            headerAlign: "left",
            info: {
                title: "name",
                desc: "remark",
                get click() {
                    return proxy.$auth.hasPermi('dist:task:query')
                        ? row => row.name && handleDetail(row)
                        : undefined;
                }
            }
        },

      { label: "归属位置", prop: "projectName", minWidth: 160, align: "left", headerAlign: "left", slot: "location" },
      { label: "分发类型 / 接收接口", prop: "dataMode", minWidth: 200, align: "left", headerAlign: "left", slot: "distribution" },
      { label: "最近执行信息", prop: "lastRunTime", minWidth: 255, align: "left", headerAlign: "left", slot: "lastExecution", className: "last-execution-column", labelClassName: "last-execution-column" },
      { label: "运行控制", prop: "status", minWidth: 165, align: "left", headerAlign: "left", slot: "status" },
      { label: "调度类型 / 周期", prop: "triggerType", minWidth: 200, align: "left", headerAlign: "left", slot: "schedule" },
      { label: "负责人", prop: "responsibleUserName", minWidth: 140, align: "left", headerAlign: "left", slot: "responsible" },
      { label: "创建人", prop: "createBy", minWidth: 115, align: "left", headerAlign: "left", slot: "creator" },
      { label: "创建时间", prop: "createTime", width: 175, align: "center", date: "{y}-{m}-{d} {h}:{i}", sortable: "custom", sortableKey: "create_time" },
      { label: "操作", width: 240, align: "center", fixed: "right", slot: "handle" }
    ],
    func: listTASK,
    params: queryParams.value,
    events: {
      // 表格的排序值转换为分发任务接口使用的数据库字段和方向。
      formatParams: params => ({
        ...params,
        orderByColumn: params.orderByColumn === "createTime" ? "create_time" : params.orderByColumn,
        isAsc: params.isAsc === "ascending" ? "asc" : params.isAsc === "descending" ? "desc" : params.isAsc
      }),
      // 编号随分页连续显示，不使用数据库主键。
      formatData: (rows, params) => rows.map((row, index) => ({
        ...row,
        schedulerStatus: 'DISABLED',
        rowNumber: (params.pageNum - 1) * params.pageSize + index + 1
      }))
    }
  });

  const searchStore = reactive({
    items: [
      { label: "分发任务名称", prop: "name", component: { is: "input", placeholder: "请输入分发任务名称" } },
      { label: "分发任务编码", prop: "code", component: { is: "input", placeholder: "请输入分发任务编码" } },
      { label: "目标系统名称", prop: "targetSystemName", component: { is: "input", placeholder: "请输入目标系统名称" } },
      { label: "主数据模型", prop: "modelId", component: { is: "select", placeholder: "请选择主数据模型", options: modelOptions } },
      { label: "分发类型", prop: "dataMode", component: { is: "select", placeholder: "请选择分发类型", options: [
        { label: "全量", value: "FULL" }, { label: "增量", value: "INCREMENTAL" }
      ] } },
      { label: "调度类型", prop: "triggerType", component: { is: "select", placeholder: "请选择调度类型", options: [
        { label: "人工", value: "MANUAL" }, { label: "定时", value: "SCHEDULED" }
      ] } },
      { label: "任务状态", prop: "status", component: { is: "select", placeholder: "请选择任务状态", options: [
        { label: "启用", value: "ENABLED" }, { label: "停用", value: "DISABLED" }
      ] } },
      { label: "最近执行状态", prop: "lastRunStatus", component: { is: "select", placeholder: "请选择最近执行状态", options: [
        { label: "运行中", value: "RUNNING" }, { label: "成功", value: "SUCCESS" }, { label: "失败", value: "FAILED" }
      ] } }
    ]
  });

  /** 与 Pro2 任务列表一致，优先展示可读的调度周期。 */
  function formatExecutionSchedule(row) {
    if (row.triggerType === 'MANUAL') return '仅人工执行';
    if (row.triggerType === 'SCHEDULED') return cronToZh(row.cronExpression) || '-';
    return row.triggerType || '-';
  }

  function executionStatus(status) {
    const normalized = String(status || '').toUpperCase();
    if (normalized === 'FAILURE') return 'failed';
    return ['RUNNING', 'SUCCESS', 'FAILED'].includes(normalized) ? normalized.toLowerCase() : 'idle';
  }

  /** 优先展示分页接口返回的目标系统名称，缺失时使用已加载的名称缓存。 */
  function targetDisplayName(row) {
    return row.targetSystemName || targetNames.value[row.targetSystemId] || row.targetSystemId || '-';
  }

  /** 读取目标系统名称缓存，供历史任务中缺少关联名称时回显。 */
  async function loadTargetNames() {
    try {
      const response = await listTargetSystem({ pageNum: 1, pageSize: 1000 });
      targetNames.value = Object.fromEntries((response.data?.rows || []).map(item => [item.id, item.name]));
    } catch {
      targetNames.value = {};
    }
  }

  /** 模型下拉项只用于搜索；列表的模型和目标系统名称由分页接口补齐。 */
  async function loadModelOptions() {
    try {
      const response = await listBase({ pageNum: 1, pageSize: 1000 });
      models.value = response.data?.rows || [];
      modelOptions.value = [
        { label: '供应商主数据', value: -1 },
        ...models.value.map(model => ({ label: model.name, value: model.id }))
      ];
    } catch {
      // 模型选项请求失败不影响任务列表展示，列表名称由分页接口提供。
      modelOptions.value = [{ label: '供应商主数据', value: -1 }];
    }
    syncTreeSelection(queryParams.value.modelId, false);
  }

  /** 修改或删除成功后刷新 QtTable 当前页。 */
  function getList() {
    tableRef.value?.getList();
  }

  // 从新增或修改整页返回时刷新列表，保持项目接入任务的页面间刷新方式。
  usePageRefresh('distTask', () => tableRef.value?.resetQuery());

  /**
   * 更新所选分发任务 ID，并同步批量删除按钮的禁用状态。
   * @param {Array<Object>} selection 当前选中的分发任务记录。
   */
  function handleSelectionChange(selection) {
    ids.value = selection.map(item => item.id);
    multiple.value = !selection.length;
  }

  /**
   * 确认并保存任务启停状态；取消或保存失败时恢复原状态。
   * @param {Object} row 当前分发任务。
   * @param {string} status 目标任务状态。
   */
  async function handleTaskStatusChange(row, status) {
    const previousStatus = status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
    const actionText = status === 'ENABLED' ? '启用' : '停用';
    try {
      await proxy.$modal.confirm(`是否确认${actionText}分发任务“${row.name || row.id}”？`);
      statusLoading[row.id] = true;
      await updateTASK({ id: row.id, status });
      if (status === 'DISABLED') row.schedulerStatus = 'DISABLED';
      proxy.$modal.msgSuccess('操作成功');
      getList();
    } catch {
      row.status = previousStatus;
    } finally {
      statusLoading[row.id] = false;
    }
  }

  /** 跳转到新增分发任务向导。 */
  function handleAdd() {
    proxy.$router.push('/dist/task/add');
  }

  /**
   * 打开指定分发任务的编辑向导。
   * @param {Object} row 当前分发任务记录。
   */
  function handleUpdate(row) {
    proxy.$router.push({ path: '/dist/task/edit', query: { id: row.id } });
  }

  /**
   * 确认后手动执行指定任务，并刷新列表显示最新执行结果。
   * @param {Object} row 当前分发任务记录。
   */
  function runTask(row) {
    if (row.status !== 'ENABLED' || executeLoading[row.id]) return;
    proxy.$modal.confirm(`确认执行分发任务“${row.name}”一次？系统将按当前配置向目标 API 推送数据。`)
      .then(async () => {
        executeLoading[row.id] = true;
        try {
          await runTASK(row.id);
          proxy.$modal.msgSuccess('执行成功');
          getList();
        } finally {
          executeLoading[row.id] = false;
        }
      }).catch(() => {});
  }

  /**
   * 打开指定分发任务的运行实例列表。
   * @param {Object} row 当前分发任务记录。
   */
  function openRuns(row) {
    runsTask.value = row;
    runsVisible.value = true;
  }

  /**
   * 确认后调用克隆接口，并刷新列表显示新任务。
   * @param {Object} row 待克隆的分发任务记录。
   */
  async function cloneTask(row) {
    if (cloneLoading[row.id]) return;
    try {
      await proxy.$modal.confirm(`确定要克隆任务【${row.name}】吗？`);
      cloneLoading[row.id] = true;
      await cloneTASK(row.id);
      tableRef.value?.resetQuery();
      proxy.$modal.msgSuccess('克隆成功');
    } catch {
      // 用户取消或请求失败时不改动当前任务列表。
    } finally {
      cloneLoading[row.id] = false;
    }
  }

  /**
   * 打开指定分发任务的详情页。
   * @param {Object} row 当前分发任务记录。
   */
  function handleDetail(row) {
    proxy.$router.push({ path: '/dist/task/detail', query: { id: row.id, rowNumber: row.rowNumber } });
  }

  /**
   * 确认后删除单条或已勾选的分发任务，并刷新当前列表。
   * @param {Object} [row] 当前行任务；省略时删除已勾选的任务。
   */
  function handleDelete(row) {
    const deleteIds = row?.id != null ? [row.id] : [...ids.value];
    if (!deleteIds.length) return;
    const message = row?.id != null
      ? `是否确认删除分发任务“${row.name || row.id}”？`
      : `是否确认删除选中的 ${deleteIds.length} 个分发任务？`;
    proxy.$modal.confirm(message).then(() => {
      return delTASK(deleteIds.join(","));
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }

  loadModelOptions();
  loadTargetNames();
</script>

<style scoped>
:deep(.last-execution-column .cell) {
  padding-left: 20px;
}

.last-execute-info {
  display: flex;
  align-items: center;
  min-width: 0;
  font-size: 14px;
  color: #1e293b;
}

.task-info {
  min-width: 0;
}

.task-info-title {
  color: var(--el-color-primary);
  cursor: pointer;
}

.task-icon-text {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.task-list-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.task-name {
  color: var(--qt-text-regular);
  font-size: var(--qt-font-size-base);
  font-weight: var(--qt-font-weight-medium);
  line-height: 22px;
}

.task-data-mode-tag :deep(.el-tag) {
  font-size: var(--qt-font-size-base);
  font-weight: var(--qt-font-weight-regular);
}

.task-description {
  margin-top: var(--qt-space-1);
}

.task-secondary {
  margin-top: var(--qt-space-1);
  color: var(--qt-text-secondary);
  font-size: var(--qt-font-size-small);
}

.task-execution-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  font-size: var(--qt-font-size-base);
  line-height: 20px;
}

.task-execution-schedule {
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-status-switches {
  display: flex;
  flex-direction: column;
  gap: var(--qt-space-1);
  line-height: 20px;
}

.task-status-switches__item {
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.task-status-switches__label {
  min-width: 60px;
  color: var(--qt-text-primary);
  font-size: var(--qt-font-size-small);
}

.task-action-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--qt-space-2);
  white-space: nowrap;
}

.task-action-toolbar :deep(.el-button) {
  margin: 0;
}

:global(.dist-task-action-popover .task-action-list) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: var(--qt-space-2);
  width: 100%;
}

:global(.dist-task-action-popover .task-action-list .el-button) {
  width: 100%;
  justify-content: center;
  margin-left: 0;
  text-align: center;
}
</style>
