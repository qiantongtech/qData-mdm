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
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDatasourceRelDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.Collection;
import java.util.List;

/**
 * 来源系统与数据源关系
 *
 * @author chentangwen
 * @date 2026-09-15
 */
public interface IngestSourceDatasourceRelMapper extends BaseMapperX<IngestSourceDatasourceRelDO> {

    /**
     * 查询来源系统下被接入任务实际使用的数据源。
     *
     * @param sourceId 来源系统 ID
     * @return 被使用的数据源 ID
     */
    List<Long> selectUsedDatasourceIds(Long sourceId);

    /**
     * 批量查询来源系统与数据源的有效关联。
     *
     * @param ids 来源系统 ID
     * @return 关联关系列表
     */
    List<IngestSourceDatasourceRelDO> selectBySourceIds(@Param("ids") Collection<Long> ids);

    /**
     * 删除单个来源系统的全部数据源关联。
     *
     * @param sourceId 来源系统 ID
     * @return 删除行数
     */
    int deleteBySourceId(Long sourceId);

    /**
     * 批量删除来源系统的数据源关联。
     *
     * @param ids 来源系统 ID
     * @return 删除行数
     */
    int deleteBySourceIds(@Param("ids") Collection<Long> ids);
}
