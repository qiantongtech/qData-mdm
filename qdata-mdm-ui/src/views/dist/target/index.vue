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

  Business License: https://docs.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div class="app-container" ref="app-container">
    <!-- 目标系统列表说明，沿用平台各业务模块的统一提示组件。 -->
    <GuideTip tip-id="dist/target.list" />
    <qt-wrap :columns="tableStore.columns" :tableRef="tableRef">
      <template #search>
        <qt-search-bar v-bind="searchStore" :params="queryParams" :tableRef="tableRef" />
      </template>
      <template #actions-data>
        <!-- 列表工具栏提供新增和批量删除。 -->
        <el-button type="primary" plain @click="handleAdd" v-hasPermi="['dist:target:add']"
                   @mousedown="(e) => e.preventDefault()"
>
          <i class="iconfont-mini icon-xinzeng mr5"></i>新增
        </el-button>
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()"
                   v-hasPermi="['dist:target:remove']"
>批量删除</el-button>
      </template>
      <qt-table v-bind="tableStore" ref="tableRef">
        <!-- 仅接口明确返回 0 时显示 0；未返回统计字段时显示横线，避免误判没有关联任务。 -->
        <template #taskCount="{ row }">{{ row.taskCount ?? '-' }}</template>
        <!-- 服务授权数量尚未接入目标系统统计，暂以横线表示。 -->
        <template #authorizationCount>-</template>
        <template #responsible="{ row }">
          <div class="person-cell">
            <span>{{ row.contactName || '-' }}</span>
            <span v-if="row.contactInfo" class="person-phone">{{ row.contactInfo }}</span>
          </div>
        </template>
        <template #creator="{ row }">
          <!-- 创建人电话读取平台账号资料，与目标系统联系人信息无关。 -->
          <div class="person-cell">
            <span>{{ row.createBy || '-' }}</span>
            <span v-if="row.creatorPhone" class="person-phone">{{ row.creatorPhone }}</span>
          </div>
        </template>
        <template #status="{ row }">
          <!-- 开关打开为启用（1），关闭为禁用（2）；无编辑权限时只读。 -->
          <el-switch v-model="row.status" active-value="1" inactive-value="2"
                     :loading="statusLoading[row.id]"
                     :disabled="!proxy.$auth.hasPermi('dist:target:edit') || statusLoading[row.id] || !['1', '2'].includes(String(row.status))"
                     @change="handleStatusSwitch(row, $event)"
/>
        </template>
        <template #handle="{ row }">
          <!-- 行操作沿用项目列表按钮；仅禁用状态允许删除。 -->
          <el-button link type="primary" icon="View" @click="handleDetail(row)"
                     v-hasPermi="['dist:target:query']"
>详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(row)"
                     v-hasPermi="['dist:target:edit']"
>修改</el-button>
          <el-button link type="danger" icon="Delete"
                     @click="handleDelete(row)"
                     v-hasPermi="['dist:target:remove']"
>删除</el-button>
        </template>
      </qt-table>
    </qt-wrap>

    <!-- 新增和修改目标系统：参照来源系统弹窗，字段按单列排列。 -->
    <el-dialog :title="title" v-model="open" width="800px" :append-to="$refs['app-container']" draggable>
      <el-form ref="TargetSystemRef" :model="form" :rules="rules" label-width="120px" @submit.prevent>
        <el-form-item label="目标系统名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入目标系统名称，支持中文、英文字母、数字、下划线和短横线" maxlength="128" />
        </el-form-item>
        <!-- 系统编码：新增时提示输入系统编码，创建后只读，不允许修改。 -->
        <el-form-item label="目标系统编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入目标系统编码，仅支持英文字母、数字和下划线" maxlength="64"
                    :disabled="form.id != null"
