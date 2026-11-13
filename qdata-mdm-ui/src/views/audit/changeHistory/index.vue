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
  @file changeHistory/index.vue
  @description 变更历史列表
  @author zyy
-->
<template>
  <div ref="app-container" class="app-container">
    <guide-tip tip-id="audit/change-history.list" />
    <el-container class="audit-tree-layout">
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :editable="false"
        :default-expand="false"
        :default-expanded-keys="defaultExpandedKeys"
        node-key="treeKey"
        wrap-height-class=".change-history-wrap"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      />
      <el-main class="main-content audit-tree-main change-history-main">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef" class="change-history-wrap">
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
              title="本页展示主数据的变更历史，包括数据内容和生命周期变化，便于查看变更过程及处理结果。"
            />
          </template>

          <qt-table v-bind="tableStore" ref="tableRef">
            <template #masterData="{ row }">
              <div :title="`${row.name && row.name !== '—' ? row.name : '-'}（${row.businessCode || '-'}）`" class="cell-main master-info">
                {{ row.name && row.name !== '—' ? row.name : '-' }}（{{ row.businessCode || '-' }}）
              </div>
            </template>
            <template #modelInfo="{ row }">
              <audit-operation-cell kind="model" :row="row" model-hint="模型信息取自当前模型资料，版本为变更记录关联版本" />
            </template>
            <template #location="{ row }">
              <audit-operation-cell kind="location" :row="row" />
            </template>
            <template #changeInfo="{ row }">
              <div class="change-info">
                <dict-tag v-if="row.type" :value="row.type" type="primary" /><span v-else>-</span>
                <dict-tag v-if="row.result" :type="row.result === '已生效' ? 'success' : 'danger'">{{ row.result }}</dict-tag><span v-else>-</span>
              </div>
            </template>
            <template #operator="{ row }">
              <div class="cell-main">{{ row.operatorName || '-' }}</div>
              <div class="cell-sub">{{ row.operatorPhone || '-' }}</div>
            </template>
            <template #action="{ row }">
              <el-button
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

<script setup name="AuditChangeHistory">
import AuditOperationCell from '../operationLog/AuditOperationCell.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { listAuditChanges, listAuditModels } from '@/api/audit/trace'
import QtTree from '@/components/QtTree/index.vue'
import { usePublishedModelTree } from '@/composables/usePublishedModelTree'

// 变更历史详情暂未开放，恢复时取消以下导入注释。
// import { useRouter } from 'vue-router'

const tableRef = ref(null);
const { proxy } = getCurrentInstance();
// const router = useRouter();

const data = reactive({
  queryParams: {
    model: "",
    name: null,
    code: null,
    type: "",
    category: "",
    result: "",
    timeRange: [],
  },
});

/** 传给 qt-table / qt-search-bar 的参数对象：
 *  必须是 reactive 的原始对象，不能传 toRefs 出来的 ref，
 *  否则 qt-table 里 Object.assign({}, store.params, props.params) 展开 ref 只会得到 { value: {...} }，筛选条件全部丢失。
 */
const queryParams = data.queryParams;

async function getChangeList(params) {
  return listAuditChanges(params);
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
    { label: "编号", prop: "recordId", align: "center", headerAlign: "center", width: 90, minWidth: 90, sortable: true },
    { label: "主数据模型", prop: "model", slot: "modelInfo", align: "left", headerAlign: "left", minWidth: 400, showOverflowTooltip: { effect: "light" } },
    { label: "归属位置", prop: "projectName", slot: "location", align: "left", headerAlign: "left", minWidth: 200, showOverflowTooltip: { effect: "light" } },
    { label: "主数据", prop: "name", slot: "masterData", align: "left", minWidth: 260 },
    { label: "变更信息", prop: "type", slot: "changeInfo", align: "left", minWidth: 200 },
    { label: "操作人", prop: "operatorName", slot: "operator", align: "left", minWidth: 160 },
    { label: "操作时间", prop: "time", date: "{y}-{m}-{d} {h}:{i}", align: "center", minWidth: 160, sortable: true },
    { label: "操作", slot: "action", width: 140, minWidth: 140, fixed: "right", align: "center" },
  ],
  func: getChangeList,
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
      prop: "code",
      component: { is: "input", placeholder: "请输入主数据ID" },
    },
    {
      label: "变化范围",
      prop: "category",
      component: {
        is: "select",
        placeholder: "全部范围",
        options: [
          { value: "", label: "全部范围" },
          { value: "数据内容变化", label: "数据内容变化" },
          { value: "生命周期变化", label: "生命周期变化" },
        ],
      },
    },
    {
      label: "变更类型",
      prop: "type",
      component: {
        is: "select",
        placeholder: "全部类型",
        options: [
          { value: "", label: "全部类型" },
          ...['新增', '修改', '复制', '历史版本恢复', '多源融合', '冻结', '解冻', '失效', '恢复', '归档', '字段变更', '生命周期变更'].map(value => ({ value, label: value })),
        ],
      },
    },
    {
      label: "变更结果",
      prop: "result",
      component: {
        is: "select",
        placeholder: "全部结果",
        options: [
          { value: "", label: "全部结果" },
          { value: "已生效", label: "已生效" },
          { value: "已驳回", label: "已驳回" },
          { value: "已撤回", label: "已撤回" },
          { value: "执行失败", label: "执行失败" },
        ],
      },
    },
    {
      label: "操作时间",
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

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  resetModelTree();
  tableRef.value?.resetQuery();
}

/** 查看详情 */
// 恢复详情跳转时，将 row 参数加回 handleDetail。
function handleDetail() {
  proxy.$modal.msgWarning('功能正在开发中');
  // 详情跳转暂时停用，恢复时取消以下注释。
  /*
  router.push({
    path: '/audit/changeHistory/detail',
    query: { id: row.id },
  });
  */
}
</script>

<style lang="scss" scoped>
.cell-main {
  line-height: 20px;
}

.master-info {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.change-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

/* 操作列：去掉 link 按钮及内部文字的下划线 */
:deep(.el-button.is-link),
:deep(.el-button.is-link > span) {
  text-decoration: none !important;
}

</style>
