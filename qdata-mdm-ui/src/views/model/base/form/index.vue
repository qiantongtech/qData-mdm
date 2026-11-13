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
  <div ref="app-container" v-loading="submitting" class="app-container model-form-page">
    <div class="model-form-header">
      <div class="model-form-header-info">
        <div class="model-form-title">{{ form.id || route.query.id ? '修改' : '新增' }}{{ form.name || '模型' }}</div>
        <dict-tag v-if="form.version?.status != null" :options="model_status"
                  :value="form.version?.status"
        />
        <dict-tag
            class="ml4 pointer model-form-version"
            :options="getVersionTagOptions(form)"
            :value="form.version?.status"
            @click="handleDetail(form, 'versions')"
        />
        <span class="model-form-header-tip">模型配置完成后按步骤执行校验与发布</span>
      </div>
      <div class="data-actions">
        <el-button
            :disabled="!showModelGraph"
            plain
            type="primary"
            @click="openModelGraph"
        >
          <template #icon>
            <svg-icon icon-class="relationship-graph"/>
          </template>
          查看单模型图谱
        </el-button>
        <el-button :loading="submitting" :disabled="!detailLoaded" @click="saveDraft">保存草稿</el-button>
      </div>
    </div>
    <div class="wizard-steps mb15">
      <Steps :steps="stepsList" :active-step="currentStep" :clickable="false"/>
    </div>

    <div class="wizard-card" :class="currentStep === 2 ? 'sub-pagecont-top' : 'pagecont-top'">
      <div class="infotop">
        <div class="wizard-content">
          <!-- 第一步：动态渲染模型基础信息。 -->
          <el-form
              ref="basicFormRef"
              v-show="currentStep === 0"
              :model="form"
              :rules="basicRules"
              label-width="100px"

              class="column-form basic-form wizard-scroll"
              @submit.prevent
          >
            <div class="h2-titles row-full">基础信息</div>
            <qt-form-item label="模型名称" prop="name">
              <el-input v-model="form.name"
                        placeholder="请输入模型名称，支持中文、英文字符、数字、下划线和短横线"
                        @change="markValidationPending"
              />
            </qt-form-item>
            <qt-form-item label="模型编码" prop="code">
              <el-input
                  v-model="form.code"
                  placeholder="请输入模型编码，支持英文字母、数字和下划线"
                  :disabled="modelStructureReadonly(form)"
                  @change="markValidationPending"
              />
            </qt-form-item>
            <qt-form-item label="主数据域" prop="projectId" tip="先选择主数据域，再选择域下模型目录">
              <el-select
                  v-model="form.projectId"
                  placeholder="请选择主数据域"
                  clearable
                  @change="handleProjectChange"
              >
                <template #label="{ label }">
                  <div v-if="selectedProject" class="task-title-row">
                    <img :src="getProjectIcon(selectedProject.icon)" alt="" class="info-icon"/>
                    <span class="text-ellipsis">{{ selectedProject.name }}</span>
                  </div>
                  <span v-else>{{ label }}</span>
                </template>
                <el-option
                    v-for="item in projects"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                >
                  <div class="task-title-row">
                    <img :src="getProjectIcon(item.icon)" alt="" class="info-icon"/>
                    <span class="text-ellipsis">{{ item.name }}</span>
                  </div>
                </el-option>
              </el-select>
            </qt-form-item>
            <qt-form-item label="模型目录" prop="layerId">
              <el-tree-select
                  v-model="form.layerId"
                  :data="layers"
                  :props="layerTreeProps"
                  :placeholder="form.projectId ? '请选择模型目录' : '请先选择主数据域'"
                  :disabled="!form.projectId"
                  check-strictly
                  clearable
                  @change="markValidationPending"
              />
            </qt-form-item>
            <qt-form-item label="模型类型" prop="type">
              <el-select
                  v-model="form.type"
                  :disabled="modelStructureReadonly(form)"
                  placeholder="请选择模型类型"
                  clearable
                  @change="markValidationPending"
              >
                <el-option
                    v-for="item in model_type"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                />
              </el-select>
            </qt-form-item>
            <!-- 默认图标只用于展示，保存时仍传空路径，之后切换模型类型才能自动换图标。 -->
            <qt-form-item label="图标" prop="icon"
                          tip="请上传大小不超过 <span class='text-danger'>5MB</span> 格式为 <span class='text-danger'>png/jpg/jpeg</span> 的图片，未上传时按模型类型展示默认图标。">
              <div class="model-icon-field">
                <model-icon-upload
                    v-model="form.icon"
                    :type="form.type"
                    @update:model-value="markValidationPending"
                />
              </div>
            </qt-form-item>
            <qt-form-item label="负责人" prop="responsibleUserId">
              <el-select v-model="form.responsibleUserId"
                         placeholder="请选择负责人"
                         filterable clearable
                         @change="markValidationPending"
              >
                <el-option v-for="user in responsibleUserOptions" :key="user.value" :label="user.label"
                           :value="user.value"
                />
              </el-select>
            </qt-form-item>
            <qt-form-item label="联系电话">
              <el-input :model-value="getResponsiblePhone(form.responsibleUserId)" disabled
                        placeholder="请输入联系电话"
              />
            </qt-form-item>
            <qt-form-item label="当前状态" prop="status">
              <el-input :model-value="selectDictLabel(model_status, form.version?.status)" disabled/>
            </qt-form-item>
            <qt-form-item label="描述" prop="description" class="row-full">
              <el-input
                  v-model="form.description"
                  type="textarea"
                  maxlength="256个字符"
                  show-word-limit
                  :min-height="192"
                  placeholder="请输入描述"
                  @change="markValidationPending"
              />
            </qt-form-item>
          </el-form>

          <!-- 第二步：维护主模型字段。 -->
          <section v-show="currentStep === 1" class="wizard-section wizard-scroll">
            <model-fields-editor
                ref="modelFieldsEditorRef"
                v-model="modelFields"
                title="模型字段"
                :data-type-options="fieldMeta.dataTypes"
                :reference-options="fieldMeta.references"
                :project-options="projects"
                :current-model-id="form.id"
                :published="publishedRevision"
                :original-fields="originalFieldsFor(form)"
                :published-fields="publishedFieldsFor(form)"
                @change="markValidationPending"
            />
          </section>

          <!-- 第三步：维护子模型，并复用模型字段组件。 -->
          <section v-show="currentStep === 2" class="wizard-section submodel-step">
            <div class="submodel-workspace">
              <aside class="submodel-sidebar">
                <div class="submodel-sidebar-header">
                  <span class="el-tabs__item">子模型列表</span>
                  <el-button
                      v-hasPermi="['model:base:add']"
                      type="primary"
                      plain
                      size="small"
                      icon="Plus"
                      @click="openSubModelDialog"
                  >新增子模型
                  </el-button>
                </div>
                <div v-if="subModels.length" class="submodel-card-list">
                  <el-row
                      v-for="model in subModels"
                      :key="model.localId"
                      class="submodel-card"
                      :class="{ 'is-active': activeSubModelId === model.localId }"
                      align="middle"
                  >
                    <el-col :span="21">
                      <button
                          type="button"
                          class="submodel-card-select"
                          :aria-pressed="activeSubModelId === model.localId"
                          @click="selectSubModel(model)"
                      >
                        <img :src="getModelIcon(model, true)" class="submodel-icon" alt=""/>
                        <el-text truncated :title="model.name || '未命名子模型'">{{
                            model.name || '未命名子模型'
                          }}
                        </el-text>
                      </button>
                    </el-col>
                    <el-col :span="3">
                      <el-button
                          v-hasPermi="['model:base:remove']"
                          link
                          type="danger"
                          icon="Delete"
                          :aria-label="`删除子模型${model.name || ''}`"
                          title="删除子模型"
                          @click.stop="removeSubModel(model.localId)"
                      />
                    </el-col>
                  </el-row>
                </div>
                <div v-else class="emptyBg submodel-empty">
                  <img class="empty-state-image" alt=""/>
                  <p>暂无子模型</p>
                </div>
              </aside>
              <el-main class="submodel-main-content">
                <div v-if="activeSubModel" class="submodel-content">
                  <el-tabs v-model="subModelTab" class="submodel-tabs">
                    <qt-tab-pane label="基本信息" name="basic">
                      <el-form
                          ref="subModelBasicFormRef"
                          :model="activeSubModel"
                          :rules="subModelRules"
                          label-width="130px"
                          class="column-form"
                          @submit.prevent
                      >
                        <qt-form-item label="子模型名称" prop="name">
                          <el-input v-model="activeSubModel.name"
                                    placeholder="请输入子模型名称，支持中文、英文字符、数字、下划线和短横线"
                                    @change="markValidationPending"
                          />
                        </qt-form-item>
                        <qt-form-item label="子模型编码" prop="code">
                          <el-input
                              v-model="activeSubModel.code"
                              :disabled="modelStructureReadonly(activeSubModel)"
                              placeholder="请输入子模型编码，支持英文字母、数字和下划线"
                              @change="markValidationPending"
                          />
                        </qt-form-item>
                        <qt-form-item label="所属主模型">
                          <el-input :model-value="`${form.name || '未命名模型'}（${form.code || '-'}）`" disabled/>
                        </qt-form-item>
                        <qt-form-item label="主子关系">
                          <el-input model-value="1:N" disabled/>
                        </qt-form-item>
                        <qt-form-item label="图标" prop="icon" class="row-full">
                          <model-icon-upload v-model="activeSubModel.icon" is-sub-model
                                             @update:model-value="markValidationPending"
                          />
                        </qt-form-item>
                        <qt-form-item label="子模型说明" class="row-full">
                          <el-input
                              v-model="activeSubModel.description"
                              type="textarea"
                              maxlength="256个字符"
                              show-word-limit
                              placeholder="请输入子模型说明"
                              :rows="5"
                              @change="markValidationPending"
                          />
                        </qt-form-item>
                      </el-form>
                    </qt-tab-pane>
                    <qt-tab-pane :label="model_column_category.find(item => item.value === 'BUSINESS')?.label || ''"
                                 :name="'BUSINESS'">
                      <model-fields-editor
                          ref="subModelFieldsEditorRef"
                          v-model="activeSubModel.fields"
                          compact
                          :initial-kind="'BUSINESS'"
                          :allowed-kinds="['BUSINESS']"
                          :data-type-options="fieldMeta.dataTypes"
                          :reference-options="fieldMeta.references"
                          :project-options="projects"
                          :current-model-id="activeSubModel.id"
                          :published="publishedRevision"
                          :original-fields="originalFieldsFor(activeSubModel)"
                          :published-fields="publishedFieldsFor(activeSubModel)"
                          @change="markValidationPending"
                      />
                    </qt-tab-pane>
                    <qt-tab-pane :label="model_column_category.find(item => item.value === 'RELATION')?.label || ''"
                                 :name="'RELATION'">
                      <model-fields-editor
                          ref="subModelRelationEditorRef"
                          v-model="activeSubModel.fields"
                          compact
                          :initial-kind="'RELATION'"
                          :allowed-kinds="['RELATION']"
                          :data-type-options="fieldMeta.dataTypes"
                          :reference-options="fieldMeta.references"
                          :project-options="projects"
                          :current-model-id="activeSubModel.id"
                          :published="publishedRevision"
                          :original-fields="originalFieldsFor(activeSubModel)"
                          :published-fields="publishedFieldsFor(activeSubModel)"
                          @change="markValidationPending"
                      />
                    </qt-tab-pane>
                    <qt-tab-pane :label="model_column_category.find(item => item.value === 'SYSTEM')?.label || ''"
                                 :name="'SYSTEM'">
                      <model-fields-editor
                          v-model="activeSubModel.fields"
                          compact
                          readonly
                          :initial-kind="'SYSTEM'"
                          :allowed-kinds="['SYSTEM']"
                          :data-type-options="fieldMeta.dataTypes"
                          :reference-options="fieldMeta.references"
                          :project-options="projects"
                      />
                    </qt-tab-pane>
                  </el-tabs>
                </div>
                <div v-else class="emptyBg submodel-empty">
                  <img class="empty-state-image" alt=""/>
                  <p>暂无子模型</p>
                </div>
              </el-main>
            </div>
          </section>

          <!-- 第四步：展示并执行模型校验。 -->
          <section v-show="currentStep === 3" class="wizard-section validation-step wizard-scroll">
            <div class="section-heading validation-heading">
              <div>
                <span class="section-title inline-title">模型校验</span>
                <span class="section-description">正式保存前必须全部通过模型校验</span>
              </div>
              <div>
                <el-button
                    v-if="showModelGraph"
                    plain
                    type="primary"
                    icon="Connection"
                    @click="openModelGraph"
                >
                  查看单模型图谱
                </el-button>
                <el-button
                    type="primary"
                    icon="Check"
                    :loading="validating"
                    @click="runValidation"
                >执行校验
                </el-button>
              </div>
            </div>

            <div class="validation-summary" :class="validationPassed ? 'is-passed' : 'is-pending'">
              <div class="validation-score">{{ passedValidationCount }}/{{ validationItems.length }}</div>
              <div>
                <strong>{{ validationPassed ? '模型校验通过' : '等待执行模型校验' }}</strong>
                <p>{{
                    validationPassed ? `最近校验：${lastValidatedAt} · 可保存模型配置` : '完成配置后执行校验，校验通过后方可保存'
                  }}</p>
              </div>
            </div>

            <div class="validation-list">
              <div v-for="item in validationItems" :key="item.key" class="validation-item">
                <span><el-icon><check/></el-icon>{{ item.label }}</span>
                <strong :class="item.status">{{
                    {
                      passed: '通过',
                      failed: '未通过',
                      pending: '待校验'
                    }[item.status] || '待校验'
                  }}</strong>
              </div>
            </div>
          </section>
        </div>
      </div>
      <!-- 操作区位于滚动内容下方。 -->
      <div class="button-style">
        <el-button @click="returnToList">返回列表</el-button>
        <el-button v-if="currentStep > 0" @click="handlePrevStep">上一步</el-button>
        <el-button
            v-if="currentStep < stepsList.length - 1"
            type="primary"
            :disabled="!detailLoaded"
            @click="handleNextStep"
        >下一步
        </el-button>
        <el-button
            v-if="currentStep === stepsList.length - 1"
            type="primary"
            :loading="submitting"
            @click="submitForm"
        >保存并退出
        </el-button>
      </div>
    </div>

    <!--    新增子模型弹窗-->
    <el-dialog
        v-model="subModelDialogVisible"
        title="新增子模型"
        width="800px"
        append-to-body
        draggable
        destroy-on-close
        class="dialog"
        :append-to="$refs['app-container']"
    >
      <el-form
          ref="subModelDialogFormRef"
          :model="subModelDraft"
          :rules="subModelRules"
          label-width="100px"
          class="column-form"
          @submit.prevent="confirmAddSubModel"
      >
        <guide-tip-panel
            class="row-full domain-intro"
            title="子模型依附当前主模型，固定形成 1:N 主子关系。本期支持普通字段、关联字段（仅普通模型）和系统字段，不开放分类字段。"
            type="primary"
            allow-wrap
        />
        <qt-form-item label="子模型名称" prop="name" class="row-full">
          <el-input v-model="subModelDraft.name" placeholder="请输入子模型名称，支持中文、英文字符、数字、下划线和短横线"
                    maxlength="64"/>
        </qt-form-item>
        <qt-form-item label="子模型编码" prop="code" class="row-full">
          <el-input v-model="subModelDraft.code" placeholder="请输入子模型编码，支持英文字母、数字和下划线"
                    maxlength="64"/>
        </qt-form-item>
        <qt-form-item label="图标" prop="icon" class="row-full" tip="未上传时使用默认子模型图标。">
          <model-icon-upload v-model="subModelDraft.icon" is-sub-model/>
        </qt-form-item>
        <qt-form-item label="描述" prop="description" class="row-full">
          <el-input
              v-model="subModelDraft.description"
              type="textarea"
              :rows="5"
              maxlength="256个字符"
              show-word-limit
              placeholder="请输入子模型描述"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="subModelDialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="confirmAddSubModel">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
        v-model="modelGraphDialog.visible"
        title="单模型图谱"
        width="1200px"
        append-to-body
        draggable
        destroy-on-close
        class="dialog"
        :append-to="$refs['app-container']"
        @closed="resetModelGraphDialog"
    >
      <model-relation-graph
          v-if="modelGraphDialog.visible && modelGraphDialog.modelId"
          :model-id="modelGraphDialog.modelId"
          height="640px"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="modelGraphDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="BaseFormDialog">

