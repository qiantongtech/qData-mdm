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
  @file sourceTrace/index.vue
  @description 来源追溯列表与预留详情
  @author zyy
-->
<template>
  <el-scrollbar always class="audit-page-scroll">
    <div ref="app-container" class="app-container">
      <guide-tip tip-id="audit/source-trace.list" />
      <el-container class="audit-tree-layout">
        <qt-tree
          ref="modelTreeRef"
          :data="modelTreeData"
          :action-loading="modelTreeLoading"
          :editable="false"
          :default-expand="false"
          :default-expanded-keys="defaultExpandedKeys"
          node-key="treeKey"
          :wrap-height="treeHeight"
          title="主数据模型"
          placeholder="搜索名称或编码"
          @node-click="handleModelTreeClick"
        />
        <el-main class="main-content audit-tree-main source-trace-main">
          <qt-wrap ref="sourceWrapRef" :columns="tableStore.columns" :tableRef="tableRef">
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
                title="本页展示权威主数据的来源构成，包括来源系统和形成方式，便于追踪和追溯主数据的形成过程。"
              />
            </template>

            <qt-table v-bind="tableStore" ref="tableRef">
              <template #number="{ row }">{{ numberFor(row) }}</template>
              <template #name="{ row }">
                <div :title="`${displayValue(row.displayName)}（${displayValue(row.code)}）`" class="cell-main master-info">{{ displayValue(row.displayName) }}（{{ displayValue(row.code) }}）</div>
              </template>
              <template #modelInfo="{ row }">
                <audit-operation-cell kind="model" :row="row" />
              </template>
              <template #location="{ row }">
                <audit-operation-cell kind="location" :row="row" />
              </template>
              <template #systems="{ row }">
                <dict-tag v-if="row.mode === '平台录入'" type="primary">平台直接形成</dict-tag>
                <div v-else-if="row.displaySystems.length" class="flex-column flex-column-gap">
                  <div v-for="(system, index) in row.displaySystems" :key="index" class="qt-table-info">
                    <div class="qt-table-info__main">
                      <img :src="defaultProjectIcon" alt="" class="info-icon" />
                      <span class="info-title text-ellipsis" :title="formatSystemTitle(system)">{{ formatSystemTitle(system) }}</span>
                    </div>
                    <div class="info-desc text-ellipsis" :title="system.description || '-'">{{ system.description || '-' }}</div>
                  </div>
                </div>
                <span v-else>-</span>
              </template>
              <template #mode="{ row }">
                <div v-if="row.mode" class="source-system">
                  <dict-tag v-for="mode in row.mode.split('、')"
                            :key="mode"
                            :type="mode === '多源融合' ? 'warning' : 'success'"
                  >{{ mode }}</dict-tag>
                </div>
                <span v-else>-</span>
              </template>
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

      <!-- 来源追溯详情暂未开放，恢复时取消此段注释。
      <el-dialog
        v-model="drawerOpen"
        title="来源追溯详情"
        class="source-detail-dialog"
        width="min(1080px, 94vw)"
        :append-to="$refs['app-container']"
        destroy-on-close
      >
        <el-scrollbar class="source-detail-scroll" always>
          <div class="source-overview">
            <div class="source-overview-main">
              <span class="source-name">{{ current?.name || '-' }}</span>
              <dict-tag :type="current?.mode === '多源融合' ? 'warning' : 'success'">{{ current?.mode || '-' }}</dict-tag>
              <dict-tag :type="['生效', '有效'].includes(detail.lifecycle) ? 'success' : 'info'">{{ detail.lifecycle || '-' }}</dict-tag>
            </div>
            <div class="source-overview-sub">{{ current?.code || '-' }} · {{ current?.model || '-' }}</div>
            <div class="source-overview-sub">形成时间：{{ current?.updated?.slice(0, 16) || '-' }}</div>
          </div>

          <div class="source-flow">
            <template v-for="(record, index) in sourceRecords" :key="sourceRecordKey(record, index)">
              <span v-if="index" class="flow-operator">＋</span>
              <div class="source-flow-item">来源{{ index + 1 }}<span>{{ record.system || '-' }} · {{ record.id || '-' }}</span></div>
            </template>
            <span v-if="sourceRecords.length" class="flow-operator">→</span>
            <div class="source-flow-item source-flow-result">当前权威主数据<span>{{ current?.name || '-' }}</span></div>
          </div>

          <div class="source-section-title">字段最终取值来自哪里</div>
          <el-table :data="authorityFields" border empty-text="暂无字段来源">
            <el-table-column label="字段" prop="field" min-width="170" />
            <el-table-column label="当前权威值"
                             prop="value"
                             min-width="320"
                             show-overflow-tooltip />
            <el-table-column label="取自来源" min-width="280" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ row.sourceRecordId ? sourceLabel(row) : row.source || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="source-section-title">{{ current?.mode === '多源融合' ? '参与合并的来源数据' : '来源数据' }}</div>
          <div class="source-records">
            <div v-for="(record, index) in sourceRecords"
                 :key="sourceRecordKey(record, index)"
                 class="source-record"
            >
              <div class="source-record-header">
                <div class="source-record-title"><span class="source-record-number">{{ index + 1 }}</span><span>{{ record.system || '-' }}</span></div>
                <div class="source-record-id">{{ record.id || '-' }}</div>
                <div class="source-record-count">贡献 {{ sourceBusinessFields(record).filter(item => item.used).length }} 个当前权威字段</div>
              </div>
              <el-table :data="sourceBusinessFields(record)"
                        border
                        size="small"
                        empty-text="暂无字段">
                <el-table-column label="字段"
                                 prop="field"
                                 min-width="115"
                                 show-overflow-tooltip />
                <el-table-column label="来源值" min-width="170" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.value ?? '-' }}<span v-if="row.used" class="source-used"> ✓ 已采用</span></template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-scrollbar>
        <template #footer>
          <el-button @click="drawerOpen = false">关闭</el-button>
        </template>
      </el-dialog>
      -->
    </div>
  </el-scrollbar>
