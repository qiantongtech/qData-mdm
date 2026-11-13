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
  <el-aside
    ref="asideRef"
    :style="{
      width: `${localLeftWidth}px`,
      marginLeft: localLeftWidth == 0 ? '-15px' : '0px',
      '--qt-wrap-height': qtWrapheight
    }"
    class="left-pane QtTree-container"
  >
    <div v-if="resolvedConfig.showSearch" class="search-wrapper">
      <slot name="before-search" />
      <slot name="search">
        <el-input
          class="filter-tree"
          v-model="innerDeptName"
          :placeholder="resolvedConfig.placeholder"
          clearable
          prefix-icon="Search"
        />
      </slot>
    </div>

    <div class="left-tree" v-loading="mergedLoading">
      <el-tree
        :key="resolvedConfig.refreshKey"
        class="dept-tree"
        :data="resolvedTreeData"
        :props="resolvedTreeProps"
        :filter-node-method="filterNode"
        ref="deptTreeRef"
        :node-key="resolvedConfig.nodeKey"
        highlight-current
        :default-expanded-keys="resolvedDefaultExpandedKeys"
        @node-click="handleElTreeNodeClick"
        @node-expand="handleNodeExpand"
        @node-collapse="handleNodeCollapse"
        :default-expand-all="resolvedConfig.defaultExpand"
        @node-contextmenu="onNodeContextMenu"
      >
        <template #default="{ node, data }">
          <span
            class="custom-tree-node"
            :title="modelNodeTypes.has(data.nodeType) ? `${t(`components.qtTree.${data.nodeType}`)} · ${nodeLabelText(data)}` : undefined"
            @dblclick.stop="handleNodeDblclick(data, node)"
          >
            <slot
              name="icon"
              :node="node"
              :data="data"
              :default-icon="getNodeIcon(node, data)"
            >
              <span class="node-icon-wrapper">
                <svg-icon
                  v-if="data.nodeType === 'project'"
                  icon-class="stack-line"
                  class="node-icon node-icon--domain"
                />
                <span
                  v-else-if="data.nodeType === 'layer' || directoryIcons"
                  class="node-icon node-icon--directory"
                  :class="{ 'is-expanded': node.expanded, 'node-icon--layer': data.nodeType === 'layer' }"
                  aria-hidden="true"
                ></span>
                <span
                  v-else-if="data.nodeType === 'model'"
                  class="node-icon node-icon--model"
                  aria-hidden="true"
                ></span>
                <img
                  v-else
                  class="node-icon"
                  :src="getNodeIcon(node, data)"
                  alt=""
                />
              </span>
            </slot>

            <slot name="label" :node="node" :data="data">
              <span class="treelabel">
                {{ nodeLabelText(data) }}
              </span>
            </slot>

            <el-dropdown
              v-if="resolvedConfig.editable"
              trigger="click"
              @command="(cmd) => handleCommand(cmd, data)"
              @visible-change="(v) => handleDropdownVisibleChange(v, getNodeKey(data))"
            >
              <span
                class="operation-trigger"
                :class="{
                  'is-active': activeDropdownNodeId === getNodeKey(data)
                }"
                @click.stop
              >
                <el-icon class="action-icon">
                  <MoreFilled />
                </el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu class="dept-tree-dropdown">
                  <slot
                    name="dropdown-menu"
                    :node="deptTreeRef?.getNode?.(getNodeKey(data))"
                    :data="data"
                    :defaultItems="getResolvedMenuItems(data)"
                    :close="closeDropdownMenu"
                    :runCommand="(cmd) => handleCommand(cmd, data)"
                  >
                    <template v-for="item in getResolvedMenuItems(data)" :key="item.command">
                      <el-dropdown-item
                        v-if="item.visible"
                        :command="item.command"
                        :class="item.class"
                      >
                        <img
                          v-if="isImgIcon(item.icon)"
                          class="dept-tree-menu-icon"
                          :src="item.icon"
                          alt=""
                        />
                        <el-icon v-else-if="isCompIcon(item.icon)" class="dept-tree-menu-icon">
                          <component :is="item.icon" />
                        </el-icon>
                        <span>{{ item.label }}</span>
                      </el-dropdown-item>
                    </template>
                  </slot>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </span>
        </template>
      </el-tree>

      <div
        v-if="resolvedConfig.contextMenu && contextMenuVisible"
        :style="{ top: `${contextMenuY}px`, left: `${contextMenuX}px` }"
        class="context-menu"
        @click.stop
      >
        <slot
          name="context-menu"
          :data="contextMenuNode?.data"
          :node="contextMenuNode?.node"
          :close="closeContextMenu"
          :position="{ x: contextMenuX, y: contextMenuY }"
        >
          <ul>
            <li
              v-for="(item, idx) in resolvedContextMenuItems"
              :key="idx"
              @click="runContextMenuItem(item)"
            >
              {{ item.label }}
            </li>
          </ul>
        </slot>
      </div>
    </div>
  </el-aside>

  <div
    class="resize-bar"
    v-if="resolvedConfig.resizable || resolvedConfig.collapsible"
    :class="{ 'resize-bar--drag-disabled': !resolvedConfig.resizable }"
    @mousedown="safeStartResize"
  >
    <div class="resize-handle-sx">
      <span class="zjsx" v-if="resolvedConfig.resizable"></span>
      <template v-if="resolvedConfig.collapsible">
        <el-icon v-if="localLeftWidth == 0" @click.stop="toggleCollapse" class="collapse-icon">
          <ArrowRight />
        </el-icon>
        <el-icon v-else class="collapse-icon" @click.stop="toggleCollapse">
          <ArrowLeft />
        </el-icon>
      </template>
    </div>
  </div>

  <slot
    name="edit-dialog"
    :dialogRef="catEditDialogRef"
    :openDialog="openDialog"
    :loadingApi="loadingApi"
    :dialogTreeOptions="dialogTreeOptions"
    :dialogSubmit="handleCatSubmit"
  >
  </slot>
