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

package tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance;

import lombok.Value;
import java.util.List;
import java.util.Map;

/** 一条已校验记录的写入内容；整批由同一目标连接提交。 */
@Value
public class RecordMutation {
    boolean insert;
    Long requestItemId;
    Long recordId;
    Map<String, Object> values;
    String expectedStatus;
    String resultData;
    List<TableMutation> children;

    public RecordMutation(boolean insert, Long requestItemId, Long recordId, Map<String, Object> values,
            String expectedStatus, String resultData, List<TableMutation> children) {
        this.insert = insert;
        this.requestItemId = requestItemId;
        this.recordId = recordId;
        this.values = values;
        this.expectedStatus = expectedStatus;
        this.resultData = resultData;
        this.children = children;
    }

    public RecordMutation(Long requestItemId, Long recordId, Map<String, Object> values,
            String expectedStatus, String resultData, List<TableMutation> children) {
        this(false, requestItemId, recordId, values, expectedStatus, resultData, children);
    }

    public static RecordMutation insert(Long requestItemId, Long recordId, Map<String, Object> values,
            String resultData, List<TableMutation> children) {
        return new RecordMutation(true, requestItemId, recordId, values, null, resultData, children);
    }
}
