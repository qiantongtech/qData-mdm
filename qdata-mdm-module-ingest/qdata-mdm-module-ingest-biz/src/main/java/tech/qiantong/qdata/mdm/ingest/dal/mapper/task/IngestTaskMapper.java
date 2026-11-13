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

import java.util.*;
import org.apache.ibatis.annotations.*;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.*;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.task.*;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

public interface IngestTaskMapper extends BaseMapperX<IngestTaskDO> {
    IngestTaskRespVO selectDetail(@Param("id") Long id);

    List<IngestTaskRespVO> selectPageRows(@Param("q") IngestTaskPageReqVO q);

    @Select({"<script>",
            "SELECT TASK_ID, SOURCE_FIELD_NAME, UNIQUE_KEY_FLAG, UNIQUE_KEY_ORDER ",
            "FROM INGEST_TASK_FIELD_MAPPING WHERE DEL_FLAG='0' AND TASK_ID IN ",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>",
            "#{taskId}",
            "</foreach>",
            "ORDER BY TASK_ID, UNIQUE_KEY_ORDER",
            "</script>"})
    List<IngestTaskFieldMappingDO> selectMappingsByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    @Select("SELECT ID AS id, MODEL_ID AS model_id, NAME AS column_name, CODE AS column_code, "
            + "COLUMN_CATEGORY AS column_category, DATA_TYPE AS data_type, DATA_LENGTH AS data_length, "
            + "DATA_SCALE AS data_scale FROM MODEL_COLUMN "
            + "WHERE MODEL_ID = #{modelId} AND DEL_FLAG = '0' ORDER BY ID")
    List<IngestModelFieldRespVO> selectModelFields(@Param("modelId") Long modelId);

    @Select(
            "SELECT COUNT(1) FROM INGEST_TASK WHERE CODE=#{code} AND DEL_FLAG='0' AND ID <> COALESCE(#{id},-1)")
    long countCode(@Param("code") String code, @Param("id") Long id);

    @Select("SELECT COUNT(1) FROM SYSTEM_USER WHERE USER_ID=#{id} AND DEL_FLAG='0'")
    long countUser(@Param("id") Long id);

    @Update("UPDATE INGEST_TASK SET CODE=#{code} WHERE ID=#{id} AND DEL_FLAG='0'")
    int updateTaskCode(@Param("id") Long id, @Param("code") String code);

    @Select("SELECT * FROM INGEST_TASK_DB_CONFIG WHERE TASK_ID=#{id} AND DEL_FLAG='0'")
    IngestTaskDbConfigDO selectDb(@Param("id") Long id);

    @Select("SELECT * FROM INGEST_TASK_FILE_CONFIG WHERE TASK_ID=#{id} AND DEL_FLAG='0'")
    IngestTaskFileConfigDO selectFile(@Param("id") Long id);

    @Select("SELECT * FROM INGEST_TASK_API_CONFIG WHERE TASK_ID=#{id} AND DEL_FLAG='0'")
    IngestTaskApiConfigDO selectApi(@Param("id") Long id);

    @Select(
            "SELECT * FROM INGEST_TASK_FIELD_MAPPING WHERE TASK_ID=#{id} AND DEL_FLAG='0' ORDER BY SORT_ORDER")
    List<IngestTaskFieldMappingDO> selectMappings(@Param("id") Long id);

    @Delete("DELETE FROM INGEST_TASK_DB_CONFIG WHERE TASK_ID=#{id}")
    int deleteDb(@Param("id") Long id);

    @Delete("DELETE FROM INGEST_TASK_FILE_CONFIG WHERE TASK_ID=#{id}")
    int deleteFile(@Param("id") Long id);

    @Delete("DELETE FROM INGEST_TASK_API_CONFIG WHERE TASK_ID=#{id}")
    int deleteApi(@Param("id") Long id);

    @Delete("DELETE FROM INGEST_TASK_FIELD_MAPPING WHERE TASK_ID=#{id}")
    int deleteMappings(@Param("id") Long id);

    @Select({"<script>",
            "SELECT ID, CODE, NAME, TYPE, SOURCE_SYSTEM_ID, MODEL_ID, REMARK ",
            "FROM INGEST_TASK ",
            "WHERE ID IN ",
            "<foreach collection='taskIds' item='taskId' open='(' separator=',' close=')'>",
            "#{taskId}",
            "</foreach>",
            "</script>"})
    List<IngestTaskDO> selectByIdsForSourceData(@Param("taskIds") Collection<Long> taskIds);
}

