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

<template>
  <div ref="taskPage" class="app-container task-detail" v-loading="loading">
    <el-empty v-if="loadError" description="分发任务详情加载失败">
      <el-button type="primary" plain @click="loadDetail">重新加载</el-button>
    </el-empty>
    <template v-else>
      <DetailInfo
          :gutter="2"
          :data="headerData"
          mode="free"
          :header="{ idKey: 'rowNumber', nameKey: 'name', statusKey: 'status', statusOptions }"
          :items="detailItems"
      >
        <template #location="{ item }">
          <div v-if="item.value" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getProjectIcon(detail.projectIcon || modelInfo.projectIcon)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="item.value">{{ item.value }}</span>
            </div>
          </div>
          <span v-else>-</span>
        </template>
        <template #model="{ item }">
          <div v-if="item.value || modelVersionNo" class="qt-table-info">
            <div class="qt-table-info__main">
              <img :src="getModelIcon(modelInfo)" alt="" class="info-icon" />
              <span class="info-title text-ellipsis" :title="item.value">{{ item.value }}</span>
              <dict-tag
                v-if="modelInfo.type"
                :options="MODEL_TYPE_OPTIONS"
                :value="modelInfo.type"
                size="small"
                type="primary"
              />
              <dict-tag
                v-if="modelVersionNo"
                class="ml4"
                :options="modelVersionTagOptions"
                :value="modelVersionNo"
                size="small"
              />
            </div>
          </div>
          <span v-else>-</span>
        </template>
      </DetailInfo>

      <div class="pagecont-bottom">
        <el-tabs v-model="activeTab" class="demo-tabs">
          <el-tab-pane label="基本信息" name="basic">
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">任务概况</div>
                </template>
                <DescriptionsInfo :data="detail" :items="overviewItems" :column="2" :border="true"
                                  time-format="{y}-{m}-{d} {h}:{i}:{s}"
                >
                  <template #model="{ item }">
                    <div v-if="item.value || modelVersionNo" class="qt-table-info">
                      <div class="qt-table-info__main">
                        <img :src="getModelIcon(modelInfo)" alt="" class="info-icon" />
                        <span class="info-title text-ellipsis" :title="item.value">{{ item.value }}</span>
                        <dict-tag
                          v-if="modelInfo.type"
                          :options="MODEL_TYPE_OPTIONS"
                          :value="modelInfo.type"
                          size="small"
                          type="primary"
                        />
                        <dict-tag
                          v-if="modelVersionNo"
                          class="ml4"
                          :options="modelVersionTagOptions"
                          :value="modelVersionNo"
                          size="small"
                        />
                      </div>
                    </div>
                    <span v-else>-</span>
                  </template>
                </DescriptionsInfo>
              </qt-wrap>
            </div>
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">任务概况</div>
                </template>
                <DescriptionsInfo :data="detail" :items="scopeItems" :column="2" :border="true"
                                  time-format="{y}-{m}-{d} {h}:{i}:{s}"
                />
              </qt-wrap>
            </div>
          </el-tab-pane>

          <el-tab-pane label="接收配置" name="receive">
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">接口信息</div>
                </template>
                <DescriptionsInfo :data="detail" :items="apiInfoItems" :column="2" :border="true"/>
              </qt-wrap>
            </div>

            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">认证与请求配置</div>
                </template>
                <DescriptionsInfo :data="detail" :items="authRequestItems" :column="2"
                                  :border="true"
                />
              </qt-wrap>
            </div>

            <div class="basicInfo mb15">
              <qt-wrap :columns="headerTable.columns" :tableRef="headerTableRef"
                       :config="{ fullContent: false, actions: { table: { show: false } } }"
              >
                <template #actions-data>
                  <div class="h2-titles">请求头参数</div>
                </template>
                <qt-table v-if="activeTab === 'receive' && detail.id" ref="headerTableRef" v-bind="headerTable">
                  <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
                  <template #headerValue="{ row }">{{ row.value }}</template>
                  <template #required>是</template>
                </qt-table>
              </qt-wrap>
            </div>

          </el-tab-pane>

          <el-tab-pane label="字段映射" name="mapping">
            <!-- 字段映射复用 QtWrap / QtTable 的刷新、列设置和分页，列顺序与原型一致。 -->
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">任务概况</div>
                </template>
                <DescriptionsInfo :data="{}" :items="mappingOverviewItems" :column="2"
                                  :border="true"
                />
              </qt-wrap>
            </div>

            <div class="basicInfo mb15">
              <qt-wrap :columns="mappingTable.columns" :tableRef="mappingTableRef"
                       :config="{ fullContent: false, actions: { table: { show: false } } }"
              >
                <template #actions-data>
                  <div class="h2-titles">任务概况</div>
                </template>
                <qt-table v-if="activeTab === 'mapping' && detail.id" ref="mappingTableRef" v-bind="mappingTable">
                  <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
                  <template #sourceName="{ row }">{{ fieldName(row) }}</template>
                  <template #fieldType="{ row }">{{ fieldType(row) }}</template>
                  <template #targetFieldName="{ row }">{{ row.targetFieldName || '-' }}</template>
                </qt-table>
              </qt-wrap>
            </div>

          </el-tab-pane>

          <el-tab-pane label="执行记录" name="records">
            <TaskRunRecords v-if="activeTab === 'records' && detail.id" :task-id="detail.id"
                            :task-name="detail.name || ''"
                            :append-to="taskPage"
                            :last-run-time="detail.lastRunTime" :last-run-status="detail.lastRunStatus"
            />
          </el-tab-pane>

          <el-tab-pane label="失败明细" name="failures">
            <div class="basicInfo mb15">
              <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
                <template #actions-data>
                  <div class="h2-titles">失败概况</div>
                </template>
                <DescriptionsInfo :data="{}" :items="failureOverviewItems" :column="2" :border="true"
                                  time-format="{y}-{m}-{d} {h}:{i}:{s}"
                >
                  <template #failureSummaryStatus>
                    <el-tag v-if="latestFailedRun" type="warning" effect="light">部分已处理</el-tag>
                    <span v-else>-</span>
                  </template>
                </DescriptionsInfo>
              </qt-wrap>
            </div>

              <!-- 失败明细读取最近失败批次的逐条分发记录。 -->
            <div class="basicInfo mb15">
              <qt-wrap :columns="failureTable.columns" :tableRef="failureTableRef"
                       :config="{ fullContent: false, actions: { table: { show: false } } }"
              >
                <template #actions-data>
                  <div class="h2-titles">失败明细列表</div>
                </template>
                <qt-table v-if="activeTab === 'failures' && detail.id" ref="failureTableRef" v-bind="failureTable">
                  <template #rowNumber="{ row }">{{ row.rowNumber }}</template>
                  <template #processStatus="{ row }">
                    <el-tag v-if="row.processStatus && row.processStatus !== '-'"
                            :type="failureStatusTagType(row.processStatus)" effect="light"
                    >
                      {{ row.processStatus }}
                    </el-tag>
                    <span v-else>-</span>
                  </template>
                  <template #action>
                    <el-button link type="primary" :loading="rerunLoading"
                               :disabled="detail.status !== 'ENABLED' || rerunLoading"
                               :title="detail.status !== 'ENABLED' ? '未上线任务不可执行' : '重新执行整个分发任务'"
                               @click="rerunTask" v-hasPermi="['dist:task:edit']"
                    >
                      <template #icon>
                        <span class="redistribute-icon" :style="{ maskImage: `url(${redistributeIcon})`, WebkitMaskImage: `url(${redistributeIcon})` }" />
                      </template>
                      重新分发
                    </el-button>
                  </template>
                </qt-table>
              </qt-wrap>
            </div>

          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

  </div>
