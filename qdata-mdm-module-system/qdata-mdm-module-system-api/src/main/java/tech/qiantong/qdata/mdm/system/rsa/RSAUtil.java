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

package tech.qiantong.qdata.mdm.system.rsa;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.stream.Collectors;

public class RSAUtil {

    public static String loadPrivateKey(String fileName) throws IOException {
        InputStream inputStream = RSAUtil.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IOException("Private key file not found in classpath: " + fileName);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 使用私钥解密数据
     * @param encryptedData 用公钥加密过后的数据
     * @return 明文
     */
    public static String decryptData(String encryptedData) {
        try {
            // 从classpath读取私钥文件
            String privateKey = loadPrivateKey("private_key.pem");
            privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "");
            privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");
            // 创建RSA对象，使用私钥
            RSA rsa = new RSA(privateKey, null);

            // 解密数据
            byte[] decryptedBytes = rsa.decrypt(encryptedData, KeyType.PrivateKey);

            // 返回解密后的字符串
            return new String(decryptedBytes);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(RSAUtil.class).error("Operation failed", e);
            return null;
        }
    }

    /**
     * 使用私钥加密数据
     * @param data 要加密的明文数据
     * @return 加密后的数据
     */
    public static String encryptData(String data) {
        try {
            if (data == null) {
                return "";
            }
            // 从classpath读取私钥文件
            String privateKey = loadPrivateKey("private_key.pem");
            privateKey = privateKey.replace("-----BEGIN PRIVATE KEY-----", "");
            privateKey = privateKey.replace("-----END PRIVATE KEY-----", "");

            // 创建RSA对象，使用私钥
            RSA rsa = new RSA(privateKey, null);

            // 加密数据
            byte[] encryptedBytes = rsa.encrypt(data.getBytes(), KeyType.PrivateKey);

            // 返回加密后的字符串（通常使用Base64编码以便可读性）
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(RSAUtil.class).error("Operation failed", e);
            return null;
        }
    }

    /**
     * 使用公钥解密数据
     * @param encryptedData 使用私钥加密过后的数据
     * @return 解密后的明文
     */
    public static String decryptWithPublicKey(String encryptedData) {
        try {
            if(StringUtils.isBlank(encryptedData)) return null;
            // 从classpath读取公钥文件
            String publicKey = loadPrivateKey("public_key.pem");
            publicKey = publicKey.replace("-----BEGIN PUBLIC KEY-----", "");
            publicKey = publicKey.replace("-----END PUBLIC KEY-----", "");

            // 创建RSA对象，使用公钥
            RSA rsa = new RSA(null, publicKey);

            // 使用Base64解码加密数据
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            // 解密数据
            byte[] decryptedBytes = rsa.decrypt(encryptedBytes, KeyType.PublicKey);

            // 返回解密后的字符串
            return new String(decryptedBytes);
        } catch (IOException e) {
            org.slf4j.LoggerFactory.getLogger(RSAUtil.class).error("Operation failed", e);
            return null;
        }
    }

    /**
     * 解密公钥加密后的数据
     */

}
