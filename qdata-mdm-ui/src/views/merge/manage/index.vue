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
  合并管理列表与处理向导，负责来源核对、字段取值、结果确认和质量提交。
  @prop detailPage 是否作为路由详情页展示。
  @author chentangwen
-->

<template>
  <div ref="app-container" class="app-container">
    <GuideTip v-if="!detailVisible" tip-id="merge/manage.list" />
    <div v-if="detailVisible && detailLoading && !detail" v-loading="true" class="merge-detail-loading"></div>
    <el-container v-else-if="!detailVisible">
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
      <el-main class="main-content merge-list-main">
        <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
          <template #actions-data>
            <GuideTipPanel
                type="plain"
                title="在此对比各条来源数据，选择或调整合并字段的最终取值，确认合并结果后执行质量检测，全部通过后即可提交审批。"
            />
          </template>
          <template #search>
            <qt-search-bar
                v-bind="searchStore"
                :params="tableStore.params"
                :tableRef="tableRef"
                @reset="resetQuery"
            />
          </template>
          <qt-table ref="tableRef" v-bind="tableStore" :columns="tableStore.columns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)" class="merge-manage-table">
            <template #taskInfoTitle="{ row }">
              <el-link
                v-if="getDisplayTitle(row.taskName) && $auth.hasPermi('merge:manage:query')"
                type="primary" :underline="false"
                role="link" tabindex="0"
                class="qt-detail-link text-ellipsis"
                :title="String(row.taskName)"
                @click.stop="openReadonlyDetail(row)"
                @keydown.enter.stop.prevent="openReadonlyDetail(row)"
                @keydown.space.stop.prevent="openReadonlyDetail(row)"
              >{{ row.taskName }}</el-link>
              <span v-else class="qt-detail-link text-ellipsis" :title="getDisplayTitle(row.taskName)">{{ getDisplayTitle(row.taskName) ?? '-' }}</span>
            </template>
            <template #location="{ row }">
              <qt-model-location :model="getModelLocation(row)" />
            </template>
            <template #modelInfo="{ row }">
              <div class="qt-table-info model-info">
                <div class="qt-table-info__main">
                  <img :src="getModelIcon({ icon: row.modelIcon, type: row.modelType })" alt="" class="info-icon"/>
                  <span
                    class="info-title model-info-title text-ellipsis"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row)"
                    @keydown.enter="openModelDetail(row)"
                    @keydown.space.prevent="openModelDetail(row)"
                  >{{ formatModelTitle(row) }}</span>
                  <dict-tag
                    v-if="row.modelType"
                    :options="MODEL_TYPE_OPTIONS"
                    :value="row.modelType"
                    size="small"
                    type="primary"
                  />
                  <dict-tag
                    v-if="row.modelVersionNo"
                    class="ml4"
                    :class="{ pointer: row.modelId != null }"
                    :options="getVersionTagOptions(row)"
                    :value="row.modelVersionStatus ?? row.modelVersionNo"
                    size="small"
                    type="info"
                    :role="row.modelId != null ? 'link' : undefined"
                    :tabindex="row.modelId != null ? 0 : undefined"
                    @click="openModelDetail(row, 'versions')"
                    @keydown.enter="openModelDetail(row, 'versions')"
                    @keydown.space.prevent="openModelDetail(row, 'versions')"
                  />
                </div>
                <div class="info-desc text-ellipsis">{{ row.modelDescription || '-' }}</div>
              </div>
            </template>
            <template #sourceSystems="{ row }">
              <source-relation-tags :items="getSourceSystemList(row.sourceSystems)"/>
            </template>
            <template #matchRules="{ row }">
              <source-relation-tags :items="getMatchRuleTags(row.ruleNames)"/>
            </template>
            <template #status="{ row }">
              <el-tag :type="getOptionType(PENDING_STATUS_OPTIONS, row.status)" size="default">
                {{ getOptionLabel(PENDING_STATUS_OPTIONS, row.status) }}
              </el-tag>
            </template>
            <template #handle="{ row }">
              <div class="merge-operation-actions">
                <el-button
                    v-hasPermi="['merge:manage:query']"
                    link
                    type="primary"
                    icon="View"
                    @click="openReadonlyDetail(row)"
                    class="merge-operation-button"
                >详情
                </el-button>
                <el-button
                    :disabled="!isPendingProcessStatus(getRowStatus(row)) && !isReprocessRow(row)"
                    v-hasPermi="['merge:manage:adjust']"
                    link
                    type="primary"
                    @click="openProcess(row, isReprocessRow(row) ? 'reprocess' : 'start')"
                    class="merge-operation-button"
                >
                  <template #icon>
                    <svg-icon :icon-class="isReprocessRow(row) ? 'manage-reprocess' : 'manage-start-processing'" />
                  </template>
                  {{ isReprocessRow(row) ? '重新处理' : '开始处理' }}
                </el-button>
                <el-popover
                    placement="bottom"
                    :width="150"
                    trigger="click"
                    popper-class="table-action-popover handle-popover"
                >
                  <template #reference>
                    <el-button link type="primary" icon="ArrowDown">更多</el-button>
                  </template>
                  <div class="butgdlist task-action-list">
                    <el-button
                        :disabled="!['6', '7'].includes(getRowStatus(row)) || !canSubmitPending(getRowStatus(row), row.qualityStatus)"
                        v-hasPermi="['merge:manage:submit']"
                        link
                        type="primary"
                        @click="handleSubmitApproval(row)"
                        class="merge-operation-button"
                    >
                      <template #icon>
                        <svg-icon icon-class="manage-submit-approval" />
                      </template>
                      提交审批
                    </el-button>
                    <el-button
                        v-hasPermi="['merge:manage:submit']"
                        link
                        type="primary"
                        :disabled="!isApprovingStatus(getRowStatus(row))"
                        @click="handleWithdraw(row)"
                        class="merge-operation-button"
                    >
                      <template #icon>
                        <svg-icon icon-class="manage-withdraw-approval" />
                      </template>
                      撤回审批
                    </el-button>
                    <el-button
                        v-hasPermi="['merge:manage:query']"
                        link
                        type="primary"
                        icon="View"
                        :disabled="!isApprovingStatus(getRowStatus(row)) && !['5', '9'].includes(getRowStatus(row))"
                        @click="openApproval(row)"
                        class="merge-operation-button"
                    >{{ getRowStatus(row) === '9' ? '查看结果' : '查看审批' }}
                    </el-button>
                  </div>
                </el-popover>
              </div>
            </template>
          </qt-table>
        </qt-wrap>
      </el-main>
    </el-container>

    <div v-else-if="isReadonlyResult" v-loading="detailLoading" class="merge-result-page">
      <DetailInfo
          v-if="detail"
          :data="resultHeaderData"
          :header="{ idKey: 'id', nameKey: 'taskName', statusKey: 'status', statusOptions: resultStatusOptions, backText: '返回', routerBack: false }"
          :items="resultHeaderItems"
          mode="free"
          @back="closeDetail"
      >
        <template #model>
          <div class="qt-table-info merge-detail-model">
            <div class="qt-table-info__main">
              <img :src="getModelIcon({type: detailModelInfo.modelType})" alt="" class="info-icon"/>
              <span class="info-title text-ellipsis" :title="formatModelTitle(detailModelInfo)">
                {{ formatModelTitle(detailModelInfo) }}
              </span>
              <dict-tag
                :options="MODEL_TYPE_OPTIONS"
                :value="detailModelInfo.modelType"
                size="small"
                type="primary"
              />
              <dict-tag
                v-if="detail.modelVersionNo"
                :options="MODEL_STATUS_OPTIONS.map(item => ({ ...item, label: detail.modelVersionNo }))"
                :value="detail.modelVersionStatus"
                size="small"
              />
            </div>
          </div>
        </template>
        <template #scope>
          <dict-tag v-if="detail.scopeType" type="primary">{{ getScopeText(detail.scopeType) }}</dict-tag>
          <span v-else>-</span>
        </template>
      </DetailInfo>
      <div v-if="detail" class="pagecont-bottom">
        <el-tabs v-model="detailActiveTab">
          <qt-tab-pane label="结果与来源" name="result">
            <ResultSourceTab :detail="detail"/>
          </qt-tab-pane>
          <qt-tab-pane label="质量检测" name="quality">
            <QualityResultTab :detail="detail"/>
          </qt-tab-pane>
          <qt-tab-pane label="审批记录" name="approval">
            <ApprovalRecordTab :detail="detail" :active="detailActiveTab === 'approval'"/>
          </qt-tab-pane>
        </el-tabs>
      </div>
    </div>

    <div v-else class="merge-config-page">
      <div class="merge-step-card">
        <Steps :steps="MERGE_WIZARD_TITLES.map((title) => ({ title }))" :active-step="wizardStep - 1"
               @change="goStep($event + 1)"
        />
      </div>
      <div v-loading="detailLoading" class="merge-config-card">
        <template v-if="detail">
          <div v-show="wizardStep === 1" class="merge-section">
            <qt-wrap
                :columns="sourceQtColumns"
                :table-ref="sourceTableRef"
                :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <div class="h2-titles merge-section-title">来源数据</div>
              </template>
              <qt-table
                  ref="sourceTableRef"
                  :columns="sourceQtColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                  :config="sourceQtConfig"
                  :func="loadSourceTableRows"
                  :events="sourceQtEvents"
              >
                <template #sourceSystem="{ row }">{{ displayText(row.systemCode || row.systemName) }}</template>
                <template #sourceValue="{ row, column_data: { sourceColumn } }">
                  {{ displayText(getSourceColumnValue(row, sourceColumn)) }}
                </template>
              </qt-table>
            </qt-wrap>
            <pagination
                v-show="sourceRows.length > 0"
                :total="sourceRows.length"
                v-model:page="sourcePage.pageNum"
                v-model:limit="sourcePage.pageSize"
                :auto-scroll="false"
            />
          </div>

          <div v-show="wizardStep === 2" class="merge-section">
            <guide-tip-panel
              v-if="isQualityFailed"
              type="warning"
              allow-wrap
              title="当前合并结果<span class='merge-tip-danger'>质量检测未通过</span>，请核对未通过项目及原因，修改不符合要求的字段后重新确认，并再次执行质量检测。"
              class="mb15"
            />
            <guide-tip-panel
              v-else-if="String(detail.status) === '5'"
              type="hint"
              allow-wrap
              class="mb15"
            >
              <template #title-append>
                <span>审批驳回原因：<span class="merge-tip-danger">{{ approvalRejectionReason }}</span>。请结合驳回意见核对合并字段，修改并确认结果后重新执行质量检测，通过后再次提交审批。</span>
              </template>
            </guide-tip-panel>
            <qt-wrap
                :columns="mergeFieldColumns"
                :table-ref="mergeFieldTableRef"
                :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <div class="h2-titles merge-section-title">字段合并</div>
              </template>
              <qt-table
                  ref="mergeFieldTableRef"
                  :columns="mergeFieldColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                  :config="mergeFieldTableConfig"
                  :func="loadMergeFields"
              >
                <template #index="{ $index }">{{ $index + 1 }}</template>
                <template #mergeField="{ row }">
                  {{ row.columnName || row.fieldPath }}
                  <el-tag size="small" :type="getFieldMergeStatus(row) === '需确认' ? 'warning' : 'info'" class="ml5">
                    {{ getFieldMergeStatus(row) }}
                  </el-tag>
                </template>
                <template #sourceCandidate="{ row, column_data: { candidate } }">
                  <span v-if="isAutoGeneratedField(row)">该字段不从来源数据取值，审批通过后自动生成</span>
                  <el-radio
                      v-else-if="getSourceCandidate(row, candidate)"
                      :model-value="row.pick"
                      :label="getCandidateKey(getSourceCandidate(row, candidate))"
                      :disabled="!row.editable || !canAdjust"
                      @change="selectSourceCandidate(row, getSourceCandidate(row, candidate))"
                  >
                    {{ displayText(formatMergeDate(getSourceCandidate(row, candidate).value, row.dataType)) }}
                    <el-tag
                        v-if="isRecommendedCandidate(row, getSourceCandidate(row, candidate))"
                        size="small"
                        type="info"
                        class="ml5"
                    >系统推荐
                    </el-tag>
                  </el-radio>
                  <span v-else>-</span>
                </template>
                <template #manualValue="{ row }">
                  <el-row align="middle">
                    <el-col :span="3">
                      <el-radio
                          :model-value="row.pick"
                          label="manual"
                          :disabled="!row.editable || !canAdjust || isAutoGeneratedField(row)"
                          @change="selectManualValue(row)"
                      >
                        <span/>
                      </el-radio>
                    </el-col>
                    <el-col :span="21">
                      <el-input
                        :model-value="row.pick === 'manual' ? row.draftValue : ''"
                        :disabled="row.pick !== 'manual' || !row.editable || !canAdjust || isAutoGeneratedField(row)"
                        placeholder="请输入其他值"
                        @update:model-value="row.draftValue = $event"
                        @focus="selectManualValue(row)"
                        @input="syncReason(row)"
                      />
                    </el-col>
                  </el-row>
                </template>
                <template #adjustReason="{ row }">
                  <el-input
                      v-model="row.draftReason"
                      placeholder="请输入调整原因"
                      :disabled="!canAdjust || !canEditReason(row)"
                  />
                </template>
              </qt-table>
            </qt-wrap>
          </div>
          <div v-show="wizardStep === 3" class="merge-section">
            <qt-wrap
              :columns="resultFieldColumns"
              :table-ref="resultFieldTableRef"
              :config="{ fullContent: false, actions: { table: { search: false } } }"
            >
              <template #actions-data>
                <div class="h2-titles merge-section-title">结果确认</div>
              </template>
              <qt-table
                ref="resultFieldTableRef"
                :columns="resultFieldColumns.map(column => column.slot && column.prop ? { ...column, property: column.prop, prop: undefined } : column)"
                :config="resultFieldTableConfig"
                :func="loadResultFields"
                :events="resultFieldTableEvents"
              >
                <template #field="{ row }">{{ row.columnName || row.fieldPath }}</template>
                <template #finalValue="{ row }">{{ displayText(formatMergeDate(row.draftValue, row.dataType)) }}</template>
                <template #source="{ row }">{{ getFinalSourceLabel(row) }}</template>
                <template #reason="{ row }">{{ displayText(getCurrentReason(row)) }}</template>
              </qt-table>
            </qt-wrap>
          </div>

          <div v-show="wizardStep === 4" class="merge-section">
            <guide-tip-panel
              v-if="isQualityFailed"
              type="warning"
              allow-wrap
              class="mb15"
            >
              <template #title-append>
                <span>质量检测结果：<span class="merge-tip-danger">质量检测未通过</span>，请核对未通过的规则及原因，返回“字段合并”修改相关字段，重新确认结果后再次检测。</span>
              </template>
            </guide-tip-panel>
            <!-- 质量检测通过且当前状态允许提交时，提示用户可以提交审批。 -->
            <guide-tip-panel
              v-else-if="canSubmit"
              type="hint"
              allow-wrap
              title="检测结果：<span class='merge-tip-success'>质量检测已全部通过</span>，请再次核对合并结果及字段取值，确认符合业务要求后，可提交审批进入后续处理流程。"
              class="mb15"
            />
            <guide-tip-panel
              v-else-if="['5', '6', '7'].includes(String(detail.status))"
              type="hint"
              allow-wrap
              title="当前可继续核对并修改合并结果，调整字段取值后请重新确认并执行质量检测，确保各项检测通过后，再次提交审批。"
              class="mb15"
            />
            <guide-tip-panel
              v-else-if="String(detail.qualityStatus) === '5'"
              type="hint"
              allow-wrap
              title="合并字段已修改，<span class='merge-tip-warning'>原质量结果已失效</span>，请核对并确认最新字段取值后重新执行质量检测，以本次检测结果作为提交审批的依据。"
              class="mb15"
            />
            <div class="column-form">
              <div class="h2-titles row-full">质量与提交</div>
              <el-empty
                v-if="String(detail.qualityStatus) === '0' && !qualityRows.length"
                :image-size="80"
                description="尚未执行质量检测，合并结果已确认，请点击下方执行质量检测。"
                class="row-full"
              />
              <el-descriptions :column="3" border class="merge-effective-info row-full">
                <template v-for="(row, index) in qualityRows" :key="row.ruleName + index">
                  <el-descriptions-item label="质量规则" label-class-name="merge-effective-label">
                    {{ displayText(row.ruleName) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="检测结果" label-class-name="merge-effective-label">
                    <dict-tag :type="getQualityResult(row).type">{{ getQualityResult(row).label }}</dict-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="检测说明" label-class-name="merge-effective-label">
                    {{ displayText(getQualityExplanation(row)) }}
                  </el-descriptions-item>
                </template>
              </el-descriptions>
            </div>
            <div v-if="approvalInfoVisible" class="mt15">
              <div class="h2-titles">审批信息</div>
              <el-descriptions :column="3" border class="merge-effective-info">
                <el-descriptions-item label="审批状态" label-class-name="merge-effective-label">
                  {{ getOptionLabel(PENDING_STATUS_OPTIONS, detail.status) }}
                </el-descriptions-item>
                <el-descriptions-item label="流程实例" label-class-name="merge-effective-label">
                  {{ displayText(detail.flowInstanceId) }}
                </el-descriptions-item>
                <el-descriptions-item label="申请记录" label-class-name="merge-effective-label">
                  {{ displayText(detail.requestId) }}
                </el-descriptions-item>
                <el-descriptions-item label="审批说明" label-class-name="merge-effective-label" :span="3">
                  {{ displayText(detail.errorMessage) }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </template>
        <div class="button-style" :class="{ 'merge-quality-actions': wizardStep === 4 }">
          <el-button @click="closeDetail">返回列表</el-button>
          <el-button v-if="wizardStep > 1 && !isQualityFailed" @click="wizardStep = wizardStep - 1">上一步</el-button>
          <el-button
              v-if="canNext"
              type="primary"
              :loading="acting"
              @click="goNextStep"
          >下一步
          </el-button>
          <el-button
              v-if="wizardStep === 3 && canConfirm"
              v-hasPermi="['merge:manage:confirm']"
              type="primary"
              :loading="acting"
              @click="handleConfirm"
          >确认合并
          </el-button>
          <el-button
              v-if="wizardStep === 4 && canQuality"
              v-hasPermi="['merge:manage:quality']"
              type="primary"
              :loading="acting"
              @click="handleQuality"
          >执行质量检测
          </el-button>
          <el-button v-if="wizardStep === 4 && isQualityFailed" @click="returnToFieldMerge">返回修改</el-button>
          <el-button
              v-if="wizardStep === 4 && canSubmit"
              v-hasPermi="['merge:manage:submit']"
              type="primary"
              :loading="acting"
              @click="handleSubmit"
          >{{ isEditableStoppedStatus(detail.status) ? '重新提交审批' : '提交审批' }}
          </el-button>
        </div>
      </div>
    </div>
    <ApprovalViewDialog ref="approvalDialogRef" :model-options="modelOptions"/>
  </div>
</template>

<script setup name="Manage">
import { getDisplayTitle } from '@/utils/anivia.js'
import { useDict } from '@/utils/dict'
import QtTree from '@/components/QtTree/index.vue'
import QtModelLocation from '@/views/merge/components/QtModelLocation/index.vue'
import {
  computed,
  getCurrentInstance,
  nextTick,
  reactive,
  ref,
  watch
} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import Steps from '@/components/Steps/index.vue'
import GuideTipPanel from '@/components/GuideTip/GuideTipPanel.vue'
import ApprovalViewDialog from '@/views/merge/manage/components/approvalViewDialog.vue'
import ResultSourceTab from '@/views/merge/manage/components/resultSourceTab.vue'
import QualityResultTab from '@/views/merge/manage/components/qualityResultTab.vue'
import ApprovalRecordTab from '@/views/merge/manage/components/approvalRecordTab.vue'
import {
  adjustMergeField,
  confirmMergePending,
  getMergePending,
  listMergePending,
  qualityMergePending,
  submitMergePending,
  withdrawMergePending,
  resubmitMergePending
} from '@/api/merge/manage'

import {getModelIcon} from '@/utils/imageUrl'
import {MODEL_TYPE_OPTIONS} from '@/utils/model/modelType'
import {MODEL_STATUS_OPTIONS} from '@/utils/model/modelStatus'
import SourceRelationTags from '@/views/ingest/source/sourceRelationTags.vue'
import {listIngestSource} from '@/api/ingest/source/source'

import { getPublishedModelTree, listPublishedModels } from '@/api/model/base/base';

import { getProjectIcon } from '@/utils/imageUrl';

const ADJUSTABLE_STATUSES = new Set(['0', '1', '2', '3', '5', '6', '7'])

function canAdjustPending(status) {
  return ADJUSTABLE_STATUSES.has(String(status))
}

function canCheckPendingQuality(status) {
  return ['1', '2', '3', '5', '6', '7'].includes(String(status))
}

function canSubmitPending(status, qualityStatus) {
  return ['3', '6', '7'].includes(String(status)) && String(qualityStatus) === '4'
}

function displayText(value) {
  if (value === null || value === undefined || value === '') return '-'
  return value
}

function formatCountText(value, unit) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} ${unit}`
}

function getScopeText(scopeType) {
  if (String(scopeType) === '1') return '全部待治理来源数据'
  if (String(scopeType) === '2') return '指定范围'
  return scopeType ? `范围${scopeType}` : '-'
}

function getOptionLabel(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.label || (value === null || value === undefined || value === '' ? '-' : value)
}

function getOptionType(options, value) {
  const found = (options || []).find((item) => String(item.value) === String(value))
  return found?.elTagType || 'info'
}

const MERGE_WIZARD_TITLES = ['来源数据', '字段合并', '结果确认', '质量与提交']

const { merge_pending_query_status: PENDING_QUERY_STATUS_OPTIONS } = useDict('merge_pending_query_status')

const { merge_pending_status: PENDING_STATUS_OPTIONS } = useDict('merge_pending_status')

function parseQueryNumber(value) {
  if (value === undefined || value === null || value === '') return undefined
  const number = Number(value)
  return Number.isNaN(number) ? undefined : number
}

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

function getSourceIcon(source) {
  return getProjectIcon(source?.icon);
}

function getSourceSystemTags(value, systems = []) {
  return [...new Set(String(value || '').split(/[,，、/|]+/).map(item => item.trim()).filter(Boolean))]
    .map(item => {
      const source = systems.find(system => system.code === item) || systems.find(system => system.name === item)
      return {label: source?.name || item, value: item, elTagType: 'primary', icon: getSourceIcon(source)}
    })
}

const props = defineProps({detailPage: {type: Boolean, default: false}})
const {proxy} = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const tableRef = ref()
const mergeFieldTableRef = ref()
const approvalDialogRef = ref()
const modelOptions = ref([])
const sourceSystemOptions = ref(null)
const detailVisible = computed(() => props.detailPage)
const detailLoading = ref(false)
const detail = ref(null)
const detailStatus = ref('')
const editFields = ref([])
const reason = ref('')
const acting = ref(false)
const wizardStep = ref(1)
const detailActiveTab = ref('result')
const sourcePage = reactive({pageNum: 1, pageSize: 10})
const sourceIndexSort = ref(null)
const resultFieldIndexSort = ref(null)

const tableStore = reactive({
  func: queryPending,
  params: {
    groupNo: route.query.groupNo || route.query.keyword || undefined,
    businessName: route.query.businessName || undefined,
    taskName: route.query.taskName || undefined,
    transferTimeRange: [],
    modelId: parseQueryNumber(route.query.modelId),
    status: route.query.status || '*',
    orderByColumn: 'create_time',
    isAsc: 'descending',
  },
  events: {formatParams, formatData},
  config: {
    sort: true,
    table: {rowKey: 'id', stripe: true, defaultSort: {prop: 'create_time', order: 'descending'}},
    pagination: {layout: 'total, sizes, prev, pager, next, jumper', pageSizes: [10, 20, 50, 100]},
  },
  columns: [
    {label: '编号', prop: 'id', sortable: true, width: 60, minWidth: 60, align: 'center'},
    {label: '任务信息', property: 'taskName', info: { custom: 'taskInfoTitle', desc: 'remark' }, minWidth: 240, align: 'left'},
    {label: '主数据模型', prop: 'modelName', slot: 'modelInfo', minWidth: 260, align: 'left', showOverflowTooltip: {effect: 'light'}},
    { label: '归属位置', prop: 'location', slot: 'location', minWidth: 190, align: 'left' },
    {label: '来源系统', prop: 'sourceSystems', slot: 'sourceSystems', minWidth: 280, align: 'left'},
    {label: '匹配概况', prop: 'ruleCount', slot: 'matchRules', minWidth: 300, align: 'left'},
    {label: '当前状态', prop: 'status', slot: 'status', width: 140, minWidth: 140, align: 'left'},
    {
      label: '最近更新时间',
      prop: 'updateTime',
      sortable: true,
      sortableKey: 'update_time',
      date: true,
      width: 180,
      minWidth: 180,
      align: 'left'
    },
    {
      label: '转入时间',
      prop: 'transferTime',
      date: true,
      width: 180,
      minWidth: 180,
      align: 'left'
    },
    {label: '操作', slot: 'handle', fixed: 'right', width: 250, minWidth: 250, align: 'center', headerAlign: 'center'},
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

const searchStore = reactive({
  items: [
    {label: '任务名称', prop: 'taskName', component: {is: 'input', placeholder: '请输入任务名称'}},
    {
      label: '转入时间',
      prop: 'transferTimeRange',
      class: 'merge-transfer-time-item',
      component: {
        is: 'date-picker',
        type: 'daterange',
        startPlaceholder: '开始日期',
        endPlaceholder: '结束日期',
      },
    },
    {label: '处理状态', prop: 'status', component: {is: 'select', options: PENDING_QUERY_STATUS_OPTIONS}},
  ],
})

const currentPendingStatus = computed(() => String(detail.value?.status ?? detailStatus.value ?? '').trim())
const canAdjust = computed(() => canAdjustPending(currentPendingStatus.value))
const canConfirm = computed(() => canAdjust.value)
const canQuality = computed(() => canCheckPendingQuality(currentPendingStatus.value))
const canSubmit = computed(() => canSubmitPending(currentPendingStatus.value, detail.value?.qualityStatus))
const isQualityFailed = computed(() => String(detail.value?.qualityStatus) === '2' || String(detail.value?.status) === '2')
const isReprocessMode = ref(false)
const canNext = computed(() => wizardStep.value < 3)
const detailModelName = computed(() => getModelName(modelOptions.value, detail.value?.modelId))
const detailModelInfo = computed(() => ({
  modelName: detailModelName.value,
  ...getModelInfo(detail.value?.modelId),
}))
const isEffective = computed(() => String(detail.value?.status || detailStatus.value) === '9')
const isApproving = computed(() => ['4', '8'].includes(String(detail.value?.status || detailStatus.value)))
const isReadonlyResult = computed(() => route.query.view === '1' || isApproving.value || isEffective.value)
const resultStatusOptions = computed(() => PENDING_STATUS_OPTIONS.value)
const resultHeaderData = computed(() => ({
  id: detail.value?.id,
  groupNo: detail.value?.groupNo || detail.value?.id,
  taskName: detail.value?.taskName || detail.value?.groupNo || '-',
  status: detail.value?.status,
}))
const resultHeaderItems = computed(() => [
  {label: '主数据模型', slot: 'model'},
  {label: '检测范围', slot: 'scope'},
  {label: '来源数据', value: formatCountText(detail.value?.sourceCount ?? sourceRows.value.length, '条')},
  {label: '描述', value: displayText(detail.value?.remark), span: 24},
  {label: '最高匹配分', value: detail.value?.maxMatchScore == null ? '-' : `${detail.value.maxMatchScore}分`},
  {label: '命中规则', value: formatCountText(detail.value?.ruleCount, '条')},
  {label: '转入时间', value: detail.value?.transferTime, type: 'time'},
])
const approvalInfoVisible = computed(() => ['4', '5', '6', '7', '8', '9'].includes(String(detail.value?.status)) || detail.value?.flowInstanceId || detail.value?.requestId)
const qualityRows = computed(() => {
  const raw = detail.value?.qualityDetail
  if (!raw) return []
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    return (Array.isArray(parsed) ? parsed : []).map((row, index) => ({
      ruleName: row.ruleName || row.name || ('质量规则 ' + (index + 1)),
      message: row.message || row.result || '',
      executed: row.executed !== false,
      passed: row.passed === true || row.result === '通过',
    }))
  } catch {
    return []
  }
})
const visibleFields = computed(() => editFields.value.filter((field) => !isSystemField(field) && !isAutoGeneratedField(field)))
const sourceCandidateColumns = computed(() => {
  const candidates = new Map()
  visibleFields.value.forEach((field) => {
    ;(field.candidates || []).forEach((item) => {
      const key = getCandidateKey(item)
      if (!key || candidates.has(key)) return
      const system = item.systemCode || item.systemName
      const businessName = item.businessName || system || '来源数据'
      candidates.set(key, {
        key,
        label: system && item.businessName ? businessName + '（' + system + '）' : businessName,
      })
    })
  })
  return [...candidates.values()]
})
const mergeFieldColumns = computed(() => {
  const columns = [
    {label: '编号', prop: 'index', slot: 'index', width: 60, minWidth: 60, align: 'center', fixed: 'left'},
    {label: '合并字段', prop: 'fieldPath', slot: 'mergeField', minWidth: 200, align: 'left', fixed: 'left'},
    ...sourceCandidateColumns.value.map((candidate) => ({
      label: candidate.label,
      prop: `sourceCandidate:${candidate.key}`,
      slot: 'sourceCandidate',
      candidate,
      minWidth: 220,
      align: 'left',
      showOverflowTooltip: true,
    })),
    { label: '其他值', prop: 'draftValue', slot: 'manualValue', minWidth: 220, align: 'left' },
    ...(visibleFields.value.some(canEditReason) ? [{ label: '调整原因', prop: 'draftReason', slot: 'adjustReason', minWidth: 220, align: 'left' }] : []),
  ]
  const context = document.createElement('canvas').getContext('2d')
  if (context) context.font = `600 14px ${getComputedStyle(document.body).fontFamily}`
  return reactive(columns.map((column) => {
    // 表头文字宽度加上单元格留白，保留编辑控件所需的最小列宽。
    const titleWidth = Math.ceil(context?.measureText(column.label).width ?? column.label.length * 14) + 32
    const widthKey = column.width ? 'width' : 'minWidth'
    return {...column, [widthKey]: Math.max(column[widthKey] || 0, titleWidth)}
  }))
})
const mergeFieldTableConfig = computed(() => ({
  initResquest: false,
  notPagination: true,
  table: {data: visibleFields.value, stripe: false, border: false},
}))

/**
 * 为字段合并表提供当前模型的可编辑字段。
 * @returns {Promise<Object>} 整理后的业务参数或展示数据。
 */
async function loadMergeFields() {
  return {data: visibleFields.value}
}

const sourceNameField = computed(() => visibleFields.value.find((field) => field.nameFlag === true)
    || visibleFields.value.find((field) => String(field.columnName || '').includes('名称')))
const SOURCE_DATA_FIELD_LIMIT = 5
const SOURCE_DATA_FIELD_PRIORITY = [
  ['统一社会信用代码', '社会信用代码', '统一信用代码', 'creditcode', 'uscc'],
  ['客户等级', '客户级别', '供应商等级', '供应商级别', 'customerlevel', 'custlevel', 'supplierlevel'],
]
const SOURCE_DATA_MANAGER_ALIASES = ['客户负责人', '供应商负责人', '客户负责人', '客户联系人', '联系人姓名', '客户经理', '业务负责人', '负责人', '负责人', '联系人', 'contactperson', 'contactname', 'responsibleperson', 'manager', 'owner']
const SOURCE_DATA_PHONE_ALIASES = ['联系电话', '联系人电话', '联系手机', '联系电话号码', '手机号码', '手机号', '电话号码', '电话', 'contactnumber', 'contactphone', 'phonenumber', 'telephone', 'mobilephone', 'mobile', 'phone']
const sourceBusinessColumns = computed(() => {
  const nameField = sourceNameField.value
  const selectedFields = nameField ? [nameField] : []
  const managerField = findSourceBusinessField(SOURCE_DATA_MANAGER_ALIASES, selectedFields)
  const phoneField = findSourceBusinessField(SOURCE_DATA_PHONE_ALIASES, [...selectedFields, managerField].filter(Boolean))
  const selectedFieldLimit = SOURCE_DATA_FIELD_LIMIT - 2 - (nameField ? 0 : 1)
  SOURCE_DATA_FIELD_PRIORITY.forEach((aliases) => {
    if (selectedFields.length < selectedFieldLimit) {
      const field = findSourceBusinessField(aliases, selectedFields)
      if (field) selectedFields.push(field)
    }
  })
  visibleFields.value.forEach((field) => {
    if (selectedFields.length < selectedFieldLimit
        && !selectedFields.includes(field)
        && field !== managerField
        && field !== phoneField) selectedFields.push(field)
  })
  const columns = selectedFields.map((field) => ({
    key: field.fieldPath,
    label: field.columnName || field.fieldPath,
    fieldPath: field.fieldPath,
    dataType: field.dataType,
    useBusinessName: field === nameField,
  }))
  if (!nameField) {
    columns.unshift({key: 'businessName', label: '业务名称', useBusinessName: true})
  }
  columns.push({
    key: managerField?.fieldPath || 'customerManager',
    label: managerField?.columnName || managerField?.fieldPath || '客户负责人',
    fieldPath: managerField?.fieldPath,
  })
  columns.push({
    key: phoneField?.fieldPath || 'contactPhone',
    label: phoneField?.columnName || phoneField?.fieldPath || '联系电话',
    fieldPath: phoneField?.fieldPath,
  })
  return columns
})
const sourceRows = computed(() => {
  const map = new Map()
  editFields.value.forEach((field) => {
    ;(field.candidates || []).forEach((item) => {
      const sourceKey = item.sourceRecordId ?? item.stableRecordId ?? item.sourceVersionId ?? item.systemCode ?? item.systemName
      const key = `${item.systemCode || item.systemName || ''}:${sourceKey}`
      if (!map.has(key)) {
        map.set(key, {
          sourceRecordId: item.sourceRecordId,
          stableRecordId: item.stableRecordId,
          systemCode: item.systemCode || '',
          systemName: item.systemName || '',
          businessName: item.businessName || '',
          values: {}
        })
      }
      map.get(key).values[field.fieldPath] = item.value
      if (field === sourceNameField.value && item.value != null) map.get(key).businessName = item.value
    })
  })
  return [...map.values()]
})
const pagedSourceRows = computed(() => {
  const start = (sourcePage.pageNum - 1) * sourcePage.pageSize
  const rows = sourceRows.value.map((row, index) => ({...row, sourceIndex: index + 1}))
  if (sourceIndexSort.value === 'descending') rows.reverse()
  return rows.slice(start, start + sourcePage.pageSize)
})
const sourceTableRef = ref()
const sourceQtConfig = {notPagination: true, sort: false, table: {stripe: true, maxHeight: 420}}
const sourceQtEvents = {
  onSortChange: ({orderByColumn, isAsc}) => handleSourceSortChange({prop: orderByColumn, order: isAsc}),
}
const sourceQtColumns = computed(() => reactive([
  {label: '编号', prop: 'sourceIndex', sortable: 'custom', width: 70, minWidth: 70, align: 'center'},
  {label: '来源系统', prop: 'systemCode', slot: 'sourceSystem', minWidth: 160, align: 'left', showOverflowTooltip: true},
  ...sourceBusinessColumns.value.map((column) => ({
    label: column.label,
    prop: column.key,
    slot: 'sourceValue',
    sourceColumn: column,
    minWidth: 160,
    align: 'left',
    showOverflowTooltip: true,
  })),
]))

/**
 * 将来源数据当前页适配为 QtTable 返回结构，沿用现有分页和编号排序。
 * @returns {Promise<Object>} 符合 QtTable 数据约定的响应。
 */
function loadSourceTableRows() {
  return Promise.resolve({data: pagedSourceRows.value})
}

watch(pagedSourceRows, async () => {
  await nextTick()
  sourceTableRef.value?.getList()
})

const resultFields = computed(() => {
  const rows = visibleFields.value.map((row, index) => ({...row, resultFieldIndex: index + 1}))
  if (resultFieldIndexSort.value === 'descending') rows.reverse()
  return rows
})

const resultFieldTableRef = ref()
const resultFieldColumns = computed(() => reactive([
  {label: '编号', prop: 'resultFieldIndex', sortable: 'custom', width: 70, minWidth: 70, align: 'center'},
  {label: '字段', prop: 'fieldPath', slot: 'field', minWidth: 180, align: 'left'},
  {label: '最终值', prop: 'draftValue', slot: 'finalValue', minWidth: 260, align: 'left'},
  {label: '取值来源', prop: 'draftType', slot: 'source', minWidth: 180, align: 'left', showOverflowTooltip: true},
  ...(isReprocessMode.value || !isPendingProcessStatus(currentPendingStatus.value)
      ? [{label: '调整原因', prop: 'draftReason', slot: 'reason', minWidth: 220, align: 'left', showOverflowTooltip: true}]
      : []),
]))
const resultFieldTableConfig = computed(() => ({
  initResquest: false,
  notPagination: true,
  table: {data: resultFields.value, stripe: true},
}))
const resultFieldTableEvents = {onSortChange: handleResultFieldSortChange}

/**
 * 为结果确认表提供最终字段取值。
 * @returns {Promise<Object>} 整理后的业务参数或展示数据。
 */
async function loadResultFields() {
  return {data: resultFields.value}
}

const approvalRejectionReason = computed(() => String(detail.value?.errorMessage || '').trim().replace(/[。.]+$/, '') || '未填写驳回原因')

/**
 * 加载模型选项并分页查询合并管理记录。
 * @param {*} params 业务查询、分页和排序参数。
 * @returns {Promise<*>} 当前业务计算结果。
 */
async function queryPending(params) {
  if (!sourceSystemOptions.value) {
    try {
      const response = await listIngestSource({pageNum: 1, pageSize: -1})
      sourceSystemOptions.value = response.data?.rows || response.data || []
    } catch {
      // 来源信息加载失败时保留原始文本并使用默认图标，下次查询重试。
    }
  }
  try {
    if (!modelOptions.value.length) modelOptions.value = await loadPublishedModels()
  } catch {
    // 模型名称降级为编号。
  }
  return listMergePending(params)
}

/**
 * 清理查询条件的附加状态，查询由搜索组件统一触发。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function resetQuery() {
  resetTreeSelection()
  tableStore.params.groupNo = undefined
  tableStore.params.businessName = undefined
  tableStore.params.taskName = undefined
  tableStore.params.transferTimeRange = []
  tableStore.params.modelId = undefined
  tableStore.params.status = '*'
}

/**
 * 整理业务筛选条件及分页排序参数。
 * @param {*} params 业务查询、分页和排序参数。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatParams(params) {
  const groupNo = String(params.groupNo || '').trim()
  const businessName = String(params.businessName || '').trim()
  const taskName = String(params.taskName || '').trim()
  const transferTimeRange = params.transferTimeRange || []
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    groupNo: groupNo || undefined,
    businessName: businessName || undefined,
    taskName: taskName || undefined,
    transferStartTime: transferTimeRange[0] ? `${transferTimeRange[0]} 00:00:00` : undefined,
    transferEndTime: transferTimeRange[1] ? `${transferTimeRange[1]} 23:59:59` : undefined,
    modelId: params.modelId || undefined,
    modelIds: params.modelIds,
    status: params.status && params.status !== 'open' ? params.status : undefined,
    orderByColumn: params.orderByColumn,
    isAsc: params.isAsc,
  }
}

/**
 * 将接口记录转换为页面展示数据。
 * @param {*} rows 待处理记录列表。
 * @returns {string} 用于页面展示的格式化文本。
 */
function formatData(rows) {
  return (rows || []).map((row) => ({
    ...row,
    modelName: getModelName(modelOptions.value, row.modelId),
    ...getModelInfo(row.modelId),
    businessName: row.businessName || '-',
  }))
}

/** 将接口中的来源系统文本转换为去重后的标签项。 */
function getSourceSystemList(sourceSystems) {
  return getSourceSystemTags(sourceSystems, sourceSystemOptions.value || [])
}

/** 将具体匹配规则转换为蓝色标签。 */
function getMatchRuleTags(names) {
  return (names || []).map((name) => ({label: name, value: name, elTagType: 'primary'}))
}

/** 使用已发布模型信息回显模型类型、编码及描述。 */
function getModelInfo(modelId) {
  const model = modelOptions.value.find((item) => String(item.value) === String(modelId))
  return {
    modelType: model?.type,
    modelIcon: model?.icon,
    modelCode: model?.code,
    modelDescription: model?.description,
  }
}

/** 统一显示主数据模型名称及编码。 */
function formatModelTitle(row) {
  if (!row.modelName) return row.modelCode || '-'
  return row.modelCode ? `${row.modelName} (${row.modelCode})` : row.modelName
}

function getVersionTagOptions(row) {
  const status = MODEL_STATUS_OPTIONS.find(item => String(item.value) === String(row.modelVersionStatus))
  return [{
    value: row.modelVersionStatus ?? row.modelVersionNo,
    label: row.modelVersionNo,
    elTagType: status?.elTagType || 'info',
  }]
}

function openModelDetail(row, tab) {
  if (row.modelId == null) return
  router.push({
    path: '/model/base/detail',
    query: {id: row.modelId, ...(tab ? {tab} : {})},
  })
}

/**
 * 统一读取合并记录的处理状态。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getRowStatus(row) {
  const value = row?.status ?? row?.processStatus ?? row?.pendingStatus ?? row?.mergeStatus ?? row?.state
  return value === undefined || value === null ? '' : String(value)
}

/**
 * 判断记录是否正处于审批中。
 * @param {*} status 业务处理状态。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isApprovingStatus(status) {
  return ['4', '8'].includes(String(status))
}

/**
 * 列表中统一显示为“待处理”的状态，都从处理向导第一步进入。
 * @param {*} status 业务处理状态。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isPendingProcessStatus(status) {
  return ['0', '1', '3'].includes(String(status))
}

/**
 * 根据处理和质量状态判断是否允许重新处理。
 * @param {*} status 业务处理状态。
 * @param {*} qualityStatus 质量检测状态。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isReprocessStatus(status, qualityStatus) {
  return ['2', '5', '6', '7'].includes(String(status)) || String(qualityStatus) === '2'
}

/**
 * 判断列表记录是否显示重新处理入口。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isReprocessRow(row) {
  return isReprocessStatus(getRowStatus(row), row?.qualityStatus)
}

/**
 * 根据入口参数及记录状态确定处理模式。
 * @param {*} status 业务处理状态。
 * @param {*} requestedMode 入口指定的处理模式。
 * @param {*} qualityStatus 质量检测状态。
 * @returns {*} 当前业务计算结果。
 */
function resolveProcessingMode(status, requestedMode, qualityStatus) {
  const mode = String(requestedMode || '').toLowerCase()
  if (isReprocessStatus(status, qualityStatus) || mode === 'reprocess') return 'reprocess'
  return 'start'
}

/**
 * 判断已停止审批的记录是否仍允许调整。
 * @param {*} status 业务处理状态。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isEditableStoppedStatus(status) {
  return ['6', '7'].includes(String(status))
}

/**
 * 将记录状态转换为页面使用的展示状态。
 * @param {*} status 业务处理状态。
 * @returns {*} 当前业务计算结果。
 */
function getViewStatus(status) {
  return ['4', '8', '9'].includes(String(status).trim())
}

/**
 * 将质量规则执行结果转换为标签文本。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getQualityResult(row) {
  if (!row.executed) return {label: '未执行', type: 'info'}
  return row.passed ? {label: '通过', type: 'success'} : {label: '不通过', type: 'danger'}
}

/**
 * 在质量不通过的规则说明后追加返回字段合并修改的操作指引。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getQualityExplanation(row) {
  const message = String(row.message || '').trim()
  if (!isQualityFailed.value || !row.executed || row.passed) return message || '-'
  const guidance = '质量检测未通过，返回“字段合并”修改后重新确认'
  return message ? `${message}，${guidance}` : guidance
}

/**
 * 统一文本格式后比较两个取值。
 * @param {*} left 用于比较的左侧值。
 * @param {*} right 用于比较的右侧值。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isSameText(left, right) {
  return String(left ?? '').trim() === String(right ?? '').trim()
}

/**
 * 将布尔值及其“是/否”文案归一后用于字段值比较。
 * @param {*} row 当前业务字段。
 * @param {*} value 待比较的字段值。
 * @returns {string} 规范化后的字段值。
 */
function normalizeFieldValue(row, value) {
  const text = String(value ?? '').trim()
  if (!['BOOLEAN', 'BOOL'].includes(String(row.dataType || '').toUpperCase())) return text
  const normalized = text.toLowerCase()
  if (['true', '1', '是', 'yes', 'y'].includes(normalized)) return 'true'
  if (['false', '0', '否', 'no', 'n'].includes(normalized)) return 'false'
  return text
}

/**
 * 按字段类型比较两个值，兼容布尔值的展示文案。
 * @param {*} row 当前业务字段。
 * @param {*} left 用于比较的左侧值。
 * @param {*} right 用于比较的右侧值。
 * @returns {boolean} 两个字段值是否相同。
 */
function isSameFieldValue(row, left, right) {
  return normalizeFieldValue(row, left) === normalizeFieldValue(row, right)
}

/**
 * 将相同候选值分组并保留其来源信息。
 * @param {*} row 当前业务记录。
 * @returns {Array} 整理后的业务记录集合。
 */
function getUniqueValues(row) {
  const grouped = new Map()
  ;(row.candidates || []).forEach((item) => {
    const value = item.value ?? ''
    const key = `value:${normalizeFieldValue(row, value)}`
    if (!grouped.has(key)) {
      grouped.set(key, {
        key,
        value,
        systems: [],
        sourceVersionId: item.sourceVersionId,
        recommended: isSameFieldValue(row, value, row.recommendedValue) && String(row.recommendedValue ?? '').trim() !== ''
      })
    }
    const bucket = grouped.get(key)
    const name = item.systemName || item.systemCode || '来源'
    if (!bucket.systems.includes(name)) bucket.systems.push(name)
    if (String(item.sourceVersionId || '') === String(row.sourceVersionId || '')) bucket.sourceVersionId = item.sourceVersionId
  })
  return [...grouped.values()]
}

/**
 * 判断字段是否为系统管理字段。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isSystemField(row) {
  return String(row.columnCategory || '').toUpperCase() === 'SYSTEM'
}

/**
 * 来源数据按编号升降序排列，未排序时保持来源原始顺序。
 * @param {Object} options 组件回调的字段、排序或选择参数。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleSourceSortChange({prop, order}) {
  if (prop !== 'sourceIndex') return
  sourceIndexSort.value = order
  sourcePage.pageNum = 1
}

/**
 * 结果确认字段按编号升降序排列，未排序时保持字段原始顺序。
 * @param {Object} options 组件回调的字段、排序或选择参数。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function handleResultFieldSortChange({orderByColumn, isAsc}) {
  if (orderByColumn !== 'resultFieldIndex') return
  resultFieldIndexSort.value = isAsc
}

/**
 * 按字段名称或路径匹配原型优先展示的来源字段。
 * @param {*} aliases 可识别的字段名称或编码。
 * @param {*} excludedFields 不参与匹配的字段集合。
 * @returns {*} 当前业务计算结果。
 */
function findSourceBusinessField(aliases, excludedFields = []) {
  const normalizedAliases = aliases.map(normalizeSourceFieldName)
  return visibleFields.value.find((field) => {
    if (field === sourceNameField.value || excludedFields.includes(field)) return false
    const names = [field.columnName, field.fieldName, field.name, field.fieldPath, field.code, field.columnCode]
        .filter(Boolean)
        .map(normalizeSourceFieldName)
    return names.some((name) => normalizedAliases.some((alias) => name.includes(alias)))
  })
}

/**
 * 统一字段名称格式，兼容下划线、空格及中英文大小写差异。
 * @param {*} value 待处理的字段值或状态编码。
 * @returns {string} 用于页面展示的格式化文本。
 */
function normalizeSourceFieldName(value) {
  return String(value || '').toLowerCase().replace(/[\s_\-（）()]/g, '')
}

/**
 * 获取当前来源行对应业务字段的原始值。
 * @param {*} row 当前业务记录。
 * @param {*} column 模型列定义。
 * @returns {*} 当前业务计算结果。
 */
function getSourceColumnValue(row, column) {
  const value = column.useBusinessName ? row.businessName
    : column.fieldPath ? row.values[column.fieldPath] : undefined
  return formatMergeDate(value, column.dataType)
}

/**
 * 按字段类型格式化日期显示，无法解析时保留原值。
 * @param {*} value 原始日期值，支持日期字符串、秒级或毫秒级时间戳。
 * @param {string} dataType 模型字段类型。
 * @returns {*} 日期文本或无需格式化的原值，使用浏览器本地时区。
 */
function formatMergeDate(value, dataType) {
  const type = String(dataType || '').toUpperCase()
  if (!['DATE', 'DATETIME', 'TIMESTAMP'].includes(type) || value == null || value === '') return value
  let date
  if (typeof value === 'number' || /^\d{10,13}$/.test(String(value))) {
    const numeric = Number(value)
    date = new Date(String(value).length === 10 ? numeric * 1000 : numeric)
  } else {
    date = new Date(value)
  }
  if (Number.isNaN(date.getTime())) return value
  const pad = (part) => String(part).padStart(2, '0')
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  if (type === 'DATE') return day
  return `${day} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

/**
 * 判断字段是否应由自动编码规则生成。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isAutoGeneratedField(row) {
  return row.autoGenerated === true || isSystemField(row)
}

/**
 * 按系统推荐与字段冲突结果显示字段合并状态，不把人工当前选值误当作系统推荐。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getFieldMergeStatus(row) {
  if (isAutoGeneratedField(row)) return '自动生成'
  if (String(row.conflictFlag) === '1' || !String(row.recommendedValue ?? '').trim()) return '需确认'
  return '已推荐'
}

/**
 * 推荐结果按字段值保存；同值候选只在首个来源位置展示一次推荐标记。
 * @param {*} row 当前业务记录。
 * @param {*} candidate 来源候选值。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isRecommendedCandidate(row, candidate) {
  if (isAutoGeneratedField(row) || candidate == null || !String(row.recommendedValue ?? '').trim()) return false
  return candidate === (row.candidates || []).find((item) => isSameFieldValue(row, item.value, row.recommendedValue))
}

/**
 * 生成字段最终取值的来源说明。
 * @param {*} row 当前业务记录。
 * @returns {string} 用于页面展示的格式化文本。
 */
function getFinalSourceLabel(row) {
  if (isAutoGeneratedField(row)) return String(row.draftValue ?? '').trim() ? '系统生成' : '-'
  if (row.pick === 'manual' || String(row.draftType) === '5') return '人工录入'
  const candidates = (row.candidates || []).filter((item) => isSameFieldValue(row, item.value, row.draftValue))
  const sourceVersionId = row.draftSourceVersionId || row.sourceVersionId
  const selectedCandidate = candidates.find((item) => sourceVersionId && String(item.sourceVersionId || '') === String(sourceVersionId))
      || candidates[0]
  if (selectedCandidate) return selectedCandidate.systemCode || selectedCandidate.systemName || '-'
  if (isSameFieldValue(row, row.draftValue, row.recommendedValue)) return '系统推荐'
  return '-'
}

/**
 * 判断当前字段取值是否需要填写调整原因。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isReasonNeeded(row) {
  if (!row.editable) return false
  if (row.pick === 'manual') return true
  const selectedCandidate = (row.candidates || []).find((candidate) => getCandidateKey(candidate) === row.pick)
  if (selectedCandidate) return !isRecommendedCandidate(row, selectedCandidate)
  if (!String(row.draftValue ?? '').trim()) return false
  return !isSameFieldValue(row, row.draftValue, row.recommendedValue)
}

/**
 * 判断所选值是否仍为系统推荐值。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isSystemRecommendedPicked(row) {
  if (row.pick === 'manual') return false
  const selectedCandidate = (row.candidates || []).find((candidate) => getCandidateKey(candidate) === row.pick)
  return selectedCandidate ? isRecommendedCandidate(row, selectedCandidate) : isSameFieldValue(row, row.draftValue, row.recommendedValue)
}

/**
 * 读取字段系统推荐取值的来源类型。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getSystemType(row) {
  if (row.survivorRuleId) return '2'
  if (String(detail.value?.processType) === '1') return '1'
  if (row.valueSourceType && !['4', '5'].includes(String(row.valueSourceType))) return String(row.valueSourceType)
  return '3'
}

/**
 * 根据已保存的取值恢复字段初始选择。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getInitialPick(row) {
  const value = row.finalValue ?? ''
  const sourceVersionId = row.sourceVersionId || row.draftSourceVersionId
  if (String(row.valueSourceType) === '5') return 'manual'
  const candidates = row.candidates || []
  const matched = candidates.find((item) => sourceVersionId
    && String(item.sourceVersionId || '') === String(sourceVersionId)
    && isSameFieldValue(row, item.value, value))
    || candidates.find((item) => isSameFieldValue(row, item.value, value))
  if (matched) return getCandidateKey(matched)
  if (String(value).trim()) return 'manual'
  return ''
}

/**
 * 使用来源版本作为字段合并单元格的稳定标识。
 * @param {*} candidate 来源候选值。
 * @returns {*} 当前业务计算结果。
 */
function getCandidateKey(candidate) {
  if (!candidate) return ''
  const identity = candidate.sourceVersionId ?? candidate.sourceRecordId ?? candidate.stableRecordId
  return identity == null ? (candidate.systemCode || candidate.systemName || '') + ':' + (candidate.businessName || '') : 'source:' + identity
}

/**
 * 查找当前来源列在指定字段上的候选值。
 * @param {*} row 当前业务记录。
 * @param {*} sourceColumn 来源数据列定义。
 * @returns {*} 当前业务计算结果。
 */
function getSourceCandidate(row, sourceColumn) {
  return (row.candidates || []).find((candidate) => getCandidateKey(candidate) === sourceColumn.key)
}

/**
 * 选择某个来源列的候选值，并记录对应来源版本。
 * @param {*} row 当前业务记录。
 * @param {*} candidate 来源候选值。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function selectSourceCandidate(row, candidate) {
  if (!candidate || !row.editable || !canAdjust.value) return
  row.pick = getCandidateKey(candidate)
  changePick(row)
}

/**
 * 选择“其他值”后清空候选来源，等待用户输入自定义值。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function selectManualValue(row) {
  if (row.editable && canAdjust.value && !isAutoGeneratedField(row) && row.pick !== 'manual') {
    row.pick = 'manual'
    changePick(row)
  }
}

/**
 * 将接口字段转换为草稿状态；待处理首次打开采用系统推荐，无推荐时要求人工选择，已保存的人工选择保持不变。
 * @param {*} fields 字段列表。
 * @param {boolean} preferSystemRecommendation 是否优先采用系统推荐值。
 * @returns {Array} 整理后的业务记录集合。
 */
function createEditFields(fields, preferSystemRecommendation = false) {
  return (fields || []).map((field) => {
    const savedPick = getInitialPick(field)
    const hasManualOverride = ['1', 'true', 'y', 'yes'].includes(String(field.manualOverrideFlag).toLowerCase())
    const recommendedCandidate = preferSystemRecommendation && !hasManualOverride
        ? (field.candidates || []).find((candidate) => isRecommendedCandidate(field, candidate))
        : null
    const requiresUserSelection = preferSystemRecommendation
        && !hasManualOverride
        && !recommendedCandidate
        && !isAutoGeneratedField(field)
    const pick = recommendedCandidate
      ? getCandidateKey(recommendedCandidate)
      : requiresUserSelection ? '' : savedPick
    const draftValue = recommendedCandidate
      ? recommendedCandidate.value ?? ''
      : requiresUserSelection ? '' : field.finalValue ?? ''
    // 已保存的系统推荐按值恢复基线，同值来源的位置变化不属于人工调整。
    const initialPick = recommendedCandidate && isSameFieldValue(field, draftValue, field.finalValue)
      ? pick
      : savedPick
    const draftType = recommendedCandidate ? getSystemType(field) : field.valueSourceType
    const draftSourceVersionId = recommendedCandidate || requiresUserSelection ? null : field.sourceVersionId
    return {
      ...field,
      pick,
      initialPick,
      requiresUserSelection,
      draftValue,
      draftReason: '',
      draftSourceVersionId,
      draftType,
    }
  })
}

/**
 * 根据来源选择同步草稿值、取值类型和调整原因。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function changePick(row) {
  if (row.pick === 'manual') {
    row.draftType = '5'
    row.draftSourceVersionId = null
    if (getUniqueValues(row).some((item) => isSameText(item.value, row.draftValue))) row.draftValue = ''
    syncReason(row)
    return
  }
  const matched = (row.candidates || []).find((item) => getCandidateKey(item) === row.pick)
  if (!matched) return
  row.draftValue = matched.value ?? ''
  if (isRecommendedCandidate(row, matched)) {
    row.draftType = getSystemType(row)
    row.draftSourceVersionId = null
    syncReason(row)
    return
  }
  row.draftType = '4'
  row.draftSourceVersionId = matched.sourceVersionId
  syncReason(row)
}

/**
 * 读取当前取值对应的调整原因。
 * @param {*} row 当前业务记录。
 * @returns {*} 当前业务计算结果。
 */
function getCurrentReason(row) {
  return isReasonNeeded(row) ? row.draftReason : ''
}

/**
 * 判断当前字段是否允许编辑调整原因。
 * @param {*} row 当前业务记录。
 * @returns {boolean} 是否满足当前业务条件。
 */
function canEditReason(row) {
  return isReprocessMode.value && hasFieldValueChanges(row) && isReasonNeeded(row)
}

/**
 * 取值变化时同步调整原因的编辑状态。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function syncReason(row) {
  if (!hasFieldValueChanges(row) || !isReasonNeeded(row)) {
    row.draftReason = ''
  }
}

/**
 * 判断本次是否修改了已保存的字段取值或来源。
 * @param {*} row 当前业务字段。
 * @returns {boolean} 是否存在字段调整。
 */
function hasFieldValueChanges(row) {
  return Boolean(row.editable) && (row.pick !== row.initialPick
    || !isSameFieldValue(row, row.draftValue, row.finalValue))
}

/**
 * 统一字段保存与未保存拦截的判断，忽略不可编辑字段及布尔展示差异。
 * @param {*} row 当前业务字段。
 * @returns {boolean} 是否存在需要保存的调整。
 */
function hasFieldChanges(row) {
  if (!row.editable) return false
  return hasFieldValueChanges(row)
    || (canEditReason(row) && !isSameText(getCurrentReason(row), row.adjustReason))
}

/**
 * 判断合并字段是否存在尚未保存的变化。
 * @returns {boolean} 是否满足当前业务条件。
 */
function isDirty() {
  return editFields.value.some(hasFieldChanges)
}

/**
 * 校验可编辑且非自动生成的整数字段，与后端 BIGINT 范围保持一致。
 * 空值由必填规则处理，使用 BigInt 避免大整数精度丢失。
 * @author chentangwen
 * @param {Object} row 字段行，包含 draftValue、dataType、autoGenerated 和 editable。
 * @returns {string} 校验失败提示，通过或不适用时返回空字符串。
 */
function mergeFieldValueError(row) {
  const value = String(row.draftValue ?? '').trim()
  if (!value || row.autoGenerated || row.editable === false) return ''
  if (!['INTEGER', 'INT', 'BIGINT', 'SMALLINT', 'TINYINT'].includes(String(row.dataType || '').toUpperCase())) return ''
  if (!/^[+-]?[0-9]+$/.test(value)) return '请输入有效整数'
  const integer = BigInt(value)
  return integer < -9223372036854775808n || integer > 9223372036854775807n ? '整数超出允许范围' : ''
}

/**
 * 校验字段取值、冲突状态及人工调整原因。
 * @returns {boolean} 是否满足当前业务条件。
 */
function validateMergeFields() {
  for (const row of editFields.value) {
    if (!row.editable) continue
    const valueError = mergeFieldValueError(row)
    if (valueError) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath}：${valueError}`)
      return false
    }
    if (row.requiresUserSelection && !row.pick) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 请先确认合并取值`)
      return false
    }
    if (row.conflictFlag === '1' && !String(row.draftValue ?? '').trim()) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 的字段冲突尚未处理`)
      return false
    }
    if (row.pick === 'manual' && !String(row.draftValue ?? '').trim()) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 请填写合并后的值`)
      return false
    }
    if (canEditReason(row) && !String(row.draftReason || '').trim()) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 请填写调整原因`)
      return false
    }
  }
  return true
}

/**
 * 根据记录当前状态确定向导初始步骤。
 * @param {*} status 业务处理状态。
 * @returns {*} 当前业务计算结果。
 */
function getInitialStep(status) {
  const value = String(status ?? '').trim()
  if (getViewStatus(value)) return 4
  if (['2', '5', '6', '7'].includes(value)) return 2
  return 1
}

/**
 * 校验当前步骤后切换合并向导。
 * @param {*} step 目标向导步骤。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function goStep(step) {
  if (acting.value || detailLoading.value || !detail.value) return
  if (step <= wizardStep.value) {
    wizardStep.value = step
    return
  }
  if (wizardStep.value === 3 && canConfirm.value) return
  // 向前切换只校验草稿，字段在结果确认时保存。
  if (step === wizardStep.value + 1) await goNextStep()
}

/**
 * 校验当前字段草稿后进入下一步预览。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function goNextStep() {
  if (acting.value || detailLoading.value || !detail.value) return
  if (wizardStep.value === 2 && canAdjust.value) {
    if (!validateMergeFields()) return
  }
  if (wizardStep.value < 4) wizardStep.value += 1
}

/**
 * 打开当前合并记录的只读详情。
 * @param {*} row 当前业务记录。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function openReadonlyDetail(row) {
  return router.push({path: '/merge/manage/detail', query: {id: row.id, view: '1'}})
}

/**
 * 携带开始或重新处理模式打开合并向导。
 * @param {*} row 当前业务记录。
 * @param {*} processingMode 本次处理模式。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function openProcess(row, processingMode) {
  const mode = resolveProcessingMode(getRowStatus(row), processingMode, row?.qualityStatus)
  const step = mode === 'reprocess' ? 2 : getInitialStep(getRowStatus(row))
  return router.push({path: '/merge/manage/detail', query: {id: row.id, step, processingMode: mode}})
}

/**
 * 加载当前合并结果详情及其展示数据。
 * @param {*} id 业务记录编号。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function loadDetail(id) {
  detailLoading.value = true
  try {
    const res = await getMergePending(id)
    if (route.path !== '/merge/manage/detail' || String(route.query.id) !== String(id)) return
    detail.value = res.data
    detailStatus.value = String(res.data?.status ?? '')
    const processingMode = resolveProcessingMode(res.data?.status, route.query.processingMode, res.data?.qualityStatus)
    isReprocessMode.value = processingMode === 'reprocess'
    // 仅未确认的记录初始化推荐值，已确认结果须按保存值恢复，避免质检后产生未保存草稿。
    const preferSystemRecommendation = processingMode === 'start' && String(res.data?.status) === '0'
    editFields.value = createEditFields(res.data?.fields || [], preferSystemRecommendation)
    reason.value = ''
  } catch {
    if (route.path === '/merge/manage/detail' && String(route.query.id) === String(id)) closeDetail()
  } finally {
    detailLoading.value = false
  }
}

/**
 * 确认合并时提交字段取值、来源及本轮调整原因。
 * @returns {Promise<boolean>} 是否满足当前业务条件。
 */
async function handleAdjust() {
  if (!validateMergeFields()) return false
  const fields = []
  for (const row of editFields.value) {
    if (!hasFieldChanges(row)) continue
    if (row.conflictFlag === '1' && !String(row.draftValue ?? '').trim()) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 的字段冲突尚未处理`)
      return false
    }
    if (canEditReason(row) && !String(row.draftReason || '').trim()) {
      proxy.$modal.msgWarning(`${row.columnName || row.fieldPath} 请填写调整原因`)
      return false
    }
    const useSourceDefault = isSystemRecommendedPicked(row)
    fields.push({
      fieldId: row.id,
      finalValue: row.draftValue,
      valueSourceType: useSourceDefault ? getSystemType(row) : row.draftType,
      sourceVersionId: !useSourceDefault && row.draftType === '4' ? row.draftSourceVersionId : undefined,
      systemRecommended: useSourceDefault,
      adjustReason: isReprocessMode.value && !useSourceDefault ? String(row.draftReason || '').trim() : undefined,
    })
  }
  if (!fields.length) {
    proxy.$modal.msgWarning('没有需要保存的调整')
    return false
  }
  try {
    await adjustMergeField({
      pendingId: detail.value.id,
      processingMode: isReprocessMode.value ? 'reprocess' : 'start',
      fields,
    })
    return true
  } catch {
    return false
  }
}

