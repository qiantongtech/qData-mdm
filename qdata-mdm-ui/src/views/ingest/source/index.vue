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
  来源系统列表：查询来源信息，维护系统配置及其数据连接关联。
-->
<template>
  <div class="app-container ingest-source" ref="app-container">
    <guide-tip tip-id="ingest/source.list"/>
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
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
            @click="handleAdd"
            v-hasPermi="['ingest:source:add']"
            @mousedown="(e) => e.preventDefault()"
        >
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
        <el-button
            type="danger"
            plain
            icon="Delete"
            :disabled="!store.rows.length"
            @click="handleDelete"
            v-hasPermi="['ingest:source:remove']"
        >
          批量删除
        </el-button>
      </template>
      <qt-table v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" ref="tableRef">
        <template #trustScore="{ row }">
          <span class="trust-score" :class="{ 'trust-score--high': (row.trustScore ?? 50) >= 80 }">
            {{ row.trustScore ?? 50 }}
          </span>
        </template>
        <template #responsiblePerson="{ row }">
          <div class="flex-column overflow-hidden">
            <span class="text-ellipsis" :title="row.responsiblePersonName || ''">{{
                row.responsiblePersonName || '-'
              }}</span>
            <span v-if="row.responsiblePersonPhone" class="desc-text">
              {{ row.responsiblePersonPhone }}
            </span>
          </div>
        </template>
        <template #createBy="{ row }">
          <div class="flex-column overflow-hidden">
            <span class="text-ellipsis" :title="row.createBy || ''">{{ row.createBy || '-' }}</span>
            <span v-if="row.creatorPhone" class="desc-text">
              {{ row.creatorPhone }}
            </span>
          </div>
        </template>
        <template #datasourceRel="{ row }">
          <source-relation-tags
              :items="(row.datasourceList || []).map(item => ({ ...item, label: item.datasourceName }))"
              is-datasource
          />
        </template>
        <template #modelNames="{ row }">
          <source-relation-tags
              :items="(row.modelList || []).map(item => ({ value: item.modelId, label: item.modelName, icon: getModelIcon(item), elTagType: 'primary' }))"
          />
        </template>
        <template #taskCount="{ row }">
          <div class="source-tags">
            <el-button v-hasPermi="['merge:detect:list']" link @click="openRelatedTasks(row, '/merge/detect')">
              <el-tag>重复检测 {{ row.duplicateTaskCount ?? 0 }}</el-tag>
            </el-button>
          </div>
        </template>
        <template #validFlag="{ row }">
          <el-switch
              :model-value="String(row.validFlag ?? '1')"
              :loading="statusLoading[row.id]"
              :disabled="!checkPermi(['ingest:source:edit'])"
              active-value="1"
              inactive-value="0"
              aria-label="来源系统状态"
              @change="value => handleStatusChange(row, value)"
          />
        </template>
        <template #handle="{ row }">
          <el-button
              link
              type="primary"
              icon="View"
              @click="handleDetail(row)"
              v-hasPermi="['ingest:source:query']"
          >
            详情
          </el-button>
          <el-button
              link
              type="primary"
              icon="Edit"
              @click="handleUpdate(row)"
              v-hasPermi="['ingest:source:edit']"
          >
            修改
          </el-button>
          <el-popover
              v-if="checkPermi(['ingest:task:add', 'ingest:source:remove'])"
              placement="bottom"
              :width="160"
              trigger="click"
          >
            <template #reference>
              <el-button link type="primary" icon="ArrowDown">
                更多
              </el-button>
            </template>
            <div class="butgdlist">
              <el-button
                  v-hasPermi="['ingest:task:add']"
                  link
                  type="primary"
                  icon="Plus"
                  @click="addTask(row)"
              >
                新增接入任务
              </el-button>
              <el-button
                  v-hasPermi="['ingest:source:remove']"
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </el-popover>
        </template>
      </qt-table>
    </qt-wrap>
    <source-form-dialog ref="formDialogRef" @success="handleFormSuccess"/>
  </div>
