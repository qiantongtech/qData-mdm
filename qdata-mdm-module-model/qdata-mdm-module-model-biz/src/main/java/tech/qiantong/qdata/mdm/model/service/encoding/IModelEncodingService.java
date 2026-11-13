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

package tech.qiantong.qdata.mdm.model.service.encoding;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingUsageRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;

import java.util.Collection;
import java.util.List;

/**
 * 编码方案Service接口，复用公共生成API，由现有编码方案服务统一实现。
 *
 * @author qdata
 * @date 2026-09-16
 */
public interface IModelEncodingService extends IService<ModelEncodingDO>, ModelEncodingApi {

    /**
     * 获得编码方案分页列表
     *
     * @param pageReqVO 分页请求
     * @return 编码方案分页列表
     */
    PageResult<ModelEncodingDO> getModelEncodingPage(ModelEncodingPageReqVO pageReqVO);

    /**
     * 查询存在规则段且当前版本状态为 3 的可用编码方案。
     */
    PageResult<ModelEncodingDO> getAvailableModelEncodingPage(ModelEncodingPageReqVO pageReqVO);

    /**
     * 创建编码方案
     *
     * @param createReqVO 编码方案信息
     * @return 编码方案编号
     */
    Long createModelEncoding(ModelEncodingSaveReqVO createReqVO);

    /**
     * 更新编码方案
     *
     * @param updateReqVO 编码方案信息
     */
    int updateModelEncoding(ModelEncodingSaveReqVO updateReqVO);

    /**
     * 校验编码方案编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 删除编码方案
     *
     * @param idList 编码方案编号
     */
    int removeModelEncoding(Collection<Long> idList);

    /**
     * 查询编码方案当前版本被主数据模型字段引用的数量
     */
    long getModelCount(Long id);

    /**
     * 批量查询编码方案是否被主数据模型字段引用
     */
    boolean hasModelReference(Collection<Long> ids);

    /**
     * 批量查询编码方案当前版本是否处于发布状态
     */
    boolean hasPublishedVersion(Collection<Long> ids);

    /**
     * 查询指定编码方案版本引用的模型基础信息及字段，模型状态与模型定义列表一致。
     */
    List<ModelEncodingUsageRespVO> getUsageByVersionId(Long encodingVersionId);

    /**
     * 获得编码方案详情
     *
     * @param id 编码方案编号
     * @return 编码方案
     */
    ModelEncodingDO getModelEncodingById(Long id);

}
