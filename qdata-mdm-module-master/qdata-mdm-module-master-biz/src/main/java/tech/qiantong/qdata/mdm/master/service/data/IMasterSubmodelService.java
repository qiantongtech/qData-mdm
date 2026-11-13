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

package tech.qiantong.qdata.mdm.master.service.data;

import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeSubmodelSaveReqVO;

import tech.qiantong.qdata.mdm.master.api.instance.dto.MasterModelRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelDataRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelRecordRespVO;
import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterSubmodelPageReqVO;

import java.util.List;
import java.util.Map;
import lombok.Value;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeRequestDO;
import tech.qiantong.qdata.mdm.master.dal.repository.data.maintenance.TableMutation;

/**
 * 子模型定义、数据查询与维护校验接口。
 */
public interface IMasterSubmodelService {

    List<Map<String, Object>> freezeChanges(MasterModelRespDTO mainModel, Long mainRecordId,
            List<MasterChangeSubmodelSaveReqVO> changes);

    WritePlan planChanges(MasterModelRespDTO mainModel, String mainMode, Long mainRecordId,
            String proposedData, MasterChangeRequestDO request);

    @Value
    class WritePlan {
        List<TableMutation> writes;
        List<Map<String, Object>> before;
        List<Map<String, Object>> after;
    }

    /**
     * 校验主子模型维护内容并收集问题。
     *
     * @param instanceId 模型物化实例 ID
     * @param mainModel 主模型及其字段配置
     * @param mainMode 主记录维护操作类型
     * @param mainRecordId 主记录 ID
     * @param changes 子模型变更集合
     * @param errors 校验问题集合
     */
    void validateChanges(Long instanceId, MasterModelRespDTO mainModel, String mainMode,
                                Long mainRecordId,
                                List<MasterChangeSubmodelSaveReqVO> changes,
                                Map<String, String> errors);

    /**
     * 查询可维护的子模型定义。
     *
     * @param instanceId 模型物化实例 ID
     * @param submodelId 子模型 ID
     * @return 子模型定义及选中子模型的分页记录
     */
    MasterSubmodelDataRespVO getDefinitions(Long instanceId, Long submodelId);

    /**
     * 查询主数据或子模型分页数据。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param reqVO 子模型、分页及排序参数
     * @return 子模型定义及选中子模型的分页记录
     */
    MasterSubmodelDataRespVO getData(Long instanceId, Long masterDataId,
                                     MasterSubmodelPageReqVO reqVO);

    /**
     * 查询指定子模型记录。
     *
     * @param instanceId 模型物化实例 ID
     * @param masterDataId 稳定主数据 ID
     * @param submodelId 子模型 ID
     * @param subRecordId 子记录 ID
     * @return 指定子记录及其字段定义
     */
    MasterSubmodelRecordRespVO getRecord(Long instanceId, Long masterDataId,
                                                          Long submodelId, Long subRecordId);
}
