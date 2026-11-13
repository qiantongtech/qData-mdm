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

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.ingest.IngestTaskInstanceLogDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * 运行日志归档查询。
 */
public interface IngestTaskInstanceLogMapper extends BaseMapperX<IngestTaskInstanceLogDO> {
    /**
     * 查询实例已持久化的最大事件序号，供 Redis 序号续接和归档去重使用。
     * 历史记录没有 EVENT_SEQ 时取 ID；包含已逻辑删除日志，避免重用旧序号。
     *
     * @param runId 运行实例 ID
     * @return 最大事件序号，没有记录时返回零
     */
    @Select("SELECT COALESCE(MAX(COALESCE(EVENT_SEQ,ID)),0) FROM INGEST_TASK_INSTANCE_LOG WHERE TASK_INSTANCE_ID=#{runId}")
    long maxSequence(@Param("runId") Long runId);

    /**
     * 按排他游标查询未删除日志并按序号升序返回。
     *
     * @param runId 运行实例 ID
     * @param after 已读取的最大序号，不包含该序号对应的记录
     * @return 游标后的日志列表，返回条数由调用方分页控制
     */
    @Select("SELECT ID, TASK_INSTANCE_ID, COALESCE(EVENT_SEQ,ID) AS EVENT_SEQ, LOG_LEVEL, STAGE, MESSAGE, LOG_TIME "
            + "FROM INGEST_TASK_INSTANCE_LOG WHERE TASK_INSTANCE_ID=#{runId} AND DEL_FLAG='0' "
            + "AND COALESCE(EVENT_SEQ,ID)>#{after} ORDER BY COALESCE(EVENT_SEQ,ID)")
    List<IngestTaskInstanceLogDO> selectAfter(@Param("runId") Long runId, @Param("after") long after);
}
