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

import { CustomEditorLanguage, TokenClassConsts } from "../constants";
import { EXTEND_SQL_KEYWORD, FLINK_SQL_BUILTIN_FUNCTIONS, FLINK_SQL_KEYWORD, FLINK_SQL_OPERATORS, FLINK_SQL_SCOPE_KEYWORDS, FLINK_SQL_TYPE_KEYWORDS } from "./keyword";

export function buildFlinkSQLConfiguration(monacoLanguages) {
  monacoLanguages?.setLanguageConfiguration(CustomEditorLanguage.FlinkSQL, {
    comments: {
      lineComment: "-- ",
      blockComment: ["/*", "*/"],
    },
    brackets: [
      ["{", "}"],
      ["[", "]"],
      ["(", ")"],
      ['"', '"'],
      ["'", "'"],
      ["`", "`"],
      ["<", ">"],
    ],
    autoClosingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: "`", close: "`" },
      { open: "<", close: ">" },
    ],
    indentationRules: {
      increaseIndentPattern: /^\s*(\w+\s+)+\w/,
      decreaseIndentPattern: /^\s*(\w+\s+)+\w/,
    },
    onEnterRules: [
      {
        beforeText: /^\s*(\/\*)/,
        afterText: /^\s*\*\/$/,
        action: { indentAction: monacoLanguages.IndentAction.IndentOutdent, appendText: " * " },
      },
    ],
    surroundingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: "`", close: "`" },
      { open: "<", close: ">" },
    ],
    colorizedBracketPairs: [
      ["{", "}"],
      ["[", "]"],
      ["(", ")"],
      ["<", ">"],
      ['"', '"'],
      ["'", "'"],
      ["`", "`"],
    ],
  });
}

