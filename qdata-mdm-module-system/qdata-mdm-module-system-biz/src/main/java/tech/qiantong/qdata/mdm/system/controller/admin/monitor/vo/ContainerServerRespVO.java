/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.qiantong.qdata.mdm.common.annotation.Excel;
import java.util.Date;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 容器管理-服务器配置 Response VO 对象 CONTAINER_SERVER
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Schema(description = "容器管理-服务器配置 Response VO")
@Data
public class ContainerServerRespVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Excel(name = "id")
    @Schema(description = "id", example = "")
    private Long id;

    @Excel(name = "服务器名称")
    @Schema(description = "服务器名称", example = "")
    private String serverName;

    @Excel(name = "服务器地址")
    @Schema(description = "服务器地址", example = "")
    private String sshHost;

    @Excel(name = "SSH端口号")
    @Schema(description = "SSH端口号", example = "")
    private Integer sshPort;

    @Excel(name = "用户名")
    @Schema(description = "用户名", example = "")
    private String userName;

    @Excel(name = "密码")
    @Schema(description = "密码", example = "")
    private String sshPassword;

    @Excel(name = "状态")
    @Schema(description = "状态", example = "")
    private String serverStatus;

    @Excel(name = "删除标志")
    @Schema(description = "删除标志", example = "")
    private Boolean delFlag;

    @Excel(name = "创建人")
    @Schema(description = "创建人", example = "")
    private String createBy;

    @Excel(name = "创建人id")
    @Schema(description = "创建人id", example = "")
    private Long creatorId;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "")
    private Date createTime;

    @Excel(name = "更新人")
    @Schema(description = "更新人", example = "")
    private String updateBy;

    @Excel(name = "更新人id")
    @Schema(description = "更新人id", example = "")
    private Long updaterId;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "")
    private Date updateTime;

    @Excel(name = "备注")
    @Schema(description = "备注", example = "")
    private String remark;

    private List<Map<String, Object>> containerList;

}