</template>

<script setup name="QtTree">
import { ref, computed, getCurrentInstance, onMounted, nextTick, resolveComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { merge } from 'lodash-es';
import { MoreFilled, ArrowRight, ArrowLeft } from '@element-plus/icons-vue';
import { useTreePanel } from './composables/useTreePanel.js';
import { useTreeSearch } from './composables/useTreeSearch.js';
import { useTreeData } from './composables/useTreeData.js';
import { useTreeActions } from './composables/useTreeActions.js';
import { mergeConfig } from './presets.js';
import folderOpenIcon from '@/assets/common/icons/tree/folder-open.svg';
import folderCloseIcon from '@/assets/common/icons/tree/folder-close.svg';
import fileIcon from '@/assets/common/icons/tree/file.svg';
import modelIcon from '@/assets/common/icons/model/model.svg';
import domainIcon from '@/assets/common/icons/svg/stack-line.svg';

const { t } = useI18n();
const modelNodeTypes = new Set(['project', 'layer', 'model']);

function getNodeIcon(node, data) {
  if (data.nodeType === 'project') return domainIcon;
  if (data.nodeType === 'model') return modelIcon;
  if (data.nodeType === 'layer' || node.childNodes.length > 0 || node.level === 1) {
    return node.expanded ? folderOpenIcon : folderCloseIcon;
  }
  return fileIcon;
}

const props = defineProps({
  preset: { type: String, default: 'default' },
  directoryIcons: Boolean,
  config: { type: Object, default: () => ({}) },
  leftWidth: { type: Number, default: undefined },
  deptName: { type: String, default: undefined },
  data: { type: Array, default: undefined },
  deptOptions: { type: Array, default: undefined },
  type: { type: String, default: undefined },
  placeholder: { type: String, default: undefined },
  defaultExpand: { type: Boolean, default: undefined },
  editable: { type: Boolean, default: undefined },
  title: { type: String, default: undefined },
  showEngName: { type: Boolean, default: undefined },
  api: { type: Object, default: undefined },
  actionLoading: { type: Boolean, default: undefined },
  menuConfig: { type: Object, default: undefined },
  disableAutoRefresh: { type: Boolean, default: undefined },
  treeProps: { type: Object, default: undefined },
  nodeKey: { type: [String, Number], default: undefined },
  // 可按层级指定项目 SVG 图标，未配置的层级沿用公共默认图标。
  levelIcons: { type: Object, default: undefined },
  defaultExpandedKeys: { type: Array, default: undefined },
  rootNodeValue: { type: [String, Number], default: undefined },
  extraParams: { type: Object, default: undefined },
  refreshKey: { type: [String, Number], default: undefined },
  headerOffset: { type: Number, default: undefined },
  wrapHeight: { type: [String, Number], default: undefined },
  wrapHeightClass: { type: String, default: undefined },
  showSearch: { type: Boolean, default: undefined },
  contextMenu: { type: [Boolean, Array], default: undefined },
  showLevel1Tooltip: { type: Boolean, default: undefined },
  useBuiltinCatEditDialog: { type: Boolean, default: undefined }
});

const emit = defineEmits([
  'nodeClick',
  'nodeDblclick',
  'update:deptName',
  'update:leftWidth',
  'nodeAdd',
  'nodeEdit',
  'nodeDelete',
  'treeChange',
  'action',
  'beforeRefresh',
  'contextMenuClick'
]);
const { proxy } = getCurrentInstance() || {};

const explicitPropsOverrides = computed(() => {
  const out = {};
  if (props.data !== undefined) out.data = props.data;
  else if (props.deptOptions !== undefined) out.data = props.deptOptions;
  const direct = [
    'type',
    'placeholder',
    'defaultExpand',
    'editable',
    'title',
    'showEngName',
    'api',
    'actionLoading',
    'menuConfig',
    'disableAutoRefresh',
    'treeProps',
    'nodeKey',
    'levelIcons',
    'defaultExpandedKeys',
    'rootNodeValue',
    'extraParams',
    'refreshKey',
    'headerOffset',
    'wrapHeight',
    'wrapHeightClass',
    'wrapHeightClass',
    'showSearch',
    'contextMenu',
    'showLevel1Tooltip',
    'useBuiltinCatEditDialog'
  ];
  for (const k of direct) {
    if (props[k] !== undefined) out[k] = props[k];
  }
  if (props.leftWidth !== undefined) out.leftWidth = props.leftWidth;
  if (props.deptName !== undefined) out.deptName = props.deptName;
  return out;
});

const resolvedConfig = computed(() => {
  const base = mergeConfig({ preset: props.preset, config: props.config });
  return merge({}, base, explicitPropsOverrides.value);
});

const FIELD_ALIAS = {
  deptOptions: 'data'
};
const pseudoProps = new Proxy(
  {},
  {
    get(_target, key) {
      const resolvedKey = FIELD_ALIAS[key] || key;
      return resolvedConfig.value[resolvedKey];
    },
    has(_target, key) {
      const resolvedKey = FIELD_ALIAS[key] || key;
      return resolvedKey in resolvedConfig.value;
    },
    ownKeys() {
      return Array.from(
        new Set([...Object.keys(resolvedConfig.value), ...Object.keys(FIELD_ALIAS)])
      );
    },
    getOwnPropertyDescriptor(_target, key) {
      const resolvedKey = FIELD_ALIAS[key] || key;
      if (resolvedKey in resolvedConfig.value) {
        return { enumerable: true, configurable: true };
      }
      return undefined;
    }
  }
);

const resolvedMenuConfigWithIcons = computed(() => {
  const cfg = resolvedConfig.value.menuConfig || {};
  const icons = { ...(cfg.icons || {}) };
  for (const k of Object.keys(icons)) {
    if (
      typeof icons[k] === 'string' &&
      !icons[k].startsWith('http') &&
      !icons[k].startsWith('/') &&
      !icons[k].startsWith('@') &&
      !icons[k].endsWith('.svg') &&
      !icons[k].endsWith('.png')
    ) {
      try {
        const comp = resolveComponent(icons[k]);
        if (comp && typeof comp !== 'string') icons[k] = comp;
      } catch (e) {
        void e;
      }
    }
  }
  return { ...cfg, icons };
});

const mergedPseudoProps = new Proxy(pseudoProps, {
  get(target, key) {
    if (key === 'menuConfig') return resolvedMenuConfigWithIcons.value;
    return Reflect.get(target, key, target);
  }
});

const resolvedTreeProps = computed(() => ({
  label: 'name',
  children: 'children',
  ...(resolvedConfig.value.treeProps || {})
}));

const { localLeftWidth, qtWrapheight, startResize, toggleCollapse } = useTreePanel({
  props: mergedPseudoProps,
  emit
});

// preset=simple 等场景下 resizable=false 时，拦截 mousedown 不触发拖拽
function safeStartResize(event) {
  if (!resolvedConfig.value.resizable) return;
  startResize(event);
}

const dataComposed = useTreeData({
  props: mergedPseudoProps,
  emit,
  proxy,
  resolvedTreeProps
});
const {
  loading,
  expandedKeys,
  currentNodeKey,
  dialogTreeOptions,
  resolvedTreeData,
  resolvedDefaultExpandedKeys,
  getNodeKey,
  setDeptTreeRef: _setDataTreeRef,
  getDeptTree,
  handleNodeClick: dataHandleNodeClick,
  handleNodeExpand,
  handleNodeCollapse,
  resetTree,
  setCurrentKey
} = dataComposed;

const deptTreeRef = ref(null);
const { innerDeptName, filterNode } = useTreeSearch({
  props: mergedPseudoProps,
  emit,
  resolvedTreeProps,
  deptTreeRef,
  resolvedTreeData,
  modelNodeTypes
});
onMounted(() => {
  nextTick(() => {
    _setDataTreeRef(deptTreeRef.value);
  });
});
const deptTreeRefForActions = {
  value: deptTreeRef,
  get getNode() {
    return deptTreeRef.value?.getNode?.bind(deptTreeRef.value);
  }
};

// 节点点击：先同步内部选中态/事件（v-model:deptName、treeChange、currentNodeKey），再对外统一 emit 1 次 nodeClick
// 注意：之前的 lazy 包装逻辑（buildLazyNodePayload）已移除，lazy 场景由独立的 lazy.vue 组件自行维护
function handleElTreeNodeClick(data, node, instance) {
  dataHandleNodeClick(data);
  emit('nodeClick', data, node, instance);
}

const catEditDialogRef = ref(null);
const actionsComposed = useTreeActions({
  props: mergedPseudoProps,
  emit,
  proxy,
  resolvedTreeProps,
  loading,
  expandedKeys,
  currentNodeKey,
  getDeptTree,
  dialogTreeOptions,
  getNodeKey,
  deptTreeRef: deptTreeRefForActions
});
const {
  mergedLoading,
  loadingApi,
  getResolvedMenuItems,
  activeDropdownNodeId,
  handleDropdownVisibleChange,
  closeDropdownMenu,
  isImgIcon,
  isCompIcon,
  nodeLabelText,
  handleCommand,
  openDialog,
  handleCatSubmit,
  setCatEditDialogRef,
  contextMenuVisible,
  contextMenuX,
  contextMenuY,
  contextMenuNode,
  resolvedContextMenuItems,
  onNodeContextMenu,
  runContextMenuItem,
  closeContextMenu
} = actionsComposed;

onMounted(() => {
  nextTick(() => setCatEditDialogRef(catEditDialogRef));
});

function handleNodeDblclick(data, node) {
  emit('nodeDblclick', data, node);
}

defineExpose({
  resetTree,
  getDeptTree,
  setCurrentKey,
  deptTreeRef,
  openDialog,
  loadingApi,
  getResolvedMenuItems,
  closeDropdownMenu,
  closeContextMenu
});
</script>

<style scoped lang="scss">
@import './tree-common.scss';
</style>
