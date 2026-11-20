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

package tech.qiantong.qdata.mdm.dist.service.target.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemPageReqVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemRespVO;
import tech.qiantong.qdata.mdm.dist.controller.admin.target.vo.DistTargetSystemSaveReqVO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.target.DistTargetSystemDO;
import tech.qiantong.qdata.mdm.dist.dal.dataobject.task.DistTaskDO;
import tech.qiantong.qdata.mdm.dist.dal.mapper.target.DistTargetSystemMapper;
import tech.qiantong.qdata.mdm.dist.dal.mapper.task.DistTaskMapper;
import tech.qiantong.qdata.mdm.dist.service.target.IDistTargetSystemService;

/**
 * 分发目标系统Service业务层处理
 *
 * @author zhangyanyan
 * @date 2026-10-10
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DistTargetSystemServiceImpl extends ServiceImpl<DistTargetSystemMapper, DistTargetSystemDO> implements IDistTargetSystemService {
    @Resource
    private DistTargetSystemMapper distTargetSystemMapper;

    @Resource
    private DistTaskMapper distTaskMapper;

    /**
     * 按查询条件读取目标系统分页数据。
     *
     * @param pageReqVO 查询和分页条件
     * @return 目标系统数据分页结果
     */
    @Override
    public PageResult<DistTargetSystemDO> getDistTargetSystemPage(DistTargetSystemPageReqVO pageReqVO) {
        return distTargetSystemMapper.selectPage(pageReqVO);
    }

    /**
     * 查询分页数据，并批量补齐人员资料和关联任务数。
     *
     * @param pageReqVO 查询和分页条件
     * @return 供列表页面展示的分页结果
     */
    @Override
    public PageResult<DistTargetSystemRespVO> getDistTargetSystemPageDetail(DistTargetSystemPageReqVO pageReqVO) {
        PageResult<DistTargetSystemRespVO> page = BeanUtils.toBean(
                getDistTargetSystemPage(pageReqVO), DistTargetSystemRespVO.class);
        enrichProfiles(page.getRows(), true);
        return page;
    }

    /**
     * 查询单个目标系统，并补齐创建人和负责人资料。
     *
     * @param id 目标系统 ID
     * @return 目标系统详情；不存在时返回 null
     */
    @Override
    public DistTargetSystemRespVO getDistTargetSystemDetail(Long id) {
        DistTargetSystemRespVO detail = BeanUtils.toBean(getDistTargetSystemById(id), DistTargetSystemRespVO.class);
        if (detail != null) {
            enrichProfiles(java.util.Collections.singletonList(detail), true);
        }
        return detail;
    }

    /**
     * 批量补齐创建人和负责人的最新资料；列表额外统计关联任务数。
     * 账号不可见时保留目标系统中已有的联系人快照。
     *
     * @param rows 待补充信息的目标系统结果
     * @param includeTaskCounts 是否统计关联任务数
     */
    private void enrichProfiles(List<DistTargetSystemRespVO> rows, boolean includeTaskCounts) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(DistTargetSystemRespVO::getId).collect(Collectors.toList());
        Map<Long, DistTargetSystemRespVO> creators = distTargetSystemMapper.selectCreatorProfiles(ids).stream()
                .collect(Collectors.toMap(DistTargetSystemRespVO::getId, Function.identity()));
        Map<Long, DistTargetSystemRespVO> contacts = distTargetSystemMapper.selectResponsibleContacts(ids).stream()
                .collect(Collectors.toMap(DistTargetSystemRespVO::getId, Function.identity()));
        Map<Long, DistTargetSystemRespVO> counts = includeTaskCounts
                ? distTargetSystemMapper.selectTaskCounts(ids).stream()
                    .collect(Collectors.toMap(DistTargetSystemRespVO::getId, Function.identity()))
                : java.util.Collections.emptyMap();
        for (DistTargetSystemRespVO row : rows) {
            DistTargetSystemRespVO creator = creators.get(row.getId());
            if (creator != null) {
                if (creator.getCreateBy() != null) {
                    row.setCreateBy(creator.getCreateBy());
                }
                row.setCreatorPhone(creator.getCreatorPhone());
            }
            DistTargetSystemRespVO contact = contacts.get(row.getId());
            if (contact != null) {
                row.setContactName(contact.getContactName());
                row.setContactInfo(contact.getContactInfo());
            }
            if (includeTaskCounts) {
                DistTargetSystemRespVO count = counts.get(row.getId());
                row.setTaskCount(count == null ? 0L : count.getTaskCount());
            }
        }
    }

    /**
     * 校验目标系统信息和负责人，保存新记录。
     *
     * @param createReqVO 目标系统保存信息
     * @return 新建目标系统的 ID
     */
    @Override
    public Long createDistTargetSystem(DistTargetSystemSaveReqVO createReqVO) {
        validateAndNormalize(createReqVO, true);
        fillResponsibleContact(createReqVO);
        ensureCodeUnique(createReqVO.getCode());
        DistTargetSystemDO targetSystem = BeanUtils.toBean(createReqVO, DistTargetSystemDO.class);
        targetSystem.setId(null);
        distTargetSystemMapper.insert(targetSystem);
        return targetSystem.getId();
    }

    /**
     * 校验并更新目标系统，系统编码创建后不可修改。
     *
     * @param updateReqVO 目标系统保存信息
     * @return 受影响的记录数
     */
    @Override
    public int updateDistTargetSystem(DistTargetSystemSaveReqVO updateReqVO) {
        if (updateReqVO == null || updateReqVO.getId() == null) {
            throw new ServiceException("目标系统ID不能为空");
        }
        DistTargetSystemDO existing = distTargetSystemMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw new ServiceException("目标系统不存在或已删除");
        }
        validateAndNormalize(updateReqVO, false);
        fillResponsibleContact(updateReqVO);
        // 系统编码创建后不可变；局部状态更新不传编码时也保持原编码。
        if (updateReqVO.getCode() != null && !Objects.equals(existing.getCode(), updateReqVO.getCode())) {
            throw new ServiceException("系统编码创建后不可修改");
        }
        DistTargetSystemDO updateObj = BeanUtils.toBean(updateReqVO, DistTargetSystemDO.class);
        updateObj.setCode(null);
        return distTargetSystemMapper.updateById(updateObj);
    }

    /**
     * 按平台用户 ID 核实负责人，并填入账号中的姓名和电话。
     *
     * @param req 待保存的目标系统信息
     */
    private void fillResponsibleContact(DistTargetSystemSaveReqVO req) {
        if (req.getResponsibleUserId() == null) return;
        DistTargetSystemRespVO user = distTargetSystemMapper.selectResponsibleUserById(req.getResponsibleUserId());
        if (user == null) throw new ServiceException("负责人账号不存在或已删除");
        req.setContactName(user.getContactName());
        req.setContactInfo(user.getContactInfo());
    }

    /**
     * 校验并规范化目标系统字段；修改时允许只提交状态等局部字段。
     *
     * @param req 待校验的目标系统信息
     * @param creating 是否为新增操作
     */
    private void validateAndNormalize(DistTargetSystemSaveReqVO req, boolean creating) {
        if (req == null) throw new ServiceException("目标系统信息不能为空");
        // 新增或编辑基本信息时必须指定负责人。仅切换状态时沿用原负责人。
        boolean editingDetails = creating || req.getName() != null || req.getCode() != null
                || req.getDescription() != null || req.getContactName() != null || req.getContactInfo() != null;
        if (editingDetails && req.getResponsibleUserId() == null) {
            throw new ServiceException("请选择负责人");
        }
        if (creating || req.getName() != null) {
            String name = req.getName() == null ? "" : req.getName().trim();
            if (name.isEmpty()) throw new ServiceException("系统名称不能为空");
            if (name.length() > 128) throw new ServiceException("系统名称不能超过128个字符");
            req.setName(name);
        }
        if (creating || req.getCode() != null) {
            String code = req.getCode() == null ? "" : req.getCode().trim();
            if (code.isEmpty()) throw new ServiceException("系统编码不能为空");
            if (code.length() > 64) throw new ServiceException("系统编码不能超过64个字符");
            req.setCode(code);
        }
        // 系统类型已从登记表单移除；兼容历史客户端传入的有效类型。
        if (req.getType() != null) {
            String type = req.getType().trim();
            if (!java.util.Arrays.asList("ERP", "SRM", "CRM", "OTHER").contains(type)) {
                throw new ServiceException("请选择有效的系统类型");
            }
            req.setType(type);
        }
        if (creating || req.getStatus() != null) {
            if (!"1".equals(req.getStatus()) && !"2".equals(req.getStatus())) {
                throw new ServiceException("请选择有效的目标系统状态");
            }
        }
        if (req.getDescription() != null && req.getDescription().length() > 256) {
            throw new ServiceException("描述不能超过256个字符");
        }
    }

    /**
     * 校验系统编码是否已被未删除的目标系统占用。
     *
     * @param code 待校验的系统编码
     */
    private void ensureCodeUnique(String code) {
        Long count = distTargetSystemMapper.selectCount(new LambdaQueryWrapper<DistTargetSystemDO>()
                .eq(DistTargetSystemDO::getCode, code));
        if (count > 0) throw new ServiceException("系统编码已存在，请更换编码");
    }
    /**
     * 批量删除目标系统；启用中或被分发任务引用的记录不能删除。
     *
     * @param idList 待删除的目标系统 ID
     * @return 受影响的记录数
     */
    @Override
    public int removeDistTargetSystem(Collection<Long> idList) {
        // 批量删除前整体校验；任意一条仍启用时全部拒绝，避免部分删除。
        if (idList == null || idList.isEmpty()) {
            throw new ServiceException("请选择需要删除的目标系统");
        }
        List<DistTargetSystemDO> systems = distTargetSystemMapper.selectBatchIds(idList);
        if (systems.size() != new HashSet<>(idList).size()) {
            throw new ServiceException("目标系统不存在或已删除，请刷新列表");
        }
        for (DistTargetSystemDO system : systems) {
            if (!"2".equals(system.getStatus())) {
                throw new ServiceException("只能删除禁用状态的目标系统，请先禁用“" + system.getName() + "”");
            }
        }
        // 有未删除任务仍引用目标系统时拒绝删除，保留任务与接收方的关联。
        Long linkedTaskCount = distTaskMapper.selectCount(new LambdaQueryWrapper<DistTaskDO>()
                .in(DistTaskDO::getTargetSystemId, idList));
        if (linkedTaskCount > 0) {
            throw new ServiceException("目标系统存在关联分发任务，请先处理关联任务后再删除");
        }
        return distTargetSystemMapper.deleteBatchIds(idList);
    }

    /**
     * 根据 ID 查询目标系统原始记录。
     *
     * @param id 目标系统 ID
     * @return 目标系统记录；不存在时返回 null
     */
    @Override
    public DistTargetSystemDO getDistTargetSystemById(Long id) {
        return distTargetSystemMapper.selectById(id);
    }


}
