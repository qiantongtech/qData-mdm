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

package tech.qiantong.qdata.mdm.model.service.encoding.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingItemMapper;
import tech.qiantong.qdata.mdm.model.enums.ModelEncodingItemTypeEnum;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingItemService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingVersionService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 编码方案规则段Service业务层处理
 *
 * @author qdata
 * @date 2026-09-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelEncodingItemServiceImpl extends ServiceImpl<ModelEncodingItemMapper, ModelEncodingItemDO> implements IModelEncodingItemService {
    @Resource
    private ModelEncodingItemMapper modelEncodingItemMapper;

    @Resource
    private IModelEncodingVersionService iModelEncodingVersionService;

    @Override
    public PageResult<ModelEncodingItemDO> getModelEncodingItemPage(ModelEncodingItemPageReqVO pageReqVO) {
        return modelEncodingItemMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelEncodingItem(ModelEncodingItemSaveReqVO createReqVO) {
        ModelEncodingItemDO dictType = BeanUtils.toBean(createReqVO, ModelEncodingItemDO.class);
        modelEncodingItemMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelEncodingItem(ModelEncodingItemSaveReqVO updateReqVO) {
        // 相关校验

        // 更新编码方案规则段
        ModelEncodingItemDO updateObj = BeanUtils.toBean(updateReqVO, ModelEncodingItemDO.class);
        return modelEncodingItemMapper.updateById(updateObj);
    }

    @Override
    public void saveModelEncodingItems(Long encodingVersionId, List<ModelEncodingItemSaveReqVO> items) {
        // 先确认父版本存在，避免生成无法归属任何编码版本的规则段。
        if (iModelEncodingVersionService.getModelEncodingVersionById(encodingVersionId) == null) {
            throw new ServiceException("编码方案版本不存在");
        }

        // 一次性加载当前版本原有规则并建立索引，用于校验更新归属和计算待删除数据。
        List<ModelEncodingItemDO> existingItems = modelEncodingItemMapper.selectList(
                new LambdaQueryWrapper<ModelEncodingItemDO>()
                        .eq(ModelEncodingItemDO::getEncodingVersionId, encodingVersionId));
        Map<Long, ModelEncodingItemDO> existingMap = new HashMap<>();
        existingItems.forEach(item -> existingMap.put(item.getId(), item));

        Set<Long> retainedIds = new HashSet<>();
        for (int index = 0; index < items.size(); index++) {
            ModelEncodingItemSaveReqVO item = items.get(index);
            validateRuleItem(item, index + 1);
            item.setEncodingVersionId(encodingVersionId);
            // 前端数组顺序即编码拼接顺序，保存时统一重排为从1开始的连续序号。
            item.setSortOrder((long) index + 1);

            // 新增项没有数据库ID，插入后继续处理下一规则段。
            if (item.getId() == null) {
                ModelEncodingItemDO createObj = BeanUtils.toBean(item, ModelEncodingItemDO.class);
                modelEncodingItemMapper.insert(createObj);
                continue;
            }

            ModelEncodingItemDO existing = existingMap.get(item.getId());
            // 仅允许更新当前版本原有数据，同时阻止通过构造ID修改其他版本规则。
            if (existing == null) {
                throw new ServiceException("规则段不存在或不属于当前编码版本，ID：" + item.getId());
            }
            if (!retainedIds.add(item.getId())) {
                throw new ServiceException("规则段ID重复：" + item.getId());
            }
            ModelEncodingItemDO updateObj = BeanUtils.toBean(item, ModelEncodingItemDO.class);
            // 使用显式set让null/空值也写入数据库，切换规则类型时可清除旧类型专属字段。
            modelEncodingItemMapper.updateById(updateObj);
        }

        // 批量请求代表当前版本的完整规则集合，数据库中未被保留的旧ID即为本次删除项。
        List<Long> removedIds = existingItems.stream()
                .map(ModelEncodingItemDO::getId)
                .filter(id -> !retainedIds.contains(id))
                .collect(Collectors.toList());
        if (!removedIds.isEmpty()) {
            modelEncodingItemMapper.deleteByIds(removedIds);
        }
    }

    @Override
    public int removeModelEncodingItem(Collection<Long> idList) {
        // 批量删除编码方案规则段
        return modelEncodingItemMapper.deleteByIds(idList);
    }

    @Override
    public ModelEncodingItemDO getModelEncodingItemById(Long id) {
        return modelEncodingItemMapper.selectById(id);
    }

    /**
     * 校验单个规则段的公共配置及流水号专属数值范围。
     *
     * @param item     规则段保存参数
     * @param position 规则段在当前编码结构中的显示位置，从1开始
     */
    private void validateRuleItem(ModelEncodingItemSaveReqVO item, int position) {
        if (item == null) {
            throw new ServiceException("第 " + position + " 个规则段不能为空");
        }
        ModelEncodingItemTypeEnum type = ModelEncodingItemTypeEnum.getByValue(item.getType());
        if (type == null) {
            throw new ServiceException("第 " + position + " 个规则段类型不正确");
        }
        // 非流水号类型都通过fixedValue承载其核心值：固定内容、字段参数或日期格式。
        if (type != ModelEncodingItemTypeEnum.SERIAL_NUMBER
                && (item.getFixedValue() == null || item.getFixedValue().trim().isEmpty())) {
            throw new ServiceException("第 " + position + " 个规则段配置不能为空");
        }
        // 流水号不使用fixedValue，改为校验位数、起始值、步长和重置方式等专属字段。
        if (type == ModelEncodingItemTypeEnum.SERIAL_NUMBER) {
            if (item.getSequenceLength() == null
                    || item.getSequenceLength() < 1 || item.getSequenceLength() > 12) {
                throw new ServiceException("第 " + position + " 个流水号规则位数必须为 1 至 12");
            }
            if (item.getSequenceStart() == null || item.getSequenceStart() < 0) {
                throw new ServiceException("第 " + position + " 个流水号规则起始值不能小于 0");
            }
            if (item.getStepSize() == null || item.getStepSize() < 1) {
                throw new ServiceException("第 " + position + " 个流水号规则步长必须大于 0");
            }
            if (item.getResetMethod() == null || item.getResetMethod().trim().isEmpty()) {
                throw new ServiceException("第 " + position + " 个流水号规则重置方式不能为空");
            }
        }
    }
}
