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
    <!-- 列表顶部复用 GuideTip；“不再展示”状态由组件按用户保存。 -->
    <guide-tip tip-id="authorization/serviceAuthorization.list" />
    <el-container>
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :editable="false"
        :use-builtin-cat-edit-dialog="false"
        :default-expanded-keys="modelTreeExpandedKeys"
        :left-width="260"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      />
      <el-main class="main-content authorization-main">
        <qt-wrap :columns="tableStore.columns" :table-ref="tableRef">
          <template #search>
            <qt-search-bar
              :items="searchStore.items"
              :params="tableStore.params"
              :table-ref="tableRef"
              @reset="resetTreeSelection"
            />
          </template>
          <template #actions-data>
            <el-button
              type="primary"
              plain
              v-hasPermi="['auth:service:add']"
              @click="handleCreate"
              @mousedown="(event) => event.preventDefault()"
            >
              <i class="iconfont-mini icon-xinzeng mr5"></i>新增
            </el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="!selection.rows.length"
              v-hasPermi="['auth:service:remove']"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </template>

          <qt-table ref="tableRef" v-bind="tableStore">
            <template #subjectTitle="{ row }">
              <el-link
                v-if="getDisplayTitle(row.subjectName)"
                type="primary" :underline="false"
                role="link" tabindex="0"
                class="qt-detail-link text-ellipsis"
                :title="String(row.subjectName)"
                @click.stop="openDetail(row)"
                @keydown.enter.stop.prevent="openDetail(row)"
                @keydown.space.stop.prevent="openDetail(row)"
              >{{ row.subjectName }}</el-link>
              <span v-else class="qt-detail-link text-ellipsis">-</span>
              <dict-tag size="small" :options="subjectTypeOptions" :value="row.subjectType" />
            </template>
            <template #serviceInfo="{ row }">
              <div class="qt-table-info service-info">
                <div class="qt-table-info__main">
                  <span
                    class="info-title service-info-title text-ellipsis"
                    :role="canOpenService(row) ? 'link' : undefined"
                    :tabindex="canOpenService(row) ? 0 : undefined"
                    @click="openServiceDetail(row)"
                    @keydown.enter="openServiceDetail(row)"
                    @keydown.space.prevent="openServiceDetail(row)"
                  >{{ formatServiceTitle(row) }}</span>
                  <dict-tag v-if="row.version && row.version !== '-'" class="ml4" size="small" type="info">
                    {{ row.version }}
                  </dict-tag>
                </div>
              </div>
            </template>
            <template #scope="{ row }">
              <qt-tag-group
                :items="[toTag(row.scopeType === 'all' ? '全部数据' : '条件范围', row.scopeType === 'all' ? 'success' : 'warning')]"
                type="none"
                :max-count="1"
              />
            </template>
            <template #fields="{ row }">
              <p class="permission-line">可见 {{ row.visibleFields }} 个字段</p>
            </template>
            <template #status="{ row }">
              <qt-tag-group
                :items="[toTag(statusMap[row.status].label, statusMap[row.status].type)]"
                type="none"
                :max-count="1"
              />
              <p v-if="row.statusRemark" class="status-remark">{{ row.statusRemark }}</p>
            </template>
            <template #action="{ row }">
              <div class="table-action">
                <el-button
                  link
                  type="primary"
                  icon="View"
                  v-hasPermi="['auth:service:query']"
                  @click="openDetail(row)"
                >详情</el-button>
                <el-button
                  link
                  type="primary"
                  icon="Edit"
                  v-hasPermi="['auth:service:edit']"
                  @click="openEdit(row)"
                >修改</el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  v-hasPermi="['auth:service:remove']"
                  @click="deleteAuthorization(row)"
                >删除</el-button>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <el-dialog
      v-model="grantOpen"
      :title="editingRecord ? '修改服务授权' : '新增服务授权'"
      width="1100px"
      class="authorization-wizard"
      destroy-on-close
      :close-on-click-modal="false"
      :close-on-press-escape="!grantBusy"
      :show-close="!grantBusy"
    >
      <steps
        :steps="wizardSteps"
        :active-step="grantStep - 1"
        is-dialog
      />
      <el-form
        ref="grantFormRef"
        :model="grantForm"
        :rules="grantRules"
        label-width="120px"
        class="wizard-form"
        :disabled="grantBusy"
      >
        <section v-show="grantStep === 1">
          <!-- 首步说明置于章节标题上方，先说明授权规则再填写主体与服务。 -->
          <guide-tip-panel
            class="mb20"
            type="primary"
            allow-wrap
            title="服务授权以单个数据服务为最小授权单元。新增时可选择一个或多个授权主体，同一套权限将分别授予每个所选主体。"
          />
          <div class="h2-titles">授权主体与服务</div>
          <div class="column-form">
            <qt-form-item
              label="主体类型"
              prop="subjectType"
              required
              class="row-full"
            >
              <el-radio-group v-model="grantForm.subjectType" @change="handleSubjectTypeChange">
                <el-radio value="user">平台用户</el-radio>
                <el-radio value="client">服务客户端</el-radio>
              </el-radio-group>
            </qt-form-item>
            <qt-form-item
              v-if="editingRecord"
              label="授权主体名称"
              prop="subjectId"
              required
              class="row-full"
            >
              <el-select v-model="grantForm.subjectId" filterable placeholder="请选择授权主体名称">
                <el-option
                  v-for="subject in filteredSubjectOptions"
                  :key="subject.id"
                  :label="`${subject.name}（${subject.code}）`"
                  :value="subject.id"
                />
              </el-select>
            </qt-form-item>
            <qt-form-item
              v-else
              label="授权主体名称"
              prop="subjectIds"
              required
              class="row-full"
            >
              <!-- 新增统一支持选择多个主体，超过五个已选标签时折叠展示。 -->
              <el-select
                v-model="grantForm.subjectIds"
                placeholder="请选择授权主体名称（可多选）"
                multiple
                filterable
                collapse-tags
                :max-collapse-tags="5"
                collapse-tags-tooltip
              >
                <el-option
                  v-for="subject in filteredSubjectOptions"
                  :key="subject.id"
                  :label="`${subject.name}（${subject.code}）`"
                  :value="subject.id"
                />
              </el-select>
            </qt-form-item>
            <qt-form-item
              label="数据服务"
              prop="serviceId"
              required
              class="row-full"
            >
              <el-select v-model="grantForm.serviceId" placeholder="请选择数据服务" filterable>
                <el-option
                  v-for="service in serviceOptions"
                  :key="service.id"
                  :label="`${service.name}（${service.code} · ${service.version}）`"
                  :value="service.id"
                />
              </el-select>
            </qt-form-item>
          </div>
        </section>
        <section v-show="grantStep === 2">
          <guide-tip-panel
            class="mb20"
            type="primary"
            allow-wrap
            title="数据范围用于控制授权主体可访问的数据记录。「按条件授权」可根据字段及筛选条件限制访问范围，满足不同业务场景下的数据隔离需求。"
          />
          <div class="h2-titles">数据范围</div>
          <el-form-item label="数据范围模式" prop="scopeType" required>
            <el-radio-group v-model="grantForm.scopeType">
              <el-radio value="condition">按条件授权</el-radio>
              <el-radio value="all">全部数据</el-radio>
            </el-radio-group>
          </el-form-item>
          <!-- 仅在按条件授权时显示条件组配置。 -->
          <div v-if="grantForm.scopeType === 'condition'">
            <template v-for="(group, groupIndex) in grantForm.scopeGroups" :key="group.id">
              <el-divider v-if="groupIndex" content-position="center">OR</el-divider>
              <div class="mb15">
                <div class="justify-between mb15">
                  <strong>条件组 {{ groupIndex + 1 }} · 组内 AND</strong>
                  <el-button
                    link
                    type="primary"
                    icon="Plus"
                    @click="addCondition(group)"
                  >
                    添加条件
                  </el-button>
                </div>
                <qt-table :columns="scopeColumns" :config="scopeTableConfig" :func="() => queryScopeConditions(group)">
                  <template #field="{ row }">
                    <el-select
                      v-model="row.field"
                      placeholder="请选择字段"
                      @change="revalidateScopeGroups"
                    >
                      <el-option
                        v-for="field in scopeFieldOptions"
                        :key="field.value"
                        :label="field.label"
                        :value="field.value"
                      />
                    </el-select>
                  </template>
                  <template #operator="{ row }">
                    <el-select
                      v-model="row.operator"
                      placeholder="请选择运算符"
                      @change="revalidateScopeGroups"
                    >
                      <el-option
                        v-for="operator in scopeOperatorOptions"
                        :key="operator.value"
                        :label="operator.label"
                        :value="operator.value"
                      />
                    </el-select>
                  </template>
                  <template #value="{ row }">
                    <el-input v-model="row.value" placeholder="请输入条件值" @change="revalidateScopeGroups" />
                  </template>
                  <template #action="{ $index }">
                    <el-tooltip content="至少保留一条条件" :disabled="canRemoveCondition(group)" placement="top">
                      <span class="scope-condition-delete">
                        <el-button
                          link
                          type="danger"
                          icon="Delete"
                          :disabled="!canRemoveCondition(group)"
                          @click="removeCondition(groupIndex, $index)"
                        >删除</el-button>
                      </span>
                    </el-tooltip>
                  </template>
                </qt-table>
                <p v-if="scopeGroupErrors[group.id]" class="wizard-block-error">{{ scopeGroupErrors[group.id] }}</p>
              </div>
            </template>
            <div class="btn-style">
              <el-button
                type="primary"
                plain
                size="small"
                icon="Plus"
                @click="addScopeGroup"
              >添加 OR 条件组</el-button>
            </div>
          </div>
          <p v-if="grantForm.scopeType === 'condition' && !grantForm.scopeGroups.length && blockErrors.scopeGroups" class="wizard-block-error">{{ blockErrors.scopeGroups }}</p>
        </section>
        <section v-show="grantStep === 3">
          <!-- 服务授权的字段权限只控制服务返回哪些字段，不授予字段修改权限。 -->
          <guide-tip-panel
            class="mb20"
            type="primary"
            allow-wrap
            title="服务仅返回具备查看权限的字段。"
          />
          <div class="h2-titles">字段权限</div>
          <qt-table
            v-if="grantStep === 3"
            :columns="grantFieldColumns"
            :config="grantFieldTableConfig"
            :func="queryGrantFields"
          >
            <template #visible="{ row }">
              <el-checkbox v-model="row.visible" @change="handleVisibleChange" />
            </template>
          </qt-table>
          <p v-if="blockErrors.fieldPermissions" class="wizard-block-error">
            {{ blockErrors.fieldPermissions }}
          </p>
        </section>
        <section v-show="grantStep === 4">
          <!-- 第四步只设置授权有效期；服务授权不再按操作类型区分权限。 -->
          <div class="h2-titles">授权有效期</div>
          <div class="column-form">
            <qt-form-item label="申请周期" class="row-full">
              <el-radio-group v-model="grantForm.longTerm">
                <el-radio :value="true">长期有效</el-radio>
                <el-radio :value="false">自定义</el-radio>
              </el-radio-group>
            </qt-form-item>
            <qt-form-item label="生效时间" prop="startDate" required>
              <el-date-picker
                v-model="grantForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择生效时间"
                class="number-input"
              />
            </qt-form-item>
            <qt-form-item label="失效时间" prop="endDate" :required="!grantForm.longTerm">
              <el-date-picker
                v-model="grantForm.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择失效时间"
                class="number-input"
                :disabled="grantForm.longTerm"
              />
            </qt-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="grantBusy" @click="grantOpen = false">返回</el-button>
          <el-button v-if="grantStep > 1" :disabled="grantBusy" @click="grantStep--">上一步</el-button>
          <el-button
            v-if="grantStep < wizardSteps.length"
            type="primary"
            :loading="grantAdvancing"
            :disabled="grantBusy"
            @click="nextStep"
          >
            下一步
          </el-button>
          <el-button
            v-else
            type="primary"
            :loading="grantSubmitting"
            :disabled="grantBusy"
            @click="submitGrant"
          >确认</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="ServiceAuthorization">
