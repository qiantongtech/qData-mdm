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

<!--
  来源系统编辑弹窗：维护负责人、可信度及数据连接关联。
  emits：success 保存成功时通知父页，携带当前表单 ID。
  expose：openAdd 打开新增表单；openEdit 按来源系统 ID 打开修改表单。
-->
<template>
  <div ref="app-container">
    <el-dialog
        :title="title"
        v-model="open"
        width="800px"
        :append-to="$refs['app-container']"
        draggable
    >
      <el-form
          ref="sourceRef"
          :model="form"
          :rules="rules"
          label-width="130px"
          class="column-form"
          @submit.prevent
      >
        <guide-tip-panel
            v-if="!form.id"
            class="row-full"
            type="primary"
            allow-wrap
            title="用于维护来源系统基础信息，并关联已配置的数据库连接，为后续数据接入和主数据整合提供来源支撑。"
        />
        <qt-form-item label="来源系统名称" prop="name" class="row-full">
          <el-input
            v-model="form.name"
            placeholder="请输入来源系统名称，支持中文、英文字符、数字、下划线和短横线"
            maxlength="64"
          />
        </qt-form-item>
        <qt-form-item label="来源系统编码" prop="code" class="row-full">
          <el-input
            v-if="!form.id"
            v-model="form.code"
            placeholder="请输入来源系统编码，支持英文字母、数字和下划线"
            maxlength="64"
            @blur="normalizeCode"
          />
          <div v-else class="form-readonly">{{ form.code }}</div>
        </qt-form-item>
        <qt-form-item label="图标" prop="icon" class="row-full"
                      tip="支持上传不超过 5MB 的 png/jpg/jpeg 图片，未上传时显示默认图标。">
          <source-icon-upload v-model="form.icon"/>
        </qt-form-item>
        <qt-form-item label="关联数据连接" prop="datasourceIds" class="row-full">
          <datasource-list
              v-model="form.datasourceIds"
              :data="datasourceOptions"
              flag="modelMaterialization"
              multiple
              multiple-tag-display
              clearable
              :validate-event="false"
              placeholder="请选择已有数据连接"
              @change="handleDatasourceChange"
          />
        </qt-form-item>
        <qt-form-item label="来源可信度" prop="trustScore" class="row-full" tip="0～100，数值越高表示来源越可信。">
          <el-input-number v-model="form.trustScore" class="trust-input" controls-position="right" :min="0" :max="100"
                           :precision="0"/>
        </qt-form-item>
        <qt-form-item label="负责人" prop="responsibleUserId" class="row-full">
          <el-select v-model="form.responsibleUserId" placeholder="请选择负责人" clearable filterable
                     class="trust-input">
            <el-option v-for="item in userOptions" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </qt-form-item>
        <qt-form-item label="联系方式" class="row-full">
          <el-input :model-value="responsiblePhone" placeholder="请输入联系方式" disabled/>
        </qt-form-item>
        <qt-form-item label="状态" prop="validFlag" class="row-full">
          <el-radio-group v-model="form.validFlag">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">停用</el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item label="描述" prop="description" class="row-full">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="256个字符"
            show-word-limit
            placeholder="请输入描述，建议说明来源系统的业务用途、数据范围等信息"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">
            确 定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SourceFormDialog">
import {
  computed,
  getCurrentInstance,
  nextTick,
  reactive,
  ref,
  toRefs,
} from "vue";

import {
  listIngestDatasourceOptions,
  getIngestSource,
  addIngestSource,
  updateIngestSource,
} from "@/api/ingest/source/source.js";
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import SourceIconUpload from "@/views/ingest/source/sourceIconUpload.vue";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";
import DatasourceList from "@/components/Datasource/List.vue";
import { validatePattern, validateTrimmedRequired, trimText } from "@/utils/validate";

const userDirectoryStore = useUserDirectoryStore();

const emit = defineEmits(["success"]);
const {proxy} = getCurrentInstance();

const open = ref(false);
const title = ref("");
const submitLoading = ref(false);
const datasourceOptions = ref([]);
const userOptions = ref([]);
const sourceRef = ref(null);

