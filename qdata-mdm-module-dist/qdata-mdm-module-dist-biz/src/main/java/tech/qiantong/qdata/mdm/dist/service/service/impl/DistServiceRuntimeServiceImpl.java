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

package tech.qiantong.qdata.mdm.dist.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceFieldDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceCallLogMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceFieldMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceMapper;
import tech.qiantong.qdata.mdm.dist.dal.repository.service.DistServiceRuntimeRepository;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import tech.qiantong.qdata.mdm.dist.util.DistServiceQueryBuilder;
import tech.qiantong.qdata.mdm.dist.service.service.DistServiceRuntimeService;
import tech.qiantong.qdata.mdm.model.base.dto.ModelMaterializedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 匹配已发布服务，解析模型字段配置并记录每次运行时调用结果。
 *
 */
@Service
public class DistServiceRuntimeServiceImpl implements DistServiceRuntimeService {
    /**
     * 单次查询允许的最大条数。
     */
    private static final int MAX_PAGE_SIZE = 100;

    @Resource
    private DistServiceMapper serviceMapper;
    @Resource
    private DistServiceFieldMapper fieldMapper;
    @Resource
    private DistServiceCallLogMapper callLogMapper;
    @Resource
    private ModelPublishedApi modelPublishedApi;
    @Resource
    private DistServiceRuntimeRepository repository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Object query(HttpServletRequest request) {
        String servicePath = stripPrefix(request.getRequestURI(), request.getContextPath());
        Match match = resolveService(servicePath);
        DistServiceDO service = match.service;
        long started = System.currentTimeMillis();
        DistServiceCallLogDO log = createCallLog(service, request, match.pathParameters.keySet());
        try {
            /*
             * 在 Service 层解析参数，使参数校验失败也能进入调用日志。
             */
            Map<String, String> parameters = new LinkedHashMap<>();
            Integer pageNum = null;
            Integer pageSize = null;
            for (Map.Entry<String, String[]> item : request.getParameterMap().entrySet()) {
                String key = item.getKey();
                String[] values = item.getValue();
                if (values == null || values.length != 1) {
                    throw new ServiceException("请求参数不能重复");
                }
                if ("pageNum".equalsIgnoreCase(key)) {
                    if (pageNum != null) throw new ServiceException("请求参数不能重复");
                    pageNum = parsePage(values[0]);
                } else if ("pageSize".equalsIgnoreCase(key)) {
                    if (pageSize != null) throw new ServiceException("请求参数不能重复");
                    pageSize = parsePage(values[0]);
                } else {
                    parameters.put(key, values[0]);
                }
            }
            /*
             * 路径变量和查询参数共同组成筛选条件，同名键不允许覆盖。
             */
            Map<String, String> filters = new LinkedHashMap<>(match.pathParameters);
            for (Map.Entry<String, String> item : parameters.entrySet()) {
                if (filters.put(item.getKey(), item.getValue()) != null) {
                    throw new ServiceException("路径参数与查询参数重复");
                }
            }
            int currentPage = pageNum == null ? DistServiceConstants.DEFAULT_PAGE_NUM : pageNum;
            int currentSize = pageSize == null ? DistServiceConstants.DEFAULT_PAGE_SIZE : pageSize;
            if (!DistServiceConstants.API_TYPE_SINGLE.equals(service.getApiType())
                    && !DistServiceConstants.API_TYPE_PAGE.equals(service.getApiType())) {
                throw new ServiceException("数据服务 API 类型无效");
            }
            if (currentPage < 1 || currentSize < 1 || currentSize > MAX_PAGE_SIZE) {
                throw new ServiceException("分页参数超出允许范围");
            }
            if (DistServiceConstants.API_TYPE_SINGLE.equals(service.getApiType())) {
                if (filters.isEmpty()) {
                    throw new ServiceException("单条查询必须传入至少一个查询条件");
                }
                for (Map.Entry<String, String> filter : filters.entrySet()) {
                    if (!StringUtils.hasText(filter.getValue())) {
                        throw new ServiceException("查询条件不能为空：" + filter.getKey());
                    }
                }
            }

            List<DistServiceFieldDO> serviceFields = fieldMapper.selectList(
                    new LambdaQueryWrapper<DistServiceFieldDO>().eq(DistServiceFieldDO::getServiceId, service.getId())
                            .orderByAsc(DistServiceFieldDO::getSortOrder, DistServiceFieldDO::getId));
            ModelPublishedMaterializationRespDTO materialization = modelPublishedApi
                    .getPublishedModelMaterialization(service.getModelId());
            ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(service.getModelId());
            ModelPublishedMaterializedTableRespDTO table = findAuthorityTable(materialization, service.getModelId());

            /*
             * 将模型字段 ID 关联到已发布物化表中的实际列名，供筛选和投影使用。
             */
            Map<Long, String> fieldCodes = structure.getFields().stream()
                    .filter(column -> !Boolean.TRUE.equals(column.getDelFlag()) && column.getCode() != null)
                    .collect(Collectors.toMap(ModelPublishedColumnRespDTO::getId,
                            column -> column.getCode().toLowerCase(Locale.ROOT), (first, second) -> first));
            Map<Long, String> physicalColumns = new HashMap<>();
            for (ModelMaterializedColumnRespDTO column : table.getColumns()) {
                if (!Boolean.TRUE.equals(column.getEnabledFlag()) || !Boolean.TRUE.equals(column.getValidFlag())
                        || Boolean.TRUE.equals(column.getDelFlag())) {
                    continue;
                }
                Long fieldId = column.getModelFieldId();
                if (fieldId == null && column.getSystemFieldKey() != null) {
                    String key = normalizeSystemKey(column.getSystemFieldKey());
                    fieldId = structure.getFields().stream()
                            .filter(field -> normalizeSystemKey(field.getCode()).equals(key))
                            .map(ModelPublishedColumnRespDTO::getId).findFirst().orElse(null);
                }
                if (fieldId != null) {
                    physicalColumns.put(fieldId, column.getColumnName());
                }
            }
            /*
             * 未配置别名时使用模型字段编码作为响应键。
             */
            List<DistServiceFieldDO> outputs = DistServiceQueryBuilder.outputFields(serviceFields);
            for (DistServiceFieldDO output : outputs) {
                if (output.getFieldAlias() == null || output.getFieldAlias().trim().isEmpty()) {
                    output.setFieldAlias(fieldCodes.get(output.getModelColumnId()));
                }
            }
            List<Object> values = new ArrayList<>();
            String where = DistServiceQueryBuilder.buildWhere(serviceFields, fieldCodes, physicalColumns,
                    filters, DialectFactory.getDialect(DbType.getDbType(materialization.getDatabaseType())), values);
            PageResult<Map<String, Object>> result = repository.query(materialization, table, outputs,
                    physicalColumns, where, values, currentPage, currentSize,
                    DistServiceConstants.API_TYPE_SINGLE.equals(service.getApiType()));
            log.setCallResult("SUCCESS");
            log.setHttpStatusCode(200L);
            log.setResponseDataCount((long) result.getRows().size());
            log.setResponseSummary("rows=" + result.getRows().size());
            return DistServiceConstants.API_TYPE_SINGLE.equals(service.getApiType())
                    ? (result.getRows().isEmpty() ? null : result.getRows().get(0)) : result;
        } catch (RuntimeException exception) {
            log.setCallResult("FAILURE");
            log.setHttpStatusCode(400L);
            log.setErrorMessage(safeMessage(exception));
            throw exception;
        } finally {
            log.setDurationMs(System.currentTimeMillis() - started);
            log.setCallTime(new Date());
            callLogMapper.insert(log);
        }
    }

