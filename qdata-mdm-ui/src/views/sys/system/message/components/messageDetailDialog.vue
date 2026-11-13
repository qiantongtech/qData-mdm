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
  <div ref="app-container" class="app-container system-dialog-host">
    <el-dialog
      :append-to="$refs['app-container']"
      append-to-body
      :title="td('sys.system.message.msgDetail')"
      v-model="visible"
      width="800px"
      draggable
      destroy-on-close
      class="dialog msg-dialog"
    >
      <el-form class="column-form" label-width="120px" :label-position="labelPosition">
        <qt-form-item
          class="row-full"
          :label="td('sys.system.message.msgTitleLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">
            {{ viewData.title }}
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.message.typeLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">
            <dict-tag :options="message_category" :value="viewData.category" />
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.message.isReadLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">
            <el-tag :type="viewData.hasRead ? 'success' : 'danger'">
              {{
                viewData.hasRead ? td('sys.system.message.read') : td('sys.system.message.unread')
              }}
            </el-tag>
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.message.msgContentLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">
            {{ viewData.content }}
          </div>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.message.createTimeLabel')"
          :label-position="labelPosition"
        >
          <div class="form-value-ifon">
            {{ viewData.createTime }}
          </div>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.close') }}</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="MessageDetailDialog">
import { computed } from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';
const { td } = useDefaultLang();
const props = defineProps({ open: Boolean, viewData: Object, messageCategory: Array });
const emit = defineEmits(['update:open']);
const visible = computed({ get: () => props.open, set: (value) => emit('update:open', value) });
const viewData = computed(() => props.viewData);
const message_category = computed(() => props.messageCategory);
</script>
<style scoped lang="scss">
.system-dialog-host {
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
.msg-dialog {
  .el-dialog__body {
    height: 300px !important;
  }
}
</style>
