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

package tech.qiantong.qdata.mdm.model.project.dto;

import lombok.*;

/**
 * 主数据域 DTO 对象 MODEL_PROJECT
 *
 * @author qdata
 * @date 2026-09-15
 */
@Data
public class ModelProjectReqDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 主数据域编码
     */
    private String code;

    /**
     * 主数据域名称
     */
    private String name;

    /**
     * 主数据域图标路径，未配置时前端显示默认图标。
     */
    private String icon;

    /**
     * 负责人用户ID，关联系统用户。
     */
    private Long responsibleUserId;

    /**
     * 描述
     */
    private String description;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    private Boolean delFlag;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    private Boolean validFlag;


}
