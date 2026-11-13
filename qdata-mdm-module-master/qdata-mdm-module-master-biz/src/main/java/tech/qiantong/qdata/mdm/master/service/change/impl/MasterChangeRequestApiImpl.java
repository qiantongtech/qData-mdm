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

package tech.qiantong.qdata.mdm.master.service.change.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysUser;
import tech.qiantong.qdata.mdm.common.exception.ServiceException;
import tech.qiantong.qdata.mdm.common.utils.object.BeanUtils;
import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitReqDTO;
import tech.qiantong.qdata.mdm.master.change.dto.MasterChangeRequestSubmitRespDTO;
import tech.qiantong.qdata.mdm.master.controller.admin.change.vo.MasterChangeRequestSaveReqVO;
import tech.qiantong.qdata.mdm.master.service.change.IMasterChangeRequestService;
import tech.qiantong.qdata.mdm.master.service.change.service.MasterChangeRequestApi;
import tech.qiantong.qdata.mdm.system.service.ISysUserService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** 对外参数转换，申请校验、落库和执行任务创建复用现有Service。 */
@Service
@RequiredArgsConstructor
public class MasterChangeRequestApiImpl implements MasterChangeRequestApi {
    private final IMasterChangeRequestService requestService;
    private final ISysUserService userService;

    @Override
    public MasterChangeRequestSubmitRespDTO submit(MasterChangeRequestSubmitReqDTO request) {
        if (request == null) throw new ServiceException("申请内容不能为空");
        String mode = request.getMode() == null ? "" : request.getMode().trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("CREATE", "UPDATE").contains(mode))
            throw new ServiceException("跨模块申请仅支持CREATE或UPDATE");
        if (request.getApplicantId() == null || request.getApplicantId() <= 0)
            throw new ServiceException("申请人ID无效");
        SysUser user = userService.selectUserById(request.getApplicantId());
        if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag()))
            throw new ServiceException("申请人不存在或已停用");
        String applicantName = user.getNickName() == null || user.getNickName().trim().isEmpty()
                ? user.getUserName() : user.getNickName();
        String source = request.getChangeSource() == null || request.getChangeSource().trim().isEmpty()
                ? "MERGE" : request.getChangeSource();

        MasterChangeRequestSaveReqVO save = new MasterChangeRequestSaveReqVO();
        save.setMode(mode);
        save.setRecordId(request.getRecordId() == null ? null : request.getRecordId().toString());
        save.setParentId(request.getParentId() == null ? null : request.getParentId().toString());
        save.setApplyReason(request.getApplyReason());
        save.setValues(normalizeValues(request.getValues()));
        return BeanUtils.toBean(requestService.submitSingle(request.getInstanceId(), save,
                request.getApplicantId(), applicantName, source), MasterChangeRequestSubmitRespDTO.class);
    }

    /** 统一跨模块申请的字段键，与主数据维护元数据保持一致。 */
    private Map<String, String> normalizeValues(Map<String, String> submitted) {
        if (submitted == null) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : submitted.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.trim().isEmpty()) {
                throw new ServiceException("申请字段编码不能为空");
            }
            String code = key.toUpperCase(Locale.ROOT);
            if (values.containsKey(code) && !Objects.equals(values.get(code), entry.getValue())) {
                throw new ServiceException("申请包含大小写重复且值不一致的字段：" + code);
            }
            values.put(code, entry.getValue());
        }
        return values;
    }
}
