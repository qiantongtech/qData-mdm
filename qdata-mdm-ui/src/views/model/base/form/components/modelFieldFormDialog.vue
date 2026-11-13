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
      :title="dialogTitle"
      draggable
      destroy-on-close
      width="1000px"
  >
    <el-form
        :key="formSession"
        ref="fieldFormRef"
        class="column-form"
        :model="form"
        :rules="formRules"
        label-width="120px"
        :validate-on-rule-change="false"
        :disabled="readonly"
        @submit.prevent
    >
      <div v-if="fieldKind === 'BUSINESS'" class="h2-titles row-full">基础配置</div>
      <!-- 名称和编码由三种字段共用，按两列展示。 -->
      <qt-form-item :label="fieldPropertyLabel('name')" prop="name">
        <el-input v-model="form.name"
                  placeholder="请输入字段名称，支持中文、英文、数字、下划线"
                  :disabled="propertyReadonly('name')"
        />
      </qt-form-item>
      <qt-form-item :label="fieldPropertyLabel('code')" prop="code">
        <el-input
            v-model="form.code"
            placeholder="请输入字段编码，仅支持英文字母、数字和下划线。"
            :disabled="propertyReadonly('code')"
            @change="handleCodeChange"
        />
      </qt-form-item>

      <!-- 普通字段基础配置。 -->
      <template v-if="fieldKind === 'BUSINESS'">
        <qt-form-item :label="fieldPropertyLabel('dataType')" prop="dataType">
          <el-select
              v-model="form.dataType"
              placeholder="请选择数据类型"
              :disabled="propertyReadonly('dataType')"
              @change="handleDataTypeChange"
          >
            <el-option
                v-for="item in dataTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
                :disabled="item.unresolved"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('defaultValue')">
          <el-select
              v-if="form.dataType === 'BOOLEAN'"
              v-model="form.defaultValue"
              placeholder="请选择默认值"
              clearable
              class="full-width"
              :disabled="propertyReadonly('defaultValue')"
          >
            <el-option label="是" value="true"/>
            <el-option label="否" value="false"/>
          </el-select>
          <el-date-picker
              v-else-if="form.dataType === 'DATE'"
              v-model="form.defaultValue"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择日期"
              class="full-width"
              :disabled="propertyReadonly('defaultValue')"
          />
          <el-date-picker
              v-else-if="form.dataType === 'DATETIME'"
              v-model="form.defaultValue"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择日期时间"
              class="full-width"
              :disabled="propertyReadonly('defaultValue')"
          />
          <!-- INTEGER 使用 number 输入，DECIMAL 保持文本；不用 v-model.number，避免大整数被转成不精确的数字。 -->
          <el-input
              v-else-if="form.dataType === 'INTEGER' || form.dataType === 'DECIMAL'"
              v-model="form.defaultValue"
              class="full-width"
              :type="form.dataType === 'INTEGER' ? 'number' : 'text'"
              :inputmode="form.dataType === 'INTEGER' ? 'numeric' : 'decimal'"
              placeholder="请输入默认值"
              :disabled="propertyReadonly('defaultValue')"
          />
          <el-input
              v-else
              v-model="form.defaultValue"
              placeholder="请输入默认值"
              :disabled="propertyReadonly('defaultValue')"
          />
        </qt-form-item>
        <qt-form-item v-if="form.dataType === 'STRING' || form.dataType === 'DECIMAL'"
                      label="长度" prop="dataLength"
                      :class="{ 'row-full': form.dataType === 'DECIMAL' }"
                      :tip="form.dataType === 'DECIMAL' ? '设置数值的最大总位数。' : undefined"
        >
          <el-input-number
              v-model="form.dataLength"
              :min="minimumLength"
              :disabled="propertyReadonly('dataLength') || !showLength"
              controls-position="right"
              class="full-width"
              @change.capture="handleLengthInputChange"
          />
        </qt-form-item>
        <qt-form-item v-if="form.dataType === 'DECIMAL'" class="row-full" label="精度">
          <el-input-number
              v-model="form.dataScale"
              :min="0"
              controls-position="right"
              class="full-width"
              :disabled="propertyReadonly('dataScale')"
          />
        </qt-form-item>
        <qt-form-item label="字段约束" :class="{ 'row-full': form.dataType !== 'STRING' }">
          <el-checkbox v-model="form.requiredFlag" :disabled="propertyReadonly('requiredFlag')">必填
          </el-checkbox>
          <el-checkbox v-model="form.uniqueFlag" :disabled="propertyReadonly('uniqueFlag')">唯一
          </el-checkbox>
          <el-checkbox v-model="form.nameFlag" :disabled="propertyReadonly('nameFlag')"
                       @change="handleNameFlagChange"
          >设为名称字段
          </el-checkbox>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('dictTypeId')"
                      tip="可选择关联的参考数据，用于规范字段取值范围。"
        >
          <!-- 保留 dictTypeId 的 null 语义，同时让空值状态在表单中显示“不关联”。 -->
          <el-select
              v-model="form.dictTypeId"
              v-loading="loadingReferences"
              :placeholder="form.dictTypeId == null ? '不关联' : '请选择参考数据'"
              :disabled="propertyReadonly('dictTypeId') || (form.encodingMode === 'AUTOMATIC')"
              clearable
              :value-on-clear="null"
              @change="handleReferenceChange"
          >
            <el-option
                v-for="item in referenceOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </qt-form-item>

        <div class="h2-titles row-full">编码配置</div>
        <qt-form-item :label="fieldPropertyLabel('encodingMode')" class="row-full">
          <el-radio-group v-model="form.encodingMode" :disabled="propertyReadonly('encodingMode')"
                          @change="handleEncodingModeChange"
          >
            <el-radio v-for="item in model_encoding_mode" :key="item.value" :value="item.value">{{
                item.label
              }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <template v-if="form.encodingMode === 'AUTOMATIC'">
          <qt-form-item label="目标编码字段">
            <el-input
                :model-value="form.name || form.code"
                placeholder="自动编码时，生成的编码将写入此字段。"
                disabled
            />
          </qt-form-item>
          <qt-form-item :label="fieldPropertyLabel('encodingVersionId')" prop="encodingVersionId">
            <el-select
                v-model="form.encodingVersionId"
                v-loading="loadingEncodings"
                placeholder="请选择编码方案"
                :disabled="propertyReadonly('encodingVersionId')"
                @change="handleEncodingChange"
            >
              <el-option
                  v-for="item in encodingOptions"
                  :key="item.currentVersionId"
                  :label="item.name"
                  :value="item.currentVersionId"
              />
            </el-select>
          </qt-form-item>
          <qt-form-item label="规则预览" class="row-full">
            <el-input
                :model-value="rulePreview"
                type="textarea"
                :rows="2"
                disabled
            />
          </qt-form-item>
          <qt-form-item v-if="encodingBindings.length" label="字段参数绑定" class="row-full">
            <qt-table v-bind="encodingBindingTableStore">
              <template #parameterName="{ row }">{{ row.parameterName }}</template>
              <template #ruleType="{ row }">{{ encodingRuleLabels[row.ruleType] || row.ruleType }}</template>
              <template #sourceField="{ row }">
                <el-select v-model="row.sourceFieldId"
                           :placeholder="row.ruleType === 'REFERENCE_MAPPING' ? '请选择参考数据项' : '请选择来源字段'"
                           clearable
                >
                  <el-option
                      v-for="item in (row.ruleType === 'REFERENCE_MAPPING' ? row.referenceDataOptions : getBindingFieldOptions(row.ruleType))"
                      :key="row.ruleType === 'REFERENCE_MAPPING' ? item.id : item.localId"
                      :label="row.ruleType === 'REFERENCE_MAPPING' ? item.name : `${item.name || '未命名字段'}（${item.code || '-'}）`"
                      :value="row.ruleType === 'REFERENCE_MAPPING' ? item.id : item.localId"
                  />
                </el-select>
              </template>
              <template #empty>暂无数据</template>
            </qt-table>
          </qt-form-item>
        </template>
      </template>

      <!-- 关联字段与分类字段共用模型关联配置。 -->
      <template v-else>
        <qt-form-item label="主数据域" prop="projectId">
          <el-select
              v-model="form.projectId"
              placeholder="请选择主数据域"
              :disabled="propertyReadonly('relatedModelId')"
              @change="handleProjectChange"
          >
            <template #label="{ label }">
              <div v-if="selectedProject" class="source-option">
                <img :src="getProjectIcon(selectedProject.icon)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ selectedProject.name }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option
                v-for="item in projectOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            >
              <div class="source-option">
                <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ item.name }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item label="模型目录" prop="layerId">
          <el-tree-select
              v-model="form.layerId"
              :data="layerOptions"
              :props="layerTreeProps"
              check-strictly
              clearable
              :disabled="propertyReadonly('relatedModelId') || (!form.projectId)"
              placeholder="请选择模型目录"
              @change="handleLayerChange"
          />
        </qt-form-item>
        <qt-form-item :label="fieldKind === 'CLASS' ? '分类模型' : '关联模型'"
                      prop="relatedModelId"
                      :tip="fieldKind === 'CLASS' ? '选择用于定义分类数据结构的主数据模型。' : undefined"
        >
          <el-select
              v-model="form.relatedModelId"
              v-loading="loadingModels"
              :placeholder="fieldKind === 'CLASS' ? '请选择分类模型' : '请选择关联模型'"
              :disabled="propertyReadonly('relatedModelId')"
              @change="handleRelatedModelChange"
          >
            <template #label="{ label }">
              <div v-if="selectedRelatedModel" class="source-option">
                <img :src="getModelIcon(selectedRelatedModel)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ selectedRelatedModel.name }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <template v-for="item in relatedModelOptions">
              <el-option
                  v-if="item.id !== props.currentModelId"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              >
                <div class="source-option">
                  <img :src="getModelIcon(item)" alt="" class="source-option__icon"/>
                  <span class="text-ellipsis">{{ item.name }}</span>
                </div>
              </el-option>
            </template>
          </el-select>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('relatedCode')" prop="relatedCode"
                      :tip="fieldKind === 'CLASS' ? '选择用于关联分类数据的键字段。' : undefined"
        >
          <el-select
              v-model="form.relatedCode"
              :loading="loadingRelatedFields"
              placeholder="请选择关联键字段"
              :disabled="propertyReadonly('relatedCode')"
              @change="handleKeyFieldChange"
          >
            <el-option
                v-for="item in keyFieldOptions"
                :key="item.key"
                :label="item.label"
                :value="item.value"
                :disabled="item.unresolved"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('displayCode')" prop="displayCode">
          <el-select
              v-model="form.displayCode"
              :loading="loadingRelatedFields"
              placeholder="请选择展示字段"
              :disabled="propertyReadonly('displayCode')"
              @change="syncSelectedLabels"
          >
            <el-option
                v-for="item in displayFieldOptions"
                :key="item.code"
                :label="item.code"
                :value="item.code"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('storageType')">
          <!-- 存储类型由关联键自动带入，只展示结果，不接受用户手动输入。 -->
          <el-input :model-value="form.storageType" disabled
                    :placeholder="fieldKind === 'CLASS'
                          ? '按关联键字段的数据类型、长度和精度自动确定'
                          : '按关联键字段的数据类型、长度和精度自动确定'"
          />
        </qt-form-item>
        <template v-if="fieldKind === 'CLASS'">
          <qt-form-item :label="fieldPropertyLabel('nodeRule')" prop="nodeRule">
            <el-select
                v-model="form.nodeRule"
                placeholder="请选择节点选择规则"
                :disabled="propertyReadonly('nodeRule')"
                @change="syncSelectedLabels"
            >
              <el-option
                  v-for="item in model_field_node_rule"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </qt-form-item>
          <qt-form-item :label="fieldPropertyLabel('displayMode')" prop="displayMode">
            <el-select
                v-model="form.displayMode"
                placeholder="请选择展示方式"
                :disabled="propertyReadonly('displayMode')"
                @change="syncSelectedLabels"
            >
              <el-option
                  v-for="item in model_field_display_mode"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </qt-form-item>
        </template>
        <qt-form-item :label="fieldPropertyLabel('requiredFlag')">
          <el-radio-group v-model="form.requiredFlag" :disabled="propertyReadonly('requiredFlag')">
            <el-radio :value="true">是</el-radio>
            <el-radio :value="false">否</el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item :label="fieldPropertyLabel('description')" class="row-full">
          <el-input
              v-model="form.description"
              type="textarea"
              maxlength="256个字符"
              show-word-limit
              :min-height="192"
              :placeholder="fieldKind === 'CLASS'
                    ? '请输入该字段记录的业务分类及用途，例如记录供应商所属行业，用于按行业筛选和统计。'
                    : '请输入该字段的关联对象及用途，例如关联供应商主数据，用于录入时选择供应商并显示其名称。'"
              :disabled="propertyReadonly('description')"
          />
        </qt-form-item>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="ModelFieldFormDialog">

