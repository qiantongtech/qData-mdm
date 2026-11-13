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
  <div ref="app-container">
    <el-dialog
        v-model="visibleDialog"
        draggable
        class="large-dialog"
        :append-to="$refs['app-container']"
        destroy-on-close
        @close="reset"
    >
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :validate-on-rule-change="false"
          scroll-to-error
          :scroll-into-view-options="{ block: 'center' }"
          label-width="110px"
          class="column-form"
          @submit.prevent
      >
        <div class="h2-title row-full">基础配置</div>

        <qt-form-item label="任务名称" prop="taskName" required>
          <el-input
            v-model="form.taskName"
            maxlength="64"
            placeholder="请输入任务名称，支持中文、英文字符、数字、下划线和短横线"
          />
        </qt-form-item>

        <qt-form-item label="主数据域" prop="projectId" required>
          <el-select v-model="selectedProjectId" placeholder="请选择主数据域" @change="handleProjectChange">
            <template #label="{ label }">
              <div v-if="selectedProject" class="source-option">
                <img :src="getProjectIcon(selectedProject.icon)" alt="" class="source-option__icon" />
                <span>{{ selectedProject.name }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option v-for="item in modelTree" :key="item.id" :value="String(item.id)" :label="item.name">
              <div class="source-option">
                <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon" />
                <span>{{ item.name }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>

        <qt-form-item label="模型目录" prop="layerId" required>
          <el-select
              v-model="selectedLayerId"
              placeholder="请选择模型目录"
              :disabled="!selectedProjectId"
              @change="handleLayerChange"
          >
            <el-option v-for="item in layerOptions" :key="item.id" :value="String(item.id)" :label="item.name" />
          </el-select>
        </qt-form-item>

        <qt-form-item label="主数据模型" prop="modelId" required>
          <el-select
              v-model="form.modelId"
              filterable
              clearable
              placeholder="请选择主数据模型"
              :disabled="!selectedLayerId"
              @change="handleModelChange"
          >
            <template #label="{ label }">
              <div v-if="selectedModelOption" class="source-option">
                <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon" />
                <span class="text-ellipsis">{{ selectedModelOption.name }} ({{ selectedModelOption.code }})</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option
                v-for="item in filteredModels"
                :key="item.id"
                :label="`${item.name} (${item.code})`"
                :value="String(item.id)"
            >
              <div class="source-option">
                <img :src="getModelIcon(item)" alt="" class="source-option__icon" />
                <span class="text-ellipsis">{{ item.name }} ({{ item.code }})</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>

        <qt-form-item label="负责人" prop="responsibleUserId">
          <el-select v-model="form.responsibleUserId" filterable placeholder="请选择负责人">
            <el-option v-for="user in responsibleUsers" :key="user.value" :label="user.label" :value="user.value" />
          </el-select>
        </qt-form-item>

        <qt-form-item label="联系电话">
          <el-input :model-value="responsiblePhone" placeholder="选择输入联系方式" disabled />
        </qt-form-item>

        <qt-form-item label="任务状态" class="row-full">
          <el-radio-group v-model="form.enableStatus">
            <el-radio value="ENABLED">上线</el-radio>
            <el-radio value="DISABLED">下线</el-radio>
          </el-radio-group>
        </qt-form-item>

        <qt-form-item label="描述" class="row-full">
          <el-input
            v-model="form.remark"
            class="description-input"
            type="textarea"
            :rows="3"
            maxlength="256"
            show-word-limit
            placeholder="请输入描述，建议说明重复检测任务的检测目的、数据范围等信息"
          />
        </qt-form-item>

        <qt-form-item
            label="检测范围"
            prop="scopeType"
            class="row-full"
            tip="用于设置本次重复检测的数据范围，可选择全部数据或指定条件范围。"
            required
        >
          <el-radio-group v-model="form.scopeType">
            <el-radio value="1">全部待治理来源数据</el-radio>
            <el-radio value="2">指定范围</el-radio>
          </el-radio-group>
        </qt-form-item>

        <template v-if="form.scopeType === '2'">
          <qt-form-item label="来源系统" prop="sourceSystemIds" required>
            <el-select
                ref="sourceSelectRef"
                v-model="form.sourceSystemIds"
                class="source-system-select"
                multiple
                collapse-tags
                collapse-tags-tooltip
                :max-collapse-tags="sourceMaxCollapseTags"
                :disabled="!form.modelId"
                filterable
                placeholder="请选择来源系统"
                @visible-change="retrySourceOptions"
            >
              <template #label="{ label, value }">
                <span class="source-option">
                  <img :src="getSourceIcon(scopeSystems.find((item) => String(item.id) === String(value)))" alt="" class="source-option__icon" />
                  <span class="text-ellipsis">{{ label }}</span>
                </span>
              </template>
              <el-option
                  v-for="item in scopeSystems"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              >
                <div class="source-option">
                  <img :src="getSourceIcon(item)" alt="" class="source-option__icon" />
                  <span>{{ item.name }}</span>
                </div>
              </el-option>
            </el-select>
            <div ref="sourceTagMeasureRef" class="source-tag-measure" aria-hidden="true">
              <el-tag v-for="item in selectedSourceSystems" :key="item.id" class="source-tag-probe" type="info" closable disable-transitions>
                <span class="source-option">
                  <img :src="getSourceIcon(item)" alt="" class="source-option__icon" />
                  <span>{{ item.name }}</span>
                </span>
              </el-tag>
              <el-tag v-for="count in selectedSourceSystems.length" :key="`more-${count}`" :data-more-count="count" type="info" disable-transitions>
                + {{ count }}
              </el-tag>
            </div>
          </qt-form-item>

          <qt-form-item label="接收时间">
            <el-date-picker
                v-model="form.receiveRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                clearable
            />
          </qt-form-item>
        </template>
        <template v-if="form.modelId">
          <section class="row-full result-section">
            <div class="result-heading">
              <div class="h2-title">检测确认</div>
              <span v-if="previewHint" class="result-hint" role="status">{{ previewHint }}</span>
            </div>
            <div v-show="!previewHint" class="preview-table-wrap">
              <qt-table ref="previewTableRef" v-bind="previewTableStore" :columns="previewTableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
                <template #empty>本页没有待检测数据</template>
                <template #previewField="{ row, column_data }">
                  {{ getSourcePreviewValue(row, column_data.property || column_data.prop) }}
                </template>
              </qt-table>
            </div>
          </section>

          <section class="row-full result-section">
            <div class="result-heading">
              <div class="h2-title">匹配规则</div>
              <span v-if="rulesHint" class="result-hint" role="status">{{ rulesHint }}</span>
            </div>
            <div v-if="!rulesHint" class="matching-rule-table-wrap">
              <qt-table
                  ref="matchingRuleTableRef"
                  :columns="matchingRuleColumns"
                  :func="queryMatchingRuleTable"
                  :config="matchingRuleTableConfig"
              >
                <template #ruleStatus="{ row }">
                  <el-tag :type="row.statusType" effect="light">{{ row.statusLabel }}</el-tag>
                </template>
              </qt-table>
            </div>
          </section>
        </template>
        <div class="column-form row-full schedule-content">
          <div class="h2-title row-full">调度周期</div>
          <qt-form-item label="执行方式" label-width="auto">
            <el-radio-group v-model="form.triggerType">
              <el-radio value="MANUAL">手动执行</el-radio>
              <el-radio value="SCHEDULED">Cron 表达式</el-radio>
            </el-radio-group>
          </qt-form-item>
          <qt-form-item
              v-if="form.triggerType === 'SCHEDULED'"
              label="调度周期"
              prop="cronExpression"
              label-width="auto"
              class="row-full"
              required
          >
            <el-input
                v-model="form.cronExpression"
                maxlength="128"
                placeholder="请输入调度周期"
            >
              <template #append>
                <el-button type="primary" @click="openCronGenerator">配置</el-button>
              </template>
            </el-input>
          </qt-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button
              v-hasPermi="['merge:detect:execute']"
              type="primary"
              :loading="submitLoading"
              :disabled="submitLoading"
              @click="confirmDialog"
          >确定</el-button>
        </div>
      </template>
    </el-dialog>
    <el-dialog
        v-model="cronDialogVisible"
        title="Cron 表达式生成器"
        width="860px"
        :append-to="$refs['app-container']"
        destroy-on-close
    >
      <crontab
          :expression="cronExpressionDraft"
          @hide="cronDialogVisible = false"
          @fill="fillCronExpression"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { getModelIcon } from "@/utils/imageUrl";
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import { useResizeObserver } from '@vueuse/core'

import Crontab from '@/components/Crontab/index.vue'
import { getSourceDataModelColumns } from '@/api/ingest/LOG'
import { previewMergeScope, saveMergeDetectTask, updateMergeDetectTask } from '@/api/merge/detect'
import { listRulematch } from '@/api/rule/match/match'

import { getPublishedModelTree } from '@/api/model/base/base'
import { listPublishedModels } from '@/api/model/base/base'
import { useUserDirectory } from '@/store/system/userDirectory'
import { getProjectIcon } from '@/utils/imageUrl'

const DETECT_PREVIEW_PAGE_SIZES = [5, 10, 20]

function parseJson(value, fallback) {
  if (value === null || value === undefined || value === '') return fallback
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const { loadResponsibleUserOptions, getResponsiblePhone } = useUserDirectory()
const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: '新增重复检测任务' },
  row: { type: Object, default: () => ({}) },
})

