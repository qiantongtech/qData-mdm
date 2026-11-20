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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.constant.HttpStatus;
import tech.qiantong.qdata.mdm.common.core.page.PageParam;
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
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictTypeDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.dict.ModelDictDataMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.dict.ModelDictTypeMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;
import tech.qiantong.qdata.mdm.model.service.dict.IModelDictDataService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    private ModelDictTypeMapper modelDictTypeMapper;

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
            throw new ServiceException("参考数据类型ID不能为空", HttpStatus.WARN);
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
    public List<ModelDictDataRespVO> getModelDictDataExportRows(ModelDictDataPageReqVO exportReqVO, String typeCode) {
        ModelDictTypeDO type = resolveTypeByCode(typeCode);
        exportReqVO.setTypeId(type.getId());
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ModelDictDataRespVO> rows = BeanUtils.toBean(getModelDictDataPage(exportReqVO).getRows(), ModelDictDataRespVO.class);
        rows.forEach(row -> row.setTypeCode(type.getCode()));
        return rows;
    }

    @Override
    public Long createModelDictData(ModelDictDataSaveReqVO request) {
        validateCode(request);
        ModelDictDataDO item = BeanUtils.toBean(request, ModelDictDataDO.class);
        modelDictDataMapper.insert(item);
        return item.getId();
    }

    @Override
    public int updateModelDictData(ModelDictDataSaveReqVO request) {
        validateCode(request);
        return modelDictDataMapper.updateById(BeanUtils.toBean(request, ModelDictDataDO.class));
    }

    private void validateCode(ModelDictDataSaveReqVO request) {
        String code = normalizeCode(request.getCode());
        ModelDictTypeDO type = request.getTypeId() == null ? null : modelDictTypeMapper.selectById(request.getTypeId());
        if (type == null) {
            throw new ServiceException("所属参考数据集合不存在", HttpStatus.WARN);
        }
        request.setCode(code);
        if (modelDictDataMapper.selectCount(codeQuery(request.getTypeId(), code)
                .ne(request.getId() != null, "id", request.getId())) > 0) {
            throw new ServiceException("参考数据集合“" + type.getCode() + "”中的项编码“" + code + "”已存在", HttpStatus.WARN);
        }
    }

    private String normalizeCode(String value) {
        String code = value == null ? "" : value.trim();
        if (code.isEmpty()) throw new ServiceException("项编码不能为空", HttpStatus.WARN);
        if (code.length() > 64) throw new ServiceException("项编码不能超过64个字符", HttpStatus.WARN);
        return code;
    }

    private QueryWrapper<ModelDictDataDO> codeQuery(Long typeId, String code) {
        return new QueryWrapper<ModelDictDataDO>().eq("type_id", typeId)
                .apply("LOWER(TRIM(code)) = {0}", code.toLowerCase(Locale.ROOT));
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
        if (idList == null || idList.isEmpty()) return 0;
        // 字段通过所属参考数据集合引用数据项，与详情页的引用关系统一。
        List<ModelDictDataDO> referencedItems = modelDictDataMapper.selectList(new LambdaQueryWrapper<ModelDictDataDO>()
                .select(ModelDictDataDO::getId, ModelDictDataDO::getCode, ModelDictDataDO::getName)
                .in(ModelDictDataDO::getId, idList)
                .exists("SELECT 1 FROM MODEL_COLUMN mc JOIN MODEL_BASE mb ON mb.ID = mc.MODEL_ID "
                        + "WHERE mc.DICT_TYPE_ID = MODEL_DICT_DATA.TYPE_ID AND mc.DEL_FLAG = '0' AND mb.DEL_FLAG = '0'")
                .orderByAsc(ModelDictDataDO::getId));
        if (!referencedItems.isEmpty()) {
            ModelDictDataDO item = referencedItems.get(0);
            String name = StringUtils.defaultIfEmpty(item.getName(),
                    StringUtils.defaultIfEmpty(item.getCode(), String.valueOf(item.getId())));
            throw new ServiceException(name + "数据项存在引用", HttpStatus.WARN);
        }
        if (modelDictDataMapper.selectCount(new LambdaQueryWrapper<ModelDictDataDO>()
                .in(ModelDictDataDO::getId, idList).eq(ModelDictDataDO::getValidFlag, Boolean.TRUE)) > 0) {
            throw new ServiceException("删除参考数据失败,请先停用!", HttpStatus.WARN);
        }
        return modelDictDataMapper.deleteByIds(idList);
    }

    @Override
    public ModelDictDataDO getModelDictDataById(Long id) {
        return modelDictDataMapper.selectById(id);
    }

    @Override
    public String importModelDictData(List<ModelDictDataRespVO> rows, String typeCode, boolean updateSupport, String operName) {
        if (rows == null || rows.isEmpty()) throw new ServiceException("导入数据不能为空", HttpStatus.WARN);

        ModelDictTypeDO selectedType = StringUtils.isBlank(typeCode) ? null : resolveTypeByCode(typeCode);
        Map<String, ModelDictTypeDO> typesByCode = new HashMap<>();
        Set<String> codes = new HashSet<>();
        Set<Long> updatedIds = new HashSet<>();
        int updated = 0;
        Date now = new Date();
        for (int index = 0; index < rows.size(); index++) {
            try {
                ModelDictDataRespVO row = rows.get(index);
                String rowTypeCode = StringUtils.trimToEmpty(row.getTypeCode());
                if (selectedType != null && !rowTypeCode.isEmpty() && !selectedType.getCode().trim().equalsIgnoreCase(rowTypeCode)) {
                    throw new ServiceException("文件中的参考数据类型编码与当前集合不一致", HttpStatus.WARN);
                }
                Long itemTypeId = selectedType == null
                        ? typesByCode.computeIfAbsent(rowTypeCode.toLowerCase(Locale.ROOT), key -> resolveTypeByCode(rowTypeCode)).getId()
                        : selectedType.getId();
                String code = normalizeCode(row.getCode());
                if (StringUtils.isBlank(row.getName())) throw new ServiceException("项名称不能为空", HttpStatus.WARN);
                if (!codes.add(itemTypeId + ":" + code.toLowerCase(Locale.ROOT))) {
                    throw new ServiceException("文件中同一参考数据集合的项编码重复", HttpStatus.WARN);
                }

                ModelDictDataSaveReqVO request = new ModelDictDataSaveReqVO();
                request.setTypeId(itemTypeId);
                request.setCode(code);
                request.setName(row.getName());
                request.setSortOrder(row.getSortOrder());
                request.setDescription(row.getDescription());
                request.setValidFlag(row.getValidFlag());
                if (updateSupport) request.setId(resolveImportId(row.getId(), itemTypeId, code));
                if (request.getId() == null) {
                    if (request.getValidFlag() == null) request.setValidFlag(Boolean.TRUE);
                    request.setCreateBy(operName);
                    request.setCreateTime(now);
                    createModelDictData(request);
                } else {
                    if (!updatedIds.add(request.getId())) throw new ServiceException("文件中同一数据项记录重复", HttpStatus.WARN);
                    request.setUpdateBy(operName);
                    request.setUpdateTime(now);
                    if (updateModelDictData(request) != 1) throw new ServiceException("参考数据项记录不存在", HttpStatus.WARN);
                    updated++;
                }
            } catch (ServiceException ex) {
                throw new ServiceException("导入第" + (index + 1) + "条数据失败，" + ex.getMessage(), ex.getCode());
            }
        }
        return "导入成功，新增" + (rows.size() - updated) + "条，更新" + updated + "条";
    }

    private ModelDictTypeDO resolveTypeByCode(String code) {
        if (StringUtils.isBlank(code)) throw new ServiceException("参考数据类型编码不能为空", HttpStatus.WARN);
        List<ModelDictTypeDO> matches = modelDictTypeMapper.selectList(new LambdaQueryWrapper<ModelDictTypeDO>()
                .select(ModelDictTypeDO::getId, ModelDictTypeDO::getCode)
                .apply("LOWER(TRIM(code)) = {0}", code.trim().toLowerCase(Locale.ROOT)));
        if (matches.isEmpty()) throw new ServiceException("参考数据类型编码不存在", HttpStatus.WARN);
        if (matches.size() > 1) {
            throw new ServiceException("参考数据集合编码“" + code.trim() + "”对应多条集合", HttpStatus.WARN);
        }
        return matches.get(0);
    }

    private Long resolveImportId(Long id, Long typeId, String code) {
        if (id != null) {
            ModelDictDataDO existing = modelDictDataMapper.selectById(id);
            if (existing == null || !Objects.equals(existing.getTypeId(), typeId)) {
                throw new ServiceException("当前参考数据集合中不存在该数据项编号", HttpStatus.WARN);
            }
            return id;
        }
        List<ModelDictDataDO> matches = modelDictDataMapper.selectList(codeQuery(typeId, code).select("id"));
        if (matches.size() > 1) throw new ServiceException("项编码对应多条记录，请使用编号更新");
        return matches.isEmpty() ? null : matches.get(0).getId();
    }
}
