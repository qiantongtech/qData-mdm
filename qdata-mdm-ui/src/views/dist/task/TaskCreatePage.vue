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

  Business License: https://qdata.qiantong.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <!-- 步骤条、表单内容区和底部操作区沿用模型表单页结构。 -->
  <div ref="app-container" class="app-container dist-task-create-page">
    <div class="wizard-steps mb15">
      <Steps :steps="stepItems" :active-step="step" :clickable="false"/>
    </div>
    <div class="pagecont-top">
      <div class="infotop">
        <div ref="wizardBodyRef" v-loading="initializing" class="main">
          <!-- 第一步：任务定义及分发类型；QtFormItem 统一字段标签与输入区域结构。 -->
          <template v-if="step === 0">
            <el-form ref="basicFormRef" :model="draft" :rules="basicRules" label-width="110px"
                     scroll-to-error
                     class="column-form" @submit.prevent
            >
              <GuideTipPanel class="row-full" type="primary" allow-wrap
                             title="用于配置主数据分发任务，将选定的权威主数据按照指定范围和分发方式发送至目标系统，实现主数据的统一共享与分发。"/>
              <div class="h2-titles row-full">任务定义</div>

              <qt-form-item label="任务名称" prop="name" required>
                <el-input v-model="draft.name" placeholder="请输入任务名称，支持中文、英文字母、数字、下划线和短横线。" maxlength="64"/>
              </qt-form-item>

              <qt-form-item label="任务编码" prop="code" required>
                <el-input v-model="draft.code" placeholder="请输入任务编码，仅支持英文字母、数字和下划线。" maxlength="64"/>
              </qt-form-item>

              <qt-form-item label="主数据域" prop="projectId" required>
                <el-select v-model="draft.projectId" filterable placeholder="请选择主数据域" @change="handleProjectChange">
                  <template #label="{ label }">
                    <div v-if="selectedProject" class="source-option">
                      <img :src="getProjectIcon(selectedProject.icon)" alt="" class="source-option__icon" />
                      <span class="text-ellipsis">{{ selectedProject.name }}</span>
                    </div>
                    <span v-else>{{ label }}</span>
                  </template>
                  <el-option v-for="item in modelTree" :key="item.id" :value="String(item.id)" :label="item.name">
                    <div class="source-option">
                      <img :src="getProjectIcon(item.icon)" alt="" class="source-option__icon" />
                      <span class="text-ellipsis">{{ item.name }}</span>
                    </div>
                  </el-option>
                </el-select>
              </qt-form-item>

              <qt-form-item label="模型目录" prop="layerId" required>
                <el-select v-model="draft.layerId" filterable :disabled="!draft.projectId" placeholder="请选择模型目录" @change="handleLayerChange">
                  <el-option v-for="item in layerOptions" :key="item.id" :value="String(item.id)" :label="item.name" />
                </el-select>
              </qt-form-item>

              <qt-form-item label="主数据模型" prop="modelId" required>
                <!-- 仅展示已成功发布的模型，避免任务绑定未发布的模型草稿。 -->
                <el-select v-model="draft.modelId" filterable :loading="modelsLoading" :disabled="!draft.layerId" placeholder="请选择本次需要分发的主数据模型。">
                  <template #label="{ label }">
                    <div v-if="selectedModelOption" class="source-option">
                      <img :src="getModelIcon(selectedModelOption)" alt="" class="source-option__icon" />
                      <span class="text-ellipsis">{{ selectedModelOption.label }}</span>
                    </div>
                    <span v-else>{{ label }}</span>
                  </template>
                  <el-option v-for="item in filteredModelOptions" :key="item.value" :label="item.label" :value="item.value">
                    <div class="source-option">
                      <img :src="getModelIcon(item)" alt="" class="source-option__icon" />
                      <span class="text-ellipsis">{{ item.label }}</span>
                    </div>
                  </el-option>
                </el-select>
              </qt-form-item>

              <qt-form-item label="目标系统" prop="targetSystemId" required>
                <el-select v-model="draft.targetSystemId" filterable placeholder="请选择接收本次主数据分发数据的目标系统。">
                  <el-option v-for="item in targetOptions" :key="item.id" :label="item.name" :value="item.id"/>
                </el-select>
              </qt-form-item>

              <qt-form-item label="数据来源">
                <el-input model-value="权威主数据" disabled/>
              </qt-form-item>

              <qt-form-item
                label="负责人"
                prop="responsibleUserId"
              >
                <el-select
                  v-model="draft.responsibleUserId"
                  filterable
                  :loading="usersLoading"
                  placeholder="请选择负责人"
                  style="width: 100%"
                >
                  <el-option v-for="user in userOptions" :key="user.value" :label="user.label" :value="user.value"
                             :disabled="user.disabled"
                  />
                </el-select>
              </qt-form-item>

              <qt-form-item label="联系电话">
                <el-input :model-value="responsiblePhone" placeholder="请输入联系电话" disabled/>
              </qt-form-item>

              <qt-form-item label="任务状态">
                <el-radio-group v-model="draft.status">
                  <el-radio value="DISABLED">未上线</el-radio>
                  <el-radio value="ENABLED">已上线</el-radio>
                </el-radio-group>
              </qt-form-item>

              <qt-form-item label="描述" class="row-full">
                <!-- 与目标系统描述框一致：限制 256 字，并在右下角显示“已输入/256个字符”。 -->
                <div class="description-input">
                  <el-input v-model="draft.remark" type="textarea" :rows="3" maxlength="256个字符"
                            show-word-limit
                            placeholder="可填写任务用途、分发范围或其他补充说明，最多256个字符。"
                  />
                </div>
              </qt-form-item>

            </el-form>
            <el-form ref="incrementalFormRef" :model="draft" :rules="incrementalRules"
                     label-width="110px" class="column-form" scroll-to-error @submit.prevent
            >
              <div class="h2-titles row-full">分发类型</div>
              <qt-form-item label="分发类型" prop="dataMode" tip="仅分发自上次成功分发后新增或发生变化的主数据，减少重复传输。">
                <el-radio-group v-model="draft.dataMode">
                  <el-radio value="FULL">全量</el-radio>
                  <el-radio value="INCREMENTAL">增量</el-radio>
                </el-radio-group>
              </qt-form-item>
              <qt-form-item v-if="draft.dataMode === 'INCREMENTAL'" label="首次增量开始时间" prop="incrementalStartTime"
                            class="incremental-start-field" required tip="用于确定首次增量分发的数据起点，系统将从该时间开始筛选需要分发的数据。"
              >
                <el-date-picker v-model="draft.incrementalStartTime" type="datetime"
                                value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择日期时间" style="width: 100%"
                />
              </qt-form-item>
            </el-form>
          </template>
          <!-- 第二步：数据范围、筛选条件及主数据到目标 API 的字段映射。 -->
          <template v-else-if="step === 1">
            <el-form :model="draft" class="column-form" label-width="110px" style="padding-bottom: 16px" @submit.prevent>
              <GuideTipPanel class="row-full" type="primary" allow-wrap
                             title="用于确定本次主数据分发的数据范围。支持分发全部数据或按条件筛选指定数据，满足不同业务场景下的数据分发需求。"/>
              <div class="h2-titles row-full">数据范围</div>
              <qt-form-item label="分发数据范围" prop="dataScopeType" class="row-full"
                            tip="全部数据：分发所选主数据模型下的全部数据；条件筛选：仅分发符合所设条件的数据。">
                <el-radio-group v-model="draft.dataScopeType">
                  <el-radio value="ALL">全部数据</el-radio>
                  <el-radio value="CONDITION">条件筛选</el-radio>
                </el-radio-group>
              </qt-form-item>
            </el-form>
            <!-- 条件筛选条件编辑区。 -->
            <qt-wrap
                v-if="draft.dataScopeType === 'CONDITION'"
                :columns="filterColumns"
                :table-ref="filterTableRef"
                :config="{ fullContent: false, actions: { table: { show: false } } }"
                class="mt15 mb15"
            >
              <template #actions-data>
                <el-button type="primary" plain size="small" icon="Plus" @click="addFilter">添加条件</el-button>
                <el-button type="danger" plain size="small" icon="Delete" :disabled="!selectedFilters.length"
                           @click="removeSelectedFilters"
                >批量删除
                </el-button>
              </template>
              <qt-table
                  ref="filterTableRef"
                  class="condition-table"
                  :columns="filterColumns"
                  :func="loadFilterRows"
                  :config="{ notPagination: true, table: { rowKey: 'key', emptyText: '暂无筛选条件', onSelectionChange: handleFilterSelectionChange } }"
              >
                <template #field="{ row }">
                  <el-select v-model="row.modelColumnId" placeholder="请选择字段"
                             @change="handleFilterFieldChange(row)"
                  >
                    <el-option v-for="field in modelFields" :key="field.id" :label="field.name" :value="field.id"/>
                  </el-select>
                  <div v-if="filterErrors[row.key]?.modelColumnId" class="filter-error">
                    {{ filterErrors[row.key].modelColumnId }}
                  </div>
                </template>
                <template #operator="{ row }">
                  <el-select v-model="row.operator" :disabled="!row.modelColumnId" placeholder="请选择表达式"
                             @change="handleFilterOperatorChange(row)"
                  >
                    <el-option v-for="item in getFilterOperators(row)" :key="item.value" :label="item.label"
                               :value="item.value"
                    />
                  </el-select>
                  <div v-if="filterErrors[row.key]?.operator" class="filter-error">{{
                      filterErrors[row.key].operator
                    }}
                  </div>
                </template>
                <template #value="{ row }">
                  <!-- 比较值控件跟随模型字段类型；为空类运算符无需输入。 -->
                  <el-input v-if="isNullOperator(row.operator)" model-value="" disabled placeholder="无需填写"/>
                  <el-date-picker v-else-if="getFilterFieldKind(row) === 'date'" v-model="row.value"
                                  :disabled="!row.operator" type="date" value-format="YYYY-MM-DD"
                                  placeholder="请选择日期" @change="clearFilterError(row)"
                  />
                  <el-date-picker v-else-if="getFilterFieldKind(row) === 'datetime'" v-model="row.value"
                                  :disabled="!row.operator" type="datetime" value-format="YYYY-MM-DD HH:mm:ss"
                                  placeholder="请选择日期时间" @change="clearFilterError(row)"
                  />
                  <el-select v-else-if="getFilterFieldKind(row) === 'boolean'" v-model="row.value"
                             :disabled="!row.operator" placeholder="请选择比较值" @change="clearFilterError(row)"
                  >
                    <el-option label="是" value="true"/>
                    <el-option label="否" value="false"/>
                  </el-select>
                  <el-input v-else v-model="row.value" :disabled="!row.operator"
                            :type="getFilterFieldKind(row) === 'number' ? 'number' : 'text'"
                            placeholder="请输入比较值"
                            @input="clearFilterError(row)"
                  />
                  <div v-if="filterErrors[row.key]?.value" class="filter-error">{{
                      filterErrors[row.key].value
                    }}
                  </div>
                </template>
                <template #action="{ row, $index }">
                  <el-button link type="danger" icon="Delete" @click="removeFilter(row, $index)">删除</el-button>
                </template>
              </qt-table>
            </qt-wrap>
            <div v-if="draft.dataScopeType === 'CONDITION' && scopeError" class="section-error">{{ scopeError }}</div>
            <qt-wrap
                class="mapping-table"
                :columns="mappingColumns"
                :table-ref="mappingTableRef"
                :config="{ fullContent: false, actions: { table: { show: false } } }"
            >
              <template #actions-data>
                <div class="section-heading">
                  <div class="h2-titles">分发字段与字段映射</div>
                  <el-text type="info" size="small">
                    勾选需要分发的主数据字段，并配置对应的下游 API 字段名称，建立字段映射关系；标记为必选的字段需保留。
                  </el-text>
                </div>
              </template>
              <qt-table ref="mappingTableRef" :config="{ notPagination: true, table: { stripe: true } }"
                        :columns="mappingColumns" :func="loadMappingRows"
              >
                <template #selected="{ row }">
                  <el-checkbox v-model="row.selected" :disabled="row.required" @change="clearMappingError(row)"/>
                </template>
                <template #source="{ row }">{{ row.name }}
                  <dict-tag v-if="row.required" size="small" type="info">必选</dict-tag>
                </template>
                <template #target="{ row }">
                  <el-input v-model="row.targetField" :disabled="!row.selected" maxlength="512"
                            placeholder="请输入下游 API 字段" @input="clearMappingError(row)"
                  />
                  <div v-if="mappingErrors[row.id]" class="filter-error">{{ mappingErrors[row.id] }}</div>
                </template>
              </qt-table>
            </qt-wrap>
            <div v-if="mappingError" class="section-error">{{ mappingError }}</div>
            <el-empty v-if="!draft.modelId" description="选择主数据模型后显示可分发字段" :image-size="65"/>
          </template>
          <!-- 第三步：目标 API、认证请求头、请求体预览和成功判定。 -->
          <template v-else-if="step === 2">
            <el-form ref="apiFormRef" :model="draft" :rules="apiRules" label-width="140px"
                     scroll-to-error
                     class="column-form" @submit.prevent
            >
              <div class="h2-titles row-full">目标 API</div>
              <qt-form-item label="目标 API 地址" prop="apiUrl" required
                            tip="填写目标系统提供的接收接口地址；请求方式需与目标 API 定义保持一致。">
                <el-input v-model="draft.apiUrl" maxlength="512" placeholder="请输入目标 API 地址"/>
              </qt-form-item>
              <qt-form-item label="请求方式">
                <el-input model-value="POST" disabled/>
              </qt-form-item>
            </el-form>
            <el-form ref="authFormRef" :model="draft" :rules="authRules" label-width="140px"
                     scroll-to-error
                     class="column-form" @submit.prevent
            >
              <div class="h2-titles row-full mt15">认证与请求头</div>
              <qt-form-item label="认证方式" prop="authType" required>
                <el-select v-model="draft.authType" placeholder="请选择认证方式">
                  <el-option label="无需认证" value="NONE"/>
                  <el-option label="Bearer Token" value="BEARER"/>
                  <el-option label="API Key" value="API_KEY"/>
                </el-select>
              </qt-form-item>
              <qt-form-item label="内容类型">
                <el-input model-value="application/json（系统维护）" disabled/>
              </qt-form-item>
              <qt-form-item v-if="draft.authType === 'BEARER'" label="Bearer Token" prop="bearerToken" required>
                <el-input v-model="draft.bearerToken" type="password" show-password
                          autocomplete="new-password" placeholder="请输入 Token"
                />
              </qt-form-item>
              <template v-if="draft.authType === 'API_KEY'">
                <qt-form-item label="API Key 名称" prop="apiKeyName" required>
                  <el-input v-model="draft.apiKeyName" placeholder="例如 X-API-Key"/>
                </qt-form-item>
                <qt-form-item label="API Key 值" prop="apiKeyValue" required>
                  <el-input v-model="draft.apiKeyValue" type="password" show-password
                            autocomplete="new-password" placeholder="请输入 API Key"
                  />
                </qt-form-item>
              </template>
              <el-form-item label="自定义请求头" class="row-full">
                <div class="header-editor">
                  <div v-for="(header, index) in draft.headers" :key="header.id" class="header-row">
                    <div v-for="field in headerFields" :key="field.key" class="header-field">
                      <el-input v-model="header[field.key]" :maxlength="field.maxlength"
                                :placeholder="field.placeholder" :aria-label="field.label"
                                :aria-invalid="!!headerErrors[header.id]?.[field.key]"
                                :aria-describedby="headerErrors[header.id]?.[field.key] ? `header-${header.id}-${field.key}-error` : undefined"
                                @input="clearHeaderError(header, field.key)"
                      />
                      <span v-if="headerErrors[header.id]?.[field.key]" :id="`header-${header.id}-${field.key}-error`"
                            class="filter-error">{{ headerErrors[header.id][field.key] }}</span>
                    </div>
                    <el-button text circle icon="Delete" class="header-remove"
                               :aria-label="`删除第 ${index + 1} 个请求头`" title="删除请求头"
                               @click="removeHeader(index)"/>
                  </div>
                  <div class="header-actions">
                    <el-button link type="primary" icon="Plus" @click="addHeader">添加请求头</el-button>
                    <span v-if="!draft.headers.length">可选</span>
                  </div>
                  <div v-if="headerError" class="filter-error">{{ headerError }}</div>
                </div>
              </el-form-item>
            </el-form>
            <qt-wrap :config="{ fullContent: false, actions: { table: { show: false } } }">
              <template #actions-data>
                <div class="section-heading mt15">
                  <div class="h2-titles">请求体预览</div>
                  <el-text type="info" size="small">
                    请求体根据上一步选择的分发字段及字段映射自动生成，展示本次发送给目标系统的数据结构。
                  </el-text>
                </div>
              </template>
              <div class="preview-wrap">
                <SqlEditor :value="requestBodyPreview" mode="application/json" theme="default"
                           auto-height :read-only="true" :autofocus="false"/>
              </div>
            </qt-wrap>
            <el-form ref="successFormRef" :model="draft" :rules="successRules"
                     label-width="110px" class="column-form" scroll-to-error @submit.prevent
            >
              <div class="h2-titles row-full mt15">成功判定</div>
              <qt-form-item label="分发成功判定" prop="successMode" class="row-full" style="padding-bottom: 16px"
                            tip="仅 HTTP 2xx：接口返回 200–299 即判定成功；HTTP 2xx + 业务成功条件：还需满足指定响应字段及成功值。">
                <el-radio-group v-model="draft.successMode">
                  <el-radio value="HTTP">仅 HTTP 2xx</el-radio>
                  <el-radio value="BUSINESS">HTTP 2xx + 业务成功条件</el-radio>
                </el-radio-group>
              </qt-form-item>
              <template v-if="draft.successMode === 'BUSINESS'">
                <qt-form-item label="响应字段" prop="successField" required>
                  <el-input v-model="draft.successField" maxlength="100"
                            placeholder="请填写接口返回结果中用于判断业务是否成功的字段，例如 code、success。"/>
                </qt-form-item>
                <qt-form-item label="比较方式" prop="successOperator" required style="padding-bottom: 16px"
                              tip="选择系统判断响应字段与成功值是否匹配的方式。">
                  <el-select v-model="draft.successOperator" placeholder="请选择比较方式">
                    <el-option label="等于" value="EQ"/>
                    <el-option label="不等于" value="NE"/>
                  </el-select>
                </qt-form-item>
                <qt-form-item label="成功值" prop="successValue" required>
                  <el-input v-model="draft.successValue" maxlength="256"
                            placeholder="请填写判定业务成功时该字段对应的返回值，例如 200、true。"/>
                </qt-form-item>
              </template>
            </el-form>
          </template>
          <!-- 第四步：执行方式与重试机制配置。 -->
          <template v-else>
            <el-form ref="scheduleFormRef" :model="draft" :rules="scheduleRules" label-width="130px"
                     scroll-to-error
                     class="column-form schedule-content" @submit.prevent
            >
              <div class="h2-titles row-full">执行方式</div>
              <qt-form-item label="执行方式" class="row-full">
                <el-radio-group v-model="draft.triggerType">
                  <el-radio value="MANUAL">人工执行</el-radio>
                  <el-radio value="SCHEDULED">定时执行</el-radio>
                </el-radio-group>
              </qt-form-item>
              <qt-form-item v-if="draft.triggerType === 'SCHEDULED'" label="调度规则" prop="cronExpression" required>
                <el-input v-model="draft.cronExpression" placeholder="请输入调度规则，如：每天 02:00 执行、每周一 03:00 执行">
                  <template #append>
                    <el-button type="primary" @click="openCronGenerator">配置</el-button>
                  </template>
                </el-input>
              </qt-form-item>
            </el-form>
            <el-form v-if="draft.triggerType === 'SCHEDULED'" :model="draft" label-width="130px"
                     class="column-form" @submit.prevent>
              <div class="h2-titles row-full mt15">重试机制配置</div>
              <qt-form-item label="失败自动重试" class="row-full">
                <el-switch v-model="draft.failRetryFlag" active-value="1" inactive-value="0"
                           aria-label="失败自动重试"/>
              </qt-form-item>
              <template v-if="autoRetryEnabled">
                <qt-form-item label="失败重试次数">
                  <el-input-number v-model="draft.failRetryTimes" :min="1" :precision="0" placeholder="请输入重试次数，建议 1–5 次" class="number-input"/>
                </qt-form-item>
                <qt-form-item label="重试间隔（分钟）">
                  <el-input-number v-model="draft.failRetryInterval" :min="1" :precision="0" placeholder="请输入重试间隔，建议 5–60 分钟" class="number-input"/>
                </qt-form-item>
                <qt-form-item label="超出次数处理">
                  <el-select v-model="draft.failRetryOutType" placeholder="请选择超出次数处理方式">
                    <el-option v-for="option in retryOutOptions" :key="option.value" :label="option.label"
                               :value="option.value"
                    />
                  </el-select>
                </qt-form-item>
              </template>
            </el-form>
          </template>
        </div>
      </div>
      <!-- 底部操作区沿用模型表单页结构。 -->
      <div class="button-style">
        <el-button @click="goBack">返回列表</el-button>
        <el-button v-if="step > 0" type="primary" plain @click="step--">上一步</el-button>
        <el-button v-if="step < 3" type="primary" :loading="stepValidating" :disabled="initializing"
                   @click="handleNext"
        >下一步
        </el-button>
        <el-button v-else type="primary" :loading="saving" :disabled="initializing" @click="handleSubmit">确认
        </el-button>
      </div>
    </div>
    <el-dialog v-model="cronOpen" title="Cron 表达式生成器" width="860px"
               :append-to="$refs['app-container']" destroy-on-close>
      <Crontab v-if="cronOpen" :expression="cronExpressionDraft" @hide="cronOpen = false" @fill="fillCron"/>
    </el-dialog>
  </div>
