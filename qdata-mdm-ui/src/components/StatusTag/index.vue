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
  <el-tag
    :type="currentStatusInfo.type"
    :size="size"
    :class="[
      currentStatusInfo.className,
      { 'status-tag--small': size === 'small' },
    ]"
  >
    <span class="tag-content">
      <el-icon
        v-if="currentStatusInfo.component"
        class="status-icon"
        :class="{ 'is-loading': currentStatusInfo.name === '运行中' }"
      >
        <component :is="currentStatusInfo.component" />
      </el-icon>
      <img v-else :src="currentStatusInfo.icon" class="status-icon" />
      <span class="status-text">{{ currentStatusInfo.name }}</span>
    </span>
  </el-tag>
</template>

<script setup>
import { computed } from "vue";
import { Loading, Clock, CircleCloseFilled } from "@element-plus/icons-vue";
import iconSuccess from "@/assets/common/icons/status-tag/success.svg";
import iconFailed from "@/assets/common/icons/status-tag/failed.svg";
import iconIdle from "@/assets/common/icons/status-tag/idle.svg";

const props = defineProps({
  status: {
    type: [String, Number],
    default: "idle",
  },
  size: {
    type: String,
    default: "default",
  },
});

const statusMap = {
  "1": { type: "primary", component: Loading, name: "运行中" },
  "2": { type: "danger", icon: iconFailed, name: "失败" },
  "3": { type: "danger", icon: iconFailed, name: "失败" },
  "4": { type: "success", icon: iconSuccess, name: "成功" },
  waiting: {
    type: "warning",
    component: Clock,
    name: "等待",
  },
  running: { type: "primary", component: Loading, name: "运行中" },
  success: { type: "success", icon: iconSuccess, name: "成功" },
  failed: { type: "danger", icon: iconFailed, name: "失败" },
  passed: { type: "success", icon: iconSuccess, name: "通过" },
  notPassed: { type: "danger", icon: iconFailed, name: "未通过" },
  stopped: {
    type: "info",
    component: CircleCloseFilled,
    name: "停止",
  },
  idle: {
    type: "",
    className: "status-tag--idle",
    icon: iconIdle,
    name: "空闲",
  },
};

const currentStatusInfo = computed(() => {
  const canonicalStatus = statusMap[props.status] ? props.status : "idle";
  return statusMap[canonicalStatus];
});
</script>

<style scoped lang="scss">
.el-tag {
  cursor: pointer;
}

.tag-content {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-text {
  position: relative;
  //top: 1px;
}

.status-icon {
  width: 15px;
  height: 15px;
  &.is-loading {
    animation: rotating 2s linear infinite;
  }
}

.status-tag--idle {
  --el-tag-bg-color: #f3e8ff;
  --el-tag-border-color: #d8b4fe;
  --el-tag-text-color: #7e22ce;
}

:deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

:deep(.el-tag__content) {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}

@keyframes rotating {
  0% {
    transform: rotateZ(0deg);
  }
  100% {
    transform: rotateZ(360deg);
  }
}

.status-tag--small {
  :deep(.el-tag) {
    height: 20px;
    padding: 3px 6px;
    font-size: 10px !important;
    line-height: 18px;
    vertical-align: middle;
  }
  :deep(.el-tag__content) {
    display: inline-flex;
    align-items: center;
    vertical-align: middle;
  }
  .tag-content {
    gap: 0;
  }
  .status-text {
    font-size: 12px;
  }
  .status-icon {
    width: 12px;
    height: 12px;
    margin-right: 3px;
  }
}
</style>
