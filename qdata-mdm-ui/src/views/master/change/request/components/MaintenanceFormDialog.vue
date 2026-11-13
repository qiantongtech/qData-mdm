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
  <div ref="dialogContainer" >
    <el-dialog v-if="dialogContainer" :model-value="modelValue" :title="dialogTitle" width="1100px"
               append-to-body :append-to="dialogContainer" destroy-on-close draggable @close="close"
    >
      <div v-loading="loading" class="column-form">
        <el-empty v-if="definitionError" :description="definitionError" class="row-full" />
        <el-form v-else ref="formRef" :model="form" :rules="rules" :validate-on-rule-change="false"
                 :disabled="saving || validating" label-width="150px" scroll-to-error @submit.prevent
                 class="column-form row-full"
        >
          <div class="h2-title row-full">基础信息</div>
          <qt-form-item v-if="modelType === 'CATEGORY'" label="上级分类" prop="parentId" :error="serverErrors.parentId">
            <el-tree-select v-model="form.parentId" :data="categoryTreeOptions" :props="categoryTreeProps"
                            value-key="id" placeholder="请选择上级分类" filterable clearable check-strictly
            />
          </qt-form-item>
          <qt-form-item v-for="column in businessColumns" :key="column.prop"
                        :label="column.label" :prop="column.prop"
                        :class="{ 'row-full': isLongText(column) }"
                        :error="serverErrors[column.prop] || fieldLoadErrors[column.prop]"
          >
            <MasterFieldInput v-model="form[column.prop]" :column="column" :disabled="saving || validating"
                              @load-error="fieldLoadErrors[column.prop] = $event"
            />
          </qt-form-item>
          <div v-if="!businessColumns.length && !loading" class="emptyBg row-full">
            <img src="@/assets/common/images/empty-state/empty-nodata.png" alt="" />
            <p>当前模型没有可维护的业务字段</p>
          </div>
        </el-form>

        <div v-if="submodelsReady && hasSubmodels" class="h2-title row-full">子模型数据</div>
        <div v-if="definition && modelValue" v-show="submodelsReady && hasSubmodels" class="row-full">
          <SubmodelMaintenanceSection ref="submodelSectionRef" :model-value="modelValue"
                                      :instance-id="instanceId" :mode="mode" :record-id="effectiveRecordId"
                                      :form-config="definition" :disabled="saving || validating"
                                      :initial-changes="[]"
                                      @ready="handleSubmodelReady"
                                      @update:changes="updateSubmodelChanges"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="close">取 消</el-button>
          <el-button type="primary" :loading="saving || validating"
                     :disabled="loading || !businessColumns.length" @click="save"
          >提交审批</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import { getMasterRecordDetail } from '@/api/master/data/data'
import { checkPermi } from '@/utils/permission'
import { getMaintenanceFormConfig, getMasterChangeRequestDetail, submitMasterChangeMaintenance, validateMasterChangeMaintenance } from '@/api/master/change/request'
import SubmodelMaintenanceSection from '../../../data/components/SubmodelMaintenanceSection.vue'
import MasterFieldInput from '../../../data/components/MasterFieldInput.vue'

function getFieldDataType(column) {
  return String(column.dataType || '').trim().toUpperCase().replace(/\(.*\)$/, '')
}

function isAutomaticColumn(column) {
  return String(column.encodingMode || '').toUpperCase() === 'AUTOMATIC'
}

function getFieldControl(column) {
  const category = String(column.columnCategory || column.category || '').toUpperCase()
  if (category === 'SYSTEM' || column.businessField === false) return 'readonly'
  if (category === 'CLASS') return 'tree'
  if (category === 'RELATION') return 'relation'
  if (isAutomaticColumn(column)) return 'automatic'
  if (column.dictTypeId != null && column.dictTypeId !== '') return 'dictionary'
  if (isBooleanColumn(column)) return 'boolean'
  if (isDateColumn(column)) return 'date'
  if (isDateTimeColumn(column)) return 'datetime'
  if (/TEXT|CLOB/.test(getFieldDataType(column))) return 'textarea'
  if (isIntegerColumn(column)) return 'integer'
  if (isDecimalColumn(column)) return 'decimal'
  return 'text'
}

