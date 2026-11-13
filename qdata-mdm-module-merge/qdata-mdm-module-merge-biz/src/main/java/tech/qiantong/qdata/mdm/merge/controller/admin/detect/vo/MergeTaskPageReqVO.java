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

package tech.qiantong.qdata.mdm.merge.controller.admin.detect.vo;

import lombok.Data;
import java.util.List;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;

/**
 * 重复检测任务分页条件
 *
 * @author chentangwen
 * @date 2026-09-22
 */
@Data
public class MergeTaskPageReqVO extends PageParam {
    private Long modelId;
    private List<Long> modelIds;
    private Long taskId;
    private String status;
    /** 任务编号，模糊匹配 */
    private String keyword;
    /** 任务名称，模糊匹配 */
    private String taskName;
    /** 检测范围：1 全部待治理来源数据，2 指定范围 */
    private String scopeType;
    /** 为 true 时实例列表返回规则和来源快照 */
    private Boolean includeSnapshot;
}
