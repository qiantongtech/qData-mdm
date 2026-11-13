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
      destroy-on-close
      class="dialog"
  >
    <el-form
        ref="formRef"
        :model="form"
        :rules="readonly ? undefined : rules"
        label-width="120px"
        class="column-form"
        @submit.prevent
    >
      <guide-tip-panel
          class="row-full domain-intro"
          title="模型目录用于分类组织和管理主数据模型，请先选择所属主数据域及上级目录，再维护目录信息。"
          type="primary"
          allow-wrap
      />
      <qt-form-item label="所属主数据域" prop="projectId" class="row-full">
        <div v-if="readonly" class="form-readonly">{{ project.name || '-' }}</div>
        <el-input v-else :model-value="project.name" disabled/>
      </qt-form-item>
      <qt-form-item label="上级模型目录" prop="parentId" class="row-full"
                    tip="支持最多 5 级模型目录；选择顶级节点时，默认创建为一级模型目录。">
        <div v-if="readonly" class="form-readonly">{{ parentLayerName }}</div>
        <el-tree-select
            v-else
            v-model="form.parentId"
            filterable
            :data="parentSelectOptions"
            :props="{ value: 'id', label: 'name', children: 'children' }"
            value-key="id"
            placeholder="请选择上级"
            check-strictly
        />
      </qt-form-item>
      <qt-form-item label="目录名称" prop="name" class="row-full">
        <div v-if="readonly" class="form-readonly">{{ form.name || '-' }}</div>
        <el-input v-else v-model="form.name"
                  placeholder="请输入模型目录名称，支持中文、英文字符、数字、下划线和短横线格"
        />
      </qt-form-item>
      <qt-form-item label="目录编码" prop="code" class="row-full">
        <div v-if="readonly" class="form-readonly">{{ form.code || '-' }}</div>
        <el-input v-else v-model="form.code" placeholder="请输入模型目录编码，支持英文字母、数字和下划线"/>
      </qt-form-item>
      <qt-form-item
          label="排序"
          prop="sortOrder"
          tip="同级目录按数值从小到大排列，未填写按 0 处理。"
          class="row-full"
      >
        <div v-if="readonly" class="form-readonly">{{ form.sortOrder ?? '-' }}</div>
        <el-input-number
            v-else
            v-model="form.sortOrder"
            class="full-width"
            controls-position="right"
            :min="0"
        />
      </qt-form-item>
      <qt-form-item label="负责人" prop="responsibleUserId" class="row-full">
        <div v-if="readonly" class="form-readonly">{{ getResponsibleName(form.responsibleUserId) || '-' }}</div>
        <el-select v-else v-model="form.responsibleUserId"
                   placeholder="请选择负责人"
                   filterable clearable>
          <el-option v-for="user in userOptions" :key="user.value" :label="user.label" :value="user.value"/>
        </el-select>
      </qt-form-item>
      <qt-form-item label="联系电话" class="row-full">
        <div v-if="readonly" class="form-readonly">{{ getResponsiblePhone(form.responsibleUserId) || '-' }}</div>
        <el-input v-else :model-value="getResponsiblePhone(form.responsibleUserId)" disabled
                  placeholder="请输入联系电话"
        />
      </qt-form-item>
      <qt-form-item label="描述" prop="description" class="row-full">
        <div v-if="readonly" class="form-readonly textarea">{{ form.description || '-' }}</div>
        <el-input
            v-else
            v-model="form.description"
            type="textarea"
            maxlength="256个字符"
            show-word-limit
            placeholder="请输入目录描述"
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
            :loading="saving"
            @click="submitForm"
        >确 定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="LayerDialog">
/**
 * @description 模型目录新增、编辑与只读详情弹窗。
 * @emits success 保存成功后通知父页面刷新
 * @prop {Object} project 当前主数据域信息
 */