import {useDict, loadDict} from "@/utils/dict";

/** @description 主数据模型向导，负责基础信息、字段及子模型配置。 */
import {Check} from "@element-plus/icons-vue";

import {getPublishedModelStructure} from "@/api/model/base/published";
import {provide} from "vue";
import {ElMessageBox} from "element-plus";
import {addBase, getBase, updateBase} from "@/api/model/base/base";
import {treeProject} from "@/api/model/project/project";
import {treeLayer} from "@/api/model/layer/layer";
import ModelFieldsEditor from "./components/modelFieldsEditor.vue";
import ModelRelationGraph from "@/views/model/components/ModelRelationGraph/index.vue";
import Steps from "@/components/Steps/index.vue";
import {getModelIcon, getProjectIcon} from "@/utils/imageUrl";
import {useUserDirectory} from '@/store/system/userDirectory';
import ModelIconUpload from "./components/modelIconUpload.vue";
import {trimText, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";
import GuideTipPanel from "@/components/GuideTip/GuideTipPanel.vue";

const {responsibleUserList, getResponsiblePhone} = useUserDirectory();

// 同一次输入失焦和点击保存可能同时触发检查，共用弹窗，避免连续询问两遍。
const pendingRestorations = new Map();

/** 找到已发布、但已从当前字段列表删除的同编码字段。 */
function findRestorableField(field, fields, publishedFields) {
  const code = String(field.code ?? "").trim();
  if (!code) return null;
  return publishedFields.find(old => old.id != null && String(old.code).trim() === code
      && String(old.id) !== String(field.id)
      && !fields.some(item => item.id != null && String(item.id) === String(old.id))) || null;
}

/** 确认后恢复原ID和原配置；取消时阻止本次确认或保存，不把同名字段作为新字段提交。 */
async function confirmFieldRestoration(field, fields, publishedFields) {
  const original = findRestorableField(field, fields, publishedFields);
  if (!original) return {field, restored: false, cancelled: false};
  const key = `${original.id}:${field.localId}`;
  let pending = pendingRestorations.get(key);
  if (!pending) {
    pending = ElMessageBox.confirm("字段已发布，是否恢复字段？", "提示", {
      confirmButtonText: "恢复字段",
      cancelButtonText: "取消",
      type: "warning",
      closeOnClickModal: false,
    }).then(() => true, () => false);
    pendingRestorations.set(key, pending);
  }
  let accepted;
  try {
    accepted = await pending;
  } finally {
    if (pendingRestorations.get(key) === pending) pendingRestorations.delete(key);
  }
  if (!accepted) return {field, restored: false, cancelled: true};
  return {
    // 弹窗新增字段可能还没有行标识，恢复时用原ID补齐，保证表格能正确定位这一行。
    field: {...JSON.parse(JSON.stringify(original)), localId: field.localId || `field-${original.id}`, delFlag: false},
    restored: true,
    cancelled: false,
  };
}

// 字段编辑器和新增弹窗共用恢复逻辑，避免输入失焦和保存时重复询问。
provide("modelFieldRestoration", confirmFieldRestoration);

const {
  model_type,
  model_status,
  model_field_data_type,
  model_field_node_rule,
  model_field_display_mode,
  model_system_field_code,
  model_column_category
} = useDict('model_type', 'model_status', 'model_field_data_type', 'model_field_node_rule', 'model_field_display_mode', 'model_system_field_code', 'model_column_category');
const nodeRuleLabel = value => model_field_node_rule.value.find(item => item.value === value)?.label || '';
const displayModeLabel = value => model_field_display_mode.value.find(item => item.value === value)?.label || '';

const {proxy} = getCurrentInstance();

const route = useRoute();

/** 沿用状态字典的标签样式，展示文字使用发布版本号。 */
function getVersionTagOptions(row) {
  return model_status.value.map(item => ({
    ...item,
    label: row.releaseVersion?.versionNo || row.version?.versionNo || '尚未发布',
  }));
}

/** 点击版本标签进入版本管理，新增模型保存前不跳转。 */
function handleDetail(row, tab) {
  const id = row?.id || route.query.id;
  if (!id) return;
  proxy.$router.push({
    path: "/model/base/detail",
    query: {id, ...(tab ? {tab} : {})},
  });
}

/** 将接口布尔标记统一为真正的 Boolean，兼容旧数据中的 0/1 字符串。 */
function toBooleanFlag(value) {
  return value === true || value === 1 || value === "1" || value === "true";
}

/** 将模型节点规则编码转换为展示名称。 */
function getNodeRuleLabel(value) {
  return nodeRuleLabel(value) || "";
}

/** 将字段展示模式编码转换为展示名称。 */
function getDisplayModeLabel(value) {
  return displayModeLabel(value) || "";
}

/** 向导步骤定义，顺序与页面内容、底部操作按钮保持一致。 */
const stepsList = [
  {title: "基础信息", id: 0},
  {title: "模型字段", id: 1},
  {title: "子模型", id: 2},
  /*{title: "模型校验", id: 3},*/
];
/** 当前激活步骤，从 0 开始。 */
const currentStep = ref(0);
/** 基础信息表单实例，用于进入下一步前执行校验。 */
const basicFormRef = ref();
/** 主模型字段编辑器实例，用于进入下一步前执行普通字段校验。 */
const modelFieldsEditorRef = ref();
/** 当前子模型的基础表单和普通字段编辑器实例。 */
const subModelBasicFormRef = ref();
const subModelFieldsEditorRef = ref();
const subModelRelationEditorRef = ref();
const subModelDialogVisible = ref(false);
const subModelDialogFormRef = ref();
const subModelDraft = reactive({name: "", code: "", icon: "", description: ""});
/** 保存与校验按钮的独立加载状态，避免重复提交。 */
const submitting = ref(false);
// 字典初始化和详情回填成功后再允许保存，避免提交缺少系统字段的配置。
const detailLoaded = ref(false);
// 独立保存打开页面时的配置，只用于表单修改规则，不用于禁用整页操作。
const originalModel = ref(null);
/** 单独保留成功发布的字段，删除并保存草稿后仍可识别同名字段。 */
const publishedStructure = ref(null);

// 分别按主模型、子模型ID查找原始配置，新加的子模型没有原记录。
function originalNode(model) {
  if (model?.id == null || !originalModel.value) {
    return null;
  }
  return [originalModel.value, ...(originalModel.value.subModels || [])]
      .find(item => item.id != null && String(item.id) === String(model.id)) || null;
}

// 只用成功发布的字段判断修改限制。保存草稿产生的字段ID不代表已经发布，仍按新增字段编辑。
function originalFieldsFor(model) {
  return publishedFieldsFor(model);
}

/** 按所属模型取成功发布字段，主子模型不能互相恢复字段。 */
function publishedFieldsFor(model) {
  if (model?.id == null || !publishedStructure.value) return [];
  const structure = publishedStructure.value;
  if (String(model.id) === String(structure.modelId)) return structure.fields || [];
  return structure.subModels?.find(item => String(item.id) === String(model.id))?.fields || [];
}

// 已发布模型的编码和类型不能修改，名称、描述仍可编辑。
function modelStructureReadonly(model) {
  return publishedRevision.value && !!originalNode(model);
}

const validating = ref(false);
/** 单模型图谱弹窗状态及当前展示的模型 ID。 */
const modelGraphDialog = reactive({visible: false, modelId: null});
/** 主数据域及其模型目录选项，由后端接口加载。 */
const projects = ref([]);
const layers = ref([]);

/** 父目录只用于展开层级，模型只能配置到没有子目录的叶子节点。 */
const layerTreeProps = {
  value: "id",
  label: "name",
  children: "children",
  disabled: data => Array.isArray(data.children) && data.children.length > 0,
};
/** 页面正在编辑的字段和子模型。 */
const modelFields = ref([]);
const subModels = ref([]);
/** 当前选中的子模型及子模型内部页签。 */
const activeSubModelId = ref(null);
const subModelTab = ref("basic");
/** 模型校验项、最近校验时间及字段基础选项。 */
const validationItems = ref([]);
const lastValidatedAt = ref("");
const fieldMeta = reactive({dataTypes: model_field_data_type, references: []});

/** 创建基础信息默认值，新增初始化和编辑数据合并时共用。 */
const createEmptyForm = () => ({
  id: null,
  name: null,
  code: null,
  projectId: null,
  layerId: null,
  type: 'NORMAL',
  icon: "",
  responsibleUserId: null,
  status: "草稿",
  versionId: null,
  description: null,
  version: {
    status: 0
  }
});
const form = reactive(createEmptyForm());
/** 从当前域选项查找已选域，兼容历史记录中的字符串 ID。 */
const selectedProject = computed(() => projects.value.find(item => String(item.id) === String(form.projectId)));
const responsibleUserOptions = computed(() => {
  const users = responsibleUserList.value;
  const selectedId = form.responsibleUserId;
  // 历史负责人可能已不在用户列表中，补一个选项，让编辑时仍能看见原来的 ID。
  if (selectedId != null && !users.some(user => user.value === String(selectedId))) {
    return [{value: String(selectedId), label: `用户 ${selectedId}（已不可用）`, phone: ''}, ...users];
  }
  return users;
});
// 存在正式版本或发布记录时，启用已发布模型的表单修改规则。
const publishedRevision = computed(() => Boolean(form.baseVersionId || form.version?.baseVersionId
    || form.releaseVersion?.id || form.publishedInstanceId || [1, 2, 3].map(String).includes(String(form.version?.status))));
const showModelGraph = computed(() => Boolean(form.id || route.query.id));

/** 第一步基础信息校验规则。 */
const basicRules = {
  code: [
    {required: true, validator: validateTrimmedRequired("模型编码不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "模型编码支持英文字母、数字和下划线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
  name: [
    {required: true, validator: validateTrimmedRequired("模型名称不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "模型名称支持中文、英文字符、数字、下划线和短横线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
  projectId: [{required: true, message: "主数据域不能为空", trigger: "change"}],
  layerId: [{required: true, message: "模型目录不能为空", trigger: "change"}],
  type: [{required: true, message: "模型类型不能为空", trigger: "change"}],
};

/** 子模型名称和编码沿用主模型相同的必填、长度及字符格式约束。 */
const subModelRules = {
  code: [
    {required: true, validator: validateTrimmedRequired("子模型编码不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[A-Za-z0-9_]+$/,
          "子模型编码支持英文字母、数字和下划线"),
      trigger: "blur",
      transform: trimText,
    },
  ],
  name: [
    {required: true, validator: validateTrimmedRequired("子模型名称不能为空", 64), trigger: "blur"},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fa5A-Za-z0-9_-]+$/,
          "子模型名称支持中文、英文字符、数字、下划线和短横线"
      ),
      trigger: "blur",
      transform: trimText,
    },
  ],
};

/** 当前子模型详情。 */
const activeSubModel = computed(() => subModels.value.find(item => item.localId === activeSubModelId.value));
/** 已通过校验数量及整体验证结果。 */
const passedValidationCount = computed(() => validationItems.value.filter(item => item.status === "passed").length);
const validationPassed = computed(() => validationItems.value.length > 0 && passedValidationCount.value === validationItems.value.length);

/** 等待字段字典后再创建系统字段；下拉选项持续绑定字典。 */
async function loadFieldMeta() {
  const [dataTypes, systemFields] = await Promise.all([
    loadDict('model_field_data_type'), loadDict('model_system_field_code'),
  ]);
  if (!dataTypes.length || !systemFields.some(item => item.value === 'ID')) {
    proxy.$modal.msgError('模型字段字典不完整，请检查数据类型和系统字段编码字典');
    throw new Error('模型字段字典不完整');
  }
  model_field_data_type.value = dataTypes;
  model_system_field_code.value = systemFields;
  // 参考数据下拉的空值选项固定排在首位，新增字段默认即为“不关联”。
  fieldMeta.references = [{id: null, name: "不关联"}];
}

const systemFieldLengths = {
  STRING: 64,
  TEXT: 500,
  DATETIME: 36,
};

/**
 * 根据系统字段字典生成预置字段，物理类型和主子关系由页面维护。
 * @param {string} idPrefix 本地行标识前缀，用于主模型与各子模型之间隔离 row-key
 */
function createSystemFields(idPrefix = "system-field", isSubModel = false) {
  const fields = [
    ['ID', 'INTEGER', "平台内部稳定唯一标识"],
    ['DATA_SOURCE', 'STRING', "标识平台录入、多源融合等数据形成方式"],
    ['CREATED_BY', 'STRING', "创建人名称"],
    ['CREATED_TIME', 'DATETIME', "创建时间"],
    ['CREATED_BY_ID', 'STRING', "创建人ID"],
    ['UPDATED_BY', 'STRING', "更新人名称"],
    ['UPDATED_BY_ID', 'STRING', "更新人ID"],
    ['UPDATED_TIME', 'DATETIME', "最近更新时间"],
    ['REMARK', 'TEXT', "系统通用备注信息"],
    ['STATUS', 'STRING', "生效、冻结、失效、归档"],
  ];
  if (isSubModel) {
    // 在主键ID后插入父级ID，不删除已有字段。
    fields.splice(1, 0, [
      'PARENT_ID',
      'INTEGER',
      "所属主模型数据ID",
    ]);
  }
  if (form.type === 'CATEGORY') {
    // 有父级ID时排在它后面，否则直接排在主键ID后面。
    fields.splice(isSubModel ? 2 : 1, 0, [
      'CATEGORY_ID',
      'INTEGER',
      "所属分类ID",
    ]);
  }
  return fields.filter(([code]) => model_system_field_code.value.some(item => item.value === code))
      .map(([code, dataType, description], index) => ({
        localId: `${idPrefix}-${index + 1}`,
        columnCategory: 'SYSTEM',
        // 通过主键编码设置约束，调整展示顺序时也不会把其他系统字段误当主键。
        primaryKeyFlag: code === 'ID',
        requiredFlag: code === 'ID',
        uniqueFlag: code === 'ID',
        nameFlag: false,
        name: model_system_field_code.value.find(item => item.value === code)?.label || code,
        code,
        dataType,
        dataLength: systemFieldLengths[dataType] ?? null,
        description,
        readonly: true,
      }));
}

/** 补齐预置字段，并统一用大写编码展示和提交；业务字段保持用户输入。 */
function ensureSystemFields(fields = [], idPrefix = "system-field", isSubModel = false) {
  const normalizedFields = Array.isArray(fields) ? fields : [];
  const existingSystemFields = new Map(
      normalizedFields
          .filter(field => field.columnCategory === 'SYSTEM')
          // 大小写统一后匹配，防止回填历史配置时重复补入同一个预制字段。
          .map(field => [String(field.code ?? "").toUpperCase(), field])
  );
  const businessFields = normalizedFields.filter(field => field.columnCategory !== 'SYSTEM');
  const systemFields = createSystemFields(idPrefix, isSubModel).map(defaultField => {
    const existingField = existingSystemFields.get(defaultField.code);
    if (!existingField) {
      return defaultField;
    }
    existingSystemFields.delete(defaultField.code);
    const field = {
      ...defaultField,
      ...existingField,
      code: defaultField.code,
      readonly: true,
    };
    // 主模型、子模型的系统主键统一为INTEGER。
    if (defaultField.code === 'ID') {
      Object.assign(field, {dataType: 'INTEGER', dataLength: null});
    }
    return field;
  });
  // 保留后端扩展字段；普通模型的表单不展示分类ID。
  const extraSystemFields = [...existingSystemFields.values()]
      .filter(field => !(String(field.code ?? "").toUpperCase() === 'CATEGORY_ID' && form.type !== 'CATEGORY'))
      .map(field => ({
        ...field,
        code: String(field.code ?? "").toUpperCase(),
        readonly: true,
      }));
  return [...businessFields, ...systemFields, ...extraSystemFields];
}

// 表单切换模型类型时，主模型及全部子模型同步展示对应的系统字段。
watch(() => form.type, () => {
  modelFields.value = ensureSystemFields(modelFields.value);
  subModels.value.forEach(model => {
    model.fields = ensureSystemFields(model.fields, `${model.localId}-system-field`, true);
  });
});

/** 加载主模型字段，预留模型字段列表接口接入位置。 */
async function loadModelFields() {
  // 模型字段查询接口可用后，用接口数据替换当前示例业务字段，并继续合并固定系统字段。
  modelFields.value = [
    ...createSystemFields(),
  ];
}

/** 加载子模型及其字段，预留子模型详情接口接入位置。 */
async function loadSubModels() {
  subModels.value = [];
  // 默认选中首个子模型；接口返回空数组时保持空态。
  activeSubModelId.value = subModels.value[0]?.localId ?? null;
}

/** 初始校验结果全部标成待检查，不能打开页面就显示通过。 */
async function loadValidationItems() {
  // 这里只列出浏览器实际执行的检查，不把数据库或后端检查显示成待完成任务。
  validationItems.value = [{key: 'local', label: '完整主子模型前端校验', status: 'pending'}];
}

/** 查询全部主数据域，供基础信息下拉框使用。 */
async function loadProjects() {
  const response = await treeProject();
  projects.value = response.data || [];
}

/**
 * 根据主数据域查询模型目录，并转换为树形结构。
 * @param {string|number|null} projectId 主数据域 ID
 */
async function loadLayers(projectId) {
  // 未选择主数据域时清空目录，避免保留上一主数据域的目录数据。
  layers.value = [];
  if (!projectId) {
    return;
  }
  const response = await treeLayer({projectId});
  const rows = response.data?.rows || response.data || [];
  layers.value = proxy.handleTree(rows, 'id', 'parentId');
}

/**
 * 加载修改模式的模型基础信息。
 * 沿用详情接口回填字段及子模型，不执行新增的成功版本比较。
 */
async function loadModelDetail(id) {
  detailLoaded.value = false;
  const response = await getBase(id);
  const detail = response.data || {};
  // 与表单分开复制，用户修改不能覆盖修改判断所需的原值。
  originalModel.value = JSON.parse(JSON.stringify(detail));
  for (const node of [originalModel.value, ...(originalModel.value.subModels || [])]) {
    for (const field of node.fields || []) {
      field.dataType = field.dataType ? String(field.dataType).toUpperCase() : field.dataType;
      for (const property of [
        'primaryKeyFlag',
        'requiredFlag',
        'uniqueFlag',
        'nameFlag',
      ]) {
        field[property] = toBooleanFlag(field[property]);
      }
    }
  }
  Object.assign(form, createEmptyForm(), detail);
  // 下拉选项的值是字符串，回填也转成字符串，避免选中了用户却无法显示姓名。
  form.responsibleUserId = detail.responsibleUserId == null ? null : String(detail.responsibleUserId);
  // 详情接口返回的 fields 直接回填第二步字段编辑器，并补齐前端行标识及展示所需的兼容值。
  if (Array.isArray(detail.fields)) {
    const normalizedFields = detail.fields.map((field, index) => ({
      ...field,
      localId: field.localId || `field-${field.id || index + 1}`,
      columnCategory: field.columnCategory || 'BUSINESS',
      dataType: field.dataType ? String(field.dataType).toUpperCase() : field.dataType,
      primaryKeyFlag: toBooleanFlag(field.primaryKeyFlag),
      requiredFlag: toBooleanFlag(field.requiredFlag),
      uniqueFlag: toBooleanFlag(field.uniqueFlag),
      nameFlag: toBooleanFlag(field.nameFlag),
      projectId: field.relatedProjectId ?? null,
      layerId: field.relatedLayerId ?? null,
      relatedCodeName: field.relatedCodeName || field.relatedCode || "",
      displayCodeName: field.displayCodeName || field.displayCode || "",
      nodeRule: field.columnCategory === 'CLASS' ? field.nodeRule : null,
      displayMode: field.columnCategory === 'CLASS' ? field.displayMode : null,
      nodeRuleLabel: field.columnCategory === 'CLASS' ? getNodeRuleLabel(field.nodeRule) : "",
      displayModeLabel: field.columnCategory === 'CLASS' ? getDisplayModeLabel(field.displayMode) : "",
    }));
    modelFields.value = normalizedFields;
  }
  modelFields.value = ensureSystemFields(modelFields.value);
  // 详情接口返回的 subModels 需要写入独立的响应式数组，form.subModels 不会驱动第三步子模型视图。
  if (Array.isArray(detail.subModels)) {
    subModels.value = detail.subModels.map((subModel, subIndex) => {
      // 已保存子模型使用数据库 ID 生成稳定的本地标识，新增子模型仍沿用前端临时标识。
      const localId = subModel.localId || `sub-${subModel.id || subIndex + 1}`;
      return {
        ...subModel,
        localId,
        // 子模型字段与主模型字段使用相同的数据格式，回填时统一补齐枚举值和字符串标识。
        fields: ensureSystemFields(Array.isArray(subModel.fields) ? subModel.fields.map((field, fieldIndex) => ({
          ...field,
          localId: field.localId || `${localId}-field-${field.id || fieldIndex + 1}`,
          columnCategory: field.columnCategory || 'BUSINESS',
          dataType: field.dataType ? String(field.dataType).toUpperCase() : field.dataType,
          primaryKeyFlag: toBooleanFlag(field.primaryKeyFlag),
          requiredFlag: toBooleanFlag(field.requiredFlag),
          uniqueFlag: toBooleanFlag(field.uniqueFlag),
          nameFlag: toBooleanFlag(field.nameFlag),
          projectId: field.relatedProjectId ?? null,
          layerId: field.relatedLayerId ?? null,
          relatedCodeName: field.relatedCodeName || field.relatedCode || "",
          displayCodeName: field.displayCodeName || field.displayCode || "",
          nodeRule: field.columnCategory === 'CLASS' ? field.nodeRule : null,
          displayMode: field.columnCategory === 'CLASS' ? field.displayMode : null,
          nodeRuleLabel: field.columnCategory === 'CLASS' ? getNodeRuleLabel(field.nodeRule) : "",
          displayModeLabel: field.columnCategory === 'CLASS' ? getDisplayModeLabel(field.displayMode) : "",
        })) : [], `${localId}-system-field`, true),
      };
    });
    // 回填完成后默认选中第一个子模型；没有子模型时保持第三步空态。
    activeSubModelId.value = subModels.value[0]?.localId ?? null;
  }
  publishedStructure.value = null;
  if (form.publishedInstanceId) {
    const publishedResponse = await getPublishedModelStructure(form.id);
    publishedStructure.value = publishedResponse.data;
    for (const node of [publishedStructure.value, ...(publishedStructure.value?.subModels || [])]) {
      for (const field of node?.fields || []) {
        field.dataType = field.dataType ? String(field.dataType).toUpperCase() : field.dataType;
        for (const property of ["primaryKeyFlag", "requiredFlag", "uniqueFlag", "nameFlag"]) {
          field[property] = toBooleanFlag(field[property]);
        }
        // 关联、分类字段继续使用编辑器原有的页面属性名。
        field.projectId = field.relatedProjectId ?? field.projectId ?? null;
        field.layerId = field.relatedLayerId ?? field.layerId ?? null;
      }
    }
  }
  await loadLayers(form.projectId);
  detailLoaded.value = true;
}

/** 初始化向导所需选项及数据；URL 带 id 时进入修改模式。 */
async function initializeWizard() {
  await loadFieldMeta();
  await Promise.all([
    loadProjects(),
    loadModelFields(),
    loadSubModels(),
    loadValidationItems(),
  ]);
  // 有模型 ID 时加载已有模型，无 ID 时保留新增默认值。
  if (route.query.id) await loadModelDetail(route.query.id);
  detailLoaded.value = true;
}

/** 主数据域变化后清空并重新加载模型目录。 */
async function handleProjectChange(projectId) {
  form.layerId = null;
  await loadLayers(projectId);
  markValidationPending();
}

/** 校验第一步基础信息。 */
async function validateBasicInfo() {
  return basicFormRef.value?.validate().catch(() => false);
}

/** 校验主模型字段是否具备最小必填信息。 */
function validateModelFields() {
  return modelFieldsEditorRef.value?.validateFields?.() ?? false;
}

// 检查全部子模型，包括当前没有打开的页签，发现错误后跳到对应位置。
async function validateSubModels() {
  const originalSubModelId = activeSubModelId.value;
  const originalTab = subModelTab.value;

  for (const subModel of subModels.value) {
    activeSubModelId.value = subModel.localId;
    subModelTab.value = "basic";
    await nextTick();

    const basicForm = subModelBasicFormRef.value;
    const basicValid = basicForm
        ? await basicForm.validate().catch(() => false)
        : false;
    if (!basicValid) return false;

    subModelTab.value = 'BUSINESS';
    await nextTick();
    const fieldsValid = subModelFieldsEditorRef.value?.validateFields?.() ?? false;
    if (!fieldsValid) return false;
  }

  activeSubModelId.value = originalSubModelId;
  subModelTab.value = originalTab;
  return true;
}

/**
 * 判断当前步骤是否允许离开。
 * 各步骤的详细接口校验可以继续追加在对应分支中。
 */
async function canLeaveStep(step) {
  if (step === 0) {
    return validateBasicInfo();
  }
  if (step === 1) {
    return validateModelFields();
  }
  if (step === 2) {
    return validateSubModels();
  }
  return true;
}

/**
 * 处理步骤条点击。
 * 允许直接返回已走过的步骤；向前只能逐步进入并校验当前步骤。
 */
async function handleStepChange(targetStep) {
  if (!detailLoaded.value) return;
  // 返回当前步骤之前的页面不需要重复校验。
  if (targetStep <= currentStep.value) {
    currentStep.value = targetStep;
    return;
  }
  // 禁止跨步骤前进，确保每一步都经过校验。
  if (targetStep !== currentStep.value + 1) {
    return;
  }
  if (await canLeaveStep(currentStep.value)) {
    currentStep.value = targetStep;
  }
}

/** 校验当前步骤后进入下一步。 */
async function handleNextStep() {
  if (!detailLoaded.value) return;
  if (await canLeaveStep(currentStep.value)) {
    currentStep.value += 1;
  }
}

/** 返回上一步，最低停留在基础信息。 */
function handlePrevStep() {
  currentStep.value = Math.max(0, currentStep.value - 1);
}

/** 打开新增弹窗，取消时不修改子模型列表。 */
function openSubModelDialog() {
  // 每次打开都清空草稿，取消上次新增时上传的图标不能带到这一次。
  Object.assign(subModelDraft, {name: "", code: "", icon: "", description: ""});
  subModelDialogVisible.value = true;
  nextTick(() => subModelDialogFormRef.value?.clearValidate());
}

/** 校验通过后创建子模型，并回显到基本信息页签。 */
async function confirmAddSubModel() {
  const valid = await subModelDialogFormRef.value?.validate().catch(() => false);
  if (!valid || !subModelDialogVisible.value) {
    return;
  }
  if (subModels.value.some(model => model.code === subModelDraft.code) || form.code === subModelDraft.code) {
    proxy.$modal.msgWarning('主子模型编码不能重复');
    return;
  }
  const localId = `sub-${Date.now()}`;
  subModels.value.push({
    localId,
    name: trimText(subModelDraft.name),
    code: trimText(subModelDraft.code),
    description: subModelDraft.description,
    icon: subModelDraft.icon || "",
    fields: createSystemFields(`${localId}-system-field`, true),
  });
  activeSubModelId.value = localId;
  subModelTab.value = "basic";
  subModelDialogVisible.value = false;
  markValidationPending();
}

/** 切换当前正在维护的子模型。 */
function selectSubModel(model) {
  activeSubModelId.value = typeof model === "object" ? model.localId : model;
}

/** 沿用原子模型列表移除操作，不调用新增的修改比较方法。 */
function removeSubModel(localId) {
  const index = subModels.value.findIndex(item => item.localId === localId);
  // 未找到目标时不修改当前列表。
  if (index < 0) {
    return;
  }
  subModels.value.splice(index, 1);
  if (activeSubModelId.value === localId) {
    activeSubModelId.value = subModels.value[Math.max(0, index - 1)]?.localId ?? null;
    subModelTab.value = "basic";
  }
  markValidationPending();
}

/** 任意配置变化后，将全部模型校验项恢复为待校验。 */
function markValidationPending() {
  validationItems.value.forEach(item => item.status = "pending");
  lastValidatedAt.value = "";
}

/** 直接检查页面中的主模型和全部子模型，不调用后端校验接口。 */
// 统一校验模块已移除，不把没有执行的检查显示成通过。
async function runValidation() {
  markValidationPending();
  proxy.$modal.msgWarning('模型校验功能暂未开放');
}

/** 打开当前模型的单模型关系图谱弹窗。 */
function openModelGraph() {
  const modelId = form.id || route.query.id;
  if (!modelId) {
    proxy.$modal.msgWarning("请先保存模型后查看图谱");
    return;
  }
  modelGraphDialog.modelId = modelId;
  modelGraphDialog.visible = true;
}

/** 弹窗关闭后清除当前图谱模型 ID。 */
function resetModelGraphDialog() {
  modelGraphDialog.modelId = null;
}

/** 汇总基础信息、主模型字段和子模型，形成保存接口参数。 */
function buildSubmitPayload() {
  // 提交页面中已按模型类型补齐的字段，保留原有字段记录ID。
  return {
    ...form, fields: JSON.parse(JSON.stringify(modelFields.value)),
    subModels: JSON.parse(JSON.stringify(subModels.value))
  };
}

// 两个按钮共用保存接口，草稿保存成功后留在当前步骤。
async function saveDraft() {
  await saveModel(true);
}

// 保存并退出在保存成功后返回列表。
async function submitForm() {
  await saveModel(false);
}

// 移除新增的统一校验和版本指纹检查，仍通过原新增、修改接口保存当前配置。
async function saveModel(draft) {
  if (!detailLoaded.value || submitting.value) return;
  const isUpdate = !!form.id;
  submitting.value = true;
  try {
    // 保存时再检查所有主子字段，覆盖未失焦就保存以及未打开的子模型页签。
    for (const node of [{...form, fields: modelFields.value}, ...subModels.value]) {
      for (let index = 0; index < node.fields.length; index++) {
        const result = await confirmFieldRestoration(node.fields[index], node.fields, publishedFieldsFor(node));
        if (result.cancelled) return;
        if (result.restored) node.fields.splice(index, 1, result.field);
      }
    }
    const payload = buildSubmitPayload();
    // 客户端只发送配置；后端保存接口仍必须独立校验，不信任客户端检查结果。
    // 后端返回的成功版本只供页面比较，不作为本次要修改的配置提交。
    const {
      revisionContext: _context, publishedModel: _published, publishedSnapshot: _snapshot,
      publishedInstance: _instance, ...command
    } = payload;
    void _context;
    void _published;
    void _snapshot;
    void _instance;
    const response = await (isUpdate ? updateBase(command) : addBase(command));
    if (draft) {
      // 新增接口返回模型 ID，后续保存必须改用修改接口。
      form.id = isUpdate ? form.id : response.data;
      await proxy.$router.replace({path: route.path, query: {...route.query, id: form.id}});
      const activeSubModelCode = activeSubModel.value?.code;
      const currentSubModelTab = subModelTab.value;
      // 同步后端生成的字段、子模型 ID 和版本，保留当前步骤与子模型页签。
      await loadModelDetail(form.id);
      activeSubModelId.value = subModels.value.find(model => model.code === activeSubModelCode)?.localId
          ?? subModels.value[0]?.localId ?? null;
      subModelTab.value = currentSubModelTab;
    } else {
      returnToList();
    }
    proxy.$modal.msgSuccess(draft ? '草稿保存成功' : isUpdate ? '修改保存成功' : '模型新增成功');
  } finally {
    submitting.value = false;
  }
}

// 无论行内、弹窗还是关联配置自动赋值，任何变化都使旧的前端校验结果失效。
watch([form, modelFields, subModels], markValidationPending, {deep: true});

/** 退出向导并返回模型列表。 */
function returnToList() {
  proxy.$router.push("/model/base");
}

// 页面加载后统一初始化向导数据。
initializeWizard();
</script>

<style scoped lang="scss">
.model-form-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 90px);
  min-height: 350px;

  .hasTagsView & {
    height: calc(100vh - 84px);
  }
}

.model-form-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--qt-space-4);
  margin-bottom: 15px;
  padding: 10px 15px;
  border: 1px solid var(--qt-border-color);
  background: var(--qt-surface-background);
}