</template>

<script setup name="IngestSource">
import {reactive, ref, getCurrentInstance} from "vue";

import {useRouter} from "vue-router";

import {
  listIngestSource,
  getIngestSource,
  updateIngestSource,
  delIngestSource,
} from "@/api/ingest/source/source.js";

import {getModelIcon} from "@/utils/imageUrl";
import {checkPermi} from "@/utils/permission";
import {usePageRefresh} from "@/composables/usePageRefresh";
import SourceFormDialog from "@/views/ingest/source/sourceFormDialog.vue";
import SourceRelationTags from "@/views/ingest/source/sourceRelationTags.vue";

import { getProjectIcon } from '@/utils/imageUrl';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const {proxy} = getCurrentInstance();
const router = useRouter();

const store = reactive({
  rows: [],
});
const tableRef = ref(null);
const formDialogRef = ref(null);
const statusLoading = reactive({});
usePageRefresh('ingestSource', () => tableRef.value?.getList());

const tableStore = reactive({
  config: {
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      defaultSort: {prop: "create_time", order: "descending"},
      onSelectionChange: function (rows) {
        store.rows = rows;
      },
    },
  },
  columns: [
    {type: 'selection', width: 55, minWidth: 55},
    {label: '编号', prop: 'id', width: 90, minWidth: 90, align: 'center', sortable: true},
    {
      label: '来源系统信息',
      prop: 'name',
      width: 350,
      minWidth: 350,
      align: 'left',
      info: {
        icon: row => getSourceIcon(row),
        title: row => row.name + (row.name ? ` (${row.code})` : ''),
        desc: 'description',
        click: row => handleDetail(row),
      },
      showOverflowTooltip: {effect: 'light'},
    },
    {
      label: '关联数据连接',
      prop: 'datasourceRel',
      slot: 'datasourceRel',
      minWidth: 250,
      headerAlign: 'left',
      align: 'left'
    },
    {label: '关联主数据模型', prop: 'modelNames', slot: 'modelNames', minWidth: 300, align: 'left'},
    {label: '关联任务', prop: 'taskCount', slot: 'taskCount', minWidth: 210, headerAlign: 'left'},
    {
      label: '来源可信度',
      prop: 'trustScore',
      slot: 'trustScore',
      width: 120,
      minWidth: 120,
      sortable: true,
      sortableKey: 'trust_score'
    },
    {label: '状态', prop: 'validFlag', slot: 'validFlag', width: 90, minWidth: 90},
    {label: '负责人', prop: 'responsiblePerson', slot: 'responsiblePerson', minWidth: 150, align: 'left'},
    {label: '创建人', prop: 'createBy', slot: 'createBy', minWidth: 150, align: 'left'},
    {
      label: '修改时间',
      prop: 'updateTime',
      width: 170,
      minWidth: 170,
      date: true,
      sortable: true,
      sortableKey: 'update_time'
    },
    {
      label: '创建时间',
      prop: 'createTime',
      width: 170,
      minWidth: 170,
      date: true,
      sortable: true,
      sortableKey: 'create_time'
    },
    {label: '操作', width: 220, minWidth: 220, fixed: 'right', slot: 'handle'},
  ],
  func: listIngestSource,
  params: {
    name: undefined,
    code: undefined,
    responsiblePerson: undefined,
    datasourceName: undefined,
    refFilter: undefined
  },
  events: {},
});

const searchStore = reactive({
  items: [
    {
      label: "来源系统名称",
      prop: "name",
      component: {
        is: "input",
        placeholder: "请输入来源系统名称",
      },
    },
    {
      label: "来源系统编码",
      prop: "code",
      component: {
        is: "input",
        placeholder: "请输入来源系统编码",
      },
    },
    {
      label: "负责人",
      prop: "responsiblePerson",
      component: {
        is: "input",
        placeholder: "请输入负责人",
      },
    },
    {
      label: "数据连接",
      prop: "datasourceName",
      component: {
        is: "input",
        placeholder: "请输入数据库连接名称",
      },
    },
    {
      label: "业务引用",
      prop: "refFilter",
      component: {
        is: "select",
        placeholder: "请选择业务引用",
        options: [
          {value: "used", label: "存在引用"},
          {value: "free", label: "无引用"},
        ],
      },
    },
  ],
});

