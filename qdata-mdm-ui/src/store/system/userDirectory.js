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

import {defineStore, storeToRefs} from 'pinia';
import {listUser} from '@/api/system/system/user.js';

const CACHE_TTL = 60000;
const RETRY_DELAY = 5000;

// Promise 和请求缓存不进入响应式状态，按 store 实例隔离，避免跨 Pinia 实例串用。
const runtimes = new WeakMap();

function getRuntime(store) {
  if (!runtimes.has(store)) {
    runtimes.set(store, {cache: new Map(), pending: new Map(), generation: 0});
  }
  return runtimes.get(store);
}

/** 系统用户列表的唯一业务入口，与当前登录用户的身份状态分开管理。 */
const useUserDirectoryStore = defineStore('userDirectory', {
  state: () => ({
    userList: [],
    loading: false,
    loadedAt: 0,
    error: null,
    lastAutoAttemptAt: 0
  }),
  getters: {
    responsibleUserList: (state) => state.userList
      .filter(user => user.userId != null)
      .map(user => ({
        value: String(user.userId),
        label: user.nickName || user.userName || String(user.userId),
        phone: user.phonenumber || ''
      })),
    usersById() {
      return new Map(this.responsibleUserList.map(user => [user.value, user]));
    }
  },
  actions: {
    /** 用户管理页保留服务端分页、部门和日期筛选，不使用下拉列表缓存。 */
    listUserPage(query) {
      return listUser(query);
    },

    /** 下拉框取完所有页，保留原始用户字段和响应结构，统一复用一分钟缓存。 */
    loadUserList(filters = {}, force = false) {
      const runtime = getRuntime(this);
      const {cache, pending} = runtime;
      const query = Object.fromEntries(Object.entries(filters)
        .filter(([key, value]) => !['pageNum', 'pageSize'].includes(key) && value != null && value !== '')
        .sort(([a], [b]) => a.localeCompare(b)));
      const key = JSON.stringify(query);
      if (pending.has(key)) return pending.get(key);
      const cached = cache.get(key);
      if (!force && cached && Date.now() - cached.loadedAt < CACHE_TTL) {
        return Promise.resolve(cached.response);
      }
      const currentGeneration = runtime.generation;
      const isDefault = key === '{}';
      if (isDefault) {
        this.loading = true;
        this.error = null;
      }
      const task = (async () => {
        const rows = [];
        const pageSize = 100;
        for (let pageNum = 1; ; pageNum++) {
          const response = await this.listUserPage({...query, pageNum, pageSize});
          // 会话清理后，旧账号请求既不能回填缓存，也不能继续请求后续页。
          if (currentGeneration !== runtime.generation) throw new Error('用户会话已变更，请重新加载用户列表');
          const pageRows = response.rows || response.data?.rows || [];
          rows.push(...pageRows);
          const total = response.total ?? response.data?.total;
          if (!pageRows.length || (total != null ? rows.length >= Number(total) : pageRows.length < pageSize)) break;
        }
        const response = {rows, total: rows.length};
        const timestamp = Date.now();
        cache.set(key, {response, loadedAt: timestamp});
        if (isDefault) {
          this.userList = rows;
          this.loadedAt = timestamp;
        }
        return response;
      })().catch(reason => {
        if (isDefault && currentGeneration === runtime.generation) this.error = reason;
        throw reason;
      }).finally(() => {
        if (pending.get(key) === task) {
          pending.delete(key);
          if (isDefault) this.loading = false;
        }
      });
      pending.set(key, task);
      return task;
    },

    /** 每次页面使用时检查缓存；读取 computed/getter 不触发网络副作用。 */
    initializeUsers() {
      const {pending} = getRuntime(this);
      if (pending.has('{}') || (this.loadedAt && Date.now() - this.loadedAt < CACHE_TTL)) return;
      if (this.lastAutoAttemptAt && Date.now() - this.lastAutoAttemptAt < RETRY_DELAY) return;
      this.lastAutoAttemptAt = Date.now();
      // 统一请求层负责提示，模板中的查询始终同步返回。
      this.loadUserList().catch(() => {
      });
    },

    getResponsibleUser(userId) {
      if (userId == null || userId === '') return null;
      return this.usersById.get(String(userId)) || null;
    },

    getResponsibleName(userId) {
      return this.getResponsibleUser(userId)?.label || '';
    },

    getResponsiblePhone(userId) {
      return this.getResponsibleUser(userId)?.phone || '';
    },

    async loadResponsibleUserOptions(selectedId) {
      await this.loadUserList();
      // 返回页面自己的数组，历史选项补齐不得污染全局列表。
      const options = [...this.responsibleUserList];
      if (selectedId != null && selectedId !== '' && !this.getResponsibleUser(selectedId)) {
        options.unshift({value: String(selectedId), label: `用户 ${selectedId}（已不可用）`, phone: ''});
      }
      return options;
    },

    clearCache() {
      const runtime = getRuntime(this);
      const {cache, pending} = runtime;
      runtime.generation++;
      cache.clear();
      pending.clear();
      this.userList = [];
      this.loadedAt = 0;
      this.loading = false;
      this.error = null;
      this.lastAutoAttemptAt = 0;
    },

    /** 用户管理变更后使所有筛选缓存失效，已使用的共享列表立即重新加载。 */
    invalidateCache() {
      const {pending} = getRuntime(this);
      const shouldReload = this.loadedAt !== 0 || pending.has('{}');
      this.clearCache();
      if (shouldReload) this.initializeUsers();
    },
  }
});

/** 页面在 setup 中调用一次，保留 ref 响应性，自动准备共享负责人列表。 */
export function useUserDirectory() {
  const store = useUserDirectoryStore();
  store.initializeUsers();
  return {
    ...storeToRefs(store),
    getResponsibleUser: store.getResponsibleUser,
    getResponsibleName: store.getResponsibleName,
    getResponsiblePhone: store.getResponsiblePhone,
    loadResponsibleUserOptions: store.loadResponsibleUserOptions,
  };
}

export {useUserDirectoryStore};
export default useUserDirectoryStore;
