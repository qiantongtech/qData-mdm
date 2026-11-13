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

package tech.qiantong.qdata.mdm.model.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;

/**
 * 发布实例目标描述只读解析；数据库/Schema不再是实例表字段。
 */
public final class ModelInstanceTargetUtils {
    /**
     * 工具类只提供静态解析方法，不允许创建实例。
     */
    private ModelInstanceTargetUtils() {
    }

    /**
     * 从发布实例执行计划的target.schemaName读取目标Schema；缺省时使用连接默认Schema。
     *
     * @param instance 发布实例，可为空
     * @return 去除首尾空白的Schema名称；实例、计划或Schema未配置时返回null
     * @throws ServiceException 已有执行计划无法解析时抛出，避免误用默认Schema
     */
    public static String getSchemaName(ModelInstanceDO instance) {
        // 还没有发布记录，或计划尚未生成时，没有指定Schema，返回null表示使用连接默认Schema。
        if (instance == null || instance.getExecutionPlan() == null
                || instance.getExecutionPlan().trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject plan = JSON.parseObject(instance.getExecutionPlan());
            if (plan == null) throw new IllegalArgumentException("发布计划为空");
            // 执行计划里按target.schemaName读取：先取目标连接说明，再取Schema名称；没有配置时仍使用默认值。
            JSONObject target = plan.getJSONObject("target");
            String schema = target == null ? null : target.getString("schemaName");
            return schema == null || schema.trim().isEmpty() ? null : schema.trim();
        } catch (Exception ex) {
            throw new ServiceException("发布实例目标信息不可用，请检查执行计划");
        }
    }
}
