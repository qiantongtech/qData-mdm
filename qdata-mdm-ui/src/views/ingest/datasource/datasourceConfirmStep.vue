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
  <div class="datasource-confirm-step">
    <DatasourceSummary
        :icon-url="getIconUrl"
        :datasource-type="form.datasourceType"
        :items="summaryItems"
    />

    <DiagnosisResultHeader
        class="overview"
        :overall-title="overallTitle"
        :overall-status="headerOverallStatus"
        :counts="diagnosisCountsWithComma"
    />

    <section class="table-wrap">
      <el-table
          :data="diagnosticRows"
          :span-method="spanMethod"
      >
        <template #empty>
          <div
              v-if="isRequesting"
              v-loading="true"
              element-loading-text="正在连接并获取诊断项..."
              class="empty-state"
          ></div>
          <div v-else class="empty-state">
            <div class="emptyBg">
              <img :src="emptyDataImage" alt=""/>
              <p>暂无诊断项</p>
            </div>
          </div>
        </template>
        <el-table-column label="诊断详情" width="250" class-name="diagnosis-group-col">
          <template #default="{ row }">
            <div class="group-cell flex-col" :class="statusMeta(row.group.status).className">
              <div class="group-header flex-row justify-between">
                <div class="icon-wrap flex-col has-svg">
                  <img :src="groupIcon(row.group.groupCode)"/>
                </div>
                <div class="flex-col">
                  <dict-tag
                      v-if="row.group.status"
                      :options="getDiagnosisDictOptions"
                      :show-icon="false"
                      :value="normalizeDiagnosisStatus(row.group.status)"
                      :label="groupBadge(row.group)"
                  />
                  <el-tag
                      v-else-if="isStreaming"
                      type="info"
                      disable-transitions
                      class="streaming-tag"
                  >
                    <div style="display: flex; align-items: center;">
                      <el-icon class="is-loading" style="margin-right: 2px">
                        <Loading/>
                      </el-icon>
                      <span>{{ getGroupProgress(row.group) }}</span>
                    </div>
                  </el-tag>
                  <span v-else>-</span>
                </div>
              </div>
              <div class="group-main flex-col justify-between">
                <span class="title">{{ row.group.groupName || "-" }}</span>
                <span class="desc" :title="row.group.description">{{ row.group.description || "-" }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="诊断项" width="168">
          <template #default="{ row }">
            {{ row.item.checkName || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="校验内容" width="256">
          <template #default="{ row }">
            {{ row.item.checkContent || "-" }}
          </template>
        </el-table-column>
        <el-table-column label="结果状态" width="120">
          <template #default="{ row }">
            <dict-tag
                v-if="row.item.status"
                :options="getDiagnosisDictOptions"
                :value="row.item.status"
            />
            <el-tag
                v-else-if="isStreaming"
                type="info"
                disable-transitions
                class="streaming-tag"
            >
              <div style="display: flex; align-items: center;">
                <el-icon class="is-loading" style="margin-right: 2px">
                  <Loading/>
                </el-icon>
                <span>诊断中</span>
              </div>
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="功能影响" min-width="200">
          <template #default="{ row }">
            <div class="impact">{{ row.item.impact || "-" }}</div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import emptyDataImage from '@/assets/common/images/empty-state/empty-nodata.png';
import DatasourceSummary from "./components/datasourceSummary.vue";
import DiagnosisResultHeader from "./components/diagnosisResultHeader.vue";
import {computed, onBeforeUnmount, ref} from "vue";
import {
  CircleCheckFilled,
  CircleCloseFilled,
  WarningFilled,
  Loading,
} from "@element-plus/icons-vue";
import {
  getDatasourceIcon,
} from "@/components/Datasource/config";
import {getTypeConfig} from "@/components/Datasource/typeFilter";
import {diagnoseDatasource} from "@/api/ingest/datasource/dataSource";
import { getAssetsFile } from '@/utils/anivia';
import baseConnectionIcon from "@/assets/modules/ingest/icons/datasource/baseConnection.svg";
import targetResourceIcon from "@/assets/modules/ingest/icons/datasource/targetResource.svg";
import metadataIcon from "@/assets/modules/ingest/icons/datasource/metadata.svg";
import readIcon from "@/assets/modules/ingest/icons/datasource/read.svg";
import writeIcon from "@/assets/modules/ingest/icons/datasource/write.svg";
import advancedIcon from "@/assets/modules/ingest/icons/datasource/advanced.svg";
import successIcon from "@/assets/modules/ingest/icons/datasource/success.svg";
import warningIcon from "@/assets/modules/ingest/icons/datasource/warning.svg";
import errorIcon from "@/assets/modules/ingest/icons/datasource/error.svg";

const svgIcons = {
  baseConnection: baseConnectionIcon,
  targetResource: targetResourceIcon,
  metadata: metadataIcon,
  read: readIcon,
  write: writeIcon,
  advanced: advancedIcon,
  success: successIcon,
  warning: warningIcon,
  error: errorIcon,
};
const themedSummaryIcons = new Set(['datasource', 'type', 'IP', 'port', 'account']);
const getSvgIcon = (name) => {
  if (themedSummaryIcons.has(name)) {
    return getAssetsFile(`modules/ingest/icons/datasource-summary/${name}.svg`);
  }
  return svgIcons[name];
};

// 诊断状态配置，用于控制图标和颜色
const STATUS_META = {
  PASS: {label: '通过', className: 'pass', type: 'success', iconName: 'success'},
  WARN: {label: '警告', className: 'warn', type: 'warning', iconName: 'warning'},
  FAIL: {label: '错误', className: 'fail', type: 'danger', iconName: 'error'},
  SKIPPED: {label: '跳过', className: 'skipped', type: 'info', iconName: 'warning'},
};

// 带有图标的诊断状态 options
const getDiagnosisDictOptions = Object.entries(STATUS_META).map(([value, meta]) => ({
  label: meta.label,
  value,
  elTagType: meta.type,
  elTagClass: meta.className,
  isSpan: true,
  icon: getSvgIcon(meta.iconName)
}));
// 校验发起诊断请求时的参数
// 确保 datasourceId (已有连接) 和 connection (新建表单) 互斥
function validateDiagnosisParams(params) {
  const values = params && typeof params === 'object' ? params : {};
  const hasDatasourceId = String(values.datasourceId ?? '').trim() !== '';
  const hasConnection = Boolean(
      values.connection &&
      typeof values.connection === 'object' &&
      !Array.isArray(values.connection),
  );

  if (hasDatasourceId === hasConnection) {
    return {
      valid: false,
      mode: null,
      message: '数据源 ID 与连接参数必须且只能提供一个',
    };
  }

  return {
    valid: true,
    mode: hasDatasourceId ? 'saved' : 'form',
    message: '',
  };
}

// 格式化诊断状态，处理未知状态为跳过
function normalizeDiagnosisStatus(status) {
  const normalized = String(status ?? '').trim().toUpperCase();
  return Object.prototype.hasOwnProperty.call(STATUS_META, normalized) ? normalized : 'SKIPPED';
}

// 生成请求失败时的兜底诊断结果对象
function createRequestErrorResult(message, source = 'request-error') {
  const text = (message || '') || '诊断请求失败';
  return {
    diagnosisId: '',
    overallStatus: 'FAIL',
    success: false,
    savable: false,
    primaryErrorCode: '',
    message: text,
    rawError: '',
    durationMs: 0,
    checkedAt: '',
    normalCount: 0,
    warningCount: 0,
    errorCount: 1,
    skippedCount: 0,
    totalCount: 1,
    items: [],
    groups: [],
    impactTips: [],
    diagnosisLogs: [],
    source: (source || ''),
  };
}

// 处理接口返回的诊断数据，补充必要的默认值
function normalizeDiagnosisResult(data, defaultMsg = '诊断完成') {
  if (!data || typeof data !== 'object') {
    return createDynamicResult(true, defaultMsg);
  }

  // 兼容直接返回结构或嵌套在 data 中的结构
  const actualData = data.overallStatus ? data : (data.data && typeof data.data === 'object' && data.data.overallStatus ? data.data : null);

  if (!actualData) {
    // 如果没有标准的诊断结构，则使用通用结构展示 API 返回的 msg
    const msg = data.msg || data.message || defaultMsg;
    const success = data.code === 200 || data.success === true || data.data === true;
    return createDynamicResult(success, msg);
  }

  const overallStatus = normalizeDiagnosisStatus(actualData.overallStatus);
  if (!['PASS', 'WARN', 'FAIL'].includes(overallStatus)) {
    return createDynamicResult(true, actualData.msg || actualData.message || defaultMsg);
  }

  return {
    ...actualData,
    overallStatus,
    success: typeof actualData.success === 'boolean' ? actualData.success : overallStatus !== 'FAIL',
    message: actualData.message || actualData.msg || defaultMsg,
    source: 'diagnosis',
  };
}

// 根据简单响应动态生成一个结果，不写死具体的业务字段
function createDynamicResult(success, message) {
  const status = success ? 'PASS' : 'FAIL';
  const text = message || (success ? '操作成功' : '操作失败');

  const item = {
    groupCode: 'RESULT',
    groupName: '执行结果',
    checkCode: 'API_RESULT',
    checkName: '返回信息',
    checkContent: text,
    status,
    message: text,
    impact: '',
    suggestions: [],
    rawError: '',
    durationMs: 0,
  };

  return {
    diagnosisId: '',
    overallStatus: status,
    success: success,
    savable: true,
    primaryErrorCode: '',
    message: text,
    rawError: '',
    durationMs: 0,
    checkedAt: '',
    normalCount: success ? 1 : 0,
    warningCount: 0,
    errorCount: success ? 0 : 1,
    skippedCount: 0,
    totalCount: 1,
    items: [item],
    groups: [
      {
        groupCode: item.groupCode,
        groupName: item.groupName,
        description: '接口返回信息',
        status,
        normalCount: success ? 1 : 0,
        warningCount: 0,
        errorCount: success ? 0 : 1,
        skippedCount: 0,
        totalCount: 1,
        items: [item],
      },
    ],
    impactTips: [],
    diagnosisLogs: [],
    source: 'dynamic',
  };
}

// 将嵌套的分组结构展平，以便在 Element Plus 的表格中进行单元格合并展示
function flattenDiagnosisGroups(groups) {
  if (!Array.isArray(groups)) return [];

  return groups.flatMap((group, groupIndex) => {
    const items = (group.items && group.items.length)
        ? group.items
        : [
          {
            groupCode: group.groupCode,
            groupName: group.groupName,
            checkCode: 'EMPTY_GROUP',
            checkName: '暂无检查项',
            status: '',
            message: '当前分组无诊断项',
          },
        ];

    return items.map((item, itemIndex) => ({
      rowKey: `${groupIndex}:${itemIndex}:${group.groupCode}:${item.checkCode}`,
      group,
      item,
      groupRowspan: itemIndex === 0 ? items.length : 0,
    }));
  });
}

// 请求并发控制，避免旧请求的结果覆盖新请求
function createLatestRequestGuard() {
  let generation = 0;

  return {
    next() {
      generation += 1;
      return generation;
    },
    isCurrent(token) {
      return token === generation;
    },
    invalidate() {
      generation += 1;
    },
  };
}

const props = defineProps({
  form: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits(["diagnosis-change", "loading-change"]);
const loading = ref(false);
const isRequesting = ref(false);
const isStreaming = ref(false);
const diagnosisResult = ref(null);
const requestGuard = createLatestRequestGuard();

// 获取数据源类型的对应图标
const getIconUrl = computed(() => getDatasourceIcon(props.form.datasourceType));

const datasourceTypeConfig = computed(() => getTypeConfig(props.form.datasourceType));
const datasourceTypeLabel = computed(
    () => datasourceTypeConfig.value?.label || props.form.datasourceType || "-"
);

// 获取用于展示的账号名称（兼容不同类型的字段名）
const accountName = computed(
    () =>
        props.form.username ||
        props.form.accessId ||
        props.form.access_id ||
        props.form.accessKeyId ||
        ""
);

const summaryItems = computed(() => {
  const items = [
    {key: "name", label: "数据连接名称", iconUrl: getSvgIcon('datasource'), value: props.form.datasourceName},
    {key: "type", label: "类型", iconUrl: getSvgIcon('type'), value: datasourceTypeLabel.value},
    {key: "ip", label: "IP", iconUrl: getSvgIcon('IP'), value: props.form.ip},
    {key: "port", label: "端口号", iconUrl: getSvgIcon('port'), value: props.form.port},
    {key: "account", label: "账号", iconUrl: getSvgIcon('account'), value: accountName.value},
  ];
  return items;
});

// 展平后的表格数据源
const diagnosticRows = computed(() =>
    diagnosisResult.value
        ? flattenDiagnosisGroups(diagnosisResult.value.groups)
        : []
);

// 整体诊断结果的标题描述
const overallTitle = computed(() => {
  if (isRequesting.value || isStreaming.value) return "正在诊断...";
  if (!diagnosisResult.value?.overallStatus) return "等待诊断";
  const titles = {PASS: "全部通过", WARN: "部分通过", FAIL: "未通过"};
  return titles[diagnosisResult.value?.overallStatus] || "未通过";
});

// 传递给头部组件的整体状态
const headerOverallStatus = computed(() => {
  if (isRequesting.value || isStreaming.value) return 'DIAGNOSING';
  return diagnosisResult.value?.overallStatus || '';
});

// 计算状态标签统计数据
const diagnosisCounts = computed(() => {
  const res = diagnosisResult.value || {
    normalCount: 0,
    warningCount: 0,
    errorCount: 0,
    skippedCount: 0
  };
  return [
    {
      type: 'PASS',
      label: '通过',
      icon: CircleCheckFilled,
      count: Math.max(0, Number(res.normalCount) || 0),
      show: true
    },
    {type: 'WARN', label: '警告', icon: WarningFilled, count: Math.max(0, Number(res.warningCount) || 0), show: true},
    {type: 'FAIL', label: '错误', icon: CircleCloseFilled, count: Math.max(0, Number(res.errorCount) || 0), show: true},
    {
      type: 'SKIPPED',
      label: '跳过',
      icon: WarningFilled,
      count: Math.max(0, Number(res.skippedCount) || 0),
      show: Math.max(0, Number(res.skippedCount) || 0) > 0
    },
  ].filter(item => item.show);
});

const diagnosisCountsWithComma = computed(() => {
  const counts = diagnosisCounts.value;
  return counts.map((item, index) => ({
    ...item,
    isLast: index === counts.length - 1
  }));
});

// 获取对应状态的颜色类名
function statusMeta(status) {
  if (!status) return {className: ''};
  return STATUS_META[status] || STATUS_META.SKIPPED;
}

// 根据诊断项的分组编码分配不同的图标
function groupIcon(groupCode) {
  const code = String(groupCode || "").toUpperCase();
  if (code.includes("BASE") || code.includes("CONNECTION")) return getSvgIcon('baseConnection');
  if (code.includes("TARGET") || code.includes("RESOURCE")) return getSvgIcon('targetResource');
  if (code.includes("META")) return getSvgIcon('metadata');
  if (code.includes("READ")) return getSvgIcon('read');
  if (code.includes("WRITE")) return getSvgIcon('write');
  return getSvgIcon('advanced');
}

// 格式化分组的统计徽标信息
function groupBadge(group) {
  if (!group.status) return '';
  const status = normalizeDiagnosisStatus(group.status);
  const countFields = {
    PASS: "normalCount",
    WARN: "warningCount",
    FAIL: "errorCount",
    SKIPPED: "skippedCount",
  };
  const current = Math.max(0, Number(group[countFields[status]]) || 0);
  const total = Math.max(0, Number(group.totalCount) || 0);
  return `${current}/${total} ${statusMeta(status).label}`;
}

// 格式化诊断中的统计徽标信息
function getGroupProgress(group) {
  const current = (group.normalCount || 0) + (group.warningCount || 0) + (group.errorCount || 0) + (group.skippedCount || 0);
  const total = group.totalCount || 0;
  return `${current}/${total} 诊断中`;
}

// 表格合并单元格逻辑，实现左侧分组列的合并
function spanMethod({row, columnIndex}) {
  if (columnIndex !== 0) return [1, 1];
  return row.groupRowspan ? [row.groupRowspan, 1] : [0, 0];
}

// 更新诊断结果并通知父组件
function applyResult(result, requestId) {
  if (!requestGuard.isCurrent(requestId)) return false;
  diagnosisResult.value = result;
  // eslint-disable-next-line vue/custom-event-name-casing
  emit("diagnosis-change", result);
  return true;
}

// 更新加载状态并通知父组件
function emitLoading(value, requestId) {
  if (!requestGuard.isCurrent(requestId)) return false;
  loading.value = value;
  // eslint-disable-next-line vue/custom-event-name-casing
  emit("loading-change", value);
  return true;
}

// 从错误对象中提取有用的错误信息文本
function getErrorMessage(error) {
  if (typeof error === "string") return error.trim();
  if (!error || typeof error !== "object") return "";

  try {
    const responseData = error.response?.data;
    const data = error.data;
    const messages = [
      error.message,
      error.msg,
      responseData?.message,
      responseData?.msg,
      data?.message,
      data?.msg,
    ];
    const message = messages.find(
        (candidate) => typeof candidate === "string" && candidate.trim()
    );
    return typeof message === "string" ? message.trim() : "";
  } catch {
    return "";
  }
}

// 发起诊断的主方法（对外暴露）
async function diagnose(params) {
  const requestId = requestGuard.next();
  const validation = validateDiagnosisParams(params);
  const datasourceType = String(props.form.datasourceType ?? "").trim();

  // 参数或类型校验不通过
  if (!validation.valid || !datasourceType) {
    const message = validation.valid ? "请选择数据源类型" : validation.message;
    const result = createRequestErrorResult(message, "local-error");
    applyResult(result, requestId);
    emitLoading(false, requestId);
    return result;
  }

  applyResult(null, requestId);

  emitLoading(true, requestId);
  isRequesting.value = true;

  let fullResult = null;
  try {
    const response = await diagnoseDatasource(params);
    if (!requestGuard.isCurrent(requestId)) return null;
    fullResult = normalizeDiagnosisResult(response, response?.msg || "连接测试成功");
  } catch (error) {
    if (!requestGuard.isCurrent(requestId)) return null;
    let errData = error.data || error.response?.data?.data || error.response?.data || error;
    fullResult = normalizeDiagnosisResult(errData, getErrorMessage(error) || "诊断请求失败");
  } finally {
    if (requestGuard.isCurrent(requestId)) {
      isRequesting.value = false;
    }
  }

  if (fullResult && requestGuard.isCurrent(requestId)) {
    await streamDiagnosisResult(fullResult, requestId);
  }

  emitLoading(false, requestId);
  return fullResult;
}

async function streamDiagnosisResult(fullResult, requestId) {
  isStreaming.value = true;

  // 提前准备好所有的组和项，全部展示出来，但状态为空
  const currentResult = {
    ...fullResult,
    groups: (fullResult.groups || []).map(g => ({
      ...g,
      normalCount: 0,
      warningCount: 0,
      errorCount: 0,
      skippedCount: 0,
      status: '', // 初始不展示分组状态
      items: (g.items || []).map(item => ({
        ...item,
        status: '' // 初始不展示项状态
      }))
    })),
    normalCount: 0,
    warningCount: 0,
    errorCount: 0,
    skippedCount: 0,
    overallStatus: '', // 初始不展示整体状态
  };

  applyResult(JSON.parse(JSON.stringify(currentResult)), requestId);

  for (let gIndex = 0; gIndex < (fullResult.groups || []).length; gIndex++) {
    const sourceGroup = fullResult.groups[gIndex];
    const targetGroup = currentResult.groups[gIndex];

    if (!requestGuard.isCurrent(requestId)) return;

    for (let iIndex = 0; iIndex < (sourceGroup.items || []).length; iIndex++) {
      const sourceItem = sourceGroup.items[iIndex];
      const targetItem = targetGroup.items[iIndex];

      if (!requestGuard.isCurrent(requestId)) return;

      await new Promise(resolve => setTimeout(resolve, 300));
      if (!requestGuard.isCurrent(requestId)) return;

      targetItem.status = sourceItem.status;

      const itemStatus = normalizeDiagnosisStatus(sourceItem.status);
      if (itemStatus === 'PASS') {
        targetGroup.normalCount++;
        currentResult.normalCount++;
      } else if (itemStatus === 'WARN') {
        targetGroup.warningCount++;
        currentResult.warningCount++;
      } else if (itemStatus === 'FAIL') {
        targetGroup.errorCount++;
        currentResult.errorCount++;
      } else {
        targetGroup.skippedCount++;
        currentResult.skippedCount++;
      }

      applyResult(JSON.parse(JSON.stringify(currentResult)), requestId);
    }

    targetGroup.status = sourceGroup.status;
    applyResult(JSON.parse(JSON.stringify(currentResult)), requestId);
  }

  if (!requestGuard.isCurrent(requestId)) return;
  currentResult.overallStatus = fullResult.overallStatus;
  applyResult(JSON.parse(JSON.stringify(currentResult)), requestId);
  isStreaming.value = false;
}

// 重置诊断状态（对外暴露）
function reset() {
  requestGuard.invalidate();
  diagnosisResult.value = null;
  loading.value = false;
  isRequesting.value = false;
  isStreaming.value = false;
  // eslint-disable-next-line vue/custom-event-name-casing
  emit("diagnosis-change", null);
  // eslint-disable-next-line vue/custom-event-name-casing
  emit("loading-change", false);
}

defineExpose({diagnose, reset});

onBeforeUnmount(() => {
  requestGuard.invalidate();
});
</script>

<style lang="scss" scoped>
$status-colors: (
    "pass": (
        color: var(--el-color-success),
        bg: var(--el-color-success-light-9),
    ),
    "warn": (
        color: var(--el-color-warning),
        bg: var(--el-color-warning-light-9),
    ),
    "fail": (
        color: var(--el-color-danger),
        bg: var(--el-color-danger-light-9),
    ),
    "skipped": (
        color: var(--el-color-info),
        bg: var(--el-color-info-light-9),
    ),
);

.datasource-confirm-step {
  padding: 0 13px;
  font-size: 13px;
  color: #303133;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  margin: 16px 0;
  border-radius: 4px;
  color: var(--el-text-color-secondary);

  .emptyBg {
    text-align: center;

    img {
      width: 120px;
      margin-bottom: 10px;
    }

    p {
      color: #909399;
      font-size: 14px;
      margin: 0;
    }
  }
}

.overview {
  margin: 16px 0;
}

.result-title {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
  margin-left: 5px;
}

.counts {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.tag {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  padding: 2px 6px;
  font-size: 12px;
  border-radius: 3px;
}

.status-text {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  font-size: 13px;
}

@each $status, $config in $status-colors {
  $color: map-get($config, color);
  $bg: map-get($config, bg);

  .group-cell.#{$status} {
    .icon-wrap {
      color: $color;
      background: $bg;

      &.has-svg {
        background: transparent;
      }
    }
  }

  .status-text .dict-tag-span.#{$status},
  .status-text.#{$status} {
    color: $color;
  }
}

.table-wrap {
  margin-bottom: 16px;
  overflow-x: auto;
  border: 1px solid var(--el-border-color);

  :deep(.el-table) {
    min-width: 1000px;

    .el-table__cell {
      padding: 7px 0;
      vertical-align: middle;
    }

    td.diagnosis-group-col {
      border-right: 1px solid var(--el-table-border-color, var(--el-border-color-lighter));
    }

    th.diagnosis-group-col {
      border-right: none;
    }

    .el-table__header-wrapper .el-table__cell {
      background: #f5f5f8;
    }
  }
}

/* Flex layout utilities from common.css */
.flex-col {
  display: flex;
  flex-direction: column;
}

.flex-row {
  display: flex;
  flex-direction: row;
}

.justify-between {
  display: flex;
  justify-content: space-between;
}

.group-cell {
  justify-content: center;
  align-items: flex-start;

  .group-header {
    width: 100%;
    height: 32px;
    align-items: center;

    .icon-wrap {
      width: 32px;
      height: 32px;
      border-radius: 4px;
      align-items: center;
      justify-content: center;
      font-size: 16px;

      img {
        width: 32px;
        height: 32px;
      }
    }

    .tag-wrap {
      height: 20px;
      border-radius: 4px;
      justify-content: center;
      align-items: center;
      padding: 0 9px;
    }
  }

  .group-main {
    margin-top: 8px;
    min-height: 40px;
    width: 100%;
    align-items: flex-start;

    .title {
      font-size: 14px;
      color: rgba(0, 0, 0, 1);
      line-height: 20px;
      font-weight: normal;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: left;
    }

    .desc {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.65);
      line-height: 18px;
      margin-top: 2px;
      font-weight: normal;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: left;
    }
  }
}

.status-tag {
  font-weight: 600;
}

.status-loading {
  display: inline-flex;
  align-items: center;
  color: var(--el-text-color-secondary);
}

.streaming-tag {
  border: none;
  border-radius: 4px;
  background-color: rgba(144, 147, 153, 0.1);
  color: rgba(0, 0, 0, 0.65);
}

.impact {
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.save-tip {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .counts {
    justify-content: flex-start;
  }
}
</style>
