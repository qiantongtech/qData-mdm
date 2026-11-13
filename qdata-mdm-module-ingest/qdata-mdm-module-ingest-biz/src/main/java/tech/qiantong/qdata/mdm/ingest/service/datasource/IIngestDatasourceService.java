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

package tech.qiantong.qdata.mdm.ingest.service.datasource;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSimpleVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据源Service接口
 *
 * @author lhs
 * @date 2025-01-21
 */
public interface IIngestDatasourceService extends IService<IngestDatasourceDO> {

    /**
     * 获得数据源分页列表
     *
     * @param pageReqVO 分页请求
     * @return 数据源分页列表
     */
    PageResult<IngestDatasourceDO> getDaDatasourcePage(IngestDatasourcePageReqVO pageReqVO);

    List<IngestDatasourceDO> getDaDatasourceList(IngestDatasourcePageReqVO pageReqVO);

    /**
     * 创建数据源
     *
     * @param createReqVO 数据源信息
     * @return 数据源编号
     */
    Long createDaDatasource(IngestDatasourceSaveReqVO createReqVO);

    /**
     * 更新数据源
     *
     * @param updateReqVO 数据源信息
     */
    int updateDaDatasource(IngestDatasourceSaveReqVO updateReqVO);

    /**
     * 删除数据源
     *
     * @param idList 数据源编号
     */
    int removeDaDatasource(Collection<Long> idList);


    /**
     * 获得数据源详情
     *
     * @param id 数据源编号
     * @return 数据源
     */
    IngestDatasourceDO getDaDatasourceById(Long id);

    IngestDatasourceRespVO getDaDatasourceByIdSimple(Long id);

    /**
     * 获得全部数据源列表
     *
     * @return 数据源列表
     */
    List<IngestDatasourceDO> getDaDatasourceList();

    /**
     * 获得全部数据源 Map
     *
     * @return 数据源 Map
     */
    Map<Long, IngestDatasourceDO> getDaDatasourceMap();

    AjaxResult clientsTest(Long id);
    /**
     * 测试连接
     * @param dbQueryProperty
     * @return
     */
    Boolean clientTest(DbQueryProperty dbQueryProperty);
    /**
     * 测试连接
     * @param id
     * @return
     */
    Boolean clientTest(Long id);

    AjaxResult clientsTest(IngestDatasourceSaveReqVO daDatasource);

    Boolean editDatasourceStatus(Long datasourceId, Long status);

    /**
     * 查询数据连接下拉列表。
     *
     * @param keyword 名称关键字
     * @return 数据连接列表
     */
    List<IngestDatasourceSimpleVO> getDatasourceOptions(String keyword);

    /**
     * 读取数据源中的数据表。
     *
     * @param sourceId 来源系统编号
     * @param id 数据源编号
     * @param tableName 表名或注释关键字，空则不过滤
     * @param limit 返回条数，小于 0 表示不限制
     * @return 数据表列表
     */
    List<DbTable> getDbTables(Long sourceId, Long id, String tableName, Long limit);

}
