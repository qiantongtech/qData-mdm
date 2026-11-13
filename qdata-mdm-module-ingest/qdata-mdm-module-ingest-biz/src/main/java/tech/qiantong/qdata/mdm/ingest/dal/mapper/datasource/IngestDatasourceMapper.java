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

package tech.qiantong.qdata.mdm.ingest.dal.mapper.datasource;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;
import tech.qiantong.qdata.mdm.mybatis.config.MasterDataSourceConfig;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.MPJLambdaWrapperX;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据源Mapper接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IngestDatasourceMapper extends BaseMapperX<IngestDatasourceDO> {

    default PageResult<IngestDatasourceDO> selectPage(IngestDatasourcePageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        MPJLambdaWrapperX<IngestDatasourceDO> daDatasourceDOLambdaQueryWrapperX = new MPJLambdaWrapperX<IngestDatasourceDO>()
                .selectAll(IngestDatasourceDO.class)
                .likeIfPresent(IngestDatasourceDO::getDatasourceName, reqVO.getDatasourceName())
                .inIfPresent(IngestDatasourceDO::getDatasourceType, reqVO.getDatasourceTypeList())
                .inIfPresent(IngestDatasourceDO::getDatasourceType, StringUtils.isNotEmpty(reqVO.getDatasourceType()) ? reqVO.getDatasourceType().split(",") : null)
                .eqIfPresent(IngestDatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eqIfPresent(IngestDatasourceDO::getIp, reqVO.getIp())
                .eqIfPresent(IngestDatasourceDO::getPort, reqVO.getPort())
                .eqIfPresent(IngestDatasourceDO::getListCount, reqVO.getListCount())
                .eqIfPresent(IngestDatasourceDO::getSyncCount, reqVO.getSyncCount())
                .eqIfPresent(IngestDatasourceDO::getDataSize, reqVO.getDataSize())
                .eqIfPresent(IngestDatasourceDO::getDescription, reqVO.getDescription())
                .eqIfPresent(IngestDatasourceDO::getCreateTime, reqVO.getCreateTime())
                .inIfPresent(IngestDatasourceDO::getId, reqVO.getIdList());
       /* daDatasourceDOLambdaQueryWrapperX
                .exists(StringUtils.isNotBlank(reqVO.getProjectCode()), "SELECT rel.id FROM DA_DATASOURCE_PROJECT_REL rel WHERE rel.DATASOURCE_ID = t.id AND rel.PROJECT_CODE = '" + reqVO.getProjectCode() + "'");*/
//                .notExists(StringUtils.isBlank(reqVO.getProjectCode()), "SELECT rel.id FROM DA_DATASOURCE_PROJECT_REL rel WHERE rel.DATASOURCE_ID = t.id");
        if (org.apache.commons.lang3.StringUtils.equals("mysql", MasterDataSourceConfig.getDatabaseType())) {
            daDatasourceDOLambdaQueryWrapperX.apply(
                    StringUtils.isNotBlank(reqVO.getAllowOperate()),
                    "COALESCE(JSON_UNQUOTE((DATASOURCE_CONFIG ->>  '$.allowOperate')), {0}) = {0}",
                    reqVO.getAllowOperate()
            );
        } else if (org.apache.commons.lang3.StringUtils.equals("dm8", MasterDataSourceConfig.getDatabaseType())) {
            daDatasourceDOLambdaQueryWrapperX.apply(
                    StringUtils.isNotBlank(reqVO.getAllowOperate()),
                    "COALESCE(JSON_VALUE(DATASOURCE_CONFIG, '$.allowOperate'), {0}) = {0}",
                    reqVO.getAllowOperate()
            );
        } else if (org.apache.commons.lang3.StringUtils.equals("gbase8s", MasterDataSourceConfig.getDatabaseType())) {
            daDatasourceDOLambdaQueryWrapperX.apply(
                    StringUtils.isNotBlank(reqVO.getAllowOperate()),
                    "(DATASOURCE_CONFIG LIKE '%\"allowOperate\":\"' || {0} || '\"%' OR DATASOURCE_CONFIG LIKE '%\"allowOperate\":' || {0} || '%')",
                    reqVO.getAllowOperate()
            );
        }
        daDatasourceDOLambdaQueryWrapperX
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(DaDatasourceDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderByStr(org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getOrderByColumn()),
                        org.apache.commons.lang3.StringUtils.equals("asc", reqVO.getIsAsc()), org.apache.commons.lang3.StringUtils.isNotBlank(reqVO.getOrderByColumn()) ? Arrays.asList(reqVO.getOrderByColumn().split(","))
                                .stream().map(e -> "t." + LambdaQueryWrapperX.camelToUnderline(e))
                                .collect(Collectors.toList()) : null);
        // 构造动态查询条件
        daDatasourceDOLambdaQueryWrapperX
                .select("cu.PHONENUMBER AS createPhoneNumber")
                .leftJoin("SYSTEM_USER cu ON cu.USER_ID = t.CREATOR_ID AND cu.DEL_FLAG = '0'");
        return selectJoinPage(reqVO, IngestDatasourceDO.class, daDatasourceDOLambdaQueryWrapperX);
    }

    public List<IngestDatasourceDO> getDataSourceByAsset();

    @Select("<script>"
            + "SELECT ID AS \"id\", DATASOURCE_NAME AS \"datasourceName\", DATASOURCE_TYPE AS \"datasourceType\", "
            + "DATASOURCE_CONFIG AS \"datasourceConfig\", IP AS \"ip\", PORT AS \"port\" "
            + "FROM INGEST_DATASOURCE "
            + "WHERE DEL_FLAG = '0' AND VALID_FLAG = '1' "
            + "<if test=\"keyword != null and keyword != ''\"> AND DATASOURCE_NAME LIKE #{keyword} </if> "
            + "ORDER BY UPDATE_TIME DESC"
            + "</script>")
    List<IngestDatasourceDO> selectOptions(@Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT ID AS \"id\", DATASOURCE_NAME AS \"datasourceName\", DATASOURCE_TYPE AS \"datasourceType\", "
            + "DATASOURCE_CONFIG AS \"datasourceConfig\", IP AS \"ip\", PORT AS \"port\" "
            + "FROM INGEST_DATASOURCE "
            + "WHERE DEL_FLAG = '0' AND ID IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "</script>")
    List<IngestDatasourceDO> selectByIds(@Param("ids") Collection<Long> ids);
}
