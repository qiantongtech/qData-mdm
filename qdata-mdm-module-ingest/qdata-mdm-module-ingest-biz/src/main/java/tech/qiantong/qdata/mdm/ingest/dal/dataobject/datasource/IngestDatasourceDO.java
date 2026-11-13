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

package tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;

import java.util.List;

/**
 * 数据源 DO 对象 DA_DATASOURCE
 *
 * @author lhs
 * @date 2025-01-21
 */
@Data
@TableName(value = "INGEST_DATASOURCE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class IngestDatasourceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据源名称
     */
    private String datasourceName;

    /**
     * 数据源类型
     */
    private String datasourceType;

    /**
     * 数据源配置(json字符串)
     */
    private String datasourceConfig;

    /**
     * 项目集合
     */
    /*  @TableField(exist = false)
    private List<DaDatasourceProjectRelDO> projectList;*/

    /**
     * 项目名称
     */
    @TableField(exist = false)
    private String projectName;

    /**
     * 是否是管理员分配给数据研发
     */
    @TableField(exist = false)
    private Boolean isAdminAddTo;

    /**
     * IP
     */
    private String ip;

    /**
     * 端口号
     */
    private Long port;

    /**
     * 数据库表数（预留）
     */
    private Long listCount;

    /**
     * 同步记录数（预留）
     */
    private Long syncCount;

    /**
     * 同步数据量大小（预留）
     */
    private Long dataSize;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否有效
     */
    private Boolean validFlag;

    /**
     * 创建人联系电话，仅用于列表展示。
     */
    @TableField(exist = false)
    private String createPhoneNumber;

    @TableLogic
    private Boolean delFlag;


    @JSONField(serialize = false)
    public String toJsonString() {
        // 默认 Fastjson：忽略 null 字段，字段顺序自动
        return JSON.toJSONString(this);
    }


    @JSONField(serialize = false)
    public DbQueryProperty simplify() {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(
                this.getDatasourceType(),
                this.getIp(),
                this.getPort(),
                this.getDatasourceConfig()
        );
        return dbQueryProperty;
    }

}
