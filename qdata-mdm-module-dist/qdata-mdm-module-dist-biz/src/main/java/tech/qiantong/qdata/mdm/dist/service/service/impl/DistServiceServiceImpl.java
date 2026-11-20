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

package tech.qiantong.qdata.mdm.dist.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.*;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceFieldDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceCallLogMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.service.DistServiceFieldMapper;
import tech.qiantong.qdata.mdm.dist.enums.service.DistServiceConstants;
import tech.qiantong.qdata.mdm.dist.service.service.IDistServiceService;
import tech.qiantong.qdata.mdm.dist.service.service.event.DistServiceRuntimeRouteChangedEvent;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;
import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据服务基础配置：主表和字段配置在同一事务中保存、更新及逻辑删除。
 * @author chentangwen
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistServiceServiceImpl extends ServiceImpl<DistServiceMapper, DistServiceDO> implements IDistServiceService {
    @Resource
    private DistServiceMapper distServiceMapper;
    @Resource
    private DistServiceFieldMapper distServiceFieldMapper;
    @Resource
    private DistServiceCallLogMapper distServiceCallLogMapper;
    @Resource
    private ModelPublishedApi modelPublishedApi;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ApplicationEventPublisher eventPublisher;
    @Override
    public PageResult<DistServiceRespVO> getDistServicePage(DistServicePageReqVO pageReqVO) {
        PageResult<DistServiceDO> page = distServiceMapper.selectPage(pageReqVO);
        List<DistServiceRespVO> rows = BeanUtils.toBean(page.getRows(), DistServiceRespVO.class);
        rows.forEach(row -> {
            row.setStatus(normalizeStatus(row.getStatus()));
            row.setPublishStatus(normalizePublishStatus(row.getPublishStatus(), row.getPublishTime()));
            row.setCallCount(0L);
            row.setServiceUrl(buildServiceUrl(row.getServicePath(), row.getApiType()));
        });
        List<Long> serviceIds = rows.stream().map(DistServiceRespVO::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (!serviceIds.isEmpty()) {
            Date now = new Date();
            Date startTime = new Date(now.getTime() - 7L * 24 * 60 * 60 * 1000);
            Map<Long, DistServiceRespVO> statsByServiceId = distServiceCallLogMapper
                    .selectStatsByServiceIds(serviceIds, startTime, now).stream()
                    .collect(Collectors.toMap(DistServiceRespVO::getId, Function.identity(), (first, second) -> first));
            rows.forEach(row -> {
                DistServiceRespVO stats = statsByServiceId.get(row.getId());
                if (stats != null) {
                    row.setCallCount(stats.getCallCount());
                    row.setLastCallTime(stats.getLastCallTime());
                }
            });
        }
        List<Long> creatorIds = rows.stream().map(DistServiceRespVO::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        /*
         * 按当前页创建人批量查询电话，避免逐条访问用户表。
         */
        if (!creatorIds.isEmpty()) {
            Map<Long, DistServiceRespVO> creators = distServiceMapper.selectCreatorPhonesByIds(creatorIds).stream()
                    .collect(Collectors.toMap(DistServiceRespVO::getCreatorId, Function.identity(), (first, second) -> first));
            rows.forEach(row -> {
                DistServiceRespVO creator = creators.get(row.getCreatorId());
                if (creator != null) {
                    row.setCreatorPhone(creator.getCreatorPhone());
                }
            });
        }
        return new PageResult<>(rows, page.getTotal());
    }

    @Override
    public DistServiceRespVO getDistServiceById(Long id) {
        DistServiceRespVO result = BeanUtils.toBean(requireService(id), DistServiceRespVO.class);
        result.setStatus(normalizeStatus(result.getStatus()));
        result.setPublishStatus(normalizePublishStatus(result.getPublishStatus(), result.getPublishTime()));
        result.setServiceUrl(buildServiceUrl(result.getServicePath(), result.getApiType()));
        List<DistServiceFieldDO> fields = distServiceFieldMapper.selectList(new LambdaQueryWrapper<DistServiceFieldDO>()
                .eq(DistServiceFieldDO::getServiceId, id)
                .orderByAsc(DistServiceFieldDO::getSortOrder, DistServiceFieldDO::getId));
        result.setFields(BeanUtils.toBean(fields, DistServiceFieldRespVO.class));
        /*
         * 详情顶部展示创建人及其电话，与服务负责人信息分别取值。
         */
        if (result.getCreatorId() != null) {
            SysUser creator = sysUserService.selectUserById(result.getCreatorId());
            if (creator != null) {
                result.setCreatorPhone(creator.getPhonenumber());
            }
        }
        if (result.getResponsibleUserId() != null) {
            SysUser user = sysUserService.selectUserById(result.getResponsibleUserId());
            if (user != null) {
                result.setResponsiblePersonName(StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUserName());
                result.setResponsiblePersonPhone(user.getPhonenumber());
            }
        }
        return result;
    }

    @Override
    public Long createDistService(DistServiceSaveReqVO request) {
        validateConfig(request, null);
        Date now = new Date();
        DistServiceDO entity = new DistServiceDO();
        /*
         * 编码、生命周期和审计信息由后端维护，新增服务默认停用。
         */
        entity.setCode(DistServiceConstants.SERVICE_CODE_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        entity.setName(request.getName().trim());
        entity.setModelId(request.getModelId());
        entity.setApiType(request.getApiType());
        entity.setResponsibleUserId(request.getResponsibleUserId());
        entity.setServicePath(request.getServicePath().trim());
        entity.setHttpMethod(DistServiceConstants.DEFAULT_HTTP_METHOD);
        entity.setStatus(DistServiceConstants.DEFAULT_STATUS);
        entity.setPublishStatus(DistServiceConstants.DEFAULT_PUBLISH_STATUS);
        entity.setDescription(request.getDescription());
        entity.setRemark(request.getRemark());
        entity.setValidFlag(DistServiceConstants.VALID_FLAG_YES);
        entity.setDelFlag(DistServiceConstants.NOT_DELETED);
        entity.setCreatorId(request.getCreatorId());
        entity.setCreateBy(request.getCreateBy());
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        if (distServiceMapper.insert(entity) != 1) {
            throw new ServiceException("新增数据服务失败");
        }
        if (entity.getId() == null) {
            throw new ServiceException("新增数据服务失败，未获取到主键ID");
        }
        entity.setCode(DistServiceConstants.SERVICE_CODE_PREFIX + entity.getId());
        if (distServiceMapper.update(null, new UpdateWrapper<DistServiceDO>()
                .eq("ID", entity.getId())
                .set("CODE", entity.getCode())) != 1) {
            throw new ServiceException("生成数据服务编码失败");
        }
        replaceFields(entity.getId(), request, request.getCreatorId(), request.getCreateBy());
        return entity.getId();
    }

    @Override
    public int updateDistService(DistServiceSaveReqVO request) {
        requireService(request.getId());
        validateConfig(request, request.getId());
        /*
         * 显式限定允许更新的列；可选文本传空时能够清空，创建信息和发布信息保留。
         */
        int rows = distServiceMapper.update(null, new UpdateWrapper<DistServiceDO>()
                .eq("ID", request.getId())
                .set("NAME", request.getName().trim())
                .set("MODEL_ID", request.getModelId())
                .set("API_TYPE", request.getApiType())
                .set("RESPONSIBLE_USER_ID", request.getResponsibleUserId())
                .set("SERVICE_PATH", request.getServicePath().trim())
                .set("DESCRIPTION", request.getDescription())
                .set("REMARK", request.getRemark())
                .set("UPDATER_ID", request.getUpdatorId())
                .set("UPDATE_BY", request.getUpdateBy())
                .set("UPDATE_TIME", new Date()));
        if (rows != 1) {
            throw new ServiceException("修改数据服务失败，记录可能已被删除");
        }
        replaceFields(request.getId(), request, request.getUpdatorId(), request.getUpdateBy());
        publishRouteChanged(request.getId());
        return rows;
    }

    @Override
    public int updateDistServiceStatus(DistServiceStatusReqVO request) {
        if (!DistServiceConstants.STATUS_DISABLED.equals(request.getStatus())
                && !DistServiceConstants.STATUS_ENABLED.equals(request.getStatus())) {
            throw new ServiceException("服务状态只能为0（停用）或1（启用）");
        }
        DistServiceDO entity = requireService(request.getId());
        /*
         * 按读取时的状态更新，避免并发切换覆盖其他用户的操作。
         */
        UpdateWrapper<DistServiceDO> update = new UpdateWrapper<DistServiceDO>()
                .eq("ID", entity.getId())
                .eq(entity.getStatus() != null, "STATUS", entity.getStatus())
                .isNull(entity.getStatus() == null, "STATUS")
                .set("STATUS", request.getStatus())
                .set("UPDATER_ID", request.getUpdatorId())
                .set("UPDATE_BY", request.getUpdateBy())
                .set("UPDATE_TIME", new Date());
        int rows = distServiceMapper.update(null, update);
        if (rows != 1) {
            throw new ServiceException("切换服务状态失败，请刷新后重试");
        }
        publishRouteChanged(entity.getId());
        return rows;
    }

    private String normalizeStatus(String status) {
        return status == null || DistServiceConstants.LEGACY_STATUS_DISABLED.equals(status)
                ? DistServiceConstants.STATUS_DISABLED : status;
    }

    /**
     * 默认写入发布标志1但未执行发布，没有发布时间的记录按未发布展示。
     */
    private String normalizePublishStatus(String status, Date publishTime) {
        return DistServiceConstants.PUBLISH_STATUS_PUBLISHED.equals(status) && publishTime != null
                ? DistServiceConstants.PUBLISH_STATUS_PUBLISHED : DistServiceConstants.PUBLISH_STATUS_UNPUBLISHED;
    }

    /**
     * 返回不绑定主机和端口的调用地址，分页服务附带默认分页参数。
     */
    private String buildServiceUrl(String servicePath, String apiType) {
        String path = servicePath == null || servicePath.isEmpty() ? "/" : servicePath;
        if (!path.startsWith("/")) path = "/" + path;
        String url = DistServiceConstants.API_ROUTE_PREFIX + path;
        if (DistServiceConstants.API_TYPE_PAGE.equals(apiType)) {
            url += "?pageNum=" + DistServiceConstants.DEFAULT_PAGE_NUM
                    + "&pageSize=" + DistServiceConstants.DEFAULT_PAGE_SIZE;
        }
        return url;
    }

    @Override
    public int publishDistService(Long id, Long operatorId, String operatorName) {
        DistServiceDO entity = requireService(id);
        if (!DistServiceConstants.STATUS_ENABLED.equals(entity.getStatus())) {
            throw new ServiceException("只有启用状态的数据服务才可以发布");
        }
        if (DistServiceConstants.PUBLISH_STATUS_PUBLISHED.equals(
                normalizePublishStatus(entity.getPublishStatus(), entity.getPublishTime()))) {
            throw new ServiceException("数据服务已发布，请勿重复发布");
        }
        Date now = new Date();
        /*
         * 启用状态和发布信息参与条件更新，避免并发停用或重复发布。
         */
        int rows = distServiceMapper.update(null, new UpdateWrapper<DistServiceDO>()
                .eq("ID", id)
                .eq("STATUS", DistServiceConstants.STATUS_ENABLED)
                .eq(entity.getPublishStatus() != null, "PUBLISH_STATUS", entity.getPublishStatus())
                .isNull(entity.getPublishStatus() == null, "PUBLISH_STATUS")
                .eq(entity.getPublishTime() != null, "PUBLISH_TIME", entity.getPublishTime())
                .isNull(entity.getPublishTime() == null, "PUBLISH_TIME")
                .set("PUBLISH_STATUS", DistServiceConstants.PUBLISH_STATUS_PUBLISHED)
                .set("PUBLISH_TIME", now)
                .set("UPDATER_ID", operatorId)
                .set("UPDATE_BY", operatorName)
                .set("UPDATE_TIME", now));
        if (rows != 1) {
            throw new ServiceException("发布失败，服务状态已发生变化，请刷新后重试");
        }
        publishRouteChanged(id);
        return rows;
    }

    @Override
    public int removeDistService(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        if (ids.contains(null)) {
            throw new ServiceException("数据服务ID不能为空");
        }
        /*
         * 先删除主表，和修改操作保持相同锁顺序，子表删除失败时由事务整体回滚。
         */
        int rows = distServiceMapper.deleteBatchIds(ids);
        distServiceFieldMapper.delete(new LambdaQueryWrapper<DistServiceFieldDO>()
                .in(DistServiceFieldDO::getServiceId, ids));
        ids.forEach(this::publishRouteChanged);
        return rows;
    }

    private void publishRouteChanged(Long serviceId) {
        eventPublisher.publishEvent(new DistServiceRuntimeRouteChangedEvent(serviceId));
    }

    private DistServiceDO requireService(Long id) {
        if (id == null) {
            throw new ServiceException("数据服务ID不能为空");
        }
        DistServiceDO entity = distServiceMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("数据服务不存在或已删除");
        }
        return entity;
    }

    /**
     * 路径唯一性及字段归属按已发布的可用模型校验，不读取模型草稿。
     */
    private void validateConfig(DistServiceSaveReqVO request, Long excludedId) {
        if (!DistServiceConstants.API_TYPE_SINGLE.equals(request.getApiType())
                && !DistServiceConstants.API_TYPE_PAGE.equals(request.getApiType())) {
            throw new ServiceException("API类型只能为0（单条查询）或1（列表分页查询）");
        }
        if (!StringUtils.hasText(request.getName()) || request.getModelId() == null) {
            throw new ServiceException("服务名称和主数据模型不能为空");
        }
        if (request.getResponsibleUserId() != null
                && sysUserService.selectUserById(request.getResponsibleUserId()) == null) {
            throw new ServiceException("所选负责人不存在");
        }
        String path = request.getServicePath();
        if (path == null || !path.matches(DistServiceConstants.SERVICE_PATH_REGEX)) {
            throw new ServiceException("服务路径须以/开头，且不能包含空白、查询参数或锚点");
        }
        Long count = distServiceMapper.selectCount(new LambdaQueryWrapper<DistServiceDO>()
                .eq(DistServiceDO::getServicePath, path)
                .ne(excludedId != null, DistServiceDO::getId, excludedId));
        if (count > 0) {
            throw new ServiceException("服务路径已存在");
        }
        List<DistServiceFieldSaveReqVO> fields = request.getFields();
        if (fields == null || fields.isEmpty()) {
            throw new ServiceException("服务字段配置不能为空");
        }
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(request.getModelId());
        Map<Long, ModelPublishedColumnRespDTO> columns = structure.getFields().stream()
                .collect(Collectors.toMap(ModelPublishedColumnRespDTO::getId, Function.identity()));
        Set<Long> columnIds = new HashSet<>();
        Set<String> outputNames = new HashSet<>();
        boolean hasOutput = false;
        for (DistServiceFieldSaveReqVO field : fields) {
            if (field == null || !columns.containsKey(field.getModelColumnId())) {
                throw new ServiceException("服务字段必须属于所选模型的已发布可用字段");
            }
            if (!columnIds.add(field.getModelColumnId())) {
                throw new ServiceException("模型字段不能重复配置");
            }
            String output = field.getOutputFlag() == null ? DistServiceConstants.FLAG_YES : field.getOutputFlag();
            String queryable = field.getQueryableFlag() == null ? DistServiceConstants.FLAG_NO : field.getQueryableFlag();
            if (!(DistServiceConstants.FLAG_NO.equals(output) || DistServiceConstants.FLAG_YES.equals(output))
                    || !(DistServiceConstants.FLAG_NO.equals(queryable) || DistServiceConstants.FLAG_YES.equals(queryable))) {
                throw new ServiceException("查询和输出标志只能为0或1");
            }
            if (DistServiceConstants.FLAG_YES.equals(output)) {
                hasOutput = true;
                String outputName = StringUtils.hasText(field.getFieldAlias())
                        ? field.getFieldAlias().trim() : columns.get(field.getModelColumnId()).getCode();
                if (!outputNames.add(outputName)) {
                    throw new ServiceException("输出字段名称或别名不能重复");
                }
            }
        }
        if (!hasOutput) {
            throw new ServiceException("至少选择一个输出字段");
        }
    }

    /**
     * 子表整体替换，子表身份和审计信息由当前服务及操作者统一生成。
     */
    private void replaceFields(Long serviceId, DistServiceSaveReqVO request, Long operatorId, String operatorName) {
        distServiceFieldMapper.delete(new LambdaQueryWrapper<DistServiceFieldDO>()
                .eq(DistServiceFieldDO::getServiceId, serviceId));
        Date now = new Date();
        for (int i = 0; i < request.getFields().size(); i++) {
            DistServiceFieldSaveReqVO source = request.getFields().get(i);
            DistServiceFieldDO entity = BeanUtils.toBean(source, DistServiceFieldDO.class);
            entity.setServiceId(serviceId);
            entity.setQueryableFlag(source.getQueryableFlag() == null ? DistServiceConstants.FLAG_NO : source.getQueryableFlag());
            entity.setOutputFlag(source.getOutputFlag() == null ? DistServiceConstants.FLAG_YES : source.getOutputFlag());
            entity.setFieldAlias(StringUtils.hasText(source.getFieldAlias()) ? source.getFieldAlias().trim() : null);
            entity.setSortOrder(source.getSortOrder() == null ? i : source.getSortOrder());
            entity.setValidFlag(DistServiceConstants.VALID_FLAG_YES);
            entity.setDelFlag(DistServiceConstants.NOT_DELETED);
            entity.setCreatorId(operatorId);
            entity.setCreateBy(operatorName);
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            if (distServiceFieldMapper.insert(entity) != 1) {
                throw new ServiceException("保存服务字段配置失败");
            }
        }
    }
}
