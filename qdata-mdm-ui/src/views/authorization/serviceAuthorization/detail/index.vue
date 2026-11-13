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
                <guide-tip-panel type="plain" allow-wrap title="服务仅返回“允许查看”为“是”的字段。" />
              </template>
              <qt-table :key="detail.id" ref="fieldTableRef" v-bind="fieldTableStore">
                <template #view="{ row }">
                  <dict-tag :type="row.view ? 'primary' : 'info'" :value="row.view ? '是' : '否'" />
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<script setup name="ServiceAuthorizationDetail">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import DetailInfo from '@/components/DetailInfo/index.vue'
import { getServiceAuthorization } from '@/api/authorization/serviceAuthorization.js'
import { getService, listService } from '@/api/dist/service/service.js'
import { getPublishedModelStructure } from '@/api/model/base/base.js'
import {
  SERVICE_AUTHORIZATION_CLIENTS,
  SERVICE_AUTHORIZATION_FIELDS,
  SERVICE_AUTHORIZATION_SERVICES,
  SERVICE_AUTHORIZATION_SUBJECT_TYPES as subjectTypeOptions
} from '@/utils/authorization/serviceAuthorization.js'

const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()
const detail = ref({})
const service = ref({})
const modelFields = ref([])
const loading = ref(false)
const loadError = ref('')
const fieldTableRef = ref()
let requestVersion = 0

const subjectType = computed(() => detail.value.subjectType === 'CLIENT' ? 'client' : 'user')
const headerData = computed(() => ({
  ...detail.value,
  title: `${detail.value.subjectName || '主体'}对${detail.value.serviceName || '数据服务'}的服务授权`
}))
const headerConfig = {
  className: 'clearfixs',
  idKey: 'id',
  nameKey: 'title',
  routerBack: false,
  statusKey: 'displayStatus',
  statusOptions: [
    { value: 'EFFECTIVE', label: '有效', elTagType: 'success' },
    { value: 'INACTIVE', label: '未生效', elTagType: 'warning' }
  ]
}
const inactiveReasons = { REVOKED: '已撤销', NOT_STARTED: '未到生效时间', EXPIRED: '已过期', INVALID: '授权无效' }
const formatDate = value => value ? proxy.parseTime(value, '{y}-{m}-{d}') || '-' : '-'
const grantedFieldsById = computed(() => new Map(
  (detail.value.fieldPermissions || []).map(field => [String(field.fieldId), field])
))
const fieldRows = computed(() => {
  const serviceFields = (service.value.fields || []).filter(field => String(field.outputFlag) === '1')
  const fallbackFields = !serviceFields.length && detail.value.serviceId
    ? SERVICE_AUTHORIZATION_FIELDS.map(field => ({ modelColumnId: field.id }))
    : []
  const modelFieldsById = new Map(modelFields.value.map(field => [String(field.id), field]))
  const rows = [...serviceFields, ...fallbackFields].map(field => {
    const fieldId = field.modelColumnId
    const definition = modelFieldsById.get(String(fieldId))
      || SERVICE_AUTHORIZATION_FIELDS.find(item => String(item.id) === String(fieldId))
    const permission = grantedFieldsById.value.get(String(fieldId))
    return {
      fieldId,
      fieldName: permission?.fieldName || definition?.name || `字段 ID：${fieldId}`,
      fieldCode: permission?.fieldCode || definition?.code || '-',
      view: Boolean(permission?.view)
    }
  })
  const mergedRows = !rows.length
    ? (detail.value.fieldPermissions || []).map(field => ({
      ...field,
      fieldName: field.fieldName || `字段 ID：${field.fieldId}`,
      fieldCode: field.fieldCode || '-',
      view: Boolean(field.view)
    }))
    : rows.concat((detail.value.fieldPermissions || []).filter(field =>
      !rows.some(row => String(row.fieldId) === String(field.fieldId))
    ).map(field => ({
      ...field,
      fieldName: field.fieldName || `字段 ID：${field.fieldId}`,
      fieldCode: field.fieldCode || '-',
      view: Boolean(field.view)
    })))
  return mergedRows.map((field, index) => ({ ...field, number: index + 1 }))
})
const scopeSummary = computed(() => {
  const names = Object.fromEntries(fieldRows.value.map(field => [field.fieldCode, field.fieldName]))
  const operators = { EQ: '等于', NE: '不等于', CONTAINS: '包含' }
  return (detail.value.dataScopeConfig?.groups || []).map(group => {
    const conditions = (group.conditions || []).map(condition =>
      `${names[condition.fieldCode] || condition.fieldCode || condition.fieldId} ${operators[condition.operator] || condition.operator} ${condition.value ?? ''}`
    )
    return conditions.length ? `（${conditions.join(group.logic === 'OR' ? ' 或 ' : ' 且 ')}）` : ''
  }).filter(Boolean).join(' 或 ')
})
const detailItems = computed(() => [
  { label: '授权主体', slot: 'subject' },
  { label: '数据服务', key: 'serviceName' },
  { label: '服务编码', key: 'serviceCode' },
   ...(detail.value.dataScopeType === 'CONDITION' ? [
    { label: '范围条件', formatter: () => scopeSummary.value || '-', span: 24, className: 'mt2' }
  ] : []),
  { label: '服务版本', formatter: () => detail.value.serviceVersion || '-', className: 'mt2' },
  { label: '数据范围', key: 'dataScopeType', dictOptions: [
    { value: 'ALL', label: '全部数据', elTagType: 'success' },
    { value: 'CONDITION', label: '条件范围', elTagType: 'warning' }
  ], className: 'mt2' },
  { label: '有效期', formatter: () => detail.value.longTerm ? '长期有效' : `${formatDate(detail.value.effectiveTime)} ~ ${formatDate(detail.value.expireTime)}`, className: 'mt2' },
  ...(detail.value.inactiveReason ? [
    { label: '状态说明', formatter: () => inactiveReasons[detail.value.inactiveReason] || detail.value.inactiveReason, className: 'mt2' }
  ] : []),
  ...(detail.value.remark ? [
    { label: '备注', key: 'remark', span: 24, className: 'mt2' }
  ] : [])
])
const fieldTableStore = reactive({
  config: {
    fullContent: false,
    autoPagination: true,
    actions: { table: { search: false, refresh: true } },
    table: { stripe: true, rowKey: 'fieldId' }
  },
  columns: [
    { label: '编号', prop: 'number', minWidth: 180 },
    { label: '字段名称', prop: 'fieldName', minWidth: 180, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '字段编码', prop: 'fieldCode', minWidth: 180, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '允许查看', prop: 'view', slot: 'view', minWidth: 180, align: 'center' }
  ],
  func: async () => ({ data: { rows: fieldRows.value, total: fieldRows.value.length } })
})

