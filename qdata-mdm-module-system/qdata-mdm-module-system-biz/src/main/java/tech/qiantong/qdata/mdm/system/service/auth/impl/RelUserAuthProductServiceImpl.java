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

package tech.qiantong.qdata.mdm.system.service.auth.impl;

import javax.annotation.Resource;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import tech.qiantong.qdata.mdm.system.dal.dataobject.auth.RelUserAuthProductDO;
import tech.qiantong.qdata.mdm.system.dal.mapper.auth.RelUserAuthProductMapper;
import tech.qiantong.qdata.mdm.system.service.auth.IRelUserAuthProductService;

/**
 * 用户与认证中心关系Service业务层处理
 *
 * @author qdata
 * @date 2024-11-07
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RelUserAuthProductServiceImpl  extends ServiceImpl<RelUserAuthProductMapper, RelUserAuthProductDO> implements IRelUserAuthProductService {
    @Resource
    private RelUserAuthProductMapper relUserAuthProductMapper;

}
