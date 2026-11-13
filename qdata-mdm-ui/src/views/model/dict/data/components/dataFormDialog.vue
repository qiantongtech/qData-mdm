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
  <!-- 新增或修改参考数据项对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :append-to="appendTo"
    draggable
  >
    <template #header>
      <span role="heading" aria-level="2" class="el-dialog__title">
        {{ title }}
      </span>
    </template>
    <el-form
      ref="formRef"
      :model="localForm"
      :rules="rules"
      label-width="80px"
      @submit.prevent
    >
      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="名称" prop="name">
            <el-input v-model="localForm.name" placeholder="请输入名称，支持中文、英文字符、数字、下划线和短横线格" />
          </qt-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="编码值" prop="code">
            <el-input v-model="localForm.code" placeholder="请输入编码值，支持英文字母、数字和下划线" />
          </qt-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="排序号" prop="sortOrder">
            <el-input-number
              v-model="localForm.sortOrder"
              class="full-width"
              controls-position="right"
              :min="0"
            />
          </qt-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col :span="24">
          <qt-form-item label="状态" prop="validFlag">
            <el-radio v-model="localForm.validFlag" :label="false">禁用</el-radio>
            <el-radio v-model="localForm.validFlag" :label="true">启用</el-radio>
          </qt-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="24">
        <el-col>

          <qt-form-item label="描述" prop="description">
            <el-input
              v-model="localForm.description"
              type="textarea"
              maxlength="256个字符"
              show-word-limit
              placeholder="请输入目录描述"
              :min-height="192"
            />
          </qt-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="DataFormDialog">
/**
 * @description 参考数据项新增与编辑表单。
 * @prop {Boolean} open open
 * @prop {String} title 弹窗标题
 * @prop {Object} form form
 * @prop {Object} rules rules
 * @prop {[String, Number]} typeId 参考数据类型或主数据域 ID
 * @prop {[String, Object]} appendTo appendTo
 * @emits update:open update:open
 * @emits cancel 取消当前表单操作
 * @emits submit 提交校验后的表单数据
 */
const props = defineProps({
  open: {
    type: Boolean,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  form: {
    type: Object,
    required: true,
  },
  rules: {
    type: Object,
    required: true,
  },
  typeId: {
    type: [String, Number],
    default: undefined,
  },
  appendTo: {
    type: [String, Object],
    default: undefined,
  },
});

const emit = defineEmits(["update:open", "cancel", "submit"]);
const formRef = ref();

const createEmptyForm = () => ({
  id: null,
  typeId: null,
  code: null,
  name: null,
  sortOrder: 0,
  description: null,
  validFlag: false,
});

const localForm = ref(createEmptyForm());

const dialogVisible = computed({
  get: () => props.open,
  set: value => emit("update:open", value),
});

watch(
  () => [props.form, props.typeId],
  ([value, typeId]) => {
    localForm.value = {
      ...createEmptyForm(),
      ...(value || {}),
      typeId: typeId ?? value?.typeId ?? null,
    };
    nextTick(() => formRef.value?.clearValidate());
  },
  { immediate: true, deep: true }
);

/** 关闭表单弹窗并通知父组件。 */
function handleCancel() {
  emit("cancel");
}

/** 校验表单并向父页面提交数据，由父页面执行保存。 */
async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  emit("submit", { ...localForm.value });
}
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}
</style>
