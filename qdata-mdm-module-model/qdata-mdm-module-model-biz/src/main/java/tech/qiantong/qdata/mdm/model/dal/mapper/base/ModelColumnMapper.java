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

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * 主数据模型字段定义Mapper接口
 *
 * @author qdata
 * @date 2026-09-20
 */
public interface ModelColumnMapper extends BaseMapperX<ModelColumnDO> {

    /**
     * 查询待恢复的原字段，只允许读取当前模型下、编码一致且已经逻辑删除的记录。
     *
     * @param id 原字段ID
     * @param modelId 当前所属模型ID
     * @param code 原字段编码
     * @return 已删除的原字段，找不到时返回null
     */
    ModelColumnDO selectDeletedField(@Param("id") Long id, @Param("modelId") Long modelId, @Param("code") String code);

    /**
     * 按原ID、所属模型和字段编码恢复逻辑删除记录，不创建新字段，也不修改物理列。
     *
     * @param id 原字段ID
     * @param modelId 当前所属模型ID
     * @param code 原字段编码
     * @return 恢复的记录数
     */
    int restoreDeletedField(@Param("id") Long id, @Param("modelId") Long modelId, @Param("code") String code);

    /** 查询模型未删除字段，按配置排序值和 ID 保持稳定顺序。 */
    default List<ModelColumnDO> selectListByModelId(Long modelId) {
        return selectList(new LambdaQueryWrapperX<ModelColumnDO>()
                .eq(ModelColumnDO::getModelId, modelId)
                .eq(ModelColumnDO::getDelFlag, false)
                .orderByAsc(ModelColumnDO::getSortOrder, ModelColumnDO::getId));
    }

    default PageResult<ModelColumnDO> selectPage(ModelColumnPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelColumnDO>()
                .eqIfPresent(ModelColumnDO::getModelId, reqVO.getModelId())
                .likeIfPresent(ModelColumnDO::getName, reqVO.getName())
                .eqIfPresent(ModelColumnDO::getCode, reqVO.getCode())
                .eqIfPresent(ModelColumnDO::getColumnCategory, reqVO.getColumnCategory())
                .eqIfPresent(ModelColumnDO::getDataType, reqVO.getDataType())
                .eqIfPresent(ModelColumnDO::getDataLength, reqVO.getDataLength())
                .eqIfPresent(ModelColumnDO::getDataScale, reqVO.getDataScale())
                .eqIfPresent(ModelColumnDO::getPrimaryKeyFlag, reqVO.getPrimaryKeyFlag())
                .eqIfPresent(ModelColumnDO::getRequiredFlag, reqVO.getRequiredFlag())
                .eqIfPresent(ModelColumnDO::getUniqueFlag, reqVO.getUniqueFlag())
                .eqIfPresent(ModelColumnDO::getNameFlag, reqVO.getNameFlag())
                .eqIfPresent(ModelColumnDO::getDefaultValue, reqVO.getDefaultValue())
                .eqIfPresent(ModelColumnDO::getDictTypeId, reqVO.getDictTypeId())
                .eqIfPresent(ModelColumnDO::getEncodingVersionId, reqVO.getEncodingVersionId())
                .eqIfPresent(ModelColumnDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelColumnDO::getCreateTime, reqVO.getCreateTime())
                .eqIfPresent(ModelColumnDO::getRelatedModelId, reqVO.getRelatedModelId())
                .eqIfPresent(ModelColumnDO::getRelatedCode, reqVO.getRelatedCode())
                .eqIfPresent(ModelColumnDO::getDisplayCode, reqVO.getDisplayCode())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelColumnDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
