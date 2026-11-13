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

/**
 * guideTipConfig 使用说明
 *
 * guideTipConfig 是一个统一管理页面提示信息的配置对象，用于在前端展示各种提示框。
 * 通过配置不同的提示 ID，可以灵活控制提示内容、样式和交互行为。
 *
 * 配置对象结构：
 *
 export const guideTipConfig = {
 '提示ID': {
 title: '提示框标题，可包含 HTML',       // 提示框标题，显示在头部
 content: '提示内容，可包含 HTML 或自定义标签，不设置则不显示相应内容', // 支持 <span>、<a> 标签及自定义属性（如 data-func、data-link）
 type: '提示类型',         // 提示类型，用于样式区分，可选值：'remind'（提醒）、'warning'（警告）、'danger'（危险），其中'danger'类型的不可设置“不再展示”
 version: '配置版本号'     // 当前配置的版本号，用于管理和更新
 }
 }
 *
 * 字段说明：
 *
 * title      string  提示框标题，显示在头部
 * content    string  提示内容，可包含纯文本或 HTML 标签，如 <span>、<a>
 * type       string  提示类型，用于样式区分，可选值：'remind'、'warning'、'danger'
 * version    string  当前配置版本号，用于管理和更新
 *
 * 示例：
 export const guideTipConfig = {
 'cat/AttQualityCat.list': {
 title: '温馨提示！',
 content: `
 这是数据资产下
 <span class="clickable" data-func="routeTo" data-link="/da/dataQuality/dataQualityTasks">数据质量任务</span>
 的目录，访问
 <a href="http://example.com" target="_blank">帮助中心</a> 了解更多
 `,
 type: 'warning',
 version: '1.0'
 },
 'cat/AttAssetCat.detail': {
 title: '注意事项！',
 content: '数据资产下数据质量任务的目录',
 type: 'remind',
 version: '1.0'
 }
 }
 *
 * 注意事项：
 * 1. 提示 ID 唯一：每个提示的 key 必须唯一。
 * 2. HTML 标签安全：仅使用安全标签（如 <span>、<a>），并绑定相应事件逻辑。
 * 3. 版本管理：更新内容时应增加 version，便于追踪和管理。
 * 4. 样式区分：type 为 'remind' 时为提醒样式，'warning' 时为警告样式。
 * 5. 内容长度：内容过长时可分段或换行，保证显示效果。
 */

