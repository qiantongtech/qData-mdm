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
  <div class="basicInfo">
    <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
      <template #actions-data>
        <div class="h2-titles">业务字段</div>
      </template>
      <DescriptionsInfo
        v-if="businessFieldItems.length"
        :data="row"
        :items="businessFieldItems"
        :column="2"
        :border="true"
      />
      <div v-else class="emptyBg">
        <img src="@/assets/common/images/empty-state/empty-nodata.png" alt="" />
        <p>暂无业务字段</p>
      </div>
    </qt-wrap>

    <div v-loading="submodelLoading">
      <el-empty v-if="submodelError" :description="submodelError" />
      <template v-else>
        <section v-for="table in submodelTables" :key="table.model.id" class="mt15">
          <qt-wrap :columns="table.columns" :table-ref="table.tableRef" :config="table.config">
            <template #actions-data>
              <div class="h2-titles">{{ table.model.name || table.model.code || table.model.id }}</div>
            </template>
            <el-empty v-if="!table.model.available"
                      :description="table.model.message || '子模型物化配置不可用'"
            />
            <template v-else>
              <el-alert v-if="table.error" :title="table.error" type="error" :closable="false" show-icon class="mb15" />
              <qt-table :ref="el => table.tableRef = el" :columns="table.columns" :config="table.config"
                        :func="table.func" :params="table.params"
              >
                <template #action="{ row }">
                  <el-button link type="primary" icon="View" @click="loadSubmodelRecord(table.model.id, row)">详情</el-button>
                </template>
              </qt-table>
            </template>
          </qt-wrap>
        </section>
      </template>

      <div ref="dialogContainer">
        <el-dialog v-if="dialogContainer" v-model="submodelRecordVisible"
                   :title="submodelRecordData?.submodelName || '子记录详情'"
                   width="800px" append-to-body :append-to="dialogContainer"
                   draggable destroy-on-close class="dialog"
        >
          <div v-loading="submodelRecordLoading">
            <el-empty v-if="submodelRecordError" :description="submodelRecordError" />
            <el-form v-else-if="submodelRecordData?.row" :model="submodelRecordData.row"
                     label-width="120px" class="column-form"
            >
              <qt-form-item v-for="column in submodelRecordColumns" :key="column.prop"
                            :label="column.label" :class="{ 'row-full': isLongText(column) }"
              >
                <div class="form-readonly" :class="{ textarea: isLongText(column) }">{{ displayValue(submodelRecordData.row[column.prop], column) }}</div>
              </qt-form-item>
            </el-form>
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="submodelRecordVisible = false">关 闭</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </div>

    <qt-wrap class="mt15" :config="{ fullContent: false, actions: { table: { show: false } } }">
      <template #actions-data>
        <div class="h2-titles">系统字段</div>
      </template>
      <DescriptionsInfo
        v-if="systemFieldItems.length"
        :data="row"
        :items="systemFieldItems"
        :column="2"
        :border="true"
      />
      <div v-else class="emptyBg">
        <img src="@/assets/common/images/empty-state/empty-nodata.png" alt="" />
        <p>暂无系统字段</p>
      </div>
    </qt-wrap>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, watch, onUnmounted } from 'vue'
import { getMasterSubmodels, getMasterSubmodelRecord } from '@/api/master/data/data'
import { parseTime } from '@/utils/anivia.js'
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue'
import { useDict } from '@/utils/dict'

const { proxy } = getCurrentInstance()
const { sys_yes_no } = useDict('sys_yes_no')

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], default: null },
  detail: { type: Object, default: null },
  statusOptions: { type: Array, default: () => [] },
  approvalStatusOptions: { type: Array, default: () => [] }
})

const row = computed(() => props.detail?.row || {})
const businessFieldItems = computed(() => (props.detail?.businessColumns || []).map(column => ({
  label: column.label,
  value: row.value[column.prop],
  formatter: value => displayValue(value, column)
})))
const systemFieldItems = computed(() => (props.detail?.systemColumns || []).map(column => ({
  label: column.label,
  value: row.value[column.prop],
  formatter: value => displayValue(value, column)
})))

