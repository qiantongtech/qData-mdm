/*
 * useTreeSearch — QtTree 搜索过滤层可组合函数
 *
 * 负责：
 *   1) 搜索关键词 innerDeptName 与 props.deptName 双向同步
 *   2) 关键词变化时自动调 el-tree.filter()（懒加载场景下仅过滤已加载节点）
 *   3) 对外 emit('update:deptName')，支持 v-model:deptName
 *   4) filterNode 方法：支持 treeProps.label 自定义字段 + engName 双字段模糊匹配
 *
 * 使用：
 *   const { innerDeptName, filterNode, setDeptTreeRef } = useTreeSearch({ props, emit, resolvedTreeProps })
 *   // 模板中拿到 el-tree ref 后：setDeptTreeRef(deptTreeRef)
 */

import { ref, watch, computed } from 'vue';

export function useTreeSearch({ props, emit, resolvedTreeProps }) {
  // ---------- 响应式状态 ----------
  // 内部使用的关键词（避免直接写 prop，与 props.deptName 双向同步）
  const innerDeptName = ref(props.deptName || '');
  // el-tree 组件的 ref（父组件挂载后通过 setDeptTreeRef 注入）
  let deptTreeRef = null;

  // ---------- 对外方法：设置 el-tree ref ----------
  function setDeptTreeRef(ref) {
    deptTreeRef = ref;
  }

  // ---------- 过滤方法（给 el-tree 的 filter-node-method） ----------
  const filterNode = (value, data) => {
    if (typeof props.filterNodeMethod === 'function') {
      return props.filterNodeMethod(value, data);
    }
    if (!value) return true;
    const labelField = resolvedTreeProps?.value?.label || 'name';
    const label = data[labelField] || data.name || '';
    const nameMatch = label && String(label).indexOf(value) !== -1;
    const engNameMatch = data.engName && String(data.engName).indexOf(value) !== -1;
    return Boolean(nameMatch || engNameMatch);
  };

  // ---------- 关键词双向同步 ----------
  // 内部 → 外部：调用 el-tree.filter + emit 双向绑定更新
  watch(innerDeptName, (val) => {
    if (deptTreeRef && typeof deptTreeRef.filter === 'function') {
      deptTreeRef.filter(val);
    }
    emit('update:deptName', val);
  });

  // 外部 → 内部：props.deptName 变化时同步（避免 v-model 场景外部更新内部不响应）
  watch(
    () => props.deptName,
    (val) => {
      const normalized = val || '';
      if (normalized !== innerDeptName.value) {
        innerDeptName.value = normalized;
      }
    }
  );

  return {
    innerDeptName,
    filterNode,
    setDeptTreeRef,
    // 内部使用的 computed 辅助：与 resolvedTreeProps 联动
    _resolvedLabel: computed(() => resolvedTreeProps?.value?.label || 'name')
  };
}
