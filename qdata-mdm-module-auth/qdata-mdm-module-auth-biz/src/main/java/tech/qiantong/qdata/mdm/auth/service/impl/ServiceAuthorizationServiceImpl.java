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

package tech.qiantong.qdata.mdm.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.auth.api.serviceauthorization.dto.ServiceAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationStatus;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationType;
import tech.qiantong.qdata.mdm.auth.api.enums.DataScopeType;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceDTO;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceFieldDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthServiceAccessDO;
import tech.qiantong.qdata.mdm.auth.dal.mapper.AuthGrantFieldMapper;
import tech.qiantong.qdata.mdm.auth.dal.mapper.AuthServiceAccessMapper;
import tech.qiantong.qdata.mdm.auth.reference.AuthorizationReferenceService;
import tech.qiantong.qdata.mdm.auth.service.ServiceAuthorizationService;
import tech.qiantong.qdata.mdm.auth.support.AuthorizationJsonCodec;
import tech.qiantong.qdata.mdm.auth.support.DataScopeValidator;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 服务授权的保存、查询和撤权。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ServiceAuthorizationServiceImpl implements ServiceAuthorizationService {
    private static final String AUTH_TYPE_SERVICE = AuthorizationType.SERVICE.name();
    private static final String STATUS_ENABLED = AuthorizationStatus.ENABLED.getCode();
    private static final String STATUS_REVOKED = AuthorizationStatus.REVOKED.getCode();
    /** 列表排序白名单：只允许这些下划线列名参与 ORDER BY，避免排序字段注入。 */
    private static final Set<String> ALLOWED_SORT_COLUMNS = new HashSet<>(Arrays.asList("id", "create_time"));

    @Resource
    private AuthServiceAccessMapper serviceAccessMapper;
    @Resource
    private AuthGrantFieldMapper grantFieldMapper;
    @Resource
    private AuthorizationJsonCodec jsonCodec;
    @Resource
    private DataScopeValidator dataScopeValidator;
    @Resource
    private AuthorizationReferenceService referenceService;

    @Override
    public Long save(ServiceAuthorizationSaveReqDTO request) {
        validate(request);
        List<Long> subjectIds = getSubjectIds(request);
        if (request.getId() != null) {
            AuthServiceAccessDO previous = requireAccess(request.getId());
            if (subjectIds.size() != 1) {
                throw new ServiceException("编辑授权时只能选择一个授权主体");
            }
            if (findCurrentAccesses(request, subjectIds.get(0), new Date()).stream()
                    .anyMatch(item -> !request.getId().equals(item.getId()))) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), subjectIds.get(0))
                        + "对该数据服务已有其他授权，请勿重复配置");
            }
            AuthServiceAccessDO access = buildAccess(request, request.getId(), subjectIds.get(0));
            if (AuthorizationStatus.REVOKED.matches(previous.getStatus()) && access.getExpireTime() != null
                    && !access.getExpireTime().after(new Date())) {
                throw new ServiceException("恢复授权时请设置晚于当前时间的失效时间");
            }
            applyAccessUpdate(access, new Date());
            replaceFields(access.getId(), request.getFieldPermissions());
            return access.getId();
        }

        Long firstId = null;
        for (Long subjectId : subjectIds) {
            if (!findCurrentAccesses(request, subjectId, new Date()).isEmpty()) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), subjectId)
                        + "对该数据服务已有授权，请使用修改授权或批量授权");
            }
            AuthServiceAccessDO access = buildAccess(request, null, subjectId);
            serviceAccessMapper.insert(access);
            replaceFields(access.getId(), request.getFieldPermissions());
            if (firstId == null) {
                firstId = access.getId();
            }
        }
        return firstId;
    }

    @Override
    public BatchAuthorizationResultVO saveBatch(ServiceAuthorizationSaveReqDTO request) {
        if (request.getId() != null) {
            throw new ServiceException("批量授权不能指定单条授权编号");
        }
        validate(request);
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        Date now = new Date();
        for (Long subjectId : getSubjectIds(request)) {
            List<AuthServiceAccessDO> matches = findCurrentAccesses(request, subjectId, now);
            if (matches.size() > 1) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), subjectId)
                        + "对该服务存在多条可用授权，请先单独处理重复记录");
            }
            if (matches.isEmpty()) {
                AuthServiceAccessDO access = buildAccess(request, null, subjectId);
                serviceAccessMapper.insert(access);
                replaceFields(access.getId(), request.getFieldPermissions());
                created++;
                continue;
            }
            AuthServiceAccessDO existing = matches.get(0);
            AuthServiceAccessDO desired = buildAccess(request, existing.getId(), subjectId);
            if (sameAccessConfig(existing, desired, request.getFieldPermissions())) {
                unchanged++;
                continue;
            }
            applyAccessUpdate(desired, now);
            replaceFields(existing.getId(), request.getFieldPermissions());
            updated++;
        }
        return new BatchAuthorizationResultVO(created, updated, unchanged);
    }

    /** 重复授权提示显示主体名称和可识别的账号/编码，避免只给用户展示数据库编号。 */
    private String subjectDisplayName(AuthorizationSubjectType subjectType, Long subjectId) {
        AuthorizationReferenceDTO subject = referenceService.getSubject(subjectType, subjectId);
        if (subject == null || subject.getName() == null || subject.getName().trim().isEmpty()) {
            return "授权主体（编号 " + subjectId + "）";
        }
        String name = subject.getName().trim();
        String code = subject.getCode();
        if (subjectType != AuthorizationSubjectType.ORG && code != null
                && !code.trim().isEmpty() && !name.equals(code.trim())) {
            return "授权主体“" + name + "（" + code.trim() + "）”";
        }
        return "授权主体“" + name + "”";
    }

    private List<AuthServiceAccessDO> findCurrentAccesses(ServiceAuthorizationSaveReqDTO request, Long subjectId, Date now) {
        return serviceAccessMapper.selectList(new LambdaQueryWrapper<AuthServiceAccessDO>()
                .eq(AuthServiceAccessDO::getSubjectType, request.getSubjectType().name())
                .eq(AuthServiceAccessDO::getSubjectId, subjectId)
                .eq(AuthServiceAccessDO::getServiceId, request.getServiceId())
                .in(AuthServiceAccessDO::getStatus, STATUS_ENABLED, AuthorizationStatus.ENABLED.name())
                .eq(AuthServiceAccessDO::getValidFlag, "1")
                .and(query -> query.isNull(AuthServiceAccessDO::getExpireTime)
                        .or().ge(AuthServiceAccessDO::getExpireTime, now)));
    }

    private void applyAccessUpdate(AuthServiceAccessDO access, Date now) {
        int updated = serviceAccessMapper.update(null, new LambdaUpdateWrapper<AuthServiceAccessDO>()
                .eq(AuthServiceAccessDO::getId, access.getId())
                .set(AuthServiceAccessDO::getSubjectType, access.getSubjectType())
                .set(AuthServiceAccessDO::getSubjectId, access.getSubjectId())
                .set(AuthServiceAccessDO::getServiceId, access.getServiceId())
                .set(AuthServiceAccessDO::getDataScopeType, access.getDataScopeType())
                .set(AuthServiceAccessDO::getDataScopeConfig, access.getDataScopeConfig())
                .set(AuthServiceAccessDO::getOperationScope, access.getOperationScope())
                .set(AuthServiceAccessDO::getEffectiveTime, access.getEffectiveTime())
                .set(AuthServiceAccessDO::getExpireTime, access.getExpireTime())
                .set(AuthServiceAccessDO::getStatus, STATUS_ENABLED)
                .set(AuthServiceAccessDO::getValidFlag, "1")
                .set(AuthServiceAccessDO::getRevokeTime, null)
                .set(AuthServiceAccessDO::getRevokeBy, null)
                .set(AuthServiceAccessDO::getUpdateTime, now));
        if (updated != 1) {
            throw new ServiceException("服务授权修改失败，请刷新后重试");
        }
    }

    private boolean sameAccessConfig(AuthServiceAccessDO existing, AuthServiceAccessDO desired,
                                     List<ServiceAuthorizationSaveReqDTO.FieldPermissionDTO> fields) {
        // 旧服务授权若有逐操作配置，重新授权时需清除。
        if (!Objects.equals(existing.getDataScopeType(), desired.getDataScopeType())
                || !Objects.equals(existing.getDataScopeConfig(), desired.getDataScopeConfig())
                || !Objects.equals(existing.getOperationScope(), desired.getOperationScope())
                || !Objects.equals(existing.getEffectiveTime(), desired.getEffectiveTime())
                || !Objects.equals(existing.getExpireTime(), desired.getExpireTime())) {
            return false;
        }
        // 旧服务授权若仍保存了编辑标记，视为配置不同并在重新授权时清除。
        Set<String> saved = getFields(existing.getId()).stream()
                .map(field -> field.getModelFieldId() + ":" + field.getViewFlag() + ":" + field.getEditFlag())
                .collect(Collectors.toSet());
        Set<String> requested = fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.getView()))
                .map(field -> field.getFieldId() + ":1:0")
                .collect(Collectors.toSet());
        return saved.equals(requested);
    }

    @Override
    public void revoke(Long id, String operator) {
        AuthServiceAccessDO existing = requireAccess(id);
        if (AuthorizationStatus.REVOKED.matches(existing.getStatus()) || "0".equals(existing.getValidFlag())) {
            throw new ServiceException("该服务授权已失效，不能重复撤权");
        }
        AuthServiceAccessDO access = new AuthServiceAccessDO();
        access.setId(id);
        access.setStatus(STATUS_REVOKED);
        access.setValidFlag("0");
        access.setRevokeTime(new java.util.Date());
        access.setRevokeBy(operator);
        serviceAccessMapper.updateById(access);
    }

    @Override
    public void revokeBatch(List<Long> ids, String operator) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择需要撤销的服务授权");
        }
        ids.stream().filter(java.util.Objects::nonNull).distinct().forEach(id -> revoke(id, operator));
    }

    @Override
    public void deleteBatch(List<Long> ids, String operator) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("请选择需要删除的服务授权");
        }
        List<Long> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
        List<AuthServiceAccessDO> accesses = new ArrayList<>();
        for (Long id : uniqueIds) {
            accesses.add(requireAccess(id));
        }

        // 先标记撤权再逻辑删除：删除后的授权立即失效，并保留撤权人和时间供审计追溯。
        for (AuthServiceAccessDO access : accesses) {
            Long id = access.getId();
            if (!AuthorizationStatus.REVOKED.matches(access.getStatus())) {
                AuthServiceAccessDO revoked = new AuthServiceAccessDO();
                revoked.setId(id);
                revoked.setStatus(STATUS_REVOKED);
                revoked.setValidFlag("0");
                revoked.setRevokeTime(new Date());
                revoked.setRevokeBy(operator);
                if (serviceAccessMapper.updateById(revoked) != 1) {
                    throw new ServiceException("服务授权删除失败，请刷新后重试");
                }
            }
            grantFieldMapper.delete(new LambdaQueryWrapper<AuthGrantFieldDO>()
                    .eq(AuthGrantFieldDO::getGrantId, id)
                    .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_SERVICE));
            if (serviceAccessMapper.deleteById(id) != 1) {
                throw new ServiceException("服务授权删除失败，请刷新后重试");
            }
        }
    }

    @Override
    public PageResult<ServiceAuthorizationRespVO> page(ServiceAuthorizationPageReqVO request) {
        Date now = new Date();
        LambdaQueryWrapperX<AuthServiceAccessDO> query = new LambdaQueryWrapperX<>();
        // 构建列表筛选条件：按主体、服务和创建时间过滤。
        // 条件方法返回父类，因此逐条调用以保留扩展查询包装器类型。
        query.eq(request.getSubjectType() != null, AuthServiceAccessDO::getSubjectType,
                request.getSubjectType() == null ? null : request.getSubjectType().name());
        query.eq(request.getSubjectId() != null, AuthServiceAccessDO::getSubjectId,
                request.getSubjectId());
        query.eq(request.getServiceId() != null, AuthServiceAccessDO::getServiceId,
                request.getServiceId());
        if (request.getServiceIds() != null) {
            if (request.getServiceIds().isEmpty()) {
                return PageResult.empty();
            }
            query.in(AuthServiceAccessDO::getServiceId, request.getServiceIds());
        }
        query.ge(request.getCreateTimeStart() != null, AuthServiceAccessDO::getCreateTime,
                request.getCreateTimeStart());
        query.le(request.getCreateTimeEnd() != null, AuthServiceAccessDO::getCreateTime,
                request.getCreateTimeEnd());
        appendDisplayStatusCondition(query, request.getDisplayStatus(), now);
        // 表格排序：仅白名单字段生效；未传排序参数时保持按编号倒序的原行为。
        if (request.getOrderByColumn() != null && !request.getOrderByColumn().trim().isEmpty()) {
            query.orderBy(request.getOrderByColumn(), request.getIsAsc(), ALLOWED_SORT_COLUMNS);
        } else {
            query.orderByDesc(AuthServiceAccessDO::getId);
        }

        PageResult<AuthServiceAccessDO> page = serviceAccessMapper.selectPage(request, query);
        Map<Long, List<ServiceAuthorizationRespVO.FieldPermissionVO>> fields = loadFieldPermissions(page.getRows());
        Map<Long, AuthorizationReferenceDTO> services = referenceService.getServices(page.getRows().stream()
                .map(AuthServiceAccessDO::getServiceId).collect(Collectors.toSet()));
        Map<String, AuthorizationReferenceDTO> subjects = new HashMap<>();
        for (AuthServiceAccessDO item : page.getRows()) {
            AuthorizationSubjectType type = parseSubjectType(item.getSubjectType());
            String key = type.name() + ":" + item.getSubjectId();
            subjects.computeIfAbsent(key, ignored -> referenceService.getSubject(type, item.getSubjectId()));
        }
        Map<Long, AuthorizationReferenceFieldDTO> fieldMetadata = referenceService.getServiceFields(fields.values()
                .stream().flatMap(List::stream).map(ServiceAuthorizationRespVO.FieldPermissionVO::getFieldId)
                .collect(Collectors.toSet()));
        List<ServiceAuthorizationRespVO> rows = page.getRows().stream()
                .map(item -> toResponse(item, fields.getOrDefault(item.getId(), Collections.emptyList()),
                        subjects.get(parseSubjectType(item.getSubjectType()).name() + ":" + item.getSubjectId()),
                        services.get(item.getServiceId()), fieldMetadata, now))
                .collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    /**
     * 复用授权查询逻辑，按数据服务 ID 获取全部授权记录。
     *
     * @param serviceId 数据服务 ID
     * @return 授权记录列表
     */
    @Override
    public List<ServiceAuthorizationRespVO> listByServiceId(Long serviceId) {
        if (serviceId == null) {
            throw new ServiceException("缺少数据服务 ID");
        }
        ServiceAuthorizationPageReqVO request = new ServiceAuthorizationPageReqVO();
        request.setServiceId(serviceId);
        request.setPageSize(PageParam.PAGE_SIZE_NONE);
        return page(request).getRows();
    }

    @Override
    public ServiceAuthorizationRespVO getDetail(Long id) {
        AuthServiceAccessDO access = requireAccess(id);
        List<ServiceAuthorizationRespVO.FieldPermissionVO> fields = toFieldPermissions(getFields(id));
        AuthorizationReferenceDTO service = referenceService.getServices(Collections.singleton(access.getServiceId()))
                .get(access.getServiceId());
        AuthorizationSubjectType subjectType = parseSubjectType(access.getSubjectType());
        AuthorizationReferenceDTO subject = referenceService.getSubject(subjectType, access.getSubjectId());
        Map<Long, AuthorizationReferenceFieldDTO> fieldMetadata = referenceService.getServiceFields(fields.stream()
                .map(ServiceAuthorizationRespVO.FieldPermissionVO::getFieldId).collect(Collectors.toSet()));
        return toResponse(access, fields, subject, service, fieldMetadata, new Date());
    }

    @Override
    public List<AuthServiceAccessDO> list() {
        return serviceAccessMapper.selectList(new LambdaQueryWrapper<AuthServiceAccessDO>().orderByDesc(AuthServiceAccessDO::getId));
    }

    @Override
    public AuthServiceAccessDO get(Long id) {
        return serviceAccessMapper.selectById(id);
    }

    @Override
    public List<AuthGrantFieldDO> getFields(Long serviceAccessId) {
        return grantFieldMapper.selectList(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .eq(AuthGrantFieldDO::getGrantId, serviceAccessId)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_SERVICE)
                .orderByAsc(AuthGrantFieldDO::getId));
    }

    private Map<Long, List<ServiceAuthorizationRespVO.FieldPermissionVO>> loadFieldPermissions(
            List<AuthServiceAccessDO> accesses) {
        if (accesses == null || accesses.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> accessIds = accesses.stream().map(AuthServiceAccessDO::getId).collect(Collectors.toList());
        List<AuthGrantFieldDO> fields = grantFieldMapper.selectList(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .in(AuthGrantFieldDO::getGrantId, accessIds)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_SERVICE)
                .orderByAsc(AuthGrantFieldDO::getId));
        Map<Long, List<ServiceAuthorizationRespVO.FieldPermissionVO>> result = new HashMap<>();
        for (AuthGrantFieldDO field : fields) {
            result.computeIfAbsent(field.getGrantId(), key -> new ArrayList<>()).add(toFieldPermission(field));
        }
        return result;
    }

    private List<ServiceAuthorizationRespVO.FieldPermissionVO> toFieldPermissions(List<AuthGrantFieldDO> fields) {
        return fields.stream().map(this::toFieldPermission).collect(Collectors.toList());
    }

    private ServiceAuthorizationRespVO.FieldPermissionVO toFieldPermission(AuthGrantFieldDO field) {
        ServiceAuthorizationRespVO.FieldPermissionVO response = new ServiceAuthorizationRespVO.FieldPermissionVO();
        response.setFieldId(field.getModelFieldId());
        response.setView("1".equals(field.getViewFlag()));
        return response;
    }

    private ServiceAuthorizationRespVO toResponse(AuthServiceAccessDO access,
                                                   List<ServiceAuthorizationRespVO.FieldPermissionVO> fields,
                                                   AuthorizationReferenceDTO subject,
                                                   AuthorizationReferenceDTO service,
                                                   Map<Long, AuthorizationReferenceFieldDTO> fieldMetadata,
                                                   Date now) {
        ServiceAuthorizationRespVO response = new ServiceAuthorizationRespVO();
        response.setId(access.getId());
        AuthorizationSubjectType subjectType = parseSubjectType(access.getSubjectType());
        response.setSubjectType(subjectType);
        response.setSubjectId(access.getSubjectId());
        if (subject != null) {
            response.setSubjectCode(subject.getCode());
            response.setSubjectName(subject.getName());
            response.setSubjectDescription(subject.getDescription());
        }
        response.setServiceId(access.getServiceId());
        if (service != null) {
            response.setServiceCode(service.getCode());
            response.setServiceName(service.getName());
            response.setServiceVersion(service.getVersion());
        }
        response.setDataScopeType(parseDataScopeType(access.getDataScopeType()));
        response.setDataScopeConfig(jsonCodec.readDataScope(access.getDataScopeConfig()));
        for (ServiceAuthorizationRespVO.FieldPermissionVO field : fields) {
            AuthorizationReferenceFieldDTO metadata = fieldMetadata.get(field.getFieldId());
            if (metadata != null) {
                field.setFieldCode(metadata.getCode());
                field.setFieldName(metadata.getName());
            }
        }
        response.setFieldPermissions(fields);
        response.setViewFieldCount((int) fields.stream().filter(item -> Boolean.TRUE.equals(item.getView())).count());
        response.setEffectiveTime(access.getEffectiveTime());
        response.setExpireTime(access.getExpireTime());
        response.setLongTerm(access.getExpireTime() == null);
        response.setPersistedStatus(access.getStatus());
        response.setRevokeTime(access.getRevokeTime());
        response.setRevokeBy(access.getRevokeBy());
        response.setCreateBy(access.getCreateBy());
        response.setCreateTime(access.getCreateTime());
        response.setUpdateBy(access.getUpdateBy());
        response.setUpdateTime(access.getUpdateTime());
        response.setRemark(access.getRemark());
        fillDisplayStatus(response, access, now);
        return response;
    }

    private void fillDisplayStatus(ServiceAuthorizationRespVO response, AuthServiceAccessDO access, Date now) {
        response.setDisplayStatus("INACTIVE");
        if (AuthorizationStatus.REVOKED.matches(access.getStatus())) {
            response.setInactiveReason("REVOKED");
        } else if (!"1".equals(access.getValidFlag())) {
            response.setInactiveReason("INVALID");
        } else if (access.getEffectiveTime() != null && now.before(access.getEffectiveTime())) {
            response.setInactiveReason("NOT_STARTED");
        } else if (access.getExpireTime() != null && now.after(access.getExpireTime())) {
            response.setInactiveReason("EXPIRED");
        } else {
            response.setDisplayStatus("EFFECTIVE");
            response.setInactiveReason(null);
        }
    }

    private void appendDisplayStatusCondition(LambdaQueryWrapper<AuthServiceAccessDO> query,
                                              String status,
                                              Date now) {
        if (status == null || status.trim().isEmpty()) {
            return;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("EFFECTIVE".equals(normalized)) {
            query.in(AuthServiceAccessDO::getStatus, STATUS_ENABLED, AuthorizationStatus.ENABLED.name())
                    .eq(AuthServiceAccessDO::getValidFlag, "1")
                    .and(item -> item.isNull(AuthServiceAccessDO::getEffectiveTime)
                            .or().le(AuthServiceAccessDO::getEffectiveTime, now))
                    .and(item -> item.isNull(AuthServiceAccessDO::getExpireTime)
                            .or().ge(AuthServiceAccessDO::getExpireTime, now));
        } else if ("INACTIVE".equals(normalized)) {
            query.and(item -> item.notIn(AuthServiceAccessDO::getStatus, STATUS_ENABLED, AuthorizationStatus.ENABLED.name())
                    .or().ne(AuthServiceAccessDO::getValidFlag, "1")
                    .or().gt(AuthServiceAccessDO::getEffectiveTime, now)
                    .or(expire -> expire.isNotNull(AuthServiceAccessDO::getExpireTime)
                            .lt(AuthServiceAccessDO::getExpireTime, now)));
        } else {
            throw new ServiceException("授权状态只能选择有效或未生效");
        }
    }

    private AuthorizationSubjectType parseSubjectType(String value) {
        try {
            return AuthorizationSubjectType.valueOf(value == null
                    ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("授权主体类型数据错误");
        }
    }

    private DataScopeType parseDataScopeType(String value) {
        try {
            return DataScopeType.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("数据范围类型数据错误");
        }
    }

    private AuthServiceAccessDO buildAccess(ServiceAuthorizationSaveReqDTO request, Long id, Long subjectId) {
        AuthServiceAccessDO access = new AuthServiceAccessDO();
        access.setId(id);
        access.setSubjectType(request.getSubjectType().name());
        access.setSubjectId(subjectId);
        access.setServiceId(request.getServiceId());
        access.setDataScopeType(request.getDataScopeType().name());
        access.setDataScopeConfig(request.getDataScopeType() == DataScopeType.ALL
                ? null : jsonCodec.writeDataScope(request.getDataScopeConfig()));
        // 保留数据库字段的空数组格式以兼容旧表结构，不再保存逐操作权限。
        access.setOperationScope(jsonCodec.writeOperations(Collections.emptyList()));
        access.setEffectiveTime(request.getEffectiveTime());
        access.setExpireTime(Boolean.TRUE.equals(request.getLongTerm()) ? null : request.getExpireTime());
        if (id == null) {
            access.setStatus(STATUS_ENABLED);
            access.setValidFlag("1");
            access.setDelFlag("0");
        }
        return access;
    }

    private void replaceFields(Long serviceAccessId, List<ServiceAuthorizationSaveReqDTO.FieldPermissionDTO> fields) {
        grantFieldMapper.delete(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .eq(AuthGrantFieldDO::getGrantId, serviceAccessId)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_SERVICE));
        for (ServiceAuthorizationSaveReqDTO.FieldPermissionDTO field : fields) {
            if (!Boolean.TRUE.equals(field.getView())) {
                continue;
            }
            AuthGrantFieldDO item = new AuthGrantFieldDO();
            item.setGrantId(serviceAccessId);
            item.setAuthType(AUTH_TYPE_SERVICE);
            item.setModelFieldId(field.getFieldId());
            item.setViewFlag(Boolean.TRUE.equals(field.getView()) ? "1" : "0");
            item.setEditFlag("0");
            item.setValidFlag("1");
            item.setDelFlag("0");
            grantFieldMapper.insert(item);
        }
    }

    private void validate(ServiceAuthorizationSaveReqDTO request) {
        if (request.getSubjectType() == null || getSubjectIds(request).isEmpty()) {
            throw new ServiceException("请选择授权主体");
        }
        if (request.getSubjectType() != AuthorizationSubjectType.USER
                && request.getSubjectType() != AuthorizationSubjectType.CLIENT) {
            throw new ServiceException("服务授权主体只支持平台用户或服务客户端");
        }
        if (request.getSubjectType() == AuthorizationSubjectType.USER
                || referenceService.serviceResourceApiAvailable()) {
            for (Long subjectId : getSubjectIds(request)) {
                if (referenceService.getSubject(request.getSubjectType(), subjectId) == null) {
                    throw new ServiceException("授权主体不存在：" + subjectId);
                }
            }
        }
        if (request.getServiceId() == null) {
            throw new ServiceException("请选择数据服务");
        }
        if (referenceService.serviceResourceApiAvailable()
                && !referenceService.getServices(Collections.singleton(request.getServiceId()))
                .containsKey(request.getServiceId())) {
            throw new ServiceException("数据服务不存在");
        }
        if (request.getDataScopeType() == null) {
            throw new ServiceException("数据范围必须选择全部数据或条件授权");
        }
        dataScopeValidator.validate(request.getDataScopeType(), request.getDataScopeConfig());
        if (request.getFieldPermissions() == null || request.getFieldPermissions().isEmpty()) {
            throw new ServiceException("至少配置一个字段权限");
        }
        if (request.getFieldPermissions().stream().noneMatch(field -> Boolean.TRUE.equals(field.getView()))) {
            throw new ServiceException("至少选择一个可查看字段");
        }
        if (!Boolean.TRUE.equals(request.getLongTerm()) && request.getExpireTime() == null) {
            throw new ServiceException("请设置失效时间或选择长期有效");
        }
        if (request.getEffectiveTime() == null) {
            throw new ServiceException("请设置生效时间");
        }
        if (!Boolean.TRUE.equals(request.getLongTerm()) && !request.getExpireTime().after(request.getEffectiveTime())) {
            throw new ServiceException("失效时间必须晚于生效时间");
        }
        for (ServiceAuthorizationSaveReqDTO.FieldPermissionDTO field : request.getFieldPermissions()) {
            if (field.getFieldId() == null) {
                throw new ServiceException("字段权限缺少字段标识");
            }
        }
        validateServiceFields(request);
    }

    private void validateServiceFields(ServiceAuthorizationSaveReqDTO request) {
        if (!referenceService.serviceResourceApiAvailable()) {
            return;
        }
        Set<Long> fieldIds = request.getFieldPermissions().stream()
                .map(ServiceAuthorizationSaveReqDTO.FieldPermissionDTO::getFieldId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (request.getDataScopeConfig() != null && request.getDataScopeConfig().getGroups() != null) {
            request.getDataScopeConfig().getGroups().stream()
                    .filter(Objects::nonNull)
                    .filter(group -> group.getConditions() != null)
                    .flatMap(group -> group.getConditions().stream())
                    .filter(Objects::nonNull)
                    .map(condition -> condition.getFieldId())
                    .filter(Objects::nonNull)
                    .forEach(fieldIds::add);
        }
        Map<Long, AuthorizationReferenceFieldDTO> fields = referenceService.getServiceFields(fieldIds);
        if (fields.size() != fieldIds.size()) {
            throw new ServiceException("字段权限或数据范围中存在无效服务字段");
        }
        boolean wrongService = fields.values().stream()
                .anyMatch(field -> !request.getServiceId().equals(field.getServiceId()));
        if (wrongService) {
            throw new ServiceException("字段不属于所选数据服务");
        }
    }

    private List<Long> getSubjectIds(ServiceAuthorizationSaveReqDTO request) {
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            return request.getSubjectIds().stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        }
        return request.getSubjectId() == null ? Collections.emptyList() : Collections.singletonList(request.getSubjectId());
    }

    private AuthServiceAccessDO requireAccess(Long id) {
        AuthServiceAccessDO access = serviceAccessMapper.selectById(id);
        if (access == null) {
            throw new ServiceException("服务授权不存在或已删除");
        }
        return access;
    }
}
