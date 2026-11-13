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

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.controller.admin.source.vo.MasterSourceDetailRespVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.source.MasterSourceRelDO;
import tech.qiantong.qdata.mdm.master.dal.mapper.source.MasterSourceRelMapper;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataVersionService;
import tech.qiantong.qdata.mdm.master.service.source.IMasterFieldSourceRelService;
import tech.qiantong.qdata.mdm.master.service.source.IMasterSourceRelService;
import tech.qiantong.qdata.mdm.master.dal.repository.data.MasterDataQueryRepository;
import tech.qiantong.qdata.mdm.master.util.MasterRecordUtils;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MASTER_SOURCE_REL 来源关系及字段来源聚合查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MasterSourceRelServiceImpl implements IMasterSourceRelService {

    private final IMasterDataMetadataService metadataService;
    private final MasterDataQueryRepository dataQueryRepository;
    private final IMasterFieldSourceRelService fieldSourceRelService;
    private final IMasterDataVersionService dataVersionService;
    private final MasterSourceRelMapper masterSourceRelMapper;

    /**
     * 查询一条主数据关联的有效来源记录。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 主数据记录 ID
     * @return 来源记录及总数；无匹配时记录为空，字段来源由上层服务单独查询
     */
    @Override
    public MasterSourceDetailRespVO getSourceRecords(Long modelId, Long masterRecordId) {
        List<MasterSourceRelDO> rows = masterSourceRelMapper.selectSourceRecords(modelId, masterRecordId);
        MasterSourceDetailRespVO response = new MasterSourceDetailRespVO();
        response.setTotal((long) rows.size());
        List<MasterSourceDetailRespVO.SourceRecord> records = new ArrayList<>();
        for (MasterSourceRelDO row : rows) {
            MasterSourceDetailRespVO.SourceRecord item = new MasterSourceDetailRespVO.SourceRecord();
            item.setRelationId(value(row.getId()));
            item.setSourceRecordId(value(row.getSourceRecordId()));
            item.setSourceVersionId(value(row.getSourceVersionId()));
            item.setSourceSystemName(value(row.getSourceSystemName()));
            item.setSourceRecordKey(value(row.getSourceRecordKey()));
            item.setStableRecordId(value(row.getStableRecordId()));
            item.setRelationType(value(row.getType()));
            item.setEffectiveTime(row.getEffectiveTime());
            item.setLastReceiveTime(row.getLastReceiveTime());
            try {
                item.setModelData(modelData(row.getSourceModelData()));
            } catch (Exception ex) {
                throw new IllegalStateException("读取来源记录版本数据失败", ex);
            }
            records.add(item);
        }
        response.setRecords(records);
        return response;
    }

    /** 保持详情接口的字符串字段表示，避免长整型 ID 在前端丢失精度。 */
    private static String value(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 兼容 CLOB 和字符串快照；超过 65536 个字符时仅返回前段并附截断提示。 */
    private static String modelData(Object raw) throws Exception {
        if (raw == null) return null;
        if (raw instanceof Clob) {
            Clob clob = (Clob) raw;
            int length = (int) Math.min(clob.length(), 65536L);
            String content = clob.getSubString(1, length);
            return clob.length() > length ? content + "\n…内容过长，已截断" : content;
        }
        String content = String.valueOf(raw);
        return content.length() > 65536 ? content.substring(0, 65536) + "\n…内容过长，已截断" : content;
    }


    /**
     * 校验实例及记录归属后，返回全部有效来源记录和当前有效版本的字段取值来源。
     *
     * @param instanceId 可用模型实例 ID
     * @param masterDataId 权威主数据记录 ID
     * @return 全部有效来源记录及字段来源；没有当前版本时字段来源为空列表
     */
    @Override
    public MasterSourceDetailRespVO getDetail(Long instanceId, Long masterDataId) {
        if (masterDataId == null || masterDataId <= 0) throw new ServiceException("主数据ID格式无效");
        MasterModelRespDTO model = metadataService.getModel(instanceId);
        // 动态物理表没有固定 DO，仅校验记录归属，不加载完整主数据详情。
        MasterRecordUtils.requireRecord(dataQueryRepository, model, masterDataId);
        try {
            MasterSourceDetailRespVO response = getSourceRecords(
                    model.getModelId(), masterDataId);
            response.setFieldSources(detailLoadFieldSources(model.getModelId(), masterDataId));
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("查询主数据来源详情失败，instanceId={}, masterDataId={}", instanceId, masterDataId, ex);
            throw new ServiceException("来源详情查询失败，请检查来源关联数据");
        }
    }

    /** 当前版本由版本服务判定；多个有效当前版本的异常原样传递，不查询其他版本的字段来源。 */
    private List<MasterSourceDetailRespVO.FieldSource> detailLoadFieldSources(Long modelId, Long masterDataId) {
        Long versionId = dataVersionService.getCurrentVersionId(modelId, masterDataId);
        if (versionId == null) return Collections.emptyList();
        return fieldSourceRelService.getFieldSources(modelId, masterDataId, versionId);
    }
}