const emit = defineEmits(['update:visible', 'confirm'])
const { proxy } = getCurrentInstance()
const { rule_match_type } = proxy.useDict('rule_match_type')
const visibleDialog = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const formRef = ref()
const submitLoading = ref(false)
const cronDialogVisible = ref(false)
const cronExpressionDraft = ref('')
const modelOptions = ref([])
const modelTree = ref([])
const selectedProjectId = ref()
const selectedLayerId = ref()
const responsibleUsers = ref([])
const scopeSystems = ref([])
const sourceSelectRef = ref()
const sourceTagMeasureRef = ref()
const sourceMaxCollapseTags = ref(1)
const scopeCount = ref(null)
const scopeReady = ref(false)
const previewTableColumns = ref([])
const previewTableRef = ref()
const previewTableStore = reactive({
  columns: computed(() => previewTableColumns.value.map(({ modelColumn, ...column }) => ({
    ...column,
    align: 'left',
    showOverflowTooltip: true,
    ...(modelColumn ? { slot: 'previewField' } : {}),
  }))),
  func: queryPreviewTable,
  params: {},
  config: {
    initResquest: false,
    table: { stripe: true },
    pagination: {
      params: { pageNum: 1, pageSize: 5 },
      pageSizes: DETECT_PREVIEW_PAGE_SIZES,
      layout: 'total, sizes, prev, pager, next',
      notAutoScroll: true,
    },
  },
  events: {},
})
const matchingRules = ref([])
const matchingRuleTableRef = ref()
const matchingRuleColumns = [
  { label: '编号', prop: 'id', width: 70, minWidth: 70, align: 'center' },
  { label: '规则信息', prop: 'ruleName', minWidth: 180, align: 'left', showOverflowTooltip: true },
  { label: '规则说明', prop: 'description', minWidth: 200, align: 'left', showOverflowTooltip: true },
  { label: '匹配方式', prop: 'matchTypeLabel', minWidth: 130, align: 'left', showOverflowTooltip: true },
  { label: '参与字段', prop: 'fieldNames', minWidth: 200, align: 'left', showOverflowTooltip: true },
  { label: '状态', property: 'statusLabel', slot: 'ruleStatus', width: 100, minWidth: 100, align: 'center' },
]
const matchingRuleTableConfig = {
  notPagination: true,
  table: { stripe: true, rowKey: 'id' },
}
const rulesReady = ref(false)
const rulesLoadError = ref(false)
let scopeRequest = 0
let rulesRequest = 0
let initRequest = 0
let columnsRequest = 0
const scopeLoadError = ref(false)
const systemsReady = ref(false)