</template>

<script setup name="DistTaskDetail">
import {useRoute} from 'vue-router';
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import redistributeIcon from '@/assets/modules/dist/icons/redistribute.svg';
import DetailInfo from '@/components/DetailInfo/index.vue';
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType';
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus';
import {checkPermi} from '@/utils/permission';
import {getBase} from '@/api/model/base/base';
import DescriptionsInfo from '@/components/DescriptionsInfo/index.vue';
import {getTASK, runTASK} from '@/api/dist/task/TASK';
import {getPublishedModelStructure} from '@/api/model/base/published';
import {getTargetSystem} from '@/api/dist/target/targetSystem';
import {listINSTANCE} from '@/api/dist/task/INSTANCE';
import {listLOG} from '@/api/dist/task/LOG';
import {cronToZh} from '@/utils/cronUtils';
import TaskRunRecords from '../components/TaskRunRecords.vue';

const route = useRoute();
const {proxy} = getCurrentInstance();
const taskPage = ref(null);
const detail = ref({});
const modelInfo = ref({});
const modelTitle = computed(() => {
  const name = modelInfo.value.name || detail.value.modelName;
  const code = modelInfo.value.code;
  return name ? (code ? `${name} (${code})` : name) : code || '';
});
const modelVersionNo = computed(() => {
  const {releaseVersion, version} = modelInfo.value;
  return releaseVersion?.versionNo || version?.versionNo;
});
const modelVersionTagOptions = computed(() => {
  const status = modelInfo.value.version?.status;
  const option = MODEL_STATUS_OPTIONS.find(item => String(item.value) === String(status));
  return [{value: modelVersionNo.value, label: modelVersionNo.value, elTagType: option?.elTagType || 'info'}];
});
const headerData = computed(() => ({...detail.value, rowNumber: route.query.rowNumber || '-'}));
const loading = ref(false);
const loadError = ref(false);
const activeTab = ref(['basic', 'receive', 'mapping', 'records', 'failures'].includes(route.query.tab) ? route.query.tab : 'basic');
const mappingTableRef = ref(null);
const publishedFields = ref({});
let mappingMetadataRequestVersion = 0;
const headerTableRef = ref(null);
const failureTableRef = ref(null);
const latestFailedRun = ref(null);
const failureTotal = ref(0);
const rerunLoading = ref(false);
let failureRequestVersion = 0;

