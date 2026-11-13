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
  <el-popover
    v-model:visible="visible"
    placement="bottom-start"
    :width="datasourcePopoverWidth"
    trigger="click"
    :disabled="props.disabled"
  >
    <div class="datasource-selector">
      <el-input
        v-model="searchKeyword"
        :placeholder="
          shouldShowDatasourceColumn ? '请输入数据连接名称' : '请输入连接类型'
        "
        clearable
        class="search-input"
        :prefix-icon="Search"
      />
      <div class="columns-container">
        <div class="selector-column category-column">
          <div class="column-content">
            <div
              class="datasource-list-item category-item"
              :class="{ active: !selectedCategory }"
              @mouseenter="handleCategoryMouseEnter('')"
              @mouseleave="handleCategoryMouseLeave('')"
              @click="selectAllCategories"
            >
              <img
                :src="getCategoryIconUrl('all.svg', !selectedCategory || hoveredCategory === '')"
                class="category-icon"
                alt=""
              />
              <span class="category-label">全部</span>
              <span class="category-count">{{ categoryTotalCount }}</span>
            </div>
            <div
              v-for="category in filteredCategoryList"
              :key="category.key"
              class="datasource-list-item category-item"
              :class="{ active: selectedCategory === category.key }"
              @mouseenter="handleCategoryMouseEnter(category.key)"
              @mouseleave="handleCategoryMouseLeave(category.key)"
              @click="selectCategory(category.key)"
            >
              <img
                :src="getCategoryIconUrl(category.icon, selectedCategory === category.key || hoveredCategory === category.key)"
                class="category-icon"
                alt=""
              />
              <span class="category-label">{{ category.label }}</span>
              <span class="category-count">{{
                getCategoryCount(category.key)
              }}</span>
            </div>
          </div>
        </div>
        <div class="selector-column type-column">
          <div class="column-content">
            <div
              class="datasource-list-item type-item"
              :class="{ active: !selectedType, disabled: false }"
              @click="selectAllTypes"
            >
              <el-radio :model-value="selectedType" value="" disabled />
              <span class="type-label">全部</span>
              <span class="category-count" v-if="!props.hideTypeCount">{{
                typeDatasourceCount
              }}</span>
            </div>
            <div
              v-for="type in typeList"
              :key="type.key"
              class="datasource-list-item type-item"
              :class="{
                active: selectedType === type.key,
                disabled: type.disabled,
              }"
              @click="!type.disabled && selectType(type.key)"
            >
              <el-radio
                :model-value="selectedType"
                :value="type.key"
                :disabled="type.disabled"
              />
              <img :src="getDatasourceIcon(type.key)" class="type-item-icon" />
              <span class="type-label">{{ type.label }}</span>
              <span
                class="category-count"
                v-if="!props.hideTypeCount && shouldShowDatasourceColumn"
              >
                {{ getDatasourceCountByType(type.key) }}
              </span>
            </div>
            <div v-if="typeList.length === 0" class="empty-state">
              <img
                class="empty-img"
                src="@/assets/common/images/empty-state/noData.png"
                alt=""
              />
              <div class="empty-desc">暂无数据类型</div>
            </div>
          </div>
          <div class="column-footer">
            {{ selectedCategory ? "当前分类" : "共" }} 有
            {{ typeList.length }} 种连接类型
          </div>
        </div>
        <div
          class="selector-column datasource-column"
          v-if="shouldShowDatasourceColumn"
        >
          <div class="column-content">
            <div
              v-for="source in datasourceList"
              :key="source.id"
              class="datasource-list-item datasource-item"
              :class="{
                active: isDatasourceSelected(source.id),
                disabled: source.disabled,
              }"
              @click="!source.disabled && selectDatasource(source)"
            >
              <el-checkbox
                v-if="props.multiple"
                :model-value="isDatasourceSelected(source.id)"
                :disabled="source.disabled"
                @click.stop
                @change="selectDatasource(source)"
              />
              <el-radio
                v-else
                :model-value="props.modelValue"
                :value="source.id"
                disabled
              />
              <img
                :src="getDatasourceIcon(source.datasourceType)"
                class="type-icon"
              />
              <span class="datasource-label">{{
                getDatasourceName(source)
              }}</span>
            </div>
            <div v-if="datasourceList.length === 0" class="empty-state">
              <img
                class="empty-img"
                src="@/assets/common/images/empty-state/noData.png"
                alt=""
              />
              <div class="empty-desc">暂无数据源</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <template #reference>
      <div
        v-if="props.multipleTagDisplay && props.multiple"
        v-bind="$attrs"
        class="datasource-tag-input el-input__wrapper"
        :class="{ 'is-disabled': props.disabled }"
        role="button"
        :tabindex="props.disabled ? -1 : 0"
        :aria-disabled="props.disabled"
        :aria-label="selectedSources.map(getDatasourceName).join('、') || props.placeholder || '请选择数据连接'"
        @keydown.enter.prevent="!props.disabled && (visible = !visible)"
        @keydown.space.prevent="!props.disabled && (visible = !visible)"
      >
        <qt-tag-group
          v-if="selectedTagItems.length"
          :items="selectedTagItems"
          :max-count="selectedTagItems.length"
          type="none"
          is-datasource
          size="small"
          class="datasource-selected-tags"
        />
        <span v-else class="datasource-tag-placeholder">{{ props.placeholder || '请选择数据连接' }}</span>
        <el-icon
          v-if="props.clearable && selectedSources.length && !props.disabled"
          class="datasource-tag-clear"
          aria-label="清空数据连接"
          @click.stop="selectAllDatasources"
        ><CircleClose /></el-icon>
        <el-icon v-else class="datasource-tag-arrow"><component :is="visible ? ArrowUp : ArrowDown" /></el-icon>
      </div>
      <el-input
        v-else
        v-bind="$attrs"
        v-model="displayValue"
        :placeholder="props.placeholder || '请选择数据连接'"
        readonly
        :disabled="props.disabled"
        :clearable="props.clearable"
        :suffix-icon="visible ? 'ArrowUp' : 'ArrowDown'"
        @clear="selectAllDatasources"
      >
        <template #prefix>
          <img
            v-if="selectedIcon"
            :src="selectedIcon"
            class="prefix-icon"
            alt=""
          />
        </template>
      </el-input>
    </template>
  </el-popover>
