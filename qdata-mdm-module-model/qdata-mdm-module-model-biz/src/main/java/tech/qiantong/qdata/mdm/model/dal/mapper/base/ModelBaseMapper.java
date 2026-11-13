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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 主数据模型基本信息Mapper接口
 *
 * @author qdata
 * @date 2026-09-18
 */
public interface ModelBaseMapper extends BaseMapperX<ModelBaseDO> {

    /**
     * 批量查询引用模型及其当前版本、正式版本，与模型列表共用版本映射。
     */
    List<ModelBaseDO> selectModelsWithVersionsByIds(@Param("modelIds") Collection<Long> modelIds);

    /**
     * 查询未删除且有效的直接子模型；有效标志为空时保留既有兼容行为。
     */
    List<ModelBaseDO> selectAvailableSubmodels(@Param("parentId") Long parentId);

    /**
     * 分页查询主模型和版本基础信息，字段与子模型由服务层批量装配。
     */
    IPage<ModelBaseDO> selectModelBaseFlatByXml(Page page, @Param("req") ModelBasePageReqVO reqVO);

    /**
     * 不分页查询主模型列表。pageSize = -1 时使用，避免分页插件生成无效的负数分页条件。
     */
    List<ModelBaseDO> selectModelBaseFlatByXmlNoPage(@Param("req") ModelBasePageReqVO reqVO);

    /**
     * 批量查询模型字段，用于列表结果装配，避免逐模型查询产生 N+1。
     */
    List<ModelColumnDO> selectFieldsByModelIds(@Param("modelIds") Collection<Long> modelIds);

    /**
     * 批量查询直接子模型，用于列表结果装配，避免逐模型查询产生 N+1。
     */
    List<ModelBaseDO> selectSubModelsByParentIds(@Param("parentIds") Collection<Long> parentIds);

    /**
     * 查询模型详情。详情场景会加载子模型的版本信息和字段信息。
     */
    ModelBaseDO selectModelBaseFlatById(@Param("id") Long id);

    default PageResult<ModelBaseDO> selectPage(ModelBasePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelBaseDO>()
                .eqIfPresent(ModelBaseDO::getCode, reqVO.getCode())
                .likeIfPresent(ModelBaseDO::getName, reqVO.getName())
                .eqIfPresent(ModelBaseDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ModelBaseDO::getResponsibleUserId, reqVO.getResponsibleUserId())
                .eqIfPresent(ModelBaseDO::getLayerId, reqVO.getLayerId())
                .eqIfPresent(ModelBaseDO::getType, reqVO.getType())
                .eqIfPresent(ModelBaseDO::getVersionId, reqVO.getVersionId())
                .eqIfPresent(ModelBaseDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelBaseDO::getCreateTime, reqVO.getCreateTime())
                .isNull(ModelBaseDO::getParentId)
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelBaseDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
