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

package tech.qiantong.qdata.mdm.model.dal.mapper.base;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 发布准备专用原始配置读取；保留状态及完整配置，不使用会过滤历史字段的常规详情查询。
 */
@Mapper
public interface ModelPublishConfigMapper {

    /**
     * 不过滤删除或停用记录，检查实例是否曾写入正式物化结构或被模型成功指针引用；存在即禁止补偿删除。
     */
    Long selectPublishedBindingCount(@Param("instanceId") Long instanceId);

    /**
     * 查询根模型及全部直接子模型，包含逻辑删除配置以保留显式状态证据。
     *
     * @param modelId 根模型ID
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectNodes(@Param("modelId") Long modelId);

    /**
     * 查询完整字段配置，包含已逻辑删除字段及当前DO尚未映射的展示配置。
     *
     * @param ids 模型节点ID集合，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectFields(@Param("ids") Collection<Long> ids);

    /**
     * 查询字段引用的参考数据集合原始配置。
     *
     * @param ids 参考数据集合ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectReferenceTypes(@Param("ids") Collection<Long> ids);

    /**
     * 查询参考数据集合全部数据项，保留停用及删除状态。
     *
     * @param ids 参考数据集合ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectReferenceItems(@Param("ids") Collection<Long> ids);

    /**
     * 查询引用的编码方案版本完整配置。
     *
     * @param ids 编码版本ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectEncodingVersions(@Param("ids") Collection<Long> ids);

    /**
     * 查询编码方案主体配置。
     *
     * @param ids 编码方案ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectEncodings(@Param("ids") Collection<Long> ids);

    /**
     * 查询编码方案版本全部组成项配置。
     *
     * @param ids 编码版本ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectEncodingItems(@Param("ids") Collection<Long> ids);

    /**
     * 查询上次成功实例全部真实表记录，不以启用状态过滤历史对象。
     *
     * @param instanceId 成功实例ID
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectMaterializedTables(@Param("instanceId") Long instanceId);

    /**
     * 查询成功物化表全部真实列记录，不以启用状态过滤停用字段。
     *
     * @param ids 物化表记录ID，不得为空
     * @return 原始配置记录
     */
    List<Map<String, Object>> selectMaterializedColumns(@Param("ids") Collection<Long> ids);
    /**
     * 检查直接子模型是否还有正常使用的下级模型，避免只读取两层而静默遗漏配置。
     *
     * @param ids 直接子模型ID，不得为空
     * @return 仍正常使用的下级模型数量
     */
    Long countActiveDescendants(@Param("ids") Collection<Long> ids);
    /**
     * 查出已使用本批编码的全部模型，包含逻辑删除记录，避免复用旧模型的物理表名。
     *
     * @param codes 本次模型编码，不得为空
     * @return 占用编码的模型ID及编码
     */
    List<Map<String, Object>> selectModelCodeOwners(@Param("codes") Collection<String> codes);
    /** 按ID只读查询数据源配置；凭证仅用于服务端连接，不写入发布JSON。 */
    List<Map<String, Object>> selectPublishDatasource(@Param("id") Long id);
}
