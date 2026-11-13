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

package tech.qiantong.qdata.mdm.merge.dal.mapper.manage;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.merge.controller.admin.manage.vo.MergePendingLogPageReqVO;
import tech.qiantong.qdata.mdm.merge.dal.dataobject.MergePendingMasterLogDO;
import tech.qiantong.qdata.mdm.merge.enums.MergeCodes;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

/**
 * 融合待提交结果日志 Mapper
 *
 * @author chentangwen
 * @date 2026-09-22
 */
public interface MergePendingMasterLogMapper extends BaseMapperX<MergePendingMasterLogDO> {

    /**
     * 按待合并结果编号分页查询有效操作日志，支持按编号或处理时间排序。
     * 未选择排序时仍按处理时间倒序展示，并用编号保证相同时间下的顺序稳定。
     *
     * @param pendingId 待合并结果编号
     * @param reqVO 分页参数
     * @return 操作日志分页数据
     */
    default PageResult<MergePendingMasterLogDO> selectPageByPendingId(
            Long pendingId, MergePendingLogPageReqVO reqVO) {
        LambdaQueryWrapperX<MergePendingMasterLogDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(MergePendingMasterLogDO::getPendingId, pendingId)
                .eq(MergePendingMasterLogDO::getValidFlag, MergeCodes.YES);
        boolean ascending = "ascending".equals(reqVO.getIsAsc());
        if (reqVO.getIsAsc() != null && "id".equals(reqVO.getOrderByColumn())) {
            if (ascending) {
                wrapper.orderByAsc(MergePendingMasterLogDO::getId);
            } else {
                wrapper.orderByDesc(MergePendingMasterLogDO::getId);
            }
        } else {
            if (ascending && "operate_time".equals(reqVO.getOrderByColumn())) {
                wrapper.orderByAsc(MergePendingMasterLogDO::getOperateTime);
            } else {
                wrapper.orderByDesc(MergePendingMasterLogDO::getOperateTime);
            }
            wrapper.orderByDesc(MergePendingMasterLogDO::getId);
        }
        return selectPage(reqVO, wrapper);
    }
}
