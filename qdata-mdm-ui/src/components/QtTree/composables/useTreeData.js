/*
 * useTreeData — QtTree 数据管理层可组合函数
 *
 * 2 种数据模式（按优先级从高到低）：
 *   A. data（兼容 deptOptions）静态数据模式：直接使用父组件传入的树数据
 *   B. api.list 接口模式：调用 list 接口 → proxy.handleTree() 转树 → 包虚拟根节点
 *
 * 附加能力：
 *   - expandedKeys：展开记忆（刷新后恢复展开态）
 *   - currentNodeKey：选中记忆（刷新后 nextTick 恢复选中态）
 *   - dialogTreeOptions：适配弹窗上级树选择器的包装数据
 *   - getNodeKey：基于 props.nodeKey 获取节点唯一 key
 *   - getIdsByLevel：递归获取指定层级内所有节点 key（用于默认展开第一层）
 *   - getDeptTree：触发整树刷新（派发 beforeRefresh）
 */

import { ref, computed, watch, onMounted, nextTick } from 'vue';
export function useTreeData({ props, emit, proxy, resolvedTreeProps, wrapRoot = true }) {
  // ---------- 响应式状态 ----------
  // 处理后的最终树数据（接口模式下含虚拟根节点）
  const processedData = ref([]);
  // 整树加载 loading（对应 api.list 拉取期间）
  const loading = ref(false);
  // 当前选中节点的 key（刷新后恢复选中）
  const currentNodeKey = ref(null);
  // 需要展开的节点 key 数组
  const expandedKeys = ref([]);
  // el-tree 组件 ref（由父组件在挂载后通过 setDeptTreeRef 注入）
  let deptTreeRef = null;

  // ---------- 对外：设置 el-tree ref ----------
  function setDeptTreeRef(refVal) {
    deptTreeRef = refVal;
  }

  // ---------- 基础工具：节点 key 获取 ----------
  function getNodeKey(data) {
    if (!data) return data;
    const key = props.nodeKey || 'id';
    return data[key];
  }

  // ---------- 基础工具：按层级递归获取节点 id ----------
  function getIdsByLevel(nodes, level = 2, currentLevel = 1) {
    const ids = [];
    if (!nodes || currentLevel > level) return ids;
    const keyField = props.nodeKey || 'id';
    const childrenField = resolvedTreeProps?.value?.children || 'children';
    for (const node of nodes) {
      ids.push(node[keyField]);
      const children = node[childrenField];
      if (children && children.length > 0) {
        ids.push(...getIdsByLevel(children, level, currentLevel + 1));
      }
    }
    return ids;
  }

  // ---------- computed：最终给 el-tree 的 :data ----------
  const resolvedTreeData = computed(() => processedData.value);

  // ---------- computed：默认展开 key（外部 props 优先）----------
  const resolvedDefaultExpandedKeys = computed(() => {
    if (Array.isArray(props.defaultExpandedKeys)) {
      return props.defaultExpandedKeys;
    }
    return expandedKeys.value;
  });

  // 数据源切换或刷新后，旧接口请求不再覆盖当前传入的数据。
  let requestVersion = 0;

  // ---------- computed：编辑弹窗用的树选项（包装虚拟根）----------
  const dialogTreeOptions = computed(() => {
    const nodes = processedData.value;
    const keyField = props.nodeKey || 'id';
    const labelField = resolvedTreeProps?.value?.label || 'name';
    const childrenField = resolvedTreeProps?.value?.children || 'children';
    if (
      Array.isArray(nodes) &&
      nodes.length === 1 &&
      nodes[0] &&
      String(nodes[0][keyField]) === String(props.rootNodeValue)
    ) {
      return nodes;
    }
    const virtualRoot = { code: '', [childrenField]: nodes };
    virtualRoot[keyField] = props.rootNodeValue;
    virtualRoot[labelField] = props.title;
    return [virtualRoot];
  });

  // ---------- 整树刷新：getDeptTree ----------
  function getDeptTree() {
    emit('beforeRefresh');
    const version = ++requestVersion;

    // 显式传入数据始终优先，空数组也是有效数据。
    if (Array.isArray(props.deptOptions)) {
      processedData.value = props.deptOptions;
      loading.value = false;
      initExpandedKeysFromNodes(processedData.value);
      emit('treeChange', processedData.value);
      return Promise.resolve();
    }

    // ② api.list 接口模式
    if (props.api?.list) {
      loading.value = true;
      return props.api
        .list(props.extraParams)
        .then((response) => {
          if (version !== requestVersion || Array.isArray(props.deptOptions)) return;
          const keyField = props.nodeKey || 'id';
          const parentKeyField = 'parentId';
          const tree =
            wrapRoot && proxy?.handleTree
              ? proxy.handleTree(response.data, keyField, parentKeyField)
              : response.data;
          // 包装虚拟根节点（适配 treeProps / nodeKey / rootNodeValue）
          const labelField = resolvedTreeProps?.value?.label || 'name';
          const childrenField = resolvedTreeProps?.value?.children || 'children';
          const virtualRoot = { value: '', code: '', [childrenField]: tree };
          virtualRoot[keyField] = props.rootNodeValue;
          virtualRoot[labelField] = props.title || '类目';
          processedData.value = wrapRoot ? [virtualRoot] : tree || [];
          // 根节点默认始终展开
          const rootKey = props.rootNodeValue;
          if (!wrapRoot) {
            initExpandedKeysFromNodes(processedData.value);
          } else if (expandedKeys.value.length === 0) {
            expandedKeys.value = [rootKey];
          } else if (!expandedKeys.value.some((k) => String(k) === String(rootKey))) {
            expandedKeys.value.unshift(rootKey);
          }
          // 刷新后恢复选中态
          if (currentNodeKey.value) {
            nextTick(() => {
              deptTreeRef?.setCurrentKey?.(currentNodeKey.value);
            });
          }
          emit('treeChange', processedData.value);
        })
        .finally(() => {
          if (version === requestVersion) loading.value = false;
        });
    }

    loading.value = false;
    return Promise.resolve();
  }

  // ---------- 节点交互：点击/展开/折叠（记忆状态）----------
  function handleNodeClick(data) {
    currentNodeKey.value = getNodeKey(data);
    // 注：不在这里 emit('nodeClick')！由外层 index.vue 的 handleElTreeNodeClick 统一 emit 1 次
    // （避免和外层同步维护 currentNodeKey / v-model:deptName / treeChange 后再对外通知）
  }

  function handleNodeExpand(data) {
    const key = getNodeKey(data);
    if (!expandedKeys.value.includes(key)) {
      expandedKeys.value.push(key);
    }
  }

  function handleNodeCollapse(data) {
    const key = getNodeKey(data);
    const idx = expandedKeys.value.indexOf(key);
    if (idx > -1) {
      expandedKeys.value.splice(idx, 1);
    }
  }

  // ---------- 工具：根据传入节点列表初始化 expandedKeys（供 watch 复用）----------
  function initExpandedKeysFromNodes(val) {
    const keyField = props.nodeKey || 'id';
    const childrenField = resolvedTreeProps?.value?.children || 'children';
    const first = val[0];
    if (
      val.length === 1 &&
      first &&
      String(first[keyField]) === String(props.rootNodeValue) &&
      Array.isArray(first[childrenField])
    ) {
      if (expandedKeys.value.length === 0) {
        expandedKeys.value = [first[keyField]];
      }
    } else if (expandedKeys.value.length === 0) {
      expandedKeys.value = getIdsByLevel(val, 1);
    }
  }

  // ---------- Watch：数据/参数变化联动 ----------
  // 静态树数据 deptOptions 变化：同步 + 初始化第一层展开
  watch(
    () => props.deptOptions,
    (val, previous) => {
      // 当外部有传递 loading，或者内部没有数据时，可以认为是初始加载
      if (val === undefined) {
        loading.value = true;
      } else {
        loading.value = false;
      }

      if (Array.isArray(val)) {
        requestVersion++;
        processedData.value = val;
        emit('treeChange', processedData.value);
      } else if (Array.isArray(previous)) {
        getDeptTree();
      }
      // 初始化默认展开第一层
      if (Array.isArray(val) && val.length > 0) {
        initExpandedKeysFromNodes(val);
      }
    },
    { immediate: true }
  );

  // extraParams 深度变化 → 重拉接口（有 api.list 时）
  watch(
    () => props.extraParams,
    (newVal, oldVal) => {
      // 避免父组件每次渲染传入新的字面量对象导致无效请求
      if (newVal !== oldVal && JSON.stringify(newVal) === JSON.stringify(oldVal)) return;
      if (!Array.isArray(props.deptOptions) && props.api?.list) {
        getDeptTree();
      }
    },
    { deep: true }
  );

  // ---------- 对外暴露的方法：重置 / 设置选中 ----------
  function resetTree() {
    deptTreeRef?.setCurrentKey?.(null);
    currentNodeKey.value = null;
  }

  function setCurrentKey(key, shouldAutoExpandParent = true) {
    // 允许业务页面只设置选中节点，不展开其上级目录；默认行为保持不变。
    deptTreeRef?.setCurrentKey?.(key, shouldAutoExpandParent);
    currentNodeKey.value = key;
  }

  // ---------- 生命周期：onMounted 触发首次加载 ----------
  onMounted(() => {
    // 接口模式 → 首次加载
    if (!Array.isArray(props.deptOptions) && props.api?.list) {
      getDeptTree();
    }
  });

  return {
    // 数据
    processedData,
    loading,
    currentNodeKey,
    expandedKeys,
    dialogTreeOptions,
    resolvedTreeData,
    resolvedDefaultExpandedKeys,
    // 工具
    getNodeKey,
    getIdsByLevel,
    setDeptTreeRef,
    // 刷新 / 交互
    getDeptTree,
    handleNodeClick,
    handleNodeExpand,
    handleNodeCollapse,
    resetTree,
    setCurrentKey
  };
}
