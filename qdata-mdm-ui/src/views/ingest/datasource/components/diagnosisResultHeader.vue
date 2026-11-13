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
  @component DiagnosisResultHeader
  @description 数据源诊断结果头部组件。用于展示数据源连接性测试或诊断的结果汇总，包括整体状态（通过、警告、错误、跳过）及具体统计数量。
-->
<template>
  <div class="diagnosis-result-header" role="status" aria-live="polite">
    <div class="header-icon-wrapper">
      <el-icon color="var(--qt-primary)" size="18px">
        <InfoFilled />
      </el-icon>
    </div>
    
    <span class="header-title">诊断结果：</span>
    
    <div class="counts" v-if="counts && counts.length > 0">
      <template v-for="item in counts" :key="item.type">
        <div v-if="item.show" class="count-wrapper">
          <span class="count-label">{{ item.label }}</span>
          <span class="count-number" :class="item.type.toLowerCase()">{{ item.count }}</span>
          <span class="count-unit">个{{ item.isLast ? '' : '，' }}</span>
        </div>
      </template>
    </div>

    <!-- 整体状态 -->
    <div class="overall-status-wrapper">
      <div v-if="overallStatus" :class="['value-tag', overallStatus.toLowerCase()]">
        {{ getDiagnosisLabel }}
      </div>
      <span v-else style="margin-left: 10px; font-size: 14px; color: #363636;">-</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';

import iconPassSvg from "@/assets/modules/ingest/icons/datasource/success.svg";
import iconWarnSvg from "@/assets/modules/ingest/icons/datasource/warning.svg";
import iconFailSvg from "@/assets/modules/ingest/icons/datasource/error.svg";

const props = defineProps({
  overallTitle: {
    type: String,
    default: ''
  },
  overallStatus: {
    type: String,
    default: ''
  },
  counts: {
    type: Array,
    default: () => []
  }
});

const getDiagnosisDictOptions = computed(() => {
  return [
    { label: '通过', value: 'PASS', elTagType: 'success', elTagClass: 'pass', isSpan: true, icon: iconPassSvg },
    { label: '警告', value: 'WARN', elTagType: 'warning', elTagClass: 'warn', isSpan: true, icon: iconWarnSvg },
    { label: '错误', value: 'FAIL', elTagType: 'danger', elTagClass: 'fail', isSpan: true, icon: iconFailSvg },
    { label: '跳过', value: 'SKIPPED', elTagType: 'info', elTagClass: 'skipped', isSpan: true, icon: iconWarnSvg },
    { label: '诊断中', value: 'DIAGNOSING', elTagType: 'info', elTagClass: 'diagnosing', isSpan: true }
  ];
});

const getDiagnosisLabel = computed(() => {
  const option = getDiagnosisDictOptions.value.find(item => item.value === props.overallStatus);
  return option ? option.label : '';
});

</script>

<style lang="scss" scoped>
.diagnosis-result-header {
  width: 100%;
  height: 40px;
  background-color: var(--qt-primary-light);
  border-radius: 2px;
  display: flex;
  align-items: center;

  .header-icon-wrapper {
    height: 24px;
    width: 24px;
    margin-left: 20px;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .header-title {
    height: 20px;
    color: #363636;
    font-size: 14px;
    line-height: 20px;
    margin-left: 10px;
  }

  .counts {
    display: flex;
    align-items: center;
    margin-left: 10px;

    .count-wrapper {
      display: flex;
      align-items: center;
      margin-right: 4px;

      .count-label {
        height: 20px;
         color: #333333 ;
        font-size: 14px;
        line-height: 20px;
      }

      .count-number {
        height: 20px;
        font-size: 14px;
        line-height: 20px;
        margin: 0 4px;

        &.pass {
          color: #12b369;
        }
        &.warn {
          color: #ffbb00;
        }
        &.fail {
          color: #ff5353;
        }
        &.skipped {
          color: #333333;
        }
      }

      .count-unit {
        height: 20px;
        color: #333333;
        font-size: 14px;
        line-height: 20px;
      }
    }
  }

  .overall-status-wrapper {
    margin-left: 20px;
    display: flex;
    align-items: center;

    .value-tag {
      width: 67px;
      height: 22px;
      display: flex;
      justify-content: center;
      align-items: center;
      font-family: PingFang SC;
      font-weight: 400;
      font-size: 13px;
      color: var(--qt-text-inverse);
      border-radius: 10px 10px 10px 0;

      &.pass {
        background: #12b369;
      }

      &.warn {
        background: #ffbb00;
      }

      &.fail {
        background: #ff5353;
      }

      &.skipped {
        background: #909399;
      }

      &.diagnosing {
        background: #f0eef9;
        color: #8737a3;
      }
    }
  }
}
</style>
