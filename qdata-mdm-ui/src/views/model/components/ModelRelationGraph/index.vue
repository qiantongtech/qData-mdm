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
  <div ref="containerRef" v-loading="loading" class="relation-graph">
    <div ref="toolbarRef" class="toolbar">
      <div class="view-controls" role="group" aria-label="画布工具">
        <div v-for="group in toolGroups" :key="group.name" class="tool-group" role="group" :aria-label="group.name">
          <el-tooltip v-for="tool in group.tools" :key="tool.icon" :content="tool.label"
                      placement="bottom" :teleported="!isFullscreen"
          >
            <span class="tool-trigger">
              <el-button class="canvas-tool" :aria-label="tool.label" :disabled="tool.disabled" @click="tool.run">
                <i class="iconfont" :class="[tool.icon, {'graph-line-icon': !tool.usePlatformFont}]" aria-hidden="true"/>
              </el-button>
            </span>
          </el-tooltip>
        </div>
      </div>
      <el-select
          class="model-locator"
          :model-value="selectedNodeId"
          aria-label="在画布中定位模型"
          placeholder="按模型名称或编码定位"
          filterable
          clearable
          default-first-option
          :filter-method="filterModels"
          :disabled="loading || !hasNodes"
          :teleported="!isFullscreen"
          no-match-text="未找到匹配的模型"
          @update:model-value="locateModel"
          @visible-change="filterModels('')"
      >
        <template #prefix>
          <img v-if="selectedModel" class="model-type-icon" :src="selectedModel.type.icon" :alt="selectedModel.type.text" :title="selectedModel.type.text"/>
          <el-icon v-else><Search/></el-icon>
        </template>
        <el-option v-for="model in matchingModels" :key="model.id" :value="model.id" :label="model.name">
          <img class="model-type-icon model-option-icon" :src="model.type.icon" :alt="model.type.text" :title="model.type.text"/>
          <span>{{ model.name }}</span>
          <span class="model-option-code">{{ model.code }}</span>
        </el-option>
        <template #empty><div class="model-no-match">未找到匹配的模型</div></template>
      </el-select>
    </div>
    <div ref="viewportRef" class="graph-viewport">
      <div ref="graphContainerRef" class="relation-graph__canvas" tabindex="0" role="region" aria-label="模型关系画布"
           @keydown="handleCanvasKey" @wheel="panWithWheel"
      />
    </div>
    <div v-if="edgeDetail" ref="edgeTipRef" class="edge-detail" role="tooltip"
         :style="{left: tipPosition.x + 'px', top: tipPosition.y + 'px'}"
         @mouseenter="cancelTipClose" @mouseleave="hideEdgeDetail"
    >
      <strong>{{ edgeDetail.source }} → {{ edgeDetail.target }}</strong>
      <dl>
        <template v-for="group in edgeDetail.groups" :key="group.type">
          <dt>关系类型</dt><dd>{{ group.type }}</dd>
          <dt>对应字段</dt><dd>{{ group.fields.join('、') }}</dd>
        </template>
      </dl>
    </div>
    <div v-if="!loading && !graphData.nodes.length" class="emptyBg relation-graph__empty">
      <img class="empty-state-image" alt=""/>
      <p>{{ loadFailed ? "模型关系加载失败" : "暂无模型关系" }}</p>
      <el-button v-if="loadFailed" link type="primary" @click="loadGraph">重新加载</el-button>
    </div>

    <div class="legend-container" :class="{ collapsed: !legendExpanded }">
      <button type="button" class="legend-header" :aria-expanded="legendExpanded" @click="legendExpanded = !legendExpanded">
        <span class="legend-title">图例</span>
        <span class="legend-toggle-btn" :class="{ rotate: legendExpanded }"></span>
      </button>
      <div v-show="legendExpanded" class="legend-content">
        <div v-for="item in modelLegend" :key="item.text" class="legend-item">
          <img :src="item.icon" alt="" class="legend-icon"/>
          <span>{{ item.text }}</span>
        </div>
        <div class="legend-item">
          <span class="edge-legend edge-legend--sub"></span>
          <span>子模型关系（固定1:N）</span>
        </div>
        <div class="legend-item">
          <span class="edge-legend edge-legend--class"></span>
          <span>分类模型引用（指向被引用模型）</span>
        </div>
        <div class="legend-item">
          <span class="edge-legend edge-legend--normal"></span>
          <span>普通模型引用（指向被引用模型）</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="ModelRelationGraph">

import {useDict} from "@/utils/dict";