import {useDict, loadDict} from "@/utils/dict";

/**
 * @description 模型字段新增与编辑弹窗。
 * @prop {Array} dataTypeOptions dataTypeOptions
 * @prop {Array} projectOptions projectOptions
 * @prop {Array} sourceFields sourceFields
 * @prop {[Number, String]} currentModelId 当前模型 ID
 * @prop {Boolean} structureReadonly structureReadonly
 * @prop {Array} originalFields originalFields
 * @prop {Array} publishedFields publishedFields
 * @prop {Boolean} readonly 是否只读
 * @prop {Array} allFields allFields
 * @emits confirm confirm
 */
import {listAvailableEncoding} from "@/api/model/encoding/encoding";
import {listVersion} from "@/api/model/encoding/version";
import {listEncodingItems} from "@/api/model/encoding/item";
import {listValidData} from "@/api/model/dict/data/data";
import {getType, listType} from "@/api/model/dict/type/type";
import {listRelatedModels} from "@/api/model/base/base";
import {treeLayer} from "@/api/model/layer/layer";
import {inject} from "vue";
import {getProjectIcon, getModelIcon} from "@/utils/imageUrl";
import {trimText, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";

const confirmFieldRestoration = inject("modelFieldRestoration");

const {
  model_field_node_rule,
  model_field_display_mode,
  model_column_category,
  model_encoding_mode,
  model_field_property,
  model_encoding_rule_type
} = useDict('model_field_node_rule', 'model_field_display_mode', 'model_column_category', 'model_encoding_mode', 'model_field_property', 'model_encoding_rule_type');
const nodeRuleLabel = value => model_field_node_rule.value.find(item => item.value === value)?.label || '';
const displayModeLabel = value => model_field_display_mode.value.find(item => item.value === value)?.label || '';
const fieldPropertyLabel = value => model_field_property.value.find(item => item.value === value)?.label || value;

const {proxy} = getCurrentInstance();
/** 弹窗选项参数均由字段编辑器传入，保持主模型和子模型复用能力。 */
const props = defineProps({
  dataTypeOptions: {type: Array, default: () => []},
  projectOptions: {type: Array, default: () => []},
  sourceFields: {type: Array, default: () => []},
  currentModelId: {type: [Number, String], default: null},
  // 这里只接收成功发布的字段，保存过草稿的新字段仍可修改类型等属性。
  structureReadonly: {type: Boolean, default: false},
  originalFields: {type: Array, default: () => []},
  publishedFields: {type: Array, default: () => []},
  readonly: {type: Boolean, default: false},
  allFields: {type: Array, default: () => []},
});
/** 确认后向字段编辑器返回完整字段对象。 */
const emit = defineEmits(["confirm"]);

/** 弹窗显示状态和当前字段类型。 */
const visible = ref(false);
const fieldKind = ref('BUSINESS');
/** 表单实例和提交状态。 */
const fieldFormRef = ref();
// 每次打开创建新的表单实例，避免重置旧控件时触发 change 校验并遗留错误。
const formSession = ref(0);
const submitting = ref(false);
/** 编码方案、规则和参数绑定数据。 */
const encodingOptions = ref([]);
const encodingRules = ref([]);
const encodingBindings = ref([]);
// 使用 table.data 保留原数组的响应式绑定，避免切换方案后表格仍持有旧数据。
const encodingBindingTableStore = computed(() => ({
  config: {initResquest: false, notPagination: true, table: {data: encodingBindings.value, border: true}},
  columns: [
    {label: "方案参数", prop: "parameterName", slot: "parameterName", minWidth: 130, align: "left"},
    {label: "规则类型", prop: "ruleTypeLabel", slot: "ruleType", minWidth: 130, align: "left"},
    {label: "绑定来源字段", slot: "sourceField", minWidth: 220, align: "left"},
  ],
  func: async () => ({data: encodingBindings.value}),
}));
// 参考数据映射规则的 fixedValue 保存类型 ID，这里缓存 ID 到名称的转换结果供预览和参数回显使用。
const dictTypeNameMap = ref({});
const loadingEncodings = ref(false);
const referenceOptions = ref([]);
const loadingReferences = ref(false);
/** 关联模型及其字段候选项。 */
const relatedModelOptions = ref([]);
const layerOptions = ref([]);
/** 关联模型目录同样只允许选择叶子节点，父目录保留展开能力。 */
const layerTreeProps = {
  value: "id",
  label: "name",
  children: "children",
  disabled: data => Array.isArray(data.children) && data.children.length > 0,
};
const relatedFieldOptions = ref([]);
const loadingModels = ref(false);
const loadingRelatedFields = ref(false);

/** 编码规则名称随系统字典异步加载结果更新。 */
const encodingRuleLabels = computed(() => Object.fromEntries(
    model_encoding_rule_type.value.map(item => [item.value, item.label])
));

/** 创建字段弹窗的统一默认表单。 */
function createEmptyForm(kind = 'BUSINESS') {
  return {
    localId: null,
    columnCategory: kind || 'BUSINESS',
    name: "",
    code: "",
    dataType: (props.dataTypeOptions[0]?.value || props.dataTypeOptions[0]) || 'STRING',
    defaultValue: "",
    dataLength: 255,
    dataScale: null,
    requiredFlag: false,
    uniqueFlag: false,
    nameFlag: false,
    dictTypeId: null,
    encodingMode: 'MANUAL',
    encodingVersionId: null,
    encodingRuleConfig: null,
    encodingBindings: [],
    projectId: null,
    layerId: null,
    layerName: "",
    relatedModelId: null,
    relatedModelName: "",
    relatedCode: null,
    relatedCodeName: "",
    displayCode: null,
    displayCodeName: "",
    storageType: "",
    nodeRule: kind === 'CLASS' ? "ANY" : null,
    nodeRuleLabel: kind === 'CLASS' ? nodeRuleLabel('ANY') : "",
    displayMode: kind === 'CLASS' ? "NAME" : null,
    displayModeLabel: kind === 'CLASS' ? displayModeLabel('NAME') : "",
    description: "",
  };
}

/** 当前表单数据。 */
const form = reactive(createEmptyForm());

// 通过ID读取独立原值，弹窗编辑不能改变修改判断基准。
const originalField = computed(() => form.id == null ? null
    : props.originalFields.find(item => item.id != null && String(item.id) === String(form.id)));

// 只替换判断值，保留现有的字段修改限制。
function propertyReadonly(property) {
  if (props.readonly || form.readonly || form.columnCategory === 'SYSTEM') {
    return true;
  }

  if (
      ['RELATION', 'CLASS'].includes(form.columnCategory)
      && [
        'storageType',
        'dataType',
        'dataLength',
        'dataScale',
      ].includes(property)
  ) {
    return true;
  }

  if (
      property === 'requiredFlag'
      && (
          toBooleanFlag(form.nameFlag)
          || toBooleanFlag(form.primaryKeyFlag)
          || toBooleanFlag(originalField.value?.primaryKeyFlag)
      )
  ) {
    return true;
  }

  if (!props.structureReadonly || !originalField.value) {
    return false;
  }

  // 名称字段必须原本和当前都必填。
  if (property === 'nameFlag') {
    return !toBooleanFlag(originalField.value.requiredFlag)
        || !toBooleanFlag(form.requiredFlag);
  }

  // 已发布字段只允许放宽必填约束。
  if (property === 'requiredFlag') {
    return !toBooleanFlag(originalField.value.requiredFlag);
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

  return property !== 'dataLength'
      || form.dataType !== 'STRING';
}

/** 新增或未发布字段选为名称时自动必填，已发布的非必填字段不允许选择。 */
function handleNameFlagChange(value) {
  if (!value) {
    return;
  }
  if (propertyReadonly('nameFlag')) {
    form.nameFlag = false;
    proxy.$modal.msgWarning('已发布的非必填字段不能设为名称字段，请先选择必填字段');
    return;
  }
  if (!props.structureReadonly || !originalField.value) {
    form.requiredFlag = true;
  }
}

// 已发布字符串字段的长度不能缩小，下限始终来自独立原值。
const minimumLength = computed(() => {
  if (props.structureReadonly && originalField.value?.dataType === 'STRING' && form.dataType === 'STRING') {
    const length = originalField.value.dataLength;
    return length == null || length === '' ? 255 : Number(length);
  }
  return 1;
});

// 在数字输入框按 min 自动恢复前检查提交值，避免缩短时无提示。
function handleLengthInputChange(event) {
  const value = event.target.value;
  if (props.structureReadonly && originalField.value?.dataType === 'STRING'
      && form.dataType === 'STRING' && value !== '' && Number(value) < minimumLength.value) {
    proxy.$modal.msgWarning('已发布字段长度不允许缩小');
  }
}

// 仅检查当前弹窗的长度，不对其他字段执行全量校验。
function validateLength(_rule, value, callback) {
  if (form.dataType === 'STRING') {
    const length = value == null || value === '' ? 255 : Number(value);
    if (!Number.isSafeInteger(length) || length < minimumLength.value) {
      callback(new Error(`长度必须是不小于${minimumLength.value}的整数`));
      return;
    }
  }
  callback();
}

/** 弹窗标题根据字段类型和新增/编辑状态生成。 */
const dialogTitle = computed(() => `${form.localId ? "编辑" : "新增"}${getKindLabel(fieldKind.value)}`);
/** 字符串和小数类型显示长度输入。 */
const showLength = computed(() => ['STRING', 'DECIMAL'].includes(form.dataType));
/** 编码规则按排序拼接形成预览。 */
const rulePreview = computed(() => encodingRules.value.map(item => item.output).join(" + ") || "请选择编码方案");
/** 统一比较接口和下拉选项中的数字/字符串 ID。 */
const sameId = (left, right) => left != null && right != null && String(left) === String(right);

/** 下拉回显从现有候选项取得图标信息。 */
const selectedProject = computed(() => props.projectOptions.find(item => sameId(item.id, form.projectId)));
const selectedRelatedModel = computed(() => relatedModelOptions.value.find(item => sameId(item.id, form.relatedModelId)));
/** 关联键仅允许主数据 ID 或唯一普通字段。 */
/** 关联键字段直接取当前模型返回的 fields，选项的 key/value 统一使用字段编码。 */
const keyFieldOptions = computed(() => relatedFieldOptions.value
    .filter(item => (toBooleanFlag(item.primaryKeyFlag) || toBooleanFlag(item.uniqueFlag)
        || (item.unresolved && item.code === form.relatedCode)))
    .map(item => ({...item, key: item.code, value: item.code, label: item.code})));
/** 展示字段使用目标模型全部可展示字段。 */
const displayFieldOptions = computed(() => relatedFieldOptions.value);

/** 根据字段类型返回中文名称。 */
function getKindLabel(kind) {
  return model_column_category.value.find(item => item.value === kind)?.label || '';
}

/** 三种字段的动态校验规则。 */
const formRules = computed(() => {
  const rules = {
    code: [
      {required: true, validator: validateTrimmedRequired('字段编码不能为空', 64), trigger: 'blur'},
      {
        validator: validatePattern(
            /^[A-Za-z0-9_]+$/,
            "字段编码支持英文字母、数字和下划线"
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    name: [
      {required: true, validator: validateTrimmedRequired('字段名称不能为空', 128), trigger: 'blur'},
      {
        validator: validatePattern(
            /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
            "字段名称支持中文、英文字符、数字、下划线和短横线"
        ),
        trigger: "blur",
        transform: trimText,
      },],
  };
  // 普通字段校验数据类型；自动编码时额外要求选择编码方案。
  if (fieldKind.value === 'BUSINESS') {
    rules.dataType = [{required: true, message: "请选择数据类型", trigger: "change"}];
    rules.dataLength = [{validator: validateLength, trigger: 'change'}];
    if (form.encodingMode === 'AUTOMATIC') {
      rules.encodingVersionId = [{required: true, message: "请选择编码方案", trigger: "change"}];
    }
  } else {
    // 关联与分类字段必须完成目标模型和关联字段配置。
    rules.projectId = [{required: true, message: "请选择主数据域", trigger: "change"}];
    rules.layerId = [{required: true, message: "请选择模型目录", trigger: "change"}];
    rules.relatedModelId = [{required: true, message: "请选择模型", trigger: "change"}];
    rules.relatedCode = [{required: true, message: "请选择关联键字段", trigger: "change"}];
    rules.displayCode = [{required: true, message: "请选择展示字段", trigger: "change"}];
  }
  // 分类字段额外校验节点规则和展示方式。
  if (fieldKind.value === 'CLASS') {
    rules.nodeRule = [{required: true, message: "请选择节点选择规则", trigger: "change"}];
    rules.displayMode = [{required: true, message: "请选择展示方式", trigger: "change"}];
  }
  return rules;
});

/** 打开新增或编辑弹窗，并加载当前类型依赖的远程选项。 */
async function openDialog(kind, row = null) {
  await Promise.all([
    loadDict('model_field_node_rule'), loadDict('model_field_display_mode'),
    loadDict('model_column_category'), loadDict('model_encoding_mode'),
    loadDict('model_field_property'), loadDict('model_encoding_rule_type'),
  ]);
  formSession.value += 1;
  fieldKind.value = kind;
  resetFormData(kind, row);
  form.requiredFlag = toBooleanFlag(form.requiredFlag);
  form.uniqueFlag = toBooleanFlag(form.uniqueFlag);
  form.nameFlag = toBooleanFlag(form.nameFlag);
  encodingRules.value = [];
  // 绑定关系从持久化 JSON 恢复；兼容旧版本仍返回 encodingBindings 数组的字段数据。
  encodingBindings.value = parseEncodingRuleConfig(form.encodingRuleConfig || form.encodingBindings);
  relatedModelOptions.value = [];
  layerOptions.value = [];
  relatedFieldOptions.value = [];
  visible.value = true;
  await nextTick();
  fieldFormRef.value?.clearValidate();
  // 普通字段预加载编码方案，关联和分类字段按主数据域加载目标模型。
  if (kind === 'BUSINESS') {
    await loadReferenceOptions();
    await loadEncodingOptions();
    if (form.encodingMode === 'AUTOMATIC' && form.encodingVersionId) {
      await loadEncodingRules();
    }
  } else if (form.projectId) {
    await loadLayers(form.projectId);
    if (form.layerId) {
      await loadRelatedModels(form.projectId, form.layerId);
    }
    if (form.relatedModelId) {
      await loadRelatedFields(form.relatedModelId);
    }
  }
}

/** 清除上一次表单的全部属性，再写入默认值和当前编辑数据。 */
function resetFormData(kind, row) {
  Object.keys(form).forEach(key => delete form[key]);
  const rowData = row ? JSON.parse(JSON.stringify(row)) : {};
  Object.assign(form, createEmptyForm(kind), rowData);
  // 存储类型是展示文字，接口可能只返回类型、长度和精度；编辑时用当前字段的真实配置拼出文字。
  // 不调用选键字段的联动方法，避免回显时把已发布字段的类型覆盖成关联模型现在的类型。
  if (row && ['RELATION', 'CLASS'].includes(kind)) {
    form.storageType = formatStorageType(rowData) || rowData.storageType || "";
  }
}

/** 将历史接口中的 0/1 标记兼容为 Boolean。 */
function toBooleanFlag(value) {
  return value === true || value === 1 || value === "1" || value === "true";
}

/** 加载可用编码方案，接口字段在此处统一转换为下拉模型。 */
async function loadReferenceOptions() {
  loadingReferences.value = true;
  try {
    const response = await listType({pageNum: 1, pageSize: 1000});
    // 在参考数据列表首位增加空值选项，新增字段默认保持不关联。
    referenceOptions.value = [{id: null, name: "不关联"}, ...(response.data?.rows || response.data || [])];
  } finally {
    loadingReferences.value = false;
  }
}

/** 加载字段可选的编码方案。 */
async function loadEncodingOptions() {
  loadingEncodings.value = true;
  try {
    // 字段配置只展示已发布且已配置规则段的编码方案，避免选择不可用方案。
    const response = await listAvailableEncoding();
    encodingOptions.value = response.data || [];
  } finally {
    loadingEncodings.value = false;
  }
}

/** 查询选中方案的有效版本 ID，兼容方案列表直接返回当前版本的情况。 */
async function resolveEncodingVersionId(encoding) {
  const directVersionId = encoding?.currentVersionId || encoding?.version?.id;
  if (directVersionId) {
    return directVersionId;
  }
  const response = await listVersion({encodingId: encoding.id, pageNum: 1, pageSize: 1000});
  const versions = response.data?.rows || response.data || [];
  return versions.find(item => [1, 2, "ENABLED", "PUBLISHED"].includes(item.status))?.id || versions[0]?.id || null;
}

/** 安全解析编码规则扩展配置。 */
function parseRuleConfig(config) {
  if (!config) {
    return {};
  }
  if (typeof config === "object") {
    return config;
  }
  try {
    return JSON.parse(config);
  } catch {
    return {};
  }
}

/** 将后端规则段转换为预览文本。 */
function formatEncodingRule(item) {
  const type = normalizeEncodingRuleType(item.type);
  const config = parseRuleConfig(item.config);
  // 动态字段规则使用参数占位符，其他规则优先展示后端固定值。
  if (type === 'REFERENCE_MAPPING') {
    return `{${dictTypeNameMap.value[item.fixedValue] || item.fixedValue || "参考数据类型"}}`;
  }
  if (type === 'BUSINESS_FIELD') {
    return `{${item.fixedValue || "字段参数"}}`;
  }
  if (type === 'SERIAL_NUMBER') {
    return `流水号(${item.sequenceLength || 1}位)`;
  }
  if (type === 'DATETIME') {
    return item.fixedValue || config.exampleOutput || "日期时间";
  }
  return item.fixedValue || "";
}

/** 将驼峰、下划线等接口规则类型统一成内部标准值。 */
function normalizeEncodingRuleType(type) {
  const normalized = String(type || "").replace(/[_\s-]/g, "").toLowerCase();
  return model_encoding_rule_type.value.find(item =>
      String(item.value).replace(/[_\s-]/g, "").toLowerCase() === normalized
  )?.value || String(type || "").toUpperCase();
}

/** 安全解析自动编码绑定配置，兼容空值、非法 JSON 和旧版数组结构。 */
function parseEncodingRuleConfig(value) {
  if (!value) {
    return [];
  }
  if (Array.isArray(value)) {
    return JSON.parse(JSON.stringify(value));
  }
  if (typeof value === "object") {
    return Array.isArray(value.bindings) ? value.bindings : [];
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : (Array.isArray(parsed.bindings) ? parsed.bindings : []);
  } catch {
    return [];
  }
}

/** 加载编码规则并提取需要由模型字段绑定的动态参数。 */
async function loadEncodingRules() {
  if (!form.encodingVersionId) {
    return;
  }
  const response = await listEncodingItems({encodingVersionId: form.encodingVersionId, pageNum: 1, pageSize: 1000});
  const rows = (response.data?.rows || response.data || []).slice()
      .sort((a, b) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0));
  // fixedValue 是参考数据类型 ID，批量查询类型详情后再生成规则预览和方案参数名称。
  const typeIds = [...new Set(rows
      .filter(item => normalizeEncodingRuleType(item.type) === 'REFERENCE_MAPPING')
      .map(item => item.fixedValue).filter(Boolean))];
  const typeEntries = await Promise.all(typeIds.map(async id => {
    const response = await getType(id);
    return [String(id), response.data?.name || id];
  }));
  dictTypeNameMap.value = Object.fromEntries(typeEntries);
  encodingRules.value = rows.map(item => ({...item, output: formatEncodingRule(item)}));
  const oldBindings = new Map(encodingBindings.value.map(item => [`${item.ruleType}-${item.parameterName}`, item.sourceFieldId]));
  encodingBindings.value = rows
      .filter(item => ['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(normalizeEncodingRuleType(item.type)))
      .map(item => {
        const ruleType = normalizeEncodingRuleType(item.type);
        const parameterName = ruleType === 'REFERENCE_MAPPING'
            ? (dictTypeNameMap.value[item.fixedValue] || item.fixedValue || "参考数据类型")
            : (item.fixedValue || "字段参数");
        return {
          parameterName,
          ruleType,
          // referenceMapping 的 fixedValue 是参考数据类型 ID，用于查询有效数据项。
          dictTypeId: ruleType === 'REFERENCE_MAPPING' ? item.fixedValue : null,
          referenceDataId: null,
          referenceDataOptions: [],
          ruleTypeLabel: encodingRuleLabels.value[ruleType] || ruleType,
          sourceFieldId: oldBindings.get(`${ruleType}-${parameterName}`) || null,
        };
      });
  // 按每条参考数据映射规则的 fixedValue 查询有效数据项，供下拉框使用。
  await Promise.all(encodingBindings.value.filter(item => item.ruleType === 'REFERENCE_MAPPING' && item.dictTypeId)
      .map(async item => {
        const response = await listValidData({typeId: item.dictTypeId, pageNum: 1, pageSize: 1000});
        item.referenceDataOptions = response.data?.rows || response.data || [];
      }));
}

/** 选择参考数据后带入显示名称和编码，只读字段不参与联动。 */
function handleReferenceChange() {
  const reference = form.dictTypeId == null ? null : referenceOptions.value.find(item => item.id === form.dictTypeId);
  if (!reference) {
    return;
  }
  if (!propertyReadonly('code')) {
    form.code = reference.code;
  }
  if (!propertyReadonly('name')) {
    form.name = reference.name;
  }
}

/** 切换自动编码时按需加载方案，并清理手工模式不需要的规则。 */
async function handleEncodingModeChange(mode) {
  // 自动编码与参考数据关联互斥：切换自动编码时清空参考数据并禁用其下拉框。
  if (mode === 'AUTOMATIC') {
    form.dictTypeId = null;
    await loadEncodingOptions();
  } else {
    // 切回手工录入时恢复参考数据选择能力，空值继续显示为“不关联”。
    form.dictTypeId = null;
    form.encodingVersionId = null;
    encodingRules.value = [];
    encodingBindings.value = [];
  }
}

/** 切换编码方案后解析版本并加载规则。 */
async function handleEncodingChange(encodingVersionId) {
  const encoding = encodingOptions.value.find(item => item.currentVersionId === encodingVersionId);
  form.encodingVersionId = encoding ? await resolveEncodingVersionId(encoding) : null;
  encodingRules.value = [];
  encodingBindings.value = [];
  if (form.encodingVersionId) {
    await loadEncodingRules();
  }
}

/** 根据动态规则类型筛选允许绑定的当前模型普通字段。 */
function getBindingFieldOptions(ruleType) {
  const candidateFields = props.sourceFields.filter(item => item.localId !== form.localId);
  // 参考数据映射只能绑定已选择参考数据的字段，业务字段可绑定全部普通字段。
  if (ruleType === 'REFERENCE_MAPPING') {
    return candidateFields.filter(item => item.dictTypeId && item.dictTypeId !== "不关联");
  }
  return candidateFields;
}

/** 主数据域变化后清空模型和字段，并重新查询对应模型。 */
async function handleProjectChange(projectId) {
  // 只读控件不执行联动，避免修改系统字段或只读表单。
  if (propertyReadonly('relatedModelId')) {
    return;
  }
  form.layerId = null;
  form.relatedModelId = null;
  form.relatedCode = null;
  form.displayCode = null;
  form.storageType = "";
  relatedFieldOptions.value = [];
  await loadLayers(projectId);
}

/** 按主数据域加载模型目录选项。 */
async function loadLayers(projectId) {
  if (!projectId) {
    layerOptions.value = [];
    return;
  }
  const response = await treeLayer({projectId});
  const rows = response.data?.rows || response.data || [];
  layerOptions.value = proxy.handleTree(rows, "id", "parentId");
}

/** 根据所选目录更新字段关联模型选项。 */
async function handleLayerChange(layerId) {
  // 只读控件不执行联动，避免修改系统字段或只读表单。
  if (propertyReadonly('relatedModelId')) {
    return;
  }
  form.relatedModelId = null;
  form.relatedCode = null;
  form.displayCode = null;
  relatedFieldOptions.value = [];
  await loadRelatedModels(form.projectId, layerId);
}

/** 加载指定主数据域下的关联模型或分类模型。 */
async function loadRelatedModels(projectId, layerId = form.layerId) {
  loadingModels.value = true;
  try {
    const response = projectId
        ? await listRelatedModels({
          projectId,
          layerId,
          type: fieldKind.value === 'CLASS' ? 'CATEGORY' : 'NORMAL',
          id: props.currentModelId || undefined,
        })
        : {data: []};
    const rows = response.data?.rows || response.data || [];
    relatedModelOptions.value = rows.map(item => ({
      id: item.id,
      name: item.name,
      icon: item.icon,
      type: item.type,
      parentId: item.parentId,
      fields: item.fields || [],
    }));
  } finally {
    loadingModels.value = false;
  }
}

/** 目标模型变化后清空旧字段，并查询目标模型字段。 */
async function handleRelatedModelChange(modelId) {
  // 只读控件不执行联动，避免修改系统字段或只读表单。
  if (propertyReadonly('relatedModelId')) {
    return;
  }
  form.relatedCode = null;
  form.displayCode = null;
  form.storageType = "";
  syncSelectedLabels();
  await loadRelatedFields(modelId);
}

/** 加载关联模型的键字段及展示字段候选项。 */
async function loadRelatedFields(modelId) {
  loadingRelatedFields.value = true;
  try {
    const model = relatedModelOptions.value.find(item => sameId(item.id, modelId));
    const options = (model?.fields || []).map(item => ({
      id: item.id,
      name: item.name,
      code: item.code,
      dataType: item.dataType,
      dataLength: item.dataLength,
      dataScale: item.dataScale,
      uniqueFlag: item.uniqueFlag,
      primaryKeyFlag: item.primaryKeyFlag,
      enabledFlag: item.enabledFlag,
      delFlag: item.delFlag,
    }));
    // 只为旧配置补一个能看见的选项；没查到真实字段就标记未确认，不能冒充有效唯一键。
    if (form.relatedCode && !options.some(item => item.code === form.relatedCode)) {
      options.push({
        name: form.relatedCode,
        code: form.relatedCode,
        dataType: form.dataType,
        dataLength: form.dataLength,
        dataScale: form.dataScale,
        unresolved: true,
      });
    }
    if (form.displayCode && !options.some(item => item.code === form.displayCode)) {
      options.push({
        name: form.displayCode,
        code: form.displayCode,
        dataType: 'STRING',
        dataLength: 255,
        uniqueFlag: false,
        unresolved: true,
      });
    }
    relatedFieldOptions.value = options;
  } finally {
    loadingRelatedFields.value = false;
  }
}

/** 关联键字段变化后自动继承其数据类型、长度和精度。 */
function handleKeyFieldChange(fieldCode) {
  // 只读控件不执行联动，避免修改系统字段或只读表单。
  if (propertyReadonly('relatedCode')) {
    return;
  }
  const field = relatedFieldOptions.value.find(item => item.code === fieldCode);
  // 未匹配字段时清空存储信息，避免保留上一字段类型。
  if (!field) {
    form.storageType = "";
    return;
  }
  form.dataType = field.dataType;
  form.dataLength = field.dataLength ?? null;
  form.dataScale = field.dataScale ?? null;
  form.storageType = formatStorageType(field);
  syncSelectedLabels();
}

/** 将字段类型、长度和精度格式化为存储类型文本。 */
function formatStorageType(field) {
  if (field.dataType === 'DECIMAL' && field.dataLength) {
    return `decimal(${field.dataLength},${field.dataScale ?? 0})`;
  }
  if (field.dataLength) {
    return `${field.dataType}(${field.dataLength})`;
  }
  return field.dataType || "";
}

/** 同步下拉项显示名称，保存后列表无需依赖选项接口再次解析。 */
function syncSelectedLabels() {
  form.relatedModelName = relatedModelOptions.value.find(item => sameId(item.id, form.relatedModelId))?.name || form.relatedModelName;
  form.relatedCodeName = form.relatedCode || "";
  form.displayCodeName = form.displayCode || "";
  form.nodeRuleLabel = nodeRuleLabel(form.nodeRule) || "";
  form.displayModeLabel = displayModeLabel(form.displayMode) || "";
}

watch([model_field_node_rule, model_field_display_mode], syncSelectedLabels);

/** 数据类型变化后清理不适用的长度和精度。 */
function handleDataTypeChange(dataType) {
  // 只读控件不执行联动，避免修改系统字段或只读表单。
  if (propertyReadonly('dataType')) {
    return;
  }
  // 切换类型后不沿用上一个类型的默认值；字符串默认长度 255，小数默认精度 2。
  form.defaultValue = "";
  if (dataType === 'STRING') {
    form.dataLength = 255;
    form.dataScale = null;
  } else if (dataType === 'DECIMAL') {
    form.dataLength = 18;
    form.dataScale = 2;
  } else {
    form.dataLength = null;
    form.dataScale = null;
  }
}

/** 普通字段输入编码后提示恢复；关联、分类字段只在点击确定时检查。 */
async function handleCodeChange() {
  if (fieldKind.value !== 'BUSINESS' || props.readonly || submitting.value) return;
  submitting.value = true;
  try {
    const result = await confirmFieldRestoration(form, props.allFields, props.publishedFields);
    if (result.restored) {
      // 确认恢复就是把原字段放回列表，无需在弹窗里再点一次确定。
      emit("confirm", result.field);
      visible.value = false;
    }
  } finally {
    submitting.value = false;
  }
}

/** 只执行原有表单校验，通过后把配置交回列表；这里不直接保存到后端。 */
async function submitForm() {
  if (submitting.value || props.readonly) {
    return;
  }
  submitting.value = true;
  try {
    const result = await confirmFieldRestoration(form, props.allFields, props.publishedFields);
    if (result.cancelled) return;
    if (result.restored) {
      emit("confirm", result.field);
      visible.value = false;
      return;
    }
  } finally {
    submitting.value = false;
  }
  const valid = await fieldFormRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  // 这里只检查当前表单的名称与必填是否一致，不检查其他字段。
  if (form.nameFlag && (!form.requiredFlag || propertyReadonly('nameFlag'))) {
    proxy.$modal.msgWarning('名称字段必须为必填，已发布的非必填字段不能设为名称字段');
    return;
  }
  const candidate = {
    ...form, localId: form.localId || `field-${Date.now()}`,
    encodingRuleConfig: form.encodingMode === 'AUTOMATIC'
        ? JSON.stringify(encodingBindings.value.map(item => ({
          parameterName: item.parameterName,
          ruleType: item.ruleType,
          sourceFieldId: item.sourceFieldId
        }))) : null
  };
  submitting.value = true;
  try {
    syncSelectedLabels();
    // 自动编码只持久化动态参数绑定；固定文本、日期和流水号来自编码方案规则段。
    form.encodingRuleConfig = candidate.encodingRuleConfig;
    // 保留临时字段兼容当前父组件，但后端实际使用 encodingRuleConfig。
    form.encodingBindings = JSON.parse(JSON.stringify(encodingBindings.value.map(item => ({
      ...item,
      ruleTypeLabel: encodingRuleLabels.value[item.ruleType] || item.ruleType,
    }))));
    emit("confirm", {...form, localId: candidate.localId});
    visible.value = false;
  } finally {
    submitting.value = false;
  }
}

defineExpose({openDialog});
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}

.source-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.source-option__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  object-fit: contain;
}
</style>
