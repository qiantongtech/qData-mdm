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
  <div ref="editorRef" class="model-fields-editor" :class="{ compact }">
    <!-- 主模型字段标题。 -->
    <div v-if="!compact && title" class="h2-titles">{{ title }}</div>

    <!-- 多字段分类场景展示分类页签，子模型紧凑模式不重复展示。 -->
    <el-tabs v-if="showKindTabs" v-model="activeKind" class="demo-tabs">
      <qt-tab-pane
          v-for="kind in visibleKinds"
          :key="kind.value"
          :label="`${kind.label}`"
          :name="kind.value"
      />
    </el-tabs>

    <!-- 各字段分类共用系统 qt-table，通过动态列与插槽保留原有编辑能力。 -->
    <qt-wrap
        :columns="fieldTableStore.columns"
        :table-ref="normalTableRef"
        :config="fieldTableStore.config"
    >
      <template #actions-data>
        <!-- 主、子模型共用页签下方的新增操作区。 -->
        <div v-if="!readonly && activeKind !== 'SYSTEM'">
          <el-button
              v-if="allowedKinds.includes('BUSINESS') && activeKind === 'BUSINESS'"
              plain
              type="primary"
              icon="Plus"
              @click="addNormalField"
          >
            新增{{ model_column_category.find(item => item.value === 'BUSINESS')?.label }}
          </el-button>
          <el-button
              v-if="allowedKinds.includes('RELATION') && activeKind === 'RELATION'"
              plain
              type="primary"
              icon="Plus"
              @click="openAddDialog('RELATION')"
          >新增{{ model_column_category.find(item => item.value === 'RELATION')?.label }}
          </el-button>
          <el-button
              v-if="allowedKinds.includes('CLASS') && activeKind === 'CLASS'"
              plain
              type="primary"
              icon="Plus"
              @click="openAddDialog('CLASS')"
          >新增{{ model_column_category.find(item => item.value === 'CLASS')?.label }}
          </el-button>

        </div>
        <guide-tip-panel
            v-if="activeKind === 'SYSTEM'"
            type="plain"
            title="系统字段由系统自动添加和维护，无需手动配置。"
        />
      </template>

      <qt-table v-bind="fieldTableStore" ref="normalTableRef">
        <template #nodeRule="{ row }">
          <dict-tag :options="model_field_node_rule" :value="row.nodeRule"/>
        </template>
        <template #displayMode="{ row }">
          <dict-tag :options="model_field_display_mode" :value="row.displayMode"/>
        </template>
        <template #rowIndex="{ $index }">{{ $index + 1 }}</template>
        <template #businessName="{ row }">
          <el-input
              v-model="row.name"
              :data-model-field="row.localId"
              data-model-property="name"
              :disabled="isReadonly(row, 'name')"
              placeholder="请输入字段中文名称"
              @change="handleFieldChange(row, 'name')"
          />
        </template>
        <template #businessCode="{ row }">
          <el-input
              v-model="row.code"
              :data-model-field="row.localId"
              data-model-property="code"
              :disabled="isReadonly(row, 'code')"
              placeholder="请输入字段英文名称"
              @change="handleFieldChange(row, 'code')"
          />
        </template>
        <template #businessDataType="{ row }">
          <el-select
              v-model="row.dataType"
              :data-model-field="row.localId"
              data-model-property="dataType"
              filterable
              placeholder="请选择数据类型"
              :disabled="isReadonly(row, 'dataType')"
              @change="handleInlineTypeChange(row)"
          >
            <el-option
                v-for="item in dataTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            />
          </el-select>
        </template>
        <template #requiredFlag="{ row }">
          <el-checkbox
              v-model="row.requiredFlag"
              :data-model-field="row.localId"
              data-model-property="requiredFlag"
              :disabled="isReadonly(row, 'requiredFlag')"
              @change="handleRequiredChange(row)"
          />
        </template>
        <template #uniqueFlag="{ row }">
          <el-checkbox
              v-model="row.uniqueFlag"
              :data-model-field="row.localId"
              data-model-property="uniqueFlag"
              :disabled="isReadonly(row, 'uniqueFlag')"
              @change="handleFieldChange(row, 'uniqueFlag')"
          />
        </template>
        <template #nameFlag="{ row }">
          <el-radio
              :data-model-field="row.localId"
              data-model-property="nameFlag"
              :model-value="row.nameFlag"
              :value="true"
              :disabled="isReadonly(row, 'nameFlag')"
              @change="setNameField(row)"
          />
        </template>
        <template #defaultValue="{ row }">
          <el-select
              v-if="row.dataType === 'BOOLEAN'"
              v-model="row.defaultValue"
              :data-model-field="row.localId"
              data-model-property="defaultValue"
              class="default-value-control"
              :disabled="isReadonly(row, 'defaultValue')"
              placeholder="请选择默认值"
              @change="handleFieldChange(row, 'defaultValue')"
          >
            <el-option label="是" value="true"/>
            <el-option label="否" value="false"/>
          </el-select>
          <el-date-picker
              v-else-if="row.dataType === 'DATE' || row.dataType === 'DATETIME'"
              v-model="row.defaultValue"
              :data-model-field="row.localId"
              data-model-property="defaultValue"
              class="default-value-control"
              :type="row.dataType === 'DATE' ? 'date':'datetime'"
              :value-format="row.dataType === 'DATE' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss'"
              :disabled="isReadonly(row, 'defaultValue')"
              placeholder="请选择日期"
              @change="handleFieldChange(row, 'defaultValue')"
          />
          <!-- INTEGER 用数字输入框，DECIMAL 保持文本输入；值仍按字符串接收，避免大整数和小数丢精度。 -->
          <el-input
              v-else-if="['INTEGER', 'DECIMAL'].includes(row.dataType)"
              v-model="row.defaultValue"
              :data-model-field="row.localId"
              data-model-property="defaultValue"
              class="default-value-control"
              :disabled="isReadonly(row, 'defaultValue')"
              :type="row.dataType === 'INTEGER' ? 'number' : 'text'"
              :inputmode="row.dataType === 'INTEGER' ? 'numeric' : 'decimal'"
              placeholder="请输入默认值"
              @change="handleFieldChange(row, 'defaultValue')"
          />
          <el-input
              v-else
              v-model="row.defaultValue"
              :data-model-field="row.localId"
              data-model-property="defaultValue"
              class="default-value-control"
              :disabled="isReadonly(row, 'defaultValue')"
              placeholder="请输入默认值"
              @change="handleFieldChange(row, 'defaultValue')"
          />
        </template>
        <template #dataLength="{ row }">
          <el-input-number
              v-if="['STRING', 'DECIMAL'].includes(row.dataType)"
              v-model="row.dataLength"
              :data-model-field="row.localId"
              data-model-property="dataLength"
              :min="minimumLength(row)"
              :disabled="isReadonly(row, 'dataLength')"
              :controls="false"
              placeholder="请输入属性长度"
              @change.capture="handleLengthInputChange(row, $event)"
              @change="handleFieldChange(row, 'dataLength')"
          />
          <span v-else>-</span>
        </template>
        <template #dataScale="{ row }">
          <el-input-number
              v-if="row.dataType === 'DECIMAL'"
              v-model="row.dataScale"
              :data-model-field="row.localId"
              data-model-property="dataScale"
              :min="0"
              :disabled="isReadonly(row, 'dataScale')"
              :controls="false"
              placeholder="请输入小数"
              @change="handleFieldChange(row, 'dataScale')"
          />
          <span v-else>-</span>
        </template>
        <template #reference="{ row }">
          <!-- Element Plus 会将 null 视为空值，因此空值时用“不关联”作为可见占位文本。 -->
          <el-select
              v-model="row.dictTypeId"
              :data-model-field="row.localId"
              data-model-property="dictTypeId"
              :disabled="isReadonly(row, 'dictTypeId')"
              clearable
              :value-on-clear="null"
              :placeholder="row.dictTypeId == null ? '不关联' : '请选择参考数据'"
              @change="handleReferenceChange(row)"
          >
            <el-option
                v-for="item in loadedReferenceOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </template>
        <template #storageType="{ row }">{{ row.storageType || formatSize(row) }}</template>
        <template #requiredLabel="{ row }">{{ row.requiredFlag ? "是" : "否" }}</template>
        <template #systemLock>
          <dict-tag type="warning">
            <span class="justify-center system-lock-tag">
              <el-icon><lock/></el-icon>
              <span>锁定</span>
            </span>
          </dict-tag>
        </template>
        <template #action="{ row }">
          <el-button
              link
              type="primary"
              icon="Edit"
              :disabled="readonly"
              @click="openEditDialog(row)"
          >修改
          </el-button>
          <el-button
              link
              type="danger"
              icon="Delete"
              :disabled="readonly || row.columnCategory === 'SYSTEM'"
              @click="removeField(row.localId)"
          >删除
          </el-button>
        </template>
      </qt-table>
    </qt-wrap>

    <model-field-form-dialog
        ref="fieldDialogRef"
        :current-model-id="currentModelId"
        :structure-readonly="published"
        :original-fields="originalFields"
        :published-fields="publishedFields"
        :readonly="readonly"
        :all-fields="modelValue"
        :data-type-options="dataTypeOptions"
        :reference-options="loadedReferenceOptions"
        :project-options="projectOptions"
        :source-fields="normalFields"
        @confirm="saveDialogField"
    />
  </div>
