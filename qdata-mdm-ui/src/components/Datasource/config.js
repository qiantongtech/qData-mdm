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

/**
 * 数据源类型
 * key: 数据源类型标识跟config关联 不要改...
 * label: 数据源类型名称 用于展示
 * value: 数据源类型标识 用于传给后端
 * description: 数据源描述 用于卡片展示
 * category: 数据源分类 用于分类展示
 */
// 合并后的数据源配置：包含分类信息和具体数据源类型
export const DatasourceConfig = [
    {
        key: 'relational',
        label: '关系型数据库',
        icon: 'data-source-category/relational.svg',
        types: {
            MySql: { key: 'MySql', label: 'MySQL', value: 'MySql', type: 'primary', description: '常用开源关系型数据库，适用于业务系统数据接入。', category: 'relational', icon: 'datasource/db/type/my-sql.svg', canDiagnose: true },
            Oracle: { key: 'Oracle', label: 'Oracle', value: 'Oracle', type: 'primary', description: '企业级关系型数据库，适用于核心业务数据接入。', category: 'relational', icon: 'datasource/db/type/oracle.svg', canDiagnose: true },
            Oracle11: { key: 'Oracle11', label: 'Oracle11', value: 'Oracle11', type: 'primary', description: 'Oracle 11g 版本数据库，适用于历史系统数据接入。', category: 'relational', icon: 'datasource/db/type/oracle.svg', canDiagnose: true },
            PostgreSQL: { key: 'PostgreSQL', label: 'PostgreSQL', value: 'PostgreSQL', type: 'primary', description: '开源关系型数据库，适用于业务库与分析场景。', category: 'relational', icon: 'datasource/db/type/postgresql.svg', tagType: 'info', canDiagnose: true },
            SQL_Server: { key: 'SQL_Server', label: 'SQL Server', value: 'SQL_Server', type: 'info', description: 'Microsoft SQL Server 数据库，适用于企业业务系统。', category: 'relational', icon: 'datasource/db/type/sqlserver.svg', canDiagnose: true },
            SQL_Server2008: {
                key: 'SQL_Server2008',
                label: 'SQL Server 2008',
                value: 'SQL_Server2008',
                type: 'info',
                description: 'SQL Server 2008 版本数据库，适用于旧系统兼容接入。',
                category: 'relational',
                icon: 'datasource/db/type/sqlserver.svg',
                canDiagnose: true
            },
            DB2: { key: 'DB2', label: 'DB2', value: 'DB2', type: 'primary', description: 'IBM DB2 关系型数据库，适用于企业级业务数据接入。', category: 'relational', icon: 'datasource/db/type/db2.svg', tagType: 'info' },
            DM8: { key: 'DM8', label: 'DM8', value: 'DM8', type: 'info', description: '达梦数据库，适用于国产化业务系统数据接入。', category: 'relational', icon: 'datasource/db/type/dm8.svg' },
            Kingbase8: { key: 'Kingbase8', label: 'Kingbase8', value: 'Kingbase8', type: 'info', description: '人大金仓数据库，适用于国产化数据库场景。', category: 'relational', icon: 'datasource/db/type/kingbase8.svg', tagType: 'info' },
            GBase: { key: 'GBase', label: 'GBase', value: 'GBase', type: 'primary', description: '南大通用数据库，适用于国产数据库数据接入。', category: 'relational', icon: 'datasource/db/type/gbase.svg', tagType: 'info' },
            OSCAR: { key: 'OSCAR', label: 'OSCAR', value: 'OSCAR', type: 'primary', description: '神通数据库，适用于国产化业务系统接入。', category: 'relational', icon: 'datasource/db/type/oscar.svg', tagType: 'info' },
            OCEAN_BASE: { key: 'OCEAN_BASE', label: 'OceanBase（MySQL模式）', value: 'OCEANBASE', type: 'primary', description: 'OceanBase MySQL 兼容租户。', category: 'relational', icon: 'datasource/db/type/ocean-base.svg', tagType: 'info' },
            OCEAN_BASE_ORACLE: { key: 'OCEAN_BASE_ORACLE', label: 'OceanBase（Oracle模式）', value: 'OCEANBASE_ORACLE', type: 'primary', description: 'OceanBase Oracle 兼容租户。', category: 'relational', icon: 'datasource/db/type/ocean-base.svg', tagType: 'info' },
            TIDB: { key: 'TIDB', label: 'TIDB', value: 'TIDB', type: 'primary', description: '分布式关系型数据库，支撑实时采集、离线计算与数据治理各类任务。', category: 'relational', icon: 'datasource/db/type/tidb.svg', tagType: 'info' },
            OPEN_GAUSS: { key: 'openGauss', label: 'openGauss', value: 'openGauss', type: 'primary', description: '开源企业级关系型数据库，适用于业务系统与分析场景。', category: 'relational', icon: 'datasource/db/type/open-gauss.svg', canDiagnose: true },
            GBASE8C: { key: 'GBase8c', label: 'GBase 8c', value: 'GBase 8c', type: 'primary', description: '分布式关系型数据库，适用于企业级业务与数据分析。', category: 'relational', icon: 'datasource/db/type/gbase8c.svg', canDiagnose: true },
            GAUSS_DB: { key: 'GaussDB', label: 'GaussDB', value: 'GaussDB', type: 'primary', description: '分布式数据仓库，适用于海量数据分析与数仓场景。', category: 'relational', icon: 'datasource/db/type/gauss-db.png', canDiagnose: true },
            HIGHGO: { key: 'HighGo', label: '瀚高数据库', value: 'HighGo', type: 'primary', description: '瀚高国产关系型数据库，按 PostgreSQL 兼容模式接入。', category: 'relational', icon: 'datasource/db/type/high-go.png', tagType: 'info', canDiagnose: true },
            // MariaDB: {key: 'MariaDB', label: 'MariaDB', value: 'MariaDB', type: 'primary', description: 'MySQL 分支开源关系型数据库，中小型业务、网站系统广泛部署。', category: 'relational', icon: 'datasource/db/type/MariaDB.svg', tagType: 'info' },
        }
    }
];

