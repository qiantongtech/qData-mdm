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

package tech.qiantong.qdata.mdm.auth.service;

import tech.qiantong.qdata.mdm.auth.api.serviceauthorization.dto.ServiceAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.ServiceAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthServiceAccessDO;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;

import java.util.List;

/**
 * 服务授权 Service 接口。
 */
public interface ServiceAuthorizationService {

    /**
     * 新增或修改服务授权；请求包含授权编号时修改已有授权。
     *
     * @param request 服务授权信息
     * @return 授权编号
     */
    Long save(ServiceAuthorizationSaveReqDTO request);

    /**
     * 批量新增服务授权。
     *
     * @param request 批量授权信息
     * @return 批量授权结果
     */
    BatchAuthorizationResultVO saveBatch(ServiceAuthorizationSaveReqDTO request);

    /**
     * 撤销服务授权。
     *
     * @param id 授权编号
     * @param operator 操作人
     */
    void revoke(Long id, String operator);

    /**
     * 批量撤销服务授权。
     *
     * @param ids 授权编号列表
     * @param operator 操作人
     */
    void revokeBatch(List<Long> ids, String operator);

    /**
     * 批量删除服务授权，删除前自动撤权，整批操作在同一事务内完成。
     *
     * @param ids 授权编号列表
     * @param operator 操作人
     */
    void deleteBatch(List<Long> ids, String operator);

    /**
     * 查询服务授权分页列表。
     *
     * @param request 分页及筛选条件
     * @return 服务授权分页结果
     */
    PageResult<ServiceAuthorizationRespVO> page(ServiceAuthorizationPageReqVO request);

    /**
     * 根据数据服务 ID 查询授权记录列表，不分页。
     *
     * @param serviceId 数据服务 ID
     * @return 授权记录列表
     */
    List<ServiceAuthorizationRespVO> listByServiceId(Long serviceId);

    /**
     * 查询服务授权详情。
     *
     * @param id 授权编号
     * @return 服务授权详情
     */
    ServiceAuthorizationRespVO getDetail(Long id);

    /**
     * 查询服务授权记录列表。
     *
     * @return 服务授权记录列表
     */
    List<AuthServiceAccessDO> list();

    /**
     * 根据编号查询服务授权记录。
     *
     * @param id 授权编号
     * @return 服务授权记录
     */
    AuthServiceAccessDO get(Long id);

    /**
     * 查询服务授权的字段权限。
     *
     * @param serviceAccessId 服务授权编号
     * @return 字段权限列表
     */
    List<AuthGrantFieldDO> getFields(Long serviceAccessId);
}
