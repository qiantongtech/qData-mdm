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

package tech.qiantong.qdata.mdm.model.base.dto;

import lombok.Data;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictTypeRespDTO;
import tech.qiantong.qdata.mdm.model.dict.dto.ModelDictDataRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingVersionRespDTO;
import tech.qiantong.qdata.mdm.model.encoding.dto.ModelEncodingItemRespDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 同一次成功发布的字段、子模型和引用配置，不混入当前可编辑配置。
 */
@Data
public class ModelPublishedStructureRespDTO {
    /** 与字段定义来自同一成功快照的基础信息。 */
    private ModelPublishedRespDTO model;
    /** 按需读取的启用权威列；数据源、Schema和表名复用model中的权威绑定。 */
    private List<ModelMaterializedColumnRespDTO> authorityColumns = new ArrayList<>();
    /**
     * 主模型ID。
     */
    private Long modelId;
    /**
     * 本响应对应的成功发布实例ID。
     */
    private Long instanceId;
    /**
     * 本响应对应的成功模型版本ID。
     */
    private Long versionId;
    /**
     * 本响应对应的成功模型版本号。
     */
    private String versionNo;
    /**
     * 主模型正常使用的字段，包含系统预制字段。
     */
    private List<ModelPublishedColumnRespDTO> fields = new ArrayList<>();
    /**
     * 正常使用的直接子模型及各自字段。
     */
    private List<ModelPublishedSubmodelRespDTO> subModels = new ArrayList<>();
    /**
     * 返回字段通过dictTypeId引用的参考数据集合。
     */
    private List<ModelDictTypeRespDTO> referenceTypes = new ArrayList<>();
    /**
     * 返回参考数据集合下正常使用的数据项，通过typeId对应集合。
     */
    private List<ModelDictDataRespDTO> referenceItems = new ArrayList<>();
    /**
     * 返回字段实际引用的编码方案主体，通过版本的encodingId对应。
     */
    private List<ModelEncodingRespDTO> encodingSchemes = new ArrayList<>();
    /**
     * 返回字段通过encodingVersionId绑定的编码方案版本。
     */
    private List<ModelEncodingVersionRespDTO> encodingVersions = new ArrayList<>();
    /**
     * 返回编码版本中正常使用的规则段，通过encodingVersionId对应版本。
     */
    private List<ModelEncodingItemRespDTO> encodingItems = new ArrayList<>();
}
