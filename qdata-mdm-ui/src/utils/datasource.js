/*
 * Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

// Resolve datasource icons through the project asset alias.
const datasourceIconModules = import.meta.glob(
  [
    '@/assets/common/images/datasource/db/type/*.{png,jpg,jpeg,svg,webp,gif}',
    '@/assets/common/icons/datasource/db/type/*.svg',
    '@/assets/common/icons/icon-flink-one.svg',
    '@/assets/common/icons/icon-spark-one.svg'
  ],
  { eager: true, import: 'default' }
);
const datasourceIcons = Object.fromEntries(
  Object.entries(datasourceIconModules).map(([path, url]) => [
    path.split('/assets/common/')[1],
    url
  ])
);

// Data source configuration table (icon + label type)
const DATASOURCE_CONFIG = {
  DM: { icon: 'images/datasource/db/type/img-dm.png' },
  DM8: { icon: 'images/datasource/db/type/img-dm.png' },
  ORACLE: { icon: 'images/datasource/db/type/img-oracle.png' },
  ORACLE11: { icon: 'images/datasource/db/type/img-oracle.png' },
  MYSQL: { icon: 'images/datasource/db/type/img-mysql.png' },
  HIVE: { icon: 'images/datasource/db/type/img-hive.png', tagType: 'warning' },
  SQLERVER: { icon: 'images/datasource/db/type/img-sql-server.png', tagType: 'warning' },
  SQLSERVER: { icon: 'images/datasource/db/type/img-sql-server.png', tagType: 'warning' },
  KAFKA: { icon: 'images/datasource/db/type/img-kafka.png' },
  HDFS: { icon: 'images/datasource/db/type/img-hdfs.png' },
  SHELL: { icon: 'images/datasource/db/type/img-shell.png' },
  KINGBASE: { icon: 'images/datasource/db/type/img-kingbase.png', tagType: 'info' },
  KINGBASE8: { icon: 'images/datasource/db/type/img-kingbase.png', tagType: 'info' },
  POSTGRESQL: { icon: 'icons/datasource/db/type/icon-postgresql.svg', tagType: 'info' },
  SQL_SERVER: { icon: 'icons/datasource/db/type/icon-sql-server.svg' },
  SQL_SERVER2008: { icon: 'icons/datasource/db/type/icon-sql-server.svg' },
  DORIS: { icon: 'icons/datasource/db/type/icon-doris.svg' },
  DB2: { icon: 'icons/datasource/db/type/icon-db2.svg', tagType: 'info' },
  OSCAR: { icon: 'icons/datasource/db/type/icon-oscar.svg', tagType: 'info' },
  CLICKHOUSE: { icon: 'icons/datasource/db/type/icon-clickhouse.svg' },
  FLINK: { icon: 'icons/icon-flink-one.svg', tagType: 'success' },
  SPARK: { icon: 'icons/icon-spark-one.svg' },
  SPARKSQL: { icon: 'icons/icon-spark-one.svg' },
  FLINKBATCH: { icon: 'icons/icon-flink-one.svg', tagType: 'success' },
  FLINKSTREAM: { icon: 'icons/icon-flink-one.svg', tagType: 'success' }
};

/**
 * Get data source icon
 * @param {string} type data source type
 * @returns {string} Icon URL
 */
export const getDatasourceIcon = (type) => {
  if (!type) return '';
  const key = type.toUpperCase();
  const config = DATASOURCE_CONFIG[key];
  if (!config || !config.icon) return '';

  return datasourceIcons[config.icon] || '';
};

/**
 * Get data source label type
 * @param {string} type data source type
 * @returns {string} tag type (info, warning, success, etc.)
 */
export const getDatasourceTagType = (type) => {
  if (!type) return 'success'; // Returns success by default
  const key = type.toUpperCase();
  const config = DATASOURCE_CONFIG[key];
  return (config && config.tagType) || 'success';
};
