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
  <div ref="app-container" :class="embedded ? 'layer-embedded' : 'app-container'">
    <guide-tip v-if="!embedded" tip-id="model/layer.list"/>
    <el-container>
      <!-- 左侧树 -->
      <qt-tree
          v-if="!embedded"
          ref="modelProjectTreeRef"
          :dept-options="projectOptions"
          placeholder="请输入主数据域名称"
          @node-click="handleNodeClick"
      />
      <el-main class="main-content">
        <qt-wrap :columns="tableStore?.columns"
                 :table-ref="tableRef"
                 :config="tableStore.config"
        >
          <template #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                :table-ref="tableRef"
                @reset="resetProjectSelection"
            />
          </template>
          <template #actions-data>
            <el-button
                v-hasPermi="[`model:layer:add`]"
                type="primary"
                plain
                icon="Plus"
                :disabled="!currentProject?.id"
                @click="handleAdd()"
            >新增
            </el-button>
            <el-button
                v-hasPermi="[`model:layer:remove`]"
                type="danger"
                plain
                icon="Delete"
                :disabled="!ids.length"
                @click="handleBatchDelete"
            >批量删除
            </el-button>
            <el-button
                v-if="parentId == null"
                class="toggle-expand-all"
                type="primary"
                plain
                @click="toggleExpandAll"
            >
              <svg-icon v-if="defaultExpandAll" icon-class="toggle"/>
              <svg-icon v-else icon-class="expand"/>
              <span>{{ defaultExpandAll ? "折叠" : "展开" }}</span>
            </el-button>
          </template>
          <qt-table
              v-if="!embedded || currentProject?.id"
              v-bind="tableStore"
              :context-key="`${currentProject?.id ?? 'all'}:${parentId ?? 'all'}`"
              ref="tableRef"
          >
            <template #modelCount="{ row }">
              <qt-tag-group type="none"
                            class="pointer" :max-count="1" :items="['关联模型 ' + (row.modelCount ?? 0)]"
                            @click="handleDetail(row, 'models')"
              />
            </template>
            <template #responsiblePerson="{ row }">
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
            <template #validFlag="{ row }">
              <el-switch
                  v-model="row.validFlag"
                  :disabled="!checkPermi(['model:layer:edit'])"
                  :active-value="true"
                  :inactive-value="false"
                  :loading="statusChanging.has(row.id)"
                  @change="handleStatusChange(row)"
              />
            </template>
            <template #action="{ row }">
              <el-button
                  v-hasPermi="['model:layer:query']"
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(row)"
              >详情
              </el-button>
              <el-button
                  v-hasPermi="[`model:layer:edit`]"
                  link
                  type="primary"
                  icon="Edit"
                  @click="handleUpdate(row)"
              >修改
              </el-button>
              <el-popover placement="bottom" :width="100" trigger="click"
                          popper-class="table-action-popover handle-popover"
              >
                <template #reference>
                  <el-button link type="primary" icon="ArrowDown">更多</el-button>
                </template>
                <div class="butgdlist task-action-list">
                  <el-button
                      v-hasPermi="[`model:layer:add`]"
                      link
                      type="primary"
                      icon="Plus"
                      @click="handleAdd(row)"
                  >新增子级
                  </el-button>
                  <el-button
                      v-hasPermi="[`model:layer:remove`]"
                      link
                      type="danger"
                      icon="Delete"
                      :disabled="row.validFlag === true"
                      @click="handleDelete(row)"
                  >删除
                  </el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <layer-form-dialog ref="layerDialogRef" :project="dialogProject" @success="refreshList"/>
  </div>
</template>

<script setup name="ModelLayer">
/** @description 模型目录树列表，按主数据域查询与维护目录。 */
import {reactive} from "vue";
import {useUserDirectory} from '@/store/system/userDirectory';

import {checkPermi} from "@/utils/permission";
import QtTree from "@/components/QtTree";
import {treeProject} from "@/api/model/project/project";
import {listLayer, updateLayerStatus, delLayer} from "@/api/model/layer/layer.js";
import LayerFormDialog from "./components/layerFormDialog.vue";

const {responsibleUserList, getResponsibleName, getResponsiblePhone} = useUserDirectory();

