-- Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.
--
-- This file is part of qData-mdm Master Data Platform (Open Source Edition).
--
-- qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
-- You may use qData-mdm for commercial purposes, but you may not remove, hide,
-- modify, or replace the qData-mdm logo, copyright notices, license notices,
-- or attribution information without a separate commercial license.
--
-- White-label use, OEM distribution, rebranding, or presenting qData-mdm as
-- another product requires separate commercial authorization from
-- Jiangsu Qiantong Technology Co., Ltd.
--
-- Business License: https://docs.qdata.tech/business/policy.html
-- See the LICENSE file in the project root for full license information.

-- 重复检测任务执行记录新增 LOG_CONTENT 字段，用于归档完整过程日志。
ALTER TABLE "QDATA_MDM"."MERGE_DUPLICATE_TASK_INSTANCE" ADD "LOG_CONTENT" TEXT;
COMMENT ON COLUMN "QDATA_MDM"."MERGE_DUPLICATE_TASK_INSTANCE"."LOG_CONTENT" IS '重复检测完整过程日志JSON，执行结束时归档';
