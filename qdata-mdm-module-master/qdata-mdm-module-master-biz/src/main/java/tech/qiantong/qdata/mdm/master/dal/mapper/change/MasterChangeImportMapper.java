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

package tech.qiantong.qdata.mdm.master.dal.mapper.change;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.master.dal.dataobject.change.MasterChangeImportDO;

public interface MasterChangeImportMapper extends BaseMapperX<MasterChangeImportDO> {
    /** 申请与导入信息一对一；仅在校验本人申请归属后调用。 */
    @Select("SELECT REQUEST_ID, FILE_NAME, IMPORT_MODE, TEMPLATE_VERSION_NO, TOTAL_COUNT "
            + "FROM MASTER_CHANGE_IMPORT WHERE REQUEST_ID = #{requestId} "
            + "AND VALID_FLAG = '1' AND DEL_FLAG = '0'")
    @Results({
            @Result(column = "REQUEST_ID", property = "requestId"),
            @Result(column = "FILE_NAME", property = "fileName"),
            @Result(column = "IMPORT_MODE", property = "importMode"),
            @Result(column = "TEMPLATE_VERSION_NO", property = "templateVersionNo"),
            @Result(column = "TOTAL_COUNT", property = "totalCount")
    })
    MasterChangeImportDO selectByRequestId(@Param("requestId") Long requestId);
}