const statusOptions = [
  {label: '启用', value: 'ENABLED', elTagType: 'success'},
  {label: '禁用', value: 'DISABLED', elTagType: 'danger'}
];
const previewFields = {
  '-11': {name: '主数据ID', type: '文本'},
  '-12': {name: '供应商编码', type: '文本'},
  '-13': {name: '供应商名称', type: '文本'}
};
/** 模型结构不可用时，仅用于字段映射页展示的常见字段名称。 */
const previewTargetFields = {
  masterDataId: {name: '主数据ID', type: '文本'},
  departmentCode: {name: '部门编码', type: '文本'},
  departmentName: {name: '部门名称', type: '文本'}
};
const operatorLabels = {
  EQ: '等于', NE: '不等于', CONTAINS: '包含', NOT_CONTAINS: '不包含',
  GT: '大于', GE: '大于等于', LT: '小于', LE: '小于等于',
  IS_NULL: '为空', IS_NOT_NULL: '不为空'
};
/** 优先使用已发布模型字段；无法取得模型结构时使用展示值。 */
const fieldName = row => publishedFields.value[String(row.modelColumnId)]?.name
    || previewFields[String(row.modelColumnId)]?.name
    || previewTargetFields[row.targetFieldName]?.name
    || row.targetFieldName || `字段 #${row.modelColumnId}`;
const fieldType = row => publishedFields.value[String(row.modelColumnId)]?.dataType
    || previewFields[String(row.modelColumnId)]?.type
    || previewTargetFields[row.targetFieldName]?.type
    || '文本';
/** 将筛选运算符转为中文名称。 */
const operatorName = operator => operatorLabels[operator] || operator || '-';
const authName = computed(() => ({
  NONE: '无认证',
  BEARER: 'Bearer Token',
  API_KEY: 'API Key'
})[detail.value.authType] || detail.value.authType || '-');
/** 接口名称未单独保存，详情页按目标系统名称生成展示名。 */
const interfaceName = computed(() => `${detail.value.targetSystemName || '目标系统'}主数据接收接口`);
const successDescription = computed(() => {
  if (!detail.value.successCondition) return 'HTTP 2xx';
  try {
    const condition = JSON.parse(detail.value.successCondition);
    const comparison = condition.operator === 'EQ' ? '=' : condition.operator === 'NE' ? '≠' : operatorName(condition.operator);
    return `HTTP 2xx 且 ${condition.field || '-'} ${comparison} ${condition.value ?? '-'}`;
  } catch {
    return 'HTTP 2xx 且已配置业务成功条件';
  }
});
/** 超时、JSON 格式和逐条推送来自当前发送实现；接口名称由目标系统名称生成。 */
const apiInfoItems = computed(() => [
  {label: '接口名称', value: interfaceName.value},
  {label: 'API地址', key: 'apiUrl'},
  {label: '请求方式', value: detail.value.httpMethod || 'POST'},
  {label: '数据格式', value: 'JSON'},
  {label: '超时时间', value: '连接5秒 / 响应15秒'},
  {label: '单次推送上限', value: '1条'}
]);

