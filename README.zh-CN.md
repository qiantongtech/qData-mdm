![qData主数据平台横幅](images/zh-CN/banner.jpg)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--mdm-v1.0.1-blue.svg" alt="qData-mdm v1.0.1"/>

  <img src="https://gitee.com/qiantongtech/qData-mdm/badge/star.svg" alt="Gitee Stars"/>
  <img src="https://img.shields.io/github/stars/qiantongtech/qData-mdm?label=GitHub%20Stars" alt="GitHub Stars"/>
  <img src="https://atomgit.com/qiantongtech/qData-mdm/star/badge.svg"/>
</p>

<p align="center">
  📖简体中文 | <a href="README.md">📖English</a> | <a href="README.ja-JP.md">📖日本語</a>
</p>

## 平台简介

**模型驱动，统一标准，质量治理，开放共享。让多个业务系统用好同一套主数据。**

**qData 主数据平台（qData-mdm）** 是一款开源主数据管理平台。通过模型驱动、统一标准、质量治理与开放服务，将核心业务对象的数据定义、日常维护和共享使用集中起来，减少重复建档、编码差异与基础信息不一致带来的协作成本。

你可以从供应商、物料、设备等一类业务对象开始，建立统一的维护入口、参考取值和编码规则，通过查询 API 与数据分发连接采购、仓储、财务等业务系统。开发团队可以复用已有的数据管理、权限和服务能力，并基于源码按项目需要扩展。

✨✨✨**在线文档**✨✨✨ <a href="https://qdata.qiantong.tech" target="_blank">https://qdata.qiantong.tech</a>

✨✨✨**开源版演示地址**✨✨✨ <a href="https://demo-mdm.qdata.tech" target="_blank">https://demo-mdm.qdata.tech</a>（账号 `qData`，密码 `qData123`）

✨✨✨**专业版演示地址**✨✨✨ <a href="https://pro-demo-mdm.qdata.tech" target="_blank">https://pro-demo-mdm.qdata.tech</a>，演示账号请 [联系客服获取](https://docs.qdata.tech/business/policy.html)。

> 如果 qData-mdm 对您有帮助，请点个 **Star ⭐️**，这是我们持续更新的动力！ 🚀

