/*
 * Copyright © 2025 Qiantong Technology Co., Ltd.
 * qData Data Middle Platform (Open Source Edition)
 *  *
 * License:
 * Released under the Apache License, Version 2.0.
 * You may use, modify, and distribute this software for commercial purposes
 * under the terms of the License.
 *  *
 * Special Notice:
 * All derivative versions are strictly prohibited from modifying or removing
 * the default system logo and copyright information.
 * For brand customization, please apply for brand customization authorization via official channels.
 *  *
 * More information: https://qdata.qiantong.tech/business.html
 *  *
 * ============================================================================
 *  *
 * 版权所有 © 2025 江苏千桐科技有限公司
 * qData 主数据平台（开源版）
 *  *
 * 许可协议：
 * 本项目基于 Apache License 2.0 开源协议发布，
 * 允许在遵守协议的前提下进行商用、修改和分发。
 *  *
 * 特别说明：
 * 所有衍生版本不得修改或移除系统默认的 LOGO 和版权信息；
 * 如需定制品牌，请通过官方渠道申请品牌定制授权。
 *  *
 * 更多信息请访问：https://qdata.qiantong.tech/business.html
 */

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ca_cert
-- ----------------------------
DROP TABLE IF EXISTS `ca_cert`;
CREATE TABLE `ca_cert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID;ID',
  `name` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '证书名称',
  `subject_id` int NOT NULL COMMENT '主体id',
  `subject_name` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主体名称',
  `certificate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '证书;证书',
  `private_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '私钥;私钥',
  `issuer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '颁发者',
  `possessor` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有者',
  `valid_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '有效期',
  `valid_flag` int NOT NULL DEFAULT '1' COMMENT '是否有效;0：无效，1：有效',
  `del_flag` int NOT NULL DEFAULT '0' COMMENT '删除标志;1：已删除，0：未删除',
  `create_by` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `creator_id` int DEFAULT NULL COMMENT '创建人id',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注;备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ID_362432102577400` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='证书管理';

