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
          size="mini"
          @click="handleAdd"
          v-hasPermi="['ca:cert:add']"
        >
          {{ td('common.button.add') }}
        </el-button>
      </template>
      <div v-loading="loading">
        <qt-table v-bind="tableStore0" ref="qtTableRef0">
          <template #id="{ row }">{{ row.id }}</template>
          <template #name="{ row }">{{ row.name }}</template>
          <template #subjectName="{ row }">{{ row.subjectName }}</template>
          <template #issuer="{ row }">{{ row.issuer }}</template>
          <template #possessor="{ row }">{{ row.possessor }}</template>
          <template #validTime="scope">
            {{ scope.row.validTime }}
            {{ td('sys.cert.year') }}
          </template>
          <template #createTime="{ row }">{{ row.createTime }}</template>
          <template #remark="scope">
            <span>{{ scope.row.remark || '-' }}</span>
          </template>
          <template #action="scope">
            <el-button
              link
              type="primary"
              icon="View"
              :disabled="!checkPermi(['ca:cert:query'])"
              @click="$refs.detailDialogRef.openDialog(scope.row)"
              >{{ td('common.button.details') }}</el-button
            >
            <el-button
              link
              type="primary"
              icon="download"
              @click="downloadFiles(scope.row)"
              v-hasPermi="['ca:cert:edit']"
            >
              {{ td('common.button.download') }}
            </el-button>
            <el-button
              link
              type="danger"
              style="color: red"
              icon="Delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['ca:cert:remove']"
            >
              {{ td('common.button.delete') }}
            </el-button>
          </template>
        </qt-table>
      </div>
    </qt-wrap>
    <cert-form-dialog ref="certFormDialogRef" :state="certFormDialogState" />
    <cert-detail-dialog ref="detailDialogRef" />
  </div>
</template>

<script>
import CertDetailDialog from './components/certDetailDialog.vue';
import { checkPermi } from '@/utils/permission';

import CertFormDialog from './components/certFormDialog.vue';
import { reactive, computed, watch, ref, unref, isRef } from 'vue';
import QtSearchBar from '@/components/QtSearchBar/index.vue';

import { listCert, getCert, delCert, addCert, updateCert } from '@/api/system/ca/cert.js';
import useDefaultLang from '@/composables/useDefaultLang';
import { listSubject } from '@/api/system/ca/subject.js';
import JSZip from 'jszip';

