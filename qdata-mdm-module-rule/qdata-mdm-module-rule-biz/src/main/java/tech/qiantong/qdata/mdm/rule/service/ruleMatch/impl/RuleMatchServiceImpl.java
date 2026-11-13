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

package tech.qiantong.qdata.mdm.rule.service.ruleMatch.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.aliyun.oss.ServiceException;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchPageReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchSaveReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
import tech.qiantong.qdata.mdm.rule.dal.mapper.ruleMatch.RuleMatchMapper;
import tech.qiantong.qdata.mdm.rule.service.ruleMatch.IRuleMatchService;
/**
 * 匹配规则Service业务层处理
 *
 * @author qdata
 * @date 2026-09-15
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RuleMatchServiceImpl  extends ServiceImpl<RuleMatchMapper,RuleMatchDO> implements IRuleMatchService {
    @Resource
    private RuleMatchMapper ruleMatchMapper;

    @Resource
    private ISysUserService sysUserService;

    private void validateResponsibleUser(Long userId) {
        if (userId != null && sysUserService.selectUserById(userId) == null) {
            throw new ServiceException("所选负责人不存在，请重新选择");
        }
    }

    @Override
    public PageResult<RuleMatchDO> getRuleMatchPage(RuleMatchPageReqVO pageReqVO) {
        PageResult<RuleMatchDO> page = ruleMatchMapper.selectPage(pageReqVO);
        if (page.getRows().isEmpty()) {
            return page;
        }
        Map<Long, RuleMatchDO> displayInfo = ruleMatchMapper.selectDisplayInfo(page.getRows().stream()
                .map(RuleMatchDO::getId).collect(Collectors.toList())).stream()
                .collect(Collectors.toMap(RuleMatchDO::getId, item -> item));
        for (RuleMatchDO row : page.getRows()) {
            RuleMatchDO info = displayInfo.get(row.getId());
            if (info != null) {
                row.setResponsibleUserName(info.getResponsibleUserName());
                row.setResponsibleUserPhone(info.getResponsibleUserPhone());
                row.setProjectName(info.getProjectName());
                row.setLayerName(info.getLayerName());
                row.setDuplicateTaskCount(info.getDuplicateTaskCount());
            }
        }
        return page;
    }

    @Override
    public Long createRuleMatch(RuleMatchSaveReqVO createReqVO) {
        validateResponsibleUser(createReqVO.getResponsibleUserId());
        RuleMatchDO dictType = BeanUtils.toBean(createReqVO, RuleMatchDO.class);
        ruleMatchMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateRuleMatch(RuleMatchSaveReqVO updateReqVO) {
        // 相关校验

        // 更新匹配规则
        RuleMatchDO updateObj = BeanUtils.toBean(updateReqVO, RuleMatchDO.class);
        validateResponsibleUser(updateReqVO.getResponsibleUserId());
        // 显式清空负责人时写入 NULL；仅切换状态时保留原负责人。
        return ruleMatchMapper.update(updateObj,
                new LambdaUpdateWrapper<RuleMatchDO>()
                        .eq(RuleMatchDO::getId, updateObj.getId())
                        .set(updateReqVO.isResponsibleUserSpecified() && updateReqVO.getResponsibleUserId() == null,
                                RuleMatchDO::getResponsibleUserId, null));
    }
    @Override
    public int removeRuleMatch(Collection<Long> idList) {
        // 批量删除匹配规则
        return ruleMatchMapper.deleteBatchIds(idList);
    }

    @Override
    public RuleMatchDO getRuleMatchById(Long id) {
        RuleMatchDO record = ruleMatchMapper.selectById(id);
        if (record != null) {
            List<RuleMatchDO> displayInfo = ruleMatchMapper.selectDisplayInfo(Collections.singletonList(id));
            if (!displayInfo.isEmpty()) {
                RuleMatchDO info = displayInfo.get(0);
                record.setProjectName(info.getProjectName());
                record.setLayerName(info.getLayerName());
                record.setResponsibleUserPhone(info.getResponsibleUserPhone());
            }
        }
        if (record != null && record.getResponsibleUserId() != null) {
            SysUser user =
                    sysUserService.selectUserById(record.getResponsibleUserId());
            if (user != null) {
                record.setResponsibleUserName(StringUtils.isNotEmpty(user.getNickName())
                        ? user.getNickName() : user.getUserName());
            }
        }
        return record;
    }

    @Override
    public List<RuleMatchDO> getRuleMatchList() {
        return ruleMatchMapper.selectList();
    }

    @Override
    public Map<Long, RuleMatchDO> getRuleMatchMap() {
        List<RuleMatchDO> ruleMatchList = ruleMatchMapper.selectList();
        return ruleMatchList.stream()
                .collect(Collectors.toMap(
                        RuleMatchDO::getId,
                        ruleMatchDO -> ruleMatchDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


        /**
         * 导入匹配规则数据
         *
         * @param importExcelList 匹配规则数据列表
         * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
         * @param operName 操作用户
         * @return 结果
         */
        @Override
        public String importRuleMatch(List<RuleMatchRespVO> importExcelList, boolean isUpdateSupport, String operName) {
            if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
                throw new ServiceException("导入数据不能为空！");
            }

            int successNum = 0;
            int failureNum = 0;
            List<String> successMessages = new ArrayList<>();
            List<String> failureMessages = new ArrayList<>();

            for (RuleMatchRespVO respVO : importExcelList) {
                try {
                    RuleMatchDO ruleMatchDO = BeanUtils.toBean(respVO, RuleMatchDO.class);
                    Long ruleMatchId = respVO.getId();
                    if (isUpdateSupport) {
                        if (ruleMatchId != null) {
                            RuleMatchDO existingRuleMatch = ruleMatchMapper.selectById(ruleMatchId);
                            if (existingRuleMatch != null) {
                                ruleMatchMapper.updateById(ruleMatchDO);
                                successNum++;
                                successMessages.add("数据更新成功，ID为 " + ruleMatchId + " 的匹配规则记录。");
                            } else {
                                failureNum++;
                                failureMessages.add("数据更新失败，ID为 " + ruleMatchId + " 的匹配规则记录不存在。");
                            }
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，某条记录的ID不存在。");
                        }
                    } else {
                        QueryWrapper<RuleMatchDO> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("id", ruleMatchId);
                        RuleMatchDO existingRuleMatch = ruleMatchMapper.selectOne(queryWrapper);
                        if (existingRuleMatch == null) {
                            ruleMatchMapper.insert(ruleMatchDO);
                            successNum++;
                            successMessages.add("数据插入成功，ID为 " + ruleMatchId + " 的匹配规则记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据插入失败，ID为 " + ruleMatchId + " 的匹配规则记录已存在。");
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
