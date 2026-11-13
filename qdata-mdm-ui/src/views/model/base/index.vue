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
  <div ref="app-container" class="model-base-page" :class="embedded ? 'model-base-embedded' : 'app-container'">

    <guide-tip v-if="!embedded" tip-id="model/base.list"/>
    <el-container>
      <qt-tree
          v-if="!embedded"
          ref="modelTreeRef"
          :data="modelTreeData"
          :action-loading="modelTreeLoading"
          :use-builtin-cat-edit-dialog="false"
          :tree-props="{ label: 'name', children: 'children' }"
          node-key="treeKey"
          :default-expand="false"
          :default-expanded-keys="modelTreeExpandedKeys"
          title="模型树"
          placeholder="请输入主数据域或模型目录名称"
          @node-click="handleModelTreeClick"
          @tree-change="handleModelTreeChange"
      />
      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
          <template #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                @query="handleQuery"
                @reset="resetQuery"
            />
          </template>

          <template v-if="!embedded" #actions-data>
            <el-button
                v-hasPermi="['model:base:add']"
                type="primary"
                plain
                icon="Plus"
                @click="handleAdd"
                @mousedown="(e) => e.preventDefault()"
            >
              新增
            </el-button>
            <el-button
                v-hasPermi="['model:base:remove']"
                type="danger"
                plain
                icon="Delete"
                :loading="batchAction === 'delete'"
                :disabled="!canBatchDelete || actionBusy"
                @click="handleBatchDelete"
            >批量删除
            </el-button>
            <el-button
                v-hasPermi="['model:base:edit']"
                type="primary"
                plain
                icon="Upload"
                :disabled="!canBatchPublish || actionBusy"
                @click="handleBatchPublish"
            >批量发布
            </el-button>
            <el-button
                v-hasPermi="['model:base:edit']"
                type="danger"
                plain
                icon="Remove"
                :loading="batchAction === 'disable'"
                :disabled="!canBatchDisable || actionBusy"
                @click="handleBatchDisable"
            >批量停用
            </el-button>
          </template>

          <qt-table v-bind="tableStore" ref="tableRef">
            <template #modelInfo="{ row }">
              <!-- 标题自己处理省略号，悬浮全文交给本列的 showOverflowTooltip，避免出现两层提示。 -->
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(row)" alt="" class="info-icon"/>
                  <span
                      class="info-title model-info-title text-ellipsis"
                      role="link"
                      tabindex="0"
                      @click="handleDetail(row)"
                      @keydown.enter="handleDetail(row)"
                      @keydown.space.prevent="handleDetail(row)"
                  >{{ row.name ? `${row.name} (${row.code})` : '-' }}</span>
                  <dict-tag :options="model_type" :value="row.type" size="small" type="primary"/>
                  <dict-tag
                      class="ml4 pointer"
                      :options="getVersionTagOptions(row)"
                      :value="row.version?.status"
                      size="small"
                      @click="handleDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.description || '-' }}</div>
              </div>
            </template>
            <template #responsiblePerson="{ row }">
              <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)"
                >{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text"
                >{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #creator="{ row }">
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)"
                >{{ row.createBy || getResponsibleName(row.creatorId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text">{{
                    getResponsiblePhone(row.creatorId)
                  }}</span>
              </div>
            </template>
            <template #layers="{ row }">
              <!-- 目录按从父到子的顺序返回，最后一项就是模型直接所属的目录。 -->
              <div v-if="row.projectName || row.layers?.length" class="flex-column flex-column-gap">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(row.projectIcon)" alt=""/>
                  <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
                </div>
                <div v-if="row.layers?.length" class="text-ellipsis flex-center">
                  <dict-tag size="small"
                            type="primary"
                            :value="row.layers[row.layers.length - 1].name"
                  />
                </div>
              </div>
              <span v-else class="text-muted">-</span>
            </template>
            <template #structuralOverview="{ row }">
              <!-- 每个标签分别定位到详情页的对应页签，数量为零时也能进入查看空列表。 -->
              <el-space :size="6" wrap>
                <qt-tag-group
                    v-for="structure in [
                      { tab: 'business', label: '业务字段' + getStructuralCount(row, 'BUSINESS') },
                      { tab: 'relation', label: '关联字段' + getStructuralCount(row, 'RELATION') },
                      { tab: 'class', label: '分类字段' + getStructuralCount(row, 'CLASS') },
                      { tab: 'subModels', label: getSubModelCount(row) > 0 ? '子模型' + getSubModelCount(row) : '无子模型' },
                    ]"
                    :key="structure.tab"
                    class="pointer"
                    type="none"
                    :max-count="1"
                    :items="[structure.label]"
                    @click="handleDetail(row, structure.tab)"
                />
              </el-space>
            </template>
            <template #version="{ row }">
              <dict-tag :options="model_status"
                        :value="row.version?.status"
              />
            </template>
            <template #updateTime="{ row }">
              <span>{{ row.version.updateTime }}</span>
            </template>
            <template #action="{ row }">
              <el-button
                  v-hasPermi="['model:base:query']"
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(row)"
              >详情
              </el-button>
              <el-button
                  v-hasPermi="['model:base:edit']"
                  link
                  type="primary"
                  icon="Edit"
                  :disabled="Number(row.version?.status) === 3 || !(row.version?.status === 1 || row.version?.status === 2 || row.version?.status === 0)"
                  @click="handleRevision(row)"
              >修改
              </el-button>
              <el-popover
                  placement="bottom"
                  :width="100"
                  trigger="click"
                  popper-class="table-action-popover handle-popover"
                  :disabled="row.version?.status === 3"
              >
                <template #reference>
                  <el-button
                      link
                      type="primary"
                      icon="ArrowDown"
                      :disabled="row.version?.status === 3"
                  >
                    更多
                  </el-button>
                </template>
                <div class="butgdlist task-action-list">
                  <!--              <el-button v-if="row.version?.status === 0 || row.version?.status === 2" link type="primary"
                             icon="CircleCheck"
                             @click="handleModelValidate(row)"
                  >
                    模型校验
                  </el-button>-->
                  <el-button
                      v-hasPermi="['model:base:edit']"
                      :disabled="!canPublish(row) || actionBusy"
                      link
                      type="primary"
                      icon="Upload"
                      @click="handlePublish(row)"
                  >
                    {{ row.version?.status === 0 ? '发布模型' : '版本发布' }}
                  </el-button>
                  <el-button
                      v-hasPermi="['model:base:edit']"
                      link
                      type="primary"
                      icon="RefreshLeft"
                      @click="handleDetail(row, 'versions')"
                  >
                    版本管理
                  </el-button>
                  <el-button
                      v-hasPermi="['model:base:edit']"
                      link
                      type="primary"
                      icon="Clock"
                      @click="handleDetail(row, 'history')"
                  >
                    发布历史
                  </el-button>
                  <el-button
                      v-hasPermi="['model:base:edit']"
                      :disabled="!canDisable(row) || actionBusy"
                      link
                      type="danger"
                      icon="Remove"
                      @click="handleDisable(row)"
                  >
                    停用模型
                  </el-button>
                  <el-button
                      v-hasPermi="['model:base:remove']"
                      :disabled="!canDelete(row) || actionBusy"
                      link
                      type="danger"
                      icon="CloseBold"
                      @click="handleDelete(row)"
                  >删除草稿
                  </el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 发布主数据模型对话框 -->
    <el-dialog
        v-model="publishDialog.visible"
        :title="publishDialog.batch ? '批量发布主数据模型' : '发布主数据模型'"
        width="800px"
        append-to-body
        draggable
        destroy-on-close
        class="model-publish-dialog"
        :close-on-click-modal="!publishDialog.submitting"
        :close-on-press-escape="!publishDialog.submitting"
        :show-close="!publishDialog.submitting"
        :append-to="$refs['app-container']"
        @closed="resetPublishDialog"
    >
      <guide-tip-panel
          title="发布前提：模型校验通过，并满足平台已有审核能力要求。若新版本发布失败，当前正式版本和正式物理结构保持不变。"
          type="primary"
          allow-wrap
      />

      <el-form
          ref="publishFormRef"
          v-loading="publishDialog.loading"
          :model="publishDialog.form"
          :rules="publishRules"
          @submit.prevent
          class="mt15"
      >
        <qt-form-item
            label="数据连接名称"
            prop="datasourceId"
            :tip="{
            content: publishDialog.datasourceLocked ? '已成功发布，固定使用首次发布的数据连接，不支持更换' : '请选择业务数据库，批量发布的模型共用此连接',
          }"
        >
          <el-input
              v-if="publishDialog.datasourceLocked"
              :model-value="publishDialog.form.datasourceName"
              placeholder="正在读取已发布的数据连接"
              disabled
          />
          <datasource-list
              v-else
              v-model="publishDialog.form.datasourceId"
              :disabled="publishDialog.loading || publishDialog.submitting"
              placeholder="请选择数据连接名称"
              flag="modelMaterialization"
              @change="handleDatasourceChange"
          />
        </qt-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button :disabled="publishDialog.submitting" @click="publishDialog.visible = false">取消</el-button>
          <el-button
              type="primary"
              :loading="publishDialog.submitting"
              :disabled="publishDialog.loading"
              @click="submitPublish"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 主数据模型基本信息详情对话框 -->
    <el-dialog
        v-model="openDetail"
        :title="title"
        width="800px"
        :append-to="$refs['app-container']"
        draggable
    >
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="baseRef" :model="form" label-width="80px">
        <el-row :gutter="20">
          <el-col :span="12">
            <qt-form-item label="模型编码，例如MDM_SUPPLIER；业务上要求系统内唯一" prop="code">
              <div>
                {{ form.code }}
              </div>
            </qt-form-item>
          </el-col>
          <el-col :span="12">
            <qt-form-item label="模型名称" prop="name">
              <div>
                {{ form.name }}
              </div>
            </qt-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <qt-form-item label="所属主数据域ID，关联MODEL_PROJECT.ID" prop="projectId">
              <div>
                {{ form.projectId }}
              </div>
            </qt-form-item>
          </el-col>
          <el-col :span="12">
            <qt-form-item label="所属模型目录ID，关联MODEL_LAYER.ID；目录必须属于PROJECT_ID对应的主数据域" prop="layerId">
              <div>
                {{ form.layerId }}
              </div>
            </qt-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <qt-form-item label="模型类型；0：普通模型，1：分类模型" prop="type">
              <div>
                {{ form.type }}
              </div>
            </qt-form-item>
          </el-col>
          <el-col :span="12">
            <qt-form-item label="当前正式生效版本号，草稿模型尚未发布时为空，修改中仍保存当前正式版本号" prop="versionId">
              <div>
                {{ form.versionId }}
              </div>
            </qt-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <qt-form-item label="模型业务描述" prop="description">
              <div>
                {{ form.description }}
              </div>
            </qt-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="ModelBase">