</template>
<script setup>
import { getModelIcon, getProjectIcon } from '@/utils/imageUrl';
import {computed, h, nextTick, onMounted, reactive, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElCheckbox, ElMessage} from 'element-plus';
import {listTargetSystem} from '@/api/dist/target/targetSystem';
import { useUserDirectoryStore } from '@/store/system/userDirectory';
import {getDicts} from '@/api/system/system/dict/data';
import {addTASK, getTASK, updateTASK} from '@/api/dist/task/TASK';
import {listPublishedModels, getPublishedModelStructure} from '@/api/model/base/published';
import {getPublishedModelTree} from '@/api/model/base/base';
import {parseTime} from '@/utils/anivia';
import {trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired} from '@/utils/validate.js';
import Crontab from '@/components/Crontab/index.vue';
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue';
import Steps from '@/components/Steps/index.vue';
import SqlEditor from '@/components/SqlEditor/index.vue';
import {usePageRefresh} from '@/composables/usePageRefresh';

const userDirectoryStore = useUserDirectoryStore();

const route = useRoute();
const router = useRouter();
const {setRefreshNeeded} = usePageRefresh('distTask');
const taskId = computed(() => route.query.id || null);
const isEdit = computed(() => taskId.value != null);
const originalAuthType = ref('NONE');
const originalApiKeyName = ref('');
const originalHeaders = ref([]);
const MASKED_SECRET = '******';
const stepItems = [{title: '基本信息'}, {title: '数据范围'}, {title: 'API 配置'}, {title: '执行方式'}];
/** 运算符编码与分发过滤条件接口一致；不同字段类型只展示可执行的选项。 */
const commonOperators = [{label: '等于', value: 'EQ'}, {label: '不等于', value: 'NE'}];
const nullOperators = [{label: '为空', value: 'IS_NULL'}, {label: '不为空', value: 'IS_NOT_NULL'}];
const textOperators = [{label: '包含', value: 'CONTAINS'}, {label: '不包含', value: 'NOT_CONTAINS'}];
const orderedOperators = [{label: '大于', value: 'GT'}, {label: '大于等于', value: 'GE'}, {
  label: '小于',
  value: 'LT'
}, {label: '小于等于', value: 'LE'}];
/** 仅用于打开历史示例任务；新任务只能选择已发布模型接口返回的模型。 */
const legacyModelId = -1;
const legacyModelFields = [
  {id: -11, code: 'masterDataId', name: '主数据ID', dataType: '文本', required: true, targetField: 'masterDataId'},
  {id: -12, code: 'supplierCode', name: '供应商编码', dataType: '文本', required: false, targetField: 'supplierCode'},
  {id: -13, code: 'supplierName', name: '供应商名称', dataType: '文本', required: false, targetField: 'supplierName'}
];
const step = ref(0);
const stepValidating = ref(false);
const saving = ref(false);
const wizardBodyRef = ref();
const basicFormRef = ref();
const incrementalFormRef = ref();
const apiFormRef = ref();
const authFormRef = ref();
const successFormRef = ref();
const scheduleFormRef = ref();
const cronOpen = ref(false);
const cronExpressionDraft = ref('');
const mappingTableRef = ref();
const filterTableRef = ref();
const modelOptions = ref([]);
const modelTree = ref([]);
const modelsLoading = ref(false);
let modelLoadSequence = 0;
const targetOptions = ref([]);
const userOptions = ref([]);
/** 超出次数处理的选项由系统字典维护，任务只保存所选字典值。 */
const retryOutOptions = ref([]);
const usersLoading = ref(false);
/** 联系电话来自所选负责人的平台账号资料，任务表仅保存用户 ID。 */
const responsiblePhone = computed(() => userOptions.value.find(user => String(user.value) === String(draft
    .responsibleUserId))?.phone || '');
