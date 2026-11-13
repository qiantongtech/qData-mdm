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

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * 描述需要common生成什么DDL；这是生成SQL之前的动作清单，不是可执行SQL，也不对应独立数据库表。
 */
@Data
@NoArgsConstructor
public class ModelDdlAction {

    /**
     * 动作在本次清单中的顺序，从1开始；一个动作可能生成多条SQL，不能直接当成SQL步骤序号。
     */
    private Integer actionNo;

    /**
     * 由模型ID、表角色、操作和固定对象标识组成，用于匹配依赖，不使用可变的物理名称。
     */
    private String actionKey;

    /**
     * 需要建表、加列、扩长还是建索引；没有业务删除动作。
     */
    private ModelDdlOperation operation;

    /**
     * 需要操作的模型、角色、表、列或索引。
     */
    private ModelSchemaObjectRef object;

    /**
     * 修改前完整定义；新增对象为空，用来防止扩长时顺带改变其他属性。
     */
    private JSONObject beforeDefinition;

    /**
     * 本次完整定义；建表动作包含该表的全部列和索引，后续只把主键并入CREATE。
     */
    private JSONObject afterDefinition;

    /**
     * 本动作依赖的其他动作固定标识；后续拆成多条SQL时，还要转成实际SQL步骤依赖。
     */
    private List<String> dependencyActionKeys = new ArrayList<>();
}
