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

package tech.qiantong.qdata.mdm.merge.support;

import tech.qiantong.qdata.mdm.common.core.domain.BaseEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 审计字段和短编号。
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public final class MergeAudit {
    private MergeAudit() {
    }

    public static void fill(BaseEntity entity, Long userId, String name) {
        Date now = new Date();
        entity.setCreatorId(userId);
        entity.setCreateBy(name);
        entity.setCreateTime(now);
        entity.setUpdatorId(userId);
        entity.setUpdateBy(name);
        entity.setUpdateTime(now);
    }

    public static String no(String prefix, int seq) {
        return prefix + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + String.format("%02d", Math.abs(seq) % 100);
    }

    /** 生成插入任务前使用的临时编号，避免并发插入时占用相同编号。 */
    public static String temporaryTaskNo() {
        return "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    /** 使用创建时间和数据库主键生成可读且并发唯一的正式任务编号。 */
    public static String taskNo(Date createTime, Long id) {
        if (createTime == null || id == null || id <= 0) {
            throw new IllegalArgumentException("创建时间和任务主键不能为空");
        }
        String idCode = Long.toString(id, Character.MAX_RADIX).toUpperCase(Locale.ROOT);
        idCode = String.format(Locale.ROOT, "%6s", idCode).replace(' ', '0');
        return "DT" + new SimpleDateFormat("yyyyMMddHHmmss").format(createTime) + "-" + idCode;
    }

    public static String cut(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    public static void touch(BaseEntity entity, Long userId, String name) {
        entity.setUpdatorId(userId);
        entity.setUpdateBy(name);
        entity.setUpdateTime(new Date());
    }

    public static String text(String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean same(String left, String right) {
        return text(left).equals(text(right));
    }

}
