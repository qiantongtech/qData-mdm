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
  <div
    ref="monacoDom"
    class="json-editors"
    :class="{ 'is-minimap-enabled': minimapEnabled }"
    @editor-query="handleQuery"
    @beforeinput.capture="handleReadOnlyInputAttempt"
    @compositionstart.capture="handleReadOnlyInputAttempt"
    @keydown.capture="handleReadOnlyKeyAttempt"
    @contextmenu.prevent="openCustomContextMenu"
  ></div>
  <Teleport to="body">
    <div
      v-if="customContextMenu.visible"
      ref="customContextMenuRef"
      class="sql-editor-context-menu"
      :style="{
        left: `${customContextMenu.x}px`,
        top: `${customContextMenu.y}px`,
      }"
      @contextmenu.prevent.stop
      @mousedown.prevent.stop
    >
      <template v-for="item in contextMenuItems" :key="item.id || item.key">
        <div v-if="item.divider" class="sql-editor-context-menu-divider"></div>
        <button
          v-else
          class="sql-editor-context-menu-item"
          :disabled="item.disabled"
          type="button"
          @click.stop="runCustomContextMenuAction(item)"
        >
          <span>{{ item.label }}</span>
          <span v-if="item.shortcut" class="sql-editor-context-menu-shortcut">
            {{ item.shortcut }}
          </span>
        </button>
      </template>
    </div>
  </Teleport>
</template>
<script setup>
import beautify from "js-beautify";
import * as monaco from "monaco-editor";
// 注册快捷键-右键菜单
import { registerEditorKeyBindingAndAction } from "@/utils/functionCodemirror";
// flinksql语法
import { FlinkSQLLanguage } from "../languages/flinksql/index";
import { LogLanguage } from "../languages/javalog/index";

const props = defineProps({
  // 绑定值
  modelValue: {
    type: String,
    default: "",
  },
  // 只读
  readOnly: {
    type: Boolean,
    default: false,
  },
  // 配置
  config: {
    type: Object,
    default() {
      return {};
    },
  },
  // 语言
  language: {
    type: String,
    default: "flinksql",
  },
  // 启用建议
  enableSuggestions: {
    type: Boolean,
    default: true,
  },
  // 启用建议预览
  enableSuggestionPreview: {
    type: Boolean,
    default: true,
  },
  autoWrap: {
    type: String,
    default: "on",
  },
  lineNumbers: {
    type: String,
    default: "on",
  },
  enableQuery: {
    type: Boolean,
    default: true,
  },
});
const emits = defineEmits([
  "update:modelValue",
  "change",
  "ready",
  "query",
  "cursor-change",
  "model-info-change",
  "readonly-attempt",
]);
const monacoDom = ref(null);
const minimapEnabled = ref(false);
const customContextMenuRef = ref(null);
const customContextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
});
let monacoInstance = null;
let syncingModelValue = false;
let minimapNudgeFrame = 0;
const minimapNudgeDisposables = [];
const contextMenuItems = computed(() => {
  const writable = !props.readOnly;
  return [
    {
      id: "editor.action.changeAll",
      label: "更改所有匹配项",
      shortcut: "Ctrl+F2",
      disabled: !writable,
    },
    {
      key: "clipboard-divider",
      divider: true,
    },
    {
      id: "editor.action.clipboardCutAction",
      label: "剪切",
      disabled: !writable,
    },
    {
      id: "editor.action.clipboardCopyAction",
      label: "复制",
      disabled: false,
    },
    {
      id: "editor.action.clipboardPasteAction",
      label: "粘贴",
      disabled: !writable,
    },
    {
      key: "editor-divider",
      divider: true,
    },
    {
      id: "format",
      label: "格式化",
      shortcut: "Ctrl+Alt+L",
      disabled: !writable,
    },
    {
      id: "formatSelection",
      label: "格式化选中内容",
      shortcut: "Ctrl+4",
      disabled: !writable,
    },
    {
      id: "commentLine",
      label: "注释/取消注释该行(区域)",
      shortcut: "Ctrl+/",
      disabled: !writable,
    },
    {
      id: "upperCase",
      label: "转为大写",
      shortcut: "Ctrl+U",
      disabled: !writable,
    },
    {
      id: "lowerCase",
      label: "转为小写",
      shortcut: "Ctrl+L",
      disabled: !writable,
    },
    ...(props.enableQuery
      ? [
          {
            id: "query",
            label: "查询",
            disabled: !writable,
          },
        ]
      : []),
    {
      key: "command-divider",
      divider: true,
    },
    {
      id: "editor.action.quickCommand",
      label: "命令面板",
      shortcut: "F1",
      disabled: false,
    },
  ];
});

