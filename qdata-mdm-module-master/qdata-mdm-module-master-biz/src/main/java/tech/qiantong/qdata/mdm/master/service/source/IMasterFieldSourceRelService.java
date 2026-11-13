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

import java.util.List;

/**
 * MASTER_FIELD_SOURCE_REL 字段取值来源的只读业务接口。
 */
public interface IMasterFieldSourceRelService {

    /**
     * 查询指定主数据版本的有效字段来源，并转换为来源详情页的五个展示字段。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @param masterVersionId 当前有效主数据版本 ID，由版本服务查询并校验
     * @return 字段来源列表；无匹配记录时为空列表，来源记录及版本 BIGINT ID 以字符串表示
     */
    List<MasterSourceDetailRespVO.FieldSource> getFieldSources(
            Long modelId, Long masterRecordId, Long masterVersionId);
}
