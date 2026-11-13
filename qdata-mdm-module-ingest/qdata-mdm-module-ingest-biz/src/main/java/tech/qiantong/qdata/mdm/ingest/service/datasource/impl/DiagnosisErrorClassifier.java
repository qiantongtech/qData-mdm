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

import lombok.Getter;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 将不同驱动和SDK异常收敛为稳定的诊断错误码，并负责对外错误摘要脱敏。
 */
class DiagnosisErrorClassifier {

    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)([\\\"']?(?:password|pwd|access[_-]?key(?:id|secret)?|secret(?:key)?|token)"
                    + "[\\\"']?\\s*[=:]\\s*[\\\"']?)([^,;\\\"'\\s&}]+)");
    private static final Pattern URI_USER_INFO = Pattern.compile(
            "(?i)(://[^\\s:/@]+:)([^\\s/@]+)(@)");

    DiagnosisError classify(Throwable throwable, String defaultCode) {
        Throwable root = rootCause(throwable);
        SQLException sqlException = findSqlException(throwable);
        String message = root == null || trimToNull(root.getMessage()) == null
                ? "执行诊断时发生异常" : root.getMessage();
        String lower = message.toLowerCase(Locale.ROOT);
        String code = defaultCode;

        if (root instanceof UnknownHostException) {
            code = "NETWORK_DNS_FAILED";
            message = "无法解析数据源域名";
        } else if (root instanceof SocketTimeoutException || lower.contains("timed out") || lower.contains("超时")) {
            code = "NETWORK_TIMEOUT";
            message = "连接数据源超时";
        } else if (root instanceof ConnectException || lower.contains("connection refused")) {
            code = "NETWORK_PORT_UNREACHABLE";
            message = "目标端口拒绝连接";
        } else if (sqlException != null) {
            String state = sqlException.getSQLState();
            int vendorCode = sqlException.getErrorCode();
            if (vendorCode == 1045 || vendorCode == 18456 || vendorCode == 1017
                    || (state != null && state.startsWith("28"))) {
                code = "AUTH_INVALID_CREDENTIALS";
                message = "数据库拒绝当前账号认证";
            } else if (vendorCode == 1049 || "3D000".equals(state)
                    || lower.contains("unknown database") || lower.contains("database does not exist")) {
                code = "DATABASE_NOT_FOUND";
                message = "指定数据库或实例不存在";
            } else if (state != null && state.startsWith("08")) {
                code = "NETWORK_CONNECTION_FAILED";
                message = "数据库网络连接失败";
            } else if ("42501".equals(state) || vendorCode == 1031 || vendorCode == 229
                    || lower.contains("permission denied") || lower.contains("insufficient privilege")
                    || lower.contains("not authorized") || lower.contains("access denied")) {
                code = defaultCode.endsWith("_DENIED") ? defaultCode : permissionCode(defaultCode);
                message = permissionMessage(code);
            } else if (lower.contains("does not exist") || lower.contains("not found")
                    || lower.contains("invalid object name")) {
                code = "TABLE_NOT_FOUND";
                message = "指定库表不存在或当前账号不可见";
            }
        } else if (lower.contains("password") || lower.contains("authentication")
                || lower.contains("login failed") || lower.contains("accesskey")
                || lower.contains("登录") || lower.contains("认证")) {
            code = "AUTH_INVALID_CREDENTIALS";
            message = "数据源拒绝当前账号认证";
        }
        return new DiagnosisError(code, message, sanitize(root == null ? null : root.getMessage()));
    }

    List<String> suggestions(String code) {
        if (code == null) {
            return Collections.emptyList();
        }
        if (code.startsWith("NETWORK_DNS")) {
            return Arrays.asList("检查域名是否正确", "检查应用服务器的DNS配置");
        }
        if (code.startsWith("NETWORK")) {
            return Arrays.asList("检查IP和端口是否正确", "检查防火墙、安全组和数据库白名单");
        }
        if (code.startsWith("AUTH")) {
            return Arrays.asList("确认账号和密码是否正确", "确认账号允许从当前应用服务器IP登录",
                    "检查账号是否锁定或密码是否过期");
        }
        if (code.startsWith("DATABASE")) {
            return Arrays.asList("检查数据库名、SID和Schema配置", "确认目标实例已创建并正在运行");
        }
        if (code.startsWith("TARGET_RESOURCE")) {
            return Arrays.asList("检查Database、Schema、Owner、Project或目录配置",
                    "确认当前账号对目标资源具备进入和列举权限");
        }
        if (code.startsWith("STORAGE")) {
            return Arrays.asList("检查HDFS路径或FTP目录是否正确",
                    "为当前账号授予目标路径的列举和访问权限");
        }
        if (code.startsWith("FILE_READ")) {
            return Arrays.asList("为当前账号授予目标文件读取权限",
                    "确认目标文件存在且不是目录");
        }
        if (code.startsWith("FILE_WRITE") || code.startsWith("FILE_CREATE")) {
            return Arrays.asList("为当前账号授予目标目录写入权限",
                    "确认目标存储空间充足且路径允许写入");
        }
        if (code.startsWith("FILE_RENAME")) {
            return Collections.singletonList("授予文件移动和重命名权限");
        }
        if (code.startsWith("FILE_DELETE")) {
            return Collections.singletonList("授予目标文件和目录删除权限");
        }
        if (code.startsWith("METADATA")) {
            return Arrays.asList("授予当前账号Schema和系统视图查询权限", "确认目标Schema对当前账号可见");
        }
        if (code.startsWith("ACCOUNT")) {
            return Arrays.asList("允许当前账号查询角色与授权信息", "联系数据库管理员确认账号有效权限");
        }
        if (code.startsWith("TABLE_WRITE")) {
            return Arrays.asList("为当前账号或所属角色授予目标表INSERT、UPDATE、DELETE权限",
                    "检查表名和Schema是否正确");
        }
        if (code.startsWith("TABLE")) {
            return Arrays.asList("为当前账号授予目标表SELECT权限", "检查表名和Schema是否正确");
        }
        if (code.startsWith("TARGET")) {
            return Arrays.asList("根据失败检查项授予CREATE、INSERT、UPDATE、DELETE或DROP权限",
                    "确认当前账号的默认Schema可写");
        }
        return Collections.singletonList("根据错误摘要检查连接配置，或联系数据库管理员");
    }

    private String permissionCode(String defaultCode) {
        if (defaultCode.startsWith("METADATA")) {
            return "METADATA_PERMISSION_DENIED";
        }
        if (defaultCode.startsWith("TABLE")) {
            return "TABLE_READ_DENIED";
        }
        return defaultCode;
    }

    private String permissionMessage(String code) {
        if (code.startsWith("METADATA")) {
            return "当前账号没有元数据读取权限";
        }
        if (code.startsWith("TABLE")) {
            return "当前账号没有指定表的读取权限";
        }
        if (code.startsWith("TARGET")) {
            return "当前账号缺少目标库写入操作权限";
        }
        return "当前账号权限不足";
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException) {
                return (SQLException) current;
            }
            current = current.getCause();
        }
        return null;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = SENSITIVE_VALUE.matcher(value).replaceAll("$1***");
        sanitized = URI_USER_INFO.matcher(sanitized).replaceAll("$1***$3");
        return sanitized.length() > 500 ? sanitized.substring(0, 500) : sanitized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Getter
    static class DiagnosisError {
        private final String code;
        private final String message;
        private final String rawError;

        DiagnosisError(String code, String message, String rawError) {
            this.code = code;
            this.message = message;
            this.rawError = rawError;
        }
    }
}
