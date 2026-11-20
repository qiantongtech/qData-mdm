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

package tech.qiantong.qdata.mdm.ingest.reader.task;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskDbConfigVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.task.vo.IngestTaskSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceDatasourceRelMapper;
import tech.qiantong.qdata.mdm.ingest.service.datasource.impl.IngestDatasourceServiceImpl;

import javax.annotation.Resource;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.DATABASE;
import static tech.qiantong.qdata.mdm.ingest.enums.task.IngestTaskConstants.INCREMENTAL;

/**
 * 接入任务的数据库读取策略，供来源预览和正式采集共用。
 *
 * <p>复用连接服务取得 JDBC 连接，通过元数据校验来源对象和字段，再按所需列构造查询并同步回调。
 * 本类负责查询投影、增量筛选和来源资源释放，字段转换、目标入库及增量位置持久化由调用方负责。</p>
 *
 * <p>增量读取首次包含配置起点，后续排除已保存的位置，要求来源增量字段随新增或修改持续递增。
 * 后续到达但增量值不大于已保存位置的记录不会被读取，本策略不提供变更日志采集或迟到数据补偿。</p>
 *
 * @author chentangwen
 */
@Component
public class IngestDatabaseReader implements IngestReader {

    /** JDBC 查询超时，不代表包含逐条业务回调的任务总耗时上限。 */
    private static final int QUERY_TIMEOUT_SECONDS = 120;
    /** 普通驱动的批次读取提示，实际取数行为由驱动决定。 */
    private static final int FETCH_SIZE = 500;
    /** 单个 CLOB 的最大字符数，限制一次性转为字符串的内存开销。 */
    private static final int MAX_CLOB_CHARACTERS = 20 * 1024 * 1024;
    /** 单个 BLOB 的最大字节数。 */
    private static final int MAX_BLOB_BYTES = 20 * 1024 * 1024;

    @Resource
    private IngestDatasourceServiceImpl datasourceService;
    @Resource
    private IngestSourceDatasourceRelMapper sourceDatasourceRelMapper;

    @Override
    public String getType() {
        return DATABASE;
    }

    /**
     * 兼容只消费字段值的读取入口。
     *
     * @param task 接入任务配置
     * @param limit 最大回调条数，小于或等于 0 时不限制
     * @param consumer 来源字段值回调
     * @return 来源对象的完整字段列表
     * @throws ServiceException 配置、来源对象或数据库读取无效时抛出
     */
    public List<String> read(IngestTaskSaveReqVO task, int limit, Consumer<Map<String, Object>> consumer) {
        if (ObjectUtil.isNull(consumer)) {
            throw new ServiceException("数据库记录回调不能为空");
        }
        return read(new IngestReadContext(task, limit), row -> consumer.accept(row.getValues()));
    }

