-- 达梦 8 系统字典初始化，包含附件中的 6 类和 48 项。
-- 在 DBeaver 选中包含 SYSTEM_DICT_TYPE 和 SYSTEM_DICT_DATA 的业务 Schema 后执行整份脚本。
-- 模拟录入时间分布在 2026-10-28 至 2026-10-30。每类的创建时间早于该类的字典项。
-- 重跑时按字典编码补齐缺失项，只回填与旧版初始化数据匹配且未编辑记录的创建时间。
-- 执行后可在系统字典管理中刷新缓存。

MERGE INTO SYSTEM_DICT_TYPE t
USING (
    SELECT '模型字段分类' AS DICT_NAME, 'model_column_category' AS DICT_TYPE, '0' AS STATUS, '吴同' AS CREATE_BY, TIMESTAMP '2026-10-28 09:16:42' AS CREATE_TIME, 'COLUMN_CATEGORY' AS REMARK FROM DUAL
    UNION ALL SELECT '模型编码方式', 'model_encoding_mode', '0', '吴同', TIMESTAMP '2026-10-28 10:11:07', 'ENCODING_MODE' FROM DUAL
    UNION ALL SELECT '模型字段属性', 'model_field_property', '0', '吴同', TIMESTAMP '2026-10-29 09:13:56', 'FIELD_PROPERTY' FROM DUAL
    UNION ALL SELECT '模型字段数据类型', 'model_field_data_type', '0', '吴同', TIMESTAMP '2026-10-29 14:18:24', 'FIELD_DATA_TYPE' FROM DUAL
    UNION ALL SELECT '模型分层最大深度', 'model_layer_max_depth', '0', '吴同', TIMESTAMP '2026-10-30 09:09:17', 'MAX_LAYER_DEPTH' FROM DUAL
    UNION ALL SELECT '模型系统字段编码', 'model_system_field_code', '0', '吴同', TIMESTAMP '2026-10-30 10:12:39', 'SYSTEM_FIELD_CODE' FROM DUAL
) s ON (t.DICT_TYPE = s.DICT_TYPE)
WHEN MATCHED THEN UPDATE SET
    t.CREATE_TIME = s.CREATE_TIME
WHERE t.CREATE_BY = s.CREATE_BY AND t.REMARK = s.REMARK
  AND t.DICT_NAME = s.DICT_NAME AND t.STATUS = s.STATUS
  AND t.UPDATE_TIME IS NULL AND COALESCE(LENGTH(TRIM(t.UPDATE_BY)), 0) = 0
  AND (t.CREATE_TIME IS NULL OR t.CREATE_TIME > s.CREATE_TIME)
