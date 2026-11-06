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

package tech.qiantong.qdata.mdm.common.database.datasource;

import org.springframework.stereotype.Component;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheDataSourceFactoryBean extends AbstractDataSourceFactory {

	/**
	 * Data source cache
	 */
	private static Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

	@Override
	public DataSource createDataSource(DbQueryProperty property) {
		String key = property.getDbType() + ":" + property.getHost()
				+ ":" + property.getPort() + ":" + property.getUsername()
				+ ":" + property.getPassword() + ":" + property.getDbName()
				+ ":" + property.getSid();
		String s = compress(key);
		DataSource dataSource = dataSourceMap.get(s);
		if (null == dataSource) {
			synchronized (CacheDataSourceFactoryBean.class) {
				dataSource = super.createDataSource(property);
				dataSourceMap.put(s, dataSource);
			}
		}
		return dataSource;
	}

	// Compression
	public static String compress(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			org.slf4j.LoggerFactory.getLogger(CacheDataSourceFactoryBean.class).error("Operation failed", e);
		}
		md.update(str.getBytes());
		byte b[] = md.digest();
		int i;
		StringBuffer buf = new StringBuffer();
		for (int offset = 0; offset < b.length; offset++) {
			i = b[offset];
			if (i < 0)
				i += 256;
			if (i < 16)
				buf.append("0");
			buf.append(Integer.toHexString(i));
		}
		return buf.toString().substring(8, 24).toUpperCase();
	}
}
