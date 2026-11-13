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
  <div ref="app-container" v-loading="loading" class="app-container">
    <!-- 复用系统统一详情头部：展示模型编号、名称、版本状态、返回按钮及基础属性。 -->
    <detail-info
      :data="detail"
      :header="{
        className: 'clearfixs',
        idKey: 'id',
        nameKey: 'name',
        statusKey: 'version.status',
        statusOptions: model_status,
      }"
      :items="detailItems"
      mode="free"
    >
      <!-- 模型类型读取系统字典，与模型列表保持一致。 -->
      <template #modelType="{ data }">
        <dict-tag :options="model_type" :value="data.type" />
      </template>
      <template #btn-right>
        <el-button
          v-if="Number(detail.version?.status) === 1"
          v-hasPermi="['model:base:edit']"
          type="danger"
          plain
          icon="Remove"
          @click="handleDisable"
        >
          停用模型
        </el-button>
        <el-button
            v-if="[0, 1, 2].includes(Number(detail.version?.status))"
            v-hasPermi="['model:base:edit']"
            type="primary"
            plain
            icon="Edit"
            @click="handleRevision"
        >
          修改
        </el-button>
      </template>
    </detail-info>

    <!-- 详情主体保留八类业务信息，并使用统一的底部内容容器。 -->
    <div class="pagecont-bottom">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <qt-tab-pane label="模型关系图谱" name="graph">
          <model-relation-graph v-if="modelId" :model-id="modelId" height="calc(100vh - 423px)"/>
        </qt-tab-pane>
        <qt-tab-pane :label="`普通字段`" name="business">
          <model-field-table :fields="businessFields" category="BUSINESS" :query-rows="loadFieldRows" />
        </qt-tab-pane>
        <qt-tab-pane :label="`关联字段`" name="relation">
          <model-field-table :fields="relationFields" category="RELATION" :query-rows="loadFieldRows" />
        </qt-tab-pane>
        <qt-tab-pane :label="`分类字段`" name="class">
          <model-field-table :fields="classFields" category="CLASS" :query-rows="loadFieldRows" />
        </qt-tab-pane>
        <qt-tab-pane :label="`子模型`" name="subModels">
          <sub-model-list :sub-models="subModels" :query-rows="loadSubModelRows" />
        </qt-tab-pane>
        <qt-tab-pane :label="`系统字段`" name="system">
          <model-field-table :fields="systemFields" category="SYSTEM" :query-rows="loadFieldRows" />
        </qt-tab-pane>
        <!-- 模型详情匹配规则：复用规则管理页面，固定当前模型。 -->
        <qt-tab-pane label="匹配规则" name="matchRules" lazy>
          <rule-match
            v-if="activeTab === 'matchRules' && modelId && String(detail.id) === String(modelId)"
            :key="String(modelId)"
            embedded
            :model-id="modelId"
            :model-detail="detail"
          />
        </qt-tab-pane>
        <!-- 模型详情生存规则：复用规则管理页面，固定当前模型。 -->
        <qt-tab-pane label="生存规则" name="survivorRules" lazy>
          <rule-survivor
            v-if="activeTab === 'survivorRules' && modelId && String(detail.id) === String(modelId)"
            :key="String(modelId)"
            embedded
            :model-id="modelId"
            :model-detail="detail"
          />
        </qt-tab-pane>
        <!-- 模型详情质量规则：复用规则管理页面，固定当前模型。 -->
        <qt-tab-pane label="质量规则" name="qualityRules" lazy>
          <rule-quality
            v-if="activeTab === 'qualityRules' && modelId && String(detail.id) === String(modelId)"
            :key="String(modelId)"
            embedded
            :model-id="modelId"
            :model-detail="detail"
          />
        </qt-tab-pane>
        <qt-tab-pane :label="`版本管理`" name="versions">
          <detail-list-table
            :rows="versionRows"
            mode="version"
            :current-version-id="currentReleaseVersionId"
            :query-rows="loadVersions"
          />
        </qt-tab-pane>
        <qt-tab-pane label="发布历史" name="history">
          <detail-list-table :rows="releaseHistory" mode="history" :query-rows="loadPublishHistory" />
        </qt-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="ModelBaseDetail">

