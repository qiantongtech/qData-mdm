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

package tech.qiantong.qdata.mdm.model.dal.mapper.encoding;

import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.controller.admin.encoding.vo.ModelEncodingPageReqVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.encoding.ModelEncodingSequenceDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;
import tech.qiantong.qdata.mdm.mybatis.core.query.LambdaQueryWrapperX;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 编码方案Mapper接口
 *
 * @author qdata
 * @date 2026-09-16
 */
public interface ModelEncodingMapper extends BaseMapperX<ModelEncodingDO> {

    /**
     * 查询可供字段配置选择的编码方案：当前版本必须为已发布状态(3)，且至少存在一个编码规则段。
     * 查询条件与普通列表保持一致，使用 EXISTS 保证分页总数和结果集准确。
     */
    default PageResult<ModelEncodingDO> selectAvailablePage(ModelEncodingPageReqVO reqVO) {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));
        LambdaQueryWrapperX<ModelEncodingDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ModelEncodingDO::getCode, reqVO.getCode());
        wrapper.likeIfPresent(ModelEncodingDO::getName, reqVO.getName());
        wrapper.eqIfPresent(ModelEncodingDO::getCurrentVersionId, reqVO.getCurrentVersionId());
        wrapper.eqIfPresent(ModelEncodingDO::getDescription, reqVO.getDescription());
        wrapper.eqIfPresent(ModelEncodingDO::getCreateTime, reqVO.getCreateTime());
        // 当前版本状态为 3（已发布/可用）。
        wrapper.apply("exists (select 1 from MODEL_ENCODING_VERSION v where v.ID = MODEL_ENCODING.CURRENT_VERSION_ID and v.STATUS = 3)");
        // 当前版本至少存在一条未删除规则段，避免返回无法实际使用的空方案。
        wrapper.apply("exists (select 1 from MODEL_ENCODING_ITEM i where i.ENCODING_VERSION_ID = MODEL_ENCODING.CURRENT_VERSION_ID and i.DEL_FLAG = 0)");
        // 单独调用扩展排序方法，避免链式 apply 返回父类类型后丢失 LambdaQueryWrapperX 的重载。
        wrapper.orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ModelEncodingDO> selectPage(ModelEncodingPageReqVO reqVO) {
        // 定义排序的字段（防止 SQL 注入，与数据库字段名称一致）
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("id", "create_time", "update_time"));

        // 构造动态查询条件
        return selectPage(reqVO, new LambdaQueryWrapperX<ModelEncodingDO>()
                .eqIfPresent(ModelEncodingDO::getCode, reqVO.getCode())
                .likeIfPresent(ModelEncodingDO::getName, reqVO.getName())
                .eqIfPresent(ModelEncodingDO::getCurrentVersionId, reqVO.getCurrentVersionId())
                .eqIfPresent(ModelEncodingDO::getDescription, reqVO.getDescription())
                .eqIfPresent(ModelEncodingDO::getCreateTime, reqVO.getCreateTime())
                // 如果 reqVO.getName() 不为空，则添加 name 的精确匹配条件（name = '<name>'）
                // .likeIfPresent(ModelEncodingDO::getName, reqVO.getName())
                // 按照 createTime 字段降序排序
                .orderBy(reqVO.getOrderByColumn(), reqVO.getIsAsc(), allowedColumns));
    }

    /**
     * 锁定方案主记录，覆盖尚无流水状态行的首次并发分配，锁随当前事务结束释放。
     *
     * @param id 编码方案主体ID
     * @return 已锁定的方案ID，不存在时返回null
     */
    Long lockScheme(@Param("id") Long id);

    /**
     * 查询指定模型、规则位置及周期的已分配进度；正式生成调用前须持有方案锁。
     *
     * @param key 包含方案ID、使用模型ID、规则排序号及周期的联合查询条件
     * @return 已分配状态；该组合尚未分配时返回null
     */
    ModelEncodingSequenceDO selectState(ModelEncodingSequenceDO key);

    /**
     * 首次分配保存数字进度及规则配置快照。
     *
     * @param state 流水联合主键、已分配值及本次规则来源信息
     * @return 插入行数，正常应为1；失败时由调用方回滚整次分配
     */
    int insertState(ModelEncodingSequenceDO state);

    /**
     * 后续分配更新当前进度及配置快照，保留其他历史周期的记录。
     *
     * @param state 用于定位记录的流水联合主键，以及新的已分配值和规则来源信息
     * @return 更新行数，正常应为1；失败时由调用方回滚整次分配
     */
    int updateState(ModelEncodingSequenceDO state);
}
