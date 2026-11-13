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
  <div class="steps-inner" :class="{ 'is-dialog': isDialog }">
    <ul
        class="zl-step"
        :class="{ 'is-dialog': isDialog, 'is-readonly': !clickable }"
        :style="{ width: '100%' }"
    >
      <li
          v-for="(step, index) in steps"
          :key="index"
          :id="'li' + index"
          :class="getStepClass(index)"
          :style="getStepStyle(index)"
          @click="handleStepClick(index)"
      >
        <div class="step-circle" :class="getCircleClass(index)">
          <span>
            {{ index + 1 }}
          </span>
        </div>

        <span class="step-name">{{ step.title }}</span>

        <div
            v-if="index < steps.length - 1"
            :id="'div' + index"
            class="jiao"
            :class="getArrowClass(index)"
        ></div>
      </li>
    </ul>
  </div>
</template>

<script setup>
const props = defineProps({
  steps: {
    type: Array,
    required: true,
    default: () => [],
  },
  activeStep: {
    type: Number,
    default: 0,
  },
  isDialog: {
    type: Boolean,
    default: false,
  },
  clickable: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(["update:activeStep", "change"]);

/**
 * 获取步骤条背景容器的样式类
 * @param {number} index 当前步骤的索引
 */
const getStepClass = (index) => {
  return {
    statusEnd: props.activeStep === index,
    prevStep: index < props.activeStep,
    cur: index > props.activeStep,
    "is-readonly": !props.clickable,
  };
};

/**
 * 获取步骤条背景容器的内联样式（计算宽度和层级）
 * @param {number} index 当前步骤的索引
 */
const getStepStyle = (index) => {
  return {
    width: 100 / props.steps.length + "%",
    zIndex: props.steps.length - index,
  };
};

/**
 * 获取步骤序号圆圈的样式类
 * @param {number} index 当前步骤的索引
 */
const getCircleClass = (index) => {
  return {
    active: props.activeStep === index,
    prev: index < props.activeStep,
  };
};

/**
 * 获取步骤条箭头的样式类
 * @param {number} index 当前步骤的索引
 */
const getArrowClass = (index) => {
  return {
    titleItem: props.activeStep === index,
    prevJiao: index < props.activeStep,
    jiaoActive: index > props.activeStep,
  };
};

/**
 * 处理步骤条点击事件
 * @param {number} index 当前点击的步骤索引
 */
const handleStepClick = (index) => {
  if (!props.clickable) return;
  emit("update:activeStep", index);
  emit("change", index);
};
</script>

<style lang="scss" scoped>
@mixin font-base {
  font-family: PingFangSC, "PingFang SC", sans-serif;
  text-align: left;
  font-style: normal;
}

.steps-inner {
  padding: 0 10px 0 20px;
  display: flex;
  width: auto;
  color: #303133;
  transition: 0.3s;
  transform: translateZ(0);

  &.is-dialog {
    padding: 0;
  }

  &::-webkit-scrollbar {
    height: 5px;
  }

  .zl-step {
    list-style: none;
    width: 100%;
    height: 20px;
    padding: 0;
    margin: 20px auto;
    cursor: pointer;
    display: flex;
    align-items: flex-end;

    &.is-readonly {
      cursor: default;
    }

    &.is-dialog {
      margin: 15px 0;
    }

    li {
      position: relative;
      flex: 1;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #d7d8da;
      color: #666;
      font-weight: 500;
      transition: background 0.3s;

      &:first-child:not(:last-child) {
        clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 50%, calc(100% - 20px) 100%, 0 100%);
      }

      &:not(:first-child):not(:last-child) {
        clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 50%, calc(100% - 20px) 100%, 0 100%, 20px 50%);
      }

      &:last-child:not(:first-child) {
        clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%, 20px 50%);
      }

      &:first-child:last-child {
        clip-path: polygon(0 0, 100% 0, 100% 100%, 0 100%);
      }

      &:not(:first-child) {
        margin-left: -2px;
      }

      &.statusEnd {
        background: var(--qt-primary);
        color: var(--qt-text-inverse);
      }

      &.prevStep {
        background: rgba(var(--qt-primary-rgb), 0.06);
        font-weight: normal;
        font-size: 14px;
        color: var(--qt-primary);
      }

      &.cur {
        background: rgba(214, 218, 225, 0.2);
        color: #a8abb2;
      }
    }
  }

  .step-circle {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 7px;
    border: 1px solid #a8abb2;
    color: #a8abb2;
    flex-shrink: 0;
    transition: all 0.3s;
    line-height: 1;

    &.active {
      background: var(--qt-primary);
      color: var(--qt-text-inverse);
      border-color: var(--qt-surface-background);
    }

    &.prev {
      background: var(--qt-primary);
      border-color: var(--qt-primary);
      color: var(--qt-text-inverse);
    }

    span {
      @include font-base;
      display: inline-block;
      font-weight: 400;
      font-size: 14px;
      line-height: 20px;
    }
  }

  .step-name {
    @include font-base;
    font-weight: 500;
    font-size: 14px;
    line-height: 20px;
  }
}

@media (max-width: 1260px) {
  .steps-inner {
    .step-circle {
      width: 18px;
      height: 18px;
      font-size: 12px;
      margin-right: 4px;
      line-height: 16px;

      span {
        padding-top: 1px;
      }
    }
    .step-name {
      font-size: 12px;
    }
  }
}

@media (max-width: 960px) {
  .steps-inner {
    .step-circle {
      width: 13px;
      height: 13px;
      font-size: 10px;
      margin-right: 4px;
      line-height: 11px;
    }
    .step-name {
      font-size: 10px;
    }
  }
}
</style>
