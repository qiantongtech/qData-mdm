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
  <div>
    <DetailInfo :data="{ ...row, _recordName: detail?.recordName || '主数据详情' }" mode="free"
      :header="{ idKey: 'ID', nameKey: '_recordName', statusKey: 'STATUS', statusOptions }"
      :items="detailItems"
    />
    <div v-loading="loading" class="pagecont-bottom">
      <template v-if="detail?.row">
        <div class="detail-tabs-bar mb15">
          <el-tabs v-model="activeTab" class="detail-tabs">
            <qt-tab-pane v-for="tab in tabs" :key="tab.key" :name="tab.key" :label="tab.label" />
          </el-tabs>
        </div>

        <SourcesTab v-if="activeTab === 'sources' && hasTab('sources')" :model-value="props.modelValue" :active-tab="activeTab"
          :instance-id="props.instanceId" :detail="props.detail"
        />

        <VersionsTab v-if="activeTab === 'versions' && hasTab('versions')" :model-value="props.modelValue" :active-tab="activeTab"
          :instance-id="props.instanceId" :detail="props.detail"
        />

        <ChangesTab v-if="activeTab === 'changes' && hasTab('changes')" :model-value="props.modelValue" :active-tab="activeTab"
          :instance-id="props.instanceId" :detail="props.detail"
        />

        <StatusesTab v-if="activeTab === 'statuses' && hasTab('statuses')" :model-value="props.modelValue" :active-tab="activeTab"
          :instance-id="props.instanceId" :detail="props.detail" :status-options="props.statusOptions"
        />

        <ApprovalsTab v-if="activeTab === 'approvals' && hasTab('approvals')" :model-value="props.modelValue" :active-tab="activeTab"
          :instance-id="props.instanceId" :detail="props.detail" :status-options="props.statusOptions"
          :approval-status-options="props.approvalStatusOptions"
        />

        <MasterDataInfo v-if="activeTab === 'info' && hasTab('info')" :model-value="props.modelValue"
          :instance-id="props.instanceId" :detail="props.detail" :status-options="props.statusOptions"
          :approval-status-options="props.approvalStatusOptions"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import DetailInfo from '@/components/DetailInfo/index.vue'
import MasterDataInfo from './MasterDataInfo.vue'
import SourcesTab from '@/views/master/source/detail/components/SourcesTab.vue'
import VersionsTab from '@/views/master/data/detail/components/VersionsTab.vue'
import ChangesTab from '@/views/master/change/detail/components/ChangesTab.vue'
import StatusesTab from '@/views/master/change/detail/components/StatusesTab.vue'
import ApprovalsTab from '@/views/master/change/detail/components/ApprovalsTab.vue'
import { parseTime } from '@/utils/anivia.js'
import { checkPermi } from '@/utils/permission'

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], default: null },
  loading: Boolean,
  detail: { type: Object, default: null },
  statusOptions: { type: Array, default: () => [] },
  approvalStatusOptions: { type: Array, default: () => [] }
})

const activeTab = ref('info')
const row = computed(() => props.detail?.row || {})

const detailItems = computed(() => [
  { label: '形成方式', key: 'DATA_SOURCE', formatter: value => formationLabel(value) },
  { label: '最近更新', value: row.value.UPDATED_TIME || row.value.UPDATE_TIME, formatter: formatDetailTime },
  { label: '更新人', value: row.value.UPDATED_BY || row.value.UPDATE_BY || '-' }
])

const tabs = computed(() => [
  { key: 'info', label: '主数据模型', permission: 'master:instance:query' },
  { key: 'sources', label: '来源数据', permission: 'master:instance:source:list' },
  { key: 'versions', label: '版本记录', permission: 'master:instance:version:list' },
  { key: 'changes', label: '变更记录', permission: 'master:instance:change:list' },
  { key: 'statuses', label: '状态记录', permission: 'master:instance:status:list' },
  { key: 'approvals', label: '审批记录', permission: 'master:instance:approval:list' }
].filter(tab => checkPermi([tab.permission])))
const hasTab = key => tabs.value.some(tab => tab.key === key)
watch(tabs, allowed => {
  if (!allowed.some(tab => tab.key === activeTab.value)) activeTab.value = allowed[0]?.key || ''
}, { immediate: true, flush: 'sync' })

watch(() => props.modelValue, () => {
  activeTab.value = tabs.value[0]?.key || ''
})

function formatDetailTime(value) {
  return parseTime(value) || '-'
}

function formationLabel(value) {
  if (value === undefined || value === null || value === '') return '-'
  return { MANUAL: '平台录入', MERGE: '多源融合', IMPORT: '批量导入' }[value] || value
}

</script>

<style scoped lang="scss">
.detail-tabs-bar {
  display: flex;
  align-items: stretch;
  min-width: 0;
  border-bottom: 2px solid var(--qt-divider-color);
}

.detail-tabs {
  flex: 1;
  min-width: 0;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

</style>
