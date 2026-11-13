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
      width="800px"
      draggable
      class="dialog"
      @closed="resetForm"
  >
    <el-form
        ref="projectFormRef"
        :model="form"
        :rules="readonly ? undefined : rules"
        label-width="120"
        class="column-form"
        @submit.prevent
    >
      <guide-tip-panel
          class="row-full domain-intro"
          title="主数据域用于划分和管理不同业务领域的主数据模型。请填写主数据域的基本信息，便于后续模型分类、统一管理与维护。"
          type="primary"
          allow-wrap
      />
      <qt-form-item label="主数据域名称" prop="name" class="row-full">
        <span v-if="readonly">{{ form.name || '-' }}</span>
        <el-input v-else v-model="form.name" placeholder="请输入主数据域名称，支持中文、英文字符、数字、下划线和短横线格"/>
      </qt-form-item>
      <qt-form-item label="主数据域编码" prop="code" class="row-full">
        <span v-if="readonly">{{ form.code || '-' }}</span>
        <el-input v-else v-model="form.code" placeholder="请输入主数据域编码，支持英文字母、数字和下划线"/>
      </qt-form-item>
      <qt-form-item
          label="图标"
          prop="icon"
          class="row-full"
          tip="请上传大小不超过 <span class='text-danger'>5MB</span> 格式为 <span class='text-danger'>png/jpg/jpeg</span> 的图片，未上传时展示默认图标。">

      <img v-if="readonly" :src="getProjectIcon(form.icon)" alt="主数据域图标" class="project-icon-preview"/>
        <ImageUpload
            v-else
            v-model="form.icon"
            class="project-icon-upload"
            :class="{ 'project-icon-upload--default': !form.icon }"
            :title="!form.icon ? '点击上传图标' : undefined"
            :limit="1"
            :file-size="5"
            :file-type="['png', 'jpg', 'jpeg']"
            :is-show-tip="false"
        />
      </qt-form-item>
      <qt-form-item
          label="负责人"
          prop="responsibleUserId"
          class="row-full"
      >
        <span v-if="readonly">{{ responsibleUser?.label || '-' }}</span>
        <el-select
            v-else
            v-model="form.responsibleUserId"
            placeholder="请选择负责人"
            filterable
            clearable
            :loading="usersLoading"
        >
          <el-option v-for="user in userOptions" :key="user.value" :label="user.label" :value="user.value"/>
        </el-select>
        <el-button v-if="usersLoadFailed" link type="primary" @click="loadUsers">加载失败，点击重试</el-button>
      </qt-form-item>
      <qt-form-item label="联系电话" class="row-full">
        <span v-if="readonly">{{ getResponsiblePhone(form.responsibleUserId) || '-' }}</span>
        <el-input v-else :model-value="getResponsiblePhone(form.responsibleUserId) || ''" disabled
                  placeholder="请输入联系电话"/>
      </qt-form-item>
      <qt-form-item label="描述" prop="description" class="row-full">
        <span v-if="readonly">{{ form.description || '-' }}</span>
        <el-input
            v-else
            v-model="form.description"
            type="textarea"
            maxlength="256个字符"
            show-word-limit
            placeholder="请输入该主题域的业务范围描述"
            :min-height="192"
        />
      </qt-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">{{ readonly ? '关 闭' : '取 消' }}</el-button>
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

<script setup name="ProjectDialog">
/**
 * @description 主数据域新增、编辑与只读详情弹窗。
 * @emits success 保存成功后通知父页面刷新
 */
import ImageUpload from "@/components/ImageUpload/index.vue";
import {getProjectIcon} from "@/utils/imageUrl";
import {addProject, getProject, updateProject} from "@/api/model/project/project";
import {
  validateAlphaNumUnderscore,
  validatePattern,
  validateTrimmedRequired,
  trimText
} from "@/utils/validate";

import { useUserDirectory } from '@/store/system/userDirectory';
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const { loadResponsibleUserOptions, getResponsibleUser, getResponsiblePhone } = useUserDirectory();

const emit = defineEmits(["success"]);
const {proxy} = getCurrentInstance();

