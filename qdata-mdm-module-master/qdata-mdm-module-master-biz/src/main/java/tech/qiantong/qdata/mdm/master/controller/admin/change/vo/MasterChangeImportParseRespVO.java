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

package tech.qiantong.qdata.mdm.master.controller.admin.change.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态模型模板解析结果，供整批校验和暂存接口复用。
 */
@Data
public class MasterChangeImportParseRespVO {
    private List<String> headers = new ArrayList<>();
    private List<MasterChangeRequestBatchSaveReqVO.ImportRow> rows = new ArrayList<>();
    private int total;
}