// 帮助中心联动使用 Markdown 源路径定位文章，避免依赖部署域名和 URL 编码。
// 大多数未收录 Markdown 的提示不配置入口；授权管理按原型保留入口。
export const guideTipHelpDocs = Object.freeze({
    // 授权管理手册尚未收录；入口沿用帮助中心的“未找到文档”反馈。
    'authorization/dataAuthorization.list': { sourcePath: '授权管理/数据授权.md' },
    'authorization/serviceAuthorization.list': { sourcePath: '授权管理/服务授权.md' },
    'model/project.list': { sourcePath: '4、模块功能讲解/1、模型管理/1、主数据域.md' },
    'model/layer.list': { sourcePath: '4、模块功能讲解/1、模型管理/2、模型目录.md' },
    'model/dict.list': { sourcePath: '4、模块功能讲解/1、模型管理/3、参考数据.md' },
    'model/base.list': { sourcePath: '4、模块功能讲解/1、模型管理/4、主数据模型.md' },
    'model/encoding.list': { sourcePath: '4、模块功能讲解/1、模型管理/5、编码方案.md' },
    'model/relation.list': { sourcePath: '4、模块功能讲解/1、模型管理/5、层级与关系.md' },
    // 规则管理
    'rule/rmatch.list': { sourcePath: '4、模块功能讲解/1、规则管理/1、匹配规则.md' },
    'rule/quality.list': { sourcePath: '4、模块功能讲解/1、规则管理/2、判断规则.md' },
    'rule/survivor.list': { sourcePath: '4、模块功能讲解/1、规则管理/3、存放规则.md' },
    /* 数据接入 */
    'ingest/source.list': { sourcePath: '4、模块功能讲解/2、数据接入/1、来源系统.md' },
    'ingest/task.list': { sourcePath: '4、模块功能讲解/2、数据接入/2、接入任务.md' },
    'da/daDatasource.list': { sourcePath: '4、模块功能讲解/2、数据接入/3、数据连接.md' },
    'merge/detect.list': { sourcePath: '4、模块功能讲解/3、多源融合/1、重复检测.md' },
    'merge/review.list': { sourcePath: '4、模块功能讲解/3、多源融合/2、匹配审核.md' },
    'merge/manage.list': { sourcePath: '4、模块功能讲解/3、多源融合/3、合并管理.md' },
    // 质量任务
    'quality/task.list': { sourcePath: '4、模块功能讲解/4、数据质量/1、数据质量任务.md' },
    // 问题数据
    'quality/issue.list': { sourcePath: '4、模块功能讲解/4、数据质量/2、问题数据.md' },
    // 主数据管理
    'master/instance.list': { sourcePath: '4、模块功能讲解/4、数据治理/2、主数据/3、主数据维护.md' },
    'master/change/request.list': { sourcePath: '4、模块功能讲解/4、数据治理/2、主数据/3、主数据维护.md' },

    /* 审计追溯：与左侧菜单层级一致，每个列表定位到自己的操作手册。 */
    // 1. 操作审计
    'audit/operation-log.list': { sourcePath: '4、模块功能讲解/5、审计追溯/1、操作审计.md' },
    // 2. 变更历史
    'audit/change-history.list': { sourcePath: '4、模块功能讲解/5、审计追溯/2、变更历史.md' },
    // 3. 数据版本
    'audit/data-version.list': { sourcePath: '4、模块功能讲解/5、审计追溯/3、数据版本.md' },
    // 4. 来源追溯
    'audit/source-trace.list': { sourcePath: '4、模块功能讲解/5、审计追溯/4、来源追溯.md' },

});

// Define document base path
const DOCS_BASE_URL = 'https://community.qdata.tech';

