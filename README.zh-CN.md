![qData-mdm 主数据平台横幅](images/zh-CN/banner.png)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--MDM-v1.0.0-blue.svg" alt="qData-mdm v1.0.0"/>

  <img src="https://gitee.com/qiantongtech/qData-mdm/badge/star.svg" alt="Gitee Stars"/>
  <img src="https://img.shields.io/github/stars/qiantongtech/qData-mdm?label=GitHub%20Stars" alt="GitHub Stars"/>
</p>

<p align="center">
  📖简体中文 | <a href="README.md">📖English</a> | <a href="README.ja-JP.md">📖日本語</a>
</p>

## 🌈 平台简介
**qData-mdm 主数据平台**帮助企业统一管理客户、供应商、物料、组织等主数据。平台支持按业务域建模，将多系统来源数据接入、匹配并融合为权威主数据，通过审批、质量检测和整改管理数据变更，再通过分发任务或查询服务向下游共享数据。工作台汇总主数据规模、生命周期、正式变更和待关注事项，授权与审计功能覆盖日常维护和对外共享。

✨✨✨**在线文档**✨✨✨ <a href="https://qdata.qiantong.tech" target="_blank">https://qdata.qiantong.tech</a>

<!-- ✨✨✨**专业版咨询地址**✨✨✨ <a href="https://qdata.qiantong.tech/business/policy.html" target="_blank">https://qdata.qiantong.tech/business/policy.html</a> -->

> 如果 qData-mdm 对您有帮助，请点个 **Star ⭐️**，这是我们持续更新的动力！ 🚀

## 🍱 使用场景

适用于需要统一主数据标准、规范主数据维护流程、建立主数据共享机制的企业、集团与政企组织。

| 场景          | 描述                            | 典型客户类型             |
| ----------- | ----------------------------- | ------------------ |
| **主数据标准建模** | 按业务域和目录组织模型，统一定义业务字段、分类层级、关联关系、参考数据和编码规则。 | 集团企业、制造企业、国企平台 |
| **权威主数据集中维护** | 统一受理新增、修改、导入和生命周期变更申请，保留正式版本、审批过程及数据来源。 | 中大型企业、共享服务中心 |
| **多源数据接入与融合** | 从业务数据库、文件和 API 接入数据，识别跨系统重复记录，经人工确认、质量检测和审批后形成权威主数据。 | 制造企业、政务平台、信息化项目 |
| **主数据分发共享** | 向 ERP、HR、财务等下游系统全量或增量推送数据，或通过已发布的只读 API 提供授权查询。 | 集团企业、政企单位、行业平台 |
| **主数据质量治理** | 按规则定期检测权威主数据，归集失败记录、分配整改责任并追踪处理结果。 | 数据治理团队、信息中心、运维团队 |


## 💡 优势

| 优势点           | 描述                             |
| ------------- | ------------------------------ |
| **模型版本与发布保护** | 修订期间现有模型继续生效，发布前检查结构兼容性、引用和数据风险，并保留历史版本及发布记录。 |
| **多源融合结果可追溯** | 保留匹配依据、字段取值来源、人工调整、检测和审批结果，可从权威主数据追溯来源记录。 |
| **统一业务待办** | 审批、填报和整改任务集中进入个人任务中心，处理状态随来源业务同步。 |
| **精细化授权** | 模型数据按行、字段和业务操作授权，对外 API 通过独立的服务授权控制调用。 |
| **支持多种部署方式** | 支持源码启动和 Docker 方式部署，便于不同环境下安装、联调和使用。 |


## 📋 功能清单

