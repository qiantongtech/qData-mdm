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
  <el-alert
      v-if="readonly"
      class="mb15"
      type="info"
      :closable="false"
      title="该版本已产生编码，当前规则仅可查看，不允许原地修改。"
  />

  <qt-wrap :columns="ruleTableStore.columns" :table-ref="ruleTableRef" :config="ruleTableStore.config">
    <template #actions-data>
      <el-button
          v-hasPermi="[draftMode ? 'model:encoding:add' : 'model:encoding:item:add']"
          type="primary"
          plain
          icon="Plus"
          :disabled="readonly || versionStatusLocked || (!draftMode && !encodingVersionId)"
          @mousedown="(e) => e.preventDefault()"
          @click="handleAddRule"
      >
        新增规则段
      </el-button>
      <el-button
          v-if="!draftMode"
          :disabled="rules.length === 0"
          @click="handleCalculate"
      >
        <template #icon>
          <svg-icon icon-class="try-calc"/>
        </template>
        试计算
      </el-button>
      <el-button
          v-if="!draftMode"
          v-hasPermi="['model:encoding:item:edit']"
          type="primary"
          :disabled="readonly || versionStatusLocked || !encodingVersionId || rules.length === 0"
          :loading="savingRules"
          @click="handleSave"
      >
        <template #icon>
          <svg-icon icon-class="save-rule"/>
        </template>
        保存规则
      </el-button>
    </template>

    <qt-table v-bind="ruleTableStore" ref="ruleTableRef">
      <template #ruleNumber="{ row }">
        <span>{{ row.id ?? '待保存' }}</span>
      </template>
      <template #output="{ row }">
        <el-text type="primary">{{ row.output || '-' }}</el-text>
      </template>
      <template #action="{ row }">
        <el-button
            v-hasPermi="[draftMode ? 'model:encoding:add' : 'model:encoding:item:edit']"
            link
            icon="Edit"
            type="primary"
            :disabled="readonly || versionStatusLocked"
            @click="handleEdit(row)"
        >
          修改
        </el-button>
        <el-button
            v-hasPermi="[draftMode ? 'model:encoding:add' : 'model:encoding:item:remove']"
            link
            icon="Delete"
            type="danger"
            :disabled="readonly || versionStatusLocked"
            @click="handleDelete(row)"
        >
          删除
        </el-button>
      </template>
    </qt-table>
  </qt-wrap>

  <el-dialog
      v-model="ruleDialogVisible"
      :title="ruleDialogTitle"
      width="800px"
      draggable
      class="encoding-dialog"
      @closed="resetRuleForm"
  >
    <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="ruleFormRules"
        label-width="110"
        @submit.prevent
    >
      <guide-tip-panel
          class="domain-intro"
          :title="`${currentSegmentMeta.helpTitle}；${currentSegmentMeta.helpText} 示例：${currentSegmentMeta.example}`"
          type="primary"
          allow-wrap
      />

      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="编码段类型" prop="segmentType">
            <el-select
                v-model="ruleForm.segmentType"
                placeholder="请选择编码段类型"
                class="full-width"
                @change="handleSegmentTypeChange"
            >
              <el-option
                  v-for="item in model_segment_type"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              />
            </el-select>
          </qt-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="顺序" prop="order">
            <el-input-number
                v-model="ruleForm.order"
                :min="1"
                :max="rules.length + (editingRuleIndex === null ? 1 : 0)"
                controls-position="right"
                class="full-width"
            />
          </qt-form-item>
        </el-col>
      </el-row>

      <el-row v-if="ruleForm.segmentType === 'FIXED_TEXT'" :gutter="24">
        <el-col :span="24">
          <qt-form-item label="固定内容" prop="fixedContent">
            <el-input
                v-model="ruleForm.fixedContent"
                placeholder="如需分隔符，可直接填写 - / _ 等固定内容"
            />
          </qt-form-item>
        </el-col>
      </el-row>

      <el-row
          v-else-if="['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(ruleForm.segmentType)"
          :gutter="24"
      >
        <el-col :span="24">
          <qt-form-item label="字段参数名称" prop="fieldParameterName" :tip="currentSegmentMeta.fieldTip">
            <el-select
                v-if="ruleForm.segmentType === 'REFERENCE_MAPPING'"
                v-model="ruleForm.fieldParameterName"
                v-loading="loadingDictTypes"
                placeholder="请选择参考数据类型"
                class="full-width"
            >
              <el-option
                  v-for="item in dictTypeOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
              />
            </el-select>
            <el-input v-else
                      v-model="ruleForm.fieldParameterName"
                      placeholder="例如：物料类别"
            />
          </qt-form-item>
        </el-col>
      </el-row>

      <el-row v-else-if="ruleForm.segmentType === 'DATETIME'" :gutter="24">
        <el-col :span="24">
          <qt-form-item label="日期输出格式" prop="dateTimeFormat">
            <el-input v-model="ruleForm.dateTimeFormat" placeholder="例如：yyyyMMdd"/>
          </qt-form-item>
        </el-col>
        <el-col :span="24">
          <qt-form-item label="示例输出" prop="exampleOutput">
            <el-input v-model="ruleForm.exampleOutput" placeholder="例如：20260901"/>
          </qt-form-item>
        </el-col>
      </el-row>

      <template v-else-if="ruleForm.segmentType === 'SERIAL_NUMBER'">
        <el-row :gutter="24">
          <el-col :span="24">
            <qt-form-item label="位数" prop="digits">
              <el-input-number
                  v-model="ruleForm.digits"
                  :min="1"
                  :max="12"
                  controls-position="right"
                  class="full-width"
              />
            </qt-form-item>
          </el-col>
          <el-col :span="24">
            <qt-form-item label="补位方式" prop="paddingMethod">
              <el-select v-model="ruleForm.paddingMethod" placeholder="请选择补位方式" class="full-width">
                <el-option
                    v-for="item in model_padding_method"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </qt-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <qt-form-item label="起始值" prop="startValue">
              <el-input-number
                  v-model="ruleForm.startValue"
                  :min="1"
                  controls-position="right"
                  class="full-width"
              />
            </qt-form-item>
          </el-col>
          <el-col :span="24">
            <qt-form-item label="步长" prop="step">
              <el-input-number
                  v-model="ruleForm.step"
                  :min="1"
                  controls-position="right"
                  class="full-width"
              />
            </qt-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="24">
            <qt-form-item
                label="重置方式"
                prop="resetMethod"
                tip="流水号正式分配后不回收；多个模型复用同一方案时按“编码方案 + 使用模型”独立计数。"
            >
              <el-select v-model="ruleForm.resetMethod" placeholder="请选择重置方式" class="full-width">
                <el-option
                    v-for="item in model_reset_method"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </qt-form-item>
          </el-col>
        </el-row>
      </template>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingRule" @click="submitRuleForm">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog
      v-model="calculateDialogVisible"
      title="编码试计算"
      :width="calculateBindings.length ? '1000px' : '800px'"
      class="encoding-dialog"
      :append-to="calculateDialogTarget"
      style="max-width: calc(100vw - 24px)"
      draggable
      destroy-on-close
      @closed="resetCalculateDialog"
  >
    <guide-tip-panel
        class="domain-intro"
        type="primary"
        allow-wrap
        :title="calculateResults.length
            ? `已生成 ${calculateResults.length} 条模拟编码，本次试算不占用真实流水号。`
            : '填写模拟绑定字段和示例值后即可试算，绑定关系仅在本次试算中有效。'"
    />

    <template v-if="calculateBindings.length">
      <h3 class="h2-titles mb15">模拟字段绑定</h3>
      <qt-table v-bind="calculateBindingTableStore">
        <template #boundField="{ row }">
          <el-input v-model="row.boundField" :aria-label="row.parameterName + '的模拟绑定字段'" placeholder="请输入来源字段"/>
        </template>
        <template #sampleValue="{ row }">
          <el-input
              v-model="row.sampleValue"
              :aria-label="row.parameterName + '的示例值'"
              :placeholder="row.segmentType === 'REFERENCE_MAPPING' ? '请输入参考数据项编码' : '请输入示例值'"
          />
        </template>
      </qt-table>
    </template>

    <div v-if="calculateResults.length" aria-live="polite">
      <h3 class="h2-titles mt15 mb15">试算结果</h3>
      <qt-table v-bind="calculateResultTableStore">
        <template #code="{ row }"><code>{{ row.code }}</code></template>
      </qt-table>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="calculateResults.length" @click="calculateDialogVisible = false">关闭</el-button>
        <template v-else>
          <el-button @click="calculateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="startCalculate">开始试计算</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="EncodingItem">
