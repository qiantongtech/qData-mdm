<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This file is part of qData-mdm Master Data Platform (Open Source Edition).

  qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
  You may use qData-mdm for commercial purposes, but you may not remove, hide,
  modify, or replace the qData-mdm logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData-mdm as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div class="navbar" ref="navbar">
    <logo
      v-if="appStore.sidebar.hide && isOnlyLogoRoute"
      :collapse="false"
      class="navbar-logo"
      :current-route="route.path"
    />
    <hamburger
      id="hamburger-container"
      :is-active="appStore.sidebar.opened"
      class="hamburger-container"
      @toggleClick="toggleSideBar"
    />
    <breadcrumb
      id="breadcrumb-container"
      class="breadcrumb-container"
      v-if="!settingsStore.topNav"
    />
    <top-nav
      ref="topNavRef"
      id="topmenu-container"
      class="topmenu-container"
      v-if="settingsStore.topNav"
      :class="{ 'has-navbar-logo': appStore.sidebar.hide && isOnlyLogoRoute }"
    />
    <div class="right-menu">
      <template v-if="appStore.device !== 'mobile'">
        <el-dropdown
          id="language-select"
          class="right-menu-item hover-effect"
          @command="handleLanguageChange"
        >
          <div
            class="language-select-wrapper"
            style="height: 100%; display: flex; align-items: center; justify-content: center"
          >
            <svg-icon iconClass="i18n" style="font-size: 22px" />
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">
                简体中文
              </el-dropdown-item>
              <el-dropdown-item command="en-US" :disabled="locale === 'en-US'">
                English
              </el-dropdown-item>
              <el-dropdown-item command="ja-JP" :disabled="locale === 'ja-JP'">
                日本語
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="right-menu-item hover-effect" @click="openDocumentation">
          <svg-icon iconClass="bzzx" style="font-size: 18px" />
        </div>
        <!-- ------------------------------- Reporting for work ---------------------------------- -->
        <el-popover
          trigger="hover"
          popper-style="
                        width: 336px;
                        height: 360px;
                        background: #FFFFFF;
                        box-shadow: 0px 2px 8px 0px rgba(0,0,0,0.15);
                        padding:0;
                    "
        >
          <template #reference>
            <el-badge
              :value="msgCount"
              :max="99"
              class="badge"
              :class="msgCount > 0 ? 'flash' : ''"
              :offset="[0, 0]"
              :hidden="msgCount == 0"
            >
              <!-- <i class="iconfont right-menu-item hover-effect" style="font-size: 22px">&#xebe7;</i> -->
              <i
                class="iconfont icon-a-dingbulingdangxianxing right-menu-item hover-effect"
                style="font-size: 20px"
              ></i>
            </el-badge>
          </template>
          <template #default>
            <el-tabs v-model="activeMsg" stretch class="mag-tabs" @tab-click="handleClick">
              <el-tab-pane :label="t('sys.dashboard.messageReminder')" name="first">
                <div class="message-list">
                  <div
                    class="msg-item"
                    v-for="(msg, index) in messages"
                    :key="index"
                    v-show="messages.length > 0"
                  >
                    <img
                      class="icon"
                      src="@/assets/common/images/layout/msg/toolbar-icon1.png"
                      alt=""
                    />
                    <div class="content">
                      <div class="title">{{ msg.title }}</div>
                      <div class="time">{{ msg.time }}</div>
                    </div>
                  </div>
                  <el-empty
                    v-show="messages.length == 0 || messages == null || messages == 'undefined'"
                    :image-size="100"
                    :description="td('common.noMessage')"
                    class="empty-block"
                  />
                </div>
              </el-tab-pane>
              <el-tab-pane :label="t('sys.dashboard.notification')" name="second">
                <!--                <message-list :msg-category="'first'"></message-list>-->
                <div class="message-list">
                  <div
                    class="msg-item"
                    v-for="(msg, index) in noticeList"
                    :key="index"
                    v-show="msg.entityType == 1"
                    @click="handleMessage(msg)"
                  >
                    <img
                      class="icon"
                      src="@/assets/common/images/layout/msg/toolbar-icon1.png"
                      alt=""
                    />
                    <div class="content">
                      <div class="title">{{ msg.title }}</div>
                      <div class="time">{{ msg.time }}</div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane :label="t('sys.dashboard.announcement')" name="third">
                <!--                <message-list :msg-category="'second'"></message-list>-->
                <div class="message-list">
                  <div
                    class="msg-item"
                    v-for="(msg, index) in noticeList"
                    :key="index"
                    v-show="msg.entityType == 2"
                    @click="handleMessage(msg)"
                  >
                    <img
                      class="icon"
                      src="@/assets/common/images/layout/msg/toolbar-icon1.png"
                      alt=""
                    />
                    <div class="content">
                      <div class="title">{{ msg.title }}</div>
                      <div class="time">{{ msg.time }}</div>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
            <div class="msg-btns">
              <div class="btn-item" @click="clearNotification">
                {{ t('sys.dashboard.markAllRead') }}
              </div>
              <div class="btn-item" @click="messageDetail">{{ t('sys.dashboard.viewMore') }}</div>
            </div>
          </template>
        </el-popover>
        <div class="right-menu-item hover-effect" @click="handleRefreshClick">
          <!-- <el-icon size="22">
                        <Refresh />
                    </el-icon> -->
          <i class="iconfont icon-a-shuaxinxianxing" style="font-size: 20px"></i>
        </div>

        <header-search id="header-search" class="right-menu-item" />

        <screenfull id="screenfull" class="right-menu-item hover-effect" />

        <!-- <el-tooltip content="Layout size" effect="dark" placement="bottom">
                  <size-select id="size-select" class="right-menu-item hover-effect" />
                </el-tooltip> -->
      </template>
      <div class="avatar-container">
        <el-dropdown @command="handleCommand" class="right-menu-item hover-effect" trigger="click">
          <div class="avatar-wrapper">
            <img :src="userStore.avatar" class="user-avatar" />
            <span class="nickName">{{ nickName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>{{ t('sys.dashboard.personalCenter') }}</el-dropdown-item>
              </router-link>
              <el-dropdown-item command="setLayout" v-if="settingsStore.showSettings">
                <span>{{ t('sys.dashboard.layoutSettings') }}</span>
              </el-dropdown-item>
              <!--               <el-dropdown-item command="about">-->
              <!--                                <span>{{ t('sys.dashboard.aboutUs') }}</span>-->
              <!--                            </el-dropdown-item>-->
              <el-dropdown-item divided command="logout">
                <span>{{ t('sys.dashboard.logout') }}</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-dialog
      :title="t('sys.dashboard.aboutUs')"
      class="about-dialog"
      v-model="activeOpen"
      append-to-body
      align-center
    >
      <div class="about-content-wrapper">
        <img
          src="@/assets/modules/sys/images/login/logo-qdata-mdm.png"
          alt="qData 主数据平台 Logo"
          class="logo"
        />
        <div class="about-title">
          {{ t('sys.dashboard.version') }}:v{{ version }}
          <!-- <span class="version-badge"></span> -->
        </div>
        <div class="copyright">
          ©{{ year }}{{ td('login.info.companyName') }}{{ td('login.info.copyrightOwner') }}
        </div>
      </div>

      <template #footer>
        <div class="about-footer">
          <div v-if="!needUpdate" class="status-text">
            {{ t('sys.dashboard.version') }}{{ version }}{{ t('sys.dashboard.isLatestVersion') }}
          </div>
          <div v-else class="status-text">
            {{ t('sys.dashboard.latestVersion') }}:
            <a
              href="javascript:void(0)"
              @click.prevent="openUpdateLog"
              rel="noopener noreferrer"
              class="update-link"
            >
              v{{ latestVersion }}
            </a>
          </div>
          <div class="head-btns">
            <el-button type="primary" @click="openUpdateLog">
              {{ t('sys.dashboard.updateLog') }}</el-button
            >
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Navbar">
import { useI18n } from 'vue-i18n';
import { ElMessageBox } from 'element-plus';
import Breadcrumb from '@/components/Breadcrumb';
import TopNav from '@/components/TopNav';
import Hamburger from '@/components/Hamburger';
import Logo from './Sidebar/Logo';
import Screenfull from '@/components/Screenfull';
import SizeSelect from '@/components/SizeSelect';
import HeaderSearch from '@/components/HeaderSearch';
import useAppStore from '@/store/system/app';
import useDefaultLang from '@/composables/useDefaultLang';
const { td } = useDefaultLang();
import useUserStore from '@/store/system/user';
import useSettingsStore from '@/store/system/settings';
import useTagsViewStore from '@/store/system/tagsView';
import defaultSettings from '@/settings';
import { getNum, listMessage, readAll } from '@/api/system/system/message/message';
import { loginOut } from '@/api/system/sso-auth.js';
// import MessageList from "@/views/sys/system/message/components/messageList.vue";
import { onMounted, ref, watch } from 'vue';
import moment from 'moment';
import { listNotice } from '@/api/system/system/notice';
import { da, id } from 'element-plus/es/locale/index.mjs';
import usePermissionStore from '@/store/system/permission';
import { useLocale } from '@/composables/useLocale';
import useLocaleStore from '@/store/system/locale.js';
import packageInfo from '../../../package.json';

const { t, locale } = useI18n();
const { changeLocale } = useLocale();
const localeStore = useLocaleStore();
import { getCurrentAppVersion, addVersionTrack } from '@/api/system/update/update.js';
// Authentication mode
const authType = import.meta.env.VITE_APP_AUTH_TYPE;

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const { proxy } = getCurrentInstance();
const visitedViews = computed(() => useTagsViewStore().visitedViews);
const isOnlyLogoRoute = computed(() => {
  const navbarLogoRoutes = defaultSettings.navbarLogoRoutes || [];
  return navbarLogoRoutes.some((logoPath) => route.path.startsWith(logoPath));
});

const nickName = computed(() => {
  const name = userStore.nickName;
  const userId = userStore.id;
  if (userId === 1) {
    return td('common.texts.superAdmin');
  }
  return name;
});
// Message type selected by default
const activeMsg = ref('first');
const permissionStore = usePermissionStore();

const needUpdate = ref(false);
const currentVersion = ref('');
const latestVersion = ref('');
const { name, version, description, author } = packageInfo;
// All routing information
const routers = computed(() => permissionStore.topbarRouters);
//-----------------------The following job application contents-------------------------
const handleMessage = (msg) => {
  console.log('Received message:', msg);
  router.push({
    path: '/sys/system/notice/detail',
    query: { id: msg.noticeId }
  });
};
// Number of message notifications
const msgCount = ref(0);
const messages = ref([]);
const noticeList = ref([]);
const sessionValue = ref(null);
getMessageNum(); // Mainly get news for the first time

const wsUri =
  (import.meta.env.VITE_APP_WEBSOCKET_API ||
    `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`) +
  '/websocket/message/' +
  userStore.userId;
// Establish socket connection
const ws = new WebSocket(wsUri);

const initWebSocket = () => {
  console.log('---------initWebSocket-------------');

  //Inquiry notification announcement
  listNotice().then((response) => {
    console.log('---------- response.rows-------------', response);
    response.rows.forEach((item) => {
      item.title = item.noticeTitle;
      item.entityType = item.noticeType;
      item.time =
        item.updateTime != undefined && item.updateTime != null
          ? formatTimestamp(item.updateTime)
          : formatTimestamp(item.createTime);
    });
    noticeList.value = response.rows;
  });

  //Query unread message notifications
  listMessage({
    receiverId: userStore.userId,
    hasRead: 0,
    pageNum: 1,
    pageSize: 1000
  }).then((response) => {
    response.data.rows?.forEach((item) => {
      item.time = item.updateTime;
      item.entityType = item.category;
      // item.title = item.title
    });
    messages.value = [...response.data.rows, ...messages.value];
    msgCount.value = messages.value ? messages.value.length : 0;
    console.log('------messages.value----', messages.value);
  });
  ws.onmessage = (event) => {
    // Server push data
    // console.log('====Server push data==========>',event.data)
    const messageData = JSON.parse(event.data);
    console.log('===Monitored messageData=========>', messageData);
    if (messageData) {
      messageData.time =
        messageData.updateTime != undefined && messageData.updateTime != null
          ? formatTimestamp(messageData.updateTime)
          : formatTimestamp(messageData.createTime);
      // messages.value.push(messageData)
      messages.value = [messageData, ...messages.value];
    }
    console.log('===Stored messages=========>', messages.value);
    // Message count update
    msgCount.value = messages.value ? messages.value.length : 0;
  };
};
onMounted(() => {
  initWebSocket();
  console.log(userStore);

  /*getCurrentAppVersion().then((res) => {
    if (res.data != null) {
      // Is it the latest version?
      needUpdate.value = res.data.needUpdate;
      //Local version number
      currentVersion.value = res.data.currentVersion;
      //Latest version number
      latestVersion.value = res.data.latestVersion;
    }
  });*/
});
// Page logout
onBeforeUnmount(() => {
  console.log('------Page logout----');
  ws.close(); // close socket
});

// Format the timestamp in YYYY-MM-DD HH:mm:ss format
function formatTimestamp(timestamp) {
  const date = new Date(timestamp);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0'); // Month starts from 0
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

// Message query
function getMessageNum() {
  getNum();
}

// tab-click event handler
const handleClick = (tab) => {
  console.log('Currently selected tab:', tab.props); // tab is an object containing information about the currently clicked tab
  const label = tab.props.label;
  activeMsg.value = tab.props.name;
};
// Help documentation
function openDocumentation() {
  const lang = localeStore.getCurrentLocale.lang || locale.value;
  const localePathMap = {
    'en-US': '/en',
    'ja-JP': '/ja'
  };
  const localePath = localePathMap[lang] || '';
  window.open(
    `https://community.qdata.tech${localePath}/docs/overview/introduction.html`,
    '_blank'
  );
}
function toggleSideBar() {
  appStore.toggleSideBar();
}

async function handleLanguageChange(lang) {
  if (lang === localeStore.getCurrentLocale.lang) return;
  localeStore.setCurrentLocale({ lang });
  await changeLocale(lang);
  window.location.reload();
}

const activeOpen = ref(false);

function handleAboutUs() {
  activeOpen.value = true;
}

function openUpdateLog() {
  addVersionTrack({
    name,
    currVersion: version,
    description,
    author
  }).catch((error) => {
    console.error('Add version track failed:', error);
  });
  window.open('https://gitee.com/qiantongtech/qData/releases', '_blank');
}

function handleCommand(command) {
  switch (command) {
    case 'setLayout':
      setLayout();
      break;
    case 'logout':
      logout();
      break;
    case 'about':
      // Jump to about us page
      // window.open('https://qiantong.tech/', '_blank');
      handleAboutUs();
      break;
    default:
      break;
  }
}

function logout() {
  ElMessageBox.confirm(t('sys.dashboard.confirmLogout'), t('common.message.prompt'), {
    confirmButtonText: t('common.button.confirm'),
    cancelButtonText: t('common.button.cancel'),
    type: 'warning'
  })
    .then(() => {
      userStore.logOut().then(() => {
        if (authType === 'sso') {
          // Exit the login status of the Unified Certification Center
          loginOut(userStore.userId).then(() => {
            location.href = '/index';
          });
        } else {
          location.href = '/index';
        }
      });
    })
    .catch(() => {});
}

const emits = defineEmits(['setLayout']);

function setLayout() {
  emits('setLayout');
}

function handleRefreshClick() {
  const activeView = visitedViews.value.find((view) => view.path === route.path);
  proxy.$tab.refreshPage(activeView);
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route);
  }
}

function messageDetail() {
  if (activeMsg.value == 'first') {
    router.push({ path: '/sys/system/bases/message' });
  } else {
    router.push({ path: '/sys/notice' });
  }
}

function clearNotification() {
  readAll().then(() => {
    messages.value = [];
    msgCount.value = 0;
    ElMessage.success(t('sys.report.markAllReadDone'));
  });
}
</script>

<style lang="scss" scoped>
.ellipsis-option {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.custom-option-style .el-select-dropdown__item {
  display: flex;
  align-items: center;
}

::v-deep {
  .el-select__wrapper {
    box-shadow: 0 0 0 1px var(--qt-border-color) inset;
    border-radius: var(--qt-radius-small) !important;
  }
}

.message-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
}

.msg-item {
  cursor: pointer;
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 16px;
  margin-bottom: 10px;
  background: #f9f9f9;
  border-radius: var(--qt-radius-medium);
}

.icon {
  width: 34px;
  height: 34px;
  margin-right: 12px;
}

.content {
  .title {
    font-size: var(--qt-font-size-base);
    font-weight: var(--qt-font-weight-medium);
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 6px;
  }

  .time {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.navbar {
  height: 60px;
  overflow: hidden;
  position: relative;
  background: var(--qt-surface-background);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  text-align: center;
  line-height: 60px;

  .navbar-logo {
    float: left;
    width: 200px !important;
    height: 100% !important;
    background-color: transparent !important;

    ::v-deep.sidebar-logo-link {
      background-color: transparent !important;
    }

    ::v-deep.sidebar-logo {
      height: 48px !important;
      margin-top: 6px !important;
      transform: none !important;
    }
  }

  ::v-deep .size-icon--style {
    line-height: 60px;
  }

  .hamburger-container {
    line-height: 60px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;

    &.has-navbar-logo {
      left: 200px;
    }
  }

  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 60px;
    display: flex;

    ::v-deep .el-form-item__label {
      color: var(--el-text-color-regular) !important;
    }

    ::v-deep .el-form-item__label:before {
      content: '*';
      color: red !important;
      margin-top: 3px !important;
    }

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }
    }

    .rwgl-item {
      display: flex !important;
      align-items: center;

      img {
        height: 18px;
        display: block;
      }
    }

    .avatar-container {
      margin: 0 15px 0 0;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        margin-top: 10px;
        position: relative;

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 20px;
        }

        .nickName {
          font-size: 15px;
          /*font-weight: bold;*/
          // color: rgba(0, 0, 0, 0.65);
          color: var(--themeColor);
          display: inline-block;
          margin-left: 10px;
        }

        i {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
        }
      }
    }
  }

  .flash ::v-deep .el-badge__content.is-fixed {
    animation: twinkle 1s infinite;
    /*margin-top: 16px;*/
    margin-right: 6px;
  }

  /* Define blinking animation */
  @keyframes twinkle {
    0% {
      opacity: 1;
      /* fully visible */
    }

    50% {
      opacity: 0.3;
      /* translucent */
    }

    100% {
      opacity: 1;
      /* fully visible */
    }
  }

  .item {
    height: 60px;
    line-height: 60px;
    display: inline-block;
    cursor: pointer;
  }

  .badge :deep(.el-badge__content.is-fixed) {
    top: 20px;
    transform: translateY(-50%) translateX(64%);
  }
}

