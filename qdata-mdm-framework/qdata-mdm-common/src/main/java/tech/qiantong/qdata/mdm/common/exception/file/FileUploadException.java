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

package tech.qiantong.qdata.mdm.common.exception.file;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * File upload exception class
 *
 * @author qdata
 */
public class FileUploadException extends Exception
{

    private static final long serialVersionUID = 1L;

    private final Throwable cause;

    public FileUploadException()
    {
        this(null, null);
    }

    public FileUploadException(final String msg)
    {
        this(msg, null);
    }

    public FileUploadException(String msg, Throwable cause)
    {
        super(msg);
        this.cause = cause;
    }

    @Override
    public void printStackTrace(PrintStream stream)
    {
        super.printStackTrace(stream);
        if (cause != null)
        {
            stream.println("Caused by:");
            cause.printStackTrace(stream);
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer)
    {
        super.printStackTrace(writer);
        if (cause != null)
        {
            writer.println("Caused by:");
            cause.printStackTrace(writer);
        }
    }

    @Override
    public Throwable getCause()
    {
        return cause;
    }
}
