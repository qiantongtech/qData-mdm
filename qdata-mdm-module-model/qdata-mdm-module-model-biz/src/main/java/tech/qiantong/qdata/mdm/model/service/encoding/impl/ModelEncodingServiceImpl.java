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

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageRespDTO;
import tech.qiantong.qdata.mdm.master.service.data.service.MasterFieldUsageApi;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelColumnRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingUsageRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelBaseDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingSequenceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelBaseMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingItemMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingVersionMapper;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateReqDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingGenerateRespDTO;
import tech.qiantong.qdata.mdm.model.enums.ModelEncodingItemTypeEnum;
import tech.qiantong.qdata.mdm.model.service.base.IModelColumnService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingItemService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingVersionService;

import javax.annotation.Resource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 编码方案Service业务层处理
 *
 * @author qdata
 * @date 2026-09-16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelEncodingServiceImpl extends ServiceImpl<ModelEncodingMapper, ModelEncodingDO> implements IModelEncodingService {
    private static final int MAX_USAGE_REQUESTS = 100;

    @Resource
    private MasterFieldUsageApi masterFieldUsageApi;

    /**
     * 统一读写编码方案与流水状态，并在正式分配前锁定方案主记录。
     */
    @Resource
    private ModelEncodingMapper modelEncodingMapper;

    @Resource
    private IModelColumnService iModelColumnService;

    /**
     * 生成前校验编码方案版本的发布及生效状态。
     */
    @Resource
    private ModelEncodingVersionMapper versionMapper;

    /**
     * 按规则排序读取完整编码结构。
     */
    @Resource
    private ModelEncodingItemMapper itemMapper;

    /**
     * 校验调用方指定的使用模型是否存在。
     */
    @Resource
    private ModelBaseMapper modelMapper;

    /**
     * 日期段与周期重置共用上海时区，使用本次生成捕获的同一时间，避免跨日不一致。
     */
    private Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));

    @Override
    public boolean checkCodeUnique(String code, Long id) {
        QueryWrapper<ModelEncodingDO> wrapper = new QueryWrapper<>();
        wrapper.eq("code", code);
        if (id != null) wrapper.ne("id", id);
        return modelEncodingMapper.selectCount(wrapper) == 0;
    }

    @Resource
    private IModelEncodingVersionService modelEncodingVersionService;

    @Resource
    @Lazy
    private IModelEncodingItemService encodingItemService;

    @Override
    public PageResult<ModelEncodingDO> getModelEncodingPage(ModelEncodingPageReqVO pageReqVO) {
        PageResult<ModelEncodingDO> pageResult = modelEncodingMapper.selectPage(pageReqVO);
        List<ModelEncodingDO> rows = pageResult.getRows();
        if (rows == null || rows.isEmpty()) {
            return pageResult;
        }

        List<Long> versionIdList = rows.stream()
                .map(ModelEncodingDO::getCurrentVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (versionIdList.isEmpty()) {
            return pageResult;
        }

        Map<Long, ModelEncodingVersionDO> versionMap = modelEncodingVersionService.listByIds(versionIdList).stream()
                .collect(Collectors.toMap(
                        ModelEncodingVersionDO::getId,
                        modelEncodingVersionDO -> modelEncodingVersionDO,
                        (existing, replacement) -> existing
                ));

        Map<Long, Long> usageMap = iModelColumnService.list(new QueryWrapper<ModelColumnDO>()
                        .select("encoding_version_id")
                        .in("encoding_version_id", versionIdList)).stream()
                .filter(column -> column.getEncodingVersionId() != null)
                .collect(Collectors.groupingBy(ModelColumnDO::getEncodingVersionId, Collectors.counting()));
        rows.forEach(modelEncodingDO -> modelEncodingDO.setVersion(versionMap.get(modelEncodingDO.getCurrentVersionId())));
        rows.forEach(modelEncodingDO -> modelEncodingDO.setModelCount(String.valueOf(usageMap.getOrDefault(modelEncodingDO.getCurrentVersionId(), 0L))));
        return pageResult;
    }

    /**
     * 查询字段配置场景可选的编码方案，并补充当前版本信息。
     * Mapper 已完成“版本状态为 3”和“存在规则段”的数据库过滤。
     */
    @Override
    public PageResult<ModelEncodingDO> getAvailableModelEncodingPage(ModelEncodingPageReqVO pageReqVO) {
        PageResult<ModelEncodingDO> pageResult = modelEncodingMapper.selectAvailablePage(pageReqVO);
        List<ModelEncodingDO> rows = pageResult.getRows();
        if (rows == null || rows.isEmpty()) return pageResult;
        List<Long> versionIds = rows.stream().map(ModelEncodingDO::getCurrentVersionId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!versionIds.isEmpty()) {
            Map<Long, ModelEncodingVersionDO> versions = modelEncodingVersionService.listByIds(versionIds).stream()
                    .collect(Collectors.toMap(ModelEncodingVersionDO::getId, v -> v, (oldValue, newValue) -> oldValue));
            rows.forEach(row -> row.setVersion(versions.get(row.getCurrentVersionId())));
        }
        return pageResult;
    }

    @Override
    public Long createModelEncoding(ModelEncodingSaveReqVO createReqVO) {
        ModelEncodingDO dictType = BeanUtils.toBean(createReqVO, ModelEncodingDO.class);
        modelEncodingMapper.insert(dictType);

        ModelEncodingVersionSaveReqVO versionSaveReqVO = new ModelEncodingVersionSaveReqVO();
        versionSaveReqVO.setEncodingId(dictType.getId());
        versionSaveReqVO.setStatus(1);
        versionSaveReqVO.setRemark("初始版本");
        Long versionId = modelEncodingVersionService.createModelEncodingVersion(versionSaveReqVO);

        if (createReqVO.getItems() != null && !createReqVO.getItems().isEmpty()) {
            createReqVO.getItems().forEach(item -> {
                item.setCreatorId(createReqVO.getCreatorId());
                item.setCreateBy(createReqVO.getCreateBy());
                item.setCreateTime(createReqVO.getCreateTime());
            });
            encodingItemService.saveModelEncodingItems(versionId, createReqVO.getItems());
        }
        return dictType.getId();
    }

    @Override
    public int updateModelEncoding(ModelEncodingSaveReqVO updateReqVO) {
        // 相关校验

        // 更新编码方案
        ModelEncodingDO updateObj = BeanUtils.toBean(updateReqVO, ModelEncodingDO.class);
        return modelEncodingMapper.updateById(updateObj);
    }

    @Override
    public int removeModelEncoding(Collection<Long> idList) {
        // 批量删除编码方案
        return modelEncodingMapper.deleteByIds(idList);
    }

    @Override
    public long getModelCount(Long id) {
        ModelEncodingDO encoding = modelEncodingMapper.selectById(id);
        if (encoding == null || encoding.getCurrentVersionId() == null) return 0L;
        return iModelColumnService.count(new QueryWrapper<ModelColumnDO>()
                .eq("encoding_version_id", encoding.getCurrentVersionId()));
    }

    @Override
    public boolean hasModelReference(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;
        List<Long> versionIds = modelEncodingMapper.selectBatchIds(ids).stream()
                .map(ModelEncodingDO::getCurrentVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return !versionIds.isEmpty()
                && iModelColumnService.count(new QueryWrapper<ModelColumnDO>()
                .in("encoding_version_id", versionIds)) > 0;
    }

    @Override
    public boolean hasPublishedVersion(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;
        List<Long> versionIds = modelEncodingMapper.selectBatchIds(ids).stream()
                .map(ModelEncodingDO::getCurrentVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return !versionIds.isEmpty()
                && modelEncodingVersionService.lambdaQuery()
                .in(ModelEncodingVersionDO::getId, versionIds)
                .eq(ModelEncodingVersionDO::getStatus, 3)
                .count() > 0;
    }

    @Override
    public List<ModelEncodingUsageRespVO> getUsageByVersionId(Long encodingVersionId) {
        if (encodingVersionId == null) return Collections.emptyList();
        List<ModelColumnDO> columns = iModelColumnService.list(new QueryWrapper<ModelColumnDO>()
                .eq("encoding_version_id", encodingVersionId).orderByAsc("sort_order", "id"));
        Map<Long, List<ModelColumnDO>> columnsByModel = columns.stream()
                .filter(column -> column.getModelId() != null)
                .collect(Collectors.groupingBy(ModelColumnDO::getModelId));
        if (columnsByModel.isEmpty()) return Collections.emptyList();

        // 与参考数据引用共用批量模型查询，返回模型当前状态及正式版本，过滤孤立引用。
        List<ModelEncodingUsageRespVO> result = modelMapper.selectModelsWithVersionsByIds(columnsByModel.keySet()).stream()
                .sorted(Comparator.comparing(ModelBaseDO::getCreateTime,
                        Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ModelBaseDO::getId))
                .map(model -> {
                    ModelEncodingUsageRespVO usage = BeanUtils.toBean(model, ModelEncodingUsageRespVO.class);
                    usage.setFields(BeanUtils.toBean(columnsByModel.get(model.getId()), ModelColumnRespVO.class));
                    return usage;
                }).collect(Collectors.toList());
        fillFieldUsageCounts(encodingVersionId, result);
        return result;
    }

    /**
     * 批量统计当前编码版本下引用字段已填写的主数据条数，并按模型、字段写回。
     *
     * @param encodingVersionId 当前查询的编码方案版本 ID
     * @param models 已查出的引用模型及字段
     */
    private void fillFieldUsageCounts(Long encodingVersionId, List<ModelEncodingUsageRespVO> models) {
        Map<Long, ModelEncodingUsageRespVO> modelsById = new LinkedHashMap<>();
        List<MasterEncodingUsageReqDTO> requests = new ArrayList<>();
        for (ModelEncodingUsageRespVO model : models) {
            modelsById.put(model.getId(), model);
            for (ModelColumnRespVO field : model.getFields()) {
                MasterEncodingUsageReqDTO request = new MasterEncodingUsageReqDTO();
                request.setModelId(model.getId());
                request.setFieldId(field.getId());
                request.setEncodingVersionId(encodingVersionId);
                requests.add(request);
            }
        }
        for (int start = 0; start < requests.size(); start += MAX_USAGE_REQUESTS) {
            List<MasterEncodingUsageReqDTO> batch = requests.subList(start,
                    Math.min(start + MAX_USAGE_REQUESTS, requests.size()));
            for (MasterEncodingUsageRespDTO count : masterFieldUsageApi.countEncodingUsageBatch(batch)) {
                if (Objects.equals(encodingVersionId, count.getEncodingVersionId())) {
                    modelsById.get(count.getModelId()).getFieldUsageCounts().put(count.getFieldId(), count.getUsageCount());
                }
            }
        }
    }

    @Override
    public ModelEncodingDO getModelEncodingById(Long id) {
        ModelEncodingDO modelEncodingDO = modelEncodingMapper.selectById(id);
        if (modelEncodingDO != null && modelEncodingDO.getCurrentVersionId() != null) {
            modelEncodingDO.setVersion(modelEncodingVersionService.getModelEncodingVersionById(modelEncodingDO.getCurrentVersionId()));
        }
        return modelEncodingDO;
    }

    /**
     * 校验全部输入后正式生成编码。独立事务确保号码不因调用方回滚而回收；
     * READ_COMMITTED保证等待方案锁后能读取最新流水状态。
     *
     * @param request 方案版本、使用模型及动态字段值
     * @return 已持久化的编码及本次各流水段数字值
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public ModelEncodingGenerateRespDTO generate(ModelEncodingGenerateReqDTO request) {
        // 版本决定规则内容，模型决定独立计数归属；两者缺失时不能定位正确的流水状态。
        if (request == null || request.getEncodingVersionId() == null || request.getModelId() == null
                || request.getEncodingVersionId() <= 0 || request.getModelId() <= 0) {
            throw new ServiceException("编码方案版本ID和使用模型ID必须为正整数");
        }
        ModelEncodingVersionDO version = versionMapper.selectById(request.getEncodingVersionId());
        validateVersion(version);
        // 锁方案主记录，确保尚无流水行的首次并发调用也不会重复起号。
        if (modelEncodingMapper.lockScheme(version.getEncodingId()) == null) {
            throw new ServiceException("编码方案不存在");
        }
        // 获得方案锁之后重新读取，避免等待锁期间版本已停用。
        version = versionMapper.selectById(request.getEncodingVersionId());
        validateVersion(version);
        ModelEncodingDO scheme = modelEncodingMapper.selectById(version.getEncodingId());
        // 父方案停用或删除后，其已发布版本也不能继续分配号码。
        if (scheme == null || Boolean.TRUE.equals(scheme.getDelFlag()) || Boolean.FALSE.equals(scheme.getValidFlag())) {
            throw new ServiceException("编码方案不存在或已停用");
        }
        ModelBaseDO model = modelMapper.selectById(request.getModelId());
        if (model == null || Boolean.TRUE.equals(model.getDelFlag())) {
            throw new ServiceException("使用模型不存在或已删除");
        }
        List<ModelEncodingItemDO> items = itemMapper.selectList(new LambdaQueryWrapper<ModelEncodingItemDO>()
                .eq(ModelEncodingItemDO::getEncodingVersionId, version.getId())
                .orderByAsc(ModelEncodingItemDO::getSortOrder).orderByAsc(ModelEncodingItemDO::getId));
        if (items.isEmpty()) throw new ServiceException("编码方案未配置规则段");
        Map<String, Deque<String>> business = parameters(request.getBusinessFields(), "业务字段");
        Map<String, Deque<String>> references = parameters(request.getReferenceMappings(), "参考数据映射");
        ModelEncodingGenerateRespDTO result = new ModelEncodingGenerateRespDTO();
        result.setEncodingId(scheme.getId());
        result.setEncodingVersionId(version.getId());
        result.setModelId(request.getModelId());
        StringBuilder code = new StringBuilder();
        LocalDateTime now = LocalDateTime.now(clock);
        // 先暂存各流水段的新进度和是否已存在，全部规则校验通过后再统一写库。
        List<ModelEncodingSequenceDO> states = new ArrayList<>();
        List<Boolean> existing = new ArrayList<>();
        Set<Long> positions = new HashSet<>();
        for (ModelEncodingItemDO item : items) {
            // 兼容历史补零标记和固定宽度字符字段，规则含义及流水归属保持一致。
            if (item.getResetMethod() != null) item.setResetMethod(item.getResetMethod().trim());
            if (item.getPadChar() != null) {
                String padChar = item.getPadChar().trim();
                item.setPadChar("0".equals(padChar) ? "LEFT_ZERO" : padChar);
            }
            // 不跳过已停用规则段，避免静默生成缺少某一段的错误编码。
            if (Boolean.TRUE.equals(item.getDelFlag()) || Boolean.FALSE.equals(item.getValidFlag())) {
                throw new ServiceException("编码方案包含已停用规则段：" + item.getId());
            }
            // 排序位置既决定拼接顺序也参与流水主键，重复位置会导致不同段共用同一进度。
            if (item.getSortOrder() == null || item.getSortOrder() < 1 || !positions.add(item.getSortOrder())) {
                throw new ServiceException("编码规则排序号必须为不重复的正整数");
            }
            ModelEncodingItemTypeEnum type = ModelEncodingItemTypeEnum.getByValue(item.getType());
            if (type == null) throw new ServiceException("未知编码规则类型：" + item.getType());
            switch (type) {
                case FIXED_TEXT:
                    code.append(required(item.getFixedValue(), "固定内容"));
                    break;
                // 业务字段与参考映射分别从各自输入中按fixedValue精确匹配，不依赖不同参数的传入顺序。
                case BUSINESS_FIELD:
                    code.append(take(business, item.getFixedValue(), "业务字段"));
                    break;
                case REFERENCE_MAPPING:
                    code.append(take(references, item.getFixedValue(), "参考数据映射"));
                    break;
                case DATETIME:
                    try {
                        code.append(now.format(DateTimeFormatter.ofPattern(required(item.getFixedValue(), "日期格式"))));
                    } catch (IllegalArgumentException | java.time.DateTimeException ex) {
                        throw new ServiceException("日期时间规则格式无效：" + item.getFixedValue());
                    }
                    break;
                case SERIAL_NUMBER:
                    validateSerial(item);
                    ModelEncodingSequenceDO state = new ModelEncodingSequenceDO();
                    state.setEncodingId(scheme.getId());
                    state.setModelId(request.getModelId());
                    // 按排序位置归属流水，新版本规则段更换ID后仍延续原进度。
                    state.setSortOrder(item.getSortOrder());
                    // 周期变化时查找另一条状态记录，从起始值重新分配；旧周期记录保留以防时间回拨。
                    state.setPeriodKey(period(item.getResetMethod(), now));
                    ModelEncodingSequenceDO previous = modelEncodingMapper.selectState(state);
                    long value;
                    try {
                        // 首次使用起始值；后续基于已提交进度加步长，精确加法在溢出时拒绝分配。
                        value = previous == null ? item.getSequenceStart()
                                : Math.addExact(previous.getCurrentValue(), item.getStepSize());
                    } catch (ArithmeticException ex) {
                        throw new ServiceException("流水号已超出数值范围");
                    }
                    // 超出配置位数时直接失败，不能通过截断或回绕重用已发放的号码。
                    if (value < 0 || String.valueOf(value).length() > item.getSequenceLength()) {
                        throw new ServiceException("流水号超出配置位数，规则段：" + item.getId());
                    }
                    String number = String.valueOf(value);
                    // 补零只影响编码展示，数据库始终保存原始数字，便于下次继续加步长。
                    if ("LEFT_ZERO".equals(item.getPadChar())) {
                        StringBuilder padded = new StringBuilder();
                        for (int n = number.length(); n < item.getSequenceLength(); n++) padded.append('0');
                        number = padded.append(number).toString();
                    }
                    code.append(number);
                    state.setCurrentValue(value);
                    state.setEncodingVersionId(version.getId());
                    state.setItemId(item.getId());
                    state.setConfigSnapshot(JSON.toJSONString(item));
                    states.add(state);
                    existing.add(previous != null);
                    ModelEncodingGenerateRespDTO.SerialValue serial = new ModelEncodingGenerateRespDTO.SerialValue();
                    serial.setItemId(item.getId());
                    serial.setSortOrder(item.getSortOrder());
                    serial.setPeriodKey(state.getPeriodKey());
                    serial.setValue(value);
                    result.getSerialValues().add(serial);
                    break;
                default:
                    throw new ServiceException("不支持的编码规则类型");
            }
        }
        // 每个动态规则段恰好消费一条输入；仍有剩余表示多传或配置值不匹配，整次拒绝分配。
        if (!business.isEmpty() || !references.isEmpty()) {
            throw new ServiceException("传入的业务字段或参考数据映射数量超过配置，或配置值不匹配");
        }
        // 所有字段与规则校验通过后一次事务写入；任何段失败均不产生部分分配。
        for (int i = 0; i < states.size(); i++) {
            int affected = existing.get(i) ? modelEncodingMapper.updateState(states.get(i))
                    : modelEncodingMapper.insertState(states.get(i));
            // 任一段未准确写入一行均抛异常，由独立事务回滚本次全部流水段的修改。
            if (affected != 1) throw new ServiceException("保存流水号状态失败");
        }
        result.setCode(code.toString());
        return result;
    }

    /**
     * 正式生成只允许使用已发布、未停用且已到生效时间的版本。
     *
     * @param version 待校验的编码方案版本，允许为空以统一处理查询不到的情况
     * @throws ServiceException 版本缺失、归属无效、已删除、已停用、未发布或尚未生效
     */
    private void validateVersion(ModelEncodingVersionDO version) {
        if (version == null || version.getEncodingId() == null || Boolean.TRUE.equals(version.getDelFlag())
                || Boolean.FALSE.equals(version.getValidFlag()) || !Integer.valueOf(3).equals(version.getStatus())
                || (version.getEffectiveTime() != null && version.getEffectiveTime().toInstant().isAfter(clock.instant()))) {
            throw new ServiceException("编码方案版本不存在、未发布、已停用或尚未生效");
        }
    }

    /**
     * 按配置值分组，同名配置保留每条输入，使每个动态规则段恰好消费一个值。
     *
     * @param fields 调用方输入；空列表或null表示没有提供此类动态参数
     * @param label  参数类别名称，用于业务异常提示
     * @return 配置值到实际值队列的映射，同名输入按出现顺序排列
     * @throws ServiceException 条目、配置值或实际值为空
     */
    private Map<String, Deque<String>> parameters(List<ModelEncodingGenerateReqDTO.FieldValue> fields, String label) {
        Map<String, Deque<String>> values = new HashMap<>();
        if (fields != null) {
            for (ModelEncodingGenerateReqDTO.FieldValue field : fields) {
                if (field == null) throw new ServiceException(label + "参数不能为空");
                String key = required(field.getConfigValue(), label + "配置值");
                values.computeIfAbsent(key, k -> new ArrayDeque<>()).add(required(field.getValue(), label + "实际值"));
            }
        }
        return values;
    }

    /**
     * 按配置取值并移除已消费输入，最终剩余参数表示多传或配置不匹配。
     *
     * @param parameters 当前参数类别中尚未消费的实际值
     * @param key        规则段fixedValue保存的配置值，精确匹配输入configValue
     * @param label      参数类别名称，用于业务异常提示
     * @return 对应配置值下一条尚未消费的实际值
     * @throws ServiceException 规则配置为空或缺少对应输入
     */
    private String take(Map<String, Deque<String>> parameters, String key, String label) {
        required(key, label + "规则配置值");
        Deque<String> values = parameters.get(key);
        if (values == null || values.isEmpty()) throw new ServiceException("缺少" + label + "参数：" + key);
        String value = values.removeFirst();
        if (values.isEmpty()) parameters.remove(key);
        return value;
    }

    /**
     * 校验字符串必填，并保留原始内容，避免自动去空格改变业务值或编码内容。
     *
     * @param value 待校验内容
     * @param label 字段名称，用于业务异常提示
     * @return 校验通过的原字符串
     * @throws ServiceException 内容为null、空串或全为空白
     */
    private String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(label + "不能为空");
        return value;
    }

    /**
     * 校验流水长度、起始值、步长和补位方式；计算所得数字是否超出位数在分配时检查。
     *
     * @param item 当前流水规则段
     * @throws ServiceException 位数不在1～12、起始值为负、步长非正或补位配置不受支持
     */
    private void validateSerial(ModelEncodingItemDO item) {
        if (item.getSequenceLength() == null || item.getSequenceLength() < 1 || item.getSequenceLength() > 12
                || item.getSequenceStart() == null || item.getSequenceStart() < 0
                || item.getStepSize() == null || item.getStepSize() < 1) {
            throw new ServiceException("流水号位数必须为1～12，起始值不能为负，步长必须为正整数");
        }
        if (!"LEFT_ZERO".equals(item.getPadChar()) && !"NONE".equals(item.getPadChar())) {
            throw new ServiceException("流水号补位方式仅支持LEFT_ZERO或NONE");
        }
    }

    /**
     * 将周期纳入流水主键并保留历史进度，防止时间回拨重用已分配号码。
     * YEAR/YEARLY、MONTH/MONTHLY、DAY/DAILY分别使用相同的标准周期前缀。
     *
     * @param method 配置的重置周期
     * @param now    本次生成统一取得的业务时间，与日期规则段保持一致
     * @return 流水周期键；不重置返回NONE，其他返回周期前缀及年月日信息
     * @throws ServiceException 重置周期缺失或不受支持
     */
    private String period(String method, LocalDateTime now) {
        if (method == null) throw new ServiceException("流水号重置周期不能为空");
        switch (method) {
            case "NONE":
                return "NONE";
            case "YEAR":
            case "YEARLY":
                return "YEAR:" + now.format(DateTimeFormatter.ofPattern("yyyy"));
            case "MONTH":
            case "MONTHLY":
                return "MONTH:" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "DAY":
            case "DAILY":
                return "DAY:" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            default:
                throw new ServiceException("不支持的流水号重置周期：" + method);
        }
    }
}