import {useDict} from "@/utils/dict";

/** @description 主数据模型详情，展示字段、子模型及版本信息。 */
import { ElMessage } from "element-plus";
import { useUserDirectory } from '@/store/system/userDirectory';
import { disableBase, getBase } from "@/api/model/base/base";
import { listModelPublishHistory } from "@/api/model/base/instance";
import { listModelVersions } from "@/api/model/base/version";
import ModelRelationGraph from "@/views/model/components/ModelRelationGraph/index.vue";
import ModelFieldTable from "./components/modelFieldTable.vue";
import SubModelList from "./components/subModelList.vue";
import DetailListTable from "./components/detailListTable.vue";
import RuleMatch from "@/views/rule/match/index.vue";
import RuleSurvivor from "@/views/rule/survivor/index.vue";
import RuleQuality from "@/views/rule/quality/index.vue";

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const {model_type, model_status} = useDict('model_type', 'model_status');

const route = useRoute();
const router = useRouter();
const { proxy } = getCurrentInstance();
const loading = ref(false);
const detailTabs = new Set(["graph", "business", "relation", "class", "subModels", "system", "versions", "history", "matchRules", "survivorRules", "qualityRules"]);
const getRouteTab = () => detailTabs.has(route.query.tab) ? route.query.tab : "graph";
const activeTab = ref(getRouteTab());
const detail = ref({ fields: [], subModels: [] });
const versionRows = ref([]);
const releaseHistory = ref([]);
const modelId = computed(() => route.query.id);

// 按字段类别拆分详情接口返回的字段，分别提供给对应 Tab 展示。
const fields = computed(() => detail.value.fields || []);
const businessFields = computed(() => fields.value.filter(item => item.columnCategory === "BUSINESS"));
const relationFields = computed(() => fields.value.filter(item => item.columnCategory === "RELATION"));
const classFields = computed(() => fields.value.filter(item => item.columnCategory === "CLASS"));
const systemFields = computed(() => fields.value.filter(item => item.columnCategory === "SYSTEM"));
const subModels = computed(() => detail.value.subModels || []);
// DetailInfo 的自由布局配置：默认三列，描述独占一行。
const detailItems = computed(() => [
  { label: "模型编码", key: "code" },
  { label: "模型类型", key: "type", slot: "modelType" },
  {
    label: "当前版本",
    key: "version",
    formatter: value => value?.versionNo || "尚未发布",
  },
  {
    label: "描述",
    key: "description",
    span: 24,
    ellipsisClass: "ellipsis-2",
    className: "mt2 mb2",
  },
  {
    label: "创建人",
    key: "createBy",
    formatter: (_, row) =>
            [
              row.createBy || getResponsibleName(row.creatorId) || "-",
              getResponsiblePhone(row.creatorId),
            ].filter(Boolean).join(" / "),
  },
  { label: "创建时间", key: "createTime", type: "time" },
  { label: "更新时间", key: "updateTime", type: "time" },
]);
const currentReleaseVersionId = computed(() => (
  detail.value.releaseVersion?.id
  || (Number(detail.value.version?.status) === 1 ? detail.value.version?.id : null)
));

let detailRequest = null;
let versionRequest = null;
let historyRequest = null;

/** 查询并更新模型详情；多个字段页签并发初始化时复用同一个请求。 */
async function queryDetail() {
  if (!detailRequest) {
    detailRequest = getBase(modelId.value).then(response => {
      detail.value = response.data || { fields: [], subModels: [] };
      return detail.value;
    }).finally(() => {
      detailRequest = null;
    });
  }
  return detailRequest;
}

/** 重新查询详情，并返回当前字段分类的数据供 qt-table 刷新。 */
async function loadFieldRows(category) {
  const latestDetail = await queryDetail();
  const rows = (latestDetail.fields || []).filter(item => item.columnCategory === category);
  return { data: { rows, total: rows.length } };
}

