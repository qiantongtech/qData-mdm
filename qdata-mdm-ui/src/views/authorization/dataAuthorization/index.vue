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
    <guide-tip tip-id="authorization/dataAuthorization.list" />
    <el-container>
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :editable="false"
        :use-builtin-cat-edit-dialog="false"
        :default-expanded-keys="modelTreeExpandedKeys"
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
              icon="Plus"
              v-hasPermi="['auth:data:add']"
              @click="handleCreate"
              @mousedown="(e) => e.preventDefault()"
            >
              新增
            </el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="!selection.rows.length"
              v-hasPermi="['auth:data:remove']"
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
              <dict-tag
                size="small"
                :options="subjectTypeOptions"
                :value="row.subjectType"
              />
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img
                    :src="getModelIcon({ icon: row.modelIcon, type: row.modelType })"
                    alt=""
                    class="info-icon"
                  />
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="canOpenModel(row) ? 'link' : undefined"
                    :tabindex="canOpenModel(row) ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ formatModelTitle(row) }}</span>
                  <dict-tag
                    v-if="row.modelType"
                    :options="model_type"
                    :value="row.modelType"
                    size="small"
                    type="primary"
                  />
                  <dict-tag
                    v-if="row.modelVersionNo"
                    class="ml4"
                    :class="{ pointer: canOpenModel(row) }"
                    :type="getVersionTagType(row)"
                    size="small"
                    :role="canOpenModel(row) ? 'link' : undefined"
                    :tabindex="canOpenModel(row) ? 0 : undefined"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  >{{ row.modelVersionNo }}</dict-tag>
                </div>
                <div class="info-desc text-ellipsis">
                  {{ row.modelDescription || '-' }}
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
              <p class="permission-line edit">可修改 {{ row.editableFields }} 个字段</p>
            </template>
            <template #operations="{ row }">
              <qt-tag-group
                :items="row.operations"
                type="none"
                :max-count="3"
              />
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
                  v-hasPermi="['auth:data:query']"
                  @click="openDetail(row)"
                >详情</el-button>
                <el-button
                  link
                  type="primary"
                  icon="Edit"
                  v-hasPermi="['auth:data:edit']"
                  @click="openEdit(row)"
                >修改</el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  v-hasPermi="['auth:data:remove']"
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
      :title="editingRecord ? '修改数据授权' : '新增数据授权'"
      width="min(1100px, calc(100% - 24px))"
      class="authorization-wizard scrollbar"
      destroy-on-close
      draggable
      :close-on-click-modal="false"
      :close-on-press-escape="!grantBusy"
      :show-close="!grantBusy"
    >
      <!-- 显示数据授权流程的当前步骤。 -->
      <auth-steps
        :steps="wizardSteps"
        :active-step="grantStep - 1"
        is-dialog
        :clickable="false"
      />
      <el-form
        ref="grantFormRef"
        :model="grantForm"
        :rules="grantRules"
        label-width="120px"
        class="wizard-form"
        :disabled="grantBusy"
        scroll-to-error
        @submit.prevent
      >
        <section v-show="grantStep === 1">
          <!-- 说明数据授权的最小单元和批量授权规则。 -->
          <guide-panel
            class="mb20"
            type="primary"
            allow-wrap
            title="数据授权以单个主数据模型为最小授权单元。新增时可选择一个或多个授权主体，同一套权限将分别授予每个所选主体。"
          />
          <div class="h2-titles">授权主体与模型</div>
          <div class="column-form">
            <qt-form-item
              label="主体类型"
              prop="subjectType"
              required
              class="row-full"
            >
              <el-radio-group v-model="grantForm.subjectType" @change="handleSubjectTypeChange">
                <el-radio value="user">用户</el-radio>
                <el-radio value="role">角色</el-radio>
                <el-radio value="organization">组织</el-radio>
              </el-radio-group>
            </qt-form-item>
            <qt-form-item
              v-if="grantMode !== 'batch'"
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
              <!-- 批量授权保留多选数据结构，输入方式与主数据模型下拉框一致。 -->
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
            <qt-form-item v-if="grantForm.subjectType === 'organization'" label="组织范围" class="row-full">
              <el-checkbox v-model="grantForm.includeChildOrg">包含全部下级组织成员</el-checkbox>
            </qt-form-item>
            <qt-form-item
              label="主数据域"
              prop="projectId"
              required
              class="row-full"
            >
              <el-select
                v-model="grantForm.projectId"
                placeholder="请选择主数据域"
                clearable
                filterable
                @change="handleGrantProjectChange"
              >
                <template #label="{ label }">
                  <div v-if="selectedProjectOption" class="source-option">
                    <img :src="getProjectIcon(selectedProjectOption.icon)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ selectedProjectOption.name }}</span>
                  </div>
                  <span v-else>{{ label }}</span>
                </template>
                <el-option
                  v-for="project in projectOptions"
                  :key="project.id"
                  :label="project.name"
                  :value="project.id"
                >
                  <div class="source-option">
                    <img :src="getProjectIcon(project.icon)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ project.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </qt-form-item>
            <qt-form-item label="模型目录" prop="layerId" class="row-full">
              <el-tree-select
                v-model="grantForm.layerId"
                :data="grantLayerOptions"
                :props="{ label: 'name', value: 'id', children: 'children' }"
                :placeholder="grantForm.projectId ? '请选择模型目录' : '请先选择主数据域'"
                :disabled="!grantForm.projectId"
                check-strictly
                clearable
                filterable
                @change="handleGrantLayerChange"
              />
            </qt-form-item>
            <qt-form-item
              label="主数据模型"
              prop="modelId"
              required
              class="row-full"
            >
              <el-select
                v-model="grantForm.modelId"
                :disabled="!grantForm.projectId"
                placeholder="请选择主数据模型"
                filterable
                @change="handleGrantModelChange"
              >
                <template #label="{ label }">
                  <div v-if="selectedModelOption" class="source-option">
                    <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ `${selectedModelOption.name}（${selectedModelOption.code}）` }}</span>
                  </div>
                  <span v-else>{{ label }}</span>
                </template>
                <el-option
                  v-for="model in grantModelOptions"
                  :key="model.id"
                  :label="`${model.name}（${model.code}）`"
                  :value="model.id"
                >
                  <div class="source-option">
                    <img :src="getModelIcon(model)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ `${model.name}（${model.code}）` }}</span>
                  </div>
                </el-option>
              </el-select>
            </qt-form-item>
            <qt-form-item label="描述" prop="remark" class="row-full">
              <el-input
                v-model="grantForm.remark"
                type="textarea"
                :rows="3"
                maxlength="256个字符"
                show-word-limit
                placeholder="请输入本次数据授权的描述"
              />
            </qt-form-item>
          </div>
        </section>
        <section v-show="grantStep === 2">
          <guide-panel
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
                  >添加条件</el-button>
                </div>
                <qt-table :columns="scopeColumns" :config="scopeTableConfig" :func="() => queryScopeConditions(group)">
                  <template #field="{ row }">
                    <el-select v-model="row.field" placeholder="请选择字段" @change="revalidateScopeGroups">
                      <el-option
                        v-for="field in scopeFieldOptions"
                        :key="field.value"
                        :label="field.label"
                        :value="field.value"
                      />
                    </el-select>
                  </template>
                  <template #operator="{ row }">
                    <el-select v-model="row.operator" placeholder="请选择运算符" @change="revalidateScopeGroups">
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
          <!-- 说明字段查看权限与修改权限的依赖关系。 -->
          <guide-panel
            class="mb20"
            type="primary"
            allow-wrap
            title="修改权限必须以查看权限为前提。未获查看权限的字段不展示；有查看无修改权限的字段只读。"
          />
          <div class="h2-titles">字段权限</div>
          <qt-table
            v-if="grantStep === 3"
            :columns="grantFieldColumns"
            :config="grantFieldTableConfig"
            :func="queryGrantFields"
          >
            <template #visible="{ row }">
              <el-checkbox v-model="row.visible" @change="handleVisibleChange(row)" />
            </template>
            <template #editable="{ row }">
              <el-checkbox v-model="row.editable" @change="handleEditableChange(row)" />
            </template>
          </qt-table>
          <p v-if="blockErrors.fieldPermissions" class="wizard-block-error">{{ blockErrors.fieldPermissions }}</p>
        </section>
        <section v-show="grantStep === 4">
          <div class="h2-titles">操作权限</div>
          <el-checkbox-group v-model="grantForm.operations" class="operation-grid">
            <el-checkbox
              v-for="operation in operationOptions"
              :key="operation"
              :value="operation"
              border
            >
              {{ operation }}
            </el-checkbox>
          </el-checkbox-group>
          <p v-if="blockErrors.operations" class="wizard-block-error">{{ blockErrors.operations }}</p>
          <div class="h2-titles validity-title">授权有效期</div>
          <div class="column-form">
            <qt-form-item label="申请周期" class="row-full">
              <el-radio-group v-model="grantForm.longTerm" @change="handleGrantPeriodChange">
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
                @change="handleGrantStartDateChange"
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
                :disabled-date="isBeforeGrantStartDate"
              />
            </qt-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="grantBusy" @click="grantOpen = false">取消</el-button>
          <el-button v-if="grantStep > 1" :disabled="grantBusy" @click="grantStep--">上一步</el-button>
          <el-button
            v-if="grantStep < wizardSteps.length"
            type="primary"
            :disabled="grantBusy"
            :loading="grantAdvancing || (grantStep === 1 && modelFieldsLoading)"
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
          >确定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="DataAuthorization">
