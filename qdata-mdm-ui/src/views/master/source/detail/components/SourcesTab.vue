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
  <div v-loading="sourceLoading">
    <el-alert v-if="sourceError" :title="sourceError" type="error" :closable="false" show-icon class="mb15" />
    <qt-wrap :columns="recordTableStore.columns" :table-ref="recordTableRef" :config="recordTableStore.config">
      <template #actions-data>
        <div class="h2-titles">来源记录</div>
      </template>
      <el-text v-if="!sourceLoading && !sourceError && !sourceData.records?.length" type="info">
        {{ detail?.row?.DATA_SOURCE === 'MANUAL' ? '平台录入，暂无外部来源记录' : '暂无有效来源记录' }}
      </el-text>
      <qt-table v-bind="recordTableStore" ref="recordTableRef">
        <template #text="{ row, column_data }">{{ row[column_data.prop] }}</template>
        <template #lastReceiveTime="{ row }">{{ parseTime(row.lastReceiveTime) || '-' }}</template>
        <template #action="{ row }">
          <el-button link type="primary" icon="View" @click="handleView(row)">查看</el-button>
        </template>
      </qt-table>
    </qt-wrap>
    <el-dialog
      v-model="dialogVisible"
      title="来源记录"
      width="800px"
      destroy-on-close
      draggable
      class="dialog"
    >
      <DescriptionsInfo
        v-if="selectedSource"
        :data="selectedSource"
        :items="sourceInfoItems"
        :column="1"
        :border="true"
        placeholder="-"
      >
        <template #modelData>
          <pre class="form-readonly textarea model-data-pre">{{ selectedSource.modelData || '-' }}</pre>
        </template>
      </DescriptionsInfo>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <qt-wrap :columns="fieldSourceTableStore.columns" :table-ref="fieldSourceTableRef" class="mt15"
             :config="fieldSourceTableStore.config"
    >
      <template #actions-data>
        <div class="h2-titles">字段取值来源</div>
      </template>
      <qt-table v-bind="fieldSourceTableStore" ref="fieldSourceTableRef">
        <template #text="{ row, column_data }">{{ row[column_data.prop] }}</template>
      </qt-table>
    </qt-wrap>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch, nextTick, onUnmounted } from 'vue'
import { getSourceDetail } from '@/api/master/source/rel'
import { parseTime } from '@/utils/anivia.js'
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue'

const props = defineProps({
  modelValue: Boolean,
  activeTab: { type: String, default: '' },
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null }
})

const sourceLoading = ref(false)
const sourceError = ref('')
const sourceData = ref({ records: [], fieldSources: [] })
const selectedSource = ref(null)
const dialogVisible = ref(false)
const sourceInfoItems = computed(() => [
  { label: '来源系统', key: 'sourceSystemName' },
  { label: '稳定记录ID', key: 'stableRecordId' },
  { label: '来源版本ID', key: 'sourceVersionId' },
  { label: '关联生效时间', value: selectedSource.value?.effectiveTime, formatter: value => parseTime(value) || '-' },
  { label: '模型数据', slot: 'modelData' }
])
const recordTableRef = ref(null)
const fieldSourceTableRef = ref(null)

const recordTableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    actions: { table: { search: false } },
    autoPagination: true,
    table: { stripe: true }
  },
  columns: [
    { label: '来源系统', prop: 'sourceSystemName', minWidth: 130, align: 'left', slot: 'text' },
    { label: '来源记录ID', prop: 'sourceRecordId', minWidth: 130, align: 'left', slot: 'text' },
    { label: '来源业务主键', prop: 'sourceRecordKey', minWidth: 150, align: 'left', slot: 'text' },
    { label: '关联类型', prop: 'relationType', minWidth: 110, align: 'left', slot: 'text' },
    { label: '最近接收时间', prop: 'lastReceiveTime', minWidth: 175, align: 'left', slot: 'lastReceiveTime' },
    { label: '操作', prop: 'action', width: 105, align: 'left', slot: 'action' }
  ],
  func: loadSourceRecords
})

const fieldSourceTableStore = reactive({
  config: {
    initResquest: false,
    fullContent: false,
    notPagination: true,
    actions: { table: { search: false } },
    table: { stripe: true }
  },
  columns: [
    { label: '权威字段路径', prop: 'fieldPath', minWidth: 160, align: 'left', slot: 'text' },
    { label: '来源系统', prop: 'sourceSystemName', minWidth: 130, align: 'left', slot: 'text' },
    { label: '来源记录ID', prop: 'sourceRecordId', minWidth: 130, align: 'left', slot: 'text' },
    { label: '来源字段路径', prop: 'sourceFieldPath', minWidth: 160, align: 'left', slot: 'text' }
  ],
  // 字段来源复用来源详情响应，不再单独请求后端。
  func: async () => ({ data: sourceData.value.fieldSources || [] })
})

function handleView(row) {
  selectedSource.value = row
  dialogVisible.value = true
}

// 弹窗状态变化、新请求或组件卸载后，旧请求不得覆盖当前结果。
let sourceRequest = 0
let latestSourceRequest = Promise.resolve()

watch([
  () => props.modelValue,
  () => props.instanceId,
  () => props.detail?.row?.ID,
  () => props.activeTab
], async () => {
  sourceRequest++
  sourceData.value = { records: [], fieldSources: [] }
  selectedSource.value = null
  dialogVisible.value = false
  sourceError.value = ''
  sourceLoading.value = false
  await nextTick()
  fieldSourceTableRef.value?.getList()
  recordTableRef.value?.resetQuery()
}, { immediate: true })

onUnmounted(() => {
  sourceRequest++
})

async function loadSourceRecords() {
  const request = ++sourceRequest
  const instanceId = props.instanceId
  const recordId = props.detail?.row?.ID
  const canQuery = props.modelValue && props.activeTab === 'sources' && instanceId && recordId
  selectedSource.value = null
  dialogVisible.value = false
  sourceError.value = ''
  sourceLoading.value = Boolean(canQuery)
  latestSourceRequest = (async () => {
    let data = { records: [], fieldSources: [] }
    try {
      if (canQuery) {
        const response = await getSourceDetail(instanceId, recordId)
        data = response?.data || data
      }
    } catch {
      if (request === sourceRequest) sourceError.value = '来源详情加载失败，请稍后重试。'
    }
    if (request !== sourceRequest || String(instanceId) !== String(props.instanceId)
        || String(recordId) !== String(props.detail?.row?.ID)
        || (canQuery && (!props.modelValue || props.activeTab !== 'sources'))) return
    sourceData.value = data
    sourceLoading.value = false
    fieldSourceTableRef.value?.getList()
  })()

  // QtTable 会应用每次 Promise 的返回值；旧调用也等待最新请求，避免旧结果覆盖新结果。
  let pendingRequest
  do {
    pendingRequest = latestSourceRequest
    await pendingRequest
  } while (pendingRequest !== latestSourceRequest)
  return { data: sourceData.value.records || [] }
}
</script>
