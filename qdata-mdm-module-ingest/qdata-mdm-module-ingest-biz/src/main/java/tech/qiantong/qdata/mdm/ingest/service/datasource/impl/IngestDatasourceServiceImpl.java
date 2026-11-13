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

package tech.qiantong.qdata.mdm.ingest.service.datasource.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import tech.qiantong.qdata.mdm.common.core.domain.AjaxResult;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.DataSourceFactory;
import tech.qiantong.qdata.mdm.common.database.DbQuery;
import tech.qiantong.qdata.mdm.common.database.constants.DbQueryProperty;
import tech.qiantong.qdata.mdm.common.database.core.DbTable;
import tech.qiantong.qdata.mdm.common.database.exception.DataQueryException;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.datasource.vo.IngestDatasourceSimpleVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.datasource.IngestDatasourceDO;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.datasource.IngestDatasourceMapper;
import tech.qiantong.qdata.mdm.ingest.dal.mapper.source.IngestSourceDatasourceRelMapper;
import tech.qiantong.qdata.mdm.ingest.service.datasource.IIngestDatasourceService;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;
import tech.qiantong.qdata.mdm.redis.service.IRedisService;
import tech.qiantong.qdata.mdm.system.service.ISysMessageService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据源Service业务层处理
 *
 * @author lhs
 * @date 2025-01-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class IngestDatasourceServiceImpl extends ServiceImpl<IngestDatasourceMapper, IngestDatasourceDO> implements IIngestDatasourceService {
    @Value("${qdata.datasource.cache.init:true}")
    private boolean cacheInitEnabled;

    @Resource
    private IngestDatasourceMapper daDatasourceMapper;

    @Resource
    private IngestSourceDatasourceRelMapper sourceDatasourceRelMapper;

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Autowired
    private ISysMessageService iSysMessageService;

    @Autowired
    private IRedisService redisService;

    /**
     * 项目启动后初始化 Redis 中的数据源缓存。
     * <p>
     * 功能：
     * 1. 从数据库中加载所有数据源记录；
     * 2. 使用 DaDatasourceDO.simplify() 方法提取关键字段；
     * 3. 将结果写入 Redis Hash：
     * <p>
     * 场景：
     * - 项目启动后预热数据源缓存，任务调度和 Worker 节点可直接从 Redis 获取配置信息；
     * - 避免运行时对数据库或中台服务的强依赖。
     * <p>
     * 注意：
     * - 可选：初始化前可清空 Redis 中已有的 "datasource" 缓存。
     */
    @PostConstruct
    public void initDatasourceCache() {
        if (!cacheInitEnabled) {
            log.info("【数据源缓存初始化】配置已禁用，跳过初始化。");
            return;
        }
        try {
            List<IngestDatasourceDO> list = daDatasourceMapper.selectList();
            if (list == null || list.isEmpty()) {
                log.info("【数据源缓存初始化】无数据源记录，跳过初始化。");
                return;
            }

            // 可选：初始化前清空缓存
            // redisService.del("datasource");

            for (IngestDatasourceDO ds : list) {
                if (ds == null || ds.getId() == null) {
                    continue;
                }

                // 最新数据源连接信息
                String field = String.valueOf(ds.getId());
                String value = com.alibaba.fastjson2.JSONObject.toJSONString(ds.simplify());
                redisService.hashPut("datasource", field, value);

                try {
                    DbQueryProperty property = new DbQueryProperty(ds.getDatasourceType(), ds.getIp(), ds.getPort(), ds.getDatasourceConfig());
                    String key = property.trainToJdbcUrl();

                    // 判断是否已存在
                    Boolean exists = redisService.hashHasKey("datasource-old", key);
                    if (Boolean.FALSE.equals(exists)) {
                        redisService.hashPut("datasource-old", key, ds.getId().toString());
                        log.info("存入新历史数据源: key={}, value={}", key, ds.getId().toString());
                    } else {
                        log.info("已存在历史数据源: key={}，跳过存入", key);
                    }
                } catch (Exception e) {
                    log.warn("不支持转化的数据源");
                }
            }

            log.info("【数据源缓存初始化】成功加载 {} 条数据源到 Redis。", list.size());
        } catch (Exception e) {
            log.error("【数据源缓存初始化】加载 Redis 缓存失败：", e);
        }
    }

    @Override
    public PageResult<IngestDatasourceDO> getDaDatasourcePage(IngestDatasourcePageReqVO pageReqVO) {
        return daDatasourceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<IngestDatasourceDO> getDaDatasourceList(IngestDatasourcePageReqVO reqVO) {
        LambdaQueryWrapperX<IngestDatasourceDO> daDatasourceDOLambdaQueryWrapperX = new LambdaQueryWrapperX<>();
        daDatasourceDOLambdaQueryWrapperX.likeIfPresent(IngestDatasourceDO::getDatasourceName, reqVO.getDatasourceName())
                .in(CollectionUtils.isNotEmpty(reqVO.getDatasourceTypeList()), IngestDatasourceDO::getDatasourceType, reqVO.getDatasourceTypeList())
                .notIn(CollectionUtils.isNotEmpty(reqVO.getDatasourceNotTypeList()), IngestDatasourceDO::getDatasourceType, reqVO.getDatasourceNotTypeList())
                .like(StringUtils.isNotEmpty(reqVO.getDatasourceType()), IngestDatasourceDO::getDatasourceType, reqVO.getDatasourceType())
                .eq(StringUtils.isNotEmpty(reqVO.getDatasourceConfig()), IngestDatasourceDO::getDatasourceConfig, reqVO.getDatasourceConfig())
                .eq(StringUtils.isNotEmpty(reqVO.getIp()), IngestDatasourceDO::getIp, reqVO.getIp());

        List<IngestDatasourceDO> daDatasourceDOS = daDatasourceMapper.selectList(daDatasourceDOLambdaQueryWrapperX);
        return daDatasourceDOS;
    }

    @Override
    public Long createDaDatasource(IngestDatasourceSaveReqVO createReqVO) {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(createReqVO.getDatasourceType(), createReqVO.getIp(), createReqVO.getPort(), createReqVO.getDatasourceConfig());

        //判断是否为开启,并且连接测试未通过
        if (createReqVO.getValidFlag() && !clientTest(dbQueryProperty)) {
            throw new ServiceException("连接测试未通过，暂无法启用。请检查连接配置后重试。", 10001);
        }
        IngestDatasourceDO dictType = BeanUtils.toBean(createReqVO, IngestDatasourceDO.class);
        daDatasourceMapper.insert(dictType);
       // delAndSaveDaDataSourceProject(dictType);
        redisService.hashPut("datasource", dictType.getId()
                .toString(), com.alibaba.fastjson2.JSONObject.toJSONString(this.getDaDatasourceById(dictType.getId())
                .simplify()));

        return dictType.getId();
    }

    @Override
    public int updateDaDatasource(IngestDatasourceSaveReqVO updateReqVO) {
        Long datasourceId = updateReqVO.getId();
        DbQueryProperty dbQueryProperty = new DbQueryProperty(updateReqVO.getDatasourceType(), updateReqVO.getIp(), updateReqVO.getPort(), updateReqVO.getDatasourceConfig());
        //判断是否为开启,并且连接测试未通过
        if (updateReqVO.getValidFlag() && !clientTest(dbQueryProperty)) {
            throw new ServiceException("连接测试未通过，暂无法启用。请检查连接配置后重试。", 10001);
        }
        // 更新数据源
        IngestDatasourceDO updateObj = BeanUtils.toBean(updateReqVO, IngestDatasourceDO.class);
        //delAndSaveDaDataSourceProject(updateObj);
        int i = daDatasourceMapper.updateById(updateObj);
        redisService.hashPut("datasource", datasourceId.toString(), com.alibaba.fastjson2.JSONObject.toJSONString(this.getDaDatasourceById(datasourceId)
                .simplify()));
        return i;
    }

    @Override
    public int removeDaDatasource(Collection<Long> idList) {
        // 批量删除数据源
        return daDatasourceMapper.deleteBatchIds(idList);
    }

    @Override
    public IngestDatasourceDO getDaDatasourceById(Long id) {
        IngestDatasourceDO daDatasourceDO = daDatasourceMapper.selectById(id);
       /* if (daDatasourceDO == null) {
            return null;
        }
        DaDatasourceProjectRelDO daDatasourceProjectRelDO = new DaDatasourceProjectRelDO();
        daDatasourceProjectRelDO.setDatasourceId(daDatasourceDO.getId());*/
       /* List<DaDatasourceProjectRelDO> daDatasourceProjectRelList = daDatasourceProjectRelService.getJoinProjectAndDatasource(daDatasourceProjectRelDO);
        daDatasourceDO.setProjectList(daDatasourceProjectRelList);*/
        return daDatasourceDO;
    }

    @Override
    public IngestDatasourceRespVO getDaDatasourceByIdSimple(Long id) {
        return null;
    }

    @Override
    public List<IngestDatasourceDO> getDaDatasourceList() {
        return null;
    }

    @Override
    public Map<Long, IngestDatasourceDO> getDaDatasourceMap() {
        return null;
    }

    @Override
    public AjaxResult clientsTest(Long id) {
        DbQuery dbQuery = this.buildDbQuery(id);
        if (dbQuery.valid()) {
            dbQuery.close();
            return AjaxResult.success("数据库连接成功");
        }
        dbQuery.close();
        return AjaxResult.error("数据库连接失败");
    }

    public DbQuery buildDbQuery(Long id) {
        IngestDatasourceDO daDatasourceBy = this.getDaDatasourceById(id);
        if (daDatasourceBy == null) {
            throw new DataQueryException("数据源详情信息查询失败");
        }
        DbQueryProperty dbQueryProperty = new DbQueryProperty(daDatasourceBy.getDatasourceType(), daDatasourceBy.getIp(), daDatasourceBy.getPort(), daDatasourceBy.getDatasourceConfig());
        return dataSourceFactory.createDbQuery(dbQueryProperty);
    }
    @Override
    public Boolean clientTest(DbQueryProperty dbQueryProperty) {

        DbQuery dbQuery = null;

        try {
            dbQuery = this.dataSourceFactory.createDbQuery(dbQueryProperty);
            if (!dbQuery.valid()) {
                return false;
            }
        } catch (DataQueryException exception) {
            return false;
        } finally {
            if (dbQuery != null) {
                dbQuery.close();
            }
        }
        return true;
    }

    @Override
    public Boolean clientTest(Long id) {
        DbQuery dbQuery = null;
        try {
            dbQuery = this.buildDbQuery(id);
            if (!dbQuery.valid()) {
                return false;
            }
        } catch (DataQueryException exception) {
            return false;
        } finally {
            if (dbQuery != null) {
                dbQuery.close();
            }
        }
        return true;
    }

    @Override
    public AjaxResult clientsTest(IngestDatasourceSaveReqVO daDatasource) {
        DbQueryProperty dbQueryProperty = new DbQueryProperty(daDatasource.getDatasourceType(), daDatasource.getIp(), daDatasource.getPort(), daDatasource.getDatasourceConfig());
        DbQuery dbQuery = dataSourceFactory.createDbQuery(dbQueryProperty);

        if (dbQuery.valid()) {
            dbQuery.close();
            return AjaxResult.success("数据库连接成功");
        }
        dbQuery.close();
        return AjaxResult.error("数据库连接失败");
    }

    @Override
    public Boolean editDatasourceStatus(Long datasourceId, Long status) {
        if (status == 1 && !clientTest(datasourceId)) {
            throw new ServiceException("连接测试未通过，暂无法启用。请检查连接配置后重试。", 10001);
        }
        return this.update(Wrappers.lambdaUpdate(IngestDatasourceDO.class)
                .eq(IngestDatasourceDO::getId, datasourceId)
                .set(IngestDatasourceDO::getValidFlag, status));
    }

    @Override
    public List<IngestDatasourceSimpleVO> getDatasourceOptions(String keyword) {
        String query = StringUtils.isEmpty(keyword) ? null : "%" + keyword.trim() + "%";
        return BeanUtils.toBean(daDatasourceMapper.selectOptions(query), IngestDatasourceSimpleVO.class);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<DbTable> getDbTables(Long sourceId, Long id, String tableName, Long limit) {
        if (sourceId == null || id == null) {
            throw new ServiceException("请选择来源系统和数据库连接");
        }
        boolean linked = sourceDatasourceRelMapper.selectBySourceIds(Collections.singletonList(sourceId)).stream()
                .anyMatch(rel -> id.equals(rel.getDatasourceId()));
        if (!linked) {
            throw new ServiceException("数据库连接未关联当前来源系统");
        }
        IngestDatasourceDO datasource = this.getDaDatasourceById(id);
        if (datasource == null) {
            throw new ServiceException("数据源不存在");
        }
        DbQueryProperty property = new DbQueryProperty(datasource.getDatasourceType(), datasource.getIp(),
                datasource.getPort(), datasource.getDatasourceConfig());
        DbQuery dbQuery = null;
        try {
            dbQuery = dataSourceFactory.createDbQuery(property);
            if (dbQuery == null || !dbQuery.valid()) {
                throw new ServiceException("数据库连接失败");
            }
            List<DbTable> tables = dbQuery.getTables(property);
            if (tables == null) {
                return Collections.emptyList();
            }
            if (StringUtils.isNotEmpty(tableName)) {
                String keyword = tableName.trim();
                tables = tables.stream()
                        .filter(table -> StringUtils.containsIgnoreCase(table.getTableName(), keyword)
                                || StringUtils.containsIgnoreCase(table.getTableComment(), keyword))
                        .collect(Collectors.toList());
            }
            if (limit != null && limit >= 0) {
                tables = tables.stream().limit(limit).collect(Collectors.toList());
            }
            return tables;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("读取数据源表失败, datasourceId={}", id, ex);
            throw new ServiceException("读取数据库表失败，请检查连接配置");
        } finally {
            if (dbQuery != null) {
                dbQuery.close();
            }
        }
    }


}