const modelFields = ref([]);
const filterErrors = reactive({});
const scopeError = ref('');
const mappingError = ref('');
const mappingErrors = reactive({});
const headerErrors = reactive({});
const headerError = ref('');
const initializing = ref(false);
/** 新建任务初始化筛选条件和重试配置默认值。 */
const newDraft = () => ({
  name: '',
  code: '',
  projectId: null,
  layerId: null,
  modelId: null,
  modelVersionId: null,
  targetSystemId: null,
  responsibleUserId: null,
  creatorId: null,
  status: 'DISABLED',
  remark: '',
  dataMode: 'FULL',
  incrementalStartTime: '',
  dataScopeType: 'CONDITION',
  filters: [{key: 1, selected: false, modelColumnId: null, operator: '', value: ''}],
  mappings: [],
  apiUrl: '',
  authType: 'NONE',
  bearerToken: '',
  apiKeyName: '',
  apiKeyValue: '',
  headers: [],
  successMode: 'HTTP',
  successField: '',
  successOperator: '',
  successValue: '',
  triggerType: 'SCHEDULED',
  cronExpression: '',
  failRetryFlag: '1',
  failRetryTimes: null,
  failRetryInterval: null,
  failRetryOutType: null
});
const draft = reactive(newDraft());
const autoRetryEnabled = computed(() => draft.triggerType === 'SCHEDULED' && draft.failRetryFlag === '1');
/** 必填校验与各步骤红色星号对应，沿用项目 el-form / QtFormItem 的字段提示。 */
const basicRules = {
  name: [
    {required: true, validator: validateTrimmedRequired('任务名称不能为空', 64), trigger: 'blur'},
    {validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, '任务名称只允许中文、英文字母、数字、下划线和短横线'), trigger: 'blur', transform: trimText}
  ],
  code: [
    {required: true, validator: validateTrimmedRequired('任务编码不能为空', 64), trigger: 'blur'},
    {validator: validateAlphaNumUnderscore('任务编码只允许英文字母、数字和下划线'), trigger: 'blur', transform: trimText}
  ],
  projectId: [{required: true, message: '请选择主数据域', trigger: 'change'}],
  layerId: [{required: true, message: '请选择模型目录', trigger: 'change'}],
  modelId: [{required: true, message: '请选择主数据模型', trigger: 'change'}],
  targetSystemId: [{required: true, message: '请选择目标系统', trigger: 'change'}]
};
const incrementalRules = {
  incrementalStartTime: [{required: true, message: '请选择首次增量开始时间', trigger: 'change'}]
};
const apiRules = {
  apiUrl: [{
    validator: (_rule, value, callback) => {
      // 地址格式与认证配置分别提示，避免把缺少主机或端口写法错误误报为账号密码问题。
      const address = String(value || '').trim();
      if (!address) return callback(new Error('请输入目标 API 地址'));
      if (!/^https?:\/\/\S+$/i.test(address)) return callback(new Error(
          '请输入完整地址，例如 http://127.0.0.1:8080/api'));
      try {
        const url = new URL(address);
        if (!url.hostname) throw new Error();
        if (url.username || url.password) return callback(new Error('请在认证方式中填写凭据，不要放在 URL 中'));
        callback();
      } catch {
        callback(new Error('目标 API 地址格式不正确，例如 http://127.0.0.1:8080/api'));
      }
    },
    trigger: 'blur'
  }]
};
const authRules = {
  authType: [{required: true, message: '请选择认证方式', trigger: 'change'}],
  bearerToken: [{required: true, whitespace: true, message: '请输入 Bearer Token', trigger: 'blur'}],
  apiKeyName: [{
    required: true,
    whitespace: true,
    message: '请输入 API Key 请求头名称',
    trigger: 'blur'
  }, {pattern: /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/, message: '请输入有效的 HTTP 请求头名称', trigger: 'blur'}],
  apiKeyValue: [{required: true, whitespace: true, message: '请输入 API Key', trigger: 'blur'}]
};
const successRules = {
  successField: [{
    required: true,
    whitespace: true,
    message: '请输入响应字段',
    trigger: 'blur'
  }, {pattern: /^[A-Za-z_][A-Za-z0-9_]*$/, message: '仅支持顶层响应字段名', trigger: 'blur'}],
  successOperator: [{required: true, message: '请选择比较方式', trigger: 'change'}],
  successValue: [{required: true, whitespace: true, message: '请输入成功值', trigger: 'blur'}]
};
/** 定时执行才校验调度周期；人工执行不需要 Cron 表达式。 */
const scheduleRules = {
  cronExpression: [{required: true, whitespace: true, message: '请输入调度规则', trigger: 'blur'}]
};
let filterKey = 1;
let headerKey = 0;
const headerFields = [
  {key: 'key', label: '请求头名称', placeholder: '名称，如 X-Request-ID', maxlength: 100},
  {key: 'value', label: '请求头值', placeholder: '请求头值', maxlength: 256}
];
const filterColumns = [
  {type: 'selection', width: 46, align: 'center', reserveSelection: true},
  {label: '字段', prop: 'modelColumnId', slot: 'field', minWidth: 190},
  {label: '表达式', prop: 'operator', slot: 'operator', minWidth: 190},
  {label: '比较值', prop: 'value', slot: 'value', minWidth: 260},
  {label: '操作', slot: 'action', width: 96, align: 'center'}
];
const mappingColumns = [{prop: 'selected', width: 80, slot: 'selected', renderHeader: renderMappingSelectionHeader}, {
  label: '主数据字段',
  prop: 'name',
  minWidth: 200,
  slot: 'source',
  align: 'left'
}, {label: '字段类型', prop: 'dataType', minWidth: 160, align: 'left'}, {
  label: '下游 API 字段',
  prop: 'targetField',
  minWidth: 260,
  slot: 'target',
  align: 'left'
}];
const selectedFilters = ref([]);
/** 条件列表与任务保存接口结构一致；字段 ID 来自当前已发布模型结构。 */
const filterPayload = computed(() => draft.dataScopeType === 'CONDITION' ?
    draft.filters.map((row, index) => ({
      modelColumnId: row.modelColumnId,
      operator: row.operator,
      filterValue: isNullOperator(row.operator) ? null : String(row.value ?? '').trim(),
      sortOrder: index + 1
    })) :
    []);