.mag-tabs {
  height: calc(100% - 50px);

  ::v-deep .el-tabs__item {
    height: 50px;
    line-height: 50px;
  }

  ::v-deep .el-tabs__header {
    margin-bottom: 6px;
  }

  ::v-deep .el-tabs__content {
    height: calc(100% - 56px);

    .el-tab-pane {
      height: 100%;
    }
  }
}

.msg-btns {
  display: flex;
  height: 50px;
  line-height: 50px;
  border-top: 1px solid #e6e6e6;

  .btn-item {
    width: 50%;
    text-align: center;
    cursor: pointer;
    color: rgba(0, 0, 0, 0.85);

    &:last-child {
      border-left: 1px solid #e6e6e6;
    }
  }
}

#custom-header {
  background-color: rgb(248, 248, 248);
}

.el-dialog__header.show-close {
  text-align: left !important;
  padding: 9px 620px 9px 20px !important;
  background: rgb(248, 248, 248) !important;
}

.el-dialog__body {
  height: 500px;
}

/* Make sure the style is effective and increase the priority of the selector */
.rounded-button,
.rounded-button .el-button {
  border-radius: var(--qt-radius-small) !important;
}

.about-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  //padding: 27px 0;
  //gap: 16px;

  .logo {
    height: 34px;
    width: 146px;
    margin-top: 27px;
  }

  .about-title {
    margin-top: 20px;
    font-family: PingFang SC;
    font-weight: 600;
    font-size: 22px;
    color: var(--qt-text-primary);

    .version-badge {
      background-color: #409eff;
      color: white;
      padding: 2px 8px;
      border-radius: var(--qt-radius-medium);
      margin-left: 6px;
    }
  }

  .copyright {
    margin-top: 27px;
    font-family: PingFang SC;
    font-weight: var(--qt-font-weight-medium);
    font-size: 16px;
    color: #6e7a97;
  }
}

