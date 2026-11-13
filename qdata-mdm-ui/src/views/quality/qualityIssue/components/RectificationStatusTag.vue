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
  <dict-tag :type="tagType">{{ value || '-' }}</dict-tag>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({value: {type: String, default: ''}});

// 保留整改情况原文及处理人信息，列表和详情使用相同的状态配色。
const tagType = computed(() => ({
  '未下发': 'info',
  '未下发整改': 'info',
  '待处理': 'warning',
  '已下发': 'primary',
  '处理中': 'primary',
  '整改中': 'primary',
  '待验收': 'warning',
  '已完成': 'success',
  '已解决': 'success',
  '已关闭': 'info',
  '已取消': 'info'
})[String(props.value || '').split('·')[0].trim()] || 'info');
</script>