function closeCustomContextMenu() {
  customContextMenu.value.visible = false;
}

function openCustomContextMenu(event) {
  customContextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
  };
  window.requestAnimationFrame(() => {
    const menu = customContextMenuRef.value;
    if (!menu || !customContextMenu.value.visible) return;
    customContextMenu.value = {
      ...customContextMenu.value,
      x: Math.max(4, Math.min(customContextMenu.value.x, window.innerWidth - menu.offsetWidth - 4)),
      y: Math.max(4, Math.min(customContextMenu.value.y, window.innerHeight - menu.offsetHeight - 4)),
    };
  });
}

async function runCustomContextMenuAction(item) {
  if (item.disabled) return;
  closeCustomContextMenu();
  const action = monacoInstance?.getAction?.(item.id);
  if (action?.run) {
    await action.run();
    return;
  }
  monacoInstance?.trigger("context-menu", item.id, null);
}

function handleContextMenuKeydown(event) {
  if (event.key === "Escape") closeCustomContextMenu();
}

function updateShortContentMinimapNudge() {
  if (!monacoInstance || !minimapEnabled.value || !monacoDom.value) return;
  const minimap = monacoDom.value.querySelector(".monaco-editor .minimap");
  const slider = minimap?.querySelector(".minimap-slider");
  if (!minimap || !slider) return;
  monacoDom.value.style.setProperty(
    "--editor-minimap-width",
    `${minimap.clientWidth}px`
  );

  const layoutHeight = monacoInstance.getLayoutInfo()?.height || 0;
  const lineHeight = monacoInstance.getOption(
    monaco.editor.EditorOption.lineHeight
  );
  const visibleLineCount = lineHeight ? layoutHeight / lineHeight : 0;
  const lineCount = monacoInstance.getModel()?.getLineCount?.() || 0;
  const isShortContent =
    visibleLineCount > 0 && lineCount <= visibleLineCount * 2.2;
  const maxScrollTop = Math.max(
    0,
    monacoInstance.getScrollHeight() - layoutHeight
  );
  const scrollRatio = maxScrollTop
    ? Math.min(1, Math.max(0, monacoInstance.getScrollTop() / maxScrollTop))
    : 0;
  const extraOffset = isShortContent ? Math.round(scrollRatio * 20) : 0;
  slider.style.transform = extraOffset
    ? `translateY(${extraOffset}px)`
    : "";
}

function scheduleShortContentMinimapNudge() {
  if (minimapNudgeFrame) cancelAnimationFrame(minimapNudgeFrame);
  minimapNudgeFrame = requestAnimationFrame(() => {
    minimapNudgeFrame = 0;
    updateShortContentMinimapNudge();
  });
}
function getEditorSelectedOrAll() {
  if (!monacoInstance) return "";
  const selection = monacoInstance.getSelection();
  if (selection && !selection.isEmpty()) {
    return monacoInstance.getModel().getValueInRange(selection);
  }
  return monacoInstance.getValue();
}
function handleQuery(sql) {
  console.log("触发查询，SQL内容:");
  emits("query", sql.detail.value);
}

function handleReadOnlyInputAttempt() {
  if (props.readOnly) emits("readonly-attempt");
}

