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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import lombok.Data;
import java.util.Map;

/**
 * 前端动态列表及详情共用的字段定义。
 */
@Data
public class MasterColumnRespVO {
    private String prop;
    /** 成功发布配置中的字段名称，用于表头；无配置名称时使用物化列名。 */
    private String label;
    /** 成功物化字段的完整注释。 */
    private String comment;
    /** 字段显示顺序，从 1 开始。columns 已按此顺序排列。 */
    private Integer sortOrder;
    private String dataType;
    private String dict;
    /** 当前字段是否支持服务端排序。 */
    private Boolean sortable;
    /** 来自模型配置的业务字段，供维护申请表单识别；系统列不允许作为业务字段编辑。 */
    private Boolean businessField;
    /** 当前模型的名称字段，前端申请表单据此进行必填校验。 */
    private Boolean nameField;
    private Boolean required;
    private Boolean unique;
    private Long dataLength;
    private Long dataScale;
    private String defaultValue;
    /** 引用值编码到名称的映射，列表与导入预览共用。 */
    private Map<String, String> valueLabels;
}
