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
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="8">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section style="height: calc(100vh - 125px)" class="sys-panel">
            <div class="sys-panel-heading">
              <collection style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.cacheList.title') }}</span>
              <el-button
                style="float: right; padding: 3px 0"
                link
                type="primary"
                icon="Refresh"
                @click="refreshCacheNames()"
              />
            </div>
            <div v-loading="loading">
              <qt-table v-bind="tableStore0" ref="qtTableRef0">
                <template #remark="{ row }">{{ row.remark }}</template>
                <template #action="scope">
                  <el-button
                    link
                    type="danger"
                    icon="Delete"
                    @click="handleClearCacheName(scope.row)"
                  />
                </template>
              </qt-table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="8">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section style="height: calc(100vh - 125px)" class="sys-panel">
            <div class="sys-panel-heading">
              <key style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{ td('sys.monitor.cacheList.keyList') }}</span>
              <el-button
                style="float: right; padding: 3px 0"
                link
                type="primary"
                icon="Refresh"
                @click="refreshCacheKeys()"
              />
            </div>
            <div v-loading="subLoading">
              <qt-table v-bind="tableStore1" ref="qtTableRef1">
                <template #action="scope">
                  <el-button
                    link
                    type="danger"
                    icon="Delete"
                    @click="handleClearCacheKey(scope.row)"
                  />
                </template>
              </qt-table>
            </div>
          </section>
        </qt-wrap>
      </el-col>
      <el-col :span="8">
        <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
          <section style="height: calc(100vh - 125px)" class="sys-panel">
            <div class="sys-panel-heading">
              <document style="width: 1em; height: 1em; vertical-align: middle" />
              <span style="vertical-align: middle">{{
                td('sys.monitor.cacheList.cacheContent')
              }}</span>
              <el-button
                style="float: right; padding: 3px 0"
                link
                type="primary"
                icon="Refresh"
                @click="handleClearCacheAll()"
              >
                {{ td('sys.monitor.cacheList.clearAll') }}
              </el-button>
            </div>
            <el-form class="column-form" :model="cacheForm">
              <qt-form-item
                class="row-full"
                :label="td('sys.monitor.cacheList.cacheName') + ':'"
                prop="cacheName"
              >
                <el-input v-model="cacheForm.cacheName" :readOnly="true" />
              </qt-form-item>
              <qt-form-item
                class="row-full"
                :label="td('sys.monitor.cacheList.cacheKeyName') + ':'"
                prop="cacheKey"
              >
                <el-input v-model="cacheForm.cacheKey" :readOnly="true" />
              </qt-form-item>
              <qt-form-item
                class="row-full"
                :label="td('sys.monitor.cacheList.cacheContent') + ':'"
                prop="cacheValue"
              >
                <el-input
                  v-model="cacheForm.cacheValue"
                  type="textarea"
                  :rows="8"
                  :readOnly="true"
                />
              </qt-form-item>
            </el-form>
          </section>
        </qt-wrap>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="CacheList">
import QtFormItem from '@/components/QtFormItem/index.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';

import useDefaultLang from '@/composables/useDefaultLang';
import {
  listCacheName,
  listCacheKey,
  getCacheValue,
  clearCacheName,
  clearCacheKey,
  clearCacheAll
} from '@/api/system/monitor/cache.js';

const { td } = useDefaultLang();
const { proxy } = getCurrentInstance();

const cacheNames = ref([]);
const cacheKeys = ref([]);
const cacheForm = ref({});
const loading = ref(true);
const subLoading = ref(false);
const nowCacheName = ref('');
const tableHeight = ref(window.innerHeight - 200);

/** Query cache name list */
function getCacheNames() {
  loading.value = true;
  listCacheName().then((response) => {
    cacheNames.value = response.data;
    loading.value = false;
  });
}

/** Refresh the cached name list */
function refreshCacheNames() {
  getCacheNames();
  proxy.$modal.msgSuccess(td('sys.monitor.cacheList.refreshSuccess'));
}

