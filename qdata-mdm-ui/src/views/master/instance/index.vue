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
  @file master/instance/index.vue
  @description 权威主数据入口，提供模型筛选、统计卡片和模型信息查看；无 props、emits。
  @author chentangwen
  @created 2026-09-29
  @lastModified 2026-10-29
-->
<template>
  <div ref="app-container" class="app-container master-authority-page">
    <guide-tip tip-id="master/instance.list" />
    <el-container>
      <qt-tree
        v-if="checkPermi(['model:base:list'])"
        ref="modelTreeRef"
        wrap-height="auto"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :use-builtin-cat-edit-dialog="false"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @tree-change="handleModelTreeChange"
        @node-click="handleTreeClick"
      />
      <el-main class="main-content">
        <qt-wrap :config="{ actions: { show: false } }">
          <template #search>
            <qt-search-bar
              v-bind="searchStore"
              :params="queryParams"
              @query="handleQuery"
              @reset="resetQuery"
            />
          </template>

          <section v-loading="loading" class="master-entry-panel">
            <div
              class="master-entry-grid-wrap"
              :class="{ 'master-entry-grid-wrap--full': cards.length === queryParams.pageSize }"
            >
              <div v-if="cards.length" class="master-entry-grid">
                <article
                  v-for="item in cards"
                  :key="item.instanceId"
                  tabindex="0"
                  @click="handleEnter(item)"
                  @keyup.enter.self="handleEnter(item)"
                  class="master-entry-card"
                >
                  <div class="master-entry-card__top">
                    <div class="master-entry-card__icon">
                      <img :src="getDefaultModelIcon(item.modelType)" alt="" />
                    </div>
                    <h3 :title="item.modelName">
                      <span
                        v-if="getDisplayTitle(item.modelName) && $auth.hasPermi('model:base:query')"
                        role="link" tabindex="0" class="pointer"
                        :title="String(item.modelName)"
                        @click.stop="handleModelInfo(item)"
                        @keydown.enter.stop.prevent="handleModelInfo(item)"
                        @keydown.space.stop.prevent="handleModelInfo(item)"
                      >{{ item.modelName }}</span>
                      <span v-else :title="getDisplayTitle(item.modelName)">{{ getDisplayTitle(item.modelName) ?? '-' }}</span>
                    </h3>
                    <dict-tag
                      :options="model_type"
                      :value="item.modelType"
                      size="default"
                      class="master-entry-card__type"
                    />
                    <div
                      :title="[getDomainName(item), item.modelDescription].filter(Boolean).join(' － ')"
                      class="master-entry-card__meta"
                    >
                      <span class="master-entry-card__domain">{{ getDomainName(item) }}</span>
                      <template v-if="item.modelDescription">
                        <span aria-hidden="true" class="master-entry-card__separator">－</span>{{ item.modelDescription }}
                      </template>
                    </div>
                  </div>

                  <div class="master-entry-card__data">
                    <span>权威数据量</span>
                    <b>{{ item.dataCountAvailable === false ? '-' : formatCount(item.dataCount) }}</b>
                    <em>条</em>
                  </div>

                  <div class="master-entry-card__statuses">
                    <div
                      v-for="status in statusItems"
                      :key="status.key"
                      class="master-entry-card__status"
                    >
                      <svg-icon :icon-class="status.icon" />
                      <span>{{ status.label }}</span>
                      <b>{{ formatCount(item[status.key]) }}</b>
                    </div>
                  </div>

                  <div class="master-entry-card__foot">
                    <div class="master-entry-card__summary">
                      <div>
                        维护审批
                        <b>{{ formatCount(item.pendingMaintenanceApprovalCount) }}</b><em>条</em>
                      </div>
                      <div>最近更新 {{ formatLatestTime(item.latestDataUpdateTime) }}</div>
                    </div>
                    <div class="master-entry-card__actions">
                      <el-button
                        v-if="checkPermi(['model:base:query'])"
                        plain
                        type="primary"
                        @click.stop="handleModelInfo(item)"
                      >
                        <svg-icon icon-class="file-list-2-line" />模型信息
                      </el-button>
                      <el-button
                        type="primary"
                        @click.stop="handleEnter(item)"
                      >
                        <svg-icon icon-class="jrwh" />进入维护
                      </el-button>
                    </div>
                  </div>
                </article>
              </div>

              <div v-else class="master-entry-empty">
                <template v-if="!loading">
                  <img :src="emptyStateUrl" alt="" />
                  <span role="status">{{ loadError ? '列表加载失败' : '暂无记录' }}</span>
                </template>
              </div>
            </div>

            <pagination
              v-show="total > 0"
              v-model:page="queryParams.pageNum"
              v-model:limit="queryParams.pageSize"
              :total="total"
              :page-sizes="[PAGE_SIZE]"
              @pagination="getList"
            />
          </section>
        </qt-wrap>
      </el-main>
    </el-container>

  </div>