const form = reactive({
  id: undefined,
  taskName: '',
  projectId: undefined,
  layerId: undefined,
  modelId: undefined,
  responsibleUserId: undefined,
  enableStatus: 'DISABLED',
  remark: '',
  scopeType: '1',
  sourceSystemIds: [],
  receiveRange: [],
  triggerType: 'MANUAL',
  cronExpression: '',
})

const previewHint = computed(() => {
  if (scopeLoadError.value) return '待检测数据加载失败'
  if (form.scopeType === '2' && !form.sourceSystemIds.length) return '请选择要检测的来源系统'
  if (!scopeReady.value) return '正在加载待检测数据…'
  return scopeCount.value > 0 ? '' : '当前范围内没有待检测数据'
})

const rulesHint = computed(() => {
  if (rulesLoadError.value) return '匹配规则加载失败'
  if (!rulesReady.value) return '正在加载匹配规则…'
  return matchingRules.value.length ? '' : '当前模型没有可执行的匹配规则'
})

const selectedSourceSystems = computed(() => form.sourceSystemIds.map((id) => {
  const source = scopeSystems.value.find((item) => String(item.id) === String(id))
  return { ...source, id, name: source?.name || String(id) }
}))

function updateSourceTagCount() {
  const selection = sourceSelectRef.value?.$el?.querySelector('.el-select__selection')
  const measure = sourceTagMeasureRef.value
  if (!selection || !measure || !selection.clientWidth) return
  const tags = Array.from(measure.querySelectorAll('.source-tag-probe'))
  const gap = Number.parseFloat(selection.ownerDocument.defaultView.getComputedStyle(selection).gap) || 0
  const inputWidth = selection.querySelector('.el-select__input')?.getBoundingClientRect().width || 12
  const available = selection.clientWidth - inputWidth - gap
  let used = 0
  let visible = 0
  for (let index = 0; index < tags.length; index++) {
    used += tags[index].getBoundingClientRect().width + (index ? gap : 0)
    const remaining = tags.length - index - 1
    const moreWidth = remaining ? measure.querySelector(`[data-more-count="${remaining}"]`).getBoundingClientRect().width + gap : 0
    if (used + moreWidth <= available) visible = index + 1
  }
  sourceMaxCollapseTags.value = Math.max(1, visible)
}