    /**
     * 仅从启用且有效的服务中查找唯一匹配路径。
     */
    private Match resolveService(String path) {
        List<DistServiceDO> candidates = serviceMapper.selectList(new LambdaQueryWrapper<DistServiceDO>()
                .eq(DistServiceDO::getStatus, DistServiceConstants.STATUS_ENABLED)
                .eq(DistServiceDO::getValidFlag, DistServiceConstants.VALID_FLAG_YES));
        List<Match> matches = new ArrayList<>();
        for (DistServiceDO candidate : candidates) {
            if (StringUtils.hasText(candidate.getServicePath()) && pathMatcher.match(candidate.getServicePath(), path)) {
                matches.add(new Match(candidate, pathMatcher.extractUriTemplateVariables(candidate.getServicePath(), path)));
            }
        }
        if (matches.size() != 1) throw new ServiceException("数据服务路径不存在或匹配不唯一");
        return matches.get(0);
    }

    /**
     * 运行时查询只使用模型当前唯一启用且有效的权威物化表。
     */
    private ModelPublishedMaterializedTableRespDTO findAuthorityTable(
            ModelPublishedMaterializationRespDTO materialization, Long modelId) {
        if (materialization == null || materialization.getTables() == null) {
            throw new ServiceException("模型没有可用的已发布物化表");
        }
        List<ModelPublishedMaterializedTableRespDTO> tables = materialization.getTables().stream()
                .filter(table -> modelId.equals(table.getModelId()))
                .filter(table -> "AUTHORITY".equalsIgnoreCase(table.getTableRole()))
                .filter(table -> Boolean.TRUE.equals(table.getEnabledFlag())
                        && Boolean.TRUE.equals(table.getValidFlag()) && !Boolean.TRUE.equals(table.getDelFlag()))
                .collect(Collectors.toList());
        if (tables.size() != 1) throw new ServiceException("模型没有唯一启用的 AUTHORITY 物化表");
        return tables.get(0);
    }

