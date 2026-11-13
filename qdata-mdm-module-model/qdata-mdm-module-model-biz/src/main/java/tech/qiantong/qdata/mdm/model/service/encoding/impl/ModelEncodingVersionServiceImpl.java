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

package tech.qiantong.qdata.mdm.model.service.encoding.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.StringUtils;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionPageReqVO;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingVersionSaveReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingItemDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelColumnDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.base.ModelColumnMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingItemMapper;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingVersionDO;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingVersionMapper;
import tech.qiantong.qdata.mdm.model.dal.mapper.encoding.ModelEncodingMapper;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingService;
import tech.qiantong.qdata.mdm.model.service.encoding.IModelEncodingVersionService;
import tech.qiantong.qdata.mdm.model.util.VersionNoUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

/**
 * 编码方案版本Service业务层处理
 *
 * @author qdata
 * @date 2026-09-17
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ModelEncodingVersionServiceImpl extends ServiceImpl<ModelEncodingVersionMapper, ModelEncodingVersionDO> implements IModelEncodingVersionService {
    @Resource
    private ModelEncodingVersionMapper modelEncodingVersionMapper;

    @Resource
    private ModelEncodingMapper modelEncodingMapper;

    @Resource
    private IModelEncodingService iModelEncodingService;

    @Resource
    private ModelEncodingItemMapper modelEncodingItemMapper;

    @Resource
    private ModelColumnMapper modelColumnMapper;

    @Override
    public PageResult<ModelEncodingVersionDO> getModelEncodingVersionPage(ModelEncodingVersionPageReqVO pageReqVO) {
        return modelEncodingVersionMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createModelEncodingVersion(ModelEncodingVersionSaveReqVO createReqVO) {
        ModelEncodingVersionDO dictType = BeanUtils.toBean(createReqVO, ModelEncodingVersionDO.class);
        // 在同一事务中锁定方案后再读取历史和分配版本号，串行处理同一方案的新增请求。
        if (dictType.getEncodingId() == null || modelEncodingMapper.lockScheme(dictType.getEncodingId()) == null) {
            throw new ServiceException("编码方案不存在，无法新增版本");
        }
        fillVersionInfo(dictType);
        modelEncodingVersionMapper.insert(dictType);
        refreshCurrentVersion(dictType);
        return dictType.getId();
    }

    @Override
    public int updateModelEncodingVersion(ModelEncodingVersionSaveReqVO updateReqVO) {
        // 相关校验

        // 更新编码方案版本
        ModelEncodingVersionDO updateObj = BeanUtils.toBean(updateReqVO, ModelEncodingVersionDO.class);
        return modelEncodingVersionMapper.updateById(updateObj);
    }

    /**
     * 根据版本ID更新状态。发布当前版本前，先停用同一编码方案下其他正在发布的版本；
     * 停用当前版本时，只更新当前版本。
     *
     * @param id     编码方案版本编号
     * @param status 版本状态（2停用，3发布）
     * @return 更新结果
     */
    @Override
    public int updateModelEncodingVersionStatus(Long id, Integer status) {
        ModelEncodingVersionDO modelEncodingVersionDO = modelEncodingVersionMapper.selectById(id);
        if (modelEncodingVersionDO == null) {
            throw new ServiceException("编码方案版本不存在");
        }

        String versionNo = modelEncodingVersionDO.getVersionNo();
        String remark = "历史版本";
        if (Integer.valueOf(3).equals(status)) {
            if (modelEncodingVersionDO.getEncodingId() == null) {
                throw new ServiceException("编码方案版本未关联编码方案，无法发布");
            }
            // 发布前仅停用同一编码方案下其他正在发布的版本，草稿状态保持不变。
            LambdaUpdateWrapper<ModelEncodingVersionDO> historyUpdateWrapper = new LambdaUpdateWrapper<>();
            historyUpdateWrapper.eq(ModelEncodingVersionDO::getEncodingId, modelEncodingVersionDO.getEncodingId())
                    .ne(ModelEncodingVersionDO::getId, id)
                    .eq(ModelEncodingVersionDO::getStatus, 3)
                    .set(ModelEncodingVersionDO::getStatus, 2)
                    .set(ModelEncodingVersionDO::getRemark, "历史版本");
            modelEncodingVersionMapper.update(null, historyUpdateWrapper);
            java.math.BigDecimal parsedVersion = VersionNoUtils.parse(versionNo);
            remark = parsedVersion != null && parsedVersion.compareTo(java.math.BigDecimal.ONE) == 0
                    ? "首个生效版本" : "当前生效版本";
        }

        ModelEncodingVersionDO updateObj = new ModelEncodingVersionDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setRemark(remark);
        int updateCount = modelEncodingVersionMapper.updateById(updateObj);
        if (updateCount == 0) {
            throw new ServiceException("编码方案版本状态更新失败");
        }
        return updateCount;
    }

    @Override
    public int removeModelEncodingVersion(Collection<Long> idList) {
        if (idList == null || idList.isEmpty()) return 0;
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>(idList);
        List<ModelEncodingVersionDO> versions = modelEncodingVersionMapper.selectList(
                new QueryWrapper<ModelEncodingVersionDO>().in("id", ids));
        if (versions.size() != ids.size()) throw new ServiceException("编码方案版本不存在，请刷新后重试");
        if (versions.stream().anyMatch(version -> !Integer.valueOf(1).equals(version.getStatus()))) {
            throw new ServiceException("仅草稿状态的编码方案版本允许删除");
        }
        if (modelColumnMapper.selectCount(new QueryWrapper<ModelColumnDO>().in("encoding_version_id", ids)) > 0) {
            throw new ServiceException("编码方案版本已被模型字段引用，无法删除");
        }
        // 删除时再次约束草稿状态，防止校验后版本已发布；整批不一致时回滚。
        int count = modelEncodingVersionMapper.delete(new QueryWrapper<ModelEncodingVersionDO>()
                .in("id", ids).eq("status", 1));
        if (count != ids.size()) throw new ServiceException("编码方案版本状态已变化，请刷新后重试");
        modelEncodingItemMapper.delete(new QueryWrapper<ModelEncodingItemDO>().in("encoding_version_id", ids));

        java.util.Set<Long> encodingIds = versions.stream().map(ModelEncodingVersionDO::getEncodingId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (!encodingIds.isEmpty()) {
            List<ModelEncodingVersionDO> remaining = modelEncodingVersionMapper.selectList(
                    new QueryWrapper<ModelEncodingVersionDO>().in("encoding_id", encodingIds));
            for (Long encodingId : encodingIds) {
                ModelEncodingVersionDO latest = getLatestVersion(remaining.stream()
                        .filter(version -> encodingId.equals(version.getEncodingId()))
                        .collect(java.util.stream.Collectors.toList()));
                // 仅当当前版本已被删除时回退；无剩余版本需显式写入 null，不能使用忽略 null 的 updateById。
                iModelEncodingService.update(new UpdateWrapper<ModelEncodingDO>().eq("id", encodingId)
                        .in("current_version_id", ids).set("current_version_id", latest == null ? null : latest.getId()));
            }
        }
        return count;
    }

    @Override
    public ModelEncodingVersionDO getModelEncodingVersionById(Long id) {
        return modelEncodingVersionMapper.selectById(id);
    }

    /**
     * 根据方案ID补齐新版本信息：生成下一个版本号，并将新版本设置为草稿状态。
     * 历史版本仅用于计算版本号和备注，创建新版本时不修改其状态。
     *
     * @param version 新增的编码方案版本
     */
    private void fillVersionInfo(ModelEncodingVersionDO version) {
        if (version.getEncodingId() == null) {
            return;
        }

        List<ModelEncodingVersionDO> historyList = modelEncodingVersionMapper.selectHistoryIncludingDeleted(version.getEncodingId());
        ModelEncodingVersionDO latestVersion = getLatestVersion(historyList.stream()
                .filter(history -> !Boolean.TRUE.equals(history.getDelFlag()))
                .collect(java.util.stream.Collectors.toList()));
        if (latestVersion != null && Integer.valueOf(1).equals(latestVersion.getStatus())) {
            throw new ServiceException("当前最新版本（" + latestVersion.getVersionNo() + "）仍为草稿，请先发布后再新增版本");
        }
        String latestVersionNo = latestVersion == null ? null : latestVersion.getVersionNo();

        // 删除记录仍占用唯一键，按全部历史最大版本递增；已删除的草稿不阻止新增。
        ModelEncodingVersionDO latestAllocated = getLatestVersion(historyList);
        version.setVersionNo(VersionNoUtils.next(latestAllocated == null ? null : latestAllocated.getVersionNo()));
        version.setStatus(1);
        if (StringUtils.isBlank(version.getRemark())) {
            version.setRemark(StringUtils.isBlank(latestVersionNo) ? "初始版本" : "由 " + latestVersionNo + " 创建新版本");
        }
    }

    /**
     * 从历史版本列表中找出版本号最大的版本，用于生成新版本号和备注。
     *
     * @param historyList 历史版本列表
     * @return 版本号最大的历史版本
     */
    private ModelEncodingVersionDO getLatestVersion(List<ModelEncodingVersionDO> historyList) {
        if (historyList == null || historyList.isEmpty()) {
            return null;
        }
        ModelEncodingVersionDO latestVersion = null;
        java.math.BigDecimal latestVersionNo = null;
        for (ModelEncodingVersionDO history : historyList) {
            java.math.BigDecimal versionNo = VersionNoUtils.parse(history.getVersionNo());
            if (versionNo != null && (latestVersionNo == null || versionNo.compareTo(latestVersionNo) > 0)) {
                latestVersionNo = versionNo;
                latestVersion = history;
            }
        }
        return latestVersion;
    }

    /**
     * 新版本创建成功后，同步编码方案表的当前版本ID。
     *
     * @param version 新增成功的编码方案版本
     */
    private void refreshCurrentVersion(ModelEncodingVersionDO version) {
        if (version.getEncodingId() == null) {
            return;
        }
        ModelEncodingDO modelEncoding = new ModelEncodingDO();
        modelEncoding.setId(version.getEncodingId());
        modelEncoding.setCurrentVersionId(version.getId());
        iModelEncodingService.updateById(modelEncoding);
    }
}
