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
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 编码方案版本Mapper接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface ModelEncodingVersionMapper extends BaseMapperX<ModelEncodingVersionDO> {

    /** 版本号不可复用，分配新版本时须包含逻辑删除的历史记录。 */
    List<ModelEncodingVersionDO> selectHistoryIncludingDeleted(@Param("encodingId") Long encodingId);


    default PageResult<ModelEncodingVersionDO> selectPage(ModelEncodingVersionPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelEncodingVersionDO>()
                .eqIfPresent(ModelEncodingVersionDO::getEncodingId, reqVO.getEncodingId())
                .eqIfPresent(ModelEncodingVersionDO::getVersionNo, reqVO.getVersionNo())
                .eqIfPresent(ModelEncodingVersionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ModelEncodingVersionDO::getEffectiveTime, reqVO.getEffectiveTime())
                .eqIfPresent(ModelEncodingVersionDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelEncodingVersionDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
