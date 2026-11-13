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
  <div class="ds-step">
    <div class="ds-step__tip">
      第一步：请选择要接入的数据连接类型，您可以通过左侧分类浏览或在搜索框中输入关键词快速查找。
    </div>
    <div class="ds-selector">
      <div class="ds-selector__sidebar">
        <div class="category-list">
          <div
            class="category-item"
            :class="{ 'category-item--active': activeCategory === 'all' }"
            @mouseenter="handleCategoryMouseEnter('all')"
            @mouseleave="handleCategoryMouseLeave('all')"
            @click="handleCategoryClick('all')"
          >
            <img
              :src="
                getThemeCategoryIcon(
                  'all.svg',
                  activeCategory === 'all' || hoveredCategory === 'all'
                )
              "
              alt="全部"
              class="category-item__icon"
            />
            <span class="category-item__label">全部</span>
            <span class="category-item__count">({{ totalDatasourceCount }})</span>
          </div>

          <div
            v-for="cat in visibleCategories"
            :key="cat.key"
            class="category-item"
            :class="{ 'category-item--active': activeCategory === cat.key }"
            @mouseenter="handleCategoryMouseEnter(cat.key)"
            @mouseleave="handleCategoryMouseLeave(cat.key)"
            @click="handleCategoryClick(cat.key)"
          >
            <img
              :src="
                getThemeCategoryIcon(
                  cat.icon,
                  activeCategory === cat.key || hoveredCategory === cat.key
                )
              "
              :alt="cat.label"
              class="category-item__icon"
            />
            <span class="category-item__label">{{ cat.label }}</span>
            <span class="category-item__count">
              ({{ getDatasourceTypesByCategory(cat.key).length }})
            </span>
          </div>
          <div v-if="visibleCategories.length === 0" class="ds-empty">
            暂无数据库分类
          </div>
        </div>
      </div>
      <div ref="rightPanelRef" class="ds-selector__main">
        <!-- 搜索框固定渲染，避免筛选结果变化时被销毁并丢失焦点 -->
        <div class="ds-search">
          <el-input
            v-model="datasourceSearchKeyword"
            placeholder="搜索数据源类型，如 MySQL、Oracle"
            clearable
            class="ds-search__input"
            :prefix-icon="Search"
          />
        </div>
        <div
          v-for="cat in rightVisibleCategories"
          :key="cat.key"
          class="category-group"
        >
          <div :id="`category-${cat.key}`" class="category-group__header">
            <span class="category-group__title">{{ cat.label }}</span>
          </div>
          <div class="ds-grid">
            <div
              v-for="item in getDatasourceTypesByCategory(cat.key)"
              :key="item.key"
              class="ds-card"
              :class="{ 'ds-card--selected': modelValue === item.value }"
              @click="handleSelectDatasourceType(item)"
            >
              <div class="ds-card__check" v-if="modelValue === item.value">
                <el-icon><CircleCheckFilled /></el-icon>
              </div>
              <div class="ds-card__header">
                <div class="ds-card__icon">
                  <img :src="getDatasourceIcon(item.value)" :alt="item.label" />
                </div>
                <div class="ds-card__title">{{ item.label }}</div>
              </div>
              <div class="ds-card__desc">{{ item.description }}</div>
            </div>
          </div>
        </div>
        <div
          v-if="rightVisibleCategories.length === 0"
          class="ds-empty ds-empty--large"
        >
          暂无数据连接类型
        </div>
      </div>
    </div>
    <div class="ds-step__footer">
      共 {{ totalDatasourceCount }} 种数据连接类型
    </div>
  </div>
</template>

<script setup name="DatasourceTypeSelector">
import { computed, ref, watch, onMounted, onUnmounted, nextTick } from "vue";

import { Search, CircleCheckFilled } from "@element-plus/icons-vue";

import { DatasourceConfig, config } from "@/components/Datasource/config";
import { getDatasourceTypes } from "@/components/Datasource/utils";
import {
  isDatasourceEngineCategory,
  isDatasourceEngineType,
} from "@/components/Datasource/typeFilter";
import { getDatasourceIcon } from "@/utils/datasource";
import { getAssetsFile } from '@/utils/anivia';


const hoveredCategory = ref(null);

/**
 * 根据当前主题与高亮状态解析分类图标地址。
 * @param {string} iconPath 分类图标路径。
 * @param {boolean} [highlighted=false] 是否使用高亮图标。
 * @returns {string} 主题图标地址。
 */
