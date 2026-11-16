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
 * Business License: https://qdata.qiantong.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.source;

import java.util.Date;
import java.util.Map;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;

/** 分发读取入口；模型数据接口接入后由对应实现提供记录。 */
public interface DistTaskRecordSource {
    boolean supports(Long modelId);

    Iterable<Record> read(DistTaskDO task, Date cutoff);

    final class Record {
        private final Long id;
        private final String code;
        private final String name;
        private final Date updateTime;
        private final Map<Long, Object> fields;

        public Record(Long id, String code, String name, Date updateTime, Map<Long, Object> fields) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.updateTime = updateTime;
            this.fields = fields;
        }

        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public Date getUpdateTime() { return updateTime; }
        public Map<Long, Object> getFields() { return fields; }
    }
}
