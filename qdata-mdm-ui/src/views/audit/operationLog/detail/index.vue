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
  @file operationLog/detail/index.vue
  @description 操作审计详情
  @author zyy
-->
<template>
  <el-scrollbar always class="audit-page-scroll">
    <div class="app-container">
      <detail-info
        :data="summaryData"
        :header="summaryHeader"
        :items="summaryItems"
        time-format="{y}-{m}-{d} {h}:{i}:{s}"
        mode="free"
      >
        <template #summaryModel="{ data }">
          <audit-operation-cell kind="model" :row="data" :show-model-description="false" />
        </template>
        <template #summaryLocation="{ data }">
          <div v-if="data.locationName" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getProjectIcon(data.projectIcon)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="data.locationName">{{ data.locationName }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template #summaryResult="{ data }">
          <dict-tag v-if="data.operateResult" :type="data.operateResult === '成功' ? 'success' : 'danger'">{{ data.operateResult }}</dict-tag>
          <span v-else>-</span>
        </template>
      </detail-info>
      <div class="pagecont-bottom">
        <el-tabs v-model="activeName" class="demo-tabs">
          <el-tab-pane label="操作详情" name="detail">
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">审计事实</div>
                </template>
                <descriptions-info :data="logData" :items="auditFactItems" :column="2">
                  <template #operateResult="{ data }">
                    <dict-tag :type="data.operateResult === '成功' ? 'success' : 'danger'">{{ data.operateResult || '-' }}</dict-tag>
                  </template>
                  <template #operationType="{ data }">
                    <dict-tag v-if="data.type" :type="operationTagType(data.type)">{{ data.type }}</dict-tag>
                    <span v-else>-</span>
                  </template>
                </descriptions-info>
              </qt-wrap>
            </div>
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">操作对象</div>
                </template>
                <descriptions-info :data="objectInfo" :items="objectItems" :column="2">
                  <template #operationType="{ data }">
                    <dict-tag v-if="data.type" :type="operationTagType(data.type)">{{ data.type }}</dict-tag>
                    <span v-else>-</span>
                  </template>
                  <template #lifecycle="{ data }">
                    <dict-tag v-if="data.lifecycle && !['-', '—'].includes(data.lifecycle)" :type="lifecycleTagType(data.lifecycle)">{{ data.lifecycle }}</dict-tag>
                    <span v-else>-</span>
                  </template>
                </descriptions-info>
              </qt-wrap>
            </div>
          </el-tab-pane>

          <el-tab-pane v-if="isBatch" label="影响对象" name="affected">
            <section class="detail-section">
              <qt-wrap :columns="affectedColumns"
                       :table-ref="affectedTableRef"
                       :config="{ fullContent: false, actions: { table: { search: false } } }"
                       class="affected-wrap"
              >
                <el-table
                  :data="pagedAffectedRows"
                  stripe
                  empty-text="暂无影响对象"
                  @sort-change="sortAffectedRows"
                >
                  <el-table-column
                    v-if="!affectedColumns[0].hide"
                    label="编号"
                    prop="rowNumber"
                    width="90"
                    align="center"
                    sortable="custom"
                  />
                  <el-table-column v-if="!affectedColumns[1].hide" label="业务对象" min-width="220">
                    <template #default="{ row }">
                      <div>{{ row.objectName || '-' }}</div>
                      <div class="cell-sub">{{ row.objectCode || '-' }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column v-if="!affectedColumns[2].hide"
                                   label="主数据模型"
                                   prop="modelName"
                                   min-width="160"
                                   align="center"
                  />
                  <el-table-column v-if="!affectedColumns[3].hide"
                                   label="当前生命周期"
                                   prop="lifecycle"
                                   min-width="120"
                                   align="center"
                  />
                  <el-table-column v-if="!affectedColumns[4].hide"
                                   label="本次处理结果"
                                   prop="result"
                                   min-width="120"
                                   align="center"
                  >
                    <template #default="{ row }"><dict-tag :type="row.result === '成功' ? 'success' : 'danger'">{{ row.result || '-' }}</dict-tag></template>
                  </el-table-column>
                </el-table>
                <pagination
                  v-if="affectedRows.length"
                  v-model:page="affectedPage"
                  v-model:limit="affectedPageSize"
                  :total="affectedRows.length"
                  :auto-scroll="false"
                />
              </qt-wrap>
            </section>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </el-scrollbar>
</template>

<script setup name="AuditOperationLogDetail">
import { getProjectIcon } from '@/utils/imageUrl';
import { getAuditOperation } from '@/api/audit/trace';
import { useRoute } from 'vue-router';
import DetailInfo from '@/components/DetailInfo/index.vue';
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue';
import Pagination from '@/components/Pagination/index.vue';
import AuditOperationCell from '../AuditOperationCell.vue';

const route = useRoute();
const activeName = ref('detail');
const logData = ref({});
const affectedPage = ref(1);
const affectedPageSize = ref(10);
const affectedSort = ref(null);
const affectedTableRef = {
  getList: () => {
    reloadAuditDetail().catch(() => {
      // 请求工具负责错误提示，刷新失败时保留当前详情。
    });
  },
};
const affectedColumns = reactive([
  { label: '编号', prop: 'rowNumber', hide: false },
  { label: '业务对象', prop: 'objectName', hide: false },
  { label: '主数据模型', prop: 'modelName', hide: false },
  { label: '当前生命周期', prop: 'lifecycle', hide: false },
  { label: '本次处理结果', prop: 'result', hide: false },
]);
const isBatch = computed(() => logData.value.type?.includes('批量') || Number(logData.value.affectCount) > 1);
const affectedRows = computed(() => logData.value.affectedRows || []);
const numberedAffectedRows = computed(() => affectedRows.value.map((row, index) => ({ ...row, rowNumber: index + 1 })));
const pagedAffectedRows = computed(() => {
  const rows = affectedSort.value === 'descending' ? [...numberedAffectedRows.value].reverse() : numberedAffectedRows.value;
  const start = (affectedPage.value - 1) * affectedPageSize.value;
  return rows.slice(start, start + affectedPageSize.value);
});

function sortAffectedRows({ order }) {
  affectedSort.value = order;
  affectedPage.value = 1;
}

const summaryData = computed(() => {
  const log = logData.value;
  const masterName = log.masterDataName || (log.scope === 'master' ? log.objectName : '');
  const masterCode = log.masterDataCode || log.businessCode;
  const directory = log.layerName?.split(' / ').map(name => name.trim()).filter(Boolean).at(-1);
  const operator = [log.operatorName, log.operatorPhone].filter(Boolean).join(' / ');
  return {
    recordId: log.sourceId ?? log.id,
    title: masterName || log.objectName || '-',
    type: log.type,
    masterCode,
    modelId: log.modelId,
    model: log.scope === 'model' ? log.objectName : '',
    modelDisplayName: log.modelDisplayName,
    modelName: log.modelName,
    modelBusinessCode: log.modelBusinessCode,
    modelCode: log.modelCode,
    modelType: log.modelType,
    modelIcon: log.modelIcon,
    modelVersionNo: log.modelVersionNo,
    modelVersion: log.modelVersion,
    projectIcon: log.projectIcon,
    locationName: [log.projectName, directory].filter(Boolean).join(' / '),
    operateDesc: log.operateDesc,
    operateResult: log.operateResult,
    operator,
    operateTime: log.operateTime,
  };
});
const summaryHeader = computed(() => ({
  showId: summaryData.value.recordId != null,
  idKey: 'recordId',
  nameKey: 'title',
  statusKey: 'type',
  statusOptions: summaryData.value.type ? [{ label: summaryData.value.type, value: summaryData.value.type, elTagType: 'primary' }] : [],
}));
const summaryItems = [
  { label: '主数据编码', key: 'masterCode' },
  { label: '主数据模型', slot: 'summaryModel' },
  { label: '归属位置', key: 'locationName', slot: 'summaryLocation' },
  { label: '操作描述', key: 'operateDesc', span: 24, className: 'mt2 mb2' },
  { label: '操作结果', key: 'operateResult', slot: 'summaryResult' },
  { label: '操作人', key: 'operator' },
  { label: '操作时间', key: 'operateTime', type: 'time' },
];
let detailRequestSequence = 0;
async function reloadAuditDetail() {
  const sequence = ++detailRequestSequence;
  const { id, source = 'log' } = route.query;
  if (!id) return;
  const response = await getAuditOperation(id, source);
  if (sequence === detailRequestSequence) logData.value = response.data || {};
}
watch(() => [route.query.id, route.query.source], async () => {
  activeName.value = 'detail';
  affectedPage.value = 1;
  affectedSort.value = null;
  logData.value = {};
  await reloadAuditDetail();
}, { immediate: true });

const auditFactItems = [
  { label: '操作结果', key: 'operateResult', slot: 'operateResult' },
  { label: '业务范围', key: 'businessScope' },
  { label: '所属功能', key: 'functionName' },
  { label: '操作类型', key: 'type', slot: 'operationType' },
];

function operationTagType(type) {
  return {
    新增: 'success', 复制: 'success', 失效: 'danger', 冻结: 'info', 归档: 'info',
    历史版本恢复: 'warning', 发布模型: 'success', 停用模型: 'danger',
  }[type] || 'primary';
}

function lifecycleTagType(value) {
  if (['生效', '有效'].includes(value)) return 'success';
  if (['失效', '冻结', '归档'].includes(value)) return 'info';
  return 'primary';
}

const objectInfo = computed(() => {
  const log = logData.value;
  if (log.scope === 'model') {
    return {
      objectType: '主数据模型', modelName: log.modelDisplayName || log.modelName || log.objectName,
      modelCode: log.modelBusinessCode || log.modelCode,
      modelVersion: log.modelVersionNo || log.modelVersion, type: log.type, operateDesc: log.operateDesc,
    };
  }
  return {
    objectType: log.objectType, objectName: log.objectName, objectId: log.objectId,
    model: log.modelDisplayName || log.modelName, lifecycle: log.masterLifecycle,
    operateDesc: log.operateDesc,
  };
});

const objectItems = computed(() => logData.value.scope === 'model' ? [
  { label: '对象类型', key: 'objectType' },
  { label: '模型名称', key: 'modelName' },
  { label: '模型编码', key: 'modelCode' },
  { label: '模型版本', key: 'modelVersion' },
  { label: '操作类型', key: 'type', slot: 'operationType' },
  { label: '操作说明', key: 'operateDesc' },
] : [
  { label: '对象类型', key: 'objectType' },
  { label: '主数据名称', key: 'objectName' },
  { label: '主数据ID', key: 'objectId' },
  { label: '主数据模型', key: 'model' },
  { label: '当前生命周期', key: 'lifecycle', slot: 'lifecycle' },
  { label: '操作说明', key: 'operateDesc' },
]);
</script>

<style lang="scss" scoped>
.audit-page-scroll {
  height: calc(100vh - 94px);
}
.app-container {
  margin: 15px 15px 0;
}
.detail-section {
  margin: 18px 0 28px;
}
.cell-sub {
  color: var(--qt-text-secondary);
  font-size: 12px;
}
</style>
