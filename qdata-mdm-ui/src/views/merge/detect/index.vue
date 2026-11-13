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
  重复检测列表与详情，负责检测任务入口、来源快照、匹配规则和执行记录展示。
  @prop detailPage 是否作为路由详情页展示。
  @author zyy
-->
<template>
  <div ref="app-container" class="app-container" :class="{ 'detect-detail-page': detailPage }">
    <GuideTip v-if="!detailPage" tip-id="merge/detect.list" />
    <el-container>
      <qt-tree v-if="!detailPage"
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
      <el-main v-loading="detailPage && detailLoading" class="main-content merge-list-main">
        <qt-wrap v-if="!detailPage" :columns="tableStore.columns" :tableRef="tableRef">
          <template #search>
            <qt-search-bar v-bind="searchStore" :params="tableStore.params" :tableRef="tableRef" @reset="clearModelTreeSelection" />
          </template>
          <template #actions-data>
            <el-button
              v-hasPermi="['merge:detect:execute']"
              type="primary"
              plain
              @click="openCreate"
            ><i class="iconfont-mini icon-xinzeng mr5"></i>新增</el-button>
            <el-button
              v-hasPermi="['merge:detect:execute']"
              type="danger"
              plain
              icon="Delete"
              :disabled="!listSelection.rows.length"
              @click="deleteTasks(listSelection.rows)"
            >批量删除</el-button>
          </template>
          <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)">
            <template #taskInfoTitle="{ row }">
              <el-link
                v-if="getDisplayTitle(row.taskName) && $auth.hasPermi('merge:detect:query')"
                type="primary" :underline="false"
                role="link" tabindex="0"
                class="qt-detail-link text-ellipsis"
                :title="String(row.taskName)"
                @click.stop="openDetail(row)"
                @keydown.enter.stop.prevent="openDetail(row)"
                @keydown.space.stop.prevent="openDetail(row)"
              >{{ row.taskName }}</el-link>
              <span v-else class="qt-detail-link text-ellipsis" :title="getDisplayTitle(row.taskName)">{{ getDisplayTitle(row.taskName) ?? '-' }}</span>
            </template>
            <template #location="{ row }">
              <qt-model-location :model="getModelLocation(row)" />
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(getTaskModel(row))" alt="" class="info-icon"/>
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ formatModelTitle(row) }}</span>
                  <dict-tag
                    v-if="getTaskModel(row).type"
                    :options="MODEL_TYPE_OPTIONS"
                    :value="getTaskModel(row).type"
                    size="small"
                    type="primary"
                  />
                  <dict-tag
                    v-if="getTaskModel(row).versionNo"
                    class="ml4"
                    :class="{ pointer: row.modelId != null }"
                    :options="getVersionTagOptions(getTaskModel(row).versionNo)"
                    :value="MODEL_STATUS.PUBLISHED"
                    size="small"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ getTaskModel(row).description || '-' }}</div>
              </div>
            </template>
            <template #scopeType="{ row }">
              <qt-tag-group
                :items="[{ value: row.scopeType, label: getScopeText(row.scopeType), elTagType: 'primary' }]"
                type="none"
              />
            </template>
            <template #detectConfig="{ row }">
              <qt-tag-group
                class="detect-config-tags"
                :items="[
                  { value: 'sourceCount', label: `范围数据：${formatCountText(row.sourceCount, '条')}`, elTagType: 'primary' },
                  { value: 'ruleCount', label: `匹配规则：${formatCountText(row.ruleCount, '条')}`, elTagType: 'primary' },
                ]"
                type="none"
              />
            </template>
            <template #lastExecuteInfo="{ row }">
              <StatusTag size="small" :status="String(row.status) === '0' ? 'idle' : row.status" />
              <div class="detect-last-execute-info">
                <div class="detect-last-execute-info__row">
                  <span></span>
                  <span class="detect-last-execute-info__value">{{ row.lastExecuteTime ? timeAgo(row.lastExecuteTime) : '-' }}</span>
                  <span class="ml10"></span>
                  <span class="detect-last-execute-info__value">{{ formatDuration(row.lastExecuteDurationMillis) }}</span>
                </div>
              </div>
            </template>
            <template #runControl="{ row }">
              <div class="flex-column flex-column-gap">
                <div class="flex-center">
                  <span class="mr5">任务状态:</span>
                  <el-switch
                    :model-value="row.enableStatus === 'ENABLED' ? '1' : '0'"
                    active-value="1"
                    inactive-value="0"
                    :loading="controlBusyIds.has(row.id)"
                    :disabled="controlBusyIds.has(row.id) || String(row.status) === '1' || !proxy.$auth.hasPermi('merge:detect:execute')"
                    aria-label="任务状态"
                    @change="changeTaskControl(row, $event, false)"
                  />
                </div>
                <div class="flex-center">
                  <span class="mr5">调度状态:</span>
                  <el-switch
                    :model-value="row.schedulerStatus || '0'"
                    active-value="1"
                    inactive-value="0"
                    :loading="controlBusyIds.has(row.id)"
                    :disabled="controlBusyIds.has(row.id) || String(row.status) === '1' || row.enableStatus !== 'ENABLED' || row.triggerType !== 'SCHEDULED' || !proxy.$auth.hasPermi('merge:detect:execute')"
                    aria-label="调度状态"
                    @change="changeTaskControl(row, $event, true)"
                  />
                </div>
              </div>
            </template>
            <template #schedule="{ row }">
              <div class="flex-column flex-column-gap">
                <dict-tag :options="DETECT_TRIGGER_OPTIONS" :value="row.triggerType" size="small" />
                <span class="detect-schedule-summary"><el-icon class="mr5"><Clock /></el-icon><span class="detect-schedule-summary__text text-ellipsis" :title="formatSchedule(row)">{{ formatSchedule(row) }}</span></span>
              </div>
            </template>
            <template #responsiblePerson="{ row }">
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="getResponsibleName(row.responsibleUserId)">{{ getResponsibleName(row.responsibleUserId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.responsibleUserId)" class="desc-text">{{ getResponsiblePhone(row.responsibleUserId) }}</span>
              </div>
            </template>
            <template #creator="{ row }">
              <div class="flex-column overflow-hidden">
                <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)">{{ row.createBy || getResponsibleName(row.creatorId) || '-' }}</span>
                <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text">{{ getResponsiblePhone(row.creatorId) }}</span>
              </div>
            </template>
            <template #createTime="{ row }">
              {{ row.createTime ? proxy.parseTime(row.createTime, '{y}-{m}-{d} {h}:{i}') : '-' }}
            </template>
            <template #handle="{ row }">
              <el-button
                v-hasPermi="['merge:detect:query']"
                link
                type="primary"
                icon="View"
                @click="openDetail(row)"
              >详情</el-button>
              <el-button
                v-hasPermi="['merge:detect:execute']"
                link
                type="primary"
                icon="Edit"
                :disabled="String(row.status) === '1'"
                @click="openEdit(row)"
              >修改</el-button>
              <el-popover
                placement="bottom"
                :width="130"
                trigger="click"
                popper-class="table-action-popover handle-popover"
              >
                <template #reference>
                  <el-button link type="primary" icon="ArrowDown">更多</el-button>
                </template>
                <div class="butgdlist task-action-list">
                  <el-button
                    v-hasPermi="['merge:detect:execute']"
                    link
                    type="primary"
                    icon="VideoPlay"
                    :disabled="String(row.status) === '1' || row.enableStatus !== 'ENABLED'"
                    @click="runTask(row)"
                  >执行一次</el-button>
                  <el-button
                    v-hasPermi="['merge:detect:query']"
                    link
                    type="primary"
                    icon="Stopwatch"
                    @click="openInstances(row)"
                  >执行实例</el-button>
                  <el-button
                    v-hasPermi="['merge:detect:execute']"
                    link
                    type="danger"
                    icon="Delete"
                    :disabled="String(row.status) === '1'"
                    @click="deleteTask(row)"
                  >删除任务</el-button>
                </div>
              </el-popover>
            </template>
          </qt-table>
        </qt-wrap>

        <div v-else-if="detailTask" v-loading="detailLoading" class="detect-detail">
          <DetailInfo
            :data="detailInfoData"
            :header="{
              className: 'clearfixs',
              idKey: 'headerId',
              nameKey: 'headerName',
              statusKey: 'enableStatus',
              statusOptions: enableStatusOptions,
              routerBack: false,
            }"
            mode="free"
            :items="detailItems"
            @back="closeDetail"
          >
            <template #model-info>
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon(getTaskModel(detailTask))" alt="" class="info-icon" />
                  <span class="info-title text-ellipsis" :title="formatModelTitle(detailTask)">
                    {{ formatModelTitle(detailTask) }}
                  </span>
                  <dict-tag :options="MODEL_TYPE_OPTIONS" :value="getTaskModel(detailTask).type" size="small" type="primary" />
                  <dict-tag
                    v-if="getTaskModel(detailTask).versionNo"
                    class="ml4"
                    :options="getVersionTagOptions(getTaskModel(detailTask).versionNo)"
                    :value="MODEL_STATUS.PUBLISHED"
                    size="small"
                  />
                </div>
              </div>
            </template>
            <template #detect-scope>
              <qt-tag-group
                :items="[{ value: detailTask.scopeType, label: getScopeText(detailTask.scopeType), elTagType: 'primary' }]"
                type="none"
              />
            </template>
            <template #detect-config>
              <el-space>
                <el-tag size="small">范围数据 {{ formatCountText(detailTask.sourceCount, '条') }}</el-tag>
                <el-tag size="small">匹配规则 {{ formatCountText(detailTask.ruleCount, '条') }}</el-tag>
              </el-space>
            </template>
            <template #detect-schedule>
              <template v-if="detailTask.triggerType === 'SCHEDULED'">
                <el-icon><Clock /></el-icon>
                {{ cronToZh(detailTask.cronExpression) }}
              </template>
              <el-tag v-else type="info">手动执行</el-tag>
            </template>
          </DetailInfo>
          <div class="pagecont-bottom">
            <el-tabs v-model="detailTab" class="demo-tabs" @tab-change="handleDetailTabChange">
            <qt-tab-pane label="当前范围来源数据" name="source">
              <div class="detail-tab-toolbar">
                <GuideTipPanel type="plain" title="查看当前重复检测范围内的来源记录，核对各来源系统及关键业务字段，了解参与检测的数据内容，确认检测范围是否符合预期。" class="detail-tab-tip" />
                <el-tooltip content="刷新当前范围来源数据" placement="top">
                  <el-button
                    circle
                    :loading="detailLoading"
                    aria-label="刷新当前范围来源数据"
                    @click="refreshDetailData"
                  >
                    <i class="iconfont icon-a-shuaxinxianxing"></i>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="当前范围来源数据显隐列" placement="top">
                  <el-dropdown trigger="click" :hide-on-click="false" popper-class="columns-popper">
                    <el-button circle icon="Menu" aria-label="当前范围来源数据显隐列" />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-for="column in sourceTableColumns" :key="column.key">
                          <el-checkbox
                            :model-value="column.visible"
                            :label="column.label"
                            @change="setSourceColumnVisible(column.key, $event)"
                          />
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </el-tooltip>
              </div>
              <qt-table
                ref="sourceTableRef"
                :columns="sourceQtColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                :config="detailQtConfig"
                :func="loadSourceTableRows"
                :events="sourceQtEvents"
              >
                <template #sourceSystem="{ row }">
                  <div class="qt-table-info">
                    <div class="qt-table-info__main">
                      <img :src="getSourceIcon(row.sourceSystem)" alt="" class="info-icon" />
                      <span class="info-title" :title="row.sourceSystemTitle">{{ row.sourceSystemTitle }}</span>
                    </div>
                    <div class="info-desc" :title="row.sourceSystem.description">{{ row.sourceSystem.description || '-' }}</div>
                  </div>
                </template>
                <template #sourceValue="{ row, column_data: { sourceField } }">
                  <div v-if="sourceField.subCodes" class="detect-source-secondary-cell">
                    <div>{{ displayText(getFieldValue(row, sourceField.codes)) }}</div>
                    <div>{{ displayText(getFieldValue(row, sourceField.subCodes)) }}</div>
                  </div>
                  <template v-else>{{ displayText(getFieldValue(row, sourceField.codes)) }}</template>
                </template>
                <template #receiveTime="{ row }">{{ formatTime(row.receiveTime) }}</template>
              </qt-table>
              <pagination
                v-show="sourceRows.length > 0"
                :total="sourceRows.length"
                v-model:page="sourcePage.pageNum"
                v-model:limit="sourcePage.pageSize"
                :auto-scroll="false"
              />
            </qt-tab-pane>
            <qt-tab-pane label="匹配规则" name="rules">
              <div class="detail-tab-toolbar">
                <GuideTipPanel type="plain" title="查看本次重复检测使用的匹配规则及具体配置，核对参与匹配的字段和判断条件，了解候选对象组的匹配依据与检测口径。" class="detail-tab-tip" />
                <el-tooltip content="刷新匹配规则" placement="top">
                  <el-button
                    circle
                    :loading="detailLoading"
                    aria-label="刷新匹配规则"
                    @click="refreshDetailData"
                  >
                    <i class="iconfont icon-a-shuaxinxianxing"></i>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="匹配规则显隐列" placement="top">
                  <el-dropdown trigger="click" :hide-on-click="false" popper-class="columns-popper">
                    <el-button circle icon="Menu" aria-label="匹配规则显隐列" />
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item v-for="column in ruleTableColumns" :key="column.key">
                          <el-checkbox v-model="column.visible" :label="column.label" />
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </el-tooltip>
              </div>
              <qt-table
                ref="ruleTableRef"
                :columns="ruleQtColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                :func="getRuleTableData"
                :config="ruleQtTableConfig"
              >
                <template #rule-id="{ row }">{{ row.id }}</template>
                <template #rule-name="{ row }">{{ displayText(row.ruleName) }}</template>
                <template #rule-type="{ row }">
                  <el-tag type="info" effect="light" size="small">{{ ruleTypeLabel(row.ruleType) }}</el-tag>
                </template>
                <template #rule-config="{ row }">
                  <div class="detect-rule-logic">
                    <div v-for="(line, index) in ruleLogicLines(row)" :key="line.key" :class="{ 'detect-rule-logic__secondary': index > 0 }">{{ line.text }}</div>
                  </div>
                </template>
                <template #rule-score="{ row }">{{ displayText(row.ruleScore) }}</template>
                <template #rule-available="{ row }">{{ ruleAvailabilityText(row) }}</template>
                <template #rule-status="{ row }">
                  <el-tag
                    v-if="row.validFlag !== undefined && row.validFlag !== null"
                    :type="isRuleEnabled(row) ? 'success' : 'info'"
                    effect="light"
                    size="small"
                  >{{ isRuleEnabled(row) ? '启用' : '禁用' }}</el-tag>
                  <span v-else>-</span>
                </template>
                <template #rule-creator="{ row }">
                  <div class="flex-column overflow-hidden">
                    <span class="text-ellipsis" :title="row.createBy || getResponsibleName(row.creatorId)">{{ row.createBy || getResponsibleName(row.creatorId) || '-' }}</span>
                    <span v-if="getResponsiblePhone(row.creatorId)" class="desc-text">{{ getResponsiblePhone(row.creatorId) }}</span>
                  </div>
                </template>
                <template #rule-create-time="{ row }">{{ formatTime(row.createTime) }}</template>
              </qt-table>
              <pagination
                v-show="ruleRows.length > 0"
                :total="ruleRows.length"
                v-model:page="rulePage.pageNum"
                v-model:limit="rulePage.pageSize"
                :auto-scroll="false"
                @pagination="refreshRuleTable"
              />
            </qt-tab-pane>
            <qt-tab-pane label="执行记录" name="executions">
              <DetectResultDialog ref="resultDialogRef" @close="resultVisible = false" />
              <template v-if="!resultVisible">
                <div class="detail-tab-toolbar">
                  <GuideTipPanel type="plain" title="查看重复检测任务的历次执行记录及检测结果，结合执行时间核对任务运行情况，追溯每次检测对应的处理状态和结果。" class="detail-tab-tip" />
                  <el-tooltip content="刷新执行记录" placement="top">
                    <el-button
                      circle
                      :loading="instanceLoading"
                      aria-label="刷新执行记录"
                      @click="refreshInstances"
                    >
                      <i class="iconfont icon-a-shuaxinxianxing"></i>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="执行记录显隐列" placement="top">
                    <el-dropdown trigger="click" :hide-on-click="false" popper-class="columns-popper">
                      <el-button circle icon="Menu" aria-label="执行记录显隐列" />
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item v-for="column in instanceColumns" :key="column.key">
                            <el-checkbox v-model="column.visible" :label="column.label" />
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </el-tooltip>
                </div>
                <qt-table
                  ref="executionTableRef"
                  v-loading="instanceLoading"
                  :columns="executionQtColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                  :config="detailQtConfig"
                  :func="loadExecutionTableRows"
                  :events="executionQtEvents"
                >
                  <template #startTime="{ row }">{{ formatTime(row.startTime) }}</template>
                  <template #sourceCount="{ row }">{{ formatCountText(row.sourceCount, '条') }}</template>
                  <template #groupCount="{ row }">{{ formatCountText(row.groupCount, '组') }}</template>
                  <template #status="{ row }">
                    <el-tag :type="getOptionType(TASK_STATUS_OPTIONS, row.status)" size="small">{{ getOptionLabel(TASK_STATUS_OPTIONS, row.status) }}</el-tag>
                  </template>
                  <template #action="{ row }">
                    <el-button
                      link
                      type="primary"
                      icon="View"
                      @click="openResult(row)"
                    >查看结果</el-button>
                  </template>
                </qt-table>
                <pagination
                  v-show="instanceQuery.total > 0"
                  :total="instanceQuery.total"
                  v-model:page="instanceQuery.pageNum"
                  v-model:limit="instanceQuery.pageSize"
                  :auto-scroll="false"
                  @pagination="loadInstances"
                />
              </template>
            </qt-tab-pane>
            </el-tabs>
          </div>
        </div>
      </el-main>
    </el-container>

    <DetectTaskDialog
      v-model:visible="createVisible"
      :title="createTitle"
      :row="createRow"
      @confirm="handleCreateConfirm"
    />
    <DetectInstancesDialog v-if="!detailPage" v-model="instancesVisible" :task="instancesTask" />
  </div>
