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
  <el-dialog
      v-model="visible"
      title="编码方案版本详情"
      width="800px"
      draggable
      destroy-on-close
      class="dialog"
  >
    <el-form
        :model="form"
        label-width="120px"
        class="column-form"
        @submit.prevent
    >
      <qt-form-item label="版本号" prop="versionNo" class="row-full">
        <div class="form-readonly">{{ form.versionNo || '-' }}</div>
      </qt-form-item>
      <qt-form-item label="状态" prop="status" class="row-full">
        <div class="form-readonly">{{ versionStatus(form.status) }}</div>
      </qt-form-item>
      <qt-form-item label="已产生编码" class="row-full">
        <div class="form-readonly">{{ form.status == 2 ? '否' : '是' }}</div>
      </qt-form-item>
      <qt-form-item label="创建时间" prop="createTime" class="row-full">
        <div class="form-readonly">{{ form.createTime || '-' }}</div>
      </qt-form-item>
      <qt-form-item label="备注" prop="remark" class="row-full">
        <div class="form-readonly textarea">{{ form.remark || '-' }}</div>
      </qt-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">关 闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="VersionDetailDialog">
/** @description 编码方案版本详情弹窗。 */
import {getVersion} from "@/api/model/encoding/version";

const {proxy} = getCurrentInstance();
const {model_encoding_status} = proxy.useDict("model_encoding_status");
const visible = ref(false);
const createEmptyForm = () => ({
  id: null,
  encodingId: null,
  versionNo: null,
  status: null,
  remark: null,
  createTime: null,
});
const form = ref(createEmptyForm());

/** 将编码方案版本状态转换为字典展示名称。 */
function versionStatus(status) {
  return model_encoding_status.value.find((item) => item.value == status)?.label || "-";
}

/** 初始化并打开弹窗，按传入记录加载详情。 */
async function openDialog(id) {
  form.value = createEmptyForm();
  const response = await getVersion(id);
  form.value = response.data;
  visible.value = true;
}

defineExpose({openDialog});
</script>
