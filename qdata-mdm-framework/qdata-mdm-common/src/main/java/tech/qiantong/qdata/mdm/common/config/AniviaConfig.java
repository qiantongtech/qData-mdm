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

package tech.qiantong.qdata.mdm.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Read project related configuration
 *
 * @author qdata
 */
@Component
@ConfigurationProperties(prefix = "qdata-mdm")
public class AniviaConfig
{
    /** Project name */
    private String name;

    /** version */
    private String version;

    /** Copyright year */
    private String copyrightYear;

    private String author;

    private String description;

    /** Upload path */
    private static String profile;

    /** Get address switch */
    private static boolean addressEnabled;

    /** Verification code type */
    private static String captchaType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public static String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        AniviaConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        AniviaConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        AniviaConfig.captchaType = captchaType;
    }

    /**
     * Get the import upload path
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * Get the avatar upload path
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * Get download path
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * Get upload path
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
