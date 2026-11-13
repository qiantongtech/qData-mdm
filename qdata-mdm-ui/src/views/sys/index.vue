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
  <div class="app-container stagingIndex">
    <guide-tip tip-id="index"/>
    <el-row :gutter="15" class="home-layout">
      <el-col :xs="24" :sm="24" :md="18" :lg="18" class="home-gutter">
        <div class="userInfo module-1">
          <div class="info-main">
            <img :src="userStore.avatar" alt="" class="avatar"/>
            <div class="info-con">
              <div class="info-con-name">
                {{ greeting }}，{{ userStore.nickName }}，{{ message }}
              </div>
              <div class="info-con-desc">
                <span class="user-role">{{ td('sys.dashboard.admin') }}</span>
                <el-divider direction="vertical"/>
                <span>{{ xljtcont }}</span>
              </div>
            </div>
            <div class="info-btns">
              <!-- <a href="/user/profile" class=""> -->
              <el-button
                  type="primary"
                  class="info-btn-dft"
                  plain
                  size="large"
                  @click="goprofile"
                  style="background: #135afb; color: #fff; font-size: 14px"
              >
                {{ td('sys.dashboard.personalCenter') }}
              </el-button>
              <el-button
                  auto-insert-space
                  @click="logout"
                  type="primary"
                  class="info-btn-dft info-btn-dfts"
                  plain
                  size="large"
                  style="color: #135afb"
              >
                {{ td('sys.dashboard.logout') }}
              </el-button>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="6" :lg="6" class="home-gutter">
        <div class="module-2">
          <weather ref="weatherRef"/>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="18" :lg="18" class="home-gutter">
        <div class="module-3" v-loading="loading">
          <div class="module-item" v-for="(item, index) in module1" :key="index">
            <div class="module-item-t">
              <div class="module-item-t-l">
                <div class="name">{{ item.name }}</div>
                <div class="value">{{ formatNumber(item.value) }}</div>
              </div>
              <img :src="item.icon" class="module-item-t-r" alt=""/>
            </div>
            <div class="module-item-border"></div>
            <div class="module-item-data">
              <span class="data-label">周同比</span>
              <span class="trend-value">{{ Math.abs(item.rate) }}%</span>
              <img
                  v-if="item.trend !== 'flat'"
                  class="trend-icon"
                  :src="item.trend === 'up' ? trendIcons.up : trendIcons.down"
                  alt=""
              />
            </div>
          </div>
        </div>
        <el-row :gutter="16" v-loading="loading">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <div class="module-9 border-item">
              <div class="border-item-head">
                <span class="head-title"> 主数据正式变更趋势 </span>
                <div class="range-switch">
                  <button
                      v-for="range in trendRanges"
                      :key="range.value"
                      :class="{ active: activeTrendRange === range.value }"
                      type="button"
                      @click="changeTrendRange(range.value)"
                  >
                    {{ range.label }}
                  </button>
                </div>
              </div>
              <div class="border-item-body">
                <div class="chart-container" ref="module5ChartRef"></div>
              </div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <div class="module-8 border-item">
              <div class="border-item-head">
                <span class="head-title">主数据规模分布 <small>（按主数据域）</small></span>
                <el-link type="primary" :underline="false" @click="routeTo('/model/project', '')"
                >查看更多
                </el-link>
              </div>
              <div class="border-item-body">
                <div class="chart-container" ref="module8ChartRef"></div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-col>
      <el-col :xs="24" :sm="24" :md="6" :lg="6" class="home-gutter">
        <div class="border-item module-6 home-gutter">
          <div class="border-item-head">
            <span class="head-title">最近动态</span>
            <el-link type="primary" :underline="false" @click="goxinwen('list')">查看更多</el-link>
          </div>
          <div class="border-item-body">
            <div
                class="module-item"
                v-for="(item, index) in module6"
                :key="index"
                @click="goxinwen(item)"
            >
              <img :src="noticeIcon(item)" alt=""/>
              <div class="value" :title="item.noticeTitle">
                {{ item.noticeTitle }}
              </div>
              <div class="time">
                {{ timeAgo(item.createTime) }}
              </div>
            </div>
          </div>
        </div>
        <div class="border-item module-summary">
          <div class="border-item-head">
            <span class="head-title"
            >本周重点摘要 <small>({{ dashboardData.summaryRange }})</small></span
            >
          </div>
          <div class="border-item-body summary-list">
            <div v-for="item in dashboardData.weeklySummary" :key="item.label" class="summary-item">
              <img :src="item.icon" alt=""/>
              <span class="summary-label">{{ item.label }}</span>
              <span class="summary-value"
              >{{ item.value }}<small>{{ item.unit }}</small></span
              >
              <span v-if="item.trend !== 'warning'" class="summary-compare">较上周</span>
              <span :class="['summary-trend', item.trend]">
                {{ item.rateText }}
                <img v-if="item.trend === 'up'" :src="trendIcons.up" alt=""/>
                <img v-if="item.trend === 'down'" :src="trendIcons.down" alt=""/>
              </span>
            </div>
          </div>
        </div>
        <div class="border-item module-7">
          <div class="news">
            <div class="border-item">
              <div class="border-item-head">
                <span class="head-title">常用入口</span>
              </div>
              <div class="border-item-body">
                <div class="all-entrance">
                  <div
                      class="entrance-item"
                      v-for="item in entranceList"
                      :key="item.name"
                      @click="routeTo(item.path, item.query)"
                  >
                    <div class="image">
                      <img :src="item.icon" class="entrance-icon" alt=""/>
                    </div>
                    <div class="name">{{ item.name }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="24" :md="18" :lg="18" class="overview-column">
        <div class="module-10 border-item" v-loading="loading">
          <div class="border-item-head">
            <span class="head-title">主数据域运行概览 <small>（按主数据域）</small></span>
            <el-link type="primary" :underline="false" @click="routeTo('/master/instance', '')"
            >查看更多
            </el-link>
          </div>
          <div class="overview-table-wrap">
            <table class="overview-table">
              <thead>
              <tr>
                <th>主数据域</th>
                <th>模型数</th>
                <th>主数据量</th>
                <th>生效</th>
                <th>冻结</th>
                <th>失效</th>
                <th>归档</th>
                <th>今日正式变更</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="row in dashboardData.domainOverview" :key="row.domain">
                <td :class="{ total: row.total }">{{ row.domain }}</td>
                <td>{{ row.models }}</td>
                <td>
                  <div class="master-count">
                    <span>{{ formatNumber(row.totalCount) }}</span>
                    <span v-if="!row.total" class="count-track">
                        <i :style="{ width: `${row.percent}%` }"></i>
                      </span>
                  </div>
                </td>
                <td>
                  {{ formatNumber(row.effective) }}
                  <small v-if="row.effectiveRate">({{ row.effectiveRate }}%)</small>
                </td>
                <td>
                  {{ formatNumber(row.frozen) }}
                  <small v-if="row.frozenRate">({{ row.frozenRate }}%)</small>
                </td>
                <td>
                  {{ formatNumber(row.invalid) }}
                  <small v-if="row.invalidRate">({{ row.invalidRate }}%)</small>
                </td>
                <td>{{ row.archived ?? '-' }}</td>
                <td class="change-count">
                  {{ row.changes > 0 ? `+${row.changes}` : row.changes }}
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </el-col>
    </el-row>
    <!-- License agreement pop-up window -->
    <el-dialog
        v-model="licenseDialogVisible"
        :title="td('common.license.title')"
        :close-on-click-modal="false"
        :show-close="false"
        @close="handleLicenseClose"
    >
      <div class="license-content">
        <p
            v-for="item in tm('common.license.text.intro')"
            :key="item"
            class="license-paragraph mb15"
        >
          {{ item }}
        </p>

        <div
            v-for="(item, index) in tm('common.license.text.terms')"
            :key="item.title"
            class="license-term"
        >
          <div class="license-term-title">
            <span class="license-term-index">{{ index + 1 }}</span>
            <span>{{ item.title }}</span>
          </div>
          <p v-for="desc in item.desc" :key="desc" class="license-term-desc">
            {{ desc }}
          </p>
        </div>

        <div class="license-notice">
          <span>{{ tm('common.license.text.notice').before }}</span>
          <a
              :href="tm('common.license.text.notice').link"
              target="_blank"
              rel="noopener noreferrer"
          >
            {{ tm('common.license.text.notice').linkText }}
          </a>
          <span>{{ tm('common.license.text.notice').after }}</span>
        </div>

        <p v-for="item in tm('common.license.text.footer')" :key="item" class="license-footer-text">
          {{ item }}
        </p>
      </div>
      <template #footer>
        <el-button @click="toPolicy">{{ td('common.button.commercialAuthorization') }}</el-button>
        <el-button
            type="primary"
            class="license-aware-btn"
            :disabled="licenseCountdown > 0"
            @click="handleLicenseClose"
        >
          {{
            licenseCountdown > 0
                ? `${td('common.button.aware')} (${licenseCountdown}s)`
                : td('common.button.aware')
          }}
        </el-button>
      </template>
    </el-dialog>
    <important-note ref="importantNoteRef"/>
  </div>
</template>

<script setup name="Index">
import trendUpIcon from '@/assets/modules/sys/icons/masterHome/trend-up.svg';
import trendDownIcon from '@/assets/modules/sys/icons/masterHome/trend-down.svg';
import metricTotalIcon from '@/assets/modules/sys/icons/masterHome/metric-total.svg';
import metricModelIcon from '@/assets/modules/sys/icons/masterHome/metric-model.svg';
import metricDomainIcon from '@/assets/modules/sys/icons/masterHome/metric-domain.svg';
import metricReferenceIcon from '@/assets/modules/sys/icons/masterHome/metric-reference.svg';
import metricAuthIcon from '@/assets/modules/sys/icons/masterHome/metric-auth.svg';
import summaryNewDataIcon from '@/assets/modules/sys/icons/masterHome/summary-new-data.svg';
import summaryUpdatedDataIcon from '@/assets/modules/sys/icons/masterHome/summary-updated-data.svg';
import summaryPublishedModelsIcon from '@/assets/modules/sys/icons/masterHome/summary-published-models.svg';
import summaryReferenceDataIcon from '@/assets/modules/sys/icons/masterHome/summary-reference-data.svg';
import summaryExpiringAuthorizationsIcon
  from '@/assets/modules/sys/icons/masterHome/summary-expiring-authorizations.svg';
import entryAuthorityIcon from '@/assets/modules/sys/icons/masterHome/entry-authority.svg';
import entryModelIcon from '@/assets/modules/sys/icons/masterHome/entry-model.svg';
import entryTaskIcon from '@/assets/modules/sys/icons/masterHome/entry-task.svg';
import entryReferenceIcon from '@/assets/modules/sys/icons/masterHome/entry-reference.svg';
import entryAuthIcon from '@/assets/modules/sys/icons/masterHome/entry-auth.svg';
import entryAuditIcon from '@/assets/modules/sys/icons/masterHome/entry-audit.svg';
import entryTraceIcon from '@/assets/modules/sys/icons/masterHome/entry-trace.svg';
import entryEncodingIcon from '@/assets/modules/sys/icons/masterHome/entry-encoding.svg';
import noticePinnedIcon from '@/assets/modules/sys/icons/masterHome/notice-pinned.svg';
import noticeMessageIcon from '@/assets/modules/sys/icons/masterHome/notice-message.svg';
import noticeAlertIcon from '@/assets/modules/sys/icons/masterHome/notice-alert.svg';
import useUserStore from '@/store/system/user';
import {sortListNotice, alertNotice} from '@/api/system/system/notice.js';
import useAppStore from '@/store/system/app';
import * as echarts from 'echarts';
import {timeAgo} from '@/utils/time';
import {loginOut} from '@/api/system/sso-auth.js';
import ImportantNote from './system/notice/components/ImportantNoteDialog.vue';
import Weather from '@/components/Weather/index.vue';

import {useTimeGreeting} from '@/composables/useTimeGreeting';

const {greeting, message} = useTimeGreeting();

let {proxy} = getCurrentInstance();
import {useRouter} from 'vue-router';
import {
  computed,
  ref,
  watch,
  onMounted,
  onBeforeUnmount,
  onActivated,
  nextTick,
  getCurrentInstance
} from 'vue';
import useDefaultLang from '@/composables/useDefaultLang';

const {td, tm, locale} = useDefaultLang();

// 演示模式标识

// const route = useRoute();
const router = useRouter();

async function routeTo(link, query = {}) {
  if (link && link.indexOf('http') !== -1) {
    window.open(link);
    return;
  }

  if (link) {
    if (link === router.currentRoute.value.path) {
      window.location.reload();
    } else {
      try {
        await router.push({path: link, query});
      } catch (err) {
        console.error('路由跳转失败:', err);
      }
    }
  }
}

// License pop-up window logic
const LICENSE_CACHE_KEY = 'qdata_mdm_license_acknowledged';
const LICENSE_COUNTDOWN_SECONDS = 10;
const licenseDialogVisible = ref(false);
const licenseCountdown = ref(0);
let licenseCountdownTimer = null;

function clearLicenseCountdown() {
  if (licenseCountdownTimer) {
    clearInterval(licenseCountdownTimer);
    licenseCountdownTimer = null;
  }
}

function startLicenseCountdown() {
  clearLicenseCountdown();
  licenseCountdown.value = LICENSE_COUNTDOWN_SECONDS;
  licenseCountdownTimer = setInterval(() => {
    if (licenseCountdown.value <= 1) {
      clearLicenseCountdown();
      licenseCountdown.value = 0;
      return;
    }

    licenseCountdown.value -= 1;
  }, 1000);
}

/**
 * Check whether the user has agreed to the License agreement
 */
function checkLicense() {
  const acknowledged = localStorage.getItem(LICENSE_CACHE_KEY);
  if (!acknowledged) {
    licenseDialogVisible.value = true;
    startLicenseCountdown();
  }
}

/**
 * Handle License pop-up window closing event
 * Record that the user has agreed to the License agreement
 */
function handleLicenseClose() {
  clearLicenseCountdown();
  localStorage.setItem(LICENSE_CACHE_KEY, 'true');
  licenseDialogVisible.value = false;
}

/**
 * Jump to License agreement page
 */
function toPolicy() {
  window.open('https://community.qdata.tech/business/policy.html');
}

const chartIntances = [];
const getChartColors = () => ['#427afd', '#fa7f6f', '#42c9c0'];
const withColorAlpha = (color, alpha) => echarts.color.modifyAlpha(color, alpha);
watch(locale, () =>
    nextTick(() => {
      initModule5();
      initModule8();
    })
);
// eslint-disable-next-line no-unused-vars
const appStore = useAppStore();
const userStore = useUserStore();
const masterIcons = {
  'trend-up.svg': trendUpIcon,
  'trend-down.svg': trendDownIcon,
  'metric-total.svg': metricTotalIcon,
  'metric-model.svg': metricModelIcon,
  'metric-domain.svg': metricDomainIcon,
  'metric-reference.svg': metricReferenceIcon,
  'metric-auth.svg': metricAuthIcon,
  'summary-new-data.svg': summaryNewDataIcon,
  'summary-updated-data.svg': summaryUpdatedDataIcon,
  'summary-published-models.svg': summaryPublishedModelsIcon,
  'summary-reference-data.svg': summaryReferenceDataIcon,
  'summary-expiring-authorizations.svg': summaryExpiringAuthorizationsIcon,
  'entry-authority.svg': entryAuthorityIcon,
  'entry-model.svg': entryModelIcon,
  'entry-task.svg': entryTaskIcon,
  'entry-reference.svg': entryReferenceIcon,
  'entry-auth.svg': entryAuthIcon,
  'entry-audit.svg': entryAuditIcon,
  'entry-trace.svg': entryTraceIcon,
  'entry-encoding.svg': entryEncodingIcon,
  'notice-pinned.svg': noticePinnedIcon,
  'notice-message.svg': noticeMessageIcon,
  'notice-alert.svg': noticeAlertIcon
};
const masterAsset = (name) => masterIcons[name];

const trendIcons = {
  up: masterAsset('trend-up.svg'),
  down: masterAsset('trend-down.svg')
};

// 红框区域统一使用这个响应式数据对象，接口返回后直接替换对应字段即可。
const dashboardData = ref({
  summaryRange: '08.27 - 09.02',
  metrics: [
    {
      name: '主数据总量',
      value: 126,
      rate: 12,
      trend: 'up',
      icon: masterAsset('metric-total.svg')
    },
    {
      name: '主数据模型',
      value: 72,
      rate: 12,
      trend: 'down',
      icon: masterAsset('metric-model.svg')
    },
    {name: '主数据域', value: 164, rate: 12, trend: 'up', icon: masterAsset('metric-domain.svg')},
    {
      name: '参考数据项',
      value: 76,
      rate: 12,
      trend: 'up',
      icon: masterAsset('metric-reference.svg')
    },
    {name: '有效授权', value: 18, rate: 12, trend: 'up', icon: masterAsset('metric-auth.svg')}
  ],
  changeTrend: {
    days7: {
      xAxis: ['04.01', '04.02', '04.03', '04.04', '04.05', '04.06', '04.07'],
      series: [
        {name: '新增', data: [720, 710, 840, 790, 710, 860, 830]},
        {name: '状态变更', data: [230, 220, 350, 300, 220, 370, 340]},
        {name: '修改', data: [480, 470, 590, 550, 470, 620, 590]}
      ]
    },
    days30: {
      xAxis: ['08.04', '08.09', '08.14', '08.19', '08.24', '08.29', '09.02'],
      series: [
        {name: '新增', data: [650, 780, 720, 880, 760, 910, 860]},
        {name: '状态变更', data: [180, 260, 230, 320, 280, 390, 350]},
        {name: '修改', data: [420, 520, 490, 610, 540, 670, 630]}
      ]
    }
  },
  domainScale: [
    {name: '供应商域', value: 880},
    {name: '物料域', value: 770},
    {name: '客户域', value: 690},
    {name: '组织域', value: 590},
    {name: '人员域', value: 480},
    {name: '产品域', value: 410},
    {name: '项目域', value: 300},
    {name: '地点域', value: 200}
  ],
  weeklySummary: [
    {
      label: '新增主数据',
      value: 82,
      unit: '条',
      rateText: '+16%',
      trend: 'up',
      icon: masterAsset('summary-new-data.svg')
    },
    {
      label: '正式修改主数据',
      value: 214,
      unit: '条',
      rateText: '-16%',
      trend: 'down',
      icon: masterAsset('summary-updated-data.svg')
    },
    {
      label: '完成模型发布',
      value: 3,
      unit: '次',
      rateText: '持平',
      trend: 'flat',
      icon: masterAsset('summary-published-models.svg')
    },
    {
      label: '新增参考数据',
      value: 5,
      unit: '组',
      rateText: '-16%',
      trend: 'down',
      icon: masterAsset('summary-reference-data.svg')
    },
    {
      label: '即将到期授权',
      value: 7,
      unit: '条',
      rateText: '未来七天到期',
      trend: 'warning',
      icon: masterAsset('summary-expiring-authorizations.svg')
    }
  ],
  domainOverview: [
    {
      domain: '供应商域',
      models: 6,
      totalCount: 12580,
      percent: 78,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 12
    },
    {
      domain: '物料域',
      models: 8,
      totalCount: 12580,
      percent: 27,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 8
    },
    {
      domain: '客户域',
      models: 5,
      totalCount: 12580,
      percent: 65,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 7
    },
    {
      domain: '组织域',
      models: 4,
      totalCount: 12580,
      percent: 42,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 4
    },
    {
      domain: '人员域',
      models: 3,
      totalCount: 12580,
      percent: 88,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 3
    },
    {
      domain: '产品域',
      models: 3,
      totalCount: 12580,
      percent: 45,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 2
    },
    {
      domain: '合计',
      models: 29,
      totalCount: 12580,
      effective: 9120,
      effectiveRate: 89.06,
      frozen: 920,
      frozenRate: 7.31,
      invalid: 800,
      invalidRate: 6.36,
      archived: '-',
      changes: 36,
      total: true
    }
  ]
});

const module1 = computed(() => dashboardData.value.metrics);
const formatNumber = (value) => Number(value ?? 0).toLocaleString('zh-CN');

const entranceList = [
  {
    name: '权威主数据',
    path: '/master/instance',
    query: {},
    perm: ['mdm:modelBase:list'],
    icon: masterAsset('entry-authority.svg')
  },
  {
    name: '主数据模型',
    path: '/model/base',
    query: {},
    perm: ['mdm:modelBase:list'],
    icon: masterAsset('entry-model.svg')
  },
  {
    name: '整改任务',
    path: '/quality/qualityIssueTask',
    query: {},
    perm: ['mdm:mergeManage:list'],
    icon: masterAsset('entry-task.svg')
  },
  {
    name: '参考数据',
    path: '/model/dict/type',
    query: {},
    perm: ['model:dict:type:list'],
    icon: masterAsset('entry-reference.svg')
  },
  {
    name: '数据授权',
    path: '/authorization/dataAuthorization',
    query: {},
    perm: ['mdm:dataAuth:list'],
    icon: masterAsset('entry-auth.svg')
  },
  {
    name: '操作审计',
    path: '/audit/operationLog',
    query: {},
    perm: ['system:logininfor:list'],
    icon: masterAsset('entry-audit.svg')
  },
  {
    name: '来源追溯',
    path: '/audit/sourceTrace',
    query: {},
    perm: ['mdm:modelBase:list'],
    icon: masterAsset('entry-trace.svg')
  },
  {
    name: '编码方案',
    path: '/model/encoding',
    query: {},
    perm: ['mdm:modelEncoding:list'],
    icon: masterAsset('entry-encoding.svg')
  }
];

//新闻跳转
function goxinwen(row) {
  if (row == 'list') {
    proxy.$router.push('/sys/notice');
  } else {
    proxy.$router.push({
      path: '/sys/system/notice/detail',
      query: {id: row.noticeId}
    });
  }
}

function goprofile() {
  proxy.$router.push('/user/profile'); // 内部页面路径
}

//获取心灵鸡汤内容
const xljtcont = ref('');

function getxljtcont() {
  let xljtlist = [
    {value: "统一管理，沉淀可信的权威主数据。"},
    {value: "按主数据域梳理模型与目录，让业务对象各有所属、清晰可查。"},
    {value: "用模型定义业务，用字段规范数据。"},
    {value: "从模型修订到版本发布，让主数据结构随业务有序演进。"},
    {value: "统一编码规则，让数据标识有规可循。"},
    {value: "维护参考数据与字典，让字段取值有依据、业务口径更一致。"},
    {value: "接入多源数据，汇聚业务信息。"},
    {value: "识别重复记录，确认合并结果，沉淀一致的主数据。"},
    {value: "及时处理质量问题，提升主数据质量。"},
    {value: "合并结果经质量检测与审批后入库，让权威主数据有据可依。"},
    {value: "数据维护先申请，正式变更经审批。"},
    {value: "追溯数据来源，对照历史版本，让每一次变更清晰可查。"},
    {value: "按业务需要授权，让主数据有序共享。"},
    {value: "通过分发任务连接业务系统，让主数据更新及时传递。"},
    {value: "从任务进度到运行结果，掌握主数据处理动态。"},
  ];
  const num = Math.floor(Math.random() * xljtlist.length);
  xljtcont.value = xljtlist[num].value;
}

// 认证模式
const authType = import.meta.env.VITE_APP_AUTH_TYPE;

function logout() {
  ElMessageBox.confirm(td('sys.dashboard.confirmLogout'), td('common.message.prompt'), {
    confirmButtonText: td('common.button.confirm'),
    cancelButtonText: td('common.button.cancel'),
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
      .catch(() => {
      });
}

const module5ChartRef = ref(null);
const activeTrendRange = ref('days7');
const trendRanges = [
  {label: '近7天', value: 'days7'},
  {label: '近30天', value: 'days30'}
];

function initModule5() {
  const chart =
      echarts.getInstanceByDom(module5ChartRef.value) || echarts.init(module5ChartRef.value);
  const data = dashboardData.value.changeTrend[activeTrendRange.value];
  const colors = getChartColors();
  chart.setOption(
      {
        tooltip: {trigger: 'axis'},
        legend: {
          top: 12,
          right: 20,
          itemWidth: 7,
          itemHeight: 7,
          icon: 'circle',
          textStyle: {color: '#666', fontSize: 12}
        },
        grid: {top: 68, left: 56, right: 20, bottom: 42},
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: data.xAxis,
          axisTick: {show: false},
          axisLine: {lineStyle: {color: '#C9CDD4'}},
          axisLabel: {color: '#777', margin: 14}
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 1000,
          interval: 250,
          axisLabel: {color: '#777'},
          splitLine: {lineStyle: {color: '#E8EBF0'}}
        },
        series: data.series.map((item, index) => ({
          ...item,
          type: 'line',
          smooth: false,
          symbol: 'circle',
          symbolSize: 7,
          lineStyle: {width: 2, color: colors[index]},
          itemStyle: {color: '#fff', borderColor: colors[index], borderWidth: 2},
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {offset: 0, color: withColorAlpha(colors[index], 0.14)},
              {offset: 1, color: withColorAlpha(colors[index], 0)}
            ])
          }
        }))
      },
      true
  );
  if (!chartIntances.includes(chart)) chartIntances.push(chart);
}

