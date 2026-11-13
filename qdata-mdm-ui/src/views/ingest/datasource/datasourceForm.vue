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
  <el-form
    ref="formRef"
    :model="internalForm"
    :rules="rules"
    :label-width="labelWidth"
    class="column-form"
    @submit.prevent
  >
    <!-- 数据连接类型和数据连接名称 -->
    <!-- 数据连接类型 -->
    <el-form-item v-if="showDatasourceType" label="数据连接类型">
      <dict-tag
        v-if="datasourceType"
        :type="getDatasourceActive(datasourceType)?.type || 'primary'"
        :datasource-type="datasourceType"
      >{{ getDatasourceActive(datasourceType)?.label || datasourceType }}</dict-tag>
    </el-form-item>
    <!-- 数据连接名称 -->
    <template
      v-if="showDatasourceType"
      v-for="field in visibleFields.filter((f) => f.key === 'datasourceName')"
      :key="field.key"
    >
      <!-- 根据是否有 tip 选择不同的 form-item 组件 -->
      <component
        :is="field.tip ? 'qt-form-item' : 'el-form-item'"
        :label="field.label"
        :prop="field.prop"
        :rules="getFieldRules(field)"
        :tip="field.tip ? { content: field.tip } : undefined"
      >
        <!-- Input（datasourceName 专用） -->
        <template v-if="field.component === 'input'">
          <el-input
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="
              (val) => {
                setFieldValue(field.prop, val);
              }
            "
            :placeholder="field.placeholder"
            :maxlength="getFieldMaxLength(field)"
            :disabled="field.disabledOnEdit && isEditMode"
            @blur="trimFieldValue(field.prop)"
          />
          <div v-else class="form-readonly">
            {{ getFieldValue(field.prop) || "-" }}
          </div>
        </template>
      </component>
    </template>

    <!-- 动态渲染字段（排除已单独处理的 datasourceName） -->
    <template
      v-for="field in visibleFields.filter((f) => f.key !== 'datasourceName')"
      :key="field.key"
    >
      <!-- 根据是否有 tip 选择不同的 form-item 组件 -->
      <!-- :is="field.tip ? 'qt-form-item' : 'el-form-item'" -->

      <component
        :is="'el-form-item'"
        :class="{ 'row-full': field.component === 'textarea' }"
        :label="field.label"
        :prop="field.prop"
        :rules="getFieldRules(field)"
        :tip="field.tip ? { content: field.tip } : undefined"
      >
        <!-- Input -->
        <template v-if="field.component === 'input'">
          <el-input
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="(val) => setFieldValue(field.prop, val)"
            :placeholder="field.placeholder"
            :maxlength="getFieldMaxLength(field)"
            :disabled="field.disabledOnEdit && isEditMode"
            @blur="trimFieldValue(field.prop)"
          />
          <div v-else class="form-readonly">
            {{ getFieldValue(field.prop) || "-" }}
          </div>
        </template>

        <!-- Password -->
        <template v-else-if="field.component === 'password'">
          <el-input
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="(val) => setFieldValue(field.prop, val)"
            type="password"
            show-password
            :placeholder="field.placeholder"
            :maxlength="getFieldMaxLength(field)"
            @blur="trimFieldValue(field.prop)"
          />
          <div v-else class="form-readonly">
            {{ getFieldValue(field.prop) ? "******" : "-" }}
          </div>
        </template>

        <!-- Textarea -->
        <template v-else-if="field.component === 'textarea'">
          <el-input
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="(val) => setFieldValue(field.prop, val)"
            type="textarea"
            :placeholder="field.placeholder"
            maxlength="256个字符"
            show-word-limit
            :min-height="192"
          />
          <div v-else class="form-readonly textarea">
            {{ getFieldValue(field.prop) || "-" }}
          </div>
        </template>

        <!-- Radio -->
        <template v-else-if="field.component === 'radio'">
          <el-radio-group
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="
              (val) => {
                setFieldValue(field.prop, val);
                handleFieldChange(field);
              }
            "
          >
            <el-radio
              v-for="option in field.options"
              :key="option.value"
              :label="option.value"
            >
              {{ option.label }}
            </el-radio>
          </el-radio-group>
          <div v-else class="form-readonly">
            {{ getRadioLabel(field, getFieldValue(field.prop)) }}
          </div>
        </template>

        <!-- Dict Radio -->
        <template v-else-if="field.component === 'dict-radio'">
          <el-radio-group
            v-if="mode !== 'detail'"
            :model-value="getFieldValue(field.prop)"
            @update:model-value="(val) => setFieldValue(field.prop, val)"
          >
            <el-radio
              v-for="dict in dictOptions[field.dictType] || []"
              :key="dict.value"
              :label="dict.value === '1'"
            >
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
          <dict-tag
            v-else
            :options="dictOptions[field.dictType] || []"
            :value="getFieldValue(field.prop) ? '1' : '0'"
          />
        </template>
      </component>
    </template>
  </el-form>
