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
  <div class="qt-search-bar">
    <el-form
      ref="formRef"
      :model="props.params"
      :inline="true"
      @submit.prevent
      v-bind="props.config?.form"
      :style="{ flexWrap: store.expand && !measuring ? 'wrap' : 'nowrap' }"
    >
      <el-form-item
        :label="item.label"
        :prop="item.prop"
        v-for="(item, index) in props.items"
        :key="item.prop"
        :ref="(element) => setItemRef(element, index)"
        v-bind="getFormItemProps(item)"
        v-show="measuring || store.expand || index < store.visibleCount"
      >
        <!-- 输入框 -->
        <el-input
          class="search-content"
          v-if="item.component.is == 'input'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请输入${item.label}`"
          v-bind="item.component"
          @keyup.enter="handleQueryClick"
        />
        <!-- 下拉框 -->
        <el-select
          class="search-content"
          v-if="item.component.is == 'select' && item.prop != 'datasourceType'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
        >
          <el-option
            v-for="(option, index) in item.component.options"
            :key="index"
            v-bind="option"
          />
        </el-select>

        <!-- 时间选择器 -->
        <el-date-picker
          class="search-content"
          v-if="item.component.is == 'date-picker'"
          v-model="props.params[item.prop]"
          :type="item.component.type || 'date'"
          clearable
          :placeholder="`请选择${item.label}`"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          v-bind="item.component"
        />

        <!-- 树形选择框 -->
        <el-tree-select
          class="search-content"
          v-if="item.component.is == 'tree-select'"
          v-model="props.params[item.prop]"
          clearable
          :placeholder="`请选择${item.label}`"
          v-bind="item.component"
        />
      </el-form-item>
      <el-form-item ref="actionRef" class="search-btns">
        <el-button plain type="primary" @click="handleQueryClick">
          <i class="iconfont-mini icon-a-zu22377 mr5"></i>查询
        </el-button>
        <el-button @click="handleResetClick">
          <i class="iconfont-mini icon-a-zu22378 mr5"></i>重置
        </el-button>
        <el-button
          plain
          type="primary"
          v-show="measuring || hasOverflow"
          @click="store.expand = !store.expand"
          class="extend-btn"
        >
          <svg-icon v-if="store.expand" icon-class="toggle" />
          <svg-icon v-else icon-class="expand" />
          <span>{{ store.expand ? '折叠' : '展开' }}</span>
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup name="QtSearchBar">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useResizeObserver } from '@vueuse/core';

const props = defineProps({
  params: {
    type: Object,
    required: true
  },
  config: {
    type: Object,
    default: () => {
      return {};
    }
  },
  items: {
    type: Array,
    required: true
  },
  tableRef: {
    type: Object,
    default: () => {
      return {};
    }
  }
});

const emits = defineEmits(['query', 'reset']);

const formRef = ref(null);
const actionRef = ref(null);
// 保存每个搜索项对应的真实 DOM，用于测量 label、控件和外边距的总宽度
const itemRefs = ref([]);
// 测量时临时展示全部搜索项，保持不换行以读取每项完整宽度
const measuring = ref(true);

const store = reactive({
  expand: props.config?.defaultExpand === true,
  // 折叠状态下，根据当前容器宽度计算出的最大可见搜索项数量
  visibleCount: props.items.length
});

const hasOverflow = computed(() => store.visibleCount < props.items.length);

// 搜索项宽度只有在全部可见时才能准确获取，测量后缓存供容器缩放时重复计算
let itemWidthCache = [];
// 分别缓存不带展开按钮和带展开按钮时的完整按钮区宽度
let actionWidth = 0;
let expandedActionWidth = 0;
let resizeFrame = 0;
// 给每轮异步测量编号，新测量开始或组件销毁后，旧测量不再更新布局。
let measurementVersion = 0;
let actionCheckVersion = 0;

// Element Plus 组件 ref 指向组件实例，原生元素 ref 则直接指向 DOM
function getElement(target) {
  return target?.$el || target || null;
}

function setItemRef(element, index) {
  itemRefs.value[index] = getElement(element);
}