export default {
  components: { CertDetailDialog, QtSearchBar, CertFormDialog },
  name: 'Cert',
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
      // Body List
      subjectList: [],
      // Certificate Form Data
      certList: [],
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
        subjectId: null,
        subjectName: null,
        certificate: null,
        privateKey: null,
        issuer: null,
        possessor: null,
        validTime: null,
        validFlag: null,
        creatorId: null
      },
      // Form parameters
      form: {},
      // Form validation
      rules: {
        name: [{ required: true, message: this.td('common.texts.name'), trigger: 'blur' }],
        subjectId: [
          { required: true, message: this.td('sys.cert.subjectIdRequired'), trigger: 'change' }
        ],
        subjectName: [
          { required: true, message: this.td('sys.cert.subjectNameRequired'), trigger: 'change' }
        ],
        issuer: [{ required: true, message: this.td('sys.cert.issuerRequired'), trigger: 'blur' }],
        possessor: [
          { required: true, message: this.td('sys.cert.ownerRequired'), trigger: 'blur' }
        ],
        validTime: [
          { required: true, message: this.td('sys.cert.validPeriodRequired'), trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.getSubjectList();
  },
  methods: {
    /** Query Certificate List */
    getList() {
      this.loading = true;
      listCert(this.queryParams).then((response) => {
        this.certList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** Query Theme List */
    getSubjectList() {
      listSubject({
        pageNum: 1,
        pageSize: 999999
      }).then((response) => {
        this.subjectList = response.rows;
      });
    },
    subjectChange(e) {
      this.subjectList.forEach((item) => {
        if (item.id === e) {
          this.form.subjectName = item.name;
          this.form.issuer = item.name;
        }
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
        subjectId: null,
        subjectName: null,
        certificate: null,
        privateKey: null,
        issuer: null,
        possessor: null,
        validTime: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        createTime: null,
        updateBy: null,
        updateTime: null,
        remark: null
      };
      this.$refs.certFormDialogRef.dialogElement0?.resetFields();
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
      this.title = this.td('sys.cert.addTitle');
    },
    /** Modify button operation */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids;
      getCert(id).then((response) => {
        this.form = response.data;
        this.open = true;
        this.title = this.td('sys.cert.editTitle');
      });
    },
    /** Submit button */
    submitForm() {
      if (this.submitLoading) return;
      this.submitLoading = true;
      this.$refs.certFormDialogRef.dialogElement0.validate((valid) => {
        if (valid) {
          if (this.form.id != null) {
            updateCert(this.form)
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
            addCert(this.form)
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
        .confirm(this.td('sys.cert.confirmDelete', { id: ids }))
        .then(function () {
          return delCert(ids);
        })
        .then(() => {
          this.getList();
          this.$modal.msgSuccess(this.td('common.message.deleteSuccess'));
        })
        .catch(() => {});
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
        saveAs(content, row.name + '_数字证书' + '.zip');
      });
    },
    /** Export button operation */
    handleExport() {
      this.download(
        'ca/cert/export',
        {
          ...this.queryParams
        },
        `cert_${new Date().getTime()}.xlsx`
      );
    },
    handleTablePagination0(params) {
      this.queryParams.pageNum = params.pageNum;
      this.queryParams.pageSize = params.pageSize;
      this.getList();
    }
  },
  computed: {
    certFormDialogState() {
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
        },
        get subjectChange() {
          return vm.subjectChange;
        },
        set subjectChange(value) {
          vm.subjectChange = value;
        },
        get subjectList() {
          return vm.subjectList;
        },
        set subjectList(value) {
          vm.subjectList = value;
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
        },
        get issuer() {
          return vm.queryParams.issuer;
        },
        set issuer(value) {
          vm.queryParams.issuer = value;
        },
        get possessor() {
          return vm.queryParams.possessor;
        },
        set possessor(value) {
          vm.queryParams.possessor = value;
        }
      });
    },
    searchStore() {
      const vm = this;
      return reactive({
        config: { form: { labelPosition: vm.labelPosition } },
        items: [
          {
            label: vm.td('common.texts.name'),
            prop: 'name',
            component: {
              is: 'input',
              placeholder: vm.td('common.form.namePlaceholder'),
              clearable: true
            }
          },
          {
            label: vm.td('sys.cert.issuer'),
            prop: 'issuer',
            component: {
              is: 'input',
              placeholder: vm.td('sys.cert.issuerPlaceholder'),
              clearable: true
            }
          },
          {
            label: vm.td('sys.cert.owner'),
            prop: 'possessor',
            component: {
              is: 'input',
              placeholder: vm.td('sys.cert.ownerPlaceholder'),
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
            defaultSort: { prop: 'createTime', order: 'descending' },
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
              return vm.td('common.texts.name');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'name'
          },
          {
            get label() {
              return vm.td('sys.cert.subjectName');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'subjectName'
          },
          {
            get label() {
              return vm.td('sys.cert.issuer');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'issuer'
          },
          {
            get label() {
              return vm.td('sys.cert.owner');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'possessor'
          },
          {
            get label() {
              return vm.td('sys.cert.validPeriod');
            },
            align: 'center',
            slot: 'validTime'
          },
          {
            prop: 'createTime',
            sortableKey: 'create_time',
            sortable: true,
            get label() {
              return vm.td('sys.cert.effectTime');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'createTime'
          },
          {
            get label() {
              return vm.td('common.texts.remark');
            },
            align: 'center',
            showOverflowTooltip: true,
            slot: 'remark'
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
        func: async () => ({ data: { rows: vm.certList, total: vm.total } }),
        params: vm.queryParams,
        events: {
          onPageCurrentChange: vm.handleTablePagination0,
          onPageSizeChange: vm.handleTablePagination0
        }
      });
    }
  },
  watch: {
    certList: {
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
