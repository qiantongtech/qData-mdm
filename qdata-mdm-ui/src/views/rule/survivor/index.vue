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
    <GuideTip v-if="!props.embedded" tip-id="rule/survivor.list"/>
    <el-container >
      <!-- 模型树：规则管理页面展示，模型详情内隐藏。 -->
      <qt-tree
        v-if="!props.embedded"
        ref="modelTreeRef"
        v-model:left-width="leftWidth"
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
              v-hasPermi="['rule:rulesurvivor:add']"
            >
              新增
            </el-button>
            <el-button
              type="danger"
              plain
              icon="Delete"
              :disabled="multiple"
              @click="handleDelete"
              v-hasPermi="['rule:rulesurvivor:remove']"
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
            <template #modelInfo="{ row }">
              <div v-if="row.modelName || row.modelCode" class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({ type: row.modelType, icon: row.modelIcon })" alt="" class="info-icon" />
                  <span
                    class="info-title model-info-title text-ellipsis"
                    role="link"
                    tabindex="0"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ getModelTitle(row) }}</span>
                  <dict-tag v-if="row.modelType" :options="model_type" :value="row.modelType" size="small" type="primary" />
                  <dict-tag
                    v-if="row.modelVersion"
                    class="ml4 pointer"
                    :value="row.modelVersion"
                    size="small"
                    type="info"
                    role="link"
                    tabindex="0"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || "-" }}</div>
              </div>
              <span v-else>-</span>
            </template>
            <template #mergeRecordCount="{ row }">{{ row.mergeRecordCount ?? "-" }}</template>
            <template #responsibleUser="{ row }">
              <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)"
                >{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text"
                >{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #modelFieldId="{ row }">
              <div class="field-cell">
<!--                <span :class="['field-type-icon', row.fieldTypeClass]">
                  {{ row.fieldTypeShort }}
                </span>-->
                <span class="field-cell-content">
                  <span class="field-cell-name">{{ row.fieldName }}</span>
                  <small>{{ row.fieldCode }}</small>
                </span>
              </div>
            </template>
            <template #fieldType="{ row }">
              <span class="type-label">{{ row.fieldType }}</span>
            </template>
            <template #ruleType="{ row }">
              <el-tag effect="plain">{{ getRuleTypeLabel(row.ruleType) }}</el-tag>
            </template>
            <template #ruleConfig="{ row }">
              <span :class="{ 'config-error-text': isAbnormalRow(row) }">
                {{ formatRuleLogic(row) }}
              </span>
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
              <div class="action-cell">

                <el-button
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(row)"
                  v-hasPermi="['rule:rulesurvivor:edit']"
                >详情</el-button>
                <el-button
                    link
                    type="primary"
                    icon="Edit"
                    @click="handleUpdate(row)"
                    v-hasPermi="['rule:rulesurvivor:edit']"
                >修改</el-button>
                <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(row)"
                  v-hasPermi="['rule:rulesurvivor:remove']"
                >删除</el-button>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 添加或修改生存规则对话框 -->
    <el-dialog :title="title" v-model="open" width="1000px" draggable>
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form
        ref="rulesurvivorRef"
        :model="form"
        :rules="rules"
        label-width="140px"
        @submit.prevent
        class="column-form"
      >
        <div class="h2-titles row-full">基础信息</div>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入名称，支持中英文、数字、下划线和短横线。" />
        </el-form-item>
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则编码，仅支持英文字母、数字和下划线。" />
        </el-form-item>
        <el-form-item label="主数据域" required>
          <el-select v-model="modelProjectId" class="full-width-input" placeholder="请选择主数据域"
            :loading="modelTreeLoading" :disabled="props.embedded" clearable filterable @change="handleProjectChange"
          >
            <template #label="{ label }">
              <div v-if="selectedProjectOption" class="task-title-row">
                <img :src="getProjectIcon(selectedProjectOption.icon)" alt="" class="info-icon" />
                <span class="text-ellipsis">{{ selectedProjectOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option v-for="item in projectOptions" :key="item.value" :label="item.label" :value="item.value">
              <div class="task-title-row">
                <img :src="getProjectIcon(item.icon)" alt="" class="info-icon" />
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="模型目录" required>
          <el-tree-select v-model="modelLayerId" class="full-width-input" :data="layerOptions"
            :placeholder="modelProjectId != null ? '请选择模型目录' : '请先选择主数据域'"
            :disabled="props.embedded || modelProjectId == null" check-strictly clearable filterable @change="handleLayerChange"
          />
        </el-form-item>
        <el-form-item label="主数据模型" prop="modelId" required>
          <el-select
            v-model="form.modelId"
            :placeholder="modelLayerId != null ? '请选择主数据模型' : '请先选择模型目录'"
            :disabled="props.embedded || modelLayerId == null"
            @change="handleModelChange"
            class="full-width-input"
          >
            <template #label="{ label }">
              <div v-if="selectedModelOption" class="task-title-row">
                <img :src="getModelIcon(selectedModelOption)" alt="" class="info-icon" />
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
              <div class="task-title-row">
                <img :src="getModelIcon(item)" alt="" class="info-icon" />
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="responsibleUserId">
          <el-select v-model="form.responsibleUserId" class="full-width-input" placeholder="请选择负责人" clearable filterable :loading="usersLoading" @clear="form.responsibleUserId = null">
            <el-option v-for="user in responsibleUserOptions" :key="user.value" :label="user.label" :value="user.value" />
          </el-select>
          <el-button v-if="usersError" link type="primary" @click="loadResponsibleUsers">重新加载负责人</el-button>
        </el-form-item>
        <el-form-item label="联系电话" >
          <el-input :model-value="responsibleUserPhone" disabled placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="规则状态" prop="validFlag">
          <el-radio-group v-model="form.validFlag">
            <el-radio :label="true">启用</el-radio>
            <el-radio :label="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="规则说明" prop="description" class="row-full">
          <el-input
            v-model="form.description"
            type="textarea"
            resize="vertical"
            placeholder="请输入规则说明"
            :min-height="192"
            show-word-limit
            maxlength="256个字符"
          />
        </el-form-item>

        <div class="h2-title row-full">字段生存配置</div>
            <el-form-item label="模型字段" prop="modelFieldId">
              <el-select
                v-model="form.modelFieldId"
                class="full-width-input"
                :placeholder="form.modelId ? '请选择模型字段' : '请先选择主数据模型'"
                :disabled="!form.modelId"
              >
                <el-option
                  v-for="item in currentFieldOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <qt-form-item label="主要自动策略" prop="ruleType" :tip="strategyTips[form.ruleType]">
              <el-select
                v-model="form.ruleType"
                class="full-width-input"
                placeholder="请选择生存策略"
                @change="handleRuleTypeChange"
              >
                <el-option
                  v-for="item in rule_rulesurvivor_type"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </qt-form-item>
            <qt-form-item v-if="form.ruleType === 'SOURCE_PRIORITY'" label="来源优先级配置" tip="按选择顺序从左到右确定优先级，先选的优先级更高。"required>
              <el-select v-model="sourcePrioritySelection" class="full-width-input" multiple
                placeholder="请选择来源系统"
              >
                <el-option v-for="item in sourcePriorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Rulesurvivor">
import { getModelIcon, defaultProjectIcon, getProjectIcon } from "@/utils/imageUrl";
import { useDict } from "@/utils/dict";
import { listRulesurvivor, getRulesurvivor, delRulesurvivor, addRulesurvivor, updateRulesurvivor } from "@/api/rule/survivor/survivor";
import QtTree from "@/components/QtTree/index.vue";
import { getPublishedModelStructure } from "@/api/model/base/base";
import { usePublishedModelTree, withModelTreeKeys } from "@/composables/usePublishedModelTree";
import { useUserDirectory } from '@/store/system/userDirectory';
import { useRoute, useRouter } from "vue-router";
import { trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired } from "@/utils/validate.js";

const {model_type, rule_match_type} = useDict('model_type', 'rule_match_type');

const { responsibleUserList, getResponsibleName, getResponsiblePhone, loadResponsibleUserOptions } = useUserDirectory();

  // 页面参数：支持模型详情内嵌，并接收当前模型信息。
  const props = defineProps({
    embedded: { type: Boolean, default: false },
    modelId: { type: [String, Number], default: null },
    modelDetail: { type: Object, default: () => ({}) },
  });

  const { proxy } = getCurrentInstance();
  const { rule_rulesurvivor_type } = useDict(
      "rule_rulesurvivor_type"
  );
  const open = ref(false);
  const tableRef = ref(null);
  const statusLoading = reactive({});
  const ids = ref([]);
  const multiple = ref(true);
  const title = ref("");
  const usersLoading = ref(false);
  const usersError = ref(false);

  const route = useRoute();
  const router = useRouter();
  const { data: modelTreeData, loading: modelTreeLoading, reload: reloadModelTree } = usePublishedModelTree(withModelTreeKeys, !props.embedded);
  const modelProjectId = ref(null);
  const modelLayerId = ref(null);
  const projectOptions = computed(() => props.embedded ? [{
    value: props.modelDetail.projectId, label: props.modelDetail.projectName || String(props.modelDetail.projectId || ''), icon: props.modelDetail.projectIcon,
  }] : modelTreeData.value.map(node => ({ value: node.projectId, label: node.name || node.code, icon: node.icon })));
  const layerOptions = computed(() => {
    if (props.embedded) return [{ value: props.modelDetail.layerId, label: props.modelDetail.layers?.map(item => item.name).join(' / ') || String(props.modelDetail.layerId || '') }];
    const project = modelTreeData.value.find(node => String(node.projectId) === String(modelProjectId.value));
    const toLayers = nodes => (nodes || []).filter(node => node.nodeType === 'layer')
      .map(node => ({ value: node.layerId, label: node.name || node.code, children: toLayers(node.children) }));
    return toLayers(project?.children);
  });
  const selectedProjectOption = computed(() => projectOptions.value.find(item => String(item.value) === String(modelProjectId.value)));
  const publishedModelFields = reactive({});
  const loadedModelFields = new Set();
  const loadingModelFields = new Map();

  const publishedModelOptions = computed(() => {
    const models = [];
    const visit = (nodes, projectName = "", layerName = "") => {
      for (const node of nodes || []) {
        const project = node.nodeType === "project" ? (node.name || node.code) : projectName;
        const layer = node.nodeType === "layer" ? (node.name || node.code) : layerName;
        if (node.modelId != null) {
          models.push({
            value: node.modelId,
            projectId: node.projectId,
            layerId: node.layerId,
            icon: node.icon,
            type: node.type,
            label: node.name || node.code || String(node.modelId),
            version: node.versionNo || "",
            projectName: project,
            layerName: layer,
            fields: publishedModelFields[node.modelId] || [],
          });
        }
        visit(node.children, project, layer);
      }
    };
    visit(modelTreeData.value);
    return models;
  });

  // 模型详情内使用传入数据；独立页面使用已发布模型树。
  const modelOptions = computed(() => props.embedded ? [{
    value: props.modelId,
    projectId: props.modelDetail.projectId,
    layerId: props.modelDetail.layerId,
    icon: props.modelDetail.icon,
    type: props.modelDetail.type,
    parentId: props.modelDetail.parentId,
    label: props.modelDetail.name || props.modelDetail.code || String(props.modelId || ""),
    projectName: props.modelDetail.projectName || "",
    layerName: props.modelDetail.layers?.map(item => item.name).join(" / ") || "",
    version: props.modelDetail.version?.versionNo || "",
    fields: (props.modelDetail.fields || []).map(field => ({
      value: field.id,
      label: field.name || field.code,
      name: field.name || field.code,
      code: field.code,
      type: field.dictTypeId != null ? "reference" : String(field.dataType || "").toLowerCase().replace(/^datetime$/, "date"),
    })),
  }] : publishedModelOptions.value);
  const filteredModelOptions = computed(() => props.embedded ? modelOptions.value : modelOptions.value.filter(item =>
    modelProjectId.value != null && modelLayerId.value != null &&
    String(item.projectId) === String(modelProjectId.value) && String(item.layerId) === String(modelLayerId.value)));

  function handleProjectChange() {
    modelLayerId.value = null;
    handleLayerChange();
  }

  function handleLayerChange() {
    form.value.modelId = null;
    form.value.modelVersion = null;
    form.value.modelFieldId = null;
  }

  function syncModelLocation() {
    const model = props.embedded ? props.modelDetail : getModelOption(form.value.modelId);
    modelProjectId.value = model?.projectId ?? null;
    modelLayerId.value = model?.layerId ?? null;
  }

  const sourceOptions = [
    { label: "ERP", value: "ERP" },
    { label: "SRM", value: "SRM" },
    { label: "CRM", value: "CRM" },
    { label: "WMS", value: "WMS" },
  ];
  const strategyTips = Object.freeze({
    NON_EMPTY_PRIORITY: "优先选择非空有效值；存在多个不同非空值时无法唯一确定值，形成字段冲突。",
    LATEST_VALUE: "优先选择有效更新时间最新的候选值；无法唯一确定值时形成字段冲突。",
    CREDIBILITY_PRIORITY: "优先选择来源系统可信度最高的候选值；无法唯一确定值时形成字段冲突。",
  });
  const DEFAULT_SOURCE_PRIORITY_LIST = Object.freeze(["ERP", "SRM", "CRM", "WMS"]);
  const EMPTY_SOURCE_PRIORITY_LIST = Object.freeze([null, null, null, null]);
  const modelTreeRef = ref(null);
  const leftWidth = ref(280);
  const selectedModelId = ref(props.embedded ? props.modelId : null);

  const data = reactive({
    form: {},
    queryParams: {
      fieldName: null,
      ruleCode: null,
      ruleName: null,
      modelId: props.embedded ? props.modelId : null,
      modelVersion: props.embedded ? modelOptions.value[0]?.version || null : null,
      modelFieldId: null,
      abnormalFlag: null,
      ruleType: null,
      ruleConfig: null,
      validFlag: null,
      description: null,
      orderByColumn: "create_time",
      isAsc: "descending",
    },
    rules: {
      ruleCode: [
        { required: true, validator: validateTrimmedRequired("规则编码不能为空", 64), trigger: "blur" },
        {
          validator: validateAlphaNumUnderscore("规则编码只允许英文字母、数字和下划线"),
          trigger: "blur",
          transform: trimText,
        },
      ],
      ruleName: [
        { required: true, validator: validateTrimmedRequired("规则名称不能为空", 64), trigger: "blur" },
        {
          validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, "规则名称只允许中文、英文字母、数字、下划线和短横线"),
          trigger: "blur",
          transform: trimText,
        },
      ],
      modelId: [{ required: true, message: "主数据模型不能为空", trigger: "change" }],
      modelFieldId: [{ required: true, message: "模型字段不能为空", trigger: "change" }],
      ruleType: [{ required: true, message: "主要自动策略不能为空", trigger: "change" }],
      validFlag: [{
        validator: (_rule, value, callback) => {
          if (typeof value !== "boolean") {
            callback(new Error("规则状态不能为空"));
            return;
          }
          callback();
        },
        trigger: "change",
      }],
    }
  });

  const { queryParams, form, rules } = toRefs(data);
  const responsibleUserOptions = computed(() => {
  const id = form.value.responsibleUserId;
  const users = responsibleUserList.value;
  if (id != null && id !== '' && !users.some(user => user.value === String(id))) {
    return [{ value: String(id), label: `用户 ${id}（已不可用）`, phone: '' }, ...users];
  }
  return users;
});
  const responsibleUserPhone = computed(() => getResponsiblePhone(form.value.responsibleUserId));

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
  const configDraft = reactive({
    sourcePriorityList: [...EMPTY_SOURCE_PRIORITY_LIST],
  });
  const sourcePrioritySelection = computed({
    get: () => configDraft.sourcePriorityList.filter(Boolean),
    set: values => { configDraft.sourcePriorityList = values; },
  });
  const sourcePriorityOptions = computed(() => [
    ...sourceOptions,
    ...sourcePrioritySelection.value
      .filter(value => !sourceOptions.some(option => String(option.value) === String(value)))
      .map(value => ({ value, label: String(value) })),
  ]);
  const currentModelOption = computed(() => getModelOption(form.value.modelId));
  const currentFieldOptions = computed(() => props.embedded
    ? currentModelOption.value?.fields || []
    : publishedModelFields[form.value.modelId] || []);

  const tableStore = reactive({
    config: {
      // 内嵌列表：取消独立容器留白，隐藏搜索切换按钮。
      ...(props.embedded ? { fullContent: false, actions: { table: { search: false } } } : {}),
      sort: true,
      table: {
        stripe: true,
        rowKey: "id",
        class: "survivor-table",
        rowClassName: getRowClassName,
        defaultSort: { prop: "createTime", order: "descending" },
        onSelectionChange: handleSelectionChange,
      },
    },
    columns: [
      { type: "selection", width: 55 },
      { label: "编号", prop: "id", width: 60 ,sortable: true},
      {
        label: "规则名称", prop: "ruleName", minWidth: 360,
        info: {
          title: row => `${row.ruleName || "-"}(${row.ruleCode || "-"})`,
          desc: "description",
          click: (row) => handleDetail(row),
          tags: [
            {
              options: rule_rulesurvivor_type,
              value: "ruleType",
            },
          ],
        }, align: "left"
      },
      { label: "主数据模型", prop: "modelName", slot: "modelInfo", minWidth: 420, align: "left", showOverflowTooltip: { effect: "light" } },
      { label: "归属位置", prop: "projectName", slot: "location", minWidth: 210, align: "left" },
      {
        label: "模型字段",
        prop: "modelFieldId",
        slot: "modelFieldId",
        minWidth: 160,
        align: "left",
      },
      /*{ label: "类型", prop: "fieldType", tag: true, width: 90 },*/
      { label: "生存策略", prop: "ruleType",dict: "rule_rulesurvivor_type",minWidth: 150 },
      {
        label: "取值逻辑",
        prop: "ruleConfig",
        slot: "ruleConfig",
        minWidth: 260,
        align: "left",
        showOverflowTooltip: { effect: "light" },
      },
      { label: "合并管理记录数", prop: "mergeRecordCount", slot: "mergeRecordCount", minWidth: 150 },
      { label: "状态", prop: "validFlag", slot: "validFlag", minWidth: 135, align: "left" },
      { label: "负责人", prop: "responsibleUserId", slot: "responsibleUser", minWidth: 140, align: "left" },
      { label: "创建时间", prop: "createTime", minWidth: 150, date: true, sortable: true, sortableKey: "create_time" },
      { label: "操作", minWidth: 220, fixed: "right", slot: "action" },
    ],
    func: listRulesurvivor,
    params: queryParams.value,
    events: {
      formatParams: (params) => {
        // 查询条件：详情内固定模型和版本，与规则管理使用相同筛选口径。
        const requestParams = props.embedded
          ? { ...params, modelId: props.modelId, modelVersion: modelOptions.value[0].version }
          : { ...params };
        delete requestParams.fieldName;
        return requestParams;
      },
      formatData: (rows) => {
        const decoratedRows = rows.map(decorateRuleRow);
        hydrateVisibleModelFields(rows);
        const keyword = String(queryParams.value.fieldName || "").trim().toLowerCase();
        if (!keyword) return decoratedRows;
        return decoratedRows.filter((row) =>
          [row.fieldName, row.fieldCode, row.modelFieldId]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(keyword))
        );
      },
    },
  });

  const searchStore = reactive({
    items: [
      {
        label: "字段名称",
        prop: "fieldName",
        component: {
          is: "input",
          placeholder: "请输入字段名称",
        },
      },
      {
        label: "生存策略",
        prop: "ruleType",
        component: {
          is: "select",
          placeholder: "请选择生存策略",
          options: rule_rulesurvivor_type,
        },
      },
      {
        label: "状态",
        prop: "validFlag",
        component: {
          is: "select",
          placeholder: "请选择状态",
          options: [
            { label: "启用", value: true },
            { label: "禁用", value: false },
          ],
        },
      },
      {
        label: "配置情况",
        prop: "abnormalFlag",
        component: {
          is: "select",
          placeholder: "全部",
          options: [
            { label: "全部", value: "" },
            { label: "配置正常", value: "0" },
            { label: "配置异常", value: "1" },
          ],
        },
      },
    ],
    config: { visibleItemCount: 4 },
  });

  function getList() {
    tableRef.value?.getList();
  }

  function loadModelFields(modelId) {
    if (props.embedded || modelId == null) return Promise.resolve(false);
    const key = String(modelId);
    if (loadedModelFields.has(key)) return Promise.resolve(false);
    if (loadingModelFields.has(key)) return loadingModelFields.get(key);
    const request = getPublishedModelStructure(modelId)
      .then(({ data }) => {
        publishedModelFields[modelId] = (data?.fields || []).map(field => ({
          value: field.id,
          label: field.name || field.code,
          name: field.name || field.code,
          code: field.code,
          type: field.dictTypeId != null ? "reference" : String(field.dataType || "").toLowerCase().replace(/^datetime$/, "date"),
        }));
        loadedModelFields.add(key);
        return true;
      })
      .finally(() => loadingModelFields.delete(key));
    loadingModelFields.set(key, request);
    return request;
  }

  function hydrateVisibleModelFields(rows) {
    if (props.embedded) return;
    const ids = [...new Set(rows.map(row => row.modelId).filter(id => id != null))];
    const missing = ids.filter(id => !loadedModelFields.has(String(id)));
    if (!missing.length) return;
    Promise.allSettled(missing.map(loadModelFields)).then(results => {
      if (results.some(result => result.status === "fulfilled" && result.value)) getList();
    });
  }

  function displayValue(value) {
    return value === null || value === undefined || value === "" ? "-" : value;
  }

  function getModelOption(modelId) {
    return modelOptions.value.find((item) =>
      String(item.value) === String(modelId) || (item.legacyValue != null && String(item.legacyValue) === String(modelId))
    );
  }

  function getModelTitle(row) {
    const { modelName, modelCode } = row;
    return modelName ? (modelCode ? `${modelName} (${modelCode})` : modelName) : modelCode || "-";
  }

  function openModelDetail(row, tab) {
    if (row.modelId == null) return;
    router.push({
      path: "/model/base/detail",
      query: { id: row.modelId, ...(tab ? { tab } : {}) },
    });
  }

  function getFieldOption(fieldId, modelId = form.value.modelId) {
    const fieldOptions = props.embedded
      ? getModelOption(modelId)?.fields || []
      : publishedModelFields[modelId] || [];
    return fieldOptions.find((item) =>
      String(item.value) === String(fieldId)
      || item.label === fieldId
      || item.name === fieldId
      || item.code === fieldId
    );
  }

  function getFieldTypeMeta(fieldOption, fieldId) {
    const typeMap = {
      string: { label: "字符串", short: "字", className: "is-string" },
      reference: { label: "参考数据", short: "参", className: "is-reference" },
      date: { label: "日期", short: "日", className: "is-date" },
      decimal: { label: "数值", short: "数", className: "is-number" },
      number: { label: "数值", short: "数", className: "is-number" },
    };
    const type = fieldOption?.type || "history";
    return typeMap[type] || {
      label: fieldOption ? "其他" : "历史字段",
      short: fieldOption ? "其" : "!",
      className: fieldOption ? "is-other" : "is-invalid",
      fieldId,
    };
  }

  function getFieldDisplayMeta(fieldId, modelId) {
    const fieldOption = getFieldOption(fieldId, modelId);
    if (!props.embedded && !loadedModelFields.has(String(modelId))) {
      return {
        fieldName: fieldOption?.name || displayValue(fieldId),
        fieldCode: fieldOption?.code || displayValue(fieldId),
        fieldType: "-",
        fieldTypeShort: "",
        fieldTypeClass: "is-pending",
      };
    }
    const typeMeta = getFieldTypeMeta(fieldOption, fieldId);
    const fallbackLabel = displayValue(fieldId);
    const rawLabel = fieldOption?.name || fieldOption?.label || fallbackLabel;
    const fieldName = fieldOption?.name
      || (fieldOption ? rawLabel.replace(/（[^）]*）$/, "") : "已删除字段");
    const fieldCode = fieldOption?.code || fallbackLabel;
    return {
      fieldName,
      fieldCode,
      fieldType: typeMeta.label,
      fieldTypeShort: typeMeta.short,
      fieldTypeClass: typeMeta.className,
    };
  }

  function decorateRuleRow(row) {
    const fieldMeta = getFieldDisplayMeta(row.modelFieldId, row.modelId);
    return {
      ...row,
      ...fieldMeta,
    };
  }

  function isAbnormalRow(row) {
    return (
      row.fieldTypeClass === "is-invalid" ||
      row.abnormalFlag === true ||
      String(row.abnormalFlag) === "1"
    );
  }

  function getRuleTypeLabel(ruleType) {
    return rule_rulesurvivor_type.value.find((item) => item.value === ruleType)?.label || displayValue(ruleType);
  }

  function getSourceLabel(source) {
    return sourceOptions.find((item) => item.value === source)?.label || displayValue(source);
  }

  function parseRuleConfig(value) {
    if (value && typeof value === "object" && !Array.isArray(value)) return value;
    if (!value) return {};
    try {
      const parsed = JSON.parse(value);
      return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
    } catch {
      return {};
    }
  }

  function formatRuleLogic(row) {
    if (row.fieldTypeClass === "is-invalid") {
      return `引用字段 [${row.fieldCode}] 在模型最新版本中已不存在，当前规则已自动隔离。`;
    }
    const config = parseRuleConfig(row.ruleConfig);
    if (row.ruleType === "SOURCE_PRIORITY") {
      const priorityList = Array.isArray(config.sourcePriorityList)
        ? config.sourcePriorityList.map(getSourceLabel).join(" > ")
        : "";
      return `${priorityList || "未配置来源优先级"}；无法唯一确定则冲突`;
    }
    if (row.ruleType === "CREDIBILITY_PRIORITY") {
      return "取来源系统可信度最高候选；无法唯一确定则冲突";
    }
    if (row.ruleType === "LATEST_VALUE") {
      return "取有效更新时间最新候选；无法唯一确定则冲突";
    }
    if (row.ruleType === "NON_EMPTY_PRIORITY") {
      return "优先采用非空有效值；多个不同非空值无法唯一确定则冲突";
    }
    return displayValue(row.description);
  }

  function handleModelTreeNodeClick(node) {
    if (!node?.modelId) return;
    selectedModelId.value = node.modelId;
    queryParams.value.modelId = node.modelId;
    queryParams.value.modelVersion = getModelOption(node.modelId)?.version || null;
    tableRef.value?.resetQuery();
  }

  onMounted(() => {
    nextTick(() => {
      if (selectedModelId.value) modelTreeRef.value?.setCurrentKey?.(`model:${selectedModelId.value}`);
    });
  });

  watch(() => route.fullPath, async () => {
    if (route.path !== "/rule/survivor") return;
    await nextTick();
    reloadModelTree().catch(() => {});
    getList();
  });

  function getRowClassName({ row }) {
    return isAbnormalRow(row) ? "abnormal-row" : "";
  }

  function normalizeModelId(modelId) {
    return getModelOption(modelId)?.value ?? modelId;
  }

  function normalizeFieldId(fieldId, modelId) {
    return getFieldOption(fieldId, modelId)?.value ?? fieldId;
  }

  function normalizeValidFlag(value) {
    return value === true || value === 1 || value === "1" || value === "true";
  }

  /** 改变启用状态 */
  function handleStatusChange(row) {
    const nextStatus = row.validFlag;
    const previousStatus = !nextStatus;
    const text = nextStatus ? "启用" : "禁用";

    proxy.$modal
      .confirm(`确认要${text} "${row.ruleName}" 生存规则吗？`)
      .then(() => {
        statusLoading[row.id] = true;
        return updateRulesurvivor({ id: row.id, validFlag: nextStatus });
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

  function syncModelVersion() {
    form.value.modelVersion = currentModelOption.value?.version || "";
  }

  function resetSourcePriorityList() {
    configDraft.sourcePriorityList = [...EMPTY_SOURCE_PRIORITY_LIST];
  }

  async function handleModelChange() {
    syncModelVersion();
    form.value.modelFieldId = null;
    const modelId = form.value.modelId;
    if (!modelId || props.embedded) return;
    try {
      await loadModelFields(modelId);
    } catch {
      proxy.$modal.msgError("主数据模型字段加载失败");
    }
  }

  function handleRuleTypeChange(ruleType) {
    if (ruleType === "SOURCE_PRIORITY") {
      resetSourcePriorityList();
    } else {
      configDraft.sourcePriorityList = [];
    }
  }

  function hydrateRuleConfig() {
    const config = parseRuleConfig(form.value.ruleConfig);
    const configuredFieldId = normalizeFieldId(
      config.modelFieldId ?? form.value.modelFieldId,
      form.value.modelId
    );
    if (configuredFieldId !== null && configuredFieldId !== undefined && configuredFieldId !== "") {
      form.value.modelFieldId = configuredFieldId;
    }
    const hasConfiguredPriorityList = Array.isArray(config.sourcePriorityList);
    const selectedSources = new Set();
    const priorityList = hasConfiguredPriorityList
      ? config.sourcePriorityList
        .filter((item) => item !== null && item !== undefined && item !== "")
        .filter((item) => {
          const key = String(item);
          if (selectedSources.has(key)) return false;
          selectedSources.add(key);
          return true;
        })
      : [];
    configDraft.sourcePriorityList = hasConfiguredPriorityList
      ? priorityList
      : [...DEFAULT_SOURCE_PRIORITY_LIST];
    if (form.value.ruleType !== "SOURCE_PRIORITY") {
      configDraft.sourcePriorityList = [];
    }
  }

  // 取消按钮
  function cancel() {
    open.value = false;
    reset();
  }

  // 表单重置
  function reset() {
    const defaultModel = props.embedded ? modelOptions.value[0] : null;
    form.value = {
      id: null,
      ruleCode: null,
      ruleName: null,
      responsibleUserId: null,
      modelId: defaultModel?.value ?? null,
      modelVersion: defaultModel?.version ?? null,
      modelFieldId: null,
      abnormalFlag: "0",
      ruleType: "SOURCE_PRIORITY",
      ruleConfig: "{}",
      validFlag: true,
      description: null,
    };
    proxy.resetForm("rulesurvivorRef");
    syncModelLocation();
    resetSourcePriorityList();
  }

  // 多选框选中数据
  function handleSelectionChange(selection) {
    ids.value = selection.map((item) => item.id);
    multiple.value = !selection.length;
  }

  /** 新增按钮操作 */
  function handleAdd() {
    reset();
    open.value = true;
    title.value = "添加生存规则";
    loadResponsibleUsers();
  }

  function openExistingRecord(row, dialogRef, dialogTitle) {
    reset();
    const id = row?.id ?? ids.value[0];
    if (!id) return;
    getRulesurvivor(id).then(async ({ data: record }) => {
      if (!props.embedded) {
        if (!modelTreeData.value.length) await reloadModelTree().catch(() => {});
        await loadModelFields(record.modelId).catch(() => {});
      }
      form.value = {
        ...record,
        responsibleUserId: record.responsibleUserId == null ? null : String(record.responsibleUserId),
        modelId: normalizeModelId(record.modelId),
        modelFieldId: normalizeFieldId(record.modelFieldId, record.modelId),
        validFlag: normalizeValidFlag(record.validFlag),
      };
      syncModelLocation();
      syncModelVersion();
      hydrateRuleConfig();
      dialogRef.value = true;
      title.value = dialogTitle;
      if (dialogRef === open) loadResponsibleUsers();
    });
  }

  /** 修改按钮操作 */
  function handleUpdate(row) {
    openExistingRecord(row, open, "修改生存规则");
  }

  /** 详情按钮操作 */
  function handleDetail(row) {
    router.push({ path: "/rule/survivor/detail", query: { id: row.id } });
  }

  function buildSubmitPayload() {
    const modelFieldId = normalizeFieldId(form.value.modelFieldId, form.value.modelId);
    const ruleConfig = {
      modelFieldId,
      matchMethod: form.value.ruleType,
    };
    if (form.value.ruleType === "SOURCE_PRIORITY") {
      ruleConfig.sourcePriorityList = configDraft.sourcePriorityList.filter(Boolean);
    }

    return {
      ...(form.value.id != null ? { id: form.value.id } : {}),
      ruleCode: form.value.ruleCode,
      ruleName: form.value.ruleName,
      responsibleUserId: form.value.responsibleUserId,
      // 规则归属：详情内保存时使用当前模型ID。
      modelId: props.embedded ? props.modelId : normalizeModelId(form.value.modelId),
      modelVersion: currentModelOption.value?.version || form.value.modelVersion,
      modelFieldId,
      validFlag: form.value.validFlag === true,
      abnormalFlag: form.value.abnormalFlag || "0",
      description: form.value.description || "",
      ruleType: form.value.ruleType,
      ruleConfig: JSON.stringify(ruleConfig),
    };
  }

  function validateSourcePriority() {
    const priorityList = configDraft.sourcePriorityList.filter(Boolean);
    const isUnique = new Set(priorityList).size === priorityList.length;
    if (!priorityList.length) {
      proxy.$modal.msgWarning("请至少选择一个来源系统");
      return false;
    }
    if (!isUnique) {
      proxy.$modal.msgWarning("来源优先级不能重复");
      return false;
    }
    return true;
  }

  /** 提交按钮 */
  function submitForm() {
    proxy.$refs["rulesurvivorRef"].validate(valid => {
      if (valid) {
        if (form.value.ruleType === "SOURCE_PRIORITY" && !validateSourcePriority()) return;
        const payload = buildSubmitPayload();
        const isUpdate = form.value.id != null;
        const request = isUpdate ? updateRulesurvivor : addRulesurvivor;
        request(payload).then(() => {
          proxy.$modal.msgSuccess(isUpdate ? "修改成功" : "新增成功");
          open.value = false;
          getList();
        }).catch(() => {});
      }
    });
  }

  /** 删除按钮操作 */
  function handleDelete(row) {
    const targetIds = row?.id ?? ids.value;
    if (!targetIds || (Array.isArray(targetIds) && !targetIds.length)) return;
    proxy.$modal.confirm('是否确认删除生存规则编号为"' + targetIds + '"的数据项？').then(function() {
      return delRulesurvivor(targetIds);
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }

  const selectedModelOption = computed(() => modelOptions.value.find(item => String(item.value) === String(form.value.modelId)));
</script>

<style scoped lang="scss">
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
