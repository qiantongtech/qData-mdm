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
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingItemSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 编码方案规则段Service接口
 *
 * @author qdata
 * @date 2026-09-17
 */
public interface IModelEncodingItemService extends IService<ModelEncodingItemDO> {

    /**
     * 获得编码方案规则段分页列表
     *
     * @param pageReqVO 分页请求
     * @return 编码方案规则段分页列表
     */
    PageResult<ModelEncodingItemDO> getModelEncodingItemPage(ModelEncodingItemPageReqVO pageReqVO);

    /**
     * 创建编码方案规则段
     *
     * @param createReqVO 编码方案规则段信息
     * @return 编码方案规则段编号
     */
    Long createModelEncodingItem(ModelEncodingItemSaveReqVO createReqVO);

    /**
     * 更新编码方案规则段
     *
     * @param updateReqVO 编码方案规则段信息
     */
    int updateModelEncodingItem(ModelEncodingItemSaveReqVO updateReqVO);

    /**
     * 按编码版本事务性保存整套规则段。
     *
     * @param encodingVersionId 编码版本ID
     * @param items 按生成顺序排列的规则段
     */
    void saveModelEncodingItems(Long encodingVersionId, List<ModelEncodingItemSaveReqVO> items);

    /**
     * 删除编码方案规则段
     *
     * @param idList 编码方案规则段编号
     */
    int removeModelEncodingItem(Collection<Long> idList);

    /**
     * 获得编码方案规则段详情
     *
     * @param id 编码方案规则段编号
     * @return 编码方案规则段
     */
    ModelEncodingItemDO getModelEncodingItemById(Long id);

}