/**
 * @description 编码规则编辑器，负责规则段维护与试算。
 * @prop {Object} encodingVersion 编码方案版本信息
 * @emits addRule addRule
 * @emits calculate calculate
 * @emits save save
 * @emits ruleCount 更新编码规则段数量
 * @emits ruleStructurePreview 更新规则结构预览
 * @emits edit edit
 * @emits delete delete
 */
import {
  listEncodingItems,
  saveEncodingItems
} from '@/api/model/encoding/item';
import {listValidType} from '@/api/model/dict/type/type';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';

const {proxy} = getCurrentInstance();
const {model_segment_type, model_padding_method, model_reset_method} = proxy.useDict(
    "model_segment_type",
    "model_padding_method",
    "model_reset_method"
);

const props = defineProps({
  encodingVersion: {
    type: Object,
    default: null
  },
  draftMode: {type: Boolean, default: false}
});

const emit = defineEmits([
  'addRule',
  'calculate',
  'save',
  'ruleCount',
  'ruleStructurePreview',
  'edit',
  'delete'
]);

const readonly = ref(false);
const encodingVersionId = computed(() => props.encodingVersion?.id ?? null);
/** 只有草稿版本允许维护规则；停用（2）和发布（3）版本统一锁定。 */
const versionStatusLocked = computed(() => [2, 3].includes(Number(props.encodingVersion?.status)));
// referenceMapping 保存参考数据类型 ID，下拉框展示参考数据类型名称。
const dictTypeOptions = ref([]);
const loadingDictTypes = ref(false);

