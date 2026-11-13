<!--
  Copyright © 2026-present Jiangsu Qiantong Technology Co., Ltd.

  This file is part of qData-mdm Master Data Platform (Open Source Edition).

  qData-mdm is licensed under Apache License 2.0 with additional qData-mdm terms.
  You may use qData-mdm for commercial purposes, but you may not remove, hide,
  modify, or replace the qData-mdm logo, copyright notices, license notices,
  or attribution information without a separate commercial license.

  White-label use, OEM distribution, rebranding, or presenting qData-mdm as
  another product requires separate commercial authorization from
  Jiangsu Qiantong Technology Co., Ltd.

  Business License: https://community.qdata.tech/business/policy.html
  See the LICENSE file in the project root for full license information.
-->

<template>
  <div class="sql-editor-wrapper" :class="{ 'sql-editor-wrapper--auto': autoHeight }">
    <textarea ref="mycode" class="codesql" :value="value" />
  </div>
</template>

<script>
import "codemirror/lib/codemirror.css";
import "codemirror/theme/blackboard.css";
import "codemirror/addon/hint/show-hint.css";
import CodeMirror from "codemirror/lib/codemirror";
import { markRaw } from "vue";

// 导入所需的附加功能和模式
import "codemirror/addon/edit/matchbrackets";
import "codemirror/addon/selection/active-line";
import "codemirror/mode/sql/sql";
import "codemirror/mode/shell/shell";
import "codemirror/mode/javascript/javascript";
import "codemirror/addon/hint/show-hint";
import "codemirror/addon/hint/sql-hint";

export default {
  name: "SqlEditor",
  props: {
    value: {
      type: String,
      default: "",
    },
    sqlStyle: {
      type: String,
      default: "default",
    },
    theme: {
      type: String,
      default: "blackboard",
    },
    autoHeight: Boolean,
    readOnly: {
      type: [Boolean, String],
      default: false,
    },
    mode: {
      type: String,
      default: "text/x-mariadb",
    },
    autofocus: {
      type: Boolean,
      default: true,
    },
  },
  watch: {
    value(newVal) {
      if (this.editor && newVal !== this.editor.getValue()) {
        this.editor.setValue(newVal || "");
      }
    },
  },
  //设置失焦后自动执行 this.$emit('changeTextarea', this.editor.getValue())

  mounted() {
    this.$refs.mycode.value = this.value;
    this.editor = markRaw(CodeMirror.fromTextArea(this.$refs.mycode, {
      mode: this.mode,
      indentWithTabs: true,
      smartIndent: true,
      lineNumbers: true,
      matchBrackets: true,
      cursorHeight: 1,
      lineWrapping: true,
      readOnly: this.readOnly,
      theme: this.theme,
      ...(this.autoHeight ? { viewportMargin: Infinity } : {}),
      autofocus: this.autofocus,
      extraKeys: { Ctrl: "autocomplete" }, // 自定义快捷键
      hintOptions: {
        // 自定义提示选项
        completeSingle: false,
      },
    }));
    // 页签显示、容器尺寸变化后刷新，避免首次进入示例为空白。
    this.resizeObserver = new ResizeObserver(() => this.editor?.refresh());
    this.resizeObserver.observe(this.$el);
    this.$nextTick(() => this.editor?.refresh());
    // 监听 blur 事件
    this.editor.on("blur", () => {
      this.$emit("changeTextarea", this.editor.getValue());
    });
  },
  beforeUnmount() {
    this.resizeObserver?.disconnect();
    this.editor?.toTextArea();
    this.editor = null;
  },
};
</script>

<style lang="scss" scoped>
.sql-editor-wrapper {
  width: 100%;
  height: 100%;

  :deep(.CodeMirror) {
    height: 100%;
    border: 1px solid #dcdfe6;
    font-size: 13px;
  }

  &--auto,
  &--auto :deep(.CodeMirror) {
    height: auto;
  }
}

// 这句为了解决匹配框显示有问题而加
.CodeMirror-hints {
  z-index: 9999 !important;
}
</style>