/**
 * 校验并确认当前合并结果。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleConfirm() {
  if (acting.value || detailLoading.value || !detail.value || !canConfirm.value) return
  if (!validateMergeFields()) return
  try {
    acting.value = true
    await proxy.$modal.confirm('确认当前合并结果？确认后会保存字段并自动执行质量检测。')
    if (isDirty()) {
      const saved = await handleAdjust()
      if (!saved) return
    }
    await confirmMergePending({pendingId: detail.value.id, reason: reason.value})
    await loadDetail(detail.value.id)
    wizardStep.value = 4
    tableRef.value?.getList()
    const passed = String(detail.value?.qualityStatus) === '4'
    if (passed) proxy.$modal.msgSuccess('结果已确认，质量检测通过')
  } catch {
    // 取消或接口失败时保留当前结果。
  } finally {
    acting.value = false
  }
}

/**
 * 执行合并结果的质量检测并刷新详情。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleQuality() {
  if (isDirty()) {
    proxy.$modal.msgWarning('请先在结果确认中点击确认合并')
    return
  }
  try {
    acting.value = true
    await qualityMergePending({pendingId: detail.value.id, reason: reason.value})
    await loadDetail(detail.value.id)
    wizardStep.value = 4
    tableRef.value?.getList()
    const passed = String(detail.value?.qualityStatus) === '4'
    if (passed) proxy.$modal.msgSuccess('质量检测通过')
  } catch {
    // 接口失败时保留当前结果。
  } finally {
    acting.value = false
  }
}

/**
 * 从质量提交步骤返回字段合并，供用户修正未通过的字段。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function returnToFieldMerge() {
  wizardStep.value = 2
}

/**
 * 提交已通过质量检测的合并结果审批。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleSubmit() {
  if (isDirty()) {
    proxy.$modal.msgWarning('请先在结果确认中点击确认合并')
    return
  }
  try {
    await proxy.$modal.confirm(isEditableStoppedStatus(detail.value.status) ? '确认重新提交审批？' : '确认提交审批？')
    acting.value = true
    await submitMergePending({pendingId: detail.value.id, reason: reason.value})
    proxy.$modal.msgSuccess('已提交审批')
    await loadDetail(detail.value.id)
    tableRef.value?.getList()
  } catch {
    // 取消或接口失败时保留当前结果。
  } finally {
    acting.value = false
  }
}

/**
 * 返回当前业务模块列表。
 * @returns {Promise<*>} 路由跳转的执行结果。
 */