function isLongText(column) { return getFieldControl(column) === 'textarea' }

function isBooleanColumn(column) { return /^(BOOL|BOOLEAN)$/.test(getFieldDataType(column)) }

function isDateColumn(column) { return getFieldDataType(column) === 'DATE' }

function isDateTimeColumn(column) { return /^(DATETIME|TIMESTAMP)$/.test(getFieldDataType(column)) }

function isIntegerColumn(column) { return /^(BIGINT|INTEGER|SMALLINT|INT|TINYINT)$/.test(getFieldDataType(column)) }

function isDecimalColumn(column) { return /^(DECIMAL|NUMBER|NUMERIC|FLOAT|DOUBLE|REAL)$/.test(getFieldDataType(column)) }

function normalizeFieldFormValue(column, value) {
  const text = String(value ?? '')
  if (['dictionary', 'relation', 'tree'].includes(getFieldControl(column))) return text
  if (isBooleanColumn(column)) {
    if (value === true || text === '1' || text.toLowerCase() === 'true') return 'true'
    if (value === false || text === '0' || text.toLowerCase() === 'false') return 'false'
  }
  if (isDateColumn(column) && /^\d{4}-\d{2}-\d{2}(?:$|[T ])/.test(text)) return text.slice(0, 10)
  if (isDateTimeColumn(column) && /^\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}/.test(text)) {
    const dateTime = text.replace('T', ' ').slice(0, 19)
    return dateTime.length === 16 ? `${dateTime}:00` : dateTime
  }
  return text
}

