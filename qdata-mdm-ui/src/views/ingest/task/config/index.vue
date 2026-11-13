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
  接入任务配置向导：维护基础信息、数据获取、唯一标识、字段映射及调度配置。
-->
<template>
  <div v-loading="loading" class="app-container task-config" ref="app-container">
    <detail-info v-if="readonly"
                 :data="form"
                 :header="{ nameKey: 'name', statusKey: 'status', statusOptions: TASK_STATUSES }"
                 :items="TASK_CONFIG_DETAIL_ITEMS"
    />
    <div v-else class="custom-card">
      <steps :steps="TASK_CONFIG_STEPS" :active-step="activeStep" @change="changeStep"/>
    </div>
    <div class="pagecont-top config-card" :class="{ 'detail-card': readonly }">
      <div class="infotop">
        <div class="main">
          <el-tabs v-if="readonly" v-model="activeStep" class="detail-tabs">
            <el-tab-pane v-for="(step, index) in TASK_CONFIG_STEPS"
                         :key="step.title"
                         :label="step.title"
                         :name="index"
            />
          </el-tabs>
          <el-form
              :model="form"
              label-width="100px"
              :disabled="readonly || saving || previewing || uploading"
              class="column-form"
          >
            <div v-show="activeStep === 0" class="column-form row-full">
              <guide-tip-panel
                  v-if="route.path === '/ingest/task/add' || isSourceTaskAdd"
                  class="row-full"
                  type="primary"
                  allow-wrap
                  title="用于配置数据接入任务的基本信息，作为后续数据获取、唯一标识、字段映射与处理及执行设置的基础。"
              />
              <div class="h2-titles row-full">基础信息</div>
              <qt-form-item label="任务名称" required>
                <el-input
                    v-model="form.name"
                    maxlength="64"
                    placeholder="请输入任务名称，支持中文、英文字符、数字、下划线和短横线"
                />
              </qt-form-item>
              <qt-form-item label="来源系统" required>
                <el-select
                    v-model="form.sourceId"
                    :disabled="isSourceTaskAdd"
                    filterable
                    placeholder="请选择来源系统"
                    @change="handleSourceChange"
                >
                  <template #label="{ label }">
                    <div v-if="selectedSource" class="source-option">
                      <img :src="getSourceIcon(selectedSource)" alt="" class="source-option__icon"/>
                      <span>{{ selectedSource.name }}</span>
                    </div>
                    <span v-else>{{ label }}</span>
                  </template>
                  <el-option
                      v-for="source in sources"
                      :key="source.id"
                      :value="source.id"
                      :label="source.name"
                  >
                    <div class="source-option">
                      <img :src="getSourceIcon(source)" alt="" class="source-option__icon"/>
                      <span>{{ source.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </qt-form-item>
              <qt-form-item label="主数据域" required>
                <el-select v-model="selectedProjectId" placeholder="请选择主数据域" @change="handleProjectChange">
                  <template #label="{ label }">
                    <div v-if="selectedProject" class="source-option">
                      <img :src="getProjectIcon(selectedProject.icon)" alt="" class="source-option__icon"/>
                      <span>{{ selectedProject.name }}</span>
                    </div>
                    <span v-else>{{ label }}</span>
                  </template>
                  <el-option v-for="item in modelTree" :key="item.id" :value="String(item.id)" :label="item.name">
                    <div class="source-option">
                      <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon"/>
                      <span>{{ item.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </qt-form-item>
              <qt-form-item label="模型目录" required>
                <el-select
                    v-model="selectedLayerId"
                    placeholder="请选择模型目录"
                    :disabled="!selectedProjectId"
                    @change="handleLayerChange"
                >
                  <el-option v-for="item in layerOptions" :key="item.id" :value="String(item.id)" :label="item.name"/>
                </el-select>
              </qt-form-item>
              <qt-form-item label="主数据模型" required>
                <el-select
                    v-model="form.modelId"
                    filterable
                    placeholder="请选择主数据模型"
                    @change="handleModelChange"
                >
                  <template #label="{ label }">
                    <div v-if="selectedModel" class="source-option">
                      <img :src="getModelIcon(selectedModel)" alt="" class="source-option__icon"/>
                      <span>{{ selectedModel.name }} ({{ selectedModel.code }})</span>
                    </div>
                    <span v-else>{{ label }}</span>
                  </template>
                  <el-option
                      v-for="model in filteredModels"
                      :key="model.id"
                      :value="model.id"
                      :label="`${model.name} (${model.code})`"
                  >
                    <div class="source-option">
                      <img :src="getModelIcon(model)" alt="" class="source-option__icon"/>
                      <span>{{ model.name }} ({{ model.code }})</span>
                    </div>
                  </el-option>
                </el-select>
              </qt-form-item>
              <qt-form-item label="负责人">
                <el-select
                    v-model="form.responsibleUserId"
                    clearable
                    filterable
                    placeholder="请选择负责人"
                    style="width: 100%"
                >
                  <el-option
                      v-for="user in userOptions"
                      :key="user.value"
                      :value="user.value"
                      :label="user.label"
                  />
                </el-select>
              </qt-form-item>
              <qt-form-item label="联系电话">
                <el-input :model-value="responsiblePhone" placeholder="请输入联系电话" disabled/>
              </qt-form-item>
              <qt-form-item label="接入类型" required>
                <el-radio-group v-model="form.type" @change="handleTypeChange">
                  <el-radio
                      v-for="item in TASK_TYPES"
                      :key="item.value"
                      :value="item.value"
                  >{{ item.label }}
                  </el-radio>
                </el-radio-group>
              </qt-form-item>
              <qt-form-item label="状态">
                <el-radio-group v-model="form.status">
                  <el-radio
                      v-for="item in TASK_STATUSES"
                      :key="item.value"
                      :value="item.value"
                  >{{ item.label }}
                  </el-radio>
                </el-radio-group>
              </qt-form-item>
              <qt-form-item label="描述" class="row-full">
                <el-input
                    v-model="form.remark"
                    type="textarea"
                    :rows="3"
                    maxlength="256个字符"
                    show-word-limit
                    placeholder="请输入描述，建议说明接入任务的目的、数据内容等信息"
                />
              </qt-form-item>
            </div>
            <div v-show="activeStep === 1" class="column-form row-full">
              <div class="h2-titles row-full">数据来源</div>
              <qt-form-item label="来源系统" required>
                <el-input :model-value="sourceName" disabled/>
              </qt-form-item>
              <qt-form-item label="主数据域" required>
                <el-input :model-value="projectName" disabled/>
              </qt-form-item>
              <qt-form-item label="模型目录" required>
                <el-input :model-value="layerName" disabled/>
              </qt-form-item>
              <qt-form-item label="主数据模型" required>
                <el-input :model-value="modelName" disabled/>
              </qt-form-item>
              <template v-if="form.type === 'DATABASE'">
                <qt-form-item label="数据连接" required>
                  <el-select
                      v-model="form.dbConfig.datasourceId"
                      filterable
                      placeholder="请选择来源系统关联的连接"
                      @change="handleDatabaseChange"
                  >
                    <el-option
                        v-for="source in datasources"
                        :key="source.id"
                        :value="source.id"
                        :label="source.datasourceName"
                    />
                  </el-select>
                </qt-form-item>
                <qt-form-item label="数据对象" required>
                  <el-select
                      v-model="form.dbConfig.sourceObject"
                      filterable
                      :loading="tableLoading"
                      placeholder="请选择数据表"
                      @visible-change="handleTableSelectVisible"
                      @change="invalidateFields"
                  >
                    <el-option
                        v-for="item in tableOptions"
                        :key="item.tableName"
                        :value="item.tableName"
                        :label="getTableOptionLabel(item)"
                    />
                  </el-select>
                </qt-form-item>
                <qt-form-item label="接入方式" class="row-full">
                  <el-radio-group
                      v-model="form.ingestMode"
                      @change="invalidateFields"
                  >
                    <el-radio value="FULL">追加</el-radio>
                    <el-radio value="INCREMENTAL">增量更新</el-radio>
                  </el-radio-group>
                </qt-form-item>
                <template v-if="form.ingestMode === 'INCREMENTAL'">
                  <qt-form-item label="增量判断字段" required>
                    <el-input
                        v-model="form.dbConfig.incrementField"
                        placeholder="更新时间或递增 ID 字段"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                  <qt-form-item label="首次增量起始值" required>
                    <el-input
                        v-model="form.dbConfig.initialIncrementValue"
                        placeholder="请输入首次读取起点"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                </template>
              </template>
              <template v-else-if="form.type === 'FILE'">
                <qt-form-item label="文件上传" class="row-full" required>
                  <el-upload
                      v-if="!readonly"
                      class="file-upload-uploader"
                      :show-file-list="false"
                      :http-request="uploadFile"
                      :before-upload="validateUploadFile"
                      accept=".csv,.xls,.xlsx"
                      :disabled="uploading"
                      drag
                  >
                    <el-icon class="el-icon--upload">
                      <upload-filled/>
                    </el-icon>
                    <div class="el-upload__text">将文件拖到此处，或<em>点击选择 CSV / XLS / XLSX 文件</em></div>
                    <template #tip>
                      <div class="el-upload__tip">
                        <div v-if="form.fileConfig.fileName" class="file-upload-name">{{
                            form.fileConfig.fileName
                          }}
                        </div>
                      </div>
                    </template>
                  </el-upload>
                  <div v-else class="file-upload-readonly">
                    <el-icon class="el-icon--upload">
                      <upload-filled/>
                    </el-icon>
                    <div class="el-upload__text">{{ form.fileConfig.fileName || '尚未选择文件' }}</div>
                    <div class="el-upload__tip text-center">文件仅用于当前接入任务</div>
                  </div>
                </qt-form-item>
              </template>
              <template v-else>
                <qt-form-item label="接口地址" required>
                  <el-input
                      v-model="form.apiConfig.url"
                      maxlength="1000"
                      placeholder="https://example.com/api/records"
                      @change="invalidateFields"
                  />
                </qt-form-item>
                <qt-form-item label="请求方式" required>
                  <el-radio-group
                      v-model="form.apiConfig.httpMethod"
                      @change="invalidateFields"
                  >
                    <el-radio value="GET">GET</el-radio>
                    <el-radio value="POST">POST</el-radio>
                  </el-radio-group>
                </qt-form-item>
                <qt-form-item label="响应数据路径" required class="nowrap-label-item">
                  <el-input
                      v-model.trim="form.apiConfig.readConfig.recordsPath"
                      maxlength="512"
                      placeholder="请输入响应数据路径"
                      @change="invalidateFields"
                  />
                </qt-form-item>
                <qt-form-item label="分页方式" required>
                  <el-radio-group
                      v-model="form.apiConfig.readConfig.pagingMode"
                      @change="invalidateFields"
                  >
                    <el-radio value="NONE">不分页</el-radio>
                    <el-radio value="PAGE">页码分页</el-radio>
                  </el-radio-group>
                </qt-form-item>
                <template v-if="form.apiConfig.readConfig.pagingMode === 'PAGE'">
                  <qt-form-item label="页码参数名" required>
                    <el-input
                        v-model.trim="form.apiConfig.readConfig.positionParam"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                  <qt-form-item label="每页条数参数名" required>
                    <el-input
                        v-model.trim="form.apiConfig.readConfig.pageSizeParam"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                  <qt-form-item label="起始页码" required>
                    <el-input
                        v-model.number="form.apiConfig.readConfig.startPosition"
                        type="number"
                        min="1"
                        step="1"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                  <qt-form-item label="每页条数" required>
                    <el-input
                        v-model.number="form.apiConfig.readConfig.pageSize"
                        type="number"
                        min="1"
                        max="10000"
                        step="1"
                        @change="invalidateFields"
                    />
                  </qt-form-item>
                </template>
                <qt-form-item label="请求头" required class="row-full">
                  <el-input
                      v-model="headersText"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入请求头 JSON（可留空）"
                      @change="invalidateFields"
                  />
                </qt-form-item>
                <qt-form-item :label="form.apiConfig.httpMethod === 'POST' ? '请求体参数' : '查询参数'" required class="row-full">
                  <el-input
                      v-model="paramsText"
                      type="textarea"
                      :rows="3"
                      placeholder="请输入查询参数 JSON（可留空）"
                      @change="invalidateFields"
                  />
                </qt-form-item>
              </template>
            </div>
            <div v-show="activeStep === 1" class="h2-titles row-full">
              来源唯一标识
            </div>
            <qt-form-item
              v-show="activeStep === 1"
                label="来源唯一标识"
                class="row-full nowrap-label-item"
                required
                tip="支持多字段组合标识，所选字段组合后能唯一确定一条来源记录。"
              >
                <el-select
                  v-model="form.uniqueFields"
                  :loading="previewing"
                  multiple
                  filterable
                  placeholder="选择一个或多个来源字段"
                  @visible-change="handleUniqueFieldsVisible"
                >
                  <el-option
                    v-for="field in sourceFields"
                    :key="field"
                    :value="field"
                    :label="field"
                  />
                </el-select>
              </qt-form-item>
            <div v-show="activeStep === 2" class="column-form row-full schedule-content">
              <div class="h2-titles row-full">调度周期
              </div>
              <qt-form-item label="执行方式" label-width="auto"
                            tip="手动执行：由用户按需手动触发任务执行；Cron 表达式：按配置的周期规则自动执行任务。">
                <el-radio-group v-model="form.triggerType">
                  <el-radio value="MANUAL">手动执行</el-radio>
                  <el-radio value="SCHEDULED">Cron 表达式</el-radio>
                </el-radio-group>
              </qt-form-item>
              <qt-form-item v-if="form.triggerType === 'SCHEDULED'"
                            label="调度周期"
                            label-width="auto"
                            class="row-full"
                            required
              >
                <el-input
                    v-model="form.cronExpression"
                    maxlength="120"
                    placeholder="请输入调度周期"
                >
                  <template #append>
                    <el-button type="primary" @click="openCronGenerator">配置</el-button>
                  </template>
                </el-input>
              </qt-form-item>
              <div class="h2-titles row-full">异常处理策略
              </div>
              <div class="tactics-content row-full">
                <div class="tactics-block">
                  <div class="block-label">
                    <div class="block-title">错误处理策略：</div>
                    <div class="block-msg">创建数据表、数据库连接异常等非数据基本错误。</div>
                  </div>
                  <div class="block-list">
                    <div
                      v-for="item in errorStrategyOptions"
                      :key="item.value"
                      :class="['tactics-item', form.errorStrategy === item.value ? 'active' : '']"
                      role="radio"
                      :aria-checked="form.errorStrategy === item.value"
                      tabindex="0"
                      @click="form.errorStrategy = item.value"
                      @keydown.space.prevent="form.errorStrategy = item.value"
                      @keydown.enter.prevent="form.errorStrategy = item.value"
                    >
                      <div class="tactics-head">
                        <div class="tactics-icon">
                          <img
                            :src="form.errorStrategy === item.value ? item.imgActive : item.img"
                            alt=""
                          />
                        </div>
                        <div class="tactics-title">{{ item.title }}</div>
                        <el-tag v-if="item.recommended" type="primary" size="small">推荐</el-tag>
                      </div>
                      <div class="tactics-body">{{ item.content }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="h2-titles row-full">失败重试

              </div>
              <qt-form-item
                  label="失败重试次数"
                  class="row-full"
                  tip="设置任务执行失败后的重试次数。"
              >
                <el-input-number
                    v-model="form.retryCount"
                    class="retry-count-input"
                    :min="0"
                    :max="10"
                    :precision="0"
                />
              </qt-form-item>
              <qt-form-item label="失败重试间隔" class="row-full"   tip="0 表示不重试，仅重试失败记录，避免重复追加成功数据。" >
                <el-input
                    v-model.number="form.retryIntervalSeconds"
                    type="number"
                    min="0"
                    max="3600"
                    step="1"
                >
                  <template #append>秒</template>
                </el-input>
              </qt-form-item>
            </div>
            <div v-show="activeStep === 1" class="h2-titles row-full">字段映射与处理</div>
            <el-text v-show="activeStep === 1" class="row-full" type="info" size="small">
              请将来源字段映射至对应的目标模型字段；如需转换数据内容，可选择处理方式并配置处理参数；无需转换时选择“不处理”。
            </el-text>
            <field-mapping
              v-show="activeStep === 1"
              v-model="form.mappings"
              :source-fields="sourceFields"
              :model-fields="modelFields"
              :unique-fields="form.uniqueFields"
              :source-name="sourceName"
              :source-trust="sourceTrust"
              :task-name="form.name"
              :readonly="readonly || saving"
              class="row-full"
            />
          </el-form>
        </div>
        <div class="button-style">
          <el-button class="action-btn" type="primary" @click="goBack">返回列表</el-button>
          <el-button v-if="!readonly && activeStep > 0"
                     class="action-btn"
                     type="primary"
                     plain
                     @click="activeStep--"
          >上一步
          </el-button>
          <el-button
              v-if="!readonly && activeStep < 2"
              class="action-btn"
              plain
              type="primary"
              :disabled="uploading || previewing"
              @click="changeStep(activeStep + 1)"
          >下一步
          </el-button>
          <el-button
              v-else-if="!readonly && activeStep === 2"
              v-hasPermi="[form.id ? 'ingest:task:edit' : 'ingest:task:add']"
              type="primary"
              :loading="saving"
              @click="submit"
          >保存并退出
          </el-button>
        </div>
      </div>
    </div>
    <el-dialog
        v-model="cronDialogVisible"
        title="Cron 表达式生成器"
        width="860px"
        :append-to="$refs['app-container']"
        destroy-on-close
    >
      <crontab
          :expression="cronExpressionDraft"
          @hide="cronDialogVisible = false"
          @fill="fillCronExpression"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, getCurrentInstance, reactive, ref, watch} from 'vue';

import {useRoute, useRouter} from 'vue-router';

import Steps from '@/components/Steps/index.vue';
import DetailInfo from '@/components/DetailInfo/index.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import Crontab from '@/components/Crontab/index.vue';
import {usePageRefresh} from '@/composables/usePageRefresh';
import errorStrategyIcon from '@/assets/modules/ingest/icons/task/error-strategy.svg';
import errorStrategyActiveIcon from '@/assets/modules/ingest/icons/task/error-strategy-active.svg';
import {getIngestSource, listIngestSource} from '@/api/ingest/source/source';
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import {getPublishedModelStructure, getPublishedModelTree, listPublishedModels} from '@/api/model/base/base';
import {
  addIngestTask,
  getIngestTask,
  previewIngestTask,
  updateIngestTask,
  uploadIngestFile,
} from '@/api/ingest/task/task';

import FieldMapping from '@/views/ingest/task/config/fieldMapping.vue';
import {tableList} from '@/api/ingest/datasource/dataSource';

import {getProjectIcon} from '@/utils/imageUrl';
import {getModelIcon} from '@/utils/imageUrl';

const TASK_TYPES = [
  { value: 'DATABASE', label: '数据库' },
  { value: 'FILE', label: '文件' },
  { value: 'API', label: 'API' },
];

const getOptionLabel = (options, value) =>
  options.find((item) => item.value === value)?.label || value || '-';

const TASK_CONFIG_DETAIL_ITEMS = [
  { label: '来源系统', key: 'sourceName' },
  { label: '主数据模型', key: 'modelName' },
  { label: '接入类型', key: 'type', formatter: (data) => getOptionLabel(TASK_TYPES, data.type) },
  { label: '备注', key: 'remark', span: 24, ellipsisClass: 'ellipsis' },
];

const TASK_CONFIG_STEPS = ['基础信息', '数据来源与处理', '调度与运行策略'].map(
  (title) => ({ title }),
);

const TASK_STATUSES = [
  { value: '1', label: '上线' },
  { value: '0', label: '下线' },
];

const TRANSFORM_DATE = '6';

const TRANSFORM_TYPE = '1';

const TRANSFORM_VALUE_MAP = '5';

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const userDirectoryStore = useUserDirectoryStore();

defineOptions({name: 'IngestTaskConfig'});
const {proxy} = getCurrentInstance();
const route = useRoute();
const router = useRouter();
const readonly = computed(() => route.path === '/ingest/task/detail');
const isSourceTaskAdd = computed(() => route.path === '/ingest/source/task/add');
const {setRefreshNeeded} = usePageRefresh('ingestTask');
const {setRefreshNeeded: setSourceRefreshNeeded} = usePageRefresh('ingestSource');
const sourceName = computed(
    () => sources.value.find((item) => String(item.id) === String(form.sourceId))?.name || '-',
);
const selectedSource = computed(
    () => sources.value.find((item) => String(item.id) === String(form.sourceId)),
);
const selectedProject = computed(
    () => modelTree.value.find((item) => String(item.id) === String(selectedProjectId.value)),
);
const selectedModel = computed(
    () => models.value.find((item) => String(item.id) === String(form.modelId)),
);
const modelName = computed(() => {
  const model = selectedModel.value;
  return model ? `${model.name} (${model.code})` : '-';
});
const sourceTrust = computed(
    () =>
        sources.value.find((item) => String(item.id) === String(form.sourceId))?.trustScore ??
        50,
);
const responsiblePhone = computed(() => {
  const user = userOptions.value.find(
      (item) => String(item.value) === String(form.responsibleUserId),
  );
  return user?.phone || '';
});
const activeStep = ref(0);
const loading = ref(false);
const saving = ref(false);
const previewing = ref(false);
const uploading = ref(false);
const cronDialogVisible = ref(false);
const cronExpressionDraft = ref('');
const sources = ref([]);
const userOptions = ref([]);
const models = ref([]);
const modelTree = ref([]);
const selectedProjectId = ref();
const selectedLayerId = ref();
const layerOptions = computed(() => modelTree.value.find(item => String(item.id) === selectedProjectId.value)?.children || []);
const projectName = computed(() => modelTree.value.find(item => String(item.id) === selectedProjectId.value)?.name || '-');
const layerName = computed(() => layerOptions.value.find(item => String(item.id) === selectedLayerId.value)?.name || '-');
const filteredModels = computed(() => models.value.filter(item => String(item.projectId) === selectedProjectId.value && String(item.layerId) === selectedLayerId.value));

function handleProjectChange() {
  selectedLayerId.value = undefined;
  handleLayerChange();
}

function handleLayerChange() {
  form.modelId = undefined;
  form.mappings = [];
  modelFields.value = [];
}

const modelFields = ref([]);
const datasources = ref([]);
const sourceFields = ref([]);
const tableOptions = ref([]);
const tableLoading = ref(false);
const headersText = ref('');
const paramsText = ref('');
const errorStrategyOptions = [
  {
    title: '错误继续执行',
    value: 'CONTINUE',
    recommended: true,
    content: '记录错误，跳过当前步骤，任务继续运行。',
    img: errorStrategyIcon,
    imgActive: errorStrategyActiveIcon,
  },
  {
    title: '错误暂停任务',
    value: 'STOP',
    content: '记录错误并停止任务执行。',
    img: errorStrategyIcon,
    imgActive: errorStrategyActiveIcon,
  },
];
/**
 * 生成独立的任务默认配置，供新增及详情回填使用。
 * @returns {Object} 初始任务表单。
 */
const createDefaultForm = () => ({
  status: '0',
  errorStrategy: 'CONTINUE',
  nullStrategy: 'IGNORE',
  retryCount: 0,
  retryIntervalSeconds: 0,
  name: '',
  code: '',
  sourceId: undefined,
  modelId: undefined,
  responsibleUserId: undefined,
  type: 'DATABASE',
  ingestMode: 'FULL',
  triggerType: 'MANUAL',
  cronExpression: '',
  remark: '',
  uniqueFields: [],
  mappings: [],
  dbConfig: {},
  fileConfig: {},
  apiConfig: {
    httpMethod: 'GET',
    url: '',
    headerConfig: {},
    paramConfig: {},
    readConfig: {
      recordsPath: '',
      pagingMode: 'NONE',
      positionParam: '',
      pageSizeParam: '',
      startPosition: '',
      pageSize: '',
    },
  },
});
const form = reactive(createDefaultForm());
let loadVersion = 0;
let tableLoadVersion = 0;
let fieldsLoadVersion = 0;
let loadedDatasourceId;
watch(() => [route.path, route.query.id, route.query.sourceSystemId], initialize, {immediate: true});

/** 按路由初始化选项与任务配置，通过请求版本防止旧数据覆盖当前任务。 */
async function initialize() {
  if (!['/ingest/task/add', '/ingest/task/edit', '/ingest/task/detail', '/ingest/source/task/add'].includes(route.path))
    return;
  if (isSourceTaskAdd.value && !route.query.sourceSystemId) {
    proxy.$modal.msgWarning('请从来源系统列表选择来源系统后新增接入任务');
    goBack();
    return;
  }
  const version = ++loadVersion;
  fieldsLoadVersion++;
  loading.value = true;
  activeStep.value = 0;
  tableLoadVersion++;
  tableOptions.value = [];
  tableLoading.value = false;
  loadedDatasourceId = undefined;
  try {
    const [sourceRes, modelRes, userRes, taskRes, treeRes] = await Promise.all([
      listIngestSource({pageNum: 1, pageSize: -1}),
      listPublishedModels({pageNum: 1, pageSize: -1}),
      userDirectoryStore.loadUserList(),
      !isSourceTaskAdd.value && route.query.id ? getIngestTask(route.query.id) : Promise.resolve(null),
      getPublishedModelTree(),
    ]);
    if (version !== loadVersion) return;
    sources.value = (sourceRes.data.rows || sourceRes.data).map((source) => ({
      ...source,
      id: String(source.id),
    }));
    models.value = modelRes.data.rows || [];
    modelTree.value = treeRes.data || [];
    userOptions.value = (userRes.rows || userRes.data?.rows || []).map((user) => ({
      value: user.userId,
      label: user.nickName || user.userName,
      phone: user.phonenumber,
    }));
    const initial = createDefaultForm();
    const data = taskRes?.data || {};
    Object.keys(form).forEach((key) => delete form[key]);
    Object.assign(form, initial, data, {
      dbConfig: data.dbConfig || initial.dbConfig,
      fileConfig: {...initial.fileConfig, ...data.fileConfig},
      apiConfig: {
        ...initial.apiConfig,
        ...data.apiConfig,
        // 兼容历史任务未配置响应路径，同时保留已保存的分页规则。
        readConfig: {
          ...initial.apiConfig.readConfig,
          ...data.apiConfig?.readConfig,
          recordsPath: (data.apiConfig?.readConfig?.recordsPath || '').replace(/^\$\.?/, ''),
        },
      },
    });
    // 不分页任务忽略后端补齐的分页默认值，首次启用时由用户填写。
    if (form.apiConfig.readConfig.pagingMode === 'NONE') {
      const {positionParam, pageSizeParam, startPosition, pageSize} = initial.apiConfig.readConfig;
      Object.assign(form.apiConfig.readConfig, {positionParam, pageSizeParam, startPosition, pageSize});
    }
    form.sourceId = isSourceTaskAdd.value && route.query.sourceSystemId ? String(route.query.sourceSystemId) : form.sourceId == null ? undefined : String(form.sourceId);
    const selectedModel = models.value.find(item => String(item.id) === String(form.modelId));
    selectedProjectId.value = selectedModel?.projectId == null ? undefined : String(selectedModel.projectId);
    selectedLayerId.value = selectedModel?.layerId == null ? undefined : String(selectedModel.layerId);
    const headerConfig = form.apiConfig.headerConfig || {};
    const paramConfig = form.apiConfig.paramConfig || {};
    headersText.value = Object.keys(headerConfig).length ? JSON.stringify(headerConfig, null, 2) : '';
    paramsText.value = Object.keys(paramConfig).length ? JSON.stringify(paramConfig, null, 2) : '';
    sourceFields.value = [
      ...new Set([
        ...(form.uniqueFields || []),
        ...(form.mappings || []).map((item) => item.sourceFieldName),
      ]),
    ];
    await Promise.all([loadDatasources(), loadModelFields()]);
    // 读取外部数据库表可能较慢，不阻塞配置页主体渲染；下拉自身显示加载状态。
    if (form.type === 'DATABASE' && form.dbConfig?.datasourceId) loadTables();
  } catch {
    /* 请求失败由统一拦截器提示 */
  } finally {
    if (version === loadVersion) loading.value = false;
  }
}

/** 读取当前来源系统关联的数据库连接，忽略切换来源前的响应。 */
async function loadDatasources() {
  datasources.value = [];
  const id = form.sourceId;
  if (!id) return;
  const res = await getIngestSource(id);
  if (String(id) === String(form.sourceId))
    datasources.value = res.data.datasourceList || [];
}

/** 读取当前模型已发布的业务字段，供映射目标选择。 */
async function loadModelFields() {
  modelFields.value = [];
  const id = form.modelId;
  if (!id) return;
  const res = await getPublishedModelStructure(id);
  if (id === form.modelId) {
    modelFields.value = (res.data?.fields || []).filter(field => field.columnCategory === 'BUSINESS');
  }
}

/** 数据来源变化时清空已读取字段、唯一标识及映射，要求重新读取。 */
function invalidateFields() {
  fieldsLoadVersion++;
  sourceFields.value = [];
  form.uniqueFields = [];
  form.mappings = [];
}

/** 切换来源系统后清空连接及字段缓存，再加载关联连接。 */
async function handleSourceChange() {
  form.dbConfig = {};
  tableLoadVersion++;
  tableOptions.value = [];
  tableLoading.value = false;
  loadedDatasourceId = undefined;
  invalidateFields();
  await loadDatasources();
}

/** 切换目标模型后清空旧映射并加载新模型业务字段。 */
async function handleModelChange() {
  form.mappings = [];
  await loadModelFields();
}

/** 切换接入类型后清空字段配置，仅数据库接入保留接入模式。 */
function handleTypeChange() {
  invalidateFields();
  form.ingestMode = form.type === 'DATABASE' ? 'FULL' : null;
}

/** 将当前 Cron 复制到草稿后打开表达式生成器。 */
function openCronGenerator() {
  cronExpressionDraft.value = form.cronExpression || '';
  cronDialogVisible.value = true;
}

/**
 * 回填生成的 Cron 表达式并关闭弹窗。
 * @param {string} value Cron 表达式。
 */
function fillCronExpression(value) {
  form.cronExpression = value;
  cronDialogVisible.value = false;
}

/** 切换连接后清空库表及字段选择，再加载可选数据表。 */
function handleDatabaseChange() {
  form.dbConfig.sourceSchema = '';
  form.dbConfig.sourceObject = '';
  invalidateFields();
  loadTables();
}

/**
 * 组合表名和注释，重复或空注释不再附加展示。
 * @param {Object} item 数据表选项。
 * @returns {string} 表名展示文本。
 */
function getTableOptionLabel(item) {
  const comment = item.tableComment?.trim();
  if (!comment || comment === item.tableName) return item.tableName;
  return `${item.tableName}（${comment}）`;
}

/**
 * 展开下拉框时补载当前连接尚未成功读取的数据表。
 * @param {boolean} visible 下拉框是否展开。
 */
function handleTableSelectVisible(visible) {
  if (
      visible &&
      form.dbConfig.datasourceId &&
      loadedDatasourceId !== form.dbConfig.datasourceId &&
      !tableLoading.value
  ) {
    loadTables();
  }
}

/** 按当前来源及连接读取数据表，保留已选表并丢弃过期响应。 */
async function loadTables() {
  const datasourceId = form.dbConfig.datasourceId;
  const version = ++tableLoadVersion;
  tableOptions.value = [];
  loadedDatasourceId = undefined;
  if (!datasourceId) return;
  if (!form.sourceId) return;
  tableLoading.value = true;
  try {
    const res = await tableList({datasourceId, sourceId: form.sourceId, limit: -1});
    if (version !== tableLoadVersion || datasourceId !== form.dbConfig.datasourceId) return;
    const tables = Array.isArray(res.data) ? res.data : [];
    const current = form.dbConfig.sourceObject;
    if (current && !tables.some((item) => item.tableName === current)) {
      tables.unshift({tableName: current, tableComment: ''});
    }
    tableOptions.value = tables;
    loadedDatasourceId = datasourceId;
  } catch {
    if (version !== tableLoadVersion || datasourceId !== form.dbConfig.datasourceId) return;
    const current = form.dbConfig.sourceObject;
    tableOptions.value = current ? [{tableName: current, tableComment: ''}] : [];
  } finally {
    if (version === tableLoadVersion) tableLoading.value = false;
  }
}

/**
 * 解析请求头或参数 JSON，仅接受非数组对象。
 * @param {string} text JSON 文本。
 * @param {string} label 用于错误提示的字段名称。
 * @returns {Object} 解析结果。
 * @throws {Error} JSON 无效或结果不是对象。
 */
function parseJsonObject(text, label) {
  try {
    const value = JSON.parse(text || '{}');
    if (!value || typeof value !== 'object' || Array.isArray(value))
      throw new Error();
    return value;
  } catch {
    throw new Error(`${label}必须是有效的 JSON 对象`);
  }
}

/**
 * 按接入类型组装提交数据，清理隐藏配置并规范 API 响应路径及分页参数。
 * @returns {Object} 接入任务提交数据。
 * @throws {Error} API 路径、请求头或分页配置不合法。
 */
function buildPayload() {
  const data = JSON.parse(
      JSON.stringify(
          Object.fromEntries(
              [...Object.keys(createDefaultForm()), 'id']
                  .map((key) => [key, form[key]]),
          ),
      ),
  );
  data.name = data.name?.trim();
  data.responsibleUserId = data.responsibleUserId || null;
  // 未选择目标的来源字段仍保留配置和唯一标识，后端仅跳过模型写入。
  data.mappings.forEach((row) => {
    if (row.targetFieldId == null || row.targetFieldId === '') row.targetFieldId = null;
  });
  data.dbConfig = form.type === 'DATABASE' ? data.dbConfig : null;
  data.ingestMode = form.type === 'DATABASE' ? data.ingestMode : null;
  if (data.dbConfig && data.ingestMode === 'FULL') {
    data.dbConfig.incrementField = null;
    data.dbConfig.initialIncrementValue = null;
  }
  data.fileConfig = form.type === 'FILE' ? data.fileConfig : null;
  data.apiConfig =
      form.type === 'API'
          ? {
            ...data.apiConfig,
            headerConfig: parseJsonObject(headersText.value, '请求头'),
            paramConfig: parseJsonObject(paramsText.value, '请求参数'),
          }
          : null;
  if (data.apiConfig) {
    const inputPath = data.apiConfig.readConfig.recordsPath?.trim() || '';
    // 页面省略根节点符号，提交时补齐后端所需路径；留空表示响应根数组。
    const recordsPath = !inputPath || inputPath.startsWith('$')
        ? inputPath || '$'
        : `${inputPath.startsWith('[') ? '$' : '$.'}${inputPath}`;
    if (recordsPath.length > 512 || !/^\$(\.[\p{L}_][\p{L}\p{N}_-]*|\[[0-9]+\])*$/u.test(recordsPath))
      throw new Error('响应数据路径格式不正确，请填写 data.rows 或 data.data 等路径');
    data.apiConfig.readConfig.recordsPath = recordsPath;
    const readConfig = data.apiConfig.readConfig;
    if (readConfig.pagingMode === 'PAGE') {
      readConfig.positionParam = readConfig.positionParam?.trim();
      readConfig.pageSizeParam = readConfig.pageSizeParam?.trim();
      if (!readConfig.positionParam || !readConfig.pageSizeParam)
        throw new Error('请填写页码参数名和每页条数参数名');
      if (readConfig.positionParam === readConfig.pageSizeParam)
        throw new Error('页码参数名和每页条数参数名不能相同');
      if (!Number.isSafeInteger(readConfig.startPosition) || readConfig.startPosition < 1)
        throw new Error('起始页码必须为大于或等于1的整数');
      if (!Number.isInteger(readConfig.pageSize) || readConfig.pageSize < 1 || readConfig.pageSize > 10000)
        throw new Error('每页条数必须为1至10000的整数');
    } else if (readConfig.pagingMode === 'NONE') {
      // 不分页时不提交已隐藏的分页字段，避免残留配置影响单次读取。
      data.apiConfig.readConfig = {recordsPath, pagingMode: 'NONE'};
    }
  }
  if (
      data.apiConfig &&
      Object.values(data.apiConfig.headerConfig).some(
          (value) => typeof value !== 'string',
      )
  )
    throw new Error('请求头的值必须是字符串');
  if (data.apiConfig && [data.apiConfig.headerConfig, data.apiConfig.paramConfig].some(
      (value) => JSON.stringify(value).length > 3072,
  )) throw new Error('请求头或参数配置不能超过3072个字符');
  return data;
}

/**
 * 校验指定步骤的必填信息、唯一标识和映射约束。
 * @param {number} step 从 0 开始的步骤索引。
 * @throws {Error} 当前步骤配置不完整或不合法。
 */
function validateStage(step) {
  if (step === 0) {
    if (!form.name?.trim() || !form.sourceId || !form.modelId)
      throw new Error('请填写任务名称，并选择来源系统和主数据模型');
    const taskName = form.name.trim();
    if (taskName.length > 64) throw new Error('任务名称不能超过64个字符');
    if (!/^[\u4e00-\u9fa5A-Za-z0-9_-]+$/.test(taskName))
      throw new Error('任务名称支持中文、英文字符、数字、下划线和短横线');
    if ((form.remark || '').length > 512)
      throw new Error('描述不能超过256个字符');
  } else if (step === 1) {
    if (
        form.type === 'DATABASE' &&
        (!form.dbConfig.datasourceId || !form.dbConfig.sourceObject)
    )
      throw new Error('请选择数据库连接和数据对象');
    if (
        form.type === 'DATABASE' &&
        form.ingestMode === 'INCREMENTAL' &&
        (!form.dbConfig.incrementField || !form.dbConfig.initialIncrementValue)
    )
      throw new Error('请完成增量判断字段和首次起始值配置');
    if (form.type === 'FILE' && !(form.fileConfig.filePath || form.fileConfig.fileToken))
      throw new Error('请先上传接入文件');
    if (form.type === 'API' && !/^https?:\/\//i.test(form.apiConfig.url))
      throw new Error('请填写有效的 HTTP 或 HTTPS 接口地址');
    buildPayload();
  } else if (step === 2) {
    if (!sourceFields.value.length) throw new Error('请先读取来源字段');
    if (!form.uniqueFields?.length) throw new Error('请选择来源记录唯一标识');
  } else if (step === 3) {
    if (!sourceFields.value.length) throw new Error('请先读取来源字段');
    // 只校验参与模型写入的行，未映射行的处理参数不影响保存。
    const selectedMappings = form.mappings.filter(row => row.targetFieldId != null && row.targetFieldId !== '');
    if (!selectedMappings.length) throw new Error('请至少选择一个目标模型字段');
    const targetIds = new Set();
    selectedMappings.forEach((row) => {
      const targetId = String(row.targetFieldId);
      const target = modelFields.value.find(field => String(field.id) === targetId);
      if (!target) throw new Error(`来源字段「${row.sourceFieldName}」选择的目标字段已失效`);
      if (targetIds.has(targetId)) throw new Error(`目标字段「${target.code}」被重复映射`);
      targetIds.add(targetId);
    });
    if (
        form.uniqueFields.some(
            (field) => !form.mappings.some((row) => row.sourceFieldName === field),
        )
    )
      throw new Error('唯一标识字段必须参与字段映射');
    if (selectedMappings.some(row => row.transformType === TRANSFORM_TYPE && !row.transformConfig?.targetType))
      throw new Error('请选择类型转换的目标类型');
    if (
        selectedMappings.some(
            (row) =>
                row.transformType === TRANSFORM_DATE &&
                (!row.transformConfig?.inputFormat ||
                    !row.transformConfig?.outputFormat),
        )
    )
      throw new Error('请完善日期格式转换配置');
    if (
        selectedMappings.some(
            (row) =>
                row.transformType === TRANSFORM_VALUE_MAP &&
                !Object.keys(row.transformConfig?.values || {}).length,
        )
    )
      throw new Error('请完善值映射配置');
  } else if (step === 4) {
    if (form.triggerType === 'SCHEDULED' && !form.cronExpression?.trim())
      throw new Error('请输入调度周期');
  }
}

function validateStep(step) {
  if (step === 0) {
    validateStage(0);
    if (!selectedProjectId.value || !selectedLayerId.value) throw new Error('请选择主数据域和模型目录');
  } else if (step === 1) {
    [1, 2, 3].forEach(validateStage);
  } else {
    validateStage(4);
    if (!Number.isInteger(form.retryCount) || form.retryCount < 0 || form.retryCount > 10) throw new Error('失败重试次数必须为0至10的整数');
    if (
      !Number.isInteger(form.retryIntervalSeconds) ||
      form.retryIntervalSeconds < 0 ||
      form.retryIntervalSeconds > 3600 ||
      (form.retryCount > 0 && form.retryIntervalSeconds < 1)
    ) throw new Error('启用失败重试时，间隔必须为1至3600秒的整数');
  }
}

/**
 * 前进时校验已完成步骤，必要时重新读取来源字段；后退保留当前配置。
 * @param {number} step 目标步骤索引。
 */
async function changeStep(step) {
  if (loading.value || saving.value || previewing.value || uploading.value)
    return;
  if (readonly.value || step < activeStep.value) {
    activeStep.value = step;
    return;
  }
  try {
    validateStep(0);
    if (step === 2) {
      validateStage(1);
      if (!sourceFields.value.length) {
        if (!(await preview(false))) return;
      }
      validateStep(1);
    }
    activeStep.value = step;
  } catch (error) {
    proxy.$modal.msgWarning(error.message);
  }
}

/** 展开 API 来源唯一标识时补读字段，避免重复请求。 */
async function handleUniqueFieldsVisible(visible) {
  if (!visible || form.type !== 'API' || sourceFields.value.length
      || readonly.value || loading.value || saving.value || uploading.value || previewing.value) return;
  await preview(false);
}

/**
 * 调用预览接口读取来源字段，清理已失效的唯一标识和映射。
 * @param {boolean} [showSuccess=true] 是否显示读取成功提示。
 * @returns {Promise<boolean>} 是否成功读取字段。
 */
async function preview(showSuccess = true) {
  let data;
  try {
    validateStage(1);
    data = buildPayload();
  } catch (error) {
    proxy.$modal.msgWarning(error.message);
    return false;
  }
  previewing.value = true;
  const version = ++fieldsLoadVersion;
  try {
    let res;
    try {
      res = await previewIngestTask(data);
    } catch {
      // 请求失败已由统一拦截器提示，停止跳转，避免外层再次弹窗。
      return false;
    }
    if (version !== fieldsLoadVersion) return false;
    sourceFields.value = res.data.fields;
    form.uniqueFields = form.uniqueFields.filter((field) =>
        sourceFields.value.includes(field),
    );
    form.mappings = form.mappings.filter((row) =>
        sourceFields.value.includes(row.sourceFieldName),
    );
    if (showSuccess)
      proxy.$modal.msgSuccess(`已读取 ${sourceFields.value.length} 个来源字段`);
    return true;
  } finally {
    previewing.value = false;
  }
}

/**
 * 上传前限制文件扩展名及大小，不符合要求时阻止上传。
 * @param {File} file 待上传文件。
 * @returns {boolean} 是否允许上传。
 */
function validateUploadFile(file) {
  if (!/\.(csv|xls|xlsx)$/i.test(file.name)) {
    proxy.$modal.msgError('仅支持 CSV、XLS、XLSX 文件');
    return false;
  }
  if (file.size > 50 * 1024 * 1024) {
    proxy.$modal.msgError('文件大小不能超过 50 MB');
    return false;
  }
  return true;
}

/**
 * 上传文件后回填文件信息，复用预览流程读取来源字段及同步映射。
 * @param {Object} options 上传组件参数，file 为本次文件。
 */
async function uploadFile({file}) {
  uploading.value = true;
  try {
    const res = await uploadIngestFile(file);
    invalidateFields();
    Object.assign(form.fileConfig, {
      fileToken: res.data.fileToken,
      filePath: res.data.filePath,
      fileName: res.data.fileName,
      fileType: res.data.fileType,
      fileSize: res.data.fileSize,
      uploadTime: res.data.uploadTime,
    });
    if (!(await preview(false))) return;
    proxy.$modal.msgSuccess(`文件上传成功，已读取 ${sourceFields.value.length} 个来源字段`);
  } finally {
    uploading.value = false;
  }
}

/** 依次校验全部步骤并保存任务，成功后标记列表刷新并返回。 */
async function submit() {
  let data;
  try {
    for (let i = 0; i < 3; i++) validateStep(i);
    data = buildPayload();
  } catch (error) {
    proxy.$modal.msgWarning(error.message);
    return;
  }
  saving.value = true;
  try {
    await (form.id ? updateIngestTask(data) : addIngestTask(data));
    proxy.$modal.msgSuccess('保存成功');
    setRefreshNeeded();
    if (isSourceTaskAdd.value) setSourceRefreshNeeded();
    goBack();
  } finally {
    saving.value = false;
  }
}

/** 按新增入口返回所属菜单列表。 */
function goBack() {
  router.push(isSourceTaskAdd.value ? '/ingest/source' : '/ingest/task');
}
</script>

<style scoped lang="scss">
/* 接入任务向导布局：步骤区、配置内容与底部操作区使用统一页面尺寸。 */
@use "element-plus/theme-chalk/src/common/var" as element;

.custom-card {
  width: 100%;
  height: 100px;
  padding: 34px 40px 26px;
  background: var(--qt-surface-background);
  box-sizing: border-box;
  margin-bottom: var(--qt-space-4);
}

/* 固定配置卡片的可用高度，底部操作区以此容器定位。 */
.task-config .config-card {
  height: 74vh;
  min-height: 480px;
  position: relative;
  padding-bottom: var(--qt-space-4);
  padding-left: 0;
  padding-right: 0;
}

.infotop {
  height: 100%;
}

/* 配置内容在卡片内滚动，底部留白避免操作按钮遮挡字段。 */
.main {
  height: 100%;
  background: var(--qt-surface-background);
  padding: 0 25px 85px;
  padding-left: calc(var(--qt-space-5) * 2);
  padding-right: calc(var(--qt-space-5) * 2);
  overflow: auto;
}

/* 复用公共表单间距，收缩网格列以避免长内容撑开页面。 */
.column-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.h2-desc {
  font-size: var(--qt-font-size-base);
  color: var(--el-text-color-regular);
}

.el-select {
  width: 100%;
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

.retry-count-input {
  width: 100%;
}

.tactics-block {
  display: flex;
  margin-bottom: 10px;
}

.block-label {
  flex: 0 0 176px;
  padding: 0 40px 0 10px;
}

.block-title {
  position: relative;
  margin-bottom: 5px;
  color: var(--el-text-color-regular);
  font-size: var(--qt-font-size-base);
  font-weight: 700;
  white-space: nowrap;
}

.block-title::before {
  position: absolute;
  left: -10px;
  color: var(--el-color-danger);
  content: '*';
}

.block-msg {
  color: var(--el-text-color-secondary);
  font-size: var(--qt-font-size-small);
}

.block-list {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.tactics-item {
  box-sizing: content-box;
  margin-bottom: 10px;
  padding: 8px;
  border: 2px solid transparent;
  border-radius: 4px;
  background-image: linear-gradient(
    to right,
    var(--el-fill-color-light),
    transparent
  );
  color: var(--el-text-color-secondary);
  cursor: pointer;
}

.tactics-item:hover,
.tactics-item.active {
  background-image: linear-gradient(
    to right,
    var(--el-color-primary-light-9),
    transparent
  );
}

.tactics-item:hover {
  border-color: var(--el-color-primary);
}

.tactics-item:last-child {
  margin-bottom: 0;
}

.tactics-head {
  display: flex;
  align-items: center;
  font-size: var(--qt-font-size-base);
}

.tactics-head :deep(.el-tag) {
  height: 20px;
  margin-left: 8px;
  padding: 0 4px;
}

.tactics-icon {
  width: 14px;
  height: 14px;
  margin-right: 4px;
  margin-bottom: 2px;
}

.tactics-icon img {
  width: 100%;
}

.tactics-title {
  color: var(--el-text-color-secondary);
  font-size: var(--qt-font-size-base);
  font-weight: 700;
}

.tactics-item:hover .tactics-title,
.tactics-item.active .tactics-title {
  color: var(--el-text-color-primary);
}

.tactics-body {
  color: var(--el-text-color-secondary);
  font-size: var(--qt-font-size-small);
}

.tactics-item:hover .tactics-body,
.tactics-item.active .tactics-body {
  color: var(--el-text-color-regular);
}

.tactics-item:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.nowrap-label-item :deep(.el-form-item__label) {
  white-space: nowrap;
}

/* 上传区域与表单同宽，详情模式使用只读文件展示。 */
.file-upload-uploader {
  width: 100%;
}

.file-upload-uploader :deep(.el-upload) {
  width: 100%;
}

.file-upload-uploader :deep(.el-upload-dragger) {
  width: 100%;
  box-sizing: border-box;
}

.file-upload-name {
  color: var(--qt-text-regular);
  font-size: var(--el-font-size-small);
  margin-top: var(--qt-space-2);
  overflow-wrap: anywhere;
}

.file-upload-readonly {
  width: 100%;
  min-height: 180px;
  border: var(--el-border-width) dashed var(--qt-border-color);
  border-radius: var(--el-border-radius-base);
  box-sizing: border-box;
  padding: calc(var(--qt-space-4) * 2) var(--qt-space-5);
  text-align: center;
  color: var(--qt-text-secondary);
}

.schedule-content :deep(.el-input-group__append) {
  padding: 0;
}

.schedule-content :deep(.el-input-group__append .el-button) {
  margin: 0;
  border: 0;
  border-radius: 0;
  color: var(--qt-text-inverse);
  background-color: var(--qt-primary);
}

/* 操作区固定在配置卡片底部，保持切换步骤和保存入口可见。 */
.button-style {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 25px 35px 25px 0;
  padding-left: calc(var(--qt-space-5) * 2);
  padding-right: calc(var(--qt-space-5) * 2);
  background: var(--qt-surface-background);
  text-align: right;
  z-index: 10;
}

.detail-tabs {
  margin-bottom: var(--qt-space-5);
}

/* 复用 Element Plus 断点，在中等宽度下收紧步骤区横向留白。 */
@media (max-width: element.$lg) {
  .custom-card {
    padding-left: calc(var(--qt-space-5) + var(--qt-space-1));
    padding-right: calc(var(--qt-space-5) + var(--qt-space-1));
  }
}

/* 窄屏改为单列表单和纵向调度项，减少横向溢出。 */
@media (max-width: element.$md) {
  .custom-card {
    height: auto;
    padding: calc(var(--qt-space-5) + var(--qt-space-1)) var(--qt-space-3);
  }

  .column-form {
    grid-template-columns: minmax(0, 1fr);
  }

  .column-form > .row-full {
    grid-column: 1 / -1;
  }

  .tactics-block {
    flex-direction: column;
  }

  .block-label {
    flex-basis: auto;
    padding: 0 0 var(--qt-space-2);
  }
}
</style>
