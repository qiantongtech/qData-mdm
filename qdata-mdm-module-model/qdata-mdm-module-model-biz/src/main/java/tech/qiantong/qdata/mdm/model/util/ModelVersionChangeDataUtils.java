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

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一份变更数据存两种内容：外层存各属性目前的最新值，_history 存每次修改的完整过程。
 * 这样版本对比能看到最终结果，也能查到中间是谁、在什么时候、把什么值改成了什么。
 */
public final class ModelVersionChangeDataUtils {
    private ModelVersionChangeDataUtils() {
    }

    /** 在当前版本的原记录后追加一次修改，再把本次新值合并到累计结果里。 */
    public static String append(String existing, Map<String, Object> before, Map<String, Object> after,
                                Long userId, String updateBy, Date updateTime) {
        // 两边都没变化就原样返回，不为了点了一次保存按钮而制造一条空历史。
        if (before.isEmpty() && after.isEmpty()) return existing;
        JSONObject data = parse(existing);
        JSONArray history = data.getJSONArray("_history");
        // 老格式没有操作历史数组，先创建一个，原来已经有的数据也要保留下来。
        if (history == null) {
            history = new JSONArray(JSONConfig.create().setIgnoreNullValue(false));
            if (!data.isEmpty()) {
                // 旧格式只有快照，保留它，但不伪造此前已丢失的操作明细。
                Map<String, Object> legacy = new LinkedHashMap<>();
                legacy.put("legacySnapshot", new LinkedHashMap<>(data));
                history.add(legacy);
            }
        }
        // 每次操作单独存前后值、操作人和时间，不改动前几次的历史。
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("before", before);
        change.put("after", after);
        change.put("userId", userId);
        change.put("updateBy", updateBy);
        // 请求没传操作时间时，用当前时间补上，避免历史记录缺少时间。
        change.put("updateTime", DateUtil.formatDateTime(updateTime == null ? new Date() : updateTime));
        history.add(change);
        // 历史追加完，再更新外层最新值；保留 null，才能表示“原来的值被明确清空了”。
        merge(data, new JSONObject(after, JSONConfig.create().setIgnoreNullValue(false)));
        data.set("_history", history);
        return data.toString();
    }

    /** 给旧版本补上需要对比的原值，只补数据，不新增操作历史，也不清掉已有历史。 */
    public static String mergeSnapshot(String existing, Map<String, Object> snapshot) {
        JSONObject data = parse(existing);
        merge(data, new JSONObject(snapshot, JSONConfig.create().setIgnoreNullValue(false)));
        return data.toString();
    }

    /**
     * 把本次变化合并到已有结果，而不是用本次列表替换整个旧列表。
     * 例如先改字段名称、再改说明，合并后名称和说明都保留，各自使用最近一次修改的值。
     */
    private static void merge(JSONObject target, JSONObject changes) {
        for (String key : changes.keySet()) {
            // 字段和子模型是明细列表，要逐条找对应记录合并，不能把其他记录一起覆盖掉。
            if (("fields".equals(key) || "subModels".equals(key)) && changes.get(key) instanceof JSONArray) {
                JSONArray current = target.getJSONArray(key);
                if (current == null) current = new JSONArray(JSONConfig.create().setIgnoreNullValue(false));
                JSONArray updates = changes.getJSONArray(key);
                for (int index = 0; index < updates.size(); index++) {
                    JSONObject update = updates.getJSONObject(index);
                    JSONObject matched = null;
                    // 在累计结果中找同一条明细：ID 一样或编码一样，就合并到它原来的位置。
                    // 新字段第一次记录可能还没 ID，后面有 ID 了仍能通过编码认出它。
                    for (int existingIndex = 0; existingIndex < current.size(); existingIndex++) {
                        JSONObject existing = current.getJSONObject(existingIndex);
                        if ((update.getLong("id") != null && update.getLong("id").equals(existing.getLong("id")))
                                || (update.getStr("code") != null && update.getStr("code").equals(existing.getStr("code")))) {
                            matched = existing;
                            break;
                        }
                    }
                    // 累计结果中没出现过这条明细，就直接加进去；出现过就只更新本次带来的属性。
                    if (matched == null) current.add(update);
                    else {
                        // 本次不是删除记录，说明该明细仍存在，清掉以前可能留下的删除标记。
                        if (!update.containsKey("_deleted")) matched.remove("_deleted");
                        // 子模型里还可能有字段和下级子模型，继续使用同样的合并规则。
                        merge(matched, update);
                    }
                }
                target.set(key, current);
            } else {
                // 普通属性只更新本次提交的值；本次没提交的其他属性仍留在累计结果中。
                target.set(key, changes.get(key));
            }
        }
    }

    /** 没有旧记录就从空对象开始；有旧记录就解析，并保留其中显式写入的 null。 */
    private static JSONObject parse(String existing) {
        JSONConfig config = JSONConfig.create().setIgnoreNullValue(false);
        return existing == null || existing.trim().isEmpty()
                ? new JSONObject(config) : JSONUtil.parseObj(existing, config);
    }
}
