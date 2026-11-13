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

package tech.qiantong.qdata.mdm.merge.dal.mapper.detect;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;

import java.util.List;

/**
 * 定时同步所需的任务锁和任务编号查询。
 *
 * @author chentangwen
 */
public interface MergeDuplicateTaskScheduleMapper {

    @Select("SELECT * FROM MERGE_DUPLICATE_TASK WHERE ID=#{id} FOR UPDATE")
    @Options(timeout = 5)
    MergeDuplicateTaskDO lockScheduleTask(@Param("id") Long id);

    @Select("SELECT ID FROM MERGE_DUPLICATE_TASK")
    List<Long> selectTaskIds();
}