const segmentMetaMap = {
  ['FIXED_TEXT']: {
    helpTitle: '配置固定输出文字',
    helpText: '不读取业务数据，可用于固定前缀、后缀或分隔符。',
    example: '配置 MD → 输出 MD'
  },
  ['BUSINESS_FIELD']: {
    helpTitle: '配置业务字段参数',
    helpText: '模型使用方案时绑定同模型字段，生成编码时输出该字段的当前值。',
    example: '类别 → 物料类别 → A01',
    fieldTip: '仅填写参数名称，实际字段在模型中绑定。'
  },
  ['REFERENCE_MAPPING']: {
    helpTitle: '配置参考数据参数',
    helpText: '模型使用方案时绑定引用该类型的字段，生成编码时输出对应的数据项编码。',
    example: '币种 → 结算币种 → 人民币 → CNY',
    fieldTip: '请选择参考数据类型，实际字段在模型中绑定。'
  },
  ['DATETIME']: {
    helpTitle: '按生成时系统时间格式化',
    helpText: '取正式发起编码生成请求时的系统日期时间，并按照配置格式输出。',
    example: '2026-09-01 + yyyyMMdd → 20260901'
  },
  ['SERIAL_NUMBER']: {
    helpTitle: '按序列规则生成递增号码',
    helpText: '支持 1～12 位、左补零/不补位、起始值、步长和重置周期；试计算不占号。',
    example: '4 位、起始 1、左补零 → 0001'
  }
};

const rules = ref([]);
const loadingRules = ref(false);
// 仅记录本次在界面中移除的已落库规则，真正删除由“保存规则”的批量接口统一完成。
const deletedRuleIds = ref([]);
// clientKey 只用来在页面中区分规则，不展示、不提交给后端；id 只保存数据库返回的真实编号。
let temporaryRuleKey = 0;

// 按当前排序拼接每个规则段的示例输出，任一增删改或排序都会自动刷新预览。
const ruleStructurePreview = computed(() => (
    rules.value.map((rule) => rule.output || '').join('') || '-'
));

const ruleDialogVisible = ref(false);
const submittingRule = ref(false);
const savingRules = ref(false);
const editingRuleIndex = ref(null);
const ruleFormRef = ref();
const calculateDialogVisible = ref(false);
const calculateDialogTarget = shallowRef('body');
const calculateBindings = ref([]);
const trialResultCount = 5;
const calculateResults = ref([]);
// 输入改变后，旧编码不再代表当前示例值。
watch(calculateBindings, () => {
  calculateResults.value = [];
}, {deep: true, flush: 'sync'});

const calculateBindingTableStore = {
  config: {notPagination: true, fullContent: false, table: {stripe: true}},
  columns: [
    {label: '编号', prop: 'index', width: 60},
    {label: '方案参数', prop: 'parameterName', minWidth: 120, align: 'left', showOverflowTooltip: true},
    {label: '编码段类型', prop: 'segmentTypeName', minWidth: 120, align: 'left'},
    {label: '模拟绑定字段', slot: 'boundField', minWidth: 170, align: 'left'},
    {label: '示例值', slot: 'sampleValue', minWidth: 170, align: 'left'},
    {label: '计算方式', prop: 'calculationMethod', minWidth: 130, align: 'left'}
  ],
  func: async () => ({data: calculateBindings.value})
};

const calculateResultTableStore = {
  config: {notPagination: true, fullContent: false, table: {stripe: true}},
  columns: [
    {label: '序号', prop: 'index', width: 60},
    {label: '模拟编码', prop: 'code', slot: 'code', align: 'left', showOverflowTooltip: true}
  ],
  func: async () => ({data: calculateResults.value.map((code, i) => ({index: i + 1, code}))})
};

const ruleTableRef = ref();

const ruleTableStore = reactive({
  config: {
    fullContent: false,
    notPagination: !props.draftMode,
    autoPagination: props.draftMode,
    actions: {table: {show: !props.draftMode, search: false, refresh: true, columns: true}},
    table: {
      stripe: true,
    },
  },
  columns: [
    {
      label: '编号',
      prop: 'sortOrder',
      width: 60,
      align: 'center',
      sortable: true
    },
    {label: '编码段类型', prop: 'type', minWidth: 150, align: 'left'},
    {label: '规则配置', prop: 'description', minWidth: 260, align: 'left', showOverflowTooltip: {effect: 'light'}},
    {label: '示例输出', prop: 'output', slot: 'output', minWidth: 160, align: 'left'},
    {label: '排序', prop: 'sortOrder', minWidth: 100, sortable: true},
    {label: '操作', prop: 'id', slot: 'action', minWidth: 180, fixed: 'right'}
  ],
  func: loadRules,
  params: {}
});

/** 让表格重新向后端查询规则，更新页面上的列表和正式编号。 */
function refreshRuleTable() {
  nextTick(() => ruleTableRef.value?.getList());
}

// 规则增删改及接口回显后，将最新结构预览同步到编码方案详情头部。
watch(ruleStructurePreview, value => emit('ruleStructurePreview', value), {immediate: true});