const selectedMappings = computed(() => draft.mappings.filter(item => item.selected));

function renderMappingSelectionHeader() {
  const rows = draft.mappings;
  const allSelected = rows.length > 0 && rows.every(row => row.selected);
  return h(ElCheckbox, {
    modelValue: allSelected,
    indeterminate: !allSelected && rows.some(row => row.selected),
    disabled: !rows.some(row => !row.required),
    'aria-label': '全选可选分发字段',
    'onUpdate:modelValue': value => {
      rows.filter(row => !row.required).forEach(row => {
        row.selected = Boolean(value);
        clearMappingError(row);
      });
    }
  });
}
/** 只提交已勾选字段；字段选择由映射行是否存在表示，不向后端发送展示字段。 */
const fieldMappingPayload = computed(() => selectedMappings.value.map((item, index) => ({
  modelColumnId: item.id,
  targetFieldName: String(item.targetField || '').trim(),
  sortOrder: index + 1
})));
/** 预览只包含已选且已填写映射的字段；展示的是示例结构，不读取真实主数据。 */
const requestBodyPreview = computed(() => JSON.stringify(Object.fromEntries(fieldMappingPayload.value.filter(item =>
    item.targetFieldName).map(item => [item.targetFieldName, '示例值'])), null, 2));
/** API 配置与生成器的任务保存字段保持一致，正式模型接口接入后可直接用于提交。 */
const apiConfigPayload = computed(() => {
  const savedAuth = isEdit.value && draft.authType === originalAuthType.value;
  const authConfig = draft.authType === 'BEARER' ?
      savedAuth && draft.bearerToken === MASKED_SECRET ? null : JSON.stringify({token: draft.bearerToken.trim()}) :
      draft.authType === 'API_KEY' ?
          savedAuth && draft.apiKeyName.trim() === originalApiKeyName.value && draft.apiKeyValue === MASKED_SECRET ?
              null : JSON.stringify({headerName: draft.apiKeyName.trim(), key: draft.apiKeyValue.trim()}) :
          null;
  const headersUnchanged = isEdit.value && draft.headers.length === originalHeaders.value.length &&
      draft.headers.every((row, index) => row.key.trim() === originalHeaders.value[index] && row.value ===
          MASKED_SECRET);
  return {
    apiUrl: draft.apiUrl.trim(),
    httpMethod: 'POST',
    authType: draft.authType,
    authConfig,
    // 未改动的配置不提交；改动时由服务端将遮蔽占位符还原为原有值后再保存。
    headerConfig: headersUnchanged ? null : JSON.stringify(draft.headers.map(item => ({
      key: item.key.trim(),
      value: item.value.trim(),
      ...(item.savedKey && item.key.trim() !== item.savedKey ? {originalKey: item.savedKey} : {})
    }))),
    requestBodyMode: 'FLAT_JSON',
    successCondition: draft.successMode === 'BUSINESS' ? JSON.stringify({
      field: draft.successField.trim(),
      operator: draft.successOperator,
      value: draft.successValue.trim()
    }) : isEdit.value ? '' : null
  };
});

