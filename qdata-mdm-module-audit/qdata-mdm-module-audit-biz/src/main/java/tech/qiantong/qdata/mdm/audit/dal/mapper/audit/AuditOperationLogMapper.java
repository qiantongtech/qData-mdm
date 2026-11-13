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

package tech.qiantong.qdata.mdm.audit.dal.mapper.audit;

import tech.qiantong.qdata.mdm.audit.dal.dataobject.audit.AuditOperationLogDO;
import java.util.Arrays;
import com.github.yulichang.base.MPJBaseMapper;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import java.util.HashSet;
import java.util.Set;
import tech.qiantong.qdata.mdm.audit.controller.admin.audit.vo.AuditOperationLogPageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务操作审计Mapper接口
 *
 * @author zhangyanyan
 * @date 2026-09-22
 */
public interface AuditOperationLogMapper extends BaseMapperX<AuditOperationLogDO> {

    default Page<AuditOperationLogDO> selectTracePage(Page<AuditOperationLogDO> page, Map<String, String> filters, Date begin, Date end) {
        return selectPage(page, traceQuery(filters, begin, end));
    }

    default List<AuditOperationLogDO> selectTraceList(Map<String, String> filters, Date begin, Date end) {
        return selectList(traceQuery(filters, begin, end));
    }

    /** 操作审计的数据库筛选与排序；业务结果转换仍由 Service 处理。 */
    default LambdaQueryWrapper<AuditOperationLogDO> traceQuery(Map<String, String> filters, Date begin, Date end) {
        LambdaQueryWrapper<AuditOperationLogDO> query = new LambdaQueryWrapper<>();
        String scope = filters.get("scope");
        if ("model".equals(scope) || "master".equals(scope)) {
            List<String> codes = traceSplit(filters.get("_moduleCodes"));
            if (codes.isEmpty()) query.eq(AuditOperationLogDO::getId, -1L);
            else query.in(AuditOperationLogDO::getModuleCode, codes);
        }
        if (traceHasText(filters.get("type"))) query.in(AuditOperationLogDO::getType, traceSplit(filters.get("_typeCodes")));
        if (traceHasText(filters.get("_modelIds"))) {
            query.in(AuditOperationLogDO::getModelId, traceSplit(filters.get("_modelIds")));
        }
        if (traceHasText(filters.get("objectName"))) query.like(AuditOperationLogDO::getObjectName, filters.get("objectName"));
        if (traceHasText(filters.get("objectId"))) query.like(AuditOperationLogDO::getObjectId, filters.get("objectId"));
        if (traceHasText(filters.get("operateId"))) query.eq(AuditOperationLogDO::getId, filters.get("operateId"));
        if (traceHasText(filters.get("operatorName"))) query.like(AuditOperationLogDO::getOperatorName, filters.get("operatorName"));
        List<String> ids = traceSplit(filters.get("treeModelIds"));
        if (traceHasText(filters.get("treeModelIds")) || traceHasText(filters.get("treeModelNames"))) {
            if (ids.isEmpty() || "__NO_MATCH__".equals(ids.get(0))) query.eq(AuditOperationLogDO::getModelId, -1L);
            else query.in(AuditOperationLogDO::getModelId, ids);
        }
        if (begin != null) query.ge(AuditOperationLogDO::getOperateTime, begin);
        if (end != null) {
            if (begin == null) query.and(condition -> condition.lt(AuditOperationLogDO::getOperateTime, end)
                    .or().isNull(AuditOperationLogDO::getOperateTime));
            else query.lt(AuditOperationLogDO::getOperateTime, end);
        }
        if ("id".equals(filters.get("orderByColumn")) && "ascending".equals(filters.get("isAsc"))) {
            query.orderByAsc(AuditOperationLogDO::getId);
        } else if ("id".equals(filters.get("orderByColumn")) && "descending".equals(filters.get("isAsc"))) {
            query.orderByDesc(AuditOperationLogDO::getId);
        } else {
            query.orderByDesc(AuditOperationLogDO::getOperateTime, AuditOperationLogDO::getId);
        }
        return query;
    }

    static boolean traceHasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** 模块编码可能有大小写差异，使用 Lambda 条件兼容原有数据。 */
    static List<String> traceModelModuleVariants() {
        String upper = "MODEL";
        List<String> variants = new ArrayList<>(32);
        for (int mask = 0; mask < 32; mask++) {
            char[] chars = upper.toCharArray();
            for (int index = 0; index < chars.length; index++) {
                if ((mask & (1 << index)) != 0) chars[index] = Character.toLowerCase(chars[index]);
            }
            variants.add(new String(chars));
        }
        return variants;
    }

    static List<String> traceSplit(String value) {
        return !traceHasText(value) ? Collections.emptyList() : Arrays.stream(value.split(","))
                .map(String::trim).filter(item -> !item.isEmpty()).collect(Collectors.toList());
    }

    default PageResult<AuditOperationLogDO> selectPage(AuditOperationLogPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<AuditOperationLogDO>()
                .eqIfPresent(AuditOperationLogDO::getModuleCode, reqVO.getModuleCode())
                .eqIfPresent(AuditOperationLogDO::getFunctionCode, reqVO.getFunctionCode())
                .eqIfPresent(AuditOperationLogDO::getObjectType, reqVO.getObjectType())
                .eqIfPresent(AuditOperationLogDO::getObjectId, reqVO.getObjectId())
                .likeIfPresent(AuditOperationLogDO::getObjectName, reqVO.getObjectName())
                .eqIfPresent(AuditOperationLogDO::getType, reqVO.getType())
                .eqIfPresent(AuditOperationLogDO::getOperatorId, reqVO.getOperatorId())
                .likeIfPresent(AuditOperationLogDO::getOperatorName, reqVO.getOperatorName())
                .eqIfPresent(AuditOperationLogDO::getOperateResult, reqVO.getOperateResult())
                .eqIfPresent(AuditOperationLogDO::getOperateTime, reqVO.getOperateTime())
                .eqIfPresent(AuditOperationLogDO::getRequestIp, reqVO.getRequestIp())
                .eqIfPresent(AuditOperationLogDO::getRequestId, reqVO.getRequestId())
                .eqIfPresent(AuditOperationLogDO::getTraceId, reqVO.getTraceId())
                .eqIfPresent(AuditOperationLogDO::getErrorMessage, reqVO.getErrorMessage())
                .eqIfPresent(AuditOperationLogDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(AuditOperationLogDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }
}

