/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import { ref, watch } from 'vue';

export function useTreeSearch({ props, emit, resolvedTreeProps, deptTreeRef, resolvedTreeData, modelNodeTypes }) {
  const innerDeptName = ref(props.deptName || '');
  let expandedBeforeSearch = null;

  function filterNode(value, data) {
    if (typeof props.filterNodeMethod === 'function') return props.filterNodeMethod(value, data);
    if (!value) return true;
    const label = data[resolvedTreeProps.value.label] || data.name || '';
    if (modelNodeTypes.has(data.nodeType)) {
      const term = value.toLowerCase();
      return [label, data.engName, data.code].some(field => field != null && String(field).toLowerCase().includes(term));
    }
    return String(label).includes(value) || Boolean(data.engName && String(data.engName).includes(value));
  }

  function visitBranches(nodes, visit) {
    for (const node of nodes) {
      if (!node.childNodes.length) continue;
      visit(node);
      visitBranches(node.childNodes, visit);
    }
  }

  watch([innerDeptName, deptTreeRef, resolvedTreeData], ([value, tree], [previousValue]) => {
    if (!tree) return;
    const roots = tree.root.childNodes;
    const modelTree = roots.some(node => modelNodeTypes.has(node.data.nodeType));
    const term = modelTree ? value.trim() : value;
    if (!term && !previousValue && !expandedBeforeSearch) return;
    if (modelTree && term && !expandedBeforeSearch) {
      expandedBeforeSearch = new Set();
      visitBranches(roots, node => {
        if (node.expanded) expandedBeforeSearch.add(node.key);
      });
    }
    tree.filter(term);
    if (!term && expandedBeforeSearch) {
      visitBranches(roots, node => {
        if (expandedBeforeSearch.has(node.key)) node.expand();
        else node.collapse();
      });
      expandedBeforeSearch = null;
    }
  }, { flush: 'post' });

  watch(innerDeptName, value => emit('update:deptName', value));
  watch(() => props.deptName, value => {
    innerDeptName.value = value || '';
  });

  return { innerDeptName, filterNode };
}
