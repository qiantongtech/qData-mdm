![qData-mdm: open-source master data management](images/en-US/banner.png)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--mdm-v1.0.2-blue.svg" alt="qData-mdm v1.0.2"/>

  <img src="https://gitee.com/qiantongtech/qData-mdm/badge/star.svg" alt="Gitee Stars"/>
  <img src="https://img.shields.io/github/stars/qiantongtech/qData-mdm?label=GitHub%20Stars" alt="GitHub Stars"/>
  <img src="https://atomgit.com/qiantongtech/qData-mdm/star/badge.svg"/>
</p>

<p align="center">
  <a href="README.zh-CN.md">📖简体中文</a> | 📖English | <a href="README.ja-JP.md">📖日本語</a>
</p>

## Platform overview

**Keep master data consistent across business systems.**

**qData Master Data Platform (qData-mdm)** is an open-source master data management platform. Define data models and standards, maintain core business records in one place, run quality checks, and share master data across systems through APIs.

Start with suppliers, materials, or equipment. Maintain records in one place and apply shared reference values and code generation rules to reduce duplicate records and inconsistent codes. Share the same master records with purchasing, inventory, and finance systems through REST APIs or scheduled distribution. Developers can extend the modules and reuse the built-in data management and access controls.

✨✨✨**Documentation**✨✨✨ <a href="https://docs.qdata.tech/en/" target="_blank">https://docs.qdata.tech/en/</a>

✨✨✨**Open-Source Edition demo**✨✨✨ <a href="https://demo-mdm.qdata.tech" target="_blank">https://demo-mdm.qdata.tech</a> (username `qData`, password `qData123`)

✨✨✨**Professional Edition demo**✨✨✨ <a href="https://pro-demo-mdm.qdata.tech" target="_blank">https://pro-demo-mdm.qdata.tech</a> · [Request a demo account](https://docs.qdata.tech/en/business/policy.html)

> If qData-mdm is useful to you, give the repository a **star ⭐️**! 🚀

