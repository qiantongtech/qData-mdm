![qData-mdm：オープンソースのマスターデータ管理](images/ja-JP/banner.png)
<p align="center">
  <img src="https://img.shields.io/badge/JDK-1.8+-brightgreen.svg" alt="JDK">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-blue.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.5.21-blue.svg" alt="Vue">
  <img src="https://img.shields.io/badge/license-Apache--2.0-green" alt="License"/>
  <img src="https://img.shields.io/badge/qData--mdm-v1.0.1-blue.svg" alt="qData-mdm v1.0.1"/>

  <img src="https://gitee.com/qiantongtech/qData-mdm/badge/star.svg" alt="Gitee Stars"/>
  <img src="https://img.shields.io/github/stars/qiantongtech/qData-mdm?label=GitHub%20Stars" alt="GitHub Stars"/>
  <img src="https://atomgit.com/qiantongtech/qData-mdm/star/badge.svg"/>
</p>

<p align="center">
  <a href="README.zh-CN.md">📖简体中文</a> | <a href="README.md">📖English</a> | 📖日本語
</p>

## プラットフォーム概要

**業務システムをまたいで、同じマスターデータを使えるように。**

**qData-mdm** は、複数の業務システムで使う基幹データを一元管理する、オープンソースのマスターデータ管理（MDM）ソフトウェアです。データモデルと標準を定義し、マスターデータの登録・更新、品質チェック、API での共有を一つの環境で行えます。

まずは仕入先、品目、設備など、一つの業務領域から始められます。参照データや採番ルールを統一して、二重登録やコード・項目値の不整合を減らします。購買・在庫・会計システムには、照会 API や定期配信を通じて同じマスターデータを提供できます。開発チームは、データ管理や権限制御の仕組みを活用しながら、要件に合わせて機能を拡張できます。

✨✨✨**ドキュメント**✨✨✨ <a href="https://docs.qdata.tech/ja/" target="_blank">https://docs.qdata.tech/ja/</a>（MDM のドキュメントは準備中）

✨✨✨**オープンソース版デモ**✨✨✨ <a href="https://demo-mdm.qdata.tech" target="_blank">https://demo-mdm.qdata.tech</a>（ユーザー名 `qData`、パスワード `qData123`）

✨✨✨**プロフェッショナル版デモ**✨✨✨ <a href="https://pro-demo-mdm.qdata.tech" target="_blank">https://pro-demo-mdm.qdata.tech</a> · [デモ用アカウントのお申し込み](https://docs.qdata.tech/ja/business/policy.html)

> qData-mdm が役に立ったら、リポジトリに **スター ⭐️** を付けて開発を応援してください！ 🚀