function closeDetail() {
  return router.push('/merge/manage')
}

/**
 * 打开当前合并记录的审批查看弹窗。
 * @param {*} row 当前业务记录。
 * @returns {void} 通过状态或事件更新页面，无返回值。
 */
function openApproval(row) {
  approvalDialogRef.value?.open(row)
}

/**
 * 确认审批操作后调用接口并刷新当前列表。
 * @param {*} message 操作前的确认提示。
 * @param {*} successMessage 操作成功后的提示。
 * @param {*} request 执行本次业务操作的接口函数。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function confirmApprovalAction(message, successMessage, request, row) {
  try {
    await ElMessageBox.confirm(message, '提示', {type: 'warning'})
  } catch {
    return
  }
  await request({pendingId: row.id})
  ElMessage.success(successMessage)
  await tableRef.value?.getList?.()
}

/**
 * 确认后撤回合并结果审批。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleWithdraw(row) {
  await confirmApprovalAction('确认撤回该合并审批任务吗？', '审批任务已撤回', withdrawMergePending, row)
}

/**
 * 从列表提交或重新提交当前合并记录。
 * @param {*} row 当前业务记录。
 * @returns {Promise<void>} 通过状态或事件更新页面，无返回值。
 */
async function handleSubmitApproval(row) {
  const status = getRowStatus(row)
  const request = ['6', '7'].includes(status) ? resubmitMergePending : submitMergePending
  await confirmApprovalAction('确认提交该合并审批吗？', '已提交审批', request, row)
}

