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

import useLocaleStore from '@/store/system/locale';

const modules = import.meta.glob(
  '@/assets/modules/sys/images/locales/**/*.{png,jpg,jpeg,svg,webp,gif}',
  { eager: true, import: 'default' }
);

const findBySuffix = (suffix) => {
  for (const [key, url] of Object.entries(modules)) {
    if (key.endsWith(suffix)) return url;
  }
  return '';
};

export function useLocaleImage() {
  const localeStore = useLocaleStore();

  const getImage = (path) => {
    const lang = localeStore.getCurrentLocale.lang;
    return findBySuffix(`/${lang}/${path}`) || findBySuffix(`/zh-CN/${path}`) || '';
  };

  return { getImage };
}