import {useDict} from "@/utils/dict";

/** @description 主数据模型列表，负责查询、版本操作与发布。 */
import {getModelIcon, getProjectIcon} from "@/utils/imageUrl";
import {useUserDirectory} from '@/store/system/userDirectory';
import {
  listBase,
  publishBase,
  disableBase,
  delBase,
  batchPublishBase,
  batchDisableBase,
  batchDeleteBase,
  getPublishedModelMaterialization
} from "@/api/model/base/base";
import {getDaDatasource} from "@/api/ingest/datasource/dataSource";
import QtTree from "@/components/QtTree/index.vue";
import {treeProject} from "@/api/model/project/project";
import {treeLayer} from "@/api/model/layer/layer";

import DatasourceList from "@/components/Datasource/List.vue";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const {responsibleUserList, getResponsibleName, getResponsiblePhone} = useUserDirectory();

const {model_type, model_status} = useDict('model_type', 'model_status');

/** 沿用状态字典的标签样式，展示文字使用发布版本号。 */
function getVersionTagOptions(row) {
  return model_status.value.map(item => ({
    ...item,
    label: row.releaseVersion?.versionNo || row.version?.versionNo || '尚未发布',
  }));
}

const {proxy} = getCurrentInstance();
const props = defineProps({
  embedded: {type: Boolean, default: false},
  search: {type: Boolean, default: false},
  projectId: {type: [String, Number], default: null},
  layerId: {type: [String, Number], default: null},
});
const emit = defineEmits(['modelCount']);

