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
  <section v-if="models.length" v-loading="loading">
    <div class="submodel-editor-table">
      <div v-if="activeModel" class="justify-between mb15">
        <el-button class="submodel-add-button" plain type="primary" icon="Plus"
                   :disabled="disabled || !activeAvailable"
                   @click="openCreate"
        >新增{{ activeModel.name || '子数据' }}
        </el-button>
      </div>

      <el-tabs v-model="activeId" class="demo-tabs" @tab-change="changeModel">
        <qt-tab-pane v-for="model in models" :key="model.id" :name="String(model.id)"
                     :label="model.name" :disabled="disabled || model.available === false"
        />
      </el-tabs>

      <el-empty v-if="activeMessage" :description="activeMessage" />
      <qt-wrap v-else :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef" class="mt10">
        <qt-table ref="tableRef" v-bind="tableStore">
          <template #recordId="{ row }">{{ row.ID || '保存后生成' }}</template>
          <template #action="{ row }">
            <el-button v-if="row._state !== 'delete'" link type="primary" icon="Edit" :disabled="disabled" @click="openEdit(row)">修改</el-button>
            <el-button v-if="row._state !== 'delete'" link type="danger" icon="Delete" :disabled="disabled" @click="removeRow(row)">删除</el-button>
            <el-button v-else link type="primary" :disabled="disabled" @click="restoreRow(row)">撤销</el-button>
          </template>
        </qt-table>
      </qt-wrap>
    </div>

    <div ref="editorDialogContainer" class="app-container" v-show="editorVisible">
    <el-dialog v-if="editorDialogContainer" v-model="editorVisible" :title="editorTitle" width="800px"
               append-to-body :append-to="editorDialogContainer" class="dialog" modal-class="submodel-editor-modal"
               align-center
               destroy-on-close draggable
    >
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" :validate-on-rule-change="false"
               :disabled="disabled" label-width="120px"
               class="column-form" scroll-to-error @submit.prevent
      >
        <qt-form-item v-for="column in currentBusinessColumns" :key="column.prop"
                      :label="column.label" :prop="column.prop" :error="editorLoadErrors[column.prop]"
                      class="row-full"
        >
          <MasterFieldInput v-model="editorForm[column.prop]" :column="column" :disabled="disabled"
                            @load-error="editorLoadErrors[column.prop] = $event"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editorVisible = false">取 消</el-button>
          <el-button type="primary" :disabled="disabled" @click="confirmEditor">确 定</el-button>
        </div>
      </template>
    </el-dialog>
    </div>
  </section>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, reactive, ref, watch, watchEffect } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getMasterSubmodelDefinitions, getMasterSubmodels } from '@/api/master/data/data'
import MasterFieldInput from './MasterFieldInput.vue'

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

// 按子表顺序合并同次发布的维护字段配置。
function mergeMaintenanceColumns(columns, configuredColumns) {
  const configured = new Map(configuredColumns.map(column => [column.prop.toUpperCase(), column]))
  return columns.map(column => {
    const config = configured.get(column.prop.toUpperCase())
    if (!config) {
      throw new Error(`字段 ${column.label || column.prop} 与成功发布配置不一致，请重新打开页面`)
    }
    return { ...column, ...config, prop: column.prop }
  })
}

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], required: true },
  mode: { type: String, default: 'create' },
  recordId: { type: [String, Number], default: '' },
  initialChanges: { type: Array, default: () => [] },
  formConfig: { type: Object, required: true },
  disabled: Boolean
})
const emit = defineEmits(['update:changes', 'ready'])
const { proxy } = getCurrentInstance()
const loading = ref(false)
const models = ref([])
const activeId = ref('')
const stores = reactive({})
const editorDialogContainer = ref(null)
const editorVisible = ref(false)
const editorMode = ref('create')
const editorForm = reactive({})
const editorFormRef = ref(null)
const editorLoadErrors = ref({})
const tableRef = ref(null)
const editingKey = ref('')
let localSerial = 0
let rowSerial = 0

