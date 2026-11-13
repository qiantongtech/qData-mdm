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
  查看合并结果的审批信息及提交或生效后的主数据字段。
  @prop modelOptions 主数据模型选项。
  @author chentangwen
-->
<template>
  <div ref="app-container">
    <el-dialog
      v-model="visible"
      :title="isEffective ? '查看结果' : '查看审批'"
      width="min(1000px, 94vw)"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
      @closed="resetDialog"
    >
      <div v-loading="loading">
        <el-empty v-if="loadError" description="审批详情加载失败">
          <el-button type="primary" plain @click="loadApprovalDetail">重新加载</el-button>
        </el-empty>
        <template v-else-if="approvalDetail">
          <section class="approval-view-section">
            <el-form label-width="140px" class="column-form approval-view-form">
              <div class="h2-titles row-full">基本信息</div>
              <el-form-item
                v-for="item in basicItems"
                :key="item.label"
                :label="item.label"
              >
                <el-tag v-if="item.label === '处理状态'" size="small" :type="getOptionType(PENDING_STATUS_OPTIONS, item.status)">
                  {{ displayValue(item.value) }}
                </el-tag>
                <div v-else :class="['form-readonly', 'approval-view-value', { 'approval-view-value-singleline': item.label === '来源系统' }]">
                  <el-tooltip
                    v-if="item.label === '来源系统'"
                    :content="displayValue(item.value)"
                    placement="top"
                  >
                    <span class="approval-source-system-text">{{ displayValue(item.value) }}</span>
                  </el-tooltip>
                  <span v-else>{{ displayValue(item.value) }}</span>
                </div>
              </el-form-item>
            </el-form>
          </section>

          <section class="approval-view-section">
            <el-form label-width="140px" class="column-form approval-view-form">
              <div class="h2-titles row-full">{{ isEffective ? '生效信息' : '审批信息' }}</div>
              <el-form-item
                v-for="item in approvalItems"
                :key="item.label"
                :label="item.label"
              >
                <div class="form-readonly approval-view-value">
                  <span>{{ displayValue(item.value) }}</span>
                </div>
              </el-form-item>
            </el-form>
          </section>

          <section class="approval-view-section">
            <el-form label-width="140px" class="column-form approval-view-form">
              <div class="h2-titles row-full">{{ resultTitle }}</div>
              <el-empty v-if="!resultFields.length" description="暂无主数据结果" :image-size="72" class="row-full" />
              <el-form-item
                v-for="field in resultFields"
                :key="field.id || field.fieldPath"
                :label="field.columnName || field.fieldPath"
              >
                <div class="form-readonly approval-view-value">
                  <span>{{ displayValue(field.finalValue) }}</span>
                </div>
              </el-form-item>
            </el-form>
          </section>
        </template>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MergeApprovalViewDialog">
import { useDict } from '@/utils/dict'
import { computed, getCurrentInstance, ref } from 'vue'
import { getMergePending } from '@/api/merge/manage'

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