const openDetail = ref(false);
const ids = ref([]);
const selectedRows = ref([]);
const batchAction = ref("");
const title = ref("");
const publishFormRef = ref();

const createPublishForm = () => ({
  datasourceId: "",
});

const publishDialog = reactive({
  visible: false,
  submitting: false,
  loading: false,
  items: [],
  batch: false,
  datasourceLocked: false,
  form: createPublishForm(),
});

// 单条和批量操作使用同一套状态判断，选中项必须全部符合条件才开放批量按钮。
const canDelete = row => row?.version?.status === 0;
const canPublish = row => [0, 2].includes(row?.version?.status);
const canDisable = row => row?.version?.status === 1;
const canBatchDelete = computed(() => selectedRows.value.length > 0 && selectedRows.value.every(canDelete));
const canBatchPublish = computed(() => selectedRows.value.length > 0 && selectedRows.value.every(canPublish));
const canBatchDisable = computed(() => selectedRows.value.length > 0 && selectedRows.value.every(canDisable));
const actionBusy = computed(() => Boolean(batchAction.value) || publishDialog.visible);

const publishRules = {
  datasourceId: [
    {required: true, message: "请选择数据连接名称", trigger: "change"},
  ],
};

const data = reactive({
  form: {},
  queryParams: {
    code: null,
    name: null,
    projectId: null,
    responsibleUserId: null,
    layerId: null,
    type: null,
    versionId: null,
    description: null,
    createTime: null,
  },
});

