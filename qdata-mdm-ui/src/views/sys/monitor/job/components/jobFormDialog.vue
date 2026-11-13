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
      :title="title"
      v-model="open"
      width="850px"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <el-form
        class="column-form"
        ref="dialogElement0"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item class="row-full" :label="td('sys.monitor.job.taskName')" prop="jobName">
          <el-input
            v-model="form.jobName"
            :placeholder="td('sys.monitor.job.taskNamePlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.job.taskGroupLabel')"
          prop="jobGroup"
          :label-position="labelPosition"
        >
          <el-select v-model="form.jobGroup" :placeholder="td('sys.monitor.job.selectPlaceholder')">
            <el-option
              v-for="dict in sys_job_group"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item class="row-full" prop="invokeTarget" :label-position="labelPosition">
          <template #label>
            <span>
              {{ td('sys.monitor.job.invokeMethod') }}
              <el-tooltip placement="top">
                <template #content>
                  <div>
                    {{ td('sys.monitor.job.beanExample') }}
                    <br />
                    {{ td('sys.monitor.job.classExample') }}
                    <br />
                    {{ td('sys.monitor.job.paramDesc') }}
                  </div>
                </template>
                <el-icon style="color: var(--qt-text-secondary)">
                  <info-filled />
                </el-icon>
              </el-tooltip>
            </span>
          </template>
          <el-input
            v-model="form.invokeTarget"
            :placeholder="td('sys.monitor.job.invokeTargetPlaceholder')"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.job.cronExprLabel')"
          prop="cronExpression"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.cronExpression"
            :placeholder="td('sys.monitor.job.cronExprPlaceholder')"
          >
            <template #append>
              <el-button type="primary" @click="handleShowCron">
                {{ td('sys.monitor.job.generate') }}
                <i class="el-icon-time el-icon--right" />
              </el-button>
            </template>
          </el-input>
        </qt-form-item>
        <template v-if="form.jobId !== undefined">
          <qt-form-item
            class="row-full"
            :label="td('common.texts.status')"
            :label-position="labelPosition"
          >
            <el-radio-group v-model="form.status">
              <el-radio v-for="dict in sys_job_status" :key="dict.value" :value="dict.value">
                {{ dict.label }}
              </el-radio>
            </el-radio-group>
          </qt-form-item>
        </template>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.job.execStrategy')"
          prop="misfirePolicy"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.misfirePolicy">
            <el-radio-button value="1">{{ td('sys.monitor.job.immediateExec') }}</el-radio-button>
            <el-radio-button value="2">{{ td('sys.monitor.job.executeOnce') }}</el-radio-button>
            <el-radio-button value="3">{{ td('sys.monitor.job.giveUpExec') }}</el-radio-button>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.monitor.job.concurrent')"
          prop="concurrent"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.concurrent">
            <el-radio-button value="0">{{ td('sys.monitor.job.allow') }}</el-radio-button>
            <el-radio-button value="1">{{ td('sys.monitor.job.forbid') }}</el-radio-button>
          </el-radio-group>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">{{
            td('common.button.confirm')
          }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup name="JobFormDialog">
import { ref, toRefs } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const { td } = useDefaultLang();
const props = defineProps({ state: { type: Object, required: true } });
const {
  title,
  open,
  cancel,
  submitLoading,
  submitForm,
  form,
  rules,
  sys_job_group,
  handleShowCron,
  sys_job_status
} = toRefs(props.state);
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
