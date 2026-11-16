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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.mapper.target;

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.target.DistTargetSystemDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 分发目标系统Mapper接口
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
public interface DistTargetSystemMapper extends BaseMapperX<DistTargetSystemDO> {

    /**
     * 按当前页目标系统 ID 批量查询创建账号姓名、电话。
     *
     * @param ids 当前页目标系统 ID
     * @return 创建人账号资料
     */
    List<DistTargetSystemRespVO> selectCreatorProfiles(@Param("ids") List<Long> ids);

    /**
     * 按目标系统 ID 批量查询负责人账号资料。
     *
     * @param ids 当前页目标系统 ID
     * @return 负责人姓名和电话
     */
    List<DistTargetSystemRespVO> selectResponsibleContacts(@Param("ids") List<Long> ids);

    /**
     * 按当前页目标系统 ID 汇总未删除的关联分发任务，避免逐行查询。
     *
     * @param ids 当前页目标系统 ID
     * @return 有关联任务的目标系统 ID 与任务数
     */
    List<DistTargetSystemRespVO> selectTaskCounts(@Param("ids") List<Long> ids);

    /**
     * 保存前按平台用户 ID 读取负责人，避免信任页面提交的姓名和电话。
     *
     * @param userId 负责人平台用户 ID
     * @return 有效用户的姓名和电话，不存在时返回 null
     */
    DistTargetSystemRespVO selectResponsibleUserById(@Param("userId") Long userId);

    /**
     * 根据目标系统字段构造分页条件，排序字段只允许白名单中的列。
     *
     * @param reqVO 筛选与分页参数
     * @return 目标系统分页结果
     */
    default PageResult<DistTargetSystemDO> selectPage(DistTargetSystemPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<DistTargetSystemDO>()
                .eqIfPresent(DistTargetSystemDO::getCode, reqVO.getCode())
                .likeIfPresent(DistTargetSystemDO::getName, reqVO.getName())
                .eqIfPresent(DistTargetSystemDO::getType, reqVO.getType())
                .likeIfPresent(DistTargetSystemDO::getContactName, reqVO.getContactName())
                .eqIfPresent(DistTargetSystemDO::getContactInfo, reqVO.getContactInfo())
                .eqIfPresent(DistTargetSystemDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DistTargetSystemDO::getDescription, reqVO.getDescription())
                .eqIfPresent(DistTargetSystemDO::getCreateTime, reqVO.getCreateTime())
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}
