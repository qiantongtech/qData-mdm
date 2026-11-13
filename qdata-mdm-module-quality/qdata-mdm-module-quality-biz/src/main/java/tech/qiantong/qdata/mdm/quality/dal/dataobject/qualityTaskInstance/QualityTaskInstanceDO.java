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

package tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityTaskInstance;

import java.util.Date;

import lombok.*;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/**
 * 主数据质量任务执行实例 DO 对象 QUALITY_TASK_INSTANCE
 *
 * @author qdata
 * @date 2026-09-22
 */
@Data
@TableName(value = "QUALITY_TASK_INSTANCE")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
// @KeySequence("QUALITY_TASK_INSTANCE_seq")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class QualityTaskInstanceDO extends BaseEntity {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质量任务ID */
    private Long qualityTaskId;

    /** 质量任务编码 */
    private String taskCode;

    /** 主数据模型ID */
    private Long modelId;

    /** 主数据模型版本 */
    private String modelVersion;

    /** 实例编号 */
    private String executionNo;

    /** 负责人用户 ID */
    private String personCharge;

    /** 负责人电话 */
    private String contactNumber;

    /** 触发方式：1人工执行、2定时执行 */
    private String triggerType;

    /** 执行结果：1执行中、2失败、3部分成功、4成功 */
    private String status;

    /** 本次执行开始时间 */
    private Date startTime;

    /** 本次执行结束时间 */
    private Date endTime;

    /** 检查数据记录数 */
    private Long totalCount;

    /** 通过数据记录数 */
    private Long passCount;

    /** 问题数据记录数 */
    private Long failCount;

    /** 执行失败说明 */
    private String errorMessage;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;

    /** 删除标志：0未删除，1已删除 */
    @TableLogic
    private Boolean delFlag;


}
