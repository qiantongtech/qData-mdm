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

package tech.qiantong.qdata.mdm.merge.support;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeCompiledRuleBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchClusterBO;
import tech.qiantong.qdata.mdm.merge.domain.detect.MergeMatchEdgeBO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按匹配规则把来源记录聚成候选组。空白值不参与相等判断，避免空对空被当成重复。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeMatchEngine {
    /** 模糊匹配计算的字符上限，保持历史行为。 */
    private static final int MAX_FUZZY_LENGTH = 128;

    private MergeMatchEngine() {
    }

    /**
     * 对记录两两比较，并按匹配边的连通分量成组。
     * A 命中 B 且 B 命中 C 时三者同组，即使 A 与 C 未直接命中；无匹配记录生成单条组。
     *
     * @param records 按固定快照顺序排列的匹配字段值
     * @param rules 按配置顺序排列的编译规则
     * @param registry Spring 管理的匹配策略注册表
     * @return 候选组及组内每条命中的匹配边
     */
    public static List<MergeMatchClusterBO> cluster(List<Map<String, String>> records,
            List<MergeCompiledRuleBO> rules, MergeMatchStrategyRegistry registry) {
        if (records == null) {
            throw new IllegalArgumentException("匹配记录不能为空");
        }
        if (rules == null || rules.isEmpty()) {
            throw new ServiceException("匹配规则不能为空");
        }
        if (registry == null) {
            throw new ServiceException("匹配策略注册表不能为空");
        }
        for (MergeCompiledRuleBO rule : rules) {
            registry.validate(rule);
        }
        int size = records.size();
        int[] parent = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
        List<MergeMatchEdgeBO> edges = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                List<MergeMatchEdgeBO> pairEdges = new ArrayList<>();
                for (MergeCompiledRuleBO rule : rules) {
                    MergeMatchEdgeBO edge = registry.get(rule.getType())
                            .match(records.get(i), records.get(j), rule, registry);
                    if (edge != null) {
                        pairEdges.add(edge);
                    }
                }
                if (pairEdges.isEmpty()) {
                    continue;
                }
                for (MergeMatchEdgeBO edge : pairEdges) {
                    edge.setLeft(i);
                    edge.setRight(j);
                    edges.add(edge);
                }
                int leftRoot = find(parent, i);
                int rightRoot = find(parent, j);
                if (leftRoot != rightRoot) {
                    parent[rightRoot] = leftRoot;
                }
            }
        }
        Map<Integer, MergeMatchClusterBO> groups = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            int root = find(parent, i);
            MergeMatchClusterBO cluster = groups.get(root);
            if (cluster == null) {
                cluster = new MergeMatchClusterBO();
                groups.put(root, cluster);
            }
            cluster.getIndexes().add(i);
        }
        for (MergeMatchEdgeBO edge : edges) {
            MergeMatchClusterBO cluster = groups.get(find(parent, edge.getLeft()));
            cluster.getEdges().add(edge);
            if (edge.getScore().compareTo(cluster.getMaxScore()) > 0) {
                cluster.setMaxScore(edge.getScore());
            }
        }
        return new ArrayList<MergeMatchClusterBO>(groups.values());
    }

    /** 使用编辑距离计算相似度，比较文本仅取前 128 个字符以限制计算量。 */
    static int similarity(String left, String right) {
        if (left.equals(right)) {
            return 100;
        }
        String lv = cut(left);
        String rv = cut(right);
        int max = Math.max(lv.length(), rv.length());
        if (max == 0) {
            return 100;
        }
        int dist = levenshtein(lv, rv);
        return (max - dist) * 100 / max;
    }

    /** 沿用历史前缀截断规则；超过上限的后缀不参与模糊匹配。 */
    private static String cut(String value) {
        return value.length() <= MAX_FUZZY_LENGTH ? value : value.substring(0, MAX_FUZZY_LENGTH);
    }

    /** 使用两行动态规划计算编辑距离，避免构造完整距离矩阵。 */
    static int levenshtein(String left, String right) {
        int n = left.length();
        int m = right.length();
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char lc = left.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = lc == right.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[m];
    }

    /** 查找并查集根节点，并压缩路径以降低后续查找成本。 */
    private static int find(int[] parent, int index) {
        int root = index;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[index] != root) {
            int next = parent[index];
            parent[index] = root;
            index = next;
        }
        return root;
    }

}
