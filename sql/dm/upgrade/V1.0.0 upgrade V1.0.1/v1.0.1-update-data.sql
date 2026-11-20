-- 新增来源数据删除、匹配审核删除的菜单权限。
SET IDENTITY_INSERT "QDATA_MDM"."SYSTEM_MENU" ON;
INSERT INTO "QDATA_MDM"."SYSTEM_MENU"("MENU_ID","MENU_NAME","PARENT_ID","ORDER_NUM","PATH","COMPONENT","QUERY","IS_FRAME","IS_CACHE","ROUTE_NAME","MENU_TYPE","VISIBLE","STATUS","PERMS","ICON","CREATE_BY","CREATE_TIME","UPDATE_BY","UPDATE_TIME","REMARK") VALUES(2497,'来源数据删除',2265,2,'#',NULL,NULL,1,0,NULL,'F','0','0','ingest:sourceData:remove','#',1,'2026-11-18 09:54:28.818665','',NULL,'');
INSERT INTO "QDATA_MDM"."SYSTEM_MENU"("MENU_ID","MENU_NAME","PARENT_ID","ORDER_NUM","PATH","COMPONENT","QUERY","IS_FRAME","IS_CACHE","ROUTE_NAME","MENU_TYPE","VISIBLE","STATUS","PERMS","ICON","CREATE_BY","CREATE_TIME","UPDATE_BY","UPDATE_TIME","REMARK") VALUES(2498,'匹配审核删除',2319,7,'#',NULL,NULL,1,0,NULL,'F','0','0','merge:review:delete','#',1,'2026-11-18 09:54:28.845185','',NULL,'');
SET IDENTITY_INSERT "QDATA_MDM"."SYSTEM_MENU" OFF;

-- 为角色 ID 为 3 的角色授予来源数据删除、匹配审核删除权限。
INSERT INTO "QDATA_MDM"."SYSTEM_ROLE_MENU"("ROLE_ID","MENU_ID","PROJECT_ID") VALUES(3,2497,0);
INSERT INTO "QDATA_MDM"."SYSTEM_ROLE_MENU"("ROLE_ID","MENU_ID","PROJECT_ID") VALUES(3,2498,0);

-- 更新质量任务菜单路径、组件及列表权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PATH" = 'task', "COMPONENT" = 'quality/task/index', "PERMS" = 'quality:task:list' WHERE "MENU_ID" = 2335;
-- 更新质量任务按钮权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:query' WHERE "MENU_ID" = 2336;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:add' WHERE "MENU_ID" = 2337;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:edit' WHERE "MENU_ID" = 2338;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:remove' WHERE "MENU_ID" = 2339;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:export' WHERE "MENU_ID" = 2340;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:task:import' WHERE "MENU_ID" = 2341;
-- 更新质量任务运行记录、执行一次权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:taskInstance:list' WHERE "MENU_ID" = 2398;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:taskInstance:add' WHERE "MENU_ID" = 2438;
-- 更新问题数据菜单路径、组件及列表权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PATH" = 'issue', "COMPONENT" = 'quality/issue/index', "PERMS" = 'quality:issue:list' WHERE "MENU_ID" = 2349;
-- 更新问题数据按钮权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:query' WHERE "MENU_ID" = 2351;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:add' WHERE "MENU_ID" = 2352;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:edit' WHERE "MENU_ID" = 2353;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:remove' WHERE "MENU_ID" = 2354;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:export' WHERE "MENU_ID" = 2355;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issue:import' WHERE "MENU_ID" = 2356;
-- 更新整改任务菜单路径、组件及列表权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PATH" = 'issueTask', "COMPONENT" = 'quality/issueTask/index', "PERMS" = 'quality:issueTask:list' WHERE "MENU_ID" = 2350;
-- 更新整改任务按钮权限。
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:list' WHERE "MENU_ID" = 2374;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:query' WHERE "MENU_ID" = 2375;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:add' WHERE "MENU_ID" = 2376;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:edit' WHERE "MENU_ID" = 2377;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:export' WHERE "MENU_ID" = 2378;
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'quality:issueTask:remove' WHERE "MENU_ID" = 2397;
-- 更新编码方案版本新增按钮权限
UPDATE "QDATA_MDM"."SYSTEM_MENU" SET "PERMS" = 'model:encoding:version:add' WHERE "MENU_ID" = 2394;