function handleReadOnlyKeyAttempt(event) {
  if (!props.readOnly || event.ctrlKey || event.metaKey || event.altKey) return;
  const editingKey =
    event.isComposing ||
    event.key.length === 1 ||
    ["Backspace", "Delete", "Enter", "Tab"].includes(event.key);
  if (editingKey) emits("readonly-attempt");
}
function getEditorModelInfo() {
  const model = monacoInstance?.getModel?.();
  const modelOptions = model?.getOptions?.() || {};
  const position = monacoInstance?.getPosition?.() || {
    lineNumber: 1,
    column: 1,
  };
  return {
    line: position.lineNumber || 1,
    column: position.column || 1,
    tabSize: modelOptions.tabSize || 4,
    insertSpaces: modelOptions.insertSpaces !== false,
    eol: model?.getEOL?.() === "\r\n" ? "CRLF" : "LF",
  };
}
function emitEditorModelInfo() {
  if (!monacoInstance) return;
  const info = getEditorModelInfo();
  emits("cursor-change", {
    line: info.line,
    column: info.column,
  });
  emits("model-info-change", info);
}
watch(
  () => props.modelValue,
  (newValue) => {
    const value = monacoInstance?.getValue();
    if (newValue !== value) {
      syncingModelValue = true;
      try {
        monacoInstance?.setValue(newValue ?? "");
      } finally {
        syncingModelValue = false;
      }
    }
  }
);
watch(
  () => props.readOnly,
  (readOnly) => {
    monacoInstance?.updateOptions({
      readOnly,
      domReadOnly: readOnly,
    });
  }
);
const defaultConfig = {
  renderSideBySide: false, //  side by side
  // autoIndent: "none", //  auto indent
  // fontSize: 14, //  font size
  // automaticLayout: true, //  auto layout
  // scrollBeyondLastLine: false, //is scroll beyond the last line
  // autoDetectHighContrast: true, // auto detect high contrast
  // ---------------------
  formatOnPaste: true, // 粘贴时格式化
  mouseWheelZoom: true, // 鼠标滚轮缩放
  screenReaderAnnounceInlineSuggestion: true, // 屏幕阅读器提示
  automaticLayout: true, // 自动布局，编辑器自适应大小
  theme: "vs", // 官方自带三种主题vs, hc-black, or vs-dark
  minimap: {
    enabled: false, // 是否开启右侧代码小窗
  },
  codeLens: true, // 代码镜头
  colorDecorators: true, // 颜色装饰器
  parameterHints: {
    enabled: true,
  },
  selectOnLineNumbers: true, //显示行号
  quickSuggestionsDelay: 100, //代码提示延时
  autoIndent: true, //自动布局
  wrappingStrategy: "advanced",
  scrollBeyondLastLine: false,
  autoDetectHighContrast: true, // auto detect high contrast
  overviewRulerLanes: 0,
  scrollbar: {
    alwaysConsumeMouseWheel: false,
    useShadows: false,
    vertical: "visible",
    horizontal: "visible",
    verticalScrollbarSize: 8,
    horizontalScrollbarSize: 8,
    verticalSliderSize: 6,
    horizontalSliderSize: 6,
    arrowSize: 30,
  },
  hover: {
    enabled: true,
    above: false,
  },
  renderLineHighlight: "none",
  fontSize: 14,
};
onMounted(() => {
  monaco.languages.json.jsonDefaults.setDiagnosticsOptions({
    allowComments: true,
    validate: true,
    trailingCommas: "ignore",
    schemaValidation: "warning",
  });
  monaco.languages.json.jsonDefaults.setModeConfiguration({
    completionItems: false,
    tokens: true,
    colors: true,
    foldingRanges: true,
    diagnostics: true,
  });
  monacoInstance = monaco.editor.create(monacoDom.value, {
    ...defaultConfig,
    tabCompletion: "on", // tab 补全
    cursorSmoothCaretAnimation: false, // 光标动画
    screenReaderAnnounceInlineSuggestion: true, // 屏幕阅读器提示
    formatOnPaste: true, // 粘贴时格式化
    mouseWheelZoom: true, // 鼠标滚轮缩放
    autoClosingBrackets: "always", // 自动闭合括号
    autoClosingOvertype: "always", // 用于在右引号或括号上键入的选项
    autoClosingQuotes: "always", // 自动闭合引号
    showUnused: true, // 显示未使用的代码
    unfoldOnClickAfterEndOfLine: true, // 控制在折叠线之后单击空内容是否会展开该线
    showFoldingControls: "always", // 代码折叠控件 'always' | 'mouseover' | 'never'
    automaticLayout: true, // 自动布局
    glyphMargin: true, // 字形边缘
    formatOnType: true, // 代码格式化
    // columnSelection: true, // 列选择
    wrappingIndent:
      props.language === "yaml" ||
      props.language === "yml" ||
      props.language === "json"
        ? "indent"
        : "none",
    inlineSuggest: {
      enabled: true,
      showToolbar: "always",
      keepOnBlur: false,
      allowQuickSuggestions: true,
      showOnAllSymbols: true,
    },
    // inlineSuggestionVisible: true,
    quickSuggestions: props.enableSuggestions,
    guides: {
      bracketPairs: true,
    },
    bracketPairColorization: {
      enabled: true,
      independentColorPoolPerBracketType: true,
    },
    foldingRanges: true,
    inlineCompletionsAccessibilityVerbose: true,
    smartSelect: {
      selectLeadingAndTrailingWhitespace: true,
      selectSubwords: true,
    },
    suggest: {
      quickSuggestions: props.enableSuggestions,
      showStatusBar: true,
      preview: props.enableSuggestionPreview,
      previewMode: "subword",
      showInlineDetails: true,
      showMethods: true,
      showFunctions: true,
      showConstructors: true,
      showFields: true,
      showEvents: true,
      showOperators: true,
      showClasses: true,
      showModules: true,
      showStructs: true,
      showInterfaces: true,
      showProperties: true,
      showUnits: true,
      showValues: true,
      showConstants: true,
      showEnums: true,
      showEnumMembers: true,
      showKeywords: true,
      showWords: true,
      showFolders: true,
      showReferences: true,
      showSnippets: true,
    },
    scrollbar: {
      useShadows: false,
      vertical: "visible",
      horizontal: "visible",
      verticalScrollbarSize: 8,
      horizontalScrollbarSize: 8,
      verticalSliderSize: 6,
      horizontalSliderSize: 6,
      arrowSize: 30,
    },
    wordWrap: props.autoWrap,
    autoDetectHighContrast: true,
    lineNumbers: props.lineNumbers,
    value:
      props.modelValue ||
      `
-- 在此编写任务脚本
`.trim(),
    language: props.language,
    ...props.config,
    // Keep read-only options last so caller config cannot reopen the input DOM.
    readOnly: props.readOnly,
    domReadOnly: props.readOnly,
    contextmenu: false,
  });
  window.addEventListener("mousedown", closeCustomContextMenu);
  window.addEventListener("keydown", handleContextMenuKeydown);
  // 注册快捷键及右键菜单
  registerEditorKeyBindingAndAction(monacoInstance, props.enableQuery);
  // 注册自定义语言 monacoLanguages, monacoEditor, registerCompletion: 代码提示
  FlinkSQLLanguage(monaco.languages, monaco.editor, true);
  LogLanguage(monaco.languages);
  // 注册主题色
  convertCodeEditTheme(monaco.editor);
  monaco.editor.setTheme("light");

  monacoInstance.onDidChangeModelContent(() => {
    if (syncingModelValue) {
      emitEditorModelInfo();
      scheduleShortContentMinimapNudge();
      return;
    }
    // 输入法组合提交等少数路径可能绕过 Monaco 的 readOnly 拦截。
    // 只读时以父级已确认的值回滚，避免将内容同步回任务脚本。
    if (props.readOnly) {
      syncingModelValue = true;
      try {
        monacoInstance?.setValue(props.modelValue ?? "");
      } finally {
        syncingModelValue = false;
      }
      emitEditorModelInfo();
      scheduleShortContentMinimapNudge();
      return;
    }
    emits("update:modelValue", monacoInstance?.getValue());
    emits("change", monacoInstance?.getValue());
    emitEditorModelInfo();
    scheduleShortContentMinimapNudge();
  });
  monacoInstance.onDidChangeCursorPosition((event) => {
    emits("cursor-change", {
      line: event.position.lineNumber,
      column: event.position.column,
    });
  });
  monacoInstance.onDidChangeModelOptions?.(() => {
    emitEditorModelInfo();
  });
  minimapNudgeDisposables.push(
    monacoInstance.onDidScrollChange(scheduleShortContentMinimapNudge),
    monacoInstance.onDidLayoutChange(scheduleShortContentMinimapNudge),
    monacoInstance.onDidContentSizeChange(scheduleShortContentMinimapNudge)
  );
  emitEditorModelInfo();
  emits("ready", monacoInstance, monaco);
  scheduleShortContentMinimapNudge();
  //   代码提示
  //   editorDidMountChange(monacoInstance, monaco);
});
onActivated(() => {
  monacoInstance?.focus();
});
onBeforeUnmount(() => {
  if (minimapNudgeFrame) cancelAnimationFrame(minimapNudgeFrame);
  window.removeEventListener("mousedown", closeCustomContextMenu);
  window.removeEventListener("keydown", handleContextMenuKeydown);
  minimapNudgeDisposables
    .splice(0)
    .forEach((disposable) => disposable?.dispose?.());
  monacoInstance?.dispose();
});
const format = () => {
  const formatStr = beautify(props.modelValue, { indent_size: 4 });
  monacoInstance?.setValue(formatStr);
};
const focus = () => {
  monacoInstance?.focus();
};
const setMinimapEnabled = (enabled) => {
  minimapEnabled.value = enabled;
  monacoInstance?.updateOptions({
    minimap: {
      enabled,
      side: "right",
      size: "proportional",
      showSlider: "mouseover",
      renderCharacters: true,
      maxColumn: 120,
      scale: 1,
    },
  });
  monacoInstance?.layout();
  scheduleShortContentMinimapNudge();
};

