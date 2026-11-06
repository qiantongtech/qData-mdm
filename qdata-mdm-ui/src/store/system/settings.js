/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

import defaultSettings from '@/settings';
import { useDynamicTitle } from '@/utils/dynamicTitle';
import { getStyleVariable } from '@/utils/theme';

const { sideTheme, showSettings, topNav, tagsView, fixedHeader, sidebarLogo, dynamicTitle } =
  defaultSettings;

const storageSetting = JSON.parse(localStorage.getItem('layout-setting')) || '';

const useSettingsStore = defineStore('settings', {
  state: () => ({
    title: '',
    theme: storageSetting.theme || getStyleVariable('--qt-primary', '#2666fb'),
    sideTheme: storageSetting.sideTheme || sideTheme,
    showSettings: showSettings,
    topNav: storageSetting.topNav === undefined ? topNav : storageSetting.topNav,
    tagsView: storageSetting.tagsView === undefined ? tagsView : storageSetting.tagsView,
    fixedHeader:
      storageSetting.fixedHeader === undefined ? fixedHeader : storageSetting.fixedHeader,
    sidebarLogo:
      storageSetting.sidebarLogo === undefined ? sidebarLogo : storageSetting.sidebarLogo,
    dynamicTitle:
      storageSetting.dynamicTitle === undefined ? dynamicTitle : storageSetting.dynamicTitle
  }),
  actions: {
    // Modify layout settings
    changeSetting(data) {
      const { key, value } = data;
      if (this.hasOwnProperty(key)) {
        this[key] = value;
      }
    },
    // Set page title
    setTitle(title) {
      this.title = title;
      useDynamicTitle();
    }
  }
});

export default useSettingsStore;