// 草稿条数变化后更新分页总数。
watch(() => props.draftMode ? rules.value.length : 0, () => {
  ruleTableRef.value?.getList();
});

watch(encodingVersionId, (versionId) => {
  // 版本切换时先清空旧版本数据，再查询新版本，避免短暂展示或误保存上一版本规则。
  rules.value = [];
  deletedRuleIds.value = [];
  emit('ruleCount', 0);
  if (versionId) {
    refreshRuleTable();
  }
}, {immediate: true});

/** 准备一份带默认值的空表单，新增规则时默认排在最后。 */
function createEmptyRuleForm() {
  // 集中维护各类型默认值，新增、切换类型和编辑回填都基于同一份表单结构。
  return {
    segmentType: 'FIXED_TEXT',
    order: Math.max(0, ...rules.value.map((item) => Number(item.sortOrder) || 0)) + 1,
    fixedContent: '',
    fieldParameterName: '',
    dateTimeFormat: 'yyyyMMdd',
    exampleOutput: '20260901',
    digits: 6,
    startValue: 1,
    step: 1,
    resetMethod: 'NONE',
    paddingMethod: 'LEFT_ZERO'
  };
}

const ruleForm = ref(createEmptyRuleForm());

const ruleDialogTitle = computed(() => (
    editingRuleIndex.value === null ? '新增规则段' : '编辑规则段'
));

const currentSegmentMeta = computed(() => (
    segmentMetaMap[ruleForm.value.segmentType] || segmentMetaMap['FIXED_TEXT']
));

const ruleFormRules = computed(() => ({
  segmentType: [{required: true, message: '请选择编码段类型', trigger: 'change'}],
  order: [
    {required: true, message: '顺序不能为空', trigger: 'change'},
    {type: 'number', message: '顺序必须为数字', trigger: 'change'}
  ],
  fixedContent: [{required: true, message: '固定内容不能为空', trigger: 'blur'}],
  fieldParameterName: [{required: true, message: '字段参数名称不能为空', trigger: 'blur'}],
  dateTimeFormat: [{required: true, message: '日期时间输出格式不能为空', trigger: 'blur'}],
  exampleOutput: [{required: true, message: '示例输出不能为空', trigger: 'blur'}],
  digits: [{required: true, message: '位数不能为空', trigger: 'change'}],
  startValue: [{required: true, message: '起始值不能为空', trigger: 'change'}],
  step: [{required: true, message: '步长不能为空', trigger: 'change'}],
  resetMethod: [{required: true, message: '请选择重置方式', trigger: 'change'}],
  paddingMethod: [{required: true, message: '请选择补位方式', trigger: 'change'}]
}));

/** 清空上次填写的内容和校验提示，并退出编辑状态。 */
function resetRuleForm() {
  editingRuleIndex.value = null;
  ruleForm.value = createEmptyRuleForm();
  ruleFormRef.value?.clearValidate();
}

/** 切换规则类型时清掉旧类型的内容，保留顺序；选参考数据时加载可选类型。 */
function handleSegmentTypeChange(segmentType) {
  const order = ruleForm.value.order;
  ruleForm.value = {
    ...createEmptyRuleForm(),
    segmentType,
    order
  };
  nextTick(() => ruleFormRef.value?.clearValidate());
  if (segmentType === 'REFERENCE_MAPPING') loadDictTypeOptions();
}

/** 从后端拿到可用的参考数据类型，供下拉选择和名称展示使用。 */
async function loadDictTypeOptions() {
  loadingDictTypes.value = true;
  try {
    const response = await listValidType();
    dictTypeOptions.value = response.data || [];
  } finally {
    loadingDictTypes.value = false;
  }
}

/** 点击新增时打开空表单；版本已经锁定时不允许新增。 */
function handleAddRule() {
  if (versionStatusLocked.value) return;
  resetRuleForm();
  ruleDialogVisible.value = true;
}

/** 按值找到对应的字典选项，忽略字符串前后空格和大小写差异。 */
function findDictOption(options, value) {
  const normalizedValue = String(value ?? '').trim().toUpperCase();
  return options.find((item) => (
      item.value === value
      || String(item.value ?? '').trim().toUpperCase() === normalizedValue
  ));
}

/** 把字典值换成用户能看懂的名称；暂时找不到名称就显示原值。 */
function getOptionLabel(options, value) {
  return findDictOption(options, value)?.label || value;
}

/** 把接口返回的值换成下拉选项实际使用的值，让下拉框能正确选中。 */
function getOptionValue(options, value) {
  return findDictOption(options, value)?.value ?? value;
}

/** 根据参考数据类型编号找到名称；还没加载到名称时先显示编号。 */
function getDictTypeLabel(value) {
  return dictTypeOptions.value.find((item) => String(item.id) === String(value))?.name || value;
}

