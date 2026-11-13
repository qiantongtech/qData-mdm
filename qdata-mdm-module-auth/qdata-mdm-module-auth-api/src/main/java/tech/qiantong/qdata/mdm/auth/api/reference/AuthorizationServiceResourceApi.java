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

package tech.qiantong.qdata.mdm.auth.api.reference;

import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceDTO;
import tech.qiantong.qdata.mdm.auth.api.reference.dto.AuthorizationReferenceFieldDTO;

import java.util.Collection;
import java.util.Map;

/** 数据服务模块后续实现此接口，授权模块无需读取其数据库表。 */
public interface AuthorizationServiceResourceApi {
    Map<Long, AuthorizationReferenceDTO> getClients(Collection<Long> clientIds);

    Map<Long, AuthorizationReferenceDTO> getServices(Collection<Long> serviceIds);

    Map<Long, AuthorizationReferenceFieldDTO> getServiceFields(Collection<Long> fieldIds);
}