const activeModel = computed(() => models.value.find(item => String(item.id) === activeId.value))
const activeStore = computed(() => stores[activeId.value] || emptyStore())
const withoutPagination = computed(() => ['create', 'update'].includes(props.mode))
const currentRows = computed(() => {
  const store = activeStore.value
  if (store.localOnly) {
    const visible = (store.rows || []).filter(row => row._state !== 'delete')
    if (withoutPagination.value) return visible
    const start = (store.pageNum - 1) * store.pageSize
    return visible.slice(start, start + store.pageSize)
  }
  if (withoutPagination.value) return store.rows || []
  const indexed = Object.fromEntries((store.rows || []).map(row => [row._key, row]))
  const pageRows = (store.currentKeys || []).map(key => indexed[key]).filter(Boolean)
  if (store.pageNum !== 1) return pageRows
  return [...(store.rows || []).filter(row => row._manualCreate), ...pageRows]
})
const maintenanceDefinitions = computed(() => new Map(Object.values(stores).map(store => {
  const columns = (store.columns || []).filter(item => item.businessField)
  try {
    if (!columns.length) return [store.modelId, { columns, error: '' }]
    const model = props.formConfig.submodels.find(item => String(item.id) === store.modelId)
    if (!model) throw new Error('子模型与成功发布配置不一致，请重新打开页面')
    return [store.modelId, { columns: mergeMaintenanceColumns(columns, model.columns), error: '' }]
  } catch (error) {
    return [store.modelId, { columns: [], error: error.message }]
  }
})))
const configurationError = computed(() => [...maintenanceDefinitions.value.values()].find(item => item.error)?.error || '')
const currentBusinessColumns = computed(() => businessColumnsFor(activeStore.value))
const activeMessage = computed(() => maintenanceDefinitions.value.get(activeId.value)?.error || activeModel.value?.message || '')
const activeAvailable = computed(() => activeModel.value?.available !== false && !activeMessage.value)
const editorTitle = computed(() => `${editorMode.value === 'create' ? '新增' : '修改'}${activeModel.value?.name || '子模型数据'}`)
const editorRules = computed(() => Object.fromEntries(currentBusinessColumns.value.map(column => [column.prop, getFieldRules(column)])))
const serializedChanges = computed(() => serializeChanges())
const tableStore = reactive({
  config: {
    fullContent: false,
    notPagination: withoutPagination.value,
    actions: { show: false },
    table: {
      class: 'field-design-table',
      stripe: true,
      height: 400,
      rowStyle: { height: '48px' },
      scrollbarAlwaysOn: false,
      rowKey: '_key',
      style: {}
    }
  },
  columns: [],
  func: async params => {
    const store = ensureStore(activeId.value)
    if (!withoutPagination.value) {
      store.pageNum = Number(params.pageNum || 1)
      store.pageSize = Number(params.pageSize || 10)
      if (!store.localOnly && store.loaded) await loadPage(store)
    }
    const rows = currentRows.value
    return { data: { rows, total: store.localOnly ? rows.length : modelCount(activeModel.value) } }
  },
  params: {}
})

function emptyStore(modelId = '') {
  return { columns: [], rows: [], currentKeys: [], pendingChanges: {}, pageNum: 1, pageSize: 10,
    total: 0, loaded: false, allLoaded: false, initialApplied: false, localOnly: false, excludedCopyIds: {},
    modelId: String(modelId), loadSerial: 0 }
}

function ensureStore(id) {
  const key = String(id)
  if (!stores[key]) stores[key] = emptyStore(key)
  return stores[key]
}

function businessValues(row, columns = currentBusinessColumns.value) {
  return serializeFieldValues(columns, row)
}

function businessColumnsFor(store) {
  return maintenanceDefinitions.value.get(store.modelId)?.columns || []
}

watchEffect(() => {
  tableStore.config.table.style = currentRows.value.length
    ? {}
    : { '--el-table-fixed-right-column': 'none' }
  tableStore.columns = [
    ...(props.mode === 'create' ? [] : [
      { label: '编号', prop: 'ID', slot: 'recordId', width: 60, align: 'center', showOverflowTooltip: { effect: 'light' } }
    ]),
    ...currentBusinessColumns.value.map(column => ({
      label: column.label,
      prop: column.prop,
      minWidth: 190,
      align: 'left',
      date: /TIME/i.test(column.dataType || '') ? '{y}-{m}-{d} {h}:{i}' : false,
      showOverflowTooltip: { effect: 'light' }
    })),
    { label: '操作', prop: '_key', slot: 'action', width: 160, fixed: 'right' }
  ]
})

