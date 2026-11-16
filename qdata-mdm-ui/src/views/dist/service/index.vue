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
  数据服务列表：查询、启停、删除服务，并打开编辑弹窗或详情页。
-->
<template>
  <div class="app-container">
    <guide-tip tip-id="dist/service.list" />
    <el-container>
      <qt-tree
        ref="modelTreeRef"
        :data="modelTreeData"
        :action-loading="modelTreeLoading"
        :editable="false"
        :default-expand="false"
        :default-expanded-keys="defaultExpandedKeys"
        node-key="treeKey"
        title="主数据模型"
        placeholder="搜索名称或编码"
        @node-click="handleModelTreeClick"
      />
      <el-main class="main-content">
    <qt-wrap :columns="tableStore.columns" :table-ref="tableRef">
      <template #search>
        <qt-search-bar v-bind="searchStore" :params="tableStore.params" :table-ref="tableRef" />
      </template>
      <template #actions-data>
        <el-button
          type="primary"
          plain
          icon="Plus"
          v-hasPermi="['dist:service:add']"
          @mousedown="(e) => e.preventDefault()"
          @click="openForm()"
        >
          新增
        </el-button>
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="!selectedRows.length || deleting"
          v-hasPermi="['dist:service:remove']"
          @click="handleDelete()"
        >
          批量删除
        </el-button>
      </template>
      <qt-table ref="tableRef" v-bind="tableStore">
        <template #name="{ row }">
          <div class="qt-table-info">
            <div class="qt-table-info__main">
              <el-link
                      v-if="proxy.$auth.hasPermi('dist:service:query')"
                      type="primary"
                      :underline="false"
                      class="info-title text-ellipsis"
                      :title="`${row.name}（${row.code}）`"
                      @click="openDetail(row)"
              >
                {{ row.name }}（{{ row.code }}）
              </el-link>

              <span
                      v-else
                      :title="`${row.name}（${row.code}）`"
                      class="info-title text-ellipsis"
              >
  {{ row.name }}（{{ row.code }}）
</span>
              <el-tag v-if="row.httpMethod" type="info">{{ row.httpMethod }}</el-tag>
            </div>
            <div :title="row.description" class="info-desc text-ellipsis">{{ row.description || '-' }}</div>
          </div>
        </template>
        <template #model="{ row }">
          <div v-if="row.modelId != null" class="qt-table-info model-info">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(modelDetails.get(String(row.modelId)) || {})" alt="" class="info-icon" />
              <el-text class="info-title text-ellipsis" :title="modelTitle(row.modelId)">{{ modelTitle(row.modelId) }}</el-text>
              <dict-tag v-if="modelDetails.get(String(row.modelId))?.type" :options="model_type" :value="modelDetails.get(String(row.modelId)).type" size="small" type="primary" />
              <dict-tag v-if="modelDetails.get(String(row.modelId))?.versionNo" :value="modelDetails.get(String(row.modelId)).versionNo" size="small" type="primary" />
            </div>
            <div class="info-desc text-ellipsis" :title="modelDetails.get(String(row.modelId))?.description || ''">{{ modelDetails.get(String(row.modelId))?.description || '-' }}</div>
          </div>
          <span v-else>-</span>
        </template>
        <template #location="{ row }">
          <div v-if="modelLocations.get(String(row.modelId))" class="flex-column flex-column-gap">
            <div class="qt-table-info__main">
              <img :src="getProjectIcon(modelLocations.get(String(row.modelId)).icon)" alt="" />
              <span class="text-ellipsis" :title="modelLocations.get(String(row.modelId)).project">{{ modelLocations.get(String(row.modelId)).project }}</span>
            </div>
            <div v-if="modelLocations.get(String(row.modelId)).layers" class="text-ellipsis">
              <dict-tag size="small" type="primary" :value="modelLocations.get(String(row.modelId)).layers" />
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template #apiType="{ row }">{{ serviceApiTypeLabel(row.apiType) }}</template>
        <template #callCount="{ row }">{{ row.callCount ?? 0 }}</template>
        <template #lastCallTime="{ row }">{{ row.lastCallTime || '-' }}</template>
        <template #serviceStatus="{ row }">
          <el-switch
            :model-value="row.status"
            :active-value="SERVICE_STATUS.ENABLED"
            :inactive-value="SERVICE_STATUS.DISABLED"
            :loading="!!statusLoading[row.id]"
            :disabled="!proxy.$auth.hasPermi('dist:service:edit') || !!statusLoading[row.id] || deleting"
            :aria-label="`服务${row.name}状态：${serviceStatusLabel(row.status)}`"
            :title="serviceStatusLabel(row.status)"
            @change="handleStatusSwitch(row)"
          />
        </template>
        <template #handle="{ row }">
          <el-button
            link
            type="primary"
            icon="View"
            v-hasPermi="['dist:service:query']"
            @click="openDetail(row)"
          >
            详情
          </el-button>
          <el-button
            link
            type="primary"
            icon="Edit"
            v-hasPermi="['dist:service:edit']"
            @click="openForm(row)"
          >
            修改
          </el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            v-hasPermi="['dist:service:remove']"
            :disabled="deleting"
            @click="handleDelete(row)"
          >删除</el-button>
        </template>
      </qt-table>
    </qt-wrap>
      </el-main>
    </el-container>
    <service-form-dialog
      v-if="formOpen"
      :service-id="editingId"
      :models="models"
      :models-loading="modelsLoading"
      :models-error="modelsError"
      @reload-models="loadModels"
      @close="formOpen = false"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup name="DistService">
