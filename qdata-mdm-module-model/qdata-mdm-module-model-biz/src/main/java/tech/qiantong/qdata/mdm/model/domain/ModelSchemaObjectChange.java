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
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSchemaChangeKind;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 说明某张表、某一列或某个索引改了什么；明确停用、逻辑删除和莫名缺失要分别记录。
 */
@Data
@NoArgsConstructor
public class ModelSchemaObjectChange {

    /**
     * 稳定对象定位
     */
    private ModelSchemaObjectRef object;

    /**
     * 新增、属性变化、元数据、停用、逻辑删除或意外缺失
     */
    private ModelSchemaChangeKind changeKind;

    /**
     * 变化前完整定义；新增对象为空
     */
    private JSONObject beforeDefinition;

    /**
     * 变化后完整定义；意外缺失为空，不能自动授权删除
     */
    private JSONObject afterDefinition;

    /**
     * 逐属性变化，包含名称、约束、长度等全部差异
     */
    private List<ModelSchemaPropertyChange> properties = new ArrayList<>();

    /**
     * 是否需要DDL；仅是比较结果，不替代变更校验/目标库校验（CHANGE/TARGET）批准
     */
    private Boolean ddlRequired = Boolean.FALSE;
}

