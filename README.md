![qData Master Data Platform](images/en-US/banner.png)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--mdm-v1.0.0-blue.svg" alt="qData-mdm v1.0.0"/>

  <img src="https://gitee.com/qiantongtech/qData-mdm/badge/star.svg" alt="Gitee Stars"/>
  <img src="https://img.shields.io/github/stars/qiantongtech/qData-mdm?label=GitHub%20Stars" alt="GitHub Stars"/>
</p>

<p align="center">
  <a href="README.zh-CN.md">📖简体中文</a> | 📖English | <a href="README.ja-JP.md">📖日本語</a>
</p>

## Platform Overview

**qData Master Data Platform** helps organizations manage master data for customers, suppliers, materials, and organizational units. Teams define models by business domain and match records from different systems that refer to the same entity. Once merged records pass quality checks and receive approval, they become authoritative. Downstream systems can receive the same data through distribution tasks or retrieve it through query APIs. Source records and change history show where each master record came from and how it has changed.

✨✨✨**Online Documentation**✨✨✨ <a href="https://qdata.qiantong.tech" target="_blank">https://qdata.qiantong.tech</a>

✨✨✨**Open-Source Edition Demo**✨✨✨ <a href="https://demo-mdm.qdata.tech" target="_blank">https://demo-mdm.qdata.tech</a> (username `qData`, password `qData123`).

✨✨✨**Professional Edition Demo**✨✨✨ <a href="https://pro-demo-mdm.qdata.tech" target="_blank">https://pro-demo-mdm.qdata.tech</a>. Please [contact customer service](https://community.qdata.tech/business/policy.html) to obtain a demo account.

> If qData-mdm helps you, give the project a **star ⭐️** to support its development.

## Feature List

| Module | Components | What you can do |
| --- | --- | --- |
| **Dashboard** | Dashboard | See totals for master records, models, domains, reference items, and active grants within your permissions. Compare record counts by domain and lifecycle state, drill into changes that took effect in the last 7 or 30 days, and follow up on failed publications or expiring grants. |
| **Master data management** | Authoritative records, my requests, data entry tasks, task center | Create, edit, copy, import, and export records under a published model. Bulk changes pass validation and take effect as a single batch. Manage active, frozen, inactive, and archived records; compare versions and request a restore. Track or withdraw your requests and handle approvals, data entry, and remediation in the task center. |
| **Model management** | Domains, model catalog, models, hierarchies and relationships, reference data, coding schemes | Organize standard and classification models by domain and folder. Define fields, model references, and one-to-many submodels; inspect dependencies in the relationship graph. Bind fields to reference values or codes built from multiple segments. Publish manually after validation and approval to create the physical schema. Revisions retain history and undergo compatibility checks. |
| **Rule management** | Matching, survivorship, quality rules | Find duplicate candidates with exact, normalized exact, fuzzy, or weighted multi-field matching. Select field values by source priority, trust score, recency, or preference for non-empty values. Define checks for required values, format, range, uniqueness, reference values, and relationships between fields. |
| **Workflow management** | Workflow configuration, workflow instances | Route models, record edits, lifecycle changes, and merge results for approval. See the current step, assignee, comments, and decision history. Approved models still require manual publication; approved record changes and merge results take effect according to business rules. |
| **Data ingestion** | Source systems, ingestion tasks, source records | Read databases in full or incrementally, import CSV/XLS/XLSX files, or pull from external APIs. Identify records by source key and map incoming fields to the model. Unchanged records are skipped; changed records get a new source version. Inspect failures by run. |
| **Multi-source consolidation** | Duplicate detection, match review, merge management | Review match scores and evidence to confirm, split, or defer candidate groups. Confirm single records individually or resolve conflicting values field by field, retaining their sources. Results must pass quality checks and approval before they create or update authoritative records. |
| **Data quality** | Quality tasks, issue records, remediation tasks | Check authoritative records manually or on a schedule. Group failures by record, inspect the rule, field, and cause, and assign individual issues for correction. A later passing check marks the item resolved; an administrator confirms closure of the issue record. |
| **Data distribution** | Target systems, distribution tasks, data services | Set the record scope, field mappings, and schedule for each target API. Push full or incremental updates, including lifecycle changes for previously distributed records. Publish read-only REST services for lookup by master record ID or filtered, paginated queries. |
| **Access management** | Data grants, service grants | Grant row, field, and operation permissions by user, role, or organization. Inspect effective access and see which grants provide it. Authorize published APIs separately by calling user or client, service, and validity period. |
| **Audit and traceability** | Operation audit, change history, data versions, source tracing | Inspect records affected by successful operations, search changes across models, and compare any two official versions. Follow current and historical mappings from an authoritative record to manual entries or external source records and their associated approvals. |
| **System administration** | Users, roles, menus, departments, positions, dictionaries, settings, notices, logs | Manage accounts and the organization hierarchy. Use roles to control menu, button, and data access. Maintain shared lookup values and runtime settings, publish notices, and search operation and system logs. |

