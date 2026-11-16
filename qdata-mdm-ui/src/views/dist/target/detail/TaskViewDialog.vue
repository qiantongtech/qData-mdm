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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <el-dialog v-if="appendTo" v-model="dialogVisible" title="查看任务" width="800px"
             :append-to="appendTo" draggable destroy-on-close
>
    <template #header>
      <span role="heading" aria-level="2" class="el-dialog__title">查看任务</span>
    </template>
    <div class="task-view-dialog" v-loading="loading">
      <el-empty v-if="loadError" description="任务详情加载失败">
        <el-button type="primary" plain @click="loadTaskDetail">重新加载</el-button>
      </el-empty>
      <template v-else-if="!loading">
        <el-form :model="detail" label-width="110px" class="column-form">
          <el-form-item v-for="item in detailItems" :key="item.label" :label="item.label"
                        :class="{ 'row-full': item.span === 2 }"
>
            <template v-if="item.tagOnly">
              <el-tag v-if="item.tagType" :type="item.tagType">{{ displayValue(item) }}</el-tag>
              <span v-else>-</span>
            </template>
            <div v-else class="form-readonly" :class="{ textarea: item.span === 2 }">{{ displayValue(item) }}</div>
          </el-form-item>
        </el-form>
      </template>
    </div>
    <template #footer>
      <div class="dialog-footer"><el-button @click="dialogVisible = false">关 闭</el-button></div>
    </template>
  </el-dialog>
</template>

<script setup name="DistTargetTaskViewDialog">
import { getTASK } from "@/api/dist/task/TASK";
import { cronToZh } from "@/utils/cronUtils";

const props = defineProps({
  visible: { type: Boolean, default: false },
  taskId: { type: [String, Number], default: null },
  targetSystemName: { type: String, default: "" },
  appendTo: { type: [Object, String], default: 'body' },
});
const emit = defineEmits(["update:visible"]);
const dialogVisible = computed({
  get: () => props.visible,
  set: value => emit("update:visible", value),
});
const detail = ref({});
const { proxy } = getCurrentInstance();
const loading = ref(false);
const loadError = ref(false);
let requestVersion = 0;

/** 最近执行结果使用详情接口中的实例状态与成功、失败数。 */
function formatRunResult(task) {
  if (!task.lastRunStatus) return "-";
  if (task.lastRunStatus === "RUNNING") return "执行中";
  if (task.lastSuccessCount != null || task.lastFailureCount != null) {
    return `成功 ${task.lastSuccessCount ?? 0} / 失败 ${task.lastFailureCount ?? 0}`;
  }
  return ({ SUCCESS: "成功", FAILED: "失败", FAILURE: "失败" })[task.lastRunStatus] || task.lastRunStatus;
}

/** 按任务当前执行方式展示调度规则，人工执行不显示旧 Cron 配置。 */
function formatSchedule(task) {
  return task.triggerType === "SCHEDULED"
    ? (cronToZh(task.cronExpression) || task.cronExpression || "-")
    : "-";
}

/** 保留目标系统任务字段，布局沿用本地敏感清单的双列详情弹窗。 */
const detailItems = computed(() => [
  { label: "任务名称", key: "name" },
  { label: "主数据模型", value: detail.value.modelName || (Number(detail.value.modelId) === -1 ? "供应商主数据" : detail.value.modelId) },
  { label: "数据来源", value: "权威主数据" },
  { label: "目标系统", value: detail.value.targetSystemName || props.targetSystemName || detail.value.targetSystemId },
  { label: "目标API", key: "apiUrl" },
  { label: "认证方式", value: ({ NONE: "无认证", BEARER: "Bearer Token", API_KEY: "API Key" })[detail.value.authType] || detail.value.authType },
  { label: "任务状态", value: ({ ENABLED: "已上线", DISABLED: "未上线" })[detail.value.status], tagOnly: true, tagType: detail.value.status === "ENABLED" ? "success" : detail.value.status === "DISABLED" ? "primary" : "" },
  { label: "分发类型", value: ({ FULL: "全量", INCREMENTAL: "增量" })[detail.value.dataMode], tagOnly: true, tagType: detail.value.dataMode === "INCREMENTAL" ? "success" : detail.value.dataMode === "FULL" ? "warning" : "" },
  { label: "执行方式", value: ({ MANUAL: "人工执行", SCHEDULED: "定时执行" })[detail.value.triggerType] || detail.value.triggerType },
  { label: "调度规则", value: formatSchedule(detail.value) },
  { label: "最近执行时间", key: "lastRunTime", type: "time" },
  { label: "最近执行结果", value: formatRunResult(detail.value) },
  { label: "创建人", key: "createBy" },
  { label: "创建时间", key: "createTime", type: "time" },
  { label: "负责人", key: "responsibleUserName", span: 2 },
  { label: "描述", key: "remark", span: 2 },
]);

/** 统一处理任务详情的空值和时间，保持只读字段与来源数据弹窗一致。 */
function displayValue(item) {
  const value = item.value ?? detail.value[item.key];
  if (value == null || value === "") return "-";
  return item.type === "time" ? proxy.parseTime(value, "{y}-{m}-{d} {h}:{i}") || "-" : String(value);
}

/** 打开弹窗时按分发任务 ID 读取任务详情。 */
async function loadTaskDetail() {
  if (!props.visible) return;
  const version = ++requestVersion;
  detail.value = {};
  loadError.value = false;
  if (!props.taskId) return;
  loading.value = true;
  try {
    const response = await getTASK(props.taskId);
    if (!response.data) throw new Error("任务不存在");
    if (version === requestVersion) detail.value = response.data;
  } catch {
    if (version === requestVersion) loadError.value = true;
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}

/** 仅在弹窗打开或切换任务时加载，关闭时清空上次详情。 */
watch(() => [props.visible, props.taskId], ([visible]) => {
  if (visible) {
    loadTaskDetail();
  } else {
    requestVersion++;
    detail.value = {};
    loading.value = false;
  }
}, { immediate: true });
</script>

<style scoped>
.task-view-dialog {
  min-height: 320px;
}
</style>
