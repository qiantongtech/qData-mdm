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

package tech.qiantong.qdata.mdm.rule.api.service.rule;

import tech.qiantong.qdata.mdm.rule.api.rule.dto.RuleModelRulesRespDTO;
import java.util.Collection;

/** 规则模块向模型发布提供关联规则读取，调用方不直接访问规则表。 */
public interface RuleModelApi {
    /**
     * 按规则表MODEL_ID读取指定主模型及子模型的三类规则。
     * 完整保留规则配置、字段关联及状态，供发布时保存；不表示这些规则均可直接执行。
     *
     * @param modelIds 需要读取的模型ID集合，不得为空；重复ID会去重
     * @return 三类规则列表，无关联记录时对应列表为空，读取失败则抛出异常
     */
    RuleModelRulesRespDTO getModelRules(Collection<Long> modelIds);
}
