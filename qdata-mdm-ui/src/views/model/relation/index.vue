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
  <div class="app-container hierarchy-relation-page">
    <guide-tip tip-id="model/relation.list" />
    <el-container class="relation-layout">
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @tree-change="handleModelTreeChange"
        @node-click="handleTreeClick"
      />
      <el-main class="main-content">
        <qt-wrap :config="wrapConfig">
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="queryParams"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>

          <div ref="graphHostRef" class="graph-host">
            <model-relation-graph
              :query="graphQuery"
              :height="graphHeight+'px'"
            />
          </div>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="ModelHierarchyRelation">
/** @description 主数据模型层级关系查询页面。 */
import { getPublishedModelTree } from "@/api/model/base/base";
import QtTree from "@/components/QtTree/index.vue";
import ModelRelationGraph from "@/views/model/components/ModelRelationGraph/index.vue";

const createQueryParams = () => ({
  id: null,
  layerId: null,
  name: null,
  code: null,
  projectId: null,
  relationType: null,
});

const queryParams = reactive(createQueryParams());
const graphQuery = ref({});
const modelTreeRef = ref();
const modelTreeData = ref([]);
const modelTreeLoading = ref(false);
const selectedTreeKey = ref(null);
let modelTreeRequestSequence = 0;
const graphHeight = ref(640);
const graphHostRef = ref();
let graphResizeObserver;

const wrapConfig = {
  fullContent: false,
  actions: { show: false },
};

const relationTypeOptions = [
  { label: "全部关系", value: null },
  { label: "主子关系", value: "CHILD" },
  { label: "引用关系", value: "REFERENCE" },
];

const searchStore = reactive({
  items: [
    { label: "模型名称", prop: "name", component: { is: "input" } },
    { label: "模型编码", prop: "code", component: { is: "input" } },
    {
      label: "关系类型",
      prop: "relationType",
      component: {
        is: "select",
        placeholder: "请选择关系类型",
        options: relationTypeOptions,
      },
    },
  ],
});

/** 复用权威主数据入口的模型树，保留层级并添加跨类型唯一 key。 */
async function loadModelTree() {
  const sequence = ++modelTreeRequestSequence;
  modelTreeLoading.value = true;
  try {
    const response = await getPublishedModelTree();
    const mapNodes = nodes => nodes.map(node => ({
      ...node,
      treeKey: `${node.nodeType}:${node.id}`,
      children: mapNodes(node.children || []),
    }));
    if (sequence === modelTreeRequestSequence) modelTreeData.value = mapNodes(response.data || []);
  } finally {
    if (sequence === modelTreeRequestSequence) modelTreeLoading.value = false;
  }
}

/** 切换节点时清除上一节点的范围，模型节点使用关系接口的起始模型 id。 */
function handleTreeClick(node) {
  selectedTreeKey.value = node.treeKey ?? null;
  queryParams.id = node.modelId ?? null;
  queryParams.projectId = node.projectId ?? null;
  queryParams.layerId = node.layerId ?? null;
  modelTreeRef.value?.setCurrentKey(selectedTreeKey.value);
  handleQuery();
}

/** 树更新后恢复选择；节点已失效时回到全部关系。 */
async function handleModelTreeChange(nodes) {
  function findNode(rows) {
    for (const node of rows) {
      if (node.treeKey === selectedTreeKey.value) return node;
      const found = findNode(node.children || []);
      if (found) return found;
    }
  }
  const selected = findNode(nodes);
  if (selectedTreeKey.value !== null && !selected) handleTreeClick({});
  await nextTick();
  modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false);
}

/** 整理模型关系查询参数，排除空值。 */
function normalizeQuery() {
  return Object.fromEntries(
    Object.entries(queryParams).filter(([, value]) => value !== null && value !== undefined && value !== ""),
  );
}

/** 按当前条件查询模型关系。 */
function handleQuery() {
  graphQuery.value = normalizeQuery();
}

/** 清空模型关系查询条件并重新查询。 */
function resetQuery() {
  Object.assign(queryParams, createQueryParams());
  selectedTreeKey.value = null;
  modelTreeRef.value?.resetTree();
  graphQuery.value = {};
}

onMounted(() => {
  loadModelTree().catch(() => {});
  // flex 布局扣除提示条、查询栏及页边距后，剩余空间就是图谱高度。
  graphResizeObserver = new ResizeObserver(entries => {
    const height = Math.floor(entries[0]?.contentRect.height || 0);
    if (height > 0) graphHeight.value = height;
  });
  graphResizeObserver.observe(graphHostRef.value);
});

onBeforeUnmount(() => {
  modelTreeRequestSequence++;
  graphResizeObserver?.disconnect();
});
</script>

<style scoped lang="scss">
.hierarchy-relation-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 90px);
  min-height: 360px;

  .hasTagsView & {
    height: calc(100vh - 124px);
  }

  .relation-layout {
    flex: 1;
    min-height: 0;
  }

  .main-content {
    min-width: 0;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.guide-tip),
  :deep(.qt-wrap--search) {
    flex-shrink: 0;
  }

  :deep(.qt-wrap),
  :deep(.qt-wrap--content) {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
  }

  :deep(.qt-wrap--main) {
    flex: 1;
    min-height: 0;
  }
}

.graph-host {
  height: 100%;
  overflow: hidden;
}
</style>