</template>

<script setup name="Detect">
import { getDisplayTitle } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import QtTree from '@/components/QtTree/index.vue'
import QtModelLocation from '@/views/merge/components/QtModelLocation/index.vue'
import { computed, getCurrentInstance, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSourceDataModelColumns } from '@/api/ingest/LOG'
import { listIngestSource } from '@/api/ingest/source/source'
import { changeMergeDetectTaskStatus, changeMergeDetectScheduleStatus, deleteMergeDetectTask, executeMergeDetectTask, listMergeInstance, listMergeTask } from '@/api/merge/detect'

import { useUserDirectory } from '@/store/system/userDirectory'
import { cronToZh } from '@/utils/cronUtils'
import { getModelIcon } from '@/utils/imageUrl'
import { MODEL_TYPE_OPTIONS } from '@/utils/model/modelType'
import { MODEL_STATUS, MODEL_STATUS_OPTIONS } from '@/utils/model/modelStatus'
import { timeAgo } from '@/utils/time'

import StatusTag from '@/components/StatusTag/index.vue'
import DetectTaskDialog from '@/views/merge/detect/components/detectTaskDialog.vue'
import DetectResultDialog from '@/views/merge/detect/components/detectResultDialog.vue'
import DetectInstancesDialog from '@/views/merge/detect/components/detectInstancesDialog.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'

