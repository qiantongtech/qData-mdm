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

package tech.qiantong.qdata.mdm.dist.service.target;

import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemSaveReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.target.DistTargetSystemDO;
/**
 * 分发目标系统Service接口
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
public interface IDistTargetSystemService extends IService<DistTargetSystemDO> {

    /**
     * 获得分发目标系统分页列表
     *
     * @param pageReqVO 分页请求
     * @return 分发目标系统分页列表
     */
    PageResult<DistTargetSystemDO> getDistTargetSystemPage(DistTargetSystemPageReqVO pageReqVO);

    /**
     * 查询目标系统分页数据，并补齐创建人、负责人和关联任务数。
     *
     * @param pageReqVO 查询和分页条件
     * @return 包含页面展示信息的目标系统分页数据
     */
    PageResult<DistTargetSystemRespVO> getDistTargetSystemPageDetail(DistTargetSystemPageReqVO pageReqVO);

    /**
     * 查询目标系统详情，并补齐创建人和负责人的最新资料。
     *
     * @param id 目标系统 ID
     * @return 目标系统详情；不存在时返回 null
     */
    DistTargetSystemRespVO getDistTargetSystemDetail(Long id);

    /**
     * 创建分发目标系统
     *
     * @param createReqVO 分发目标系统信息
     * @return 分发目标系统编号
     */
    Long createDistTargetSystem(DistTargetSystemSaveReqVO createReqVO);

    /**
     * 更新分发目标系统
     *
     * @param updateReqVO 分发目标系统信息
     * @return 受影响的记录数
     */
    int updateDistTargetSystem(DistTargetSystemSaveReqVO updateReqVO);

    /**
     * 删除分发目标系统
     *
     * @param idList 分发目标系统编号
     * @return 受影响的记录数
     */
    int removeDistTargetSystem(Collection<Long> idList);

    /**
     * 获得分发目标系统详情
     *
     * @param id 分发目标系统编号
     * @return 分发目标系统
     */
    DistTargetSystemDO getDistTargetSystemById(Long id);


}
