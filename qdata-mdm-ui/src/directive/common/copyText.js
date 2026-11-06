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

/**
 * v-copyText copies text content
 * Copyright (c) 2022 qdata
 */

export default {
  beforeMount(el, { value, arg }) {
    if (arg === 'callback') {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
      const handler = () => {
        copyTextToClipboard(el.$copyValue);
        if (el.$copyCallback) {
          el.$copyCallback(el.$copyValue);
        }
      };
      el.addEventListener('click', handler);
      el.$destroyCopy = () => el.removeEventListener('click', handler);
    }
  }
};

function copyTextToClipboard(input, { target = document.body } = {}) {
  const element = document.createElement('textarea');
  const previouslyFocusedElement = document.activeElement;

  element.value = input;

  // Prevent keyboard from showing on mobile
  element.setAttribute('readonly', '');

  element.style.contain = 'strict';
  element.style.position = 'absolute';
  element.style.left = '-9999px';
  element.style.fontSize = '12pt'; // Prevent zooming on iOS

  const selection = document.getSelection();
  const originalRange = selection.rangeCount > 0 && selection.getRangeAt(0);

  target.append(element);
  element.select();

  // Explicit selection workaround for iOS
  element.selectionStart = 0;
  element.selectionEnd = input.length;

  let isSuccess = false;
  try {
    isSuccess = document.execCommand('copy');
  } catch {}

  element.remove();

  if (originalRange) {
    selection.removeAllRanges();
    selection.addRange(originalRange);
  }

  // Get the focus back on the previously focused element, if any
  if (previouslyFocusedElement) {
    previouslyFocusedElement.focus();
  }

  return isSuccess;
}