import { getDisplayTitle } from '@/utils/anivia.js'
import { useRouter } from 'vue-router'
import Steps from '@/components/Steps/index.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import QtTree from '@/components/QtTree/index.vue'
import QtTable from '@/components/QtTable/index.vue'
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import { listBase } from '@/api/model/base/base.js'
import { listProject } from '@/api/model/project/project.js'
import { listLayer } from '@/api/model/layer/layer.js'
import { listService } from '@/api/dist/service/service.js'
import {
  SERVICE_AUTHORIZATION_CLIENTS as clientSubjects,
  SERVICE_AUTHORIZATION_FIELDS as fieldDefinitions,
  SERVICE_AUTHORIZATION_SERVICES,
  SERVICE_AUTHORIZATION_SUBJECT_TYPES as subjectTypeOptions
} from '@/utils/authorization/serviceAuthorization.js'
import {
  batchGrantServiceAuthorizations,
  batchDeleteServiceAuthorizations,
  createServiceAuthorization,
  getServiceAuthorization,
  listServiceAuthorizations,
  updateServiceAuthorization
} from '@/api/authorization/serviceAuthorization.js'

const userDirectoryStore = useUserDirectoryStore();

const { proxy } = getCurrentInstance()
const router = useRouter()
const canViewService = computed(() => proxy.$auth.hasPermi('dist:service:query'))
const tableRef = ref()
const modelTreeRef = ref()
const modelTreeData = ref([])
const modelTreeLoading = ref(true)
const modelTreeExpandedKeys = ref([])
const selectedTreeNode = ref(null)
const grantOpen = ref(false)
const grantStep = ref(1)
const grantSubmitting = ref(false)
const grantAdvancing = ref(false)
const grantBusy = computed(() => grantSubmitting.value || grantAdvancing.value)
const editingRecord = ref()
const selection = reactive({ rows: [] })