.model-form-header-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--qt-space-3);
  min-width: 0;
}

.model-form-title.h2-titles {
  margin: 0;
}

.model-form-header-tip {
  margin-left: 6px;
  font-size: var(--qt-font-size-secondary);
  color: var(--qt-text-secondary);
}

.wizard-card {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;

  &.pagecont-top {
    margin-bottom: 0;
  }
}

.infotop {
  flex: 1;
  min-height: 0;
}

.wizard-content,
.submodel-step {
  height: 100%;
}

.wizard-scroll {
  max-height: 100%;
  overflow: auto;
}

.wizard-steps {
  flex-shrink: 0;
  width: 100%;
  padding: 40px 177px var(--qt-space-5) 189px;
  box-sizing: border-box;
  background: var(--qt-surface-background);
}

.section-title {
  position: relative;
  margin-bottom: 28px;
  padding-left: calc(var(--qt-title-marker-width) + var(--qt-title-marker-gap));
  font-size: var(--qt-font-size-title);
  font-weight: var(--qt-font-weight-bold);
}

.section-title::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: var(--qt-title-marker-width);
  height: var(--qt-title-marker-height);
  border-radius: var(--qt-title-marker-radius);
  background: var(--qt-title-marker-color);
  transform: translateY(-50%);
  content: "";
}

