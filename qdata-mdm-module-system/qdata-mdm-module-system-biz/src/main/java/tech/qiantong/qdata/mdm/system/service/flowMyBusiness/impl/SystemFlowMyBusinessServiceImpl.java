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

package tech.qiantong.qdata.mdm.system.service.flowMyBusiness.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.system.api.flowMyBusiness.dto.SystemFlowMyBusinessReqDTO;
import tech.qiantong.qdata.mdm.system.api.flowMyBusiness.dto.SystemFlowMyBusinessRespDTO;
import tech.qiantong.qdata.mdm.system.api.flowMyBusiness.service.SysFlowMyBusinessService;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessSaveReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness.SystemFlowMyBusinessDO;
import tech.qiantong.qdata.mdm.system.dal.mapper.flowMyBusiness.SystemFlowMyBusinessMapper;
import tech.qiantong.qdata.mdm.system.service.flowMyBusiness.ISystemFlowMyBusinessService;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 审批记录Service业务层处理
 *
 * @author qdata
 * @date 2025-04-25
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SystemFlowMyBusinessServiceImpl extends ServiceImpl<SystemFlowMyBusinessMapper, SystemFlowMyBusinessDO> implements ISystemFlowMyBusinessService, SysFlowMyBusinessService {
    @Resource
    private SystemFlowMyBusinessMapper systemFlowMyBusinessMapper;

    @Override
    public PageResult<SystemFlowMyBusinessDO> getSystemFlowMyBusinessPage(SystemFlowMyBusinessPageReqVO pageReqVO) {
        return systemFlowMyBusinessMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<SystemFlowMyBusinessRespDTO> listPage(SystemFlowMyBusinessReqDTO updateReqVO) {
        SystemFlowMyBusinessPageReqVO flowMyBusinessPageReqVO = BeanUtils.toBean(updateReqVO, SystemFlowMyBusinessPageReqVO.class);
        PageResult<SystemFlowMyBusinessDO> systemFlowMyBusinessPage = this.getSystemFlowMyBusinessPage(flowMyBusinessPageReqVO);
        PageResult<SystemFlowMyBusinessRespDTO> myBusinessRespDTOPageResult = BeanUtils.toBean(systemFlowMyBusinessPage, SystemFlowMyBusinessRespDTO.class);
        return myBusinessRespDTOPageResult;
    }

    @Override
    public List<SystemFlowMyBusinessRespDTO> list(SystemFlowMyBusinessReqDTO updateReqVO) {
        LambdaQueryWrapperX<SystemFlowMyBusinessDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(SystemFlowMyBusinessDO::getActStatus, updateReqVO.getActStatus())
                     .eqIfPresent(SystemFlowMyBusinessDO::getType, updateReqVO.getType())
                     .eqIfPresent(SystemFlowMyBusinessDO::getDataId,updateReqVO.getDataId())
                     .eqIfPresent(SystemFlowMyBusinessDO::getCreatorId,updateReqVO.getUserId());
        List<SystemFlowMyBusinessDO> flowMyBusinessDOList = systemFlowMyBusinessMapper.selectList(queryWrapperX);
        return BeanUtils.toBean(flowMyBusinessDOList, SystemFlowMyBusinessRespDTO.class);
    }

    @Override
    public Long createSystemFlowMyBusiness(SystemFlowMyBusinessSaveReqVO createReqVO) {
        SystemFlowMyBusinessDO dictType = BeanUtils.toBean(createReqVO, SystemFlowMyBusinessDO.class);
        systemFlowMyBusinessMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateSystemFlowMyBusiness(SystemFlowMyBusinessSaveReqVO updateReqVO) {
        // 相关校验

        // 更新审批记录
        SystemFlowMyBusinessDO updateObj = BeanUtils.toBean(updateReqVO, SystemFlowMyBusinessDO.class);
        if (updateObj.getId() == null) {
            LambdaQueryWrapperX<SystemFlowMyBusinessDO> queryWrapperX = new LambdaQueryWrapperX<>();
            queryWrapperX.eqIfPresent(SystemFlowMyBusinessDO::getProcessInstanceId, updateObj.getProcessInstanceId());
            return systemFlowMyBusinessMapper.update(updateObj, queryWrapperX);
        }
        return systemFlowMyBusinessMapper.updateById(updateObj);
    }

    @Override
    public Long createSysFlowMyBusiness(SystemFlowMyBusinessReqDTO flowMyBusinessReqDTO) {
        SystemFlowMyBusinessDO dictType = BeanUtils.toBean(flowMyBusinessReqDTO, SystemFlowMyBusinessDO.class);
        systemFlowMyBusinessMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateSysFlowMyBusiness(SystemFlowMyBusinessReqDTO updateReqVO) {
        // 相关校验

        // 更新审批记录
        SystemFlowMyBusinessSaveReqVO updateObj = BeanUtils.toBean(updateReqVO, SystemFlowMyBusinessSaveReqVO.class);
        return this.updateSystemFlowMyBusiness(updateObj);
    }

    @Override
    public int removeSystemFlowMyBusiness(Collection<Long> idList) {
        // 批量删除审批记录
        return systemFlowMyBusinessMapper.deleteBatchIds(idList);
    }

    @Override
    public int removeFlowMyBusiness(SystemFlowMyBusinessReqDTO updateReqVO) {
        LambdaQueryWrapperX<SystemFlowMyBusinessDO> queryWrapperX = new LambdaQueryWrapperX<>();
        queryWrapperX.eqIfPresent(SystemFlowMyBusinessDO::getDataId,updateReqVO.getDataId()).eqIfPresent(SystemFlowMyBusinessDO::getType,updateReqVO.getType());
        return systemFlowMyBusinessMapper.delete(queryWrapperX);
    }

    @Override
    public SystemFlowMyBusinessDO getSystemFlowMyBusinessById(Long id) {
        return systemFlowMyBusinessMapper.selectById(id);
    }

    @Override
    public List<SystemFlowMyBusinessDO> getSystemFlowMyBusinessList() {
        return systemFlowMyBusinessMapper.selectList();
    }

    @Override
    public Map<Long, SystemFlowMyBusinessDO> getSystemFlowMyBusinessMap() {
        List<SystemFlowMyBusinessDO> systemFlowMyBusinessList = systemFlowMyBusinessMapper.selectList();
        return systemFlowMyBusinessList.stream()
                .collect(Collectors.toMap(
                        SystemFlowMyBusinessDO::getId,
                        systemFlowMyBusinessDO -> systemFlowMyBusinessDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 导入审批记录数据
     *
     * @param importExcelList 审批记录数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importSystemFlowMyBusiness(List<SystemFlowMyBusinessRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (SystemFlowMyBusinessRespVO respVO : importExcelList) {
            try {
                SystemFlowMyBusinessDO systemFlowMyBusinessDO = BeanUtils.toBean(respVO, SystemFlowMyBusinessDO.class);
                Long systemFlowMyBusinessId = respVO.getId();
                if (isUpdateSupport) {
                    if (systemFlowMyBusinessId != null) {
                        SystemFlowMyBusinessDO existingSystemFlowMyBusiness = systemFlowMyBusinessMapper.selectById(systemFlowMyBusinessId);
                        if (existingSystemFlowMyBusiness != null) {
                            systemFlowMyBusinessMapper.updateById(systemFlowMyBusinessDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + systemFlowMyBusinessId + " 的审批记录记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + systemFlowMyBusinessId + " 的审批记录记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<SystemFlowMyBusinessDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", systemFlowMyBusinessId);
                    SystemFlowMyBusinessDO existingSystemFlowMyBusiness = systemFlowMyBusinessMapper.selectOne(queryWrapper);
                    if (existingSystemFlowMyBusiness == null) {
                        systemFlowMyBusinessMapper.insert(systemFlowMyBusinessDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + systemFlowMyBusinessId + " 的审批记录记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + systemFlowMyBusinessId + " 的审批记录记录已存在。");
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
}