const statusMap = {
  active: { label: '有效', type: 'success' },
  pending: { label: '未生效', type: 'warning' }
}
const subjectOptions = ref([])
const treeServiceOptions = ref([])

function buildAuthorizationModelTree(projects = [], layers = [], models = []) {
  const projectNodes = projects.map(project => ({
    ...project, nodeType: 'project', treeKey: `project:${project.id}`, children: []
  }))
  const projectMap = new Map(projectNodes.map(node => [String(node.id), node]))
  const layerNodes = layers.map(layer => ({
    ...layer, nodeType: 'layer', treeKey: `layer:${layer.id}`, children: []
  }))
  const layerMap = new Map(layerNodes.map(node => [String(node.id), node]))
  for (const layer of layerNodes) {
    const project = projectMap.get(String(layer.projectId))
    if (!project) continue
    if (layer.parentId == null || String(layer.parentId) === '0') {
      project.children.push(layer)
    } else {
      const parent = layerMap.get(String(layer.parentId))
      if (parent && String(parent.projectId) === String(layer.projectId)) parent.children.push(layer)
    }
  }
  for (const model of models) {
    if (!model.id || (model.parentId != null && String(model.parentId) !== '0')) continue
    const project = projectMap.get(String(model.projectId))
    const layer = layerMap.get(String(model.layerId))
    const parent = layer && String(layer.projectId) === String(model.projectId) ? layer : project
    parent?.children.push({
      ...model, nodeType: 'model', modelId: model.id, treeKey: `model:${model.id}`, children: []
    })
  }
  return projectNodes
}