ライセンスは **Apache License 2.0 およびプロジェクトの追加条件**です。[LICENSE](LICENSE) と[商用利用の案内](#license-and-services)をご確認ください。

## 主な特長

### モデルを定義し、データの保守と照会に活用

顧客、仕入先、品目、設備などのモデルと項目を定義し、マスターデータのドメインやモデルカタログごとに整理します。同じモデルを登録・更新と API での照会に利用できるため、対象ごとに管理画面や採番ロジック、照会 API を作り直す手間を減らせます。

### 参照データと採番ルールを統一

分類、入力値、識別コードのルールを一元管理します。固定文字列、業務項目、参照データのマッピング、日時、連番を組み合わせてコードを生成できます。

### データ品質の問題を特定し、是正まで追跡

マスターレコードに対して、手動またはスケジュールで品質チェックを実行し、対象のレコード、項目、エラー原因を特定します。担当者への是正タスクの割り当て、進捗の確認、修正後の再チェックまで管理できます。

### REST API とデータ配信で業務システムを連携

公開済みのモデルから**参照専用の REST API** を提供します。検索条件、返却項目、アクセス権限を設定し、1 件取得やページ単位の照会に対応できます。配信先 API に合わせて項目をマッピングし、全件または差分データを手動またはスケジュールで配信できます。

### 業務要件に合わせて機能を拡張

Java、Spring Boot、Vue 3 を採用し、モデリング、マスターデータ、データ品質、取り込み、配信などの機能をモジュール化しています。データ管理や権限管理を共通で利用しながら、品質ルールの追加や業務フローのカスタマイズができます。

## クイックスタート

### オンラインデモを試す

[qData-mdm オープンソース版デモ](https://demo-mdm.qdata.tech)を開くと、ブラウザーで機能を試せます。

**ユーザー名 `qData`　パスワード `qData123`**

既存のマスターデータモデルを一つ選び、次の順に確認してください。

1. **モデルとデータ標準。** モデルの項目、参照データ、採番ルールを確認します。
2. **マスターデータの保守。** 正本レコードを開き、一元管理されている業務項目を確認します。
3. **データ品質。** 品質チェック、検出されたレコード、是正タスクを確認します。
4. **データ共有。** 公開済みのデータサービスや配信ジョブを開き、他システムとの連携方法を確認します。

操作権限がある場合は、照会 API の公開も試せます。公開済みモデルを選び、検索条件と返却項目を設定します。サービスの公開とアクセス権限の設定が済んだら、公開画面に表示される URL から、レコードを 1 件またはページ単位で取得します。

### インストール方法を選ぶ

- **Docker Compose で起動。** `deploy/docker/` の設定を使って、ローカル環境でサービス一式を起動します。
- **ソースコードから起動。** バックエンドの `qdata-mdm-server` とフロントエンドの `qdata-mdm-ui` を個別に起動して、開発や結合テストに利用できます。
- **手動でインストール。** 本番環境や個別要件に合わせて、各サービスをインストール・設定します。

## 製品の機能構成

![qData-mdm オープンソース版の機能構成図。モデリング、データ標準化、取り込み、品質管理、API、配信、権限制御](images/ja-JP/architecture.png)

## 画面イメージ（中国語）

![ログイン画面](images/zh-CN/system/login.png)

![ダッシュボード](images/zh-CN/system/index.png)

![マスターデータの正本](images/zh-CN/system/master.png)

![正本レコード一覧](images/zh-CN/system/master_detail.png)

![マスターデータドメインの詳細](images/zh-CN/system/project_detail.png)

![モデルカタログの詳細](images/zh-CN/system/layer_detail.png)

![マスターデータモデル一覧](images/zh-CN/system/model.png)

![モデルの詳細](images/zh-CN/system/model_detail.png)

![モデルの基本設定](images/zh-CN/system/model_form_one.png)

![1 対多の子モデルの設定](images/zh-CN/system/model_form_four.png)

![モデル間の関連](images/zh-CN/system/relation.png)

![モデル間の関連の詳細](images/zh-CN/system/relation_detail.png)

![照合ルールの設定](images/zh-CN/system/match_form.png)

![項目値の採用ルール](images/zh-CN/system/survivor_form.png)

![データ品質ルールの設定](images/zh-CN/system/quality_form.png)

![重複候補の確認](images/zh-CN/system/review.png)

![データソース接続](images/zh-CN/system/datasource.png)

![データソースの接続テスト](images/zh-CN/system/datasource_detail.png)

![データ取り込みジョブ](images/zh-CN/system/ingest_task.png)

![データ取り込みジョブの詳細](images/zh-CN/system/ingest_task_detail.png)

![データ品質チェックジョブ](images/zh-CN/system/quality_task.png)

![データ品質チェックジョブの設定](images/zh-CN/system/quality_task_form.png)

![データ権限の付与先と対象モデル](images/zh-CN/system/data_auth_form_one.png)

![データ権限の操作範囲と有効期限](images/zh-CN/system/data_auth_form_four.png)

![データ品質の是正タスク](images/zh-CN/system/quality_issue_task.png)

![監査ログ](images/zh-CN/system/operation_log.png)

## 機能概要

モデリングや取り込みから、名寄せ・統合、正本の保守、データ品質管理、他システムへの配信まで、マスターデータのライフサイクルを一元管理します。

| 機能 | 概要 |
| --- | --- |
| **マスターデータのモデリングと標準化** | 標準モデル、分類モデル、1 対多の子モデル、モデル間の関連をドメインやカタログで整理します。参照データと採番ルールで項目値や識別コードを統一し、モデルのバージョンで構造の変更を追跡できます。 |
| **複数データソースからの取り込み** | データベースからの全件・差分取り込み、Excel・CSV のインポート、外部 API からの取得に対応します。ソース項目をモデル項目にマッピングし、元レコード単位で変更を検出して取り込み履歴を残せます。 |
| **名寄せ・データ統合** | 完全一致、あいまい一致、複数項目の重み付けによる照合で、重複候補を抽出します。項目ごとの採用ルールに基づき、値の競合を解消して採用元を記録します。品質チェックと承認を経て正本レコードに反映します。 |
| **マスターデータのライフサイクル管理** | データの登録・更新・一括管理に対応します。承認フローを通じて、変更や有効化、凍結、無効化、アーカイブを管理します。過去のバージョンを比較し、復元を申請できます。 |
| **ルールに基づくデータ品質管理** | 必須項目、一意性、形式、範囲、項目間の整合性をチェックします。手動またはスケジュールで検査し、問題のあるレコードと項目を特定します。担当者に是正タスクを割り当て、再チェックで修正を確認できます。 |
| **データ配信と照会 API** | 配信先ごとに対象データと項目マッピングを設定し、全件、差分、配信済みレコードの状態変更を送信します。参照専用の REST API を公開し、権限を付与した利用者に 1 件取得やページ単位の照会を提供できます。 |
| **きめ細かなアクセス制御と監査** | ユーザー、ロール、組織ごとに、行・項目・操作単位の権限を設定します。業務操作、承認経路、バージョン間の差分、正本レコードの元データを追跡できます。 |

💡 ご提案や機能のご要望は [Issue](https://gitee.com/qiantongtech/qData-mdm/issues) でお寄せください。

## 開発と拡張

フロントエンドとバックエンドを分離し、機能ごとにモジュール化しています。実装を確認しやすく、他の業務システムとの連携や要件に合わせた機能追加に対応できます。

| レイヤー | 主な技術 |
| --- | --- |
| バックエンド | Java、Spring Boot、Spring Security、MyBatis-Plus |
| フロントエンド | Vue 3、Element Plus、Vite |
| データストアとキャッシュ | リレーショナルデータベース、Redis |
| API ドキュメントと動作確認 | Knife4j、OpenAPI |
| 図表・可視化 | AntV X6、ECharts |

## QQ コミュニティ

最新情報の確認や技術サポート、利用者同士の情報交換にご利用ください。 👉 <a href="https://docs.qdata.tech/ja/discuss.html">qData 公式 QQ グループに参加</a>

<a id="license-and-services"></a>

## ライセンスと商用サービス

qData-mdm は **Apache License 2.0 およびプロジェクトの追加条件**に基づき、商用利用が可能です。[LICENSE](LICENSE) に従い、ロゴ、著作権表示、ライセンス表示、クレジットなど、指定された表示を保持してください。

所定のロゴや表示の削除・変更、ホワイトラベル化、OEM 提供、別製品としての再パッケージには、別途商用ライセンスを取得してください。ブランドのカスタマイズ、プロフェッショナル版、技術サービスについては[商用利用の案内](https://docs.qdata.tech/ja/business/policy.html)をご覧ください。

## ミッションとビジョン

**あらゆる企業が、信頼できる統一されたマスターデータを、手の届くコストで整備し、継続的に管理できるようにする。**

私たちは、企業の規模や IT 予算にかかわらず、マスターデータ管理を利用できるべきだと考えています。\
qData-mdm はオープンソースで導入のハードルを下げ、柔軟なモデリングでさまざまな業務に対応します。名寄せとデータ統合によって、システムごとに分散した業務データを、正確で信頼できる正本レコードにまとめます。

コミュニティとともに、オープンで使いやすく、信頼できる MDM ソフトウェアを育てていきます。より多くの企業が自社の基幹データを主体的に管理し、データの定義を組織内でそろえ、信頼できるデータを業務システム間で活用できるようにすることで、業務のデジタル化と新たな取り組みを支えます。
