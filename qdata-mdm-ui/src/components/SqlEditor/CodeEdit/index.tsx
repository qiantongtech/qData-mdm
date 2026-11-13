/*
 * Copyright (c) 2026 Jiangsu Qiantong Technology Co., Ltd.
 *  *
 * Software Name: qData Master Data Management Platform (Commercial Edition)
 * Software Copyright Registration No. 16069171
 *  *
 * [RIGHTS AND LICENSE STATEMENT]
 * This file contains non-public commercial source code of which Jiangsu Qiantong
 * Technology Co., Ltd. lawfully possesses complete intellectual property rights.
 *  *
 * Access and use are limited to entities or individuals who have signed a valid
 * commercial license agreement, within the scope stipulated in the agreement.
 * The "accessibility" of this source code is premised on lawful authorization
 * and does not constitute any form of transfer of intellectual property rights
 * or implied licensing.
 *  *
 * [PROHIBITIONS]
 * Unless explicitly agreed in the license agreement, the following acts in any
 * form are strictly prohibited:
 * 1. Copying, disseminating, disclosing, selling, renting, or redistributing
 * this source code;
 * 2. Providing the software's functionality to third parties via SaaS, PaaS,
 * cloud hosting, or other means;
 * 3. Using this software or its derivative versions to develop products that
 * compete with the Right Holder;
 * 4. Providing or displaying this source code or related technical information
 * to unauthorized third parties;
 * 5. Tampering with, circumventing, or destroying copyright notices, license
 * verifications, or other technical protection measures.
 *  *
 * [LEGAL LIABILITY]
 * Any unauthorized use constitutes an infringement of trade secrets and
 * intellectual property rights.
 *  *
 * The Right Holder will strictly pursue liability for breach of contract and
 * infringement in accordance with the commercial agreement and laws such as
 * the "Copyright Law of the People's Republic of China" and the "Anti-Unfair
 * Competition Law".
 *  *
 * ============================================================================
 *  *
 * Copyright (c) 2026 江苏千桐科技有限公司
 *  *
 * 软件名称：qData-mdm 主数据管理平台（专业版） | 软著登字第16069171号
 *  *
 * 【权利与授权声明】
 * 本文件属于江苏千桐科技有限公司依法享有完全知识产权的非公开商业源代码。
 * 仅限已签署有效商业授权合同的单位或个人在约定范围内查阅和使用。
 * 源代码的“可访问性”均以合法授权为前提，不构成任何形式的知识产权转让或默示授权。
 *  *
 * 【禁止事项】
 * 除授权合同明确约定外，严禁任何形式的：
 * 1. 复制、传播、披露、出售、出租或再分发本源代码；
 * 2. 通过 SaaS、PaaS、云托管等方式向第三方提供本软件功能；
 * 3. 将本软件或其衍生版本用于开发与权利人构成竞争的产品；
 * 4. 向未授权第三方提供或展示本源代码或相关技术信息；
 * 5. 篡改、规避或破坏版权标识、授权校验及其他技术保护措施。
 *  *
 * 【法律责任】
 * 任何未经授权的利用行为，均构成对商业秘密及知识产权的侵害。
 * 权利人将依据商业合同及《中华人民共和国著作权法》《反不正当竞争法》
 * 等法律法规，严厉追究违约与侵权责任。
 */

/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import * as monaco from 'monaco-editor';
import { editor, languages, Position } from 'monaco-editor';

import { buildAllSuggestionsToEditor } from '@/components/CustomEditor/CodeEdit/function';
import { handleInitEditorAndLanguageOnBeforeMount } from '@/components/CustomEditor/function';
import { MonacoEditorOptions, SuggestionInfo } from '@/types/Public/data';
import { convertCodeEditTheme } from '@/utils/function';
import { Editor, loader, Monaco, OnChange } from '@monaco-editor/react';
import { connect } from '@umijs/max';
import useMemoCallback from 'rc-menu/es/hooks/useMemoCallback';
import { memo, useCallback, useContext, useRef } from 'react';
import { DataStudioState } from '@/pages/DataStudio/model';
import ITextModel = editor.ITextModel;
import CompletionItem = languages.CompletionItem;
import CompletionContext = languages.CompletionContext;
import CompletionList = languages.CompletionList;
import ProviderResult = languages.ProviderResult;
import LanguageSelector = languages.LanguageSelector;
import { DevopsContext } from '@/pages/DevOps';
import { DataStudioContext, DataStudioContextType } from '@/pages/DataStudio/DataStudioContext';

