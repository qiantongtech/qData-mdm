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

package tech.qiantong.qdata.mdm.master.dal.mapper.data;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import tech.qiantong.qdata.mdm.master.controller.admin.data.vo.MasterDataVersionPageReqVO;
import tech.qiantong.qdata.mdm.master.dal.dataobject.data.MasterDataVersionDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.List;

/**
 * MASTER_DATA_VERSION 正式主数据版本快照 Mapper。
 */
public interface MasterDataVersionMapper extends BaseMapperX<MasterDataVersionDO> {

    /**
     * 分页查询指定主记录的有效版本；关联有效变更历史及申请单，缺失关联时仍保留版本行。
     * 默认按版本序号和 ID 倒序，XML 仅开放 ID 自定义排序。
     *
     * @param page 分页参数
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @param reqVO 分页及排序参数，排序字段为空时采用默认排序
     * @return 版本分页及展示所需的变更类型、操作人、申请单号等列
     */
    IPage<MasterDataVersionDO> selectVersionPage(Page<MasterDataVersionDO> page,
                                               @Param("modelId") Long modelId,
                                               @Param("masterRecordId") Long masterRecordId,
                                               @Param("req") MasterDataVersionPageReqVO reqVO);

    /**
     * 查询单条主数据的全部当前有效、未删除版本 ID，不在 SQL 中截断结果。
     *
     * @param modelId 主数据模型 ID
     * @param masterRecordId 权威主数据记录 ID
     * @return 匹配的版本 ID；业务层负责判断无版本或重复当前版本
     */
    List<Long> selectCurrentVersionIds(@Param("modelId") Long modelId,
                                       @Param("masterRecordId") Long masterRecordId);

    Integer selectMaxVersionSeq(@Param("modelId") Long modelId,
                                @Param("masterRecordId") Long masterRecordId);

    /**
     * 批量查询指定记录的当前有效、未删除版本号，按生成时间及 ID 降序。
     *
     * @param modelId 主数据模型 ID
     * @param recordIds 本批主数据记录 ID，调用方控制批次大小
     * @return 记录 ID 与展示版本号的行列表，供业务层检查重复当前版本
     */
    List<MasterDataVersionDO> selectCurrentVersionNumbers(@Param("modelId") Long modelId,
                                                        @Param("recordIds") List<Long> recordIds);

    /** 按模型、主记录和版本 ID 查询有效快照，不加载其他版本的大字段。 */
    MasterDataVersionDO selectSnapshot(@Param("modelId") Long modelId,
                                       @Param("masterRecordId") Long masterRecordId,
                                       @Param("versionId") Long versionId);
}