    /**
     * 校验来源系统与连接的关联，按字段配置生成只读查询并同步回调。
     *
     * <p>有条数限制时返回全部来源列，无限制时尽量只读取配置所需列。
     * PostgreSQL 使用只读事务游标，MySQL 使用驱动流式模式；其他驱动使用批次读取提示。
     * 资源清理异常作为主异常的附加信息保留，回调的运行时异常原样抛出。
     * 已完成回调不会因后续读取失败而回滚，事务及失败策略由调用方决定。</p>
     *
     * @param context 本次读取配置与进度
     * @param consumer 来源记录回调
     * @return 来源对象的完整字段列表
     * @throws ServiceException 入口配置、来源对象或 JDBC 读取无效，以及读取中断时抛出
     */
    @Override
    public List<String> read(IngestReadContext context, Consumer<IngestRow> consumer) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(consumer)) {
            throw new ServiceException("数据库读取上下文和记录回调不能为空");
        }
        checkInterrupted();
        IngestTaskSaveReqVO task = context.getTask();
        IngestTaskDbConfigVO config = task.getDbConfig();
        if (ObjectUtil.isNull(config) || ObjectUtil.isNull(config.getSourceObject())
                || config.getSourceObject().trim().isEmpty()
                || ObjectUtil.isNull(task.getSourceId()) || ObjectUtil.isNull(config.getDatasourceId())) {
            throw new ServiceException("请选择来源系统、数据库连接和数据对象");
        }
        boolean linked = sourceDatasourceRelMapper.selectBySourceIds(Collections.singletonList(task.getSourceId()))
                .stream().anyMatch(r -> config.getDatasourceId().equals(r.getDatasourceId()));
        if (!linked) {
            throw new ServiceException("数据库连接未关联当前来源系统");
        }
        DbQuery query = datasourceService.buildDbQuery(config.getDatasourceId());
        // DbQuery 未实现 AutoCloseable，使用适配器使关闭异常遵循自动资源管理的抑制规则。
        try (AutoCloseable queryResource = query::close;
             Connection connection = query.getConnection()) {
            connection.setReadOnly(true);
            Map<String, Integer> fieldTypes = getFieldTypes(connection, config);
            List<String> fields = new ArrayList<>(fieldTypes.keySet());
            context.validateFields(fields);
            boolean incremental = INCREMENTAL.equals(task.getIngestMode());
            Set<String> selected = context.requiredFields();
            // 预览返回全部来源列；执行只读取映射、标识及增量列。
            if (context.getLimit() > 0 || CollectionUtils.isEmpty(selected)) {
                selected = new LinkedHashSet<>(fields);
            }
            String sql = buildSelectSql(connection, task, selected, incremental,
                    ObjectUtil.isNotNull(context.getCurrentIncrementValue()));
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
            boolean transactionCursor = product.contains("postgresql");
            if (transactionCursor) {
                connection.setAutoCommit(false);
            }
            try (AutoCloseable cursorResource = transactionCursor ? connection::rollback : () -> { };
                 PreparedStatement statement = connection.prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                // MySQL 使用驱动流式模式；PostgreSQL 使用事务游标。
                statement.setFetchSize(product.contains("mysql") ? Integer.MIN_VALUE : FETCH_SIZE);
                if (context.getLimit() > 0) {
                    statement.setMaxRows(context.getLimit());
                }
                if (incremental) {
                    // 按元数据类型绑定，避免数字、日期列与 VARCHAR 参数比较时出现驱动兼容问题。
                    Object start = ObjectUtil.isNull(context.getCurrentIncrementValue())
                            ? config.getInitialIncrementValue() : context.getCurrentIncrementValue();
                    statement.setObject(1, start, fieldTypes.get(config.getIncrementField()));
                }
                try (ResultSet rows = statement.executeQuery()) {
                    ResultSetMetaData metadata = rows.getMetaData();
                    long position = 0;
                    while (!context.reachedLimit()) {
                        checkInterrupted();
                        if (!rows.next()) {
                            break;
                        }
                        context.emit(readRow(rows, metadata),
                                config.getSourceObject() + " 第" + (++position) + "条", consumer);
                    }
                }
            }
            return fields;
        } catch (RuntimeException ex) {
            // 回调异常（含取消任务）保留原始类型和原因，避免被误报为 JDBC 读取失败。
            throw ex;
        } catch (Exception ex) {
            ServiceException failure = new ServiceException("数据库读取失败，请检查连接、数据对象和增量配置");
            failure.initCause(ex);
            throw failure;
        }
    }

    /**
     * 在访问来源前及逐行读取前检查中断，保留线程的中断标志。
     *
     * @throws ServiceException 当前读取线程已被中断时抛出
     */
    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ServiceException("数据库读取已中断");
        }
    }

    /**
     * 校验表或视图存在性，获取来源列名及 JDBC 类型，供字段校验与增量参数绑定使用。
     *
     * @param connection 本次读取的来源连接
     * @param config 来源对象配置，模式名和对象名分别处理
     * @return 按驱动返回顺序排列的列名与 JDBC 类型
     * @throws SQLException 元数据查询失败时抛出
     * @throws ServiceException 对象名称无效、对象不存在或没有可读字段时抛出
     */
    private Map<String, Integer> getFieldTypes(Connection connection, IngestTaskDbConfigVO config) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String quote = metadata.getIdentifierQuoteString().trim();
        quoteIdentifier(config.getSourceObject(), quote);
        String catalog = connection.getCatalog();
        String schema = config.getSourceSchema();
        if (ObjectUtil.isNotNull(schema) && !schema.trim().isEmpty()) {
            quoteIdentifier(schema, quote);
            if (metadata.getDatabaseProductName().toLowerCase(Locale.ROOT).contains("mysql")) {
                // MySQL 的库名通过 catalog 查询，不能直接作为 JDBC schema 条件。
                catalog = schema;
                schema = null;
            }
        } else {
            schema = connection.getSchema();
        }
        String escape = metadata.getSearchStringEscape();
        String pattern = escapePattern(config.getSourceObject(), escape);
        String schemaPattern = ObjectUtil.isNull(schema) ? null : escapePattern(schema, escape);
        boolean exists = false;
        try (ResultSet tables = metadata.getTables(catalog, schemaPattern, pattern, new String[]{"TABLE", "VIEW"})) {
            while (tables.next()) {
                if (config.getSourceObject().equals(tables.getString("TABLE_NAME"))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) {
            throw new ServiceException("来源表或视图不存在");
        }
        Map<String, Integer> fields = new LinkedHashMap<>();
        try (ResultSet columns = metadata.getColumns(catalog, schemaPattern, pattern, null)) {
            while (columns.next()) {
                fields.put(columns.getString("COLUMN_NAME"), columns.getInt("DATA_TYPE"));
            }
        }
        if (CollectionUtils.isEmpty(fields)) {
            throw new ServiceException("来源对象没有可读取字段");
        }
        return fields;
    }

    /**
     * 元数据查询使用模式匹配，转义名称中的通配符，避免下划线匹配到其他表。
     *
     * @param name 需要精确匹配的对象或模式名称
     * @param escape 驱动提供的转义字符串，为空时保留原值
     * @return 元数据查询使用的匹配模式
     */
    private String escapePattern(String name, String escape) {
        if (ObjectUtil.isEmpty(escape)) {
            return name;
        }
        return name.replace(escape, escape + escape).replace("_", escape + "_").replace("%", escape + "%");
    }

    /**
     * 仅接受单段标识符，并使用驱动提供的引号；模式名和表名分别处理。
     *
     * @param text 未包含引号或限定前缀的标识符
     * @param quote 驱动提供并去除空白的标识符引号
     * @return 包含驱动引号的标识符
     * @throws ServiceException 标识符为空或包含不支持的字符时抛出
     */
    private String quoteIdentifier(String text, String quote) {
        if (ObjectUtil.isNull(text) || !text.matches("[\\p{L}_][\\p{L}\\p{N}_$]*")) {
            throw new ServiceException("数据对象或字段名称无效");
        }
        return quote + text + quote;
    }

    /**
     * 构造所需字段的 SELECT 查询，增量条件使用占位符绑定。
     *
     * <p>首次使用大于等于起点，续读使用大于已保存位置；按增量字段及唯一标识排序。
     * 同轮读取相同增量值的记录不会主动截断，后续新增的相同增量值记录不在续读范围内。</p>
     *
     * @param connection 来源连接，用于获取标识符引号
     * @param task 接入任务与来源对象配置
     * @param fields 已通过元数据校验的查询列
     * @param incremental 是否执行增量筛选
     * @param resumed 是否使用已保存的增量位置
     * @return 待执行的参数化 SQL
     * @throws SQLException 取得驱动元数据失败时抛出
     * @throws ServiceException 标识符、增量字段或首次起点无效时抛出
     */
    private String buildSelectSql(Connection connection, IngestTaskSaveReqVO task,
                                  Set<String> fields, boolean incremental, boolean resumed) throws SQLException {
        IngestTaskDbConfigVO config = task.getDbConfig();
        String quote = connection.getMetaData().getIdentifierQuoteString().trim();
        String table = quoteIdentifier(config.getSourceObject(), quote);
        if (ObjectUtil.isNotNull(config.getSourceSchema()) && !config.getSourceSchema().trim().isEmpty()) {
            table = quoteIdentifier(config.getSourceSchema(), quote) + "." + table;
        }
        String sql = "SELECT " + fields.stream().map(field -> quoteIdentifier(field, quote))
                .collect(Collectors.joining(", ")) + " FROM " + table;
        Set<String> order = new LinkedHashSet<>();
        if (incremental) {
            if (!fields.contains(config.getIncrementField()) || ObjectUtil.isNull(config.getInitialIncrementValue())) {
                throw new ServiceException("增量字段或首次起始值无效");
            }
            sql += " WHERE " + quoteIdentifier(config.getIncrementField(), quote) + (resumed ? " > ?" : " >= ?");
            order.add(config.getIncrementField());
        }
        if (CollectionUtils.isNotEmpty(task.getUniqueFields())) {
            order.addAll(task.getUniqueFields());
        }
        if (CollectionUtils.isNotEmpty(order)) {
            sql += " ORDER BY " + order.stream().map(field -> quoteIdentifier(field, quote))
                    .collect(Collectors.joining(", "));
        }
        return sql;
    }

    /**
     * 读取当前行，普通字段保留 JDBC 类型，大字段在回调前转为独立值并释放底层资源。
     *
     * @param rows 已定位到当前行的结果集
     * @param metadata 查询结果元数据
     * @return 按查询列顺序排列的字段和值；CLOB 为字符串，BLOB 为字节数组
     * @throws Exception 字段读取或大字段资源释放失败时抛出
     * @throws ServiceException 单个大字段超出读取上限时抛出
     */
    private Map<String, Object> readRow(ResultSet rows, ResultSetMetaData metadata) throws Exception {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            Object value = rows.getObject(i);
            if (value instanceof Clob) {
                Clob clob = (Clob) value;
                try (AutoCloseable clobResource = clob::free) {
                    long length = clob.length();
                    if (length > MAX_CLOB_CHARACTERS) {
                        throw new ServiceException("来源CLOB字段超过20M字符");
                    }
                    value = length == 0 ? "" : clob.getSubString(1, (int) length);
                }
            } else if (value instanceof Blob) {
                Blob blob = (Blob) value;
                try (AutoCloseable blobResource = blob::free) {
                    long length = blob.length();
                    if (length > MAX_BLOB_BYTES) {
                        throw new ServiceException("来源BLOB字段超过20MB");
                    }
                    value = length == 0 ? new byte[0] : blob.getBytes(1, (int) length);
                }
            }
            row.put(metadata.getColumnLabel(i), value);
        }
        return row;
    }
}
