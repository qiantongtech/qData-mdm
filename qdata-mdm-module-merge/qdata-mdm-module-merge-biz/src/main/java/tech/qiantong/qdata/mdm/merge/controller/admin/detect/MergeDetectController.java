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

package tech.qiantong.qdata.mdm.merge.controller.admin.detect;

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
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectExecuteReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectScopeRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeDetectStartReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeGroupTransferReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeInstanceRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskPageReqVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskRespVO;
import tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo.MergeTaskStatusReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeDuplicateTaskInstanceDO;
import tech.qiantong.qdata.mdm.merge.service.detect.IMergeDetectService;
import tech.qiantong.qdata.mdm.merge.service.review.IMergeReviewService;

import java.util.Arrays;
import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 重复检测
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Tag(name = "重复检测")
@RestController
@RequestMapping("/merge/detect")
@Validated
public class MergeDetectController extends BaseController {
    @Resource
    private IMergeDetectService mergeDetectService;

    @Resource
    private IMergeReviewService mergeReviewService;

    /** 查询任务列表，补充执行计数、规则数量和范围描述。 */
    @Operation(summary = "查询重复检测任务")
    @PreAuthorize("@ss.hasPermi('merge:detect:list')")
    @GetMapping("/task/list")
    public CommonResult<PageResult<MergeTaskRespVO>> tasks(MergeTaskPageReqVO reqVO) {
        PageResult<MergeDuplicateTaskDO> page = mergeDetectService.pageTasks(reqVO);
        PageResult<MergeTaskRespVO> result = BeanUtils.toBean(page, MergeTaskRespVO.class);
        mergeDetectService.fillTaskMetrics(result.getRows());
        return CommonResult.success(result);
    }

    /** 查询运行实例，按请求开关回显固定快照。 */
    @Operation(summary = "查询重复检测实例")
    @PreAuthorize("@ss.hasPermi('merge:detect:query')")
    @GetMapping("/instance/list")
    public CommonResult<PageResult<MergeInstanceRespVO>> instances(MergeTaskPageReqVO reqVO) {
        PageResult<MergeDuplicateTaskInstanceDO> page = mergeDetectService.pageInstances(reqVO);
        return CommonResult.success(BeanUtils.toBean(page, MergeInstanceRespVO.class));
    }

    /** 查询候选组并补充审核摘要。 */
    @Operation(summary = "查询重复检测候选组")
    @PreAuthorize("@ss.hasPermi('merge:detect:query')")
    @GetMapping("/group/list")
    public CommonResult<PageResult<MergeGroupRespVO>> groups(MergeGroupPageReqVO reqVO) {
        PageResult<MergeCandidateGroupDO> page = mergeDetectService.pageGroups(reqVO);
        PageResult<MergeGroupRespVO> result = BeanUtils.toBean(page, MergeGroupRespVO.class);
        mergeReviewService.fillSummary(result.getRows());
        return CommonResult.success(result);
    }

    /** 预览可检测来源与范围，不改变任务或治理状态。 */
    @Operation(summary = "预览重复检测范围")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @PostMapping("/scope")
    public CommonResult<MergeDetectScopeRespVO> scope(@Valid @RequestBody MergeDetectStartReqVO reqVO) {
        return CommonResult.success(mergeDetectService.previewScope(reqVO));
    }

    /** 保存检测任务配置，不立即执行。 */
    @Operation(summary = "保存重复检测任务")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.INSERT)
    @PostMapping("/task/save")
    public CommonResult<Long> save(@Valid @RequestBody MergeDetectStartReqVO reqVO) {
        return CommonResult.success(mergeDetectService.save(reqVO, getUserId(), getNickName()));
    }

    /** 修改非运行中任务的范围配置。 */
    @Operation(summary = "修改重复检测任务")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.UPDATE)
    @PutMapping("/task/{taskId}")
    public CommonResult<Integer> update(@PathVariable Long taskId,
                                        @Valid @RequestBody MergeDetectStartReqVO reqVO) {
        return CommonResult.toAjax(mergeDetectService.updateTask(taskId, reqVO, getUserId(), getNickName()));
    }

    /** 更新任务启停开关。 */
    @Operation(summary = "修改重复检测任务启停状态")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测任务状态", businessType = BusinessType.UPDATE)
    @PutMapping("/task/status")
    public CommonResult<Integer> changeStatus(@Valid @RequestBody MergeTaskStatusReqVO reqVO) {
        return CommonResult.toAjax(mergeDetectService.changeStatus(
                reqVO.getId(), reqVO.getStatus(), getUserId(), getNickName()));
    }

    /** 独立更新定时调度开关。 */
    @Operation(summary = "修改重复检测任务调度状态")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测调度状态", businessType = BusinessType.UPDATE)
    @PutMapping("/task/schedule-status")
    public CommonResult<Integer> changeScheduleStatus(@Valid @RequestBody MergeTaskStatusReqVO reqVO) {
        return CommonResult.toAjax(mergeDetectService.changeScheduleStatus(
                reqVO.getId(), reqVO.getStatus(), getUserId(), getNickName()));
    }

    /** 按编号删除非运行中的任务。 */
    @Operation(summary = "删除重复检测任务")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.DELETE)
    @DeleteMapping("/task/{taskIds}")
    public CommonResult<Integer> delete(@PathVariable Long[] taskIds) {
        return CommonResult.toAjax(mergeDetectService.deleteTasks(Arrays.asList(taskIds)));
    }

    /** 创建任务并立即生成检测实例。 */
    @Operation(summary = "发起重复检测")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.INSERT)
    @PostMapping("/start")
    public CommonResult<Long> start(@Valid @RequestBody MergeDetectStartReqVO reqVO) {
        return CommonResult.success(mergeDetectService.start(reqVO, getUserId(), getNickName()));
    }

    /** 执行已保存任务。 */
    @Operation(summary = "执行已保存的重复检测任务")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.UPDATE)
    @PostMapping("/task/execute")
    public CommonResult<Long> execute(@Valid @RequestBody MergeDetectExecuteReqVO reqVO) {
        return CommonResult.success(mergeDetectService.execute(reqVO.getTaskId(), getUserId(), getNickName()));
    }

    /** 使未进入结果处理的旧候选组失效后重新检测。 */
    @Operation(summary = "重新检测仍停留在审核阶段的结果")
    @PreAuthorize("@ss.hasPermi('merge:detect:redetect')")
    @Log(title = "重复检测", businessType = BusinessType.UPDATE)
    @PostMapping("/redetect")
    public CommonResult<Long> redetect(@Valid @RequestBody MergeDetectStartReqVO reqVO) {
        return CommonResult.success(mergeDetectService.redetect(reqVO.getModelId(), getUserId(), getNickName()));
    }

    /** 将候选组转入审核或单条结果处理。 */
    @Operation(summary = "将待转入候选对象组转入后续处理")
    @PreAuthorize("@ss.hasPermi('merge:detect:execute')")
    @Log(title = "重复检测", businessType = BusinessType.UPDATE)
    @PostMapping("/group/transfer")
    public CommonResult<Integer> transfer(@Valid @RequestBody MergeGroupTransferReqVO reqVO) {
        return CommonResult.success(mergeDetectService.transfer(reqVO.getGroupIds(), getUserId(), getNickName()));
    }
}
