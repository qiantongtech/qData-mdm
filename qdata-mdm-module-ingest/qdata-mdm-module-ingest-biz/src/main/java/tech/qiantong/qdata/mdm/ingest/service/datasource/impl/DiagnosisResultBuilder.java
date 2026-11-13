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

import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceDiagnosisRespVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 统一构建诊断条目、六类展示分组和统计结果，协议诊断器只关心检查本身。
 */
class DiagnosisResultBuilder {

    static final String PASS = "PASS";
    static final String WARN = "WARN";
    static final String FAIL = "FAIL";

    private static final List<String> GROUP_ORDER = Collections.unmodifiableList(Arrays.asList(
            "BASE_CONNECTION", "TARGET_RESOURCE_ACCESS", "METADATA_CAPABILITY",
            "READ_CAPABILITY", "WRITE_CAPABILITY", "ADVANCED_CAPABILITY"));

    private final DiagnosisErrorClassifier errorClassifier = new DiagnosisErrorClassifier();

    void addPass(IngestDatasourceDiagnosisRespVO result, String code, String name,
                 String message, long startedAt) {
        addItem(result, code, name, PASS, message, null, Collections.emptyList(),
                System.currentTimeMillis() - startedAt);
    }

    void addWarn(IngestDatasourceDiagnosisRespVO result, String code, String name,
                 String message, String impact, List<String> suggestions, long durationMs) {
        addItem(result, code, name, WARN, message, impact, suggestions, durationMs);
    }

    void addFailure(IngestDatasourceDiagnosisRespVO result, String checkCode, String checkName,
                    Throwable exception, String defaultErrorCode, String impact) {
        DiagnosisErrorClassifier.DiagnosisError error =
                errorClassifier.classify(exception, defaultErrorCode);
        addItem(result, checkCode, checkName, FAIL, error.getMessage(), impact,
                errorClassifier.suggestions(error.getCode()), 0L);
        if (result.getPrimaryErrorCode() == null) {
            result.setPrimaryErrorCode(error.getCode());
            result.setRawError(error.getRawError());
        }
    }

    void addItem(IngestDatasourceDiagnosisRespVO result, String code, String name, String status,
                 String message, String impact, List<String> suggestions, Long durationMs) {
        IngestDatasourceDiagnosisRespVO.Item item = new IngestDatasourceDiagnosisRespVO.Item();
        item.setCheckCode(code);
        item.setCheckName(name);
        item.setStatus(status);
        item.setExecuted(true);
        item.setMessage(message);
        item.setImpact(impact);
        item.setSuggestions(suggestions == null ? Collections.emptyList() : suggestions);
        item.setDurationMs(durationMs);
        addToGroup(result, item);
    }

    void addBlockedFailure(IngestDatasourceDiagnosisRespVO result, String code, String name,
                           String message, String impact,
                           List<String> suggestions) {
        IngestDatasourceDiagnosisRespVO.Item item = new IngestDatasourceDiagnosisRespVO.Item();
        item.setCheckCode(code);
        item.setCheckName(name);
        item.setStatus(FAIL);
        item.setExecuted(false);
        item.setMessage(message);
        item.setImpact(impact);
        item.setSuggestions(suggestions == null ? Collections.emptyList() : suggestions);
        item.setDurationMs(0L);
        addToGroup(result, item);
    }

    String sanitize(String value) {
        return errorClassifier.sanitize(value);
    }

