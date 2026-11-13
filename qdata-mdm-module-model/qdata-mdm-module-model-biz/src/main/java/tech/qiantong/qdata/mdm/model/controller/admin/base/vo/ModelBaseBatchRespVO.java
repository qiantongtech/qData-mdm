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

package tech.qiantong.qdata.mdm.model.controller.admin.base.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次批量请求的逐项结果，部分失败不会把已成功的模型一起回滚。
 */
@Data
public class ModelBaseBatchRespVO {
    private int successCount;
    private int failureCount;
    private List<Item> results = new ArrayList<>();

    @Data
    public static class Item {
        private Long modelId;
        private String modelName;
        private boolean success;
        private String message;
        private ModelBasePublishRespVO publishResult;
    }
}