import { getDisplayTitle } from '@/utils/anivia.js'
import AuthSteps from '@/components/Steps/index.vue'
import GuidePanel from '@/components/GuideTip/GuideTipPanel.vue'
import QtTree from '@/components/QtTree/index.vue'
import QtTable from '@/components/QtTable/index.vue'
import { useRouter } from 'vue-router'
import {
  DATA_AUTHORIZATION_OPERATIONS as operationMap,
  DATA_AUTHORIZATION_SUBJECT_TYPES as subjectTypeOptions
} from '@/utils/authorization/dataAuthorization.js'
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import { listRole } from '@/api/system/system/role.js'
import { listDept } from '@/api/system/system/dept.js'
import { listBase, getBase } from '@/api/model/base/base.js'

import { getProjectIcon, getModelIcon } from '@/utils/imageUrl'
import { useDict } from '@/utils/dict'
import { listProject } from '@/api/model/project/project.js'
import { listLayer } from '@/api/model/layer/layer.js'
import {
  batchGrantDataAuthorizations,
  batchDeleteDataAuthorizations,
  createDataAuthorization,
  getDataAuthorization,
  listDataAuthorizations,
  updateDataAuthorization
} from '@/api/authorization/dataAuthorization.js'

const { model_type, model_status } = useDict('model_type', 'model_status')
const userDirectoryStore = useUserDirectoryStore();

