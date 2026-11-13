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
  匹配审核只读详情，展示候选组基本信息及来源数据。
  @author chentangwen
-->
<template>
  <div v-loading="loading" class="app-container">
    <el-empty v-if="errorMessage" :description="errorMessage">
      <el-button type="primary" icon="Refresh" @click="loadDetail(route.query.id)">重新加载</el-button>
      <el-button icon="Back" @click="router.push('/merge/review')">返回列表</el-button>
    </el-empty>
    <template v-else-if="detail">
      <detail-info
        :data="headerData"
        :header="headerConfig"
        :items="detailItems"
        mode="free"
        @back="router.push('/merge/review')"
      >
        <template #model>
          <div class="qt-table-info review-detail-model">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(model)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="modelTitle">{{ modelTitle }}</span>
              <dict-tag
                v-if="model?.type"
                class="model-tag"
                :options="MODEL_TYPE_OPTIONS"
                :value="model.type"
                size="small"
                type="primary"
              />
              <dict-tag
                v-if="detail.modelVersionNo"
                class="model-tag"
                :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: detail.modelVersionNo }))"
                :value="detail.modelVersionStatus"
                size="small"
              />
            </div>
          </div>
        </template>
        <template #scope>
          <dict-tag v-if="detail.scopeType" type="primary">{{ getScopeText(detail.scopeType) }}</dict-tag>
          <span v-else>-</span>
        </template>
      </detail-info>
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab">
          <qt-tab-pane label="来源数据" name="members">
            <qt-wrap :columns="memberTable.columns" :table-ref="memberTableRef" :config="wrapConfig">
              <template #actions-data>
                <guide-tip-panel
                  type="plain"
                  title="查看当前候选组的来源记录，对比各来源系统的业务字段与数据差异，结合成员角色了解参与匹配的数据及历史比较记录。"
                />
              </template>
              <qt-table ref="memberTableRef" v-bind="memberTable" :columns="memberTable.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
                <template #sourceSystem="{ row }">
                  <div class="qt-table-info">
                    <div class="qt-table-info__main">
                      <img :src="getProjectIcon(row.sourceSystem.icon)" alt="" class="info-icon" />
                      <span class="info-title text-ellipsis" :title="row.sourceSystem.title">{{ row.sourceSystem.title }}</span>
                    </div>
                    <div class="info-desc text-ellipsis" :title="row.sourceSystem.description">{{ row.sourceSystem.description || '-' }}</div>
                  </div>
                </template>
                <template #memberValue="{ row, column_data }">
                  {{ formatMemberValue(row[(column_data.property || column_data.prop)]) }}
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>
</template>

<script setup name="mergeReviewDetail">
import { useDict } from '@/utils/dict'
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMergeReview } from '@/api/merge/review'
import { getBase } from '@/api/model/base/base'
import { listIngestSource } from '@/api/ingest/source/source'
import { getProjectIcon, getModelIcon } from '@/utils/imageUrl'
import { MODEL_TYPE_OPTIONS } from '@/utils/model/modelType'
import { MODEL_STATUS_OPTIONS } from '@/utils/model/modelStatus'
import DetailInfo from '@/components/DetailInfo/index.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const detail = ref(null)
const model = ref(null)
const sourceSystems = ref([])
const activeTab = ref('members')
const memberTableRef = ref()
let requestVersion = 0

const { merge_group_status: GROUP_STATUS_OPTIONS } = useDict('merge_group_status')
const headerConfig = computed(() => ({
  idKey: 'id', nameKey: 'taskName', statusKey: 'status', statusOptions: GROUP_STATUS_OPTIONS.value, routerBack: false,
}))
const headerData = computed(() => ({
  ...detail.value,
  taskName: detail.value?.taskName || detail.value?.groupNo || '-',
}))
const wrapConfig = { fullContent: false, actions: { table: { search: false } } }
const modelTitle = computed(() => {
  const name = model.value?.name || detail.value?.modelId || '-'
  return model.value?.code ? `${name} (${model.value.code})` : String(name)
})
const detailItems = computed(() => [
  { label: '主数据模型', slot: 'model' },
  { label: '检测范围', slot: 'scope' },
  { label: '来源数据', value: formatCount(detail.value?.sourceCount, '条') },
  { label: '描述', value: detail.value?.remark || '-', span: 24 },
  { label: '最高匹配分', value: formatScore(detail.value?.maxMatchScore) },
  { label: '命中规则', value: formatCount(detail.value?.ruleCount, '条') },
  { label: '转入时间', key: 'transferTime', type: 'time' },
])