WHEN NOT MATCHED THEN INSERT
    (DICT_NAME, DICT_TYPE, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (s.DICT_NAME, s.DICT_TYPE, s.STATUS, s.CREATE_BY, s.CREATE_TIME, s.REMARK);

MERGE INTO SYSTEM_DICT_DATA t
USING (
    SELECT 1 AS DICT_SORT, '普通字段' AS DICT_LABEL, 'BUSINESS' AS DICT_VALUE, 'model_column_category' AS DICT_TYPE, 'default' AS LIST_CLASS, 'N' AS IS_DEFAULT, '0' AS STATUS, '吴同' AS CREATE_BY, TIMESTAMP '2026-10-28 09:17:55' AS CREATE_TIME, 'COLUMN_CATEGORY.BUSINESS' AS REMARK FROM DUAL
    UNION ALL SELECT 2, '关联字段', 'RELATION', 'model_column_category', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-28 09:19:41', 'COLUMN_CATEGORY.RELATION' FROM DUAL
    UNION ALL SELECT 3, '分类字段', 'CLASS', 'model_column_category', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-28 09:21:03', 'COLUMN_CATEGORY.CLASS' FROM DUAL
    UNION ALL SELECT 4, '系统字段', 'SYSTEM', 'model_column_category', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-28 09:23:02', 'COLUMN_CATEGORY.SYSTEM' FROM DUAL

    UNION ALL SELECT 1, '手工录入', 'MANUAL', 'model_encoding_mode', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-28 10:12:20', 'ENCODING_MODE.MANUAL' FROM DUAL
    UNION ALL SELECT 2, '自动编码', 'AUTOMATIC', 'model_encoding_mode', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-28 10:14:06', 'ENCODING_MODE.AUTOMATIC' FROM DUAL

    UNION ALL SELECT 1, '字段名称', 'name', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:15:09', 'FIELD_PROPERTY.NAME' FROM DUAL
    UNION ALL SELECT 2, '字段编码', 'code', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:16:55', 'FIELD_PROPERTY.CODE' FROM DUAL
    UNION ALL SELECT 3, '字段描述', 'description', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:18:17', 'FIELD_PROPERTY.DESCRIPTION' FROM DUAL
    UNION ALL SELECT 4, '显示顺序', 'sortOrder', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:20:16', 'FIELD_PROPERTY.SORT_ORDER' FROM DUAL
    UNION ALL SELECT 5, '存储类型', 'storageType', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:21:24', 'FIELD_PROPERTY.STORAGE_TYPE' FROM DUAL
    UNION ALL SELECT 6, '数据类型', 'dataType', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:22:55', 'FIELD_PROPERTY.DATA_TYPE' FROM DUAL
    UNION ALL SELECT 7, '字段长度', 'dataLength', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:25:08', 'FIELD_PROPERTY.DATA_LENGTH' FROM DUAL
    UNION ALL SELECT 8, '小数位数', 'dataScale', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:26:25', 'FIELD_PROPERTY.DATA_SCALE' FROM DUAL
    UNION ALL SELECT 9, '是否主键', 'primaryKeyFlag', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:28:19', 'FIELD_PROPERTY.PRIMARY_KEY_FLAG' FROM DUAL
    UNION ALL SELECT 10, '是否必填', 'requiredFlag', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:29:56', 'FIELD_PROPERTY.REQUIRED_FLAG' FROM DUAL
    UNION ALL SELECT 11, '是否唯一', 'uniqueFlag', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:31:01', 'FIELD_PROPERTY.UNIQUE_FLAG' FROM DUAL
    UNION ALL SELECT 12, '是否名称字段', 'nameFlag', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:32:14', 'FIELD_PROPERTY.NAME_FLAG' FROM DUAL
    UNION ALL SELECT 13, '参考数据', 'dictTypeId', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:34:00', 'FIELD_PROPERTY.DICT_TYPE_ID' FROM DUAL
    UNION ALL SELECT 14, '编码方式', 'encodingMode', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:35:22', 'FIELD_PROPERTY.ENCODING_MODE' FROM DUAL
    UNION ALL SELECT 15, '编码方案版本', 'encodingVersionId', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:37:21', 'FIELD_PROPERTY.ENCODING_VERSION_ID' FROM DUAL
    UNION ALL SELECT 16, '编码规则配置', 'encodingRuleConfig', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:38:29', 'FIELD_PROPERTY.ENCODING_RULE_CONFIG' FROM DUAL
    UNION ALL SELECT 17, '关联模型', 'relatedModelId', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:40:00', 'FIELD_PROPERTY.RELATED_MODEL_ID' FROM DUAL
    UNION ALL SELECT 18, '关联键字段', 'relatedCode', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:42:13', 'FIELD_PROPERTY.RELATED_CODE' FROM DUAL
    UNION ALL SELECT 19, '展示字段', 'displayCode', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:43:30', 'FIELD_PROPERTY.DISPLAY_CODE' FROM DUAL
    UNION ALL SELECT 20, '展示方式', 'displayMode', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:45:24', 'FIELD_PROPERTY.DISPLAY_MODE' FROM DUAL
    UNION ALL SELECT 21, '节点选择规则', 'nodeRule', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:47:01', 'FIELD_PROPERTY.NODE_RULE' FROM DUAL
    UNION ALL SELECT 22, '默认值', 'defaultValue', 'model_field_property', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 09:48:06', 'FIELD_PROPERTY.DEFAULT_VALUE' FROM DUAL

    UNION ALL SELECT 1, 'string', 'STRING', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:19:37', 'FIELD_DATA_TYPE.STRING' FROM DUAL
    UNION ALL SELECT 2, 'integer', 'INTEGER', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:21:23', 'FIELD_DATA_TYPE.INTEGER' FROM DUAL
    UNION ALL SELECT 3, 'decimal', 'DECIMAL', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:22:45', 'FIELD_DATA_TYPE.DECIMAL' FROM DUAL
    UNION ALL SELECT 4, 'boolean', 'BOOLEAN', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:24:44', 'FIELD_DATA_TYPE.BOOLEAN' FROM DUAL
    UNION ALL SELECT 5, 'date', 'DATE', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:25:52', 'FIELD_DATA_TYPE.DATE' FROM DUAL
    UNION ALL SELECT 6, 'datetime', 'DATETIME', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:27:23', 'FIELD_DATA_TYPE.DATETIME' FROM DUAL
    UNION ALL SELECT 7, 'text', 'TEXT', 'model_field_data_type', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-29 14:29:36', 'FIELD_DATA_TYPE.TEXT' FROM DUAL

    UNION ALL SELECT 1, '最大层级深度', '5', 'model_layer_max_depth', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 09:10:30', 'MAX_LAYER_DEPTH' FROM DUAL

    UNION ALL SELECT 1, '主数据主键', 'ID', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:13:52', 'SYSTEM_FIELD_CODE.ID' FROM DUAL
    UNION ALL SELECT 2, '父记录ID', 'PARENT_ID', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:15:38', 'SYSTEM_FIELD_CODE.PARENT_ID' FROM DUAL
    UNION ALL SELECT 3, '分类ID', 'CATEGORY_ID', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:17:00', 'SYSTEM_FIELD_CODE.CATEGORY_ID' FROM DUAL
    UNION ALL SELECT 4, '数据来源', 'DATA_SOURCE', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:18:59', 'SYSTEM_FIELD_CODE.DATA_SOURCE' FROM DUAL
    UNION ALL SELECT 5, '创建人', 'CREATED_BY', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:20:07', 'SYSTEM_FIELD_CODE.CREATED_BY' FROM DUAL
    UNION ALL SELECT 6, '创建时间', 'CREATED_TIME', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:21:38', 'SYSTEM_FIELD_CODE.CREATED_TIME' FROM DUAL
    UNION ALL SELECT 7, '创建人ID', 'CREATED_BY_ID', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:23:51', 'SYSTEM_FIELD_CODE.CREATED_BY_ID' FROM DUAL
    UNION ALL SELECT 8, '更新人', 'UPDATED_BY', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:25:08', 'SYSTEM_FIELD_CODE.UPDATED_BY' FROM DUAL
    UNION ALL SELECT 9, '更新人ID', 'UPDATED_BY_ID', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:27:02', 'SYSTEM_FIELD_CODE.UPDATED_BY_ID' FROM DUAL
    UNION ALL SELECT 10, '更新时间', 'UPDATED_TIME', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:28:39', 'SYSTEM_FIELD_CODE.UPDATED_TIME' FROM DUAL
    UNION ALL SELECT 11, '备注', 'REMARK', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:29:44', 'SYSTEM_FIELD_CODE.REMARK' FROM DUAL
    UNION ALL SELECT 12, '状态', 'STATUS', 'model_system_field_code', 'default', 'N', '0', '吴同', TIMESTAMP '2026-10-30 10:30:57', 'SYSTEM_FIELD_CODE.STATUS' FROM DUAL
) s ON (t.DICT_TYPE = s.DICT_TYPE AND t.DICT_VALUE = s.DICT_VALUE)
WHEN MATCHED THEN UPDATE SET
    t.CREATE_TIME = s.CREATE_TIME
WHERE t.CREATE_BY = s.CREATE_BY AND t.REMARK = s.REMARK
  AND t.DICT_LABEL = s.DICT_LABEL AND t.DICT_SORT = s.DICT_SORT
  AND t.CSS_CLASS IS NULL AND t.LIST_CLASS = s.LIST_CLASS AND t.IS_DEFAULT = s.IS_DEFAULT AND t.STATUS = s.STATUS
  AND t.UPDATE_TIME IS NULL AND COALESCE(LENGTH(TRIM(t.UPDATE_BY)), 0) = 0
  AND (t.CREATE_TIME IS NULL OR t.CREATE_TIME > s.CREATE_TIME)
WHEN NOT MATCHED THEN INSERT
    (DICT_SORT, DICT_LABEL, DICT_VALUE, DICT_TYPE, CSS_CLASS, LIST_CLASS, IS_DEFAULT, STATUS, CREATE_BY, CREATE_TIME, REMARK)
VALUES (s.DICT_SORT, s.DICT_LABEL, s.DICT_VALUE, s.DICT_TYPE, NULL, s.LIST_CLASS, s.IS_DEFAULT, s.STATUS,
        s.CREATE_BY, s.CREATE_TIME, s.REMARK);

COMMIT;

-- 执行后核对各字典项数量。已存在的自定义项也会计入。
SELECT T.DICT_TYPE, T.DICT_NAME, COUNT(D.DICT_CODE) AS ITEM_COUNT
FROM SYSTEM_DICT_TYPE T
LEFT JOIN SYSTEM_DICT_DATA D ON D.DICT_TYPE = T.DICT_TYPE
WHERE T.DICT_TYPE IN (
    'model_column_category',
    'model_encoding_mode',
    'model_field_property',
    'model_field_data_type',
    'model_layer_max_depth',
    'model_system_field_code'
)
GROUP BY T.DICT_TYPE, T.DICT_NAME
ORDER BY T.DICT_TYPE;