export function buildMonarchTokensProvider(monacoLanguages) {
  monacoLanguages?.setMonarchTokensProvider(CustomEditorLanguage.FlinkSQL, {
    defaultToken: "",
    tokenPostfix: ".sql",
    keywords: [...FLINK_SQL_KEYWORD, ...EXTEND_SQL_KEYWORD],
    operators: FLINK_SQL_OPERATORS,
    ignoreCase: true, // 忽略大小写
    builtinFunctions: FLINK_SQL_BUILTIN_FUNCTIONS,
    builtinVariables: [],
    typeKeywords: FLINK_SQL_TYPE_KEYWORDS,
    scopeKeywords: FLINK_SQL_SCOPE_KEYWORDS,
    pseudoColumns: [],
    comments: {
      lineComment: "--",
      blockComment: ["/*", "*/"],
    },
    brackets: [
      { open: "{", close: "}", token: "delimiter.curly" },
      { open: "[", close: "]", token: "delimiter.bracket" },
      { open: "(", close: ")", token: "delimiter.parenthesis" },
      { open: "<", close: ">", token: "delimiter.angle" },
    ],
    autoClosingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: "`", close: "`" },
      { open: "<", close: ">" },
    ],
    surroundingPairs: [
      { open: "{", close: "}" },
      { open: "[", close: "]" },
      { open: "(", close: ")" },
      { open: '"', close: '"' },
      { open: "'", close: "'" },
      { open: "`", close: "`" },
      { open: "<", close: ">" },
    ],
    folding: {
      markers: {
        start: /((CREATE|ALERT|DROP|USE\s+)?(TABLE|DATABASE|STREAM|FUNCTION|CATALOG|SCHEMA|VIEW)\b)|((EXECUTE\s+)?(JAR|CDCSOURCE)?\b)/i,
        end: /\)\\;\b/i,
      },
    },
    tokenizer: {
      root: [
        { include: "@comments" },
        { include: "@whitespace" },
        { include: "@pseudoColumns" },
        { include: "@numbers" },
        { include: "@strings" },
        { include: "@complexIdentifiers" },
        { include: "@scopes" },
        { include: "@complexDataTypes" },
        [
          /[\w@#$]+/,
          {
            cases: {
              "@scopeKeywords": TokenClassConsts.KEYWORD_SCOPE,
              "@operators": TokenClassConsts.OPERATOR_KEYWORD,
              "@typeKeywords": TokenClassConsts.TYPE,
              "@builtinVariables": TokenClassConsts.VARIABLE,
              "@builtinFunctions": TokenClassConsts.PREDEFINED,
              "@keywords": TokenClassConsts.KEYWORD,
              "@default": TokenClassConsts.IDENTIFIER,
              fontWeight: "normal",
            },
          },
        ],
      ],
      whitespace: [[/[\s\t\r\n]+/, TokenClassConsts.WHITE]],
      comments: [
        [/--+.*/, TokenClassConsts.COMMENT],
        [/\/\*/, { token: TokenClassConsts.COMMENT_QUOTE, next: "@comment" }],
      ],
      comment: [
        [/[^*/]+/, TokenClassConsts.COMMENT],
        [/\/\*/, { token: TokenClassConsts.COMMENT_QUOTE, next: "@push" }], // nested comment not allowed :-(
        [/\*\//, { token: TokenClassConsts.COMMENT_QUOTE, next: "@pop" }],
        [/./, TokenClassConsts.COMMENT],
      ],
      pseudoColumns: [
        [
          /[$][A-Za-z_][\w@#$]*/,
          {
            cases: {
              "@pseudoColumns": TokenClassConsts.PREDEFINED,
              "@default": TokenClassConsts.IDENTIFIER,
            },
          },
        ],
      ],
      numbers: [
        [/0[xX][0-9a-fA-F]*/, TokenClassConsts.NUMBER_HEX],
        [/[$][+-]*\d*(\.\d*)?/, TokenClassConsts.NUMBER],
        [/((\d+(\.\d*)?)|(\.\d+))([eE][\\\-+]?\d+)?/, TokenClassConsts.NUMBER],
      ],
      strings: [[/'/, { token: "custom-error", next: "@string" }]],
      string: [
        [/[^']+/, TokenClassConsts.STRING],
        [/''/, TokenClassConsts.STRING],
        [/'/, { token: TokenClassConsts.STRING, next: "@pop" }],
      ],
      complexIdentifiers: [[/`/, { token: TokenClassConsts.IDENTIFIER_QUOTE, next: "@quotedIdentifier" }]],
      quotedIdentifier: [
        [/[^`]+/, TokenClassConsts.IDENTIFIER_QUOTE],
        [/``/, TokenClassConsts.IDENTIFIER_QUOTE],
        [/`/, { token: TokenClassConsts.IDENTIFIER_QUOTE, next: "@pop" }],
      ],
      scopes: [
        [/(EXECUTE\s+)?JAR\s+/i, TokenClassConsts.KEYWORD_SCOPE],
        [/(EXECUTE\s+)?CDCSOURCE\s/i, TokenClassConsts.KEYWORD_SCOPE],
        [/(PRINT\s+)?\s/i, TokenClassConsts.KEYWORD_SCOPE],
      ],
      complexDataTypes: [
        [/DOUBLE\s+PRECISION\b/i, { token: TokenClassConsts.TYPE }],
        [/DECIMAL\s+PRECISION\b/i, { token: TokenClassConsts.TYPE }],
        [/TIMESTAMP\s+PRECISION\b/i, { token: TokenClassConsts.TYPE }],
        [/WITHOUT\s+TIME\s+ZONE\b/i, { token: TokenClassConsts.TYPE }],
        [/WITH\s+LOCAL\s+TIME\s+ZONE\b/i, { token: TokenClassConsts.TYPE }],
        [/WITH\s+TIME\s+ZONE\b/i, { token: TokenClassConsts.TYPE }],
        [/WITH\s+TIME\s+ZONE\s+OFFSET\b/i, { token: TokenClassConsts.TYPE }],
        [/WITH\s+LOCAL\s+TIME\s+ZONE\s+OFFSET\b/i, { token: TokenClassConsts.TYPE }],
      ],
    },
    unicode: true,
  });
}

function buildFlinkSQLCompletionItem(monacoLanguages, model, position) {
  const word = model.getWordUntilPosition(position);
  const range = {
    startLineNumber: position.lineNumber,
    endLineNumber: position.lineNumber,
    startColumn: word.startColumn,
    endColumn: word.endColumn,
  };
  return {
    suggestions: FLINK_SQL_KEYWORD.map((item) => {
      return {
        label: item,
        range: range,
        kind: monacoLanguages.CompletionItemKind.Keyword,
        insertText: item,
      };
    }),
  };
}

let flinkSQLCompletionProvider = {
  dispose: () => {},
};

export function registerFlinkSQLCompilation(monacoLanguages) {
  flinkSQLCompletionProvider.dispose();
  flinkSQLCompletionProvider =
    monacoLanguages?.registerCompletionItemProvider(CustomEditorLanguage.FlinkSQL, {
      provideCompletionItems: (model, position) =>
        buildFlinkSQLCompletionItem(monacoLanguages, model, position),
    }) || {
      dispose: () => {},
    };
}
