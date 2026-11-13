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
  <div class="app-container">
    <guide-tip tip-id="master/change/request.list" />
    <el-container>
      <qt-tree
        ref="categoryTreeRef"
        :data="categoryTreeData"
        :action-loading="categoryTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        node-key="treeKey"
        title="类目"
        placeholder="请输入主数据域或目录名称"
        @node-click="handleCategoryClick"
      />
      <el-main class="main-content">
        <qt-wrap
          :columns="tableStore.columns"
          :config="tableStore.config"
          :table-ref="tableRef"
        >
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="queryParams"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>

          <template #actions-data>
            <guide-tip-panel
              type="plain"
              title="以下展示本人提交的主数据维护申请及处理进度。"
            />
          </template>

          <qt-table ref="tableRef" v-bind="tableStore">
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(row.model)" alt="" class="info-icon" />
                  <span
                    class="info-title model-info-title text-ellipsis"
                    role="link"
                    tabindex="0"
                    @click="handleModelDetail(row)"
                    @keydown.enter="handleModelDetail(row)"
                    @keydown.space.prevent="handleModelDetail(row)"
                  >{{ getModelTitle(row) }}</span>
                  <dict-tag :options="model_type" :value="row.model?.type" size="small" type="primary" />
                  <dict-tag
                    class="ml4 pointer"
                    :options="getVersionTagOptions(row.model)"
                    :value="row.model?.version?.status"
                    size="small"
                    @click="handleModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.model?.description || '-' }}</div>
              </div>
            </template>
            <template #location="{ row }">
              <!-- 与模型定义列表一致，路径最后一项为模型直接所属目录。 -->
              <div v-if="row.model?.projectName || row.model?.layers?.length" class="flex-column flex-column-gap">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(row.model.projectIcon)" alt="" />
                  <span class="text-ellipsis" :title="row.model.projectName || ''">{{ row.model.projectName || '-' }}</span>
                </div>
                <div v-if="row.model.layers?.length" class="text-ellipsis flex-center">
                  <dict-tag size="small" type="primary" :value="row.model.layers[row.model.layers.length - 1].name" />
                </div>
              </div>
              <span v-else class="text-muted">-</span>
            </template>
            <template #applyReason="{ row }">
              {{ row.applyReason?.trim() || '-' }}
            </template>
            <template #changeType="{ row }">
              <dict-tag :value="row.changeType" :options="changeTypeOptions" />
            </template>
            <template #approvalStatus="{ row }">
              <dict-tag :value="row.approvalStatus" :options="approvalStatusOptions" />
            </template>
            <template #applicant="{ row }">
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="row.applicantName">
                  {{ row.applicantName || '-' }}
                </span>
                <span v-if="row.applicantPhone" class="desc-text">
                  {{ row.applicantPhone }}
                </span>
              </div>
            </template>
            <template #action="{ row }">
              <el-button v-if="checkPermi(['master:change:query'])" link type="primary" icon="View" @click="openDetail(row)">详情</el-button>
              <el-button
                v-if="checkPermi(['master:change:withdraw'])"
                link type="primary" :disabled="!row.canWithdraw" @click="handleWithdraw(row)"
              >
                <template #icon>
                  <svg-icon icon-class="manage-withdraw-approval" />
                </template>
                撤回
              </el-button>
              <el-button
                v-if="checkPermi(['master:change:reapply'])"
                link type="primary" :disabled="!row.canReapply || !canReapply(row)" @click="handleReapply(row)"
              >
                <template #icon>
                  <svg-icon icon-class="manage-submit-approval" />
                </template>
                重新申请
              </el-button>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
  </div>
</template>

<script setup name="MasterChangeRequests">
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

import { useRouter } from 'vue-router'
import { checkPermi } from '@/utils/permission'

import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import QtTree from '@/components/QtTree/index.vue'
import { useUserDirectory } from '@/store/system/userDirectory';
import { getModelIcon, getProjectIcon } from '@/utils/imageUrl'
import { useDict } from '@/utils/dict'
import { handleTree } from '@/utils/anivia'
import { treeProject } from '@/api/model/project/project'
import { treeLayer } from '@/api/model/layer/layer'
import { listMasterChangeRequests, reapplyMasterChangeRequest, withdrawMasterChangeRequest } from '@/api/master/change/request'

const { responsibleUserList } = useUserDirectory();

const { proxy } = getCurrentInstance()
const router = useRouter()
const { master_change_type, master_change_request_status, model_type, model_status } = useDict(
  'master_change_type', 'master_change_request_status', 'model_type', 'model_status'
)

const queryParams = reactive({
  projectId: undefined,
  layerIds: undefined,
  requestNo: undefined,
  targetName: undefined,
  changeType: undefined,
  applicantId: undefined,
  approvalStatus: undefined
})
const tableRef = ref(null)
const categoryTreeRef = ref(null)
// 保留原来的域、目录业务数据，QtTree 只负责展示和点击交互。
const categoryTreeData = ref([])
const categoryTreeLoading = ref(false)
let categoryTreeRequestSequence = 0