</template>

<script setup name="ModelFieldsEditor">

import {useDict} from "@/utils/dict";

/**
 * @description 模型字段编辑器，负责分类、排序与字段配置。
 * @prop {Array} modelValue 弹窗或表单的双向绑定值
 * @prop {String} title 弹窗标题
 * @prop {Boolean} compact compact
 * @prop {Boolean} readonly 是否只读
 * @prop {Boolean} published published
 * @prop {Array} originalFields originalFields
 * @prop {Array} publishedFields publishedFields
 * @prop {String} initialKind 初始字段分类
 * @prop {Array} allowedKinds allowedKinds
 * @prop {Array} dataTypeOptions dataTypeOptions
 * @prop {Array} referenceOptions referenceOptions
 * @prop {Array} projectOptions projectOptions
 * @prop {[Number, String]} currentModelId 当前模型 ID
 * @emits update:modelValue 同步双向绑定值
 * @emits change change
 */
import {Lock} from "@element-plus/icons-vue";
import {ElMessage} from "element-plus";

import {inject} from "vue";
import ModelFieldFormDialog from "./modelFieldFormDialog.vue";
import QtWrap from "@/components/QtWrap/index.vue";
import QtTable from "@/components/QtTable/index.vue";
import {listType} from "@/api/model/dict/type/type";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const confirmFieldRestoration = inject("modelFieldRestoration");

