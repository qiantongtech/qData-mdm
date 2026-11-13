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

package tech.qiantong.qdata.mdm.ingest.service.source;

import com.baomidou.mybatisplus.extension.service.IService;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourcePageReqVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceRespVO;
import tech.qiantong.qdata.mdm.ingest.controller.admin.source.vo.IngestSourceSaveReqVO;
import tech.qiantong.qdata.mdm.ingest.dal.dataobject.source.IngestSourceDO;

import java.util.Collection;

/**
 * 来源系统
 *
 * @author chentangwen
 * @date 2026-09-15
 */
public interface IIngestSourceService extends IService<IngestSourceDO> {

    /**
     * 获得来源系统分页列表。
     *
     * @param pageReqVO 分页及筛选条件
     * @return 来源系统分页列表
     */
    PageResult<IngestSourceRespVO> getIngestSourcePage(IngestSourcePageReqVO pageReqVO);

    /**
     * 获得来源系统详情。
     *
     * @param id 来源系统编号
     * @return 来源系统详情
     */
    IngestSourceRespVO getIngestSource(Long id);

    /**
     * 创建来源系统。
     *
     * @param createReqVO 来源系统信息
     * @return 来源系统编号
     */
    Long createIngestSource(IngestSourceSaveReqVO createReqVO);

    /**
     * 更新来源系统。
     *
     * @param updateReqVO 来源系统信息
     * @return 更新行数
     */
    int updateIngestSource(IngestSourceSaveReqVO updateReqVO);

    /**
     * 删除来源系统。
     *
     * @param idList 来源系统编号集合
     * @return 删除行数
     */
    int removeIngestSource(Collection<Long> idList);
}