    IngestDatasourceDiagnosisRespVO finish(IngestDatasourceDiagnosisRespVO result, long startedAt) {
        boolean failed = items(result).anyMatch(item -> FAIL.equals(item.getStatus()));
        boolean warned = items(result).anyMatch(item -> WARN.equals(item.getStatus()));
        result.setOverallStatus(failed ? FAIL : warned ? WARN : PASS);
        if (failed) {
            IngestDatasourceDiagnosisRespVO.Item failedItem = items(result)
                    .filter(item -> FAIL.equals(item.getStatus())).findFirst().orElse(null);
            String message = failedItem == null ? "数据连接诊断未通过"
                    : "诊断未通过：" + failedItem.getMessage();
            boolean hasBlockedItems = items(result)
                    .anyMatch(item -> Boolean.FALSE.equals(item.getExecuted()));
            result.setMessage(hasBlockedItems ? message + "，后续深度诊断未实际执行" : message);
        } else if ("BASIC".equals(result.getDiagnosisMode())) {
            result.setMessage("基础网络诊断通过，当前类型暂不支持认证和权限诊断");
        } else if (warned) {
            result.setMessage("数据连接可用，但存在需要关注的风险");
        } else {
            result.setMessage("数据连接诊断通过");
        }
        result.setDurationMs(System.currentTimeMillis() - startedAt);
        buildPresentationModel(result);
        return result;
    }

    private void buildPresentationModel(IngestDatasourceDiagnosisRespVO result) {
        for (IngestDatasourceDiagnosisRespVO.Group group : result.getGroups()) {
            group.setStatus(group.getErrorCount() > 0 ? FAIL
                    : group.getWarningCount() > 0 ? WARN
                    : PASS);
        }
        result.setNormalCount(countStatus(result, PASS));
        result.setWarningCount(countStatus(result, WARN));
        result.setErrorCount(countStatus(result, FAIL));
        result.setTotalCount(result.getNormalCount() + result.getWarningCount() + result.getErrorCount());
    }

    private void addToGroup(IngestDatasourceDiagnosisRespVO result,
                            IngestDatasourceDiagnosisRespVO.Item item) {
        String code = groupCode(item.getCheckCode());
        IngestDatasourceDiagnosisRespVO.Group group = result.getGroups().stream()
                .filter(existing -> code.equals(existing.getGroupCode()))
                .findFirst().orElse(null);
        if (group == null) {
            group = newGroup(code);
            int targetOrder = GROUP_ORDER.indexOf(code);
            int insertAt = 0;
            while (insertAt < result.getGroups().size()
                    && GROUP_ORDER.indexOf(result.getGroups().get(insertAt).getGroupCode()) < targetOrder) {
                insertAt++;
            }
            result.getGroups().add(insertAt, group);
        }
        item.setCheckContent(checkContent(item.getCheckCode(), item.getCheckName()));
        group.getItems().add(item);
        incrementGroupCount(group, item.getStatus());
    }

    private IngestDatasourceDiagnosisRespVO.Group newGroup(String code) {
        IngestDatasourceDiagnosisRespVO.Group group = new IngestDatasourceDiagnosisRespVO.Group();
        group.setGroupCode(code);
        group.setGroupName(groupName(code));
        group.setDescription(groupDescription(code));
        return group;
    }

    private void incrementGroupCount(IngestDatasourceDiagnosisRespVO.Group group, String status) {
        group.setTotalCount(group.getTotalCount() + 1);
        if (PASS.equals(status)) {
            group.setNormalCount(group.getNormalCount() + 1);
        } else if (WARN.equals(status)) {
            group.setWarningCount(group.getWarningCount() + 1);
        } else if (FAIL.equals(status)) {
            group.setErrorCount(group.getErrorCount() + 1);
        }
    }

    private int countStatus(IngestDatasourceDiagnosisRespVO result, String status) {
        return (int) items(result).filter(item -> status.equals(item.getStatus())).count();
    }

    private Stream<IngestDatasourceDiagnosisRespVO.Item> items(IngestDatasourceDiagnosisRespVO result) {
        return result.getGroups().stream().flatMap(group -> group.getItems().stream());
    }