/** 将已发布结构中的字段信息转换为筛选器、映射表共用的数据形状。 */
function normalizePublishedFields(fields = []) {
  return fields.filter(field => !(field.delFlag === true || field.delFlag === 1 || field.delFlag === '1' || field
      .delFlag === 'true'))
      .map(field => ({
        id: Number(field.id),
        code: field.code || '',
        name: field.name || field.code || `字段 #${field.id}`,
        dataType: field.dataType || '-',
        required: field.primaryKeyFlag === true || field.primaryKeyFlag === 1 || field.primaryKeyFlag === '1' ||
            field.requiredFlag === true || field.requiredFlag === 1 || field.requiredFlag === '1',
        targetField: field.code || ''
      }));
}

/** 读取所选模型最近一次成功发布的结构，并使用该结构的版本和字段 ID 保存配置。 */
async function loadPublishedModelStructure(modelId) {
  const requestSequence = ++modelLoadSequence;
  modelFields.value = [];
  draft.modelVersionId = null;
  try {
    const response = await getPublishedModelStructure(modelId);
    if (requestSequence !== modelLoadSequence) return false;
    const structure = response?.data;
    if (!structure || String(structure.modelId) !== String(modelId) || structure.versionId == null) {
      throw new Error('已发布模型结构与所选模型不匹配');
    }
    modelFields.value = normalizePublishedFields(structure.fields);
    draft.modelVersionId = structure.versionId;
    return true;
  } catch {
    if (requestSequence === modelLoadSequence) {
      modelFields.value = [];
    }
    return false;
  }
}

/** 初始化已发布模型、目标系统、平台用户和编辑详情；用户列表用于负责人选择与电话回显。 */
async function initialize() {
  initializing.value = true;
  try {
    Object.assign(draft, newDraft());
    originalAuthType.value = 'NONE';
    originalApiKeyName.value = '';
    originalHeaders.value = [];
    filterKey = 1;
    step.value = 0;
    stepValidating.value = false;
    saving.value = false;
    cronOpen.value = false;
    cronExpressionDraft.value = '';
    clearStepTwoErrors();
    clearHeaderErrors();
    headerKey = 0;
    modelFields.value = [];
    modelOptions.value = [];
    draft.mappings = [];
    modelsLoading.value = true;
    usersLoading.value = true;
    const [models, targets, users, retryOutTypes, existing, tree] = await Promise.allSettled([
      listPublishedModels({pageNum: 1, pageSize: 1000}),
      listTargetSystem({pageNum: 1, pageSize: 1000}),
      userDirectoryStore.loadUserList(),
      getDicts('dmd_push_retry_out_type'),
      isEdit.value ? getTASK(taskId.value, {includeFilters: true, includeFieldMappings: true}) : Promise
          .resolve(null),
      getPublishedModelTree()
    ]);
    modelsLoading.value = false;
    usersLoading.value = false;
    const publishedModels = models.status === 'fulfilled' ? (models.value.data?.rows || models.value.rows || []) : [];
    modelOptions.value = publishedModels.map(model => ({
      value: Number(model.id),
      label: model.name || model.code || `模型 #${model.id}`,
      code: model.code,
      type: model.type,
      projectId: model.projectId,
      layerId: model.layerId,
      versionId: model.versionId
    }));
    modelTree.value = tree.status === 'fulfilled' ? (tree.value.data || []) : [];
    if (tree.status !== 'fulfilled') ElMessage.error('主数据域和模型目录加载失败，请刷新页面重试');
    targetOptions.value = targets.status === 'fulfilled' ? (targets.value.data?.rows || []) : [];
    userOptions.value = users.status === 'fulfilled' ? (users.value.rows || users.value.data?.rows || []).map(user =>
        ({
          value: user.userId,
          label: user.nickName || user.userName,
          phone: user.phonenumber || ''
        })) : [];
    if (users.status !== 'fulfilled') ElMessage.error('负责人列表加载失败，请刷新页面重试');
    retryOutOptions.value = retryOutTypes.status === 'fulfilled' ?
        (retryOutTypes.value.data || []).map(item => ({label: item.dictLabel, value: String(item.dictValue)})) : [];
    if (retryOutTypes.status !== 'fulfilled') ElMessage.error('超出次数处理字典加载失败，请刷新页面重试');
    if (isEdit.value) {
      if (existing.status !== 'fulfilled' || !existing.value?.data) {
        ElMessage.error('分发任务加载失败，请返回列表重试');
      } else {
        await fillExistingTask(existing.value.data, models.status === 'fulfilled');
      }
    }
  } catch {
    ElMessage.error('分发任务加载失败，请返回列表重试');
  } finally {
    usersLoading.value = false;
    modelsLoading.value = false;
    initializing.value = false;
  }
}