watch([sourceSelectRef, sourceTagMeasureRef, selectedSourceSystems], () => nextTick(updateSourceTagCount), { flush: 'post' })
useResizeObserver(sourceSelectRef, updateSourceTagCount)
useResizeObserver(sourceTagMeasureRef, updateSourceTagCount)
useResizeObserver(() => sourceSelectRef.value?.$el?.querySelector('.el-select__input'), updateSourceTagCount)

const rules = computed(() => ({
  taskName: [
    { required: true, whitespace: true, message: '请输入任务名称', trigger: 'blur' },
    { max: 64, transform: (value) => value.trim(), message: '任务名称不能超过64个字符', trigger: 'blur' },
    { pattern: /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/, transform: (value) => value.trim(), message: '任务名称支持中文、英文字符、数字、下划线和短横线', trigger: 'blur' },
  ],
  projectId: [{ required: true, message: '请选择主数据域', trigger: 'change' }],
  layerId: [{ required: true, message: '请选择模型目录', trigger: 'change' }],
  modelId: [{ required: true, message: '请选择主数据模型', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择检测范围', trigger: 'change' }],
  sourceSystemIds: form.scopeType === '2'
      ? [{ required: true, type: 'array', min: 1, message: '请选择来源系统', trigger: 'change' }]
      : [],
  cronExpression: form.triggerType === 'SCHEDULED'
      ? [{ required: true, whitespace: true, message: '请输入调度周期', trigger: 'blur' }]
      : [],
}))

const matchingRuleRows = computed(() => matchingRules.value.map((rule) => {
  const config = parseJson(rule.ruleConfig, {})
  const configuredFields = Array.isArray(config.fieldList) && config.fieldList.length
    ? config.fieldList
    : String(rule.modelFieldId || '').split(',').filter(Boolean).map((modelFieldId) => ({ modelFieldId }))
  const fieldNames = configuredFields
    .map((item) => item?.modelFieldId ?? item?.fieldId ?? item?.columnId)
    .filter((fieldId) => fieldId != null)
    .map((fieldId) => {
      const field = previewTableColumns.value.find((column) => {
        const modelColumn = column.modelColumn
        return modelColumn && [modelColumn.id, modelColumn.columnCode, modelColumn.columnName]
          .some((value) => String(value) === String(fieldId))
      })?.modelColumn
      return field?.columnName || String(fieldId)
    })
  const ruleType = rule.ruleType || rule.type
  return {
    ...rule,
    ruleName: rule.ruleName || rule.name || '未命名规则',
    description: rule.description || rule.remark || '-',
    matchTypeLabel: rule_match_type.value.find((item) => item.value === ruleType)?.label || ruleType || '-',
    fieldNames: fieldNames.join('、') || '-',
    statusLabel: rule.validFlag === true || String(rule.validFlag) === '1' ? '启用' : '停用',
    statusType: rule.validFlag === true || String(rule.validFlag) === '1' ? 'success' : 'info',
  }
}))

function getSubmitError() {
  const range = form.receiveRange || []
  if (range[0] && range[1] && range[0] > range[1]) return '接收开始日期不能晚于结束日期'
  if (!scopeReady.value) return '检测范围正在加载，请稍后重试'
  if (Number(scopeCount.value || 0) <= 0) return '当前检测范围没有待治理来源数据，请调整检测范围'
  if (!rulesReady.value) return '匹配规则正在加载，请稍后重试'
  if (!matchingRuleRows.value.length) return '当前模型没有可执行匹配规则，请先配置并启用匹配规则'
  return ''
}

function reset() {
  initRequest++
  columnsRequest++
  scopeLoadError.value = false
  systemsReady.value = false
  scopeRequest++
  rulesRequest++
  form.id = undefined
  form.taskName = ''
  form.projectId = undefined
  form.layerId = undefined
  form.modelId = undefined
  selectedProjectId.value = undefined
  selectedLayerId.value = undefined
  form.responsibleUserId = undefined
  form.enableStatus = 'DISABLED'
  form.remark = ''
  form.scopeType = '1'
  form.sourceSystemIds = []
  form.receiveRange = []
  form.triggerType = 'MANUAL'
  form.cronExpression = ''
  scopeSystems.value = []
  scopeCount.value = null
  scopeReady.value = false
  previewTableColumns.value = []
  matchingRules.value = []
  rulesReady.value = false
  rulesLoadError.value = false
  formRef.value?.clearValidate()
}

async function init(row = {}) {
  reset()
  const requestId = initRequest
  // 表单初值在请求前设置，避免异步响应覆盖用户已经完成的输入和选择。
  form.id = row.id
  form.taskName = row.taskName || ''
  form.modelId = row.modelId == null ? undefined : String(row.modelId)
  form.responsibleUserId = row.responsibleUserId == null ? undefined : String(row.responsibleUserId)
  form.enableStatus = row.enableStatus || 'DISABLED'
  form.remark = row.remark || ''
  form.scopeType = String(row.scopeType || '1')
  const sourceSystemIds = parseJson(row.sourceSystemIds, [])
  form.sourceSystemIds = Array.isArray(sourceSystemIds) ? sourceSystemIds.map(String) : []
  form.receiveRange = form.scopeType === '2'
      ? [formatDate(row.receiveStartTime), formatDate(row.receiveEndTime)].filter(Boolean)
      : []
  form.triggerType = row.triggerType || 'MANUAL'
  form.cronExpression = row.cronExpression || ''
  const [modelsRes, tree, users] = await Promise.all([
    listPublishedModels({ pageNum: 1, pageSize: -1 }),
    getPublishedModelTree().catch(() => ({ data: [] })),
    loadResponsibleUserOptions(row.responsibleUserId),
  ])
  if (requestId !== initRequest || !visibleDialog.value) return
  modelOptions.value = modelsRes.data?.rows || modelsRes.rows || modelsRes.data || []
  modelTree.value = tree.data || []
  responsibleUsers.value = users
  if (selectedProjectId.value != null || String(form.modelId ?? '') !== String(row.modelId ?? '')) return
  const selectedModel = modelOptions.value.find((item) => String(item.id) === String(row.modelId))
  selectedProjectId.value = selectedModel?.projectId == null ? undefined : String(selectedModel.projectId)
  selectedLayerId.value = selectedModel?.layerId == null ? undefined : String(selectedModel.layerId)
  form.projectId = selectedProjectId.value
  form.layerId = selectedLayerId.value
}

const selectedProject = computed(() => modelTree.value.find((item) => String(item.id) === String(selectedProjectId.value)))
const layerOptions = computed(() => selectedProject.value?.children || [])
const filteredModels = computed(() => modelOptions.value.filter((item) =>
  String(item.projectId) === String(selectedProjectId.value)
  && String(item.layerId) === String(selectedLayerId.value)
))

function handleProjectChange() {
  form.projectId = selectedProjectId.value
  selectedLayerId.value = undefined
  form.layerId = undefined
  form.modelId = undefined
  handleModelChange()
}

function handleLayerChange() {
  form.layerId = selectedLayerId.value
  form.modelId = undefined
  handleModelChange()
}

function formatDate(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d}') : undefined
}

function handleModelChange() {
  systemsReady.value = false
  scopeSystems.value = []
  form.sourceSystemIds = []
  form.receiveRange = []
  matchingRules.value = []
  rulesReady.value = false
  rulesLoadError.value = false
}

function buildScopePayload() {
  const payload = {
    taskName: form.taskName.trim(),
    responsibleUserId: form.responsibleUserId,
    enableStatus: form.enableStatus,
    remark: form.remark,
    modelId: form.modelId,
    scopeType: form.scopeType || '1',
    triggerType: form.triggerType,
    cronExpression: form.triggerType === 'SCHEDULED' ? form.cronExpression.trim() : null,
  }
  if (payload.scopeType === '2') {
    payload.sourceSystemIds = form.sourceSystemIds
    const range = form.receiveRange || []
    if (range[0]) payload.receiveStartTime = range[0]
    if (range[1]) payload.receiveEndTime = range[1]
  }
  return payload
}

function openCronGenerator() {
  cronExpressionDraft.value = form.cronExpression || ''
  cronDialogVisible.value = true
}

function fillCronExpression(value) {
  form.cronExpression = value
  cronDialogVisible.value = false
}

/**
 * 将范围预览接口转换为 QtTable 的分页数据结构。
 * @param {Object} params 表格提供的页码和每页条数。
 * @returns {Promise<Object>} 包含当前页 rows 和总条数 total 的响应。
 */
async function queryPreviewTable(params = {}) {
  const requestId = ++scopeRequest
  if (!visibleDialog.value || !form.modelId) {
    scopeCount.value = null
    scopeReady.value = false
    return { data: { rows: [], total: 0 } }
  }
  const custom = form.scopeType === '2'
  const needsSource = custom && !form.sourceSystemIds.length
  scopeLoadError.value = false
  scopeReady.value = false
  scopeCount.value = null
  if (needsSource && systemsReady.value) return { data: { rows: [], total: 0 } }

  // 范围预览同时返回模型的全部可用来源；缺少来源选择时仅取选项，不展示全量记录。
  const payload = {
    ...(custom && !needsSource ? buildScopePayload() : { modelId: form.modelId, scopeType: '1' }),
    previewPage: params.pageNum || 1,
    previewPageSize: needsSource ? 1 : (params.pageSize || 5),
  }
  let response
  try {
    response = await previewMergeScope(payload)
  } catch (error) {
    if (requestId === scopeRequest) {
      scopeLoadError.value = true
      // 编辑历史任务时，已停用的来源可能导致范围校验失败，仍需加载可供改选的来源。
      if (custom && !needsSource && !systemsReady.value) {
        const fallback = await previewMergeScope({ modelId: form.modelId, scopeType: '1', previewPage: 1, previewPageSize: 1 })
        if (requestId === scopeRequest) setScopeSystems(fallback)
      }
    }
    throw error
  }
  if (requestId !== scopeRequest) return { data: { rows: [], total: 0 } }
  setScopeSystems(response)
  if (needsSource) return { data: { rows: [], total: 0 } }
  const records = response.data?.records || []
  scopeCount.value = response.data?.count ?? 0
  scopeReady.value = true
  return {
    data: {
      rows: records.map((row) => ({
        ...row,
        fields: parseJson(row.modelData, {}),
        receiveTime: row.receiveTime ? proxy.parseTime(row.receiveTime, '{y}-{m}-{d} {h}:{i}') : '-',
      })),
      total: Number(scopeCount.value || 0),
    },
  }
}

function setScopeSystems(response) {
  scopeSystems.value = (response.data?.systems || []).map((item) => ({ ...item, id: String(item.id) }))
  systemsReady.value = true
}

function retrySourceOptions(open) {
  if (open && scopeLoadError.value && !systemsReady.value) previewTableRef.value?.resetQuery()
}

async function loadModelColumns() {
  const requestId = ++columnsRequest
  previewTableColumns.value = []
  if (!visibleDialog.value || !form.modelId) return
  let columns = []
  try {
    const result = await getSourceDataModelColumns(form.modelId)
    columns = result.data || []
  } catch {
    // 字段不可用时仍展示来源系统、记录 ID 和接收时间。
  }
  if (requestId !== columnsRequest) return
  previewTableColumns.value = [
    { prop: 'systemName', label: '来源系统', minWidth: 180 },
    { prop: 'sourceRecordKey', label: '来源记录ID', minWidth: 220 },
    ...columns.map((column) => ({
      prop: `fields.${column.columnCode || column.id}`,
      label: column.columnName,
      minWidth: 160,
      modelColumn: column,
    })),
    { prop: 'receiveTime', label: '接收时间', minWidth: 200 },
  ]
  if (matchingRules.value.length) nextTick(() => matchingRuleTableRef.value?.getList())
}

async function loadMatchingRules() {
  const requestId = ++rulesRequest
  if (!visibleDialog.value || !form.modelId) {
    matchingRules.value = []
    rulesReady.value = false
    rulesLoadError.value = false
    return
  }
  rulesReady.value = false
  rulesLoadError.value = false
  try {
    const res = await listRulematch({ modelId: form.modelId, pageNum: 1, pageSize: -1, validFlag: true })
    if (requestId !== rulesRequest) return
    matchingRules.value = (res.data?.rows || []).filter((rule) => String(rule.abnormalFlag) !== '1')
  } catch {
    if (requestId === rulesRequest) {
      matchingRules.value = []
      rulesLoadError.value = true
    }
  } finally {
    if (requestId === rulesRequest) {
      rulesReady.value = true
    }
  }
}

async function queryMatchingRuleTable() {
  return { data: matchingRuleRows.value }
}

function getSourcePreviewValue(row, prop) {
  const column = previewTableColumns.value.find((item) => item.prop === prop)?.modelColumn
  if (!column) return '-'
  const value = row.fields?.[column.columnCode] ?? row.fields?.[column.id]
  return value === null || value === undefined || value === '' ? '-' : value
}

function closeDialog() {
  visibleDialog.value = false
}

async function confirmDialog() {
  if (submitLoading.value) return
  const requestId = initRequest
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || submitLoading.value || requestId !== initRequest || !visibleDialog.value) return
  if (scopeLoadError.value || rulesLoadError.value) {
    if (scopeLoadError.value) previewTableRef.value?.resetQuery()
    if (rulesLoadError.value) loadMatchingRules()
    proxy.$modal.msgWarning('正在重新加载检测配置，请在加载完成后再次确认')
    return
  }
  const submitError = getSubmitError()
  if (submitError) {
    proxy.$modal.msgWarning(submitError)
    return
  }
  submitLoading.value = true
  try {
    const payload = buildScopePayload()
    if (form.id) {
      await updateMergeDetectTask(form.id, payload)
      proxy.$modal.msgSuccess('检测任务修改成功')
    } else {
      const res = await saveMergeDetectTask(payload)
      proxy.$modal.msgSuccess(`检测任务已保存，任务ID ${res.data}，可点击“执行一次”发起检测`)
    }
    emit('confirm')
    if (requestId === initRequest) visibleDialog.value = false
  } finally {
    submitLoading.value = false
  }
}