</template>

<script setup name="DatasourceForm">
import { ref, computed, watch, getCurrentInstance, onMounted } from "vue";

import { getDatasourceActive } from "@/components/Datasource/utils";

const baseFields = [
  {
    key: 'datasourceName',
    label: '数据连接名称',
    prop: 'datasourceName',
    required: true,
    component: 'input',
    placeholder: '请输入数据连接名称',
    colSpan: 24
  }
];

const ipPortFields = [
  {
    key: 'ip',
    label: 'IP',
    prop: 'ip',
    required: true,
    component: 'input',
    placeholder: '请输入IP',
    tip: '数据连接服务器的 IP 地址，支持IPv4',
    colSpan: 12
  },
  {
    key: 'port',
    label: '端口号',
    prop: 'port',
    required: true,
    component: 'input',
    placeholder: '请输入端口号',
    colSpan: 12
  }
];

const authFields = [
  {
    key: 'username',
    label: '账号',
    prop: 'username',
    required: true,
    component: 'input',
    placeholder: '请输入账号',
    tip: '连接数据连接所用的账号，需要具有访问权限',
    colSpan: 12
  },
  {
    key: 'password',
    label: '密码',
    prop: 'password',
    required: true,
    component: 'password',
    placeholder: '请输入密码',
    tip: '对应账号的登录密码，系统会加密存储',
    colSpan: 12
  }
];

const dbNameField = [
  {
    key: 'dbname',
    label: '数据库名称',
    prop: 'dbname',
    required: true,
    component: 'input',
    placeholder: '请输入数据库名称',
    disabledOnEdit: true,
    colSpan: 12
  }
];

const sidField = [
  {
    key: 'sid',
    label: '模式名称',
    prop: 'sid',
    required: true,
    component: 'input',
    placeholder: '请输入模式名称',
    disabledOnEdit: true,
    colSpan: 12
  }
];

const commonConfigFields = [
  {
    key: 'allowOperate',
    label: '限制操作',
    prop: 'allowOperate',
    required: false,
    component: 'radio',
    options: [
      { label: '不限制', value: 0 },
      { label: '限制', value: 1 }
    ],
    defaultValue: 0,
    colSpan: 12
  },
  {
    key: 'validFlag',
    label: '状态',
    prop: 'validFlag',
    required: false,
    component: 'dict-radio',
    dictType: 'sys_disable',
    defaultValue: 1,
    colSpan: 12
  },
  {
    key: 'description',
    label: '描述',
    prop: 'description',
    required: false,
    component: 'textarea',
    placeholder: '请输入描述，建议说明数据连接的用途、连接的数据内容等信息',
    maxlength: 500,
    colSpan: 24
  },
];

const sslFieldsConfig = {
  MySql: [
    {
      key: 'sslConfig.trustCertificateKeyStoreUrl',
      label: '信任证书路径',
      prop: 'sslConfig.trustCertificateKeyStoreUrl',
      required: true,
      component: 'input',
      placeholder: '请输入信任证书路径',
      colSpan: 12
    },
    {
      key: 'sslConfig.trustCertificateKeyStorePassword',
      label: '信任证书密码',
      prop: 'sslConfig.trustCertificateKeyStorePassword',
      required: true,
      component: 'password',
      placeholder: '请输入信任证书密码',
      colSpan: 12
    }
  ],
  SQL_Server: [
    {
      key: 'sslConfig.trustStore',
      label: '信任证书路径',
      prop: 'sslConfig.trustStore',
      required: true,
      component: 'input',
      placeholder: '请输入信任证书路径',
      colSpan: 12
    },
    {
      key: 'sslConfig.trustStorePassword',
      label: '信任证书密码',
      prop: 'sslConfig.trustStorePassword',
      required: true,
      component: 'password',
      placeholder: '请输入信任证书密码',
      colSpan: 12
    }
  ],
  DM8: [
    {
      key: 'sslConfig.sslFilesPath',
      label: 'SSL 目录',
      prop: 'sslConfig.sslFilesPath',
      required: true,
      component: 'input',
      placeholder: '请输入SSL目录',
      tip: '低版本jdk请使用jks格式并指定.keystore.jks',
      colSpan: 12
    },
    {
      key: 'sslConfig.sslKeystorePass',
      label: 'SSL 验证密码',
      prop: 'sslConfig.sslKeystorePass',
      required: true,
      component: 'password',
      placeholder: '请输入SSL验证密码',
      colSpan: 12
    }
  ]
};