function changeTrendRange(range) {
  activeTrendRange.value = range;
  nextTick(initModule5);
}

const module6 = ref([]);

function noticeIcon(item) {
  if (item.topFlag == 1) return masterAsset('notice-pinned.svg');
  return masterAsset(item.noticeType == 1 ? 'notice-message.svg' : 'notice-alert.svg');
}

function initModule6() {
  let query = {
    pageNum: 1,
    pageSize: 8
  };
  sortListNotice(query)
      .then((response) => {
        if (response.rows?.length) module6.value = response.rows;
      })
      .catch(() => {
      });
}

const importantNoteRef = ref(null);

function initAlertNotice() {
  alertNotice()
      .then((response) => {
        if (response.code == 200 && response.data) {
          importantNoteRef.value?.show(response.data);
        }
      })
      .catch(() => {
      });
}

const module8ChartRef = ref(null);

function initModule8() {
  const chart =
      echarts.getInstanceByDom(module8ChartRef.value) || echarts.init(module8ChartRef.value);
  const data = dashboardData.value.domainScale;
  const [primaryColor] = getChartColors();
  const backgroundColor = withColorAlpha(primaryColor, 0.1);
  const maxValue = Math.max(...data.map((item) => item.value), 1000);
  chart.setOption(
      {
        tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
        grid: {top: 68, left: 56, right: 20, bottom: 42},
        xAxis: {
          type: 'category',
          data: data.map((item) => item.name),
          axisTick: {show: false},
          axisLine: {lineStyle: {color: '#C9CDD4'}},
          axisLabel: {color: '#777', margin: 14, interval: 0}
        },
        yAxis: {
          type: 'value',
          name: '单位：次',
          nameTextStyle: {color: '#666', align: 'right', padding: [0, 0, 10, 0]},
          min: 0,
          max: Math.ceil(maxValue / 250) * 250,
          interval: 250,
          axisLabel: {color: '#777'},
          splitLine: {lineStyle: {color: '#E8EBF0'}}
        },
        series: [
          {
            name: '主数据量',
            type: 'custom',
            encode: {x: 0, y: 1},
            data: data.map((item, index) => [index, item.value]),
            renderItem(params, api) {
              const categoryIndex = api.value(0);
              const value = api.value(1);
              const center = api.coord([categoryIndex, 0])[0];
              const baseY = api.coord([categoryIndex, 0])[1];
              const valueY = api.coord([categoryIndex, value])[1];
              const maxY = api.coord([categoryIndex, Math.ceil(maxValue / 250) * 250])[1];

              return {
                type: 'group',
                children: [
                  {
                    type: 'rect',
                    shape: {
                      x: center - 23,
                      y: maxY,
                      width: 46,
                      height: baseY - maxY
                    },
                    style: {fill: backgroundColor},
                    silent: true
                  },
                  {
                    type: 'rect',
                    shape: {
                      x: center - 10,
                      y: valueY,
                      width: 20,
                      height: baseY - valueY,
                      r: [2, 2, 0, 0]
                    },
                    style: {
                      fill: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                        {offset: 0, color: withColorAlpha(primaryColor, 0.72)},
                        {offset: 1, color: primaryColor}
                      ])
                    }
                  }
                ]
              };
            }
          }
        ]
      },
      true
  );
  if (!chartIntances.includes(chart)) chartIntances.push(chart);
}

