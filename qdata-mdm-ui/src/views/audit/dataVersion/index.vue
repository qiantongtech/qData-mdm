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
  @file dataVersion/index.vue
  @description 数据版本列表与版本查看
  @author zyy
-->
<template>
  <div ref="app-container" class="app-container">
    <guide-tip tip-id="audit/data-version.list" />
    <el-container class="audit-tree-layout">
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
      <el-main class="main-content audit-tree-main data-version-main">
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
            <guide-tip-panel
              type="plain"
              title="本页展示主数据的正式版本，包括版本形成原因和当前生命周期状态，支持查看和对比版本。"
            />
          </template>

          <qt-table v-bind="tableStore" ref="tableRef">
            <template #number="{ row }">{{ row.displayId || row.id || '-' }}</template>
            <template #name="{ row }">
              <div :title="`${displayValue(row.name)}（${displayValue(row.businessCode)}）`" class="version-name">
                <el-link
                  v-if="getDisplayTitle(displayValue(row.name))"
                  type="primary" :underline="false"
                  role="link" tabindex="0"
                  class="qt-detail-link text-ellipsis version-name-text"
                  :title="String(displayValue(row.name))"
                  @click.stop="handleDetail(row)"
                  @keydown.enter.stop.prevent="handleDetail(row)"
                  @keydown.space.stop.prevent="handleDetail(row)"
                >{{ displayValue(row.name) }}</el-link>
                <span v-else class="qt-detail-link text-ellipsis version-name-text">-</span>
                <span class="master-code">（{{ displayValue(row.businessCode) }}）</span>
              </div>
              <div class="version-tags">
                <dict-tag v-if="row.state" :type="['生效', '有效'].includes(row.state) ? 'success' : 'info'">{{ row.state }}</dict-tag>
                <span v-else class="cell-sub">-</span>
                <el-tag v-if="row.version"
                        size="small"
                        type="info"
                        effect="plain"
                        class="version-badge"
                >{{ formatDataVersion(row.version) }}</el-tag>
                <span v-else class="cell-sub">-</span>
              </div>
            </template>
            <template #modelInfo="{ row }">
              <audit-operation-cell kind="model" :row="row" />
            </template>
            <template #location="{ row }">
              <audit-operation-cell kind="location" :row="row" />
            </template>
            <template #formation="{ row }">
              <div class="cell-main"><dict-tag v-if="row.formationType" :value="row.formationType" type="primary" /><span v-else>-</span></div>
              <div class="cell-sub">{{ row.category || '-' }}</div>
            </template>
            <template #operator="{ row }">
              <div class="cell-main">{{ row.operatorName || '-' }}</div>
              <div class="cell-sub">{{ row.operatorPhone || '-' }}</div>
            </template>
            <template #time="{ row }">{{ formatMinute(row.time) }}</template>
            <template #action="{ row }">
              <el-button link
                         type="primary"
                         icon="view"
                         @click="handleDetail(row)"
              >查看版本</el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 数据版本详情弹窗 -->
    <el-dialog
      v-model="drawerOpen"
      width="min(1000px, 94vw)"
      :append-to="$refs['app-container']"
      destroy-on-close
      class="version-detail-dialog"
    >
      <template #header>
        <div class="version-dialog-title">数据版本详情</div>
      </template>
      <div class="version-toolbar">
        <template v-if="!compareMode">
          <div class="version-tabs">
            <el-button
              v-for="item in currentVersions"
              :key="item.id"
              :type="item.id === current?.id ? 'primary' : 'default'"
              :plain="item.id !== current?.id"
              @click="selectVersion(item)"
            >{{ formatDataVersion(item.version) || '-' }}</el-button>
          </div>
          <el-button :disabled="currentVersions.length < 2" @click="toggleCompare">版本对比</el-button>
        </template>
        <template v-else>
          <el-form :inline="true" class="compare-form">
            <el-form-item label="版本 A">
              <el-select v-model="versionA" class="version-compare-select">
                <el-option v-for="item in currentVersions"
                           :key="item.id"
                           :label="formatDataVersion(item.version) || '-'"
                           :value="item.id"
                           :disabled="item.id === versionB"
                />
              </el-select>
            </el-form-item>
            <span class="compare-separator">对比</span>
            <el-form-item label="版本 B">
              <el-select v-model="versionB" class="version-compare-select">
                <el-option v-for="item in currentVersions"
                           :key="item.id"
                           :label="formatDataVersion(item.version) || '-'"
                           :value="item.id"
                           :disabled="item.id === versionA"
                />
              </el-select>
            </el-form-item>
            <el-form-item><el-checkbox v-model="onlyChanged">仅查看变化字段</el-checkbox></el-form-item>
          </el-form>
          <el-button @click="toggleCompare">退出对比</el-button>
        </template>
      </div>
      <el-scrollbar max-height="62vh" always>
        <template v-if="!compareMode">
          <div class="h2-titles version-heading">{{ formatDataVersion(current?.version) || '-' }}</div>
          <el-table :data="snapshotRows" border empty-text="该版本暂无业务字段">
            <el-table-column label="字段" prop="field" width="220" />
            <el-table-column label="字段值"
                             prop="value"
                             min-width="360"
                             show-overflow-tooltip
            />
          </el-table>
        </template>
        <template v-else-if="currentVersions.length < 2">
          <el-empty description="当前只有 1 个正式版本，暂无可对比版本" />
        </template>
        <template v-else>
          <el-table :data="compareRows" border empty-text="两个版本没有差异字段">
            <el-table-column label="字段" prop="field" width="180" />
            <el-table-column :label="versionALabel"
                             prop="before"
                             min-width="280"
                             show-overflow-tooltip
            />
            <el-table-column :label="versionBLabel"
                             prop="after"
                             min-width="280"
                             show-overflow-tooltip
            />
          </el-table>
        </template>
      </el-scrollbar>
      <template #footer><el-button @click="drawerOpen = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="AuditDataVersion">