/** Clear cache of specified name */
function handleClearCacheName(row) {
  clearCacheName(row.cacheName).then((response) => {
    proxy.$modal.msgSuccess(td('sys.monitor.cacheList.clearNameSuccess', { name: row.cacheName }));
    getCacheKeys();
  });
}

/** Query the cache key list */
function getCacheKeys(row) {
  const cacheName = row !== undefined ? row.cacheName : nowCacheName.value;
  if (cacheName === '') {
    return;
  }
  subLoading.value = true;
  listCacheKey(cacheName).then((response) => {
    cacheKeys.value = response.data;
    subLoading.value = false;
    nowCacheName.value = cacheName;
  });
}

/** Refresh the cache key list */
function refreshCacheKeys() {
  getCacheKeys();
  proxy.$modal.msgSuccess(td('sys.monitor.cacheList.refreshKeySuccess'));
}

/** Clear cache of specified key name */
function handleClearCacheKey(cacheKey) {
  clearCacheKey(cacheKey).then((response) => {
    proxy.$modal.msgSuccess(td('sys.monitor.cacheList.clearKeySuccess', { key: cacheKey }));
    getCacheKeys();
  });
}

/** List prefix removal */
function nameFormatter(row) {
  return row.cacheName.replace(':', '');
}

/** Key name prefix removal */
function keyFormatter(cacheKey) {
  return cacheKey.replace(nowCacheName.value, '');
}

/** Query cache content details */
function handleCacheValue(cacheKey) {
  getCacheValue(nowCacheName.value, cacheKey).then((response) => {
    cacheForm.value = response.data;
  });
}

/** Clear all cache */
function handleClearCacheAll() {
  clearCacheAll().then((response) => {
    proxy.$modal.msgSuccess(td('sys.monitor.cacheList.clearAllSuccess'));
  });
}

getCacheNames();

const qtTableRef0 = ref(null);

const tableStore0 = reactive({
  config: computed(() => ({
    notPagination: true,
    table: {
      stripe: true,
      height: tableHeight,
      highlightCurrentRow: true,
      onRowClick: getCacheKeys,
      style: 'width: 100%'
    }
  })),
  columns: [
    {
      sortable: true,
      get label() {
        return td('common.display.index');
      },
      width: 60,
      type: 'index'
    },
    {
      get label() {
        return td('sys.monitor.cacheList.cacheName');
      },
      align: 'center',
      showOverflowTooltip: true,
      formatter: nameFormatter,
      prop: 'cacheName'
    },
    {
      get label() {
        return td('common.texts.remark');
      },
      align: 'center',
      showOverflowTooltip: true,
      slot: 'remark'
    },
    {
      get label() {
        return td('common.texts.operation');
      },
      width: '60',
      align: 'center',
      className: 'small-padding fixed-width',
      slot: 'action'
    }
  ],
  func: async () => ({ data: unref(cacheNames) }),
  params: {},
  events: {}
});

watch([() => unref(cacheNames)], () => qtTableRef0.value?.resetQuery(), { flush: 'post' });

const qtTableRef1 = ref(null);

const tableStore1 = reactive({
  config: computed(() => ({
    notPagination: true,
    table: {
      stripe: true,
      height: tableHeight,
      highlightCurrentRow: true,
      onRowClick: handleCacheValue,
      style: 'width: 100%'
    }
  })),
  columns: [
    {
      sortable: true,
      get label() {
        return td('common.display.index');
      },
      width: 60,
      type: 'index'
    },
    {
      get label() {
        return td('sys.monitor.cacheList.cacheKeyName');
      },
      align: 'center',
      showOverflowTooltip: true,
      formatter: keyFormatter
    },
    {
      get label() {
        return td('common.texts.operation');
      },
      width: '60',
      align: 'center',
      className: 'small-padding fixed-width',
      slot: 'action'
    }
  ],
  func: async () => ({ data: unref(cacheKeys) }),
  params: {},
  events: {}
});

watch([() => unref(cacheKeys)], () => qtTableRef1.value?.resetQuery(), { flush: 'post' });
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
.sys-panel {
  padding: 15px;
  background: var(--qt-surface-background);
  border-radius: var(--qt-radius-small);
}
.sys-panel-heading {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 15px;
}
</style>