/**
 * 打开所选来源系统详情。
 * @param {Object} row 来源系统记录。
 */
function addTask(row) {
  router.push({path: '/ingest/source/task/add', query: {sourceSystemId: row.id}});
}

function openRelatedTasks(row, path) {
  router.push({path, query: {sourceSystemId: row.id, sourceName: row.name}});
}

function handleDetail(row) {
  router.push({
    path: "/ingest/source/detail",
    query: {id: row.id},
  });
}

/** 调用来源系统弹窗，初始化新增表单。 */
function handleAdd() {
  formDialogRef.value?.openAdd();
}

/**
 * 按来源系统 ID 打开修改弹窗。
 * @param {Object} row 来源系统记录。
 */
function handleUpdate(row) {
  formDialogRef.value?.openEdit(row.id);
}

/** 来源系统保存成功后刷新当前列表。 */
function handleFormSuccess() {
  tableRef.value?.getList();
}

/** 确认后复用修改接口保存状态，保留来源系统配置及数据连接关联。 */
async function handleStatusChange(row, value) {
  if (statusLoading[row.id] || !checkPermi(['ingest:source:edit'])) {
    return;
  }
  statusLoading[row.id] = true;
  const text = value === '1' ? '启用' : '停用';
  try {
    await proxy.$modal.confirm(`确认要${text}来源系统“${row.name}”吗？`);
    const {data} = await getIngestSource(row.id);
    await updateIngestSource({
      id: data.id,
      name: data.name,
      icon: data.icon || '',
      validFlag: value,
      responsibleUserId: data.responsibleUserId || null,
      code: data.code,
      trustScore: data.trustScore,
      description: data.description,
      datasourceIds: data.datasourceIds || [],
    });
    proxy.$modal.msgSuccess(`${text}成功`);
    tableRef.value?.getList();
  } catch {
    // 取消或保存失败时保留原状态，接口错误由请求层统一提示。
  } finally {
    statusLoading[row.id] = false;
  }
}

/**
 * 排除仍被任务引用的来源系统，确认后删除可删部分。
 * @param {Object} [row] 单条记录，未指定时使用表格多选记录。
 */
function handleDelete(row) {
  const selected = row?.id ? [row] : store.rows;
  if (!selected.length) {
    return;
  }
  const blocked = selected.filter((item) => (item.taskCount || 0) > 0);
  const allowed = selected.filter((item) => (item.taskCount || 0) <= 0);
  if (!allowed.length) {
    const message =
        blocked.length === 1
            ? `该来源系统当前存在 ${blocked[0].taskCount} 个接入任务引用，请先解除业务引用后再删除`
            : "所选来源系统均存在接入任务引用，无法删除";
    proxy.$modal.msgError(message);
    return;
  }
  const message = blocked.length
      ? `可删除${allowed.length}个，不可删除${blocked.length}个（存在接入任务引用），是否删除可删部分`
      : row?.id
          ? `是否确认删除来源系统“${row.name}”？`
          : `是否确认删除选中的${allowed.length}条数据？`;
  proxy.$modal
      .confirm(message)
      .then(() => delIngestSource(allowed.map((item) => item.id)))
      .then(() => {
        tableRef.value.getList();
        proxy.$modal.msgSuccess("删除成功");
      })
      .catch(() => {
      });
}
</script>

<style scoped lang="scss">
.source-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--qt-space-2);
}

.source-tags :deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  gap: var(--qt-space-1);
}

.source-tags .el-button {
  margin-left: 0;
}
</style>
