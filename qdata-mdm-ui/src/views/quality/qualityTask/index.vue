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
  <div class="app-container quality-task" ref="app-container">
    <GuideTip tip-id="quality/task.list"/>
    <el-container>
      <qt-tree
          ref="modelTreeRef"
          :data="modelTreeData"
          :action-loading="modelTreeLoading"
          :use-builtin-cat-edit-dialog="false"
          :editable="false"
          node-key="treeKey"
          title="主数据模型"
          placeholder="搜索名称或编码"
          @node-click="handleModelTreeClick"
      />
      <el-main class="main-content quality-task-main">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #search>
            <qt-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef"
                           @reset="resetTreeSelection"
/>
          </template>
          <template #actions-data>
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['quality:qualitytask:add']">
              新增
            </el-button>
            <!--        <span class="muted toolbar-tip">仅检测权威主数据 ｜ 规则随执行时状态读取</span>-->
          </template>
          <qt-table v-bind="tableStore" ref="tableRef">
            <template #location="{ row }">
              <div v-if="modelInfo(row).projectName || modelInfo(row).layers?.length"
                   class="qt-table-info flex-column-gap overflow-hidden"
>
                <div class="qt-table-info__main">
                  <img :src="getProjectIcon(modelInfo(row).projectIcon)" alt="" class="info-icon"/>
                  <span class="text-ellipsis" :title="modelInfo(row).projectName">{{
                      modelInfo(row).projectName || '-'
                    }}</span>
                </div>
                <div v-if="modelInfo(row).layers?.length" class="text-ellipsis" :title="modelDirectory(row)">
                  <dict-tag size="small" type="primary" :value="modelDirectory(row)"/>
                </div>
              </div>
              <span v-else class="muted">-</span>
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(modelInfo(row))" alt="" class="info-icon"/>
                  <span
                      class="info-title model-info-title text-ellipsis"
                      :role="canOpenModel(row) ? 'link' : undefined"
                      :tabindex="canOpenModel(row) ? 0 : undefined"
                      @click="openModelDetail(row)"
                      @keydown.enter="openModelDetail(row)"
                      @keydown.space.prevent="openModelDetail(row)"
                  >{{ modelTitle(row) }}</span>
                  <dict-tag v-if="modelInfo(row).type" :options="model_type" :value="modelInfo(row).type" size="small"
                            type="primary"
/>
                  <dict-tag
                      v-if="getModelVersionNo(row)"
                      class="ml4"
                      :class="{ pointer: canOpenModel(row) }"
                      :type="getVersionTagType(row)"
                      size="small"
                      :role="canOpenModel(row) ? 'link' : undefined"
                      :tabindex="canOpenModel(row) ? 0 : undefined"
                      @click="openModelDetail(row, 'versions')"
                      @keydown.enter="openModelDetail(row, 'versions')"
                      @keydown.space.prevent="openModelDetail(row, 'versions')"
                  >{{ getModelVersionNo(row) }}
                  </dict-tag>
                </div>
                <div class="info-desc text-ellipsis">{{ modelInfo(row).description || '-' }}</div>
              </div>
            </template>
            <template #lastExecution="{ row }">
              <div class="flex-column flex-column-gap overflow-hidden">
                <div>
                  <el-tag v-if="row.lastRunStatus" :type="resultType(row.lastRunStatus)" size="small" effect="plain">
                    {{ resultLabel(row.lastRunStatus) }}
                  </el-tag>
                  <span v-else class="muted">尚未执行</span>
                </div>
                <span class="desc-text">{{
                    row.lastRunTime ? proxy.parseTime(row.lastRunTime, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
                  }}</span>
                <span class="desc-text">耗时：{{ durationText(row.lastRunDurationMs) }}</span>
              </div>
            </template>
            <template #runControl="{ row }">
              <div class="flex-column task-controls">
                <div class="task-control">
                  <span class="black-label">任务</span>
                  <el-switch v-model="row.status" active-value="1" inactive-value="2" :loading="statusLoading[row.id]"
                             :disabled="!proxy.$auth.hasPermi('quality:qualitytask:edit') || scheduleLoading[row.id]"
                             :aria-label="`${row.taskName}任务状态`" @change="changeStatus(row)"
                  />
                </div>
                <div class="task-control">
                  <span class="black-label">调度</span>
                  <el-switch v-model="row.scheduleFlag" active-value="1" inactive-value="0"
                             :loading="scheduleLoading[row.id]"
                             :disabled="!proxy.$auth.hasPermi('quality:qualitytask:edit') || statusLoading[row.id]"
                             :aria-label="`${row.taskName}调度开关`" @change="changeSchedule(row)"
                  />
                </div>
              </div>
            </template>
            <template #scopeSchedule="{ row }">
              <div class="flex-column task-schedule overflow-hidden">
                <div>
                  <dict-tag :options="dataScopeTypeOptions" :value="row.dataScopeType" size="small"/>
                </div>
                <div class="flex-center overflow-hidden" :title="scheduleText(row)">
                  <el-icon class="schedule-clock">
                    <Clock/>
                  </el-icon>
                  <span class="text-ellipsis cron-text">{{ scheduleText(row) }}</span>
                </div>
              </div>
            </template>
            <template #responsiblePerson="{ row }">
              <!-- 这里只保存用户 ID，姓名和电话统一从公共用户列表取，列表每行不单独请求。 -->
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)">{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text">{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #creator="{ row }">
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)">{{
                    row.createBy || getResponsibleName(row.creatorId) || '-'
                  }}</span>
                <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text"
                      :title="getResponsiblePhone(row.creatorId)"
