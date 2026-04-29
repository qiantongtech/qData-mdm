/*
 * Copyright © 2025 Qiantong Technology Co., Ltd.
 * qData Data Middle Platform (Open Source Edition)
 *  *
 * License:
 * Released under the Apache License, Version 2.0.
 * You may use, modify, and distribute this software for commercial purposes
 * under the terms of the License.
 *  *
 * Special Notice:
 * All derivative versions are strictly prohibited from modifying or removing
 * the default system logo and copyright information.
 * For brand customization, please apply for brand customization authorization via official channels.
 *  *
 * More information: https://qdata.qiantong.tech/business.html
 *  *
 * ============================================================================
 *  *
 * 版权所有 © 2025 江苏千桐科技有限公司
 * qData 主数据平台（开源版）
 *  *
 * 许可协议：
 * 本项目基于 Apache License 2.0 开源协议发布，
 * 允许在遵守协议的前提下进行商用、修改和分发。
 *  *
 * 特别说明：
 * 所有衍生版本不得修改或移除系统默认的 LOGO 和版权信息；
 * 如需定制品牌，请通过官方渠道申请品牌定制授权。
 *  *
 * 更多信息请访问：https://qdata.qiantong.tech/business.html
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

// 定义文档基础路径
const DOCS_BASE_URL = 'https://qdata.qiantong.tech';


export const guideTipConfig = {
    // 首页
    'index': {
        title: '重要提醒：本系统为演示站点，<u>每日凌晨 02:30 清除所有用户数据</u>，以保障系统整洁与稳定运行。',
        content: `如需体验完整功能，请下载开源代码自行部署。如需 <u>开源版品牌授权</u> 或 <u>咨询商业版</u>，请点击查看详情： 👉  <a href="${DOCS_BASE_URL}/business.html" target="_blank">了解授权详情</a>`,
        type: 'danger', version: '1.0'
    },


}