const CODE_EDIT_THEME = {
  LIGHT: "light",
  DARK: "vs-dark",
};
function convertCodeEditTheme(editorInstance) {
  if (!editorInstance) {
    // eslint-disable-next-line no-param-reassign
    editorInstance = editor;
  }
  if (editorInstance === undefined) {
    return CODE_EDIT_THEME.LIGHT;
  } else {
    /**
     * 定义亮色 覆盖vs主题,增加扩展规则
     */
    editorInstance?.defineTheme?.(CODE_EDIT_THEME.LIGHT, {
      base: "vs", // 指定基础主题 , 可选值: 'vs', 'vs-dark', 'hc-black' , base theme
      inherit: true, // 是否继承主题配置
      rules: [
        // 注意,默认的不做修改 因为上边继承了父主题, 只添加自己定义的 , 否则会覆盖默认的 , 导致编辑器样式不一致
        { token: "custom-info", foreground: "#808080" },
        { token: "custom-thread", foreground: "#9fa19f" },
        { token: "custom-class", foreground: "#1060d9" },
        { token: "custom-error", foreground: "#ff0000", fontStyle: "bold" },
        { token: "custom-warning", foreground: "#FFA500", fontStyle: "bold" },
        { token: "custom-date", foreground: "#008800" },
        { token: "custom-process", foreground: "#07f313" },
      ],
      colors: {
        "editor.background": "#FFFFFF",
        "minimap.background": "#FFFFFF",
        "minimap.selectionHighlight": "#FFFFFF",
        "minimap.errorHighlight": "#FFFFFF",
        "minimap.warningHighlight": "#FFFFFF",
        "minimapSlider.background": "#8C8C8C26",
        "minimapSlider.hoverBackground": "#8C8C8C3D",
        "minimapSlider.activeBackground": "#8C8C8C59",
      },
      encodedTokensColors: [],
    });

    /**
     * 定义暗色 覆盖vs-dark主题,增加扩展规则
     */
    editorInstance?.defineTheme?.(CODE_EDIT_THEME.DARK, {
      base: "vs-dark", // 指定基础主题 , 可选值: 'vs', 'vs-dark', 'hc-black' , base theme
      inherit: true, // 是否继承主题配置
      rules: [
        // 注意,默认的不做修改 因为上边继承了父主题, 只添加自己定义的 , 否则会覆盖默认的 , 导致编辑器样式不一致
        { token: "custom-info", foreground: "#008800" },
        { token: "custom-thread", foreground: "#9fa19f" },
        { token: "custom-class", foreground: "#1060d9" },
        { token: "custom-error", foreground: "#ff0000", fontStyle: "bold" },
        { token: "custom-warning", foreground: "#FFA500", fontStyle: "bold" },
        { token: "custom-date", foreground: "#008800" },
        { token: "custom-process", foreground: "#07f313" },
      ],
      colors: {},
      encodedTokensColors: [],
    });
  }

  // const theme = getLocalTheme();
  // switch (theme) {
  //   case THEME.dark:
  //     return CODE_EDIT_THEME.DARK;
  //   case THEME.light:
  //     return CODE_EDIT_THEME.LIGHT;
  //   default:
  //     return CODE_EDIT_THEME.LIGHT;
  // }
  return CODE_EDIT_THEME.LIGHT;
}
function reloadCompilation(monacoIns, segmentedWords) {
  provider.dispose();
  provider = monacoIns.languages.registerCompletionItemProvider(
    props.language,
    {
      provideCompletionItems: (model, position, context) => {
        const allSuggestions = memoizedBuildAllSuggestionsCallback(
          model,
          position
        );

        // editor context
        const wordSuggestions = segmentedWords.map((word) => ({
          key: word,
          label: {
            label: word,
            detail: "",
            description: "",
          },
          kind: monaco.languages.CompletionItemKind.Text,
          insertText: word,
        }));
        let completionList = buildAllSuggestionsToEditor(
          model,
          position,
          wordSuggestions
        );
        const suggestions = allSuggestions.then((res) => {
          return {
            // eslint-disable-next-line no-unsafe-optional-chaining
            suggestions: [
              ...(res?.suggestions ?? []),
              ...completionList?.suggestions,
            ],
          };
        });

        // 获取当前光标行的文本
        const lineText = model.getLineContent(position.lineNumber) ?? "";
        context.triggerKind =
          monacoIns.languages.CompletionTriggerKind.TriggerCharacter;
        // 设置以当前光标行的文本为触发字符
        context.triggerCharacter = lineText;
        return suggestions;
      },
      resolveCompletionItem: (item) => {
        return {
          ...item,
          detail: item.detail,
        };
      },
    }
  );
}