const {
  model_column_category,
  model_field_property,
  model_field_node_rule,
  model_field_display_mode
} = useDict('model_column_category', 'model_field_property', 'model_field_node_rule', 'model_field_display_mode');
const fieldPropertyLabel = value => model_field_property.value.find(item => item.value === value)?.label || value;

/** 字段编辑器参数；选项由父组件注入，选项读取系统字典。 */
const props = defineProps({
  modelValue: {type: Array, default: () => []},
  title: {type: String, default: ""},
  compact: {type: Boolean, default: false},
  readonly: {type: Boolean, default: false},
  // 发布限制只作用于有原始记录的字段，本次新增字段正常填写。
  published: {type: Boolean, default: false},
  originalFields: {type: Array, default: () => []},
  publishedFields: {type: Array, default: () => []},
  initialKind: {type: String, default: 'BUSINESS'},
  allowedKinds: {
    type: Array,
    default: () => ['BUSINESS', 'RELATION', 'CLASS', 'SYSTEM']
  },
  dataTypeOptions: {type: Array, default: () => []},
  referenceOptions: {type: Array, default: () => []},
  projectOptions: {type: Array, default: () => []},
  currentModelId: {type: [Number, String], default: null},
});
/** 同步字段列表，并通知父组件字段配置发生变化。 */
const emit = defineEmits(["update:modelValue", "change"]);

