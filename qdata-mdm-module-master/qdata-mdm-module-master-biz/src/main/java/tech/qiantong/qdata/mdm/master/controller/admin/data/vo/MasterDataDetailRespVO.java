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

package tech.qiantong.qdata.mdm.master.controller.admin.data.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 单条正式主数据详情，字段随模型动态变化。
 */
@Data
public class MasterDataDetailRespVO {
    /** 详情标题使用模型唯一名称字段，而非假定第一个业务字段。 */
    private String recordName;
    private List<MasterColumnRespVO> businessColumns;
    private List<MasterColumnRespVO> systemColumns;
    /** 兼容已有详情调用方。 */
    private List<MasterColumnRespVO> columns;
    private Map<String, Object> row;
}