const { proxy } = getCurrentInstance()
const router = useRouter()
const canViewModel = computed(() => proxy.$auth.hasPermi('model:base:query'))
const tableRef = ref()
const modelTreeRef = ref()
const modelTreeData = ref([])
const modelTreeLoading = ref(true)
const modelTreeExpandedKeys = ref([])
const selectedTreeNode = ref(null)
const grantOpen = ref(false)
const grantMode = ref('single')
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
const projectOptions = ref([])
const layerOptions = ref([])
const modelOptions = ref([])
const modelFieldsLoading = ref(false)
let modelFieldsRequest = 0
const modelFieldCache = ref({})
// 全量模型行缓存（含子模型/未发布模型），用于列表展示模型名称、编码与图标；
// 下拉可选项 modelOptions 只保留顶层且带版本的模型，解析名称不能只用它。
const modelRowCache = ref({})

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
  { title: '主体与模型' },
  { title: '数据范围' },
  { title: '字段权限' },
  { title: '操作与有效期' }
]
const operationCodeMap = Object.fromEntries(Object.entries(operationMap).map(([code, label]) => [label, code]))
const operationOptions = Object.values(operationMap)
const fieldDefinitions = ref([])
const scopeFieldOptions = computed(() => fieldDefinitions.value.map(field => ({ label: field.name, value: field.code, id: field.id })))
const scopeOperatorOptions = [
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '包含', value: 'contains' }
]
let scopeConditionSeed = 0
function createScopeCondition() { return { id: `condition-${++scopeConditionSeed}`, field: fieldDefinitions.value[0]?.code || '', operator: 'eq', value: '' } }
function createScopeGroups() { return [{ id: `group-${++scopeConditionSeed}`, conditions: [createScopeCondition()] }] }
function createFieldPermissions() {
  return fieldDefinitions.value.map(field => ({ ...field, visible: true, editable: false }))
}
const grantForm = reactive({
  subjectType: 'user', subjectId: '', subjectIds: [], includeChildOrg: false,
  projectId: '', layerId: '', modelId: '', remark: '',
  scopeType: 'condition', scopeDetail: '', scopeGroups: createScopeGroups(),
  fieldPermissions: [], operations: ['查看'], startDate: '', endDate: '', longTerm: true
})
const filteredSubjectOptions = computed(() => subjectOptions.value.filter(item => item.type === grantForm.subjectType))
const grantLayerOptions = computed(() => proxy.handleTree(layerOptions.value
  .filter(layer => String(layer.projectId) === String(grantForm.projectId))
  .map(layer => ({ ...layer })), 'id', 'parentId'))