function collectAuthorizationModelIds(node) {
  if (node.nodeType === 'model') return [node.modelId]
  return (node.children || []).flatMap(collectAuthorizationModelIds)
}

const wizardSteps = [
  { title: '主体与服务' },
  { title: '数据范围' },
  { title: '字段权限' },
  { title: '授权有效期' }
]
const scopeFieldOptions = [
  { label: '所属区域', value: 'region' },
  { label: '供应商等级', value: 'supplier_level' },
  { label: '供应商类型', value: 'supplier_type' },
  { label: '状态', value: 'status' }
]
const scopeOperatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '包含', value: 'contains' }
]
let scopeConditionSeed = 0
function createScopeCondition() { return { id: `condition-${++scopeConditionSeed}`, field: 'region', operator: 'eq', value: '' } }
function createScopeGroups() { return [{ id: `group-${++scopeConditionSeed}`, conditions: [createScopeCondition()] }] }
function createFieldPermissions() {
  return fieldDefinitions.map((field, index) => ({ ...field, visible: index < 8 }))
}
const grantForm = reactive({
  subjectType: 'user', subjectId: '', subjectIds: [], serviceId: '', scopeType: 'all', scopeDetail: '',
  scopeGroups: createScopeGroups(), fieldPermissions: createFieldPermissions(), startDate: '', endDate: '', longTerm: true
})
const serviceOptions = ref(SERVICE_AUTHORIZATION_SERVICES.map(service => ({ ...service })))
const filteredSubjectOptions = computed(() => subjectOptions.value.filter(item => item.type === grantForm.subjectType))
const scopeColumns = [
  { label: '编号', type: 'index', width: 70, minWidth: 70 },
  { label: '字段', prop: 'field', slot: 'field', minWidth: 200, align: 'left' },
  { label: '运算符', prop: 'operator', slot: 'operator', minWidth: 160, align: 'left' },
  { label: '条件值', prop: 'value', slot: 'value', minWidth: 200, align: 'left' },
  { label: '操作', slot: 'action', width: 100, minWidth: 100 }
]
const scopeTableConfig = {
  fullContent: false, notPagination: true,
  table: { stripe: true, border: false, rowKey: 'id' }
}
const grantFieldColumns = [
  { label: '编号', type: 'index', width: 70, minWidth: 70 },
  { label: '字段名称', prop: 'name', minWidth: 200, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { label: '字段编码', prop: 'code', minWidth: 200, align: 'left', showOverflowTooltip: { effect: 'light' } },
  { label: '允许查看', prop: 'visible', slot: 'visible', minWidth: 140 }
]
const grantFieldTableConfig = {
  fullContent: false, notPagination: true,
  table: { stripe: true, border: false, rowKey: 'id', maxHeight: 330 }
}
async function queryScopeConditions(group) { return { data: { rows: group.conditions } } }
async function queryGrantFields() { return { data: { rows: grantForm.fieldPermissions } } }
const tableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: 'id',
      // 显式清空默认排序，避免 QtTable 自动使用创建时间倒序。
      defaultSort: { prop: '', order: null },
      onSelectionChange(rows) {
        selection.rows = rows
      }
    },
    pagination: {
      layout: 'total, sizes, prev, pager, next, jumper',
      pageSizes: [10, 20, 50, 100]
    }
  },
  columns: [
    { type: 'selection', width: 55, minWidth: 55 },
    { label: '编号', prop: 'id', width: 70, minWidth: 70, sortable: true },
    { label: '授权主体', info: { custom: 'subjectTitle', desc: 'subjectDescription' }, minWidth: 250, align: 'left' },
    { label: '数据服务 / API', slot: 'serviceInfo', minWidth: 340, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '数据范围', slot: 'scope', minWidth: 240, align: 'left' },
    { label: '字段权限', slot: 'fields', minWidth: 210, align: 'left' },
    { label: '有效期', prop: 'validPeriod', minWidth: 230 },
    { label: '授权状态', slot: 'status', minWidth: 120 },
    { label: '操作', slot: 'action', width: 260, minWidth: 260, align: 'center', fixed: 'right' }
  ],
  params: { subjectId: '', subjectType: '', serviceId: '', status: '' },
  func: fetchAuthorizationPage
})
const searchStore = reactive({
  items: [
    { label: '授权主体', prop: 'subjectId', component: { is: 'select', options: [] } },
    { label: '主体类型', prop: 'subjectType', component: { is: 'select', options: [{ label: '平台用户', value: 'user' }, { label: '服务客户端', value: 'client' }] } },
    { label: '数据服务', prop: 'serviceId', component: { is: 'select', options: serviceOptions.value.map(service => ({ label: service.name, value: service.id })) } },
    { label: '授权状态', prop: 'status', component: { is: 'select', options: [{ label: '有效', value: 'active' }, { label: '未生效', value: 'pending' }] } }
  ]
})