function refreshTable() {
  nextTick(() => tableRef.value?.getList())
}

function normalizeRow(row, state = 'original', extra = {}, columns = currentBusinessColumns.value) {
  const values = { ...row }
  const key = values._key || `sub-row-${++rowSerial}`
  return { ...values, ...extra, _key: key, _state: state,
    _original: state === 'original' ? businessValues(values, columns) : null }
}

async function loadDefinitions(serial) {
  const response = await getMasterSubmodelDefinitions(props.instanceId)
  if (serial !== localSerial) return
  const data = response?.data || {}
  models.value = data.submodels || []
  emit('ready', models.value.length > 0)
  activeId.value = data.selectedSubmodelId ? String(data.selectedSubmodelId) : String(models.value[0]?.id || '')
  if (!activeId.value) return
  ensureStore(activeId.value).columns = data.columns || []
  await loadActive(serial, true)
  refreshTable()
}

async function loadActive(serial = localSerial, resetPage = false) {
  if (!activeId.value) return
  const store = ensureStore(activeId.value)
  if (resetPage) store.pageNum = 1
  if (props.mode === 'create' || !props.recordId) {
    if (!store.columns.length) {
      const response = await getMasterSubmodelDefinitions(props.instanceId, { submodelId: store.modelId })
      if (serial !== localSerial) return
      store.columns = response?.data?.columns || []
    }
    store.loaded = true
    store.localOnly = true
    applyInitialChanges(store.modelId)
    store.total = store.rows.filter(row => row._state !== 'delete').length
    return
  }
  if (!store.columns.length) {
    const definitionResponse = await getMasterSubmodelDefinitions(props.instanceId, { submodelId: store.modelId })
    if (serial !== localSerial) return
    store.columns = definitionResponse?.data?.columns || []
  }
  applyInitialChanges(store.modelId)
  const savedCopyChanges = props.mode === 'copy' && props.initialChanges.some(change =>
    String(change.submodelId) === store.modelId && change.operation === 'CREATE')
  if (savedCopyChanges) {
    store.localOnly = true
    store.total = store.rows.filter(row => row._state !== 'delete').length
    store.loaded = true
    return
  }
  if (props.mode === 'update') await loadAllUpdateRows(store, serial)
  else await loadPage(store, serial)
}

async function loadAllUpdateRows(store, serial) {
  if (store.allLoaded) return
  store.pageSize = 100
  store.pageNum = 1
  await loadPage(store, serial)
  if (serial !== localSerial) return
  const pages = Math.ceil(store.total / store.pageSize)
  for (let page = 2; page <= pages; page++) {
    store.pageNum = page
    await loadPage(store, serial)
    if (serial !== localSerial) return
  }
  store.pageNum = 1
  store.allLoaded = true
}

async function loadPage(store, serial = localSerial) {
  const pageSerial = ++store.loadSerial
  const response = await getMasterSubmodels(props.instanceId, props.recordId,
    { submodelId: store.modelId, pageNum: store.pageNum, pageSize: store.pageSize })
  if (serial !== localSerial || pageSerial !== store.loadSerial) return
  const data = response?.data || {}
  store.columns = data.columns || []
  const businessColumns = businessColumnsFor(store)
  store.total = Number(data.total || 0)
  applyInitialChanges(store.modelId)
  const keys = []
  for (const sourceRow of data.rows || []) {
    if (props.mode === 'copy') {
      const sourceId = String(sourceRow.ID)
      if (store.excludedCopyIds[sourceId]) continue
      const existing = store.rows.find(item => item._sourceRecordId === sourceId)
      if (existing) {
        keys.push(existing._key)
        continue
      }
      const copied = businessValues(sourceRow, businessColumns)
      businessColumns.filter(item => item.unique || isAutomaticColumn(item)).forEach(column => { copied[column.prop] = '' })
      const normalized = normalizeRow(copied, 'create',
        { _sourceRecordId: sourceId, _manualCreate: false }, businessColumns)
      store.rows.push(normalized)
      keys.push(normalized._key)
      continue
    }
    const recordId = String(sourceRow.ID)
    let row = store.rows.find(item => String(item.ID) === recordId)
    if (!row) {
      row = normalizeRow(sourceRow, 'original', {}, businessColumns)
      const pending = store.pendingChanges[recordId]
      if (pending) {
        if (pending.operation === 'DELETE') row._state = 'delete'
        if (pending.operation === 'UPDATE') Object.assign(row, pending.values || {}, { _state: 'update' })
        delete store.pendingChanges[recordId]
      }
      store.rows.push(row)
    }
    keys.push(row._key)
  }
  store.currentKeys = keys
  store.loaded = true
}

