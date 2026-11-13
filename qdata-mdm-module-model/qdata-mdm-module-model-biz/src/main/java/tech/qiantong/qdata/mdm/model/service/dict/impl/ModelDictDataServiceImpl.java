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

package tech.qiantong.qdata.mdm.model.service.dict.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.service.MasterFieldUsageApi;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataUsageRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictDataDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.dict.ModelDictDataMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictDataService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 参考数据项Service业务层处理
 *
 * @author qdata
 * @date 2026-09-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelDictDataServiceImpl extends ServiceImpl<ModelDictDataMapper, ModelDictDataDO> implements IModelDictDataService {

    private static final int MAX_USAGE_REQUESTS = 100;
    private static final int MAX_USAGE_ITEM_CODES = 500;

    @Resource
    private ModelDictDataMapper modelDictDataMapper;

    @Resource
    private IModelColumnService iModelColumnService;

    @Resource
    private ModelBaseMapper modelBaseMapper;

    @Resource
    private MasterFieldUsageApi masterFieldUsageApi;

    /**
     * 按模型汇总参考数据引用，批量查询模型，保留每个模型的全部引用字段。
     */
    @Override
    public List<ModelDictDataUsageRespVO> getModelDictDataUsage(Long typeId) {
        if (typeId == null) {
            throw new ServiceException("参考数据类型ID不能为空");
        }
        List<ModelColumnDO> columns = iModelColumnService.list(
                new QueryWrapper<ModelColumnDO>().eq("dict_type_id", typeId)
                        .orderByAsc("sort_order", "id"));
        java.util.Map<Long, List<ModelColumnDO>> columnsByModel = columns.stream()
                .filter(column -> column.getModelId() != null)
                .collect(Collectors.groupingBy(ModelColumnDO::getModelId));
        if (columnsByModel.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // 批量加载模型和版本，过滤已删除模型，孤立字段不生成引用记录。
        List<ModelBaseDO> models = modelBaseMapper.selectModelsWithVersionsByIds(columnsByModel.keySet());
        List<ModelDictDataUsageRespVO> result = new ArrayList<>();
        models.stream()
                .sorted(java.util.Comparator.comparing(ModelBaseDO::getCreateTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(ModelBaseDO::getId))
                .forEach(model -> {
                    ModelDictDataUsageRespVO row = BeanUtils.toBean(model, ModelDictDataUsageRespVO.class);
                    row.setFields(BeanUtils.toBean(columnsByModel.get(model.getId()), ModelColumnRespVO.class));
                    result.add(row);
                });
        fillFieldUsageCounts(typeId, result);
        return result;
    }

    /**
     * 批量统计当前参考数据类型的使用数量，按模型、字段和数据项编码写回引用列表。
     *
     * @param typeId 参考数据类型 ID
     * @param models 已查出的模型及其引用字段
     */
    private void fillFieldUsageCounts(Long typeId, List<ModelDictDataUsageRespVO> models) {
        if (models.isEmpty()) {
            return;
        }
        List<String> itemCodes = modelDictDataMapper.selectList(new LambdaQueryWrapper<ModelDictDataDO>()
                        .select(ModelDictDataDO::getCode)
                        .eq(ModelDictDataDO::getTypeId, typeId))
                .stream()
                .map(ModelDictDataDO::getCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (itemCodes.isEmpty()) {
            return;
        }

        Map<Long, ModelDictDataUsageRespVO> modelsById = new LinkedHashMap<>();
        List<MasterDictUsageReqDTO> requests = new ArrayList<>();
        for (ModelDictDataUsageRespVO model : models) {
            modelsById.put(model.getId(), model);
            for (ModelColumnRespVO field : model.getFields()) {
                MasterDictUsageReqDTO request = new MasterDictUsageReqDTO();
                request.setModelId(model.getId());
                request.setFieldId(field.getId());
                request.setDictTypeId(typeId);
                requests.add(request);
            }
        }

        // 同时满足单批字段数和数据项编码总量限制。单个字段也可能需要多批编码。
        for (int codeStart = 0; codeStart < itemCodes.size(); codeStart += MAX_USAGE_ITEM_CODES) {
            List<String> codes = itemCodes.subList(codeStart, Math.min(codeStart + MAX_USAGE_ITEM_CODES, itemCodes.size()));
            int fieldsPerBatch = Math.min(MAX_USAGE_REQUESTS, MAX_USAGE_ITEM_CODES / codes.size());
            for (int fieldStart = 0; fieldStart < requests.size(); fieldStart += fieldsPerBatch) {
                List<MasterDictUsageReqDTO> batch = requests.subList(fieldStart,
                        Math.min(fieldStart + fieldsPerBatch, requests.size()));
                batch.forEach(request -> request.setItemCodes(codes));
                for (MasterDictUsageRespDTO count : masterFieldUsageApi.countDictUsageBatch(batch)) {
                    modelsById.get(count.getModelId()).getFieldUsageCounts()
                            .computeIfAbsent(count.getFieldId(), key -> new LinkedHashMap<>())
                            .put(count.getItemCode(), count.getUsageCount());
                }
            }
        }
    }

    @Override
    public PageResult<ModelDictDataDO> getModelDictDataPage(ModelDictDataPageReqVO pageReqVO) {
        return modelDictDataMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelDictData(ModelDictDataSaveReqVO createReqVO) {
        ModelDictDataDO dictType = BeanUtils.toBean(createReqVO, ModelDictDataDO.class);
        modelDictDataMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelDictData(ModelDictDataSaveReqVO updateReqVO) {
        // 相关校验

        // 更新参考数据项
        ModelDictDataDO updateObj = BeanUtils.toBean(updateReqVO, ModelDictDataDO.class);
        return modelDictDataMapper.updateById(updateObj);
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelDictDataDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelDictDataMapper.selectCount(wrapper) == 0;
    }

    @Override
    public int updateModelDictDataStatus(Long id, Boolean status) {
        ModelDictDataDO updateObj = new ModelDictDataDO();
        updateObj.setId(id);
        updateObj.setValidFlag(status);
        return modelDictDataMapper.updateById(updateObj);
    }

    @Override
    public int removeModelDictData(Collection<Long> idList) {
        // 批量删除参考数据项
        return modelDictDataMapper.deleteByIds(idList);
    }

    @Override
    public ModelDictDataDO getModelDictDataById(Long id) {
        return modelDictDataMapper.selectById(id);
    }

    /**
     * 导入参考数据项数据
     *
     * @param importExcelList 参考数据项数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelDictData(List<ModelDictDataRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelDictDataRespVO respVO : importExcelList) {
            try {
                ModelDictDataDO modelDictDataDO = BeanUtils.toBean(respVO, ModelDictDataDO.class);
                Long modelDictTypeId = respVO.getId();
                if (isUpdateSupport) {
                    if (modelDictTypeId != null) {
                        ModelDictDataDO existingModelDictData = modelDictDataMapper.selectById(modelDictTypeId);
                        if (existingModelDictData != null) {
                            modelDictDataMapper.updateById(modelDictDataDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + modelDictTypeId + " 的参考数据项记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + modelDictTypeId + " 的参考数据项记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelDictDataDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", modelDictTypeId);
                    ModelDictDataDO existingModelDictData = modelDictDataMapper.selectOne(queryWrapper);
                    if (existingModelDictData == null) {
                        modelDictDataMapper.insert(modelDictDataDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + modelDictTypeId + " 的参考数据项记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + modelDictTypeId + " 的参考数据项记录已存在。");
                    }
                }
            } catch (Exception e) {
                failureNum++;
                String errorMsg = "数据导入失败，错误信息：" + e.getMessage();
                failureMessages.add(errorMsg);
                log.error(errorMsg, e);
            }
        }
        StringBuilder resultMsg = new StringBuilder();
        if (failureNum > 0) {
            resultMsg.append("很抱歉，导入失败！共 ").append(failureNum).append(" 条数据格式不正确，错误如下：");
            resultMsg.append("<br/>").append(String.join("<br/>", failureMessages));
            throw new ServiceException(resultMsg.toString());
        } else {
            resultMsg.append("恭喜您，数据已全部导入成功！共 ").append(successNum).append(" 条。");
        }
        return resultMsg.toString();
    }
}
