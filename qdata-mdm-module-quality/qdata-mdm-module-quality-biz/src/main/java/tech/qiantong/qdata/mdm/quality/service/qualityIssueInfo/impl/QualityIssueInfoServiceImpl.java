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

package tech.qiantong.qdata.mdm.quality.service.qualityIssueInfo.impl;

import com.aliyun.oss.ServiceException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoPageReqVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoRespVO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueInfo.vo.QualityIssueInfoSaveReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueInfo.QualityIssueInfoMapper;
import tech.qiantong.qdata.mdm.quality.service.qualityIssueInfo.IQualityIssueInfoService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 问题数据明细Service业务层处理
 *
 * @author qdata
 * @date 2026-09-23
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class QualityIssueInfoServiceImpl  extends ServiceImpl<QualityIssueInfoMapper,QualityIssueInfoDO> implements IQualityIssueInfoService {
    @Resource
    private QualityIssueInfoMapper qualityIssueInfoMapper;

    @Override
    public PageResult<QualityIssueInfoDO> getQualityIssueInfoPage(QualityIssueInfoPageReqVO pageReqVO) {
        return qualityIssueInfoMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createQualityIssueInfo(QualityIssueInfoSaveReqVO createReqVO) {
        QualityIssueInfoDO dictType = BeanUtils.toBean(createReqVO, QualityIssueInfoDO.class);
        qualityIssueInfoMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateQualityIssueInfo(QualityIssueInfoSaveReqVO updateReqVO) {
        // 相关校验

        // 更新问题数据明细
        QualityIssueInfoDO updateObj = BeanUtils.toBean(updateReqVO, QualityIssueInfoDO.class);
        return qualityIssueInfoMapper.updateById(updateObj);
    }
    @Override
    public int removeQualityIssueInfo(Collection<Long> idList) {
        // 批量删除问题数据明细
        return qualityIssueInfoMapper.deleteBatchIds(idList);
    }

    @Override
    public QualityIssueInfoDO getQualityIssueInfoById(Long id) {
        return qualityIssueInfoMapper.selectById(id);
    }

    @Override
    public List<QualityIssueInfoDO> getQualityIssueInfoList() {
        return qualityIssueInfoMapper.selectList();
    }

    @Override
    public Map<Long, QualityIssueInfoDO> getQualityIssueInfoMap() {
        List<QualityIssueInfoDO> qualityIssueInfoList = qualityIssueInfoMapper.selectList();
        return qualityIssueInfoList.stream()
                .collect(Collectors.toMap(
                        QualityIssueInfoDO::getId,
                        qualityIssueInfoDO -> qualityIssueInfoDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入问题数据明细数据
         *
         * @param importExcelList 问题数据明细数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importQualityIssueInfo(List<QualityIssueInfoRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (QualityIssueInfoRespVO respVO : importExcelList) {
                try {
                    QualityIssueInfoDO qualityIssueInfoDO = BeanUtils.toBean(respVO, QualityIssueInfoDO.class);
                    Long qualityIssueInfoId = respVO.getId();
                    if (isUpdateSupport) {
                        if (qualityIssueInfoId != null) {
                            QualityIssueInfoDO existingQualityIssueInfo = qualityIssueInfoMapper.selectById(qualityIssueInfoId);
                            if (existingQualityIssueInfo != null) {
                                qualityIssueInfoMapper.updateById(qualityIssueInfoDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + qualityIssueInfoId + " 的问题数据明细记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + qualityIssueInfoId + " 的问题数据明细记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<QualityIssueInfoDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", qualityIssueInfoId);
                        QualityIssueInfoDO existingQualityIssueInfo = qualityIssueInfoMapper.selectOne(queryWrapper);
                        if (existingQualityIssueInfo == null) {
                            qualityIssueInfoMapper.insert(qualityIssueInfoDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + qualityIssueInfoId + " 的问题数据明细记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + qualityIssueInfoId + " 的问题数据明细记录已存在。");
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