/** 把规则类型和配置整理成表格里的一行，同时生成规则说明、示例输出和页面标识。正式编号只取后端返回的 ID。 */
function createRuleModel(segmentType, config, options = {}) {
  const type = model_segment_type.value.find((item) => item.value === segmentType)?.label || segmentType;
  const rule = {
    id: options.id ?? null,
    clientKey: options.clientKey ?? (options.id != null
        ? `saved-${options.id}`
        : `draft-${temporaryRuleKey++}`),
    persisted: options.persisted ?? false,
    sortOrder: Number(options.sortOrder) || 1,
    segmentType,
    type,
    description: '',
    output: '',
    config
  };

  // 不同类型拥有不同的展示和输出算法，统一在这里生成，保证新增与接口回显效果一致。
  if (segmentType === 'FIXED_TEXT') {
    rule.description = config.fixedContent;
    rule.output = config.fixedContent;
  } else if (['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(segmentType)) {
    // referenceMapping 的配置值保存的是 ID，规则概览必须显示参考数据类型名称。
    const parameterLabel = segmentType === 'REFERENCE_MAPPING'
        ? getDictTypeLabel(config.fieldParameterName)
        : config.fieldParameterName;
    rule.description = `字段参数：${parameterLabel}`;
    rule.output = `{${parameterLabel}}`;
  } else if (segmentType === 'DATETIME') {
    rule.description = config.dateTimeFormat;
    rule.output = config.exampleOutput || config.dateTimeFormat;
  } else if (segmentType === 'SERIAL_NUMBER') {
    const paddingLabel = getOptionLabel(model_padding_method.value, config.paddingMethod);
    const resetLabel = getOptionLabel(model_reset_method.value, config.resetMethod);
    rule.description = `${config.digits} 位 / ${paddingLabel} / 起始 ${config.startValue} / 步长 ${config.step} / ${resetLabel}`;
    rule.output = config.paddingMethod === 'LEFT_ZERO'
        ? String(config.startValue).padStart(config.digits, '0')
        : String(config.startValue);
  }

  return rule;
}

/** 把接口里的配置转成对象，已经是对象就直接用，空值或解析失败就用空配置。 */
function parseItemConfig(config) {
  // config 可能来自接口 JSON 字符串，也可能已被请求层转换为对象；无效内容按空配置兼容。
  if (!config) return {};
  if (typeof config === 'object') return config;
  try {
    return JSON.parse(config);
  } catch {
    return {};
  }
}

/** 把历史数据中的中文类型名称换成英文类型值，方便后面统一判断规则类型。 */
function normalizeSegmentType(type) {
  // 兼容历史数据中可能保存中文名称的情况，内部统一使用英文类型值判断。
  return model_segment_type.value.find((item) => item.value === type || item.label === type)?.value || type;
}

/** 把后端返回的一条规则还原成页面需要的配置和展示内容，并保留正式编号。 */
function mapApiItemToRule(item) {
  const segmentType = normalizeSegmentType(item.type);
  const extensionConfig = parseItemConfig(item.config);
  let config = {};

  if (segmentType === 'FIXED_TEXT') {
    config = {fixedContent: item.fixedValue || ''};
  } else if (segmentType === 'BUSINESS_FIELD') {
    config = {fieldParameterName: item.fixedValue || ''};
  } else if (segmentType === 'REFERENCE_MAPPING') {
    config = {fieldParameterName: item.fixedValue == null ? '' : Number(item.fixedValue)};
  } else if (segmentType === 'DATETIME') {
    config = {
      dateTimeFormat: item.fixedValue || '',
      exampleOutput: extensionConfig.exampleOutput || ''
    };
  } else if (segmentType === 'SERIAL_NUMBER') {
    config = {
      digits: Number(item.sequenceLength ?? 1),
      startValue: Number(item.sequenceStart ?? 1),
      step: Number(item.stepSize ?? 1),
      resetMethod: item.resetMethod || 'NONE',
      paddingMethod: item.padChar
    };
  }

  return createRuleModel(segmentType, config, {
    id: item.id,
    persisted: true,
    sortOrder: item.sortOrder
  });
}

/**
 * 获取整套规则供表格展示，草稿读取本地数据，详情查询当前版本。
 * @returns {Promise<{data: Object[]}>} 当前的全部规则。
 */
async function loadRules() {
  if (props.draftMode) return {data: rules.value};
  if (!encodingVersionId.value) {
    rules.value = [];
    return {data: []};
  }
  loadingRules.value = true;
  try {
    const response = await listEncodingItems({
      encodingVersionId: encodingVersionId.value,
      pageSize: -1
    });
    const rows = response.data?.rows || [];
    emit('ruleCount', Number(response.data?.total ?? rows.length));
    // 先加载名称映射，再把接口中的 referenceMapping ID 转成概览文本。
    if (rows.some((item) => normalizeSegmentType(item.type) === 'REFERENCE_MAPPING')) {
      await loadDictTypeOptions();
    }
    // 即使后端默认已排序，前端仍按 sortOrder 排一次，确保预览与实际生成顺序一致。
    rules.value = rows
        .slice()
        .sort((a, b) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0))
        .map(mapApiItemToRule);
    deletedRuleIds.value = [];
    return {data: rules.value};
  } finally {
    loadingRules.value = false;
  }
}