/>
        </el-form-item>
        <!-- 创建人由登录账号自动记录；表单仅选择负责人并回显其电话。 -->
        <qt-form-item
          label="负责人"
          prop="responsibleUserId"
        >
          <el-select
            v-model="form.responsibleUserId"
            filterable
            :loading="usersLoading"
            placeholder="请选择负责人"
            @change="handleResponsibleChange"
            style="width: 100%"
          >
            <el-option v-for="user in userOptions" :key="user.value"
                       :label="user.label" :value="user.value" :disabled="user.disabled"
            />
          </el-select>
        </qt-form-item>
        <qt-form-item label="联系电话">
          <el-input :model-value="form.contactInfo || ''" placeholder="请输入联系电话" disabled />
        </qt-form-item>
        <!-- 状态值沿用现有接口：1 启用、2 禁用。 -->
        <qt-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="1">启用</el-radio>
            <el-radio label="2">禁用</el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="256个字符"
            show-word-limit
            placeholder="可简要说明系统用途、对接范围或其他备注信息，最多256个字符。"
          />
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取 消</el-button>
          <el-button type="primary" :loading="loading" @click="submitForm">确 定</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="TargetSystem">
  import { listTargetSystem, getTargetSystem, delTargetSystem, addTargetSystem, updateTargetSystem } from "@/api/dist/target/targetSystem";
  import { useUserDirectoryStore } from '@/store/system/userDirectory';
  import { trimText, validateAlphaNumUnderscore, validatePattern, validateTrimmedRequired } from "@/utils/validate";
  import { useRoute } from "vue-router";