.about-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 35px;
  border-top: 1px solid var(--el-border-color-light); // Using Element Plus theme variables

  .status-text {
    font-family: PingFang SC;
    font-weight: 600;
    font-size: 18px;
    color: var(--qt-text-primary);
  }
  .update-link {
    color: #126bed; // Element Plus primary color, you can also use var(--el-color-primary)
    text-decoration: underline;
    cursor: pointer;
    font-size: 18px;
    transition: color 0.2s;

    &:hover {
      color: #66b1ff; // Color brightens on mouseover
    }

    &:active {
      color: #3a8ee6; // The color becomes darker when clicked
    }
  }
  .head-btns {
    img {
      margin-right: 6px;
    }
    .currImg {
      display: inline-block;
    }

    .act {
      display: none;
    }

    .el-button {
      height: 34px;
      width: 114px;
      border-radius: var(--qt-radius-medium) !important;
      font-size: 18px;
      font-family: PingFang SC;
      font-weight: 600;
      color: var(--qt-text-inverse);

      &:hover {
        .act {
          display: inline-block;
        }

        .currImg {
          display: none;
        }
      }
    }
  }
}
.markdown-content {
  padding: 0 15px 15px 15px;
}
</style>

<style lang="scss">
.about-dialog:not(.is-fullscreen) {
  margin: auto !important;
  width: 600px;
  height: 300px;
  padding: 0;
  .el-dialog__header {
    height: 47px !important;
    background: #f8f8f8 !important;
    line-height: 47px;
    padding-left: 27px;
    color: var(--qt-text-primary);
    padding-bottom: 0px;
    font-family: PingFang SC;
    .el-dialog__close {
      font-size: 18px;
      color: #6a6a6a;
      font-weight: bold;
    }

    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
      color: var(--qt-text-primary);
    }
  }
  .el-dialog__footer {
    padding-top: 0px;
  }
  .about-footer {
    padding: 11px 32px;
  }
}
</style>