/** 回填任务、筛选、映射和重试配置；负责人账号不可用时保留原 ID 供识别。 */
async function fillExistingTask(task, modelListLoaded = true) {
  const modelId = Number(task.modelId);
  const selectedModel = modelOptions.value.find(item => item.value === modelId);
  draft.projectId = selectedModel?.projectId == null ? null : String(selectedModel.projectId);
  draft.layerId = selectedModel?.layerId == null ? null : String(selectedModel.layerId);
  const modelAvailable = modelOptions.value.some(item => item.value === modelId);
  if (!modelAvailable && modelListLoaded) {
    // 已封存或未成功发布的旧关联不加入可选列表，要求改绑到当前可用模型。
    draft.modelId = null;
    draft.modelVersionId = null;
    modelFields.value = [];
    ElMessage.warning('当前任务关联的主数据模型已不可用，请重新选择已发布模型');
  } else if (modelId === legacyModelId) {
    draft.modelId = modelId;
    modelFields.value = legacyModelFields.map(field => ({...field}));
  } else {
    draft.modelId = modelId;
    await loadPublishedModelStructure(modelId);
  }
  await nextTick();
  const mappings = task.fieldMappings || [];
  const filters = task.filters || [];
  // el-select 的值类型须与用户选项一致；后端 Long 被序列化为字符串时也能正确选中。
  const savedResponsibleId = task.responsibleUserId;
  const matchedResponsible = userOptions.value.find(user => String(user.value) === String(savedResponsibleId));
  const savedMappings = new Map(mappings.map(item => [Number(item.modelColumnId), item]));
  const savedApiKeyName = task.authHeaderName || task.headers?.find(header => header.source === 'AUTH')?.key || '';
  Object.assign(draft, {
    name: task.name || '',
    code: task.code || '',
    modelVersionId: !modelAvailable && modelListLoaded ?
        null :
        modelId === legacyModelId ? task.modelVersionId : (draft.modelVersionId || task.modelVersionId),
    targetSystemId: task.targetSystemId,
    responsibleUserId: savedResponsibleId == null ? null : (matchedResponsible?.value ?? savedResponsibleId),
    creatorId: task.creatorId == null ? null : String(task.creatorId),
    status: task.status || 'DISABLED',
    remark: task.remark || '',
    dataMode: task.dataMode || 'FULL',
    incrementalStartTime: task.incrementalStartTime ? parseTime(task.incrementalStartTime) : '',
    dataScopeType: task.dataScopeType || 'ALL',
    filters: filters.map((item, index) => ({
      key: index + 1,
      selected: false,
      modelColumnId: Number(item.modelColumnId),
      operator: item.operator || '',
      value: item.filterValue ?? ''
    })),
    mappings: modelFields.value.map(field => ({
      ...field,
      selected: field.required || savedMappings.has(field.id),
      targetField: savedMappings.get(field.id)?.targetFieldName || field.targetField || ''
    })),
    apiUrl: task.apiUrl || '',
    authType: task.authType || 'NONE',
    bearerToken: task.authType === 'BEARER' ? MASKED_SECRET : '',
    apiKeyName: savedApiKeyName,
    apiKeyValue: task.authType === 'API_KEY' && savedApiKeyName ? MASKED_SECRET : '',
    headers: (task.headers || []).filter(header => header.source !== 'AUTH').map(header => ({
      id: ++headerKey,
      key: header.key,
      value: MASKED_SECRET,
      savedKey: header.key
    })),
    // 编辑页以详情接口返回值回填；旧任务缺值时默认人工重试并关闭自动重试。
    triggerType: task.triggerType || 'MANUAL',
    failRetryFlag: task.triggerType === 'SCHEDULED' && task.retryTriggerType === 'SCHEDULED' && task.failRetryFlag === '1' ? '1' : '0',
    failRetryTimes: task.failRetryTimes ?? null,
    failRetryInterval: task.failRetryInterval ?? null,
    failRetryOutType: task.failRetryOutType ?? null,
    cronExpression: task.cronExpression || ''
  });
  if (draft.responsibleUserId != null && !userOptions.value.some(user => String(user.value) === String(draft
      .responsibleUserId))) {
    userOptions.value.unshift({
      value: draft.responsibleUserId,
      label: task.responsibleUserName || String(draft.responsibleUserId),
      phone: task.responsiblePhone || '',
      disabled: true
    });
  }
  if (draft.responsibleUserId != null) {
    await nextTick();
    basicFormRef.value?.clearValidate?.('responsibleUserId');
  }
  filterKey = draft.filters.length;
  originalAuthType.value = draft.authType;
  originalApiKeyName.value = draft.apiKeyName;
  originalHeaders.value = draft.headers.map(header => header.key);
  if (task.successCondition) {
    try {
      const condition = JSON.parse(task.successCondition);
      draft.successMode = 'BUSINESS';
      draft.successField = condition.field || '';
      draft.successOperator = condition.operator || '';
      draft.successValue = condition.value || '';
    } catch {
      draft.successMode = 'HTTP';
    }
  }
}

/** 下一步只校验当前步骤；增量时间仅在选择“增量”时必填。 */
async function validateCurrentStep() {
  /** 将表单校验失败转换为布尔值，供当前步骤合并判断。 */
  const validateForm = formRef => formRef.value ?
      formRef.value.validate().catch(() => false) :
      Promise.resolve(false);
  let results = [];
  if (step.value === 0) {
    results = await Promise.all([
      validateForm(basicFormRef),
      draft.dataMode === 'INCREMENTAL' ? validateForm(incrementalFormRef) : Promise.resolve(true)
    ]);
  } else if (step.value === 1) {
    return validateScopeStep();
  } else if (step.value === 2) {
    results = await Promise.all([validateForm(apiFormRef), validateForm(authFormRef), draft.successMode ===
    'BUSINESS' ? validateForm(successFormRef) : Promise.resolve(true)
    ]);
    results.push(validateHeaders());
  } else if (step.value === 3 && draft.triggerType === 'SCHEDULED') {
    results = [await validateForm(scheduleFormRef)];
  }
  if (step.value === 3 && autoRetryEnabled.value) {
    if (!Number.isInteger(draft.failRetryTimes) || draft.failRetryTimes < 1 ||
        !Number.isInteger(draft.failRetryInterval) || draft.failRetryInterval < 1) {
      ElMessage.error('失败重试次数和重试间隔必须大于0');
      return false;
    }
    if (!retryOutOptions.value.some(item => item.value === draft.failRetryOutType)) {
      ElMessage.error('请选择超出次数处理方式');
      return false;
    }
  }
  if (results.every(Boolean)) return true;
  await nextTick();
  wizardBodyRef.value?.querySelector('.el-form-item.is-error, .filter-error, .section-error')
      ?.scrollIntoView({block: 'nearest'});
  return false;
}

