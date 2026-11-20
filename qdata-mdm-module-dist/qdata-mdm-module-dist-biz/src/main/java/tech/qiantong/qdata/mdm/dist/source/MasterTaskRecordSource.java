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

package tech.qiantong.qdata.mdm.dist.source;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDataSyncReqDTO;
import tech.qiantong.qdata.mdm.master.service.data.service.MasterDataApi;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedColumnRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedStructureRespDTO;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

/**
 * 真实主数据取数适配器：全量和增量统一调用时间点同步接口，再把字段编码转换为
 * 分发筛选和字段映射使用的模型字段 ID。
 */
@Component
public class MasterTaskRecordSource implements DistTaskRecordSource {
    private static final Date FULL_SYNC_START = new Date(0L);

    /** 主数据时间点同步接口。 */

 @Resource
 private MasterDataApi masterDataApi;

    /** 成功发布的模型字段结构接口。 */
    @Resource
    private ModelPublishedApi modelPublishedApi;

    /**
     * 仅处理已发布的真实模型；历史示例模型不参与执行。
     * @param modelId 任务选择的主数据模型 ID
     * @return 是否可处理该模型
     */
    @Override
    public boolean supports(Long modelId) {
        return modelId != null && modelId > 0;
    }

    /**
     * 固定模型成功发布版本，按全量或增量方式读取时间点之后的数据。
     * @param task 待执行的分发任务，提供模型及发布版本 ID
     * @param cutoff 本批次开始时间，用于增量记录的上界校验
     * @return 同步接口返回的记录序列
     */
    @Override
    public Iterable<Record> read(DistTaskDO task, Date cutoff) {

        if (task.getModelVersionId() == null) {
            throw new ServiceException("任务缺少已发布模型版本，请重新选择主数据模型");
        }
        ModelPublishedStructureRespDTO structure = modelPublishedApi.getPublishedModelStructure(task.getModelId());
        if (structure == null || !task.getModelVersionId().equals(structure.getVersionId())) {
            throw new ServiceException("主数据模型发布版本已变化，请重新配置分发任务");
        }
        List<ModelPublishedColumnRespDTO> columns = structure.getFields();
        if (columns == null || columns.isEmpty()) {
            throw new ServiceException("已发布主数据模型没有可分发字段");
        }
        Date updatedAfter;
        boolean incremental = "INCREMENTAL".equals(task.getDataMode());
        if (incremental) {
            updatedAfter = task.getLastSuccessTime() == null
                    ? task.getIncrementalStartTime() : task.getLastSuccessTime();
            if (updatedAfter == null) {
                throw new ServiceException("增量任务缺少开始时间");
            }
            if (cutoff == null || updatedAfter.after(cutoff)) {
                throw new ServiceException("增量任务的时间范围不正确");
            }
        } else if ("FULL".equals(task.getDataMode())) {
            updatedAfter = FULL_SYNC_START;
        } else {
            throw new ServiceException("分发模式不正确");
        }

        MasterDataSyncReqDTO request = new MasterDataSyncReqDTO();
        request.setModelId(task.getModelId());
        request.setModelVersionId(task.getModelVersionId());
        request.setUpdatedAfter(updatedAfter);
        // 不传状态，按主数据同步接口约定读取全部状态。
        List<Map<String, Object>> rows = masterDataApi.getMasterDataSyncList(request);
        if (rows == null) {
            throw new ServiceException("主数据同步接口未返回记录列表");
        }

        List<Record> records = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            if (row == null) {
                throw new ServiceException("主数据同步接口返回了空记录");
            }
            Date updateTime = incremental ? getUpdateTime(row) : null;
            records.add(toRecord(row, columns, updateTime));
        }

        return records;

    }

    /**
     * 按已发布模型字段建立 ID 到值的映射，并保留增量变更时间。
     *
     * @param row 主数据接口返回的记录
     * @param columns 已发布模型字段
     * @param updateTime 增量记录的更新时间；全量记录为 null
     * @return 可供筛选和字段映射使用的分发记录
     */
    private Record toRecord(Map<String, Object> row, List<ModelPublishedColumnRespDTO> columns, Date updateTime) {
        if (row == null) {
            throw new ServiceException("主数据同步接口返回了空记录");
        }
        Object rawId = value(row, "ID");
        Long id;
        try {
            id = Long.valueOf(String.valueOf(rawId));
        } catch (NumberFormatException ex) {
            throw new ServiceException("主数据记录缺少有效 ID");
        }
        Map<Long, Object> fields = new LinkedHashMap<>();
        String name = null;
        String code = null;
        for (ModelPublishedColumnRespDTO column : columns) {
            if (column.getId() == null || column.getCode() == null) {
                continue;
            }
            String fieldCode = column.getCode();
            if (!contains(row, fieldCode)) {
                continue;
            }
            Object fieldValue = value(row, fieldCode);
            fields.put(column.getId(), fieldValue);
            if (Boolean.TRUE.equals(column.getNameFlag()) && fieldValue != null) {
                name = String.valueOf(fieldValue);
            }
            if ("CODE".equalsIgnoreCase(fieldCode) && fieldValue != null) {
                code = String.valueOf(fieldValue);
            }
        }
        return new Record(id, code == null ? String.valueOf(id) : code,
                name == null ? String.valueOf(id) : name, updateTime, fields);
    }

    /** 同步接口将系统时间格式化为带毫秒的字符串；增量过滤需要还原为 Date。 */
    private Date getUpdateTime(Map<String, Object> row) {
        Object rawTime = firstValue(row, "UPDATED_TIME", "UPDATE_TIME", "CREATED_TIME", "CREATE_TIME");
        if (rawTime instanceof Date) {
            return (Date) rawTime;
        }
        if (rawTime == null) {
            throw new ServiceException("主数据增量记录缺少更新时间和创建时间");
        }

        String value = String.valueOf(rawTime).trim();
        for (String pattern : new String[]{"yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss"}) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
            format.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = format.parse(value, position);
            if (parsed != null && position.getIndex() == value.length()) {
                return parsed;
            }
        }
        throw new ServiceException("主数据增量记录的更新时间格式不正确: " + value);
    }

    private Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = value(row, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 不区分大小写检查主数据记录是否包含模型字段编码。
     *
     * @param row 主数据记录
     * @param code 模型字段编码
     * @return 包含该字段时为 true
     */
    private boolean contains(Map<String, Object> row, String code) {
        return row.keySet().stream().anyMatch(key -> key != null && key.toUpperCase(Locale.ROOT).equals(code.toUpperCase(Locale.ROOT)));
    }

    /**
     * 不区分大小写读取字段值，兼容数据库驱动返回的大写列名。
     *
     * @param row 主数据记录
     * @param code 模型字段编码
     * @return 字段值；不存在时返回 null
     */
    private Object value(Map<String, Object> row, String code) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(code)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