const generateSslFields = (type) => {
  return [
    {
      key: 'useSSL',
      label: 'SSL 认证',
      prop: 'useSSL',
      required: false,
      component: 'radio',
      options: [
        { label: '否', value: 0 },
        { label: '是', value: 1 }
      ],
      defaultValue: 0,
      colSpan: 24,
      triggersChildFields: true
    },
    ...(sslFieldsConfig[type] || []).map(field => ({
      ...field,
      showWhen: (form) => form.useSSL === 1
    }))
  ];
};

const withDefaults = (fields, defaults = {}) => {
  return fields.map(field => {
    if (defaults[field.key] !== undefined && field.defaultValue === undefined) {
      return { ...field, defaultValue: defaults[field.key] };
    }
    return field;
  });
};

const standardRelationalConfig = (type, options = {}) => {
  const { hasSid = false, hasSsl = false, hasDbName = true, defaults = {} } = options;
  const fields = [...baseFields, ...ipPortFields];

  fields.push(...authFields);

  if (hasDbName) {
    fields.push(...dbNameField);
  }

  if (hasSid) {
    fields.push(...sidField);
  }

  if (hasSsl) {
    fields.push(...generateSslFields(type));
  }

  fields.push(...commonConfigFields);
  return withDefaults(fields, defaults);
};

const oceanBaseAuthFields = authFields.map((field) => {
  if (field.key !== 'username') {
    return { ...field };
  }

  return {
    ...field,
    placeholder: '用户名@租户名 或 用户名@租户名#集群名',
    tip: '请填写完整 OceanBase 账号'
  };
});

const datasourceFieldsConfig = {
  // 关系型数据库
  MySql: standardRelationalConfig('MySql', {
    hasDbName: true,
    hasSsl: true,
    defaults: { ip: 'localhost', port: '3306', username: 'root' }
  }),
  Oracle: standardRelationalConfig('Oracle', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '1521', username: 'system', sid: 'orcl' }
  }),
  Oracle11: standardRelationalConfig('Oracle11', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '1521', username: 'system', sid: 'orcl' }
  }),
  PostgreSQL: standardRelationalConfig('PostgreSQL', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '5432', username: 'postgres', dbname: 'postgres', sid: 'public' }
  }),
  SQL_Server: standardRelationalConfig('SQL_Server', {
    hasSid: true,
    hasDbName: true,
    hasSsl: true,
    defaults: { ip: 'localhost', port: '1433', username: 'sa', dbname: 'master', sid: 'dbo' }
  }),
  SQL_Server2008: standardRelationalConfig('SQL_Server2008', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '1433', username: 'sa', dbname: 'master', sid: 'dbo' }
  }),
  DB2: standardRelationalConfig('DB2', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '50000', username: 'db2inst1' }
  }),
  openGauss: standardRelationalConfig('openGauss', { hasSid: true, hasDbName: true }),
  GaussDB: standardRelationalConfig('GaussDB', { hasSid: true, hasDbName: true }),

  // 国产数据库
  DM8: standardRelationalConfig('DM8', {
    hasDbName: true,
    hasSsl: true,
    defaults: { ip: 'localhost', port: '5236', username: 'SYSDBA' }
  }),
  Kingbase8: standardRelationalConfig('Kingbase8', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '54321', username: 'SYSTEM', dbname: 'TEST', sid: 'public' }
  }),
  GBase: standardRelationalConfig('GBase', {
    hasDbName: true,
    defaults: { ip: 'localhost', port: '5258', username: 'gbase' }
  }),
  OSCAR: standardRelationalConfig('OSCAR', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '2003', username: 'SYSDBA', dbname: 'OSRDB' }
  }),
  GBase8c: standardRelationalConfig('GBase8c', { hasSid: true, hasDbName: true }),
  HighGo: standardRelationalConfig('HighGo', {
    hasSid: true,
    hasDbName: true,
    defaults: { ip: 'localhost', port: '5866', sid: 'public' }
  }),
    OCEANBASE: withDefaults([
    ...baseFields,
    ...ipPortFields,
    ...oceanBaseAuthFields,
    {
      key: 'dbname',
      label: '数据库名称',
      prop: 'dbname',
      required: true,
      component: 'input',
      placeholder: '请输入数据库名称',
      colSpan: 12
    },
    ...commonConfigFields
  ], { ip: 'localhost', port: '2883', username: 'root@sys' }),
  OCEANBASE_ORACLE: withDefaults([
    ...baseFields,
    ...ipPortFields,
    ...oceanBaseAuthFields,
    {
      key: 'dbname',
      label: 'Schema',
      prop: 'dbname',
      required: true,
      component: 'input',
      placeholder: '请输入 Schema',
      colSpan: 12
    },
    ...commonConfigFields
  ], { ip: 'localhost', port: '2883', username: 'root@sys' }),

};

