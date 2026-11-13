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

package tech.qiantong.qdata.mdm.audit.dal.dataobject.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/** 审计追溯读取权威主数据快照所需的字段。 */
@Data
@TableName("MASTER_DATA_VERSION")
public class MasterDataVersionSnapshotDO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;
    private Long modelVersionId;
    private Long masterRecordId;
    private Long changeId;
    private Integer versionSeq;
    private String versionNo;
    private String generateType;
    private Date generateTime;
    private String currentFlag;
    private String snapshotData;

    @TableLogic(value = "0", delval = "1")
    private String delFlag;
}