💡 Have a suggestion or feature request? [Open an issue](https://gitee.com/qiantongtech/qData-mdm/issues).

## Technology Stack

qData-mdm separates the Spring Boot backend from the Vue 3 frontend and integrates authentication, database access, caching, and UI components.

<table>
  <tr>
    <th>Category</th><th>Technology</th><th>Purpose</th>
  </tr>
  <tr>
    <td rowspan="6">Backend</td><td>Spring Boot</td><td>Application framework for rapid development and consistent startup</td>
  </tr>
  <tr>
    <td>Spring Security</td><td>User authentication, authorization, and security controls</td>
  </tr>
  <tr>
    <td>MySQL, DM8</td><td>Persistent storage and data source configuration</td>
  </tr>
  <tr>
    <td>MyBatis-Plus</td><td>Database access and multiple data sources</td>
  </tr>
  <tr>
    <td>Redis</td><td>Caching, session state, and middleware</td>
  </tr>
  <tr>
    <td>Knife4j / OpenAPI</td><td>API documentation and integration testing</td>
  </tr>

  <tr>
    <td rowspan="3">Frontend</td><td>Vue 3</td><td>Reactive frontend framework</td>
  </tr>
  <tr>
    <td>Element Plus</td><td>UI components for the admin interface</td>
  </tr>
  <tr>
    <td>Vite</td><td>Development and build tooling</td>
  </tr>

  <tr>
    <td rowspan="2">Third-party libraries</td><td>AntV X6</td><td>Workflow diagrams, visual design, and interaction</td>
  </tr>
  <tr>
    <td>ECharts</td><td>Charts and data visualization</td>
  </tr>
</table>

## Commercial Licensing

qData-mdm provides two editions: **Professional Edition** and **Open-Source Edition**, meeting user needs across different scales and scenarios. Each has its own focus while complementing the other: the Open-Source Edition helps users get started at low cost, while the Professional Edition provides greater depth and support. Whichever edition you choose, qData-mdm aims to be a reliable partner that helps enterprises unlock data value and accelerate digital transformation.

> 👉 For **Open-Source Edition brand authorization** or **Professional Edition consultation**, see [💼 Licensing Details](https://community.qdata.tech/business/policy.html).

## Quick Start

### 1. Online Demo

- Sign in to the [Community Open-Source Edition demo](#platform-overview) to explore the main features without installing anything.

### 2. Choose a Deployment Method

- **Local deployment**. Use the Docker Compose configuration in `deploy/docker/` to start the full environment locally.
- **Run from source**. Start the `qdata-mdm-server` backend and `qdata-mdm-ui` frontend separately for development and integration testing.
- **Self-managed deployment**. Install, configure, and manage the services manually for production or custom environments.

## Deployment Environment

Install the following prerequisites before deploying qData-mdm.

<table>
  <tr>
    <th>Environment</th><th>Component</th><th>Recommended version</th><th>Notes</th>
  </tr>
  <tr>
    <td rowspan="6">Backend</td><td>JDK</td><td>1.8 or later</td><td>OpenJDK 8 recommended</td>
  </tr>
  <tr>
    <td>Maven</td><td>3.6+</td><td>Build and dependency management</td>
  </tr>
  <tr>
    <td>DM8 / MySQL</td><td>8.0+</td><td>Relational database. Development configuration defaults to DM8.</td>
  </tr>
  <tr>
    <td>Redis</td><td>5.0+</td><td>Caching and session state</td>
  </tr>
  <tr>
    <td>Docker / Docker Compose</td><td>Optional</td><td>Quick evaluation and test deployments</td>
  </tr>
  <tr>
    <td>Operating system</td><td>Windows / Linux / Mac</td><td>All listed systems are supported</td>
  </tr>

  <tr>
    <td rowspan="3">Frontend</td><td>Node.js</td><td>16+</td><td>Frontend build tooling</td>
  </tr>
  <tr>
    <td>npm</td><td>8+</td><td>Package manager</td>
  </tr>
  <tr>
    <td>Chrome / Edge</td><td>Latest</td><td>Recommended for local debugging and access</td>
  </tr>
</table>

## QQ Community

Join the official qData QQ community for updates, technical support, and discussions.

👉 <a href="https://community.qdata.tech/discuss.html">Join the QQ community</a>

## System Screenshots

The screenshots show the Simplified Chinese interface.

<table>
  <tr>
    <td><img src="images/zh-CN/system/login.png" alt="登录" width="400"/></td>
    <td><img src="images/zh-CN/system/index.png" alt="首页" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/master.png" alt="权威主数据" width="400"/></td>
    <td><img src="images/zh-CN/system/project.png" alt="主数据域" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/layer.png" alt="模型目录" width="400"/></td>
    <td><img src="images/zh-CN/system/model.png" alt="模型定义" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/relation.png" alt="模型关系" width="400"/></td>
    <td><img src="images/zh-CN/system/match.png" alt="匹配规则" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/survivor.png" alt="生存规则" width="400"/></td>
    <td><img src="images/zh-CN/system/qualitys.png" alt="质量规则" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/datasource.png" alt="数据接入" width="400"/></td>
    <td><img src="images/zh-CN/system/datasource_detail.png" alt="测试连接" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/ingest_task.png" alt="接入任务" width="400"/></td>
    <td><img src="images/zh-CN/system/review.png" alt="匹配审核" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/manage.png" alt="合并管理" width="400"/></td>
    <td><img src="images/zh-CN/system/quality_task.png" alt="质量任务" width="400"/></td>
  </tr>
  <tr>
    <td><img src="images/zh-CN/system/quality_issue_task.png" alt="整改任务" width="400"/></td>
    <td><img src="images/zh-CN/system/operation_log.png" alt="操作审计" width="400"/></td>
  </tr>
</table>