function formatCountText(value, unit) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} ${unit}`
}

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

function getOptionType(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.elTagType || 'info'
}

const { merge_pending_status: PENDING_STATUS_OPTIONS } = useDict('merge_pending_status')

function getModelName(models, modelId) {
  const found = (models || []).find((item) => String(item.value) === String(modelId))
  return found?.label || (modelId ? `模型${modelId}` : '—')
}

const props = defineProps({
  modelOptions: { type: Array, default: () => [] },
})

const { proxy } = getCurrentInstance()
const visible = ref(false)
const loading = ref(false)
const loadError = ref(false)
const selectedRow = ref(null)
const approvalDetail = ref(null)
let requestVersion = 0

const isEffective = computed(() => String(approvalDetail.value?.status ?? selectedRow.value?.status) === '9')

/** 从来源候选记录中汇总系统编码，弹窗不展示系统中文名称。 */
const sourceSystemText = computed(() => {
  const codes = (approvalDetail.value?.fields || [])
    .flatMap(field => field.candidates || [])
    .map(candidate => candidate.systemCode)
    .filter(Boolean)
  return [...new Set(codes)].join(' / ') || '-'
})

/** 按列表摘要和审批详情接口组装弹窗中的三组只读字段。 */
const basicItems = computed(() => {
  const row = selectedRow.value || {}
  const detail = approvalDetail.value || {}
  return [
    { label: '业务名称', value: row.businessName },
    { label: '主数据模型', value: row.modelName || getModelName(props.modelOptions, detail.modelId || row.modelId) },
    { label: '来源系统', value: sourceSystemText.value },
    { label: '来源数据', value: formatCountText(row.sourceCount, '条') },
    { label: '处理状态', value: getOptionLabel(PENDING_STATUS_OPTIONS.value, detail.status || row.status), status: detail.status || row.status },
    { label: '最近更新时间', value: formatTime(detail.updateTime || row.updateTime) },
  ]
})
const approvalItems = computed(() => {
  const detail = approvalDetail.value || {}
  const row = selectedRow.value || {}
  const automaticField = resultFields.value.find(field => field.automaticEncoding === true)
  const changeType = detail.targetRecordId != null ? '更新主数据' : '新增主数据'
  if (isEffective.value) {
    return [
      { label: '审批实例', value: detail.flowInstanceId },
      { label: '权威主数据ID', value: detail.resultRecordId },
      { label: '生效方式', value: changeType },
      { label: '正式版本', value: detail.effectiveVersionNo },
      { label: '自动编码', value: automaticField?.finalValue },
      { label: '来源映射', value: detail.sourceMappingCount == null ? null : `${detail.sourceMappingCount}条来源数据已关联` },
    ]
  }
  return [
    { label: '审批实例', value: detail.flowInstanceId },
    { label: '申请类型', value: changeType },
    { label: '审批对象', value: detail.groupNo || row.groupNo || row.mergeRecordId || row.id },
    { label: '自动编码', value: automaticField?.finalValue || (automaticField ? '审批通过后生成' : null) },
  ]
})
const resultFields = computed(() => {
  const detail = approvalDetail.value || {}
  const fields = (detail.fields || []).filter(field => String(field.columnCategory || '').toUpperCase() !== 'SYSTEM')
  if (!isEffective.value) return fields
  const values = detail.effectiveData || {}
  // 生效后仅展示本次正式版本，避免把提交时尚未生成的编码当成最终结果。
  return fields.map(field => ({ ...field, finalValue: values[field.fieldPath] }))
})
const resultTitle = computed(() => isEffective.value ? '最终主数据结果' : '提交的主数据结果')

/**
 * 使用项目统一时间格式展示列表更新时间。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') || '-' : '-'
}

/**
 * 将空字段统一显示为项目详情页使用的占位符。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {*} 当前业务计算结果。
 */
function displayValue(value) {
  return displayText(value)
}

/**
 * 读取选中记录的审批详情并维护请求序号，避免快速切换时旧请求覆盖新记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadApprovalDetail() {
  const id = selectedRow.value?.id
  if (id == null) return
  const currentVersion = ++requestVersion
  loading.value = true
  loadError.value = false
  approvalDetail.value = null
  try {
    const response = await getMergePending(id)
    if (currentVersion !== requestVersion || !visible.value) return
    approvalDetail.value = response?.data || null
    if (!approvalDetail.value) loadError.value = true
  } catch {
    if (currentVersion === requestVersion) loadError.value = true
  } finally {
    if (currentVersion === requestVersion) loading.value = false
  }
}

/**
 * 打开审批详情弹窗并加载当前记录的完整字段结果。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function open(row) {
  requestVersion++
  selectedRow.value = { ...row }
  approvalDetail.value = null
  loadError.value = false
  visible.value = true
  loadApprovalDetail()
}

/**
 * 关闭后清除数据，下一次打开时重新读取最新审批状态。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function resetDialog() {
  requestVersion++
  selectedRow.value = null
  approvalDetail.value = null
  loading.value = false
  loadError.value = false
}

defineExpose({ open })
</script>

<style scoped>
.approval-view-section + .approval-view-section {
  margin-top: var(--qt-space-4);
}

.approval-view-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.approval-view-form :deep(.el-form-item),
.approval-view-form :deep(.el-form-item__content) {
  min-width: 0;
}

.approval-view-value {
  min-width: 0;
}

.approval-view-value-singleline:not(.textarea) {
  overflow: hidden;
  white-space: nowrap;
}

.approval-view-value-singleline:not(.textarea) :deep(.el-tooltip__trigger) {
  display: block;
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
}

.approval-source-system-text {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
