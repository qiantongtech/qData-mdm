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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

/**
 * 系统预置字段固定字典。默认编码和物理列名统一为大写，不对业务字段自动改名。
 */
@Getter
@AllArgsConstructor
public enum ModelSystemField {
    /**
     * 主表、子表统一自增主键；权威申请仍可预分配并显式写入记录ID。
     */
    ID("INTEGER", null, false, null, "记录主键，自增列"),
    /**
     * 子表对应的本角色主表记录ID。
     */
    PARENT_ID("INTEGER", null, true, null, "所属主表记录ID，来源子表关联来源主表，权威子表关联权威主表"),
    /**
     * 分类模型对应的分类节点ID。
     */
    CATEGORY_ID("INTEGER", null, true, null, "所属分类节点ID"),
    /**
     * 权威记录形成方式，例如平台录入或融合。
     */
    DATA_SOURCE("STRING", 64L, true, null, "数据形成方式，例如平台录入或融合"),
    /**
     * 权威记录创建人名称。
     */
    CREATED_BY("STRING", 64L, true, null, "创建人名称"),
    /**
     * 权威记录创建时间。
     */
    CREATED_TIME("DATETIME", null, true, null, "创建时间"),
    /**
     * 权威记录创建人ID，沿用现有预制字段的字符串类型。
     */
    CREATED_BY_ID("STRING", 64L, true, null, "创建人ID"),
    /**
     * 权威记录最后修改人名称。
     */
    UPDATED_BY("STRING", 64L, true, null, "最后修改人名称"),
    /**
     * 权威记录最后修改人ID。
     */
    UPDATED_BY_ID("STRING", 64L, true, null, "最后修改人ID"),
    /**
     * 权威记录最后修改时间。
     */
    UPDATED_TIME("DATETIME", null, true, null, "最后修改时间"),
    /**
     * 权威记录备注。
     */
    REMARK("TEXT", null, true, null, "记录备注"),
    /**
     * 来源和权威记录的业务状态，两类表使用相同字段定义。
     */
    STATUS("STRING", 64L, true, null, "数据业务状态"),
    /**
     * 来源系统平台ID。
     */
    SOURCE_SYSTEM_ID("INTEGER", null, true, null, "来源系统ID"),
    /**
     * 接入任务平台ID。
     */
    ACCESS_TASK_ID("INTEGER", null, true, null, "接入任务ID"),
    /**
     * 来源批次标识，使用字符串容纳不同接入方式的批次编号。
     */
    BATCH_ID("STRING", 255L, true, null, "接入批次标识"),
    /**
     * 外部来源记录标识，和平台内部id是两个字段，不自增。
     */
    SOURCE_RECORD_ID("STRING", 255L, true, null, "外部来源记录标识，与平台内部记录主键不同"),
    /**
     * 本次来源数据采集时间。
     */
    COLLECTION_TIME("DATETIME", null, true, null, "数据采集时间"),
    /**
     * 系统逻辑删除标记，0正常、1已删除。
     */
    DEL_FLAG("BOOLEAN", null, false, "0", "逻辑删除标记，0正常，1已删除");

    /**
     * 平台逻辑类型，具体数据库类型交给common转换。
     */
    private final String logicalType;
    /**
     * 字符串系统列的固定长度。
     */
    private final Long characterLength;
    /**
     * 系统列是否允许为空，和来源业务字段的放宽规则分开。
     */
    private final boolean nullable;
    /**
     * 静态默认值，不在这里写数据库表达式。
     */
    private final String defaultValue;
    /**
     * 写入数据库列注释的中文说明，让查看物理表的人知道字段用途。
     */
    private final String columnComment;

    /**
     * 返回系统字段默认列名，不进行大小写转换。
     *
     * @return 例如CREATED_TIME
     */
    public String getColumnName() {
        return name();
    }

    /**
     * 识别现有的大写预制编码或规范的小写名称，不接受自造系统字段及混合大小写写法。
     *
     * @param code 模型配置中的系统字段编码
     * @return 找到的预制字段，找不到时返回null
     */
    public static ModelSystemField getByCode(String code) {
        for (ModelSystemField field : values()) {
            if (field.name().equals(code) || field.name().toLowerCase(Locale.ROOT).equals(code)) {
                return field;
            }
        }
        return null;
    }
}