import { getDisplayTitle } from '@/utils/anivia.js'
import AuditOperationCell from '../operationLog/AuditOperationCell.vue'
import { onActivated, onDeactivated } from 'vue'

import axios from 'axios'

import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { listAuditModels, listAuditObjectVersions, listAuditVersions } from '@/api/audit/trace'
import QtTree from '@/components/QtTree/index.vue'
import { usePublishedModelTree } from '@/composables/usePublishedModelTree'
import { getBase } from '@/api/model/base/base'


const tableRef = ref(null);
const { proxy } = getCurrentInstance();
const drawerOpen = ref(false);
const compareMode = ref(false);
const current = ref(null);
const onlyChanged = ref(false);
const versionA = ref('');
const versionB = ref('');
const currentVersions = ref([]);
const versionSnapshots = ref({});

const data = reactive({
  queryParams: {
    model: "",
    name: null,
    code: null,
    version: null,
    state: "",
    timeRange: [],
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：
 *  必须是 reactive 的原始对象，不能传 toRefs 出来的 ref，
 *  否则 qt-table 里 Object.assign({}, store.params, props.params) 展开 ref 只会得到 { value: {...} }，筛选条件全部丢失。
 */
const queryParams = data.queryParams;
let pendingVersionRequests = 0;
let versionReloadNeeded = false;

async function getVersionList(params) {
  let response;
  pendingVersionRequests++;
  try {
    response = await listAuditVersions(params);
  } catch (error) {
    if (axios.isCancel(error)) versionReloadNeeded = true;
    throw error;
  } finally {
    pendingVersionRequests--;
  }
  const rows = response?.data?.rows || [];
  const displayRows = rows.map(row => reactive(enrichVersionRow(row)));
  if (proxy.$auth.hasPermi('model:base:query')) {
    const ids = [...new Set(rows.map(row => row.modelId).filter(id => id != null))];
    ids.forEach(async id => {
      try {
        const result = await getBase(id);
        if (result?.data) {
          rows.forEach((row, index) => {
            if (String(row.modelId) === String(id)) Object.assign(displayRows[index], enrichVersionRow(row, result.data));
          });
        }
      } catch {
        // 模型详情不可用时保留接口返回的版本列表。
      }
    });
  }
  return {
    ...response,
    data: { ...response.data, rows: displayRows },
  };
}

function formatDataVersion(value) {
  const version = String(value ?? '').trim();
  const match = version.match(/^v?(\d+)(?:\.(\d+))?$/i);
  return match ? `V${match[1]}.${match[2] ?? '0'}` : version;
}

function enrichVersionRow(row, model) {
  const type = model?.type === '0' ? 'NORMAL' : model?.type === '1' ? 'CATEGORY' : model?.type;
  const layers = model?.layers?.[model.layers.length - 1]?.name;
  const lifecycleTypes = ['冻结', '解冻', '失效', '恢复', '归档'];
  const contentTypes = ['新增', '修改', '复制', '历史版本恢复', '多源融合', '初始版本'];
  return {
    ...row,
    modelCode: row.modelCode || model?.code,
    modelType: row.modelType || type,
    modelIcon: row.modelIcon || model?.icon,
    modelDescription: row.modelDescription || model?.description,
    projectName: row.projectName || model?.projectName,
    projectIcon: row.projectIcon || model?.projectIcon,
    layerName: row.layerName || layers,
    formationType: row.formationType || row.reason,
    category: row.category || (lifecycleTypes.includes(row.reason) ? '生命周期变化' : contentTypes.includes(row.reason) ? '数据内容变化' : ''),
  };
}

const tableStore = reactive({
  config: {
    stripe: true,
    table: {
      rowKey: "id",
      stripe: true,
      defaultSort: { prop: "time", order: "descending" },
    },
  },
  columns: [
    { label: "编号", prop: "id", slot: "number", align: "center", headerAlign: "center", width: 90, minWidth: 90, sortable: true,
      sortBy: row => Number(row.displayId || row.id) || 0 },
    { label: "主数据模型", prop: "model", slot: "modelInfo", align: "left", minWidth: 380, showOverflowTooltip: { effect: "light" } },
    { label: "归属位置", prop: "projectName", slot: "location", align: "left", minWidth: 260, showOverflowTooltip: { effect: "light" } },
    {
      label: "主数据信息",
      prop: "name",
      slot: "name",
      align: "left",
      minWidth: 290,
    },
    {
      label: "形成原因",
      prop: "reason",
      slot: "formation",
      align: "left",
      minWidth: 150,
      showOverflowTooltip: { effect: "light" },
    },
    { label: "操作人", prop: "operatorName", slot: "operator", align: "left", minWidth: 150 },
    { label: "操作时间", prop: "time", slot: "time", align: "left", minWidth: 150, sortable: true },
    { label: "操作", slot: "action", width: 170, minWidth: 170, fixed: "right", align: "center" },
  ],
  func: getVersionList,
  params: queryParams,
  events: { formatData: data => data },
});

const modelOptions = ref([{ value: '', label: '全部模型' }]);
function addTreeKeys(nodes = []) {
  return nodes.map(node => ({
    ...node,
    treeKey: `${node.nodeType}-${node.id}`,
    children: addTreeKeys(node.children || []),
  }))
}

function findModelsByName(nodes, name, matches = []) {
  for (const node of nodes || []) {
    if (node.nodeType === 'model' && node.modelId != null && node.name === name) matches.push(node)
    findModelsByName(node.children, name, matches)
  }
  return matches
}
/** 审计列表当前按模型名称筛选；树数据与分发任务共用已发布模型接口。 */

const modelTreeRef = ref(null)
const selectedTreeKey = ref(null)
const { data: modelTreeData, loading: modelTreeLoading, reload: reloadModelTree } = usePublishedModelTree(addTreeKeys)
const defaultExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey))
let treePageDeactivated = false
onDeactivated(() => {
  treePageDeactivated = true
  if (pendingVersionRequests > 0) versionReloadNeeded = true
})
onActivated(() => {
  if (!treePageDeactivated) return
  treePageDeactivated = false
  // 切页可能取消首次请求；返回缓存页面时重试空树。
  if (!modelTreeData.value.length) reloadModelTree().catch(() => {
    // 请求工具负责错误提示，避免重复弹窗。
  })
  if (versionReloadNeeded) {
    versionReloadNeeded = false
    tableRef.value?.getList()
  }
})
let syncVersion = 0

function handleModelTreeClick(node) {
  if (node?.nodeType !== 'model' || node.modelId == null || !node.name) {
    modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false)
    return
  }
  selectedTreeKey.value = node.treeKey
  if (!modelOptions.value.some(option => option.value === node.name)) {
    modelOptions.value.push({ value: node.name, label: node.name })
  }
  queryParams.model = node.name
  tableRef.value?.resetQuery()
}