/**
 * @description 模型关系图，负责关系查询、布局、缩放与导出。
 * @prop {[String, Number]} modelId 当前模型 ID
 * @prop {Object} query 模型关系查询条件
 * @prop {Number} height height
 */
import {useCssVars} from "vue";

import {layoutRelations} from "./layout";
import {useFullscreen} from "@vueuse/core";
import {Search} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";
import {Graph} from "@antv/x6";
import {Export} from "@antv/x6-plugin-export";

import {getModelRelationGraph} from "@/api/model/base/base";
import classModelIcon from "@/assets/common/icons/model/class-model.svg";
import normalModelIcon from "@/assets/common/icons/model/normal-model.svg";
import subModelIcon from "@/assets/common/icons/model/sub-model.svg";
import modelIconSource from "@/assets/common/icons/model/model.svg?raw";
import codeIconSource from "@/assets/common/icons/model/code.svg?raw";

const {model_type, model_status} = useDict('model_type', 'model_status');

const props = defineProps({
  modelId: {type: [String, Number], default: null},
  query: {type: Object, default: () => ({})},
  height: {type: String, default: '640px'},
});

useCssVars(() => ({
  "model-relation-graph-height": `${props.height}`,
}));

const containerRef = ref();
const viewportRef = ref();
const toolbarRef = ref();
const graphContainerRef = ref();
const edgeTipRef = ref();
const edgeDetail = shallowRef(null);
const tipPosition = reactive({x: 0, y: 0});
let tipCloseTimer;
const loading = ref(false);
const legendExpanded = ref(true);
const graphData = reactive({nodes: [], edges: []});
let graph;
const selectedNodeId = ref(null);
const modelKeyword = ref("");
let resizeObserver;
let requestSequence = 0;
const loadFailed = ref(false);
const {isFullscreen, toggle: toggleFullscreen, isSupported: fullscreenSupported} = useFullscreen(containerRef, {autoExit: true});

const NODE_WIDTH = 180;
const NODE_HEIGHT = 128;
const MIN_SCALE = 0.35;
const MAX_SCALE = 1.8;
const INITIAL_SCALE = 0.85;

const scale = ref(INITIAL_SCALE);
const hasNodes = computed(() => graphData.nodes.length > 0);
const toolGroups = computed(() => [
  {
    name: "缩放",
    tools: [
      {label: "放大", icon: "icon-zoom-in-line", disabled: !hasNodes.value || scale.value >= MAX_SCALE, run: () => zoomGraph(0.2)},
      {label: "缩小", icon: "icon-zoom-out-line", disabled: !hasNodes.value || scale.value <= MIN_SCALE, run: () => zoomGraph(-0.2)},
      {label: "实际大小（1:1）", icon: "icon-checkbox-blank-line", disabled: !hasNodes.value, run: () => graph?.zoomTo(1)},
    ],
  },
  {
    name: "视图",
    tools: [
      {label: "查看全图", icon: "icon-layout-4-line", disabled: !hasNodes.value, run: fitGraph},
      {label: "恢复初始视图", icon: "icon-home-4-line", disabled: !hasNodes.value, run: resetView},
      {label: isFullscreen.value ? "退出全屏" : "全屏", icon: isFullscreen.value ? "icon-fullscreen-exit-line" : "icon-a-quanpingxianxing",
        usePlatformFont: true, disabled: !fullscreenSupported.value, run: changeFullscreen},
    ],
  },
  {
    name: "刷新",
    tools: [{label: "刷新关系数据", icon: "icon-a-shuaxinxianxing", usePlatformFont: true, disabled: loading.value, run: loadGraph}],
  },
  {
    name: "导出",
    tools: [{label: "导出全图为 PNG", icon: "icon-download-line", disabled: !hasNodes.value, run: exportGraph}],
  },
]);

const TYPE_STYLES = {
  normal: {text: "普通模型", color: "#2666fb", background: "#edf5ff", icon: normalModelIcon},
  class: {text: "分类模型", color: "#f69008", background: "#fff3e2", icon: classModelIcon},
  sub: {text: "子模型", color: "#12b369", background: "#eff9f6", icon: subModelIcon},
};

const modelLegend = [TYPE_STYLES.class, TYPE_STYLES.normal, TYPE_STYLES.sub];

