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
  <div class="app-container">
    <RecordDetailContent
      v-if="recordDetail && canViewDetail"
      :key="`${instanceId}-${masterDataId}`"
      :model-value="true"
      :instance-id="instanceId"
      :loading="loading"
      :detail="recordDetail"
      :status-options="master_data_status"
      :approval-status-options="master_change_request_status"
    />
    <div v-else class="record-placeholder" v-loading="loading">
      <el-empty v-if="error" :description="error">
        <el-button type="primary" plain @click="returnToList">← 返回列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup name="MasterRecordDetail">
import { computed, getCurrentInstance, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMasterRecordDetail } from '@/api/master/data/data'
import RecordDetailContent from './components/RecordDetailContent.vue'
import { checkPermi } from '@/utils/permission'
import { useDict } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const routeBase = computed(() => '/master/instance')
const { proxy } = getCurrentInstance()
const { master_data_status, master_change_request_status } = useDict(
  'master_data_status', 'master_change_request_status'
)
const instanceId = computed(() => route.query.instanceId)
const modelId = computed(() => route.query.modelId)
const masterDataId = computed(() => route.params.masterDataId)
const recordDetail = ref(null)
const loading = ref(false)
const error = ref('')
const canViewDetail = computed(() => checkPermi(['master:instance:query']))
let latestRequest = 0

watch([instanceId, masterDataId, modelId, canViewDetail], async ([requestedId, requestedRecordId, requestedModelId, allowed]) => {
  const requestId = ++latestRequest
  recordDetail.value = null
  error.value = ''
  loading.value = false
  if (!allowed) {
    error.value = '当前账号无主数据详情查看权限。'
    return
  }
  if (!requestedId || !requestedRecordId || !requestedModelId) {
    error.value = '缺少模型、实例或主数据ID，无法加载详情。'
    proxy.$modal.msgWarning(error.value)
    return
  }
  loading.value = true
  try {
    const response = await getMasterRecordDetail(requestedId, requestedRecordId, requestedModelId)
    if (requestId !== latestRequest) return
    recordDetail.value = response?.data?.row ? response.data : null
    if (!recordDetail.value) {
      error.value = '未找到这条主数据。'
      proxy.$modal.msgWarning(error.value)
    }
  } catch (err) {
    if (requestId === latestRequest) error.value = err?.msg || err?.message || '主数据详情加载失败，请稍后重试。'
  } finally {
    if (requestId === latestRequest) loading.value = false
  }
}, { immediate: true })

onUnmounted(() => { latestRequest++ })

function returnToList() {
  router.push({
    path: route.query.from === 'category' ? `${routeBase.value}/category` : `${routeBase.value}/data`,
    query: {
      instanceId: instanceId.value,
      modelId: route.query.modelId,
      modelVersionId: route.query.modelVersionId
    }
  })
}

</script>

<style scoped lang="scss">
.record-placeholder {
  min-height: 340px;
  padding: var(--qt-space-3) var(--qt-space-4);
  background: var(--qt-surface-background);
  border: 1px solid var(--qt-border-color);
}
.record-placeholder .el-button { margin-top: var(--qt-space-4); }
</style>