function resetModelTree() {
  selectedTreeKey.value = null
  queryParams.model = ''
  modelTreeRef.value?.setCurrentKey(null, false)
}

// 下拉框与树保持一致；重名时保留当前节点，否则不猜测应选中哪个节点。
watch([() => queryParams.model, modelTreeData, modelOptions], async ([name, nodes]) => {
  const version = ++syncVersion
  await nextTick()
  if (version !== syncVersion) return
  const matches = name ? findModelsByName(nodes, name) : []
  const selected = matches.find(node => node.treeKey === selectedTreeKey.value)
    || (matches.length === 1 ? matches[0] : null)
  selectedTreeKey.value = selected?.treeKey || null
  if (selected && !modelOptions.value.some(option => option.value === selected.name)) {
    modelOptions.value.push({ value: selected.name, label: selected.name })
  }
  modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false)
}, { flush: 'post' })
onMounted(() => listAuditModels().then(res => {
  if (res?.data) modelOptions.value = [{ value: '', label: '全部模型' }, ...res.data];
}).catch(() => {
  // 请求工具已提示错误，保留现有模型选项。
}));

const searchStore = reactive({
  items: [
    {
      label: "主数据模型",
      prop: "model",
      component: {
        is: "select",
        placeholder: "全部模型",
        options: modelOptions,
      },
    },
    {
      label: "主数据名称",
      prop: "name",
      component: { is: "input", placeholder: "请输入主数据名称" },
    },
    {
      label: "主数据ID",
      prop: "code",
      component: { is: "input", placeholder: "请输入主数据ID" },
    },
    {
      label: "版本号",
      prop: "version",
      component: { is: "input", placeholder: "请输入版本号" },
    },
    {
      label: "当前生命周期",
      prop: "state",
      component: { is: "input", placeholder: "请输入生命周期状态" },
    },
    {
      label: "版本形成时间",
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

function displayValue(value) {
  return value === null || value === undefined || value === '' || value === '—' ? '-' : value;
}

// 仅调整展示精度；查询、筛选和排序仍使用接口返回的原始时间。
function formatMinute(value) {
  return value ? String(value).slice(0, 16) : '-';
}

function versionContentRows(version, includeTime = true) {
  if (!version) return [];
  const snapshot = versionSnapshots.value[version.id] || [];
  const rows = [
    { code: '__model', field: '主数据模型', value: displayValue(version.model) },
    ...snapshot.map(item => ({ code: item.code || item.field, field: item.field, value: displayValue(item.value) })),
    { code: '__reason', field: '形成原因', value: displayValue(version.reason) },
  ];
  if (includeTime) rows.push({ code: '__time', field: '形成时间', value: formatMinute(version.time) });
  return rows;
}

const snapshotRows = computed(() => versionContentRows(current.value));
const versionALabel = computed(() => formatDataVersion(currentVersions.value.find(item => item.id === versionA.value)?.version) || '版本 A');
const versionBLabel = computed(() => formatDataVersion(currentVersions.value.find(item => item.id === versionB.value)?.version) || '版本 B');
const compareRows = computed(() => {
  if (!versionA.value || !versionB.value || versionA.value === versionB.value) return [];
  const left = currentVersions.value.find(item => item.id === versionA.value);
  const right = currentVersions.value.find(item => item.id === versionB.value);
  const before = new Map(versionContentRows(left, false).map(item => [item.code, item]));
  const after = new Map(versionContentRows(right, false).map(item => [item.code, item]));
  return [...new Set([...before.keys(), ...after.keys()])]
      .map(code => ({
        field: before.get(code)?.field || after.get(code)?.field || code,
        before: before.get(code)?.value ?? '-',
        after: after.get(code)?.value ?? '-',
      }))
      .map(row => ({ ...row, changed: row.before !== row.after }))
      .filter(row => !onlyChanged.value || row.changed);
});

/** 数据版本记录数（由表格数据回传，不依赖 qt-table 内部状态） */

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  resetModelTree();
  tableRef.value?.resetQuery();
}

/** 查看同一主数据的真实版本。 */
async function handleDetail(row) {
  current.value = row;
  currentVersions.value = [];
  versionSnapshots.value = {};
  const response = await listAuditObjectVersions(row.modelId, row.masterRecordId);
  currentVersions.value = response.data || [];
  versionSnapshots.value = Object.fromEntries(currentVersions.value.map(item => [item.id, item.snapshot || []]));
  current.value = currentVersions.value.find(item => item.id === row.id) || row;
  compareMode.value = false;
  onlyChanged.value = false;
  drawerOpen.value = true;
}

function selectVersion(row) {
  current.value = row;
  compareMode.value = false;
}

function toggleCompare() {
  if (!compareMode.value) {
    if (currentVersions.value.length < 2) return;
    const index = currentVersions.value.findIndex(item => item.id === current.value?.id);
    const left = index > 0 ? currentVersions.value[index - 1] : currentVersions.value[0];
    const right = index > 0 ? currentVersions.value[index] : currentVersions.value[1];
    versionA.value = left.id;
    versionB.value = right.id;
  }
  compareMode.value = !compareMode.value;
}
</script>

<style lang="scss" scoped>
.audit-tree-layout {
  min-height: 0;
}
.audit-tree-main {
  min-width: 0;
}

.cell-main {
  line-height: 20px;
}

.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.version-name {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.version-name {
  flex-wrap: nowrap;
  min-width: 0;
}
.version-name-text {
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.master-code {
  white-space: nowrap;
  flex-shrink: 0;
}
.version-badge {
  flex: none;
}
.version-tags {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 操作列：去掉 link 按钮及内部文字的下划线 */
:deep(.el-button.is-link),
:deep(.el-button.is-link > span) {
  text-decoration: none !important;
}

.version-dialog-title {
  font-size: 18px;
  line-height: 24px;
}
.version-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.version-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.version-tabs .el-button + .el-button {
  margin-left: 0;
}
.version-heading {
  margin: 0 0 12px;
  line-height: 20px;
}
.compare-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
}
.compare-form :deep(.el-form-item) {
  margin: 0;
}
.version-compare-select {
  width: 150px;
}
.compare-separator {
  color: var(--qt-text-regular);
}
:deep(.version-detail-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 14px;
}
:deep(.version-detail-dialog .el-dialog__footer) {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 14px;
}
</style>
