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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.controller.admin.target.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;

/**
 * 分发目标系统 Response VO 对象 DIST_TARGET_SYSTEM
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
@Schema(description = "分发目标系统 Response VO")
@Data
public class DistTargetSystemRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    @Schema(description = "ID")
    private Long id;

    @Excel(name = "目标系统编码")
    @Schema(description = "目标系统编码", example = "")
    private String code;

    @Excel(name = "目标系统名称")
    @Schema(description = "目标系统名称", example = "")
    private String name;

    @Excel(name = "目标系统类型：ERP、SRM、CRM、OTHER")
    @Schema(description = "目标系统类型：ERP、SRM、CRM、OTHER", example = "")
    private String type;

    @Excel(name = "目标系统联系人姓名")
    @Schema(description = "目标系统联系人姓名", example = "")
    private String contactName;

    @Excel(name = "目标系统联系人信息")
    @Schema(description = "目标系统联系人信息", example = "")
    private String contactInfo;

    @Excel(name = "状态：1、启用、2、停用")
    @Schema(description = "状态：1、启用、2、停用", example = "")
    private String status;

    @Excel(name = "目标系统用途及业务说明")
    @Schema(description = "目标系统用途及业务说明", example = "")
    private String description;

    /** 列表按目标系统统计关联的未删除分发任务数，不是目标系统表字段。 */
    @Schema(description = "关联分发任务数量")
    private Long taskCount;

    @Excel(name = "是否有效；0：无效，1：有效")
    @Schema(description = "是否有效；0：无效，1：有效", example = "")
    private String validFlag;

    @Excel(name = "逻辑删除标志：0未删除，1已删除")
    @Schema(description = "逻辑删除标志：0未删除，1已删除", example = "")
    private String delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "负责人ID")
    @Schema(description = "创建人平台用户ID", example = "")
    private Long creatorId;

    @Schema(description = "负责人平台用户ID，存于 RESP_USER_ID")
    private Long responsibleUserId;

    /** 创建账号在平台用户资料中的电话，不使用目标系统联系人信息。 */
    @Schema(description = "创建人电话")
    private String creatorPhone;

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
