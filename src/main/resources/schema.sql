-- users テーブルが存在する場合は削除する。
DROP TABLE IF EXISTS users CASCADE;
-- attendance テーブルが存在する場合は削除する。
DROP TABLE IF EXISTS attendance CASCADE;
-- fix_request テーブルが存在する場合は削除する。
DROP TABLE IF EXISTS fix_request CASCADE;
-- users テーブルを作成する。
CREATE TABLE users (
id SERIAL PRIMARY KEY, -- ユーザーを一意に識別するための ID。自動採番される。
username VARCHAR(50) NOT NULL UNIQUE, -- ログイン名として利用するユーザー名。重複不可。
password VARCHAR(255) NOT NULL, -- ユーザーのパスワード。暗号化して保存するのが通常。
role VARCHAR(20) NOT NULL, -- ユーザーの役割（例：EMPLOYEE や ADMIN など）。
email VARCHAR(255) UNIQUE, -- ユーザーのメールアドレス。重複不可。NULL は許可。
slack_webhook VARCHAR(255), -- Slack 通知用の Webhook URL。NULL 許可。
enabled BOOLEAN DEFAULT TRUE -- アカウントの有効・無効を管理するフラグ。デフォルトは有効。
);
-- attendance テーブルを作成する。
CREATE TABLE attendance (
id SERIAL PRIMARY KEY, -- 勤怠レコードの一意な ID。自動採番される。
user_id INT NOT NULL, -- 勤怠データを持つユーザーの ID。users テーブルの外部キー。
record_date DATE NOT NULL, -- 勤怠記録の日付。
check_in_time TIMESTAMP, -- 出勤時刻。NULL の場合は未打刻を表す。
check_out_time TIMESTAMP, -- 退勤時刻。NULL の場合は未打刻を表す。
break_start_time TIMESTAMP, -- 休憩開始時刻。NULL の場合は未打刻を表す。
break_end_time TIMESTAMP, -- 休憩終了時刻。NULL の場合は未打刻を表す。
location VARCHAR(255), -- 勤怠時の勤務場所などを記録するカラム。NULL 許可。
status VARCHAR(50) DEFAULT 'normal', -- 勤怠の状態。通常は「normal」、異常検知で別の値が入る場合がある。
FOREIGN KEY (user_id) REFERENCES users(id) -- user_id は users テーブルの id を参照する外部キー制約。
);
-- fix_request テーブルを作成する。
CREATE TABLE fix_request (
id SERIAL PRIMARY KEY, -- 修正依頼レコードの一意な ID。自動採番される。
user_id INT NOT NULL, -- 修正を依頼したユーザーの ID。users テーブルの外部キー。
attendance_id INT, -- 修正対象の attendance レコード ID。NULL の場合は新規作成の可能性もある。
request_date DATE NOT NULL, -- 修正依頼を送信した日付。
new_check_in_time TIMESTAMP, -- 修正後の出勤時刻。NULL 許可。
new_check_out_time TIMESTAMP, -- 修正後の退勤時刻。NULL 許可。
new_break_start_time TIMESTAMP, -- 修正後の休憩開始時刻。NULL 許可。
new_break_end_time TIMESTAMP, -- 修正後の休憩終了時刻。NULL 許可。
reason TEXT NOT NULL, -- 修正を依頼する理由。必須。
status VARCHAR(50) DEFAULT 'pending', -- 修正依頼のステータス。pending/approved/rejected など。
FOREIGN KEY (user_id) REFERENCES users(id), -- user_id は users テーブルの id を参照する外部キー制約。
FOREIGN KEY (attendance_id) REFERENCES attendance(id) -- attendance_id は attendance テーブルの id を参照する外部キー制約。
);