.inline-title {
  display: inline-block;
  margin: 0 var(--qt-space-5) 0 0;
}

.section-heading,
.validation-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-description,
.validation-summary p {
  font-size: var(--qt-font-size-secondary);
  color: var(--qt-text-secondary);
}

.submodel-workspace {
  display: flex;
  height: 100%;
}

.submodel-sidebar {
  display: flex;
  flex-direction: column;
  flex: 0 0 var(--qt-model-sidebar-width);
  width: var(--qt-model-sidebar-width);
  background: var(--qt-surface-background);

  .submodel-empty {
    flex: 1;
    justify-content: center;

    p {
      margin: 0;
    }
  }

  .empty-state-image {
    width: 144px;
    max-width: 100%;
  }
}

.submodel-sidebar-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: var(--qt-model-workspace-header-height);
  padding: 0 var(--qt-space-4);
  border-bottom: 1px solid var(--qt-border-color);
  background: var(--qt-surface-background);

  span {
    margin: 0;
    padding: 0;
  }
}

.submodel-card-list {
  flex: 1;
  min-height: 0;
  padding: 10px;
  overflow-y: auto;
}

.submodel-card {
  background: var(--qt-surface-background);

  &.is-active {
    background: var(--qt-element-fixed-table-row-hover-background);
  }
}

