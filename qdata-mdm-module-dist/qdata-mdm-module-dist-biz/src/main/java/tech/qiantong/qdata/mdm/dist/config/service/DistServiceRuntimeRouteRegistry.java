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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.config.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tech.qiantong.qdata.mdm.dist.controller.openapi.service.DistServiceRuntimeController;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceMapper;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import tech.qiantong.qdata.mdm.dist.service.service.event.DistServiceRuntimeRouteChangedEvent;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 启用数据服务时，按服务路径动态注册运行时 API。
 *
 * @author chentangwen
 */
@Component
public class DistServiceRuntimeRouteRegistry implements ApplicationRunner {

    @Resource
    private RequestMappingHandlerMapping handlerMapping;
    @Resource
    private DistServiceRuntimeController controller;
    @Resource
    private DistServiceMapper serviceMapper;

    private final Map<Long, RequestMappingInfo> registeredMappings = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        serviceMapper.selectList(null).stream()
                .filter(DistServiceRuntimeRouteRegistry::isRoutable)
                .forEach(this::register);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void refresh(DistServiceRuntimeRouteChangedEvent event) {
        DistServiceDO service = serviceMapper.selectById(event.getServiceId());
        if (isRoutable(service)) {
            register(service);
        } else {
            unregister(event.getServiceId());
        }
    }

    synchronized void register(DistServiceDO service) {
        unregister(service.getId());
        RequestMappingInfo requestMapping = RequestMappingInfo
                .paths(buildRequestPath(service.getServicePath()))
                .methods(resolveRequestMethod(service.getHttpMethod()))
                .build();
        try {
            Method queryMethod = DistServiceRuntimeController.class.getMethod("query", javax.servlet.http.HttpServletRequest.class);
            handlerMapping.registerMapping(requestMapping, controller, queryMethod);
            registeredMappings.put(service.getId(), requestMapping);
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("数据服务运行时处理方法不存在", exception);
        }
    }

    synchronized void unregister(Long serviceId) {
        RequestMappingInfo requestMapping = registeredMappings.remove(serviceId);
        if (requestMapping != null) {
            handlerMapping.unregisterMapping(requestMapping);
        }
    }

    static boolean isRoutable(DistServiceDO service) {
        return service != null
                && service.getId() != null
                && DistServiceConstants.STATUS_ENABLED.equals(service.getStatus())
                && DistServiceConstants.VALID_FLAG_YES.equals(service.getValidFlag())
                && StringUtils.hasText(service.getServicePath());
    }

    private static RequestMethod resolveRequestMethod(String httpMethod) {
        String method = StringUtils.hasText(httpMethod)
                ? httpMethod.trim().toUpperCase(Locale.ROOT)
                : DistServiceConstants.DEFAULT_HTTP_METHOD;
        return RequestMethod.valueOf(method);
    }

    static String buildRequestPath(String servicePath) {
        String normalizedPath = servicePath == null ? "" : servicePath.trim();
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        return DistServiceConstants.API_ROUTE_PREFIX + normalizedPath;
    }
}