/** 字段分类定义，value 与后端字段类型标识保持独立映射。 */
const kindOptions = model_column_category;
/** 当前字段分类页签。 */
const activeKind = ref(props.initialKind);
/** 字段表单弹窗实例，用于统一处理新增和编辑。 */
const fieldDialogRef = ref();
const normalTableRef = ref();
const editorRef = ref();
const loadedReferenceOptions = ref([]);
/** 根据使用场景筛选允许展示的字段分类。 */
const visibleKinds = computed(() => kindOptions.value.filter(item => props.allowedKinds.includes(item.value)));
/** 紧凑模式只展示指定分类，不再显示分类页签。 */
const showKindTabs = computed(() => !props.compact && visibleKinds.value.length > 1);
/** 当前分类对应的字段列表。 */
const visibleFields = computed(() => props.modelValue.filter(item => item.columnCategory === activeKind.value));
/** 普通字段可作为自动编码参数的绑定来源。 */
const normalFields = computed(() => props.modelValue.filter(item => item.columnCategory === 'BUSINESS'));

const businessColumns = computed(() => [
  {label: "编号", prop: "localId", slot: "rowIndex", width: 60, sortable: true},
  {label: fieldPropertyLabel('code'), prop: "code", slot: "businessCode", minWidth: 190, align: "left"},
  {label: fieldPropertyLabel('name'), prop: "name", slot: "businessName", minWidth: 170, align: "left"},
  {label: fieldPropertyLabel('dataType'), prop: "dataType", slot: "businessDataType", minWidth: 125},
  {label: "属性长度", prop: "localId", slot: "dataLength", minWidth: 100},
  {label: "小数", prop: "localId", slot: "dataScale", minWidth: 65},
  {label: "默认值", prop: "localId", slot: "defaultValue", minWidth: 132},
  {label: fieldPropertyLabel('requiredFlag'), prop: "requiredFlag", slot: "requiredFlag", minWidth: 60},
  {label: fieldPropertyLabel('uniqueFlag'), prop: "uniqueFlag", slot: "uniqueFlag", minWidth: 60},
  {label: fieldPropertyLabel('nameFlag'), prop: "nameFlag", slot: "nameFlag", minWidth: 80},
  {label: "参考数据", prop: "localId", slot: "reference", minWidth: 190, align: "left"},
]);

const fieldTableStore = reactive({
  config: {
    fullContent: false,
    notPagination: true,
    actions: {show: true, table: {show: true, search: false, refresh: true, columns: true}},
    table: {
      class: "field-design-table",
      scrollbarAlwaysOn: false,
      rowKey: "localId",
    },
  },
  columns: [],
  func: async () => ({data: visibleFields.value}),
  params: {},
});

