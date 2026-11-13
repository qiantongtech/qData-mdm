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
  <div class="model-data-list">
    <qt-wrap v-if="view === 'formal'" :columns="columns" :table-ref="toolbarRef || tableRef">
      <template #search>
        <ModelDataSearch :params="params" :table-ref="tableRef" :source-system-select="sourceSystemSelect"
                         :approval-status-options="approvalStatusOptions" :disabled="searchDisabled" />
      </template>
      <template #actions-data>
        <ModelDataTabs v-bind="tabs" @change="changeTab" />
      </template>
      <div class="model-data-actions"><slot name="actions" /></div>
      <slot />
    </qt-wrap>
    <ApprovalPanel v-else-if="canViewApprovals" :instance-id="instanceId" :approval-status-options="approvalStatusOptions" full-content>
      <template #tabs><ModelDataTabs v-bind="tabs" @change="changeTab" /></template>
    </ApprovalPanel>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { checkPermi } from '@/utils/permission'
import ApprovalPanel from '../../change/request/components/ApprovalPanel.vue'
import ModelDataSearch from './ModelDataSearch.vue'
import ModelDataTabs from './ModelDataTabs.vue'

const view = defineModel('view', { type: String, required: true })
const props = defineProps({
  instanceId: [String, Number],
  columns: { type: Array, required: true },
  params: { type: Object, required: true },
  tableRef: Object,
  toolbarRef: Object,
  sourceSystemSelect: { type: Object, required: true },
  statusOptions: { type: Array, required: true },
  approvalStatusOptions: { type: Array, required: true },
  searchDisabled: Boolean
})
const canViewApprovals = computed(() => checkPermi(['master:instance:approval:list']))
const tabs = computed(() => ({
  activeTab: view.value === 'approvals' ? 'approvals' : (props.params.status || 'all'),
  statusOptions: props.statusOptions,
  canViewApprovals: canViewApprovals.value
}))

function changeTab(tab) {
  if (tab === 'approvals') {
    if (canViewApprovals.value) view.value = 'approvals'
    return
  }
  props.params.status = tab === 'all' ? undefined : tab
  if ('pageNum' in props.params) props.params.pageNum = 1
  // 从审批返回时，正式数据表会在挂载时使用新的状态加载。
  if (view.value === 'formal') props.tableRef?.getList()
  view.value = 'formal'
}

watch(canViewApprovals, allowed => {
  if (!allowed) view.value = 'formal'
})
</script>

<style scoped lang="scss">
.model-data-list :deep(.qt-wrap--actions:has(.model-data-tabs)) {
  align-items: flex-end;
  box-shadow: inset 0 -2px var(--qt-border-color);

  .data-actions {
    flex: 1;
    min-width: 0;
  }

  .table-actions {
    flex-shrink: 0;
    padding-bottom: 8px;
  }
}

.model-data-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;

  &:not(:empty) {
    margin-bottom: 15px;
  }

  :deep(.el-button) {
    height: 28px;
    margin-left: 0;
    padding: 8px 11px;
    font-size: 12px;
  }
}
</style>
