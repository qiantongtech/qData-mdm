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

package tech.qiantong.qdata.mdm.model.util;

import com.alibaba.fastjson2.JSONObject;
import tech.qiantong.qdata.mdm.model.controller.admin.base.vo.ModelBasePublishRespVO;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishContext;
import tech.qiantong.qdata.mdm.model.domain.ModelPublishRemainingEffect;
import tech.qiantong.qdata.mdm.model.domain.ModelSqlExecutionStep;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelPublishStatus;
import tech.qiantong.qdata.mdm.model.enums.materialization.ModelSqlStepStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布流程的无状态工具：读取模型配置、判断标记，并把已有执行结果整理成接口响应。
 * 不查数据库，不持有连接，不修改配置或发布状态。
 */
public final class ModelPublishUtils {

    /**
     * 工具类不需要创建实例。
     */
    private ModelPublishUtils() {
    }

    /**
     * 物理名称只接受英文字母、数字和下划线，和模型编码的字符范围一致，不另加首字符限制。
     * 大小写及关键字由配置规则另行检查；这里仅防止把SQL表达式当作名称。
     */
    public static boolean identifier(String name) {
        return name != null && name.matches("[A-Za-z0-9_]+");
    }

    /**
     * 按主模型在前、直接子模型在后的顺序取配置，格式不对的条目交给配置规则报告。
     *
     * @param context 本次发布数据
     * @return 能正常读取的模型配置列表
     */
    public static List<JSONObject> models(ModelPublishContext context) {
        List<JSONObject> result = new ArrayList<>();
        // 统一把主模型和直接子模型展开成列表，后续构建和校验都用相同的模型范围。
        JSONObject root = context.getSnapshot().getModel();
        if (root != null) {
            result.add(root);
            result.addAll(objects(root, "subModels"));
        }
        return result;
    }

    /**
     * 只读取真正的JSON对象数组，避免错误格式让后面的规则空指针报错。
     *
     * @param object 所属配置对象
     * @param key 数组属性名
     * @return 数组中的对象，非对象条目由完整性规则另行报错
     */
    public static List<JSONObject> objects(JSONObject object, String key) {
        List<JSONObject> result = new ArrayList<>();
        if (object == null || !(object.get(key) instanceof List)) {
            return result;
        }
        for (Object item : (List<?>) object.get(key)) {
            if (item instanceof JSONObject) {
                result.add((JSONObject) item);
            }
        }
        return result;
    }

    /**
     * 判断模型是否正常使用；子模型沿用主版本时，以配置中的删除标记及版本状态判断。
     *
     * @param model 模型配置
     * @return 未删除且未封存时为true
     */
    public static boolean active(JSONObject model) {
        if (Boolean.TRUE.equals(flag(model, "delFlag"))) {
            return false;
        }
        JSONObject version = model.getJSONObject("version");
        Long status = version == null ? null : version.getLong("status");
        return status == null || (status != 3 && status != 4);
    }

    /**
     * 生成错误位置，使用固定ID帮助定位，不把字段编码当成唯一身份。
     *
     * @param model 模型配置
     * @return 包含模型名称及ID的位置说明
     */
    public static String path(JSONObject model) {
        return "模型[" + model.getString("name") + ", ID=" + model.getString("id") + "]";
    }

    /**
     * 读取页面布尔值和数据库0/1标记。错误值返回null，由规则给出具体提示，不在取值时直接报解析异常。
     *
     * @param object 配置对象
     * @param key 标记属性名
     * @return true、false或未配置/无法识别时的null
     */
    public static Boolean flag(JSONObject object, String key) {
        Object value = object.get(key);
        if (value == null) {
            return null;
        }
        if ("1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString())) {
            return true;
        }
        if ("0".equals(value.toString()) || "false".equalsIgnoreCase(value.toString())) {
            return false;
        }
        return null;
    }

    /** 使用当前已经取得的执行证据生成响应，不额外查库，也不修改实例状态。 */
    public static ModelBasePublishRespVO toResponse(ModelPublishContext context, String failureReason) {
        ModelBasePublishRespVO result = new ModelBasePublishRespVO();
        result.setModelId(context.getModelId());
        result.setInstanceId(context.getInstanceId());
        result.setModelVersionId(context.getModelVersionId());
        result.setBaseInstanceId(context.getBaseInstanceId());
        if (context.getStatus() == ModelPublishStatus.SUCCEEDED) {
            result.setPublishedModelVersionId(context.getModelVersionId());
        }
        result.setStatus(context.getStatus() == null ? null : context.getStatus().name());
        result.setCurrentStage(context.getCurrentStage() == null ? null : context.getCurrentStage().name());
        result.setFailureReason(failureReason);
        if (failureReason != null) {
            // 原实例可能已经处于补偿中；无法证明原始阶段时留空，不把补偿阶段冒充原始失败阶段。
            result.setFailureStage("COMPENSATE".equals(result.getCurrentStage()) ? null : result.getCurrentStage());
        }
        if (context.getExecutionPlan() != null) {
            result.setDdlTransactionStatus(context.getExecutionPlan().getDdlTransactionStatus().name());
            for (ModelSqlExecutionStep step : context.getExecutionPlan().getSteps()) {
                ModelBasePublishRespVO.ModelPublishStepRespVO item = new ModelBasePublishRespVO.ModelPublishStepRespVO();
                item.setStepNo(step.getStepNo());
                item.setStatus(step.getStatus().name());
                item.setCompensationStatus(step.getCompensationStatus().name());
                result.getSteps().add(item);
                if (result.getFailedStepNo() == null && (step.getStatus() == ModelSqlStepStatus.FAILED
                        || step.getStatus() == ModelSqlStepStatus.UNKNOWN)) {
                    result.setFailedStepNo(step.getStepNo());
                    // 失败步骤号仍可定位原计划；这里不猜测是发送SQL还是核验时失败。
                }
            }
        }
        for (ModelPublishRemainingEffect effect : context.getRemainingEffects()) {
            ModelBasePublishRespVO.ModelPublishRemainingEffectRespVO item = new ModelBasePublishRespVO.ModelPublishRemainingEffectRespVO();
            item.setStepNo(effect.getStepNo());
            item.setVerificationStatus(effect.getVerificationStatus() == null ? null : effect.getVerificationStatus().name());
            if (effect.getObject() != null) {
                item.setTableName(effect.getObject().getTableName());
                item.setColumnName(effect.getObject().getColumnName());
                item.setIndexName(effect.getObject().getIndexName());
            }
            result.getRemainingEffects().add(item);
        }
        return result;
    }
}
