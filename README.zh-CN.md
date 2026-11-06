# qData-mdm

qData 主数据平台基础项目。模块命名和 Java 包结构与参考项目 qData-mdm 保持一致，当前仅保留系统基础能力。

| 目录 | 用途 |
| --- | --- |
| `qdata-mdm-framework` | 通用组件、安全、配置、持久化、文件、认证及代码生成等基础组件 |
| `qdata-mdm-module-system` | 用户、角色、菜单、部门、字典、通知等系统管理，分为 api 和 biz |
| `qdata-mdm-server` | 后端服务启动入口（server） |
| `qdata-mdm-ui` | Vue 前端，仅保留系统页面、接口和路由 |

Maven 根坐标：`tech.qiantong:qData-mdm:0.6.0`。
Java 包前缀：`tech.qiantong.qdata.mdm`；系统模块使用 `tech.qiantong.qdata.mdm.system`。

## 本地开发

需要 JDK 8、Maven 3.6+、Node.js 18+、MySQL 8 和 Redis。

1. 导入 `sql/mysql/qdata-mdm.sql`，创建 `qdata_mdm` 数据库。脚本只包含 system/framework 所需表，保留系统菜单并清理业务菜单及关联关系。DM8 使用 `sql/dm/qdata-mdm.sql`，先创建 `QDATA_MDM` 用户/模式。
2. 配置 `qdata-mdm-server/src/main/resources/application-dev.yml` 中数据库和 Redis。MySQL 密码可通过 `MYSQL_PASSWORD` 环境变量覆盖。
3. 在根目录执行 `mvn -DskipTests package`，运行 `java -jar qdata-mdm-server/target/qdata-mdm-server.jar --spring.profiles.active=dev`。
4. 在 `qdata-mdm-ui` 执行 `npm ci`、`npm run dev`。开发代理连接 `http://localhost:8080`。

生产构建：`mvn -DskipTests package`，前端执行 `npm run build:prod`。

## Docker 部署

完成前后端生产构建后：

```sh
cp docker/.env.example docker/.env
# 按实际环境修改 docker/.env 中数据库密码
docker compose -f docker/docker-compose.yml up -d --build
```

前端访问端口 80，后端端口 8080。默认仅启动 MySQL、Redis、server 和 ui。
MySQL 仅在全新数据卷首次启动时执行初始化脚本。已有数据库需自行迁移菜单和数据；本次修改不会操作正在运行的数据库。

基础框架中的扩展组件保留；数据资产、数据开发、数据服务、元数据、AI、质量执行器等独立业务模块已移除。

原有版权、标识和许可条款见 [LICENSE](LICENSE)。