function applyInitialChanges(modelId) {
  const store = ensureStore(modelId)
  if (store.initialApplied) return
  store.initialApplied = true
  const changes = props.initialChanges.filter(item => String(item.submodelId) === String(modelId))
  for (const change of changes) {
    if (change.operation === 'CREATE') {
      const columns = businessColumnsFor(store)
      store.rows.push(normalizeRow(change.values || {}, 'create', { _manualCreate: true }, columns))
    } else store.pendingChanges[String(change.recordId)] = { ...change }
  }
}

async function changeModel() {
  loading.value = true
  try { await loadActive() } finally {
    loading.value = false
    refreshTable()
  }
}

function openCreate() {
  editorMode.value = 'create'
  editingKey.value = ''
  resetEditor(Object.fromEntries(currentBusinessColumns.value.map(column =>
    [column.prop, normalizeFieldFormValue(column, isAutomaticColumn(column) ? '' : column.defaultValue ?? '')])))
}

function openEdit(row) {
  editorMode.value = 'update'
  editingKey.value = row._key
  resetEditor(businessValues(row))
}

function resetEditor(values) {
  editorLoadErrors.value = {}
  Object.keys(editorForm).forEach(key => delete editorForm[key])
  currentBusinessColumns.value.forEach(column => {
    editorForm[column.prop] = normalizeFieldFormValue(column, values[column.prop])
  })
  editorVisible.value = true
  nextTick(() => editorFormRef.value?.clearValidate())
}

async function confirmEditor() {
  try { await editorFormRef.value.validate() } catch { return }
  const store = ensureStore(activeId.value)
  if (editorMode.value === 'create') {
    store.rows.unshift(normalizeRow(businessValues(editorForm), 'create', { _manualCreate: true }))
    store.pageNum = 1
    if (store.localOnly) store.total = store.rows.filter(row => row._state !== 'delete').length
  } else {
    const row = store.rows.find(item => item._key === editingKey.value)
    if (row) {
      Object.assign(row, businessValues(editorForm))
      if (row._state !== 'create') {
        row._state = Object.keys(serializeFieldChanges(currentBusinessColumns.value, row, row._original)).length
          ? 'update' : 'original'
      }
    }
  }
  editorVisible.value = false
  refreshTable()
}