.submodel-card-select {
  display: flex;
  align-items: center;
  gap: var(--qt-space-3);
  width: 100%;
  min-width: 0;
  height: 40px;
  padding: 0 14px;
  border: 0;
  background: transparent;
  font: inherit;
  text-align: left;
  color: var(--el-text-color-primary);
  cursor: pointer;
}

.submodel-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
}

.submodel-main-content {
  min-width: 0;
  padding: 0;
  margin-left: 15px;
  overflow: hidden;
  background: var(--qt-surface-background);
}

.submodel-content {
  height: 100%;
}

.submodel-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;

  :deep(.el-tabs__header) {
    align-items: end;
    flex-shrink: 0;
    padding: 0 var(--qt-space-5);
    border-bottom: 1px solid var(--qt-border-color);
    margin: 20px 0 15px 0;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
    padding: 0 15px;
    overflow: auto;
  }
}

.submodel-empty {
  p {
    color: var(--el-text-color-secondary);
  }
}

.submodel-dialog-tip {
  padding: var(--qt-space-3);
  border: 1px solid var(--qt-border-color);
  background: var(--qt-element-fixed-table-header-background);
  line-height: 1.6;
  color: var(--qt-text-secondary);
}

.validation-summary {
  display: flex;
  align-items: center;
  gap: var(--qt-space-5);
  margin: var(--qt-space-6) 0 var(--qt-space-4);
  padding: 18px 22px;
  border: 1px solid var(--qt-border-color);
}

