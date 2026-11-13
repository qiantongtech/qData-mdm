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
        :data="encodingDetail"
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
      <template #creator="{ data }">
        {{
          [data.createBy || getResponsibleName(data.creatorId) || '-', getResponsiblePhone(data.creatorId)].filter(Boolean).join(' / ')
        }}
      </template>
      <template #status="{ data }">
        <dict-tag
            :options="model_encoding_status"
            :value="data.version?.status"
        />
      </template>
      <template #isCode="{ data }">
        <span v-if="data.version?.status == 2">否</span>
        <span v-else>是</span>
      </template>
      <template #modelCount>{{ modelCount }}</template>
      <template #ruleCount>{{ ruleCount }}</template>
      <template #ruleStructurePreview>
        <span class="ellipsis el-text el-text--primary el-text--default"
              :title="ruleStructurePreview">{{ ruleStructurePreview }}</span>
      </template>
    </detail-info>

    <div class="pagecont-bottom">
      <el-tabs v-model="activeName" class="demo-tabs">
        <qt-tab-pane label="规则配置" name="1">
          <!-- 当前版本变化时重建规则编辑器，清除上一版本的规则和编辑状态。 -->
          <encoding-item
              :key="encodingDetail.version?.id"
              :encoding-version="encodingDetail.version"
              @rule-count="ruleCount = $event"
              @rule-structure-preview="ruleStructurePreview = $event"
          />
        </qt-tab-pane>
        <qt-tab-pane label="版本管理" name="2">
          <encoding-version :encoding-id="currentEncodingId" @change="handleVersionChange"/>
        </qt-tab-pane>
        <qt-tab-pane label="引用情况" name="3">
          <usage-situation
              :encoding-version-id="encodingDetail.version?.id"
              @model-count="modelCount = $event"
          />
        </qt-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="EncodingDetail">
/** @description 编码方案详情，展示规则、版本及引用情况。 */
import {useRoute} from 'vue-router';
import {getEncoding} from "@/api/model/encoding/encoding";
import {useUserDirectory} from '@/store/system/userDirectory';
import UsageSituation from "@/views/model/encoding/detail/components/usageSituation.vue";
import EncodingItem from "@/views/model/encoding/detail/components/encodingItem.vue";
import EncodingVersion from "@/views/model/encoding/detail/components/version.vue";

const {getResponsibleName, getResponsiblePhone} = useUserDirectory();

const {proxy} = getCurrentInstance();
const {model_encoding_status} = proxy.useDict(
    "model_encoding_status"
);

const activeName = ref('1');
const showSearch = ref(true);
const route = useRoute();
const currentEncodingId = ref();
let requestVersion = 0;
const data = reactive({
  encodingDetail: {},
  form: {},
  ruleCount: 0,
  ruleStructurePreview: '-',
  modelCount: 0,
});

const {encodingDetail, ruleCount, ruleStructurePreview, modelCount} = toRefs(data);

watch(
    () => route.query.id,
    (newId) => {
      loadEncodingDetail(newId);
    },
    {immediate: true}
);

const detailItems = computed(() => [
  {label: "方案编码", key: "code"},
  {
    label: "已产生编码", key: "isCode",
    slot: "isCode"
  },
  {
    label: "规则结构预览",
    key: "ruleStructurePreview",
    slot: "ruleStructurePreview",
    ellipsisClass: "ellipsis",
  },
  {
    label: "描述",
    key: "description",
    span: 24,
    ellipsisClass: "ellipsis-2",
    className: "mt2 mb2",
  },
  {label: "创建人", key: "createBy", slot: "creator"},
  {label: "创建时间", key: "createTime", type: "time"},
  {label: "更新时间", key: "updateTime", type: "time"},
]);

/** 版本变更后刷新当前版本；切换方案后忽略上一方案尚未完成的操作通知。 */
function handleVersionChange(encodingId) {
  if (encodingId === currentEncodingId.value) loadEncodingDetail(encodingId);
}

/** 加载编码方案详情 */
function loadEncodingDetail(encodingId) {
  const currentRequest = ++requestVersion;
  currentEncodingId.value = encodingId;
  encodingDetail.value = {};
  ruleCount.value = 0;
  ruleStructurePreview.value = '-';
  modelCount.value = 0;
  if (encodingId == null) return;

  getEncoding(encodingId).then(response => {
    if (currentRequest === requestVersion && currentEncodingId.value === encodingId) {
      encodingDetail.value = response.data;
    }
  });
}

onActivated(() => {
  if (currentEncodingId.value !== route.query.id) {
    loadEncodingDetail(route.query.id);
  }
});

onDeactivated(() => {
  requestVersion++;
  currentEncodingId.value = undefined;
  encodingDetail.value = {};
  ruleCount.value = 0;
  ruleStructurePreview.value = '-';
  modelCount.value = 0;
  activeName.value = '1';
});

</script>
