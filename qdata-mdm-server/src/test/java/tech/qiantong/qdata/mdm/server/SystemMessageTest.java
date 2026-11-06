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

package tech.qiantong.qdata.mdm.server;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import tech.qiantong.qdata.mdm.system.controller.admin.system.message.vo.MessageSaveReqVO;
import tech.qiantong.qdata.mdm.system.service.message.impl.MessageServiceImpl;

import java.util.HashMap;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = QDataMdmApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SystemMessageTest {

    @Autowired
    private MessageServiceImpl iMessageService;
    /**
     *
     * Message test
     * Long templateId, MessageSaveReqVO messageSaveReqVO, Object entity
     */
    @Test
    public void testInternalMessageSend() {
        MessageSaveReqVO messageSaveReqVO = new MessageSaveReqVO();
        messageSaveReqVO.setSenderId(1L);
        messageSaveReqVO.setReceiverId(731L);
        Map<String, Object> map = new HashMap<>();
        map.put("test","àáâäǎæãåā");


        iMessageService.send(1L,messageSaveReqVO,map);




    }



}
