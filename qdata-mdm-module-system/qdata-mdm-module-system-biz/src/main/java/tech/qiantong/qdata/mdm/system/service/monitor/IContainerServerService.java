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

package tech.qiantong.qdata.mdm.system.service.monitor;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerSaveReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerPageReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.monitor.ContainerServerDO;
/**
 * 容器管理-服务器配置Service接口
 *
 * @author CHAOS
 * @date 2025-09-02
 */
public interface IContainerServerService extends IService<ContainerServerDO> {

    /**
     * 获得容器管理-服务器配置分页列表
     *
     * @param pageReqVO 分页请求
     * @return 容器管理-服务器配置分页列表
     */
    PageResult<ContainerServerDO> getContainerServerPage(ContainerServerPageReqVO pageReqVO);

    /**
     * 创建容器管理-服务器配置
     *
     * @param createReqVO 容器管理-服务器配置信息
     * @return 容器管理-服务器配置编号
     */
    Long createContainerServer(ContainerServerSaveReqVO createReqVO);

    /**
     * 更新容器管理-服务器配置
     *
     * @param updateReqVO 容器管理-服务器配置信息
     */
    int updateContainerServer(ContainerServerSaveReqVO updateReqVO);

    /**
     * 删除容器管理-服务器配置
     *
     * @param idList 容器管理-服务器配置编号
     */
    int removeContainerServer(Collection<Long> idList);

    /**
     * 获得容器管理-服务器配置详情
     *
     * @param id 容器管理-服务器配置编号
     * @return 容器管理-服务器配置
     */
    ContainerServerRespVO getContainerServerById(Long id, boolean all);

    /**
     * 获得全部容器管理-服务器配置列表
     *
     * @return 容器管理-服务器配置列表
     */
    List<ContainerServerDO> getContainerServerList();

    /**
     * 获得全部容器管理-服务器配置 Map
     *
     * @return 容器管理-服务器配置 Map
     */
    Map<Long, ContainerServerDO> getContainerServerMap();

}
