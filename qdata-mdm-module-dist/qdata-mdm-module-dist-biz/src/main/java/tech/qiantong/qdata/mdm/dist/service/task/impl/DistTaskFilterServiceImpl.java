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

package tech.qiantong.qdata.mdm.dist.service.task.impl;

import java.util.Arrays;
import java.util.Collections;
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
import tech.qiantong.qdata.mdm.dist.controller.admin.task.vo.DistTaskFilterSaveReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskFilterDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskFilterMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskMapper;
import tech.qiantong.qdata.mdm.dist.service.task.IDistTaskFilterService;
/**
 * 分发任务过滤条件Service业务层处理
 *
 * @author qdata
 * @date 2026-10-13
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistTaskFilterServiceImpl  extends ServiceImpl<DistTaskFilterMapper,DistTaskFilterDO> implements IDistTaskFilterService {
    private static final Set<String> ALLOWED_OPERATORS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "EQ", "NE", "CONTAINS", "NOT_CONTAINS", "GT", "GE", "LT", "LE", "IS_NULL", "IS_NOT_NULL")));

    @Resource
    private DistTaskFilterMapper distTaskFilterMapper;

    @Resource
    private DistTaskMapper distTaskMapper;

    /** 任务与条件在同一事务保存；先校验完整列表，再替换旧条件。 */
    @Override
    public void replaceTaskFilters(Long taskId, List<DistTaskFilterSaveReqVO> filters) {
        List<DistTaskFilterSaveReqVO> rows = filters == null ? Collections.emptyList() : filters;
        if (taskId == null || taskId <= 0 || distTaskMapper.selectById(taskId) == null) {
            throw new ServiceException("分发任务不存在");
        }
        if (!rows.isEmpty()) {
            requireConditionTask(taskId);
            rows.forEach(row -> validateFilter(row, taskId));
        }
        distTaskFilterMapper.delete(new LambdaQueryWrapper<DistTaskFilterDO>()
                .eq(DistTaskFilterDO::getTaskId, taskId));
        for (int index = 0; index < rows.size(); index++) {
            DistTaskFilterDO filter = BeanUtils.toBean(rows.get(index), DistTaskFilterDO.class);
            filter.setId(null);
            filter.setTaskId(taskId);
            filter.setSortOrder((long) index + 1);
            filter.setValidFlag("1");
            filter.setDelFlag("0");
            if (isNullOperator(filter.getOperator())) filter.setFilterValue(null);
            else filter.setFilterValue(filter.getFilterValue().trim());
            distTaskFilterMapper.insert(filter);
        }
    }

    /** 过滤条件只能绑定真实任务，且运算符和值必须符合本期简单筛选协议。 */
    private void validateFilter(DistTaskFilterSaveReqVO row, Long taskId) {
        if (row == null || taskId == null || taskId <= 0) throw new ServiceException("请选择分发任务");
        Long fieldId = row.getModelColumnId();
        DistTaskDO task = distTaskMapper.selectById(taskId);
        boolean allowedField = fieldId != null && task != null
                && (Long.valueOf(-1L).equals(task.getModelId())
                    ? (fieldId == -11L || fieldId == -12L || fieldId == -13L)
                    : fieldId > 0);
        if (!allowedField) {
            throw new ServiceException("请选择有效的主数据模型字段");
        }
        if (!ALLOWED_OPERATORS.contains(row.getOperator())) throw new ServiceException("筛选条件运算符无效");
        if (!isNullOperator(row.getOperator())
                && (row.getFilterValue() == null || row.getFilterValue().trim().isEmpty())) {
            throw new ServiceException("请输入筛选比较值");
        }
        if (row.getFilterValue() != null && row.getFilterValue().length() > 512) {
            throw new ServiceException("筛选比较值不能超过512个字符");
        }
    }

    private boolean isNullOperator(String operator) {
        return "IS_NULL".equals(operator) || "IS_NOT_NULL".equals(operator);
    }

    private void requireConditionTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new ServiceException("分发任务不存在");
        }
        DistTaskDO task = distTaskMapper.selectById(taskId);
        if (task == null) throw new ServiceException("分发任务不存在");
        if (!"CONDITION".equals(task.getDataScopeType())) {
            throw new ServiceException("仅条件筛选任务可以配置筛选条件");
        }
    }

}
