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
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataRespVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataSaveReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.dict.vo.ModelDictDataUsageRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.dict.ModelDictDataDO;

import java.util.Collection;
import java.util.List;

/**
 * 参考数据项Service接口
 *
 * @author qdata
 * @date 2026-09-16
 */
public interface IModelDictDataService extends IService<ModelDictDataDO> {

    /**
     * 获得参考数据项分页列表
     *
     * @param pageReqVO 分页请求
     * @return 参考数据项分页列表
     */
    PageResult<ModelDictDataDO> getModelDictDataPage(ModelDictDataPageReqVO pageReqVO);

    /**
     * 创建参考数据项
     *
     * @param createReqVO 参考数据项信息
     * @return 参考数据项编号
     */
    Long createModelDictData(ModelDictDataSaveReqVO createReqVO);

    /**
     * 更新参考数据项
     *
     * @param updateReqVO 参考数据项信息
     */
    int updateModelDictData(ModelDictDataSaveReqVO updateReqVO);

    /**
     * 校验参考数据项编码是否唯一
     */
    boolean checkCodeUnique(String code, Long id);

    /**
     * 更新参考数据项状态
     *
     * @param id     参考数据项编号
     * @param status 状态
     */
    int updateModelDictDataStatus(Long id, Boolean status);

    /**
     * 删除参考数据项
     *
     * @param idList 参考数据项编号
     */
    int removeModelDictData(Collection<Long> idList);

    /**
     * 获得参考数据项详情
     *
     * @param id 参考数据项编号
     * @return 参考数据项
     */
    ModelDictDataDO getModelDictDataById(Long id);

    /**
     * 查询指定参考数据类型的模型引用列表。
     *
     * @param typeId 参考数据类型 ID
     * @return 模型基础信息及其引用当前参考数据类型的字段列表；无引用时返回空列表
     */
    List<ModelDictDataUsageRespVO> getModelDictDataUsage(Long typeId);


    /**
     * 导入参考数据项数据
     *
     * @param importExcelList 参考数据项数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    String importModelDictData(List<ModelDictDataRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
