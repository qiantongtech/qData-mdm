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

package tech.qiantong.qdata.mdm.ingest.dal.mapper.task;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDO;

import java.util.Date;
import java.util.List;

/**
 * 调度同步包含已删除任务，以便移除遗留的触发器。
 */
public interface IngestTaskScheduleMapper {
    /**
     * 锁定任务行，串行化调度同步、执行占用及恢复操作；需在事务中调用。
     *
     * @param id 接入任务 ID
     * @return 任务记录，包含逻辑删除记录；不存在时为空
     */
    @Select("SELECT * FROM INGEST_TASK WHERE ID=#{id} FOR UPDATE")
    @Options(timeout = 5)
    IngestTaskDO lockScheduleTask(@Param("id") Long id);

    /**
     * 读取全部任务 ID，保留已删除任务以清理其关联调度。
     *
     * @return 待校正任务 ID 列表
     */
    @Select("SELECT ID FROM INGEST_TASK")
    List<Long> selectTaskIds();

    /**
     * 仅为未删除、已启用且空闲的任务设置运行实例和数据库心跳时间。
     *
     * @param id 接入任务 ID
     * @param runId 新建的运行实例 ID
     * @return 更新行数，1 表示取得占用，0 表示条件已不满足
     */
    @Update("UPDATE INGEST_TASK SET ACTIVE_RUN_ID=#{runId}, RUN_HEARTBEAT_TIME=CURRENT_TIMESTAMP WHERE ID=#{id} "
            + "AND DEL_FLAG='0' AND STATUS='1' AND ACTIVE_RUN_ID IS NULL")
    int claimRun(@Param("id") Long id, @Param("runId") Long runId);

    /**
     * 按实例归属清空运行占用和心跳，旧实例不能释放后续实例的占用。
     *
     * @param id 接入任务 ID
     * @param runId 申请释放占用的实例 ID
     * @return 更新行数，0 表示占用已释放或已属于其他实例
     */
    @Update("UPDATE INGEST_TASK SET ACTIVE_RUN_ID=NULL, RUN_HEARTBEAT_TIME=NULL WHERE ID=#{id} AND ACTIVE_RUN_ID=#{runId}")
    int releaseRun(@Param("id") Long id, @Param("runId") Long runId);

    /**
     * 仅为仍持有占用的实例刷新心跳，时间由数据库生成。
     *
     * @param id 接入任务 ID
     * @param runId 本机正在负责的运行实例 ID
     * @return 更新行数，0 表示该实例已失去占用
     */
    @Update("UPDATE INGEST_TASK SET RUN_HEARTBEAT_TIME=CURRENT_TIMESTAMP WHERE ID=#{id} AND ACTIVE_RUN_ID=#{runId}")
    @Options(timeout = 3)
    int heartbeat(@Param("id") Long id, @Param("runId") Long runId);

    /**
     * 取得数据库当前时间，避免服务节点时钟差异影响租期判断。
     *
     * @return 数据库时间；任务表为空时返回空值
     */
    @Select("SELECT CURRENT_TIMESTAMP FROM INGEST_TASK WHERE ID=(SELECT MIN(ID) FROM INGEST_TASK)")
    Date databaseTime();

    /**
     * 查询有占用且心跳缺失或早于截止时间的任务，恢复前仍须加锁复核。
     *
     * @param before 按数据库时间计算的过期截止时间
     * @return 候选超时任务 ID 列表
     */
    @Select("SELECT ID FROM INGEST_TASK WHERE ACTIVE_RUN_ID IS NOT NULL "
            + "AND (RUN_HEARTBEAT_TIME IS NULL OR RUN_HEARTBEAT_TIME < #{before})")
    List<Long> selectExpiredRuns(@Param("before") Date before);
}