</template>

<script setup name="DatasourceList">
import { reactive, ref, computed, watch } from "vue";
import { getDatasourceData, getAvailableDatasource } from "./utils";
import {
  DatasourceConfig,
  getDatasourceIcon,
  config,
} from "./config";
import {
  createDatasourceTypeFilter,
  isDatasourceTypeAllowed,
  isDatasourceTypeHiddenByFlag,
  getTypeConfig,
} from "./typeFilter";
import {
  getDatasourceTypeSelectorCategories,
  getDatasourceTypeSelectorTypes,
} from "./typeOptions";
import useUserStore from "@/store/system/user";
import { ArrowDown, ArrowUp, CircleClose, Search } from "@element-plus/icons-vue";
import { getAssetsFile } from '@/utils/anivia';
import QtTagGroup from "@/components/QtTagGroup/index.vue";

/**
 * 组件选项配置
 */
defineOptions({
  inheritAttrs: false,
});

/**
 * 组件属性定义
 */
const props = defineProps({
  modelValue: {
    type: [String, Number, Array],
    default: "",
  },
  multiple: {
    type: Boolean,
    default: false,
  },
  multipleTagDisplay: {
    type: Boolean,
    default: false,
  },
  flag: {
    type: String,
    default: "",
  },
  data: {
    type: Array,
    default: undefined,
  },
  project: {
    type: Boolean,
  },
  placeholder: {
    type: String,
    default: "",
  },
  params: {
    type: Object,
    default: () => ({}),
  },
  datasourceType: {
    type: [String, Number, Array],
    default: "",
  },
  // 只控制是否可选择，不影响 datasourceType 定义的显示范围；不传时保持原行为
  selectableDatasourceType: {
    type: [String, Number, Array],
    default: "",
  },
  filterMethod: {
    type: Function,
    default: undefined,
  },
  listApi: {
    type: String,
    default: "",
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  clearable: {
    type: Boolean,
    default: false,
  },
  selectionMode: {
    type: String,
    default: "instance",
  },
  hideTypeCount: {
    type: Boolean,
    default: false,
  },
});

/**
 * 组件事件触发
 */
const emits = defineEmits(["update:modelValue", "change"]);

/**
 * 用户状态管理，用于获取项目信息
 */
const userStore = useUserStore();

/**
 * 组件响应式状态
 */
const visible = ref(false);
const selectedCategory = ref("");
const hoveredCategory = ref(null);
const selectedType = ref("");
const displayValue = ref("");
const searchKeyword = ref("");
const selectedSourceType = ref("");

/**
 * 判断是否需要显示数据源列
 * @returns {boolean}
 */
const shouldShowDatasourceColumn = computed(() => {
  return props.selectionMode !== "type";
});

/**
 * 根据选择模式计算弹出层宽度
 * @returns {number}
 */
const datasourcePopoverWidth = computed(() => {
  return shouldShowDatasourceColumn.value ? 780 : 480;
});

/**
 * 缓存数据源数据的存储
 */
const store = reactive({
  types: [],
  datasources: [],
});

/**
 * 数据源类型过滤器计算属性
 * @returns {Function}
 */
const datasourceTypeFilter = computed(() =>
  createDatasourceTypeFilter(props.datasourceType, getTypeConfig)
);

const selectableDatasourceTypeFilter = computed(() =>
  createDatasourceTypeFilter(props.selectableDatasourceType, getTypeConfig)
);

const hasSelectableDatasourceType = computed(() => {
  if (Array.isArray(props.selectableDatasourceType)) {
    return props.selectableDatasourceType.length > 0;
  }
  return String(props.selectableDatasourceType || "").trim().length > 0;
});

/**
 * 类型选择器的类型列表
 * @returns {Array}
 */
const typeSelectorTypes = computed(() =>
  getDatasourceTypeSelectorTypes({
    flag: props.flag,
    datasourceType: props.datasourceType,
  })
);

/**
 * 判断数据源是否匹配搜索关键词
 * @param {Object} source - 数据源项
 * @returns {boolean}
 */
function isDatasourceMatchSearch(source) {
  if (!searchKeyword.value) return true;
  return getDatasourceName(source)
    .toLowerCase()
    .includes(searchKeyword.value.toLowerCase());
}

/**
 * 判断数据源类型是否匹配搜索关键词
 * @param {Object} type - 数据源类型
 * @returns {boolean}
 */
function isDatasourceTypeMatchSearch(type) {
  if (!searchKeyword.value) return true;
  const keyword = searchKeyword.value.toLowerCase();
  return [type.key, type.value, type.label].some((item) =>
    String(item || "")
      .toLowerCase()
      .includes(keyword)
  );
}

/**
 * 根据过滤器判断类型是否可用
 * @param {Object|string} type - 类型配置或类型键
 * @returns {boolean}
 */
function isTypeAllowed(type) {
  return isDatasourceTypeAllowed(
    type,
    datasourceTypeFilter.value,
    getTypeConfig
  );
}

function isTypeDisabled(type) {
  const typeConfig = getTypeConfig(type);
  const disabledByFlag =
    props.flag && config[props.flag]?.[typeConfig?.key || type] === 2;
  const disabledBySelectableRange =
    hasSelectableDatasourceType.value &&
    !isDatasourceTypeAllowed(
      type,
      selectableDatasourceTypeFilter.value,
      getTypeConfig
    );
  return Boolean(disabledByFlag || disabledBySelectableRange);
}

/**
 * 根据标志判断类型是否应该隐藏
 * @param {Object|string} type - 类型配置或类型键
 * @returns {boolean}
 */
function isTypeHidden(type) {
  return isDatasourceTypeHiddenByFlag(
    type,
    props.flag,
    config,
    datasourceTypeFilter.value,
    getTypeConfig
  );
}

/**
 * 数据源项的通用过滤器
 * 包含类型允许检查、隐藏检查和搜索匹配
 * @param {Object} source - 数据源项
 * @returns {boolean}
 */
function isDatasourceValid(source) {
  return (
    isTypeAllowed(source.datasourceType) &&
    !isTypeHidden(source.datasourceType) &&
    isDatasourceMatchSearch(source)
  );
}

/**
 * 分类列表计算属性
 * @returns {Array}
 */
const categoryList = computed(() => {
  if (!shouldShowDatasourceColumn.value) {
    return getDatasourceTypeSelectorCategories(typeSelectorTypes.value);
  }

  return DatasourceConfig.filter((category) => {
    return Object.values(category.types).some(
      (type) => !isTypeHidden(type) && isTypeAllowed(type)
    );
  }).map((category) => ({
    key: category.key,
    label: category.label,
    icon: category.icon,
  }));
});

/**
 * 根据搜索关键词过滤后的分类列表
 * @returns {Array}
 */
const filteredCategoryList = computed(() => {
  if (!searchKeyword.value) return categoryList.value;
  if (!shouldShowDatasourceColumn.value) {
    return categoryList.value.filter(
      (category) => getTypeSelectorTypesByCategory(category.key).length > 0
    );
  }

  return categoryList.value.filter((category) => {
    return store.datasources.some(
      (source) =>
        isDatasourceOfCategory(source, category.key) &&
        isDatasourceValid(source)
    );
  });
});

/**
 * 类型数据源数量计算属性
 * 统计所有类型下的唯一数据源数量
 * @returns {number}
 */
const typeDatasourceCount = computed(() => {
  if (!shouldShowDatasourceColumn.value) {
    return typeList.value.length;
  }

  const seenIds = new Set();
  typeList.value.forEach((type) => {
    store.datasources.forEach((source) => {
      if (isDatasourceOfType(source, type.key) && isDatasourceValid(source)) {
        seenIds.add(source.id);
      }
    });
  });
  return seenIds.size;
});

/**
 * 根据数据源类型获取选中的图标
 * @returns {string}
 */
const selectedIcon = computed(() => {
  if (!selectedSourceType.value) return "";
  return getDatasourceIcon(selectedSourceType.value);
});

const selectedSources = computed(() => {
  const selectedIds = Array.isArray(props.modelValue) ? props.modelValue : [];
  return selectedIds
    .map((id) => store.datasources.find((source) => isSameValue(source.id, id)))
    .filter(Boolean);
});

const selectedTagItems = computed(() =>
  selectedSources.value.map((source) => ({
    ...source,
    label: getDatasourceName(source),
  }))
);

/**
 * 获取指定分类下的所有类型配置
 * @param {string} categoryKey - 分类键
 * @returns {Array}
 */
function getTypesByCategory(categoryKey) {
  if (!categoryKey) {
    return DatasourceConfig.flatMap((c) => Object.values(c.types));
  }
  const category = DatasourceConfig.find((c) => c.key === categoryKey);
  return category ? Object.values(category.types) : [];
}

/**
 * 判断类型是否有匹配的数据源（用于搜索过滤）
 * @param {Object} type - 类型配置
 * @returns {boolean}
 */
function hasMatchingDatasource(type) {
  return store.datasources.some(
    (source) =>
      isDatasourceOfType(source, type.key) && isDatasourceValid(source)
  );
}

/**
 * 类型列表计算属性
 * @returns {Array}
 */
const typeList = computed(() => {
  if (!shouldShowDatasourceColumn.value) {
    return getTypeSelectorTypesByCategory(selectedCategory.value);
  }

  const types = getTypesByCategory(selectedCategory.value);
  const seenKeys = new Set();

  return types.reduce((acc, type) => {
    if (isTypeHidden(type) || seenKeys.has(type.key) || !isTypeAllowed(type)) {
      return acc;
    }
    if (searchKeyword.value && !hasMatchingDatasource(type)) {
      return acc;
    }
    seenKeys.add(type.key);
    acc.push({
      key: type.key,
      label: type.label,
      value: type.value,
      icon: type.icon,
      disabled: isTypeDisabled(type),
    });
    return acc;
  }, []);
});

/**
 * 数据源列表计算属性
 * 根据分类、类型、搜索关键词过滤数据源
 * @returns {Array}
 */
const datasourceList = computed(() => {
  const targetTypeKeySet = selectedType.value
    ? getTargetTypeKeys(selectedType.value)
    : getCategoryTypeKeys(selectedCategory.value);

  let filteredDatasources = store.datasources.filter((source) => {
    if (targetTypeKeySet.size > 0) {
      const typeKey = String(source.datasourceType || "").toUpperCase();
      if (!targetTypeKeySet.has(typeKey)) return false;
    }
    return (
      isTypeAllowed(source.datasourceType) &&
      !isTypeHidden(source.datasourceType)
    );
  });

  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase();
    filteredDatasources = filteredDatasources.filter((source) =>
      getDatasourceName(source).toLowerCase().includes(keyword)
    );
  }

  if (!hasSelectableDatasourceType.value) {
    return filteredDatasources;
  }
  return filteredDatasources.map((source) => ({
    ...source,
    disabled: Boolean(source.disabled || isTypeDisabled(source.datasourceType)),
  }));
});

