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
  @file changeHistory/detail/index.vue
  @description 变更历史详情
  @author zyy
-->
<template>
  <el-scrollbar always class="audit-page-scroll">
    <div class="app-container">
      <detail-info :header="{ className: 'clearfixs no-square-id', idKey: 'code', nameKey: 'name' }"
                   :data="change"
                   :items="[]"
                   mode="free"
                   class="change-detail-header"
      />
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab" class="demo-tabs">
          <qt-tab-pane label="变更概览" name="overview">
            <descriptions-info :data="change"
                               :items="overviewItems"
                               :column="2"
                               class="overview-descriptions"
            >
              <template #result="{ data }">
                <dict-tag :type="data.result === '已生效' ? 'success' : 'danger'">{{ data.result || '-' }}</dict-tag>
              </template>
            </descriptions-info>
          </qt-tab-pane>
          <qt-tab-pane label="变化明细" name="detail">
            <template v-if="change.category === '生命周期变化'">
              <el-alert title="生命周期变化不生成新的数据版本，这里展示本次状态迁移和最终正式状态。" type="info" :closable="false" />
              <div class="drawer-title">生命周期状态变化</div>
              <el-steps :active="3" finish-status="success" align-center>
                <el-step title="变更前状态" :description="lifecycle.before || '-'" />
                <el-step :title="change.result === '已生效' ? '变更后状态' : '拟变更状态'" :description="lifecycle.proposed || '-'" />
                <el-step title="最终正式状态" :description="lifecycle.final || '-'" />
              </el-steps>
              <descriptions-info :data="lifecycleInfo" :items="lifecycleItems" :column="2" />
            </template>
            <template v-else-if="change.result === '已生效'">
              <el-table :data="fields"
                        stripe
                        border
                        empty-text="暂无业务字段"
              >
                <el-table-column label="字段" prop="field" min-width="140" />
                <el-table-column label="修改前"
                                 prop="before"
                                 min-width="160"
                                 show-overflow-tooltip
                />
                <el-table-column label="修改后"
                                 prop="after"
                                 min-width="160"
                                 show-overflow-tooltip
                />
                <el-table-column label="变化" width="90" align="center">
                  <template #default="{ row }">
                    <dict-tag :type="row.before === row.after ? 'info' : 'warning'">{{ row.before === row.after ? '未变化' : row.before === '-' ? '新增' : '修改' }}</dict-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <template v-else>
              <el-alert title="本次拟变更未形成新的正式主数据，最终正式值保持不变。" type="warning" :closable="false" />
              <el-table :data="fields"
                        stripe
                        border
                        empty-text="暂无业务字段"
              >
                <el-table-column label="字段" prop="field" min-width="140" />
                <el-table-column label="变更前正式值"
                                 prop="before"
                                 min-width="160"
                                 show-overflow-tooltip
                />
                <el-table-column label="本次拟变更值"
                                 prop="proposed"
                                 min-width="160"
                                 show-overflow-tooltip
                />
                <el-table-column label="最终正式值"
                                 prop="final"
                                 min-width="160"
                                 show-overflow-tooltip
                />
              </el-table>
            </template>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </div>
  </el-scrollbar>
</template>

<script setup name="AuditChangeHistoryDetail">
import { useRoute } from 'vue-router';
import { getAuditChange } from '@/api/audit/trace';
import DetailInfo from '@/components/DetailInfo/index.vue';
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue';

const route = useRoute();
const activeTab = ref('overview');
const change = ref({});
const fields = computed(() => change.value.fields || []);
const lifecycle = computed(() => change.value.lifecycle || {});
const lifecycleInfo = computed(() => ({
  type: change.value.type,
  time: change.value.time,
  result: change.value.result,
  final: lifecycle.value.final,
  summary: change.value.summary,
}));

const overviewItems = [
  { label: '变更编号', key: 'operateId' },
  { label: '变化分类', key: 'category' },
  { label: '变更类型', key: 'type' },
  { label: '变更时间', key: 'time' },
  { label: '处理结果', key: 'result', slot: 'result' },
  { label: '主数据模型', key: 'model' },
  { label: '变化说明', key: 'summary', span: 2 },
];
const lifecycleItems = [
  { label: '变更类型', key: 'type' },
  { label: '变更时间', key: 'time' },
  { label: '处理结果', key: 'result' },
  { label: '最终状态', key: 'final' },
  { label: '变化说明', key: 'summary', span: 2 },
];

watch(() => route.query.id, async id => {
  change.value = {};
  activeTab.value = 'overview';
  if (!id) return;
  const detail = (await getAuditChange(id)).data || {};
  // 详情编码直接使用审计接口返回值，不用 MASTER_RECORD_ID 或前端演示规则替代。
  change.value = { ...detail, code: detail.businessCode || '-' };
}, { immediate: true });
</script>

<style lang="scss" scoped>
.audit-page-scroll {
  height: calc(100vh - 94px);
}
.app-container {
  margin: 15px 15px 0;
}
.change-detail-header {
  display: flex;
  align-items: center;
  min-height: 72px;
}
.change-detail-header :deep(.infotop) {
  width: 100%;
}
.change-detail-header :deep(.infotop-title) {
  margin-bottom: 0;
}
.demo-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}
.overview-descriptions :deep(.el-descriptions__table) {
  table-layout: fixed;
}
.drawer-title {
  font-weight: normal;
  margin: 20px 0 12px;
}
</style>