qData-mdm is available under **Apache License 2.0 with additional project terms**. See [LICENSE](LICENSE) and [commercial licensing](#license-and-services).

## Key features

### Define master data models once and reuse them

Define models and fields for entities such as customers, suppliers, materials, and equipment. Group models by domain and in a model catalog. Use the same model for record maintenance and API queries to avoid rebuilding forms, code generation logic, and lookup endpoints for every entity.

### Keep reference values and codes consistent

Manage classifications, allowed values, and identifier formats in one place. Build codes from literal text, business fields, reference data mappings, timestamps, and sequence numbers.

### Find and fix data quality issues

Run on-demand or scheduled checks on master records to identify the affected records, fields, and validation failures. Assign remediation tasks, track progress, and rerun checks after corrections.

### Share master records through REST APIs and data distribution

Publish **read-only REST APIs** from approved, published models. Configure filters, response fields, and access grants for individual record lookups and paginated queries. Map master data fields to target API requests and push full or incremental datasets on demand or on a schedule.

### Extend the software for your workflows

The Java, Spring Boot, and Vue 3 codebase is organized by capability, including modeling, master records, data quality, ingestion, and distribution. Extend validation rules or customize workflows while reusing existing data management and authorization.

## Quick start

### Try the live demo

Open the [qData-mdm Open-Source Edition demo](https://demo-mdm.qdata.tech) to explore the software in your browser.

**Username `qData` Password `qData123`**

Pick an existing master data model and explore how its records are defined, maintained, checked, and shared.

1. **Modeling and standards.** Review model fields, reference data, and code generation rules.
2. **Master record maintenance.** Open a master record and review the business fields that teams maintain centrally.
3. **Data quality.** Review quality jobs, flagged records, and remediation tasks.
4. **Data sharing.** Inspect a published data service or distribution job to see how other systems receive master data.

With the appropriate permissions, try publishing a query API. Select a published model, choose filters and response fields, publish the service, and grant access. Use the URL on the publishing page to retrieve one record or a page of results.

### Choose a deployment method

- **Docker Compose.** Run these commands from the repository root to start the full environment locally.

  ```bash
  cd deploy/docker
  docker compose -p qdata-mdm-ce --profile all up -d
  ```

- **Run from source.** Start the backend `qdata-mdm-server` and frontend `qdata-mdm-ui` separately for development and integration testing.
- **Manual deployment.** Install and configure each service for production or a customized environment.

## Product architecture

![qData-mdm open-source edition architecture showing modeling, data standards, ingestion, governance, APIs, distribution, and access controls](images/en-US/architecture.png)

## Screenshots

![Sign-in screen](images/zh-CN/system/login.png)

![Dashboard](images/zh-CN/system/index.png)

![Master records](images/zh-CN/system/master.png)

![Master record list](images/zh-CN/system/master_detail.png)

![Master data domain details](images/zh-CN/system/project_detail.png)

![Model catalog details](images/zh-CN/system/layer_detail.png)

![Master data models](images/zh-CN/system/model.png)

![Model details](images/zh-CN/system/model_detail.png)

![Model configuration basics](images/zh-CN/system/model_form_one.png)

![One-to-many child model configuration](images/zh-CN/system/model_form_four.png)

![Model relationships](images/zh-CN/system/relation.png)

![Model relationship details](images/zh-CN/system/relation_detail.png)

![Record matching rule](images/zh-CN/system/match_form.png)

![Field survivorship rule](images/zh-CN/system/survivor_form.png)

![Data quality rule](images/zh-CN/system/quality_form.png)

![Review of potential matches](images/zh-CN/system/review.png)

![Data connections](images/zh-CN/system/datasource.png)

![Data connection test](images/zh-CN/system/datasource_detail.png)

![Ingestion jobs](images/zh-CN/system/ingest_task.png)

![Ingestion job details](images/zh-CN/system/ingest_task_detail.png)

![Data quality jobs](images/zh-CN/system/quality_task.png)

![Data quality job configuration](images/zh-CN/system/quality_task_form.png)

![Data access grant for a principal and model](images/zh-CN/system/data_auth_form_one.png)

![Operations and expiration for a data access grant](images/zh-CN/system/data_auth_form_four.png)

![Data quality remediation tasks](images/zh-CN/system/quality_issue_task.png)

![Audit log](images/zh-CN/system/operation_log.png)

## Capabilities overview

qData-mdm aims to give ERP, CRM, SRM, and WMS applications one set of customer, supplier, and material data, with a common identifier, an authoritative record, and traceable sources for each entity.

| Capability | What it enables |
| --- | --- |
| **Multi-source data ingestion** | Bring supplier records from ERP databases, spreadsheets, and external APIs into the same supplier model. Load database records in full or sync changes incrementally, and track source record changes by version. |
| **Shared models and automatic identifiers** | Define fields, classifications, relationships, and details for customers, suppliers, and materials. Use shared reference data and code generation rules to keep material classifications and business identifiers consistent across systems. |
| **Cross-system deduplication and consolidation** | Use name similarity and multi-field matching to identify the same supplier under different names. Recommend field values based on rules such as source reliability. Users resolve conflicts, and quality checks and approval turn the result into a traceable master record. |
| **10+ quality checks with tracked remediation** | Schedule checks for duplicate material codes, invalid Chinese business registration identifiers, and start dates later than end dates. Pinpoint affected fields, assign owners, and verify fixes with follow-up checks. |
| **Lifecycle management and version history** | Manage records from activation through freezing, deactivation, and archiving. Compare past versions, review the reasons and approvals behind changes, and restore earlier content through approval. |
| **Master data distribution and query APIs** | Set the data scope and fields for each destination, then push full datasets, incremental updates, and lifecycle changes for previously distributed records. Offer authorized lookups by master record ID and paginated queries through read-only REST APIs. |
| **Fine-grained permissions and audit trails** | Set row, field, and operation permissions by user, role, or organizational unit. Manage API access for service clients independently, and trace permission changes, business operations, and source records. |

💡 Have feedback or a feature request? [Open an issue](https://gitee.com/qiantongtech/qData-mdm/issues).

## Development and extensions

qData-mdm separates the frontend and backend and organizes the code into modules so teams can integrate it with other systems and extend features as needed.

| Layer | Technologies |
| --- | --- |
| Backend | Java, Spring Boot, Spring Security, MyBatis-Plus |
| Frontend | Vue 3, Element Plus, Vite |
| Storage and caching | Relational database, Redis |
| API documentation and testing | Knife4j, OpenAPI |
| Diagrams and charts | AntV X6, ECharts |

## QQ community

Follow project updates, ask technical questions, and talk with other users. 👉 <a href="https://docs.qdata.tech/en/discuss.html">Join the official qData QQ group</a>

<a id="license-and-services"></a>

## License and commercial services

qData-mdm is available for commercial use under **Apache License 2.0 with additional project terms**. Follow the [LICENSE](LICENSE), including its requirements for logos, copyright notices, license notices, and attribution.

Obtain separate commercial authorization to remove or modify required project branding, white-label the software, offer it as an OEM product, or repackage it as another product. For custom branding, the Professional Edition, or technical services, see [commercial licensing and support](https://docs.qdata.tech/en/business/policy.html).

## Our mission and vision

**Make it affordable for every organization to build a consistent, trustworthy master data system it can maintain over time.**

We believe organizations of all sizes should have access to master data management, even when budgets and IT resources are limited.\
Open source makes qData-mdm easier to adopt. Flexible modeling fits different business domains, while record matching and consolidation turn fragmented data into accurate, trusted master records.

Our vision is to build an open, easy-to-use, reliable MDM solution with the community. We want organizations to retain control of their core data, agree on a common business vocabulary, and share trusted data across systems to support their digital operations and future innovation.
