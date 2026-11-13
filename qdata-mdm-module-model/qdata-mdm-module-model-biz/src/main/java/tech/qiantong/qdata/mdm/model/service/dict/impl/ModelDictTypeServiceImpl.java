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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictDataDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictTypeDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.dict.ModelDictTypeMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictDataService;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictTypeService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 参考数据Service业务层处理
 *
 * @author qdata
 * @date 2026-09-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelDictTypeServiceImpl extends ServiceImpl<ModelDictTypeMapper, ModelDictTypeDO> implements IModelDictTypeService {
    @Resource
    private ModelDictTypeMapper modelDictTypeMapper;

    @Resource
    private IModelColumnService iModelColumnService;

    @Resource
    private IModelDictDataService iModelDictDataService;

    @Override
    public PageResult<ModelDictTypeDO> getModelDictTypePage(ModelDictTypePageReqVO pageReqVO) {
        PageResult<ModelDictTypeDO> page = modelDictTypeMapper.selectPage(pageReqVO);
        List<ModelDictTypeDO> rows = page.getRows();
        if (rows == null || rows.isEmpty()) return page;
        List<Long> ids = rows.stream().map(ModelDictTypeDO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, Long> counts = iModelDictDataService.list(new QueryWrapper<ModelDictDataDO>().select("type_id").in("type_id", ids))
                .stream().collect(Collectors.groupingBy(ModelDictDataDO::getTypeId, Collectors.counting()));
        rows.forEach(row -> row.setDataCount(String.valueOf(counts.getOrDefault(row.getId(), 0L))));
        return page;
    }

    @Override
    public Long createModelDictType(ModelDictTypeSaveReqVO createReqVO) {
        ModelDictTypeDO dictType = BeanUtils.toBean(createReqVO, ModelDictTypeDO.class);
        modelDictTypeMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelDictType(ModelDictTypeSaveReqVO updateReqVO) {
        // 相关校验

        // 更新参考数据
        ModelDictTypeDO updateObj = BeanUtils.toBean(updateReqVO, ModelDictTypeDO.class);
        return modelDictTypeMapper.updateById(updateObj);
    }

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelDictTypeDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelDictTypeMapper.selectCount(wrapper) == 0;
    }

    @Override
    public int updateModelDictTypeStatus(Long id, Boolean status) {
        ModelDictTypeDO updateObj = new ModelDictTypeDO();
        updateObj.setId(id);
        updateObj.setValidFlag(status);
        return modelDictTypeMapper.updateById(updateObj);
    }

    @Override
    public int updateModelDictTypeStatusBatch(Collection<Long> idList, Boolean status) {
        ModelDictTypeDO updateObj = new ModelDictTypeDO();
        updateObj.setValidFlag(status);
        return modelDictTypeMapper.update(updateObj, new QueryWrapper<ModelDictTypeDO>().in("id", idList));
    }

    @Override
    public int removeModelDictType(Collection<Long> idList) {
        // 批量删除参考数据
        return modelDictTypeMapper.deleteByIds(idList);
    }

    @Override
    public long getDataCount(Collection<Long> ids) {
        return ids == null || ids.isEmpty()
                ? 0
                : iModelColumnService.count(new QueryWrapper<ModelColumnDO>().in("dict_type_id", ids));
    }

    @Override
    public ModelDictTypeDO getModelDictTypeById(Long id) {
        return modelDictTypeMapper.selectById(id);
    }

    @Override
    public List<ModelDictTypeDO> getModelDictTypeList() {
        return modelDictTypeMapper.selectList();
    }

    @Override
    public Map<Long, ModelDictTypeDO> getModelDictTypeMap() {
        List<ModelDictTypeDO> modelDictTypeList = modelDictTypeMapper.selectList();
        return modelDictTypeList.stream()
                .collect(Collectors.toMap(
                        ModelDictTypeDO::getId,
                        modelDictTypeDO -> modelDictTypeDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }


    /**
     * 导入参考数据数据
     *
     * @param importExcelList 参考数据数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importModelDictType(List<ModelDictTypeRespVO> importExcelList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(importExcelList) || importExcelList.isEmpty()) {
            throw new ServiceException("导入数据不能为空！");
        }

        int successNum = 0;
        int failureNum = 0;
        List<String> successMessages = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (ModelDictTypeRespVO respVO : importExcelList) {
            try {
                ModelDictTypeDO modelDictTypeDO = BeanUtils.toBean(respVO, ModelDictTypeDO.class);
                Long modelDictTypeId = respVO.getId();
                if (isUpdateSupport) {
                    if (modelDictTypeId != null) {
                        ModelDictTypeDO existingModelDictType = modelDictTypeMapper.selectById(modelDictTypeId);
                        if (existingModelDictType != null) {
                            modelDictTypeMapper.updateById(modelDictTypeDO);
                            successNum++;
                            successMessages.add("数据更新成功，ID为 " + modelDictTypeId + " 的参考数据记录。");
                        } else {
                            failureNum++;
                            failureMessages.add("数据更新失败，ID为 " + modelDictTypeId + " 的参考数据记录不存在。");
                        }
                    } else {
                        failureNum++;
                        failureMessages.add("数据更新失败，某条记录的ID不存在。");
                    }
                } else {
                    QueryWrapper<ModelDictTypeDO> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("id", modelDictTypeId);
                    ModelDictTypeDO existingModelDictType = modelDictTypeMapper.selectOne(queryWrapper);
                    if (existingModelDictType == null) {
                        modelDictTypeMapper.insert(modelDictTypeDO);
                        successNum++;
                        successMessages.add("数据插入成功，ID为 " + modelDictTypeId + " 的参考数据记录。");
                    } else {
                        failureNum++;
                        failureMessages.add("数据插入失败，ID为 " + modelDictTypeId + " 的参考数据记录已存在。");
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