// 校验日期及时间范围，包含闰年和每月天数。
function isValidFieldDate(text, withTime) {
  const match = text.match(withTime
    ? /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/
    : /^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return false
  const [, year, month, day, hour = 0, minute = 0, second = 0] = match.map(Number)
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
  return year > 0 && month >= 1 && month <= 12 && day >= 1 && day <= days[month - 1] &&
    hour <= 23 && minute <= 59 && second <= 59
}

function getFieldError(column, value) {
  const raw = String(value ?? '')
  const text = raw.trim()
  const control = getFieldControl(column)
  const dataType = getFieldDataType(column)
  if (['readonly', 'automatic'].includes(control)) return ''
  if (!text && (column.required || column.nameField)) return `${column.label}不能为空`
  if (control === 'dictionary' && text && Array.isArray(column.referenceOptions) &&
      !column.referenceOptions.some(option => String(option.value) === raw)) {
    return `${column.label}请选择有效的参考数据`
  }
  if (/^(STRING|VARCHAR2?|N?CHAR|N?VARCHAR2?|TEXT|CLOB)$/.test(dataType) &&
      Number(column.dataLength) > 0 && Array.from(raw).length > Number(column.dataLength)) {
    return `${column.label}不能超过 ${column.dataLength} 字`
  }
  if (!text) return ''
  if (isBooleanColumn(column) && !(control === 'boolean' ? /^(true|false)$/ : /^(true|false|0|1)$/).test(text)) {
    return `${column.label}请选择是或否`
  }
  if (isIntegerColumn(column) && !/^-?\d+$/.test(text)) return `${column.label}须为整数`
  if (isIntegerColumn(column) && (BigInt(text) < BigInt('-9223372036854775808') || BigInt(text) > BigInt('9223372036854775807'))) {
    return `${column.label}超出整数范围`
  }
  if (isDecimalColumn(column) && !/^-?\d+(\.\d+)?$/.test(text)) return `${column.label}须为数字`
  if (/^(DECIMAL|NUMBER|NUMERIC)$/.test(dataType)) {
    const [integer, fraction = ''] = text.replace(/^-/, '').split('.')
    const integerDigits = integer.replace(/^0+/, '').length
    const precision = Number(column.dataLength)
    const hasScale = column.dataScale != null && column.dataScale !== ''
    const scale = column.dataScale == null || column.dataScale === '' ? 0 : Number(column.dataScale)
    if (((hasScale || precision > 0) && fraction.length > scale) ||
        (precision > 0 && (integerDigits + fraction.length > precision ||
          integerDigits > precision - scale))) {
      return `${column.label}超过模型配置的数字精度`
    }
  }
  if ((isDateColumn(column) || isDateTimeColumn(column)) && !isValidFieldDate(text, isDateTimeColumn(column))) {
    return `${column.label}的日期格式不正确`
  }
  return ''
}

function getFieldRules(column) {
  return [{
    required: !['readonly', 'automatic'].includes(getFieldControl(column)) && Boolean(column.required || column.nameField),
    validator: (_rule, value, callback) => {
      const error = getFieldError(column, value)
      callback(error ? new Error(error) : undefined)
    },
    trigger: ['blur', 'change']
  }]
}

function serializeFieldValues(columns, values) {
  return Object.fromEntries(columns.filter(column => getFieldControl(column) !== 'readonly' &&
    !(isAutomaticColumn(column) && (values[column.prop] == null || values[column.prop] === '')))
    .map(column => [column.prop, values[column.prop] == null ? '' : String(values[column.prop])]))
}

// 仅提交修改过的字段，将清空操作转换为 null。
function serializeFieldChanges(columns, values, original) {
  const current = serializeFieldValues(columns, values)
  const before = serializeFieldValues(columns, original)
  return Object.fromEntries(columns.filter(column => Object.hasOwn(current, column.prop) &&
    normalizeFieldFormValue(column, current[column.prop]) !== normalizeFieldFormValue(column, before[column.prop]))
    .map(column => [column.prop, current[column.prop] === '' ? null : current[column.prop]]))
}

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], required: true },
  modelId: { type: [String, Number], required: true },
  modelVersionId: { type: [String, Number], required: true },
  mode: { type: String, default: 'create' },
  recordId: { type: [String, Number], default: '' },
  modelType: { type: String, default: 'NORMAL' },
  parentId: { type: [String, Number], default: '' },
  categoryTree: { type: Array, default: () => [] },
  initialValues: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['update:modelValue', 'saved'])
const { proxy } = getCurrentInstance()
const dialogContainer = ref(null)
const formRef = ref(null)
const submodelSectionRef = ref(null)
const form = reactive({})
const sourceRow = ref({})
const loading = ref(false)
const checked = ref(false)
const validating = ref(false)
const saving = ref(false)
const serverErrors = ref({})
const fieldLoadErrors = ref({})
const effectiveRecordId = ref('')
const submodelChanges = ref([])
const hasSubmodels = ref(true)
const submodelsReady = ref(false)
const definition = ref(null)
const definitionError = ref('')
let submissionSnapshot = null
let requestSerial = 0
let validationSerial = 0

