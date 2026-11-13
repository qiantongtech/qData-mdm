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

package tech.qiantong.qdata.mdm.quality.controller.admin.qualityTaskInstance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.annotation.Excel;

import java.io.Serializable;
import java.util.Date;

/**
 * 主数据质量任务执行实例 Response VO 对象 QUALITY_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-09-22
 */
@Schema(description = "主数据质量任务执行实例 Response VO")
@Data
public class QualityTaskInstanceRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "质量任务ID")
    @Schema(description = "质量任务ID", example = "")
    private Long qualityTaskId;

    @Excel(name = "质量任务编码")
    @Schema(description = "质量任务编码", example = "")
    private String taskCode;

    @Excel(name = "主数据模型ID")
    @Schema(description = "主数据模型ID", example = "")
    private Long modelId;

    @Excel(name = "主数据模型版本")
    @Schema(description = "主数据模型版本", example = "")
    private String modelVersion;

    @Excel(name = "实例编号")
    @Schema(description = "实例编号", example = "")
    private String executionNo;

    @Excel(name = "负责人ID")
    @Schema(description = "负责人ID", example = "")
    private String personCharge;

    @Excel(name = "负责人电话")
    @Schema(description = "负责人电话", example = "")
    private String contactNumber;

    @Excel(name = "触发方式：1人工执行、2定时执行")
    @Schema(description = "触发方式：1人工执行、2定时执行", example = "")
    private String triggerType;

    @Excel(name = "执行结果：1执行中、2失败、3部分成功、4成功")
    @Schema(description = "执行结果：1执行中、2失败、3部分成功、4成功", example = "")
    private String status;

    @Excel(name = "本次执行开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "本次执行开始时间", example = "")
    private Date startTime;

    @Excel(name = "本次执行结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "本次执行结束时间", example = "")
    private Date endTime;

    @Excel(name = "检查数据记录数")
    @Schema(description = "检查数据记录数", example = "")
    private Long totalCount;

    @Excel(name = "通过数据记录数")
    @Schema(description = "通过数据记录数", example = "")
    private Long passCount;

    @Excel(name = "问题数据记录数")
    @Schema(description = "问题数据记录数", example = "")
    private Long failCount;

    @Excel(name = "执行失败说明")
    @Schema(description = "执行失败说明", example = "")
    private String errorMessage;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private Boolean validFlag;

    @Excel(name = "删除标志：0未删除，1已删除")
    @Schema(description = "删除标志：0未删除，1已删除", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人ID")
    @Schema(description = "创建人ID", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "最后更新人ID")
    @Schema(description = "最后更新人ID", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

}
