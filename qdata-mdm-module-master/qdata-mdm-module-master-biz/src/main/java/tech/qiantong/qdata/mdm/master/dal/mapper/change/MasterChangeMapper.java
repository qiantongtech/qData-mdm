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

package tech.qiantong.qdata.mdm.master.dal.mapper.change;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangePageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeDO;
import tech.qiantong.qdata.mdm.master.dal.dto.change.MasterChangeRecordDTO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

/**
 * MASTER_CHANGE 权威主数据变更记录 Mapper。
 */
public interface MasterChangeMapper extends BaseMapperX<MasterChangeDO> {

    /**
     * 分页查询指定主数据记录已生效的业务变更，并关联有效申请单号。
     *
     * @param page 分页参数
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @param reqVO 分页及排序参数，XML 仅开放 id，其他字段采用默认排序
     * @return 变更分页以及列表展示所需字段
     */
    IPage<MasterChangeRecordDTO> selectEffectiveChangePage(
            Page<MasterChangeRecordDTO> page,
            @Param("modelId") Long modelId,
            @Param("masterRecordId") Long masterRecordId,
            @Param("req") MasterChangePageReqVO reqVO);
}
