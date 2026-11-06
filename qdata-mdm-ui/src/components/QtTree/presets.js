/*
 * presets.js — QtTree 配置预设与工具
 *
 * 负责：
 *   1) ICONS：静态资源 ES module 导入（避免 Vite runtime 字符串 404）
 *   2) DEFAULT_CONFIG：所有 props/开关/配置收敛成纯数据对象
 *   3) PRESETS：预设 partial 对象，配合 mergeConfig 快速切换配置
 *   4) mergeConfig：lodash-es.merge 深合并 3 层覆盖（DEFAULT → PRESET → user config）
 *
 * 注意：lazy 懒加载树（数据源→表→字段）已独立拆分到 QtTree/lazy.vue 自行维护，
 *       不再通过 preset="lazy" / buildLazyNodePayload / genSelectSQL 等通用封装提供。
 */

import { merge } from 'lodash-es';

export const ICONS = { menuAddIcon: 'Plus' };

export const DEFAULT_CONFIG = {
  data: undefined,
  api: {
    list: null,
    get: null,
    add: null,
    update: null,
    del: null
  },
  treeProps: {
    label: 'name',
    children: 'children'
  },
  nodeKey: 'id',
  rootNodeValue: '0',
  defaultExpandedKeys: null,
  defaultExpand: false,
  refreshKey: 0,
  extraParams: {},
  disableAutoRefresh: false,

  showSearch: true,
  editable: false,
  showEngName: false,
  placeholder: '请输入部门名称',
  title: '类目',
  type: '',
  contextMenu: false,
  showLevel1Tooltip: false,
  useBuiltinCatEditDialog: false,
  headerOffset: 0,
  wrapHeight: undefined,

  leftWidth: 280,
  deptName: '',
  actionLoading: false,
  resizable: true, // 是否展示拖拽条并允许拖拽调整 leftWidth（preset=simple 下默认关闭，嵌入式场景不需要）
  collapsible: true, // 是否展示折叠/展开按钮（preset=simple 下默认关闭，嵌入式场景不需要）

  menuConfig: {
    visible: {
      add: true,
      addSibling: true,
      edit: true,
      delete: true
    },
    labels: {
      add: '新增子级',
      addSibling: '新增同级',
      edit: '修改',
      delete: '删除'
    },
    icons: {
      add: ICONS.menuAddIcon,
      addSibling: 'CopyDocument',
      edit: 'Edit',
      delete: 'Delete'
    },
    class: {
      add: '',
      addSibling: '',
      edit: '',
      delete: 'delete-item'
    },
    deleteConfirmText: (data) => `是否确认删除"${data?.name ?? ''}"？`
  }
};

Object.freeze(DEFAULT_CONFIG);

export const PRESETS = {
  default: {
    editable: false,
    useBuiltinCatEditDialog: false,
    showSearch: true,
    title: '类目',
    contextMenu: false,
    showLevel1Tooltip: false
  },
  simple: {
    editable: false,
    useBuiltinCatEditDialog: false,
    showSearch: false,
    contextMenu: false,
    showLevel1Tooltip: false,
    resizable: false,
    collapsible: false
  }
};

export const helpers = {};

export function mergeConfig({ preset = 'default', config = {} } = {}) {
  return merge({}, DEFAULT_CONFIG, PRESETS[preset] || {}, config);
}