watchEffect(() => {
  const actionColumn = props.readonly
      ? []
      : [{label: "操作", prop: "localId", slot: "action", minWidth: 180, fixed: "right"}];

  if (activeKind.value === 'BUSINESS') {
    fieldTableStore.columns = [...businessColumns.value, ...actionColumn];
    return;
  }
  if (['RELATION', 'CLASS'].includes(activeKind.value)) {
    fieldTableStore.columns = [
      {label: "编号", prop: "localId", slot: "rowIndex", width: 60, sortable: true},
      {label: fieldPropertyLabel('code'), prop: "code", minWidth: 190, align: "left"},
      {label: fieldPropertyLabel('name'), prop: "name", minWidth: 170, align: "left"},
      {
        label: activeKind.value === 'CLASS' ? "分类模型" : "关联模型",
        prop: "relatedModelName",
        minWidth: 150,
        align: "left",
      },
      {label: "关联键字段", prop: "relatedCodeName", minWidth: 140, align: "left"},
      {label: "展示字段", prop: "displayCodeName", minWidth: 140, align: "left"},
      {label: "存储类型", prop: "localId", slot: "storageType", minWidth: 120},
      {label: fieldPropertyLabel('requiredFlag'), prop: "requiredFlag", slot: "requiredLabel", minWidth: 90},
      ...(activeKind.value === 'CLASS' ? [
        {label: "节点选择规则", prop: "nodeRule", slot: "nodeRule", minWidth: 120},
        {label: "展示方式", prop: "displayMode", slot: "displayMode", minWidth: 110},
      ] : []),
      ...actionColumn,
    ];
    return;
  }
  fieldTableStore.columns = [
    {label: "编号", prop: "localId", slot: "rowIndex", width: 60, sortable: true},
    {label: fieldPropertyLabel('code'), prop: "code", minWidth: 190, align: "left"},
    {label: fieldPropertyLabel('name'), prop: "name", minWidth: 170, align: "left"},
    {label: fieldPropertyLabel('dataType'), prop: "dataType", minWidth: 130},
    {label: "系统维护", prop: "localId", slot: "systemLock", minWidth: 130},
    {
      label: fieldPropertyLabel('description'),
      prop: "description",
      minWidth: 240,
      align: "left",
      showOverflowTooltip: true
    },
  ];
});

onMounted(async () => {
  const response = await listType({pageNum: 1, pageSize: 1000});
  // 在接口返回的参考数据前追加空值选项，明确表示字段不关联参考数据。
  loadedReferenceOptions.value = [{id: null, name: "不关联"}, ...(response.data?.rows || response.data || [])];
});
// qt-table保留自己的行数据，恢复字段即使行数不变也要刷新，才能显示原字段的类型和配置。
// 等父组件接收新字段列表后再刷新；切换分类页签也会更新visibleFields。
watch(visibleFields, () => {
  normalTableRef.value?.getList();
}, {flush: 'post'});

// 父组件切换子模型字段页签时，同步编辑器的当前字段分类。
watch(() => props.initialKind, value => activeKind.value = value);

// 只按成功发布字段的ID识别旧字段；新增字段即使保存过草稿、有了ID，也不受发布限制。
function originalField(row) {
  return row.id == null ? null : props.originalFields.find(item => item.id != null && String(item.id) === String(row.id));
}

// 按属性执行已确认的表单修改规则，不禁用整个页面。
function isReadonly(row, property) {
  if (props.readonly || row.readonly || row.columnCategory === 'SYSTEM') {
    return true;
  }
  if (['RELATION', 'CLASS'].includes(row.columnCategory) && [
    'dataType',
    'dataLength',
    'dataScale',
    'storageType',
  ].includes(property)) {
    return true;
  }
  if (property === 'requiredFlag' && (row.nameFlag === true || row.primaryKeyFlag === true || originalField(row)?.primaryKeyFlag === true)) {
    return true;
  }
  const old = originalField(row);
  if (!props.published || !old) {
    return false;
  }
  // 已发布的非必填字段不能选为名称字段，已取消必填的字段需先恢复必填。
  if (property === 'nameFlag') {
    return old.requiredFlag !== true || row.requiredFlag !== true;
  }
  // 原本必填可以取消再恢复，原本非必填不能收紧约束。
  if (property === 'requiredFlag') {
    return old.requiredFlag !== true;
  }
  if ([
    'name',
    'description',
    'sortOrder',
    'dictTypeId',
    'encodingMode',
    'encodingVersionId',
    'encodingRuleConfig',
    'displayCode',
    'displayMode',
    'defaultValue',
  ].includes(property)) {
    return false;
  }
  return property !== 'dataLength' || row.dataType !== 'STRING';
}

