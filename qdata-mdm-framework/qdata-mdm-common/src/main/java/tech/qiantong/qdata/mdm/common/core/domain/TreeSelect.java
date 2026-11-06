/*
 * Copyright © 2025-present Jiangsu Qiantong Technology Co., Ltd.
 *
 * This document is part of the qData Master Data Platform (open source version).
 *
 * qData master data platform is licensed under Apache License 2.0 with additional qData master data platform terms.
 * You may use qData for commercial purposes, but you may not remove, hide,
 * modify, or replace the qData master data platform logo, copyright notices, license notices,
 * or attribution information without a separate commercial license.
 *
 * White-label use, OEM distribution, rebranding, or presenting qData master data platform as
 * another product requires separate commercial authorization from
 * Jiangsu Qiantong Technology Co., Ltd.
 *
 * Business License: https://community.qdata.tech/business/policy.html
 * See the LICENSE file in the project root for full license information.
 */

package tech.qiantong.qdata.mdm.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import tech.qiantong.qdata.mdm.common.core.domain.entity.RpAreaDict;
import tech.qiantong.qdata.mdm.common.core.domain.entity.RpDept;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysDept;
import tech.qiantong.qdata.mdm.common.core.domain.entity.SysMenu;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Treeselect tree structure entity class
 *
 * @author qdata
 */
public class TreeSelect implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Node ID */
    private Long id;

    /** Node name */
    private String label;

    /** Child node */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelect> children;

    public TreeSelect()
    {

    }

    public TreeSelect(SysDept dept)
    {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        this.children = dept.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    public TreeSelect(SysMenu menu)
    {
        this.id = menu.getMenuId();
        this.label = menu.getMenuName();
        this.children = menu.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    public TreeSelect(RpDept rpDeptDO) {
        this.id = rpDeptDO.getDeptId();
        this.label = rpDeptDO.getDeptName();
        this.children = rpDeptDO.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    public TreeSelect(RpAreaDict rpDeptDO) {
        this.id = rpDeptDO.getId();
        this.label = rpDeptDO.getName();
        this.children = rpDeptDO.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public List<TreeSelect> getChildren()
    {
        return children;
    }

    public void setChildren(List<TreeSelect> children)
    {
        this.children = children;
    }
}