/** 底部“下一步”和步骤条向前切换共用校验，防止跳过必填字段。 */
async function handleNext() {
  if (initializing.value || stepValidating.value || step.value >= stepItems.length - 1) return;
  stepValidating.value = true;
  const currentStep = step.value;
  try {
    if (await validateCurrentStep() && step.value === currentStep) step.value += 1;
  } finally {
    stepValidating.value = false;
  }
}

/** 提交四步任务配置；修改时沿用任务 ID，并保存所选负责人。 */
async function handleSubmit() {
  if (initializing.value || saving.value || step.value !== 3 || !(await validateCurrentStep())) return;
  const payload = {
    ...(isEdit.value ? {id: taskId.value} : {}),
    name: draft.name.trim(),
    code: draft.code.trim(),
    modelId: draft.modelId,
    modelVersionId: draft.modelVersionId,
    targetSystemId: draft.targetSystemId,
    responsibleUserId: draft.responsibleUserId,
    // 新增时由后端取当前登录用户；修改时保留原任务创建人。
    ...(isEdit.value ? {creatorId: draft.creatorId} : {}),
    status: draft.status,
    remark: draft.remark.trim(),
    type: 'API',
    dataMode: draft.dataMode,
    incrementalStartTime: draft.dataMode === 'INCREMENTAL' ? draft.incrementalStartTime : null,
    dataScopeType: draft.dataScopeType,
    filters: filterPayload.value,
    fieldMappings: fieldMappingPayload.value,
    ...apiConfigPayload.value,
    triggerType: draft.triggerType,
    // 重试配置随任务保存，超限处理只提交字典键值。
    failRetryFlag: autoRetryEnabled.value ? '1' : '0',
    failRetryTimes: autoRetryEnabled.value ? draft.failRetryTimes : null,
    failRetryInterval: autoRetryEnabled.value ? draft.failRetryInterval : null,
    failRetryOutType: autoRetryEnabled.value ? draft.failRetryOutType : null,
    cronExpression: draft.triggerType === 'SCHEDULED' ? draft.cronExpression.trim() : null
  };
  saving.value = true;
  try {
    if (isEdit.value) await updateTASK(payload);
    else await addTASK(payload);
    ElMessage.success(isEdit.value ? '修改成功' : '分发任务保存成功');
    setRefreshNeeded();
    goBack();
  } finally {
    saving.value = false;
  }
}

/** 页面式新增和修改均由路由进入，保存或返回时回到分发任务列表。 */
function goBack() {
  router.push('/dist/taskDistribute');
}

onMounted(initialize);

/** 固定请求头逐条编辑；系统接管 Content-Type 与认证头，避免用户覆盖。 */
function addHeader() {
  draft.headers.push({id: ++headerKey, key: '', value: ''});
}

/** 删除指定请求头及该行校验提示。 */
function removeHeader(index) {
  const [removed] = draft.headers.splice(index, 1);
  if (removed) delete headerErrors[removed.id];
  headerError.value = '';
}

/** 只清除正在修改的字段提示。 */
function clearHeaderError(header, field) {
  if (headerErrors[header.id]) delete headerErrors[header.id][field];
  headerError.value = '';
}

/** 重新校验请求头前清空所有历史提示。 */
function clearHeaderErrors() {
  Object.keys(headerErrors).forEach(key => delete headerErrors[key]);
  headerError.value = '';
}

/** 自定义 Header 必须完整、名称合法且不重复；配置长度受生成器字段上限约束。 */
function validateHeaders() {
  clearHeaderErrors();
  const names = new Set();
  const reserved = new Set(['content-type', 'authorization']);
  if (draft.authType === 'API_KEY') reserved.add(draft.apiKeyName.trim().toLowerCase());
  draft.headers.forEach(header => {
    const errors = {};
    const name = header.key.trim();
    const value = header.value.trim();
    if (!name) errors.key = '请输入 Header 名称';
    else if (!/^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/.test(name)) errors.key = '请求头名称格式不正确';
    else if (reserved.has(name.toLowerCase())) errors.key = '该请求头由系统维护';
    else if (names.has(name.toLowerCase())) errors.key = '请求头名称不能重复';
    if (!value) errors.value = '请输入 Header 值';
    else if (/[\r\n]/.test(value)) errors.value = 'Header 值不能包含换行';
    if (Object.keys(errors).length) headerErrors[header.id] = errors;
    if (name) names.add(name.toLowerCase());
  });
  if (apiConfigPayload.value.headerConfig?.length > 512) headerError.value = '请求头配置不能超过512个字符';
  if (apiConfigPayload.value.authConfig?.length > 512) headerError.value = '认证配置不能超过512个字符';
  return !Object.keys(headerErrors).length && !headerError.value;
}

/** 条件草稿仅保存在当前配置页；空值与页面勾选状态不会直接写入后端。 */
function addFilter() {
  draft.filters.push({key: ++filterKey, selected: false, modelColumnId: null, operator: '', value: ''});
}

function handleFilterSelectionChange(rows) {
  selectedFilters.value = rows;
}

/** 批量移除已勾选的筛选条件。 */
function removeSelectedFilters() {
  const selectedKeys = new Set(selectedFilters.value.map(item => item.key));
  filterTableRef.value?.elTableRef?.clearSelection();
  selectedFilters.value = [];
  draft.filters = draft.filters.filter(item => !selectedKeys.has(item.key));
  clearStepTwoErrors();
}

/** 移除单条筛选条件，并清理对应的提示。 */
function removeFilter(row, index) {
  if (selectedFilters.value.some(item => item.key === row.key)) {
    filterTableRef.value?.elTableRef?.toggleRowSelection(row, false);
  }
  draft.filters.splice(index, 1);
  clearFilterError(row);
}

/** 筛选条件修改后清除该行校验提示。 */
function clearFilterError(row) {
  delete filterErrors[row.key];
  scopeError.value = '';
}

/** 进入第二步校验前清空筛选和映射提示。 */
function clearStepTwoErrors() {
  Object.keys(filterErrors).forEach(key => delete filterErrors[key]);
  Object.keys(mappingErrors).forEach(key => delete mappingErrors[key]);
  scopeError.value = '';
  mappingError.value = '';
}

/** 当前行修改后清除映射提示；取消勾选时保留草稿，但不会进入保存报文。 */
function clearMappingError(row) {
  delete mappingErrors[row.id];
  mappingError.value = '';
}

/** 根据模型字段类型选择比较控件及可用运算符，避免文本字段出现大小比较。 */
function getFilterFieldKind(row) {
  const type = String(modelFields.value.find(field => field.id === row.modelColumnId)?.dataType || '').toLowerCase();
  if (/bool|布尔/.test(type)) return 'boolean';
  if (/datetime|timestamp|日期时间|时间/.test(type)) return 'datetime';
  if (/date|日期/.test(type)) return 'date';
  if (/int|decimal|number|numeric|float|double|数字|整数|小数/.test(type)) return 'number';
  return 'text';
}

/** 根据字段类型组合当前筛选行可使用的比较运算符。 */
function getFilterOperators(row) {
  const kind = getFilterFieldKind(row);
  return [...commonOperators, ...(kind === 'text' ? textOperators : []), ...(['number', 'date', 'datetime'].includes(
      kind) ? orderedOperators : []), ...nullOperators];
}

/** 判断运算符是否不需要比较值。 */
function isNullOperator(operator) {
  return operator === 'IS_NULL' || operator === 'IS_NOT_NULL';
}

/** 模型字段变化时重置原运算符与比较值。 */
function handleFilterFieldChange(row) {
  row.operator = '';
  row.value = '';
  clearFilterError(row);
}

/** 运算符变化时清空原比较值和该行提示。 */
function handleFilterOperatorChange(row) {
  row.value = '';
  clearFilterError(row);
}