import { getPublishedModelTree, listPublishedModels } from '@/api/model/base/base';

import { getProjectIcon } from '@/utils/imageUrl';

function formatCountText(value, unit) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} ${unit}`
}

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

function getOptionType(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.elTagType || 'info'
}

function parseJson(value, fallback) {
  if (value === null || value === undefined || value === '') return fallback
  if (typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function getScopeText(scopeType) {
  if (String(scopeType) === '1') return '全部待治理来源数据'
  if (String(scopeType) === '2') return '指定范围'
  return scopeType ? `范围${scopeType}` : '-'
}

const { merge_task_status: TASK_STATUS_OPTIONS } = useDict('merge_task_status')

let publishedModelsLoading

function loadPublishedModels() {
  if (!publishedModelsLoading) {
    // 与接入任务保持一致，只从成功发布模型接口构建可选项。
    publishedModelsLoading = listPublishedModels({ pageNum: 1, pageSize: -1 })
      .then((res) => (res.data?.rows || res.rows || [])
        .map((item) => ({ ...item, label: item.name, value: item.id })))
      .catch((error) => {
        publishedModelsLoading = undefined
        throw error
      })
  }
  return publishedModelsLoading
}

function getModelName(models, modelId) {
  const found = (models || []).find((item) => String(item.value) === String(modelId))
  return found?.label || (modelId ? `模型${modelId}` : '—')
}

function formatDuration(value) {
  if (value == null) return '-';
  if (value < 1000) return `${value} 毫秒`;
  return `${(value / 1000).toFixed(1)} 秒`;
}

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

const { getResponsibleName, getResponsiblePhone } = useUserDirectory()
const props = defineProps({ detailPage: { type: Boolean, default: false } })
const { proxy } = getCurrentInstance()
const { rule_match_type } = proxy.useDict('rule_match_type')
const route = useRoute()
const router = useRouter()
const tableRef = ref()
const ruleTableRef = ref()
const listSelection = reactive({ rows: [] })
const modelOptions = ref([])
const starting = ref(false)
const createVisible = ref(false)
const createTitle = ref('新增重复检测任务')
const createRow = ref({})
const instancesVisible = ref(false)
const instancesTask = ref(null)
const controlBusyIds = reactive(new Set())
const detailTask = ref(null)
const { merge_scheduler_status: enableStatusOptions } = useDict('merge_scheduler_status')
const { merge_detect_trigger: DETECT_TRIGGER_OPTIONS } = useDict('merge_detect_trigger')
const detailInfoData = computed(() => ({
  ...detailTask.value,
  headerId: detailTask.value?.id,
  headerName: detailTask.value?.taskNo,
}))
const detailItems = computed(() => {
  const task = detailTask.value || {}
  const responsibleName = getResponsibleName(task.responsibleUserId)
  const responsiblePhone = getResponsiblePhone(task.responsibleUserId)
  return [
    { label: '主数据模型', slot: 'model-info' },
    { label: '检测范围', slot: 'detect-scope' },
    { label: '检测配置', slot: 'detect-config' },
    { label: '描述', value: displayText(task.remark), span: 24, className: 'mt2 mb2' },
    { label: '调度周期', slot: 'detect-schedule' },
    { label: '责任人', value: [responsibleName || '-', responsiblePhone].filter(Boolean).join(' / ') },
    { label: '创建时间', value: formatDetailTime(task.createTime) },
  ]
})
const detailLoading = ref(false)
const detailTab = ref('source')
const latestInstance = ref(null)
let sourceDetailLoaded = false
let sourceDetailPromise = null
let instancesLoaded = false
let instancesPromise = null
const instanceLoading = ref(false)
const instanceRows = ref([])
const instanceColumns = reactive([
  { key: 'id', label: '编号', visible: true },
  { key: 'instanceNo', label: '执行编号', visible: true },
  { key: 'startTime', label: '执行时间', visible: true },
  { key: 'sourceCount', label: '实际检测数据', visible: true },
  { key: 'groupCount', label: '候选对象组', visible: true },
  { key: 'status', label: '执行状态', visible: true },
  { key: 'action', label: '操作', visible: true },
])
const instanceQuery = reactive({ taskId: undefined, pageNum: 1, pageSize: 10, total: 0, orderByColumn: 'start_time', isAsc: 'descending' })
const resultDialogRef = ref()
const resultVisible = ref(false)
const tableStore = reactive({
  func: queryTasks,
  params: { taskName: undefined, scopeType: undefined, modelId: undefined, status: undefined },
  events: { formatParams, formatData },
  config: {
    sort: true,
    table: {
      rowKey: 'id',
      stripe: true,
      defaultSort: { prop: 'create_time', order: 'descending' },
      onSelectionChange: (rows) => { listSelection.rows = rows || [] },
    },
    pagination: { layout: 'total, sizes, prev, pager, next, jumper', pageSizes: [10, 20, 50, 100] },
  },
  columns: [
    { type: 'selection', width: 48, minWidth: 48, selectable: canDeleteTask },
    { label: '编号', prop: 'id', sortable: true, width: 60, minWidth: 60, align: 'center' },
    { label: '任务信息', property: 'taskName', info: { custom: 'taskInfoTitle', desc: 'remark' }, minWidth: 200, align: 'left' },
    { label: '主数据模型', prop: 'modelId', slot: 'modelInfo', minWidth: 300, align: 'left', showOverflowTooltip: { effect: 'light' } },
    { label: '归属位置', prop: 'location', slot: 'location', minWidth: 190, align: 'left' },
    { label: '检测范围', prop: 'scopeType', slot: 'scopeType', minWidth: 200, align: 'left' },
    { label: '检测配置', prop: 'sourceCount', slot: 'detectConfig', minWidth: 300, align: 'left' },
    { label: '最近执行信息', prop: 'lastExecuteTime', slot: 'lastExecuteInfo', sortable: true, sortableKey: 'last_execute_time', minWidth: 255, align: 'left' },
    { label: '运行控制', prop: 'enableStatus', slot: 'runControl', width: 165, minWidth: 165, align: 'left' },
    { label: '执行方式/调度周期', prop: 'triggerType', slot: 'schedule', minWidth: 190, align: 'left' },
    { label: '责任人', prop: 'responsibleUserId', slot: 'responsiblePerson', width: 140, minWidth: 140, align: 'left' },
    { label: '创建人', prop: 'createBy', slot: 'creator', width: 160, minWidth: 160, align: 'left' },
    { label: '创建时间', prop: 'createTime', slot: 'createTime', sortable: true, sortableKey: 'create_time', width: 180, minWidth: 180, align: 'left' },
    { label: '操作', slot: 'handle', fixed: 'right', width: 220, minWidth: 220, align: 'left' },
  ],
})

const modelTreeRef = ref()
const modelTreeData = ref([])
const modelTreeLoading = ref(false)

function normalizeModelTree(nodes = [], context = {}) {
  return nodes.map(node => {
    const projectName = node.nodeType === 'project' ? node.name : context.projectName
    const projectIcon = node.nodeType === 'project' ? node.icon : context.projectIcon
    const layers = node.nodeType === 'layer' ? [...(context.layers || []), { name: node.name }] : (context.layers || [])
    return {
      ...node, projectName, projectIcon, layers,
      treeKey: `${node.nodeType}_${node.id}`,
      children: normalizeModelTree(node.children || [], { projectName, projectIcon, layers }),
    }
  })
}

function collectTreeModels(nodes = []) {
  const models = new Map()
  function visit(items) {
    for (const node of items) {
      if (node.nodeType === 'model' && node.modelId != null) models.set(String(node.modelId), node)
      visit(node.children || [])
    }
  }
  visit(nodes)
  return [...models.values()]
}

function handleModelTreeClick(node) {
  tableStore.params.modelId = node.nodeType === 'model' ? node.modelId : undefined
  const ids = collectTreeModels([node]).map(model => model.modelId)
  tableStore.params.modelIds = node.nodeType === 'model' ? undefined : (ids.length ? ids : [-1])
  tableRef.value?.resetQuery()
}

function handleModelSelect(id) {
  tableStore.params.modelIds = undefined
  const node = collectTreeModels(modelTreeData.value).find(model => String(model.modelId) === String(id))
  modelTreeRef.value?.setCurrentKey(node?.treeKey ?? null, false)
}

function resetTreeSelection() {
  tableStore.params.modelIds = undefined
  modelTreeRef.value?.resetTree()
}

const modelsById = computed(() => new Map(collectTreeModels(modelTreeData.value).map(node => [String(node.modelId), node])))
function getModelLocation(row) {
  return modelsById.value.get(String(row.modelId)) || {}
}

async function loadModelTree() {
  modelTreeLoading.value = true
  try {
    const { data } = await getPublishedModelTree()
    modelTreeData.value = normalizeModelTree(data || [])
    await nextTick()
    handleModelSelect(tableStore.params.modelId)
  } catch {
    // 接口统一提示错误，保留列表和下拉筛选可用。
    modelTreeData.value = []
  } finally {
    modelTreeLoading.value = false
  }
}

loadModelTree()

/** 清空模型树筛选条件。 */
function clearModelTreeSelection() {
  tableStore.params.modelId = undefined
  resetTreeSelection()
}

const searchStore = reactive({
  items: [
    { label: '任务名称', prop: 'taskName', component: { is: 'input', placeholder: '请输入任务名称' } },
    { label: '检测范围', prop: 'scopeType', component: { is: 'select', placeholder: '请选择检测范围', clearable: true, options: ['1', '2'].map(value => ({ value, label: getScopeText(value) })) } },
    { label: '最近执行状态', prop: 'status', component: { is: 'select', placeholder: '请选择最近执行状态', options: TASK_STATUS_OPTIONS } },
  ],
})

const sourcePage = reactive({ pageNum: 1, pageSize: 10 })
const sourceSortOrder = ref(null)
const sourceIdSort = ref(null)
const sourceColumns = ref([])
const sourceColumnVisibility = reactive({})
const ruleTableColumns = reactive([
  { key: 'id', label: '编号', visible: true },
  { key: 'ruleName', label: '规则名称', visible: true },
  { key: 'ruleType', label: '匹配方式', visible: true },
  { key: 'ruleConfig', label: '参与字段 / 匹配逻辑', visible: true },
  { key: 'ruleScore', label: '匹配分值', visible: true },
  { key: 'available', label: '当前是否可用', visible: true },
  { key: 'status', label: '规则状态', visible: true },
  { key: 'createBy', label: '创建人', visible: true },
  { key: 'createTime', label: '创建时间', visible: true },
])
const ruleQtColumnOptions = {
  id: { prop: 'id', slot: 'rule-id', width: 70, minWidth: 70, align: 'center', sortable: true },
  ruleName: { prop: 'ruleName', slot: 'rule-name', minWidth: 220, align: 'left', showOverflowTooltip: true },
  ruleType: { prop: 'ruleType', slot: 'rule-type', minWidth: 150, align: 'left', showOverflowTooltip: true },
  ruleConfig: { prop: 'ruleConfig', slot: 'rule-config', minWidth: 260, align: 'left', showOverflowTooltip: true },
  ruleScore: { prop: 'ruleScore', slot: 'rule-score', minWidth: 110, align: 'center' },
  available: { prop: 'available', slot: 'rule-available', minWidth: 130, align: 'center' },
  status: { prop: 'status', slot: 'rule-status', minWidth: 140, align: 'center' },
  createBy: { prop: 'createBy', slot: 'rule-creator', minWidth: 130, align: 'left', showOverflowTooltip: true },
  createTime: { prop: 'createTime', slot: 'rule-create-time', minWidth: 160, align: 'left', showOverflowTooltip: true },
}
const ruleQtColumns = computed(() => ruleTableColumns.map((column) => ({
  label: column.label,
  hide: !column.visible,
  ...ruleQtColumnOptions[column.key],
})))
const ruleQtTableConfig = { notPagination: true, initResquest: false, table: { stripe: true } }
const rulePage = reactive({ pageNum: 1, pageSize: 10 })
const sourceTableColumns = computed(() => [
  { key: 'id', label: '编号', visible: sourceColumnVisibility.id !== false },
  { key: 'system', label: '来源系统', visible: sourceColumnVisibility.system !== false },
  ...sourceFields.value.map((field) => ({
    key: field.key,
    label: field.label,
    visible: sourceColumnVisibility[field.key] !== false,
  })),
  { key: 'receiveTime', label: '接收时间', visible: sourceColumnVisibility.receiveTime !== false },
])
const sourceSystems = ref([])
const sourceRows = computed(() => {
  const rows = parseJson(latestInstance.value?.sourceDataSnapshot, [])
  if (!Array.isArray(rows)) return []
  const systemsById = new Map(sourceSystems.value.map((system) => [String(system.id), system]))
  const systemsByCode = new Map(sourceSystems.value.map((system) => [system.code, system]))
  return rows.map((row) => {
    const sourceSystem = systemsById.get(String(row.systemId)) || systemsByCode.get(row.systemCode) || {}
    const name = row.systemName || sourceSystem.name
    const code = row.systemCode || sourceSystem.code
    return {
      ...row,
      sourceSystem,
      sourceSystemTitle: name ? `${name}${code ? ` (${code})` : ''}` : code || '-',
      fields: parseFieldMap(row.modelData),
    }
  })
})
/** 按编号或接收时间对全部来源数据排序，再由分页截取当前页。 */
const sortedSourceRows = computed(() => {
  if (sourceIdSort.value) {
    const direction = sourceIdSort.value === 'ascending' ? 1 : -1
    return [...sourceRows.value].sort((left, right) => (Number(left.id) - Number(right.id)) * direction)
  }
  if (!sourceSortOrder.value) return sourceRows.value
  const direction = sourceSortOrder.value === 'ascending' ? 1 : -1
  return [...sourceRows.value].sort((left, right) => {
    const leftTime = new Date(left.receiveTime).getTime()
    const rightTime = new Date(right.receiveTime).getTime()
    if (Number.isNaN(leftTime) || Number.isNaN(rightTime)) return 0
    return (leftTime - rightTime) * direction
  })
})
const pagedSourceRows = computed(() => {
  const start = (sourcePage.pageNum - 1) * sourcePage.pageSize
  return sortedSourceRows.value.slice(start, start + sourcePage.pageSize)
})

const sourceFields = computed(() => {
  return sourceColumns.value
    .filter((column) => column && (column.columnCode || column.columnName))
    .map((column, index) => ({
      key: `field-${column.id ?? column.columnCode ?? index}`,
      label: column.columnName || column.columnCode,
      isDateTime: /^(DATETIME|TIMESTAMP)$/i.test(String(column.dataType || '')) || column.columnName === '最近访问时间' || normalizeSourceFieldCode(column.columnCode) === 'LASTVISITTIME',
      codes: [column.columnCode, column.id, column.columnName].filter((value) => value !== null && value !== undefined && value !== ''),
    }))
})

/**
 * 统一来源字段编码以兼容不同命名格式。
 * @param {*} code 待规范化的字段编码。
 * @returns {*} 当前业务计算结果。
 */
function normalizeSourceFieldCode(code) {
  return String(code || '').replace(/[^a-zA-Z0-9\u4e00-\u9fff]/g, '').toUpperCase()
}

/**
 * 依次按字段别名读取来源快照中的值。
 * @param {*} row 当前业务记录。
 * @param {*} codes 字段别名列表。
 * @returns {*} 当前业务计算结果。
 */
function getFieldValue(row, codes) {
  if (!row?.fields) return ''
  const keys = Object.keys(row.fields)
  for (const code of codes) {
    const key = keys.find((item) => normalizeSourceFieldCode(item) === normalizeSourceFieldCode(code))
    if (key && row.fields[key] !== null && row.fields[key] !== undefined && row.fields[key] !== '') return row.fields[key]
  }
  return ''
}

/**
 * 取得匹配方式对应的展示名称。
 * @param {*} type 规则或状态类型。
 * @returns {string} 用于页面展示的格式化文本。
 */
function ruleTypeLabel(type) {
  return rule_match_type.value.find((item) => item.value === type)?.label || type || '-'
}

const ruleRows = computed(() => {
  const rows = parseJson(latestInstance.value?.matchRuleSnapshot, [])
  return Array.isArray(rows) ? rows : []
})
const pagedRuleRows = computed(() => {
  const start = (rulePage.pageNum - 1) * rulePage.pageSize
  return ruleRows.value.slice(start, start + rulePage.pageSize)
})

/**
 * 将当前页规则数据适配为 QtTable 响应。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function getRuleTableData() {
  return Promise.resolve({ data: pagedRuleRows.value })
}

/**
 * 刷新匹配规则表格并重新计算布局。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function refreshRuleTable() {
  ruleTableRef.value?.getList()
}

/**
 * 根据模型字段编号获取规则字段名称。
 * @param {*} fieldId 模型字段编号。
 * @returns {string} 用于页面展示的格式化文本。
 */
function ruleFieldName(fieldId) {
  const field = sourceColumns.value.find((column) =>
    [column.id, column.columnCode, column.columnName].some((value) => String(value) === String(fieldId))
  )
  return field?.columnName || String(fieldId || '-')
}

/**
 * 将参与字段与匹配逻辑拆为上下两行展示。
 * @param {*} row 当前业务记录。
 * @returns {Array} 整理后的业务记录集合。
 */
function ruleLogicLines(row) {
  const config = parseJson(row.ruleConfig, {})
  const fields = Array.isArray(config?.fieldList) && config.fieldList.length
    ? config.fieldList
    : String(row.modelFieldId || '').split(',').filter(Boolean).map((modelFieldId) => ({ modelFieldId }))
  const validFields = fields.filter((item) => item?.modelFieldId)
  const fieldText = validFields.map((item) => ruleFieldName(item.modelFieldId)).join('、') || '参与字段'
  const displayFieldText = row.ruleType === 'WEIGHTED'
    ? validFields.map((item) => `${ruleFieldName(item.modelFieldId)} ${item.weight ?? '-'}%`).join(' ') || fieldText
    : fieldText
  let logicText = '完全一致'
  if (row.ruleType === 'FUZZY') logicText = `编辑距离相似度 ≥ ${config.similarityThreshold ?? '-'}`
  else if (row.ruleType === 'WEIGHTED') logicText = `最终阈值 ${config.finalThreshold ?? '-'}`
  else if (row.ruleType === 'STD_EXACT') logicText = '标准化后完全一致'
  return [
    { key: 'fields', text: displayFieldText },
    { key: 'logic', text: logicText },
  ]
}

/**
 * 判断规则是否处于启用状态。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isRuleEnabled(row) {
  return row.validFlag === true || row.validFlag === 1 || row.validFlag === '1'
}

/**
 * 判断规则是否满足当前检测的可用条件。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isRuleAvailable(row) {
  return isRuleEnabled(row) && String(row.abnormalFlag) !== '1'
}

/**
 * 生成规则当前是否可用的展示文本。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function ruleAvailabilityText(row) {
  if (row.validFlag === undefined || row.validFlag === null) return '-'
  return isRuleAvailable(row) ? '是' : '否'
}

/**
 * 加载模型选项并查询重复检测任务列表。
 * @param {*} params 表格查询、分页及排序参数。
 * @returns {Promise<*>} 当前业务计算结果。
 */
async function queryTasks(params) {
  try {
    if (!modelOptions.value.length) modelOptions.value = await loadPublishedModels()
  } catch {
    // 模型名称降级为编号，列表仍可查询。
  }
  return listMergeTask(params)
}

/**
 * 整理查询条件及分页排序参数。
 * @param {*} params 表格查询、分页及排序参数。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatParams(params) {
  const taskName = String(params.taskName || '').trim()
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    taskName: taskName || undefined,
    scopeType: params.scopeType || undefined,
    modelId: params.modelId || undefined,
    modelIds: params.modelIds,
    status: params.status || undefined,
  }
}

/**
 * 将接口列表转换为页面展示数据。
 * @param {*} rows 待处理的记录列表。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatData(rows) {
  return (rows || []).map((row) => ({ ...row, modelName: getModelName(modelOptions.value, row.modelId) }))
}

/** 按模型编号读取已发布模型列表中的展示信息。 */
function getTaskModel(row) {
  return modelOptions.value.find((item) => String(item.value) === String(row.modelId)) || {}
}

/** 按接入任务列表格式显示模型名称及编码。 */
function formatModelTitle(row) {
  const model = getTaskModel(row)
  if (!model.name) return model.code || row.modelName || '-'
  return model.code ? `${model.name} (${model.code})` : model.name
}

function getVersionTagOptions(versionNo) {
  return MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: versionNo }))
}

function openModelDetail(row, tab) {
  if (row.modelId == null) return
  router.push({
    path: '/model/base/detail',
    query: {id: row.modelId, ...(tab ? {tab} : {})},
  })
}

/**
 * 将接口时间格式化为页面展示文本。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}') : '-'
}

/** 将重复检测任务的执行方式和调度周期整理为列表文本。 */
function formatSchedule(row) {
  if (row.triggerType !== 'SCHEDULED') return '仅人工执行'
  return cronToZh(row.cronExpression) || '定时执行'
}

/**
 * 格式化详情页创建时间和最近执行时间，保留秒级精度。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatDetailTime(value) {
  return value ? proxy.parseTime(value, '{y}-{m}-{d} {h}:{i}:{s}') : '-'
}

/**
 * 将来源快照 JSON 转换为字段值映射。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {*} 解析后的数据，失败时返回约定的默认值。
 */
function parseFieldMap(value) {
  const data = parseJson(value, {})
  if (!data || typeof data !== 'object' || Array.isArray(data)) return {}
  return data
}

/**
 * 清理编辑记录并打开新增检测任务弹窗。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openCreate() {
  createRow.value = { modelId: tableStore.params.modelId }
  createTitle.value = '新增重复检测任务'
  createVisible.value = true
}

/**
 * 携带所选任务打开修改弹窗。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openEdit(row) {
  createRow.value = { ...row }
  createTitle.value = '修改重复检测任务'
  createVisible.value = true
}

/**
 * 任务保存成功后刷新检测列表。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleCreateConfirm() {
  tableRef.value?.getList()
}

/**
 * 判断检测任务是否允许删除。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function canDeleteTask(row) {
  return String(row?.status) !== '1'
}

/**
 * 删除选定检测任务并刷新当前列表。
 * @param {*} rows 待处理的记录列表。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function deleteTasks(rows) {
  const list = (rows || []).filter((row) => row?.id && canDeleteTask(row))
  if (!list.length) return
  try {
    await proxy.$modal.confirm(`确认删除选中的 ${list.length} 个检测任务？`)
    await deleteMergeDetectTask(list.map((row) => row.id).join(','))
    proxy.$modal.msgSuccess('删除成功')
    listSelection.rows = []
    tableRef.value?.elTableRef?.clearSelection()
    tableRef.value?.getList()
  } catch {
    // 取消或接口失败时保留当前页面。
  }
}

/**
 * 确认并删除当前检测任务。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function deleteTask(row) {
  try {
    await proxy.$modal.confirm(`是否确认删除检测任务“${row.taskNo || row.id}”？`)
    await deleteMergeDetectTask(row.id)
    proxy.$modal.msgSuccess('删除成功')
    tableRef.value?.getList()
  } catch {
    // 取消或接口失败时保留当前页面。
  }
}

/**
 * 执行所选检测任务。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function runTask(row) {
  if (row?.enableStatus !== 'ENABLED') return
  await runModel(row?.id)
}

/** 沿用接入任务的确认交互，保存后重新读取任务及调度状态。 */
async function changeTaskControl(row, status, schedule) {
  if (controlBusyIds.has(row.id)) return
  controlBusyIds.add(row.id)
  try {
    const action = status === '1' ? '启用' : (schedule ? '暂停' : '停用')
    await proxy.$modal.confirm(`确认${action}该${schedule ? '任务的定时调度' : '重复检测任务'}？`)
    const request = schedule ? changeMergeDetectScheduleStatus : changeMergeDetectTaskStatus
    await request({ id: row.id, status })
    proxy.$modal.msgSuccess('操作成功')
  } catch {
    // 取消或请求失败后重新读取，兼容配置已保存但调度同步失败的情况。
  } finally {
    controlBusyIds.delete(row.id)
    tableRef.value?.getList()
  }
}

/**
 * 提交检测执行请求并刷新任务信息。
 * @param {*} taskId 检测任务编号。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function runModel(taskId) {
  if (!taskId) {
    proxy.$modal.msgWarning('重复检测任务不存在')
    return
  }
  if (starting.value) return
  starting.value = true
  try {
    await proxy.$modal.confirm('确认执行一次？本次将按已保存的任务范围获取当前可参与检测的来源数据。')
    await executeMergeDetectTask({ taskId })
    proxy.$modal.msgSuccess('执行成功')
    if (detailTask.value) closeDetail()
    await nextTick()
    tableRef.value?.getList()
  } catch {
    // 取消或接口失败时保留当前页面。
  } finally {
    starting.value = false
  }
}

/**
 * 携带任务标识跳转到重复检测详情。
 * @param {*} row 当前业务记录。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function openDetail(row) {
  return router.push({ path: '/merge/detect/detail', query: { id: row.id, taskNo: row.taskNo } })
}

const sourceTableRef = ref()
const executionTableRef = ref()
const detailQtConfig = { notPagination: true, sort: false, table: { stripe: true } }
const sourceQtColumns = computed(() => [
  { label: '编号', prop: 'id', sortable: 'custom', width: 70, minWidth: 70, align: 'center', hide: !isSourceColumnVisible('id') },
  { label: '来源系统', slot: 'sourceSystem', minWidth: 280, align: 'left', showOverflowTooltip: true, hide: !isSourceColumnVisible('system') },
  ...sourceFields.value.map((field) => ({
    label: field.label,
    prop: field.key,
    slot: 'sourceValue',
    sourceField: field,
    minWidth: field.isDateTime ? 180 : 140,
    className: field.isDateTime ? 'detect-source-datetime-cell' : '',
    align: 'left',
    showOverflowTooltip: true,
    hide: !isSourceColumnVisible(field.key),
  })),
  { label: '接收时间', prop: 'receiveTime', slot: 'receiveTime', sortable: 'custom', minWidth: 140, align: 'left', showOverflowTooltip: true, hide: !isSourceColumnVisible('receiveTime') },
].filter((column) => !column.hide))
const executionQtColumns = computed(() => [
  { label: '编号', prop: 'id', sortable: 'custom', width: 70, minWidth: 70 },
  { label: '执行编号', prop: 'instanceNo', minWidth: 180, align: 'left', showOverflowTooltip: true },
  { label: '执行时间', prop: 'startTime', slot: 'startTime', sortable: 'custom', minWidth: 160 },
  { label: '实际检测数据', prop: 'sourceCount', slot: 'sourceCount', minWidth: 130 },
  { label: '候选对象组', prop: 'groupCount', slot: 'groupCount', minWidth: 120 },
  { label: '执行状态', prop: 'status', slot: 'status', minWidth: 110 },
  { label: '操作', prop: 'action', slot: 'action', minWidth: 110, fixed: 'right' },
].filter((column) => isInstanceColumnVisible(column.prop)))
const sourceQtEvents = {
  onSortChange: ({ orderByColumn, isAsc }) => {
    handleSourceSort({ prop: orderByColumn, order: isAsc })
    sourceIdSort.value = orderByColumn === 'id' ? isAsc : null
  },
}
const executionQtEvents = {
  onSortChange: ({ orderByColumn, isAsc }) => handleInstanceSort({ prop: orderByColumn, order: isAsc }),
}
/**
 * 来源快照沿用既有分页，编号默认不排序，点击后切换升降序。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function loadSourceTableRows() {
  return Promise.resolve({ data: pagedSourceRows.value })
}

/**
 * 将服务端分页后的执行记录交给 QtTable，保留原有请求参数。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function loadExecutionTableRows() {
  return Promise.resolve({ data: instanceRows.value })
}

watch(pagedSourceRows, async () => {
  await nextTick()
  sourceTableRef.value?.getList()
})
watch(instanceRows, async () => {
  await nextTick()
  executionTableRef.value?.getList()
})

/**
 * 打开当前任务的执行实例弹窗。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openInstances(row) {
  instancesTask.value = row
  instancesVisible.value = true
}

/**
 * 加载任务详情所需的来源快照、模型列及执行记录。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadTaskDetail(row) {
  resultVisible.value = false
  detailTask.value = row
  detailTab.value = route.query.tab === 'executions' ? 'executions' : 'source'
  sourcePage.pageNum = 1
  sourceSortOrder.value = null
  sourceIdSort.value = null
  rulePage.pageNum = 1
  sourceColumns.value = []
  sourceSystems.value = []
  latestInstance.value = null
  sourceDetailLoaded = false
  sourceDetailPromise = null
  instancesLoaded = false
  instancesPromise = null
  instanceQuery.taskId = row.id
  instanceQuery.pageNum = 1
  // 执行记录不依赖来源快照和模型字段定义，避免这些请求拖住执行记录页签。
  detailLoading.value = detailTab.value !== 'executions'
  try {
    await loadDetailTabData(detailTab.value)
    await nextTick()
    if (detailTab.value === 'rules') refreshRuleTable()
  } finally {
    detailLoading.value = false
  }
}

/** 按当前页签加载必需数据，首次切换页签时才请求对应内容。 */
async function loadDetailTabData(tabName) {
  if (tabName === 'executions') {
    if (instancesLoaded) return
    if (!instancesPromise) {
      instancesPromise = loadInstances().then((loaded) => {
        instancesLoaded = loaded
      }).finally(() => {
        instancesPromise = null
      })
    }
    await instancesPromise
    return
  }

  if (!sourceDetailLoaded) {
    if (!sourceDetailPromise) {
      sourceDetailPromise = Promise.all([
        loadLatestSnapshot(detailTask.value.id),
        loadSourceColumns(detailTask.value.modelId),
        loadSourceSystems(),
      ]).then(() => {
        sourceDetailLoaded = true
      }).finally(() => {
        sourceDetailPromise = null
      })
    }
    await sourceDetailPromise
  }
}

/** 首次打开页签时按需加载；执行记录使用自身的表格加载状态。 */
async function handleDetailTabChange(tabName) {
  if (!detailTask.value) return
  if (tabName === 'executions') {
    await loadDetailTabData(tabName)
    return
  }
  detailLoading.value = true
  try {
    await loadDetailTabData(tabName)
    await nextTick()
    if (tabName === 'rules') refreshRuleTable()
  } finally {
    detailLoading.value = false
  }
}

/**
 * 返回当前模块的列表页面。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function closeDetail() {
  return router.push('/merge/detect')
}

/**
 * 刷新当前来源数据或匹配规则页签所需的快照和字段。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function refreshDetailData() {
  if (!detailTask.value || detailLoading.value) return
  detailLoading.value = true
  try {
    sourceDetailLoaded = false
    sourceDetailPromise = null
    await loadDetailTabData(detailTab.value)
    await nextTick()
    if (detailTab.value === 'rules') refreshRuleTable()
  } finally {
    detailLoading.value = false
  }
}

/**
 * 读取当前任务最新执行实例的来源快照。
 * @param {*} taskId 检测任务编号。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadLatestSnapshot(taskId) {
  const res = await listMergeInstance({ taskId, pageNum: 1, pageSize: 1, includeSnapshot: true })
  latestInstance.value = res.data?.rows?.[0] || null
}

/** 补齐来源快照未保存的系统图标和描述。 */
async function loadSourceSystems() {
  try {
    const response = await listIngestSource({ pageNum: 1, pageSize: -1 })
    sourceSystems.value = response.data?.rows || response.data || []
  } catch (error) {
    console.warn('加载来源系统信息失败，保留快照中的名称和编码', error)
  }
}

/**
 * 读取当前模型的来源字段定义。
 * @param {*} modelId 主数据模型编号。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadSourceColumns(modelId) {
  if (!modelId) return
  try {
    const res = await getSourceDataModelColumns(modelId)
    sourceColumns.value = Array.isArray(res.data) ? res.data : []
  } catch {
    sourceColumns.value = []
  }
}

/**
 * 按当前分页排序条件加载执行记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadInstances() {
  instanceLoading.value = true
  let loaded = false
  try {
    const res = await listMergeInstance({
      taskId: instanceQuery.taskId,
      pageNum: instanceQuery.pageNum,
      pageSize: instanceQuery.pageSize,
      orderByColumn: instanceQuery.orderByColumn,
      isAsc: instanceQuery.isAsc,
    })
    instanceRows.value = res.data?.rows || []
    instanceQuery.total = res.data?.total || 0
    loaded = true
  } catch {
    instanceRows.value = []
    instanceQuery.total = 0
  } finally {
    instanceLoading.value = false
  }
  return loaded
}

/**
 * 重置执行记录页码并重新查询。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function refreshInstances() {
  instanceQuery.pageNum = 1
  loadInstances()
}

/**
 * 判断执行记录列是否处于显示状态。
 * @param {*} key 待判断或更新的列标识。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isInstanceColumnVisible(key) {
  return instanceColumns.find((column) => column.key === key)?.visible !== false
}

/**
 * 判断来源数据列是否处于显示状态。
 * @param {*} key 待判断或更新的列标识。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isSourceColumnVisible(key) {
  return sourceColumnVisibility[key] !== false
}

/**
 * 更新指定来源数据列的显隐状态。
 * @param {*} key 待判断或更新的列标识。
 * @param {*} visible 该列是否显示。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function setSourceColumnVisible(key, visible) {
  sourceColumnVisibility[key] = visible
}

/**
 * 切换接收时间的升序、降序或默认顺序。
 * @param {Object} options 组件回调的字段、排序或选择参数。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleSourceSort({ prop, order }) {
  sourceSortOrder.value = prop === 'receiveTime' ? order : null
  sourcePage.pageNum = 1
}

/**
 * 切换执行记录排序并重新查询第一页。
 * @param {Object} options 组件回调提供的字段集合。
 * @param {*} options.prop 排序列字段名。
 * @param {*} options.order 排序方向。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleInstanceSort({ prop, order }) {
  instanceQuery.orderByColumn = prop === 'startTime' ? 'start_time' : prop
  instanceQuery.isAsc = order === 'ascending' ? 'asc' : 'descending'
  instanceQuery.pageNum = 1
  loadInstances()
}

/**
 * 打开所选执行实例的候选组结果。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openResult(row) {
  resultVisible.value = true
  resultDialogRef.value?.open(row, detailTask.value?.modelId)
}

watch(() => route.fullPath, async () => {
  if (props.detailPage || route.path !== '/merge/detect') return
  await nextTick()
  tableRef.value?.getList()
})

watch(() => [route.query.id, route.query.taskNo, route.query.tab], async ([id, taskNo], _previous, onCleanup) => {
  if (!props.detailPage || route.path !== '/merge/detect/detail') return
  let active = true
  onCleanup(() => { active = false })
  detailLoading.value = true
  detailTask.value = null
  try {
    const res = id && taskNo ? await queryTasks({ keyword: taskNo, pageNum: 1, pageSize: 10 }) : null
    if (!active || route.path !== '/merge/detect/detail') return
    const row = res?.data?.rows?.find((task) => String(task.id) === String(id))
    if (!row) {
      proxy.$modal.msgWarning('重复检测任务不存在')
      await closeDetail()
      return
    }
    await loadTaskDetail(row)
  } finally {
    if (active) detailLoading.value = false
  }
}, { immediate: true })
</script>

<style scoped>
.model-info {
  min-width: 0;
}

.model-info-title {
  display: block;
}

.model-info-title[role='link'] {
  cursor: pointer;
  color: var(--el-color-primary);
}

.model-info :deep(.qt-table-info__main > .dict-tag-container) {
  flex-shrink: 0;
}
</style>

<style lang="scss">
.merge-list-main {
  min-width: 0;
}

.detect-detail-page {
  display: flex;
  flex-direction: column;
  > .el-container {
    flex: 1;
  }
  .main-content > .detect-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
}

.detect-rule-logic {
  line-height: 20px;
}

.detect-last-execute-info {
  display: flex;
  flex-direction: column;
  font-size: var(--qt-font-size-base);
  color: var(--qt-text-primary);
}

.detect-config-tags .tag-container {
  flex-wrap: nowrap;
}

.detect-schedule-summary {
  display: flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.detect-schedule-summary > .el-icon {
  flex-shrink: 0;
}

.detect-schedule-summary__text {
  min-width: 0;
}

.detect-last-execute-info__row {
  display: flex;
  align-items: center;
  min-width: 0;
}

.detect-last-execute-info__value {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detect-rule-logic__secondary {
  color: var(--el-text-color-secondary);
  font-size: var(--el-font-size-extra-small);
}

.detect-source-secondary-cell {
  line-height: 1.45;
}

.detect-source-datetime-cell .cell {
  white-space: nowrap;
  word-break: keep-all;
}

.detect-source-secondary-cell > div + div {
  color: var(--qt-text-secondary);
  font-size: var(--el-font-size-extra-small);
}

.detect-detail {
  .pagecont-bottom {
    min-height: calc(100vh - 340px) !important;
  }
  .infotop-title.clearfixs {
    .task-item {
      flex: 1;
      min-width: 0;
    }
  }
}

.detail-tab-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--qt-space-3);
  min-height: var(--qt-control-height);
  margin-bottom: var(--qt-space-2);
}

.detail-tab-tip {
  flex: 1;
  min-width: 0;
}
</style>
