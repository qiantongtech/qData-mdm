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

package tech.qiantong.qdata.mdm.model.service.base;

import tech.qiantong.qdata.mdm.common.core.page.PageResult;
import tech.qiantong.qdata.mdm.model.base.dto.*;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelTreeRespVO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelInstanceDO;
import tech.qiantong.qdata.mdm.model.dal.dataobject.base.ModelVersionDO;
import tech.qiantong.qdata.mdm.model.domain.*;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelDdlTransactionStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStage;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelValidationStage;
import tech.qiantong.qdata.mdm.model.util.ModelPublishDatabaseSession;

import java.util.List;

/**
 * 统一处理模型发布、分阶段校验、过程记录及成功版本查询。
 */
public interface IModelPublishService {

    /**
     * 返回主数据域、非空目录及最近成功发布模型，不增加虚拟根节点。
     */
    List<ModelTreeRespVO> getPublishedModelTree();

    /**
     * 按成功发布内容筛选并分页，固定按主模型ID升序；只返回当前可用主模型。
     */
    PageResult<ModelPublishedRespDTO> getPublishedModelPage(ModelPublishedPageReqDTO request);

    /**
     * 按主模型ID读取最近一次成功发布的基本信息；不可用或无成功记录时报错。
     */
    ModelPublishedRespDTO getPublishedModel(Long modelId);

    /** 按成功实例定位主模型ID，不读取快照。 */
    Long getPublishedModelId(Long instanceId);

    /** 只核对当前成功实例和版本，不读取快照。 */
    boolean isPublishedInstance(Long modelId, Long instanceId, Long versionId);

