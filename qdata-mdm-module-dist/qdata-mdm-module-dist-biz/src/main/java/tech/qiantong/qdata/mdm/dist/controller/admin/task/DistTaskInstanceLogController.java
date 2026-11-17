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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.task;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.utils.poi.ExcelUtil;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceLogPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskInstanceLogRespVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskInstanceLogDO;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskInstanceLogService;

/**
 * 分发任务逐条执行日志接口。
 * 提供日志分页查询和按条件导出。
 *
 * @author qdata
 * @date 2026-10-13
 */
@Tag(name = "分发任务执行日志")
@RestController
@RequestMapping("/dist/task/log")
@Validated
public class DistTaskInstanceLogController extends BaseController {
    @Resource
    private IDistTaskInstanceLogService distTaskInstanceLogService;

    /**
     * 分页查询逐条分发的执行日志。
     *
     * @param distTaskInstanceLog 查询和分页条件
     * @return 执行日志分页结果
     */
    @Operation(summary = "查询分发任务执行日志列表")
    @PreAuthorize("@ss.hasAnyPermi('dist:task:query,dist:task:log:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<DistTaskInstanceLogRespVO>> list(DistTaskInstanceLogPageReqVO distTaskInstanceLog) {
        PageResult<DistTaskInstanceLogDO> page = distTaskInstanceLogService.getDistTaskInstanceLogPage(distTaskInstanceLog);
        return CommonResult.success(BeanUtils.toBean(page, DistTaskInstanceLogRespVO.class));
    }

    /**
     * 按查询条件导出全部逐条执行日志。
     *
     * @param response HTTP 响应，用于输出 Excel 文件
     * @param exportReqVO 导出查询条件
     */
    @Operation(summary = "导出分发任务执行日志列表")
    @PreAuthorize("@ss.hasAnyPermi('dist:task:query,dist:task:log:export')")
    @Log(title = "分发任务执行日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DistTaskInstanceLogPageReqVO exportReqVO) {
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DistTaskInstanceLogDO> list = distTaskInstanceLogService.getDistTaskInstanceLogPage(exportReqVO).getRows();
        ExcelUtil<DistTaskInstanceLogRespVO> util = new ExcelUtil<>(DistTaskInstanceLogRespVO.class);
        util.exportExcel(response, BeanUtils.toBean(list, DistTaskInstanceLogRespVO.class), "分发任务执行日志");
    }

}
