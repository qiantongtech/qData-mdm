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

package tech.qiantong.qdata.mdm.merge.controller.admin.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewActionReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewAddReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewDetailRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.review.vo.MergeReviewSplitReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewService;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 匹配审核
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Tag(name = "匹配审核")
@RestController
@RequestMapping("/merge/review")
@Validated
public class MergeReviewController extends BaseController {
    @Resource
    private IMergeReviewService mergeReviewService;

    @Operation(summary = "查询待审核候选组")
    @PreAuthorize("@ss.hasPermi('merge:review:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<MergeGroupRespVO>> list(MergeReviewPageReqVO reqVO) {
        PageResult<MergeCandidateGroupDO> page = mergeReviewService.page(reqVO);
        PageResult<MergeGroupRespVO> result = BeanUtils.toBean(page, MergeGroupRespVO.class);
        mergeReviewService.fillSummary(result.getRows());
        return CommonResult.success(result);
    }

    @Operation(summary = "查询候选组详情")
    @PreAuthorize("@ss.hasPermi('merge:review:query')")
    @GetMapping("/{id}")
    public CommonResult<MergeReviewDetailRespVO> get(@PathVariable("id") Long id) {
        return CommonResult.success(mergeReviewService.detail(id));
    }

    @Operation(summary = "确认同一对象")
    @PreAuthorize("@ss.hasPermi('merge:review:confirm')")
    @Log(title = "匹配审核", businessType = BusinessType.UPDATE)
    @PostMapping("/confirm")
    public CommonResult<Boolean> confirm(@Valid @RequestBody MergeReviewActionReqVO reqVO) {
        mergeReviewService.confirm(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "拆分为不同对象")
    @PreAuthorize("@ss.hasPermi('merge:review:split')")
    @Log(title = "匹配审核", businessType = BusinessType.UPDATE)
    @PostMapping("/split")
    public CommonResult<Boolean> split(@Valid @RequestBody MergeReviewSplitReqVO reqVO) {
        mergeReviewService.split(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "暂缓处理")
    @PreAuthorize("@ss.hasPermi('merge:review:suspend')")
    @Log(title = "匹配审核", businessType = BusinessType.UPDATE)
    @PostMapping("/suspend")
    public CommonResult<Boolean> suspend(@Valid @RequestBody MergeReviewActionReqVO reqVO) {
        mergeReviewService.suspend(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "恢复处理")
    @PreAuthorize("@ss.hasPermi('merge:review:resume')")
    @Log(title = "匹配审核", businessType = BusinessType.UPDATE)
    @PostMapping("/resume")
    public CommonResult<Boolean> resume(@Valid @RequestBody MergeReviewActionReqVO reqVO) {
        mergeReviewService.resume(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    @Operation(summary = "补入漏匹配来源")
    @PreAuthorize("@ss.hasPermi('merge:review:add')")
    @Log(title = "匹配审核", businessType = BusinessType.UPDATE)
    @PostMapping("/add")
    public CommonResult<Boolean> add(@Valid @RequestBody MergeReviewAddReqVO reqVO) {
        mergeReviewService.add(reqVO, getUserId(), getNickName());
        return CommonResult.success(true);
    }

    /** 删除待审核或暂缓处理的候选组。 */
    @Operation(summary = "删除匹配审核候选组")
    @PreAuthorize("@ss.hasPermi('merge:review:delete')")
    @Log(title = "匹配审核", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> delete(@PathVariable("id") Long id) {
        mergeReviewService.delete(id, getUserId(), getNickName());
        return CommonResult.success(true);
    }
}
