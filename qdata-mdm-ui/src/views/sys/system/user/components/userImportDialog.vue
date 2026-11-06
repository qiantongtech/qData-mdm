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
  <div ref="app-container" class="app-container user-dialog-host">
    <el-dialog
      v-model="visible"
      :title="title"
      width="800px"
      append-to-body
      draggable
      destroy-on-close
      class="dialog"
      :append-to="$refs['app-container']"
      @closed="resetForm"
    >
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="uploadHeaders"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :on-error="handleFileError"
        v-model:file-list="fileList"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <upload-filled />
        </el-icon>
        <div class="el-upload__text" v-html="td('sys.system.user.dragOrClick')"></div>
        <template #tip>
          <div class="el-upload__tip text-center">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" />
              {{ td('sys.system.user.updateExistingData') }}
            </div>
            <span>{{ td('sys.system.user.importTip') }}</span>
            <el-link
              type="primary"
              :underline="false"
              style="font-size: 12px; vertical-align: baseline"
              @click="importTemplate"
              >{{ td('sys.system.user.downloadTemplate') }}
            </el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="UserImportDialog">
/** 用户导入弹窗；导入完成后通知父页面刷新。 */
import { getCurrentInstance, reactive, ref } from 'vue';
import { getToken } from '@/utils/auth.js';
import useDefaultLang from '@/composables/useDefaultLang';

const emit = defineEmits(['success']);
const { proxy } = getCurrentInstance();
const { td } = useDefaultLang();
const visible = ref(false);
const title = ref('');
const submitting = ref(false);
const uploadRef = ref();
const fileList = ref([]);
const uploadHeaders = ref({});
const upload = reactive({
  isUploading: false,
  updateSupport: 0,
  url: import.meta.env.VITE_APP_BASE_API + '/system/user/importData'
});

/** 关闭后清空文件与上传状态。 */
function resetForm() {
  fileList.value = [];
  upload.isUploading = false;
  upload.updateSupport = 0;
  submitting.value = false;
  uploadRef.value?.clearFiles();
}

function openDialog() {
  resetForm();
  uploadHeaders.value = { Authorization: 'Bearer ' + getToken() };
  title.value = td('sys.system.user.userImport');
  visible.value = true;
}

function importTemplate() {
  proxy.download('system/user/importTemplate', {}, `user_template_${new Date().getTime()}.xlsx`);
}

function handleFileUploadProgress() {
  upload.isUploading = true;
}

function handleFileSuccess(response) {
  upload.isUploading = false;
  submitting.value = false;
  visible.value = false;
  proxy.$alert(
    "<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" +
      response.msg +
      '</div>',
    td('sys.system.user.importResult'),
    { dangerouslyUseHTMLString: true }
  );
  emit('success');
}

function handleFileError(error) {
  upload.isUploading = false;
  submitting.value = false;
  proxy.$modal.msgError(error?.message || td('sys.system.user.importFailed', '导入失败，请重试'));
}

function submitForm() {
  if (submitting.value || !uploadRef.value) return;
  if (!fileList.value.length) {
    proxy.$modal.msgWarning(td('sys.system.user.selectImportFile', '请选择要导入的文件'));
    return;
  }
  submitting.value = true;
  uploadRef.value.submit();
}

defineExpose({ openDialog });
</script>

<style scoped lang="scss">
.user-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}

:deep(.el-upload),
:deep(.el-upload-dragger) {
  width: 100%;
}

:deep(.el-upload-list__item) {
  width: 100%;
  height: 25px;
}
</style>
