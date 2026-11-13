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

package tech.qiantong.qdata.mdm.ingest.writer.task;

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.DbDialect;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.service.datasource.impl.IngestDatasourceServiceImpl;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializationRespDTO;
import tech.qiantong.qdata.mdm.model.base.dto.ModelPublishedMaterializedTableRespDTO;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelTableRole;
import tech.qiantong.qdata.mdm.model.service.base.service.ModelPublishedApi;

import javax.annotation.Resource;
import java.util.List;

/**
 * 来源表写入入口，在一次执行开始时解析目标并创建独立写入会话。
 */
@Component
public class IngestSourceTableWriter {
    @Resource
    private IngestDatasourceServiceImpl datasourceService;

    @Resource
    private ModelPublishedApi modelPublishedApi;

    /**
     * 从模型成功发布的物化信息中选择启用的来源表，并按目标数据源选取公共方言。
     * 将发布的 schema 与表名传入独立会话，物理字段校验、事务及逐行写入由会话处理。
     *
     * @param modelId 主数据模型编号
     * @param columns 模型的全部业务字段编码，未映射列写入 null
     * @return 由执行服务在运行结束后关闭的写入会话
     */
    public IngestSourceTableSession open(Long modelId, List<String> columns) {
        if (modelId == null) {
            throw new ServiceException("来源表写入缺少模型编号");
        }
        ModelPublishedMaterializationRespDTO materialization = modelPublishedApi.getPublishedModelMaterialization(modelId);
        if (materialization == null || materialization.getDatasourceId() == null || materialization.getTables() == null) {
            throw new ServiceException("模型未配置已发布的来源表写入目标");
        }
        ModelPublishedMaterializedTableRespDTO sourceTable = materialization.getTables().stream()
                .filter(table -> modelId.equals(table.getModelId())
                        && ModelTableRole.SOURCE.name().equals(table.getTableRole())
                        && Boolean.TRUE.equals(table.getEnabledFlag()))
                .findFirst().orElseThrow(() -> new ServiceException("模型没有已启用的来源表"));
        DbDialect dialect = DialectFactory.getDialect(DbType.getDbType(materialization.getDatabaseType()));
        DbQuery query = datasourceService.buildDbQuery(materialization.getDatasourceId());
        return IngestSourceTableSession.open(query, dialect, materialization.getSchemaName(), sourceTable.getTableName(), columns);
    }
}
