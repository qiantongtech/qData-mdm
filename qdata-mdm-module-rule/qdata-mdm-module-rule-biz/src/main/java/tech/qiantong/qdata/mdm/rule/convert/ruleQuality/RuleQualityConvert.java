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

package tech.qiantong.qdata.mdm.rule.convert.ruleQuality;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualitySaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;

/**
 * 质量规则 Convert
 *
 * @author qdata
 * @date 2026-09-18
 */
@Mapper
public interface RuleQualityConvert {
    RuleQualityConvert INSTANCE = Mappers.getMapper(RuleQualityConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ruleQualityPageReqVO 请求参数
     * @return RuleQualityDO
     */
     RuleQualityDO convertToDO(RuleQualityPageReqVO ruleQualityPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ruleQualitySaveReqVO 保存请求参数
     * @return RuleQualityDO
     */
     RuleQualityDO convertToDO(RuleQualitySaveReqVO ruleQualitySaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ruleQualityDO 实体对象
     * @return RuleQualityRespVO
     */
     RuleQualityRespVO convertToRespVO(RuleQualityDO ruleQualityDO);

    /**
     * DOList 转换为 RespVOList
     * @param ruleQualityDOList 实体对象列表
     * @return List<RuleQualityRespVO>
     */
     List<RuleQualityRespVO> convertToRespVOList(List<RuleQualityDO> ruleQualityDOList);
}
