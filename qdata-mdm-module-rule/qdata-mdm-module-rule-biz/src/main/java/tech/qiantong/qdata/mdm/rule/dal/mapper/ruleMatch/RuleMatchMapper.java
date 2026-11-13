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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleMatch;

import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import java.util.Arrays;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchPageReqVO;


/**
 * 匹配规则Mapper接口
 *
 * @author qdata
 * @date 2026-09-15
 */
public interface RuleMatchMapper extends BaseMapperX<RuleMatchDO> {

    /** 模型发布专用的完整关联规则读取，按MODEL_ID筛选，不过滤启停或逻辑删除状态。 */
    List<RuleMatchDO> selectModelRules(@Param("modelIds") Collection<Long> modelIds);


    /** 批量补充列表展示信息，避免逐条查询。 */
    List<RuleMatchDO> selectDisplayInfo(@Param("ids") Collection<Long> ids);

    default PageResult<RuleMatchDO> selectPage(RuleMatchPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<RuleMatchDO>()
                .eqIfPresent(RuleMatchDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(RuleMatchDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(RuleMatchDO::getModelId, reqVO.getModelId())
                .eqIfPresent(RuleMatchDO::getModelVersion, reqVO.getModelVersion())
                .eqIfPresent(RuleMatchDO::getPriority, reqVO.getPriority())
                .eqIfPresent(RuleMatchDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(RuleMatchDO::getAbnormalFlag, reqVO.getAbnormalFlag())
                .eqIfPresent(RuleMatchDO::getModelFieldId, reqVO.getModelFieldId())
                .eqIfPresent(RuleMatchDO::getRuleType, reqVO.getRuleType())
                .eqIfPresent(RuleMatchDO::getRuleConfig, reqVO.getRuleConfig())
                .eqIfPresent(RuleMatchDO::getRuleScore, reqVO.getRuleScore())
                .eqIfPresent(RuleMatchDO::getDescription, reqVO.getDescription())
                .eqIfPresent(RuleMatchDO::getCreateBy, reqVO.getCreateBy())
                .eqIfPresent(RuleMatchDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(RuleMatchDO::getUpdateBy, reqVO.getUpdateBy())
                .eqIfPresent(RuleMatchDO::getUpdateTime, reqVO.getUpdateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(RuleMatchDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