/** 第二步校验所有条件行和字段映射，失败时留在当前步骤并定位提示。 */
async function validateScopeStep() {
  clearStepTwoErrors();
  if (draft.dataScopeType === 'CONDITION') {
    if (!draft.filters.length) scopeError.value = '请至少添加一条筛选条件';
    draft.filters.forEach(row => {
      const errors = {};
      const field = modelFields.value.find(item => item.id === row.modelColumnId);
      if (!field) errors.modelColumnId = '请选择模型字段';
      if (!row.operator) errors.operator = '请选择表达式';
      else if (field && !getFilterOperators(row).some(item => item.value === row.operator)) errors.operator =
          '表达式不适用于该字段';
      if (row.operator && !isNullOperator(row.operator)) {
        const value = String(row.value ?? '').trim();
        if (!value) errors.value = '请输入比较值';
        else if (value.length > 512) errors.value = '比较值不能超过512个字符';
        else if (field && getFilterFieldKind(row) === 'number' && !Number.isFinite(Number(value))) errors.value =
            '请输入有效数字';
      }
      if (Object.keys(errors).length) filterErrors[row.key] = errors;
    });
  }
  const mappings = selectedMappings.value;
  if (!mappings.length || !draft.mappings.filter(item => item.required).every(item => item.selected)) {
    mappingError.value = '请至少选择一个分发字段，并保留模型必填字段';
  }
  const targetNames = new Set();
  const sourceIds = new Set();
  mappings.forEach(item => {
    const name = String(item.targetField || '').trim();
    if (!Number.isSafeInteger(item.id) || item.id === 0) mappingErrors[item.id] = '主数据模型字段无效';
    else if (sourceIds.has(item.id)) mappingErrors[item.id] = '主数据模型字段不能重复';
    else if (!name) mappingErrors[item.id] = '请填写下游 API 字段';
    else if (name.length > 512) mappingErrors[item.id] = '下游 API 字段不能超过512个字符';
    else if (/[.[\]]/.test(name)) mappingErrors[item.id] = '请填写平铺 JSON 属性名';
    else if (targetNames.has(name)) mappingErrors[item.id] = '下游 API 字段不能重复';
    sourceIds.add(item.id);
    if (name) targetNames.add(name);
  });
  if (!scopeError.value && !mappingError.value && !Object.keys(filterErrors).length && !Object.keys(mappingErrors)
      .length) return true;
  await nextTick();
  wizardBodyRef.value?.querySelector('.filter-error, .section-error')?.scrollIntoView({block: 'nearest'});
  return false;
}

/** 返回筛选条件草稿，保留行引用以支持表格内编辑。 */
function loadFilterRows() {
  return Promise.resolve({data: [...draft.filters]});
}

/** QtTable 的本地数据入口，直接展示当前模型字段映射草稿。 */
function loadMappingRows() {
  const rows = [...draft.mappings].sort((left, right) => Number(right.required) - Number(left.required));
  return Promise.resolve({data: rows});
}

/** 打开项目 Cron 生成器时复制当前表达式，取消编辑不改变向导草稿。 */
function openCronGenerator() {
  cronExpressionDraft.value = draft.cronExpression || '';
  cronOpen.value = true;
}

/** 生成器确认后回填调度周期，并清除原有必填提示。 */
function fillCron(expression) {
  draft.cronExpression = expression;
  cronOpen.value = false;
  nextTick(() => scheduleFormRef.value?.clearValidate('cronExpression'));
}

/** 用户切换模型时加载其最新发布结构，并清空引用旧字段的筛选和映射草稿。 */
watch(() => draft.modelId, async modelId => {
  if (initializing.value) return;
  // 切换模型后原筛选字段失效，重新保留一条空条件供配置。
  draft.filters = [{key: ++filterKey, selected: false, modelColumnId: null, operator: '', value: ''}];
  clearStepTwoErrors();
  if (modelId == null) {
    ++modelLoadSequence;
    draft.modelVersionId = null;
    modelFields.value = [];
  } else if (Number(modelId) === legacyModelId) {
    ++modelLoadSequence;
    draft.modelVersionId = -1;
    modelFields.value = legacyModelFields.map(field => ({...field}));
  } else {
    await loadPublishedModelStructure(modelId);
  }
  draft.mappings = modelFields.value.map(field => ({...field, selected: field.required}));
  if (step.value === 1) nextTick(() => mappingTableRef.value?.getList());
});
watch(step, value => {
  if (value !== 1) selectedFilters.value = [];
  if (value === 1) nextTick(() => mappingTableRef.value?.getList());
});
// 增删或重置条件后刷新本地表格；字段编辑保留原行引用，无需重新加载。
watch(() => draft.filters.map(row => row.key), () => {
  filterTableRef.value?.getList();
}, {flush: 'post'});
watch(() => draft.dataScopeType, () => {
  selectedFilters.value = [];
  clearStepTwoErrors();
});

const selectedProject = computed(() => modelTree.value.find(item => String(item.id) === String(draft.projectId)));
const layerOptions = computed(() => selectedProject.value?.children || []);
const filteredModelOptions = computed(() => modelOptions.value.filter(item =>
  String(item.projectId) === String(draft.projectId) && String(item.layerId) === String(draft.layerId)));
const selectedModelOption = computed(() => modelOptions.value.find(item => String(item.value) === String(draft.modelId)));

function handleProjectChange() {
  draft.layerId = null;
  handleLayerChange();
}

function handleLayerChange() {
  draft.modelId = null;
}
</script>

<style scoped>
.section-heading {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.dist-task-create-page {
  display: flex;
  flex-direction: column;
}

.incremental-start-field :deep(.el-form-item__label) {
  white-space: nowrap;
}

  .column-form .number-input :deep(.el-input__wrapper) {
    padding-left: 42px;
  }
.main {
  height: 100%;
  background: var(--qt-surface-background);
  padding: 0 25px 85px;
  overflow: auto;
}

.pagecont-top {
  position: relative;
  min-height: var(--qt-model-workspace-min-height);
  flex: 1;
  margin-bottom: 0;
}

.wizard-steps {
  width: 100%;
  padding: 18px 8%;
  box-sizing: border-box;
  background: var(--qt-surface-background);
}

.wizard-steps :deep(.steps-inner) {
  padding: 0;
}

.wizard-steps :deep(.zl-step) {
  height: auto;
  margin: 0;
  align-items: center;
}

.button-style {
  position: absolute;
  right: 30px;
  bottom: var(--qt-space-5);
}

.description-input {
  position: relative;
  width: 100%;
}

.description-input :deep(.el-textarea__inner) {
  padding-bottom: var(--qt-space-6);
}

.condition-table {
  margin: 0 var(--qt-space-3) var(--qt-space-5) 0;
}

.condition-table :deep(.el-select),
.condition-table :deep(.el-input) {
  width: 100%;
}

.condition-table :deep(.el-date-editor) {
  width: 100%;
}

.filter-error,
.section-error {
  color: var(--qt-status-danger);
  font-size: var(--qt-font-size-small);
  line-height: 18px;
}

.section-error {
  margin: -12px var(--qt-space-3) var(--qt-space-4) var(--qt-space-5);
}

.header-editor {
  display: grid;
  gap: 8px;
  width: 100%;
  max-width: 760px;
  min-width: 0;
}

.header-row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 3fr) auto;
  align-items: start;
  gap: 8px;
}

.header-field {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.header-remove {
  color: var(--qt-text-secondary);
}

.header-remove:hover,
.header-remove:focus-visible {
  color: var(--qt-status-danger);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--qt-text-secondary);
  font-size: var(--qt-font-size-small);
}

@media (max-width: 700px) {
  .header-row {
    grid-template-columns: minmax(0, 1fr) auto;
    row-gap: 4px;
  }

  .header-field {
    grid-column: 1;
  }

  .header-remove {
    grid-column: 2;
    grid-row: 1;
  }
}

.preview-wrap {
  margin: 0 var(--qt-space-3) 0 var(--qt-space-5);
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

@media (max-width: 992px) {
  .main {
    padding-left: 0;
    padding-right: 0;
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
