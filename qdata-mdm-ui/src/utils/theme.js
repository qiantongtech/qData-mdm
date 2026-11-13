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

// Canvas and graph renderers need resolved colors instead of CSS var() strings.
export function getStyleVariable(name, fallback = '') {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

// Keep the existing color setting connected to the shared style variables.
export function handleThemeStyle(theme) {
  const style = document.documentElement.style;
  const primaryVariables = {
    '--qt-primary': theme,
    '--qt-primary-rgb': hexToRgb(theme).join(', '),
    '--qt-primary-hover': getLightColor(theme, 0.3),
    '--qt-primary-active': getDarkColor(theme, 0.2),
    '--qt-primary-light': getLightColor(theme, 0.9),
    '--qt-primary-disabled': getLightColor(theme, 0.5),
    '--qt-element-fixed-primary-light-7': getLightColor(theme, 0.7),
    '--qt-element-fixed-primary-light-8': getLightColor(theme, 0.8)
  };
  // Read the stylesheet defaults so custom hover/active colors remain effective.
  Object.keys(primaryVariables).forEach((name) => style.removeProperty(name));
  const defaultPrimary = getStyleVariable('--qt-primary', theme);
  if (theme.toLowerCase() !== defaultPrimary.toLowerCase()) {
    Object.entries(primaryVariables).forEach(([name, value]) => style.setProperty(name, value));
  }

  const lightVariables = {
    3: '--qt-primary-hover',
    5: '--qt-primary-disabled',
    7: '--qt-element-fixed-primary-light-7',
    8: '--qt-element-fixed-primary-light-8',
    9: '--qt-primary-light'
  };
  style.setProperty('--el-color-primary', 'var(--qt-primary)');
  style.setProperty('--el-color-primary-rgb', 'var(--qt-primary-rgb)');
  for (let i = 1; i <= 9; i++) {
    style.setProperty(
      `--el-color-primary-light-${i}`,
      lightVariables[i] ? `var(${lightVariables[i]})` : getLightColor(theme, i / 10)
    );
  }
  for (let i = 1; i <= 9; i++) {
    style.setProperty(
      `--el-color-primary-dark-${i}`,
      i === 2 ? 'var(--qt-primary-active)' : getDarkColor(theme, i / 10)
    );
  }
}

// hex color to rgb color
export function hexToRgb(str) {
  str = str.replace('#', '');
  let hexs = str.match(/../g);
  for (let i = 0; i < 3; i++) {
    hexs[i] = parseInt(hexs[i], 16);
  }
  return hexs;
}

// rgb color to hex color
export function rgbToHex(r, g, b) {
  let hexs = [r.toString(16), g.toString(16), b.toString(16)];
  for (let i = 0; i < 3; i++) {
    if (hexs[i].length == 1) {
      hexs[i] = `0${hexs[i]}`;
    }
  }
  return `#${hexs.join('')}`;
}

// Lighten color value
export function getLightColor(color, level) {
  let rgb = hexToRgb(color);
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor((255 - rgb[i]) * level + rgb[i]);
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2]);
}

// Darken color value
export function getDarkColor(color, level) {
  let rgb = hexToRgb(color);
  for (let i = 0; i < 3; i++) {
    rgb[i] = Math.floor(rgb[i] * (1 - level));
  }
  return rgbToHex(rgb[0], rgb[1], rgb[2]);
}
