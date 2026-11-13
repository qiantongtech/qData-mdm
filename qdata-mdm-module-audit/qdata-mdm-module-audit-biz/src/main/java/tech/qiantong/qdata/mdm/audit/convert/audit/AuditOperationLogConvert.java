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

package tech.qiantong.qdata.mdm.audit.convert.audit;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogPageReqVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogSaveReqVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;

/**
 * 业务操作审计 Convert
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
@Mapper
public interface AuditOperationLogConvert {
    AuditOperationLogConvert INSTANCE = Mappers.getMapper(AuditOperationLogConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param auditOperationLogPageReqVO 请求参数
     * @return AuditOperationLogDO
     */
     AuditOperationLogDO convertToDO(AuditOperationLogPageReqVO auditOperationLogPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param auditOperationLogSaveReqVO 保存请求参数
     * @return AuditOperationLogDO
     */
     AuditOperationLogDO convertToDO(AuditOperationLogSaveReqVO auditOperationLogSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param auditOperationLogDO 实体对象
     * @return AuditOperationLogRespVO
     */
     AuditOperationLogRespVO convertToRespVO(AuditOperationLogDO auditOperationLogDO);

    /**
     * DOList 转换为 RespVOList
     * @param auditOperationLogDOList 实体对象列表
     * @return List<AuditOperationLogRespVO>
     */
     List<AuditOperationLogRespVO> convertToRespVOList(List<AuditOperationLogDO> auditOperationLogDOList);
}