</template>

<script setup name="MasterInstance">
import { computed, nextTick, onActivated, onDeactivated, onMounted, onUnmounted, reactive, ref } from 'vue'

import { useRouter } from 'vue-router'

import { usePageRefresh } from '@/composables/usePageRefresh'
import { checkPermi } from '@/utils/permission'
import { listMasterInstances } from '@/api/master/instance/instance'
import { getPublishedModelTree } from '@/api/model/base/base'
import QtTree from '@/components/QtTree/index.vue'
import { getDisplayTitle, parseTime } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import { getDefaultModelIcon } from '@/utils/imageUrl'
import emptyStateUrl from '@/assets/common/images/empty-state/empty-nodata.png'

const { model_type } = useDict('model_type')
const PAGE_SIZE = 6
const router = useRouter()
const routeBase = computed(() => '/master/instance')
const statusItems = [
  { key: 'activeCount', label: '生效', icon: 'effective' },
  { key: 'frozenCount', label: '冻结', icon: 'frozen' },
  { key: 'invalidCount', label: '失效', icon: 'invalid' },
  { key: 'archivedCount', label: '归档', icon: 'archived' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: PAGE_SIZE,
  name: undefined,
  projectId: undefined,
  layerId: undefined,
  modelId: undefined,
  type: undefined
})

const modelTreeRef = ref(null)
const selectedTreeKey = ref(null)
// 页面负责查询业务树，QtTree 直接展示传入的层级，不再由组件重新组树或补顶级节点。
const modelTreeData = ref([])
const modelTreeLoading = ref(false)
let modelTreeRequestSequence = 0
const domainNames = reactive({})
const searchStore = computed(() => ({
  items: [
    { label: '模型名称', prop: 'name', component: { is: 'input' } },
    {
      label: '模型类型',
      prop: 'type',
      component: {
        is: 'select',
        placeholder: '请选择模型类型',
        options: model_type.value
      }
    }
  ]
}))

const cards = ref([])
const total = ref(0)
const loading = ref(false)
const loadError = ref(false)

/**
 * 判断入口卡片是否属于分类模型。
 * @param {Object} item 模型卡片数据。
 * @returns {boolean} 是否为分类模型。
 */
function isCategoryModel(item) {
  return item?.modelType === 'CATEGORY'
}

/**
 * 格式化统计数量，缺失值使用占位符。
 * @param {number|string|null|undefined} value 接口返回的数量。
 * @returns {string} 本地化数量文本。
 */
function formatCount(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return Number(value).toLocaleString('zh-CN')
}

/**
 * 格式化最近更新时间，忽略无效日期。
 * @param {Date|string|number|null} value 接口返回的时间。
 * @returns {string} 精确到分钟的时间或占位符。
 */
function formatLatestTime(value) {
  if (!value) return '-'

  let date
  if (value instanceof Date) {
    date = value
  } else if (typeof value === 'string') {
    const normalized = value.replace(/-/g, '/').replace('T', ' ')
    date = new Date(normalized)
  } else {
    date = new Date(value)
  }

  if (Number.isNaN(date.getTime()) || date.getFullYear() <= 0) {
    return '-'
  }
  return parseTime(date, '{y}-{m}-{d} {h}:{i}') || '-'
}

let listRequestSequence = 0
let listNeedsRefresh = true
/**
 * 按当前条件加载卡片，仅接收最新请求结果；失败后清空旧结果。
 * @returns {Promise<void>} 当前请求处理完成后结束。
 */
async function getList() {
  const sequence = ++listRequestSequence
  listNeedsRefresh = true
  loading.value = true
  loadError.value = false
  try {
    const response = await listMasterInstances({ ...queryParams })
    if (sequence !== listRequestSequence) return
    const page = response?.data
    if (!page || !Array.isArray(page.rows)) throw new Error('Invalid master instance list response')
    cards.value = page.rows
    total.value = Number(page.total || 0)
    listNeedsRefresh = false
  } catch {
    if (sequence !== listRequestSequence) return
    cards.value = []
    total.value = 0
    loadError.value = true
  } finally {
    if (sequence === listRequestSequence) loading.value = false
  }
}

/**
 * 回到第一页，按当前条件查询。
 * @returns {void}
 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/**
 * 清空筛选条件并只请求一次第一页。
 * @returns {void}
 */
function resetQuery() {
  queryParams.pageNum = 1
  queryParams.name = undefined
  queryParams.projectId = undefined
  queryParams.layerId = undefined
  queryParams.modelId = undefined
  queryParams.type = undefined
  selectedTreeKey.value = null
  modelTreeRef.value?.resetTree()
  getList()
}

