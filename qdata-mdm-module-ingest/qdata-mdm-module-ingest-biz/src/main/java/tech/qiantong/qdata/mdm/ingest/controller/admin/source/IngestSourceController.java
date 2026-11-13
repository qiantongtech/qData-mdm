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

package tech.qiantong.qdata.mdm.ingest.controller.admin.source;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.service.source.IIngestSourceService;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;

/**
 * 来源系统 Controller
 *
 * @author chentangwen
 * @date 2026-09-15
 */
@Tag(name = "来源系统")
@RestController
@RequestMapping("/ingest/source")
@Validated
public class IngestSourceController extends BaseController {

    @Resource
    private IIngestSourceService ingestSourceService;

    @Operation(summary = "查询来源系统列表")
    @PreAuthorize("@ss.hasPermi('ingest:source:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<IngestSourceRespVO>> list(IngestSourcePageReqVO pageReqVO) {
        return CommonResult.success(ingestSourceService.getIngestSourcePage(pageReqVO));
    }

    @Operation(summary = "获取来源系统详细信息")
    @PreAuthorize("@ss.hasPermi('ingest:source:query')")
    @GetMapping("/{id}")
    public CommonResult<IngestSourceRespVO> getInfo(@PathVariable("id") Long id) {
        return CommonResult.success(ingestSourceService.getIngestSource(id));
    }

    @Operation(summary = "新增来源系统")
    @PreAuthorize("@ss.hasPermi('ingest:source:add')")
    @Log(title = "来源系统", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody IngestSourceSaveReqVO createReqVO) {
        createReqVO.setCreatorId(getUserId());
        createReqVO.setCreateBy(getNickName());
        createReqVO.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(ingestSourceService.createIngestSource(createReqVO));
    }

    @Operation(summary = "修改来源系统")
    @PreAuthorize("@ss.hasPermi('ingest:source:edit')")
    @Log(title = "来源系统", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody IngestSourceSaveReqVO updateReqVO) {
        updateReqVO.setUpdatorId(getUserId());
        updateReqVO.setUpdateBy(getNickName());
        updateReqVO.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(ingestSourceService.updateIngestSource(updateReqVO));
    }

    @Operation(summary = "删除来源系统")
    @PreAuthorize("@ss.hasPermi('ingest:source:remove')")
    @Log(title = "来源系统", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(ingestSourceService.removeIngestSource(Arrays.asList(ids)));
    }
}