function returnToList() {
  router.push('/authorization/serviceAuthorization')
}

async function loadDetail() {
  const version = ++requestVersion
  const id = route.query.id
  detail.value = {}
  service.value = {}
  modelFields.value = []
  loadError.value = ''
  loading.value = false
  if (!id || Array.isArray(id)) {
    loadError.value = '缺少有效的授权编号'
    return
  }
  loading.value = true
  try {
    const response = await getServiceAuthorization(id)
    if (version !== requestVersion) return
    const record = response?.data ?? response
    if (!record?.id) {
      loadError.value = '服务授权不存在或已删除'
      return
    }
    const fallbackService = SERVICE_AUTHORIZATION_SERVICES.find(item => String(item.id) === String(record.serviceId))
    const fallbackClient = record.subjectType === 'CLIENT'
      ? SERVICE_AUTHORIZATION_CLIENTS.find(item => String(item.id) === String(record.subjectId))
      : undefined
    const resolvedRecord = {
      ...record,
      subjectName: record.subjectName || fallbackClient?.name || record.subjectCode || '-',
      serviceName: record.serviceName || fallbackService?.name || '-',
      serviceCode: record.serviceCode || fallbackService?.code || '-',
      serviceVersion: record.serviceVersion || fallbackService?.version || '-'
    }
    let serviceDetail = {}
    let fields = []
    if (record.serviceId) {
      try {
        // 旧演示授权可能没有关联的真实服务，避免直接查详情触发全局错误提示。
        const pageSize = 100
        let serviceExists = false
        for (let pageNum = 1; ; pageNum++) {
          const servicesResponse = await listService({ pageNum, pageSize })
          if (version !== requestVersion) return
          const servicesData = servicesResponse?.data ?? servicesResponse
          const services = servicesData?.rows || servicesData?.list || (Array.isArray(servicesData) ? servicesData : [])
          serviceExists = services.some(item => String(item.id) === String(record.serviceId))
          if (serviceExists || !services.length || (servicesData?.total != null
            ? pageNum * pageSize >= Number(servicesData.total)
            : services.length < pageSize)) break
        }
        if (serviceExists) {
          const serviceResponse = await getService(record.serviceId)
          serviceDetail = serviceResponse?.data || {}
        }
        if (serviceDetail.modelId) {
          const modelResponse = await getPublishedModelStructure(serviceDetail.modelId)
          fields = modelResponse?.data?.fields || []
        }
      } catch {
        // 服务配置不可用时仍展示授权接口返回的字段信息。
      }
    }
    if (version !== requestVersion) return
    detail.value = resolvedRecord
    service.value = serviceDetail
    modelFields.value = fields
  } catch {
    if (version === requestVersion) loadError.value = '服务授权详情加载失败'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => [route.path, route.query.id], ([path]) => {
  if (path === '/authorization/serviceAuthorization/detail') loadDetail()
  else requestVersion++
}, { immediate: true })
</script>
