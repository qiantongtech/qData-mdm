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
import tech.qiantong.qdata.mdm.merge.domain.source.MergeSourceRecordBO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergeCandidateGroupMemberDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单页审核列表批量加载的数据，供摘要组装复用。
 *
 * @author chentangwen
 */
@Data
public class MergeReviewSummaryBO {

    /** 本页候选组编号。 */
    private List<Long> groupIds = new ArrayList<>();

    /** 本页模型编号。 */
    private Set<Long> modelIds = new HashSet<>();

    /** 本页有效目标成员。 */
    private List<MergeCandidateGroupMemberDO> members = new ArrayList<>();

    /** 各组目标来源编号，保留成员顺序。 */
    private Map<Long, List<Long>> sourceIds = new HashMap<>();

    /** 各组首个目标成员的来源表行主键。 */
    private Map<Long, Long> firstSourceRowIds = new HashMap<>();

    /** 本页全部目标来源编号。 */
    private Set<Long> allSourceIds = new HashSet<>();

    /** 按模型及物理行主键索引，避免不同来源表的相同主键相互覆盖。 */
    private Map<Long, Map<Long, MergeSourceRecordBO>> sourcesByModel = new HashMap<>();

    /** 来源系统。 */
    private Map<Long, IngestSourceDO> systems = new HashMap<>();

    /** 按模型索引字段定义。 */
    private Map<Long, List<ModelColumnDO>> columnsByModel = new HashMap<>();

    /** 每组去重后的命中规则。 */
    private Map<Long, Set<Long>> ruleIds = new HashMap<>();

    /** 本页命中规则的名称。 */
    private Map<Long, String> ruleNames = new HashMap<>();

    /** 每组命中字段，保留首次出现顺序。 */
    private Map<Long, LinkedHashSet<String>> hitCodes = new HashMap<>();
}
