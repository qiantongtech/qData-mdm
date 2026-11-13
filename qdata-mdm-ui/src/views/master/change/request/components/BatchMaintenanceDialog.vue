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
    <el-dialog :model-value="modelValue" :title="title"
             :width="isBatchStatus ? 'min(800px, calc(100% - 24px))' : 'min(1080px, calc(100% - 24px))'"
             :top="isApprovalMode ? '15vh' : '5vh'"
             class="scrollbar"
             destroy-on-close draggable :close-on-click-modal="false" @close="close"
    >
    <div v-if="!isBatchStatus" class="batch-steps mb15">
      <Steps :steps="stepsList" :active-step="step" :is-dialog="true" :clickable="false" />
    </div>

    <el-form v-if="step === 0" ref="batchFormRef" :model="form" :rules="batchRules" :validate-on-rule-change="false" scroll-to-error
             :label-width="isBatchStatus ? '120px' : isApprovalMode ? '128px' : '110px'"
             class="column-form" :disabled="isBatchStatus && (validating || submitting)"
             @submit.prevent
    >

      <template v-if="mode === 'BATCH_UPDATE'">
        <div class="h2-titles row-full">统一修改字段</div>
        <el-alert v-if="definitionError" :title="definitionError" type="error" show-icon
                  :closable="false" class="row-full"
        />
        <div v-if="definitionError" class="row-full">
          <el-button link type="primary" icon="Refresh" @click="loadMaintenanceDefinition">重新加载</el-button>
        </div>
        <qt-form-item label="修改字段" required class="row-full">
          <el-select v-model="checkedFields" multiple filterable clearable collapse-tags
                     collapse-tags-tooltip :max-collapse-tags="7"
                     :loading="definitionLoading" :disabled="updateUnavailable"
                     :placeholder="definitionLoading ? '字段配置加载中' : '请选择需要统一修改的字段'"
          >
            <el-option v-for="column in editableColumns" :key="column.prop"
                       :label="column.label" :value="column.prop"
            />
          </el-select>
          <el-text v-if="maintenanceDefinition && !editableColumns.length" type="info" size="small">
            当前模型没有可批量修改的字段
          </el-text>
        </qt-form-item>
        <qt-form-item v-for="field in checkedFields" :key="field" :label="columnLabel(field)"
                      :prop="`values.${field}`" class="row-full"
        >
          <el-date-picker v-if="isDateField(field)" v-model="form.values[field]" type="date"
                          value-format="YYYY-MM-DD" placeholder="请选择日期" clearable class="number-input"
          />
          <el-date-picker v-else-if="isDateTimeField(field)" v-model="form.values[field]" type="datetime"
                          value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择日期时间" clearable class="number-input"
          />
          <el-input v-else v-model="form.values[field]" clearable
                    :placeholder="fieldPlaceholder(field)"
          />
        </qt-form-item>
      </template>

      <qt-form-item v-else-if="isBatchStatus" label="目标状态操作" prop="action">
        <el-select v-model="form.action" placeholder="请选择状态操作">
          <el-option v-for="item in statusActions" :key="item.value" :label="item.label"
                     :value="item.value" :disabled="item.disabled"
          />
        </el-select>
      </qt-form-item>

      <template v-else>
        <div class="h2-titles row-full">导入文件</div>
        <GuideTipPanel v-if="definitionError" type="hint" allow-wrap class="row-full" role="alert">
          <template #title-append>
            <span class="tip-import-error">{{ definitionError }}</span>
          </template>
        </GuideTipPanel>
        <div v-if="definitionError" class="row-full">
          <el-button link type="primary" @click="loadMaintenanceDefinition">重新加载配置</el-button>
        </div>
        <GuideTipPanel v-if="parseError" type="warning" allow-wrap class="row-full" role="alert">
          <template #title-append>
            <span class="tip-import-error">文件解析失败：{{ parseError }}</span>
          </template>
        </GuideTipPanel>
        <div class="import-file-section row-full">
          <el-upload ref="uploadRef" class="import-file-upload" drag :limit="1" :auto-upload="false"
                     accept=".csv,.xlsx" :disabled="parsing || definitionUnavailable" :on-change="parseFile"
                     :on-remove="removeImportFile" :on-exceed="handleImportExceed"
          >
            <el-icon class="import-file-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              {{ parsing ? '文件解析中，请稍候' : '将文件拖到此处，或' }}
              <em v-if="!parsing">点击选择</em>
            </div>
            <template #tip>
              <div class="import-file-tip">
                <span>仅允许导入 CSV、XLSX 格式文件。</span>
                <el-link type="primary" :underline="false" :disabled="templateLoading || parsing || definitionUnavailable"
                         @click.stop="downloadTemplate"
                >
                  {{ templateLoading ? '模板生成中' : '下载模板' }}
                </el-link>
              </div>
            </template>
          </el-upload>
        </div>
        <div class="h2-titles row-full">导入设置</div>
        <qt-form-item label="导入方式" prop="importMode" required class="row-full">
          <el-radio-group v-model="form.importMode" :disabled="definitionUnavailable">
            <el-radio value="CREATE_ONLY">仅新增</el-radio>
            <el-radio value="UPSERT">新增＋更新</el-radio>
            <el-radio value="UPDATE_ONLY">仅更新</el-radio>
          </el-radio-group>
        </qt-form-item>
        <el-text v-if="recordIdImport" type="info" class="row-full">
          已识别正式数据导出文件，按主数据ID匹配现有记录；新增行请留空主数据ID。
        </el-text>
        <el-text v-else-if="directCreateImport" type="info" class="row-full">
          编码将自动生成，文件中的每行数据均按新增处理。
        </el-text>
        <qt-form-item v-else label="匹配业务键" prop="matchField" required class="row-full">
          <el-text v-if="definitionLoading" type="info">字段配置加载中</el-text>
          <el-select v-if="uniqueColumns.length" v-model="form.matchField" filterable clearable
                     placeholder="请选择匹配业务键"
          >
            <el-option v-for="column in uniqueColumns" :key="column.prop" :value="column.prop"
                       :label="`${column.label}（${column.prop}）`"
            />
          </el-select>
          <el-text v-else-if="!definitionUnavailable" type="warning" role="alert">
            {{ form.importMode === 'CREATE_ONLY' ? '未检测到可用于导入匹配的手工唯一业务字段。' : '当前模型没有手工唯一业务字段，无法匹配现有数据，请使用仅新增方式。' }}
          </el-text>
        </qt-form-item>
      </template>

      <div v-if="!isBatchStatus" class="h2-titles row-full">申请信息</div>
      <qt-form-item label="申请原因" prop="applyReason" required class="row-full">
        <el-input v-model="form.applyReason"
                  type="textarea"
                  :rows="3"
                  maxlength="256个字符"
                  show-word-limit
                  placeholder="请输入本次批量操作的原因"
        />
      </qt-form-item>
    </el-form>

    <div v-if="isBatchStatus ? validation && !validation.valid : step === 1 && (!isApprovalMode || !validation?.valid)"
         :class="{ 'mt15': isBatchStatus }"
    >
      <GuideTipPanel v-if="mode === 'BATCH_IMPORT' && !validation?.valid" type="warning" allow-wrap class="mb15" role="alert">
        <template #title-append>
          <span class="tip-import-error">
            {{ validationBatchErrors.length ? `校验未通过：${validationBatchErrors.join('；')}` : '校验未通过，请检查导入数据。' }}
          </span>
        </template>
      </GuideTipPanel>
      <el-alert v-else-if="validationBatchErrors.length || isBatchStatus" type="error" :closable="false" class="mb15">
        <template #title>{{ validationBatchErrors.join('；') || '校验未通过，请核对所选数据的状态和操作权限。' }}</template>
      </el-alert>
      <qt-table v-if="validation?.rows?.length" ref="validationTableRef" v-bind="validationTableStore">
        <template #rowNumber="{ row }">{{ row.rowNo }}</template>
        <template #recordId="{ row }">{{ row.recordId ?? '-' }}</template>
        <template #recordName="{ row }">{{ validationRecordName(row) }}</template>
        <template #businessKey="{ row }">{{ validationBusinessKey(row) }}</template>
        <template #status="{ row }">
          <el-tag :type="hasValidationErrors(row) ? 'danger' : row.operation === 'SKIP' ? 'info' : 'success'">
            {{ hasValidationErrors(row) ? '失败' : row.operation === 'SKIP' ? '不处理' : '通过' }}
          </el-tag>
        </template>
        <template #operation="{ row }">
          <el-tooltip :disabled="!hasValidationErrors(row) && !row.message"
                      :content="validationFailureReason(row)" placement="top"
          >
            <dict-tag :value="row.operation" :options="importOperationOptions" />
          </el-tooltip>
        </template>
        <template #reason="{ row }">{{ validationFailureReason(row) }}</template>
        <template #importValue="{ row, column_data }">
          <el-tooltip :disabled="!row.errors?.[column_data.prop]" :content="row.errors?.[column_data.prop] || ''" placement="top">
            <el-text :type="row.errors?.[column_data.prop] ? 'danger' : undefined">
              {{ displayImportValue(row, column_data.prop) }}
            </el-text>
          </el-tooltip>
        </template>
      </qt-table>
    </div>

    <div v-else-if="!isBatchStatus && isApprovalMode && step === confirmStep">
      <DescriptionsInfo :column="2" :border="true" :items="confirmItems"
                        placeholder="-" class="mt15 mb15"
      >
        <template #updateSummary>
          <el-space wrap>
            <el-tag v-for="item in updateSummary" :key="item.field" type="info">
              {{ item.label }}：{{ displayValue(item.value, item.field) }}
            </el-tag>
          </el-space>
        </template>
      </DescriptionsInfo>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="step === 0" :disabled="isBatchStatus && (validating || submitting)" @click="close">取消</el-button>
        <el-button v-if="!isBatchStatus && step > 0 && step <= confirmStep"
                   :disabled="validating || submitting" @click="step--"
        >上一步</el-button>
        <el-button v-if="step === 0" type="primary" :loading="validating || submitting || parsing || definitionLoading"
                   :disabled="(mode === 'BATCH_IMPORT' && importUnavailable) || (mode === 'BATCH_UPDATE' && updateUnavailable)"
                   @click="validateAndNext"
        >{{ isBatchStatus ? '提交审批' : '校验' }}</el-button>
        <el-button v-if="!isBatchStatus && step === confirmStep" type="primary" :loading="submitting"
                   :disabled="!validation?.valid || validating"
                   @click="confirmSubmit"
        >{{ mode === 'BATCH_IMPORT' ? '确定' : '提交审批' }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import { useDict } from '@/utils/dict'
import { UploadFilled } from '@element-plus/icons-vue'
import { saveAs } from 'file-saver'

import Steps from '@/components/Steps/index.vue'
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import { parseTime } from '@/utils/anivia.js'
import { trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired } from '@/utils/validate.js'
import { downloadMasterChangeImportTemplate, parseMasterChangeImport, submitMasterChangeBatch,
  validateMasterChangeBatch, getMaintenanceFormConfig } from '@/api/master/change/request'

function isAutomaticColumn(column) {
  return String(column.encodingMode || '').toUpperCase() === 'AUTOMATIC'
}

function getReferenceDisplayValue(value, column) {
  if (value === null || value === undefined || value === '') return value
  const label = column.valueLabels?.[String(value)]
    ?? column.referenceOptions?.find(option => String(option.value) === String(value))?.label
  return label === null || label === undefined || label === '' ? value : label
}

// 与后端 MasterLifecycleRules 保持一致，归档为终态。
const LIFECYCLE_TRANSITIONS = {
  '1': { FREEZE: '2', INVALIDATE: '3' },
  '2': { UNFREEZE: '1', INVALIDATE: '3' },
  '3': { RECOVER: '1', ARCHIVE: '4' }
}

function getLifecycleTarget(status, action) {
  return Object.hasOwn(LIFECYCLE_TRANSITIONS, status) && Object.hasOwn(LIFECYCLE_TRANSITIONS[status], action)
    ? LIFECYCLE_TRANSITIONS[status][action] : ''
}

function hasPendingMaintenance(row) {
  if (!row.MASTER_APPROVAL_REQUEST_ID) return false
  const approval = String(row.MASTER_APPROVAL_STATUS)
  return approval === '0' || (approval === '1' && ['0', '1'].includes(String(row.MASTER_EXECUTE_STATUS)))
}

function canLifecycleAction(row, action) {
  return !hasPendingMaintenance(row) && Boolean(getLifecycleTarget(row.STATUS, action))
}

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], required: true },
  modelId: { type: [String, Number], default: '' },
  modelVersionId: { type: [String, Number], default: '' },
  mode: { type: String, default: 'BATCH_UPDATE' },
  selectedRows: { type: Array, default: () => [] },
  recordIds: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  modelName: { type: String, default: '' },
  modelType: { type: String, default: 'NORMAL' },
})
const emit = defineEmits(['update:modelValue', 'saved', 'submitted'])
const { proxy } = getCurrentInstance()
const { master_change_type } = useDict('master_change_type')
const importOperationOptions = computed(() => [
  { value: 'CREATE', label: '新增', elTagType: 'success' },
  { value: 'UPDATE', label: '更新', elTagType: 'primary' },
  { value: 'SKIP', label: '不处理', elTagType: 'info' }
].map(option => ({
  ...option,
  ...(master_change_type.value || []).find(item => item.value === option.value)
})))
const step = ref(0)
const checkedFields = ref([])
const maintenanceDefinition = ref(null)
const definitionLoading = ref(false)
const definitionError = ref('')
let definitionSerial = 0
const validation = ref(null)
const validating = ref(false)
const submitting = ref(false)
const parsing = ref(false)
const parseError = ref('')
const templateLoading = ref(false)
const fileName = ref('')
const importHeaders = ref([])
const uploadRef = ref(null)
const batchFormRef = ref(null)
const isBatchStatus = computed(() => props.mode === 'BATCH_STATUS')
const isApprovalMode = computed(() => ['BATCH_UPDATE', 'BATCH_STATUS'].includes(props.mode))
const confirmStep = 1
const stepsList = computed(() => {
  const firstStep = {
    title: props.mode === 'BATCH_IMPORT' ? '导入数据' : '选择字段'
  }
  return isApprovalMode.value
    ? [firstStep, { title: '提交审批' }]
    : [firstStep, { title: '校验确认' }]
})
const form = reactive({
  recordIds: [], businessKeys: {}, values: {}, action: '', rows: [], importMode: 'CREATE_ONLY', matchField: '', applyReason: ''
})

