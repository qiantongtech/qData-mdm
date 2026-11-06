/*
 * useTreeActions — QtTree 操作交互层可组合函数
 *
 * 负责：
 *   1) loadingApi 门面：{ start, stop, withLoading }，合并内部/外部 loading 为 mergedLoading
 *   2) 下拉菜单配置：DEFAULT_MENU_CONFIG + 深合并 + 适配自定义 nodeKey/rootNodeValue 根节点判断
 *   3) 菜单命令分发 handleCommand：先 emit('action') 给父组件接管，再走内部默认增删改流程
 *   4) 统一弹窗入口 openDialog + 兜底 CatEditDialog 提交回调 handleCatSubmit
 *   5) 右键菜单（contextMenu prop）：onNodeContextMenu / handleClickOutside
 *   6) 节点显示标签：nodeLabelText（支持中英文拼接）
 *   7) 工具：deepMerge / isImgIcon / isCompIcon
 */

import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';

/**
 * 深度合并两个对象（仅 plain object，数组直接覆盖）
 * 用于合并 DEFAULT_MENU_CONFIG 与用户传入的 menuConfig
 */
function deepMerge(target, src) {
  if (!src || typeof src !== 'object') return target;
  const out = Array.isArray(target) ? target.slice() : { ...target };
  for (const key of Object.keys(src)) {
    const tv = out[key];
    const sv = src[key];
    if (
      tv &&
      sv &&
      typeof tv === 'object' &&
      typeof sv === 'object' &&
      !Array.isArray(tv) &&
      !Array.isArray(sv)
    ) {
      out[key] = deepMerge(tv, sv);
    } else if (sv !== undefined) {
      out[key] = sv;
    }
  }
  return out;
}

/** 判断菜单图标是否为图片 URL（字符串）*/
function isImgIcon(icon) {
  return typeof icon === 'string';
}

/** 判断菜单图标是否为 Vue 组件 */
function isCompIcon(icon) {
  return (
    icon !== null &&
    icon !== undefined &&
    !isImgIcon(icon) &&
    (typeof icon === 'object' || typeof icon === 'function')
  );
}

