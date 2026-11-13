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
  <div ref="app-container" class="app-container">
    <guide-tip tip-id="model/project.list" />
    <qt-wrap :columns="tableStore.columns" :config="tableStore.config" :table-ref="tableRef">
      <template #search>
        <qt-search-bar
          v-bind="searchStore"
          :params="tableStore.params"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>

      <template #actions-data>
        <el-button
          v-hasPermi="['model:project:add']"
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          @mousedown="(e) => e.preventDefault()"
        >
          新增
        </el-button>
        <el-button
          v-hasPermi="['model:project:remove']"
          type="danger"
          plain
          icon="Delete"
          :disabled="!ids.length"
          @click="handleBatchDelete"
        >批量删除
        </el-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <template #action="{ row }">
          <el-button
            v-hasPermi="['model:project:query']"
            link
            type="primary"
            icon="view"
            @click="handleDetail(row)"
          >详情
          </el-button>
          <el-button
            v-hasPermi="['model:project:edit']"
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(row)"
          >修改
          </el-button>
          <el-popover
            v-if="checkPermi(['model:layer:add', 'model:project:remove'])"
            placement="bottom"
            :width="110"
            trigger="click"
            popper-class="table-action-popover handle-popover"
          >
            <template #reference>
              <el-button link type="primary" icon="ArrowDown">更多</el-button>
            </template>
            <div class="butgdlist task-action-list">
              <el-button
                v-hasPermi="['model:layer:add']"
                link
                type="primary"
                icon="Plus"
                @click="handleAddDirectory(row)"
              >新增目录</el-button>
              <el-button
                v-hasPermi="['model:project:remove']"
                link
                type="danger"
                icon="Delete"
                :disabled="row.validFlag === true"
                @click="handleDelete(row)"
              >删除</el-button>
            </div>
          </el-popover>
        </template>

        <template #resourceOverview="{ row }">
          <!-- 两个标签分别跳到对应页签，沿用标签组原样式，不给整个单元格绑定同一个跳转。 -->
          <el-space :size="6" wrap>
            <qt-tag-group
              class="pointer"
              type="none"
              :max-count="1"
              :items="['模型目录 ' + (row.layerCount ?? 0)]"
              @click="handleDetail(row, '1')"
            />
            <qt-tag-group
              class="pointer"
              type="none"
              :max-count="1"
              :items="['关联模型 ' + (row.modelCount ?? 0)]"
              @click="handleDetail(row, '2')"
            />
          </el-space>
        </template>

        <template #ruleOverview="{ row }">
          <!-- 三类数量由后端按域下全部模型汇总，前端只负责展示，不逐行再查规则。 -->
          <qt-tag-group
            type="none"
            :max-count="3"
            :items="[
              '匹配规则 '+(row.matchRuleCount ?? 0),
              '生存规则 '+(row.survivorRuleCount ?? 0),
              '质量规则 '+(row.qualityRuleCount ?? 0)
            ]"
          />
        </template>

        <template #responsiblePerson="{ row }">
          <div class="flex-column overflow-hidden">
            <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)">{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
            <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text">
              {{ getResponsiblePhone(row.responsibleUserId) }}
            </span>
          </div>
        </template>

        <template #creator="{ row }">
          <div class="flex-column overflow-hidden">
            <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)">{{ row.createBy || getResponsibleName(row.creatorId) || '-' }}</span>
            <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text">
              {{ getResponsiblePhone(row.creatorId) }}
            </span>
          </div>
        </template>

        <template #validFlag="{ row }">
          <el-switch
            v-model="row.validFlag"
            :disabled="!checkPermi(['model:project:edit'])"
            :active-value="true"
            :inactive-value="false"
            :loading="statusChanging.has(row.id)"
            @change="handleStatusChange(row)"
          >
          </el-switch>
        </template>
      </qt-table>
    </qt-wrap>
    <project-form-dialog ref="projectDialogRef" @success="refreshList" />
    <layer-form-dialog ref="layerDialogRef" :project="directoryProject" @success="refreshList" />
  </div>
</template>

<script setup name="Project">
/** @description 主数据域列表，负责查询、维护及状态管理。 */
import { checkPermi } from "@/utils/permission";
import { useUserDirectory } from '@/store/system/userDirectory';
import { listProject, delProject, updateProjectStatus } from "@/api/model/project/project";
import { getProjectIcon } from "@/utils/imageUrl";
import ProjectFormDialog from "./components/projectFormDialog.vue";
import LayerFormDialog from "@/views/model/layer/components/layerFormDialog.vue";
import { listLayer } from "@/api/model/layer/layer.js";

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const { proxy } = getCurrentInstance();
const statusChanging = reactive(new Set());

const ids = ref([]);
const projectDialogRef = ref();
const layerDialogRef = ref();
const directoryProject = ref({});
let directoryRequestVersion = 0;

const data = reactive({
  queryParams: {
    code: null,
    name: null,
    description: null,
    status: null,
    sortOrder: null,
    createTime: null,
  }
});

const { queryParams } = toRefs(data);