const grantModelOptions = computed(() => modelOptions.value.filter(model =>
  String(model.projectId) === String(grantForm.projectId)
  && (!grantForm.layerId || String(model.layerId) === String(grantForm.layerId))))
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
  { label: '允许查看', prop: 'visible', slot: 'visible', minWidth: 140 },
  { label: '允许修改', prop: 'editable', slot: 'editable', minWidth: 140 }
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
      scrollbarAlwaysOn: true,
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
    /* 内容列采用接近的最小宽度，剩余空间按列分配，避免列间距忽近忽远。 */
    { type: 'selection', width: 50, minWidth: 50 },
    { label: '编号', prop: 'id', width: 70, minWidth: 70, sortable: true },
    { label: '授权主体', info: { custom: 'subjectTitle', desc: 'subjectDescription' }, minWidth: 180, align: 'left' },
    { label: '主数据模型', slot: 'modelInfo', minWidth: 320, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '数据范围', slot: 'scope', minWidth: 180, align: 'left' },
    { label: '字段权限', slot: 'fields', minWidth: 180, align: 'left' },
    { label: '操作权限', slot: 'operations', minWidth: 200, align: 'left' },
    { label: '有效期', prop: 'validPeriod', minWidth: 200 },
    { label: '状态', slot: 'status', minWidth: 180 },
    { label: '操作', slot: 'action', width: 260, minWidth: 260, align: 'center', fixed: 'right' }
  ],
  params: { subjectId: '', subjectType: '', modelId: '', status: '' },
  func: fetchAuthorizationPage
})
const searchStore = reactive({
  items: [
    { label: '授权主体', prop: 'subjectId', component: { is: 'select', options: [] } },
    { label: '主体类型', prop: 'subjectType', component: { is: 'select', options: [{ label: '用户', value: 'user' }, { label: '角色', value: 'role' }, { label: '组织', value: 'organization' }] } },
    { label: '主数据模型', prop: 'modelId', component: { is: 'select', options: [], onChange: handleModelSelect } },
    { label: '授权状态', prop: 'status', component: { is: 'select', options: [{ label: '有效', value: 'active' }, { label: '未生效', value: 'pending' }] } }
  ]
})

function handleModelTreeClick(node) {
  selectedTreeNode.value = node
  tableStore.params.modelId = node.nodeType === 'model' ? node.modelId : ''
  selection.rows = []
  tableRef.value?.elTableRef?.clearSelection()
  tableRef.value?.resetQuery()
}

function handleModelSelect(modelId) {
  selectedTreeNode.value = null
  modelTreeRef.value?.setCurrentKey(modelId ? `model:${modelId}` : null, false)
}

function resetTreeSelection() {
  selectedTreeNode.value = null
  modelTreeRef.value?.setCurrentKey(null, false)
}

