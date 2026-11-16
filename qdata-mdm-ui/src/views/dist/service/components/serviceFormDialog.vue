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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<!--
  数据服务编辑向导：配置基本信息、查询条件与返回字段。
  props：serviceId 为待修改服务 ID；models 为已发布模型；modelsLoading、modelsError 表示模型加载状态。
  emits：close 关闭弹窗；saved 保存成功；reloadModels 请求父页重新加载模型。
-->
<template>
  <div ref="app-container">
    <el-dialog
      :model-value="true"
      :title="serviceId ? '修改数据服务' : '新增数据服务'"
      width="1100px"
      :append-to="$refs['app-container']"
      draggable
      :close-on-click-modal="false"
      :close-on-press-escape="!saving"
      :before-close="handleClose"
    >
      <div v-loading="loading">
        <steps
          :steps="SERVICE_STEPS"
          :active-step="step"
          is-dialog
          :clickable="false"
        />
        <el-empty v-if="detailError" description="数据服务加载失败">
          <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
        </el-empty>
        <template v-else>
          <el-form
            v-show="step === 0"
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="130px"
            :disabled="loading || saving"
            @submit.prevent
            class="column-form"
          >
            <div class="h2-title row-full">基本信息</div>
            <el-form-item label="数据服务名称" prop="name">
              <el-input v-model="form.name" maxlength="128" placeholder="请输入名称，支持中英文、数字、下划线和短横线" />
            </el-form-item>
            <qt-form-item label="主数据域" required>
              <el-select v-model="modelProjectId" class="field-select" placeholder="请选择主数据域"
                         :loading="modelTreeLoading" clearable filterable @change="handleProjectChange">
                <template #label="{ label }">
                  <div v-if="selectedProjectOption" class="source-option">
                    <img :src="getProjectIcon(selectedProjectOption.icon)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ selectedProjectOption.label }}</span>
                  </div>
                  <span v-else>{{ label }}</span>
                </template>
                <el-option v-for="item in projectOptions" :key="item.value" :label="item.label" :value="item.value">
                  <div class="source-option">
                    <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ item.label }}</span>
                  </div>
                </el-option>
              </el-select>
            </qt-form-item>
            <qt-form-item label="模型目录" required>
              <el-tree-select v-model="modelLayerId" class="field-select" :data="layerOptions"
                              :placeholder="modelProjectId != null ? '请选择模型目录' : '请先选择主数据域'"
                              :disabled="modelProjectId == null" check-strictly clearable filterable
                              @change="handleLayerChange" />
            </qt-form-item>
            <el-alert v-if="modelTreeError" title="主数据域和目录加载失败，请重试" type="error"
                      :closable="false" show-icon class="row-full">
              <el-button link type="primary" @click="loadModelTree">重新加载</el-button>
            </el-alert>
            <qt-form-item label="主数据模型" prop="modelId" required>
              <el-select
                v-model="form.modelId"
                filterable
                :loading="modelsLoading"
                :disabled="fieldsLoading || modelLayerId == null"
                :placeholder="modelLayerId != null ? '请选择已发布的主数据模型' : '请先选择模型目录'"
                @change="handleModelChange"
              >
                <template #label="{ label }">
                  <div v-if="selectedModelOption" class="source-option">
                    <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ selectedModelOption.name }}</span>
                  </div>
                  <span v-else>{{ label }}</span>
                </template>
                <el-option
                  v-if="unavailableModel"
                  :value="form.modelId"
                  :label="`${form.modelId}`"
                  disabled
                />
                <el-option
                  v-for="model in filteredModelOptions"
                  :key="model.id"
                  :label="model.name"
                  :value="model.id"
                >
                  <div class="source-option">
                    <img :src="getModelIcon(model)" alt="" class="source-option__icon" />
                    <span class="text-ellipsis">{{ model.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </qt-form-item>
            <el-alert
              v-if="modelsError"
              title="模型列表加载失败，请重试"
              type="error"
              :closable="false"
              show-icon
              class="row-full"
            >
              <el-button link type="primary" @click="emit('reloadModels')">重新加载模型</el-button>
            </el-alert>
            <el-form-item label="API 类型" prop="apiType">
              <el-select v-model="form.apiType" placeholder="选择接口类型，确定调用方访问该服务的方式">
                <el-option
                  v-for="option in SERVICE_API_TYPE_OPTIONS"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
            <qt-form-item label="API 路径" prop="servicePath" tip="填写服务访问路径，建议使用简洁、规范且有业务含义的路径">
              <el-input v-model="form.servicePath" maxlength="512" placeholder="例如：/master/customer" />
            </qt-form-item>
            <qt-form-item
              label="负责人"
              prop="responsibleUserId"
            >
              <el-select
                v-model="form.responsibleUserId"
                clearable
                filterable
                :loading="usersLoading"
                placeholder="请选择负责人"
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.value"
                  :label="user.label"
                  :value="user.value"
                />
              </el-select>
            </qt-form-item>
            <el-alert
              v-if="usersError"
              title="负责人列表加载失败，请重试"
              type="error"
              :closable="false"
              show-icon
              class="row-full"
            >
              <el-button link type="primary" @click="loadUserOptions">重新加载负责人</el-button>
            </el-alert>
            <qt-form-item label="联系电话">
              <el-input :model-value="responsiblePhone" disabled placeholder="请输入联系电话" />
            </qt-form-item>
            <el-form-item label="描述" prop="description" class="row-full">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                maxlength="256个字符"
                show-word-limit
                placeholder="简要说明服务用途、提供的数据内容及适用场景，最多256个字符"
              />
            </el-form-item>
          </el-form>
          <div v-show="step === 1" v-loading="fieldsLoading" class="column-form">
            <el-alert
              v-if="fieldsError"
              title="模型字段加载失败，请重试"
              type="error"
              :closable="false"
              show-icon
              class="row-full"
            >
              <el-button link type="primary" @click="loadFields(form.fields)">重新加载字段</el-button>
            </el-alert>
            <el-alert
              v-if="missingFieldNames.length"
              :title="`以下配置字段已不可用，请移除后保存：${missingFieldNames.join('、')}`"
              type="warning"
              :closable="false"
              show-icon
              class="row-full"
            >
              <el-button link type="primary" @click="removeUnavailableFields">移除不可用字段</el-button>
            </el-alert>
            <div class="h2-title row-full">查询条件</div>
            <div class="row-full">
              <qt-table ref="queryTableRef" v-bind="queryTable">
                <template #selected="{ row }">
                  <el-checkbox
                    v-model="row.queryableFlag"
                    :true-value="SERVICE_FLAG.YES"
                    :false-value="SERVICE_FLAG.NO"
                    :disabled="saving || row.unavailable"
                    :aria-label="`查询条件：${row.name}`"
                  />
                </template>
                <template #queryExampleValue="{ row }">
                  <el-input
                    v-model="row.queryExampleValue"
                    maxlength="512"
                    placeholder="请输入示例值"
                    :disabled="saving || row.unavailable"
                    :aria-label="`查询条件示例值：${row.name}`"
                  />
                </template>
              </qt-table>
            </div>
            <div class="h2-title row-full">返回字段</div>
            <div class="row-full">
              <qt-table ref="outputTableRef" v-bind="outputTable">
                <template #selected="{ row }">
                  <el-checkbox
                    v-model="row.outputFlag"
                    :true-value="SERVICE_FLAG.YES"
                    :false-value="SERVICE_FLAG.NO"
                    :disabled="saving || row.unavailable"
                    :aria-label="`返回字段：${row.name}`"
                  />
                </template>
                <template #exampleValue="{ row }">
                  <el-input
                    v-model="row.exampleValue"
                    maxlength="512"
                    placeholder="请输入示例值"
                    :disabled="saving || row.unavailable"
                    :aria-label="`示例值：${row.name}`"
                  />
                </template>
              </qt-table>
            </div>
          </div>
        </template>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="saving" @click="handleClose">取 消</el-button>
          <el-button v-if="step === 1" :disabled="saving" @click="step = 0">上一步</el-button>
          <el-button
            v-if="step === 0"
            type="primary"
            :disabled="loading || detailError || modelsLoading || modelsError"
            :loading="fieldsLoading"
            @click="nextStep"
          >下一步</el-button>
          <el-button
            v-else
            type="primary"
            :loading="saving"
            :disabled="fieldsLoading || fieldsError || !!missingFieldNames.length"
            @click="save"
          >确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DistServiceFormDialog">
import { getModelIcon, getProjectIcon } from "@/utils/imageUrl";
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { usePublishedModelTree, withModelTreeKeys } from '@/composables/usePublishedModelTree';

import Steps from '@/components/Steps/index.vue';
import { addService, getService, updateService } from '@/api/dist/service/service';
import { getPublishedModelStructure } from '@/api/model/base/base';
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import { trimText, validatePattern, validateTrimmedRequired } from '@/utils/validate';

const SERVICE_API_TYPE = Object.freeze({ SINGLE: '0', PAGE: '1' });

const SERVICE_API_TYPE_OPTIONS = Object.freeze([
  { label: '单条查询', value: SERVICE_API_TYPE.SINGLE },
  { label: '列表分页查询', value: SERVICE_API_TYPE.PAGE },
]);

const SERVICE_FIELD_COLUMNS = Object.freeze([
  { label: '字段名称', prop: 'name', minWidth: 140, align: 'left' },
  { label: '字段编码', prop: 'code', minWidth: 140, align: 'left' },
  { label: '字段类型', prop: 'dataType', minWidth: 100 },
]);

const SERVICE_FLAG = Object.freeze({ NO: '0', YES: '1' });

const SERVICE_PATH_PATTERN = /^\/[^\s?#]*$/;

const SERVICE_STEPS = Object.freeze([
  { title: '基本信息' },
  { title: '查询与返回' },
]);

const userDirectoryStore = useUserDirectoryStore();

const props = defineProps({
  serviceId: { type: [Number, String], default: null },
  models: { type: Array, default: () => [] },
  modelsLoading: { type: Boolean, default: false },
  modelsError: { type: Boolean, default: false },
});
const emit = defineEmits(['close', 'saved', 'reloadModels']);
const { proxy } = getCurrentInstance();
const step = ref(0);
const loading = ref(false);
const saving = ref(false);
const detailError = ref(false);
const fieldsLoading = ref(false);
const fieldsError = ref(false);
const formRef = ref(null);
const queryTableRef = ref(null);
const outputTableRef = ref(null);
const usersLoading = ref(false);
const usersError = ref(false);
const userOptions = ref([]);
const form = reactive({ name: '', modelId: null, apiType: null, servicePath: '', responsibleUserId: null, description: '', remark: '', fields: [] });
const { data: modelTreeData, loading: modelTreeLoading, reload: reloadModelTree } = usePublishedModelTree(withModelTreeKeys, false);
const modelTreeError = ref(false);
const modelProjectId = ref(null);
const modelLayerId = ref(null);
const locationInitialized = ref(false);
const projectOptions = computed(() => modelTreeData.value.map(node => ({
  value: node.projectId, label: node.name || node.code, icon: node.icon
})).filter(item => item.value != null));
const selectedProjectOption = computed(() => projectOptions.value.find(item => String(item.value) === String(modelProjectId.value)));
const layerOptions = computed(() => {
  const project = modelTreeData.value.find(node => String(node.projectId) === String(modelProjectId.value));
  const toLayers = nodes => (nodes || []).filter(node => node.nodeType === 'layer')
    .map(node => ({ value: node.layerId, label: node.name || node.code, children: toLayers(node.children) }));
  return toLayers(project?.children);
});
const modelLocations = computed(() => {
  const locations = new Map();
  function visit(nodes, projectId = null, layerId = null) {
    for (const node of nodes || []) {
      const project = node.projectId ?? projectId;
      const layer = node.nodeType === 'layer' ? (node.layerId ?? node.id) : (node.layerId ?? layerId);
      if (node.modelId != null && project != null && layer != null) locations.set(String(node.modelId), { projectId: project, layerId: layer });
      visit(node.children, project, layer);
    }
  }
  visit(modelTreeData.value);
  return locations;
});
const filteredModelOptions = computed(() => props.models.filter(model => {
  const location = modelLocations.value.get(String(model.id));
  return location && modelProjectId.value != null && modelLayerId.value != null &&
    String(location.projectId) === String(modelProjectId.value) && String(location.layerId) === String(modelLayerId.value);
}));
watch([modelLocations, () => form.modelId], ([locations, modelId]) => {
  if (!props.serviceId || locationInitialized.value || modelId == null) return;
  const location = locations.get(String(modelId));
  if (!location) return;
  modelProjectId.value = location.projectId;
  modelLayerId.value = location.layerId;
  locationInitialized.value = true;
}, { immediate: true });
async function loadModelTree() {
  modelTreeError.value = false;
  try { await reloadModelTree(); } catch { modelTreeError.value = true; }
}
function handleProjectChange() {
  locationInitialized.value = true;
  modelLayerId.value = null;
  handleLayerChange();
}
function handleLayerChange() {
  locationInitialized.value = true;
  fieldsRequest += 1;
  fieldsLoading.value = false;
  fieldsError.value = false;
  form.modelId = null;
  form.fields = [];
  refreshFields();
}
const originalName = ref('');
const responsiblePhone = computed(() => userOptions.value.find(user => String(user.value) === String(form.responsibleUserId))?.phone || '');
const unavailableModel = computed(() => form.modelId != null && !props.modelsLoading && !props.models.some(model => String(model.id) === String(form.modelId)));
const missingFieldNames = computed(() => form.fields.filter(field => field.unavailable).map(field => field.name));
let fieldsRequest = 0;

const rules = {
  name: [
    { required: true, validator: validateTrimmedRequired('数据服务名称不能为空', 128), trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (props.serviceId && value === originalName.value) return callback();
        return validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, '数据服务名称只允许中文、英文字母、数字、下划线和短横线')(rule, value, callback);
      },
      trigger: 'blur',
      transform: trimText
    }
  ],
  modelId: [{ required: true, message: '请选择已发布的主数据模型', trigger: 'change' }],
  apiType: [{ required: true, message: '请选择API类型', trigger: 'change' }],
  servicePath: [
    { required: true, message: '请输入API路径', trigger: 'blur' },
    { pattern: SERVICE_PATH_PATTERN, message: '路径须以 / 开头，且不能包含空白、查询参数或锚点', trigger: 'blur' },
  ],
};

const fieldTable = {
  func: () => Promise.resolve({ data: form.fields }),
  config: {
    autoPagination: true, initResquest: false,
    table: { rowKey: 'modelColumnId', maxHeight: 230, stripe: true },
    pagination: { pageSizes: [10, 20, 50, 100], pagerCount: 5, notAutoScroll: true },
  },
};
const queryTable = { ...fieldTable, columns: [
  { label: '选择', slot: 'selected', width: 65 }, ...SERVICE_FIELD_COLUMNS,
  { label: '示例值', prop: 'queryExampleValue', slot: 'queryExampleValue', minWidth: 160, align: 'left', headerAlign: 'center' },
] };
const outputTable = { ...fieldTable, columns: [
  { label: '选择', slot: 'selected', width: 65 }, ...SERVICE_FIELD_COLUMNS,
  { label: '示例值', prop: 'exampleValue', slot: 'exampleValue', minWidth: 160, align: 'left', headerAlign: 'center' },
] };

/**
 * 字段更新后等待表格挂载，再重置两张表的分页与数据。
 */
function refreshFields() {
  nextTick(() => {
    queryTableRef.value?.resetQuery();
    outputTableRef.value?.resetQuery();
  });
}

/** 按字段 ID 合并已保存配置，避免修改时丢失勾选、别名、排序及备注。 */
async function loadFields(savedFields = []) {
  const requestId = ++fieldsRequest;
  fieldsLoading.value = true;
  fieldsError.value = false;
  try {
    const response = await getPublishedModelStructure(form.modelId);
    if (requestId !== fieldsRequest) return;
    const savedById = new Map(savedFields.map(field => [String(field.modelColumnId), field]));
    const availableIds = new Set(response.data.fields.map(column => String(column.id)));
    const fields = response.data.fields.map((column, index) => {
      const saved = savedById.get(String(column.id));
      return {
        modelColumnId: column.id, name: column.name, code: column.code, dataType: column.dataType,
        queryableFlag: saved?.queryableFlag ?? SERVICE_FLAG.NO,
        outputFlag: saved?.outputFlag ?? SERVICE_FLAG.NO,
        exampleValue: saved?.exampleValue ?? '',
        queryExampleValue: saved?.queryExampleValue ?? '',
        fieldAlias: saved?.fieldAlias || '', sortOrder: saved?.sortOrder ?? index, remark: saved?.remark || '',
      };
    });
    // 模型重新发布后移除的配置需要用户明确移除，不能在打开修改弹窗时静默丢弃。
    const unavailable = savedFields.filter(field => !availableIds.has(String(field.modelColumnId)))
      .map(field => ({ ...field, name: `字段 ID：${field.modelColumnId}`, unavailable: true }));
    form.fields = [...fields, ...unavailable];
    refreshFields();
  } catch {
    if (requestId === fieldsRequest) fieldsError.value = true;
  } finally {
    if (requestId === fieldsRequest) fieldsLoading.value = false;
  }
}

/**
 * 切换模型后清空旧字段配置，加载新模型的已发布字段。
 */
function handleModelChange() {
  form.fields = [];
  refreshFields();
  loadFields();
}

/**
 * 按用户操作移除模型已删除的字段配置，再刷新字段表格。
 */
function removeUnavailableFields() {
  form.fields = form.fields.filter(field => !field.unavailable);
  refreshFields();
}

/** 复用系统用户接口，电话随负责人回显，不作为可编辑字段提交。 */
async function loadUserOptions() {
  usersLoading.value = true;
  usersError.value = false;
  try {
    const response = await userDirectoryStore.loadUserList();
    userOptions.value = (response.rows || response.data?.rows || [])
      .filter(user => user.nickName || user.userName)
      .map(user => ({ value: user.userId, label: user.nickName || user.userName, phone: user.phonenumber }));
    const selected = userOptions.value.find(user => String(user.value) === String(form.responsibleUserId));
    if (selected) {
      form.responsibleUserId = selected.value;
    } else if (form.responsibleUserId != null) {
      userOptions.value.unshift({ value: form.responsibleUserId, label: form.responsiblePersonName || String(form.responsibleUserId), phone: form.responsiblePersonPhone });
    }
  } catch {
    usersError.value = true;
  } finally {
    usersLoading.value = false;
  }
}

/**
 * 修改时回填服务配置，兼容模型 ID 的字符串与数字类型。
 */
async function loadDetail() {
  if (!props.serviceId) return;
  loading.value = true;
  detailError.value = false;
  try {
    const response = await getService(props.serviceId);
    Object.assign(form, response.data, { apiType: response.data.apiType ?? null });
    originalName.value = trimText(form.name);
    // 下拉选项和详情可能分别返回数字或字符串 ID，统一使用当前选项的实际值。
    const model = props.models.find(item => String(item.id) === String(form.modelId));
    if (model) form.modelId = model.id;
    await loadFields(form.fields || []);
  } catch {
    detailError.value = true;
  } finally {
    loading.value = false;
  }
}

/**
 * 校验基本信息及模型可用性后进入字段配置。
 */
async function nextStep() {
  if (fieldsLoading.value || loading.value) return;
  if (!await formRef.value.validate().catch(() => false)) return;
  if (modelTreeLoading.value || modelTreeError.value) {
    proxy.$modal.msgWarning('请等待主数据域和目录加载完成');
    return;
  }
  if (modelProjectId.value == null || modelLayerId.value == null) {
    proxy.$modal.msgWarning('请选择主数据域和模型目录');
    return;
  }
  if (!filteredModelOptions.value.some(model => String(model.id) === String(form.modelId))) {
    proxy.$modal.msgWarning('所选模型归属不可用，请重新选择');
    return;
  }
  if (unavailableModel.value) {
    proxy.$modal.msgWarning('请选择可用的已发布主数据模型');
    return;
  }
  step.value = 1;
  if (fieldsError.value || !form.fields.length) await loadFields(form.fields);
}

/**
 * 校验返回字段后整体提交服务配置；失败时保留输入。
 */
async function save() {
  if (saving.value || fieldsLoading.value || fieldsError.value || missingFieldNames.value.length) return;
  const outputs = form.fields.filter(field => field.outputFlag === SERVICE_FLAG.YES);
  if (!outputs.length) {
    proxy.$modal.msgWarning('至少选择一个返回字段');
    return;
  }
  const names = outputs.map(field => field.fieldAlias?.trim() || field.code);
  if (new Set(names).size !== names.length) {
    proxy.$modal.msgWarning('返回字段编码或别名不能重复');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      id: props.serviceId || undefined, name: form.name.trim(), modelId: form.modelId,
      apiType: form.apiType,
      responsibleUserId: form.responsibleUserId || null,
      servicePath: form.servicePath, description: form.description, remark: form.remark,
      fields: form.fields.map(({ modelColumnId, queryableFlag, outputFlag, fieldAlias, exampleValue, queryExampleValue, sortOrder, remark }) => ({
        modelColumnId, queryableFlag, outputFlag, fieldAlias: fieldAlias?.trim() || '', exampleValue, queryExampleValue, sortOrder, remark,
      })),
    };
    await (props.serviceId ? updateService(payload) : addService(payload));
    proxy.$modal.msgSuccess(props.serviceId ? '修改成功' : '新增成功');
    emit('saved');
  } catch {
    // 保存失败保留当前配置，供用户修正或重试。
  } finally {
    saving.value = false;
  }
}

/**
 * 保存期间阻止关闭，避免丢失当前提交状态。
 */
function handleClose() {
  if (!saving.value) emit('close');
}

onMounted(async () => {
  loadModelTree();
  await loadDetail();
  await loadUserOptions();
});
onBeforeUnmount(() => { fieldsRequest += 1; });

const selectedModelOption = computed(() => props.models.find(item => String(item.id) === String(form.modelId)));
</script>

<style scoped>
.field-select {
  width: 100%;
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
