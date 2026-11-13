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
    <detail-info
        :show="showSearch"
        :data="projectDetail"
        :header="{
        className: 'clearfixs',
        nameKey: 'name',
        statusKey: 'validFlag',
        statusOptions: [
          { label: '启用', value: true, elTagType: 'success' },
          { label: '禁用', value: false, elTagType: 'danger' },
        ],
      }"
        mode="free"
        :items="detailItems"
    >
      <template #responsiblePerson="{ data }">
        <!-- 姓名和电话用斜杠分开，缺少电话时不留一个多余的斜杠。 -->
        {{
          [getResponsibleName(data.responsibleUserId) || '-', getResponsiblePhone(data.responsibleUserId)].filter(Boolean).join(' / ')
        }}
      </template>
      <template #creator="{ data }">
        {{
          [data.createBy || getResponsibleName(data.creatorId) || '-', getResponsiblePhone(data.creatorId)].filter(Boolean).join(' / ')
        }}
      </template>
      <template #modelCount="{ data }">
        {{ data.modelCount }} 个
      </template>
      <template #layerCount="{ data }">
        {{ data.layerCount }} 个
      </template>
    </detail-info>

    <div class="pagecont-bottom">
      <el-tabs v-model="activeName" class="demo-tabs">
        <qt-tab-pane label="模型目录" name="1">
          <model-layer
              v-if="projectDetail.id != null"
              :key="currentTypeId"
              embedded
              search
              :project="projectDetail"
              @changed="loadProjectDetail(currentTypeId, true)"
          />
        </qt-tab-pane>
        <qt-tab-pane label="关联模型" name="2">
          <model-base v-if="currentTypeId != null" :key="currentTypeId" embedded search :project-id="currentTypeId"
                      @model-count="modelCount"
          />
        </qt-tab-pane>
      </el-tabs>
    </div>

  </div>
</template>

<script setup name="ModelProjectDetail">
/** @description 主数据域详情，展示模型目录与关联模型。 */
import {useRoute} from 'vue-router';

import {getProject} from "@/api/model/project/project";
import {useUserDirectory} from '@/store/system/userDirectory';
import ModelLayer from "@/views/model/layer/index.vue";
import ModelBase from "@/views/model/base/index.vue";

const {getResponsibleName, getResponsiblePhone} = useUserDirectory();

const activeName = ref('1');
const showSearch = ref(true);
const route = useRoute();
const currentTypeId = ref();
const detailItems = computed(() => [
  {label: "主数据域编码", key: "code"},
  {label: "主数据模型数", key: "modelCount", slot: "modelCount"},
  {label: "模型目录数", key: "layerCount", slot: "layerCount"},
  {label: "描述", key: "description", span: 24, ellipsisClass: "ellipsis-2", className: "mt2 mb2"},
  {label: "负责人", key: "responsibleUserId", slot: "responsiblePerson"},
  {label: "创建人", key: "createBy", slot: "creator"},
  {label: "更新时间", key: "updateTime", type: "time"},
]);
const data = reactive({
  projectDetail: {},
});

const {projectDetail} = toRefs(data);
let requestVersion = 0;

// 首次打开和同页切换主数据域都从路由取 ID，不沿用上一次详情。
watch(() => route.query.id, id => loadProjectDetail(id), {immediate: true});

// 资源标签把目标页签带在地址里；只切换页签时不必重新查询主数据域详情。
watch(() => route.path === '/model/project/detail' ? route.query.tab : null, tab => {
  if (route.path === '/model/project/detail') activeName.value = tab === '2' ? '2' : '1';
}, {immediate: true});

/** 根据当前路由加载主数据域详情，忽略切换前未完成的请求。 */
function loadProjectDetail(projectId, refresh = false) {
  const currentRequest = ++requestVersion;
  currentTypeId.value = projectId;
  if (!refresh) projectDetail.value = {};
  if (projectId == null) return;

  getProject(projectId).then(response => {
    // 切换前发出的请求可能更晚回来，只允许当前这次请求更新详情。
    if (currentRequest !== requestVersion) return;
    projectDetail.value = {
      ...response.data,
      layerCount: response.data?.layerCount ?? 0,
      modelCount: response.data?.modelCount ?? 0,
    };
  });
}

// 返回缓存页时，当前域与地址中的域不同才补查，避免首次进入重复请求。
onActivated(() => {
  // 离开缓存页时会重置页签，再次进入要按本次跳转参数恢复，而不是固定显示第一个。
  activeName.value = route.query.tab === '2' ? '2' : '1';
  if (currentTypeId.value !== route.query.id) loadProjectDetail(route.query.id);
});

// 离开时清理数据和页签，并作废未完成的请求，防止再次进入看到旧域内容。
onDeactivated(() => {
  requestVersion++;
  currentTypeId.value = undefined;
  projectDetail.value = {};
  activeName.value = '1';
});

/** 获取模型的数量 */
function modelCount(count) {
  nextTick(() => {
    projectDetail.value.modelCount = count;
  });
}
</script>
