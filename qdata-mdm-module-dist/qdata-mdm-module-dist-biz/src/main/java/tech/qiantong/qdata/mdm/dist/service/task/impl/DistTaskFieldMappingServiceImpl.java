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

package tech.qiantong.qdata.mdm.dist.service.task.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Resource;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskFieldMappingSaveReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFieldMappingDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskFieldMappingMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskMapper;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskFieldMappingService;
/**
 * 分发字段映射Service业务层处理
 *
 * @author qdata
 * @date 2026-10-13
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistTaskFieldMappingServiceImpl  extends ServiceImpl<DistTaskFieldMappingMapper,DistTaskFieldMappingDO> implements IDistTaskFieldMappingService {
    @Resource
    private DistTaskFieldMappingMapper distTaskFieldMappingMapper;

    @Resource
    private DistTaskMapper distTaskMapper;

    /** 任务保存时只存已选字段；先校验整个列表，再在同一事务中替换旧映射。 */
    @Override
    public void replaceTaskFieldMappings(Long taskId, List<DistTaskFieldMappingSaveReqVO> mappings) {
        if (taskId == null || taskId <= 0 || distTaskMapper.selectById(taskId) == null) {
            throw new ServiceException("分发任务不存在");
        }
        if (mappings == null || mappings.isEmpty()) {
            throw new ServiceException("请至少选择一个分发字段");
        }
        Set<Long> sourceIds = new HashSet<>();
        Set<String> targetNames = new HashSet<>();
        for (DistTaskFieldMappingSaveReqVO row : mappings) {
            validateMapping(row, taskId);
            if (!sourceIds.add(row.getModelColumnId())) {
                throw new ServiceException("主数据模型字段不能重复选择");
            }
            if (!targetNames.add(row.getTargetFieldName().trim())) {
                throw new ServiceException("下游 API 字段不能重复");
            }
        }
        distTaskFieldMappingMapper.delete(new LambdaQueryWrapper<DistTaskFieldMappingDO>()
                .eq(DistTaskFieldMappingDO::getTaskId, taskId));
        for (int index = 0; index < mappings.size(); index++) {
            DistTaskFieldMappingDO mapping = BeanUtils.toBean(mappings.get(index), DistTaskFieldMappingDO.class);
            mapping.setId(null);
            mapping.setTaskId(taskId);
            mapping.setTargetFieldName(mapping.getTargetFieldName().trim());
            mapping.setSortOrder((long) index + 1);
            mapping.setValidFlag("1");
            mapping.setDelFlag("0");
            distTaskFieldMappingMapper.insert(mapping);
        }
    }

    /** 正式字段使用正数 ID；当前固定供应商模型仅允许三个约定字段用于页面联调。 */
    private void validateMapping(DistTaskFieldMappingSaveReqVO row, Long taskId) {
        if (row == null || row.getModelColumnId() == null || !isAllowedField(taskId, row.getModelColumnId())) {
            throw new ServiceException("请选择有效的主数据模型字段");
        }
        String target = row.getTargetFieldName() == null ? "" : row.getTargetFieldName().trim();
        if (target.isEmpty()) throw new ServiceException("请填写下游 API 字段");
        if (target.length() > 512) throw new ServiceException("下游 API 字段不能超过512个字符");
        if (target.contains(".") || target.contains("[") || target.contains("]")) {
            throw new ServiceException("下游 API 字段只能填写平铺 JSON 属性名");
        }
    }

    private boolean isAllowedField(Long taskId, Long fieldId) {
        DistTaskDO task = taskId == null ? null : distTaskMapper.selectById(taskId);
        if (task == null) return false;
        if (Long.valueOf(-1L).equals(task.getModelId())) {
            return fieldId == -11L || fieldId == -12L || fieldId == -13L;
        }
        return fieldId > 0;
    }

}
