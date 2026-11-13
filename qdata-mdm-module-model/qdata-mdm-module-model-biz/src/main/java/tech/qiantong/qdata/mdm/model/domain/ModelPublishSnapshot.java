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

package tech.qiantong.qdata.mdm.model.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.fastjson2.JSONObject;

/**
 * MODEL_SNAPSHOT的完整配置载体；读取后独立复制，执行及恢复不重新读取可编辑草稿。
 */
@Data
@NoArgsConstructor
public class ModelPublishSnapshot {

    /**
     * JSON结构格式版本，不是模型业务版本
     */
    private Integer formatVersion = 1;

    /**
     * 本次整体发布的根模型ID
     */
    private Long modelId;

    /**
     * 本次根工作版本ID
     */
    private Long modelVersionId;

    /**
     * 完整根模型配置，含字段、子模型、各自版本状态、DEL_FLAG及展示配置
     */
    private JSONObject model = new JSONObject();

    /**
     * 字段关联的参考数据完整信息，按稳定ID组织
     */
    private JSONObject referenceData = new JSONObject();

    /**
     * 编码方案版本及完整配置，按稳定ID组织
     */
    private JSONObject encodingSchemes = new JSONObject();

    /**
     * 模型关联规则及版本配置，按稳定ID组织
     */
    private JSONObject rules = new JSONObject();
}