export const guideTipConfig = {
    // 来源数据
    'ingest/sourceData.list': {
        title: '<strong>来源数据</strong> — 统一承载已接入并按目标主数据模型完成结构化的来源记录',
        content: '来源数据用于统一承载按目标主数据模型完成结构化处理的接入记录。支持按模型查询当前来源数据，并展示待融合、融合中、已融合等状态；连接失败时显示无记录，仅待融合数据可删除。',
        helpCenterOnly: true,
        type: 'remind', version: '1.0'
    },
    // 首页
    'index': {
        title: '重要提醒：本系统为演示站点，<u>每日凌晨 02:30 清除所有用户数据</u>，以保障系统整洁与稳定运行。',
        content: `如需体验完整功能，请下载开源代码自行部署。如需 <u>开源版品牌授权</u> 或 <u>咨询商业版</u>，请点击查看详情： 👉  <a href="${DOCS_BASE_URL}/business/policy.html" target="_blank">了解授权详情</a>`,
        type: 'danger', version: '1.0'
    },
    /* 授权管理：文字取自授权管理原型，视觉与交互沿用 GuideTip。 */
    'authorization/dataAuthorization.list': {
        title: '<strong>数据授权</strong> — 管理主数据访问与操作权限',
        content: '数据授权用于管理主数据模型的访问和操作权限，每条授权对应一个主数据模型。支持配置授权范围、字段权限及权限合并规则，控制用户查看和编辑主数据的权限。',
        type: 'remind',
        version: '1.0'
    },
    'authorization/serviceAuthorization.list': {
        title: '<strong>服务授权</strong> — 管理数据服务访问权限',
        content: '服务授权用于管理数据服务的访问权限，支持配置授权对象、可访问的数据服务及授权范围，并设置可查看字段、数据范围和有效期。通过统一授权管理，控制服务调用权限，确保主数据服务安全、规范地共享与使用。',
        type: 'remind',
        version: '1.0'
    },
    /* 模型管理 */
    // 主数据域
    'model/project.list': {
        title: '<strong>主数据域</strong> — 划分主数据模型的业务范围',
        content: `主数据域采用单层结构，用于组织模型目录和主数据模型，只有启用的主数据域可在模型定义中被新选择;禁用不影响已有模型归属，已被模型引用的主数据域不可删除。<a href="" target="_blank">查看帮助文档</a>`,
        type: 'remind', version: '1.0'
    },
    // 模型目录
    'model/layer.list': {
        title: '<strong>模型目录</strong> — 在主数据域内组织主数据模型',
        content: `模型目录用于分类组织和管理主数据模型。请先选择所属主数据域，再维护目录结构。目录最多支持5层，仅可在所属主数据域内调整；已关联模型的目录允许编辑，但不可删除，确保目录结构规范及模型关联关系完整。<a href="" target="_blank">查看帮助文档</a>`,
        type: 'remind', version: '1.0'
    },
    // 参考数据
    'model/dict.list': {
        title: '<strong>参考数据</strong> — 在主数据域内组织主数据模型',
        content: `参考数据用于统一管理平台标准取值集合，为主数据模型提供规范的字段取值依据。支持维护参考数据及对应数据项，两者可分别启用或停用，便于灵活管理标准值的使用状态，确保数据口径统一、取值规范。`,
        type: 'remind', version: '1.0'
    },
    // 编码方案
    'model/encoding.list': {
        title: '<strong>编码方案</strong> — 维护主数据编码规则',
        content: `编码方案用于统一管理主数据编码规则，支持维护编码方案及编码配置，为主数据模型提供编码依据，确保编码唯一、规范、易识别，便于模型引用和日常管理。`,
        type: 'remind', version: '1.0'
    },
    // 主数据模型
    'model/base.list': {
        title: '<strong>模型定义</strong> — 管理模型信息和模型字段，最终物化',
        content: `模型定义用于统一定义和管理主数据的结构与业务属性，支持模型基本信息、字段定义、模型关系及发布生命周期管理。通过规范模型结构和字段标准，为主数据接入、质量校验、多源融合及统一管理提供基础支撑。`,
        type: 'remind', version: '1.0'
    },
    // 层级与关系
    'model/relation.list': {
        title: '<strong>模型关系</strong> — 自动展示正式模型的主子关系、引用关系和分类层级',
        content: `模型关系用于展示模型的主子关系、引用关系和分类层级。系统依据已生效模型元数据自动生成图谱，本页仅供查看模型关联及引用方向，不支持新增、修改或删除。`,
        type: 'remind', version: '1.0'
    },
    /* 规则管理 */
    // 匹配规则
    'rule/match.list': {
        title: '<strong>匹配规则</strong> — 定义数据匹配规则',
        content: `匹配规则用于配置主数据匹配条件，支持设置字段匹配方式及分值，范围为0～100的整数。系统依据规则识别重复或相似对象，为重复检测和多源融合提供依据。`,
        type: 'remind', version: '1.0'
    },
    // 判断规则
    'rule/quality.list': {
        title: '<strong>质量规则</strong> — 定义数据质量规则',
        content: `质量规则用于定义和管理主数据的质量校验标准，支持从完整性、规范性、唯一性等维度配置校验条件。系统依据规则识别不符合要求的数据，并标记为问题数据，为数据治理、整改任务及质量提升提供依据。`,
        type: 'remind', version: '1.0'
    },
    // 存存规则
    'rule/survivor.list': {
        title: '<strong>生存规则</strong> — 定义数据生存规则',
        content: `生存规则用于配置多源数据合并时的字段取值策略，可按数据来源、质量和更新时间等设置优先级。系统按规则选择字段值，减少数据冲突，保障主数据准确、一致和可信。`,
        type: 'remind', version: '1.0'
    },

    /* 数据接入 */
    // 来源系统
    'ingest/source.list': {
        title: '<strong>来源系统</strong> — 统一标识主数据业务来源及其技术连接关系',
        content: `来源系统用于回答数据业务上来自谁，本期支持新增、修改和删除，编码创建后不可修改；可信度为0～100整数，可与平台已有数据库连接多对多关联，有任务引用时禁止删除。`,
        type: 'remind', version: '1.0'
    },
    // 接入任务
    'ingest/task.list': {
        title: '<strong>接入任务</strong> — 定义来源数据如何获取、转换并进入主数据模型',
        content: `接入任务用于回答数据从哪里获取、如何转换以及何时执行。每个任务固定对应 1 个来源系统和 1 个主数据模型；停用后不可执行，修改只影响后续执行，删除不会清除已形成的来源数据。`,
        type: 'remind', version: '1.0'
    },

    'da/daDatasource.list': {
        title: '<strong>数据连接</strong> — 统一管理多源异构数据接入',
        content: `用于配置和管理平台所需的数据源连接，支持关系型数据库连接。作为<u>数据采集</u>、<u>元数据同步</u>和<u>任务执行</u>的基础，保障<u>资产地图</u>等模块的数据可发现与可访问。
        <a href="" target="_blank">查看帮助文档</a>
    `,
        type: 'remind', version: '1.0'
    },
    /* 数据治理 */
    // 重复检测
    'merge/detect.list': {
        title: '<strong>重复检测</strong> — 按模型识别待治理来源中的重复关系',
        content: `重复检测用于识别不同来源数据中的潜在重复对象。用户按已发布模型发起检测，系统生成候选组；重复候选组进入匹配审核，未发现重复的数据进入单条结果确认。`,
        type: 'remind', version: '1.0'
    },
    // 匹配审核
    'merge/review.list': {
        title: '<strong>匹配审核</strong> — 按模型识别待治理来源中的重复关系',
        content: `匹配审核用于人工判断重复检测生成的候选组是否属于同一主数据对象。确认同一对象后可提交至合并管理；不正确的候选组可删除，确保后续数据融合准确、可靠。`,
        type: 'remind', version: '1.1'
    },
    // 合并管理
    'merge/manage.list': {
        title: '<strong>合并管理</strong> — 承接单条确认、多源合并与结果提交',
        content: `合并管理用于处理单条数据确认和多源数据合并，支持字段调整、质量检测及结果确认。完成后可提交审批；本期仅记录审批中状态，不创建审批流程或写入权威主数据。`,
        type: 'remind', version: '1.0'
    },
    // 质量任务
    'quality/task.list': {
        title: '<strong>质量任务</strong> — 定义数据质量任务',
        content: `质量任务用于管理主数据质量检测，支持依据质量规则校验数据，识别完整性、规范性、唯一性等问题并记录结果，为问题分析、整改任务生成和质量提升提供依据。`,
        type: 'remind', version: '1.0'
    },
    // 问题数据
    'quality/issue.list': {
        title: '<strong>问题数据</strong> — 查看和维护当前模型的问题数据',
        content: `
问题数据用于查看和管理质量检测发现的异常记录，支持按主数据模型筛选，查看问题详情、关联规则及检测结果，并可下发整改任务、跟踪处理进度，便于持续治理。`,
        type: 'remind', version: '1.0'
    },

    // 整改任务
    'quality/rectification.list': {
        title: '<strong>整改任务</strong> — 把质量问题落实到具体责任人',
        content: `整改任务用于管理和跟踪数据质量问题的处理过程。任务下发后通知责任人，责任人可进入任务详情，按流程处理问题并完成整改，反馈处理结果，推动问题及时解决。`,
        helpCenterOnly: true,
        type: 'remind', version: '1.1'
    },

    // 权威主数据入口
    'master/instance.list': {
        title: '<strong>权威主数据</strong> — 统一查看和维护平台最终认可的主数据',
        content: `选择需要管理的主数据对象进入工作区，支持主数据新增、修改、导入及生命周期管理等操作，实现主数据的统一维护与管理，确保数据准确、一致、可追溯。
        <a href="" target="_blank">查看帮助文档</a>
        `,
        type: 'remind', version: '1.0'
    },

    // 操作审计
    // 我的申请
    'master/change/request.list': {
        title: '<strong>我的申请</strong> — 查看本人提交的主数据维护申请',
        content: `支持按申请类型和处理进度筛选，点击“详情”查看申请内容、审批进度及结果。审批中且未执行的申请可撤回；已驳回、已撤回或已终止且未执行的申请可重新申请。操作按钮按账号权限展示，不满足操作条件时置灰。维护申请需在对应主数据工作区发起。<a href="" target="_blank">查看帮助文档</a>`,
        type: 'remind', version: '1.1'
    },
    // 数据分发
    //目标系统
    'dist/target.list': {
        title: '<strong>目标系统</strong> —— 统一登记主数据接收方',
        content: '只维护下游系统的业务身份信息。详情页补齐了启用 / 停用、关联分发任务查看，以及停用影响说明；接口地址、认证、Header 和字段映射仍统一在分发任务中维护。',
        // 暂无专属文章时，沿用来源数据页面的帮助中心入口。
        helpCenterOnly: true,
        type: 'remind', version: '1.2'
    },
    'dist/task.list': {
        title: '<strong>分发任务</strong> —— 权威主数据主动分发',
        content: '\n' +
            '分发任务用于将权威主数据分发至目标系统，每个任务对应一个模型、目标系统和目标API。支持查看、编辑、执行及失败重试，并查看执行结果和日志。',
        helpCenterOnly: true,
        type: 'remind', version: '1.0'
    },
    'dist/service.list': {
        title: '<strong>数据服务</strong> —— 基于主数据模型提供标准查询 API',
        content: '数据服务用于基于已发布可用的主数据模型配置REST查询API，可设置查询条件和返回字段，明确接口查询范围及响应内容，为业务系统提供统一的主数据查询服务。',
        helpCenterOnly: true,
        type: 'remind', version: '1.0'
    },
    /* 审计追溯：四个列表共用 GuideTip 提示组件，文档入口由上方 guideTipHelpDocs 映射。 */
    // 操作审计：核查一次用户操作的审计事实。
    'audit/operation-log.list': {
        title: '<strong>操作审计</strong> — 查看主数据与模型操作记录',
        content: `操作审计用于记录和追溯主数据及模型的操作行为，支持查看操作人员、操作时间、业务对象、操作类型及变更内容，帮助核查数据变更过程，明确操作来源与责任。模型管理仅记录主数据模型本身的操作。`,
        type: 'remind', version: '1.0'
    },
    // 变更历史：查看单条主数据的数据和生命周期变化。
    'audit/change-history.list': {
        title: '<strong>变更历史</strong> — 追查主数据变化',
        content: `变更历史用于记录和追踪主数据在维护过程中的变化情况，支持查看变更对象、变更时间、变更前后内容及最终处理结果，帮助用户全面了解主数据的变化过程，实现数据变更的全程追溯与历史核查。`,
        type: 'remind', version: '1.0'
    },
    // 数据版本：查看正式快照并对比两个版本。
    'audit/data-version.list': {
        title: '<strong>数据版本</strong> — 查看正式历史版本',
        content: `数据版本用于管理主数据的正式历史版本，支持查看版本记录、形成时间及各阶段状态，追踪数据演变过程，为历史查询、数据核查和版本追溯提供依据。`,
        type: 'remind', version: '1.0'
    },
    // 来源追溯：从权威主数据定位来源记录与字段取值。
    'audit/source-trace.list': {
        title: '<strong>来源追溯</strong> — 追查权威主数据来源',
        content: `来源追溯用于查看权威主数据的来源构成，支持追踪主数据由哪些来源记录形成，并查看各字段对应的来源记录及取值依据，帮助核查数据来源与字段归属，确保主数据形成过程清晰、可追溯。`,
        type: 'remind', version: '1.0'
    },
}
