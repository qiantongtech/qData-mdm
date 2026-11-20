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

package tech.qiantong.qdata.mdm.model.service.dict.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
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

import tech.qiantong.qdata.mdm.model.util.ModelCodeGuard;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
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
    private Validator validator;

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
    public Long createModelDictType(ModelDictTypeSaveReqVO request) {
        ModelCodeGuard.DICT_TYPE.lockTransaction();
        validateCode(request);
        ModelDictTypeDO type = BeanUtils.toBean(request, ModelDictTypeDO.class);
        ModelCodeGuard.DICT_TYPE.write(type.getCode(), () -> modelDictTypeMapper.insert(type));
        return type.getId();
    }

    @Override
    public int updateModelDictType(ModelDictTypeSaveReqVO request) {
        ModelCodeGuard.DICT_TYPE.lockTransaction();
        validateCode(request);
        ModelDictTypeDO type = BeanUtils.toBean(request, ModelDictTypeDO.class);
        return ModelCodeGuard.DICT_TYPE.write(type.getCode(), () -> modelDictTypeMapper.updateById(type));
    }

    private void validateCode(ModelDictTypeSaveReqVO request) {
        String code = normalizeCode(request.getCode());
        request.setCode(code);
        if (modelDictTypeMapper.selectCount(codeQuery(code).ne(request.getId() != null, "id", request.getId())) > 0) {
            throw ModelCodeGuard.DICT_TYPE.duplicateCode(code);
        }
    }

    private String normalizeCode(String value) {
        String code = value == null ? "" : value.trim();
        if (code.isEmpty()) throw new ServiceException("参考数据编码不能为空", HttpStatus.WARN);
        if (code.length() > 64) throw new ServiceException("参考数据编码不能超过64个字符", HttpStatus.WARN);
        return code;
    }

    private QueryWrapper<ModelDictTypeDO> codeQuery(String code) {
        return new QueryWrapper<ModelDictTypeDO>().apply("LOWER(TRIM(code)) = {0}", code.toLowerCase(Locale.ROOT));
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


    @Override
    public String importModelDictType(List<ModelDictTypeRespVO> rows, boolean updateSupport, String operName) {
        ModelCodeGuard.DICT_TYPE.lockTransaction();
        if (rows == null || rows.isEmpty()) throw new ServiceException("导入数据不能为空", HttpStatus.WARN);

        Set<String> codes = new HashSet<>();
        Set<Long> updatedIds = new HashSet<>();
        int updated = 0;
        Date now = new Date();
        for (int index = 0; index < rows.size(); index++) {
            try {
                ModelDictTypeRespVO row = rows.get(index);
                String code = normalizeCode(row.getCode());
                if (!codes.add(code.toLowerCase(Locale.ROOT))) throw new ServiceException("文件中参考数据编码重复", HttpStatus.WARN);

                ModelDictTypeSaveReqVO request = new ModelDictTypeSaveReqVO();
                request.setCode(code);
                request.setName(row.getName());
                request.setDescription(row.getDescription());
                request.setValidFlag(row.getValidFlag());
                Set<ConstraintViolation<ModelDictTypeSaveReqVO>> violations = validator.validate(request);
                if (!violations.isEmpty()) throw new ServiceException(violations.iterator().next().getMessage());
                if (updateSupport) request.setId(resolveImportId(row.getId(), code));
                if (request.getId() == null) {
                    if (request.getValidFlag() == null) request.setValidFlag(Boolean.TRUE);
                    request.setCreateBy(operName);
                    request.setCreateTime(now);
                    createModelDictType(request);
                } else {
                    if (!updatedIds.add(request.getId())) throw new ServiceException("文件中同一参考数据记录重复", HttpStatus.WARN);
                    request.setUpdateBy(operName);
                    request.setUpdateTime(now);
                    if (updateModelDictType(request) != 1) throw new ServiceException("参考数据记录不存在", HttpStatus.WARN);
                    updated++;
                }
            } catch (ServiceException ex) {
                throw new ServiceException("导入第" + (index + 1) + "条数据失败，" + ex.getMessage());
            }
        }
        return "导入成功，新增" + (rows.size() - updated) + "条，更新" + updated + "条";
    }

    private Long resolveImportId(Long id, String code) {
        if (id != null) {
            if (modelDictTypeMapper.selectById(id) == null) throw new ServiceException("参考数据记录不存在", HttpStatus.WARN);
            return id;
        }
        // 旧版导出没有 ID，使用唯一编码定位原记录。
        List<ModelDictTypeDO> matches = modelDictTypeMapper.selectList(codeQuery(code).select("id"));
        if (matches.size() > 1) throw new ServiceException("参考数据编码对应多条记录，请使用编号更新");
        return matches.isEmpty() ? null : matches.get(0).getId();
    }
}