import { computed, getCurrentInstance, markRaw, nextTick, onMounted, reactive, ref, watch } from 'vue';

import { useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';

import { changeServiceStatus, deleteService, listService, listServiceModels } from '@/api/dist/service/service';
import ServiceFormDialog from '@/views/dist/service/components/serviceFormDialog.vue';
import QtTree from '@/components/QtTree/index.vue';
import { usePublishedModelTree } from '@/composables/usePublishedModelTree';
import { getProjectIcon, getModelIcon } from '@/utils/imageUrl';
import { useDict } from '@/utils/dict';

const { model_type } = useDict('model_type');
const SERVICE_STATUS = Object.freeze({ DISABLED: '0', ENABLED: '1' });

const SERVICE_STATUS_OPTIONS = Object.freeze([
  { label: '启用', value: SERVICE_STATUS.ENABLED },
  { label: '停用', value: SERVICE_STATUS.DISABLED },
]);

const SERVICE_API_TYPE = Object.freeze({ SINGLE: '0', PAGE: '1' });

const SERVICE_API_TYPE_OPTIONS = Object.freeze([
  { label: '单条查询', value: SERVICE_API_TYPE.SINGLE },
  { label: '列表分页查询', value: SERVICE_API_TYPE.PAGE },
]);

function serviceApiTypeLabel(value) {
  return SERVICE_API_TYPE_OPTIONS.find(option => option.value === value)?.label || value || '-';
}

function serviceStatusLabel(value) {
  return SERVICE_STATUS_OPTIONS.find(option => option.value === String(value))?.label || value || '-';
}

const { proxy } = getCurrentInstance();
const router = useRouter();
const tableRef = ref(null);
const modelTreeRef = ref(null);
const { data: modelTreeData, loading: modelTreeLoading } = usePublishedModelTree(addTreeKeys);
const defaultExpandedKeys = computed(() => modelTreeData.value.map(node => node.treeKey));
const modelLocations = computed(() => {
  const locations = new Map();
  function visit(nodes, project = null, layers = []) {
    for (const node of nodes || []) {
      const currentProject = node.nodeType === 'project' ? node : project;
      const currentLayers = node.nodeType === 'layer' ? [...layers, node.name] : layers;
      if (node.nodeType === 'model' && node.modelId != null && currentProject) {
        locations.set(String(node.modelId), {
          project: currentProject.name,
          icon: currentProject.icon,
          layers: currentLayers.join(' / '),
        });
      }
      visit(node.children, currentProject, currentLayers);
    }
  }
  visit(modelTreeData.value);
  return locations;
});
const selectedRows = ref([]);
const deleting = ref(false);
const statusLoading = reactive({});
const formOpen = ref(false);
const editingId = ref(null);
const models = ref([]);
const modelsLoading = ref(false);
const modelsError = ref(false);
const modelNames = computed(() => new Map(models.value.map(model => [String(model.id), model.name])));
const modelDetails = computed(() => new Map(models.value.map(model => [String(model.id), model])));
/** 根据模型 ID 回显名称，模型不可用时保留 ID。 */
const modelName = id => modelNames.value.get(String(id)) || `${id}`;
const modelTitle = id => {
  const model = modelDetails.value.get(String(id));
  return model?.code ? `${modelName(id)} (${model.code})` : modelName(id);
};

const searchStore = reactive({
  items: [
    { label: '服务名称', prop: 'name', component: { is: 'input' } },
    { label: '服务编码', prop: 'code', component: { is: 'input', placeholder: '请输入服务编码' } },
    { label: 'API 路径', prop: 'servicePath', component: { is: 'input', placeholder: '请输入API路径' } },
    // 模型异步更新不改变控件宽度，避免触发搜索栏深度监听后临时展开重测。
    { label: '主数据模型', prop: 'modelId', component: markRaw({
      is: 'select', filterable: true,
      get loading() { return modelsLoading.value; },
      get options() { return models.value.map(model => ({ label: model.name, value: model.id })); },
    }) },
    { label: '服务状态', prop: 'status', component: {
      is: 'select', placeholder: '全部',
      options: [{ label: '全部', value: '' }, ...SERVICE_STATUS_OPTIONS],
    } },
  ],
});

const tableStore = reactive({
  func: listService,
  params: {},
  config: {
    table: {
      rowKey: 'id', stripe: true, onSelectionChange: rows => { selectedRows.value = rows; },
      defaultSort: { prop: 'createTime', order: 'descending' },
    },
    sort: true,
  },
  events: {
    formatParams: params => {
      if (!params.isAsc) {
        const requestParams = { ...params };
        delete requestParams.orderByColumn;
        delete requestParams.isAsc;
        return requestParams;
      }
      return {
        ...params,
        isAsc: ['ascending', 'asc'].includes(params.isAsc) ? 'asc' : 'desc',
      };
    },
  },
  columns: [
    { type: 'selection', width: 48 },
    { label: '编号', prop: 'id', width: 65, sortable: 'custom' },
    { label: '数据服务信息', prop: 'name', slot: 'name', minWidth: 340, align: 'left' },
    { label: '归属位置', slot: 'location', minWidth: 200, align: 'left' },
    { label: 'API 路径', prop: 'servicePath', minWidth: 185, align: 'left', showOverflowTooltip: true },
    { label: '近7天调用次数', slot: 'callCount', minWidth: 135 },
    { label: '最近调用时间', slot: 'lastCallTime', minWidth: 160 },
    { label: '服务状态', slot: 'serviceStatus', minWidth: 140 },
    { label: '创建人', minWidth: 145, list: [{ prop: 'createBy' }, { prop: 'creatorPhone' }], align: 'left' },
    { label: '创建时间', prop: 'createTime', date: true, minWidth: 165, sortable: 'custom' },
    { label: '操作', slot: 'handle', width: 300, fixed: 'right', className: 'is-handle', labelClassName: 'is-handle' },
  ],
});

function addTreeKeys(nodes = []) {
  return nodes.map(node => ({
    ...node,
    treeKey: `${node.nodeType}-${node.id}`,
    children: addTreeKeys(node.children || []),
  }));
}

function findModelNode(nodes, modelId) {
  for (const node of nodes || []) {
    if (node.nodeType === 'model' && String(node.modelId) === String(modelId)) return node;
    const found = findModelNode(node.children, modelId);
    if (found) return found;
  }
  return null;
}

function syncTreeSelection(modelId, expandParent = true) {
  const node = modelId == null || modelId === '' ? null : findModelNode(modelTreeData.value, modelId);
  modelTreeRef.value?.setCurrentKey(node?.treeKey ?? null, expandParent);
}

function handleModelTreeClick(node) {
  if (node.nodeType !== 'model' || node.modelId == null) {
    syncTreeSelection(tableStore.params.modelId, false);
    return;
  }
  tableStore.params.modelId = node.modelId;
  tableRef.value?.resetQuery();
}

watch([() => tableStore.params.modelId, modelTreeData], async () => {
  await nextTick();
  syncTreeSelection(tableStore.params.modelId);
}, { flush: 'post' });

/**
 * 加载已发布模型，供列表名称回显及查询、编辑共用。
 */
async function loadModels() {
  if (modelsLoading.value) return;
  modelsLoading.value = true;
  modelsError.value = false;
  try {
    models.value = await listServiceModels();
  } catch {
    modelsError.value = true;
  } finally {
    modelsLoading.value = false;
  }
}

/**
 * 打开新增或修改弹窗。
 * @param {Object} [row] 待修改服务，省略时新增。
 */
function openForm(row) {
  editingId.value = row?.id ?? null;
  formOpen.value = true;
}

/**
 * 跳转到所选服务详情。
 * @param {Object} row 服务记录。
 */
function openDetail(row) {
  router.push({ path: '/dist/service/detail', query: { id: row.id } });
}

/**
 * 保存后关闭弹窗并刷新当前列表。
 */
function handleSaved() {
  formOpen.value = false;
  tableRef.value?.getList();
}

/** 确认并保存成功后才更新开关；取消或失败时保留原状态。 */
async function handleStatusSwitch(row) {
  if (statusLoading[row.id] || deleting.value || !proxy.$auth.hasPermi('dist:service:edit')) return false;
  const status = row.status === SERVICE_STATUS.ENABLED ? SERVICE_STATUS.DISABLED : SERVICE_STATUS.ENABLED;
  const action = serviceStatusLabel(status);
  statusLoading[row.id] = true;
  try {
    await ElMessageBox.confirm(`是否确认${action}数据服务“${row.name}”？`, '系统提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    await changeServiceStatus({ id: row.id, status });
    row.status = status;
    proxy.$modal.msgSuccess(`${action}成功`);
    tableRef.value?.getList();
    return true;
  } catch {
    // 取消保持原状态；接口错误由公共请求拦截器提示。
    return false;
  } finally {
    delete statusLoading[row.id];
  }
}

/**
 * 确认后删除单条或已选服务，并重置列表查询。
 * @param {Object} [row] 单条服务，省略时处理多选记录。
 */
async function handleDelete(row) {
  const ids = row ? [row.id] : selectedRows.value.map(item => item.id);
  if (!ids.length || deleting.value) return;
  try {
    await ElMessageBox.confirm(row ? `是否确认删除数据服务“${row.name}”？` : `是否确认删除选中的 ${ids.length} 条数据服务？`, '系统提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning',
    });
    deleting.value = true;
    await deleteService(ids);
    proxy.$modal.msgSuccess('删除成功');
    selectedRows.value = [];
    tableRef.value?.resetQuery();
  } catch {
    // 取消确认时保持列表；请求错误由公共拦截器提示。
  } finally {
    deleting.value = false;
  }
}

onMounted(loadModels);
</script>

