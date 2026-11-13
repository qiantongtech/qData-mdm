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

package tech.qiantong.qdata.mdm.ingest.dal.mapper.source;

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceModelNameRow;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceResponsibleUserRow;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 来源系统
 *
 * @author chentangwen
 * @date 2026-09-15
 */
public interface IngestSourceMapper extends BaseMapperX<IngestSourceDO> {

    /**
     * 分页查询来源系统。
     *
     * @param reqVO 查询条件
     * @param referencedSourceIds 已被接入任务引用的来源系统 ID
     * @return 来源系统分页数据
     */
    default PageResult<IngestSourceDO> selectPage(IngestSourcePageReqVO reqVO,
                                                   Collection<Long> referencedSourceIds) {
        LambdaQueryWrapperX<IngestSourceDO> query = new LambdaQueryWrapperX<>();
        String keyword = reqVO.getKeyword() == null ? null : reqVO.getKeyword().trim();
        if (keyword != null && !keyword.isEmpty()) {
            List<Long> sourceIds = selectSourceIdsByDatasourceKeyword("%" + keyword + "%");
            query.and(wrapper -> {
                wrapper.like(IngestSourceDO::getName, keyword)
                        .or().like(IngestSourceDO::getCode, keyword)
                        .or().like(IngestSourceDO::getDescription, keyword);
                if (sourceIds != null && !sourceIds.isEmpty()) {
                    wrapper.or().in(IngestSourceDO::getId, sourceIds);
                }
            });
        }
        if (reqVO.getName() != null && !reqVO.getName().trim().isEmpty()) {
            query.like(IngestSourceDO::getName, reqVO.getName().trim());
        }
        if (reqVO.getCode() != null && !reqVO.getCode().trim().isEmpty()) {
            query.like(IngestSourceDO::getCode, reqVO.getCode().trim());
        }
        if (reqVO.getResponsiblePerson() != null && !reqVO.getResponsiblePerson().trim().isEmpty()) {
            if (reqVO.getResponsibleUserIds() == null || reqVO.getResponsibleUserIds().isEmpty()) {
                query.eq(IngestSourceDO::getId, -1L);
            } else {
                query.in(IngestSourceDO::getResponsibleUserId, reqVO.getResponsibleUserIds());
            }
        }
        if (reqVO.getDatasourceName() != null && !reqVO.getDatasourceName().trim().isEmpty()) {
            List<Long> sourceIds = selectSourceIdsByDatasourceKeyword(
                    "%" + reqVO.getDatasourceName().trim() + "%");
            if (sourceIds == null || sourceIds.isEmpty()) {
                query.eq(IngestSourceDO::getId, -1L);
            } else {
                query.in(IngestSourceDO::getId, sourceIds);
            }
        }
        if ("used".equals(reqVO.getRefFilter())) {
            if (referencedSourceIds == null || referencedSourceIds.isEmpty()) {
                query.eq(IngestSourceDO::getId, -1L);
            } else {
                query.in(IngestSourceDO::getId, referencedSourceIds);
            }
        } else if ("free".equals(reqVO.getRefFilter())
                && referencedSourceIds != null && !referencedSourceIds.isEmpty()) {
            query.notIn(IngestSourceDO::getId, referencedSourceIds);
        }

        Set<String> allowedColumns = new HashSet<>(Arrays.asList(
                "id", "code", "name", "responsible_user_id", "trust_score", "update_time", "create_time"));
        if (reqVO.getOrderByColumn() != null && !reqVO.getOrderByColumn().trim().isEmpty()) {
            List<String> columns = Arrays.stream(reqVO.getOrderByColumn().trim().split(","))
                    .map(String::trim)
                    .map(LambdaQueryWrapperX::camelToUnderline)
                    .collect(Collectors.toList());
            if (columns.stream().anyMatch(column -> !allowedColumns.contains(column))) {
                throw new IllegalArgumentException("非法的排序字段：" + reqVO.getOrderByColumn());
            }
            query.last("ORDER BY " + String.join(",", columns)
                    + ("asc".equalsIgnoreCase(reqVO.getIsAsc()) ? " ASC" : " DESC"));
        } else {
            query.orderByDesc(IngestSourceDO::getCreateTime)
                    .orderByDesc(IngestSourceDO::getId);
        }
        return selectPage(reqVO, query);
    }

    /**
     * 根据数据源名称查询关联的来源系统 ID。
     *
     * @param keyword 数据源名称关键字
     * @return 来源系统 ID 列表
     */
    List<Long> selectSourceIdsByDatasourceKeyword(String keyword);

    /**
     * 查询来源系统编码数量。
     *
     * @param code 来源系统编码
     * @return 编码数量
     */
    Long countByCode(String code);

    /**
     * 查询已被接入任务引用的来源系统 ID。
     *
     * @return 来源系统 ID 列表
     */
    List<Long> selectReferencedSourceIds();

    /**
     * 查询来源系统关联的接入任务数量。
     *
     * @param sourceId 来源系统 ID
     * @return 任务数量
     */
    Long countTaskBySourceId(Long sourceId);

    /**
     * 批量查询来源系统对应的任务来源 ID。
     *
     * @param ids 来源系统 ID
     * @return 任务来源 ID 列表
     */
    List<Long> selectTaskSourceIdsBySourceIds(@Param("ids") Collection<Long> ids);

    /**
     * 查询来源系统下的接入任务及最近运行时间。
     *
     * @param sourceId 来源系统 ID
     * @return 接入任务列表
     */
    List<IngestSourceTaskRespVO> selectTaskListBySourceId(Long sourceId);

    /**
     * 批量查询来源系统下任务使用的模型名称。
     *
     * @param ids 来源系统 ID
     * @return 来源系统模型名称列表
     */
    List<IngestSourceModelNameRow> selectModelNamesBySourceIds(@Param("ids") Collection<Long> ids);

    /**
     * 根据责任人关键字查询用户 ID。
     *
     * @param keyword 用户关键字
     * @return 用户 ID 列表
     */
    List<Long> selectResponsibleUserIdsByKeyword(String keyword);

    /**
     * 批量查询用户展示信息。
     *
     * @param ids 用户 ID
     * @return 用户信息列表
     */
    List<IngestSourceResponsibleUserRow> selectResponsibleUsersByIds(@Param("ids") Collection<Long> ids);

    /**
     * 查询来源数据页面所需的来源系统基础信息。
     *
     * @param sourceSystemIds 来源系统 ID
     * @return 来源系统基础信息
     */
    List<IngestSourceDO> selectByIdsForSourceData(@Param("sourceSystemIds") Collection<Long> sourceSystemIds);
}