function chartIntancesResize() {
  chartIntances.forEach((intance) => {
    // 组件销毁后不再调整已释放的 ECharts 实例。
    if (!intance.isDisposed()) intance.resize();
  });
}

// 监听图表容器尺寸的变化。侧边栏展开/收起时不一定会触发 window.resize，
// 所以不能只依赖浏览器窗口事件，需要直接观察图表 DOM 的宽高。
let chartResizeObserver = null;

// 用于合并同一帧内的多次尺寸变化，避免侧边栏动画过程中频繁重绘。
let chartResizeFrame = 0;

// 侧边栏过渡动画结束后的兜底定时器。
let sidebarResizeTimer = 0;

// 将多次 resize 请求合并到浏览器的下一次绘制帧中执行。
function scheduleChartResize() {
  cancelAnimationFrame(chartResizeFrame);
  chartResizeFrame = requestAnimationFrame(chartIntancesResize);
}

// 初始化容器尺寸监听，容器宽高变化后让 ECharts 同步更新画布。
function observeChartContainers() {
  // 低版本浏览器不支持 ResizeObserver 时，仍可使用菜单事件和 window.resize 兜底。
  if (typeof ResizeObserver === 'undefined') return;
  chartResizeObserver?.disconnect();
  chartResizeObserver = new ResizeObserver(() => scheduleChartResize());
  [module5ChartRef.value, module8ChartRef.value].forEach((element) => {
    if (element) chartResizeObserver.observe(element);
  });
}

