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
  <div
      class="sidebar-logo-container"
      :class="{ collapse: collapse }"
      :style="{
      backgroundColor:
        sideTheme === 'theme-dark'
          ? 'var(--qt-sidebar-menu-background)'
          : 'var(--qt-sidebar-menu-light-background)'
    }"
  >
    <transition name="sidebarLogoFade">
      <router-link v-if="collapse" key="collapse" class="sidebar-logo-link" to="/">
        <!--        <img v-if="logo" :src="simpLogo" class="sidebar-logo" />-->
        <img v-if="logo" :src="displaySimpLogo" class="sidebar-logo sidebar-logo-width"/>
        <h1 v-else class="sidebar-title" :style="{ color: 'var(--qt-text-inverse)' }">
          {{ title }}
        </h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <!--        <img v-if="logo" :src="logo" class="sidebar-logo" /> -->
        <img
            v-if="logo"
            :src="displayLogo"
            ref="logoRef"
            class="sidebar-logo"
            :class="{ 'logo-intro': logoIntroActive }"
            @mouseenter="replayHover"
        />
      </router-link>
    </transition>
  </div>
</template>

<script setup>
import logo from '@/assets/common/images/logo/logo-qdata-mdm-white.png';
import logo1 from '@/assets/common/images/logo/logo-qdata-mdm-grey.png';
import simpLogo from '@/assets/common/images/logo/logo-qdata-mdm-sim.png'; //Thousands

import useSettingsStore from '@/store/system/settings';
import defaultSettings from '@/settings';
import {getContent} from '@/api/system/system/content';

import {computed, ref as vueRef, nextTick} from 'vue';

// Use ref to create a responsive logo
const refLogo = ref(null); // Initialize logo to simpLogo.png
const refSimpLogo = ref(null); // Initialize logo to simpLogo.png

// animation control
const logoIntroActive = ref(false);

const props = defineProps({
  collapse: {
    type: Boolean,
    required: true
  },
  currentRoute: {
    type: String,
    default: '/'
  }
});

const displayLogo = computed(() => {
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  const isSpecialRoute = navbarLogoRoutes.some((logoPath) =>
      props.currentRoute.startsWith(logoPath)
  );
  return isSpecialRoute ? logo1 : refLogo.value;
});

const displaySimpLogo = computed(() => {
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  const isSpecialRoute = navbarLogoRoutes.some((logoPath) =>
      props.currentRoute.startsWith(logoPath)
  );
  return isSpecialRoute ? logo1 : refSimpLogo.value;
});
onMounted(() => {
  logoIntroActive.value = true;
  window.setTimeout(() => {
    logoIntroActive.value = false;
  }, 1800);
  fetchContent();
});
// Use getContent to get data instead of redefining a getContent function
const fetchContent = async () => {
  try {
    const res = await getContent(1);
    if (res.code == 200) {
      const data = res.data;
      const sysLogo = data.logo;
      refLogo.value = sysLogo ? sysLogo : logo;
      refSimpLogo.value = sysLogo ? sysLogo : simpLogo;
    }

    // this.$message.success('Content loaded successfully');
  } catch (error) {
    refLogo.value = logo;
    refSimpLogo.value = simpLogo;
  }
};

const title = import.meta.env.VITE_APP_TITLE;
const settingsStore = useSettingsStore();
const sideTheme = computed(() => settingsStore.sideTheme);
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 60px;
  line-height: 50px;
  text-align: center;
  overflow: hidden;

  & .logo-intro,
  &:hover .sidebar-logo {
    animation: logoEntrance 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94) forwards;
  }

  & .sidebar-logo-link {
    height: 100%;
    width: 100%;
    background-color: inherit;
    text-align: center;
    overflow: hidden;

    & .sidebar-logo {
      background-color: inherit;
      height: 48px;
      margin-top: 6px;
      margin-right: 12px;
      transform: scale(0.43);
      margin-left: -116px;
    }

    & .sidebar-logo {
      background-color: inherit;
      height: 48px;
      margin-top: 6px;
      margin-right: 12px;
      transform: scale(0.43);
      margin-left: -116px;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: var(--qt-text-inverse);
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family: Avenir,
      Helvetica Neue,
      Arial,
      Helvetica,
      sans-serif;
      vertical-align: middle;
    }
  }

  &.collapse {
    .sidebar-logo {
      height: 42px;
      margin-right: 0;
      margin-left: 0;
      margin-top: 9px;
    }

    .sidebar-logo-width {
      height: 60px !important;
      margin-top: 0 !important;
      margin-right: 0 !important;
      margin-left: 0 !important;
    }
  }
}

/* Entrance animation: Phase 1: Fade in and slide in + zoom → Phase 2: Zoom in place → Phase 3: Ball rebound */
@keyframes logoEntrance {
  /* Stage 1: Fade in from left while zooming from small to large */
  0% {
    transform: translateX(-200px) scale(0.1);
    opacity: 0;
  }
  35% {
    transform: translateX(0) scale(0.2);
    opacity: 1;
  }
  100% {
    transform: scale(0.43);
    opacity: 1;
  }
}
</style>