| 核心业务功能 | 重点能力 |
|--------------|----------|
| **主数据建模与发布** | 按主数据域和目录组织普通模型、分类模型，配置关联关系及 1:N 子模型。模型通过校验和审核后手动发布并生成物理结构，修订时检查结构兼容性并保留历史版本。 |
| **参考数据与自动编码** | 统一维护参考数据并绑定模型字段，规范业务取值。按版本配置编码规则，支持固定文本、业务字段、参考数据映射、日期时间和流水号，正式生成时校验编码唯一性。 |
| **权威主数据维护** | 按已发布模型查询正式数据及关联明细，支持新增、修改、复制、批量修改和导入导出。批量变更整批校验、整批生效，支持对比历史版本并发起恢复申请。 |
| **生命周期与变更审批** | 管理生效、冻结、失效和归档状态，支持冻结、解冻、失效、恢复和归档。主数据维护与状态变更按业务规则审批，可追踪申请进度、审批意见和生效结果。 |
| **多源数据接入** | 支持数据库全量与增量读取、CSV/XLS/XLSX 文件导入和外部 API 主动拉取。按来源唯一标识和字段映射识别变更，保留采集版本及失败明细。 |
| **重复检测与数据融合** | 通过精确、模糊或多字段加权匹配识别重复记录，支持人工确认或拆分候选。按来源优先级、可信度、最新值或非空值推荐字段取值，处理冲突后经质量检测和审批形成权威主数据。 |
| **质量检测与整改** | 支持非空、格式、唯一性、参考数据合法性及字段间逻辑等规则，手动或定时检测权威主数据。归集失败记录和原因，按问题项分派责任人并跟踪整改。 |
| **数据分发与服务** | 按目标系统配置分发范围和字段映射，支持手动或定时全量、增量推送，以及生命周期变化同步。可发布只读 REST API，提供单条与分页查询。 |
| **数据与服务授权** | 按用户、角色和组织控制模型的行、字段和业务操作权限。数据服务独立授权，按调用主体、服务和有效期控制 API 访问。 |
| **业务审计与来源追溯** | 记录关键业务操作及影响对象，支持跨模型查询主数据变更和历史版本，从正式数据定位来源记录、映射关系和审批过程。 |