    /**
     * 查询同一次成功发布中的字段、直接子模型及字段引用的参考数据、编码方案。
     *
     * @param modelId 主模型ID
     * @return 成功发布的正常使用配置及实例/版本标识，不含物化信息或规则配置
     */
    ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId);

    /** 结构查询按需附带权威主表及列绑定。 */
    ModelPublishedStructureRespDTO getPublishedModelStructure(Long modelId, boolean includeAuthorityTable);

    /**
     * 查询最近成功实例保存的目标数据库和来源、权威主子表完整物化信息。
     *
     * @param modelId 主模型ID
     * @return 成功模型及完整表、字段、索引记录，含停用标记，不含连接凭证
     */
    ModelPublishedMaterializationRespDTO getPublishedModelMaterialization(Long modelId);

    /**
     * 查询成功发布时保存的本主模型关联规则，不查询当前规则配置。
     *
     * @param modelId      主模型ID
     * @param ruleCategory MATCH匹配、SURVIVOR生存、QUALITY质量
     * @return 对应成功版本及指定类别正常启用的规则
     */
    ModelPublishedRulesRespDTO getPublishedModelRules(Long modelId, String ruleCategory);

    /**
     * 从现有发布入口整体完成准备、计划、执行、失败处理和正式保存。
     *
     * <p>
     * 统一入口的顺序：处理未完成实例 → 保存本次配置 → 构建和比较 → 三阶段校验 → 生成并保存SQL计划 → 执行及核验 → 保存正式结果。
     * 内部恢复、重试和补偿都沿用原实例；对外仍只有这一个发布入口。
     *
     * @param modelId      主模型ID
     * @param datasourceId 目标数据源ID，连接参数由后端读取
     * @param userId       操作人ID
     * @param userName     操作人名称
     * @return 整体成功的发布上下文；失败抛出包含原实例ID的提示
     */
    ModelPublishContext publish(Long modelId, Long datasourceId, Long userId, String userName);

    /**
     * 查询可信数据源并核对连接身份，调用方使用后关闭会话；统一发布内的共享查询器和普通连接由入口最终释放。
     * <p>
     * 计划、执行、核验和补偿都会调用；返回资源由调用方关闭，不承担平台配置库事务。
     */
    ModelPublishDatabaseSession openTarget(ModelPublishContext context);

    /**
     * 读取上次成功的配置、表、字段和索引；已经停用但仍在数据库中的表、列也要保留。
     *
     * <p>
     * 由发布准备阶段调用，作为新旧比较的依据；读取的是平台成功记录，目标库真实结构由TARGET阶段再查。
     *
     * @param context      本次发布上下文
     * @param baseInstance 已核对归属与成功状态的历史实例，首次发布为空
     */
    void readPublishedStructure(ModelPublishContext context, ModelInstanceDO baseInstance);

    /**
     * 用平台独立短事务保存业务库事务阶段；不改变逐SQL执行证据，也不更新模型成功状态。
     * 只允许执行、未知或已确认回滚失败状态，提交结果未知时仍保留原阶段。
     * <p>
     * 保存的是业务库整批事务的阶段，平台记录本身另用独立事务提交；两者不是一个数据库事务。
     */
    ModelPublishContext recordDdlTransaction(Long modelId, Long instanceId, ModelDdlTransactionStatus transactionStatus,
                                             ModelPublishStatus status, String message);

    /**
     * 单独保存已经完成的准备阶段日志；关闭阶段只补日志，不反转正式成功。
     *
     * @param context 当前原实例的发布上下文
     * @param stage   CONFIG、BUILD、CHANGE、TARGET或CLOSE
     * @param message 已完成事项的通俗说明
     */
    void saveStage(ModelPublishContext context, ModelPublishStage stage, String message);

    /**
     * 读取原待提交实例及保存的配置、计划，已成功时返回原结果，不创建新的发布记录。
     *
     * @param modelId    主模型ID
     * @param instanceId 原发布实例ID
     * @return 待提交或已成功的原发布上下文
     */
    ModelPublishContext readPendingCommit(Long modelId, Long instanceId);

    /**
     * 在同一个平台事务中保存完整物化表、字段、成功实例、版本状态和主模型成功指针。
     *
     * <p>
     * 由DDL服务的commitPlan调用；业务库资源已关闭，正式物化记录、版本、成功指针和成功日志在同一个平台事务保存。
     *
     * @param context   从原实例读取的发布上下文
     * @param structure 刚从目标库采集并核验的真实结构，包含停用表和字段
     */
    void completeSuccess(ModelPublishContext context, ModelMaterializedStructure structure);

    /**
     * 正式保存失败后单独记录原因，保留待提交状态，允许同一个实例重新补保存。
     *
     * @param modelId    主模型ID
     * @param instanceId 原发布实例ID
     */
    void saveCommitFailure(Long modelId, Long instanceId);

    /**
     * 单独保存完整SQL计划和待执行结构，不改变成功指针或模型版本状态。
     */
    void saveExecutionPlan(ModelInstanceDO record, ModelPublishExecutionPlan plan);

    /**
     * 尚未执行DDL时记录计划阶段失败，不触碰上次成功记录。
     */
    void failPlanning(ModelInstanceDO record, ModelPublishStage stage, String reason);

    /**
     * 创建发布实例和唯一全流程日志，并写入开始事件；实例与日志一起保存，任一失败一起回滚。
     *
     * @param record 已检查根归属的发布记录，可空版本和数据源用于记录早期失败
     * @return 已生成主键的发布记录
     */
    ModelInstanceDO start(ModelInstanceDO record);

    /**
     * 单独保存完整比较结果和DIFF日志；CHANGE即使随后失败，也能查到失败前实际比较出的变化。
     *
     * @param record 当前发布实例，保存前核对归属和状态
     * @param diff   本次完整新旧结构比较结果，不代表变更已获批准
     */
    void saveComparison(ModelInstanceDO record, ModelPublishStructureDiff diff);

    /**
     * 原子切换准备完成的实例到执行中，并返回平台保存的完整计划，不能用调用方临时SQL替代。
     * <p>
     * 由executePlan调用；先独立保存执行开始，再允许向业务库发送SQL。
     */
    ModelPublishExecutionPlan beginExecution(ModelInstanceDO record);

    /**
     * 读取未完成实例的配置、目标和完整原计划，保存恢复开始日志；拒绝成功、终结失败及已开始补偿的记录。
     *
     * <p>
     * 由recoverPlan调用；只还原原实例的配置与计划，不重新读取草稿或重新生成SQL。
     *
     * @param modelId    主模型ID
     * @param instanceId 要恢复的发布实例ID
     * @return 完全来自已保存记录的发布上下文，不重新读取当前草稿生成SQL
     */
    ModelPublishContext beginRecovery(Long modelId, Long instanceId);

    /**
     * 独立保存一条SQL的恢复核验结果；不增加发送次数，也不能修改计划或撤回已核验成功的步骤。
     */
    void saveRecoveryProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan, String reason);

    /**
     * 读取失败实例及原计划，核对没有正式绑定后保存补偿开始记录；已清理完成的实例直接返回原结果。
     * <p>
     * 由compensatePlan调用；先核对从未正式生效，再保存补偿开始，待提交和成功实例不进入清理。
     */
    ModelPublishContext beginCompensation(Long modelId, Long instanceId);

    /**
     * 独立保存补偿证据、实际残留与日志；不能改写正向SQL及结果，也不能标记发布成功。
     * <p>
     * 由逆向步骤执行器调用；只推进补偿证据，正向执行事实保持原样。
     */
    void saveCompensationProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan, ModelPublishStatus status,
                                  List<ModelPublishRemainingEffect> effects, String reason);

    /**
     * 独立保存执行证据和日志；新计划在整批结束、异常或恢复核实完成后统一保存。
     * 旧计划保留逐条进度方式，只允许改变结果，不允许改写SQL定义或提前标记发布成功。
     * 实例证据与全流程日志在同一独立事务提交，保存成功后才能进入正式发布。
     */
    void saveExecutionProgress(ModelInstanceDO record, ModelPublishExecutionPlan plan,
                               ModelPublishStage stage, ModelPublishStatus status, String reason);

    /**
     * 执行一个阶段的全部规则，把错误留在发布上下文中，再抛出能直接看懂的错误提示。
     *
     * <p>
     * CONFIG查配置，CHANGE查允许哪些修订，TARGET查真实业务库；三个阶段依次通过后才形成可执行计划。
     *
     * @param stage   要执行的阶段
     * @param context 本次发布数据
     */
    void validate(ModelValidationStage stage, ModelPublishContext context);

    /**
     * 只读核对一条SQL的前置或后置条件，将结果写回条件对象；全部满足才返回true。
     * 调用方在使用后保存核验结果。
     * <p>
     * 生成计划后的再次核验：前置条件决定能否发送SQL，后置条件证明SQL是否达到预期，补偿条件决定能否安全撤销。
     */
    boolean verifyConditions(ModelPublishContext context, ModelPublishDatabaseSession session,
                             List<ModelVerificationCondition> conditions);

    /**
     * 把模型、字段、参考数据、编码方案和规则一起读出来。使用独立的只读事务，读取失败就报错。
     *
     * <p>
     * 由发布准备阶段通过Spring代理调用，开启REQUIRES_NEW独立只读事务；读取结果作为本次发布固定配置。
     *
     * @param modelId 根模型ID
     * @param version 已核对归属的根当前版本，子模型沿用根版本
     * @return 本次发布完整配置快照
     */
    ModelPublishSnapshot readCurrentConfig(Long modelId, ModelVersionDO version);

    /**
     * 把本次配置保存成JSON，发布仍处于准备中；不修改上次成功记录ID，也不把版本改成已发布。
     *
     * <p>
     * 通过Spring代理独立提交，后续校验失败也保留本次准备发布的内容。
     *
     * @param record   当前发布实例
     * @param snapshot 完整且独立的模型配置
     */
    void saveSnapshot(ModelInstanceDO record, ModelPublishSnapshot snapshot);

    /**
     * 本次配置和上次成功结构读完后，向全流程日志追加“读取完成”事件，不代表整个发布已经成功。
     *
     * <p>
     * 通过Spring代理独立保存读取完成事件，不把整个实例改成成功。
     *
     * @param record 当前发布实例
     */
    void completePreparation(ModelInstanceDO record);

    /**
     * 准备阶段出错时保存失败原因和日志。此时还没有执行目标SQL，所以只记录本次失败，上次成功结构保持原样。
     *
     * <p>
     * 实例失败状态和对应日志在同一个REQUIRES_NEW事务提交；任一保存失败则一起回滚。
     *
     * @param record 当前发布实例
     * @param reason 可安全记录的失败原因
     */
    void failPreparation(ModelInstanceDO record, String reason);

    /**
     * 对象归属来自平台已经提交的前置不存在、正向发送、结束及后置成功证据，并排除旧成功对象。
     * <p>
     * 由安全补偿条件核验调用，通过Spring代理独立读取平台证据，避免只相信内存中的创建标记。
     */
    boolean ownsCompensationObject(Long modelId, Long instanceId, Integer stepNo);
}
