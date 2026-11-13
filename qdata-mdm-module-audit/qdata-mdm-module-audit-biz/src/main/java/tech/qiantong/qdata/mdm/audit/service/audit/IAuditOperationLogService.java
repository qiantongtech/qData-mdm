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

package tech.qiantong.qdata.mdm.audit.service.audit;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogSaveReqVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogPageReqVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogRespVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
/**
 * 业务操作审计Service接口
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
public interface IAuditOperationLogService extends IService<AuditOperationLogDO> {

    /**
     * 获得业务操作审计分页列表
     *
     * @param pageReqVO 分页请求
     * @return 业务操作审计分页列表
     */
    PageResult<AuditOperationLogDO> getAuditOperationLogPage(AuditOperationLogPageReqVO pageReqVO);

    /**
     * 创建业务操作审计
     *
     * @param createReqVO 业务操作审计信息
     * @return 业务操作审计编号
     */
    Long createAuditOperationLog(AuditOperationLogSaveReqVO createReqVO);

    /**
     * 更新业务操作审计
     *
     * @param updateReqVO 业务操作审计信息
     */
    int updateAuditOperationLog(AuditOperationLogSaveReqVO updateReqVO);

    /**
     * 删除业务操作审计
     *
     * @param idList 业务操作审计编号
     */
    int removeAuditOperationLog(Collection<Long> idList);

    /**
     * 获得业务操作审计详情
     *
     * @param id 业务操作审计编号
     * @return 业务操作审计
     */
    AuditOperationLogDO getAuditOperationLogById(Long id);

    /**
     * 获得全部业务操作审计列表
     *
     * @return 业务操作审计列表
     */
    List<AuditOperationLogDO> getAuditOperationLogList();

    /**
     * 获得全部业务操作审计 Map
     *
     * @return 业务操作审计 Map
     */
    Map<Long, AuditOperationLogDO> getAuditOperationLogMap();


    /**
     * 导入业务操作审计数据
     *
     * @param importExcelList 业务操作审计数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importAuditOperationLog(List<AuditOperationLogRespVO> importExcelList, boolean isUpdateSupport, String operName);

}

