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

package tech.qiantong.qdata.mdm.master.data.dto;

import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 同一次增量查询的分页结果。
 * 每条记录须包含 ID、CHANGE_TIME 及模型业务字段。
 */
@Data
public class MasterDataChangePageRespDTO {

    /** 本次查询固定的服务端截止时间；所有页面必须一致。 */
    private Date snapshotTime;

    /** 当前页主数据记录；CHANGE_TIME 使用 java.util.Date。 */
    private List<Map<String, Object>> rows;

    /** 下一页游标；为 null 或空串表示本次查询结束。 */
    private String nextCursor;
}
