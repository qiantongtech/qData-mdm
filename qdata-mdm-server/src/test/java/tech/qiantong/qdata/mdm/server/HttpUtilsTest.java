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

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tech.qiantong.qdata.mdm.common.httpClient.HeaderEntity;
import tech.qiantong.qdata.mdm.common.httpClient.HttpUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * HttpUtils test
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = QDataMdmApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpUtilsTest {

    @Test
    public void testSendGetComm() {
        String url = "https://www.baidu.com";
        List<HeaderEntity> headers = new ArrayList<>();
        try {
            HttpUtils.ResponseObject responseObject = HttpUtils.sendGet(url, headers);
            System.out.println(responseObject.getBody().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @Test
    public void testSendGetUtils() {
        String url = "https://www.baidu.com";
        List<HeaderEntity> headers = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        try {
            String s = tech.qiantong.qdata.mdm.common.utils.http.HttpUtils.sendGet(url, "{}");
            System.out.println(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


}