/**
 * 获取分类图标URL
 * @param {string} iconPath - 图标路径
 * @returns {string}
 */
function getCategoryIconUrl(iconPath, highlighted = false) {
  const fileName = String(iconPath || "").split("/").pop();
  const relativePath = highlighted
    ? `common/icons/data-source-category/active/${fileName}`
    : `common/icons/data-source-category/${fileName}`;
  return getAssetsFile(relativePath);
}

function handleCategoryMouseEnter(categoryKey) {
  hoveredCategory.value = categoryKey;
}

function handleCategoryMouseLeave(categoryKey) {
  if (hoveredCategory.value === categoryKey) hoveredCategory.value = null;
}

/**
 * 获取分类数量
 * @param {string} categoryKey - 分类键
 * @returns {number}
 */
function getCategoryCount(categoryKey) {
  if (!shouldShowDatasourceColumn.value) {
    return getTypeSelectorTypesByCategory(categoryKey).length;
  }

  return store.datasources.filter(
    (source) =>
      isDatasourceOfCategory(source, categoryKey) && isDatasourceValid(source)
  ).length;
}

/**
 * 分类总数量计算属性
 * @returns {number}
 */
const categoryTotalCount = computed(() => {
  if (!shouldShowDatasourceColumn.value) {
    return getTypeSelectorTypesByCategory().length;
  }

  return store.datasources.filter(isDatasourceValid).length;
});