const validationTableRef = ref(null)
const validationTableStore = computed(() => ({
  config: { initResquest: false, autoPagination: true,
    pagination: { pageSizes: [10, 20, 50, 100], size: 'small', pagerCount: 5,
      layout: 'total, sizes, prev, pager, next', notAutoScroll: true },
    table: { stripe: true, ...(props.mode === 'BATCH_IMPORT' ? { maxHeight: 340 } : { height: 340 }), rowKey: 'rowNo' } },
  columns: [
    { label: props.mode === 'BATCH_STATUS' ? '行号' : '编号', prop: 'rowNo', width: 80, align: 'left', slot: 'rowNumber', fixed: 'left', sortable: 'custom' },
    ...(props.mode === 'BATCH_UPDATE' ? [
      { label: '主数据ID', prop: 'recordId', minWidth: 180, align: 'left', slot: 'recordId', showOverflowTooltip: true },
      ...(businessKeyColumn.value ? [{ label: businessKeyColumn.value.label || businessKeyColumn.value.prop,
        prop: 'businessKey', minWidth: 180, align: 'left', slot: 'businessKey', showOverflowTooltip: true }] : []),
      ...(nameFieldColumn.value && nameFieldColumn.value.prop !== businessKeyColumn.value?.prop
        ? [{ label: nameFieldColumn.value.label || nameFieldColumn.value.prop, prop: 'recordName',
          minWidth: 180, align: 'left', slot: 'recordName', showOverflowTooltip: true }] : [])
    ] : !directCreateImport.value
      ? [{ label: recordIdImport.value ? '主数据ID' : '业务主键', prop: 'businessKey', minWidth: 180, align: 'left', slot: 'businessKey', showOverflowTooltip: true }] : []),
    ...(props.mode !== 'BATCH_IMPORT'
      ? [{ label: '校验状态', prop: 'status', width: 110, slot: 'status', align: 'left' }] : []),
    { label: '处理方式', prop: 'operation', width: 110, slot: 'operation', align: 'left' },
    ...(props.mode !== 'BATCH_IMPORT'
      ? [{ label: '校验说明', prop: 'reason', minWidth: 260, align: 'left', slot: 'reason', showOverflowTooltip: true }] : []),
    ...(props.mode === 'BATCH_IMPORT' ? importHeaders.value.map(header => ({
      label: columnLabel(header), prop: header, minWidth: 180,
      align: 'left', slot: 'importValue', showOverflowTooltip: true
    })) : [])
  ],
  func: async (params = {}) => {
    const rows = [...(validation.value?.rows || [])]
    if (params.orderByColumn === 'rowNo' && params.isAsc) {
      const direction = params.isAsc === 'descending' ? -1 : 1
      rows.sort((left, right) => direction * (left.rowNo - right.rowNo))
    }
    return { data: rows }
  }
}))

