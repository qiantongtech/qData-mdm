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
  <div class="column-form master-field-input">
    <el-select v-if="control === 'dictionary'" v-model="value" filterable clearable class="row-full"
               :disabled="disabled" :placeholder="`请选择${column.label}`"
    >
      <el-option v-for="option in column.referenceOptions || []" :key="option.value"
                 :label="option.label" :value="option.value"
      />
    </el-select>
    <el-select v-else-if="control === 'relation'" v-model="value" filterable remote clearable class="row-full"
               :disabled="disabled" :remote-method="search" :loading="loading" remote-show-suffix
               :placeholder="`搜索并选择${column.label}`" @visible-change="openOptions"
    >
      <el-option v-for="option in visibleOptions" :key="option.value" :label="option.label" :value="option.value" />
      <template v-if="hasMore" #footer>
        <el-button link type="primary" :loading="loading" @click="loadMore">加载更多</el-button>
      </template>
    </el-select>
    <el-tree-select v-else-if="control === 'tree'" v-model="value" :data="options" class="row-full"
                    :props="treeProps" node-key="value" filterable clearable check-strictly
                    :render-after-expand="false"
                    :disabled="disabled" :loading="loading" :placeholder="`请选择${column.label}`"
                    @visible-change="openOptions"
    />
    <el-radio-group v-else-if="control === 'boolean'" v-model="value" :disabled="disabled" class="row-full">
      <el-radio value="true">是</el-radio>
      <el-radio value="false">否</el-radio>
    </el-radio-group>
    <el-date-picker v-else-if="control === 'date' || control === 'datetime'" v-model="value" class="row-full number-input"
                    :type="control" :value-format="control === 'date' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'"
                    :disabled="disabled" :placeholder="control === 'date' ? '请选择日期' : '请选择日期时间'"
                    clearable
    />
    <el-input v-else-if="control === 'automatic'" :model-value="value" disabled class="row-full"
              placeholder="由模型编码规则生成"
    />
    <el-input v-else v-model="value" :type="control === 'textarea' ? 'textarea' : 'text'" class="row-full"
              :rows="control === 'textarea' ? 3 : undefined" :inputmode="getFieldInputMode(column)"
              :maxlength="control === 'textarea' ? '256个字符' : undefined"
              :show-word-limit="control === 'textarea'"
              :disabled="disabled || control === 'readonly'"
              :clearable="control !== 'readonly'"
              :placeholder="placeholder || `请输入${column.label}`"
    />
    <el-text v-if="control === 'dictionary' && !column.referenceOptions?.length" type="info" size="small" class="row-full">
      当前发布配置暂无可用参考数据
    </el-text>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { getMasterReferenceModel, getMasterData, getMasterClassificationOptions } from '@/api/master/data/data'

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
  const dataType = getFieldDataType(column)
  if (/^(BOOL|BOOLEAN)$/.test(dataType)) return 'boolean'
  if (dataType === 'DATE') return 'date'
  if (/^(DATETIME|TIMESTAMP)$/.test(dataType)) return 'datetime'
  if (/TEXT|CLOB/.test(dataType)) return 'textarea'
  if (isIntegerColumn(column)) return 'integer'
  if (isDecimalColumn(column)) return 'decimal'
  return 'text'
}

function isIntegerColumn(column) { return /^(BIGINT|INTEGER|SMALLINT|INT|TINYINT)$/.test(getFieldDataType(column)) }

function isDecimalColumn(column) { return /^(DECIMAL|NUMBER|NUMERIC|FLOAT|DOUBLE|REAL)$/.test(getFieldDataType(column)) }

function getFieldInputMode(column) {
  if (isIntegerColumn(column)) return 'numeric'
  if (isDecimalColumn(column)) return 'decimal'
  return undefined
}

function getRecordValue(row, code) {
  if (!row || !code) return undefined
  const key = Object.keys(row).find(name => name.toUpperCase() === String(code).toUpperCase())
  return key == null ? undefined : row[key]
}

// 将生效记录转换为关联选项，并检查关联键唯一性。
function buildRelationOptions(rows, column) {
  const seen = new Set()
  return rows.filter(row => String(getRecordValue(row, 'STATUS')) === '1').map(row => {
    const value = getRecordValue(row, column.relatedCode)
    const label = getRecordValue(row, column.displayCode)
    if (value == null || label == null) throw new Error('关联模型未返回配置的关联键或展示字段')
    if (seen.has(String(value))) throw new Error('关联键存在重复数据，无法选择')
    seen.add(String(value))
    return { value: String(value), label: String(label) }
  })
}