function handleModelTreeClick(node) {
  selectedTreeNode.value = node
  tableStore.params.serviceId = ''
  selection.rows = []
  tableRef.value?.elTableRef?.clearSelection()
  tableRef.value?.resetQuery()
}

function resetTreeSelection() {
  selectedTreeNode.value = null
  modelTreeRef.value?.setCurrentKey(null, false)
  selection.rows = []
  tableRef.value?.elTableRef?.clearSelection()
}

function resetGrantForm() {
  Object.assign(grantForm, {
    subjectType: 'user', subjectId: '', subjectIds: [], serviceId: '', scopeType: 'all', scopeDetail: '',
    scopeGroups: createScopeGroups(), fieldPermissions: createFieldPermissions(),
    startDate: formatDate(new Date()), endDate: '', longTerm: true
  })
  Object.assign(blockErrors, { scopeGroups: '', fieldPermissions: '' })
  nextTick(() => grantFormRef.value?.clearValidate())
}
function handleCreate() { resetGrantForm(); editingRecord.value = undefined; grantStep.value = 1; grantOpen.value = true }
function resetSubjects() { grantForm.subjectId = ''; grantForm.subjectIds = [] }
function handleSubjectTypeChange() {
  resetSubjects()
  const firstSubject = filteredSubjectOptions.value[0]
  if (editingRecord.value) grantForm.subjectId = firstSubject?.id || ''
  grantFormRef.value?.clearValidate(['subjectId', 'subjectIds'])
}
function handleVisibleChange() {
  blockErrors.fieldPermissions = fieldPermissionsMessage()
}
function addCondition(group) { group.conditions.push(createScopeCondition()); revalidateScopeGroups() }
function addScopeGroup() { grantForm.scopeGroups.push({ id: `group-${++scopeConditionSeed}`, conditions: [createScopeCondition()] }); revalidateScopeGroups() }
function canRemoveCondition(group) { return grantForm.scopeGroups.length > 1 || group.conditions.length > 1 }
function removeCondition(groupIndex, conditionIndex) {
  const group = grantForm.scopeGroups[groupIndex]
  if (!group || !canRemoveCondition(group)) return
  if (group.conditions.length > 1) group.conditions.splice(conditionIndex, 1)
  else grantForm.scopeGroups.splice(groupIndex, 1)
  revalidateScopeGroups()
}
const grantFormRef = ref()
const blockErrors = reactive({ scopeGroups: '', fieldPermissions: '' })
const scopeGroupErrors = computed(() => {
  if (grantForm.scopeType !== 'condition' || !blockErrors.scopeGroups) return {}
  return Object.fromEntries(grantForm.scopeGroups.map((group, index) => [group.id, scopeGroupMessage(group, index)]))
})
const stepFields = {
  1: () => ['subjectType', editingRecord.value ? 'subjectId' : 'subjectIds', 'serviceId'],
  2: () => ['scopeType'],
  3: () => [],
  4: () => ['startDate', 'endDate']
}
function scopeGroupMessage(group, groupIndex) {
  if (!group.conditions.length) return '请为条件组' + (groupIndex + 1) + '添加条件'
  for (let conditionIndex = 0; conditionIndex < group.conditions.length; conditionIndex++) {
    const condition = group.conditions[conditionIndex]
    const position = '条件组' + (groupIndex + 1) + '第' + (conditionIndex + 1) + '条'
    if (!condition.field) return '请选择' + position + '的字段'
    if (!condition.operator) return '请选择' + position + '的运算符'
    if (!String(condition.value ?? '').trim()) return '请填写' + position + '的条件值'
  }
  return ''
}
function scopeGroupsMessage() {
  if (grantForm.scopeType !== 'condition') return ''
  if (!grantForm.scopeGroups.length) return '请添加条件组'
  for (let groupIndex = 0; groupIndex < grantForm.scopeGroups.length; groupIndex++) {
    const message = scopeGroupMessage(grantForm.scopeGroups[groupIndex], groupIndex)
    if (message) return message
  }
  return ''
}
function fieldPermissionsMessage() {
  return grantForm.fieldPermissions.some(item => item.visible) ? '' : '请至少选择一个可查看字段'
}
function revalidateScopeGroups() {
  blockErrors.scopeGroups = scopeGroupsMessage()
}
function validateBlockChecks(step) {
  blockErrors.scopeGroups = ''
  blockErrors.fieldPermissions = ''
  if (step === 2) blockErrors.scopeGroups = scopeGroupsMessage()
  if (step === 3) blockErrors.fieldPermissions = fieldPermissionsMessage()
  return !blockErrors.scopeGroups && !blockErrors.fieldPermissions
}
function validateSubjectIds(rule, value, callback) {
  if (!grantForm.subjectIds.length) return callback(new Error('请选择授权主体'))
  callback()
}
function validateEndDate(rule, value, callback) {
  if (grantForm.longTerm || !grantForm.startDate) return callback()
  if (!grantForm.endDate) return callback(new Error('请选择失效时间'))
  if (grantForm.endDate < grantForm.startDate) return callback(new Error('失效时间不能早于生效时间'))
  callback()
}
const grantRules = {
  subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }],
  subjectId: [{ required: true, message: '请选择授权主体', trigger: 'change' }],
  subjectIds: [{ validator: validateSubjectIds, trigger: 'change' }],
  serviceId: [{ required: true, message: '请选择数据服务', trigger: 'change' }],
  scopeType: [{ required: true, message: '请选择数据范围模式', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  // 与表单项的动态 required 合并，保留中文必填提示。
  endDate: [{ required: false, message: '请选择失效时间', trigger: 'change' }, { validator: validateEndDate, trigger: 'change' }]
}
async function validateGrantStep(step) {
  const blocksValid = validateBlockChecks(step)
  const fields = stepFields[step]()
  if (!fields.length) return blocksValid
  try {
    await grantFormRef.value.validateField(fields)
    return blocksValid
  } catch {
    return false
  }
}
async function nextStep() {
  if (grantBusy.value) return
  grantAdvancing.value = true
  try {
    if (await validateGrantStep(grantStep.value)) grantStep.value++
  } finally {
    grantAdvancing.value = false
  }
}
async function submitGrant() {
  if (grantBusy.value) return
  grantSubmitting.value = true
  try {
    for (let step = 1; step <= 4; step++) {
      if (!(await validateGrantStep(step))) {
        grantStep.value = step
        return
      }
    }
    const subjectIds = editingRecord.value ? [grantForm.subjectId] : grantForm.subjectIds
    const isBatchCreate = !editingRecord.value && subjectIds.length > 1
    const payload = buildSavePayload(subjectIds, isBatchCreate)
    let batchResult
    if (editingRecord.value) await updateServiceAuthorization({ ...payload, id: editingRecord.value.id, subjectIds: undefined })
    else if (isBatchCreate) batchResult = unwrapData(await batchGrantServiceAuthorizations(payload))
    else await createServiceAuthorization(payload)
    grantOpen.value = false
    tableRef.value?.getList()
    proxy.$modal.msgSuccess(editingRecord.value ? (isRevokedStatus(editingRecord.value.persistedStatus) ? '授权修改并恢复成功' : '授权修改成功') : isBatchCreate
      ? formatBatchResult(batchResult)
      : '授权成功')
  } catch {
    // 请求拦截器已显示后端的具体错误；保留弹窗供用户修改后重试，避免重复提示。
  } finally {
    grantSubmitting.value = false
  }
}
function formatBatchResult(result) {
  if (result?.createdCount == null || result?.updatedCount == null || result?.unchangedCount == null) return '批量授权处理完成'
  return `批量授权完成：新增 ${result.createdCount} 条，更新 ${result.updatedCount} 条，未变更 ${result.unchangedCount} 条`
}
// 批量删除同时终止所选授权；后端在同一事务中完成撤权与删除。
function handleBatchDelete() {
  const ids = selection.rows.map(item => item.id)
  if (!ids.length) return
  proxy.$modal.confirm(`确认删除选中的 ${ids.length} 条服务授权吗？删除后授权立即失效。`).then(() => {
    return batchDeleteServiceAuthorizations(ids)
  }).then(() => {
    selection.rows = []
    tableRef.value?.getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
// 删除单条授权时同时终止该主体对服务的访问权限。
function deleteAuthorization(row) {
  proxy.$modal.confirm('确认删除该服务授权？删除后立即失效。').then(() => {
    return batchDeleteServiceAuthorizations([row.id])
  }).then(() => {
    tableRef.value?.getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
function openDetail(row) {
  router.push({ path: '/authorization/serviceAuthorization/detail', query: { id: row.id } })
}
function formatServiceTitle(row) {
  const name = row.serviceName === '-' ? '' : row.serviceName
  const code = row.serviceCode === '-' ? '' : row.serviceCode
  return name ? (code ? `${name} (${code})` : name) : code || '-'
}
function canOpenService(row) {
  return canViewService.value && row.serviceId != null
}
function openServiceDetail(row) {
  if (!canOpenService(row)) return
  router.push({ path: '/dist/service/detail', query: { id: row.serviceId } })
}
async function openEdit(row) {
  let detail
  try {
    detail = unwrapData(await getServiceAuthorization(row.id))
  } catch {
    return
  }
  Object.assign(grantForm, {
    subjectType: fromSubjectType(detail.subjectType),
    subjectId: detail.subjectId,
    subjectIds: [],
    serviceId: detail.serviceId,
    scopeType: detail.dataScopeType === 'CONDITION' ? 'condition' : 'all',
    scopeGroups: deserializeScopeGroups(detail.dataScopeConfig),
    fieldPermissions: mergeFieldPermissions(detail.fieldPermissions),
    startDate: formatDate(detail.effectiveTime),
    endDate: formatDate(detail.expireTime),
    longTerm: Boolean(detail.longTerm)
  })
  editingRecord.value = detail
  grantStep.value = 1
  grantOpen.value = true
}
function toTag(label, type = 'primary') { return { label, value: label, elTagType: type } }

function unwrapData(response) { return response?.data ?? response }
function isRevokedStatus(value) { return value === '0' || value === 'REVOKED' }
function extractRows(response) {
  const data = unwrapData(response) || {}
  return { rows: data.rows || data.list || (Array.isArray(data) ? data : []), total: data.total || 0 }
}
async function listAllServices() {
  const pageSize = 100
  const firstPage = extractRows(await listService({ pageNum: 1, pageSize }))
  const pageCount = Math.ceil(firstPage.total / pageSize)
  if (pageCount <= 1) return firstPage.rows
  const remainingPages = await Promise.all(
    Array.from({ length: pageCount - 1 }, (_, index) =>
      listService({ pageNum: index + 2, pageSize }).then(response => extractRows(response).rows))
  )
  return firstPage.rows.concat(...remainingPages)
}
function formatDate(value) {
  if (!value) return ''
  if (value instanceof Date) {
    const year = value.getFullYear()
    const month = String(value.getMonth() + 1).padStart(2, '0')
    const day = String(value.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? String(value).slice(0, 10) : formatDate(parsed)
}
function toDateTime(value, endOfDay = false) { return value ? new Date(`${value}T${endOfDay ? '23:59:59.999' : '00:00:00'}`).toISOString() : null }
function fromSubjectType(value) { return value === 'CLIENT' ? 'client' : 'user' }
function toSubjectType(value) { return value === 'client' ? 'CLIENT' : 'USER' }
function deserializeScopeGroups(config) {
  if (!config?.groups?.length) return createScopeGroups()
  return config.groups.map(group => ({ id: `group-${++scopeConditionSeed}`, conditions: (group.conditions || []).map(condition => ({ id: `condition-${++scopeConditionSeed}`, field: condition.fieldCode, operator: String(condition.operator || 'EQ').toLowerCase(), value: condition.value || '' })) }))
}
function summarizeScope(config) {
  const operators = { EQ: '等于', NE: '不等于', CONTAINS: '包含' }
  return (config?.groups || []).map(group => (group.conditions || []).map(condition => `${condition.fieldCode} ${operators[condition.operator] || condition.operator} ${condition.value}`).join(' 且 ')).filter(Boolean).join(' 或 ')
}
function mergeFieldPermissions(saved) {
  const savedMap = new Map((saved || []).map(item => [String(item.fieldId ?? item.fieldCode), item]))
  return fieldDefinitions.map(field => {
    const permission = savedMap.get(String(field.id)) || savedMap.get(field.code)
    return { ...field, visible: Boolean(permission?.view) }
  })
}
function mapAuthorization(item) {
  const service = serviceOptions.value.find(option => String(option.id) === String(item.serviceId))
  const scopeType = item.dataScopeType === 'CONDITION' ? 'condition' : 'all'
  const fieldPermissions = item.fieldPermissions || []
  const subjectType = fromSubjectType(item.subjectType)
  const client = subjectType === 'client' ? clientSubjects.find(option => String(option.id) === String(item.subjectId)) : undefined
  return {
    ...item,
    subjectType,
    subjectName: item.subjectName || (subjectType === 'client' ? client?.name || item.subjectCode || '-' : item.subjectName),
    serviceName: item.serviceName || service?.name || '-',
    serviceCode: item.serviceCode || service?.code || '-',
    version: item.serviceVersion || item.version || service?.version || '-',
    endpoint: item.endpoint || '-', protocol: item.protocol || '-', method: item.method || '-',
    scopeType,
    scopeGroups: deserializeScopeGroups(item.dataScopeConfig),
    scopeSummary: scopeType === 'condition' ? summarizeScope(item.dataScopeConfig) : '',
    scopeDetail: scopeType === 'condition' ? summarizeScope(item.dataScopeConfig) : '',
    visibleFields: item.viewFieldCount ?? fieldPermissions.filter(field => field.view).length,
    fieldNames: fieldPermissions
      .filter(field => field.view)
      .map(field => field.fieldName || field.fieldCode)
      .filter(name => name && name !== '-'),
    validPeriod: item.longTerm ? '长期有效' : `${formatDate(item.effectiveTime)} ~ ${formatDate(item.expireTime)}`,
    status: item.displayStatus === 'EFFECTIVE' ? 'active' : 'pending',
    statusRemark: ({ REVOKED: '已撤销', NOT_STARTED: '未到生效时间', EXPIRED: '已过期', INVALID: '授权无效' })[item.inactiveReason] || ''
  }
}
async function fetchAuthorizationPage(params) {
  const selectedModelIds = selectedTreeNode.value
    ? new Set(collectAuthorizationModelIds(selectedTreeNode.value).map(String)) : null
  let serviceIds
  if (selectedModelIds) {
    const services = selectedModelIds.size ? treeServiceOptions.value : []
    // 空目录或未关联服务的模型仍传入筛选条件，避免退化成查询全部授权。
    serviceIds = services.filter(service => selectedModelIds.has(String(service.modelId)))
      .map(service => service.id).join(',') || '-1'
  }
  const response = await listServiceAuthorizations({
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    subjectId: params.subjectId || undefined,
    subjectType: params.subjectType ? toSubjectType(params.subjectType) : undefined,
    serviceId: params.serviceId || undefined,
    serviceIds,
    displayStatus: params.status === 'active' ? 'EFFECTIVE' : params.status === 'pending' ? 'INACTIVE' : undefined,
    orderByColumn: params.orderByColumn || undefined,
    isAsc: params.isAsc === 'ascending' ? 'asc' : params.isAsc === 'descending' ? 'desc' : undefined
  })
  const page = extractRows(response)
  page.rows.forEach(item => {
    // 只用真实服务名补充下拉选项，不再用“服务 + ID”编造名称。
    if (item.serviceId && item.serviceName
      && !serviceOptions.value.some(service => String(service.id) === String(item.serviceId))) {
      serviceOptions.value.push({ id: item.serviceId, name: item.serviceName, code: item.serviceCode || '-', version: item.serviceVersion || item.version || '-' })
    }
  })
  searchStore.items[2].component.options = serviceOptions.value.map(service => ({ label: service.name, value: service.id }))
  return { data: { rows: page.rows.map(mapAuthorization), total: page.total } }
}
function buildSavePayload(subjectIds, batch) {
  return {
    subjectType: toSubjectType(grantForm.subjectType),
    subjectId: batch ? undefined : subjectIds[0],
    subjectIds: batch ? subjectIds : undefined,
    serviceId: grantForm.serviceId,
    dataScopeType: grantForm.scopeType === 'condition' ? 'CONDITION' : 'ALL',
    dataScopeConfig: grantForm.scopeType === 'condition' ? { version: 1, groups: grantForm.scopeGroups.map(group => ({ logic: 'AND', conditions: group.conditions.filter(condition => String(condition.value ?? '').trim()).map(condition => ({ fieldId: fieldDefinitions.find(field => field.code === condition.field)?.id, fieldCode: condition.field, operator: condition.operator.toUpperCase(), value: String(condition.value).trim() })) })) } : null,
    // 服务授权字段只控制返回范围，不提交字段修改权限。
    fieldPermissions: grantForm.fieldPermissions.map(field => ({ fieldId: field.id, fieldCode: field.code, view: field.visible })),
    effectiveTime: toDateTime(grantForm.startDate),
    expireTime: grantForm.longTerm ? null : toDateTime(grantForm.endDate, true),
    longTerm: grantForm.longTerm
  }
}
async function loadReferences() {
  const [users, projects, layers, models, services] = await Promise.allSettled([
    userDirectoryStore.loadUserList(),
    listProject({ pageNum: 1, pageSize: -1 }),
    listLayer({ pageNum: 1, pageSize: -1, orderByColumn: 'sortOrder', isAsc: 'ascending' }),
    listBase({ pageNum: 1, pageSize: -1 }),
    listAllServices()
  ])
  treeServiceOptions.value = services.status === 'fulfilled' ? services.value : []
  if (services.status === 'rejected') proxy.$modal.msgError('数据服务加载失败，请刷新后重试')
  if ([projects, layers, models].every(result => result.status === 'fulfilled')) {
    modelTreeData.value = buildAuthorizationModelTree(
      extractRows(projects.value).rows, extractRows(layers.value).rows, extractRows(models.value).rows
    )
    modelTreeExpandedKeys.value = modelTreeData.value.map(node => node.treeKey)
  } else {
    proxy.$modal.msgError('类目树加载失败，请刷新后重试')
  }
  modelTreeLoading.value = false
  const userRows = users.status === 'fulfilled' ? extractRows(users.value).rows : []
  subjectOptions.value = [
    ...userRows.map(item => ({ id: item.userId, name: item.nickName, code: item.userName, type: 'user' })),
    ...clientSubjects
  ]
  searchStore.items[0].component.options = subjectOptions.value.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id }))
}

onMounted(loadReferences)
</script>

<style scoped lang="scss">
.authorization-main {
  min-width: 0;
  padding: 0;
}

.scope-condition-delete {
  display: inline-flex;
}

.service-info {
  min-width: 0;

  .service-info-title {
    display: block;

    &[role='link'] {
      cursor: pointer;
      color: var(--el-color-primary);
    }
  }

  :deep(.qt-table-info__main > .dict-tag-container) {
    flex-shrink: 0;
  }
}
.permission-line {
  margin: var(--qt-space-1) 0 0;
  color: var(--qt-text-regular);
  font-size: var(--qt-font-size-base);
  line-height: var(--qt-space-5);
}
.status-remark {
  margin: var(--qt-space-1) 0 0;
  color: var(--qt-text-secondary);
  font-size: var(--qt-font-size-small);
  line-height: 18px;
}

/* 列表操作列：按钮组在固定宽度列内居中，左右留出一致的边距。 */
.table-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}
.table-action :deep(.el-button + .el-button) {
  margin-left: 0;
}
.wizard-form {
  min-height: 390px;
}
.h2-titles {
  margin-bottom: var(--qt-space-5);
}
.wizard-block-error {
  margin: 0;
  padding-top: 2px;
  color: var(--el-color-danger);
  font-size: var(--qt-font-size-small);
  line-height: 1;
}

</style>
