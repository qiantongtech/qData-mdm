/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.convert.monitor;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerSaveReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.monitor.ContainerServerDO;

/**
 * 容器管理-服务器配置 Convert
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Mapper
public interface ContainerServerConvert {
    ContainerServerConvert INSTANCE = Mappers.getMapper(ContainerServerConvert.class);

    /**
     * PageReqVO 转换为 DO
     * @param containerServerPageReqVO 请求参数
     * @return ContainerServerDO
     */
     ContainerServerDO convertToDO(ContainerServerPageReqVO containerServerPageReqVO);

    /**
     * SaveReqVO 转换为 DO
     * @param containerServerSaveReqVO 保存请求参数
     * @return ContainerServerDO
     */
     ContainerServerDO convertToDO(ContainerServerSaveReqVO containerServerSaveReqVO);

    /**
     * DO 转换为 RespVO
     * @param containerServerDO 实体对象
     * @return ContainerServerRespVO
     */
     ContainerServerRespVO convertToRespVO(ContainerServerDO containerServerDO);

    /**
     * DOList 转换为 RespVOList
     * @param containerServerDOList 实体对象列表
     * @return List<ContainerServerRespVO>
     */
     List<ContainerServerRespVO> convertToRespVOList(List<ContainerServerDO> containerServerDOList);
}
