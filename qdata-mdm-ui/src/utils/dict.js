/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This file is part of qData-mdm Master Data Platform (Open Source Edition).
 *
 * qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
 * You may use qData-mdm for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData-mdm logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData-mdm as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import useDictStore from '@/store/system/dict';
import { getDicts } from '@/api/system/system/dict/data';
import { i18n } from '@/plugins/vueI18n';

function translateLabel(dictType, value, fallback) {
  const key = `dict.${dictType}.${value}`;
  const translated = i18n.global.t(key);
  return translated !== key ? translated : fallback;
}

function translateRemark(dictType, value, fallback) {
  const key = `dict.${dictType}.remark.${value}`;
  const translated = i18n.global.t(key);
  return translated !== key ? translated : fallback;
}

function mapAndTranslate(dictType, items) {
  return items.map((p) => ({
    ...p,
    label: translateLabel(dictType, p.value, p.label),
    remark: translateRemark(dictType, p.value, p.label)
  }));
}

// 同一字典在多个组件中同时使用时，共用请求；失败后允许重新加载。
const pendingDictRequests = new Map();

/** 加载系统字典，供需要等待字典后再初始化表单的页面使用。 */
export function loadDict(dictType) {
  const store = useDictStore();
  const cached = store.getDict(dictType);
  if (cached) return Promise.resolve(mapAndTranslate(dictType, cached));
  if (!pendingDictRequests.has(dictType)) {
    const request = getDicts(dictType).then(resp => {
      const items = resp.data.map(p => ({
        label: p.dictLabel,
        value: p.dictValue,
        elTagType: p.listClass,
        elTagClass: p.cssClass,
        remark: p.remark
      }));
      store.setDict(dictType, items);
      return items;
    }).finally(() => pendingDictRequests.delete(dictType));
    pendingDictRequests.set(dictType, request);
  }
  return pendingDictRequests.get(dictType).then(items => mapAndTranslate(dictType, items));
}

/** 字典选项异步更新，模板和 computed 均可直接使用。 */
export function useDict(...args) {
  const res = ref({});
  args.forEach(dictType => {
    const cached = useDictStore().getDict(dictType);
    res.value[dictType] = cached ? mapAndTranslate(dictType, cached) : [];
    if (!cached) {
      loadDict(dictType).then(items => {
        res.value[dictType] = items;
      }).catch(() => {
        // 请求失败由统一请求拦截器提示；保持空选项，不使用本地常量回退。
      });
    }
  });
  return toRefs(res.value);
}
