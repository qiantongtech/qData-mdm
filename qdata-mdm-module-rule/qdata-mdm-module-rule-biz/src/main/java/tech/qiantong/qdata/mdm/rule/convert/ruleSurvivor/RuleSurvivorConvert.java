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

package tech.qiantong.qdata.mdm.rule.convert.ruleSurvivor;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorSaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;

/**
 * 生存规则 Convert
 *
 * @author qdata
 * @date 2026-09-17
 */
@Mapper
public interface RuleSurvivorConvert {
    RuleSurvivorConvert INSTANCE = Mappers.getMapper(RuleSurvivorConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param ruleSurvivorPageReqVO 请求参数
     * @return RuleSurvivorDO
     */
     RuleSurvivorDO convertToDO(RuleSurvivorPageReqVO ruleSurvivorPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param ruleSurvivorSaveReqVO 保存请求参数
     * @return RuleSurvivorDO
     */
     RuleSurvivorDO convertToDO(RuleSurvivorSaveReqVO ruleSurvivorSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param ruleSurvivorDO 实体对象
     * @return RuleSurvivorRespVO
     */
     RuleSurvivorRespVO convertToRespVO(RuleSurvivorDO ruleSurvivorDO);

    /**
     * DOList 转换为 RespVOList
     * @param ruleSurvivorDOList 实体对象列表
     * @return List<RuleSurvivorRespVO>
     */
     List<RuleSurvivorRespVO> convertToRespVOList(List<RuleSurvivorDO> ruleSurvivorDOList);
}
