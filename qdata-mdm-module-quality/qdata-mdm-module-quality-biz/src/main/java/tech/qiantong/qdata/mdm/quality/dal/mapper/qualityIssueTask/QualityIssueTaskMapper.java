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

package tech.qiantong.qdata.mdm.quality.dal.mapper.qualityIssueTask;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskIssueItemRespVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssue.QualityIssueDO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueInfo.QualityIssueInfoDO;
import tech.qiantong.qdata.mdm.quality.controller.admin.qualityIssueTask.vo.QualityIssueTaskPageReqVO;
import tech.qiantong.qdata.mdm.quality.dal.dataobject.qualityIssueTask.QualityIssueTaskDO;
import tech.qiantong.qdata.mdm.mybatis.core.mapper.BaseMapperX;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 问题整改任务Mapper接口
 *
 * @author qdata
 * @date 2026-09-23
 */
public interface QualityIssueTaskMapper extends BaseMapperX<QualityIssueTaskDO> {

    /** 锁定父问题，保证首次下发也能在多个服务实例之间串行校验。 */
    QualityIssueDO lockIssue(@Param("id") Long id);

    List<QualityIssueInfoDO> selectIssueItemsByIds(@Param("ids") List<Long> ids);

    QualityIssueInfoDO lockIssueItem(@Param("id") Long id);

    int markIssueDispatched(@Param("id") Long id, @Param("userId") Long userId, @Param("userName") String userName);

    int restoreUndispatchedIssue(@Param("id") Long id, @Param("userId") Long userId, @Param("userName") String userName);

    List<QualityIssueTaskIssueItemRespVO> selectTaskIssueItems(@Param("id") Long taskId);

    /** 锁定任务并读取最新状态，防止并发状态操作重复生效。 */
    QualityIssueTaskDO lockTask(@Param("id") Long id);

    /** 返回全部候选项，由服务校验唯一性；不任取第一项。 */
    List<QualityIssueInfoDO> selectIssueItemsForUpdate(@Param("issueId") Long issueId, @Param("ruleId") Long ruleId);

    IPage<QualityIssueTaskDO> selectDisplayPage(IPage<QualityIssueTaskDO> page,
            @Param("req") QualityIssueTaskPageReqVO req);

    /** 不分页查询全部展示数据，导出时使用，避免 Page(-1) 被分页拦截器当成空分页。 */
    List<QualityIssueTaskDO> selectDisplayList(@Param("req") QualityIssueTaskPageReqVO req);

    QualityIssueTaskDO selectDisplay(@Param("id") Long id);

    /** 按当前列表的任务 ID 批量统计问题项，避免列表分页前汇总全表。 */
    List<QualityIssueTaskDO> selectDisplayCounts(@Param("taskIds") List<Long> taskIds);

    /** 按当前列表的任务 ID 批量查询最近处理时间。 */
    List<QualityIssueTaskDO> selectLastHandledTimes(@Param("taskIds") List<Long> taskIds);

    List<Map<String, Object>> selectStatusCounts(@Param("req") QualityIssueTaskPageReqVO req);

    /** 仅在预期状态仍然匹配时切换审批状态。 */
    int updateApprovalStatus(@Param("id") Long id, @Param("expectedStatus") String expectedStatus,
                             @Param("targetStatus") String targetStatus, @Param("updaterId") Long updaterId,
                             @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /** 最后一条问题项整改完成后，处理中任务转为待审批。 */
    int markReadyForApproval(@Param("id") Long id, @Param("updaterId") Long updaterId,
                             @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /** 修改结果使问题项回退为整改中时，将待审批/审批中的任务回退为处理中。 */
    int revertToRectifying(@Param("id") Long id, @Param("updaterId") Long updaterId,
                           @Param("updateBy") String updateBy, @Param("updateTime") Date updateTime);

    /** 查询质量问题项关联的模型字段 ID。 */
    Long selectIssueFieldId(@Param("issueInfoId") Long issueInfoId);

    /** 查询主数据记录的当前快照。 */
    String selectSnapshotData(@Param("modelId") Long modelId, @Param("masterRecordId") Long masterRecordId);

}
