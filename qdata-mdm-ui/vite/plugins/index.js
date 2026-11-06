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

import vue from '@vitejs/plugin-vue';

import createAutoImport from './auto-import';
import createSvgIcon from './svg-icon';
import createCompression from './compression';
import createSetupExtend from './setup-extend';
// import { visualizer } from "rollup-plugin-visualizer";

export default function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins = [vue()];
  vitePlugins.push(createAutoImport());
  vitePlugins.push(createSetupExtend());
  vitePlugins.push(createSvgIcon(isBuild));
  // vitePlugins.push(
  //   visualizer({
  //     gzipSize: true,
  //     brotliSize: true,
  //     filename: "err.html", // The stats.html file is generated in the project root directory by default and can be customized
  //     open: true, //Automatically open the browser for viewing after generation
  //   }),
  // );
  isBuild && vitePlugins.push(...createCompression(viteEnv));
  return vitePlugins;
}
