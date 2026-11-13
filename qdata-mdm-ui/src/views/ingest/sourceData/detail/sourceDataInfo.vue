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
  来源信息展示：整理记录标识、来源系统、主数据模型及融合状态。
  props：detail 为来源记录详情。
-->
<template>
  <section class="source-detail-info">
    <div class="h2-title">来源信息</div>
    <source-data-field-grid :items="sourceFields" />
  </section>
</template>

<script setup>
import { computed } from 'vue'

import SourceDataFieldGrid from '@/views/ingest/sourceData/detail/sourceDataFieldGrid.vue'

const GOVERNANCE_STATUS = Object.freeze({
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  MERGED: 'MERGED'
})

const statusDisplay = {
  [GOVERNANCE_STATUS.PENDING]: { label: '待融合', type: 'warning' },
  [GOVERNANCE_STATUS.PROCESSING]: { label: '融合中', type: 'primary' },
  [GOVERNANCE_STATUS.MERGED]: { label: '已融合', type: 'success' }
}

function governanceStatusLabel(status) {
  return statusDisplay[String(status)]?.label || '-'
}

function governanceStatusType(status) {
  return statusDisplay[String(status)]?.type || 'info'
}

const props = defineProps({
  detail: { type: Object, default: () => ({}) }
})

const sourceFields = computed(() => [
  { label: '来源记录ID', value: props.detail.sourceRecordKey },
  {
    label: '来源系统',
    value: props.detail.sourceSystemName
      || (props.detail.sourceSystemId ? `来源系统ID：${props.detail.sourceSystemId}` : '-')
  },
  { label: '主数据模型', value: formatModelName(props.detail) },
  {
    label: '接收时间',
    value: formatTime(props.detail.receiveTime || props.detail.lastReceiveTime)
  },
  {
    label: '融合状态',
    value: governanceStatusLabel(props.detail.governanceStatus),
    tagType: governanceStatusType(props.detail.governanceStatus)
  }
])

/**
 * 优先展示模型名称及编码，缺少名称时回退到模型 ID。
 * @param {Object} detail 来源记录详情。
 * @returns {string} 模型展示名称。
 */
function formatModelName(detail) {
  const name = detail.modelName || (detail.modelId ? `模型ID：${detail.modelId}` : '-')
  return detail.modelCode ? `${name}（${detail.modelCode}）` : name
}

/** 统一日期时间分隔符，无法匹配时保留原文。 */
function formatTime(value) {
  if (!value) return '-'
  const result = String(value).match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/)
  return result ? `${result[1]} ${result[2]}` : String(value)
}
</script>

