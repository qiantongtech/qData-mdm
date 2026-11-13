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

package tech.qiantong.qdata.mdm.audit.service.audit.impl;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogPageReqVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogRespVO;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogSaveReqVO;
import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
import tech.qiantong.qdata.mdm.audit.dal.mapper.audit.AuditOperationLogMapper;
import tech.qiantong.qdata.mdm.audit.service.audit.IAuditOperationLogService;
/**
 * 业务操作审计Service业务层处理
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AuditOperationLogServiceImpl  extends ServiceImpl<AuditOperationLogMapper,AuditOperationLogDO> implements IAuditOperationLogService {
    @Resource
    private AuditOperationLogMapper auditOperationLogMapper;
    @Resource
    private AuditOperationRelationService auditOperationRelationService;

    @Override
    public PageResult<AuditOperationLogDO> getAuditOperationLogPage(AuditOperationLogPageReqVO pageReqVO) {
        return auditOperationLogMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createAuditOperationLog(AuditOperationLogSaveReqVO createReqVO) {
        AuditOperationLogDO dictType = BeanUtils.toBean(createReqVO, AuditOperationLogDO.class);
        auditOperationRelationService.validate(dictType);
        auditOperationLogMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateAuditOperationLog(AuditOperationLogSaveReqVO updateReqVO) {
        // 相关校验

        // 更新业务操作审计
        AuditOperationLogDO updateObj = BeanUtils.toBean(updateReqVO, AuditOperationLogDO.class);
        prepareRelationsForUpdate(updateObj);
        return auditOperationLogMapper.updateById(updateObj);
    }
    @Override
    public int removeAuditOperationLog(Collection<Long> idList) {
        // 批量删除业务操作审计
        return auditOperationLogMapper.deleteBatchIds(idList);
    }

    @Override
    public AuditOperationLogDO getAuditOperationLogById(Long id) {
        return auditOperationLogMapper.selectById(id);
    }

    @Override
    public List<AuditOperationLogDO> getAuditOperationLogList() {
        return auditOperationLogMapper.selectList();
    }

    @Override
    public Map<Long, AuditOperationLogDO> getAuditOperationLogMap() {
        List<AuditOperationLogDO> auditOperationLogList = auditOperationLogMapper.selectList();
        return auditOperationLogList.stream()
                .collect(Collectors.toMap(
                        AuditOperationLogDO::getId,
                        auditOperationLogDO -> auditOperationLogDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入业务操作审计数据
         *
         * @param importExcelList 业务操作审计数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importAuditOperationLog(List<AuditOperationLogRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (AuditOperationLogRespVO respVO : importExcelList) {
                try {
                    AuditOperationLogDO auditOperationLogDO = BeanUtils.toBean(respVO, AuditOperationLogDO.class);
                    Long auditOperationLogId = respVO.getId();
                    if (isUpdateSupport) {
                        if (auditOperationLogId != null) {
                            AuditOperationLogDO existingAuditOperationLog = auditOperationLogMapper.selectById(auditOperationLogId);
                            if (existingAuditOperationLog != null) {
                                prepareRelationsForUpdate(auditOperationLogDO);
                                auditOperationLogMapper.updateById(auditOperationLogDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + auditOperationLogId + " 的业务操作审计记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + auditOperationLogId + " 的业务操作审计记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        AuditOperationLogDO existingAuditOperationLog = auditOperationLogId == null
                                ? null : auditOperationLogMapper.selectById(auditOperationLogId);
                        if (existingAuditOperationLog == null) {
                            auditOperationRelationService.validate(auditOperationLogDO);
                            auditOperationLogMapper.insert(auditOperationLogDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + auditOperationLogId + " 的业务操作审计记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + auditOperationLogId + " 的业务操作审计记录已存在。");
                        }
                    }
                } catch (Exception e) {
                    failureNum++;
                    String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                    failureMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }
            StringBuilder resultMsg = new StringBuilder();
            if (failureNum > 0) {
                resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
                resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
                throw new ServiceException(resultMsg.toString());
            } else {
                resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
            }
            return resultMsg.toString();
        }
    /** 编辑未传的关联字段沿用原值，再校验组合关系。 */
    private void prepareRelationsForUpdate(AuditOperationLogDO update) {
        AuditOperationLogDO existing = update.getId() == null ? null : auditOperationLogMapper.selectById(update.getId());
        if (existing == null) throw new ServiceException("操作日志不存在");
        if (update.getModelId() == null) update.setModelId(existing.getModelId());
        if (update.getModelVersionId() == null) update.setModelVersionId(existing.getModelVersionId());
        if (update.getMasterRecordId() == null) update.setMasterRecordId(existing.getMasterRecordId());
        if (update.getChangeId() == null) update.setChangeId(existing.getChangeId());
        auditOperationRelationService.validate(update);
    }
}