watch(() => route.fullPath, async () => {
  if (props.detailPage || route.path !== '/merge/manage') return
  tableStore.params.groupNo = route.query.groupNo || route.query.keyword || undefined
  tableStore.params.businessName = route.query.businessName || undefined
  tableStore.params.taskName = route.query.taskName || undefined
  tableStore.params.transferTimeRange = []
  tableStore.params.modelId = parseQueryNumber(route.query.modelId)
  handleModelSelect(tableStore.params.modelId)
  tableStore.params.status = route.query.status || '*'
  await nextTick()
  tableRef.value?.getList()
})

watch(wizardStep, async (step) => {
  if (step !== 2) return
  await nextTick()
  mergeFieldTableRef.value?.elTableRef?.doLayout()
}, {flush: 'post'})

watch(() => [route.query.id, route.query.step, route.query.view, route.query.processingMode], async ([id]) => {
  if (!props.detailPage || route.path !== '/merge/manage/detail') return
  if (!id) {
    await closeDetail()
    return
  }
  detail.value = null
  detailStatus.value = ''
  sourcePage.pageNum = 1
  sourceIndexSort.value = null
  resultFieldIndexSort.value = null
  detailActiveTab.value = 'result'
  detailLoading.value = true
  try {
    if (!modelOptions.value.length) modelOptions.value = await loadPublishedModels()
  } catch {
    // 模型名称降级为编号。
  }
  await loadDetail(id)
  if (route.path !== '/merge/manage/detail' || String(route.query.id) !== String(id)) return
  const step = parseQueryNumber(route.query.step)
  wizardStep.value = step >= 1 && step <= 4 ? step : getInitialStep(detail.value?.status)
  if (wizardStep.value === 2) {
    await nextTick()
    mergeFieldTableRef.value?.elTableRef?.doLayout()
  }
}, {immediate: true})
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

