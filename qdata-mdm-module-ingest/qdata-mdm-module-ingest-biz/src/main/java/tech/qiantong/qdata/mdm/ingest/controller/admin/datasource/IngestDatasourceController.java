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

package tech.qiantong.qdata.mdm.ingest.controller.admin.datasource;

import cn.hutool.core.date.DateUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.qiantong.qdata.mdm.common.annotation.Log;
import tech.qiantong.qdata.mdm.common.core.controller.BaseController;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.domain.CommonResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.common.enums.BusinessType;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSimpleVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceService;

import javax.validation.Valid;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisRespVO;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceDiagnosisService;
import java.util.Arrays;
import java.util.List;

/**
 * 数据源Controller
 *
 * @author lhs
 * @date 2025-01-21
 */
@Tag(name = "数据源")
@RestController
@RequestMapping("/ingest/dataSource")
@Validated
@RequiredArgsConstructor
public class IngestDatasourceController extends BaseController {

    private final IIngestDatasourceDiagnosisService daDatasourceDiagnosisService;
    private final IIngestDatasourceService daDatasourceService;

    @Operation(summary = "查询数据源列表")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:list')")
    @GetMapping("/list")
    public CommonResult<PageResult<IngestDatasourceRespVO>> list(IngestDatasourcePageReqVO daDatasource) {
        PageResult<IngestDatasourceDO> page = daDatasourceService.getDaDatasourcePage(daDatasource);
        return CommonResult.success(BeanUtils.toBean(page, IngestDatasourceRespVO.class));
    }




    @Operation(summary = "查询数据源列表")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:list')")
    @GetMapping("/getDaDatasourceList")
    public CommonResult<List<IngestDatasourceRespVO>> getDaDatasourceList(IngestDatasourcePageReqVO daDatasource) {
        List<IngestDatasourceDO> page = daDatasourceService.getDaDatasourceList(daDatasource);
        return CommonResult.success(BeanUtils.toBean(page, IngestDatasourceRespVO.class));
    }


    @Operation(summary = "查询数据连接下拉列表")
    @PreAuthorize("@ss.hasPermi('ingest:source:list')")
    @GetMapping("/options")
    public CommonResult<List<IngestDatasourceSimpleVO>> options(@RequestParam(value = "keyword", required = false) String keyword) {
        return CommonResult.success(daDatasourceService.getDatasourceOptions(keyword));
    }

    @Operation(summary = "获取数据源详细信息")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:query')")
    @GetMapping(value = "/{id}")
    public CommonResult<IngestDatasourceRespVO> getInfo(@PathVariable("id") Long id) {
        IngestDatasourceDO daDatasourceDO = daDatasourceService.getDaDatasourceById(id);
        return CommonResult.success(BeanUtils.toBean(daDatasourceDO, IngestDatasourceRespVO.class));
    }

    @Operation(summary = "新增数据源")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:add')")
    @Log(title = "数据源", businessType = BusinessType.INSERT)
    @PostMapping
    public CommonResult<Long> add(@Valid @RequestBody IngestDatasourceSaveReqVO daDatasource) {
        daDatasource.setCreatorId(getUserId());
        daDatasource.setCreateBy(getNickName());
        daDatasource.setCreateTime(DateUtil.date());
        return CommonResult.toAjax(daDatasourceService.createDaDatasource(daDatasource));
    }

    @Operation(summary = "修改数据源")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:edit')")
    @Log(title = "数据源", businessType = BusinessType.UPDATE)
    @PutMapping
    public CommonResult<Integer> edit(@Valid @RequestBody IngestDatasourceSaveReqVO daDatasource) {
        daDatasource.setUpdatorId(getUserId());
        daDatasource.setUpdateBy(getNickName());
        daDatasource.setUpdateTime(DateUtil.date());
        return CommonResult.toAjax(daDatasourceService.updateDaDatasource(daDatasource));
    }

    @Operation(summary = "删除数据源")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:remove')")
    @Log(title = "数据源", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public CommonResult<Integer> remove(@PathVariable Long[] ids) {
        return CommonResult.toAjax(daDatasourceService.removeDaDatasource(Arrays.asList(ids)));
    }

    @Operation(summary = "修改数据源状态")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:edit')")
    @GetMapping(value = "/editDatasourceStatus/{id}/{status}")
    public AjaxResult editDatasourceStatus(@PathVariable Long id, @PathVariable Long status) {
        Boolean isOk = daDatasourceService.editDatasourceStatus(id, status);
        if (!isOk) {
            return AjaxResult.error("任务状态修改失败，请联系系统管理员");
        }
        return AjaxResult.success("修改成功");
    }


    @Operation(summary = "测试连接")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:remove')")
    @Log(title = "测试连接", businessType = BusinessType.DELETE)
    @GetMapping("/clientsTest/{id}")
    public AjaxResult clientsTest(@PathVariable("id") Long ids) {
        return daDatasourceService.clientsTest(ids);
    }


    @Operation(summary = "测试连接")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:remove')")
    @Log(title = "测试连接", businessType = BusinessType.DELETE)
    @PostMapping("/clientsTestStatus")
    public AjaxResult clientsTest(@Valid @RequestBody IngestDatasourceSaveReqVO daDatasource) {
        return daDatasourceService.clientsTest(daDatasource);
    }

    @Operation(summary = "获取数据源里面的数据表")
    @PreAuthorize("@ss.hasPermi('ingest:task:query') or @ss.hasPermi('ingest:task:add') or @ss.hasPermi('ingest:task:edit')")
    @GetMapping("/tableList/{id}")
    public CommonResult<List<DbTable>> getTableList(@PathVariable("id") Long id,
                                                    @RequestParam("sourceId") Long sourceId,
                                                    @RequestParam(required = false) String tableName,
                                                    @RequestParam(defaultValue = "-1") Long limit) {
        return CommonResult.success(daDatasourceService.getDbTables(sourceId, id, tableName, limit));
    }



    @Operation(summary = "数据源测试诊断")
    @PreAuthorize("@ss.hasPermi('ingest:datasource:edit')")
    @Log(title = "数据源测试诊断", businessType = BusinessType.OTHER,
            isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/diagnose")
    public CommonResult<IngestDatasourceDiagnosisRespVO> diagnose(
            @RequestBody IngestDatasourceDiagnosisReqVO reqVO) {
        return CommonResult.success(daDatasourceDiagnosisService.diagnose(reqVO));
    }
}