const applicationChangeTypes = new Set([
  'CREATE', 'UPDATE', 'BATCH_UPDATE', 'BATCH_IMPORT', 'STATUS_CHANGE', 'VERSION_RESTORE'
])
const changeTypeTagTypes = {
  CREATE: 'success',
  UPDATE: 'primary',
  BATCH_UPDATE: 'primary',
  BATCH_IMPORT: 'warning',
  STATUS_CHANGE: 'warning',
  VERSION_RESTORE: 'info'
}
const approvalStatusTagTypes = {
  0: 'warning',
  1: 'success',
  2: 'danger',
  3: 'info',
  4: 'info'
}
const changeTypeOptions = computed(() => (master_change_type.value || [])
  .filter(item => applicationChangeTypes.has(item.value))
  .map(item => ({ ...item, elTagType: changeTypeTagTypes[item.value] || 'primary' })))
const approvalStatusOptions = computed(() => (master_change_request_status.value || [])
  .map(item => ({ ...item, elTagType: approvalStatusTagTypes[item.value] || 'primary' })))

const columns = reactive([
  { prop: 'requestId', label: '编号', width: 100, align: 'center', sortable: true, sortableKey: 'id' },
  { prop: 'modelName', label: '模型信息', minWidth: 380, align: 'left', slot: 'modelInfo', showOverflowTooltip: { effect: 'light' } },
  { prop: 'modelLocation', label: '归属位置', minWidth: 200, align: 'left', slot: 'location', showOverflowTooltip: { effect: 'light' } },
  { prop: 'applyReason', label: '申请原因', minWidth: 220, align: 'left', slot: 'applyReason', showOverflowTooltip: { effect: 'light' } },
  { prop: 'changeType', label: '申请类型', minWidth: 120, align: 'left', slot: 'changeType' },
  { prop: 'approvalStatus', label: '处理进度', minWidth: 120, align: 'left', slot: 'approvalStatus' },
  { prop: 'applicantName', label: '申请人', minWidth: 140, align: 'left', slot: 'applicant' },
  { prop: 'submitTime', label: '申请时间', width: 150, date: true, sortable: true, sortableKey: 'submit_time' },
  { label: '操作', width: 280, fixed: 'right', align: 'center', slot: 'action' }
])

const searchStore = computed(() => ({ items: [
  { label: '申请编号', prop: 'requestNo', component: { is: 'input', placeholder: '请输入申请编号', maxlength: 100 } },
  { label: '目标主数据名称', prop: 'targetName', component: { is: 'input', placeholder: '请输入目标主数据名称', maxlength: 100 } },
  { label: '申请类型', prop: 'changeType', component: { is: 'select', options: changeTypeOptions.value } },
  { label: '申请人', prop: 'applicantId', component: { is: 'select', filterable: true, clearable: true, options: responsibleUserList.value } },
  { label: '处理进度', prop: 'approvalStatus', component: { is: 'select', options: approvalStatusOptions.value } }
] }))
const tableStore = reactive({
  config: {
    notPagination: false,
    notPaginationParams: true,
    table: { rowKey: 'requestId', stripe: true, tableLayout: 'fixed' },
    pagination: { pageSizes: [10, 20, 50] }
  },
  columns,
  func: async params => {
    if (!checkPermi(['master:change:list'])) return { data: { rows: [], total: 0 } }
    const response = await listMasterChangeRequests(params)
    return {
      ...response,
      data: {
        ...response.data,
        rows: (response.data?.rows || []).map(row => ({
          ...row,
          modelLocation: [row.model?.projectName, row.model?.layers?.at(-1)?.name].filter(Boolean).join(' / ') || '-'
        }))
      }
    }
  },
  params: queryParams
})

/** 加载主数据域和目录，直接交给 QtTree，保留原有搜索和布局。
 * @returns {Promise<void>} 更新仅包含域和目录的树数据。
 */
async function loadCategoryTree() {
  // 页面销毁或发起新请求后，旧请求返回也不能继续更新页面。
  const sequence = ++categoryTreeRequestSequence
  categoryTreeLoading.value = true
  try {
    const [projects, layers] = await Promise.all([
      checkPermi(['model:project:list']) ? treeProject() : Promise.resolve({ data: [] }),
      checkPermi(['model:layer:list'])
        ? treeLayer({ validFlag: true, orderByColumn: 'sortOrder', isAsc: 'ascending' })
        : Promise.resolve({ data: [] })
    ])
    // 先按所属域分开目录再组树，不能把不同域下的目录混在同一棵子树里。
    const children = (projects.data || []).map(project => ({
      ...project, treeKey: `project_${project.id}`, nodeType: 'project', projectId: project.id,
      children: handleTree((layers.data || [])
        .filter(layer => String(layer.projectId) === String(project.id))
        .map(layer => ({ ...layer, treeKey: `layer_${layer.id}`, nodeType: 'layer' })), 'id', 'parentId')
    }))
    if (sequence === categoryTreeRequestSequence) categoryTreeData.value = children
  } finally {
    if (sequence === categoryTreeRequestSequence) categoryTreeLoading.value = false
  }
}

