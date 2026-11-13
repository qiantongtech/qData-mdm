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

package tech.qiantong.qdata.mdm.master.data.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;

/**
 * 已发布模型的权威主数据增量同步条件，不分页，模型和版本按成功发布内容判断。
 */
@Data
public class MasterDataSyncReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 已发布主模型ID，普通模型和分类模型均可。
     */
    @NotNull(message = "模型ID不能为空")
    @Positive(message = "模型ID必须为正数")
    private Long modelId;

    /**
     * 成功发布的模型版本ID，对应ModelPublishedRespDTO.versionId，不是数据历史版本ID。
     */
    @NotNull(message = "模型发布版本ID不能为空")
    @Positive(message = "模型发布版本ID必须为正数")
    private Long modelVersionId;

    /**
     * 同步时间下界，严格返回更新时间大于此时间的记录，不包含相等时间。
     * 更新时间为空时使用创建时间；Java调用传入Date，保留毫秒精度。
     */
    @NotNull(message = "同步时间节点不能为空")
    private Date updatedAfter;

    /**
     * 可选的生命周期状态：1生效、2冻结、3失效、4归档；不传或空字符串时返回全部状态。
     * 所有状态均不包含逻辑删除记录。
     */
    @Pattern(regexp = "^$|[1-4]", message = "主数据状态无效")
    private String status;
}
