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
  <el-dialog
      v-model="visible"
      :title="compareTitle"
      width="1000px"
      draggable
      class="dialog"
      @closed="resetCompare"
  >
    <guide-tip-panel
        type="hint"
        allow-wrap
        title="历史版本仅支持查看和对比，不提供恢复或回退。"
    />

    <el-form :model="queryParams" :inline="true" class="mt15">
      <qt-form-item label="对比版本" prop="compareTargetId">
        <el-select
            v-model="queryParams.compareTargetId"
            clearable
            placeholder="请选择对比版本"
            class="version-select"
            @change="handleQuery"
        >
          <el-option
              v-for="item in compareOptions"
              :key="item.id"
              :label="item.versionNo"
              :value="item.id"
          />
        </el-select>
      </qt-form-item>
    </el-form>

    <qt-table v-bind="tableStore" ref="tableRef"/>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="VersionCompareDialog">
/**
 * @description 模型版本比较弹窗。
 * @prop {Array} rows 列表数据
 * @prop {[Number, String]} currentVersionId 当前版本 ID
 */
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const props = defineProps({
  rows: {type: Array, default: () => []},
  currentVersionId: {type: [Number, String], default: null},
});

const visible = ref(false);
const tableRef = ref(null);
const compareBase = ref(null);

const data = reactive({
  queryParams: {
    compareTargetId: null,
  },
});
const {queryParams} = toRefs(data);

const compareOptions = computed(() => props.rows.filter(
    item => String(item.id) !== String(compareBase.value?.id)
));
const compareTarget = computed(() => props.rows.find(
    item => String(item.id) === String(queryParams.value.compareTargetId)
) || null);
const compareBaseLabel = computed(() => getVersionLabel(compareBase.value));
const compareTargetLabel = computed(() => (
    compareTarget.value ? getVersionLabel(compareTarget.value) : "请选择对比版本"
));
const compareTitle = computed(() => (
    `版本对比 · ${compareBase.value?.versionNo || "-"} ↔ ${compareTarget.value?.versionNo || "-"}`
));
const compareRows = computed(() => {
  // 两边的版本都选好了才开始比较，少选一个就先显示空表格。
  if (!compareBase.value || !compareTarget.value) return [];
  return buildCompareRows(compareBase.value.changeData, compareTarget.value.changeData);
});

const tableStore = reactive({
  config: {
    stripe: false,
    table: {
      class: "version-compare-table",
      // 只给右边的值加背景色：新增用绿色，修改用黄色，删除用红色。
      cellClassName: ({row, column}) => column.property === "rightValue"
          ? `compare-cell-${row.changeType}` : "",
    },
    notPagination: true,
    notPaginationParams: true,
    fullContent: false,
  },
  columns: [],
  func: async () => ({data: compareRows.value}),
  params: queryParams,
});

watchEffect(() => {
  tableStore.columns = [
    {label: "对比项", prop: "label", minWidth: 230, align: "left"},
    {
      label: compareBaseLabel.value, prop: "leftValue", minWidth: 310, align: "left",
      showOverflowTooltip: {effect: "light"}
    },
    {
      label: compareTargetLabel.value, prop: "rightValue", minWidth: 310, align: "left",
      showOverflowTooltip: {effect: "light"}
    },
  ];
});

/** 将版本变更数据转换为可比较的对象，兼容对象和 JSON 字符串。 */
function parseChangeData(changeData) {
  if (!changeData) return {};
  if (typeof changeData === "object") return changeData;
  try {
    const parsed = JSON.parse(changeData);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {changeData};
  }
}

/** 把字段信息拼成一句简短说明，普通字段显示类型和长度，分类/关联字段显示关联关系。 */
function formatField(field) {
  // 旧数据如果不是字段对象，就直接当作普通值展示，避免读取属性时报错。
  if (!field || typeof field !== "object") return formatValue(field);
  const entries = [];
  // 分类字段和关联字段重点看“连到了哪个模型、用哪个字段关联、显示哪个字段”。
  if (["CLASS", "RELATION"].includes(field.columnCategory)) {
    const mapping = [field.relatedCode, field.displayCode].filter(hasValue);
    // 关联键在箭头左边，展示字段在右边；只有一个有值时，只显示这一个。
    if (mapping.length) entries.push(mapping.join(" → "));
  } else {
    // 普通字段按“string, length 255, scale 0”展示，没配置的内容不拼进去。
    if (hasValue(field.dataType)) entries.push(String(field.dataType).toLowerCase());
    if (hasValue(field.dataLength)) entries.push(`length ${field.dataLength}`);
    if (hasValue(field.dataScale)) entries.push(`scale ${field.dataScale}`);
  }
  // 没有任何可展示的信息时，用横线表示这一侧没有值。
  return entries.join(", ") || "-";
}