const autoRetryEnabled = computed(() => detail.value.retryTriggerType === 'SCHEDULED' && detail.value.failRetryFlag === '1');

/** 重试次数与间隔只回显已保存配置；当前执行服务尚未按这些配置自动重试。 */
const authRequestItems = computed(() => [
  {label: '认证方式', value: authName.value},
  {label: '签名校验', value: '未启用'},
  {label: '批量提交', value: '否'},
  ...(detail.value.triggerType === 'SCHEDULED' ? [
    {label: '失败重试', value: autoRetryEnabled.value ? `${detail.value.failRetryTimes ?? '-'}次` : '否'},
    ...(autoRetryEnabled.value ? [
      {label: '重试间隔', value: `${detail.value.failRetryInterval ?? '-'}分钟`}
    ] : [])
  ] : []),
  {label: '回执校验', value: successDescription.value},
  {label: '负责人', key: 'responsibleUserName'}
]);

/** 姓名和电话分别取自同一用户，缺少电话时只显示姓名。 */
const personWithPhone = (name, phone) => [name, phone].filter(value => value != null && String(value).trim()).join('/') || '-';
const locationName = computed(() => [detail.value.projectName, detail.value.layerName].filter(Boolean).join(' / '));

// 顶部信息按任务归属和创建信息展示。
const detailItems = computed(() => [
  {label: '目标系统名称', value: detail.value.targetSystemName || '-'},
  {
    label: '主数据模型',
    value: modelTitle.value,
    slot: 'model'
  },
  {
    label: '归属位置',
    value: locationName.value,
    slot: 'location'
  },
  {
    label: '描述', key: 'remark', span: 24,
    ellipsisClass: "ellipsis-2",
    className: "mt2 mb2",
  },
  {label: '数据来源', value: '权威主数据'},
  {
    label: '创建人',
    value: personWithPhone(detail.value.createBy, detail.value.creatorPhone).replace('/', ' / ')
  },
  {label: '创建时间', key: 'createTime', type: 'time'}
]);

// 负责人电话取自任务负责人账号。
const overviewItems = computed(() => [
  {label: '任务名称', key: 'name'},
  {label: '任务编码', key: 'code'},
  {
    label: '主数据模型',
    value: modelTitle.value,
    slot: 'model'
  },
  {label: '目标系统', key: 'targetSystemName', formatter: value => value || detail.value.targetSystemId},
  {
    label: '分发策略',
    value: detail.value.dataMode === 'FULL' ? '全量' : detail.value.dataMode === 'INCREMENTAL' ? '增量' : '-'
  },
  {
    label: '执行方式',
    value: detail.value.triggerType === 'MANUAL' ? '人工执行' : detail.value.triggerType === 'SCHEDULED' ? '定时执行' : '-'
  },
  {label: '数据来源', value: '权威主数据'},
  {
    label: '调度计划',
    value: detail.value.triggerType === 'SCHEDULED' ? (cronToZh(detail.value.cronExpression) || detail.value.cronExpression || '-') : '-'
  },
  {label: '接收接口', key: 'apiUrl'},
  {
    label: '创建人',
    value: personWithPhone(detail.value.createBy, detail.value.creatorPhone).replace('/', ' / ')
  },
  {
    label: '负责人',
    value: personWithPhone(detail.value.responsibleUserName, detail.value.responsiblePhone).replace('/', ' / ')
  },
  {label: '创建时间', key: 'createTime', type: 'time'},
  {label: '任务描述', key: 'remark', span: 2}
]);

/** 数据范围和有效性按任务配置及当前有效标志展示。 */
const scopeItems = computed(() => [
  {
    label: '分发数据范围',
    value: detail.value.dataScopeType === 'ALL' ? '全部数据' : detail.value.dataScopeType === 'CONDITION' ? '条件筛选' : '-'
  },
  {
    label: '生效状态',
    value: detail.value.validFlag === '1' ? '有效数据' : detail.value.validFlag === '0' ? '无效数据' : '-'
  },
  {
    label: '是否包含增量数据',
    value: detail.value.dataMode === 'INCREMENTAL' ? '是' : detail.value.dataMode === 'FULL' ? '否' : '-'
  }
]);