// 长度下限固定为打开页面时的原值，不能跟着用户本次输入增大。
function minimumLength(row) {
  const old = originalField(row);
  if (props.published && old?.dataType === 'STRING' && row.dataType === 'STRING') {
    return old.dataLength == null || old.dataLength === '' ? 255 : Number(old.dataLength);
  }
  return 1;
}

// 在数字输入框按 min 自动恢复前检查提交值，避免缩短时无提示。
function handleLengthInputChange(row, event) {
  const value = event.target.value;
  if (props.published && originalField(row)?.dataType === 'STRING'
      && row.dataType === 'STRING' && value !== '' && Number(value) < minimumLength(row)) {
    ElMessage.warning('已发布字段长度不允许缩小');
  }
}

// 沿用字段编辑器原有基础检查，不调用新增的统一校验。
function validateFields() {
  const configurableFields = props.modelValue.filter(field => field.columnCategory !== 'SYSTEM');
  if (!configurableFields.length) {
    activeKind.value = 'BUSINESS';
    ElMessage.warning("请至少添加一个字段");
    return false;
  }

  // 系统字段虽然不参与编辑校验，但其编码仍需参与重复性判断。
  const fieldCodes = new Set(
      props.modelValue
          .filter(field => field.columnCategory === 'SYSTEM' && field.code)
          .map(field => String(field.code).trim().toLowerCase())
  );
  const rowCounts = new Map();
  for (const field of configurableFields) {
    const kind = field.columnCategory || 'BUSINESS';
    const rowNumber = (rowCounts.get(kind) || 0) + 1;
    rowCounts.set(kind, rowNumber);
    const kindLabel = kindOptions.value.find(item => item.value === kind)?.label || "字段";
    const warnInvalidField = message => {
      activeKind.value = kind;
      ElMessage.warning(`${kindLabel}第${rowNumber}行${message}`);
      return false;
    };
    const fieldName = String(field.name ?? "").trim();
    const fieldCode = String(field.code ?? "").trim();

    if (!fieldName) {
      return warnInvalidField("字段中文名称不能为空");
    }
    if (fieldName.length > 64) {
      return warnInvalidField("字段中文名称不能超过64个字符");
    }
    if (!/^[\u4e00-\u9fa5A-Za-z0-9_-]+$/.test(fieldName)) {
      return warnInvalidField("字段中文名称支持中文、英文字符、数字、下划线和短横线");
    }
    if (!fieldCode) {
      return warnInvalidField("字段英文名称不能为空");
    }
    if (fieldCode.length > 64) {
      return warnInvalidField("字段英文名称不能超过64个字符");
    }
    if (!/^[A-Za-z0-9_]+$/.test(fieldCode)) {
      return warnInvalidField("字段编码支持英文字母、数字和下划线");
    }
    const normalizedCode = fieldCode.toLowerCase();
    if (fieldCodes.has(normalizedCode)) {
      return warnInvalidField(`字段英文名称“${fieldCode}”不能重复`);
    }
    fieldCodes.add(normalizedCode);
    if (!field.dataType) {
      return warnInvalidField("数据类型不能为空");
    }
    // 字符串长度必须为整数，已发布字段还不能小于原长度。
    if (field.dataType === 'STRING') {
      const length = field.dataLength == null || field.dataLength === '' ? 255 : Number(field.dataLength);
      if (!Number.isSafeInteger(length) || length < minimumLength(field)) {
        return warnInvalidField(`属性长度必须是不小于${minimumLength(field)}`);
      }
    }
    if (!field.columnCategory) {
      return warnInvalidField("字段类别不能为空");
    }

    field.name = fieldName;
    field.code = fieldCode;
  }
  return true;
}