// 获取元素占用的真实宽度，包含 el-form-item / el-button 的左右外边距
function getOuterWidth(element) {
  if (!element) return 0;

  const style = window.getComputedStyle(element);
  return (
    element.getBoundingClientRect().width +
    (parseFloat(style.marginLeft) || 0) +
    (parseFloat(style.marginRight) || 0)
  );
}

function getFormAvailableWidth() {
  const formElement = getElement(formRef.value);
  if (!formElement) return 0;

  const style = window.getComputedStyle(formElement);
  const formWidth =
    formElement.clientWidth -
    parseFloat(style.paddingLeft || 0) -
    parseFloat(style.paddingRight || 0);
  const barElement = formElement.closest('.qt-search-bar');
  const visibleWidth = Math.max(
    0,
    Math.min(barElement?.getBoundingClientRect().right || Infinity, window.innerWidth) -
      formElement.getBoundingClientRect().left
  );

  // 页面横向内容可能撑大 form，取实际可见区域，避免按钮被算进屏幕外
  return Math.min(formWidth, visibleWidth);
}

// 部分业务页面给表单设置了 column-gap，计算时需要一并计入
function getFormColumnGap() {
  const formElement = getElement(formRef.value);
  if (!formElement) return 0;

  const columnGap = window.getComputedStyle(formElement).columnGap;
  return columnGap === 'normal' ? 0 : parseFloat(columnGap || 0);
}

function getActionWidth(includeExpand = true) {
  const actionElement = getElement(actionRef.value);
  if (!actionElement) return 0;

  const content = actionElement.querySelector('.el-form-item__content');
  const buttons = [...(content?.querySelectorAll('.el-button') || [])].filter(
    (button) => includeExpand || !button.classList.contains('extend-btn')
  );
  const style = window.getComputedStyle(actionElement);
  const contentStyle = content && window.getComputedStyle(content);
  const gap = contentStyle?.columnGap === 'normal' ? 0 : parseFloat(contentStyle?.columnGap || 0);
  const buttonWidth = buttons.reduce((total, button) => total + getOuterWidth(button), 0);

  const horizontalSpace = (elementStyle) =>
    [
      'marginLeft',
      'marginRight',
      'paddingLeft',
      'paddingRight',
      'borderLeftWidth',
      'borderRightWidth'
    ].reduce((total, key) => total + (parseFloat(elementStyle?.[key]) || 0), 0);

  // 使用按钮总宽度和盒模型间距，避免按钮区换行或伸缩后的宽度干扰测量。
  return (
    buttonWidth +
    Math.max(buttons.length - 1, 0) * gap +
    horizontalSpace(style) +
    horizontalSpace(contentStyle)
  );
}

function calculateVisibleCount() {
  const itemCount = props.items.length;
  const availableWidth = getFormAvailableWidth();
  const columnGap = getFormColumnGap();

  if (!itemCount) {
    store.visibleCount = 0;
    store.expand = false;
    return;
  }

  // 页面尚未显示或条件还没量完时，保留上一轮结果，等拿到真实宽度再调整。
  if (!availableWidth || itemWidthCache.length !== itemCount) return;

  store.visibleCount = getVisibleSearchCount({
    itemWidths: itemWidthCache,
    availableWidth,
    actionWidth,
    expandedActionWidth,
    columnGap
  });

  // 所有搜索项都能与完整按钮区共处一行时，不再显示无意义的展开按钮
  if (store.visibleCount === itemCount) store.expand = false;
}

function getVisibleSearchCount({
  itemWidths,
  availableWidth,
  actionWidth,
  expandedActionWidth,
  columnGap = 0
}) {
  // clientWidth 会取整，预留 1px，避免小数像素让最后一个按钮越界。
  const usableWidth = Math.max(0, availableWidth - 1);
  const allWidth = itemWidths.reduce((total, width) => total + width + columnGap, actionWidth);
  // 全部放得下时不需要“展开”按钮，先按最省空间的按钮组合尝试。
  if (allWidth <= usableWidth) return itemWidths.length;

  let usedWidth = expandedActionWidth;
  // 放不下就先留出“展开”按钮的位置，再从左到右决定保留几个条件。
  let count = 0;
  for (const width of itemWidths) {
    if (usedWidth + columnGap + width > usableWidth) break;
    usedWidth += columnGap + width;
    count++;
  }
  return count;
}

