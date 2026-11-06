<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This document is part of the qData Master Data Platform (open source version).

  qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
  You may use qData for commercial purposes, but you may not remove, hide,
  modify, or replace the qData master data platform logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData master data platform as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div ref="app-container" class="app-container system-dialog-host">
    <el-dialog
      class="importantNote-dialog"
      :title="td('sys.system.notice.platformName')"
      v-model="visible"
      @close="close"
      width="842px"
      :show-close="false"
      :close-on-click-modal="false"
      :append-to="$refs['app-container']"
      draggable
      destroy-on-close
    >
      <template #header>
        <div class="header-title">
          <div class="title-text">
            <img src="@/assets/modules/sys/images/notice/img-icon-ten.png" alt="" />
            <span>{{ td('sys.system.notice.platformName') }}</span>
          </div>
          <div class="close">
            <img src="@/assets/modules/sys/images/notice/action-close.png" alt="" @click="close" />
          </div>
        </div>
      </template>
      <div class="content">
        <div class="content-text" v-html="notice.noticeContentText" />
        <div class="content-line" />
        <div class="href" @click="openDocs">{{ td('sys.system.notice.getMoreInfo') }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="ImportantNote">
import useDefaultLang from '@/composables/useDefaultLang';

const { td } = useDefaultLang();
const visible = ref(false);
const notice = ref({});

/** Show popup */
function show(row) {
  visible.value = true;
  notice.value = row;
}
function close() {
  visible.value = false;
  notice.value = {};
}
function openDocs() {
  window.open('https://qdata.qiantong.tech', '_blank');
}

defineExpose({
  show
});
</script>

<style lang="scss">
.importantNote-dialog {
  padding: 0;
  border-radius: var(--qt-radius-medium);
  margin: auto;
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  bottom: 0;
  height: fit-content;
  &:not(.is-fullscreen) {
    margin: auto !important;
  }
  .el-dialog__header {
    padding: 0;
  }
  .header-title {
    height: 50px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: url('@/assets/modules/sys/images/notice/bg-head.png') no-repeat;
    background-size: 100% 100%;
    color: var(--qt-text-inverse);
    padding: 0 24px 0 35px;
    .title-text {
      display: flex;
      align-items: center;
      img {
        width: 40px;
        margin-right: 12px;
      }
      span {
        font-family: PingFang SC;
        font-weight: 800;
        font-size: 20px;
      }
    }
    .close {
      img {
        cursor: pointer;
        width: 18px;
      }
    }
  }
  .content {
    padding: 30px;
    p {
      margin: 10px 0 10px;
    }
    .content-text {
      min-height: 250px;
      max-height: 500px;
      overflow-y: auto;
      &::-webkit-scrollbar {
        width: 4px;
      }
    }
    .content-line {
      height: 1px;
      background: #eeeff0;
      margin: 16px 0;
    }
    .href {
      cursor: pointer;
      font-family: Microsoft YaHei;
      font-size: var(--qt-font-size-base);
      color: #108ee9;
      &:hover {
        color: var(--qt-primary);
      }
    }
  }
}
</style>

<style scoped lang="scss">
.system-dialog-host {
  margin: 0;
  padding: 0;
  min-height: 0;
}
</style>
