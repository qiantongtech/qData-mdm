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

  Business License: https://docs.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<!--
  数据服务详情：展示已保存的请求与响应配置、授权概况及调用记录。
-->
<template>
  <div v-loading="loading" class="app-container">
    <el-empty v-if="detailError" description="数据服务详情加载失败">
      <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
    </el-empty>
    <template v-else-if="detail.id">
      <detail-info
        :data="detail"
        mode="free"
        :header="{ idKey: 'id', nameKey: 'name', statusKey: 'status', statusOptions }"
        :items="detailItems"
        :time-format="timeFormat"
      >
        <template #model>
          <div v-if="detail.modelName" class="qt-table-info"><div class="qt-table-info__main">
            <img :src="getModelIcon(modelInfo)" alt="" class="info-icon" />
            <span class="text-ellipsis" :title="modelTitle">{{ modelTitle }}</span>
            <dict-tag v-if="modelInfo.type" :options="MODEL_TYPE_OPTIONS" :value="modelInfo.type" size="small" type="primary" />
            <dict-tag v-if="modelInfo.versionNo" :value="modelInfo.versionNo" size="small" type="primary" />
          </div></div>
          <span v-else>-</span>
        </template>
        <template #location>
          <div v-if="locationName" class="qt-table-info"><div class="qt-table-info__main">
            <img :src="getProjectIcon(locationIcon)" alt="" class="info-icon" />
            <span class="text-ellipsis" :title="locationName">{{ locationName }}</span>
          </div></div>
          <span v-else>-</span>
        </template>
      </detail-info>
      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab" class="demo-tabs">
          <qt-tab-pane label="请求参数" name="query" class="column-form request-response">
            <qt-wrap
              :columns="queryTable.columns"
              :table-ref="queryTableRef"
              :config="fieldWrapConfig"
              class="row-full"
            >
              <template #actions-data>
                <div class="h2-titles">请求参数列表</div>
              </template>
              <qt-table :key="`query-${detail.id}`" ref="queryTableRef" v-bind="queryTable">
                <template #queryExampleValue="{ row }">
                  {{ row.queryExampleValue || '-' }}
                </template>
              </qt-table>
            </qt-wrap>
            <div class="h2-titles row-full">请求示例</div>
            <div v-if="requestExample" class="example-editor row-full">
              <sql-editor :value="requestExample" mode="shell" theme="default" auto-height
                          :read-only="true" :autofocus="false" />
            </div>
            <el-empty v-else description="暂无请求示例" class="row-full" />
          </qt-tab-pane>
          <qt-tab-pane label="响应参数" name="response" class="column-form request-response">
            <qt-wrap
              :columns="outputTable.columns"
              :table-ref="outputTableRef"
              :config="fieldWrapConfig"
              class="row-full"
            >
              <template #actions-data><div class="h2-titles row-full">响应参数列表</div></template>
              <qt-table :key="`output-${detail.id}`" ref="outputTableRef" v-bind="outputTable">
                <template #exampleValue="{ row }">{{ row.exampleValue == null || row.exampleValue === '' ? '-' : row.exampleValue }}</template>
              </qt-table>
            </qt-wrap>
            <div class="example-heading row-full">
              <div class="h2-titles">响应示例</div>
              <el-button v-if="responseExample" link type="primary" icon="CopyDocument"
                         @click="copyExample(responseExample)">复制 JSON</el-button>
            </div>
            <div v-if="responseExample" class="example-editor row-full">
              <sql-editor :value="responseExample" mode="application/json" theme="default" auto-height
                          :read-only="true" :autofocus="false" />
            </div>
            <el-empty v-else description="暂无返回字段" class="row-full" />
          </qt-tab-pane>
          <qt-tab-pane label="授权记录" name="authorization" class="column-form">
            <div class="h2-titles row-full">授权概况</div>
            <el-alert
              v-if="authorizationSummaryError"
              title="授权概况加载失败"
              type="error"
              :closable="false"
              show-icon
              class="row-full"
            >
              <el-button link type="primary" @click="authorizationTableRef?.getList()">重新加载</el-button>
            </el-alert>
            <descriptions-info
              v-else
              :data="authorizationSummary"
              :items="authorizationItems"
              :time-format="timeFormat"
              class="basicInfo row-full"
            />
            <qt-wrap
              :columns="authorizationTable.columns"
              :table-ref="authorizationTableRef"
              :config="fieldWrapConfig"
              class="row-full"
            >
              <template #actions-data><div class="h2-titles row-full">授权记录列表</div></template>
              <qt-table :key="`authorization-${detail.id}`" ref="authorizationTableRef" v-bind="authorizationTable">
                <template #scope="{ row }">
                  {{ row.dataScopeType === 'ALL' ? '全部数据' : row.dataScopeType === 'CONDITION' ? '条件范围' : '-' }}
                </template>
                <template #expireTime="{ row }">
                  {{ row.longTerm ? '长期有效' : proxy.parseTime(row.expireTime, timeFormat) || '-' }}
                </template>
                <template #action="{ row }">
                  <el-button
                    link
                    type="primary"
                    icon="View"
                    v-hasPermi="['auth:service:query']"
                    @click="openAuthorizationDetail(row)"
                  >查看</el-button>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
          <qt-tab-pane label="调用记录" name="calls" class="column-form">
            <div class="h2-titles row-full">调用情况</div>
            <descriptions-info
              :data="callSummary"
              :items="callItems"
              :time-format="timeFormat"
              class="basicInfo row-full"
            />
            <div class="h2-titles row-full">最近一次调用摘要</div>
            <descriptions-info :data="lastCall" :items="lastCallItems" class="basicInfo row-full" />
            <qt-wrap
              :columns="callTable.columns"
              :table-ref="callTableRef"
              :config="fieldWrapConfig"
              class="row-full"
            >
              <template #actions-data><div class="h2-titles row-full">调用记录列表</div></template>
              <qt-table :key="`calls-${detail.id}`" ref="callTableRef" v-bind="callTable">
                <template #action="{ row }">
                  <el-button
                    link
                    type="primary"
                    icon="View"
                    @click="openCallDetail(row)"
                  >详情</el-button>
                </template>
              </qt-table>
            </qt-wrap>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </template>
    <el-dialog
      v-model="callDetailVisible"
      title="调用记录详情"
      width="900px"
      destroy-on-close
    >
      <el-form :model="callDetail" label-width="120px" class="column-form">
        <el-form-item
          v-for="item in callDetailItems"
          :key="item.key"
          :label="item.label"
          :class="{ 'row-full': item.span === 2 }"
        >
          <div class="form-readonly">{{ formatCallDetailValue(item) }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="callDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DistServiceDetail">
import { computed, getCurrentInstance, ref, watch } from 'vue';

import { useRoute } from 'vue-router';
import { useClipboard } from '@vueuse/core';

import SqlEditor from '@/components/SqlEditor/index.vue';
import { getService, listServiceModels } from '@/api/dist/service/service';
import { getPublishedModelStructure } from '@/api/model/base/base';
import { listServiceAuthorizationsByServiceId } from '@/api/authorization/serviceAuthorization';
import { getServiceCallLog, getServiceCallLogSummary, listServiceCallLogs } from '@/api/dist/service/callLog';

import { getModelIcon, getProjectIcon } from '@/utils/imageUrl';
import { MODEL_TYPE_OPTIONS } from '@/utils/model/modelType';
import { usePublishedModelTree } from '@/composables/usePublishedModelTree';

const SERVICE_API_TYPE = Object.freeze({ SINGLE: '0', PAGE: '1' });

const SERVICE_FLAG = Object.freeze({ NO: '0', YES: '1' });

const SERVICE_STATUS = Object.freeze({ DISABLED: '0', ENABLED: '1' });

const SERVICE_STATUS_OPTIONS = Object.freeze([
  { label: '启用', value: SERVICE_STATUS.ENABLED },
  { label: '禁用', value: SERVICE_STATUS.DISABLED },
]);

const SERVICE_API_TYPE_OPTIONS = Object.freeze([
  { label: '单条查询', value: SERVICE_API_TYPE.SINGLE },
  { label: '列表分页查询', value: SERVICE_API_TYPE.PAGE },
]);

function serviceApiTypeLabel(value) {
  return SERVICE_API_TYPE_OPTIONS.find(option => option.value === value)?.label || value || '-';
}

// 单引号包裹动态参数，确保空格、引号等字符在复制到终端后仍作为数据传递。
const shellQuote = value => "'" + String(value).replaceAll("'", "'\\''") + "'";

const SERVICE_PAGINATION = Object.freeze({ PAGE_NUM: 1, PAGE_SIZE: 100 });

function getRequestParameters(fields, servicePath, apiType) {
  const pathCodes = new Set([...String(servicePath || '').matchAll(/\{([^{}]+)\}/g)].map(match => match[1]));
  const parameters = fields
    .filter(field => field.queryableFlag === SERVICE_FLAG.YES && field.code && field.code !== '-')
    .map(field => {
      const position = pathCodes.has(field.code) ? 'Path' : 'Query';
      return { ...field, position, required: position === 'Path' ? '是' : '否' };
    });
  if (apiType === SERVICE_API_TYPE.PAGE) {
    parameters.push(
      {
        code: 'pageNum', dataType: 'INTEGER', position: 'Query', required: '否',
        modelColumnId: 'pageNum', queryExampleValue: String(SERVICE_PAGINATION.PAGE_NUM),
        description: `页码，从 1 开始，默认 ${SERVICE_PAGINATION.PAGE_NUM}`,
      },
      {
        code: 'pageSize', dataType: 'INTEGER', position: 'Query', required: '否',
        modelColumnId: 'pageSize', queryExampleValue: String(SERVICE_PAGINATION.PAGE_SIZE),
        description: `每页条数，范围 1～100，默认 ${SERVICE_PAGINATION.PAGE_SIZE}`,
      },
    );
  }
  return parameters;
}

function getServiceRoute(serviceUrl) {
  if (!serviceUrl) return '';
  if (/^https?:\/\//i.test(serviceUrl)) return decodeURIComponent(new URL(serviceUrl).pathname);
  return String(serviceUrl).split(/[?#]/)[0];
}

function buildAbsoluteRequestUrl(requestPath, apiBase, origin) {
  if (!origin) return requestPath;
  const base = String(apiBase || '/');
  const baseUrl = new URL(base.endsWith('/') ? base : `${base}/`, origin);
  return new URL(String(requestPath).replace(/^\/+/, ''), baseUrl).toString();
}

function buildRequestExample({ httpMethod, servicePath, serviceUrl, fields, apiType, apiBase, origin }) {
  if (!servicePath || !httpMethod) return '';

  const parameters = getRequestParameters(fields, servicePath, apiType);
  const sample = field => String(field.queryExampleValue ?? '').trim();
  const notes = [];
  const serviceRoute = getServiceRoute(serviceUrl) || '/services' + servicePath;
  const path = serviceRoute.replace(/\{([^{}]+)\}/g, (_, code) => {
    const field = parameters.find(parameter => parameter.code === code);
    if (field && sample(field)) return encodeURIComponent(sample(field));
    notes.push(`# 请将路径中的 ${code} 替换为实际值（URL 编码）。`);
    return encodeURIComponent(code);
  });
  const query = parameters.filter(field => field.position === 'Query');
  const method = String(httpMethod).toUpperCase();
  const command = [
    `curl --get --request ${shellQuote(method)} ${shellQuote(buildAbsoluteRequestUrl(path, apiBase, origin))}`,
    "  --header 'Accept: application/json'",
    '  --header "Authorization: Bearer ${ACCESS_TOKEN}"',
  ];
  const needsFilter = apiType === SERVICE_API_TYPE.SINGLE && !parameters.some(field => field.position === 'Path');
  const hasSampleFilter = query.some(field => sample(field));
  for (const field of query) {
    let value = sample(field);
    if (!value && needsFilter && !hasSampleFilter && field === query[0]) value = `<${field.code}>`;
    if (value) command.push(`  --data-urlencode ${shellQuote(`${field.code}=${value}`)}`);
    else notes.push(`# 可选筛选：按需追加 --data-urlencode ${shellQuote(`${field.code}=<实际值>`)}`);
  }
  const hint = apiType === SERVICE_API_TYPE.SINGLE
    ? '# 单条查询至少需要一个筛选条件。'
    : '# 分页查询可通过 pageNum 和 pageSize 调整返回范围。';
  return ['# 将访问令牌设置到环境变量 ACCESS_TOKEN。', hint, ...notes, '', command.join(' \\\n')].join('\n');
}

const route = useRoute();
const { proxy } = getCurrentInstance();
const { copy } = useClipboard();
const statusOptions = SERVICE_STATUS_OPTIONS.map(option => ({
  ...option,
  elTagType: option.value === SERVICE_STATUS.ENABLED ? 'success' : 'danger',
}));

/**
 * 复制响应示例并反馈操作结果。
 * @param {string} value 示例代码。
 */
async function copyExample(value) {
  try {
    await copy(value);
    proxy.$modal.msgSuccess('复制成功');
  } catch {
    proxy.$modal.msgError('复制失败');
  }
}
const loading = ref(false);
const detailError = ref(false);
const detail = ref({});
const modelInfo = ref({});
const { data: modelTreeData } = usePublishedModelTree();
const modelTitle = computed(() => modelInfo.value.code ? `${detail.value.modelName} (${modelInfo.value.code})` : detail.value.modelName || '');
const modelLocation = computed(() => {
  function find(nodes, project = null, layers = []) {
    for (const node of nodes || []) {
      const currentProject = node.nodeType === 'project' ? node : project;
      const currentLayers = node.nodeType === 'layer' ? [...layers, node.name] : layers;
      if (node.nodeType === 'model' && node.modelId != null && String(node.modelId) === String(detail.value.modelId)) {
        return { name: [currentProject?.name, ...currentLayers].filter(Boolean).join(' / '), icon: currentProject?.icon };
      }
      const found = find(node.children, currentProject, currentLayers);
      if (found) return found;
    }
    return null;
  }
  return find(modelTreeData.value);
});
const locationName = computed(() => modelLocation.value?.name || '');
const locationIcon = computed(() => modelLocation.value?.icon);
const fields = ref([]);
const activeTab = ref('query');
const queryTableRef = ref(null);
const outputTableRef = ref(null);
const authorizationTableRef = ref(null);
const callTableRef = ref(null);
const callSummary = ref({});
const lastCall = ref({});
const callDetail = ref({});
const callDetailVisible = ref(false);
const authorizationSummary = ref({});
const authorizationSummaryError = ref(false);
let summaryRequestId = 0;
const timeFormat = '{y}-{m}-{d} {h}:{i}:{s}';
const fieldWrapConfig = { fullContent: false, actions: { table: { search: false } } };
const detailItems = [
  { label: 'API 类型', formatter: (_, row) => serviceApiTypeLabel(row.apiType) },
  { label: '主数据模型', slot: 'model' },
  { label: '归属位置', slot: 'location' },
  { label: '描述', key: 'description', span: 24, className: 'mt2 mb2' },
  { label: 'API 地址', key: 'servicePath' },
  {
    label: '创建人',
    formatter: (_, row) =>
            [row.createBy, row.creatorPhone]
                    .filter(value => value != null && String(value).trim())
                    .join(' / ')
  },
  { label: '创建时间', key: 'createTime', type: 'time' },
];
const queryFields = computed(() => fields.value.filter(field => field.queryableFlag === SERVICE_FLAG.YES));
const requestParameters = computed(() => getRequestParameters(queryFields.value, detail.value.servicePath, detail.value.apiType));
const outputFields = computed(() => fields.value.filter(field => field.outputFlag === SERVICE_FLAG.YES));
const requestExample = computed(() => {
  return buildRequestExample({
    ...detail.value,
    fields: queryFields.value,
    apiBase: import.meta.env.VITE_APP_BASE_API,
    origin: window.location.origin,
  });
});
const responseExample = computed(() => {
  const availableFields = outputFields.value.filter(field => field.outputName !== '-');
  if (!availableFields.length) return '';
  const record = Object.fromEntries(availableFields.map(field => [field.outputName, getExampleValue(field)]));
  const data = detail.value.apiType === SERVICE_API_TYPE.PAGE ? { rows: [record], total: 1 } : record;
  return JSON.stringify({ code: 200, data, msg: detail.value.successMessage }, null, 2);
});

/**
 * 按模型字段类型转换示例值，超出安全精度的整数保留原文。
 * @param {Object} field 字段配置。
 * @returns {*} 适合 JSON 展示的示例值。
 */
function getExampleValue(field) {
  const value = field.exampleValue;
  if (value == null || value === '') return null;
  if (['INTEGER', 'DECIMAL'].includes(field.dataType) && value.trim() !== '') {
    const number = Number(value);
    if (Number.isFinite(number) && (field.dataType !== 'INTEGER' || Number.isSafeInteger(number))) return number;
  }
  if (field.dataType === 'BOOLEAN' && ['true', 'false'].includes(value)) return value === 'true';
  return value;
}
const fieldTable = {
  config: { notPagination: true, table: { stripe: true, rowKey: 'modelColumnId' } },
};
const authorizationItems = [
  { label: '已授权系统数', key: 'authorizedSystemCount' },
  { label: '已停用授权数', key: 'disabledAuthorizationCount' },
  { label: '默认访问控制', key: 'accessControl' },
  { label: '最近授权时间', key: 'lastAuthorizationTime', formatter: value => value ? proxy.parseTime(value, timeFormat) : '暂无授权' },
  { label: '授权方式', key: 'authorizationMethod' },
  { label: '授权范围', key: 'authorizationScope' },
];
const authorizationTable = {
  config: { autoPagination: true, table: { stripe: true, rowKey: 'id' } },
  func: () => loadAuthorizationRecords(detail.value.id),
  columns: [
    { label: '编号', type: 'index', width: 65 },
    { label: '目标系统（授权主体）', prop: 'subjectName', minWidth: 200 },
    { label: '授权范围', prop: 'dataScopeType', slot: 'scope', minWidth: 160 },
    { label: '授权人', prop: 'createBy', minWidth: 120 },
    { label: '授权时间', prop: 'createTime', minWidth: 180, date: timeFormat },
    { label: '到期时间', prop: 'expireTime', slot: 'expireTime', minWidth: 180 },
    { label: '操作', slot: 'action', width: 100, fixed: 'right' },
  ],
};

const callItems = [
  { label: '近7天调用次数', key: 'callCount' },
  { label: '最近调用时间', key: 'lastCallTime', type: 'time' },
  { label: '成功次数', key: 'successCount' },
  { label: '最近调用系统', key: 'lastCallSystem' },
  { label: '失败次数', key: 'failureCount' },
  { label: '最近调用结果', key: 'lastCallResult' },
];
const lastCallItems = [
  { label: '请求参数摘要', key: 'requestSummary' },
  { label: '响应摘要', key: 'responseSummary' },
  { label: '错误信息', key: 'errorMessage', span: 2 },
];
const callDetailItems = [
  { label: 'API 服务名称', key: 'serviceName' },
  { label: '授权单号', key: 'authorizationNo' },
  { label: '调用系统', key: 'callerSystem' },
  { label: '调用 IP', key: 'callerIp' },
  { label: '调用接口地址', key: 'requestPath', span: 2 },
  { label: '请求方式', key: 'requestMethod' },
  { label: '响应状态码', key: 'httpStatusCode' },
  { label: '调用时间', key: 'callTime', type: 'time' },
  { label: '调用耗时(ms)', key: 'durationMs' },
  { label: '响应数据量', key: 'responseDataCount' },
  { label: '调用结果', key: 'callResult' },
  { label: '请求参数摘要', key: 'requestSummary', span: 2 },
  { label: '响应摘要', key: 'responseSummary', span: 2 },
  { label: '异常信息', key: 'errorMessage', span: 2 },
];
const callTable = {
  config: { autoPagination: true, table: { stripe: true, rowKey: 'id' } },
  func: params => listServiceCallLogs({ ...params, serviceId: detail.value.id }),
  columns: [
    { label: '编号', type: 'index', width: 65 },
    { label: '授权单号', prop: 'authorizationNo', minWidth: 180 },
    { label: '调用系统', prop: 'callerSystem', minWidth: 160 },
    { label: '调用时间', prop: 'callTime', minWidth: 180, date: timeFormat },
    { label: '响应状态码', prop: 'httpStatusCode', minWidth: 120 },
    { label: '调用结果', prop: 'callResult', minWidth: 120 },
    { label: '耗时(ms)', prop: 'durationMs', minWidth: 110 },
    { label: '操作', slot: 'action', width: 100, fixed: 'right' },
  ],
};

/**
 * 加载调用统计和最近调用摘要，响应返回时校验当前展示的服务。
 */
async function loadCallSummary(serviceId) {
  try {
    const response = await getServiceCallLogSummary(serviceId);
    if (String(serviceId) !== String(detail.value.id)) return;
    callSummary.value = response.data || {};
    lastCall.value = response.data || {};
  } catch {
    callSummary.value = {};
    lastCall.value = {};
  }
}

/**
 * 查询日志详情后展示弹窗，保留该次调用时的快照信息。
 */
async function openCallDetail(row) {
  try {
    const response = await getServiceCallLog(row.id);
    callDetail.value = response.data || {};
    callDetailVisible.value = true;
  } catch {
    proxy.$modal.msgError('调用记录详情加载失败');
  }
}

/**
 * 格式化调用快照中的时间、对象及空值。
 * @param {Object} item 详情字段定义。
 * @returns {*} 展示值。
 */
function formatCallDetailValue(item) {
  const value = callDetail.value[item.key];
  if (value === null || value === undefined || value === '') return '-';
  if (item.type === 'time') return proxy.parseTime(value, timeFormat) || '-';
  if (typeof value === 'object') return JSON.stringify(value);
  return value;
}

/**
 * 加载授权列表并更新概况，忽略已切换服务的过期响应。
 * @param {string|number} serviceId 服务 ID。
 * @returns {Promise<Object>} 通用表格需要的数据对象。
 */
async function loadAuthorizationRecords(serviceId) {
  const currentRequest = ++summaryRequestId;
  authorizationSummaryError.value = false;
  try {
    const response = await listServiceAuthorizationsByServiceId(serviceId);
    if (currentRequest !== summaryRequestId || String(serviceId) !== String(detail.value.id)) return { data: [] };
    const records = response.data;
    authorizationSummary.value = summarizeAuthorizations(records);
    return { data: records };
  } catch (error) {
    if (currentRequest === summaryRequestId && String(serviceId) === String(detail.value.id)) {
      authorizationSummaryError.value = true;
    }
    throw error;
  }
}

/**
 * 按接口状态统计授权主体，范围仅汇总当前有效授权。
 * @param {Array<Object>} records 授权记录。
 * @returns {Object} 授权概况。
 */
function summarizeAuthorizations(records) {
  // displayStatus、persistedStatus、inactiveReason 均沿用授权接口定义，避免混用服务启停状态。
  const active = records.filter(row => row.displayStatus === 'EFFECTIVE');
  const methods = [...new Set(records.map(row => row.subjectType === 'CLIENT' ? '客户端授权' : '用户授权'))];
  const scopes = [...new Set(active.map(row => row.dataScopeType === 'CONDITION' ? '条件范围' : '全部数据'))];
  const latest = records.filter(row => row.createTime).reduce((value, row) =>
    !value || new Date(row.createTime).getTime() > new Date(value).getTime() ? row.createTime : value, null);
  return {
    authorizedSystemCount: new Set(active.map(row => `${row.subjectType}:${row.subjectId}`)).size,
    disabledAuthorizationCount: records.filter(row => ['0', 'REVOKED'].includes(row.persistedStatus) || row.inactiveReason === 'INVALID').length,
    accessControl: '授权后访问',
    lastAuthorizationTime: latest,
    authorizationMethod: methods.join('、') || '暂无授权',
    authorizationScope: scopes.join('、') || '暂无有效授权',
  };
}

/** 跳转至对应的服务授权详情页。 */
function openAuthorizationDetail(row) {
  proxy.$router.push({
    path: '/authorization/serviceAuthorization/detail',
    query: { id: row.id },
  });
}
const queryTable = computed(() => ({
  ...fieldTable,
  func: () => Promise.resolve({ data: requestParameters.value }),
  columns: [
    { label: '编号', type: 'index', width: 65 },
    { label: '参数名称', prop: 'code', minWidth: 180, align: 'left' },
    { label: '参数位置', prop: 'position', minWidth: 120 },
    { label: '参数类型', prop: 'dataType', dict: 'model_column_data_type', minWidth: 140 },
    { label: '是否必填', prop: 'required', minWidth: 120 },
    { label: '示例值', prop: 'queryExampleValue', slot: 'queryExampleValue', minWidth: 180 },
    { label: '参数说明', prop: 'description', minWidth: 220, showOverflowTooltip: true },
  ],
}));
const outputTable = computed(() => ({
  ...fieldTable,
  func: () => Promise.resolve({ data: outputFields.value }),
  columns: [
    { label: '编号', type: 'index', width: 65 },
    { label: '字段名称', prop: 'outputName', minWidth: 180, align: 'left' },
    { label: '字段类型', prop: 'dataType', dict: 'model_column_data_type', minWidth: 140 },
    { label: '是否必返', prop: 'required', minWidth: 120 },
    { label: '示例值', prop: 'exampleValue', slot: 'exampleValue', minWidth: 180 },
    { label: '参数说明', prop: 'name', minWidth: 220, showOverflowTooltip: true },
  ],
}));
let requestId = 0;

/**
 * 字段名称取已发布模型；模型不可用时仍展示已保存的字段 ID 和标志。
 */
async function loadDetail() {
  const currentRequest = ++requestId;
  const id = route.query.id;
  detailError.value = false;
  if (!id || !/^\d+$/.test(String(id))) {
    detailError.value = true;
    proxy.$modal.msgWarning('缺少有效的数据服务 ID');
    return;
  }
  loading.value = true;
  fields.value = [];
  modelInfo.value = {};
  detail.value = {};
  summaryRequestId += 1;
  authorizationSummary.value = {};
  authorizationSummaryError.value = false;
  callSummary.value = {};
  lastCall.value = {};
  activeTab.value = 'query';
  try {
    const response = await getService(id);
    const service = response.data;
    const results = await Promise.allSettled([listServiceModels(), getPublishedModelStructure(service.modelId)]);
    if (currentRequest !== requestId) return;
    const models = results[0].status === 'fulfilled' ? results[0].value : [];
    modelInfo.value = models.find(model => String(model.id) === String(service.modelId)) || {};
    const columns = results[1].status === 'fulfilled' ? results[1].value.data?.fields || [] : [];
    const columnsById = new Map(columns.map(column => [String(column.id), column]));
    fields.value = (service.fields || []).map(field => {
      const column = columnsById.get(String(field.modelColumnId));
      return {
        ...field, name: column?.name || `字段 ID：${field.modelColumnId}`, code: column?.code || '-',
        outputName: field.fieldAlias?.trim() || column?.code?.toLowerCase() || '-', dataType: column?.dataType || '-',
        required: column?.requiredFlag ? '是' : '否',
        description: column?.name || '-',
      };
    });
    /*
     * 仅从已保存配置推导展示值；尚无数据来源的原型字段由通用组件展示占位符。
     */
    detail.value = {
      ...service,
      successMessage: response.msg,
      modelName: modelInfo.value.name,
      queryCondition: fields.value.filter(field => field.queryableFlag === SERVICE_FLAG.YES).map(field => field.name).join('、'),
    };
  } catch {
    if (currentRequest === requestId) detailError.value = true;
  } finally {
    if (currentRequest === requestId) loading.value = false;
  }
}

watch(activeTab, tab => {
  if (tab === 'calls' && detail.value.id) {
    loadCallSummary(detail.value.id);
    callTableRef.value?.getList();
  }
});

watch(() => route.query.id, loadDetail, { immediate: true });
</script>

<style scoped lang="scss">
.example-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.example-editor {
  min-width: 0;

  :deep(.CodeMirror-lines) {
    padding: 10px 0;
  }
}
</style>