watch([step, () => form.rows, validation], async () => {
  await nextTick()
  validationTableRef.value?.resetQuery()
})

// 导入规则以维护配置为准；列表字段不包含自动编码等维护属性。
const businessColumns = computed(() => (props.mode === 'BATCH_IMPORT'
  ? maintenanceDefinition.value?.columns || [] : props.columns).filter(column =>
  column.businessField === true && column.prop && column.prop !== 'ID'))
const editableColumns = computed(() => (maintenanceDefinition.value?.columns || []).filter(column =>
  column.businessField === true && column.prop && column.prop !== 'ID'
    && !isAutomaticColumn(column)
    && String(column.columnCategory || column.category || '').toUpperCase() !== 'SYSTEM'))
const definitionUnavailable = computed(() => definitionLoading.value || !maintenanceDefinition.value
  || Boolean(definitionError.value))
const updateUnavailable = computed(() => definitionUnavailable.value || !editableColumns.value.length)
const uniqueColumns = computed(() => businessColumns.value.filter(column =>
  column.unique === true && !isAutomaticColumn(column)))
const recordIdImport = computed(() => props.mode === 'BATCH_IMPORT'
  && form.rows.some(row => String(row.recordId || '').trim()))
const directCreateImport = computed(() => props.mode === 'BATCH_IMPORT' && form.importMode === 'CREATE_ONLY'
  && !recordIdImport.value && !uniqueColumns.value.length && businessColumns.value.some(isAutomaticColumn))
