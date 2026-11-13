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

import tech.qiantong.qdata.mdm.auth.api.dataauthorization.dto.DataAuthorizationSaveReqDTO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationPageReqVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.BatchAuthorizationResultVO;
import tech.qiantong.qdata.mdm.auth.controller.admin.authorization.vo.DataAuthorizationRespVO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantDO;
import tech.qiantong.qdata.mdm.auth.dal.dataobject.AuthGrantFieldDO;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;

import java.util.List;

/**
 * 数据授权 Service 接口。
 */
public interface DataAuthorizationService {

    /**
     * 新增或修改数据授权；请求包含授权编号时修改已有授权。
     *
     * @param request 数据授权信息
     * @return 授权编号
     */
    Long save(DataAuthorizationSaveReqDTO request);

    /**
     * 批量新增数据授权。
     *
     * @param request 批量授权信息
     * @return 批量授权结果
     */
    BatchAuthorizationResultVO saveBatch(DataAuthorizationSaveReqDTO request);

    /**
     * 撤销数据授权。
     *
     * @param id 授权编号
     * @param operator 操作人
     */
    void revoke(Long id, String operator);

    /**
     * 批量撤销数据授权。
     *
     * @param ids 授权编号列表
     * @param operator 操作人
     */
    void revokeBatch(List<Long> ids, String operator);

    /**
     * 批量删除数据授权，删除前自动撤权，整批操作在同一事务内完成。
     *
     * @param ids 授权编号列表
     * @param operator 操作人
     */
    void deleteBatch(List<Long> ids, String operator);

    /**
     * 查询数据授权分页列表。
     *
     * @param request 分页及筛选条件
     * @return 数据授权分页结果
     */
    PageResult<DataAuthorizationRespVO> page(DataAuthorizationPageReqVO request);

    /**
     * 查询数据授权详情。
     *
     * @param id 授权编号
     * @return 数据授权详情
     */
    DataAuthorizationRespVO getDetail(Long id);

    /**
     * 查询授权记录列表。
     *
     * @return 授权记录列表
     */
    List<AuthGrantDO> list();

    /**
     * 根据编号查询授权记录。
     *
     * @param id 授权编号
     * @return 授权记录
     */
    AuthGrantDO get(Long id);

    /**
     * 查询数据授权的字段权限。
     *
     * @param grantId 授权编号
     * @return 字段权限列表
     */
    List<AuthGrantFieldDO> getFields(Long grantId);
}
