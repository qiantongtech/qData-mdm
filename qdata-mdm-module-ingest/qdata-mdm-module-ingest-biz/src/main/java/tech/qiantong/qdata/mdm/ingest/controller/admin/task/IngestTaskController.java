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

package tech.qiantong.qdata.mdm.ingest.controller.admin.task;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestFileUploadRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskPreviewRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskRunRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskStatusReqVO;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestRunLogService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskReadService;
import tech.qiantong.qdata.mdm.ingest.service.task.IIngestTaskService;

import java.util.Arrays;
import javax.annotation.Resource;
import javax.validation.Valid;

@Tag(name = "接入任务")
@RestController
@RequestMapping("/ingest/task")
@Validated
public class IngestTaskController extends BaseController {

    @Resource
    private IIngestTaskService taskService;

    @Resource
    private IIngestTaskReadService taskReadService;

    @Resource
    private IIngestRunLogService runLogService;

    /**
     * 根据实例内序号增量读取运行日志，同时返回最新进度。
     *
     * @param id 接入任务 ID
     * @param runId 运行实例 ID，服务层校验其任务归属
     * @param afterSeq 已读取最大序号，默认从零开始
     * @return 合并 Redis 和数据库后的日志分页及游标
     */
    @Operation(summary = "增量读取运行日志和实时进度")
    @PreAuthorize("@ss.hasPermi('ingest:task:query')")
    @GetMapping("/{id}/runs/{runId}/logs")
    public CommonResult<tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestRunLogRespVO> logs(
            @PathVariable Long id, @PathVariable Long runId, @RequestParam(defaultValue = "0") long afterSeq) {
        return CommonResult.success(runLogService.read(id, runId, afterSeq));
    }

    @Operation(summary = "查询接入任务列表")
    @PreAuthorize("@ss.hasPermi('ingest:task:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<IngestTaskRespVO>> list(IngestTaskPageReqVO req) {
        return CommonResult.success(taskService.page(req));
    }

    @Operation(summary = "查询接入任务详情")
    @PreAuthorize("@ss.hasPermi('ingest:task:query')")
    @GetMapping("/{id}")
    public CommonResult<IngestTaskRespVO> detail(@PathVariable Long id) {
        return CommonResult.success(taskService.detail(id));
    }

    /**
     * 分页查询任务运行记录，并叠加 Redis 中尚未归档的进度。
     *
     * @param id 接入任务 ID
     * @param req 分页参数
     * @return 运行实例分页列表
     */
    @Operation(summary = "查询接入任务运行记录")
    @PreAuthorize("@ss.hasPermi('ingest:task:query')")
    @GetMapping("/{id}/runs")
    public CommonResult<PageResult<IngestTaskRunRespVO>> runs(@PathVariable Long id, @Valid PageParam req) {
        return CommonResult.success(taskService.runs(id, req));
    }

    /**
     * 人工提交一次接入执行，提交成功后由工作线程异步处理。
     *
     * @param id 接入任务 ID
     * @return 已受理的实例信息，最终结果通过运行记录查询
     */
    @Operation(summary = "人工执行接入任务一次")
    @PreAuthorize("@ss.hasPermi('ingest:task:edit')")
    @PostMapping("/{id}/run")
    public CommonResult<IngestTaskRunRespVO> run(@PathVariable Long id) {
        return CommonResult.success(taskService.execute(id, getNickName()));
    }

    @Operation(summary = "创建接入任务")
    @PreAuthorize("@ss.hasPermi('ingest:task:add')")
    @Log(
            title = "接入任务",
            businessType = BusinessType.INSERT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody IngestTaskSaveReqVO req) {
        req.setCreateBy(getNickName());
        return CommonResult.success(taskService.create(req));
    }

    @Operation(summary = "修改接入任务")
    @PreAuthorize("@ss.hasPermi('ingest:task:edit')")
    @Log(
            title = "接入任务",
            businessType = BusinessType.UPDATE,
            isSaveRequestData = false,
            isSaveResponseData = false)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody IngestTaskSaveReqVO req) {
        req.setUpdateBy(getNickName());
        return CommonResult.success(taskService.update(req));
    }

    @Operation(summary = "复制接入任务")
    @PreAuthorize("@ss.hasPermi('ingest:task:add')")
    @PostMapping("/{id}/copy")
    public CommonResult<Long> copy(@PathVariable Long id) {
        return CommonResult.success(taskService.copy(id, getNickName()));
    }

    @Operation(summary = "启用或停用接入任务")
    @PreAuthorize("@ss.hasPermi('ingest:task:edit')")
    @PutMapping("/status")
    public CommonResult<Integer> status(@RequestBody IngestTaskStatusReqVO req) {
        return CommonResult.success(
                taskService.changeStatus(req.getId(), req.getStatus(), getNickName()));
    }

    @Operation(summary = "启用或暂停接入任务调度")
    @PreAuthorize("@ss.hasPermi('ingest:task:edit')")
    @Log(title = "接入任务调度", businessType = BusinessType.UPDATE)
    @PutMapping("/schedule/status")
    public CommonResult<Integer> scheduleStatus(@RequestBody IngestTaskStatusReqVO req) {
        return CommonResult.success(
                taskService.changeScheduleStatus(req.getId(), req.getStatus(), getNickName()));
    }

    @Operation(summary = "删除接入任务")
    @PreAuthorize("@ss.hasPermi('ingest:task:remove')")
    @Log(title = "接入任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.success(taskService.remove(Arrays.asList(ids)));
    }


    @Operation(summary = "上传并解析任务文件")
    @PreAuthorize("@ss.hasPermi('ingest:task:add') or @ss.hasPermi('ingest:task:edit')")
    @PostMapping("/file")
    public CommonResult<IngestFileUploadRespVO> file(@RequestParam MultipartFile file) {
        return CommonResult.success(taskReadService.upload(file));
    }

    @Operation(summary = "预览来源字段及前20条数据")
    @PreAuthorize("@ss.hasPermi('ingest:task:query')")
    @PostMapping("/preview")
    public CommonResult<IngestTaskPreviewRespVO> preview(@RequestBody IngestTaskSaveReqVO req) {
        return CommonResult.success(taskReadService.preview(req));
    }
}
