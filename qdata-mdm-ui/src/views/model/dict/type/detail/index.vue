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
        :data="dataDetail"
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
    </detail-info>

    <div class="pagecont-bottom">
      <el-tabs v-model="activeName" class="demo-tabs">
        <qt-tab-pane label="数据项" name="1">
          <dict-data-index
              v-if="currentTypeId != null"
              :type-id="currentTypeId"
              @items-change="usageRef?.refreshList()"
          />
        </qt-tab-pane>
        <qt-tab-pane label="引用情况" name="2">
          <data-usage
              v-if="currentTypeId != null"
              ref="usageRef"
              :type-id="currentTypeId"
              @counts-change="handleCountsChange"
          />
        </qt-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup name="TypeDetail">
/** @description 参考数据类型详情，展示数据项及引用情况。 */
import {useRoute} from 'vue-router';

import {getType} from "@/api/model/dict/type/type";
import { useUserDirectory } from '@/store/system/userDirectory';
import DataUsage from "@/views/model/dict/type/detail/dataUsage.vue";
import DictDataIndex from "@/views/model/dict/data/index.vue";

const { getResponsibleName, getResponsiblePhone } = useUserDirectory();

const activeName = ref('1');
const usageRef = ref(null);
const overviewCounts = ref({});
const detailItems = computed(() => [
  {label: "参考数据编码", key: "code"},
  {label: "数据项数量", value: overviewCounts.value.itemCount ?? "-"},
  {label: "引用情况数量", value: overviewCounts.value.referenceCount ?? "-"},
  {label: "描述", key: "description", span: 24, ellipsisClass: "ellipsis", className: "mt2 mb2"},
  {label: "创建人", key: "createBy", slot: "creator"},
  {label: "创建时间", key: "createTime", type: "time"},
  {label: "更新时间", key: "updateTime", type: "time"},
]);

const showSearch = ref(true);
const route = useRoute();
const currentTypeId = ref();
let requestVersion = 0;
const data = reactive({
  dataDetail: {},
  form: {},
});

const {dataDetail} = toRefs(data);

watch(
    () => route.query.id,
    (newId) => {
      loadTypeDetail(newId);
    },
    {immediate: true}
);

/**
 * 只接收当前参考数据类型的概览数量。
 * @param {string|number} typeId 参考数据类型 ID
 * @param {{itemCount: number, referenceCount: number}} counts 两个页签的总条数
 */
function handleCountsChange(typeId, counts) {
  if (typeId === currentTypeId.value) {
    overviewCounts.value = counts;
  }
}

/**
 * 加载参考数据类型详情并清空上一类型的概览数量。
 * @param {string|number|undefined} typeId 参考数据类型 ID
 */
function loadTypeDetail(typeId) {
  const currentRequest = ++requestVersion;
  currentTypeId.value = typeId;
  dataDetail.value = {};
  overviewCounts.value = {};
  if (typeId == null) return;

  getType(typeId).then(response => {
    if (currentRequest === requestVersion && currentTypeId.value === typeId) {
      dataDetail.value = response.data;
    }
  });
}

onActivated(() => {
  if (currentTypeId.value !== route.query.id) {
    loadTypeDetail(route.query.id);
  }
});

onDeactivated(() => {
  requestVersion++;
  currentTypeId.value = undefined;
  dataDetail.value = {};
  overviewCounts.value = {};
  activeName.value = '1';
});

</script>
