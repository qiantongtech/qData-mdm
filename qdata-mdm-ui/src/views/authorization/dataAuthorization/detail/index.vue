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
  <div v-loading="loading" class="app-container">
    <el-empty v-if="loadError" :description="loadError">
      <el-button
        type="primary"
        plain
        icon="Back"
        @click="returnToList"
      >返回列表</el-button>
      <el-button
        v-if="route.query.id"
        type="primary"
        plain
        icon="Refresh"
        @click="loadDetail"
      >重新加载</el-button>
    </el-empty>
    <template v-else-if="detail.id">
      <detail-info
        :data="headerData"
        :header="headerConfig"
        :items="detailItems"
        mode="free"
        @back="returnToList"
      >
        <template #subject>
          <el-space :size="6" wrap>
            <span>{{ detail.subjectName || '-' }}</span>
            <dict-tag :options="subjectTypeOptions" :value="subjectType" size="small" />
          </el-space>
        </template>
        <template #model="{ item }">
          <div class="qt-table-info model-info">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(modelPresentation)" alt="" class="info-icon" />
              <span
                class="info-title text-ellipsis"
                :title="item.value"
                :role="canOpenModel ? 'link' : undefined"
                :tabindex="canOpenModel ? 0 : undefined"
                @click="openModelDetail()"
                @keydown.enter="openModelDetail()"
                @keydown.space.prevent="openModelDetail()"
              >{{ item.value }}</span>
              <dict-tag
                v-if="modelPresentation.type"
                :options="model_type"
                :value="modelPresentation.type"
                size="small"
                type="primary"
              />
              <dict-tag
                v-if="modelPresentation.versionNo"
                class="ml4"
                :class="{ pointer: canOpenModel }"
                :type="modelVersionTagType"
                size="small"
                :role="canOpenModel ? 'link' : undefined"
                :tabindex="canOpenModel ? 0 : undefined"
                @click="openModelDetail('versions')"
                @keydown.enter="openModelDetail('versions')"
                @keydown.space.prevent="openModelDetail('versions')"
              >{{ modelPresentation.versionNo }}</dict-tag>
            </div>
          </div>
        </template>
        <template #operations>
          <el-space v-if="detail.operations?.length" :size="8" wrap>
            <dict-tag
              v-for="operation in detail.operations"
              :key="operation"
              type="primary"
              :value="operationMap[operation] || operation"
            />
          </el-space>
          <span v-else>-</span>
        </template>
      </detail-info>

      <div class="pagecont-bottom">
        <el-tabs model-value="fields" class="demo-tabs">
          <qt-tab-pane label="字段权限" name="fields">
            <qt-wrap
              :columns="fieldTableStore.columns"
              :table-ref="fieldTableRef"
              :config="fieldTableStore.config"
            >
              <template #actions-data>
                <guide-tip-panel type="plain" allow-wrap title="按字段核对本次授权的查看和修改权限。仅允许查看的字段为只读。" />
              </template>
              <qt-table :key="detail.id" ref="fieldTableRef" v-bind="fieldTableStore">
                <template #view="{ row }">
                  <dict-tag :type="row.view ? 'primary' : 'info'" :value="row.view ? '是' : '否'" />
                </template>
                <template #edit="{ row }">
                  <dict-tag :type="row.edit ? 'primary' : 'info'" :value="row.edit ? '是' : '否'" />
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<script setup name="DataAuthorizationDetail">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import DetailInfo from '@/components/DetailInfo/index.vue'
import { getDataAuthorization } from '@/api/authorization/dataAuthorization.js'
import { getBase } from '@/api/model/base/base.js'
import { getModelIcon } from '@/utils/imageUrl'
import { useDict } from '@/utils/dict'
import {
  DATA_AUTHORIZATION_OPERATIONS as operationMap,
  DATA_AUTHORIZATION_SUBJECT_TYPES as subjectTypeOptions
} from '@/utils/authorization/dataAuthorization.js'

const { model_type, model_status } = useDict('model_type', 'model_status')
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const detail = ref({})
const model = ref({})
const loading = ref(false)
const loadError = ref('')
const fieldTableRef = ref()
let requestVersion = 0

const subjectType = computed(() => ({ USER: 'user', ROLE: 'role', ORG: 'organization' })[detail.value.subjectType] || detail.value.subjectType)
const headerData = computed(() => ({
  ...detail.value,
  title: detail.value.grantName || `${detail.value.subjectName || '主体'}对${detail.value.modelName || model.value.name || '模型'}的数据授权`
}))
const headerConfig = {
  className: 'clearfixs', idKey: 'id', nameKey: 'title', routerBack: false,
  statusKey: 'displayStatus',
  statusOptions: [
    { value: 'EFFECTIVE', label: '有效', elTagType: 'success' },
    { value: 'INACTIVE', label: '未生效', elTagType: 'warning' }
  ]
}
const inactiveReasons = { REVOKED: '已撤销', NOT_STARTED: '未到生效时间', EXPIRED: '已过期', INVALID: '授权无效' }
const formatDate = value => value ? proxy.parseTime(value, '{y}-{m}-{d}') || '-' : '-'
const modelPresentation = computed(() => ({
  name: detail.value.modelName || model.value.name,
  code: detail.value.modelCode || model.value.code,
  icon: detail.value.modelIcon || model.value.icon,
  type: detail.value.modelType || model.value.type,
  versionNo: detail.value.modelVersionNo || model.value.releaseVersion?.versionNo || model.value.version?.versionNo,
  versionStatus: detail.value.modelVersionStatus ?? model.value.version?.status ?? model.value.releaseVersion?.status
}))
const modelVersionTagType = computed(() => {
  const status = modelPresentation.value.versionStatus
  return model_status.value.find(item => String(item.value) === String(status))?.elTagType || 'info'
})
const canOpenModel = computed(() => detail.value.modelId != null && proxy.$auth.hasPermi('model:base:query'))
const modelFields = computed(() => model.value.fields || [])
const fieldsById = computed(() => new Map(modelFields.value.map(field => [String(field.id), field])))
const fieldRows = computed(() => (detail.value.fieldPermissions || [])
  .filter(field => field.view || field.edit)
  .map((field, index) => {
    const definition = fieldsById.value.get(String(field.fieldId))
    return {
      ...field, number: index + 1,
      fieldName: field.fieldName || definition?.name || `字段 ID：${field.fieldId}`,
      fieldCode: field.fieldCode || definition?.code || '-',
      view: Boolean(field.view), edit: Boolean(field.edit)
    }
  }))
