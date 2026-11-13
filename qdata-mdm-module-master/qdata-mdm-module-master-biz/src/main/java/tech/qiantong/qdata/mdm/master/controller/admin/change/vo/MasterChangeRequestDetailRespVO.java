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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterFieldRespDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 直接维护审批申请单只读详情。
 */
@Data
public class MasterChangeRequestDetailRespVO {
    private String requestId;
    private String requestNo;
    private String requestName;
    private String modelId;
    private String modelName;
    private String instanceId;
    private String changeSource;
    private String applicantPhone;
    /** 仅返回与申请版本一致的字段配置，历史快照独立于当前正式数据。 */
    private List<MasterFieldRespDTO> fields;
    /** 字段编码 -> 关联值 -> 显示名称或分类路径，不替换申请中的原始快照。 */
    private Map<String, Map<String, String>> fieldValueLabels;
    private String changeType;
    private String approvalStatus;
    private String executeStatus;
    private String applicantName;
    private String applyReason;
    private String flowInstanceId;
    private Integer itemCount;
    private Integer successItemCount;
    private Integer failItemCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;
    private String terminateReason;
    private String executeMessage;
    private List<MasterChangeRequestItemRespVO> items;
    /** 批量导入申请对应的上传信息；历史申请无关联记录时为空。 */
    private ImportInfo importInfo;

    @Data
    public static class ImportInfo {
        private String fileName;
        private String importMode;
        private String templateVersionNo;
        private Integer totalCount;
        /** 正式导入申请在整批校验通过后才建立，校验通过数等于导入总数（包含不处理行）。 */
        private Integer passedCount;
    }
}
