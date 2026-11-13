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

package tech.qiantong.qdata.mdm.model.dal.dataobject.base;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 成功物化表定义 DO，对应 MODEL_MATERIALIZED_TABLE。
 *
 * @author qdata
 */
@Data
@TableName("MODEL_MATERIALIZED_TABLE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModelMaterializedTableDO extends BaseEntity {

    /**
     * 成功物化表定义ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 本次成功发布记录ID
     */
    private Long instanceId;

    /**
     * 当前物化表对应模型ID，主模型和子模型均保存自己的ID
     */
    private Long modelId;

    /**
     * 主模型为0，子模型为所属主模型ID
     */
    private Long parentId;

    /**
     * SOURCE来源/AUTHORITY权威；不通过解析表名前缀决定用途
     */
    private String tableRole;

    /**
     * 本成功实例内、同角色的主物化表记录ID，主表为空
     */
    private Long parentTableId;

    /**
     * 实际业务数据源，类型/连接从可信配置读取
     */
    private Long datasourceId;

    /**
     * 实际核验的物理表名
     */
    private String tableName;

    /**
     * 数据库真实表注释，与模型展示中文名分开
     */
    private String tableComment;

    /**
     * 实际主键与索引JSON，含索引名、用途/规则、唯一性和成员顺序
     */
    private String indexDefinition;

    /**
     * 本次成功结构中是否启用，停用表及其结构仍保留
     */
    private Boolean enabledFlag;

    /**
     * 本次结构核验时间
     */
    private Date verifiedTime;

    /**
     * 系统有效标志，1有效、0无效
     */
    private Boolean validFlag;

    /**
     * 系统逻辑删除标志，0未删除、1已删除；不删除实际物理表或字段
     */
    @TableLogic
    private Boolean delFlag;

}