const dialogTitle = computed(() => {
  const subject = props.modelType === 'CATEGORY' ? '分类' : '主数据'
  const action = { update: '修改', copy: '新增', create: '新增' }[props.mode] || ''
  return `${action}${subject}申请`
})
const businessColumns = computed(() => {
  const columns = (definition.value?.columns || []).filter(column =>
    column.businessField === true && column.prop && column.prop !== 'ID')
  const orderedColumns = [
    ...columns.filter(column => column.label === '所属产品分类'),
    ...columns.filter(column => column.label !== '所属产品分类')
  ]
  for (const [name, code] of [['客户名称', '客户编码'], ['产品名称', '产品编码']]) {
    const nameIndex = orderedColumns.findIndex(column => column.label === name)
    const codeIndex = orderedColumns.findIndex(column => column.label === code)
    if (nameIndex !== -1 && codeIndex !== -1 && nameIndex > codeIndex) {
      ;[orderedColumns[nameIndex], orderedColumns[codeIndex]] = [orderedColumns[codeIndex], orderedColumns[nameIndex]]
    }
  }
  return orderedColumns
})
const categoryTreeProps = { value: 'id', label: 'label', children: 'children', disabled: 'disabled' }
const forbiddenParentIds = computed(() => {
  if (props.mode !== 'update' || !effectiveRecordId.value) return new Set()
  const find = nodes => {
    for (const node of nodes || []) {
      if (String(node.id) === effectiveRecordId.value) return node
      const found = find(node.children)
      if (found) return found
    }
    return null
  }
  const ids = new Set()
  const collect = node => {
    if (!node) return
    ids.add(String(node.id))
    for (const child of node.children || []) collect(child)
  }
  collect(find(props.categoryTree))
  return ids
})
const categoryTreeOptions = computed(() => {
  const mapNodes = nodes => (nodes || []).map(node => ({
    ...node,
    id: String(node.id),
    label: node.label || node.name || '未命名类目',
    disabled: forbiddenParentIds.value.has(String(node.id)),
    children: mapNodes(node.children)
  }))
  return mapNodes(props.categoryTree)
})
const categoryIds = computed(() => {
  const ids = new Set()
  const collect = nodes => (nodes || []).forEach(node => {
    ids.add(String(node.id))
    collect(node.children)
  })
  collect(props.categoryTree)
  return ids
})
const rules = computed(() => ({
  ...Object.fromEntries(businessColumns.value.map(column => [column.prop, getFieldRules(column)])),
  parentId: [{ validator: (_rule, value, callback) => {
    if (!value) return callback()
    const id = String(value)
    if (forbiddenParentIds.value.has(id)) return callback(new Error('不能选择当前分类或其后代作为父级'))
    if (!categoryIds.value.has(id)) return callback(new Error('上级分类不存在'))
    callback()
  }, trigger: 'change' }]
}))

async function checkForm() {
  let serial = null
  checked.value = false
  serverErrors.value = {}
  if (!businessColumns.value.length) return
  validating.value = true
  try {
    await formRef.value.validate()
    if (hasSubmodels.value && !await submodelSectionRef.value?.validate()) {
      return
    }
    // 子模型校验会整理并同步变更；完成后再标记本次服务端校验，避免把响应误判为过期请求。
    serial = ++validationSerial
    validating.value = true
    const values = props.mode === 'update'
      ? serializeFieldChanges(businessColumns.value, form, sourceRow.value)
      : serializeFieldValues(businessColumns.value, form)
    const changes = submodelSectionRef.value?.serializeChanges() || submodelChanges.value
    const snapshot = payload(changes, values)
    const response = await validateMasterChangeMaintenance(props.instanceId, snapshot)
    if (serial !== validationSerial) return
    serverErrors.value = response?.data?.errors || {}
    checked.value = response?.data?.valid === true
    submissionSnapshot = checked.value ? snapshot : null
    if (!checked.value) {
      proxy.$modal.msgError(Object.values(serverErrors.value)[0] || '数据校验未通过，请检查填写内容。')
    }
  } catch {
    // el-form 与请求拦截器分别显示本地校验和接口错误。
  } finally {
    if (serial === null || serial === validationSerial) validating.value = false
  }
}

function payload(changes, values) {
  return {
    mode: props.mode === 'update' ? 'UPDATE' : 'CREATE',
    recordId: props.mode === 'update' ? effectiveRecordId.value : null,
    parentId: props.modelType === 'CATEGORY' ? form.parentId : null,
    values,
    submodelChanges: changes
  }
}

function updateSubmodelChanges(changes) {
  submodelChanges.value = changes
  validationSerial++
  validating.value = false
  checked.value = false
}

