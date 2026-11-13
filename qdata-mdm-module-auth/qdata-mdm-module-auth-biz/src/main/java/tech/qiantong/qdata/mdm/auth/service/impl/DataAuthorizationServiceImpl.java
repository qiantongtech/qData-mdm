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
import tech.qiantong.qdata.mdm.auth.api.dataauthorization.dto.DataAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationOperation;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationSubjectType;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationStatus;
import tech.qiantong.qdata.mdm.auth.api.enums.AuthorizationType;
import tech.qiantong.qdata.mdm.auth.api.enums.DataScopeType;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationModelFieldDTO;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.AuthorizationFieldPermissionRespVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.auth.dal.mapper.AuthGrantFieldMapper;
import tech.qiantong.qdata.mdm.auth.dal.mapper.AuthGrantMapper;
import tech.qiantong.qdata.mdm.auth.reference.AuthorizationReferenceService;
import tech.qiantong.qdata.mdm.auth.service.DataAuthorizationService;
import tech.qiantong.qdata.mdm.auth.support.AuthorizationJsonCodec;
import tech.qiantong.qdata.mdm.auth.support.DataScopeValidator;
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
import java.util.UUID;
import java.util.stream.Collectors;

/** 数据授权的保存、查询和撤权。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DataAuthorizationServiceImpl implements DataAuthorizationService {
    private static final String AUTH_TYPE_DATA = AuthorizationType.DATA.name();
    private static final String STATUS_NORMAL = AuthorizationStatus.ENABLED.getCode();
    private static final String STATUS_REVOKED = AuthorizationStatus.REVOKED.getCode();
    /** 列表排序白名单：只允许这些下划线列名参与 ORDER BY，避免排序字段注入。 */
    private static final Set<String> ALLOWED_SORT_COLUMNS = new HashSet<>(Arrays.asList("id", "create_time"));

    @Resource
    private AuthGrantMapper grantMapper;
    @Resource
    private AuthGrantFieldMapper grantFieldMapper;
    @Resource
    private AuthorizationJsonCodec jsonCodec;
    @Resource
    private DataScopeValidator dataScopeValidator;
    @Resource
    private AuthorizationReferenceService referenceService;

    @Override
    public Long save(DataAuthorizationSaveReqDTO request) {
        validate(request);
        if (request.getId() != null) {
            AuthGrantDO previous = requireGrant(request.getId());
            if (getSubjectIds(request).size() != 1) {
                throw new ServiceException("编辑授权时只能选择一个授权主体");
            }
            if (findCurrentGrants(request, getSubjectIds(request).get(0), new Date()).stream()
                    .anyMatch(item -> !request.getId().equals(item.getId()))) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), getSubjectIds(request).get(0))
                        + "对该主数据模型已有其他授权，请勿重复配置");
            }
            AuthGrantDO grant = buildGrant(request, request.getId(), getSubjectIds(request).get(0));
            if (AuthorizationStatus.REVOKED.matches(previous.getStatus()) && grant.getExpireTime() != null
                    && !grant.getExpireTime().after(new Date())) {
                throw new ServiceException("恢复授权时请设置晚于当前时间的失效时间");
            }
            applyGrantUpdate(grant, new Date());
            replaceFields(grant.getId(), request.getFieldPermissions());
            return grant.getId();
        }

        Long firstId = null;
        for (Long subjectId : getSubjectIds(request)) {
            if (!findCurrentGrants(request, subjectId, new Date()).isEmpty()) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), subjectId)
                        + "对该主数据模型已有授权，请使用修改授权或批量授权");
            }
            AuthGrantDO grant = buildGrant(request, null, subjectId);
            grantMapper.insert(grant);
            replaceFields(grant.getId(), request.getFieldPermissions());
            if (firstId == null) {
                firstId = grant.getId();
            }
        }
        return firstId;
    }

    @Override
    public BatchAuthorizationResultVO saveBatch(DataAuthorizationSaveReqDTO request) {
        if (request.getId() != null) {
            throw new ServiceException("批量授权不能指定单条授权编号");
        }
        validate(request);
        int created = 0;
        int updated = 0;
        int unchanged = 0;
        Date now = new Date();
        for (Long subjectId : getSubjectIds(request)) {
            List<AuthGrantDO> matches = findCurrentGrants(request, subjectId, now);
            if (matches.size() > 1) {
                throw new ServiceException(subjectDisplayName(request.getSubjectType(), subjectId)
                        + "对该模型存在多条可用授权，请先单独处理重复记录");
            }
            if (matches.isEmpty()) {
                AuthGrantDO grant = buildGrant(request, null, subjectId);
                grantMapper.insert(grant);
                replaceFields(grant.getId(), request.getFieldPermissions());
                created++;
                continue;
            }
            AuthGrantDO existing = matches.get(0);
            AuthGrantDO desired = buildGrant(request, existing.getId(), subjectId);
            if (sameGrantConfig(existing, desired, request.getFieldPermissions())) {
                unchanged++;
                continue;
            }
            applyGrantUpdate(desired, now);
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

    private List<AuthGrantDO> findCurrentGrants(DataAuthorizationSaveReqDTO request, Long subjectId, Date now) {
        return grantMapper.selectList(new LambdaQueryWrapper<AuthGrantDO>()
                .eq(AuthGrantDO::getSubjectType, request.getSubjectType().name())
                .eq(AuthGrantDO::getSubjectId, subjectId)
                .eq(AuthGrantDO::getModelId, request.getModelId())
                .in(AuthGrantDO::getStatus, STATUS_NORMAL, AuthorizationStatus.ENABLED.name())
                .eq(AuthGrantDO::getValidFlag, "1")
                .and(query -> query.isNull(AuthGrantDO::getExpireTime)
                        .or().ge(AuthGrantDO::getExpireTime, now)));
    }

    private void applyGrantUpdate(AuthGrantDO grant, Date now) {
        int updated = grantMapper.update(null, new LambdaUpdateWrapper<AuthGrantDO>()
                .eq(AuthGrantDO::getId, grant.getId())
                .set(AuthGrantDO::getSubjectType, grant.getSubjectType())
                .set(AuthGrantDO::getSubjectId, grant.getSubjectId())
                .set(AuthGrantDO::getModelId, grant.getModelId())
                .set(AuthGrantDO::getRemark, grant.getRemark())
                .set(AuthGrantDO::getDataScopeType, grant.getDataScopeType())
                .set(AuthGrantDO::getDataScopeConfig, grant.getDataScopeConfig())
                .set(AuthGrantDO::getIncludeChildOrgFlag, grant.getIncludeChildOrgFlag())
                .set(AuthGrantDO::getOperationScope, grant.getOperationScope())
                .set(AuthGrantDO::getEffectiveTime, grant.getEffectiveTime())
                .set(AuthGrantDO::getExpireTime, grant.getExpireTime())
                .set(AuthGrantDO::getStatus, STATUS_NORMAL)
                .set(AuthGrantDO::getValidFlag, "1")
                .set(AuthGrantDO::getRevokeTime, null)
                .set(AuthGrantDO::getRevokeBy, null)
                .set(AuthGrantDO::getUpdateTime, now));
        if (updated != 1) {
            throw new ServiceException("数据授权修改失败，请刷新后重试");
        }
    }

    private boolean sameGrantConfig(AuthGrantDO existing, AuthGrantDO desired,
                                    List<DataAuthorizationSaveReqDTO.FieldPermissionDTO> fields) {
        if (!Objects.equals(existing.getRemark(), desired.getRemark())
                || !Objects.equals(existing.getDataScopeType(), desired.getDataScopeType())
                || !Objects.equals(existing.getDataScopeConfig(), desired.getDataScopeConfig())
                || !Objects.equals(existing.getIncludeChildOrgFlag(), desired.getIncludeChildOrgFlag())
                || !Objects.equals(existing.getOperationScope(), desired.getOperationScope())
                || !Objects.equals(existing.getEffectiveTime(), desired.getEffectiveTime())
                || !Objects.equals(existing.getExpireTime(), desired.getExpireTime())) {
            return false;
        }
        Set<String> saved = getFields(existing.getId()).stream()
                .map(field -> field.getModelFieldId() + ":" + field.getViewFlag() + ":" + field.getEditFlag())
                .collect(Collectors.toSet());
        Set<String> requested = fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.getView()) || Boolean.TRUE.equals(field.getEdit()))
                .map(field -> field.getFieldId() + ":" + (Boolean.TRUE.equals(field.getView()) ? "1" : "0")
                        + ":" + (Boolean.TRUE.equals(field.getEdit()) ? "1" : "0"))
                .collect(Collectors.toSet());
        return saved.equals(requested);
    }

    @Override
    public void revoke(Long id, String operator) {
        AuthGrantDO existing = requireGrant(id);
        if (AuthorizationStatus.REVOKED.matches(existing.getStatus()) || "0".equals(existing.getValidFlag())) {
            throw new ServiceException("该数据授权已失效，不能重复撤权");
        }
        AuthGrantDO grant = new AuthGrantDO();
        grant.setId(id);
        grant.setStatus(STATUS_REVOKED);
        grant.setValidFlag("0");
        grant.setRevokeTime(new java.util.Date());
        grant.setRevokeBy(operator);
        grantMapper.updateById(grant);
    }

    @Override
    public void revokeBatch(List<Long> ids, String operator) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("请选择需要撤销的数据授权");
        }
        ids.stream().filter(java.util.Objects::nonNull).distinct().forEach(id -> revoke(id, operator));
    }

    @Override
    public void deleteBatch(List<Long> ids, String operator) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("请选择需要删除的数据授权");
        }
        List<Long> uniqueIds = ids.stream().distinct().collect(Collectors.toList());
        List<AuthGrantDO> grants = new ArrayList<>();
        for (Long id : uniqueIds) {
            grants.add(requireGrant(id));
        }

        // 先标记撤权再逻辑删除：删除后的授权立即失效，并保留撤权人和时间供审计追溯。
        for (AuthGrantDO grant : grants) {
            Long id = grant.getId();
            if (!AuthorizationStatus.REVOKED.matches(grant.getStatus())) {
                AuthGrantDO revoked = new AuthGrantDO();
                revoked.setId(id);
                revoked.setStatus(STATUS_REVOKED);
                revoked.setValidFlag("0");
                revoked.setRevokeTime(new Date());
                revoked.setRevokeBy(operator);
                if (grantMapper.updateById(revoked) != 1) {
                    throw new ServiceException("数据授权删除失败，请刷新后重试");
                }
            }
            grantFieldMapper.delete(new LambdaQueryWrapper<AuthGrantFieldDO>()
                    .eq(AuthGrantFieldDO::getGrantId, id)
                    .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_DATA));
            if (grantMapper.deleteById(id) != 1) {
                throw new ServiceException("数据授权删除失败，请刷新后重试");
            }
        }
    }

    @Override
    public PageResult<DataAuthorizationRespVO> page(DataAuthorizationPageReqVO request) {
        Date now = new Date();
        LambdaQueryWrapperX<AuthGrantDO> query = new LambdaQueryWrapperX<>();
        // 构建列表筛选条件：按主体、模型和创建时间过滤。
        // 条件方法返回父类，因此逐条调用以保留扩展查询包装器类型。
        query.eq(request.getSubjectType() != null, AuthGrantDO::getSubjectType,
                request.getSubjectType() == null ? null : request.getSubjectType().name());
        query.eq(request.getSubjectId() != null, AuthGrantDO::getSubjectId, request.getSubjectId());
        query.eq(request.getModelId() != null, AuthGrantDO::getModelId, request.getModelId());
        if (request.getModelIds() != null) {
            if (request.getModelIds().isEmpty()) {
                return PageResult.empty();
            }
            query.in(AuthGrantDO::getModelId, request.getModelIds());
        }
        query.ge(request.getCreateTimeStart() != null, AuthGrantDO::getCreateTime,
                request.getCreateTimeStart());
        query.le(request.getCreateTimeEnd() != null, AuthGrantDO::getCreateTime,
                request.getCreateTimeEnd());
        appendDisplayStatusCondition(query, request.getDisplayStatus(), now);
        // 表格排序：仅白名单字段生效；未传排序参数时保持按编号倒序的原行为。
        if (request.getOrderByColumn() != null && !request.getOrderByColumn().trim().isEmpty()) {
            query.orderBy(request.getOrderByColumn(), request.getIsAsc(), ALLOWED_SORT_COLUMNS);
        } else {
            query.orderByDesc(AuthGrantDO::getId);
        }

        PageResult<AuthGrantDO> page = grantMapper.selectPage(request, query);
        Map<Long, List<AuthorizationFieldPermissionRespVO>> fields = loadFieldPermissions(page.getRows());
        Map<Long, AuthorizationReferenceDTO> models = referenceService.getModels(page.getRows().stream()
                .map(AuthGrantDO::getModelId).collect(Collectors.toSet()));
        Map<String, AuthorizationReferenceDTO> subjects = new HashMap<>();
        for (AuthGrantDO item : page.getRows()) {
            AuthorizationSubjectType type = parseSubjectType(item.getSubjectType());
            String key = type.name() + ":" + item.getSubjectId();
            subjects.computeIfAbsent(key, ignored -> referenceService.getSubject(type, item.getSubjectId()));
        }
        Set<Long> fieldIds = fields.values().stream().flatMap(List::stream)
                .map(AuthorizationFieldPermissionRespVO::getFieldId).collect(Collectors.toSet());
        Map<Long, AuthorizationModelFieldDTO> fieldMetadata = referenceService.getModelFields(fieldIds);
        List<DataAuthorizationRespVO> rows = page.getRows().stream()
                .map(item -> toResponse(item, fields.getOrDefault(item.getId(), Collections.emptyList()),
                        subjects.get(parseSubjectType(item.getSubjectType()).name() + ":" + item.getSubjectId()),
                        models.get(item.getModelId()), fieldMetadata, now))
                .collect(Collectors.toList());
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    public DataAuthorizationRespVO getDetail(Long id) {
        AuthGrantDO grant = requireGrant(id);
        List<AuthorizationFieldPermissionRespVO> fields = toFieldPermissions(getFields(id));
        AuthorizationReferenceDTO model = referenceService.getModels(Collections.singleton(grant.getModelId()))
                .get(grant.getModelId());
        AuthorizationSubjectType subjectType = parseSubjectType(grant.getSubjectType());
        AuthorizationReferenceDTO subject = referenceService.getSubject(subjectType, grant.getSubjectId());
        Map<Long, AuthorizationModelFieldDTO> fieldMetadata = referenceService.getModelFields(fields.stream()
                .map(AuthorizationFieldPermissionRespVO::getFieldId).collect(Collectors.toSet()));
        return toResponse(grant, fields, subject, model, fieldMetadata, new Date());
    }

    @Override
    public List<AuthGrantDO> list() {
        return grantMapper.selectList(new LambdaQueryWrapper<AuthGrantDO>().orderByDesc(AuthGrantDO::getId));
    }

    @Override
    public AuthGrantDO get(Long id) {
        return grantMapper.selectById(id);
    }

    @Override
    public List<AuthGrantFieldDO> getFields(Long grantId) {
        return grantFieldMapper.selectList(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .eq(AuthGrantFieldDO::getGrantId, grantId)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_DATA)
                .orderByAsc(AuthGrantFieldDO::getId));
    }

    private Map<Long, List<AuthorizationFieldPermissionRespVO>> loadFieldPermissions(List<AuthGrantDO> grants) {
        if (grants == null || grants.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> grantIds = grants.stream().map(AuthGrantDO::getId).collect(Collectors.toList());
        List<AuthGrantFieldDO> fields = grantFieldMapper.selectList(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .in(AuthGrantFieldDO::getGrantId, grantIds)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_DATA)
                .orderByAsc(AuthGrantFieldDO::getId));
        Map<Long, List<AuthorizationFieldPermissionRespVO>> result = new HashMap<>();
        for (AuthGrantFieldDO field : fields) {
            result.computeIfAbsent(field.getGrantId(), key -> new ArrayList<>()).add(toFieldPermission(field));
        }
        return result;
    }

    private List<AuthorizationFieldPermissionRespVO> toFieldPermissions(List<AuthGrantFieldDO> fields) {
        return fields.stream().map(this::toFieldPermission).collect(Collectors.toList());
    }

    private AuthorizationFieldPermissionRespVO toFieldPermission(AuthGrantFieldDO field) {
        AuthorizationFieldPermissionRespVO response = new AuthorizationFieldPermissionRespVO();
        response.setFieldId(field.getModelFieldId());
        response.setView("1".equals(field.getViewFlag()));
        response.setEdit("1".equals(field.getEditFlag()));
        return response;
    }

    private DataAuthorizationRespVO toResponse(AuthGrantDO grant,
                                                List<AuthorizationFieldPermissionRespVO> fields,
                                                AuthorizationReferenceDTO subject,
                                                AuthorizationReferenceDTO model,
                                                Map<Long, AuthorizationModelFieldDTO> fieldMetadata,
                                                Date now) {
        DataAuthorizationRespVO response = new DataAuthorizationRespVO();
        response.setId(grant.getId());
        response.setGrantNo(grant.getGrantNo());
        response.setGrantName(grant.getGrantName());
        AuthorizationSubjectType subjectType = parseSubjectType(grant.getSubjectType());
        response.setSubjectType(subjectType);
        response.setSubjectId(grant.getSubjectId());
        if (subject != null) {
            response.setSubjectCode(subject.getCode());
            response.setSubjectName(subject.getName());
            response.setSubjectDescription(subject.getDescription());
        }
        response.setModelId(grant.getModelId());
        if (model != null) {
            response.setModelCode(model.getCode());
            response.setModelName(model.getName());
        }
        response.setIncludeChildOrg("1".equals(grant.getIncludeChildOrgFlag()));
        response.setDataScopeType(parseDataScopeType(grant.getDataScopeType()));
        response.setDataScopeConfig(jsonCodec.readDataScope(grant.getDataScopeConfig()));
        for (AuthorizationFieldPermissionRespVO field : fields) {
            AuthorizationModelFieldDTO metadata = fieldMetadata.get(field.getFieldId());
            if (metadata != null) {
                field.setFieldCode(metadata.getCode());
                field.setFieldName(metadata.getName());
            }
        }
        response.setFieldPermissions(fields);
        response.setViewFieldCount((int) fields.stream().filter(item -> Boolean.TRUE.equals(item.getView())).count());
        response.setEditFieldCount((int) fields.stream().filter(item -> Boolean.TRUE.equals(item.getEdit())).count());
        response.setOperations(jsonCodec.readOperations(grant.getOperationScope()));
        response.setEffectiveTime(grant.getEffectiveTime());
        response.setExpireTime(grant.getExpireTime());
        response.setLongTerm(grant.getExpireTime() == null);
        response.setPersistedStatus(grant.getStatus());
        response.setRevokeTime(grant.getRevokeTime());
        response.setRevokeBy(grant.getRevokeBy());
        response.setCreateBy(grant.getCreateBy());
        response.setCreateTime(grant.getCreateTime());
        response.setUpdateBy(grant.getUpdateBy());
        response.setUpdateTime(grant.getUpdateTime());
        response.setRemark(grant.getRemark());
        fillDisplayStatus(response, grant, now);
        return response;
    }

    private void fillDisplayStatus(DataAuthorizationRespVO response, AuthGrantDO grant, Date now) {
        response.setDisplayStatus("INACTIVE");
        if (AuthorizationStatus.REVOKED.matches(grant.getStatus())) {
            response.setInactiveReason("REVOKED");
        } else if (!"1".equals(grant.getValidFlag())) {
            response.setInactiveReason("INVALID");
        } else if (grant.getEffectiveTime() != null && now.before(grant.getEffectiveTime())) {
            response.setInactiveReason("NOT_STARTED");
        } else if (grant.getExpireTime() != null && now.after(grant.getExpireTime())) {
            response.setInactiveReason("EXPIRED");
        } else {
            response.setDisplayStatus("EFFECTIVE");
            response.setInactiveReason(null);
        }
    }

    private void appendDisplayStatusCondition(LambdaQueryWrapper<AuthGrantDO> query, String status, Date now) {
        if (status == null || status.trim().isEmpty()) {
            return;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("EFFECTIVE".equals(normalized)) {
            query.in(AuthGrantDO::getStatus, STATUS_NORMAL, AuthorizationStatus.ENABLED.name())
                    .eq(AuthGrantDO::getValidFlag, "1")
                    .and(item -> item.isNull(AuthGrantDO::getEffectiveTime)
                            .or().le(AuthGrantDO::getEffectiveTime, now))
                    .and(item -> item.isNull(AuthGrantDO::getExpireTime)
                            .or().ge(AuthGrantDO::getExpireTime, now));
        } else if ("INACTIVE".equals(normalized)) {
            query.and(item -> item.notIn(AuthGrantDO::getStatus, STATUS_NORMAL, AuthorizationStatus.ENABLED.name())
                    .or().ne(AuthGrantDO::getValidFlag, "1")
                    .or().gt(AuthGrantDO::getEffectiveTime, now)
                    .or(expire -> expire.isNotNull(AuthGrantDO::getExpireTime)
                            .lt(AuthGrantDO::getExpireTime, now)));
        } else {
            throw new ServiceException("授权状态只能选择有效或未生效");
        }
    }

    private AuthorizationSubjectType parseSubjectType(String value) {
        try {
            String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
            return AuthorizationSubjectType.valueOf("ORGANIZATION".equals(normalized) ? "ORG" : normalized);
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

    private AuthGrantDO buildGrant(DataAuthorizationSaveReqDTO request, Long id, Long subjectId) {
        AuthGrantDO grant = new AuthGrantDO();
        grant.setId(id);
        if (id == null) {
            grant.setGrantNo("AUTH-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
            grant.setGrantName("数据授权-" + subjectId + "-" + request.getModelId());
        }
        grant.setSubjectType(request.getSubjectType().name());
        grant.setSubjectId(subjectId);
        grant.setModelId(request.getModelId());
        grant.setRemark(request.getRemark() == null ? null : request.getRemark().trim());
        grant.setDataScopeType(request.getDataScopeType().name());
        grant.setDataScopeConfig(request.getDataScopeType() == DataScopeType.ALL
                ? null : jsonCodec.writeDataScope(request.getDataScopeConfig()));
        grant.setIncludeChildOrgFlag(Boolean.TRUE.equals(request.getIncludeChildOrg()) ? "1" : "0");
        grant.setOperationScope(jsonCodec.writeOperations(normalizeOperations(request.getOperations())));
        grant.setEffectiveTime(request.getEffectiveTime());
        grant.setExpireTime(Boolean.TRUE.equals(request.getLongTerm()) ? null : request.getExpireTime());
        if (id == null) {
            grant.setStatus(STATUS_NORMAL);
            grant.setValidFlag("1");
            grant.setDelFlag("0");
        }
        return grant;
    }

    private void replaceFields(Long grantId, List<DataAuthorizationSaveReqDTO.FieldPermissionDTO> fields) {
        grantFieldMapper.delete(new LambdaQueryWrapper<AuthGrantFieldDO>()
                .eq(AuthGrantFieldDO::getGrantId, grantId)
                .eq(AuthGrantFieldDO::getAuthType, AUTH_TYPE_DATA));
        for (DataAuthorizationSaveReqDTO.FieldPermissionDTO field : fields) {
            if (!Boolean.TRUE.equals(field.getView()) && !Boolean.TRUE.equals(field.getEdit())) {
                continue;
            }
            AuthGrantFieldDO item = new AuthGrantFieldDO();
            item.setGrantId(grantId);
            item.setAuthType(AUTH_TYPE_DATA);
            item.setModelFieldId(field.getFieldId());
            item.setViewFlag(Boolean.TRUE.equals(field.getView()) ? "1" : "0");
            item.setEditFlag(Boolean.TRUE.equals(field.getEdit()) ? "1" : "0");
            item.setValidFlag("1");
            item.setDelFlag("0");
            grantFieldMapper.insert(item);
        }
    }

    private void validate(DataAuthorizationSaveReqDTO request) {
        if (request.getRemark() != null && request.getRemark().length() > 256) {
            throw new ServiceException("描述不能超过256个字符");
        }
        if (request.getSubjectType() == null || getSubjectIds(request).isEmpty()) {
            throw new ServiceException("请选择授权主体");
        }
        if (request.getSubjectType() == AuthorizationSubjectType.CLIENT) {
            throw new ServiceException("数据授权主体只支持用户、角色或组织");
        }
        for (Long subjectId : getSubjectIds(request)) {
            if (referenceService.getSubject(request.getSubjectType(), subjectId) == null) {
                throw new ServiceException("授权主体不存在：" + subjectId);
            }
        }
        if (request.getModelId() == null) {
            throw new ServiceException("请选择主数据模型");
        }
        if (referenceService.modelResourceApiAvailable()
                && !referenceService.getModels(Collections.singleton(request.getModelId())).containsKey(request.getModelId())) {
            throw new ServiceException("主数据模型不存在");
        }
        if (request.getDataScopeType() == null) {
            throw new ServiceException("数据范围必须选择全部数据或条件授权");
        }
        dataScopeValidator.validate(request.getDataScopeType(), request.getDataScopeConfig());
        if (!Boolean.TRUE.equals(request.getLongTerm()) && request.getExpireTime() == null) {
            throw new ServiceException("请设置失效时间或选择长期有效");
        }
        if (request.getEffectiveTime() == null) {
            throw new ServiceException("请设置生效时间");
        }
        if (!Boolean.TRUE.equals(request.getLongTerm()) && !request.getExpireTime().after(request.getEffectiveTime())) {
            throw new ServiceException("失效时间必须晚于生效时间");
        }
        if (request.getFieldPermissions() == null || request.getFieldPermissions().isEmpty()) {
            throw new ServiceException("至少配置一个字段权限");
        }
        if (request.getFieldPermissions().stream().noneMatch(field -> Boolean.TRUE.equals(field.getView()))) {
            throw new ServiceException("至少选择一个可查看字段");
        }
        if (request.getOperations() == null || request.getOperations().isEmpty()) {
            throw new ServiceException("至少配置一个操作权限");
        }
        normalizeOperations(request.getOperations());
        for (DataAuthorizationSaveReqDTO.FieldPermissionDTO field : request.getFieldPermissions()) {
            if (field.getFieldId() == null) {
                throw new ServiceException("字段权限缺少字段标识");
            }
            if (Boolean.TRUE.equals(field.getEdit()) && !Boolean.TRUE.equals(field.getView())) {
                throw new ServiceException("编辑权限必须以查看权限为前提");
            }
        }
        validateModelFields(request);
    }

    private void validateModelFields(DataAuthorizationSaveReqDTO request) {
        if (!referenceService.modelResourceApiAvailable()) {
            return;
        }
        Set<Long> fieldIds = request.getFieldPermissions().stream()
                .map(DataAuthorizationSaveReqDTO.FieldPermissionDTO::getFieldId)
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
        Map<Long, AuthorizationModelFieldDTO> fields = referenceService.getModelFields(fieldIds);
        if (fields.size() != fieldIds.size()) {
            throw new ServiceException("字段权限或数据范围中存在无效字段");
        }
        boolean wrongModel = fields.values().stream()
                .anyMatch(field -> !request.getModelId().equals(field.getModelId()));
        if (wrongModel) {
            throw new ServiceException("字段不属于所选主数据模型");
        }
    }

    private List<Long> getSubjectIds(DataAuthorizationSaveReqDTO request) {
        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            return request.getSubjectIds().stream().filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        }
        return request.getSubjectId() == null ? Collections.emptyList() : Collections.singletonList(request.getSubjectId());
    }

    private List<AuthorizationOperation> normalizeOperations(List<AuthorizationOperation> operations) {
        if (operations.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("存在不支持的操作权限编码");
        }
        return operations.stream().distinct().collect(Collectors.toList());
    }

    private AuthGrantDO requireGrant(Long id) {
        AuthGrantDO grant = grantMapper.selectById(id);
        if (grant == null) {
            throw new ServiceException("数据授权不存在或已删除");
        }
        return grant;
    }
}