/** 重新查询详情，并返回最新子模型列表供 qt-table 刷新。 */
async function loadSubModelRows() {
  const latestDetail = await queryDetail();
  const rows = latestDetail.subModels || [];
  return { data: { rows, total: rows.length } };
}

/** 版本管理继续读取版本表，与物化发布实例历史分开查询。 */
async function loadVersions() {
  if (!versionRequest) {
    versionRequest = listModelVersions({
      modelId: modelId.value,
      pageNum: 1,
      pageSize: -1,
      orderByColumn: "update_time",
      isAsc: "desc",
    }).then(versionResponse => {
      const allVersions = versionResponse.data?.rows || [];
      versionRows.value = allVersions;
    }).finally(() => {
      versionRequest = null;
    });
  }
  await versionRequest;
  const rows = versionRows.value;
  return { data: { rows, total: rows.length } };
}

/** 发布历史直接读取当前模型的MODEL_INSTANCE，不从版本记录推测发布结果。 */
async function loadPublishHistory() {
  const requestedModelId = modelId.value;
  if (!historyRequest || historyRequest.modelId !== requestedModelId) {
    const promise = listModelPublishHistory(requestedModelId).then(response => {
      const rows = (response.data || []).map(instance => ({
        ...instance,
        releaseTime: instance.publishedTime || instance.startedTime || instance.createTime,
        releaseBy: instance.createBy || "-",
        releaseResult: instance.status,
        releaseDescription: instance.failureReason || instance.remark || "-",
      }));
      // 路由切换到其他模型后，不用旧请求覆盖新模型的发布历史。
      if (modelId.value === requestedModelId) releaseHistory.value = rows;
      return rows;
    }).finally(() => {
      if (historyRequest?.modelId === requestedModelId) historyRequest = null;
    });
    historyRequest = { modelId: requestedModelId, promise };
  }
  const rows = await historyRequest.promise;
  return { data: { rows, total: rows.length } };
}

/** 根据路由模型 ID 加载完整详情，路由缺少 ID 时返回上一页。 */
async function loadDetail() {
  if (!modelId.value) {
    ElMessage.warning("缺少模型ID");
    router.back();
    return;
  }
  loading.value = true;
  try {
    await Promise.all([
      queryDetail(),
      loadVersions(),
      loadPublishHistory(),
    ]);
  } finally {
    loading.value = false;
  }
}

/** 停用当前模型，并刷新详情中的版本状态。 */
function handleDisable() {
  proxy.$modal.confirm("确认停用该模型吗？").then(() => {
    return disableBase(modelId.value);
  }).then(() => {
    proxy.$modal.msgSuccess("模型已封存");
    loadDetail();
  });
}

/** 发起模型修改；草稿或修改中的模型直接进入修改页面。 */
function handleRevision() {
  const status = Number(detail.value.version?.status);
  if (status === 0 || status === 2) {
    router.push({ path: "/model/base/edit", query: { id: modelId.value } });
    return;
  }
  proxy.$modal.confirm("确认发起模型修改吗？").then(() => {
    router.push({ path: "/model/base/edit", query: { id: modelId.value } });
  });
}

watch(modelId, loadDetail, { immediate: true });
watch(() => route.query.tab, () => {
  activeTab.value = getRouteTab();
});
</script>

