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

package tech.qiantong.qdata.mdm.system.service.flowMyBusiness;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessSaveReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.flowMyBusiness.vo.SystemFlowMyBusinessPageReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.flowMyBusiness.SystemFlowMyBusinessDO;
/**
 * 审批记录Service接口
 *
 * @author qdata
 * @date 2025-04-25
 */
public interface ISystemFlowMyBusinessService extends IService<SystemFlowMyBusinessDO> {

    /**
     * 获得审批记录分页列表
     *
     * @param pageReqVO 分页请求
     * @return 审批记录分页列表
     */
    PageResult<SystemFlowMyBusinessDO> getSystemFlowMyBusinessPage(SystemFlowMyBusinessPageReqVO pageReqVO);

    /**
     * 创建审批记录
     *
     * @param createReqVO 审批记录信息
     * @return 审批记录编号
     */
    Long createSystemFlowMyBusiness(SystemFlowMyBusinessSaveReqVO createReqVO);

    /**
     * 更新审批记录
     *
     * @param updateReqVO 审批记录信息
     */
    int updateSystemFlowMyBusiness(SystemFlowMyBusinessSaveReqVO updateReqVO);

    /**
     * 删除审批记录
     *
     * @param idList 审批记录编号
     */
    int removeSystemFlowMyBusiness(Collection<Long> idList);

    /**
     * 获得审批记录详情
     *
     * @param id 审批记录编号
     * @return 审批记录
     */
    SystemFlowMyBusinessDO getSystemFlowMyBusinessById(Long id);

    /**
     * 获得全部审批记录列表
     *
     * @return 审批记录列表
     */
    List<SystemFlowMyBusinessDO> getSystemFlowMyBusinessList();

    /**
     * 获得全部审批记录 Map
     *
     * @return 审批记录 Map
     */
    Map<Long, SystemFlowMyBusinessDO> getSystemFlowMyBusinessMap();

    /**
     * 导入审批记录数据
     *
     * @param importExcelList 审批记录数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    String importSystemFlowMyBusiness(List<SystemFlowMyBusinessRespVO> importExcelList, boolean isUpdateSupport, String operName);

}