// 从 DatasourceConfig 生成旧的 DatasourceCategories（保持向后兼容）
export const DatasourceCategories = DatasourceConfig.map(({ key, label, icon }) => ({ key, label, icon }));

// 从 DatasourceConfig 生成旧的 DatasourceTypesByCategory（保持向后兼容）
const DatasourceTypesByCategory = {};
DatasourceConfig.forEach(({ key, types }) => {
    DatasourceTypesByCategory[key] = types;
});
export { DatasourceTypesByCategory };

// 将配置扁平化，保持向后兼容（只包含真实的数据源，不包含重复别名）
const flattenDatasourceTypes = (config) => {
    const result = {};
    config.forEach(({ types }) => {
        Object.assign(result, types);
    });
    return result;
};

export const DatasourceTypes = flattenDatasourceTypes(DatasourceConfig);

// 向后兼容：别名只用于查找功能，不添加到 DatasourceTypes 中（避免重复显示）
// 从 DatasourceConfig 构建一个完整的查找表，包含所有别名
const buildTypeMap = () => {
    const typeMap = {};

    // 先添加标准类型
    DatasourceConfig.forEach(category => {
        Object.entries(category.types).forEach(([key, config]) => {
            typeMap[key.toUpperCase()] = config;
            // 同时也用 value（转为大写）作为 key，以便支持通过 value 查找
            if (config.value) {
                typeMap[config.value.toUpperCase()] = config;
            }
        });
    });

    // 添加向后兼容的别名（只用于查找，不会导致显示重复）
    // 格式：别名 -> 真实 key
    const aliasMappings = [
        ['DM', 'DM8'],
        ['SQLERVER', 'SQL_Server'],
        ['SQLSERVER', 'SQL_Server'],
        ['KINGBASE', 'Kingbase8'],
    ];

    // 为别名建立映射
    aliasMappings.forEach(([aliasKey, targetKey]) => {
        const targetConfig = typeMap[targetKey.toUpperCase()];
        if (targetConfig) {
            typeMap[aliasKey.toUpperCase()] = targetConfig;
        }
    });

    return typeMap;
};

const TYPE_MAP = buildTypeMap();

/**
 * 判断数据源类型是否支持新的诊断接口
 * @param {string} type 数据源类型标识
 * @returns {boolean}
 */
export const isDatasourceSupportDiagnosis = (type) => {
    if (!type) return false;
    const str = String(type).toUpperCase();

    // 优先尝试完全匹配
    let config = TYPE_MAP[str];

    // 模糊匹配（向下兼容部分特殊的类型别名）
    if (!config) {
        const sortedKeys = Object.keys(TYPE_MAP).sort((a, b) => b.length - a.length);
        const matchKey = sortedKeys.find(key => str.includes(key));
        if (matchKey) {
            config = TYPE_MAP[matchKey];
        }
    }

    return Boolean(config && config.canDiagnose);
};

/**
 * 获取分类图标
 * @param {string} iconPath 图标路径
 * @returns {string} 图标URL
 */