    private String groupCode(String checkCode) {
        if (checkCode == null || checkCode.startsWith("CONFIG") || checkCode.startsWith("DNS")
                || checkCode.startsWith("DIAGNOSIS_SCOPE")
                || checkCode.startsWith("TCP") || checkCode.startsWith("AUTH")) {
            return "BASE_CONNECTION";
        }
        if (checkCode.contains("RESOURCE_ACCESS") || checkCode.startsWith("DATABASE_ACCESS")
                || checkCode.startsWith("SCHEMA_ACCESS") || checkCode.startsWith("PROJECT_ACCESS")
                || checkCode.startsWith("DIRECTORY_ACCESS")
                || checkCode.startsWith("ACCOUNT_PERMISSION")) {
            return "TARGET_RESOURCE_ACCESS";
        }
        if (checkCode.startsWith("METADATA") || checkCode.startsWith("RESOURCE_DISCOVERY")
                || checkCode.startsWith("STRUCTURE") || checkCode.startsWith("EXTENDED_METADATA")
                || checkCode.startsWith("FILE_LIST") || checkCode.startsWith("FILE_PROPERTIES")) {
            return "METADATA_CAPABILITY";
        }
        if (checkCode.startsWith("TABLE_READ") || checkCode.startsWith("QUERY")
                || checkCode.startsWith("FILE_READ")) {
            return "READ_CAPABILITY";
        }
        if (checkCode.startsWith("INSERT") || checkCode.startsWith("BATCH_WRITE")
                || checkCode.startsWith("FILE_UPLOAD") || checkCode.startsWith("TEMP_WRITE")) {
            return "WRITE_CAPABILITY";
        }
        return "ADVANCED_CAPABILITY";
    }

    private String groupName(String groupCode) {
        switch (groupCode) {
            case "TARGET_RESOURCE_ACCESS": return "目标资源访问";
            case "METADATA_CAPABILITY": return "元数据能力";
            case "READ_CAPABILITY": return "读取能力";
            case "WRITE_CAPABILITY": return "写入能力";
            case "ADVANCED_CAPABILITY": return "高级操作能力";
            default: return "基础连接";
        }
    }

    private String groupDescription(String groupCode) {
        switch (groupCode) {
            case "TARGET_RESOURCE_ACCESS": return "验证目标Database、Schema、Project或HDFS/FTP目录是否有效且可访问";
            case "METADATA_CAPABILITY": return "验证资源发现、结构及扩展属性读取能力";
            case "READ_CAPABILITY": return "验证表读取或文件内容读取权限";
            case "WRITE_CAPABILITY": return "验证INSERT、批量写入或文件上传能力";
            case "ADVANCED_CAPABILITY": return "验证更新、全量处理、结构管理、删除重命名及CDC等高级能力";
            default: return "验证网络连通性、端口和账号认证";
        }
    }

    private String checkContent(String checkCode, String checkName) {
        if (checkCode == null) {
            return checkName;
        }
        switch (checkCode) {
            case "DNS_RESOLVE": return "检查数据源IP或域名是否可解析";
            case "TCP_CONNECT": return "检查目标服务端口是否可以建立连接";
            case "AUTHENTICATION": return "检查账号、密码、Token或AccessKey等认证信息";
            case "DIAGNOSIS_SCOPE": return "说明当前数据源类型的后端诊断能力范围";
            case "TARGET_RESOURCE_ACCESS": return "检查配置的目标资源是否存在且当前账号可以进入";
            case "METADATA_READ":
            case "RESOURCE_DISCOVERY": return "检查是否能够发现目标范围内的表或目录";
            case "STRUCTURE_READ": return "检查是否能够读取字段、类型及主键等结构信息";
            case "TABLE_READ": return "检查是否能够执行目标表读取操作并返回结果";
            case "FILE_READ": return "诊断不下载文件，文件内容读取权限需管理员确认";
            case "FILE_UPLOAD": return "诊断不上传文件，目标目录写入权限需管理员确认";
            case "DIRECTORY_CREATE": return "创建临时诊断目录并验证目录创建权限";
            case "FILE_RENAME": return "重命名临时诊断文件并验证重命名权限";
            case "FILE_DELETE": return "删除临时诊断文件和目录并验证删除权限";
            default: return checkName == null ? checkCode : "检查" + checkName;
        }
    }
}
