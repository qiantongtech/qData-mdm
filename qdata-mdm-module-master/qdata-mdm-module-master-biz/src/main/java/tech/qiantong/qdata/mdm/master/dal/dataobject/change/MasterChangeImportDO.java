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

package tech.qiantong.qdata.mdm.master.dal.dataobject.change;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

/** 随导入申请同事务保存的上传信息，不保存原文件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("MASTER_CHANGE_IMPORT")
public class MasterChangeImportDO extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long requestId;
    private Long modelId;
    private Long modelVersionId;
    private Long instanceId;
    private String templateVersionNo;
    private String fileName;
    private String filePath;
    private String importMode;
    private String matchField;
    private Integer totalCount;
    private Integer createCount;
    private Integer updateCount;
    private Boolean validFlag;
    @TableLogic
    private Boolean delFlag;
}