/**
 * 根据分类获取类型选择器的类型列表
 * @param {string} categoryKey - 分类键
 * @returns {Array}
 */
function getTypeSelectorTypesByCategory(categoryKey = "") {
  return typeSelectorTypes.value.filter((type) => {
    const matchesCategory = !categoryKey || type.category === categoryKey;
    return matchesCategory && isDatasourceTypeMatchSearch(type);
  });
}

/**
 * 判断数据源是否属于指定类型
 * @param {Object} source - 数据源项
 * @param {string} typeKey - 类型键
 * @returns {boolean}
 */
function isDatasourceOfType(source, typeKey) {
  const typeConfig = getTypeConfig(typeKey);
  if (!typeConfig) return false;
  const sourceType = String(source.datasourceType || "").toUpperCase();
  return (
    sourceType === typeConfig.key?.toUpperCase() ||
    sourceType === typeConfig.value?.toUpperCase()
  );
}

/**
 * 获取指定分类下所有类型的键集合（用于快速匹配）
 * @param {string} categoryKey - 分类键
 * @returns {Set<string>}
 */
function getCategoryTypeKeys(categoryKey) {
  if (!categoryKey) {
    return new Set(
      DatasourceConfig.flatMap((c) =>
        Object.values(c.types).flatMap((t) => [
          t.key?.toUpperCase(),
          t.value?.toUpperCase(),
        ])
      )
    );
  }
  const category = DatasourceConfig.find((c) => c.key === categoryKey);
  if (!category) return new Set();
  return new Set(
    Object.values(category.types).flatMap((t) => [
      t.key?.toUpperCase(),
      t.value?.toUpperCase(),
    ])
  );
}

