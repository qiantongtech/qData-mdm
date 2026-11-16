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

package tech.qiantong.qdata.mdm.dist.service.task;

import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskSaveReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskPageReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
/**
 * 分发任务业务接口。
 * 提供任务查询、配置保存、克隆和删除能力。
 *
 * @author qdata
 * @date 2026-10-13
 */
public interface IDistTaskService extends IService<DistTaskDO> {

    /**
     * 获得分发任务分页列表
     *
     * @param pageReqVO 分页请求
     * @return 分发任务分页列表
     */
    PageResult<DistTaskDO> getDistTaskPage(DistTaskPageReqVO pageReqVO);

    /**
     * 查询分发任务分页数据，并补充列表展示信息，隐藏认证配置。
     *
     * @param pageReqVO 查询和分页条件
     * @return 分发任务分页结果
     */
    PageResult<DistTaskRespVO> getDistTaskPageDetail(DistTaskPageReqVO pageReqVO);

    /**
     * 查询分发任务详情，按需加载筛选条件和字段映射，并遮蔽认证配置。
     *
     * @param id 分发任务 ID
     * @param includeFilters 是否加载筛选条件
     * @param includeFieldMappings 是否加载字段映射
     * @return 分发任务详情；不存在时返回 null
     */
    DistTaskRespVO getDistTaskDetail(Long id, boolean includeFilters, boolean includeFieldMappings);

    /**
     * 创建分发任务
     *
     * @param createReqVO 分发任务信息
     * @return 分发任务编号
     */
    Long createDistTask(DistTaskSaveReqVO createReqVO);

    /**
     * 克隆任务定义及其筛选条件、字段映射，并重置运行状态。
     *
     * @param sourceId 原任务 ID
     * @param operatorId 当前操作人 ID
     * @param operatorName 当前操作人姓名
     * @return 新任务 ID
     */
    Long cloneDistTask(Long sourceId, Long operatorId, String operatorName);

    /**
     * 更新分发任务
     *
     * @param updateReqVO 分发任务信息
     * @return 受影响的记录数
     */
    int updateDistTask(DistTaskSaveReqVO updateReqVO);

    /**
     * 删除分发任务
     *
     * @param idList 分发任务编号
     * @return 受影响的记录数
     */
    int removeDistTask(Collection<Long> idList);

    /**
     * 获得分发任务详情
     *
     * @param id 分发任务编号
     * @return 分发任务
     */
    DistTaskDO getDistTaskById(Long id);


}
