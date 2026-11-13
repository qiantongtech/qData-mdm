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

package tech.qiantong.qdata.mdm.master.service.source;

import tech.qiantong.qdata.mdm.master.controller.admin.source.vo.MasterSourceDetailRespVO;

/**
 * MASTER_SOURCE_REL 来源关系及字段来源聚合查询。
 */
public interface IMasterSourceRelService {

    /**
     * 查询一条主数据关联的有效来源记录。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 主数据记录 ID
     * @return 来源记录及总数；无匹配时记录为空，字段来源由上层服务单独查询
     */
    MasterSourceDetailRespVO getSourceRecords(Long modelId, Long masterRecordId);

    /**
     * 查询来源关系并聚合当前版本字段来源。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @return 全部有效来源记录及当前版本字段来源
     */
    MasterSourceDetailRespVO getDetail(Long instanceId, Long masterDataId);
}