const memberTable = reactive({
  func: () => Promise.resolve({ data: (detail.value?.members || []).map((member, index) => ({
    ...member,
    sourceIndex: index + 1,
    systemName: member.systemName || member.systemCode || '-',
    sourceSystem: getSourceSystemInfo(member),
    stableRecordId: member.stableRecordId || member.sourceRecordId || '-',
    memberRoleLabel: String(member.memberRole) === '2' ? '历史比较' : '待处理',
    ...Object.fromEntries(Object.entries(member.values || {}).map(([key, value]) => [`field_${key}`, value])),
  })) }),
  config: {
    autoPagination: true,
    table: { rowKey: 'memberId', stripe: true },
    pagination: { pageSizes: [10, 20, 50], notAutoScroll: true },
  },
  columns: [],
})

function formatScore(value) {
  return value === null || value === undefined || value === '' ? '-' : `${value}分`
}

function formatMemberValue(value) {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'string' && (!value.trim() || value === 'null')) return '-'
  return value
}

function formatCount(value, unit) {
  return value === null || value === undefined || value === '' ? '-' : `${value} ${unit}`
}

function getScopeText(scopeType) {
  if (String(scopeType) === '1') return '全部待治理来源数据'
  if (String(scopeType) === '2') return '指定范围'
  return scopeType ? `范围${scopeType}` : '-'
}

function getSourceSystemInfo(member) {
  const system = sourceSystems.value.find(item => member.systemCode && item.code === member.systemCode)
    || sourceSystems.value.find(item => member.systemName && item.name === member.systemName)
    || {}
  const name = member.systemName || system.name
  const code = member.systemCode || system.code
  return {
    icon: system.icon,
    title: name ? `${name}${code ? ` (${code})` : ''}` : code || '-',
    description: system.description,
  }
}

function buildMemberColumns() {
  const fields = new Map()
  const fieldIds = new Set()
  ;(model.value?.fields || []).forEach(field => {
    const code = field.columnCode || field.code
    if (code) {
      fields.set(code, field.columnName || field.name || code)
      if (field.id !== null && field.id !== undefined) fieldIds.add(String(field.id))
    }
  })
  // 快照同时按字段编码和 ID 索引
  ;(detail.value?.members || []).forEach(member => {
    Object.keys(member.values || {}).forEach(code => {
      if (!fieldIds.has(code) && !fields.has(code)) fields.set(code, code)
    })
  })
  return [
    { label: '编号', prop: 'sourceIndex', width: 70 },
    { label: '来源系统', prop: 'systemName', slot: 'sourceSystem', minWidth: 280, align: 'left' },
    { label: '来源记录ID', prop: 'stableRecordId', minWidth: 180, align: 'left' },
    { label: '成员角色', prop: 'memberRoleLabel', minWidth: 110 },
    ...[...fields].map(([code, label]) => ({
      label, prop: `field_${code}`, slot: 'memberValue', minWidth: 160, align: 'left', showOverflowTooltip: true,
    })),
  ]
}

async function loadDetail(id) {
  const version = ++requestVersion
  detail.value = null
  model.value = null
  sourceSystems.value = []
  activeTab.value = 'members'
  errorMessage.value = ''
  loading.value = false
  if (typeof id !== 'string' || !/^[1-9]\d*$/.test(id)) {
    errorMessage.value = '缺少有效的候选组编号，请从匹配审核列表进入详情。'
    return
  }
  loading.value = true
  try {
    const response = await getMergeReview(id)
    if (version !== requestVersion) return
    if (!response.data) {
      errorMessage.value = '候选组不存在或已失效。'
      return
    }
    const [modelResult, systemsResult] = await Promise.allSettled([
      getBase(response.data.modelId),
      listIngestSource({ pageNum: 1, pageSize: -1 }),
    ])
    if (version !== requestVersion) return
    // 辅助信息加载失败时保留来源快照，图标使用默认值。
    model.value = modelResult.status === 'fulfilled' ? modelResult.value.data : null
    if (systemsResult.status === 'fulfilled') {
      sourceSystems.value = systemsResult.value.data?.rows || systemsResult.value.data || []
    }
    detail.value = response.data
    memberTable.columns = buildMemberColumns()
  } catch {
    if (version === requestVersion) errorMessage.value = '匹配审核详情加载失败，请重试。'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => route.query.id, loadDetail, { immediate: true })
</script>

<style scoped>
.review-detail-model {
  min-width: 0;
  max-width: 100%;
}

.review-detail-model .qt-table-info__main {
  flex-wrap: nowrap;
  margin-bottom: 0;
}

.review-detail-model .info-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-detail-model .model-tag {
  flex-shrink: 0;
}
</style>
