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
  <qt-search-bar :items="items" :config="{ form: { disabled } }" :params="params" :table-ref="tableRef"
                 @query="prepareQuery" @reset="resetQuery" />
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  params: { type: Object, required: true },
  tableRef: Object,
  sourceSystemSelect: { type: Object, required: true },
  approvalStatusOptions: { type: Array, required: true },
  disabled: Boolean
})

const items = computed(() => [
  { label: '数据名称', prop: 'name', component: { is: 'input', placeholder: '请输入数据名称' } },
  { label: '来源系统', prop: 'sourceSystem', component: props.sourceSystemSelect },
  { label: '状态', prop: 'approvalStatus', component: { is: 'select', options: props.approvalStatusOptions } }
])

function prepareQuery() {
  for (const { prop } of items.value) {
    props.params[prop] = String(props.params[prop] ?? '').trim() || undefined
  }
  if ('pageNum' in props.params) props.params.pageNum = 1
}

function resetQuery() {
  for (const { prop } of items.value) props.params[prop] = undefined
  if ('pageNum' in props.params) props.params.pageNum = 1
}
</script>
