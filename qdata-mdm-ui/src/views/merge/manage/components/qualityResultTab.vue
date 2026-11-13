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
  展示合并结果的质量规则、参与字段及已有检测结果。
  @prop detail 合并管理详情数据。
  @author chentangwen
-->
<template>
  <qt-wrap
    :columns="tableStore.columns"
    :table-ref="{ getList: refreshQualityData }"
    :config="tableWrapConfig"
  >
    <template #actions-data>
      <guide-tip-panel
        type="plain"
        title="查看合并结果适用的质量规则、参与字段及各项检测结果，重点核对未通过项目的原因，为后续调整合并字段提供依据。"
      />
    </template>
    <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" class="quality-result-table">
      <template #fields="{ row }">{{ row.fields || '-' }}</template>
      <template #result="{ row }">
        <el-tag size="small" :type="row.resultType">{{ row.resultLabel }}</el-tag>
      </template>
      <template #description="{ row }">{{ row.description || '-' }}</template>
      <template #empty><span class="text-muted">{{ loadError ? '质量规则加载失败' : '暂无质量规则' }}</span></template>
    </qt-table>
  </qt-wrap>
</template>

<script setup name="MergeQualityResultTab">
import { computed, nextTick, reactive, ref, watch } from 'vue'

import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { getBase } from '@/api/model/base/base'
import { listRulequality } from '@/api/rule/quality/quality'

const props = defineProps({
  detail: { type: Object, default: null },
})

const tableRef = ref()
const loadError = ref(false)
const modelFields = ref([])
const qualityRules = ref([])
let requestVersion = 0

const tableWrapConfig = {
  fullContent: false,
  actions: { table: { search: false, refresh: true, columns: true } },
}

const configuredRows = computed(() => {
  const fields = modelFields.value
  const rules = qualityRules.value.map((rule) => {
    const config = parseRuleConfig(rule.ruleConfig)
    const fieldIds = getRuleFieldIds(rule, config)
    return {
      rowKey: `rule-${rule.id}`,
      id: rule.id,
      ruleName: rule.ruleName || '质量规则',
      ruleType: rule.ruleType,
      fields: fieldIds.map((id) => getFieldName(fields, id)).filter(Boolean).join(' / '),
      fieldIds,
      description: getRuleDescription(rule, config),
      execution: null,
    }
  })

  const configuredRequiredFields = new Set(
    rules.filter((row) => row.ruleType === 'NOT_EMPTY').flatMap((row) => row.fieldIds.map(String)),
  )
  const requiredFields = fields.filter((field) => isEnabledFlag(field.requiredFlag)
    && String(field.columnCategory || '').toUpperCase() !== 'SYSTEM'
    && field.encodingVersionId == null
    && !configuredRequiredFields.has(String(field.id)))
  if (requiredFields.length) {
    rules.unshift({
      rowKey: 'model-required-fields',
      ruleName: '必填字段检查',
      ruleType: 'MODEL_REQUIRED',
      fields: requiredFields.map((field) => field.name || field.columnName || field.code).filter(Boolean).join(' / '),
      fieldIds: requiredFields.map((field) => field.id),
      description: '不能为空',
      execution: null,
    })
  }
  return rules
})

const executionRows = computed(() => {
  const raw = props.detail?.qualityDetail
  if (!raw) return []
  try {
    const result = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(result) ? result : []
  } catch {
    return []
  }
})

const hasQualityResult = computed(() => props.detail?.qualityLogId != null
  || ['2', '4'].includes(String(props.detail?.qualityStatus)))

const tableRows = computed(() => {
  const remaining = [...executionRows.value]
  const rows = configuredRows.value.map((rule) => {
    if (rule.ruleType === 'MODEL_REQUIRED') {
      const failures = remaining.filter((row) => String(row.ruleName || '').startsWith('模型约束:')
        && /必填|不能为空/.test(String(row.message || '')))
      failures.forEach((failure) => remaining.splice(remaining.indexOf(failure), 1))
      const execution = hasQualityResult.value
        ? { executed: true, passed: failures.length === 0, message: failures[0]?.message }
        : null
      return formatResultRow(rule, execution, failures.length ? failures.map((row) => row.ruleName.replace('模型约束:', '')).join(' / ') : rule.fields)
    }
    const executionIndex = remaining.findIndex((row) => row.ruleName === rule.ruleName)
    const execution = executionIndex < 0 ? null : remaining.splice(executionIndex, 1)[0]
    return formatResultRow(rule, execution)
  })

  remaining.forEach((execution, index) => {
    const constraintName = String(execution.ruleName || '').replace(/^模型约束:/, '')
    rows.push(formatResultRow({
      rowKey: `result-${index}`,
      ruleName: String(execution.ruleName || '').startsWith('模型约束:') ? '模型约束检查' : (execution.ruleName || '质量规则'),
      fields: constraintName,
      description: execution.message || '按模型约束检查',
    }, execution))
  })
  return rows
})