function getDatasourceFields(type) {
  return datasourceFieldsConfig[type] || standardRelationalConfig(type, { hasDbName: true });
}

const { proxy } = getCurrentInstance();

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({}),
  },
  datasourceType: {
    type: String,
    default: "",
  },
  mode: {
    type: String,
    default: "add", // 'add' | 'edit' | 'detail'
    validator: (value) => ["add", "edit", "detail"].includes(value),
  },
  labelWidth: {
    type: String,
    default: "110px",
  },
  showDatasourceType: {
    type: Boolean,
    default: true,
  },
  dictOptions: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(["update:modelValue", "fieldChange"]);

const formRef = ref(null);

const MAX_LENGTH_FIELDS = new Set([
  "datasourceName",
  "ip",
  "username",
  "password",
  "dbname",
  "sid",
]);

// 使用本地 ref 状态
const internalForm = ref({});

// 初始化和同步函数
function syncFromPropsInternal() {
  const model = JSON.parse(JSON.stringify(props.modelValue || {}));

  // 应用默认值
  if (props.datasourceType && props.mode === 'add') {
    const fields = getDatasourceFields(props.datasourceType);
    fields.forEach((field) => {
      if (field.defaultValue !== undefined) {
        const keys = field.prop.split('.');
        let current = model;
        for (let i = 0; i < keys.length - 1; i++) {
          if (current[keys[i]] === undefined) {
            current[keys[i]] = {};
          }
          current = current[keys[i]];
        }
        const lastKey = keys[keys.length - 1];
        if (current[lastKey] == null || current[lastKey] === "") {
          current[lastKey] = field.defaultValue;
        }
      }
    });
  }

  internalForm.value = {
    ...model,
    datasourceType: props.datasourceType,
  };
}

// 监听 props.modelValue 的 id 变化来同步（避免每次修改都触发）
// 只有在 id 真正变化（从 null/undefined 变成有值，或者不同 id）时才同步整个表单
let lastSyncedId = null;
watch(
  () => props.modelValue?.id,
  (newId) => {
    if (newId !== lastSyncedId) {
      lastSyncedId = newId;
      syncFromPropsInternal();
    }
  },
  { immediate: true }
);

// 监听 datasourceType 变化 - 只有当类型真正变化时才同步
let lastSyncedType = null;
watch(
  () => props.datasourceType,
  (newType) => {
    if (newType !== lastSyncedType) {
      lastSyncedType = newType;
      syncFromPropsInternal();
    }
  },
  { immediate: true }
);

// 当前数据源类型的字段配置
const currentFields = computed(() => {
  return getDatasourceFields(props.datasourceType);
});

// 可见字段（根据 showWhen 条件过滤）
const visibleFields = computed(() => {
  return currentFields.value.filter((field) => {
    if (field.showWhen && typeof field.showWhen === "function") {
      return field.showWhen(internalForm.value);
    }
    return true;
  });
});

// 是否为编辑模式
const isEditMode = computed(() => props.mode === "edit");

// 获取字段值（支持嵌套属性）
function getFieldValue(prop) {
  const keys = prop.split(".");
  let value = internalForm.value;
  for (const key of keys) {
    if (value === null || value === undefined) {
      return undefined;
    }
    value = value[key];
  }
  return value;
}

// 设置字段值（支持嵌套属性）
function setFieldValue(prop, value) {
  const keys = prop.split(".");
  let obj = internalForm.value;

  for (let i = 0; i < keys.length - 1; i++) {
    const key = keys[i];
    if (obj[key] === null || obj[key] === undefined) {
      obj[key] = {};
    }
    obj = obj[key];
  }

  obj[keys[keys.length - 1]] = value;

  // 第二步表单独立工作，不实时 emit 到父组件，避免递归
  // 父组件可以在需要时通过 validate 获取当前值，或者在提交前手动同步
}