/**
 *  editorDidMount
 * @param {editor.IStandaloneCodeEditor} editor
 * @param monacoIns
 */
const editorInstance = ref(null);
let provider = {
  dispose: () => {},
};
const editorDidMountChange = (editor, monacoIns) => {
  editorInstance.current = editor;
  monacoInstance.current = monacoIns;

  let timeoutId = null;
  editor.onDidChangeModelContent(() => {
    if (timeoutId !== null) {
      return;
    }

    timeoutId = setTimeout(() => {
      timeoutId = null;
    }, 3000);

    const model = editor.getModel();
    if (model) {
      const segmenter = new Intl.Segmenter("en", { granularity: "word" });
      const segments = segmenter.segment(model.getValue());
      const segmentedWords = [];
      for (const segment of segments) {
        const trimmedSegment = segment.segment.trim().replace(/\n/g, "");
        if (trimmedSegment.length > 1) {
          segmentedWords.push(segment.segment);
        }
      }
      const uniqueSegmentedWords = Array.from(new Set(segmentedWords));
      reloadCompilation(monacoIns, uniqueSegmentedWords);
    }
  });

  if (props.enableSuggestions) {
    reloadCompilation(monacoInstance.current, []);
  }
  editor.layout();
  editor.focus();
};
defineExpose({
  format,
  focus,
  getEditorSelectedOrAll,
  setMinimapEnabled,
});
</script>
<style lang="scss" scoped>
.json-editors {
  width: 100%;
  height: 100%;
  --editor-minimap-width: 129px;

  :deep(.monaco-editor-background) {
    background: #ffffff !important;
  }

  :deep(.monaco-editor .margin) {
    background: #ffffff !important;
  }

  :deep(.monaco-scrollable-element > .scrollbar > .slider) {
    opacity: 0 !important;
    background: transparent !important;
    transition: opacity 0.15s ease, background-color 0.15s ease;
  }

  :deep(.monaco-scrollable-element > .scrollbar:hover > .slider),
  :deep(.monaco-scrollable-element > .scrollbar > .slider:hover),
  :deep(.monaco-scrollable-element > .scrollbar > .slider.active) {
    opacity: 1 !important;
    background: rgba(139, 149, 165, 0.72) !important;
  }

  :deep(.monaco-editor .minimap) {
    border-left: 1px solid #e6eaf2;
    box-sizing: border-box;
  }

  :deep(.monaco-editor .minimap .minimap-slider) {
    left: 1.54% !important;
    width: 97.69% !important;
    height: 8.65% !important;
    opacity: 1;
    border: 0;
    background: rgba(0, 0, 0, 0.043137) !important;
    box-shadow: none;
    backdrop-filter: blur(3.6243733333px);
    -webkit-backdrop-filter: blur(3.6243733333px);
    box-sizing: border-box;
    transition: opacity 0.16s ease;
  }

  :deep(.monaco-editor .minimap .minimap-slider-horizontal) {
    width: 100% !important;
    height: 100% !important;
    background: transparent !important;
  }

  :deep(.monaco-editor .minimap:hover .minimap-slider),
  :deep(.monaco-editor .minimap .minimap-slider:hover),
  :deep(.monaco-editor .minimap .minimap-slider.active) {
    opacity: 1;
    background: rgba(0, 0, 0, 0.043137) !important;
  }

  &.is-minimap-enabled {
    :deep(.monaco-editor .minimap) {
      left: auto !important;
      right: 0 !important;
    }

    :deep(.monaco-editor .scrollbar.vertical) {
      right: var(--editor-minimap-width) !important;
    }

    :deep(.monaco-editor .decorationsOverviewRuler) {
      right: var(--editor-minimap-width) !important;
    }
  }
}

.sql-editor-context-menu {
  position: fixed;
  z-index: 10000;
  width: 292px;
  padding: 4px 0;
  box-sizing: border-box;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.14);
  color: #606266;
  font-size: 13px;
  user-select: none;
}

.sql-editor-context-menu-item {
  width: 100%;
  height: 26px;
  padding: 0 12px 0 30px;
  border: 0;
  background: transparent;
  color: inherit;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font: inherit;
  line-height: 26px;
  text-align: left;
  cursor: pointer;

  &:hover:not(:disabled) {
    background: #f5f7fa;
    color: #303133;
  }

  &:disabled {
    color: #c0c4cc;
    cursor: not-allowed;
  }
}

.sql-editor-context-menu-shortcut {
  flex: 0 0 auto;
  color: #909399;
}

.sql-editor-context-menu-divider {
  height: 1px;
  margin: 4px 0;
  background: #ebeef5;
}
</style>
