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

package tech.qiantong.qdata.mdm.ingest.dal.mapper.ingest;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * 接入任务历史运行记录查询。
 */
public interface IngestTaskInstanceMapper extends BaseMapperX<IngestTaskInstanceDO> {

    /**
     * 锁定未逻辑删除的实例行，串行化多节点对同一实例的日志归档。
     *
     * @param runId 运行实例 ID
     * @return 运行实例；不存在或已逻辑删除时为空
     */
    @Select("SELECT * FROM INGEST_TASK_INSTANCE WHERE ID=#{runId} AND DEL_FLAG='0' FOR UPDATE")
    @Options(timeout = 5)
    IngestTaskInstanceDO lockRun(@Param("runId") Long runId);

    /**
     * 按开始时间和实例 ID 倒序查询未删除的运行记录。
     *
     * @param taskId 所属接入任务 ID
     * @return 运行记录列表，分页由调用方配置
     */
    @Select("SELECT * FROM INGEST_TASK_INSTANCE WHERE TASK_ID=#{taskId} AND DEL_FLAG='0' "
            + "ORDER BY START_TIME DESC, ID DESC")
    List<IngestTaskInstanceDO> selectByTaskId(@Param("taskId") Long taskId);
}
