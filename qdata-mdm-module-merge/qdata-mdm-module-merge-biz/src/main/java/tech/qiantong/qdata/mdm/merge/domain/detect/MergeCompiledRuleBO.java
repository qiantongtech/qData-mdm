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

package tech.qiantong.qdata.mdm.merge.domain.detect;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 参与本次检测的编译规则。
 *
 * @author chentangwen
 */
@Data
public class MergeCompiledRuleBO {

    /** 匹配规则编号。 */
    private Long id;

    /** 匹配规则名称。 */
    private String name;

    /** 规则类型，用于从策略注册表选择执行策略。 */
    private String type;

    /** 规则优先级，分数相同时数值较小者优先。 */
    private Long priority;

    /** 原始规则配置 JSON。 */
    private String snapshot;

    /** 整条规则的加权命中百分比阈值。 */
    private int finalThreshold = 100;

    /** 标准化精确匹配使用的显式转换选项。 */
    private List<String> standardOptions = new ArrayList<>();

    /** 按配置顺序编译的字段规则。 */
    private List<MergeMatchFieldBO> fields = new ArrayList<>();
}