async function removeRow(row) {
  try {
    await ElMessageBox.confirm('确定删除这条子模型数据吗？',
      '删除子数据', { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' })
  } catch { return }
  const store = ensureStore(activeId.value)
  if (row._state === 'create') {
    if (row._sourceRecordId) store.excludedCopyIds[row._sourceRecordId] = true
    store.rows.splice(store.rows.indexOf(row), 1)
    if (store.localOnly) store.total = store.rows.filter(item => item._state !== 'delete').length
  }
  else row._state = 'delete'
  refreshTable()
}

function restoreRow(row) {
  row._state = Object.keys(serializeFieldChanges(currentBusinessColumns.value, row, row._original)).length
    ? 'update' : 'original'
  refreshTable()
}

function serializeChanges() {
  const result = []
  for (const model of models.value) {
    const store = stores[String(model.id)]
    if (!store) continue
    const columns = businessColumnsFor(store)
    for (const row of store.rows || []) {
      if (row._state === 'original') continue
      result.push({
        submodelId: String(model.id),
        submodelName: model.name,
        operation: row._state.toUpperCase(),
        recordId: row._state === 'create' ? null : String(row.ID),
        values: row._state === 'delete' ? {} : row._state === 'update'
          ? serializeFieldChanges(columns, row, row._original) : businessValues(row, columns)
      })
    }
    for (const change of Object.values(store.pendingChanges || {})) {
      result.push({
        submodelId: String(model.id), submodelName: model.name,
        operation: change.operation, recordId: String(change.recordId),
        values: change.operation === 'DELETE' ? {} : { ...(change.values || {}) }
      })
    }
  }
  return result
}

async function validate() {
  await prepareAll()
  for (const model of models.value) {
    const store = stores[String(model.id)]
    if (!store?.loaded) continue
    const columns = businessColumnsFor(store)
    const rows = (store.rows || []).filter(row => row._state !== 'delete')
    for (const [index, row] of rows.entries()) {
      if (row._state === 'original') continue
      for (const column of columns) {
        const error = getFieldError(column, row[column.prop])
        if (error) {
          activeId.value = String(model.id)
          proxy.$modal.msgWarning(`${model.name}第 ${index + 1} 行：${error}`)
          refreshTable()
          openEdit(row)
          await nextTick()
          editorFormRef.value?.validateField(column.prop).catch(() => {})
          return false
        }
      }
    }
  }
  return true
}

async function prepareAll() {
  const originalId = activeId.value
  loading.value = true
  try {
    for (const model of models.value) {
      activeId.value = String(model.id)
      await loadActive()
      if (props.mode === 'copy') await loadAllCopyRows(ensureStore(model.id))
    }
  } finally {
    activeId.value = originalId
    loading.value = false
  }
  if (configurationError.value) throw new Error(configurationError.value)
  const changes = serializeChanges()
  if (changes.length > 500) {
    proxy.$modal.msgWarning('本次子模型变更超过 500 条，请分批维护')
    throw new Error('子模型变更数量超限')
  }
  return changes
}

async function loadAllCopyRows(store) {
  if (store.localOnly) return
  if (store.total > 500) {
    proxy.$modal.msgWarning('复制的子模型记录超过 500 条，请缩小数据范围后再操作')
    throw new Error('子模型复制数量超限')
  }
  const originalPage = store.pageNum
  const originalSize = store.pageSize
  store.pageSize = 100
  const pages = Math.max(1, Math.ceil(store.total / store.pageSize))
  for (let page = 1; page <= pages; page++) {
    store.pageNum = page
    await loadPage(store)
  }
  store.pageSize = originalSize
  store.pageNum = originalPage
  await loadPage(store)
}

function modelCount(model) {
  const store = stores[String(model.id)]
  if (!store) return model.recordCount ?? 0
  if (store.localOnly) return store.rows.filter(row => row._state !== 'delete').length
  const creates = store.rows.filter(row => row._manualCreate && row._state === 'create').length
  const deletes = store.rows.filter(row => row._state === 'delete').length + Object.values(store.pendingChanges)
    .filter(change => change.operation === 'DELETE').length
  return Math.max(0, Number(store.total ?? model.recordCount ?? 0) + creates - deletes)
}

watch(() => props.modelValue, async visible => {
  if (!visible) return
  const serial = ++localSerial
  models.value = []
  activeId.value = ''
  Object.keys(stores).forEach(key => delete stores[key])
  loading.value = true
  try { await loadDefinitions(serial) } finally { if (serial === localSerial) loading.value = false }
}, { immediate: true })
watch(withoutPagination, value => {
  tableStore.config.notPagination = value
  refreshTable()
})
watch(serializedChanges, changes => emit('update:changes', changes), { deep: true })

defineExpose({ validate, prepareAll, serializeChanges })
</script>

<style scoped lang="scss">
.submodel-add-button {
  height: 28px;
  padding: 8px 11px;
  font-size: 12px;
}

.submodel-editor-table :deep(.justify-between.mb15) {
  margin-bottom: 15px;
  padding: 0;
}

.submodel-editor-table :deep(.qt-wrap.mt10) {
  // 页签头部已提供 15px 下间距，表格不再叠加额外的上间距。
  margin-top: 0 !important;
}

:global(.submodel-editor-modal .el-overlay-dialog) {
  align-items: center;
}

:global(.submodel-editor-modal .el-dialog) {
  margin: 0 auto !important;
  transform: translateY(-20%);
}
</style>
