![qData主数据平台横幅](images/zh-CN/banner.jpg)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--mdm-v1.0.2blue.svg" alt="qData-mdm v1.0.2"/>

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

- **Docker Compose 本地部署**。在仓库根目录执行以下命令，启动完整环境。

  ```bash
  cd deploy/docker
  docker compose -p qdata-mdm-ce --profile all up -d
  ```

- **源码启动**：分别启动后端 `qdata-mdm-server` 和前端 `qdata-mdm-ui`，适合开发和联调。
- **自主部署**：手动安装、配置和管理各项服务，适合生产环境和个性化配置。

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

qData-mdm 的目标是让 ERP、CRM、SRM、WMS 等系统共用一套客户、供应商和物料主数据，让同一对象有统一编码、权威记录和可追溯的来源。

| 核心能力 | 能力亮点 |
| --- | --- |
| **多源数据接入** | 将 ERP 数据库、供应商台账和外部 API 中的记录汇入同一供应商模型。数据库支持全量采集与增量同步，来源记录的变化可按版本追溯。 |
| **统一建模与自动编码** | 用模型定义客户、供应商和物料的字段、分类、关联及明细。通过共享参考数据和编码规则，让各系统沿用同一套物料分类和业务编码。 |
| **跨系统去重与融合** | 结合名称相似度和多字段匹配，识别不同系统中名称写法不同的同一供应商。按来源可信度等规则推荐字段取值，由业务人员确认冲突项，经质量检查和审批后形成可追溯的权威记录。 |
| **10+ 项质量检查与整改闭环** | 定时检查物料编码重复、统一社会信用代码格式异常、起止日期倒置等问题。定位问题字段、指派责任人整改，并复检确认结果。 |
| **全生命周期与版本追溯** | 管理权威记录的生效、冻结、失效和归档。对比历史版本，查看变更依据和审批记录，并经审批恢复历史内容。 |
| **主数据分发与开放查询** | 按下游系统所需的数据范围和字段，推送全量、增量及已分发记录的状态变化。业务系统也可通过授权的只读 REST API 按主数据 ID 或分页查询。 |
| **细粒度授权与审计追溯** | 按用户、角色和组织配置行、字段及操作权限，独立管理服务客户端的 API 调用授权。可追溯授权变更、业务操作和原始记录。 |

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

## QQ交流群

欢迎加入 qData 官方 QQ 交流群，获取最新动态、技术支持与使用交流。 👉 <a href="https://docs.qdata.tech/discuss.html">点击加入 QQ 交流群</a>

<a id="license-and-services"></a>

## 许可与商业服务

qData-mdm 按 **Apache License 2.0 及项目附加条件**提供，支持商业使用。使用时需遵守 [LICENSE](LICENSE)，保留其中要求的 Logo、版权、许可与署名信息。

如需去除或修改相关标识、白标、OEM 或重新包装为其他产品，请取得相应商业授权。品牌定制授权、专业版与技术服务咨询，请查看[商业合作说明](https://docs.qdata.tech/business/policy.html)。

## 我们的使命与愿景

**让每一家企业，都能以更低的成本建立统一、可信、可持续管理的主数据体系。**

我们相信，主数据管理不应只是大型企业的专属能力，也不应被复杂的系统和高昂的成本所限制。\
qData 主数据平台以开源降低使用门槛，以灵活建模适应不同业务，以智能匹配与多源融合打破数据孤岛，让分散在各个系统中的核心业务数据形成统一、准确、可信的权威数据。

我们希望与社区共同打造一个开放、易用、可靠的主数据平台，让更多企业能够自主掌握核心数据，建立统一的数据语言，让可信数据在业务系统之间高效流转，持续支撑企业的数字化发展与创新。
