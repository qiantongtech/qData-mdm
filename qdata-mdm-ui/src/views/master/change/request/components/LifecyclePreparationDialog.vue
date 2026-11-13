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
  <div ref="dialogContainer">
    <el-dialog v-if="dialogContainer" :model-value="modelValue" :title="`${actionLabel}主数据申请`" width="800px"
               append-to-body :append-to="dialogContainer" draggable destroy-on-close @close="close"
               class="dialog"
    >
      <div v-loading="loading">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="80px"
                 class="column-form" scroll-to-error @submit.prevent
        >
          <qt-form-item label="编号" class="row-full">
            <el-input :model-value="recordNo" disabled />
          </qt-form-item>
          <qt-form-item label="申请原因" prop="applyReason" class="row-full">
            <el-input v-model="form.applyReason"
                      type="textarea"
                      :rows="4"
                      maxlength="256个字符"
                      show-word-limit placeholder="请输入本次状态变更的原因"
            />
          </qt-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="close">取 消</el-button>
          <el-button type="primary" :loading="saving"
                     :disabled="loading || !targetStatus" @click="save"
          >确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { useDict } from '@/utils/dict'
import { getMasterRecordDetail } from '@/api/master/data/data'
import { submitMasterChangeMaintenance } from '@/api/master/change/request'

// 与后端 MasterLifecycleRules 保持一致，归档为终态。
const LIFECYCLE_TRANSITIONS = {
  '1': { FREEZE: '2', INVALIDATE: '3' },
  '2': { UNFREEZE: '1', INVALIDATE: '3' },
  '3': { RECOVER: '1', ARCHIVE: '4' }
}

function getLifecycleTarget(status, action) {
  return Object.hasOwn(LIFECYCLE_TRANSITIONS, status) && Object.hasOwn(LIFECYCLE_TRANSITIONS[status], action)
    ? LIFECYCLE_TRANSITIONS[status][action] : ''
}

function hasPendingMaintenance(row) {
  if (!row.MASTER_APPROVAL_REQUEST_ID) return false
  const approval = String(row.MASTER_APPROVAL_STATUS)
  return approval === '0' || (approval === '1' && ['0', '1'].includes(String(row.MASTER_EXECUTE_STATUS)))
}

const props = defineProps({
  modelValue: Boolean,
  instanceId: { type: [String, Number], required: true },
  modelId: { type: [String, Number], required: true },
  recordId: { type: [String, Number], default: '' },
  action: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue', 'saved'])
const { proxy } = getCurrentInstance()
const { master_change_type } = useDict('master_change_type')
const row = ref({})
const form = reactive({ applyReason: '' })
const formRef = ref(null)
const dialogContainer = ref(null)
const loading = ref(false)
const saving = ref(false)
let requestSerial = 0
const currentStatus = computed(() => String(row.value.STATUS || ''))
const recordNo = computed(() => String(row.value.ID || props.recordId || ''))
const targetStatus = computed(() => hasPendingMaintenance(row.value) ? '' : getLifecycleTarget(currentStatus.value, props.action))
const actionLabel = computed(() => proxy.selectDictLabel(master_change_type.value || [], props.action) || '状态变更')
const rules = { applyReason: [{ required: true, whitespace: true, message: '请输入申请原因', trigger: 'blur' }] }

function payload() {
  return { mode: props.action, recordId: String(props.recordId), parentId: null,
    applyReason: form.applyReason, values: {} }
}

async function save() {
  if (saving.value || loading.value || !targetStatus.value) return
  saving.value = true
  try {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) return
    await submitMasterChangeMaintenance(props.instanceId, payload())
    proxy.$modal.msgSuccess('提交成功')
    emit('saved')
    close()
  } catch {
    // 保留当前输入，供用户修正后重试。
  } finally {
    saving.value = false
  }
}

function close() {
  requestSerial++
  emit('update:modelValue', false)
}

watch(() => props.modelValue, async visible => {
  if (!visible) return
  const serial = ++requestSerial
  row.value = {}
  form.applyReason = ''
  loading.value = true
  try {
    const response = await getMasterRecordDetail(props.instanceId, props.recordId, props.modelId)
    if (serial !== requestSerial) return
    row.value = response?.data?.row || {}
  } catch {
    if (serial === requestSerial) close()
  } finally {
    if (serial === requestSerial) loading.value = false
  }
}, { immediate: true })
</script>