const tableRef = ref(null);
const tableStore = reactive({
  config: {
    sort: true,
    stripe: true,
    notPagination: false,
    notPaginationParams: true,
    table: {
      onSelectionChange: handleSelectionChange,
      stripe: true,
      rowKey: "id",
      defaultSort: { prop: "createTime", order: "descending" },
    },
  },
  columns: [
    { type: "selection", width: 55, align: "center" },
    { label: "编号", prop: "id", width: 60, align: "center", sortable: true },
    {
      label: '主数据域信息',
      minWidth: 350,
      align: 'left',
      info: {
        icon: (row) => getProjectIcon(row.icon),
        title: (row) =>
            row.name + (row.name ? ` (${row.code})` : ''),
        desc: 'description',
        click: (row) => handleDetail(row)
      },
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "资源概览",
      prop: "layerCount",
      slot: "resourceOverview",
      align: "left",
      minWidth: 220,
    },
    {
      label: "规则概览",
      prop: "ruleCount",
      align: "left",
      minWidth: 380,
      slot: "ruleOverview",
    },
    {
      label: "状态", prop: "validFlag", slot: "validFlag",
      minWidth: 100
    },
    {
      label: "负责人",
      prop: "responsibleUserId",
      align: "left",
      headerAlign: "left",
      minWidth: 120,
      slot: "responsiblePerson",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "创建人",
      prop: "createBy",
      align: "left",
      headerAlign: "left",
      minWidth: 120,
      slot: "creator",
      showOverflowTooltip: { effect: "light" },
    },
    {
      label: "创建时间",
      prop: "createTime",
      minWidth: 150,
      sortable: true,
      sortableKey: "create_time",
      date: true,
    },
    { label: "操作", minWidth: 220, slot: "action", fixed: "right" },
  ],
  func: listProject,
  params: queryParams
});

const searchStore = reactive({
  items: [
    {
      label: "主数据域名称",
      prop: "name",
      align: "left",
      component: {
        is: "input",
      },
    },
    {
      label: "主数据域编码",
      prop: "code",
      component: {
        is: "input",
      },
    },
  ],
});

/** 刷新主数据域列表 */
function refreshList() {
  tableRef.value?.getList();
}

/** 搜索按钮操作 */
function handleQuery() {
  tableRef.value?.resetQuery();
}

/** 重置按钮操作 */
function resetQuery() {
  tableRef.value?.resetQuery();
}

/** 新增按钮操作 */
function handleAdd() {
  projectDialogRef.value.openDialog();
}

/** 修改按钮操作 */
function handleUpdate(row) {
  projectDialogRef.value.openDialog(row.id || ids.value);
}

/** 复用模型目录弹窗，在当前主数据域下新增目录，默认挂在顶级节点。 */
async function handleAddDirectory(row) {
  const currentRequest = ++directoryRequestVersion;
  // 上级目录选项必须来自当前域的完整目录列表，不能使用主数据域表格里的统计数量。
  const response = await listLayer({ projectId: row.id });
  // 连续点击不同域时只打开最后一次的弹窗，避免把目录建到之前点过的域下。
  if (currentRequest !== directoryRequestVersion) return;
  directoryProject.value = { ...row };
  // 等子弹窗收到新的主数据域后再打开，否则可能沿用上一次的域。
  await nextTick();
  layerDialogRef.value?.openDialog({
    title: "新增模型目录",
    parentId: 0,
    layerTreeOptions: [{
      id: 0,
      name: "顶级节点",
      children: proxy.handleTree(response.data || [], "id", "parentId"),
    }],
  });
}

/** 默认查看模型目录，资源标签可指定直接打开关联模型页签。 */
function handleDetail(row, tab = '1') {
  proxy.$router.push({
    path: "/model/project/detail",
    query: { id: row.id, tab },
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value;
  if (row && row.layerCount > 0) {
    proxy.$modal.msgWarning("删除失败,主数据域被模型目录引用!");
    return;
  }
  if (row && row.modelCount > 0) {
    proxy.$modal.msgWarning("删除失败,主数据域被主数据模型引用!");
    return;
  }
  proxy.$modal.confirm('是否确认删除主数据域编号为"' + _ids + '"的数据项？').then(function () {
    return delProject(_ids);
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

/** 批量删除主数据域 */
function handleBatchDelete() {
  proxy.$modal.confirm('是否确认删除选中的主数据域？').then(() => delProject(ids.value.join(','))).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    refreshList();
    ids.value = [];
  });
}

/** 确认后更新启停状态，取消或失败时还原开关。 */
async function handleStatusChange(row) {
  if (statusChanging.has(row.id)) return;
  const enabled = row.validFlag === true;
  const previousStatus = !enabled;
  const text = enabled ? "启用" : "禁用";
  statusChanging.add(row.id);
  try {
    await proxy.$modal.confirm(`确认要${text}“${row.name}”主数据域吗？`);
    await updateProjectStatus(row.id, enabled);
  } catch {
    row.validFlag = previousStatus;
    return;
  } finally {
    statusChanging.delete(row.id);
  }
  proxy.$modal.msgSuccess(text + "成功");
  refreshList();
}
</script>