/** 从表单中只拿当前规则类型需要的内容，整理成一条页面规则。 */
function createRuleFromForm() {
  const form = ruleForm.value;
  let config = {};

  // 只提取当前规则类型需要的字段，避免切换类型后残留字段参与保存或预览。
  if (form.segmentType === 'FIXED_TEXT') {
    config = {fixedContent: form.fixedContent};
  } else if (['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(form.segmentType)) {
    config = {fieldParameterName: form.fieldParameterName};
  } else if (form.segmentType === 'DATETIME') {
    config = {
      dateTimeFormat: form.dateTimeFormat,
      exampleOutput: form.exampleOutput
    };
  } else if (form.segmentType === 'SERIAL_NUMBER') {
    config = {
      digits: form.digits,
      startValue: form.startValue,
      step: form.step,
      resetMethod: form.resetMethod,
      paddingMethod: form.paddingMethod
    };
  }

  return createRuleModel(form.segmentType, config, {sortOrder: Number(form.order)});
}

/** 按规则的顺序值排列列表；顺序相同时用页面标识区分，保证排列稳定。 */
function sortRulesByOrder() {
  rules.value.sort((left, right) => (
      Number(left.sortOrder || 0) - Number(right.sortOrder || 0)
      || String(left.clientKey).localeCompare(String(right.clientKey))
  ));
}

/** 按列表的实际位置把顺序重新编号为 1、2、3，避免删除或移动后出现空号。 */
function normalizeRuleOrders() {
  rules.value.forEach((rule, index) => {
    rule.sortOrder = index + 1;
  });
}

/** 用页面标识找到这条规则，把它移到指定位置，再重新整理所有规则的顺序。 */
function moveRuleToOrder(rule, order) {
  const currentIndex = rules.value.findIndex((item) => item.clientKey === rule.clientKey);
  if (currentIndex === -1) return;

  const targetIndex = Math.min(
      rules.value.length - 1,
      Math.max(0, (Number(order) || 1) - 1)
  );
  const [movedRule] = rules.value.splice(currentIndex, 1);
  rules.value.splice(targetIndex, 0, movedRule);
  normalizeRuleOrders();
}

/** 先检查表单，再把新增或修改的内容放到页面草稿中；点击“保存规则”后才写入数据库。 */
async function submitRuleForm() {
  const valid = await ruleFormRef.value.validate().catch(() => false);
  if (!valid) return;

  submittingRule.value = true;
  try {
    const savedRule = createRuleFromForm();
    // 弹窗确认只修改前端草稿；数据库写入统一由页面“保存规则”触发。
    if (editingRuleIndex.value === null) {
      rules.value.push(savedRule);
      moveRuleToOrder(savedRule, savedRule.sortOrder);
      emit('addRule', {
        rule: savedRule,
        index: rules.value.findIndex((item) => item.clientKey === savedRule.clientKey),
        form: {...ruleForm.value},
        rules: [...rules.value]
      });
    } else {
      const index = editingRuleIndex.value;
      const originalRule = rules.value[index];
      // 编辑后仍用原来的页面标识和数据库编号，避免把同一条规则当成另一条新增规则。
      const updatedRule = {
        ...originalRule,
        ...savedRule,
        id: originalRule.id,
        clientKey: originalRule.clientKey,
        persisted: originalRule.persisted
      };
      rules.value.splice(index, 1, updatedRule);
      moveRuleToOrder(updatedRule, updatedRule.sortOrder);
      emit('edit', {
        rule: updatedRule,
        originalRule,
        index,
        form: {...ruleForm.value},
        rules: [...rules.value]
      });
    }
    emit('ruleCount', rules.value.length);
    ruleDialogVisible.value = false;
  } finally {
    submittingRule.value = false;
  }
}

/** 为业务字段和参考数据规则准备试算输入项，用页面标识区分规则，避免未保存的规则互相串值。 */
function createCalculateBindings() {
  // 只有依赖业务数据的两类规则需要模拟字段绑定，固定值、日期和流水号可直接计算。
  return rules.value
      .filter((rule) => ['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(rule.segmentType))
      .map((rule, index) => {
        // 试算预览使用参考数据类型名称，规则配置保存类型 ID。
        const parameterName = rule.segmentType === 'REFERENCE_MAPPING'
            ? getDictTypeLabel(rule.config.fieldParameterName)
            : rule.config.fieldParameterName;
        return {
          index: index + 1,
          ruleId: rule.id,
          ruleKey: rule.clientKey,
          parameterName,
          segmentType: rule.segmentType,
          segmentTypeName: getOptionLabel(model_segment_type.value, rule.segmentType),
          dictTypeId: rule.segmentType === 'REFERENCE_MAPPING' ? rule.config.fieldParameterName : null,
          boundField: parameterName,
          sampleValue: '',
          calculationMethod: rule.segmentType === 'REFERENCE_MAPPING'
              ? '转参考数据项编码'
              : '取字段原值'
        };
      });
}

/** 清空试算输入和结果，保证下次打开时重新填写。 */
function resetCalculateDialog() {
  calculateBindings.value = [];
  calculateResults.value = [];
}

/** 打开试算弹窗，根据当前规则准备输入项，并清掉上一次的试算结果。 */
function handleCalculate() {
  // 弹窗挂在当前页面容器中，沿用平台弹窗主题，也支持在新增方案弹窗内打开。
  calculateDialogTarget.value = ruleTableRef.value?.$el.closest('.app-container') || 'body';
  calculateBindings.value = createCalculateBindings();
  calculateResults.value = [];
  if (!calculateBindings.value.length) startCalculate();
  calculateDialogVisible.value = true;
}

/** 算出一条规则在某次试算中要输出的内容，流水号根据起始值和步长模拟，不占用真实号码。 */
function getTrialSegmentOutput(rule, resultIndex) {
  if (rule.segmentType === 'FIXED_TEXT') {
    return rule.config.fixedContent || '';
  }

  if (['BUSINESS_FIELD', 'REFERENCE_MAPPING'].includes(rule.segmentType)) {
    return calculateBindings.value.find((item) => item.ruleKey === rule.clientKey)?.sampleValue || '';
  }

  if (rule.segmentType === 'DATETIME') {
    return rule.config.exampleOutput || '';
  }

  if (rule.segmentType === 'SERIAL_NUMBER') {
    // 第 N 条模拟流水号 = 起始值 + N * 步长；试计算仅展示结果，不占用真实流水号。
    const serialValue = Number(rule.config.startValue) + resultIndex * Number(rule.config.step);
    return rule.config.paddingMethod === 'LEFT_ZERO'
        ? String(serialValue).padStart(Number(rule.config.digits), '0')
        : String(serialValue);
  }

  return '';
}

/** 检查试算输入是否填完整，再按规则顺序生成模拟编码，并把结果通知父页面。 */
function startCalculate() {
  // 依赖字段的规则必须同时具备模拟绑定字段和示例值，否则无法得到完整编码。
  const incompleteBinding = calculateBindings.value.find((item) => (
      !item.boundField?.trim() || !item.sampleValue?.trim()
  ));
  if (incompleteBinding) {
    proxy.$modal.msgWarning(`请完善参数“${incompleteBinding.parameterName}”的模拟绑定字段和示例值`);
    return;
  }

  const results = Array.from({length: trialResultCount}, (_, resultIndex) => (
      rules.value.map((rule) => getTrialSegmentOutput(rule, resultIndex)).join('')
  ));
  calculateResults.value = results;
  emit('calculate', {
    bindings: calculateBindings.value.map((item) => ({...item})),
    rules: [...rules.value],
    results
  });
}

/** 把页面规则整理成后端保存接口需要的字段；已保存规则带真实 ID，新规则不带 ID，页面标识不提交。 */
function mapRuleToApiPayload(rule) {
  const payload = {
    type: rule.segmentType,
    sortOrder: Number(rule.sortOrder),
    fixedValue: '',
    sequenceLength: null,
    sequenceStart: null,
    padChar: '',
    config: '{}',
    resetMethod: '',
    stepSize: null
  };

  // 只有已落库规则携带 ID；临时规则不传 ID，后端据此执行新增。
  if (rule.persisted && rule.id != null) {
    payload.id = rule.id;
  }

  if (rule.segmentType === 'FIXED_TEXT') {
    payload.fixedValue = rule.config.fixedContent;
  } else if (rule.segmentType === 'REFERENCE_MAPPING') {
    // 参考数据映射的 fixedValue 约定保存参考数据类型 ID，不保存下拉框展示名称。
    payload.fixedValue = rule.config.fieldParameterName == null || rule.config.fieldParameterName === ''
        ? null
        : Number(rule.config.fieldParameterName);
  } else if (rule.segmentType === 'BUSINESS_FIELD') {
    payload.fixedValue = rule.config.fieldParameterName;
  } else if (rule.segmentType === 'DATETIME') {
    payload.fixedValue = rule.config.dateTimeFormat;
    // 数据表无独立“示例输出”列，使用 config JSON 保存该扩展属性。
    payload.config = JSON.stringify({exampleOutput: rule.config.exampleOutput});
  } else if (rule.segmentType === 'SERIAL_NUMBER') {
    payload.sequenceLength = rule.config.digits;
    payload.sequenceStart = rule.config.startValue;
    // padChar 直接保存 model_padding_method 的字典值，不再转换为 0/1 或补位字符。
    payload.padChar = rule.config.paddingMethod;
    payload.resetMethod = rule.config.resetMethod;
    payload.stepSize = rule.config.step;
  }

  return payload;
}

/**
 * 按规则顺序组装保存数据，规则的版本归属由保存入口确定。
 * @returns {Object[]} 待保存的编码规则段
 */
function buildSavePayload() {
  sortRulesByOrder();
  normalizeRuleOrders();
  return rules.value.map(mapRuleToApiPayload);
}

/** 把当前版本的全部规则一次性保存到后端，成功后刷新列表，拿到新增规则的正式编号。 */
async function handleSave() {
  if (versionStatusLocked.value) return;
  if (!encodingVersionId.value) {
    proxy.$modal.msgWarning('当前编码方案没有可用的版本，无法保存规则');
    return;
  }

  const items = buildSavePayload();
  savingRules.value = true;
  try {
    await saveEncodingItems({
      encodingVersionId: Number(encodingVersionId.value),
      items
    });
    proxy.$modal.msgSuccess('规则保存成功');
    emit('save', {
      encodingVersionId: Number(encodingVersionId.value),
      items,
      deletedIds: [...deletedRuleIds.value]
    });
    refreshRuleTable();
  } finally {
    savingRules.value = false;
  }
}

/** 找到要修改的规则，把已有内容填回表单，再打开编辑弹窗。 */
function handleEdit(rule, index) {
  // 优先使用表格传入的下标；下标不对应时，用页面标识找到真正要编辑的规则。
  const targetIndex = rules.value[index]?.clientKey === rule.clientKey
      ? index
      : rules.value.findIndex((item) => item.clientKey === rule.clientKey);
  if (targetIndex === -1) return;

  resetRuleForm();
  editingRuleIndex.value = targetIndex;
  ruleForm.value = {
    ...createEmptyRuleForm(),
    ...rule.config,
    segmentType: rule.segmentType,
    order: rule.sortOrder
  };
  if (ruleForm.value.segmentType === 'SERIAL_NUMBER') {
    ruleForm.value.resetMethod = getOptionValue(model_reset_method.value, ruleForm.value.resetMethod);
  }
  // 编辑已有参考数据映射规则时，补加载下拉选项以便正确显示当前 ID 对应的名称。
  if (ruleForm.value.segmentType === 'REFERENCE_MAPPING') loadDictTypeOptions();
  ruleDialogVisible.value = true;
  nextTick(() => ruleFormRef.value?.clearValidate());
}

watch(model_reset_method, (options) => {
  if (ruleDialogVisible.value && ruleForm.value.segmentType === 'SERIAL_NUMBER') {
    ruleForm.value.resetMethod = getOptionValue(options, ruleForm.value.resetMethod);
  }
}, {deep: true});

/** 确认后先从页面移除规则并整理顺序，记录已保存规则的编号，点击“保存规则”后才真正删除。 */
function handleDelete(rule, index) {
  const currentIndex = Number.isInteger(index)
      ? index
      : rules.value.findIndex((item) => item.clientKey === rule.clientKey);
  proxy.$modal.confirm(`是否确认删除排序为 ${rule.sortOrder} 的“${rule.type}”规则段？`).then(() => {
    const targetIndex = rules.value.findIndex((item) => item.clientKey === rule.clientKey);
    if (targetIndex === -1) return;

    const [deletedRule] = rules.value.splice(targetIndex, 1);
    normalizeRuleOrders();
    // 未落库规则无需交给后端删除；已落库规则随整套规则保存时从数据库移除。
    if (deletedRule.persisted) {
      deletedRuleIds.value.push(deletedRule.id);
    }
    emit('delete', {
      rule: deletedRule,
      index: currentIndex,
      rules: [...rules.value]
    });
    emit('ruleCount', rules.value.length);
    proxy.$modal.msgSuccess(props.draftMode ? '已移除规则段' : '已移除规则段，点击“保存规则”后生效');
  }).catch(() => {
  });
}

defineExpose({buildSavePayload, handleCalculate});
</script>

<style scoped lang="scss">

.type-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  height: 100%;
}

.type-card :deep(.el-card__header) {
  padding: 10px !important;
  background: linear-gradient(
      80deg,
      var(--el-color-primary-light-9) 0%,
      var(--el-fill-color-blank) 100%
  );
}

.type-card :deep(.el-card__body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 0 !important;
}

.type-card__header {
  gap: var(--qt-space-2);
}

.type-card__header strong {
  font-size: var(--qt-font-size-emphasis);
  font-weight: var(--qt-font-weight-bold);
}

.type-card__header :deep(.el-tag) {
  height: 24px;
  padding: 0 var(--qt-space-2);
}

.type-card__description {
  flex: 1;
  min-height: 62px;
  padding: var(--qt-space-3) 14px;
  box-sizing: border-box;
  overflow: hidden;
}

.type-card__description .desc-text {
  margin: 0;
}

.type-example {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 40px;
  padding: var(--qt-space-2) 14px;
  box-sizing: border-box;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: var(--qt-font-size-secondary);
  white-space: nowrap;
  color: var(--el-text-color-secondary);
}

.type-example code {
  margin-left: var(--qt-space-1);
  color: var(--el-color-primary);
}

.example-arrow {
  margin: 0 6px;
}

.mb15 {
  margin-bottom: 15px;
}

:global(.encoding-dialog .domain-intro) {
  --tip-title-weight: normal;
  --tip-title-color: var(--el-text-color-regular);
  margin-bottom: 14px;
}

:global(.encoding-dialog .el-form-item.has-tip .tip-content span) {
  overflow: visible;
  text-overflow: clip;
  white-space: normal;
}

.domain-intro {
  --tip-title-weight: normal;
  --tip-title-color: var(--el-text-color-regular);
  margin-bottom: 14px;
}

.full-width {
  width: 100%;
}

</style>
