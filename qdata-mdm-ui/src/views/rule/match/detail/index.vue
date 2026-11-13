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

<!-- 匹配规则详情：独立加载规则，展示基本信息、参与字段及检测任务。 -->
<template>
  <div v-loading="loading" class="app-container">
    <detail-info
        :data="detail"
        :header="detailHeader"
        :items="detailItems"
        :gutter="2"
        @back="router.push('/rule/match')"
        class="rule-detail-info"
        mode="free"
    >
      <template #model>
        <RuleModelDisplay :model="model" :fallback="detail"/>
      </template>
      <template #location>
        <RuleModelDisplay :model="model" :fallback="detail" location/>
      </template>
      <template #responsible>
        {{ getResponsibleName(detail.responsibleUserId) || '-' }} /
        {{ getResponsiblePhone(detail.responsibleUserId) || '-' }}
      </template>
    </detail-info>
    <div class="pagecont-bottom">
      <rule-detail-tabs
          v-if="detail.modelId"
          :key="detail.id"
          :model-id="detail.modelId"
          :selected-field-ids="selectedFieldIds"
          @model-loaded="model = $event"
      />
    </div>
  </div>
</template>

<script setup name="ruleMatchDetail">
import {computed, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {getRulematch} from '@/api/rule/match/match';
import {useUserDirectory} from '@/store/system/userDirectory';
import DetailInfo from '@/components/DetailInfo/index.vue';
import RuleModelDisplay from '@/views/rule/components/RuleModelDisplay.vue';
import RuleDetailTabs from '@/views/rule/match/detail/ruleDetailTabs.vue';

/** 匹配规则详情页。@author chentangwen */

const {getResponsibleName, getResponsiblePhone} = useUserDirectory();

const route = useRoute();
const router = useRouter();
const detail = ref({});
const model = ref({});
const loading = ref(false);
const detailHeader = {
  className: 'clearfixs',
  idKey: 'id',
  nameKey: 'ruleName',
  routerBack: false,
  statusKey: 'validFlag',
  statusOptions: [
    {label: '启用', value: 'true', elTagType: 'success'},
    {label: '禁用', value: 'false', elTagType: 'danger'},
  ],
};
const detailItems = [
  {label: '规则编码', key: 'ruleCode'},
  {label: '主数据模型', key: 'modelId', slot: 'model'},
  {label: '归属位置', key: 'projectName', slot: 'location'},
  {label: '描述', key: 'description', span: 24},
  {label: '匹配分值', key: 'ruleScore'},
  {label: '负责人', key: 'responsibleUserId', slot: 'responsible'},
  {label: '创建时间', key: 'createTime', type: 'time'},
];

// 优先使用配置中的字段列表，兼容历史 JSON 数组及逗号分隔格式。
const selectedFieldIds = computed(() => {
  let config = detail.value.ruleConfig;
  if (typeof config === 'string') {
    try {
      config = JSON.parse(config);
    } catch {
      config = {};
    }
  }
  if (Array.isArray(config?.fieldList) && config.fieldList.length) {
    return config.fieldList.map(item => item?.modelFieldId)
        .filter(id => id !== null && id !== undefined && id !== '');
  }
  const value = detail.value.modelFieldId;
  if (Array.isArray(value)) return value;
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed;
  } catch {
    // 历史规则使用逗号分隔字段 ID。
  }
  return String(value).split(',').map(id => id.trim()).filter(Boolean);
});

// 切换记录或离开路由时，使旧请求失效。
watch(() => [route.path, route.query.id], async ([path, id], _, onCleanup) => {
  let active = true;
  onCleanup(() => {
    active = false;
  });
  if (path !== '/rule/match/detail') return;
  detail.value = {};
  model.value = {};
  loading.value = false;
  if (!id) return;
  loading.value = true;
  try {
    const response = await getRulematch(id);
    if (!active) return;
    detail.value = response.data || {};

  } catch {
    // 请求失败提示由公共请求工具处理。
  } finally {
    if (active) loading.value = false;
  }
}, {immediate: true});
</script>

<style scoped lang="scss">
.rule-detail-info :deep(.el-row) {
  row-gap: var(--qt-space-1);
}

.rule-detail-icon-text {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.rule-detail-icon {
  flex-shrink: 0;
  width: var(--qt-space-4);
  height: var(--qt-space-4);
}
</style>

<style scoped lang="scss">
/* 来源与任务详情共用的展示规则；固定描述表列宽，避免长内容挤压相邻字段。 */
.basic-info :deep(.el-descriptions__table),
.source-business-fields :deep(.el-descriptions__table) {
  width: 100%;
  table-layout: fixed;
}

/* 保留任务描述的换行，长连续文本可在容器内折行。 */
.task-remark {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* 唯一标识较多时允许标签换行，保持各项间距一致。 */
.unique-fields {
  display: flex;
  flex-wrap: wrap;
  gap: var(--qt-space-2);
  padding: var(--qt-space-2) 0;
}

/* 让详情标题内容可收缩，并将操作按钮排在信息项之后。 */
:deep(.infotop-title.clearfixs) {
  .task-item {
    flex: 1;
    min-width: 0;
  }

  .btn-style {
    order: 3;
  }
}

/* 来源系统描述保留用户输入的空格和换行。 */
.source-basic-info :deep(.base-content) {
  white-space: pre-wrap;
}
</style>