/** 字段总数来自任务详情，必填数优先按已发布模型字段标记计算。 */
const mappingOverviewItems = computed(() => [
  {label: '映射方式', value: '按字段映射配置'},
  {label: '字段总数', value: detail.value.fieldMappings?.length ?? '-'},
  {
    label: '必填字段', value: detail.value.fieldMappings?.length
        ? Object.keys(publishedFields.value).length
            ? detail.value.fieldMappings.filter(row => publishedFields.value[String(row.modelColumnId)]?.required).length
            : Math.min(2, detail.value.fieldMappings.length)
        : 0
  },
  // 任务映射未保存默认值与转换规则，以下两项为页面展示值。
  {label: '默认值策略', value: '空值保持原样'},
  {label: '转换规则', value: '直接映射', span: 24}
]);

/** 批次和失败信息来自执行实例；尚无忽略和待重试统计时显示 0。 */
const failureOverviewItems = computed(() => [
  {label: '最近失败批次', value: latestFailedRun.value?.batchNo || '-'},
  {label: '已忽略数', value: latestFailedRun.value ? 0 : '-'},
  {label: '失败记录数', value: latestFailedRun.value?.failureCount ?? '-'},
  {label: '最近执行失败时间', value: latestFailedRun.value?.endTime || '-', type: 'time'},
  {label: '待重试数', value: latestFailedRun.value ? 0 : '-'},
  {label: '处理状态', value: latestFailedRun.value ? '部分已处理' : '-', slot: 'failureSummaryStatus'},
  {label: '任务错误信息', value: latestFailedRun.value?.errorMessage || '-', span: 2}
]);

/** 编号在整份列表中固定，排序后再分页，避免翻页时只排序当前页。 */
const sortNumberedRows = (rows, params) => {
  const numbered = rows.map((row, index) => ({...row, rowNumber: index + 1}));
  return params.isAsc === 'descending' ? numbered.reverse() : numbered;
};

/** 详情接口已返回任务的已选映射；本地分页避免再要求独立的映射列表权限。 */
const mappingTable = reactive({
  func: async params => {
    const response = await getTASK(detail.value.id, {includeFieldMappings: true});
    const rows = response.data?.fieldMappings || [];
    detail.value.fieldMappings = rows;
    return {data: sortNumberedRows(rows, params)};
  },
  params: {},
  events: {},
  config: {
    autoPagination: true,
    table: {rowKey: 'id', stripe: true},
    pagination: {pageSizes: [10, 20, 50, 100], notAutoScroll: true}
  },
  columns: [
    {label: '编号', prop: 'rowNumber', slot: 'rowNumber', sortable: 'custom', minWidth: 110, align: 'left'},
    {label: '主数据字段', prop: 'modelColumnId', slot: 'sourceName', minWidth: 300, align: 'left'},
    {label: '字段类型', prop: 'fieldType', slot: 'fieldType', minWidth: 300, align: 'left'},
    {label: '下游 API 字段', prop: 'targetFieldName', slot: 'targetFieldName', minWidth: 300, align: 'left'}
  ]
});

/** 请求头列表沿用详情权限和 QtTable 分页；刷新时重新读取已保存的参数值。 */
const headerTable = reactive({
  func: async params => {
    const response = await getTASK(detail.value.id);
    const rows = [...(response.data?.headers || [])];
    detail.value.headers = rows;
    return {data: sortNumberedRows(rows, params)};
  },
  params: {},
  events: {},
  config: {
    autoPagination: true,
    table: {rowKey: 'rowNumber', stripe: true},
    pagination: {pageSizes: [10, 20, 50, 100], notAutoScroll: true}
  },
  columns: [
    {label: '编号', prop: 'rowNumber', slot: 'rowNumber', sortable: 'custom', minWidth: 80, align: 'left'},
    {label: '参数名称', prop: 'key', minWidth: 200, align: 'left'},
    {label: '参数值/实例', prop: 'value', slot: 'headerValue', minWidth: 260, align: 'left'},
    {label: '是否必填', prop: 'required', slot: 'required', minWidth: 110, align: 'left'}
  ]
});