const tableStore = reactive({
  func: loadQualityTableRows,
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
  columns: [
    { label: '编码', prop: 'id', width: 100, minWidth: 100, sortable: 'custom', sortableKey: 'id' },
    { label: '规则名称', prop: 'ruleName', minWidth: 190, align: 'left', showOverflowTooltip: true },
    { label: '字段', prop: 'fields', slot: 'fields', minWidth: 340, align: 'left', showOverflowTooltip: true },
    { label: '检测结果', prop: 'resultLabel', slot: 'result', minWidth: 130 },
    { label: '说明', prop: 'description', slot: 'description', minWidth: 260, align: 'left', showOverflowTooltip: true },
  ],
})

/**
 * 对全部质量规则按编码排序，再交由表格分页；无规则 ID 的模型约束排在末尾。
 * @param {Object} params 表格排序参数。
 * @returns {Promise<Object>} 表格数据响应。
 */
async function loadQualityTableRows({ orderByColumn, isAsc } = {}) {
  const rows = [...tableRows.value]
  if (orderByColumn === 'id' && isAsc) {
    const direction = isAsc === 'ascending' ? 1 : -1
    rows.sort((left, right) => {
      if (left.id == null && right.id == null) return 0
      if (left.id == null) return 1
      if (right.id == null) return -1
      return direction * String(left.id).localeCompare(String(right.id), 'en', { numeric: true })
    })
  }
  return { data: rows }
}

/**
 * 将配置规则和执行结果合并为当前质量结果表格行。
 * @param {*} rule 规则定义。
 * @param {*} execution 已有规则执行记录。
 * @param {*} failedFields 已检测失败的字段集合。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatResultRow(rule, execution, failedFields) {
  const executed = hasQualityResult.value && execution != null && execution.executed !== false
  const invalidated = String(props.detail?.qualityStatus) === '5'
  let resultLabel = '未检测'
  let resultType = 'info'
  if (invalidated) {
    resultLabel = '结果已失效'
    resultType = 'warning'
  } else if (executed) {
    resultLabel = execution.passed ? '通过' : '不通过'
    resultType = execution.passed ? 'success' : 'danger'
  } else if (execution && execution.executed === false) {
    resultLabel = '未执行'
  }
  return {
    ...rule,
    fields: failedFields || rule.fields,
    resultLabel,
    resultType,
    description: execution?.message || rule.description,
  }
}

/**
 * 独立加载规则与模型字段；任一接口较慢时，已返回的数据仍先显示。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function refreshQualityData() {
  const version = ++requestVersion
  loadError.value = false
  const modelId = props.detail?.modelId
  qualityRules.value = []
  modelFields.value = []
  await nextTick()
  tableRef.value?.resetQuery()
  if (modelId == null) return

  const refreshTable = async () => {
    await nextTick()
    if (version === requestVersion) tableRef.value?.getList()
  }

  const rulesRequest = listRulequality({ modelId, validFlag: true, abnormalFlag: '0', pageNum: 1, pageSize: 100 })
    .then(async (response) => {
      if (version !== requestVersion) return
      qualityRules.value = getResponseRows(response).filter((rule) => !isAbnormalRule(rule))
      await refreshTable()
    })
    .catch(async () => {
      if (version !== requestVersion) return
      qualityRules.value = []
      loadError.value = true
      await refreshTable()
    })
  const fieldsRequest = getBase(modelId)
    .then(async (response) => {
      if (version !== requestVersion) return
      modelFields.value = (response?.data?.fields || []).filter((field) => String(field.columnCategory || '').toUpperCase() !== 'SYSTEM')
      await refreshTable()
    })
    .catch(async () => {
      if (version !== requestVersion) return
      loadError.value = true
      await refreshTable()
    })
  await Promise.allSettled([rulesRequest, fieldsRequest])
}

/**
 * 兼容质量规则接口分页响应及直接数组响应。
 * @param {*} response 调用方传入的业务配置。
 * @returns {*} 当前业务计算结果。
 */