import { useUserDirectory } from '@/store/system/userDirectory';
import {addLayer, getLayer, updateLayer} from "@/api/model/layer/layer.js";
import {trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";
import {useDict, loadDict} from "@/utils/dict";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const { responsibleUserList, getResponsibleName, getResponsiblePhone } = useUserDirectory();

const emit = defineEmits(["success"]);
const props = defineProps({
  project: {
    type: Object,
    required: true,
  },
});
const {proxy} = getCurrentInstance();
const {model_layer_max_depth} = useDict('model_layer_max_depth');
const maxLayerDepth = computed(() => {
  const depth = Number(model_layer_max_depth.value[0]?.value);
  return Number.isSafeInteger(depth) && depth > 0 ? depth : 0;
});

/** 顶级节点为第 0 级；编辑时需为整棵子树预留层数。 */
function buildLayerParentOptions(nodes = [], editingId = null) {
  const sameId = (left, right) => left != null && right != null && String(left) === String(right);
  const height = node => 1 + Math.max(0, ...(node.children || []).map(height));
  let subtreeHeight = 1;
  function findEditing(items) {
    for (const node of items) {
      if (sameId(node.id, editingId)) subtreeHeight = height(node);
      findEditing(node.children || []);
    }
  }
  findEditing(nodes);
  function build(items, parentDepth, insideEditing) {
    return items.map(node => {
      const depth = String(node.id) === '0' ? 0 : parentDepth + 1;
      const blocked = insideEditing || sameId(node.id, editingId);
      return {
        ...node,
        disabled: blocked || depth + subtreeHeight > maxLayerDepth.value,
        children: build(node.children || [], depth, blocked),
      };
    });
  }
  return build(nodes, 0, false);
}

function findLayerOption(nodes, id) {
  for (const node of nodes || []) {
    if (String(node.id) === String(id)) return node;
    const found = findLayerOption(node.children, id);
    if (found) return found;
  }
  return null;
}

const visible = ref(false);
const readonly = ref(false);
const title = ref("");
const saving = ref(false);
const formRef = ref();
const emptyForm = () => ({
  id: null,
  projectId: null,
  parentId: props.project?.id,
  code: null,
  name: null,
  responsibleUserId: null,
  sortOrder: 0,
  description: null,
});
const form = ref(emptyForm());
const userOptions = computed(() => {
  const users = responsibleUserList.value;
  const selected = form.value.responsibleUserId;
  // 历史负责人可能已不在公共列表中，保留一个选项，编辑时不丢失原选择。
  if (selected && !users.some(user => user.value === String(selected))) {
    return [{value: String(selected), label: `用户 ${selected}`, phone: ''}, ...users];
  }
  return users;
});
const rules = {
  parentId: [{
    validator: (_rule, value, callback) => {
      const parent = findLayerOption(parentSelectOptions.value, value);
      if (!parent) return callback(new Error("请选择上级模型目录"));
      if (parent.disabled) return callback(new Error(`请选择有效的上级目录，模型目录及子目录最多支持 ${maxLayerDepth.value} 级`));
      callback();
    },
    trigger: "change",
  }],
  code: [
    {required: true, validator: validateTrimmedRequired("目录编码不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "目录编码支持英文字母、数字和下划线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
  name: [
    {required: true, validator: validateTrimmedRequired("目录名称不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "目录名称支持中文、英文字符、数字、下划线和短横线格"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
};

/** 重置模型目录表单并清除校验结果。 */
function reset() {
  form.value = emptyForm();
  formRef.value?.clearValidate();
}

const layerTreeOptions = ref([]);
const parentSelectOptions = computed(() => buildLayerParentOptions(layerTreeOptions.value, form.value.id));
const parentLayerName = computed(() => {
  const findNode = (nodes) => {
    for (const node of nodes || []) {
      if (String(node.id) === String(form.value.parentId)) return node;
      const matched = findNode(node.children);
      if (matched) return matched;
    }
    return null;
  };
  return findNode(layerTreeOptions.value)?.name || form.value.parentName || "-";
});

/** 初始化并打开弹窗，按传入记录加载详情。 */
async function openDialog(row) {
  model_layer_max_depth.value = await loadDict('model_layer_max_depth');
  if (!maxLayerDepth.value) {
    proxy.$modal.msgError('请先配置模型分层最大深度字典');
    return;
  }
  reset();
  readonly.value = Boolean(row.readonly);
  layerTreeOptions.value = row.layerTreeOptions || [];
  if (row?.id != null) {
    title.value = row.title;
    const response = await getLayer(row.id);
    // 下拉选项用字符串 ID，回填也统一成字符串，避免有值却显示不出姓名。
    form.value = {
      ...response.data,
      responsibleUserId: response.data.responsibleUserId == null ? null : String(response.data.responsibleUserId),
    };
  } else {
    title.value = row.title;
    form.value.parentId = row?.parentId ?? 0;
  }
  visible.value = true;
}

/** 校验表单并保存当前记录，成功后通知父页面刷新。 */
async function submitForm() {
  if (readonly.value) return;
  form.value.projectId = props.project.id;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  saving.value = true;
  try {
    const editing = form.value.id != null;
    // 清空选择后提交 null，不把下拉框的空字符串当成用户 ID。
    const payload = {...form.value, responsibleUserId: form.value.responsibleUserId || null};
    await (editing ? updateLayer(payload) : addLayer(payload));
    proxy.$modal.msgSuccess(editing ? "修改成功" : "新增成功");
    visible.value = false;
    emit("success");
  } finally {
    saving.value = false;
  }
}

defineExpose({openDialog});
</script>

<style scoped lang="scss">
.full-width {
  width: 100%;
}
</style>
