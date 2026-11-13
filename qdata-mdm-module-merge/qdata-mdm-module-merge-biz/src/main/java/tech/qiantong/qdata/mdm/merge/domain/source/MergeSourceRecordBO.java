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

package tech.qiantong.qdata.mdm.merge.domain.source;

import java.util.Date;
import lombok.Data;

/**
 * 模型已发布来源表的一行数据，不映射固定数据库表。
 * 业务字段随模型变化，保存在 modelData 中；id 为来源表物理行主键。
 *
 * @author chentangwen
 */
@Data
public class MergeSourceRecordBO {
    private Long id;
    private Long modelId;
    private Long sourceSystemId;
    private Long taskId;
    private String sourceRecordKey;
    private String governanceStatus;
    private Date receiveTime;
    private String modelData;
}
