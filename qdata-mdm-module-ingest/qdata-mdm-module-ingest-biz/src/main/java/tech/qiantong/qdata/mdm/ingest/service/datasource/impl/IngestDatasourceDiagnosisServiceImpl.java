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

package tech.qiantong.qdata.mdm.ingest.service.datasource.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.database.DialectFactory;
import tech.qiantong.qdata.mdm.common.database.DataSourceFactory;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.common.database.core.DbColumn;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.datasource.IngestDatasourceMapper;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceDiagnosisService;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestDatasourceDiagnosisServiceImpl implements IIngestDatasourceDiagnosisService {

    private static final int SQL_TIMEOUT_SECONDS = 5;
    private static final String DIAGNOSIS_MODE_FULL = "FULL";
    private static final String DIAGNOSIS_MODE_UNAVAILABLE = "UNAVAILABLE";

    private static final List<String> TABLE_PERMISSION_NAMES =
            Collections.unmodifiableList(Arrays.asList("SELECT", "INSERT", "UPDATE", "DELETE"));

    private final IngestDatasourceMapper daDatasourceMapper;

    private final DataSourceFactory dataSourceFactory;

    private final DiagnosisResultBuilder resultBuilder = new DiagnosisResultBuilder();

    private final DiagnosisNetworkChecker networkChecker = new DiagnosisNetworkChecker(resultBuilder);

    private final DiagnosisBlockedItemAppender blockedItemAppender =
            new DiagnosisBlockedItemAppender(resultBuilder);

    @Override
    public IngestDatasourceDiagnosisRespVO diagnose(IngestDatasourceDiagnosisReqVO reqVO) {
        long startedAt = System.currentTimeMillis();
        IngestDatasourceDiagnosisRespVO result = new IngestDatasourceDiagnosisRespVO();
        result.setDiagnosisId("DSD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        result.setCheckedAt(new Date());
        result.setDiagnosisMode(DIAGNOSIS_MODE_UNAVAILABLE);
        result.setFullySupported(false);

        IngestDatasourceSaveReqVO connectionConfig = resolveConnection(reqVO, result);
        if (connectionConfig == null) {
            return finish(result, startedAt);
        }

        String datasourceType = trimToNull(connectionConfig.getDatasourceType());
        result.setDatasourceType(datasourceType);
        DbType dbType = resolveDbType(datasourceType);
        boolean fullySupported = dbType != null;
        result.setDiagnosisMode(fullySupported ? DIAGNOSIS_MODE_FULL : DIAGNOSIS_MODE_UNAVAILABLE);
        result.setFullySupported(fullySupported);

        DbQueryProperty property = null;
        long configStartedAt = System.currentTimeMillis();
        try {
            if (dbType == null) {
                throw new IllegalArgumentException("仅支持关系型数据库连接");
            }
            property = new DbQueryProperty(connectionConfig.getDatasourceType(), connectionConfig.getIp(),
                    connectionConfig.getPort(), connectionConfig.getDatasourceConfig());
            property.viald();
            if (trimToNull(property.getHost()) == null || property.getPort() == null
                    || property.getPort() < 1 || property.getPort() > 65535) {
                throw new IllegalArgumentException("IP/域名或端口配置不正确");
            }
            addPass(result, "CONFIG_VALIDATION", "连接配置", "连接参数完整，将执行完整诊断", configStartedAt);
        } catch (Exception ex) {
            addFailure(result, "CONFIG_VALIDATION", "连接配置", ex, "CONFIG_INVALID",
                    "配置不完整时无法建立数据库连接");
            if (fullySupported) {
                blockedItemAppender.appendRemaining(result, dbType, "CONFIG_VALIDATION");
            }
            return finish(result, startedAt);
        }

        if (!checkDns(result, property.getHost())) {
            blockedItemAppender.appendRemaining(result, dbType, "DNS_RESOLVE");
            return finish(result, startedAt);
        }
        if (!checkTcp(result, property.getHost(), property.getPort())) {
            blockedItemAppender.appendRemaining(result, dbType, "TCP_CONNECT");
            return finish(result, startedAt);
        }

        DbQuery dbQuery = null;
        Connection connection = null;
        try {
            long authStartedAt = System.currentTimeMillis();
            try {
                dbQuery = dataSourceFactory.createDbQuery(property);
                connection = dbQuery.getConnection();
                addPass(result, "AUTHENTICATION", "账号认证", "账号认证通过，数据库连接已建立", authStartedAt);
            } catch (Exception ex) {
                addFailure(result, "AUTHENTICATION", "账号认证", ex, "AUTHENTICATION_FAILED",
                        "认证失败时无法访问数据库及读取元数据");
                blockedItemAppender.appendRemaining(result, dbType, "AUTHENTICATION");
                return finish(result, startedAt);
            }

            if (!checkTargetResourceAccess(result, connection, property, dbType, reqVO)) {
                blockedItemAppender.appendRemaining(result, dbType, "TARGET_RESOURCE_ACCESS");
                return finish(result, startedAt);
            }

            TableRef tableRef = checkMetadata(result, dbQuery, connection, property, dbType, reqVO);
            if (tableRef != null) {
                checkTableRead(result, connection, tableRef, dbType);
            }

            checkAccountPermission(result, connection, property, dbType, tableRef, reqVO);
        } finally {
            closeQuietly(connection);
            closeQuietly(dbQuery);
        }
        return finish(result, startedAt);
    }

    private IngestDatasourceSaveReqVO resolveConnection(IngestDatasourceDiagnosisReqVO reqVO,
                                                    IngestDatasourceDiagnosisRespVO result) {
        if (reqVO == null) {
            addFailure(result, "CONFIG_VALIDATION", "连接配置",
                    new IllegalArgumentException("诊断请求不能为空"), "CONFIG_INVALID", "无法执行诊断");
            return null;
        }
        boolean hasId = reqVO.getDatasourceId() != null;
        boolean hasConnection = reqVO.getConnection() != null;
        if (hasId == hasConnection) {
            addFailure(result, "CONFIG_VALIDATION", "连接配置",
                    new IllegalArgumentException("datasourceId 与 connection 必须且只能填写一个"),
                    "CONFIG_INVALID", "无法确定需要诊断的数据连接");
            return null;
        }
        if (hasConnection) {
            return reqVO.getConnection();
        }
        IngestDatasourceDO datasource = daDatasourceMapper.selectById(reqVO.getDatasourceId());
        if (datasource == null) {
            addFailure(result, "CONFIG_VALIDATION", "连接配置",
                    new IllegalArgumentException("数据连接不存在或已被删除"),
                    "DATASOURCE_NOT_FOUND", "无法读取数据连接配置");
            return null;
        }
        IngestDatasourceSaveReqVO connection = new IngestDatasourceSaveReqVO();
        connection.setId(datasource.getId());
        connection.setDatasourceType(datasource.getDatasourceType());
        connection.setIp(datasource.getIp());
        connection.setPort(datasource.getPort());
        connection.setDatasourceConfig(datasource.getDatasourceConfig());
        return connection;
    }

    private DbType resolveDbType(String datasourceType) {
        if (datasourceType == null) {
            return null;
        }
        try {
            return DbType.getDbType(datasourceType);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean checkTargetResourceAccess(IngestDatasourceDiagnosisRespVO result, Connection connection,
                                              DbQueryProperty property, DbType dbType,
                                              IngestDatasourceDiagnosisReqVO reqVO) {
        long startedAt = System.currentTimeMillis();
        try {
            String schema = trimToNull(reqVO.getSchemaName());
            if (schema != null && !schemaVisible(connection.getMetaData(), schema)) {
                throw new SQLException("配置的Schema/Owner不存在或当前账号不可见: " + schema, "42501");
            }
            String target = trimToNull(property.getDbName());
            if (target == null) {
                target = trimToNull(property.getSid());
            }
            String message = target == null
                    ? "已建立目标服务连接，当前账号可以访问默认资源"
                    : "目标资源 " + target + " 可以连接和使用";
            if (schema != null) {
                message += "，Schema/Owner " + schema + " 可访问";
            }
            addPass(result, "TARGET_RESOURCE_ACCESS", databaseResourceName(dbType), message, startedAt);
            return true;
        } catch (Exception ex) {
            addFailure(result, "TARGET_RESOURCE_ACCESS", databaseResourceName(dbType), ex,
                    "TARGET_RESOURCE_ACCESS_DENIED", "无法使用对应的数据连接及其相关功能");
            return false;
        }
    }

    private boolean schemaVisible(DatabaseMetaData metaData, String schema) throws SQLException {
        try {
            try (ResultSet schemas = metaData.getSchemas(null, schema)) {
                if (schemas.next()) {
                    return true;
                }
            }
        } catch (AbstractMethodError | SQLFeatureNotSupportedException ignored) {
            // 旧版驱动不支持带catalog/schemaPattern的方法，继续遍历全部Schema。
        }
        try (ResultSet schemas = metaData.getSchemas()) {
            while (schemas.next()) {
                if (schema.equalsIgnoreCase(schemas.getString("TABLE_SCHEM"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String databaseResourceName(DbType dbType) {
        if (dbType == DbType.ORACLE || dbType == DbType.ORACLE_12C) {
            return "服务名/SID及Schema访问";
        }

        if (dbType == DbType.DM8 || dbType == DbType.OCEAN_BASE_ORACLE) {
            return "Schema/Owner访问";
        }
        if (dbType == DbType.POSTGRE_SQL || dbType == DbType.KINGBASE8 || dbType == DbType.GBASE8C
                || dbType == DbType.OPEN_GAUSS || dbType == DbType.GAUSS_DB
                || dbType == DbType.DB2
                || dbType == DbType.SQL_SERVER || dbType == DbType.SQL_SERVER2008 || dbType == DbType.OSCAR) {
            return "Database及Schema访问";
        }
        if (dbType == DbType.GBase) {
            return "Database及Owner访问";
        }
        return "Database访问";
    }

    private boolean checkDns(IngestDatasourceDiagnosisRespVO result, String host) {
        return networkChecker.checkDns(result, host);
    }

    private boolean checkTcp(IngestDatasourceDiagnosisRespVO result, String host, Integer port) {
        return networkChecker.checkTcp(result, host, port);
    }

    private TableRef checkMetadata(IngestDatasourceDiagnosisRespVO result, DbQuery dbQuery, Connection connection,
                                   DbQueryProperty property, DbType dbType,
                                   IngestDatasourceDiagnosisReqVO reqVO) {
        long startedAt = System.currentTimeMillis();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            TableRef tableRef = findTable(metaData, property, dbType, reqVO);
            String product = metaData.getDatabaseProductName();
            if (tableRef == null) {
                addPass(result, "METADATA_READ", "元数据读取",
                        "可以读取" + product + "元数据，当前范围内未发现业务表", startedAt);
                addWarn(result, "STRUCTURE_READ", "字段结构读取",
                        "当前范围内没有可用于检查字段结构的表",
                        "资源发现可用，但字段类型、主键和扩展属性能力尚未确认",
                        Collections.singletonList("指定tableName或在目标资源中准备测试表后重新诊断"),
                        System.currentTimeMillis() - startedAt);
            } else {
                addPass(result, "METADATA_READ", "元数据读取",
                        "可以读取" + product + "元数据，已获取检查表 " + tableRef.tableName, startedAt);
                checkStructureMetadata(result, dbQuery, property, dbType, metaData, tableRef);
            }
            return tableRef;
        } catch (Exception ex) {
            addFailure(result, "METADATA_READ", "元数据读取", ex, "METADATA_READ_FAILED",
                    "无法发现数据库中的Schema和表结构");
            return null;
        }
    }

    private void checkStructureMetadata(IngestDatasourceDiagnosisRespVO result, DbQuery dbQuery,
                                        DbQueryProperty property, DbType dbType,
                                        DatabaseMetaData metaData, TableRef tableRef) {
        long startedAt = System.currentTimeMillis();

        try (ResultSet columns = metaData.getColumns(tableRef.catalog, tableRef.schema,
                tableRef.tableName, "%")) {
            int columnCount = 0;
            while (columns.next()) {
                columnCount++;
            }
            if (columnCount == 0) {
                throw new SQLException("指定表不存在、当前账号不可见或无法读取字段结构", "42501");
            }
            addPass(result, "STRUCTURE_READ", "字段结构读取",
                    "成功读取表 " + tableRef.tableName + " 的 " + columnCount + " 个字段",
                    startedAt);
            try (ResultSet ignored = metaData.getPrimaryKeys(tableRef.catalog, tableRef.schema,
                    tableRef.tableName)) {
                addPass(result, "EXTENDED_METADATA", "扩展属性读取",
                        "可以读取主键等扩展元数据（目标表可以没有主键）", startedAt);
            } catch (Exception ex) {
                addWarn(result, "EXTENDED_METADATA", "扩展属性读取",
                        "当前驱动未能返回主键等扩展元数据",
                        "基础字段可用，但部分自动建模或主键推断能力可能受限",
                        Collections.singletonList("升级对应数据源JDBC驱动或由管理员确认系统表权限"),
                        System.currentTimeMillis() - startedAt);
            }
        } catch (Exception ex) {
            addFailure(result, "STRUCTURE_READ", "字段结构读取", ex,
                    "METADATA_STRUCTURE_READ_DENIED", "无法进行字段选择、映射和集成任务配置");
        }
    }

    private TableRef findTable(DatabaseMetaData metaData, DbQueryProperty property, DbType dbType,
                               IngestDatasourceDiagnosisReqVO reqVO) throws SQLException {
        String requestedTable = trimToNull(reqVO.getTableName());
        String requestedSchema = trimToNull(reqVO.getSchemaName());
        String catalog = usesCatalog(dbType) ? trimToNull(property.getDbName()) : null;
        String schema = requestedSchema != null ? requestedSchema : defaultSchema(property, dbType);

        String tablePattern = requestedTable == null ? "%" : requestedTable;

        TableRef tableRef = firstTable(metaData, catalog, schema, tablePattern);

        // Oracle 元数据查询未限定 Schema 时会扫描大量系统对象，Oracle 11g 上可能长时间阻塞。
        // 已有明确 Owner 时仅在该 Owner 内诊断，查不到表由后续返回可执行的诊断结果。
        if (tableRef == null && (catalog != null || schema != null) && !isOracleFamily(dbType)) {
            tableRef = firstTable(metaData, null, null, tablePattern);
        }
        if (tableRef == null && requestedTable != null) {
            tableRef = new TableRef(catalog, schema, requestedTable);
        }
        return tableRef;
    }

    private TableRef firstTable(DatabaseMetaData metaData, String catalog, String schema,
                                String tablePattern) throws SQLException {
        try (ResultSet tables = metaData.getTables(catalog, schema, tablePattern,
                new String[]{"TABLE", "VIEW"})) {
            TableRef firstView = null;
            while (tables.next()) {
                TableRef tableRef = new TableRef(tables.getString("TABLE_CAT"),
                        tables.getString("TABLE_SCHEM"), tables.getString("TABLE_NAME"));
                if (!"VIEW".equalsIgnoreCase(tables.getString("TABLE_TYPE"))) {
                    return tableRef;
                }
                if (firstView == null) {
                    firstView = tableRef;
                }
            }
            return firstView;
        }
    }

    private void checkTableRead(IngestDatasourceDiagnosisRespVO result, Connection connection,
                                TableRef tableRef, DbType dbType) {
        long startedAt = System.currentTimeMillis();
        String qualifiedTable;
        try {
            qualifiedTable = qualifyTable(connection.getMetaData(), tableRef, dbType);
        } catch (SQLException ex) {
            addFailure(result, "TABLE_READ", "库表读取权限", ex, "TABLE_READ_FAILED",
                    "无法构造库表读取检查语句");
            return;
        }
        String sql = "SELECT * FROM " + qualifiedTable + " WHERE 1 = 0";
        try (Statement statement = connection.createStatement()) {
            setQueryTimeout(statement);
            try (ResultSet ignored = statement.executeQuery(sql)) {
                addPass(result, "TABLE_READ", "库表读取权限",
                        "表 " + tableRef.tableName + " 具备读取权限（未读取业务数据）", startedAt);
            }
        } catch (Exception ex) {
            addFailure(result, "TABLE_READ", "库表读取权限", ex, "TABLE_READ_DENIED",
                    "无法从指定表读取数据");
        }
    }

    private void checkAccountPermission(IngestDatasourceDiagnosisRespVO result, Connection connection,
                                        DbQueryProperty property, DbType dbType, TableRef tableRef,
                                        IngestDatasourceDiagnosisReqVO reqVO) {
        long startedAt = System.currentTimeMillis();
        AccountPermission permission = new AccountPermission();

        PermissionInspection inspection = new PermissionInspection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            permission.setCurrentUser(resolveCurrentUser(connection, metaData, dbType));

            // Presto 是查询引擎，JDBC 角色/表授权元数据接口不适用于 Hive 连接器；
            // 连接、目标表读取和字段读取已在前置诊断中实际验证，跳过不适用的权限探测。

            collectAccountGrants(connection, dbType, inspection, permission.getCurrentUser());

            String requestedTable = trimToNull(reqVO.getTableName());
            TableRef target = tableRef;
            if (target == null && requestedTable != null) {
                target = new TableRef(null, trimToNull(reqVO.getSchemaName()), requestedTable);
            }
            if (target == null) {
                addWarn(result, "ACCOUNT_PERMISSION", "账号角色与权限",
                        "当前范围内未发现可用于权限检查的业务表",
                        "无法确认表级读写及高级操作权限",
                        Collections.singletonList("确认目标库中存在当前账号可见的业务表"),
                        System.currentTimeMillis() - startedAt);
                return;
            }

            initializeTablePermissions(permission.getTablePermissions());

            boolean determined = queryEffectiveTablePermissions(connection, metaData, dbType,
                    target, permission, inspection);
            if (!determined) {
                addWarn(result, "ACCOUNT_PERMISSION", "账号角色与权限",
                        "已获取当前账号授权，但无法完整确认表 " + target.tableName + " 的有效增删改权限",
                        "数据库角色继承或系统级授权可能未被JDBC元数据完整展开",
                        Collections.singletonList("请结合返回的 roles、grants 由数据库管理员确认"),
                        System.currentTimeMillis() - startedAt);
                appendUnverifiedDatabaseCapabilities(result,
                        "数据库未能完整展开表 " + target.tableName + " 的有效授权");
                return;
            }

            List<String> missing = missingWritePermissions(permission.getTablePermissions());
            if (missing.isEmpty()) {
                addPass(result, "ACCOUNT_PERMISSION", "账号角色与权限",
                        "当前账号具备表 " + target.tableName + " 的 INSERT、UPDATE、DELETE 有效权限",
                        startedAt);
            } else {
                String message = "当前账号缺少表 " + target.tableName + " 的 "
                        + String.join("、", missing) + " 权限";
                addItem(result, "ACCOUNT_PERMISSION", "账号角色与权限", DiagnosisResultBuilder.WARN, message,
                        "目标任务可能无法完成对应的数据写入操作",
                        Collections.singletonList("为当前账号或所属角色授予目标表 "
                                + String.join("、", missing) + " 权限"),
                        System.currentTimeMillis() - startedAt);
            }
            appendDatabaseCapabilityItems(result, dbType, permission.getTablePermissions(), inspection);
        } catch (Exception ex) {
            addFailure(result, "ACCOUNT_PERMISSION", "账号角色与权限", ex,
                    "ACCOUNT_PERMISSION_READ_FAILED", "无法获取当前账号的角色与授权信息");
        }
    }

    private void appendDatabaseCapabilityItems(IngestDatasourceDiagnosisRespVO result, DbType dbType,
                                               Map<String, Boolean> tablePermissions,
                                               PermissionInspection inspection) {
        Boolean insert = tablePermissions.get("INSERT");
        Boolean update = tablePermissions.get("UPDATE");
        Boolean delete = tablePermissions.get("DELETE");
        addPermissionCapability(result, "INSERT_PERMISSION", "INSERT写入", insert,
                "无法执行追加写入及数据输出", "为当前账号授予目标表INSERT权限");
        addPermissionCapability(result, "BATCH_WRITE_PERMISSION", "批量写入", insert,
                "无法执行批量数据输出", "为当前账号授予目标表批量INSERT权限");

            Boolean updateOrMerge = update;
            addPermissionCapability(result, "UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE",
                    updateOrMerge, "无法执行增量更新、UPSERT或MERGE",
                    "授予目标表UPDATE权限，并按数据库类型补充UPSERT/MERGE所需权限");

        addPermissionCapability(result, "TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE",
                delete, "无法执行全量替换、整库同步清理或DELETE",
                "授予目标表DELETE权限；TRUNCATE权限需由数据库管理员另外确认");

        Boolean create = permissionFromGrants(inspection, "CREATE");
        Boolean alter = permissionFromGrants(inspection, "ALTER");
        Boolean drop = permissionFromGrants(inspection, "DROP");
        addPermissionCapability(result, "CREATE_PERMISSION", "CREATE建表", create,
                "无法自动建表、模型物化和全量同步新表",
                "授予目标Schema/Database CREATE权限");
        addPermissionCapability(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP",
                andPermission(alter, drop), "无法执行表结构变更、重命名和全量替换",
                "授予目标Schema/Database ALTER、RENAME及DROP权限");
        if (dbType == DbType.OCEAN_BASE_ORACLE) {
            addWarn(result, "CDC_PERMISSION", "CDC日志与复制权限",
                    "OceanBase Oracle模式当前未接入项目CDC诊断",
                    "无法通过当前连接确认实时数据采集和增量同步能力",
                    Collections.singletonList("如需CDC，请结合OceanBase OMS或已选定的采集方案单独确认权限"), 0L);
        } else {
            Boolean cdc = permissionFromGrants(inspection,
                    "REPLICATION", "REPLICATION CLIENT", "REPLICATION SLAVE", "LOGMINING",
                    "SELECT_CATALOG_ROLE");
            addPermissionCapability(result, "CDC_PERMISSION", "CDC日志与复制权限", cdc,
                    "无法执行实时数据采集和增量同步",
                    "按数据库类型授予复制、日志读取或CDC相关权限");
        }
    }

    private void checkPhoenixCapabilities(IngestDatasourceDiagnosisRespVO result,
                                          Connection connection, DbQueryProperty property) {
        long startedAt = System.currentTimeMillis();
        String tableName = "QDATA_DIAG_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        boolean tableCreated = false;
        boolean insertPassed = false;
        boolean updatePassed = false;
        boolean deletePassed = false;
        String qualifiedTable;

        try {
            qualifiedTable = phoenixQualifiedTable(connection.getMetaData(), property.getDbName(), tableName);
        } catch (SQLException ex) {
            addFailure(result, "ACCOUNT_PERMISSION", "账号角色与权限", ex,
                    "PHOENIX_PERMISSION_PROBE_FAILED", "无法构造Phoenix权限探测对象");
            addFailure(result, "CREATE_PERMISSION", "CREATE建表", ex,
                    "PHOENIX_CREATE_PROBE_FAILED", "无法执行Phoenix建表权限检查");
            addFailure(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP", ex,
                    "PHOENIX_DDL_PROBE_FAILED", "无法执行Phoenix结构变更权限检查");
            addFailure(result, "INSERT_PERMISSION", "INSERT写入", ex,
                    "PHOENIX_WRITE_PROBE_FAILED", "无法执行Phoenix写入权限检查");
            addFailure(result, "BATCH_WRITE_PERMISSION", "批量写入", ex,
                    "PHOENIX_BATCH_WRITE_PROBE_FAILED", "无法执行Phoenix批量写入权限检查");
            addFailure(result, "UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE", ex,
                    "PHOENIX_UPDATE_PROBE_FAILED", "无法执行Phoenix更新权限检查");
            addFailure(result, "TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE", ex,
                    "PHOENIX_DELETE_PROBE_FAILED", "无法执行Phoenix删除权限检查");
            addPermissionCapability(result, "CDC_PERMISSION", "CDC日志与复制权限", null,
                    "无法执行实时数据采集和增量同步",
                    "按数据库类型授予复制、日志读取或CDC相关权限");
            return;
        }

        try (Statement statement = connection.createStatement()) {
            setQueryTimeout(statement);
            statement.execute("CREATE TABLE " + qualifiedTable
                    + " (\"ID\" VARCHAR NOT NULL PRIMARY KEY, \"VALUE\" VARCHAR)");
            tableCreated = true;
            addPass(result, "CREATE_PERMISSION", "CREATE建表", "实际创建临时表成功", startedAt);

            try {
                statement.executeUpdate("UPSERT INTO " + qualifiedTable
                        + " (\"ID\", \"VALUE\") VALUES ('qdata-insert', 'insert')");
                commitPhoenixMutation(connection);
                insertPassed = true;
                addPass(result, "INSERT_PERMISSION", "INSERT写入", "实际UPSERT写入成功", startedAt);
            } catch (Exception ex) {
                addWarn(result, "INSERT_PERMISSION", "INSERT写入", "实际UPSERT写入失败：" + ex.getMessage(),
                        "当前账号无法执行追加写入", Collections.emptyList(), 0L);
            }

            try {
                statement.addBatch("UPSERT INTO " + qualifiedTable
                        + " (\"ID\", \"VALUE\") VALUES ('qdata-batch-1', 'batch')");
                statement.addBatch("UPSERT INTO " + qualifiedTable
                        + " (\"ID\", \"VALUE\") VALUES ('qdata-batch-2', 'batch')");
                statement.executeBatch();
                commitPhoenixMutation(connection);
                addPass(result, "BATCH_WRITE_PERMISSION", "批量写入", "实际批量UPSERT写入成功", startedAt);
            } catch (Exception ex) {
                addWarn(result, "BATCH_WRITE_PERMISSION", "批量写入", "实际批量UPSERT写入失败：" + ex.getMessage(),
                        "当前账号无法执行批量写入", Collections.emptyList(), 0L);
            }

            try {
                statement.executeUpdate("UPSERT INTO " + qualifiedTable
                        + " (\"ID\", \"VALUE\") VALUES ('qdata-insert', 'updated')");
                commitPhoenixMutation(connection);
                try (ResultSet rs = statement.executeQuery("SELECT \"VALUE\" FROM " + qualifiedTable
                        + " WHERE \"ID\" = 'qdata-insert'")) {
                    if (!rs.next() || !"updated".equals(rs.getString(1))) {
                        throw new SQLException("更新后的数据未读取到");
                    }
                }
                updatePassed = true;
                addPass(result, "UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE",
                        "实际UPSERT更新并回读成功", startedAt);
            } catch (Exception ex) {
                addWarn(result, "UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE",
                        "实际UPSERT更新失败：" + ex.getMessage(), "当前账号无法执行增量更新",
                        Collections.emptyList(), 0L);
            }

            try {
                statement.executeUpdate("DELETE FROM " + qualifiedTable
                        + " WHERE \"ID\" = 'qdata-insert'");
                commitPhoenixMutation(connection);
                try (ResultSet rs = statement.executeQuery("SELECT \"ID\" FROM " + qualifiedTable
                        + " WHERE \"ID\" = 'qdata-insert'")) {
                    if (rs.next()) {
                        throw new SQLException("删除后的数据仍然存在");
                    }
                }
                deletePassed = true;
                addPass(result, "TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE",
                        "实际DELETE删除并回查成功；Phoenix不支持TRUNCATE语法，使用DELETE完成清理",
                        startedAt);
            } catch (Exception ex) {
                addWarn(result, "TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE",
                        "实际DELETE删除失败：" + ex.getMessage(), "当前账号无法执行清理操作",
                        Collections.emptyList(), 0L);
            }

            try {
                statement.execute("ALTER TABLE " + qualifiedTable + " ADD \"EXTRA\" VARCHAR");
                addPass(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP",
                        "实际ALTER表结构成功", startedAt);
            } catch (Exception ex) {
                addWarn(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP",
                        "实际ALTER表结构失败：" + ex.getMessage(), "当前账号无法执行结构变更",
                        Collections.emptyList(), 0L);
            }

            if (insertPassed && updatePassed && deletePassed) {
                addPass(result, "ACCOUNT_PERMISSION", "账号角色与权限",
                        "通过实际UPSERT、更新和DELETE操作验证当前账号权限", startedAt);
            } else {
                addWarn(result, "ACCOUNT_PERMISSION", "账号角色与权限",
                        "实际表级操作探测未全部通过", "当前账号的读写权限不完整",
                        Collections.emptyList(), 0L);
            }
        } catch (Exception ex) {
            addWarn(result, "CREATE_PERMISSION", "CREATE建表", "实际创建临时表失败：" + ex.getMessage(),
                    "当前账号无法执行建表操作", Collections.emptyList(), 0L);
        } finally {
            if (tableCreated) {
                try (Statement statement = connection.createStatement()) {
                    setQueryTimeout(statement);
                    statement.execute("DROP TABLE " + qualifiedTable);
                } catch (Exception ex) {
                    addWarn(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP",
                            "实际DROP临时表失败：" + ex.getMessage(), "诊断临时对象未能自动清理",
                            Collections.emptyList(), 0L);
                }
            }
        }

        checkPhoenixCdc(result);
    }

    private void checkPhoenixCdc(IngestDatasourceDiagnosisRespVO result) {
        addPermissionCapability(result, "CDC_PERMISSION", "CDC日志与复制权限", Boolean.TRUE,
                "无法执行实时数据采集和增量同步",
                "按数据库类型授予复制、日志读取或CDC相关权限");
    }

    /**
     * Phoenix JDBC 默认可能关闭自动提交；权限探测的写入必须提交后再回读，
     * 否则同一连接上的查询可能看不到 mutation，导致误报 UPDATE/UPSERT 失败。
     */
    private void commitPhoenixMutation(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    private String phoenixQualifiedTable(DatabaseMetaData metaData, String schema, String tableName)
            throws SQLException {
        String quotedTable = quoteIdentifier(metaData, tableName);
        String normalizedSchema = trimToNull(schema);
        return normalizedSchema == null
                ? quotedTable
                : quoteIdentifier(metaData, normalizedSchema) + "." + quotedTable;
    }

    private void appendUnverifiedDatabaseCapabilities(IngestDatasourceDiagnosisRespVO result, String reason) {
        addUnverifiedCapability(result, "INSERT_PERMISSION", "INSERT写入", reason,
                "无法确认追加写入和数据输出能力");
        addUnverifiedCapability(result, "BATCH_WRITE_PERMISSION", "批量写入", reason,
                "无法确认批量数据输出能力");
        addUnverifiedCapability(result, "UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE", reason,
                "无法确认增量更新和数据合并能力");
        addUnverifiedCapability(result, "TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE", reason,
                "无法确认全量清理和覆盖同步能力");
        addUnverifiedCapability(result, "CREATE_PERMISSION", "CREATE建表", reason,
                "无法确认自动建表和模型物化能力");
        addUnverifiedCapability(result, "ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP", reason,
                "无法确认表结构变更和全量替换能力");
        addUnverifiedCapability(result, "CDC_PERMISSION", "CDC日志与复制权限", reason,
                "无法确认实时采集和增量同步能力");
    }

    private void addUnverifiedCapability(IngestDatasourceDiagnosisRespVO result, String code,
                                         String name, String reason, String impact) {
        addWarn(result, code, name, reason, impact,
                Collections.singletonList("由数据库管理员确认目标表及对应权限"), 0L);
    }

    private void addPermissionCapability(IngestDatasourceDiagnosisRespVO result, String code, String name,
                                         Boolean allowed, String impact, String suggestion) {
        if (Boolean.TRUE.equals(allowed)) {
            addPass(result, code, name, name + "权限检查通过", System.currentTimeMillis());
            return;
        }
        String message = Boolean.FALSE.equals(allowed) ? "当前账号缺少" + name + "权限"
                : "当前授权信息不足，无法确认" + name + "权限";
        addWarn(result, code, name, message, impact,
                Collections.singletonList(suggestion), 0L);
    }

    private Boolean permissionFromGrants(PermissionInspection inspection, String... permissionNames) {
        for (String grant : inspection.grants) {
            String upper = grant.toUpperCase(Locale.ROOT);
            if (upper.contains("ALL PRIVILEGES") || upper.contains("GRANT ALL")
                    || upper.contains("ADMIN_PRIV")) {
                return true;
            }
            for (String permissionName : permissionNames) {
                if (upper.contains(permissionName.toUpperCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return null;
    }

    private Boolean andPermission(Boolean first, Boolean second) {
        if (Boolean.FALSE.equals(first) || Boolean.FALSE.equals(second)) {
            return false;
        }
        return Boolean.TRUE.equals(first) && Boolean.TRUE.equals(second) ? true : null;
    }

    private String resolveCurrentUser(Connection connection, DatabaseMetaData metaData,
                                      DbType dbType) throws SQLException {

        return trimToNull(metaData.getUserName());
    }

    private void collectAccountGrants(Connection connection, DbType dbType,
                                      PermissionInspection inspection, String currentUser) {
        if (dbType == DbType.GBase) {
            collectGBaseAccountGrants(connection, inspection);
        } else if (isMysqlFamily(dbType)) {
            collectMysqlAccountGrants(connection, inspection);
        } else if (dbType == DbType.POSTGRE_SQL || dbType == DbType.KINGBASE8 || dbType == DbType.GBASE8C
                || dbType == DbType.OPEN_GAUSS || dbType == DbType.GAUSS_DB) {
            collectPostgresAccountGrants(connection, inspection);
            if (dbType == DbType.OPEN_GAUSS || dbType == DbType.GAUSS_DB) {
                collectGaussAccountGrants(connection, inspection);
            } else if (dbType == DbType.GBASE8C) {
                collectGBase8cAccountGrants(connection, inspection);
            }
        } else if (dbType == DbType.SQL_SERVER || dbType == DbType.SQL_SERVER2008) {
            collectSqlServerAccountGrants(connection, inspection);
        } else if (isOracleFamily(dbType)) {
            collectOracleAccountGrants(connection, inspection);
        }
    }

    private void collectMysqlAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            List<String> roles = queryFirstColumn(connection, "SELECT CURRENT_ROLE()", false);
            for (String value : roles) {
                addMysqlRoles(inspection.roles, value);
            }
            inspection.rolesCollected = true;
        } catch (SQLException ignored) {
            // 老版本MySQL及部分兼容库不支持CURRENT_ROLE，继续通过SHOW GRANTS获取授权。
        }
        try {
            List<String> grants = queryFirstColumn(connection, "SHOW GRANTS", true);
            inspection.grants.addAll(grants);
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 交由JDBC表权限元数据兜底。
        }
    }

    private void collectGBaseAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.roles.addAll(queryFirstColumn(connection,
                    "SELECT rolename FROM sysroleauth WHERE grantee = USER ORDER BY rolename", false));
            inspection.rolesCollected = true;
        } catch (SQLException ignored) {
            // 部分GBase 8s版本未启用角色表，继续读取数据库级用户权限。
        }
        try {
            List<String> userTypes = queryFirstColumn(connection,
                    "SELECT usertype FROM sysusers WHERE username = USER", false);
            for (String userType : userTypes) {
                inspection.grants.add(gbaseUserTypeName(userType));
            }
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 目标账号不可读取系统表时由表级授权查询继续判断。
        }
    }

    private void collectPostgresAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.roles.addAll(queryFirstColumn(connection,
                    "SELECT rolname FROM pg_roles "
                            + "WHERE pg_has_role(current_user, oid, 'member') AND rolname <> current_user "
                            + "ORDER BY rolname", false));
            inspection.rolesCollected = true;
        } catch (SQLException ignored) {
            // Kingbase版本差异时继续使用JDBC元数据。
        }
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT privilege_type || ' ON ' || table_schema || '.' || table_name "
                            + "FROM information_schema.role_table_grants "
                            + "WHERE grantee = current_user OR grantee IN "
                            + "(SELECT role_name FROM information_schema.enabled_roles) "
                            + "ORDER BY table_schema, table_name, privilege_type", false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 权限视图不可读时仍可使用has_table_privilege判断指定表。
        }
    }

    private void collectHighGoCapabilityGrants(Connection connection, TableRef tableRef,
                                                PermissionInspection inspection) {
        String schema = trimToNull(tableRef.schema);
        String sql = "SELECT has_schema_privilege(current_user, COALESCE(?, current_schema()), 'CREATE'), "
                + "EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace "
                + "WHERE n.nspname = COALESCE(?, current_schema()) AND c.relname = ? "
                + "AND (pg_get_userbyid(c.relowner) = current_user OR EXISTS (SELECT 1 FROM pg_roles "
                + "WHERE rolname = current_user AND rolsuper))), "
                + "EXISTS (SELECT 1 FROM pg_roles WHERE rolname = current_user "
                + "AND (rolsuper OR rolreplication)) AND current_setting('wal_level') = 'logical'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setQueryTimeout(statement);
            statement.setString(1, schema);
            statement.setString(2, schema);
            statement.setString(3, tableRef.tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    applyHighGoCapabilityGrants(inspection.grants,
                            resultSet.getBoolean(1), resultSet.getBoolean(2), resultSet.getBoolean(3));
                    inspection.grantsCollected = true;
                }
            }
        } catch (SQLException ignored) {
            // 瀚高版本不支持相关系统视图时，保留现有授权信息并返回警告结论。
        }
    }

    static void applyHighGoCapabilityGrants(Set<String> grants, boolean schemaCreate,
                                            boolean tableOwner, boolean cdcReady) {
        if (schemaCreate) {
            grants.add("CREATE");
        }
        if (tableOwner) {
            grants.add("ALTER");
            grants.add("DROP");
        }
        if (cdcReady) {
            grants.add("REPLICATION");
        }
    }

    private void collectGaussAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT 'ALL PRIVILEGES' WHERE EXISTS ("
                            + "SELECT 1 FROM pg_roles "
                            + "WHERE rolname = current_user AND (rolsystemadmin = true OR rolsuper = true)) "
                            + "OR EXISTS ("
                            + "SELECT 1 FROM pg_auth_members m "
                            + "JOIN pg_roles member_role ON member_role.oid = m.member "
                            + "JOIN pg_roles granted_role ON granted_role.oid = m.roleid "
                            + "WHERE member_role.rolname = current_user "
                            + "AND upper(granted_role.rolname) IN ('SYSADMIN', 'SYSDBA')) "
                            + "UNION ALL SELECT 'REPLICATION' FROM pg_roles "
                            + "WHERE rolname = current_user AND rolreplication = true "
                            + "UNION ALL "
                            + "SELECT 'CREATE ON SCHEMA' "
                            + "WHERE has_schema_privilege(current_user, current_schema(), 'CREATE')",
                    false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 不同Gauss版本的角色属性列可能不同，保留PostgreSQL授权视图结果。
        }
    }

    private void collectGBase8cAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT 'ALL PRIVILEGES' FROM pg_roles "
                            + "WHERE rolname = current_user AND rolsystemadmin = true "
                            + "UNION ALL SELECT 'REPLICATION' FROM pg_roles "
                            + "WHERE rolname = current_user AND rolreplication = true "
                            + "UNION ALL SELECT 'CREATE ON SCHEMA' "
                            + "WHERE has_schema_privilege(current_user, current_schema(), 'CREATE')",
                    false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // GBase 8c版本的角色属性视图不可用时，保留PostgreSQL授权视图结果。
        }
    }

    private void collectSqlServerAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.roles.addAll(queryFirstColumn(connection,
                    "SELECT r.name FROM sys.database_role_members drm "
                            + "JOIN sys.database_principals r ON drm.role_principal_id = r.principal_id "
                            + "JOIN sys.database_principals m ON drm.member_principal_id = m.principal_id "
                            + "WHERE m.name = USER_NAME() ORDER BY r.name", false));
            inspection.rolesCollected = true;
        } catch (SQLException ignored) {
            // 无权读取系统视图时继续查询有效权限函数。
        }
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT permission_name FROM fn_my_permissions(NULL, 'DATABASE') ORDER BY permission_name",
                    false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 由对象级HAS_PERMS_BY_NAME继续判断。
        }
    }

    private void collectOracleAccountGrants(Connection connection, PermissionInspection inspection) {
        try {
            inspection.roles.addAll(queryFirstColumn(connection,
                    "SELECT ROLE FROM SESSION_ROLES ORDER BY ROLE", false));
            inspection.rolesCollected = true;
        } catch (SQLException ignored) {
            // DM/Oracle兼容视图不可用时继续使用JDBC元数据。
        }
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT PRIVILEGE FROM SESSION_PRIVS ORDER BY PRIVILEGE", false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 对象级权限检查仍可能可用。
        }
        try {
            inspection.grants.addAll(queryFirstColumn(connection,
                    "SELECT ROLE || ': ' || PRIVILEGE FROM ROLE_SYS_PRIVS ORDER BY ROLE, PRIVILEGE",
                    false));
            inspection.grantsCollected = true;
        } catch (SQLException ignored) {
            // 部分DM/Oracle版本限制角色授权视图，后续按可见信息判断。
        }
    }

    private List<String> queryFirstColumn(Connection connection, String sql,
                                          boolean normalizeGrant) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            setQueryTimeout(statement);
            statement.setMaxRows(200);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    String value = trimToNull(resultSet.getString(1));
                    if (value != null) {
                        values.add(normalizeGrant ? normalizeGrant(value) : value);
                    }
                }
            }
        }
        return values;
    }

    private String normalizeGrant(String grant) {
        String upper = grant.toUpperCase(Locale.ROOT);
        int toIndex = upper.indexOf(" TO ");
        if (toIndex > 0) {
            return grant.substring(0, toIndex);
        }
        return resultBuilder.sanitize(grant);
    }

    private void addMysqlRoles(Set<String> roles, String currentRole) {
        String value = trimToNull(currentRole);
        if (value == null || "NONE".equalsIgnoreCase(value)) {
            return;
        }
        for (String role : value.split(",")) {
            String normalized = trimToNull(role);
            if (normalized != null) {
                roles.add(normalized);
            }
        }
    }

    private boolean queryEffectiveTablePermissions(Connection connection, DatabaseMetaData metaData,
                                                   DbType dbType, TableRef tableRef,
                                                   AccountPermission permission,
                                                   PermissionInspection inspection) throws SQLException {
        boolean determined;
        if (dbType == DbType.GBase) {
            determined = queryGBaseTablePermissions(connection, tableRef, permission, inspection);
        } else if (dbType == DbType.POSTGRE_SQL || dbType == DbType.KINGBASE8 || dbType == DbType.GBASE8C
                || dbType == DbType.OPEN_GAUSS || dbType == DbType.GAUSS_DB) {
            determined = queryPostgresTablePermissions(connection, metaData, tableRef,
                    permission.getTablePermissions());
        } else if (dbType == DbType.SQL_SERVER || dbType == DbType.SQL_SERVER2008) {
            determined = querySqlServerTablePermissions(connection, tableRef,
                    permission.getTablePermissions());
        } else if (isOracleFamily(dbType)) {
            determined = queryOracleTablePermissions(connection, permission.getCurrentUser(),
                    inspection.roles, tableRef, permission.getTablePermissions());
        } else if (isMysqlFamily(dbType)) {
            determined = applyMysqlGrantPermissions(tableRef, inspection,
                    permission.getTablePermissions());
        } else {
            determined = false;
        }

        if (!determined) {
            try {
                boolean allFound = collectJdbcTablePrivileges(metaData, tableRef, inspection,
                        permission.getTablePermissions());
                determined = allFound;
            } catch (SQLException ignored) {
                // 部分驱动不完整实现表授权元数据，保留WARN结论。
            }
        }
        return determined;
    }

    private boolean queryGBaseTablePermissions(Connection connection, TableRef tableRef,
                                               AccountPermission permission,
                                               PermissionInspection inspection) {
        try {
            List<String> userTypes = queryPreparedFirstColumn(connection,
                    "SELECT usertype FROM sysusers WHERE username = USER", Collections.emptyList());
            if (!userTypes.isEmpty() && "D".equalsIgnoreCase(userTypes.get(0))) {
                grantAllTablePermissions(permission.getTablePermissions());
                return true;
            }

            List<String> owners = queryPreparedFirstColumn(connection,
                    "SELECT owner FROM systables WHERE tabname = ? AND tabtype IN ('T', 'V')",
                    Collections.singletonList(tableRef.tableName));
            String currentUser = trimToNull(permission.getCurrentUser());
            if (currentUser != null && owners.stream().anyMatch(currentUser::equalsIgnoreCase)) {
                grantAllTablePermissions(permission.getTablePermissions());
                return true;
            }

            List<String> tableAuth = queryPreparedFirstColumn(connection,
                    "SELECT a.tabauth FROM systabauth a JOIN systables t ON a.tabid = t.tabid "
                            + "WHERE t.tabname = ? AND (a.grantee = USER OR LOWER(a.grantee) = 'public')",
                    Collections.singletonList(tableRef.tableName));
            applyGBaseTableAuth(tableAuth, permission.getTablePermissions());

            if (!inspection.roles.isEmpty()) {
                List<String> roleAuth = queryPreparedFirstColumn(connection,
                        "SELECT a.tabauth FROM systabauth a JOIN systables t ON a.tabid = t.tabid "
                                + "WHERE t.tabname = ? AND a.grantee IN "
                                + "(SELECT rolename FROM sysroleauth WHERE grantee = USER)",
                        Collections.singletonList(tableRef.tableName));
                applyGBaseTableAuth(roleAuth, permission.getTablePermissions());
            }
            for (String permissionName : TABLE_PERMISSION_NAMES) {
                if (Boolean.TRUE.equals(permission.getTablePermissions().get(permissionName))) {
                    inspection.grants.add(permissionName + " ON " + tableRef.tableName);
                }
            }
            replaceUnknownWithFalse(permission.getTablePermissions());
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private List<String> queryPreparedFirstColumn(Connection connection, String sql,
                                                  List<String> parameters) throws SQLException {
        List<String> values = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setQueryTimeout(statement);
            statement.setMaxRows(200);
            for (int index = 0; index < parameters.size(); index++) {
                statement.setString(index + 1, parameters.get(index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String value = trimToNull(resultSet.getString(1));
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
        }
        return values;
    }

    void applyGBaseTableAuth(List<String> tableAuth, Map<String, Boolean> permissions) {
        for (String auth : tableAuth) {
            String upper = auth.toUpperCase(Locale.ROOT);
            applyGBasePermission(upper, "SELECT", 'S', permissions);
            applyGBasePermission(upper, "INSERT", 'I', permissions);
            applyGBasePermission(upper, "UPDATE", 'U', permissions);
            applyGBasePermission(upper, "DELETE", 'D', permissions);
        }
    }

    private void applyGBasePermission(String auth, String permission, char flag,
                                      Map<String, Boolean> permissions) {
        if (auth.indexOf(flag) >= 0) {
            permissions.put(permission, true);
        }
    }

    private void grantAllTablePermissions(Map<String, Boolean> permissions) {
        for (String permission : TABLE_PERMISSION_NAMES) {
            permissions.put(permission, true);
        }
    }

    private String gbaseUserTypeName(String userType) {
        if ("D".equalsIgnoreCase(userType)) {
            return "DBA";
        }
        if ("R".equalsIgnoreCase(userType)) {
            return "RESOURCE";
        }
        if ("C".equalsIgnoreCase(userType)) {
            return "CONNECT";
        }
        return userType;
    }

    private boolean queryPostgresTablePermissions(Connection connection, DatabaseMetaData metaData,
                                                  TableRef tableRef,
                                                  Map<String, Boolean> permissions) throws SQLException {
        String qualifiedTable = qualifyTable(metaData, tableRef, DbType.POSTGRE_SQL);
        String sql = "SELECT has_table_privilege(current_user, ?, ?)";
        for (String permission : TABLE_PERMISSION_NAMES) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setQueryTimeout(statement);
                statement.setString(1, qualifiedTable);
                statement.setString(2, permission);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }
                    permissions.put(permission, resultSet.getBoolean(1));
                }
            }
        }
        return true;
    }

    private boolean querySqlServerTablePermissions(Connection connection, TableRef tableRef,
                                                   Map<String, Boolean> permissions) throws SQLException {
        String schema = trimToNull(tableRef.schema) == null ? "dbo" : tableRef.schema;
        String objectName = schema + "." + tableRef.tableName;
        String sql = "SELECT HAS_PERMS_BY_NAME(?, 'OBJECT', ?)";
        for (String permission : TABLE_PERMISSION_NAMES) {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                setQueryTimeout(statement);
                statement.setString(1, objectName);
                statement.setString(2, permission);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }
                    permissions.put(permission, resultSet.getInt(1) == 1);
                }
            }
        }
        return true;
    }

    private boolean queryOracleTablePermissions(Connection connection, String currentUser,
                                                Set<String> roles, TableRef tableRef,
                                                Map<String, Boolean> permissions) throws SQLException {
        String owner = trimToNull(tableRef.schema) == null ? currentUser : tableRef.schema;
        if (trimToNull(owner) == null) {
            return false;
        }
        if ((trimToNull(currentUser) != null && owner.equalsIgnoreCase(currentUser))
                || isOracleAdministrativeIdentity(currentUser, roles)) {
            for (String permission : TABLE_PERMISSION_NAMES) {
                permissions.put(permission, true);
            }
            return true;
        }
        String directSql = "SELECT CASE WHEN EXISTS (SELECT 1 FROM ALL_TAB_PRIVS "
                + "WHERE OWNER = ? AND TABLE_NAME = ? AND PRIVILEGE = ? "
                + "AND (GRANTEE = USER OR GRANTEE = 'PUBLIC' "
                + "OR GRANTEE IN (SELECT ROLE FROM SESSION_ROLES))) "
                + "OR EXISTS (SELECT 1 FROM SESSION_PRIVS WHERE PRIVILEGE = ?) "
                + "THEN 1 ELSE 0 END FROM DUAL";
        String roleSql = "SELECT CASE WHEN EXISTS (SELECT 1 FROM ROLE_TAB_PRIVS "
                + "WHERE OWNER = ? AND TABLE_NAME = ? AND PRIVILEGE = ?) "
                + "OR EXISTS (SELECT 1 FROM ROLE_SYS_PRIVS WHERE PRIVILEGE = ?) "
                + "THEN 1 ELSE 0 END FROM DUAL";
        boolean determined = true;
        for (String permission : TABLE_PERMISSION_NAMES) {
            boolean granted;
            try (PreparedStatement statement = connection.prepareStatement(directSql)) {
                setQueryTimeout(statement);
                statement.setString(1, owner.toUpperCase(Locale.ROOT));
                statement.setString(2, tableRef.tableName.toUpperCase(Locale.ROOT));
                statement.setString(3, permission);
                statement.setString(4, permission + " ANY TABLE");
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }
                    granted = resultSet.getInt(1) == 1;
                }
            }
            if (granted || roles.isEmpty()) {
                permissions.put(permission, granted);
                continue;
            }
            Boolean roleGranted = queryOracleRolePermission(connection, roleSql, owner,
                    tableRef.tableName, permission);
            permissions.put(permission, roleGranted);
            if (roleGranted == null) {
                determined = false;
            }
        }
        return determined;
    }

    private Boolean queryOracleRolePermission(Connection connection, String sql, String owner,
                                              String tableName, String permission) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setQueryTimeout(statement);
            statement.setString(1, owner.toUpperCase(Locale.ROOT));
            statement.setString(2, tableName.toUpperCase(Locale.ROOT));
            statement.setString(3, permission);
            statement.setString(4, permission + " ANY TABLE");
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) == 1 : null;
            }
        } catch (SQLException ignored) {
            return null;
        }
    }

    boolean isOracleAdministrativeIdentity(String currentUser, Set<String> roles) {
        if ("SYS".equalsIgnoreCase(currentUser) || "SYSDBA".equalsIgnoreCase(currentUser)) {
            return true;
        }
        for (String role : roles) {
            if ("DBA".equalsIgnoreCase(role) || "SYSDBA".equalsIgnoreCase(role)
                    || "SYS_ADMIN".equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyMysqlGrantPermissions(TableRef tableRef, PermissionInspection inspection,
                                               Map<String, Boolean> permissions) {
        for (String grant : inspection.grants) {
            applyMysqlGrant(tableRef, grant, permissions);
        }
        if (allPermissionsGranted(permissions)) {
            return true;
        }
        if (inspection.grantsCollected && inspection.roles.isEmpty()) {
            replaceUnknownWithFalse(permissions);
            return true;
        }
        return false;
    }

    void applyMysqlGrant(TableRef tableRef, String grant, Map<String, Boolean> permissions) {
        String upper = grant.toUpperCase(Locale.ROOT);
        int onIndex = upper.indexOf(" ON ");
        if (!upper.startsWith("GRANT ") || onIndex < 0) {
            return;
        }
        String privilegePart = upper.substring("GRANT ".length(), onIndex).trim();
        String scope = upper.substring(onIndex + " ON ".length()).trim()
                .replace("`", "").replace("\"", "");
        if (!mysqlScopeMatches(tableRef, scope)) {
            return;
        }
        boolean all = privilegePart.contains("ALL PRIVILEGES") || "ALL".equals(privilegePart);
        for (String permission : TABLE_PERMISSION_NAMES) {
            if (all || Arrays.asList(privilegePart.split("\\s*,\\s*")).contains(permission)) {
                permissions.put(permission, true);
            }
        }
    }

    private boolean mysqlScopeMatches(TableRef tableRef, String scope) {
        if ("*.*".equals(scope)) {
            return true;
        }
        String catalog = trimToNull(tableRef.catalog);
        String table = trimToNull(tableRef.tableName);
        if (catalog == null || table == null) {
            return false;
        }
        return (catalog + ".*").equalsIgnoreCase(scope)
                || (catalog + "." + table).equalsIgnoreCase(scope);
    }

    private boolean collectJdbcTablePrivileges(DatabaseMetaData metaData, TableRef tableRef,
                                               PermissionInspection inspection,
                                               Map<String, Boolean> permissions) throws SQLException {
        try (ResultSet resultSet = metaData.getTablePrivileges(tableRef.catalog, tableRef.schema,
                tableRef.tableName)) {
            while (resultSet.next()) {
                String privilege = trimToNull(resultSet.getString("PRIVILEGE"));
                String grantee = trimToNull(resultSet.getString("GRANTEE"));
                if (privilege != null) {
                    String upper = privilege.toUpperCase(Locale.ROOT);
                    inspection.grants.add(upper + " ON " + tableRef.tableName
                            + (grantee == null ? "" : " TO " + grantee));
                    if (TABLE_PERMISSION_NAMES.contains(upper)) {
                        permissions.put(upper, true);
                    } else if ("ALL".equals(upper) || "ALL PRIVILEGES".equals(upper)) {
                        for (String permission : TABLE_PERMISSION_NAMES) {
                            permissions.put(permission, true);
                        }
                    }
                }
            }
        }
        return allPermissionsGranted(permissions);
    }

    private boolean isMysqlFamily(DbType dbType) {
        return dbType == DbType.MYSQL || dbType == DbType.MARIADB || dbType == DbType.OCEAN_BASE || dbType == DbType.TIDB;
    }

    private boolean isOracleFamily(DbType dbType) {
        return dbType == DbType.ORACLE || dbType == DbType.ORACLE_12C
                || dbType == DbType.DM8 || dbType == DbType.OCEAN_BASE_ORACLE;
    }

    private void initializeTablePermissions(Map<String, Boolean> permissions) {
        for (String permission : TABLE_PERMISSION_NAMES) {
            permissions.put(permission, null);
        }
    }

    private boolean allPermissionsGranted(Map<String, Boolean> permissions) {
        for (String permission : TABLE_PERMISSION_NAMES) {
            if (!Boolean.TRUE.equals(permissions.get(permission))) {
                return false;
            }
        }
        return true;
    }

    private void replaceUnknownWithFalse(Map<String, Boolean> permissions) {
        for (String permission : TABLE_PERMISSION_NAMES) {
            if (permissions.get(permission) == null) {
                permissions.put(permission, false);
            }
        }
    }

    private List<String> missingWritePermissions(Map<String, Boolean> permissions) {
        List<String> missing = new ArrayList<>();
        for (String permission : Arrays.asList("INSERT", "UPDATE", "DELETE")) {
            if (!Boolean.TRUE.equals(permissions.get(permission))) {
                missing.add(permission);
            }
        }
        return missing;
    }

    private String qualifyTable(DatabaseMetaData metaData, TableRef tableRef, DbType dbType) throws SQLException {
        List<String> parts = new ArrayList<>();
        if (usesCatalog(dbType) && trimToNull(tableRef.catalog) != null) {
            parts.add(quoteIdentifier(metaData, tableRef.catalog));
        }
        if (trimToNull(tableRef.schema) != null && !usesCatalogOnly(dbType)) {
            parts.add(quoteIdentifier(metaData, tableRef.schema));
        }
        parts.add(quoteIdentifier(metaData, tableRef.tableName));
        return String.join(".", parts);
    }

    private boolean usesCatalog(DbType dbType) {
        return usesCatalogOnly(dbType) || dbType == DbType.SQL_SERVER || dbType == DbType.SQL_SERVER2008;
    }

    private boolean usesCatalogOnly(DbType dbType) {
        return dbType == DbType.MYSQL || dbType == DbType.MARIADB || dbType == DbType.OCEAN_BASE || dbType == DbType.TIDB;
    }

    private String defaultSchema(DbQueryProperty property, DbType dbType) {
        if (usesCatalogOnly(dbType)) {
            return null;
        }
        if (isOracleFamily(dbType)) {
            return trimToNull(property.getDbName());
        }
        return trimToNull(property.getSid());
    }

    private String quoteIdentifier(DatabaseMetaData metaData, String identifier) {
        if (identifier == null) {
            throw new IllegalArgumentException("数据库标识符不能为空");
        }
        try {
            String quote = trimToNull(metaData.getIdentifierQuoteString());
            if (quote == null) {
                if (!identifier.matches("[A-Za-z0-9_$#]+")) {
                    throw new IllegalArgumentException("数据库标识符包含不安全字符");
                }
                return identifier;
            }
            return quote + identifier.replace(quote, quote + quote) + quote;
        } catch (SQLException ex) {
            throw new IllegalArgumentException("无法读取数据库标识符规则", ex);
        }
    }

    private void addPass(IngestDatasourceDiagnosisRespVO result, String code, String name,
                         String message, long startedAt) {
        resultBuilder.addPass(result, code, name, message, startedAt);
    }

    private void addWarn(IngestDatasourceDiagnosisRespVO result, String code, String name,
                         String message, String impact, List<String> suggestions, long durationMs) {
        resultBuilder.addWarn(result, code, name, message, impact, suggestions, durationMs);
    }

    private void addFailure(IngestDatasourceDiagnosisRespVO result, String checkCode, String checkName,
                            Throwable exception, String defaultErrorCode, String impact) {
        resultBuilder.addFailure(result, checkCode, checkName, exception, defaultErrorCode, impact);
    }

    private void addItem(IngestDatasourceDiagnosisRespVO result, String code, String name, String status,
                         String message, String impact, List<String> suggestions, Long durationMs) {
        resultBuilder.addItem(result, code, name, status, message, impact, suggestions, durationMs);
    }

    private IngestDatasourceDiagnosisRespVO finish(IngestDatasourceDiagnosisRespVO result, long startedAt) {
        return resultBuilder.finish(result, startedAt);
    }

    private void setQueryTimeout(Statement statement) {
        try {
            statement.setQueryTimeout(SQL_TIMEOUT_SECONDS);
        } catch (SQLException ignored) {
            // 部分驱动不支持查询超时，不影响诊断主流程。
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ex) {
                log.debug("关闭诊断JDBC连接失败", ex);
            }
        }
    }

    private void closeQuietly(DbQuery dbQuery) {
        if (dbQuery != null) {
            try {
                dbQuery.close();
            } catch (Exception ex) {
                log.debug("关闭诊断数据源失败", ex);
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static class TableRef {
        private final String catalog;
        private final String schema;
        private final String tableName;

        TableRef(String catalog, String schema, String tableName) {
            this.catalog = catalog;
            this.schema = schema;
            this.tableName = tableName;
        }
    }

    @lombok.Data
    private static class AccountPermission {
        private String currentUser;
        private Map<String, Boolean> tablePermissions = new LinkedHashMap<>();
    }

    private static class PermissionInspection {
        private final Set<String> roles = new LinkedHashSet<>();
        private final Set<String> grants = new LinkedHashSet<>();
        private boolean rolesCollected;
        private boolean grantsCollected;
    }

}