const scopeSummary = computed(() => {
  const fieldNames = Object.fromEntries(modelFields.value.map(field => [field.code, field.name]))
  const operators = { EQ: '等于', NE: '不等于', CONTAINS: '包含' }
  return (detail.value.dataScopeConfig?.groups || []).map(group => {
    const conditions = (group.conditions || []).map(condition => {
      const fieldName = fieldNames[condition.fieldCode] || fieldsById.value.get(String(condition.fieldId))?.name || condition.fieldCode || condition.fieldId
      return `${fieldName} ${operators[condition.operator] || condition.operator} ${condition.value ?? ''}`
    })
    return conditions.length ? `（${conditions.join(group.logic === 'OR' ? ' 或 ' : ' 且 ')}）` : ''
  }).filter(Boolean).join(' 或 ')
})
const detailItems = computed(() => [
  { label: '授权主体', slot: 'subject' },
  { label: '主数据模型', value: formatModelName(modelPresentation.value), slot: 'model' },
  { label: '数据范围', key: 'dataScopeType', dictOptions: [
    { value: 'ALL', label: '全部数据', elTagType: 'success' },
    { value: 'CONDITION', label: '条件范围', elTagType: 'warning' }
  ] },
  ...(detail.value.dataScopeType === 'CONDITION' ? [
    { label: '范围条件', formatter: () => scopeSummary.value || '-', span: 24, className: 'mt2 mb2' }
  ] : []),
  { label: '有效期', formatter: () => detail.value.longTerm ? '长期有效' : `${formatDate(detail.value.effectiveTime)} ~ ${formatDate(detail.value.expireTime)}` },
  ...(subjectType.value === 'organization' ? [
    { label: '组织范围', formatter: () => detail.value.includeChildOrg ? '包含下级组织' : '仅当前组织' }
  ] : []),
  { label: '操作权限', slot: 'operations' },
  ...(detail.value.inactiveReason ? [
    { label: '状态说明', formatter: () => inactiveReasons[detail.value.inactiveReason] || detail.value.inactiveReason }
  ] : [])
])

function formatModelName({ name, code }) {
  return name ? (code ? `${name} (${code})` : name) : code || detail.value.modelId || '-'
}

const fieldTableStore = reactive({
  config: {
    fullContent: false, autoPagination: true,
    actions: { table: { search: false, refresh: true } },
    table: { stripe: true, rowKey: 'fieldId' }
  },
  columns: [
    { label: '编号', prop: 'number', minWidth: 80, sortable: 'custom' },
    { label: '字段名称', prop: 'fieldName', minWidth: 180, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '字段编码', prop: 'fieldCode', minWidth: 180, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '允许查看', prop: 'view', slot: 'view', minWidth: 180, align: 'center' },
    { label: '允许修改', prop: 'edit', slot: 'edit', minWidth: 180, align: 'center' }
  ],
  func: async (params = {}) => {
    const rows = [...fieldRows.value]
    if (params.orderByColumn === 'number' && ['ascending', 'descending'].includes(params.isAsc)) {
      const direction = params.isAsc === 'ascending' ? 1 : -1
      rows.sort((a, b) => (a.number - b.number) * direction)
    }
    return { data: { rows, total: rows.length } }
  }
})

function returnToList() {
  router.push('/authorization/dataAuthorization')
}

function openModelDetail(tab) {
  if (!canOpenModel.value) return
  router.push({ path: '/model/base/detail', query: { id: detail.value.modelId, ...(tab ? { tab } : {}) } })
}

async function loadDetail() {
  const version = ++requestVersion
  const id = route.query.id
  detail.value = {}
  model.value = {}
  loadError.value = ''
  loading.value = false
  if (!id || Array.isArray(id)) {
    loadError.value = '缺少有效的授权编号'
    return
  }
  loading.value = true
  try {
    const response = await getDataAuthorization(id)
    if (version !== requestVersion) return
    const record = response?.data ?? response
    if (!record?.id) {
      loadError.value = '数据授权不存在或已删除'
      return
    }
    let modelDetail = {}
    // 补齐模型图标、类型、发布版本，以及字段和范围条件中的字段名。
    if (record.modelId) {
      try {
        const modelResponse = await getBase(record.modelId)
        modelDetail = modelResponse?.data || {}
      } catch {
        // 模型不可用时保留授权信息，字段名称回退为字段 ID。
      }
    }
    if (version !== requestVersion) return
    model.value = modelDetail
    detail.value = record
  } catch {
    if (version === requestVersion) loadError.value = '数据授权详情加载失败'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => [route.path, route.query.id], ([path]) => {
  if (path === '/authorization/dataAuthorization/detail') loadDetail()
  else requestVersion++
}, { immediate: true })
</script>

<style scoped>
.model-info .info-title[role='link'] {
  cursor: pointer;
  color: var(--el-color-primary);
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}
</style>
