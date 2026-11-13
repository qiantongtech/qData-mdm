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

package tech.qiantong.qdata.mdm.rule.service.ruleQuality;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.QualityMergePendingDO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualitySaveReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleQuality.vo.RuleQualityPageReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleQuality.RuleQualityDO;
/**
 * 质量规则Service接口
 *
 * @author qdata
 * @date 2026-09-18
 */
public interface IRuleQualityService extends IService<RuleQualityDO> {

    /**
     * 获得质量规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 质量规则分页列表
     */
    PageResult<RuleQualityDO> getRuleQualityPage(RuleQualityPageReqVO pageReqVO);

    PageResult<QualityMergePendingDO> getMergeRecords(Long ruleId, PageParam page);

    /**
     * 创建质量规则
     *
     * @param createReqVO 质量规则信息
     * @return 质量规则编号
     */
    Long createRuleQuality(RuleQualitySaveReqVO createReqVO);

    /**
     * 更新质量规则
     *
     * @param updateReqVO 质量规则信息
     */
    int updateRuleQuality(RuleQualitySaveReqVO updateReqVO);

    /**
     * 删除质量规则
     *
     * @param idList 质量规则编号
     */
    int removeRuleQuality(Collection<Long> idList);

    /**
     * 获得质量规则详情
     *
     * @param id 质量规则编号
     * @return 质量规则
     */
    RuleQualityDO getRuleQualityById(Long id);

    /**
     * 获得全部质量规则列表
     *
     * @return 质量规则列表
     */
    List<RuleQualityDO> getRuleQualityList();

    /**
     * 获得全部质量规则 Map
     *
     * @return 质量规则 Map
     */
    Map<Long, RuleQualityDO> getRuleQualityMap();


    /**
     * 导入质量规则数据
     *
     * @param importExcelList 质量规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importRuleQuality(List<RuleQualityRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