许可：**Apache License 2.0 及项目附加条件**，详见 [LICENSE](LICENSE) 与[商用说明](#license-and-services)。

## 核心特点

### 模型驱动：按业务定义主数据，减少重复建设

围绕客户、供应商、物料、设备等对象配置主数据模型与字段，在统一模型下组织数据维护与查询服务。按主数据域和模型目录管理业务对象，复用平台已有的基础能力，减少反复建设维护页面、编码逻辑和查询接口的工作。

### 统一标准：集中管理编码与取值，保持业务口径一致

统一维护参考数据与编码方案，为业务分类、标准取值和编号规则提供共同依据。支持组合固定文本、业务字段、参考数据映射、日期时间与流水号等编码段，让不同业务对象的编码有规则可循，让基础信息有一致的表达方式。

### 质量治理：定位数据问题，跟踪责任整改

通过基础质量规则及人工、定时质量任务检查正式主数据，定位问题记录、字段和原因。结合问题归集与整改任务，明确处理责任、跟踪整改进度；修正后可再次执行检查，把数据质量管理落实到日常维护中。

### 开放服务：通过查询与分发，让业务系统使用主数据

基于已发布模型配置**只读 REST API**，提供单条与分页查询，按需设置查询条件、响应字段和调用授权。支持人工或定时执行全量、增量分发，将主数据字段映射到目标 API，为业务系统提供统一的数据使用入口。

### 开源可扩展：理解实现方式，按项目需要二次开发

采用 Java、Spring Boot 与 Vue 3 技术栈，按模型、主数据、规则质量、数据接入、服务分发等模块组织代码。开发者可以从具体业务模块入手，阅读实现、扩展质量规则或定制业务能力，复用已有的数据管理与权限基础。

## 快速开始

### 在线体验

访问 [开源版演示环境](https://demo-mdm.qdata.tech)，无需安装即可了解产品。

**账号：`qData`　密码：`qData123`**

首次体验建议选择一个已有主数据模型，依次了解：

1. **模型与标准**：查看模型字段、参考数据和编码方案，了解一类业务对象如何统一定义。
2. **主数据维护**：查看正式记录及业务字段，了解日常基础信息如何集中维护。
3. **质量检查**：查看质量任务、问题数据与整改任务，了解问题如何被定位和处理。
4. **服务与共享**：查看数据服务或分发任务，了解主数据如何提供给其他业务系统。

在具备相应操作权限的环境中，可进一步配置一个查询服务：选择已发布模型，设置查询与响应字段，发布服务并完成调用授权，再使用发布页面提供的实际地址获取一条主数据或分页结果。

### 选择部署方式

- **本地部署**：使用 `deploy/docker/` 下的 Docker Compose 配置在本机启动完整环境。
- **源码启动**：分别启动后端 `qdata-mdm-server` 和前端 `qdata-mdm-ui`，适合开发和联调。
- **自主部署**：手动安装、配置和管理各项服务，适合生产环境和个性化配置。

源码部署前，请先准备数据库与 Redis，并将数据库驱动、连接配置和初始化脚本对应到所选代码版本，再配置前后端服务。前端构建依赖以对应分支的项目配置为准。

## 产品功能架构

![qData 主数据中台开源版产品功能架构图](images/zh-CN/architecture.jpg)

## 系统配图

<table>
    <tr>
      <td><img src="images/zh-CN/system/login.png" alt="登录页面" width="400"/></td>
      <td><img src="images/zh-CN/system/index.png" alt="首页" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/master.png" alt="权威主数据" width="400"/></td>
      <td><img src="images/zh-CN/system/master_detail.png" alt="权威主数据列表" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/project_detail.png" alt="主数据域详情" width="400"/></td>
      <td><img src="images/zh-CN/system/layer_detail.png" alt="模型目录详情" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/model.png" alt="模型定义" width="400"/></td>
      <td><img src="images/zh-CN/system/model_detail.png" alt="模型详情" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/model_form_one.png" alt="基础信息" width="400"/></td>
      <td><img src="images/zh-CN/system/model_form_four.png" alt="模子模型" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/relation.png" alt="模型关系" width="400"/></td>
      <td><img src="images/zh-CN/system/relation_detail.png" alt="模型关系详情" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/match_form.png" alt="匹配规则表单" width="400"/></td>
      <td><img src="images/zh-CN/system/survivor_form.png" alt="生存规则表单" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/quality_form.png" alt="质量规则表单" width="400"/></td>
      <td><img src="images/zh-CN/system/review.png" alt="匹配审核" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/datasource.png" alt="数据连接" width="400"/></td>
      <td><img src="images/zh-CN/system/datasource_detail.png" alt="测试连接" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/ingest_task.png" alt="接入任务" width="400"/></td>
      <td><img src="images/zh-CN/system/ingest_task_detail.png" alt="接入任务详情" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/quality_task.png" alt="质量任务" width="400"/></td>
      <td><img src="images/zh-CN/system/quality_task_form.png" alt="质量任务表单" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/data_auth_form_one.png" alt="主体与模型" width="400"/></td>
      <td><img src="images/zh-CN/system/data_auth_form_four.png" alt="操作与有效期" width="400"/></td>
    </tr>
    <tr>
      <td><img src="images/zh-CN/system/quality_issue_task.png" alt="整改任务" width="400"/></td>
      <td><img src="images/zh-CN/system/operation_log.png" alt="操作审计" width="400"/></td>
    </tr>
</table>

## 能力概览

| 模块 | 子模块 | 主要功能 |
| --- | --- | --- |
| **工作台** | 工作台 | 在权限范围内汇总主数据、模型、域、参考数据项和有效授权。查看各域数据规模、生命周期分布及近 7 天或 30 天的正式变更，从图表进入明细，或查看发布失败、授权即将到期等待关注事项。 |
| **主数据管理** | 权威主数据、我的申请、填报任务、任务中心 | 按已发布模型新增、修改、复制和导入导出数据；批量变更整批校验、整批生效。管理生效、冻结、失效和归档状态，对比版本并申请恢复。在“我的申请”跟踪或撤回申请，在任务中心处理审批、填报和整改待办。 |
| **模型管理** | 主数据域、模型目录、主数据模型、层级与关系、参考数据、编码方案 | 按域和目录组织普通模型与分类模型，配置字段、模型引用和 1:N 子模型，并在关系图中查看依赖。字段可绑定参考数据或分段编码方案。模型经校验、审核后手动发布并生成物理结构；修订时检查兼容性并保留历史版本。 |
| **规则管理** | 匹配规则、生存规则、质量规则 | 用精确、标准化、模糊或多字段加权匹配识别重复候选。按来源优先级、可信度、最新值或非空值推荐字段取值。配置必填、格式、范围、唯一性、参考数据及字段间逻辑校验。 |
| **流程管理** | 流程配置、流程实例 | 统一审批模型、主数据内容与状态变更，以及多源融合结果。查看当前节点、处理人、意见和审批轨迹。模型审核通过后仍需手动发布；数据与融合结果按业务规则生效。 |
| **数据接入** | 来源系统、接入任务、来源数据 | 支持数据库全量或增量读取、CSV/XLS/XLSX 文件导入及外部 API 拉取。按来源唯一标识定位记录，将来源字段映射到模型字段；内容未变时跳过，有变化时保留采集版本，可按运行批次排查失败记录。 |
| **多源融合** | 重复检测、匹配审核、合并管理 | 根据匹配分值和命中依据确认、拆分或暂缓候选组。单条记录可单独确认；多源记录逐字段处理冲突并保留取值来源。结果通过质量检测和审批后，新增或更新权威主数据。 |
| **数据质量** | 质量任务、问题数据、整改任务 | 手动或定时检测正式主数据，按记录归集失败规则、字段和原因。可将问题项分别派给责任人整改，后续检测通过后标记对应项已解决；问题由管理员确认后关闭。 |
| **数据分发** | 目标系统、分发任务、数据服务 | 为目标 API 配置数据范围、字段映射和调度，推送全量、增量及已分发记录的生命周期变化。也可发布只读 REST API，按主数据 ID 查询单条记录或按条件分页查询。 |
| **授权管理** | 数据授权、服务授权 | 按用户、角色或组织授予模型的行、字段和操作权限，查看最终权限及其来源。已发布 API 单独授权，按调用用户或客户端、服务及有效期控制访问。 |
| **审计追溯** | 操作审计、变更历史、数据版本、来源追溯 | 从成功的业务操作查看受影响对象，跨模型查询变更并对比任意两个正式版本。沿当前或历史映射，从权威记录追溯到人工录入或外部来源记录及相关审批。 |
| **系统管理** | 用户、角色、菜单、部门、岗位、字典、参数、通知公告、日志 | 维护账号和组织结构，通过角色控制菜单、按钮及数据权限。统一维护公共字典和运行参数，发布通知并检索操作与运行日志。 |

💡 如您有好的建议或功能需求，欢迎 [提交Issue](https://gitee.com/qiantongtech/qData-mdm/issues)，与我们共同完善主数据平台功能。

## 开发与扩展

qData-mdm 采用前后端分离与模块化组织方式，便于开发者阅读代码、接入业务系统，并围绕实际项目需求扩展能力。

| 层次 | 主要技术 |
| --- | --- |
| 后端 | Java、Spring Boot、Spring Security、MyBatis-Plus |
| 前端 | Vue 3、Element Plus、Vite |
| 数据存储与缓存 | 关系型数据库、Redis |
| 接口文档与联调 | Knife4j / OpenAPI |
| 图形与统计展示 | AntV X6、ECharts |

具体依赖以对应版本的 [pom.xml](pom.xml) 与前端项目配置为准。数据库部署应使用匹配的驱动、连接配置和初始化脚本。

## QQ交流群

欢迎加入 qData 官方 QQ 交流群，获取最新动态、技术支持与使用交流。 👉 <a href="https://docs.qdata.tech/discuss.html">点击加入 QQ 交流群</a>

<a id="license-and-services"></a>

## 许可与商业服务

qData-mdm 按 **Apache License 2.0 及项目附加条件**提供，支持商业使用。使用时需遵守 [LICENSE](LICENSE)，保留其中要求的 Logo、版权、许可与署名信息。

如需去除或修改相关标识、白标、OEM 或重新包装为其他产品，请取得相应商业授权。品牌定制授权、专业版与技术服务咨询，请查看[商业合作说明](https://docs.qdata.tech/business/policy.html)。