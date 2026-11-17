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
 * Business License: https://docs.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.dist.dal.mapper.service;

import tech.qiantong.qdata.mdm.dist.dal.dataobject.service.DistServiceCallLogDO;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceRespVO;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.dist.controller.admin.service.vo.DistServiceCallLogPageReqVO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 数据服务调用日志Mapper接口
 *
 * @author chentangwen
 * @date 2026-10-21
 */
public interface DistServiceCallLogMapper extends BaseMapperX<DistServiceCallLogDO> {

    /**
     * 批量汇总指定时间段内的有效调用次数及历史最近调用时间；服务 ID 集合须非空。
     */
    @Select({
            "<script>",
            "SELECT SERVICE_ID AS id,",
            "SUM(CASE WHEN CALL_TIME &gt;= #{startTime} AND CALL_TIME &lt;= #{endTime} THEN 1 ELSE 0 END) AS callCount,",
            "MAX(CALL_TIME) AS lastCallTime",
            "FROM DIST_SERVICE_CALL_LOG",
            "WHERE SERVICE_ID IN",
            "<foreach collection='serviceIds' item='serviceId' open='(' separator=',' close=')'>",
            "#{serviceId}",
            "</foreach>",
            "AND VALID_FLAG = '1' AND DEL_FLAG = '0'",
            "GROUP BY SERVICE_ID",
            "</script>"
    })
    List<DistServiceRespVO> selectStatsByServiceIds(@Param("serviceIds") Collection<Long> serviceIds,
                                                     @Param("startTime") Date startTime,
                                                     @Param("endTime") Date endTime);

    /**
     * 按服务 ID 筛选日志并按调用时间倒序分页，其余请求字段不参与筛选。
     */
    default PageResult<DistServiceCallLogDO> selectPage(DistServiceCallLogPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DistServiceCallLogDO>()
                .eq(DistServiceCallLogDO::getServiceId, reqVO.getServiceId())
                .orderByDesc(DistServiceCallLogDO::getCallTime));
    }
}
