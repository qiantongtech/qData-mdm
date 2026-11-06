<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This document is part of the qData Master Data Platform (open source version).

  qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
  You may use qData for commercial purposes, but you may not remove, hide,
  modify, or replace the qData master data platform logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData master data platform as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div ref="app-container" class="app-container gen-edit-page">
    <div class="model-form-header">
      <div class="model-form-header-info">
        <div class="model-form-title h2-titles">
          {{ td('sys.tool.genIndex.edit') }}
          <span v-if="info.tableComment || info.tableName">
            · {{ info.tableComment || info.tableName }}</span
          >
        </div>
        <el-text v-if="info.tableName" type="info">{{ info.tableName }}</el-text>
      </div>
    </div>

    <div class="wizard-steps mb15">
      <el-steps :active="currentStep" finish-status="success" align-center>
        <el-step v-for="step in stepsList" :key="step.id" :title="step.title" />
      </el-steps>
    </div>

    <div class="pagecont-top">
      <div class="infotop">
        <div class="main">
          <section v-show="currentStep === 0" class="wizard-section basic-section">
            <div class="h2-titles">{{ td('sys.tool.genEdit.basicInfo') }}</div>
            <basic-info-form ref="basicInfo" :info="info" />
          </section>

          <section v-show="currentStep === 1" class="wizard-section field-section">
            <div class="h2-titles">{{ td('sys.tool.genEdit.fieldInfo') }}</div>
            <qt-table v-bind="tableStore0" ref="qtTableRef0">
              <template #columnName="{ row }">{{ row.columnName }}</template>
              <template #cell2="scope">
                <el-input v-model="scope.row.columnComment" />
              </template>
              <template #columnType="{ row }">{{ row.columnType }}</template>
              <template #cell4="scope">
                <el-select v-model="scope.row.javaType">
                  <el-option label="Long" value="Long" />
                  <el-option label="String" value="String" />
                  <el-option label="Integer" value="Integer" />
                  <el-option label="Double" value="Double" />
                  <el-option label="BigDecimal" value="BigDecimal" />
                  <el-option label="Date" value="Date" />
                  <el-option label="Boolean" value="Boolean" />
                </el-select>
              </template>
              <template #cell5="scope">
                <el-input v-model="scope.row.javaField" />
              </template>
              <template #cell6="scope">
                <el-checkbox true-label="1" false-label="0" v-model="scope.row.isEdit" />
              </template>
              <template #cell7="scope">
                <el-checkbox true-label="1" false-label="0" v-model="scope.row.isList" />
              </template>
              <template #cell8="scope">
                <el-checkbox true-label="1" false-label="0" v-model="scope.row.isQuery" />
              </template>
              <template #cell9="scope">
                <el-select v-model="scope.row.queryType">
                  <el-option label="=" value="EQ" />
                  <el-option label="!=" value="NE" />
                  <el-option label=">" value="GT" />
                  <el-option label=">=" value="GTE" />
                  <el-option label="<" value="LT" />
                  <el-option label="<=" value="LTE" />
                  <el-option label="LIKE" value="LIKE" />
                  <el-option label="BETWEEN" value="BETWEEN" />
                </el-select>
              </template>
              <template #cell10="scope">
                <el-checkbox true-label="1" false-label="0" v-model="scope.row.isRequired" />
              </template>
              <template #cell11="scope">
                <el-select v-model="scope.row.htmlType">
                  <el-option :label="td('sys.tool.genEdit.textbox')" value="input" />
                  <el-option :label="td('sys.tool.genEdit.textarea')" value="textarea" />
                  <el-option :label="td('sys.tool.genEdit.dropdown')" value="select" />
                  <el-option :label="td('sys.tool.genEdit.radio')" value="radio" />
                  <el-option :label="td('sys.tool.genEdit.checkbox')" value="checkbox" />
                  <el-option :label="td('sys.tool.genEdit.datePicker')" value="datetime" />
                  <el-option :label="td('sys.tool.genEdit.imageUpload')" value="imageUpload" />
                  <el-option :label="td('sys.tool.genEdit.fileUpload')" value="fileUpload" />
                  <el-option :label="td('sys.tool.genEdit.richText')" value="editor" />
                </el-select>
              </template>
              <template #action="scope">
                <el-select
                  v-model="scope.row.dictType"
                  clearable
                  filterable
                  :placeholder="td('sys.tool.genEdit.selectPlaceholder')"
                >
                  <el-option
                    v-for="dict in dictOptions"
                    :key="dict.dictType"
                    :label="dict.dictName"
                    :value="dict.dictType"
                  >
                    <span style="float: left">{{ dict.dictName }}</span>
                    <span style="float: right; color: #8492a6; font-size: 13px">{{
                      dict.dictType
                    }}</span>
                  </el-option>
                </el-select>
              </template>
            </qt-table>
          </section>

          <section v-show="currentStep === 2" class="wizard-section gen-section">
            <div class="h2-titles">{{ td('sys.tool.genEdit.genInfo') }}</div>
            <gen-info-form ref="genInfo" :info="info" :tables="tables" />
          </section>
        </div>
      </div>

      <div class="button-style data-actions">
        <el-button @click="close()">{{ td('common.button.back') }}</el-button>
        <el-button :disabled="currentStep === 0" @click="handlePrevStep">
          {{ td('common.button.previousStep') }}
        </el-button>
        <el-button
          v-show="currentStep < stepsList.length - 1"
          type="primary"
          @click="handleNextStep"
        >
          {{ td('common.button.nextStep') }}
        </el-button>
        <el-button
          v-show="currentStep === stepsList.length - 1"
          type="primary"
          @click="submitForm()"
        >
          {{ td('sys.tool.genEdit.submit') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup name="GenEdit">
import { reactive, computed, watch, ref, unref, isRef } from 'vue';

import { getGenTable, updateGenTable } from '@/api/system/tool/gen.js';
import { optionselect as getDictOptionselect } from '@/api/system/system/dict/type.js';
import basicInfoForm from './basicInfoForm.vue';
import genInfoForm from './genInfoForm.vue';
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const route = useRoute();
const { proxy } = getCurrentInstance();

const currentStep = ref(0);
const stepsList = computed(() => [
  { id: 0, title: td('sys.tool.genEdit.basicInfo') },
  { id: 1, title: td('sys.tool.genEdit.fieldInfo') },
  { id: 2, title: td('sys.tool.genEdit.genInfo') }
]);

function handleNextStep() {
  currentStep.value = Math.min(currentStep.value + 1, stepsList.value.length - 1);
}

function handlePrevStep() {
  currentStep.value = Math.max(currentStep.value - 1, 0);
}

const tableHeight = ref(document.documentElement.scrollHeight - 245 + 'px');
const tables = ref([]);
const columns = ref([]);
const dictOptions = ref([]);
const info = ref({});

/** submit button */
function submitForm() {
  const basicForm = proxy.$refs.basicInfo.$refs.basicInfoForm;
  const genForm = proxy.$refs.genInfo.$refs.genInfoForm;
  Promise.all([basicForm, genForm].map(getFormPromise)).then((res) => {
    const validateResult = res.every((item) => !!item);
    if (validateResult) {
      const genTable = Object.assign({}, info.value);
      genTable.columns = columns.value;
      genTable.params = {
        treeCode: info.value.treeCode,
        treeName: info.value.treeName,
        treeParentCode: info.value.treeParentCode,
        parentMenuId: info.value.parentMenuId
      };
      updateGenTable(genTable).then((res) => {
        proxy.$modal.msgSuccess(res.msg);
        if (res.code === 200) {
          close();
        }
      });
    } else {
      proxy.$modal.msgError(td('sys.tool.genEdit.formValidationFailed'));
    }
  });
}

function getFormPromise(form) {
  return new Promise((resolve) => {
    form.validate((res) => {
      resolve(res);
    });
  });
}

function close() {
  const obj = { path: '/tool/gen', query: { t: Date.now(), pageNum: route.query.pageNum } };
  proxy.$tab.closeOpenPage(obj);
}

(() => {
  const tableId = route.params && route.params.tableId;
  if (tableId) {
    // Get table details
    getGenTable(tableId).then((res) => {
      columns.value = res.data.rows;
      info.value = res.data.info;
      tables.value = res.data.tables;
    });
    /** Query dictionary drop-down list */
    getDictOptionselect().then((response) => {
      dictOptions.value = response.data;
    });
  }
})();

const qtTableRef0 = ref(null);

const tableStore0 = reactive({
  config: computed(() => ({
    notPagination: true,
    table: {
      defaultSort: { prop: 'columnId', order: 'descending' },
      stripe: true,
      height: '650px',
      rowKey: 'columnId',
      maxHeight: tableHeight
    }
  })),
  columns: [
    {
      sortableKey: 'column_id',
      prop: 'columnId',
      sortable: true,
      get label() {
        return td('sys.tool.genEdit.index');
      },
      type: 'index',
      width: 60
    },
    {
      get label() {
        return td('sys.tool.genEdit.fieldColumnName');
      },
      width: '150',
      showOverflowTooltip: true,
      slot: 'columnName',
      align: 'left'
    },
    {
      get label() {
        return td('sys.tool.genEdit.fieldDesc');
      },
      minWidth: '150',
      align: 'center',
      slot: 'cell2'
    },
    {
      get label() {
        return td('sys.tool.genEdit.physicalType');
      },
      width: '150',
      showOverflowTooltip: true,
      align: 'center',
      slot: 'columnType'
    },
    {
      get label() {
        return td('sys.tool.genEdit.javaType');
      },
      width: '150',
      align: 'center',
      slot: 'cell4'
    },
    {
      get label() {
        return td('sys.tool.genEdit.javaProperty');
      },
      width: '150',
      align: 'center',
      slot: 'cell5'
    },
    {
      get label() {
        return td('sys.tool.genEdit.addEdit');
      },
      width: '120',
      align: 'center',
      slot: 'cell6'
    },
    {
      get label() {
        return td('sys.tool.genEdit.list');
      },
      width: '120',
      align: 'center',
      slot: 'cell7'
    },
    {
      get label() {
        return td('sys.tool.genEdit.query');
      },
      width: '120',
      align: 'center',
      slot: 'cell8'
    },
    {
      get label() {
        return td('sys.tool.genEdit.queryMethod');
      },
      width: '150',
      align: 'center',
      slot: 'cell9'
    },
    {
      get label() {
        return td('sys.tool.genEdit.required');
      },
      width: '60',
      align: 'center',
      slot: 'cell10'
    },
    {
      get label() {
        return td('sys.tool.genEdit.displayType');
      },
      minWidth: '150',
      align: 'center',
      slot: 'cell11'
    },
    {
      get label() {
        return td('sys.tool.genEdit.dictType');
      },
      width: '150',
      align: 'center',
      slot: 'action'
    }
  ],
  func: async () => ({ data: unref(columns) }),
  params: {},
  events: {}
});

watch([() => unref(columns)], () => qtTableRef0.value?.resetQuery(), { flush: 'post' });
</script>
<style scoped lang="scss">
.gen-edit-page {
  min-width: 0;
}

.model-form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 15px;
  padding: 10px 15px;
  border: 1px solid var(--qt-border-color, var(--el-border-color));
  background: var(--qt-surface-background, var(--el-bg-color));
}

.model-form-header-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.model-form-title.h2-titles {
  margin: 0;
}

.wizard-steps {
  width: 100%;
  margin-bottom: 15px;
  padding: 32px clamp(15px, 12vw, 180px);
  box-sizing: border-box;
  background: var(--qt-surface-background, var(--el-bg-color));
}

.pagecont-top {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 340px);
  background: var(--qt-surface-background, var(--el-bg-color));
}

.infotop {
  display: flex;
  flex: 1;
  min-width: 0;
}

.main {
  flex: 1;
  min-width: 0;
  padding: 24px;
  overflow: auto;
}

.wizard-section {
  min-width: 0;
}

.button-style {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  padding: 20px 30px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

@media (max-width: 900px) {
  .main {
    padding: 15px;
  }

  .button-style {
    padding: 15px;
  }
}
</style>