function handleSubmodelReady(value) {
  hasSubmodels.value = value
  submodelsReady.value = true
}

async function save() {
  if (!businessColumns.value.length || loading.value || saving.value || validating.value) return
  const serial = requestSerial
  saving.value = true
  try {
    await checkForm()
    if (!checked.value || !submissionSnapshot) return
    const response = await submitMasterChangeMaintenance(props.instanceId, submissionSnapshot)
    // 正式数据由后台异步写入，执行结束后再触发已有的列表刷新回调。
    const requestId = response?.data?.requestId
    for (let attempt = 0; requestId && checkPermi(['master:change:query'])
        && serial === requestSerial && attempt < 20; attempt++) {
      let result
      try { result = await getMasterChangeRequestDetail(requestId) } catch { break }
      if (['2', '3', '4'].includes(String(result?.data?.executeStatus))) break
      await new Promise(resolve => setTimeout(resolve, 500))
    }
    proxy.$modal.msgSuccess('提交成功')
    emit('saved')
    if (serial === requestSerial) close()
  } catch {
    // 请求拦截器展示服务端错误，保留当前输入供重试。
  } finally {
    saving.value = false
  }
}

function close() {
  requestSerial++
  validationSerial++
  validating.value = false
  emit('update:modelValue', false)
}

async function initializeForm() {
  const serial = ++requestSerial
  loading.value = true
  definition.value = null
  definitionError.value = ''
  submissionSnapshot = null
  hasSubmodels.value = true
  submodelsReady.value = false
  checked.value = false
  serverErrors.value = {}
  sourceRow.value = {}
  fieldLoadErrors.value = {}
  submodelChanges.value = []
  effectiveRecordId.value = props.recordId ? String(props.recordId) : ''
  Object.keys(form).forEach(key => delete form[key])
  form.parentId = props.parentId ? String(props.parentId) : ''
  try {
    const response = await getMaintenanceFormConfig(props.instanceId, {
      modelId: props.modelId, modelVersionId: props.modelVersionId
    })
    if (serial !== requestSerial) return
    const loaded = response?.data
    if (!loaded || String(loaded.instanceId) !== String(props.instanceId) ||
        String(loaded.modelId) !== String(props.modelId) ||
        String(loaded.modelVersionId) !== String(props.modelVersionId)) {
      const message = '模型发布版本已变化，请刷新页面后重新打开表单'
      proxy.$modal.msgError(message)
      throw new Error(message)
    }
    definition.value = loaded
    if (props.mode !== 'create' && props.recordId) {
      const response = await getMasterRecordDetail(props.instanceId, effectiveRecordId.value, props.modelId)
      if (serial !== requestSerial) return
      sourceRow.value = response?.data?.row || {}
      if (props.modelType === 'CATEGORY') {
        const categoryId = String(sourceRow.value.CATEGORY_ID ?? '')
        form.parentId = categoryId === '0' ? '' : categoryId
      }
    }
    if (serial !== requestSerial) return
    businessColumns.value.forEach(column => {
      const copiedValue = props.mode === 'copy' && (column.unique || isAutomaticColumn(column))
        ? '' : sourceRow.value[column.prop]
      form[column.prop] = normalizeFieldFormValue(column, copiedValue ??
        (props.mode === 'create' && !isAutomaticColumn(column) ? props.initialValues[column.prop] ?? column.defaultValue ?? '' : ''))
    })
    await nextTick()
    formRef.value?.clearValidate()
  } catch (error) {
    if (serial === requestSerial) {
      definition.value = null
      definitionError.value = error?.msg || error?.message || '新增表单配置加载失败，请重新打开'
    }
  } finally {
    if (serial === requestSerial) loading.value = false
  }
}
watch(() => props.modelValue, visible => {
  if (visible) initializeForm()
}, { immediate: true })
watch(form, () => {
  validationSerial++
  validating.value = false
  checked.value = false
  serverErrors.value = {}
}, { deep: true })
</script>
