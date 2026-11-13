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
  <section class="model-summary">
    <div class="model-summary__identity">
      <div class="model-summary__icon" aria-hidden="true">
        <img :src="getDefaultModelIcon(modelType)" alt="" />
      </div>
      <div class="model-summary__name">
        <div class="model-summary__title">
          <strong :title="detail.modelName || fallbackName">{{ detail.modelName || fallbackName }}</strong>
          <el-tag size="small" type="success" effect="light">已发布</el-tag>
        </div>
        <div class="model-summary__code" :title="detail.modelCode">{{ detail.modelCode || '-' }}</div>
      </div>
    </div>

    <div class="model-summary__facts">
      <div v-for="item in items" :key="item.label" class="model-summary__fact">
        <div class="model-summary__icon" aria-hidden="true">
          <svg-icon :icon-class="item.icon" />
        </div>
        <div class="model-summary__fact-text">
          <span>{{ item.label }}</span>
          <strong :title="item.value">{{ item.value }}</strong>
        </div>
      </div>
    </div>

    <div class="model-summary__actions btn-style">
      <el-button
          class="fh_btn"
          type="primary"
          plain
          @mousedown="event => event.preventDefault()"
          @click="$emit('back')"
      >
        <svg-icon iconClass="fhs"/>
        返回
      </el-button>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { parseTime, selectDictLabel } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import { getDefaultModelIcon } from '@/utils/imageUrl'

const props = defineProps({
  detail: { type: Object, default: () => ({}) },
  modelType: { type: String, default: 'NORMAL' }
})

defineEmits(['back'])

const { model_type } = useDict('model_type')
const fallbackName = computed(() => props.modelType === 'CATEGORY' ? '分类主数据' : '权威主数据')
const items = computed(() => [
  { label: '模型类目', icon: 'model-category', value: selectDictLabel(model_type.value, props.modelType) || '-' },
  { label: '主数据域', icon: 'master-data-domain', value: props.detail.domainName || '-' },
  { label: '当前版本', icon: 'current-version', value: props.detail.versionNo || '-' },
  { label: '正式数据', icon: 'formal-data', value: props.detail.dataCount == null ? '-' : `${props.detail.dataCount} 条` },
  { label: '最近更新', icon: 'latest-update', value: parseTime(props.detail.latestDataUpdateTime) || '-' }
])
</script>

<style scoped lang="scss">
.model-summary {
  display: flex;
  align-items: center;
  gap: 0;
  min-height: 80px;
  margin-bottom: 15px;
  padding: 15px;
  background: #fff;
  border-radius: 2px;
}

.model-summary__identity {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 276px;
  min-width: 0;
  padding-right: 15px;
  border-right: 1px solid #f0f0f0;

}

.model-summary__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 44px;
  width: 44px;
  height: 44px;
  background: var(--el-color-primary-light-9);
  border-radius: 4px;

  .svg-icon {
    width: 28px;
    height: 28px;
  }

  img {
    width: 28px;
    height: 28px;
    object-fit: contain;
  }
}

.model-summary__name { min-width: 0; }

.model-summary__title {
  display: flex;
  align-items: center;
  gap: 8px;

  strong {
    min-width: 0;
    overflow: hidden;
    color: #303133;
    font-size: 14px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .el-tag { flex-shrink: 0; }
}

.model-summary__code {
  margin-top: 6px;
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-summary__facts {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, max-content));
  flex: 0 1 auto;
  min-width: 0;
}

.model-summary__fact {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  min-width: 0;
  padding: 0 42px;
  border-right: 1px solid #f0f0f0;

  &:last-child { border-right: 0; }

  .model-summary__icon {
    background: transparent;

    .svg-icon {
      width: 44px;
      height: 44px;
    }
  }
}

.model-summary__fact-text {
  min-width: 0;

  span,
  strong { display: block; }

  span {
    margin-bottom: 6px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
  }

  strong {
    overflow: hidden;
    color: #303133;
    font-family: PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif;
    font-size: 14px;
    font-weight: 500;
    line-height: 20px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.model-summary__actions {
  flex: 0 0 auto;
  margin-left: auto;
}

@media (max-width: 1180px) {
  .model-summary {
    flex-wrap: wrap;
    row-gap: 15px;
  }

  .model-summary__identity {
    flex: 1;
    border-right: 0;
  }

  .model-summary__facts {
    grid-template-columns: repeat(4, minmax(0, 1fr)) minmax(0, 1.55fr);
    flex-basis: 100%;
    order: 3;
  }

  .model-summary__fact {
    gap: 12px;
    padding: 0 12px;
  }
}

@media (max-width: 760px) {
  .model-summary__facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 15px 0;
  }

  .model-summary__fact {
    padding: 0;
    border-right: 0;

    &:last-child { grid-column: 1 / -1; }
  }
}

@media (max-width: 480px) {
  .model-summary__facts { grid-template-columns: minmax(0, 1fr); }
}
</style>