function resetGrantForm() {
  clearGrantModel()
  Object.assign(grantForm, { subjectType: 'user', subjectId: '', subjectIds: [], includeChildOrg: false, projectId: '', layerId: '', remark: '', scopeType: 'condition', scopeDetail: '', operations: ['查看'], startDate: formatDate(new Date()), endDate: '', longTerm: true })
  Object.assign(blockErrors, { scopeGroups: '', fieldPermissions: '', operations: '' })
  nextTick(() => grantFormRef.value?.clearValidate())
}
function handleCreate() { resetGrantForm(); editingRecord.value = undefined; grantStep.value = 1; grantMode.value = 'batch'; grantOpen.value = true }
function resetSubjects() { grantForm.subjectId = ''; grantForm.subjectIds = [] }
function handleSubjectTypeChange() {
  resetSubjects()
  grantForm.includeChildOrg = false
  grantFormRef.value?.clearValidate(['subjectId', 'subjectIds'])
}
function clearGrantModel() {
  modelFieldsRequest++
  modelFieldsLoading.value = false
  grantForm.modelId = ''
  fieldDefinitions.value = []
  grantForm.fieldPermissions = []
  grantForm.scopeGroups = createScopeGroups()
  blockErrors.scopeGroups = ''
  blockErrors.fieldPermissions = ''
}
function handleGrantProjectChange() {
  grantForm.layerId = ''
  clearGrantModel()
  grantFormRef.value?.clearValidate(['layerId', 'modelId'])
}
function handleGrantLayerChange() {
  clearGrantModel()
  grantFormRef.value?.clearValidate('modelId')
}
async function handleGrantModelChange(modelId) {
  const loaded = await loadModelFields(modelId)
  if (!loaded && grantForm.modelId === modelId) grantForm.modelId = ''
}
function handleGrantPeriodChange(longTerm) {
  if (longTerm) grantForm.endDate = ''
  grantFormRef.value?.clearValidate('endDate')
}
function handleGrantStartDateChange() {
  if (!grantForm.longTerm && grantForm.endDate) grantFormRef.value?.validateField('endDate').catch(() => {})
}
function isBeforeGrantStartDate(date) {
  return Boolean(grantForm.startDate && formatDate(date) < grantForm.startDate)
}
function handleVisibleChange(row) {
  if (!row.visible) row.editable = false
  blockErrors.fieldPermissions = fieldPermissionsMessage()
}
function handleEditableChange(row) { if (row.editable) row.visible = true }
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
const blockErrors = reactive({ scopeGroups: '', fieldPermissions: '', operations: '' })
const scopeGroupErrors = computed(() => {
  if (grantForm.scopeType !== 'condition' || !blockErrors.scopeGroups) return {}
  return Object.fromEntries(grantForm.scopeGroups.map((group, index) => [group.id, scopeGroupMessage(group, index)]))
})
const stepFields = {
  1: () => ['subjectType', grantMode.value === 'batch' ? 'subjectIds' : 'subjectId', 'projectId', 'modelId', 'remark'],
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
function operationsMessage() {
  return grantForm.operations.length ? '' : '请选择操作权限'
}
function revalidateScopeGroups() {
  blockErrors.scopeGroups = scopeGroupsMessage()
}
function validateBlockChecks(step) {
  blockErrors.scopeGroups = ''
  blockErrors.fieldPermissions = ''
  blockErrors.operations = ''
  if (step === 2) blockErrors.scopeGroups = scopeGroupsMessage()
  if (step === 3) blockErrors.fieldPermissions = fieldPermissionsMessage()
  if (step === 4) blockErrors.operations = operationsMessage()
  return !blockErrors.scopeGroups && !blockErrors.fieldPermissions && !blockErrors.operations
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
  projectId: [{ required: true, message: '请选择主数据域', trigger: 'change' }],
  modelId: [{ required: true, message: '请选择主数据模型', trigger: 'change' }],
  remark: [{ max: 256, message: '描述不能超过256个字符', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择数据范围模式', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  // 与表单项的动态 required 合并，保留中文必填提示。
  endDate: [{ required: false, message: '请选择失效时间', trigger: 'change' }, { validator: validateEndDate, trigger: 'change' }]
}
async function validateGrantStep(step) {
  if (step === 1 && modelFieldsLoading.value) {
    proxy.$modal.msgWarning('模型字段加载中，请稍后')
    return false
  }
  const blocksValid = validateBlockChecks(step)
  const fields = stepFields[step]()
  let invalidFields
  try {
    if (fields.length) await grantFormRef.value.validateField(fields)
  } catch (errors) {
    invalidFields = errors
  }
  if (blocksValid && !invalidFields) return true
  grantStep.value = step
  await nextTick()
  const firstInvalidField = invalidFields && Object.keys(invalidFields)[0]
  if (firstInvalidField) grantFormRef.value?.scrollToField(firstInvalidField)
  else grantFormRef.value?.$el.querySelector('.wizard-block-error')?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  return false
}
async function nextStep() {
  if (grantBusy.value || grantStep.value >= wizardSteps.length) return
  grantAdvancing.value = true
  try {
    if (await validateGrantStep(grantStep.value)) grantStep.value++
  } finally {
    grantAdvancing.value = false
  }
}
// 提交数据授权前依次校验四个步骤，并按新增、批量授权或修改调用对应接口。
async function submitGrant() {
  if (grantBusy.value) return
  grantSubmitting.value = true
  const subjectIds = editingRecord.value ? [grantForm.subjectId] : grantForm.subjectIds
  const isBatchCreate = !editingRecord.value && subjectIds.length > 1
  try {
    for (let step = 1; step <= wizardSteps.length; step++) {
      if (!(await validateGrantStep(step))) return
    }
    const payload = buildSavePayload(subjectIds, isBatchCreate)
    let batchResult
    if (editingRecord.value) await updateDataAuthorization({ ...payload, id: editingRecord.value.id, subjectIds: undefined })
    else if (isBatchCreate) batchResult = unwrapData(await batchGrantDataAuthorizations(payload))
    else await createDataAuthorization(payload)
    grantOpen.value = false
    tableRef.value?.getList()
    proxy.$modal.msgSuccess(editingRecord.value ? (isRevokedStatus(editingRecord.value.persistedStatus) ? '授权修改并恢复成功' : '授权修改成功') : isBatchCreate
      ? formatBatchResult(batchResult)
      : '授权成功')
  } catch {
    // 批量接口失败时反馈整批未保存；单条接口错误由项目统一请求处理器提示。
    if (isBatchCreate) proxy.$modal.msgError(`批量授权失败：成功 0 条，失败 ${subjectIds.length} 条；本次未保存任何授权`)
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
  proxy.$modal.confirm(`确认删除选中的 ${ids.length} 条数据授权吗？删除后授权立即失效。`).then(() => {
    return batchDeleteDataAuthorizations(ids)
  }).then(() => {
    selection.rows = []
    tableRef.value?.getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
// 删除单条授权时同时终止该主体对模型的访问权限。
function deleteAuthorization(row) {
  proxy.$modal.confirm(`确认删除“${row.subjectName}”在“${row.modelName}”的数据授权记录吗？删除后授权立即失效。`).then(() => {
    return batchDeleteDataAuthorizations([row.id])
  }).then(() => {
    tableRef.value?.getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}
function openDetail(row) {
  router.push({ path: '/authorization/dataAuthorization/detail', query: { id: row.id } })
}
// 获取授权详情和模型字段后，将已有权限回填到修改表单。
async function openEdit(row) {
  let detail
  try {
    detail = unwrapData(await getDataAuthorization(row.id))
  } catch {
    // 详情获取失败时不打开修改表单，错误提示由统一请求处理器负责。
    return
  }
  await ensureReferences()
  if (!await loadModelFields(detail.modelId, true)) return
  const model = modelRowCache.value[String(detail.modelId)] || {}
  Object.assign(grantForm, {
    subjectType: fromSubjectType(detail.subjectType),
    subjectId: detail.subjectId,
    subjectIds: [],
    includeChildOrg: Boolean(detail.includeChildOrg),
    projectId: model.projectId || '',
    layerId: model.layerId || '',
    modelId: detail.modelId,
    remark: detail.remark || '',
    scopeType: detail.dataScopeType === 'CONDITION' ? 'condition' : 'all',
    scopeGroups: deserializeScopeGroups(detail.dataScopeConfig),
    fieldPermissions: mergeFieldPermissions(detail.fieldPermissions),
    operations: (detail.operations || []).map(code => operationMap[code] || code),
    startDate: formatDate(detail.effectiveTime),
    endDate: formatDate(detail.expireTime),
    longTerm: Boolean(detail.longTerm)
  })
  editingRecord.value = detail
  grantMode.value = 'single'
  grantStep.value = 1
  grantOpen.value = true
  Object.assign(blockErrors, { scopeGroups: '', fieldPermissions: '', operations: '' })
  nextTick(() => grantFormRef.value?.clearValidate())
}
function toTag(label, type = 'primary') { return { label, value: label, elTagType: type } }

function unwrapData(response) { return response?.data ?? response }
function isRevokedStatus(value) { return value === '0' || value === 'REVOKED' }
function extractRows(response) {
  const data = unwrapData(response) || {}
  return { rows: data.rows || data.list || (Array.isArray(data) ? data : []), total: data.total || 0 }
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
function fromSubjectType(value) { return ({ USER: 'user', ROLE: 'role', ORG: 'organization' })[value] || value?.toLowerCase() }
function toSubjectType(value) { return ({ user: 'USER', role: 'ROLE', organization: 'ORG' })[value] }
function formatModelTitle(row) {
  const name = row.modelName === '-' ? '' : row.modelName
  const code = row.modelCode === '-' ? '' : row.modelCode
  return name ? (code ? `${name} (${code})` : name) : code || '-'
}
function getVersionTagType(row) {
  return model_status.value.find(item => String(item.value) === String(row.modelVersionStatus))?.elTagType || 'info'
}
function canOpenModel(row) {
  return canViewModel.value && row.modelId != null
}
function openModelDetail(row, tab) {
  if (!canOpenModel(row)) return
  router.push({
    path: '/model/base/detail',
    query: { id: row.modelId, ...(tab ? { tab } : {}) }
  })
}
function mapAuthorization(item) {
  const model = modelRowCache.value[String(item.modelId)] || modelOptions.value.find(option => String(option.id) === String(item.modelId))
  const scopeType = item.dataScopeType === 'CONDITION' ? 'condition' : 'all'
  const fieldPermissions = item.fieldPermissions || []
  const statusRemark = ({ REVOKED: '已撤销', NOT_STARTED: '未到生效时间', EXPIRED: '已过期', INVALID: '授权无效' })[item.inactiveReason] || ''
  return {
    ...item,
    modelName: item.modelName || model?.name || '-',
    modelCode: item.modelCode || model?.code || '-',
    modelIcon: item.modelIcon || model?.icon,
    modelType: item.modelType || model?.type,
    modelVersionNo: item.modelVersionNo || model?.releaseVersion?.versionNo || model?.version?.versionNo,
    modelVersionStatus: item.modelVersionStatus ?? model?.version?.status ?? model?.releaseVersion?.status,
    modelDescription: item.modelDescription || model?.description || '-',
    subjectType: fromSubjectType(item.subjectType),
    scopeType,
    scopeGroups: deserializeScopeGroups(item.dataScopeConfig),
    scopeSummary: scopeType === 'condition' ? summarizeScope(item.dataScopeConfig) : '',
    scopeDetail: scopeType === 'condition' ? summarizeScope(item.dataScopeConfig) : '',
    visibleFields: item.viewFieldCount ?? fieldPermissions.filter(field => field.view).length,
    editableFields: item.editFieldCount ?? fieldPermissions.filter(field => field.edit).length,
    fieldNames: fieldPermissions
      .filter(field => field.view)
      .map(field => field.fieldName || field.fieldCode)
      .filter(name => name && name !== '-'),
    operations: (item.operations || []).map(code => operationMap[code] || code),
    validPeriod: item.longTerm ? '长期有效' : `${formatDate(item.effectiveTime)} ~ ${formatDate(item.expireTime)}`,
    status: item.displayStatus === 'EFFECTIVE' ? 'active' : 'pending',
    statusRemark
  }
}
async function fetchAuthorizationPage(params) {
  // QtTable 在自身 setup 阶段就会发起首次查询，早于父组件 onMounted 的引用加载，
  // 因此这里先等模型引用加载完成，避免首屏把模型名/编码解析成 ID 兜底。
  await ensureReferences()
  const response = await listDataAuthorizations({
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    subjectId: params.subjectId || undefined,
    subjectType: params.subjectType ? toSubjectType(params.subjectType) : undefined,
    modelId: params.modelId || undefined,
    // 空目录传入不可能匹配的编号，避免空数组被查询参数序列化器忽略后返回全部授权。
    modelIds: selectedTreeNode.value && selectedTreeNode.value.nodeType !== 'model'
      ? (collectAuthorizationModelIds(selectedTreeNode.value).join(',') || '-1') : undefined,
    displayStatus: params.status === 'active' ? 'EFFECTIVE' : params.status === 'pending' ? 'INACTIVE' : undefined,
    orderByColumn: params.orderByColumn || undefined,
    isAsc: params.isAsc === 'ascending' ? 'asc' : params.isAsc === 'descending' ? 'desc' : undefined
  })
  const page = extractRows(response)
  return { data: { rows: page.rows.map(mapAuthorization), total: page.total } }
}
function deserializeScopeGroups(config) {
  if (!config?.groups?.length) return createScopeGroups()
  return config.groups.map(group => ({ id: `group-${++scopeConditionSeed}`, conditions: (group.conditions || []).map(condition => ({ id: `condition-${++scopeConditionSeed}`, field: condition.fieldCode, operator: String(condition.operator || 'EQ').toLowerCase(), value: condition.value || '' })) }))
}
function summarizeScope(config) {
  const labels = Object.fromEntries(scopeFieldOptions.value.map(item => [item.value, item.label]))
  const operators = { EQ: '等于', NE: '不等于', CONTAINS: '包含' }
  return (config?.groups || []).map(group => (group.conditions || []).map(condition => `${labels[condition.fieldCode] || condition.fieldCode} ${operators[condition.operator] || condition.operator} ${condition.value}`).join(' 且 ')).filter(Boolean).join(' 或 ')
}
function mergeFieldPermissions(saved = []) {
  const savedMap = new Map(saved.map(item => [String(item.fieldId ?? item.fieldCode), item]))
  return fieldDefinitions.value.map(field => {
    const permission = savedMap.get(String(field.id)) || savedMap.get(field.code)
    return { ...field, visible: Boolean(permission?.view), editable: Boolean(permission?.edit) }
  })
}
function buildSavePayload(subjectIds, batch = grantMode.value === 'batch') {
  return {
    subjectType: toSubjectType(grantForm.subjectType),
    subjectId: batch ? undefined : subjectIds[0],
    subjectIds: batch ? subjectIds : undefined,
    modelId: grantForm.modelId,
    remark: grantForm.remark.trim(),
    dataScopeType: grantForm.scopeType === 'condition' ? 'CONDITION' : 'ALL',
    dataScopeConfig: grantForm.scopeType === 'condition' ? { version: 1, groups: grantForm.scopeGroups.map(group => ({ logic: 'AND', conditions: group.conditions.filter(condition => condition.value.trim()).map(condition => ({ fieldId: scopeFieldOptions.value.find(field => field.value === condition.field)?.id, fieldCode: condition.field, operator: condition.operator.toUpperCase(), value: condition.value.trim() })) })) } : null,
    includeChildOrg: grantForm.subjectType === 'organization' && grantForm.includeChildOrg,
    fieldPermissions: grantForm.fieldPermissions.map(field => ({ fieldId: field.id, fieldCode: field.code, view: field.visible, edit: field.editable })),
    operations: grantForm.operations.map(label => operationCodeMap[label]),
    effectiveTime: toDateTime(grantForm.startDate),
    expireTime: grantForm.longTerm ? null : toDateTime(grantForm.endDate, true),
    longTerm: grantForm.longTerm
  }
}
async function loadModelFields(modelId, verifyExists = false) {
  if (!modelId) return false
  const request = ++modelFieldsRequest
  modelFieldsLoading.value = true
  fieldDefinitions.value = []
  grantForm.fieldPermissions = []
  let fields = modelFieldCache.value[String(modelId)] || []
  try {
    if (verifyExists || !fields.length) {
      try {
        const response = await getBase(modelId)
        const model = response?.data
        if (request !== modelFieldsRequest) return false
        if (!model) {
          proxy.$modal.msgWarning(`关联的主数据模型不存在（ID：${modelId}），请先核对模型数据`)
          return false
        }
        fields = model?.fields || []
        modelFieldCache.value[String(modelId)] = fields
        modelRowCache.value[String(modelId)] = model
      } catch {
        if (request === modelFieldsRequest) proxy.$modal.msgError('模型字段加载失败，请重试')
        return false
      }
    }
    if (request !== modelFieldsRequest) return false
    const definitions = fields.filter(field => field.id && field.code)
      .map(field => ({ id: field.id, name: field.name || field.code, code: field.code }))
    if (!definitions.length) {
      proxy.$modal.msgWarning('该主数据模型没有可授权字段')
      return false
    }
    fieldDefinitions.value = definitions
    grantForm.fieldPermissions = createFieldPermissions()
    grantForm.scopeGroups = createScopeGroups()
    return true
  } finally {
    if (request === modelFieldsRequest) modelFieldsLoading.value = false
  }
}
async function loadReferences() {
  const [users, roles, departments, models, projects, layers] = await Promise.allSettled([
    userDirectoryStore.loadUserList(), listRole({ pageNum: 1, pageSize: 1000 }), listDept({}),
    listBase({ pageNum: 1, pageSize: -1 }),
    listProject({ pageNum: 1, pageSize: -1 }),
    listLayer({ pageNum: 1, pageSize: -1, orderByColumn: 'sortOrder', isAsc: 'ascending' })
  ])
  const append = (result, type, idKey, nameKey, codeKey) => result.status === 'fulfilled' ? extractRows(result.value).rows.map(item => ({ id: item[idKey], name: item[nameKey], code: item[codeKey] || String(item[idKey]), type })) : []
  subjectOptions.value = [
    ...append(users, 'user', 'userId', 'nickName', 'userName'),
    ...append(roles, 'role', 'roleId', 'roleName', 'roleKey'),
    ...append(departments, 'organization', 'deptId', 'deptName', 'deptCode')
  ]
  const modelRows = models.status === 'fulfilled' ? extractRows(models.value).rows : []
  projectOptions.value = projects.status === 'fulfilled' ? extractRows(projects.value).rows : []
  layerOptions.value = layers.status === 'fulfilled' ? extractRows(layers.value).rows : []
  if ([projects, layers, models].every(result => result.status === 'fulfilled')) {
    modelTreeData.value = buildAuthorizationModelTree(extractRows(projects.value).rows, extractRows(layers.value).rows, modelRows)
    modelTreeExpandedKeys.value = modelTreeData.value.map(node => node.treeKey)
    nextTick(() => modelTreeRef.value?.setCurrentKey(null, false))
  } else {
    proxy.$modal.msgError('类目树加载失败，请刷新后重试')
  }
  modelTreeLoading.value = false
  // 下拉框只列顶层主数据模型，但编辑历史授权时需要任意模型的字段，因此单独缓存一份全量映射。
  modelFieldCache.value = Object.fromEntries(modelRows
    .filter(model => model.id)
    .map(model => [String(model.id), model.fields || []]))
  modelRowCache.value = Object.fromEntries(modelRows
    .filter(model => model.id)
    .map(model => [String(model.id), model]))
  modelOptions.value = modelRows
    .filter(model => model.id && model.versionId && (model.parentId == null || Number(model.parentId) === 0))
    .map(model => ({
      id: model.id, name: model.name, code: model.code, projectId: model.projectId, layerId: model.layerId,
      icon: model.icon, type: model.type, parentId: model.parentId
    }))
  if (models.status === 'rejected') proxy.$modal.msgError('主数据模型列表加载失败')
  searchStore.items[0].component.options = subjectOptions.value.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id }))
  searchStore.items[2].component.options = modelOptions.value.map(item => ({ label: `${item.name}（${item.code}）`, value: item.id }))
}

let referencesPromise = null
function ensureReferences() {
  if (!referencesPromise) referencesPromise = loadReferences()
  return referencesPromise
}

onMounted(ensureReferences)

const selectedProjectOption = computed(() => projectOptions.value.find(item => String(item.id) === String(grantForm.projectId)));
const selectedModelOption = computed(() => grantModelOptions.value.find(item => String(item.id) === String(grantForm.modelId)));
</script>

<style scoped lang="scss">
.authorization-main {
  min-width: 0;
  padding: 0;
}

.model-info {
  min-width: 0;

  .model-info-title {
    display: block;

    &[role='link'] {
      cursor: pointer;
      color: var(--el-color-primary);
    }
  }
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}

.scope-condition-delete {
  display: inline-flex;
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

.operation-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px 14px;
}
.operation-grid :deep(.el-checkbox) {
  width: 100%;
  height: 42px;
  margin-right: 0;
  padding: 0 14px;
}
/* 仅调整本页操作选项外框，保留项目组件自身的勾选状态。 */
.operation-grid :deep(.el-checkbox.is-bordered) {
  border-radius: var(--qt-radius-small);
}

/* 有效期标题与上一组操作权限保持间距。 */
.validity-title {
  margin-top: 28px !important;
}

.source-option {
  display: flex;
  align-items: center;
  gap: var(--qt-space-2);
  min-width: 0;
}

.source-option__icon {
  width: var(--qt-space-4);
  height: var(--qt-space-4);
  flex: 0 0 var(--qt-space-4);
  object-fit: contain;
}

@media (max-width: 768px) {
  .wizard-form .column-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .wizard-form .column-form .row-full {
    grid-column: 1 / -1;
  }

  .operation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
