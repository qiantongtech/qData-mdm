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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 已发布主模型的查询条件，名称等条件按成功发布时的内容判断。
 * 页码必须大于0，每页条数为-1时查询全部匹配记录，其他情况必须大于0。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelPublishedPageReqDTO extends PageParam {
    /**
     * 主模型ID，精确匹配。
     */
    private Long id;
    /**
     * 成功发布的模型编码，包含匹配。
     */
    private String code;
    /**
     * 成功发布的模型名称，包含匹配。
     */
    private String name;
    /**
     * 成功发布时的模型说明，包含匹配。
     */
    private String description;
    /**
     * 成功发布时所属主数据域。
     */
    private Long projectId;
    /**
     * 成功发布时所属目录。
     */
    private Long layerId;
    /**
     * 成功发布时的模型类型。
     */
    private String type;
    /**
     * 成功发布的版本ID，不是正在编辑的版本ID。
     */
    private Long versionId;

}
