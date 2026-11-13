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

package tech.qiantong.qdata.mdm.system.ca.service.impl;

import java.util.List;

import tech.qiantong.qdata.mdm.system.ca.domain.CaCert;
import tech.qiantong.qdata.mdm.system.ca.mapper.CaCertMapper;
import tech.qiantong.qdata.mdm.system.ca.service.ICaCertService;
import tech.qiantong.qdata.mdm.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 证书管理Service业务层处理
 *
 * @author qdata
 * @date 2024-08-18
 */
@Service
public class CaCertServiceImpl implements ICaCertService
{
    @Autowired
    private CaCertMapper caCertMapper;

    /**
     * 查询证书管理
     *
     * @param id 证书管理主键
     * @return 证书管理
     */
    @Override
    public CaCert selectCaCertById(Long id)
    {
        return caCertMapper.selectCaCertById(id);
    }

    /**
     * 查询证书管理列表
     *
     * @param caCert 证书管理
     * @return 证书管理
     */
    @Override
    public List<CaCert> selectCaCertList(CaCert caCert)
    {
        return caCertMapper.selectCaCertList(caCert);
    }

    /**
     * 新增证书管理
     *
     * @param caCert 证书管理
     * @return 结果
     */
    @Override
    public int insertCaCert(CaCert caCert)
    {
        caCert.setCreateTime(DateUtils.getNowDate());
        return caCertMapper.insertCaCert(caCert);
    }

    /**
     * 修改证书管理
     *
     * @param caCert 证书管理
     * @return 结果
     */
    @Override
    public int updateCaCert(CaCert caCert)
    {
        caCert.setUpdateTime(DateUtils.getNowDate());
        return caCertMapper.updateCaCert(caCert);
    }

    /**
     * 批量删除证书管理
     *
     * @param ids 需要删除的证书管理主键
     * @return 结果
     */
    @Override
    public int deleteCaCertByIds(Long[] ids)
    {
        return caCertMapper.deleteCaCertByIds(ids);
    }

    /**
     * 删除证书管理信息
     *
     * @param id 证书管理主键
     * @return 结果
     */
    @Override
    public int deleteCaCertById(Long id)
    {
        return caCertMapper.deleteCaCertById(id);
    }
}