/** 判断有没有配置值：null、undefined 和空字符串不展示，0 和 false 都算有值。 */
function hasValue(value) {
  return value != null && value !== "";
}

/** 只取子模型中还存在的业务字段，分类、关联和系统字段不计入业务字段数量。 */
function businessFields(model) {
  // 标了删除的字段不再计数；旧数据没写字段分类时，按业务字段处理。
  return (model?.fields || []).filter(field => field && !field._deleted
      && (!field.columnCategory || field.columnCategory === "BUSINESS"));
}

/** 子模型先显示“几个业务字段”，左右两边各自补充相对另一边新增、删除、修改了哪些字段。 */
function formatSubModel(model, previous) {
  const fields = businessFields(model);
  const entries = [`${fields.length} 个业务字段`];
  // 另一侧没有这个子模型时，按没有业务字段来比较，这边的字段都会标为新增。
  // 按字段身份建索引，比较同一个字段，不依赖它在列表中的位置。
  const before = new Map(businessFields(previous).map(field => [detailKey(field), field]));
  const after = new Map(fields.map(field => [detailKey(field), field]));
  // 这边有、另一边没有就是新增；另一边有、这边没有就是删除。
  const added = fields.filter(field => !before.has(detailKey(field)));
  const removed = businessFields(previous).filter(field => !after.has(detailKey(field)));
  // 两边都有同一个字段，但业务内容不一样，才算修改。
  const changed = fields.filter(field => before.has(detailKey(field))
      && signature(field) !== signature(before.get(detailKey(field))));
  const changes = [["新增", added], ["删除", removed], ["修改", changed]]
      // 哪一类没有变化就不显示，避免出现“新增”后面没有字段的空说明。
      .filter(([, items]) => items.length)
      .map(([label, items]) => `${label} ${items.map(field => field.code || field.name || field.id).join("、")}`);
  if (changes.length) entries.push(`（${changes.join("；")}）`);
  return entries.join("");
}

/** 把业务内容整理成可以比较的文本，避免属性顺序和操作记录造成“看起来有变化”。 */
function signature(value) {
  // 列表不比较排列顺序，也不比较已删除的内容，只比较还存在的业务内容。
  if (Array.isArray(value)) return JSON.stringify(value.filter(item => !item?._deleted).map(signature).sort());
  if (value && typeof value === "object") {
    // 属性先排序；ID、创建/更新时间和下划线开头的内部记录，不算业务内容的变化。
    return JSON.stringify(Object.keys(value).sort()
        .filter(key => !key.startsWith("_") && !["id", "localId", "createTime", "updateTime", "createBy", "updateBy"].includes(key))
        .map(key => [key, signature(value[key])]));
  }
  // null 和 undefined 都按没值处理，避免仅仅缺少属性就被算作变更。
  return JSON.stringify(value ?? null);
}

/** 找到同一个字段或子模型：优先用 ID，没有 ID 再用编码、名称，最后用列表位置兜底。 */
function detailKey(item, index = 0) {
  return String(item.id ?? item.code ?? item.name ?? index);
}

