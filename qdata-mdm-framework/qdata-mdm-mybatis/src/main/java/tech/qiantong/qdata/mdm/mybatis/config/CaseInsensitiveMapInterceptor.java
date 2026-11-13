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

package tech.qiantong.qdata.mdm.mybatis.config;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拦截所有查询，把返回的 Map 结果包成大小写不敏感的 {@link CaseInsensitiveMap}。
 * <p>
 * 背景：GBase 8s（Informix 体系）把 SQL 列别名折成小写——
 * 例如 {@code SELECT ... AS statDate} 经 JDBC 返回的列标签是 {@code statdate}，
 * 而 Java 里按 camelCase（{@code "statDate"}）读取 {@code resultType="map"} 的结果会取到 null 进而 NPE；
 * 达梦/其它库则保留原别名大小写，混用时消费侧 key 无法统一。
 * <p>
 * 包装后：get/containsKey/remove 大小写不敏感（GBase 小写 key 也能被 camelCase 读到），
 * 同时保留 key 原始大小写（序列化到前端的 JSON 不变，零回归）。
 */
@Component
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class CaseInsensitiveMapInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object result = invocation.proceed();
        if (result instanceof List) {
            List list = (List) result;
            for (int i = 0; i < list.size(); i++) {
                Object e = list.get(i);
                // 只包装普通 Map，已是 CaseInsensitiveMap 的跳过（避免重复）
                if (e instanceof Map && !(e instanceof CaseInsensitiveMap)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> src = (Map<String, Object>) e;
                    list.set(i, new CaseInsensitiveMap(src));
                }
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    /**
     * LinkedHashMap 子类：保留插入顺序与 key 原始大小写（不影响 JSON 输出），
     * 但 get/containsKey/remove 大小写不敏感。
     */
    public static class CaseInsensitiveMap extends LinkedHashMap<String, Object> {

        public CaseInsensitiveMap() {
            super();
        }

        /** 从已有 Map 拷贝（Interceptor 包装查询结果时用） */
        public CaseInsensitiveMap(Map<? extends String, ?> m) {
            super();
            putAll(m);
        }

        /** 大小写不敏感地找出真实存在的 key；不存在返回 null */
        private String resolveKey(Object key) {
            if (key == null) {
                return null;
            }
            String lower = key.toString().toLowerCase();
            for (String real : super.keySet()) {
                if (real != null && real.toLowerCase().equals(lower)) {
                    return real;
                }
            }
            return null;
        }

        @Override
        public Object get(Object key) {
            String real = resolveKey(key);
            return real == null ? null : super.get(real);
        }

        @Override
        public boolean containsKey(Object key) {
            return resolveKey(key) != null;
        }

        @Override
        public Object getOrDefault(Object key, Object defaultValue) {
            String real = resolveKey(key);
            return real == null ? defaultValue : super.get(real);
        }

        @Override
        public Object put(String key, Object value) {
            // 若已存在仅大小写不同的 key，先移除，保证同一逻辑 key 只存一份（保留本次写入的大小写）
            String real = resolveKey(key);
            if (real != null && !real.equals(key)) {
                super.remove(real);
            }
            return super.put(key, value);
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            if (m == null) {
                return;
            }
            for (Map.Entry<? extends String, ?> e : m.entrySet()) {
                put(e.getKey(), e.getValue());
            }
        }

        @Override
        public Object remove(Object key) {
            String real = resolveKey(key);
            return real == null ? null : super.remove(real);
        }
    }
}