const userDirectoryStore = useUserDirectoryStore();

  const { proxy } = getCurrentInstance();
  const route = useRoute();
  const listRoutePath = route.path;

  const tableRef = ref(null);

  const open = ref(false);
  const loading = ref(false);
  const selectedRows = ref([]);
  const multiple = ref(true);
  const statusLoading = reactive({});
  const title = ref("");
  const userOptions = ref([]);
  const usersLoading = ref(false);

  const data = reactive({
    form: {},
    queryParams: {
      code: null,
      name: null,
      status: null,
    },
    rules: {
      name: [
        { required: true, validator: validateTrimmedRequired("目标系统名称不能为空", 128), trigger: "blur" },
        { validator: validatePattern(/^[\u4e00-\u9fffA-Za-z0-9_-]+$/, "目标系统名称只允许中文、英文字母、数字、下划线和短横线"), trigger: "blur", transform: trimText }
      ],
      code: [
        { required: true, validator: validateTrimmedRequired("目标系统编码不能为空", 64), trigger: "blur" },
        { validator: validateAlphaNumUnderscore("目标系统编码只允许英文字母、数字和下划线"), trigger: "blur", transform: trimText }
      ]
    }
  });

  const { queryParams, form, rules } = toRefs(data);

  /** 目标系统列表复用平台表格组件，分页、加载和空状态由组件统一处理。 */
  const tableStore = reactive({
    config: {
      table: {
        stripe: true,
        rowKey: "id",
        onSelectionChange: handleSelectionChange,
        defaultSort: { prop: "createTime", order: "descending" }
      },
      sort: true
    },
    columns: [
      { type: "selection", width: 55 },
      // 负责人与创建人分别读取目标系统联系人和创建账号资料。
      { label: "编号", prop: "rowNumber", width: 65, align: "center", sortable: "custom", sortableKey: "id" },
      {
        label: "目标系统名称", prop: "name", minWidth: 190, align: "left", headerAlign: "left",
        info: {
          title: "name",
          get click() {
            return proxy.$auth.hasPermi('dist:target:query') ? row => row.name && handleDetail(row) : undefined;
          },
        },
      },
      { label: "系统编码", prop: "code", minWidth: 160, align: "left", headerAlign: "left" },
      { label: "描述", prop: "description", width: 340, align: "left", headerAlign: "left", showOverflowTooltip: true },
      { label: "分发任务", prop: "taskCount", width: 110, align: "center", slot: "taskCount" },
      { label: "服务授权", width: 110, align: "center", slot: "authorizationCount" },
      { label: "状态", prop: "status", width: 120, align: "left", headerAlign: "left", slot: "status" },
      { label: "负责人", prop: "contactName", minWidth: 140, align: "left", headerAlign: "left", slot: "responsible" },
      { label: "创建人", prop: "createBy", minWidth: 140, align: "left", headerAlign: "left", slot: "creator" },
      { label: "创建时间", prop: "createTime", minWidth: 180, align: "left", headerAlign: "left", date: true, sortable: "custom", sortableKey: "create_time" },
      { label: "操作",  width: 220, align: "center", fixed: "right", slot: "handle" }
    ],
    func: listTargetSystem,
    params: queryParams.value,
    events: {
      // 表格组件使用 ascending/descending；接口按 asc/desc 对整张表排序。
      formatParams: (params) => {
        if (!params.isAsc) {
          const rest = { ...params };
          delete rest.orderByColumn;
          delete rest.isAsc;
          return rest;
        }
        return {
          ...params,
          orderByColumn: params.orderByColumn === "createTime" ? "create_time" : params.orderByColumn,
          isAsc: ["ascending", "asc"].includes(params.isAsc) ? "asc" : "desc"
        };
      },
      // 保持编号跨页连续，不使用数据库主键作为展示编号。
      formatData: (rows, params) => rows.map((row, index) => ({
        ...row,
        rowNumber: (params.pageNum - 1) * params.pageSize + index + 1
      }))
    }
  });

  /** 目标系统列表搜索条件，复用平台统一搜索栏。 */
  const searchStore = reactive({
    items: [
      {
        label: "系统名称",
        prop: "name",
        component: {
          is: "input",
          placeholder: "请输入系统名称"
        }
      },
      {
        label: "系统编码",
        prop: "code",
        component: {
          is: "input",
          placeholder: "请输入系统编码"
        }
      },
      {
        label: "状态",
        prop: "status",
        component: {
          is: "select",
          placeholder: "全部",
          options: [
            { label: "启用", value: "1" },
            { label: "禁用", value: "2" }
          ]
        }
      }
    ]
  });

  /** 刷新目标系统列表，供新增、修改、删除和启停成功后调用。 */
  function getList() {
    tableRef.value?.getList();
  }

  // 缓存列表从详情路由返回时重新查询；新挂载的列表仍由 QtTable 自行加载。
  watch(() => route.path, (path) => {
    if (path === listRoutePath) nextTick(getList);
  });

  /** 关闭目标系统编辑弹窗，并重置表单。 */
  function cancel() {
    open.value = false;
    reset();
  }

  /** 恢复目标系统表单默认值并清除校验状态。 */
  function reset() {
    form.value = {
        id: null,
        code: null,
        name: null,
        contactName: null,
        contactInfo: null,
        status: "1",
        description: null,
        validFlag: null,
        delFlag: null,
        createBy: null,
        creatorId: null,
        responsibleUserId: null,
        createTime: null,
        updateBy: null,
        updaterId: null,
        updateTime: null,
        remark: null
    };
    proxy.resetForm("TargetSystemRef");
  }

  /**
   * 更新所选目标系统，并同步批量删除按钮的禁用状态。
   * @param {Array<Object>} selection 当前选中的目标系统记录。
   */
  function handleSelectionChange(selection) {
    selectedRows.value = selection;
    multiple.value = !selection.length;
  }
  /** 选择平台用户 ID；姓名与电话先回显，保存时由后端重新读取该账号资料。 */
  function handleResponsibleChange(userId) {
    const user = userOptions.value.find(item => String(item.value) === String(userId));
    form.value.contactName = user?.label || "";
    form.value.contactInfo = user?.phone || "";
  }

  /** 修改时按用户 ID 回显；历史无 ID 的联系人保留原姓名和电话，重新选择后建立账号关联。 */
  async function loadUserOptions(savedContact = {}) {
    usersLoading.value = true;
    const selectedId = savedContact.responsibleUserId;
    // 加载失败时也保留已保存负责人的姓名，避免下拉框只显示用户 ID。
    userOptions.value = selectedId != null
      ? [{ value: selectedId, label: savedContact.contactName || String(selectedId), phone: savedContact.contactInfo || "", disabled: true }]
      : [];
    try {
      const response = await userDirectoryStore.loadUserList();
      const rows = response.rows || response.data?.rows || [];
      const loadedUsers = rows.map(user => ({
        value: user.userId,
        label: user.nickName || user.userName,
        phone: user.phonenumber || "",
      })).filter(user => user.label);
      userOptions.value = loadedUsers;
      if (selectedId != null && !loadedUsers.some(user => String(user.value) === String(selectedId))) {
        const saved = { value: selectedId, label: savedContact.contactName || String(selectedId),
          phone: savedContact.contactInfo || "", disabled: true };
        userOptions.value.unshift(saved);
      }
    } catch (error) {
      proxy.$modal.msgError("负责人列表加载失败，请重试");
      throw error;
    } finally {
      usersLoading.value = false;
    }
  }

  /** 打开新增目标系统弹窗，并加载负责人选项。 */
  function handleAdd() {
    reset();
    open.value = true;
    title.value = "新增目标系统";
    loadUserOptions().catch(() => {});
  }

  /** 查询目标系统信息并打开修改弹窗。 */
  function handleUpdate(row) {
    reset();
    getTargetSystem(row.id).then(response => {
      form.value = response.data;
      open.value = true;
      title.value = "修改目标系统";
      loadUserOptions(response.data).catch(() => {});
    });
  }

  /** 打开目标系统独立详情页，沿用项目其他业务模块的路由方式。 */
  function handleDetail(row) {
    proxy.$router.push({
      path: "/dist/target/detail",
      query: { id: row.id }
    });
  }

  /** 状态开关切换后先确认；取消或保存失败时恢复原状态。 */
  async function handleStatusSwitch(row, status) {
    const previousStatus = status === "1" ? "2" : "1";
    const actionText = status === "1" ? "启用" : "禁用";
    try {
      await proxy.$modal.confirm(`是否确认${actionText}目标系统“${row.name || row.code}”？`);
      statusLoading[row.id] = true;
      await updateTargetSystem({ id: row.id, status });
      proxy.$modal.msgSuccess(`${actionText}成功`);
      getList();
    } catch {
      row.status = previousStatus;
    } finally {
      statusLoading[row.id] = false;
    }
  }

  /** 提交新增或修改的目标系统。 */
  function submitForm() {
    proxy.$refs["TargetSystemRef"].validate(valid => {
      if (valid && !loading.value) {
        const isUpdate = form.value.id != null;
        const submit = isUpdate ? updateTargetSystem : addTargetSystem;
        // 只提交当前表单字段；创建人由后端记录，历史类型字段不再随表单提交。
        const payload = {
          id: form.value.id,
          name: form.value.name,
          code: form.value.code,
          responsibleUserId: form.value.responsibleUserId,
          contactName: form.value.contactName,
          contactInfo: form.value.contactInfo,
          status: form.value.status,
          description: form.value.description
        };
        loading.value = true;
        submit(payload).then(() => {
          proxy.$modal.msgSuccess(isUpdate ? "修改成功" : "新增成功");
          open.value = false;
          getList();
        }).catch(() => {}).finally(() => {
          loading.value = false;
        });
      }
    });
  }

  /** 删除前提交所有选中记录，由后端统一校验状态和关联任务。 */
  function handleDelete(row) {
    const isSingle = Boolean(row?.id);
    const rows = isSingle ? [row] : selectedRows.value;
    if (!rows.length) return;
    const message = isSingle
      ? `是否确认删除目标系统“${row.name || row.code}”？`
      : `是否确认删除选中的 ${rows.length} 个目标系统？`;
    proxy.$modal.confirm(message).then(() => {
      return delTargetSystem(rows.map(item => item.id).join(","));
    }).then(() => {
      getList();
      proxy.$modal.msgSuccess("删除成功");
    }).catch(() => {});
  }

</script>

<style scoped>
.person-cell {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 20px;
}

.person-phone {
  color: var(--qt-text-secondary);
  font-size: var(--qt-font-size-small);
}

</style>