function getFieldMaxLength(field) {
  // InfluxDB 2.x Token commonly exceeds the default 50-character credential limit.
  if (props.datasourceType === "InfluxDB" && field.prop === "password") {
    return 512;
  }
  return MAX_LENGTH_FIELDS.has(field.prop) ? 50 : undefined;
}

function trimFieldValue(prop) {
  if (!MAX_LENGTH_FIELDS.has(prop)) {
    return;
  }

  const value = getFieldValue(prop);
  if (typeof value === "string") {
    setFieldValue(prop, value.trim());
  }
}

function trimValidatedFields() {
  currentFields.value.forEach((field) => trimFieldValue(field.prop));
}

// 获取字段验证规则
function getFieldRules(field) {
  if (props.mode === "detail") {
    return [];
  }

  const rules = [];

  // 必填和去除空格规则
  if (field.required) {
    const isRequired =
      typeof field.required === "function"
        ? field.required(internalForm.value)
        : field.required;

    if (isRequired) {
      rules.push({
        required: true,
        message: `${field.label}不能为空`,
        trigger: ["blur", "change"],
        transform: (value) =>
          value === null || value === undefined ? value : String(value).trim(),
      });
      rules.push({
        validator: (rule, value, callback) => {
          if (typeof value === "string" && value.trim() === "") {
            callback(new Error(`${field.label}不能为空`));
          } else {
            callback();
          }
        },
        trigger: ["blur", "change"],
      });
    }
  }

  // 长度限制
  if (
    ["datasourceName", "ip", "username", "password", "dbname", "sid"].includes(
      field.prop
    )
  ) {
    const maxLength = getFieldMaxLength(field);
    rules.push({
      max: maxLength,
      message: `${field.label}长度不能超过${maxLength}个字符`,
      trigger: ["blur", "change"],
    });
  }

  // 端口验证规则 (1-65535)
  if (field.prop === "port") {
    rules.push({
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value === "") {
          callback();
          return;
        }

        const port = String(value).trim();
        if (!/^\d+$/.test(port)) {
          callback(new Error("端口仅支持输入正整数"));
          return;
        }

        const portNumber = Number(port);
        if (portNumber < 1 || portNumber > 65535) {
          callback(new Error("端口范围应为1～65535"));
          return;
        }

        callback();
      },
      trigger: ["blur", "change"],
    });
  }

  // 配置参数 JSON 验证
  if (field.prop === "config") {
    rules.push({
      trigger: "blur",
      validator: (rule, value, callback) => {
        if (!value || value.trim() === "") {
          callback();
          return;
        }
        try {
          JSON.parse(value);
          callback();
        } catch (e) {
          callback("不是一个正确的JSON格式");
        }
      },
    });
  }

  // SID 特殊验证
  if (field.prop === "sid") {
    rules.push({
      trigger: "blur",
      validator: (rule, value, callback) => {
        if (props.datasourceType === "GBase") {
          callback();
          return;
        }
        if (value === null || value === undefined || value === "") {
          callback(new Error("模式不能为空"));
          return;
        }
        callback();
      },
    });
  }

  return rules;
}

// 生成验证规则对象（用于 el-form）
const rules = computed(() => {
  const rulesObj = {};
  currentFields.value.forEach((field) => {
    rulesObj[field.prop] = getFieldRules(field);
  });
  return rulesObj;
});

// 获取 Radio 选项的显示文本
function getRadioLabel(field, value) {
  const option = field.options?.find((opt) => opt.value === value);
  return option?.label || (value === 0 ? "否" : value === 1 ? "是" : value);
}

// 处理字段值变化
function handleFieldChange(field) {
  emit("fieldChange", { field, value: getFieldValue(field.prop) });
}

// 暴露方法给父组件
defineExpose({
  formRef,
  validate: () => {
    trimValidatedFields();
    return formRef.value?.validate();
  },
  resetFields: () => {
    syncFromPropsInternal();
    formRef.value?.resetFields();
  },
  clearValidate: () => formRef.value?.clearValidate(),
  syncFromProps: syncFromPropsInternal,
  // 获取当前表单数据
  getFormData: () => ({ ...internalForm.value }),
  // 手动同步到父组件
  syncToParent: () => {
    emit("update:modelValue", { ...internalForm.value });
  },
});
</script>

<style lang="scss" scoped>
.form-readonly {
  line-height: 32px;
  color: var(--el-text-color-secondary);

  &.textarea {
    min-height: 40px;
    line-height: 1.5;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>