const submodelLoading = ref(false)
const submodelError = ref('')
const submodelTables = ref([])

let submodelRequest = 0
const submodelRecordVisible = ref(false)
const submodelRecordLoading = ref(false)
const submodelRecordError = ref('')
const submodelRecordData = ref(null)
const dialogContainer = ref(null)
const hiddenRecordFields = new Set(['CREATED_BY_ID', 'UPDATED_BY_ID'])
const submodelRecordColumns = computed(() => {
  const columns = (submodelRecordData.value?.columns || [])
    .filter(column => !hiddenRecordFields.has(String(column?.prop || '').toUpperCase()))
    .map(column => String(column.prop).toUpperCase() === 'DEL_FLAG'
      ? { ...column, label: '是否删除', dict: 'sys_yes_no' } : column)
  return [...columns.filter(column => !isLongText(column)), ...columns.filter(isLongText)]
})

let submodelRecordRequest = 0

watch(() => props.modelValue, (val) => {
  if (!val) {
    submodelRequest++
    submodelRecordRequest++
    submodelTables.value = []
    submodelError.value = ''
    submodelLoading.value = false
    submodelRecordVisible.value = false
    submodelRecordData.value = null
    submodelRecordError.value = ''
    submodelRecordLoading.value = false
  }
})

const loadTrigger = computed(() => {
  if (!props.modelValue || !props.detail?.row?.ID || !props.instanceId) {
    return null
  }
  return `${props.instanceId}_${props.detail.row.ID}`
})

watch(loadTrigger, (val) => {
  if (val) loadSubmodels()
}, { immediate: true })

onUnmounted(() => {
  submodelRequest++
  submodelRecordRequest++
})

watch(submodelRecordVisible, visible => {
  if (!visible) {
    submodelRecordRequest++
    submodelRecordLoading.value = false
  }
})

async function loadSubmodels() {
  const recordId = props.detail?.row?.ID
  if (!props.modelValue || !props.instanceId || !recordId) return
  const request = ++submodelRequest
  submodelRecordRequest++
  submodelRecordVisible.value = false
  submodelTables.value = []
  const instanceId = props.instanceId
  submodelError.value = ''
  submodelLoading.value = true
  try {
    const response = await getMasterSubmodels(instanceId, recordId, { pageNum: 1, pageSize: 10 })
    if (request !== submodelRequest || !props.modelValue || String(instanceId) !== String(props.instanceId)) return
    submodelTables.value = (response?.data?.submodels || [])
      .map(model => createSubmodelTable(model, instanceId, recordId, request))
  } catch {
    if (request === submodelRequest) submodelError.value = '子模型数据加载失败，请检查子模型物化配置。'
  } finally {
    if (request === submodelRequest) submodelLoading.value = false
  }
}

function createSubmodelTable(model, instanceId, recordId, request) {
  const table = reactive({
    model,
    tableRef: null,
    error: '',
    config: {
      fullContent: false,
      actions: { table: { search: false, show: model.available } },
      sort: true,
      notPagination: true,
      table: { maxHeight: 400, defaultSort: { prop: 'ID', order: 'descending' } }
    },
    columns: [],
    params: { submodelId: model.id },
    func: async params => {
      if (request !== submodelRequest || !props.modelValue) {
        return { data: { rows: [], total: 0 } }
      }
      table.error = ''
      try {
        const rows = []
        let columns = []
        let total = 0
        let pageNum = 1
        // 接口单页最多 100 条，取齐关联子记录后以无分页列表展示。
        do {
          const response = await getMasterSubmodels(instanceId, recordId,
            { ...params, pageNum, pageSize: 100 })
          if (request !== submodelRequest || !props.modelValue) {
            return { data: { rows: [], total: 0 } }
          }
          const data = response?.data || {}
          if (pageNum === 1) {
            columns = data.columns || []
            total = Number(data.total || 0)
          }
          const pageRows = data.rows || []
          if (!pageRows.length && rows.length < total) {
            proxy.$modal.msgWarning('子模型记录未完整返回')
            throw new Error('子模型记录未完整返回')
          }
          rows.push(...pageRows)
          pageNum++
        } while (rows.length < total)
        table.columns = [
          ...buildTableColumns(columns),
          { label: '操作', prop: 'action', width: 90, fixed: 'right', align: 'center', slot: 'action' }
        ]
        return { data: { rows, total: rows.length } }
      } catch (error) {
        if (request === submodelRequest) table.error = '子模型数据加载失败，请稍后重试。'
        throw error
      }
    }
  })
  return table
}