const data = reactive({
  form: {},
  rules: {
    name: [
      {
        required: true,
        validator: validateTrimmedRequired("来源系统名称不能为空", 64),
        trigger: "blur",
      },
      {
        validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "来源系统名称支持中文、英文字符、数字、下划线和短横线",
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    code: [
      {
        required: true,
        validator: validateTrimmedRequired("来源系统编码不能为空", 64),
        trigger: "blur",
      },
      {
        validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "来源系统编码支持英文字母、数字和下划线",
        ),
        trigger: "blur",
        transform: trimText,
      },
    ],
    trustScore: [
      {required: true, message: "来源可信度不能为空", trigger: "change"},
    ],
    datasourceIds: [{type: "array", required: true, min: 1, message: "请选择关联数据连接", trigger: "change"}],
  },
});

const {form, rules} = toRefs(data);

const responsiblePhone = computed(() => {
  const selected = userOptions.value.find(
      (item) => String(item.value) === String(form.value.responsibleUserId),
  );
  return selected?.phone || "";
});

/** 恢复新增默认值并重置表单校验，避免上次编辑内容残留。 */
function reset() {
  form.value = {
    id: null,
    code: null,
    name: null,
    responsibleUserId: null,
    trustScore: 50,
    icon: "",
    validFlag: "1",
    description: null,
    datasourceIds: [],
    datasourceList: [],
    usedDatasourceIds: [],
  };
  nextTick(() => sourceRef.value?.clearValidate());
}

/** 关闭弹窗并清空当前表单。 */
function cancel() {
  open.value = false;
  reset();
}

/** 提交前去除来源系统编码两端的空白。 */
function normalizeCode() {
  if (form.value.code) {
    form.value.code = form.value.code.trim();
  }
}

/**
 * 补入已关联但不在当前选项中的连接，确保修改时可以回显。
 * @param {Array<Object>} selectedList 已关联的连接记录。
 */
function mergeDatasourceOptions(selectedList) {
  const current = [...datasourceOptions.value];
  (selectedList || []).forEach((item) => {
    if (item?.id && !current.some((option) => option.id === item.id)) {
      current.push(item);
    }
  });
  datasourceOptions.value = current;
}

/**
 * 加载可选连接并合并当前已关联的连接。
 * @param {Array<Object>} [selectedList] 当前已关联连接。
 * @returns {Promise<void>} 选项加载任务。
 */
function loadDatasourceOptions(selectedList) {
  return listIngestDatasourceOptions().then((response) => {
    datasourceOptions.value = response.data || [];
    mergeDatasourceOptions(selectedList);
  });
}

/**
 * 加载负责人选项，补回列表中缺失的已选用户及联系电话。
 * @param {Object} [selectedUser] 当前来源系统的负责人信息。
 * @returns {Promise<void>} 选项加载任务。
 */
function loadUserOptions(selectedUser = {}) {
  return userDirectoryStore.loadUserList().then((response) => {
    const rows = response.rows || response.data?.rows || [];
    userOptions.value = rows
        .map((item) => {
          const label = item.nickName || item.userName;
          return label
              ? {
                label,
                value: item.userId,
                phone: item.phonenumber,
              }
              : null;
        })
        .filter(Boolean);
    const selectedValue = selectedUser.responsibleUserId;
    if (selectedValue && !userOptions.value.some((item) => String(item.value) === String(selectedValue))) {
      userOptions.value.unshift({
        label: selectedUser.responsiblePersonName || String(selectedValue),
        value: selectedValue,
        phone: selectedUser.responsiblePersonPhone,
      });
    }
  });
}

/**
 * 恢复被接入任务使用的连接关联，防止用户取消仍在使用的连接。
 * @param {Array<string|number>} value 本次选中的连接 ID。
 */
async function handleDatasourceChange(value) {
  const selectedIds = value || [];
  const usedIds = form.value.usedDatasourceIds || [];
  const missing = usedIds.filter(id => !selectedIds.some(selected => String(selected) === String(id)));
  form.value.datasourceIds = [...selectedIds, ...missing];
  if (missing.length) proxy.$modal.msgWarning("存在接入任务正在使用的数据连接，不能取消关联");
  await nextTick();
  sourceRef.value?.validateField("datasourceIds").catch(() => {
  });
}

/** 初始化默认表单及下拉选项后打开新增弹窗。 */
function openAdd() {
  reset();
  loadDatasourceOptions();
  loadUserOptions();
  open.value = true;
  title.value = "新增来源系统";
}

/**
 * 读取来源系统详情并补齐下拉回显后打开修改弹窗。
 * @param {string|number} id 来源系统 ID。
 */
function openEdit(id) {
  reset();
  getIngestSource(id).then((response) => {
    const detail = response.data || {};
    delete detail.createTime;
    delete detail.updateTime;
    form.value = {
      ...detail,
      trustScore: detail.trustScore ?? 50,
      icon: detail.icon || "",
      validFlag: String(detail.validFlag ?? "1"),
      datasourceIds: detail.datasourceIds || [],
      datasourceList: detail.datasourceList || [],
      usedDatasourceIds: detail.usedDatasourceIds || [],
    };
    loadDatasourceOptions(form.value.datasourceList);
    loadUserOptions(detail);
    open.value = true;
    title.value = "修改来源系统";
  });
}

/** 校验表单后提交来源系统配置，成功时关闭弹窗并通知父页刷新。 */
function submitForm() {
  sourceRef.value.validate((valid) => {
    if (!valid) {
      return;
    }
    normalizeCode();
    submitLoading.value = true;
    const payload = {
      id: form.value.id,
      name: form.value.name?.trim(),
      icon: form.value.icon || "",
      validFlag: form.value.validFlag,
      responsibleUserId: form.value.responsibleUserId || null,
      code: form.value.code,
      trustScore: form.value.trustScore,
      description: form.value.description,
      datasourceIds: form.value.datasourceIds || [],
    };
    const request = form.value.id ? updateIngestSource(payload) : addIngestSource(payload);
    request
        .then(() => {
          proxy.$modal.msgSuccess(form.value.id ? "修改成功" : "新增成功");
          open.value = false;
          emit("success", form.value.id);
        })
        .finally(() => {
          submitLoading.value = false;
        });
  });
}

defineExpose({openAdd, openEdit});
</script>

<style scoped lang="scss">
/* 来源系统编辑与来源数据只读表单共用的布局规则。 */
.trust-input {
  width: 100%;
}

/* 只读字段保持两列均分，允许列宽随容器收缩。 */
.source-detail-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

/* 解除表单项及内容区的最小宽度限制，防止长字段撑开整列。 */
.source-detail-form :deep(.el-form-item) {
  min-width: 0;
}

.source-detail-form :deep(.el-form-item__content) {
  min-width: 0;
}

.source-detail-form :deep(.el-form-item__label) {
  white-space: nowrap;
}

/* 长字段值在本项内横向滚动，保持标签与相邻行对齐。 */
.source-detail-form__value.form-readonly {
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  word-break: normal;
}

/* 保留普通文本的完整宽度，使超长内容能触发横向滚动。 */
.source-detail-form__value > span:not(.el-tag) {
  flex: none;
}
</style>