const getThemeCategoryIcon = (iconPath, highlighted = false) => {
  const fileName = String(iconPath || "").split("/").pop();
  const relativePath = highlighted
    ? `common/icons/data-source-category/active/${fileName}`
    : `common/icons/data-source-category/${fileName}`;
  return getAssetsFile(relativePath);
};

/** 记录鼠标所在分类，用于切换高亮图标。 */
const handleCategoryMouseEnter = (categoryKey) => {
  hoveredCategory.value = categoryKey;
};

/** 鼠标离开当前分类时清除悬停状态。 */
const handleCategoryMouseLeave = (categoryKey) => {
  if (hoveredCategory.value === categoryKey) hoveredCategory.value = null;
};
const props = defineProps({
  modelValue: {
    type: String,
    default: "",
  },
});

const emit = defineEmits(["update:modelValue"]);

const DATASOURCE_FLAG = "datasource";

const activeCategory = ref("all");
const categorySearchKeyword = ref("");
const datasourceSearchKeyword = ref("");
const rightPanelRef = ref(null);
const isClickScrolling = ref(false);
const isInitializing = ref(true);
const isUserSelecting = ref(false);
let observer = null;

const allDatasourceTypes = getDatasourceTypes(DATASOURCE_FLAG);

const selectableDatasourceTypes = computed(() =>
  allDatasourceTypes.filter((type) => !isDatasourceEngineType(type))
);

const visibleCategories = computed(() => {
  let categories = DatasourceConfig.filter((cat) => {
    if (isDatasourceEngineCategory(cat)) return false;
    return Object.values(cat.types).some((type) => {
      if (type.disabled) return false;
      const rules = config?.[DATASOURCE_FLAG];
      if (!rules) return true;
      return rules[type.key] !== 3;
    });
  });

  if (categorySearchKeyword.value) {
    const keyword = categorySearchKeyword.value.toLowerCase();
    categories = categories.filter(
      (cat) =>
        cat.label.toLowerCase().includes(keyword) ||
        cat.key.toLowerCase().includes(keyword)
    );
  }

  return categories;
});

const rightVisibleCategories = computed(() => {
  let categories = [...visibleCategories.value];

  // 右侧搜索时，只显示包含匹配数据源的分类
  if (datasourceSearchKeyword.value) {
    categories = categories.filter((cat) => {
      return getDatasourceTypesByCategory(cat.key).length > 0;
    });
  }

  return categories;
});

const totalDatasourceCount = computed(() => {
  return selectableDatasourceTypes.value.filter((item) => !item.disabled)
    .length;
});

/**
 * 按分类和关键词筛选可选连接类型，排除禁用项。
 * @param {string} categoryKey 分类标识。
 * @returns {Array<Object>} 可见的连接类型。
 */
function getDatasourceTypesByCategory(categoryKey) {
  let source = selectableDatasourceTypes.value;
  source = source.filter(
    (item) => item.category === categoryKey && !item.disabled
  );

  if (datasourceSearchKeyword.value) {
    const keyword = datasourceSearchKeyword.value.toLowerCase();
    source = source.filter(
      (item) =>
        item.label.toLowerCase().includes(keyword) ||
        item.key.toLowerCase().includes(keyword)
    );
  }

  return source;
}

/**
 * 清空搜索并滚动到所选分类，滚动期间暂停导航自动回写。
 * @param {string} category 分类标识。
 */
function handleCategoryClick(category) {
  activeCategory.value = category;
  if (category === "all") {
    categorySearchKeyword.value = "";
    datasourceSearchKeyword.value = "";
  } else {
    categorySearchKeyword.value = "";
    datasourceSearchKeyword.value = "";
  }

  isClickScrolling.value = true;
  const targetElement = document.getElementById(`category-${category}`);
  if (targetElement) {
    targetElement.scrollIntoView({ behavior: "smooth", block: "start" });
  } else if (category === "all") {
    if (rightPanelRef.value) {
      rightPanelRef.value.scrollTop = 0;
    }
  }
  setTimeout(() => {
    isClickScrolling.value = false;
  }, 500);
}

/**
 * 回传选中类型并标记主动选择，避免监听逻辑重复调整位置。
 * @param {Object} item 连接类型配置。
 */
function handleSelectDatasourceType(item) {
  activeCategory.value = item.category;
  isUserSelecting.value = true;
  emit("update:modelValue", item.value);
  nextTick(() => {
    isUserSelecting.value = false;
  });
}

