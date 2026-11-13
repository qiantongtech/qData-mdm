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

package tech.qiantong.qdata.mdm.master.service.source.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.master.controller.admin.source.vo.MasterSourceDetailRespVO;
import tech.qiantong.qdata.mdm.master.dal.dto.source.MasterFieldSourceDTO;
import tech.qiantong.qdata.mdm.master.dal.mapper.source.MasterFieldSourceRelMapper;
import tech.qiantong.qdata.mdm.master.service.source.IMasterFieldSourceRelService;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 MASTER_FIELD_SOURCE_REL 的关联查询结果映射为详情页字段来源，不补造缺失血缘。
 */
@Service
@RequiredArgsConstructor
public class MasterFieldSourceRelServiceImpl implements IMasterFieldSourceRelService {

    private final MasterFieldSourceRelMapper masterFieldSourceRelMapper;

    /**
     * 查询指定主数据版本的有效字段来源，并转换为来源详情页的五个展示字段。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @param masterVersionId 当前有效主数据版本 ID，由版本服务查询并校验
     * @return 字段来源列表；无匹配记录时为空列表，来源记录及版本 BIGINT ID 以字符串表示
     */
    @Override
    public List<MasterSourceDetailRespVO.FieldSource> getFieldSources(
            Long modelId, Long masterRecordId, Long masterVersionId) {
        List<MasterFieldSourceDTO> rows = masterFieldSourceRelMapper.selectFieldSources(
                modelId, masterRecordId, masterVersionId);
        List<MasterSourceDetailRespVO.FieldSource> result = new ArrayList<>();
        for (MasterFieldSourceDTO row : rows) {
            MasterSourceDetailRespVO.FieldSource item = new MasterSourceDetailRespVO.FieldSource();
            item.setFieldPath(row.getFieldPath());
            item.setSourceFieldPath(row.getSourceFieldPath());
            item.setSourceSystemName(row.getSourceSystemName());
            item.setSourceRecordId(value(row.getSourceRecordId()));
            item.setSourceVersionId(value(row.getSourceVersionId()));
            result.add(item);
        }
        return result;
    }

    /** 以字符串保留 BIGINT ID 的精度。 */
    private static String value(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