const instance = getCurrentInstance();

// 侧边栏状态切换后先在下一轮 DOM 更新时调整图表，
// 再在 350ms 后补一次，覆盖侧边栏 CSS 过渡动画结束的时刻。
const handleSidebarStatus = () => {
  nextTick(scheduleChartResize);
  clearTimeout(sidebarResizeTimer);
  // 兼容没有 ResizeObserver 的环境，也覆盖侧边栏过渡动画结束时刻。
  sidebarResizeTimer = window.setTimeout(scheduleChartResize, 350);
};
onBeforeUnmount(() => {
  clearLicenseCountdown();
  // 页面销毁时移除所有监听、定时器和动画帧，防止重复回调及内存泄漏。
  window.removeEventListener('resize', scheduleChartResize);
  instance.appContext.config.globalProperties.$bus.off('getsidebarStatus', handleSidebarStatus);
  chartResizeObserver?.disconnect();
  cancelAnimationFrame(chartResizeFrame);
  clearTimeout(sidebarResizeTimer);
  chartIntances.forEach((chart) => chart.dispose());
});
const loading = ref(false);

// 接入真实接口时，请在此处将返回值合并到 dashboardData，模板和图表无需改动。
function renderDashboard() {
  initModule5();
  initModule8();
  // 图表初始化后再绑定容器观察器，确保 ref 已指向真实 DOM。
  observeChartContainers();
  // 同时保留浏览器窗口尺寸变化的处理。
  window.addEventListener('resize', scheduleChartResize);
}