const importUnavailable = computed(() => definitionUnavailable.value
  || (!recordIdImport.value && !directCreateImport.value && !uniqueColumns.value.some(column => column.prop === form.matchField)))
function syncMatchField() {
  if (!uniqueColumns.value.some(column => column.prop === form.matchField)) {
    form.matchField = uniqueColumns.value.length === 1 ? uniqueColumns.value[0].prop : ''
  }
}
watch(uniqueColumns, syncMatchField, { immediate: true })
watch([() => form.matchField, () => form.importMode], () => {
  validation.value = null
})
const businessKeyColumn = computed(() => {
  if (props.mode === 'BATCH_IMPORT' && form.matchField) {
    return businessColumns.value.find(column => column.prop === form.matchField)
  }
  return uniqueColumns.value[0] || businessColumns.value.find(column => column.nameField === true)
})
const nameFieldColumn = computed(() => businessColumns.value.find(column => column.nameField === true))
const selectedRecordsById = computed(() => new Map(props.selectedRows.map(row => [String(row.ID), row])))
const title = computed(() => props.mode === 'BATCH_UPDATE'
  ? '批量修改'
  : props.mode === 'BATCH_IMPORT'
    ? '批量导入'
    : props.mode === 'BATCH_STATUS'
      ? '批量状态'
    : `${modeLabel(props.mode)}${props.modelName ? ` · ${props.modelName}` : ''}`)
