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

package tech.qiantong.qdata.mdm.model.service.dict;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypePageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictTypeSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictTypeDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 参考数据Service接口
 *
 * @author qdata
 * @date 2026-09-16
 */
public interface IModelDictTypeService extends IService<ModelDictTypeDO> {

    /**
     * 获得参考数据分页列表
     *
     * @param pageReqVO 分页请求
     * @return 参考数据分页列表
     */
    PageResult<ModelDictTypeDO> getModelDictTypePage(ModelDictTypePageReqVO pageReqVO);

    /**
     * 创建参考数据
     *
     * @param createReqVO 参考数据信息
     * @return 参考数据编号
     */
    Long createModelDictType(ModelDictTypeSaveReqVO createReqVO);

    /**
     * 更新参考数据
     *
     * @param updateReqVO 参考数据信息
     */
    int updateModelDictType(ModelDictTypeSaveReqVO updateReqVO);

    /**
     * 校验参考数据编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 更新参考数据状态
     *
     * @param id     参考数据编号
     * @param status 状态
     */
    int updateModelDictTypeStatus(Long id, Boolean status);

    /**
     * 批量更新参考数据状态
     */
    int updateModelDictTypeStatusBatch(Collection<Long> idList, Boolean status);

    /**
     * 删除参考数据
     *
     * @param idList 参考数据编号
     */
    int removeModelDictType(Collection<Long> idList);

    /**
     * 批量查询参考数据关联的数据项数量
     */
    long getDataCount(Collection<Long> ids);

    /**
     * 获得参考数据详情
     *
     * @param id 参考数据编号
     * @return 参考数据
     */
    ModelDictTypeDO getModelDictTypeById(Long id);

    /**
     * 获得全部参考数据列表
     *
     * @return 参考数据列表
     */
    List<ModelDictTypeDO> getModelDictTypeList();

    /**
     * 获得全部参考数据 Map
     *
     * @return 参考数据 Map
     */
    Map<Long, ModelDictTypeDO> getModelDictTypeMap();


    /**
     * 导入参考数据数据
     *
     * @param importExcelList 参考数据数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importModelDictType(List<ModelDictTypeRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