</template>

<script setup name="AuditSourceTrace">
import AuditOperationCell from '../operationLog/AuditOperationCell.vue'
import { defaultProjectIcon } from '@/utils/imageUrl'
import { listAuditModels, listAuditSources } from '@/api/audit/trace'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import QtTree from '@/components/QtTree/index.vue'
import { usePublishedModelTree } from '@/composables/usePublishedModelTree'
// 详情暂未开放，恢复时取消以下导入注释。
// import { getAuditSource } from '@/api/audit/trace'


const tableRef = ref(null);
const sourceWrapRef = ref(null);
const treeHeight = ref('86vh');
let wrapObserver = null;
let measureFrame = null;
let pageDisposed = false;

function measureSourceWrap() {
  if (measureFrame != null) cancelAnimationFrame(measureFrame);
  measureFrame = requestAnimationFrame(() => {
    measureFrame = null;
    const height = sourceWrapRef.value?.$el?.getBoundingClientRect().height;
    if (height > 0) treeHeight.value = `${Math.ceil(height)}px`;
  });
}

onMounted(async () => {
  await nextTick();
  if (pageDisposed) return;
  const element = sourceWrapRef.value?.$el;
  if (element && typeof ResizeObserver !== 'undefined') {
    wrapObserver = new ResizeObserver(measureSourceWrap);
    wrapObserver.observe(element);
  }
  window.addEventListener('resize', measureSourceWrap);
  measureSourceWrap();
});

onBeforeUnmount(() => {
  pageDisposed = true;
  wrapObserver?.disconnect();
  window.removeEventListener('resize', measureSourceWrap);
  if (measureFrame != null) cancelAnimationFrame(measureFrame);
});
const { proxy } = getCurrentInstance();
// 来源详情暂未开放，恢复时与弹窗一起取消以下注释。
// const drawerOpen = ref(false);
// const current = ref(null);
// const detail = ref({});
// const authorityFields = computed(() => (detail.value.fields || []).filter(isBusinessField));
// const sourceRecords = computed(() => detail.value.records || []);
//
// function isBusinessField(field) {
//   const code = String(field.code || '').replace(/[_-]/g, '').toLowerCase();
//   return !['status', 'lifecyclestatus', 'validflag', 'delflag', 'createtime', 'updatetime'].includes(code)
//       && field.field !== '生命周期状态';
// }
//
// function sourceBusinessFields(record) {
//   return (record.fields || []).filter(isBusinessField);
// }
//
// function sourceRecordKey(record, index) {
//   return record.relationId == null ? `${record.system || ''}-${record.id || ''}-${index}` : `relation-${record.relationId}`;
// }
//
// function sourceRecordIndex(field) {
//   const byRelation = sourceRecords.value.findIndex(record => record.relationId != null
//       && String(record.relationId) === String(field.sourceRelationId));
//   if (byRelation >= 0) return byRelation;
//   return sourceRecords.value.findIndex(record => String(record.id) === String(field.sourceRecordId)
//       && (!field.source || record.system === field.source));
// }
//
// function sourceLabel(field) {
//   const index = sourceRecordIndex(field);
//   const prefix = index < 0 ? '来源' : `来源${index + 1}`;
//   const system = String(sourceRecords.value[index]?.system || field.source || '');
//   const shortName = system.match(/[（(]([A-Za-z0-9_-]+)[）)]/)?.[1]
//       || (/^[A-Za-z0-9_-]+$/.test(system) ? system : '');
//   return `${prefix}：${shortName ? `${shortName}:` : ''}${field.sourceRecordId}`;
// }

