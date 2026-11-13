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

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskDbConfigDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

public interface IngestTaskDbConfigMapper extends BaseMapperX<IngestTaskDbConfigDO> {
    /** 仅推进本次执行使用的配置，避免覆盖编辑后重建的配置。 */
    @Update("UPDATE INGEST_TASK_DB_CONFIG SET CURRENT_INCREMENT_VALUE=#{cursor}, UPDATE_TIME=CURRENT_TIMESTAMP "
            + "WHERE ID=#{configId} AND TASK_ID=#{taskId} AND DEL_FLAG='0'")
    int updateIncrementValue(@Param("configId") Long configId, @Param("taskId") Long taskId,
                             @Param("cursor") String cursor);
}
