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

package tech.qiantong.qdata.mdm.system.domain.vo;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * <P>
 * 用途:
 * </p>
 *
 * @author: FXB
 * @create: 2025-07-03 16:04
 **/
@Data
public class GetResponseColumnReqVO {
    /**
     * 接口地址
     */
    private String apiUrl;

    /**
     * API方法 GET、POST、PUT、DELETE
     */
    private String apiMethod;

    /**
     * 返回数据格式 1：JSON对象 2：JSON数组 3：XML(目前只做JSON对象)
     */
    private Integer returnFormat;
    /**
     * 返回数据行所在字段 如API返回格式为:{status:1,rows:[{}...]}则此处填写[$.rows],
     * 支持添加多个如果rows为json字符串就需要再配置一个例如 {status:1,data:"\"rows\":[{}...]"}  [$.data,$.rows]
     */
    private List<String> returnDataLine;

    /**
     * 是否分页 0:否 1:是
     */
    private Integer pageFlag;

    /**
     * 分页参数
     */
    private Page page;

    /**
     * 请求参数
     */
    private InParams inParams;

    /**
     * 请求头(需校验不能添加重复的name)
     */
    private List<JSONObject> apiHeaders;

    @Data
    public class Page {
        //分页参数key，需在参数中用${pageNo}进行占位，pageNo为当前参数的值
        private String pageNoKey;
        //最大页数
        private Integer maxPage;
    }

    @Data
    public class InParams {
        //参数类型 1:表单参数 2:body参数
        private Integer type;
        //参数列表
        private List<Map<String, Object>> urlParams;
        //body参数
        private String bodyParams;
    }
}
