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

package tech.qiantong.qdata.mdm.server;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Start the program
 *
 * @author qdata
 */
@EnableFileStorage
@EnableScheduling // Add function to enable scheduled tasks
@ComponentScan(basePackages = {"tech.qiantong.qdata.mdm"})
@ServletComponentScan(basePackages = {"tech.qiantong.qdata.mdm"})
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class QDataMdmApplication
{
    public static final String BRAND_BLUE = "\u001B[38;2;29;80;163m";
    public static final String RESET = "\u001B[0m";

    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(QDataMdmApplication.class, args);

        System.out.println(
                BRAND_BLUE +
                        "     (♥◠‿◠)ﾉﾞ  qData-mdm started successfully!  ლ(´ڡ`ლ)ﾞ\n" +
                        "═════════════════════════════════════════════════════\n" +
                        "           ____            _                                _\n" +
                        "    __ _  |  _ \\    __ _  | |_    __ _       _ __ ___    __| |  _ __ ___\n" +
                        "   / _` | | | | |  / _` | | __|  / _` |     | '_ ` _ \\  / _` | | '_ ` _ \\\n" +
                        "  | (_| | | |_| | | (_| | | |_  | (_| |     | | | | | || (_| | | | | | | |\n" +
                        "   \\__, | |____/   \\__,_|  \\__|  \\__,_|     |_| |_| |_| \\__,_| |_| |_| |_|\n" +
                        "      |_|\n" +
                        "═════════════════════════════════════════════════════\n" +
                        "     LOCALIZED STACK  ·  STABLE  ·  EFFICIENT  ·  SECURE" +
                        RESET
        );
    }
}