loader.config({ monaco });

let provider = {
  dispose: () => {}
};

export type CodeEditFormProps = {
  height?: string;
  width?: string;
  language: string;
  options?: any;
  onChange?: OnChange;
  code: string;
  readOnly?: boolean;
  lineNumbers?: string;
  autoWrap?: string;
  editorDidMount?: (editor: editor.IStandaloneCodeEditor, monaco: Monaco) => void;
  enableSuggestions?: boolean;
  enableSuggestionPreview?: boolean;
  monacoRef?: any;
};

const CodeEdit = (props: CodeEditFormProps) => {
  /**
   * 1. height: edit height
   * 2. width: edit width
   * 3. language: edit language
   * 4. options: edit options
   * 5. onChange: edit change callback
   * 6. code: content
   * 7. readOnly: is readOnly, value: true | false
   * 8. lineNumbers: is show lineNumbers, value: on | off | relative | interval
   * 9. theme: edit theme , value: vs-dark | vs | hc-black
   * 10. autoWrap: is auto wrap, value: on | off | wordWrapColumn | bounded
   * 11. showFloatButton: is show float button, value: true | false (default: false)
   */
  const {
    height = '100%', // if null or undefined, set default value
    width = '100%', // if null or undefined, set default value
    language, // edit language
    options,
    onChange, // edit change callback
    code, // content
    readOnly = false, // is readOnly
    lineNumbers, // show lineNumbers
    enableSuggestions = false, // enable suggestions
    enableSuggestionPreview = false, // enable suggestion preview
    // @ts-ignore
    suggestionsData, // suggestions data
    autoWrap = 'on', // auto wrap
    editorDidMount,
    monacoRef
  } = props;

  const { theme } = useContext<DataStudioContextType>(DataStudioContext);
  const editorInstance = useRef<editor.IStandaloneCodeEditor | undefined>(
    monacoRef?.current?.editor
  );
  const monacoInstance = useRef<Monaco | undefined>(monacoRef);

  /**
   * build all suggestions
   */
  const buildAllSuggestions = (model: ITextModel, position: monaco.Position) => {
    return buildAllSuggestionsToEditor(model, position, suggestionsData);
  };

  const buildAllSuggestionsCallback = useCallback(
    async (model: ITextModel, position: monaco.Position) => {
      return buildAllSuggestions(model, position);
    },
    []
  );

  // memo
  const memoizedBuildAllSuggestionsCallback = useMemoCallback(buildAllSuggestionsCallback);

  function reloadCompilation(monacoIns: Monaco, segmentedWords: string[]) {
    provider.dispose();
    provider = monacoIns.languages.registerCompletionItemProvider(language!!, {
      provideCompletionItems: (
        model: editor.ITextModel,
        position: Position,
        context: CompletionContext
      ) => {
        const allSuggestions = memoizedBuildAllSuggestionsCallback(model, position);

        // editor context
        const wordSuggestions: SuggestionInfo[] = segmentedWords.map((word) => ({
          key: word,
          label: {
            label: word,
            detail: '',
            description: ''
          },
          kind: monaco.languages.CompletionItemKind.Text,
          insertText: word
        }));
        let completionList: ProviderResult<CompletionList> = buildAllSuggestionsToEditor(
          model,
          position,
          wordSuggestions
        );
        const suggestions: Promise<languages.CompletionList> = allSuggestions.then((res) => {
          return {
            suggestions: [
              ...(res?.suggestions ?? []),
              ...(completionList as CompletionList)?.suggestions
            ]
          };
        });

        // 获取当前光标行的文本
        const lineText = model.getLineContent(position.lineNumber) ?? '';
        context.triggerKind = monacoIns.languages.CompletionTriggerKind.TriggerCharacter;
        // 设置以当前光标行的文本为触发字符
        context.triggerCharacter = lineText;
        return suggestions;
      },
      resolveCompletionItem: (item: CompletionItem) => {
        return {
          ...item,
          detail: item.detail
        };
      }
    });
  }

  /**
   *  editorDidMount
   * @param {editor.IStandaloneCodeEditor} editor
   * @param monacoIns
   */
  const editorDidMountChange = (editor: editor.IStandaloneCodeEditor, monacoIns: Monaco) => {
    if (editorDidMount) {
      editorDidMount(editor, monacoIns);
    }
    editorInstance.current = editor;
    monacoInstance.current = monacoIns;

    let timeoutId: NodeJS.Timeout | null = null;
    editor.onDidChangeModelContent((e) => {
      if (timeoutId !== null) {
        return;
      }

      timeoutId = setTimeout(() => {
        timeoutId = null;
      }, 3000);

      const model = editor.getModel();
      if (model) {
        const segmenter = new Intl.Segmenter('en', { granularity: 'word' });
        const segments = segmenter.segment(model.getValue());
        const segmentedWords = [];
        for (const segment of segments) {
          const trimmedSegment = segment.segment.trim().replace(/\n/g, '');
          if (trimmedSegment.length > 1) {
            segmentedWords.push(segment.segment);
          }
        }
        const uniqueSegmentedWords = Array.from(new Set(segmentedWords));
        reloadCompilation(monacoIns, uniqueSegmentedWords);
      }
    });

    if (enableSuggestions) {
      reloadCompilation(monacoInstance.current, []);
    }
    editor.layout();
    editor.focus();
  };


  const finalEditorOptions = {
    ...MonacoEditorOptions, // set default options
    tabCompletion: 'on', // tab 补全
    cursorSmoothCaretAnimation: false, // 光标动画
    screenReaderAnnounceInlineSuggestion: true, // 屏幕阅读器提示
    formatOnPaste: true, // 粘贴时格式化
    mouseWheelZoom: true, // 鼠标滚轮缩放
    autoClosingBrackets: 'always', // 自动闭合括号
    autoClosingOvertype: 'always', // 用于在右引号或括号上键入的选项
    autoClosingQuotes: 'always', // 自动闭合引号
    showUnused: true, // 显示未使用的代码
    unfoldOnClickAfterEndOfLine: true, // 控制在折叠线之后单击空内容是否会展开该线
    showFoldingControls: 'always', // 代码折叠控件 'always' | 'mouseover' | 'never'
    automaticLayout: true, // 自动布局
    readOnly, // 是否只读
    glyphMargin: true, // 字形边缘
    formatOnType: true, // 代码格式化
    // columnSelection: true, // 列选择
    wrappingIndent:
      language === 'yaml' || language === 'yml' || language === 'json' ? 'indent' : 'none',
    inlineSuggest: {
      enabled: true,
      showToolbar: 'always',
      keepOnBlur: false,
      allowQuickSuggestions: true,
      showOnAllSymbols: true
    },
    // inlineSuggestionVisible: true,
    quickSuggestions: enableSuggestions,
    guides: {
      bracketPairs: true
    },
    bracketPairColorization: {
      enabled: true,
      independentColorPoolPerBracketType: true
    },
    foldingRanges: true,
    inlineCompletionsAccessibilityVerbose: true,
    smartSelect: {
      selectLeadingAndTrailingWhitespace: true,
      selectSubwords: true
    },
    suggest: {
      quickSuggestions: enableSuggestions,
      showStatusBar: true,
      preview: enableSuggestionPreview,
      previewMode: 'subword',
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
      showSnippets: true
    },
    scrollbar: {
      useShadows: false,
      vertical: 'visible',
      horizontal: 'visible',
      verticalScrollbarSize: 8,
      horizontalScrollbarSize: 8,
      arrowSize: 30
    },
    wordWrap: autoWrap,
    autoDetectHighContrast: true,
    lineNumbers,
    ...options
  };
  return (
    <>
      <div className={'monaco-float'}>
        <Editor
          beforeMount={(monaco) => handleInitEditorAndLanguageOnBeforeMount(monaco, true)}
          width={width}
          height={height}
          value={code}
          language={language}
          options={finalEditorOptions}
          className={'editor-develop'}
          onMount={editorDidMountChange}
          onChange={onChange}
          //zh-CN: 因为在 handleInitEditorAndLanguageOnBeforeMount 中已经注册了自定义语言，所以这里的作用仅仅是用来切换主题 不需要重新加载自定义语言的 token 样式 , 所以这里入参需要为空, 否则每次任意的 props 改变时(包括高度等),会出现编辑器闪烁的问题
          //en-US: because the custom language has been registered in handleInitEditorAndLanguageOnBeforeMount, so the only purpose here is to switch the theme, and there is no need to reload the token style of the custom language, so the incoming parameters here need to be empty, otherwise any props change (including height, etc.) will cause the editor to flash
          theme={theme === 'realDark' ? 'vs-dark' : theme}
        />
      </div>
    </>
  );
};

export default connect(({ DataStudio }: { DataStudio: DataStudioState }) => ({
  suggestionsData: DataStudio.tempData.suggestions
}))(memo(CodeEdit));