/**
 * 根据类型键获取目标类型的匹配键集合
 * @param {string} typeKey - 类型键
 * @returns {Set<string>}
 */
function getTargetTypeKeys(typeKey) {
  const targetTypeKey = typeKey.toUpperCase();
  const targetTypeConfig = DatasourceConfig.flatMap((c) =>
    Object.values(c.types)
  ).find(
    (t) =>
      t.key?.toUpperCase() === targetTypeKey ||
      t.value?.toUpperCase() === targetTypeKey
  );
  if (!targetTypeConfig) return new Set();
  return new Set([
    targetTypeConfig.key?.toUpperCase(),
    targetTypeConfig.value?.toUpperCase(),
  ]);
}

/**
 * 判断数据源是否属于指定分类
 * @param {Object} source - 数据源项
 * @param {string} categoryKey - 分类键
 * @returns {boolean}
 */
function isDatasourceOfCategory(source, categoryKey) {
  const category = DatasourceConfig.find((c) => c.key === categoryKey);
  if (!category) return false;
  const typeKeys = Object.values(category.types).flatMap((t) => [
    t.key?.toUpperCase(),
    t.value?.toUpperCase(),
  ]);
  const sourceType = String(source.datasourceType || "").toUpperCase();
  return typeKeys.includes(sourceType);
}

/**
 * 判断分类是否包含可用类型
 * @param {string} categoryKey - 分类键
 * @returns {boolean}
 */
function hasAllowedTypeInCategory(categoryKey) {
  const category = DatasourceConfig.find((c) => c.key === categoryKey);
  return Boolean(
    category &&
      Object.values(category.types).some((type) => isTypeAllowed(type))
  );
}

/**
 * 根据数据源类型过滤器重置选择
 */
function resetSelectionByDatasourceType() {
  if (selectedType.value && !isTypeAllowed(selectedType.value)) {
    selectedType.value = "";
  }
  if (
    selectedCategory.value &&
    !hasAllowedTypeInCategory(selectedCategory.value)
  ) {
    selectedCategory.value = "";
  }
}

/**
 * 比较两个值是否相等
 * @param {any} left - 左值
 * @param {any} right - 右值
 * @returns {boolean}
 */
function isSameValue(left, right) {
  return String(left ?? "") === String(right ?? "");
}

function isDatasourceSelected(id) {
  return props.multiple
    ? Array.isArray(props.modelValue) && props.modelValue.some((value) => isSameValue(value, id))
    : isSameValue(props.modelValue, id);
}

/**
 * 获取数据源显示名称
 * @param {Object} source - 数据源项
 * @returns {string}
 */
function getDatasourceName(source) {
  return source?.datasourceName || source?.name || "";
}

/**
 * 选择所有分类（清空分类选择）
 */
function selectAllCategories() {
  selectedCategory.value = "";
  selectedType.value = "";
}

/**
 * 根据类型获取数据源数量
 * @param {string} typeKey - 类型键
 * @returns {number}
 */
function getDatasourceCountByType(typeKey) {
  return store.datasources.filter(
    (source) => isDatasourceOfType(source, typeKey) && isDatasourceValid(source)
  ).length;
}

/**
 * 选择所有类型（清空类型选择）
 */
function selectAllTypes() {
  selectedType.value = "";
}

/**
 * 选择分类
 * @param {string} key - 分类键
 */
function selectCategory(key) {
  selectedCategory.value = key;
  selectedType.value = "";
}

/**
 * 选择类型
 * @param {string} key - 类型键
 */
function selectType(key) {
  selectedType.value = key;

  if (!shouldShowDatasourceColumn.value) {
    visible.value = false;
    const selectedTypeObj = typeList.value.find((type) => type.key == key);
    if (selectedTypeObj) {
      emits("update:modelValue", key);
      emits("change", key, selectedTypeObj);
    }
  }
}

/**
 * 选择数据源
 * @param {Object} source - 数据源项
 */
function selectDatasource(source) {
  if (source.disabled) return;
  if (props.multiple) {
    const current = Array.isArray(props.modelValue) ? props.modelValue : [];
    const selectedIds = isDatasourceSelected(source.id)
      ? current.filter((id) => !isSameValue(id, source.id))
      : [...current, source.id];
    const selectedSources = store.datasources.filter((item) =>
      selectedIds.some((id) => isSameValue(id, item.id))
    );
    emits("update:modelValue", selectedIds);
    emits("change", selectedIds, selectedSources);
    return;
  }
  displayValue.value = getDatasourceName(source);
  selectedSourceType.value = source.datasourceType;
  visible.value = false;
  emits("update:modelValue", source.id);
  emits("change", source.id, source);
}