function getResponseRows(response) {
  const data = response?.data ?? response
  if (Array.isArray(data)) return data
  return data?.rows || data?.list || []
}

/**
 * 解析历史规则配置，兼容单字段、多字段及旧版逗号分隔配置。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {*} 当前业务计算结果。
 */
function parseRuleConfig(value) {
  if (value && typeof value === 'object') return value
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

/**
 * 兼容数组、JSON 和逗号分隔的字段编号。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {Array} 整理后的业务记录集合。
 */
function parseFieldIds(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  if (value === null || value === undefined || value === '') return []
  try {
    const parsed = JSON.parse(value)
    if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean)
  } catch {
    // 兼容历史逗号分隔字段 ID。
  }
  return String(value).split(',').map((item) => item.trim()).filter(Boolean)
}

/**
 * 从质量规则配置中提取参与校验的模型字段 ID。
 * @param {*} rule 规则定义。
 * @param {*} config 规则配置。
 * @returns {Array} 整理后的业务记录集合。
 */
function getRuleFieldIds(rule, config) {
  const configuredIds = parseFieldIds(config.fieldIdList)
  if (configuredIds.length) return configuredIds
  if (rule.ruleType === 'FIELD_LOGIC') {
    return [config.fieldId1 ?? config.modelFieldId1 ?? config.left, config.fieldId2 ?? config.modelFieldId2 ?? config.right]
      .filter((id) => id !== null && id !== undefined && id !== '')
      .map(String)
  }
  if (rule.ruleType === 'UNIQUE') {
    const fieldList = Array.isArray(config.fieldList) ? config.fieldList.map((item) => item?.fieldId ?? item?.modelFieldId ?? item) : []
    return fieldList.length ? fieldList.map(String) : parseFieldIds(rule.modelFieldId)
  }
  return [config.fieldId ?? config.modelFieldId ?? parseFieldIds(rule.modelFieldId)[0]]
    .filter((id) => id !== null && id !== undefined && id !== '')
    .map(String)
}

/**
 * 把规则配置转换成简洁可读的检测说明。
 * @param {*} rule 规则定义。
 * @param {*} config 规则配置。
 * @returns {*} 当前业务计算结果。
 */
function getRuleDescription(rule, config) {
  if (rule.ruleType === 'NOT_EMPTY') return '不能为空'
  if (rule.ruleType === 'FORMAT') {
    const presetDescriptions = { phone: '11位手机号', creditCode: '18位大写字母或数字', email: '邮箱格式' }
    return presetDescriptions[config.preset] || config.regex || rule.description || '按配置格式校验'
  }
  if (rule.ruleType === 'LENGTH') return `长度 ${config.minLen ?? config.min ?? '-'}～${config.maxLen ?? config.max ?? '-'} 位`
  return rule.description || '按规则配置进行校验'
}

/**
 * 根据字段编号读取模型字段名称。
 * @param {*} fields 模型或合并字段列表。
 * @param {*} id 当前记录编号。
 * @returns {string} 用于页面展示的格式化文本。
 */
function getFieldName(fields, id) {
  const field = fields.find((item) => String(item.id) === String(id))
  return field?.name || field?.columnName || field?.code || String(id)
}

/**
 * 兼容布尔、数字和字符串形式的启用标记。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isEnabledFlag(value) {
  return value === true || value === 1 || ['1', 'true', 'y', 'yes'].includes(String(value).toLowerCase())
}

/**
 * 判断质量规则是否被标记为异常。
 * @param {*} rule 规则定义。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isAbnormalRule(rule) {
  return rule?.abnormalFlag === true || String(rule?.abnormalFlag) === '1'
}

watch(() => [props.detail?.id, props.detail?.modelId], refreshQualityData, { immediate: true })
watch(() => [props.detail?.qualityLogId, props.detail?.qualityStatus, props.detail?.qualityDetail], async () => {
  await nextTick()
  tableRef.value?.getList()
})
</script>

<style scoped>
.quality-result-summary {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quality-result-table {
  margin-top: var(--qt-space-2);
}
</style>