const itemCount = computed(() => props.mode === 'BATCH_IMPORT' ? form.rows.length : form.recordIds.length)
const updateSummary = computed(() => checkedFields.value.map(field => ({
  field,
  label: columnLabel(field),
  value: form.values[field]
})))
const applyReasonRules = [
  { required: true, whitespace: true, message: '请输入申请原因', trigger: 'blur' },
  { max: 500, message: '申请原因不能超过500字', trigger: 'blur' }
]
const batchRules = computed(() => {
  const rules = { applyReason: applyReasonRules }
  if (isBatchStatus.value) {
    rules.action = [{ required: true, message: '请选择目标状态操作', trigger: 'change' }]
    return rules
  }
  if (props.mode === 'BATCH_IMPORT') {
    rules.importMode = [{ required: true, message: '请选择导入方式', trigger: 'change' }]
    if (!recordIdImport.value && !directCreateImport.value) {
      rules.matchField = [{ required: true, message: '请选择匹配业务键', trigger: 'change' }]
    }
    return rules
  }
  for (const field of checkedFields.value) {
    const label = columnLabel(field)
    if (['客户名称', '产品名称'].includes(label)) {
      rules[`values.${field}`] = [
        { required: true, validator: validateTrimmedRequired(`${label}不能为空`, 64), trigger: 'blur' },
        { validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, `${label}只允许中文、英文字母、数字、下划线和短横线`), trigger: 'blur', transform: trimText }
      ]
      continue
    }
    if (['供应商编码', '产品编码'].includes(label)) {
      rules[`values.${field}`] = [
        { required: true, validator: validateTrimmedRequired(`${label}不能为空`, 64), trigger: 'blur' },
        { validator: validateAlphaNumUnderscore(`${label}只允许英文字母、数字和下划线`), trigger: 'blur', transform: trimText }
      ]
      continue
    }
    rules[`values.${field}`] = [{
      required: true,
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || (typeof value === 'string' && !value.trim())) {
          return callback(new Error(`${columnLabel(field)}不能为空`))
        }
        callback()
      },
      trigger: ['blur', 'change']
    }]
  }
  return rules
})
const statusActionCodes = new Set(['FREEZE', 'UNFREEZE', 'INVALIDATE', 'RECOVER', 'ARCHIVE'])
const statusActions = computed(() => (master_change_type.value || [])
  .filter(item => statusActionCodes.has(item.value))
  .map(item => ({ ...item, disabled: props.selectedRows.some(row => !canLifecycleAction(row, item.value)) })))
