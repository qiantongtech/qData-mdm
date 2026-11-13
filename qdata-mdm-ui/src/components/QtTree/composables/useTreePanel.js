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

/*
 * useTreePanel — QtTree 面板容器层可组合函数
 *
 * 负责：
 *   1) 左侧面板宽度拖拽（带 16ms 节流 mousemove）
 *   2) 折叠/展开图标切换（0 ↔ 280px）
 *   3) 容器高度：优先使用 props.wrapHeight 父组件传入；否则按 .qt-wrap/.el-main 真实 DOM 计算
 *   4) 所有全局事件监听在组件卸载时自动清理，避免内存泄漏
 *
 * 使用：
 *   const {
 *     localLeftWidth, qtWrapheight, isResizing,
 *     startResize, toggleCollapse,
 *   } = useTreePanel({ props, emit })
 *
 * 高度优先级（从高到低）：
 *   props.wrapHeight（父组件显式传入）
 *   → DOM 测量：.qt-wrap 或 .el-main 的 offsetHeight + headerOffset
 *   → 默认兜底："86vh"
 */

import { ref, watch, onMounted, onUnmounted } from 'vue';

/**
 * 节流函数（throttle）：指定时间内只触发一次
 * 用于拖拽（16ms ≈ 一帧）、高度计算（100ms）等高频场景
 */
function throttle(fn, wait = 16) {
  let lastTime = 0;
  let timer = null;
  return function (...args) {
    const now = Date.now();
    const remaining = wait - (now - lastTime);
    if (remaining <= 0) {
      if (timer) {
        clearTimeout(timer);
        timer = null;
      }
      lastTime = now;
      fn.apply(this, args);
    } else if (!timer) {
      timer = setTimeout(() => {
        lastTime = Date.now();
        timer = null;
        fn.apply(this, args);
      }, remaining);
    }
  };
}

export function useTreePanel({ props, emit }) {
  // ---------- 响应式状态 ----------
  // 本地维护的左侧宽度（避免直接改 prop，支持双向绑定同步）
  const localLeftWidth = ref(props.leftWidth ?? 280);
  // 是否正在拖拽
  const isResizing = ref(false);
  // 容器动态高度（绑定到 CSS 变量 --qt-wrap-height）
  const qtWrapheight = ref('86vh');

  // ---------- 内部私有变量 ----------
  let startX = 0;
  let throttledResizeHandler = null; // 拖拽过程的节流回调（复用同一引用便于 removeEventListener）
  let resizeObserver = null; // ResizeObserver 实例
  let throttledHeightHandler = null; // 高度变化的节流回调

  // ---------- 宽度同步 ----------
  // 外部 props.leftWidth 变化时同步到本地
  watch(
    () => props.leftWidth,
    (val) => {
      if (val !== undefined && val !== null) {
        localLeftWidth.value = val;
      }
    }
  );

  // ---------- 拖拽逻辑 ----------
  function startResize(event) {
    isResizing.value = true;
    startX = event.clientX;
    // 懒创建节流回调（同一组件实例复用，避免每拖一次新注册）
    if (!throttledResizeHandler) {
      throttledResizeHandler = throttle(updateResize, 16);
    }
    document.addEventListener('mousemove', throttledResizeHandler);
    document.addEventListener('mouseup', stopResize);
  }

  function stopResize() {
    isResizing.value = false;
    if (throttledResizeHandler) {
      document.removeEventListener('mousemove', throttledResizeHandler);
    }
    document.removeEventListener('mouseup', stopResize);
  }

  function updateResize(event) {
    if (isResizing.value) {
      const delta = event.clientX - startX;
      const newWidth = localLeftWidth.value + delta;
      localLeftWidth.value = newWidth < 0 ? 0 : newWidth;
      startX = event.clientX;
      emit('update:leftWidth', localLeftWidth.value);
    }
  }

  // ---------- 折叠/展开 ----------
  function toggleCollapse() {
    localLeftWidth.value = localLeftWidth.value === 0 ? 280 : 0;
    emit('update:leftWidth', localLeftWidth.value);
  }

  // ---------- 高度计算（父组件传入优先）----------
  function normalizeHeight(val) {
    if (val === undefined || val === null || val === '') return null;
    // 纯数字：单位为 px
    if (typeof val === 'number') return val + 'px';
    return String(val);
  }

  function applyHeightFromProps() {
    const explicit = normalizeHeight(props.wrapHeight);
    if (explicit) {
      qtWrapheight.value = explicit;
      return true;
    }
    return false;
  }

  function resolveHeightTarget() {
    if (props.wrapHeightClass) {
      const target = document.querySelector(props.wrapHeightClass);
      if (target) return target;
    }
    return document.querySelector('.qt-wrap') || document.querySelector('.el-main');
  }

  function getQtWrapHeight() {
    // 父组件显式传了 wrapHeight → 直接用
    if (applyHeightFromProps()) return;

    const element = resolveHeightTarget();
    if (element) {
      const offset = typeof props.headerOffset === 'number' ? props.headerOffset : 0;
      qtWrapheight.value = element.offsetHeight + offset + 'px';
    } else {
      qtWrapheight.value = '86vh';
    }
  }

  // 监听 wrapHeight 变化：父组件动态切换传入时立即应用
  watch(
    () => props.wrapHeight,
    () => {
      getQtWrapHeight();
    }
  );

  // 监听 headerOffset 变化：重新计算高度（仅在没传 wrapHeight 时真正生效）
  if (props.headerOffset !== undefined) {
    watch(
      () => props.headerOffset,
      () => {
        getQtWrapHeight();
      }
    );
  }

  // ---------- 生命周期：挂载监听 + 卸载清理 ----------
  onMounted(() => {
    // 立即计算一次高度（不走节流）
    getQtWrapHeight();

    // 已显式传入 wrapHeight → 不再注册 ResizeObserver / window resize，
    // 高度完全由父组件控制
    if (normalizeHeight(props.wrapHeight)) return;

    // 创建高度变化的节流版本（100ms）
    throttledHeightHandler = throttle(getQtWrapHeight, 100);

    // ResizeObserver 监听 .qt-wrap（优先）或 .el-main 的尺寸变化
    const targetElement = resolveHeightTarget();
    if (targetElement) {
      try {
        resizeObserver = new ResizeObserver(() => {
          throttledHeightHandler();
        });
        resizeObserver.observe(targetElement);
      } catch (e) {
        // ResizeObserver 不可用时降级为只监听 window resize
        void e;
      }
    }

    // 监听浏览器窗口大小变化
    window.addEventListener('resize', throttledHeightHandler);
  });

  onUnmounted(() => {
    // 清理 ResizeObserver
    if (resizeObserver) {
      try {
        const targetElement = resolveHeightTarget();
        if (targetElement) {
          resizeObserver.unobserve(targetElement);
        }
      } catch (e) {
        void e;
        /* ignore */
      }
      resizeObserver.disconnect();
      resizeObserver = null;
    }
    // 清理窗口 resize
    if (throttledHeightHandler) {
      window.removeEventListener('resize', throttledHeightHandler);
      throttledHeightHandler = null;
    }
    // 兜底：如果组件卸载时还在拖拽中，清理全局监听
    stopResize();
  });

  return {
    localLeftWidth,
    qtWrapheight,
    isResizing,
    startResize,
    stopResize,
    toggleCollapse,
    getQtWrapHeight,
    // 内部工具函数：供其他 composables 复用节流器
    throttle
  };
}
