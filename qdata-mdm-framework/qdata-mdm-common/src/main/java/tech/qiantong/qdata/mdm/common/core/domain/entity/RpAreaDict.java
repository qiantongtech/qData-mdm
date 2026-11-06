/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Service Resource Portal Area Dictionary DO Object RP_AREA_DICT
 *
 * @author qdata
 * @date 2025-04-21
 */
@Data
public class RpAreaDict extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** ID */
    private Long id;

    /** Region name */
    private String name;

    /** Encoding */
    private String code;

    /** Parent id */
    private Long parentId;

    /** Sort */
    private Long sortOrder;

    /** Type */
    private String type;

    /** Is it valid */
    private Boolean validFlag;

    /** Delete flag */
    @TableLogic
    private Boolean delFlag;

    private List<RpAreaDict> children = new ArrayList<RpAreaDict>();

}