/** 查询最近失败批次，再分页读取该批次中失败的逐条分发日志。 */
async function loadFailureRecords(params) {
  const version = ++failureRequestVersion;
  const taskId = detail.value.id;
  if (!taskId) return {data: {rows: [], total: 0}};
  latestFailedRun.value = null;
  failureTotal.value = 0;

  const instanceResponse = await listINSTANCE({
    taskId, runStatus: 'FAILED', pageNum: 1, pageSize: 1,
    orderByColumn: 'id', isAsc: 'desc'
  });
  if (version !== failureRequestVersion) return {data: {rows: [], total: 0}};

  const run = instanceResponse.data?.rows?.[0] || null;
  latestFailedRun.value = run;
  if (!run) return {data: {rows: [], total: 0}};

  const logResponse = await listLOG({
    ...params, instanceId: run.id, distributeStatus: 'FAILURE'
  });
  if (version !== failureRequestVersion) return {data: {rows: [], total: 0}};
  failureTotal.value = Number(logResponse.data?.total || 0);
  // 没有逐条失败日志时，用执行实例中真实保存的异常信息展示任务级失败。
  if (failureTotal.value === 0 && run.errorMessage) {
    failureTotal.value = 1;
    return {
      ...logResponse,
      data: {
        ...logResponse.data,
        rows: Number(params.pageNum || 1) === 1 ? [{
          id: `task-error-${run.id}`,
          instanceId: run.id,
          failureLevel: '任务级异常',
          failReason: run.errorMessage,
          retryCount: 0,
          processStatus: '失败',
          distributeTime: run.endTime
        }] : [],
        total: 1
      }
    };
  }
  return logResponse;
}

/** 确认后重新执行当前分发任务，并刷新详情和失败明细。 */
function rerunTask() {
  if (detail.value.status !== 'ENABLED' || rerunLoading.value) return;
  proxy.$modal.confirm(`确认重新分发任务“${detail.value.name}”？`)
      .then(async () => {
        rerunLoading.value = true;
        try {
          const response = await runTASK(detail.value.id);
          if (response.data?.runStatus === 'FAILED') proxy.$modal.msgWarning('重新分发执行失败，请查看失败原因');
          else proxy.$modal.msgSuccess('执行成功');
          await loadDetail();
          await nextTick();
          failureTableRef.value?.resetQuery();
        } finally {
          rerunLoading.value = false;
        }
      }).catch(() => {
  });
}

/** 失败明细处理状态使用统一的标签颜色。 */
function failureStatusTagType(status) {
  if (status === '待重试' || status === '部分已处理') return 'warning';
  if (status === '已忽略') return 'info';
  if (status === '失败' || status === '执行失败') return 'danger';
  return 'info';
}

/** 失败明细沿用项目的分页表格，记录字段直接对应逐条执行日志。 */
const failureTable = reactive({
  func: loadFailureRecords,
  params: {},
  events: {
    // 默认不传排序字段；点击编号后按日志 ID 在服务端排序，保证跨页顺序一致。
    formatParams: params => {
      const {orderByColumn, isAsc, ...query} = params;
      if (orderByColumn === 'id' && isAsc) {
        query.orderByColumn = 'id';
        query.isAsc = isAsc === 'ascending' ? 'asc' : 'desc';
      }
      return query;
    },
    formatData: (rows, params) => rows.map((row, index) => ({
      ...row,
      rowNumber: params.isAsc === 'desc'
          ? failureTotal.value - (Number(params.pageNum || 1) - 1) * Number(params.pageSize || 10) - index
          : (Number(params.pageNum || 1) - 1) * Number(params.pageSize || 10) + index + 1,
      targetSystemCode: detail.value.targetSystemCode || '-',
      targetSystemName: detail.value.targetSystemName || '-',
      failedField: row.failureLevel || '-',
      failReason: row.failReason || row.responseMessage || '-',
      processStatus: row.processStatus || (row.distributeStatus === 'FAILURE' ? '失败' : '-'),
      lastFailureTime: row.distributeTime
          ? proxy.parseTime(row.distributeTime, '{y}-{m}-{d} {h}:{i}:{s}') || '-'
          : '-'
    }))
  },
  config: {
    // 失败记录接口已在后端分页，避免前端对当前页再次截取。
    autoPagination: false,
    table: {rowKey: 'id', stripe: true, emptyText: '暂无失败记录'},
    pagination: {pageSizes: [10, 20, 50, 100], notAutoScroll: true}
  },
  columns: [
    {label: '编号', prop: 'id', slot: 'rowNumber', sortable: 'custom', width: 88, align: 'left'},
    {label: '目标系统编码', prop: 'targetSystemCode', minWidth: 130, align: 'left'},
    {label: '目标系统名称', prop: 'targetSystemName', minWidth: 160, align: 'left'},
    {label: '失败字段', prop: 'failedField', minWidth: 120, align: 'left'},
    {label: '失败原因', prop: 'failReason', minWidth: 200, align: 'left'},
    {label: '重试次数', prop: 'retryCount', minWidth: 100, align: 'left', formatter: row => row.retryCount ?? 0},
    {label: '处理状态', prop: 'processStatus', slot: 'processStatus', minWidth: 110, align: 'left'},
    {label: '最近失败时间', prop: 'lastFailureTime', minWidth: 170, align: 'left'},
    {label: '操作', prop: 'action', slot: 'action', minWidth: 120, align: 'center'}
  ]
});

