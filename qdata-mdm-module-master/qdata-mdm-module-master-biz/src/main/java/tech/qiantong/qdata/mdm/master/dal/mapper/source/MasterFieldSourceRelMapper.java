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

package tech.qiantong.qdata.mdm.master.dal.mapper.source;

import org.apache.ibatis.annotations.Param;

import tech.qiantong.qdata.mdm.master.dal.dataobject.source.MasterFieldSourceRelDO;
import tech.qiantong.qdata.mdm.master.dal.dto.source.MasterFieldSourceDTO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * MASTER_FIELD_SOURCE_REL 字段取值来源关系 Mapper。
 */
public interface MasterFieldSourceRelMapper extends BaseMapperX<MasterFieldSourceRelDO> {

    /**
     * 查询指定主数据版本的有效字段来源，关联有效的采集记录、来源系统和采集版本。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @param masterVersionId 当前有效主数据版本 ID
     * @return 字段路径、来源字段路径、系统名称及来源记录和版本 ID；无匹配时返回空列表
     */
    List<MasterFieldSourceDTO> selectFieldSources(
            @Param("modelId") Long modelId,
            @Param("masterRecordId") Long masterRecordId,
            @Param("masterVersionId") Long masterVersionId);
}
