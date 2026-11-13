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
  <div :class="{ 'app-container': !props.embedded }" >
    <GuideTip v-if="!props.embedded" tip-id="rule/match.list"/>
    <el-container>
      <!-- 模型树：规则管理页面展示，模型详情内隐藏。 -->
      <qt-tree
          v-if="!props.embedded"
          ref="modelTreeRef"
          :action-loading="modelTreeLoading"
          :data="modelTreeData"
          :use-builtin-cat-edit-dialog="false"
          :tree-props="{ label: 'name', children: 'children' }"
          node-key="treeKey"
          title="主数据模型"
          placeholder="搜索名称或编码"
          :default-expand="false"
          @node-click="handleModelTreeNodeClick"
      />

      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef" :config="tableStore.config">
          <!-- 搜索栏：规则管理页面展示，模型详情内隐藏。 -->
          <template v-if="!props.embedded" #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                :tableRef="tableRef"
            />
          </template>
          <template #actions-data>
            <el-button
                type="primary"
                plain
                icon="Plus"
                @click="handleAdd"
                v-hasPermi="['rule:rulematch:add']"
            >
              新增
            </el-button>
            <el-button
                type="danger"
                plain
                icon="Delete"
                :disabled="multiple"
                @click="handleDelete"
                v-hasPermi="['rule:rulematch:remove']"
            >
              批量删除
            </el-button>
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #location="{ row }">
              <!-- 目录按从父到子的顺序返回，最后一项就是模型直接所属的目录。 -->
              <div v-if="row.projectName || row.layers?.length" class="flex-column flex-column-gap">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(row.projectIcon)" alt=""/>
                  <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
                </div>
                <div v-if="row.layerName" class="text-ellipsis flex-center">
                  <dict-tag size="small" type="primary" :value="row.layerName"/>
                </div>
              </div>
              <span v-else class="text-muted">-</span>
            </template>
            <template #responsibleUser="{ row }">
              <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)"
                >{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text"
                >{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #ruleType="{ row }">
              <el-tag effect="plain">{{ getRuleTypeLabel(row.ruleType) }}</el-tag>
            </template>
            <template #ruleConfig="{ row }">
              {{ formatRuleConfig(row) }}
            </template>
            <template #validFlag="{ row }">
              <div class="status-cell">
                <el-switch
                    v-model="row.validFlag"
                    active-color="#13ce66"
                    inactive-color="#ff4949"
                    :loading="statusLoading[row.id]"
                    @change="handleStatusChange(row)"
                />
                <span v-if="isAbnormalRow(row)" class="status-badge is-abnormal">
                  <i></i>配置异常
                </span>
              </div>
            </template>
            <template #action="{ row }">

              <el-button
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(row)"
                  v-hasPermi="['rule:rulematch:edit']"
              >详情
              </el-button>
              <el-button
                  link
                  type="primary"
                  icon="Edit"
                  @click="handleUpdate(row)"
                  v-hasPermi="['rule:rulematch:edit']"
              >修改
              </el-button>
              <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(row)"
                  v-hasPermi="['rule:rulematch:remove']"
              >删除
              </el-button>

            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 添加或修改匹配规则对话框 -->
    <el-dialog :title="title" v-model="open" width="1000px" draggable>
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="rulematchRef" :model="form" :rules="rules" label-width="110px" class="column-form" @submit.prevent>
        <div class="h2-title row-full">基础信息</div>
        <qt-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入名称，支持中英文、数字、下划线和短横线。"/>
        </qt-form-item>
        <qt-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则编码，仅支持英文字母、数字和下划线。"/>
        </qt-form-item>
        <qt-form-item label="主数据域" required>
          <el-select
              v-model="modelProjectId"
              class="field-select"
              placeholder="请选择主数据域"
              :loading="modelTreeLoading"
              :disabled="props.embedded"
              clearable
              filterable
              @change="handleProjectChange"
          >
            <template #label="{ label }">
              <div v-if="selectedProjectOption" class="source-option">
                <img :src="getProjectIcon(selectedProjectOption.icon)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ selectedProjectOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option
                v-for="item in projectOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            >
              <div class="source-option">
                <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>

        <qt-form-item label="模型目录" required>
          <el-tree-select
              v-model="modelLayerId"
              class="field-select"
              :data="layerOptions"
              :placeholder="modelProjectId != null ? '请选择模型目录' : '请先选择主数据域'"
              :disabled="props.embedded || modelProjectId == null"
              check-strictly
              clearable
              filterable
              @change="handleLayerChange"
          />
        </qt-form-item>

        <qt-form-item label="主数据模型" prop="modelId" required>
          <!-- 禁止切换表单所属模型 -->
          <el-select
              v-model="form.modelId"
              class="field-select"
              :placeholder="modelLayerId != null ? '请选择主数据模型' : '请先选择模型目录'"
              :disabled="props.embedded || modelLayerId == null"
              filterable
              @change="handleModelChange"
          >
            <template #label="{ label }">
              <div v-if="selectedModelOption" class="source-option">
                <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ selectedModelOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option
                v-for="item in filteredModelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            >
              <div class="source-option">
                <img :src="getModelIcon(item)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>

        <qt-form-item
            label="负责人"
            prop="responsibleUserId"
        >
          <el-select
              v-model="form.responsibleUserId"
              class="field-select"
              clearable
              filterable
              :loading="usersLoading"
              placeholder="请选择负责人"
              @clear="form.responsibleUserId = null"
          >
            <el-option v-for="user in responsibleUserOptions" :key="user.value"
                       :label="user.label" :value="user.value"
            />
          </el-select>
          <el-button v-if="usersError" link type="primary" @click="loadResponsibleUsers">重新加载负责人</el-button>
        </qt-form-item>

        <qt-form-item label="联系电话" >
          <el-input :model-value="responsibleUserPhone" disabled placeholder="请输入联系电话"/>
        </qt-form-item>

        <el-form-item label="状态" prop="validFlag">
          <el-radio-group v-model="form.validFlag">
            <el-radio :label="true">启用</el-radio>
            <el-radio :label="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <qt-form-item label="描述" class="row-full" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              placeholder="请输入描述"
              :min-height="192"
              show-word-limit
              maxlength="256个字符"
          />
        </qt-form-item>

        <div class="h2-title row-full">规则配置</div>

        <qt-form-item label="匹配类型" prop="ruleType" tip="字段原始值壳全一致时判定匹配。">
          <el-select v-model="form.ruleType" placeholder="请选择匹配类型" @change="handleRuleTypeChange">
            <el-option v-for="item in rule_match_type" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </qt-form-item>

        <qt-form-item label="匹配分值" prop="ruleScore" tip="本规则命中时计入的分值，取值范围 0 ~ 100。">
          <el-input-number v-model="form.ruleScore" class="number-input" :min="0" :max="100"
                           controls-position="right" placeholder="0 ~ 100"
          />
        </qt-form-item>

        <div class="column-form row-full">
          <template v-if="form.ruleType === 'FUZZY' && fieldRows.length">

            <qt-form-item label="参与字段" tip="选择用于执行模糊相似比较的主数据字段。">
              <el-select
                  v-model="fieldRows[0].modelFieldId"
                  class="field-select"
                  placeholder="请选择参与字段"
              >
                <el-option
                    v-for="field in currentFieldOptions"
                    :key="field.value"
                    :label="field.label"
                    :value="field.value"
                />
              </el-select>
            </qt-form-item>

            <qt-form-item label="相似度阈值" tip="相似度达到法值时匹配:数值亮，要求严格。">
              <el-input-number
                  v-model="configDraft.similarityThreshold"
                  class="number-input"
                  :min="0"
                  :max="100"
                  controls-position="right"
              />
            </qt-form-item>

          </template>
          <template v-if="EXACT_RULE_TYPES.has(form.ruleType) && fieldRows.length">

            <qt-form-item label="参与字段" tip="选择需要进行比较的主数据字段。">
              <el-select v-model="fieldRows[0].modelFieldId" class="field-select" placeholder="请选择参与字段">
                <el-option v-for="field in currentFieldOptions" :key="field.value" :label="field.label"
                           :value="field.value"
                />
              </el-select>
            </qt-form-item>

            <template v-if="form.ruleType === 'STD_EXACT'">
              <qt-form-item label="标准化处理" tip="选择比较前的预处理方式，使数据格式保持一致。">
                <el-select ref="standardSelectRef" v-model="configDraft.standardOptions" class="field-select" multiple
                           collapse-tags collapse-tags-tooltip :max-collapse-tags="standardVisibleTags"
                           placeholder="请选择标准化处理"
                >
                  <el-option v-for="item in standardOptions" :key="item.value" :label="item.label" :value="item.value"/>
                </el-select>
                <div ref="standardTagMeasureRef" class="standard-tag-measure" aria-hidden="true">
                  <el-tag v-for="value in configDraft.standardOptions" :key="value" type="info" closable
                          disable-transitions
                  >
                    {{ standardOptions.find(item => item.value === value)?.label }}
                  </el-tag>
                  <el-tag type="info" disable-transitions>+ {{ configDraft.standardOptions.length }}</el-tag>
                </div>
              </qt-form-item>
            </template>
          </template>
          <template v-if="form.ruleType === 'WEIGHTED'">

            <qt-form-item label="权重合计" tip="各参与字段的权重合计必线为 100%。">
              <el-input :model-value="`${totalWeight}%`" disabled/>
            </qt-form-item>

            <qt-form-item label="最终匹配阈值" tip="加权汇总得分达到读值时，判定本受规则命中。">
              <el-input-number v-model="configDraft.finalThreshold" class="number-input" :min="0" :max="100"
                               controls-position="right" placeholder="0 ~ 100"
              />
            </qt-form-item>

          </template>
          <div v-if="form.ruleType === 'WEIGHTED'" class="h2-title row-full">组合字段配置</div>
          <div v-if="form.ruleType === 'WEIGHTED'" class="row-full">
            <el-button type="primary" plain icon="Plus" size="small" @click="addFieldRow">添加字段</el-button>
          </div>
          <qt-form-item v-if="form.ruleType === 'WEIGHTED'" class="weighted-table-wrap row-full" label-width="0"
                        tip="示例：字段 A 得分 100，权重 60%；字段 B 得分 80，权重 40%；加权得分为 92。"
          >
            <qt-table ref="weightedEditTable"
                      :columns="weightedColumns" :config="weightedTableConfig" :func="loadWeightedRows"
            >
              <template #modelFieldId="{ row, $index }">
                <el-select v-model="row.modelFieldId" class="field-select" placeholder="请选择参与字段">
                  <el-option v-for="field in currentFieldOptions" :key="field.value"
                             :label="field.label" :value="field.value"
                             :disabled="isFieldOptionDisabled(field.value, $index)"
                  />
                </el-select>
              </template>
              <template #matchMethod="{ row }">
                <el-select v-model="row.matchMethod" class="field-select" placeholder="请选择匹配方法">
                  <el-option label="精确匹配" value="EXACT"/>
                  <el-option label="模糊相似" value="FUZZY"/>
                </el-select>
              </template>
              <template #weight="{ row }">
                <el-input-number v-model="row.weight" class="number-input" :min="0" :max="100"
                                 controls-position="right"
                />
              </template>
              <template #fieldThreshold="{ row }">
                <el-input-number v-model="row.fieldThreshold" class="number-input" :min="0" :max="100"
                                 controls-position="right"
                />
              </template>
              <template #operation="{ $index }">
                <el-button type="danger" link icon="Delete" :disabled="fieldRows.length <= 1"
                           @click="removeFieldRow($index)"
                >删除
                </el-button>
              </template>
            </qt-table>
          </qt-form-item>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Rulematch">
import {getModelIcon, defaultProjectIcon, getProjectIcon} from "@/utils/imageUrl";
import {useDict} from "@/utils/dict";
import {listRulematch, getRulematch, delRulematch, addRulematch, updateRulematch} from "@/api/rule/match/match";
import QtTree from "@/components/QtTree/index.vue";
import {getPublishedModelStructure} from "@/api/model/base/base";
import {usePublishedModelTree, withModelTreeKeys} from "@/composables/usePublishedModelTree";
import {useUserDirectory} from '@/store/system/userDirectory';
import {trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";
import {useRoute, useRouter} from "vue-router";

const {responsibleUserList, getResponsibleName, getResponsiblePhone, loadResponsibleUserOptions} = useUserDirectory();

// 页面参数：支持模型详情内嵌，并接收当前模型信息。
//  embedded 默认 false；详情页传入 true，隐藏模型树、调整外层留白
//modelId  详情页传入当前模型 ID，固定查询和保存的模型归属
const props = defineProps({
  embedded: {type: Boolean, default: false},
  modelId: {type: [String, Number], default: null},
  modelDetail: {type: Object, default: () => ({})},
});
const route = useRoute();
const router = useRouter();

const {proxy} = getCurrentInstance();
const {rule_match_type} = useDict(
    "rule_match_type"
);
const tableRef = ref(null);
const statusLoading = reactive({});
const usersLoading = ref(false);
const usersError = ref(false);

const responsibleUserPhone = computed(() => getResponsiblePhone(form.value.responsibleUserId));
const responsibleUserOptions = computed(() => {
  const id = form.value.responsibleUserId;
  const users = responsibleUserList.value;
  if (id != null && id !== '' && !users.some(user => user.value === String(id))) {
    return [{value: String(id), label: `用户 ${id}（已不可用）`, phone: ''}, ...users];
  }
  return users;
});

async function loadResponsibleUsers() {
  usersLoading.value = true;
  usersError.value = false;
  try {
    await loadResponsibleUserOptions(form.value.responsibleUserId);
  } catch {
    usersError.value = true;
  } finally {
    usersLoading.value = false;
  }
}

const open = ref(false);
const ids = ref([]);
const multiple = ref(true);
const title = ref("");
const {
  data: modelTreeData,
  loading: modelTreeLoading,
  reload: reloadModelTree
} = usePublishedModelTree(withModelTreeKeys, !props.embedded);
const modelProjectId = ref(null);
const modelLayerId = ref(null);

const projectOptions = computed(() => {
  if (props.embedded) return [{
    value: props.modelDetail.projectId,
    icon: props.modelDetail.projectIcon,
    label: props.modelDetail.projectName || String(props.modelDetail.projectId || ""),
  }];
  return modelTreeData.value.map(node => ({value: node.projectId, label: node.name || node.code, icon: node.icon}));
});

const layerOptions = computed(() => {
  if (props.embedded) return [{
    value: props.modelDetail.layerId,
    label: props.modelDetail.layers?.map(item => item.name).join(" / ") || String(props.modelDetail.layerId || ""),
  }];
  const project = modelTreeData.value.find(node => String(node.projectId) === String(modelProjectId.value));
  const toLayerOptions = nodes => (nodes || [])
      .filter(node => node.nodeType === "layer")
      .map(node => ({value: node.layerId, label: node.name || node.code, children: toLayerOptions(node.children)}));
  return toLayerOptions(project?.children);
});

// 模型选项：详情内使用传入数据，独立入口保留原有模型列表。
const publishedModelFields = reactive({});
const publishedModelOptions = computed(() => {
  const models = [];
  const visit = (nodes) => {
    for (const node of nodes || []) {
      if (node.modelId != null) {
        models.push({
          value: node.modelId,
          treeKey: node.treeKey,
          projectId: node.projectId,
          layerId: node.layerId,
          label: node.name || node.code || String(node.modelId),
          version: node.versionNo || "",
          fields: publishedModelFields[node.modelId] || [],
        });
      }
      visit(node.children);
    }
  };
  visit(modelTreeData.value);
  return models;
});

const modelOptions = computed(() => props.embedded ? [{
  value: props.modelId,
  icon: props.modelDetail.icon,
  type: props.modelDetail.type,
  parentId: props.modelDetail.parentId,
  label: props.modelDetail.name || props.modelDetail.code || String(props.modelId || ""),
  version: props.modelDetail.version?.versionNo || "",
  fields: (props.modelDetail.fields || []).map(field => ({
    value: field.id,
    label: field.name || field.code,
    name: field.name || field.code,
    code: field.code,
    type: field.dataType,
  })),
}] : publishedModelOptions.value);

const filteredModelOptions = computed(() => props.embedded ? modelOptions.value : modelOptions.value.filter(item =>
    modelProjectId.value != null && modelLayerId.value != null &&
    String(item.projectId) === String(modelProjectId.value) && String(item.layerId) === String(modelLayerId.value)
));

/** 上级归属变化后清空模型及其字段配置。 */
function handleProjectChange() {
  modelLayerId.value = null;
  handleLayerChange();
}

function handleLayerChange() {
  form.value.modelId = null;
  form.value.modelVersion = null;
  form.value.modelFieldId = null;
  form.value.ruleConfig = "{}";
  resetFieldSelection();
}

/** 根据模型回显归属，域和目录只用于选择，不重复保存到规则。 */
function syncModelLocation() {
  const model = props.embedded ? props.modelDetail : getModelOption(form.value.modelId);
  modelProjectId.value = model?.projectId ?? null;
  modelLayerId.value = model?.layerId ?? null;
}

const standardOptions = [
  {label: "去除首尾空格", value: "TRIM"},
  {label: "连续空格处理", value: "SPACES"},
  {label: "大小写统一", value: "CASE"},
  {label: "全半角统一", value: "WIDTH"},
  {label: "常见标点处理", value: "PUNCT"},
];
const DEFAULT_STANDARD_OPTIONS = Object.freeze(["TRIM", "SPACES", "CASE", "WIDTH", "PUNCT"]);
const DEFAULT_SIMILARITY_THRESHOLD = 88;
const DEFAULT_FUZZY_FIELD_THRESHOLD = 88;
const EXACT_RULE_TYPES = new Set(["EXACT", "STD_EXACT"]);

const modelTreeRef = ref(null);
const selectedModelId = ref(props.embedded ? props.modelId : null);
const fieldRows = ref([]);
const weightedEditTable = ref(null);
const weightedTableConfig = {notPagination: true, table: {border: false, stripe: false}};
const weightedDetailColumns = [
  {label: "参与字段", prop: "modelFieldId", slot: "modelFieldId", minWidth: 200, align: "left"},
  {label: "匹配方法", prop: "matchMethod", slot: "matchMethod", minWidth: 140},
  {label: "权重（%）", prop: "weight", slot: "weight", minWidth: 130},
  {label: "字段阈值", prop: "fieldThreshold", slot: "fieldThreshold", minWidth: 130},
];
const weightedColumns = [...weightedDetailColumns,
  {label: "操作", slot: "operation", width: 80},
];

function loadWeightedRows() {
  return Promise.resolve({data: fieldRows.value});
}

watch(fieldRows, async () => {
  await nextTick();
  weightedEditTable.value?.getList();
});

const configDraft = reactive({
  standardOptions: [],
  similarityThreshold: 0,
  finalThreshold: null,
});
const standardSelectRef = ref(null);
const standardTagMeasureRef = ref(null);
const standardVisibleTags = ref(1);
let standardSelectObserver;

function updateStandardVisibleTags() {
  const selection = standardSelectRef.value?.$el?.querySelector('.el-select__selection');
  const tags = standardTagMeasureRef.value?.children;
  const count = configDraft.standardOptions.length;
  if (!selection || !tags || !count) return;
  const gap = parseFloat(getComputedStyle(selection).gap) || 6;
  const available = selection.clientWidth;
  const widths = Array.from(tags).slice(0, count).map(tag => tag.getBoundingClientRect().width);
  const total = widths.reduce((sum, width) => sum + width, 0) + gap * (count - 1);
  if (total <= available) {
    standardVisibleTags.value = count;
    return;
  }
  const counterWidth = tags[count].getBoundingClientRect().width;
  let used = 0;
  let visible = 0;
  for (const width of widths) {
    if (used + width + gap + counterWidth > available) break;
    used += width + gap;
    visible++;
  }
  standardVisibleTags.value = Math.max(1, visible);
}

watch(standardSelectRef, async (select) => {
  standardSelectObserver?.disconnect();
  if (!select) return;
  await nextTick();
  standardSelectObserver = new ResizeObserver(updateStandardVisibleTags);
  standardSelectObserver.observe(select.$el);
  updateStandardVisibleTags();
});
watch(() => [...configDraft.standardOptions], async () => {
  await nextTick();
  updateStandardVisibleTags();
});
onBeforeUnmount(() => standardSelectObserver?.disconnect());

const totalWeight = computed(() =>
    fieldRows.value.reduce((sum, item) => sum + (Number(item.weight) || 0), 0)
);
const currentModelOption = computed(() =>
    modelOptions.value.find((item) => String(item.value) === String(form.value.modelId))
);
const currentFieldOptions = computed(() => currentModelOption.value?.fields || []);
//	初始化查询模型；内嵌查询不限定固定版本
const data = reactive({
  form: {},
  queryParams: {
    ruleCode: null,
    ruleName: null,
    modelId: null,
    modelVersion: null,
    priority: null,
    validFlag: null,
    abnormalFlag: null,
    modelFieldId: null,
    ruleType: null,
    ruleConfig: null,
    ruleScore: null,
    description: null,
    createBy: null,
    createTime: null,
    updateBy: null,
    updateTime: null,
    orderByColumn: "create_time",
    isAsc: "descending",
  },
  rules: {
    ruleCode: [
      {required: true, validator: validateTrimmedRequired("规则编码不能为空", 64), trigger: "blur"},
      {
        validator: validateAlphaNumUnderscore("规则编码只允许英文字母、数字和下划线"),
        trigger: "blur",
        transform: trimText,
      },
    ],
    ruleName: [
      {required: true, validator: validateTrimmedRequired("规则名称不能为空", 64), trigger: "blur"},
      {
        validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, "规则名称只允许中文、英文字母、数字、下划线和短横线"),
        trigger: "blur",
        transform: trimText,
      },
    ],
    ruleType: [{required: true, message: "匹配类型不能为空", trigger: "change"}],
    ruleScore: [{required: true, message: "匹配分值不能为空", trigger: "change"}],
  }
});

const {queryParams, form, rules} = toRefs(data);

const tableStore = reactive({
  config: {
    // 内嵌列表：取消独立容器留白，隐藏搜索切换按钮。
    ...(props.embedded ? {fullContent: false, actions: {table: {search: false}}} : {}),
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      class: "rulematch-table",
      rowClassName: getRowClassName,
      defaultSort: {prop: "createTime", order: "descending"},
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    {type: "selection", width: 55},
    {label: "编号", prop: "id", width: 60, sortable: true},
    {
      label: "规则名称", prop: "ruleName", minWidth: 360,
      info: {
        title: row => `${row.ruleName || "-"}(${row.ruleCode || "-"})`,
        desc: "description",
        click: (row) => handleDetail(row),
        tags: [
          {
            options: rule_match_type,
            value: "ruleType",
          },
        ],
      }, align: "left",
      showOverflowTooltip: {effect: 'light'},
    },
    {label: "归属位置", prop: "projectName", slot: "location", minWidth: 180, align: "left"},
    {label: "匹配方式", prop: "ruleType", minWidth: 170, dict: "rule_match_type", align: "left"},
    {label: "参与字段 / 匹配逻辑", prop: "ruleConfig", slot: "ruleConfig", minWidth: 250, align: "left"},
    {label: "匹配分值", prop: "ruleScore", minWidth: 100, align: "left"},
    {label: "重复检测任务数", prop: "duplicateTaskCount", minWidth: 130, align: "left"},
    {label: "状态", prop: "validFlag", slot: "validFlag", minWidth: 135, align: "left"},
    { label: "负责人", prop: "responsibleUserId", slot: "responsibleUser", minWidth: 140, align: "left" },
    {label: "创建时间", prop: "createTime", minWidth: 150, date: true, sortable: true, sortableKey: "create_time"},
    {label: "操作", minWidth: 215, fixed: "right", slot: "action"},
  ],
  func: listRulematch,
  params: queryParams.value,
  events: {
    // 查询条件：详情内固定模型和版本，与规则管理使用相同筛选口径。
    formatParams: params => props.embedded
        ? {...params, modelId: props.modelId, modelVersion: modelOptions.value[0].version}
        : params,
  },
});

const searchStore = reactive({
  items: [
    {label: "规则名称", prop: "ruleName", component: {is: "input", placeholder: "请输入规则名称"}},
    {
      label: "匹配方式",
      prop: "ruleType",
      component: {is: "select", placeholder: "请选择匹配方式", options: rule_match_type},
    },
    {
      label: "状态",
      prop: "validFlag",
      component: {
        is: "select",
        placeholder: "请选择状态",
        options: [
          {label: "启用", value: true},
          {label: "禁用", value: false},
        ],
      },
    },
    {
      label: "配置情况",
      prop: "abnormalFlag",
      component: {
        is: "select",
        class: "config-search-input",
        placeholder: "全部",
        options: [
          {label: "全部", value: ""},
          {label: "配置正常", value: "0"},
          {label: "配置异常", value: "1"},
        ],
      },
    },
  ],
});

/** 查询匹配规则列表 */
function getList() {
  tableRef.value?.getList();
}

// 取消按钮
function cancel() {
  open.value = false;
  reset();
}

// 表单重置
function reset() {
  proxy.resetForm("rulematchRef");
  form.value = {
    id: null,
    ruleCode: null,
    ruleName: null,
    responsibleUserId: null,
    responsibleUserName: null,
    responsibleUserPhone: null,
    modelId: props.embedded ? props.modelId : null,
    modelVersion: props.embedded ? modelOptions.value[0]?.version || null : null,
    priority: null,
    validFlag: true,
    abnormalFlag: null,
    modelFieldId: null,
    ruleType: "EXACT",
    ruleConfig: "{}",
    ruleScore: null,
    description: null,
    delFlag: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null,
    remark: null,
  };
  syncModelLocation();
  resetFieldSelection();
}

function createFieldRow(id = "", ruleType = form.value.ruleType, hydrate = false) {
  const matchMethod = ruleType === "STD_EXACT"
      ? "STD_EXACT"
      : EXACT_RULE_TYPES.has(ruleType)
          ? "EXACT"
          : "FUZZY";
  return {
    modelFieldId: id,
    matchMethod: ruleType === "WEIGHTED" && !hydrate ? "" : matchMethod,
    weight: ruleType === "WEIGHTED" ? 0 : 100,
    fieldThreshold: !hydrate ? 0 : ruleType === "FUZZY" ? DEFAULT_FUZZY_FIELD_THRESHOLD : 100,
  };
}

function getModelOption(modelId) {
  return modelOptions.value.find((item) =>
      String(item.value) === String(modelId) || (item.legacyValue != null && String(item.legacyValue) === String(modelId))
  );
}

function handleModelTreeNodeClick(node) {
  if (node?.nodeType !== "model" || node.modelId == null) return;
  selectedModelId.value = node.modelId;
  modelTreeRef.value?.setCurrentKey?.(node.treeKey);
  queryParams.value.modelId = node.modelId;
  queryParams.value.modelVersion = getModelOption(node.modelId)?.version || null;
  tableRef.value?.resetQuery();
}

/** 树异步加载或刷新后，恢复当前模型的选中状态。 */
watch(modelTreeData, async () => {
  if (props.embedded || selectedModelId.value == null) return;
  await nextTick();
  modelTreeRef.value?.setCurrentKey?.(getModelOption(selectedModelId.value)?.treeKey ?? null);
});

watch(() => route.fullPath, async () => {
  if (route.path !== "/rule/match") return;
  await nextTick();
  reloadModelTree().catch(() => {
  });
  tableRef.value?.getList();
});

function normalizeModelId(modelId) {
  return getModelOption(modelId)?.value ?? modelId;
}

function normalizeFieldId(fieldId, modelId = form.value.modelId) {
  if (fieldId === null || fieldId === undefined || fieldId === "") return null;
  const fieldOption = getFieldOption(fieldId, modelId);
  if (fieldOption) return fieldOption.value;
  const numericValue = Number(fieldId);
  return Number.isNaN(numericValue) ? fieldId : numericValue;
}

function syncModelVersion() {
  const model = getModelOption(form.value.modelId);
  if (model) form.value.modelVersion = model.version;
}

function resetFieldSelection() {
  fieldRows.value = [createFieldRow()];
  configDraft.standardOptions = [];
  configDraft.similarityThreshold = 0;
  configDraft.finalThreshold = 0;
}

async function handleModelChange() {
  syncModelVersion();
  resetFieldSelection();
  const modelId = form.value.modelId;
  if (!modelId || props.embedded) return;
  try {
    const response = await getPublishedModelStructure(modelId);
    if (String(form.value.modelId) !== String(modelId)) return;
    publishedModelFields[modelId] = (response.data?.fields || []).map(field => ({
      value: field.id,
      label: field.name || field.code,
      name: field.name || field.code,
      code: field.code,
      type: field.dataType,
    }));
  } catch {
    proxy.$modal.msgError("主数据模型字段加载失败");
  }
}

function parseFieldIds(value) {
  if (Array.isArray(value)) return value.map(String);
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed.map(String);
  } catch {
    // 兼容历史的逗号分隔格式
  }
  return String(value).split(",").map((item) => item.trim()).filter(Boolean);
}

function parseRuleConfig(value) {
  if (value && typeof value === "object") return value;
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function isFieldOptionDisabled(fieldValue, rowIndex) {
  return fieldRows.value.some(
      (item, index) =>
          index !== rowIndex &&
          item.modelFieldId &&
          String(item.modelFieldId) === String(fieldValue)
  );
}

function addFieldRow() {
  fieldRows.value.push(createFieldRow());
}

function removeFieldRow(index) {
  fieldRows.value.splice(index, 1);
}

function handleRuleTypeChange() {
  resetFieldSelection();
}

function hydrateRuleConfig() {
  const ids = parseFieldIds(form.value.modelFieldId);
  const config = parseRuleConfig(form.value.ruleConfig);
  const ruleType = form.value.ruleType;
  const fieldList = Array.isArray(config.fieldList) && config.fieldList.length
      ? config.fieldList
      : ids.map((id) => ({modelFieldId: id}));

  fieldRows.value = fieldList.map((item) => {
    const modelFieldId = normalizeFieldId(item?.modelFieldId, form.value.modelId) ?? "";
    const defaultRow = createFieldRow(modelFieldId, ruleType, true);
    return {
      ...defaultRow,
      modelFieldId,
      matchMethod: item?.matchMethod || defaultRow.matchMethod,
      weight: item?.weight ?? defaultRow.weight,
      fieldThreshold: item?.fieldThreshold ?? defaultRow.fieldThreshold,
    };
  });
  if (!fieldRows.value.length) fieldRows.value = [createFieldRow("", ruleType, true)];
  configDraft.standardOptions = Array.isArray(config.standardOptions)
      ? [...config.standardOptions]
      : [...DEFAULT_STANDARD_OPTIONS];
  configDraft.similarityThreshold = config.similarityThreshold ?? DEFAULT_SIMILARITY_THRESHOLD;
  configDraft.finalThreshold = config.finalThreshold ?? 0;
}

function getRuleTypeLabel(type) {
  return rule_match_type.value.find((item) => item.value === type)?.label || type || "-";
}

function getFieldOption(fieldId, modelId = form.value.modelId) {
  const fieldOptions = getModelOption(modelId)?.fields || [];
  return fieldOptions.find((item) =>
      String(item.value) === String(fieldId)
      || item.label === fieldId
      || item.name === fieldId
      || item.code === fieldId
  );
}

function getFieldName(fieldId, modelId = form.value.modelId) {
  const fieldOption = getFieldOption(fieldId, modelId);
  return fieldOption?.name || fieldOption?.label || fieldId || "-";
}

function getRowRuleConfig(row) {
  return parseRuleConfig(row.ruleConfig);
}

function isAbnormalRow(row) {
  return row?.abnormalFlag === true || String(row?.abnormalFlag) === "1";
}

function getRowClassName({row}) {
  return isAbnormalRow(row) ? "abnormal-row" : "";
}

function formatRuleConfig(row) {
  const config = getRowRuleConfig(row);
  const fields = Array.isArray(config.fieldList) && config.fieldList.length
      ? config.fieldList
      : parseFieldIds(row.modelFieldId).map((modelFieldId) => ({modelFieldId}));
  const fieldText = fields
      .filter((item) => item?.modelFieldId)
      .map((item) => getFieldName(item.modelFieldId, row.modelId))
      .join("、");
  if (row.ruleType === "FUZZY") return `${fieldText || "参与字段"} 编辑距离相似度 ≥ ${config.similarityThreshold ?? "-"}`;
  if (row.ruleType === "WEIGHTED") {
    const weightedFieldText = fields
        .filter((item) => item?.modelFieldId)
        .map((item) => `${getFieldName(item.modelFieldId, row.modelId)} ${item.weight ?? "-"}%`)
        .join(" · ");
    return `${weightedFieldText || "参与字段"}；最终阈值 ${config.finalThreshold ?? "-"}`;
  }
  if (row.ruleType === "STD_EXACT") return `${fieldText || "参与字段"} 标准化后完全一致`;
  return `${fieldText || "参与字段"} 完全一致`;
}

function serializeRuleConfig() {
  const modelId = normalizeModelId(form.value.modelId);
  const fields = fieldRows.value
      .map((item) => ({
        modelFieldId: normalizeFieldId(item.modelFieldId, modelId),
        matchMethod: item.matchMethod,
        weight: item.weight,
        fieldThreshold: item.fieldThreshold,
      }))
      .filter((item) => item.modelFieldId !== null);
  const fieldIds = fields.map((item) => item.modelFieldId);
  form.value.modelFieldId = fieldIds.join(",");
  const config = {fieldList: fields};
  if (form.value.ruleType === "STD_EXACT") config.standardOptions = configDraft.standardOptions;
  if (form.value.ruleType === "FUZZY") config.similarityThreshold = configDraft.similarityThreshold;
  if (form.value.ruleType === "WEIGHTED") config.finalThreshold = configDraft.finalThreshold;
  form.value.ruleConfig = JSON.stringify(config);
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
}

/** 改变启用状态 */
function handleStatusChange(row) {
  const nextStatus = row.validFlag;
  const previousStatus = !nextStatus;
  const text = nextStatus ? "启用" : "禁用";

  proxy.$modal
      .confirm(`确认要${text} "${row.ruleName}" 匹配规则吗？`)
      .then(() => {
        statusLoading[row.id] = true;
        return updateRulematch({id: row.id, validFlag: nextStatus});
      })
      .then(() => {
        proxy.$modal.msgSuccess(`${text}成功`);
      })
      .catch(() => {
        row.validFlag = previousStatus;
      })
      .finally(() => {
        statusLoading[row.id] = false;
      });
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加匹配规则";
  loadResponsibleUsers();
}

function openExistingRecord(row, dialogRef, dialogTitle) {
  reset();
  const id = row?.id ?? ids.value[0];
  if (!id) return;

  getRulematch(id).then(async ({data: record}) => {
    if (!props.embedded && !modelTreeData.value.length) await reloadModelTree();
    form.value = {
      ...record,
      responsibleUserId: record.responsibleUserId == null ? null : String(record.responsibleUserId),
      modelId: normalizeModelId(record.modelId),
    };
    syncModelLocation();
    syncModelVersion();
    if (!props.embedded && form.value.modelId) await handleModelChange();
    hydrateRuleConfig();
    dialogRef.value = true;
    title.value = dialogTitle;
    loadResponsibleUsers();
  });
}

/** 修改按钮操作 */
function handleUpdate(row) {
  openExistingRecord(row, open, "修改匹配规则");
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push({path: "/rule/match/detail", query: {id: row.id}});
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["rulematchRef"].validate(valid => {
    if (valid) {
      if (!fieldRows.value.some((item) => item.modelFieldId)) {
        proxy.$modal.msgWarning("参与字段不能为空");
        return;
      }
      if (form.value.ruleType === "WEIGHTED" && totalWeight.value !== 100) {
        proxy.$modal.msgWarning("权重合计必须为 100%");
        return;
      }
      serializeRuleConfig();
      // 规则归属：详情内保存时使用当前模型ID。
      if (props.embedded) form.value.modelId = props.modelId;
      if (form.value.id != null) {
        updateRulematch(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        }).catch(() => {
        });
      } else {
        addRulematch(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        }).catch(() => {
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row?.id ?? ids.value;
  if (!_ids || (Array.isArray(_ids) && !_ids.length)) return;
  proxy.$modal.confirm('是否确认删除匹配规则编号为"' + _ids + '"的数据项？').then(function () {
    return delRulematch(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

const selectedProjectOption = computed(() => projectOptions.value.find(item => String(item.value) === String(modelProjectId.value)));
const selectedModelOption = computed(() => filteredModelOptions.value.find(item => String(item.value) === String(form.value.modelId)));
</script>

<style scoped lang="scss">
@use '@/styles/common/component-patterns.scss' as patterns;

:deep(.rulematch-table .info-desc) {
  -webkit-line-clamp: 1;
}

.rule-display-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.rule-display-cell small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rule-project-name {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rule-project-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

/* 详情内布局：去除重复留白，表格使用页签宽度。 */
.rule-page-embedded {
  margin: 0;
  padding: 0;
  min-width: 0;
  min-height: 0;
  background-color: transparent;

  > .el-container > .main-content {
    padding: 0;
    min-width: 0;
  }
}

.standard-tag-measure {
  position: absolute;
  visibility: hidden;
  pointer-events: none;
  display: flex;
  white-space: nowrap;
  height: 0;
  overflow: hidden;
}

.rule-config-fields {
  box-sizing: border-box;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
  min-width: 0;
}

.rule-config-fields > :deep(.el-form-item) {
  min-width: 0;
}

.weighted-table-wrap {
  width: 100%;
  min-width: 0;
}

.weighted-table-wrap :deep(.qt-table),
.weighted-table-wrap :deep(.qt-table--main),
.weighted-table-wrap :deep(.el-table) {
  width: 100%;
}

.weight-status {
  line-height: 1.4;
}

.rule-config-panel {
  margin: 0 0 18px 110px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  background: var(--el-fill-color-lighter);
}

.rule-config-title {
  margin-bottom: 12px;
  font-weight: 600;
}

.standardization-config-item {
  display: block;
  margin-bottom: 0;
}

.standardization-config-item :deep(.el-form-item__label) {
  width: auto !important;
  justify-content: flex-start;
  padding: 0;
  text-align: left;
}

.standardization-config-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
  text-align: left;
}

.standardization-config-item :deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: nowrap;
  gap: 20px;
}

.standardization-config-item :deep(.el-checkbox) {
  margin-right: 0;
  white-space: nowrap;
}

.fuzzy-field-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fuzzy-field-item :deep(.el-form-item__label) {
  width: auto !important;
  justify-content: flex-start;
  padding: 0;
  text-align: left;
}

.fuzzy-field-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
  text-align: left;
}

.fuzzy-config-row {
  margin-bottom: 0;
}

.weighted-summary {
  margin-top: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.weighted-summary-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.weighted-threshold-label {
  margin-left: 8px;
}

.weighted-threshold-input {
  width: 90px;
}

.weight-status-column {
  display: flex;
  align-items: center;
}

.weight-status {
  font-size: 13px;
}

.weighted-summary-tip {
  margin-top: 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.is-valid {
  color: var(--el-color-success);
}

.is-invalid {
  color: var(--el-color-danger);
}

.field-select {
  width: 100%;
}

.field-config-row {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.field-config-row .el-input,
.field-config-row .el-select {
  flex: 1;
}

.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 7px;
  font-size: 12px;
  line-height: 16px;
  border: 1px solid transparent;
  border-radius: 12px;
  white-space: nowrap;
}

.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge.is-abnormal {
  color: var(--el-color-danger);
  border-color: rgba(245, 108, 108, 0.22);
  background: rgba(245, 108, 108, 0.06);
}

:deep(.rulematch-table .el-table__body tr.abnormal-row > td) {
  background-color: rgba(245, 108, 108, 0.04) !important;
}

:deep(.rulematch-table .el-table__body tr.abnormal-row:hover > td) {
  background-color: rgba(245, 108, 108, 0.04) !important;
}

:deep(.rulematch-table .el-table__body tr.abnormal-row > td.el-table-fixed-column--right),
:deep(.rulematch-table .el-table__body tr.abnormal-row:hover > td.el-table-fixed-column--right) {
  background-color: var(--el-table-tr-bg-color) !important;
  background-image: linear-gradient(
      rgba(245, 108, 108, 0.04),
      rgba(245, 108, 108, 0.04)
  ) !important;
}

.source-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.source-option__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  object-fit: contain;
}
</style>