const datasourceAssets = {
    ...import.meta.glob("@/assets/common/icons/data-source-category/*.svg", { eager: true, import: "default" }),
    ...import.meta.glob("@/assets/common/icons/datasource/**/*.{svg,png}", { eager: true, import: "default" }),
    ...import.meta.glob("@/assets/common/images/datasource/**/*.{svg,png}", { eager: true, import: "default" })
};

const resolveDatasourceAsset = (iconPath) => {
    const assetType = /\.(png|jpe?g|gif|webp)$/i.test(iconPath) ? "images" : "icons";
    return datasourceAssets[`/src/assets/common/${assetType}/${iconPath}`] || "";
};

export const getCategoryIcon = (iconPath) => {
    if (!iconPath) return "";
    return resolveDatasourceAsset(iconPath);
};

/**
 * 获取数据源图标
 * @param {string} type 数据源类型
 * @returns {string} 图标URL
 */
export const getDatasourceIcon = (type) => {
    if (!type) return "";
    const str = String(type).toUpperCase();

    // 1. 尝试完全匹配
    let config = TYPE_MAP[str];

    // 2. 如果没有完全匹配，尝试模糊匹配 (判断是否包含关键字)
    if (!config) {
        // 按关键字长度降序排序，优先匹配长关键字（如 SQLSERVER 优先于 SQL）
        const sortedKeys = Object.keys(TYPE_MAP).sort((a, b) => b.length - a.length);
        const matchKey = sortedKeys.find(key => str.includes(key));
        if (matchKey) {
            config = TYPE_MAP[matchKey];
        }
    }

    if (!config || !config.icon) return "";

    return resolveDatasourceAsset(config.icon);
};

/**
 * 获取数据源标签类型
 * @param {string} type 数据源类型
 * @returns {string} 标签类型 (info, warning, success, etc.)
 */
export const getDatasourceTagType = (type) => {
    if (!type) return "success"; // 默认返回 success
    const key = type.toUpperCase();
    const config = TYPE_MAP[key];
    return (config && config.tagType) || "success";
};

/**
 * 数据源类型配置
 * key: 数据源类型标识
 * value: 1正常 2禁用 3隐藏
 */
export const config = {
    datasource: {},
    daAsset: {},
    daAssetUnstructured: {
        Oracle11: 3,
        MySql: 3,
        Oracle: 3,
        PostgreSQL: 3,
        SQL_Server2008: 3,
        SQL_Server: 3,
        DM8: 3,
        Kingbase8: 3,
        OSCAR: 3,
        DB2: 3,
    },
    daDataQuery: {},
    daDiscovery: {
        OSCAR: 2,
        DB2: 2,
    },
    // 模型物化：仅允许关系型数据库
    modelMaterialization: {},
    // 物化表 逻辑模型的东西
    dpModel: {},

    daQuality: {},
    dppIntegratioTaskInSPARK: {},
    dppIntegratioTaskOutSPARK: {},
    dppIntegratioTaskOdpsInSPARK: {
        MySql: 3,
        Oracle: 3,
        Oracle11: 3,
        PostgreSQL: 3,
        SQL_Server: 3,
        SQL_Server2008: 3,
        DB2: 3,
        DM8: 3,
        Kingbase8: 3,
        GBase: 3,
        OSCAR: 3,
    },
    dppIntegratioTaskOdpsOutSPARK: {
        MySql: 3,
        Oracle: 3,
        Oracle11: 3,
        PostgreSQL: 3,
        SQL_Server: 3,
        SQL_Server2008: 3,
        DB2: 3,
        DM8: 3,
        Kingbase8: 3,
        GBase: 3,
        OSCAR: 3,
    },
    dppIntegratioTaskInFLINK: {
        SQL_Server2008: 2,
        OSCAR: 2,
        DB2: 2,
    },
    dppIntegratioTaskOutFLINK: {
        SQL_Server2008: 2,
        OSCAR: 2,
        DB2: 2,
    },
    dsApi: {},
    dppIntegratioTaskInTDengine: {
        MySql: 3,
        DM8: 3,
        Oracle: 3,
        Oracle11: 3,
        Kingbase8: 3,
        SQL_Server: 3,
        SQL_Server2008: 3,
        PostgreSQL: 3,
        DB2: 3,
        OSCAR: 3,
    },
    mcStructuredTask: {},
    mcStructuredTaskFile: {
        MySql: 3,
        Oracle: 3,
        Oracle11: 3,
        PostgreSQL: 3,
        SQL_Server: 3,
        SQL_Server2008: 3,
        DB2: 3,
        DM8: 3,
        Kingbase8: 3,
        GBase: 3,
        OSCAR: 3,
    }
};