// 独立页面自己选择主数据域；嵌入详情页时由外层传入域和父目录，只展示对应子目录。
const props = defineProps({
  embedded: {type: Boolean, default: false},
  project: {type: Object, default: null},
  parentId: {type: [String, Number], default: null},
  search: {type: Boolean, default: false},
});
const emit = defineEmits(['changed']);
const {proxy} = getCurrentInstance();
const statusChanging = reactive(new Set());

const modelProjectTreeRef = ref(null);
const projectOptions = ref([]);
const tableRef = ref(null);
const layerDialogRef = ref();
const selectedProject = ref(null);
const currentProject = computed(() => props.embedded ? props.project : selectedProject.value);
const dialogProject = ref({});
const ids = ref([]);

const data = reactive({
  queryParams: {
    responsibleUserId: null,
    code: null,
    name: null,
    sortOrder: null,
    description: null,
    createTime: null,
  }
});

const {queryParams} = toRefs(data);

const tableStore = reactive({
  config: {
    stripe: true,
    fullContent: !props.embedded,
    notPagination: true,
    notPaginationParams: true,
    sort: true,
    search: props.search,
    table: {
      onSelectionChange: handleSelectionChange,
      rowKey: "id",
      cellClassName: ({column}) => column.property === "name" ? "layer-directory-info-cell" : "",
      defaultExpandAll: props.parentId != null,
      defaultSort: {prop: "sortOrder", order: "ascending"},
      treeProps: {children: "children", hasChildren: "hasChildren"},
    },
  },
  columns: [
    {type: "selection", width: 55, align: "center"},
    {
      label: "模型目录信息",
      prop: "name",
      align: "left",
      minWidth: 220,
      info: {
        title: "name",
        desc: "code",
        click: row => handleDetail(row),
      },
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "描述",
      prop: "description",
      align: "left",
      minWidth: 280,
      showOverflowTooltip: {effect: "light"},
    },
    {
      label: "资源概览",
      prop: "modelCount",
      slot: "modelCount",
      align: "center",
      minWidth: 120,
    },
    {
      label: "排序",
      prop: "sortOrder",
      sortable: true,
      sortableKey: "sortOrder",
      minWidth: 80
    },
    {
      label: "状态",
      prop: "validFlag",
      slot: "validFlag",
      align: "center",
      minWidth: 80
    },
    {
      label: "负责人", prop: "responsiblePerson", slot: "responsiblePerson",
      align: "left", minWidth: 120,
    },
    {
      label: "创建人", prop: "createBy", slot: "creator",
      align: "left", minWidth: 120,
    },
    {
      label: "创建时间",
      prop: "createTime",
      minWidth: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    {label: "操作", minWidth: 240, slot: "action", fixed: "right"},
  ],
  func: getList,
  params: queryParams,
  events: {
    formatData: (data) => proxy.handleTree(data, "id", "parentId"),
  },
});

const searchStore = computed(() => ({
  // 公共用户列表加载完成后，负责人下拉框同步更新选项。
  items: [
    {label: "模型目录名称", prop: "name", component: {is: "input"}},
    {label: "模型目录编码", prop: "code", component: {is: "input"}},
    {
      label: "负责人",
      prop: "responsibleUserId",
      component: {is: "select", filterable: true, options: responsibleUserList.value}
    },
  ],
  config: {permi: [`model:layer:query`]},
}));

const defaultExpandAll = computed({
  get() {
    return tableStore.config.table.defaultExpandAll;
  },
  set(val) {
    tableStore.config.table.defaultExpandAll = val;
  },
});

/** 切换模型目录树的展开与折叠状态。 */
function toggleExpandAll() {
  defaultExpandAll.value = !defaultExpandAll.value;
  tableRef.value?.reload();
}

/** 查询模型目录层级列表 */
function getList(params) {
  const projectId = currentProject.value?.id;
  if (props.embedded && !projectId) return Promise.resolve({data: []});
  return listLayer({...params, projectId, parentId: props.parentId});
}

/** 刷新当前模型目录列表。 */
function refreshList() {
  tableRef.value?.getList();
  // 目录新增、修改后通知详情页更新上方统计，避免列表变了而数量没变。
  emit('changed');
}

/** 获取主数据域 */
async function getProjectList() {
  const response = await treeProject();
  projectOptions.value = (response.data || []).map(project => ({ ...project, nodeType: 'project' }));
  await nextTick();
  // 组件默认展开第一层，此页面初始化时收起，后续仍可正常点击展开。
  projectOptions.value.forEach((project) => {
    modelProjectTreeRef.value?.deptTreeRef?.getNode(project.id)?.collapse();
  });
}

/** 按操作目标所属的域加载完整目录树，供弹窗选择上级。 */
async function openLayerDialog(options, row) {
  const projectId = row?.projectId ?? currentProject.value?.id;
  if (!projectId) return;

  const project = String(currentProject.value?.id) === String(projectId)
    ? currentProject.value
    : projectOptions.value.find(item => String(item.id) === String(projectId))
      || {id: projectId, name: String(projectId)};
  const source = await listLayer({projectId});
  const children = proxy.handleTree(source.data || [], "id", "parentId");
  dialogProject.value = project;
  layerDialogRef.value.openDialog({
    ...options,
    layerTreeOptions: [{id: 0, name: "顶级节点", children}],
  });
}

/** 优先新增到点击的目录下；详情页新增挂到当前目录，独立页面新增挂到顶级。 */
function handleAdd(row) {
  return openLayerDialog({
    title: "新增模型目录",
    parentId: row?.id ?? props.parentId ?? 0,
  }, row);
}

/** 修改按钮操作 */
function handleUpdate(row) {
  return openLayerDialog({
    title: "修改模型目录",
    ...row,
  }, row);
}

/** 打开目录详情，默认展示子目录页签。 */
function handleDetail(row, tab = 'children') {
  proxy.$router.push({path: "/model/layer/detail", query: {id: row.id, tab}});
}

/** 删除按钮操作 */
function handleDelete(row) {
  if (row.modelCount > 0) {
    proxy.$modal.msgWarning("删除失败,模型目录被主数据模型引用!");
    return;
  }
  proxy.$modal.confirm('是否确认删除模型目录层级编号为"' + row.id + '"的数据项？').then(function () {
    return delLayer(row.id);
  }).then(() => {
    refreshList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

/** 记录列表选中项 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
}

/** 批量删除模型目录 */
function handleBatchDelete() {
  proxy.$modal.confirm('是否确认删除选中的模型目录？').then(() => delLayer(ids.value.join(','))).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    ids.value = [];
    refreshList();
  });
}

/** 清除主数据域筛选，随后由搜索栏刷新表格。 */
function resetProjectSelection() {
  if (props.embedded) return;

  if (selectedProject.value) ids.value = [];
  selectedProject.value = null;
  modelProjectTreeRef.value?.resetTree();
}

/** 节点单击事件 */
function handleNodeClick(data) {
  if (data.id != 0) {
    selectedProject.value = data;
    ids.value = [];
    tableRef.value?.resetQuery();
  }
}

/** 确认后更新启停状态，取消或失败时还原开关。 */
async function handleStatusChange(row) {
  if (statusChanging.has(row.id)) return;
  const enabled = row.validFlag === true;
  const previousStatus = !enabled;
  const text = enabled ? "启用" : "禁用";
  statusChanging.add(row.id);
  try {
    await proxy.$modal.confirm(`确认要${text}“${row.name}”模型目录吗？`);
    await updateLayerStatus(row.id, enabled);
  } catch {
    row.validFlag = previousStatus;
    return;
  } finally {
    statusChanging.delete(row.id);
  }
  proxy.$modal.msgSuccess(text + "成功");
  refreshList();
}

// 只有独立页面首次打开需要查询左侧域树，嵌入列表直接使用外层传入的数据。
onMounted(() => {
  if (!props.embedded) getProjectList();
});
// 首次加载由表格发起，从详情返回缓存列表时再刷新。
let hasActivated = false;
onActivated(() => {
  if (props.embedded) return;
  if (hasActivated) refreshList();
  hasActivated = true;
});
</script>

<style scoped lang="scss">
.action-menu {
  width: var(--qt-action-menu-width);
}

// 展开箭头与目录标题放在同一行，目录编码仍在第二行，避免箭头单独占一行。
:deep(.el-table__body td.layer-directory-info-cell > .cell) {
  display: flex !important;
  align-items: baseline;
}
</style>
