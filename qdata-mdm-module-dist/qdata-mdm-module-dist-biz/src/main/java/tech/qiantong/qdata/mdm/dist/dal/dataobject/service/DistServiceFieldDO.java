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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.dataobject.service;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;

/**
 * 数据服务字段配置 DO 对象 DIST_SERVICE_FIELD
 *
 * @author chentangwen
 * @date 2026-10-20
 */
@Data
@TableName(value = "DIST_SERVICE_FIELD")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistServiceFieldDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 数据服务ID
     */
    private Long serviceId;

    /**
     * 模型字段ID
     */
    private Long modelColumnId;

    /**
     * 是否可作为查询条件；0：否，1：是
     */
    private String queryableFlag;

    /**
     * 是否作为响应字段输出；0：否，1：是
     */
    private String outputFlag;

    /**
     * 服务输出字段别名
     */
    private String fieldAlias;

    /**
     * 示例值
     */
    private String exampleValue;

    /**
     * 查询条件示例值
     */
    private String queryExampleValue;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 是否有效；0：无效，1：有效
     */
    private String validFlag;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    @TableLogic(value = DistServiceConstants.NOT_DELETED, delval = DistServiceConstants.DELETED)
    private String delFlag;

    public Long getUpdaterId() {
        return getUpdatorId();
    }

    public void setUpdaterId(Long updaterId) {
        setUpdatorId(updaterId);
    }
}
