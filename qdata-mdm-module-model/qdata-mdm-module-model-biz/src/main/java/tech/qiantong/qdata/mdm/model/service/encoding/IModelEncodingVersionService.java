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
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;

import java.util.Collection;

/**
 * 编码方案版本Service接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface IModelEncodingVersionService extends IService<ModelEncodingVersionDO> {

    /**
     * 获得编码方案版本分页列表
     *
     * @param pageReqVO 分页请求
     * @return 编码方案版本分页列表
     */
    PageResult<ModelEncodingVersionDO> getModelEncodingVersionPage(ModelEncodingVersionPageReqVO pageReqVO);

    /**
     * 创建编码方案版本
     *
     * @param createReqVO 编码方案版本信息
     * @return 编码方案版本编号
     */
    Long createModelEncodingVersion(ModelEncodingVersionSaveReqVO createReqVO);

    /**
     * 更新编码方案版本
     *
     * @param updateReqVO 编码方案版本信息
     */
    int updateModelEncodingVersion(ModelEncodingVersionSaveReqVO updateReqVO);

    /**
     * 更新编码方案版本状态
     *
     * @param id 编码方案版本编号
     * @param status 版本状态
     * @return 更新结果
     */
    int updateModelEncodingVersionStatus(Long id, Integer status);

    /**
     * 删除编码方案版本
     *
     * @param idList 编码方案版本编号
     */
    int removeModelEncodingVersion(Collection<Long> idList);

    /**
     * 获得编码方案版本详情
     *
     * @param id 编码方案版本编号
     * @return 编码方案版本
     */
    ModelEncodingVersionDO getModelEncodingVersionById(Long id);

}