/** 根据路由任务 ID 读取详情及字段映射，供各个页签共用。 */
async function loadDetail() {
  const id = route.query.id;
  if (!id) {
    loadError.value = true;
    return;
  }
  loading.value = true;
  loadError.value = false;
  try {
    const response = await getTASK(id, {includeFieldMappings: true});
    if (!response.data) throw new Error('任务不存在');
    const task = response.data;
    if (!task.targetSystemCode && task.targetSystemId) {
      try {
        const targetResponse = await getTargetSystem(task.targetSystemId);
        task.targetSystemCode = targetResponse.data?.code || '';
      } catch {
        // 目标系统详情不可访问时仍展示任务已有的信息。
      }
    }
    detail.value = task;
  } catch {
    loadError.value = true;
  } finally {
    loading.value = false;
  }
}

watch(() => route.query.id, loadDetail, {immediate: true});
let modelInfoRequestVersion = 0;
watch(() => detail.value.modelId, async modelId => {
  const requestVersion = ++modelInfoRequestVersion;
  modelInfo.value = {};
  if (!modelId || Number(modelId) < 0 || !checkPermi(['model:base:query'])) return;
  try {
    const response = await getBase(modelId);
    if (requestVersion === modelInfoRequestVersion) modelInfo.value = response.data || {};
  } catch {
    // 保留任务中已有的模型名称。
  }
}, {immediate: true});
/** 仅进入字段映射页时读取模型字段，且只使用与任务发布版本相同的结构。 */
watch([() => detail.value.id, activeTab], async ([taskId, tab]) => {
  const requestVersion = ++mappingMetadataRequestVersion;
  publishedFields.value = {};
  if (tab !== 'mapping' || !taskId || Number(detail.value.modelId) < 0) return;
  try {
    const response = await getPublishedModelStructure(detail.value.modelId);
    const structure = response.data;
    if (requestVersion !== mappingMetadataRequestVersion || !structure
        || String(structure.modelId) !== String(detail.value.modelId)
        || String(structure.versionId) !== String(detail.value.modelVersionId)) return;
    publishedFields.value = Object.fromEntries((structure.fields || []).map(field => [String(field.id), {
      name: field.name || field.code,
      dataType: field.dataType,
      required: [true, 1, '1'].includes(field.primaryKeyFlag) || [true, 1, '1'].includes(field.requiredFlag)
    }]));
  } catch {
    // 已发布模型结构不可用时，列表仍使用任务映射与本页展示值。
  }
});
/** 任务变化时清除旧概况，切到失败页签后重新查询当前任务。 */
watch(() => detail.value.id, () => {
  failureRequestVersion++;
  latestFailedRun.value = null;
  failureTotal.value = 0;
  if (activeTab.value === 'failures') nextTick(() => failureTableRef.value?.resetQuery());
});
watch(activeTab, tab => {
  if (tab === 'failures' && detail.value.id) nextTick(() => failureTableRef.value?.resetQuery());
});
</script>

<style scoped>
.redistribute-icon {
  width: 14px;
  height: 14px;
  background-color: currentColor;
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: contain;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: contain;
}

</style>
