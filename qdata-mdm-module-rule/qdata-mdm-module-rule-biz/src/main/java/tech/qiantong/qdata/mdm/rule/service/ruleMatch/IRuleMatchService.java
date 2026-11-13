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

package tech.qiantong.qdata.mdm.rule.service.ruleMatch;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchSaveReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleMatch.vo.RuleMatchPageReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleMatch.RuleMatchDO;
/**
 * 匹配规则Service接口
 *
 * @author qdata
 * @date 2026-09-15
 */
public interface IRuleMatchService extends IService<RuleMatchDO> {

    /**
     * 获得匹配规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 匹配规则分页列表
     */
    PageResult<RuleMatchDO> getRuleMatchPage(RuleMatchPageReqVO pageReqVO);

    /**
     * 创建匹配规则
     *
     * @param createReqVO 匹配规则信息
     * @return 匹配规则编号
     */
    Long createRuleMatch(RuleMatchSaveReqVO createReqVO);

    /**
     * 更新匹配规则
     *
     * @param updateReqVO 匹配规则信息
     */
    int updateRuleMatch(RuleMatchSaveReqVO updateReqVO);

    /**
     * 删除匹配规则
     *
     * @param idList 匹配规则编号
     */
    int removeRuleMatch(Collection<Long> idList);

    /**
     * 获得匹配规则详情
     *
     * @param id 匹配规则编号
     * @return 匹配规则
     */
    RuleMatchDO getRuleMatchById(Long id);

    /**
     * 获得全部匹配规则列表
     *
     * @return 匹配规则列表
     */
    List<RuleMatchDO> getRuleMatchList();

    /**
     * 获得全部匹配规则 Map
     *
     * @return 匹配规则 Map
     */
    Map<Long, RuleMatchDO> getRuleMatchMap();


    /**
     * 导入匹配规则数据
     *
     * @param importExcelList 匹配规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importRuleMatch(List<RuleMatchRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