async function ensureActionVisible() {
  // 宽度计算后再看按钮真实位置，避免页面自己的边距或按钮样式把操作区挤出去。
  const version = ++actionCheckVersion;
  while (!store.expand && store.visibleCount > 0) {
    await nextTick();
    if (version !== actionCheckVersion || measuring.value) return;
    const formElement = getElement(formRef.value);
    const actionElement = getElement(actionRef.value);
    const content = actionElement?.querySelector('.el-form-item__content');
    const buttons = [...(content?.querySelectorAll('.el-button') || [])].filter(
      (button) => window.getComputedStyle(button).display !== 'none'
    );
    if (!formElement || !buttons.length) return;
    const firstItem = itemRefs.value[0]?.getBoundingClientRect();
    const firstButton = buttons[0].getBoundingClientRect();
    const lastButton = buttons[buttons.length - 1].getBoundingClientRect();
    const barRight = formElement.closest('.qt-search-bar')?.getBoundingClientRect().right;
    const visibleRight = Math.min(barRight || Infinity, window.innerWidth);
    const actionWrapped = firstItem && actionElement.getBoundingClientRect().top > firstItem.bottom;
    const buttonsWrapped = lastButton.top >= firstButton.bottom;
    if (lastButton.right <= visibleRight && !actionWrapped && !buttonsWrapped) return;
    // 测量受业务样式影响时，逐项收起到按钮完整落在第一行，不再回退到固定三项。
    store.visibleCount--;
  }
}

async function measureLayout() {
  const version = ++measurementVersion;
  // v-show 隐藏后的元素宽度为 0，因此测量前需要临时展示所有搜索项和展开按钮
  measuring.value = true;
  itemRefs.value.length = props.items.length;
  await nextTick();
  if (version !== measurementVersion) return;

  itemWidthCache = props.items.map((_, index) => getOuterWidth(itemRefs.value[index]));

  // 两种按钮组合都要量：全部展开得下时，不应额外预留“展开”按钮的空间。
  actionWidth = getActionWidth(false);
  expandedActionWidth = getActionWidth();

  calculateVisibleCount();
  measuring.value = false;
  await ensureActionVisible();
}

// ResizeObserver 可能在一次布局变化中连续触发，合并到下一帧只计算一次
function scheduleCalculate() {
  cancelAnimationFrame(resizeFrame);
  resizeFrame = requestAnimationFrame(() => {
    calculateVisibleCount();
    ensureActionVisible();
  });
}

useResizeObserver(formRef, scheduleCalculate);

// 搜索配置发生变化后，控件或 label 宽度可能改变，需要重新获取真实宽度
watch(
  () => props.items,
  () => measureLayout(),
  { deep: true }
);

// 挂载后才能量到真实宽度，首次计算不能在页面还没渲染时进行。
onMounted(measureLayout);

onBeforeUnmount(() => {
  // 取消排队中的计算，并让正在等待 DOM 更新的旧测量失效。
  measurementVersion++;
  actionCheckVersion++;
  cancelAnimationFrame(resizeFrame);
});

// 过滤form参数
function getFormItemProps(item) {
  const { component, ...data } = item;
  return data;
}

// 查询
function handleQueryClick() {
  emits('query');
  if (typeof props.tableRef?.getList === 'function') props.tableRef.getList();
}

// 重置
function handleResetClick() {
  formRef.value.resetFields();
  emits('reset');
  if (typeof props.tableRef?.resetQuery === 'function') props.tableRef.resetQuery();
}
</script>

<style lang="scss" scoped>
// 搜索项本身不压缩，折叠态由可见数量控制宽度，避免按钮被挤到第二行。
.qt-search-bar :deep(.el-form) {
  display: flex;
  align-items: flex-start;
}

.qt-search-bar :deep(.el-form > .el-form-item) {
  flex: none;
}

.search-btns :deep(.el-form-item__content) {
  flex-wrap: nowrap;
  white-space: nowrap;
}

.search-content {
  width: 210px;
}

.search-btns {
  .el-button {
    height: 30px;
    padding: 8px 11px;
    font-size: 12px;
  }
}
.extend-btn {
  .svg-icon {
    font-size: 12px;
    margin-right: 6px;
  }
}
</style>