/**
 * 清空数据源选择
 */
function selectAllDatasources() {
  displayValue.value = "";
  selectedSourceType.value = "";
  visible.value = false;
  emits("update:modelValue", props.multiple ? [] : "");
  emits("change", props.multiple ? [] : "", props.multiple ? [] : null);
}

/**
 * 获取过滤后的数据源列表
 * @param {Array} data - 原始数据源数据
 * @returns {Array}
 */
function getFilteredDatasource(data) {
  let list = getAvailableDatasource(data, props.flag);
  if (datasourceTypeFilter.value) {
    list = list.filter((item) => isTypeAllowed(item.datasourceType));
  }
  if (!props.filterMethod) return list;
  return list.filter((item) => props.filterMethod(item));
}

/**
 * 初始化数据源数据
 */
function initDatasourceData() {
  resetSelectionByDatasourceType();
  if (props.data) {
    store.datasources = getFilteredDatasource(props.data);
    return;
  }
  const params = {
    pageNum: 1,
    pageSize: 9999,
    orderByColumn: "create_time",
    isAsc: "descending",
    ...(props.params || {}),
  };
  if (props.project) {
    params.projectId = userStore.projectId;
    params.projectCode = userStore.projectCode;
  }
  if (props.datasourceType) {
    params.datasourceType = Array.isArray(props.datasourceType)
      ? props.datasourceType.join(",")
      : props.datasourceType;
  }
  getDatasourceData(params, props.listApi).then((data) => {
    store.datasources = getFilteredDatasource(data);
  });
}

/**
 * 监听属性变化并重新初始化数据
 */
watch(
  [
    () => props.data,
    () => props.flag,
    () => props.project,
    () => props.datasourceType,
    () => props.filterMethod,
    () => props.listApi,
    () => props.params,
    () => props.selectionMode,
  ],
  initDatasourceData,
  { immediate: true, deep: true }
);

/**
 * 根据数据源类型查找所属的分类和类型键
 * @param {string} datasourceType - 数据源类型
 * @returns {Object|null} - {categoryKey, typeKey} 或 null
 */
function findCategoryAndTypeByDatasourceType(datasourceType) {
  const sourceType = String(datasourceType || "").toUpperCase();
  for (const category of DatasourceConfig) {
    for (const [typeKey, typeConfig] of Object.entries(category.types)) {
      if (
        typeConfig.key?.toUpperCase() === sourceType ||
        typeConfig.value?.toUpperCase() === sourceType
      ) {
        return { categoryKey: category.key, typeKey };
      }
    }
  }
  return null;
}

/**
 * 更新选中状态（分类、类型、显示值）
 * @param {Object} source - 数据源项
 */
function updateSelectionBySource(source) {
  if (source.disabled) {
    emits("update:modelValue", "");
    displayValue.value = "";
    selectedSourceType.value = "";
    return;
  }
  selectedSourceType.value = source.datasourceType;
  const match = findCategoryAndTypeByDatasourceType(source.datasourceType);
  if (match) {
    selectedCategory.value = match.categoryKey;
    selectedType.value = match.typeKey;
  }
  displayValue.value = getDatasourceName(source);
}

/**
 * 更新类型选择模式下的显示状态
 * @param {any} modelValue - 模型值
 */
function updateTypeSelection(modelValue) {
  const selectedTypeObj = typeList.value.find(
    (type) => type.key === modelValue
  );
  if (selectedTypeObj) {
    displayValue.value = selectedTypeObj.label;
    selectedSourceType.value = selectedTypeObj.key;
  } else {
    displayValue.value = "";
    selectedSourceType.value = "";
  }
}

/**
 * 监听模型值变化并更新显示
 */
watch(
  [() => props.modelValue, () => store.datasources, () => props.multiple],
  ([newModelValue, newDatasources]) => {
    if (props.multiple) {
      const selectedIds = Array.isArray(newModelValue) ? newModelValue : [];
      const selectedSources = selectedIds
        .map((id) => newDatasources.find((item) => isSameValue(item.id, id)))
        .filter(Boolean);
      displayValue.value = selectedSources.map(getDatasourceName).join("、");
      selectedSourceType.value = selectedSources.length === 1
        ? selectedSources[0].datasourceType
        : "";
      return;
    }
    if (!newModelValue) {
      displayValue.value = "";
      selectedSourceType.value = "";
      selectedType.value = "";
      selectedCategory.value = "";
      searchKeyword.value = "";
      return;
    }

    if (props.selectionMode === "type") {
      updateTypeSelection(newModelValue);
      return;
    }

    if (newDatasources.length === 0) return;

    const source = newDatasources.find((item) =>
      isSameValue(item.id, newModelValue)
    );
    if (source) {
      updateSelectionBySource(source);
    } else {
      displayValue.value = "";
      selectedSourceType.value = "";
    }
  },
  { immediate: true }
);