<style scoped lang="scss">
.ds-selector {
  display: flex;
  width: 100%;
  height: 75vh;
  margin: 0 auto;
  border-radius: 2px;
  overflow: hidden;
  border: 1px solid rgba(232, 232, 232, 0.45);
}
.ds-selector__sidebar {
  width: 308px;
  padding: 22px 0px;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
  overflow-y: hidden;
  overflow-x: hidden;
  box-sizing: border-box;
}
.ds-selector__sidebar :deep(.QtTree-container) {
  height: 100%;
  overflow: hidden;
}
.ds-selector__main {
  position: relative;
  flex: 1;
  padding: 20px;
  border-left: 1px solid rgba(214, 218, 225, 0.45);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 统一左右两侧滚动条样式 */
.ds-selector__sidebar::-webkit-scrollbar,
.ds-selector__main::-webkit-scrollbar {
  width: 4px;
  height: 6px;
}
.ds-selector__sidebar::-webkit-scrollbar-thumb,
.ds-selector__main::-webkit-scrollbar-thumb {
  border-radius: 78px;
  background: rgba(0, 0, 0, 0.1);
}
.ds-selector__sidebar::-webkit-scrollbar-track,
.ds-selector__main::-webkit-scrollbar-track {
  border-radius: 3px;
  background: transparent;
}

.ds-search {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1;
  margin-bottom: 0;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}
.ds-search__input {
  width: 300px;
}
.ds-search__input :deep(.el-input__wrapper) {
  height: 32px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px rgb(214, 218, 225) inset;
}
.ds-search__input :deep(.el-input__inner) {
  font-size: 14px;
  line-height: 22px;
}

.tree-label-with-count {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}
.tree-label-with-count .label-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-label-with-count .count-text {
  font-family: PingFangSC;
  font-size: 14px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.45);
  line-height: 20px;
  flex-shrink: 0;
  text-align: center;
  width: 45px;
}

.category-group {
  margin-bottom: 24px;
}
.category-group.is-first {
  margin-top: 0;
}
.category-group:last-child {
  margin-bottom: 0;
}
.category-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 32px;
  margin: 0 320px 20px 0;
  font-family: PingFang SC;
  font-size: 16px;
  font-weight: 500;
  color: #333333;
  line-height: 22px;
}
.category-group__title {
  flex: 1;
  text-align: left;
}

.ds-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.ds-card {
  position: relative;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  grid-template-rows: 20px 37px;
  column-gap: 17px;
  row-gap: 4px;
  width: 100%;
  height: 86px;
  padding: 13px 17px 12px;
  overflow: hidden;
  border: 1px solid rgba(214, 218, 225, 0.45);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s;
}
.ds-card.is-disabled {
  cursor: not-allowed;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 2px;
  border: 1px solid rgba(214, 218, 225, 0.45);
}
.ds-card.is-disabled .ds-card__title {
  color: rgba(0, 0, 0, 0.65);
}
.ds-card.is-disabled .ds-card__desc {
  color: rgba(0, 0, 0, 0.45);
}
.ds-card:hover:not(.is-disabled) {
  border-color: var(--qt-primary);
  box-shadow: 0 2px 8px rgba(var(--qt-primary-rgb), 0.15);
}
.ds-card--selected {
  border: 1px solid rgba(232, 232, 232, 0.45);
  background-image: linear-gradient(180deg, rgba(255, 255, 255, 0) 0, var(--qt-primary-light) 100%);
  border-color: var(--qt-primary);
}
.ds-card__check {
  position: absolute;
  top: 12px;
  right: 11px;
  width: 14px;
  height: 14px;
  border: 4px solid var(--qt-primary);
  border-radius: 50%;
  background-color: #ffffff;
  color: transparent;
  font-size: 0;
  line-height: 0;
}
.ds-card__check :deep(.el-icon) {
  display: none;
}
.ds-card__header {
  display: contents;
}
.ds-card__icon {
  grid-row: 1/span 2;
  align-self: center;
  width: 36px;
  height: 36px;
  font-size: 32px;
  flex-shrink: 0;
  color: var(--qt-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.ds-card__icon.is-disabled {
  color: rgba(0, 0, 0, 0.2);
}
.ds-card__title {
  grid-column: 2;
  width: 100%;
  height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: PingFangSC;
  font-size: 14px;
  font-weight: 400;
  color: #000000;
  line-height: 20px;
}
.ds-card__desc {
  grid-column: 2;
  width: 100%;
  height: 37px;
  font-family: PingFangSC;
  font-size: 12px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.65);
  line-height: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.empty-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
}
.empty-wrapper .emptyBg {
  text-align: center;
}
.empty-wrapper .emptyBg img {
  width: 130px;
  height: 104px;
  margin-bottom: 8px;
}
.empty-wrapper .emptyBg p {
  color: rgba(0, 0, 0, 0.45);
  font-size: 14px;
  line-height: 22px;
  margin: 0;
}

.detail-value {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagecont-bottom {
  min-height: calc(100vh - 343px)!important
}
</style>
