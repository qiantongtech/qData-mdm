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

import java.util.Date;
import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;

/**
 * 主数据服务定义 DO 对象 DIST_SERVICE
 *
 * @author chentangwen
 * @date 2026-10-20
 */
@Data
@TableName(value = "DIST_SERVICE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DistServiceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 服务编码
     */
    private String code;

    /**
     * 服务名称
     */
    private String name;

    /**
     * 主数据模型ID
     */
    private Long modelId;

    /**
     * API类型：0单条查询、1列表分页查询；历史未配置记录为空。
     */
    private String apiType;

    /**
     * 负责人用户ID，姓名和电话从系统用户信息回显。
     */
    private Long responsibleUserId;

    /**
     * 服务路径
     */
    private String servicePath;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 服务状态：0停用、1启用。
     */
    private String status;

    /**
     * 发布状态：0未发布、1已发布
     */
    private String publishStatus;

    /**
     * 描述
     */
    private String description;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 是否有效；0：无效，1：有效
     */
    private String validFlag;

    /**
     * 逻辑删除标志：0未删除，1已删除
     */
    @TableLogic(value = DistServiceConstants.NOT_DELETED, delval = DistServiceConstants.DELETED)
    private String delFlag;

    /**
     * 兼容公共基类的Updator拼写，使UPDATER_ID可按标准Bean属性映射和复制。
     */
    public Long getUpdaterId() {
        return getUpdatorId();
    }

    public void setUpdaterId(Long updaterId) {
        setUpdatorId(updaterId);
    }
}
