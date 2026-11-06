<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This document is part of the qData Master Data Platform (open source version).

  qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
  You may use qData for commercial purposes, but you may not remove, hide,
  modify, or replace the qData master data platform logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData master data platform as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div ref="app-container" class="app-container sys-dialog-host">
    <el-dialog
      v-model="open"
      :title="td('common.button.details')"
      width="800px"
      append-to-body
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
      class="dialog"
      @close="clearDetail"
    >
      <el-form
        v-loading="loading"
        class="column-form"
        :model="form"
        label-width="120px"
        :label-position="labelPosition"
      >
        <qt-form-item
          v-for="field in fields"
          :key="field.prop"
          class="row-full"
          :label="field.label"
          :label-position="labelPosition"
        >
          <div class="form-readonly">
            <dict-tag
              v-if="field.options && form[field.prop] != null"
              :options="field.options"
              :value="form[field.prop]"
            />
            <template v-else>{{ displayValue(field) }}</template>
          </div>
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

<script setup name="ClientDetailDialog">
import { computed, getCurrentInstance, ref } from 'vue';
import QtFormItem from '@/components/QtFormItem/index.vue';
import useDefaultLang from '@/composables/useDefaultLang';
import { parseTime } from '@/utils/anivia.js';
import { getClient } from '@/api/system/auth/client.js';

const { td } = useDefaultLang();
const { auth_app_type, auth_public, sys_valid } = getCurrentInstance().proxy.useDict(
  'auth_app_type',
  'auth_public',
  'sys_valid'
);
const open = ref(false);
const loading = ref(false);
const form = ref({});
let activeRequest = 0;
const fields = computed(() => [
  { prop: 'id', label: td('common.texts.number') },
  { prop: 'homeUrl', label: td('sys.client.clientHomepage') },
  { prop: 'syncUrl', label: td('sys.client.syncUrl') },
  { prop: 'name', label: td('sys.client.clientName') },
  { prop: 'type', label: td('sys.client.clientType'), options: auth_app_type.value },
  { prop: 'publicFlag', label: td('sys.client.isPublic'), options: auth_public.value },
  { prop: 'validFlag', label: td('sys.client.isActive'), options: sys_valid.value },
  { prop: 'icon', label: td('sys.client.clientIcon') },
  { prop: 'redirectUrl', label: td('sys.client.authorizedUrl') },
  { prop: 'remark', label: td('common.texts.remark') },
  { prop: 'createTime', label: td('common.texts.createdTime'), date: true },
  { prop: 'updateTime', label: td('common.texts.updatedTime'), date: true }
]);

function displayValue(field) {
  const value = field.value ? field.value(form.value) : form.value[field.prop];
  if (value == null || value === '') return '-';
  return field.date ? parseTime(value) : value;
}

async function openDialog(row) {
  const requestId = ++activeRequest;
  form.value = { ...row };
  open.value = true;
  loading.value = true;
  try {
    const response = await getClient(row.id);
    if (requestId !== activeRequest) return;
    form.value = { ...row, ...response.data };
  } catch {
    if (requestId === activeRequest) open.value = false;
  } finally {
    if (requestId === activeRequest) loading.value = false;
  }
}

function clearDetail() {
  activeRequest += 1;
  form.value = {};
  loading.value = false;
}

defineExpose({ openDialog });
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
.form-readonly {
  width: 100%;
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
