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
import tech.qiantong.qdata.mdm.master.dal.dataobject.source.MasterSourceRelDO;
import tech.qiantong.qdata.mdm.master.dal.dto.source.MasterSourceSystemDTO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import java.util.List;

/**
 * 权威主数据来源映射Mapper接口
 *
 * @author qdata
 * @date 2026-10-09
 */
public interface MasterSourceRelMapper extends BaseMapperX<MasterSourceRelDO> {

    /**
     * 查询全部有效来源关系及关联的采集记录、来源系统和采集版本。
     */
    List<MasterSourceRelDO> selectSourceRecords(@Param("modelId") Long modelId, @Param("masterRecordId") Long masterRecordId);

    /**
     * 查询模型正式数据关联的有效来源系统；物理表名须由业务层通过标识符白名单校验。
     */
    List<MasterSourceSystemDTO> selectSourceSystems(@Param("modelId") Long modelId);

    /**
     * 查询指定来源系统关联的有效主数据记录 ID。
     *
     * @param modelId 主数据模型 ID
     * @param sourceSystemId 来源系统 ID
     * @return 主数据记录 ID 集合
     */
    List<Long> selectMasterRecordIdsBySourceSystem(@Param("modelId") Long modelId,
                                                    @Param("sourceSystemId") Long sourceSystemId);
}