-- ----------------------------
-- Records of ca_cert
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for ca_subject
-- ----------------------------
DROP TABLE IF EXISTS `ca_subject`;
CREATE TABLE `ca_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID;主体ID',
  `name` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主体名称;主体名称',
  `cn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通用名称;通用名称',
  `ou` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织部门;组织单位名称',
  `o` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '组织名称',
  `l` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '城市名称',
  `st` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '省名称',
  `c` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家',
  `certificate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '证书;证书',
  `private_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '私钥;私钥',
  `valid_flag` int NOT NULL DEFAULT '1' COMMENT '是否有效;是否有效 0：无效，1：有效',
  `del_flag` int NOT NULL DEFAULT '0' COMMENT '删除标志;删除标志 1：已删除，0：未删除',
  `create_by` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人;创建人',
  `creator_id` int DEFAULT NULL COMMENT '创建人id;创建人id',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间;创建时间',
  `update_by` varchar(90) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人;更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间;更新时间',
  `remark` varchar(900) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注;备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ID_362432513978300` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='主体管理';

-- ----------------------------
-- Records of ca_subject
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for gen_table
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
  `table_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `tpl_web_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '前端模板类型（element-ui模版 element-plus模版）',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '其它生成选项',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`) USING BTREE,
  UNIQUE KEY `TABLE_ID_362025612516800` (`table_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=188 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表';

-- ----------------------------
-- Records of gen_table
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for gen_table_column
-- ----------------------------
DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
  `column_id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `sort` int DEFAULT NULL COMMENT '排序',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`) USING BTREE,
  UNIQUE KEY `COLUMN_ID_362026581858300` (`column_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3104 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='代码生成业务表字段';

-- ----------------------------
-- Records of gen_table_column
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `sender_id` bigint DEFAULT NULL COMMENT '发送人',
  `receiver_id` bigint DEFAULT NULL COMMENT '接收人',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息标题',
  `content` varchar(3072) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息模板内容',
  `category` int NOT NULL COMMENT '消息类别',
  `msg_level` int NOT NULL DEFAULT '0' COMMENT '消息等级',
  `module` int NOT NULL DEFAULT '0' COMMENT '消息模块',
  `entity_type` int DEFAULT NULL COMMENT '实体类型',
  `entity_id` bigint DEFAULT NULL COMMENT '实体id',
  `entity_url` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息链接',
  `has_read` tinyint DEFAULT '0' COMMENT '是否已读',
  `has_retraction` tinyint DEFAULT '0' COMMENT '是否撤回',
  `valid_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效;0：无效，1：有效',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志;1：已删除，0：未删除',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ID_362038349924500` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='消息';

-- ----------------------------
-- Records of message
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for message_template
-- ----------------------------
DROP TABLE IF EXISTS `message_template`;
CREATE TABLE `message_template` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息标题',
  `content` varchar(3072) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息模板内容',
  `category` int NOT NULL COMMENT '消息类别',
  `msg_level` int NOT NULL DEFAULT '0' COMMENT '消息等级',
  `valid_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否有效;0：无效，1：有效',
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '删除标志;1：已删除，0：未删除',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
  `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `ID_362038929647900` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='消息模板';

-- ----------------------------
-- Records of message_template
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `blob_data` longblob COMMENT '存放持久化Trigger对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Blob类型的触发器表';

-- ----------------------------
-- Records of qrtz_blob_triggers
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '日历名称',
  `calendar` longblob NOT NULL COMMENT '存放持久化calendar对象',
  PRIMARY KEY (`sched_name`,`calendar_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='日历信息表';

-- ----------------------------
-- Records of qrtz_calendars
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `cron_expression` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'cron表达式',
  `time_zone_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '时区',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Cron类型的触发器表';

-- ----------------------------
-- Records of qrtz_cron_triggers
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `entry_id` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度器实例id',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度器实例名',
  `fired_time` bigint NOT NULL COMMENT '触发的时间',
  `sched_time` bigint NOT NULL COMMENT '定时器制定的时间',
  `priority` int NOT NULL COMMENT '优先级',
  `state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务组名',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否并发',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '是否接受恢复执行',
  PRIMARY KEY (`sched_name`,`entry_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='已触发的触发器表';

-- ----------------------------
-- Records of qrtz_fired_triggers
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '相关介绍',
  `job_class_name` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '执行任务类名称',
  `is_durable` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否持久化',
  `is_nonconcurrent` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否并发',
  `is_update_data` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否更新数据',
  `requests_recovery` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '是否接受恢复执行',
  `job_data` longblob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`job_name`,`job_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='任务详细信息表';

-- ----------------------------
-- Records of qrtz_job_details
-- ----------------------------
BEGIN;
INSERT INTO `qrtz_job_details` (`sched_name`, `job_name`, `job_group`, `description`, `job_class_name`, `is_durable`, `is_nonconcurrent`, `is_update_data`, `requests_recovery`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME1', 'DEFAULT', NULL, 'com.ruoyi.common.utils.job.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', 0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000174000F5441534B5F50524F5045525449455373720027636F6D2E72756F79692E70726F6A6563742E6D6F6E69746F722E646F6D61696E2E5379734A6F6200000000000000010200084C000A636F6E63757272656E747400124C6A6176612F6C616E672F537472696E673B4C000E63726F6E45787072657373696F6E71007E00094C000C696E766F6B6554617267657471007E00094C00086A6F6247726F757071007E00094C00056A6F6249647400104C6A6176612F6C616E672F4C6F6E673B4C00076A6F624E616D6571007E00094C000D6D697366697265506F6C69637971007E00094C000673746174757371007E000978720029636F6D2E72756F79692E6672616D65776F726B2E7765622E646F6D61696E2E42617365456E7469747900000000000000010200094C0009626567696E54696D6571007E00094C0008637265617465427971007E00094C000A63726561746554696D6571007E00094C0007656E6454696D6571007E00094C0006706172616D7371007E00034C000672656D61726B71007E00094C000B73656172636856616C756571007E00094C0008757064617465427971007E00094C000A75706461746554696D6571007E000978707074000561646D696E740013323032342D30382D31382030313A32333A323970707400007070707400013174000E302F3130202A202A202A202A203F74001172795461736B2E72794E6F506172616D7374000744454641554C547372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000000000000001740018E7B3BBE7BB9FE9BB98E8AEA4EFBC88E697A0E58F82EFBC8974000133740001317800);
INSERT INTO `qrtz_job_details` (`sched_name`, `job_name`, `job_group`, `description`, `job_class_name`, `is_durable`, `is_nonconcurrent`, `is_update_data`, `requests_recovery`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME2', 'DEFAULT', NULL, 'com.ruoyi.common.utils.job.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', 0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000174000F5441534B5F50524F5045525449455373720027636F6D2E72756F79692E70726F6A6563742E6D6F6E69746F722E646F6D61696E2E5379734A6F6200000000000000010200084C000A636F6E63757272656E747400124C6A6176612F6C616E672F537472696E673B4C000E63726F6E45787072657373696F6E71007E00094C000C696E766F6B6554617267657471007E00094C00086A6F6247726F757071007E00094C00056A6F6249647400104C6A6176612F6C616E672F4C6F6E673B4C00076A6F624E616D6571007E00094C000D6D697366697265506F6C69637971007E00094C000673746174757371007E000978720029636F6D2E72756F79692E6672616D65776F726B2E7765622E646F6D61696E2E42617365456E7469747900000000000000010200094C0009626567696E54696D6571007E00094C0008637265617465427971007E00094C000A63726561746554696D6571007E00094C0007656E6454696D6571007E00094C0006706172616D7371007E00034C000672656D61726B71007E00094C000B73656172636856616C756571007E00094C0008757064617465427971007E00094C000A75706461746554696D6571007E000978707074000561646D696E740013323032342D30382D31382030313A32333A323970707400007070707400013174000E302F3135202A202A202A202A203F74001572795461736B2E7279506172616D7328277279272974000744454641554C547372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000000000000002740018E7B3BBE7BB9FE9BB98E8AEA4EFBC88E69C89E58F82EFBC8974000133740001317800);
INSERT INTO `qrtz_job_details` (`sched_name`, `job_name`, `job_group`, `description`, `job_class_name`, `is_durable`, `is_nonconcurrent`, `is_update_data`, `requests_recovery`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME3', 'DEFAULT', NULL, 'com.ruoyi.common.utils.job.QuartzDisallowConcurrentExecution', '0', '1', '0', '0', 0xACED0005737200156F72672E71756172747A2E4A6F62446174614D61709FB083E8BFA9B0CB020000787200266F72672E71756172747A2E7574696C732E537472696E674B65794469727479466C61674D61708208E8C3FBC55D280200015A0013616C6C6F77735472616E7369656E74446174617872001D6F72672E71756172747A2E7574696C732E4469727479466C61674D617013E62EAD28760ACE0200025A000564697274794C00036D617074000F4C6A6176612F7574696C2F4D61703B787001737200116A6176612E7574696C2E486173684D61700507DAC1C31660D103000246000A6C6F6164466163746F724900097468726573686F6C6478703F4000000000000C7708000000100000000174000F5441534B5F50524F5045525449455373720027636F6D2E72756F79692E70726F6A6563742E6D6F6E69746F722E646F6D61696E2E5379734A6F6200000000000000010200084C000A636F6E63757272656E747400124C6A6176612F6C616E672F537472696E673B4C000E63726F6E45787072657373696F6E71007E00094C000C696E766F6B6554617267657471007E00094C00086A6F6247726F757071007E00094C00056A6F6249647400104C6A6176612F6C616E672F4C6F6E673B4C00076A6F624E616D6571007E00094C000D6D697366697265506F6C69637971007E00094C000673746174757371007E000978720029636F6D2E72756F79692E6672616D65776F726B2E7765622E646F6D61696E2E42617365456E7469747900000000000000010200094C0009626567696E54696D6571007E00094C0008637265617465427971007E00094C000A63726561746554696D6571007E00094C0007656E6454696D6571007E00094C0006706172616D7371007E00034C000672656D61726B71007E00094C000B73656172636856616C756571007E00094C0008757064617465427971007E00094C000A75706461746554696D6571007E000978707074000561646D696E740013323032342D30382D31382030313A32333A323970707400007070707400013174000E302F3230202A202A202A202A203F74003872795461736B2E72794D756C7469706C65506172616D7328277279272C20747275652C20323030304C2C203331362E3530442C203130302974000744454641554C547372000E6A6176612E6C616E672E4C6F6E673B8BE490CC8F23DF0200014A000576616C7565787200106A6176612E6C616E672E4E756D62657286AC951D0B94E08B02000078700000000000000003740018E7B3BBE7BB9FE9BB98E8AEA4EFBC88E5A49AE58F82EFBC8974000133740001317800);
COMMIT;

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `lock_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '悲观锁名称',
  PRIMARY KEY (`sched_name`,`lock_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='存储的悲观锁信息表';

-- ----------------------------
-- Records of qrtz_locks
-- ----------------------------
BEGIN;
INSERT INTO `qrtz_locks` (`sched_name`, `lock_name`) VALUES ('RuoyiScheduler', 'TRIGGER_ACCESS');
COMMIT;

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  PRIMARY KEY (`sched_name`,`trigger_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='暂停的触发器表';

-- ----------------------------
-- Records of qrtz_paused_trigger_grps
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `instance_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '实例名称',
  `last_checkin_time` bigint NOT NULL COMMENT '上次检查时间',
  `checkin_interval` bigint NOT NULL COMMENT '检查间隔时间',
  PRIMARY KEY (`sched_name`,`instance_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='调度器状态表';

-- ----------------------------
-- Records of qrtz_scheduler_state
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `repeat_count` bigint NOT NULL COMMENT '重复的次数统计',
  `repeat_interval` bigint NOT NULL COMMENT '重复的间隔时间',
  `times_triggered` bigint NOT NULL COMMENT '已经触发的次数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='简单触发器的信息表';

-- ----------------------------
-- Records of qrtz_simple_triggers
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_name的外键',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_triggers表trigger_group的外键',
  `STR_PROP_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'String类型的trigger的第一个参数',
  `STR_PROP_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'String类型的trigger的第二个参数',
  `STR_PROP_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'String类型的trigger的第三个参数',
  `INT_PROP_1` int DEFAULT NULL COMMENT 'int类型的trigger的第一个参数',
  `INT_PROP_2` int DEFAULT NULL COMMENT 'int类型的trigger的第二个参数',
  `LONG_PROP_1` bigint DEFAULT NULL COMMENT 'long类型的trigger的第一个参数',
  `LONG_PROP_2` bigint DEFAULT NULL COMMENT 'long类型的trigger的第二个参数',
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第一个参数',
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL COMMENT 'decimal类型的trigger的第二个参数',
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第一个参数',
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Boolean类型的trigger的第二个参数',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='同步机制的行锁表';

-- ----------------------------
-- Records of qrtz_simprop_triggers
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers` (
  `sched_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调度名称',
  `trigger_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器的名字',
  `trigger_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器所属组的名字',
  `job_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_details表job_name的外键',
  `job_group` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'qrtz_job_details表job_group的外键',
  `description` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '相关介绍',
  `next_fire_time` bigint DEFAULT NULL COMMENT '上一次触发时间（毫秒）',
  `prev_fire_time` bigint DEFAULT NULL COMMENT '下一次触发时间（默认为-1表示不触发）',
  `priority` int DEFAULT NULL COMMENT '优先级',
  `trigger_state` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器状态',
  `trigger_type` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '触发器的类型',
  `start_time` bigint NOT NULL COMMENT '开始时间',
  `end_time` bigint DEFAULT NULL COMMENT '结束时间',
  `calendar_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日程表名称',
  `misfire_instr` smallint DEFAULT NULL COMMENT '补偿执行的策略',
  `job_data` longblob COMMENT '存放持久化job对象',
  PRIMARY KEY (`sched_name`,`trigger_name`,`trigger_group`) USING BTREE,
  KEY `sched_name` (`sched_name`,`job_name`,`job_group`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='触发器详细信息表';

-- ----------------------------
-- Records of qrtz_triggers
-- ----------------------------
BEGIN;
INSERT INTO `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`, `job_name`, `job_group`, `description`, `next_fire_time`, `prev_fire_time`, `priority`, `trigger_state`, `trigger_type`, `start_time`, `end_time`, `calendar_name`, `misfire_instr`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME1', 'DEFAULT', 'TASK_CLASS_NAME1', 'DEFAULT', NULL, 1723915630000, -1, 5, 'PAUSED', 'CRON', 1723915630000, 0, NULL, 2, NULL);
INSERT INTO `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`, `job_name`, `job_group`, `description`, `next_fire_time`, `prev_fire_time`, `priority`, `trigger_state`, `trigger_type`, `start_time`, `end_time`, `calendar_name`, `misfire_instr`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME2', 'DEFAULT', 'TASK_CLASS_NAME2', 'DEFAULT', NULL, 1723915635000, -1, 5, 'PAUSED', 'CRON', 1723915631000, 0, NULL, 2, NULL);
INSERT INTO `qrtz_triggers` (`sched_name`, `trigger_name`, `trigger_group`, `job_name`, `job_group`, `description`, `next_fire_time`, `prev_fire_time`, `priority`, `trigger_state`, `trigger_type`, `start_time`, `end_time`, `calendar_name`, `misfire_instr`, `job_data`) VALUES ('RuoyiScheduler', 'TASK_CLASS_NAME3', 'DEFAULT', 'TASK_CLASS_NAME3', 'DEFAULT', NULL, 1723915640000, -1, 5, 'PAUSED', 'CRON', 1723915632000, 0, NULL, 2, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_config
-- ----------------------------
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `config_id` int NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`) USING BTREE,
  UNIQUE KEY `CONFIG_ID_362528470964900` (`config_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='参数配置表';

-- ----------------------------
-- Records of system_config
-- ----------------------------
BEGIN;
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '主框架页-默认皮肤样式名称', 'sys.index.skinName', 'skin-blue', 'Y', '吴同', '2024-05-06 06:12:18', '吴同', '2025-03-07 14:07:03', '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow');
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', '吴同', '2024-05-06 06:12:18', NULL, NULL, '初始化密码 123456');
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '主框架页-侧边栏主题', 'sys.index.sideTheme', 'theme-dark', 'Y', '吴同', '2024-05-06 06:12:18', NULL, NULL, '深色主题theme-dark，浅色主题theme-light');
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, '账号自助-验证码开关', 'sys.account.captchaEnabled', 'true', 'Y', '吴同', '2024-05-06 06:12:18', NULL, NULL, '是否开启验证码功能（true开启，false关闭）');
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', '吴同', '2024-05-06 06:12:18', NULL, NULL, '是否开启注册用户功能（true开启，false关闭）');
INSERT INTO `system_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, '用户登录-黑名单列表', 'sys.login.blackIPList', '-', 'Y', '吴同', '2024-05-06 06:12:18', '吴同', '2024-11-26 15:51:27', '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
COMMIT;

-- ----------------------------
-- Table structure for system_content
-- ----------------------------
DROP TABLE IF EXISTS `system_content`;
CREATE TABLE `system_content` (
  `id` int NOT NULL COMMENT 'id',
  `sys_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统名称',
  `logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '系统logo',
  `login_logo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录页面logo',
  `carousel_image` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '轮播图',
  `contact_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `copyright` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '版权方',
  `record_number` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备案号',
  `del_flag` int DEFAULT NULL COMMENT '删除标记',
  `status` int DEFAULT NULL COMMENT '状态',
  `create_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
  `creator_id` int DEFAULT NULL COMMENT '创建人id',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '修改人',
  `updater_id` int DEFAULT NULL COMMENT '修改人id',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `config_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '配置信息',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of system_content
-- ----------------------------
BEGIN;
INSERT INTO `system_content` (`id`, `sys_name`, `logo`, `login_logo`, `carousel_image`, `contact_number`, `email`, `copyright`, `record_number`, `del_flag`, `status`, `create_by`, `creator_id`, `create_time`, `update_by`, `updater_id`, `update_time`, `remark`, `config_json`) VALUES (1, NULL, NULL, NULL, NULL, '400-660-8208', 'sales@qiantong.tech', 'Copyright ©2025 江苏千桐科技有限公司 版权所有', '苏ICP备2022008519号-1', 0, NULL, NULL, NULL, NULL, '吴大同', 1, '2025-09-16 17:00:27', NULL, '{\"copyrightHttp\":\"https://qiantong.tech\",\"systemMenuFullLogo\":\"\",\"systemMenuLogo\":\"\"}');
COMMIT;

-- ----------------------------
-- Table structure for system_dept
-- ----------------------------
DROP TABLE IF EXISTS `system_dept`;
CREATE TABLE `system_dept` (
  `dept_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门id',
  `ancestors` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `leader` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '负责人',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '联系电话',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '部门状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`dept_id`) USING BTREE,
  UNIQUE KEY `DEPT_ID_362529575293800` (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=875 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='部门表';

-- ----------------------------
-- Records of system_dept
-- ----------------------------
BEGIN;
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (852, 0, '0', '江苏千桐科技有限公司', 0, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:09:24', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (853, 852, '0,852', '南京总公司', 1, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:09:35', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (854, 852, '0,852', '郑州分公司', 2, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:09:43', 'admin', '2025-09-18 14:09:48');
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (855, 852, '0,852', '新疆分公司', 3, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:09:55', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (856, 852, '0,852', '合作伙伴', 4, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:10:05', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (857, 853, '0,852,853', '研发部门', 0, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:11:49', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (858, 853, '0,852,853', '市场部门', 1, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:12:06', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (859, 853, '0,852,853', '测试部门', 2, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:12:15', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (860, 853, '0,852,853', '财务部门', 3, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:12:26', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (861, 853, '0,852,853', '运维部门', 4, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:12:37', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (862, 854, '0,852,854', '研发部门', 0, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:14:22', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (863, 854, '0,852,854', '市场部门', 1, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:14:28', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (864, 854, '0,852,854', '测试部门', 2, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:14:36', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (865, 854, '0,852,854', '财务部门', 3, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:14:44', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (866, 854, '0,852,854', '运维部门', 4, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:14:55', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (867, 855, '0,852,855', '研发部门', 0, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:15:36', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (868, 855, '0,852,855', '市场部门', 1, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:15:46', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (869, 855, '0,852,855', '测试部门', 2, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:15:55', 'admin', '2025-09-18 14:16:12');
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (870, 855, '0,852,855', '财务部门', 3, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:16:06', 'admin', '2025-09-18 14:16:16');
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (871, 855, '0,852,855', '运维部门', 4, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:16:26', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (872, 856, '0,852,856', '研发部门', 0, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:16:36', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (873, 856, '0,852,856', '市场部门', 1, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:16:45', NULL, NULL);
INSERT INTO `system_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (874, 856, '0,852,856', '测试部门', 2, NULL, NULL, NULL, '0', '0', 'admin', '2025-09-18 14:16:53', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `system_dict_data`;
CREATE TABLE `system_dict_data` (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE,
  UNIQUE KEY `DICT_CODE_362530128201100` (`dict_code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=414 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典数据表';

-- ----------------------------
-- Records of system_dict_data
-- ----------------------------
BEGIN;
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, 1, '男', '0', 'sys_user_sex', NULL, NULL, 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '性别男');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, 2, '女', '1', 'sys_user_sex', NULL, NULL, 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '性别女');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, 3, '未知', '2', 'sys_user_sex', NULL, NULL, 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '性别未知');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, 1, '显示', '0', 'sys_show_hide', NULL, 'primary', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '显示菜单');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, 2, '隐藏', '1', 'sys_show_hide', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '隐藏菜单');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, 1, '正常', '0', 'sys_normal_disable', NULL, 'primary', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '正常状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7, 2, '停用', '1', 'sys_normal_disable', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '停用状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8, 1, '正常', '0', 'sys_job_status', NULL, 'primary', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '正常状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9, 2, '暂停', '1', 'sys_job_status', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '停用状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (10, 1, '默认', 'DEFAULT', 'sys_job_group', NULL, NULL, 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '默认分组');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (11, 2, '系统', 'SYSTEM', 'sys_job_group', NULL, NULL, 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '系统分组');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (12, 1, '是', 'Y', 'sys_yes_no', NULL, 'primary', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '系统默认是');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (13, 2, '否', 'N', 'sys_yes_no', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '系统默认否');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (14, 1, '通知', '1', 'sys_notice_type', NULL, 'warning', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '通知');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (15, 2, '公告', '2', 'sys_notice_type', NULL, 'success', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '公告');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (16, 1, '正常', '0', 'sys_notice_status', NULL, 'primary', 'Y', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '正常状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (17, 2, '关闭', '1', 'sys_notice_status', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '关闭状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (18, 99, '其他', '0', 'sys_oper_type', NULL, 'info', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '其他操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (19, 1, '新增', '1', 'sys_oper_type', NULL, 'info', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '新增操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (20, 2, '修改', '2', 'sys_oper_type', NULL, 'info', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '修改操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (21, 3, '删除', '3', 'sys_oper_type', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '删除操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (22, 4, '授权', '4', 'sys_oper_type', NULL, 'primary', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '授权操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (23, 5, '导出', '5', 'sys_oper_type', NULL, 'warning', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '导出操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (24, 6, '导入', '6', 'sys_oper_type', NULL, 'warning', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '导入操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (25, 7, '强退', '7', 'sys_oper_type', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '强退操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (26, 8, '生成代码', '8', 'sys_oper_type', NULL, 'warning', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '生成操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (27, 9, '清空数据', '9', 'sys_oper_type', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '清空操作');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (28, 1, '成功', '0', 'sys_common_status', NULL, 'primary', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '正常状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (29, 2, '失败', '1', 'sys_common_status', NULL, 'danger', 'N', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '停用状态');
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (30, 0, 'Web', '0', 'auth_app_type', NULL, 'primary', 'N', '0', '吴同', '2024-08-31 14:27:22', '吴同', '2024-08-31 14:27:31', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (31, 1, 'App', '1', 'auth_app_type', NULL, 'info', 'N', '0', '吴同', '2024-08-31 14:27:47', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (32, 2, '小程序', '2', 'auth_app_type', NULL, 'warning', 'N', '0', '吴同', '2024-08-31 14:28:06', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (33, 1, '有效', '1', 'sys_valid', NULL, 'success', 'N', '0', '吴同', '2024-08-31 15:33:49', '吴同', '2024-08-31 15:34:08', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (34, 0, '无效', '0', 'sys_valid', NULL, 'danger', 'N', '0', '吴同', '2024-08-31 15:34:21', '吴同', '2025-01-21 16:31:24', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (35, 0, '非公开', '0', 'auth_public', NULL, 'warning', 'N', '0', '吴同', '2024-08-31 15:36:48', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (36, 1, '公开', '1', 'auth_public', NULL, 'success', 'N', '0', '吴同', '2024-08-31 15:36:59', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (37, 0, '通知', '0', 'message_category', NULL, 'success', 'N', '0', '吴同', '2024-11-07 14:28:29', '吴同', '2024-12-19 14:19:58', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (38, 0, '重要', '0', 'message_level', NULL, 'default', 'N', '0', '吴同', '2024-11-07 14:28:42', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (39, 2, '审批', '2', 'message_category', NULL, 'warning', 'N', '0', '吴同', '2024-11-19 14:20:42', '吴同', '2024-11-19 16:55:16', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (41, 3, '其他', '3', 'message_category', NULL, 'info', 'N', '0', '吴同', '2024-11-19 14:21:12', '吴同', '2024-12-19 14:19:31', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (42, 1, '公告', '1', 'message_category', NULL, 'primary', 'N', '0', '吴同', '2024-11-19 16:54:18', '吴同', '2024-11-19 16:55:05', NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (43, 0, '是', 'true', 'sys_boolean', NULL, 'success', 'N', '0', '吴同', '2024-12-03 15:46:40', NULL, NULL, NULL);
INSERT INTO `system_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (44, 0, '否', 'false', 'sys_boolean', NULL, 'danger', 'N', '0', '吴同', '2024-12-03 15:46:57', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_dict_type
-- ----------------------------
DROP TABLE IF EXISTS `system_dict_type`;
CREATE TABLE `system_dict_type` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '字典类型',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`) USING BTREE,
  UNIQUE KEY `DICT_ID_362530784644900` (`dict_id`) USING BTREE,
  UNIQUE KEY `dict_type` (`dict_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=108 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='字典类型表';

-- ----------------------------
-- Records of system_dict_type
-- ----------------------------
BEGIN;
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '用户性别', 'sys_user_sex', '0', '吴同', '2024-05-06 06:12:18', '吴同', '2025-03-07 14:06:58', '用户性别列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '菜单状态', 'sys_show_hide', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '菜单状态列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '系统开关', 'sys_normal_disable', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '系统开关列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, '任务状态', 'sys_job_status', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '任务状态列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, '任务分组', 'sys_job_group', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '任务分组列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, '系统是否', 'sys_yes_no', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '系统是否列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7, '通知类型', 'sys_notice_type', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '通知类型列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8, '通知状态', 'sys_notice_status', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '通知状态列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (9, '操作类型', 'sys_oper_type', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '操作类型列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (10, '系统状态', 'sys_common_status', '0', '吴同', '2024-05-06 06:12:18', NULL, NULL, '登录状态列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (11, '应用类型', 'auth_app_type', '0', '吴同', '2024-08-31 14:27:03', NULL, NULL, '应用类型列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (12, '是否有效', 'sys_valid', '0', '吴同', '2024-08-31 15:33:08', '吴同', '2024-08-31 15:36:13', '系统有效列表');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (13, '是否公开', 'auth_public', '0', '吴同', '2024-08-31 15:35:49', '吴同', '2024-08-31 15:36:00', '应用是否公开');
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (14, '消息类型', 'message_category', '0', '吴同', '2024-11-07 14:27:58', NULL, NULL, NULL);
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (15, '消息等级', 'message_level', '0', '吴同', '2024-11-07 14:28:09', NULL, NULL, NULL);
INSERT INTO `system_dict_type` (`dict_id`, `dict_name`, `dict_type`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (16, '布尔值字典', 'sys_boolean', '0', '吴同', '2024-12-03 15:46:14', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_job
-- ----------------------------
DROP TABLE IF EXISTS `system_job`;
CREATE TABLE `system_job` (
  `job_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'DEFAULT' COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `cron_expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT 'cron执行表达式',
  `misfire_policy` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '3' COMMENT '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  `concurrent` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '是否并发执行（0允许 1禁止）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '状态（0正常 1暂停）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注信息',
  PRIMARY KEY (`job_id`,`job_name`,`job_group`) USING BTREE,
  UNIQUE KEY `JOB_ID_362531584181100` (`job_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='定时任务调度表';

-- ----------------------------
-- Records of system_job
-- ----------------------------
BEGIN;
INSERT INTO `system_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams', '0/10 * * * * ?', '3', '1', '1', '吴同', '2024-12-03 09:27:24', NULL, '2024-12-04 09:40:50', NULL);
INSERT INTO `system_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')', '0/15 * * * * ?', '3', '1', '1', '吴同', '2024-12-03 09:27:24', NULL, NULL, NULL);
INSERT INTO `system_job` (`job_id`, `job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)', '0/20 * * * * ?', '3', '1', '1', '吴同', '2024-12-03 09:27:24', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_job_log
-- ----------------------------
DROP TABLE IF EXISTS `system_job_log`;
CREATE TABLE `system_job_log` (
  `job_log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务日志ID',
  `job_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `job_group` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务组名',
  `invoke_target` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用目标字符串',
  `job_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '日志信息',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '执行状态（0正常 1失败）',
  `exception_info` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '异常信息',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`job_log_id`) USING BTREE,
  UNIQUE KEY `JOB_LOG_ID_362532255904100` (`job_log_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='定时任务调度日志表';

-- ----------------------------
-- Records of system_job_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for system_logininfor
-- ----------------------------
DROP TABLE IF EXISTS `system_logininfor`;
CREATE TABLE `system_logininfor` (
  `info_id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户账号',
  `ipaddr` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作系统',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '提示消息',
  `login_time` timestamp NULL DEFAULT NULL COMMENT '访问时间',
  PRIMARY KEY (`info_id`) USING BTREE,
  UNIQUE KEY `INFO_ID_362532582426400` (`info_id`) USING BTREE,
  KEY `idx_sys_logininfor_lt` (`login_time`) USING BTREE,
  KEY `idx_sys_logininfor_s` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=272 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='系统访问记录';

-- ----------------------------
-- Records of system_logininfor
-- ----------------------------
BEGIN;
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (259, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:34:10');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (260, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '退出成功', '2025-10-09 10:37:49');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (261, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:37:55');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (262, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '退出成功', '2025-10-09 10:38:13');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (263, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:38:16');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (264, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '退出成功', '2025-10-09 10:38:43');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (265, 'admin', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:38:48');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (266, 'admin', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '退出成功', '2025-10-09 10:41:15');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (267, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:41:18');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (268, 'qData', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '退出成功', '2025-10-09 10:42:34');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (269, 'admin', '172.28.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-09 10:42:39');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (270, 'qData', '127.0.0.1', '内网IP', 'Chrome 14', 'Windows 10', '1', '用户不存在/密码错误', '2025-10-27 15:41:07');
INSERT INTO `system_logininfor` (`info_id`, `user_name`, `ipaddr`, `login_location`, `browser`, `os`, `status`, `msg`, `login_time`) VALUES (271, 'admin', '127.0.0.1', '内网IP', 'Chrome 14', 'Windows 10', '0', '登录成功', '2025-10-27 15:41:11');
COMMIT;

-- ----------------------------
-- Table structure for system_menu
-- ----------------------------
DROP TABLE IF EXISTS `system_menu`;
CREATE TABLE `system_menu` (
  `menu_id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '路由地址',
  `component` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '组件路径',
  `query` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由参数',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `route_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '路由名称',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '#' COMMENT '菜单图标',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`) USING BTREE,
  UNIQUE KEY `MENU_ID_362533204409300` (`menu_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2687 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='菜单权限表';

-- ----------------------------
-- Records of system_menu
-- ----------------------------
BEGIN;
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, '系统管理', 0, 998, 'sys', NULL, NULL, 1, 0, NULL, 'M', '0', '0', NULL, 'settings-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:01:16', '系统管理目录');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, '系统监控', 0, 999, 'monitor', NULL, NULL, 1, 0, NULL, 'M', '0', '0', NULL, 'slideshow-3-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-05-30 16:16:33', '系统监控目录');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, '系统工具', 0, 1000, 'tool', NULL, NULL, 1, 0, NULL, 'M', '0', '0', NULL, 'briefcase-2-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-05-30 16:17:04', '系统工具目录');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (100, '用户管理', 1, 1, 'user', 'sys/system/user/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:user:list', 'user-3-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:00', '用户管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (101, '角色管理', 1, 2, 'role', 'sys/system/role/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:role:list', 'group-3-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:10', '角色管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (102, '菜单管理', 1, 3, 'menu', 'sys/system/menu/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:menu:list', 'list-view', 'qData', '2024-05-06 06:12:17', 'admin', '2025-05-30 16:20:45', '菜单管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (103, '部门管理', 1, 4, 'dept', 'sys/system/dept/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:dept:list', 'contacts-book-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:14', '部门管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (104, '岗位管理', 1, 5, 'post', 'sys/system/post/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:post:list', 'node-tree', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:19', '岗位管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (105, '字典管理', 1, 6, 'dict', 'sys/system/dict/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:dict:list', 'book-2-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:01:54', '字典管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (106, '参数设置', 1, 7, 'config', 'sys/system/config/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:config:list', 'exchange-funds-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:23', '参数设置菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (107, '通知公告', 1, 8, 'notice', 'sys/system/notice/index', NULL, 1, 0, NULL, 'C', '0', '0', 'system:notice:list', 'megaphone-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:28', '通知公告菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (108, '日志管理', 1, 9, 'log', NULL, NULL, 1, 0, NULL, 'M', '0', '0', NULL, 'book-marked-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-05-30 16:24:05', '日志管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (109, '在线用户', 2, 1, 'online', 'sys/monitor/online/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:online:list', 'user-3-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:50', '在线用户菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (110, '定时任务', 2, 2, 'job', 'sys/monitor/job/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:job:list', 'calendar-schedule-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:54', '定时任务菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (112, '服务监控', 2, 4, 'server', 'sys/monitor/server/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:server:list', 'slideshow-4-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:58', '服务监控菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (113, '缓存监控', 2, 5, 'cache', 'sys/monitor/cache/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:cache:list', 'slideshow-2-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:04:03', '缓存监控菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (114, '缓存列表', 2, 6, 'cacheList', 'sys/monitor/cache/list', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:cache:list', 'file-list-2-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:04:11', '缓存列表菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (116, '代码生成', 3, 2, 'gen', 'sys/tool/gen/index', NULL, 1, 0, NULL, 'C', '0', '0', 'tool:gen:list', 'code-box-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:04:20', '代码生成菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (117, '系统接口', 3, 3, 'swagger', 'sys/tool/swagger/index', NULL, 1, 0, NULL, 'C', '0', '0', 'tool:swagger:list', 'equalizer-line', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:04:24', '系统接口菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (500, '操作日志', 108, 1, 'operlog', 'sys/monitor/operlog/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:operlog:list', '#', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:38', '操作日志菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (501, '登录日志', 108, 2, 'logininfor', 'sys/monitor/logininfor/index', NULL, 1, 0, NULL, 'C', '0', '0', 'monitor:logininfor:list', '#', 'qData', '2024-05-06 06:12:17', 'admin', '2025-09-30 10:03:43', '登录日志菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1000, '用户查询', 100, 1, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1001, '用户新增', 100, 2, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1002, '用户修改', 100, 3, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1003, '用户删除', 100, 4, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1004, '用户导出', 100, 5, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1005, '用户导入', 100, 6, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:import', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1006, '重置密码', 100, 7, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:user:resetPwd', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1007, '角色查询', 101, 1, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:role:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1008, '角色新增', 101, 2, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:role:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1009, '角色修改', 101, 3, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:role:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1010, '角色删除', 101, 4, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:role:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1011, '角色导出', 101, 5, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:role:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1012, '菜单查询', 102, 1, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:menu:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1013, '菜单新增', 102, 2, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:menu:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1014, '菜单修改', 102, 3, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:menu:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1015, '菜单删除', 102, 4, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:menu:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1016, '部门查询', 103, 1, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dept:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1017, '部门新增', 103, 2, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dept:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1018, '部门修改', 103, 3, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dept:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1019, '部门删除', 103, 4, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dept:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1020, '岗位查询', 104, 1, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:post:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1021, '岗位新增', 104, 2, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:post:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1022, '岗位修改', 104, 3, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:post:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1023, '岗位删除', 104, 4, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:post:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1024, '岗位导出', 104, 5, NULL, NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:post:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1025, '字典查询', 105, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dict:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1026, '字典新增', 105, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dict:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1027, '字典修改', 105, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dict:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1028, '字典删除', 105, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dict:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1029, '字典导出', 105, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:dict:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1030, '参数查询', 106, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:config:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1031, '参数新增', 106, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:config:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1032, '参数修改', 106, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:config:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1033, '参数删除', 106, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:config:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1034, '参数导出', 106, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:config:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1035, '公告查询', 107, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:notice:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1036, '公告新增', 107, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:notice:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1037, '公告修改', 107, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:notice:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1038, '公告删除', 107, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:notice:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1039, '操作查询', 500, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:operlog:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1040, '操作删除', 500, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:operlog:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1041, '日志导出', 500, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:operlog:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1042, '登录查询', 501, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:logininfor:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1043, '登录删除', 501, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:logininfor:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1044, '日志导出', 501, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:logininfor:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1045, '账户解锁', 501, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:logininfor:unlock', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1046, '在线查询', 109, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:online:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1047, '批量强退', 109, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1048, '单条强退', 109, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1049, '任务查询', 110, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1050, '任务新增', 110, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:add', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1051, '任务修改', 110, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1052, '任务删除', 110, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1053, '状态修改', 110, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:changeStatus', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1054, '任务导出', 110, 6, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'monitor:job:export', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1055, '生成查询', 116, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:query', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1056, '生成修改', 116, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:edit', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1057, '生成删除', 116, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:remove', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1058, '导入代码', 116, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:import', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1059, '预览代码', 116, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:preview', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1060, '生成代码', 116, 6, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'tool:gen:code', '#', 'qData', '2024-05-06 06:12:17', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2026, '应用管理', 1, 1, 'client', 'sys/auth/client/index', NULL, 1, 0, NULL, 'C', '1', '1', 'auth:client:list', 'user-3-line', 'qData', '2024-08-31 14:33:05', 'admin', '2025-09-30 10:03:05', '应用管理菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2027, '应用管理查询', 2026, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'auth:client:query', '#', 'qData', '2024-08-31 14:33:05', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2028, '应用管理新增', 2026, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'auth:client:add', '#', 'qData', '2024-08-31 14:33:05', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2029, '应用管理修改', 2026, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'auth:client:edit', '#', 'qData', '2024-08-31 14:33:05', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2030, '应用管理删除', 2026, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'auth:client:remove', '#', 'qData', '2024-08-31 14:33:05', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2031, '应用管理导出', 2026, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'auth:client:export', '#', 'qData', '2024-08-31 14:33:05', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2062, '消息', 1, 1, 'message', 'sys/message/message/index', NULL, 1, 0, NULL, 'C', '0', '1', 'system:message:message:list', '#', 'qData', '2024-11-01 09:51:29', 'admin', '2025-09-30 10:02:51', '消息菜单');
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2063, '消息查询', 2062, 1, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:message:message:query', '#', 'qData', '2024-11-01 09:51:29', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2064, '消息新增', 2062, 2, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:message:message:add', '#', 'qData', '2024-11-01 09:51:29', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2065, '消息修改', 2062, 3, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:message:message:edit', '#', 'qData', '2024-11-01 09:51:29', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2066, '消息删除', 2062, 4, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:message:message:remove', '#', 'qData', '2024-11-01 09:51:29', NULL, NULL, NULL);
INSERT INTO `system_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `route_name`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2067, '消息导出', 2062, 5, '#', NULL, NULL, 1, 0, NULL, 'F', '0', '0', 'system:message:message:export', '#', 'qData', '2024-11-01 09:51:29', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_notice
-- ----------------------------
DROP TABLE IF EXISTS `system_notice`;
CREATE TABLE `system_notice` (
  `notice_id` int NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `notice_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `top_flag` smallint NOT NULL DEFAULT '0' COMMENT '是否置顶',
  `alert_flag` smallint NOT NULL DEFAULT '0' COMMENT '是否弹窗',
  `alert_start_time` datetime DEFAULT NULL COMMENT '开始弹窗时间',
  `alert_end_time` datetime DEFAULT NULL COMMENT '结束弹窗时间',
  `notice_content_text` varchar(5120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '内容',
  PRIMARY KEY (`notice_id`) USING BTREE,
  UNIQUE KEY `NOTICE_ID_362533615039300` (`notice_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='通知公告表';

-- ----------------------------
-- Records of system_notice
-- ----------------------------
BEGIN;
INSERT INTO `system_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `top_flag`, `alert_flag`, `alert_start_time`, `alert_end_time`, `notice_content_text`) VALUES (1, 'qData 开源版重磅发布，赋能数据生态共建', '2', NULL, '0', '吴同', '2025-05-27 15:47:20', 'wutong', '2025-09-23 14:24:41', NULL, 0, 0, NULL, NULL, '<p class=\"ql-align-justify\"><strong style=\"color: rgb(60, 60, 67);\">江苏千桐科技有限</strong><span style=\"color: rgb(64, 72, 91);\">公司自主开源核心平台——主数据平台“qData”，致力于构建开放、共享、可持续的技术生态，赋能开发者与合作伙伴，共同推动智能化技术创新。</span></p><p class=\"ql-align-justify\"><img src=\"https://wdcdn.qpic.cn/MTY4ODg1MDA2NTY0ODY1MA_811419__o3wPbAJJK_CpNhV_1758092306?w=1920&amp;h=300&amp;type=image/png\" height=\"94.4896\" width=\"604.733\"></p><p class=\"ql-align-justify\"><strong style=\"color: rgb(60, 60, 67);\">qData 主数据平台</strong><span style=\"color: rgb(60, 60, 67);\">是一套集数据集成、标准管理、资产治理、质量控制、服务开放与智能问数于一体的企业级数据管理平台，致力于帮助企业实现数据资源的统一管理、高效治理与价值释放。我们秉承“</span><strong style=\"color: rgb(60, 60, 67);\">高效、安全、灵活、开放</strong><span style=\"color: rgb(60, 60, 67);\">”的理念，持续引入前沿技术，让数据真正成为企业发展的核心驱动力。</span></p><p class=\"ql-align-justify\"><span style=\"color: rgb(60, 60, 67);\">qData 主数据平台提供 </span><strong style=\"color: rgb(60, 60, 67);\">商业版</strong><span style=\"color: rgb(60, 60, 67);\"> 与 </span><strong style=\"color: rgb(60, 60, 67);\">开源版</strong><span style=\"color: rgb(60, 60, 67);\"> 两种形态，满足不同规模与场景下的用户需求。</span></p><p>●&nbsp;<strong style=\"color: rgb(60, 60, 67);\">商业版</strong><span style=\"color: rgb(60, 60, 67);\">：功能完整，覆盖系统管理、数据接入、建模、安全与服务的全链路，经过严格打磨，适用于大规模、复杂场景，提供专属服务与智能化能力，助力企业稳定高效运行。</span></p><p>●&nbsp;<strong style=\"color: rgb(60, 60, 67);\">开源版</strong><span style=\"color: rgb(60, 60, 67);\">：聚焦核心功能，包含基础管理、常见数据源接入、建模与质量校验，轻量易用，门槛低，适合中小企业或个人开发者快速实践与探索。</span></p><blockquote><strong style=\"color: rgb(111, 66, 193);\">鼓励一下 🚀</strong></blockquote><blockquote><span style=\"color: rgb(60, 60, 67);\">这是一个 </span><strong style=\"color: rgb(60, 60, 67);\">有用、持续更新、具备真实生产价值</strong><span style=\"color: rgb(60, 60, 67);\"> 的项目。</span></blockquote><blockquote><span style=\"color: rgb(60, 60, 67);\">如果 qData 对你有帮助，请给我们点个 </span><strong style=\"color: rgb(60, 60, 67);\">Star</strong><span style=\"color: rgb(60, 60, 67);\"> ??，这将是我们持续更新的最大动力！</span></blockquote><blockquote><span style=\"color: rgb(60, 60, 67);\">👉 </span><a href=\"https://gitee.com/qiantongtech/qData\" rel=\"noopener noreferrer\" target=\"_blank\" style=\"color: rgb(30, 111, 255);\">[在 Gitee 上 Star 项目]</a><span style=\"color: rgb(60, 60, 67);\"> |👉</span><a href=\"https://github.com/qiantongtech/qData\" rel=\"noopener noreferrer\" target=\"_blank\" style=\"color: rgb(30, 111, 255);\">[在 GitHub 上 Star 项目]</a></blockquote>');
INSERT INTO `system_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `top_flag`, `alert_flag`, `alert_start_time`, `alert_end_time`, `notice_content_text`) VALUES (2, 'qData  商业版 v1.1.7 功能增强与升级', '2', NULL, '0', '吴同', '2025-08-01 14:56:47', 'wutong', '2025-09-23 14:23:29', NULL, 0, 0, NULL, NULL, '<p>版本类型：功能更新 + 问题修复</p><h2>🆕 新功能</h2><h3>1、标准数据元</h3><ul><li>数据元类型字段支持绑定稽查/清洗规则，逻辑与质量任务、转化组件保持一致</li><li>配置质量任务或转化组时，自动带入已绑定规则，支持二次修改</li></ul><h3>2、图谱展示</h3><ul><li>以图谱形式展示表、库、任务间依赖关系，支持缩放、拖拽</li><li>资产详情页展示当前表的上下游血缘链路</li></ul><h3>3、资产详情页优化</h3><ul><li>顶部展示基本信息，其余字段统一归入【概览】Tab，完整呈现所有字段</li><li>已存在血缘的数据源，资产视图可直接查看上下游关系（预留扩展接口）</li></ul><h3>4、数据质量</h3><ul><li>资产详情页新增【数据质量】Tab，展示任务、评分、趋势图</li><li>无任务时支持新建稽查任务（自动带入标准规则），可修改调度周期与规则</li><li>质量评分展示在资产列表中</li></ul><h3>5、数据清洗</h3><ul><li>资产详情页新增【数据清洗】Tab，展示最新清洗报告</li><li>支持新建清洗任务（自动带入规则），支持双执行引擎，可调整调度与规则</li></ul><h3>6、ETL 转化组件</h3><ul><li>完成列拆分多行、剪切字符串、设常量、双流 Join 等核心组件开发</li><li>优化值映射、字段选择、字符串操作等已有组件逻辑</li><li>表输入组件仅展示“我申请到的资产”</li></ul><h3>7、转化组组件</h3><ul><li>交互逻辑重构，与质量任务规则添加方式保持一致</li><li>支持10类清洗规则实现，兼容主流数据库类型</li></ul><h2>🐛 问题修复</h2><h3>1、日志问题</h3><ul><li>解决 DS 与后端必须部署在一起的问题，支持多节点日志合并</li></ul><h3>2、数据源管理</h3><ul><li>优化无权限拉取表结构时的报错提示，隐藏代码细节</li></ul><h3>3、逻辑模型</h3><ul><li>修复物化失败无提示问题</li><li>主键字段自动设为必填且不可修改</li><li>修复人大金仓字段长度不一致问题</li></ul><h3>4、部署流程</h3><ul><li>每次更新打包带版本号的前后端镜像</li><li>提供 docker-compose 一键启动文件（不含调度器）</li><li>补充调度器、Spark 原生部署文档</li><li>增加部署环境确认单与初始化测试脚本</li></ul>');
INSERT INTO `system_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `top_flag`, `alert_flag`, `alert_start_time`, `alert_end_time`, `notice_content_text`) VALUES (3, '直播预告｜与qData团队开源面对面', '1', NULL, '0', '吴同', '2025-07-13 15:47:20', 'wutong', '2025-09-23 14:28:33', NULL, 0, 0, NULL, NULL, '<h1>📢 直播预告｜qData开源面对面：从0到1构建你的主数据平台！</h1><p>qData团队首次与大家在线见面！一起见证 qData 的技术演进与社区成长！</p><p><strong>📌 精彩看点：</strong></p><p>1、qData 产品全景能力解析</p><p>2、一线工程师手把手 Demo 演示</p><p>3、社区路线图首次公开</p><p>4、用户互动，交流答疑</p><p><strong>📅 时间：</strong>2025年07月23日（周三）晚 20:00</p><p><strong> 1、地点：</strong>腾讯会议直播</p><p><strong> 2、立即报名：</strong>微信扫二维码预约观看直播！<img src=\"/prod-api//profile/2025/09/18/68cbad5be4b0649f2379b11d.png\"></p><p><br></p>');
INSERT INTO `system_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `top_flag`, `alert_flag`, `alert_start_time`, `alert_end_time`, `notice_content_text`) VALUES (4, 'qData 商业版 v1.1.8 稳定性提升更新', '2', NULL, '0', '吴同', '2025-09-10 14:56:47', 'wutong', '2025-09-23 14:27:50', NULL, 0, 0, NULL, NULL, '<p>版本类型：功能更新 + 问题修复</p><h2>🆕 新功能</h2><h3>1、清洗规则管理</h3><ul><li>新增规则分类管理（支持多级分类）  </li><li>录入规划中的全部规则（编码、名称、描述、场景、示例）  </li></ul><h3>2、数据资产</h3><ul><li>资产详情页优化</li><li>资产血缘集成：可直接查看数据源上下游关系  </li><li>新增质量 / 清洗 Tab，支持任务新增、报告展示、调度周期修改  </li></ul><h3>3、数据研发</h3><ul><li>新增/优化 ETL 转换组件</li><li>转化组组件交互逻辑重构，与质量任务规则交互保持一致  </li><li>表输入组件仅支持“我申请到的资产”  </li></ul><h3>4、数据服务</h3><ul><li>API 发布取消表范围限制，支持选择数据库内所有表  </li></ul><h2>🐛 问题修复</h2><p>1、日志问题：解决 DS 与后端必须部署在一起的问题，多节点日志合并  </p><p>2、数据源管理：拉取表结构无权限时，优化报错信息展示  </p><p>3、逻辑模型：物化失败无提示问题修复；字段主键必填逻辑优化；金仓字段类型长度不一致问题修复  </p>');
INSERT INTO `system_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `top_flag`, `alert_flag`, `alert_start_time`, `alert_end_time`, `notice_content_text`) VALUES (5, 'qData 商业版v1.1.9 新版本上线，功能全面优化', '2', NULL, '0', '吴同', '2025-09-23 00:47:20', 'wutong', '2025-09-23 14:27:18', NULL, 0, 0, NULL, NULL, '<p>版本类型：功能更新 + 问题修复</p><h2>🆕 新功能</h2><h3>1、稽查规则管理</h3><ul><li>按国标重建分类：完整性、唯一性、有效性、一致性、时效性</li><li>清除预置规则，按【稽查规则2.0】标准录入元数据</li><li>质量任务支持自动绑定稽查规则，可二次修改</li></ul><h3>2、清洗规则管理</h3><ul><li>新增两级分类体系，与【清洗规则2.0】对齐</li><li>完成全部清洗规则录入，支持数据元类型绑定</li></ul><h3>3、数据血缘（库表级）</h3><ul><li>自动解析 ETL 及 SQL 任务（Spark/Hive/Flink）的数据流动关系</li><li>支持列级血缘与多语句解析，资产详情页展示上下游链路</li><li>新增图谱化依赖视图，支持拖拽、缩放查看</li></ul><h3>4、资产清洗能力</h3><ul><li>资产详情页新增“数据清洗”Tab，展示最新清洗报告</li><li>支持一键创建清洗任务（自动带入标准规则），可修改调度周期</li></ul><h2>🐛 问题修复</h2><p><strong>1、部署架构</strong>：解决 DS 与 JAR 包必须部署在一起的问题，支持分布式部署</p><p><strong>2、日志系统</strong>：统一多节点日志采集与展示逻辑</p><p><strong>3、系统性能</strong>：修复应用授权缓存导致的响应慢问题</p><p><strong>4、数据标准</strong>：修复规则绑定异常、分类保存失败等问题</p><p><strong>5、数据资产</strong>：修复资产加载慢、质量报告缺失等问题</p><p><strong>6、数据研发</strong>：修复任务复制异常、SQL 解析中断等问题</p><p><strong>7、数据服务</strong>：修复 API 调用超时、参数映射错乱等问题</p>');
COMMIT;

-- ----------------------------
-- Table structure for system_oper_log
-- ----------------------------
DROP TABLE IF EXISTS `system_oper_log`;
CREATE TABLE `system_oper_log` (
  `oper_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '模块标题',
  `business_type` int DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求方式',
  `operator_type` int DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '操作地点',
  `oper_param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '请求参数',
  `json_result` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '返回参数',
  `status` int DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '错误消息',
  `oper_time` timestamp NULL DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint DEFAULT '0' COMMENT '消耗时间',
  UNIQUE KEY `OPER_ID_362534240648700` (`oper_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=922 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='操作日志记录';

-- ----------------------------
-- Records of system_oper_log
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for system_post
-- ----------------------------
DROP TABLE IF EXISTS `system_post`;
CREATE TABLE `system_post` (
  `post_id` bigint NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `post_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位编码',
  `post_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '岗位名称',
  `post_sort` int NOT NULL COMMENT '显示顺序',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`) USING BTREE,
  UNIQUE KEY `POST_ID_362534293863900` (`post_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='岗位信息表';

-- ----------------------------
-- Records of system_post
-- ----------------------------
BEGIN;
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1, 'CEO', '总经理', 0, '0', 'admin', '2025-09-18 14:06:34', NULL, NULL, NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (2, 'DM', '部门经理', 1, '0', 'admin', '2025-09-18 14:06:47', NULL, NULL, NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (3, 'AR', '架构师', 2, '0', 'admin', '2025-09-18 14:07:01', NULL, NULL, NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (4, 'SE', '软件工程师', 3, '0', 'admin', '2025-09-18 14:07:21', 'admin', '2025-09-18 14:08:06', NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (5, 'PM', '产品经理', 4, '0', 'admin', '2025-09-18 14:07:41', 'admin', '2025-09-18 14:08:10', NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (6, 'QA', '测试工程师', 5, '0', 'admin', '2025-09-18 14:07:56', NULL, NULL, NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (7, 'OP', '运维工程师', 6, '0', 'admin', '2025-09-18 14:08:23', NULL, NULL, NULL);
INSERT INTO `system_post` (`post_id`, `post_code`, `post_name`, `post_sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (8, 'CM', '客户经理', 7, '0', 'admin', '2025-09-18 14:08:36', NULL, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_role
-- ----------------------------
DROP TABLE IF EXISTS `system_role`;
CREATE TABLE `system_role` (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  `menu_check_strictly` tinyint DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint DEFAULT '1' COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `project_id` bigint DEFAULT NULL COMMENT '项目空间ID',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE KEY `ROLE_ID_362534717975500` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色信息表';

-- ----------------------------
-- Records of system_role
-- ----------------------------
BEGIN;
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (1, '超级管理员', 'admin', 0, '1', 1, 1, '0', '0', 'system', '2025-09-18 14:05:00', 'system', '2025-09-18 14:05:00', '系统最高权限角色，拥有平台所有权限（用户/组织/权限/数据访问等）。仅用于系统初始化与运维。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (2, '系统管理员', 'system', 1, '1', 1, 1, '0', '0', 'admin', '2025-09-18 13:47:48', 'admin', '2025-09-30 16:24:06', '系统最高业务管理角色，负责用户、部门、岗位、角色等组织权限配置。用于日常系统使用管理。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (3, '数据研发员', 'developer', 2, '1', 1, 1, '0', '0', 'admin', '2025-09-18 14:02:42', 'wutong', '2025-09-23 17:30:58', '负责数据加工、任务开发与调度、数据集成，构建与维护核心数据链路。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (4, '数据分析师', 'analyst', 3, '1', 1, 1, '0', '0', 'admin', '2025-09-18 14:03:13', 'wutong', '2025-09-23 17:29:12', '基于主数据平台开展查询分析与报表生成，权限限定于授权业务数据范围。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (5, '数据服务管理员', 'service', 4, '1', 1, 1, '0', '0', 'admin', '2025-09-18 14:03:32', 'wutong', '2025-09-23 17:30:39', '负责接口注册、服务发布、调用监控及流量管控，保障数据服务稳定可靠。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (6, '客户经理', 'sales', 5, '1', 1, 1, '0', '0', 'admin', '2025-09-18 14:03:57', 'wutong', '2025-09-23 17:29:54', '拥有全部业务操作权限，负责用户全生命周期管理及业务权限配置。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (7, '体验用户', 'guest', 6, '1', 1, 1, '0', '0', 'admin', '2025-09-18 14:04:23', 'admin', '2025-10-09 10:41:12', '具备全业务功能试用权限，用于系统能力体验，不包含系统管理权限。', 0);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (10, '项目管理员', 'gly', 0, '1', 1, 1, '0', '0', 'admin', '2025-09-18 16:34:45', 'admin', '2025-09-30 11:19:58', '建议项目负责人员勾选', 1);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (11, '数据开发人员', 'sjkf', 2, '1', 1, 1, '0', '0', 'admin', '2025-09-19 09:37:53', NULL, NULL, '建议从事数据集成、数据清洗等工作的人员勾选', 1);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (12, '质量评估人员', 'zlpg', 3, '1', 1, 1, '0', '0', 'admin', '2025-09-19 09:38:17', 'admin', '2025-09-19 09:38:24', '建议负责数据质量审核、质量评估的人员勾选\n', 1);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (13, '数据分析人员', 'sjfx', 4, '1', 1, 1, '0', '0', 'admin', '2025-09-19 09:38:42', NULL, NULL, '建议从事数据统计分析、指标设计的人员勾选\n', 1);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (14, '运维人员', 'yw', 5, '1', 1, 1, '0', '0', 'admin', '2025-09-19 09:39:01', NULL, NULL, '建议从事运行维护、监控运维等工作的人员勾选\n', 1);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (15, '项目管理员', 'jcss-admin', 0, '1', 1, 1, '0', '0', 'admin', '2025-09-23 10:37:02', 'admin', '2025-09-23 10:37:25', '建议项目负责人员勾选', 2);
INSERT INTO `system_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `project_id`) VALUES (16, '项目管理员', 'yesj-admin', 0, '1', 1, 1, '0', '0', 'admin', '2025-09-23 10:37:48', NULL, NULL, '建议项目负责人员勾选', 3);
COMMIT;

-- ----------------------------
-- Table structure for system_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `system_role_dept`;
CREATE TABLE `system_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色和部门关联表';

-- ----------------------------
-- Records of system_role_dept
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for system_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `system_role_menu`;
CREATE TABLE `system_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  `project_id` bigint DEFAULT '0' COMMENT '项目id',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='角色和菜单关联表';

-- ----------------------------
-- Records of system_role_menu
-- ----------------------------
BEGIN;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 3, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 100, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 101, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 102, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 103, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 104, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 105, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 106, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 107, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 108, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 109, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 110, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 112, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 113, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 114, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 116, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 117, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 501, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1000, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1001, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1002, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1003, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1004, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1005, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1006, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1007, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1008, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1009, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1010, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1011, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1012, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1013, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1014, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1015, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1016, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1017, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1018, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1019, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1020, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1021, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1022, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1023, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1024, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1025, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1026, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1027, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1028, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1029, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1030, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1031, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1032, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1033, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1034, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1035, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1036, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1037, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1038, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1039, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1040, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1041, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1042, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1043, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1044, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1045, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1046, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1047, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1048, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1049, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1050, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1051, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1052, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1053, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1054, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1055, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1056, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1057, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1058, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1059, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 1060, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2062, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2063, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2064, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2065, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2066, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2067, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2562, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2563, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2564, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2565, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2566, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2567, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2568, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2569, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2570, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2571, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2572, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2573, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2574, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2575, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2674, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2675, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2676, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2677, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2678, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2679, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2683, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2684, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2685, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (2, 2686, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 107, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 1035, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2285, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2286, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2287, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2288, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2289, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2290, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2291, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2405, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2406, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2407, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2408, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2409, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2410, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2411, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2412, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2413, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2414, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2415, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2416, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2418, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2419, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2420, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2421, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2422, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2423, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2424, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2450, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2451, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2452, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2453, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2454, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2455, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2456, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2605, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2607, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2608, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2609, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2610, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2611, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2612, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2613, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2614, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2615, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2616, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2617, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2618, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2619, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2620, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2621, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2622, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2623, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2624, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2625, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2626, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2627, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2628, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2629, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2630, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2631, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2632, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2633, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2634, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2635, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2636, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2637, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2638, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2639, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2641, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2642, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2643, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2644, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2645, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2646, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2647, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2648, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2656, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2657, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2658, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2659, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2660, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2661, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2662, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2663, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2664, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2665, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2666, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2667, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (3, 2668, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2405, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2406, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2407, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2408, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2409, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2410, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2411, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2412, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2413, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2414, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2415, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2416, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2418, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2419, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2420, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2421, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2422, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2423, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2424, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2450, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2451, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2452, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2453, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2454, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2455, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2456, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2605, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2621, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2622, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2637, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2638, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2639, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2641, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2642, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2643, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 2644, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (4, 999999, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 107, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 1035, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 2600, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 999999, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (5, 1000025, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 100, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 107, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1000, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1001, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1002, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1003, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1004, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1005, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1006, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1035, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1036, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1037, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1038, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2285, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2286, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2287, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2288, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2289, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2290, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2291, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2405, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2406, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2407, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2408, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2409, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2410, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2411, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2412, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2413, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2414, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2415, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2416, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2418, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2419, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2420, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2421, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2422, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2423, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2424, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2450, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2451, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2452, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2453, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2454, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2455, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2456, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2600, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2605, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2607, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2608, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2609, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2610, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2611, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2612, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2613, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2614, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2615, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2616, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2617, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2618, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2619, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2620, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2621, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2622, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2623, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2624, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2625, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2626, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2627, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2628, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2629, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2630, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2631, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2632, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2633, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2634, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2635, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2636, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2637, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2638, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2639, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2641, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2642, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2643, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2644, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2645, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2646, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2647, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2648, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2656, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2657, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2658, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2659, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2660, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2661, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2662, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2663, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2664, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2665, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2666, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2667, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 2668, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 999999, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (6, 1000025, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 107, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 108, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 501, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1035, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1039, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1040, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1041, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1042, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1043, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1044, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 1045, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2062, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2063, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2562, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2563, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2564, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2565, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2566, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2567, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2568, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2569, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2570, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2571, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2572, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2573, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2574, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2575, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2674, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2675, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2676, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2677, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2678, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2679, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2683, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2684, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2685, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (7, 2686, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 100, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1000, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1001, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1002, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1003, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1004, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1005, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1006, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2285, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2286, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2287, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2288, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2289, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2290, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2291, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2405, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2406, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2407, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2408, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2409, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2410, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2411, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2412, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2413, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2414, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2415, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2416, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2418, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2419, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2420, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2421, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2422, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2423, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2424, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2450, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2451, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2452, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2453, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2454, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2455, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2456, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2600, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2605, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2607, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2608, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2609, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2610, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2611, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2612, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2613, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2614, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2615, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2616, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2617, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2618, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2619, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2620, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2621, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2622, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2623, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2624, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2625, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2626, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2627, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2628, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2629, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2630, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2631, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2632, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2633, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2634, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2635, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2636, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2637, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2638, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2639, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2641, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2642, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2643, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2644, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2645, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2646, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2647, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2648, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2656, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2657, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2658, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2659, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2660, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2661, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2662, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2663, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2664, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2665, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2666, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2667, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 2668, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 999999, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (8, 1000025, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2243, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2244, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2245, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2246, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2247, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2248, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2249, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2257, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2258, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2259, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2260, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2261, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2262, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2263, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2264, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2265, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2266, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2267, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2268, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2269, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2270, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2271, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2272, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2273, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2274, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2275, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2276, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2277, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2278, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2279, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2280, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2281, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2282, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2283, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2284, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2285, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2286, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2287, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2288, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2289, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2290, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2291, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2292, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2293, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2294, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2295, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2296, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2297, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2298, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2299, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2300, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2301, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2302, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2303, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2304, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2305, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2306, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2307, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2308, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2309, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2310, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2311, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2312, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2313, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2314, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2315, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2316, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2317, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2318, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2319, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2320, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2321, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2322, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2323, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2324, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2325, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2326, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2327, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2328, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2329, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2330, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2331, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2332, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2333, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2334, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2335, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2336, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2337, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2338, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2339, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2340, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2341, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2342, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2343, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2344, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2345, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2346, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2347, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2348, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2349, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2350, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2351, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2352, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2354, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2355, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2356, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2357, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2358, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2359, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2360, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2368, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2369, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2370, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2371, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2372, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2373, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2374, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2375, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2376, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2377, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2378, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2379, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2380, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2381, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2382, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2383, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2384, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2385, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2386, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2387, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2388, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2389, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2390, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2391, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2392, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2393, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2394, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2395, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2396, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2405, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2406, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2407, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2408, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2409, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2410, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2411, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2412, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2413, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2414, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2415, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2416, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2418, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2419, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2420, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2421, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2422, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2423, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2424, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2427, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2428, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2429, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2430, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2431, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2432, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2433, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2434, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2436, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2437, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2438, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2439, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2440, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2441, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2442, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2450, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2451, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2452, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2453, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2454, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2455, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2456, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2457, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2458, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2459, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2460, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2461, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2462, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2463, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2494, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2495, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2496, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2497, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2498, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2499, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2500, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2530, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2531, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2532, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2533, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2534, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2535, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2536, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2554, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2555, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2556, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2557, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2558, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2559, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2560, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2561, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2576, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2577, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2578, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2600, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2605, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2607, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2608, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2609, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2610, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2611, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2612, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2613, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2614, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2615, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2616, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2617, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2618, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2619, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2620, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2621, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2622, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2623, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2624, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2625, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2626, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2627, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2628, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2629, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2630, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2631, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2632, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2633, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2634, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2635, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2636, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2637, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2638, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2639, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2641, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2642, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2643, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2644, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2645, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2646, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2647, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2648, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2656, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2657, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2658, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2659, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2660, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2661, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2662, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2663, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2664, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2665, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2666, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2667, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 2668, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 999999, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (9, 1000025, 0);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2397, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2398, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2399, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2400, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2401, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2402, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2403, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2404, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2426, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2444, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2445, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2446, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2447, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2448, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2449, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2464, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2465, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2466, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2467, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2468, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2469, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2470, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2471, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2472, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2473, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2474, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2475, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2476, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2477, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2487, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2488, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2489, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2490, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2491, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2492, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2493, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2501, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2502, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2503, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2504, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2505, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2506, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2507, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2510, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2511, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2513, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2519, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2520, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2521, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2522, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2523, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2524, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2525, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2526, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2527, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2528, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2529, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2537, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2538, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2539, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2540, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2541, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2542, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2543, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2544, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2545, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2546, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2547, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2548, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2549, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2552, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2673, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2680, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (10, 2681, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2397, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2426, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2464, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2465, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2466, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2467, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2468, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2469, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2470, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2471, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2472, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2473, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2474, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2475, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2476, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2477, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2480, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2481, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2482, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2483, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2484, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2485, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2486, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2487, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2488, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2489, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2490, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2491, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2492, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2493, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2501, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2502, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2503, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2504, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2505, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2506, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2507, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2508, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2510, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2511, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2512, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2513, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2514, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2515, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2516, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2517, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2518, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2519, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2520, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2521, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2522, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2523, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2524, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2525, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2526, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2527, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2528, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2529, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2537, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2538, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2539, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2540, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2541, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2542, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2543, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2544, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2545, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2546, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2547, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2548, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2549, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2550, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2551, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2552, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2553, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (11, 2673, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2397, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2426, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2464, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2465, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2466, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2467, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2468, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2469, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2470, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2471, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2472, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2473, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2474, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2475, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2476, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2477, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2480, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2481, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2482, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2483, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2484, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2485, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2486, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2487, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2488, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2489, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2490, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2491, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2492, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2493, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2501, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2502, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2503, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2504, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2505, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2506, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2507, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2508, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2510, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2511, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2512, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2513, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2514, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2515, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2516, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2517, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2518, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2519, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2520, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2521, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2522, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2523, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2524, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2525, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2526, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2527, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2528, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2529, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2537, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2538, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2539, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2540, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2541, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2542, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2543, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2544, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2545, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2546, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2547, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2548, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2549, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2550, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2551, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2552, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2553, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (12, 2673, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2397, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2426, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2464, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2465, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2466, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2467, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2468, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2469, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2470, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2471, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2472, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2473, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2474, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2475, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2476, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2477, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2480, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2481, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2482, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2483, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2484, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2485, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2486, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2487, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2488, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2489, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2490, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2491, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2492, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2493, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2501, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2502, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2503, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2504, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2505, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2506, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2507, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2508, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2510, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2511, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2512, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2513, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2514, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2515, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2516, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2517, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2518, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2519, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2520, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2521, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2522, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2523, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2524, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2525, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2526, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2527, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2528, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2529, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2537, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2538, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2539, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2540, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2541, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2542, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2543, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2544, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2545, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2546, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2547, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2548, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2549, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2550, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2551, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2552, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2553, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (13, 2673, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2397, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2426, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2464, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2465, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2466, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2467, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2468, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2469, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2470, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2471, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2472, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2473, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2474, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2475, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2476, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2477, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2480, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2481, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2482, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2483, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2484, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2485, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2486, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2487, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2488, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2489, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2490, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2491, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2492, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2493, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2501, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2502, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2503, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2504, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2505, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2506, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2507, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2508, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2510, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2511, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2512, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2513, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2514, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2515, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2516, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2517, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2518, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2519, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2520, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2521, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2522, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2523, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2524, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2525, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2526, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2527, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2528, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2529, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2537, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2538, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2539, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2540, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2541, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2542, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2543, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2544, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2545, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2546, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2547, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2548, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2549, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2550, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2551, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2552, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2553, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (14, 2673, 1);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2397, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2398, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2399, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2400, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2401, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2402, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2403, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2404, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2426, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2444, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2445, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2446, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2447, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2448, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2449, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2464, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2465, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2466, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2467, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2468, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2469, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2470, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2471, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2472, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2473, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2474, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2475, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2476, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2477, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2480, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2481, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2482, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2483, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2484, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2485, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2486, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2487, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2488, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2489, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2490, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2491, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2492, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2493, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2501, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2502, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2503, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2504, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2505, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2506, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2507, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2508, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2510, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2511, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2512, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2513, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2514, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2515, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2516, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2517, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2518, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2519, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2520, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2521, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2522, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2523, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2524, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2525, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2526, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2527, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2528, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2529, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2537, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2538, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2539, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2540, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2541, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2542, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2543, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2544, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2545, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2546, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2547, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2548, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2549, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2550, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2551, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2552, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2553, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (15, 2673, 2);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2397, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2398, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2399, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2400, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2401, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2402, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2403, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2404, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2426, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2444, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2445, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2446, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2447, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2448, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2449, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2464, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2465, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2466, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2467, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2468, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2469, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2470, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2471, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2472, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2473, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2474, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2475, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2476, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2477, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2480, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2481, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2482, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2483, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2484, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2485, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2486, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2487, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2488, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2489, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2490, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2491, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2492, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2493, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2501, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2502, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2503, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2504, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2505, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2506, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2507, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2508, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2510, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2511, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2512, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2513, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2514, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2515, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2516, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2517, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2518, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2519, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2520, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2521, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2522, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2523, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2524, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2525, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2526, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2527, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2528, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2529, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2537, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2538, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2539, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2540, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2541, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2542, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2543, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2544, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2545, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2546, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2547, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2548, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2549, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2550, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2551, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2552, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2553, 3);
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `project_id`) VALUES (16, 2673, 3);
COMMIT;

-- ----------------------------
-- Table structure for system_user
-- ----------------------------
DROP TABLE IF EXISTS `system_user`;
CREATE TABLE `system_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '最后登录IP',
  `login_date` timestamp NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '创建者',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '' COMMENT '更新者',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `auth_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '认证平台id',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE KEY `USER_ID_362536017164800` (`user_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户信息表';

-- ----------------------------
-- Records of system_user
-- ----------------------------
BEGIN;
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (1, NULL, 'admin', '超级管理员', '00', 'support@qiantong.tech', '15888888888', '1', '/profile/avatar/2025/09/18/68cb6865e4b0b6ef59810506.png', '$2a$10$LOgcmTijb/krLwReokOD5.24moLGwspqbfBzXMKykmJse/ETUHQwS', '0', '0', '127.0.0.1', '2025-10-27 15:41:09', NULL, '2025-09-18 09:55:44', NULL, '2025-10-27 15:41:11', '管理员', NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (2, 857, 'qData', '吴同', '00', NULL, '15365128327', '0', '/profile/avatar/2025/09/26/68d62ea8e4b02460a489adef.png', '$2a$10$yzCKAV8QlDNMCdYe6fElbOIpwRA8vyQ7hdFuw4bzD4bq749gruVzO', '0', '0', '172.28.0.1', '2025-10-09 10:41:18', 'admin', '2025-09-18 14:33:40', 'qData', '2025-10-09 10:41:18', NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (3, 858, 'qiutonghui', '邱同辉', '00', NULL, '17720259142', '0', NULL, '$2a$10$0LxoCTkdoDNrW1kikuxmzeO0l7os8lOi.bVapKdHgMsSouDgJaLOm', '0', '0', NULL, NULL, 'wutong', '2025-09-18 14:39:27', NULL, NULL, NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (4, 857, 'wangming', '王明', '00', NULL, '19951942682', '0', NULL, '$2a$10$Q2wIQyKJBWLS.nBuUmNf5.RvxUoJJZp7mqOYINIVligFfqyNLL3AO', '0', '0', NULL, NULL, 'wutong', '2025-09-18 14:40:12', NULL, NULL, NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (5, 857, 'pengxinglang', '彭星浪', '00', NULL, '18944002920', '0', NULL, '$2a$10$imCUc6P1NxLP6nWv1cNyMOjVauZPVqn1j74Qvwu07bNnJGf3PKTUO', '0', '0', '172.28.0.1', '2025-09-22 16:29:20', 'wutong', '2025-09-18 14:40:50', NULL, '2025-09-22 16:29:20', NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (6, 858, 'liruisa', '李瑞洒', '00', NULL, '13811581064', '1', NULL, '$2a$10$7rkp3G9I.rNW./bVdQ9Hl.HsaxOzM/oyIH5B88vA9bNeigY2I15VK', '0', '0', '192.168.0.155', '2025-09-23 20:24:59', 'wutong', '2025-09-18 14:41:47', 'wutong', '2025-09-23 20:24:58', NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (7, 858, 'zhengmengke', '郑梦可', '00', NULL, '19517228692', '1', NULL, '$2a$10$S4LT8I8jD2NmBoc6nFwBO.xE6Gh/cIfJUiLY7z2ObHSjtKW2gDrky', '0', '0', NULL, NULL, 'wutong', '2025-09-18 14:42:23', 'wutong', '2025-09-23 17:35:40', NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (8, 864, 'huge', '胡歌', '00', NULL, '17766550633', '0', NULL, '$2a$10$dU8Vnlt7rnvlEu6iJxMpmemRR7aiDmbh9V8Sz8SDpSuAkhdiMa1WS', '0', '0', '172.28.0.1', '2025-09-30 16:00:53', 'wutong', '2025-09-18 14:43:16', 'qData', '2025-10-09 10:34:53', NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (9, 871, 'xiarui', '夏芮', '00', NULL, '15105175865', '1', NULL, '$2a$10$Cs9zISmlsK2ujf2sK1fmXutnvEX6MKggBIKq90ipeKkmvKtFPw1S.', '0', '0', NULL, NULL, 'wutong', '2025-09-18 14:44:30', NULL, NULL, NULL, NULL);
INSERT INTO `system_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `auth_id`) VALUES (10, 862, 'wangdawei', '王大伟', '00', NULL, '18210566666', '0', NULL, '$2a$10$3/UuZeHV8G90IUOpo1NjeucOR20EeenK1Q5VlALqI35M9KmWqeXVK', '0', '0', '192.168.0.112', '2025-09-23 18:05:34', 'wutong', '2025-09-18 14:45:19', 'qData', '2025-10-09 10:34:38', NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for system_user_post
-- ----------------------------
DROP TABLE IF EXISTS `system_user_post`;
CREATE TABLE `system_user_post` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (`user_id`,`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户与岗位关联表';

-- ----------------------------
-- Records of system_user_post
-- ----------------------------
BEGIN;
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (2, 2);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (3, 8);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (4, 3);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (5, 4);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (6, 8);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (7, 5);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (8, 6);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (9, 7);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (10, 7);
INSERT INTO `system_user_post` (`user_id`, `post_id`) VALUES (729, 6);
COMMIT;

-- ----------------------------
-- Table structure for system_user_role
-- ----------------------------
DROP TABLE IF EXISTS `system_user_role`;
CREATE TABLE `system_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='用户和角色关联表';

-- ----------------------------
-- Records of system_user_role
-- ----------------------------
BEGIN;
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (2, 7);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (2, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (2, 15);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (2, 16);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (3, 2);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (3, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (4, 2);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (4, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (5, 2);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (5, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (6, 6);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (6, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (7, 6);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (7, 10);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (8, 3);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (8, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (9, 4);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (9, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (10, 7);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (10, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (10, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (729, 4);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (749, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (749, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (749, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (749, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (819, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (819, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (819, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (819, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (836, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (836, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (836, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (836, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (854, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (854, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (854, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (854, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (861, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (861, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (861, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (861, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (871, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (871, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (871, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (871, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (880, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (880, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (880, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (880, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (882, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (882, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (882, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (882, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (884, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (884, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (884, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (884, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (885, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (885, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (885, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (885, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (886, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (886, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (886, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (886, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (887, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (887, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (887, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (887, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (895, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (895, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (895, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (895, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (901, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (901, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (901, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (901, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (911, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (911, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (911, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (911, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (917, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (917, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (917, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (917, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (918, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (918, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (918, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (918, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (919, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (919, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (919, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (919, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (920, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (920, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (920, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (920, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (921, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (921, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (921, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (921, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (922, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (922, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (922, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (922, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (923, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (923, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (923, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (923, 14);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (924, 11);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (924, 12);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (924, 13);
INSERT INTO `system_user_role` (`user_id`, `role_id`) VALUES (924, 14);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
