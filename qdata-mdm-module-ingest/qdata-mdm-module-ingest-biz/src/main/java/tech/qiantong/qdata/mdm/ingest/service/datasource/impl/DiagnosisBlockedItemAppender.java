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

import tech.qiantong.qdata.mdm.common.database.constants.DbType;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisRespVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 深度诊断前置项失败时，补齐后续未实际执行的检查项。
 */
class DiagnosisBlockedItemAppender {

    private static final List<CheckDefinition> JDBC_CHECKS = Collections.unmodifiableList(Arrays.asList(
            check("CONFIG_VALIDATION", "连接配置"),
            check("DNS_RESOLVE", "网络地址解析"),
            check("TCP_CONNECT", "端口连通性"),
            check("AUTHENTICATION", "账号认证"),
            check("TARGET_RESOURCE_ACCESS", "目标资源访问"),
            check("METADATA_READ", "元数据读取"),
            check("STRUCTURE_READ", "字段结构读取"),
            check("EXTENDED_METADATA", "扩展属性读取"),
            check("TABLE_READ", "库表读取权限"),
            check("ACCOUNT_PERMISSION", "账号角色与权限"),
            check("INSERT_PERMISSION", "INSERT写入"),
            check("BATCH_WRITE_PERMISSION", "批量写入"),
            check("UPDATE_UPSERT_PERMISSION", "UPDATE/UPSERT/MERGE"),
            check("TRUNCATE_DELETE_PERMISSION", "TRUNCATE/DELETE"),
            check("CREATE_PERMISSION", "CREATE建表"),
            check("ALTER_RENAME_DROP_PERMISSION", "ALTER/RENAME/DROP"),
            check("CDC_PERMISSION", "CDC日志与复制权限")
    ));

    private final DiagnosisResultBuilder resultBuilder;

    DiagnosisBlockedItemAppender(DiagnosisResultBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
    }

    void appendRemaining(IngestDatasourceDiagnosisRespVO result, DbType dbType,
                         String failedCheckCode) {
        List<CheckDefinition> definitions = JDBC_CHECKS;
        int failedIndex = indexOf(definitions, failedCheckCode);
        if (failedIndex < 0) {
            return;
        }
        String failedCheckName = definitions.get(failedIndex).name;
        for (int index = failedIndex + 1; index < definitions.size(); index++) {
            CheckDefinition definition = definitions.get(index);
            resultBuilder.addBlockedFailure(result, definition.code, definition.name,
                    failedCheckName + "未通过，未执行" + definition.name,
                    "前置检查未通过，无法验证该项能力",
                    Collections.singletonList("请先处理" + failedCheckName + "问题后重新诊断"));
        }
    }

    private int indexOf(List<CheckDefinition> definitions, String code) {
        for (int index = 0; index < definitions.size(); index++) {
            if (definitions.get(index).code.equals(code)) {
                return index;
            }
        }
        return -1;
    }

    private static CheckDefinition check(String code, String name) {
        return new CheckDefinition(code, name);
    }

    private static final class CheckDefinition {
        private final String code;
        private final String name;

        private CheckDefinition(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
