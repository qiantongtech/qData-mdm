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
  @component DatasourceSummary
  @description 数据源摘要信息展示组件。用于在数据源配置确认等场景中，展示数据源的图标、类型以及各项配置信息（如地址、端口、数据库名等）。
-->
<template>
  <div class="datasource-summary">
    <div class="summary-icon">
      <img :src="iconUrl" :alt="datasourceType || '数据源'" />
    </div>
    <div class="summary-info">
      <div class="info-item" v-for="item in items" :key="item.key">
        <img v-if="item.iconUrl" :src="item.iconUrl" class="custom-svg-icon" :alt="item.label" />
        <div v-else class="item-icon">
          <el-icon v-if="item.icon" aria-hidden="true">
            <component :is="item.icon" />
          </el-icon>
          <i v-else aria-hidden="true">{{ item.iconText }}</i>
        </div>
        <div class="item-text">
          <span class="label">{{ item.label }}</span>
          <span class="value" :title="String(item.value)">{{ item.value||'-' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  iconUrl: {
    type: String,
    required: true
  },
  datasourceType: {
    type: String,
    default: ''
  },
  items: {
    type: Array,
    default: () => []
  }
});
</script>

<style scoped>
.datasource-summary {
  display: flex;
  align-items: center;
  border-radius: 2px;
  border: 1px solid rgba(232, 232, 232, 1);
  min-height: 86px;
  padding: 16px;
  margin-bottom: 20px;
  background-color: #fff;
}

.summary-icon {
  background-color: rgba(255, 255, 255, 1);
  border-radius: 4px;
  height: 54px;
  width: 112px;
  border: 1px solid rgba(232, 232, 232, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-icon img {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.summary-info {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: nowrap;
  gap: 0;
  margin-left: 40px;
  flex: 1;
  overflow-x: auto;
}

.info-item {
  display: flex;
  flex-direction: row;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.item-icon {
  background-color: rgba(var(--qt-primary-rgb), 0.06);
  border-radius: 4px;
  height: 42px;
  width: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--qt-primary);
  flex-shrink: 0;
}

.custom-svg-icon {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

.item-icon .el-icon {
  font-size: 20px;
}

.item-icon i {
  font-size: 14px;
  font-style: normal;
  font-weight: 600;
}

.item-text {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-left: 16px;
  height: 44px;
  flex: 1;
  min-width: 0;
}

.item-text .label {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12px;
  font-weight: normal;
  text-align: left;
  line-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-text .value {
  color: rgba(0, 0, 0, 1);
  font-size: 14px;
  font-weight: normal;
  text-align: left;
  line-height: 20px;
  margin-top: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