/** 将 SVG 主色替换为节点类型颜色，再编码为可供 X6 图片节点使用的地址。 */
function svgDataUri(source, color) {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(source.replaceAll("#2666FB", color))}`;
}

/** 查找字典项，兼容接口与字典值在类型、大小写和首尾空格上的差异。 */
function findDictOption(options, value) {
  const normalizedValue = String(value ?? "").trim().toUpperCase();
  // 先直接匹配，再将双方转为统一格式比较；字典尚未加载时按空列表处理。
  return (options || []).find(item => (
      item.value === value
      || String(item.value ?? "").trim().toUpperCase() === normalizedValue
  ));
}

/** 优先从父子关系识别子模型，再用固定类型选项区分分类模型与普通模型。 */
function nodeType(node) {
  const nodeId = String(node.id);
  const parentId = String(node.parentId ?? "");
  // 筛选指向当前节点的关系，并统一关系类型大小写，用于识别 CHILD 关系。
  const incomingTypes = graphData.edges
      .filter(edge => String(edge.target) === nodeId)
      .map(edge => String(edge.type).toUpperCase());

  // 子模型标记、非空且非顶级的父 ID、指向当前节点的 CHILD 关系，任一满足即视为子模型。
  if (node.subModel || (parentId && parentId !== "0") || incomingTypes.includes("CHILD")) return "sub";

  const typeValue = node.type ?? node.modelType;
  if (typeValue === 'CATEGORY') return "class";

  return "normal";
}

/** 从系统字典获取状态名称，未匹配时由调用方决定占位显示。 */
function statusLabel(status) {
  return findDictOption(model_status.value, status)?.label || "";
}

/** 生成卡片中的模型类型名称，子模型关系优先于接口中的类型字段。 */
function modelTypeLabel(node) {
  if (nodeType(node) === "sub") return "子模型";
  return findDictOption(model_type.value, node.type ?? node.modelType)?.label || "未配置模型类型";
}

/** 按优先级取首个有效字段值，统一转为去除首尾空格的字符串。 */
function firstNonEmpty(...values) {
  // 排除 null、undefined 和空白文本后再转换；全部为空时返回空字符串。
  return values.find(value => value !== null && value !== undefined && String(value).trim())?.toString().trim() || "";
}

/** X6 对无空格的长英文换行时可能返回空文本，因此先保留可见前缀并补充省略号。 */
function ellipsisText(text, maxWidth = 124, fontSize = 12) {
  const value = String(text || "");
  // 粗略按全角和半角字符估算宽度，避免依赖浏览器文本测量。
  const charWidth = char => (char.charCodeAt(0) > 255 ? fontSize : fontSize * 0.64);
  // 累加字符宽度；能完整容纳时直接保留原文。
  if (Array.from(value).reduce((width, char) => width + charWidth(char), 0) <= maxWidth) return value;

  const ellipsis = "...";
  const ellipsisWidth = ellipsis.length * fontSize * 0.64;
  let width = 0;
  let visibleText = "";
  for (const char of value) {
    const nextWidth = width + charWidth(char);
    // 提前预留省略号宽度，防止截断后的文本仍超出卡片边界。
    if (nextWidth + ellipsisWidth > maxWidth) break;
    visibleText += char;
    width = nextWidth;
  }
  return `${visibleText}${ellipsis}`;
}

/** 兼容不同接口字段名称，生成卡片的模型编码、所属数据域和类型说明。 */
function modelCode(node) {
  return firstNonEmpty(node.code, node.modelCode, node.model_code, node.model?.code);
}

function nodeDisplayInfo(node) {
  return {
    code: modelCode(node) || "未配置模型编码",
    meta: `${firstNonEmpty(node.projectName, node.domainName) || "未配置数据域"} · ${modelTypeLabel(node)}`,
  };
}

// 注册统一的模型卡片形状；具体文字、图标和颜色在 createNodeAttrs 中按节点填充。
Graph.registerNode("model-relation-card", {
  inherit: "rect",
  width: NODE_WIDTH,
  height: NODE_HEIGHT,
  markup: [
    {tagName: "rect", selector: "body"},
    {tagName: "image", selector: "typeIcon"},
    {tagName: "text", selector: "title"},
    {tagName: "rect", selector: "content"},
    {tagName: "image", selector: "codeIcon"},
    {tagName: "text", selector: "code"},
    {tagName: "image", selector: "modelIcon"},
    {tagName: "text", selector: "meta"},
    {tagName: "rect", selector: "versionBody"},
    {tagName: "text", selector: "version"},
    {tagName: "rect", selector: "statusBody"},
    {tagName: "text", selector: "status"},
  ],
  attrs: {
    body: {class: "relation-card-body", width: NODE_WIDTH, height: NODE_HEIGHT, rx: 3, ry: 3, stroke: "none"},
    typeIcon: {x: 9, y: 9, width: 15, height: 18},
    title: {
      refX: 31, refY: 18, fill: "#202124", fontSize: 14, fontWeight: 600,
      textAnchor: "start", textVerticalAnchor: "middle",
      textWrap: {width: 139, height: 20, ellipsis: true},
    },
    content: {x: 8, y: 35, width: 164, height: 85, rx: 3, ry: 3, fill: "#fff", stroke: "none"},
    codeIcon: {x: 18, y: 46, width: 14, height: 14},
    code: {
      refX: 39, refY: 53, fill: "#909399", fontSize: 12,
      textAnchor: "start", textVerticalAnchor: "middle",
    },
    modelIcon: {x: 18, y: 69, width: 14, height: 14},
    meta: {
      refX: 39, refY: 76, fill: "#909399", fontSize: 12,
      textAnchor: "start", textVerticalAnchor: "middle",
      textWrap: {width: 124, height: 18, ellipsis: true},
    },
    versionBody: {x: 18, y: 93, width: 46, height: 18, rx: 2, ry: 2, strokeWidth: 1},
    version: {refX: 41, refY: 102, fontSize: 12, textAnchor: "middle", textVerticalAnchor: "middle"},
    statusBody: {x: 72, y: 93, width: 48, height: 18, rx: 2, ry: 2, fill: "#f5f5f5", stroke: "#dedede"},
    status: {refX: 96, refY: 102, fill: "#909399", fontSize: 12, textAnchor: "middle", textVerticalAnchor: "middle"},
  },
}, true);

/** 根据模型类型、版本和状态生成卡片属性，并为缺失信息提供占位显示。 */
function createNodeAttrs(node) {
  const style = TYPE_STYLES[nodeType(node)];
  // 当前节点未提供版本信息时沿用父模型信息，版本号统一只保留一个 V 前缀。
  const versionNo = firstNonEmpty(node.versionNo, node.parentVersionNo);
  const versionStatus = node.versionStatus ?? node.parentVersionStatus;
  const version = versionNo ? `V${versionNo.replace(/^V/i, "")}` : "-";
  const status = statusLabel(versionStatus) || "-";
  const displayInfo = nodeDisplayInfo(node);

  return {
    body: {fill: style.background},
    typeIcon: {xlinkHref: style.icon},
    title: {text: node.name || "未命名模型"},
    codeIcon: {xlinkHref: svgDataUri(codeIconSource, style.color)},
    // 英文编码由前端预先截断，避免 X6 textWrap 将无空格长文本整段裁空。
    code: {text: ellipsisText(displayInfo.code, 128, 12)},
    modelIcon: {xlinkHref: svgDataUri(modelIconSource, style.color)},
    meta: {text: ellipsisText(displayInfo.meta)},
    versionBody: {fill: style.background, stroke: `${style.color}33`},
    version: {text: version, fill: style.color},
    // 无有效状态时同时隐藏状态底色和文字，避免显示空标签。
    statusBody: {opacity: status === "-" ? 0 : 1},
    status: {text: status, opacity: status === "-" ? 0 : 1},
  };
}

/** 按目标模型类型设置关系线样式，与图例中的颜色和实虚线对应。 */
function edgeVisualStyle(edge, nodeMap) {
  const targetNode = nodeMap.get(String(edge.target));
  const targetType = targetNode ? nodeType(targetNode) : "normal";

  if (targetType === "sub") return {color: TYPE_STYLES.sub.color, dashed: true};
  if (targetType === "class") return {color: TYPE_STYLES.class.color, dashed: false};
  return {color: TYPE_STYLES.normal.color, dashed: true};
}

function describeRelations(relations, nodeMap) {
  const first = relations[0];
  const source = nodeMap.get(String(first.source));
  const target = nodeMap.get(String(first.target));
  const groups = new Map();
  for (const relation of relations) {
    const kind = String(relation.type || "").toUpperCase();
    const child = kind === "CHILD";
    const classification = kind === "CLASS" || (kind === "REFERENCE" && nodeType(target) === "class");
    const type = child ? "主子模型关系（1:N）" : classification ? "分类模型引用" : "普通模型引用";
    if (!groups.has(type)) groups.set(type, {type, fields: []});
    const field = child ? "无（模型层级关系）" : firstNonEmpty(
      relation.label?.replace(/^引用\s*[：:]\s*/, ""), relation.fieldId != null ? `字段 ID ${relation.fieldId}` : "未提供",
    );
    groups.get(type).fields.push(field);
  }
  return {
    source: source.name || nodeDisplayInfo(source).code,
    target: target.name || nodeDisplayInfo(target).code,
    groups: Array.from(groups.values()),
  };
}

function cancelTipClose() {
  clearTimeout(tipCloseTimer);
}

function hideEdgeDetail() {
  cancelTipClose();
  edgeDetail.value = null;
}

async function showEdgeDetail({edge, e}) {
  cancelTipClose();
  const detail = edge.getData().detail;
  edgeDetail.value = detail;
  const bounds = containerRef.value.getBoundingClientRect();
  const x = e.clientX - bounds.left, y = e.clientY - bounds.top;
  await nextTick();
  if (edgeDetail.value !== detail || !edgeTipRef.value) return;
  const {offsetWidth, offsetHeight} = edgeTipRef.value;
  tipPosition.x = Math.max(4, Math.min(x + 8, bounds.width - offsetWidth - 4));
  tipPosition.y = Math.max(4, y + 8 + offsetHeight <= bounds.height ? y + 8 : y - offsetHeight - 8);
}

/** 只标记当前节点及其一跳邻居，两个邻居之间的其他关系仍然淡化。 */
function highlightRelations(node = null) {
  const id = node?.id ?? null;
  if (id === selectedNodeId.value) return;
  selectedNodeId.value = id;
  const related = new Set();
  if (node) {
    for (const edge of graph.getConnectedEdges(node)) {
      related.add(edge.id);
      related.add(edge.getSourceCellId());
      related.add(edge.getTargetCellId());
    }
  }
  for (const cell of graph.getCells()) {
    if (!node) {
      cell.attr("root/data-relation-focus", "idle");
    } else {
      const focus = cell.id === id ? "selected" : related.has(cell.id) ? "related" : "dimmed";
      cell.attr("root/data-relation-focus", focus);
    }
  }
}

const modelOptions = computed(() => graphData.nodes.map(node => ({
  id: String(node.id),
  type: TYPE_STYLES[nodeType(node)],
  name: node.name || "未命名模型",
  code: modelCode(node),
})));

const selectedModel = computed(() => modelOptions.value.find(model => model.id === selectedNodeId.value));

const matchingModels = computed(() => modelOptions.value.filter(model =>
  model.name.toLowerCase().includes(modelKeyword.value) || model.code.toLowerCase().includes(modelKeyword.value)
));

function filterModels(keyword) {
  modelKeyword.value = keyword.trim().toLowerCase();
}

function locateModel(id) {
  const node = id == null || id === "" ? null : graph?.getCellById(id);
  highlightRelations(node);
  if (node) graph.centerCell(node);
}

/** 清空旧图，重新布局并绘制模型卡片与关系线，最后恢复初始视角。 */
function renderGraph() {
  if (!graph) return;
  selectedNodeId.value = null;
  filterModels("");
  graph.clearCells();
  if (!graphData.nodes.length) return;

  const {positions, routes} = layoutRelations(graphData.nodes, graphData.edges, NODE_WIDTH, NODE_HEIGHT);
  const nodeMap = new Map(graphData.nodes.map(node => [String(node.id), node]));

  graphData.nodes.forEach(node => {
    const position = positions.get(String(node.id)) || {x: 40, y: 40};
    graph.addNode({
      id: String(node.id),
      shape: "model-relation-card",
      x: position.x,
      y: position.y,
      zIndex: 2,
      attrs: createNodeAttrs(node)
    });
  });

  // 相同方向的字段引用共用布局路径，绘制一次并保留该路径对应的全部字段。
  const relationsByRoute = new Map();
  graphData.edges.forEach(relation => {
    const route = routes.get(relation.id);
    if (!route) return;
    if (!relationsByRoute.has(route)) relationsByRoute.set(route, []);
    relationsByRoute.get(route).push(relation);
  });
  relationsByRoute.forEach((relations, route) => {
    const edge = relations[0];
    const {color, dashed} = edgeVisualStyle(edge, nodeMap);
    graph.addEdge({
      id: edge.id,
      ...route,
      data: {detail: describeRelations(relations, nodeMap)},
      connector: {name: "rounded", args: {radius: 8}},
      zIndex: 1,
      attrs: {
        wrap: {strokeWidth: 10, vectorEffect: "non-scaling-stroke"},
        line: {
          class: "relation-edge-line",
          stroke: color, strokeWidth: 1.2, strokeDasharray: dashed ? "5 4" : "",
          targetMarker: {name: "block", width: 7, height: 7, fill: color, stroke: color},
        }
      },
    });
  });

  // 等待本轮界面更新后设置缩放与居中，统一每次加载后的视角。
  nextTick(resetView);
}

/** 按模型或筛选条件加载关系图，并补充节点可回退使用的父模型信息。 */
async function loadGraph() {
  hideEdgeDetail();
  const sequence = ++requestSequence;
  loading.value = true;
  loadFailed.value = false;
  try {
    const query = {...props.query};
    // 仅在提供有效模型 ID 时覆盖筛选条件中的 id，避免将空值传给接口。
    if (props.modelId !== null && props.modelId !== undefined && props.modelId !== "") {
      query.id = props.modelId;
    }
    const response = await getModelRelationGraph(query);
    if (sequence !== requestSequence) return;
    graphData.nodes = response.data?.nodes || [];
    // 构建 ID 索引，便于逐个查找父模型，不必为每个节点重复扫描完整列表。
    const nodeMap = new Map(graphData.nodes.map(node => [String(node.id), node]));
    graphData.nodes.forEach(node => {
      const parent = nodeMap.get(String(node.parentId));
      node.parentName = parent?.name;
      node.parentProjectName = firstNonEmpty(parent?.projectName, parent?.domainName);
      node.parentVersionNo = parent?.versionNo;
      node.parentVersionStatus = parent?.versionStatus;
    });
    graphData.edges = response.data?.edges || [];
    renderGraph();
  } catch {
    if (sequence !== requestSequence) return;
    loadFailed.value = true;
    graphData.nodes = [];
    graphData.edges = [];
    renderGraph();
  } finally {
    if (sequence === requestSequence) loading.value = false;
  }
}

/** 在当前缩放比例上增减，并限制在手动缩放的上下限之间。 */
function zoomGraph(delta) {
  if (!graph || !hasNodes.value) return;
  graph.zoomTo(Math.min(MAX_SCALE, Math.max(MIN_SCALE, graph.zoom() + delta)));
}

/** 从左侧开始阅读，按原始比例显示卡片，超出画布的部分通过平移查看。 */
function resetView() {
  if (!graph || !graphData.nodes.length || !containerRef.value) return;
  const {clientWidth: width, clientHeight: height} = graphContainerRef.value;
  if (!width || !height) return;
  const top = toolbarRef.value.offsetTop + toolbarRef.value.offsetHeight + 12;
  const bounds = graph.getContentArea();
  const x = Math.max(24, (width - bounds.width * INITIAL_SCALE) / 2);
  const y = Math.max(top, (height - bounds.height * INITIAL_SCALE) / 2);
  graph.zoomTo(INITIAL_SCALE);
  graph.translate(x - bounds.x * INITIAL_SCALE, y - bounds.y * INITIAL_SCALE);
}

/** 将完整关系图导出为白底 PNG，并为图内容预留边距。 */
function exportGraph() {
  if (!graph || !graphData.nodes.length) return;
  graph.exportPNG("模型关系图", {padding: 32, backgroundColor: "#ffffff", quality: 1});
}

// “查看全图”仅在主动选择时缩小，留出工具栏和展开图例所在的空间。
function fitGraph() {
  if (!graph || !hasNodes.value) return;
  const {clientWidth: width, clientHeight: height} = graphContainerRef.value;
  const legend = containerRef.value.querySelector(".legend-container");
  const left = Math.min(legend.offsetLeft + legend.offsetWidth + 12, width / 2);
  const top = toolbarRef.value.offsetTop + toolbarRef.value.offsetHeight + 12;
  const bounds = graph.getContentArea();
  if (width <= left + 12 || height <= top + 12) return;
  graph.zoomTo(Math.min(INITIAL_SCALE, (width - left - 12) / bounds.width, (height - top - 12) / bounds.height));
  graph.positionPoint(bounds.getCenter(), (width + left - 12) / 2, (height + top - 12) / 2);
}

async function changeFullscreen() {
  try {
    await toggleFullscreen();
  } catch {
    ElMessage.warning("浏览器未能切换画布全屏");
  }
}

function panWithWheel(event) {
  if (!graph || !hasNodes.value || event.ctrlKey || event.metaKey) return;
  event.preventDefault();
  const unit = event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? graphContainerRef.value.clientHeight : 1;
  const horizontal = event.shiftKey && !event.deltaX;
  graph.translateBy(-(horizontal ? event.deltaY : event.deltaX) * unit, horizontal ? 0 : -event.deltaY * unit);
}

function handleCanvasKey(event) {
  if (event.ctrlKey || event.metaKey || event.altKey || !graph || !hasNodes.value) return;
  const step = event.shiftKey ? 160 : 48;
  switch (event.key) {
    case "+": case "=": zoomGraph(0.2); break;
    case "-": zoomGraph(-0.2); break;
    case "0": resetView(); break;
    case "ArrowLeft": graph.translateBy(step, 0); break;
    case "ArrowRight": graph.translateBy(-step, 0); break;
    case "ArrowUp": graph.translateBy(0, step); break;
    case "ArrowDown": graph.translateBy(0, -step); break;
    default: return;
  }
  event.preventDefault();
}

// 容器挂载后创建 X6 实例；允许平移与组合键缩放，禁止直接编辑节点和连线。
onMounted(() => {
  graph = new Graph({
    container: graphContainerRef.value,
    width: graphContainerRef.value.clientWidth,
    height: graphContainerRef.value.clientHeight,
    background: {color: "#ffffff"},
    panning: {enabled: true, eventTypes: ["leftMouseDown", "mouseWheelDown"]},
    mousewheel: {enabled: true, zoomAtMousePosition: true, modifiers: ["ctrl", "meta"], minScale: MIN_SCALE, maxScale: MAX_SCALE},
    interacting: false,
  });
  graph.use(new Export());
  graph.on("edge:mouseenter", showEdgeDetail);
  graph.on("edge:mouseleave", () => { tipCloseTimer = setTimeout(hideEdgeDetail, 140); });
  graph.on("translate", hideEdgeDetail);
  graph.on("node:click", ({node}) => highlightRelations(node));
  graph.on("blank:click", () => highlightRelations());
  graph.on("scale", ({sx}) => { scale.value = sx; hideEdgeDetail(); });
  graph.on("node:dblclick", ({node}) => {
    graph.zoomTo(INITIAL_SCALE);
    graph.centerCell(node);
  });
  // 随容器实际宽高调整画布，兼容动态高度及父组件传入的 style。
  resizeObserver = new ResizeObserver(entries => {
    const {width, height} = entries[0]?.contentRect || {};
    if (width && height && graph) {
      hideEdgeDetail();
      const wasHidden = !graph.options.width || !graph.options.height;
      graph.resize(width, height);
      if (wasHidden) resetView();
    }
  });
  resizeObserver.observe(viewportRef.value);
  loadGraph();
});

// 切换当前模型或替换筛选条件时重新获取关系数据。
watch(() => [props.modelId, props.query], loadGraph);
watch([model_type, model_status], () => {
  if (!graph) return;
  graphData.nodes.forEach(node => graph.getCellById(String(node.id))?.attr(createNodeAttrs(node)));
});
// 离开页面时释放尺寸监听与图实例，避免重复监听和资源残留。
onBeforeUnmount(() => {
  hideEdgeDetail();
  requestSequence++;
  resizeObserver?.disconnect();
  graph?.dispose();
  graph = null;
});

defineExpose({reload: loadGraph, fitGraph, resetView});
</script>

<style scoped lang="scss">
.relation-graph {
  position: relative;
  height: var(--model-relation-graph-height);
  width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: var(--qt-surface-background);
}

.relation-graph:fullscreen { width: 100vw; height: 100vh; }

.edge-detail {
  position: absolute;
  z-index: var(--qt-model-graph-toolbar-layer);
  width: max-content;
  max-width: min(360px, calc(100% - 8px));
  max-height: min(260px, calc(100% - 8px));
  overflow: auto;
  overflow-wrap: anywhere;
  padding: 10px 12px;
  background: var(--qt-surface-background);
  border: 1px solid var(--qt-border-color);
  border-radius: var(--qt-radius-medium);
  box-shadow: 0 2px 10px #182d481a;
  color: var(--qt-text-primary);
  font-size: 12px;
  line-height: 1.65;

  strong { font-size: 13px; }
  dl { display: grid; grid-template-columns: auto 1fr; gap: 3px 10px; margin: 6px 0 0; }
  dt { color: var(--qt-text-secondary); }
  dd { margin: 0; }
}

.relation-graph__canvas:focus-visible { outline: 2px solid var(--qt-element-fixed-primary); outline-offset: -2px; }

.graph-viewport { position: relative; flex: 1; min-height: 0; }

.relation-graph__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;

  :deep(.x6-node) { cursor: pointer; }
  :deep([data-relation-focus="dimmed"]) { opacity: 0.22; }
  :deep([data-relation-focus="selected"] .relation-card-body) {
    stroke: var(--qt-element-fixed-primary, #2666fb);
    stroke-width: 3;
  }
  :deep([data-relation-focus="related"] .relation-card-body) {
    stroke: #8699b4;
    stroke-width: 1.5;
  }
  :deep([data-relation-focus="related"] .relation-edge-line) { stroke-width: 2.5; }

  .emptyBg .empty-state-image {
    content: var(--qt-empty-state-image);
    width: var(--qt-model-encoding-empty-width);
    margin-top: 0;
  }
}

.relation-graph__empty {
  position: absolute;
  inset: 0;
  z-index: var(--qt-model-graph-empty-layer);
  background: color-mix(in srgb, var(--qt-surface-background) 88%, transparent);

  img {
    margin-top: var(--qt-model-graph-empty-offset);
  }
}

/* 项目两套 iconfont 共用字体名，单独指定旧图集，避免同码位显示成其他图标。 */
@font-face {
  font-family: "qdata-graph-line";
  src: url("@/assets/common/icons/iconfont/font/iconfont.woff2") format("woff2");
}
.graph-line-icon { font-family: "qdata-graph-line" !important; }
.tool-trigger, .tool-group { display: inline-flex; align-items: center; }

.tool-group + .tool-group::before {
  content: "";
  height: var(--qt-space-4);
  margin: 0 var(--qt-space-1);
  border-left: 1px solid var(--qt-border-color);
}

.toolbar {
  position: absolute;
  inset: var(--qt-space-3) var(--qt-space-3) auto;
  z-index: var(--qt-model-graph-toolbar-layer);
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: var(--qt-space-2);
  pointer-events: none;
}

.view-controls {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  max-width: 100%;
  padding: 0 2px;
  border: 1px solid var(--qt-border-color);
  border-radius: var(--qt-radius-medium);
  background: var(--qt-surface-background);
  box-shadow: 0 2px 6px color-mix(in srgb, var(--qt-text-primary) 8%, transparent);
  pointer-events: auto;

  .canvas-tool {
    margin: 0;
    width: var(--qt-control-height);
    height: calc(var(--qt-control-height) - 2px);
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--qt-text-regular);

    &:hover:not(:disabled), &:focus-visible {
      background: var(--qt-primary-light);
      color: var(--qt-primary);
    }
    &:disabled { color: var(--qt-control-disabled-text); }
    .iconfont { font-size: 16px; line-height: 1; }
  }
}

.model-locator { width: 240px; max-width: 100%; margin-left: auto; pointer-events: auto; }
.model-type-icon { width: 16px; height: 16px; vertical-align: middle; }
.model-option-icon { margin-right: var(--qt-space-2); }
.model-option-code { margin-left: 10px; color: var(--qt-text-secondary); font-size: 12px; }
.model-no-match { padding: 8px 12px; text-align: center; color: var(--qt-text-secondary); }

.legend-container {
  position: absolute;
  bottom: var(--qt-space-4);
  left: var(--qt-space-3);
  z-index: var(--qt-model-graph-toolbar-layer);
  width: var(--qt-model-graph-legend-width);
  height: var(--qt-model-graph-legend-height);
  overflow: hidden;
  border: 1px solid var(--qt-border-color);
  border-radius: var(--qt-radius-medium);
  background: var(--qt-surface-background);
  box-shadow: 0 1px 8px color-mix(in srgb, var(--qt-text-primary) 14%, transparent);
  transition: height var(--qt-model-graph-motion-duration) ease;

  &.collapsed {
    height: var(--qt-control-height);
  }
}

.legend-header {
  width: 100%;
  background: transparent;
  border: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--qt-control-height);
  padding: 0 var(--qt-space-3);
  box-sizing: border-box;
  cursor: pointer;
}

.legend-title {
  font-size: var(--qt-font-size-secondary);
  color: var(--qt-text-primary);
}

.legend-toggle-btn {
  width: 7px;
  height: 5px;
  background: var(--qt-text-secondary);
  clip-path: polygon(0 0, 100% 0, 50% 100%);
  transition: transform var(--qt-model-graph-motion-duration) ease;

  &.rotate {
    transform: rotate(180deg);
  }
}

.legend-content {
  padding: 2px var(--qt-space-2) var(--qt-space-2);
}

.legend-item {
  display: flex;
  align-items: center;
  min-height: 25px;
  font-size: var(--qt-font-size-small);
  white-space: nowrap;
  color: var(--qt-text-primary);
}

.legend-icon {
  flex: 0 0 auto;
  width: 15px;
  height: 18px;
  margin: 0 var(--qt-space-2) 0 6px;
}

.edge-legend {
  position: relative;
  flex: 0 0 auto;
  width: 23px;
  height: 0;
  margin-right: 7px;
  border-top: 1px solid;

  &::after {
    position: absolute;
    top: -4px;
    right: -1px;
    width: 0;
    height: 0;
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    border-left: 6px solid;
    content: "";
  }
}

.edge-legend--normal {
  color: var(--qt-element-fixed-primary);
  border-top-style: dashed;
}

.edge-legend--class {
  color: var(--qt-status-warning);
}

.edge-legend--sub {
  border-top-style: dashed;
  color: var(--qt-status-success);
}
</style>
