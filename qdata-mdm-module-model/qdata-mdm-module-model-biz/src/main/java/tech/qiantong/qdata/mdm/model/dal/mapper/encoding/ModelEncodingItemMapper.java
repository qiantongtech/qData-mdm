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

package tech.qiantong.qdata.mdm.model.dal.mapper.encoding;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 编码方案规则段Mapper接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface ModelEncodingItemMapper extends BaseMapperX<ModelEncodingItemDO> {

    default PageResult<ModelEncodingItemDO> selectPage(ModelEncodingItemPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "sort_order", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelEncodingItemDO>()
                .eqIfPresent(ModelEncodingItemDO::getEncodingVersionId, reqVO.getEncodingVersionId())
                .eqIfPresent(ModelEncodingItemDO::getType, reqVO.getType())
                .eqIfPresent(ModelEncodingItemDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(ModelEncodingItemDO::getFixedValue, reqVO.getFixedValue())
                .eqIfPresent(ModelEncodingItemDO::getSequenceLength, reqVO.getSequenceLength())
                .eqIfPresent(ModelEncodingItemDO::getSequenceStart, reqVO.getSequenceStart())
                .eqIfPresent(ModelEncodingItemDO::getPadChar, reqVO.getPadChar())
                .eqIfPresent(ModelEncodingItemDO::getConfig, reqVO.getConfig())
                .eqIfPresent(ModelEncodingItemDO::getValidFlag, reqVO.getValidFlag())
                .eqIfPresent(ModelEncodingItemDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(ModelEncodingItemDO::getResetMethod, reqVO.getResetMethod())
                .eqIfPresent(ModelEncodingItemDO::getStepSize, reqVO.getStepSize())
                // 规则段必须按编码拼接顺序返回；ID作为顺序相同时的稳定排序条件。
                .orderByAsc(ModelEncodingItemDO::getSortOrder)
                .orderByAsc(ModelEncodingItemDO::getId)
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelEncodingItemDO::getName, reqVO.getName())
                // 仍允许列表请求在白名单范围内追加指定排序条件。
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