    /**
     * 记录请求元信息，不保存筛选参数的实际值。
     */
    private DistServiceCallLogDO createCallLog(DistServiceDO service, HttpServletRequest request,
            Set<String> pathParameterNames) {
        DistServiceCallLogDO log = new DistServiceCallLogDO();
        log.setServiceId(service.getId());
        log.setServiceName(service.getName());
        log.setCallerSystem("runtime-api");
        log.setCallerIp(request.getRemoteAddr());
        log.setRequestPath(request.getRequestURI());
        log.setRequestMethod(request.getMethod());
        Set<String> fieldNames = new LinkedHashSet<>(pathParameterNames);
        fieldNames.addAll(request.getParameterMap().keySet());
        log.setRequestSummary("fields=" + String.join(",", fieldNames));
        log.setValidFlag("1");
        log.setDelFlag("0");
        return log;
    }

    /**
     * 去掉 services 路由前缀，保留服务路径供模板匹配。
     */
    private String stripPrefix(String requestPath, String contextPath) {
        String path = requestPath;
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        String prefix = DistServiceConstants.API_ROUTE_PREFIX;
        if (!path.equals(prefix) && !path.startsWith(prefix + "/")) {
            throw new ServiceException("数据服务路径无效");
        }
        path = path.substring(prefix.length());
        return path.isEmpty() ? "/" : path;
    }

    /**
     * 将分页参数转换为整数，格式校验统一返回服务异常。
     */
    private Integer parsePage(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new ServiceException("分页参数格式错误");
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? "数据服务查询失败" : message.substring(0, Math.min(message.length(), 500));
    }

    /**
     * 系统字段键不区分大小写和下划线，便于匹配物化列定义。
     */
    private String normalizeSystemKey(String value) {
        return value == null ? "" : value.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static final class Match {
        private final DistServiceDO service;
        private final Map<String, String> pathParameters;

        private Match(DistServiceDO service, Map<String, String> pathParameters) {
            this.service = service;
            this.pathParameters = pathParameters;
        }
    }
}