const data = reactive({
  queryParams: {
    model: "",
    name: null,
    masterRecordId: null,
    sourceRecordId: null,
    mode: "",
    systems: "",
    timeRange: [],
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：
 *  必须是 reactive 的原始对象，不能传 toRefs 出来的 ref，
 *  否则 qt-table 里 Object.assign({}, store.params, props.params) 展开 ref 只会得到 { value: {...} }，筛选条件全部丢失。
 */
const queryParams = data.queryParams;

function displayValue(value) {
  return value === null || value === undefined || value === '' || value === '—' ? '-' : value;
}

function formatSystemTitle(system) {
  return system.code ? `${system.name} (${system.code})` : system.name;
}

function buildSourceSystems(row) {
  const seen = new Set();
  return (row.records || []).reduce((result, record) => {
    const name = record.system || '未知来源';
    const id = record.sourceSystemId;
    const key = id != null && id !== '' ? `system:${id}` : `name:${name}`;
    if (seen.has(key)) return result;
    seen.add(key);
    result.push({
      name,
      code: record.sourceSystemCode,
      description: record.sourceSystemDescription,
      confidence: record.sourceSystemTrustScore == null ? null : `${record.sourceSystemTrustScore} / 100`,
    });
    return result;
  }, []);
}

async function getSourceList(params) {
  const response = await listAuditSources(params);
  if (Array.isArray(response?.data?.rows)) {
    response.data.rows = response.data.rows.map(row => ({
      ...row, displayName: row.name, displaySystems: buildSourceSystems(row),
    }));
  }
  return response;
}

const tableStore = reactive({
  config: {
    stripe: true,
    table: {
      rowKey: "id",
      stripe: true,
      defaultSort: { prop: "updated", order: "descending" },
    },
  },
  columns: [
    { label: "编号", prop: "sourceRelId", slot: "number", align: "center", width: 90, minWidth: 90, sortable: true,
      sortBy: row => Number(numberFor(row)) || 0 },
    { label: "主数据模型", prop: "model", slot: "modelInfo", align: "left", minWidth: 380, showOverflowTooltip: { effect: "light" } },
    { label: "归属位置", prop: "projectName", slot: "location", align: "left", minWidth: 260, showOverflowTooltip: { effect: "light" } },
    {
      label: "主数据信息",
      prop: "name",
      slot: "name",
      align: "left",
      minWidth: 300,
    },
    { label: "形成方式", prop: "mode", slot: "mode", align: "left", headerAlign: "left", minWidth: 240 },
    { label: "来源构成", prop: "systems", slot: "systems", align: "left", headerAlign: "left", minWidth: 300 },
    { label: "形成时间", prop: "updated", date: '{y}-{m}-{d} {h}:{i}', align: "center", minWidth: 170, sortable: true },
    { label: "操作", slot: "action", width: 140, minWidth: 140, fixed: "right", align: "center" },
  ],
  func: getSourceList,
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
const { data: modelTreeData, loading: modelTreeLoading } = usePublishedModelTree(addTreeKeys)
const defaultExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey))
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
      prop: "masterRecordId",
      component: { is: "input", placeholder: "请输入主数据ID" },
    },
    {
      label: "来源记录标识",
      prop: "sourceRecordId",
      component: { is: "input", placeholder: "请输入来源记录标识" },
    },
    {
      label: "形成方式",
      prop: "mode",
      component: {
        is: "select",
        placeholder: "全部方式",
        options: [
          { value: "", label: "全部方式" },
          { value: "平台录入", label: "平台录入" },
          { value: "多源融合", label: "多源融合" },
          { value: "批量导入", label: "批量导入" },
        ],
      },
    },
    {
      label: "来源系统",
      prop: "systems",
      component: { is: "input", placeholder: "请输入来源系统" },
    },
    {
      label: "形成时间",
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

/** 来源追溯记录数（由表格数据回传，不依赖 qt-table 内部状态） */

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  resetModelTree();
  tableRef.value?.resetQuery();
}

function numberFor(row) {
  // 编号优先用后端给的全局唯一数字（同一模型内记录号会与其他模型重复，不能直接当编号）。
  if (row.sourceRelId !== undefined && row.sourceRelId !== null) return row.sourceRelId;
  if (row.masterRecordId !== undefined && row.masterRecordId !== null) return row.masterRecordId;
  return '-';
}

/** 查看追溯 */
// 恢复详情弹窗时，将 row 参数加回 handleDetail，并恢复 async。
function handleDetail() {
  proxy.$modal.msgWarning('功能正在开发中');

}
</script>

<style lang="scss" scoped>
  .audit-page-scroll {
    height: 100%;
    width: 100%;
  }
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
  .master-info {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .source-system {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;
  }
  /* 操作列：去掉 link 按钮及内部文字的下划线 */
  :deep(.el-button.is-link),
  :deep(.el-button.is-link > span) {
    text-decoration: none !important;
  }

  /* 来源详情暂未开放，恢复时与弹窗一起取消此段注释。
  :deep(.source-detail-dialog .el-dialog__body) {
    padding: 16px 20px;
  }
  :deep(.source-detail-dialog .el-dialog__footer) {
    border-top: 1px solid var(--el-border-color-lighter);
    padding-top: 12px;
  }
  .source-detail-scroll {
    height: min(65vh, 700px);
  }
  .source-overview {
    border: 1px solid #dcdfe6;
    background: #f7f8fa;
    padding: 12px;
  }
  .source-overview-main {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .source-name {
    font-size: 17px;
  }
  .source-overview-sub {
    color: var(--qt-text-secondary);
    font-size: 13px;
    line-height: 22px;
  }
  .source-flow {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    min-height: 92px;
    overflow-x: auto;
    margin: 16px 0 20px;
    padding: 16px;
    border: 1px solid #dcdfe6;
    background: #f7f8fa;
  }
  .source-flow-item {
    flex: 0 0 auto;
    min-width: 120px;
    padding: 8px 10px;
    border: 1px solid #dcdfe6;
    background: var(--qt-surface-background);
    text-align: center;
    font-size: 12px;
    line-height: 20px;
  }
  .source-flow-item span {
    display: block;
    white-space: nowrap;
  }
  .source-flow-result {
    min-width: 160px;
  }
  .flow-operator {
    flex: 0 0 auto;
    font-size: 22px;
  }
  .source-section-title {
    display: flex;
    align-items: center;
    gap: var(--qt-title-marker-gap);
    margin: 20px 0 12px;
    font-size: 15px;
    font-weight: 600;
    line-height: 20px;
  }
  .source-section-title::before {
    content: '';
    flex: 0 0 var(--qt-title-marker-width);
    width: var(--qt-title-marker-width);
    height: var(--qt-title-marker-height);
    border-radius: var(--qt-title-marker-radius);
    background: var(--qt-title-marker-color);
  }
  .source-records {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }
  .source-record {
    min-width: 0;
    border: 1px solid #dcdfe6;
  }
  .source-record-header {
    padding: 9px 10px;
    background: #f7f8fa;
  }
  .source-record-title {
    display: flex;
    align-items: center;
    gap: 5px;
    font-weight: 600;
  }
  .source-record-number {
    display: inline-flex;
    width: 18px;
    height: 18px;
    align-items: center;
    justify-content: center;
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    font-size: 12px;
  }
  .source-record-id, .source-record-count {
    margin-top: 3px;
    color: var(--qt-text-secondary);
    font-size: 12px;
  }
  .source-used {
    color: var(--qt-text-secondary);
    font-size: 12px;
    white-space: nowrap;
  }
  :deep(.source-record .el-table) {
    --el-table-border-color: #dcdfe6;
  }
  @media (max-width: 900px) { .source-records { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
  @media (max-width: 640px) { .source-records { grid-template-columns: minmax(0, 1fr); } }
  */
</style>
