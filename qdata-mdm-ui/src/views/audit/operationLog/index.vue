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

<!--
  @file operationLog/index.vue
  @description 操作审计列表
  @author zyy
-->
<template>
  <div ref="app-container" class="app-container">
    <guide-tip tip-id="audit/operation-log.list" />
    <el-container class="operation-audit-layout">
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        :tree-props="{ label: 'name', children: 'children' }"
        node-key="treeKey"
        :editable="false"
        :default-expand="false"
        :default-expanded-keys="modelTreeExpandedKeys"
        :wrap-height="treeHeight"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      >
        <template #before-search>
          <el-text v-if="!modelTreeLoading && !modelTreeData.length" type="info">暂无可显示的模型树数据</el-text>
        </template>
      </qt-tree>
      <el-main class="main-content operation-audit-main">
        <!-- 业务范围：主数据管理 -->
        <qt-wrap
          v-show="scope === 'master'"
          ref="masterWrapRef"
          :columns="masterTableStore.columns"
          :tableRef="masterTableRef"
        >
          <template #search>
            <!-- 切换业务范围后再挂载搜索栏，避免隐藏状态下宽度测量为 0。 -->
            <qt-search-bar
              v-if="scope === 'master'"
              v-bind="masterSearchStore"
              :params="masterTableStore.params"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>
          <template #actions-data>
            <div v-if="scope === 'master'" class="audit-scope">
              <el-tabs v-model="scope" @tab-change="handleScopeChange">
                <qt-tab-pane label="主数据管理" name="master" />
                <qt-tab-pane label="模型管理" name="model" />
              </el-tabs>
            </div>
          </template>

          <qt-table v-bind="masterTableStore" ref="masterTableRef">
            <template #modelInfo="{ row }"><audit-operation-cell kind="model" :row="row" scope="master" /></template>
            <template #location="{ row }"><audit-operation-cell kind="location" :row="row" scope="master" /></template>
            <template #masterData="{ row }"><audit-operation-cell kind="masterData" :row="row" scope="master" @open-detail="handleDetail" /></template>
            <template #operationInfo="{ row }"><audit-operation-cell kind="operation" :row="row" scope="master" /></template>
            <template #operatorInfo="{ row }"><audit-operation-cell kind="operator" :row="row" scope="master" /></template>
            <template #action="{ row }">
              <el-button
                v-hasPermi="['audit:log:query']"
                link
                type="primary"
                icon="view"
                @click="handleDetail(row)"
              >详情</el-button>
            </template>
          </qt-table>
        </qt-wrap>

        <!-- 业务范围：模型管理 -->
        <qt-wrap
          v-show="scope === 'model'"
          ref="modelWrapRef"
          :columns="modelTableStore.columns"
          :tableRef="modelTableRef"
        >
          <template #search>
            <qt-search-bar
              v-if="scope === 'model'"
              v-bind="modelSearchStore"
              :params="modelTableStore.params"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>
          <template #actions-data>
            <div v-if="scope === 'model'" class="audit-scope">
              <el-tabs v-model="scope" @tab-change="handleScopeChange">
                <qt-tab-pane label="主数据管理" name="master" />
                <qt-tab-pane label="模型管理" name="model" />
              </el-tabs>
            </div>
          </template>

          <qt-table v-bind="modelTableStore" ref="modelTableRef">
            <template #modelInfo="{ row }"><audit-operation-cell kind="model" :row="row" scope="model" /></template>
            <template #location="{ row }"><audit-operation-cell kind="location" :row="row" scope="model" /></template>
            <template #operationInfo="{ row }"><audit-operation-cell kind="operation" :row="row" scope="model" /></template>
            <template #operatorInfo="{ row }"><audit-operation-cell kind="operator" :row="row" scope="model" /></template>
            <template #action="{ row }">
              <el-button
                v-hasPermi="['audit:log:query']"
                link
                type="primary"
                icon="view"
                @click="handleDetail(row)"
              >详情</el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="AuditOperationLog">