// 先切到出错的字段分类，等表格渲染完，再滚动并聚焦到出错输入框。
async function focusError(error) {
  if (error.kind) {
    activeKind.value = error.kind;
  }
  await nextTick();
  await normalTableRef.value?.getList?.();
  await nextTick();
  const localId = error.fieldLocalId || props.modelValue.find(item => item.id != null && String(item.id) === String(error.fieldId))?.localId;
  const controls = [...(editorRef.value?.querySelectorAll('[data-model-field]') || [])]
      .filter(element => element.dataset.modelField === localId);
  const target = controls.find(element => element.dataset.modelProperty === error.property) || controls[0];
  target?.scrollIntoView?.({block: 'center'});
  target?.querySelector('input,textarea,button')?.focus();
}

/** 创建普通字段默认结构，列表新增与弹窗编辑使用同一套字段模型。 */
function createNormalField() {
  const hasNameField = props.modelValue.some(item => item.nameFlag === true);
  return {
    localId: `field-${Date.now()}`,
    columnCategory: 'BUSINESS',
    name: "",
    code: "",
    dataType: (props.dataTypeOptions[0]?.value || props.dataTypeOptions[0]) || 'STRING',
    requiredFlag: !hasNameField,
    uniqueFlag: false,
    nameFlag: !hasNameField,
    defaultValue: "",
    dataLength: 255,
    dataScale: null,
    dictTypeId: null,
    encodingMode: 'MANUAL',
    encodingVersionId: null,
  };
}

/** 新增{{ model_column_category.find(item => item.value === 'BUSINESS')?.label }}时直接向列表追加一行。 */
function addNormalField() {
  if (props.readonly) {
    return;
  }
  activeKind.value = 'BUSINESS';
  updateFields([...props.modelValue, createNormalField()]);
}

/** 打开关联字段或分类字段新增弹窗。 */
function openAddDialog(kind) {
  if (props.readonly) {
    return;
  }
  activeKind.value = kind;
  fieldDialogRef.value?.openDialog(kind);
}

/** 打开当前行对应类型的编辑弹窗。 */
function openEditDialog(row) {
  if (props.readonly) {
    return;
  }
  fieldDialogRef.value?.openDialog(row.columnCategory, row);
}

/** 保存弹窗返回的字段，并区分新增与编辑。 */
function saveDialogField(field) {
  if (props.readonly) {
    return;
  }
  // 仅检查当前弹窗字段，不能借名称标记收紧已发布字段的必填约束。
  if (field.nameFlag && isReadonly(field, 'nameFlag')) {
    ElMessage.warning('已发布的非必填字段不能设为名称字段，请先选择必填字段');
    return;
  }
  if (field.nameFlag && (!props.published || !originalField(field))) {
    field.requiredFlag = true;
  }
  // 切换名称字段时取消其他字段的名称标记，允许从原有字段切到新增字段。
  const index = props.modelValue.findIndex(item => item.localId === field.localId);
  const next = props.modelValue.map(item => ({...item}));
  if (index >= 0) {
    next.splice(index, 1, field);
  } else {
    next.push(field);
  }
  // 不通过名称字段联动修改其他已物化字段的必填约束。
  if (field.nameFlag) {
    next.forEach(item => {
      if (item.localId !== field.localId) {
        item.nameFlag = false;
      }
    });
  }
  activeKind.value = field.columnCategory;
  updateFields(next);
}

/** 删除前仅在前端拦截唯一字段和名称字段。 */
function removeField(localId) {
  const field = props.modelValue.find(item => item.localId === localId);
  if (!field || props.readonly || field.columnCategory === 'SYSTEM') {
    return;
  }
  if (field.uniqueFlag) {
    ElMessage.warning('唯一字段不能删除');
    return;
  }
  if (field.nameFlag) {
    ElMessage.warning('名称字段不能删除，请先切换名称字段');
    return;
  }
  updateFields(props.modelValue.filter(item => item.localId !== localId));
}

