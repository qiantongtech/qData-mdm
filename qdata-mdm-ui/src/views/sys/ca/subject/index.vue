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
  <div class="app-container" ref="app-container">
    <qt-wrap
      :columns="tableStore0.columns"
      :config="{ actions: { table: { columns: false } } }"
      :table-ref="{ getList }"
    >
      <template #search>
        <qt-search-bar
          ref="queryForm"
          v-bind="searchStore"
          :params="searchParams"
          @query="handleQuery"
          @reset="resetQuery"
        />
      </template>
      <template #actions-data>
        <el-button
          type="primary"
          plain
          icon="plus"
          @click="handleAdd"
          v-hasPermi="['ca:subject:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #id="{ row }">{{ row.id }}</template>
          <template #name="{ row }">{{ row.name }}</template>
          <template #cn="{ row }">{{ row.cn }}</template>
          <template #ou="{ row }">{{ row.ou }}</template>
          <template #o="{ row }">{{ row.o }}</template>
          <template #l="{ row }">{{ row.l }}</template>
          <template #st="{ row }">{{ row.st }}</template>
          <template #c="{ row }">{{ row.c }}</template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['ca:subject:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="download"
              @click="downloadFiles(scope.row)"
              v-hasPermi="['ca:subject:remove']"
            >
              {{ td('common.button.download') }}
            </el-button>
            <el-button
              link
              type="primary"
              style="color: red"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['ca:subject:remove']"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <subject-form-dialog ref="subjectFormDialogRef" :state="subjectFormDialogState" />
    <subject-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script>
import SubjectDetailDialog from './components/subjectDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import SubjectFormDialog from './components/subjectFormDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import {
  listSubject,
  getSubject,
  delSubject,
  addSubject,
  updateSubject
} from '@/api/system/ca/subject.js';
import useDefaultLang from '@/composables/useDefaultLang';
import JSZip from 'jszip';
import { red } from 'chalk';

