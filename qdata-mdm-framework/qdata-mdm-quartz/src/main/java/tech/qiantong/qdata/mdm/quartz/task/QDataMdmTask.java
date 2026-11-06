/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
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

package tech.qiantong.qdata.mdm.quartz.task;

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;

/**
 * 定时任务调度测试
 *
 * @author qdata
 */
@Component("qDataMdmTask")
public class QDataMdmTask
{
    public void qDataMdmMultipleParams(String s, Boolean b, Long l, Double d, Integer i)
    {
        org.slf4j.LoggerFactory.getLogger(QDataMdmTask.class).info(StringUtils.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void qDataMdmParams(String params)
    {
        org.slf4j.LoggerFactory.getLogger(QDataMdmTask.class).info("执行有参方法：" + params);
    }

    public void qDataMdmNoParams()
    {
        org.slf4j.LoggerFactory.getLogger(QDataMdmTask.class).info("执行无参方法");
    }
}
