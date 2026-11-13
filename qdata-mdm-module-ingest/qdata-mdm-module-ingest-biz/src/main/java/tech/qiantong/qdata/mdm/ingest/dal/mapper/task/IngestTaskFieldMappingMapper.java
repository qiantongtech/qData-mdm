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
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.IngestTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.Collection;
import java.util.List;

public interface IngestTaskFieldMappingMapper extends BaseMapperX<IngestTaskFieldMappingDO> {
    @Select({"<script>",
            "SELECT ID, TASK_ID, SOURCE_FIELD_NAME, TARGET_FIELD_ID, TRANSFORM_TYPE, TRANSFORM_CONFIG, DEL_FLAG ",
            "FROM INGEST_TASK_FIELD_MAPPING WHERE DEL_FLAG = '0' AND TASK_ID IN ",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>#{taskId}</foreach>",
            "</script>"})
    List<IngestTaskFieldMappingDO> selectByTaskIdsForSourceData(@Param("taskIds") Collection<Long> taskIds);
}
