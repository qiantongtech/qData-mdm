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
  @file operationLog/AuditOperationCell.vue
  @description 审计列表的模型、归属位置和操作信息单元格
  @author zyy
  @prop kind 单元格类型
  @prop row 当前审计记录
  @prop scope 业务范围
-->
<template>
  <template v-if="kind === 'model'">
    <div v-if="modelTitle || modelVersion" class="qt-table-info model-info" :title="modelHint">
      <div class="qt-table-info__main">
        <img :src="getModelIcon({ type: row.modelType, icon: row.modelIcon })" alt="" class="info-icon" />
        <span
          v-if="modelTitle"
          class="info-title model-info-title text-ellipsis"
          :role="canOpenModel ? 'link' : undefined"
          :tabindex="canOpenModel ? 0 : undefined"
          @click="openModelDetail()"
          @keydown.enter="openModelDetail()"
          @keydown.space.prevent="openModelDetail()"
        >{{ modelTitle }}</span>
        <span v-else class="info-title">-</span>
        <dict-tag v-if="row.modelType" :options="model_type" :value="row.modelType" size="small" type="primary" />
        <dict-tag
          v-if="modelVersion"
          class="ml4"
          :class="{ pointer: canOpenModel }"
          size="small"
          type="info"
          :role="canOpenModel ? 'link' : undefined"
          :tabindex="canOpenModel ? 0 : undefined"
          @click="openModelDetail('versions')"
          @keydown.enter="openModelDetail('versions')"
          @keydown.space.prevent="openModelDetail('versions')"
        >{{ modelVersion }}</dict-tag>
      </div>
      <div v-if="showModelDescription" class="info-desc text-ellipsis">{{ row.modelDescription || '-' }}</div>
    </div>
    <span v-else>-</span>
  </template>

  <template v-else-if="kind === 'location'">
    <div v-if="row.projectName || directoryName" class="qt-table-info flex-column-gap overflow-hidden">
      <div class="qt-table-info__main">
        <img :src="getProjectIcon(row.projectIcon)" alt="" class="info-icon" />
        <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
      </div>
      <div v-if="directoryName" class="text-ellipsis flex-center" :title="directoryName">
        <dict-tag :value="directoryName" size="small" type="primary" />
      </div>
    </div>
    <span v-else class="text-muted">-</span>
  </template>

  <template v-else-if="kind === 'masterData'">
    <template v-if="masterName">
      <div :title="`${masterName}（${masterCode || '-'}）`" class="cell-main master-name">
        <el-link
          v-if="getDisplayTitle(masterName) && $auth.hasPermi('audit:log:query')"
          type="primary" :underline="false"
          role="link" tabindex="0"
          class="qt-detail-link text-ellipsis"
          :title="String(masterName)"
          @click.stop="$emit('open-detail', row)"
          @keydown.enter.stop.prevent="$emit('open-detail', row)"
          @keydown.space.stop.prevent="$emit('open-detail', row)"
        >{{ masterName }}</el-link>
        <span v-else class="qt-detail-link text-ellipsis" :title="getDisplayTitle(masterName)">{{ getDisplayTitle(masterName) ?? '-' }}</span>（{{ masterCode || '-' }}）
      </div>
      <div class="master-tags">
        <dict-tag v-if="masterLifecycle" :type="lifecycleTagType(masterLifecycle)">{{ masterLifecycle }}</dict-tag>
        <span v-else class="cell-sub">-</span>
        <dict-tag v-if="masterVersion" class="ml5" type="primary">{{ masterVersion }}</dict-tag>
        <span v-else class="cell-sub">-</span>
      </div>
    </template>
    <span v-else>-</span>
  </template>

  <template v-else-if="kind === 'operation'">
    <div class="operation-tags">
      <dict-tag v-if="row.type" :type="OPERATION_TYPE_TAGS[row.type] || 'primary'">{{ row.type }}</dict-tag>
      <span v-else>-</span>
      <dict-tag v-if="row.operateResult" class="ml5" :type="resultTagType(row.operateResult)">{{ row.operateResult }}</dict-tag>
      <span v-else>-</span>
    </div>
  </template>

  <template v-else-if="kind === 'operator'">
    <div class="cell-main">{{ row.operatorName || '-' }}</div>
    <div class="cell-sub">{{ row.operatorPhone || '-' }}</div>
  </template>