.validation-summary.is-passed {
  border-color: color-mix(in srgb, var(--qt-status-success) 25%, var(--qt-surface-background));
  background: color-mix(in srgb, var(--qt-status-success) 6%, var(--qt-surface-background));
}

.validation-summary.is-pending {
  background: var(--qt-element-fixed-table-header-background);
}

.validation-score {
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--qt-model-validation-score-size);
  height: var(--qt-model-validation-score-size);
  border: 5px solid var(--qt-status-success);
  border-radius: 50%;
  font-weight: var(--qt-font-weight-bold);
  color: var(--qt-status-success);
}

.validation-summary p {
  margin: var(--qt-space-2) 0 0;
}

.validation-list {
  border: 1px solid var(--qt-border-color);
}

.validation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: 0 18px;
  border-bottom: 1px solid var(--qt-border-color);
}

.validation-item:last-child {
  border-bottom: 0;
}

.validation-item span {
  display: flex;
  align-items: center;
  gap: var(--qt-space-4);
}

.validation-item .passed,
.validation-item .el-icon {
  color: var(--qt-status-success);
}

.validation-item .pending {
  color: var(--qt-text-secondary);
}

.button-style {
  flex-shrink: 0;
  padding: var(--qt-space-4) 30px 20px;
  background: var(--qt-surface-background);
  text-align: right;
}

@media (max-width: 900px) {
  .wizard-section {
    padding: 22px var(--qt-space-4);
  }
}

.domain-intro {
  --tip-title-weight: normal;
  --tip-title-color: var(--el-text-color-regular);
}

.source-option {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.source-option__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  object-fit: contain;
}
</style>