watch(() => props.visible, (visible) => {
  if (visible) init(props.row)
})

watch(
    () => [previewTableRef.value, visibleDialog.value, form.modelId, form.scopeType, form.sourceSystemIds.join(','), (form.receiveRange || []).join(',')],
    () => {
      if (visibleDialog.value) {
        scopeRequest++
        scopeReady.value = false
        previewTableRef.value?.resetQuery()
      }
    },
    { flush: 'post' },
)

watch(() => [visibleDialog.value, form.modelId], loadMatchingRules)
watch(() => [visibleDialog.value, form.modelId], loadModelColumns)

const selectedModelOption = computed(() => modelOptions.value.find(item => String(item.id) === String(form.modelId)));
const responsiblePhone = computed(() => getResponsiblePhone(form.responsibleUserId));
</script>

<style lang="scss" scoped>
.el-form.column-form .h2-title {
  margin: 0;
}

.description-input :deep(.el-input__count)::after {
  content: '个字符';
}

.el-form.column-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 24px;
}

.el-form.column-form :deep(.el-form-item),
.el-form.column-form :deep(.el-form-item__content) {
  min-width: 0;
}

.el-form.column-form :deep(.el-input),
.el-form.column-form :deep(.el-select),
.el-form.column-form :deep(.el-date-editor) {
  width: 100%;
}

.result-section,
.schedule-content {
  min-width: 0;
  margin-top: 8px;
}

.result-section {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr);
}

.result-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 12px;
}

.result-hint {
  color: var(--qt-text-secondary);
  line-height: 20px;
}

.preview-table-wrap :deep(.qt-table--pagination) {
  padding: 12px 0 0;
}

.schedule-content {
  row-gap: 12px;
}

.source-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.source-tag-measure {
  position: fixed;
  display: flex;
  width: max-content;
  visibility: hidden;
  pointer-events: none;
}

.source-system-select :deep(.el-select__selection) {
  flex-wrap: nowrap;
}

.source-system-select :deep(.el-select__selected-item) {
  flex-shrink: 0;
}

.source-system-select :deep(.el-tag),
.source-tag-measure :deep(.el-tag) {
  border-color: transparent;
  background-color: transparent;
  color: var(--qt-text-primary);
}

.source-option__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  object-fit: contain;
}

.schedule-content :deep(.el-input-group__append) {
  padding: 0;
}

.schedule-content :deep(.el-input-group__append .el-button) {
  margin: 0;
  border: 0;
  border-radius: 0;
  color: var(--qt-text-inverse);
  background-color: var(--qt-primary);
}
</style>