</template>

<script setup name="AuditOperationCell">
import { getDisplayTitle } from '@/utils/anivia.js'
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import {useDict} from '@/utils/dict';
import { computed, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'

const { model_type } = useDict('model_type')

const OPERATION_TYPE_TAGS = {
  新增: 'success',
  复制: 'success',
  失效: 'danger',
  冻结: 'info',
  归档: 'info',
  历史版本恢复: 'warning',
  发布模型: 'success',
  停用模型: 'danger',
}

defineEmits(['open-detail'])
const props = defineProps({
  kind: { type: String, required: true },
  row: { type: Object, required: true },
  scope: { type: String, default: '' },
  modelHint: String,
  showModelDescription: { type: Boolean, default: true },
})

const router = useRouter()
const { proxy } = getCurrentInstance()
// 模型展示信息由审计接口按真实业务记录的明确关联返回。
const modelTitle = computed(() => {
  const { modelDisplayName, modelName, model, modelBusinessCode, modelCode } = props.row
  const name = firstDisplayValue(modelDisplayName, modelName, model)
  const code = firstDisplayValue(modelBusinessCode, modelCode)
  return name ? (code ? `${name} (${code})` : name) : code
})
const modelVersion = computed(() => {
  const versionNo = firstDisplayValue(props.row.modelVersionNo)
  const legacyVersion = firstDisplayValue(props.row.modelVersion)
  return versionNo || (props.scope ? formatVersion(legacyVersion) : legacyVersion)
})
const directoryName = computed(() => props.row.layerName?.split(' / ').map(name => name.trim()).filter(Boolean).at(-1))
const canOpenModel = computed(() => props.row.modelId != null && proxy.$auth.hasPermi('model:base:query'))
const masterName = computed(() => props.row.masterDataName || (props.scope === 'master' ? props.row.objectName : ''))
// 内部对象ID不作为业务编码显示。
const masterCode = computed(() => props.row.businessCode || props.row.masterDataCode || '')
const masterLifecycle = computed(() => props.row.masterLifecycle || props.row.lifecycle)
const masterVersion = computed(() => formatVersion(props.row.masterVersion || props.row.dataVersion))

function openModelDetail(tab) {
  if (!canOpenModel.value) return
  router.push({
    path: '/model/base/detail',
    query: { id: props.row.modelId, ...(tab ? { tab } : {}) }
  })
}

function firstDisplayValue(...values) {
  return values.map(value => String(value ?? '').trim()).find(value => value && value !== '-' && value !== '—')
}

function formatVersion(value) {
  if (value == null || value === '') return ''
  const version = String(value).trim()
  const match = version.match(/^v?(\d+)(?:\.(\d+))?$/i)
  return match ? `V${match[1]}.${match[2] ?? '0'}` : version
}

function lifecycleTagType(value) {
  if (['生效', '有效'].includes(value)) return 'success'
  if (['失效', '冻结', '归档'].includes(value)) return 'info'
  return 'primary'
}

function resultTagType(value) {
  if (value === '成功') return 'success'
  if (value === '失败') return 'danger'
  return 'info'
}
</script>

<style lang="scss" scoped>
.cell-main {
  line-height: 20px;
}
.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
  line-height: 18px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.model-info {
  min-width: 0;
}
.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}
.model-info-title {
  display: block;
}
.model-info-title[role='link'] {
  cursor: pointer;
  color: var(--el-color-primary);
}
.master-name {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.master-tags {
  margin-top: 4px;
}
</style>
