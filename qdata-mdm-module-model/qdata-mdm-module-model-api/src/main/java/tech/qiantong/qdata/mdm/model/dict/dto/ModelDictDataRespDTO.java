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

package tech.qiantong.qdata.mdm.model.dict.dto;

import lombok.*;

/**
 * 参考数据项 DTO 对象 MODEL_DICT_TYPE
 *
 * @author qdata
 * @date 2026-09-16
 */
@Data
public class ModelDictDataRespDTO {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 参考数据类型ID */
    private Long typeId;

    /** 项编码 */
    private String code;

    /** 项名称 */
    private String name;

    /** 排序号 */
    private Long sortOrder;

    /** 逻辑删除标志：0未删除，1已删除 */
    private Boolean delFlag;

    /** 描述 */
    private String description;

    /** 是否有效；0：无效，1：有效 */
    private Boolean validFlag;


}
