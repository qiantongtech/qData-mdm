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
      append-to-body
      :title="td('sys.monitor.jobLog.scheduleLogDetail')"
      v-model="open"
      width="800px"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <el-form
        class="column-form"
        :model="form"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.logIndex')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.jobLogId }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.taskName')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.jobName }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.taskGroupLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.jobGroup }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.execTime')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.createTime }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.invokeMethod')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.invokeTarget }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.logInfo')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.jobMessage }}</div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.execStatus')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon" v-if="form.status == 0">
            {{ td('sys.monitor.jobLog.normal') }}
          </div>
          <div class="form-value-ifon" v-else-if="form.status == 1">
            {{ td('sys.monitor.jobLog.failed') }}
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.jobLog.exceptionInfo')"
          v-if="form.status == 1"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">{{ form.exceptionInfo }}</div>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">{{ td('common.button.close') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="JobLogDetailDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const { open, form } = toRefs(props.state);

defineExpose({});
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
<style scoped lang="scss">
.form-value-ifon {
  width: 100%;
  border: 1px solid #c0c4cc;
  padding: 0px 10px;
  min-height: 34px;
}
</style>