import { listAuditModels, listAuditOperations } from '@/api/audit/trace'
import { usePublishedModelTree } from '@/composables/usePublishedModelTree'
import QtTree from '@/components/QtTree/index.vue'
import AuditOperationCell from './AuditOperationCell.vue'
import { useRoute, useRouter } from "vue-router";

const MASTER_OPERATION_TYPES = [
  '新增', '修改', '复制', '冻结', '解冻', '失效', '恢复', '归档', '历史版本恢复', '多源融合',
].map(value => ({ value, label: value }))
const MODEL_OPERATION_TYPES = [
  { value: '发布模型', label: '发布模型' },
  { value: '停用模型', label: '停用模型' },
]

const route = useRoute();
const router = useRouter();
const masterTableRef = ref(null);
const modelTableRef = ref(null);
const modelTreeRef = ref(null);
const selectedTreeKey = ref(null);
const { data: modelTreeData, loading: modelTreeLoading } = usePublishedModelTree(addModelTreeKeys);
const modelTreeExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey));

/** 业务范围：master 主数据管理 / model 模型管理 */
const scope = ref(route.query.scope === 'model' ? 'model' : 'master');
const masterWrapRef = ref(null);
const modelWrapRef = ref(null);
const treeHeight = ref('86vh');
let wrapObserver = null;
let measureFrame = null;
let pageDisposed = false;

function currentWrapElement() {
  const wrap = scope.value === 'model' ? modelWrapRef.value : masterWrapRef.value;
  return wrap?.$el || null;
}

function measureCurrentWrap() {
  if (measureFrame != null) cancelAnimationFrame(measureFrame);
  measureFrame = requestAnimationFrame(() => {
    measureFrame = null;
    const height = currentWrapElement()?.getBoundingClientRect().height;
    if (height > 0) treeHeight.value = `${Math.ceil(height)}px`;
  });
}

async function observeCurrentWrap() {
  await nextTick();
  if (pageDisposed) return;
  wrapObserver?.disconnect();
  const element = currentWrapElement();
  if (!element) return;
  wrapObserver?.observe(element);
  measureCurrentWrap();
}

onMounted(() => {
  if (typeof ResizeObserver !== 'undefined') wrapObserver = new ResizeObserver(measureCurrentWrap);
  window.addEventListener('resize', measureCurrentWrap);
  observeCurrentWrap();
});

watch(scope, observeCurrentWrap, { flush: 'post' });

onBeforeUnmount(() => {
  pageDisposed = true;
  wrapObserver?.disconnect();
  window.removeEventListener('resize', measureCurrentWrap);
  if (measureFrame != null) cancelAnimationFrame(measureFrame);
});

/** 主数据搜索条件 */
const masterData = reactive({
  queryParams: {
    objectType: "",
    objectName: null,
    objectId: null,
    type: null,
    operatorName: "",
    timeRange: [],
    treeModelIds: '',
    treeModelNames: '',
    treeModelCodes: '',
  },
});