/**
 * 获取卡片所属主数据域，优先使用接口返回的名称。
 * @param {Object} item 模型卡片数据。
 * @returns {string} 主数据域名称或占位文本。
 */
function getDomainName(item) {
  return item.domainName || domainNames[item.projectId] || (item.projectId ? '-' : '未归属主数据域')
}

/**
 * 为公共模型树添加跨节点类型唯一的展示 key，保留接口层级和业务 ID。
 * @returns {Promise<void>} 将最新树数据交给 QtTree 展示。
 */
async function loadModelTree() {
  if (!checkPermi(['model:base:list'])) {
    modelTreeData.value = []
    return
  }
  // 只采用当前这轮请求；离开页面或再次刷新后，旧结果不再覆盖树数据。
  const sequence = ++modelTreeRequestSequence
  modelTreeLoading.value = true
  try {
    const response = await getPublishedModelTree()
    const mapNodes = nodes => nodes.map(node => ({
      ...node,
      treeKey: `${node.nodeType}:${node.id}`,
      children: mapNodes(node.children || [])
    }))
    if (sequence === modelTreeRequestSequence) modelTreeData.value = mapNodes(response.data || [])
  } finally {
    // 旧请求结束不能关掉新请求的加载提示。
    if (sequence === modelTreeRequestSequence) modelTreeLoading.value = false
  }
}

/**
 * 从树数据补全卡片中的主数据域名称；刷新后恢复选择，失效节点回到全部。
 * @param {Array<Object>} nodes 树节点。
 * @returns {Promise<void>}
 */
async function handleModelTreeChange(nodes) {
  let selectionExists = selectedTreeKey.value === null
  Object.keys(domainNames).forEach(key => { delete domainNames[key] })
  function visit(rows) {
    rows.forEach(node => {
      if (node.treeKey === selectedTreeKey.value) selectionExists = true
      if (node.nodeType === 'project') domainNames[node.projectId] = node.name
      visit(node.children || [])
    })
  }
  visit(nodes)
  await nextTick()
  if (selectionExists) modelTreeRef.value?.setCurrentKey(selectedTreeKey.value)
  else handleTreeClick({ treeKey: null })
}

/**
 * 切换域、目录或模型时清除上次节点条件，按当前查询条件请求第一页。
 * @param {Object} node 当前树节点。
 * @returns {void}
 */
function handleTreeClick(node) {
  selectedTreeKey.value = node.treeKey
  queryParams.projectId = node.projectId
  queryParams.layerId = node.layerId
  queryParams.modelId = node.modelId
  modelTreeRef.value?.setCurrentKey(node.treeKey)
  handleQuery()
}

let pageActive = false
// 首次打开和返回缓存页都从同一个入口取树，避免挂载与激活分别请求一次。
let treeNeedsRefresh = true
/**
 * 离开页面时使未完成列表请求失效，并在返回后刷新模型树。
 * @returns {void}
 */
function deactivatePage() {
  pageActive = false
  listRequestSequence++
  modelTreeRequestSequence++
  modelTreeLoading.value = false
  treeNeedsRefresh = true
  loading.value = false
}

onDeactivated(deactivatePage)
onUnmounted(deactivatePage)

/**
 * 打开对应模型的定义详情，沿用权威主数据入口的菜单高亮。
 * @param {Object} item 模型卡片数据。
 * @returns {void}
 */
function handleModelInfo(item) {
  if (!checkPermi(['model:base:query'])) return
  router.push({
    path: `${routeBase.value}/model/detail`,
    query: { id: item.modelId }
  })
}

/**
 * 按模型类型进入普通或分类主数据维护页。
 * @param {Object} item 模型卡片数据，包含模型、实例和版本 ID。
 * @returns {void}
 */
function handleEnter(item) {
  const path = isCategoryModel(item) ? `${routeBase.value}/category` : `${routeBase.value}/data`
  router.push({
    path,
    query: { modelId: item.modelId, instanceId: item.instanceId, modelVersionId: item.modelVersionId }
  })
}

/**
 * 激活页面并补充加载必要数据，合并首次挂载和缓存激活的请求。
 * @returns {Promise<void>}
 */
async function activatePage() {
  if (pageActive) return
  pageActive = true
  const requests = []
  if (listNeedsRefresh) requests.push(getList())
  if (treeNeedsRefresh) {
    treeNeedsRefresh = false
    requests.push(loadModelTree().catch(() => { treeNeedsRefresh = true }))
  }
  await Promise.allSettled(requests)
}

// 沿用数据中台的刷新标记；未成功加载时也在返回后补查询。
usePageRefresh('masterInstance', () => { listNeedsRefresh = true })

// 缓存名单可能晚于首次挂载更新，两个钩子共用入口避免重复请求。
onMounted(activatePage)
onActivated(activatePage)
</script>

<style scoped lang="scss" src="@/views/master/instance/index.scss"></style>
