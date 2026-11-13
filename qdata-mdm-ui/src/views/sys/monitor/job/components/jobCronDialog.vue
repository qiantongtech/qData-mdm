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
  <div ref="app-container" class="app-container sys-dialog-host">
    <el-dialog
      draggable
      append-to-body
      :title="td('sys.monitor.job.cronGenerator')"
      v-model="openCron"
      :append-to="$refs['app-container']"
      destroy-on-close
    >
      <crontab
        ref="dialogElement0"
        @hide="openCron = false"
        @fill="crontabFill"
        :expression="expression"
      />
    </el-dialog>
  </div>
</template>
<script setup name="JobCronDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';
import Crontab from '@/components/Crontab/index.vue';
const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { openCron, crontabFill, expression } = toRefs(props.state);
const dialogElement0 = ref(null);
defineExpose({ dialogElement0 });
</script>
<style scoped lang="scss">
.sys-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}
.column-form {
  display: flex;
  flex-direction: column;
}
.row-full {
  width: 100%;
  margin-right: 0;
}
.column-form :deep(.el-select),
.column-form :deep(.el-tree-select) {
  width: 100%;
}
</style>