const confirmItems = computed(() => {
  const items = [
    { label: '目标模型', value: props.modelName || '-' },
    { label: '数据数量', value: `${itemCount.value} 条` },
    { label: '申请类型', value: modeLabel(props.mode) },
    { label: '校验结果', value: `${validation.value?.passed || 0}/${validation.value?.total || itemCount.value} 条通过` }
  ]
  if (props.mode === 'BATCH_UPDATE') {
    items.push({ label: '统一修改字段', slot: 'updateSummary', span: 2 })
  } else if (props.mode === 'BATCH_IMPORT') {
    const modeLabel = { CREATE_ONLY: '仅新增', UPSERT: '新增＋更新', UPDATE_ONLY: '仅更新' }[form.importMode]
    items.push({ label: '导入方式', value: recordIdImport.value ? `${modeLabel}（按主数据ID匹配）` : directCreateImport.value
      ? `${modeLabel}（编码自动生成）` : `${modeLabel}（按${columnLabel(form.matchField)}匹配）`, span: 2 })
    items.push({ label: '本次处理', value: `${validation.value?.rows?.filter(row => row.operation === 'CREATE').length || 0} 条新增、${validation.value?.rows?.filter(row => row.operation === 'UPDATE').length || 0} 条更新、${validation.value?.rows?.filter(row => row.operation === 'SKIP').length || 0} 条不处理`, span: 2 })
  }
  items.push({ label: '申请原因', value: form.applyReason, span: 2 })
  return items
})

function reset() {
  step.value = 0
  validation.value = null
  fileName.value = ''
  parseError.value = ''
  form.recordIds = (props.recordIds.length
    ? props.recordIds
    : props.selectedRows.map(row => row?.ID))
    .filter(id => id !== null && id !== undefined && id !== '')
    .map(String)
  const businessKeyProp = businessKeyColumn.value?.prop
  form.businessKeys = Object.fromEntries(props.selectedRows
    .filter(row => row?.ID !== null && row?.ID !== undefined)
    .map(row => [String(row.ID), businessKeyProp ? row[businessKeyProp] : row.ID]))
  form.values = {}
  form.action = ''
  form.rows = []
  form.importMode = 'CREATE_ONLY'
  form.matchField = ''
  syncMatchField()
  form.applyReason = ''
  checkedFields.value = Object.keys(form.values)
  importHeaders.value = [...new Set(form.rows.flatMap(row => Object.keys(row.values || {})))]
}

async function loadMaintenanceDefinition() {
  const serial = ++definitionSerial
  maintenanceDefinition.value = null
  definitionError.value = ''
  definitionLoading.value = true
  checkedFields.value = []
  form.values = {}
  validation.value = null
  try {
    if (!props.modelId || !props.modelVersionId) throw new Error('模型发布配置不可用，请刷新页面后重试')
    const response = await getMaintenanceFormConfig(props.instanceId, {
      modelId: props.modelId, modelVersionId: props.modelVersionId
    })
    if (serial !== definitionSerial) return
    const definition = response?.data
    if (!definition || String(definition.instanceId) !== String(props.instanceId)
        || String(definition.modelId) !== String(props.modelId)
        || String(definition.modelVersionId) !== String(props.modelVersionId)
        || !Array.isArray(definition.columns)) {
      throw new Error('模型发布版本已变化，请刷新页面后重新打开弹窗')
    }
    maintenanceDefinition.value = definition
  } catch (error) {
    if (serial === definitionSerial) {
      definitionError.value = error?.msg || error?.message || '字段配置加载失败，请重试'
    }
  } finally {
    if (serial === definitionSerial) definitionLoading.value = false
  }
}

function modeLabel(mode) {
  return proxy.selectDictLabel(master_change_type.value || [], mode) || mode || '-'
}

function columnLabel(prop) {
  return businessColumns.value.find(column => column.prop === prop)?.label || prop
}

function fieldPlaceholder(field) {
  const label = columnLabel(field)
  if (['客户名称', '产品名称'].includes(label)) return `请输入${label}，支持中文、英文字母、数字、下划线和短横线`
  if (['供应商编码', '产品编码'].includes(label)) return `请输入${label}，仅支持英文字母、数字和下划线`
  return `请输入${label}`
}

function importSourceRow(row) {
  return form.rows.find((item, index) => Number(item.rowNo ?? index + 2) === Number(row.rowNo))
}

function validationBusinessKey(row) {
  const prop = businessKeyColumn.value?.prop
  if (props.mode === 'BATCH_IMPORT') {
    const source = importSourceRow(row)
    if (recordIdImport.value) return source?.recordId || '-'
    return (prop && source?.values?.[prop]) || '-'
  }
  return form.businessKeys[String(row.recordId)] ?? row.recordId ?? '-'
}