💡 如您有好的建议或功能需求，欢迎 [提交Issue](https://gitee.com/qiantongtech/qData/issues)，与我们共同完善主数据平台功能。

[//]: # (## 🧩 架构图)

[//]: # (![framework.png]&#40;images%2Fframework.png&#41;)

## 🛠️ 技术栈
qData-mdm 平台采用前后端分离架构，后端基于 Spring Boot，前端基于 Vue 3，并整合了主流的认证、数据库访问、缓存与前端组件能力。

<table>
  <tr>
    <th>分类</th><th>技术</th><th>描述</th>
  </tr>
  <tr>
    <td rowspan="6">后端技术栈</td><td>Spring Boot</td><td>提供快速开发能力与统一服务启动入口</td>
  </tr>
  <tr>
    <td>Spring Security</td><td>实现用户认证、授权与安全控制</td>
  </tr>
  <tr>
    <td>MySQL、达梦8</td><td>持久化存储与数据源配置管理</td>
  </tr>
  <tr>
    <td>MyBatis-Plus</td><td>简化数据库操作与多数据源访问</td>
  </tr>
  <tr>
    <td>Redis</td><td>支持缓存、登录态与基础中间件能力</td>
  </tr>
  <tr>
    <td>Knife4j / OpenAPI</td><td>提供接口文档展示与联调支持</td>
  </tr>

  <tr>
    <td rowspan="3">前端技术栈</td><td>Vue 3</td><td>现代化响应式前端框架</td>
  </tr>
  <tr>
    <td>Element Plus</td><td>常用 UI 组件支持与后台管理界面构建</td>
  </tr>
  <tr>
    <td>Vite</td><td>快速开发与构建工具</td>
  </tr>

  <tr>
    <td rowspan="2">第三方插件</td><td>AntV X6</td><td>支撑流程、图形化设计与可视化交互场景</td>
  </tr>
  <tr>
    <td>ECharts</td><td>支撑统计图表与可视化展示能力</td>
  </tr>
</table>


## 🏗️ 部署要求

在部署 qData-mdm 之前，请确保以下环境和工具已正确安装：

<table>
  <tr>
    <th>环境</th><th>项目</th><th>推荐版本</th><th>说明</th>
  </tr>
  <tr>
    <td rowspan="6">后端</td><td>JDK</td><td>1.8 或以上</td><td>建议使用 OpenJDK 8 </td>
  </tr>
  <tr>
    <td>Maven</td><td>3.6+</td><td>项目构建与依赖管理</td>
  </tr>
  <tr>
    <td>达梦8 / MySQL</td><td>8.0+</td><td>关系型数据库环境，开发配置默认偏向达梦8</td>
  </tr>
  <tr>
    <td>Redis</td><td>5.0+</td><td>缓存与登录态等基础能力支持</td>
  </tr>
  <tr>
    <td>Docker / Docker Compose</td><td>可选</td><td>用于快速体验和测试环境部署</td>
  </tr>
  <tr>
    <td>操作系统</td><td>Windows / Linux / Mac</td><td>通用环境均可运行</td>
  </tr>

  <tr>
    <td rowspan="3">前端</td><td>Node.js</td><td>16+</td><td>前端构建工具依赖</td>
  </tr>
  <tr>
    <td>npm</td><td>8+</td><td>包管理器</td>
  </tr>
  <tr>
    <td>Chrome / Edge</td><td>最新版</td><td>推荐用于本地调试与系统访问</td>
  </tr>
</table>


[//]: # (## 🚨 商用授权)

[//]: # ()
[//]: # (qData-mdm 提供 **专业版** 与 **开源版** 两种形态，满足不同规模与场景下的用户需求。开源版提供系统管理、主数据建模、维护、采集、分发和质量治理等基础功能；专业版则面向更复杂的企业级场景，提供组合模型、订阅体系、实时分发、整改处理、复杂审批与更完整的治理保障。)

[//]: # ()
[//]: # (👉 如需 **开源版品牌授权** 或 **咨询专业版**，请点击按钮查看详情：[💼 了解授权详情]&#40;https://qdata.qiantong.tech/business/policy.html&#41;)

[//]: # (## 🚀 快速开始)

[//]: # ()
[//]: # (| 部署方式                    | 说明                                                              | 适用场景               |)

[//]: # (| ----------------------- | --------------------------------------------------------------- | ------------------ |)

[//]: # (| Docker Compose 体验部署 | 通过 `docker/` 目录中的编排文件快速启动数据库、Redis、Nginx 与后端服务，适合先体验主数据平台开源版功能。 | **初学者快速上手**、功能演示、测试环境  |)

[//]: # (| 使用源代码本地启动  | 后端可通过 `mvn -pl qData-mdm-server -am spring-boot:run` 启动，前端在 `qData-mdm-ui` 下执行 `npm install` 和 `npm run dev`。  | **日常开发**、功能联调          |)

[//]: # (| 自主部署（纯手工安装）  | 根据 `application-dev.yml` / `application-prod.yml`、数据库初始化脚本和前端构建产物，手工完成部署与参数配置。 | **生产环境**、大规模部署、个性化定制场景 |)

[//]: # ()
[//]: # (👉 查看完整的安装与部署资料时，建议结合 `docker/`、`qData-mdm-server/src/main/resources/`、`sql/` 目录与官方文档一并使用。)

## 👥 QQ交流群
欢迎加入 qData 官方 QQ 交流群，获取最新动态、技术支持与使用交流。

👉 <a href="https://community.qdata.tech/discuss.html">点击加入 QQ 交流群</a>

<!-- [![加入QQ群](https://img.shields.io/badge/QQ群-814145903（已满）-blue.svg)](https://qm.qq.com/q/Qz5NJut20y)
[![加入QQ群](https://img.shields.io/badge/QQ群-236734346-blue.svg)](https://qm.qq.com/q/Bpml33lTWK)
[![加入QQ群](https://img.shields.io/badge/QQ群-675093323-blue.svg)](https://qm.qq.com/q/VIq5vtICo6) -->

## 🖼️ 系统配图

<table>
  <tr>
    <td><img src="images/zh-CN/system/mdm-01.png" alt="工作台" width="400"/></td>
    <td><img src="images/zh-CN/system/mdm-02.png" alt="实体模型列表" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/mdm-03.png" alt="实体模型字段配置" width="400"/></td>
    <td><img src="images/zh-CN/system/mdm-04.png" alt="模型关系配置" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/mdm-05.png" alt="分类模型配置" width="400"/></td>
    <td><img src="images/zh-CN/system/mdm-06.png" alt="主数据模型关系图谱" width="400"/></td>
  </tr>
</table>