.merge-list-main {
  --qt-merge-transfer-search-width: 320px;
  min-width: 0;
}

.merge-list-main :deep(.merge-transfer-time-item) {
  width: var(--qt-merge-transfer-search-width);
}

.merge-detail-model .qt-table-info__main {
  flex-wrap: wrap;
  margin-bottom: 0;
}

.merge-manage-table {
  min-height: calc(100vh - 320px);
}

.merge-value-select {
  width: 100%;
}

.merge-section-title {
  min-height: var(--qt-control-height);
}

:deep(.merge-tip-danger) {
  color: var(--qt-status-danger);
}

:deep(.merge-tip-success) {
  color: var(--qt-status-success);
}

:deep(.merge-tip-warning) {
  color: var(--qt-status-warning);
}

.merge-detail-loading {
  min-height: calc(100vh - 110px);
}

.merge-result-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 110px);
}

.merge-step-card {
  display: flex;
  align-items: center;
  min-height: 100px;
  box-sizing: border-box;
  padding: 20px 140px;
  margin-bottom: var(--qt-space-4);
  background: var(--qt-surface-background);
}

.merge-step-card :deep(.steps-inner) {
  width: 100%;
}

.merge-step-card :deep(.zl-step) {
  height: 40px;
  margin: 0 auto;
  align-items: center;
}