function validationRecordName(row) {
  const column = nameFieldColumn.value
  const record = selectedRecordsById.value.get(String(row.recordId))
  const value = column && record?.[column.prop]
  if (value === null || value === undefined || value === '') return '-'
  return getReferenceDisplayValue(value, column)
}

function hasValidationErrors(row) {
  return Object.keys(row.errors || {}).length > 0
}

const validationBatchErrors = computed(() => Object.entries(validation.value?.errors || {})
  .filter(([field, message]) => !(props.mode === 'BATCH_IMPORT' && field === 'rows'
    && message === '当前导入方式下没有可处理的数据行'))
  .map(([field, message]) => `${{ applyReason: '申请原因', rows: '导入数据', matchField: '匹配业务键', importMode: '导入方式' }[field] || field}：${message}`))

watch([() => form.action, () => form.applyReason], () => {
  if (isBatchStatus.value && !validating.value && !submitting.value) validation.value = null
})

function validationFailureReason(row) {
  const messages = Object.entries(row.errors || {}).filter(([, message]) => message)
    .map(([field, message]) => `${field === 'row' ? '行' : field === 'permission' ? '权限' : columnLabel(field)}：${message}`)
  if (messages.length) return messages.join('；')
  return row.message || (row.operation === 'SKIP' ? '根据导入方式跳过' : '-')
}

function columnDataType(prop) {
  return String(businessColumns.value.find(column => column.prop === prop)?.dataType || '').toUpperCase()
}

function isDateField(prop) {
  return columnDataType(prop) === 'DATE'
}

function isDateTimeField(prop) {
  return columnDataType(prop) === 'DATETIME'
}

function displayValue(value, field) {
  if (value === null || value === undefined || value === '') return '（清空）'
  const column = businessColumns.value.find(item => item.prop === field)
  return /TIME/i.test(column?.dataType || '') ? parseTime(value) || value : value
}

function displayImportValue(row, field) {
  const value = importSourceRow(row)?.values?.[field]
  const column = businessColumns.value.find(item => item.prop === field) || { prop: field }
  const label = getReferenceDisplayValue(value, column)
  return label === value ? displayValue(value, field) : label
}

function payload() {
  const data = { mode: props.mode, applyReason: form.applyReason }
  if (props.mode === 'BATCH_UPDATE') {
    data.recordIds = [...form.recordIds]
    data.values = Object.fromEntries(checkedFields.value
      .filter(field => editableColumns.value.some(column => column.prop === field))
      .map(field => [field, form.values[field] ?? '']))
  } else if (props.mode === 'BATCH_STATUS') {
    data.recordIds = [...form.recordIds]
    data.action = form.action || null
  } else {
    data.rows = form.rows.map((row, index) => ({
      rowNo: row.rowNo ?? index + 2,
      recordId: row.recordId || null,
      parentId: row.parentId ?? null,
      values: { ...(row.values || {}) }
    }))
    data.importMode = form.importMode
    data.matchField = recordIdImport.value || directCreateImport.value ? null : form.matchField || null
    data.fileName = fileName.value
  }
  return data
}

function checkBeforeValidate() {
  if (props.mode === 'BATCH_IMPORT' && definitionUnavailable.value) {
    throw new Error(definitionError.value || '请等待维护字段配置加载完成')
  }
  if (props.mode === 'BATCH_IMPORT' && parseError.value) throw new Error(parseError.value)
  if (!itemCount.value) throw new Error(props.mode === 'BATCH_IMPORT' ? '请先上传并解析导入文件' : '请先勾选主数据')
  if (!form.applyReason.trim()) throw new Error('请输入申请原因')
  if (props.mode === 'BATCH_UPDATE') {
    if (updateUnavailable.value) throw new Error(definitionError.value || '当前没有可用的批量修改字段')
    if (!checkedFields.value.length) throw new Error('请选择至少一个修改字段')
    if (checkedFields.value.some(field => !editableColumns.value.some(column => column.prop === field))) {
      throw new Error('修改字段已不可编辑，请重新选择')
    }
  }
  if (props.mode === 'BATCH_STATUS' && !form.action) throw new Error('请选择目标状态操作')
  if (props.mode === 'BATCH_IMPORT' && importUnavailable.value) {
    throw new Error('请选择模型配置的唯一业务字段作为匹配业务键')
  }
}

async function validateAndNext() {
  if (validating.value || submitting.value || parsing.value || definitionLoading.value) return
  validating.value = true
  try {
    try { await batchFormRef.value?.validate() } catch { return }
    try { checkBeforeValidate() } catch (error) { proxy.$modal.msgWarning(error.message); return }
    validation.value = null
    const response = await validateMasterChangeBatch(props.instanceId, payload())
    validation.value = response?.data || { valid: false, rows: [], errors: { request: '未返回校验结果' } }
    if (!isBatchStatus.value) step.value = 1
  } catch (error) {
    if (!isBatchStatus.value) throw error
  } finally {
    validating.value = false
  }
  if (isBatchStatus.value && validation.value?.valid === true) {
    try { await confirmSubmit() } catch { /* 请求拦截器展示错误，保留表单供重试。 */ }
  }
}