/**
 * 检查当前选中的类型是否有匹配的数据源
 * @returns {boolean}
 */
function hasSelectedTypeMatch() {
  return store.datasources.some(
    (source) =>
      isDatasourceOfType(source, selectedType.value) &&
      isDatasourceValid(source)
  );
}

/**
 * 检查当前选中的分类是否有匹配的数据源
 * @returns {boolean}
 */
function hasSelectedCategoryMatch() {
  return store.datasources.some(
    (source) =>
      isDatasourceOfCategory(source, selectedCategory.value) &&
      isDatasourceValid(source)
  );
}

/**
 * 监听搜索关键词变化，若无匹配则重置选择
 */
watch(searchKeyword, () => {
  if (!searchKeyword.value) return;

  if (selectedType.value && !hasSelectedTypeMatch()) {
    selectedType.value = "";
  }

  if (selectedCategory.value && !hasSelectedCategoryMatch()) {
    selectedCategory.value = "";
    selectedType.value = "";
  }
});

/**
 * 监听禁用状态，禁用时关闭弹出层
 */
watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) visible.value = false;
  }
);
</script>

<style scoped lang="scss">
$primary-color: var(--qt-primary);
$primary-light: rgba(var(--qt-primary-rgb), 0.06);
$text-primary: #333;
$text-secondary: rgba(51, 51, 51, 0.65);
$text-placeholder: #a8abb2;
$text-disabled: #c0c4cc;
$text-empty: #909399;
$border-color: rgba(232, 232, 232, 0.45);
$border-color-light: rgba(214, 218, 225, 0.45);
$bg-white: #fff;
$font-family: PingFangSC-Regular, "PingFang SC", "Microsoft YaHei", Arial,
  sans-serif;
$font-family-medium: PingFangSC-Medium, "PingFang SC", "Microsoft YaHei", Arial,
  sans-serif;

$icon-width-sm: 18px;
$icon-height-sm: 18px;
$icon-width-md: 20px;
$icon-height-md: 20px;
$icon-width-prefix: 21px;

$input-height: 32px;
$input-inner-height: 30px;

$item-height: 40px;
$item-margin-bottom: 6px;

$column-category-width: 248px;
$column-type-width: 218px;

$popover-shadow: 0 4px 17px rgba(0, 0, 0, 0.1);

@mixin reset-scrollbar {
  scrollbar-width: none;
  &::-webkit-scrollbar {
    width: 0;
    height: 0;
  }
}

@mixin ellipsis-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prefix-icon {
  height: $icon-width-prefix;
  display: block;
  object-fit: contain;
}