const visible = ref(false);
const readonly = ref(false);
const title = ref("");
const submitting = ref(false);
const projectFormRef = ref();
const userOptions = ref([]);
const usersLoading = ref(false);
const usersLoadFailed = ref(false);
const responsibleUser = computed(() => getResponsibleUser(form.value.responsibleUserId));

// 复用公共用户列表，失败时保留重试入口，不影响先查看或填写其他表单项。
async function loadUsers() {
  usersLoading.value = true;
  usersLoadFailed.value = false;
  try {
    userOptions.value = await loadResponsibleUserOptions(form.value.responsibleUserId);
  } catch {
    usersLoadFailed.value = true;
  } finally {
    usersLoading.value = false;
  }
}

const createEmptyForm = () => ({
  id: null,
  code: null,
  name: null,
  icon: "",
  responsibleUserId: null,
  description: null
});

const form = ref(createEmptyForm());
const rules = {
  code: [
    {required: true, validator: validateTrimmedRequired("主数据域编码不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "主数据域名称支持英文字母、数字和下划线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
  name: [
    {required: true, validator: validateTrimmedRequired("主数据域名称不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "主数据域名称支持中文、英文字符、数字、下划线和短横线格"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
};

/** 重置表单数据并清除校验结果。 */
function resetForm() {
  form.value = createEmptyForm();
  userOptions.value = [];
  usersLoadFailed.value = false;
  projectFormRef.value?.clearValidate();
}

/** 初始化并打开弹窗，按传入记录加载详情。 */
async function openDialog(id, detail = false) {
  resetForm();
  readonly.value = detail;
  title.value = detail ? "主数据域详情" : id ? "修改主数据域" : "新增主数据域";

  if (id) {
    const response = await getProject(id);
    // 旧记录可能没有图标或负责人；统一空值，并让负责人 ID 与下拉选项的字符串类型一致。
    form.value = {
      ...response.data,
      icon: response.data.icon || "",
      responsibleUserId: response.data.responsibleUserId == null ? null : String(response.data.responsibleUserId),
    };
  }
  visible.value = true;
  loadUsers();
}

/** 校验表单并保存当前记录，成功后通知父页面刷新。 */
async function submitForm() {
  const valid = await projectFormRef.value.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    const editing = form.value.id != null;
    // 空图标表示使用默认图标，空负责人表示取消选择，不把默认图片地址写入数据库。
    const payload = {
      ...form.value,
      icon: form.value.icon || "",
      responsibleUserId: form.value.responsibleUserId || null,
    };
    await (editing ? updateProject(payload) : addProject(payload));
    proxy.$modal.msgSuccess(editing ? "修改成功" : "新增成功");
    visible.value = false;
    emit("success");
  } finally {
    submitting.value = false;
  }
}

defineExpose({openDialog});
</script>

<style scoped lang="scss">
.project-icon-preview {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

.project-icon-upload {
  line-height: 1;

  :deep(.el-upload--picture-card),
  :deep(.el-upload-list--picture-card .el-upload-list__item) {
    width: 48px !important;
    height: 48px !important;
    line-height: 48px !important;
  }

  :deep(.avatar-uploader-icon) {
    font-size: 18px;
  }

  &.project-icon-upload--default {
    // 没上传时用默认图标替代空上传框，悬浮才露出加号，点击上传的行为保持原样。
    :deep(.el-upload--picture-card) {
      position: relative;
      border: 0;
      background: transparent url('@/assets/common/icons/icon-project-default.svg') center / 32px 32px no-repeat !important;

      &::after {
        content: "+";
        position: absolute;
        inset: 8px;
        display: grid;
        place-items: center;
        border-radius: 4px;
        color: #fff;
        background: rgba(0, 0, 0, 0.5);
        font-size: 22px;
        line-height: 1;
        opacity: 0;
        pointer-events: none;
      }

      &:hover::after,
      &:focus-within::after {
        opacity: 1;
      }
    }

    :deep(.avatar-uploader-icon) {
      visibility: hidden;
    }
  }
}
</style>
