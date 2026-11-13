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
  <div ref="app-container" class="app-container user-dialog-host">
    <el-dialog
      v-model="visible"
      :title="title"
      width="800px"
      append-to-body
      draggable
      destroy-on-close
      class="dialog"
      :append-to="$refs['app-container']"
      @closed="resetForm"
    >
      <el-form
        ref="userFormRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        :label-position="labelPosition"
        class="column-form"
        @submit.prevent
      >
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.userNickNameLabel')"
          prop="nickName"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.nickName"
            :placeholder="td('sys.system.user.userNickNamePlaceholder')"
            maxlength="30"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.belongDept')"
          prop="deptId"
          :label-position="labelPosition"
        >
          <el-tree-select
            v-model="form.deptId"
            :data="props.deptOptions"
            :props="{ value: 'id', label: 'label', children: 'children' }"
            value-key="id"
            :placeholder="td('sys.system.user.selectBelongDept')"
            check-strictly
          />
        </qt-form-item>
        <qt-form-item class="row-full" :label="td('sys.system.user.phone')" prop="phonenumber">
          <el-input
            v-model="form.phonenumber"
            :placeholder="td('sys.system.user.phonePlaceholder')"
            maxlength="11"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.email')"
          prop="email"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.email"
            :placeholder="td('sys.system.user.emailPlaceholder')"
            maxlength="50"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          v-if="form.userId == undefined"
          :label="td('sys.system.user.userNameLabel')"
          prop="userName"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.userName"
            :placeholder="td('sys.system.user.userNameLabelPlaceholder')"
            maxlength="30"
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          v-if="form.userId == undefined"
          :label="td('sys.system.user.userPassword')"
          prop="password"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.password"
            :placeholder="td('sys.system.user.userPasswordPlaceholder')"
            type="password"
            maxlength="20"
            show-password
          />
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.userGender')"
          :label-position="labelPosition"
        >
          <el-select v-model="form.sex" :placeholder="td('sys.system.user.selectPlaceholder')">
            <el-option
              v-for="dict in sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.status')"
          :label-position="labelPosition"
        >
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value"
              >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.post')"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.postIds"
            multiple
            :placeholder="td('sys.system.user.selectPlaceholder')"
            class="selectlist"
          >
            <el-option
              v-for="item in postOptions"
              :key="item.postId"
              :label="item.postName"
              :value="item.postId"
              :disabled="item.status == 1"
            ></el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('sys.system.user.role')"
          prop="roleIds"
          :label-position="labelPosition"
        >
          <el-select
            v-model="form.roleIds"
            multiple
            :placeholder="td('sys.system.user.selectPlaceholder')"
            class="selectlist"
          >
            <el-option
              v-for="item in roleOptions"
              :key="item.roleId"
              :label="item.roleName"
              :value="item.roleId"
              :disabled="item.status == 1"
            ></el-option>
          </el-select>
        </qt-form-item>
        <qt-form-item
          class="row-full"
          :label="td('common.texts.remark')"
          :label-position="labelPosition"
        >
          <el-input
            v-model="form.remark"
            type="textarea"
            :placeholder="td('sys.system.user.inputContent')"
          ></el-input>
        </qt-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="visible = false">{{ td('common.button.cancel') }}</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            {{ td('common.button.confirm') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="UserFormDialog">
/** 用户新增、编辑弹窗；保存成功后通知列表刷新。 */
import { computed, getCurrentInstance, ref } from 'vue';
import { addUser, getUser, updateUser } from '@/api/system/system/user.js';
import useUserStore from '@/store/system/user';
import useDefaultLang from '@/composables/useDefaultLang';
import QtFormItem from '@/components/QtFormItem/index.vue';

const props = defineProps({ deptOptions: { type: Array, default: () => [] } });
const emit = defineEmits(['success']);
const { proxy } = getCurrentInstance();
const { td } = useDefaultLang();
const userStore = useUserStore();
const userId = ref(userStore.id);
const { sys_normal_disable, sys_user_sex } = proxy.useDict('sys_normal_disable', 'sys_user_sex');
const visible = ref(false);
const title = ref('');
const submitting = ref(false);
const userFormRef = ref();
const postOptions = ref([]);
const roleOptions = ref([]);

const createEmptyForm = () => ({
  userId: undefined,
  deptId: undefined,
  userName: undefined,
  nickName: undefined,
  password: undefined,
  phonenumber: undefined,
  email: undefined,
  sex: undefined,
  status: '0',
  remark: undefined,
  postIds: [],
  roleIds: []
});
const form = ref(createEmptyForm());

// Computed property dynamically sets phonenumber rule
const phonenumberRules = computed(() => {
  const rules = [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: td('sys.system.user.phoneRequired'),
      trigger: 'blur'
    }
  ];
  if (userId.value != 1) {
    rules.unshift({
      required: true,
      message: td('sys.system.user.contactPhoneRequired'),
      trigger: 'blur'
    });
  }

  return rules;
});

// Form validation rules
const rules = computed(() => ({
  userName: [
    { required: true, message: td('sys.system.user.userNameRequired'), trigger: 'blur' },
    {
      min: 2,
      max: 20,
      message: td('sys.system.user.userNameLengthRange'),
      trigger: 'blur'
    }
  ],
  nickName: [
    { required: true, message: td('sys.system.user.userNickNameRequired'), trigger: 'blur' }
  ],
  roleIds: [{ required: true, message: td('sys.system.user.roleRequired'), trigger: 'change' }],
  deptId: [{ required: true, message: td('sys.system.user.deptRequired'), trigger: 'change' }],
  password: [
    { required: true, message: td('sys.system.user.passwordRequired'), trigger: 'blur' },
    {
      min: 8,
      max: 20,
      message: td('sys.system.user.passwordLengthRange'),
      trigger: 'blur'
    },
    {
      pattern: /^[^<>"'|\\]+$/,
      message: td('sys.system.user.invalidChars'),
      trigger: 'blur'
    },
    {
      validator: (rule, value, callback) => {
        const strengthRegex = {
          minLength: /^.{8,}$/,
          upperCase: /[A-Z]/,
          lowerCase: /[a-z]/,
          number: /\d/,
          specialChar: /[!@#$%^&*(),.?":{}|<>]/
        };

        if (!strengthRegex.minLength.test(value)) {
          callback(new Error(td('sys.system.user.passwordMinLength')));
        } else if (!strengthRegex.upperCase.test(value)) {
          callback(new Error(td('sys.system.user.passwordUppercase')));
        } else if (!strengthRegex.lowerCase.test(value)) {
          callback(new Error(td('sys.system.user.passwordLowercase')));
        } else if (!strengthRegex.number.test(value)) {
          callback(new Error(td('sys.system.user.passwordDigit')));
        } else if (!strengthRegex.specialChar.test(value)) {
          callback(new Error(td('sys.system.user.passwordSpecial')));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      type: 'email',
      message: td('sys.system.user.emailRequired'),
      trigger: ['blur', 'change']
    }
  ],
  phonenumber: phonenumberRules.value
}));
/** 关闭后恢复初始表单并清除校验。 */
function resetForm() {
  form.value = createEmptyForm();
  postOptions.value = [];
  roleOptions.value = [];
  userFormRef.value?.clearValidate();
}

/** 按用户 ID 加载详情；不传 ID 时打开新增表单。 */
async function openDialog(id) {
  resetForm();
  const editing = id != null;
  const response = await getUser(id);
  postOptions.value = response.posts || [];
  roleOptions.value = response.roles || [];
  if (editing) {
    form.value = {
      ...response.data,
      postIds: response.postIds || [],
      roleIds: response.roleIds || [],
      password: ''
    };
  }
  title.value = td(editing ? 'sys.system.user.editUser' : 'sys.system.user.addUser');
  visible.value = true;
}

/** 校验、提交并通知父页面刷新；失败时保留表单。 */
async function submitForm() {
  if (submitting.value || !userFormRef.value) return;
  submitting.value = true;
  try {
    const valid = await userFormRef.value.validate().catch(() => false);
    if (!valid) return;
    const editing = form.value.userId != null;
    await (editing ? updateUser(form.value) : addUser(form.value));
    proxy.$modal.msgSuccess(
      td(editing ? 'common.message.editSuccess' : 'common.message.addSuccess')
    );
    visible.value = false;
    emit('success');
  } finally {
    submitting.value = false;
  }
}

defineExpose({ openDialog });
</script>

<style scoped lang="scss">
.user-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}

.column-form {
  display: flex;
  flex-direction: column;
}

.row-full {
  width: 100%;
  margin-right: 0;
}

.column-form :deep(.el-select),
.column-form :deep(.el-tree-select) {
  width: 100%;
}

:deep(.selectlist .el-tag.el-tag--info) {
  background: #f3f8ff !important;
  border: 0 !important;
  color: var(--qt-primary) !important;
}
</style>