function buildClassificationOptions(nodes) {
  return (nodes || []).map(node => ({
    key: String(node.id), value: String(node.value), label: node.label,
    disabled: !node.assignable, children: buildClassificationOptions(node.children)
  }))
}

const props = defineProps({
  modelValue: { type: [String, Number, Boolean], default: '' },
  column: { type: Object, required: true },
  placeholder: { type: String, default: '' },
  disabled: Boolean
})
const emit = defineEmits(['update:modelValue', 'loadError'])
const value = computed({ get: () => props.modelValue == null ? '' : String(props.modelValue),
  set: next => emit('update:modelValue', next == null ? '' : String(next)) })
const control = computed(() => getFieldControl(props.column))
const options = ref([])
const loading = ref(false)
const error = ref('')
const hasMore = ref(false)
const selected = ref(null)
const treeProps = { value: 'value', label: 'label', children: 'children', disabled: 'disabled' }
const visibleOptions = computed(() => {
  if (!value.value || options.value.some(option => option.value === value.value)) return options.value
  const current = selected.value?.value === value.value ? selected.value : { value: value.value, label: `${value.value}（当前值）` }
  return [current, ...options.value]
})
let serial = 0
let published = null
let keyword = ''
let page = 1

async function load(query = '', nextPage = 1) {
  const expected = ++serial
  loading.value = true
  error.value = ''
  try {
    const column = props.column
    if (!column.relatedModelId || !column.relatedCode || !column.displayCode) {
      throw new Error('关联字段配置不完整，请检查已发布模型')
    }
    const model = (control.value === 'tree' ? null : published) || (await getMasterReferenceModel(column.relatedModelId))?.data
    if (expected !== serial) return
    if (!model?.instanceId || !model.versionId || String(model.id) !== String(column.relatedModelId)) {
      throw new Error('关联模型尚未成功发布')
    }
    published = model
    const params = { modelId: model.id, modelVersionId: model.versionId, status: '1', pageNum: nextPage, pageSize: 50 }
    if (control.value === 'tree') {
      if (model.type !== 'CATEGORY') throw new Error('关联目标不是分类模型')
      const response = await getMasterClassificationOptions(model.instanceId, {
        modelId: model.id, modelVersionId: model.versionId, relatedCode: column.relatedCode,
        displayCode: column.displayCode, nodeRule: column.nodeRule || undefined, displayMode: column.displayMode || undefined
      })
      if (expected !== serial) return
      options.value = buildClassificationOptions(response?.data?.nodes || [])
      hasMore.value = false
    } else {
      const response = await getMasterData(model.instanceId, { ...params, name: query || undefined })
      if (expected !== serial) return
      const rows = response?.data?.rows || []
      const loaded = buildRelationOptions(rows, column)
      const combined = nextPage === 1 ? loaded : [...options.value, ...loaded]
      options.value = [...new Map(combined.map(option => [option.value, option])).values()]
      hasMore.value = nextPage * 50 < Number(response?.data?.total || 0)
      keyword = query
      page = nextPage
    }
  } catch (cause) {
    if (expected !== serial) return
    options.value = []
    hasMore.value = false
    error.value = cause?.msg || cause?.message || '选项加载失败，请稍后重新展开选择器'
  } finally {
    if (expected === serial) loading.value = false
  }
}

function search(query) { return load(query, 1) }
function loadMore() { if (!loading.value) return load(keyword, page + 1) }
function openOptions(open) {
  if (open && !loading.value) return load(control.value === 'tree' ? '' : keyword, 1)
}

watch(error, message => emit('loadError', message), { immediate: true })
watch(value, next => {
  if (control.value === 'relation') selected.value = options.value.find(option => option.value === next) || null
  // 复制/修改时的值可能在字段挂载后才回填，无需展开下拉框就加载分类名称。
  if (control.value === 'tree' && next && !options.value.length && !loading.value) load()
})
watch(() => [props.column.prop, props.column.relatedModelId, props.column.relatedCode, props.column.displayCode,
  props.column.nodeRule, props.column.displayMode], () => {
  serial++
  published = null
  options.value = []
  selected.value = null
  error.value = ''
  loading.value = false
  hasMore.value = false
  keyword = ''
  page = 1
  if (control.value === 'tree' && value.value) load()
}, { immediate: true })
onBeforeUnmount(() => { serial++ })
</script>

<style scoped lang="scss">
.master-field-input {
  width: 100%;
  min-width: 0;
  row-gap: 4px;
}
</style>

