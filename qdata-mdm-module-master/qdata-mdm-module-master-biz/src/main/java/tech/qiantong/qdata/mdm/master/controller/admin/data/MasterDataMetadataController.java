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

package tech.qiantong.qdata.mdm.master.controller.admin.data;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataMetadataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterMaintenanceFormConfigRespVO;
import tech.qiantong.qdata.mdm.master.service.data.IMasterDataMetadataService;
import tech.qiantong.qdata.mdm.master.service.data.reference.MasterReferenceService;
//import tech.qiantong.qdata.mdm.auth.api.dataauthorization.service.DataAuthorizationApi;
import tech.qiantong.qdata.mdm.common.utils.SecurityUtils;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import javax.validation.constraints.Positive;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "主数据")
@RestController
@RequestMapping("/master/data/metadata")
@RequiredArgsConstructor
@Validated
public class MasterDataMetadataController {
    private final IMasterDataMetadataService metadataService;
    private final MasterReferenceService references;
    private final ModelPublishedApi publishedModels;
//    private final DataAuthorizationApi dataAuthorization;

    /** 维护控件只读取关联模型定位信息，不要求用户拥有模型定义管理权限。 */
    @Operation(summary = "查询维护控件关联模型的已发布实例定位信息")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:add,master:instance:edit,master:instance:import,master:instance:status')")
    @GetMapping("/referenceModel/{modelId}")
    public CommonResult<Map<String, String>> referenceModel(
            @PathVariable("modelId") @Positive(message = "模型ID必须为正数") Long modelId) {
//        if (!SecurityUtils.getLoginUser().getPermissions().contains("*:*:*")
//                && !dataAuthorization.hasOperationPermission(SecurityUtils.getUserId(), modelId, "VIEW")) {
//            throw new ServiceException("无关联模型查看权限");
//        }
        ModelPublishedRespDTO model = publishedModels.getPublishedModel(modelId);
        if (model == null || model.getInstanceId() == null || model.getVersionId() == null) {
            throw new ServiceException("关联模型尚未成功发布");
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(model.getId()));
        result.put("instanceId", String.valueOf(model.getInstanceId()));
        result.put("versionId", String.valueOf(model.getVersionId()));
        result.put("type", model.getType());
        return CommonResult.success(result);
    }

    @Operation(summary = "查询主数据列表定义")
    @PreAuthorize("@ss.hasPermi('master:instance:list')")
    @GetMapping("/{instanceId}")
    public CommonResult<MasterDataMetadataRespVO> metadata(@PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId,
            @RequestParam(required = false) @Positive(message = "模型ID必须为正数") Long modelId) {
        MasterDataMetadataRespVO result = metadataService.getMetadata(instanceId, modelId);
        references.enrich(result);
        return CommonResult.success(result);
    }

    @Operation(summary = "查询主数据维护表单配置")
    @PreAuthorize("@ss.hasAnyPermi('master:instance:add,master:instance:edit,master:instance:import,master:instance:status')")
    @GetMapping("/{instanceId}/maintenance")
    public CommonResult<MasterMaintenanceFormConfigRespVO> getMaintenanceFormConfig(
            @PathVariable("instanceId") @Positive(message = "实例ID必须为正数") Long instanceId,
            @RequestParam @Positive(message = "模型ID必须为正数") Long modelId,
            @RequestParam @Positive(message = "模型版本ID必须为正数") Long modelVersionId) {
        MasterMaintenanceFormConfigRespVO result = metadataService.getMaintenanceFormConfig(instanceId, modelId, modelVersionId);
        references.enrich(result);
        return CommonResult.success(result);
    }
}