/** 将不同类型的属性值统一转换为可读文本。 */
function formatValue(value) {
  if (!hasValue(value)) return "-";
  if (typeof value === "boolean") return value ? "是" : "否";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

/** 把模型属性、字段和子模型拆成一条条对比项，方便两边找到对应的内容。 */
function buildChangeEntries(changeData) {
  const data = parseChangeData(changeData);
  const result = {};
  const labels = {
    name: "模型名称", code: "模型编码", description: "模型说明",
    projectId: "所属主数据域", layerId: "模型目录", type: "模型类型",
  };
  Object.entries(data).forEach(([key, value]) => {
    // 下划线开头的是历史记录、删除标记等内部信息，不作为对比项展示。
    if (key.startsWith("_")) return;
    // 字段和子模型都是列表，拆开后每个字段或子模型各占一行。
    if (["fields", "subModels"].includes(key) && Array.isArray(value)) {
      value.forEach((item, index) => {
        // 空记录和已删除的记录不展示；左边没有的对比项，也不会因为右边有就单独列出来。
        if (!item || item._deleted) return;
        const kind = key === "subModels" ? "subModel" : "field";
        // 左边的对比项名称要能看出它是普通字段、分类字段、关联字段还是子模型。
        const prefix = kind === "subModel" ? "子模型"
            : item.columnCategory === "CLASS" ? "分类字段"
                : item.columnCategory === "RELATION" ? "关联字段" : "字段";
        const name = kind === "subModel" ? item.name || item.code : item.code || item.name;
        // 用身份来配对，用名称来展示；改了名称也能继续比较同一条记录。
        result[`${key}:${detailKey(item, index)}`] = {
          kind, value: item, label: `${prefix}：${name || index + 1}`,
        };
      });
      return;
    }
    // 模型说明等单个属性直接占一行，已知属性用中文名称显示。
    result[key] = {kind: "property", value, label: labels[key] || key};
  });
  return result;
}

/** 只拿左侧最新版本里的对比项去比较，左侧没有的项和两边相同的项都不展示。 */
function buildCompareRows(leftData, rightData) {
  const left = buildChangeEntries(leftData);
  const right = buildChangeEntries(rightData);
  // 左侧决定展示范围：右侧有、左侧没有的历史内容，不额外生成一行。
  return Object.keys(left).flatMap(key => {
    const before = left[key];
    const after = right[key];
    // 业务内容一样就不放进表格，让用户只看到有差异的行。
    if (signature(before?.value) === signature(after?.value)) return [];
    const entry = before;
    // 不同内容用不同的摘要格式；某一侧没有这条记录时显示横线。
    const format = (item, previous) => {
      if (!item) return "-";
      if (item.kind === "field") return formatField(item.value);
      if (item.kind === "subModel") return formatSubModel(item.value, previous?.value);
      return formatValue(item.value);
    };
    // 保留右侧的颜色规则：右边没有这条记录用红色，两边都有但内容不同用黄色。
    const changeType = !after ? "removed" : "changed";
    const rightValue = format(after, before);
    return [{
      // 左边也传入右边的数据，这样左右两列都能列出各自的字段变化。
      // 例如某字段只在左边有，左边写“新增”，右边写“删除”；修改的字段两边都会列出。
      key, label: entry.label, leftValue: format(before, after),
      rightValue,
      // 子模型本身还在，但里面新增了业务字段，也按截图用绿色强调这一行。
      changeType: entry.kind === "subModel" && after && before
      && businessFields(after.value).some(field => !businessFields(before.value)
          .some(old => detailKey(old) === detailKey(field))) ? "added" : changeType,
    }];
  });
}

/** 生成版本表头文案，并标记当前正式版本。 */
function getVersionLabel(version) {
  if (!version) return "-";
  const current = String(version.id) === String(props.currentVersionId);
  return `${version.versionNo || "-"}${current ? "（当前正式）" : ""}`;
}

/** 打开版本对比弹窗，并默认选择当前正式版本或首个可用版本。 */
function openDialog(row) {
  compareBase.value = row;
  const preferred = compareOptions.value.find(item => String(item.id) === String(props.currentVersionId));
  queryParams.value.compareTargetId = (preferred || compareOptions.value[0])?.id ?? null;
  visible.value = true;
  nextTick(refreshList);
}

/** 刷新版本结构对比列表 */
function refreshList() {
  tableRef.value?.getList();
}

/** 搜索按钮操作 */
function handleQuery() {
  refreshList();
}

/** 弹窗关闭后清空本次版本对比状态。 */
function resetCompare() {
  compareBase.value = null;
  queryParams.value.compareTargetId = null;
}

defineExpose({openDialog});
</script>

<style scoped lang="scss">
:deep(.version-compare-table) {
  .el-table td.compare-cell-changed {
    background-color: color-mix(in srgb, var(--qt-status-warning) 12%, var(--qt-surface-background));
  }

  .el-table td.compare-cell-added {
    background-color: color-mix(in srgb, var(--qt-status-success) 8%, var(--qt-surface-background));
  }

  .el-table td.compare-cell-removed {
    background-color: color-mix(in srgb, var(--qt-status-danger) 10%, var(--qt-surface-background));
  }
}

.version-select {
  width: var(--qt-model-version-select-width);
}
</style>
