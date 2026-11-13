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

package tech.qiantong.qdata.mdm.master.service.data;

import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataMetadataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceFormConfigRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterColumnRespVO;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;

import java.util.List;

/** 主数据列表和详情的模型、字段定义读取入口。 */
public interface IMasterDataMetadataService {
    /** 只读取成功发布标识和基本信息，不加载字段、物化绑定或业务数据。 */
    ModelPublishedRespDTO getPublishedModel(Long instanceId, Long modelId);

    MasterDataMetadataRespVO getMetadata(Long instanceId);
    MasterDataMetadataRespVO getMetadata(Long instanceId, Long modelId);
    MasterMaintenanceFormConfigRespVO getMaintenanceFormConfig(Long instanceId, Long modelId, Long modelVersionId);
    MasterModelRespDTO getModel(Long instanceId);
    MasterModelRespDTO getModel(Long instanceId, Long modelId);
    /** 一次读取主模型发布定义，按需绑定权威主表；instanceId为空时读取当前成功实例。 */
    MasterModelRespDTO getPublishedDefinition(Long instanceId, Long modelId, boolean includeAuthorityTable);
    /** 按同一份模型元数据生成字段定义，供列表和详情共同使用。 */
    List<MasterColumnRespVO> getColumns(MasterModelRespDTO model);
}
