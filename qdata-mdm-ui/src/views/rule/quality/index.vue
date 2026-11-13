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
  <div :class="{ 'app-container': !props.embedded }" ref="app-container">
    <GuideTip v-if="!props.embedded" tip-id="rule/quality.list"/>
    <el-container>
      <!-- 模型树：规则管理页面展示，模型详情内隐藏。 -->
      <qt-tree
          v-if="!props.embedded"
          ref="modelTreeRef"
          v-model:left-width="leftWidth"
          :action-loading="modelTreeLoading"
          :data="modelTreeData"
          :use-builtin-cat-edit-dialog="false"
          :tree-props="{ label: 'name', children: 'children' }"
          node-key="treeKey"
          title="主数据模型"
          placeholder="搜索名称或编码"
          :default-expand="false"
          @node-click="handleModelTreeNodeClick"
      />

      <el-main class="main-content">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef" :config="tableStore.config">
          <!-- 搜索栏：规则管理页面展示，模型详情内隐藏。 -->
          <template v-if="!props.embedded" #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                :tableRef="tableRef"
            />
          </template>
          <template #actions-data>
            <el-button
                type="primary"
                plain
                icon="Plus"
                @click="handleAdd"
                v-hasPermi="['rule:rulequality:add']"
            >
              新增
            </el-button>
            <el-button
                type="danger"
                plain
                icon="Delete"
                :disabled="multiple"
                @click="handleDelete"
                v-hasPermi="['rule:rulequality:remove']"
            >
              批量删除
            </el-button>
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #modelInfo="{ row }">
              <div v-if="row.modelName || row.modelCode" class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({ type: row.modelType, icon: row.modelIcon })" alt="" class="info-icon" />
                  <span
                    class="info-title model-info-title text-ellipsis"
                    role="link"
                    tabindex="0"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ getModelTitle(row) }}</span>
                  <dict-tag v-if="row.modelType" :options="model_type" :value="row.modelType" size="small" type="primary" />
                  <dict-tag
                    v-if="row.modelVersion"
                    class="ml4 pointer"
                    :value="row.modelVersion"
                    size="small"
                    type="info"
                    role="link"
                    tabindex="0"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || "-" }}</div>
              </div>
              <span v-else>{{ row.modelId != null ? `模型 ID：${row.modelId}` : '-' }}</span>
            </template>

            <template #location="{ row }">
              <!-- 目录按从父到子的顺序返回，最后一项就是模型直接所属的目录。 -->
              <div v-if="row.projectName || row.layers?.length" class="flex-column flex-column-gap">
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(row.projectIcon)" alt=""/>
                  <span class="text-ellipsis" :title="row.projectName || ''">{{ row.projectName || '-' }}</span>
                </div>
                <div v-if="row.layerName" class="text-ellipsis flex-center">
                  <dict-tag size="small" type="primary" :value="row.layerName"/>
                </div>
              </div>
              <span v-else class="text-muted">-</span>
            </template>
            <template #responsibleUser="{ row }">
              <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)"
                >{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text"
                >{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #ruleType="{ row }">
              <el-tag effect="plain">{{ getRuleTypeLabel(row.ruleType) }}</el-tag>
            </template>
            <template #modelFieldId="{ row }">
              {{ formatQualityFields(row) }}
            </template>
            <template #ruleConfig="{ row }">
              {{ formatRuleConfig(row) }}
            </template>
            <template #qualityTaskCount="{ row }">{{ row.qualityTaskCount ?? '-' }}</template>
            <template #mergeSnapshotCount="{ row }">{{ row.mergeSnapshotCount ?? '-' }}</template>
            <template #validFlag="{ row }">
              <div class="status-cell">
                <el-switch
                    v-model="row.validFlag"
                    active-color="#13ce66"
                    inactive-color="#ff4949"
                    :loading="statusLoading[row.id]"
                    @change="handleStatusChange(row)"
                />
                <span v-if="isAbnormalRow(row)" class="status-badge is-abnormal">
                  <i></i>配置异常
                </span>
              </div>
            </template>
            <template #action="{ row }">
              <el-button
                  link
                  type="primary"
                  icon="view"
                  @click="handleDetail(row)"
                  v-hasPermi="['rule:rulequality:edit']"
              >详情
              </el-button>
              <el-button
                  link
                  type="primary"
                  icon="Edit"
                  @click="handleUpdate(row)"
                  v-hasPermi="['rule:rulequality:edit']"
              >修改
              </el-button>
              <el-button
                  link
                  type="danger"
                  icon="Delete"
                  @click="handleDelete(row)"
                  v-hasPermi="['rule:rulequality:remove']"
              >删除
              </el-button>

            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <!-- 添加或修改质量规则对话框 -->
    <el-dialog :title="title" v-model="open" width="1100px" draggable>
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form ref="rulequalityRef" :model="form" :rules="rules" label-width="110px" @submit.prevent
               class="column-form">
        <div class="h2-title row-full">基础信息</div>
        <qt-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入名称，支持中英文、数字、下划线和短横线。"/>
        </qt-form-item>
        <qt-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则编码，仅支持英文字母、数字和下划线。"/>
        </qt-form-item>
        <qt-form-item label="主数据域" prop="projectId" tip="先选择主数据域，再选择域下模型目录">
          <el-select
              v-model="modelProjectId"
              placeholder="请选择主数据域"
              clearable
              @change="handleProjectChange"
          >
            <template #label="{ label }">
              <div v-if="selectedProjectOption" class="task-title-row">
                <img :src="getProjectIcon(selectedProjectOption.icon)" alt="" class="info-icon"/>
                <span class="text-ellipsis">{{ selectedProjectOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option v-for="item in projectOptions" :key="item.value" :label="item.label" :value="item.value">
              <div class="task-title-row">
                <img :src="getProjectIcon(item.icon)" alt="" class="info-icon"/>
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item label="模型目录" required>
          <el-tree-select v-model="modelLayerId" class="field-select" :data="layerOptions"
                          :placeholder="modelProjectId != null ? '请选择模型目录' : '请先选择主数据域'"
                          :disabled="props.embedded || modelProjectId == null" check-strictly clearable filterable
                          @change="handleLayerChange"
          />
        </qt-form-item>
        <qt-form-item label="主数据模型" prop="modelId" required>
          <el-select
              v-model="form.modelId"
              class="field-select"
              :placeholder="modelLayerId != null ? '请选择主数据模型' : '请先选择模型目录'"
              :disabled="props.embedded || modelLayerId == null"
              @change="handleModelChange"
          >
            <template #label="{ label }">
              <div v-if="selectedModelOption" class="task-title-row">
                <img :src="getModelIcon(selectedModelOption)" alt="" class="info-icon"/>
                <span class="text-ellipsis">{{ selectedModelOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option
                v-for="item in filteredModelOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
            >
              <div class="task-title-row">
                <img :src="getModelIcon(item)" alt="" class="info-icon"/>
                <span class="text-ellipsis">{{ item.label }}</span>
              </div>
            </el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item label="负责人" prop="responsibleUserId">
          <el-select v-model="form.responsibleUserId" class="number-input" placeholder="请选择负责人" clearable
                     filterable :loading="usersLoading" @clear="form.responsibleUserId = null">
            <el-option v-for="user in responsibleUserOptions" :key="user.value" :label="user.label"
                       :value="user.value"/>
          </el-select>
          <el-button v-if="usersError" link type="primary" @click="loadResponsibleUsers">重新加载负责人</el-button>
        </qt-form-item>
        <qt-form-item label="联系电话" >
          <el-input :model-value="responsibleUserPhone" disabled placeholder="请输入联系电话"/>
        </qt-form-item>
        <el-form-item label="状态" prop="validFlag">
          <el-radio-group v-model="form.validFlag">
            <el-radio :label="true">启用</el-radio>
            <el-radio :label="false">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <qt-form-item label="规则说明" prop="description" class="row-full">
          <el-input
              v-model="form.description"
              type="textarea"
              resize="vertical"
              placeholder="请输入规则说明"
              :min-height="192"
              show-word-limit
              maxlength="256个字符"
          />
        </qt-form-item>
        <div class="h2-title row-full">规则配置</div>
        <qt-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" @change="handleRuleTypeChange">
            <el-option v-for="item in rule_rulequality_type" :key="item.value" :label="item.label"
                       :value="item.value"/>
          </el-select>
        </qt-form-item>
        <div v-if="form.ruleType === 'NOT_EMPTY'" class="h2-title row-full">非空配置</div>
        <qt-form-item v-if="form.ruleType === 'NOT_EMPTY'" label="参与字段" required>
          <el-select v-model="qualityConfig.fieldId" class="number-input" placeholder="请选择参与字段">
            <el-option v-for="field in currentQualityFieldOptions" :key="field.value" :label="field.label"
                       :value="field.value"/>
          </el-select>
        </qt-form-item>
        <template v-if="form.ruleType === 'FORMAT'">
          <div class="h2-title row-full">格式配置</div>
          <qt-form-item label="字符串字段" required>
            <el-select v-model="qualityConfig.fieldId" class="number-input" placeholder="请选择字符串字段">
              <el-option v-for="field in currentQualityFieldOptions" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="格式方式" required>
            <el-select v-model="qualityConfig.formatMode" class="number-input" placeholder="请选择格式方式">
              <el-option v-for="item in formatModeOptions" :key="item.value" :label="item.label" :value="item.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item v-if="isPresetFormatMode(qualityConfig.formatMode)" label="预置格式" required
                        tip="字段为空时不由该规则判失败；如需限制为空，请单独配置非空规则。">
            <el-select v-model="qualityConfig.preset" class="number-input" placeholder="请选择预置格式">
              <el-option v-for="item in formatPresetOptions" :key="item.value" :label="item.label"
                         :value="item.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item v-else label="正则表达式" tip="字段为空时不由该规则判失败；如需限制为空，请单独配置非空规则。">
            <el-input v-model="qualityConfig.regex" class="number-input" placeholder="请输入正则表达式"/>
          </qt-form-item>
        </template>
        <template v-if="form.ruleType === 'LENGTH'">
          <div class="h2-title row-full">长度配置</div>
          <qt-form-item label="字符串字段" required>
            <el-select v-model="qualityConfig.fieldId" class="number-input" placeholder="请选择字符串字段">
              <el-option v-for="field in currentQualityFieldOptions" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="最小长度" required>
            <el-input-number v-model="qualityConfig.minLen" class="number-input" :min="0" :max="999999"
                             controls-position="right"/>
          </qt-form-item>
          <qt-form-item label="最大长度" required tip="最小长度与最大长度相同时为固定长度；空值不判失败。">
            <el-input-number v-model="qualityConfig.maxLen" class="number-input" :min="0" :max="999999"
                             controls-position="right"/>
          </qt-form-item>
        </template>
        <template v-if="form.ruleType === 'NUMBER_RANGE'">
          <div class="h2-title row-full">数值范围配置</div>
          <qt-form-item label="数值字段" required>
            <el-select v-model="qualityConfig.fieldId" class="number-input" placeholder="请选择数值字段">
              <el-option v-for="field in currentQualityFieldOptions" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="最小值">
            <div class="quality-range-row">
              <el-select v-model="qualityConfig.minOp" class="quality-range-op">
                <el-option label="≥" value=">="/>
                <el-option label=">" value=">"/>
              </el-select>
              <el-input v-model="qualityConfig.minVal" placeholder="可为空"/>
            </div>
          </qt-form-item>
          <qt-form-item label="最大值" tip="最小值、最大值至少填写一项；空值不判失败。">
            <div class="quality-range-row">
              <el-select v-model="qualityConfig.maxOp" class="quality-range-op">
                <el-option label="≤" value="<="/>
                <el-option label="<" value="<"/>
              </el-select>
              <el-input v-model="qualityConfig.maxVal" placeholder="可为空"/>
            </div>
          </qt-form-item>
        </template>
        <template v-if="form.ruleType === 'REF_DATA_VALID'">
          <div class="h2-title row-full">参考数据合法性配置</div>
          <qt-form-item label="参考数据字段" required tip="仅校验该字段已绑定且启用的参考数据项；空值不判失败。">
            <el-select v-model="qualityConfig.fieldId" class="number-input" placeholder="请选择参考数据字段"
                       @change="handleQualityFieldChange">
              <el-option v-for="field in currentQualityFieldOptions" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="参考数据集" required>
            <el-select v-model="qualityConfig.refDataSetCode" class="number-input" placeholder="请选择参考数据集">
              <el-option v-for="item in currentReferenceDataOptions" :key="item.value" :label="item.label"
                         :value="item.value" :disabled="item.disabled"/>
            </el-select>
          </qt-form-item>
        </template>
        <template v-if="form.ruleType === 'UNIQUE'">
          <div class="h2-title row-full">唯一性配置</div>
          <qt-form-item label="参与字段" required tip="当前主数据模型内校验唯一性;任一参与字段为空时跳过唯一性判断">
            <el-select v-model="qualityConfig.fieldIdList" class="number-input" multiple
                       placeholder="请选择参与字段">
              <el-option v-for="field in currentModelOption?.fields || []" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
        </template>
        <template v-if="form.ruleType === 'FIELD_LOGIC'">
          <div class="h2-title row-full">字段间逻辑比较配置</div>
          <qt-form-item label="字段一" required>
            <el-select v-model="qualityConfig.fieldId1" class="number-input" placeholder="请选择字段一"
                       @change="handleLogicFieldChange">
              <el-option v-for="field in currentModelOption?.fields || []" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="比较符">
            <el-select v-model="qualityConfig.compareOp" class="number-input">
              <el-option v-for="operator in currentLogicOperators" :key="operator" :label="operator"
                         :value="operator"/>
            </el-select>
          </qt-form-item>
          <qt-form-item label="字段二" required tip="字符串、参考数据仅支持等于或不等于；数值、日期支持大小比较。">
            <el-select v-model="qualityConfig.fieldId2" class="number-input" placeholder="请选择字段二">
              <el-option v-for="field in currentLogicFieldOptions" :key="field.value" :label="field.label"
                         :value="field.value"/>
            </el-select>
          </qt-form-item>
        </template>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="fieldsLoading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 质量规则详情对话框 -->
    <el-dialog :title="title" v-model="openDetail" width="1000px" draggable>
      <template #header>
        <span role="heading" aria-level="2" class="el-dialog__title">
          {{ title }}
        </span>
      </template>
      <el-form
          ref="rulequalityDetailRef"
          :model="form"
          label-width="90px"
          class="column-form rulequality-detail-form"
      >
        <div class="h2-title row-full">基础信息</div>
        <el-form-item label="规则编码">
          <div class="form-readonly">{{ form.ruleCode || "-" }}</div>
        </el-form-item>
        <el-form-item label="规则名称">
          <div class="form-readonly">{{ form.ruleName || "-" }}</div>
        </el-form-item>
        <el-form-item label="主数据模型">
          <div class="form-readonly">{{ getModelLabel(form.modelId) }}</div>
        </el-form-item>
        <el-form-item label="状态">
          <div class="form-readonly">{{ form.validFlag ? "启用" : "禁用" }}</div>
        </el-form-item>
        <el-form-item label="规则说明" class="row-full">
          <div class="form-readonly textarea">{{ form.description ?? "-" }}</div>
        </el-form-item>

        <div class="h2-title row-full">规则配置</div>
        <el-form-item label="规则类型">
          <div class="form-readonly">{{ currentRuleTypeLabel }}</div>
        </el-form-item>
        <div class="rule-config-panel quality-config-panel row-full">
          <div class="rule-config-title">{{ currentRuleTypeLabel }}配置</div>
          <template v-if="form.ruleType === 'NOT_EMPTY'">
            <el-form-item class="quality-format-item" label="参与字段" >
              <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId) }}</div>
            </el-form-item>
            <div class="quality-config-tip">字段必须存在有效值。</div>
          </template>
          <template v-else-if="form.ruleType === 'FORMAT'">
            <el-row :gutter="20" class="quality-format-row">
              <el-col :span="12">
                <el-form-item class="quality-format-item" label="字符串字段">
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item class="quality-format-item" label="格式方式" >
                  <div class="form-readonly quality-detail-readonly">{{ qualityConfig.formatMode || "-" }}</div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item class="quality-format-item"
                          :label="isPresetFormatMode(qualityConfig.formatMode) ? '预置格式' : '正则表达式'">
              <div class="form-readonly quality-detail-readonly">
                {{
                  isPresetFormatMode(qualityConfig.formatMode) ? getFormatPresetLabel(qualityConfig.preset) : (qualityConfig.regex || "-")
                }}
              </div>
            </el-form-item>
          </template>
          <template v-else-if="form.ruleType === 'LENGTH'">
            <el-row :gutter="12" class="quality-format-row quality-logic-row">
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="字符串字段">
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="最小长度">
                  <div class="form-readonly quality-detail-readonly">{{ qualityConfig.minLen ?? "-" }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="最大长度">
                  <div class="form-readonly quality-detail-readonly">{{ qualityConfig.maxLen ?? "-" }}</div>
                </el-form-item>
              </el-col>
            </el-row>
            <div class="quality-config-tip quality-config-tip--flush">最小长度与最大长度相同时表示固定长度。<br/>字段为空时不由该规则判失败；如需限制为空，请单独配置非空规则。
            </div>
          </template>
          <template v-else-if="form.ruleType === 'NUMBER_RANGE'">
            <el-row :gutter="12" class="quality-format-row quality-logic-row quality-range-logic-row">
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="数值字段">
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="最小值">
                  <div class="quality-range-row">
                    <div class="form-readonly quality-range-op-readonly">
                      {{ getRangeOperatorLabel(qualityConfig.minOp, ">=") }}
                    </div>
                    <div class="form-readonly quality-detail-readonly">{{ qualityConfig.minVal || "-" }}</div>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="最大值">
                  <div class="quality-range-row">
                    <div class="form-readonly quality-range-op-readonly">
                      {{ getRangeOperatorLabel(qualityConfig.maxOp, "<=") }}
                    </div>
                    <div class="form-readonly quality-detail-readonly">{{ qualityConfig.maxVal || "-" }}</div>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <div class="quality-config-tip quality-config-tip--flush">最小值、最大值至少填写一项。<br/>字段为空时不由该规则判失败；如需限制为空，请单独配置非空规则。
            </div>
          </template>
          <template v-else-if="form.ruleType === 'REF_DATA_VALID'">
            <el-row :gutter="20" class="quality-format-row">
              <el-col :span="12">
                <el-form-item class="quality-format-item quality-reference-item" label="参考数据字段" >
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item class="quality-format-item quality-reference-item" label="参考数据集">
                  <div class="form-readonly quality-detail-readonly">
                    {{ getReferenceDataLabel(qualityConfig.refDataSetCode) }}
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <div class="quality-config-tip quality-config-tip--flush">使用字段已绑定的参考数据集合，仅启用状态的参考数据项视为合法值。<br/>
              字段为空时不由该规则判失败；如需限制为空，请单独配置非空规则
            </div>
          </template>
          <template v-else-if="form.ruleType === 'UNIQUE'">
            <el-form-item class="quality-format-item">
              <el-checkbox-group
                  :model-value="qualityConfig.fieldIdList"
                  class="quality-checkbox-group"
                  disabled
              >
                <el-checkbox
                    v-for="field in currentModelOption?.fields || []"
                    :key="field.value"
                    :label="field.value"
                >
                  {{ field.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <div class="quality-config-tip quality-config-tip--flush">
              在当前主数据模型内校验唯一性；任一参与字段为空时跳过唯一性判断。
            </div>
          </template>
          <template v-else-if="form.ruleType === 'FIELD_LOGIC'">
            <el-row :gutter="12" class="quality-format-row quality-logic-row">
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="字段一">
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId1) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="比较符">
                  <div class="form-readonly quality-detail-readonly">{{ qualityConfig.compareOp || "-" }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8" class="quality-logic-col">
                <el-form-item class="quality-format-item quality-logic-item" label="字段二">
                  <div class="form-readonly quality-detail-readonly">{{ getFieldName(qualityConfig.fieldId2) }}</div>
                </el-form-item>
              </el-col>
            </el-row>
            <div class="quality-config-tip quality-config-tip--flush">字符串、参考数据仅支持等于或不等于；数值、日期支持大小比较。<br/>
              任一字段为空时不判失败。
            </div>
          </template>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Rulequality">
import {getModelIcon, defaultProjectIcon, getProjectIcon} from "@/utils/imageUrl";
import {useDict} from "@/utils/dict";
import {
  listRulequality,
  getRulequality,
  delRulequality,
  addRulequality,
  updateRulequality
} from "@/api/rule/quality/quality";
import QtTree from "@/components/QtTree/index.vue";
import {getPublishedModelStructure} from "@/api/model/base/base";
import {usePublishedModelTree, withModelTreeKeys} from "@/composables/usePublishedModelTree";
import {useUserDirectory} from '@/store/system/userDirectory';
import {useRoute, useRouter} from "vue-router";
import {trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired} from "@/utils/validate.js";

const {model_type, rule_match_type} = useDict('model_type', 'rule_match_type');

const {responsibleUserList, getResponsibleName, getResponsiblePhone, loadResponsibleUserOptions} = useUserDirectory();

// 页面参数：支持模型详情内嵌，并接收当前模型信息。
const props = defineProps({
  embedded: {type: Boolean, default: false},
  modelId: {type: [String, Number], default: null},
  modelDetail: {type: Object, default: () => ({})},
});

const {proxy} = getCurrentInstance();
const router = useRouter();
const {rule_rulequality_type} = useDict(
    "rule_rulequality_type"
);
const tableRef = ref(null);
const statusLoading = reactive({});

const open = ref(false);
const openDetail = ref(false);
const ids = ref([]);
const multiple = ref(true);
const title = ref("");
const usersLoading = ref(false);
const usersError = ref(false);

const modelTreeRef = ref(null);
const leftWidth = ref(280);
const selectedModelId = ref(props.embedded ? props.modelId : null);
const {
  data: modelTreeData,
  loading: modelTreeLoading,
  reload: reloadModelTree
} = usePublishedModelTree(withModelTreeKeys, !props.embedded);
const modelProjectId = ref(null);
const modelLayerId = ref(null);
const projectOptions = computed(() => props.embedded ? [{
  value: props.modelDetail.projectId,
  label: props.modelDetail.projectName || String(props.modelDetail.projectId || ''),
  icon: props.modelDetail.projectIcon,
}] : modelTreeData.value.map(node => ({value: node.projectId, label: node.name || node.code, icon: node.icon})));
const layerOptions = computed(() => {
  if (props.embedded) return [{
    value: props.modelDetail.layerId,
    label: props.modelDetail.layers?.map(item => item.name).join(' / ') || String(props.modelDetail.layerId || '')
  }];
  const project = modelTreeData.value.find(node => String(node.projectId) === String(modelProjectId.value));
  const toLayers = nodes => (nodes || []).filter(node => node.nodeType === 'layer')
      .map(node => ({value: node.layerId, label: node.name || node.code, children: toLayers(node.children)}));
  return toLayers(project?.children);
});
const selectedProjectOption = computed(() => projectOptions.value.find(item => String(item.value) === String(modelProjectId.value)));
const publishedModelFields = reactive({});
const publishedReferenceTypes = reactive({});
const loadedModelFields = new Set();
const loadingModelFields = new Map();
const failedModelFields = new Set();
const fieldsLoading = ref(false);

function normalizePublishedField(field) {
  return {
    value: field.id,
    label: field.name || field.code || String(field.id),
    name: field.name || field.code,
    code: field.code,
    type: field.dictTypeId != null ? "reference" : String(field.dataType || "").toLowerCase().replace(/^datetime$/, "date"),
    dictTypeId: field.dictTypeId,
  };
}

// 独立页面使用已发布模型树，模型详情内使用调用方传入的数据。
const publishedModelOptions = computed(() => {
  const models = [];
  const visit = nodes => {
    for (const node of nodes || []) {
      if (node.modelId != null) {
        models.push({
          value: node.modelId,
          projectId: node.projectId,
          layerId: node.layerId,
          icon: node.icon,
          type: node.type,
          label: node.name || node.code || String(node.modelId),
          version: node.versionNo || "",
          fields: publishedModelFields[String(node.modelId)] || [],
        });
      }
      visit(node.children);
    }
  };
  visit(modelTreeData.value);
  return models;
});

const modelOptions = computed(() => props.embedded ? [{
  value: props.modelId,
  projectId: props.modelDetail.projectId,
  layerId: props.modelDetail.layerId,
  icon: props.modelDetail.icon,
  parentId: props.modelDetail.parentId,
  label: props.modelDetail.name || props.modelDetail.code || String(props.modelId || ""),
  code: props.modelDetail.code || "",
  type: props.modelDetail.type || "",
  description: props.modelDetail.description || "",
  version: props.modelDetail.version?.versionNo || "",
  fields: (props.modelDetail.fields || []).map(normalizePublishedField),
}] : publishedModelOptions.value);
const filteredModelOptions = computed(() => props.embedded ? modelOptions.value : modelOptions.value.filter(item =>
    modelProjectId.value != null && modelLayerId.value != null &&
    String(item.projectId) === String(modelProjectId.value) && String(item.layerId) === String(modelLayerId.value)));

function handleProjectChange() {
  modelLayerId.value = null;
  handleLayerChange();
}

function handleLayerChange() {
  form.value.modelId = null;
  form.value.modelVersion = null;
  resetQualityConfig();
}

function syncModelLocation() {
  const model = props.embedded ? props.modelDetail : getModelOption(form.value.modelId);
  modelProjectId.value = model?.projectId ?? null;
  modelLayerId.value = model?.layerId ?? null;
}

const SINGLE_FIELD_RULE_TYPES = new Set([
  "NOT_EMPTY",
  "FORMAT",
  "LENGTH",
  "NUMBER_RANGE",
  "REF_DATA_VALID",
]);
const QUALITY_FIELD_TYPE_FILTERS = {
  FORMAT: new Set(["string"]),
  LENGTH: new Set(["string"]),
  NUMBER_RANGE: new Set(["number", "decimal", "int", "integer", "long", "float", "double"]),
  REF_DATA_VALID: new Set(["reference"]),
};
const NUMERIC_FIELD_TYPES = new Set(["int", "integer", "long", "float", "double", "number", "decimal"]);
const COMPARABLE_FIELD_TYPES = new Set(["number", "date"]);

const formatModeOptions = [
  {label: "系统预置", value: "系统预置"},
  {label: "自定义正则", value: "自定义正则"},
];
const formatPresetOptions = [
  {label: "统一社会信用代码", value: "creditCode"},
  {label: "手机号码", value: "phone"},
  {label: "电子邮箱", value: "email"},
];

const qualityConfig = reactive(createDefaultQualityConfig());
const currentRuleTypeLabel = computed(
    () => rule_rulequality_type.value.find((item) => item.value === form.value.ruleType)?.label || "质量规则"
);
const currentModelOption = computed(() =>
    modelOptions.value.find((item) => String(item.value) === String(form.value.modelId))
);
const currentQualityFieldOptions = computed(() =>
    getQualityFieldOptions(form.value.ruleType, form.value.modelId)
);
const currentReferenceDataOptions = computed(() =>
    getReferenceDataOptions(form.value.modelId, qualityConfig.fieldId, qualityConfig.refDataSetCode)
);
const currentLogicFieldOptions = computed(() =>
    getComparableFieldOptions(form.value.modelId, qualityConfig.fieldId1)
);
const currentLogicOperators = computed(() =>
    getComparisonOperators(getFieldType(qualityConfig.fieldId1, form.value.modelId))
);

function createDefaultForm(model) {
  return {
    id: null,
    ruleCode: null,
    ruleName: null,
    responsibleUserId: null,
    modelId: model?.value ?? null,
    modelVersion: model?.version || null,
    validFlag: true,
    abnormalFlag: "0",
    modelFieldId: null,
    ruleType: "NOT_EMPTY",
    ruleConfig: "{}",
    description: null,
  };
}

const data = reactive({
  form: createDefaultForm(props.embedded ? modelOptions.value[0] : undefined),
  queryParams: {
    ruleCode: null,
    ruleName: null,
    modelId: props.embedded ? props.modelId : null,
    modelVersion: props.embedded ? modelOptions.value[0]?.version : null,
    validFlag: null,
    abnormalFlag: null,
    modelFieldId: null,
    ruleType: null,
    orderByColumn: "create_time",
    isAsc: "descending",
  },
  rules: {
    ruleCode: [
      {required: true, validator: validateTrimmedRequired("规则编码不能为空", 64), trigger: "blur"},
      {
        validator: validateAlphaNumUnderscore("规则编码只允许英文字母、数字和下划线"),
        trigger: "blur",
        transform: trimText,
      },
    ],
    ruleName: [
      {required: true, validator: validateTrimmedRequired("规则名称不能为空", 64), trigger: "blur"},
      {
        validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, "规则名称只允许中文、英文字母、数字、下划线和短横线"),
        trigger: "blur",
        transform: trimText,
      },
    ],
    modelId: [{required: true, message: "主数据模型不能为空", trigger: "change"}],
    ruleType: [{required: true, message: "规则类型不能为空", trigger: "change"}],
  }
});

const {queryParams, form, rules} = toRefs(data);

const responsibleUserOptions = computed(() => {
  const id = form.value.responsibleUserId;
  const users = responsibleUserList.value;
  if (id != null && id !== '' && !users.some(user => user.value === String(id))) {
    return [{value: String(id), label: `用户 ${id}（已不可用）`, phone: ''}, ...users];
  }
  return users;
});
const responsibleUserPhone = computed(() => getResponsiblePhone(form.value.responsibleUserId));

async function loadResponsibleUsers() {
  usersLoading.value = true;
  usersError.value = false;
  try {
    await loadResponsibleUserOptions(form.value.responsibleUserId);
  } catch {
    usersError.value = true;
  } finally {
    usersLoading.value = false;
  }
}

const tableStore = reactive({
  config: {
    // 内嵌列表：取消独立容器留白，隐藏搜索切换按钮。
    ...(props.embedded ? {fullContent: false, actions: {table: {search: false}}} : {}),
    sort: true,
    table: {
      stripe: true,
      rowKey: "id",
      class: "rulequality-table",
      rowClassName: getRowClassName,
      defaultSort: {prop: "createTime", order: "descending"},
      onSelectionChange: handleSelectionChange,
    },
  },
  columns: [
    {type: "selection", width: 55},
    {label: "编号", prop: "id", width: 60, sortable: true},
    {
      label: "规则名称", prop: "ruleName", minWidth: 360,
      info: {
        title: row => `${row.ruleName || "-"}(${row.ruleCode || "-"})`,
        desc: "description",
        click: (row) => handleDetail(row),
        tags: [
          {
            options: rule_rulequality_type,
            value: "ruleType",
          },
        ],
      }, align: "left"
    },
    {label: "主数据模型", prop: "modelId", slot: "modelInfo", minWidth: 360, align: "left", showOverflowTooltip: { effect: "light" }},
    {label: "归属位置", prop: "projectName", slot: "location", minWidth: 210, align: "left"},
    {label: "规则类型", prop: "ruleType", dict: "rule_rulequality_type", minWidth: 150, align: "left"},
    {label: "参与字段", prop: "modelFieldId", slot: "modelFieldId", minWidth: 180, align: "left"},
    {label: "判断规则", prop: "ruleConfig", slot: "ruleConfig", minWidth: 230, align: "left"},
    {label: "质量任务数", prop: "qualityTaskCount", slot: "qualityTaskCount", minWidth: 160},
    {label: "合并管理记录数", prop: "mergeSnapshotCount", slot: "mergeSnapshotCount", minWidth: 200},
    {label: "状态", prop: "validFlag", slot: "validFlag", minWidth: 135, align: "left"},
    {label: "负责人", prop: "responsibleUserId", slot: "responsibleUser", minWidth: 140, align: "left"},
    {label: "创建时间", prop: "createTime", minWidth: 150, date: true, sortable: true, sortableKey: "create_time"},
    {label: "操作", minWidth: 220, fixed: "right", slot: "action"},
  ],
  func: listRulequality,
  params: queryParams.value,
  events: {
    formatData: rows => {
      if (!props.embedded) hydrateListModelFields(rows);
      return rows;
    },
    // 查询条件：详情内固定模型和版本，与规则管理使用相同筛选口径。
    formatParams: params => props.embedded
        ? {...params, modelId: props.modelId, modelVersion: modelOptions.value[0].version}
        : params,
  },
});

const searchStore = reactive({
  items: [
    {label: "规则名称", prop: "ruleName", component: {is: "input", placeholder: "请输入规则名称"}},
    {
      label: "规则类型",
      prop: "ruleType",
      component: {is: "select", placeholder: "请选择规则类型", options: rule_rulequality_type},
    },
    {
      label: "状态",
      prop: "validFlag",
      component: {
        is: "select",
        placeholder: "请选择状态",
        options: [
          {label: "启用", value: true},
          {label: "禁用", value: false},
        ],
      },
    },
    {
      label: "配置情况",
      prop: "abnormalFlag",
      component: {
        is: "select",
        class: "config-search-input",
        placeholder: "全部",
        options: [
          {label: "全部", value: ""},
          {label: "配置正常", value: "0"},
          {label: "配置异常", value: "1"},
        ],
      },
    },
  ],
});

/** 查询匹配规则列表 */
function getList() {
  tableRef.value?.getList();
}

// 取消按钮
function cancel() {
  open.value = false;
  openDetail.value = false;
  reset();
}

function createDefaultQualityConfig() {
  return {
    fieldId: null,
    fieldIdList: [],
    formatMode: "系统预置",
    preset: "统一社会信用代码",
    regex: "",
    minLen: 0,
    maxLen: 100,
    minOp: ">=",
    minVal: "",
    minNullable: true,
    maxOp: "<=",
    maxVal: "",
    maxNullable: false,
    refDataSetCode: "",
    fieldId1: null,
    compareOp: "=",
    fieldId2: null,
  };
}

function getModelOption(modelId) {
  return modelOptions.value.find(item => String(item.value) === String(modelId))
      || (publishedModelFields[String(modelId)] ? {
        value: modelId,
        fields: publishedModelFields[String(modelId)]
      } : undefined);
}

async function hydrateListModelFields(rows) {
  if (!modelTreeData.value.length) {
    try {
      await reloadModelTree();
    } catch {
      return;
    }
  }
  const publishedIds = new Set(publishedModelOptions.value.map(model => String(model.value)));
  const modelIds = new Set(rows.map(row => row.modelId).filter(id => id != null));
  modelIds.forEach(id => {
    if (publishedIds.has(String(id))) loadModelFields(id).catch(() => {
    });
  });
}

function loadModelFields(modelId, retry = false) {
  if (props.embedded || modelId == null) return Promise.resolve();
  const key = String(modelId);
  if (loadedModelFields.has(key) || (failedModelFields.has(key) && !retry)) return Promise.resolve();
  if (loadingModelFields.has(key)) return loadingModelFields.get(key);
  const request = getPublishedModelStructure(modelId).then(({data}) => {
    publishedModelFields[key] = (data?.fields || []).map(normalizePublishedField);
    publishedReferenceTypes[key] = data?.referenceTypes || [];
    loadedModelFields.add(key);
    failedModelFields.delete(key);
  }).catch(error => {
    failedModelFields.add(key);
    throw error;
  }).finally(() => loadingModelFields.delete(key));
  loadingModelFields.set(key, request);
  return request;
}

function getModelLabel(modelId) {
  return getModelOption(modelId)?.label || modelId || "-";
}

function getModelTitle(row) {
  const { modelName, modelCode } = row;
  return modelName ? (modelCode ? `${modelName} (${modelCode})` : modelName) : modelCode || "-";
}

function openModelDetail(row, tab) {
  if (row.modelId == null) return;
  router.push({
    path: "/model/base/detail",
    query: { id: row.modelId, ...(tab ? { tab } : {}) },
  });
}

function handleModelTreeNodeClick(node) {
  if (!node?.modelId) return;
  selectedModelId.value = node.modelId;
  queryParams.value.modelId = node.modelId;
  queryParams.value.modelVersion = node.versionNo || null;
  tableRef.value?.resetQuery();
}

onMounted(() => {
  resetQualityConfig();
  nextTick(() => {
    if (selectedModelId.value) modelTreeRef.value?.setCurrentKey?.(`model:${selectedModelId.value}`);
  });
});

const route = useRoute();
watch(() => route.fullPath, async () => {
  if (route.path !== "/rule/quality") return;
  await nextTick();
  reloadModelTree().catch(() => {
  });
  tableRef.value?.getList();
});

function normalizeModelId(modelId) {
  return getModelOption(modelId)?.value ?? modelId;
}

function syncModelVersion() {
  const model = getModelOption(form.value.modelId);
  if (model) form.value.modelVersion = model.version;
}

function getFieldOption(fieldId, modelId = form.value.modelId) {
  const fieldOptions = getModelOption(modelId)?.fields || [];
  return fieldOptions.find((item) =>
      String(item.value) === String(fieldId)
      || item.label === fieldId
      || item.name === fieldId
      || item.code === fieldId
  );
}

function normalizeFieldId(fieldId, modelId = form.value.modelId) {
  if (fieldId === null || fieldId === undefined || fieldId === "") return null;
  const option = getFieldOption(fieldId, modelId);
  if (option) return option.value;
  const numericValue = Number(fieldId);
  return Number.isNaN(numericValue) ? fieldId : numericValue;
}

function getFieldName(fieldId, modelId = form.value.modelId) {
  const fieldOption = getFieldOption(fieldId, modelId);
  return fieldOption?.name || fieldOption?.label || fieldId || "-";
}

function getFieldType(fieldId, modelId = form.value.modelId) {
  return getFieldOption(fieldId, modelId)?.type || "";
}

function normalizeComparableType(type) {
  if (NUMERIC_FIELD_TYPES.has(type)) {
    return "number";
  }
  return type || "";
}

function getComparisonOperators(type) {
  return COMPARABLE_FIELD_TYPES.has(normalizeComparableType(type))
      ? ["=", "≠", ">", "≥", "<", "≤"]
      : ["=", "≠"];
}

function isValidRegex(value) {
  if (!value) return false;
  try {
    new RegExp(value);
    return true;
  } catch {
    return false;
  }
}

function areFieldsComparable(fieldId1, fieldId2, modelId = form.value.modelId) {
  const type1 = normalizeComparableType(getFieldType(fieldId1, modelId));
  const type2 = normalizeComparableType(getFieldType(fieldId2, modelId));
  return Boolean(type1 && type2 && type1 === type2);
}

function getQualityFieldOptions(ruleType, modelId = form.value.modelId) {
  const fields = getModelOption(modelId)?.fields || [];
  const allowedTypes = QUALITY_FIELD_TYPE_FILTERS[ruleType];
  return allowedTypes
      ? fields.filter((field) => allowedTypes.has(field.type))
      : fields;
}

function getComparableFieldOptions(modelId, fieldId1) {
  const fields = getModelOption(modelId)?.fields || [];
  if (!fieldId1) return fields;
  const type1 = normalizeComparableType(getFieldType(fieldId1, modelId));
  return fields.filter((field) =>
      String(field.value) !== String(fieldId1)
      && normalizeComparableType(field.type) === type1
  );
}

function getReferenceDataLabel(code) {
  if (!code) return "-";
  const types = [...Object.values(publishedReferenceTypes).flat(), ...(props.modelDetail.referenceTypes || [])];
  const item = types.find(option => option.code === code);
  return item ? `${item.name || item.code}（${item.code}）` : code;
}

function normalizeReferenceDataCode(code) {
  if (!code) return "";
  return code;
}

function getReferenceDataOptions(modelId, fieldId, fallbackCode = "") {
  const field = getFieldOption(fieldId, modelId);
  const types = props.embedded ? (props.modelDetail.referenceTypes || []) : (publishedReferenceTypes[String(modelId)] || []);
  const item = field?.dictTypeId == null ? null : types.find(type => String(type.id) === String(field.dictTypeId));
  const result = item?.code ? [{label: `${item.name || item.code}（${item.code}）`, value: item.code}] : [];
  if (fallbackCode && !result.some((item) => item.value === fallbackCode)) {
    result.push({label: `${fallbackCode}（原配置不可用）`, value: fallbackCode, disabled: true});
  }
  return result;
}

function getFormatPresetOption(value) {
  return formatPresetOptions.find((item) => item.value === value || item.label === value);
}

function normalizeFormatPreset(value) {
  return getFormatPresetOption(value)?.value || value || "";
}

function getFormatPresetLabel(value) {
  return getFormatPresetOption(value)?.label || value || "-";
}

function getFirstFieldId(ruleType, modelId = form.value.modelId) {
  return getQualityFieldOptions(ruleType, modelId)[0]?.value ?? null;
}

function getFirstComparableFieldId(modelId, fieldId1) {
  return getComparableFieldOptions(modelId, fieldId1)[0]?.value ?? null;
}

function normalizeRangeOperator(operator, fallback) {
  return {
    "≥": ">=",
    "≤": "<=",
  }[operator] || operator || fallback;
}

function resetQualityConfig() {
  const modelId = form.value.modelId;
  const ruleType = form.value.ruleType;
  const firstFieldId = getFirstFieldId(ruleType, modelId);
  const logicFieldId1 = getFirstFieldId("FIELD_LOGIC", modelId);
  const logicFieldId2 = getFirstComparableFieldId(modelId, logicFieldId1);
  const referenceOptions = getReferenceDataOptions(modelId, firstFieldId);

  Object.assign(qualityConfig, {
    ...createDefaultQualityConfig(),
    fieldId: firstFieldId,
    fieldIdList: [],
    refDataSetCode: referenceOptions[0]?.value || "",
    fieldId1: logicFieldId1,
    fieldId2: logicFieldId2,
  });
}

async function handleModelChange() {
  form.value.modelId = normalizeModelId(form.value.modelId);
  syncModelVersion();
  resetQualityConfig();
  const modelId = form.value.modelId;
  if (modelId == null || props.embedded) return;
  fieldsLoading.value = true;
  try {
    await loadModelFields(modelId, true);
    if (String(form.value.modelId) === String(modelId)) resetQualityConfig();
  } catch {
    if (String(form.value.modelId) === String(modelId)) proxy.$modal.msgError("加载模型字段失败，请重新选择模型重试");
  } finally {
    if (String(form.value.modelId) === String(modelId)) fieldsLoading.value = false;
  }
}

function handleRuleTypeChange() {
  resetQualityConfig();
}

function handleQualityFieldChange() {
  const options = currentReferenceDataOptions.value;
  if (!options.some((item) => item.value === qualityConfig.refDataSetCode)) {
    qualityConfig.refDataSetCode = options[0]?.value || "";
  }
}

function handleLogicFieldChange() {
  const fieldOptions = currentLogicFieldOptions.value;
  if (!fieldOptions.some((field) => String(field.value) === String(qualityConfig.fieldId2))) {
    qualityConfig.fieldId2 = fieldOptions[0]?.value ?? null;
  }
  const operators = currentLogicOperators.value;
  if (!operators.includes(qualityConfig.compareOp)) {
    qualityConfig.compareOp = operators[0] || "=";
  }
}

function parseFieldIds(value) {
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  if (value === null || value === undefined || value === "") return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean);
  } catch {
    // 兼容历史的逗号分隔格式
  }
  return String(value).split(",").map((item) => item.trim()).filter(Boolean);
}

function parseRuleConfig(value) {
  if (value && typeof value === "object") return value;
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function getLegacyConfigFieldIds(config) {
  if (!Array.isArray(config?.fieldList)) return [];
  return config.fieldList
      .map((item) => item?.fieldId ?? item?.modelFieldId ?? item)
      .filter((fieldId) => fieldId !== null && fieldId !== undefined && fieldId !== "");
}

function normalizeFormatMode(mode) {
  return ["自定义正则", "REGEX", "CUSTOM_REGEX"].includes(mode)
      ? "自定义正则"
      : "系统预置";
}

function isPresetFormatMode(mode) {
  return normalizeFormatMode(mode) === "系统预置";
}

function reset() {
  proxy.resetForm("rulequalityRef");
  const defaultModel = getModelOption(selectedModelId.value) || (props.embedded ? modelOptions.value[0] : undefined);
  form.value = createDefaultForm(defaultModel);
  syncModelLocation();
  resetQualityConfig();
}

function hydrateRuleConfig() {
  const config = parseRuleConfig(form.value.ruleConfig);
  const modelFieldIds = parseFieldIds(form.value.modelFieldId);
  const configuredFieldIds = parseFieldIds(config.fieldIdList);
  const legacyConfigFieldIds = getLegacyConfigFieldIds(config);

  resetQualityConfig();
  Object.assign(qualityConfig, {
    fieldId: normalizeFieldId(
        config.fieldId
        ?? config.modelFieldId
        ?? legacyConfigFieldIds[0]
        ?? modelFieldIds[0]
    ),
    fieldIdList: (configuredFieldIds.length
        ? configuredFieldIds
        : (legacyConfigFieldIds.length ? legacyConfigFieldIds : modelFieldIds))
        .map((fieldId) => normalizeFieldId(fieldId))
        .filter((fieldId) => fieldId !== null),
    formatMode: normalizeFormatMode(
        config.formatMode || (config.regex ? "自定义正则" : qualityConfig.formatMode)
    ),
    preset: normalizeFormatPreset(config.preset || qualityConfig.preset),
    regex: config.regex ?? "",
    minLen: config.minLen ?? config.min ?? qualityConfig.minLen,
    maxLen: config.maxLen ?? config.max ?? qualityConfig.maxLen,
    minOp: normalizeRangeOperator(config.minOp, qualityConfig.minOp),
    minVal: config.minVal ?? (config.min ?? ""),
    minNullable: config.minNullable ?? qualityConfig.minNullable,
    maxOp: normalizeRangeOperator(config.maxOp, qualityConfig.maxOp),
    maxVal: config.maxVal ?? (config.max ?? ""),
    maxNullable: config.maxNullable ?? qualityConfig.maxNullable,
    refDataSetCode: normalizeReferenceDataCode(
        config.refDataSetCode || config.refSet || qualityConfig.refDataSetCode
    ),
    fieldId1: normalizeFieldId(
        config.fieldId1
        ?? config.modelFieldId1
        ?? config.left
        ?? modelFieldIds[0]
    ),
    compareOp: config.compareOp || config.op || qualityConfig.compareOp,
    fieldId2: normalizeFieldId(
        config.fieldId2
        ?? config.modelFieldId2
        ?? config.right
        ?? modelFieldIds[1]
    ),
  });

  // 保留历史配置，失效字段由校验提示用户重新选择。
}

function getRuleTypeLabel(type) {
  return rule_rulequality_type.value.find((item) => item.value === type)?.label || type || "-";
}

function getRowRuleConfig(row) {
  return parseRuleConfig(row.ruleConfig);
}

function isAbnormalRow(row) {
  return row?.abnormalFlag === true || String(row?.abnormalFlag) === "1";
}

function getRowClassName({row}) {
  return isAbnormalRow(row) ? "abnormal-row" : "";
}

function getQualityFieldIds(row, config = getRowRuleConfig(row)) {
  if (row.ruleType === "UNIQUE") {
    const configuredIds = parseFieldIds(config.fieldIdList);
    const legacyConfigFieldIds = getLegacyConfigFieldIds(config);
    return configuredIds.length
        ? configuredIds
        : (legacyConfigFieldIds.length ? legacyConfigFieldIds : parseFieldIds(row.modelFieldId));
  }
  if (row.ruleType === "FIELD_LOGIC") {
    return [
      config.fieldId1 ?? config.modelFieldId1 ?? config.left,
      config.fieldId2 ?? config.modelFieldId2 ?? config.right,
    ]
        .filter((fieldId) => fieldId !== null && fieldId !== undefined && fieldId !== "");
  }
  return [config.fieldId ?? config.modelFieldId ?? parseFieldIds(row.modelFieldId)[0]].filter(
      (fieldId) => fieldId !== null && fieldId !== undefined && fieldId !== ""
  );
}

function formatNumberRangeDetail(config = qualityConfig) {
  const parts = [];
  const minVal = config.minVal ?? config.min;
  const maxVal = config.maxVal ?? config.max;
  if (minVal !== null && minVal !== undefined && String(minVal).trim() !== "") {
    parts.push(`${getRangeOperatorLabel(config.minOp, ">=")}${minVal}`);
  }
  if (maxVal !== null && maxVal !== undefined && String(maxVal).trim() !== "") {
    parts.push(`${getRangeOperatorLabel(config.maxOp, "<=")}${maxVal}`);
  }
  return parts.join(" 且 ") || "-";
}

function getRangeOperatorLabel(operator, fallback) {
  return {
    ">=": "≥",
    "<=": "≤",
  }[operator] || operator || fallback;
}

function getQualityFieldNames(row, config = getRowRuleConfig(row)) {
  return getQualityFieldIds(row, config)
      .map((fieldId) => getFieldName(fieldId, row.modelId));
}

function formatQualityFields(row) {
  const fieldNames = getQualityFieldNames(row)
      .filter((fieldName) => fieldName && fieldName !== "-");
  return fieldNames.join("、") || "-";
}

function formatRuleConfig(row) {
  const config = getRowRuleConfig(row);
  const fieldIds = getQualityFieldIds(row, config);
  const fieldNames = getQualityFieldNames(row, config);
  const firstFieldName = fieldNames[0] || "参与字段";
  const formatIsPreset = config.formatMode
      ? isPresetFormatMode(config.formatMode)
      : !config.regex;

  switch (row.ruleType) {
    case "NOT_EMPTY":
      return `${firstFieldName} 非空`;
    case "FORMAT":
      return `${firstFieldName} 格式校验（${formatIsPreset ? getFormatPresetLabel(config.preset) : (config.regex || "-")}）`;
    case "LENGTH":
      return `${firstFieldName} 长度 ${config.minLen ?? config.min ?? "-"}～${config.maxLen ?? config.max ?? "-"} 位`;
    case "NUMBER_RANGE":
      return `${firstFieldName} ${formatNumberRangeDetail(config)}`;
    case "REF_DATA_VALID":
      return `${firstFieldName} 参考数据合法（${getReferenceDataLabel(config.refDataSetCode || config.refSet)}）`;
    case "UNIQUE":
      return `${fieldNames.join("、") || "参与字段"} 组合唯一`;
    case "FIELD_LOGIC":
      return `${getFieldName(config.fieldId1 ?? config.modelFieldId1 ?? config.left ?? fieldIds[0], row.modelId)} ${config.compareOp || config.op || "-"} ${getFieldName(config.fieldId2 ?? config.modelFieldId2 ?? config.right ?? fieldIds[1], row.modelId)}`;
    default:
      return fieldNames.join("、") || "-";
  }
}

function validateQualityConfig() {
  const type = form.value.ruleType;
  const modelId = form.value.modelId;
  const fieldId = normalizeFieldId(qualityConfig.fieldId, modelId);

  if (SINGLE_FIELD_RULE_TYPES.has(type) && fieldId === null) {
    return "请选择参与字段";
  }
  if (SINGLE_FIELD_RULE_TYPES.has(type) && !getFieldOption(fieldId, modelId)) return "原配置字段不可用，请重新选择";
  if (type === "FORMAT") {
    if (isPresetFormatMode(qualityConfig.formatMode) && !qualityConfig.preset) {
      return "请选择预置格式";
    }
    if (!isPresetFormatMode(qualityConfig.formatMode)) {
      const regex = String(qualityConfig.regex || "").trim();
      if (!regex) return "请输入正则表达式";
      if (!isValidRegex(regex)) return "正则表达式格式不正确";
    }
  }
  if (type === "LENGTH") {
    const minLen = Number(qualityConfig.minLen);
    const maxLen = Number(qualityConfig.maxLen);
    if (!Number.isInteger(minLen) || !Number.isInteger(maxLen) || minLen < 0 || maxLen < 0) {
      return "长度必须为不小于 0 的整数";
    }
    if (minLen > maxLen) return "最小长度不能大于最大长度";
  }
  if (type === "NUMBER_RANGE") {
    const minVal = String(qualityConfig.minVal ?? "").trim();
    const maxVal = String(qualityConfig.maxVal ?? "").trim();
    if (!minVal && !maxVal) return "最小值、最大值至少填写一项";
    if (minVal && !Number.isFinite(Number(minVal))) return "最小值必须为数字";
    if (maxVal && !Number.isFinite(Number(maxVal))) return "最大值必须为数字";
    if (minVal && maxVal && Number(minVal) > Number(maxVal)) return "最小值不能大于最大值";
  }
  if (type === "REF_DATA_VALID" && !qualityConfig.refDataSetCode) {
    return "请选择参考数据集";
  }
  if (type === "REF_DATA_VALID" && !getReferenceDataOptions(modelId, fieldId).some(item => item.value === qualityConfig.refDataSetCode)) return "参考数据集不可用，请重新选择";
  if (type === "UNIQUE") {
    const fieldIds = qualityConfig.fieldIdList
        .map((fieldId) => normalizeFieldId(fieldId, modelId))
        .filter((fieldId) => fieldId !== null);
    if (!fieldIds.length) return "唯一性至少选择一个字段";
    if (fieldIds.some(id => !getFieldOption(id, modelId))) return "原配置字段不可用，请重新选择";
    if (new Set(fieldIds.map(String)).size !== fieldIds.length) return "唯一性字段不能重复";
  }
  if (type === "FIELD_LOGIC") {
    const fieldId1 = normalizeFieldId(qualityConfig.fieldId1, modelId);
    const fieldId2 = normalizeFieldId(qualityConfig.fieldId2, modelId);
    if (fieldId1 === null || fieldId2 === null) return "请选择两个比较字段";
    if (String(fieldId1) === String(fieldId2)) return "字段间逻辑不能选择相同字段";
    if (!areFieldsComparable(fieldId1, fieldId2, modelId)) return "字段间逻辑只能比较相同类型字段";
    if (!getComparisonOperators(getFieldType(fieldId1, modelId)).includes(qualityConfig.compareOp)) {
      return "当前字段类型不支持该比较符";
    }
  }
  return "";
}

function serializeRuleConfig() {
  const modelId = normalizeModelId(form.value.modelId);
  const type = form.value.ruleType;
  let config = {};
  let fieldIds = [];

  if (SINGLE_FIELD_RULE_TYPES.has(type)) {
    const modelFieldId = normalizeFieldId(qualityConfig.fieldId, modelId);
    fieldIds = [modelFieldId];
    config = {
      modelFieldId,
      matchMethod: type,
    };
  } else if (type === "UNIQUE") {
    fieldIds = qualityConfig.fieldIdList
        .map((fieldId) => normalizeFieldId(fieldId, modelId))
        .filter((fieldId) => fieldId !== null);
    config = {
      fieldList: fieldIds,
      matchMethod: type,
    };
  } else if (type === "FIELD_LOGIC") {
    const modelFieldId1 = normalizeFieldId(qualityConfig.fieldId1, modelId);
    const modelFieldId2 = normalizeFieldId(qualityConfig.fieldId2, modelId);
    fieldIds = [modelFieldId1, modelFieldId2];
    config = {
      modelFieldId1,
      compareOp: qualityConfig.compareOp,
      modelFieldId2,
      matchMethod: type,
    };
  }

  if (type === "LENGTH") {
    config.minLen = Number(qualityConfig.minLen);
    config.maxLen = Number(qualityConfig.maxLen);
  }
  if (type === "FORMAT") {
    config.formatMode = qualityConfig.formatMode;
    if (isPresetFormatMode(qualityConfig.formatMode)) {
      const presetOption = getFormatPresetOption(qualityConfig.preset);
      config.preset = presetOption?.label || qualityConfig.preset;
      config.regex = presetOption?.value || String(qualityConfig.regex || "").trim();
    } else {
      config.preset = "自定义正则";
      config.regex = String(qualityConfig.regex || "").trim();
    }
  }
  if (type === "NUMBER_RANGE") {
    config.minOp = qualityConfig.minOp;
    config.minVal = String(qualityConfig.minVal ?? "").trim();
    config.maxOp = qualityConfig.maxOp;
    config.maxVal = String(qualityConfig.maxVal ?? "").trim();
  }
  if (type === "REF_DATA_VALID") {
    config.refDataSetCode = qualityConfig.refDataSetCode;
  }

  form.value.modelId = modelId;
  form.value.modelFieldId = fieldIds.filter((fieldId) => fieldId !== null).map(String).join(",");
  form.value.ruleConfig = JSON.stringify(config);
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map((item) => item.id);
  multiple.value = !selection.length;
}

/** 改变启用状态 */
function handleStatusChange(row) {
  const nextStatus = row.validFlag;
  const previousStatus = !nextStatus;
  const text = nextStatus ? "启用" : "禁用";

  proxy.$modal
      .confirm(`确认要${text} "${row.ruleName}" 质量规则吗？`)
      .then(() => {
        statusLoading[row.id] = true;
        return updateRulequality({id: row.id, validFlag: nextStatus});
      })
      .then(() => {
        proxy.$modal.msgSuccess(`${text}成功`);
      })
      .catch(() => {
        row.validFlag = previousStatus;
      })
      .finally(() => {
        statusLoading[row.id] = false;
      });
}

/** 新增按钮操作 */
async function handleAdd() {
  reset();
  loadResponsibleUsers();
  open.value = true;
  title.value = "添加质量规则";
  if (form.value.modelId != null && !props.embedded) await handleModelChange();
}

function normalizeValidFlag(value) {
  if (value === null || value === undefined) return true;
  return value === true || value === 1 || value === "1" || value === "true";
}

async function openExistingRecord(row, dialogRef, dialogTitle) {
  reset();
  const id = row?.id ?? ids.value[0];
  if (!id) return;

  try {
    const {data: record} = await getRulequality(id);
    if (!props.embedded) {
      if (!modelTreeData.value.length) await reloadModelTree().catch(() => {
      });
      fieldsLoading.value = true;
      try {
        await loadModelFields(record.modelId, true);
      } catch {
        proxy.$modal.msgError("模型字段加载失败，原配置已保留，请重试");
      } finally {
        fieldsLoading.value = false;
      }
    }
    form.value = {
      ...record,
      responsibleUserId: record.responsibleUserId == null ? null : String(record.responsibleUserId),
      modelId: normalizeModelId(record.modelId),
      ruleType: record.ruleType || "NOT_EMPTY",
      validFlag: normalizeValidFlag(record.validFlag),
      abnormalFlag: record.abnormalFlag ?? "0",
    };
    syncModelLocation();
    if (!form.value.modelVersion) syncModelVersion();
    hydrateRuleConfig();
    dialogRef.value = true;
    title.value = dialogTitle;
  } catch {
    proxy.$modal.msgError("质量规则加载失败");
  }
}

/** 修改按钮操作 */
function handleUpdate(row) {
  loadResponsibleUsers();
  openExistingRecord(row, open, "修改质量规则");
}

/** 详情按钮操作 */
function handleDetail(row) {
  router.push({path: "/rule/quality/detail", query: {id: row.id}});
}

/** 提交按钮 */
function submitForm() {
  if (fieldsLoading.value) return;
  if (!props.embedded && failedModelFields.has(String(form.value.modelId))) {
    proxy.$modal.msgWarning("模型字段加载失败，请重新选择模型重试");
    return;
  }
  proxy.$refs["rulequalityRef"].validate(valid => {
    if (valid) {
      const qualityError = validateQualityConfig();
      if (qualityError) {
        proxy.$modal.msgWarning(qualityError);
        return;
      }
      serializeRuleConfig();
      const payload = {
        id: form.value.id ?? null,
        ruleCode: form.value.ruleCode,
        ruleName: form.value.ruleName,
        responsibleUserId: form.value.responsibleUserId,
        // 规则归属：详情内保存时使用当前模型ID。
        modelId: props.embedded ? props.modelId : normalizeModelId(form.value.modelId),
        modelVersion: form.value.modelVersion,
        modelFieldId: String(form.value.modelFieldId || ""),
        validFlag: normalizeValidFlag(form.value.validFlag),
        abnormalFlag: "0",
        ruleType: form.value.ruleType,
        ruleConfig: form.value.ruleConfig,
        description: form.value.description,
      };
      const request = payload.id != null
          ? updateRulequality(payload)
          : addRulequality(payload);
      request.then(() => {
        if (payload.id != null) {
          proxy.$modal.msgSuccess("修改成功");
        } else {
          proxy.$modal.msgSuccess("新增成功");
        }
        open.value = false;
        getList();
      }).catch(() => {
      });
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row?.id ?? ids.value;
  if (!_ids || (Array.isArray(_ids) && !_ids.length)) return;
  proxy.$modal.confirm('是否确认删除质量规则编号为"' + _ids + '"的数据项？').then(function () {
    return delRulequality(_ids);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {
  });
}

const selectedModelOption = computed(() => modelOptions.value.find(item => String(item.value) === String(form.value.modelId)));
</script>
<style scoped lang="scss">
.model-info {
  min-width: 0;

  .model-info-title {
    display: block;
    cursor: pointer;
    color: var(--el-color-primary);
  }

  :deep(.qt-table-info__main > .dict-tag-container) {
    flex-shrink: 0;
  }
}

.quality-range-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quality-range-row .el-input {
  flex: 1;
  min-width: 0;
  width: auto;
}
.quality-range-op {
  width: 72px;
  flex: 0 0 72px;
}
.quality-range-op-readonly {
  box-sizing: border-box;
  width: 72px;
  flex: 0 0 72px;
  justify-content: center;
}
</style>