async function confirmSubmit() {
  if (!validation.value?.valid || validating.value || submitting.value) return
  submitting.value = true
  try {
    const response = await submitMasterChangeBatch(props.instanceId, payload())
    const result = response?.data || {}
    proxy.$modal.msgSuccess(props.mode === 'BATCH_IMPORT' ? '上传成功' : '提交成功')
    emit('submitted', result)
    emit('update:modelValue', false)
  } catch (error) {
    if (props.mode === 'BATCH_IMPORT') {
      proxy.$modal.msgError(error?.msg || error?.message || '上传失败')
    }
  } finally {
    submitting.value = false
  }
}

function beforeImport(file) {
  if (!/\.(csv|xlsx)$/i.test(file.name || '')) { parseError.value = '仅允许导入 CSV、XLSX 格式文件。'; return false }
  if (file.size > 5 * 1024 * 1024) { parseError.value = '导入文件不能超过 5 MiB'; return false }
  return true
}

async function parseFile(uploadFile) {
  const raw = uploadFile?.raw
  if (!raw) return
  removeImportFile()
  if (!beforeImport(raw)) return
  parsing.value = true
  try {
    const response = await parseMasterChangeImport(props.instanceId, raw)
    const data = response?.data || {}
    if (!Array.isArray(data.rows) || !data.rows.length) {
      throw new Error('文件中没有可导入的数据')
    }
    form.rows = data.rows || []
    if (recordIdImport.value) form.importMode = 'UPDATE_ONLY'
    importHeaders.value = data.headers?.filter(header => header !== 'CATEGORY_ID') || []
    fileName.value = raw.name
    validation.value = null
    proxy.$modal.msgSuccess(`文件解析完成，共 ${form.rows.length} 行`)
  } catch (error) {
    form.rows = []
    importHeaders.value = []
    fileName.value = ''
    parseError.value = error?.msg || error?.message || '导入文件解析失败，请检查文件格式和表头是否与当前模型一致'
  } finally {
    parsing.value = false
  }
}

function removeImportFile() {
  parseError.value = ''
  form.rows = []
  importHeaders.value = []
  fileName.value = ''
  validation.value = null
}

function handleImportExceed(files) {
  uploadRef.value?.clearFiles()
  if (files?.[0]) uploadRef.value?.handleStart(files[0])
}

async function downloadTemplate() {
  templateLoading.value = true
  try {
    const blob = await downloadMasterChangeImportTemplate(props.instanceId)
    saveAs(blob, `authority_import_template_${props.instanceId}.xlsx`)
  } finally {
    templateLoading.value = false
  }
}

function close() {
  if (validating.value || parsing.value || submitting.value) return
  definitionSerial++
  emit('update:modelValue', false)
}

watch([() => props.modelValue, () => props.instanceId, () => props.modelId,
  () => props.modelVersionId, () => props.mode], ([visible]) => {
  definitionSerial++
  if (!visible) return
  maintenanceDefinition.value = null
  definitionError.value = ''
  definitionLoading.value = false
  reset()
  if (['BATCH_UPDATE', 'BATCH_IMPORT'].includes(props.mode)) loadMaintenanceDefinition()
}, { immediate: true })
watch(checkedFields, async () => {
  await nextTick()
  batchFormRef.value?.clearValidate()
}, { deep: true })
</script>

<style scoped lang="scss">
.batch-steps :deep(.zl-step.is-dialog) {
  // 步骤条与步骤项等高，避免步骤项向上溢出，侵占弹窗顶部的 15px 留白。
  height: 40px;
  margin: 0;
}

:deep(.tip-import-error) {
  color: var(--qt-status-danger);
}

.import-file-section {
  .import-file-upload {
    width: 100%;

    :deep(.el-upload) {
      width: 100%;
    }

    :deep(.el-upload-dragger) {
      width: 100%;
      min-height: 184px;
      padding: 48px 20px 36px;
      border-color: var(--el-border-color);
      border-radius: 4px;
    }

    :deep(.el-upload-list) {
      margin-top: 10px;
    }
  }

  .import-file-icon {
    margin-bottom: 14px;
    color: var(--el-text-color-placeholder);
    font-size: 58px;
  }

  .import-file-tip {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 4px;
    margin-top: 8px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 20px;
  }
}
</style>