/** 设置名称字段，同一模型字段集合中只允许存在一个名称字段。 */
function setNameField(row) {
  if (isReadonly(row, 'nameFlag')) {
    return;
  }
  props.modelValue.forEach(item => {
    item.nameFlag = item.localId === row.localId;
    // 新增、未发布字段自动必填；已发布字段必须原本和当前都必填才能选中。
    if (item.nameFlag && (!props.published || !originalField(item))) {
      item.requiredFlag = true;
    }
  });
  emitChange();
}

/** 名称字段不能取消必填；其他字段正常编辑，保存时由后端检查变更范围。 */
function handleRequiredChange(row) {
  // 正常操作由禁用状态拦截，这里也避免控件联动把名称字段改成非必填。
  if (row.nameFlag && !row.requiredFlag) {
    row.requiredFlag = true;
    ElMessage.warning('请先切换名称字段，再取消当前字段的必填');
    return;
  }
  if (isReadonly(row, 'requiredFlag')) {
    return;
  }
  emitChange();
}

/** 选择参考数据后带入名称和编码，系统只读字段不参与联动。 */
function handleReferenceChange(row) {
  const reference = row.dictTypeId == null ? null : loadedReferenceOptions.value.find(item => item.id === row.dictTypeId);
  if (reference) {
    if (!isReadonly(row, 'code')) {
      row.code = reference.code;
    }
    if (!isReadonly(row, 'name')) {
      row.name = reference.name;
    }
  }
  emitChange();
}

/** 内联切换类型时同步合理的长度和精度默认值。 */
function handleInlineTypeChange(row) {
  if (isReadonly(row, 'dataType')) {
    return;
  }
  // 切换类型后不沿用上一个类型的默认值，并同步长度和精度配置。
  row.defaultValue = "";
  if (row.dataType === 'STRING') {
    row.dataLength = 255;
    row.dataScale = null;
  }
  if (row.dataType === 'DECIMAL') {
    row.dataLength = row.dataLength || 18;
    row.dataScale = 2;
  }
  if (!['STRING', 'DECIMAL'].includes(row.dataType)) {
    row.dataLength = null;
    row.dataScale = null;
  }
  emitChange();
}

/** 格式化字段长度与精度，用于列表展示和关联字段存储类型回退。 */
function formatSize(row) {
  if (row.dataType === 'DECIMAL' && row.dataLength) {
    return `${row.dataLength},${row.dataScale ?? 0}`;
  }
  return row.dataLength ?? "-";
}

/** 使用新数组同步父组件，避免直接替换只读 props。 */
function updateFields(next) {
  emit("update:modelValue", next);
  emit("change", next);
}

// 行内变化只同步字段列表；新增统一校验模块已按要求移除。
async function handleFieldChange(row, property) {
  if (property === 'code' && row.columnCategory === 'BUSINESS') {
    const result = await confirmFieldRestoration(row, props.modelValue, props.publishedFields);
    if (result.restored) {
      activeKind.value = result.field.columnCategory;
      updateFields(props.modelValue.map(item => item.localId === row.localId ? result.field : item));
      return;
    }
  }
  emitChange();
}

/** 将当前字段列表同步给父组件，并触发模型校验失效逻辑。 */
function emitChange() {
  updateFields([...props.modelValue]);
}

defineExpose({validateFields, focusError});
</script>

<style scoped lang="scss">
.system-lock-tag {
  gap: var(--qt-space-1);
}

:deep(.default-value-control) {
  width: 100% !important;
}

// 日期组件把空 prefix-icon 当作使用默认图标，这里只隐藏日期默认值输入框的前缀区域，右侧清空按钮保留。
:deep(.default-value-control.el-date-editor .el-input__prefix) {
  display: none;
}

.data-actions {
  :deep(.el-button) {
    height: var(--qt-action-button-height);
    padding: var(--qt-action-button-padding);
    font-size: var(--qt-font-size-small);
  }
}
</style>
