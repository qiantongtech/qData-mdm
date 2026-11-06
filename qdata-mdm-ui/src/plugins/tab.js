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

import useTagsViewStore from '@/store/system/tagsView';
import router from '@/router';

export default {
  // Refresh the current tab page
  refreshPage(obj) {
    const { path, query, matched } = router.currentRoute.value;
    if (obj === undefined) {
      matched.forEach((m) => {
        if (m.components && m.components.default && m.components.default.name) {
          if (!['Layout', 'ParentView'].includes(m.components.default.name)) {
            obj = { name: m.components.default.name, path: path, query: query };
          }
        }
      });
    }
    return useTagsViewStore()
      .delCachedView(obj)
      .then(() => {
        const { path, query } = obj;
        router.replace({
          path: '/redirect' + path,
          query: query
        });
      });
  },
  // Close the current tab and open a new tab
  closeOpenPage(obj) {
    useTagsViewStore().delView(router.currentRoute.value);
    if (obj !== undefined) {
      return router.push(obj);
    }
  },
  // Close the specified tab
  closePage(obj) {
    if (obj === undefined) {
      return useTagsViewStore()
        .delView(router.currentRoute.value)
        .then(({ visitedViews }) => {
          const latestView = visitedViews.slice(-1)[0];
          if (latestView) {
            return router.push(latestView.fullPath);
          }
          return router.push('/');
        });
    }
    return useTagsViewStore().delView(obj);
  },
  // Close all tabs
  closeAllPage() {
    return useTagsViewStore().delAllViews();
  },
  // Close the left tab
  closeLeftPage(obj) {
    return useTagsViewStore().delLeftTags(obj || router.currentRoute.value);
  },
  // Close the right tab
  closeRightPage(obj) {
    return useTagsViewStore().delRightTags(obj || router.currentRoute.value);
  },
  // Close other tabs
  closeOtherPage(obj) {
    return useTagsViewStore().delOthersViews(obj || router.currentRoute.value);
  },
  // Open tab
  openPage(url) {
    return router.push(url);
  },
  // Modify tabs
  updatePage(obj) {
    return useTagsViewStore().updateVisitedView(obj);
  }
};
