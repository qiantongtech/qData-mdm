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

package tech.qiantong.qdata.mdm.merge.domain.review;

import lombok.Data;
import tech.qiantong.qdata.mdm.merge.support.SourceSnapshot;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.rule.dal.dataobject.ruleSurvivor.RuleSurvivorDO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核一次操作中固定的模型字段、成员来源快照和生存规则。
 *
 * @author chentangwen
 */
@Data
public class MergeReviewSourceContextBO {

    /** 模型字段，仅加载一次供所有分区使用。 */
    private List<ModelColumnDO> columns = new ArrayList<>();

    /** 按原成员编号索引的固定来源快照，无法解析时值为 null。 */
    private Map<Long, SourceSnapshot> snapshots = new HashMap<>();

    /** 按来源编号索引结果快照，拆分后的新成员编号变化时仍使用原固定版本。 */
    private Map<Long, SourceSnapshot> sourceSnapshots = new HashMap<>();

    /** 检测实例固定的模型版本。 */
    private Long modelVersionId;

    /** 生存规则，仅用于多成员结果。 */
    private List<RuleSurvivorDO> survivorRules = new ArrayList<>();
}
