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

package tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor;

import org.apache.ibatis.annotations.Param;
import java.util.Collection;
import java.util.List;

import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorPageReqVO;


/**
 * 生存规则Mapper接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface RuleSurvivorMapper extends BaseMapperX<RuleSurvivorDO> {

    /** 模型发布专用的完整关联规则读取，按MODEL_ID筛选，不过滤启停或逻辑删除状态。 */
    List<RuleSurvivorDO> selectModelRules(@Param("modelIds") Collection<Long> modelIds);

    /** 批量查询当前页规则关联的模型及归属信息。 */
    List<RuleSurvivorDO> selectDisplayInfo(@Param("ids") Collection<Long> ids);


    default PageResult<RuleSurvivorDO> selectPage(RuleSurvivorPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<RuleSurvivorDO>()
                .eqIfPresent(RuleSurvivorDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(RuleSurvivorDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(RuleSurvivorDO::getModelId, reqVO.getModelId())
                .eqIfPresent(RuleSurvivorDO::getModelVersion, reqVO.getModelVersion())
                .eqIfPresent(RuleSurvivorDO::getModelFieldId, reqVO.getModelFieldId())
                .eqIfPresent(RuleSurvivorDO::getAbnormalFlag, reqVO.getAbnormalFlag())
                .eqIfPresent(RuleSurvivorDO::getRuleType, reqVO.getRuleType())
                .eqIfPresent(RuleSurvivorDO::getRuleConfig, reqVO.getRuleConfig())
                .eqIfPresent(RuleSurvivorDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(RuleSurvivorDO::getDescription, reqVO.getDescription())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(RuleSurvivorDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