/** 根据右侧当前可见的最上方分类同步左侧导航。 */
function handleScroll() {
  if (!rightPanelRef.value || isClickScrolling.value) return;

  const containerTop = rightPanelRef.value.getBoundingClientRect().top;
  const containerBottom = rightPanelRef.value.getBoundingClientRect().bottom;

  if (rightVisibleCategories.value.length === 0) return;

  let topmostVisibleCategory = rightVisibleCategories.value[0].key;
  let topmostPosition = Infinity;

  rightVisibleCategories.value.forEach((cat) => {
    const element = document.getElementById(`category-${cat.key}`);
    if (element) {
      const rect = element.getBoundingClientRect();
      if (rect.bottom > containerTop && rect.top < containerBottom) {
        if (rect.top < topmostPosition) {
          topmostPosition = rect.top;
          topmostVisibleCategory = cat.key;
        }
      }
    }
  });

  activeCategory.value = topmostVisibleCategory;
}

/** 监听分类分组可见性，自动更新导航并避开初始化及点击滚动过程。 */
function initIntersectionObserver() {
  if (!rightPanelRef.value) return;

  observer = new IntersectionObserver(
    (entries) => {
      if (isClickScrolling.value || isInitializing.value) return;

      if (
        rightPanelRef.value.scrollTop < 50 &&
        rightVisibleCategories.value.length > 0
      ) {
        activeCategory.value = rightVisibleCategories.value[0].key;
        return;
      }

      const visibleEntries = entries.filter((entry) => entry.isIntersecting);
      if (visibleEntries.length > 0) {
        visibleEntries.sort((a, b) => a.target.offsetTop - b.target.offsetTop);
        const categoryKey = visibleEntries[0].target.id.replace(
          "category-",
          ""
        );
        activeCategory.value = categoryKey;
      }
    },
    {
      root: rightPanelRef.value,
      threshold: 0.5,
      rootMargin: "-10% 0% -40% 0%",
    }
  );

  rightVisibleCategories.value.forEach((cat) => {
    const element = document.getElementById(`category-${cat.key}`);
    if (element) {
      observer.observe(element);
    }
  });
}

/** 筛选结果变化后断开旧观察器，等待 DOM 更新再绑定分类分组。 */
function reinitObserver() {
  if (observer) {
    observer.disconnect();
  }
  nextTick(() => {
    initIntersectionObserver();
  });
}

watch(
  () => props.modelValue,
  (newValue) => {
    if (isUserSelecting.value) return;

    if (newValue) {
      const type = selectableDatasourceTypes.value.find(
        (t) => t.value === newValue
      );
      if (type) {
        activeCategory.value = type.category;
        isClickScrolling.value = true;
        const targetElement = document.getElementById(
          `category-${type.category}`
        );
        if (targetElement) {
          targetElement.scrollIntoView({ behavior: "smooth", block: "start" });
        }
        setTimeout(() => {
          isClickScrolling.value = false;
        }, 500);
      }
    } else {
      // If modelValue is empty, set activeCategory to 'all'
      activeCategory.value = "all";
      if (rightPanelRef.value) {
        // Scroll to top if 'all' is selected
        rightPanelRef.value.scrollTop = 0;
      }
    }
  },
  { immediate: true }
);

watch(categorySearchKeyword, () => {
  reinitObserver();
});

watch(datasourceSearchKeyword, () => {
  reinitObserver();
});

onMounted(() => {
  nextTick(() => {
    if (rightVisibleCategories.value.length > 0 && !props.modelValue) {
      activeCategory.value = "all";
    }
    initIntersectionObserver();
    setTimeout(() => {
      isInitializing.value = false;
    }, 100);

    if (rightPanelRef.value) {
      rightPanelRef.value.addEventListener("scroll", handleScroll);
    }
  });
});

onUnmounted(() => {
  if (observer) {
    observer.disconnect();
  }
});
</script>

<style lang="scss" scoped>
// ==========================================
// 样式变量
// ==========================================
$font-family-base: PingFangSC;
$font-family-medium: PingFang SC;

$color-text-main: #333333;
$color-text-title: #000000;
$color-text-secondary: rgba(0, 0, 0, 0.65);
$color-text-muted: rgba(0, 0, 0, 0.45);
$color-primary: var(--qt-primary);
$color-primary-light: rgba(var(--qt-primary-rgb), 0.06);
$color-primary-alt: var(--qt-primary);

$border-light: rgba(232, 232, 232, 0.45);
$border-base: rgba(214, 218, 225, 0.45);

