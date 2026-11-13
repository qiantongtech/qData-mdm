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
      :title="title"
      :width="creating ? '1000px' : '800px'"
      draggable
      class="encoding-dialog"
      @closed="resetForm"
  >
    <el-form
        ref="encodingFormRef"
        :model="form"
        :rules="readonly ? undefined : rules"
        label-width="80"
        class="column-form"
        @submit.prevent
    >
      <guide-tip-panel
          v-if="creating"
          class="row-full domain-intro m0"
          title="填写方案基本信息并配置编码规则，保存后生成 V1.0 草稿版本，确认规则无误后可发布生效。"
          type="primary"
          allow-wrap
      />
      <h3 v-if="creating" class="h2-titles row-full m0">基本信息</h3>
      <qt-form-item label="方案名称" prop="name" class="row-full">
        <span v-if="readonly">{{ form.name || '-' }}</span>
        <el-input v-else v-model="form.name"
                  placeholder="请输入方案名称，支持中文、英文字符、数字、下划线和短横线。"
        />
      </qt-form-item>
      <qt-form-item label="方案编码" prop="code" class="row-full">
        <span v-if="readonly">{{ form.code || '-' }}</span>
        <el-input v-else v-model="form.code" placeholder="请输入方案编码，支持英文字母、数字和下划线"/>
      </qt-form-item>
      <qt-form-item label="当前版本" class="row-full">
        <el-input v-model="form.version.versionNo" disabled/>
      </qt-form-item>
      <qt-form-item label="版本状态" class="row-full">
        <el-input disabled :model-value="versionStatus(form.version.status)"/>
      </qt-form-item>
      <qt-form-item label="描述" prop="description" class="row-full">
        <span v-if="readonly">{{ form.description || '-' }}</span>
        <el-input
            v-else
            v-model="form.description"
            type="textarea"
            maxlength="256个字符"
            show-word-limit
            placeholder="请输入编码方案描述"
            :min-height="192"
        />
      </qt-form-item>
    </el-form>
    <section v-if="visible && creating" class="mt15">
      <h3 class="h2-titles m0">规则配置</h3>
      <encoding-item
          ref="ruleEditorRef"
          draft-mode
          @rule-count="ruleCount = $event"
          class="mt15"
      />
    </section>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ readonly ? '关 闭' : '取 消' }}</el-button>
        <el-button
            v-if="creating"
            :disabled="ruleCount === 0"
            @click="ruleEditorRef?.handleCalculate()"
        >
          试计算
        </el-button>
        <el-button
            v-if="!readonly"
            type="primary"
            :loading="submitting"
            @click="submitForm"
        >确 定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="EncodingDialog">
/**
 * @description 编码方案新增与编辑弹窗。
 * @emits success 保存成功后通知父页面刷新
 */
import {addEncoding, getEncoding, updateEncoding} from "@/api/model/encoding/encoding";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";
import {trimText, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";
import EncodingItem from "../detail/components/encodingItem.vue";

const {proxy} = getCurrentInstance();
const {model_encoding_status} = proxy.useDict(
    "model_encoding_status",
);

const emit = defineEmits(["success"]);

const visible = ref(false);
const readonly = ref(false);
const title = ref("");
const submitting = ref(false);
const encodingFormRef = ref();
const ruleEditorRef = ref();
const ruleCount = ref(0);

/** 将编码方案版本状态转换为字典展示名称。 */
function versionStatus(status) {
  return model_encoding_status.value.find((item) => item.value == status)
      ?.label || "-";
}

const createEmptyForm = () => ({
  id: null,
  code: null,
  name: null,
  description: null,
  version: {
    versionNo: "V1.0",
    status: 1
  }
});

const form = ref(createEmptyForm());
const creating = computed(() => !readonly.value && form.value.id == null);
const rules = {
  code: [
    {required: true, validator: validateTrimmedRequired("方案编码不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "方案编码支持英文字母、数字和下划线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
  name: [
    {required: true, validator: validateTrimmedRequired("方案名称不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "方案名称支持中文、英文字符、数字、下划线和短横线。"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
};

/** 重置表单数据并清除校验结果。 */
function resetForm() {
  form.value = createEmptyForm();
  ruleCount.value = 0;
  encodingFormRef.value?.clearValidate();
}

/** 初始化并打开弹窗，按传入记录加载详情。 */
async function openDialog(id, detail = false) {
  resetForm();
  readonly.value = detail;
  title.value = detail ? "编码方案详情" : id ? "修改编码方案" : "新增编码方案";

  if (id) {
    const response = await getEncoding(id);
    form.value = response.data;
  }
  visible.value = true;
}

/**
 * 校验并提交编码方案。新增时一并提交草稿规则，成功后刷新列表。
 * @returns {Promise<void>}
 */
async function submitForm() {
  const valid = await encodingFormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const editing = form.value.id != null;
    if (editing) {
      await updateEncoding(form.value);
    } else {
      await addEncoding({...form.value, items: ruleEditorRef.value.buildSavePayload()});
    }
    proxy.$modal.msgSuccess(editing ? "修改成功" : "新增成功");
    visible.value = false;
    emit("success");
  } finally {
    submitting.value = false;
  }
}

defineExpose({openDialog});
</script>

<style lang="scss">
.encoding-dialog {
  .el-dialog__body {
    padding: var(--qt-space-3) 14px 0;
  }

  .dialog-footer {
    text-align: right;
  }

  .domain-intro {
    --tip-title-weight: normal;
    --tip-title-color: var(--el-text-color-regular);
  }

  .el-form-item.has-tip .tip-content span {
    overflow: visible;
    text-overflow: clip;
    white-space: normal;
  }
}
</style>