onMounted(() => {
  initModule6();
  getxljtcont();
  initAlertNotice();
  checkLicense();
  nextTick(renderDashboard);
  instance.appContext.config.globalProperties.$bus.on('getsidebarStatus', handleSidebarStatus);
});
onActivated(() => {
  nextTick(scheduleChartResize);
});
</script>

<style scoped lang="scss">
.home-gutter {
  position: relative;

  .monitor-status {
    padding: 20px;

    .status-row {
      margin-bottom: -20px; // Offset the effect of the last line of gutter

      .el-col {
        margin-bottom: 20px;
      }
    }

    .status-card {
      border-radius: 2px;
      min-height: 50px;
      display: flex;
      flex-direction: column;

      .card-body {
        display: flex;
        align-items: center;
        height: 100%;
        gap: 16px;

        @media (max-width: 768px) {
          flex-direction: column;
          align-items: stretch;
          gap: 12px;
        }
      }

      .left-info {
        background-color: #eaf3fe;
        flex: 0 0 30%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 10px 0;
        min-height: 80px;

        @media (max-width: 768px) {
          flex: none;
          width: 100%;
          padding: 16px 0;
          min-height: 40px;
        }

        .icon {
          color: #409eff;
          margin-bottom: 6px;
        }

        .name {
          font-size: 13px;
          font-weight: bold;
          color: #303133;
          text-align: center;
        }
      }

      .divider {
        width: 1px;
        background-color: #8fc7ff;
        margin: 0 20px;
        align-self: stretch;

        @media (max-width: 768px) {
          width: 100%;
          height: 1px;
          margin: 0;
        }
      }

      .right-status {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 20px;

        @media (max-width: 768px) {
          flex: none;
          width: 100%;
          flex-direction: row;
          justify-content: space-around;
          gap: 12px;
        }

        .status-line {
          display: flex;
          align-items: center;

          .count {
            width: 40px;
            height: 18px;
            font-size: 12px;
            font-weight: bold;
            line-height: 18px;
            text-align: center;
            color: #fff;
            margin-right: 6px;

            &.normal {
              background-color: #2ad14d;
            }

            &.error {
              background-color: #fd0201;
            }
          }

          .label {
            font-size: 13px;
            font-weight: bold;
            color: #5a5e66;
            text-align: center;
          }
        }
      }
    }
  }

  .news {
    height: 245px;

    .border-item-body {
      display: block;
    }

    .toAll {
      font-family: PingFangSC-Regular;
      font-size: 14px;
      color: #262626;
      font-weight: 400;

      &:hover {
        color: #0f62ff;
      }
    }

    .all-entrance {
      width: 100%;
      display: grid;
      grid-template-columns: repeat(3, 1fr);

      .entrance-item {
        padding: 10px;
        text-align: center;

        .name {
          margin-top: 5px;
          font-family: PingFangSC,
          PingFang SC;
          font-weight: 400;
          font-size: 14px;
          color: rgba(0, 0, 0, 0.85);
          font-style: normal;
          line-height: 16px;
        }

        .image {
          height: 44px;
          display: flex;
          justify-content: center;

          // Icon rounded square background
          .icon-background {
            width: 40px;
            height: 40px;
            border-radius: 6px;
            position: relative;
          }

          // icon
          .icon {
            display: flex;
            justify-content: center;
            align-items: center;
            font-size: 24px;
            color: #fff;
            line-height: 40px;
          }

          // Background color: main color
          .color-primary {
            background-image: linear-gradient(to bottom right, #2162fc 30%, #4f84fd 80%);
          }

          // Background color: orange
          .color-orange {
            background-image: linear-gradient(to bottom right, #f59040 30%, #f9bd77 90%);
          }

          // Background color: light blue
          .color-pale-blue {
            background-image: linear-gradient(to bottom right, #348bf2 10%, #63abff 60%);
          }

          // Background color: pink
          .color-pink {
            background-image: linear-gradient(to bottom right, #fb6594 20%, #fc92bb 80%);
          }
        }
      }
    }
  }
}

.work-time-div {
  width: 85%;
  height: 99%;
  position: absolute;
  /* top: 20px; */
  right: 20px;
  background-color: #ffffff;
  /* color: white; */
  /* font-size: 18px; */
  /* font-weight: bold; */
  /* padding: 30px 30px; */
  border-radius: 2px;
  text-align: center;
  /* cursor: pointer; */
  /* box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); */
  /* transition: transform 0.3s, box-shadow 0.3s;
          }

          .work-time-div:hover {
              /*transform: scale(1.05);*/
  /*box-shadow: 0 6px 12px rgba(0, 0, 0, 0.3);*/
}

.work-time-div:active {
  transform: scale(1);
}

.stagingIndex {
  min-width: 1290px;
  max-width: 100%;
  // width: 100%;
  height: 100%;
  // padding: 24px;
  background: #f0f2f5;

  ::v-deep .el-carousel {
    height: 100%;

    .el-carousel__container {
      height: 100%;
    }

    .el-carousel__arrow--left {
      display: none;
    }

    .el-carousel__arrow--right {
      display: none;
    }
  }

  :deep(.el-dialog__body) {
    padding: 20px !important;
  }
}

.module-4,
.module-5,
.module-8,
.module-9 {
  height: 350px !important;
}

.module-10 {
  height: 254px !important;

  .monitor-status {
    padding: 10px 20px;
  }

  .title {
    border-left: 3px solid #409eff;
    padding-left: 8px;
    margin-bottom: 12px;
    color: #409eff;
    font-weight: 600;
    font-size: 16px;
  }

  .cards {
    display: flex;
    // flex-wrap: wrap;
    flex-wrap: nowrap;
    gap: 16px;
    padding: 16px;
  }

  .card {
    flex: 1;
    min-width: 220px;
    max-width: calc(25% - 13px);
    height: 170px;
    box-sizing: border-box;
    background: #fff;
    border-radius: 2px;
    border: 1px solid rgba(0, 0, 0, 0.06);
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;

    .icon-title {
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(29, 96, 251, 0.04);
      border-radius: 2px;
      padding: 4px 8px;
      gap: 10px;
      width: 100%;
      height: 40px;

      .icon {
        width: 30px;
        height: 30px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .title-text {
        font-size: 14px;
        font-weight: 600;
        white-space: nowrap;
        font-family: REEJI-HongGuangGB, REEJI-HongGuangGB;
        font-weight: bold;
        color: #2666fb;
      }
    }

    .stats {
      display: flex;
      gap: 15px;
      justify-content: center;
      width: 100%;
      box-sizing: border-box;

      .stat-box {
        flex: 1;
        height: 82px;
        border: 1px solid rgba(0, 0, 0, 0.04);
        padding: 15px 30px;
        display: flex;
        flex-direction: column;
        align-items: center;
        border-radius: 2px;
        background: #fff;

        .num {
          font-size: 20px;
          font-weight: 500;
          margin-bottom: 5px;
          line-height: 1;
          font-family: YouSheBiaoTiHei;
          font-size: 24px;
          color: #40cfc1;
        }

        .label {
          font-family: PingFangSC,
          PingFang SC;
          font-weight: 500;
          font-size: 14px;
          width: 28px;
          color: rgba(0, 0, 0, 0.45);
        }
      }

      .normal-box .num {
        color: #3dc47f;
      }

      .abnormal-box .num {
        color: #f8b34a;
      }
    }
  }
}

.module-6,
.module-7 {
  height: 250px !important;
  //   box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
}

.module-report {
  margin-bottom: 15px;
  height: 60px !important;
}

.module-report .report-button {
  width: 100%;
  height: 60px;
  font-size: 22px;
  border-radius: 2px;
}

.home-gutter {
  margin-bottom: 15px;
}

.userInfo {
  height: 150px;
  padding: 35px 60px 0 32px;
  border-radius: 2px;
  justify-content: space-between;
  align-items: center;
  background: url(@/assets/common/images/bg-content.png) no-repeat center;
  background-size: 100% 100%;
  //   box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
}

.userInfo .menu {
  margin-bottom: 16px;
}

.userInfo .menu .el-breadcrumb__inner.is-link {
  font-family: PingFang SC;
  font-size: 14px;
  color: #888888;
  line-height: 22px;
  font-weight: 400;
}

.userInfo .menu .el-breadcrumb__inner {
  font-family: PingFang SC;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  text-align: left;
  line-height: 22px;
  font-weight: 400;
}

.userInfo .menu .el-breadcrumb__separator {
  font-size: 14px;
  color: #888888;
}

.userInfo .info-main {
  min-width: 100%;
  display: flex;
  position: relative;
  flex-shrink: 0;
}

.userInfo .info-main .avatar {
  width: 72px;
  height: 72px;
  margin-right: 24px;
  border-radius: 50%;
  border-radius: 50%;
  // border: 3px solid #fff;
  object-fit: cover;
  // box-shadow: 0 0 4px rgba(0, 0, 0, 0.1); /* Optional, add some shadow to enhance the layering */
}

.userInfo .info-main .info-con {
  height: 72px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
}

.userInfo .info-main .info-con .info-con-name {
  font-family: PingFang SC;
  font-size: 20px;
  color: rgba(0, 0, 0, 0.85);
  line-height: 28px;
  font-weight: 600;
}

.userInfo .info-main .info-con .info-con-desc {
  font-family: PingFang SC;
  font-size: 14px;
  color: #888888;
  line-height: 22px;
  font-weight: 400;
}

.userInfo .info-main .info-btns {
  position: absolute;
  top: 18px;
  right: 0;
  font-family: PingFang SC;
  font-size: 14px;
  font-weight: 500;
}

.userInfo .info-main .info-btns .info-btn {
  width: 100px;
  height: 40px;
  border-radius: 4px !important;
  border: none !important;
}

.userInfo .info-main .info-btns .info-btn-dft {
  font-family: PingFangSC,
  PingFang SC !important;
  font-weight: 500 !important;
  height: 40px;
  border-radius: 2px !important;
  margin-left: 20px;
}

.userInfo .info-main .info-btns .info-btn-dfts {
  &:hover,
  &:focus,
  &:active {
    color: #135afb !important;
    border-color: #135afb !important;
    background-color: transparent !important;
    box-shadow: none !important;
  }
}

.module-2 {
  height: 150px;
  // background-color: #fff;
  border-radius: 2px;
  text-align: center;
  //   box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);

  img {
    height: 100%;
  }
}

.module-2 #mobile12 .wtleft {
  width: 30%;
}

.module-3 {
  display: flex;

  justify-content: space-between;
  height: 150px;
  margin-bottom: 15px;
}

.module-3 .module-item {
  // width: 18.3%;
  // min-width: 185px;
  flex: 1;
  height: 100%;
  border-radius: 2px;
  padding: 22px 24px;
  background: #fff;
  margin-right: 15px;
  //   box-shadow: 1px 1px 3px rgba(0, 0, 0, 0.2);
}

.module-3 .module-item:last-of-type {
  margin-right: 0px;
}

.module-3 .module-item .module-item-t {
  display: flex;

  align-items: flex-end;

  justify-content: space-between;
}

.module-3 .module-item .module-item-t .module-item-t-l {
  font-family: PingFang SC;
  font-size: 14px;
  color: #888888;
  line-height: 22px;
  font-weight: 400;
}

.module-3 .module-item .module-item-t .module-item-t-l .name {
  margin-bottom: 10px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  font-family: PingFangSC,
  PingFang SC;
  font-weight: 400;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 22px;
  text-align: left;
  font-style: normal;
}

.module-3 .module-item .module-item-t .module-item-t-l span {
  // font-family: Sharp;
  // font-size: 30px;
  color: #000;
  font-family: YouSheBiaoTiHei;
  font-size: 36px;
  color: rgba(0, 0, 0, 0.85);
}

.module-3 .module-item .module-item-t .module-item-t-r {
  width: 48px;
  height: 48px;
}

.module-3 .module-item .module-item-border {
  height: 1px;
  background: #e2e2e2;
  margin: 20px 0 14px;
}

.module-3 .module-item .module-item-data {
  display: flex;
  align-items: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;

  .data-label {
    font-family: PingFangSC,
    PingFang SC;
    font-weight: 400;
    color: rgba(0, 0, 0, 0.65);
  }
}

.module-3 .module-item .module-item-data .data-up {
  font-size: 14px;
  margin-left: 8;
  margin-right: 4;
  // color: #000;
  color: rgba(0, 0, 0, 0.85);
  position: relative;
}

.module-3 .module-item .module-item-data .data-up:after {
  content: '';
  position: absolute;
  top: -1px;
  right: -20px;
  width: 0;
  height: 12px;
  border: 6px solid #f5222d;
  border-color: transparent transparent #f5222d transparent;
}

.module-3 .module-item .module-item-data .data-down {
  color: rgba(0, 0, 0, 0.85);
  font-size: 14px;
  margin-left: 8;
  margin-right: 10;
  position: relative;
}

.module-3 .module-item .module-item-data .data-down:after {
  content: '';
  position: absolute;
  top: 5px;
  right: -20px;
  width: 0;
  height: 12px;
  border: 6px solid #f5222d;
  border-color: #60f522 transparent transparent transparent;
}

.module-3 .module-item .module-item-data .data-equal {
  font-size: 14px;
  margin: 0 10px;
  color: #000;
  position: relative;
}

.border-item {
  width: 100%;
  height: 100%;
  background: #fff;
  border-radius: 2px;
}

.border-item .border-item-head {
  height: 50px;
  padding: 0 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e8e8e8;

  :deep(.el-link__inner) {
    color: rgba(0, 0, 0, 0.85) !important;
    font-family: PingFangSC,
    PingFang SC;
    font-weight: 400;
    font-size: 14px;
  }
}

.border-item .border-item-head .head-title {
  font-size: 16px;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
  font-family: PingFangSC,
  PingFang SC;
  font-weight: 500;
}

.border-item .border-item-head .head-title-seach {
  cursor: pointer;
  font-size: 14px;
  color: #0f62ff;
  line-height: 0px;
  font-style: normal;
}

.border-item .border-item-head .head-title:before {
  display: inline-block;
  content: '';
  width: 3px;
  height: 20px;
  border-radius: 2px 2px 2px 2px;
  background: var(--el-color-primary);
  margin-right: 10px;
}

.border-item .border-item-body {
  height: calc(100% - 50px);
  position: relative;
}

.border-item .border-item-body .chart-container {
  height: 100%;
}

.border-item .border-item-body .border-item-order {
  display: flex;
  justify-content: space-between;
  padding: 12px 20px 0 20px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 22px;
  font-weight: 400;
}

.border-item .border-item-body .border-item-order .name-span {
  width: 225px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.border-item .border-item-body .border-item-order .name-span:hover {
  color: #2666fb;
}

.border-item .border-item-body .border-item-order .time-span {
  margin-left: 20px;
  width: 60px;
  min-width: 60px;
  height: 22px;
  font-size: 14px;
  color: #888888;
  line-height: 22px;
}

.border-item .border-item-body .border-item-order .top {
  width: 50px;
  height: 22px;
  background: rgba(255, 0, 25, 0.06);
  border-radius: 2px;
  color: #ff0019;
}

.border-item .border-item-body .border-item-order .hui-span {
  background: #f0f2f5 !important;
  color: rgba(0, 0, 0, 0.65) !important;
}

.module3-item {
  background-color: #f7f7f7;
}

.module-6 {
  .border-item-body {
    overflow-y: auto;
    overflow-x: hidden;
    padding: 12px 0px;
  }

  .module-item {
    display: flex;
    align-items: center;
    padding: 6px 20px;
    cursor: pointer;
  }

  // .value,
  // .title {
  //     margin: 0 10px;
  // }

  .value {
    margin: 0 0 0 10px;
    font-size: 14px;
    white-space: nowrap;
    text-overflow: ellipsis;
    overflow: hidden;
    flex: 1;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.85);
    font-weight: 400;
  }

  .time {
    width: 66px;
    text-align: right;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    white-space: nowrap;
  }
}

.module-7 {
  .border-item-body {
    padding: 21px 0px;
    display: flex;
    align-items: center;
    justify-content: center;

    .carouselcont {
      height: 100%;
      width: calc(100% - 84px);
      margin-left: 8px;
      margin-right: 8px;
    }

    .qhbut {
      width: 21px;
      height: 21px;
      background: var(--el-color-primary);
      border-radius: 50%;
      border: 0px;
      color: #ffffff;
      text-align: center;
    }
  }

  .module-item {
    height: 100%;
    // background-color: #f6f6f6;
    // width: 100%;
    // height: 100%;
    // display: flex;
    // justify-content: center;
  }

  // ::v-deep {
  //     // .el-carousel__arrow {
  //     //     width: 21px;
  //     //     height: 21px;
  //     // }
  //     .el-carousel__arrow--left {
  //         display: none;
  //     }
  //     .el-carousel__arrow--right {
  //         display: none;
  //     }
  // }
}

.module-9 {
  // ::v-deep .el-table {
  //     thead .el-table__cell.is-leaf {
  //         background-color: #fff !important;
  //     }
  // }
  ::v-deep {
    // List header
    .el-table thead {
      height: 53px;

      .el-table__cell.is-leaf {
        background: rgba(19, 90, 251, 0.04) !important;
        // border-radius: 4px 4px 0px 0px;
      }
    }
  }
}

.module-9 {
  // .border-item-body {
  //   padding: 15px 34px 15px 34px;
  // }

  .table-column-code {
    font-size: 14px;
    color: #135afb;
  }
}

.license-content {
  padding: 8px 24px 0;
  color: rgba(0, 0, 0, 0.82);
  font-family: PingFang SC, Microsoft YaHei, Helvetica Neue, Arial, sans-serif;
  font-size: 14px;
  line-height: 1.8;

  .license-paragraph,
  .license-term-desc,
  .license-footer-text {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .license-paragraph {
    margin-bottom: 15px;
    line-height: normal;
  }

  .license-paragraph:first-child {
    color: rgba(0, 0, 0, 0.9);
    font-size: 16px;
    font-weight: 600;
    line-height: 1.6;
  }

  .license-term {
    padding: 18px 18px 15px 16px;
    background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
    border: 1px solid #d8e0ed;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.95);
  }

  .license-term + .license-term {
    margin-top: 15px;
  }

  .license-term-title {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    color: rgba(0, 0, 0, 0.9);
    font-weight: 600;
    line-height: 1.7;
  }

  .license-term-index {
    flex: 0 0 20px;
    width: 20px;
    height: 20px;
    margin-top: 2px;
    border-radius: 50%;
    background: #1767ff;
    color: #ffffff;
    font-size: 12px;
    font-weight: 600;
    line-height: 20px;
    text-align: center;
  }

  .license-term-desc {
    padding-left: 30px;
    color: rgba(0, 0, 0, 0.78);
  }

  .license-term-desc + .license-term-desc {
    margin-top: 2px;
  }

  .license-notice {
    position: relative;
    margin-top: 15px;
    padding: 12px 16px 12px 44px;
    color: rgba(0, 0, 0, 0.78);
    background: #fbfdff;
    border: 1px solid #cddcff;
    line-height: 1.8;
    word-break: break-word;

    a {
      color: #1767ff;
      text-decoration: none;
    }
  }

  .license-notice::before {
    content: "i";
    position: absolute;
    width: 18px;
    height: 18px;
    margin-left: -30px;
    margin-top: 3px;
    border: 1px solid #1767ff;
    border-radius: 50%;
    color: #1767ff;
    font-size: 13px;
    font-weight: 600;
    line-height: 17px;
    text-align: center;
  }

  .license-footer-text {
    margin-top: 15px;
    color: rgba(0, 0, 0, 0.82);
    font-weight: 600;
    line-height: normal;
  }
}

.license-aware-btn {
  min-width: 120px;
}
</style>
<style lang="scss" scoped>
@media screen and (max-width: 1280px) {
}

@media screen and (max-width: 992px) {
  .stagingIndex {
    min-width: 100%;
    padding: 12px;

    .userInfo {
      height: 100px;
      padding: 15px 15px;

      .info-main {
        align-items: center;

        .avatar {
          width: 48px;
          height: 48px;
          margin-right: 12px;
        }

        .info-con {
          .info-con-name {
            font-size: 14px;
            line-height: 20px;
          }

          .info-con-desc {
            font-size: 10px;
            line-height: 20px;
          }
        }
      }

      .info-btns {
        display: none;
      }
    }

    .module-3 {
      flex-direction: column;
      height: auto;
    }

    .module-3 .module-item {
      width: 100%;
      margin-bottom: 15px;
    }
  }

  .module-4 {
    min-height: 480px !important;
  }

  .module-10 {
    height: auto !important;

    .cards {
      flex-wrap: wrap !important;
      gap: 12px;
    }

    .card {
      max-width: calc(50% - 8px); // 2 per row
    }
  }
}

@media screen and (max-width: 768px) {
  .stagingIndex {
    min-width: 100%;
    padding: 12px;

    .userInfo {
      height: 100px;
      padding: 15px 15px;

      .info-main {
        align-items: center;

        .avatar {
          width: 48px;
          height: 48px;
          margin-right: 12px;
        }

        .info-con {
          .info-con-name {
            font-size: 14px;
            line-height: 20px;
          }

          .info-con-desc {
            font-size: 10px;
            line-height: 20px;
          }
        }
      }

      .info-btns {
        display: none;
      }
    }

    .module-3 {
      flex-direction: column;
      height: auto;
    }

    .module-3 .module-item {
      width: 100%;
      margin-bottom: 15px;
    }
  }

  .module-4 {
    min-height: 1150px !important;
  }

  .module-10 {
    height: auto !important;

    .cards {
      flex-wrap: wrap !important;
    }
  }
}

@media screen and (max-width: 576px) {
  .stagingIndex {
    min-width: 100%;
    padding: 12px;

    .userInfo {
      height: 100px;
      padding: 15px 15px;

      .info-main {
        align-items: center;

        .avatar {
          width: 48px;
          height: 48px;
          margin-right: 12px;
        }

        .info-con {
          .info-con-name {
            font-size: 14px;
            line-height: 20px;
          }

          .info-con-desc {
            font-size: 10px;
            line-height: 20px;
          }
        }
      }

      .info-btns {
        display: none;
      }
    }

    .module-3 {
      flex-direction: column;
      height: auto;
    }

    .module-3 .module-item {
      width: 100%;
      margin-bottom: 15px;
    }
  }

  .module-4 {
    min-height: 1140px !important;
  }

  .module-10 {
    height: auto !important;

    .cards {
      flex-direction: column; // Change to one column
      flex-wrap: wrap !important;
      gap: 12px;
    }

    .card {
      max-width: 100%;
      width: 100%;
    }

    .stats {
      flex-direction: column;
      gap: 10px;

      .stat-box {
        width: 100%;
        padding: 12px;
      }
    }
  }
}
</style>

<style scoped lang="scss">
/* Layout support for the copied dashboard content, using the existing page's visual styles. */
.stagingIndex {
  min-width: 0;
  width: auto;
  height: auto;

  .home-layout {
    display: grid;
    grid-template-columns: minmax(0, 3fr) minmax(320px, 1fr);
    grid-template-areas: 'welcome weather' 'main side' 'overview side';
    align-items: stretch;
    gap: 15px;
    margin: 0 !important;

    > .el-col {
      width: auto;
      max-width: none;
      min-width: 0;
      padding: 0 !important;
      margin-bottom: 0;
    }

    > .el-col:nth-child(1) {
      grid-area: welcome;
    }

    > .el-col:nth-child(2) {
      grid-area: weather;
    }

    > .el-col:nth-child(3) {
      grid-area: main;
    }

    > .el-col:nth-child(4) {
      grid-area: side;
      display: flex;
      flex-direction: column;
      gap: 15px;
    }

    > .el-col:nth-child(5) {
      grid-area: overview;
    }
  }

  .module-3 {
    .module-item {
      min-width: 0;
      padding-left: 16px;
      padding-right: 16px;
    }

    .module-item-t-l {
      min-width: 0;
    }

    .value {
      font-size: 30px;
      line-height: 1.2;
      color: rgba(0, 0, 0, 0.85);
    }

    .trend-value {
      color: #333;
    }

    .trend-icon {
      width: 16px;
      height: 17px;
      margin-left: 5px;
      vertical-align: middle;
    }
  }

  .border-item-head .head-title small {
    margin-left: 5px;
    font-size: 12px;
    font-weight: 400;
    color: #888;
  }

  .user-role {
    color: var(--el-color-primary);
  }

  .range-switch {
    display: flex;

    button {
      padding: 4px 8px;
      border: 1px solid #e8e8e8;
      background: #fff;
      color: #888;
      cursor: pointer;

      & + button {
        margin-left: -1px;
      }

      &.active {
        position: relative;
        color: var(--el-color-primary);
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }
    }
  }

  .module-6 {
    margin-bottom: 0;
    flex-shrink: 0;
    height: 360px !important;

    .border-item-body {
      height: calc(100% - 46px);
      overflow-y: auto;
      overflow-x: hidden;
      padding: 14px 20px;

      .module-item {
        display: flex;
        align-items: center;
        margin-bottom: 16px;
        padding: 0;
        cursor: pointer;

        img {
          width: 20px;
          height: 20px;
        }

        &:last-child {
          margin-bottom: 0;
        }
      }

      .value {
        margin: 0 0 0 9px;
        font-size: 14px;
        white-space: nowrap;
        text-overflow: ellipsis;
        overflow: hidden;
        flex: 1;
        color: #4e5969;
        font-weight: 400;
      }

      .time {
        width: 58px;
        height: 18px;
        line-height: 18px;
        font-size: 12px;
        font-weight: 400;
        color: #a2a8b1;
        background: transparent;
        text-align: right;
        border-radius: 2px;
      }
    }
  }

  .module-summary {
    flex: 1;

    .summary-list {
      padding: 0 20px;
    }

    .summary-item {
      display: grid;
      grid-template-columns: 26px minmax(0, 1.5fr) 62px 52px minmax(68px, 1fr);
      column-gap: 4px;
      align-items: center;
      height: 45px;
      border-bottom: 1px solid #f0f1f3;
      font-size: 13px;

      &:last-child {
        border-bottom: 0;
      }

      > img {
        width: 22px;
        height: 22px;
      }

      .summary-label {
        overflow: hidden;
        color: #4e5969;
        white-space: nowrap;
        text-overflow: ellipsis;
      }

      .summary-value {
        color: #1d2129;
        font-size: 18px;
        font-weight: 500;
        white-space: nowrap;
      }

      .summary-value small {
        margin-left: 4px;
        font-size: 12px;
        font-weight: 400;
      }

      .summary-compare {
        color: #8a919b;
        white-space: nowrap;
      }

      .summary-trend {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 3px;
        text-align: right;
        white-space: nowrap;

        img {
          width: 16px;
          height: 17px;
        }
      }

      .summary-trend.up {
        color: #f04455;
      }

      .summary-trend.down {
        color: #22b573;
      }

      .summary-trend.flat {
        color: #86909c;
        justify-content: flex-end;
        text-align: right;
        box-sizing: border-box;
        padding-right: 19px;
      }

      .summary-trend.warning {
        grid-column: 4 / 6;
        color: #f3b426;
      }
    }
  }

  .module-7 {
    flex-shrink: 0;

    .all-entrance {
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .entrance-item {
      min-width: 0;
      cursor: pointer;
      padding: 10px 3px;
    }

    .entrance-icon {
      width: 40px;
      height: 40px;
    }

    .all-entrance .entrance-item .name {
      font-size: 13px;
    }
  }

  .module-10 {
    height: auto !important;
    min-height: 360px;

    .overview-table-wrap {
      padding: 15px;
      overflow-x: auto;
    }

    .overview-table {
      width: 100%;
      min-width: 850px;
      border-collapse: collapse;
      table-layout: fixed;
      font-size: 13px;
      color: #888;

      th,
      td {
        padding: 10px 5px;
        border-bottom: 1px solid #e8e8e8;
        text-align: center;
      }

      th {
        background: rgba(19, 90, 251, 0.04);
        color: #333;
        font-weight: 500;
      }

      th:first-child {
        width: 12%;
      }

      th:nth-child(3) {
        width: 20%;
      }

      th:last-child {
        width: 13%;
      }

      small {
        font-size: 11px;
      }

      tr:last-child td {
        border: 0;
        background: rgba(19, 90, 251, 0.04);
      }

      td.total,
      .change-count {
        color: var(--el-color-primary);
      }
    }

    .master-count {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }

    .count-track {
      width: 48px;
      height: 6px;
      background: var(--el-color-primary-light-9);
      border-radius: 2px;
      overflow: hidden;
    }

    .count-track i {
      display: block;
      height: 100%;
      background: var(--el-color-primary);
    }
  }
}

@media (max-width: 1100px) {
  .stagingIndex .home-layout {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas: 'welcome' 'weather' 'main' 'side' 'overview';
  }
}

@media (max-width: 992px) {
  .stagingIndex {
    .module-3 {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 15px;
    }

    .module-3 .module-item {
      width: auto;
      margin: 0;
    }

    .module-3 .module-item:last-child {
      grid-column: 1 / -1;
    }

    .module-9 {
      margin-bottom: 15px;
    }
  }
}
</style>
