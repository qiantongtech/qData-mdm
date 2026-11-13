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
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedTableReqDTO;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelMaterializedTableDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 成功物化表定义 Mapper。
 */
public interface ModelMaterializedTableMapper extends BaseMapperX<ModelMaterializedTableDO> {

    /** 读取一个成功实例的完整物化表，保留停用和系统状态，交由公共查询核对历史完整性。 */
    List<ModelMaterializedTableDO> selectPublishedTables(@Param("instanceId") Long instanceId);

    /**
     * 分页查询当前成功且可用的权威主表绑定，历史成功实例不重复作为当前绑定返回。
     *
     * @param reqVO 模型名称、类型、目录及分页条件
     * @return 当前可用权威主表分页结果
     */
    default PageResult<ModelMaterializedTableDO> selectAvailableMainPage(ModelBasePageReqVO reqVO) {
        IPage<ModelMaterializedTableDO> page = selectAvailableMainPageByXml(
                new Page<>(reqVO.getPageNum(), reqVO.getPageSize()), reqVO);
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    /**
     * 执行当前可用权威主表绑定的XML分页查询，按模型成功指针及当前版本状态筛选。
     *
     * @param page  MyBatis Plus分页参数
     * @param reqVO 模型筛选条件
     * @return 包含记录及总数的分页结果
     */
    IPage<ModelMaterializedTableDO> selectAvailableMainPageByXml(
            Page<ModelMaterializedTableDO> page, @Param("req") ModelBasePageReqVO reqVO);

    /**
     * 查询存在多个当前可用权威主表绑定的模型，供调用方检查绑定唯一性。
     *
     * @return 重复绑定的模型ID列表；无重复时为空列表
     */
    List<Long> selectDuplicateActiveMainModelIds();

    /**
     * 按主模型查询当前成功且可用的权威主表绑定。
     *
     * @param modelId 主模型ID
     * @return 权威主表绑定列表；模型封存或没有可用绑定时为空列表
     */
    List<ModelMaterializedTableDO> selectAvailableMainInstances(@Param("modelId") Long modelId);

    /**
     * 按发布实例查询当前可用权威主表绑定；历史成功实例和失败实例不返回绑定。
     *
     * @param instanceId 发布实例ID
     * @return 当前权威主表绑定列表；实例不可用时为空列表
     */
    List<ModelMaterializedTableDO> selectAvailableMainByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 查询指定成功实例中的可用权威子表，校验子模型当前版本及同实例、同角色的父表绑定。
     *
     * @param instanceId 主模型成功发布实例ID
     * @param submodelId 子模型ID
     * @return 可用权威子表绑定列表
     */
    List<ModelMaterializedTableDO> selectAvailableSubmodelInstances(
            @Param("instanceId") Long instanceId, @Param("submodelId") Long submodelId);

    /**
     * 分页查询未逻辑删除的物化表定义；不限定当前成功指针，支持查看历史实例。
     *
     * @param reqDTO 分页、筛选及排序参数，排序字段限于白名单
     * @return 符合条件的分页记录
     */
    default PageResult<ModelMaterializedTableDO> selectPage(ModelMaterializedTableReqDTO reqDTO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        // eqIfPresent只在请求参数有值时加筛选条件；排序字段还要经过白名单检查，避免直接拼入任意列名。
        return selectPage(reqDTO, new LambdaQueryWrapperX<ModelMaterializedTableDO>()
                .eqIfPresent(ModelMaterializedTableDO::getId, reqDTO.getId())
                .eqIfPresent(ModelMaterializedTableDO::getInstanceId, reqDTO.getInstanceId())
                .eqIfPresent(ModelMaterializedTableDO::getModelId, reqDTO.getModelId())
                .eqIfPresent(ModelMaterializedTableDO::getParentId, reqDTO.getParentId())
                .eqIfPresent(ModelMaterializedTableDO::getTableRole, reqDTO.getTableRole())
                .eqIfPresent(ModelMaterializedTableDO::getParentTableId, reqDTO.getParentTableId())
                .eqIfPresent(ModelMaterializedTableDO::getDatasourceId, reqDTO.getDatasourceId())
                .eqIfPresent(ModelMaterializedTableDO::getTableName, reqDTO.getTableName())
                .eqIfPresent(ModelMaterializedTableDO::getEnabledFlag, reqDTO.getEnabledFlag())
                .eqIfPresent(ModelMaterializedTableDO::getValidFlag, reqDTO.getValidFlag())
                .orderBy(reqDTO.getOrderByColumn(), reqDTO.getIsAsc(), allowedColumns));
    }
}