async function loadSubmodelRecord(submodelId, record) {
  const recordId = props.detail?.row?.ID
  if (!props.modelValue || !props.instanceId || !recordId || !submodelId || !record?.ID) return
  const request = ++submodelRecordRequest
  const instanceId = props.instanceId
  submodelRecordVisible.value = true
  submodelRecordLoading.value = true
  submodelRecordData.value = null
  submodelRecordError.value = ''
  try {
    const response = await getMasterSubmodelRecord(instanceId, recordId, submodelId, record.ID)
    if (request !== submodelRecordRequest || !props.modelValue || !submodelRecordVisible.value
        || String(instanceId) !== String(props.instanceId)
        || String(recordId) !== String(props.detail?.row?.ID)) return
    submodelRecordData.value = response?.data || null
  } catch {
    if (request === submodelRecordRequest) submodelRecordError.value = '子记录详情加载失败，请稍后重试。'
  } finally {
    if (request === submodelRecordRequest) submodelRecordLoading.value = false
  }
}

function hasValue(value) {
  return value !== undefined && value !== null && value !== ''
}

function optionLabel(options, value) {
  if (!hasValue(value)) return '-'
  return options.find(option => String(option.value) === String(value))?.label || String(value)
}

function formationLabel(value) {
  if (!hasValue(value)) return '-'
  return { MANUAL: '平台录入', MERGE: '多源融合', IMPORT: '批量导入' }[value] || value
}

function buildTableColumns(columns) {
  const idColumn = columns.find(column => String(column.prop).toUpperCase() === 'ID')
  const orderedColumns = idColumn
    ? [idColumn, ...columns.filter(column => String(column.prop).toUpperCase() !== 'ID')]
    : columns
  return orderedColumns.map(column => {
    const isId = String(column.prop).toUpperCase() === 'ID'
    return {
      label: isId ? '编号' : column.label,
      prop: column.prop,
      width: isId ? 60 : undefined,
      minWidth: isId ? undefined : 140,
      align: isId ? 'center' : 'left',
      sortable: isId,
      showOverflowTooltip: { effect: 'light' },
      formatter: row => displayValue(row[column.prop], column)
    }
  })
}

function isLongText(column) {
  return /TEXT|CLOB|JSON/i.test(column?.dataType || '')
    || (!column?.dataType && ['REMARK', 'DESCRIPTION'].includes(String(column?.prop || '').toUpperCase()))
}

function displayValue(value, column) {
  if (!hasValue(value)) return '-'
  if (String(column.prop).toUpperCase() === 'MASTER_VERSION_NO') return `V${String(value).replace(/^v/i, '')}`
  if (String(column.prop).toUpperCase() === 'DEL_FLAG') {
    const flag = { '0': 'N', false: 'N', '1': 'Y', true: 'Y' }[String(value).toLowerCase()]
    return flag ? optionLabel(sys_yes_no.value, flag) : value
  }
  if (column.dict === 'master_data_status') return optionLabel(props.statusOptions, value)
  if (column.dict === 'master_change_request_status') return optionLabel(props.approvalStatusOptions, value)
  if (column.prop === 'DATA_SOURCE') return formationLabel(value)
  return /TIME/i.test(column.dataType || '') ? parseTime(value) || value : value
}
</script>