const {queryParams, form} = toRefs(data);

const tableRef = ref(null);
const modelTreeRef = ref(null);
const modelTreeExpandedKeys = ref([]);
const modelTreeData = ref([]);
const modelTreeLoading = ref(false);
let modelTreeRequestVersion = 0;
const treeScope = ref({});
const selectedTreeKey = ref(null);
const tableStore = reactive({
  config: {
    fullContent: !props.embedded,
    search: props.search,
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: {prop: "createTime", order: "descending"},
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    ...(!props.embedded ? [{type: "selection", width: 55, align: "center", fixed: "left"}] : []),
    {label: "编号", prop: "id", width: 60, align: "center", sortable: true, fixed: "left"},
    {
      label: '模型信息',
      minWidth: 380,
      align: 'left',
      slot: 'modelInfo',
      showOverflowTooltip: {effect: 'light'},
    },
    {
      label: "归属位置",
      prop: "layers",
      align: "left",
      minWidth: 200,
      slot: "layers",
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "结构概览", prop: "id", align: "light",
      minWidth: 380,
      slot: "structuralOverview",
    },
    {
      label: "状态", prop: "version",
      slot: "version",
      minWidth: 140,
    },
    {
      label: "负责人", prop: "responsibleUserId", slot: "responsiblePerson", align: "light", minWidth: 120
    },
    {
      label: "创建人", prop: "creatorId", slot: "creator", align: "light", minWidth: 120
    },
    {
      label: "创建时间",
      prop: "createTime",
      minWidth: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    ...(!props.embedded ? [{label: "操作", minWidth: 220, slot: "action", fixed: "right"}] : []),
  ],
  func: loadModelList,
  params: queryParams,
});

const searchStore = computed(() => ({
  // 用户列表异步加载完成后，下拉选项也要跟着更新，所以这里不能只在初始化时取一次。
  items: [
    {label: "模型名称", prop: "name", component: {is: "input"}},
    {label: "模型编码", prop: "code", component: {is: "input"}},
    {
      label: "负责人",
      prop: "responsibleUserId",
      component: {is: "select", filterable: true, clearable: true, options: responsibleUserList.value}
    },
    {
      label: "模型类型",
      prop: "type",
      component: {
        is: "select",
        placeholder: "请选择模型类型",
        options: model_type.value,
      },
    },
    {
      label: "状态",
      prop: "status",
      component: {
        is: "select",
        placeholder: "请选择状态",
        options: model_status.value,
      },
    },
  ],
}));

/** 按当前树节点范围查询，搜索条件和分页由右侧表格维护。 */
function loadModelList(params) {
  // 详情内的查询始终限定在当前域或目录，搜索、重置和分页都不能改变该范围。
  const scope = props.embedded
      ? {projectId: props.projectId, layerId: props.layerId, includeDescendantLayers: props.layerId != null}
      : treeScope.value;
  return listBase({...params, ...scope}).then(response => {
    emit('modelCount', response.data.total);
    return response;
  });
}

/** 复用启用域、启用目录接口，组合为域下目录树，不包含模型节点。 */
async function reloadModelTree() {
  // 每次刷新递增编号，旧请求晚返回或页面已关闭时，不再采用它的结果。
  const version = ++modelTreeRequestVersion;
  modelTreeLoading.value = true;
  try {
    const [projectResponse, layerResponse] = await Promise.all([
      treeProject(),
      treeLayer({validFlag: true, orderByColumn: 'sortOrder', isAsc: 'ascending'}),
    ]);
    if (version !== modelTreeRequestVersion) return;
    const projects = (projectResponse.data || []).map(project => ({
      ...project, nodeType: 'project', projectId: project.id,
      treeKey: `project:${project.id}`, children: [],
    }));
    // 域和目录可能有相同 ID，用不同前缀作为树节点 key，避免选中或展开错节点。
    const projectsById = new Map(projects.map(project => [String(project.id), project]));
    const layers = (layerResponse.data || []).map(layer => ({
      ...layer, nodeType: 'layer', layerId: layer.id,
      treeKey: `layer:${layer.id}`, children: [],
    }));
    const layersById = new Map(layers.map(layer => [String(layer.id), layer]));
    // 顶级目录挂到所属域，下级目录挂到父目录；域不存在或父目录属于别的域时不混挂。
    for (const layer of layers) {
      const project = projectsById.get(String(layer.projectId));
      if (!project) continue;
      if (String(layer.parentId) === '0') {
        project.children.push(layer);
      } else {
        const parent = layersById.get(String(layer.parentId));
        if (parent && String(parent.projectId) === String(layer.projectId)) {
          parent.children.push(layer);
        }
      }
    }
    // 只展开主数据域，显示第二级目录，更深目录保持收起。
    modelTreeExpandedKeys.value = projects.map(project => project.treeKey);
    modelTreeData.value = projects;
  } finally {
    // 旧请求结束时不能关掉新一轮请求的加载提示。
    if (version === modelTreeRequestVersion) modelTreeLoading.value = false;
  }
}

// 首次进入就加载域和目录；接口错误由公共请求工具提示。
onMounted(() => {
  if (props.embedded) return;
  reloadModelTree().catch(() => {
  });
});
// 页面销毁后，未完成的树请求全部作废。
onBeforeUnmount(() => {
  modelTreeRequestVersion++;
});

/** 点击域或目录时固定查询范围，目录包含所有后代目录。 */
function handleModelTreeClick(node) {
  selectedTreeKey.value = node.treeKey;
  if (node.nodeType === 'layer') {
    treeScope.value = {id: null, projectId: node.projectId, layerId: node.layerId, includeDescendantLayers: true};
  } else if (node.nodeType === 'project') {
    treeScope.value = {id: null, projectId: node.projectId, layerId: null, includeDescendantLayers: false};
  } else {
    treeScope.value = {id: null, projectId: null, layerId: null, includeDescendantLayers: false};
  }
  ids.value = [];
  tableRef.value?.resetQuery();
}

/** 删除或移动模型后，树节点失效时回到全部模型。 */
async function handleModelTreeChange(nodes) {
  const find = list => {
    for (const node of list || []) {
      if (node.treeKey === selectedTreeKey.value) return node;
      const found = find(node.children);
      if (found) return found;
    }
    return null;
  };
  const selected = find(nodes);
  // 等树按新数据渲染后再恢复选中；节点已被删掉时清空筛选，不再查询失效 ID。
  await nextTick();
  if (selectedTreeKey.value != null && !selected) handleModelTreeClick({treeKey: null});
  modelTreeRef.value?.setCurrentKey(selectedTreeKey.value, false);
}

/** 维护后同步刷新模型列表和左侧树。 */
function getList() {
  tableRef.value?.getList();
  if (props.embedded) return;
  reloadModelTree().catch(() => {
  });
}

let activatedOnce = false;
// 首次打开已经会初始化；从详情返回缓存页面时，再同步刷新列表和树。
onActivated(() => {
  if (activatedOnce) getList();
  activatedOnce = true;
});

/** 统计模型指定分类的字段数量，列表接口未返回字段时按 0 展示。 */
function getStructuralCount(row, category) {
  return (row?.fields || []).filter(field => field.columnCategory === category).length;
}

/** 获取当前模型直接关联的子模型数量，不递归统计更深层级。 */
function getSubModelCount(row) {
  return (row?.subModels || []).length;
}

// 取消按钮
function cancel() {
  openDetail.value = false;
  reset();
}

// 表单重置
function reset() {
  form.value = {
    id: null,
    code: null,
    name: null,
    projectId: null,
    layerId: null,
    type: null,
    versionId: null,
    description: null,
    createBy: null,
    creatorId: null,
    createTime: null,
    updateBy: null,
    updaterId: null,
    updateTime: null
  };
  proxy.resetForm("baseRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  if (props.embedded) {
    tableRef.value?.resetQuery();
    return;
  }

  modelTreeRef.value?.resetTree();
  handleModelTreeClick({treeKey: null});
}

/** 新增按钮操作 */
function handleAdd() {
  // 新增采用独立向导页面，不再打开列表页弹窗。
  proxy.$router.push({path: "/model/base/add"});
}

/** 修改按钮操作 */
function handleUpdate(row) {
  // 携带模型 ID 进入同一向导，由向导根据 ID 加载修改数据。
  proxy.$router.push({
    path: "/model/base/edit",
    query: {id: row?.id || ids.value},
  });
}

/** 发起模型修改；修改中的模型直接进入继续修改页面。 */
function handleRevision(row) {
  if (row?.version?.status === 0 || row?.version?.status === 2) {
    handleUpdate(row);
    return;
  }
  handleUpdate(row);
}

/** 记录完整选中行，批量按钮需要检查每个模型的版本状态。 */
function handleSelectionChange(rows) {
  selectedRows.value = rows;
  ids.value = rows.map(row => row.id);
}

/** 整批模型共用一个连接；包含已发布模型时，公共连接必须与其首次绑定一致。 */
async function openPublishDialog(rows, batch = false) {
  if (actionBusy.value || !rows.length || !rows.every(canPublish)) return;
  publishDialog.items = rows.map(row => ({row}));
  publishDialog.batch = batch;
  publishDialog.form = createPublishForm();
  const items = publishDialog.items;
  const lockedRows = rows.filter(row => row.publishedInstanceId);
  publishDialog.datasourceLocked = lockedRows.length > 0;
  publishDialog.loading = publishDialog.datasourceLocked;
  publishDialog.visible = true;
  nextTick(() => publishFormRef.value?.clearValidate());

  try {
    if (!lockedRows.length) return;
    const materializations = await Promise.all(lockedRows.map(async row => {
      const {data} = await getPublishedModelMaterialization(row.id);
      if (!data?.datasourceId) throw new Error(`${row.name || row.code}：未读取到已发布模型的数据连接`);
      return data;
    }));
    if (publishDialog.items !== items || !publishDialog.visible) return;
    const datasourceIds = new Set(materializations.map(item => String(item.datasourceId)));
    // 已绑定不同连接的模型无法共用同一个连接，按连接分组后才能保持原发布规则。
    if (datasourceIds.size > 1) throw new Error("所选模型已绑定不同的数据连接，请按数据连接分组发布");
    const datasourceId = materializations[0].datasourceId;
    const {data: datasource} = await getDaDatasource(datasourceId);
    if (publishDialog.items !== items || !publishDialog.visible) return;
    if (!datasource) throw new Error("原发布数据连接不存在或无法读取");
    Object.assign(publishDialog.form, {
      datasourceId,
      datasourceName: datasource.datasourceName || datasource.name || "",
      datasourceType: datasource.datasourceType || "",
    });
  } catch (error) {
    if (publishDialog.items !== items || !publishDialog.visible) return;
    publishDialog.visible = false;
    proxy.$modal.msgError(error.message || "读取原发布数据连接失败");
  } finally {
    if (publishDialog.items === items) publishDialog.loading = false;
  }
}

function handlePublish(row) {
  return openPublishDialog([row]);
}

function handleBatchPublish() {
  return openPublishDialog([...selectedRows.value], true);
}

/** 发布弹窗只维护一份连接信息，整批首次发布模型共用所选连接。 */
function handleDatasourceChange(value, datasource) {
  publishDialog.form.datasourceId = value;
  publishDialog.form.datasourceType = datasource?.datasourceType || "";
  publishDialog.form.datasourceName = datasource?.datasourceName || "";
}

/** 关闭后清理公共连接、批量标记及模型列表，防止下次操作带入旧值。 */
function resetPublishDialog() {
  publishDialog.items = [];
  publishDialog.form = createPublishForm();
  publishDialog.batch = false;
  publishDialog.datasourceLocked = false;
  publishDialog.submitting = false;
  publishDialog.loading = false;
  publishFormRef.value?.clearValidate();
}

/** 后端统一返回逐项结果，前端只对应失败模型，用于提示和重试，不再逐条调用接口。 */
function getBatchFailures(data, items) {
  const results = new Map((data?.results || []).map(result => [String(result.modelId), result]));
  return items.flatMap(item => {
    const result = results.get(String((item.row || item).id));
    return result?.success ? [] : [{item, message: result?.message || "未返回该模型的执行结果"}];
  });
}

/** 展示后端汇总的成功数和失败模型，不把部分成功当成整批成功。 */
function showActionResult(label, total, failed) {
  if (!failed.length) {
    proxy.$modal.msgSuccess(total === 1 ? `${label}成功` : `${label}完成，共${total}个模型`);
    return;
  }
  const details = failed.map(({item, message}) => {
    const row = item.row || item;
    return `${row.name || row.code || row.id}：${message}`;
  }).join("；");
  proxy.$modal.alertWarning(`${label}成功${total - failed.length}个，失败${failed.length}个。${details}`);
}

/** 批量发布只请求一次，失败时保留后端返回的失败模型，重试不重复提交成功项。 */
async function submitPublish() {
  if (!publishDialog.visible || publishDialog.loading || publishDialog.submitting) return;
  // 表单校验也会异步等待，先锁定提交，快速连点时不会同时发起两批发布。
  publishDialog.submitting = true;
  try {
    const valid = await publishFormRef.value?.validate().catch(() => false);
    if (!valid || !publishDialog.items.length) return;
    const items = [...publishDialog.items];
    const publishData = {...publishDialog.form};
    let failed = [];
    if (publishDialog.batch) {
      const {data} = await batchPublishBase({...publishData, ids: items.map(item => item.row.id)});
      failed = getBatchFailures(data, items);
    } else {
      await publishBase(items[0].row.id, publishData);
    }
    showActionResult("发布", items.length, failed);
    if (failed.length) {
      publishDialog.items = failed.map(result => result.item);
      publishDialog.datasourceLocked = publishDialog.items.some(item => item.row.publishedInstanceId);
      nextTick(() => publishFormRef.value?.clearValidate());
    } else {
      publishDialog.visible = false;
    }
    clearModelSelection();
    getList();
  } finally {
    publishDialog.submitting = false;
  }
}

/** 操作结束清理勾选，避免刷新后的旧行继续参与下一次批量操作。 */
function clearModelSelection() {
  selectedRows.value = [];
  ids.value = [];
  tableRef.value?.elTableRef?.clearSelection();
}

/** 单条继续用原接口，批量只调用一次新增接口，状态判断保持一致。 */
async function confirmModelActions(rows, action, batch = false) {
  const deleting = action === "delete";
  const allowed = deleting ? canDelete : canDisable;
  if (actionBusy.value || !rows.length || !rows.every(allowed)) return;
  const label = deleting ? "删除" : "停用";
  batchAction.value = action;
  try {
    // 在确认前固定本次选中项，等待确认期间重新勾选不会改变操作范围。
    try {
      await proxy.$modal.confirm(rows.length === 1
          ? (deleting ? `是否确认删除主数据模型编号为"${rows[0].id}"的数据项？` : "确认停用该模型吗？")
          : `确认${label}选中的${rows.length}个模型吗？`);
    } catch {
      return;
    }
    let failed = [];
    if (batch) {
      const ids = rows.map(row => row.id);
      const {data} = await (deleting ? batchDeleteBase(ids) : batchDisableBase(ids));
      failed = getBatchFailures(data, rows);
    } else {
      await (deleting ? delBase(rows[0].id) : disableBase(rows[0].id));
    }
    showActionResult(label, rows.length, failed);
    clearModelSelection();
    getList();
  } finally {
    batchAction.value = "";
  }
}

function handleDelete(row) {
  return confirmModelActions([row], "delete");
}

function handleBatchDelete() {
  return confirmModelActions([...selectedRows.value], "delete", true);
}

function handleDisable(row) {
  return confirmModelActions([row], "disable");
}

function handleBatchDisable() {
  return confirmModelActions([...selectedRows.value], "disable", true);
}

/** 详情按钮操作，可按需定位到指定 Tab。 */
function handleDetail(row, tab) {
  proxy.$router.push({
    path: "/model/base/detail",
    query: {
      id: row?.id || ids.value,
      ...(tab ? {tab} : {}),
    },
  });
}

</script>

<style scoped lang="scss">
.text-muted {
  color: color-mix(in srgb, var(--qt-text-secondary) 60%, var(--qt-surface-background));
}

/* 结构概览标签保持紧凑排列，避免四项统计在列表单元格中换行过度。 */
.structural-overview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

:global(.model-publish-dialog .el-dialog__body) {
  padding: var(--qt-space-6) 48px 28px;
}

.publish-intro {
  --tip-title-weight: normal;
  --tip-title-color: var(--el-text-color-regular);
  margin-bottom: 14px;
}

.model-base-page .main-content {
  min-width: 0;
}

.model-base-embedded .main-content {
  padding: 0;
}

// 只约束本页标题：给它收缩空间，超长文字出省略号，旁边的标签保留完整宽度。
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
