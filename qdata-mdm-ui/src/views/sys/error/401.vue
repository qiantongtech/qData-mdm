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
  <div class="errPage-container">
    <qt-wrap :config="{ fullContent: false, actions: { show: false } }">
      <el-button icon="arrow-left" class="pan-back-btn" @click="back">
        {{ td('common.button.return') }}
      </el-button>
      <el-row>
        <el-col :span="12">
          <h1 class="text-jumbo text-ginormous">
            {{ td('common.error.code401') }}
          </h1>
          <h2>{{ td('common.error.notAccessRights') }}</h2>
          <h6>{{ td('common.error.Sorry401') }}</h6>
          <ul class="list-unstyled">
            <li class="link-type">
              <router-link to="/">
                {{ td('common.button.return') }}
              </router-link>
            </li>
          </ul>
        </el-col>
        <el-col :span="12">
          <img :src="errGif" width="313" height="428" alt="Girl has dropped her ice cream." />
        </el-col>
      </el-row>
    </qt-wrap>
  </div>
</template>

<script setup>
import useDefaultLang from '@/composables/useDefaultLang';
import errImage from '@/assets/common/images/error/img-no-access-permission.gif';

const { td } = useDefaultLang();

let { proxy } = getCurrentInstance();

const errGif = ref(errImage + '?' + +new Date());

function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: '/' });
  } else {
    proxy.$router.go(-1);
  }
}
</script>

<style lang="scss" scoped>
.errPage-container {
  width: 800px;
  max-width: 100%;
  margin: 100px auto;
  .pan-back-btn {
    background: #008489;
    color: var(--qt-text-inverse);
    border: none !important;
  }
  .pan-gif {
    margin: 0 auto;
    display: block;
  }
  .pan-img {
    display: block;
    margin: 0 auto;
    width: 100%;
  }
  .text-jumbo {
    font-size: 60px;
    font-weight: var(--qt-font-weight-bold);
    color: #484848;
  }
  .list-unstyled {
    font-size: var(--qt-font-size-base);
    li {
      padding-bottom: 5px;
    }
    a {
      color: #008489;
      text-decoration: none;
      &:hover {
        text-decoration: underline;
      }
    }
  }
}
</style>