.merge-config-card {
  position: relative;
  min-height: max(74vh, 480px);
  padding: 15px calc(var(--qt-space-5) * 2) 90px;
  background: var(--qt-surface-background);
  box-sizing: border-box;
}

:deep(.merge-effective-label) {
  width: 138px;
  font-weight: var(--qt-font-weight-regular) !important;
}

:deep(.merge-effective-info .el-descriptions__table) {
  table-layout: fixed;
}

.button-style {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 25px calc(var(--qt-space-5) * 2);
  background: var(--qt-surface-background);
  text-align: right;
  z-index: 10;
}

.merge-quality-actions :deep(.el-button) {
  width: calc(var(--qt-font-size-base) * 6 + var(--qt-space-4) * 2);
  height: var(--qt-control-height);
  font-family: var(--qt-font-family);
  font-size: var(--qt-font-size-base);
  font-weight: var(--qt-font-weight-regular);
}

.merge-quality-actions :deep(.el-button > span) {
  font-family: var(--qt-font-family);
  font-size: var(--qt-font-size-base);
  font-weight: var(--qt-font-weight-regular);
  line-height: 1;
  letter-spacing: normal;
}

@media (max-width: 900px) {
  .merge-step-card {
    padding: var(--qt-space-5) var(--qt-space-3);
    overflow-x: auto;
  }
}

.merge-operation-button .el-icon {
  margin-right: var(--qt-space-1);
  vertical-align: -2px;
}

.merge-operation-svg-icon {
  display: inline-block;
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.merge-operation-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: var(--qt-space-1);
  white-space: nowrap;
}

.merge-operation-actions .el-button + .el-button {
  margin-left: 0;
}

:deep(.merge-business-name-column .cell) {
  white-space: nowrap;
  word-break: keep-all;
}
</style>