/** 收集当前目录及全部下级目录 ID。
 * @param {Object} node 当前目录节点。
 * @returns {Array<string|number>} 用于筛选的目录 ID
 */
function getCategoryLayerIds(node) {
  return [node.id, ...(node.children || []).flatMap(getCategoryLayerIds)]
}

/** 切换类目后按当前顶部条件查询第一页。
 * @param {Object} node 点击的域或目录节点。
 */
function handleCategoryClick(node) {
  queryParams.projectId = node.projectId
  queryParams.layerIds = node.nodeType === 'layer' ? getCategoryLayerIds(node) : undefined
  handleQuery()
}

function refreshList() {
  tableRef.value?.getList()
}

function handleQuery() {
  tableRef.value?.resetQuery()
}

function resetQuery() {
  queryParams.projectId = undefined
  queryParams.layerIds = undefined
  categoryTreeRef.value?.setCurrentKey(null, false)
  queryParams.requestNo = undefined
  queryParams.targetName = undefined
  queryParams.changeType = undefined
  queryParams.applicantId = undefined
  queryParams.approvalStatus = undefined
  tableRef.value?.resetQuery()
}

function getModelTitle(row) {
  const name = row.model?.name || row.modelName
  const code = row.model?.code || row.modelCode
  return name ? (code ? `${name} (${code})` : name) : code || '-'
}

/** 与模型定义列表一致：沿用状态字典样式，标签文字显示发布版本号。 */
function getVersionTagOptions(model) {
  return model_status.value.map(item => ({
    ...item,
    label: model?.releaseVersion?.versionNo || model?.version?.versionNo || '尚未发布'
  }))
}

function handleModelDetail(row, tab) {
  const modelId = row.model?.id || row.modelId
  if (!modelId) return
  router.push({
    path: '/model/base/detail',
    query: { id: modelId, ...(tab ? { tab } : {}) }
  })
}

function openDetail(row) {
  if (!checkPermi(['master:change:query'])) return
  router.push({ name: 'masterChangeRequestDetail', params: { requestId: row.requestId } })
}

function handleWithdraw(row) {
  if (!row.canWithdraw || !checkPermi(['master:change:withdraw'])) return
  proxy.$modal.confirm(`是否确认撤回申请编号为“${row.requestNo}”的申请？`).then(() => {
    return withdrawMasterChangeRequest(row.requestId)
  }).then(() => {
    proxy.$modal.msgSuccess('撤回成功')
    refreshList()
  }).catch(() => {})
}

function canReapply(row) {
  const permission = {
    CREATE: 'master:instance:add', COPY: 'master:instance:add',
    UPDATE: 'master:instance:edit', BATCH_UPDATE: 'master:instance:edit',
    BATCH_IMPORT: 'master:instance:import', BATCH_STATUS: 'master:instance:status',
    FREEZE: 'master:instance:status', UNFREEZE: 'master:instance:status',
    INVALIDATE: 'master:instance:status', RECOVER: 'master:instance:status',
    ARCHIVE: 'master:instance:status'
  }[row.changeType]
  return typeof permission === 'string' && checkPermi(['master:change:reapply']) && checkPermi([permission])
}

function handleReapply(row) {
  if (!row.canReapply || !canReapply(row)) return
  return proxy.$modal.confirm(`确认基于申请“${row.requestNo}”重新发起吗？系统将生成新的申请编号。`).then(() => {
    return reapplyMasterChangeRequest(row.requestId)
  }).then(response => {
    proxy.$modal.msgSuccess(`重新申请成功，新申请编号：${response?.data?.requestNo || '-'}`)
    refreshList()
  }).catch(() => {})
}

onMounted(() => {
  // QtTree 使用外部数据后，由页面首次进入时主动加载，接口失败由公共请求工具提示。
  loadCategoryTree().catch(() => {})
})
// 离开页面后让未完成的目录请求失效，避免返回结果继续更新已销毁的页面。
onBeforeUnmount(() => {
  categoryTreeRequestSequence++
})
</script>

<style scoped lang="scss">
.text-muted {
  color: color-mix(in srgb, var(--qt-text-secondary) 60%, var(--qt-surface-background));
}

.model-info {
  min-width: 0;

  .model-info-title {
    display: block;
    cursor: pointer;
    color: var(--el-color-primary);
  }

  :deep(.qt-table-info__main > .dict-tag-container) {
    flex-shrink: 0;
  }
}
</style>
