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

package tech.qiantong.qdata.mdm.system.service.monitor.impl;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.database.utils.AesEncryptUtil;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.httpClient.JsonUtil;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerPageReqVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerRespVO;
import tech.qiantong.qdata.mdm.system.controller.admin.monitor.vo.ContainerServerSaveReqVO;
import tech.qiantong.qdata.mdm.system.dal.dataobject.monitor.ContainerServerDO;
import tech.qiantong.qdata.mdm.system.dal.mapper.monitor.ContainerServerMapper;
import tech.qiantong.qdata.mdm.system.service.monitor.IContainerServerService;

/**
 * 容器管理-服务器配置Service业务层处理
 *
 * @author CHAOS
 * @date 2025-09-02
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ContainerServerServiceImpl extends ServiceImpl<ContainerServerMapper, ContainerServerDO> implements IContainerServerService {
    @Resource
    private ContainerServerMapper containerServerMapper;

    @Override
    public PageResult<ContainerServerDO> getContainerServerPage(ContainerServerPageReqVO pageReqVO) {
        return containerServerMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createContainerServer(ContainerServerSaveReqVO createReqVO) {
        ContainerServerDO dictType = BeanUtils.toBean(createReqVO, ContainerServerDO.class);
        containerServerMapper.insert(dictType);
        return dictType.getId();
    }

    @Override
    public int updateContainerServer(ContainerServerSaveReqVO updateReqVO) {
        // 相关校验

        // 更新容器管理-服务器配置
        ContainerServerDO updateObj = BeanUtils.toBean(updateReqVO, ContainerServerDO.class);
        return containerServerMapper.updateById(updateObj);
    }

    @Override
    public int removeContainerServer(Collection<Long> idList) {
        // 批量删除容器管理-服务器配置
        return containerServerMapper.deleteBatchIds(idList);
    }

    @Override
    public ContainerServerRespVO getContainerServerById(Long id, boolean all) {
        ContainerServerDO containerServerDO = containerServerMapper.selectById(id);
        ContainerServerRespVO res = BeanUtils.toBean(containerServerDO, ContainerServerRespVO.class);
        String password;
        try {
            password = AesEncryptUtil.desEncrypt(containerServerDO.getSshPassword());
            if (password == null) {
                throw new ServiceException("服务器配置错误了");
            }
        } catch (Exception e) {
            log.error("服务器密码解密失败", e);
            throw new ServiceException("服务器配置错误");
        }
        Session session = JschUtil.getSession(containerServerDO.getSshHost(), containerServerDO.getSshPort(),
                containerServerDO.getUserName(), password);
        String command = "docker ps --format json";
        if (all) {
            command += " -a";
        }
        ByteArrayOutputStream errStream = new ByteArrayOutputStream();
        String text = JschUtil.exec(session, command, StandardCharsets.UTF_8, errStream);
        if (errStream.size() != 0) {
            try {
                String err = errStream.toString(StandardCharsets.UTF_8.displayName());
                log.warn(err);
            } catch (UnsupportedEncodingException e) {
                log.error("解析错误流失败", e);
            }
        }
        if ("".equals(text)) {
            command = "sudo " + command;
            text = JschUtil.exec(session, command, StandardCharsets.UTF_8);
        }
        String[] split = text.split("\n");
        if (split.length == 1) {
            log.info(text);
            return res;
        }
        List<Map<String, Object>> containList = Arrays.stream(split)
                .map(jsonString -> {
                    Map<String, Object> stringObjectMap = JsonUtil.parseJsonToMap(jsonString);
                    stringObjectMap.remove("Labels");
                    return stringObjectMap;
                }).collect(Collectors.toList());
        res.setContainerList(containList);
        return res;
    }

    @Override
    public List<ContainerServerDO> getContainerServerList() {
        return containerServerMapper.selectList();
    }

    @Override
    public Map<Long, ContainerServerDO> getContainerServerMap() {
        List<ContainerServerDO> containerServerList = containerServerMapper.selectList();
        return containerServerList.stream()
                .collect(Collectors.toMap(
                        ContainerServerDO::getId,
                        containerServerDO -> containerServerDO,
                        // 保留已存在的值
                        (existing, replacement) -> existing
                ));
    }

}
