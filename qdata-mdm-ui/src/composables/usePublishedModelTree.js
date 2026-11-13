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

import { onBeforeUnmount, onMounted, ref } from 'vue';
import { getPublishedModelTree } from '@/api/model/base/base';

/** 域、目录和模型的业务 ID 可能重复，树节点键需要区分类型。 */
export function withModelTreeKeys(nodes) {
  return nodes.map(node => ({
    ...node,
    treeKey: `${node.nodeType}:${node.id}`,
    children: withModelTreeKeys(node.children || []),
  }));
}

/** 模型业务页面负责取数，QtTree 只接收树数据并提供通用交互。 */
export function usePublishedModelTree(transform = nodes => nodes, autoLoad = true) {
  const data = ref([]);
  const loading = ref(false);
  let pending = null;
  let disposed = false;

  function reload() {
    if (disposed) return Promise.resolve(data.value);
    if (pending) return pending;
    loading.value = true;
    pending = getPublishedModelTree().then(response => {
      if (!disposed) data.value = transform(response.data || []);
      return data.value;
    }).finally(() => {
      pending = null;
      if (!disposed) loading.value = false;
    });
    return pending;
  }

  onMounted(() => {
    if (autoLoad) reload().catch(() => {});
  });
  onBeforeUnmount(() => { disposed = true; });
  return { data, loading, reload };
}
