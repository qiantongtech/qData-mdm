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
  <div class="app-container">
    <el-alert v-if="loadFailed" type="error" :closable="false" title="模型目录详情加载失败">
      <el-button link type="primary" @click="loadDetail(route.query.id)">重新加载</el-button>
    </el-alert>
    <detail-info
        mode="free"
        v-loading="loading"
        :data="detail"
        :header="{
        nameKey: 'name', statusKey: 'validFlag',
        statusOptions: [
          { label: '启用', value: true, elTagType: 'success' },
          { label: '禁用', value: false, elTagType: 'danger' },
        ],
      }"
        :items="detailItems"
    >
      <template #projectName="{ data }">
        <span class="project-name-with-icon">
          <img :src="getProjectIcon(project.icon)" alt=""/>
          <span>{{ data.projectName || '-' }}</span>
        </span>
      </template>
      <template #responsiblePerson="{ data }">
        <!-- 电话为空就不拼分隔符；公共用户列表加载完成后，姓名和电话会自动补上。 -->
        {{
          [getResponsibleName(data.responsibleUserId) || '-', getResponsiblePhone(data.responsibleUserId)].filter(Boolean).join(' / ')
        }}
      </template>
      <template #creator="{ data }">
        {{
          [data.createBy || getResponsibleName(data.creatorId) || '-', getResponsiblePhone(data.creatorId)].filter(Boolean).join(' / ')
        }}
      </template>
    </detail-info>
    <div v-if="detail.id && project.id" class="pagecont-bottom">
      <el-tabs v-model="activeTab">
        <qt-tab-pane label="子模型目录" name="children">
          <model-layer
              :key="detail.id"
              embedded
              search
              :project="project"
              :parent-id="detail.id"
              @changed="refreshDetail"
          />
        </qt-tab-pane>
        <qt-tab-pane label="关联模型" name="models" lazy>
          <model-base :key="detail.id" embedded search :project-id="detail.projectId" :layer-id="detail.id"/>
        </qt-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="ModelLayerDetail">
import {useRoute} from 'vue-router';
import {getLayer} from '@/api/model/layer/layer';
import {getProject} from '@/api/model/project/project';
import {getProjectIcon} from '@/utils/imageUrl';
import { useUserDirectory } from '@/store/system/userDirectory';
import ModelLayer from '@/views/model/layer/index.vue';
import ModelBase from '@/views/model/base/index.vue';

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const route = useRoute();
const detail = ref({});
const project = ref({});
const loading = ref(false);
const loadFailed = ref(false);
const activeTab = ref('children');
let requestVersion = 0;
let currentLayerId;
const detailItems = [
  {label: '目录编码', key: 'code'},
  {label: '所属主数据域', key: 'projectName', slot: 'projectName'},
  {label: '关联模型数', key: 'modelCount'},
  {label: '描述', key: 'description', span: 24, ellipsisClass: "ellipsis-2", className: "mt2 mb2"},
  {label: '负责人', key: 'responsibleUserId', slot: 'responsiblePerson'},
  {label: '创建人', key: 'createBy', slot: 'creator'},
  {label: '更新时间', key: 'updateTime', date: true},
];

/** 路由切换时清理旧内容，异步返回只更新当前目录。 */
async function loadDetail(id, refresh = false) {
  const version = ++requestVersion;
  currentLayerId = id;
  if (!refresh) {
    // 切换到另一条目录才重置页签，当前目录保存后的刷新保留正在看的页签。
    detail.value = {};
    project.value = {};
    activeTab.value = route.query.tab === 'models' ? 'models' : 'children';
  }
  loadFailed.value = false;
  if (id == null || id === '') return;
  loading.value = true;
  try {
    const response = await getLayer(id);
    // 用户可能已经切换目录，旧请求返回后不能替换新目录的内容。
    if (version !== requestVersion) return;
    if (!response.data) {
      loadFailed.value = true;
      return;
    }
    const projectResponse = await getProject(response.data.projectId);
    // 查询主数据域期间也可能发生切换，所以第二次请求返回后还要检查一次。
    if (version !== requestVersion) return;
    project.value = projectResponse.data || {};
    detail.value = {...response.data, projectName: project.value.name};
  } catch {
    if (version === requestVersion) loadFailed.value = true;
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}

// 子目录变更后刷新当前详情和统计，继续停留在原页签。
function refreshDetail() {
  loadDetail(route.query.id, true);
}

// 缓存页面也能听到路由变化，先判断路径，避免拿其他页面的 id 来查询目录。
watch(() => route.path === '/model/layer/detail' ? route.query.id : null, id => {
  if (id != null) loadDetail(id);
}, {immediate: true});
// 点击关联模型数进入时直接选中“关联模型”；只切页签不必重查基础信息。
watch(() => route.path === '/model/layer/detail' ? route.query.tab : null, tab => {
  if (route.path === '/model/layer/detail') activeTab.value = tab === 'models' ? 'models' : 'children';
}, {immediate: true});
// 重新进入缓存详情时，路由指向了其他目录才重新加载。
onActivated(() => {
  if (String(currentLayerId) !== String(route.query.id)) loadDetail(route.query.id);
});
// 离开详情就清空旧记录，并让未完成的请求失效，防止下次进入短暂显示上一条。
onDeactivated(() => {
  requestVersion++;
  currentLayerId = undefined;
  detail.value = {};
  project.value = {};
  loading.value = false;
});
</script>

<style scoped lang="scss">
.project-name-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  vertical-align: middle;

  img {
    width: 16px;
    height: 16px;
    flex-shrink: 0;
    object-fit: contain;
  }
}
</style>
