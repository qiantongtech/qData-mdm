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

package tech.qiantong.qdata.mdm.rule.service.ruleSurvivor;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.SurvivorMergeRecordDO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorRespVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorSaveReqVO;
import tech.qiantong.qdata.mdm.rule.controller.admin.ruleSurvivor.vo.RuleSurvivorPageReqVO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;
/**
 * 生存规则Service接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface IRuleSurvivorService extends IService<RuleSurvivorDO> {

    /**
     * 获得生存规则分页列表
     *
     * @param pageReqVO 分页请求
     * @return 生存规则分页列表
     */
    PageResult<RuleSurvivorDO> getRuleSurvivorPage(RuleSurvivorPageReqVO pageReqVO);

    /** 查询规则实际关联的有效合并管理记录。 */
    PageResult<SurvivorMergeRecordDO> getMergeRecords(Long ruleId, PageParam page);

    /**
     * 创建生存规则
     *
     * @param createReqVO 生存规则信息
     * @return 生存规则编号
     */
    Long createRuleSurvivor(RuleSurvivorSaveReqVO createReqVO);

    /**
     * 更新生存规则
     *
     * @param updateReqVO 生存规则信息
     */
    int updateRuleSurvivor(RuleSurvivorSaveReqVO updateReqVO);

    /**
     * 删除生存规则
     *
     * @param idList 生存规则编号
     */
    int removeRuleSurvivor(Collection<Long> idList);

    /**
     * 获得生存规则详情
     *
     * @param id 生存规则编号
     * @return 生存规则
     */
    RuleSurvivorDO getRuleSurvivorById(Long id);

    /**
     * 获得全部生存规则列表
     *
     * @return 生存规则列表
     */
    List<RuleSurvivorDO> getRuleSurvivorList();

    /**
     * 获得全部生存规则 Map
     *
     * @return 生存规则 Map
     */
    Map<Long, RuleSurvivorDO> getRuleSurvivorMap();


    /**
     * 导入生存规则数据
     *
     * @param importExcelList 生存规则数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importRuleSurvivor(List<RuleSurvivorRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
