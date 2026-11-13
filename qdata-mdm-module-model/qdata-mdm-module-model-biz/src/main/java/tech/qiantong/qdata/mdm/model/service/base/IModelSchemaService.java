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

package tech.qiantong.qdata.mdm.model.service.base;

import tech.qiantong.qdata.mdm.model.domain.ModelMaterializedStructure;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishStructureDiff;

/**
 * 统一构建来源、权威主子表结构，并输出完整新旧结构比较结果。
 */
public interface IModelSchemaService {

    /**
     * 整体构建主模型及直接子模型的双表，全部完成后才把结果放入发布上下文。
     *
     * <p>
     * preparePlan整理动作时调用：先完成CONFIG校验，再分别创建SOURCE和AUTHORITY结构，最后合入必须保留的旧物理对象。
     *
     * @param context 已读取本次配置和上次成功结构的发布数据
     * @return 本次完整结构，包含必须保留的停用物理对象
     */
    ModelMaterializedStructure build(ModelPublishContext context);

    /**
     * 先在独立副本上完成比较，再写入上下文，避免后续修改配置把本次比较记录一起改掉。
     *
     * <p>
     * build完成后调用：以模型ID、角色、字段ID等固定身份匹配对象，再逐项比较属性；这里只记录差异，不批准DDL。
     *
     * @param context 已构建完整新结构的发布数据
     * @return 包含全部对象及逐项属性的比较结果
     */
    ModelPublishStructureDiff compare(ModelPublishContext context);
}
