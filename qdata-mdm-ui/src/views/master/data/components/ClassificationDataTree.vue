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
  <qt-tree ref="treeRef" :dept-options="nodes" v-model:dept-name="keyword" :tree-props="treeProps"
            :default-expanded-keys="expandedKeys"
            :action-loading="loading" :use-builtin-cat-edit-dialog="false"
            wrap-height-class=".master-authority-data-page .qt-wrap"
            title="分类模型" placeholder="请输入分类名称" @node-click="node => emit('select', node)"
  >
    <template #icon="{ data }">
      <span v-if="data.kind === 'field'" class="classification-icon classification-icon--model" aria-hidden="true"></span>
      <svg-icon v-else class="classification-icon" icon-class="file-list-2-line" />
    </template>
  </qt-tree>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import QtTree from '@/components/QtTree/index.vue'
import { useDict } from '@/utils/dict'

const props = defineProps({
  groups: { type: Array, default: () => [] },
  selectedId: { type: String, default: '__all__' },
  loading: Boolean
})
const emit = defineEmits(['select'])
const { master_data_status } = useDict('master_data_status')
const treeRef = ref(null)
const keyword = ref('')
const treeProps = { label: 'label', children: 'children' }
const nodes = computed(() => displayNodes(props.groups))
const expandedKeys = computed(() => props.groups.map(group => group.id))
watch([nodes, () => props.selectedId], async () => {
  await nextTick()
  treeRef.value?.setCurrentKey(props.selectedId)
}, { immediate: true })
function statusLabel(status) {
  return master_data_status.value.find(item => String(item.value) === String(status))?.label || '非生效'
}
function displayNodes(nodes) {
  return (nodes || []).map(node => ({
    ...node,
    label: node.status && node.status !== '1' ? `${node.label}（${statusLabel(node.status)}）` : node.label,
    children: displayNodes(node.children)
  }))
}
</script>

<style scoped>
.classification-icon {
  width: 18px;
  height: 18px;
  flex: none;
  margin-right: 6px;
  color: #ffb800;
}
.classification-icon--model {
  background-color: var(--el-color-primary);
  mask: url('@/assets/common/icons/model/model.svg') center / contain no-repeat;
}
</style>
