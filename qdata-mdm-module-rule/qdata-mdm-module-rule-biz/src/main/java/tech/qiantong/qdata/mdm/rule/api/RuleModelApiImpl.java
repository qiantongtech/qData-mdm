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

package tech.qiantong.qdata.mdm.rule.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.rule.api.rule.dto.RuleModelRulesRespDTO;
import tech.qiantong.qdata.mdm.rule.api.service.rule.RuleModelApi;
import tech.qiantong.qdata.mdm.rule.api.ruleMatch.dto.RuleMatchRespDTO;
import tech.qiantong.qdata.mdm.rule.api.ruleSurvivor.dto.RuleSurvivorRespDTO;
import tech.qiantong.qdata.mdm.rule.api.ruleQuality.dto.RuleQualityRespDTO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleMatch.RuleMatchMapper;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.RuleSurvivorMapper;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleQuality.RuleQualityMapper;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
import javax.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 规则表只由规则模块读取，三类配置在同一个平台只读事务中返回。 */
@Service
@Transactional(readOnly = true)
public class RuleModelApiImpl implements RuleModelApi {
    /** 读取指定模型关联的匹配规则，保留逻辑删除和启停状态用于完整发布记录。 */
    @Resource
    private RuleMatchMapper ruleMatchMapper;
    /** 读取指定模型关联的生存策略及原配置文本。 */
    @Resource
    private RuleSurvivorMapper ruleSurvivorMapper;
    /** 读取指定模型关联的质量规则及原配置文本。 */
    @Resource
    private RuleQualityMapper ruleQualityMapper;

    /** 不套用管理分页和启用过滤，调用方拿到完整状态后决定正常业务展示范围。 */
    @Override
    public RuleModelRulesRespDTO getModelRules(Collection<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            throw new ServiceException("请提供需要查询规则的模型ID");
        }
        Set<Long> ids = new LinkedHashSet<>(modelIds);
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ServiceException("规则查询模型ID必须为正数");
        }
        List<RuleMatchDO> match = ruleMatchMapper.selectModelRules(ids);
        List<RuleSurvivorDO> survivor = ruleSurvivorMapper.selectModelRules(ids);
        List<RuleQualityDO> quality = ruleQualityMapper.selectModelRules(ids);
        if (match == null || survivor == null || quality == null) {
            throw new ServiceException("模型关联规则读取结果不完整");
        }
        RuleModelRulesRespDTO result = new RuleModelRulesRespDTO();
        result.setMatch(BeanUtils.toBean(match, RuleMatchRespDTO.class));
        result.setSurvivor(BeanUtils.toBean(survivor, RuleSurvivorRespDTO.class));
        result.setQuality(BeanUtils.toBean(quality, RuleQualityRespDTO.class));
        return result;
    }
}
