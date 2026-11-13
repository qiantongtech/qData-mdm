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

package tech.qiantong.qdata.mdm.rule.service.ruleSurvivor.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeRecordDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.SurvivorMergeFieldMapper;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.SurvivorMergeRecordMapper;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorSaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleSurvivor.RuleSurvivorMapper;
import tech.qiantong.qdata.mdm.rule.service.ruleSurvivor.IRuleSurvivorService;
/**
 * 生存规则Service业务层处理
 *
 * @author qdata
 * @date 2026-09-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RuleSurvivorServiceImpl  extends ServiceImpl<RuleSurvivorMapper,RuleSurvivorDO> implements IRuleSurvivorService {
    @Resource
    private RuleSurvivorMapper ruleSurvivorMapper;

    @Resource
    private SurvivorMergeFieldMapper survivorMergeFieldMapper;

    @Resource
    private SurvivorMergeRecordMapper survivorMergeRecordMapper;

    @Resource
    private ISysUserService sysUserService;

    private void validateResponsibleUser(Long userId) {
        if (userId != null && sysUserService.selectUserById(userId) == null) {
            throw new ServiceException("所选负责人不存在，请重新选择");
        }
    }

    @Override
    public PageResult<RuleSurvivorDO> getRuleSurvivorPage(RuleSurvivorPageReqVO pageReqVO) {
        PageResult<RuleSurvivorDO> page = ruleSurvivorMapper.selectPage(pageReqVO);
        if (page.getRows().isEmpty()) {
            return page;
        }
        Map<Long, RuleSurvivorDO> displayInfo = ruleSurvivorMapper.selectDisplayInfo(page.getRows().stream()
                .map(RuleSurvivorDO::getId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(RuleSurvivorDO::getId, item -> item));
        List<Long> ruleIds = page.getRows().stream().map(RuleSurvivorDO::getId).collect(Collectors.toList());
        Map<Long, Long> mergeCounts = new HashMap<>();
        for (int start = 0; start < ruleIds.size(); start += 500) {
            List<Long> batch = ruleIds.subList(start, Math.min(start + 500, ruleIds.size()));
            survivorMergeFieldMapper.selectDistinctAssociations(batch).forEach(item ->
                    mergeCounts.merge(item.getSurvivorRuleId(), 1L, Long::sum));
        }
        for (RuleSurvivorDO row : page.getRows()) {
            row.setMergeRecordCount(mergeCounts.getOrDefault(row.getId(), 0L));
            RuleSurvivorDO info = displayInfo.get(row.getId());
            if (info != null) {
                row.setModelName(info.getModelName());
                row.setModelCode(info.getModelCode());
                row.setModelType(info.getModelType());
                row.setModelIcon(info.getModelIcon());
                row.setModelDescription(info.getModelDescription());
                row.setProjectName(info.getProjectName());
                row.setLayerName(info.getLayerName());
                row.setResponsibleUserName(info.getResponsibleUserName());
                row.setResponsibleUserPhone(info.getResponsibleUserPhone());
            }
        }
        return page;
    }

    @Override
    public PageResult<SurvivorMergeRecordDO> getMergeRecords(Long ruleId, PageParam page) {
        return survivorMergeRecordMapper.selectPageByRule(ruleId, page);
    }

    @Override
    public Long createRuleSurvivor(RuleSurvivorSaveReqVO createReqVO) {
        validateResponsibleUser(createReqVO.getResponsibleUserId());
        RuleSurvivorDO ruleSurvivor = BeanUtils.toBean(createReqVO, RuleSurvivorDO.class);
        ruleSurvivorMapper.insert(ruleSurvivor);
        return ruleSurvivor.getId();
    }

    @Override
    public int updateRuleSurvivor(RuleSurvivorSaveReqVO updateReqVO) {
        // 相关校验

        // 更新生存规则
        RuleSurvivorDO updateObj = BeanUtils.toBean(updateReqVO, RuleSurvivorDO.class);
        validateResponsibleUser(updateReqVO.getResponsibleUserId());
        return ruleSurvivorMapper.update(updateObj, new LambdaUpdateWrapper<RuleSurvivorDO>()
                .eq(RuleSurvivorDO::getId, updateObj.getId())
                .set(updateReqVO.isResponsibleUserSpecified() && updateReqVO.getResponsibleUserId() == null,
                        RuleSurvivorDO::getResponsibleUserId, null));
    }
    @Override
    public int removeRuleSurvivor(Collection<Long> idList) {
        // 批量删除生存规则
        return ruleSurvivorMapper.deleteBatchIds(idList);
    }

    @Override
    public RuleSurvivorDO getRuleSurvivorById(Long id) {
        RuleSurvivorDO record = ruleSurvivorMapper.selectById(id);
        if (record != null) {
            List<RuleSurvivorDO> info = ruleSurvivorMapper.selectDisplayInfo(Collections.singletonList(id));
            if (!info.isEmpty()) {
                record.setModelName(info.get(0).getModelName());
                record.setModelCode(info.get(0).getModelCode());
                record.setModelType(info.get(0).getModelType());
                record.setModelIcon(info.get(0).getModelIcon());
                record.setProjectName(info.get(0).getProjectName());
                record.setLayerName(info.get(0).getLayerName());
                record.setResponsibleUserName(info.get(0).getResponsibleUserName());
                record.setResponsibleUserPhone(info.get(0).getResponsibleUserPhone());
            }
        }
        return record;
    }

    @Override
    public List<RuleSurvivorDO> getRuleSurvivorList() {
        return ruleSurvivorMapper.selectList();
    }

    @Override
    public Map<Long, RuleSurvivorDO> getRuleSurvivorMap() {
        List<RuleSurvivorDO> ruleSurvivorList = ruleSurvivorMapper.selectList();
        return ruleSurvivorList.stream()
                .collect(Collectors.toMap(
                        RuleSurvivorDO::getId,
                        ruleSurvivorDO -> ruleSurvivorDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入生存规则数据
         *
         * @param importExcelList 生存规则数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importRuleSurvivor(List<RuleSurvivorRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (RuleSurvivorRespVO respVO : importExcelList) {
                try {
                    RuleSurvivorDO ruleSurvivorDO = BeanUtils.toBean(respVO, RuleSurvivorDO.class);
                    Long ruleSurvivorId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ruleSurvivorId != null) {
                            RuleSurvivorDO existingRuleSurvivor = ruleSurvivorMapper.selectById(ruleSurvivorId);
                            if (existingRuleSurvivor != null) {
                                ruleSurvivorMapper.updateById(ruleSurvivorDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ruleSurvivorId + " 的生存规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ruleSurvivorId + " 的生存规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<RuleSurvivorDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ruleSurvivorId);
                        RuleSurvivorDO existingRuleSurvivor = ruleSurvivorMapper.selectOne(queryWrapper);
                        if (existingRuleSurvivor == null) {
                            ruleSurvivorMapper.insert(ruleSurvivorDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ruleSurvivorId + " 的生存规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ruleSurvivorId + " 的生存规则记录已存在。");
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