// ==========================================
// 样式混入 (Mixins)
// ==========================================
@mixin text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin font-style($size, $color, $weight: 400, $lh: normal) {
  font-family: if($weight == 500, $font-family-medium, $font-family-base);
  font-size: $size;
  font-weight: $weight;
  color: $color;
  line-height: $lh;
}

.ds-step {
  font-family: $font-family-base;

  &__tip {
    @include font-style(14px, $color-text-secondary, 400, 18px);
    margin: 0px auto 16px;
  }

  &__footer {
    @include font-style(14px, $color-text-muted, 400, 18px);
    margin: 16px auto 0;
  }
}

.ds-selector {
  display: flex;
  width: 100%;
  height: 496px;
  margin: 0 auto;
  border-radius: 2px;
  overflow: hidden;
  border: 1px solid $border-light;

  &__sidebar {
    width: 308px;
    background-color: #ffffff;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    scrollbar-width: thin;
    -ms-overflow-style: none;
  }

  &__main {
    position: relative;
    flex: 1;
    padding: 20px;
    border-left: 1px solid $border-base;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
  }
}

.category-list {
  flex: 1;
  padding: 20px 20px 0;
}

.category-item {
  display: flex;
  align-items: center;
  width: 268px;
  height: 40px;
  margin-bottom: 6px;
  padding: 0 10px 0 20px;
  border-radius: 2px;
  cursor: pointer;
  @include font-style(14px, $color-text-main, 400, 20px);
  transition: all 0.2s;
  @include text-ellipsis;

  &__label {
    flex: 1;
    margin-right: 8px;
    min-width: 0;
    @include text-ellipsis;
  }

  &__icon {
    margin-right: 12px;
    width: 18px;
    height: 18px;
    flex-shrink: 0;
    object-fit: contain;
  }

  &__count {
    @include font-style(14px, $color-text-muted, 400, 20px);
    flex-shrink: 0;
  }

  &:hover {
    background: $color-primary-light;
    color: $color-primary;
  }

  &--active {
    background: $color-primary-light;
    color: $color-primary;
    font-family: $font-family-medium;
    font-weight: 500;
  }
}

.ds-empty {
  @include flex-center;
  height: 100px;
  color: var(--el-text-color-secondary);
  font-size: 14px;

  &--large {
    height: 200px;
  }
}

.ds-search {
  position: absolute;
  top: 20px;
  right: 20px;
  z-index: 1;
  margin-bottom: 0;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;

  &__input {
    width: 300px;

    :deep(.el-input__wrapper) {
      height: 32px;
      border-radius: 2px;
      box-shadow: 0 0 0 1px rgba(214, 218, 225, 1) inset;
    }

    :deep(.el-input__inner) {
      font-size: 14px;
      line-height: 22px;
    }
  }
}

.category-group {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    min-height: 22px;
    margin: 6px 320px 24px 0;
    @include font-style(16px, $color-text-main, 500, 22px);
  }

  &__title {
    flex: 1;
    text-align: left;
  }
}

.ds-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.ds-card {
  position: relative;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  grid-template-rows: 20px 37px;
  column-gap: 14px;
  row-gap: 4px;
  width: 100%;
  height: 86px;
  padding: 13px 15px 12px;
  overflow: hidden;
  border: 1px solid $border-base;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $color-primary-alt;
    box-shadow: 0 2px 8px rgba(var(--qt-primary-rgb), 0.15);
  }

  &--selected {
    border: 1px solid $border-light;
    background-image: linear-gradient(
      180deg,
      rgba(255, 255, 255, 0) 0,
      rgba(var(--qt-primary-rgb), 0.05) 100%
    );
  }

  &__check {
    position: absolute;
    top: 12px;
    right: 11px;
    width: 14px;
    height: 14px;
    border: 4px solid $color-primary-alt;
    border-radius: 50%;
    background-color: #ffffff;
    color: transparent;
    font-size: 0;
    line-height: 0;

    :deep(.el-icon) {
      display: none;
    }
  }

  &__header {
    display: contents;
  }

  &__icon {
    grid-row: 1 / span 2;
    align-self: center;
    width: 36px;
    height: 36px;
    font-size: 36px;
    flex-shrink: 0;
    @include flex-center;

    img {
      width: 36px !important;
      height: 36px !important;
      object-fit: contain;
    }
  }

  &__title {
    grid-column: 2;
    width: 100%;
    height: 20px;
    @include text-ellipsis;
    @include font-style(14px, $color-text-title, 400, 20px);
  }

  &__desc {
    grid-column: 2;
    width: 100%;
    height: 37px;
    @include font-style(12px, $color-text-secondary, 400, 18px);
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
</style>