export function useTreeActions({
  props,
  emit,
  proxy,
  resolvedTreeProps,
  // 来自 useTreeData 的引用
  loading,
  expandedKeys,
  currentNodeKey,
  getDeptTree,
  dialogTreeOptions,
  getNodeKey,
  deptTreeRef
  // CatEditDialog ref（由 index.vue 模板渲染完成后通过 setCatEditDialogRef 注入）
}) {
  // ---------- 操作级 loading + 合并 ----------
  const innerActionLoading = ref(false);
  const loadingApi = {
    start() {
      innerActionLoading.value = true;
    },
    stop() {
      innerActionLoading.value = false;
    },
    async withLoading(fn) {
      innerActionLoading.value = true;
      try {
        return await fn();
      } finally {
        innerActionLoading.value = false;
      }
    }
  };
  // 最终给 v-loading 绑定的状态
  const mergedLoading = computed(
    () => loading.value || innerActionLoading.value || !!props.actionLoading
  );

  // ---------- 节点标签文本（支持中英文拼接）----------
  function nodeLabelText(data) {
    if (!data) return '';
    const labelField = resolvedTreeProps?.value?.label || 'name';
    const name = data[labelField] || data.name || '';
    if (props.showEngName) {
      const engName = data.engName || '';
      const separator = name && engName ? ' / ' : '';
      return name + separator + engName;
    }
    return name || data.engName || '';
  }

  // ---------- 根节点判断（适配自定义 nodeKey / rootNodeValue）----------
  function isRootNodeByConfig(data) {
    if (!data) return false;
    const key = props.nodeKey || 'id';
    return String(data[key]) === String(props.rootNodeValue);
  }

  // ---------- 默认菜单配置（与原 QtTree 完全保持一致）----------
  const DEFAULT_MENU_CONFIG = Object.freeze({
    visible: {
      add: () => true,
      addSibling: (data) => !isRootNodeByConfig(data),
      edit: (data) => !isRootNodeByConfig(data),
      delete: (data) => !isRootNodeByConfig(data)
    },
    labels: {
      add: '新增子级',
      addSibling: '新增同级',
      edit: '修改',
      delete: '删除'
    },
    icons: {},
    class: {
      add: '',
      addSibling: '',
      edit: '',
      delete: 'delete-item'
    },
    deleteConfirmText: (data) => `是否确认删除"${nodeLabelText(data)}"？`
  });

  // ---------- 解析后的菜单配置（默认值 + 用户 menuConfig 深合并）----------
  const resolvedMenuConfig = computed(() => {
    const merged = deepMerge(DEFAULT_MENU_CONFIG, props.menuConfig || {});
    // 如果用户没覆盖 visible 函数，则基于 nodeKey/rootNodeValue 重写默认根节点判断
    const orig = DEFAULT_MENU_CONFIG.visible;
    const v = merged.visible || {};
    if (v.addSibling === orig.addSibling) {
      v.addSibling = (data) => !isRootNodeByConfig(data);
    }
    if (v.edit === orig.edit) {
      v.edit = (data) => !isRootNodeByConfig(data);
    }
    if (v.delete === orig.delete) {
      v.delete = (data) => !isRootNodeByConfig(data);
    }
    merged.visible = v;
    return merged;
  });

  // 根据节点数据生成最终的菜单项（供模板 v-for + #dropdown-menu slot 的 defaultItems 使用）
  function getResolvedMenuItems(data) {
    const cfg = resolvedMenuConfig.value;
    const commands = ['add', 'addSibling', 'edit', 'delete'];
    return commands.map((cmd) => {
      const visibleFn = cfg.visible?.[cmd];
      return {
        command: cmd,
        label: cfg.labels?.[cmd] ?? '',
        icon: cfg.icons?.[cmd],
        class: cfg.class?.[cmd] ?? '',
        visible: typeof visibleFn === 'function' ? !!visibleFn(data) : true
      };
    });
  }

  // ---------- 下拉菜单显隐 + 激活态 ----------
  const activeDropdownNodeId = ref(null);
  function handleDropdownVisibleChange(visible, nodeId) {
    activeDropdownNodeId.value = visible ? nodeId : null;
  }
  function closeDropdownMenu() {
    activeDropdownNodeId.value = null;
  }

  // ---------- 兜底 CatEditDialog ref（由 index.vue 在模板渲染后注入）----------
  let catEditDialogRef = null;
  function setCatEditDialogRef(ref) {
    catEditDialogRef = ref;
  }

  // ---------- 统一弹窗入口 ----------
  function openDialog({ command, data }) {
    if (command === 'edit') {
      handleNodeEdit(data);
    } else if (command === 'addSibling') {
      handleNodeAddSibling(data);
    } else {
      handleNodeAdd(data);
    }
  }

  // ---------- 命令分发（先 emit action 再走默认逻辑）----------
  function handleCommand(command, data) {
    // 先向外派发统一 action 事件，父组件可完全接管
    emit('action', {
      command,
      data,
      node: deptTreeRef.value?.getNode?.(getNodeKey(data)),
      loadingApi,
      defaultItems: getResolvedMenuItems(data),
      closeDropdown: closeDropdownMenu
    });

    switch (command) {
      case 'add':
        handleNodeAdd(data);
        break;
      case 'addSibling':
        handleNodeAddSibling(data);
        break;
      case 'edit':
        handleNodeEdit(data);
        break;
      case 'delete':
        handleNodeDelete(data);
        break;
    }
  }

  // ---------- 新增子级 ----------
  function handleNodeAdd(data) {
    if (props.api?.add) {
      catEditDialogRef?.value?.open?.({
        title: '新增' + props.title,
        nameLabel: props.title + '名称',
        treeOptions: dialogTreeOptions.value,
        type: props.type,
        form: {
          parentId: getNodeKey(data),
          validFlag: true,
          ...(props.extraParams || {})
        }
      });
    } else {
      emit('nodeAdd', data);
    }
  }

  // ---------- 新增同级 ----------
  function handleNodeAddSibling(data) {
    const parentId = data.parentId !== undefined && data.parentId !== null ? data.parentId : 0;
    if (props.api?.add) {
      catEditDialogRef?.value?.open?.({
        title: '新增' + props.title + '同级',
        nameLabel: props.title + '名称',
        treeOptions: dialogTreeOptions.value,
        type: props.type,
        form: {
          parentId,
          validFlag: true,
          ...(props.extraParams || {})
        }
      });
    } else {
      emit('nodeAdd', { ...data, parentId });
    }
  }

  // ---------- 修改（先拉详情再开弹窗，期间 loadingApi 驱动树 loading）----------
  function handleNodeEdit(data) {
    if (props.api?.get) {
      loadingApi.withLoading(async () => {
        const response = await props.api.get(getNodeKey(data));
        catEditDialogRef?.value?.open?.({
          title: '编辑' + props.title,
          nameLabel: props.title + '名称',
          treeOptions: dialogTreeOptions.value,
          type: props.type,
          form: response.data
        });
      });
    } else {
      emit('nodeEdit', data);
    }
  }

  // ---------- 删除（二次确认 → 调接口 → 清空选中 → 刷新树）----------
  function handleNodeDelete(data) {
    if (props.api?.del) {
      const confirmText =
        typeof resolvedMenuConfig.value.deleteConfirmText === 'function'
          ? resolvedMenuConfig.value.deleteConfirmText(data)
          : `是否确认删除"${nodeLabelText(data)}"？`;
      proxy?.$modal
        ?.confirm?.(confirmText)
        ?.then(function () {
          return loadingApi.withLoading(async () => {
            if (props.api?.del) {
              await props.api.del(getNodeKey(data));
            }
            proxy?.$modal?.msgSuccess?.('删除成功');
            // 删除当前选中节点 → 清空选中态并 emit 空对象
            if (currentNodeKey.value === getNodeKey(data)) {
              currentNodeKey.value = null;
              emit('nodeClick', {});
            }
            if (!props.disableAutoRefresh) getDeptTree();
          });
        })
        ?.catch((err) => {
          if (err === 'cancel' || err === 'close') return;
          throw err;
        });
    } else {
      emit('nodeDelete', data);
    }
  }

  // ---------- 兜底 CatEditDialog 提交（新增/修改 + 先关弹窗再刷新树）----------
  async function handleCatSubmit(formData) {
    try {
      if (formData.id) {
        // ----- 修改场景 -----
        if (props.api?.update) {
          await props.api.update(formData);
          proxy?.$modal?.msgSuccess?.('修改成功');
          catEditDialogRef?.value?.close?.();
          if (!props.disableAutoRefresh) {
            await loadingApi.withLoading(() => getDeptTree());
          }
        }
      } else {
        // ----- 新增场景 -----
        if (props.api?.add) {
          await props.api.add(formData);
          proxy?.$modal?.msgSuccess?.('新增成功');
          if (formData.parentId && !expandedKeys.value.includes(formData.parentId)) {
            expandedKeys.value.push(formData.parentId);
          }
          catEditDialogRef?.value?.close?.();
          if (!props.disableAutoRefresh) {
            await loadingApi.withLoading(() => getDeptTree());
          }
        }
      }
    } finally {
      catEditDialogRef?.value?.stopLoading?.();
    }
  }

  // ====================================================================
  // 右键菜单（contextMenu prop）
  // ====================================================================

  // 右键菜单定位与显隐
  const contextMenuVisible = ref(false);
  const contextMenuX = ref(0);
  const contextMenuY = ref(0);
  const contextMenuNode = ref(null); // { data, node }

  /** 解析传入的 contextMenu prop 为标准菜单项数组 */
  const resolvedContextMenuItems = computed(() => {
    if (!props.contextMenu) return [];
    if (props.contextMenu === true) {
      // true = 默认项（目前只有 SELECT，可后续扩展）
      return [{ label: 'SELECT' }];
    }
    if (Array.isArray(props.contextMenu)) {
      return props.contextMenu;
    }
    return [];
  });

  /** 判断某菜单项是否对当前 (data, node) 可见 */
  function isContextItemVisible(item, data, node) {
    if (typeof item?.visible === 'function') {
      try {
        return !!item.visible(data, node);
      } catch (e) {
        void e;
        return true;
      }
    }
    return true;
  }

  /** 关闭右键菜单 */
  function closeContextMenu() {
    contextMenuVisible.value = false;
    contextMenuNode.value = null;
  }

  /** 触发某菜单项 handler（内部调用）*/
  function runContextMenuItem(item) {
    const ctx = contextMenuNode.value;
    if (!ctx) return;
    try {
      if (typeof item?.handler === 'function') {
        item.handler(ctx.data, ctx.node, closeContextMenu);
      } else {
        // 没有 handler → 派发自定义事件，让父组件自己处理
        emit('context-menu-click', {
          item,
          data: ctx.data,
          node: ctx.node,
          close: closeContextMenu
        });
      }
    } finally {
      // 菜单项 handler 没主动关的话这里兜底关（若 handler 想异步控制可自己调 close）
      nextTick(() => {
        // 保持原状，交给 handler 或 handleClickOutside 控制
      });
    }
  }

  /** el-tree @node-contextmenu 处理 */
  function onNodeContextMenu(event, data, node) {
    event.preventDefault();
    if (!props.contextMenu) return;
    const visibleItems = resolvedContextMenuItems.value.filter((it) =>
      isContextItemVisible(it, data, node)
    );
    if (visibleItems.length === 0) {
      contextMenuVisible.value = false;
      return;
    }
    contextMenuVisible.value = true;
    contextMenuX.value = event.clientX;
    contextMenuY.value = event.clientY;
    contextMenuNode.value = { data, node };
  }

  /** 点击空白关闭右键菜单（window click 全局监听）*/
  function handleClickOutside() {
    if (contextMenuVisible.value) {
      closeContextMenu();
    }
  }

  // ====================================================================
  // 生命周期：右键菜单全局点击监听
  // ====================================================================
  // 是否启用右键菜单（需要 window click 监听关闭）
  const hasContextMenuFeature = computed(() => !!props.contextMenu);
  onMounted(() => {
    if (hasContextMenuFeature.value) {
      window.addEventListener('click', handleClickOutside);
    }
  });
  onBeforeUnmount(() => {
    window.removeEventListener('click', handleClickOutside);
  });
  // 运行时切换 contextMenu prop：动态注册/卸载监听
  watch(hasContextMenuFeature, (val) => {
    if (val) {
      window.addEventListener('click', handleClickOutside);
    } else {
      window.removeEventListener('click', handleClickOutside);
      closeContextMenu();
    }
  });

  return {
    // loading
    mergedLoading,
    loadingApi,
    innerActionLoading,
    // 菜单配置
    resolvedMenuConfig,
    getResolvedMenuItems,
    activeDropdownNodeId,
    handleDropdownVisibleChange,
    closeDropdownMenu,
    isImgIcon,
    isCompIcon,
    nodeLabelText,
    // 命令分发 & 增删改
    handleCommand,
    openDialog,
    handleCatSubmit,
    handleNodeAdd,
    handleNodeAddSibling,
    handleNodeEdit,
    handleNodeDelete,
    // 弹窗 ref 注入
    setCatEditDialogRef,
    // 右键菜单
    contextMenuVisible,
    contextMenuX,
    contextMenuY,
    contextMenuNode,
    resolvedContextMenuItems,
    onNodeContextMenu,
    runContextMenuItem,
    closeContextMenu,
    isContextItemVisible
  };
}