.datasource-selector {
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 398px;
  font-family: $font-family;

  .search-input {
    flex: 0 0 $input-height;
    width: 100%;

    :deep(.el-input__wrapper) {
      min-height: $input-height;
      height: $input-height;
      padding: 0 10px;
      border-radius: 2px;
      background: $bg-white;
      box-shadow: 0 0 0 1px #d6dae1 inset;

      &.is-focus {
        box-shadow: 0 0 0 1px $primary-color inset;
      }
    }

    :deep(.el-input__prefix-inner) {
      color: $text-placeholder;

      .el-icon {
        width: 16px;
        height: 16px;
        font-size: 16px;
      }
    }

    :deep(.el-input__inner) {
      height: $input-inner-height;
      color: $text-primary;
      font-family: inherit;
      font-size: 14px;
      line-height: 22px;
      letter-spacing: 0;

      &::placeholder {
        color: $text-placeholder;
      }
    }
  }

  .columns-container {
    box-sizing: border-box;
    flex: 0 0 346px;
    display: flex;
    width: 100%;
    height: 346px;
    overflow: hidden;
    margin-top: 20px;
    border: 1px solid $border-color;
    border-radius: 4px;
    background: $bg-white;

    .selector-column {
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      min-width: 0;
      height: 100%;
      overflow: hidden;
      border-right: 1px solid $border-color-light;

      &:last-child {
        border-right: none;
      }

      &.category-column {
        flex: 0 0 $column-category-width;
        width: $column-category-width;
      }

      &.type-column {
        flex: 0 0 $column-type-width;
        width: $column-type-width;
      }

      &.datasource-column {
        flex: 1 1 auto;
        width: auto;
      }

      .column-content {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        overflow-x: hidden;
        padding: 0;
        @include reset-scrollbar;
      }

      .column-footer {
        box-sizing: border-box;
        flex: 0 0 28px;
        height: 28px;
        padding: 3px 10px 8px;
        overflow: hidden;
        color: $text-secondary;
        font-size: 12px;
        font-weight: 400;
        line-height: 17px;
        white-space: nowrap;
      }

      .datasource-list-item {
        box-sizing: border-box;
        flex: 0 0 $item-height;
        display: flex;
        align-items: center;
        width: 100%;
        height: $item-height;
        margin-bottom: $item-margin-bottom;
        padding: 0 10px;
        overflow: hidden;
        border-radius: 2px;
        color: $text-primary;
        cursor: pointer;
        font-family: inherit;
        font-weight: 400;
        font-size: 14px;
        line-height: 20px;
        letter-spacing: 0;
        white-space: nowrap;
        transition: background-color 0.2s, color 0.2s;

        &:last-child {
          margin-bottom: 0;
        }

        &:hover:not(.disabled) {
          background: $primary-light;
        }

        &.active:not(.disabled) {
          background: $primary-light;
        }

        &.disabled {
          color: $text-disabled;
          cursor: not-allowed;
          opacity: 0.6;
        }
      }

      .category-label,
      .type-label,
      .datasource-label {
        flex: 1;
        min-width: 0;
        @include ellipsis-text;
      }

      .category-count {
        flex-shrink: 0;
        color: rgba(0, 0, 0, 0.45);
        font-size: 14px;
        font-weight: 400;
        line-height: 20px;

        &::before {
          content: "(";
        }

        &::after {
          content: ")";
        }
      }

      .category-item {
        .category-icon {
          width: $icon-width-sm;
          height: $icon-height-sm;
          margin-right: 12px;
          flex-shrink: 0;
          object-fit: contain;
        }

        .category-label {
          margin-right: 8px;
        }

        &:hover .category-label,
        &.active .category-label {
          color: $primary-color;
        }

        &.active {
          .category-label {
            font-family: $font-family-medium;
            font-weight: 500;
          }
        }
      }

      .type-item {
        .type-item-icon {
          width: $icon-width-md;
          height: $icon-height-md;
          margin-right: 8px;
          flex-shrink: 0;
          object-fit: contain;
        }

        .type-label {
          margin-right: 8px;
        }
      }

      .datasource-item {
        .type-icon {
          width: $icon-width-md;
          height: $icon-height-md;
          margin-right: 8px;
          flex-shrink: 0;
          object-fit: contain;
        }

        .datasource-label {
          font-size: 14px;
        }

        &:hover .datasource-label,
        &.active .datasource-label {
          color: $primary-color;
        }

        &.active .datasource-label {
          font-family: $font-family-medium;
          font-weight: 500;
        }
      }

      .type-item,
      .datasource-item {
        :deep(.el-radio),
        :deep(.el-checkbox) {
          flex-shrink: 0;
          width: 16px;
          height: 16px;
          margin-right: 10px;

          .el-radio__input,
          .el-radio__inner,
          .el-checkbox__input,
          .el-checkbox__inner {
            width: 16px;
            height: 16px;
          }

          .el-radio__label,
          .el-checkbox__label {
            display: none;
          }
        }

        :deep(.el-radio) {
          .el-radio__inner {
            border-color: rgba(51, 51, 51, 0.2);
            background: $bg-white;
          }

          .el-radio__input.is-disabled {
            .el-radio__inner {
              border-color: rgba(51, 51, 51, 0.2);
              background: $bg-white;

              &::after {
                width: 4px;
                height: 4px;
                background: $bg-white;
                transform: translate(-50%, -50%) scale(1);
              }
            }

            &.is-checked .el-radio__inner {
              border-color: $primary-color;
              background: $primary-color;
            }
          }
        }
      }

      .empty-state {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-direction: column;
        min-height: 180px;
        padding: 20px;

        .empty-img {
          width: 100px;
          height: auto;
          margin-bottom: 12px;
          object-fit: contain;
        }

        .empty-desc {
          font-size: 13px;
          color: $text-empty;
        }
      }
    }
  }
}

.datasource-tag-input {
  box-sizing: border-box;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 32px;
  cursor: pointer;

  &:hover { box-shadow: 0 0 0 1px var(--el-input-hover-border-color) inset; }
  &:focus-visible,
  &:focus-within { box-shadow: 0 0 0 1px var(--el-input-focus-border-color) inset; }
  &.is-disabled { background: var(--el-disabled-bg-color); cursor: not-allowed; }

  :deep(.datasource-selected-tags) {
    display: block;
    flex: 1;
    min-width: 0;
    overflow: hidden;
  }

  :deep(.tag-container) { gap: 4px; }
  .datasource-tag-placeholder { flex: 1; color: var(--el-text-color-placeholder); }
  .datasource-tag-arrow,
  .datasource-tag-clear { flex: 0 0 auto; margin-left: 8px; color: var(--el-text-color-placeholder); }
  .datasource-tag-clear { cursor: pointer; }
  &.is-disabled .datasource-tag-arrow { color: var(--el-disabled-text-color); }
}

:global(.datasource-list-popper.el-popover.el-popper) {
  box-sizing: border-box;
  padding: 20px;
  border: 0;
  border-radius: 4px;
  background: $bg-white;
  box-shadow: $popover-shadow;
}
</style>
