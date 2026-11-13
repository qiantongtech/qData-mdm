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

<!--
  根据模型字段展示合并结果对应的各来源记录。
  @prop detail 合并管理详情数据。
  @author chentangwen
-->
<template>
  <qt-wrap :columns="tableStore.columns" :table-ref="tableRef" :config="tableWrapConfig">
    <template #actions-data>
      <GuideTipPanel
        type="plain"
        title="查看当前合并结果及其对应的来源记录，对比各来源系统提供的业务字段，核对数据差异，了解合并结果的数据来源。"
      />
    </template>
    <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" class="result-source-table">
      <template #rowIndex="{ row }">{{ row.sourceIndex }}</template>
      <template #sourceSystem="{ row }">
        <div class="qt-table-info">
          <div class="qt-table-info__main">
            <img :src="getSourceIcon(row.sourceSystem)" alt="" class="info-icon" />
            <span class="info-title" :title="row.sourceSystemTitle">{{ row.sourceSystemTitle }}</span>
          </div>
          <div class="info-desc" :title="row.sourceSystem.description">{{ row.sourceSystem.description || '-' }}</div>
        </div>
      </template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="MergeResultSourceTab">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getBase } from '@/api/model/base/base'
import { listIngestSource } from '@/api/ingest/source/source'

import { getProjectIcon } from '@/utils/imageUrl';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const props = defineProps({
  detail: { type: Object, default: null },
})

const tableRef = ref()
const modelFields = ref([])
const sourceSystems = ref([])
let requestVersion = 0

const tableWrapConfig = {
  fullContent: false,
  actions: { table: { search: false, refresh: true, columns: true } },
}

const sourceRows = computed(() => {
  const rows = new Map()
  const systemsByCode = new Map(sourceSystems.value.map(system => [system.code, system]))
  ;(props.detail?.fields || []).forEach((field) => {
    ;(field.candidates || []).forEach((candidate) => {
      const sourceKey = candidate.sourceRecordId ?? candidate.stableRecordId ?? candidate.sourceVersionId ?? candidate.systemCode ?? candidate.systemName ?? 'source'
      const key = `${candidate.systemCode || candidate.systemName || ''}:${sourceKey}`
      if (!rows.has(key)) {
        const sourceSystem = systemsByCode.get(candidate.systemCode) || {}
        const name = candidate.systemName || sourceSystem.name
        const code = candidate.systemCode || sourceSystem.code
        rows.set(key, {
          rowKey: key,
          systemCode: candidate.systemCode || '',
          sourceSystem,
          sourceSystemTitle: name ? `${name}${code ? ` (${code})` : ''}` : code || '-',
        })
      }
      const row = rows.get(key)
      const value = candidate.value
      row[sourceFieldProp(field)] = value === null || value === undefined || value === '' ? '-' : value
    })
  })
  return [...rows.values()]
})

const displayFields = computed(() => {
  const fields = new Map()
  modelFields.value.forEach((field) => fields.set(String(field.id ?? field.columnCode ?? field.code ?? field.fieldPath), normalizeField(field)))
  ;(props.detail?.fields || []).forEach((field) => {
    const key = String(field.modelColumnId ?? field.id ?? field.fieldPath)
    fields.set(key, { ...(fields.get(key) || {}), ...normalizeField(field) })
  })
  return [...fields.values()]
})

const tableStore = reactive({
  func: loadSourceRows,
  params: {},
  config: {
    initResquest: false,
    autoPagination: true,
    table: { rowKey: 'rowKey', stripe: true },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50, 100],
      notAutoScroll: true,
    },
  },
  columns: [],
})

/**
 * 根据当前模型的全部字段生成来源表列，字段值直接取自各来源快照。
 * @returns {*} 当前业务计算结果。
 */
function buildColumns() {
  const columns = [
    { label: '编号', prop: 'sourceIndex', slot: 'rowIndex', sortable: 'custom', width: 70, minWidth: 70, align: 'center' },
    { label: '来源系统', prop: 'systemCode', slot: 'sourceSystem', minWidth: 280, align: 'left', showOverflowTooltip: true },
  ]
  displayFields.value.forEach((field, index) => {
    columns.push({
      label: field.columnName || field.columnCode || field.fieldPath || `字段${index + 1}`,
      prop: sourceFieldProp(field),
      minWidth: 150,
      align: 'left',
      showOverflowTooltip: true,
    })
  })
  return columns
}

/**
 * 为模型字段生成来源表使用的稳定属性名。
 * @param {*} field 模型字段定义。
 * @returns {string} 用于页面展示的格式化文本。
 */
function sourceFieldProp(field) {
  const key = field.modelColumnId ?? field.id ?? field.fieldPath ?? field.columnCode ?? field.code ?? 'unknown'
  return `modelField_${key}`
}

/**
 * 来源快照沿用原始顺序编号，仅在用户点击编号表头后按编号升降序排列。
 * @param {*} params 业务筛选、分页和排序参数。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function loadSourceRows(params = {}) {
  const rows = sourceRows.value.map((row, index) => ({ ...row, sourceIndex: index + 1 }))
  if (params.orderByColumn === 'sourceIndex' && params.isAsc === 'descending') rows.reverse()
  return Promise.resolve({ data: rows })
}

/**
 * 统一模型接口和待融合详情的字段名、编码及字段 ID，便于跨接口映射。
 * @param {*} field 模型字段定义。
 * @returns {Object} 整理后的业务参数或展示数据。
 */
function normalizeField(field) {
  return {
    ...field,
    id: field.id ?? field.modelColumnId,
    modelColumnId: field.modelColumnId ?? field.id,
    fieldPath: field.fieldPath || field.path,
    columnName: field.columnName || field.name,
    columnCode: field.columnCode || field.code,
  }
}

/**
 * 详情切换时并行读取模型字段和来源系统，再重建动态列和来源行分页。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadModelFields() {
  const version = ++requestVersion
  modelFields.value = []
  sourceSystems.value = []
  const modelId = props.detail?.modelId
  const [modelResult, systemsResult] = await Promise.allSettled([
    modelId != null ? getBase(modelId) : Promise.resolve(null),
    props.detail ? listIngestSource({ pageNum: 1, pageSize: -1 }) : Promise.resolve(null),
  ])
  if (version !== requestVersion) return
  // 模型接口失败时仍使用详情中的字段和候选值。
  if (modelResult.status === 'fulfilled') {
    modelFields.value = (modelResult.value?.data?.fields || []).map(normalizeField)
  }
  if (systemsResult.status === 'fulfilled') {
    sourceSystems.value = systemsResult.value?.data?.rows || systemsResult.value?.data || []
  } else {
    console.warn('加载来源系统信息失败，保留候选值中的名称和编码', systemsResult.reason)
  }
  tableStore.columns = buildColumns()
  await nextTick()
  if (version === requestVersion) tableRef.value?.resetQuery()
}

watch(() => props.detail?.id, loadModelFields, { immediate: true })
</script>

<style scoped>
.result-source-table {
  margin-top: var(--qt-space-2);
}
</style>
