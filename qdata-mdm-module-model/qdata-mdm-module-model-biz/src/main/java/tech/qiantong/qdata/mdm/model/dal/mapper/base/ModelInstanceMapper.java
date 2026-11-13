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
import org.apache.ibatis.session.ResultHandler;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceHistoryRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelInstanceReqDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedPageReqDTO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 模型发布实例 Mapper。
 */
public interface ModelInstanceMapper extends BaseMapperX<ModelInstanceDO> {

    /**
     * 进度保存只取归属、状态、原计划及结构摘要，不重复读取快照、变更内容和累计SQL等大字段。
     *
     * @param instanceId 发布实例ID
     * @return 本次校验和日志更新需要的实例信息
     */
    default ModelInstanceDO selectExecutionProgressById(Long instanceId) {
        return selectOne(new LambdaQueryWrapperX<ModelInstanceDO>()
                .select(ModelInstanceDO::getId, ModelInstanceDO::getModelId, ModelInstanceDO::getModelVersionId,
                        ModelInstanceDO::getDatasourceId, ModelInstanceDO::getBaseInstanceId,
                        ModelInstanceDO::getStatus, ModelInstanceDO::getCurrentStage,
                        ModelInstanceDO::getStartedTime, ModelInstanceDO::getFinishedTime,
                        ModelInstanceDO::getExecutionPlan, ModelInstanceDO::getStructureHash)
                .eq(ModelInstanceDO::getId, instanceId));
    }

    /**
     * 正式保存只读取原计划、快照、归属、状态和创建信息，不读取累计SQL、变更明细及残留效果。
     * 每个保存事务仍重新查询，不复用上一个事务读到的实例状态。
     *
     * @param instanceId 发布实例ID
     * @return 正式保存及还原原上下文需要的实例信息
     */
    default ModelInstanceDO selectCommitById(Long instanceId) {
        return selectOne(new LambdaQueryWrapperX<ModelInstanceDO>()
                .select(ModelInstanceDO::getId, ModelInstanceDO::getModelId, ModelInstanceDO::getModelVersionId,
                        ModelInstanceDO::getDatasourceId, ModelInstanceDO::getBaseInstanceId,
                        ModelInstanceDO::getStatus, ModelInstanceDO::getCurrentStage,
                        ModelInstanceDO::getCompensationStatus, ModelInstanceDO::getStartedTime,
                        ModelInstanceDO::getFinishedTime, ModelInstanceDO::getModelSnapshot,
                        ModelInstanceDO::getExecutionPlan, ModelInstanceDO::getStructureHash,
                        ModelInstanceDO::getCreatorId, ModelInstanceDO::getCreateBy)
                .eq(ModelInstanceDO::getId, instanceId));
    }

    /**
     * 资源关闭日志只需要实例归属、当前状态和起止时间，不读取快照、计划等大字段。
     *
     * @param instanceId 发布实例ID
     * @return CLOSE阶段校验和日志更新需要的实例信息
     */
    default ModelInstanceDO selectCloseById(Long instanceId) {
        return selectOne(new LambdaQueryWrapperX<ModelInstanceDO>()
                .select(ModelInstanceDO::getId, ModelInstanceDO::getModelId,
                        ModelInstanceDO::getStatus, ModelInstanceDO::getCurrentStage,
                        ModelInstanceDO::getStartedTime, ModelInstanceDO::getFinishedTime)
                .eq(ModelInstanceDO::getId, instanceId));
    }

    /**
     * 按主模型ID查询真实发布历史，最新记录在前，成功、失败及进行中的记录都保留。
     */
    List<ModelInstanceHistoryRespDTO> selectHistoryByModelId(@Param("modelId") Long modelId);

    /** 按成功快照筛选计数，不传输快照大字段。 */
    long countPublishedInstances(@Param("req") ModelPublishedPageReqDTO request);

    Long selectPublishedModelId(@Param("instanceId") Long instanceId);

    /** 按需关联权威主表，一次读取成功快照，避免基础、字段、物化查询重复读取实例。 */
    ModelInstanceDO selectPublishedStructure(@Param("modelId") Long modelId,
                                           @Param("includeAuthorityTable") boolean includeAuthorityTable);

    long countPublishedInstance(@Param("modelId") Long modelId, @Param("instanceId") Long instanceId,
                                @Param("versionId") Long versionId);

    /** 数据库分页，返回当前页成功快照及权威主表统计定位信息。 */
    IPage<ModelInstanceDO> selectPublishedInstancePage(Page<ModelInstanceDO> page,
                                                      @Param("req") ModelPublishedPageReqDTO request);

    /** 按相同筛选条件查询全部成功快照，不分页。 */
    List<ModelInstanceDO> selectPublishedInstanceList(@Param("req") ModelPublishedPageReqDTO request);

    /**
     * 按成功指针逐条读取可用主模型；回调处理配置，不在应用中积攒全部完整JSON。
     */
    void selectPublishedInstances(@Param("modelId") Long modelId,
                                  @Param("includeMaterialization") boolean includeMaterialization,
                                  ResultHandler<ModelInstanceDO> handler);

    /**
     * 按模型、版本、发布状态及补偿状态等条件分页查询发布记录，保留成功和失败历史。
     *
     * @param reqDTO 分页、筛选及排序参数，排序字段限于白名单
     * @return 符合条件的发布记录分页结果
     */
    default PageResult<ModelInstanceDO> selectPage(ModelInstanceReqDTO reqDTO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        // eqIfPresent只在请求参数有值时加筛选条件；排序字段还要经过白名单检查，避免直接拼入任意列名。
        return selectPage(reqDTO, new LambdaQueryWrapperX<ModelInstanceDO>()
                .eqIfPresent(ModelInstanceDO::getId, reqDTO.getId())
                .eqIfPresent(ModelInstanceDO::getModelId, reqDTO.getModelId())
                .eqIfPresent(ModelInstanceDO::getModelVersionId, reqDTO.getModelVersionId())
                .eqIfPresent(ModelInstanceDO::getDatasourceId, reqDTO.getDatasourceId())
                .eqIfPresent(ModelInstanceDO::getBaseInstanceId, reqDTO.getBaseInstanceId())
                .eqIfPresent(ModelInstanceDO::getStatus, reqDTO.getStatus())
                .eqIfPresent(ModelInstanceDO::getCurrentStage, reqDTO.getCurrentStage())
                .eqIfPresent(ModelInstanceDO::getChangeStatus, reqDTO.getChangeStatus())
                .eqIfPresent(ModelInstanceDO::getCompensationStatus, reqDTO.getCompensationStatus())
                .orderBy(reqDTO.getOrderByColumn(), reqDTO.getIsAsc(), allowedColumns));
    }
}
