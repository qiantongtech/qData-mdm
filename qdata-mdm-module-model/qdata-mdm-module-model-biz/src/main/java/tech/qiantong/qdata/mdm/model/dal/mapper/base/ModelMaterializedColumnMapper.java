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

package tech.qiantong.qdata.mdm.model.dal.mapper.base;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnReqDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelMaterializedColumnDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import org.apache.ibatis.annotations.Param;

/**
 * 成功物化字段定义 Mapper。
 */
public interface ModelMaterializedColumnMapper extends BaseMapperX<ModelMaterializedColumnDO> {

    /** 批量读取本次成功表的全部真实字段，调用方传入的表ID集合不得为空。 */
    List<ModelMaterializedColumnDO> selectPublishedColumns(@Param("tableIds") Collection<Long> tableIds);

    /**
     * 分页查询未逻辑删除的物化字段定义；未指定启用条件时保留停用字段，供历史结构查询。
     *
     * @param reqDTO 分页、筛选及排序参数，排序字段限于白名单
     * @return 符合条件的分页记录
     */
    default PageResult<ModelMaterializedColumnDO> selectPage(ModelMaterializedColumnReqDTO reqDTO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        // eqIfPresent只在请求参数有值时加筛选条件；排序字段还要经过白名单检查，避免直接拼入任意列名。
        return selectPage(reqDTO, new LambdaQueryWrapperX<ModelMaterializedColumnDO>()
                .eqIfPresent(ModelMaterializedColumnDO::getId, reqDTO.getId())
                .eqIfPresent(ModelMaterializedColumnDO::getMaterializedTableId, reqDTO.getMaterializedTableId())
                .eqIfPresent(ModelMaterializedColumnDO::getModelFieldId, reqDTO.getModelFieldId())
                .eqIfPresent(ModelMaterializedColumnDO::getSystemFieldKey, reqDTO.getSystemFieldKey())
                .eqIfPresent(ModelMaterializedColumnDO::getColumnName, reqDTO.getColumnName())
                .eqIfPresent(ModelMaterializedColumnDO::getLogicalType, reqDTO.getLogicalType())
                .eqIfPresent(ModelMaterializedColumnDO::getEnabledFlag, reqDTO.getEnabledFlag())
                .eqIfPresent(ModelMaterializedColumnDO::getValidFlag, reqDTO.getValidFlag())
                .orderBy(reqDTO.getOrderByColumn(), reqDTO.getIsAsc(), allowedColumns));
    }
}
