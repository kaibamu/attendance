# 勤怠管理システム（attendance）

Spring Boot を用いた勤怠管理システム（管理者・社員権限、CSV出力対応）

## 概要
社員は打刻（出勤/退勤/休憩）と勤怠確認ができ、管理者は全社員の勤怠を検索・CSV出力できます。

## 主な機能
### 社員
- 出勤 / 退勤 / 休憩開始 / 休憩終了（直行/直帰対応）
- 自分の勤怠一覧表示（ダッシュボード）
- 勤怠履歴詳細表示
- 勤怠修正依頼（申請）

### 管理者
- 全社員の勤怠一覧表示
- 社員・期間で検索（条件保持）
- CSV出力（BOM付与、null安全、CSVエスケープ対応）
- 異常検知（表示）

## 使用技術
- Java / Spring Boot
- Spring Security
- Thymeleaf
- PostgreSQL

## 起動方法（例）
1. PostgreSQL を起動し、DB を用意
2. `application.properties` の DB 設定を確認
3. 起動：IDEから起動 または `./mvnw spring-boot:run`

## ログイン
- 管理者：ADMIN 権限ユーザーでログイン
- 社員：EMPLOYEE 権限ユーザーでログイン
