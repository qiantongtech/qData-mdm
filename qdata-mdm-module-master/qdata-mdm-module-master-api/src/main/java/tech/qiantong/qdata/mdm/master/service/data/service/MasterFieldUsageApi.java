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

package tech.qiantong.qdata.mdm.master.service.data.service;

import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterDictUsageRespDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageReqDTO;
import tech.qiantong.qdata.mdm.master.data.dto.MasterEncodingUsageRespDTO;

import java.util.List;

/** 主数据字段使用统计；其他模块只需依赖 master-api 并注入本接口。 */
public interface MasterFieldUsageApi {

    /**
     * 按最近成功发布配置，批量统计参考数据项在权威主表中的使用条数。
     * 排除逻辑删除记录，包含全部生命周期状态；查询失败静默返回0。
     *
     * @param requests 模型、字段、参考数据类型及数据项编码，最多100项请求、合计500个编码
     * @return 各数据项使用条数，无匹配数据时为0；重复条件合并，空请求返回空列表
     */
    List<MasterDictUsageRespDTO> countDictUsageBatch(List<MasterDictUsageReqDTO> requests);

    /**
     * 按成功发布配置，批量统计引用指定编码版本字段的非空编码记录数，不追溯历史生成版本。
     * 排除逻辑删除记录，包含全部生命周期状态；查询失败静默返回0。
     *
     * @param requests 模型、字段及编码方案版本，最多100项请求
     * @return 各引用字段使用条数；重复条件合并，空请求返回空列表
     */
    List<MasterEncodingUsageRespDTO> countEncodingUsageBatch(List<MasterEncodingUsageReqDTO> requests);
}