/** 模型搜索条件 */
const modelData = reactive({
  queryParams: {
    modelName: null,
    modelCode: null,
    type: null,
    operatorName: "",
    timeRange: [],
    treeModelIds: '',
    treeModelNames: '',
    treeModelCodes: '',
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：
 *  必须是 reactive 的原始对象，不能传 toRefs 出来的 ref，
 *  否则 qt-table 里 Object.assign({}, store.params, props.params) 展开 ref 只会得到 { value: {...} }，筛选条件全部丢失。
 */
const masterParams = masterData.queryParams;
const modelParams = modelData.queryParams;

function addModelTreeKeys(nodes = []) {
  return nodes.map(node => ({
    ...node,
    treeKey: `${node.nodeType}-${node.id}`,
    children: addModelTreeKeys(node.children || []),
  }));
}

function findModelsByName(nodes, name, matches = []) {
  for (const node of nodes || []) {
    if (node.nodeType === 'model' && node.modelId != null && node.name === name) matches.push(node);
    findModelsByName(node.children, name, matches);
  }
  return matches;
}

function applyTreeModel(node) {
  selectedTreeKey.value = node?.treeKey || null;
  for (const params of [masterParams, modelParams]) {
    params.treeModelIds = node ? String(node.modelId) : '';
    params.treeModelNames = node ? node.name : '';
    params.treeModelCodes = node?.code || '';
  }
}

/** 只按模型节点筛选，域和目录用于展开或收起。 */
function handleModelTreeClick(node) {
  if (node?.nodeType !== 'model' || node.modelId == null || !node.name) {
    modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false);
    return;
  }
  applyTreeModel(node);
  if (!masterModelOptions.value.some(option => option.value === node.name)) {
    masterModelOptions.value.push({ value: node.name, label: node.name });
  }
  masterParams.objectType = node.name;
  masterTableRef.value?.resetQuery();
  modelTableRef.value?.resetQuery();
}

async function getMasterList(params) {
  return listAuditOperations({ ...params, scope: 'master' });
}

async function getModelList(params) {
  return listAuditOperations({ ...params, scope: 'model' });
}

const masterModelOptions = ref([{ value: '', label: '全部模型' }]);
let treeSyncVersion = 0;
watch([() => masterParams.objectType, modelTreeData, masterModelOptions], async ([name, nodes]) => {
  const version = ++treeSyncVersion;
  await nextTick();
  if (version !== treeSyncVersion) return;
  const matches = name ? findModelsByName(nodes, name) : [];
  const selected = matches.find(node => node.treeKey === selectedTreeKey.value)
      || (matches.length === 1 ? matches[0] : null);
  applyTreeModel(selected);
  if (selected && !masterModelOptions.value.some(option => option.value === selected.name)) {
    masterModelOptions.value.push({ value: selected.name, label: selected.name });
  }
  modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false);
}, { flush: 'post' });
onMounted(() => {
  listAuditModels().then(res => {
    if (res?.data) masterModelOptions.value = [{ value: '', label: '全部模型' }, ...res.data];
  }).catch(() => {
    // 请求工具已提示错误，保留现有模型选项。
  });
});

const baseConfig = {
  stripe: true,
  table: {
    rowKey: "eventKey",
    stripe: true,
    scrollbarAlwaysOn: true,
    defaultSort: { prop: "operateTime", order: "descending" },
  },
};

/** 两个页签分别持有列配置，列显隐互不影响。 */
function createAuditColumns() {
  return [
    { label: "编号", prop: "id", width: 90, minWidth: 90, align: "center", headerAlign: "center", sortable: "custom" },
    { label: "主数据模型", prop: "objectType", slot: "modelInfo", align: "left", minWidth: 400, showOverflowTooltip: { effect: "light" } },
    { label: "归属位置", prop: "projectName", slot: "location", align: "left", minWidth: 240, showOverflowTooltip: { effect: "light" } },
    { label: "主数据", prop: "objectName", slot: "masterData", align: "left", minWidth: 360 },
    { label: "操作信息", prop: "type", slot: "operationInfo", align: "left", minWidth: 200 },
    { label: "操作人", prop: "operatorName", slot: "operatorInfo", align: "left", minWidth: 160 },
    { label: "操作时间", prop: "operateTime", date: "{y}-{m}-{d} {h}:{i}", align: "center", minWidth: 180, sortable: true },
    { label: "操作", slot: "action", width: 140, minWidth: 140, fixed: "right", align: "center" },
  ];
}

const masterTableStore = reactive({
  config: baseConfig,
  columns: createAuditColumns(),
  func: getMasterList,
  params: masterParams,
  events: {
    formatData: data => data,
  },
});

const modelTableStore = reactive({
  config: baseConfig,
  columns: createAuditColumns().filter(column => column.slot !== 'masterData'),
  func: getModelList,
  params: modelParams,
  events: {
    formatData: data => data,
  },
});

const masterSearchStore = reactive({
  items: [
    {
      label: "主数据模型",
      prop: "objectType",
      component: { is: "select", placeholder: "全部模型", options: masterModelOptions },
    },
    {
      label: "主数据名称",
      prop: "objectName",
      component: { is: "input", placeholder: "请输入主数据名称" },
    },
    {
      label: "主数据ID",
      prop: "objectId",
      component: { is: "input", placeholder: "请输入主数据ID" },
    },
    {
      label: "操作类型",
      prop: "type",
      component: { is: "select", placeholder: "全部类型", options: MASTER_OPERATION_TYPES },
    },
    {
      label: "操作人",
      prop: "operatorName",
      component: { is: "input", placeholder: "请输入操作人" },
    },
    {
      label: "修改时间",
      prop: "timeRange",
      component: {
        is: "date-picker",
        type: "daterange",
        rangeSeparator: "-",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        style: { width: "252px" },
      },
    },
  ],
});

const modelSearchStore = reactive({
  items: [
    {
      label: "模型名称",
      prop: "modelName",
      component: { is: "input", placeholder: "请输入模型名称" },
    },
    {
      label: "模型编码",
      prop: "modelCode",
      component: { is: "input", placeholder: "请输入模型编码" },
    },
    {
      label: "操作类型",
      prop: "type",
      component: { is: "select", placeholder: "全部类型", options: MODEL_OPERATION_TYPES },
    },
    {
      label: "操作人",
      prop: "operatorName",
      component: { is: "input", placeholder: "请输入操作人" },
    },
    {
      label: "修改时间",
      prop: "timeRange",
      component: {
        is: "date-picker",
        type: "daterange",
        rangeSeparator: "-",
        startPlaceholder: "开始日期",
        endPlaceholder: "结束日期",
        style: { width: "252px" },
      },
    },
  ],
});

/** 切换业务范围 */
async function handleScopeChange(name) {
  await router.replace({ query: { ...route.query, scope: name } });
  await nextTick();
  if (scope.value !== name) return;
  if (name === 'model') {
    modelTableRef.value?.resetQuery();
  } else {
    masterTableRef.value?.resetQuery();
  }
}

/** 搜索按钮操作 */
function handleQuery() {
  if (scope.value === 'model') {
    modelTableRef.value?.resetQuery();
  } else {
    masterTableRef.value?.resetQuery();
  }
}

/** 重置按钮操作 */
function resetQuery() {
  selectedTreeKey.value = null;
  masterParams.objectType = '';
  for (const params of [masterParams, modelParams]) {
    params.treeModelIds = '';
    params.treeModelNames = '';
    params.treeModelCodes = '';
  }
  modelTreeRef.value?.setCurrentKey(null, false);
  handleQuery();
}

/** 查看详情 */
function handleDetail(row) {
  router.push({
    path: '/audit/operationLog/detail',
    query: { id: row.sourceId || row.id, scope: row.scope, source: row.sourceType },
  });
}
</script>

<style lang="scss" scoped>
.operation-audit-layout {
  min-height: 0;
}
.operation-audit-main {
  min-width: 0;
}

.audit-scope {
  width: 100%;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
}

:deep(.qt-wrap--actions) {
  align-items: flex-end;
  position: relative;
}

:deep(.qt-wrap--actions::after) {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 2px;
  background-color: var(--qt-border-color);
}

:deep(.qt-wrap--actions .data-actions) {
  flex: 1;
  min-width: 0;
  position: relative;
  z-index: 1;
}

:deep(.qt-wrap--actions .table-actions) {
  padding-bottom: 8px;
}

/* 操作列：去掉 link 按钮及内部文字的下划线 */
:deep(.el-button.is-link),
:deep(.el-button.is-link > span) {
  text-decoration: none !important;
}
</style>
