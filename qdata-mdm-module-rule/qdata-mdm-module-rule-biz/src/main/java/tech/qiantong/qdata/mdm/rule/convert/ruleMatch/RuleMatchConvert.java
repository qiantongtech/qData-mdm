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

package tech.qiantong.qdata.mdm.rule.convert.ruleMatch;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchSaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;

/**
 * 匹配规则 Convert
 *
 * @author qdata
 * @date 2026-09-15
 */
@Mapper
public interface RuleMatchConvert {
    RuleMatchConvert INSTANCE = Mappers.getMapper(RuleMatchConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ruleMatchPageReqVO 请求参数
     * @return RuleMatchDO
     */
     RuleMatchDO convertToDO(RuleMatchPageReqVO ruleMatchPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ruleMatchSaveReqVO 保存请求参数
     * @return RuleMatchDO
     */
     RuleMatchDO convertToDO(RuleMatchSaveReqVO ruleMatchSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ruleMatchDO 实体对象
     * @return RuleMatchRespVO
     */
     RuleMatchRespVO convertToRespVO(RuleMatchDO ruleMatchDO);

    /**
     * DOList 转换为 RespVOList
     * @param ruleMatchDOList 实体对象列表
     * @return List<RuleMatchRespVO>
     */
     List<RuleMatchRespVO> convertToRespVOList(List<RuleMatchDO> ruleMatchDOList);
}
