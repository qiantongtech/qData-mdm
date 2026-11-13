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

package tech.qiantong.qdata.mdm.model.enums.materialization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 现有MODEL_VERSION.STATUS业务状态；只判断当前版本，不因历史版本封存而停用当前模型。
 */
@Getter
@RequiredArgsConstructor
public enum ModelVersionStatus {
    /**
     * 未发布草稿。
     */
    DRAFT(0L),
    /**
     * 已发布。
     */
    PUBLISHED(1L),
    /**
     * 修订中，其他模块仍读取上次成功发布。
     */
    REVISING(2L),
    /**
     * 封存，即当前模型停用。
     */
    SEALED(3L),
    /**
     * 业务逻辑删除，物理表和历史数据保留。
     */
    DELETED(4L);

    /**
     * 对应现有数据库数字状态，不使用枚举序号。
     */
    private final Long status;

    /**
     * 将现有数字状态转换为发布领域使用的业务版本状态。
     *
     * @param status MODEL_VERSION.STATUS值
     * @return 对应模型版本状态
     * @throws IllegalArgumentException 状态为空或不是已定义数字时抛出
     */
    public static ModelVersionStatus fromStatus(Long status) {
        for (ModelVersionStatus value : values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("未识别的模型版本状态：" + status);
    }
}