>{{ getResponsiblePhone(row.creatorId) }}</span>
              </div>
            </template>
            <template #action="{ row }">

              <el-button link type="primary" icon="View" @click="handleDetail(row)"
                         v-hasPermi="['quality:qualitytask:query']"
              >
                详情
              </el-button>

              <el-button link type="primary" icon="Edit" @click="openRecord(row, false)"
                         v-hasPermi="['quality:qualitytask:edit']"
              >修改
              </el-button>

              <el-popover placement="bottom" :width="130" trigger="click">
                <template #reference>
                  <el-button link type="primary" icon="ArrowDown">更多</el-button>
                </template>
                <div class="butgdlist">
                  <el-button link type="primary" icon="Clock" @click="runHistoryRef.open(row, modelLabel(row.modelId))"
                             v-hasPermi="['quality:qualitytaskinstance:list']"
                  >运行记录
                  </el-button>
                  <el-button
                      v-if="proxy.$auth.hasPermiAnd(['quality:qualitytask:edit', 'quality:qualitytaskinstance:add'])"
                      link
                      type="primary" icon="VideoPlay" :loading="executionLoading[row.id]"
                      :disabled="String(row.status) !== '1' || String(row.lastRunStatus) === '1'"
                      @click="executeOnce(row, false)"
                  >执行一次
                  </el-button>
                  <el-button link type="danger" icon="Delete" @click="handleDelete(row)"
                             v-hasPermi="['quality:qualitytask:remove']"
                  >删除
                  </el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>
    <el-dialog v-model="open" :title="readonly ? '质量任务详情' : form.id ? '修改质量任务' : '新增'"
               :width="readonly ? '800px' : '1000px'" top="5vh" append-to-body :append-to="$refs['app-container']"
               class="task-dialog dialog" draggable destroy-on-close
               @closed="invalidateRules"
    >
      <div v-if="readonly" class="task-detail-page">
        <div class="detail-hero">
          <div class="detail-mark">质</div>
          <div>
            <h2>{{ form.taskName || '质量任务详情' }}
              <el-tag type="success" effect="plain">{{ String(form.status) === '1' ? '启用' : '停用' }}</el-tag>
            </h2>
            <small>{{ form.taskCode || '-' }}</small></div>
          <div class="detail-actions">
            <el-button @click="readonly=false">修改</el-button>
            <el-button type="primary" @click="executeOnce(form)">执行一次</el-button>
          </div>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="任务编码">{{ form.taskCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主数据模型">{{ modelLabel(form.modelId) }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">{{
              String(form.status) === '1' ? '启用' : '停用'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="检测范围">{{
              String(form.dataScopeType) === '2' ? '条件筛选数据' : '全部数据'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="定时周期">{{ form.scheduleExpression || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前规则">{{ qualityRules.length }} 条可执行规则</el-descriptions-item>
          <el-descriptions-item label="负责人">{{
              getResponsibleName(form.responsibleUserId) || '-'
            }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
              getResponsiblePhone(form.responsibleUserId) || '-'
            }}
          </el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="detailTab" class="detail-tabs">
          <el-tab-pane label="质量规则" name="rules">
            <el-table :data="qualityRules" border>
              <el-table-column label="规则编码" prop="ruleCode"/>
              <el-table-column label="规则名称" prop="ruleName"/>
              <el-table-column label="规则类型" prop="ruleType"/>
              <el-table-column label="校验字段" prop="modelFieldId"/>
              <el-table-column label="状态" align="center" header-align="center">
                <template #default>
                  <el-tag type="success" effect="plain">启用</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="运行记录" name="history">
            <el-empty description="请点击运行记录查看执行明细"/>
            <el-button link type="primary" @click="runHistoryRef.open(form, modelLabel(form.modelId))">查看运行记录
            </el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
      <el-form v-else ref="formRef" :model="form" :rules="validation" label-width="110px"
               class="column-form task-form" :disabled="readonly || saving" scroll-to-error @submit.prevent
      >
        <div class="h2-title row-full">基础信息</div>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" maxlength="512"
                    placeholder="请输入任务名称,支持中英文、数字、下划线和短横线。"
/>
        </el-form-item>
        <el-form-item label="主数据域" required>
          <el-select
              v-model="modelProjectId"
              placeholder="请选择主数据域"
              :loading="modelTreeLoading"
              clearable
              filterable
              @change="handleProjectChange"
          >
            <el-option
                v-for="project in projectOptions"
                :key="project.value"
                :label="project.label"
                :value="project.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="模型目录" required>
          <el-tree-select
              v-model="modelLayerId"
              :data="layerOptions"
              :placeholder="modelProjectId == null ? '请先选择主数据域' : '请选择模型目录'"
              :disabled="modelProjectId == null"
              check-strictly
              clearable
              filterable
              @change="handleLayerChange"
          />
        </el-form-item>
        <el-form-item label="主数据模型" prop="modelId">
          <el-select
              v-model="form.modelId"
              :placeholder="modelLayerId == null ? '请先选择模型目录' : '请选择主数据模型'"
              :disabled="modelLayerId == null"
              :loading="modelTreeLoading"
              @change="changeModel"
          >
            <template #label="{ label }">
              <div v-if="selectedModelOption" class="source-option">
                <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ selectedModelOption.label }}</span>
              </div>
              <span v-else>{{ label }}</span>
            </template>
            <el-option v-for="model in filteredModelOptions" :key="model.value" :label="model.label"
                       :value="model.value"
>
              <div class="source-option">
                <img :src="getModelIcon(model)" alt="" class="source-option__icon"/>
                <span class="text-ellipsis">{{ model.label }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="负责人" prop="responsibleUserId">
          <el-select v-model="form.responsibleUserId" placeholder="请选择负责人" filterable clearable>
            <el-option v-for="user in responsibleUserOptions" :key="user.value" :label="user.label"
                       :value="user.value"
/>
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input :model-value="getResponsiblePhone(form.responsibleUserId)" disabled
                    placeholder="选择负责人后显示联系电话"
/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="2">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="检测范围" prop="dataScopeType">
          <el-radio-group v-model="form.dataScopeType">
            <el-radio label="1">全部权威主数据</el-radio>
            <el-radio label="2">按条件筛选</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行方式" prop="executionMode">
          <el-radio-group v-model="form.executionMode">
            <el-radio label="manual">手动执行</el-radio>
            <el-radio label="cron">Cron 表达式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.executionMode === 'cron'" label="调度周期" prop="scheduleExpression"
                      class="schedule-field" required
>
          <el-input v-model="form.scheduleExpression" maxlength="120" placeholder="请输入调度周期">
            <template #append>
              <el-button type="primary" @click="openSchedulePicker">配置</el-button>
            </template>
          </el-input>
        </el-form-item>
        <div v-if="form.dataScopeType === '2'" class="condition-panel row-full">
          <template v-if="form.legacyScope">
            <el-alert title="此任务使用旧版筛选配置，保存时将保留原配置。重新配置后可使用多条件筛选。" type="warning"
                      :closable="false"
            />
            <el-input :model-value="form.legacyScope" type="textarea" :rows="3" readonly class="legacy-scope"/>
            <el-button v-if="!readonly" @click="resetConditions">重新配置条件</el-button>
          </template>
          <template v-else>
            <div class="condition-actions btn-style">
              <el-button type="primary" plain size="small" icon="Plus" :disabled="!form.modelId"
                         @click="form.conditions.push(newCondition())"
>添加条件
              </el-button>
              <span class="muted">字段类型变化时自动匹配可用条件。</span>
            </div>
            <div class="condition-heading">
              <span>条件关系</span>
              <el-select v-model="form.conditionLogic" class="condition-logic">
                <el-option label="满足全部条件（且）" value="AND"/>
                <el-option label="满足任一条件（或）" value="OR"/>
              </el-select>
              <span class="muted">按模型业务字段筛选</span>
            </div>
            <div v-for="(condition, index) in form.conditions" :key="index" class="condition-row">
              <el-select v-model="condition.fieldId" placeholder="请选择字段"
                         :disabled="!form.modelId || modelFieldsLoading"
                         :loading="modelFieldsLoading"
                         @change="changeConditionField(condition)"
              >
                <el-option v-for="field in conditionFields" :key="field.value" :label="field.name"
                           :value="field.value"
                />
              </el-select>
              <el-select v-model="condition.operator" placeholder="请选择运算符" @change="condition.value = ''">
                <el-option v-for="operator in conditionOperators(condition)" :key="operator.value"
                           :label="operator.label" :value="operator.value"
                />
              </el-select>
              <el-input v-model="condition.value" :disabled="isEmptyOperator(condition.operator)"
                        :placeholder="conditionPlaceholder(condition)"
              />
              <el-button icon="Close" :aria-label="`删除第${index + 1}条条件`"
                         @click="form.conditions.splice(index, 1)"
              />
            </div>
          </template>
        </div>
        <!-- 描述沿用模型定义新增表单的整行多行输入样式及字数提示。 -->
        <qt-form-item label="描述" prop="description" class="row-full">
          <el-input
              v-model="form.description"
              type="textarea"
              maxlength="256个字符"
              show-word-limit
              :min-height="192"
              placeholder="请输入描述"
          />
        </qt-form-item>
        <div class="h2-title row-full">质量规则</div>
        <el-alert v-if="rulesError" class="row-full" title="质量规则加载失败，请重试" type="error" :closable="false">
          <el-button link type="primary" :disabled="false" @click="loadRules">重新加载</el-button>
        </el-alert>
        <qt-wrap class="row-full" :config="{ fullContent: false, actions: { table: { show: false } } }">
          <template #actions-data>
            <el-button plain type="primary" icon="Edit" @click="goQualityRules">前往质量规则维护</el-button>
          </template>
          <el-table v-loading="rulesLoading" :data="qualityRules" class="rules-table" max-height="320">
            <template #empty>
              <div class="emptyBg rules-empty">
                <img src="@/assets/common/images/empty-state/empty-nodata.png" alt=""/>
                <p>{{ form.modelId ? '该模型暂无可执行质量规则' : '请先选择主数据模型' }}</p>
              </div>
            </template>
            <el-table-column label="编号" prop="id" width="60" align="left" sortable/>
            <el-table-column label="规则编码" prop="ruleCode" width="140" show-overflow-tooltip/>
            <el-table-column label="规则名称" prop="ruleName" min-width="180" show-overflow-tooltip/>
            <el-table-column label="规则类型" width="130" show-overflow-tooltip>
              <template #default="{ row }">
                {{ ruleTypeOptions.find(item => item.value === row.ruleType)?.label || row.ruleType || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="校验字段" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ fieldLabel(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center" header-align="center">
              <template #default>
                <el-tag type="success" effect="plain">启用</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </qt-wrap>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="open = false">{{ readonly ? '关 闭' : '取 消' }}</el-button>
          <el-button v-if="!readonly" type="primary" :loading="saving" :disabled="modelFieldsLoading"
                     @click="submitForm"
>
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
    <RunHistoryDialog ref="runHistoryRef"/>

    <el-dialog v-model="openCron" title="Cron 表达式生成器" width="860px" :append-to="$refs['app-container']"
               destroy-on-close
>
      <Crontab v-if="openCron" :expression="cronExpression" @hide="openCron = false" @fill="fillSchedule"/>
    </el-dialog>
  </div>
</template>

<script setup name="Qualitytask">
import {ref, reactive, computed, getCurrentInstance, nextTick, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import Crontab from '@/components/Crontab/index.vue';
import QtTree from '@/components/QtTree/index.vue';
import RunHistoryDialog from '@/views/quality/qualityTaskInstance/RunHistoryDialog.vue';
import {executeQualitytask} from '@/api/quality/qualityTaskInstance/qualitytaskinstance';
import {cronToZh} from '@/utils/cronUtils';
import {Clock} from '@element-plus/icons-vue';
import {getModelIcon, getProjectIcon} from '@/utils/imageUrl';
import {useDict} from '@/utils/dict';

const {model_type, model_status} = useDict('model_type', 'model_status');
import {useUserDirectory} from '@/store/system/userDirectory';
import {trimText, validatePattern, validateTrimmedRequired} from '@/utils/validate.js';

import {
  listQualitytask,
  getQualitytask,
  addQualitytask,
  updateQualitytask,
  delQualitytask
} from '@/api/quality/qualityTask/qualitytask';
import {listRulequality} from '@/api/rule/quality/quality';
import {getBase, getPublishedModelTree, getPublishedModelStructure} from '@/api/model/base/base';

const {proxy} = getCurrentInstance();
const {rule_rulequality_type} = proxy.useDict(
    "rule_rulequality_type"
);
const modelOptions = reactive([]);
const modelTreeRef = ref();
const modelTreeData = ref([]);
const modelTreeLoading = ref(false);
const selectedTreeNode = ref(null);
const modelProjectId = ref(null);
const modelLayerId = ref(null);
const modelFieldsLoading = ref(false);
const modelMetadata = reactive({});
const modelMetadataRequests = new Map();
let modelFieldsRequest = 0;
const ruleTypeOptions = rule_rulequality_type.value;

function normalizeModelTree(nodes = [], context = {}) {
  return nodes.map(node => {
    const projectName = node.nodeType === 'project' ? node.name : context.projectName || '';
    const layerPath = node.nodeType === 'layer'
        ? [...(context.layerPath || []), node.name]
        : (context.layerPath || []);
    return {
      ...node,
      projectName,
      layerPath,
      treeKey: `${node.nodeType}_${node.id}`,
      children: normalizeModelTree(node.children || [], {projectName, layerPath})
    };
  });
}

function collectModelNodes(nodes = []) {
  const models = new Map();

  function collect(items) {
    for (const node of items) {
      if (node.nodeType === 'model' && node.modelId != null) {
        models.set(String(node.modelId), node);
      }
      collect(node.children || []);
    }
  }

  collect(nodes);
  return [...models.values()];
}

const projectOptions = computed(() => modelTreeData.value
    .filter(node => node.nodeType === 'project')
    .map(node => ({
      label: node.name || node.code || String(node.projectId),
      value: node.projectId
    })));

const layerOptions = computed(() => {
  const project = modelTreeData.value.find(node =>
      node.nodeType === 'project' && String(node.projectId) === String(modelProjectId.value)
  );
  const toOptions = nodes => (nodes || [])
      .filter(node => node.nodeType === 'layer')
      .map(node => ({
        label: node.name || node.code || String(node.layerId),
        value: node.layerId,
        children: toOptions(node.children)
      }));
  return toOptions(project?.children);
});

const filteredModelOptions = computed(() => {
  if (modelProjectId.value == null || modelLayerId.value == null) return [];
  return modelOptions.filter(model =>
      String(model.projectId) === String(modelProjectId.value)
      && String(model.layerId) === String(modelLayerId.value)
  );
});

const dataScopeTypeOptions = [
  {label: "全部权威主数据", value: "1", elTagType: "primary"},
  {label: "按条件筛选", value: "2", elTagType: "primary"}
];
// dataScopeConfig: JSON.stringify({ logic: 'AND' | 'OR', conditions: [{ fieldId, operator, value }] })
// IN/NOT_IN 使用数组，IS_NULL/IS_NOT_NULL 使用 null，其余条件值使用字符串。
const operators = [
  {value: 'EQ', label: '等于'}, {value: 'NE', label: '不等于'},
  {value: 'STARTS_WITH', label: '开头为'}, {value: 'ENDS_WITH', label: '结尾为'},
  {value: 'CONTAINS', label: '包含'}, {value: 'NOT_CONTAINS', label: '不包含'},
  {value: 'IS_NULL', label: '为空'}, {value: 'IS_NOT_NULL', label: '不为空'},
];
const isEmptyOperator = operator => ['IS_NULL', 'IS_NOT_NULL'].includes(operator);
const isListOperator = operator => ['IN', 'NOT_IN'].includes(operator);
const splitValues = value => String(value ?? '').split(/[,，\n]/).map(item => item.trim()).filter(Boolean);
const newCondition = () => ({fieldId: '', operator: 'EQ', value: ''});

function availableOperators(field) {
  const excluded = ['number', 'decimal', 'int', 'integer', 'long', 'float', 'double', 'date'].includes(field?.type)
      ? ['CONTAINS', 'NOT_CONTAINS']
      : field?.type === 'reference' ? ['CONTAINS', 'NOT_CONTAINS', 'GT', 'GE', 'LT', 'LE'] : ['GT', 'GE', 'LT', 'LE'];
  return operators.filter(item => !excluded.includes(item.value));
}

function validateConditions(conditions, fields) {
  if (!conditions?.length) return '请至少添加一个筛选条件';
  for (const [index, row] of conditions.entries()) {
    const field = fields.find(item => String(item.value) === String(row.fieldId));
    const prefix = `第 ${index + 1} 条条件：`;
    if (!field) return prefix + '请选择当前模型的字段';
    if (!availableOperators(field).some(item => item.value === row.operator)) return prefix + '请选择有效的运算符';
    if (isEmptyOperator(row.operator)) continue;
    const values = isListOperator(row.operator) ? splitValues(row.value) : [String(row.value ?? '').trim()].filter(Boolean);
    if (!values.length) return prefix + '请输入条件值';
    if (['number', 'decimal', 'int', 'integer', 'long', 'float', 'double'].includes(field.type)
        && values.some(value => !Number.isFinite(Number(value)))) return prefix + '请输入有效数字';
    if (field.type === 'date' && values.some(value => !/^\d{4}-\d{2}-\d{2}$/.test(value) || Number.isNaN(Date.parse(value)) || new Date(value).toISOString().slice(0, 10) !== value)) return prefix + '请输入有效日期（YYYY-MM-DD）';
  }
  return '';
}

function serializeConditions(logic, conditions) {
  return JSON.stringify({
    logic, conditions: conditions.map(row => ({
      fieldId: row.fieldId, operator: row.operator,
      value: isEmptyOperator(row.operator) ? null : isListOperator(row.operator) ? splitValues(row.value) : String(row.value ?? '').trim(),
    }))
  });
}

function hydrateConditions(raw) {
  if (!raw) return {conditionLogic: 'AND', conditions: [newCondition()], legacyScope: ''};
  try {
    const config = JSON.parse(raw);
    if (['AND', 'OR'].includes(config?.logic) && Array.isArray(config.conditions) && config.conditions.every(row => row && typeof row === 'object')) {
      return {
        conditionLogic: config.logic, conditions: config.conditions.map(row => ({
          fieldId: row.fieldId, operator: row.operator,
          value: Array.isArray(row.value) ? row.value.join(', ') : String(row.value ?? ''),
        })), legacyScope: ''
      };
    }
  } catch { /* 历史 where 配置保留原文，不静默覆盖 */
  }
  return {conditionLogic: 'AND', conditions: [], legacyScope: raw};
}

const {responsibleUserList, getResponsibleName, getResponsiblePhone} = useUserDirectory();
const route = useRoute();
const router = useRouter();
const canViewModels = computed(() => proxy.$auth.hasPermi('model:base:query'));
const tableRef = ref();
const runHistoryRef = ref();
const executionLoading = reactive({});

async function executeOnce(row, showResult = true) {
  if (executionLoading[row.id] || String(row.status) !== '1') return;
  executionLoading[row.id] = true;
  try {
    const {data} = await executeQualitytask(row.id);
    if (showResult || String(data.status) === '2') {
      runHistoryRef.value.showResult(data);
    } else {
      proxy.$modal.msgSuccess(Number(data.failCount) > 0 ? `检测完成，发现 ${data.failCount} 条问题数据` : '检测完成');
    }
    tableRef.value?.getList();
  } finally {
    executionLoading[row.id] = false;
  }
}

const formRef = ref();
const open = ref(false);
const readonly = ref(false);
const saving = ref(false);
const form = ref({});
const responsibleUserOptions = computed(() => {
  const users = responsibleUserList.value;
  const selected = form.value.responsibleUserId;
  if (selected && !users.some(user => user.value === String(selected))) {
    return [{value: String(selected), label: `用户 ${selected}（已不可用）`, phone: ''}, ...users];
  }
  return users;
});
const openCron = ref(false);
const cronExpression = ref('');

function isCronExpression(value) {
  return [6, 7].includes(String(value || '').trim().split(/\s+/).length);
}

function openSchedulePicker() {
  cronExpression.value = isCronExpression(form.value.scheduleExpression) ? form.value.scheduleExpression : '';
  openCron.value = true;
}

function fillSchedule(value) {
  form.value.scheduleExpression = value.trim();
  openCron.value = false;
  nextTick(() => formRef.value?.validateField('scheduleExpression'));
}

const qualityRules = ref([]);
const rulesLoading = ref(false);
const rulesError = ref(false);
const statusLoading = reactive({});
const scheduleLoading = reactive({});
let rulesRequest = 0;
const tableStore = reactive({
    config: {
        table: {
            stripe: true,
            rowKey: 'id',
            defaultSort: { prop: 'createTime', order: 'descending' }
        }
    },
columns: [
    {label: '编号', prop: 'id', width: 70, sortable: true},
    {
      label: '质量任务信息', prop: 'taskName', align: 'left', minWidth: 240,
      info: {
        title: "taskName",
        desc: "remark",
        click: (row) => handleDetail(row),
      },
      showOverflowTooltip: {effect: 'light'},
    },
    {
      label: '主数据模型',
      prop: 'modelId',
      slot: 'modelInfo',
      align: 'left',
      minWidth: 380,
      showOverflowTooltip: {effect: 'light'}
    },
    {label: '归属位置', slot: 'location', align: 'left', minWidth: 190},
    {label: '最近执行信息', slot: 'lastExecution', align: 'left', minWidth: 205},
    {label: '运行控制', prop: 'status', slot: 'runControl', align: 'left', width: 130},
    {label: '检测范围与调度', prop: 'scheduleConfig', slot: 'scopeSchedule', align: 'left', minWidth: 210},
    {label: '负责人', prop: 'responsibleUserId', slot: 'responsiblePerson', align: 'left', width: 130},
    {label: '创建人', prop: 'createBy', slot: 'creator', align: 'left', width: 130},
    {label: "创建时间", prop: "createTime", minWidth: 180, date: true, sortable: true, sortableKey: "create_time"},
    {label: '操作', slot: 'action', width: 220, fixed: 'right'},

  ],
  func: listQualitytask,
  events: {
    formatData: rows => {
      rows.forEach(row => loadModelMetadata(row.modelId));
      return rows.map(row => ({...row, status: String(row.status), scheduleFlag: String(row.scheduleFlag || '0')}));
    }
  },
  params: {
    taskName: undefined,
    responsibleUserId: undefined,
    modelId: undefined,
    modelIds: undefined,
    status: undefined,
    orderByColumn: 'create_time',
    isAsc: 'descending'
  },
});
const searchStore = computed(() => ({
  items: [
    {label: '任务名称', prop: 'taskName', component: {is: 'input', placeholder: '请输入任务名称'}},
    {
      label: '负责人',
      prop: 'responsibleUserId',
      component: {is: 'select', filterable: true, clearable: true, options: responsibleUserList.value}
    },
    {
      label: '主数据模型',
      prop: 'modelId',
      component: {is: 'select', placeholder: '全部', options: modelOptions, onChange: handleModelSelect}
    },
    {
      label: '任务状态',
      prop: 'status',
      component: {
        is: 'select',
        placeholder: '全部',
        options: [{label: '启用', value: '1'}, {label: '停用', value: '2'}]
      }
    },
  ]
}));
const validation = {
  taskName: [
    {required: true, validator: validateTrimmedRequired('任务名称不能为空', 512), trigger: 'blur'},
    {
      validator: validatePattern(
          /^[\u4e00-\u9fffA-Za-z0-9_-]+$/,
          '任务名称只允许中文、英文字母、数字、下划线和短横线'
      ),
      trigger: 'blur',
      transform: trimText
    }
  ],
  modelId: [{required: true, message: '请选择主数据模型', trigger: 'change'}],
  status: [{required: true, message: '请选择任务状态', trigger: 'change'}],
  dataScopeType: [{required: true, message: '请选择检测范围', trigger: 'change'}],
  executionMode: [{required: true, message: '请选择执行方式', trigger: 'change'}],
  scheduleExpression: [{
    validator: (_, value, callback) => callback(form.value.executionMode === 'cron' && !value?.trim() ? new Error('请输入调度周期') : undefined),
    trigger: 'blur'
  }],
};

async function goQualityRules() {
  // 菜单由后端动态注册，优先使用实际路由，兼容菜单路径的大小写和层级。
  const target = router.getRoutes().find(route =>
      String(route.name).toLowerCase() === 'qualitys'
      || /\/qualitys\/?$/i.test(route.path)
  );
  const failure = await router.push(target?.path || '/rule/qualitys');
  if (!failure) open.value = false;
}

function modelLabel(id) {
  return modelMetadata[id]?.name || modelOptions.find(item => String(item.value) === String(id))?.treeLabel || id || '-';
}

function modelInfo(row) {
  const option = modelOptions.find(item => String(item.value) === String(row.modelId)) || {};
  return {
    ...option, ...(modelMetadata[row.modelId] || {}),
    name: modelMetadata[row.modelId]?.name || option.treeLabel || modelLabel(row.modelId)
  };
}

function modelTitle(row) {
  const {name, code} = modelInfo(row);
  return name ? (code ? `${name} (${code})` : name) : code || '-';
}

function canOpenModel(row) {
  return canViewModels.value && row.modelId != null;
}

function getModelVersionNo(row) {
  const model = modelInfo(row);
  return model?.releaseVersion?.versionNo || model?.version?.versionNo || row.modelVersion;
}

function getVersionTagType(row) {
  const status = modelInfo(row)?.version?.status;
  return model_status.value.find(item => String(item.value) === String(status))?.elTagType || 'info';
}

function openModelDetail(row, tab) {
  if (!canOpenModel(row)) return;
  router.push({path: '/model/base/detail', query: {id: row.modelId, ...(tab ? {tab} : {})}});
}

function modelDirectory(row) {
  let list = (modelInfo(row).layers || []);
  return list[list.length - 1]?.name;
}

async function loadModelMetadata(id) {
  if (id == null || !proxy.$auth.hasPermi('model:base:query')) return null;
  if (modelMetadata[id]) return modelMetadata[id];
  if (!modelMetadataRequests.has(String(id))) {
    const request = getBase(id).then(({data}) => {
      if (data) modelMetadata[id] = data;
      return data;
    }).catch(() => null).finally(() => modelMetadataRequests.delete(String(id)));
    modelMetadataRequests.set(String(id), request);
  }
  return modelMetadataRequests.get(String(id));
}

async function loadModelTree() {
  modelTreeLoading.value = true;
  try {
    const {data} = await getPublishedModelTree();
    modelTreeData.value = normalizeModelTree(data || []);
    modelOptions.splice(0, modelOptions.length, ...collectModelNodes(modelTreeData.value).map(node => ({
      label: node.versionNo ? `${node.name}（${node.versionNo}）` : node.name,
      treeLabel: node.name,
      code: node.code,
      value: node.modelId,
      projectId: node.projectId,
      projectName: node.projectName,
      layerId: node.layerId,
      layerPath: node.layerPath || [],
      version: node.versionNo,
      treeKey: node.treeKey,
      fields: []
    })));
  } catch {
    modelTreeData.value = [];
  } finally {
    modelTreeLoading.value = false;
  }
}

function handleModelTreeClick(node) {
  selectedTreeNode.value = node;
  tableStore.params.modelId = node.nodeType === 'model' ? node.modelId : undefined;
  tableStore.params.modelIds = node.nodeType === 'model' ? undefined
      : collectModelNodes([node]).map(model => model.modelId);
  if (tableStore.params.modelIds?.length === 0) tableStore.params.modelIds = [-1];
  tableRef.value?.resetQuery();
}

function handleModelSelect(id) {
  tableStore.params.modelIds = undefined;
  selectedTreeNode.value = collectModelNodes(modelTreeData.value).find(node => String(node.modelId) === String(id)) || null;
  modelTreeRef.value?.setCurrentKey(selectedTreeNode.value?.treeKey ?? null, false);
}

function resetTreeSelection() {
  selectedTreeNode.value = null;
  tableStore.params.modelIds = undefined;
  modelTreeRef.value?.resetTree();
}

async function loadModelFields() {
  const request = ++modelFieldsRequest;
  modelFieldsLoading.value = false;
  const model = modelOptions.find(item => String(item.value) === String(form.value.modelId));
  if (!model) return;
  model.fields = [];
  modelFieldsLoading.value = true;
  try {
    const {data} = await getPublishedModelStructure(model.value);
    if (request !== modelFieldsRequest) return;
    model.fields = (data?.fields || []).map(field => ({
      label: field.name,
      name: field.name,
      code: field.code,
      type: field.dictTypeId != null ? 'reference' : String(field.dataType || '').toLowerCase().replace(/^datetime$/, 'date'),
      value: field.id
    }));
  } catch {
    // 请求失败时保持字段为空，避免沿用其他模型的条件字段。
  } finally {
    if (request === modelFieldsRequest) modelFieldsLoading.value = false;
  }
}

function configText(value, key) {
  if (!value) return '';
  try {
    const parsed = JSON.parse(value);
    return typeof parsed === 'string' ? parsed : parsed?.[key] ?? value;
  } catch {
    return value;
  }
}

function scheduleText(row) {
  if (String(row.scheduleFlag) !== '1') return '-';
  const expression = configText(row.scheduleConfig, 'expression');
  if (!expression) return '-';
  const fieldCount = String(expression).trim().split(/\s+/).length;
  return [6, 7].includes(fieldCount) ? cronToZh(expression) : expression;
}

function resultLabel(value) {
  return ({0: '尚未执行', 1: '执行中', 2: '执行失败', 3: '部分成功', 4: '执行成功'})[value] || '未知状态';
}

function durationText(value) {
  if (value == null || value === '' || !Number.isFinite(Number(value)) || Number(value) < 0) return '-';
  const milliseconds = Number(value);
  if (milliseconds < 1000) return `${milliseconds} 毫秒`;
  if (milliseconds < 60000) return `${Number((milliseconds / 1000).toFixed(1))} 秒`;
  const seconds = Math.floor(milliseconds / 1000);
  const minutes = Math.floor(seconds / 60);
  return minutes < 60 ? `${minutes} 分 ${seconds % 60} 秒` : `${Math.floor(minutes / 60)} 时 ${minutes % 60} 分 ${seconds % 60} 秒`;
}

function resultType(value) {
  return ({1: 'primary', 2: 'danger', 3: 'warning', 4: 'success', 0: ''})[value] || 'info';
}

function fieldLabel(row) {
  let ids = row.modelFieldId;
  try {
    ids = JSON.parse(ids);
  } catch { /* 兼容逗号分隔字段 */
  }
  ids = Array.isArray(ids) ? ids : String(ids ?? '').split(',').filter(Boolean);
  const fields = modelOptions.find(item => String(item.value) === String(row.modelId))?.fields || [];
  return ids.map(id => fields.find(field => String(field.value) === String(id) || field.code === id)?.name || id).join('、') || '-';
}

function invalidateRules() {
  rulesRequest++;
  rulesLoading.value = false;
  modelFieldsRequest++;
  modelFieldsLoading.value = false;
}

async function loadRules() {
  const request = ++rulesRequest;
  qualityRules.value = [];
  rulesError.value = false;
  rulesLoading.value = false;
  if (!form.value.modelId) return;
  rulesLoading.value = true;
  const modelId = form.value.modelId;
  try {
    const rows = [];
    let pageNum = 1;
    let total = 0;
    do {
      const response = await listRulequality({modelId, validFlag: true, abnormalFlag: '0', pageNum, pageSize: 100});
      if (request !== rulesRequest) return;
      const page = response.data.rows || [];
      rows.push(...page);
      total = Number(response.data.total || 0);
      if (!page.length) break;
      pageNum++;
    } while (rows.length < total);
    qualityRules.value = rows;
  } catch {
    if (request === rulesRequest) rulesError.value = true;
  } finally {
    if (request === rulesRequest) rulesLoading.value = false;
  }
}

const conditionFields = computed(() => modelOptions.find(item => String(item.value) === String(form.value.modelId))?.fields || []);

function conditionOperators(row) {
  return availableOperators(conditionFields.value.find(item => String(item.value) === String(row.fieldId)));
}

function changeConditionField(row) {
  if (!conditionOperators(row).some(item => item.value === row.operator)) row.operator = 'EQ';
  row.value = '';
}

function conditionPlaceholder(row) {
  if (isEmptyOperator(row.operator)) return '无需填写';
  const field = conditionFields.value.find(item => String(item.value) === String(row.fieldId));
  if (isListOperator(row.operator)) return field?.type === 'date' ? '多个日期用逗号分隔：2026-01-01, 2026-02-01' : '多个值用逗号分隔';
  return field?.type === 'date' ? '请输入日期：YYYY-MM-DD' : '请输入条件值';
}

function resetConditions() {
  form.value.conditionLogic = 'AND';
  form.value.conditions = [newCondition()];
  form.value.legacyScope = '';
}

function clearSelectedModel() {
  form.value.modelId = null;
  form.value.modelVersion = null;
  resetConditions();
  loadRules();
  loadModelFields();
}

function handleProjectChange() {
  modelLayerId.value = null;
  clearSelectedModel();
}

function handleLayerChange() {
  clearSelectedModel();
}

function syncModelLocation() {
  const model = modelOptions.find(item => String(item.value) === String(form.value.modelId));
  modelProjectId.value = model?.projectId ?? null;
  modelLayerId.value = model?.layerId ?? null;
}

function changeModel() {
  const model = modelOptions.find(item => String(item.value) === String(form.value.modelId));
  form.value.modelVersion = model?.version || null;
  if (model) {
    modelProjectId.value = model.projectId ?? null;
    modelLayerId.value = model.layerId ?? null;
  }
  resetConditions();
  loadRules();
  loadModelFields();
}

function handleAdd() {
  readonly.value = false;
  form.value = {
    taskCode: `QT${Date.now()}`,
    taskName: '',
    description: '',
    responsibleUserId: null,
    modelId: null,
    modelVersion: null,
    status: '1',
    dataScopeType: '1',
    executionMode: 'manual',
    conditionLogic: 'AND',
    conditions: [newCondition()],
    legacyScope: '',
    scheduleFlag: '0',
    scheduleExpression: '',
    validFlag: true
  };
  modelProjectId.value = selectedTreeNode.value?.projectId ?? null;
  modelLayerId.value = ['layer', 'model'].includes(selectedTreeNode.value?.nodeType)
      ? selectedTreeNode.value?.layerId ?? null
      : null;
  if (selectedTreeNode.value?.nodeType === 'model') {
    form.value.modelId = selectedTreeNode.value.modelId;
    form.value.modelVersion = selectedTreeNode.value.versionNo;
  }
  open.value = true;
  loadRules();
  loadModelFields();
  nextTick(() => formRef.value?.clearValidate());
}

async function openRecord(row, detail) {
  await modelsReady;
  const {data} = await getQualitytask(row.id);
  form.value = {
    ...data,
    description: data.remark || '',
    responsibleUserId: data.responsibleUserId == null ? null : String(data.responsibleUserId),
    modelId: modelOptions.find(item => String(item.value) === String(data.modelId))?.value ?? data.modelId,
    status: String(data.status),
    dataScopeType: String(data.dataScopeType || '1'),
    scheduleFlag: String(data.scheduleFlag || '0'), ...hydrateConditions(data.dataScopeConfig),
    scheduleExpression: configText(data.scheduleConfig, 'expression'),
    executionMode: String(data.scheduleFlag || '0') === '1' ? 'cron' : 'manual'
  };
  syncModelLocation();
  readonly.value = detail;
  open.value = true;
  loadRules();
  loadModelFields();
  nextTick(() => formRef.value?.clearValidate());
}

function handleDetail(row) {
  router.push({
    path: "/quality/qualityTask/detail",
    query: {id: row.id},
  });
}

const modelsReady = loadModelTree();

watch(() => route.query.edit, async (id) => {
  if (!id) return;
  await nextTick();
  await openRecord({id}, false);
  const query = {...route.query};
  delete query.edit;
  router.replace({path: route.path, query});
}, {immediate: true});

async function submitForm() {
  if (saving.value || modelFieldsLoading.value || !await formRef.value.validate().catch(() => false)) return;
  const {
    conditionLogic,
    conditions,
    legacyScope,
    scheduleExpression,
    description,
    executionMode,
    ...payload
  } = form.value;
  if (payload.dataScopeType === '2' && !legacyScope) {
    const error = validateConditions(conditions, conditionFields.value);
    if (error) {
      proxy.$modal.msgWarning(error);
      return;
    }
  }
  payload.taskName = payload.taskName.trim();
  payload.taskCode = String(payload.taskCode || `QT${Date.now()}`).trim();
  payload.remark = description?.trim() || '';
  payload.responsibleUserId = payload.responsibleUserId || null;
  payload.dataScopeConfig = payload.dataScopeType === '2' ? (legacyScope || serializeConditions(conditionLogic, conditions)) : '';
  payload.scheduleFlag = executionMode === 'cron' ? '1' : '0';
  payload.scheduleConfig = payload.scheduleFlag === '1' ? JSON.stringify({expression: scheduleExpression.trim()}) : '';
  if ((payload.dataScopeConfig?.length || 0) > 512 || (payload.scheduleConfig?.length || 0) > 512) {
    proxy.$modal.msgWarning('条件或周期配置过长，请缩短后保存');
    return;
  }
  saving.value = true;
  try {
    await (payload.id ? updateQualitytask(payload) : addQualitytask(payload));
    proxy.$modal.msgSuccess(payload.id ? '修改成功' : '新增成功');
    open.value = false;
    tableRef.value?.getList();
  } finally {
    saving.value = false;
  }
}

async function changeStatus(row) {
  const previous = row.status === '1' ? '2' : '1';
  statusLoading[row.id] = true;
  try {
    await updateQualitytask({id: row.id, status: row.status});
    proxy.$modal.msgSuccess('状态更新成功');
  } catch {
    row.status = previous;
  } finally {
    statusLoading[row.id] = false;
  }
}

async function changeSchedule(row) {
  const previous = row.scheduleFlag === '1' ? '0' : '1';
  if (row.scheduleFlag === '1' && !String(configText(row.scheduleConfig, 'expression')).trim()) {
    row.scheduleFlag = previous;
    proxy.$modal.msgWarning('请先在修改任务中配置执行周期');
    return;
  }
  scheduleLoading[row.id] = true;
  try {
    await updateQualitytask({id: row.id, scheduleFlag: row.scheduleFlag});
    proxy.$modal.msgSuccess('调度开关更新成功');
  } catch {
    row.scheduleFlag = previous;
  } finally {
    scheduleLoading[row.id] = false;
  }
}

async function handleDelete(row) {
  try {
    await proxy.$modal.confirm(`是否确认删除质量任务“${row.taskName}”？`);
    await delQualitytask(row.id);
    proxy.$modal.msgSuccess('删除成功');
    tableRef.value?.getList();
  } catch { /* 取消删除或请求失败时保留列表 */
  }
}

const selectedModelOption = computed(() => modelOptions.find(item => String(item.value) === String(form.value.modelId)));
</script>

<style scoped lang="scss">
.model-info {
  min-width: 0;

  .model-info-title {
    display: block;

    &[role='link'] {
      cursor: pointer;
      color: var(--el-color-primary);
    }
  }

  :deep(.qt-table-info__main > .dict-tag-container) {
    flex-shrink: 0;
  }
}

.quality-task-main {
  min-width: 0;
}

.quality-task :deep(.inline-field) {
  display: flex;
  align-items: flex-start;
}

.quality-task :deep(.inline-field > .el-form-item__label) {
  display: inline-flex;
  flex: 0 0 100px;
  width: 100px;
  height: 32px;
  line-height: 32px;
  justify-content: flex-end;
  padding: 0 12px 0 0;
  margin-bottom: 0;
}

.quality-task :deep(.inline-field > .el-form-item__content) {
  flex: 1;
  min-width: 0;
  margin-left: 0;
}

.quality-task :deep(.inline-field .el-select) {
  width: 100%;
}

.task-tip {
  margin-bottom: 14px;
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.task-control {
  display: flex;
  align-items: center;
  gap: 5px;
}

.task-controls {
  font-size: 12px;
  gap: 2px;
}

.task-schedule {
  font-size: 14px;
  gap: 7px;
}

.schedule-clock {
  flex-shrink: 0;
  margin-right: 5px;
  color: #3b82f6;
}

.toolbar-tip {
  margin-left: 16px;
}

.quality-task :deep(.task-dialog) {
  max-width: calc(100vw - 40px);
}

.task-form :deep(.el-form-item) {
  min-width: 0;
}

.task-form :deep(.el-form-item__content),
.task-form :deep(.el-select),
.task-form :deep(.el-input),
.task-form :deep(.el-textarea) {
  width: 100%;
  min-width: 0;
}

.task-form .condition-panel {
  margin-top: -2px;
}

.quality-task :deep(.task-detail-page) {
  padding: 4px 0 16px;
}

.quality-task :deep(.detail-hero) {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.quality-task :deep(.detail-mark) {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #3478ed;
  color: #fff;
  border-radius: 4px;
  font-weight: 600;
}

.quality-task :deep(.detail-hero h2) {
  margin: 0 0 3px;
  font-size: 18px;
  color: #303133;
}

.quality-task :deep(.detail-hero small) {
  color: #909399;
}

.quality-task :deep(.detail-actions) {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.quality-task :deep(.detail-tabs) {
  margin-top: 18px;
}

.quality-task :deep(.task-dialog .el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
}

.quality-task :deep(.rules-table .rules-empty img) {
  margin-top: 20px;
}

.condition-panel {
  padding: 16px;
  margin-left: 100px;
  margin-bottom: 20px;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-light);
}

.condition-panel .el-form-item {
  margin-bottom: 0;
}

.schedule-field :deep(.el-input-group__append) {
  padding: 0;
}

.schedule-field :deep(.el-input-group__append .el-button) {
  margin: 0;
  border: 0;
  border-radius: 0;
  color: var(--qt-text-inverse);
  background-color: var(--qt-primary);
}

.condition-heading, .condition-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.condition-heading .condition-logic {
  width: 200px;
}

.condition-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) minmax(0, 1.9fr) 32px;
  gap: 8px;
  margin-bottom: 10px;
}

.condition-row .el-select {
  width: 100%;
}

.condition-row .el-button {
  width: 32px;
  padding: 0;
}

.legacy-scope {
  margin: 12px 0;
}

.butgdlist .el-button {
  width: 100%;
  justify-content: flex-start;
  margin-left: 0;
  padding-left: 14px;
}

@media (max-width: 700px) {
  .condition-panel {
    margin-left: 0;
  }

  .condition-row {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
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