export default {
  components: { SubjectDetailDialog, QtSearchBar, SubjectFormDialog },
  name: 'Subject',
  setup() {
    const { td, t } = useDefaultLang();
    return { td, t, checkPermi };
  },
  data() {
    return {
      // Mask
      loading: true,
      // Selected array
      ids: [],
      // Not Disable by Single
      single: true,
      // Not Disable
      multiple: true,
      // Show search conditions
      showSearch: true,
      // Total number of articles
      total: 0,
      // Master Management Table Data
      subjectList: [],
      // Popup Layer Title
      title: '',
      // Whether to show the eject layer
      open: false,
      // Submit buttons to avoid repeat clicks
      submitLoading: false,
      // Query parameters
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        cn: null,
        ou: null,
        o: null,
        l: null,
        st: null,
        c: null,
        certificate: null,
        privateKey: null,
        validFlag: null,
        creatorId: null
      },
      // Form parameters
      form: {},
      // Form validation
      rules: {
        name: [
          { required: true, message: this.td('sys.subject.subjectNameRequired'), trigger: 'blur' }
        ],
        cn: [
          { required: true, message: this.td('sys.subject.commonNameRequired'), trigger: 'blur' }
        ],
        ou: [{ required: true, message: this.td('sys.subject.orgUnitRequired'), trigger: 'blur' }],
        o: [{ required: true, message: this.td('sys.subject.orgNameRequired'), trigger: 'blur' }],
        l: [{ required: true, message: this.td('sys.subject.cityNameRequired'), trigger: 'blur' }],
        st: [
          { required: true, message: this.td('sys.subject.provinceNameRequired'), trigger: 'blur' }
        ],
        c: [{ required: true, message: this.td('sys.subject.countryRequired'), trigger: 'blur' }]
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    red,
    /** Query Subject Management List */
    getList() {
      this.loading = true;
      listSubject(this.queryParams).then((response) => {
        this.subjectList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    async downloadFiles(row) {
      const zip = new JSZip();

      const files = [row.privateKey, row.certificate];

      for (let fileUrl of files) {
        const response = await fetch(fileUrl);
        const blob = await response.blob();

        // Automatically get filenames
        const fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        zip.file(fileName, blob);
      }

      zip.generateAsync({ type: 'blob' }).then((content) => {
        saveAs(content, row.name + '_根证书' + '.zip');
      });
    },
    // Cancel button
    cancel() {
      this.open = false;
      this.reset();
    },
    // Form Reset
    reset() {
      this.form = {
        id: null,
        name: null,
        cn: null,
        ou: null,
        o: null,
        l: null,
        st: null,
        c: null,
        certificate: null,
        privateKey: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.$refs.subjectFormDialogRef.dialogElement0?.resetFields();
    },
    /** Search button operation */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** Reset button operations */
    resetQuery() {
      this.$refs.queryForm?.$refs.formRef?.resetFields();
      this.handleQuery();
    },
    // Multiple box selected data
    handleSelectionChange(selection) {
      this.ids = selection.map((item) => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** Add button operation */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = this.td('sys.subject.addTitle');
    },
    /** Modify button operation */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids;
      getSubject(id).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = this.td('sys.subject.editTitle');
      });
    },
    /** Submit button */
    submitForm() {
      if (this.submitLoading) return;
      this.submitLoading = true;
      this.$refs.subjectFormDialogRef.dialogElement0.validate((valid) => {
        if (valid) {
          if (this.form.id != null) {
            updateSubject(this.form)
              .then((response) => {
                this.$modal.msgSuccess(this.td('common.message.editSuccess'));
                this.open = false;
                this.getList();
                this.submitLoading = false;
              })
              .catch(() => {
                this.submitLoading = false;
              });
          } else {
            addSubject(this.form)
              .then((response) => {
                this.$modal.msgSuccess(this.td('common.message.addSuccess'));
                this.open = false;
                this.getList();
                this.submitLoading = false;
              })
              .catch(() => {
                this.submitLoading = false;
              });
          }
        } else {
          this.submitLoading = false;
        }
      });
    },
    /** Remove button operation */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal
        .confirm(this.td('sys.subject.confirmDelete', { id: ids }))
        .then(function () {
          return delSubject(ids);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.td('common.message.deleteSuccess'));
        })
        .catch(() => {});
    },
    /** Export button operation */
    handleExport() {
      this.download(
        'ca/subject/export',
        {
          ...this.queryParams
        },
        `subject_${new Date().getTime()}.xlsx`
      );
    },
    handleTablePagination0(params) {
      this.queryParams.pageNum = params.pageNum;
      this.queryParams.pageSize = params.pageSize;
      this.getList();
    }
  },
  computed: {
    subjectFormDialogState() {
      const vm = this;
      return reactive({
        get title() {
          return vm.title;
        },
        set title(value) {
          vm.title = value;
        },
        get open() {
          return vm.open;
        },
        set open(value) {
          vm.open = value;
        },
        get cancel() {
          return vm.cancel;
        },
        set cancel(value) {
          vm.cancel = value;
        },
        get submitLoading() {
          return vm.submitLoading;
        },
        set submitLoading(value) {
          vm.submitLoading = value;
        },
        get submitForm() {
          return vm.submitForm;
        },
        set submitForm(value) {
          vm.submitForm = value;
        },
        get form() {
          return vm.form;
        },
        set form(value) {
          vm.form = value;
        },
        get rules() {
          return vm.rules;
        },
        set rules(value) {
          vm.rules = value;
        }
      });
    },
    searchParams() {
      const vm = this;
      return reactive({
        get name() {
          return vm.queryParams.name;
        },
        set name(value) {
          vm.queryParams.name = value;
        }
      });
    },
    searchStore() {
      const vm = this;
      return reactive({
        config: { form: { labelPosition: vm.labelPosition } },
        items: [
          {
            label: vm.td('sys.subject.subjectName'),
            prop: 'name',
            component: {
              is: 'input',
              placeholder: vm.td('sys.subject.subjectNamePlaceholder'),
              clearable: true
            }
          }
        ]
      });
    },
    tableStore0() {
      const vm = this;
      return reactive({
        config: {
          table: {
            defaultSort: { prop: 'id', order: 'descending' },
            stripe: true,
            height: '60vh',
            onSelectionChange: vm.handleSelectionChange
          },
          pagination: {
            pageSizes: [10, 20, 30, 50],
            pagerCount: 5,
            params: { pageNum: vm.queryParams.pageNum, pageSize: vm.queryParams.pageSize }
          }
        },
        columns: [
          {
            sortableKey: 'id',
            prop: 'id',
            sortable: true,
            width: 60,
            get label() {
              return vm.td('common.texts.number');
            },
            align: 'center',
            slot: 'id'
          },
          {
            get label() {
              return vm.td('sys.subject.subjectName');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'name'
          },
          {
            get label() {
              return vm.td('sys.subject.commonName');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'cn'
          },
          {
            get label() {
              return vm.td('sys.subject.dept');
            },
            align: 'center',
            slot: 'ou'
          },
          {
            get label() {
              return vm.td('sys.subject.orgName');
            },
            align: 'center',
            slot: 'o'
          },
          {
            get label() {
              return vm.td('sys.subject.cityName');
            },
            align: 'center',
            slot: 'l'
          },
          {
            get label() {
              return vm.td('sys.subject.provinceName');
            },
            align: 'center',
            slot: 'st'
          },
          {
            get label() {
              return vm.td('sys.subject.country');
            },
            align: 'center',
            slot: 'c'
          },
          {
            get label() {
              return vm.td('common.texts.operation');
            },
            align: 'center',
            className: 'small-padding fixed-width',
            fixed: 'right',
            width: '240',
            slot: 'action'
          }
        ],
        func: async () => ({ data: { rows: vm.subjectList, total: vm.total } }),
        params: vm.queryParams,
        events: {
          onPageCurrentChange: vm.handleTablePagination0,
          onPageSizeChange: vm.handleTablePagination0
        }
      });
    }
  },
  watch: {
    subjectList: {
      handler() {
        this.$nextTick(() => this.$refs.qtTableRef0?.resetQuery());
      }
    },
    total: {
      handler() {
        this.$nextTick(() => this.$refs.qtTableRef0?.resetQuery());
      }
    },
    'queryParams.pageNum': {
      handler() {
        this.$nextTick(() => this.$refs.qtTableRef0?.resetQuery());
      }
    },
    'queryParams.pageSize': {
      handler() {
        this.$nextTick(() => this.$refs.qtTableRef0?.resetQuery());
      }
    }
  }
};
</script>
